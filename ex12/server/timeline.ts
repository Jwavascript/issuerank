import { createHash } from 'node:crypto';
import { z } from 'zod';

export type TimelineArticle = { id: number; issueId: string | null; title: string; description: string | null; link: string; sourceId: string; publishedAt: string; collectedOn: string };
export type TimelineIssue = { id: string; title: string; summary: string[] | null };
export type TimelineSnapshot = { articles: TimelineArticle[]; issues: TimelineIssue[]; firstDate: string | null; lastDate: string | null; updatedAt: string | null; failedCollections: string[] };
export type TimelineQuery = { terms: string[]; from: string; to: string; order: 'asc' | 'desc'; mode: 'major' | 'all'; cursor?: string };
export type TimelineEvent = { id: string; key: string; title: string; summary: string[] | null; publishedAt: string; date: string; collectedOn: string; keywords: string[]; reasons: string[]; sources: { name: string; title: string; url: string }[]; fingerprints: string[] };
export const normalize = (s: string) => s.normalize('NFKC').toLocaleLowerCase('ko').replace(/\s+/gu, ' ').trim();
export const keywordName = z.string().trim().min(2).max(40).refine(v => !/[\p{Cc}\p{Cf}]/u.test(v));
export const digest = (s: string) => createHash('sha256').update(s).digest('hex');
export const kstDate = (s: string | number) => new Date((typeof s === 'number' ? s : Date.parse(s)) + 9 * 3600e3).toISOString().slice(0, 10);
export const dayShift = (day: string, n: number) => new Date(Date.parse(day + 'T00:00:00Z') + n * 864e5).toISOString().slice(0, 10);
export const safeUrl = (s: string) => { try { return ['http:', 'https:'].includes(new URL(s).protocol); } catch { return false; } };

// 구절 양끝이 같은 문자군의 단어 내부이면 일치하지 않는다(삼성전자≠삼성전자서비스, AI≠chair).
export function matches(text: string, term: string): boolean {
  const hay = normalize(text), needle = normalize(term);
  let at = -1;
  const sameWord = (a: string, b: string) => /[가-힣]/u.test(a) && /[가-힣]/u.test(b)
    || /[a-z0-9]/u.test(a) && /[a-z0-9]/u.test(b);
  while ((at = hay.indexOf(needle, at + 1)) !== -1) {
    const tail = hay.slice(at + needle.length);
    const particle = /^(?:은|는|이|가|을|를|의|에|와|과|도|만|로|으로|에서|에게|부터|까지)(?=$|[^가-힣])/u.test(tail);
    if (!sameWord(hay[at - 1] ?? '', needle[0]) && (!sameWord(hay[at + needle.length] ?? '', needle.at(-1)!) || particle)) return true;
  }
  return false;
}

export function parseTimeline(params: URLSearchParams, now = Date.now()): TimelineQuery | null {
  const today = kstDate(now);
  const raw = params.getAll('q');
  const parsed = raw.map(t => keywordName.safeParse(t));
  if (raw.length < 1 || raw.length > 3 || parsed.some(r => !r.success)) return null;
  const terms = [...new Map(parsed.map(r => [normalize(r.data!), r.data!])).values()];
  const from = params.get('from') ?? dayShift(today, -29), to = params.get('to') ?? today;
  if (!z.iso.date().safeParse(from).success || !z.iso.date().safeParse(to).success || from > to || to > today || Date.parse(to) - Date.parse(from) > 89 * 864e5) return null;
  const order = params.get('order') ?? 'desc', mode = params.get('mode') ?? 'major', cursor = params.get('cursor') ?? undefined;
  if (!['asc', 'desc'].includes(order) || !['major', 'all'].includes(mode)) return null;
  if (cursor && !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z\|[a-f0-9]{64}$/.test(cursor)) return null;
  return { terms, from, to, order: order as 'asc' | 'desc', mode: mode as 'major' | 'all', cursor };
}

type Group = { articles: TimelineArticle[]; issues: TimelineIssue[] };
export function timelineEvents(snapshot: TimelineSnapshot, query: TimelineQuery): TimelineEvent[] {
  const issueMap = new Map(snapshot.issues.map(i => [i.id, i]));
  const initial = new Map<string, Group>();
  for (const article of snapshot.articles) {
    const date = kstDate(article.publishedAt);
    if (date < query.from || date > query.to || !safeUrl(article.link)) continue;
    const id = article.issueId ?? `article-${article.id}`;
    const group = initial.get(id) ?? { articles: [], issues: article.issueId && issueMap.has(article.issueId) ? [issueMap.get(article.issueId)!] : [] };
    group.articles.push(article); initial.set(id, group);
  }
  // URL 또는 제목+설명문 전체가 같을 때만 날짜/섹션을 넘어 병합. 유사어 추론은 하지 않는다.
  const groups: Group[] = [], aliases = new Map<string, Group>();
  const articleAliases = (a: TimelineArticle) => ['url:' + digest(a.link), ...(a.description?.trim()
    ? ['text:' + digest(kstDate(a.publishedAt) + '\n' + normalize(a.title) + '\n' + normalize(a.description))] : [])];
  for (const group of initial.values()) {
    const keys = group.articles.flatMap(articleAliases);
    const hits = [...new Set(keys.map(k => aliases.get(k)).filter((g): g is Group => !!g))];
    const target = hits.shift() ?? { articles: [], issues: [] };
    if (!groups.includes(target)) groups.push(target);
    for (const hit of hits) {
      target.articles.push(...hit.articles); target.issues.push(...hit.issues);
      groups.splice(groups.indexOf(hit), 1);
      for (const [key, owner] of aliases) if (owner === hit) aliases.set(key, target);
    }
    target.articles.push(...group.articles); target.issues.push(...group.issues);
    for (const key of keys) aliases.set(key, target);
  }
  const result: TimelineEvent[] = [];
  for (const group of groups) {
    group.issues.sort((a, b) => a.id.localeCompare(b.id));
    const keywords = query.terms.filter(term => group.articles.some(a => matches(a.title, term) || matches(a.description ?? '', term)) || group.issues.some(i => matches(i.title, term)));
    if (!keywords.length) continue;
    const articles = [...group.articles].sort((a, b) => a.publishedAt.localeCompare(b.publishedAt) || a.link.localeCompare(b.link));
    const first = articles[0];
    const fingerprints = [...new Set(articles.flatMap(articleAliases))].sort();
    const id = digest(first.link + '\n' + normalize(first.title));
    const reasons: string[] = [];
    if (new Set(articles.map(a => a.sourceId).filter(Boolean)).size >= 2) reasons.push('여러 매체 보도');
    const title = group.issues[0]?.title ?? first.title;
    if (!/예정|검토|추진|전망|가능|논의|계획|미확정|불발|부인|취소/.test(title)) {
      if (/확정|의결|승인|처분|과징금/.test(title)) reasons.push('결정·처분 보도');
      else if (/발표|공시/.test(title)) reasons.push('발표 보도');
      else if (/시행|출시/.test(title)) reasons.push('시행·출시 보도');
    }
    const sources = [...new Map(articles.map(a => [a.link, { name: a.sourceId || '출처', title: a.title, url: a.link }])).values()];
    result.push({ id, key: new Date(first.publishedAt).toISOString() + '|' + id, title, summary: group.issues.find(i => i.summary)?.summary ?? null,
      publishedAt: first.publishedAt, date: kstDate(first.publishedAt), collectedOn: articles.map(a => a.collectedOn).sort()[0], keywords, reasons, sources, fingerprints });
  }
  const all = result.sort((a, b) => b.sources.length - a.sources.length || b.key.localeCompare(a.key));
  if (query.mode === 'all') return all;
  const counts = new Map<string, number>();
  return all.filter(event => {
    if (!event.reasons.length) return false;
    const keys = event.keywords.map(k => normalize(k) + '|' + event.date);
    if (!keys.every(k => (counts.get(k) ?? 0) < 3)) return false;
    for (const key of keys) counts.set(key, (counts.get(key) ?? 0) + 1);
    return true;
  });
}

export function buildTimeline(snapshot: TimelineSnapshot, query: TimelineQuery) {
  const allQuery = { ...query, mode: 'all' as const };
  const relatedCount = timelineEvents(snapshot, allQuery).length;
  const events = timelineEvents(snapshot, query).sort((a, b) => query.order === 'asc' ? a.key.localeCompare(b.key) : b.key.localeCompare(a.key));
  const remaining = query.cursor ? events.filter(e => query.order === 'asc' ? e.key > query.cursor! : e.key < query.cursor!) : events;
  const page = remaining.slice(0, 20);
  return { items: page, nextCursor: remaining.length > 20 ? page.at(-1)!.key : null, total: events.length, relatedCount,
    firstDate: snapshot.firstDate, lastDate: snapshot.lastDate, updatedAt: snapshot.updatedAt, failedCollections: snapshot.failedCollections };
}
