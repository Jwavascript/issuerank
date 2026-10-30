import { Hono, type Context, type Next } from 'hono';
import { bodyLimit } from 'hono/body-limit';
import type { Store } from '../app';
import { BODY_LIMIT, MAX_SUBSCRIPTIONS, pushService, validKeys } from '../push-config';
import { BAD_REQUEST } from '../schemas';

// 아침 알림 구독(O5 · D30, AC-14-1 · 14-4): POST · DELETE /api/push/subscriptions. 로그인 · 쿠키 없이 기기 단위
// 검사 순서 Origin → 크기(4KB) → JSON · 칸 → endpoint(알려진 푸시 서비스 4종) → 키 → 저장. 모든 응답 no-store · Set-Cookie 0,
// 로그는 안전 코드(PUSH_SAVE_FAILED)만 — endpoint · 키는 찍지 않음. 그 밖 메서드는 Hono 기본 404
const FULL = '지금은 알림을 켤 수 없습니다';
const LOCAL = 'http://localhost:3000';

// 화면 출처 = BETTER_AUTH_URL의 origin(배포 https://issuerank-kappa.vercel.app · 비면 로컬). 형식이 틀리면 null → 모두 403
const appOrigin = () => { try { return new URL(process.env.BETTER_AUTH_URL || LOCAL).origin; } catch { return null; } };

// 다른 사이트의 스크립트로 구독 · 해지를 못 하게(Origin이 없거나 다르면 403). curl 같은 위조는 못 막음 — 상한 · 404/410 정리로 대비
const sameOrigin = async (c: Context, next: Next) => {
  const origin = appOrigin();
  if (!origin || c.req.header('Origin') !== origin) return c.json({ message: BAD_REQUEST }, 403);
  await next();
};
const limit = bodyLimit({ maxSize: BODY_LIMIT, onError: c => c.json({ message: BAD_REQUEST }, 413) });

// 본문 JSON 객체(깨졌거나 객체가 아니면 null)
async function readBody(c: Context): Promise<Record<string, unknown> | null> {
  try {
    const value: unknown = await c.req.json();
    return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : null;
  } catch { return null; }
}

export const pushRoute = (store: Store) => {
  const route = new Hono();
  route.use('*', async (c, next) => { c.header('Cache-Control', 'no-store'); await next(); });

  // 본문 = PushSubscription.toJSON() 꼴 { endpoint, expirationTime?, keys: { p256dh, auth } } — 다른 칸 · expirationTime은 무시
  route.post('/', sameOrigin, limit, async c => {
    const body = await readBody(c);
    const keys = body?.keys as Record<string, unknown> | undefined;
    if (!body || !pushService(body.endpoint) || !keys || typeof keys !== 'object' || !validKeys(keys.p256dh, keys.auth)) {
      return c.json({ message: BAD_REQUEST }, 400);
    }
    const sub = { endpoint: body.endpoint as string, p256dh: keys.p256dh as string, auth: keys.auth as string };
    let saved: 'created' | 'updated' | null;
    try {
      // 5,000행이면 새 endpoint만 막음(같은 endpoint의 키 갱신은 됨)
      const allowNew = (await store.pushSubscriptionCount()) < MAX_SUBSCRIPTIONS;
      saved = await store.savePushSubscription(sub, allowNew);
      if (!saved) return c.json({ message: FULL }, 503);
    } catch {
      console.error('PUSH_SAVE_FAILED');
      return c.text('Internal Server Error', 500);
    }
    // 새 endpoint 행 = 201 { created: true }(화면은 이미 있던 기기 구독인데 201이면 서버에서 지워진 것으로 보고 구독을 새로 만듦 — O7 · D32) · 이미 있던 endpoint = 204
    return saved === 'created' ? c.json({ created: true }, 201) : c.body(null, 204);
  });

  // 해지: 본문 { endpoint } → 그 행 삭제, 없어도 204(여러 번 같아도 됨)
  route.delete('/', sameOrigin, limit, async c => {
    const body = await readBody(c);
    if (!body || !pushService(body.endpoint)) return c.json({ message: BAD_REQUEST }, 400);
    try {
      await store.deletePushSubscription(body.endpoint as string);
    } catch {
      console.error('PUSH_SAVE_FAILED');
      return c.text('Internal Server Error', 500);
    }
    return c.body(null, 204);
  });
  return route;
};
