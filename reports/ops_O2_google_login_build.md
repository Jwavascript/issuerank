# ops O2 Google 로그인 — 제작 보고 (2026-10-18)

> 기준: `plan/PLAN_ops_O2_google_login.md`(확정, 끝 절 「문의 이메일 확정」 포함) · SPEC US-06 · AC-06-1~4 · US-07~11 · PLAN.md D9 · D10 · D21~D26 · `plan/PLAN_backend_ex10.md`(인증 규칙) · `plan/PLAN_backend_ex12.md`(BFF).
> 사용자 지시: "plan/PLAN_ops_O2_google_login.md대로 O2 Google 로그인을 ex12에서 만들기". 제작 역할(`agent_prompts/2_build_agent.md`).
> **커밋 · 푸시 안 함 → 운영(배포 · 하루 실행)에는 아직 반영되지 않음.** 푸시는 사용자 승인 뒤(9절).

## 1. 작업 범위

- 운영 폴더 `ex12/`에서만 고침(D21). ex01~ex11 · frontend · `web/` · `plan/PLAN_web.md` · `.github/workflows/daily.yml` 변경 0.
- 바뀐 파일 7: `ex12/server/auth.ts` · `ex12/.env.example` · `ex12/openapi.yaml` · `ex12/public/index.html` · `ex12/next.config.mjs` · `ex12/test/auth.test.ts` · `ex12/test/auth-proxy.test.ts`.
- 새 파일 3: `ex12/public/privacy.html` · `ex12/public/terms.html` · `ex12/test/google-ui.test.ts`.
- 문서: 이 보고(새) · `HANDOFF.md`(3절 지금 상태 · 4절 다음 작업에 O2 한 줄씩) · `PLAN.md` 10절 메모 1줄.
- 새 npm 패키지 0 · 마이그레이션 0 · DB 스키마 변경 0(`package.json` · `package-lock.json` · `server/db/*` · `drizzle/*` 해시 같음).
- 문의 이메일: 사용자가 공개를 승인한 `jwavascript@gmail.com`을 `/privacy`의 `#contact` 절과 `/terms`의 문의처 절에만 넣음. 메인 화면(`index.html`) · 로그인 창에는 이메일 0(링크만). 자리표시자 `[문의 이메일 준비 중]`은 쓰지 않음(0개).
- **`/privacy` · `/terms` 문안은 PLAN 항목대로 쓴 초안(법률 문안 아님)이다. Google 앱 게시 전 사용자 확인이 필요하고 법률 검토를 권장한다**(확인할 점은 11절 1).
- 검증 스크립트 · 결과 · 캡처: `ex12/.data/o2-build-20261018/`(git 무시 폴더, 12절).

## 2. PLAN 항목별 반영 위치

| PLAN 항목 | 반영 위치 |
|---|---|
| 켜는 조건(두 값이 다 있을 때만, 없으면 404 `PROVIDER_NOT_FOUND`, 앱 준비 실패로 만들지 않음) | `ex12/server/auth.ts:10` `googleProvider(env)` — `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` 중 하나라도 비면 `{}`. `:42` `socialProviders: googleProvider(env)`. `authOptions`의 기존 검사(secret · URL)는 그대로라 Google 값이 없어도 앱은 뜸 |
| Google 주소(scope 3개 고정 · PKCE · access_type/prompt/nonce 없음) | `auth.ts:15` `disableDefaultScope: true, scope: ['openid', 'email', 'profile']`, prompt · accessType · loginHint 설정 없음(라이브러리가 PKCE S256 · state 32자 · include_granted_scopes=true를 붙임). redirect_uri = `{BETTER_AUTH_URL}/api/auth/callback/google`(라이브러리, baseURL 그대로) |
| 상태 쿠키(cookie 방식 · 10분 · DB 쓰기 0) | `auth.ts:45` `account.storeStateStrategy: 'cookie'` → `better-auth.oauth_state`(https면 `__Secure-` · Secure), HttpOnly · SameSite=Lax · Path=/ · Max-Age=600 |
| 콜백 실패 주소(`onAPIError.errorURL: "/"`) | `auth.ts:41` `onAPIError: { throw: true, errorURL: '/' }`(기존 `throw: true` 유지) → 상태를 못 읽으면 `/?error=state_mismatch` |
| 저장 칸(name · image) | `auth.ts:17` `mapProfileToUser`: name = Google `name` 앞뒤 공백 뺀 값, 비면 이메일 `@` 앞부분 · `image: undefined`(사진 주소 저장 안 함 → DB NULL). email 소문자 · email_verified · account_id = sub는 라이브러리 |
| 토큰 저장 안 함(③) | `auth.ts:47` `account.updateAccountOnSignIn: false` + `:51` `databaseHooks.account.create.before`가 access/refresh/id 토큰 · 만료 2칸을 null로(모든 account 생성에 적용 — credential 행은 원래 이 칸이 비어 있어 차이 없음) |
| 계정 규칙(①② 같은 이메일 잇지 않음) | `auth.ts:49` `account.accountLinking.disableImplicitLinking: true` → `error=account_not_linked` · 새 행 0. `trustedProviders` 없음 |
| 닫는 경로 5개 · idToken 로그인 닫음 | `auth.ts:55` `disabledPaths: ['/link-social', '/unlink-account', '/get-access-token', '/refresh-token', '/account-info']`, `:16` `disableIdTokenSignIn: true` |
| 보안 · 로그 · 속도 그대로 | `trustedOrigins` · CSRF/Origin 검사 · `logger`(`AUTH_INTERNAL_ERROR` 한 줄) · 속도 제한(라이브러리 기본) 코드 변경 0 |
| BFF · 라우트 그대로 | `server/bff.ts` · `app/api/[...path]/route.ts` 변경 0(해시 같음). 302 · 상대 `Location` · 여러 `Set-Cookie` · no-store는 테스트(8)와 로컬 서버(12)로 확인 |
| `.env.example` 이름 2개 | `ex12/.env.example:16-19`(설명 2줄 + `GOOGLE_CLIENT_ID=` · `GOOGLE_CLIENT_SECRET=`, 값 없음) |
| `openapi.yaml` 경로 2개 · version 0.3.0 | `ex12/openapi.yaml:4` version 0.3.0, `:116` `POST /api/auth/sign-in/social`(요청 3칸 · 200 · 403 · 404), `:167` `GET /api/auth/callback/google`(302 · Location · Set-Cookie · error 코드). 다른 경로 변경 0 |
| 화면: "Google로 계속하기" 버튼 · "또는 이메일로" | `ex12/public/index.html:496` 폼 첫 요소 `#authGoogle`(type=button, 표준 4색 G 로고 SVG 20px + "Google로 계속하기"), `:497` `.auth-or` "또는 이메일로". CSS `:377` `.google-btn`(바탕 #FFFFFF · 테두리 1px #747775 · 글자 #1F1F1F · 높이 40 · 창 너비 가득 · 알약 모양), `:383` `.auth-or`. 두 모드 같은 버튼(`setAuthMode`는 손대지 않음) · 첫 초점 이메일 칸 그대로 |
| 버튼 동작(잠금 · 시작 · 이동 · 실패 문구 · bfcache) | `index.html:1095` `googleLeaving`, `:1096` `googleStartMessage`(404 `PROVIDER_NOT_FOUND` → "지금은 Google 로그인을 사용할 수 없습니다", 429 → "잠시 후 다시 시도해 주세요", 그 밖 → "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요"), `:1101` `googleSignIn()`(창의 버튼 4개 잠금 → `POST sign-in/social` 3칸 → `url`이 `https://accounts.google.com/`로 시작할 때만 `location.assign`, 잠금 유지 / 실패면 문구 · 잠금 풂), `:1118` 버튼 연결, `:1120` `pageshow`(persisted)면 잠금 풂 |
| 돌아올 주소(`/?login=google` + `&issue=` + 해시) | `index.html:1024` `authIssue` · `:1025` `openAuth(issue)`(문자열일 때만 기억, 그 밖 null), `:1139` 비로그인 북마크 토스트의 "로그인" = `() => openAuth(id)`. `googleSignIn`이 `'/?login=google' + (&issue={id}) + location.hash`를 callbackURL · errorCallbackURL 둘 다로 |
| 첫 로드 처리(쿼리 전체 지움 · 순서 · 문구) | `index.html:1183` `issueShortcut()`(이름 · 위젯 바로가기 동작 유지): issue · login · error 중 하나라도 있으면 즉시 `replaceState(pathname + hash)`, `{ issue, google, error }` 반환. `:1197` `googleReturn()`: 로그인 + `login=google` + error 없음 → 토스트 "로그인했습니다" / 비로그인 + error → `openAuth()`(로그인 모드) 뒤 `#authError` = access_denied "Google 로그인을 취소했습니다" · account_not_linked "이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요" · 그 밖 "Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요" / 그 밖 아무것도 안 함. `:1218` `start()`가 issue 처리 다음에 부름. error 값 자체는 화면에 쓰지 않음 |
| 정책 링크 2곳(⑤) | 로그인 창 맨 아래 `index.html:504` "이용약관 · 개인정보처리방침"(새 탭, `rel=noopener`), 메인 맨 아래 모든 화면 공통 `:485` `<footer class="site-foot">` "개인정보처리방침 · 이용약관 · 문의"(문의 → `/privacy#contact`), CSS `:284` |
| 휴대폰 로그아웃(⑨) | `index.html:476` 북마크 화면 제목 아래 `#bmUser`, `:997` `renderUser()`가 로그인 상태면 보이고 문구 `{이름} · 로그아웃`, `:1150` 누르면 기존 `signOut()`. CSS `:278` 기본 숨김, `:357` 640px 이하에서만 보임(641px 이상은 헤더 버튼 그대로) |
| `/privacy` · `/terms` 최소 페이지 | `ex12/public/privacy.html`(새, 113줄, 1~12절: ①수집 항목 ②목적 ③보관 ④Google 사용자 데이터 ⑤제3자 제공 없음 · 처리 위탁 ⑥삭제 요청 ⑦쿠키 ⑧권리 · 안전 조치 · 책임자 · `#contact` 문의처 · 시행일 · 변경 고지), `ex12/public/terms.html`(새, 99줄, 1~10절: 서비스 내용 · 계정 · 콘텐츠 · 금지 행위 · 변경 · 중단 · 책임 제한 · 이용 제한 · 개인정보 · 문의처 · 시행일). `widget.html`과 같은 꼴(사이트 토큰 · 다크 · 시스템 글꼴 · 외부 파일 0 · Web Analytics 2줄) |
| rewrite 2줄 | `ex12/next.config.mjs:5-6` `/privacy` → `/privacy.html`, `/terms` → `/terms.html` |
| 테스트 | `test/auth.test.ts`(+6) · `test/auth-proxy.test.ts`(+1) · 새 `test/google-ui.test.ts`(6) — 5절 |

## 3. 지운 것

- 제품 코드 삭제 없음. 바꾼 기존 줄: 비로그인 북마크 토스트의 동작 인수 `openAuth` → `() => openAuth(id)` 1줄, `issueShortcut()`의 첫 조건(issue만 → issue · login · error)과 반환 꼴(`{ id, date }` → `{ issue: { id, date } | null, google, error }`, 부르는 곳 `start()`만 맞춤), `openAuth()` → `openAuth(issue)`. 위젯 바로가기 동작은 그대로(`widget-ui` 테스트 8개 그대로 통과).
- 기존 테스트 기대값 수정 0. `test/auth.test.ts` 첫 테스트 본문을 `emailFlow(extra)`로 옮겨 "이메일만"(원래 이름 그대로) · "Google 켬"으로 두 번 실행(PLAN 7번). 지운 줄 6개는 import · setup 서명 · 이 이동에 따른 줄뿐, `expect` 0.
- 검증 중 만든 임시 파일(소스 백업 · HEAD openapi 사본 · 정지 파일 · 실수 넣기 출력) · Chromium 프로필 3개는 지움.

## 4. 필요했지만 없던 키 · 계정

- `ex12/.env`에 `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` **이름이 없음**(값은 읽거나 출력하지 않고 이름 존재만 확인). 로컬 검증은 가짜 값(`o2-local.apps.googleusercontent.com` · 가짜 secret, 테스트는 `o2-test…`)으로 함. 실제 Google 로그인은 사람만 가능 → 17번(사용자).
- Google Cloud 설정(동의 화면 · 웹 클라이언트 · 테스트 사용자)과 Vercel Production `GOOGLE_CLIENT_ID`(Config) · `GOOGLE_CLIENT_SECRET`(Secret)은 사용자가 마쳤다고 알려 옴(PLAN 끝 절, 2026-10-18) — **제작은 확인하지 않음**(값 · 화면을 보지 않음). 15 · 18번에서 확인.

## 5. 테스트

`bash scripts/ex12-ubuntu.sh npm test` → **17파일 155/155 통과**(기존 142 + 새 13), 마지막 실행 24.97s. vitest가 바꾼 추적 파일 0.

| 파일 | 새로 더한 것 |
|---|---|
| `test/auth.test.ts` (+6, 계 13) | 실제 Better Auth 1.7.7 + 메모리 어댑터 + 전역 fetch 가짜(토큰 주소 POST에만 답하고 그 밖은 실패). ⑦ 첫 이메일 흐름을 Google 켠 설정으로 한 번 더. **3 시작**: 200 · `{url, redirect:true}` 두 칸 · 주소 = `accounts.google.com/o/oauth2/v2/auth` · 칸 이름 8개 정확(access_type · prompt · login_hint · nonce · hd 없음) · scope `openid email profile` · redirect_uri 로컬 콜백 · state 32 · code_challenge 43 · 200에도 붙는 Location = url · 쿠키 `better-auth.oauth_state` 1개(암호화 값, Max-Age=600 · Path=/ · HttpOnly · SameSite=Lax · Secure 없음) · no-store · 4표 행 0 · fetch 0. **4 새 가입**: 302 `/?login=google#bookmarks` · 세션 쿠키(7일) · 상태 쿠키 만료 · no-store · 토큰 요청 1번(grant_type · code · redirect_uri · client_id · client_secret 본문 · code_verifier 128자, challenge = S256(verifier)) · user(email 소문자 · verified · 이름 trim · image 없음) · account(google · sub · password/토큰 5칸 null) · session 1 · 저장소에 토큰 · 사진 주소 · ID 토큰 0 · get-session 본인 · name 공백/없음 → 이메일 @ 앞부분. **5 계정 규칙**: ① 같은 sub(Google 쪽 이름 · 이메일 바꿈) → 새 행 0 · session +1 · 저장값 · account 행 그대로 ② 같은 이메일(대문자 섞음, Google 인증 true/false 둘 다) → `/?login=google&error=account_not_linked#bookmarks` · 새 행 · 세션 0 · credential 행 그대로 · 로그 1줄, **로컬 계정이 인증됐다고 가정해도 잇지 않음**(폐지 예정 기본값 requireLocalEmailVerified에 기대지 않음) ③ 미인증 새 이메일 → 가입 · email_verified false ④ Google 가입자 이메일 로그인 401 · 가입 422 · 새 행 0. **6 실패 · 보안**: ① 취소 `…error=access_denied#bookmarks` · fetch 0 · 로그 0줄 ② 쿠키 없음 `/?error=state_mismatch` · 1줄 ③ 다른 state ④ 11분 뒤(가짜 시계) `…error=state_mismatch#bookmarks` ⑤ 토큰 400 `…error=invalid_code#bookmarks` · 1줄 · 세션 0 ⑥ callbackURL · errorCallbackURL 다른 주소 403 `INVALID_CALLBACK_URL` · `INVALID_ERROR_CALLBACK_URL` · 쿠키 0 ⑦ 쿠키 POST + 다른 Origin 403 ⑧ 본문 idToken 404 `ID_TOKEN_NOT_SUPPORTED` · JWKS fetch 0 ⑨ 세션 쿠키로 닫은 경로 5개 모두 404 "Not Found"(대조 `list-accounts` 200) ⑩ env 없음 · ID만 · secret만 · 빈 문자열 → 404 `PROVIDER_NOT_FOUND` · 쿠키 0, 같은 설정 이메일 가입 · 로그인 200 ⑪ 모든 경우 콘솔 = `AUTH_INTERNAL_ERROR` · `API_INTERNAL_ERROR` 한 줄씩뿐, 가짜 secret · 토큰 · code · 이메일 · `@` 0 |
| `test/auth-proxy.test.ts` (+1, 계 8) | 가짜 앱이 `/api/auth/callback/google`에 302 · `Location: /?login=google#list` · Set-Cookie 2개(Expires 포함) → BFF 302 · Location 그대로 · `getSetCookie()` 2개 그대로 · no-store · 요청 Cookie 그대로 · 외부 fetch 0 |
| `test/google-ui.test.ts` (새 6) | `index.html`의 실제 로그인 창 · 문서 클릭 · 첫 로드 코드를 VM에서 가짜 fetch · location · history · toast · openAuth로. ① 헤더 · 북마크 탭 "로그인" · 비로그인 북마크 토스트 "로그인" × 해시 없음 · `#list` · `#bookmarks`(9경우): 창 열림 · 첫 초점 이메일 · 본문 3칸 정확(`/?login=google[&issue=…]` + 해시) · `credentials same-origin` · `cache no-store` · `location.assign` 1번 = url · 버튼 4개 잠김 유지 / 회원가입 모드도 같은 본문 · 요청 중 다시 눌러도 1번 ② 404 · 429 · 500 · 502 · 403 · 네트워크 · `evil.example` · `accounts.google.com.evil.example` · url 없음 → 표의 문구 · 이동 0 · 잠금 풀림 ③ 첫 로드 10경우(성공 · 해시 · 취소 · 연결 안 됨 · 이상한 값(`zzz<b>`) · 상태 실패 · `/?error=` 단독 · 세션 + error · 비로그인 + login만 · `login=other`): 쿼리 지움(해시 · 기록 수 그대로) · 토스트 / 로그인 창 + 문구 / 아무것도 안 함, error 값 · `<b>` 화면 0 / `?issue={그날 id}&login=google` + 세션 → 그날 랭킹 → 상세창 → 토스트 순서 ④ pageshow(persisted:false 그대로 · true 잠금 풂 · 다시 누르면 요청 2번째) |

실수 넣기(테스트가 잡는가, 끝나면 원본 복원 · 해시 확인): `server/auth.ts`에서 disableImplicitLinking · disabledPaths · cookie 상태 · updateAccountOnSignIn · 토큰 훅 · errorURL · disableIdTokenSignIn을 하나씩 빼면 **7가지 모두 실패**(각 1~3개), `index.html`에서 Google 주소 검사 · 토스트 issue · 쿼리 지움 조건 · pageshow 잠금 풂 · error 문구 대신 원래 값 넣기 **5가지 모두 실패**.

## 6. build 결과

- `bash scripts/ex12-ubuntu.sh npm run build`(next build Turbopack + `tsc --noEmit`) → **성공**(마지막 실행: Compiled · Finished TypeScript · static 2/2). 출력 누출 검사 false.
- 첫 빌드는 `tsc`가 테스트 파일의 타입 오류 1개로 실패(TS2345, 6⑩ 환경 변수 배열의 합집합 타입) → `Record<string, string>[]`로 고친 뒤 성공(vitest는 타입을 보지 않아 통과했었음).
- 빌드 산출물(`.next/static` · `.next/server`) · `public/` · 설정 파일 129개에서 `.env` 비밀값 · 가짜 Google secret 검색 → 0. `git check-ignore ex12/.env` 무시됨 · 추적 `.env` 0.

## 7. 검증 21줄

| # | 판정 | 근거 |
|---|---|---|
| 1 변경 범위 · 하루 실행 불변 | OK | 작업 전 `ex12` 추적 파일 + daily.yml 82개 SHA-256 기록 → 끝에 다시 셈: 바뀐 것 7개 = 1절 목록뿐(75/82 같음), 새 파일 3개 = 구현 순서 2~4 목록. PLAN이 지정한 하루 실행 · 보호 파일 `server/{ai,app,bff,daily,steps,collect,group,grouping,newsdata,summary,summarize,ranking}.ts` · `server/db/*.ts` · `app/api/[...path]/route.ts` · `public/widget.{js,html}` · `server/routes/widget.ts` · `package.json` · `package-lock.json` · `.github/workflows/daily.yml` **모두 같음**. `drizzle` · `drizzle/meta` 파일 수 19 그대로. ex01~ex11 · frontend · web · SPEC · plan/ · .github 변경 0. PLAN.md는 지시받은 10절 메모 1줄(문서)만. 작업 트리의 `HANDOFF.md` · `reports/ex09_daily.md` · `reports/ops_O1_widget_judge.md` 변경은 다른 세션(O1 17번 기록)의 것 — 보존 |
| 2 스키마 그대로 | OK | 공식 CLI `npx auth generate --config .data/o2-build-20261018/auth-generate.ts --adapter drizzle --dialect postgresql`(Google 켠 설정 · 가짜 값 · DB 접속 없음 · 출력 누출 false) 결과와 `server/db/auth-schema.ts`가 주석 · `.enableRLS()`만 빼고 **같음**(4표 user 7 · session 8 · account 13 · verification 6칸). 안전 실행 `npm run db:generate` → "No schema changes, nothing to migrate", 새 SQL · snapshot 0 |
| 3 시작(테스트, AC-06-3) | OK | `auth.test.ts` "3 시작"(5절) |
| 4 콜백 새 가입(테스트, AC-06-3) | OK | "4 콜백 새 가입"(5절) |
| 5 다시 로그인 · 계정 규칙(AC-06-2 · 06-3 · 06-4) | OK | "5 다시 로그인 · 계정 규칙"(5절) + 실수 넣기에서 disableImplicitLinking · updateAccountOnSignIn을 빼면 실패 |
| 6 실패 · 보안(AC-06-4) | OK | "6 실패 · 보안" ①~⑨⑪ · "6⑩"(5절). 참고: 깨진 상태 쿠키는 라이브러리가 `state_invalid`로 보냄(11절 5) |
| 7 이메일 로그인 회귀(AC-06-1 · 06-2) | OK | 기존 `auth.test.ts` 7 · `account-ui` 6 · `ownership` 5 그대로 통과(기대값 수정 0) + 첫 auth 테스트 Google 켬으로 한 번 더 |
| 8 BFF 302 | OK | `auth-proxy.test.ts` 새 1개(5절) |
| 9 화면(VM, AC-06-3 · 06-4 · 07-3) | OK | `google-ui.test.ts` ①~④(5절). 기존 `widget-ui` 8 · `account-ui` 6 그대로 통과 |
| 10 OpenAPI | OK | 캐시된 `@redocly/cli@2.64.1`(npx --offline, 텔레메트리 끔) `lint openapi.yaml` → **오류 0** · 경고 20. 같은 버전으로 HEAD 판을 재면 경고 18 → 새 경고 2개는 `GET /api/auth/callback/google`의 `operation-2xx-response` · `operation-4xx-response`(PLAN 표대로 302만 있는 경로라 생김). diff = version 줄 + 두 경로 추가뿐(지운 줄은 `version: 0.2.0` 하나), 다른 경로 차이 0 |
| 11 로컬 회귀 · 빌드 | OK | 155/155 · 빌드 · 타입 성공 · 새 패키지 0(5 · 6절) |
| 12 로컬 서버(next start, 안전 실행) | OK | `next start`(3001) + 요청을 세는 프록시(3000), 자식 env에 가짜 Google 값. Node fetch **20/20**: `POST sign-in/social`(Origin localhost:3000, `/?login=google`) 200 · no-store · 본문 `{url, redirect}` · 주소 `accounts.google.com/o/oauth2/v2/auth` · 칸 8개 · redirect_uri `http://localhost:3000/api/auth/callback/google` · scope 3개 · code · S256 · include_granted_scopes · **client_id = 자식 env의 가짜 값** · 쿠키 1개 `better-auth.oauth_state`(값 640자, HttpOnly · SameSite=Lax · Path=/ · Max-Age=600 · Secure 없음) → 그 쿠키로 `?state=…&error=access_denied` **302 `Location: /?login=google&error=access_denied`(상대 주소 그대로)** · no-store · 상태 쿠키 만료 · 세션 쿠키 0 → 쿠키 없이 `?state=x&code=y` 302 `/?error=state_mismatch` · no-store → 닫은 경로 `/account-info` 404 → `/api/rankings` 200(date 2026-10-18) · `/api/widget` 200(30개) · `/` 200(버튼 · 정책 링크 포함). 자식 env Google 값을 빈 문자열로 다시 띄움 **4/4**: `POST sign-in/social` 404 `PROVIDER_NOT_FOUND` · 쿠키 0 · no-store, 없는 이메일 `sign-in/email` 401 · 세션 쿠키 0. 서버 로그 = Next 시작 줄 + `AUTH_INTERNAL_ERROR` 1줄씩(상태 실패 · 제공자 없음)뿐, 누출 false. DB 9표 해시 전후 같음(14번). `/sign-in*` 같은 경로 10초 안 최대 2번. "Next가 자식 env 빈 문자열을 `.env` 값으로 덮지 않는가": `ex12/.env`에 GOOGLE 이름이 없어 이 실행만으로는 가를 수 없음 — 설치된 `@next/env`가 원래 process.env에 키가 있으면(빈 문자열 포함) `.env` 값을 쓰지 않는 코드(`typeof p[t]==="undefined"`)임을 확인, 가짜 client_id가 그대로 나온 것도 같은 규칙 |
| 13 로컬 화면(Chromium CDP, AC-06-3 · 06-4 · 07-3 · 11-2) | OK | Termux Chromium 155 headless + CDP **92/92**(2차 실행; 1차는 검증 스크립트 문제로 ③에서 멈춤 — 10절). Google 요청은 두 겹으로 막음: CDP `Fetch`로 Google 호스트 요청을 잡아 칸 이름 · 공개 값 · 길이만 기록하고 Aborted로 취소 + `--host-resolver-rules`로 Google 도메인 이름 풀이 NOTFOUND. ① 360(북마크 탭 "로그인" · 비로그인 북마크 토스트 "로그인") · 1280(헤더 "로그인"): 버튼 = 폼 첫 요소 · 제목 아래 20px · "Google로 계속하기" · G 로고 4색 20×20 · 바탕 rgb(255,255,255) · 테두리 rgb(116,119,117) 1px · 글자 rgb(31,31,31) · 높이 40 · 창 너비 가득(360: 274/274, 1280: 350/350, 창 400) · 아래 "또는 이메일로" → 입력 칸 · 기존 버튼 "로그인 · 회원가입 · 닫기" · 첫 초점 이메일 · 창 맨 아래 "이용약관 · 개인정보처리방침"(`_blank` · noopener) · 창 안 이메일 0 · 가로 넘침 0 ② 회원가입 모드: 같은 버튼 · 같은 크기 · 창 안 같은 자리(이름 칸이 생겨 창이 가운데로 다시 맞춰짐) ③ 360 · 1280 버튼 → 시작 요청 1번(200, 본문 3칸 `/?login=google#bookmarks`) → Google 이동 1번이 잡혀 취소됨 · 주소 칸 = 12번(가짜 client_id · state 32 · challenge 43) · 막은 직후 같은 문서에서 버튼 4개 잠김 · authBusy · googleLeaving · 그 요청 응답 0 · `net::ERR_ABORTED` ④ `/?issue={위젯 첫 줄}` 상세창 북마크(비로그인) → 토스트 "북마크하려면 로그인이 필요합니다" + "로그인" → 창 → 버튼: 본문 callbackURL = errorCallbackURL = `/?login=google&issue={그 id}` ⑤ 바로 연 탭 `/?login=google&error=access_denied#bookmarks` → 북마크 화면 + 로그인 창(로그인 모드) + "Google 로그인을 취소했습니다" · 초점 이메일 · search 빈 값 · 해시 `#bookmarks` · `history.length` 1 · error 값 화면 0 / `/?error=state_mismatch` → "Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요" · 기록 1 ⑥ 없는 이메일 틀린 로그인 → "이메일 또는 비밀번호를 확인해 주세요" · 401 · 비로그인 ⑦ 다크: 창 바탕 흰색 · 버튼 색 그대로(메인 바탕 rgb(18,18,17)) ⑧ `/?issue={위젯 items[0].id}` → 그 이슈 상세창 · 쿼리 지움 · 기록 1 · 오류 토스트 0(O1 13①과 같음) ⑨ `/privacy` · `/terms` 200 · 절 12개 · 10개 다 있음 · PLAN 항목 낱말 다 있음 · 이메일 = `jwavascript@gmail.com` 하나(mailto) · `#contact` 밖 `@` 0 · 자리표시자 0 · 360 가로 넘침 0 · Analytics 2줄 · 메인 하단 공통 한 줄 "개인정보처리방침 · 이용약관 · 문의"(이슈맵 · 목록 · 북마크 3화면, 문의 → `/privacy#contact`) · 메인 글 · HTML 이메일 0 ⑩ 임시 이메일 계정으로 360 가입 → "로그인했습니다" · 북마크 화면 제목 아래 "{이름} · 로그아웃"(inline-block, 헤더 버튼 숨김) → 누름 → "로그아웃했습니다" · 로그인 안내 / 1280 로그인 → 휴대폰 줄 display none · 헤더 "{이름} · 로그아웃" → 로그아웃. JS 예외 0. 외부 호스트 = cdn.jsdelivr.net(Pretendard 글꼴) · 막은 accounts.google.com 3번뿐. 캡처 15장(12절), 그중 5장 직접 열어 확인 |
| 14 안전 · 한도(로컬) | OK | 3000 요청 **48**(프록시 기록: Node 9 · 브라우저 39 — 1차 중단 실행 6 포함, 막힘 0 · 5xx 0) + 3001 직접 준비 확인 2(`/api/auth/ok`, 10절) = **50/100**. newsdata 0 · OpenAI 0. **Google로 실제 나간 요청 0**(브라우저 안 이동 시도 = 2차 3 + 1차 1 + 진단 2, 모두 CDP Fetch로 잡혀 취소, DNS도 차단 · 서버는 Google에 요청하지 않음 — 토큰 교환은 code가 있을 때만이고 로컬에서는 취소 · 상태 실패만 보냄). 누출 false(모든 출력 · 결과 · 서버 로그 · Chromium 로그). DB: 9표 건수 · 내용 md5 **전후 같음**(articles 937 · issues 616 · collect_runs 90 · bookmarks 1 · folders 1 · user 1 · session 1 · account 1 · verification 0). 쓰기는 13⑩ 임시 계정 1개(`o2-<uuid>@example.invalid`)뿐 → 정리에서 그 userId 행만 bookmarks 0 → folders 0 → session 0(로그아웃 2번으로 이미 지워짐) → account 1 → user 1 삭제, 남은 `o2-` 계정 0. 소유 프로세스 0: `curl` 3000 · 3001 · 9361 · 9362 = 000, `ps`에 next-server · next start · server.mjs · 내 Chromium 0(일치한 것은 확인 명령을 돌린 내 셸 하나뿐), Chromium은 띄우기 전 PID 목록과 비교해 새로 생긴 것 중 내 chrome의 자손 · 내 프로필 경로만 정리(남은 0), 프로필 3개 삭제. 사용자 code-oss(PID 17320) 살아 있음 |
| 15 배포 API(환경 변수 · 푸시 뒤) | SKIP — 푸시 후 | 커밋 · 푸시 전. 8절 절차 |
| 16 배포 화면(CDP 360) | SKIP — 푸시 후 | 8절 절차 |
| 17 실제 Google 로그인(사용자) | SKIP — 사용자 확인 대기(푸시 후) | 사람만 가능. 8절 절차, PASS 아님 |
| 18 Google 설정(사용자 → 대조) | SKIP — 사용자 전달 대기 | 8절 |
| 19 배포 로그(사용자, 선택) | SKIP — 사용자, 선택 | 8절 |
| 20 문의처 자리표시자 · 개인 이메일 | 저장소 부분 OK · **배포 부분 SKIP — 푸시 후** | `ex12/public/{privacy,terms,index}.html`: 자리표시자 0 · 0 · 0, 이메일 꼴 = privacy · terms 각각 `jwavascript@gmail.com` 하나(2번 = mailto + 글자, 모두 `#contact` 절 안) · index.html 0(나머지 `@`는 CSS `@media` · 글꼴 주소 `pretendard@v1.3.9`). 사용자가 알려 준 주소 밖의 이메일 0. 배포 `/privacy` · `/terms` · `/` 본문 셈은 푸시 뒤 |
| 21 정책 문안 확인(사용자) | SKIP — 사용자 문안 확인 대기 | 초안. 게시 금지 · PASS 아님. 확인할 점 11절 1 |

**합계: OK 14 · FAIL 0 · SKIP 7**(15~21, 20은 저장소 부분 OK). 사용자 줄(17 · 18 · 21)과 푸시 뒤 줄만 남음 → 계획 판정 규칙상 아직 PASS 아님(심사 · 배포 줄 · 사용자 줄 뒤 판정). 20번 저장소 부분에 자리표시자가 없어 "[보류 — 문의 이메일 대기]"는 해당 없음.

## 8. 배포 줄 · 사용자 줄 실행 절차(심사 · 사용자용)

전제: 사용자 커밋 · 푸시 승인(9절), Vercel Production에 `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET`이 푸시 **전**에 있어야 함(없으면 배포 직후 버튼이 "지금은 Google 로그인을 사용할 수 없습니다" → 넣고 Redeploy). 한도: 배포 요청 ≤ 40(브라우저 자동 요청 포함), newsdata · OpenAI 0, 에이전트의 Google 요청 0, DB 쓰기 0.

- **15 배포 API**(Node fetch, 약 7요청): `ex12/.data/o2-build-20261018/api.mjs`를 복사해 `BASE` · `ORIGIN` = `https://issuerank-kappa.vercel.app`, client_id 검사를 "가짜 값과 같음" → "`.apps.googleusercontent.com`으로 끝남(값은 적지 않음)"으로 바꿔 실행. 기대: `POST /api/auth/sign-in/social`(본문 `{provider:'google', callbackURL:'/?login=google', errorCallbackURL:'/?login=google'}`, Origin = 배포 주소) 200 · no-store · url 호스트 accounts.google.com · `redirect_uri` = `https://issuerank-kappa.vercel.app/api/auth/callback/google` 정확히 · scope 3개 · 쿠키 `__Secure-better-auth.oauth_state` Secure · HttpOnly · SameSite=Lax · Max-Age=600 → 그 쿠키로 `GET /api/auth/callback/google?state={state}&error=access_denied`(redirect: manual) 302 `Location: /?login=google&error=access_denied`(Vercel이 상대 주소를 그대로 두는지) · no-store → 쿠키 없이 `?state=x&code=y` 302 `/?error=state_mismatch` → `/api/auth/account-info` 404 → `/api/rankings` · `/api/widget` 200. 이 요청들은 Google에 아무것도 보내지 않음(서버는 code가 올 때만 토큰 교환).
- **16 배포 화면**(CDP 360, 약 15요청): `ui.mjs`의 탭 M(①③) · T5a(⑤)만 `ORIGIN` = 배포 주소로(Google 이동은 같은 방식으로 잡아 취소 · DNS 차단). 기대 = 로컬과 같음, redirect_uri = 배포 콜백, client_id는 접미사만.
- **20 배포 부분**(3요청): 배포 `/privacy` · `/terms` · `/` 본문에서 자리표시자 수 · 이메일 꼴 목록 → 0 · `jwavascript@gmail.com` 하나 · `/`는 0.
- **17 실제 Google 로그인(사용자 PC · 폰)**: 테스트 사용자로 등록한 Google 계정으로. ① PC(로그아웃 상태) 로그인 › Google로 계속하기 › 계정 선택 · 동의 → 누른 화면으로 돌아옴 · "로그인했습니다" · 헤더 이름 · 새로고침해도 유지 · 북마크 1개 ② 로그아웃 → 다시 Google → 같은 계정 · 같은 북마크 ③ 폰(Safari · Chrome) 같은 Google 계정 → PC와 같은 목록 · 칩 · 개수, 폰에서는 북마크 화면 제목 아래 "{이름} · 로그아웃"으로 로그아웃 → 다시 로그인 ④ Google 화면에서 취소 → "Google 로그인을 취소했습니다" · 비로그인 ⑤ (있으면) 이미 이메일로 가입한 주소의 Google 계정 → "이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요" · 그 이메일 계정 북마크 그대로 ⑥ (선택) 카카오톡 안 링크에서 버튼 · 테스트 사용자가 아닌 계정 → 결과 그대로 + 동의 화면에 보인 이름(도메인). 결과를 글로 전달(계정 · 이메일 주소는 적지 않아도 됨). 사용자 계정은 지우지 않음.
- **18 Google 설정**: Google Auth Platform의 대상(외부 · 게시 상태) · 데이터 액세스(범위) · 클라이언트(승인된 리디렉션 URI) · 브랜딩(링크)을 글이나 캡처로(비밀 빼고). 기대: 외부 · 범위 openid · userinfo.email · userinfo.profile 3개 · redirect URI = 배포 콜백(+ 로컬은 선택) · 20 · 21번 OK 전 게시 상태 = 테스트.
- **19(선택)**: 15 · 17번 시간대 Vercel Logs → 안전 코드만인지.
- **21 문안 확인(사용자)**: 배포된 `/privacy` · `/terms`(푸시 전이면 `ex12/public/privacy.html` · `terms.html` 파일)를 읽고 "확인" 또는 고칠 점(11절 1 목록 참고, 법률 검토 권장). 고치면 문안만 고친 뒤 20번 다시.
- 게시(PLAN 사용자 할 일 11): 20 · 21 OK 뒤 브랜딩 › 앱 도메인(홈 · `/privacy` · `/terms` · 승인된 도메인) → 대상 › 앱 게시 → 18번 다시.

## 9. 사용자 할 일

1. **푸시 시점 결정**: PLAN은 "푸시는 O1 17번 확인 뒤". O1 17번은 다른 세션 기록상 아직 [SKIP — PLAN 창 밖 확인](`reports/ops_O1_widget_judge.md` 13절, ①10-19 아침 재확인 또는 ②기록 인정 중 사용자 결정 대기).
2. 커밋 · 푸시 승인: 대상 = `ex12/` 10파일(1절) + `reports/ops_O2_google_login_build.md` · `HANDOFF.md` · `PLAN.md`(+ 다른 세션의 O1 · ex09 문서 변경을 함께 넣을지는 그 세션 기록과 맞춰 결정). 푸시하면 Vercel 재배포 · 다음 04:17 하루 실행이 이 코드를 씀(하루 실행 파일은 해시 불변 — 인증 변경은 collect · group · summarize에 닿지 않음: 그 셋은 `server/auth.ts`를 import하지 않음).
3. 푸시 뒤: 배포 줄 15 · 16 · 20(심사 또는 제작) → 17번 실기기 → 21번 문안 확인 → 게시 → 18번.
4. 로컬에서 실제 Google 로그인을 해 보려면 `ex12/.env`에 같은 두 이름을 넣을 수 있으나 **로컬과 배포는 같은 DB라 실제 계정이 생김**(PLAN 사용자 할 일 6).

## 10. 정직하게 남김(검증 중 문제)

- **1차 화면 검증이 ③에서 멈춤**(검증 스크립트 문제, 제품 아님): Fetch로 잡은 Google 이동을 실패시키기 **전에** 그 페이지의 잠금 상태를 `Runtime.evaluate`로 읽게 짰는데, 이동이 잡혀 있는 동안 그 페이지 평가는 답하지 않아 20초 대기 끝에 중단. 그 Google 이동은 잡힌 채로 브라우저를 닫아 **나가지 않았음**(+ DNS 차단). 로컬 요청 6 사용(누계 포함). 원인 확인용 진단 `diag-fetch.mjs`(로컬 서버 요청 0): about:blank 탭에서 `Page.navigate` · `location.assign`으로 `accounts.example.invalid` · `accounts.google.com` 각 2번 → 4번 모두 `requestWillBeSent → requestPaused → net::ERR_ABORTED`, 잡힌 동안 평가 1.5초 무응답, 취소 뒤 같은 문서(표시값 유지). → "먼저 취소, 막은 직후 같은 문서에서 읽기"로 고쳐 2차 92/92.
- 1차의 13② 비교식이 틀렸음(창은 가운데 정렬이라 이름 칸이 생기면 절대 y가 바뀜) → 창 안 상대 위치 비교로 고침.
- 서버 준비 확인으로 3001에 직접 2요청(`/api/auth/ok`, 프록시 카운터 밖) → 14번 누계에 더함(50/100).
- 첫 `npm run build`가 테스트 파일 타입 오류로 실패 → 고친 뒤 성공(6절).

## 11. 추가로 보인 점

1. **문안 확인 때 볼 점**(21번, 사용자 결정 · 법률 검토 권장): ① 보관 기간의 "관계 법령에 따라 보관해야 하는 정보" 문장 — 실제 의무가 있는지(PLAN "확인 필요") ② 처리 위탁 · 국외 이전 고지 형식 — Vercel Inc. · Supabase Inc.는 미국 회사이고 서버 지역은 서울로만 적음, 개인정보 보호법의 위탁 · 국외 이전 고지 항목 충족 여부 ③ Google 연결 해제 주소 `myaccount.google.com/connections`(PLAN "주소 확인 필요") ④ 개인정보 보호책임자 = "이슈랭크 운영자"(성명 · 직책을 적어야 하는지) ⑤ 시행일 2026-10-18(실제 게시일로 바꿀지) ⑥ 가입 동의 방식(지금은 로그인 창 아래 링크만 — 안내 문구인지 체크인지) ⑦ PLAN 항목 밖에서 문맥상 더한 문장: privacy 4항 "같은 이메일 계정은 자동으로 잇지 않음" · 6항 "탈퇴 버튼은 아직 없습니다" / terms 2항 "계정과 비밀번호는 본인이 관리" · 3항 "연관기사 수와 순위는 참고 지표" · 4항 "보안 장치를 피하려는 행위" · 8항 개인정보처리방침 링크 ⑧ (선택) Google API 서비스 사용자 데이터 정책(제한적 사용 요건) 준수 문장 — 기본 범위만이라 필수 아님(PLAN 근거 링크의 검증 안내), 넣지 않음.
2. 라이브러리의 `POST /sign-in/social`은 화면이 보내지 않는 칸(`scopes` · `additionalParams` · `loginHint` · `disableRedirect` · `requestSignUp` · `newUserCallbackURL` · `additionalData`)도 받음 → 누군가 자기 요청에 범위를 더할 수는 있으나 토큰을 저장하지 않아 앱에 남는 것은 없음. 막으려면 before 훅(PLAN 밖, 제안만).
3. `GET /list-accounts`(본인 연결 계정의 providerId · sub · 범위)는 PLAN의 닫는 목록에 없어 열려 있음(본인 정보만, 화면 미사용) — 닫을지 후보.
4. cookie 상태 방식에서는 로그아웃 응답이 `better-auth.oauth_state` 만료 쿠키도 함께 보냄(라이브러리 동작, 영향 없음).
5. 상태 쿠키가 **깨져** 있으면 라이브러리는 `/?error=state_invalid`(쿠키 **없음**은 `state_mismatch`) — 화면은 둘 다 일반 문구, openapi에 적음.
6. Google 버튼 글꼴: 지침은 Roboto Medium이지만 Google Fonts(구글로 가는 요청)를 피하려고 사이트 글꼴 굵기 500. 색 · 로고 · 크기 · 테두리는 지침대로.
7. 360px 헤더 왼쪽 브랜드 마크가 안 보임 — `.brand span { display: none }`이 `span.brand-mark`까지 숨김. O2 전부터(O1 심사 캡처도 같음), 고치지 않음.
8. 로그인 창을 닫아도 이메일 칸 값은 남음(비밀번호만 비움, 기존 동작).
9. 속도 제한은 라이브러리 기본(메모리 · 인스턴스마다)이라 Vercel처럼 인스턴스가 여럿이면 실제 한도는 느슨할 수 있음(기존 이메일 로그인과 같음).
10. Google 앱이 "테스트" 상태인 동안은 테스트 사용자만 로그인 가능 — 그 밖의 계정은 Google 화면에서 막힘(17⑥에서 기록). 인앱 브라우저(카카오톡 등) 차단 여부도 17⑥에서 보고 문제가 확인되면 O3(PLAN).

## 12. 증거 파일(`ex12/.data/o2-build-20261018/`, git 무시)

- 기준 · 비교: `hashes-before.txt`(작업 전 82개) · `hash-changed.txt` · `ps-before*.txt` · `ps-mine-server-*.txt`
- 테스트 · 빌드 출력: `npm-test-1.txt` · `npm-test-2.txt` · `npm-test-final.txt` · `npm-build.txt` · `npm-build-final.txt` · `tsc.txt` · `redocly.txt` · `redocly-head.txt` · `db-generate.txt` · `auth-generate.txt` · `auth-generate.ts` · `auth-schema-generated.ts`(모두 누출 검사 false)
- 스크립트: `o2lib.mjs`(누출 검사 · `.env` · 가짜 Google 값) · `server.mjs`(next start + 세는 프록시) · `api.mjs`(12) · `ui.mjs`(13) · `diag-fetch.mjs`(10절 진단) · `db.mjs`(스냅숏 · 임시 계정 정리)
- 결과: `api-fake.json` · `api-off.json` · `ui-result.json` · `ui-run.txt`(1차) · `ui-run2.txt`(2차) · `server-state-{fake,off}.json` · `server-log-{fake,off}.txt` · `request-count.json` · `request-log.json`(경로만, 쿼리 없음) · `db-snapshot-{before,after}.json` · `temp-account.json`(지운 임시 계정의 이메일 · id, 비밀번호 없음) · `widget-first.json`
- 캡처 15장: `o2-360-dialog-login` · `o2-360-dialog-signup` · `o2-360-dialog-dark` · `o2-360-issue-shortcut` · `o2-360-return-cancel` · `o2-360-return-generic` · `o2-360-bookmarks-logout` · `o2-360-bookmarks-after-logout` · `o2-1280-dialog-login` · `o2-1280-bookmarks` · `o2-360-privacy-top` · `o2-360-privacy-contact` · `o2-360-privacy-contact-dark` · `o2-360-terms-top` · `o2-360-terms-contact`(.png). 직접 연 것: dialog-login · return-cancel · bookmarks-logout · 1280-dialog-login · privacy-contact · privacy-contact-dark
