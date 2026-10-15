import { afterEach, expect, it, vi } from 'vitest';
import { createBff } from '../server/bff';

const MESSAGE = { message: 'API 서버에 연결할 수 없습니다' };
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

it('BFF는 같은 프로세스 앱의 상태·본문·복수 쿠키를 그대로 돌려주고 요청 Cookie/Origin/본문을 그대로 넘긴다', async () => {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  headers.append('Set-Cookie', 'session=one; Expires=Wed, 21 Oct 2026 07:28:00 GMT; HttpOnly');
  headers.append('Set-Cookie', 'other=two; Path=/; SameSite=Lax');
  const seen: { cookie: string | null; origin: string | null; body: string; url: string }[] = [];
  const app = { fetch: vi.fn(async (req: Request) => {
    seen.push({ cookie: req.headers.get('cookie'), origin: req.headers.get('origin'), body: await req.text(), url: req.url });
    return new Response('{"code":"INVALID_PASSWORD"}', { status: 401, headers });
  }) };
  const fetchMock = vi.fn();
  vi.stubGlobal('fetch', fetchMock);
  const handle = createBff(() => app);
  const req = new Request('http://localhost:3000/api/auth/sign-in/email', { method: 'POST', headers: { Cookie: 'session=old', Origin: 'http://localhost:3000', 'Content-Type': 'application/json' }, body: '{"email":"a@example.invalid"}' });
  const res = await handle(req);
  expect(res.status).toBe(401);
  expect(await res.json()).toEqual({ code: 'INVALID_PASSWORD' });
  expect(res.headers.getSetCookie()).toEqual(headers.getSetCookie());
  expect(res.headers.getSetCookie()).toHaveLength(2);
  expect(res.headers.get('cache-control')).toBe('no-store');
  expect(seen).toEqual([{ cookie: 'session=old', origin: 'http://localhost:3000', body: '{"email":"a@example.invalid"}', url: 'http://localhost:3000/api/auth/sign-in/email' }]);
  expect(fetchMock).not.toHaveBeenCalled();
});

it('앱은 첫 요청 때 한 번만 만들고 다음 요청은 재사용한다', async () => {
  const load = vi.fn(() => ({ fetch: () => Response.json([]) }));
  const handle = createBff(load);
  await handle(new Request('http://localhost:3000/api/rankings'));
  await handle(new Request('http://localhost:3000/api/bookmarks'));
  expect(load).toHaveBeenCalledTimes(1);
});

it('앱 준비 실패는 비밀값 없는 502·no-store·API_START_FAILED 한 줄이고 다음 요청이 다시 시도한다', async () => {
  const log = vi.spyOn(console, 'error').mockImplementation(() => {});
  const load = vi.fn()
    .mockRejectedValueOnce(new Error('postgres://user:FAKE_SECRET_123@db.example.invalid:6543/postgres'))
    .mockReturnValueOnce({ fetch: () => Response.json({ user: null }) });
  const handle = createBff(load);
  const failed = await handle(new Request('http://localhost:3000/api/auth/get-session'));
  expect(failed.status).toBe(502);
  const body = await failed.text();
  expect(JSON.parse(body)).toEqual(MESSAGE);
  expect(body).not.toContain('FAKE_SECRET_123');
  expect(failed.headers.get('cache-control')).toBe('no-store');
  expect(log.mock.calls).toEqual([['API_START_FAILED']]);
  const ok = await handle(new Request('http://localhost:3000/api/auth/get-session'));
  expect(ok.status).toBe(200);
  expect(load).toHaveBeenCalledTimes(2);
});

it('앱이 던지면 같은 502 문구다', async () => {
  const log = vi.spyOn(console, 'error').mockImplementation(() => {});
  const handle = createBff(() => ({ fetch: () => { throw new Error('PRIVATE_FAILURE'); } }));
  const res = await handle(new Request('http://localhost:3000/api/folders', { method: 'POST', body: '{}' }));
  expect(res.status).toBe(502);
  expect(await res.json()).toEqual(MESSAGE);
  expect(res.headers.get('cache-control')).toBe('no-store');
  expect(log.mock.calls).toEqual([['API_START_FAILED']]);
});

it('공개 경로 /api/rankings는 no-store를 붙이지 않고 외부 fetch를 하지 않는다', async () => {
  const fetchMock = vi.fn();
  vi.stubGlobal('fetch', fetchMock);
  const handle = createBff(() => ({ fetch: () => Response.json({ date: '2026-10-07' }) }));
  const res = await handle(new Request('http://localhost:3000/api/rankings'));
  expect(res.status).toBe(200);
  expect(await res.json()).toEqual({ date: '2026-10-07' });
  expect(res.headers.get('cache-control')).toBeNull();
  expect(fetchMock).not.toHaveBeenCalled();
});

it('공개 경로의 앱 준비 실패 502에는 no-store를 붙이지 않는다', async () => {
  vi.spyOn(console, 'error').mockImplementation(() => {});
  const handle = createBff(() => { throw new Error('DB_CONFIG_MISSING'); });
  const res = await handle(new Request('http://localhost:3000/api/rankings'));
  expect(res.status).toBe(502);
  expect(await res.json()).toEqual(MESSAGE);
  expect(res.headers.get('cache-control')).toBeNull();
});
