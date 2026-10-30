// 아침 알림 자동 다시 연결 화면(O7 · D32 · AC-14-5 · 회귀 AC-14-1 · 14-4): public/index.html API + Push (O5) 구역을 VM에서 실행 —
// 가짜 navigator(serviceWorker.ready · pushManager getSubscription · subscribe) · Notification(permission · requestPermission 셈) · localStorage(Map · 예외 모드) ·
// document(addEventListener · visibilityState) · Date.now · fetch(가짜 서버 행 집합: 새 endpoint POST 201 { created: true } · 있던 것 204 · DELETE 204 · 실패 모드) · toast · location.
// 한 기기(권한 · 기기 구독 · 서버 행 · localStorage)를 여러 페이지(열기)가 이어 씀
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';
import { VAPID_PUBLIC_KEY } from '../server/push-config';

const html = readFileSync('public/index.html', 'utf8');
const between = (start: string, end: string) => {
  const i = html.indexOf(start), j = html.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`없음: ${start}`);
  return html.slice(i, j);
};
const API = between('async function api(path', '/* ─────────────── Date');
const PUSH = between('/* ─────────────── Push (O5)', '/* ─────────────── Share · install (O4)');
const settle = async () => { for (let i = 0; i < 16; i++) await new Promise(r => setTimeout(r, 0)); };
const LABEL = { off: '아침 알림 받기', on: '아침 알림 끄기', blocked: '아침 알림 차단됨 — 해제 방법 보기', unsupported: '아침 알림 — 켜는 방법 안내' };
const shown = (state: keyof typeof LABEL) => ({ state, label: LABEL[state], title: LABEL[state] });
const KEY_BYTES = [...Buffer.from(VAPID_PUBLIC_KEY, 'base64url')];
const KEYS = { p256dh: 'BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4', auth: 'BTBZMqHH6r4Tts7J_aSIgg' };
const EP = (name: string) => `https://fcm.googleapis.com/fcm/send/${name}`;
const short = (endpoint: string) => endpoint.slice(endpoint.lastIndexOf('/') + 1);
const MARK = 'issuerank-push';
const MIN = 60e3;
const LOST = ['아침 알림 연결이 끊겼습니다', '다시 켜기'];
const BLOCKED = ['알림이 차단되어 있습니다', '방법 보기'];
const OFF = ['아침 알림이 꺼져 있습니다', '다시 켜기'];

type Perm = 'default' | 'granted' | 'denied';
type Storage = 'ok' | 'missing' | 'get-throws' | 'access-throws' | 'set-throws';
type Sub = { endpoint: string; toJSON: () => unknown; unsubscribe: ReturnType<typeof vi.fn> };
type Deferred = { promise: Promise<Response>; resolve: (r: Response) => void };
const deferred = (): Deferred => { let resolve!: (r: Response) => void; const promise = new Promise<Response>(r => { resolve = r; }); return { promise, resolve }; };
const status = (code: number, message?: string) => (message ? Response.json({ message }, { status: code }) : new Response(code === 500 ? 'Internal Server Error' : null, { status: code }));
const FAILS: [string, () => Promise<Response>][] = [
  ['400', async () => status(400, '요청 형식이 올바르지 않습니다')],
  ['503', async () => status(503, '지금은 알림을 켤 수 없습니다')],
  ['500', async () => status(500)],
  ['네트워크', async () => { throw new TypeError('Failed to fetch'); }],
];

function device(o: { permission?: Perm; sub?: string | null; rows?: string[]; mark?: boolean; storage?: Storage; unsubscribeFails?: boolean } = {}) {
  const dev = {
    permission: (o.permission ?? 'granted') as Perm,
    sub: null as Sub | null,
    rows: new Set((o.rows ?? []).map(EP)),                         // 서버 push_subscriptions endpoint
    store: new Map<string, string>(o.mark ? [[MARK, 'on']] : []), // 이 기기 localStorage
    storage: (o.storage ?? 'ok') as Storage,
    now: 1_790_000_000_000,
    seq: 0,
    log: [] as string[],                                           // 기기 · 서버 사건(페이지 사이에도 이어짐)
  };
  const makeSub = (endpoint: string, fails = false): Sub => {
    const s: Sub = {
      endpoint,
      toJSON: () => ({ endpoint, expirationTime: null, keys: KEYS }),
      unsubscribe: vi.fn(async () => {
        dev.log.push(`unsubscribe ${short(endpoint)} mark=${dev.store.get(MARK) ?? '-'}`);
        if (fails) throw new DOMException('unsubscribe failed', 'AbortError');
        if (dev.sub === s) dev.sub = null;
        return true;
      }),
    };
    return s;
  };
  if (o.sub) dev.sub = makeSub(EP(o.sub), o.unsubscribeFails);

  // 열기 1번 = 새 VM 문맥(전역 · 메모리 상태 새로), 기기 상태(dev)는 이어 씀
  function open(p: { respond?: (method: string, endpoint: string) => Response | Promise<Response> | undefined; subscribe?: () => Promise<Sub> | undefined; ready?: 'now' | 'later' } = {}) {
    const from = dev.log.length;
    let gesture = false;
    const listeners: Record<string, (e: unknown) => void> = {};
    const attrs: Record<string, string> = { 'aria-label': LABEL.unsupported };
    const btn = { dataset: { push: 'unsupported' }, title: LABEL.unsupported, setAttribute: (k: string, v: string) => { attrs[k] = v; }, addEventListener: (t: string, fn: (e: unknown) => void) => { listeners[t] = fn; } };
    const Notification = { get permission() { return dev.permission; }, requestPermission: vi.fn(async () => dev.permission) };
    const subscribeCalls: { userVisibleOnly: boolean; key: number[]; permission: Perm; gesture: boolean }[] = [];
    const defaultSubscribe = async () => {
      if (dev.permission === 'denied') throw new DOMException('Registration failed - permission denied', 'NotAllowedError');
      if (dev.permission === 'default') dev.permission = 'granted'; // 권한 창에서 허용(누름에서만 — gesture로 확인)
      const s = makeSub(EP(`new-${++dev.seq}`));
      dev.sub = s;
      return s;
    };
    const subscribe = vi.fn((options: { userVisibleOnly: boolean; applicationServerKey: Uint8Array }) => {
      subscribeCalls.push({ userVisibleOnly: options.userVisibleOnly, key: [...options.applicationServerKey], permission: dev.permission, gesture });
      dev.log.push('subscribe');
      return p.subscribe?.() ?? defaultSubscribe();
    });
    const getSubscription = vi.fn(async () => dev.sub);
    const registration = { pushManager: { subscribe, getSubscription } };
    let resolveReady: (r: unknown) => void = () => {};
    const ready = new Promise(r => { resolveReady = r; });
    if ((p.ready ?? 'now') === 'now') resolveReady(registration);
    const requests: { url: string; method: string; endpoint: string; body: string; cache?: string; result: number | string }[] = [];
    const fetchMock = vi.fn(async (url: string, init: { method: string; body: string; cache?: string }) => {
      const endpoint = JSON.parse(init.body).endpoint as string;
      const entry = { url, method: init.method, endpoint: short(endpoint), body: init.body, cache: init.cache, result: 'pending' as number | string };
      requests.push(entry);
      try {
        const custom = p.respond?.(init.method, endpoint);
        let res: Response;
        if (custom) res = await custom;
        else if (init.method === 'POST') { const isNew = !dev.rows.has(endpoint); dev.rows.add(endpoint); res = isNew ? Response.json({ created: true }, { status: 201 }) : new Response(null, { status: 204 }); }
        else { dev.rows.delete(endpoint); res = new Response(null, { status: 204 }); }
        entry.result = res.status;
        dev.log.push(`${init.method} ${short(endpoint)} ${res.status}`);
        return res;
      } catch (e) {
        entry.result = 'network';
        dev.log.push(`${init.method} ${short(endpoint)} network`);
        throw e;
      }
    });
    const storage = {
      getItem: vi.fn((k: string) => { if (dev.storage === 'get-throws') throw new DOMException('blocked', 'SecurityError'); return dev.store.get(k) ?? null; }),
      setItem: vi.fn((k: string, v: string) => { if (dev.storage === 'set-throws') throw new DOMException('full', 'QuotaExceededError'); dev.store.set(k, String(v)); }),
      removeItem: vi.fn((k: string) => { if (dev.storage === 'set-throws') throw new DOMException('full', 'QuotaExceededError'); dev.store.delete(k); }),
    };
    const window: Record<string, unknown> = { PushManager: function PushManager() {}, Notification };
    const globals: Record<string, unknown> = {};
    if (dev.storage === 'access-throws') Object.defineProperty(window, 'localStorage', { get: () => { throw new DOMException('blocked', 'SecurityError'); } });
    else if (dev.storage !== 'missing') { window.localStorage = storage; globals.localStorage = storage; }
    const docListeners: [string, (e: unknown) => void][] = [];
    const document = {
      visibilityState: 'visible',
      getElementById: (id: string) => (id === 'pushBtn' ? btn : null),
      addEventListener: vi.fn((type: string, fn: (e: unknown) => void) => { docListeners.push([type, fn]); }),
    };
    const toast = vi.fn();
    const location = { assign: vi.fn() };
    const context = createContext({
      navigator: { serviceWorker: { ready } }, window, Notification, fetch: fetchMock, toast, location, atob, Uint8Array, Date: { now: () => dev.now },
      state: { authVersion: 0 }, applyUser: vi.fn(), openAuth: vi.fn(), document, ...globals,
    });
    runInContext(API + PUSH, context);
    const inGesture = (fn: () => void) => { gesture = true; try { fn(); } finally { gesture = false; } };
    return {
      requests, subscribe, subscribeCalls, getSubscription, Notification, document, storage, toast, location,
      view: () => ({ state: btn.dataset.push, label: attrs['aria-label'], title: btn.title }),
      events: () => dev.log.slice(from),
      calls: () => requests.map(r => `${r.method} ${r.endpoint} ${r.result}`),
      toasts: () => toast.mock.calls.map(c => c.slice(0, 2)),
      click: () => inGesture(() => listeners.click({ preventDefault: vi.fn() })),
      press: (i = toast.mock.calls.length - 1) => inGesture(() => (toast.mock.calls[i][2] as () => void)()),
      show: async (state: 'visible' | 'hidden' = 'visible') => {
        document.visibilityState = state;
        for (const [type, fn] of docListeners) if (type === 'visibilitychange') fn({});
        await settle();
      },
      resolveReady: () => resolveReady(registration),
    };
  }
  return { dev, open, makeSub };
}

describe('저장소 막힘 = O5 그대로 (AC-14-5 마지막 문장)', () => {
  it('① 없음 · getItem 예외 · 접근 예외 × (granted+구독 · granted+없음 · denied · default): 열 때 getSubscription 1 · 서버 요청 0 · addEventListener 0 · 토스트 0 · 종 = O5 · 표시 쓰기 0 · 켜기도 O5', async () => {
    for (const storage of ['missing', 'get-throws', 'access-throws'] as const) {
      for (const [permission, sub, want] of [['granted', 'A', 'on'], ['granted', null, 'off'], ['denied', null, 'blocked'], ['default', null, 'off'], ['default', 'A', 'off']] as const) {
        const label = `${storage} ${permission} ${sub}`;
        const d = device({ storage, permission, sub, rows: [], mark: true });
        const p = d.open();
        await settle();
        expect([p.getSubscription.mock.calls.length, p.requests.length, p.document.addEventListener.mock.calls.length, p.toast.mock.calls.length], label).toEqual([1, 0, 0, 0]);
        expect(p.view(), label).toEqual(shown(want));
        expect([p.subscribe.mock.calls.length, p.storage.setItem.mock.calls.length, p.storage.removeItem.mock.calls.length, p.Notification.requestPermission.mock.calls.length], label).toEqual([0, 0, 0, 0]);
        if (want === 'off' && permission === 'granted') {
          p.click(); // 켜기(O5): 요청 = POST 1 · 표시 쓰기 0
          await settle();
          expect([p.view(), p.calls(), p.toasts(), p.storage.setItem.mock.calls.length], label).toEqual([shown('on'), ['POST new-1 201'], [['아침 알림을 켰습니다']], 0]);
        }
      }
    }
  });
});

describe('켜 둔 표시 (AC-14-5 · 14-1 · 14-4)', () => {
  it('② 켜기 성공 = 표시 on · 켜기 실패(503) = 표시 그대로(없음) · 끄기 = unsubscribe 전에 표시 지움(DELETE 실패여도)', async () => {
    const d = device({ permission: 'default', sub: null });
    const p = d.open();
    await settle();
    expect([p.view(), p.requests.length, p.toasts()]).toEqual([shown('off'), 0, []]);
    p.click();
    expect(p.subscribeCalls.map(c => c.gesture)).toEqual([true]); // 누른 같은 틱
    await settle();
    expect([p.view(), p.calls(), d.dev.store.get(MARK), p.toasts()]).toEqual([shown('on'), ['POST new-1 201'], 'on', [['아침 알림을 켰습니다']]]);

    const failed = device({ permission: 'granted', sub: null });
    const f = failed.open({ respond: m => (m === 'POST' ? status(503, '지금은 알림을 켤 수 없습니다') : undefined) });
    await settle();
    f.click();
    await settle();
    expect([f.view(), f.calls(), failed.dev.store.has(MARK), f.toasts(), failed.dev.sub]).toEqual([shown('off'), ['POST new-1 503'], false, [['지금은 아침 알림을 켤 수 없습니다']], null]);

    for (const [label, respond] of [['204', undefined], ['500', async () => status(500)], ['네트워크', async () => { throw new TypeError('offline'); }]] as [string, (() => Promise<Response>) | undefined][]) {
      const on = device({ permission: 'granted', sub: 'A', rows: ['A'], mark: true });
      const q = on.open({ respond: m => (m === 'DELETE' && respond ? respond() : undefined) });
      await settle();
      expect(q.view(), label).toEqual(shown('on'));
      q.click();
      await settle();
      expect([q.view(), q.events().slice(1), on.dev.store.has(MARK)], label).toEqual([shown('off'), ['unsubscribe A mark=-', `DELETE A ${label === '네트워크' ? 'network' : label}`], false]);
      expect(q.toasts().slice(-1), label).toEqual([['아침 알림을 껐습니다', '다시 켜기']]);
      const again = on.open(); // 끈 기기를 다시 열기: 그대로 꺼짐(O5)
      await settle();
      expect([again.view(), again.requests.length, again.subscribe.mock.calls.length, again.toasts()], label).toEqual([shown('off'), 0, 0, []]);
    }
  });
});

describe('열 때 확인 — 권한 허용 (AC-14-5)', () => {
  it('③ 구독 있음 + 서버 행 있음: POST 1(본문 = toJSON · no-store) → 204 · 그 밖 요청 · subscribe · unsubscribe 0 · 켜짐 · 토스트 0', async () => {
    const d = device({ permission: 'granted', sub: 'A', rows: ['A'], mark: true });
    const p = d.open();
    await settle();
    expect(p.requests.map(({ url, method, body, cache }) => ({ url, method, body, cache }))).toEqual([{ url: '/api/push/subscriptions', method: 'POST', body: JSON.stringify(d.dev.sub!.toJSON()), cache: 'no-store' }]);
    expect([p.calls(), p.events(), p.view(), p.toasts(), d.dev.store.get(MARK), p.document.addEventListener.mock.calls.map(c => c[0])]).toEqual([['POST A 204'], ['POST A 204'], shown('on'), [], 'on', ['visibilitychange']]);
    expect([p.subscribe.mock.calls.length, d.dev.sub!.unsubscribe.mock.calls.length, p.Notification.requestPermission.mock.calls.length]).toEqual([0, 0, 0]);
  });

  it('④ ③인데 표시 없음(O5 때 켠 기기): 표시 씀 + ③ / 쓰기 예외(QuotaExceeded)여도 확인은 그대로', async () => {
    for (const storage of ['ok', 'set-throws'] as const) {
      const d = device({ permission: 'granted', sub: 'A', rows: ['A'], mark: false, storage });
      const p = d.open();
      await settle();
      expect([p.calls(), p.view(), p.toasts(), d.dev.store.get(MARK) ?? null, p.storage.setItem.mock.calls], storage).toEqual([['POST A 204'], shown('on'), [], storage === 'ok' ? 'on' : null, [[MARK, 'on']]]);
    }
  });

  it('⑤ 구독 있음 + 서버 행 없음(410 꼴): POST 옛(201) → DELETE {옛} → POST 새(201) 정확히 · unsubscribe 1 · subscribe 1(키 65B · granted · 누름 없이) · requestPermission 0 · 켜짐 · 토스트 0 → 끄면 새 endpoint DELETE', async () => {
    for (const [label, opts] of [['표시 있음', { mark: true }], ['표시 없음(O5 기기)', { mark: false }], ['unsubscribe 실패', { mark: true, unsubscribeFails: true }]] as const) {
      const d = device({ permission: 'granted', sub: 'A', rows: [], ...opts });
      const old = d.dev.sub!;
      const p = d.open();
      await settle();
      expect(p.calls(), label).toEqual(['POST A 201', 'DELETE A 204', 'POST new-1 201']);
      expect(p.requests[1].body, label).toBe(JSON.stringify({ endpoint: EP('A') }));
      expect(p.events(), label).toEqual(['POST A 201', 'unsubscribe A mark=on', 'DELETE A 204', 'subscribe', 'POST new-1 201']);
      expect([old.unsubscribe.mock.calls.length, p.subscribeCalls, p.Notification.requestPermission.mock.calls.length], label)
        .toEqual([1, [{ userVisibleOnly: true, key: KEY_BYTES, permission: 'granted', gesture: false }], 0]);
      expect([p.view(), p.toasts(), d.dev.store.get(MARK), [...d.dev.rows].map(short)], label).toEqual([shown('on'), [], 'on', ['new-1']]);
      p.click(); // 끄기 = 새 구독
      await settle();
      expect(p.calls().slice(3), label).toEqual(['DELETE new-1 204']);
      expect([p.view(), d.dev.rows.size], label).toEqual([shown('off'), 0]);
    }
  });

  it('⑥ ⑤에서 subscribe 거절 · 새 POST 400 · 503 · 500 · 네트워크: 새 구독 unsubscribe · 꺼짐 · "아침 알림 연결이 끊겼습니다" + 다시 켜기(누르면 같은 틱 subscribe) · 표시 그대로', async () => {
    const cases: [string, Parameters<ReturnType<typeof device>['open']>[0]][] = [
      ['subscribe 거절', { subscribe: () => Promise.reject(new DOMException('no user activation', 'NotAllowedError')) }],
      ...FAILS.map(([label, fail]) => [`새 POST ${label}`, { respond: (m: string, e: string) => (m === 'POST' && e.endsWith('new-1') ? fail() : undefined) }] as [string, Parameters<ReturnType<typeof device>['open']>[0]]),
    ];
    for (const [label, opts] of cases) {
      const d = device({ permission: 'granted', sub: 'A', rows: [], mark: true });
      const p = d.open(opts);
      await settle();
      const rejected = label === 'subscribe 거절';
      expect(p.calls(), label).toEqual(['POST A 201', 'DELETE A 204', ...(rejected ? [] : [`POST new-1 ${label.endsWith('네트워크') ? 'network' : label.slice(-3)}`])]);
      expect(p.events().filter(e => e.startsWith('unsubscribe')), label).toEqual(rejected ? ['unsubscribe A mark=on'] : ['unsubscribe A mark=on', 'unsubscribe new-1 mark=on']);
      expect([p.view(), p.toasts(), d.dev.store.get(MARK), d.dev.sub, p.Notification.requestPermission.mock.calls.length], label).toEqual([shown('off'), [LOST], 'on', null, 0]);
      // 다시 켜기 = 기존 켜기(누름): subscribe가 누른 같은 틱에
      const before = p.subscribeCalls.length;
      p.press();
      expect(p.subscribeCalls.length - before, label).toBe(1);
      expect(p.subscribeCalls.at(-1)!.gesture, label).toBe(true);
      await settle();
    }
    // 다시 켜기 성공 = 켜짐 · 토스트 "켰습니다" / 다시 켜기도 실패 = 표시 그대로 on
    const d = device({ permission: 'granted', sub: 'A', rows: [], mark: true });
    let reject = true;
    const p = d.open({ subscribe: () => (reject ? (reject = false, Promise.reject(new DOMException('x', 'NotAllowedError'))) : undefined) });
    await settle();
    p.press();
    await settle();
    expect([p.view(), p.toasts(), p.calls().slice(-1), d.dev.store.get(MARK)]).toEqual([shown('on'), [LOST, ['아침 알림을 켰습니다']], ['POST new-1 201'], 'on']);
    const failing = device({ permission: 'granted', sub: 'A', rows: [], mark: true });
    const q = failing.open({ respond: (m, e) => (m === 'POST' && !e.endsWith('/A') ? status(500) : undefined) });
    await settle();
    q.press();
    await settle();
    expect([q.view(), q.toasts().slice(-1), failing.dev.store.get(MARK)]).toEqual([shown('off'), [['알림을 켜지 못했습니다. 잠시 후 다시 시도해 주세요']], 'on']);
  });

  it('⑦ 구독 없음 + 표시 = subscribe 1 → POST 1 → 켜짐 · 토스트 0 / 구독 없음 + 표시 없음 = 요청 · subscribe 0 · 꺼짐', async () => {
    const d = device({ permission: 'granted', sub: null, mark: true });
    const p = d.open();
    await settle();
    expect([p.events(), p.calls(), p.view(), p.toasts(), p.subscribeCalls.map(c => [c.permission, c.gesture])]).toEqual([['subscribe', 'POST new-1 201'], ['POST new-1 201'], shown('on'), [], [['granted', false]]]);
    const none = device({ permission: 'granted', sub: null, mark: false });
    const q = none.open();
    await settle();
    expect([q.requests.length, q.subscribe.mock.calls.length, q.view(), q.toasts(), none.dev.store.has(MARK)]).toEqual([0, 0, shown('off'), [], false]);
  });
});

describe('열 때 확인 — 권한 꺼짐 (AC-14-5)', () => {
  it('⑧ denied + 표시: 차단 · "알림이 차단되어 있습니다" + 방법 보기(→ /widget#push) · 표시 지움 · subscribe · 요청 0 → 다시 열기 토스트 0', async () => {
    for (const sub of [null, 'A']) {
      const d = device({ permission: 'denied', sub, rows: ['A'], mark: true });
      const p = d.open();
      expect(p.view()).toEqual(shown('blocked')); // 준비 전에도(O5)
      await settle();
      expect([p.view(), p.toasts(), d.dev.store.has(MARK), p.subscribe.mock.calls.length, p.requests.length]).toEqual([shown('blocked'), [BLOCKED], false, 0, 0]);
      p.press();
      expect(p.location.assign.mock.calls).toEqual([['/widget#push']]);
      const again = d.open();
      await settle();
      expect([again.view(), again.toasts(), again.requests.length, again.subscribe.mock.calls.length]).toEqual([shown('blocked'), [], 0, 0]);
    }
  });

  it('⑨ default + 표시: 꺼짐 · "아침 알림이 꺼져 있습니다" + 다시 켜기 · 표시 지움 · 열 때 subscribe 0 / 단추 → subscribe 1(누름) → 켜짐 · 표시 on', async () => {
    const d = device({ permission: 'default', sub: null, mark: true });
    const p = d.open();
    await settle();
    expect([p.view(), p.toasts(), d.dev.store.has(MARK), p.subscribe.mock.calls.length, p.requests.length, p.Notification.requestPermission.mock.calls.length]).toEqual([shown('off'), [OFF], false, 0, 0, 0]);
    p.press();
    expect(p.subscribeCalls).toEqual([{ userVisibleOnly: true, key: KEY_BYTES, permission: 'default', gesture: true }]);
    await settle();
    expect([p.view(), p.calls(), d.dev.store.get(MARK)]).toEqual([shown('on'), ['POST new-1 201'], 'on']);
    const again = device({ permission: 'default', sub: null, mark: false }).open();
    await settle();
    expect([again.view(), again.toasts()]).toEqual([shown('off'), []]);
  });
});

describe('다시 보일 때 · 상한 (AC-14-5)', () => {
  it('⑩ 다시 등록 POST 실패 4가지: 토스트 0 · 켜짐 · 표시 그대로 · subscribe · unsubscribe 0 → 60분 뒤 다시 보이면 POST 다시', async () => {
    for (const [label, fail] of FAILS) {
      const d = device({ permission: 'granted', sub: 'A', rows: ['A'], mark: true });
      let failing = true;
      const p = d.open({ respond: m => (m === 'POST' && failing ? fail() : undefined) });
      await settle();
      expect([p.toasts(), p.view(), d.dev.store.get(MARK), p.subscribe.mock.calls.length, d.dev.sub!.unsubscribe.mock.calls.length, p.requests.length], label).toEqual([[], shown('on'), 'on', 0, 0, 1]);
      failing = false;
      d.dev.now += 60 * MIN;
      await p.show();
      expect(p.calls().slice(1), label).toEqual(['POST A 204']);
    }
  });

  it('⑪ 다시 보임: 바로 · 59분 · 60분 hidden = 0 / 60분 visible = 확인 1 / 확인 중 · 종 처리 중 = 건너뜀 / 같은 페이지 두 번째 201 = 바꾸기 0 / 토스트 한 페이지 ≤ 1', async () => {
    const d = device({ permission: 'granted', sub: 'A', rows: ['A'], mark: true });
    let hold: Deferred | null = null;
    const p = d.open({ respond: () => (hold ? hold.promise : undefined) });
    await settle();
    expect(p.calls()).toEqual(['POST A 204']);
    await p.show();                         // 바로
    d.dev.now += 59 * MIN; await p.show();  // 59분
    d.dev.now += 1 * MIN; await p.show('hidden'); // 60분이지만 hidden
    expect(p.requests.length).toBe(1);
    await p.show();                         // 60분 visible
    expect(p.calls()).toEqual(['POST A 204', 'POST A 204']);
    // 확인 중: 다음 확인의 POST를 잡아 둔 동안 60분 뒤 다시 보임 · 종 누름 = 건너뜀 · 무시
    hold = deferred();
    d.dev.now += 60 * MIN; await p.show();
    d.dev.now += 60 * MIN; await p.show();
    p.click();
    expect([p.requests.length, p.subscribe.mock.calls.length, d.dev.sub!.unsubscribe.mock.calls.length]).toEqual([3, 0, 0]);
    hold.resolve(new Response(null, { status: 204 })); hold = null;
    await settle();
    expect([p.requests.length, p.view()]).toEqual([3, shown('on')]);
    // 종 처리 중(끄기의 unsubscribe가 안 끝남): 60분 뒤 다시 보임 = 건너뜀
    let finish: (v: boolean) => void = () => {};
    d.dev.sub!.unsubscribe.mockImplementationOnce(() => new Promise<boolean>(r => { finish = r; }));
    p.click();
    d.dev.now += 60 * MIN; await p.show();
    expect(p.requests.length).toBe(3);
    finish(true);
    await settle();
    expect([p.calls().slice(3), p.view()]).toEqual([['DELETE A 204'], shown('off')]);

    // 같은 페이지 두 번째 201: 바꾸기는 한 번만(POST가 행을 다시 만들었음)
    const twice = device({ permission: 'granted', sub: 'A', rows: [], mark: true });
    const t = twice.open();
    await settle();
    expect(t.calls()).toEqual(['POST A 201', 'DELETE A 204', 'POST new-1 201']);
    twice.dev.rows.clear(); // 그사이 발송이 410으로 지움
    twice.dev.now += 60 * MIN; await t.show();
    expect([t.calls().slice(3), t.subscribe.mock.calls.length, t.view(), t.toasts(), [...twice.dev.rows].map(short)]).toEqual([['POST new-1 201'], 1, shown('on'), [], ['new-1']]);

    // 토스트 한 페이지 1개: 바꾸기 실패 토스트 뒤 60분 다시 보임 = 바꾸기 · 토스트 0(표시는 그대로 → 다음 열기에서 다시)
    const lost = device({ permission: 'granted', sub: 'A', rows: [], mark: true });
    const l = lost.open({ subscribe: () => Promise.reject(new DOMException('x', 'NotAllowedError')) });
    await settle();
    lost.dev.now += 60 * MIN; await l.show();
    expect([l.toasts(), l.subscribe.mock.calls.length, l.view(), lost.dev.store.get(MARK)]).toEqual([[LOST], 1, shown('off'), 'on']);
    const next = lost.open(); // 다음 열기: 구독 없음 + 표시 → 새로 구독
    await settle();
    expect([next.calls(), next.view(), next.toasts()]).toEqual([['POST new-1 201'], shown('on'), []]);
    // 같은 페이지에서 그 뒤 권한이 꺼짐(denied): 차단 · 표시 지움은 하지만 두 번째 토스트는 띄우지 않음
    const limit = device({ permission: 'granted', sub: 'A', rows: [], mark: true });
    const m = limit.open({ subscribe: () => Promise.reject(new DOMException('x', 'NotAllowedError')) });
    await settle();
    limit.dev.permission = 'denied';
    limit.dev.now += 60 * MIN; await m.show();
    expect([m.toasts(), m.view(), limit.dev.store.has(MARK), m.requests.length]).toEqual([[LOST], shown('blocked'), false, 2]);
  });

  it('⑫ 등록 준비 전: 확인 0 · 다시 보임 0 / 준비 뒤 확인 1 — 확인 중 종 누름 무시(subscribe · unsubscribe 0) → 끝난 뒤 O5대로(누르면 끄기)', async () => {
    const d = device({ permission: 'granted', sub: 'A', rows: ['A'], mark: true });
    let hold: Deferred | null = deferred();
    const p = d.open({ ready: 'later', respond: () => (hold ? hold.promise : undefined) });
    await settle();
    await p.show();
    expect([p.getSubscription.mock.calls.length, p.requests.length, p.view()]).toEqual([0, 0, shown('off')]);
    p.resolveReady();
    await settle();
    expect([p.getSubscription.mock.calls.length, p.requests.length]).toEqual([1, 1]);
    p.click();
    p.click();
    expect([p.subscribe.mock.calls.length, d.dev.sub!.unsubscribe.mock.calls.length, p.toasts()]).toEqual([0, 0, []]);
    hold!.resolve(new Response(null, { status: 204 })); hold = null;
    await settle();
    expect(p.view()).toEqual(shown('on'));
    p.click();
    await settle();
    expect([p.calls(), p.view(), p.toasts()]).toEqual([['POST A 204', 'DELETE A 204'], shown('off'), [['아침 알림을 껐습니다', '다시 켜기']]]);
  });
});

describe('코드 위치 · 문구', () => {
  it('새 코드 = Push (O5) 구역 안(표시 키 · 60분 · visibilitychange는 저장소가 될 때만) · 토스트 문구 3 · requestPermission · setTimeout · 새 serviceWorker.register 0', () => {
    expect(PUSH).toContain("const PUSH_MARK = 'issuerank-push';");
    expect(PUSH).toContain('const PUSH_RECHECK = 60 * 60e3;');
    expect(PUSH).toContain("document.addEventListener('visibilitychange'");
    expect(html.split("addEventListener('visibilitychange'").length - 1).toBe(1);
    for (const words of LOST.concat(BLOCKED, OFF)) expect(PUSH).toContain(`'${words}'`);
    expect(PUSH).not.toMatch(/requestPermission|setTimeout|serviceWorker\.register\(/);
    expect(html.split('issuerank-push').length).toBe(PUSH.split('issuerank-push').length); // 표시 키는 Push 구역에만
  });
});
