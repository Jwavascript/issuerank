// BFF: /api/* 를 같은 프로세스의 Hono 앱(app.fetch)으로 처리. 상시 8787 서버 없이 Vercel 함수 하나로 동작(D10)
import { createBff, loadApp } from '../../../server/bff';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const handle = createBff(loadApp);

export { handle as GET, handle as POST, handle as DELETE };
