// 인스타 카드 데이터 · 캡션 · 라우트 처리기(O3 · D28 · AC-12-1~4). 저장소는 메모리 샘플(시각은 setup에서 2026-10-07 12:00 KST)
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Store } from '../server/app';
import { buildCard, captionResponse, cardCaption, cardTitle, createCardHandler, dateLabel, parseCardQuery, type Card } from '../server/card';
import { cardPngResponse } from '../server/card-image';
import { SECTIONS, type Widget } from '../server/ranking';
import { memoryStore } from '../server/sample';

const DATE = '2026-10-07';
const CACHE = 'public, max-age=0, s-maxage=300';
const MSG = {
  badRequest: '요청 형식이 올바르지 않습니다',
  dateFormat: '날짜는 YYYY-MM-DD 형식으로 입력해 주세요',
  dateFuture: '오늘 이후 날짜는 조회할 수 없습니다',
  noRanking: '해당 날짜의 랭킹 데이터가 없습니다',
  unavailable: 'API 서버에 연결할 수 없습니다',
  render: '카드를 만들 수 없습니다',
};

const widgetOn = async (date = DATE) => (await memoryStore.widget(date))!;
const ids = (items: { id: string }[]) => items.map(it => it.id);
const query = (s: string) => parseCardQuery(new URLSearchParams(s));

afterEach(() => { vi.restoreAllMocks(); });

describe('카드 데이터 buildCard (AC-12-1 · AC-12-4)', () => {
  it('① 그대로: buildWidget 앞 6개(id · 순서 · 섹션 · 원문 제목), date · updatedLabel = 위젯', async () => {
    const w = await widgetOn();
    const card = buildCard(w, [])!;
    expect(ids(card.items)).toEqual(ids(w.items.slice(0, 6)));
    expect(card.items).toEqual(w.items.slice(0, 6).map(({ id, section, title }) => ({ id, section, title })));
    expect(card).toMatchObject({ date: DATE, dateLabel: '10월 7일 수요일', updatedLabel: w.updatedLabel, hidden: [] });
    expect(Object.keys(card).sort()).toEqual(['date', 'dateLabel', 'hidden', 'items', 'updatedLabel']);
  });

  it('② hide 1개 · 3개: 뺀 id는 없고 다음 항목으로 6개, hidden = 보낸 순서', async () => {
    const w = await widgetOn();
    const one = buildCard(w, [w.items[0].id])!;
    expect(ids(one.items)).toEqual(ids(w.items.slice(1, 7)));
    expect(one.hidden).toEqual([w.items[0].id]);
    const hide = [w.items[4].id, w.items[1].id, w.items[7].id];
    const three = buildCard(w, hide)!;
    expect(ids(three.items)).toEqual(ids(w.items.filter(it => !hide.includes(it.id)).slice(0, 6)));
    expect(ids(three.items)).toEqual([0, 2, 3, 5, 6, 8].map(i => w.items[i].id));
    expect(three.hidden).toEqual(hide);
  });

  it('③ hide 4개 · 중복 → 400 문구, 그날 목록에 없는 id → null(400)', async () => {
    const w = await widgetOn();
    expect(query(`hide=${ids(w.items.slice(0, 4)).join(',')}`)).toEqual({ ok: false, message: MSG.badRequest });
    expect(query(`hide=${w.items[0].id},${w.items[0].id}`)).toEqual({ ok: false, message: MSG.badRequest });
    expect(buildCard(w, ['2026-10-06-pol-0'])).toBeNull();
    expect(buildCard(w, [w.items[0].id, 'nope'])).toBeNull();
    expect(query(`hide=${ids(w.items.slice(0, 3)).join(',')}`)).toEqual({ ok: true, date: undefined, hide: ids(w.items.slice(0, 3)) });
    expect(query('')).toEqual({ ok: true, date: undefined, hide: [] });
  });

  it('④ 항목이 3개뿐이면 3개, 다 빼면 null', async () => {
    const w = await widgetOn();
    const small: Widget = { ...w, items: w.items.slice(0, 3) };
    expect(ids(buildCard(small, [])!.items)).toEqual(ids(small.items));
    expect(ids(buildCard(small, [small.items[1].id])!.items)).toEqual([small.items[0].id, small.items[2].id]);
    expect(buildCard(small, ids(small.items))).toBeNull();
  });

  it('⑤ 제목 다듬기: 33 · 34자 그대로, 35자 → 33자 + …, 공백 하나로, 그림 문자 0, 한자 그대로', () => {
    const t33 = '가'.repeat(33), t34 = '나'.repeat(34), t35 = '다'.repeat(34) + '라';
    expect(cardTitle(t33)).toBe(t33);
    expect(cardTitle(t34)).toBe(t34);
    expect(cardTitle(t35)).toBe(`${'다'.repeat(33)}…`);
    expect([...cardTitle(t35)]).toHaveLength(34);
    expect(cardTitle('  美 대통령,\t\n中   방문  ')).toBe('美 대통령, 中 방문');
    expect(cardTitle('속보🔥 日 총리 👍🏽 회담 🇰🇷 ❤️ 결과 👨‍👩‍👧')).toBe('속보 日 총리 회담 결과');
    expect(cardTitle('“北 발사” ‘與野’ 합의…尹·李')).toBe('“北 발사” ‘與野’ 합의…尹·李');
    expect(/\p{Extended_Pictographic}/u.test(cardTitle('A😀B✅C'))).toBe(false);
    // 자른 끝이 공백이면 지우고 … (사용자 결정 2026-10-19). 34자 이하는 끝이 공백이어도(다듬기 뒤) 그대로
    const spaced = `${'가'.repeat(32)} 입장 발표`; // 33번째 = 공백
    expect(cardTitle(spaced)).toBe(`${'가'.repeat(32)}…`);
    expect(cardTitle(`${'가'.repeat(31)}  입장 발표 예정`)).toBe(`${'가'.repeat(31)} 입…`); // 공백은 하나로 정리된 뒤 자름
    expect(cardTitle(`${'나'.repeat(33)} 다`)).toBe(`${'나'.repeat(33)}…`); // 35자 → 33자(끝 공백 없음) + …
    expect(cardTitle(`${'나'.repeat(32)} 다`)).toBe(`${'나'.repeat(32)} 다`); // 34자 그대로
    expect(cardTitle(`${'나'.repeat(33)} `)).toBe('나'.repeat(33)); // 다듬기로 33자 → 그대로
    // 코드 포인트로 셈(서로게이트 쌍 한자 1자)
    expect(cardTitle('𠀀'.repeat(35))).toBe(`${'𠀀'.repeat(33)}…`);
  });

  it('⑥ dateLabel', () => {
    expect(dateLabel('2026-10-07')).toBe('10월 7일 수요일');
    expect(dateLabel('2026-10-19')).toBe('10월 19일 월요일');
    expect(dateLabel('2026-01-01')).toBe('1월 1일 목요일');
  });
});

describe('캡션 cardCaption (AC-12-2)', () => {
  const card: Card = {
    date: '2026-10-19', dateLabel: '10월 19일 월요일', updatedLabel: '10.19 오전 6:34 수집', hidden: [],
    items: [
      { id: '2026-10-19-pol-1', section: 'pol', title: '여야, 예산안 처리 합의' },
      { id: '2026-10-19-eco-2', section: 'eco', title: '美 금리 동결 🔥 시장 반응' },
      { id: '2026-10-19-soc-3', section: 'soc', title: '수도권 첫 한파특보' },
      { id: '2026-10-19-cul-4', section: 'cul', title: '가을 단풍 절정' },
      { id: '2026-10-19-wor-5', section: 'wor', title: '中 수출 통계 발표' },
      { id: '2026-10-19-it-6', section: 'it', title: 'AI 반도체 수출 증가' },
    ],
  };

  it('고정 입력의 캡션 전문 = D28 꼴(한 글자도 다르지 않음)', () => {
    expect(cardCaption(card)).toBe([
      '10월 19일 오늘의 이슈 — 어제 가장 많이 보도된 뉴스',
      '',
      '1. [정치] 여야, 예산안 처리 합의',
      '2. [경제] 美 금리 동결 시장 반응',
      '3. [사회] 수도권 첫 한파특보',
      '4. [생활·문화] 가을 단풍 절정',
      '5. [세계] 中 수출 통계 발표',
      '6. [IT·과학] AI 반도체 수출 증가',
      '',
      '섹션별 순위 · 3줄 요약 · 원문 링크는 프로필 링크의 이슈랭크에서 볼 수 있어요.',
      '기사 제목 출처: 각 언론사(newsdata.io 수집). 순위는 연관기사 수 기준 참고 지표입니다.',
      '',
      '#오늘의이슈 #뉴스 #헤드라인 #시사 #이슈랭크',
    ].join('\n'));
  });

  it('번호 1~6 = items 순서 · 섹션 이름 = SECTIONS · 긴 제목 99자 + … · ≤ 2,200자 · # 5개 · URL · 연관기사 수 0', async () => {
    const long: Card = { ...card, items: card.items.map((it, i) => ({ ...it, title: `${i}${'긴'.repeat(149)}` })) };
    const text = cardCaption(long);
    const lines = text.split('\n').slice(2, 8);
    lines.forEach((line, i) => {
      const name = SECTIONS.find(s => s.key === long.items[i].section)!.name;
      expect(line).toBe(`${i + 1}. [${name}] ${i}${'긴'.repeat(98)}…`);
    });
    expect([...text].length).toBeLessThanOrEqual(2200);
    expect(text.match(/#/g)).toHaveLength(5);
    // 캡션도 자른 끝 공백을 지움: 99번째가 공백인 제목 → 98자 + …
    const spaced: Card = { ...card, items: [{ ...card.items[0], title: `${'긴'.repeat(98)} 끝말 이어짐` }] };
    expect(cardCaption(spaced).split('\n')[2]).toBe(`1. [정치] ${'긴'.repeat(98)}…`);
    const exact: Card = { ...card, items: [{ ...card.items[0], title: '긴'.repeat(100) }] };
    expect(cardCaption(exact).split('\n')[2]).toBe(`1. [정치] ${'긴'.repeat(100)}`);
    expect(text).not.toMatch(/https?:|www\.|vercel\.app/);
    const w = await widgetOn();
    const real = cardCaption(buildCard(w, [])!);
    expect(real).not.toMatch(/\d+건/);
    expect([...real].length).toBeLessThanOrEqual(2200);
  });
});

/* ─── 라우트 처리기(AC-12-3): 실제 라우트와 같은 respond, 저장소만 메모리 ─── */
const fakePng = async () => new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
const noDataOn = (date: string): Store => ({ ...memoryStore, widget: async d => (d === date ? null : memoryStore.widget(d)) });
const routes = (store: Store | (() => never) = memoryStore) => {
  const load = vi.fn(typeof store === 'function' ? store : () => store);
  return {
    load,
    caption: createCardHandler(load, captionResponse),
    card: createCardHandler(load, card => cardPngResponse(card, fakePng)),
  };
};
const get = (handle: (req: Request) => Promise<Response>, path: string) => handle(new Request(`http://localhost:3000${path}`));

describe('GET /api/card/caption · /api/card 처리기 (AC-12-3)', () => {
  it('caption 200: 칸 = 표 · items = 위젯 앞 6개 · 캐시 헤더 · Set-Cookie 0 (date 생략 = 최근 수집일)', async () => {
    const { caption } = routes(noDataOn('2026-10-01'));
    for (const path of ['/api/card/caption', `/api/card/caption?date=${DATE}`]) {
      const res = await get(caption, path);
      expect(res.status).toBe(200);
      expect(res.headers.get('cache-control')).toBe(CACHE);
      expect(res.headers.get('set-cookie')).toBeNull();
      const body = await res.json();
      expect(Object.keys(body).sort()).toEqual(['caption', 'date', 'dateLabel', 'hidden', 'items', 'updatedLabel']);
      const w = await widgetOn();
      expect(body.date).toBe(DATE);
      expect(body.items).toEqual(w.items.slice(0, 6).map(({ id, section, title }) => ({ id, section, title })));
      expect(body.caption).toBe(cardCaption(buildCard(w, [])!));
    }
  });

  it('caption · card hide: 같은 6개 · hidden', async () => {
    const { caption, card } = routes();
    const w = await widgetOn();
    const hide = [w.items[2].id, w.items[0].id];
    const body = await (await get(caption, `/api/card/caption?date=${DATE}&hide=${hide.join(',')}`)).json();
    expect(ids(body.items)).toEqual(ids(buildCard(w, hide)!.items));
    expect(body.hidden).toEqual(hide);
    const res = await get(card, `/api/card?date=${DATE}&hide=${hide.join(',')}`);
    expect(res.status).toBe(200);
  });

  it('card 200: image/png · 캐시 헤더 · Set-Cookie 0', async () => {
    const { card } = routes();
    const res = await get(card, '/api/card');
    expect(res.status).toBe(200);
    expect(res.headers.get('content-type')).toBe('image/png');
    expect(res.headers.get('cache-control')).toBe(CACHE);
    expect(res.headers.get('set-cookie')).toBeNull();
    expect([...new Uint8Array(await res.arrayBuffer())]).toEqual([137, 80, 78, 71, 13, 10, 26, 10]);
  });

  it.each([
    ['?date=2026-10-08', 400, MSG.dateFuture],
    ['?date=2026-1-7', 400, MSG.dateFormat],
    ['?date=', 400, MSG.dateFormat],
    ['?date=2026-10-01', 404, MSG.noRanking],
    ['?hide=a,b,c,d', 400, MSG.badRequest],
    ['?hide=2026-10-07-pol-0,2026-10-07-pol-0', 400, MSG.badRequest],
    ['?hide=2026-10-06-pol-0', 400, MSG.badRequest],
  ])('%s → %i 문구 · 캐시 헤더 없음 (caption · card 같음)', async (q, status, message) => {
    const { caption, card } = routes(noDataOn('2026-10-01'));
    for (const [handle, base] of [[caption, '/api/card/caption'], [card, '/api/card']] as const) {
      const res = await get(handle, base + q);
      expect(res.status).toBe(status);
      expect(await res.json()).toEqual({ message });
      expect(res.headers.get('cache-control')).toBeNull();
      expect(res.headers.get('set-cookie')).toBeNull();
    }
  });

  it('형식 400은 저장소를 열지 않음', async () => {
    const r = routes();
    await get(r.caption, '/api/card/caption?date=2026-1-7');
    await get(r.card, '/api/card?hide=a,b,c,d');
    expect(r.load).not.toHaveBeenCalled();
  });

  it('최근 수집일이 없으면 404', async () => {
    const { caption, card } = routes({ ...memoryStore, latestDate: async () => null });
    for (const [handle, path] of [[caption, '/api/card/caption'], [card, '/api/card']] as const) {
      const res = await get(handle, path);
      expect(res.status).toBe(404);
      expect(await res.json()).toEqual({ message: MSG.noRanking });
    }
  });

  it('저장소 준비 실패 → 502 문구 · 로그 CARD_START_FAILED만, 다음 요청이 다시 준비', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    let calls = 0;
    const load = vi.fn(() => { calls++; if (calls === 1) throw new Error('postgres://secret@host'); return memoryStore; });
    const handle = createCardHandler(load, captionResponse);
    const res = await get(handle, '/api/card/caption');
    expect(res.status).toBe(502);
    expect(await res.json()).toEqual({ message: MSG.unavailable });
    expect(res.headers.get('cache-control')).toBeNull();
    expect(log.mock.calls).toEqual([['CARD_START_FAILED']]);
    expect((await get(handle, '/api/card/caption')).status).toBe(200);
    expect((await get(handle, '/api/card/caption')).status).toBe(200);
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('조회 실패(DB 접속은 첫 조회 때) → 502 · 안전 로그', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    const { card } = routes({ ...memoryStore, widget: async () => { throw new Error('ECONNREFUSED secret'); } });
    const res = await get(card, '/api/card');
    expect(res.status).toBe(502);
    expect(await res.json()).toEqual({ message: MSG.unavailable });
    expect(log.mock.calls).toEqual([['CARD_START_FAILED']]);
  });

  it('렌더 실패 → 500 카드를 만들 수 없습니다 · 캐시 헤더 없음 · 로그 CARD_RENDER_FAILED만', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    const handle = createCardHandler(() => memoryStore, card => cardPngResponse(card, async () => { throw new Error('satori stack'); }));
    const res = await get(handle, '/api/card');
    expect(res.status).toBe(500);
    expect(await res.json()).toEqual({ message: MSG.render });
    expect(res.headers.get('cache-control')).toBeNull();
    expect(log.mock.calls).toEqual([['CARD_RENDER_FAILED']]);
  });
});
