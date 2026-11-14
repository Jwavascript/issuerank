// npm run today: 그날(KST 오늘) 랭킹이 이미 있는지(O9 · D37 · AC-01-3). daily.yml이 예약 · 정시(cron) 실행 전에 부름
// 종료 코드: 0 있음(daily 건너뜀) · 1 없음 · 2 DB 오류(daily는 그대로 실행). 오류 객체 · 환경 변수는 찍지 않음
import { connectDb } from './db/client';
import { healthStore } from './db/store';
import { todayResult } from './health-check';
import { hide } from './hide';
import { kstToday } from './sample';

async function main(): Promise<number> {
  const date = kstToday();
  if (!process.env.DATABASE_URL) { console.log('DB 오류: DATABASE_URL이 없습니다'); return 2; }
  let conn: ReturnType<typeof connectDb> | undefined;
  try {
    conn = connectDb({ max: 1 });
    const { code, line } = todayResult(date, await healthStore(conn.db).issueCount(date));
    console.log(line);
    return code;
  } catch (err) {
    const e = err instanceof Error && err.cause instanceof Error ? err.cause : err;
    console.log(`DB 오류: ${hide(e instanceof Error ? e.message : String(e))}`);
    return 2;
  } finally {
    await conn?.client.end().catch(() => {});
  }
}

main().then(code => { process.exitCode = code; });
