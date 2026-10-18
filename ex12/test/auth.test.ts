import { spawn } from 'node:child_process';
import { createHash } from 'node:crypto';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { betterAuth } from 'better-auth';
import { memoryAdapter, type MemoryDB } from 'better-auth/adapters/memory';
import { authOptions } from '../server/auth';
import { createApp } from '../server/app';
import { memoryStore } from '../server/sample';

const origin = 'http://localhost:3000';
const env = { BETTER_AUTH_SECRET: 'test-only-dcf7c42a775f0a81cda35609a2', BETTER_AUTH_URL: origin };
const user = { name: '테스트', email: 'test@example.invalid', password: 'LocalTest-only-9201!' };
// Google 로그인(O2): 가짜 클라이언트 값(실제 Google 요청 0 — 전역 fetch는 가짜)
const GOOGLE = { GOOGLE_CLIENT_ID: 'o2-test.apps.googleusercontent.com', GOOGLE_CLIENT_SECRET: 'o2-test-only-secret-5c1f0a7e93' };
function setup(extra: Record<string, string> = {}) {
  const db: MemoryDB = { user: [], session: [], account: [], verification: [] };
  const auth = betterAuth({ ...authOptions({ ...env, ...extra }), database: memoryAdapter(db) });
  const app = createApp(memoryStore, auth);
  const request = (path: string, body?: unknown, cookie?: string, requestOrigin = origin) => app.request(origin + '/api/auth/' + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { Origin: requestOrigin, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(cookie ? { Cookie: cookie } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { db, auth, request };
}
const cookies = (res: Response) => res.headers.getSetCookie().map(c => c.split(';')[0]).join('; ');

describe('실제 Better Auth 라이브러리 + 격리 메모리 어댑터', () => {
  const emailFlow = async (extra: Record<string, string> = {}) => {
    const { db, request } = setup(extra);
    expect(await (await request('get-session')).json()).toBeNull();
    const signup = await request('sign-up/email', user);
    expect(signup.status).toBe(200);
    expect(signup.headers.get('cache-control')).toBe('no-store');
    expect(signup.headers.get('set-cookie')).toContain('HttpOnly');
    expect(signup.headers.get('set-cookie')).toContain('SameSite=Lax');
    const id = (await signup.json()).user.id;
    expect(db.user).toHaveLength(1);
    expect(db.account[0].password).not.toBe(user.password);
    expect(db.account[0].password.length).toBeGreaterThan(50);
    const cookie = cookies(signup);
    expect((await (await request('get-session', undefined, cookie)).json()).user.id).toBe(id);
    expect((await request('sign-out', {}, cookie)).status).toBe(200);
    expect(await (await request('get-session', undefined, cookie)).json()).toBeNull();
    expect(db.session).toHaveLength(0);
    expect((await request('sign-in/email', { ...user, password: 'wrong-password' })).status).toBe(401);
    expect((await request('sign-in/email', { ...user, email: 'absent@example.invalid' })).status).toBe(401);
    expect(db.session).toHaveLength(0);
    const login = await request('sign-in/email', user);
    expect(login.status).toBe(200);
    expect((await (await request('get-session', undefined, cookies(login))).json()).user.id).toBe(id);
  };
  it('가입 자동로그인·비밀번호 해시·세션·로그아웃·틀린/정상 로그인', () => emailFlow());
  it('Google을 켠 설정에서도 같은 이메일 가입·로그인 흐름(O2 회귀, AC-06-1 · 06-2)', () => emailFlow(GOOGLE));

  it('중복·이메일형식·빈 이름·비밀번호 길이를 거절하고 새 세션을 만들지 않는다', async () => {
    const { db, request } = setup();
    await request('sign-up/email', user);
    const duplicate = await request('sign-up/email', user);
    expect(duplicate.status).toBe(422);
    expect((await duplicate.json()).code).toBe('USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL');
    for (const changes of [{email:'invalid'}, {name:''}, {name:'   '}, {password:'short'}, {password:'a'.repeat(129)}]) {
      expect((await request('sign-up/email', { ...user, email: 'second@example.invalid', ...changes })).status).toBe(400);
    }
    expect(db.user).toHaveLength(1); expect(db.session).toHaveLength(1);
  });

  it('변조·만료 쿠키와 악성 Origin을 거절한다', async () => {
    const { db, request } = setup();
    const signup = await request('sign-up/email', user);
    const cookie = cookies(signup);
    expect(await (await request('get-session', undefined, cookie + 'tampered')).json()).toBeNull();
    db.session[0].expiresAt = new Date('2000-01-01');
    expect(await (await request('get-session', undefined, cookie)).json()).toBeNull();
    const count = db.session.length;
    expect((await request('sign-in/email', user, undefined, 'https://evil.example')).status).toBe(403);
    expect(db.session).toHaveLength(count);
  });

  it('Hono 예외와 auth 로거는 가짜 비밀값 대신 안전 코드만 기록한다', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
      const app = createApp(memoryStore, { handler: async () => { throw new Error('SECRET_URL_PASSWORD_TOKEN'); } });
      expect((await app.request('/api/auth/get-session')).status).toBe(500);
      authOptions(env).logger!.log!('error', 'SECRET_URL_PASSWORD_TOKEN', new Error('PRIVATE_STACK'));
      expect(log.mock.calls).toEqual([['API_INTERNAL_ERROR'], ['AUTH_INTERNAL_ERROR']]);
    } finally { log.mockRestore(); }
  });

  it('실제 auth 어댑터 예외도 원본 오류나 스택을 출력하지 않는다', async () => {
    const { auth, request } = setup();
    const context = await auth.$context;
    context.adapter.findOne = async () => { throw new Error('PRIVATE_DATABASE_PASSWORD'); };
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
      const response = await request('sign-in/email', user);
      expect(response.status).toBe(500);
      expect(JSON.stringify(log.mock.calls)).not.toContain('PRIVATE_DATABASE_PASSWORD');
      expect(log.mock.calls.length).toBeGreaterThan(0);
      expect(log.mock.calls.every(call => call.length === 1 && /^(AUTH_INTERNAL_ERROR|API_INTERNAL_ERROR)$/.test(call[0]))).toBe(true);
      expect(await response.text()).not.toContain('PRIVATE_DATABASE_PASSWORD');
    } finally { log.mockRestore(); }
  });

  it('진입점의 누락 설정과 잘못된 DB 연결은 안전하게 종료한다', async () => {
    for (const variables of [{ BETTER_AUTH_SECRET: '', DATABASE_URL: '' }, { ...env, DATABASE_URL: 'postgres://u:PRIVATE_DATABASE_PASSWORD@[bad/x' }]) {
      const result = await new Promise<{status:number|null; stdout:string; stderr:string}>((resolve, reject) => {
        const child = spawn(process.execPath, ['--import', 'tsx', 'server/index.ts'], {env:{...process.env,...variables},stdio:['ignore','pipe','pipe']});
        let stdout='', stderr='';
        child.stdout.on('data', value => stdout += value);
        child.stderr.on('data', value => stderr += value);
        const timeout = setTimeout(() => { child.kill('SIGKILL'); reject(new Error('STARTUP_TEST_TIMEOUT')); }, 15000);
        child.on('error', () => { clearTimeout(timeout); reject(new Error('STARTUP_TEST_FAILED')); });
        child.on('exit', status => { clearTimeout(timeout); resolve({status,stdout,stderr}); });
      });
      expect(result.status).toBe(2);
      expect(result.stdout).toBe('');
      expect(result.stderr).toBe('API_START_FAILED\n');
    }
  }, 35000);

  it('필수 설정 누락과 잘못된 주소는 안전한 오류만 반환한다', () => {
    for (const invalid of [{}, {...env, BETTER_AUTH_SECRET:'short'}, {...env, BETTER_AUTH_URL:'postgres://user:PRIVATE@db/a'}, {...env, BETTER_AUTH_URL:origin + '/api/auth'}]) {
      expect(() => authOptions(invalid)).toThrow('AUTH_CONFIG_INVALID');
    }
  });
});

// ── Google 로그인(O2, D26): 실제 Better Auth + 메모리 어댑터 + 가짜 토큰 엔드포인트(가짜 전역 fetch). 실제 Google 요청 0 ──
const BACK = '/?login=google#bookmarks';
const TOKEN_URL = 'https://oauth2.googleapis.com/token';
const CALLBACK = 'http://localhost:3000/api/auth/callback/google';
const FAKE_ACCESS = 'ya29.o2-fake-access-token-7f3e';
const FAKE_REFRESH = '1//o2-fake-refresh-token-9a1c';
type Claims = { sub: string; email: string; email_verified: boolean; name?: string };
const b64url = (value: object) => Buffer.from(JSON.stringify(value)).toString('base64url');
// 서명 없는 ID 토큰: 1.7.7 Google provider는 토큰 응답의 ID 토큰을 검증 없이 해석하고 userinfo를 부르지 않음(코드 확인)
const idToken = (claims: Claims) => `${b64url({ alg: 'none', typ: 'JWT' })}.${b64url({ iss: 'https://accounts.google.com', aud: GOOGLE.GOOGLE_CLIENT_ID, picture: 'https://lh3.googleusercontent.com/a/o2-fake-picture', ...claims })}.`;
// refresh_token은 실제로는 오지 않지만(access_type 없음) 와도 저장하지 않는지 보려고 넣음
const tokenReply = (claims: Claims) => () => Response.json({
  access_token: FAKE_ACCESS, refresh_token: FAKE_REFRESH, expires_in: 3599, token_type: 'Bearer',
  scope: 'openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile', id_token: idToken(claims),
});
// 전역 fetch 가짜: 토큰 주소 POST에만 reply로 답하고 그 밖(Google JWKS · userinfo 등)은 실패. 불린 요청은 모두 기록
function fakeFetch(reply?: () => Response) {
  const calls: { url: string; method: string; body: string }[] = [];
  vi.stubGlobal('fetch', vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const req = new Request(input, init);
    calls.push({ url: req.url, method: req.method, body: await req.text() });
    if (!reply || req.url !== TOKEN_URL || req.method !== 'POST') throw new TypeError('UNEXPECTED_FETCH');
    return reply();
  }));
  return calls;
}
type SetCookie = { name: string; value: string; attrs: string[] };
const setCookies = (res: Response): SetCookie[] => res.headers.getSetCookie().map(line => {
  const [pair, ...attrs] = line.split(';').map(s => s.trim());
  const i = pair.indexOf('=');
  return { name: pair.slice(0, i), value: pair.slice(i + 1), attrs: attrs.map(a => a.toLowerCase()) };
});
// 브라우저처럼 값이 있고 만료되지 않은 쿠키만 다음 요청에 보냄
const jar = (res: Response) => setCookies(res).filter(c => c.value && !c.attrs.includes('max-age=0')).map(c => `${c.name}=${c.value}`).join('; ');
const SOCIAL = { provider: 'google', callbackURL: BACK, errorCallbackURL: BACK };
type AuthRequest = ReturnType<typeof setup>["request"];
async function start(request: AuthRequest) {
  const res = await request('sign-in/social', SOCIAL);
  const url = new URL((await res.json()).url);
  return { res, url, state: url.searchParams.get('state')!, cookie: jar(res) };
}
// 시작 → 그 쿠키 · state로 콜백(code). 가짜 fetch가 claims로 토큰 응답
async function googleLogin(request: AuthRequest, claims: Claims) {
  const calls = fakeFetch(tokenReply(claims));
  const s = await start(request);
  const res = await request(`callback/google?state=${s.state}&code=o2-fake-code`, undefined, s.cookie);
  return { res, calls, start: s };
}
const counts = (db: MemoryDB) => [db.user.length, db.account.length, db.session.length, db.verification.length];

describe('Google 로그인(O2, D26) — 실제 Better Auth + 메모리 어댑터 + 가짜 토큰 엔드포인트', () => {
  // ⑪ 모든 경우의 콘솔: 안전 코드 한 줄씩뿐(원문 · 토큰 · client secret · 이메일 0)
  let logs: unknown[][] = [];
  beforeEach(() => {
    logs = [];
    for (const method of ['error', 'warn', 'log', 'info', 'debug'] as const) vi.spyOn(console, method).mockImplementation((...args: unknown[]) => { logs.push([method, ...args]); });
  });
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });
  const expectSafeLogs = () => {
    expect(logs.every(([method, ...args]) => method === 'error' && args.length === 1 && /^(AUTH_INTERNAL_ERROR|API_INTERNAL_ERROR)$/.test(String(args[0])))).toBe(true);
    const text = JSON.stringify(logs);
    for (const secret of [GOOGLE.GOOGLE_CLIENT_SECRET, FAKE_ACCESS, FAKE_REFRESH, 'o2-fake-code', '@', 'invalid']) expect(text).not.toContain(secret);
  };
  const authLines = () => logs.filter(([, line]) => line === 'AUTH_INTERNAL_ERROR').length;

  it('3 시작(AC-06-3): 200 · redirect true · Google 주소 칸 정확 · 상태 쿠키 1개 · no-store · DB 쓰기 0 · fetch 0', async () => {
    const { db, request } = setup(GOOGLE);
    const calls = fakeFetch();
    const res = await request('sign-in/social', SOCIAL);
    expect(res.status).toBe(200);
    expect(res.headers.get('cache-control')).toBe('no-store');
    const body = await res.json();
    expect(Object.keys(body).sort()).toEqual(['redirect', 'url']);
    expect(body.redirect).toBe(true);
    const url = new URL(body.url);
    expect(url.origin + url.pathname).toBe('https://accounts.google.com/o/oauth2/v2/auth');
    const p = url.searchParams;
    // access_type · prompt · login_hint · nonce · hd 없음(→ refresh token을 받지 않음)
    expect([...p.keys()].sort()).toEqual(['client_id', 'code_challenge', 'code_challenge_method', 'include_granted_scopes', 'redirect_uri', 'response_type', 'scope', 'state']);
    expect(Object.fromEntries([...p].filter(([k]) => !['state', 'code_challenge'].includes(k)))).toEqual({
      response_type: 'code', client_id: GOOGLE.GOOGLE_CLIENT_ID, scope: 'openid email profile', redirect_uri: CALLBACK,
      code_challenge_method: 'S256', include_granted_scopes: 'true',
    });
    expect(p.get('state')).toMatch(/^[A-Za-z0-9_-]{32}$/);
    expect(p.get('code_challenge')).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(res.headers.get('location')).toBe(body.url); // 라이브러리가 200에도 Location을 붙임(fetch는 따라가지 않음)
    const set = setCookies(res);
    expect(set.map(c => c.name)).toEqual(['better-auth.oauth_state']);
    expect(set[0].value.length).toBeGreaterThan(100);
    expect(set[0].value).not.toContain(p.get('state')); // 암호화된 값
    expect(set[0].attrs).toEqual(expect.arrayContaining(['max-age=600', 'path=/', 'httponly', 'samesite=lax']));
    expect(set[0].attrs).not.toContain('secure'); // http://localhost(배포 https는 __Secure- · Secure)
    expect(counts(db)).toEqual([0, 0, 0, 0]);
    expect(calls).toEqual([]);
    expectSafeLogs();
  });

  it('4 콜백 새 가입(AC-06-3): 302 · 세션 쿠키 · 상태 쿠키 만료 · 토큰 요청 1번 · user/account/session 1 · 토큰 · 사진 저장 0', async () => {
    const { db, request } = setup(GOOGLE);
    const claims = { sub: '109876543210987654321', email: 'O2.Tester@Example.Invalid', email_verified: true, name: '  구글 사용자  ' };
    const { res, calls, start: s } = await googleLogin(request, claims);
    expect(res.status).toBe(302);
    expect(res.headers.get('location')).toBe(BACK);
    expect(res.headers.get('cache-control')).toBe('no-store');
    const set = setCookies(res);
    const session = set.find(c => c.name === 'better-auth.session_token')!;
    expect(session.value.length).toBeGreaterThan(20);
    expect(session.attrs).toEqual(expect.arrayContaining(['max-age=604800', 'path=/', 'httponly', 'samesite=lax']));
    const state = set.find(c => c.name === 'better-auth.oauth_state')!;
    expect(state.value).toBe('');
    expect(state.attrs).toContain('max-age=0');
    // 토큰 교환 1번: code · PKCE code_verifier(128자, challenge = S256(verifier)) · 같은 redirect_uri · client secret(본문)
    expect(calls.map(c => `${c.method} ${c.url}`)).toEqual([`POST ${TOKEN_URL}`]);
    const form = new URLSearchParams(calls[0].body);
    expect(form.get('grant_type')).toBe('authorization_code');
    expect(form.get('code')).toBe('o2-fake-code');
    expect(form.get('redirect_uri')).toBe(CALLBACK);
    expect(form.get('client_id')).toBe(GOOGLE.GOOGLE_CLIENT_ID);
    expect(form.get('client_secret')).toBe(GOOGLE.GOOGLE_CLIENT_SECRET);
    expect(form.get('code_verifier')).toMatch(/^[A-Za-z0-9_-]{128}$/);
    expect(createHash('sha256').update(form.get('code_verifier')!).digest('base64url')).toBe(s.url.searchParams.get('code_challenge'));
    // 저장 칸
    expect(counts(db)).toEqual([1, 1, 1, 0]);
    expect(db.user[0]).toMatchObject({ email: 'o2.tester@example.invalid', emailVerified: true, name: '구글 사용자' });
    expect(db.user[0].image ?? null).toBeNull();
    expect(db.account[0]).toMatchObject({ providerId: 'google', accountId: claims.sub, userId: db.user[0].id });
    for (const k of ['password', 'accessToken', 'refreshToken', 'idToken', 'accessTokenExpiresAt', 'refreshTokenExpiresAt']) expect(db.account[0][k] ?? null).toBeNull();
    expect(db.account[0].scope).toContain('openid');
    const stored = JSON.stringify([db.user, db.account, db.session]);
    for (const leaked of [FAKE_ACCESS, FAKE_REFRESH, 'o2-fake-picture', 'eyJ']) expect(stored).not.toContain(leaked);
    expect(db.session[0].userId).toBe(db.user[0].id);
    const me = await (await request('get-session', undefined, jar(res))).json();
    expect(me.user).toMatchObject({ id: db.user[0].id, email: 'o2.tester@example.invalid', name: '구글 사용자' });
    // name이 비거나(공백) 없으면 이름 = 이메일 @ 앞부분(다른 sub)
    for (const [sub, email, name] of [['200', 'blank.name@example.invalid', '   '], ['201', 'no.name@example.invalid', undefined]] as const) {
      const again = await googleLogin(request, { sub, email, email_verified: true, ...(name === undefined ? {} : { name }) });
      expect(again.res.headers.get('location')).toBe(BACK);
      expect(db.user.find(u => u.email === email)?.name).toBe(email.split('@')[0]);
    }
    expect(counts(db)).toEqual([3, 3, 3, 0]);
    expectSafeLogs();
  });

  it('5 다시 로그인 · 계정 규칙(AC-06-2 · 06-3 · 06-4): 같은 sub · 같은 이메일 거절 · 미인증 새 가입 · Google 가입자의 이메일 로그인', async () => {
    const { db, request } = setup(GOOGLE);
    // ① 같은 Google 계정(sub)으로 다시: 그 계정 · 새 행 0 · session +1 · Google 쪽 이름 · 이메일이 바뀌어도 저장값 그대로 · account 행 그대로
    await googleLogin(request, { sub: '3000', email: 'again@example.invalid', email_verified: true, name: '처음 이름' });
    const account = JSON.stringify(db.account[0]);
    const second = await googleLogin(request, { sub: '3000', email: 'renamed@example.invalid', email_verified: true, name: '바뀐 이름' });
    expect(second.res.status).toBe(302);
    expect(second.res.headers.get('location')).toBe(BACK);
    expect(counts(db)).toEqual([1, 1, 2, 0]);
    expect(db.user[0]).toMatchObject({ email: 'again@example.invalid', name: '처음 이름' });
    expect(JSON.stringify(db.account[0])).toBe(account);
    expect((await (await request('get-session', undefined, jar(second.res))).json()).user.id).toBe(db.user[0].id);
    // ② 같은 이메일(대문자 섞음)의 이메일 · 비밀번호 계정이 있으면 잇지 않음(Google이 인증했다고 해도 · 미인증이라고 해도)
    const signup = await request('sign-up/email', { name: '이메일 가입자', email: 'shared@example.invalid', password: 'Shared-only-pass-4821' });
    expect(signup.status).toBe(200);
    const credential = JSON.stringify(db.account.find(a => a.providerId === 'credential'));
    const before = counts(db);
    for (const [sub, verified] of [['4000', true], ['4001', false]] as const) {
      const linesBefore = authLines();
      const r = await googleLogin(request, { sub, email: 'Shared@Example.INVALID', email_verified: verified, name: 'G' });
      expect(r.res.status).toBe(302);
      expect(r.res.headers.get('location')).toBe('/?login=google&error=account_not_linked#bookmarks');
      expect(setCookies(r.res).some(c => c.name === 'better-auth.session_token' && c.value)).toBe(false);
      expect(authLines() - linesBefore).toBe(1); // account_not_linked 1줄
    }
    // 라이브러리 기본값(requireLocalEmailVerified, 폐지 예정)에 기대지 않음: 로컬 계정이 인증된 상태라고 해도 disableImplicitLinking으로 잇지 않음
    const local = db.user.find(u => u.email === 'shared@example.invalid')!;
    local.emailVerified = true;
    const verifiedBoth = await googleLogin(request, { sub: '4002', email: 'shared@example.invalid', email_verified: true, name: 'G' });
    expect(verifiedBoth.res.headers.get('location')).toBe('/?login=google&error=account_not_linked#bookmarks');
    local.emailVerified = false;
    expect(counts(db)).toEqual(before);
    expect(JSON.stringify(db.account.find(a => a.providerId === 'credential'))).toBe(credential);
    expect(db.account.filter(a => a.providerId === 'google')).toHaveLength(1);
    // ③ Google이 미인증이라고 한 새 이메일: 가입 허용 · email_verified false
    const unverified = await googleLogin(request, { sub: '5000', email: 'unverified@example.invalid', email_verified: false, name: '미인증' });
    expect(unverified.res.headers.get('location')).toBe(BACK);
    expect(db.user.find(u => u.email === 'unverified@example.invalid')).toMatchObject({ emailVerified: false, name: '미인증' });
    // ④ Google 가입자는 비밀번호(credential 행)가 없음 → 이메일 로그인 401 · 이메일 가입 422 · 새 행 0
    const rows = counts(db);
    expect((await request('sign-in/email', { email: 'again@example.invalid', password: 'Any-password-123' })).status).toBe(401);
    expect((await request('sign-up/email', { name: 'x', email: 'again@example.invalid', password: 'Any-password-123' })).status).toBe(422);
    expect(counts(db)).toEqual(rows);
    expectSafeLogs();
  });

  it('6 실패 · 보안(AC-06-4): 취소 · 상태 실패 · 만료 · 코드 교환 실패 · 다른 주소 · 다른 Origin · idToken · 닫은 경로 · Google 꺼짐', async () => {
    const { db, request } = setup(GOOGLE);
    const callback = (query: string, cookie?: string) => request(`callback/google?${query}`, undefined, cookie);
    const location = (res: Response) => res.headers.get('location');
    // ① 쿠키 있고 Google이 error=access_denied: errorCallbackURL + error · fetch 0 · 세션 0 · 로그 0줄
    let calls = fakeFetch();
    let s = await start(request);
    let lines = authLines();
    const denied = await callback(`state=${s.state}&error=access_denied`, s.cookie);
    expect([denied.status, location(denied), denied.headers.get('cache-control')]).toEqual([302, '/?login=google&error=access_denied#bookmarks', 'no-store']);
    expect(authLines() - lines).toBe(0);
    // ② 쿠키 없이: 상태를 읽지 못함 → onAPIError.errorURL "/"
    lines = authLines();
    const noCookie = await callback(`state=${s.state}&code=x`);
    expect([noCookie.status, location(noCookie)]).toEqual([302, '/?error=state_mismatch']);
    expect(authLines() - lines).toBe(1);
    // ③ 쿠키와 다른 state
    s = await start(request);
    const other = await callback('state=' + 'x'.repeat(32) + '&code=x', s.cookie);
    expect(location(other)).toBe('/?login=google&error=state_mismatch#bookmarks');
    // ④ 11분 뒤(가짜 시계, 상태 만료 10분)
    s = await start(request);
    vi.setSystemTime(Date.now() + 11 * 60e3);
    const late = await callback(`state=${s.state}&code=x`, s.cookie);
    expect(location(late)).toBe('/?login=google&error=state_mismatch#bookmarks');
    expect(calls).toEqual([]);
    expect(counts(db)).toEqual([0, 0, 0, 0]);
    // ⑤ 토큰 응답 400: invalid_code · 세션 0 · 로그 1줄
    calls = fakeFetch(() => Response.json({ error: 'invalid_grant', error_description: 'Bad Request' }, { status: 400 }));
    s = await start(request);
    lines = authLines();
    const badCode = await callback(`state=${s.state}&code=o2-fake-code`, s.cookie);
    expect(location(badCode)).toBe('/?login=google&error=invalid_code#bookmarks');
    expect(calls).toHaveLength(1);
    expect(authLines() - lines).toBe(1);
    expect(counts(db)).toEqual([0, 0, 0, 0]);
    // ⑥ 다른 주소로 돌아오기: 403 · 쿠키 0
    calls = fakeFetch();
    for (const [body, code] of [[{ ...SOCIAL, callbackURL: 'https://evil.example/' }, 'INVALID_CALLBACK_URL'], [{ ...SOCIAL, errorCallbackURL: 'https://evil.example/' }, 'INVALID_ERROR_CALLBACK_URL']] as const) {
      const res = await request('sign-in/social', body);
      expect(res.status).toBe(403);
      expect((await res.json()).code).toBe(code);
      expect(res.headers.getSetCookie()).toEqual([]);
    }
    // ⑦ 쿠키가 있는 POST + 다른 Origin: 403
    s = await start(request);
    const evil = await request('sign-in/social', SOCIAL, s.cookie, 'https://evil.example');
    expect(evil.status).toBe(403);
    expect(evil.headers.getSetCookie()).toEqual([]);
    // ⑧ 본문 idToken 로그인: 404 ID_TOKEN_NOT_SUPPORTED · Google JWKS fetch 0
    const withIdToken = await request('sign-in/social', { provider: 'google', idToken: { token: idToken({ sub: '9', email: 'id@example.invalid', email_verified: true }) } });
    expect(withIdToken.status).toBe(404);
    expect((await withIdToken.json()).code).toBe('ID_TOKEN_NOT_SUPPORTED');
    expect(calls).toEqual([]);
    expect(counts(db)).toEqual([0, 0, 0, 0]);
    // ⑨ 세션 쿠키를 들고 닫은 경로 5개: 모두 404(라이브러리 disabledPaths 응답 "Not Found"). 대조: 닫지 않은 list-accounts는 200
    const signedIn = await googleLogin(request, { sub: '6000', email: 'closed@example.invalid', email_verified: true, name: 'C' });
    const session = jar(signedIn.res);
    for (const [path, body] of [['link-social', { provider: 'google', callbackURL: '/' }], ['unlink-account', { accountId: db.account[0].id }], ['get-access-token', { providerId: 'google' }], ['refresh-token', { providerId: 'google' }], ['account-info', undefined]] as const) {
      const res = await request(path, body, session);
      expect([path, res.status, await res.text()]).toEqual([path, 404, 'Not Found']);
    }
    expect((await request('list-accounts', undefined, session)).status).toBe(200);
    expectSafeLogs();
  });

  it('6⑩ Google 환경 변수가 없거나 하나만 있으면 404 PROVIDER_NOT_FOUND, 같은 설정에서 이메일 가입 · 로그인은 200', async () => {
    const variants: Record<string, string>[] = [{}, { GOOGLE_CLIENT_ID: GOOGLE.GOOGLE_CLIENT_ID }, { GOOGLE_CLIENT_SECRET: GOOGLE.GOOGLE_CLIENT_SECRET }, { GOOGLE_CLIENT_ID: '', GOOGLE_CLIENT_SECRET: '' }];
    for (const extra of variants) {
      const { db, request } = setup(extra);
      const calls = fakeFetch();
      const res = await request('sign-in/social', SOCIAL);
      expect(res.status).toBe(404);
      expect((await res.json()).code).toBe('PROVIDER_NOT_FOUND');
      expect(res.headers.getSetCookie()).toEqual([]);
      expect((await request('sign-up/email', user)).status).toBe(200);
      expect((await request('sign-in/email', user)).status).toBe(200);
      expect(calls).toEqual([]);
      expect(db.user).toHaveLength(1);
    }
    expectSafeLogs();
  });
});
