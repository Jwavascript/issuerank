import { expect, it, vi } from 'vitest';
import { GET, POST } from '../app/api/[...path]/route';

it('BFF는 요청 Cookie/Origin/본문과 응답 오류·복수 쿠키를 그대로 중계한다', async () => {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  headers.append('Set-Cookie', 'session=one; Expires=Wed, 21 Oct 2026 07:28:00 GMT; HttpOnly');
  headers.append('Set-Cookie', 'other=two; Path=/; SameSite=Lax');
  const fetchMock = vi.fn().mockResolvedValue(new Response('{"code":"INVALID_PASSWORD"}', { status: 401, headers }));
  vi.stubGlobal('fetch', fetchMock);
  try {
    const req = new Request('http://localhost:3000/api/auth/sign-in/email', { method:'POST', headers: { Cookie:'session=old', Origin:'http://localhost:3000', 'Content-Type':'application/json' }, body:'{"email":"a@example.invalid"}' });
    const res = await POST(req);
    expect(res.status).toBe(401);
    expect(await res.json()).toEqual({code:'INVALID_PASSWORD'});
    expect(res.headers.getSetCookie()).toEqual(headers.getSetCookie());
    expect(res.headers.get('cache-control')).toBe('no-store');
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8787/api/auth/sign-in/email');
    expect(init.headers.get('cookie')).toBe('session=old');
    expect(init.headers.get('origin')).toBe('http://localhost:3000');
    expect(new TextDecoder().decode(init.body)).toBe('{"email":"a@example.invalid"}');
  } finally { vi.unstubAllGlobals(); }
});

it('BFF 연결 실패는 비밀값 없는 502와 no-store다', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('PRIVATE_CONNECTION')));
  try {
    const res = await GET(new Request('http://localhost:3000/api/auth/get-session'));
    expect(res.status).toBe(502);
    expect(await res.json()).toEqual({message:'API 서버에 연결할 수 없습니다'});
    expect(res.headers.get('cache-control')).toBe('no-store');
  } finally { vi.unstubAllGlobals(); }
});
