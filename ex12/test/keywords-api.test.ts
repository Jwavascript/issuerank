import { describe, expect, it, vi } from 'vitest';
import { createApp } from '../server/app';
import { createBff } from '../server/bff';
import { memoryStore } from '../server/sample';
import { article, memoryKeywords, snapshot } from './helpers/keyword-fixture';

const endpoint = 'https://fcm.googleapis.com/fcm/send/fixture';
function setup() {
  const fixture = memoryKeywords(snapshot([article(1)]));
  const auth = { handler: async () => new Response(null, { status: 404 }), api: { getSession: async ({ headers }: { headers: Headers }) => headers.has('x-user') ? { user: { id: headers.get('x-user')! }, session: { id: 'session-' + headers.get('x-user') } } : null } };
  const app = createApp(memoryStore, auth, fixture.store);
  const call = (path: string, method = 'GET', body?: unknown, user: string | null = 'one', extra: Record<string, string> = {}) => app.request('/api' + path, { method, headers: { Origin: 'http://localhost:3000', 'Content-Type': 'application/json', ...(user ? { 'x-user': user } : {}), ...extra }, ...(body === undefined ? {} : { body: typeof body === 'string' ? body : JSON.stringify(body) }) });
  return { ...fixture, app, call };
}
describe('키워드 API·계정 분리', () => {
  it('공개 검색은 로그인 불필요하며 입력 오류는 400', async () => {
    const { call } = setup();
    expect((await call('/timeline?q=삼성전자', 'GET', undefined, null)).status).toBe(200);
    expect((await call('/timeline?q=a')).status).toBe(400);
  });
  it('인증을 JSON 파싱·개인 저장소 접근보다 먼저 검사한다', async () => {
    const { call, store } = setup(); const spy = vi.spyOn(store, 'create');
    const res = await call('/keywords', 'POST', '{broken', null);
    expect(res.status).toBe(401); expect(res.headers.get('Cache-Control')).toBe('no-store'); expect(spy).not.toHaveBeenCalled();
  });
  it('Origin·본문 크기·깨진JSON·이름 검증', async () => {
    const { call } = setup();
    expect((await call('/keywords', 'POST', { name: '삼성전자' }, 'one', { Origin: 'https://evil.example' })).status).toBe(403);
    expect((await call('/keywords', 'POST', 'x'.repeat(4100))).status).toBe(413);
    expect((await call('/keywords', 'POST', '{broken')).status).toBe(400);
    expect((await call('/keywords', 'POST', { name: 'x' })).status).toBe(400);
  });
  it('중복·5개 상한·빈 색상 재사용·서버 기준 소유권·baseline 비공개', async () => {
    const { call } = setup();
    const created = await call('/keywords', 'POST', { name: ' 삼성전자 ' });
    expect(created.status).toBe(201); const first = await created.json(); expect(first.name).toBe('삼성전자'); expect(first.baseline).toBeUndefined();
    expect(Object.keys(first).sort()).toEqual(['id', 'name', 'color', 'alerts', 'createdAt'].sort());
    expect((await call('/keywords', 'POST', { name: '삼성전자' })).status).toBe(409);
    for (const name of ['반도체', '기준금리', '전기차', '인공지능']) expect((await call('/keywords', 'POST', { name })).status).toBe(201);
    expect((await call('/keywords', 'POST', { name: '여섯번째' })).status).toBe(409);
    expect(await (await call('/keywords', 'GET', undefined, 'two')).json()).toEqual([]);
    expect((await call('/keywords/' + first.id, 'DELETE', undefined, 'two')).status).toBe(404);
    expect((await call('/keywords/' + first.id, 'PATCH', { alerts: true }, 'two')).status).toBe(404);
    expect((await call('/keywords/' + first.id, 'DELETE')).status).toBe(204);
    expect((await (await call('/keywords', 'POST', { name: '새키워드' })).json()).color).toBe(first.color);
  });
  it('알림 켜기는 현재 결과로 baseline을 갱신하고 재요청은 baseline을 바꾸지 않는다', async () => {
    const { call, store } = setup();
    const first = await (await call('/keywords', 'POST', { name: '삼성전자' })).json();
    expect((await call('/keywords/' + first.id, 'PATCH', { alerts: true })).status).toBe(200);
    expect((await store.list('one'))[0].baseline.length).toBeGreaterThan(0);
    const spy = vi.spyOn(store, 'update');
    await call('/keywords/' + first.id, 'PATCH', { alerts: true }); expect(spy).not.toHaveBeenCalled();
  });
  it('기기는 구독 암호화값을 증명하고 다른 활성 계정에 연결하지 못한다', async () => {
    const { call } = setup();
    expect((await call('/keywords/device', 'POST', { endpoint, morning: false, auth: 'bad' })).status).toBe(404);
    expect((await call('/keywords/device', 'POST', { endpoint, morning: false, auth: 'proof' })).status).toBe(204);
    expect((await call('/keywords/device', 'POST', { endpoint, morning: true, auth: 'proof' }, 'two')).status).toBe(409);
    expect(await (await call('/keywords/device/status', 'POST', { endpoint }, 'two')).json()).toBeNull();
    expect((await (await call('/keywords/device/status', 'POST', { endpoint })).json()).active).toBe(true);
    expect((await call('/keywords/device', 'DELETE', { endpoint })).status).toBe(204);
    expect(await (await call('/keywords/device/status', 'POST', { endpoint })).json()).toBeNull();
  });
  it('시작 실패에도 BFF 개인 응답은 no-store이고 비밀값을 내보내지 않는다', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    try { const bff = createBff(() => { throw new Error('PRIVATE_SECRET'); }); const res = await bff(new Request('http://localhost/api/keywords'));
      expect(res.status).toBe(502); expect(res.headers.get('Cache-Control')).toBe('no-store'); expect(await res.text()).not.toContain('PRIVATE_SECRET');
    } finally { log.mockRestore(); }
  });
});
