// GET /api/card/caption?date=&hide= → 카드와 같은 6개 · 캡션 JSON(O3 · D28). 공개, 쿠키 없음. 글꼴 · 그림 코드는 들어오지 않음
import { captionResponse, createCardHandler } from '../../../../server/card';
import { connectDb } from '../../../../server/db/client';
import { dbStore } from '../../../../server/db/store';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// 첫 요청 때 연결 1개(max: 1, D10). 읽기만(latestDate · widget)
function loadStore() {
  if (!process.env.DATABASE_URL) throw new Error('DB_CONFIG_MISSING');
  return dbStore(connectDb({ max: 1 }).db);
}

const handle = createCardHandler(loadStore, captionResponse);

export { handle as GET };
