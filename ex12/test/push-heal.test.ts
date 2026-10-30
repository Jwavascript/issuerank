// 아침 알림 자동 다시 연결(O7 · D32 · AC-14-5) — 서버 · 서비스 워커 쪽:
// ① 구독 API: POST 새 endpoint 행 = 201 { created: true } · 이미 있던 endpoint = 204 (메모리 저장소 Hono 앱 app.request · createBff, Origin http://localhost:3000)
// ② public/sw.js pushsubscriptionchange를 가짜 self(registration.pushManager.subscribe · location) · fetch · atob으로 VM에서 실행
import { readFileSync } from 'node:fs';
import { createECDH, randomBytes } from 'node:crypto';
import { createContext, runInContext } from 'node:vm';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createApp } from '../server/app';
import { createBff } from '../server/bff';
import { VAPID_PUBLIC_KEY } from '../server/push-config';
import { memoryPushStore, memoryStore } from '../server/sample';

const LOCAL = 'http://localhost:3000';
const BAD = { message: '요청 형식이 올바르지 않습니다' };
const FULL = { message: '지금은 알림을 켤 수 없습니다' };
const app = createApp(memoryStore);
const key = () => { const e = createECDH('prime256v1'); e.generateKeys(); return e.getPublicKey().toString('base64url'); };
const auth = () => randomBytes(16).toString('base64url');
const sub = (endpoint: string) => ({ endpoint, expirationTime: null, keys: { p256dh: key(), auth: auth() } });
const HOSTS = [
  'https://fcm.googleapis.com/fcm/send/heal-example',
  'https://updates.push.services.mozilla.com/wpush/v2/heal-example',
  'https://web.push.apple.com/aGVhbC1leGFtcGxl',
  'https://wns2-par02p.notify.windows.com/w/?token=heal-example',
];
const CREATED = { status: 201, body: { created: true }, cache: 'no-store', cookie: null };
const UPDATED = { status: 204, body: null, cache: 'no-store', cookie: null };

type Call = { status: number; body: unknown; cache: string | null; cookie: string | null; type?: string | null };
async function call(method: string, body: unknown, { origin = LOCAL as string | null, raw = false, handle = (r: Request) => app.request(r) as Promise<Response> } = {}): Promise<Call> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (origin) headers.Origin = origin;
  const res = await handle(new Request(`${LOCAL}/api/push/subscriptions`, { method, headers, body: raw ? body as string : JSON.stringify(body) }));
  const text = await res.text();
  let parsed: unknown = text;
  try { parsed = text ? JSON.parse(text) : null; } catch {}
  return { status: res.status, body: parsed, cache: res.headers.get('cache-control'), cookie: res.headers.get('set-cookie') };
}
const rows = () => memoryPushStore.list();

beforeEach(() => { vi.stubEnv('BETTER_AUTH_URL', ''); });
afterEach(() => { vi.unstubAllEnvs(); vi.restoreAllMocks(); });

describe('POST /api/push/subscriptions 201 · 204 (AC-14-5)', () => {
  it('① 4종 새 endpoint → 201 · 본문 {"created":true} 정확히 · JSON · no-store · Set-Cookie 0 · 행 4', async () => {
    for (const e of HOSTS) {
      const res = await app.request(new Request(`${LOCAL}/api/push/subscriptions`, { method: 'POST', headers: { 'Content-Type': 'application/json', Origin: LOCAL }, body: JSON.stringify(sub(e)) }));
      expect([res.status, await res.text(), res.headers.get('content-type'), res.headers.get('cache-control'), res.headers.get('set-cookie')], e)
        .toEqual([201, '{"created":true}', 'application/json', 'no-store', null]);
    }
    expect((await rows()).map(r => r.endpoint)).toEqual(HOSTS);
  });

  it('② 같은 endpoint 다시(같은 키 · 다른 키) → 204 · 본문 0 · 행 1(키 갱신 · id 그대로) / 같은 새 endpoint 두 요청 동시 → 201 하나 + 204 하나 · 행 1', async () => {
    const first = sub(HOSTS[0]);
    expect(await call('POST', first)).toEqual(CREATED);
    const [before] = await rows();
    expect(await call('POST', first)).toEqual(UPDATED);
    expect(await rows()).toEqual([before]);
    const other = sub(HOSTS[0]);
    expect(await call('POST', other)).toEqual(UPDATED);
    expect(await rows()).toEqual([{ ...before, p256dh: other.keys.p256dh, auth: other.keys.auth }]);
    const both = await Promise.all([call('POST', sub(HOSTS[1])), call('POST', sub(HOSTS[1]))]);
    expect(both.map(r => r.status).sort()).toEqual([201, 204]);
    expect((await rows()).filter(r => r.endpoint === HOSTS[1])).toHaveLength(1);
  });

  it('③ DELETE 뒤 같은 endpoint POST → 201 · 새 id(서버에서 지워진 구독을 다시 보냄 = 화면이 바꾸기를 하는 경우)', async () => {
    expect(await call('POST', sub(HOSTS[2]))).toEqual(CREATED);
    const [old] = await rows();
    expect(await call('DELETE', { endpoint: HOSTS[2] })).toEqual(UPDATED);
    expect(await rows()).toEqual([]);
    expect(await call('POST', sub(HOSTS[2]))).toEqual(CREATED);
    const [again] = await rows();
    expect(again.endpoint).toBe(HOSTS[2]);
    expect(again.id).not.toBe(old.id);
    expect(again.lastSentOn).toBeNull();
  });

  it('④ 5,000행이면 기존 endpoint 204(키 갱신) · 새 endpoint 503 "지금은 알림을 켤 수 없습니다" · 행 수 그대로', async () => {
    for (let i = 0; i < 5000; i++) await memoryStore.savePushSubscription({ endpoint: `https://fcm.googleapis.com/fcm/send/heal-${i}`, p256dh: 'x', auth: 'y' }, true);
    const again = sub('https://fcm.googleapis.com/fcm/send/heal-9');
    expect(await call('POST', again)).toEqual(UPDATED);
    expect((await rows()).find(r => r.endpoint.endsWith('heal-9'))!.p256dh).toBe(again.keys.p256dh);
    expect(await call('POST', sub(HOSTS[3]))).toEqual({ status: 503, body: FULL, cache: 'no-store', cookie: null });
    expect(await memoryStore.pushSubscriptionCount()).toBe(5000);
  });

  it('⑤ 형식 밖 endpoint 400 · Origin 없음 403 · 4,097B 413 = O5 문구 · no-store · 저장 0', async () => {
    expect(await call('POST', sub('https://evil.example/x'))).toEqual({ status: 400, body: BAD, cache: 'no-store', cookie: null });
    expect(await call('POST', sub(HOSTS[0]), { origin: null })).toEqual({ status: 403, body: BAD, cache: 'no-store', cookie: null });
    const big = JSON.stringify({ ...sub(HOSTS[0]), pad: '' });
    const sized = JSON.stringify({ ...JSON.parse(big), pad: 'x'.repeat(4097 - big.length) });
    expect(Buffer.byteLength(sized)).toBe(4097);
    expect(await call('POST', sized, { raw: true })).toEqual({ status: 413, body: BAD, cache: 'no-store', cookie: null });
    expect(await rows()).toEqual([]);
  });

  it('⑥ createBff를 지난 201 · 204 = 앱과 같은 상태 · 본문 · 머리(content-type · cache-control · set-cookie)', async () => {
    const handle = createBff(() => createApp(memoryStore));
    const raw = async (h: (r: Request) => Promise<Response>, s: unknown) => {
      const res = await h(new Request(`${LOCAL}/api/push/subscriptions`, { method: 'POST', headers: { 'Content-Type': 'application/json', Origin: LOCAL }, body: JSON.stringify(s) }));
      return { status: res.status, text: await res.text(), type: res.headers.get('content-type'), cache: res.headers.get('cache-control'), cookie: res.headers.get('set-cookie') };
    };
    const viaBff = [await raw(handle, sub(HOSTS[0])), await raw(handle, sub(HOSTS[0]))];
    await memoryPushStore.remove((await rows())[0].id);
    const viaApp = [await raw(r => app.request(r) as Promise<Response>, sub(HOSTS[0])), await raw(r => app.request(r) as Promise<Response>, sub(HOSTS[0]))];
    expect(viaBff).toEqual(viaApp);
    expect(viaBff).toEqual([
      { status: 201, text: '{"created":true}', type: 'application/json', cache: 'no-store', cookie: null },
      { status: 204, text: '', type: null, cache: 'no-store', cookie: null },
    ]);
  });

  it("⑦ memoryStore.savePushSubscription 직접: 새 = 'created' · 기존 = 'updated' · allowNew 거짓 + 새 = null(쓰기 0) · allowNew 거짓 + 기존 = 'updated'", async () => {
    const a = { endpoint: HOSTS[0], p256dh: 'p1', auth: 'a1' };
    expect(await memoryStore.savePushSubscription(a, true)).toBe('created');
    expect(await memoryStore.savePushSubscription({ ...a, p256dh: 'p2' }, true)).toBe('updated');
    expect(await memoryStore.savePushSubscription({ endpoint: HOSTS[1], p256dh: 'p', auth: 'a' }, false)).toBeNull();
    expect(await memoryStore.savePushSubscription({ ...a, auth: 'a3' }, false)).toBe('updated');
    expect((await rows()).map(r => [r.endpoint, r.p256dh, r.auth])).toEqual([[HOSTS[0], 'p1', 'a3']]);
  });
});

// ── 서비스 워커 pushsubscriptionchange(AC-14-5 · 회귀 AC-13-2) ──
const KEY_BYTES = [...Buffer.from(VAPID_PUBLIC_KEY, 'base64url')];
const OLD = 'https://fcm.googleapis.com/fcm/send/sw-old';
const NEW = 'https://fcm.googleapis.com/fcm/send/sw-new';
const KEYS = { p256dh: 'BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4', auth: 'BTBZMqHH6r4Tts7J_aSIgg' };
const swSub = (endpoint: string) => ({ endpoint, toJSON: () => ({ endpoint, expirationTime: null, keys: KEYS }) });

function loadSw(opts: { subscribe?: () => Promise<unknown>; respond?: (method: string) => Promise<Response> } = {}) {
  const listeners: Record<string, (event: unknown) => void> = {};
  const touched: string[] = []; // 맨 위 실행 중 registration · atob 접근
  let loading = true;
  const subscribe = vi.fn(async (_options: unknown) => (opts.subscribe ? opts.subscribe() : swSub(NEW)));
  const registration = { pushManager: { subscribe }, showNotification: vi.fn(), getNotifications: vi.fn(async () => []) };
  const caches = { open: vi.fn(), keys: vi.fn(), delete: vi.fn(), match: vi.fn() };
  const self: Record<string, unknown> = { addEventListener: (type: string, fn: (event: unknown) => void) => { listeners[type] = fn; }, location: { origin: LOCAL }, clients: {}, skipWaiting: vi.fn() };
  Object.defineProperty(self, 'registration', { get: () => { if (loading) touched.push('registration'); return registration; } });
  const requests: { url: string; method: string; headers: unknown; body: string }[] = [];
  const fetchMock = vi.fn(async (url: string, init: { method: string; headers: unknown; body: string }) => {
    requests.push({ url, method: init.method, headers: init.headers, body: init.body });
    return opts.respond ? opts.respond(init.method) : new Response(null, { status: init.method === 'POST' ? 201 : 204 });
  });
  const context: Record<string, unknown> = { self, caches, fetch: fetchMock, URL, Promise, Uint8Array };
  Object.defineProperty(context, 'atob', { enumerable: true, get: () => { if (loading) touched.push('atob'); return atob; } });
  runInContext(readFileSync('public/sw.js', 'utf8'), createContext(context));
  loading = false;
  // 이벤트 1번: waitUntil 수 · 그 약속의 끝(거절이면 'rejected')
  const fire = async (event: Record<string, unknown>) => {
    const waits: Promise<unknown>[] = [];
    listeners.pushsubscriptionchange({ ...event, waitUntil: (p: Promise<unknown>) => { waits.push(p); } });
    const settled = await Promise.all(waits.map(p => p.then(() => 'resolved', () => 'rejected')));
    return { waits: waits.length, settled };
  };
  const summary = () => requests.map(r => `${r.method} ${JSON.parse(r.body).endpoint}`);
  return { listeners, touched, subscribe, registration, caches, requests, fire, summary };
}
const done = { waits: 1, settled: ['resolved'] };

describe('sw.js pushsubscriptionchange (AC-14-5)', () => {
  it('① old · new 둘 다 null(Chrome 권한 다시 허용) ② 두 칸 없음(Firefox) → subscribe 1(userVisibleOnly · 키 65B = 코드 키) → POST 1(본문 = toJSON · JSON 머리) · DELETE 0', async () => {
    for (const event of [{ oldSubscription: null, newSubscription: null }, {}]) {
      const sw = loadSw();
      expect(await sw.fire(event)).toEqual(done);
      expect(sw.subscribe).toHaveBeenCalledTimes(1);
      const arg = sw.subscribe.mock.calls[0][0] as { userVisibleOnly: boolean; applicationServerKey: Uint8Array };
      expect([arg.userVisibleOnly, arg.applicationServerKey.length, [...arg.applicationServerKey]]).toEqual([true, 65, KEY_BYTES]);
      expect(sw.requests).toEqual([{ url: '/api/push/subscriptions', method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(swSub(NEW).toJSON()) }]);
    }
  });

  it('③ old + new(다른 endpoint) → subscribe 0 · POST new · DELETE { endpoint: old }', async () => {
    const sw = loadSw();
    expect(await sw.fire({ oldSubscription: swSub(OLD), newSubscription: swSub(NEW) })).toEqual(done);
    expect(sw.subscribe).not.toHaveBeenCalled();
    expect(sw.summary()).toEqual([`POST ${NEW}`, `DELETE ${OLD}`]);
    expect(sw.requests[1]).toEqual({ url: '/api/push/subscriptions', method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ endpoint: OLD }) });
  });

  it('④ old + new 없음 → subscribe 1 · POST · DELETE old', async () => {
    for (const event of [{ oldSubscription: swSub(OLD), newSubscription: null }, { oldSubscription: swSub(OLD) }]) {
      const sw = loadSw();
      expect(await sw.fire(event)).toEqual(done);
      expect(sw.subscribe).toHaveBeenCalledTimes(1);
      expect(sw.summary()).toEqual([`POST ${NEW}`, `DELETE ${OLD}`]);
    }
  });

  it('⑤ old = new(같은 endpoint) → POST 1 · DELETE 0 · subscribe 0', async () => {
    const sw = loadSw();
    expect(await sw.fire({ oldSubscription: swSub(OLD), newSubscription: swSub(OLD) })).toEqual(done);
    expect([sw.subscribe.mock.calls.length, sw.summary()]).toEqual([0, [`POST ${OLD}`]]);
  });

  it('⑥ subscribe 거절(NotAllowedError — 권한 꺼짐) + old → POST 0 · DELETE old 1 · 거절 없이 끝남 / old도 없으면 요청 0', async () => {
    const denied = async () => { throw new DOMException('Registration failed - permission denied', 'NotAllowedError'); };
    const sw = loadSw({ subscribe: denied });
    expect(await sw.fire({ oldSubscription: swSub(OLD), newSubscription: null })).toEqual(done);
    expect([sw.subscribe.mock.calls.length, sw.summary()]).toEqual([1, [`DELETE ${OLD}`]]);
    const none = loadSw({ subscribe: denied });
    expect(await none.fire({})).toEqual(done);
    expect(none.requests).toEqual([]);
  });

  it('⑦ old + new에서 POST 500 · 네트워크 실패 → 거절 없이 끝남 · DELETE old 그대로 1', async () => {
    for (const respond of [
      async (method: string) => new Response(method === 'POST' ? 'Internal Server Error' : null, { status: method === 'POST' ? 500 : 204 }),
      async (method: string) => { if (method === 'POST') throw new TypeError('Failed to fetch'); return new Response(null, { status: 204 }); },
      async () => { throw new TypeError('Failed to fetch'); },
    ]) {
      const sw = loadSw({ respond });
      expect(await sw.fire({ oldSubscription: swSub(OLD), newSubscription: swSub(NEW) })).toEqual(done);
      expect(sw.summary()).toEqual([`POST ${NEW}`, `DELETE ${OLD}`]);
    }
  });

  it('⑧ 정적 · 맨 위: 공개 키 상수 = VAPID_PUBLIC_KEY(87자, 파일에 1번) · 맨 위 실행에서 registration · atob 접근 0 · 처리기는 showNotification · caches 호출 0', async () => {
    const source = readFileSync('public/sw.js', 'utf8');
    const constKey = /const PUSH_KEY = '([\w-]+)'; \/\/ server\/push-config\.ts VAPID_PUBLIC_KEY와 같은 값\(테스트가 비교\)/.exec(source)![1];
    expect(constKey).toBe(VAPID_PUBLIC_KEY);
    expect(constKey).toHaveLength(87);
    expect(source.match(/[\w-]{87}/g)!.filter(s => s === VAPID_PUBLIC_KEY)).toHaveLength(1);
    const sw = loadSw();
    expect(sw.touched).toEqual([]);
    expect(Object.keys(sw.listeners).sort()).toEqual(['activate', 'fetch', 'install', 'notificationclick', 'push', 'pushsubscriptionchange']);
    await sw.fire({ oldSubscription: swSub(OLD), newSubscription: null });
    expect([sw.registration.showNotification.mock.calls.length, sw.registration.getNotifications.mock.calls.length]).toEqual([0, 0]);
    expect(Object.values(sw.caches).map(f => f.mock.calls.length)).toEqual([0, 0, 0, 0]);
    // 맨 위 주석: v3 · fetch 처리 없는 판에도 pushsubscriptionchange를 남김
    expect(source).toContain('// 같은 주소에 fetch 처리 없는 판으로 바꿀 때도 push · notificationclick · pushsubscriptionchange는 남김(빼면 아침 알림이 깨짐)');
    expect(source).toContain("const CACHE = 'issuerank-v3';");
  });
});
