# ex12 제작 보고 — 배포(Vercel) 준비

- 날짜: 2026-10-15 (KST, 14:10~15:00) · 역할: 제작(agent_prompts/2_build_agent.md) · 실행: Claude Code, Android/Termux + proot Ubuntu, `bash scripts/ex12-ubuntu.sh …`
- 기준: `plan/PLAN_backend_ex12.md`, PLAN.md 5절 D10 · D17 · D18, AGENTS.md
- 선행 조건(구현 순서 0): `reports/ex09_judge.md` = "PASS 보류 — 예약 실행 확인 대기", OK11/FAIL0/SKIP1(예약 실행 1줄). 사용자 지시(2026-10-15)대로 시작. ex09는 건드리지 않음.
- 외부 호출: newsdata 0 · OpenAI 0. Vercel CLI · 배포 · Vercel/GitHub 설정 · 커밋 · 푸시 안 함.

## 1. 작업 범위

`ex09/`를 `ex12/`로 복사(node_modules · .next · dist · .data · .vitest · tsconfig.tsbuildinfo · next-env.d.ts · .env 제외, `ex12/.env`는 `cp`로 따로 비공개 복사 — `cmp` 동일, 내용 읽기 · 출력 0). `scripts/ex12-ubuntu.sh`는 ex09 래퍼에서 주석 2줄과 `--work-dir`만 `ex12`. `npm ci --no-audit --no-fund` 종료 0, esbuild 실제 변환 `const a = 1;` 확인, Node v24.18.0.

BFF를 "8787로 fetch 중계"에서 "같은 프로세스 `app.fetch(req)`"로 바꾸고, Vercel 설정(`vercel.json` icn1) · Node 24.x 고정 · 서버리스용 DB 연결 1개를 더함. API 경로 · 응답 · 쿠키 · 화면 · 인증 · 소유권 · AI · 수집/묶기/요약/하루 실행 코드는 그대로.

## 2. PLAN 항목별 반영 위치

| PLAN 항목 | 반영 위치 | 내용 |
|---|---|---|
| 요청 경로 | `ex12/app/api/[...path]/route.ts` | `runtime = 'nodejs'` · `dynamic = 'force-dynamic'`, `createBff(loadApp)`의 처리 함수를 `GET · POST · DELETE`로 내보냄. `API_URL` 읽기 제거 |
| 앱 준비 | `ex12/server/bff.ts` `loadApp()` | `DATABASE_URL` 확인 → `authOptions(process.env)` → `connectDb({ max: 1 })` → `createApp(dbStore(db), createAuth(db, process.env))`. 실패 시 만든 클라이언트만 `end()` 후 다시 던짐 |
| 한 번만 · 실패 시 재시도 | `ex12/server/bff.ts` `createBff(load)` | 첫 요청 때 만든 Promise를 모듈 범위에 저장해 재사용(동시 첫 요청도 1번). 실패하면 저장값을 지워 다음 요청이 다시 시도 |
| 계약 유지 | `createBff` | 요청은 그대로 `app.fetch(req)`(Cookie · Origin · 본문 그대로). 응답 상태 · 본문 · 헤더 복사, `Set-Cookie`는 `getSetCookie()`로 하나씩 다시 붙임(쉼표로 안 나눔). `/api/auth/*` · `/api/bookmarks*` · `/api/folders*`는 성공 · 실패 · 502 모두 `Cache-Control: no-store` |
| 502 · 로그 | `createBff` | 앱 준비 실패 · `app.fetch`가 던짐 → 502 `{ message: "API 서버에 연결할 수 없습니다" }`, `console.error('API_START_FAILED')` 한 줄만(오류 객체 · 스택 · env 출력 0) |
| DB(서버리스) | `ex12/server/db/client.ts` | `connectDb(options = {})`, `max`가 있을 때만 postgres 옵션에 넣음 → 인수 없는 기존 호출(index · collect · group · summarize)은 동작 불변. 주석의 연결 파일 목록에 `bff.ts` 추가 |
| Vercel 설정 | `ex12/vercel.json` | `{ "regions": ["icn1"] }` |
| Node 버전 | `ex12/package.json` | `"engines": { "node": "24.x" }` 한 줄. 의존성 · 스크립트 변경 0, lockfile `cmp` 동일 |
| 환경 변수 설명 | `ex12/.env.example` | 첫 주석 1줄 → 2줄(`API_PORT`는 `npm run api` 로컬 확인용, `API_URL`은 더 이상 안 씀 · 이름만 남김). 변수 이름 그대로 |
| BFF 테스트 | `ex12/test/auth-proxy.test.ts` | 가짜 앱 주입 6개(아래 4절). 기존 fetch 모킹 2개를 대체 |
| 로컬 개발 | 코드 변경 없음 | `npm run dev` 하나로 화면 + API. `npm run api`(8787)는 그대로 남김 |
| Better Auth · 화면 · 하루 실행 | 변경 없음 | `server/auth.ts` · `public/index.html` · `server/daily.ts` · `server/steps.ts` · `.github/workflows/daily.yml` 그대로 |

## 3. 지운 것

- `ex12/app/api/[...path]/route.ts`의 fetch 중계 코드(`API_URL` 읽기 · `fetch(API_URL + …)`) — PLAN 범위(같은 프로세스 호출로 교체).
- `ex12/test/auth-proxy.test.ts`의 기존 2개(8787 fetch 모킹) — PLAN 표대로 가짜 앱 주입 테스트로 교체.
- 그 밖의 삭제 없음. ex09 · ex11 변경 0.

## 4. 테스트 · 빌드

| 확인 | 명령 · 결과 | 판정 |
|---|---|---|
| 출발점 · 변경 범위 | `git status --short ex09/ ex11/ .github/` 빈 출력 · `git diff --stat .github/` 빈 출력. `diff -rq`(산출물 · .env 제외) 차이 = `.env.example` · `app/api/[...path]/route.ts` · `package.json` · `server/bff.ts`(새) · `server/db/client.ts` · `test/auth-proxy.test.ts` · `vercel.json`(새)뿐. `sha256sum` 쌍 일치: auth `a85e2345…` · app `81119158…` · ai `07e10b1e…` · daily `94eb6beb…` · steps `0d8c9e54…` | OK |
| 로컬 회귀(테스트) | `bash scripts/ex12-ubuntu.sh npm test` → **14파일 115/115 통과**(ex09 111 − 바뀐 BFF 2 + 새 BFF 6). 새 설치 직후 1·2번째 실행은 기존 `auth.test.ts` "진입점의 누락 설정…"이 `STARTUP_TEST_TIMEOUT`(15초)로 1개 실패 — `server/index.ts`(변경 없음) 기동이 23.8초 → 9.9초(ex09 같은 명령 3.6초)로 느린 새 node_modules 첫 변환 탓. 3번째 실행부터 115/115, 마지막 재실행도 115/115. ex09 제작 때와 같은 환경 요인 | OK(첫 실행 시간 초과 기록) |
| BFF 같은 프로세스(테스트) | ① 401 + 쿠키 2개(Expires 포함) → 상태 · 본문 · `getSetCookie()` 그대로 · no-store ② 요청 Cookie · Origin · 본문 · URL 그대로 · 전역 fetch 0 ③ 두 요청에 로더 1번 ④ 로더가 가짜 비밀값 든 오류 → 502 같은 문구 · no-store · 본문에 가짜 비밀값 없음 · `console.error` 호출 `[['API_START_FAILED']]`뿐 → 다음 요청 로더 다시 불려 200 ⑤ 앱이 던짐 → 502 같은 문구 · no-store ⑥ `/api/rankings` 200 · no-store 없음 · fetch 0. 추가: 공개 경로 502에는 no-store 없음 | OK |
| 빌드 | `bash scripts/ex12-ubuntu.sh npm run build` **종료 0**(2회): Next.js 16.4.0 Turbopack 컴파일 · TypeScript · `tsc --noEmit`. 경로 `ƒ /api/[...path]`(동적) · `○ /_not-found`. 빌드 로그에 `API_START_FAILED` · postgres · 연결 오류 0(빌드 중 DB 접속 없음). 새 패키지 0 | OK |
| 비밀값 누출(빌드 · 저장소) | `.next/static`(9) · `.next/server`(113) · `public/`(1) · `vercel.json` · 빌드 로그에서 `.env` 값(DB 전체 · 인코딩/디코딩 비밀번호 · 두 API 키 · BETTER_AUTH_SECRET) 검색 → 모두 false. `git check-ignore ex12/.env` 무시됨 · `git ls-files`의 `.env` 0건 | OK |
| vitest 추적 파일 | ex12는 아직 추적 전(`?? ex12/`), 저장소 다른 추적 파일 변경 0 | 되돌릴 것 없음 |

## 5. 로컬 검증(실제 DB · 안전 실행)

- 스크립트 `ex12/.data/verify-local.ts`(git 무시), 안전 실행 `ex12/.data/safe-run.mjs`(stdout+stderr를 모은 뒤 `.env` 비밀값 · 비밀번호 · 쿠키 · 세션 토큰 포함 여부를 bool로 먼저 검사, 덮어쓰기 인수가 `.env`보다 이김) · `ex12/.data/check-files.mjs`. 결과 `ex12/.data/build-local-verdict-{start,dev-webpack,dev-turbopack}.json`, 서버 로그도 같은 폴더(누출 검사 false 뒤에만 열람).
- 서버는 스크립트가 `detached`로 띄우고 프로세스 그룹째 종료. 끝난 뒤 `curl … http://127.0.0.1:3000/` = 000 · 8787 = 000, `ps -ef | grep -E "server/index.ts|next-server|next dev|next start|proot"` 0줄.
- 요청: 로컬 3000 누계 **60/100**(첫 dev 실패 2회 18 + start 21 + dev-webpack 21). 임시 계정 2개 생성(실행마다 A 1개, `ex12-<uuid>@example.invalid`) → 모두 정리. 북마크는 실제 랭킹 Top 5 카드 2개에만.
- 정리 · 보존: finally에서 이번 userId의 bookmarks → folders → session → account → user(+verification)만 지움. articles · issues · collect_runs · bookmarks · folders · user · session · account · verification 9개 테이블 건수 + 내용 md5가 전후 같음(`originalRowsPreserved: true`, 두 실행 모두). 현재 건수: 기사 443 · 이슈 235 · 수집 기록 72 · 북마크 0 · 폴더 0 · 회원 0. 기사 · 이슈 · 요약 삽입 0.

| 확인 | 결과 | 판정 |
|---|---|---|
| 환경 문제: `npm run dev`(Turbopack) | 첫 API 요청 컴파일에서 `OS file watch limit reached`(better-auth · next 내부 폴더 감시 실패) → `Module not found` 로 API 응답 실패. 이제 API 라우트가 better-auth · drizzle · hono 전체를 묶어 감시 파일 수가 늘어난 탓. proot에서 inotify 한도(`/proc/sys/fs/inotify/max_user_watches` 읽기 권한 없음)를 바꿀 수 없음. 제품 코드 문제 아님(같은 코드의 `next build` · `next start` 정상), 권한 · 설정 변경 안 함 | 환경 실패(기록) |
| 로컬 한 서버 — `next start`(빌드 결과, Vercel과 같은 프로덕션 모드) | `npm run api` 없이 3000 하나: `GET /` 200(HTML) · `/api/rankings` 200, `date` = SQL 최근 수집일 `2026-10-15` · 섹션 6 · `updatedAt` 있음 · no-store 없음 · `?date=2000-01-02` 404 "해당 날짜의 랭킹 데이터가 없습니다" · 내일 400 · `/api/bookmarks`(쿠키 없음) 401 "로그인이 필요합니다" no-store | OK |
| 로컬 한 서버 — `next dev --webpack`(`WATCHPACK_POLLING=true`, 이 기기용 대체) | 위와 같은 5개 모두 같은 결과 | OK(대체 실행) |
| 추가: 같은 프로세스 BFF로 인증 · 북마크(두 모드 모두) | 가입 200 · no-store · 쿠키 `HttpOnly` · `SameSite=Lax` true(로컬 http라 Secure · `__Secure-`는 배포 줄에서) · get-session 본인 · 악성 Origin 로그인 403 · 틀린 비밀번호 401 · 폴더 201 · 북마크 2개 201 · 목록 2 no-store · 폴더 count 2 · 해제 204 · 로그아웃 200 · 이후 세션 null | OK |
| 앱 준비 실패(로컬) | 자식 env `DATABASE_URL=` 빈 값 / 따로 `BETTER_AUTH_SECRET=short`(덮어쓰기가 `.env`를 이김 — `@next/env`는 이미 있는 값은 덮지 않음을 코드로 확인): `GET /api/rankings` · `GET /api/auth/get-session` 모두 502 "API 서버에 연결할 수 없습니다", auth는 no-store. 서버 로그의 앱 줄은 `API_START_FAILED` 2줄뿐(요청마다 재시도), 인증 성공 0. 누출 false | OK |
| 서버 로그 | 정상 실행의 앱 줄은 `AUTH_INTERNAL_ERROR` 1줄(악성 Origin 거절 — ex10 심사에 기록된 의도된 안전 로그). dev 로그의 `[?25h`는 터미널 커서 제어 문자. 스크립트의 엄격한 로그 검사 bool이 이 두 가지 때문에 false로 나와 verdict 파일의 `status`는 FAIL로 찍혔지만, 로그 원문을 누출 검사 뒤 직접 보고 다른 줄 없음을 확인 | OK(설명) |
| 비밀값 누출(실행) | 3개 모드 모두 stdout · stderr · 서버 로그 `leak:false` | OK |

## 6. 배포 줄 — [SKIP — 사용자 배포 대기]

PLAN 검증표의 `배포 설정` · `배포 랭킹 · 날짜` · `배포 가입 · 쿠키` · `배포 계정별 북마크` · `두 기기 같은 목록(AC-08-3)` · `배포 화면 회귀` · `배포 로그` 7줄은 배포 주소가 없어 실행하지 않음. PASS로 치지 않음.

### 사용자가 할 일(단계별)

1. **커밋 · 푸시**: `ex12/` · `scripts/ex12-ubuntu.sh` · 문서를 커밋해 `main`에 푸시(`ex12/.env`는 git 무시 확인됨).
2. **배포용 BETTER_AUTH_SECRET 만들기(사용자 터미널에서, 값은 아무 데도 붙여 넣지 말고 Vercel 입력란에만)**: 예) `openssl rand -base64 32` 또는 `node -e "console.log(require('crypto').randomBytes(32).toString('base64url'))"`. 32자 이상 · 로컬 `.env` 값과 다른 새 값(D10). 채팅 · 문서에 남기지 않음.
3. **프로젝트 가져오기**: Vercel 대시보드 › **Add New… › Project** › GitHub `Jwavascript/capstone` **Import** › 프로젝트 이름 정하기(= 주소 `https://{프로젝트}.vercel.app`) › **Root Directory** 옆 Edit → `ex12` › **Framework Preset** Next.js(자동 감지 확인) › Build/Install 명령은 기본값 그대로(`npm run build`, lockfile로 설치).
4. **환경 변수(Environment Variables, 같은 화면 또는 Settings › Environment Variables)** — 환경은 **Production만** 체크:
   - `DATABASE_URL` — 종류 **Secret** — 로컬과 같은 Supabase 풀러(transaction 6543) 연결 문자열
   - `BETTER_AUTH_SECRET` — 종류 **Secret** — 2단계에서 만든 새 값
   - `BETTER_AUTH_URL` — 종류 **Config** — `https://{프로젝트}.vercel.app`(끝 `/` 없이 origin만)
   - 등록하지 않음: `NEWSDATA_API_KEY` · `OPENAI_API_KEY` · `API_URL` · `API_PORT`
   - 주소를 아직 모르면 3단계에서 정한 이름으로 `BETTER_AUTH_URL`을 먼저 넣는다. Vercel이 이름 충돌로 다른 주소(예: 접미사 붙음)를 줬다면 5단계 뒤 고친다.
5. **Deploy** → 빌드 성공 후 프로젝트 화면의 **Domains**에 보이는 프로덕션 주소 확인.
6. **주소가 4단계와 다르면**: Settings › Environment Variables에서 `BETTER_AUTH_URL`을 실제 주소로 고침 → **Deployments › 최신 Production 배포 › ⋯ › Redeploy**(환경 변수는 새 배포에만 반영됨). 같으면 이 단계 생략.
7. **확인 · 전달**(값은 보내지 않음): Settings › Build and Deployment(Root Directory `ex12` · Framework Next.js · Node.js Version 24.x) · Settings › Functions(Function Region에 icn1 — vercel.json이 이김) · Settings › Environment Variables(이름 3개 · Production만) · Settings › Deployment Protection(Standard Protection)을 캡처나 글로, 그리고 **프로덕션 주소**를 전달.
8. **PC · 폰(AC-08-3)**: 본인 계정으로 PC 브라우저에서 가입(에이전트가 지우지 않는 계정) → 북마크 1개 저장 → 폰 브라우저에서 같은 계정 로그인 → 북마크 화면의 목록 · 폴더 칩 · 개수가 같은지 결과를 전달.
9. **로그**: 검증 시간대의 Vercel › 프로젝트 › **Logs** 캡처(비밀값 · 쿠키 · SQL 인수가 없는지, 오류는 `API_START_FAILED` · `API_INTERNAL_ERROR` · `AUTH_INTERNAL_ERROR`만인지).

### 배포 후 심사(또는 제작 이어서)가 실행할 절차

- 한도: 배포 주소 요청 역할마다 최대 150(브라우저 자동 요청 포함), 로컬 3000 최대 100, newsdata · OpenAI 0. 임시 계정 A · B(`ex12-<uuid>@example.invalid`), 북마크는 Top 5 카드 2개만. finally에서 이번 userId의 bookmarks → folders → session/account/user만 SQL 삭제, 9개 테이블 건수 · md5 전후 비교(`ex12/.data/verify-local.ts`의 `snapshot()` · 정리 블록을 재사용 가능 — `BASE`만 배포 주소로).
- `x-vercel-id`: `GET https://{프로젝트}.vercel.app/` 응답 헤더에 `icn1` 포함.
- 랭킹: 배포 `GET /api/rankings` · `?date={SQL로 본 직전 수집일}` · `?date=2000-01-02` · 내일 → 같은 요청을 로컬 `bash scripts/ex12-ubuntu.sh npm run api`(8787)에 보내 앞 둘 본문 비교(같은 DB). 섹션 6 · `updatedAt`.
- 가입 · 쿠키: 메모리 쿠키 jar로 A 가입 → get-session → 로그아웃 → 틀린 비밀번호 401 → 정상 로그인 200 · `Origin: https://evil.example` 로그인 403. `Set-Cookie`의 `Secure` · `HttpOnly` · `SameSite=Lax` · `__Secure-` 접두를 bool로만. 배포 고유 주소(`{프로젝트}-{해시}.vercel.app`, Deployments 화면에서 확인) `GET /` → Vercel 보호 화면(401 또는 vercel.com 로그인으로 이동, 앱 응답 아님).
- 북마크: A 폴더 → Top 5 이슈 2개 저장(201 · 201) → 목록(최근 저장 순) · 폴더 count → 하나 해제 204 / B 목록 `[]` · 폴더 `[]` · A 폴더로 POST 404 "폴더를 찾을 수 없습니다". 개인 응답 모두 `Cache-Control: no-store` · `x-vercel-cache` ≠ HIT.
- 두 기기: ex11 CDP 방식(`ex11/.data/verify-ownership.ts`의 `page()` · `loginUi()`)으로 격리 Chromium 컨텍스트 2개, 같은 A 로그인 → 한쪽 저장 · 해제 → 다른 쪽 새로고침 후 목록 · 폴더 칩 · 개수 같음, B 전환 컨텍스트에 A 카드 0. 사용자 PC · 폰 결과는 전달 내용을 그대로 적음(전달 전이면 SKIP).
- 화면 회귀: 비로그인 북마크 클릭 → 로그인 안내 · 저장 요청 0, 360px 가로 스크롤 0, 이전 날 → "오늘" 복귀, 원문 링크 새 탭.

## 7. 필요했지만 없던 키 · 계정

- Vercel 프로젝트 · 프로덕션 주소 · Vercel 환경 변수 3개 · 배포용 `BETTER_AUTH_SECRET` — 사용자 몫(위 6절). 그 때문에 배포 줄 7개 SKIP.
- 로컬 실행에 필요한 `ex12/.env` 값(DB · BETTER_AUTH)은 ex09에서 복사해 충분. 지어낸 값 없음.

## 8. 공식 문서 확인(2026-10-15)

- [Hono Next.js](https://hono.dev/docs/getting-started/nextjs): 현재 예시는 `@hono/vercel`의 `handle(app)`(새 패키지) → PLAN대로 `app.fetch` 직접 호출, Node 런타임 필요.
- [Vercel 함수 지역](https://vercel.com/docs/functions/configuring-functions/region): `vercel.json` `regions`, 기본 iad1, Hobby는 1개 지역.
- [Vercel 환경 변수](https://vercel.com/docs/environment-variables): Config / Secret 종류, Production · Preview · Development 선택, 바꾸면 새 배포에만 반영.
- [Node 버전](https://vercel.com/docs/functions/runtimes/node-js/node-js-versions): 24.x 기본 · 22.x, `engines.node`가 프로젝트 설정보다 우선.
- [배포 보호](https://vercel.com/docs/deployment-protection): Standard Protection은 프로덕션 도메인 말고 전부 보호(프로덕션의 생성 배포 주소도 보호).
- [빌드 설정 · Root Directory](https://vercel.com/docs/builds/configure-a-build): Root Directory 밖 파일은 접근 불가(ex12는 자체 완결), Next.js는 `build` 스크립트 사용.

## 9. 추가로 보인 점

1. **이 기기의 `npm run dev`(Turbopack)가 감시 한도로 API를 못 띄움** — ex12에서 새로 생긴 환경 문제(라우트가 서버 코드 전체를 묶음). Vercel 배포 · `next build` · `next start`에는 영향 없음. 이 기기에서 개발 서버가 필요하면 `bash scripts/ex12-ubuntu.sh /usr/bin/env WATCHPACK_POLLING=true npm run dev -- --webpack --hostname 127.0.0.1` 또는 `npm run build` 뒤 `npx next start -p 3000 --hostname 127.0.0.1`(package.json에 `start` 스크립트가 없어 npx로). 스크립트 추가는 PLAN 밖이라 안 함 — 원하면 확인 후.
2. **첫 `npm test` 기동 테스트 시간 초과** 재현(ex09와 같음). 새 설치 직후 1~2회만.
3. **package.json `name`이 `issuerank-ex11` 그대로**(ex09부터). Vercel 프로젝트 이름과 무관해 그대로 둠.
4. **Vercel이 `main` 푸시마다 ex12를 다시 배포**: Root Directory 밖(ex09 등)만 바뀐 커밋도 빌드됨. 원하면 Settings › Build and Deployment › Root Directory의 "Skip deployment"(영향 없는 커밋 건너뛰기)를 사용자가 켤 수 있음(선택, PLAN 밖).
5. **서버리스 연결 1개**: Fluid compute에서 한 인스턴스가 동시 요청을 받으면 같은 연결 1개로 차례 처리. API 쿼리가 짧아 문제는 예상 안 되지만, 배포 줄에서 응답 시간을 함께 본다.
6. 2026-10-16 ex09 예약 실행에서 문제가 나오면 ex09 · ex12를 함께 고친다(사용자 결정). ex12 PASS 뒤 닫기에서 `.github/workflows/daily.yml`의 `working-directory` · `cache-dependency-path` 2줄을 `ex12`로.

## 10. 2026-10-15 KST — 연관기사 수 검색어 보완(D19) 제작 · 실제 검증

> 근거: `plan/PLAN_backend_ex12.md` 마지막 절 「연관기사 수 검색어 보완」, PLAN.md D19 · D13 · D18. 사용자 지시 "검색어 보완 절대로 ex12를 고치고 ex09에 반영, 오늘 실제 검증까지". 제작 역할(Claude Code 세션). 커밋 · 푸시 안 함(푸시는 10-16 예약 실행 확인 뒤 사용자 확인).

**판정: 실제 검색 줄 FAIL**(첫 후보 6개 중 n > 묶음 크기 **1개** < 기준 4개). 나머지 줄(변경 범위 · 해시 · 테스트 · 빌드 · DB 쓰기 0 · n < 1,000 · 사람 검토)은 OK. 기준은 실행 전 확정한 그대로이며 완화하지 않았고, 재시도 · 추가 호출 없음.

### 10.1 바뀐 것

| 파일 | 내용 |
|---|---|
| `ex12/server/ai.ts` | METADATA_SYSTEM 「4. 검색어와 중요도」의 terms 문장 · 예시만 교체(6줄 → 6줄): 그룹1 = 제목에 흔한 짧은 핵심 명사(인물 · 기관 · 대상) 1개, 그룹2 = 사건 핵심어 1개 · 각 말 1~2낱말 · 따옴표가 필요한 긴 구절 · 수치 · 지명 상세 · 수식어 금지 · 동의어 OR은 확실할 때만 2~3개(원인/결과 금지 유지) · 흔한 말만으로 된 그룹끼리 금지 · 두 그룹 중 하나는 사건 고유 명사 · 한자 약칭/검색 문법 금지 유지. importance 문장 · 섹션 지시 · 배정 지시 · 스키마 · 모델 · effort · 출력 한도는 그대로 |
| `ex12/test/ai.test.ts` | 테스트 1개 추가 「메타데이터 지시문은 terms를 짧은 핵심어로 요구한다(D19)」: 두 번째(메타) 요청 instructions에 `짧은 핵심 명사` · `사건 핵심어` · `1~2낱말` · `긴 구절, 수치` · `흔한 말만으로 된 그룹`이 있고 `가장 구체적인 표현 하나`가 없음 |
| `ex09/server/ai.ts` · `ex09/test/ai.test.ts` | ex12 파일을 그대로 복사(해시 일치) |

- 지시문 예시는 오늘 검증 사건(김지용 · 청주시 분양 등)이 프롬프트에 들어가 검증이 오염되지 않게 일반 예시(`누리호/발사` · `한국은행/기준금리` · `부산시/청약`, `○○청장 후보자 인사청문회 → 인사청문회` 등)로 썼다. PLAN 표의 예시 낱말은 규칙 설명용으로 보았다.
- 지운 것: 옛 terms 지시 문장(“가장 구체적인 표현 하나”, “짧은 명칭이나 행동 구절”, “말은 한 낱말이나 띄어쓰기 있는 구절”)은 같은 위치에서 새 문장으로 바뀜. 코드 · 테스트 삭제 없음.
- 필요했지만 없던 키/계정: 없음(ex12/.env 사용, 값 출력 없음).

해시(`sha256sum`):

| 파일 | 해시 |
|---|---|
| ex09 · ex12 `server/ai.ts` | `d4a396ab42443ee0d9c47e11664262936ccfb97224cfa9544032a0084930b843`(같음) |
| ex08 · ex11 `server/ai.ts` | `07e10b1e7746732594bf904c6b17f74084f526d9486e9334336d3bca3a159134`(불변) |
| ex09 · ex12 `test/ai.test.ts` | `ea3477abee1c79585e8bd3dc47bb6a1cb925804866e32f60a3f8c1e712101468`(같음) |
| ex09 · ex12 `server/grouping.ts` | `f085810c9e5ab705425424ab5e635cfa43c8de60768d607e07d64bcc4d7bdb1d`(불변) |

`git diff --stat ex08 ex09 ex11 ex12` → ex09 · ex12 각각 `server/ai.ts`(+6 −5) · `test/ai.test.ts`(+14)만. ex08 · ex11 변경 0.

### 10.2 테스트 · 빌드

| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 **116/116** 통과(기존 115 + 새 1). 첫 실행 통과(auth 기동 테스트 13.0초, 15초 제한 안) |
| `bash scripts/ex09-ubuntu.sh npm test` | 14파일 **112/112** 통과(기존 111 + 새 1) |
| `bash scripts/ex12-ubuntu.sh npm run build` · `bash scripts/ex09-ubuntu.sh npm run build` | 둘 다 성공(라우트 `/_not-found` · `/api/[...path]`) |

vitest가 git 추적 파일을 바꾼 것 없음(`git status` 전후 같음). 새 패키지 0.

### 10.3 실제 검증(로컬, 2026-10-15 15:55:08~15:56:30 KST)

방법: `ex12/.data/verify-terms.ts`(gitignore, `node .data/safe-run.mjs -- node_modules/.bin/tsx .data/verify-terms.ts`로 실행, 출력 `leak:false`)가 제품 `group()`을 그대로 부름 — `search: 1`(섹션마다 첫 후보 1개 = D13 후보 순서 `draftOrder`), `waitMs: 1000`, `organize = aiClient(key).organize`, 검색 · 1쪽 검증 · `buildQuery` · `relatedCount`는 제품 코드. 저장소는 `groupStore(db).load`를 **읽기 전용 트랜잭션**(`accessMode: 'read only'`)으로 감싸고 `save`는 기록만 하는 함수로 바꿈(제품 저장 함수 · CLI 호출 없음). OpenAI는 전역 fetch 가로채기로 횟수(최대 2) · 원본 응답 기록, newsdata는 횟수(최대 8) 제한. p는 1쪽 제목으로 같은 낱말 규칙(`words` · `same`)으로 다시 계산해 n과 일치 확인(6개 모두 일치). 먼저 가짜 응답 모드(`VERIFY_MOCK=1`, 외부 호출 0, DB 읽기만)로 후처리를 확인하고 그 출력 폴더는 지움.

- 입력: DB 2026-10-15 기사 **158건**(북마크 섹션 없음 → 건너뛴 기사 0) → `inputLines` 158줄, sha256 `7526497bea54c3989a8030f03c1407ab0a06ac23e41c998f512a9f3a49547ed5`.
- AI 정리: `ok · 묶음 127 · 제외 17 · 빠진 기사 0 · 고침 모두 0`(형식 통과).
- DB: 9테이블(articles 443 · issues 235 · collect_runs 72 · bookmarks 0 · folders 0 · user/session/account/verification 0) 건수 · md5 해시 실행 전후 **같음**(쓰기 0).

| 섹션 | 대표 제목 | 새 검색어(terms) | n | 묶음 | totalResults · 1쪽 · p | 옛 검색어(run 38683426817) | 옛 n | 사람 검토 · 판단 |
|---|---|---|---|---|---|---|---|---|
| pol | 김지용 청문회서 부각된 여권 분열… | `김지용 AND 인사청문회` | **29** | 3 | 41 · 10 · 0.7 | `김지용 AND "중수청장 인사청문회"` | 3 | 1쪽 10건 모두 김지용 중수청장 후보자 청문회 → 같은 사건 OK · **n > 묶음** |
| eco | 9월 CPI 보고서에 대한 전문가 및 애널리스트 반응 종합 | `"미국 소비자물가지수" AND 물가` | 3 | 3 | 0 · 0 · 0 | `"미국 9월 CPI" AND "물가 지표 발표"` | 3 | 검색어는 같은 사건(미국 9월 CPI)을 가리킴 · 결과 0(묶음 제목은 `美` · `CPI` 표기) → n = 묶음 |
| soc | 흉기 난동범 “병원 무좀 치료 불만…” | `"인천 흉기 난동" AND 구속영장` | 3 | 3 | 3 · 3 · 1 | `"인천 상가 흉기 난동" AND "구속영장 신청"` | 3 | 1쪽 3건 모두 같은 사건 OK · 3낱말 구절(지시 위반) → n = 묶음 |
| cul | [사진]트리플에스 김채연, 시구를 마치고 | `"트리플에스 김채연" AND 시구` | 2 | 2 | 6 · 6 · 0.33 | (같은 사건 없음 — 옛 첫 후보는 `흥이·나유 AND "꿈돌이 협업 굿즈"` 1건) | – | 1쪽 6건 모두 김채연 WC 1차전 시구 → 같은 사건 OK · p가 낮아(묶음 제목 낱말 `사진` · `승리` 등 공유 2/6) n = 묶음 |
| wor | 트럼프 “그린란드서 대규모 석유 탐사 예정”…그린란드 반박 | `그린란드 AND "석유 탐사"` | 4 | 4 | 1 · 1 · 1 | `트럼프 AND "그린란드 석유 탐사"` | 4 | 1쪽 1건 = 같은 기사 OK · 다른 제목은 `석유·가스 탐사` · `석유 등 탐사` 표기 → n = 묶음 |
| it | AI해킹, 금융사 16곳서 개인정보 10만건 유출 | `금융회사 AND "AI 해킹"` | 1 | 1 | 0 · 0 · 0 | `"국내 금융회사" AND "AI 해킹 개인정보 유출"` | 1 | 검색어는 같은 사건을 가리킴 · 결과 0(제목 표기 `금융사` · `AI해킹`) → n = 묶음 |

- 확정 기준(D19) 대조: n > 묶음 **1/6**(기준 4 이상 → **FAIL**) · 모든 n < 1,000 OK(최대 29) · p 6개 기록 OK · 사람 검토: 오병합 · 다른 사건 검색어 0 → OK.
- terms 지시 준수(전체 127이슈 · 254말): 3낱말 이상 **8**(`인천 흉기 난동` · `다사랑 어울림 축제` · `제미나이 4 아르곤` 등, 지시 위반) · 2낱말(따옴표 구절이 됨) 85 · 숫자 포함 6(`10월 15일` · `7세대 HBM` · `레오 14세` 등) · `·` 붙인 말 2 · 동의어 OR 0. PLAN 기대 “terms 모두 1~2낱말”도 8개로 미충족.
- 옛 실행과 비교: 같은 사건 5개 중 n이 늘어난 것은 pol 1개(3 → 29), 나머지 4개는 그대로.

원인 관찰(고치지 않음):
1. **2낱말 말도 따옴표 완전 일치**가 됨 — `"석유 탐사"`는 `석유·가스 탐사`, `"AI 해킹"`은 `AI해킹`, `"인천 흉기 난동"`은 `인천 상가서 … 흉기`를 못 찾음. 지시가 2낱말을 허용하는 한 같은 문제가 남음.
2. **표기 차이**: `금융회사` vs 제목 `금융사`, `미국` vs 제목 `美`(기존 지시 “한자 약칭 대신 한글”과 D13 기록 “한자 약칭 제목은 한글 검색에 안 잡힘”이 충돌).
3. 모델이 1~2낱말 제한을 8번 어김(gpt-6-luna 메타 단계 effort low, 추론 토큰 35).
4. 일부는 실제로 48시간 newsdata 표본이 작음(wor 그린란드 totalResults 1). 무료 플랜 표본 한계(R2)와 겹침.
5. cul 첫 후보가 `[사진]` 기사 2건 묶음(배정 단계 사진 제외 누락, ex09 품질 기록과 같은 종류, 이번 범위 밖).

다음 방식은 사용자 결정 필요(이번에 하지 않음): ① 그룹2는 1낱말만, 2낱말은 그룹1의 고유 명칭에만 허용 ② “제목에 실제로 쓰인 표기 그대로”로 바꾸고 한자 약칭 규칙 재검토 ③ `buildQuery` 따옴표 처리 변경(PLAN이 제외함) ④ 이번 수정을 유지/되돌릴지. 재검증은 다음 크레딧(제작 · 심사 각각 newsdata 8 · OpenAI 2 한도).

### 10.4 호출 · 크레딧 · 비용

| 항목 | 값 |
|---|---|
| newsdata | **8/8**: 크레딧 확인 직전 1(status 200 · 남은 **105**, PLAN 기준 106과 차이 1 — 이번 세션 전 사용) + 검색 6(group 마지막 remaining 99) + 크레딧 확인 직후 1(status 200 · 남은 **98** = 105 − 6 − 1, 차이 0). 70 이상이라 실행 · 내일 예약 실행 몫(최대 54) 남음 |
| OpenAI | **2/2**(organize 1회 = 배정 + 메타). 배정: 입력 17,039 · 출력 2,985(추론 2,022) / 메타: 입력 22,720 · 출력 3,514(추론 35). 합 입력 39,759 · 출력 6,499 → 약 0.007달러(D18 단가 0.10/0.50달러, 캐시 쓰기 토큰 단가는 미확인) |
| 재시도 | 0 |
| 서버 | 띄우지 않음 |

### 10.5 증거(`ex12/.data/terms-20261015/`, 덮어쓰기 금지 `wx`로 저장, `check-files.mjs` leak=false)

| 파일 | 내용 |
|---|---|
| `input-lines.txt` | organize 입력 158줄 |
| `openai-responses.json` | OpenAI 원본 응답 2개(usage 포함) |
| `group-result.json` | group 반환 · 저장 대신 기록한 행 · organize 결과 · 호출 수 · 크레딧 · DB 불변 |
| `newsdata-page1.json` | 검색 6개의 검색어 · status · totalResults · 1쪽 제목 |
| `result.json` | 첫 후보 표 · terms 통계 · 자동 판정 · 사용량 · DB 전후 해시 · 전체 이슈 terms |
| `old-run-candidates.txt` | run 38683426817 후보 줄 37줄(`gh run view --log`, 누출 검사 false 뒤 저장) |

전체 해시: group-result `6b1d47982e33d5103619fb24d36f8ddf6ffba892197922ff048275d5702c444c` · input-lines `7526497bea54c3989a8030f03c1407ab0a06ac23e41c998f512a9f3a49547ed5` · newsdata-page1 `590e7db96a1879543af04a3de6b791f2bc2e6853ff1d78d5b43f6eaaa7daf58c` · old-run-candidates `4406b44be3d767c11ed0df2e9b94c1ee1b1386a4b758cc0a17921b6b5dd04e37` · openai-responses `3f1fee62b90b1524f3d4431dc1d1097226630bfe57bf5f08f40071e80a997d96` · result `c4fca7a6ff444e8772cfd9f28547de0dfc07ba9ca3827eebdecd210ff231c8e0`. 실행 스크립트 `ex12/.data/verify-terms.ts`도 leak=false.

### 10.6 추가로 보인 점
1. 실제 검색 FAIL이므로 이번 ai.ts 변경(ex09 · ex12)은 커밋 · 푸시하지 않은 채 작업 트리에만 있다. 10-16 예약 실행은 푸시된 옛 코드로 돈다(ex09 심사 근거와 섞이지 않음).
2. 옛 로그에도 `흥이·나유` · `삼성전자·SK하이닉스`처럼 `·`로 두 이름을 한 말에 붙인 사례가 있음(이번 결과 2개). OR로 나누는 지시는 이번 범위에 없음.

## 11. 2026-10-15 KST — 검색어 보완 2차 · 사진 접두 제외(D19-2) 제작 · 실제 검증

범위: `plan/PLAN_backend_ex12.md` 「검색어 보완 2차 · 사진 접두 제외 (D19-2)」 · PLAN.md D19 · D19-2. 1차 변경(미커밋, ai.ts d4a396ab…)을 바탕으로 이어서 고침. 커밋 · 푸시 안 함(10-16 예약 실행 확인 뒤 사용자 확인).

### 11.1 바뀐 것
- `ex12/server/ai.ts`
  - METADATA_SYSTEM terms 지시(4절 검색어 문단만): 모든 말은 **띄어쓰기 없는 한 낱말**, 그룹1 = 사건 주체/대상 고유 명사, 그룹2 = 핵심 행동/대상 한 낱말, 각 그룹에 실제 제목에 쓰인 **표기 변형 동의어 1~3개**(예 `["미국","美"]` · `["중국","中"]` · `["일본","日"]` · `["반도체","칩"]`), 美·中·日 약칭 허용(기존 "한자 약칭(美·中·日) 대신 한글" 문장 삭제), 수치 · 지명 상세 · 수식어 · 가운뎃점 붙인 말 금지, 흔한 말만으로 된 그룹 금지 유지, 원인/결과 · 다른 사건 나열 금지 유지. 1차의 1~2낱말 문장과 `○○청장 후보자 인사청문회` 예시(오늘 검증 사건과 가까움)는 지우고 일반 예시(누리호 · 한국은행 · 부산시 · ○○구)만 둠.
  - organize: `PHOTO_PREFIX = /^\S+ \[[a-z]+\] \[(사진|포토|화보)\]/`로 제목이 사진 접두로 시작하는 줄을 배정 입력 · 배정 스키마 · 후보 힌트(candidateGroups는 남은 줄로 계산) · 메타 입력에서 빼고 excluded 앞에 넣음. 번호 검사(중복 · 형식)는 전체 줄로 그대로, 남은 줄이 0이면 AI 호출 0으로 `{ issues: [], excluded: 사진 번호 }`. 대표 번호 검사도 남은 번호 기준.
  - 그대로: 스키마 · 모델 gpt-6-luna · effort(배정 medium · 메타 low) · ASSIGN_SYSTEM · 섹션/importance 지시 · `grouping.ts`(buildQuery · D13 · checkOrganize) · 저장 · 순위.
- `ex12/test/ai.test.ts`: 1차 문구 테스트를 새 문구로 고침(한 낱말 · 고유 명사 · 표기 변형 · `["미국","美"]` · 약칭 허용 · 흔한 말 금지 있음, "한자 약칭(美·中·日) 대신 한글" · "1~2낱말" · "가장 구체적인 표현 하나" 없음) + 새 2개: 사진 접두 3건(`[사진]` · `[포토]` · `[화보]`, 중간 `[사진]`은 제외 안 됨) → 배정 요청 input · 스키마 · 메타 input에 그 번호 없음 · excluded = 사진 3건 · 묶음+제외 = 전체 번호 / 전부 사진이면 fetch 0회.
- ex09: 두 파일을 복사.

| 파일 | SHA-256 |
|---|---|
| ex09 · ex12 `server/ai.ts` | `8c41162a740254dd73cc12565d15e4e075f58a91c9e631fc277caff972150782`(같음) |
| ex09 · ex12 `test/ai.test.ts` | `8b939bbf944512d1b01210ec06613e1fd0f7a735fe2ea5c02504ff8922e4ec54`(같음) |
| ex08 · ex11 `server/ai.ts` | `07e10b1e7746732594bf904c6b17f74084f526d9486e9334336d3bca3a159134`(불변) |
| ex09 · ex12 `server/grouping.ts` | `f085810c9e5ab705425424ab5e635cfa43c8de60768d607e07d64bcc4d7bdb1d`(불변) |

### 11.2 테스트 · 빌드
| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 **118/118**(1차 116 + 새 2) |
| `bash scripts/ex09-ubuntu.sh npm test` | 14파일 **114/114**(1차 112 + 새 2) |
| `bash scripts/ex12-ubuntu.sh npm run build` · `bash scripts/ex09-ubuntu.sh npm run build` | 둘 다 성공(`/_not-found` · `/api/[...path]`) |

vitest가 추적 파일을 바꾼 것 없음(`git status --short` 전후 같음). 새 패키지 0.

### 11.3 실제 검증(로컬, 2026-10-15 16:16:48~16:17:58 KST)

방법: 1차 `verify-terms.ts`를 복사한 `ex12/.data/verify-terms2.ts`(출력 폴더 `terms2-20261015` · 공백 포함 말 · 동의어 수 · 사진 접두 확인 · OpenAI 요청 input 기록만 추가). 제품 `group()` 그대로(`search: 1`, load 읽기 전용 트랜잭션, save 기록만), OpenAI 최대 2 · newsdata 최대 8 가로채기, 재시도 없음. 먼저 가짜 응답 모드(외부 호출 0, DB 읽기만)로 후처리 확인 후 그 폴더 삭제. 실행 출력 `leak:false`.

- 입력: DB 10-15 기사 158건 → 158줄, sha256 `7526497b…`로 **1차 입력과 같음**.
- 사진 접두 5건(a098 · a099 `[사진]트리플에스 김채연…`, a109 · a110 `[사진]…트와이스 미나…`, a144 `[사진] 버넘 英 총리의 약속…`): **모두 excluded**, 배정 요청 input · 메타 input에 번호 없음, 배정 입력 153줄.
- AI 정리: `ok · 묶음 134 · 제외 10(사진 접두 5 + AI 제외 5: 레서판다 · 빌리 츠키 · 변우석 장면 사진, 부음, 실용한자) · 빠진 0 · 고침 모두 0`.
- DB 9테이블 건수 · md5 전후 **같음**(쓰기 0, articles 443 · issues 235 · collect_runs 72 · 나머지 0).

| 섹션 | 대표 제목 | 새 검색어 | n | 묶음 | totalResults · 1쪽 · p | 1차 검색어 | 1차 n | 옛(Actions) n | 판단 |
|---|---|---|---|---|---|---|---|---|---|
| pol | 김지용 청문회서 부각된 여권 분열… | `김지용 AND 청문회` | **35** | 3 | 44 · 10 · 0.8 | `김지용 AND 인사청문회` | 29 | 3 | 1쪽 10건 모두 김지용 중수청장 청문회 → OK · n > 묶음 |
| eco | 9월 CPI 보고서에 대한 전문가 및 애널리스트 반응 종합 | `(미국 OR 美) AND (소비자물가지수 OR CPI)` | **23** | 3 | 26 · 10 · 0.9 | `"미국 소비자물가지수" AND 물가` | 3 | 3 | 1쪽 10건 모두 美 9월 CPI 3.7% → OK · n > 묶음 |
| soc | 흉기 난동범 "병원 무좀 치료 불만…" | `인천 AND 흉기난동` | **58** | 3 | 115 · 10 · 0.5 | `"인천 흉기 난동" AND 구속영장` | 3 | 3 | 1쪽 10건 모두 인천 구월동 상가 흉기난동(구속영장 · 진술 · 신상공개 + 후속 반응: BJ 희화화 사과 · 모방범죄 대비) → 같은 사건 OK · n > 묶음 |
| cul | '흥이·나유'와 '꿈돌이' 만났다…협업 굿즈 첫선 | `(흥이 OR 나유 OR 꿈돌이) AND 굿즈` | 1 | 1 | 1 · 1 · 1 | (1차 첫 후보는 `[사진]트리플에스` 2건 → 이번엔 제외됨) | – | 1(`흥이·나유 AND "꿈돌이 협업 굿즈"`) | 1쪽 1건 = 같은 기사 OK · n = 묶음(표본 1) |
| wor | 트럼프 "그린란드서 대규모 석유 탐사 예정"…그린란드 반박 | `그린란드 AND 탐사` | **6** | 4 | 6 · 6 · 1 | `그린란드 AND "석유 탐사"` | 4 | 4 | 1쪽 6건 모두 같은 사건(`석유·가스 탐사` 표기도 잡힘) → OK · n > 묶음 |
| it | AI해킹, 금융사 16곳서 개인정보 10만건 유출 | `금융사 AND 해킹` | **20** | 1 | 20 · 10 · 1 | `금융회사 AND "AI 해킹"` | 1 | 1 | 1쪽 10건 모두 금융사 16곳 해킹 · 10만건 유출 → OK · n > 묶음 |

옛(Actions) n은 run 38683426817(1차 10.3 표와 같은 값). 첫 후보 묶음 구성원(pol 3 · eco 3 · soc 3 · wor 4)도 모두 같은 사건 → 오병합 0. p는 1쪽 제목으로 다시 계산해 6개 모두 n과 일치.

- **판정(D19 · D19-2, 실행 전 기준 그대로): PASS** — n > 묶음 **5/6**(기준 4 이상) · 모든 n < 1,000(최대 58) · p 6개 기록 · 사람 검토 OK(오병합 0, 검색어 모두 그 사건) · 접두 사진 5건 전부 excluded.
- 기록만(판정 아님): 전체 134이슈 277말 중 공백 포함 **5**(`Beyond Air` · `State Street` · `Tata Technologies` · `폐 수술` · `GEN 레스토랑 그룹` — 영문 고유 명칭 4 · 지시 위반 1), 숫자 포함 2(`레오14세` · `오우라링5`), 가운뎃점 0, 동의어 사용 **9**(동의어 있는 그룹 8 · 이슈 7). 1차: 공백 포함 93(3낱말 이상 8) · 동의어 0.

### 11.4 호출 · 크레딧 · 비용
| 항목 | 값 |
|---|---|
| newsdata | **8/8**: 직전 크레딧 확인 1(200 · 남은 **97**; 1차 직후 98과 차이 1 — 이 실행 밖에서 사용) + 검색 6(group 마지막 remaining 91) + 직후 확인 1(200 · 남은 **90** = 97 − 6 − 1). 70 이상 · 내일 예약 실행 몫(최대 54) 남음 |
| OpenAI | **2/2**(organize 1회). 배정: 입력 16,478 · 출력 2,189(추론 1,256) / 메타: 입력 23,173 · 출력 3,540(추론 74). 합 입력 39,651 · 출력 5,729 → 약 0.007달러(D18 단가, 캐시 쓰기 단가 미확인) |
| 재시도 · 서버 | 0 · 띄우지 않음 |

### 11.5 증거(`ex12/.data/terms2-20261015/`, `wx` 덮어쓰기 금지 저장, `check-files.mjs` leak=false)
| 파일 | SHA-256 |
|---|---|
| `input-lines.txt`(158줄) | `7526497bea54c3989a8030f03c1407ab0a06ac23e41c998f512a9f3a49547ed5` |
| `openai-responses.json`(원본 응답 2 · usage) | `e4a60cbe47dbc6dba1be6e09e0405459c96dde4cc487feeb882b1f5c705148ce` |
| `group-result.json` | `3b8dc3c9decf088e750d54d6f95e8f0ca235228d43c2f20210fdd47b01f90bc6` |
| `newsdata-page1.json` | `9fcc6b85ff4edcc11397db31af67b0faa42854a1c8eb4acf7d010d71a812de74` |
| `result.json`(표 · terms 통계 · 사진 확인 · 사용량 · DB 해시) | `5948f437fc3deef19be237e424f79608912bd3dbfde240931638cb69dcc1b639` |
| 스크립트 `ex12/.data/verify-terms2.ts` | `f067b3ac008d271718334d0c4cef934271e87b4556a8d58561ff3f5c627a95ba`(leak=false) |

### 11.6 추가로 보인 점
1. a144 `[사진] 버넘 英 총리의 약속… "우크라에 대한 지원은 계속될 것"`은 발언 내용이 있는 기사지만 승인 규칙(접두면 제외)대로 제외됨. 사진 접두 + 실질 사건 기사도 빠지는 것은 D19-2 규칙의 의도된 결과로 기록.
2. soc `인천 AND 흉기난동` 1쪽에 후속 반응 기사(BJ 희화화 사과 · 모방범죄 대비)가 섞임 — 같은 사건의 연관기사로 봄. p 0.5로 n은 115 → 58로 줄어듦.
3. 영문 고유 명칭(`State Street` 등)은 한 낱말 지시와 달리 공백을 유지 — 영문 이름은 따옴표 구절이 맞으므로 문제로 보지 않음(기록만).
4. 변경은 미커밋. 10-16 06:17 KST 예약 실행은 푸시된 옛 코드로 돈다. 푸시는 그 확인 · ex09 예약 실행 심사 뒤 사용자 확인으로 → 다음 예약 실행에서 분포 기록(PLAN 순서 3).
5. 같은 표본(10-15)에서 1회만 본 결과다. 독립 심사(같은 한도: newsdata 8 · OpenAI 2)는 남은 크레딧 90 기준으로 가능하지만 내일 예약 실행 몫과 겹치지 않게 시점을 정해야 한다.

## 12. 2026-10-16 KST — 뉴스 모음 기사 머리표 제외(D19-3) 제작 · 실데이터 오탐 검사

범위: `plan/PLAN_backend_ex12.md` 「뉴스 모음 기사 머리표 제외 (D19-3)」 · PLAN.md D19-2 · D19-3. D19-2는 커밋 5156e89에 들어 있고 이번 변경은 그 위에 작업 트리로만. 커밋 · 푸시 안 함.

### 12.1 바뀐 것
- `ex12/server/ai.ts`
  - 새 순수 함수 `export function excludedBeforeAi(line)`: 입력 줄(`a000 [섹션] 제목 | 설명문`)이 사진 접두(`PHOTO_PREFIX` 그대로)이거나, 제목 맨 앞 대괄호 머리표(`/^\S+ \[[a-z]+\] \[([^\]]*)\]/`) 안의 글자에서 공백을 지운 값에 `헤드라인|뉴스리뷰|주요뉴스|뉴스브리핑`이 있으면 true. 머리표가 아닌 제목 · 설명문 속 낱말은 보지 않음.
  - organize: 사진 접두 제외 두 줄의 `PHOTO_PREFIX.test`를 `excludedBeforeAi`로 바꿈(같은 위치 · 방식). 그래서 모음 기사도 배정 입력 · 배정 스키마 · 후보 힌트 · 메타 입력에서 빠지고 excluded에 들어감, 번호 완전성 유지, 전부 해당하면 AI 호출 0.
  - 그대로: 지시문(ASSIGN · METADATA · SUMMARY) · 스키마 · 모델 · effort · `grouping.ts`(buildQuery · D13 · checkOrganize, 해시 `f085810c…` 불변).
- `ex12/test/ai.test.ts`: import에 `excludedBeforeAi` 추가 + 새 1개 — `[이시각헤드라인] 10월 15일 뉴스리뷰` · `[주요 뉴스]` · `[뉴스 브리핑]` · `[뉴스리뷰]` · `[헤드라인]` 제외, 머리표가 아닌 `헤드라인 뉴스 개편 발표 | 헤드라인` · `[단독] 헤드라인 조작 의혹` 유지, `[포토]` 함께 제외 → 배정 input · 스키마 · 메타 input에 제외 번호 없음 · excluded 5건 · 묶음+제외 = 전체 번호.
- ex09: 두 파일 복사.

| 파일 | SHA-256 |
|---|---|
| ex09 · ex12 `server/ai.ts` | `dff44e9fd7f48adbe8618fbf7c993b08d061f089ad948ac48592a41b2012b1f8`(같음) |
| ex09 · ex12 `test/ai.test.ts` | `c096e870fda5856c3b539a57657cd3e3e67bb4f6096b49aa510c71dc454e52d0`(같음) |
| ex08 · ex11 `server/ai.ts` | `07e10b1e7746732594bf904c6b17f74084f526d9486e9334336d3bca3a159134`(불변) |

### 12.2 테스트 · 빌드
| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 **119/119**(118 + 새 1) |
| `bash scripts/ex09-ubuntu.sh npm test` | 14파일 **115/115**(114 + 새 1) |
| `bash scripts/ex12-ubuntu.sh npm run build` · `bash scripts/ex09-ubuntu.sh npm run build` | 둘 다 성공(종료 0, `/_not-found` · `/api/[...path]`) |

vitest가 추적 파일을 바꾼 것 없음(`git status --short` 전후 같음). 새 패키지 0.

### 12.3 실데이터 오탐 검사(로컬, 읽기만)
방법: `ex12/.data/check-digest.ts`(sha256 `ab98432f…`)를 `node .data/safe-run.mjs -- node_modules/.bin/tsx .data/check-digest.ts`로 실행(출력 `leak:false`). fetch 전부 차단(외부 호출 0), DB 클라이언트 생성은 try 안 · 오류는 코드만. articles 전체를 읽기 전용 트랜잭션으로 읽어 날짜별 제품 `inputLines`로 입력 줄을 만들고 제품 `excludedBeforeAi`를 적용. articles 건수 · md5 전후 같음(**DB 쓰기 0**). 결과 `ex12/.data/digest-20261016/result.json`(sha256 `3791359e…`, `wx` 저장).

- 대상: 기사 **610건**(5일).
- 모음 머리표 일치 **1건**: 2026-10-16 `[이시각헤드라인] 10월 15일 뉴스리뷰` → 기대와 같음, **오탐 0**.
- 사진 접두 일치 **7건**: 10-15 5건(`[사진]트리플에스 김채연` 2 · `[사진]…트와이스 미나` 2 · `[사진] 버넘 英 총리의 약속…`), 10-16 2건(`[포토] 담양터미널 앞 가을꽃 심는 어르신들` · `[포토] 구탕, 득점이 필요해`). 버넘 총리 기사는 D19-2 11절과 같은 의도된 규칙 결과(발언 내용 있음).
- 모음 낱말이 제목 어디든 있으나 제외되지 않은 제목: 0건(본문 속 낱말 오탐 검토 대상 없음).
- **판정: PASS**(단위 테스트 + 일치 1건 · 오탐 0 · 외부 호출 0 · DB 쓰기 0).

### 12.4 추가로 보인 점
1. 머리표 판정은 대괄호 안 어디든 낱말이 있으면 제외한다(예 `[헤드라인 단독]`도 제외). 저장 기사에는 그런 꼴이 없어 실제 영향 0.
2. 다음 예약 실행부터 적용하려면 커밋 · 푸시가 필요하다(사용자 확인). 이번 변경은 AI 지시문을 바꾸지 않아 OpenAI 재검증은 하지 않았다.

## 13. D19-3 보완: 간추린 추가 (2026-10-16)

심사 11절에서 [간추린 단신] 모음 기사 미제외를 발견, 사용자 승인으로 DIGEST_WORD에 간추린 추가(단신 단독은 [단신] 실질 기사 오탐 위험으로 넣지 않음). ex12 수정 → ex09 복사, ai.ts e1a41d4f… · ai.test.ts 1e56fd2f… 두 폴더 일치, ex08·ex11 불변. 테스트에 [간추린 단신] 제외·[단신] 유지 2줄 추가. ex12 119/119·ex09 115/115·빌드 둘 다 종료0. 저장 기사 610건(읽기 전용) 판정: 제외 9 = 사진 7 + 모음 2([이시각헤드라인]…뉴스리뷰, [간추린 단신]…외), 오탐 0, 단신·간추린 머리표는 [간추린 단신] 1종뿐. 외부 호출 0·DB 쓰기 0. 코디네이터가 직접 제작(작은 변경).

## 14. D20 모바일 이슈맵 가독성 (2026-10-16)

근거: `plan/PLAN_backend_ex12.md` 끝 절 · PLAN.md D20. 제작은 Claude Code 세션(제작 역할). 커밋 · 푸시 없음.

### 14.1 변경 내용 (`ex12/public/index.html`만, +11 −1, sha256 `348787b4…`)
| 위치 | 변경 |
|---|---|
| `drawMap()` | 뷰포트 640px 이하(`matchMedia('(max-width: 640px)')` = 기존 CSS 경계) · 섹션 확대 아님일 때만 섹션 블록을 한 줄에 1개씩 세로로 쌓음. 블록 높이 `round(min(320, max(260, 지도폭×0.8)))`(360px → 262, 390px → 286), 지도 높이 = 이슈 있는 섹션 수 × 블록 높이(인라인 style). 그 밖(641px 이상 · 확대)에는 인라인 높이를 `''`로 비워 기존 CSS 높이 · 기존 squarify 그대로. 섹션 안 타일 산식(log1p squarify) · 색 · 문구 · aria-label 그대로 |
| CSS `.tile.xs` | 순위 숨김 해제: `.rk`를 보이고 `::after " ·"`로 `n위 · m건`(좁으면 두 줄로 줄바꿈, 낱말은 nowrap). 제목 · 북마크 표시는 xs에서 계속 숨김, `xxs`는 기존처럼 foot 숨김 |
| 유지 | 확대/전체 전환 · 범례 · 상세창 · 다른 화면 · API 변경 0. 새 패키지 0. ex09 미복사 |

참고: xs 순위 표시 CSS는 폭과 무관(PLAN 「작은 타일」 줄)이라 넓은 화면에 xs 타일이 생기면 그곳도 `n위 · m건`이 보인다. 오늘 데이터로 768·1280의 xs는 0개라 화면 차이 없음.

### 14.2 측정 (실제 Chromium 155 headless + CDP, `next start` 127.0.0.1:3000, 10-16 랭킹 30타일)
스크립트 `ex12/.data/verify-mapmobile.mjs`(sha256 `47edebd9…`, `node .data/verify-mapmobile.mjs <before|after> 9339`, 결과 저장 전 `.env` 비밀값 검사 `leak:false`). 변경 전 코드로 `before`, 변경 후 `after` 실행.

| 뷰포트 | 전/후 | 지도 높이 | 타일 | xs | xxs | 순위 보임 | 섹션 1위 제목(줄 수) | 가로 스크롤 |
|---|---|---|---|---|---|---|---|---|
| 360×800 | 전 | 760 | 30 | 12 | 0 | 18/30 | 6/6 (3·2·4·3·4·4) | 0 |
| 360×800 | 후 | 1572 | 30 | **2** | 0 | **30/30** | 6/6 (2·3·2·3·3·3) | 0 (360=360) |
| 390×844 | 전 | 760 | 30 | 9 | 0 | 21/30 | 6/6 | 0 |
| 390×844 | 후 | 1716 | 30 | **0** | 0 | **30/30** | 6/6 (2·3·2·3·3·3) | 0 (390=390) |
| 768×1024 | 전=후 | 764 | 30 | 0 | 0 | 30/30 | 6/6 | 0 |
| 1280×800 | 전=후 | 540 | 30 | 0 | 0 | 30/30 | 6/6 | 0 |

- 360 xs 2개는 정치 4·5위(폭 67.6px), 표시 `4위 · 1건` · `5위 · 1건`(순위 · 건수 모두 타일 안).
- 768·1280 넓은 화면: 섹션 블록 6개 + 타일 30개의 data-id별 left/top/width/height · xs/xxs가 변경 전과 **문자열까지 동일**(true/true).
- 타일 클릭(360, 가장 작은 정치 4위) → 상세창 h2 = 이슈 제목 true. 정치 확대 → 지도 760 · 정치 블록만 · 가로 스크롤 0 → 전체 복귀 1572 · 6블록.
- 다크 모드(`prefers-color-scheme: dark` 에뮬레이션, 360): 배경 rgb(18,18,17) · 가로 스크롤 0, 캡처 눈으로 확인(타일 흰 글자 · 섹션 이름 읽힘).
- 요청: 뷰포트마다 `GET /api/rankings` · `GET /api/auth/get-session`만(전 4쌍 · 후 4쌍), 외부(newsdata · OpenAI) 0, DB 쓰기 0(랭킹 읽기만). 서버 로그 Error 없음.

캡처(`ex12/.data/mapmobile-20261016/`): `before-{360x800,390x844,768x1024,1280x800}.png` · `after-{360x800,390x844,768x1024,1280x800}.png` · `after-360-drawer.png` · `after-360-zoom-pol.png` · `after-360-dark.png`, 측정값 `before.json` · `after.json`. 전체 길이 캡처라 `before-*`에는 닫힌 바텀시트가 아래를 흰색으로 덮고(캡처 방식 문제 — `after-*`는 캡처 동안만 숨김), drawer · dark 캡처는 스크롤 위치의 sticky 머리글 · 시트가 중간에 찍힘(화면 문제 아님).

### 14.3 테스트 · 빌드 · 정리
| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 **119/119** |
| `bash scripts/ex12-ubuntu.sh npm run build` | 성공(종료 0) — 변경 전(기준 측정용) · 후 두 번 |

vitest 뒤 `git status --short` 변경 없음(추적 파일은 index.html · 사용자의 PLAN 두 파일뿐). 종료 확인: `curl` 3000 = 000 · CDP 9339 = 000, `ps -ef | grep -E "next-server|next start|chromium"` 0줄, 브라우저 프로필(스크래치 폴더) 삭제.

### 14.4 남은 것
- 배포 확인(PLAN 검증 마지막 문장): 사용자 확인 후 커밋 · 푸시 → Vercel 자동 재배포 → 배포 주소 360px 캡처 1회. 아직 미실행(SKIP).
- 독립 심사(`reports/ex12_judge.md` 새 절).
