import { Hono } from 'hono';
import { bodyLimit } from 'hono/body-limit';
import { z } from 'zod';
import type { SessionEnv } from '../app';
import type { KeywordStore } from '../keyword-store';
import { keywordBaseline, publicKeyword } from '../keyword-store';
import { buildTimeline, keywordName, parseTimeline } from '../timeline';
import { BAD_REQUEST } from '../schemas';
import { pushService } from '../push-config';

export function timelineRoute(store: KeywordStore) {
  const route = new Hono();
  route.get('/', async c => {
    c.header('Cache-Control', 'no-store');
    const query = parseTimeline(new URL(c.req.url).searchParams);
    if (!query) return c.json({ message: '키워드 1~3개(각 2~40자)와 오늘까지 최대 90일의 기간을 입력해 주세요' }, 400);
    return c.json(buildTimeline(await store.snapshot(query.from, query.to), query));
  });
  return route;
}
export function keywordsRoute(store: KeywordStore) {
  const route = new Hono<SessionEnv>();
  route.use('*', async (c, next) => {
    c.header('Cache-Control', 'no-store');
    if (c.req.method !== 'GET') {
      let origin: string;
      try { origin = new URL(process.env.BETTER_AUTH_URL || 'http://localhost:3000').origin; } catch { return c.json({ message: BAD_REQUEST }, 403); }
      if (c.req.header('Origin') !== origin) return c.json({ message: BAD_REQUEST }, 403);
    }
    await next();
  });
  route.use('*', bodyLimit({ maxSize: 4096, onError: c => c.json({ message: BAD_REQUEST }, 413) }));
  route.get('/', async c => c.json((await store.list(c.get('userId'))).map(publicKeyword)));
  route.post('/', async c => {
    const body = z.object({ name: keywordName }).safeParse(await c.req.json().catch(() => null));
    if (!body.success) return c.json({ message: '키워드는 2~40자로 입력해 주세요' }, 400);
    const baseline = await keywordBaseline(store, body.data.name);
    const result = await store.create(c.get('userId'), body.data.name, baseline);
    if (result === 'duplicate' || result === 'full') return c.json({ message: result === 'full' ? '관심 키워드는 최대 5개까지 저장할 수 있습니다' : '이미 저장한 키워드입니다' }, 409);
    return c.json(publicKeyword(result), 201);
  });
  route.post('/device', async c => {
    const data = z.object({ endpoint: z.string().refine(v => !!pushService(v)), morning: z.boolean(), auth: z.string().min(1).max(128) }).safeParse(await c.req.json().catch(() => null));
    if (!data.success) return c.json({ message: BAD_REQUEST }, 400);
    const sessionId = c.get('sessionId');
    if (!sessionId) return c.json({ message: '로그인이 필요합니다' }, 401);
    const result = await store.link(c.get('userId'), sessionId, data.data.endpoint, data.data.morning, data.data.auth);
    if (result !== 'ok') return c.json({ message: result === 'conflict' ? '이 기기는 다른 계정에 연결되어 있습니다. 먼저 로그아웃해 주세요' : '이 기기의 아침 알림을 먼저 켜 주세요' }, result === 'conflict' ? 409 : 404);
    return c.body(null, 204);
  });
  route.post('/device/status', async c => {
    const body = z.object({ endpoint: z.string().refine(v => !!pushService(v)) }).safeParse(await c.req.json().catch(() => null));
    if (!body.success) return c.json({ message: BAD_REQUEST }, 400);
    const { endpoint } = body.data;
    return c.json(await store.device(c.get('userId'), c.get('sessionId') ?? '', endpoint));
  });
  route.delete('/device', async c => {
    const data = z.object({ endpoint: z.string().refine(v => !!pushService(v)) }).safeParse(await c.req.json().catch(() => null));
    if (!data.success) return c.json({ message: BAD_REQUEST }, 400);
    await store.unlink(c.get('userId'), data.data.endpoint);
    return c.body(null, 204);
  });
  route.patch('/:id', async c => {
    const id = c.req.param('id');
    const body = z.object({ alerts: z.boolean() }).safeParse(await c.req.json().catch(() => null));
    if (!z.uuid().safeParse(id).success || !body.success) return c.json({ message: BAD_REQUEST }, 400);
    const existing = (await store.list(c.get('userId'))).find(k => k.id === id);
    if (!existing) return c.json({ message: '키워드를 찾을 수 없습니다' }, 404);
    if (existing.alerts === body.data.alerts) return c.json(publicKeyword(existing));
    const baseline = body.data.alerts ? await keywordBaseline(store, existing.name) : existing.baseline;
    const result = await store.update(c.get('userId'), id, body.data.alerts, baseline);
    return result ? c.json(publicKeyword(result)) : c.json({ message: '키워드를 찾을 수 없습니다' }, 404);
  });
  route.delete('/:id', async c => {
    const id = c.req.param('id');
    if (!z.uuid().safeParse(id).success) return c.json({ message: BAD_REQUEST }, 400);
    if (!await store.remove(c.get('userId'), id)) return c.json({ message: '키워드를 찾을 수 없습니다' }, 404);
    return c.body(null, 204);
  });
  return route;
}
