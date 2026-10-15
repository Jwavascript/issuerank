// BFF: /api/* 를 Hono API 서버로 그대로 중계
const API_URL = process.env.API_URL || 'http://localhost:8787';

async function proxy(req: Request) {
  const { pathname, search } = new URL(req.url);
  const headers = new Headers(req.headers);
  headers.delete('host');
  const authRequest = pathname.startsWith('/api/auth/');
  let res: Response;
  try {
    res = await fetch(API_URL + pathname + search, {
      method: req.method,
      headers,
      body: req.method === 'GET' ? undefined : await req.arrayBuffer(),
      redirect: 'manual',
      cache: 'no-store',
    });
  } catch {
    return Response.json({ message: 'API 서버에 연결할 수 없습니다' }, { status: 502, headers: authRequest ? { 'Cache-Control': 'no-store' } : undefined });
  }
  const responseHeaders = new Headers(res.headers);
  // Set-Cookie의 Expires에는 쉼표가 있으므로 split(',')하지 않는다.
  responseHeaders.delete('set-cookie');
  for (const cookie of res.headers.getSetCookie()) responseHeaders.append('set-cookie', cookie);
  if (authRequest) responseHeaders.set('Cache-Control', 'no-store');
  return new Response(res.body, { status: res.status, headers: responseHeaders });
}

export { proxy as GET, proxy as POST, proxy as DELETE };
