// npm run health [-- --run <결론>] [--mode after|morning]: 그날(KST 오늘) 운영 상태 검사(O9 · D37 · AC-01-4). 판단은 health-check.ts
// 출력 = 첫 줄 `상태 OK|문제 n` + 문제마다 한 줄(숫자 · 코드 · 섹션 키 · 시각만). 종료 코드: 0 OK · 1 문제 · 2 검사 오류(CHECK_ERROR) · 인수 오류
import { connectDb } from './db/client';
import { healthStore } from './db/store';
import { checkHealth, formatHealth, parseHealthArgs } from './health-check';
import { kstToday } from './sample';

async function main(): Promise<number> {
  const options = parseHealthArgs(process.argv.slice(2));
  if (!options) { console.log('--run <결론> · --mode after|morning만 받습니다'); return 2; }
  const date = kstToday();
  let conn: ReturnType<typeof connectDb> | undefined;
  try {
    if (!process.env.DATABASE_URL) throw new Error('DB_CONFIG_MISSING');
    conn = connectDb({ max: 1 });
    const problems = checkHealth(await healthStore(conn.db).day(date), options);
    for (const line of formatHealth(date, problems)) console.log(line);
    return problems.length ? 1 : 0;
  } catch {
    // 오류 문구 · 객체는 찍지 않음(공개 이슈 · 실행 요약에 그대로 감)
    for (const line of formatHealth(date, [{ code: 'CHECK_ERROR', text: 'DB를 읽지 못함' }])) console.log(line);
    return 2;
  } finally {
    await conn?.client.end().catch(() => {});
  }
}

main().then(code => { process.exitCode = code; });
