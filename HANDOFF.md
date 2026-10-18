# HANDOFF — 이슈랭크 캡스톤

최종 갱신: 2026-10-18 · 운영 단계(D21, ex12 운영 폴더, 로드맵 plan/PRODUCT_PLAN.md). O0 04:17 예약 적용(10-18 첫 실행 06:26 시작 · 06:30 완료, 129분 지연). **O1 위젯** 배포 완료 · PASS 보류(OK19/FAIL0/SKIP2: 17번은 10-18 확인이 05~09시 창 밖이라 SKIP → 사용자 결정으로 **10-19 아침 07~09시 재확인**, 18번 iPhone 당분간 불가). **O2 Google 로그인** 로컬 커밋 `b2c9379`(미푸시) + D27(정지 뒤 창 잠김 수정, 작업 트리) · **독립 심사 PASS 보류**(reports/ops_O2_google_login_judge.md: 로컬 줄 1~14 모두 OK · OK14/FAIL0/SKIP7 · 위반 0, D27 재심사 OK(11절), 남은 것 = 푸시 뒤 15 · 16 · 20 배포 부분 + 사용자 17 · 18 · 21 + 19 선택) → 다음: 10-19 아침 O1 17번 확인 뒤 사용자 커밋 · 푸시 승인(D26) → 배포 줄 → 사용자 실제 Google 로그인 · 정책 문안 확인 · Google 앱 게시. 품질 기록 reports/ex09_daily.md 4/7일(10-18 이상 2건: 섹션 다른 같은 사건 미병합, 검색어 OR에 다른 주체)

다른 PC · 새 Codex 세션에서 이어 갈 때 AGENTS.md와 이 파일을 읽는다. 이전 PC 결과와 현재 Termux/Ubuntu에서 다시 실행한 결과를 각 단계 기록에서 구분한다.

## 1. Codex에서 시작하기

1. 저장소 루트에서 AGENTS.md를 읽는다. ex10 이메일·비밀번호 로그인은 PASS했다. 사용자 승인으로 ex08 품질 FAIL과 ex09 자동화는 보류했다.
2. ex10 근거는 `plan/PLAN_backend_ex10.md`, `reports/ex10_build.md`, `reports/ex10_judge.md`다. ex11 계정별 북마크·폴더도 계획·제작·심사 PASS(`reports/ex11_build.md`, `reports/ex11_judge.md`). 다음 단계는 사용자와 정한다.
3. 현재 Termux에서는 `bash scripts/ex11-ubuntu.sh npm test`와 `bash scripts/ex11-ubuntu.sh npm run build`를 쓴다. 의존성 복원은 같은 래퍼의 `npm ci --no-audit --no-fund`다.
4. 현재 실행 설정은 ex11/.env에만 둔다. 기존 DB 연결과 새 BETTER_AUTH_SECRET, BETTER_AUTH_URL을 준비했다. 값은 출력하지 않는다. Google OAuth·SMTP는 이번 범위에 없다.
5. 서버는 별도 터미널에서 `bash scripts/ex11-ubuntu.sh npm run api`(8787), `bash scripts/ex11-ubuntu.sh npm run dev -- --hostname 127.0.0.1`(3000). 실제 검증은 단계 PLAN의 안전 실행과 호출 한도를 따른다.
6. 새 세션 요청 예: “AGENTS.md와 HANDOFF.md를 읽어 줘. ex10 로그인은 PASS했고 ex08 품질과 ex09 자동화는 보류 상태야. ex11 계정별 북마크·폴더도 PASS했어. 다음 단계를 정하자.”

### 현재 실행 환경 주의

- Termux/proot에서는 Next.js의 LAN 주소 자동 탐지가 `ERR_SYSTEM_ERROR`·`uv_interface_addresses`로 실패할 수 있다. dev/start에 `--hostname 127.0.0.1`을 지정한다. 권한 설정 변경은 필요 없다. 브라우저는 BETTER_AUTH_URL과 같은 `http://localhost:3000`으로 접속한다.

### 현재 환경 확인 (2026-10-13)

- Android/Termux에 Node.js 24.18.0 · npm 11.20.0 설치 완료. Termux 직접 실행은 테스트 51개 통과했지만 Next.js Turbopack과 TypeScript 7의 Android 네이티브 실행 파일이 없어 전체 빌드 불가. 기존 proot Ubuntu에 공식 Linux ARM64 Node.js 24.18.0을 SHA-256 확인 후 `/opt/node-v24.18.0-linux-arm64`에 설치했다.
- ex08은 Ubuntu에서 실행한다. 아래 래퍼를 사용하며, Termux Node와 Ubuntu Node 사이에서 같은 node_modules를 번갈아 설치하지 않는다. OS가 바뀌면 대상 환경에서 `npm ci`로 복원한다.
- Ubuntu 검증 완료: Node.js 24.18.0 · npm 11.16.0, `npm ci` 성공, 테스트 6파일 51개 통과, 기본 `npm run build`(Turbopack + 타입 검사) 성공. 재개 기록은 `reports/ex08_resume.md`.
- `ex08/.env`의 필수 값 입력과 DB 비밀번호 재설정 후 실제 DB·newsdata·OpenAI 연결 확인 완료. 실제 값은 이 파일에만 두고 .env.example은 이름·설명만 보관한다.
- 샌드박스 실행이 `bwrap: Can't read /proc/sys/kernel/overflowuid: Permission denied`로 실패했다. 이번 파일 작업은 명시적 실행 승인 후 진행했다. 제품 코드 오류와 별개이며 권한 설정을 임의로 바꾸지 않는다.
- 초기 Codex 전환 후, 사용자 승인에 따라 ex08 중요도 순위와 OpenAI SDK 전환까지 구현했다. 이후 실제 검증에서 newsdata 19회·OpenAI 4회를 사용했다. 프롬프트 보강 작업 자체는 추가 유료 호출 0회다.

### 현재 기기에서 실행하는 명령

저장소 루트의 Termux 터미널에서:

```bash
bash scripts/ex08-ubuntu.sh node --version
bash scripts/ex08-ubuntu.sh npm ci --no-audit --no-fund
bash scripts/ex08-ubuntu.sh npm test
bash scripts/ex08-ubuntu.sh npm run build
```

화면·API는 `.env`를 로컬에서 준비한 뒤 별도 터미널 두 곳에서:

```bash
bash scripts/ex08-ubuntu.sh npm run api
bash scripts/ex08-ubuntu.sh npm run dev
```

래퍼는 기존 Ubuntu에 저장소를 `/workspace/capstone`으로 연결하고 ex08 폴더에서 실행한다. 인수 없이 실행하면 그 환경의 셸이 열린다. 일반 Linux/Windows PC에서는 래퍼 없이 ex08에서 npm 명령을 사용한다.

ex08 당시에는 0004까지 적용했고, 현재 공유 DB에는 ex11의 0006까지 적용됐다. 다른 DB를 준비할 때만 이전 적용 기록을 확인한 뒤 아래 마이그레이션 명령을 사용한다:

```bash
bash scripts/ex08-ubuntu.sh npm run db:migrate
```

실제 실행은 PLAN의 안전 실행 절차를 따른다. 기존 DB의 이전 마이그레이션 적용 여부를 먼저 확인하고, 실제 키·접속 문자열을 출력하지 않는다. OpenAI 키는 OpenAI Platform에서 발급하며 ChatGPT 구독과 API 과금은 별도다.

- 기본 모델: `gpt-4.1-mini-2025-04-14`, 정리·요약 공통. 정리 지시문은 사건 단위 기준과 반례를 보강했다. 요약 지시문은 유지했으며, 1차 지시문은 합성 입력에서 제외·섹션·검색어 의미 FAIL. 이후 2차 보완 완료, 2차 지시문은 합성 재검증에서 불필요한 제외·섹션 기준 FAIL. 이후 3차 보완을 완료했고 단일 정리 구조에서 발생한 대량 누락·중복을 해결하기 위해 2단계 배정 구조로 변경했다. 최신 합성 검증은 번호 완전성 통과·사진 제외 FAIL이다.
- [OpenAI 키 준비](https://platform.openai.com/api-keys) · [모델/단가](https://developers.openai.com/api/docs/models/gpt-4.1-mini) · [구독과 API 요금 구분](https://learn.chatgpt.com/docs/pricing)
- 최초 검토안은 `reports/ex08_resume.md`, 최신 제작·심사는 각 보고의 2026-10-13 절을 따른다.

## 2. 진행 방식

- `AGENTS.md`를 공통 규칙으로 사용한다. `plan_helper.md`에 따라 단계 exXX를 진행하고 계획 → 제작 → 심사 순서를 유지한다.
  - `agent_prompts/1_plan_agent.md`: 계획 → `plan/PLAN_backend_exXX.md`
  - `agent_prompts/2_build_agent.md`: 제작 → `exXX/` · `reports/exXX_build.md`
  - `agent_prompts/3_judge_agent.md`: 심사 → `reports/exXX_judge.md`
- 이전 Claude Code의 Opus 서브에이전트 지정은 Codex의 역할별 순차 작업으로 옮겼다. 독립 심사는 새 세션에서 하고, 서브에이전트는 별도 요청 시에만 쓴다.
- 이미 승인한 범위는 계속 진행하되 미결정에 대한 응답을 임의로 "예"로 간주하지 않는다.
- 커밋은 사용자가 원할 때만. `web/` · `plan/PLAN_web.md`는 사용자의 별도 작업이라 건드리지 않는다.
- 결정 기록은 `PLAN.md` 5절(D0~D15), 단계에서 실제로 문제가 된 것은 10절에 적는다.
- 답은 한국어로, 쉬운 말로. 제품 AI는 사용자 요청에 따라 OpenAI Responses API로 전환했다.

## 3. 지금 상태
| 단계 | 상태 |
|---|---|
| O2 Google 로그인(운영, ex12) | **로컬 커밋 `b2c9379`(미푸시) + D27 수정은 작업 트리에만**(2026-10-18, `reports/ops_O2_google_login_build.md`). `server/auth.ts`(두 env가 다 있을 때만 Google · scope openid/email/profile · 상태 쿠키 · `disableImplicitLinking` · 토큰 저장 0 · 닫는 경로 5 · idToken 닫음 · errorURL `/`) · 로그인 창 "Google로 계속하기"와 돌아온 뒤 처리 · `/privacy` · `/terms` 초안(문의 `jwavascript@gmail.com`은 두 페이지 문의처 절에만, 자리표시자 0) · 메인/창 정책 링크 · 640px 이하 북마크 화면 로그아웃. 새 패키지 · 마이그레이션 0, 보호 · 하루 실행 파일 해시 불변. 테스트155/155(+13) · 빌드 OK · OpenAPI 오류0. 로컬 서버 24/24 · Chromium 92/92(Google 이동은 CDP로 잡아 취소 + DNS 차단, 실제 Google 요청 0) · 로컬 요청 50/100 · newsdata/OpenAI 0 · DB 9표 해시 불변(임시 계정 1개 정리). 검증 OK14/FAIL0/SKIP7(15~21: 푸시 후 · 사용자 줄, 20은 저장소 부분 OK). 문안은 초안 — 게시 전 사용자 확인 · 법률 검토 권장. **독립 심사 PASS 보류**(2026-10-18, `reports/ops_O2_google_login_judge.md`): OK14/FAIL0/SKIP7 · 위반 0 — 심사 스크립트로 모든 줄 다시 실행: 테스트155/155 · 깨끗한 빌드 · OpenAPI 오류0(경고 20, HEAD 18 + 302 경로 2) · 보호 49파일 = HEAD · `auth generate`(Google 켬) = auth-schema · db:generate 변경 0, 독립 인증 테스트 90/90(실제 Better Auth + 메모리 어댑터 + 가짜 Google 인가 · 토큰 — 새 가입 · 재로그인 · 같은 이메일 잇지 않음 · 미인증 이메일 · 취소 · state · 만료 · PKCE 코드 주입 · 토큰 null), 로컬 서버 21/21(켬 · 끔), Chromium 52/52 + bfcache 복원 2/2. 로컬 요청 67/100 · newsdata/OpenAI/Google 0 · DB 9표 건수 · 내용 해시 전후 같음 · 임시 계정 1개 정리 · 소유 프로세스 0(code-oss 17320 유지). 참고(낮음): Google 이동 도중 "정지"면 창 잠김 · `/list-accounts` 열림(본인 정보만) · 시작 요청 추가 칸(`scopes` 등, 저장 0) · 문안 확인 후보(심사 보고 7절). **D27(사용자, 푸시 전 수정) 제작(제작 보고 13절)**: `public/index.html`만 — Google 이동을 시작했는데 페이지가 그대로면 5초(`GOOGLE_STALL_MS`, 근거 13.2) 뒤 창의 버튼 · Esc 다시 됨(문구 없음), 페이지가 떠나면 `pagehide`에서 타이머 지움, bfcache 복원 풀기 그대로. `/list-accounts` 그대로. 테스트158/158(+3) · 빌드 OK · 보호 파일 해시 불변. 재검증 Chromium(정지 = 이동 취소 → 5046ms에 풀림 · Esc 막힘 → 닫힘, 느린 이동 · bfcache 복원 · 시작 실패 · 로그인 창 · 돌아온 화면 문구 회귀) OK, 로컬 요청 26/40 · 외부 · Google 0 · DB 쓰기 0. **D27 독립 재심사 OK**(2026-10-18 15:50 KST, 심사 보고 11절): diff 범위 = 두 파일뿐 · 다른 추적 83개(보호 파일 전부) = HEAD · 테스트158/158 · 깨끗한 빌드, 심사 VM 25/25(4999ms 잠김 → 5000ms 풀림 · pagehide면 변화 0 · bfcache · 시작 실패 7 · 잠긴 동안 창이 닫혀도 5초 뒤 다시 열림, 실수 넣기 6가지 모두 잡힘), 실제 Chromium 20/20(정지: 시작 응답 뒤 5006ms 풀림 → Esc 닫힘 → 임시 계정 이메일 로그인 200 · 느린 이동: 잡힌 동안 5006ms 풀림 → 7초에 놓자 이동 완료 · 정상 이동: 떠난 동안 풀림 0 · 복원 순간 1번 · 360/1280 창 · 돌아온 화면 회귀), 5초 선택 타당(넘겨도 이동은 이어짐). 로컬 요청 20/40 · newsdata/OpenAI/Google 0 · DB 9표 해시 전후 같음 · 임시 계정 1개 정리 · 소유 프로세스 0(code-oss 17320 유지). 21줄 합계 OK14/FAIL0/SKIP7 그대로 → **PASS 보류**. 다음: O1 17번 → 사용자 커밋(D27 두 파일 + 문서) · 푸시 승인 → 배포 줄 |
| O1 홈 화면 위젯 1단계(운영, ex12) | **배포 반영**(커밋 `d249399` 푸시 · Vercel 재배포, 2026-10-17). 독립 심사 배포 줄(심사 보고 11절, 16:35 KST): 15 OK(`/api/widget` 30초 뒤 `HIT age 30` · 클라이언트 `public, max-age=0` · 순서 = SQL 독립 계산 · 정적 3개 = 저장소 파일) · 16 OK(360px 13①④⑦ · 20② · history 1) · 21② OK(script.js 404 · JS 예외 0 · va · 쿠키 0) · 배포 요청 32/40. **21③ OK**(심사 보고 12절, 16:58 KST, Analytics Enable · Redeploy 뒤 배포 `/` 1번: script.js 200 · `POST /_vercel/insights/view` 1회 200 · Set-Cookie · document.cookie 0 · JS 예외 0, 요청 7/10 별도 카운터; 대시보드 방문 수는 사용자 확인 대기) → **합계 OK19/FAIL0/SKIP2(17 · 18 [SKIP — iPhone 실기기 당분간 불가]), PASS 보류**. **17번(심사 보고 13절, 10-18 13:19 KST): [SKIP — PLAN 창(05~09시) 밖 확인]** — 창 밖 관찰은 기대 결과 충족(배포 widget · rankings date 2026-10-18 · 순서 = 그날 SQL 재계산 30개 · `10.18 오전 6:27 수집`, 배포 요청 2/6), 기록상 10-18 예약 실행 129분 지연(06:26 시작 · 06:30:22 끝) → 늦어도 06:35:22부터 그날 데이터, 05:00~06:30은 전날 데이터라 창 안 확인 결과가 시각에 따라 갈려 OK로 바꾸지 않음. 사용자 결정 필요: ① 10-19 아침 실행 끝난 뒤 ~ 09:00 창 안 2요청 확인을 17번 대체로 인정(심사 권장) ② 13절 기록 근거 인정 → OK면 "18번만 남은 PASS 보류(운영 반영 완료)". 아래는 제작 · 로컬 심사 기록 — 제작 로컬 완료(2026-10-17): 공개 `GET /api/widget`(200만 `s-maxage=300`) · `/?issue=<id>` 바로가기 · 상세창 "수집" = 실제 수집 완료 시각 · Web Analytics 2줄 · Scriptable `/widget.js` · 안내 `/widget`. D25(사용자 승인) 반영: `main { overflow-x: clip }`로 360~1280px 화면 전환 중 · 뒤 가로 넘침 0(이슈맵 좌표 · 641px 이상 목록 배치 HEAD와 같음) · 바로가기 날짜 랭킹 5xx · 네트워크 실패면 기존 오류 토스트 그대로. 테스트142/142 · 빌드 OK · 하루 실행 파일 해시 불변. 검증 OK15/FAIL1/SKIP5 — FAIL 14(처음 제작 검증 로컬 요청 누계 약142/100 초과 기록 유지, D25 재검증은 53/60 한도 안), SKIP 15~17 · 21②③(푸시 후) · 18(iPhone 대기). `reports/ops_O1_widget_build.md` 13절. **독립 심사 PASS 보류**(2026-10-17, `reports/ops_O1_widget_judge.md`): OK16/FAIL0/SKIP5(15 · 16 · 17 · 21 푸시 후, 18 iPhone), D25 추가 OK2. 테스트142/142 · 빌드 · OpenAPI 오류0, 보호 15파일 = HEAD, SQL만으로 따로 계산한 순서 = `/api/widget` 30개 일치, 가짜 Scriptable 하네스 826검사 × 5시간대 통과(대조 실수 11개 모두 잡힘), 브라우저 57검사(바로가기 · 5xx 모의 · 5폭 × 6전환 넘침 0 · 좌표 HEAD와 같음 · 상세창 시각 = 위젯 하단). 심사 로컬 요청 70/100 · newsdata/OpenAI 0 · DB 9테이블 해시 불변 · 임시 계정 0 · 소유 프로세스 0(code-oss 17320 유지). 증거 `ex12/.data/judge-o1-20261017/` |
| ex01~ex06 | 완료 · 커밋 |
| ex07 | 완료. 2차 PASS 31/31, 이슈맵 로그 비율 보완 PASS(커밋 `5cb2a7a` · `7d62470`) |
| ex12 배포(Vercel) | 제작 로컬 완료 · 사용자 배포(https://issuerank-kappa.vercel.app). 독립 심사 PASS 보류: OK16/FAIL0/SKIP1(`reports/ex12_judge.md` 12절, 2026-10-16) — 7b 설정 화면·11b PC/폰 사용자 확인 OK, 9b 고유 주소 2개 비로그인 302→vercel.com/sso-api OK. 남은 SKIP: 13 Vercel Logs(2026-10-16 01:49:50Z~01:50:10Z = 10:49:50~10:50:10 KST, 확인 요청 10줄) 사용자 전달 대기 |
| ex09 하루 1회 자동 실행 | 제작 완료(2026-10-15): `npm run daily`·`.github/workflows/daily.yml`(06:17 KST). 테스트111/111·빌드, 로컬 실제 하루 종료0(뉴스18/20·OpenAI5/5, 잔여161). 푸시·Secrets3·수동 실행 success(기사158·이슈133·요약29). 독립 심사 **PASS**(2026-10-16): 예약 실행 38825041172 success(144분 지연, 기사167·이슈135·요약29). 실행 요약 화면은 사용자 확인 남음 |
| ex11 계정별 북마크·폴더 | 완료. 0006 적용. 독립 심사 PASS(2026-10-15): 테스트96/96·빌드·API/BFF/Chromium 80요청, OK11/FAIL0/SKIP0 |
| ex10 이메일·비밀번호 로그인 | 완료. Better Auth 1.7.7, 0005 인증 테이블 적용. 독립 심사 PASS: 테스트85/85·빌드·API18·Chromium UI7, OK11/FAIL0/SKIP0 |
| ex08 AI 이슈 정리 · 3줄 요약 | 완료. 심사 PASS(2026-10-15): OK31/FAIL0/SKIP0/대체9(D17). gpt-6-luna·후보 힌트·배정 effort medium(조정 기준: 섹션 경계 기록), ex11 반영 |

ex08 요약:
- `npm run group`: OpenAI(`gpt-4.1-mini-2025-04-14`)가 그날 기사를 묶고, 섹션 · 제외 · `terms`(검색 핵심어 2묶음) · `importance`(1~5)를 정한다. 실패하면 ex07 규칙으로 묶는다.
- 연관기사 수: `qInTitle` 검색 + 1쪽 검증(D13). 순위: 건수 ↓ → importance ↓ → 시각 ↓ → id 자연 순서(D16). importance는 DB에 저장하고 기존 행·규칙·빠진 기사는 0이다.
- `npm run summarize`: Top 5 중 요약이 없는 이슈를 3줄로 요약한다. 설명문이 없으면 건너뛴다.
- 하루 실행 순서: `collect` → `group` → `summarize`(ex09 `npm run daily`로 묶음, Actions 예약 06:17 KST)

## 4. 다음 작업

- **O2 Google 로그인 제작(2026-10-18)**: `reports/ops_O2_google_login_build.md`. 바뀐 파일 ex12 7개 + 새 3개(`public/privacy.html` · `public/terms.html` · `test/google-ui.test.ts`), 작업 트리에만(운영 미반영). 다음 순서: ① O1 17번 처리(사용자 결정, O2 PLAN은 "푸시는 O1 17번 확인 뒤") ② O2 독립 심사(새 세션, `agent_prompts/3_judge_agent.md`, 로컬 3000 ≤ 100 · newsdata/OpenAI 0 · 에이전트 Google 요청 0) ③ 사용자 커밋 · 푸시 승인(Vercel GOOGLE_* 두 값은 사용자가 넣었다고 함 — 확인 안 함) → 배포 줄 15 · 16 · 20(보고 8절, 배포 요청 ≤ 40) ④ 사용자 17번(PC · 폰 실제 Google 로그인) · 18번(Google 설정 전달) ⑤ 21번 문안 확인(보고 11절 1, 법률 검토 권장) → 게시 → 18번 다시. 검증 스크립트 `ex12/.data/o2-build-20261018/`(`server.mjs` next start 3001 + 3000 세는 프록시 · `api.mjs` · `ui.mjs`), CDP로 잡은 이동 중에는 그 페이지 `Runtime.evaluate`가 답하지 않음 → 먼저 취소하고 읽을 것(보고 10절).
- **O2 독립 심사(2026-10-18)**: `reports/ops_O2_google_login_judge.md` **PASS 보류**(OK14/FAIL0/SKIP7, 위반 0). 위 ②는 끝. 남은 순서: ① O1 17번 처리(10-19 아침) ② 사용자 커밋 · 푸시 승인(커밋 대상 = ex12 10파일 + 계획 · 제작/심사 보고 · HANDOFF · PLAN.md 10절 메모, Vercel Production GOOGLE_* 두 값이 푸시 전에 있어야 함) ③ 배포 줄 15 · 16 · 20 배포 부분(심사 보고 9절 절차, 배포 요청 ≤ 40, code가 붙은 콜백은 state가 틀리거나 쿠키 없을 때만 → 서버의 Google 토큰 요청 0) ④ 사용자 17(PC · 폰 실제 Google 로그인) · 18(Google 설정 전달) · 19(선택) ⑤ 21 문안 확인(심사 보고 7절 6 + 제작 보고 11절 1, 법률 검토 권장) → 게시 → 18 다시. 심사 스크립트는 저장소에 남기지 않음(세션 scratchpad). zsh는 `$L` 같은 변수를 낱말로 나누지 않으므로 PID 목록은 `$(cat 파일)`로 넘길 것(심사 보고 8절).
- **O1 위젯 제작(2026-10-17)**: `reports/ops_O1_widget_build.md`. 바뀐 파일 ex12 15개(새 `public/widget.js` · `public/widget.html` · `server/routes/widget.ts` · `test/widget-ui.test.ts` · `test/widget-script.test.ts`), 작업 트리에만. 다음 순서: ① 사용자 결정 — 13번 기존 가로 넘침(보고 11절 ㉠ `main { overflow-x: clip }` 등 별도 수정 또는 기록만) · 14번은 검증 절차 문제(심사는 자기 한도 100) ② O1 독립 심사(새 세션, `agent_prompts/3_judge_agent.md`, 로컬 3000 ≤ 100 · 배포 ≤ 40 · newsdata/OpenAI 0) ③ 사용자 커밋 · 푸시 승인 → 배포 줄 15 · 16 · 21②(보고 8절) ④ Vercel › Analytics › Enable → Redeploy → 21③ ⑤ 푸시 다음 날 05~09시 17번 ⑥ iPhone이 생기면 18번(보고 9절 4). 검증 스크립트 `ex12/.data/verify-o1-widget.mjs`(실행마다 로컬 요청 약 55), 이 기기 Chromium은 주소로 바로 연 탭에서 첫 CDP 이벤트가 빠질 수 있음(보고 11절 7). 정리 중 사용자 code-oss의 크래시 처리기 1개를 잘못 종료함(보고 10절, code-oss 재시작 때 복구).
- **O1 독립 심사(2026-10-17)**: `reports/ops_O1_widget_judge.md` **PASS 보류**(OK16/FAIL0/SKIP5, 위반 없음). ①②(13번 결정 = D25 반영 · 독립 심사) 끝. 남은 순서: ③ 사용자 커밋 · 푸시 승인(커밋 대상 = ex12 15파일 + PLAN_ops_O1_widget · 제작/심사 보고 · HANDOFF · PLAN. 커밋 전 PLAN.md 10절 O1 메모의 "테스트141 · 수정 여부 사용자 결정"을 D25 뒤 문장으로 고치면 좋음 — 심사 7절 1) → 배포 줄 15 · 16 · 21②(심사 보고 9절, 배포 요청 ≤ 40) ④ Vercel › Analytics › Enable → Redeploy → 21③ ⑤ 푸시 다음 날 05~09시 17번(`judge-db.mjs order`로 SQL 순서 비교) ⑥ iPhone 18번. 심사 스크립트 `ex12/.data/judge-o1-20261017/`(`judge-server.mjs` = next start 3001 + 3000 세기 프록시, `judge-ui.mjs` 실행마다 로컬 약 61요청).
- **ex09 제작(2026-10-15)**: `reports/ex09_build.md`. ex11 복사·`scripts/ex09-ubuntu.sh`, 명령은 `bash scripts/ex09-ubuntu.sh ...`. 새 파일 `server/steps.ts`·`server/daily.ts`·`test/daily.test.ts`·package.json `daily`, 루트 `.github/workflows/daily.yml`(cron `17 21 * * *`·수동, checkout@v7·setup-node@v7). ai.ts 해시 `07e10b1e…` 유지. 테스트111/111·빌드 통과. 실제: 잘못된 키 줄(확인행6 삭제·해시 불변), 로컬 하루 `--pages 1 --search 1 --max 3` 종료0(기사108·이슈92·요약3), newsdata 18크레딧·OpenAI 5회, 잔여 161. 품질 첫 줄 `reports/ex09_daily.md`.
- **ex09 심사(2026-10-15)**: `reports/ex09_judge.md` OK11/FAIL0/SKIP1(예약 실행)·해당 없음1. 테스트111/111(첫 실행 통과)·빌드, 잘못된 키 줄 확인행6 삭제·해시 불변, 로그 누출 false, Secrets 이름3. newsdata 1(잔여106)·OpenAI0. 수동 실행 품질 줄을 `reports/ex09_daily.md`에 추가.
- **ex12 제작(2026-10-15)**: `reports/ex12_build.md`. 사용자가 할 일(6절): main 푸시 → 배포용 BETTER_AUTH_SECRET 직접 생성 → Vercel Add New › Project › `Jwavascript/capstone` Import, Root Directory `ex12`·Next.js → Production 환경 변수 3개(DATABASE_URL·BETTER_AUTH_SECRET Secret, BETTER_AUTH_URL Config) → Deploy → 주소가 다르면 BETTER_AUTH_URL 고치고 Redeploy → 설정 화면·주소 전달 → PC·폰 같은 계정 결과 전달. 이후 배포 줄 실행(제작 이어서 또는 심사, 배포 요청 최대150·newsdata/OpenAI 0). 이 기기에서 `npm run dev`(Turbopack)는 파일 감시 한도로 API 실패 → `WATCHPACK_POLLING=true npm run dev -- --webpack --hostname 127.0.0.1` 사용.
- **ex12 심사(2026-10-15)**: `reports/ex12_judge.md` OK13/FAIL0/SKIP4. 배포 요청 66/150·로컬 22/100·임시계정2 정리·9테이블 해시 불변·뉴스/OpenAI0. 증거 `ex12/.data/judge-*`.
- **D19 검색어 보완 제작(2026-10-15)**: `reports/ex12_build.md` 10절. 바뀐 파일 ex09·ex12 `server/ai.ts`(METADATA_SYSTEM terms 문장·예시만)·`test/ai.test.ts`(+1) — 작업 트리에만, 커밋·푸시 없음. 실제 검증(10-15 기사158·organize 1회·첫 후보 검색6) FAIL: n>묶음 1/6(기준 4). 원인 관찰: 2낱말 말도 따옴표 완전 일치(`"석유 탐사"` vs `석유·가스 탐사`), 표기 차이(`금융회사`/`금융사`, `미국`/`美`), 3낱말 이상 8개(지시 위반). 증거 `ex12/.data/terms-20261015/`.
- **D19 다음**: 사용자가 D19-2(한 낱말·표기 변형 동의어·사진 접두 제외)로 결정(PLAN.md D19-2).
- **D19-2 제작(2026-10-15)**: `reports/ex12_build.md` 11절. ex09·ex12 `server/ai.ts`(METADATA_SYSTEM terms 문단 + organize 사진 접두 제외)·`test/ai.test.ts`(1차 문구 테스트 수정 + 새 2) — 작업 트리에만, 커밋·푸시 없음. 실제 검증 PASS: pol 35·eco 23·soc 58·wor 6·it 20(모두 n>묶음), cul 1=1. 공백 포함 말 5/277·동의어 9(기록만). 증거 `ex12/.data/terms2-20261015/`, 스크립트 `ex12/.data/verify-terms2.ts`.
- **D19-2 심사(2026-10-15)**: `reports/ex12_judge.md` 10절 OK14/FAIL0/SKIP1. 실제 검증 기준 통과(pol 34·eco 알래스카 45·soc 58·wor 6·it 20 n>묶음, cul 1). 제작과 차이: 묶음132·제외15, eco 첫 후보 다름(CPI 단독), 동의어 0. 증거 `ex12/.data/judge-terms-20261015/`, 남은 크레딧 82.
- **D19-2 다음**: 10-16 06:17 KST 예약 실행(푸시된 옛 코드)과 ex09 예약 실행 심사 확인 → 사용자 확인 후 커밋·푸시 → 그다음 예약 실행(10-17) 연관기사 수 분포를 `reports/ex09_daily.md`에 기록.
- **D19-3 제작(2026-10-16)**: `reports/ex12_build.md` 12절. 바뀐 파일 ex09·ex12 `server/ai.ts`(excludedBeforeAi + organize 제외 두 줄)·`test/ai.test.ts`(+1) — 작업 트리에만. 검사 스크립트 `ex12/.data/check-digest.ts`, 결과 `ex12/.data/digest-20261016/result.json`. 다음: 사용자 확인 후 D19-3 커밋 · D19-2(5156e89 로컬)와 함께 푸시 → 다음 예약 실행에서 모음 기사 제외·연관기사 수 분포를 `reports/ex09_daily.md`에 기록.
- **D19-3 심사(2026-10-16)**: `reports/ex12_judge.md` 11절 PASS(OK13/FAIL0/SKIP0). 독립 스크립트(삭제함)로 610건 읽기 전용 검사, 결과 `ex12/.data/judge-digest-20261016/result3.json`. 참고: `[간추린 단신]`(id 1449) 단신 모음은 4낱말 밖이라 미제외 — 넣을지는 사용자 결정. 경계 `[헤드라인 단독]` 제외 · 전각 괄호 · 앞 공백 미제외(현재 데이터 영향 0).
- **D20 모바일 이슈맵 제작(2026-10-16)**: `reports/ex12_build.md` 14절. `ex12/public/index.html`만(+11 −1): 640px 이하·확대 아님이면 섹션 블록 세로 쌓기(블록 높이 260~320px), xs 타일 `n위 · m건`. 실제 Chromium 측정 360 xs 12→2·390 9→0, 순위 30/30·xxs 0·가로 스크롤 0·1위 제목 6/6, 768·1280 좌표 변경 전과 동일, 클릭→상세창·확대·다크 OK. 테스트119·빌드 OK, 외부0·DB 쓰기0. 측정 스크립트 `ex12/.data/verify-mapmobile.mjs`, 캡처 `ex12/.data/mapmobile-20261016/`. 다음: 독립 심사 → 사용자 확인 후 커밋·푸시 → 배포 주소 360px 캡처 1회. 로컬 서버는 `next start --hostname 127.0.0.1`(dev Turbopack 불가).
- **D20 독립 심사(2026-10-16)**: `reports/ex12_judge.md` 14절 PASS 보류(OK18/FAIL0/SKIP1). 독립 CDP 측정 7뷰포트(360·375·390·640·641·768·1280): 640 이하 세로 쌓기·순위 30/30·xxs 0·가로 스크롤 0·넘침 0, 641·768·1280 좌표 변경 전과 동일, 클릭·확대·날짜·창 크기·다크 OK, 외부 0·DB 쓰기 0. 캡처 `ex12/.data/judge-mapmobile-20261016/`. 남은 SKIP: 푸시 후 배포 주소 360px(전체 길이 캡처는 바텀시트가 가리므로 뷰포트 캡처).
- **다음 작업**: (ex12) 사용자에게 받을 것 4가지(`reports/ex12_judge.md` 8절: Vercel 설정 화면·배포 고유 주소·PC/폰 결과·15:20~15:45 KST Logs) → SKIP 4줄만 다시 판정(배포 요청 남은 한도 84) → PASS면 워크플로 2줄 ex12로. (ex09) 예약 실행 심사 PASS(2026-10-16, `reports/ex09_judge.md` 9절). 남은 것: 사용자가 로그인 브라우저로 run 38825041172·38683426817 Summary에 daily 출력·끝 줄이 있는지 확인, `ubuntu-latest`→Ubuntu 26 이동(10-19) 뒤 첫 예약 실행 확인, 7일째(10-21)까지 `reports/ex09_daily.md` 기록(10-16 줄은 n=묶음 6/6, 옛 검색어 코드). D19-2 커밋 5156e89는 로컬만(푸시는 사용자 확인 후).
- (완료) 이전 사용자 단계: 커밋 → main 푸시 → GitHub Settings › Secrets and variables › Actions에 `DATABASE_URL`(풀러)·`NEWSDATA_API_KEY`·`OPENAI_API_KEY` 등록 → Actions › daily › Run workflow 1회. 그다음 ex09 독립 심사(새 세션, `reports/ex09_judge.md`, 심사 newsdata 1·OpenAI 0), 다음 날 06:17 KST 이후 예약 실행 확인, 7일째까지 `reports/ex09_daily.md` 기록.

- ex11 제작 검증 완료: `reports/ex11_build.md`, 테스트13파일96개·빌드 통과, 실제 API/BFF·브라우저 합계74/250요청·임시계정2개. 0006 적용·재실행 불변, 계정 분리·재시작·과거 기사 저장·두 브라우저 계정 전환·세션 만료 통과. 임시 행 정리·기존 내용 해시 보존·뉴스/OpenAI0. 독립 심사 PASS: `reports/ex11_judge.md`, OK11/FAIL0/SKIP0·심사 80/250요청·임시계정2개 정리·기존 내용 해시 불변·뉴스/OpenAI0. 실제 검증은 Codex 세션이 실행했고 보고·테스트/빌드 재실행·남은 브라우저 프로필 정리는 Claude Code 세션이 이어서 했다.
- 현재 공유 DB에는 0006까지 적용됐다. ex10 소스는 보존하지만 무소유 저장 쓰기는 호환되지 않으므로 실행은 ex11 래퍼를 사용한다.

- ex11 준비 중 proot Ubuntu가 자동 이동되어 기존 네이티브 도구의 절대 링크가 끊어졌다. 현재 Ubuntu에서 ex11 npm ci를 재실행했고 esbuild 실제 변환을 확인했다. 의존성 버전·권한 설정 변경은 없다. 이전 단계에서 같은 문제가 나면 해당 단계 lockfile로 같은 환경에서 복원한다.

- 최신 요청: ex10을 보존하고 ex11 계정별 북마크·폴더 저장을 계획 → 제작 → 심사한다. ex11 복사본·로컬 env·Ubuntu 래퍼를 준비했다. 명령은 `bash scripts/ex11-ubuntu.sh ...`.
- ex11 계획 전 현재 DB 읽기 확인: 북마크0·폴더0·회원0. 0006 적용 직전 다시 확인하고 기존 무소유 행이 생겼다면 삭제나 임의 귀속 없이 적용을 멈춘다.

- ex10 제작: `reports/ex10_build.md`. 테스트11파일85개·빌드/타입 통과, 실제 인증18요청 PASS(제작 누계 보수20/30). `ex10/.data/build-auth-verdict.json`에 결과. 임시계정 삭제·소유 서버 종료·기존 데이터 보존 확인. 뉴스/OpenAI0.
- 0005 인증4테이블·RLS 적용 완료. 기존 기사285·이슈102·북마크0·폴더0·수집기록54의 내용 불변. 기존 마이그레이션을 다시 적용하거나 기록을 복구할 필요 없다.
- ex10 독립 심사: `reports/ex10_judge.md`. OK11/FAIL0/SKIP0, API18·실제 Chromium UI7 통과. 가입 자동로그인·새로고침 세션·로그아웃·오류·정상 로그인·360px 모바일·기존 화면을 확인했다. 심사 임시계정2개와 소유 서버/브라우저를 정리했고 기존 데이터 내용 해시는 불변이다.

- 최신 사용자 승인에 따른 ex10 선행 구현은 계획·제작·독립 심사까지 PASS했다. ex09 자동화는 계속 보류하며 아래 ex08 과제도 후속으로 유지한다. ex11 계정별 북마크·폴더도 PASS했다. 다음 후보(ex08 품질 재검증 → ex09, 또는 ex12 배포)는 사용자 결정이 필요하다.
- ex10의 당시 실행 기록은 ex10 보고에 보존한다. 현재 실행 대상은 ex11이며 ex11/.env와 ex11 Ubuntu 래퍼를 사용한다.



0. **심사 PASS(2026-10-15)**: 같은 세션 심사(독립성 제한 명시). 사용자 승인으로 남은 실제 줄까지 실행, 검증표 40줄 OK31/FAIL0/SKIP0/대체9(D17). 실제 오늘(10-15) 수집 113·이슈 88·요약 3을 DB에 남김, newsdata 잔여 181. 다음은 ex09 자동화(ex11 복사 출발).
0. **제작(2026-10-15)**: 7차 결과를 사용자 결정으로 PASS(섹션 경계1건 기록), 요약2건 PASS, ai.ts·AI 테스트2개를 ex11에 반영(해시 일치, ex11 99/99·빌드). 다음: ex08 독립 심사(새 세션, reports/ex08_judge.md) → ex09 자동화(ex11 복사 출발). 모델 gpt-6-luna(D18), 하루 정리 약 0.006달러.
0. 2026-10-15 7차(배정 effort medium): 79/79·빌드, 합성 PASS, 실제116은 병합·제외·오병합 통과, 섹션 1건(a054 정형돈 eco)만 엄격 기준 FAIL. v7 4/6회, 요약 SKIP, ex11 미반영, 기준 처리 사용자 결정 대기.
0. 2026-10-15 6차(gpt-6-luna effort low, D18): 79/79·빌드, 합성 PASS, 실제116 FAIL — 오병합0·사진 정확·SNS 병합, 그러나 삼성·하이닉스 급락/이강인/술래게임 인터뷰 미병합(배정 추론 토큰0). v6 4/6회, 요약 SKIP, ex11 미반영, 다음(배정 effort 상향 등) 사용자 결정 대기.
0. 2026-10-15 5차(코드 후보+AI 확인): 로컬79/79·빌드, 합성 PASS, 실제116 FAIL — SNS 3건 병합 성공, 그러나 사진4건이 후보 묶음째 a006·a069와 병합(제외0). OpenAI 8/10(사용자 승인 상한), 요약 SKIP, ex11 미반영, 다음 방식 사용자 결정 대기.
0. 2026-10-15 4차 보완(사진 판별 단서): 로컬77/77·빌드 통과, 합성 PASS, 실제116 FAIL — 연속 사진4건 #photo 정확·누락0이지만 112묶음 전부 단독(같은 SNS 발언 a005·a023·a061 미병합). OpenAI 4/8 사용·뉴스0·DB0, 증거 ex08/.data/recheck-20261015/. ex11 미반영. 다음 방식(구조·모델 변경 등)은 사용자 결정 대기. 결정 D17: ex08 수정→ex11 반영, ex09는 ex11 출발.
1. 현재 정리는 최대2회: 기사 번호별 required 배정 → 코드의 묶음 조립 → 사건별 required 메타데이터. 모델·최종 반환 형식·DB 저장은 유지. PLAN 마지막 절 참고.
2. 이번 실제 합성 13기사 검증: 8사건 병합·번호 완전성·보정0은 통과, 사진 a003 미제외로 9묶음·제외1 FAIL. 실제116·요약은 중간 실패로 SKIP. 남은 과제는 제외 판단의 안정성 및 실제 대량 의미 검증이다.
3. 이번 OpenAI2/최대6·뉴스0·DB 접속/변경0. 증거 ex08/.data/recheck-assignment/synthetic-result.json·synthetic-verdict.json. 중단 후 추가 호출 없음.
4. 이전 116기사 실패/스냅샷은 ex08/.data/recheck-20261014-v3/real-input.json·real-result.json·resumed-verdict.json. 당시9누락·3중복·104단독은 이전 단일 정리 구조의 결과다. 입력 외부 전송은 사용자 승인됨.
5. 현재 코드 로컬 테스트9파일76개·빌드/타입 통과. 제품 API/DB 데이터는 변경하지 않았고 기존 기사·이슈·요약·북마크는 보존. 기사 본문·비밀값·링크는 AI 입력으로 보내지 않음.
6. 실제 의미 품질 및 브라우저·Next BFF·재시작·북마크 CRUD 등이 남아 ex08 PASS 보류, ex09 자동화는 보류. ex10 로그인은 별도 사용자 승인으로 먼저 진행한다.

## 5. 꼭 지킬 것 (실제로 사고가 났던 곳)
- **비밀값 누출**:
  - `DATABASE_URL`이 두 번 출력으로 샌 적이 있다(ex05 심사, 2026-10-08 조사 스크립트). 그때마다 비밀번호를 교체했다.
  - `.env`를 직접 읽는 스크립트는 큰따옴표를 떼고, 클라이언트 생성까지 try 안에 두고, `process.on('uncaughtException')`에서 오류 코드만 찍는다.
  - 출력은 먼저 비밀값이 섞였는지 true/false로 검사한 뒤에만 보여 준다(PLAN의 "안전 실행").
- **newsdata.io 크레딧**:
  - 하루 200이고, 2026-10-08 끝에 46이 남았다(날마다 초기화).
  - 수집 1번 = 12~18, 묶기 검색 = 섹션당 1~6.
  - 실제 호출은 PLAN 검증표가 정한 만큼만, 나머지는 fixture로 한다.
- **AI 호출**: 확인용은 PLAN 한도만. 과거 Claude 응답 원문은 `research/anthropic/`과 `ex08/test/fixtures/ai/`에 있다.
- **DB**: 확인용 행은 끝나면 지우고, 실제 수집 기사 · 이슈 · 요약은 남긴다. 샘플 데이터는 실제 DB에 넣지 않는다(D14).
- **환경**: 이전 PC는 Windows였다. 현재 명령은 AGENTS.md의 OS별 규칙을 따른다. 3000 · 8787 서버는 끝나면 끈다. 단, 사용자가 띄운 서버는 끄지 않는다.

## 6. 왜 이렇게 됐나 (짧은 역사)
- **출처**: 네이버는 robots.txt와 약관 때문에 제외 → newsdata.io 무료 플랜(D0).
- **연관기사 수**: `q` 한 낱말 검색은 `ai` 7,221건처럼 부풀었다 → `qInTitle` + 1쪽 검증(D13). 실험 자료는 `research/newsdata/q_vs_qintitle_2026-10-08.json`.
- **규칙 묶기의 한계**:
  - newsdata `category` 태그 자체가 틀린다(범죄 기사가 politics).
  - 같은 사건을 다른 낱말로 쓴다(`장기금리` 2건 vs 동의어 OR 72건).
  - 그래서 AI 정리(D15)로 넘어갔다.
- **Haiku 구조화 출력**:
  - 같은 입력 4번 중 2번이 `terms` 묶음 수를 어겼다.
  - 그래서 zod를 느슨하게 받고 코드에서 고쳐 쓴다.
  - 번호 중복도 자주 나와서 처음 것만 쓴다.
- **화면**: 누리호 408건이 지도를 덮었다 → 섹션 블록은 같은 넓이, 타일은 log(1+n) 비율로 바꿨다.
