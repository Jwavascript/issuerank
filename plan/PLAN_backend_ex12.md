# PLAN_backend_ex12 — 배포(Vercel)

> 이유: 지금은 내 기기에서 두 서버(Next 3000 · Hono 8787)를 띄워야만 화면이 열려, 폰 등 다른 기기에서 같은 계정 · 같은 북마크를 보일 수 없다(D10). 그래서 최신 폴더 하나(`ex12/`)를 Vercel에 배포하고, Vercel은 상시 서버를 돌리지 않으므로 Next API 라우트가 Hono 앱(`createApp`)을 같은 프로세스에서 직접 부르게 바꾼다.
> 코드 폴더: `ex12/` (`ex09/` 복사 — 최신 = ex11 기능 + 하루 실행. ex09는 보존). Vercel 프로젝트 Root Directory = `ex12`, 별도 v1 폴더 없음.
> 선행 조건: 원래 ex09 심사 PASS 뒤였으나, 사용자 지시(2026-10-15)로 ex09가 예약 실행 확인 1줄만 남은 PASS 보류 상태에서 제작 시작. 2026-10-16 예약 실행에서 문제가 나오면 ex09와 ex12를 함께 고친다(사용자 결정). D10 확정(2026-10-15).
> SPEC 근거: AC-08-3(US-08 · US-06, 다른 기기 같은 목록). 회귀: AC-03-1 · AC-05-1 · AC-05-2 · AC-06-1 · AC-06-2 · AC-07-1~3 · AC-08-1 · AC-08-2 · AC-09-1~3 · AC-10-1~3. D1 · D9 · D10(확정), R4 · R7 적용.

## 이번 범위
- BFF를 "8787로 fetch 중계"에서 "같은 프로세스에서 `app.fetch(req)`"로 바꾸고, Vercel 설정 파일 · Node 버전 고정 · 서버리스용 DB 연결(연결 1개)을 더함. API 경로 · 응답 · 쿠키 · 화면 · 인증 규칙(`server/auth.ts`) · 소유권 · AI · 수집/묶기/요약/하루 실행 코드는 ex09 그대로(한 글자도 안 바꿈).
- 새 패키지: 없음. Hono 공식 Next.js 연동의 `handle`은 `(req) => app.fetch(req)`와 같고(설치된 hono 4.13.13의 `hono/vercel`은 v5에서 제거 예고 · 대체 `@hono/vercel`은 새 패키지) → 라우트에서 `app.fetch`를 직접 부름.
- 하지 않음: 하루 실행을 Vercel Cron으로 옮기기(GitHub Actions 그대로, 함수 시간 제한 Hobby 300초) · 하루 실행 코드 변경 · ex12 제작 · 심사 중 워크플로 변경(ex12 PASS 뒤 닫기에서만) · 프리뷰 배포에서 로그인 · 사용자 도메인 연결 · Vercel Analytics 등 부가 기능 · Google OAuth · 이메일 발송 · `web/`. 배포 생성 · 환경 변수 등록 · 도메인은 사용자가 함(에이전트 안 함).

## 배포 규칙
| 항목 | 규칙 |
|---|---|
| 요청 경로 | 브라우저 → `https://{프로젝트}.vercel.app/api/*` → `app/api/[...path]/route.ts`(Node 런타임, `export const runtime = 'nodejs'` · `dynamic = 'force-dynamic'`) → 새 `server/bff.ts`의 `createBff(loadApp)`가 만든 처리 함수 → `app.fetch(req)`. `GET · POST · DELETE`만 내보냄(기존과 같음). `API_URL`은 BFF가 더 이상 읽지 않음(`.env.example` 주석만 고침, 이름은 남김) |
| 앱 준비 | `loadApp()`은 첫 요청 때 한 번만 `DATABASE_URL` 확인 → `authOptions(process.env)` → `connectDb({ max: 1 })` → `createApp(dbStore(db), createAuth(db, process.env))`를 모듈 범위에 저장해 다음 요청이 재사용(빌드 중에는 DB 접속 0). 실패하면 저장하지 않고(다음 요청이 다시 시도) 502 |
| 계약 유지 | 응답 상태 · 본문 · 헤더 그대로, 복수 `Set-Cookie` 각각 보존(쉼표로 나누지 않음), 요청 `Cookie` · `Origin` · 본문 그대로 앱에 전달. `/api/auth/*` · `/api/bookmarks*` · `/api/folders*`는 `Cache-Control: no-store`(성공 · 실패 · 502 모두). 앱 준비 실패나 `app.fetch`가 던지면 502 `{ message: "API 서버에 연결할 수 없습니다" }`(문구 그대로 = 화면 기존 안내), 로그는 `API_START_FAILED` 한 줄만(오류 객체 · 스택 · env 출력 금지) |
| DB(서버리스) | `server/db/client.ts`의 `connectDb(options?)`에 `max`만 선택 인수로 더함(없으면 지금과 똑같이 postgres 기본값 → `npm run api` · collect · group · summarize 동작 불변). BFF만 `max: 1`. `DATABASE_URL`은 지금과 같은 Supabase 풀러 transaction 모드 6543 · `prepare: false` · IPv4(R7, 서버리스 권장 모드) |
| 로컬 개발 | `npm run dev` 하나로 화면 + API(Next가 `ex12/.env`를 읽음). `npm run api`(8787 · `server/index.ts`)는 그대로 남겨 API 단독 확인 · 기존 기동 테스트용. 두 서버를 같이 띄울 필요 없음 |
| Better Auth | 코드 변경 없음(`baseURL` = `BETTER_AUTH_URL`의 origin, `trustedOrigins` = 그 하나, CSRF · Origin 검사 유지). Vercel Production의 `BETTER_AUTH_URL` = `https://{프로젝트}.vercel.app`(끝 `/` 없이 origin만, `{프로젝트}`는 사용자가 Vercel 프로젝트를 만들 때 정함 — 정해지면 보고 · 검증에 실제 주소를 씀). https라 쿠키는 `Secure`(라이브러리 기본, `__Secure-` 접두) · `HttpOnly` · `SameSite=Lax`. 접속 주소는 이 하나뿐 — 배포마다 생기는 고유 주소(`{프로젝트}-{해시}.vercel.app`) · 프리뷰 주소는 Origin이 달라 로그인 403이고 Vercel 기본 보호(Standard Protection)로 막힘. 와일드카드(`*.vercel.app`) 신뢰 출처 · `allowedHosts` 안 씀 |
| Vercel 설정 | `ex12/vercel.json` `{ "regions": ["icn1"] }`(Supabase ap-northeast-2 서울과 같은 지역, D10 확정. Hobby는 1개 지역, 기본은 iad1 워싱턴). `package.json`에 `"engines": { "node": "24.x" }`(Vercel 기본도 24.x). 빌드 명령은 기존 `npm run build`(next build + tsc) 그대로, 설치는 lockfile로 `npm ci`(Vercel 자동). `maxDuration` 안 정함(기본 300초, API는 짧음) |
| 환경 변수(Vercel) | Production에만 3개: `DATABASE_URL`(Secret) · `BETTER_AUTH_SECRET`(Secret, 32자 이상, 로컬과 다른 배포용 새 값 — D10 확정) · `BETTER_AUTH_URL`(Config). `NEWSDATA_API_KEY` · `OPENAI_API_KEY` · `API_URL` · `API_PORT`는 등록 안 함(API가 안 씀). 바꾸면 다시 배포해야 반영됨. 값은 채팅 · 문서 · 로그에 남기지 않음 |
| 하루 실행 | GitHub Actions `.github/workflows/daily.yml` 그대로(ex12 제작 · 심사 중 안 고침). ex12 PASS 뒤 닫기에서 `working-directory` · `cache-dependency-path` 2줄만 `ex12`로(D10). 같은 Supabase DB를 쓰므로 배포 화면에 매일 새 날짜가 보임 |

- 공식 근거(2026-10-15 확인): [Hono Next.js](https://hono.dev/docs/getting-started/nextjs) · [Vercel Root Directory · 빌드 설정](https://vercel.com/docs/builds/configure-a-build) · [환경 변수](https://vercel.com/docs/environment-variables) · [함수 시간](https://vercel.com/docs/functions/configuring-functions/duration) · [함수 지역](https://vercel.com/docs/functions/configuring-functions/region) · [지역 목록(icn1 서울)](https://vercel.com/docs/regions) · [Node 버전](https://vercel.com/docs/functions/runtimes/node-js/node-js-versions) · [배포 보호](https://vercel.com/docs/deployment-protection) · [Better Auth 옵션(baseURL · trustedOrigins · 보안 쿠키)](https://better-auth.com/docs/reference/options) · [Supabase 연결(서버리스 = 6543 · 연결 1개)](https://supabase.com/docs/guides/database/connecting-to-postgres). 제작 때 다시 확인.

## 화면 변경
- 없음(`public/index.html` 그대로 — 이미 같은 출처 상대 경로 `/api/...`로 부름). 폰 폭은 기존 AC-05-2 레이아웃.

## 구현 순서
0. 선행 조건 확인: `reports/ex09_judge.md`가 PASS이거나 "예약 실행 확인 대기" 1줄만 남은 보류(사용자 2026-10-15 지시). 그 밖의 FAIL이면 멈춤.
1. `ex09/` → `ex12/` 복사(node_modules · .next · dist · .data · .vitest · tsconfig.tsbuildinfo · next-env.d.ts 제외), `ex12/.env`는 ex09 값을 비공개 복사(출력 금지). `scripts/ex12-ubuntu.sh`(ex09 래퍼에서 작업 폴더만 `ex12`). `bash scripts/ex12-ubuntu.sh npm ci --no-audit --no-fund`.
2. `server/db/client.ts`(선택 `max`) · `server/bff.ts`(`createBff` · `loadApp`) · `app/api/[...path]/route.ts` · `test/auth-proxy.test.ts`를 가짜 앱 주입 테스트로 바꿈(아래 표) · `vercel.json` · `package.json` engines · `.env.example` 주석.
3. 로컬 검증(아래 로컬 줄) → 보고에 "사용자가 할 일"을 적고 멈춤: ① main 푸시 ② Vercel 대시보드 › Add New › Project › GitHub `Jwavascript/capstone` 가져오기 › 프로젝트 이름(= `{프로젝트}`, 주소) 정하기 › Root Directory `ex12` · Framework Next.js ③ Production 환경 변수 3개(값은 직접 입력, `BETTER_AUTH_SECRET`은 새 값) ④ Deploy → 주소 전달 ⑤ PC · 폰으로 같은 계정 확인 후 결과 전달. Vercel CLI는 쓰지 않음(Termux 동작 미확인).
4. 주소를 받으면 배포 줄 실행(제작 한도 안) → `reports/ex12_build.md`.
마지막. `bash scripts/ex12-ubuntu.sh npm test` · `bash scripts/ex12-ubuntu.sh npm run build`. (ex12 PASS 뒤 닫기: 워크플로 2줄을 ex12로 — 하루 실행 행 참고)

## 검증 방법
- 실행 위치 · 안전 실행 = ex09 · ex11 방식 그대로(`ex12/.env`만, stdout + stderr를 먼저 캡처해 `DATABASE_URL`(전체 · 인코딩/디코딩 비밀번호) · 두 API 키 · `BETTER_AUTH_SECRET` · 이번 테스트 비밀번호 · 세션 토큰/쿠키가 있는지 true/false부터, false일 때만 출력. 응답 · `Set-Cookie` 원문은 출력하지 않고 상태 · bool · 건수만). 로컬 서버는 `npm run dev -- --hostname 127.0.0.1`(3000), 접속은 `http://localhost:3000`, 소유 서버만 종료.
- 호출 한도: newsdata · OpenAI 0회(제작 · 심사 모두). 배포 주소 요청 = 제작 · 심사 각각 최대 150(브라우저 자동 요청 포함), 로컬 3000 요청 각각 최대 100. 임시 계정은 역할마다 A · B 2개(`ex12-<uuid>@example.invalid`), 북마크는 실제 랭킹 Top 5 이슈 2개에만, 새 기사 · 이슈 · 요약 · 수집 기록 삽입 금지(D14). finally에서 이번 userId의 bookmarks → folders → session/account/user만 SQL로 지움, 기존 사용자 · 북마크 · 폴더 · 기사 · 이슈 내용 해시 전후 비교.

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 출발점 · 변경 범위 | `git status --short ex09/ ex11/` · `git diff --no-index --stat ex09 ex12`(산출물 · .env 제외) · `sha256sum ex09/server/{auth,app,ai,daily,steps}.ts ex12/server/{auth,app,ai,daily,steps}.ts` · `git diff --stat .github/` | ex09 · ex11 변경 없음. 차이는 `app/api/[...path]/route.ts` · `server/bff.ts` · `server/db/client.ts` · `test/auth-proxy.test.ts` · `vercel.json` · `package.json`(engines 한 줄) · `.env.example`(주석)뿐. 다섯 파일 해시 쌍마다 같음 · 워크플로 변경 0 |
| 로컬 회귀 · 빌드 | wrapper `npm test` · `npm run build` | ex09 기존 111개(바뀐 BFF 테스트 포함) 모두 통과 · 빌드/타입 성공 · 새 패키지 0(lockfile 의존성 차이 없음) · 빌드 로그에 DB 접속 · `API_START_FAILED` 없음 |
| BFF 같은 프로세스(테스트) | `createBff`에 가짜 앱 주입: ① 401 + 쿠키 2개(Expires 포함) 응답 ② 요청 Cookie · Origin · 본문 기록 ③ 두 번 요청 ④ 로더가 가짜 비밀값 든 오류를 던짐 → 다음 요청은 성공 ⑤ 앱이 던짐 ⑥ `/api/rankings` 200 | ① 상태 · 본문 · `getSetCookie()` 그대로 · no-store ② 셋 다 같음 ③ 로더 1번만 ④ 502 "API 서버에 연결할 수 없습니다" · no-store · 출력 `API_START_FAILED`만(가짜 비밀값 0) → 로더 다시 불려 200 ⑤ 502 같은 문구 ⑥ no-store 안 붙음(공개 경로) · fetch 호출 0 |
| 로컬 한 서버 | 안전 실행, `npm run api` 없이 `npm run dev`만 → Node fetch: `GET /` · `/api/rankings` · `?date=2000-01-02` · 내일 날짜 · `/api/bookmarks`(쿠키 없음) | 200 화면 · 200(`date` = SQL 최근 수집일) · 404 "해당 날짜의 랭킹 데이터가 없습니다" · 400 · 401 "로그인이 필요합니다" no-store |
| 앱 준비 실패(로컬) | 자식 env `DATABASE_URL=` 빈 값 · 따로 `BETTER_AUTH_SECRET=short`로 `npm run dev` → `GET /api/rankings` · `GET /api/auth/get-session` | 누출 false · 둘 다 502 같은 문구 · auth는 no-store · 서버 로그 `API_START_FAILED`만 · 인증 성공 0 |
| 비밀값 누출(빌드 · 저장소) | 빌드 후 `.next/static` · `.next/server` · `public/` · `vercel.json`에서 `.env` 값 검색(bool만) · `git check-ignore ex12/.env` · `git ls-files | grep -E '(^|/)\.env$'` | 모두 false · 무시됨 · 0건 |
| 배포 설정(사용자 → 확인) | 사용자가 대시보드 화면(Root Directory · Framework · Node · 지역 · 환경 변수 이름/환경 · Deployment Protection)을 캡처나 글로 전달. 에이전트: 배포 주소 `GET /` 응답 헤더 `x-vercel-id` | Root `ex12` · Next.js · 24.x · 환경 변수 3개 Production만(값은 보지 않음) · Standard Protection · `x-vercel-id`에 `icn1` |
| 배포 랭킹 · 날짜(AC-03-1 · 05-1 · 09-1~3) | 배포 주소 Node fetch `GET /api/rankings` · `?date={SQL로 본 직전 수집일}` · `?date=2000-01-02` · 내일 → 같은 요청을 로컬 `npm run api`(8787)에 | 200 · 200 · 404 같은 문구 · 400, 앞 둘 본문이 8787과 같음(같은 DB) · 섹션 6개 · `updatedAt` 있음 |
| 배포 가입 · 쿠키(AC-06-1 · 06-2) | 쿠키 jar(메모리)로 A 가입 → get-session → 로그아웃 → 틀린 비밀번호 로그인 → 정상 로그인 · 악성 Origin(`https://evil.example`) 로그인 · 배포 고유 주소(`…-{해시}.vercel.app`)로 `GET /` | 200 · 쿠키 `Secure` · `HttpOnly` · `SameSite=Lax` · `__Secure-` 접두 bool 모두 true · 세션 본인 · 로그아웃 뒤 null · 401 · 200 · 403 · 고유 주소는 Vercel 보호 화면(401 또는 로그인으로 이동, 앱 응답 아님) |
| 배포 계정별 북마크(AC-07-1 · 07-2 · 08-1 · 08-2 · 10-1 · 10-2) | A: 폴더 만들기 → Top 5 이슈 2개 저장 → 목록 · 폴더 count → 하나 해제 / B: 목록 · 폴더 · A 폴더로 POST | 201 · 201 · 최근 저장 순 · count 맞음 → 204 / B `[]` · `[]` · 404 "폴더를 찾을 수 없습니다". 개인 응답 모두 `Cache-Control: no-store` · `x-vercel-cache` ≠ HIT |
| 두 기기 같은 목록(AC-08-3) | ① 에이전트: 서로 격리된 Chromium 컨텍스트 2개(ex11 CDP 방식)로 배포 주소에서 같은 A 로그인 → 한쪽에서 저장 · 해제 → 다른 쪽 새로고침 ② 사용자: 자기 계정(사용자가 직접 가입 · 에이전트가 지우지 않음, 임시 계정 비밀번호는 전달하지 않음)으로 PC 브라우저에서 북마크 1개 저장 → 폰 브라우저 로그인 → 북마크 화면 | ① 두 컨텍스트 목록 · 폴더 칩 · 개수가 같음, B 전환 컨텍스트에 A 카드 0 ② 사용자가 전달한 결과: PC 브라우저와 폰의 북마크 목록 · 폴더 칩 · 개수가 같음(전달 내용을 보고에 그대로 적음, 아직 전달 전이면 SKIP · PASS 아님) |
| 배포 화면 회귀(AC-04-2 · 05-2 · 07-3 · 09-2) | 같은 브라우저: 비로그인 북마크 클릭 · 360px 폭 · 이전 날 → "오늘" · 원문 링크 | 로그인 안내 · 저장 요청 0 · 가로 스크롤 0 · 날짜 표시 바뀜 → 오늘 복귀 · 새 탭 언론사 주소 |
| 배포 로그 | 사용자가 Vercel › Logs에서 이번 검증 시간대 로그 전달(또는 캡처) | 비밀값 · SQL 인수 · 쿠키 없음, 오류는 안전 코드(`API_START_FAILED` · `API_INTERNAL_ERROR` · `AUTH_INTERNAL_ERROR`)만 |
| 정리 · 보존 | 이번 userId 행 정리 → SQL 건수 · 내용 해시 · 소유 프로세스 · 요청 카운터 | 임시 계정 · 세션 · 북마크 · 폴더 0 · 기존 해시 불변 · 뉴스/OpenAI 0 · 한도 안 · 남은 프로세스 0 |

### 앞 단계 검증표 보존
- ex09 · ex11 · ex10 검증표는 회귀 기준이다. ex09의 하루 실행 줄(키 없음 · DB 오류 · 잘못된 키 · 실제 하루 · 워크플로 · 예약 실행 · 품질 기록)은 코드 · 워크플로가 안 바뀌므로(`출발점 · 변경 범위` 줄) 다시 돌리지 않고 `로컬 회귀 · 빌드`로 대신한다. ex10 · ex11의 `BFF 쿠키 회귀`는 위 `BFF 같은 프로세스(테스트)`로, `Hono 꺼짐` 502는 `앱 준비 실패(로컬)`로 바뀐다. ex08 `저장소 나눔`의 DB 연결 파일 목록에 `server/bff.ts`를 더한다.

## 2026-10-15 KST — 연관기사 수 검색어 보완

> 이유: 배포 화면에서 섹션 1위가 3~4건, 2~5위 대부분 1건이다. 오늘 Actions 수동 실행(run 38683426817)에서 검색 36번 중 33번이 `n건 (묶음 n)`이었다(예: `김지용 AND "중수청장 인사청문회"` 3건, `"청주시 장성지구 A2블록" AND "공동주택 분양 승인"` 1건). 원인은 `server/ai.ts` METADATA_SYSTEM의 terms 지시("가장 구체적인 표현 하나" · "짧은 명칭이나 행동 구절")가 긴 구절을 만들고, `buildQuery`가 띄어쓰기 있는 말을 따옴표 구절로 감싸 제목 완전 일치만 남기 때문이다. 그래서 terms 지시만 짧은 핵심어로 고친다.
> 위치: ex12에서 고친 `server/ai.ts`를 ex09에 해시 일치로 복사(ex09 = 하루 실행 워크플로 작업 폴더, D10 "함께 고침"). ex08 · ex11은 기록 보존을 위해 그대로 둔다(D17 취지: 재검증 기록은 한 폴더에 남기고 다른 폴더에는 해시 일치로만 반영). D19 확정(2026-10-15, 사용자): ex12 수정 → ex09 해시 일치 복사, ex08 · ex11 보존(07e10b1e… 유지), 실제 검증은 오늘 로컬, 푸시는 10-16 예약 실행 확인 뒤.

### 이번 범위
- 바꿈: `ex12/server/ai.ts` METADATA_SYSTEM의 terms 지시 문장과 예시만 + `test/ai.test.ts` 지시문 문구 테스트 1개. 스키마 · terms 2×(1~3) 계약 · 모델 gpt-6-luna · effort(D18) · 배정 단계 지시 · 섹션/importance 지시 · 저장 · 순위(D16) · D13 산식 · 후보 6개는 그대로.
- `grouping.ts`(`buildQuery`)는 안 바꿈: 띄어쓰기 있는 말을 따옴표로 감싸야 qInTitle이 그 구절을 한 덩어리로 찾는다(풀면 낱말이 흩어져 뜻이 바뀜). 문제는 따옴표 처리가 아니라 AI가 긴 구절을 주는 것이고, terms가 1~2낱말이 되면 따옴표는 2낱말 명칭에만 붙는다. ex07 규칙 경로 · 기존 테스트도 불변 → 복사 대상은 ai.ts 하나.
- 하지 않음: D13 산식 · p 계산 변경, 후보 수 · 검색 수 변경, 워크플로 변경, 10-16 예약 실행 확인 전 푸시.

| 항목 | 규칙(METADATA_SYSTEM terms) |
|---|---|
| 그룹 1 | 제목에 흔히 쓰이는 짧은 핵심 명사 1개(인물 · 기관 · 대상, 예: `김지용` · `누리호` · `청주시`) |
| 그룹 2 | 사건 핵심어 1개(예: `인사청문회` · `발사` · `분양`) |
| 길이 | 각 말 1~2낱말. 따옴표가 필요한 긴 구절 · 수치(`40곳` · `A2블록`) · 지명 상세 · 수식어 금지 |
| 동의어 OR | 같은 대상 · 행동임이 확실할 때만 2~3개(지금 규칙 유지), 원인/결과 · 이어지는 행동은 넣지 않음 |
| 너무 넓음 | `AI` · `정부` · `대통령`처럼 흔한 말만으로 된 그룹 금지, 두 그룹 중 하나는 그 사건 고유 명사. 위험: 흔한 말끼리면 totalResults가 수천이 됨. D13 1쪽 검증 p는 1쪽 제목이 묶음 낱말을 공유하는 비율로 비례 축소만 하므로, 흔한 묶음 낱말이 1쪽에 겹치면 p가 높게 나와 부풀림을 못 막음 → 아래 상한 확인 · 사람 검토로 막음 |

### 순서
1. 오늘(10-15): ex12 지시 수정 · 테스트 → `sha256sum`으로 ex09에 복사(ex09 · ex12 ai.ts 같은 새 해시, ex08 · ex11은 07e10b1e… 그대로) → ex09 · ex12 각각 wrapper `npm test` · `npm run build`.
2. 오늘(10-15) 바로 실제 검증(아래 표, 로컬 실행). 남은 크레딧 106 기준이며 실행 직전 크레딧 확인으로 다시 확인. newsdata 상한 8을 넘지 않아 내일 예약 실행 몫(최대 54)을 남긴다. 로컬 실행이라 예약 실행 코드와 섞이지 않음.
3. 커밋은 사용자 요청 시. **푸시는 10-16 06:17 KST 예약 실행과 ex09 예약 실행 심사 확인 뒤**(그 전에 푸시하면 예약 실행이 새 코드를 써서 ex09 심사 근거가 섞임) → 통과했으면 사용자 확인 후 푸시 → 다음 예약 실행(10-17)에서 연관기사 수 분포(섹션별 1~5위 n · `n건 (묶음 n)` 비율 · 최대 n)를 `reports/ex09_daily.md`에 기록. Vercel 재배포는 화면 · API 동작 변화 없음.

### 검증 방법
- 안전 실행 = ex12 방식(`ex12/.env`만, stdout + stderr를 먼저 비밀값 true/false 검사 후 false일 때만 출력). DB는 읽기만(쓰기 0, 실제 기사 · 이슈 · 요약 · 북마크 해시 전후 같음). 응답 · 로그 원문은 문서에 붙이지 않고 검색어 · n · 묶음 크기 · p만 적는다.
- 호출 한도(제작 · 심사 각각): OpenAI 최대 2(organize 1회 = 배정 + 메타), newsdata 최대 8(검색 6 + 크레딧 확인 1~2). 실패 · 한도 초과면 중단, 재시도 없음. 기준은 실행 전 확정하고 결과를 본 뒤 완화하지 않는다.

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 변경 범위 · 해시 | `git diff --stat ex08 ex09 ex11 ex12` · `sha256sum ex0{8,9}/server/ai.ts ex1{1,2}/server/ai.ts ex09/server/grouping.ts ex12/server/grouping.ts` | ex09 · ex12는 `server/ai.ts` · `test/ai.test.ts`만, 두 ai.ts 해시 같음 · ex08 · ex11 = 07e10b1e… · grouping.ts 둘 다 f085810c… 불변 |
| 지시문 문구(테스트) | `test/ai.test.ts`: 메타 요청 instructions에 새 규칙(1~2낱말 · 긴 구절/수치 금지 · 흔한 말만 금지)이 있고 "가장 구체적인 표현 하나"가 없음 | 통과 |
| 로컬 회귀 · 빌드 | ex12 · ex09 각각 wrapper `npm test` · `npm run build` | 기존 테스트 모두 + 새 1개 통과 · 빌드 성공 · 새 패키지 0 |
| 실제 정리(10-15 로컬) | DB 읽기로 2026-10-15 기사 158건 입력을 만들어 `ex12/.data/`에 저장(Actions 입력 스냅샷은 저장소에 없음) → organize 1회 | OpenAI 2회 · 형식 통과 · terms 모두 1~2낱말 · 따옴표 붙는 말은 2낱말 명칭뿐 · DB 쓰기 0 |
| 실제 검색(10-15 로컬) | 섹션별 첫 후보 중 6개를 D13 그대로 qInTitle 검색(오늘 로그와 같은 사건이면 옛 검색어 · n과 나란히) | 확정 기준(D19): 6개 중 4개 이상 n > 묶음 크기 · 모든 n < 1,000 · 각 p 기록 · 검색어와 1쪽 제목이 같은 사건인지 사람 검토 OK |
| 다음 날 분포 | 푸시 뒤 첫 예약 실행 로그 · DB 읽기 | `n건 (묶음 n)` 비율이 33/36보다 낮음 · 최대 n < 1,000 · `reports/ex09_daily.md` 기록 |

### 2026-10-15 KST — 검색어 보완 2차 · 사진 접두 제외 (D19-2)

1차 로컬 실측 FAIL(첫 후보 6개 중 n>묶음 1개, 기준 4개; 오병합 0). 원인: 2낱말도 buildQuery가 따옴표 완전 일치로 만듦, 제목 표기 차이(美·CPI·금융사·AI해킹), 모델의 1~2낱말 지시 위반 8건. 사용자 승인으로 다음을 바꾼다. 1차 변경(ex12·ex09 ai.ts, 미커밋)을 바탕으로 이어서 고친다.

| 변경 | 규칙 |
|---|---|
| terms | 모든 말은 띄어쓰기 없는 **한 낱말**(buildQuery가 따옴표를 붙이지 않게). 그룹1 = 사건 주체/대상 고유 명사, 그룹2 = 사건 핵심 행동/대상 한 낱말. 각 그룹에 실제 기사 제목에 쓰인 표기 변형을 동의어 OR로 1~3개(예 ["미국","美"]·["CPI","소비자물가"]·["금융사","금융회사"]). 美·中·日 등 약칭 허용(기존 한자 약칭 금지 문장 삭제). 흔한 말만으로 된 그룹 금지는 유지. 스키마 2×(1~3)·buildQuery·D13 산식 그대로 |
| 코드 확인 | 메타데이터 응답의 말에 공백이 있으면 공백을 지운 한 낱말로 바꾸지 않고 그대로 두되(기존 동작), 검증에서 공백 포함 말 수를 기록 |
| 사진 접두 | `server/ai.ts` organize에서 제목(입력 줄의 `[섹션] ` 뒤)이 `[사진]`·`[포토]`·`[화보]`로 시작하는 기사는 AI 배정 입력에서 빼고 excluded에 넣음(번호 완전성 유지). 전부 해당하면 AI 호출 0. 테스트: 접두 기사 제외·나머지만 배정 요청·번호 완전성 |
| 복사 | 바뀐 ai.ts·테스트를 ex09에 해시 일치 복사, ex08·ex11 보존 |

검증: ex12·ex09 test/build → 같은 10-15 DB 입력(158건, 읽기만)으로 group(search 1, save 기록만) 1회 = OpenAI 2·newsdata 최대 8(직전·직후 크레딧 확인 포함, 직전 70 미만이면 중단). 통과 = 첫 후보 6개 중 4개 이상 n>묶음·모든 n<1,000·사람 검토(같은 사건) + 접두 사진 기사 전부 excluded. terms의 공백 포함 말 수는 기록만(참고, 판정 아님). 사후 기준 완화·재시도 없음.
