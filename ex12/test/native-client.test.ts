// Android 앱 요청 꼴(O8 · D35 · AC-15-2 · 15-3): 실제 Better Auth 1.7.7 + 격리 메모리 어댑터 + createApp(memoryStore)을 app.request로 부름(시각은 setup 고정)
// 앱은 비브라우저 → Cookie · Origin을 직접 붙이고 Fetch Metadata(Sec-Fetch-*)는 없음. Google ID 토큰은 시험 RSA 키로 서명하고
// Google 공개 키(JWKS)는 가짜 전역 fetch가 줌 — 실제 Google 요청 0 · DB 0
import { generateKeyPairSync, sign, type KeyObject } from 'node:crypto';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { betterAuth } from 'better-auth';
import { memoryAdapter, type MemoryDB } from 'better-auth/adapters/memory';
import { authOptions } from '../server/auth';
import { createApp } from '../server/app';
import { memoryStore } from '../server/sample';

const SITE = 'http://localhost:3000';
const EVIL = 'https://evil.example';
const env = { BETTER_AUTH_SECRET: 'test-only-o8-native-client-5e1b9c4a7d', BETTER_AUTH_URL: SITE };
// 가짜 Google 클라이언트 값(시험 전용 — 웹 클라이언트 ID 자리)
const GOOGLE = { GOOGLE_CLIENT_ID: 'o8-test-web.apps.googleusercontent.com', GOOGLE_CLIENT_SECRET: 'o8-test-only-secret-7c3e9a15' };
const APP = { 'User-Agent': 'issuerank-android/1.0.0', Accept: 'application/json' };
const user = { name: '앱 사용자', email: 'app.user@example.invalid', password: 'App-test-only-pass-5518' };
const login = { email: user.email, password: user.password };

type Call = (method: string, path: string, opts?: { body?: unknown; cookie?: string; headers?: Record<string, string> }) => Promise<Response>;
function setup(extra: Record<string, string> = GOOGLE, base = SITE) {
  const db: MemoryDB = { user: [], session: [], account: [], verification: [] };
  const auth = betterAuth({ ...authOptions({ ...env, BETTER_AUTH_URL: base, ...extra }), database: memoryAdapter(db) });
  const app = createApp(memoryStore, auth);
  // 앱처럼 머리는 준 것만(Origin · Cookie를 저절로 붙이지 않음)
  const call: Call = async (method, path, { body, cookie, headers } = {}) => app.request(base + path, {
    method,
    headers: { ...APP, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(cookie ? { Cookie: cookie } : {}), ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { db, call };
}
type SetCookie = { name: string; value: string; attrs: string[] };
const setCookies = (res: Response): SetCookie[] => res.headers.getSetCookie().map(line => {
  const [pair, ...attrs] = line.split(';').map(s => s.trim());
  const i = pair.indexOf('=');
  return { name: pair.slice(0, i), value: pair.slice(i + 1), attrs: attrs.map(a => a.toLowerCase()) };
});
// 앱 쿠키 보관처럼: 값이 있고 Max-Age=0이 아닌 것만 다음 요청에
const jar = (res: Response) => setCookies(res).filter(c => c.value && !c.attrs.includes('max-age=0')).map(c => `${c.name}=${c.value}`).join('; ');
const counts = (db: MemoryDB) => [db.user.length, db.account.length, db.session.length, db.verification.length];
const signUp = async (call: Call, who = user) => {
  const res = await call('POST', '/api/auth/sign-up/email', { body: who });
  expect(res.status).toBe(200);
  return jar(res);
};

// 전역 fetch 가짜: Google 공개 키 주소 GET에만 시험 JWKS, 그 밖은 실패. 불린 요청은 모두 기록(실제 바깥 요청 0)
const CERTS = 'https://www.googleapis.com/oauth2/v3/certs';
const KID = 'o8-test-kid';
const testKey = generateKeyPairSync('rsa', { modulusLength: 2048 });
const otherKey = generateKeyPairSync('rsa', { modulusLength: 2048 });
const JWKS = { keys: [{ ...testKey.publicKey.export({ format: 'jwk' }), kid: KID, alg: 'RS256', use: 'sig' }] };
let calls: string[] = [];
// ⑪ 콘솔: 안전 코드 한 줄씩뿐(토큰 · 이메일 · nonce 0)
let logs: unknown[][] = [];
beforeEach(() => {
  calls = [];
  vi.stubGlobal('fetch', vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const req = new Request(input, init);
    calls.push(`${req.method} ${req.url}`);
    if (req.method === 'GET' && req.url === CERTS) return Response.json(JWKS);
    throw new TypeError('UNEXPECTED_FETCH');
  }));
  logs = [];
  for (const method of ['error', 'warn', 'log', 'info', 'debug'] as const) vi.spyOn(console, method).mockImplementation((...args: unknown[]) => { logs.push([method, ...args]); });
});
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });
const expectSafeLogs = (...secrets: string[]) => {
  expect(logs.every(([method, ...args]) => method === 'error' && args.length === 1 && /^(AUTH_INTERNAL_ERROR|API_INTERNAL_ERROR)$/.test(String(args[0])))).toBe(true);
  const text = JSON.stringify(logs);
  for (const secret of [GOOGLE.GOOGLE_CLIENT_SECRET, NONCE, '@', ...secrets]) expect(text).not.toContain(secret);
};

describe('앱 세션 = 쿠키 · POST = Origin(D35) — 실제 Better Auth + 메모리 어댑터', () => {
  it('① 쿠키 · Origin 없는 가입 · 로그인: 200 + 세션 쿠키 1개(로컬 http = better-auth.session_token, https = __Secure- · Secure)', async () => {
    for (const [base, name, secure] of [[SITE, 'better-auth.session_token', false], ['https://app-test.example.invalid', '__Secure-better-auth.session_token', true]] as const) {
      const { db, call } = setup(GOOGLE, base);
      for (const [path, body] of [['sign-up/email', user], ['sign-in/email', login]] as const) {
        const res = await call('POST', '/api/auth/' + path, { body });
        expect([path, res.status, res.headers.get('cache-control')]).toEqual([path, 200, 'no-store']);
        const set = setCookies(res);
        expect(set.map(c => c.name)).toEqual([name]);
        expect(set[0].attrs).toEqual(expect.arrayContaining(['max-age=604800', 'path=/', 'httponly', 'samesite=lax']));
        expect(set[0].attrs.includes('secure')).toBe(secure);
        expect((await res.json()).user.email).toBe(user.email);
      }
      expect(counts(db)).toEqual([1, 1, 2, 0]);
    }
    expect(calls).toEqual([]);
    expectSafeLogs(user.password);
  });

  it('② 쿠키만(Origin 없음): get-session 200 · 북마크 · 폴더 GET 200 · POST 201 · DELETE 204 — 개인 API(Hono)는 세션만 봄', async () => {
    const { call } = setup();
    const cookie = await signUp(call);
    const me = await call('GET', '/api/auth/get-session', { cookie });
    expect(me.status).toBe(200);
    expect((await me.json()).user.email).toBe(user.email);
    for (const path of ['/api/bookmarks', '/api/folders']) {
      const res = await call('GET', path, { cookie });
      expect([path, res.status, await res.json()]).toEqual([path, 200, []]);
    }
    const folder = await call('POST', '/api/folders', { cookie, body: { name: '앱 폴더' } });
    expect(folder.status).toBe(201);
    const folderId: string = (await folder.json()).id;
    const issueId = (await memoryStore.ranking('2026-10-07'))!.sections[0].issues[0].id;
    expect((await call('POST', '/api/bookmarks', { cookie, body: { issueId, folderId } })).status).toBe(201);
    expect(await (await call('GET', '/api/folders', { cookie })).json()).toEqual([{ id: folderId, name: '앱 폴더', count: 1 }]);
    const removed = await call('DELETE', '/api/bookmarks/' + issueId, { cookie });
    expect([removed.status, await removed.text()]).toEqual([204, '']);
    const dropped = await call('DELETE', '/api/folders/' + folderId, { cookie });
    expect([dropped.status, await dropped.text()]).toEqual([204, '']);
    expect(await (await call('GET', '/api/folders', { cookie })).json()).toEqual([]);
    // 쿠키가 없으면 401(앱은 로그인 안내)
    const anon = await call('GET', '/api/bookmarks');
    expect([anon.status, await anon.json()]).toEqual([401, { message: '로그인이 필요합니다' }]);
    expect(calls).toEqual([]);
    expectSafeLogs(user.password);
  });

  it('③ 쿠키가 있는 sign-out: Origin · Referer 없음 · Origin "null" = 403 MISSING_OR_NULL_ORIGIN(세션 그대로) → Origin = 사이트 = 200 · 세션 쿠키 만료 · 세션 행 0', async () => {
    const { db, call } = setup();
    const cookie = await signUp(call);
    const missing: Record<string, string>[] = [{}, { Origin: 'null' }];
    for (const headers of missing) {
      const res = await call('POST', '/api/auth/sign-out', { cookie, headers });
      expect([res.status, (await res.json()).code]).toEqual([403, 'MISSING_OR_NULL_ORIGIN']);
      expect(res.headers.getSetCookie()).toEqual([]);
    }
    expect(db.session).toHaveLength(1);
    expect((await (await call('GET', '/api/auth/get-session', { cookie })).json()).user.email).toBe(user.email);
    // 본문 없는 POST(앱 꼴)도 됨. 만료 쿠키는 세션 말고도 라이브러리 쿠키 3개(session_data · oauth_state · dont_remember)
    const out = await call('POST', '/api/auth/sign-out', { cookie, headers: { Origin: SITE } });
    expect([out.status, await out.json()]).toEqual([200, { success: true }]);
    const expired = setCookies(out);
    expect(expired.map(c => c.name)).toEqual(['better-auth.session_token', 'better-auth.session_data', 'better-auth.oauth_state', 'better-auth.dont_remember']);
    expect(expired.every(c => c.value === '' && c.attrs.includes('max-age=0'))).toBe(true);
    expect(db.session).toHaveLength(0);
    expect(await (await call('GET', '/api/auth/get-session', { cookie })).json()).toBeNull();
    // Origin이 없으면 Referer를 봄 — 사이트 페이지 주소면 통과
    const again = await call('POST', '/api/auth/sign-in/email', { body: login });
    const byReferer = await call('POST', '/api/auth/sign-out', { cookie: jar(again), headers: { Referer: SITE + '/#bookmarks' } });
    expect(byReferer.status).toBe(200);
    expect(db.session).toHaveLength(0);
    expect(calls).toEqual([]);
    expectSafeLogs(user.password);
  });

  it('④ 다른 Origin = 403 INVALID_ORIGIN · 새 행 0: 가입 · 로그인(쿠키 없어도) · 쿠키가 있는 로그아웃 · Google / Fetch Metadata가 있으면 Origin 필요', async () => {
    const { db, call } = setup();
    const cookie = await signUp(call);
    const before = counts(db);
    const cases: [string, unknown, string | undefined, Record<string, string>][] = [
      ['sign-up/email', { ...user, email: 'second@example.invalid' }, undefined, { Origin: EVIL }],
      ['sign-in/email', login, undefined, { Origin: EVIL }],
      ['sign-out', undefined, cookie, { Origin: EVIL }],
      ['sign-out', undefined, cookie, { Referer: EVIL + '/' }],
      ['sign-in/social', { provider: 'google', idToken: { token: 'x.y.z' } }, cookie, { Origin: EVIL }],
    ];
    for (const [path, body, c, headers] of cases) {
      const res = await call('POST', '/api/auth/' + path, { body, cookie: c, headers });
      expect([path, res.status, (await res.json()).code]).toEqual([path, 403, 'INVALID_ORIGIN']);
      expect(res.headers.getSetCookie()).toEqual([]);
    }
    // Fetch Metadata(브라우저 머리)가 있으면 쿠키가 없어도 Origin 검사: 다른 사이트 이동 = 차단 · same-origin인데 Origin 없음 = MISSING_OR_NULL_ORIGIN
    for (const [headers, code] of [[{ 'Sec-Fetch-Site': 'cross-site', 'Sec-Fetch-Mode': 'navigate' }, 'CROSS_SITE_NAVIGATION_LOGIN_BLOCKED'], [{ 'Sec-Fetch-Site': 'same-origin' }, 'MISSING_OR_NULL_ORIGIN']] as const) {
      const res = await call('POST', '/api/auth/sign-in/email', { body: login, headers });
      expect([res.status, (await res.json()).code]).toEqual([403, code]);
    }
    expect(counts(db)).toEqual(before);
    expect((await (await call('GET', '/api/auth/get-session', { cookie })).json()).user.email).toBe(user.email);
    expect(calls).toEqual([]);
    expectSafeLogs(user.password);
  });
});

// ── ⑤ Google ID 토큰 로그인(D35): 시험 RSA 키(2048) 서명 + 가짜 Google 공개 키. aud = GOOGLE_CLIENT_ID(웹 클라이언트 ID) ──
const NONCE = 'o8-test-nonce-4d1f0b';
const b64url = (value: object) => Buffer.from(JSON.stringify(value)).toString('base64url');
const nowSec = () => Math.floor(Date.now() / 1000);
type Claims = Record<string, unknown>;
const claims = (over: Claims = {}): Claims => ({
  iss: 'https://accounts.google.com', aud: GOOGLE.GOOGLE_CLIENT_ID, azp: 'o8-test-android.apps.googleusercontent.com',
  sub: '110000000000000000001', email: 'O8.Google@Example.Invalid', email_verified: true, name: '  구글 앱 사용자  ',
  picture: 'https://lh3.googleusercontent.com/a/o8-fake-picture', iat: nowSec(), exp: nowSec() + 3600, nonce: NONCE, ...over,
});
function idToken(payload: Claims, { key = testKey.privateKey, alg = 'RS256', kid = KID }: { key?: KeyObject; alg?: string; kid?: string } = {}) {
  const data = `${b64url({ alg, kid, typ: 'JWT' })}.${b64url(payload)}`;
  if (alg === 'none') return `${data}.`;
  return `${data}.${sign(alg === 'RS512' ? 'sha512' : 'sha256', Buffer.from(data), key).toString('base64url')}`;
}
// 앱 꼴 요청: { provider, idToken: { token, nonce } } + Origin = 사이트(nonce null = 본문에 nonce 없음)
const google = (call: Call, token: string, { nonce = NONCE, cookie, origin = SITE }: { nonce?: string | null; cookie?: string; origin?: string } = {}) =>
  call('POST', '/api/auth/sign-in/social', { body: { provider: 'google', idToken: nonce === null ? { token } : { token, nonce } }, cookie, headers: { Origin: origin } });
const TOKEN_FIELDS = ['password', 'accessToken', 'refreshToken', 'idToken', 'accessTokenExpiresAt', 'refreshTokenExpiresAt'];

describe('⑤ Google ID 토큰 로그인(D35) — 실제 Better Auth google 공급자 + 시험 RSA 키 · 가짜 공개 키, 실제 Google 요청 0', () => {
  it('정상: 200 · redirect false · 세션 쿠키 1개 · user/account/session 1 · 토큰 칸 null · image 빈 값 · 같은 sub = 같은 계정 · 바깥 요청 = 가짜 공개 키뿐', async () => {
    const { db, call } = setup();
    const token = idToken(claims());
    const res = await google(call, token);
    expect([res.status, res.headers.get('cache-control'), res.headers.get('location')]).toEqual([200, 'no-store', null]);
    const body = await res.json();
    expect(Object.keys(body).sort()).toEqual(['redirect', 'token', 'user']); // 돌아가기(url) 없음. token은 라이브러리 응답(앱은 저장하지 않음)
    expect(body.redirect).toBe(false);
    expect(body.user).toMatchObject({ id: expect.any(String), email: 'o8.google@example.invalid', name: '구글 앱 사용자', emailVerified: true });
    expect(body.user.image ?? null).toBeNull();
    const set = setCookies(res);
    expect(set.map(c => c.name)).toEqual(['better-auth.session_token']);
    expect(set[0].attrs).toEqual(expect.arrayContaining(['max-age=604800', 'path=/', 'httponly', 'samesite=lax']));
    expect(counts(db)).toEqual([1, 1, 1, 0]);
    expect(db.user[0]).toMatchObject({ email: 'o8.google@example.invalid', emailVerified: true, name: '구글 앱 사용자' });
    expect(db.user[0].image ?? null).toBeNull();
    expect(db.account[0]).toMatchObject({ providerId: 'google', accountId: '110000000000000000001', userId: db.user[0].id });
    for (const k of TOKEN_FIELDS) expect(db.account[0][k] ?? null).toBeNull();
    expect(JSON.stringify([db.user, db.account, db.session])).not.toContain(token.split('.')[2]);
    for (const leaked of ['eyJ', 'o8-fake-picture', NONCE]) expect(JSON.stringify([db.user, db.account])).not.toContain(leaked);
    expect((await (await call('GET', '/api/auth/get-session', { cookie: jar(res) })).json()).user.id).toBe(db.user[0].id);
    // 같은 sub로 다시: 같은 사용자 · account 행 그대로(토큰 다시 저장 0) · session +1 · Google 쪽 이름 · 이메일이 바뀌어도 저장값 그대로
    const account = JSON.stringify(db.account[0]);
    const again = await google(call, idToken(claims({ email: 'renamed@example.invalid', name: '바뀐 이름' })));
    expect(again.status).toBe(200);
    expect((await again.json()).user.id).toBe(db.user[0].id);
    expect(counts(db)).toEqual([1, 1, 2, 0]);
    expect(JSON.stringify(db.account[0])).toBe(account);
    expect(db.user[0]).toMatchObject({ email: 'o8.google@example.invalid', name: '구글 앱 사용자' });
    // 받아들이는 경계: 발급자 accounts.google.com(https 없음) · 발급 59분 전 · 본문에 nonce가 없으면 nonce를 보지 않음(앱은 늘 보냄)
    // · 쿠키 없는 sign-in/social은 Origin을 보지 않음(라이브러리: 이 경로엔 formCsrf 없음 — 쿠키가 있으면 ④처럼 403)
    const edges: [string, Response][] = [
      ['iss', await google(call, idToken(claims({ iss: 'accounts.google.com' })))],
      ['59분', await google(call, idToken(claims({ iat: nowSec() - 59 * 60, exp: nowSec() + 60 })))],
      ['nonce 없음', await google(call, idToken(claims()), { nonce: null })],
      ['다른 Origin · 쿠키 없음', await google(call, idToken(claims()), { origin: EVIL })],
    ];
    expect(edges.map(([label, r]) => [label, r.status])).toEqual(edges.map(([label]) => [label, 200]));
    expect(counts(db)).toEqual([1, 1, 6, 0]);
    expect(calls).toEqual(Array(6).fill(`GET ${CERTS}`));
    expectSafeLogs(token);
  });

  it('거절: aud · 발급자 다름 · 1시간 넘음 · 만료 · nonce 틀림 · 토큰에 nonce 없음 · 서명 틀림 · 내용 바꿈 · RS512 · alg none · 모르는 kid · JWT 아님 → 401 INVALID_TOKEN / 이메일 없음 → 401 USER_EMAIL_NOT_FOUND · 쿠키 0 · 행 0', async () => {
    const { db, call } = setup();
    const [head, , signature] = idToken(claims()).split('.');
    const cases: [string, string, string?][] = [
      ['aud 다름', idToken(claims({ aud: 'o8-other-client.apps.googleusercontent.com' }))],
      ['발급자 다름', idToken(claims({ iss: EVIL }))],
      ['1시간 넘음(iat 61분 전 · exp는 남음)', idToken(claims({ iat: nowSec() - 61 * 60, exp: nowSec() + 600 }))],
      ['만료(exp 지남)', idToken(claims({ iat: nowSec() - 1200, exp: nowSec() - 60 }))],
      ['nonce 틀림', idToken(claims()), 'o8-other-nonce'],
      ['토큰에 nonce 없음', idToken(claims({ nonce: undefined }))],
      ['서명 틀림(다른 키)', idToken(claims(), { key: otherKey.privateKey })],
      ['내용 바꿈(서명 그대로)', `${head}.${b64url(claims({ sub: '999' }))}.${signature}`],
      ['RS512(같은 키)', idToken(claims(), { alg: 'RS512' })],
      ['alg none', idToken(claims(), { alg: 'none' })],
      ['모르는 kid', idToken(claims(), { kid: 'o8-unknown-kid' })],
      ['JWT 아님', 'o8-not-a-jwt'],
    ];
    for (const [label, token, nonce] of cases) {
      const res = await google(call, token, { nonce });
      expect([label, res.status, await res.json()]).toEqual([label, 401, { code: 'INVALID_TOKEN', message: 'Invalid token' }]);
      expect(res.headers.getSetCookie()).toEqual([]);
    }
    // 서명은 맞아도 토큰에 이메일이 없으면 401 USER_EMAIL_NOT_FOUND
    const noEmail = await google(call, idToken(claims({ email: undefined, email_verified: undefined })));
    expect([noEmail.status, (await noEmail.json()).code]).toEqual([401, 'USER_EMAIL_NOT_FOUND']);
    expect(noEmail.headers.getSetCookie()).toEqual([]);
    expect(counts(db)).toEqual([0, 0, 0, 0]);
    // 공개 키는 검사마다 1번(가짜 fetch). JWT 꼴(점 3개)이 아니면 키를 부르지 않음
    expect(calls).toEqual(Array(cases.length).fill(`GET ${CERTS}`));
    expectSafeLogs(signature);
  });

  it('계정 규칙(D26): 같은 이메일의 이메일 · 비밀번호 계정 → 401 OAUTH_LINK_ERROR(account not linked) · 잇지 않음 · 행 그대로 / 미인증 Google 이메일 = 새 가입만', async () => {
    const { db, call } = setup();
    await signUp(call, { name: '이메일 가입자', email: 'shared@example.invalid', password: 'Shared-only-pass-4821' });
    const credential = JSON.stringify(db.account[0]);
    const before = counts(db);
    for (const [sub, verified] of [['4000', true], ['4001', false]] as const) {
      const res = await google(call, idToken(claims({ sub, email: 'Shared@Example.INVALID', email_verified: verified })));
      expect([res.status, await res.json()]).toEqual([401, { code: 'OAUTH_LINK_ERROR', message: 'account not linked' }]);
      expect(res.headers.getSetCookie()).toEqual([]);
    }
    // 라이브러리 기본값(requireLocalEmailVerified)에 기대지 않음: 로컬 계정이 인증된 상태여도 disableImplicitLinking으로 잇지 않음
    db.user[0].emailVerified = true;
    expect((await google(call, idToken(claims({ sub: '4002', email: 'shared@example.invalid' })))).status).toBe(401);
    db.user[0].emailVerified = false;
    expect(counts(db)).toEqual(before);
    expect(JSON.stringify(db.account[0])).toBe(credential);
    expect(db.account.filter(a => a.providerId === 'google')).toHaveLength(0);
    // Google이 미인증이라고 한 새 이메일: 가입 허용 · emailVerified false · 토큰 칸 null
    const unverified = await google(call, idToken(claims({ sub: '5000', email: 'unverified@example.invalid', email_verified: false, name: '미인증' })));
    expect(unverified.status).toBe(200);
    expect(db.user.find(u => u.email === 'unverified@example.invalid')).toMatchObject({ emailVerified: false, name: '미인증' });
    const googleAccount = db.account.find(a => a.providerId === 'google')!;
    expect(googleAccount.accountId).toBe('5000');
    for (const k of TOKEN_FIELDS) expect(googleAccount[k] ?? null).toBeNull();
    expect(counts(db)).toEqual([2, 2, 2, 0]);
    // Google 가입자는 비밀번호(credential 행)가 없음 → 같은 이메일 비밀번호 로그인 401 · 새 행 0
    expect((await call('POST', '/api/auth/sign-in/email', { body: { email: 'unverified@example.invalid', password: 'Any-password-123' } })).status).toBe(401);
    expect(counts(db)).toEqual([2, 2, 2, 0]);
    expect(calls).toEqual(Array(4).fill(`GET ${CERTS}`));
    expectSafeLogs();
  });

  it('Google이 꺼지면(환경 변수 없음) idToken도 404 PROVIDER_NOT_FOUND · 공개 키 요청 0 · 행 0', async () => {
    const { db, call } = setup({});
    const res = await google(call, idToken(claims()));
    expect([res.status, (await res.json()).code]).toEqual([404, 'PROVIDER_NOT_FOUND']);
    expect(res.headers.getSetCookie()).toEqual([]);
    expect(calls).toEqual([]);
    expect(counts(db)).toEqual([0, 0, 0, 0]);
    expectSafeLogs();
  });
});
