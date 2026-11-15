import { describe, expect, it } from 'vitest';
import { buildTimeline, matches, parseTimeline, timelineEvents, type TimelineQuery } from '../server/timeline';
import { article, snapshot } from './helpers/keyword-fixture';

const query: TimelineQuery = { terms: ['삼성전자'], from: '2026-10-01', to: '2026-10-07', mode: 'major', order: 'desc' };
describe('관심 키워드 검색·타임라인', () => {
  it('구절·조사·영문 경계와 NFKC를 검사하고 접두어 기업명을 섞지 않는다', () => {
    expect(matches('삼성전자가 실적 발표', '삼성전자')).toBe(true);
    expect(matches('삼성전자서비스 실적', '삼성전자')).toBe(false);
    expect(matches('삼성전자·반도체', '삼성전자')).toBe(true);
    expect(matches('chair', 'AI')).toBe(false);
    expect(matches('ＡＩ 반도체', 'ai')).toBe(true);
  });
  it('기간90일·미래·달력·키워드 수·커서·빈값을 검증한다', () => {
    expect(parseTimeline(new URLSearchParams('q=삼성전자'))?.from).toBe('2026-09-08');
    for (const value of ['q=a', 'q=삼성전자&from=2026-02-30', 'q=삼성전자&to=2026-10-08', 'q=삼성전자&from=2026-01-01', 'q=삼성전자&cursor=oops', 'q=삼성전자&mode=no', 'q=하나&q=두개&q=세개&q=네개']) expect(parseTimeline(new URLSearchParams(value))).toBeNull();
  });
  it('Top5 밖·미연결 기사·설명문·이슈 제목을 찾고 KST 보도일로 정렬한다', () => {
    const data = snapshot([article(1, { title: '분기 실적 발표', description: '삼성전자 실적', publishedAt: '2026-10-06T16:00:00Z' }), article(2, { issueId: 'not-top5', title: '공급 계약', description: null })], { issues: [{ id: 'not-top5', title: '삼성전자 공급 계약 확정', summary: ['요약'] }] });
    const result = buildTimeline(data, query);
    expect(result.total).toBe(2); expect(result.items[0].date).toBe('2026-10-07');
    expect(result.items.some(e => e.summary?.[0] === '요약')).toBe(true);
    expect(result.items.some(e => e.summary === null)).toBe(true);
  });
  it('한 사건의 여러 출처·다중 키워드를 한 카드로 합친다', () => {
    const data = snapshot([article(1, { issueId: 'same', title: '삼성전자 반도체 계약' }), article(2, { issueId: 'same', sourceId: 'two', title: 'SK하이닉스 계약 참여' })]);
    const events = timelineEvents(data, { ...query, terms: ['삼성전자', 'SK하이닉스'] });
    expect(events).toHaveLength(1); expect(events[0].keywords).toEqual(['삼성전자', 'SK하이닉스']);
    expect(events[0].sources).toHaveLength(2); expect(events[0].reasons).toContain('여러 매체 보도');
  });
  it('URL 또는 제목+설명문이 같은 재수집은 합치고 새 사실/제목은 보존한다', () => {
    const data = snapshot([article(1), article(2, { ...article(1), id: 2, collectedOn: '2026-10-08' }), article(3, { title: article(1).title, description: article(1).description }), article(4, { title: '삼성전자 신제품 출시 확정' })]);
    const events = timelineEvents(data, query);
    expect(events).toHaveLength(2); expect(events.find(e => e.title === article(1).title)?.sources).toHaveLength(2);
    expect(events.find(e => e.title.includes('출시'))).toBeDefined();
  });
  it('동일한 제목이어도 설명문이 달라지면 별도 사건으로 보존한다', () => {
    const events = timelineEvents(snapshot([article(1), article(2, { title: article(1).title, description: '다른 분기 실적' })]), query);
    expect(events).toHaveLength(2);
  });
  it('다른 보도일의 정형 제목과 설명문 없는 기사를 함부로 합치지 않는다', () => {
    const data = snapshot([article(1), article(2, { title: article(1).title, description: article(1).description, publishedAt: '2026-10-04T22:00:00Z' }),
      article(3, { title: '삼성전자 발표', description: null }), article(4, { title: '삼성전자 발표', description: null })]);
    expect(timelineEvents(data, { ...query, mode: 'all' })).toHaveLength(4);
  });
  it('주요 하루3개·기준 미달 전체보기·추정 규모 합산 없음·위험URL 제외', () => {
    const data = snapshot([...Array.from({ length: 5 }, (_, i) => article(i)), article(8, { title: '삼성전자 관련 전망', description: null }), article(9, { link: 'javascript:alert(1)' })]);
    expect(buildTimeline(data, query).total).toBe(3);
    expect(buildTimeline(data, { ...query, mode: 'all' }).total).toBe(6);
    expect(JSON.stringify(buildTimeline(data, query))).not.toContain('articleCount');
  });
  it('계획/예정/부인 제목을 발표 확정 근거로 사용하지 않는다', () => {
    const result = buildTimeline(snapshot([article(1, { title: '삼성전자 출시 계획 발표 예정' })]), query);
    expect(result.total).toBe(0); expect(result.relatedCount).toBe(1);
  });
  it('공통 사건이 있어도 어느 키워드도 하루3개를 넘지 않는다', () => {
    const rows = [article(1), article(2), article(3), article(4, { title: '삼성전자 SK하이닉스 공동 발표' }), article(5, { title: 'SK하이닉스 실적 발표' })];
    const events = timelineEvents(snapshot(rows), { ...query, terms: ['삼성전자', 'SK하이닉스'] });
    for (const name of ['삼성전자', 'SK하이닉스']) expect(events.filter(e => e.keywords.includes(name)).length).toBeLessThanOrEqual(3);
  });
  it('커서를 따라 모든 결과를 중복/누락 없이 양방향으로 읽는다', () => {
    const data = snapshot(Array.from({ length: 47 }, (_, i) => article(i)));
    for (const order of ['asc', 'desc'] as const) {
      const q = { ...query, order, mode: 'all' as const };
      const first = buildTimeline(data, q), second = buildTimeline(data, { ...q, cursor: first.nextCursor! }), third = buildTimeline(data, { ...q, cursor: second.nextCursor! });
      expect([first.items.length, second.items.length, third.items.length]).toEqual([20, 20, 7]);
      expect(new Set([...first.items, ...second.items, ...third.items].map(e => e.id)).size).toBe(47); expect(third.nextCursor).toBeNull();
    }
  });
  it('빈 데이터와 수집 실패·저장 범위 메타데이터를 구별한다', () => {
    const result = buildTimeline(snapshot([], { firstDate: null, failedCollections: ['2026-10-07'] }), query);
    expect(result.total).toBe(0); expect(result.relatedCount).toBe(0); expect(result.firstDate).toBeNull(); expect(result.failedCollections).toEqual(['2026-10-07']);
  });
});
