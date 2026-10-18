// Google 로그인 화면(O2, AC-06-3 · 06-4 · 07-3): 실제 public/index.html 함수를 잘라 VM에서 가짜 fetch · location · history · toast · openAuth로 실행
// 시각은 setup.ts가 2026-10-07 12:00 KST로 고정(VM에도 같은 Date를 넘김)
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';
import { memoryStore } from '../server/sample';

const html = readFileSync('public/index.html', 'utf8');
const between = (start: string, end: string) => {
  const i = html.indexOf(start), j = html.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`index.html에 없음: ${start}`);
  return html.slice(i, j);
};
const AUTH = between('/* 세션만 메모리에 보관한다.', '/* ─────────────── Events');
const EVENTS = between('/* ─────────────── Events', 'let raf;');
const DATA = between('const SECTIONS = [', 'const secColor');
const DATES = between('// KST 오늘 YYYY-MM-DD', '// 크기는 로그 비율');
const RANKING = between('let ranking = null;', '// 첫 진입은');
const ISSUE_BY_ID = between('const issueById =', '/* ─────────────── Icons');
const API = between('async function api(path', '/* ─────────────── Date');
const LOAD = between('async function loadRanking(date)', 'async function loadBookmarks()');
const VIEW = between('const ORDER =', 'function placePill()') + between('const viewFromHash', "addEventListener('popstate'");
const INIT = between('function issueShortcut()', '\nstart();');

const GOOGLE_URL = 'https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=o2-test.apps.googleusercontent.com&state=s';
const GENERIC = '인증 요청을 처리하지 못했습니다. 다시 시도해 주세요';
const settle = async () => { for (let i = 0; i < 6; i++) await new Promise(r => setTimeout(r, 0)); };

// ── 로그인 창(AUTH)과 문서 클릭(EVENTS) 코드를 가짜 DOM으로 실행 ──
type Listener = (event: unknown) => unknown;
function element(id: string) {
  const listeners: Record<string, Listener[]> = {};
  return {
    id, textContent: '', disabled: false, hidden: false, value: '', required: false, autocomplete: '',
    addEventListener(type: string, fn: Listener) { (listeners[type] ??= []).push(fn); },
    fire(type: string, event: unknown = {}) { for (const fn of listeners[type] ?? []) fn(event); },
    showModal: vi.fn(), close: vi.fn(), focus: vi.fn(),
  };
}
type Reply = (path: string, init: RequestInit) => Response | Promise<Response>;
function dialog(hash: string, reply: Reply) {
  const ids = ['authDialog', 'authForm', 'authTitle', 'authSubmit', 'authSwitch', 'authClose', 'authGoogle', 'authNameLabel', 'authError', 'userBtn', 'bmUser'];
  const els = Object.fromEntries(ids.map(id => [id, element(id)])) as Record<string, ReturnType<typeof element>>;
  const fields = { email: element('email'), password: element('password'), name: element('name') };
  const buttons = [els.authGoogle, els.authSubmit, els.authSwitch, els.authClose];
  Object.assign(els.authForm, { elements: fields, querySelectorAll: (selector: string) => (selector === 'button' ? buttons : []) });
  const documentListeners: Record<string, Listener[]> = {}, windowListeners: Record<string, Listener[]> = {};
  const requests: { path: string; method?: string; credentials?: string; cache?: string; contentType?: string; body: Record<string, unknown> }[] = [];
  const fetchMock = vi.fn(async (path: string, init: RequestInit = {}) => {
    requests.push({ path, method: init.method, credentials: init.credentials, cache: init.cache, contentType: (init.headers as Record<string, string> | undefined)?.['Content-Type'], body: JSON.parse(String(init.body)) });
    return reply(path, init);
  });
  const toast = vi.fn();
  const location = { pathname: '/', search: '', hash, assign: vi.fn() };
  const state = { loggedIn: false, user: null, authVersion: 0, view: 'map', folders: [], bookmarks: [] };
  const context = createContext({
    state, location, fetch: fetchMock, toast, Response, Error, JSON, Promise, scrim: {}, popFor: null,
    document: { getElementById: (id: string) => els[id] ?? null, addEventListener: (type: string, fn: Listener) => { (documentListeners[type] ??= []).push(fn); } },
    addEventListener: (type: string, fn: Listener) => { (windowListeners[type] ??= []).push(fn); },
    closePop: vi.fn(), closeDrawer: vi.fn(), renderUser: vi.fn(), syncBookmarkUI: vi.fn(), renderBookmarks: vi.fn(), loadBookmarks: vi.fn(), findBookmark: () => null,
  });
  runInContext(AUTH + EVENTS, context);
  // 문서 클릭: target.closest(selector)는 그 선택자에만 요소를 돌려줌(실제 이벤트 위임 코드를 그대로 지남)
  const click = (selector: string, target: unknown) => { for (const fn of documentListeners.click ?? []) fn({ target: { closest: (s: string) => (s === selector ? target : null) } }); };
  const pageshow = (persisted: boolean) => { for (const fn of windowListeners.pageshow ?? []) fn({ persisted }); };
  return { els, fields, buttons, requests, toast, location, click, pageshow, busy: () => runInContext('authBusy', context) as boolean };
}
const googleOk: Reply = () => Response.json({ url: GOOGLE_URL, redirect: true });

describe('로그인 창 "Google로 계속하기"(O2)', () => {
  it('① 헤더 · 북마크 탭 "로그인" · 비로그인 북마크 안내 토스트 "로그인"으로 연 창 × 해시 없음 · #list · #bookmarks: 시작 본문 3칸 · Google로 이동 1번 · 잠금 유지', async () => {
    const issue = '2026-10-07-pol-1';
    for (const hash of ['', '#list', '#bookmarks']) for (const opener of ['header', 'loginTab', 'bookmarkToast'] as const) {
      const label = `${opener} ${hash || '(해시 없음)'}`;
      const d = dialog(hash, googleOk);
      if (opener === 'header') d.click('#userBtn', d.els.userBtn);
      else if (opener === 'loginTab') d.click('[data-login]', {});
      else {
        d.click('[data-bm]', { dataset: { bm: issue } });
        expect(d.toast.mock.calls, label).toEqual([['북마크하려면 로그인이 필요합니다', '로그인', expect.any(Function)]]);
        d.toast.mock.calls[0][2]();
      }
      expect(d.els.authDialog.showModal, label).toHaveBeenCalledTimes(1);
      expect(d.fields.email.focus, label).toHaveBeenCalledTimes(1); // 첫 초점 = 이메일 칸
      d.els.authGoogle.fire('click');
      await settle();
      const back = '/?login=google' + (opener === 'bookmarkToast' ? `&issue=${issue}` : '') + hash;
      expect(d.requests, label).toEqual([{ path: '/api/auth/sign-in/social', method: 'POST', credentials: 'same-origin', cache: 'no-store', contentType: 'application/json', body: { provider: 'google', callbackURL: back, errorCallbackURL: back } }]);
      expect(Object.keys(d.requests[0].body), label).toEqual(['provider', 'callbackURL', 'errorCallbackURL']); // 다른 칸은 보내지 않음
      expect(d.location.assign.mock.calls, label).toEqual([[GOOGLE_URL]]);
      expect(d.buttons.every(b => b.disabled), label).toBe(true); // 이동하는 동안 창의 버튼 잠금 유지
      expect(d.busy(), label).toBe(true);
      expect(d.els.authError.textContent, label).toBe('');
    }
  });

  it('① 회원가입 모드에서도 같은 버튼 · 같은 본문, 요청 중 다시 눌러도 요청 1번', async () => {
    let release!: () => void;
    const d = dialog('#bookmarks', () => new Promise<Response>(resolve => { release = () => resolve(googleOk('', {}) as Response); }));
    d.click('[data-login]', {});
    d.els.authSwitch.fire('click'); // 회원가입 모드
    expect(d.els.authTitle.textContent).toBe('회원가입');
    d.els.authGoogle.fire('click');
    await settle();
    expect(d.buttons.every(b => b.disabled)).toBe(true);
    d.els.authGoogle.fire('click');
    await settle();
    expect(d.requests).toHaveLength(1);
    release();
    await settle();
    expect(d.requests[0].body).toEqual({ provider: 'google', callbackURL: '/?login=google#bookmarks', errorCallbackURL: '/?login=google#bookmarks' });
    expect(d.location.assign).toHaveBeenCalledTimes(1);
  });

  it('② 시작 실패(404 PROVIDER_NOT_FOUND · 429 · 500 · 502 · 네트워크 · url이 Google 주소 아님): 표의 문구 · 이동 0 · 잠금 풀림', async () => {
    const cases: [string, Reply, string][] = [
      ['404', () => Response.json({ code: 'PROVIDER_NOT_FOUND', message: 'Provider not found' }, { status: 404 }), '지금은 Google 로그인을 사용할 수 없습니다'],
      ['429', () => Response.json({ message: 'Too many requests. Please try again later.' }, { status: 429 }), '잠시 후 다시 시도해 주세요'],
      ['500', () => new Response('Internal Server Error', { status: 500 }), GENERIC],
      ['502', () => Response.json({ message: 'API 서버에 연결할 수 없습니다' }, { status: 502 }), GENERIC],
      ['403', () => Response.json({ code: 'INVALID_CALLBACK_URL' }, { status: 403 }), GENERIC],
      ['네트워크 실패', () => { throw new TypeError('Failed to fetch'); }, GENERIC],
      ['evil.example', () => Response.json({ url: 'https://evil.example/o/oauth2/v2/auth', redirect: true }), GENERIC],
      ['비슷한 호스트', () => Response.json({ url: 'https://accounts.google.com.evil.example/x', redirect: true }), GENERIC],
      ['url 없음', () => Response.json({ redirect: true }), GENERIC],
    ];
    for (const [label, reply, message] of cases) {
      const d = dialog('', reply);
      d.click('#userBtn', d.els.userBtn);
      d.els.authGoogle.fire('click');
      await settle();
      expect(d.els.authError.textContent, label).toBe(message);
      expect(d.location.assign, label).not.toHaveBeenCalled();
      expect(d.buttons.every(b => !b.disabled), label).toBe(true);
      expect(d.busy(), label).toBe(false);
    }
  });

  it('④ Google 화면에서 뒤로가기로 페이지가 그대로 복원되면(pageshow persisted) 잠금을 풀고 다시 누를 수 있음', async () => {
    const d = dialog('', googleOk);
    d.click('#userBtn', d.els.userBtn);
    d.els.authGoogle.fire('click');
    await settle();
    expect(d.busy()).toBe(true);
    d.pageshow(false); // 새로 연 페이지(복원 아님)는 그대로
    expect([d.busy(), d.buttons.every(b => b.disabled)]).toEqual([true, true]);
    d.pageshow(true);
    expect([d.busy(), d.buttons.some(b => b.disabled)]).toEqual([false, false]);
    d.els.authGoogle.fire('click');
    await settle();
    expect(d.requests).toHaveLength(2);
    expect(d.location.assign).toHaveBeenCalledTimes(2);
  });
});

// ── 첫 로드(Google에서 돌아옴): 실제 start()를 가짜 fetch · history · location으로 실행(위젯 바로가기 테스트와 같은 방식) ──
async function boot(url: string, session: boolean) {
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
  const fetchMock = vi.fn(async (path: string) => {
    calls.push(`fetch ${path}`);
    const date = new URL(path, 'http://localhost:3000').searchParams.get('date');
    const ranking = await memoryStore.ranking(date ?? await memoryStore.latestDate() as string);
    return ranking ? Response.json(ranking) : Response.json({ message: '해당 날짜의 랭킹 데이터가 없습니다' }, { status: 404 });
  });
  const log = (name: string) => vi.fn((...args: unknown[]) => { calls.push([name, ...args].join(' ')); });
  const authError = { textContent: '' };
  const state = { date: '2026-10-07', authVersion: 0, loggedIn: false, user: null as null | { id: string; name: string } };
  const context = createContext({
    state, location, history, fetch: fetchMock, URLSearchParams, Response, Error, JSON, Promise, Date,
    toast: log('toast'), openDrawer: log('openDrawer'), renderUser: log('renderUser'), renderTabs: log('renderTabs'),
    restoreSession: vi.fn(async () => { calls.push('restoreSession'); if (session) Object.assign(state, { loggedIn: true, user: { id: 'g1', name: '구글 사용자' } }); }),
    renderList: log('renderList'), renderBookmarks: log('renderBookmarks'), go: log('go'), placePill: log('placePill'), drawMap: log('drawMap'),
    applyUser: log('applyUser'), openAuth: log('openAuth'),
    document: { getElementById: (id: string) => (id === 'authError' ? authError : null) },
  });
  runInContext(DATA + DATES + RANKING + ISSUE_BY_ID + API + LOAD + VIEW + INIT, context);
  await runInContext('start()', context);
  const of = (name: string) => calls.filter(c => c.split(' ')[0] === name);
  return { calls, of, location, history, authError };
}

describe('Google에서 돌아온 첫 로드(O2)', () => {
  it('③ 쿼리를 바로 지우고(해시 · 기록 수 그대로) 성공 토스트 / 로그인 창 + 표의 문구 / 아무것도 안 함, error 값은 화면에 0', async () => {
    const cases: { url: string; session: boolean; toast: string[]; openAuth: number; authError: string; hash: string; view: string }[] = [
      { url: '/?login=google', session: true, toast: ['toast 로그인했습니다'], openAuth: 0, authError: '', hash: '', view: 'map' },
      { url: '/?login=google#list', session: true, toast: ['toast 로그인했습니다'], openAuth: 0, authError: '', hash: '#list', view: 'list' },
      { url: '/?login=google&error=access_denied#bookmarks', session: false, toast: [], openAuth: 1, authError: 'Google 로그인을 취소했습니다', hash: '#bookmarks', view: 'bookmarks' },
      { url: '/?login=google&error=account_not_linked', session: false, toast: [], openAuth: 1, authError: '이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요', hash: '', view: 'map' },
      { url: '/?login=google&error=zzz%3Cb%3Ezzz', session: false, toast: [], openAuth: 1, authError: 'Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요', hash: '', view: 'map' },
      { url: '/?login=google&error=state_mismatch#bookmarks', session: false, toast: [], openAuth: 1, authError: 'Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요', hash: '#bookmarks', view: 'bookmarks' },
      { url: '/?error=state_mismatch', session: false, toast: [], openAuth: 1, authError: 'Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요', hash: '', view: 'map' },
      { url: '/?login=google&error=access_denied#bookmarks', session: true, toast: [], openAuth: 0, authError: '', hash: '#bookmarks', view: 'bookmarks' },
      { url: '/?login=google', session: false, toast: [], openAuth: 0, authError: '', hash: '', view: 'map' },
      { url: '/?login=other', session: true, toast: [], openAuth: 0, authError: '', hash: '', view: 'map' },
    ];
    for (const c of cases) {
      const label = `${c.url} session=${c.session}`;
      const r = await boot(c.url, c.session);
      expect(r.of('replaceState'), label).toEqual([`replaceState /${c.hash}`]);
      expect([r.location.search, r.location.hash, r.history.length], label).toEqual(['', c.hash, 1]);
      expect(r.of('fetch'), label).toEqual(['fetch /api/rankings']);
      expect(r.of('go'), label).toEqual([`go ${c.view} false`]);
      expect(r.of('toast'), label).toEqual(c.toast);
      expect(r.of('openAuth'), label).toEqual(Array(c.openAuth).fill('openAuth')); // 인수 없음 = 로그인 모드 · 이슈 없음
      expect(r.authError.textContent, label).toBe(c.authError);
      expect(r.of('openDrawer'), label).toEqual([]);
      const shown = JSON.stringify([r.calls, r.authError.textContent]);
      for (const raw of ['access_denied', 'account_not_linked', 'state_mismatch', 'zzz', '<b>']) expect(shown, label).not.toContain(raw);
      if (c.toast.length) expect(r.calls.indexOf(c.toast[0]), label).toBeGreaterThan(r.calls.indexOf(`go ${c.view} false`));
    }
  });

  it('③ ?issue={그날 id}&login=google + 세션: 그날 랭킹 → 상세창 → 토스트 "로그인했습니다" (북마크하려던 이슈로 돌아옴)', async () => {
    const ranking = await memoryStore.ranking('2026-10-05') as { sections: { issues: { id: string }[] }[] };
    const id = ranking.sections[1].issues[2].id;
    const r = await boot(`/?login=google&issue=${id}#list`, true);
    expect(r.calls).toEqual([
      'renderUser', 'renderTabs', 'replaceState /#list', 'fetch /api/rankings?date=2026-10-05', 'restoreSession',
      'renderList', 'renderBookmarks', 'go list false', `openDrawer ${id}`, 'toast 로그인했습니다',
    ]);
    // 비로그인으로 돌아오면(취소) 상세창은 열고 로그인 창에 문구
    const cancelled = await boot(`/?login=google&issue=${id}&error=access_denied`, false);
    expect(cancelled.of('openDrawer')).toEqual([`openDrawer ${id}`]);
    expect(cancelled.of('openAuth')).toEqual(['openAuth']);
    expect(cancelled.authError.textContent).toBe('Google 로그인을 취소했습니다');
    expect(cancelled.of('toast')).toEqual([]);
  });
});
