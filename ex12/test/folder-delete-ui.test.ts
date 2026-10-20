// 폴더 삭제 화면(O6 · D31 · AC-10-5): 실제 public/index.html의 renderBookmarks · 새 구역(Folder delete (O6)) + api · loadBookmarks · currentAccount를 잘라
// VM에서 가짜 document · dialog(showModal · close · cancel · focus · 사각형) · fetch · toast · syncBookmarkUI로 실행
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';

const html = readFileSync('public/index.html', 'utf8');
const between = (start: string, end: string) => {
  const i = html.indexOf(start), j = html.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`index.html에 없음: ${start}`);
  return html.slice(i, j);
};
const CODE = [
  between('async function api(path', '/* ─────────────── Date'),
  between('async function loadBookmarks()', 'const allBookmarks'),
  between('const allBookmarks =', 'const findBookmark'),
  between('const esc =', 'const issueById'),
  between('const currentAccount =', 'async function save('),
  between('function renderBookmarks()', '/* ─────────────── Tooltip'),
  between('/* ─────────────── Folder delete (O6)', '/* ─────────────── Toast'),
].join('');

const NAME = '<b>&"';
const DEL_MARKUP = '<button type="button" class="chip-del" data-folder-delete="f2" aria-label="‘&lt;b&gt;&amp;&quot;’ 폴더 삭제">폴더 삭제</button>';
const card = (id: string, folderId: string, savedAt: string) => ({ id, date: id.slice(0, 10), folderId, savedAt });
const BOOKMARKS = [
  card('2026-10-06-it-1', 'f2', '2026-10-07T03:00:00Z'), card('2026-10-06-eco-2', 'f1', '2026-10-07T02:00:00Z'),
  card('2026-10-05-pol-1', 'f2', '2026-10-07T01:00:00Z'), card('2026-10-05-soc-3', 'f2', '2026-10-07T00:30:00Z'), card('2026-10-04-wor-1', 'f1', '2026-10-07T00:00:00Z'),
];
const FOLDERS = [{ id: 'f1', name: '반도체·AI', count: 2 }, { id: 'f2', name: NAME, count: 3 }, { id: 'f3', name: '빈 폴더', count: 0 }];
const plain = <T,>(v: T): T => JSON.parse(JSON.stringify(v));
const settle = async () => { for (let i = 0; i < 12; i++) await new Promise(r => setTimeout(r, 0)); };
const deferred = () => { let resolve!: (r: Response) => void, reject!: (e: unknown) => void; const promise = new Promise<Response>((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
type Listener = (event: Record<string, unknown>) => unknown;
type Fake = { id: string; textContent: string; innerHTML: string; disabled: boolean; listeners: Record<string, Listener>; addEventListener: (type: string, fn: Listener) => void; focus: (opts?: { preventScroll?: boolean }) => void };

function page(filter = 'f2') {
  const log: string[] = [];
  const events: string[] = []; // 초점 · 버튼 잠금 · 칩 줄 가로 위치 쓰기의 순서(보완 10-20)
  let focused: string | null = null;
  const base = (id: string): Fake => {
    const listeners: Record<string, Listener> = {};
    return { id, textContent: '', innerHTML: '', disabled: false, listeners, addEventListener: (type, fn) => { listeners[type] = fn; },
      focus: opts => { focused = id; events.push(`focus:${id}${opts?.preventScroll ? '(preventScroll)' : ''}`); } };
  };
  // 칩 줄: 고른 칩 · 마지막 자식(`폴더 삭제`)의 사각형은 geo로 정함. 조회 · 접근 · scrollLeft 쓰기는 셈(접근자는 defineProperties로)
  const geo = { picked: 200, del: 300 };
  const chipsAccess = { querySelector: 0, lastElementChild: 0, scrollSet: 0 };
  let scrollLeft = 0;
  const chips = Object.defineProperties(base('folderChips'), {
    querySelector: { value: (sel: string) => { chipsAccess.querySelector++; return sel === '[aria-pressed="true"]' ? { getBoundingClientRect: () => ({ right: geo.picked }) } : null; } },
    lastElementChild: { get: () => { chipsAccess.lastElementChild++; return { getBoundingClientRect: () => ({ left: geo.del }) }; } },
    scrollLeft: { get: () => scrollLeft, set: (v: number) => { chipsAccess.scrollSet++; scrollLeft = v; events.push(`scroll:${v}`); } },
  });
  const dialog = Object.assign(base('folderDialog'), {
    open: false,
    showModal: vi.fn(() => { dialog.open = true; log.push('showModal'); }),
    close: vi.fn(() => { dialog.open = false; log.push('close'); }),
    getBoundingClientRect: () => ({ left: 20, top: 200, right: 340, bottom: 420 }),
  });
  const els: Record<string, Fake> = { folderChips: chips, folderDialog: dialog };
  for (const id of ['bmBadge', 'bmList', 'bmSub', 'folderTitle', 'folderNote', 'folderCancel', 'folderConfirm']) els[id] = base(id);
  for (const id of ['folderCancel', 'folderConfirm']) {
    let value = false;
    Object.defineProperty(els[id], 'disabled', { get: () => value, set: (v: boolean) => { value = v; events.push(`disabled:${id}=${v}`); } });
  }
  const node = base;
  const delButton = node('folderDelete'), allChip = node('chip-all');
  const gets: string[] = [];
  const document = {
    getElementById: vi.fn((id: string) => { gets.push(id); return els[id] ?? null; }),
    querySelector: vi.fn((sel: string) => sel === '#folderChips [data-folder-delete]' ? (chips.innerHTML.includes('data-folder-delete') ? delButton : null)
      : sel === '#folderChips [data-folder="all"]' ? (chips.innerHTML.includes('data-folder="all"') ? allChip : null) : null),
  };
  // 서버 쪽 목록(다시 받기 응답) · DELETE 응답(테스트가 정함)
  const server = { bookmarks: plain(BOOKMARKS), folders: plain(FOLDERS) };
  let onDelete: () => Response | Promise<Response> = () => new Response(null, { status: 204 });
  let onList: (() => Response) | null = null;
  const fetchMock = vi.fn(async (url: string, opts: { method?: string } = {}) => {
    const method = opts.method ?? 'GET';
    log.push(`${method} ${url}`);
    if (method === 'DELETE' && url.startsWith('/api/folders/')) return onDelete();
    if (method === 'GET' && url === '/api/bookmarks') return onList ? onList() : Response.json(server.bookmarks);
    if (method === 'GET' && url === '/api/folders') return Response.json(server.folders);
    throw new Error(`UNEXPECTED ${method} ${url}`);
  });
  const state = { view: 'bookmarks', loggedIn: true, user: { id: 'A', name: 'A' }, authVersion: 1, folderFilter: filter, folders: plain(FOLDERS), bookmarks: plain(BOOKMARKS) };
  const toast = vi.fn((...args: unknown[]) => { log.push(`toast ${String(args[0])}`); });
  const applyUser = vi.fn(async (user: unknown) => {
    state.authVersion++; state.user = user as typeof state.user; state.loggedIn = !!user;
    state.bookmarks = []; state.folders = []; state.folderFilter = 'all';
  });
  const context = createContext({
    state, document, fetch: fetchMock, toast, applyUser, openAuth: vi.fn(), Response,
    itemHTML: (it: { id: string }) => `<li data-id="${it.id}"></li>`, fmtDate: (s: string) => s,
  });
  const sync = vi.fn(() => { log.push('sync'); runInContext('renderBookmarks()', context); });
  context.syncBookmarkUI = sync;
  runInContext(CODE, context);
  const run = (code: string) => runInContext(code, context);
  run('renderBookmarks()');
  // `폴더 삭제` 누름(칩 줄 클릭 위임): target.closest('[data-folder-delete]')만 그 버튼
  const clickDelete = (id = state.folderFilter) => chips.listeners.click({ target: { closest: (s: string) => (s === '[data-folder-delete]' ? { dataset: { folderDelete: id } } : null) } });
  const fire = (target: ReturnType<typeof node>, type: string, event: Record<string, unknown> = {}) => target.listeners[type](event);
  return {
    log, events, state, els, chips, dialog, delButton, allChip, document, gets, geo, chipsAccess, server, fetchMock, toast, applyUser, sync, run, clickDelete, fire,
    get focused() { return focused; }, get scrollLeft() { return scrollLeft; }, set scrollLeft(v: number) { scrollLeft = v; },
    setDelete: (fn: () => Response | Promise<Response>) => { onDelete = fn; }, setList: (fn: () => Response) => { onList = fn; },
    countDel: () => (chips.innerHTML.match(/data-folder-delete/g) ?? []).length,
  };
}

describe('칩 줄 `폴더 삭제` (renderBookmarks)', () => {
  it('① 전체 = 버튼 0 · 새 DOM 조회 · 메서드 0 / 폴더 고름 = 마지막 자식 1(aria-label 이스케이프) / 없는 폴더 = 전체 · 0 / 비로그인 = 0', () => {
    const all = page('all');
    expect(all.gets).toEqual(['folderDialog', 'folderCancel', 'folderConfirm', 'folderChips', 'bmBadge', 'folderChips', 'bmList', 'bmSub']); // 구역 맨 위 4 + 그리기 4
    all.gets.length = 0;
    all.run('renderBookmarks()');
    expect(all.gets).toEqual(['bmBadge', 'folderChips', 'bmList', 'bmSub']); // 전체 길 = 지금과 같은 조회뿐
    expect(all.countDel()).toBe(0);
    expect(all.chipsAccess).toEqual({ querySelector: 0, lastElementChild: 0, scrollSet: 0 });
    expect(all.document.querySelector).not.toHaveBeenCalled();

    const p = page('f2');
    expect(p.countDel()).toBe(1);
    expect(p.chips.innerHTML.endsWith(DEL_MARKUP)).toBe(true);
    expect(p.chips.innerHTML).toContain('data-folder="f2" aria-pressed="true">&lt;b&gt;&amp;&quot; <span class="cnt num">3</span></button>');

    const gone = page('f9');
    expect(gone.state.folderFilter).toBe('all');
    expect(gone.countDel()).toBe(0);
    expect(gone.chips.innerHTML).toContain('data-folder="all" aria-pressed="true"');

    const out = page('f2');
    out.state.loggedIn = false;
    out.run('renderBookmarks()');
    expect(out.chips.innerHTML).toBe('');
  });

  it('① 고른 칩이 버튼(+ 왼쪽 6px 띠)에 가리면 칩 줄만 가로로 밀고, 안 가리면 그대로', () => {
    const p = page('f2');
    p.scrollLeft = 100; p.geo.picked = 330; p.geo.del = 300; // 고른 칩 오른쪽 330 > 버튼 왼쪽 300 − 6
    p.run('renderBookmarks()');
    expect(p.scrollLeft).toBe(136);
    p.geo.picked = 294; // 딱 맞음
    p.run('renderBookmarks()');
    expect(p.scrollLeft).toBe(136);
    p.geo.picked = 294.2; // 소수점은 올림
    p.run('renderBookmarks()');
    expect(p.scrollLeft).toBe(137);
  });
});

describe('확인 창 (Folder delete (O6))', () => {
  it('② 열기: 제목 · 본문 = 표 문구(n = 칩 숫자 3 · n = 0) · 이름은 글자 그대로(textContent) · showModal 1 · 초점 = 취소', () => {
    const p = page('f2');
    p.clickDelete();
    expect(p.els.folderTitle.textContent).toBe(`‘${NAME}’ 폴더를 삭제할까요?`);
    expect(p.els.folderTitle.innerHTML).toBe('');
    expect(p.els.folderNote.textContent).toBe('폴더 안의 북마크 3개도 함께 삭제됩니다. 삭제하면 되돌릴 수 없습니다.');
    expect([p.dialog.showModal.mock.calls.length, p.dialog.open, p.focused]).toEqual([1, true, 'folderCancel']);
    p.fire(p.els.folderCancel, 'click');

    p.state.folderFilter = 'f3'; p.run('renderBookmarks()');
    p.clickDelete();
    expect(p.els.folderTitle.textContent).toBe('‘빈 폴더’ 폴더를 삭제할까요?');
    expect(p.els.folderNote.textContent).toBe('이 폴더는 비어 있습니다. 삭제하면 되돌릴 수 없습니다.');
    expect(p.dialog.showModal).toHaveBeenCalledTimes(2);
    p.clickDelete(); // 이미 열림 → 다시 열지 않음
    p.clickDelete('f9'); // 없는 폴더
    expect(p.dialog.showModal).toHaveBeenCalledTimes(2);
    expect(p.fetchMock).not.toHaveBeenCalled();
    // 창 자체가 초점을 받을 수 있음(보내는 동안 초점 자리, 보완 10-20) — 새 마크업에만 tabindex
    expect(html).toContain('<dialog id="folderDialog" class="auth-dialog" role="alertdialog" aria-labelledby="folderTitle" aria-describedby="folderNote" tabindex="-1">');
    expect(html.match(/tabindex="-1"/g)).toHaveLength(1);
  });

  it('③ 취소 · Esc(cancel) · 바깥 누름 = 닫힘 · 요청 0 · 상태 그대로 · 초점 = 폴더 삭제 / 창 안 여백 · 안쪽 요소 누름 = 열린 채', () => {
    const closers: [string, (p: ReturnType<typeof page>) => void][] = [
      ['취소', p => p.fire(p.els.folderCancel, 'click')],
      ['Esc', p => { const e = { preventDefault: vi.fn() }; p.fire(p.dialog, 'cancel', e); expect(e.preventDefault).toHaveBeenCalled(); }],
      ['바깥 누름', p => p.fire(p.dialog, 'click', { target: p.dialog, clientX: 10, clientY: 100 })],
      ['바깥 누름(아래)', p => p.fire(p.dialog, 'click', { target: p.dialog, clientX: 200, clientY: 700 })],
    ];
    for (const [label, close] of closers) {
      const p = page('f2');
      const before = plain(p.state);
      p.clickDelete();
      close(p);
      expect([p.dialog.open, p.focused], label).toEqual([false, 'folderDelete']);
      expect(p.fetchMock, label).not.toHaveBeenCalled();
      expect(plain(p.state), label).toEqual(before);
      expect(p.countDel(), label).toBe(1);
    }
    const p = page('f2');
    p.clickDelete();
    p.fire(p.dialog, 'click', { target: p.dialog, clientX: 30, clientY: 210 }); // 창 안 여백
    p.fire(p.dialog, 'click', { target: { id: 'folderNote' }, clientX: 100, clientY: 300 }); // 창 안 글
    expect([p.dialog.open, p.dialog.close.mock.calls.length]).toEqual([true, 0]);
  });

  it('④ 삭제 204: 응답을 잡아 둔 동안 다시 누름 · Esc · 바깥 누름 무시 → 창 닫힘 → 다시 받기 → 상태에서 그 폴더 · 북마크 0 · 전체 · sync 1 · 토스트(동작 0) · 초점 = 전체', async () => {
    const p = page('f2');
    const d = deferred();
    p.setDelete(() => d.promise);
    p.scrollLeft = 120; // 칩 줄을 오른쪽으로 민 채
    p.clickDelete();
    p.fire(p.els.folderConfirm, 'click');
    await settle();
    expect(p.fetchMock).toHaveBeenCalledTimes(1);
    expect(p.fetchMock.mock.calls[0][0]).toBe('/api/folders/f2');
    expect(p.fetchMock.mock.calls[0][1]).toMatchObject({ method: 'DELETE', cache: 'no-store' });
    expect([p.els.folderCancel.disabled, p.els.folderConfirm.disabled, p.dialog.open]).toEqual([true, true, true]);
    // 보완(10-20): 잠그기 전에 초점 = 창 자체(preventScroll) → 보내는 동안 창 안에 머묾
    expect(p.focused).toBe('folderDialog');
    expect(p.events.indexOf('focus:folderDialog(preventScroll)')).toBeGreaterThan(p.events.indexOf('focus:folderCancel'));
    expect(p.events.indexOf('focus:folderDialog(preventScroll)')).toBeLessThan(p.events.indexOf('disabled:folderCancel=true'));
    expect(p.events.indexOf('focus:folderDialog(preventScroll)')).toBeLessThan(p.events.indexOf('disabled:folderConfirm=true'));
    p.fire(p.els.folderConfirm, 'click');
    const esc = { preventDefault: vi.fn() };
    p.fire(p.dialog, 'cancel', esc);
    p.fire(p.dialog, 'click', { target: p.dialog, clientX: 5, clientY: 5 });
    p.fire(p.els.folderCancel, 'click');
    await settle();
    expect(esc.preventDefault).toHaveBeenCalled();
    expect([p.fetchMock.mock.calls.length, p.dialog.open, p.dialog.close.mock.calls.length]).toEqual([1, true, 0]);

    p.server.bookmarks = plain(BOOKMARKS.filter(b => b.folderId !== 'f2'));
    p.server.folders = plain(FOLDERS.filter(f => f.id !== 'f2'));
    d.resolve(new Response(null, { status: 204 }));
    await settle();
    expect(p.log).toEqual(['showModal', 'DELETE /api/folders/f2', 'close', 'GET /api/bookmarks', 'GET /api/folders', 'sync', `toast ‘${NAME}’ 폴더를 삭제했습니다`]);
    expect(p.state.folders.map(f => f.id)).toEqual(['f1', 'f3']);
    expect(p.state.bookmarks.filter(b => b.folderId === 'f2')).toEqual([]);
    expect(p.state.bookmarks).toHaveLength(2);
    expect(p.state.folderFilter).toBe('all');
    expect(p.sync).toHaveBeenCalledTimes(1);
    expect(p.toast.mock.calls).toEqual([[`‘${NAME}’ 폴더를 삭제했습니다`]]);
    expect(p.focused).toBe('chip-all');
    expect([p.els.folderCancel.disabled, p.els.folderConfirm.disabled]).toEqual([false, false]);
    expect(p.countDel()).toBe(0);
    expect(p.chips.innerHTML).not.toContain('data-folder="f2"');
    // 보완(10-20): 칩 줄을 맨 앞으로 되돌린 뒤 `전체` 칩에 초점(민 채 지워도 전체 칩이 다 보임)
    expect(p.scrollLeft).toBe(0);
    expect(p.events.slice(-2)).toEqual(['scroll:0', 'focus:chip-all']);
  });

  it('④ 다시 받기가 실패해도 화면 상태는 맞음(그 폴더 · 북마크 빠짐 · 전체) — 기존 loadBookmarks 토스트 뒤 삭제 토스트', async () => {
    const p = page('f2');
    p.setList(() => new Response('Internal Server Error', { status: 500, statusText: 'Internal Server Error' }));
    p.clickDelete();
    p.fire(p.els.folderConfirm, 'click');
    await settle();
    expect(p.state.folders.map(f => f.id)).toEqual(['f1', 'f3']);
    expect(p.state.bookmarks.map(b => b.folderId)).toEqual(['f1', 'f1']);
    expect(p.state.folderFilter).toBe('all');
    expect(p.toast.mock.calls).toEqual([['Internal Server Error'], [`‘${NAME}’ 폴더를 삭제했습니다`]]);
    expect(p.chips.innerHTML).not.toContain('data-folder="f2"');
  });

  it('⑤ 404: 토스트 `폴더를 찾을 수 없습니다` · 다시 받음 → 그 칩이 사라지고 전체', async () => {
    const p = page('f2');
    p.setDelete(() => Response.json({ message: '폴더를 찾을 수 없습니다' }, { status: 404 }));
    p.server.folders = plain(FOLDERS.filter(f => f.id !== 'f2'));
    p.server.bookmarks = plain(BOOKMARKS.filter(b => b.folderId !== 'f2'));
    p.clickDelete();
    p.fire(p.els.folderConfirm, 'click');
    await settle();
    expect(p.log).toEqual(['showModal', 'DELETE /api/folders/f2', 'close', 'toast 폴더를 찾을 수 없습니다', 'GET /api/bookmarks', 'GET /api/folders', 'sync']);
    expect(p.toast.mock.calls).toEqual([['폴더를 찾을 수 없습니다']]);
    expect([p.state.folderFilter, p.countDel()]).toEqual(['all', 0]);
    expect(p.chips.innerHTML).not.toContain('data-folder="f2"');
  });

  it('⑥ 500 · 502 · 네트워크 실패: 토스트 = 기존 e.message · 다시 받기 0 · 상태 · 고른 칩 그대로 · 버튼 다시 됨 · 초점 = 폴더 삭제', async () => {
    const cases: [string, () => Response | Promise<Response>, string][] = [
      ['500', () => new Response('Internal Server Error', { status: 500, statusText: 'Internal Server Error' }), 'Internal Server Error'],
      ['502', () => Response.json({ message: 'API 서버에 연결할 수 없습니다' }, { status: 502 }), 'API 서버에 연결할 수 없습니다'],
      ['네트워크', () => Promise.reject(new TypeError('Failed to fetch')), 'Failed to fetch'],
    ];
    for (const [label, respond, message] of cases) {
      const p = page('f2');
      const before = plain(p.state);
      p.setDelete(respond);
      p.scrollLeft = 120;
      p.clickDelete();
      const from = p.events.length;
      p.fire(p.els.folderConfirm, 'click');
      await settle();
      expect(p.toast.mock.calls, label).toEqual([[message]]);
      expect(p.fetchMock, label).toHaveBeenCalledTimes(1);
      expect(plain(p.state), label).toEqual(before);
      expect([p.dialog.open, p.els.folderCancel.disabled, p.els.folderConfirm.disabled, p.focused], label).toEqual([false, false, false, 'folderDelete']);
      // 보완(10-20): 보내는 동안 초점 = 창 → 끝나면 폴더 삭제(계획 규칙) · 칩 줄 가로 위치 그대로(실패 길은 되돌리지 않음)
      // (a.disabled = b.disabled = v 는 오른쪽 b부터 씀)
      expect(p.events.slice(from), label).toEqual(['focus:folderDialog(preventScroll)', 'disabled:folderConfirm=true', 'disabled:folderCancel=true',
        'disabled:folderConfirm=false', 'disabled:folderCancel=false', 'focus:folderDelete']);
      expect(p.scrollLeft, label).toBe(120);
      expect([p.sync.mock.calls.length, p.countDel()], label).toEqual([0, 1]);
      p.clickDelete(); // 다시 누를 수 있음
      expect(p.dialog.open, label).toBe(true);
    }
  });

  it('⑦ 401: 기존 api() 처리(로그아웃 상태 · 토스트 `로그인이 필요합니다` 하나) · 창 닫힘 · 다시 받기 0', async () => {
    const p = page('f2');
    p.setDelete(() => Response.json({ message: '로그인이 필요합니다' }, { status: 401 }));
    p.clickDelete();
    p.fire(p.els.folderConfirm, 'click');
    await settle();
    expect(p.applyUser).toHaveBeenCalledWith(null);
    expect(p.toast).toHaveBeenCalledTimes(1);
    expect(p.toast.mock.calls[0][0]).toBe('로그인이 필요합니다');
    expect([p.dialog.open, p.fetchMock.mock.calls.length, p.sync.mock.calls.length, p.state.loggedIn]).toEqual([false, 1, 0, false]);
  });

  it('⑧ 응답 전에 계정이 바뀜(204 · 404 · 500): 창만 닫힘 · 토스트 · 다시 받기 · sync 0', async () => {
    for (const status of [204, 404, 500]) {
      const p = page('f2');
      const d = deferred();
      p.setDelete(() => d.promise);
      p.clickDelete();
      p.fire(p.els.folderConfirm, 'click');
      await settle();
      p.state.authVersion++; p.state.user = { id: 'B', name: 'B' }; p.state.bookmarks = []; p.state.folders = []; p.state.folderFilter = 'all';
      d.resolve(status === 204 ? new Response(null, { status }) : Response.json({ message: 'x' }, { status }));
      await settle();
      expect([p.toast.mock.calls.length, p.fetchMock.mock.calls.length, p.sync.mock.calls.length, p.dialog.open], String(status)).toEqual([0, 1, 0, false]);
      expect([p.els.folderCancel.disabled, p.els.folderConfirm.disabled], String(status)).toEqual([false, false]);
      expect(p.state.folders, String(status)).toEqual([]);
    }
  });

  it('⑨ 창 안 keydown은 문서로 올리지 않음(stopPropagation) · 기본 동작(Esc 닫기)은 그대로', () => {
    const p = page('f2');
    p.clickDelete();
    for (const key of ['1', '2', '3', 'ArrowLeft', 'ArrowRight', 'Escape', 'Tab']) {
      const e = { key, stopPropagation: vi.fn(), preventDefault: vi.fn() };
      p.fire(p.dialog, 'keydown', e);
      expect([e.stopPropagation.mock.calls.length, e.preventDefault.mock.calls.length], key).toEqual([1, 0]);
    }
  });
});
