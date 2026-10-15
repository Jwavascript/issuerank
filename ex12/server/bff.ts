// BFF: Next API 라우트(app/api/[...path]/route.ts)가 Hono 앱을 같은 프로세스에서 app.fetch로 부른다(D10, Vercel 서버리스)
// 앱은 첫 요청 때 한 번만 만들어 모듈 범위에 두고 재사용한다. 실패하면 저장하지 않아 다음 요청이 다시 시도한다.
import { createApp } from './app';
import { authOptions, createAuth } from './auth';
import { connectDb } from './db/client';
import { dbStore } from './db/store';

type App = { fetch: (request: Request) => Response | Promise<Response> };

const UNAVAILABLE = 'API 서버에 연결할 수 없습니다';
const isPrivate = (pathname: string) => pathname.startsWith('/api/auth/') || /^\/api\/(bookmarks|folders)(\/|$)/.test(pathname);

// 서버리스 함수 하나가 연결 1개만 쓰도록 max: 1. 빌드 중에는 불리지 않는다(첫 요청 때만).
export async function loadApp(): Promise<App> {
  let connection: ReturnType<typeof connectDb> | undefined;
  try {
    if (!process.env.DATABASE_URL) throw new Error('DB_CONFIG_MISSING');
    authOptions(process.env);
    connection = connectDb({ max: 1 });
    return createApp(dbStore(connection.db), createAuth(connection.db, process.env));
  } catch (err) {
    void connection?.client.end();
    throw err;
  }
}

export function createBff(load: () => App | Promise<App>) {
  let ready: Promise<App> | undefined;
  return async function handle(req: Request): Promise<Response> {
    const privateRequest = isPrivate(new URL(req.url).pathname);
    let res: Response;
    try {
      const pending = ready ??= Promise.resolve().then(load);
      let app: App;
      try { app = await pending; } catch (err) {
        if (ready === pending) ready = undefined;
        throw err;
      }
      res = await app.fetch(req);
    } catch {
      // 오류 객체 · 스택 · env는 출력하지 않는다.
      console.error('API_START_FAILED');
      return Response.json({ message: UNAVAILABLE }, { status: 502, headers: privateRequest ? { 'Cache-Control': 'no-store' } : undefined });
    }
    const headers = new Headers(res.headers);
    // Set-Cookie의 Expires에는 쉼표가 있으므로 split(',')하지 않는다.
    headers.delete('set-cookie');
    for (const cookie of res.headers.getSetCookie()) headers.append('set-cookie', cookie);
    if (privateRequest) headers.set('Cache-Control', 'no-store');
    return new Response(res.body, { status: res.status, statusText: res.statusText, headers });
  };
}
