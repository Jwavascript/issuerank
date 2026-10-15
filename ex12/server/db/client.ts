// DB 연결: DATABASE_URL(Supabase Transaction pooler, 포트 6543). 이 모드는 prepared statement를 못 쓰므로 prepare: false
// 연결을 만드는 곳은 api 서버(index.ts) · BFF(bff.ts) · 수집(collect.ts) · 묶기(group.ts) · 요약(summarize.ts)뿐
// max를 주지 않으면 postgres 기본값 그대로. 서버리스 BFF만 max: 1(D10)
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';

export function connectDb(options: { max?: number } = {}) {
  const client = postgres(process.env.DATABASE_URL!, { prepare: false, onnotice: () => {}, ...(options.max === undefined ? {} : { max: options.max }) });
  return { client, db: drizzle(client) };
}

export type Db = ReturnType<typeof connectDb>['db'];
