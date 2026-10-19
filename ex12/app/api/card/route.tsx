// GET /api/card?date=&hide= → 1080×1350 PNG(O3 · D28). 공개, 쿠키 없음. 빌드 때 DB를 읽지 않게 동적
// 이 함수에만 글꼴이 들어감(next.config.mjs outputFileTracingIncludes '/api/card')
import { createCardHandler } from '../../../server/card';
import { cardPngResponse } from '../../../server/card-image';
import { connectDb } from '../../../server/db/client';
import { dbStore } from '../../../server/db/store';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// 첫 요청 때 연결 1개(max: 1, D10). 읽기만(latestDate · widget)
function loadStore() {
  if (!process.env.DATABASE_URL) throw new Error('DB_CONFIG_MISSING');
  return dbStore(connectDb({ max: 1 }).db);
}

const handle = createCardHandler(loadStore, card => cardPngResponse(card));

export { handle as GET };
