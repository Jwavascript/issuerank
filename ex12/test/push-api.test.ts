// 아침 알림 구독 API(O5 · D30 · AC-14-1 · 14-4): POST · DELETE /api/push/subscriptions를 메모리 저장소 Hono 앱(app.request)과 BFF(createBff)로.
// 세션 · 쿠키 없이 동작 · 검사 순서 Origin → 크기 → JSON · 칸 → endpoint → 키 · 모든 응답 no-store · Set-Cookie 0
import { createECDH, randomBytes } from 'node:crypto';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createApp, type Store } from '../server/app';
import { createBff } from '../server/bff';
import { memoryPushStore, memoryStore } from '../server/sample';

const LOCAL = 'http://localhost:3000', DEPLOY = 'https://issuerank-kappa.vercel.app';
const BAD = { message: '요청 형식이 올바르지 않습니다' };
const FULL = { message: '지금은 알림을 켤 수 없습니다' };
const app = createApp(memoryStore);
const key = () => { const e = createECDH('prime256v1'); e.generateKeys(); return e.getPublicKey().toString('base64url'); };
const auth = () => randomBytes(16).toString('base64url');
const sub = (endpoint: string, extra: Record<string, unknown> = {}) => ({ endpoint, expirationTime: null, keys: { p256dh: key(), auth: auth() }, ...extra });

type Call = { status: number; body: unknown; cache: string | null; cookie: string | null };
async function call(method: string, body: unknown, { origin = LOCAL as string | null, raw = false, handle = (r: Request) => app.request(r) as Promise<Response> } = {}): Promise<Call> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (origin) headers.Origin = origin;
  const res = await handle(new Request(`${LOCAL}/api/push/subscriptions`, { method, headers, ...(method === 'GET' ? {} : { body: raw ? body as string : JSON.stringify(body) }) }));
  const text = await res.text();
  let parsed: unknown = text;
  try { parsed = text ? JSON.parse(text) : null; } catch {}
  return { status: res.status, body: parsed, cache: res.headers.get('cache-control'), cookie: res.headers.get('set-cookie') };
}
const rows = () => memoryPushStore.list();
const HOSTS = [
  'https://fcm.googleapis.com/fcm/send/dGVzdA:APA91b-example',
  'https://updates.push.services.mozilla.com/wpush/v2/gAAAAAB-example',
  'https://web.push.apple.com/QGx0bHlvLWV4YW1wbGU',
  'https://wns2-par02p.notify.windows.com/w/?token=BQYAAAB-example',
];

beforeEach(() => { vi.stubEnv('BETTER_AUTH_URL', ''); });
afterEach(() => { vi.unstubAllEnvs(); vi.restoreAllMocks(); });

describe('POST /api/push/subscriptions (AC-14-1)', () => {
  it('알려진 푸시 서비스 4종 → 201 · { created: true } · no-store · Set-Cookie 0 · 세션 없이 · 행 4 · last_sent_on null', async () => {
    for (const e of HOSTS) expect(await call('POST', sub(e))).toEqual({ status: 201, body: { created: true }, cache: 'no-store', cookie: null });
    const saved = await rows();
    expect(saved.map(r => r.endpoint)).toEqual(HOSTS);
    expect(saved.every(r => r.lastSentOn === null && /^[0-9a-f-]{36}$/.test(r.id))).toBe(true);
  });

  it('같은 endpoint 다시(키 바꿈) → 204 · 행 1 · 키만 갱신(id 그대로) · 다른 칸 · expirationTime 무시', async () => {
    const first = sub(HOSTS[0]), second = sub(HOSTS[0], { expirationTime: 1790000000000, extra: 'x', keys: { p256dh: key(), auth: auth(), more: 1 } });
    expect((await call('POST', first)).status).toBe(201);
    const [before] = await rows();
    expect((await call('POST', second)).status).toBe(204);
    const after = await rows();
    expect(after).toHaveLength(1);
    expect(after[0]).toEqual({ ...before, p256dh: second.keys.p256dh, auth: second.keys.auth });
    expect(Object.keys(after[0]).sort()).toEqual(['auth', 'endpoint', 'id', 'lastSentOn', 'p256dh']);
  });

  it('400 묶음(같은 문구 · no-store · 저장 0): http · 꼬리 붙은 호스트 · 접미사 아닌 apple · IP · localhost · 사용자 정보 · 포트 · 옛 GCM · 2,049자 · 키 형식 · 곡선 밖 점 · 깨진 JSON', async () => {
    const offCurve = Buffer.concat([Buffer.from([4]), Buffer.alloc(64, 1)]).toString('base64url');
    const long = `https://fcm.googleapis.com/fcm/send/${'a'.repeat(2049 - 'https://fcm.googleapis.com/fcm/send/'.length)}`;
    const okKeys = { p256dh: key(), auth: auth() };
    const bodies: [string, unknown][] = [
      ['http', sub('http://fcm.googleapis.com/fcm/send/x')],
      ['꼬리 호스트', sub('https://fcm.googleapis.com.evil.example/x')],
      ['evilpush.apple.com', sub('https://evilpush.apple.com/x')],
      ['접미사만', sub('https://.push.apple.com/x')],
      ['IP', sub('https://127.0.0.1/')],
      ['IPv6', sub('https://[::1]/x')],
      ['localhost', sub('https://localhost/')],
      ['사용자 정보', sub('https://u@fcm.googleapis.com/x')],
      ['빈 사용자 정보', sub('https://@fcm.googleapis.com/x')],
      ['포트', sub('https://fcm.googleapis.com:8443/x')],
      ['기본 포트 표기', sub('https://fcm.googleapis.com:443/x')],
      ['옛 GCM', sub('https://android.googleapis.com/gcm/send/x')],
      ['스테이징', sub('https://updates-autopush.stage.mozaws.net/wpush/v2/x')],
      ['끝 점', sub('https://fcm.googleapis.com./x')],
      ['#', sub('https://fcm.googleapis.com/x#y')],
      ['공백', sub('https://fcm.googleapis.com/x y')],
      ['제어 문자', sub('https://fcm.googleapis.com/x\u0007')],
      ['역슬래시', sub('https://fcm.googleapis.com\\@evil.example/x')],
      ['원형 속성 constructor', sub('https://constructor/x')],
      ['원형 속성 __proto__', sub('https://__proto__/x')],
      ['2,049자', sub(long)],
      ['endpoint 숫자', { endpoint: 1, keys: okKeys }],
      ['endpoint 없음', { keys: okKeys }],
      ['keys 없음', { endpoint: HOSTS[0] }],
      ['keys 문자열', { endpoint: HOSTS[0], keys: 'x' }],
      ['p256dh 64B', { endpoint: HOSTS[0], keys: { p256dh: Buffer.from(key(), 'base64url').subarray(0, 64).toString('base64url'), auth: auth() } }],
      ['p256dh 첫 바이트 2', { endpoint: HOSTS[0], keys: { p256dh: Buffer.concat([Buffer.from([2]), Buffer.from(key(), 'base64url').subarray(1)]).toString('base64url'), auth: auth() } }],
      ['곡선 밖 점', { endpoint: HOSTS[0], keys: { p256dh: offCurve, auth: auth() } }],
      ['p256dh base64 표준(+/)', { endpoint: HOSTS[0], keys: { p256dh: Buffer.from(key(), 'base64url').toString('base64'), auth: auth() } }],
      ['auth 15B', { endpoint: HOSTS[0], keys: { p256dh: key(), auth: randomBytes(15).toString('base64url') } }],
      ['auth 17B', { endpoint: HOSTS[0], keys: { p256dh: key(), auth: randomBytes(17).toString('base64url') } }],
      ['배열', [sub(HOSTS[0])]],
      ['null', null],
    ];
    for (const [label, body] of bodies) expect(await call('POST', body), label).toEqual({ status: 400, body: BAD, cache: 'no-store', cookie: null });
    for (const raw of ['{', 'not json', '', '{"endpoint":']) expect(await call('POST', raw, { raw: true }), raw).toEqual({ status: 400, body: BAD, cache: 'no-store', cookie: null });
    // 2,048자는 받음(경계)
    expect((await call('POST', sub(long.slice(0, 2048)))).status).toBe(201);
    expect((await rows()).map(r => r.endpoint.length)).toEqual([2048]);
  });

  it('Origin 없음 · 다른 사이트 · http/https 다름 → 403(같은 문구) · 저장 0 / BETTER_AUTH_URL = 배포 주소면 배포 Origin만', async () => {
    for (const origin of [null, 'https://evil.example', 'https://localhost:3000', 'http://127.0.0.1:3000', 'null']) {
      expect(await call('POST', sub(HOSTS[0]), { origin }), String(origin)).toEqual({ status: 403, body: BAD, cache: 'no-store', cookie: null });
    }
    expect(await rows()).toEqual([]);
    vi.stubEnv('BETTER_AUTH_URL', DEPLOY);
    expect((await call('POST', sub(HOSTS[0]), { origin: LOCAL })).status).toBe(403);
    expect((await call('POST', sub(HOSTS[0]), { origin: DEPLOY })).status).toBe(201);
    vi.stubEnv('BETTER_AUTH_URL', 'not a url');
    expect((await call('POST', sub(HOSTS[1]), { origin: LOCAL })).status).toBe(403);
    expect(await rows()).toHaveLength(1);
  });

  it('본문 4,097B → 413(같은 문구) · 4,096B는 지나감 · Origin이 먼저(다른 Origin + 큰 본문 = 403)', async () => {
    const sized = (n: number) => { const b = sub(HOSTS[0], { pad: '' }); const base = JSON.stringify(b).length; return JSON.stringify({ ...b, pad: 'x'.repeat(n - base) }); };
    expect(Buffer.byteLength(sized(4097))).toBe(4097);
    expect(await call('POST', sized(4097), { raw: true })).toEqual({ status: 413, body: BAD, cache: 'no-store', cookie: null });
    expect((await call('POST', sized(4097), { raw: true, origin: 'https://evil.example' })).status).toBe(403);
    expect(await rows()).toEqual([]);
    expect((await call('POST', sized(4096), { raw: true })).status).toBe(201);
    expect(await call('DELETE', JSON.stringify({ endpoint: HOSTS[0], pad: 'x'.repeat(4097) }), { raw: true })).toEqual({ status: 413, body: BAD, cache: 'no-store', cookie: null });
    expect(await rows()).toHaveLength(1);
  });

  it('5,000행이면 새 endpoint 503 "지금은 알림을 켤 수 없습니다" · 기존 endpoint 키 갱신은 204', async () => {
    for (let i = 0; i < 5000; i++) await memoryStore.savePushSubscription({ endpoint: `https://fcm.googleapis.com/fcm/send/fill-${i}`, p256dh: 'x', auth: 'y' }, true);
    expect(await memoryStore.pushSubscriptionCount()).toBe(5000);
    expect(await call('POST', sub(HOSTS[0]))).toEqual({ status: 503, body: FULL, cache: 'no-store', cookie: null });
    const again = sub('https://fcm.googleapis.com/fcm/send/fill-7');
    expect((await call('POST', again)).status).toBe(204);
    expect(await memoryStore.pushSubscriptionCount()).toBe(5000);
    expect((await rows()).find(r => r.endpoint.endsWith('fill-7'))!.p256dh).toBe(again.keys.p256dh);
  });

  it('저장소 실패 → 500 · 로그 = PUSH_SAVE_FAILED 한 줄(endpoint · 키 · 오류 문구 0)', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    const broken: Store = { ...memoryStore, pushSubscriptionCount: async () => { throw new Error('postgres://user:PRIVATE@db/x'); } };
    const res = await call('POST', sub(HOSTS[0]), { handle: r => createApp(broken).request(r) as Promise<Response> });
    expect([res.status, res.cache, JSON.stringify(res.body).includes('PRIVATE')]).toEqual([500, 'no-store', false]);
    expect(log.mock.calls).toEqual([['PUSH_SAVE_FAILED']]);
  });
});

describe('DELETE /api/push/subscriptions (AC-14-4) · 그 밖 메서드 · BFF', () => {
  it('있음 → 204 · 행 0 / 없음 → 204 / 두 번 → 204 · 400(깨진 JSON · endpoint 없음 · 받지 않는 호스트) · 403', async () => {
    const s = sub(HOSTS[2]);
    expect((await call('POST', s)).status).toBe(201);
    expect((await call('POST', sub(HOSTS[3]))).status).toBe(201);
    expect(await call('DELETE', { endpoint: HOSTS[2] })).toEqual({ status: 204, body: null, cache: 'no-store', cookie: null });
    expect((await rows()).map(r => r.endpoint)).toEqual([HOSTS[3]]);
    for (let i = 0; i < 2; i++) expect(await call('DELETE', { endpoint: HOSTS[2] })).toEqual({ status: 204, body: null, cache: 'no-store', cookie: null });
    for (const body of ['{', '{}', JSON.stringify({ endpoint: 'https://evil.example/x' }), JSON.stringify({ endpoint: 3 })]) {
      expect(await call('DELETE', body, { raw: true }), body).toEqual({ status: 400, body: BAD, cache: 'no-store', cookie: null });
    }
    expect((await call('DELETE', { endpoint: HOSTS[3] }, { origin: null })).status).toBe(403);
    expect((await call('DELETE', { endpoint: HOSTS[3] }, { origin: 'https://evil.example' })).status).toBe(403);
    expect((await rows()).map(r => r.endpoint)).toEqual([HOSTS[3]]);
  });

  it('GET · PUT · PATCH → 404(Hono 기본) · no-store', async () => {
    for (const method of ['GET', 'PUT', 'PATCH']) {
      const res = await call(method, method === 'GET' ? undefined : sub(HOSTS[0]));
      expect([res.status, res.cache], method).toEqual([404, 'no-store']);
    }
    expect(await rows()).toEqual([]);
  });

  it('createBff를 지난 POST · DELETE · 403 · 400 = 앱과 같은 상태 · 본문 · no-store · Set-Cookie 0', async () => {
    const handle = createBff(() => createApp(memoryStore));
    const via = { handle: (r: Request) => handle(r) };
    expect(await call('POST', sub(HOSTS[0]), via)).toEqual({ status: 201, body: { created: true }, cache: 'no-store', cookie: null });
    expect(await call('POST', sub('https://evil.example/x'), via)).toEqual({ status: 400, body: BAD, cache: 'no-store', cookie: null });
    expect(await call('POST', sub(HOSTS[1]), { ...via, origin: null })).toEqual({ status: 403, body: BAD, cache: 'no-store', cookie: null });
    expect(await call('DELETE', { endpoint: HOSTS[0] }, via)).toEqual({ status: 204, body: null, cache: 'no-store', cookie: null });
    expect(await rows()).toEqual([]);
  });
});
