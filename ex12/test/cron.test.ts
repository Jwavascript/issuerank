// Vercel Cron → GitHub workflow_dispatch(O9 · D37 · AC-01-3): server/cron.ts handleCron. 네트워크 없이 가짜 fetch로
import { describe, expect, it } from 'vitest';
import { handleCron } from '../server/cron';

const SECRET = 'a'.repeat(40);
const TOKEN = 'fake-dispatch-token-for-tests-1234567890';
const env = { CRON_SECRET: SECRET, GITHUB_DISPATCH_TOKEN: TOKEN };
const req = (auth?: string) => new Request('https://x/api/cron/daily', { headers: auth === undefined ? {} : { authorization: auth } });

function fakeFetch(status: number | 'throw' = 204) {
  const calls: { url: string; init: RequestInit }[] = [];
  const fetcher = async (url: string, init: RequestInit) => {
    calls.push({ url, init });
    if (status === 'throw') throw new Error(`boom ${TOKEN}`);
    return new Response(status === 204 ? null : `{"message":"x ${TOKEN}"}`, { status });
  };
  return { calls, fetcher };
}

async function run(job: string, auth: string | undefined, e: Record<string, string | undefined> = env, status: number | 'throw' = 204) {
  const f = fakeFetch(status);
  const logs: string[] = [];
  const res = await handleCron(job, req(auth), e, f.fetcher, line => logs.push(line));
  return { res, body: await res.json(), calls: f.calls, logs };
}

describe('인증 순서', () => {
  it('없는 job = 404 · fetch 0', async () => {
    for (const job of ['x', 'constructor', '__proto__', 'toString']) {
      const r = await run(job, `Bearer ${SECRET}`);
      expect([r.res.status, r.body.message, r.calls.length]).toEqual([404, 'NOT_FOUND', 0]);
    }
  });
  it('CRON_SECRET 없음 = 503 · fetch 0', async () => {
    const r = await run('daily', `Bearer ${SECRET}`, { GITHUB_DISPATCH_TOKEN: TOKEN });
    expect([r.res.status, r.body.message, r.calls.length]).toEqual([503, 'CRON_NOT_CONFIGURED', 0]);
  });
  it('머리 없음 · 틀림 · 길이 다름 · Bearer 없음 = 401 · fetch 0', async () => {
    for (const auth of [undefined, `Bearer ${'b'.repeat(40)}`, `Bearer ${SECRET}x`, SECRET, `bearer ${SECRET}`]) {
      const r = await run('daily', auth);
      expect([r.res.status, r.body.message, r.calls.length]).toEqual([401, 'UNAUTHORIZED', 0]);
    }
  });
  it('맞는 비밀값 + 토큰 없음 = 503 · fetch 0', async () => {
    const r = await run('daily', `Bearer ${SECRET}`, { CRON_SECRET: SECRET });
    expect([r.res.status, r.body.message, r.calls.length]).toEqual([503, 'CRON_NOT_CONFIGURED', 0]);
  });
});

describe('GitHub 호출', () => {
  it('daily → daily.yml {ref:main, inputs:{trigger:cron}} · 머리 · 202', async () => {
    const r = await run('daily', `Bearer ${SECRET}`);
    expect(r.res.status).toBe(202);
    expect(r.body).toEqual({ dispatched: 'daily' });
    expect(r.calls).toHaveLength(1);
    const { url, init } = r.calls[0];
    expect(url).toBe('https://api.github.com/repos/Jwavascript/issuerank/actions/workflows/daily.yml/dispatches');
    expect(init.method).toBe('POST');
    expect(JSON.parse(String(init.body))).toEqual({ ref: 'main', inputs: { trigger: 'cron' } });
    const h = init.headers as Record<string, string>;
    expect(h.Authorization).toBe(`Bearer ${TOKEN}`);
    expect(h.Accept).toBe('application/vnd.github+json');
    expect(h['X-GitHub-Api-Version']).toBe('2022-11-28');
    expect(h['User-Agent']).toBe('issuerank-cron');
    expect(init.signal).toBeInstanceOf(AbortSignal);
  });
  it('health → health.yml {mode:morning}', async () => {
    const r = await run('health', `Bearer ${SECRET}`, env, 200);
    expect(r.res.status).toBe(202);
    expect(r.calls[0].url).toMatch(/\/workflows\/health\.yml\/dispatches$/);
    expect(JSON.parse(String(r.calls[0].init.body))).toEqual({ ref: 'main', inputs: { mode: 'morning' } });
  });
  it('GitHub 401 · 404 · 500 · 던짐 = 502 + 상태, 응답 · 로그에 토큰 · 비밀값 0', async () => {
    for (const [status, want] of [[401, 401], [404, 404], [500, 500], ['throw', 0]] as const) {
      const r = await run('daily', `Bearer ${SECRET}`, env, status);
      expect(r.res.status).toBe(502);
      expect(r.body).toEqual({ message: 'DISPATCH_FAILED', status: want });
      expect(r.logs).toEqual([`CRON_DISPATCH_FAILED ${want}`]);
      const text = JSON.stringify(r.body) + r.logs.join('');
      expect(text).not.toContain(TOKEN);
      expect(text).not.toContain(SECRET);
    }
  });
  it('모든 응답 no-store · 쿠키 0', async () => {
    for (const [job, auth] of [['x', ''], ['daily', ''], ['daily', `Bearer ${SECRET}`]]) {
      const r = await run(job, auth);
      expect(r.res.headers.get('cache-control')).toBe('no-store');
      expect(r.res.headers.get('set-cookie')).toBeNull();
    }
  });
});
