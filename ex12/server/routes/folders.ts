import { Hono } from 'hono';
import type { SessionEnv, Store } from '../app';
import { BAD_REQUEST, FOLDER_ID, folderCreate, validate } from '../schemas';

export const foldersRoute = (store: Store) => {
  const route = new Hono<SessionEnv>();

  route.get('/', async c => c.json(await store.folders(c.get('userId')), 200));

  route.post('/', validate('json', folderCreate), async c => {
    const { name } = c.req.valid('json'); // 앞뒤 공백을 뺀 이름
    return c.json(await store.createFolder(c.get('userId'), name), 201);
  });

  // 폴더 삭제(O6 · D31): 세션(app.ts 미들웨어, 401) → id 형식(400) → 저장소. 없는 폴더와 남의 폴더는 같은 404, 지우면 204(본문 없음)
  route.delete('/:folderId', async c => {
    const folderId = c.req.param('folderId');
    if (!FOLDER_ID.test(folderId)) return c.json({ message: BAD_REQUEST }, 400);
    if (!(await store.deleteFolder(c.get('userId'), folderId))) return c.json({ message: '폴더를 찾을 수 없습니다' }, 404);
    return c.body(null, 204);
  });
  return route;
};
