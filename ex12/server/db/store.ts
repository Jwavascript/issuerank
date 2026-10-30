// DB 저장소(api 서버용): issues · folders · bookmarks를 읽고 씀. 랭킹 조립은 ranking.ts(메모리 저장소와 같은 곳)
// 수집 저장소(collect.ts용): collectStore가 articles · collect_runs를 읽고 씀. 무엇을 지우고 넣을지는 newsdata.ts가 정함
// 묶기 저장소(group.ts용): groupStore가 articles · issues · bookmarks를 읽고 issues · articles.issue_id를 씀. 무엇을 지우고 넣을지는 grouping.ts가 정함
// 요약 저장소(summarize.ts용): summaryStore가 그날 issues · 묶인 articles를 읽고 issues.summary를 씀. 대상 · 검사는 summary.ts가 정함
// 아침 알림(O5): 구독 API는 dbStore의 저장 · 삭제 · 개수, 발송 명령(push.ts)은 pushStore(목록 · 그날 표시 · 되돌림 · 삭제)로 push_subscriptions를 씀
import { randomUUID } from 'node:crypto';
import { and, asc, count, desc, eq, inArray, isNull, lt, lte, max, ne, notInArray, or, sql } from 'drizzle-orm';
import type { Store } from '../app';
import type { GroupStore } from '../grouping';
import type { CollectStore } from '../newsdata';
import type { PushStore } from '../push-run';
import type { SummaryStore } from '../summary';
import { buildRanking, buildWidget, findCard, SECTIONS, type IssueRow } from '../ranking';
import { kstToday } from '../sample';
import type { Db } from './client';
import { articles, bookmarks, collectRuns, folders, issues, pushSubscriptions } from './schema';

// DB 행 → 랭킹 후보 행(발행 시각은 ISO 문자열로)
const toRow = (r: typeof issues.$inferSelect): IssueRow => ({ ...r, latestPublishedAt: r.latestPublishedAt.toISOString() });

export function dbStore(db: Db): Store {
  // 그 날짜들의 이슈 행 전부
  const rowsOn = async (dates: string[]) =>
    dates.length ? (await db.select().from(issues).where(inArray(issues.date, dates))).map(toRow) : [];

  // 랭킹 · 위젯 목록의 입력: 그날 행 + 대체 섹션 행(D8: 그날 이슈가 없는 섹션만, 그 이전에 그 섹션 이슈가 있는 가장 늦은 날짜의 행)
  // + 그날 ok 실행의 가장 늦은 finished_at만. 그날 행이 없으면 null
  const rankingInput = async (date: string) => {
    const rows = await rowsOn([date]);
    if (rows.length === 0) return null;
    for (const { key } of SECTIONS.filter(s => !rows.some(r => r.section === s.key))) {
      const [prev] = await db.select({ date: max(issues.date) }).from(issues).where(and(eq(issues.section, key), lt(issues.date, date)));
      if (prev?.date) rows.push(...(await db.select().from(issues).where(and(eq(issues.section, key), eq(issues.date, prev.date)))).map(toRow));
    }
    const [run] = await db.select({ finishedAt: max(collectRuns.finishedAt) }).from(collectRuns)
      .where(and(eq(collectRuns.date, date), eq(collectRuns.status, 'ok')));
    return { rows, runs: run?.finishedAt ? [{ status: 'ok', finishedAt: run.finishedAt.toISOString() }] : [] };
  };

  return {
    // D3: KST 오늘 이하 가장 늦은 date
    latestDate: async () => {
      const [r] = await db.select({ date: max(issues.date) }).from(issues).where(lte(issues.date, kstToday()));
      return r?.date ?? null;
    },

    ranking: async date => {
      const input = await rankingInput(date);
      return input && buildRanking(date, input.rows, input.runs);
    },

    // 같은 입력 · 같은 카드로 위젯 순서(D22)
    widget: async date => {
      const input = await rankingInput(date);
      return input && buildWidget(date, input.rows, input.runs);
    },

    issue: async id => {
      const [r] = await db.select({ date: issues.date }).from(issues).where(eq(issues.id, id));
      return r ? findCard(id, await rowsOn([r.date])) : null;
    },

    // rank는 그 이슈 날짜 · 섹션 랭킹에서, savedAt = created_at.toISOString()
    bookmarks: async userId => {
      const list = await db
        .select({ issueId: bookmarks.issueId, folderId: bookmarks.folderId, createdAt: bookmarks.createdAt, date: issues.date })
        .from(bookmarks).innerJoin(issues, eq(bookmarks.issueId, issues.id))
        .where(eq(bookmarks.userId, userId)).orderBy(desc(bookmarks.createdAt), desc(bookmarks.id));
      const rows = await rowsOn([...new Set(list.map(b => b.date))]);
      return list.map(b => ({ ...findCard(b.issueId, rows)!, folderId: b.folderId, savedAt: b.createdAt.toISOString() }));
    },

    // (user_id, issue_id) unique: 이미 있으면 넣지 않고 null
    saveBookmark: async (userId, issue, folderId) => {
      const [r] = await db.insert(bookmarks).values({ userId, issueId: issue.id, folderId })
        .onConflictDoNothing({ target: [bookmarks.userId, bookmarks.issueId] })
        .returning({ createdAt: bookmarks.createdAt });
      return r ? { ...issue, folderId, savedAt: r.createdAt.toISOString() } : null;
    },

    deleteBookmark: async (userId, issueId) =>
      (await db.delete(bookmarks).where(and(eq(bookmarks.userId, userId), eq(bookmarks.issueId, issueId))).returning({ id: bookmarks.id })).length > 0,

    folders: async userId =>
      db.select({ id: folders.id, name: folders.name, count: count(bookmarks.id) })
        .from(folders).leftJoin(bookmarks, and(eq(bookmarks.folderId, folders.id), eq(bookmarks.userId, folders.userId)))
        .where(eq(folders.userId, userId))
        .groupBy(folders.id).orderBy(asc(folders.createdAt), asc(folders.id)),

    // 동시 생성에서도 충돌하지 않는 불투명 id
    createFolder: async (userId, name) => {
      const id = `f${randomUUID()}`;
      await db.insert(folders).values({ id, userId, name });
      return { id, name, count: 0 };
    },

    // 폴더 삭제(O6 · D31), 한 트랜잭션: 그 사용자의 그 폴더 행을 FOR UPDATE로 잠가 읽음(없거나 남의 폴더면 false · 쓰기 0) → 그 사용자 · 그 폴더 북마크 → 폴더.
    // bookmarks.folder_id FK 두 개가 ON DELETE 지정 없음이라 북마크를 먼저 지움. 잠근 사이 다른 기기의 같은 폴더 저장은 기다렸다가 FK 오류로 실패(지우기는 실패 0),
    // 중간 실패면 모두 되돌림. 조건에 user_id + 복합 FK(user_id, folder_id)가 같은 사용자 북마크만 허용 → 다른 사용자 행 영향 0
    deleteFolder: async (userId, folderId) => db.transaction(async tx => {
      const [row] = await tx.select({ id: folders.id }).from(folders)
        .where(and(eq(folders.id, folderId), eq(folders.userId, userId))).for('update');
      if (!row) return false;
      await tx.delete(bookmarks).where(and(eq(bookmarks.userId, userId), eq(bookmarks.folderId, folderId)));
      await tx.delete(folders).where(and(eq(folders.id, folderId), eq(folders.userId, userId)));
      return true;
    }),

    // 아침 알림 구독(O5 · O7): endpoint unique — 있으면 키만 갱신(created_at · last_sent_on 그대로) = 'updated', 없으면 allowNew일 때만 새 행(id UUID) = 'created'.
    // UPDATE … RETURNING 1행 = 'updated' → 0행이면 INSERT … ON CONFLICT (endpoint) DO NOTHING RETURNING 1행 = 'created' · 0행(그사이 다른 요청이 넣음) = UPDATE 한 번 더 → 'updated'
    // (같은 새 endpoint 두 요청이 겹쳐도 행 1 · 'created' 하나) / allowNew 거짓 = null
    savePushSubscription: async (sub, allowNew) => {
      const update = () => db.update(pushSubscriptions).set({ p256dh: sub.p256dh, auth: sub.auth })
        .where(eq(pushSubscriptions.endpoint, sub.endpoint)).returning({ id: pushSubscriptions.id });
      if ((await update()).length > 0) return 'updated';
      if (!allowNew) return null;
      const inserted = await db.insert(pushSubscriptions).values({ id: randomUUID(), ...sub })
        .onConflictDoNothing({ target: pushSubscriptions.endpoint }).returning({ id: pushSubscriptions.id });
      if (inserted.length > 0) return 'created';
      await update();
      return 'updated';
    },

    deletePushSubscription: async endpoint => {
      await db.delete(pushSubscriptions).where(eq(pushSubscriptions.endpoint, endpoint));
    },

    pushSubscriptionCount: async () => (await db.select({ n: count() }).from(pushSubscriptions))[0]?.n ?? 0,
  };
}

// 발송 명령(push.ts)용: 구독 전부(상한 5,000이라 한 번에) · 그날 표시(보내기 전) · 되돌림 · 삭제. 무엇을 언제 할지는 push-run.ts가 정함
export function pushStore(db: Db): PushStore {
  return {
    list: async () => db.select({
      id: pushSubscriptions.id, endpoint: pushSubscriptions.endpoint, p256dh: pushSubscriptions.p256dh,
      auth: pushSubscriptions.auth, lastSentOn: pushSubscriptions.lastSentOn,
    }).from(pushSubscriptions).orderBy(asc(pushSubscriptions.createdAt), asc(pushSubscriptions.id)),

    // 아직 그날 표시가 아닐 때만 그날로(겹친 실행이 먼저 표시했으면 false → 보내지 않음)
    mark: async (id, day) => (await db.update(pushSubscriptions).set({ lastSentOn: day })
      .where(and(eq(pushSubscriptions.id, id), or(isNull(pushSubscriptions.lastSentOn), ne(pushSubscriptions.lastSentOn, day))))
      .returning({ id: pushSubscriptions.id })).length > 0,

    // 그날 표시를 전 값으로(그사이 다른 값이 되었으면 그대로)
    revert: async (id, day, previous) => {
      await db.update(pushSubscriptions).set({ lastSentOn: previous })
        .where(and(eq(pushSubscriptions.id, id), eq(pushSubscriptions.lastSentOn, day)));
    },

    remove: async id => {
      await db.delete(pushSubscriptions).where(eq(pushSubscriptions.id, id));
    },
  };
}

export function collectStore(db: Db): CollectStore {
  return {
    // 그날 articles 행의 link · section
    dayArticles: async date =>
      db.select({ link: articles.link, section: articles.section }).from(articles).where(eq(articles.date, date)),

    // 한 트랜잭션: 그날 sections 행 지움 → 행을 순서대로 넣음 → collect_runs 행(시각은 ISO 문자열 → Date)
    save: async (date, sections, rows, runs) => {
      await db.transaction(async tx => {
        if (sections.length) await tx.delete(articles).where(and(eq(articles.date, date), inArray(articles.section, sections)));
        if (rows.length) await tx.insert(articles).values(rows.map(r => ({ ...r, publishedAt: new Date(r.publishedAt) })));
        await tx.insert(collectRuns).values(runs.map(r => ({ ...r, finishedAt: new Date(r.finishedAt) })));
      });
    },
  };
}

export function groupStore(db: Db): GroupStore {
  return {
    // 그날 articles(섹션 · 제목 · 설명문 · 링크 · 발행 시각 · issue_id) · 그날 issues(id · 섹션 · url) · 북마크가 가리키는 그날 이슈의 섹션
    load: async date => ({
      articles: (await db.select({
        section: articles.section, title: articles.title, description: articles.description,
        link: articles.link, publishedAt: articles.publishedAt, issueId: articles.issueId,
      })
        .from(articles).where(eq(articles.date, date)).orderBy(asc(articles.id)))
        .map(a => ({ ...a, publishedAt: a.publishedAt.toISOString() })),
      issues: await db.select({ id: issues.id, section: issues.section, url: issues.url }).from(issues).where(eq(issues.date, date)),
      bookmarked: (await db.selectDistinct({ section: issues.section }).from(bookmarks)
        .innerJoin(issues, eq(bookmarks.issueId, issues.id)).where(eq(issues.date, date))).map(r => r.section),
    }),

    // 한 트랜잭션: 섹션마다 그날 이슈 중 빠진 것 지움(그 기사들의 issue_id는 null) → 넣기/고치기(있던 id는 summary 그대로, 새 id는 summary null)
    // → links의 그날 기사 issue_id(섹션 조건 없이 link → 이슈 id, 이슈가 없으면 null)
    save: async (date, sections, rows, links) => {
      await db.transaction(async tx => {
        for (const section of sections) {
          const keep = rows.filter(r => r.section === section).map(r => r.id);
          await tx.delete(issues).where(and(eq(issues.date, date), eq(issues.section, section), notInArray(issues.id, keep)));
        }
        if (rows.length) {
          await tx.insert(issues).values(rows.map(r => ({ ...r, latestPublishedAt: new Date(r.latestPublishedAt) })))
            .onConflictDoUpdate({
              target: issues.id,
              set: {
                title: sql`excluded.title`, url: sql`excluded.url`,
                articleCount: sql`excluded.article_count`, importance: sql`excluded.importance`, latestPublishedAt: sql`excluded.latest_published_at`,
              },
            });
        }
        const bound = links.filter(l => l.issueId !== null);
        if (links.length) {
          await tx.update(articles)
            .set({ issueId: bound.length ? sql`case ${articles.link} ${sql.join(bound.map(l => sql`when ${l.link} then ${l.issueId}`), sql` `)} end` : null })
            .where(and(eq(articles.date, date), inArray(articles.link, links.map(l => l.link))));
        }
      });
    },
  };
}

export function summaryStore(db: Db): SummaryStore {
  return {
    // 그날 issues(id · 섹션 · 연관기사 수 · 최신 발행 시각 · summary)
    issues: async date =>
      (await db.select({
        id: issues.id, section: issues.section, articleCount: issues.articleCount, importance: issues.importance,
        latestPublishedAt: issues.latestPublishedAt, summary: issues.summary,
      }).from(issues).where(eq(issues.date, date)))
        .map(r => ({ ...r, latestPublishedAt: r.latestPublishedAt.toISOString() })),

    // 그 이슈에 묶인 articles(제목 · 설명문 · 링크 · 발행 시각)
    articles: async issueId =>
      (await db.select({ title: articles.title, description: articles.description, link: articles.link, publishedAt: articles.publishedAt })
        .from(articles).where(eq(articles.issueId, issueId)))
        .map(a => ({ ...a, publishedAt: a.publishedAt.toISOString() })),

    save: async (issueId, summary) => {
      await db.update(issues).set({ summary }).where(eq(issues.id, issueId));
    },
  };
}
