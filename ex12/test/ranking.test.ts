import { describe, expect, it } from 'vitest';
import { buildRanking, buildWidget, findCard, rankIssues, updatedLabel, type Candidate, type IssueRow, type SectionKey } from '../server/ranking';

const AT_03 = '2026-10-07T03:00:00+09:00';

// 동점 후보: c2 · c3 · c4는 80건, c4는 UTC 표기(= KST 06:00)라 문자열로 비교하면 가장 이르게 보임
const TIED = [
  { id: 'c1', articleCount: 120, latestPublishedAt: AT_03 },
  { id: 'c2', articleCount: 80, latestPublishedAt: '2026-10-07T05:10:00+09:00' },
  { id: 'c3', articleCount: 80, latestPublishedAt: '2026-10-07T05:40:00+09:00' },
  { id: 'c4', articleCount: 80, latestPublishedAt: '2026-10-06T21:00:00Z' },
  { id: 'c5', articleCount: 30, latestPublishedAt: AT_03 },
  { id: 'c6', articleCount: 30, latestPublishedAt: AT_03 },
  { id: 'c7', articleCount: 10, latestPublishedAt: AT_03 },
];

const FEW = [
  { id: 'a', articleCount: 5, latestPublishedAt: AT_03 },
  { id: 'b', articleCount: 50, latestPublishedAt: AT_03 },
  { id: 'c', articleCount: 20, latestPublishedAt: AT_03 },
];

describe('rankIssues', () => {
  it('동점: 연관기사 수 ↓ → 발행 시각(시각으로 비교) ↓ → id ↑, 앞 5개에 rank 1~5 (AC-03-2)', () => {
    const expected = [
      { id: 'c1', rank: 1 }, { id: 'c4', rank: 2 }, { id: 'c3', rank: 3 }, { id: 'c2', rank: 4 }, { id: 'c5', rank: 5 },
    ];
    for (const input of [TIED, [...TIED].reverse()]) {
      const ranked = rankIssues(input);
      expect(ranked.map(({ id, rank }) => ({ id, rank }))).toEqual(expected);
      expect(ranked.map(x => x.id)).not.toContain('c6');
      expect(ranked.map(x => x.id)).not.toContain('c7');
    }
  });

  it('두 자리 번호: 같은 건수 · 시각이면 id 자연 순서(-9가 -10보다 위)', () => {
    const pair = [
      { id: '2026-10-07-pol-10', articleCount: 50, latestPublishedAt: AT_03 },
      { id: '2026-10-07-pol-9', articleCount: 50, latestPublishedAt: AT_03 },
    ];
    for (const input of [pair, [...pair].reverse()]) {
      expect(rankIssues(input).map(x => x.id)).toEqual(['2026-10-07-pol-9', '2026-10-07-pol-10']);
    }
  });

  it('적은 후보: 3개면 3개, rank 1~3', () => {
    const ranked = rankIssues(FEW);
    expect(ranked.map(({ id, rank }) => ({ id, rank }))).toEqual([
      { id: 'b', rank: 1 }, { id: 'c', rank: 2 }, { id: 'a', rank: 3 },
    ]);
  });
});

// 이슈 행 하나(랭킹 후보)
const row = (date: string, section: SectionKey, n: number, articleCount: number): IssueRow => ({
  id: `${date}-${section}-${n}`, date, section, title: `${section} ${n}`, summary: null, articleCount, importance: 0,
  url: `https://example.com/${date}-${section}-${n}`, latestPublishedAt: `${date}T03:00:00+09:00`,
});
const KEYS: SectionKey[] = ['pol', 'eco', 'soc', 'cul', 'wor', 'it'];

describe('buildRanking (D8 · AC-05-1)', () => {
  // 10-06: 6섹션 1행씩, 10-07: pol 6행(60 · 50 · 40 · 30 · 20 · 10)
  const ROWS = [
    ...KEYS.map(k => row('2026-10-06', k, 1, 10)),
    ...[60, 50, 40, 30, 20, 10].map((count, i) => row('2026-10-07', 'pol', i + 1, count)),
  ];

  it('⑦ D8: 그날 이슈가 없는 섹션만 직전 날짜의 Top 5(id · date는 그 날짜), 그날 행이 없으면 null', () => {
    for (const rows of [ROWS, [...ROWS].reverse()]) {
      const ranking = buildRanking('2026-10-07', rows)!;
      expect(ranking.date).toBe('2026-10-07');
      const pol = ranking.sections.find(s => s.key === 'pol')!.issues;
      expect(pol.map(x => [x.id, x.rank, x.articleCount, x.date])).toEqual(
        [1, 2, 3, 4, 5].map(n => [`2026-10-07-pol-${n}`, n, 70 - n * 10, '2026-10-07']));
      for (const s of ranking.sections.filter(x => x.key !== 'pol')) {
        expect(s.issues.map(x => [x.id, x.date, x.rank])).toEqual([[`2026-10-06-${s.key}-1`, '2026-10-06', 1]]);
      }
    }
    expect(buildRanking('2026-10-07', ROWS.filter(r => r.date === '2026-10-06'))).toBeNull();
  });

  it('⑧ updatedAt: 그날 ok 실행의 가장 늦은 finished_at(KST, 초 아래 버림), ok가 없으면 06:00', () => {
    const runs = [
      { status: 'ok', finishedAt: '2026-10-06T21:03:05.123Z' },
      { status: 'failed', finishedAt: '2026-10-06T22:00:00Z' },
    ];
    expect(buildRanking('2026-10-07', ROWS, runs)!.updatedAt).toBe('2026-10-07T06:03:05+09:00');
    expect(buildRanking('2026-10-07', ROWS, runs.filter(r => r.status === 'failed'))!.updatedAt).toBe('2026-10-07T06:00:00+09:00');
    expect(buildRanking('2026-10-07', ROWS)!.updatedAt).toBe('2026-10-07T06:00:00+09:00');
  });
});


describe('D16 중요도 동점 순위', () => {
  it('건수 → 중요도 → 시각 → id 자연 순서, 입력 순서와 무관하고 원본을 보존한다', () => {
    const candidates = [
      { id: 'count', articleCount: 11, importance: 0, latestPublishedAt: AT_03 },
      { id: 'issue-10', articleCount: 10, importance: 5, latestPublishedAt: AT_03 },
      { id: 'issue-9', articleCount: 10, importance: 5, latestPublishedAt: AT_03 },
      { id: 'newer', articleCount: 10, importance: 5, latestPublishedAt: '2026-10-06T21:00:00Z' },
      { id: 'lower', articleCount: 10, importance: 1, latestPublishedAt: '2026-10-07T12:00:00+09:00' },
      { id: 'legacy', articleCount: 10, latestPublishedAt: AT_03 },
    ];
    for (const input of [candidates, [...candidates].reverse()]) {
      const before = structuredClone(input);
      expect(rankIssues(input).map(x => x.id)).toEqual(['count', 'newer', 'issue-9', 'issue-10', 'lower']);
      expect(input).toEqual(before);
    }
    const legacy = candidates[5];
    expect(rankIssues([{ ...legacy, id: 'issue-10', importance: 0 }, { ...legacy, id: 'issue-9' }]).map(x => x.id))
      .toEqual(['issue-9', 'issue-10']);
  });

  it('화면과 북마크 카드의 순위가 같고 importance는 API 카드에 노출하지 않는다', () => {
    const rows = [1, 2, 3, 4, 5, 6].map(n => ({ ...row('2026-10-07', 'pol', n, 10), importance: n === 6 ? 5 : 0 }));
    const cards = buildRanking('2026-10-07', rows)!.sections[0].issues;
    expect(cards.map(x => x.id)).toEqual([6, 1, 2, 3, 4].map(n => `2026-10-07-pol-${n}`));
    for (const card of cards) {
      expect(findCard(card.id, rows)).toEqual(card);
      expect(card).not.toHaveProperty('importance');
      expect(card).not.toHaveProperty('latestPublishedAt');
    }
  });
});

// D16 비교식을 compareIssues 하나로 뺀 뒤에도 rankIssues(→ 화면 · 북마크 카드 · 요약 대상) 결과가 바뀌지 않음:
// 바꾸기 전 rankIssues의 정렬식을 그대로 옮긴 기준과, 동점이 많이 나오는 무작위 후보 300묶음에서 같은지 봄
describe('D16 비교식 공유 뒤 rankIssues 결과 불변', () => {
  const before = <T extends Candidate>(candidates: T[]) => [...candidates]
    .sort((a, b) =>
      b.articleCount - a.articleCount
      || (b.importance ?? 0) - (a.importance ?? 0)
      || Date.parse(b.latestPublishedAt) - Date.parse(a.latestPublishedAt)
      || a.id.localeCompare(b.id, 'en', { numeric: true }))
    .slice(0, 5)
    .map((c, i) => ({ ...c, rank: i + 1 }));
  it('건수 · 중요도(없음 포함) · 시각(UTC · +09:00 섞음) · 두 자리 id가 겹치는 300묶음에서 바꾸기 전과 같다', () => {
    let seed = 7;
    const rand = (n: number) => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed % n; };
    const TIMES = ['2026-10-07T03:00:00+09:00', '2026-10-06T18:00:00Z', '2026-10-07T05:00:00+09:00', '2026-10-06T20:00:00.000Z'];
    for (let k = 0; k < 300; k++) {
      const list: Candidate[] = Array.from({ length: 1 + rand(9) }, (_, i) => ({
        id: `2026-10-07-pol-${i * 3 + rand(3)}`, articleCount: 1 + rand(3), latestPublishedAt: TIMES[rand(4)],
        ...(rand(3) ? { importance: rand(3) } : {}),
      }));
      expect(rankIssues(list)).toEqual(before(list));
    }
  });
});

// 위젯 목록(D22 · AC-11-1): 랭킹 카드 전부를 섹션 1위들 → 나머지, 각각 D16
describe('buildWidget (D22 · AC-11-1)', () => {
  const D = '2026-10-07';
  const ids = (w: ReturnType<typeof buildWidget>) => w!.items.map(x => x.id);
  const cardIds = (rows: IssueRow[], date = D) => buildRanking(date, rows)!.sections.flatMap(s => s.issues.map(x => x.id));

  it('① 6섹션 × 5: 30개 · 앞 6개 = 섹션 1위(건수 ↓) · id 집합 = 랭킹 카드 · 칸 4개 · path · morePath (⑥ 입력 순서 무관)', () => {
    // 섹션 i(0~5)의 n위 후보 = (6 − n) × 10 + i건 → 같은 n끼리는 섹션 뒤쪽이 많음
    const rows = KEYS.flatMap((k, i) => [1, 2, 3, 4, 5].map(n => row(D, k, n, (6 - n) * 10 + i)));
    const expected = [1, 2, 3, 4, 5].flatMap(n => [...KEYS].reverse().map(k => `${D}-${k}-${n}`));
    for (const input of [rows, [...rows].reverse()]) {
      const widget = buildWidget(D, input)!;
      expect(Object.keys(widget).sort()).toEqual(['date', 'items', 'morePath', 'updatedAt', 'updatedLabel']);
      expect(widget.date).toBe(D);
      expect(widget.morePath).toBe('/#list');
      expect(ids(widget)).toEqual(expected);
      expect(new Set(ids(widget))).toEqual(new Set(cardIds(input)));
      for (const item of widget.items) {
        expect(Object.keys(item).sort()).toEqual(['id', 'path', 'section', 'title']);
        expect(item.path).toBe(`/?issue=${item.id}`);
        const r = input.find(x => x.id === item.id)!;
        expect([item.section, item.title]).toEqual([r.section, r.title]);
      }
    }
  });

  it('② 동점: 1위끼리 · 나머지끼리 건수 같으면 importance ↓ → 시각 ↓(UTC · +09:00 섞음) → id 자연 순서(-9 < -10) (⑥ 입력 순서 무관)', () => {
    const at = (r: IssueRow, importance: number, latestPublishedAt: string): IssueRow => ({ ...r, importance, latestPublishedAt });
    const rows = [
      row(D, 'cul', 1, 60),
      at(row(D, 'eco', 1, 50), 3, `${D}T03:00:00+09:00`),
      at(row(D, 'soc', 1, 50), 2, '2026-10-06T21:00:00Z'), // = 10-07 06:00 KST(문자열로는 가장 이르게 보임)
      at(row(D, 'pol', 1, 50), 2, `${D}T05:00:00+09:00`),
      row(D, 'soc', 2, 20),
      at(row(D, 'eco', 2, 10), 1, `${D}T01:00:00+09:00`),
      at(row(D, 'eco', 3, 10), 1, '2026-10-06T17:00:00Z'), // = 10-07 02:00 KST
      row(D, 'pol', 10, 10),
      row(D, 'pol', 9, 10),
    ];
    const expected = ['cul-1', 'eco-1', 'soc-1', 'pol-1', 'soc-2', 'eco-3', 'eco-2', 'pol-9', 'pol-10'].map(x => `${D}-${x}`);
    for (const input of [rows, [...rows].reverse()]) expect(ids(buildWidget(D, input))).toEqual(expected);
  });

  it('③ pol 2개 · eco 5개 · 나머지 0: 1위 2개(건수 적은 pol 1위도 eco 2위보다 앞) → 나머지 5개', () => {
    const rows = [row(D, 'pol', 1, 5), row(D, 'pol', 2, 3), ...[100, 90, 80, 70, 60].map((n, i) => row(D, 'eco', i + 1, n))];
    expect(ids(buildWidget(D, rows))).toEqual(['eco-1', 'pol-1', 'eco-2', 'eco-3', 'eco-4', 'eco-5', 'pol-2'].map(x => `${D}-${x}`));
  });

  it('④ 그날 행 없음: null', () => {
    expect(buildWidget(D, [row('2026-10-06', 'pol', 1, 10)])).toBeNull();
    expect(buildWidget(D, [])).toBeNull();
  });

  it('⑤ D8: 그날 pol만 있으면 1위 묶음에 전날 대체 섹션 1위 5개(id · path 날짜도 전날) (⑥ 입력 순서 무관)', () => {
    const rows = [
      ...KEYS.map(k => row('2026-10-06', k, 1, 10)),
      ...[60, 50, 40, 30, 20, 10].map((count, i) => row(D, 'pol', i + 1, count)),
    ];
    const expected = [`${D}-pol-1`, ...['cul', 'eco', 'it', 'soc', 'wor'].map(k => `2026-10-06-${k}-1`), ...[2, 3, 4, 5].map(n => `${D}-pol-${n}`)];
    for (const input of [rows, [...rows].reverse()]) {
      const widget = buildWidget(D, input)!;
      expect(widget.date).toBe(D);
      expect(ids(widget)).toEqual(expected);
      expect(widget.items.slice(1, 6).map(x => x.path)).toEqual(expected.slice(1, 6).map(id => `/?issue=${id}`));
      expect(new Set(ids(widget))).toEqual(new Set(cardIds(input)));
    }
  });

  it('⑦ updatedLabel: 화면 날짜 줄과 같은 꼴(0시 → 오전 12시 · 12시 → 오후 12시), updatedAt은 랭킹과 같음', () => {
    expect(updatedLabel('2026-10-17T04:31:12+09:00')).toBe('10.17 오전 4:31 수집');
    expect(updatedLabel('2026-10-17T00:07:00+09:00')).toBe('10.17 오전 12:07 수집');
    expect(updatedLabel('2026-10-17T12:00:00+09:00')).toBe('10.17 오후 12:00 수집');
    expect(updatedLabel('2026-10-17T13:05:00+09:00')).toBe('10.17 오후 1:05 수집');
    const rows = [row(D, 'pol', 1, 10)];
    const runs = [{ status: 'ok', finishedAt: '2026-10-06T19:31:12.345Z' }, { status: 'failed', finishedAt: '2026-10-06T20:00:00Z' }];
    expect(buildWidget(D, rows, runs)).toMatchObject({ updatedAt: '2026-10-07T04:31:12+09:00', updatedLabel: '10.7 오전 4:31 수집' });
    expect(buildWidget(D, rows, runs)!.updatedAt).toBe(buildRanking(D, rows, runs)!.updatedAt);
    expect(buildWidget(D, rows)).toMatchObject({ updatedAt: `${D}T06:00:00+09:00`, updatedLabel: '10.7 오전 6:00 수집' });
  });
});
