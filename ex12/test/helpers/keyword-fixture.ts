import { randomUUID } from 'node:crypto';
import type { KeywordStore, SavedKeyword } from '../../server/keyword-store';
import { normalize, type TimelineArticle, type TimelineSnapshot } from '../../server/timeline';

export const article = (id: number, overrides: Partial<TimelineArticle> = {}): TimelineArticle => ({ id, issueId: null, title: `삼성전자 실적 발표 ${id}`, description: `분기 실적 ${id}`, link: `https://news.example/${id}`, sourceId: 'one', publishedAt: '2026-10-06T23:00:00.000Z', collectedOn: '2026-10-07', ...overrides });
export const snapshot = (articles: TimelineArticle[] = [], extra: Partial<TimelineSnapshot> = {}): TimelineSnapshot => ({ articles, issues: [], firstDate: '2026-10-01', lastDate: '2026-10-07', updatedAt: '2026-10-07T06:30:00+09:00', failedCollections: [], ...extra });
export function memoryKeywords(initial = snapshot()) {
  let data = initial;
  const owners = new Map<string, SavedKeyword[]>();
  const devices = new Map<string, { userId: string; sessionId: string; morning: boolean }>();
  const list = (userId: string) => { if (!owners.has(userId)) owners.set(userId, []); return owners.get(userId)!; };
  const store: KeywordStore = {
    snapshot: async () => structuredClone(data),
    list: async userId => structuredClone(list(userId)),
    create: async (userId, name, baseline) => {
      const current = list(userId);
      if (current.some(k => normalize(k.name) === normalize(name))) return 'duplicate';
      if (current.length === 5) return 'full';
      const row = { id: randomUUID(), name, color: [0, 1, 2, 3, 4].find(n => !current.some(k => k.color === n))!, alerts: false,
        baseline, createdAt: new Date().toISOString(), enabledAt: new Date().toISOString() };
      current.push(row); return structuredClone(row);
    },
    update: async (userId, id, alerts, baseline) => {
      const row = list(userId).find(k => k.id === id);
      if (!row) return null;
      Object.assign(row, { alerts, baseline, enabledAt: new Date().toISOString() }); return structuredClone(row);
    },
    remove: async (userId, id) => { const rows = list(userId); const at = rows.findIndex(k => k.id === id); if (at < 0) return false; rows.splice(at, 1); return true; },
    link: async (userId, sessionId, endpoint, morning, auth) => {
      if (auth !== 'proof' || endpoint.endsWith('/missing')) return 'missing';
      const old = devices.get(endpoint);
      if (old && old.userId !== userId) return 'conflict';
      devices.set(endpoint, { userId, sessionId, morning }); return 'ok';
    },
    unlink: async (userId, endpoint) => { if (devices.get(endpoint)?.userId === userId) devices.delete(endpoint); },
    device: async (userId, sessionId, endpoint) => {
      const row = devices.get(endpoint); return row?.userId === userId ? { endpoint, morning: row.morning, active: row.sessionId === sessionId } : null;
    },
  };
  return { store, setSnapshot: (next: TimelineSnapshot) => { data = next; } };
}
