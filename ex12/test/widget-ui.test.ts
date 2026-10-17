// 위젯 바로가기(/?issue=<id>, AC-11-2)와 상세창 "수집" 칸(D24): 실제 public/index.html 함수를 잘라 VM에서 가짜 fetch · history · location으로 실행
// 시각은 setup.ts가 2026-10-07 12:00 KST로 고정(VM에도 같은 Date를 넘김)
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';
import { updatedLabel } from '../server/ranking';
import { memoryStore } from '../server/sample';

const html = readFileSync('public/index.html', 'utf8');
const between = (start: string, end: string) => {
  const i = html.indexOf(start), j = html.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`index.html에 없음: ${start}`);
  return html.slice(i, j);
};
const DATA = between('const SECTIONS = [', 'const secColor');
const DATES = between('// KST 오늘 YYYY-MM-DD', '// 크기는 로그 비율');
const RANKING = between('let ranking = null;', '// 첫 진입은');
const ISSUE_BY_ID = between('const issueById =', '/* ─────────────── Icons');
const API = between('async function api(path', '/* ─────────────── Date');
const LOAD = between('async function loadRanking(date)', 'async function loadBookmarks()');
const VIEW = between('const ORDER =', 'function placePill()') + between('const viewFromHash', "addEventListener('popstate'");
const INIT = between('function issueShortcut()', '\nstart();');
const DRAWER = between('function openDrawer(id)', 'function closeDrawer()');

const NOT_FOUND = '이슈를 찾을 수 없습니다';
type Ranking = { date: string; sections: { issues: { id: string; date: string }[] }[] };
const cardsOn = async (date: string) => (await memoryStore.ranking(date) as Ranking).sections.flatMap(s => s.issues.map(x => x.id));

// 첫 로드: 주소 url로 start() 실행. 랭킹 응답은 메모리 저장소(서버와 같은 샘플) 또는 reply(던지면 네트워크 실패), 그 밖의 화면 함수는 호출 기록만
async function boot(url: string, reply?: (date: string | null) => Response) {
  const u = new URL(url, 'http://localhost:3000');
  const calls: string[] = [];
  const location = { pathname: u.pathname, search: u.search, hash: u.hash };
  const history = {
    length: 1, state: null,
    replaceState: vi.fn((_state: unknown, _title: string, next: string) => {
      calls.push(`replaceState ${next}`);
      const n = new URL(next, 'http://localhost:3000');
      Object.assign(location, { pathname: n.pathname, search: n.search, hash: n.hash });
    }),
  };
  const fetchMock = vi.fn(async (path: string, init: { cache?: string }) => {
    calls.push(`fetch ${path} ${init.cache}`);
    const date = new URL(path, 'http://localhost:3000').searchParams.get('date');
    if (reply) return reply(date);
    const ranking = await memoryStore.ranking(date ?? await memoryStore.latestDate() as string);
    return ranking ? Response.json(ranking) : Response.json({ message: '해당 날짜의 랭킹 데이터가 없습니다' }, { status: 404 });
  });
  const log = (name: string) => vi.fn((...args: unknown[]) => { calls.push([name, ...args].join(' ')); });
  const state = { date: '2026-10-07', authVersion: 0 };
  const context = createContext({
    state, location, history, fetch: fetchMock, URLSearchParams, Response, Error, JSON, Promise, Date,
    toast: log('toast'), openDrawer: log('openDrawer'), renderUser: log('renderUser'), renderTabs: log('renderTabs'),
    restoreSession: vi.fn(async () => { calls.push('restoreSession'); }), renderList: log('renderList'),
    renderBookmarks: log('renderBookmarks'), go: log('go'), placePill: log('placePill'), drawMap: log('drawMap'),
    applyUser: log('applyUser'), openAuth: log('openAuth'), document: {},
  });
  runInContext(DATA + DATES + RANKING + ISSUE_BY_ID + API + LOAD + VIEW + INIT, context);
  await runInContext('start()', context);
  const of = (name: string) => calls.filter(c => c.split(' ')[0] === name);
  return { calls, of, state, location, history, ranking: runInContext('ranking', context) as Ranking | null };
}

describe('위젯 바로가기 /?issue=<id> (AC-11-2)', () => {
  it('① 그날 카드: 쿼리를 바로 지움 → ?date=그날 1회 → 로그인 확인 · 화면 그리기 뒤 상세창 1회, 토스트 0', async () => {
    const id = (await cardsOn('2026-10-07'))[7];
    const r = await boot(`/?issue=${id}`);
    expect(r.calls).toEqual([
      'renderUser', 'renderTabs', 'replaceState /', 'fetch /api/rankings?date=2026-10-07 no-store', 'restoreSession',
      'renderList', 'renderBookmarks', 'go map false', `openDrawer ${id}`,
    ]);
    expect(r.state.date).toBe('2026-10-07');
    expect(r.location.search).toBe(''); // 가짜 history에는 pushState가 없음(부르면 실패) → 기록 수는 브라우저 줄(13 ①)에서 확인
  });

  it('② 지난 날 카드 · 해시: 날짜도 그날 · 해시 그대로(#list) · 상세창', async () => {
    const id = (await cardsOn('2026-10-05'))[0];
    const r = await boot(`/?issue=${id}#list`);
    expect(r.of('replaceState')).toEqual(['replaceState /#list']);
    expect(r.of('fetch')).toEqual(['fetch /api/rankings?date=2026-10-05 no-store']);
    expect(r.of('go')).toEqual(['go list false']);
    expect(r.of('openDrawer')).toEqual([`openDrawer ${id}`]);
    expect(r.of('toast')).toEqual([]);
    expect([r.state.date, r.ranking!.date]).toEqual(['2026-10-05', '2026-10-05']);
    expect(r.location.hash).toBe('#list');
  });

  it('③ 형식 틀림 · 없는 날 · 미래 · 없는 섹션 · 빈 값: 날짜 없는 랭킹 1회, 상세창 0, 토스트 하나(값은 화면에 쓰지 않음)', async () => {
    for (const value of ['abc', '2026-02-30-pol-1', '2026-10-08-pol-1', '2026-10-07-xyz-1', '2026-10-07-constructor-1', '', '<b>2026-10-07-pol-1</b>']) {
      const r = await boot(`/?issue=${encodeURIComponent(value)}`);
      expect(r.of('replaceState')).toEqual(['replaceState /']);
      expect(r.of('fetch')).toEqual(['fetch /api/rankings no-store']);
      expect(r.of('openDrawer')).toEqual([]);
      expect(r.of('toast')).toEqual([`toast ${NOT_FOUND}`]);
      expect(r.state.date).toBe('2026-10-07');
    }
  });

  it('④ 그 날짜 랭킹 없음(404): 그 날짜 · 빈 화면 · 상세창 0 · 같은 토스트 하나', async () => {
    const r = await boot('/?issue=2026-08-01-pol-1');
    expect(r.of('fetch')).toEqual(['fetch /api/rankings?date=2026-08-01 no-store']);
    expect([r.state.date, r.ranking]).toEqual(['2026-08-01', null]);
    expect(r.of('openDrawer')).toEqual([]);
    expect(r.of('toast')).toEqual([`toast ${NOT_FOUND}`]);
  });

  it('⑤ 그날 랭킹에 없는 id(Top 5 밖 · 없는 번호): 그날 랭킹 · 상세창 0 · 같은 토스트', async () => {
    const r = await boot('/?issue=2026-10-07-pol-99999');
    expect(r.of('fetch')).toEqual(['fetch /api/rankings?date=2026-10-07 no-store']);
    expect(r.state.date).toBe('2026-10-07');
    expect(r.of('openDrawer')).toEqual([]);
    expect(r.of('toast')).toEqual([`toast ${NOT_FOUND}`]);
  });

  it('D25: 바로가기 날짜 랭킹이 500 · 502 · 네트워크 실패면 기존 오류 토스트 그대로(덮지 않음), 200(id 없음) · 404만 "이슈를 찾을 수 없습니다"', async () => {
    const id = (await cardsOn('2026-10-07'))[0];
    const failures: [string, () => Response, string][] = [
      ['500', () => new Response('Internal Server Error', { status: 500, statusText: 'Internal Server Error' }), 'Internal Server Error'],
      ['502', () => Response.json({ message: 'API 서버에 연결할 수 없습니다' }, { status: 502 }), 'API 서버에 연결할 수 없습니다'],
      ['네트워크 실패', () => { throw new TypeError('Failed to fetch'); }, 'Failed to fetch'],
    ];
    for (const [label, reply, message] of failures) {
      const r = await boot(`/?issue=${id}`, reply);
      expect(r.of('fetch'), label).toEqual(['fetch /api/rankings?date=2026-10-07 no-store']);
      expect(r.of('toast'), label).toEqual([`toast ${message}`]);
      expect(r.of('openDrawer'), label).toEqual([]);
      expect([r.state.date, r.ranking], label).toEqual(['2026-10-07', null]);
      expect(r.of('replaceState'), label).toEqual(['replaceState /']);
    }
    // 형식 틀림인데 최근 날짜 요청도 실패: 같은 규칙으로 서버 오류 토스트만
    const bad = await boot('/?issue=abc', () => Response.json({ message: 'API 서버에 연결할 수 없습니다' }, { status: 502 }));
    expect(bad.of('fetch')).toEqual(['fetch /api/rankings no-store']);
    expect(bad.of('toast')).toEqual(['toast API 서버에 연결할 수 없습니다']);
    // 200(id 없음) · 404는 ④ ⑤처럼 "이슈를 찾을 수 없습니다" 하나
    const today = await memoryStore.ranking('2026-10-07');
    const answered: [string, () => Response][] = [
      ['/?issue=2026-10-07-pol-99999', () => Response.json(today)],
      [`/?issue=${id}`, () => Response.json({ message: '해당 날짜의 랭킹 데이터가 없습니다' }, { status: 404 })],
    ];
    for (const [url, reply] of answered) {
      const r = await boot(url, reply);
      expect(r.of('toast'), url).toEqual([`toast ${NOT_FOUND}`]);
      expect(r.of('openDrawer'), url).toEqual([]);
    }
  });

  it('⑥ 쿼리 없음 · #list만: 지금과 같음(replaceState 0 · 토스트 0 · 날짜 없는 랭킹)', async () => {
    for (const url of ['/', '/#list']) {
      const r = await boot(url);
      expect(r.calls).toEqual([
        'renderUser', 'renderTabs', 'fetch /api/rankings no-store', 'restoreSession', 'renderList', 'renderBookmarks',
        `go ${url === '/' ? 'map' : 'list'} false`,
      ]);
    }
  });
});

describe('상세창 "수집" 칸 (D24)', () => {
  // openDrawer를 잘라 가짜 랭킹(10-17)과 같은 날 카드 · D8 대체 카드(10-16)로 엶
  function drawerWith(updatedAt: string) {
    const drawer = { innerHTML: '', classList: { add: vi.fn() }, setAttribute: vi.fn() };
    const context = createContext({
      state: { selected: null }, drawer, scrim: { classList: { add: vi.fn() } }, tip: { classList: { remove: vi.fn() } },
      drawMap: vi.fn(), findBookmark: () => null, ratio: () => 0.5, rankMax: () => 100, secColor: (k: string) => `var(--s-${k})`,
      ICON_X: '', ICON_EXT: '', ICON_BM: '', Date,
    });
    runInContext(DATA + DATES + between('const esc =', 'const issueById') + RANKING + ISSUE_BY_ID
      + between('const summaryHTML =', '/* ─────────────── Shared item renderer') + DRAWER, context);
    const card = (id: string, date: string, section: string) => ({ id, date, section, rank: 1, title: id, summary: null, articleCount: 10, url: 'https://example.com/' });
    runInContext(`ranking = ${JSON.stringify({ date: '2026-10-17', updatedAt, sections: [
      { key: 'pol', name: '정치', issues: [card('2026-10-17-pol-1', '2026-10-17', 'pol')] },
      { key: 'eco', name: '경제', issues: [card('2026-10-16-eco-1', '2026-10-16', 'eco')] },
    ] })}`, context);
    const collected = (id: string) => {
      runInContext(`openDrawer(${JSON.stringify(id)})`, context);
      return /<dt>수집<\/dt><dd>([^<]*)<\/dd>/.exec(drawer.innerHTML)![1];
    };
    return { same: collected('2026-10-17-pol-1'), fallback: collected('2026-10-16-eco-1') };
  }

  it('같은 날 카드 = 날짜 줄과 같은 실제 수집 완료 시각(= 위젯 updatedLabel에서 " 수집"을 뺀 값), 대체 섹션 카드 = 그 날짜만, 고정 "오전 6:00" 없음', () => {
    for (const [updatedAt, expected] of [
      ['2026-10-17T04:31:12+09:00', '10.17 오전 4:31'],
      ['2026-10-17T13:05:00+09:00', '10.17 오후 1:05'],
      ['2026-10-17T00:07:00+09:00', '10.17 오전 12:07'],
    ]) {
      const { same, fallback } = drawerWith(updatedAt);
      expect(same).toBe(expected);
      expect(same).toBe(updatedLabel(updatedAt).replace(/ 수집$/, ''));
      expect(fallback).toBe('10.16');
    }
    expect(DRAWER).not.toContain('오전 6:00');
  });
});
