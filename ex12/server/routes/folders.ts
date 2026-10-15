import { Hono } from 'hono';
import type { SessionEnv, Store } from '../app';
import { folderCreate, validate } from '../schemas';

export const foldersRoute = (store: Store) => {
  const route = new Hono<SessionEnv>();

  route.get('/', async c => c.json(await store.folders(c.get('userId')), 200));

  route.post('/', validate('json', folderCreate), async c => {
    const { name } = c.req.valid('json'); // 앞뒤 공백을 뺀 이름
    return c.json(await store.createFolder(c.get('userId'), name), 201);
  });
  return route;
};
