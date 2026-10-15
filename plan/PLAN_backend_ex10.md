# PLAN_backend_ex10 — Better Auth 이메일·비밀번호 로그인

> 이유: 화면의 로그인은 상태만 바꾸는 데모다. 실제 가입·로그인·로그아웃과 새로고침 후 세션 복원을 연결한다.
> 코드 폴더: `ex10/` (`ex08/` 출발, 이미 복사됨·재복사 없음). 사용자 승인(2026-10-14)으로 ex08 품질 FAIL·ex09 자동화를 보류한다.
> SPEC 근거: US-06 · AC-06-1 · AC-06-2. AC-07-3은 기존 화면 안내 연결만, 서버 접근 차단·계정별 저장 완성은 ex11.

## 이번 범위
- `server/auth.ts`에서 Better Auth와 기존 Postgres/Drizzle 연결, Hono `/api/auth/*`, Next BFF 쿠키 전달, `public/index.html` 실제 인증. 기존 데이터·AI 코드·폴더/북마크 계약은 보존한다.
- 새 패키지: `better-auth` · `@better-auth/drizzle-adapter`(런타임), `auth`(개발용 공식 스키마 생성 CLI). 설치 시 npm registry의 호환 버전을 확인해 같은 릴리스로 고정하고 lockfile·보고에 기록한다. 기존 패키지 임의 업그레이드 없음.
- 하지 않음: Google 등 소셜 OAuth(후속 예정)·계정 연결·이메일 인증/발송·비밀번호 재설정·배포·ex09 자동 실행·ex08 품질 수정·ex11 `user_id`/북마크·폴더 API의 401/계정별 분리. 기존 사용자 없는 데이터 삭제·이관 없음.

## 로그인 규칙
| 항목 | 규칙 |
|---|---|
| 구성·외부 준비 | 기존 Supabase DB만 사용. `ex10/.env`의 `DATABASE_URL`·`BETTER_AUTH_SECRET`(고엔트로피 32자 이상)·`BETTER_AUTH_URL=http://localhost:3000` 필요. 부모가 로컬 준비, 값 출력 없음. OAuth/SMTP/API 키 추가 불필요. `basePath=/api/auth`, 신뢰 출처는 앱 주소로 제한. CSRF·Origin 검사 비활성화 금지. |
| DB | 공식 CLI `auth generate`로 별도 `server/db/auth-schema.ts` 생성(기존 schema 덮어쓰기 금지), 기존 `schema.ts`에서 내보내 Drizzle와 adapter에 연결. user/session/account/verification 4테이블·필요 인덱스/FK만 `0005` 추가. 이메일 unique, 비밀번호는 라이브러리 해시, 세션은 DB 저장. 신규 인증 4테이블도 기존 Supabase 패턴대로 RLS를 활성화하고 공개 정책은 추가하지 않는다. 기존 0000~0004 및 업무 테이블 유지. |
| 가입 | `POST /api/auth/sign-up/email` JSON `{name,email,password}`. 이름은 빈 값 불가, 이메일 형식, 비밀번호 8~128자. `emailAndPassword.enabled=true`, 가입 후 자동로그인, 이메일 인증 요구 없음. 정상 200·계정/세션 생성·Set-Cookie. 중복 이메일 422·형식/길이 오류 400(라이브러리 오류 code 그대로, 새 세션 없음). |
| 로그인 | `POST /api/auth/sign-in/email` JSON `{email,password}`. 정상 200·세션 쿠키. 틀린 비밀번호/없는 이메일 401·새 세션 없음, 화면 공통 문구 “이메일 또는 비밀번호를 확인해 주세요”. 인증 성공 확인 전 화면을 로그인으로 바꾸지 않음. |
| 세션·로그아웃 | `GET /api/auth/get-session`: 유효하면 200 사용자·세션, 쿠키 없음/변조/만료면 200 null. `POST /api/auth/sign-out`: 200·현재 서버 세션 폐기·쿠키 만료. 기존 쿠키 재사용해도 null. 초기 로딩/새로고침은 세션 조회로 상태 결정, 토큰·비밀번호 localStorage 저장 금지. |
| BFF | 브라우저는 같은 출처 `/api/auth/*`만 fetch. 요청 Cookie·Origin·본문, 응답 상태·본문·복수 Set-Cookie 각각을 보존. auth 응답 `Cache-Control: no-store`, 쿠키 HttpOnly·SameSite 라이브러리 기본 유지(HTTPS에서 Secure). 임의 wildcard 신뢰 출처·공유 Domain 없음. |
| 실패·비밀값 | 서버 시작 시 DB/auth 생성도 try 안, 필수 env 누락은 안전한 코드로 실패. auth logger 및 Hono onError가 DB URL·SQL 인수·비밀번호·쿠키/토큰·오류 객체/스택을 출력하지 않게 안전 코드만 기록. 화면 오류는 한국어 고정 문구로 매핑하고 이름은 HTML escape/textContent. |
| 범위 경계 | 로그인 전 북마크 버튼/북마크 화면은 기존 안내→실제 로그인 폼. 로그인 성공 뒤 기존 폴더/북마크를 불러오며 로그아웃 뒤 메모리 목록 비움. ex10의 해당 API는 여전히 공용이며 인증·사용자별 격리는 ex11에서 완성; ex10 인증만으로 개인 저장이 완성됐다고 보고하지 않음. |

## 화면 변경
- 기존 로그인 버튼·안내에서 열리는 가입/로그인 폼 추가: 가입은 이름·이메일·비밀번호, 로그인은 이메일·비밀번호. 전환·닫기·오류 표시·요청 중 중복 제출 방지, 입력 label·비밀번호 type·모바일 폭 확보. 성공 시 폼 닫고 실제 사용자 표시와 로그아웃 버튼, 비밀번호 입력 비우기.
- “로그인했습니다 (데모)” 토글만 실제 흐름으로 교체하고 기존 이슈맵·카드·날짜·섹션·북마크 안내 문구 유지. 로그인 실패는 비로그인 그대로, 가입 성공은 즉시 로그인(AC-06-1·2).

## 구현 순서
1. ex10 상태 보존, `bash scripts/ex10-ubuntu.sh npm ci --no-audit --no-fund` 후 승인 패키지 설치. 같은 래퍼로 모든 패키지 명령 실행(Termux 직접 빌드 금지).
2. auth 설정/CLI 생성·Drizzle0005 추가, SQL 검토 후 안전 실행으로 기존 적용기록 확인 및 미적용0005만 적용. Next BFF·Hono 연결과 OpenAPI에 실제 사용하는 인증 4경로/쿠키/오류 계약 추가.
3. 화면 데모를 실제 흐름으로 연결하고 격리된 라이브러리 테스트 어댑터/fixture로 인증·BFF·기존 76개 회귀 검증. 실제 DB 검증은 아래 범위만. 제작 보고에 변경·이전코드 보존·SKIP·사용한 버전을 기록.
- 현재 Termux/proot의 Next.js LAN 주소 탐지 실패(`uv_interface_addresses`)를 피하려면 dev/start에 `--hostname 127.0.0.1`을 지정한다. 접속 Origin은 설정과 같은 `http://localhost:3000`을 유지한다.
마지막. `bash scripts/ex10-ubuntu.sh npm test` · `bash scripts/ex10-ubuntu.sh npm run build`, 심사는 별도 에이전트가 코드 수정 없이 수행.

## 검증 방법
- **ex10 실행 범위가 아래 보존한 과거 검증표보다 우선한다.** 뉴스/OpenAI 호출은 제작·심사 모두 0회, collect/group/summarize 실서비스 실행 없음. 과거 AI 의미 FAIL은 보류 그대로이며 ex10 인증의 합격과 구분한다. 기존 fixture·로컬 회귀는 실행하되 과거 실서비스/샘플 DB/원상복구 SQL·mutation 검사는 이번 범위에서 실행하지 않는다.
- 안전 실행: `ex10/.env`만 로드한 부모 프로세스가 자식 stdout+stderr를 먼저 메모리에 캡처. DATABASE_URL·URL에서 추출한 인코딩/디코딩 비밀번호·기존 API 키·BETTER_AUTH_SECRET 및 검증 중 생성한 비밀번호/세션 토큰/쿠키 존재를 검사해 `leak:false`일 때만 안전한 결과를 출력한다. 누출이면 원문 미출력·실패 코드만. SQL 클라이언트 생성도 try 안, 오류 객체/스택 출력 금지. API 응답·Set-Cookie 원문은 출력/보고하지 않고 상태·검증 bool·건수만 기록.
- 실제 DB: 마이그레이션 전 기존 업무 테이블 행수와 내용 해시/인증 테이블 존재·Drizzle 적용기록을 확인. 0000~0004 적용을 확인하지 못하면 이력 복구를 임의 수행하지 말고 해당 DB 검증 SKIP. 0005는 auth테이블 신규 생성만 검토 후 적용하며 재실행은 변경0. 테스트용 UUID 이메일 `ex10-<uuid>@example.invalid`로 제작·심사 각각 최대 2계정, 요청 총 30회 이하/역할. 이번 생성 user ID를 즉시 기록하고 finally에서 해당 session/account/user만 삭제(verification 생성 시 이번 identifier만), 기존 사용자·업무 행/테이블 전체 삭제 금지. auth 스키마는 결과물로 남기며 원본 기사·이슈·요약·북마크/폴더 불변을 재확인한다.
- 실제 요청은 임시로 띄운 Hono8787·Next3000을 통한 Node fetch, 쿠키 jar는 메모리만. 다른 서버가 점유하면 종료하지 않고 환경 이유 기록. 브라우저 도구 없으면 UI 관련 줄은 SKIP하고 API 검증을 브라우저 검증으로 대체하지 않음. SKIP 남으면 전체 PASS 금지.

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 로컬 회귀·빌드 | 위 wrapper test/build, ex08와 ex10 소스 비교 | 기존 76개 포함 전체 테스트·타입/빌드 통과, ex08 수정0, AI/순위 계약 유지 |
| auth 단위 통합 | 실제 Better Auth + 격리 메모리 어댑터로 가입→쿠키 세션→로그아웃→틀린/정상 로그인, 중복/길이/변조/만료; 테스트는 외부 DB/API 없음 | 실제 라이브러리 비밀번호 해시와 세션 경로 검증, 상태/오류 위 표와 일치, 모의 handler만으로 인증 PASS 금지 |
| BFF 쿠키 회귀 | 응답 쿠키 2개(Expires 포함)·오류상태 모의 upstream, 요청 Cookie/Origin/본문 비교 | 복수 Set-Cookie 분리 유지, 응답/상태 그대로·no-store, upstream 실패 502 “API 서버에 연결할 수 없습니다” |
| 설정·로그 | 필수 변수 누락·DB/auth 예외를 가짜 비밀값으로 유발, 출력 검사 | 안전 코드만, 비밀값·원본 오류0, 잘못된 설정으로 인증 성공0 |
| DB 마이그레이션·보존 | 안전 실행으로 적용이력·SQL 확인→0005 적용→재실행 및 기존 업무 데이터 비교 | 인증 4테이블/필요 제약 생성, 기존 테이블/데이터 불변, 반복 변경0 |
| 가입/API(AC-06-1) | BFF POST 가입→쿠키 GET get-session→서버 재시작 후 같은 쿠키 조회 | 200, 같은 user ID 및 DB계정1·해시≠입력, 재시작 후 세션 유지 |
| 잘못된 로그인(API, AC-06-2) | 로그아웃 후 잘못된 비번·없는 이메일 POST 로그인→세션 조회, 중복가입/짧은비번 요청 | 로그인401·세션null, 중복422·길이400, 새 계정/세션0 |
| 정상 로그인·폐기·출처 | 정상 로그인→세션→로그아웃→이전쿠키 재사용; 악성 Origin POST | 로그인200·본인정보, 로그아웃 뒤null, 악성출처403·세션생성0, 비밀값 출력0 |
| UI 가입/오류(AC-06-1·2) | 브라우저 localhost:3000 가입폼 제출·새로고침·로그아웃·틀린 로그인·정상 로그인 | 가입 즉시 사용자/로그아웃 표시, 새로고침 유지, 오류 문구/비로그인 유지, 정상 성공. 모바일 폼 가로넘침0 |
| 기존 화면 안내/회귀 | 비로그인 북마크 버튼·북마크 탭→로그인폼, 이슈맵/목록/날짜/원문/요약 확인 | 기존 로그인 안내, 로그인 없이 저장요청0, 랭킹 열람 가능, 카드/문구/링크 보존 |
| 정리 | finally 이번 생성 ID 행만 삭제, 전후 건수/해시 및 소유 서버 종료 확인 | 임시계정/세션0·기존 데이터 불변, 뉴스0·OpenAI0 |

### 공식 근거(2026-10-14 확인)
[설치·환경](https://better-auth.com/docs/installation), [이메일·비밀번호](https://better-auth.com/docs/authentication/email-password), [Drizzle 어댑터](https://better-auth.com/docs/adapters/drizzle), [CLI](https://better-auth.com/docs/concepts/cli), [Hono](https://better-auth.com/docs/integrations/hono), [세션](https://better-auth.com/docs/concepts/session-management). 문서는 최신1.7.7 표기이나 설치 가능 버전은 registry/lockfile로 확정한다. 상세 응답 code는 설치된 라이브러리와 검증해 계약에 기록하며 보안 검사를 우회하지 않는다.

### 앞 단계 검증표 보존
아래는 `PLAN_backend_ex08.md` 검증표 원문이다. 역사·회귀 기준 보존을 위한 부록이며 실행 범위·플랫폼 명령·현행 테스트 수·로그인 상태에 따른 화면 기대값은 위 ex10 기준을 우선한다. 약50줄 원칙보다 이전 검증표 보존과 실제 인증/DB 안전절차를 우선해 길어졌다.

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 계약 그대로 | `git diff --no-index ex07/openapi.yaml ex08/openapi.yaml` · `npx @redocly/cli lint ex08/openapi.yaml`(설치 없이 npx) | 차이 없음(ex05에서 확인한 엔드포인트 · 상태 코드 · 실패 메시지 · 이슈 칸 · 섹션 키 · 폴더 이름 · 이슈 id 규칙 그대로) · 오류 0개(경고는 참고) |
| 출발점 · fixture 그대로 | `git status frontend/ ex01/ ex02/ ex03/ ex04/ ex05/ ex06/ ex07/` · `git diff --no-index research/newsdata ex08/test/fixtures/newsdata` | 변경 없음(` M ex07/.env.example` 한 줄 말고: ex07 뒤 `OPENAI_API_KEY=` 이름만 더한 것, ex08 커밋에 함께 넣음) · 차이 없음(7개 파일) |
| 빌드 | `npm.cmd run build` | 성공 |
| 테스트 | `npm.cmd test` | 테스트 파일 6개 모두 통과, 실패 0 |
| 테스트 범위 | 테스트 이름 · 내용을 `plan/PLAN_backend_ex04.md` 테스트 표 + `plan/PLAN_backend_ex05.md` "새 테스트" 줄 + `plan/PLAN_backend_ex06.md` "새 테스트" ①~⑥(`plan/PLAN_backend_ex07.md`에서 고친 대로) + `plan/PLAN_backend_ex07.md` `group.test.ts` ①~⑤ · `ranking.test.ts` ⑦ ⑧ + 위 `group.test.ts` ⑥~⑧ · `summarize.test.ts` ①~③과 비교 | 15개 묶음 + 3개 + 6개 + 5개 + 2개 + 3개 + 3개가 각각 테스트로 있음(AC-03-1 · 03-2 · 07-1 · 07-2 · 08-1 · 08-2 · 09-1 · 09-3 · 09-4 · 10-1 · 10-2 · 10-4 · D3 · AC-01-1 · AC-01-2 · AC-02-1 · AC-02-2 · AC-05-1 · D8 · D15 · AC-04-3) |
| 서버 · DB 없이 | npm test(현재 기기는 Ubuntu 래퍼), test/의 외부 연결 확인 | 모두 통과. 기존 규칙 테스트는 네트워크·DB 없음. ai.test.ts는 가짜 HTTP 응답으로 실제 SDK 요청·파싱만 검증하고 실제 API 호출 없음 |
| 시계 고정 | PowerShell `$env:TZ='America/Los_Angeles'; npm.cmd test`, `test/`에서 `new Date\(\)` · `Date\.now\(` 검색 | 통과, 0건 |
| 순서 무관 | `npx vitest run --sequence.shuffle` | 통과 |
| 테스트가 잡는가 | `server/ranking.ts`의 발행 시각 비교 방향을 뒤집고 `npm.cmd test` → 되돌림. `server/newsdata.ts`에서 failed 섹션의 그날 행도 지우게 바꾸고 → 되돌림. `server/grouping.ts`에서 대표 제목 대신 묶음 안 아무 기사와 비교하게(줄줄이 잇기) → 되돌림. `server/grouping.ts`의 합치기를 끄고 → 되돌림. `server/grouping.ts`의 1쪽 검증 p를 늘 1로 → 되돌림. `server/ranking.ts`에서 D8 대체를 끄고(그날 행만) → 되돌림. `server/grouping.ts`의 없는 번호 검사를 끄고 → 되돌림. AI 섹션 대신 수집 섹션을 쓰게 → 되돌림. `server/summary.ts`에서 요약 있는 이슈도 대상으로 → 되돌리고 다시 | 동점 테스트 실패 · ④ ⑥ 실패 · group ①(줄줄이 잇기: 누리호+군집위성 3건도 한 묶음) 실패 · group ① ② ③(합치기 끔: it 3묶음) 실패 · group ② ⑤ 실패 · ⑦ 실패 · group ⑦(`a999`) 실패 · group ⑥ ⑧ 실패 · summarize ① ② 실패 → 모두 통과 |
| 화면 그대로 | `git diff --no-index ex07/public/index.html ex08/public/index.html` | 차이 없음(ex07의 화면 문구 · 화면 보완 검증 결과 그대로) |
| 테이블만 | 새 SQL · snapshot · journal · schema.ts 비교 | 0004에 issues.importance integer NOT NULL DEFAULT 0만 추가. 기존 테이블·행 삭제 없음 |
| 마이그레이션 | 키 준비 후 db:migrate → public 테이블·drizzle.__drizzle_migrations·issues.importance 확인 → 한 번 더 | 테이블 5개, 마이그레이션 기록 5행, importance integer NOT NULL DEFAULT 0, 기존 행 0, 재실행해도 5행. 키 없으면 SKIP |
| 연결 설정 | prepare false · X-ACCESS-KEY · OpenAI 모델 · SDK import · .env.example · git check-ignore ex08/.env | DB prepare false 유지, newsdata 키 헤더 유지, OpenAI SDK는 ai.ts만, DATABASE_URL · NEWSDATA_API_KEY · OPENAI_API_KEY 이름(비밀값 없음), .env 무시 |
| 저장소 나눔 | `server/app.ts` · `server/routes/` · `server/newsdata.ts` · `server/grouping.ts` · `server/summary.ts` · `server/ai.ts` · 기존 규칙 테스트(ai.test.ts의 SDK/HTTP 모의, hide.test.ts의 가짜 env 검증은 예외)에서 `\bpostgres\b` · `drizzle-orm` · `db/client` · `process\.env` 검색 | 0건(DB 연결은 `index.ts` · `server/collect.ts` · `server/group.ts` · `server/summarize.ts`만) |
| 샘플 없음(D14) | SQL: `url` = `https://news.naver.com/`인 issues · (id, 이름)이 샘플 3개인 folders · 그 둘을 가리키는 bookmarks 행 수 → `npm.cmd run seed` → `ex08/server/db/seed.ts` 있는지, `server/`에서 `\bseedRows\b` · `\bmemoryStore\b` 검색 → `GET /api/bookmarks` · `GET /api/folders` | 0 · 0 · 0 → 종료 코드 0 아님 · `Missing script: "seed"` → 없음, `server/sample.ts`에만 → 샘플 이슈 · 폴더 없음(사용자가 만든 것이 없으면 `[]` · `[]`, AC-08-2) |
| 지난 날짜 · 대체 섹션(D8) | SQL로 확인 행: issues `2000-01-03-{섹션}-1` 6행(article_count 10) · `2000-01-04-pol-1`~`-6`(60 · 50 · 40 · 30 · 20 · 10), 링크 `https://example.com/ex07-{id}`, latest_published_at 그 전날 `12:00Z`, summary null / collect_runs `2000-01-04` pol `ok` `2000-01-03T21:03:05Z` · pol `failed` `2000-01-03T22:00:00Z` → `GET /api/rankings?date=2000-01-04` · `?date=2000-01-03` · `?date=2000-01-05` · `?date=2000-01-02` | 200 `updatedAt` `2000-01-04T06:03:05+09:00` · pol 5개(`-1`~`-5`, rank 1~5, 60~20) · 나머지 5섹션은 `2000-01-03-{섹션}-1` 하나씩 · `date` `2000-01-03`(AC-01-2, D8) · 200 `updatedAt` `2000-01-03T06:00:00+09:00`(ok 실행 없음) · 404 · 404 "해당 날짜의 랭킹 데이터가 없습니다"(AC-09-1 · AC-09-3) |
| 다시 켜도 유지 | `POST /api/folders` `{ name: "유지확인" }` → 그 id로 `2000-01-04-pol-1` POST → api 서버를 끄고 다시 `npm.cmd run api` → 두 목록 | 201 `{ id: "f{n}", name: "유지확인", count: 0 }`(n = 있던 폴더 번호 최대 + 1, 없으면 1) → 201 → 재시작 뒤 북마크 맨 앞이 그 이슈(`rank` 1 · `folderId` 그 폴더) · 폴더 목록 끝에 유지확인 count 1(US-09 · US-10 · AC-09-5 · AC-10-2 · AC-08-1) |
| 폴더 하나 · 해제(DB) | 같은 이슈로 다시 POST → `{ issueId: "2000-01-04-pol-6", folderId: 그 id }`(Top 5 밖) · `{ issueId: "2000-01-01-pol-0", folderId: 그 id }` · `{ issueId: "2000-01-04-pol-1", folderId: "f999" }` POST → 그 이슈 DELETE 두 번 → 폴더 목록 | 409 "이미 북마크한 이슈입니다" → 404 "이슈를 찾을 수 없습니다" · 같음 · "폴더를 찾을 수 없습니다" → 204 · 404 "북마크를 찾을 수 없습니다" → 유지확인 count 0(AC-10-4 · AC-07-2) |
| 수집 · 묶기 · 요약 전 거절 | `ex08/`의 PowerShell에서 `Remove-Item Env:NEWSDATA_API_KEY, Env:OPENAI_API_KEY -ErrorAction SilentlyContinue` 뒤 `npx.cmd tsx server/collect.ts --pages 3` · `--pages x` · `--pages 0`, `npx.cmd tsx server/group.ts --search 7` · `--search 1` · `--ai 2` · `--search 0`, `npx.cmd tsx server/summarize.ts --max 31` · `--max 1`(`.env`를 안 읽어 키 없음), 각각 `$LASTEXITCODE` | 모두 2, 출력은 `--pages는 0~2입니다` · 같음 · `NEWSDATA_API_KEY가 없습니다` · `--search는 0~6입니다` · `NEWSDATA_API_KEY가 없습니다` · `--ai는 0 또는 1입니다` · `OPENAI_API_KEY가 없습니다` · `--max는 0~30입니다` · `OPENAI_API_KEY가 없습니다` 한 줄씩(섹션 · 요청 줄 없음 = 요청 0번) |
| DB 오류 | 안전 실행으로 `npm.cmd run collect -- --pages 1` · `npm.cmd run group -- --search 1` · `npm.cmd run summarize -- --max 1`을 각각 두 번: 자식 env `NEWSDATA_API_KEY=wrong-key-0000` · `OPENAI_API_KEY=wrong-ant-0000`, `DATABASE_URL`은 `postgres://u:SECRETPW@[bad/x` 한 번 · `postgres://u:SECRETPW@127.0.0.1:9/x` 한 번 | 여섯 다 누출 false(`SECRETPW` · `wrong-key-0000` · `wrong-ant-0000` 포함) · 종료 코드 2 · `DB 오류: `로 시작하는 줄이 있고 섹션 · AI · 요약 줄(`pol ok` · `pol 기사` · `AI 정리` · `  … ok` 등) 없음(요청 전에 멈춤) |
| 수집(실제) | SQL로 확인 행 2개(section pol, 링크 · date는 `https://example.com/ex07-today` · <오늘>과 `https://example.com/ex07-yesterday` · <어제>, 나머지 칸은 아무 값)를 넣고 → 안전 실행 `npm.cmd run collect -- --pages 1`(실제 호출 12번) | 누출 false · 종료 코드 0 · `수집 <오늘> · 섹션당 top 1페이지 + 전체 최대 1페이지` 뒤 pol→eco→soc→cul→wor→it 6줄 `ok` · 끝 줄 `ok 6 · failed 0 · 기사 {n}건 · 요청 12번 · 남은 크레딧 {숫자}`(이 숫자를 R1로 적음). 실제 응답 탓 failed 줄이 있으면 종료 코드 1 · 메시지가 실패 규칙 형식(원인을 보고) |
| 수집 결과(DB) | SQL: <오늘> `collect_runs` 마지막 6행 · <오늘> `articles` 섹션별 행 수 · `source_id` 종류 수 · `(date, link)` 중복 수 · `published_at` > `finished_at`인 행 수 · 확인 행 2개 | 6행 `finished_at` 같음 · ok 섹션마다 `message` null · `article_count` = 그 섹션 행 수(1~20), `source_id` 6종 이상(D12), 중복 0 · 0행, `ex07-today` 없음(pol ok일 때) · `ex07-yesterday` 그대로(AC-01-1) |
| 잘못된 키(수집) | SQL로 `collect_runs` 최대 id를 M으로 적음 → 안전 실행(자식 env `NEWSDATA_API_KEY=wrong-key-0000`) `npm.cmd run collect -- --pages 1` → SQL: `id > M` 행 · <오늘> `articles` 섹션별 행 수 · `message`에 실제 키나 `wrong-key-0000`이 든 행 수 | 누출 false · 종료 코드 2 · 6줄 모두 `failed`(메시지에 `HTTP {코드}`, 보통 401) · 끝 줄에 `요청 6번` → 6행 모두 failed · `article_count` 0, 섹션별 행 수는 `수집 결과(DB)`와 같음(AC-01-2: 실패 섹션은 그날 기사 유지), 0행 |
| 묶기(검색 없이) | 안전 실행 `npm.cmd run group -- --search 0 --ai 0` → SQL: <오늘> 기사 중 `issue_id` null 행 수 · 이슈마다 묶인 기사 수 · `article_count` · `url`이 묶인 기사 `link` 중 하나인지 · id 꼴 | 누출 false · 종료 코드 0 · 첫 줄 `묶기 <오늘> · 섹션당 검색 최대 0개` · 둘째 줄 `AI 정리 안 함(--ai 0)`, 섹션마다 `{섹션} 기사 {a} · 이슈 {i}`(a = `수집 결과(DB)` 행 수) + 후보 줄 최대 6개(` 검색 안 함`) · 끝 줄 `이슈 {n} · 검색 0번 · 실패 0 · 남은 크레딧 ?` → 0행, 묶인 기사 수 = 출력의 묶음 = `article_count`(단독 이슈는 1, AC-02-2), 모두 예, `<오늘>-{섹션}-{n}`이고 겹치지 않음(AC-02-1) |
| 다시 묶기(id 유지 · 북마크) | SQL로 <오늘> issues의 (id, url, article_count) 목록을 적음 → `GET /api/rankings`에서 <오늘> 이슈가 있는 첫 섹션 S의 1위를 `유지확인` 폴더로 POST → 안전 실행 `npm.cmd run group -- --search 0 --ai 0` → SQL 목록 → 그 북마크 DELETE | 201 → 종료 코드 0 · `{S} 건너뜀: 북마크 있음` · 다른 섹션 줄은 앞과 같음 → 목록이 앞과 똑같음(같은 기사 → 같은 대표 → 같은 id, 중복 없음) → 204 |
| 잘못된 키(묶기) | 안전 실행(자식 env `NEWSDATA_API_KEY=wrong-key-0000`) `npm.cmd run group -- --search 1 --ai 0` → SQL 목록 | 누출 false · 종료 코드 1 · 섹션마다 첫 후보 줄에 ` 검색 실패: HTTP {코드}`(보통 401) · 끝 줄 `검색 {첫 후보에 검색어가 있는 섹션 수}번 · 실패 {같은 수}` → 목록이 `다시 묶기`와 같음(검색 실패 → 묶음 크기, D13) |
| 잘못된 키(AI 정리) | 안전 실행(자식 env `OPENAI_API_KEY=wrong-ant-0000`) `npm.cmd run group -- --search 0` → SQL 목록 | 누출 false · 종료 코드 1 · 둘째 줄 `AI 정리 실패: 호출 실패: …`(401 문구, 키 없음) `→ 규칙으로 묶음` → 목록이 `다시 묶기`와 같음(D15 대체) |
| 묶기(실제) | 안전 실행 `npm.cmd run group -- --search 1`(실제 호출 = OpenAI 정리 1번 + 첫 후보가 있는 섹션 수, 최대 6) → SQL: <오늘> 섹션별 이슈 수 · `issue_id` null 기사 수 · 이슈 섹션이 묶인 기사 수집 섹션과 다른 기사 수 → 크레딧 확인(실제 호출 1번) | 누출 false · 종료 코드 0 · 둘째 줄 `AI 정리 · 묶음 {b} · 제외 {x} · 빠진 기사 {m}` · 섹션마다 첫 후보 줄이 `{id} {AI 검색어} {n}건 (묶음 {k})`이고 n ≥ k, 나머지는 ` 검색 안 함` · 모든 후보 줄 n < 1,000 · 끝 줄 `실패 0 · 남은 크레딧 R2`(R2 = R1 − 검색 수) → 이슈 수 합 = `이슈 {n}`, null 기사 수 = x(+ 빠진 기사 없으면 그대로) · 사람 확인: 각 섹션 첫 후보의 대표 제목 · 묶인 기사 제목 · 섹션 · 검색어를 보고에 적어 한 사건인지 · 섹션이 맞는지 · 제외가 사진 · 주가 기사인지 봄(AC-02-1, D15) → 상태 200 · `x-api-limit-remaining` = R2 − 1(다르면 차이를 숫자로 적음). AI 정리 실패 · 검색 실패가 있으면 종료 코드 1 · 원인을 보고(다시 돌리지 않음) |
| 오늘 랭킹(DB) | `GET /api/rankings` · `?date=<오늘>` | 둘 다 200 · 같은 본문(D3), `date` <오늘>, `updatedAt` = SQL로 본 <오늘> `ok` `collect_runs` 최대 `finished_at`을 KST `+09:00`(초까지)로 바꾼 값(AC-05-1), 섹션 6개 · 섹션마다 5개 이하 · `articleCount` 내림차순 · `rank` 1부터, <오늘> 이슈가 있는 섹션의 순위 = 저장된 이슈 전체를 연관기사 수 ↓ → importance ↓ → 최신 발행 시각 ↓ → id 자연 순서로 정렬한 Top 5(검색 전 첫 후보와 같다고 가정하지 않음), 없는 섹션은 이전 날짜 이슈(D8), `url`은 언론사 주소(`news.naver.com` 아님) · `summary`는 null(같은 날 앞서 요약한 이슈가 id를 유지했으면 그 이슈만 3줄, 그 수를 k로 적음, AC-03-1 · AC-02-1) |
| 요약(실제) | 안전 실행 `npm.cmd run summarize -- --max 3`(실제 OpenAI 호출 3번) → SQL: 출력의 세 id `summary` | 누출 false · 종료 코드 0 · 첫 줄 `요약 <오늘> · 최대 3개` · `  {id} ok` 3줄(`오늘 랭킹(DB)`에서 summary null인 이슈의 pol→it · rank 순 앞 3개) · 끝 줄 `대상 {t} · 요청 3번 · 성공 3 · 실패 0 · 이미 요약 {k}` → 셋 다 3줄 · 줄마다 1~100자 · 사람 확인: 3줄을 보고에 적고 묶인 기사 제목 · 설명문에 있는 내용인지 봄(US-04). 실패가 있으면 종료 코드 1 · 그 이슈 `summary` null · 원인을 보고(다시 돌리지 않음) |
| 요약 다시(잘못된 키) | 안전 실행(자식 env `OPENAI_API_KEY=wrong-ant-0000`) `npm.cmd run summarize -- --max 3` → SQL로 `요약(실제)`의 세 id와 이번 실패 id `summary` | 누출 false · 종료 코드 1 · 요청 줄 3개가 `요약(실제)`의 세 id가 아닌 다음 대상이고 모두 `실패: 호출 실패: …`(401, 키 없음) · 끝 줄 `대상 {t − 3} · 요청 3번 · 성공 0 · 실패 3 · 이미 요약 {k + 3}` → 앞 세 요약 그대로 · 실패 이슈 null(AC-04-3) |
| 화면 주소 | 브라우저로 `http://localhost:3000/` | 이슈맵이 뜨고 6개 섹션 · 갱신 시각 줄이 보임(AC-05-1) |
| 갱신 시각(화면) | 같은 화면의 갱신 시각 줄 | `{월}.{일} 오전/오후 {시}:{분} 수집 · 이슈 {n}개`, 시각이 `오늘 랭킹(DB)`의 `updatedAt`과 같음(06:00 고정 아님, AC-05-1) |
| 원문 새 탭 · 요약 | 목록 보기에서 카드의 `기사 원문` 클릭, `요약(실제)` 이슈 카드와 다른 카드의 요약 칸 · 이슈맵 타일을 눌러 연 상세 | 새 탭에서 그 카드 `url`(언론사 주소)이 열림(AC-04-2), `요약(실제)` 이슈는 목록 · 상세에 3줄(SQL과 같은 글), 나머지 카드는 "요약을 준비 중입니다"이고 연관기사 수 · 원문 링크는 그대로 보임(AC-04-1 · AC-04-3) |
| 화면 오늘(KST) | 브라우저 DevTools › Sensors › Location을 San Francisco(시간대 America/Los_Angeles)로 두고 새로고침 | 날짜 표시가 `GET /api/rankings`의 `date`와 같고 "· 오늘"이 붙음, 다음 날 버튼 비활성, 달력에서 KST 오늘 다음 날은 고를 수 없음(AC-09-4) |
| 화면 흐름 | `npm.cmd run api` 재시작 뒤 브라우저(<오늘> 카드): 카드 북마크 → 폴더 창에서 새 폴더 `화면확인` 만들기 → 새로고침 → 해제 → "되돌리기" → 북마크 화면 폴더 칩 | 폴더가 생기며 바로 저장(AC-10-2), 새로고침해도 활성 그대로, 되돌리기 후 다시 보임, 칩을 누르면 그 폴더만 · "전체"는 모두 · 칩마다 개수(AC-07-1 · AC-10-3) |
| 화면 지난 날짜 | 브라우저: 이전 날 버튼 → "오늘" 버튼 | 날짜 표시가 <어제>로 바뀌고 카드 또는 "해당 날짜의 랭킹 데이터가 없습니다"(그날 이슈가 없으면)가 보이며 "오늘" 버튼이 보임 → 누르면 <오늘> · "· 오늘" 표시 · 버튼 숨김(AC-09-2) |
| 중계 그대로 | `GET http://localhost:8787/api/rankings?date=<어제>`와 3000의 같은 요청 비교 | 상태 코드 · 본문 같음 |
| Hono 꺼짐 | api 서버를 끄고 `GET /api/rankings` | 502 "API 서버에 연결할 수 없습니다" |
| 원상복구(마지막) | SQL로 bookmarks 중 `2000-01-0`으로 시작하는 이슈를 가리키거나 `유지확인` · `화면확인` 폴더에 든 행 → 그 두 폴더 → `2000-01-0`으로 시작하는 issues → `date` `2000-01-04`이거나 `id > M`인 `collect_runs` → 링크가 `https://example.com/ex07-`로 시작하는 `articles`를 지우고 → 다섯 테이블 행 수 · <오늘> `articles` · `issues` · 요약 있는 issues · `ok` `collect_runs` 수 | 확인 행 0, <오늘> 실제 기사 · 이슈 · 요약 · ok 실행 기록은 남음(다음 단계가 씀), 샘플 이슈 · 폴더 0 그대로 |
