import { randomUUID } from 'node:crypto';
import { and, asc, eq, gte, lte, min, max, sql } from 'drizzle-orm';
import type { KeywordStore, SavedKeyword } from '../keyword-store';
import { keywordNotification } from '../keyword-store';
import { dayShift, normalize } from '../timeline';
import type { PushRow } from '../push-run';
import type { Db } from './client';
import { articles, collectRuns, issues, keywordDevices, pushSubscriptions, savedKeywords } from './schema';
import { session, user } from './auth-schema';

const saved = (row: typeof savedKeywords.$inferSelect): SavedKeyword => ({ ...row, createdAt: row.createdAt.toISOString(), enabledAt: row.enabledAt.toISOString() });
export function dbKeywordStore(db: Db): KeywordStore {
  return {
    snapshot: async (from, to) => {
      const rows = await db.select({ article: articles, issue: { id: issues.id, title: issues.title, summary: issues.summary } }).from(articles)
        .leftJoin(issues, eq(articles.issueId, issues.id))
        .where(and(gte(articles.publishedAt, new Date(from + 'T00:00:00+09:00')), lte(articles.publishedAt, new Date(to + 'T23:59:59.999+09:00'))));
      const candidates = await db.select().from(issues).where(and(gte(issues.latestPublishedAt, new Date(from + 'T00:00:00+09:00')), lte(issues.latestPublishedAt, new Date(to + 'T23:59:59.999+09:00'))));
      const linked = new Set(rows.map(r => r.article.issueId));
      const orphanArticles = candidates.filter(i => !linked.has(i.id)).map((i, index) => ({ id: -(index + 1), issueId: i.id, title: i.title, description: null,
        link: i.url, sourceId: (() => { try { return new URL(i.url).hostname; } catch { return ''; } })(), publishedAt: i.latestPublishedAt.toISOString(), collectedOn: i.date }));
      const [range] = await db.select({ first: min(articles.publishedAt), last: max(articles.publishedAt) }).from(articles);
      const [issueRange] = await db.select({ first: min(issues.latestPublishedAt), last: max(issues.latestPublishedAt) }).from(issues);
      const [run] = await db.select({ at: max(collectRuns.finishedAt) }).from(collectRuns).where(eq(collectRuns.status, 'ok'));
      const failed = await db.select({ date: collectRuns.date, section: collectRuns.section, status: collectRuns.status, at: collectRuns.finishedAt }).from(collectRuns)
        .where(and(gte(collectRuns.date, from), lte(collectRuns.date, dayShift(to, 1)))).orderBy(asc(collectRuns.finishedAt));
      const statuses = new Map(failed.map(r => [r.date + '|' + r.section, r]));
      const date = (value: Date | null) => value ? new Date(value.getTime() + 9 * 3600e3).toISOString().slice(0, 10) : null;
      const first = [range?.first, issueRange?.first].filter((d): d is Date => !!d).sort((a, b) => a.getTime() - b.getTime())[0];
      const last = [range?.last, issueRange?.last].filter((d): d is Date => !!d).sort((a, b) => b.getTime() - a.getTime())[0];
      return { articles: [...rows.map(({ article: a }) => ({ ...a, collectedOn: a.date, publishedAt: a.publishedAt.toISOString() })), ...orphanArticles],
        issues: [...new Map([...rows.flatMap(r => r.issue ? [[r.issue.id, r.issue] as const] : []), ...candidates.map(i => [i.id, { id: i.id, title: i.title, summary: i.summary }] as const)]).values()],
        firstDate: date(first ?? null), lastDate: date(last ?? null), updatedAt: run?.at?.toISOString() ?? null,
        failedCollections: [...new Set([...statuses.values()].filter(r => r.status !== 'ok').map(r => r.date))] };
    },
    list: async userId => (await db.select().from(savedKeywords).where(eq(savedKeywords.userId, userId)).orderBy(asc(savedKeywords.color))).map(saved),
    create: async (userId, name, baseline) => db.transaction(async tx => {
      await tx.select({ id: user.id }).from(user).where(eq(user.id, userId)).for('update');
      const list = await tx.select().from(savedKeywords).where(eq(savedKeywords.userId, userId));
      if (list.some(k => k.normalized === normalize(name))) return 'duplicate';
      if (list.length >= 5) return 'full';
      const color = [0, 1, 2, 3, 4].find(n => !list.some(k => k.color === n))!;
      const [row] = await tx.insert(savedKeywords).values({ id: randomUUID(), userId, name, normalized: normalize(name), color, baseline }).returning();
      return saved(row);
    }),
    update: async (userId, id, alerts, baseline) => {
      const [row] = await db.update(savedKeywords).set({ alerts, baseline, enabledAt: new Date() }).where(and(eq(savedKeywords.userId, userId), eq(savedKeywords.id, id))).returning();
      return row ? saved(row) : null;
    },
    remove: async (userId, id) => (await db.delete(savedKeywords).where(and(eq(savedKeywords.userId, userId), eq(savedKeywords.id, id))).returning({ id: savedKeywords.id })).length > 0,
    link: async (userId, sessionId, endpoint, morning, auth) => db.transaction(async tx => {
      const [sub] = await tx.select().from(pushSubscriptions).where(and(eq(pushSubscriptions.endpoint, endpoint), eq(pushSubscriptions.auth, auth))).for('update');
      if (!sub) return 'missing';
      const [current] = await tx.select().from(session).where(and(eq(session.id, sessionId), eq(session.userId, userId), gte(session.expiresAt, new Date())));
      if (!current) return 'missing';
      const [old] = await tx.select({ owner: keywordDevices.userId, expiresAt: session.expiresAt }).from(keywordDevices)
        .leftJoin(session, eq(keywordDevices.sessionId, session.id)).where(eq(keywordDevices.subscriptionId, sub.id));
      if (old && old.owner !== userId && old.expiresAt && old.expiresAt > new Date()) return 'conflict';
      await tx.insert(keywordDevices).values({ subscriptionId: sub.id, userId, sessionId, morning })
        .onConflictDoUpdate({ target: keywordDevices.subscriptionId, set: { userId, sessionId, morning, seen: sql`case when ${keywordDevices.userId} = ${userId} then ${keywordDevices.seen} else ARRAY[]::text[] end` } });
      return 'ok';
    }),
    unlink: async (userId, endpoint) => {
      const [sub] = await db.select({ id: pushSubscriptions.id }).from(pushSubscriptions).where(eq(pushSubscriptions.endpoint, endpoint));
      if (sub) await db.delete(keywordDevices).where(and(eq(keywordDevices.subscriptionId, sub.id), eq(keywordDevices.userId, userId)));
    },
    device: async (userId, sessionId, endpoint) => {
      const [row] = await db.select({ endpoint: pushSubscriptions.endpoint, morning: keywordDevices.morning, sessionId: keywordDevices.sessionId }).from(keywordDevices)
        .innerJoin(pushSubscriptions, eq(keywordDevices.subscriptionId, pushSubscriptions.id))
        .where(and(eq(keywordDevices.userId, userId), eq(pushSubscriptions.endpoint, endpoint)));
      return row ? { endpoint: row.endpoint, morning: row.morning, active: row.sessionId === sessionId } : null;
    },
  };
}

// 기존 아침 구독은 그대로. 연결된 기기만 개인 payload를 준비하고 로그아웃/만료는 일반 알림 설정만 사용.
export async function prepareKeywordPush(db: Db, row: PushRow, day: string) {
  const [link] = await db.select({ device: keywordDevices, validSession: session.id, expiresAt: session.expiresAt }).from(keywordDevices)
    .leftJoin(session, and(eq(keywordDevices.sessionId, session.id), eq(keywordDevices.userId, session.userId))).where(eq(keywordDevices.subscriptionId, row.id));
  if (!link) return { payload: undefined, seen: undefined };
  const active = !!link.validSession && !!link.expiresAt && link.expiresAt > new Date();
  const store = dbKeywordStore(db);
  const keywords = active ? await store.list(link.device.userId) : [];
  const snapshot = active && keywords.some(k => k.alerts) ? await store.snapshot(dayShift(day, -89), day)
    : { articles: [], issues: [], firstDate: null, lastDate: null, updatedAt: null, failedCollections: [] };
  // 수집 실패가 있는 날에는 개인 결과를 정상적인 '소식 없음'으로 확정하지 않는다. 이력은 기록하지 않음.
  const ready = keywordNotification(keywords, snapshot, link.device.seen, link.device.morning, active && !snapshot.failedCollections.includes(day), day);
  return { ...ready, sessionId: link.device.sessionId };
}
