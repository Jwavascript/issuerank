import { Hono } from 'hono';
import { HTTPException } from 'hono/http-exception';
import { rankings } from './routes/rankings';
import { widgetRoute } from './routes/widget';
import { bookmarksRoute } from './routes/bookmarks';
import { foldersRoute } from './routes/folders';
import { pushRoute } from './routes/push';
import { BAD_REQUEST } from './schemas';
import type { Issue, Ranking, Widget } from './ranking';

export type Bookmark = Issue & { folderId: string; savedAt: string };
export type Folder = { id: string; name: string; count: number };
export type PushSubscriptionIn = { endpoint: string; p256dh: string; auth: string }; // 아침 알림 구독(O5) 한 기기

// 저장소: 라우트는 이 함수들만 부름. 메모리 구현은 sample.ts(테스트), DB 구현은 db/store.ts(api 서버)
export type Store = {
  latestDate(): Promise<string | null>;                              // 최근 수집일(KST 오늘 이하 가장 늦은 date), 없으면 null
  ranking(date: string): Promise<Ranking | null>;                    // 날짜의 이슈 → 랭킹, 없으면 null
  widget(date: string): Promise<Widget | null>;                      // 날짜의 랭킹 카드 → 위젯 목록(D22), 없으면 null
  issue(id: string): Promise<Issue | null>;                          // 이슈 하나(카드 8칸)
  bookmarks(userId: string): Promise<Bookmark[]>;                                  // 북마크 목록, savedAt ↓
  saveBookmark(userId: string, issue: Issue, folderId: string): Promise<Bookmark | null>; // 저장, 이미 북마크한 이슈면 null
  deleteBookmark(userId: string, issueId: string): Promise<boolean>;                 // 해제, 없으면 false
  folders(userId: string): Promise<Folder[]>;                                      // 폴더 목록(만든 순) + 개수
  createFolder(userId: string, name: string): Promise<Folder>;                       // 폴더 만들기
  deleteFolder(userId: string, folderId: string): Promise<boolean>;                 // 폴더 삭제(O6 · D31): 그 사용자의 그 폴더 북마크 → 폴더를 함께 지움, 없거나 남의 폴더면 false(쓰기 0)
  savePushSubscription(sub: PushSubscriptionIn, allowNew: boolean): Promise<'created' | 'updated' | null>; // 같은 endpoint면 키만 갱신(행 하나) = 'updated', 새 endpoint는 allowNew일 때만 넣음 = 'created'(O7 · D32 — API 201), 안 썼으면 null
  deletePushSubscription(endpoint: string): Promise<void>;                          // 그 endpoint 행 삭제(없어도 됨)
  pushSubscriptionCount(): Promise<number>;                                         // 구독 행 전체 수(상한 비교)
};

// Hono 앱(라우트 5개 · onError). 서버는 index.ts가 띄우고, 테스트는 app.request로 바로 부름
export type SessionEnv = { Variables: { userId: string } };
type Auth = {
  handler: (request: Request) => Promise<Response>;
  api?: { getSession: (input: { headers: Headers }) => Promise<{ user: { id: string } } | null> };
};

export function createApp(store: Store, auth?: Auth) {
  const app = new Hono<SessionEnv>();
  if (auth) app.on(['GET', 'POST'], '/api/auth/*', async c => {
    c.header('Cache-Control', 'no-store');
    const response = await auth.handler(c.req.raw);
    response.headers.set('Cache-Control', 'no-store');
    return response;
  });
  app.route('/api/rankings', rankings(store));
  app.route('/api/widget', widgetRoute(store));
  app.route('/api/push/subscriptions', pushRoute(store)); // 아침 알림 구독(O5): 로그인 없이 기기 단위
  // JSON 검사와 저장소 접근보다 먼저 서버 세션을 검증한다.
  app.use('/api/*', async (c, next) => {
    if (!/^\/api\/(bookmarks|folders)(\/|$)/.test(c.req.path)) return next();
    c.header('Cache-Control', 'no-store');
    const session = await auth?.api?.getSession({ headers: c.req.raw.headers });
    if (!session?.user.id) return c.json({ message: '로그인이 필요합니다' }, 401);
    c.set('userId', session.user.id);
    await next();
  });
  app.route('/api/bookmarks', bookmarksRoute(store));
  app.route('/api/folders', foldersRoute(store));

  // 깨진 JSON 본문(검사기가 던지는 400)은 { message }로. 그 밖은 Hono 기본 처리와 같음
  app.onError((err, c) => {
    if (err instanceof HTTPException && err.status === 400) return c.json({ message: BAD_REQUEST }, 400);
    if (err instanceof HTTPException) return err.getResponse();
    console.error('API_INTERNAL_ERROR');
    return c.text('Internal Server Error', 500);
  });
  return app;
}
