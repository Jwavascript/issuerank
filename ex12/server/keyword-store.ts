import type { PushPayload } from './push-config';
import type { TimelineSnapshot } from './timeline';
import { dayShift, kstDate, timelineEvents } from './timeline';

export type SavedKeyword = { id: string; name: string; color: number; alerts: boolean; createdAt: string; enabledAt: string; baseline: string[] };
export type KeywordDevice = { endpoint: string; morning: boolean; active: boolean };
export type KeywordStore = {
  snapshot(from: string, to: string): Promise<TimelineSnapshot>;
  list(userId: string): Promise<SavedKeyword[]>;
  create(userId: string, name: string, baseline: string[]): Promise<SavedKeyword | 'duplicate' | 'full'>;
  update(userId: string, id: string, alerts: boolean, baseline: string[]): Promise<SavedKeyword | null>;
  remove(userId: string, id: string): Promise<boolean>;
  link(userId: string, sessionId: string, endpoint: string, morning: boolean, auth: string): Promise<'ok' | 'missing' | 'conflict'>;
  unlink(userId: string, endpoint: string): Promise<void>;
  device(userId: string, sessionId: string, endpoint: string): Promise<KeywordDevice | null>;
};
export const publicKeyword = ({ id, name, color, alerts, createdAt }: SavedKeyword) => ({ id, name, color, alerts, createdAt });
export async function keywordBaseline(store: KeywordStore, name: string, now = Date.now()): Promise<string[]> {
  const to = kstDate(now), from = dayShift(to, -89);
  return timelineEvents(await store.snapshot(from, to), { terms: [name], from, to, order: 'desc', mode: 'all' }).flatMap(e => e.fingerprints);
}

// 결과를 준비하는 순수 함수. 발송 서비스는 성공/응답불명일 때만 returned seen을 저장한다.
export function keywordNotification(keywords: SavedKeyword[], snapshot: TimelineSnapshot, seen: string[], morning: boolean, active: boolean, day: string): { payload: PushPayload | null; seen: string[] } {
  const found = new Map<string, { terms: Set<string>; ids: string[] }>();
  if (active) for (const keyword of keywords.filter(k => k.alerts)) {
    const from = dayShift(day, -89);
    const events = timelineEvents(snapshot, { terms: [keyword.name], from, to: day, mode: 'major', order: 'desc' });
    for (const event of events) {
      // 수집일의 끝이 활성화 이후여야 함. 활성화 날은 baseline으로 이미 있던 결과를 제외한다.
      if (event.collectedOn < kstDate(keyword.enabledAt) || event.date < dayShift(day, -2)) continue;
      const ids = event.fingerprints.map(f => keyword.id + ':' + f);
      if (event.fingerprints.some(f => keyword.baseline.includes(f)) || ids.some(id => seen.includes(id))) continue;
      const key = event.id;
      const previous = found.get(key) ?? { terms: new Set<string>(), ids: [] };
      previous.terms.add(keyword.name); previous.ids.push(...ids); found.set(key, previous);
    }
  }
  if (!found.size) return { payload: morning ? { title: '오늘의 뉴스가 도착했어요', body: '', tag: 'issuerank-daily', url: '/p' } : null, seen };
  // 개인 키워드는 잠금 화면에 노출하지 않고, 누른 뒤 로그인한 화면에서 확인한다.
  const focus = [...new Set([...found.values()].flatMap(e => e.ids.map(id => id.split(':')[0])))];
  const params = new URLSearchParams({ timeline: '1', focus: focus.join(','), from: dayShift(day, -2), to: day });
  const payload = { title: '관심 키워드에 새로운 주요 소식이 있어요', body: `주요 소식 ${found.size}건${morning ? ' · 아침 뉴스도 준비됐어요' : ''}`, tag: 'issuerank-daily', url: '/?' + params + '#keywords' };
  // URL에는 검색어 대신 불투명 ID와 기간만 담는다. 다른 계정에서는 저장 목록과 일치하지 않아 선택되지 않는다.
  return { payload, seen: [...new Set([...seen, ...[...found.values()].flatMap(e => e.ids)])] };
}
