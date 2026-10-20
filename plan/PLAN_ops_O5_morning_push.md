# PLAN_ops_O5_morning_push — 아침 알림(웹 푸시)

> 이유: 하루 실행은 04:17 예약이지만 실제로는 06:26 · 06:30 · 07:53에 시작해(10-18 · 10-19 · 10-20, 10-20은 07:58에 끝남) 출근길 사용자(PRODUCT_PLAN 2절 A)는 랭킹이 언제 바뀌었는지 모르고 직접 열어 봐야 한다(3절 여정 06:30 "알림 · 앱이 많아 뉴스는 뒤로"). 홈 화면 앱(O4)은 생겼지만 다시 열게 하는 신호가 없다(4절 O4 "다시 오게 하기"). 그래서 원하는 기기만 헤더의 종 아이콘으로 구독하게 하고, 그날 랭킹이 준비되면 06:30~10:00에 "오늘의 뉴스가 도착했어요" 알림을 하루 한 통 보내며, 누르면 첫 화면이 열리게 한다. 사용자 요청(2026-10-20)으로 O4 계획 "아침 푸시(Q10)" 표에서 출발하고, SPEC 범위 밖 "알림/구독" 중 **웹 푸시만** 범위 안으로 옮겼다(이메일 알림은 그대로 범위 밖).
> 코드 폴더: `ex12/`(운영 폴더, 복사 없음 — PLAN.md D21) + 저장소 루트 새 `.github/workflows/push.yml`. 보고: `reports/ops_O5_morning_push_build.md` · `reports/ops_O5_morning_push_judge.md`.
> SPEC 근거: **US-14 · AC-14-1~4**(운영 O5, PLAN.md D30 — 2026-10-20 SPEC.md 반영). 회귀: AC-13-1 · 13-2 · 13-3 · AC-11-2 · AC-06-3 · AC-01-1 · 범위 밖 "이메일 알림" · "기사 본문 전체 재게시". 적용 결정: D3(날짜 = 수집한 날 · 04:17 예약) · D5(자동 재시도 없음 · 같은 날 수동 재실행 ≤ 2) · D10(프로덕션 주소 하나) · D14(실제 DB에 샘플 없음 — 시험 행은 만든 것만 지움) · D21 · D24(Web Analytics) · D26(정책 페이지 · 이메일은 문의처에만) · D29(서비스 워커 규칙 · v2 규칙 · 설치 유도 — 좁은 폭 헤더 설치 아이콘은 이번에 바꿈). **D30**(아침 알림 규칙, 확정 — 사용자 2026-10-20, 아래 "결정" Q1~Q6).
> 상태: **확정** — 다음 = 제작(`agent_prompts/2_build_agent.md`). 작업 트리의 O4 미커밋 기록(`HANDOFF.md` · `reports/ops_O4_pwa_share_judge.md` — O4 배포 줄)은 O5가 아니며 변경 범위 · 검증 1의 셈에서 뺀다.

## 이번 범위
- 구독: 헤더 종 아이콘(켜짐 · 꺼짐 · 차단 · 지원 안 함, 권한은 누를 때만) → 새 공개 API `POST · DELETE /api/push/subscriptions` → 새 표 `push_subscriptions`(마이그레이션 0007, 로그인 없이 기기 단위). 발송: 새 명령 `npm run push`(node:crypto로 RFC 8291 aes128gcm 암호화 payload + RFC 8292 VAPID) → 새 워크플로 `push.yml`(하루 실행 `daily`가 끝나면 + 수동 시험). 알림 = 고정 문구 "오늘의 뉴스가 도착했어요", 누르면 `/p`(첫 화면). 받기: `public/sw.js`에 `push` · `notificationclick`(캐시 `issuerank-v2`). 그 밖: 안내 `/widget#push` · `/privacy` 문안 · 측정 경로 `/p` · 배지 그림 1개.
- 새 패키지: **없음** — 암호 · 서명은 Node 24 내장 `node:crypto`(ECDH P-256 · HKDF-SHA-256 · AES-128-GCM · ECDSA ES256), 본문 크기 제한은 설치된 hono의 `hono/body-limit`.
- 확정(D30, 사용자 2026-10-20): ① 하루 실행이 끝나면 바로 1통 · 06:30~10:00 KST(06:30 전에 끝나면 기다림 · 10:00 뒤 · 새 랭킹 없음 = 그날 안 보냄) · 기기마다 그날 표시로 재실행해도 1통 ② 발송 직접 구현(새 패키지 0) ③ 새 `push.yml`(`workflow_run`), `daily.yml` · 하루 실행 코드 · ex09 변경 0 ④ 알림은 고정 문구만(뉴스 내용 · 숫자 · 요약 0), 누르면 `/p`(`?issue=` 없음) ⑤ 헤더 종 아이콘 · 로그인 없이 기기 단위 ⑥ SPEC · D30 반영(완료) · DB 0007은 제작 때 운영 DB 적용 · `/privacy`는 배포 뒤 O2 21번과 함께 사용자 확인 · 심사 결과 사용자 줄만 남으면 코디네이터가 커밋 · 푸시 · 배포 확인 · Secret은 코디네이터가 `gh`로 등록.
- O4에서 바뀌는 것: D29 "아침 푸시 = 범위 밖" → 범위 안 · 헤더 설치 아이콘은 400px 이하에서 숨기고(맨 아래 `앱 설치` 그대로) 900px 미만에서는 글자를 숨김 · `sw.js` v1 → v2 · `/widget`에 `#push` 절 · `tools/pwa-images.tsx` 그림 6 → 7개. O2: `/privacy` 문장 추가. O3: 변경 0.
- 변경 범위(예상): 새 = `server/{push-config,push-keys,webpush,push-run,push}.ts` · `server/routes/push.ts` · `drizzle/0007_push_subscriptions.sql` · `drizzle/meta/0007_snapshot.json` · `public/icons/badge-96.png` · `test/{push,push-api,push-sw,push-ui}.test.ts` · `.github/workflows/push.yml` / 바뀜 = `server/{app,sample}.ts` · `server/db/{schema,store}.ts` · `drizzle/meta/_journal.json` · `public/{sw.js,index.html,widget.html,privacy.html}` · `next.config.mjs` · `tools/pwa-images.tsx` · `openapi.yaml` · `package.json`(scripts 2줄) · `.env.example` · `test/pwa.test.ts`(두 가지) / 그대로 = 검증 1 해시 목록.
- 하지 않음: 알림에 뉴스 내용(제목 · 섹션 · 숫자 · 요약 · 날짜) · 이메일 알림 · 키워드 알림 · 하루 2통 이상 · 알림 시각 · 섹션 고르기 · 계정 연결 · 알림 기록 화면 · 알림 버튼(actions) · 그림(image) · 소리 · 진동 설정 · 앱 배지 숫자(Badging API) · `pushsubscriptionchange`(보내는지 · 언제 보내는지가 브라우저마다 다름 — 확인 필요, 끄고 다시 켜면 복구) · 구독 다시 보내기(매일 upsert) · 예비 예약(cron) · Vercel Cron · Vercel 함수 발송 · Firebase SDK · payload 없는 푸시(아래 판단) · 첫 방문 · 설치 직후 자동 권한 요청(다음 후보: 설치한 앱 첫 실행 때 토스트로 권유) · 맨 아래 줄 아침 알림 링크(헤더 하나로, 필요하면 다음에 보조 링크) · 헤더 로고 마크(640px 이하에서 마크까지 숨는 10-07부터의 동작 — 사용자 결정 10-19대로 그대로, 넣으면 320px 최악에서 15.4px 모자람) · iPhone 실기기 · 카카오 알림톡 · `daily.yml` · `server/{daily,steps,collect,group,summarize,ai,hide,bff,card}.ts` · AI · 수집 · 위젯 · 카드 · 공유 · 로그인 · 이용약관 · ex09 · `web/`.

## 보내는 시각 · 방식 (Q1 · Q3)
| 항목 | 규칙 |
|---|---|
| 언제 | 하루 실행(`daily`)이 끝나면(성공 · 일부 실패 · 멈춤 · 취소 모두) 새 워크플로 `push`가 시작([workflow_run](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows) — 기본 브랜치에 있는 파일만 · Secrets 사용 가능). 보낼지는 실행 결과가 아니라 **DB**로 정함: KST 오늘 날짜 랭킹이 있어야 함(`latestDate() = kstToday()`) |
| 시각 창 | **06:30~10:00 KST**(06:30 = PRODUCT_PLAN 3절 기상, 10:00 = D24 아침 지표 창 끝). 06:30 전이면 06:30까지 기다린 뒤 랭킹을 다시 확인하고 보냄(최대 180분 — 03:30 전에 결정한 실행은 기다리지 않고 "너무 이른 시각"으로 건너뜀, 다음 하루 실행이 보냄). 10:00부터는 그날 보내지 않음 |
| 실측 지연 | 04:17 예약 → 실제 시작 10-18 06:26 · 10-19 06:30 · 10-20 07:53(07:58 끝)(GitHub 실행 기록). 지금 지연이면 알림은 대개 06:31~08:00(하루 실행 끝 + 약 1~2분: 러너 · `npm ci`). 예약이 제때 돌면(04:25쯤 끝) 06:30에 감. 예약은 부하 때 늦을 수 있어(GitHub 문서) 10:00을 넘긴 날은 알림 0 |
| 새 데이터 없음 | 수집 전부 실패 · 이슈 0 · 자정을 넘긴 실행 → KST 오늘 랭킹 없음 → `보내지 않음: 새 데이터 없음(최근 수집일 …)`. 일부 섹션만 실패(D8 대체)한 날은 그날 랭킹이 있으므로 보냄(알림에 내용이 없어 어느 섹션이 대체인지와 무관) |
| 하루 1통 | `push_subscriptions.last_sent_on`(KST 날짜)을 기기마다 **보내기 전에** 그날로 바꿈(이미 그날이면 건너뜀 → 재실행 · 수동 재시도 · 겹친 실행에도 1통). 결과: 2xx = 그대로 · 404 · 410 = 행 삭제 · 10초 무응답 = 그대로(이미 받았을 수 있음) · 그 밖(400 · 401 · 403 · 413 · 429 · 5xx · 연결 실패) = 전 값으로 되돌림(같은 날 다시 실행하면 그 기기만 다시 시도). 시험 알림은 이 표시를 건드리지 않음 |
| 로컬 실행 | `GITHUB_ACTIONS`가 `true`가 아니면 키 확인 · 결정 · 대상 수까지만 출력하고 `로컬 실행: 보내지 않음`(fetch · 표시 · 삭제 0) — ex12/.env에 실제 키가 있어도 로컬에서 실제 구독자에게 나가지 않게 |
| 하루 실행과 분리 | `daily.yml` · `server/daily.ts` · `steps.ts` · ex09 변경 0(D21) → 발송이 실패해도 하루 실행 결과 · 종료 코드 그대로, `push` 실행만 빨갛게 됨. push 명령은 하루 실행 쪽 DB 읽기 함수 `latestDate()`만 씀 |
| 종료 코드 | 0 = 보냄 또는 규칙대로 건너뜀(실패 0) · 1 = 일부 실패(되돌림 · 무응답) · 2 = 설정 · DB 오류(키 없음 · 키가 코드와 다름 · 시험 코드 틀림 · DB) |
| 실행 요약 | `GITHUB_STEP_SUMMARY`에 출력 전체(daily.yml과 같은 방식): `아침 알림 {날짜} · {HH:MM} KST` · `키 확인 OK` · `구독 n · 이미 보냄 n · 대상 n` · `보냄 n · 만료 삭제 n · 실패 n · 응답 없음 n` · 상태별 수 · 서비스별 수(fcm · apple · mozilla · windows) 또는 `보내지 않음: …` · `06:30까지 기다림(약 n분)`. **endpoint · 키 · 행 id · 기기 코드 · 이메일 0**(공개 저장소라 실행 기록은 누구나 봄). DB 오류 문구는 `hide()` + VAPID 값도 *** |
| 워크플로 | 새 `.github/workflows/push.yml`(Secrets를 쓰는 새 자동 실행 — 사용자 승인 Q3 · Q6): `on: workflow_run(workflows [daily], types [completed])` · `workflow_dispatch(input test_code, 비우면 아침 규칙)` · `concurrency: push`(cancel 안 함) · `permissions: contents: read` · `timeout-minutes: 200` · 단계 = checkout@v7 · setup-node@v7(24 · npm 캐시 `ex12/package-lock.json`) · `npm ci --no-audit --no-fund` · `npm run push`(env `DATABASE_URL` · `VAPID_PRIVATE_KEY` = Secrets, `TEST_CODE` = 입력 — `${{ }}`를 run 줄에 직접 넣지 않고 env로) · tee → STEP_SUMMARY → 그 코드로 끝(daily.yml 꼴) |
| 시험 알림 | 수동 실행(Run workflow)에 `test_code` = 그 기기 코드(SHA-256(endpoint) 앞 8자, `/widget#push`에 보임) → 코드가 **정확히 한 기기**와 맞을 때만 그 기기에 `[시험]` 알림 1통(시각 창 · 그날 표시 · 새 랭킹 여부 무시). 0개 · 2개 이상 · 형식 틀림(8자 16진수 아님) = 보내지 않음 · 종료 2. 다른 구독자에게 갈 길 없음, 다음 날 아침을 기다리지 않고 실제 알림 · 누르기를 확인하는 길 |
- 고르지 않은 것(Q1): 고정 시각 Vercel Cron(Hobby 하루 1번 · ±59분, [Vercel](https://vercel.com/docs/cron-jobs/usage-and-pricing)) — 07:58에 끝난 10-20 같은 날은 데이터가 없어 못 보내고 Vercel에 개인 키가 들어감 / 06:30 전 끝나면 건너뜀 — 예약이 제때 도는 날 알림 0. (Q3) daily.yml job 추가 — 보호 파일 변경 / Vercel 함수 발송 — 개인 키 · 번들 · 예약 수단 필요.

## 발송 구현 (Q2) · payload 판단 (Q4 뒤)
| 항목 | node:crypto 직접 구현(확정) | `web-push`(고르지 않음) |
|---|---|---|
| 새 패키지 | 0 | 16개(`web-push` 3.6.7 + jws · jwa · asn1.js · http_ece · https-proxy-agent 등 14 + `@types/web-push` 3.6.4) |
| 라이선스 · 유지 | — · 규격(RFC 8291 · 8292)이 고정, 우리 코드 약 150줄 | MPL-2.0 · 마지막 판 2024-01-16([npm](https://registry.npmjs.org/web-push)) |
| 맞는지 확인 | RFC 8291 5절 예시(부록 A 입력, salt · 서버 키 주입)와 **바이트가 같음** + 가짜 푸시 서비스가 WebCrypto로 따로 쓴 복호화로 풂 + JWT 서명 두 방식 검증. 계획 단계 확인(2026-10-20, 세션 scratchpad · 네트워크 0): 이 기기 Node 24.18 `node:crypto` 약 15줄로 5절 예시 144B와 바이트까지 같음 · ES256 `ieee-p1363` 64B 서명 · 곡선 밖 점은 ECDH · JWK 모두 거절 | 같은 가짜 서비스 시험(RFC 바이트 대조는 빠짐) |
| Vercel 함수 번들 | 암호화 파일은 Actions 명령만 import → 함수 번들 0(구독 API는 검사 함수만) — 빌드 산출물 표지 문자열로 확인(검증 13) | import하지 않으면 번들 0, 설치는 Vercel 빌드 · Actions 둘 다 |

| 판단 | **암호화 payload 유지(결정)** | payload 없는 푸시(VAPID만) |
|---|---|---|
| 보내는 것 | `{ title, body, tag, url }` 약 120B를 aes128gcm으로 | 빈 본문(`Content-Encoding` 없음) |
| 시험 구분 | `[시험]` 제목 · 다른 tag | 못 함(아침 알림과 같은 모양) |
| 문구 바꾸기 | 서버 상수만(sw.js · 캐시 판 그대로) | sw.js 수정 + 캐시 `issuerank-v3` |
| 이후 기능 | 그대로 확장(예: 특별 공지) | 그때 암호화를 다시 붙여야 함 |
| 지원 | aes128gcm = Chrome · Firefox · Safari 공통 | Apple 문서는 "payload가 비면 Content-Encoding을 빼도 됨"만 적음(빈 본문 처리 명시 없음 → Safari 확인 필요, iPhone 기기 없음) |
| 실패 모습 | 복호화가 틀리면 브라우저가 버려 알림 0 → RFC 예시 · 독립 복호화 · 사용자 시험 알림(순서 4)으로 실제 기기 확인 | 실패 지점이 적음 |
- 결정 근거: 새 패키지는 어느 쪽이든 0이고, 암호화 코드는 계획 단계에서 RFC 예시와 바이트 일치를 이미 확인했다. 시험 알림 구분과 서버에서 문구를 바꾸는 길을 남기는 이득이 약 60줄보다 크다. SW는 payload가 없거나 깨져도 아침 알림 문구로 띄우므로, 나중에 payload 없는 푸시로 바꿔야 하면 서버만 고치면 된다(반대 방향은 SW 수정 필요).

## 키 (VAPID)
| 값 | 비밀 | 두는 곳 | 근거 · 규칙 |
|---|---|---|---|
| 공개 키(P-256 비압축 65B, base64url 87자) | 아님 | **코드 상수** 2곳: `server/push-config.ts` · `public/index.html`(같은 값 — 테스트가 비교). .env · Secrets · Vercel env에 넣지 않음 | 브라우저 `applicationServerKey`로 공개되는 값([MDN](https://developer.mozilla.org/en-US/docs/Web/API/PushManager/subscribe)) · 요청마다 `k=`로 보냄([RFC 8292](https://www.rfc-editor.org/rfc/rfc8292.html)) |
| 개인 키(32B, base64url 43자) | 비밀 | `ex12/.env` `VAPID_PRIVATE_KEY`(만든 곳 · 보관) + GitHub Actions Secret `VAPID_PRIVATE_KEY`(새). **Vercel env에는 넣지 않음**(Vercel은 보내지 않음) | 발송 명령은 개인 키로 계산한 공개 키가 코드 상수와 같을 때만 보냄(다르면 fetch 0 · 종료 2 — Mozilla는 키가 안 맞으면 404를 줘 구독을 지우게 되므로, [web.dev](https://web.dev/articles/push-notifications-common-issues-and-reporting-bugs)) |
| subject(`sub`) | 아님 | 코드 상수 `https://issuerank-kappa.vercel.app` | RFC 8292 "mailto: 또는 https:" · Apple: URL · mailto가 아니면 BadJwtToken. 이메일은 쓰지 않음(D26: privacy · terms 문의처 절에만) |
| JWT | — | 실행마다 푸시 서비스 origin별 1개, `exp` = 지금 + 12시간 | RFC ≤ 24시간 · Apple ≤ 하루 · Apple "1시간에 한 번보다 자주 새로 만들지 말 것"(시험을 1시간에 여러 번 하면 Apple 기기는 확인 필요 — 지금 기기 없음) |
| `DATABASE_URL` | 비밀 | 그대로(ex12/.env · Secrets(ex09부터) · Vercel) | push 명령이 구독 · 최근 수집일을 읽음 |
- 키 만들기: 새 `npm run push:keys`(`server/push-keys.ts`) — `generateKeyPairSync('ec', { namedCurve: 'prime256v1' })` → `ex12/.env`에 `VAPID_PRIVATE_KEY=…` 한 줄을 덧붙임(이미 있으면 바꾸지 않고 멈춤 · 권한 600) → 화면에는 **공개 키만**(`VAPID 공개 키: …` · `개인 키는 .env에만 썼습니다`). 제작이 한 번 실행해 공개 키를 코드에 넣음. 키는 바꾸지 않음(바꾸면 모든 기기가 다시 켜야 함 — 개인 키가 새었을 때만, 옛 구독 정리는 사용자 결정).
- 비밀값 출력 0: 키 · 발송 명령 출력 = 숫자 · 상태 코드 · 공개 키뿐 · 안전 실행 검사 목록에 `VAPID_PRIVATE_KEY` 추가 · Actions는 Secrets 값을 로그에서 가림.

### 코디네이터 할 일 (값은 채팅 · 문서 · 캡처에 남기지 않음 — 사용자 승인 Q6)
1. (제작 뒤, 커밋 · 푸시 전) Secret 등록: 저장소 루트에서 `grep -c '^VAPID_PRIVATE_KEY=' ex12/.env` = `1` 확인(줄 수만 나옴) → `grep '^VAPID_PRIVATE_KEY=' ex12/.env | cut -d= -f2- | tr -d '\r\n' | gh secret set VAPID_PRIVATE_KEY --repo Jwavascript/issuerank`(`--body` 없이 쓰면 값을 표준 입력에서 읽음 — `gh secret set --help`) → `gh secret list --repo Jwavascript/issuerank`(이름 · 날짜만). 새 Secret은 이것 하나 · `DATABASE_URL`(ex09)은 그대로 · Vercel 환경 변수 추가 0 · Firebase · Apple 개발자 계정 0.
2. 독립 심사 결과 사용자 줄(21 · 22 · 24)과 iPhone SKIP(23)만 남으면 다시 묻지 않고 커밋 · 푸시 → 배포 줄 18 · 19 → 20(push 수동 실행 1번, **10:00~24:00 KST에만** · 코드 비움: `gh workflow run push.yml --repo Jwavascript/issuerank` → 그 실행 요약 확인). 06:30~10:00에는 실행하지 않음(아직 그날 알림을 못 받은 기기에 실제로 나감).
3. DB 0007 적용은 제작 역할이 함(승인됨 — 적용 전 기록 7행 · 9표 해시).

## 구독 저장 · API
| 항목 | 규칙 |
|---|---|
| 표 | 새 `push_subscriptions`(0007 — 지금 마지막은 `0006_ownership`): `id` text PK(UUID) · `endpoint` text NOT NULL **UNIQUE**(이 고유 키가 인덱스 — 따로 인덱스 없음, 상한 5,000이라 발송은 전부 읽음) · `p256dh` text NOT NULL · `auth` text NOT NULL · `created_at` timestamptz NOT NULL default now() · `last_sent_on` date NULL(그날 표시). RLS 켜기만(정책 없음 — 다른 표와 같음). 계정 · IP · user agent 칸 없음 |
| 마이그레이션 | 안전 실행 `npm run db:generate -- --name push_subscriptions` → `drizzle/0007_push_subscriptions.sql`(CREATE TABLE 1 · UNIQUE · ENABLE RLS뿐) + `meta/0007_snapshot.json` · journal. 적용(`npm run db:migrate`)은 제작 때 운영 DB에 1번(승인됨), 적용 전 `drizzle.__drizzle_migrations` 7행 · 9표 해시 기록. 기존 표 변경 0 → 옛 코드에 영향 없음(푸시 전 적용 가능). 되돌리기 = `DROP TABLE` + 기록 1행(필요할 때만 · 승인) |
| 구독 | `POST /api/push/subscriptions` 본문 = 브라우저 `PushSubscription.toJSON()` 꼴 `{ endpoint, expirationTime?, keys: { p256dh, auth } }`(다른 칸 · expirationTime 무시). 로그인 · 쿠키 불필요. 검사 순서 Origin → 크기 → JSON · 칸 → endpoint(아래 표) → 키(p256dh = base64url 65B · 첫 바이트 0x04 · P-256 곡선 위의 점, auth = base64url 16B) → 저장(같은 endpoint면 키만 갱신, 행 하나). **204**(새 · 기존 같음). 실패: 400 `{ "message": "요청 형식이 올바르지 않습니다" }`(기존 BAD_REQUEST) · 403 같은 문구(Origin) · 413 같은 문구(본문 4KB 넘음, `hono/body-limit`) · 503 `{ "message": "지금은 알림을 켤 수 없습니다" }`(새 endpoint인데 이미 5,000행) |
| 해지 | `DELETE /api/push/subscriptions` 본문 `{ endpoint }` → 그 행 삭제, 없어도 204(여러 번 같아도 됨). 400 · 403 · 413은 위와 같음. 그 밖 메서드 404(Hono 기본) |
| Origin | `Origin`이 `BETTER_AUTH_URL`의 origin(배포 `https://issuerank-kappa.vercel.app` · 로컬 `http://localhost:3000`, 비면 로컬 값)일 때만, 없거나 다르면 403 — 다른 사이트의 스크립트로 구독 · 해지를 못 하게(curl 같은 위조는 못 막음 → 아래 남용 대비) |
| 응답 · 연결 | 모든 응답 `Cache-Control: no-store` · Set-Cookie 0 · 본문 = 위 JSON뿐 · 로그 = 안전 코드만(`PUSH_SAVE_FAILED`), endpoint · 키 0. 새 `server/routes/push.ts`를 `app.ts`에서 연결, Store에 저장 · 삭제 · 개수 3개(dbStore · memoryStore) → `bff.ts` · `index.ts` · `/api/[...path]` 그대로(DELETE 이미 내보냄) |
| 계약 | `openapi.yaml` `/api/push/subscriptions` post(`PushSubscriptionIn` · 204 · 400 · 403 · 413 · 503) · delete(`PushSubscriptionRef` · 204 · 400 · 403 · 413), info.version 0.5.0 |
| 기기 단위(Q5) | 로그인 없이(사용자 확정): 비로그인 다수도 받음 · 계정과 잇지 않아 개인정보가 작음 · 계정 삭제와 무관(그 기기에서 끄면 지워짐) |

### 받는 푸시 서비스 (SSRF 방지 — 이 목록 밖은 400)
| 호스트 | 브라우저 | 근거 |
|---|---|---|
| `fcm.googleapis.com`(정확히) | Chrome · Edge(Android) · Opera · Samsung Internet 등 Chromium 계열 | Chrome의 FCM 엔드포인트([Chrome](https://developer.chrome.com/blog/web-push-interop-wins)). Samsung Internet은 확인 필요(21번에서 호스트만 기록) |
| `updates.push.services.mozilla.com`(정확히) | Firefox | Mozilla autopush 프로덕션 엔드포인트([autopush](https://autopush.readthedocs.io/en/stable/)) |
| `*.push.apple.com`(접미사 `.push.apple.com`) | Safari · iOS 16.4+ 홈 화면 앱 | Apple "allow access for https://*.push.apple.com"([Apple](https://developer.apple.com/documentation/usernotifications/sending-web-push-notifications-in-web-apps-and-browsers)) |
| `*.notify.windows.com`(접미사 `.notify.windows.com`) | Edge(Windows) | Windows 푸시 서비스 WNS 도메인([WNS](https://learn.microsoft.com/en-us/windows/apps/develop/notifications/push-notifications/push-request-response-headers)). Edge 웹 푸시 주소 꼴은 공식 문서에 없음 → 확인 필요(알려진 목록 [pushpad](https://github.com/pushpad/known-push-services)과 같음) |
- 그 밖 규칙: `https:`만 · 포트 표기 없음 · 사용자 정보(`user@`) 없음 · 호스트는 소문자로 비교, 접미사는 점부터(`evilpush.apple.com` · `fcm.googleapis.com.evil.example` 거절) · IP 주소 · `localhost` 거절 · `#` · 공백 · 제어 문자 없음 · ≤ 2,048자. 옛 GCM(`android.googleapis.com`) · 스테이징 · 개발 주소는 받지 않음.
- 보내는 쪽: 보내기 직전 같은 검사를 한 번 더(못 지나면 보내지 않고 삭제) · `fetch(…, { redirect: 'error' })` · 요청마다 10초 · 10개씩 동시에.
- 남용 대비: 본문 4KB · 요청 하나 = 구독 하나 · 전체 5,000행(새 endpoint만 막고 기존 갱신은 됨, 넘으면 사용자 결정) · 같은 endpoint 한 행 · 가짜 · 만료 endpoint는 다음 발송의 404 · 410으로 지워짐. IP는 저장하지 않고 코드에서 IP별 제한도 하지 않음 — 남용이 보이면 사용자가 Vercel › Firewall에 Rate Limit 규칙 1개(Hobby 1개 · IP · 10초~10분 창, [Vercel](https://vercel.com/docs/vercel-firewall/vercel-waf/rate-limiting)). DB가 새어도 endpoint만으로는 알림을 못 보냄(구독이 우리 공개 키에 묶여 개인 키 서명이 필요 — VAPID).

## 알림 내용 · 누르면 (Q4 — 사용자 지정 "내용 말고 '오늘의 뉴스가 도착했어요' 같은 알림만")
| 칸 | 아침 알림 | 시험 알림 |
|---|---|---|
| title | `오늘의 뉴스가 도착했어요` | `[시험] 오늘의 뉴스가 도착했어요` |
| body | 없음(빈 문자열) — 보낸 곳은 기기가 따로 보여 줌: 설치한 앱은 알림 머리에 앱 이름 `이슈랭크`(Android WebAPK · iOS 홈 화면 앱), Chrome 탭에서 켠 경우는 사이트 주소 `issuerank-kappa.vercel.app`(확인 필요 → 21) | `시험 알림입니다` |
| tag | `issuerank-daily` — 같은 tag가 떠 있으면 SW가 **먼저 닫고 새로 띄움** → 어제 알림을 대체 · 똑같은 알림이 쌓이지 않음 · 소리 · 진동은 남. `renotify`는 Chrome 계열만 지원(Firefox · Safari 없음 — [MDN BCD](https://github.com/mdn/browser-compat-data/blob/main/api/ServiceWorkerRegistration.json))이고 같은 tag 대체는 조용히 일어나므로([web.dev](https://web.dev/articles/push-notifications-notification-behaviour)) 닫고 띄우는 쪽으로 | `issuerank-test`(시험끼리만 대체 — 아침 알림을 덮지 않음) |
| 누르면 | `/p` — 새 측정 경로(rewrite → `/index.html`, O4 `/app` · `/s`와 같은 방식, Hobby Analytics는 쿼리 없는 경로만 보임) → 첫 화면(최근 랭킹, `?issue=` 없음) · Pages `/p` = 알림으로 들어온 방문(대략) | 같음 |
| 아이콘 · 배지 | `icon` = `/icons/icon-192.png`(O4 그대로 — 192px 이상 권장) · `badge` = 새 `/icons/badge-96.png`(흰 마크 · 투명 바탕 96×96 — Android 상태 표시줄은 알파만 씀 · 72px 이상 권장, [web.dev](https://web.dev/articles/push-notifications-display-a-notification)). O4 아이콘을 배지로 쓰면 불투명 타일이 흰 사각형으로만 보여 새로 그림(`tools/pwa-images.tsx` 7번째 출력, 기존 6개 바이트 그대로) | 같음 |
| 넣지 않음 | 날짜 · 뉴스 제목 · 섹션 · 연관기사 수 · 요약 · 원문 URL · image · actions · requireInteraction · silent · vibrate · renotify | 같음 |
| 보내는 JSON | `{ "title", "body", "tag", "url" }` 약 120B(한도: RFC 8291 평문 ≤ 3,993B · 본문 ≤ 4,096B, Apple 4KB), `lang: 'ko'`는 SW가 붙임. 문구 · tag · url 상수는 `server/push-config.ts` 한 곳 | 같음 |
| 요청 머리 | `TTL: 21600`(6시간 — 꺼진 휴대폰이 점심 전에 켜지면 받음, RFC 8030 필수) · `Urgency: normal`(잠든 기기를 깨우지 않고 화면을 켜면 받음) · `Content-Encoding: aes128gcm` · `Content-Type: application/octet-stream` · `Authorization: vapid t=…, k=…`. Topic 없음 | 같음 |

## 서비스 워커 (`public/sw.js`)
| 항목 | 규칙 |
|---|---|
| push | `event.waitUntil(…)` 안에서 ① payload를 읽어 검사(네 칸이 문자열 · title 1~80자 · body 0~200자 · tag `[a-z0-9-]` 1~64자 · url이 `/`로 시작하고 `//`가 아님) ② 맞지 않거나 payload가 없으면 **아침 알림 값**(`오늘의 뉴스가 도착했어요` · 빈 body · `issuerank-daily` · `/p`) ③ `registration.getNotifications({ tag })`로 같은 tag 알림을 닫음 ④ `showNotification(title, { body, icon, badge, tag, data: { url }, lang: 'ko' })`. **모든 push에 알림 1개**(보이지 않는 푸시 금지: Chrome · Edge는 `userVisibleOnly: true` 필수, Safari는 안 보이면 권한 회수 — MDN · Apple). 화면을 보고 있어도 보임(건너뛰지 않음) |
| notificationclick | `notification.close()` → `clients.matchAll({ type: 'window', includeUncontrolled: true })`에 같은 출처 창이 있으면 `focus()` + `navigate(origin + url)`(navigate 실패 → `openWindow`), 없으면 `clients.openWindow(origin + url)` — 모두 `waitUntil` 안([web.dev](https://web.dev/articles/push-notifications-common-notification-patterns)). url 기본 `/p`, 다른 출처 창은 보지 않음. 설치한 앱 창으로 열리는지는 확인 필요(21번) |
| O4 규칙 그대로 | install(`/offline.html` 하나 · skipWaiting) · activate(다른 이름 캐시 삭제 · claim) · fetch(같은 출처 · GET · 탐색, `/api/` · `/_vercel/` 제외만 네트워크 → 실패면 오프라인 안내, `cache.put` 0) 글자 그대로 |
| 캐시 이름 | **`issuerank-v2`로 올림**(O4 규칙 "sw.js · offline.html을 바꾸면 v2" · D29 "새 배포 시 옛 캐시 정리", offline.html은 그대로). 배포 뒤 기존 방문자 브라우저가 바뀐 sw.js를 받아 설치 → v1 삭제(O4 심사에서 두 방향 확인). 푸시 구독은 등록(scope `/` · `/sw.js`)에 붙어 SW 판이 바뀌어도 남음 → **scope · 주소 · 등록 코드는 바꾸지 않음** |
| 고장 대비 | O4 "같은 주소에 fetch 처리 없는 판" 교체 때도 push · notificationclick은 남김(빼면 알림이 깨짐) |
| 테스트 조정 | `test/pwa.test.ts`만 두 가지: ① 캐시 이름 기대값 `issuerank-v1` → `v2`(install · activate · 남은 캐시 · 테스트 제목) + 옛 캐시 목록에 `issuerank-v1` 추가(지워지는지) ② widget 앱 절 끝 경계 `<h2>iPhone</h2>` → 새 `<h2 id="push">`(앱 절 문구 기대값은 한 글자도 그대로). 그 밖 기존 테스트 파일 수정 0 — 새 SW 시험은 새 파일 |

## 화면 · UX (Q5 — 사용자 지정: 헤더 종 아이콘)
| 항목 | 규칙 |
|---|---|
| 자리 | 헤더 오른쪽 `[설치(O4)] [종] [북마크] [이름 · 로그인(641px 이상)]` — 새 `<a class="pill-btn" id="pushBtn" href="/widget#push">`(SVG 14px)를 `installBtn`과 `bmNav` 사이에. **모든 폭에서 글자 없는 아이콘**(좌우 9px → 34×32px), `title` = aria-label(PC 마우스 툴팁). 맨 아래 줄 변경 0 |
| 좁은 폭 규칙 | ① 900px 미만: `#installBtn .lbl` 숨김(설치는 아이콘만) · `.header-right` gap 8 → 6px ② **400px 이하: 헤더 `#installBtn` 숨김**(`display: none !important` — 맨 아래 `앱 설치`가 같은 설치 창 · `/widget#app` 안내를 그대로 함) ③ 340px 이하: `.header-inner` gap 10 → 8px · 전환 버튼 좌우 12 → 10px ④ 로고(640px 이하 숨김) · 헤더 높이 56px · 전환 동작 · 북마크 숫자 · 641px 이상 이름/로그인 버튼은 그대로 |
| 폭 계산(640px 이하) | 필요 폭 = 좌우 패딩 32 + 로고 0 + 칸 사이 2개 + 전환 + 오른쪽(종 34 + 사이 6 + 북마크 47 · 숫자 3자리 67, 401px 이상은 설치 40 + 사이 6 더함). 실측값(이 기기 Chromium 155 headless, Pretendard CDN은 막혀 대체 글꼴): 전환 160.4px(버튼 좌우 12) · 152.4px(좌우 10) · 설치 40 · 종 34 · 북마크 47 / 60.3(2자리) / 67(3자리) |
| 여유(남는 폭) | 320px: 288 − 16 − 152.4 − 87~107 = **32.6~12.6px** · 340px: 52.6~32.6 · 341px: 309 − 20 − 160.4 − 87~107 = 41.6~21.6 · **360px: 60.6~40.6px** · 400px: 100.6~80.6 · 401px(설치 보임): 55.6~35.6 · 412px(설치 보임): 66.6~46.6. 대조: 지금 CSS에 종(40)만 더하면 360px · 설치 보임 · 북마크 2자리에서 −8.7px → 전환 글자가 두 줄(실측 버튼 높이 34 → 55), 지금 HEAD도 320px · 설치 보임 · 북마크 2자리에서 −0.7px(이번 400px 규칙으로 해결) |
| 실측 확인(계획 단계) | 위 규칙을 넣은 페이지를 320 · 340 · 341 · 360 · 375 · 390 · 400 · 401 · 412 · 640 · 641 · 700 · 768 · 899 · 900 · 1024 · 1280px × 상태 5가지(종만 · 설치 보임 · 북마크 123 · 이름 `홍길동 · 로그아웃` · 8자 이름) × 스크롤바 0/15px로 잼(세션 scratchpad, 바깥 요청 0, 정적 서버 140요청, 내 프로세스만 정리): **가로 넘침 0(모두)** · 640px 이하는 전환 한 줄 · 오른쪽 버튼 제 폭(눌림 0) · 641px 이상은 8자 이름 + 641px만 전환 줄바꿈(HEAD도 같은 꼴 — 이름이 길면 이미 버튼 말줄임), 설치 보임일 때 641~767px 오른쪽 버튼 눌림은 HEAD보다 작음(641px: HEAD 약 51px → 24px) |
| 상태 4가지 | 아래 표(`data-push` 값으로 CSS · aria). 처음 그릴 때는 `unsupported`(링크) — 스크립트가 바로 고침 |
| 동작 | 지원 = `'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window`. 페이지가 뜰 때 `navigator.serviceWorker.ready`로 등록을 미리 받아 `getSubscription()` · `Notification.permission`으로 상태를 정함. 꺼짐을 누른 순간 **앞에 await 없이** `registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: 코드 공개 키 바이트 })`(권한 창은 이것이 띄움 — Apple "제스처 처리기에서 바로") → `POST /api/push/subscriptions`(본문 = `toJSON()`) → `아침 알림을 켰습니다` · 켜짐. 켜짐을 누르면 `unsubscribe()` → `DELETE`(`{ endpoint }`, 실패해도 끈 것 — 서버 행은 다음 발송 404 · 410로 지워짐) → `아침 알림을 껐습니다` + 버튼 `다시 켜기`(누르면 같은 켜기). 차단을 누르면 `알림이 차단되어 있습니다` + `방법 보기` → `/widget#push`. 지원 안 함은 링크 그대로 `/widget#push`(preventDefault 0). 등록이 아직이면 `잠시 후 다시 눌러 주세요`. 처리 중 다시 누르면 무시 |
| 실패 | 권한 거절 → `알림이 허용되지 않았습니다` · 차단(서버 요청 0) / POST 400 → `이 브라우저에서는 아침 알림을 켤 수 없습니다` / 503 → `지금은 아침 알림을 켤 수 없습니다` / 그 밖 · 네트워크 · subscribe 오류 → `알림을 켜지 못했습니다. 잠시 후 다시 시도해 주세요`. POST가 실패하면 브라우저 구독도 `unsubscribe()`(상태를 맞춤) |
| 코드 위치 | `index.html` 새 구역 `/* ─────────────── Push (O5)`를 **Toast 구역 뒤 · O4 `Share · install (O4)` 구역 앞**에(O4 share-ui 테스트가 자르는 `Share · install (O4)` ~ `Navigation` · Events · Init · `\nstart();\nregisterServiceWorker();\n</script>` 글자 그대로, 버튼 처리는 구역 안 addEventListener, `serviceWorker.register(` 새로 0). CSS는 Header · Responsive 절에 위 규칙만 |
| `/widget#push` | 새 절 `<h2 id="push">아침 알림</h2>`을 앱 절 뒤 · iPhone 절 앞에: `매일 아침 그날 랭킹이 준비되면(대개 6:30~8:00, 10시까지) "오늘의 뉴스가 도착했어요" 알림을 한 번 보냅니다. 누르면 이슈랭크 첫 화면이 열립니다.` / **Android(Chrome)**: `이슈랭크(Chrome 또는 설치한 앱) 오른쪽 위 종 모양 버튼 → 허용. 다시 누르면 끕니다. 차단했다면 Chrome 주소창 왼쪽 아이콘 › 권한(또는 설정 › 사이트 설정 › 알림)에서 이슈랭크를 허용으로 바꾼 뒤 다시 누릅니다. Android 13 이상은 휴대폰 설정 › 앱 › Chrome(또는 이슈랭크) › 알림도 켜져 있어야 합니다.` / **iPhone**: `iOS 16.4 이상에서 위 앱으로 설치로 홈 화면에 추가한 이슈랭크 앱을 열고 오른쪽 위 종 모양 버튼을 누릅니다. Safari 탭에서는 알림을 켤 수 없습니다.` / **카카오톡 · 인스타그램 앱 안 브라우저**: `알림을 받을 수 없습니다. 메뉴에서 다른 브라우저(Chrome · Safari)로 열어 켜 주세요.` / 상태 한 줄(이 페이지 스크립트, 서버 요청 0): `이 기기: 아침 알림 켜짐 · 기기 코드 {SHA-256(endpoint) 앞 8자}(시험 알림용)` · `이 기기: 아침 알림 꺼짐` · `이 브라우저에서는 아침 알림을 받을 수 없습니다`. 메뉴 이름은 기기마다 달라 21번에서 실제 이름을 받아 글만 맞춤 |
| 지원 근거 | Android WebView(카카오톡 · 인스타 앱 안 브라우저)는 Push API 없음 · iOS는 16.4+ 홈 화면 앱만([MDN BCD](https://github.com/mdn/browser-compat-data/blob/main/api/PushManager.json) · Apple) · Chrome · Edge는 `userVisibleOnly` 필수([MDN](https://developer.mozilla.org/en-US/docs/Web/API/PushManager/subscribe)) · Android 13+는 앱 알림 권한도 필요([Android](https://developer.android.com/develop/ui/views/notifications/notification-permission), Chrome 쪽 동작은 확인 필요) |

| 상태(`data-push`) | 언제 | 아이콘 · 색(다크는 같은 토큰) | aria-label = title | 누르면 |
|---|---|---|---|---|
| `off` 꺼짐 | 지원 · 이 기기 구독 없음(권한 묻기 전 · 허용) | 선 종 · `--muted`(hover `--hover` 바탕 · `--text`) | `아침 알림 받기` | 구독(권한 창) |
| `on` 켜짐 | 구독 있음 · 권한 허용 | 칠한 종(`fill: currentColor`) · `--text` | `아침 알림 끄기` | 해지 + `다시 켜기` |
| `blocked` 차단 | `Notification.permission === 'denied'` | 선 종 + 사선 · `--muted` | `아침 알림 차단됨 — 해제 방법 보기` | 토스트 + `방법 보기` |
| `unsupported` 지원 안 함 | 위 지원 조건이 거짓(앱 안 브라우저 · iPhone Safari 탭 · iOS 16.3 이하 등) | 선 종 · 불투명도 .55 | `아침 알림 — 켜는 방법 안내` | `/widget#push`로 이동(링크 기본 동작) |

## 개인정보 — `/privacy` 문안 초안 (배포 뒤 O2 21번 문안 확인과 함께 사용자 확인 · 법률 검토 권장)
- 1항 "처리하는 개인정보"에 추가: `아침 알림을 켠 기기: 그 기기 브라우저가 만든 알림 받는 주소(푸시 서비스 endpoint)와 알림 암호화용 공개 값 2개, 켠 시각, 마지막으로 알림을 보낸 날짜. 계정 · 이름 · 이메일 · IP 주소와 연결하지 않습니다.`
- 2항 목적에 추가: `아침 알림(하루 한 번 "오늘의 뉴스가 도착했어요") 보내기 — 알림을 켠 기기에만`
- 3항 보관에 추가: `아침 알림 정보는 알림을 끄면 바로 지웁니다. 브라우저 알림 권한을 끄거나 앱 · 사이트 데이터를 지워 푸시 서비스가 그 주소를 더 쓸 수 없다고 알려 오면 다음 발송 때 지웁니다.`
- 5항 위탁에 추가: `GitHub Inc. — 매일 자동 실행(뉴스 수집 · 아침 알림 발송 작업)` + `아침 알림은 기기 브라우저 회사의 푸시 서비스(Google · Apple · Mozilla · Microsoft)를 거쳐 전달됩니다. 알림 내용은 국제 표준(RFC 8291)에 따라 암호화되어 푸시 서비스가 읽을 수 없고, 푸시 서비스는 전달에 필요한 주소만 처리합니다.`
- 6항 삭제 요청에 추가: `아침 알림 정보는 계정과 연결되어 있지 않아, 그 기기에서 종 아이콘으로 아침 알림을 끄면 지워집니다.`
- 7항 쿠키는 그대로(알림은 쿠키를 쓰지 않음) · 새 문장에 이메일 0(D26 — 11항 문의처에만) · 시행일을 배포일로 바꾸고 12항에 `변경: 2026년 10월 {배포일} — 아침 알림(웹 푸시) 구독 정보 추가`. 이용약관은 고칠 것 없음(서비스 내용에 알림을 넣을지는 21번 확인 때).
- 확인 필요(법률 판단 안 함): 푸시 서비스 회사가 처리 위탁인지 · 국외 이전 고지 형식 · GitHub 기재 방식.

## 비용 · 한도
- 발송비 0: 브라우저 푸시 서비스는 무료(FCM no-cost — [Firebase](https://firebase.google.com/pricing) · Apple은 Developer Program 불필요 · Mozilla · WNS 계정 0). Firebase 프로젝트 · SDK 없음(VAPID만).
- Actions: push 실행 1번 ≈ 1~2분(러너 · `npm ci` · 발송), 예약이 제때 도는 날만 06:30까지 대기(최대 180분). 공개 저장소의 표준 러너는 무료([GitHub](https://docs.github.com/en/billing/concepts/product-billing/github-actions)) · job 최대 6시간([한도](https://docs.github.com/en/actions/reference/limits)). 저장소를 비공개로 바꾸면 대기 분이 무료 분을 씀 → 그때는 기다림 대신 건너뜀으로 바꿈.
- DB: 행 ≈ 0.4~0.7KB(endpoint 200~500자) → 상한 5,000행이어도 약 3.5MB + 고유 인덱스. Supabase 무료 500MB, 지금 약 12MB(PRODUCT_PLAN 6절).
- Vercel: 켜기 · 끄기 = 함수 1회씩(드묾) · 알림 아이콘 · 배지 = 정적 파일 · 누르면 `/p` = 보통 방문. 새 env 0 · Cron 0. newsdata · OpenAI 추가 호출 0(push는 DB에서 최근 수집일만 읽음). 구독 수 × 요청 1이라는 사용자 비례 작업이 처음 생기지만 돈은 0 — 발송 시간만 늘어남(5,000개 ≈ 10개씩 1~2분).

## SPEC · PLAN.md 반영 (2026-10-20 완료 — 사용자 Q6 승인, 계획 역할이 반영)
- SPEC.md: 사용자 스토리에 **US-14 (아침 알림, 운영 O5)** · 인수 기준 `### US-14 아침 알림 (운영 O5, PLAN.md D30)` **AC-14-1~4** — AC-14-1 헤더 종 아이콘으로 구독(권한은 누를 때만 · 지원 안 함은 안내) · AC-14-2 06:30~10:00 하루 1통(재실행 중복 0 · 새 랭킹 없음 · 10:00 뒤 안 보냄 · 하루 실행 결과 그대로) · AC-14-3 고정 문구만(뉴스 내용 없음) · 누르면 첫 화면 · AC-14-4 끄면 서버 구독 삭제(권한 끈 기기는 다음 발송 때). 범위 밖 줄: `알림/구독 (푸시, 이메일 알림)` → `알림/구독 중 이메일 알림 · 키워드 알림 · 하루 2통 이상의 푸시 (아침 웹 푸시 하루 1통은 US-14로 범위 안 — 2026-10-20)`.
- PLAN.md 5절: **D30 아침 알림(O5, 웹 푸시)** 줄을 D29 아래에 추가(확정 — 사용자 2026-10-20, 이 계획의 확정 내용 그대로). 6절 AC 커버리지 · 7절 이름 표는 앞 운영 단계(O1~O4)처럼 고치지 않음.

## 결정 (D30 확정 — 사용자 2026-10-20)
| # | 질문 | 확정 |
|---|---|---|
| Q1 | 발송 시각 | 하루 실행이 끝나면 바로 1통 · 06:30~10:00(06:30 전에 끝나면 기다림 · 10:00 뒤 · 새 랭킹 없음 = 그날 안 보냄) |
| Q2 | 발송 구현 | node:crypto 직접(새 패키지 0) |
| Q3 | 보내는 곳 | 새 `push.yml`(daily 끝나면 + 수동 시험), daily.yml · ex09 변경 0 |
| Q4 | 알림 내용 | 사용자 지정: 고정 문구 `오늘의 뉴스가 도착했어요`만(뉴스 내용 · 숫자 · 요약 0), 누르면 `/p`(`?issue=` 없음). 계획 역할이 정한 세부: body 없음 · tag `issuerank-daily`로 전날 알림 대체(닫고 띄움) · 시험은 `[시험]` + `시험 알림입니다` + tag `issuerank-test` · 암호화 payload 유지(위 판단 표) |
| Q5 | 버튼 · 구독 단위 | 사용자 지정: 헤더 종 아이콘(상태 4가지 · 400px 이하 헤더 설치 아이콘 숨김 · 900px 미만 설치 글자 숨김 · 340px 이하 간격 줄임) · 로그인 없이 기기 단위 |
| Q6 | 문서 · 운영 반영 | 모두 승인: SPEC · D30 반영(완료) · DB 0007 제작 때 운영 DB 적용 · `/privacy`는 배포 뒤 O2 21번과 함께 사용자 확인 · 심사 결과 사용자 줄만 남으면 코디네이터가 커밋 · 푸시 · 배포 확인 · Secret `VAPID_PRIVATE_KEY`는 코디네이터가 `gh`로 등록 |

## 화면 변경
- 헤더: 오른쪽에 종 아이콘 1개(상태 4가지, 위 표). 400px 이하에서 헤더 설치 아이콘이 사라지고(맨 아래 `앱 설치` 그대로), 641~899px에서 설치 버튼이 아이콘만 됨, 340px 이하에서 칸 사이 · 전환 버튼 좌우가 2px씩 줄어듦. 그 밖의 헤더 · 이슈맵(D20) · 상세창 · 카드 · 로그인 창 · 공유 · 맨 아래 줄은 그대로.
- 새 토스트 8개: `아침 알림을 켰습니다` · `아침 알림을 껐습니다`(+ `다시 켜기`) · `알림이 허용되지 않았습니다` · `알림이 차단되어 있습니다`(+ `방법 보기`) · `이 브라우저에서는 아침 알림을 켤 수 없습니다` · `지금은 아침 알림을 켤 수 없습니다` · `알림을 켜지 못했습니다. 잠시 후 다시 시도해 주세요` · `잠시 후 다시 눌러 주세요`.
- `/widget`: `#push` 절(앱 절 뒤) · 이 기기 상태 한 줄. `/privacy`: 위 문안. 새 경로 `/p`(화면 = `/`). 휴대폰 알림: 제목 `오늘의 뉴스가 도착했어요` · 아이콘 · 상태 표시줄 배지.
- (10-20 코디네이터 결정) 641~1199px 설치 · 북마크 글자 숨김(C2) — 종 추가로 글자 꺾임

## 구현 순서
0. 결정 · 문서: D30 확정 · SPEC US-14 · AC-14-1~4 · 범위 밖 줄 · PLAN.md D30 반영 완료(2026-10-20).
1. 복사 없음(D21): `git status --short` 확인 · 기존 미커밋(HANDOFF · O4 심사 보고)은 손대지 않음 · 작업 전 해시(검증 1).
2. 키: `server/push-keys.ts` · `package.json` `push:keys` → 한 번 실행(공개 키만 출력, 개인 키는 ex12/.env) → `server/push-config.ts`(공개 키 · subject · 문구 · tag · url · 허용 호스트 · 상한 · endpoint 검사) · `.env.example`(`VAPID_PRIVATE_KEY=` + 설명).
3. 서버: `server/db/schema.ts`(표) → `db:generate` → `server/db/store.ts`(Store 저장 · 삭제 · 개수 + 발송용 `pushStore`: 목록 · 그날 표시 · 되돌림 · 삭제) · `server/sample.ts`(메모리) → `server/routes/push.ts` · `server/app.ts` → `server/webpush.ts`(암호화 · VAPID · 요청 1건) → `server/push-run.ts`(규칙 · 순서 · 요약 — 저장소 · fetch · 시계 · sleep을 받음) · `server/push.ts`(명령) · `package.json` `push` → `openapi.yaml`.
4. 정적 · 화면: `public/sw.js` → `tools/pwa-images.tsx`(badge) 래퍼로 실행 → `public/icons/badge-96.png` → `public/index.html`(헤더 `#pushBtn` · CSS 4규칙 · Push 구역) → `public/widget.html`(#push · 상태 스크립트) → `public/privacy.html` → `next.config.mjs`(rewrite `/p`).
5. 워크플로: `.github/workflows/push.yml`.
6. 테스트: 새 `test/push.test.ts` · `test/push-api.test.ts` · `test/push-sw.test.ts` · `test/push-ui.test.ts` + `test/pwa.test.ts` 두 가지 조정.
7. DB 0007 적용(승인됨 — 적용 전 기록 7행 · 9표 해시) → 로컬 검증 1~17 → `reports/ops_O5_morning_push_build.md` → 독립 심사 → 코디네이터: Secret · 커밋 · 푸시 · 배포 줄 18 · 19 · 20 → 사용자 확인 한 번에(아래).
마지막. `bash scripts/ex12-ubuntu.sh npm test` · `bash scripts/ex12-ubuntu.sh npm run build`

## 검증 방법
- 실행: 래퍼 `bash scripts/ex12-ubuntu.sh …`. 로컬 서버 = `npm run build` 뒤 `npx next start -p 3001 --hostname 127.0.0.1` + 3000 세는 프록시(O4 방식), 접속 `http://localhost:3000`. Chromium = Termux Chromium 155 headless + CDP, 측정마다 새 프로필 · 끝나면 삭제. 안전 실행 = ex12 방식(`ex12/.env`만 · stdout + stderr 비밀값 true/false 검사 후 false일 때만 출력 — 검사 목록에 `VAPID_PRIVATE_KEY` 추가). 산출물 `ex12/.data/o5-build-<날짜>/`.
- 한도: newsdata · OpenAI · Google · Kakao 0 · **실제 푸시 서비스(fcm · mozilla · apple · windows) 요청 0**(명령은 주입 fetch · 가로채기, Chromium은 `MAP * ~NOTFOUND`) · 진짜 `pushManager.subscribe` 0(이 기기에서 FCM 등록 요청이 나감) · DB 쓰기 = 승인된 0007 적용 1번 + `push_subscriptions` 시험 행(`o5-build-` · `o5-judge-` · `o5-deploy-` 꼴, 끝에 삭제)뿐 · 다른 9표 쓰기 0 · 임시 계정 0 · 로컬 3000 ≤ 100 · 배포 Node ≤ 20 · CDP ≤ 25 · 배포 줄은 06:30~10:00 KST를 피함 · 제작 · 심사 역할의 Actions 실행 0(20번은 코디네이터). 소유 서버 · Chromium PID만 종료(넓은 패턴 kill 금지 — 사용자 VS Code가 Electron), 끝에 `curl` 000 · `ps` 0. zsh는 PID 목록을 `$(cat 파일)`로.
- 판정: 20 = 코디네이터 줄(푸시 뒤), 21 · 22 · 24 = 사용자 줄, 23 = [SKIP — iPhone 기기 없음]. 그 밖 줄이 모두 OK이고 이것들만 남으면 "PASS 보류"(PASS 아님).

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 1 변경 범위 · 불변 | `git status --short` · `git diff --stat`(작업 전부터 있던 `HANDOFF.md` · `reports/ops_O4_pwa_share_judge.md` 변경은 셈에서 뺌) · 작업 전후 `sha256sum .github/workflows/daily.yml ex12/server/{daily,steps,collect,group,grouping,newsdata,summary,summarize,ai,hide,auth,bff,ranking,schemas,index,card}.ts ex12/server/card-image.tsx ex12/server/routes/{rankings,widget,bookmarks,folders}.ts ex12/server/db/{client,auth-schema}.ts "ex12/app/api/[...path]/route.ts" ex12/app/api/card/route.tsx ex12/app/api/card/caption/route.ts ex12/public/{card,terms,offline}.html ex12/public/{widget.js,manifest.webmanifest,og.png} ex12/public/icons/{icon-192,icon-512,maskable-192,maskable-512,apple-touch-icon}.png ex12/package-lock.json ex12/vercel.json ex12/drizzle.config.ts ex12/drizzle/000[0-6]_*.sql ex12/drizzle/meta/000[0-6]_snapshot.json ex12/assets/fonts/*` · `git status --short ex09` | 바뀐 · 새 파일 = "변경 범위" 목록뿐 · 해시 모두 같음(lockfile 같음 = 새 패키지 0 · package.json은 scripts 2줄만) · ex01~ex11 · ex09 · `web/` 변경 0 · SPEC · PLAN.md는 0단계 반영분(2026-10-20)뿐 |
| 2 암호화 = RFC 예시(단위, AC-14-3) | 새 `test/push.test.ts`: RFC 8291 부록 A 입력(서버 개인 키 · salt 주입, ua 공개 키 · auth)으로 암호화 / 주입 없이 두 번 | 본문 = RFC 8291 5절 base64url과 바이트 같음(144B · 머리 86B = salt 16 · rs 4096 · idlen 65 · 서버 공개 키) / 무작위 판은 두 번 다름 · 둘 다 풀림 |
| 3 VAPID(단위) | 같은 테스트: 엔드포인트 4종(fcm · mozilla · apple 하위 · windows 하위)의 Authorization · 서명을 `crypto.verify`(ieee-p1363)와 WebCrypto `subtle.verify`로 · 다른 개인 키 · 키 없음 | `vapid t={JWT}, k={코드 공개 키}` · 머리 `{"typ":"JWT","alg":"ES256"}` · aud = 엔드포인트 origin · exp = 지금 + 43,200초 · sub = `https://issuerank-kappa.vercel.app` · 두 검증 참 / 다른 키 → fetch 0 · 종료 2 `VAPID 키가 코드의 공개 키와 다릅니다` · 없음 → `VAPID_PRIVATE_KEY가 없습니다` · 키 값 출력 0 |
| 4 요청 · 복호화(가짜 푸시 서비스, AC-14-3) | 같은 테스트: 브라우저처럼 만든 구독자 키 쌍 · auth 3개 → 주입 fetch가 받은 요청을 **WebCrypto로 따로 쓴 복호화**로 풂(메모리 저장소의 2026-10-07 샘플 랭킹이 있는 날) | POST · 머리 = 알림 표(TTL 21600 · Urgency normal · aes128gcm · octet-stream · vapid) · `redirect: 'error'` · 본문 ≤ 4,096B · 풀린 JSON = `{"title":"오늘의 뉴스가 도착했어요","body":"","tag":"issuerank-daily","url":"/p"}` 정확히 · 샘플 이슈 제목 · 섹션 이름 · 숫자 · 날짜 문자열 0 |
| 5 결과 · 하루 1통(단위, AC-14-2 · 14-4) | 같은 테스트(메모리 구독 저장소 · 가짜 fetch · 07:00 KST): 기기 10개에 201 · 404 · 410 · 400 · 403 · 413 · 429 · 500 · 연결 실패 · 10초 무응답 → 같은 날 두 번째 → 다음 날 | 첫 번째: 삭제 = 404 · 410 둘 · 그날 표시 = 201 · 무응답 둘만(나머지 6개 되돌림) · 요약 숫자 = 표 · 종료 1 / 두 번째: 201 · 무응답 기기 요청 0 · 되돌린 6개만 다시 / 다음 날: 남은 8개 모두 대상 / 모든 출력에 endpoint · 키 · id · 기기 코드 문자열 0 |
| 6 날 · 시각(단위, AC-14-2) | 가짜 시계 · 가짜 sleep(KST): 그날 랭킹 + 07:00 · 09:59 · 10:00 · 05:10 · 03:29 / 최근 수집일 = 어제 + 07:00 / 그날 일부 섹션만 있음(D8) + 07:00 / `GITHUB_ACTIONS` 없음 | 07:00 · 09:59 보냄 · 10:00 `보내지 않음: 10:00 지남` · 05:10 sleep(80분) 1번 → 최근 수집일 다시 확인하고 보냄 · 03:29 `보내지 않음: 너무 이른 시각` · 어제 `보내지 않음: 새 데이터 없음(최근 수집일 …)` · 안 보내는 경우 fetch · 표시 0 / D8: 보냄 / 로컬: 결정 · 대상 수만 · `로컬 실행: 보내지 않음` · fetch · 표시 · 삭제 0 |
| 7 시험 알림(단위) | `--test`: 맞는 기기 1 · 0 · 2(앞 8자 같은 가짜) · `xyz` · 11:00 KST · 최근 수집일 = 어제 | 1 → 그 기기만 요청 1 · JSON = `{"title":"[시험] 오늘의 뉴스가 도착했어요","body":"시험 알림입니다","tag":"issuerank-test","url":"/p"}` · 그날 표시 그대로 · 시각 · 새 랭킹 규칙 안 봄 / 0 · 2 · 형식 틀림 → 요청 0 · 종료 2 · 각 문구 |
| 8 구독 API(테스트, AC-14-1 · 14-4) | 새 `test/push-api.test.ts`(메모리, `app.request`, Origin `http://localhost:3000`): 4호스트 정상 · 같은 endpoint 다시(키 바꿈) · 400 묶음(http · `fcm.googleapis.com.evil.example` · `evilpush.apple.com` · `https://127.0.0.1/` · `https://localhost/` · `https://u@fcm.googleapis.com/x` · `:8443` · 옛 GCM · 2,049자 · p256dh 64B · 곡선 밖 점 · auth 15B · keys 없음 · 깨진 JSON) · Origin 없음 · `https://evil.example` · 4,097B · 5,000행 채운 뒤 새 · 기존 · DELETE 있음 · 없음 · GET · `createBff`를 지난 DELETE | 204 · 행 1(키 갱신) / 400 `요청 형식이 올바르지 않습니다` 전부 / 403 · 413 같은 문구 / 503 `지금은 알림을 켤 수 없습니다` · 기존 204 / DELETE 204 · 204 · 행 0 / GET 404 / 모두 no-store · Set-Cookie 0 · 세션 없이 됨 · BFF 뒤 상태 · 본문 같음 · 기존 api · auth · ownership 테스트 그대로 |
| 9 스키마 · 마이그레이션 | 안전 실행 `npm run db:generate -- --name push_subscriptions` → 생성 SQL 읽기 → 한 번 더 generate | 새 = `0007_push_subscriptions.sql` · `meta/0007_snapshot.json` · journal 1항목 · SQL = CREATE TABLE 1 · UNIQUE(endpoint) · ENABLE ROW LEVEL SECURITY뿐(ALTER · DROP 0) · 두 번째 "No schema changes" · 0000~0006 해시 같음 |
| 10 서비스 워커(VM, AC-14-3 · 13-2) | 새 `test/push-sw.test.ts`: sw.js를 가짜 self(registration.showNotification · getNotifications · clients.matchAll · openWindow · claim · location)로 — push: 아침 JSON · 시험 JSON · 데이터 없음 · JSON 아님 · 칸 빠짐 · tag `ISSUE!` · url `https://evil.example` · `//evil.example` · 300자 / 같은 tag 알림이 이미 떠 있음 / notificationclick: 같은 출처 창 · navigate 실패 · 창 없음 · 다른 출처 창만 / O4 install · activate · fetch 시험은 v2로 고친 `pwa.test.ts`가 그대로 | push마다 waitUntil 안 showNotification 정확히 1 · 아침 = `('오늘의 뉴스가 도착했어요', { body: '', icon '/icons/icon-192.png', badge '/icons/badge-96.png', tag 'issuerank-daily', data: { url: '/p' }, lang 'ko' })` · 시험 = `[시험]` 값 · 나머지 = 아침 값(기본) · 같은 tag 알림 close 1 → 그 뒤 show · renotify 0 / 클릭: close 1 · focus → navigate(`http://localhost:3000/p`) · 실패 → openWindow · 없음 → openWindow · 다른 출처 0 / 캐시 = `issuerank-v2`에 offline.html 하나 · `issuerank-v1` 삭제 · cache.put 0 |
| 11 화면 함수(VM, AC-14-1 · 14-4) | 새 `test/push-ui.test.ts`: index.html Push 구역을 가짜 navigator(serviceWorker.ready · pushManager) · Notification · fetch · toast · location으로 ① 지원 안 함 3종 ② 구독 없음 → 누름 → 허용 ③ 거절 ④ POST 400 · 503 · 500 · 네트워크 ⑤ 켜짐 → 끄기(DELETE 204 · 실패) → `다시 켜기` ⑥ 차단 ⑦ 등록 전 누름 ⑧ 빠르게 두 번 | ① `data-push` = `unsupported` · aria-label = title = `아침 알림 — 켜는 방법 안내` · 링크 기본 동작(preventDefault 0) ② subscribe = 누른 같은 틱(앞 await 0) · `{ userVisibleOnly: true, applicationServerKey: 코드 공개 키 65B }` → POST 본문 = toJSON() · `on` · `아침 알림 끄기` · `아침 알림을 켰습니다` ③ POST 0 · `알림이 허용되지 않았습니다` · `blocked` ④ 각 토스트 · unsubscribe 1 · `off` ⑤ unsubscribe 1 → DELETE `{endpoint}` · `아침 알림을 껐습니다` + `다시 켜기`(누르면 subscribe 1) · DELETE 실패여도 `off` ⑥ 토스트 + `방법 보기` → `/widget#push` ⑦ `잠시 후 다시 눌러 주세요` · subscribe 0 ⑧ subscribe 1 · 기존 share-ui · widget-ui · google-ui 테스트 그대로 |
| 12 안내 · 정책 · 그림(단위) | `widget.html` 원문 + 상태 스크립트(VM: 켜짐 · 꺼짐 · 지원 안 함) · `privacy.html` 원문 · `tools/pwa-images.tsx` 두 번(출력은 `.data/`) | `#push` 절 위치(앱 절 뒤 · iPhone 앞) · 문구 = 표 · 켜짐 = `기기 코드` = SHA-256(endpoint) 앞 8자 / privacy 새 문장 = 초안 그대로 · 이메일 꼴 = 문의처 하나뿐 · 시행일 · 변경 줄 / 그림 7개 두 번 같음 · 기존 6개 = 커밋 파일 바이트 · badge 96×96 · 픽셀 색 = 흰색(가장자리 반투명 포함) 또는 완전 투명 · 불투명 픽셀 있음 · fetch는 `data:`뿐 |
| 13 OpenAPI · 회귀 · 빌드 · 번들 | `npx @redocly/cli lint openapi.yaml` · 래퍼 `npm test` · `npm run build` · `.next/server` 전체에서 `WebPush: info` · `Content-Encoding: aes128gcm` 찾기 | 오류 0(경고 = HEAD와 같은 수 + 새 경로분만) · 0.5.0 / 기존 219개(기대값을 바꾼 곳 = pwa.test.ts 두 가지뿐) + 새 테스트 통과 · 빌드 · tsc 성공 / Vercel 함수 산출물에 발송 코드 0 |
| 14 로컬 서버 · 실제 DB(0007 적용 뒤, 안전 실행) | next start 3001 + 3000 세는 프록시. Node fetch `/sw.js` · `/p` · `/icons/badge-96.png` · `/widget` · `/privacy` / POST(시험 endpoint `https://fcm.googleapis.com/fcm/send/o5-build-{uuid}` · 스크립트가 만든 키) → DB 읽기 → 같은 endpoint 다른 키 POST → DELETE → DB 읽기 / Origin 없음 · 다른 호스트 · 4,097B / 회귀 `/api/rankings` · `/api/widget` · `/api/card/caption` · `/` | sw.js 헤더 3개 = O4 · 본문 = 파일(v2) / `/p` = index.html / badge 200 `image/png` / widget · privacy = 파일 / 204 → 행 1 → 204(행 1 · 키 바뀜) → 204 → 행 0 / 403 · 400 · 413 · 모두 no-store / 회귀 상태 · 헤더 = O4 기록 |
| 15 로컬 브라우저(CDP, AC-14-1 · 14-3 · 회귀) | `--host-resolver-rules="MAP localhost 127.0.0.1, MAP * ~NOTFOUND, EXCLUDE 127.0.0.1"`: ① 헤더 폭: 320 · 340 · 341 · 360 · 375 · 390 · 400 · 401 · 412 · 640 · 641 · 700 · 768 · 899 · 900 · 1024 · 1280px × (종만 · 가짜 `beforeinstallprompt`로 설치 보임 · 북마크 숫자 3자리 · 임시 계정 로그인) × 밝게/어둡게 ② 360×800 `/` → SW activated · 캐시 → `Browser.grantPermissions(notifications)` → `ServiceWorker.deliverPushMessage`(아침 JSON 평문 · 같은 것 두 번 · 시험 JSON) → `registration.getNotifications()` ③ 문서 전 가짜 pushManager(진짜 subscribe 0)로 종 누름 → 서버 POST → 다시 누름 → DELETE · 상태 4가지 그림 · `/widget#push` ④ v1 → v2: 세는 프록시가 표지 파일이 있을 때 `/sw.js`에 HEAD 판(v1, `git show HEAD:ex12/public/sw.js`)을 줘서 설치 → 표지 끄고 update(O4 심사 방식) ⑤ 회귀: O4 오프라인 안내 · 공유 · 맨 아래 `앱 설치`(400px 이하에서도 보임) · 401px 이상 헤더 설치 아이콘 · O1 `/?issue=` 상세창 · `/p` = 첫 화면(경로 `/p` 남음 · 상세창 0) · O2 로그인 창 · Google 버튼 이동 시도(이름 풀이 막힘 — Google 요청 0) · D20 좌표(HEAD index.html 비교) | ① 모든 폭 · 상태: `scrollWidth ≤ clientWidth`(가로 넘침 0) · 640px 이하 전환 버튼 높이 34(한 줄) · 오른쪽 버튼 제 폭(눌림 0) · 400px 이하 헤더 설치 아이콘 0 · 641px 이상 넘침 0(전환 줄바꿈은 8자 넘는 이름 + 641px만 허용 · 기록) · 종 아이콘 34×32 · 다크 = 토큰 ② v2 · 캐시 = offline.html 하나 · 아침 알림 1(두 번째는 앞 알림을 대체해 여전히 1) + 시험 1 = 제목 · tag · data.url `/p`(이 판이 답하지 않으면 [SKIP 사유] + 10번 VM으로 대신) ③ `off` → `on` → `off` · 토스트 · POST 본문 = 가짜 구독 · 시험 행 생김 → 지워짐 ④ v1 설치 → v2 활성 · 캐시 키 = v2만 · push 처리 있음 ⑤ O4 기록과 같음 · JS 예외 0 · 바깥 요청 0 · 진짜 subscribe 0 |
| 16 로컬 발송 경로(실제 DB 읽기 + 가짜 푸시 서비스) | 0007 적용 뒤 · `push_subscriptions`에 `o5-` 시험 꼴이 아닌 행이 있으면 멈춤 · 시험 행 3개(fcm · apple · mozilla 꼴, 스크립트가 만든 키) → 검증 스크립트가 `runPush`(실제 `pushStore` · 가짜 서비스 fetch · now = SQL 최근 수집일 07:00 KST · Actions 모드) 2번(첫 번째에 한 기기는 410) → `--test {코드}` 1번 → 실제 env로 `npm run push`(로컬 모드, `--import` 가로채기: fetch · 소켓 기록, 허용 호스트도 막음) 1번 → 시험 행 삭제 | 첫: 요청 3 · 풀린 본문 = 아침 JSON(4번과 같음) · 410 행 삭제 · 나머지 둘 last_sent_on = 그날 / 둘째: 요청 0 · `이미 보냄 2` / 시험: 그 1개만 · 표시 그대로 / `npm run push`: `키 확인 OK` · 결정 · 대상 수 · `로컬 실행: 보내지 않음` · 가로챈 fetch 0 / 끝에 시험 행 0 · 다른 9표 해시 같음 · 출력 누출 false |
| 17 안전 · 한도(로컬) | 요청 카운터 · 바깥 연결 기록 · 비밀값 검사(ex12/.env 비밀값 + `VAPID_PRIVATE_KEY`) · DB 해시 · 프로세스 | 3000 ≤ 100 · newsdata · OpenAI · Google · Kakao · 푸시 서비스 실제 요청 0 · 진짜 subscribe 0 · 누출 false · 9표 해시 같음 · 시험 행 0 · 소유 PID 0 · code-oss 살아 있음 |
| 18 배포 정적 · API(푸시 뒤, Node ≤ 20) | `https://issuerank-kappa.vercel.app`에 14번 정적 줄 + POST/DELETE(Origin = 배포 주소, `o5-deploy-{uuid}`) + DB 읽기 전용 확인 + Origin 없음 · 다른 호스트 | 14번과 같음(Vercel에서도 sw.js v2 · 헤더 3개 · `/p` rewrite) · 204 → 행 1 → 204 → 행 0 · 403 · 400 |
| 19 배포 브라우저(CDP, ≤ 25 — NetLog로 셈) | O4 줄 15 방식 새 프로필 360 · 320: `/` → SW v2 activated · 설치 오류 `[]` · 헤더 넘침 · deliverPushMessage → getNotifications · 가짜 pushManager로 종 상태(`/api/push/` 요청은 CDP Fetch로 막음) | v2 · 캐시 하나 · 넘침 0 · 알림 1(15번과 같음) · 배포 호스트 밖 요청 0 |
| 20 워크플로 연결(코디네이터, 푸시 뒤) | Secret 등록 · 푸시 뒤 **10:00~24:00 KST**에 `gh workflow run push.yml --repo Jwavascript/issuerank`(코드 비움) 1번 → 그 실행 요약 | push 실행 초록 · 요약 `키 확인 OK · 구독 n · 보내지 않음: 10:00 지남` · 요청 0 · daily 실행 · daily.yml 그대로 |
| 21 Android 실제(사용자, AC-14-1 · 14-3 · 14-4) | 아래 "사용자 확인 순서" 1~5 · 7 | 결과 · 캡처 전 [SKIP — 사용자 확인 대기](PASS 아님). 기대: 종 → 권한 창 → 칠한 종(켜짐) · 기기 코드 · 시험 알림 1~2분 안(`[시험] 오늘의 뉴스가 도착했어요` · 아이콘 · 상태 표시줄 배지 · 알림 머리 이름) · 한 번 더 보내면 앞 알림이 사라지고 새 알림이 소리와 함께 옴 · 누르면 첫 화면(앱 창인지 기록) · 끈 뒤 시험 = `코드에 맞는 기기가 없습니다` · 앱 안 브라우저 = 안내. 메뉴 이름 · 브라우저 호스트(Samsung 등)가 다르면 글만 고침 |
| 22 다음 날 아침 실제 발송(사용자 + 기록) | 푸시 다음 날 예약 하루 실행 뒤 push 실행 요약 · 휴대폰에 온 시각(순서 6) | 06:30~10:00에 `오늘의 뉴스가 도착했어요` 1통 · 요약 보냄 = 대상 · 같은 날 다른 실행이 있어도 2통 0 · daily 결과 무관 · 실제 시각 기록(10:00을 넘긴 날은 규칙대로 0통) |
| 23 iPhone(사용자) | 기기가 생기면 iOS 16.4+ 홈 화면 앱에서 켜기 · 시험 · 누르기 · Safari 탭 안내 · 같은 tag 대체 | 지금은 [SKIP — iPhone 기기 없음](PASS 아님) |
| 24 정책 문안(사용자, O2 21번과 함께) | 배포 `/privacy` 새 문장 · 위탁(GitHub) · 푸시 서비스 문장(순서 8) | "확인" 전 [SKIP — 사용자 문안 확인 대기] · 법률 검토 권장 · 고칠 점이 오면 문안만 고치고 12번 다시 |

- AC → 줄: AC-14-1 → 8 · 11 · 15 · 19 · 21(23 SKIP) / AC-14-2 → 1 · 5 · 6 · 7 · 16 · 20 · 22 / AC-14-3 → 2 · 3 · 4 · 10 · 14 · 15 · 19 · 21 / AC-14-4 → 5 · 8 · 11 · 14 · 18 · 21 / 회귀 AC-13-1 · 13-2 → 10 · 15 · 19 · AC-13-3 → 11 · 15 · AC-11-2 → 15 · AC-06-3 → 15 · AC-01-1 → 1 · 22.

### 사용자 확인 순서 (마지막에 한 번에 — 휴대폰 · GitHub 화면만)
- 미리(코디네이터가 함): Secret 등록 · 커밋 · 푸시 · 배포 줄 18~20.
1. Android Chrome(또는 설치한 이슈랭크 앱)으로 이슈랭크 → 오른쪽 위 **종 모양 버튼** → 허용 → 종이 칠해지고 `아침 알림을 켰습니다` 토스트(캡처).
2. `/widget#push` → `이 기기: 아침 알림 켜짐 · 기기 코드 xxxxxxxx` 확인(코드는 비밀 아님).
3. 시험 알림: GitHub › Actions › **push** › Run workflow › `test_code`에 그 코드 → Run(또는 코드를 코디네이터에게 알려 주면 `gh workflow run push.yml -f test_code=…`로 대신 실행) → 1~2분 안에 `[시험] 오늘의 뉴스가 도착했어요` 알림(아이콘 · 상태 표시줄 배지 · 알림 머리에 보이는 이름 캡처). 한 번 더 실행하면 앞 알림이 사라지고 새 알림이 소리와 함께 오는지.
4. 알림 누르기 → 이슈랭크 첫 화면이 열림(Chrome 탭인지 앱 창인지 기록).
5. (선택) 카카오톡 나와의 채팅에 이슈랭크 주소 → 앱 안 브라우저에서 종 버튼 → `/widget#push` 안내 · 메뉴 이름 기록.
6. 다음 날 아침: 알림이 온 시각 · 모양 + Actions › push 그날 실행 요약 캡처(22번).
7. 그 뒤 종을 한 번 더 눌러 끄기 → `아침 알림을 껐습니다` → (선택) 3번 다시 → 요약 `코드에 맞는 기기가 없습니다`(빨간 실행이 정상).
8. `/privacy` 새 문장 확인(O2 21번과 같이) → "확인" 또는 고칠 점(24번). iPhone이 생기면 23번 · 며칠 뒤 (선택) Vercel › Analytics › Pages에서 `/p`(기록만).

- 공식 근거(2026-10-20 확인): [RFC 8030](https://www.rfc-editor.org/rfc/rfc8030.html)(TTL 필수 · 201 · 만료 404 · 4,096B 이하 413 금지 · 429) · [RFC 8291](https://www.rfc-editor.org/rfc/rfc8291.html)(aes128gcm 한 레코드 · 평문 3,993B · 5절 예시) · [RFC 8292](https://www.rfc-editor.org/rfc/rfc8292.html)(aud · exp ≤ 24h · sub mailto/https · `vapid t= k=` · ES256 · 403) · [Apple 웹 푸시](https://developer.apple.com/documentation/usernotifications/sending-web-push-notifications-in-web-apps-and-browsers)(`*.push.apple.com` · JWT 하루 · 1시간 · 410 만료 · 4KB · iOS 16.4 홈 화면 · 보이지 않는 푸시 = 권한 회수 · 제스처에서 바로 · payload가 비면 Content-Encoding 생략 가능 · Developer Program 불필요) · [autopush](https://autopush.readthedocs.io/en/latest/http.html)(404 · 410 다시 쓰지 말 것 · TTL 0~2,592,000) · [web.dev 문제 해결](https://web.dev/articles/push-notifications-common-issues-and-reporting-bugs)(404 · 410 삭제 · Mozilla 키 불일치 404) · [MDN BCD showNotification](https://github.com/mdn/browser-compat-data/blob/main/api/ServiceWorkerRegistration.json)(renotify · badge = Chrome 계열만) · [GitHub workflow_run · schedule 지연](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows) · [Firebase 요금](https://firebase.google.com/pricing).
- 확인 필요: 알림 머리에 보이는 이름(Chrome 탭 = 사이트 주소인지 · 설치 앱 = `이슈랭크`인지) · 같은 tag를 닫고 다시 띄울 때 소리가 나는지 → 21 · Samsung Internet 푸시 호스트(fcm인지) · Edge(Windows) 주소 꼴 → 21 기록 · headless Chromium 155에서 `deliverPushMessage` → `getNotifications()`가 답하는지 → 15 · 19 · 알림을 누르면 설치한 앱 창으로 열리는지 → 21 · Android 13+ 앱 알림 권한과 Chrome 메뉴 이름 → 21 · Pretendard가 실제로 불린 폰에서 헤더 폭(계획 실측은 대체 글꼴, 여유 ≥ 12.6px) → 15 · 19 · 21 · 이 기기 Chromium의 진짜 FCM 구독(시도하지 않음) · iOS 홈 화면 앱의 Origin 헤더 · `getNotifications`/tag 동작 · Apple JWT 1시간 규칙 → 23 · `workflow_run`이 예약 실행 끝에 바로 오는지 → 22 · 법률(처리 위탁 문구) → 24.
