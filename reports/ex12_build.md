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
