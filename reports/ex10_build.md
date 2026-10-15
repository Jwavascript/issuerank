# ex10 제작 보고 — 이메일·비밀번호 로그인

2026-10-14. 출발점은 보존한 ex08이며 사용자 승인으로 ex08 AI 품질 FAIL·ex09 자동화를 보류하고 ex10을 먼저 구현했다. 계획: `plan/PLAN_backend_ex10.md`.

## 작업 범위와 반영 위치

| 계획 | 반영 |
|---|---|
| Better Auth 이메일 가입·로그인·세션·로그아웃 | `ex10/server/auth.ts`, `server/index.ts`, `server/app.ts`. 이메일·비밀번호만 사용, 비밀번호8~128자·공백 이름 거절, 가입 자동로그인, 신뢰 Origin 제한, Origin/CSRF 검사 명시적으로 활성화 |
| DB 인증 스키마 | 공식 `auth generate`로 `server/db/auth-schema.ts` 별도 생성. 기존 `schema.ts`에서 export. `drizzle/0005_auth.sql` 및 snapshot/journal 추가. user/session/account/verification4테이블·고유값·FK·인덱스, 기존 Supabase 패턴대로 신규 테이블 RLS 활성화 |
| 안전한 로그·초기화 | DB/auth 생성은 진입점 try 안. Hono 오류는 `API_INTERNAL_ERROR`, 시작 실패는 `API_START_FAILED`. Better Auth logger는 `AUTH_INTERNAL_ERROR`, onAPIError.throw로 better-call의 원본 console.error 경로를 우회하여 Hono에 위임. Postgres notice 출력도 차단 |
| Next BFF | `app/api/[...path]/route.ts`: 요청 Cookie/Origin/본문, 응답 상태/본문 보존. `getSetCookie()`로 복수 쿠키 각각 전달(Expires의 쉼표 분할 없음). 인증 응답과502 모두 no-store |
| 실제 화면 | `public/index.html`: 초기 비로그인→세션 복원, 가입/로그인 모달·필드 label·요청 중 중복 제출 방지·한국어 오류·실제 이름 textContent·로그아웃. 성공 전 로그인 상태 변경 없음. 초기 복원/목록 로드 경합 가드, 로그아웃 뒤 목록 비움, 남은 되돌리기/저장 동작도 로그인 확인 |
| 계약·환경 | `openapi.yaml` 인증4경로/쿠키/입력·오류 계약, `.env.example` 인증변수2개. 로컬 `.env`는 부모가 준비했고 출력/커밋 안 함 |
| 검증 | `test/auth.test.ts` 실제 라이브러리+메모리 어댑터7개, `test/auth-proxy.test.ts` BFF2개. 기존76개 테스트 유지 |

설치 버전: `better-auth@1.7.7`, `@better-auth/drizzle-adapter@1.7.7`, 개발용 `auth@1.7.7`, 모두 정확 버전·lockfile 기록. 기존 패키지를 임의로 올리지 않았다. npm은 기존 esbuild 설치 스크립트 allow-scripts 경고를 냈으나 권한을 완화하지 않고 테스트·빌드가 성공했다.

## 삭제·준비·범위 경계

- 삭제한 파일/기존 데이터 없음. 요청 범위인 화면의 데모 로그인 토글만 실제 인증으로 교체했다. ex08·ex09·web/는 수정하지 않았다.
- OAuth/SMTP 계정·키는 필요 없음. DB와 새 인증 비밀값·앱 origin은 준비됨.
- 북마크/폴더 서버 API는 여전히 공용이다. 계정별 저장과 미로그인401은 ex11이며 이번 인증 완료를 개인 데이터 격리 완료로 보지 않는다.
- Google 등 소셜 로그인, 이메일 인증/비밀번호 재설정, AI 개선, 자동 실행, 배포는 이번 범위 밖이다.

## 실행한 명령과 현재 결과

모든 패키지 명령은 저장소 루트의 `bash scripts/ex10-ubuntu.sh ...`로 Ubuntu Node24.18.0에서 실행했다.

| 명령 | 결과 |
|---|---|
| `npm ci --no-audit --no-fund` | 부모 실행 완료 |
| `npm view better-auth version`, 어댑터·auth 동일 조회 | 모두1.7.7 |
| `npm install --save-exact better-auth@1.7.7 @better-auth/drizzle-adapter@1.7.7 --no-audit --no-fund` | 성공 |
| `npm install --save-dev --save-exact auth@1.7.7 --no-audit --no-fund` | 성공 |
| `node .data/safe-run.mjs npx auth generate --config .data/auth-generate.ts --adapter drizzle --dialect postgresql --output server/db/auth-schema.ts --yes` | 공식 스키마 생성, DB접속 없이 실행, leak:false |
| `node .data/safe-run.mjs npm run db:generate -- --name auth` | 0005만 생성, leak:false |
| `npm test` | 최종11파일85/85 PASS. 기존76+인증7+BFF2 |
| `node .data/safe-run.mjs npm run build` | Postgres notice 차단을 포함한 최종 코드의 Next16.4 Turbopack·tsc 성공, leak:false |
| `git diff --check` | 오류0 |
| 인라인 화면 스크립트 `new Function` 구문 확인 | PASS(실제 화면 조작 아님) |
| 설치된 Python YAML 파서로 OpenAPI 중복 키·내부 `$ref` 확인 | PASS, 인증4경로. 처음 Node yaml 모듈이 없어 Python 기존 설치를 사용했으며 새 패키지 추가 없음 |
| `git check-ignore ex10/.env ex10/.data/build-auth-verdict.json` | 둘 다 무시됨 |
| ex08 시작시 SHA-256 baseline 비교 | 변경0 |
| ex08/ex10 lockfile 공통 패키지 버전 비교 | 변경0 |

처음 인증 테스트에서는 라이브러리가 테스트 환경에서 Origin 검사를 기본 생략하는 것을 발견했다. 제품 옵션에 검사를 명시적으로 활성화했고 악성 Origin403을 확인했다. 추가 예외 테스트에서 better-call의 직접 오류 출력 경로를 발견해 Hono 안전 오류 처리로 위임했다. 진입점 테스트의 동기 자식 실행은 proot에서 지연되어 비동기로 변경 후 안전 종료를 확인했다.

## 실제 DB·BFF 검증

- `.data/safe-run.mjs`는 env의 DB URL/암호(인코딩·디코딩), API 키, 인증 비밀값을 검사한 뒤 stdout/stderr를 공개한다. `.data/verify-auth.ts`는 생성 비밀번호·쿠키·세션 토큰까지 자식 서버 로그에서 검사한다. 원문 응답/쿠키/오류 스택을 보고하지 않는다.
- 0000~0004 hash·시각을 로컬 파일과 대조했다. 0005 auth4테이블만 적용, 재실행 변경0, RLS4개 확인. 기존 업무 데이터 내용 해시 동일: 기사285, 이슈102, 북마크0, 폴더0, 수집기록54.
- 검증 스크립트 초기 CJS top-level-await는 서비스 호출 전 실패했고 main으로 수정했다. SQL 사전 검사가 FK의 ON DELETE cascade를 데이터 삭제로 오검출해0요청으로 중단했으며 statement 허용목록으로 수정했다. 이 실패들은 제품 인증 실패와 구분한다.
- 최종 실제 인증 검증 **PASS**: `bash scripts/ex10-ubuntu.sh node .data/safe-run.mjs npx tsx .data/verify-auth.ts build`. 마지막 시나리오18요청·임시계정1개, 앞선 실패/불명 실행을 보수적으로 합산한 제작 역할 누계20/30요청. 뉴스0·OpenAI0. 증거 `ex10/.data/build-auth-verdict.json`.
- Next 기본 시작은 Termux/proot의 `os.networkInterfaces()`가 `ERR_SYSTEM_ERROR`/`uv_interface_addresses`를 내면서 준비되지 않았다. 이 때문에 초기 가입 요청1회에서 성공 응답을 확인하지 못했다. 별도 준비 진단은 API0회였고, 서버에 `--hostname 127.0.0.1`을 지정해 LAN 주소 자동탐지를 피한 뒤 정상 작동했다. 권한 설정이나 라이브러리를 바꾸지 않았다. 앱 origin·실제 fetch는 `http://localhost:3000`을 유지했다.
- 검증 성공: 가입200·비밀번호해시·HttpOnly/SameSite쿠키·BFF no-store·가입 직후 세션·Hono 서버 재시작 후 세션 유지. 로그아웃200/쿠키만료·이전쿠키null, 틀린 비번/없는 이메일401·세션0, 중복422·짧은비번400, 정상로그인200, 변조/만료쿠키null, 악성Origin403·세션0.
- finally에서 이번 생성 계정의 session/account/user만 정리했다. 임시계정0·이전 인증 데이터와 업무 데이터 내용 해시 불변·소유 서버 모두 종료. 환경 비밀값과 생성한 비밀번호/세션/쿠키까지 검사해 leak:false.
- 심사 역할은 같은 스크립트에 `judge` 인수를 사용하면 독립 UUID계정과 역할별30회 제한으로 재검증한다. `judge-auth-verdict.json`과 `judge-request-count.json`으로 별도 기록한다.
- 브라우저 도구·Chromium이 없어 실제 UI 조작/모바일 넘침/새로고침 화면/기존 화면 회귀는 SKIP. API 검증으로 UI PASS를 대신하지 않는다. 따라서 전체 ex10 PASS는 보류한다.

공식 참고: [설치](https://better-auth.com/docs/installation), [Drizzle](https://better-auth.com/docs/adapters/drizzle), [CLI](https://better-auth.com/docs/concepts/cli), [이메일·비밀번호](https://better-auth.com/docs/authentication/email-password), [Hono](https://better-auth.com/docs/integrations/hono).
