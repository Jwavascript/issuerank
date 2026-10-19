// 공유 · 설치 · 서비스 워커 등록(O4 · D29 · AC-13-1 · 13-3): 실제 public/index.html의 새 구역(Share · install)과 문서 클릭 분기(Events)를 잘라
// VM에서 가짜 navigator.share · canShare · matchMedia · clipboard · 대화상자 · 설치 이벤트로 실행
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';

const html = readFileSync('public/index.html', 'utf8');
const between = (start: string, end: string) => {
  const i = html.indexOf(start), j = html.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`index.html에 없음: ${start}`);
  return html.slice(i, j);
};
const DATA = between('const SECTIONS = [', 'const secColor');
const RANKING = between('let ranking = null;', '// 첫 진입은');
const FIND_BOOKMARK = between('const findBookmark =', 'const folderName');
const ISSUE_BY_ID = between('const issueById =', '/* ─────────────── Icons');
const SHARE = between('/* ─────────────── Share · install (O4)', '/* ─────────────── Navigation');
const EVENTS = between('/* ─────────────── Events', 'let raf;');

const BASE = 'https://issuerank-kappa.vercel.app';
const COPIED = '링크를 복사했습니다';
const settle = async () => { for (let i = 0; i < 6; i++) await new Promise(r => setTimeout(r, 0)); };
const plain = <T,>(value: T): T => JSON.parse(JSON.stringify(value));

// 그날 랭킹(정치 1위 = 요약 · 큰 연관기사 수 · 원문 URL이 있는 이슈) + 랭킹에 없는 북마크 카드 1개
const RANKED = {
  id: '2026-10-07-pol-1', date: '2026-10-07', section: 'pol', rank: 1, title: '“여야” 예산안 <합의> & 본회의 통과',
  summary: ['요약 첫 줄입니다', '요약 둘째 줄입니다', '요약 셋째 줄입니다'], articleCount: 4321, url: 'https://news.example.com/article/a1',
};
const SAVED = {
  id: '2026-10-01-eco-3', date: '2026-10-01', section: 'eco', rank: 3, title: '북마크에만 있는 경제 이슈',
  summary: null, articleCount: 7, url: 'https://news.example.com/article/b3', folderId: 'f1', savedAt: '2026-10-06T00:00:00Z',
};
const issueData = (it: { id: string; title: string }, sectionName: string) => ({ title: '이슈랭크', text: `[${sectionName}] ${it.title}`, url: `${BASE}/s?issue=${it.id}` });
const SITE = { title: '이슈랭크', text: '어제 가장 많이 보도된 뉴스 — 섹션별 순위 · 3줄 요약', url: `${BASE}/s` };
const copyText = (d: { text: string; url: string }) => `${d.text}\n${d.url}`;

type Listener = (event: unknown) => unknown;
type Options = {
  share?: (data: unknown) => Promise<void>; canShare?: (data: unknown) => boolean; coarse?: boolean;
  clipboard?: ((text: string) => Promise<void>) | null; standalone?: 'media' | 'navigator'; serviceWorker?: { register: (...args: unknown[]) => Promise<unknown> } | null;
};
function page(opts: Options = {}) {
  const el = (id: string) => {
    const listeners: Record<string, Listener> = {};
    return {
      id, hidden: id === 'installBtn', value: '', open: false, listeners,
      addEventListener(type: string, fn: Listener) { listeners[type] = fn; },
      showModal: vi.fn(function (this: { open: boolean }) { this.open = true; }), close: vi.fn(function (this: { open: boolean }) { this.open = false; }),
      focus: vi.fn(), select: vi.fn(), setSelectionRange: vi.fn(),
    };
  };
  const els = Object.fromEntries(['shareDialog', 'shareText', 'shareClose', 'installBtn', 'installFoot', 'installLink'].map(id => [id, el(id)]));
  const documentListeners: Record<string, Listener[]> = {}, windowListeners: Record<string, Listener[]> = {};
  const shared: unknown[] = [], copied: string[] = [];
  const navigator: Record<string, unknown> = {};
  if (opts.share) navigator.share = vi.fn((data: unknown) => { shared.push(plain(data)); return opts.share!(data); });
  if (opts.canShare) navigator.canShare = opts.canShare;
  if (opts.clipboard !== null) navigator.clipboard = { writeText: opts.clipboard ?? (async (text: string) => { copied.push(text); }) };
  if (opts.standalone === 'navigator') navigator.standalone = true;
  if (opts.serviceWorker) navigator.serviceWorker = opts.serviceWorker;
  const media: Record<string, boolean> = { '(pointer: coarse)': !!opts.coarse, '(display-mode: standalone)': opts.standalone === 'media' };
  const matchMedia = vi.fn((query: string) => ({ matches: media[query] ?? false }));
  const toast = vi.fn();
  const state = { bookmarks: [SAVED] };
  const context = createContext({
    state, navigator, matchMedia, toast, encodeURIComponent, scrim: {}, closePop: vi.fn(),
    document: {
      getElementById: (id: string) => els[id] ?? null,
      addEventListener: (type: string, fn: Listener) => { (documentListeners[type] ??= []).push(fn); },
    },
    addEventListener: (type: string, fn: Listener) => { (windowListeners[type] ??= []).push(fn); },
  });
  runInContext(DATA + RANKING + FIND_BOOKMARK + ISSUE_BY_ID + SHARE + EVENTS, context);
  runInContext(`ranking = ${JSON.stringify({ date: '2026-10-07', sections: [{ key: 'pol', name: '정치', issues: [RANKED] }] })}`, context);
  // 문서 클릭: target.closest(selector)는 그 선택자에만 요소를 돌려줌(실제 위임 코드를 그대로 지남)
  const click = (selector: string, target: unknown = {}) => { for (const fn of documentListeners.click ?? []) fn({ target: { closest: (s: string) => (s === selector ? target : null) } }); };
  const fire = (type: string, event: unknown = {}) => { for (const fn of windowListeners[type] ?? []) fn(event); };
  return { els, shared, copied, toast, navigator, matchMedia, click, fire, windowListeners, run: (code: string) => runInContext(code, context) };
}
const shareIssue = (p: ReturnType<typeof page>, id: string) => p.click('[data-share]', { dataset: { share: id } });
const abort = () => Promise.reject(new DOMException('Share canceled', 'AbortError'));

describe('공유 (AC-13-3)', () => {
  it('① 휴대폰(coarse) + 공유 성공: 누른 같은 틱에 share 1회 · 값 = 보내는 값 표 · 토스트 · 복사 · 창 0', async () => {
    const p = page({ share: async () => {}, canShare: () => true, coarse: true });
    shareIssue(p, RANKED.id);
    expect(p.shared).toEqual([issueData(RANKED, '정치')]); // await 전에 이미 부름
    expect((p.navigator.share as ReturnType<typeof vi.fn>).mock.calls).toHaveLength(1);
    await settle();
    expect([p.toast.mock.calls, p.copied, p.els.shareDialog.showModal.mock.calls.length]).toEqual([[], [], 0]);
  });

  it('② 취소(AbortError): 토스트 · 복사 · 창 0', async () => {
    const p = page({ share: abort, coarse: true });
    shareIssue(p, RANKED.id);
    await settle();
    expect([p.shared.length, p.toast.mock.calls, p.copied, p.els.shareDialog.showModal.mock.calls.length]).toEqual([1, [], [], 0]);
  });

  it('③ 그 밖 실패(NotAllowedError): 복사 1 = text + 줄바꿈 + url · "링크를 복사했습니다"', async () => {
    const p = page({ share: () => Promise.reject(new DOMException('Not allowed', 'NotAllowedError')), coarse: true });
    shareIssue(p, RANKED.id);
    await settle();
    expect(p.copied).toEqual([`[정치] ${RANKED.title}\n${BASE}/s?issue=${RANKED.id}`]);
    expect(p.toast.mock.calls).toEqual([[COPIED]]);
  });

  it('④ 마우스 기기(fine) · ⑤ 공유 API 없음 · canShare 거짓: share 0 · 복사 1 · 같은 토스트', async () => {
    for (const [label, opts] of [
      ['fine', { share: async () => {}, canShare: () => true, coarse: false }],
      ['share 없음', { coarse: true }],
      ['canShare 거짓', { share: async () => {}, canShare: () => false, coarse: true }],
    ] as [string, Options][]) {
      const p = page(opts);
      shareIssue(p, RANKED.id);
      await settle();
      expect(p.shared, label).toEqual([]);
      expect(p.copied, label).toEqual([copyText(issueData(RANKED, '정치'))]);
      expect(p.toast.mock.calls, label).toEqual([[COPIED]]);
    }
  });

  it('⑥ 복사 거절 · 클립보드 없음: 창 열림 · 글 상자 = 복사 글 · 전체 선택 · 토스트 0 · 닫기', async () => {
    for (const clipboard of [async () => { throw new DOMException('denied', 'NotAllowedError'); }, null]) {
      const p = page({ clipboard, coarse: false });
      shareIssue(p, RANKED.id);
      await settle();
      const box = p.els.shareText, want = copyText(issueData(RANKED, '정치'));
      expect([p.els.shareDialog.showModal.mock.calls.length, p.els.shareDialog.open, box.value]).toEqual([1, true, want]);
      expect([box.focus.mock.calls.length, box.select.mock.calls.length, box.setSelectionRange.mock.calls]).toEqual([1, 1, [[0, want.length]]]);
      expect(p.toast.mock.calls).toEqual([]);
      p.els.shareClose.listeners.click({});
      expect(p.els.shareDialog.open).toBe(false);
    }
  });

  it('⑦ 랭킹에 없고 북마크에만 있는 이슈: 북마크의 제목 · 섹션 · 그 이슈 주소', async () => {
    const mobile = page({ share: async () => {}, coarse: true });
    shareIssue(mobile, SAVED.id);
    expect(mobile.shared).toEqual([issueData(SAVED, '경제')]);
    const pc = page({ coarse: false });
    shareIssue(pc, SAVED.id);
    await settle();
    expect(pc.copied).toEqual([`[경제] ${SAVED.title}\n${BASE}/s?issue=${SAVED.id}`]);
  });

  it('⑧ 사이트(맨 아래 이슈랭크 공유): 값 = 표 · 복사 글 = text + 줄바꿈 + url', async () => {
    const mobile = page({ share: async () => {}, coarse: true });
    mobile.click('[data-share-site]');
    expect(mobile.shared).toEqual([SITE]);
    const pc = page({ coarse: false });
    pc.click('[data-share-site]');
    await settle();
    expect(pc.copied).toEqual([`${SITE.text}\n${SITE.url}`]);
    expect(pc.toast.mock.calls).toEqual([[COPIED]]);
  });

  it('⑨ 요약 있는 이슈: 요약 · 연관기사 수 · 원문 URL이 보내는 값 어디에도 0 · 칸은 title · text · url뿐', async () => {
    const mobile = page({ share: async () => {}, coarse: true });
    const pc = page({ coarse: false });
    shareIssue(mobile, RANKED.id);
    shareIssue(pc, RANKED.id);
    await settle();
    expect(Object.keys(mobile.shared[0] as object)).toEqual(['title', 'text', 'url']);
    const sent = JSON.stringify([mobile.shared, pc.copied]);
    for (const banned of [...RANKED.summary, String(RANKED.articleCount), RANKED.url, 'news.example.com', '연관기사']) expect(sent, banned).not.toContain(banned);
    expect(sent).toContain(RANKED.title.replace(/"/g, '\\"')); // 제목 원문 그대로(이스케이프 없음)
  });

  it('없는 id: 아무것도 안 함 · 클릭 분기는 [data-share] · [data-share-site] 두 줄', async () => {
    const p = page({ share: async () => {}, coarse: true });
    shareIssue(p, '2026-10-07-pol-99');
    await settle();
    expect([p.shared, p.copied, p.toast.mock.calls]).toEqual([[], [], []]);
    expect(EVENTS.match(/data-share/g)).toHaveLength(2);
    expect(EVENTS).toContain("if ((b = el('[data-share]'))) return share(issueShare(b.dataset.share));");
    expect(EVENTS).toContain("if (el('[data-share-site]')) return share(SITE_SHARE);");
  });
});

describe('설치 · 서비스 워커 등록 (AC-13-1)', () => {
  const installEvent = (outcome: 'accepted' | 'dismissed') => ({ preventDefault: vi.fn(), prompt: vi.fn(async () => ({ outcome })) });

  it('beforeinstallprompt → preventDefault 1 · 헤더 · 맨 아래 앱 설치 보임 → 누름 → prompt 1 · 숨김(accepted · dismissed 둘 다) → 다시 눌러도 0', () => {
    for (const outcome of ['accepted', 'dismissed'] as const) {
      const p = page();
      expect([p.els.installBtn.hidden, p.els.installFoot.hidden]).toEqual([true, false]); // 이벤트 전: 헤더 숨김 · 맨 아래 보임
      const e = installEvent(outcome);
      p.fire('beforeinstallprompt', e);
      expect([e.preventDefault.mock.calls.length, p.els.installBtn.hidden, p.els.installFoot.hidden]).toEqual([1, false, false]);
      p.els.installBtn.listeners.click({});
      expect([e.prompt.mock.calls.length, p.els.installBtn.hidden], outcome).toEqual([1, true]);
      p.els.installBtn.listeners.click({});
      expect(e.prompt.mock.calls.length).toBe(1);
    }
  });

  it('맨 아래 앱 설치: 이벤트가 있으면 prompt 1(링크 이동 막음) · 없으면 링크 그대로 /widget#app', () => {
    const p = page();
    const e = installEvent('accepted');
    p.fire('beforeinstallprompt', e);
    const click = { preventDefault: vi.fn() };
    p.els.installLink.listeners.click(click);
    expect([click.preventDefault.mock.calls.length, e.prompt.mock.calls.length, p.els.installBtn.hidden]).toEqual([1, 1, true]);
    const again = { preventDefault: vi.fn() };
    p.els.installLink.listeners.click(again); // 이벤트는 한 번만 → 이제 안내 링크
    expect([again.preventDefault.mock.calls.length, e.prompt.mock.calls.length]).toEqual([0, 1]);
    const none = page();
    const noEvent = { preventDefault: vi.fn() };
    none.els.installLink.listeners.click(noEvent);
    expect(noEvent.preventDefault).not.toHaveBeenCalled();
    expect(html).toContain('<span id="installFoot"> · <a href="/widget#app" id="installLink">앱 설치</a></span>');
  });

  it('appinstalled → 헤더 숨김 · "앱을 설치했습니다" / standalone(display-mode · navigator.standalone) → 설치 UI 0', () => {
    const p = page();
    p.fire('beforeinstallprompt', installEvent('accepted'));
    p.fire('appinstalled');
    expect([p.els.installBtn.hidden, p.toast.mock.calls]).toEqual([true, [['앱을 설치했습니다']]]);
    for (const standalone of ['media', 'navigator'] as const) {
      const app = page({ standalone });
      const e = installEvent('accepted');
      app.fire('beforeinstallprompt', e);
      expect([app.els.installBtn.hidden, app.els.installFoot.hidden, e.preventDefault.mock.calls.length], standalone).toEqual([true, true, 1]);
      app.els.installBtn.listeners.click({});
      expect(e.prompt).not.toHaveBeenCalled();
    }
    expect(html).toMatch(/<button type="button" class="pill-btn" id="installBtn" aria-label="이슈랭크 앱 설치" hidden>[\s\S]*?<span class="lbl">앱 설치<\/span>/);
  });

  it("서비스 워커: load 뒤 register 1회 = ('/sw.js', { scope: '/', updateViaCache: 'none' }) · API 없음 · 실패 → 예외 · 토스트 0", async () => {
    const register = vi.fn(async () => ({}));
    const p = page({ serviceWorker: { register } });
    p.run('registerServiceWorker()');
    expect(register).not.toHaveBeenCalled(); // load 전
    p.fire('load');
    expect(plain(register.mock.calls)).toEqual([['/sw.js', { scope: '/', updateViaCache: 'none' }]]);

    const none = page({ serviceWorker: null });
    expect(() => none.run('registerServiceWorker()')).not.toThrow();
    expect(none.windowListeners.load).toBeUndefined();

    const unhandled: unknown[] = [];
    const onUnhandled = (reason: unknown) => { unhandled.push(reason); };
    process.on('unhandledRejection', onUnhandled);
    try {
      const failing = page({ serviceWorker: { register: vi.fn(async () => { throw new DOMException('denied', 'SecurityError'); }) } });
      failing.run('registerServiceWorker()');
      expect(() => failing.fire('load')).not.toThrow();
      await settle();
      expect([unhandled, failing.toast.mock.calls]).toEqual([[], []]);
    } finally { process.off('unhandledRejection', onUnhandled); }
  });
});
