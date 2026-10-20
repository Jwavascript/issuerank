// 폴더 삭제 API(O6 · D31 · AC-10-5 · 10-3 · 10-4): Hono 앱(메모리 저장소)을 app.request로 부름. 시각은 setup에서 2026-10-07 12:00 KST로 고정
// 세션은 api.test 꼴 가짜(샘플 사용자) 또는 ownership.test 꼴 Better Auth 메모리 두 계정
import { randomUUID } from 'node:crypto';
import { betterAuth } from 'better-auth';
import { memoryAdapter, type MemoryDB } from 'better-auth/adapters/memory';
import { describe, expect, it, vi } from 'vitest';
import { authOptions } from '../server/auth';
import { createApp, type Store } from '../server/app';
import { createBff } from '../server/bff';
import { memoryStore, SAMPLE_USER } from '../server/sample';

type Bookmark = { id: string; folderId: string; savedAt: string };
type Folder = { id: string; name: string; count: number };
const MSG = { badRequest: '요청 형식이 올바르지 않습니다', noFolder: '폴더를 찾을 수 없습니다', login: '로그인이 필요합니다' };

// 저장소 함수를 모두 감싸 호출 수를 셈(동작은 메모리 저장소 그대로)
function spyStore(): Store {
  const store = { ...memoryStore };
  for (const key of Object.keys(store) as (keyof Store)[]) Object.assign(store, { [key]: vi.fn(memoryStore[key]) });
  return store;
}
const fakeAuth = { handler: async () => new Response(null, { status: 404 }), api: { getSession: async () => ({ user: { id: SAMPLE_USER } }) } };

function sampleApp() {
  const store = spyStore();
  const app = createApp(store, fakeAuth);
  const call = (path: string, method = 'GET', body?: unknown) => app.request(path, { method, ...(body === undefined ? {} : { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }) });
  const bookmarks = async () => (await (await call('/api/bookmarks')).json()) as Bookmark[];
  const folders = async () => (await (await call('/api/folders')).json()) as Folder[];
  return { store, app, call, bookmarks, folders };
}
const expectMessage = async (res: Response, status: number, message: string) => {
  expect(res.status).toBe(status);
  expect(res.headers.get('cache-control')).toBe('no-store');
  expect(await res.json()).toEqual({ message });
};
const todayPolTop = async () => (await memoryStore.ranking('2026-10-07'))!.sections[0].issues[0].id;

describe('DELETE /api/folders/{folderId} — 샘플 사용자', () => {
  it('① f1 삭제: 204 · 본문 0 · no-store → 폴더 f2 3 · f3 3 · 북마크 6(f1의 4개만 빠지고 순서 그대로) → 다시 404 → f1에 저장 404', async () => {
    const { call, bookmarks, folders } = sampleApp();
    const before = await bookmarks();
    expect(before.filter(b => b.folderId === 'f1')).toHaveLength(4);

    const res = await call('/api/folders/f1', 'DELETE');
    expect(res.status).toBe(204);
    expect(res.headers.get('cache-control')).toBe('no-store');
    expect(await res.text()).toBe('');

    expect(await folders()).toEqual([{ id: 'f2', name: '부동산·금리', count: 3 }, { id: 'f3', name: '나중에 읽기', count: 3 }]);
    const after = await bookmarks();
    expect(after).toHaveLength(6);
    expect(after).toEqual(before.filter(b => b.folderId !== 'f1'));

    await expectMessage(await call('/api/folders/f1', 'DELETE'), 404, MSG.noFolder);
    await expectMessage(await call('/api/bookmarks', 'POST', { issueId: await todayPolTop(), folderId: 'f1' }), 404, MSG.noFolder);
    expect(await bookmarks()).toHaveLength(6);
  });

  it('② 새로 만든 빈 폴더 삭제: 204 → 그 폴더만 빠지고 북마크 10개 그대로', async () => {
    const { call, bookmarks, folders } = sampleApp();
    const before = await bookmarks();
    const made = (await (await call('/api/folders', 'POST', { name: '빈 폴더' })).json()) as Folder;
    expect(made).toEqual({ id: expect.stringMatching(/^f[0-9a-f-]{36}$/), name: '빈 폴더', count: 0 });
    const res = await call(`/api/folders/${made.id}`, 'DELETE');
    expect([res.status, res.headers.get('cache-control'), await res.text()]).toEqual([204, 'no-store', '']);
    expect((await folders()).map(f => f.id)).toEqual(['f1', 'f2', 'f3']);
    expect(await bookmarks()).toEqual(before);
  });

  it('③ 형식 틀림 9가지: 모두 400 같은 문구 · no-store · 저장소 deleteFolder 호출 0 · 변화 0', async () => {
    const { store, call, bookmarks, folders } = sampleApp();
    const [b0, f0] = [await bookmarks(), await folders()];
    const uuid = randomUUID();
    const bad = ['abc', 'F1', 'f', 'f1x', 'f%20', 'f1%2Fx', `f${uuid.toUpperCase()}`, `f${uuid}0`, `f${'0'.repeat(64)}`];
    expect(bad.at(-1)).toHaveLength(65);
    for (const id of bad) await expectMessage(await call(`/api/folders/${id}`, 'DELETE'), 400, MSG.badRequest);
    expect(store.deleteFolder).not.toHaveBeenCalled();
    expect([await bookmarks(), await folders()]).toEqual([b0, f0]);
  });

  it('④ 없는 폴더 f999 · f{새 UUID}: 404 같은 문구', async () => {
    const { store, call, folders } = sampleApp();
    for (const id of ['f999', `f${randomUUID()}`]) await expectMessage(await call(`/api/folders/${id}`, 'DELETE'), 404, MSG.noFolder);
    expect(store.deleteFolder).toHaveBeenCalledTimes(2);
    expect(await folders()).toHaveLength(3);
  });

  it('⑦ GET /api/folders/f1 · DELETE /api/folders · DELETE /api/folders/f1/x: Hono 기본 404 · 지운 것 0', async () => {
    const { store, call, bookmarks, folders } = sampleApp();
    for (const [path, method] of [['/api/folders/f1', 'GET'], ['/api/folders', 'DELETE'], ['/api/folders/f1/x', 'DELETE']]) {
      const res = await call(path, method);
      expect(res.status, `${method} ${path}`).toBe(404);
      expect(await res.text()).toBe('404 Not Found');
    }
    expect(store.deleteFolder).not.toHaveBeenCalled();
    expect((await folders()).map(f => [f.id, f.count])).toEqual([['f1', 4], ['f2', 3], ['f3', 3]]);
    expect(await bookmarks()).toHaveLength(10);
  });

  it('⑧ createBff를 지나도 204 · 404 상태 · 본문 · no-store 같음', async () => {
    const { app } = sampleApp();
    const bff = createBff(() => app);
    const del = (id: string) => bff(new Request(`http://localhost:3000/api/folders/${id}`, { method: 'DELETE' }));
    const ok = await del('f2');
    expect([ok.status, ok.headers.get('cache-control'), await ok.text()]).toEqual([204, 'no-store', '']);
    await expectMessage(await del('f2'), 404, MSG.noFolder);
    await expectMessage(await del('bad'), 400, MSG.badRequest);
  });

  it('⑨ memoryStore.deleteFolder(남의 userId, f1) 직접: false · 폴더 · 북마크 변화 0', async () => {
    const before = [await memoryStore.folders(SAMPLE_USER), await memoryStore.bookmarks(SAMPLE_USER)];
    expect(await memoryStore.deleteFolder('o6-other-user', 'f1')).toBe(false);
    expect([await memoryStore.folders(SAMPLE_USER), await memoryStore.bookmarks(SAMPLE_USER)]).toEqual(before);
    expect(await memoryStore.deleteFolder(SAMPLE_USER, 'f1')).toBe(true);
    expect((await memoryStore.folders(SAMPLE_USER)).map(f => f.id)).toEqual(['f2', 'f3']);
  });
});

// ── Better Auth 메모리 두 계정(ownership.test 꼴) ──
const origin = 'http://localhost:3000';
const password = 'Fixture-password-987!';
function authSetup() {
  const db: MemoryDB = { user: [], session: [], account: [], verification: [] };
  const auth = betterAuth({ ...authOptions({ BETTER_AUTH_SECRET: 'only-fixture-auth-secret-1234567890abcdef', BETTER_AUTH_URL: origin }), database: memoryAdapter(db) });
  const store = spyStore();
  const app = createApp(store, auth);
  const request = (path: string, method = 'GET', body?: unknown, cookie?: string) => app.request(origin + path, {
    method, headers: { Origin: origin, ...(cookie ? { Cookie: cookie } : {}), ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const signup = async (email: string) => {
    const res = await request('/api/auth/sign-up/email', 'POST', { name: 'fixture', email, password });
    expect(res.status).toBe(200);
    return { id: (await res.json()).user.id as string, cookie: res.headers.getSetCookie().map(c => c.split(';')[0]).join('; ') };
  };
  return { db, store, request, signup };
}

describe('DELETE /api/folders/{folderId} — 계정 두 개 · 세션', () => {
  it('⑤ B가 A 폴더 → 404(없는 폴더와 상태 · 본문 · 머리 같음) · A 그대로 / A가 자기 폴더 → 204 · B 폴더 · 북마크 깊은 비교 같음', async () => {
    const { request, signup } = authSetup();
    const a = await signup('o6-a@example.invalid'), b = await signup('o6-b@example.invalid');
    const [issue1, issue2] = (await memoryStore.ranking('2026-10-07'))!.sections[0].issues;
    const folderOf = async (u: { cookie: string }, name: string) => (await (await request('/api/folders', 'POST', { name }, u.cookie)).json()) as Folder;
    const fa = await folderOf(a, 'A 폴더'), fb = await folderOf(b, 'B 폴더'), fb2 = await folderOf(b, 'B 둘째');
    expect((await request('/api/bookmarks', 'POST', { issueId: issue1.id, folderId: fa.id }, a.cookie)).status).toBe(201);
    expect((await request('/api/bookmarks', 'POST', { issueId: issue2.id, folderId: fa.id }, a.cookie)).status).toBe(201);
    expect((await request('/api/bookmarks', 'POST', { issueId: issue1.id, folderId: fb.id }, b.cookie)).status).toBe(201);
    const listOf = async (u: { cookie: string }) => [await (await request('/api/folders', 'GET', undefined, u.cookie)).json(), await (await request('/api/bookmarks', 'GET', undefined, u.cookie)).json()];
    const aBefore = await listOf(a), bBefore = await listOf(b);

    const other = await request(`/api/folders/${fa.id}`, 'DELETE', undefined, b.cookie);
    const none = await request(`/api/folders/f${randomUUID()}`, 'DELETE', undefined, b.cookie);
    expect([other.status, none.status]).toEqual([404, 404]);
    expect([...other.headers.entries()]).toEqual([...none.headers.entries()]);
    expect(await other.text()).toBe(await none.text());
    expect(await listOf(a)).toEqual(aBefore);
    expect(aBefore[0]).toEqual([{ id: fa.id, name: 'A 폴더', count: 2 }]);

    const own = await request(`/api/folders/${fa.id}`, 'DELETE', undefined, a.cookie);
    expect([own.status, own.headers.get('cache-control'), await own.text()]).toEqual([204, 'no-store', '']);
    expect(await listOf(a)).toEqual([[], []]);
    expect(await listOf(b)).toEqual(bBefore);
    expect(bBefore[0]).toEqual([{ id: fb.id, name: 'B 폴더', count: 1 }, { id: fb2.id, name: 'B 둘째', count: 0 }]);
  });

  it('⑥ 쿠키 없음 · 변조 · 만료: 형식 틀린 id 포함 모두 401 · no-store · 저장소 호출 0', async () => {
    const { db, store, request, signup } = authSetup();
    const a = await signup('o6-expired@example.invalid');
    db.session[0].expiresAt = new Date('2000-01-01');
    for (const cookie of [undefined, a.cookie + 'wrong', a.cookie]) for (const id of ['f1', `f${randomUUID()}`, 'bad', 'F1']) {
      await expectMessage(await request(`/api/folders/${id}`, 'DELETE', undefined, cookie), 401, MSG.login);
    }
    for (const key of ['deleteFolder', 'folders', 'bookmarks', 'createFolder', 'saveBookmark', 'deleteBookmark'] as const) expect(store[key]).not.toHaveBeenCalled();
  });
});
