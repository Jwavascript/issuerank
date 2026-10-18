# ops O2 Google 로그인 — 독립 심사 (2026-10-18)

> 역할: `agent_prompts/3_judge_agent.md`(제작과 다른 독립 심사, 제품 코드 수정 0 · 측정용 임시 교체 0). 심사 시각 2026-10-18 14:05~14:50 KST, Termux + proot Ubuntu(`scripts/ex12-ubuntu.sh`), Termux Chromium 155.
> 대상: `ex12/`의 미커밋 변경(바뀐 7 + 새 3). 기준: `plan/PLAN_ops_O2_google_login.md`(확정, 끝 절 「문의 이메일 확정」 포함) · SPEC US-06 · AC-06-1~4 · 회귀 AC-07-3 · 08-2 · 08-3 · 11-2 · PLAN.md D9 · D10 · D21~D26 · `plan/PLAN_backend_ex10.md`. 제작 보고 `reports/ops_O2_google_login_build.md`는 참고만 하고 모든 줄을 **심사가 따로 만든 스크립트로 다시 실행**(제작 스크립트 `ex12/.data/o2-build-20261018/` 재사용 0).
> **판정: PASS 보류** — 로컬로 확인할 수 있는 줄(1~14)과 20번 저장소 부분은 모두 OK, FAIL 0, 위반 0. 남은 것은 푸시 뒤 배포 줄(15 · 16 · 20 배포 부분)과 사용자 줄(17 · 18 · 21), 19(선택). 합계 **OK 14 · FAIL 0 · SKIP 7**.

## 1. PLAN 항목별

| PLAN 항목 | 판정 | 심사 근거 |
|---|---|---|
| 켜는 조건(두 env가 다 있을 때만, 끄면 404 `PROVIDER_NOT_FOUND`, 앱 준비 실패 아님) | done | `server/auth.ts` `googleProvider(env)`. 독립 테스트: env 없음 · ID만 · secret만 · 빈 문자열 4경우 모두 404 `PROVIDER_NOT_FOUND` · 쿠키 0, 같은 설정에서 이메일 가입 · 로그인 200. 로컬 서버를 빈 값으로 다시 띄워도 404 · `/api/rankings` 200 |
| 시작 `POST /api/auth/sign-in/social`(본문 3칸 → 200 `{url, redirect:true}` · 상태 쿠키 1 · no-store) | done | 독립 테스트 · 로컬 서버 · 브라우저 3번 모두: 본문 칸 `redirect,url` 두 개, 200에도 `Location` = url(fetch는 따라가지 않음), no-store. 화면이 보내는 본문은 정확히 `provider · callbackURL · errorCallbackURL` 3칸 |
| Google 주소(scope 3개 고정 · PKCE · access_type/prompt/nonce 없음 · redirect_uri) | done | 칸 이름 정확히 8개(`client_id · code_challenge · code_challenge_method · include_granted_scopes · redirect_uri · response_type · scope · state`), `scope=openid email profile`, `state` 32자 · `code_challenge` 43자 · S256, `access_type · prompt · login_hint · nonce · hd` 0. redirect_uri 로컬 `http://localhost:3000/api/auth/callback/google`, https 설정(배포 주소)으로 바꾸면 `https://issuerank-kappa.vercel.app/api/auth/callback/google`(독립 테스트 8, 15번 예측) |
| 상태 쿠키(cookie 방식 · 10분 · DB 쓰기 0 · 불일치 거절 · 끝나면 만료) | done | `better-auth.oauth_state` 1개 · HttpOnly · SameSite=Lax · Path=/ · Max-Age=600 · Secure 없음(로컬), https면 `__Secure-` 접두 · Secure. 값은 암호화(평문 state · 돌아올 주소 없음, 길이 640~680). 시작 뒤 메모리 DB 4표 0행(verification 0), 실제 DB도 전후 같음(8절). 다른 state · 11분 뒤 → `state_mismatch`, 취소 · 성공 응답에서 상태 쿠키 만료 |
| 콜백(토큰 교환 · ID 토큰 · 성공 302 · 실패 `error=` · 상태 못 읽음 `/?error=state_mismatch` · no-store) | done | 가짜 Google 토큰 주소에서: 토큰 요청 1번(`grant_type · code · redirect_uri` 같음 · `code_verifier` 128자 · S256 = 시작 주소의 challenge · client_secret_post), 성공 302 `Location` = callbackURL + 세션 쿠키(HttpOnly · Lax · 7일) + 상태 쿠키 만료. 실패는 errorCallbackURL 뒤 해시 앞에 `error=`(예 `/?login=google&error=access_denied#bookmarks`). 쿠키 없음 `/?error=state_mismatch` · 깨진 쿠키 `/?error=state_invalid` · state 칸 없음 `/?error=state_not_found`(화면은 모두 일반 문구) |
| 저장 칸(name · email 소문자 · email_verified · image null · account · 토큰 null) | done | user: email 소문자 · emailVerified = Google 값 · name 앞뒤 공백 제거(공백만 · 없음 → 이메일 `@` 앞부분) · image null. account: `providerId google` · `accountId` = sub · password null · scope = 라이브러리 값 · 토큰 3칸 + 만료 2칸 null. 저장소 전체에 가짜 access/refresh/ID 토큰 · 사진 주소 · secret 0 |
| 계정 규칙 ①②④(D26) | done | 아래 2절 AC-06-3 · 06-4 |
| 닫는 경로 5개 · idToken 로그인 닫음 · trustedProviders 없음 | done | 세션 쿠키를 들고도 `/link-social · /unlink-account · /get-access-token · /refresh-token · /account-info` 모두 404. 우회 변형(끝 `/` · `//` · 대문자 · `%2D` · 쿼리 · `%2F`) 11개도 모두 404(Next 경유 `//`는 308). 본문 `idToken` → 404 `ID_TOKEN_NOT_SUPPORTED` · JWKS fetch 0. `auth.ts`에 `trustedProviders` 없음 |
| 보안 그대로(trustedOrigins 1 · CSRF/Origin) | done | 다른 주소 callbackURL · errorCallbackURL · newUserCallbackURL(`https://evil.example/` · `//evil.example/` · `/\evil.example` · `http://localhost:3000.evil.example/` · `javascript:`) → 403 해당 코드 · 쿠키 0. 쿠키 있는 POST + 다른 Origin → 403 `INVALID_ORIGIN`(쿠키 있고 Origin 없음 → 403 `MISSING_OR_NULL_ORIGIN`). 쿠키 없는 다른 Origin 시작은 200 — PLAN이 기록만 하기로 한 라이브러리 동작 |
| 로그 · 속도 그대로 | done | 콘솔 = `AUTH_INTERNAL_ERROR` 한 줄씩뿐(독립 테스트 28줄, 로컬 서버 API 실행 5줄 = 상태 실패 2 · 다른 주소 · idToken · Origin 각 1, 취소 0줄, 꺼짐 서버 1줄). 가짜 secret · 토큰 · code · 이메일 · `@` 0. 속도 제한 설정 변경 0(`rateLimit` 옵션 없음, 라이브러리 기본) |
| BFF · 계약(`bff.ts` · route 그대로 · 302 · Location · 여러 Set-Cookie · no-store) | done | 두 파일 SHA-256 = HEAD. 독립 테스트는 실제 `createBff → createApp → auth.handler` 경로로 실행: 302 상대 Location 그대로 · Set-Cookie 2개(세션 + 상태 만료) 그대로. 로컬 Next도 `Location: /?login=google&error=access_denied` 상대 주소 그대로 |
| `.env.example` 이름 2개 | done | `GOOGLE_CLIENT_ID=` · `GOOGLE_CLIENT_SECRET=`(값 없음) + 설명 2줄. `ex12/.env`에는 GOOGLE 이름 없음(이름만 확인) |
| `openapi.yaml` 경로 2개 · 0.3.0 | done | 4절 |
| 화면: "Google로 계속하기" · "또는 이메일로" · 기존 칸 · 두 모드 · 첫 초점 | done | 5.4 ①② |
| 화면: 누르면 잠금 · 시작 · Google 주소만 이동 · 실패 문구 · bfcache 풀기 | done | 5.4 ③ · 모의 8가지 · 실제 Chromium bfcache 복원(pageshow persisted) 뒤 잠금 풀림 |
| 화면: 돌아올 주소 `/?login=google` + `&issue=` + 해시 | done | 실제 본문 `/?login=google#map`(1280, 화면 전환 뒤 해시) · `/?login=google#bookmarks`(360 북마크 탭) · `/?login=google&issue=2026-10-18-pol-1`(비로그인 북마크 토스트) |
| 화면: 첫 로드 처리(쿼리 지움 · 순서 · 문구 · error 값 화면 0) | done | 5.4 ⑤ · `/?login=google` + 세션 → 토스트 "로그인했습니다" |
| 정책 링크 2곳(⑤) | done | 메인 하단 공통 한 줄(이슈맵 · 목록 · 북마크 세 화면) · 로그인 창 맨 아래(새 탭 · noopener) |
| 휴대폰 로그아웃(⑨) | done | 같은 `@media (max-width: 640px)` 블록에서 `#userBtn` 숨김 ↔ `.bm-user` 보임(빈틈 없음). 360 제목 아래 "{이름} · 로그아웃" → 로그아웃, 1280은 헤더 버튼만 |
| `/privacy` · `/terms` 최소 페이지(항목 · 문의처 위치) | done(문안은 초안 — 21번 사용자 확인 대기) | 5.4 ⑨ · 6절 |
| `next.config.mjs` rewrite 2줄 | done | `/privacy → /privacy.html` · `/terms → /terms.html`, 로컬 서버 200 · text/html |
| 새 패키지 0 · 스키마 · 마이그레이션 0 | done | 3절 |
| 테스트 3파일(`auth` +6 · `auth-proxy` +1 · 새 `google-ui` 6) | done | 4절 |
| 하지 않음 목록(카카오 · link-social · 비밀번호 설정 · 메일 · 탈퇴 버튼 · One Tap · 추가 범위 · 위젯 · 하루 실행 · bff · 라우트 · web) | 지켜짐 | 바뀐 파일 목록 · 보호 파일 해시(3절). Google 가입자에게 비밀번호를 붙이는 경로 없음: `set-password` 404 · `change-password` 400 · `request-password-reset` 400 · credential 행 0 |

## 2. SPEC AC

| AC | 판정 | 근거 |
|---|---|---|
| AC-06-3 (Google로 가입 · 로그인 뒤 누른 화면으로) | 로컬 범위 충족(실제 Google은 17번) | 독립 테스트: 처음 보는 sub → user 1 · account 1 · session 1 · 302 callbackURL · get-session 본인. 같은 sub 재로그인(Google 쪽 이름 · 이메일 바꿈) → 새 행 0 · session +1 · 저장값 · account 행 그대로(updatedAt까지). 미인증 새 이메일 → 가입(email_verified false). 화면: 버튼 → 본문 · Google 이동 · 돌아올 주소가 누른 화면(해시 · issue) |
| AC-06-4 (같은 이메일 · 취소 · 실패 → 잇지 않음 · 안내 · 비로그인) | 로컬 범위 충족 | 같은 이메일(대문자 섞음) × Google 인증 true/false → `/?login=google&error=account_not_linked#bookmarks` · 세션 쿠키 0 · **DB 그대로**(credential 행 포함). 로컬 계정을 인증됨으로 바꿔도 잇지 않음(폐지 예정 기본값에 기대지 않음). 다른 sub가 Google 가입자의 이메일을 대도 잇지 않음. 취소 · state 불일치 · 쿠키 없음 · 깨짐 · 만료 · PKCE 불일치(코드 주입) · 토큰 400 · ID 토큰 없음 · 이메일 없음 → 모두 세션 0. 화면: 돌아온 주소마다 로그인 창 + 표의 문구(취소 · 연결 안 됨 · 그 밖) |
| AC-06-1 · 06-2 (이메일 가입 · 오류, 회귀) | 충족 | 기존 테스트 그대로 통과 + Google 켠 설정의 이메일 흐름(독립 9) + 브라우저: 360 가입 200 · "로그인했습니다", 없는 이메일 401 · "이메일 또는 비밀번호를 확인해 주세요" · 비로그인 유지, 1280 정상 로그인 200 |
| AC-07-3 (비로그인 북마크 → 안내 · 저장 0) | 충족 | 상세창 북마크 → 토스트 "북마크하려면 로그인이 필요합니다" + "로그인", `POST /api/bookmarks` 0 |
| AC-08-2 · 08-3 (빈 목록 · 계정별 북마크) | 08-2 충족 · 08-3은 회귀만(실기기는 17번) | 새 임시 계정 로그인 → "북마크한 기사가 없습니다"(DB의 다른 계정 북마크 1개가 보이지 않음), `ownership.test.ts` 5/5 |
| AC-11-2 (위젯 바로가기) | 충족 | `/?issue=2026-10-18-pol-1`(배포와 같은 DB의 위젯 첫 줄) → 그 이슈 상세창 · 쿼리 지움 · 기록 수 그대로 · 오류 토스트 0, `widget-ui` 8/8 |

## 3. 범위 · 보호 파일 · 새 패키지 · 마이그레이션

- `git status`: ex12 바뀐 7(`server/auth.ts` · `.env.example` · `openapi.yaml` · `public/index.html` · `next.config.mjs` · `test/auth.test.ts` · `test/auth-proxy.test.ts`) + 새 3(`public/privacy.html` · `public/terms.html` · `test/google-ui.test.ts`) = PLAN 구현 순서 2~4 목록과 같음. 문서: 제작 보고(새) · HANDOFF · PLAN.md 10절 메모 1줄. `reports/ex09_daily.md` · `reports/ops_O1_widget_judge.md` · HANDOFF의 O1 부분은 다른 세션(O1 17번) 기록 — 보존.
- **보호 49파일 SHA-256 = HEAD**(`git show HEAD:… | sha256sum`과 비교): `server/bff.ts` · `app/api/[...path]/route.ts` · `server/db/*` 4 · `drizzle/*` 15 · `package.json` · `package-lock.json` · 위젯 `server/routes/widget.ts` · `public/widget.{js,html}` · 하루 실행 `server/{daily,steps,collect,group,grouping,newsdata,summary,summarize,ai}.ts` · `server/{app,ranking,index,hide,schemas,sample}.ts` · `server/routes/{bookmarks,folders,rankings}.ts` · `vercel.json` · `tsconfig.json` · `vitest.config.ts` · `drizzle.config.ts` · `.github/workflows/daily.yml`.
- ex01~ex11 · frontend · `web/` · `.github` · SPEC · `plan/` 변경 0(`web/`은 status만 보고 내용은 읽지 않음). 새 패키지 0(package 두 파일 = HEAD).
- 스키마: 공식 CLI `npx --offline auth generate`(Google 켠 실제 `authOptions` + 가짜 값, `--adapter drizzle --dialect postgresql`, DB 접속 없음, 심사 전용 설정 파일) 결과 = `server/db/auth-schema.ts`(주석 · `.enableRLS()` 빼고 차이 0, user 7 · session 8 · account 13 · verification 6칸). 안전 실행 `npm run db:generate` → "No schema changes, nothing to migrate", `drizzle/` status 0.
- 참고: 검증 1번 기대 "PLAN.md 변경 0"과 AGENTS.md "단계 문제는 PLAN.md 10절에 기록"이 겹친다. 바뀐 것은 10절 메모 1줄(문서)뿐이라 범위 위반으로 보지 않음.
- 심사 중 저장소 변화 0: 심사 전후 `git status` 같음 · 추적 + 새 파일 85개 해시 같음 · 문서 5개 해시 같음.

## 4. 테스트 · 빌드 · OpenAPI

- `bash scripts/ex12-ubuntu.sh npm test` → **17파일 155/155 통과**(26.7초). vitest가 바꾼 추적 파일 0(해시 비교) → `git checkout` 필요 없음.
- `bash scripts/ex12-ubuntu.sh npm run build` → 성공(`.next` · tsbuildinfo를 지운 **깨끗한 빌드**도 성공: Turbopack 컴파일 + `tsc --noEmit`). 산출물 `.next/server` · `.next/static` · `public` · 설정 131파일에서 `.env` 비밀값 0.
- OpenAPI: 캐시된 `@redocly/cli@2.64.1`(`npx --offline`, 텔레메트리 끔) → **오류 0 · 경고 20**. 같은 도구로 HEAD 판 18 → 새 경고 2개는 `GET /api/auth/callback/google`의 `operation-2xx-response` · `operation-4xx-response`(PLAN대로 302만 있는 경로). diff = version 0.2.0→0.3.0 한 줄 + 두 경로 추가뿐. `sign-in/social` 요청 3칸(required 3, provider enum google) · 200(url · redirect const true · Set-Cookie · no-store) · 403 · 404, `callback/google` 쿼리 state · code · error · 쿠키 · 302(Location · Set-Cookie · no-store · error 코드 목록) = PLAN 표.

## 5. 검증 21줄 (심사가 직접 다시 실행)

| # | 판정 | 근거 |
|---|---|---|
| 1 변경 범위 · 하루 실행 불변 | OK | 3절 |
| 2 스키마 그대로 | OK | 3절 |
| 3 시작(테스트, AC-06-3) | OK | 제작 테스트 통과 + 독립 테스트 1(16검사, 5.2) |
| 4 콜백 새 가입(테스트) | OK | 독립 테스트 2 · 2b · 6 |
| 5 다시 로그인 · 계정 규칙 | OK | 독립 테스트 3 · 4 · 5 · ④ |
| 6 실패 · 보안 | OK | 독립 테스트 5①~⑤d · 6⑥~⑩ · 11 |
| 7 이메일 로그인 회귀 | OK | 기존 `auth` · `account-ui` 6 · `ownership` 5 그대로 통과 + 독립 9 + 브라우저(5.4 ⑥⑩) |
| 8 BFF 302 | OK | 제작 테스트 + 독립 테스트가 실제 BFF 경로로 302 · 여러 Set-Cookie 확인 + 로컬 Next 상대 Location |
| 9 화면(VM) | OK | `google-ui` 6/6 · `widget-ui` 8 · `account-ui` 6 통과 + 같은 내용을 **실제 Chromium**에서 다시(5.4, 모의 8가지 · 돌아온 주소 4가지 · bfcache) |
| 10 OpenAPI | OK | 4절 |
| 11 로컬 회귀 · 빌드 | OK | 4절 |
| 12 로컬 서버(next start, 안전 실행) | OK | 21/21(5.3) |
| 13 로컬 화면(Chromium CDP) | OK | 52/52 + bfcache 2/2(5.4) |
| 14 안전 · 한도(로컬) | OK | 8절: 3000 요청 **67/100** · newsdata 0 · OpenAI 0 · **Google로 나간 요청 0** · 누출 false · DB 9표 건수 · 내용 해시 전후 같음 · 임시 계정 1개 정리 · 남은 프로세스 0 |
| 15 배포 API | SKIP — 푸시 후 | 커밋 · 푸시 전(9절 절차) |
| 16 배포 화면(CDP 360) | SKIP — 푸시 후 | 9절 |
| 17 실제 Google 로그인(PC · 폰) | SKIP — 사용자 확인 대기 | 사람만 가능. PASS 아님 |
| 18 Google 설정 대조 | SKIP — 사용자 확인 대기 | 사용자가 설정을 마쳤다고 알려 왔으나 심사는 화면 · 값을 보지 않음 |
| 19 배포 로그(사용자, 선택) | SKIP — 사용자, 선택 | 15 · 17번 시간대 |
| 20 문의처 자리표시자 · 개인 이메일 | SKIP — 푸시 후(**저장소 · 로컬 부분 OK**) | 저장소 `privacy` · `terms` · `index` 자리표시자 0 · 0 · 0, 이메일 꼴 = `jwavascript@gmail.com` 하나(privacy · terms 각 2번 = mailto + 글자, 모두 `#contact` 절 안), `index.html` 0. 로컬 서버가 준 `/privacy` · `/terms` · `/` 본문도 같음. 그 밖의 주소 0 → "[보류 — 문의 이메일 대기]"는 해당 없음 |
| 21 정책 문안 확인 | SKIP — 사용자 문안 확인 대기 | 초안. 확인할 점은 7절 6 |

### 5.1 합계

**OK 14 · FAIL 0 · SKIP 7**(15 · 16 · 20 = 푸시 후, 17 · 18 · 21 = 사용자, 19 = 선택). PLAN 판정 규칙상 **PASS 보류**(PASS 아님 · Google 앱 게시는 20 · 21 OK 뒤).

### 5.2 독립 인증 테스트(`judge-auth.mts`, 90검사 90 OK)

실제 Better Auth 1.7.7 + 메모리 어댑터 + 실제 `authOptions`(Google 켬, 가짜 client · secret) + 실제 `createApp` · `createBff` 경로. **모든 import 전에 전역 fetch를 가짜로 바꿈**(원래 fetch는 보관하지 않음, 토큰 주소 POST만 답하고 그 밖은 실패 — 불린 17번 모두 가짜 토큰 주소). 가짜 Google은 인가 때 받은 challenge를 code에 묶고 토큰 요청의 `S256(code_verifier)`를 검사(Google과 같은 규칙), ID 토큰은 RS256 머리 · 가짜 서명(1.7.7 Google provider는 토큰 응답의 ID 토큰을 검증 없이 해석 · userinfo를 부르지 않음 — 설치된 코드 확인).

- ① 새 가입: 302 `/?login=google#bookmarks` · 세션 · 상태 쿠키 만료 · user(소문자 · verified · 이름 trim · image null) · account(google · sub · password null · 토큰 5칸 null) · get-session 본인.
- ② 같은 sub 재로그인: 새 행 0 · session +1 · user · account 행 바이트 단위로 그대로.
- ③ 같은 이메일의 이메일 계정 있음(Google 인증 true · false, 대문자 섞음): `account_not_linked` · 세션 0 · DB 그대로 · 이메일 계정은 이메일 로그인 200 그대로. 로컬 계정을 인증됨으로 바꿔도 잇지 않음.
- ④ Google 미인증 이메일: 같은 이메일 계정이 없으면 새 가입(email_verified false), 그 이메일로 이메일 가입 422 · 로그인 401 · 새 행 0. 다른 sub가 같은 이메일 → `account_not_linked`.
- ⑤ 취소 `access_denied`(fetch 0 · 상태 쿠키 만료) · state 불일치 · 쿠키 없음 · 깨진 쿠키 · state 칸 없음 · 11분 뒤 · **PKCE 실패**(흐름 A의 code를 흐름 B 쿠키 · state로 = 코드 주입 → 서버가 B의 verifier를 보내 가짜 Google이 거절 → `invalid_code` · 세션 0, 대조로 A는 성공) · 토큰 400 · ID 토큰 없음(`unable_to_get_user_info`) · 이메일 없음(`email_not_found`).
- ⑥ 이 테스트에서 만든 모든 Google account 행의 토큰 · 만료 칸 null, `access_type=offline`을 일부러 넣어 refresh token이 와도 null.
- 그 밖: 닫은 경로 5 · 우회 변형 11 · idToken · 다른 주소 7가지 · Origin · env 4경우 · https 설정(배포 예측) · 이메일 흐름 · 로그.

### 5.3 12번 로컬 서버(`judge-api.mjs`, 21검사 21 OK)

`npm run build` 뒤 `npx next start -p 3000 --hostname 127.0.0.1`. 가짜 Google 값은 심사 임시 시작 스크립트 안에만(명령줄에 비밀 없음). 서버 로그는 파일로 받아 누출 검사 뒤 확인.

- 켬: `/api/widget` 200(30개, date 2026-10-18) · `/api/rankings` 200 · `/` 200(버튼 · 하단 한 줄 · 창 하단 링크 · 이메일 꼴 0) · `/privacy` · `/terms` 200 text/html(이메일 = 그 주소뿐 · 자리표시자 0) → 시작 200 · no-store · `{url, redirect}` · 칸 8개 · redirect_uri 로컬 콜백 · scope 3개 · **client_id = 자식 env의 가짜 값** · 쿠키 속성(Secure 없음) → 그 쿠키로 취소 302 **`/?login=google&error=access_denied`(상대 주소 그대로)** · no-store · 상태 쿠키 만료 · 세션 0 → 쿠키 없이 `?state=x&code=y` 302 `/?error=state_mismatch` → 쿠키 + 다른 state 302 `/?login=google&error=state_mismatch` → 닫은 경로 5개 404(대조 `list-accounts` 401 = 열린 경로) · Next 경유 `//account-info` 308 · `%2D` 404 → 다른 주소 callbackURL 403 · idToken 404 · 쿠키 + 다른 Origin 403.
- 끔(빈 문자열로 다시 띄움): 시작 404 `PROVIDER_NOT_FOUND` · 쿠키 0 · no-store, 없는 이메일 로그인 401, `/api/rankings` 200.
- Google 요청 0의 근거: code가 붙은 콜백은 state가 맞지 않을 때만 보냄 → 라이브러리가 state 검사에서 멈추고 토큰 교환을 하지 않음(설치된 `callback.mjs` 순서 확인). 서버 로그 = Next 시작 줄 + `AUTH_INTERNAL_ERROR` 5줄(켬) · 1줄(끔).
- `/sign-in*`는 경로마다 마지막 요청에서 10초가 지나야 초기화되는 규칙(설치된 rate-limiter 확인)에 맞춰 3번마다 10.5초 쉼 → 429 0.
- PLAN "확인 필요" 하나: Next(`@next/env` 16.4.0)는 자식 env의 **빈 문자열을 `.env` 값으로 덮지 않음**(빈 값 유지, 없는 이름만 `.env`에서 채움) — 임시 폴더의 `.env`로 따로 확인.

### 5.4 13번 브라우저(`judge-ui.mjs` 52검사 52 OK · `judge-bfcache.mjs` 2검사 2 OK)

Termux Chromium 155 headless + CDP. Google 차단 두 겹: ① 모든 요청을 CDP `Fetch`로 Request 단계에서 멈추고 Google 호스트면 주소 칸만 기록하고 Aborted ② `--host-resolver-rules`로 Google 도메인 이름 풀이 NOTFOUND. `/_vercel/insights/script.js` · `/favicon.ico`는 브라우저 안에서 404로 답함(서버 요청 0, 12번). 탭은 about:blank에서 준비한 뒤 이동하고, 기록 수는 문서 시작 때 잰 값과 비교.

- ① 1280 헤더 "로그인" · 360 북마크 탭 "로그인": 버튼 = 폼 첫 요소 · 제목 아래 · "Google로 계속하기" · type=button · 바탕 rgb(255,255,255) · 테두리 1px rgb(116,119,117) · 글자 rgb(31,31,31) · G 로고 4색(#EA4335 · #4285F4 · #FBBC05 · #34A853) 20×20 · 창 너비 가득 → "또는 이메일로" → 이메일 칸 · 기존 버튼 "로그인 · 회원가입 · 닫기" · 첫 초점 이메일 · 창 맨 아래 "이용약관 · 개인정보처리방침"(`_blank` · `noopener`) · 창 안 이메일 0 · 가로 넘침 0(페이지 · 창 안).
- ② 회원가입 모드: 같은 버튼 · 같은 크기 · 창 안 같은 자리.
- 시작 실패 8가지(모의 응답, 서버 0): 404 `PROVIDER_NOT_FOUND` → "지금은 Google 로그인을 사용할 수 없습니다", 429 → "잠시 후 다시 시도해 주세요", 500 · 403 · 네트워크 실패 · `https://evil.example` · `https://accounts.google.com.evil.example` · url 없음 → "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요". 모두 잠금 풀림 · Google 이동 0 · 같은 문서.
- ③ 실제 버튼 3번(1280 · 360 · 360 issue): 시작 요청 1번(200) → Google 이동 1번이 잡혀 취소(Document 요청, 주소 칸 = 12번) → 같은 문서에서 버튼 4개 잠김 · authBusy · googleLeaving → 상태 쿠키 HttpOnly · SameSite=Lax · Secure 없음 · Path=/ · 남은 시간 약 600초.
- ④ `/?issue=2026-10-18-pol-1` 상세창 북마크(비로그인) → 토스트 → "로그인" → 창(authIssue = 그 id) → 본문 `/?login=google&issue=2026-10-18-pol-1`, 저장 요청 0.
- ⑤ 돌아온 주소를 바로 엶: `/?login=google&error=access_denied#bookmarks` → 북마크 화면 + 창 + "Google 로그인을 취소했습니다" · `/?error=state_mismatch` → "Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요" · `/?login=google&error=account_not_linked` → "이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요" · `error=<img src=x onerror=alert(1)>#list` → 목록 화면 + 일반 문구 · 삽입 0 · alert 0. 모두 로그인 모드 · 첫 초점 이메일 · `location.search` 빈 값 · 해시 그대로 · 기록 수 그대로 · error 값 화면 0.
- ⑥ 없는 이메일 로그인 401 · 기존 문구 · 비로그인. ⑦ 다크: 창 · 버튼 지침 색 그대로(메인은 어두움).
- ⑨ 메인 하단 공통 한 줄(이슈맵 · 목록 · 북마크 모두 보임, 문의 → `/privacy#contact`, 메인 HTML 이메일 0) · `/privacy` 200 · 12절 · `/terms` 200 · 10절 · PLAN 항목 낱말(privacy 39 · terms 17) 모두 있음 · 이메일 = 그 주소뿐(모두 `#contact` 안) · 자리표시자 0 · 360 넘침 0 · Analytics 2줄 · 다크 바탕 rgb(18,18,17).
- ⑩ 임시 계정(360 가입) → "로그인했습니다" · 북마크 화면 제목 아래 "심사O2 · 로그아웃"(inline-block, 헤더 버튼 숨김) · "북마크한 기사가 없습니다" → `/?login=google` + 세션 → 토스트 "로그인했습니다" · 창 안 열림 → "로그아웃" 누름 → sign-out 200 · "로그아웃했습니다" · 줄 숨김 → 1280 헤더로 로그인 → 헤더 "심사O2 · 로그아웃"만(휴대폰 줄 display none) → 로그아웃.
- bfcache(모든 요청을 CDP가 답함 — 서버 0 · 네트워크 0, Google 문서는 가짜 HTML): 버튼 → 가짜 Google 문서로 이동 → 뒤로가기 → **bfcache 복원(pageshow persisted true)** → 버튼 4개 잠금 풀림 · authBusy · googleLeaving false.
- JS 예외 0 · 외부 호스트 = `cdn.jsdelivr.net`(Pretendard 글꼴)뿐. 캡처 16장(10절), 9장을 직접 열어 확인(1280 창 · 1280 404 문구 · 360 창 · 360 다크 · 취소 화면 · 360 로그아웃 줄 · 1280 로그인 헤더 · privacy · terms 문의처).

## 6. 지켜야 할 것 위반

- 없음.
- 비밀값: 모든 명령 출력 · 결과 파일 · 서버 로그 · Chromium 로그 누출 false(`.env` 비밀값 + 가짜 Google secret · 임시 비밀번호 · state · 상태 쿠키 값까지 검사). 코드 · 빌드 산출물에 키 0. `ex12/.env` git 무시 · 추적 `.env` 0 · GOOGLE 값은 `.env`/Vercel에만 두는 구조(`.env.example`은 이름만).
- PLAN 밖 패키지 0 · 기능 0(문안에 문맥상 더한 문장은 제작 보고 11절 1⑦ — 문안 확인 때 사용자 결정).
- 개인정보 최소화: 토큰 · 만료 · 사진 주소 저장 0, Google 쪽 저장 = sub · 이메일 · 인증 여부 · 이름 + scope 문자열(PLAN의 "라이브러리 값").
- 문의 이메일: 제품 파일 중 `public/privacy.html`(`#contact`) · `public/terms.html`(문의처 절)에만 있음. `index.html` · 로그인 창 · 다른 제품 파일 0. 내부 문서(PLAN.md D26 · 계획 · 보고 · HANDOFF)의 언급은 결정 기록이라 해당 없음.
- AGENTS.md: 커밋 · 푸시 0 · `web/` 내용 열람 0 · ex01~ex11 변경 0 · 제품 코드 수정 0.

## 7. 발견한 점(심각도 순, 모두 낮음 · 참고 — PASS 보류 판정에는 영향 없음)

1. **Google로 가는 이동이 도중에 멈추면 로그인 창이 잠긴 채 남음**(낮음). 이동이 Google 페이지로 넘어가기 전에 사용자가 브라우저 "정지"를 누르면 `pageshow`가 오지 않아 버튼 4개 · Esc 닫기가 잠긴 채라 새로고침해야 한다(심사의 Aborted 상태와 같음). 실제 뒤로가기(bfcache)와 시작 실패는 정상으로 풀림을 확인. PLAN 밖이라 기록만, 문제가 보이면 O3 후보(예: 몇 초 뒤 잠금 풀기).
2. **`GET /api/auth/list-accounts`가 열려 있음**(낮음). 세션이 있어야 하고 본인 것만: `providerId · accountId(= Google sub) · scopes · userId · 시각`. 화면은 쓰지 않음. 닫으려면 `disabledPaths`에 한 줄(PLAN 밖 → 사용자 결정).
3. **시작 요청의 추가 칸**(매우 낮음). 라이브러리가 `scopes`(Google 주소 scope에 덧붙음) · `additionalParams`(예약 칸 `state · client_id · redirect_uri · response_type · code_challenge(_method) · nonce · scope`는 400, `access_type=offline` · `prompt=consent`는 들어감) · `loginHint` · `disableRedirect` · `requestSignUp` · `newUserCallbackURL`(같은 Origin 검사 · 다른 주소 403) · `additionalData`(state의 callbackURL · errorURL · link를 덮지 못함 — 확인)를 받는다. 효과는 그 요청을 보낸 브라우저의 자기 로그인에만 미치고(상태 쿠키가 있는 브라우저에서만 끝남), refresh token이 와도 저장 0(확인). 공개된 client_id · redirect_uri로 누구나 같은 Google 주소를 직접 만들 수 있어 새 위험은 거의 없음. 남는 것은 그 사용자가 스스로 범위를 더하면 `account.scope`에 그 문자열이 남는 정도. 막으려면 before 훅(PLAN 밖, 제안만).
4. **쿠키 없는 다른 Origin의 시작 POST는 200**(참고, PLAN이 기록만 하기로 한 라이브러리 동작). 상태 쿠키가 요청한 브라우저에만 생겨 다른 사람 브라우저에서는 `state_mismatch`로 끝남(5.2 · 5.3에서 확인).
5. **상태 쿠키는 서버 쪽 1회용이 아님**(참고). cookie 방식이라 응답이 쿠키를 만료시키지만 같은 값을 다시 보내면 10분 안에는 state 비교를 통과(취소 재전송으로 확인, 세션 생성 0). HttpOnly 쿠키를 훔쳐야 하고 Google code도 1회용이라 실제 영향 없음 — PLAN의 cookie 방식 선택(시작 때 DB 쓰기 0)과 맞바꾼 것.
6. **문안 확인(21번) 때 볼 점 — 심사 의견이며 법률 판단 아님, 법률 검토 권장**: 제작 보고 11절 1의 ①~⑧에 더해, 개인정보 처리방침에 흔히 들어가는 항목 중 초안에 따로 없는 것 — 파기 절차 · 방법(지금은 "삭제 요청 시 삭제"만), 권익침해 구제 방법 안내(분쟁 조정 · 신고 기관 등), 만 14세 미만 가입 처리, 보호책임자 성명(또는 부서) · 연락처, 위탁 · 국외 이전 고지의 세부(이전 국가 · 항목 · 시기 · 방법 · 보유 기간). 약관은 "이용하면 동의한 것으로 봄" 방식 — 가입 동의 방식(안내 문구 · 체크)은 PLAN대로 사용자 결정.
7. (참고) 360px 헤더 왼쪽 브랜드 마크가 안 보임 — O2 전부터(제작 보고 11절 7), O2 범위 밖.
8. (참고) 콜백에 `state` 칸 자체가 없으면 `/?error=state_not_found`(화면은 일반 문구), Next는 `//` 경로를 308로 고침 — 둘 다 닫힘 · 안내 규칙에 영향 없음.

## 8. 요청 수 · 외부 · DB · 임시 계정 · 프로세스

- **로컬 3000 요청 67/100**(심사 누계, 시작 전 0): Node 24 = 12번 켬 20(위젯 · 랭킹 · `/` · 정책 2 · 시작 4 · 콜백 3 · 닫은 경로 5 · list-accounts 1 · 변형 2) + 끔 3 + 종료 확인 curl 1(아래) / 브라우저 43 = 탭 A 4 · B 4 · C 4 · D~G 3씩 · H 17 · I 2. 브라우저 안에서 답한 요청(서버에 안 닿음): insights · favicon 12 · 모의 8 · bfcache 탭 전부. 막힘 0 · 429 0 · 5xx 0.
- 정직하게 남김: 끔 서버를 끌 때 zsh가 변수 `$L`을 낱말로 나누지 않아 첫 kill이 적용되지 않았고, 그 확인 curl 1번이 아직 떠 있던 서버에 닿음(GET `/` 200) → 카운터에 더함(위 67에 포함). 이어 PID 목록을 명령 치환으로 다시 넘겨 정상 종료.
- 외부: newsdata 0 · OpenAI 0 · **Google 0**(서버 토큰 교환 0 · 브라우저 Google 이동 3번은 CDP에서 Aborted + DNS NOTFOUND · bfcache 탭의 Google 문서는 CDP가 만든 가짜 · 단위 테스트는 가짜 fetch만). 그 밖 외부 = `cdn.jsdelivr.net`(글꼴)뿐.
- DB(읽기 전용 스냅숏, 값 · id 출력 없음): 전후 9표 articles 937 · issues 616 · collect_runs 90 · bookmarks 1 · folders 1 · user 1 · session 1 · account 1 · verification 0 — **건수 · 내용 해시 같음 · 행 단위 변화 0**. 실제 Google 계정 행 생성 0.
- 임시 계정: 1개(`o2j-<uuid>@example.invalid`, 13⑩). 정리 직전 그 userId 행 = user 1 · account 1(credential, 토큰 칸 null) · session 0(로그아웃 2번으로 지워짐) · bookmarks 0 · folders 0 → account 1 · user 1 삭제, 다시 찾기 0 · 남은 `o2j-` 계정 0.
- 프로세스: 내 서버 두 벌(켬 4981→4992→5080→5081 · 끔 14486→14495→14557→14558)과 Chromium 두 벌(9 PID · bfcache 1벌)은 띄우기 전 PID 목록과 비교해 내 것만 종료. 끝에 `curl` 3000 · 3001 · 9377 · 9378 = 000, `ps`에 next-server · next start · npm exec next · 내 chromium · 프로필 경로 0(Claude Code 자신만 제외), Chromium 프로필 2개 삭제, 사용자 code-oss(PID 17320) 살아 있음(사용자의 `usb-bookmarks --watch` 루프도 그대로).
- 심사 임시 파일: 저장소 안에 만든 `ex12/.data/judge-o2-20261018/`(스키마 설정 · 독립 테스트 · DB 도구 · 시작 스크립트)는 끝에 지움. 저장소에 남긴 것은 이 보고와 HANDOFF의 O2 줄뿐.

## 9. 푸시 후 확인할 줄(심사 · 사용자)

전제: O1 17번 처리(PLAN "푸시는 O1 17번 확인 뒤", HANDOFF상 10-19 아침 재확인) → 사용자 커밋 · 푸시 승인(Vercel Production `GOOGLE_CLIENT_ID`(Config) · `GOOGLE_CLIENT_SECRET`(Secret)이 푸시 **전**에 있어야 함, 없으면 버튼이 "지금은 Google 로그인을 사용할 수 없습니다" → 넣고 Redeploy). 한도: 배포 요청 ≤ 40(브라우저 자동 요청 포함) · newsdata · OpenAI · 에이전트의 Google 요청 0 · DB 쓰기 0.

- **15 배포 API**(Node fetch, 약 8요청): `POST https://issuerank-kappa.vercel.app/api/auth/sign-in/social`(Origin = 배포 주소, 본문 `{provider:'google', callbackURL:'/?login=google', errorCallbackURL:'/?login=google'}`) → 200 · no-store · url 호스트 `accounts.google.com` · `redirect_uri` = `https://issuerank-kappa.vercel.app/api/auth/callback/google` 정확히 · scope 3개 · `client_id`가 `.apps.googleusercontent.com`으로 끝남(값은 적지 않음) · 쿠키 `__Secure-better-auth.oauth_state` Secure · HttpOnly · SameSite=Lax · Max-Age=600(독립 테스트 8이 예측한 꼴) → 그 쿠키로 `GET /api/auth/callback/google?state={state}&error=access_denied`(redirect manual) 302 `Location: /?login=google&error=access_denied`(Vercel이 상대 주소를 그대로 두는지) → 쿠키 없이 `?state=x&code=y` 302 `/?error=state_mismatch` → `/api/auth/account-info` 404 → `/api/rankings` · `/api/widget` 200. **code가 붙은 요청은 state가 틀리거나 쿠키가 없을 때만**(그래야 서버가 Google 토큰 주소를 부르지 않음).
- **16 배포 화면**(CDP 360, 약 15요청): 13번 ①(북마크 탭 → 창 측정) · ③(버튼 → Google 이동을 Fetch에서 잡아 취소 + DNS 차단) · ⑤(`/?login=google&error=access_denied#bookmarks`) — 로컬과 같은 결과, redirect_uri = 배포 콜백, client_id는 접미사만.
- **20 배포 부분**(3요청): 배포 `/privacy` · `/terms` · `/` 본문에서 자리표시자 수 · 이메일 꼴 목록 → 0 · `jwavascript@gmail.com` 하나 · `/`는 0.
- **사용자 17**: 테스트 사용자 Google 계정으로 PC(로그아웃 상태) 로그인 › Google로 계속하기 → 누른 화면 · "로그인했습니다" · 새로고침 유지 · 북마크 1개 → 로그아웃 → 다시 Google(같은 북마크) → 폰(Safari · Chrome) 같은 계정(같은 목록 · 칩 · 개수, 북마크 화면 제목 아래 "{이름} · 로그아웃") → Google 화면에서 취소("Google 로그인을 취소했습니다") → (있으면) 이미 이메일로 가입한 주소의 Google 계정(연결 안내 문구 · 그 계정 북마크 그대로) → (선택) 카카오톡 안 링크 · 테스트 사용자가 아닌 계정. 동의 화면에 보인 이름(도메인)도 함께 전달. 사용자 계정은 지우지 않음.
- **사용자 18**: Google Auth Platform 대상(외부 · 게시 상태) · 데이터 액세스(openid · userinfo.email · userinfo.profile 3개뿐) · 클라이언트 승인된 리디렉션 URI(배포 콜백 + 로컬은 선택) · 브랜딩 링크를 글이나 캡처로(비밀 빼고). 20 · 21 OK 전 게시 상태는 "테스트"여야 함.
- **사용자 19(선택)**: 15 · 17번 시간대 Vercel Logs → `AUTH_INTERNAL_ERROR` · `API_INTERNAL_ERROR`만인지.
- **사용자 21**: 배포된 `/privacy` · `/terms`(푸시 전이면 저장소 파일)를 읽고 "확인" 또는 고칠 점(7절 6 · 제작 보고 11절 1, 법률 검토 권장). 고치면 문안만 고친 뒤 20번 다시 → 게시(브랜딩 › 앱 도메인 홈 · `/privacy` · `/terms` · 승인된 도메인 → 대상 › 앱 게시) → 18번 다시.

## 10. 증거 파일(저장소 밖 — 심사 세션 scratchpad, 세션이 끝나면 지워질 수 있음)

`/data/data/com.termux/files/usr/tmp/claude-10308/-data-data-com-termux-files-home-storage-capstone/3646d5a7-8930-403e-9918-202599e5948c/scratchpad/o2judge/`: 스크립트 `jlib.mjs`(누출 검사 · 요청 카운터 · 속도 제한 문) · `judge-auth.mts`(사본, 5.2) · `judge-api.mjs`(12) · `judge-ui.mjs`(13) · `judge-bfcache.mjs` · `db.mjs`(사본) / 결과 `judge-auth-result.json` · `api-fake.json` · `api-off.json` · `ui-result.json` · `bfcache-result.json` · `request-count.json` · `db-before.json` · `db-after.json` · `protected-hash.txt` · `npm-test-1.txt` · `npm-build-2-clean.txt` · `redocly-{work,head}.txt` · `db-generate.txt` · `auth-generate.txt` · `server-{fake,off}.log`(모두 누출 검사 false) / 캡처 16장 `o2j-*.png`. 이 보고의 숫자가 판정 근거이고, 파일은 다시 볼 때만 참고.

## 11. 2026-10-18 15:50 KST — D27 재심사(정지 뒤 로그인 창 잠김 수정)

> 대상: 7절 발견 1번을 사용자가 푸시 전에 고치기로 한 D27(PLAN.md D27 · `plan/PLAN_ops_O2_google_login.md` 끝 「범위 추가 (D27)」). 기준 HEAD `b2c9379`, 미커밋 변경 `ex12/public/index.html`(+17 −7) · `ex12/test/google-ui.test.ts`(+55 −1). 제작 보고 13절은 참고만 하고 영향받는 줄을 **새 심사 스크립트로 다시 실행**(제작 `ex12/.data/o2-d27-20261018/` 재사용 0). 제품 코드 수정 0.
> **판정: D27 OK — O2는 계속 PASS 보류.** D27 추가 줄 OK, FAIL 0, 위반 0. 21줄 합계 **OK 14 · FAIL 0 · SKIP 7**(변화 없음, 15 · 16 · 20 배포 부분 = 푸시 후, 17 · 18 · 21 = 사용자, 19 = 선택).

### 11.1 범위 · 보호 파일
- `git diff b2c9379`: ex12에서 바뀐 것은 `public/index.html` · `test/google-ui.test.ts` 2개뿐. 나머지 추적 83개(ex12 + `.github/workflows/daily.yml`)는 HEAD와 SHA-256이 같음. 여기에는 보호 파일 전부(`server/bff.ts` · route · `server/db/*` · `drizzle/*` · package · lock · 위젯 3 · 하루 실행 파일 · `server/auth.ts` · `privacy.html` · `terms.html` · `openapi.yaml` · `next.config.mjs`)가 들어감. 새 파일 0 · 새 패키지 0.
- 문서: PLAN.md D27 한 줄 · 계획 D27 절 · 제작 보고 13절 · HANDOFF. ex01~ex11 · frontend · web · SPEC 변경 0.
- `index.html` 변경 내용(직접 읽음):
  - `GOOGLE_STALL_MS = 5000` · `googleStall` · `googleUnlock()`(타이머 지움 · googleLeaving · authBusy false · 창 버튼 4개 다시 됨)을 더함.
  - `location.assign` 뒤에 `googleLeaving = true` + 5초 타이머를 둠.
  - `pagehide`에서 타이머를 지우고, `pageshow`(persisted)에서 `googleUnlock`.
  - 시작 실패 경로 · 돌아온 화면 처리 · 이메일 흐름은 그대로.

### 11.2 테스트 · 빌드
- `bash scripts/ex12-ubuntu.sh npm test` → **17파일 158/158**(`google-ui` 9 = 기존 6 + D27 3). vitest가 바꾼 추적 파일 0.
- `.next`를 지운 깨끗한 `npm run build`(Turbopack + `tsc --noEmit`) 성공. 출력 누출 false.

### 11.3 심사 VM(`judge-d27-vm.mjs`, 25검사 25 OK)
작업 트리 `index.html`의 로그인 블록을 그대로 잘라, 내가 만든 가짜 DOM · 가짜 시계(ms 단위 예약표) · 가짜 fetch에서 실행함. 제작 테스트 도구는 쓰지 않음.

- **정지**:
  - 누르면 응답 전에 이미 잠김. 응답 뒤 이동 1번 · 예약 1개(5000).
  - **4999ms: 잠김 · Esc 막힘 → 5000ms: 버튼 4개 · authBusy · googleLeaving false**. 문구 · 토스트 0, 남은 예약 0, 입력 칸 계속 됨.
  - 그 뒤 Esc로 닫힘. 다시 열어 이메일 로그인하면 `sign-in/email` 1번과 기존 401 문구.
- **잠긴 동안 엔터 제출**: 요청 0. 풀린 뒤 1번.
- **다시 누름**: 새 시작 · 새 예약. 다시 5000ms에 한 번만 풀림.
- **정상 이동(pagehide)**: 예약이 지워지고, 20초가 지나도 아무것도 바뀌지 않음.
- **bfcache**:
  - pagehide persisted → 떠난 동안 그대로 → pageshow persisted에서 즉시 풀림 · 예약 0, 그 뒤 변화 0.
  - persisted가 아닌 pageshow는 그대로. 예약 중 pageshow persisted면 예약도 지움.
- **시작 실패 7가지**(404 `PROVIDER_NOT_FOUND` · 429 · 500 글 · 네트워크 · Google 아닌 url · url 없음 · JSON 아님): 표의 문구 · 즉시 풀림 · 이동 0 · 예약 0, 10초 뒤에도 같음.
- **기타**:
  - 응답 전 두 번 누름 → 요청 1번.
  - `location.assign`이 던지면 → 일반 문구 · 풀림 · 예약 0.
  - **잠긴 동안 창이 닫혀도**(Chrome이 활성화 없는 두 번째 Esc를 취소 불가 cancel로 보낼 때) 5초 뒤 다시 열 수 있음. D27 전에는 이 경우도 새로고침 전까지 막혔음.
- **실수 넣기**(파일은 그대로 두고 메모리 안 코드 문자열만 바꿈 → 해시 · 백업 불필요): 6가지 모두 잡힘.

  | 넣은 실수 | 실패한 검사 수 |
  |---|---|
  | 타이머 없음 | 10 |
  | pagehide가 타이머를 안 지움 | 2 |
  | 풀 때 authBusy 그대로 | 10 |
  | 15초 | 8 |
  | pageshow 풀기 없음 | 3 |
  | 이동 전에 상태를 바꿈 | 1 |

### 11.4 실제 Chromium(`judge-d27-ui.mjs`, 20검사 20 OK)
- **실행 조건**:
  - 로컬 `npx next start -p 3000 --hostname 127.0.0.1`. 가짜 Google 값은 scratchpad 시작 스크립트에만 둠.
  - Termux Chromium 155 headless + CDP.
  - Google 이동(문서)은 탭마다 다르게 처리함.
    - 정지: 잡자마자 Aborted. 브라우저 "정지"와 같은 상태.
    - 느린 이동: 잡아 둔 채 7초 뒤 가짜 HTML로 놓음.
    - 정상 이동: 바로 가짜 HTML.
  - Google의 다른 요청은 브라우저 안 404, 이름 풀이도 NOTFOUND.
  - 시각은 페이지 안 기록기(문서 capture 단계 · `performance.now`)로 잼.
- **360 정지** — `/#bookmarks` 북마크 탭 "로그인"으로 시작:
  - 잠김은 누른 뒤 3ms. 시작 응답 200은 누른 뒤 38ms.
  - Google 이동을 취소한 직후: 같은 문서 · 창 열림 · 버튼 4개 잠김 · 입력 칸 됨.
  - 5초 전 Esc(이메일 칸을 눌러 활성화를 준 뒤): cancel이 취소 가능 상태로 와서 **막힘**.
  - **풀림 = 시작 응답 뒤 5006ms**(누른 뒤 5044ms). 문구 없음 · 창 그대로.
  - 풀린 뒤 Esc → **닫힘**.
  - 다시 열어 **임시 계정으로 이메일 로그인 → 200** · "로그인했습니다" · 북마크 화면 "심사D27 · 로그아웃" → 로그아웃 200.
- **1280 느린 이동** — 헤더 "로그인"으로 시작:
  - 이동을 잡아 둔 동안 **시작 응답 뒤 5006ms에 풀림**. 그 기록이 잡혀 있는 중에도 CDP로 도착함.
  - 7013ms 뒤 놓자 **이동이 끝까지 감**: 가짜 Google 문서가 열리고, pagehide persisted는 시작 응답 뒤 7153ms(풀림 뒤).
  - 뒤로 → bfcache 복원 · 버튼 정상 · 풀림 1번뿐.
- **360 정상 이동**:
  - 페이지가 시작 응답 뒤 58ms에 떠남(pagehide persisted).
  - 6.5초 머문 뒤 뒤로 → pageshow persisted(6628ms) 순간에 풀림(차이 1ms, 1번). 떠난 동안 타이머가 풀지 않음.
  - 6초를 더 기다려도 새 기록 0.
- **시작 실패 회귀**(같은 탭, 브라우저 안 모의 → 서버 0): 404 · 429 · 500 · 네트워크 · Google 아닌 url 5가지 모두 표의 문구 · 즉시 풀림 · 이동 0. 5.6초 뒤에도 문구 · 상태 · 기록이 그대로(타이머 없음).
- **로그인 창 회귀(360 · 1280)**: 첫 요소 · 문구 · 지침 색 · G 4색 20×20 · 높이 40 · 너비 가득 · "또는 이메일로" · 기존 버튼 · 첫 초점 · 정책 링크 · 창 안 이메일 0 · 넘침 0, 1280 창 400.
- **돌아온 화면 회귀**: `/?login=google&error=access_denied#bookmarks` → 북마크 화면 + 창 + "Google 로그인을 취소했습니다" · 버튼 됨 · 쿼리 지움 · 해시 그대로 · 5.6초 뒤에도 그대로.
- **기타**: Google 문서 이동은 3번뿐(정지 · 느린 · 정상)이고 모두 CDP가 취소하거나 가짜로 답함. 주소 칸 정상(redirect_uri 로컬 콜백 · scope 3개 · 가짜 client_id · state 32 · challenge 43). JS 예외 0. 캡처 `d27j-360-stop-locked` · `d27j-360-stop-unlocked`를 직접 열어 확인함(잠김 = Google 버튼만 흐림).

### 11.5 5초 선택에 대한 심사 판단 — 타당
- **정상 이동에서 페이지가 떠나는 때**: Google 첫 응답 바이트가 아니라, 새 문서가 확정(commit)되는 때다. 그때까지 이름 풀이 · TCP · TLS · 첫 요청 + Google 쪽 리디렉트(같은 연결 재사용, 한 번에 왕복 1번) + 마지막 응답이 지나간다. 그래서 제작 근거의 "왕복 3~4번"은 조금 낙관적이다.
- **실제 걸리는 시간**: 왕복 0.1초(좋은 LTE)면 약 1초 안, 0.3초(약한 신호)면 약 2~3초. 아주 느린 연결(왕복 0.6초 이상 · 혼잡)에서는 5초를 넘을 수 있다.
- **5초를 넘겨도 문제없음**: 그때는 **잠금이 먼저 풀릴 뿐 진행 중인 이동은 그대로 이어진다**. 11.4 느린 이동에서 5006ms에 풀린 뒤 7초에 놓은 이동이 끝까지 감을 확인했다. 남는 위험은 그 사이 다시 누르면 새 시작이 첫 이동을 대신하는 정도이고, 상태가 맞지 않으면 돌아온 화면에 일반 문구가 나온다(제작 보고 13.2). 정상 흐름을 망가뜨리지 않는다.
- **정지한 사람 입장**: 최대 5초만 기다리면 된다.
- 결론: 늦게 풀려도 해가 없고 빨리 풀려도 해가 없으니, 둘의 균형으로 5초는 합리적이다. 더 길게(예 8~10초) 하면 느린 연결의 이른 풀림은 줄지만 정지한 사람의 기다림이 길어진다. 지금 값을 바꿀 근거는 없다.
- 참고(D27 전부터 있던 것, 고치지 않음): 잠긴 동안 Google 버튼만 흐려지고 다른 버튼 3개는 비활성 모양이 없음(제작 13.5). Chrome의 Esc 남용 방지 규칙 때문에 잠긴 동안에도 활성화 없는 두 번째 Esc는 창을 닫을 수 있다. 이 경우도 이제 5초 뒤 다시 열 수 있다(11.3).

### 11.6 요청 수 · 외부 · DB · 임시 계정 · 프로세스
- **로컬 3000 요청 20/40**(이번 재심사 새 카운터, 시작 0, 막힘 0 · 429 0 · 5xx 0).
  - Node 1: 임시 계정 가입.
  - 브라우저 19: 정지 탭 8 · 느린 이동 4 · 정상 이동 4 · 돌아온 화면 3. bfcache 복원 2번은 요청 0.
  - 브라우저 안에서 답해 서버에 닿지 않은 요청: insights · favicon 5 · 모의 5 · 가짜 Google 문서 2.
- **외부**: newsdata 0 · OpenAI 0 · **Google 0**. Google 호스트 요청은 이동 문서 3 + 가짜 화면 favicon 1이고, 모두 CDP가 취소하거나 답함. DNS도 NOTFOUND. 서버는 code가 붙은 콜백을 받지 않아 토큰 교환 0. 서버 로그 = Next 시작 줄뿐(`AUTH_INTERNAL_ERROR` 0줄, 누출 false).
- **DB**(읽기 전용 스냅숏): 9표 건수 · 내용 해시 전후 같음, 행 단위 변화 0(articles 937 · issues 616 · collect_runs 90 · bookmarks 1 · folders 1 · user 1 · session 1 · account 1 · verification 0).
- **임시 계정 1개**(`o2j-<uuid>@example.invalid`): 정리 직전 그 userId 행 = session 1(API 가입 세션, 브라우저 세션은 로그아웃으로 지워짐) · account 1(credential, 토큰 칸 null) · bookmarks · folders 0. 그 행만 지운 뒤 다시 찾기 0.
- **프로세스 · 파일**:
  - 내 서버(11556 → 11575 → 11638 → 11639)와 Chromium 1벌(9 PID)만 PID로 종료. 이번에는 PID 목록을 명령 치환으로 넘겨 zsh 문제 없음.
  - 끝에 `curl` 3000 · 3001 · 9379 = 000. `ps`에 next-server · next start · 내 chromium 0. 사용자 code-oss(PID 17320) 살아 있음.
  - 프로필 삭제. 저장소 안 임시 폴더(`ex12/.data/judge-o2-d27/`, DB 도구)와 가짜 secret이 든 파일도 지움.
  - 심사 중 저장소 변화 0: status · 추적 85파일 · 문서 5개 해시 같음.
- **증거**: scratchpad `…/scratchpad/o2d27/`(`judge-d27-vm.mjs` · `judge-d27-ui.mjs` · `vm-result*.json` · `ui-d27-result.json` · `request-count.json` · `db-before.json` · `db-after.json` · `npm-test.txt` · `npm-build.txt` · `server.log` · 캡처 2장). 세션이 끝나면 지워질 수 있다.

### 11.7 갱신된 합계 · 다음
- 21줄 합계는 그대로 **OK 14 · FAIL 0 · SKIP 7**이고, D27 추가 줄은 OK. 판정 **PASS 보류**.
- 7절 발견 1번은 **해결**됨(D27). 2번 `/list-accounts`는 D27 결정대로 그대로 둠.
- 다음 순서:
  1. O1 17번 처리.
  2. 사용자 커밋 · 푸시 승인. 커밋 대상 = D27 두 파일 + 문서.
  3. 배포 줄 15 · 16 · 20(9절 절차). 16번에서 정지 상태도 같은 방법(이동 취소 → 약 5초 뒤 풀림)으로 볼 수 있음.
  4. 사용자 17 · 18 · 21(19는 선택).
