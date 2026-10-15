import { spawn } from 'node:child_process';
import { describe, expect, it, vi } from 'vitest';
import { betterAuth } from 'better-auth';
import { memoryAdapter, type MemoryDB } from 'better-auth/adapters/memory';
import { authOptions } from '../server/auth';
import { createApp } from '../server/app';
import { memoryStore } from '../server/sample';

const origin = 'http://localhost:3000';
const env = { BETTER_AUTH_SECRET: 'test-only-dcf7c42a775f0a81cda35609a2', BETTER_AUTH_URL: origin };
const user = { name: '테스트', email: 'test@example.invalid', password: 'LocalTest-only-9201!' };
function setup() {
  const db: MemoryDB = { user: [], session: [], account: [], verification: [] };
  const auth = betterAuth({ ...authOptions(env), database: memoryAdapter(db) });
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
  it('가입 자동로그인·비밀번호 해시·세션·로그아웃·틀린/정상 로그인', async () => {
    const { db, request } = setup();
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
  });

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
