// 아침 알림 화면(O5 · D30 · AC-14-1 · 14-4): public/index.html Push 구역(+ API 구역)을 가짜 navigator(serviceWorker.ready · pushManager) · Notification ·
// fetch · toast · location으로 VM에서 실행 / 헤더 종 마크업 · CSS 규칙 / widget.html #push 절 · 상태 스크립트 / privacy.html 새 문장 / 배지 PNG
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { inflateSync } from 'node:zlib';
import { describe, expect, it, vi } from 'vitest';
import { deviceCode, validKeys, VAPID_PUBLIC_KEY } from '../server/push-config';

const html = readFileSync('public/index.html', 'utf8');
const between = (source: string, start: string, end: string) => {
  const i = source.indexOf(start), j = source.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`없음: ${start}`);
  return source.slice(i, j);
};
const API = between(html, 'async function api(path', '/* ─────────────── Date');
const PUSH = between(html, '/* ─────────────── Push (O5)', '/* ─────────────── Share · install (O4)');
const settle = async () => { for (let i = 0; i < 8; i++) await new Promise(r => setTimeout(r, 0)); };
const text = (fragment: string) => fragment.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
const LABEL = { off: '아침 알림 받기', on: '아침 알림 끄기', blocked: '아침 알림 차단됨 — 해제 방법 보기', unsupported: '아침 알림 — 켜는 방법 안내' };
const ENDPOINT = 'https://fcm.googleapis.com/fcm/send/ui-device';
const KEY_BYTES = [...Buffer.from(VAPID_PUBLIC_KEY, 'base64url')];

type Sub = { endpoint: string; toJSON: () => unknown; unsubscribe: ReturnType<typeof vi.fn> };
const fakeSub = (endpoint = ENDPOINT): Sub => ({
  endpoint, unsubscribe: vi.fn(async () => true),
  toJSON: () => ({ endpoint, expirationTime: null, keys: { p256dh: 'BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4', auth: 'BTBZMqHH6r4Tts7J_aSIgg' } }),
});

type Options = {
  support?: 'all' | 'no-sw' | 'no-push' | 'no-notification';
  permission?: 'default' | 'granted' | 'denied';
  existing?: Sub | null;
  subscribe?: (opts: unknown) => Promise<Sub>;
  respond?: (method: string, body: string) => Promise<Response>;
  ready?: 'now' | 'later';
};
function page(o: Options = {}) {
  const support = o.support ?? 'all';
  const listeners: Record<string, (e: unknown) => void> = {};
  const attrs: Record<string, string> = { 'aria-label': LABEL.unsupported };
  const btn = { dataset: { push: 'unsupported' }, title: LABEL.unsupported, setAttribute: (k: string, v: string) => { attrs[k] = v; }, addEventListener: (t: string, fn: (e: unknown) => void) => { listeners[t] = fn; } };
  const Notification = { permission: o.permission ?? 'default' };
  const subscribe = vi.fn(o.subscribe ?? (async () => fakeSub()));
  const registration = { pushManager: { subscribe, getSubscription: vi.fn(async () => o.existing ?? null) } };
  let resolveReady: (r: unknown) => void = () => {};
  const ready = new Promise(r => { resolveReady = r; });
  if ((o.ready ?? 'now') === 'now') resolveReady(registration);
  const navigator: Record<string, unknown> = support === 'no-sw' ? {} : { serviceWorker: { ready } };
  const window: Record<string, unknown> = {};
  if (support !== 'no-push') window.PushManager = function PushManager() {};
  if (support !== 'no-notification') window.Notification = Notification;
  const requests: { url: string; method: string; body: string; cache?: string }[] = [];
  const fetchMock = vi.fn(async (url: string, init: { method: string; body: string; cache?: string }) => {
    requests.push({ url, method: init.method, body: init.body, cache: init.cache });
    return o.respond ? o.respond(init.method, init.body) : new Response(null, { status: 204 });
  });
  const toast = vi.fn();
  const location = { assign: vi.fn() };
  const context = createContext({
    navigator, window, Notification, fetch: fetchMock, toast, location, atob, Uint8Array,
    state: { authVersion: 0 }, applyUser: vi.fn(), openAuth: vi.fn(),
    document: { getElementById: (id: string) => (id === 'pushBtn' ? btn : null) },
  });
  runInContext(API + PUSH, context);
  const click = () => { const preventDefault = vi.fn(); listeners.click({ preventDefault }); return preventDefault; };
  const view = () => ({ state: btn.dataset.push, label: attrs['aria-label'], title: btn.title });
  return { btn, view, click, subscribe, registration, requests, toast, location, Notification, resolveReady: () => resolveReady(registration) };
}
const shown = (state: keyof typeof LABEL) => ({ state, label: LABEL[state], title: LABEL[state] });

describe('헤더 종 버튼 동작 (AC-14-1 · 14-4)', () => {
  it('① 지원 안 함 3종(serviceWorker · PushManager · Notification 없음): unsupported · 라벨 = 안내 · 누르면 링크 기본 동작(preventDefault 0) · subscribe 0', async () => {
    for (const support of ['no-sw', 'no-push', 'no-notification'] as const) {
      const p = page({ support });
      await settle();
      expect(p.view(), support).toEqual(shown('unsupported'));
      expect(p.click().mock.calls.length, support).toBe(0);
      expect([p.subscribe.mock.calls.length, p.requests.length, p.toast.mock.calls.length], support).toEqual([0, 0, 0]);
    }
  });

  it('② 구독 없음 → off → 누름: subscribe = 누른 같은 틱(앞 await 0) · { userVisibleOnly: true, applicationServerKey: 코드 공개 키 65B } → POST 본문 = toJSON() → on · "아침 알림을 켰습니다"', async () => {
    const p = page();
    await settle();
    expect(p.view()).toEqual(shown('off'));
    const prevent = p.click();
    expect(prevent).toHaveBeenCalledTimes(1);
    expect(p.subscribe).toHaveBeenCalledTimes(1); // await 전에 이미
    const arg = p.subscribe.mock.calls[0][0] as { userVisibleOnly: boolean; applicationServerKey: Uint8Array };
    expect([arg.userVisibleOnly, arg.applicationServerKey.length, [...arg.applicationServerKey]]).toEqual([true, 65, KEY_BYTES]);
    await settle();
    expect(p.requests).toEqual([{ url: '/api/push/subscriptions', method: 'POST', body: JSON.stringify(fakeSub().toJSON()), cache: 'no-store' }]);
    expect(p.view()).toEqual(shown('on'));
    expect(p.toast.mock.calls).toEqual([['아침 알림을 켰습니다']]);
  });

  it('③ 권한 거절(denied): POST 0 · "알림이 허용되지 않았습니다" · blocked / 창만 닫음(default): 같은 문구 · off', async () => {
    const denied = page({ subscribe: async () => { denied.Notification.permission = 'denied'; throw new DOMException('permission denied', 'NotAllowedError'); } });
    await settle();
    denied.click();
    await settle();
    expect([denied.requests.length, denied.toast.mock.calls, denied.view()]).toEqual([0, [['알림이 허용되지 않았습니다']], shown('blocked')]);
    const dismissed = page({ subscribe: async () => { throw new DOMException('permission denied', 'NotAllowedError'); } });
    await settle();
    dismissed.click();
    await settle();
    expect([dismissed.requests.length, dismissed.toast.mock.calls, dismissed.view()]).toEqual([0, [['알림이 허용되지 않았습니다']], shown('off')]);
    const other = page({ subscribe: async () => { throw new DOMException('push service error', 'AbortError'); } });
    await settle();
    other.click();
    await settle();
    expect([other.requests.length, other.toast.mock.calls, other.view()]).toEqual([0, [['알림을 켜지 못했습니다. 잠시 후 다시 시도해 주세요']], shown('off')]);
  });

  it('④ POST 400 · 503 · 500 · 네트워크 실패: 각 토스트 · 브라우저 구독 unsubscribe 1 · off', async () => {
    for (const [respond, want] of [
      [async () => Response.json({ message: '요청 형식이 올바르지 않습니다' }, { status: 400 }), '이 브라우저에서는 아침 알림을 켤 수 없습니다'],
      [async () => Response.json({ message: '지금은 알림을 켤 수 없습니다' }, { status: 503 }), '지금은 아침 알림을 켤 수 없습니다'],
      [async () => new Response('Internal Server Error', { status: 500 }), '알림을 켜지 못했습니다. 잠시 후 다시 시도해 주세요'],
      [async () => { throw new TypeError('Failed to fetch'); }, '알림을 켜지 못했습니다. 잠시 후 다시 시도해 주세요'],
    ] as [() => Promise<Response>, string][]) {
      const sub = fakeSub();
      const p = page({ subscribe: async () => sub, respond });
      await settle();
      p.click();
      await settle();
      expect([p.requests.length, p.toast.mock.calls, sub.unsubscribe.mock.calls.length, p.view()], want).toEqual([1, [[want]], 1, shown('off')]);
    }
  });

  it('⑤ 켜짐(구독 있음 · granted) → 누름: unsubscribe 1 → DELETE { endpoint } → "아침 알림을 껐습니다" + 다시 켜기(누르면 subscribe 1) · DELETE 실패여도 off', async () => {
    for (const respond of [async () => new Response(null, { status: 204 }), async () => new Response('x', { status: 500 }), async () => { throw new TypeError('offline'); }]) {
      const existing = fakeSub();
      const p = page({ existing, permission: 'granted', respond });
      await settle();
      expect(p.view()).toEqual(shown('on'));
      p.click();
      await settle();
      expect(existing.unsubscribe).toHaveBeenCalledTimes(1);
      expect(p.requests).toEqual([{ url: '/api/push/subscriptions', method: 'DELETE', body: JSON.stringify({ endpoint: ENDPOINT }), cache: 'no-store' }]);
      expect(p.view()).toEqual(shown('off'));
      expect(p.toast.mock.calls.map(c => c.slice(0, 2))).toEqual([['아침 알림을 껐습니다', '다시 켜기']]);
      const again = p.toast.mock.calls[0][2] as () => void;
      again();
      expect(p.subscribe).toHaveBeenCalledTimes(1); // 다시 켜기 = 같은 켜기(그 틱에 subscribe)
      await settle();
    }
  });

  it('⑥ 차단(denied): 처음부터 blocked · 누르면 "알림이 차단되어 있습니다" + 방법 보기 → /widget#push · subscribe 0', async () => {
    const p = page({ permission: 'denied', existing: fakeSub() });
    expect(p.view()).toEqual(shown('blocked')); // 등록 준비 전에도
    await settle();
    expect(p.view()).toEqual(shown('blocked'));
    expect(p.click()).toHaveBeenCalledTimes(1);
    expect(p.toast.mock.calls.map(c => c.slice(0, 2))).toEqual([['알림이 차단되어 있습니다', '방법 보기']]);
    (p.toast.mock.calls[0][2] as () => void)();
    expect(p.location.assign.mock.calls).toEqual([['/widget#push']]);
    expect([p.subscribe.mock.calls.length, p.requests.length]).toEqual([0, 0]);
  });

  it('⑦ 등록 준비 전 누름: "잠시 후 다시 눌러 주세요" · subscribe 0 → 준비된 뒤 누르면 subscribe', async () => {
    const p = page({ ready: 'later' });
    await settle();
    expect(p.view()).toEqual(shown('off'));
    p.click();
    expect([p.toast.mock.calls, p.subscribe.mock.calls.length]).toEqual([[['잠시 후 다시 눌러 주세요']], 0]);
    p.resolveReady();
    await settle();
    p.click();
    expect(p.subscribe).toHaveBeenCalledTimes(1);
  });

  it('⑧ 처리 중 빠르게 두 번: subscribe 1 · POST 1', async () => {
    let finish: (s: Sub) => void = () => {};
    const p = page({ subscribe: () => new Promise<Sub>(r => { finish = r; }) });
    await settle();
    p.click(); p.click();
    expect(p.subscribe).toHaveBeenCalledTimes(1);
    finish(fakeSub());
    await settle();
    p.click(); // 켜진 뒤 누르면 끄기(구독 다시 0)
    await settle();
    expect([p.subscribe.mock.calls.length, p.requests.map(r => r.method)]).toEqual([1, ['POST', 'DELETE']]);
  });
});

describe('index.html 헤더 · 코드 위치 · 공개 키', () => {
  it('#pushBtn = 설치 · 북마크 사이 <a href="/widget#push"> · 처음 unsupported · aria-label = title · SVG 14px 종 + 사선', () => {
    const right = between(html, '<div class="header-right">', '</div>\n  </div>\n</header>');
    expect(right.indexOf('id="installBtn"')).toBeLessThan(right.indexOf('id="pushBtn"'));
    expect(right.indexOf('id="pushBtn"')).toBeLessThan(right.indexOf('id="bmNav"'));
    expect(right).toContain('<a class="pill-btn" id="pushBtn" href="/widget#push" data-push="unsupported" aria-label="아침 알림 — 켜는 방법 안내" title="아침 알림 — 켜는 방법 안내">');
    const svg = between(right, '<a class="pill-btn" id="pushBtn"', '</a>');
    expect(svg).toMatch(/<svg width="14" height="14" viewBox="0 0 24 24"/);
    expect(svg).toContain('class="bell"');
    expect(svg).toContain('class="slash"');
    expect(text(svg)).toBe(''); // 글자 없는 아이콘
  });

  it('CSS: 종 34×32(좌우 9px) · 상태 4 · 900px 미만 설치 글자 숨김 + 간격 6 · 400px 이하 헤더 설치 숨김 · 340px 이하 간격 8 · 전환 좌우 10 — 640px 규칙 뒤', () => {
    for (const rule of [
      '#pushBtn { padding: 0 9px; text-decoration: none; }',
      '#pushBtn .slash { display: none; }',
      '#pushBtn[data-push="on"] { color: var(--text); }',
      '#pushBtn[data-push="on"] .bell { fill: currentColor; }',
      '#pushBtn[data-push="blocked"] .slash { display: inline; }',
      '#pushBtn[data-push="unsupported"] { opacity: .55; }',
    ]) expect(between(html, '/* ── Header', '/* ── Views & transition'), rule).toContain(rule);
    const responsive = between(html, '/* Responsive */', '/* 이메일·비밀번호 인증 */');
    const narrow = responsive.indexOf('@media (max-width: 640px)');
    for (const rule of [
      '@media (max-width: 1199px) { #installBtn .lbl, #bmNav .lbl { display: none; } }',
      '@media (max-width: 899px) { #installBtn .lbl { display: none; } .header-right { gap: 6px; } }',
      '@media (max-width: 400px) { #installBtn { display: none !important; } }',
      '@media (max-width: 340px) { .header-inner { gap: 8px; } .switch button { padding: 6px 10px; } }',
    ]) expect(responsive.indexOf(rule), rule).toBeGreaterThan(narrow);
    expect(narrow).toBeGreaterThan(0);
  });

  it('Push 구역 = Toast 뒤 · Share · install (O4) 앞 · 버튼 처리는 구역 안 addEventListener · serviceWorker.register( 새로 0 · PUSH_KEY = server VAPID_PUBLIC_KEY(곡선 위 65B)', () => {
    const toastAt = html.indexOf('/* ─────────────── Toast'), pushAt = html.indexOf('/* ─────────────── Push (O5)'), shareAt = html.indexOf('/* ─────────────── Share · install (O4)');
    expect(toastAt).toBeGreaterThan(0);
    expect(pushAt).toBeGreaterThan(toastAt);
    expect(shareAt).toBeGreaterThan(pushAt);
    expect(PUSH).toContain("pushBtn.addEventListener('click'");
    expect(PUSH).not.toContain('serviceWorker.register(');
    expect(html.match(/serviceWorker\.register\(/g)).toHaveLength(1);
    expect(html).toContain('\nstart();\nregisterServiceWorker();\n</script>');
    const key = /const PUSH_KEY = '([\w-]+)';/.exec(PUSH)![1];
    expect(key).toBe(VAPID_PUBLIC_KEY);
    expect(key).toHaveLength(87);
    expect(validKeys(key, 'BTBZMqHH6r4Tts7J_aSIgg')).toBe(true);
    expect(html.match(/[\w-]{87}/g)!.filter(s => s === VAPID_PUBLIC_KEY)).toHaveLength(1);
  });
});

describe('widget.html #push 절 · 상태 한 줄', () => {
  const widget = readFileSync('public/widget.html', 'utf8');
  it('#push = 앱 절 뒤 · iPhone 절 앞 · 문구 = 계획 표 · 상태 칸', () => {
    const app = widget.indexOf('<h2 id="app">'), push = widget.indexOf('<h2 id="push">아침 알림</h2>'), iphone = widget.indexOf('<h2>iPhone</h2>');
    expect(app).toBeGreaterThan(0);
    expect(push).toBeGreaterThan(app);
    expect(iphone).toBeGreaterThan(push);
    expect(text(widget.slice(push, iphone))).toBe('아침 알림 '
      + '매일 아침 그날 랭킹이 준비되면(대개 6:30~8:00, 10시까지) "오늘의 뉴스가 도착했어요" 알림을 한 번 보냅니다. 누르면 이슈랭크 첫 화면이 열립니다. '
      + 'Android(Chrome): 이슈랭크(Chrome 또는 설치한 앱) 오른쪽 위 종 모양 버튼 → 허용. 다시 누르면 끕니다. 차단했다면 Chrome 주소창 왼쪽 아이콘 › 권한(또는 설정 › 사이트 설정 › 알림)에서 이슈랭크를 허용으로 바꾼 뒤 다시 누릅니다. Android 13 이상은 휴대폰 설정 › 앱 › Chrome(또는 이슈랭크) › 알림도 켜져 있어야 합니다. '
      + 'iPhone: iOS 16.4 이상에서 위 앱으로 설치로 홈 화면에 추가한 이슈랭크 앱을 열고 오른쪽 위 종 모양 버튼을 누릅니다. Safari 탭에서는 알림을 켤 수 없습니다. '
      + '카카오톡 · 인스타그램 앱 안 브라우저: 알림을 받을 수 없습니다. 메뉴에서 다른 브라우저(Chrome · Safari)로 열어 켜 주세요.');
    expect(widget.slice(push, iphone)).toContain('<p class="status" id="pushState" role="status"></p>');
  });

  // 상태 스크립트(서버 요청 0): 켜짐 = 기기 코드(서버 deviceCode와 같은 값) · 꺼짐 · 등록 없음 · 지원 안 함
  const script = between(widget, '<script>\n// 아침 알림(O5)', '</script>').slice('<script>'.length);
  const run = async (opts: { support?: boolean; reg?: boolean; sub?: { endpoint: string } | null }) => {
    const el = { textContent: '' };
    const fetchMock = vi.fn();
    const window: Record<string, unknown> = opts.support === false ? {} : { PushManager: function () {}, Notification: {} };
    const navigator = { serviceWorker: { getRegistration: vi.fn(async () => (opts.reg === false ? undefined : { pushManager: { getSubscription: async () => opts.sub ?? null } })) } };
    runInContext(script, createContext({ window, navigator, document: { getElementById: (id: string) => (id === 'pushState' ? el : null) }, crypto: globalThis.crypto, TextEncoder, Uint8Array, Array, fetch: fetchMock }));
    await settle();
    expect(fetchMock).not.toHaveBeenCalled();
    return el.textContent;
  };
  it('켜짐 → "이 기기: 아침 알림 켜짐 · 기기 코드 {SHA-256 앞 8자}(시험 알림용)" · 꺼짐 · 등록 없음 → 꺼짐 · 지원 안 함 → 안내', async () => {
    for (const endpoint of [ENDPOINT, 'https://web.push.apple.com/QGx0bHlv']) {
      expect(await run({ sub: { endpoint } })).toBe(`이 기기: 아침 알림 켜짐 · 기기 코드 ${deviceCode(endpoint)}(시험 알림용)`);
      expect(deviceCode(endpoint)).toMatch(/^[0-9a-f]{8}$/);
    }
    expect(await run({ sub: null })).toBe('이 기기: 아침 알림 꺼짐');
    expect(await run({ reg: false })).toBe('이 기기: 아침 알림 꺼짐');
    expect(await run({ support: false })).toBe('이 브라우저에서는 아침 알림을 받을 수 없습니다');
  });
});

describe('privacy.html 아침 알림 문장(초안 그대로)', () => {
  const privacy = readFileSync('public/privacy.html', 'utf8');
  const section = (n: number) => text(between(privacy, `<h2>${n}. `, n === 6 ? '<h2>7. ' : `<h2>${n + 1}. `));
  it('1 · 2 · 3 · 5 · 6항 새 문장 · 7항 그대로 · 시행일 · 변경 줄 · 이메일 꼴은 문의처 안에만', () => {
    expect(section(1)).toContain('아침 알림을 켠 기기: 그 기기 브라우저가 만든 알림 받는 주소(푸시 서비스 endpoint)와 알림 암호화용 공개 값 2개, 켠 시각, 마지막으로 알림을 보낸 날짜. 계정 · 이름 · 이메일 · IP 주소와 연결하지 않습니다.');
    expect(section(2)).toContain('아침 알림(하루 한 번 "오늘의 뉴스가 도착했어요") 보내기 — 알림을 켠 기기에만');
    expect(section(3)).toContain('아침 알림 정보는 알림을 끄면 바로 지웁니다. 브라우저 알림 권한을 끄거나 앱 · 사이트 데이터를 지워 푸시 서비스가 그 주소를 더 쓸 수 없다고 알려 오면 다음 발송 때 지웁니다.');
    expect(section(5)).toContain('GitHub Inc. — 매일 자동 실행(뉴스 수집 · 아침 알림 발송 작업)');
    expect(section(5)).toContain('아침 알림은 기기 브라우저 회사의 푸시 서비스(Google · Apple · Mozilla · Microsoft)를 거쳐 전달됩니다. 알림 내용은 국제 표준(RFC 8291)에 따라 암호화되어 푸시 서비스가 읽을 수 없고, 푸시 서비스는 전달에 필요한 주소만 처리합니다.');
    expect(section(6)).toContain('아침 알림 정보는 계정과 연결되어 있지 않아, 그 기기에서 종 아이콘으로 아침 알림을 끄면 지워집니다.');
    expect(text(between(privacy, '<h2>7. ', '<h2>8. '))).toBe('7. 쿠키 로그인 세션 쿠키와, Google 로그인을 진행하는 동안에만 쓰는 10분짜리 상태 쿠키만 사용합니다. 광고 · 추적 쿠키는 쓰지 않습니다. 브라우저에서 쿠키를 막으면 로그인할 수 없습니다.');
    expect(privacy).toContain('<p class="note">시행일 2026년 10월 20일</p>');
    expect(text(between(privacy, '<h2>12. ', '<p class="foot">'))).toBe('12. 시행일과 변경 이 방침은 2026년 10월 20일부터 적용합니다. 내용을 바꾸면 이 페이지에 바뀐 내용과 시행일을 먼저 알립니다. 변경: 2026년 10월 20일 — 아침 알림(웹 푸시) 구독 정보 추가');
    const contact = between(privacy, '<section id="contact"', '</section>');
    const emails = privacy.match(/[\w.+-]+@[\w-]+\.[\w.]+/g)!;
    expect(emails.every(e => e === 'jwavascript@gmail.com')).toBe(true);
    expect(emails.length).toBe(contact.match(/[\w.+-]+@[\w-]+\.[\w.]+/g)!.length);
  });
});

describe('배지 그림 badge-96.png', () => {
  it('96×96 RGBA · 픽셀 = 흰색(가장자리 · 흐린 칸 반투명 포함) 또는 완전 투명 · 불투명 흰 픽셀 있음 · 모서리 투명 · tools/pwa-images.tsx 7번째 출력', () => {
    const png = readFileSync('public/icons/badge-96.png');
    expect([...png.subarray(0, 8)]).toEqual([137, 80, 78, 71, 13, 10, 26, 10]);
    const width = png.readUInt32BE(16), height = png.readUInt32BE(20);
    expect([width, height, png[24], png[25], png[28]]).toEqual([96, 96, 8, 6, 0]);
    const idat: Buffer[] = [];
    for (let at = 8; at < png.length;) { const len = png.readUInt32BE(at); if (png.toString('latin1', at + 4, at + 8) === 'IDAT') idat.push(png.subarray(at + 8, at + 8 + len)); at += 12 + len; }
    const raw = inflateSync(Buffer.concat(idat)), stride = width * 4;
    let prev = new Uint8Array(stride), opaque = 0, partial = 0, transparent = 0, bad = 0;
    const corners: number[] = [];
    for (let y = 0; y < height; y++) {
      const f = raw[y * (stride + 1)], line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)), cur = new Uint8Array(stride);
      for (let i = 0; i < stride; i++) {
        const a = i >= 4 ? cur[i - 4] : 0, b = prev[i], c = i >= 4 ? prev[i - 4] : 0, p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        cur[i] = (line[i] + (f === 0 ? 0 : f === 1 ? a : f === 2 ? b : f === 3 ? (a + b) >> 1 : pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 255;
      }
      for (let x = 0; x < width; x++) {
        const [r, g, bl, al] = cur.subarray(x * 4, x * 4 + 4);
        if ((x === 0 || x === width - 1) && (y === 0 || y === height - 1)) corners.push(al);
        if (al === 0) transparent++;
        else if (r !== 255 || g !== 255 || bl !== 255) bad++;
        else if (al === 255) opaque++;
        else partial++;
      }
      prev = cur;
    }
    expect({ bad, corners }).toEqual({ bad: 0, corners: [0, 0, 0, 0] });
    expect(opaque).toBeGreaterThan(1000);
    expect(partial).toBeGreaterThan(0);
    expect(transparent).toBeGreaterThan(1000);
    const tool = readFileSync('tools/pwa-images.tsx', 'utf8');
    const files = [...tool.matchAll(/\{ file: '([^']+)', size: \[(\d+), (\d+)\]/g)].map(m => `${m[1]} ${m[2]}x${m[3]}`);
    expect(files).toEqual(['icons/icon-192.png 192x192', 'icons/icon-512.png 512x512', 'icons/maskable-192.png 192x192', 'icons/maskable-512.png 512x512', 'icons/apple-touch-icon.png 180x180', 'og.png 1200x630', 'icons/badge-96.png 96x96']);
  });
});
