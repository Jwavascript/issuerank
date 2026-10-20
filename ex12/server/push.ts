// npm run push [-- --test <기기 코드>]: 아침 알림 발송(O5 · D30, push.yml이 하루 실행 daily가 끝나면 · 수동 시험). 규칙 · 요약 · 종료 코드는 push-run.ts
// 실제로 보내는 것은 GitHub Actions(GITHUB_ACTIONS=true)에서만 — 로컬은 키 확인 · 결정 · 대상 수까지만(ex12/.env에 실제 키가 있어도 구독자에게 나가지 않음)
// DB는 구독 표와 하루 실행 쪽 최근 수집일(latestDate)만 읽음. 오류는 문구만(오류 객체 · 환경 변수 금지), DB 오류 문구는 hide() + VAPID 개인 키도 ***
import { connectDb } from './db/client';
import { dbStore, pushStore } from './db/store';
import { hide } from './hide';
import { runPush } from './push-run';

const secret = process.env.VAPID_PRIVATE_KEY ?? '';
const safe = (text: string) => (secret ? hide(text).replaceAll(secret, '***') : hide(text));
const log = (line: string) => console.log(safe(line));

async function main(): Promise<number> {
  if (!process.env.DATABASE_URL) { log('DATABASE_URL이 없습니다'); return 2; }
  let conn: ReturnType<typeof connectDb> | undefined;
  try {
    conn = connectDb({ max: 1 });
    return await runPush(process.argv.slice(2), {
      store: pushStore(conn.db), latestDate: dbStore(conn.db).latestDate,
      fetch: globalThis.fetch, now: Date.now, sleep: ms => new Promise(r => setTimeout(r, ms)), env: process.env, log,
    });
  } catch (err) {
    // Drizzle이 감싼 원래 오류(cause)가 있으면 그 문구(summarize.ts와 같음). 첫 줄만(Drizzle 문구의 params 줄 = 행 값은 찍지 않음)
    const e = err instanceof Error && err.cause instanceof Error ? err.cause : err;
    log(`DB 오류: ${(e instanceof Error ? e.message : String(e)).split('\n')[0]}`);
    return 2;
  } finally {
    await conn?.client.end();
  }
}

main().then(code => { process.exitCode = code; });
