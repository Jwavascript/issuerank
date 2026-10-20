# ops O5 아침 알림(웹 푸시) — 제작 보고

- 날짜: 2026-10-20(KST, 11:32~12:40 · C2 반영 12:45~13:10 · 1199px 보강 13:15~13:40 · 심사 FAIL 수정 14:00~14:15, 15절) · 역할: 제작(`agent_prompts/2_build_agent.md`) · 기준: `plan/PLAN_ops_O5_morning_push.md`(확정), SPEC US-14 · AC-14-1~4(회귀 AC-13-1~3 · 11-2 · 06-3 · 01-1), PLAN.md D10 · D21~D30
- 사용자 지시(2026-10-20): "아침 푸시 알림 기능 기획 후 구현, 검증해줘. 내가 확인해야 하는 테스트는 나중에 한번에 할게." · 승인: SPEC · D30, 운영 DB 0007 적용, 새 `push.yml`, node:crypto 직접 구현(새 패키지 0), `/privacy` 문안(사용자 확인은 배포 뒤)
- 작업 폴더: `ex12/` + 새 `.github/workflows/push.yml`(D21, 복사 없음). 시작 HEAD `8aaf6bd`(origin과 같음). 시작 때 미커밋 5개(`HANDOFF.md` · `PLAN.md` · `SPEC.md` · `reports/ops_O4_pwa_share_judge.md` · `plan/PLAN_ops_O5_morning_push.md`)는 O5 코드가 아님 — 되돌리지 않았고 변경 범위 셈에서 뺌(이 보고 작성 전까지 다섯 파일 sha256 그대로, 끝에 HANDOFF · PLAN.md만 지시대로 갱신)
- 하지 않음: git commit · push · Vercel/GitHub 설정(Secret 포함) · Actions 실행 · `web/` · `plan/PLAN_web.md` · 계획 밖 기능. 새 npm 패키지 0(`package-lock.json` 해시 같음)
- 증거 · 검증 스크립트: `ex12/.data/o5-build-20261020/`(git 제외)

## 1. 바뀐 · 새 파일 (= 계획 "변경 범위" 목록과 정확히 같음)

| 파일 | 구분 | 내용 |
|---|---|---|
| `ex12/server/push-config.ts` | 새 | 공개 키 상수 · subject · 아침/시험 문구 · tag · url · 상한(5,000 · 4,096B) · 받는 푸시 서비스(정확히 2 + 접미사 2) · `pushService()` endpoint 검사 · `validKeys()`(65B · 0x04 · 곡선 위 점 / 16B) · `deviceCode()`(SHA-256 앞 8자) |
| `ex12/server/push-keys.ts` | 새 | `npm run push:keys` — P-256 키 쌍 → `.env`에 `VAPID_PRIVATE_KEY=` 한 줄(권한 600, 이미 있으면 멈춤 · 종료 2) → 화면엔 공개 키만 |
| `ex12/server/webpush.ts` | 새 | RFC 8291 aes128gcm(`encrypt`, salt · 서버 키 주입은 RFC 예시 시험용) · RFC 8292 VAPID(`vapidKey` = 개인 키로 계산한 공개 키가 상수와 같을 때만, `vapidAuthorization` ES256 ieee-p1363 · exp +12h) · `sendPush`(머리 5 · `redirect: 'error'` · 10초 · 2xx/404·410/응답 없음/그 밖) |
| `ex12/server/push-run.ts` | 새 | 규칙(06:30~10:00 · 03:30 전 건너뜀 · 06:30까지 기다림 · KST 오늘 랭킹 · 보내기 전 그날 표시 · 404/410 삭제 · 응답 없음 유지 · 그 밖 되돌림 · 10개씩) · `--test` · 로컬 모드 · 요약 · 종료 코드. 저장소 · latestDate · fetch · 시계 · sleep · env · 출력 주입 |
| `ex12/server/push.ts` | 새 | `npm run push` 명령(DB 연결 try 안 · DB 오류 = `hide()` + VAPID 값 *** · 첫 줄만) |
| `ex12/server/routes/push.ts` | 새 | `POST · DELETE /api/push/subscriptions`(Origin → 4KB(`hono/body-limit`) → JSON · 칸 → endpoint → 키 → 저장, 204/400/403/413/503, 모든 응답 no-store, 로그 `PUSH_SAVE_FAILED`만) |
| `ex12/drizzle/0007_push_subscriptions.sql` · `meta/0007_snapshot.json` | 새 | drizzle 생성(CREATE TABLE 1 · UNIQUE(endpoint) · ENABLE RLS뿐) |
| `ex12/public/icons/badge-96.png` | 새 | 96×96 흰 마크 · 투명 바탕(2절) |
| `ex12/test/push.test.ts` · `push-api.test.ts` · `push-sw.test.ts` · `push-ui.test.ts` | 새 | 16 · 10 · 8 · 15개(5절). C2 뒤 `push-ui.test.ts` CSS 기대값 1줄 더함 |
| `.github/workflows/push.yml` | 새 | `workflow_run`(daily completed) + `workflow_dispatch(test_code)` · concurrency push(취소 안 함) · contents: read · 200분 · checkout@v7 · setup-node@v7(24, npm 캐시) · `npm ci` · `npm run push`(Secrets · 입력은 env로만, 따옴표 배열 인수) · tee → STEP_SUMMARY → 그 코드로 끝 |
| `ex12/server/app.ts` | 고침 | Store에 3개(`savePushSubscription` · `deletePushSubscription` · `pushSubscriptionCount`) · 라우트 연결 1줄 |
| `ex12/server/db/schema.ts` | 고침 | `push_subscriptions` 표(6칸 · RLS) |
| `ex12/server/db/store.ts` | 고침 | dbStore 3개 + 발송용 `pushStore`(목록 · 그날 표시(아직 그날이 아닐 때만) · 되돌림(아직 그날일 때만) · 삭제) |
| `ex12/server/sample.ts` | 고침 | 메모리 구독 표 · memoryStore 3개 · `memoryPushStore` · `resetSample`이 비움 |
| `ex12/drizzle/meta/_journal.json` | 고침 | 0007 항목 1 |
| `ex12/public/sw.js` | 고침 | `issuerank-v2` · `push`(검사 → 아니면 아침 값 · 같은 tag 닫고 show 1) · `notificationclick`(같은 출처 창 focus + navigate, 실패/없음 → openWindow). install · activate · fetch 글자 그대로 |
| `ex12/public/index.html` | 고침 | 헤더 `<a id="pushBtn">`(설치 · 북마크 사이) · CSS(Header 6줄 · Responsive 3줄 + **C2 1줄**(주석 1줄 포함, 9절 ①) — `@media (max-width: 1199px) { #installBtn .lbl, #bmNav .lbl { display: none; } }` — 처음 1099px → 코디네이터 보강으로 1199px) · 새 구역 `/* ─── Push (O5)`(Toast 뒤 · Share · install (O4) 앞) |
| `ex12/public/widget.html` | 고침 | `<h2 id="push">아침 알림</h2>` 절(앱 절 뒤 · iPhone 앞) + 상태 한 줄 스크립트(별도 `<script>`, 서버 요청 0) |
| `ex12/public/privacy.html` | 고침 | 초안 문장 그대로(1 · 2 · 3 · 5 · 6항) · 시행일 · 12항 변경 줄(3절 주의) |
| `ex12/next.config.mjs` | 고침 | rewrite `/p` → `/index.html` 1개 |
| `ex12/tools/pwa-images.tsx` | 고침 | 7번째 출력 `icons/badge-96.png`(Badge 컴포넌트), 기존 6개 그대로 |
| `ex12/openapi.yaml` | 고침 | `/api/push/subscriptions` post · delete · 응답 3 · 스키마 2 · info.version 0.5.0 |
| `ex12/package.json` | 고침 | scripts `push` · `push:keys` 2줄(+ `daily` 줄 끝 쉼표) |
| `ex12/.env.example` | 고침 | `VAPID_PRIVATE_KEY=` 이름 + 설명 |
| `ex12/test/pwa.test.ts` | 고침 | 계획의 두 가지만: ① 캐시 v1 → v2(제목 · install · activate · 남은 캐시) + 옛 캐시 목록에 `issuerank-v1` ② widget 앱 절 끝 경계 `<h2>iPhone</h2>` → `<h2 id="push">`(앱 절 기대 문구 그대로) |

지운 것: 없음(기존 코드 · 문구 삭제 0, sw.js 머리 주석 1줄만 v2 설명으로 고침). 보호 58파일(검증 1 목록) 작업 전후 sha256 같음.

## 2. VAPID 키 · 배지 그림

- 키: `bash scripts/ex12-ubuntu.sh node .data/o5-build-20261020/safe-run.mjs push-keys -- npm run push:keys` 1번(출력 누출 검사 false 뒤에만 표시). 공개 키 `BH8iVJ-iq1b6TqxcnK4kkV0R7ZuD_qeF0zHKgA864mWlRbhi3qlBuQD8d-N95LThbBc-OuraowXCbrzLfWS-AN0`(87자 · 곡선 위 점) → `push-config.ts` · `index.html` `PUSH_KEY`(테스트가 같은지 비교). 개인 키는 `ex12/.env`에만(줄 1개 · 43자 · 권한 600 · git 제외 `.gitignore:4:.env` 확인), 개인 키로 계산한 공개 키 = 위 값(true만 출력). 두 번째 실행 = `이미 있습니다 · 바꾸지 않았습니다` 종료 2 · `.env` 해시 그대로. 테스트는 실행마다 만든 일회용 키(`push.test.ts`가 상수만 그 공개 키로 바꿈) + RFC 8291 예시 키만 씀.
- 배지: `npx tsx tools/pwa-images.tsx .data/o5-build-20261020/gen1` · `gen2` 두 번(`public/`에는 쓰지 않음 → 배지만 복사). 7개 모두 두 번 바이트 같음, 기존 6개 = 커밋 파일 sha256(그대로), 실행마다 `dataFetches 1`(내장 wasm) · `blocked 0`. `badge-96.png` 96×96 RGBA 611B sha256 `0417d4cd…f191442`: 투명 4,456 · 불투명 흰 4,091 · 반투명 흰 669(가장자리 + 흐린 칸 45%) · 흰색 아닌 픽셀 0 · 마크 72px 가운데(12~83). 직접 봄: `badge-preview.png`(어두운 바탕 · 상태 표시줄 꼴 4배) — 사이트 3칸 마크 그대로.

## 3. DB 0007 적용(운영 DB, 승인됨)

- 생성 SQL을 먼저 읽음: `CREATE TABLE "push_subscriptions"(id text PK · endpoint text NOT NULL · p256dh · auth · created_at timestamptz default now() NOT NULL · last_sent_on date · CONSTRAINT push_subscriptions_endpoint_unique UNIQUE(endpoint))` + `ALTER TABLE "push_subscriptions" ENABLE ROW LEVEL SECURITY` — 새 표에만. DROP · DELETE · 기존 표 ALTER 0. 두 번째 generate "No schema changes".
- 적용: `safe-run … npm run db:migrate` 종료 0 · 누출 false. 적용 바로 전 · 바로 뒤 스냅숏(읽기 전용 트랜잭션): 기존 9표 건수 · 내용 md5 같음(articles 1279 · issues 897 · collect_runs 102 · bookmarks 2 · folders 2 · user 2 · session 2 · account 1 · verification 0), 마이그레이션 기록 7 → 8행(앞 7행 같음, 새 행 hash = 0007 파일 sha256 · created_at = journal when). 새 표: 열 6 · 제약 = pkey + endpoint unique · 인덱스 2(고유) · RLS 켜짐(force 아님) · 정책 0 · 행 0. 검증이 다 끝난 뒤 최종 스냅숏도 9표 같음 · 행 0.
- 되돌리기(필요할 때만 · 승인): `DROP TABLE push_subscriptions` + 기록 1행.

## 4. PLAN 항목별 반영 위치

| 계획 | 반영 |
|---|---|
| 보내는 시각 · 방식(Q1 · Q3) | `push-run.ts` 규칙 · `push.yml`. 종료 0/1/2 · 로컬 모드(`GITHUB_ACTIONS` ≠ true면 fetch · 표시 · 삭제 · sleep 0) · 요약 줄(endpoint · 키 · id · 기기 코드 0) |
| 발송 구현(Q2) · payload 유지 | `webpush.ts`(node:crypto만) — RFC 예시 바이트 일치(5절) |
| 키(VAPID) | `push-keys.ts` · `push-config.ts` · `.env.example` · `vapidKey()`(다르면 fetch 0 · 종료 2) |
| 구독 저장 · API · SSRF | `schema.ts` · `store.ts` · `sample.ts` · `routes/push.ts` · `app.ts` · `openapi.yaml` |
| 알림 내용 · 누르면 | `push-config.ts`(문구 · tag · url) · `sw.js` · rewrite `/p` · 배지 |
| 서비스 워커 | `sw.js` v2(push · notificationclick, O4 처리 그대로) |
| 화면(Q5) | `index.html` 헤더 · CSS 4규칙 · Push 구역 · 토스트 8개 · `widget.html#push` |
| 개인정보 | `privacy.html` 초안 그대로 |
| 워크플로 | `.github/workflows/push.yml` |
| 테스트 | 새 4파일 + `pwa.test.ts` 두 가지 |

## 5. 테스트 · 빌드 · 계약

- `bash scripts/ex12-ubuntu.sh npm test`: **26파일 268/268**(기존 22파일 219 그대로 — 기대값 바꾼 곳 = `pwa.test.ts` 두 가지뿐, 새 49 = push 16 · push-api 10 · push-sw 8 · push-ui 15). vitest가 추적 파일을 바꾸지 않음(git status 전후 같음).
- `npm run build`(next build + `tsc --noEmit`, 안전 실행): 성공 · 누출 false. 두 번(코드 완성 뒤 · 끝).
- **C2(1099px 판) 반영 뒤 다시**(12:5x): `npm test` 26파일 268/268 · vitest 추적 파일 변경 0 · `npm run build` 성공(누출 false). index.html 차이 = 주석 1 + 규칙 1줄뿐(VM 테스트가 잘라 쓰는 구역 · 헤더 마크업 · `start();` 표지 글자 그대로 — 비교 스크립트로 확인).
- **1199px 보강 뒤 다시**: 규칙 줄 숫자 1개(1099 → 1199) + `push-ui.test.ts` 기대값 1줄만 바뀜(diff 확인) → `npm test` 26파일 268/268 · vitest 추적 파일 변경 0 · `npm run build` 성공(누출 false). 주석 줄은 지시대로 그대로라 "1099px 이하는…" 글자가 남음(14절 5).
- 번들(검증 13): `.next/server` 전체에서 `WebPush: info` · `Content-Encoding: aes128gcm` · `Content-Encoding: nonce` · `vapid t=` · `PUSH_PAYLOAD_TOO_LARGE` 0파일(발송 코드 0) / 구독 라우트(`PUSH_SAVE_FAILED` · 호스트 목록 · 503 문구)는 `/api/[...path]` 청크에 있음.
- `npx --offline @redocly/cli@2.64.1 lint openapi.yaml`: 오류 0 · 경고 20(HEAD 20과 같음 — 새 경로 경고 0) · 0.5.0.
- 돌연변이 확인(제품 파일을 잠깐 바꿔 해당 테스트 실행 → 되돌림 · 해시 같음): HKDF info 한 글자 → push 4 실패 · 접미사 점 빼기 → push-api 1 실패 · 응답 없음에 되돌림 → push 1 실패 · 같은 tag 닫기 뺌 → push-sw 1 실패 · subscribe 앞에 한 틱 → push-ui 4 실패. 되돌린 뒤 60/60.

## 6. 검증 줄(PLAN 1~24)

| # | 결과 | 근거(요약) |
|---|---|---|
| 1 | OK | 바뀐 · 새 = 1절 목록뿐 · 보호 58파일 해시 같음(`hash-before/after.txt`) · lockfile 같음 · package.json scripts 2줄 · ex09 · `web/` 변경 0 · SPEC · PLAN.md는 0단계분(작업 중 해시 그대로) |
| 2 | OK | RFC 8291 부록 A 입력 → 5절 본문 base64url과 바이트 같음 · 144B · 머리 86B(salt · 4096 · 65 · 서버 공개 키) · WebCrypto로 풀림 / 무작위 두 번 다름 · 둘 다 풀림 · 3,994B 거절 |
| 3 | OK | 4종 endpoint: `vapid t=…, k={코드 공개 키}` · 머리 `{"typ":"JWT","alg":"ES256"}` · aud = origin · exp = 지금 + 43,200 · sub · 서명 64B를 `crypto.verify`(ieee-p1363) · `subtle.verify` 둘 다 참 / 다른 키 · 형식 틀림 → `VAPID 키가 코드의 공개 키와 다릅니다` · 없음 → `VAPID_PRIVATE_KEY가 없습니다` · fetch · 목록 0 · 종료 2 · 키 문자열 0 |
| 4 | OK | 구독자 3(WebCrypto 키) · 2026-10-07 샘플: POST · 머리 = 표 · `redirect: 'error'` · AbortSignal · ≤ 4,096B · 풀린 JSON = 아침 JSON 정확히 · 샘플 제목 · 섹션 이름 · 숫자 0 |
| 5 | OK | 10기기(201 · 404 · 410 · 400 · 403 · 413 · 429 · 500 · 연결 실패 · 응답 없음) → 404 · 410 삭제 · 201 · 응답 없음만 그날 표시 · 6 되돌림 · 종료 1 · 요약 숫자 = 표 / 두 번째 = 되돌린 6만 / 다음 날 = 남은 8 / 출력에 endpoint · 키 · id · 기기 코드 0 · 보내기 직전 재검사(받지 않는 endpoint · 깨진 키 → 보내지 않고 삭제) |
| 6 | OK | 07:00 · 09:59:59 보냄 · 10:00 · 23:59 `10:00 지남` · 03:29:59 · 00:10 `너무 이른 시각` · 어제 · 없음 `새 데이터 없음(최근 수집일 …)` · 05:10 → sleep 80분 1번 · latestDate 2번 · 보냄 · 03:30 → 180분 · 기다린 뒤 랭킹 사라짐 → 건너뜀 · D8 보냄 · 로컬(없음 · false) = 결정 · 대상 수 · `로컬 실행: 보내지 않음` · fetch · 표시 · 삭제 · sleep 0 |
| 7 | OK | `--test`(대문자도 됨) 11:00 · 최근 수집일 어제 → 그 기기만 1 · 시험 JSON · 그날 표시 그대로 · latestDate · sleep 0 / 0개 · 2개(SHA-256 앞 8자 충돌 쌍을 실행 중에 찾음) · `xyz` · 7자 · 9자 → 요청 0 · 종료 2 · 각 문구 / 로컬 = 보내지 않음 / 다른 인수 → 종료 2 |
| 8 | OK → 심사 FAIL → **고침(15절)** | 제작 때 놓친 것: 원형 속성 호스트(`constructor` · `__proto__`)가 통과하던 검사(심사 발견) — 15절에서 1줄 고침 · 테스트 추가. 4호스트 204(본문 0 · no-store · Set-Cookie 0 · 세션 없이) · 같은 endpoint = 행 1 · 키만 갱신 · 400 35가지(본문 31 + 깨진 JSON 4: http · 꼬리 호스트 · `evilpush.apple.com` · `.push.apple.com` · IPv4 · IPv6 · localhost · `u@` · 빈 `@` · `:8443` · `:443` · 옛 GCM · 스테이징 · 끝 점 · # · 공백 · 제어 문자 · 역슬래시 · 2,049자 · 키 형식 · 곡선 밖 점 · 표준 base64 · auth 15/17B · 배열 · null) · 2,048자 204 · 403(Origin 없음 · 다른 사이트 · https/IP 다름 · `null`, BETTER_AUTH_URL = 배포면 배포 Origin만) · 4,097B 413 · 4,096B 지나감 · Origin이 크기보다 먼저 · 5,000행 → 새 503 · 기존 204 · 저장소 실패 500 + 로그 `PUSH_SAVE_FAILED` 한 줄 · DELETE 있음/없음/두 번 204 · 400 · 403 · GET · PUT · PATCH 404(no-store) · createBff 지나도 같음 · 기존 api · auth · ownership 테스트 그대로 |
| 9 | OK | 3절 |
| 10 | OK | push 아침 · 시험 → waitUntil 1 · show 1 = 표 값 · renotify · actions · image · silent · vibrate · requireInteraction 0 / 데이터 없음 · JSON 아님 · 칸 빠짐 · 숫자 · tag `ISSUE!` · 65자 · 바깥 url · `//` · 상대 · 300자 제목 · 빈 제목 · 201자 body · null → 아침 값 / 같은 tag close 1 → show · getNotifications 실패여도 show 1 / 클릭: close 1 · matchAll(window · includeUncontrolled) · focus → navigate(`http://localhost:3000/p`) · 실패 → openWindow · 없음 → openWindow · 다른 출처 창 0 · data.url 틀림 → /p / 캐시 v2 · cache.put 0(`pwa.test.ts` v2) |
| 11 | OK | ① 지원 안 함 3종 unsupported · 라벨 · preventDefault 0 ② subscribe = 누른 같은 틱 · `{userVisibleOnly: true, applicationServerKey: 65B = 코드 키}` → POST 본문 = toJSON() · on · `아침 알림 끄기` · 토스트 ③ denied → POST 0 · blocked / 창 닫음(default) → off · 그 밖 오류 문구 ④ 400 · 503 · 500 · 네트워크 → 각 토스트 · unsubscribe 1 · off ⑤ 끄기 → unsubscribe 1 → DELETE `{endpoint}` → `아침 알림을 껐습니다` + 다시 켜기(누르면 subscribe 1) · DELETE 실패여도 off ⑥ 차단 → 토스트 + 방법 보기 → `/widget#push` ⑦ 등록 전 `잠시 후 다시 눌러 주세요` · subscribe 0 ⑧ 두 번 → subscribe 1 / share-ui · widget-ui · google-ui 그대로 |
| 12 | OK | widget `#push` 위치 · 문구 = 표 · 상태 VM(켜짐 = 서버 `deviceCode()`와 같은 8자 · 꺼짐 · 등록 없음 · 지원 안 함, fetch 0) · privacy 새 문장 = 초안 · 7항 그대로 · 시행일 · 변경 줄 · 이메일 꼴은 문의처 안 2개뿐 · 그림 7개 두 번 같음 · 기존 6 = 커밋 바이트 · 배지 픽셀 검사 · fetch는 `data:`뿐 |
| 13 | OK | 5절 |
| 14 | OK | `api.mjs` 17/17(로컬 15요청): sw.js 헤더 3 = O4 · 본문 = 파일(v2) · `/` · `/p` = index.html · 헤더 = O4 html · badge 200 image/png 바이트 같음 · widget · privacy = 파일 / 실제 DB: POST 204 → 행 1(키 같음 · last_sent_on null) → 다른 키 POST 204 → 행 1(키 바뀜 · created_at 그대로) → DELETE 204 → 행 0 / Origin 없음 403 · 다른 호스트 400 · 4,097B 413 · 모두 no-store · 그 뒤 행 0 / 회귀 widget · rankings · caption 상태 · 헤더 = O4 기록 |
| 15 | OK — C2(1199px) 적용 뒤 다시 잼(9절 ①) | 실제 Chromium headless + CDP 9번(A · B · F = 로컬 서버, C · D · E · G · H · I = file:// 요청 0). ① 17폭 × 5상태 × 밝게/어둡게 × 모바일/데스크톱 = 340경우: 가로 넘침 0 · 400px 이하 헤더 설치 0 · 종 34×32 · 상태 색 · 칠 · 사선 · 불투명도 = 토큰 · 폰 폭(모바일) 640px 이하 전환 한 줄 · 눌림 0 · 641px 8자 이름만 전환 줄바꿈(기록, HEAD도 같음). 기록: 데스크톱 꼴 320px + 스크롤바 15px + 북마크 3자리에서 전환 두 줄(9절 ②) · C2 전 641~1024px 글자 줄바꿈(9절 ①) → **C2(1199px) 뒤 file:// 340경우 다시(요청 0)**: 모바일 0건 · 641~1280px 꺾임 · 넘침 · 겹침 0 · 설치 · 북마크는 641~1199px 아이콘(+숫자) · 1200px 이상 글자 · 남은 것 = 데스크톱 320px 6경우(②, C2와 무관) · 641px 8자 이름 전환 줄바꿈은 데스크톱 꼴에만 ② v2 activated · 캐시 = offline.html 하나 · `ServiceWorker.deliverPushMessage` 답함 → `getNotifications()` 아침 1 → 같은 것 다시 1(대체) → 시험 더해 2 · 제목 · body · tag · icon · badge · data.url `/p` · lang ko(대신 방법 불필요, SW 쪽 기록기 3건도 같음) ③ 가짜 pushManager(문서 전 스크립트, 진짜 subscribe 0)로 종 → POST 204 → DB 시험 행 1 → `/widget#push` `켜짐 · 기기 코드 566e2c11` · 다크 → 다시 종 → DELETE 204 → 행 0 · 권한 denied → 거절 · blocked · 방법 보기(서버 요청 0) · 상태 4가지 그림 ④ HEAD 판(v1, 프록시 표지) 설치 → 캐시 v1 · push 처리 없음 → update() → v2 활성 · 캐시 v2만 · `readPush` 있음 ⑤ 맨 아래 `앱 설치` 320 · 360 · 400 보임 · 공유 복사 · 진짜 설치 이벤트 412 보임 · 360 숨김 · 누르면 prompt 1 · `/s?issue=` · `/?issue=` 상세 · `/p` = 첫 화면(경로 /p · 상세 0) · 로그인 창 · Google 버튼(로컬 GOOGLE_* 없음 → 사용할 수 없음 문구 · 이동 0) · 북마크 화면 · 오프라인 안내 → 다시 시도 · D20 타일 좌표 360 · 768 · 1280 = HEAD(30개씩) · JS 예외 0 · 바깥 요청 = jsdelivr 글꼴 시도(이름 풀이 실패)뿐 |
| 16 | OK | `push-local.mts` 6/6(실제 pushStore · latestDate · 실제 VAPID 키는 메모리에서만): 시험 행 3(fcm · apple · mozilla) → 1번째 요청 3 · WebCrypto로 풂 = 아침 JSON · 머리 = 표 · JWT 서명 · aud · exp · sub · k = 코드 키 모두 참 · 410(mozilla) 행 삭제 · 나머지 2 그날 표시 · 요약 줄 = 표 / 2번째 요청 0 · `구독 2 · 이미 보냄 2 · 대상 0` / `--test` fcm만 · 시험 JSON · 표시 그대로 / 출력에 endpoint · 키 · id · 코드 0 / 실제 무응답(메모리 저장소, DB 쓰기 0) 10,012ms 뒤 `응답 없음 1` · 표시 유지 · 종료 1. 실제 `npm run push`(진짜 .env · 로컬 모드 · `--import` 가로채기 · 시험 행 2): `키 확인 OK` · `구독 2 · 이미 보냄 0 · 대상 2` · `보내지 않음: 10:00 지남` · `로컬 실행: 보내지 않음` · 종료 0 · fetch 0 · 막힌 시도 0 · 연결 = DB 소켓 1 + 로컬뿐 → 시험 행 삭제 · 남은 0 · 9표 해시 같음 |
| 17 | OK | 10절 |
| 18 | SKIP — 푸시 후 | 11절 절차 |
| 19 | SKIP — 푸시 후 | 11절 절차 |
| 20 | SKIP — 코디네이터(푸시 뒤 10:00~24:00 KST) | 11절 절차 |
| 21 | SKIP — 사용자 확인 대기 | 12절 1~5 · 7 |
| 22 | SKIP — 사용자 + 기록(푸시 다음 날) | 12절 6 |
| 23 | SKIP — iPhone 기기 없음 | — |
| 24 | SKIP — 사용자 문안 확인 대기 | 12절 8 |

합계 **OK 17 / FAIL 0 / SKIP 7**(18 · 19 · 20 푸시 후 · 코디네이터, 21 · 22 · 24 사용자, 23 iPhone) — C2 반영 뒤에도 같음. 독립 심사(13:58) = OK16/FAIL1/SKIP7(줄 8) → 줄 8 고침(15절), 심사 9절 ① 다시 확인 대기.

## 7. 화면 — 눈으로 본 결과(`ex12/.data/o5-build-20261020/`)

- `ui-header-360-light.png` · `-dark.png` · `ui-header-1280-light.png`: 위에서 off(선 종) · on(칠한 종) · blocked(사선) · unsupported(흐림) — 밝게/어둡게 토큰대로, 360 한 줄. `ui-header-320-install-123.png`: 설치 가능 + 123이어도 헤더 설치 아이콘 없음 · 한 줄.
- `ui-360-bell-on.png` · `ui-360-bell-blocked-toast.png`(토스트 `알림이 차단되어 있습니다 · 방법 보기`) · `ui-360-widget-push.png` · `-dark.png`(절 · 상태 줄 · 기기 코드) · `ui-1280-login.png` · `ui-360-p.png`.
- `ui-d-header-960-head-vs-now.png` · `ui-d-header-641-head-vs-now.png`(위 = HEAD, 아래 = C2 전): 9절 ①의 글자 줄바꿈 그대로 보임.
- **C2(처음 1099px 판) 뒤** `ui-g-header-960-head-vs-c2.png` · `-768-` · `-641-` · `-1100-`(위 = HEAD, 아래 = C2, 데스크톱 꼴 스크롤바 15px): 960은 HEAD도 `앱 설/치` · `북마/크`로 꺾이는데 C2는 설치 아이콘 · 종 · 북마크 아이콘 + 42 · `홍길동 · 로그아웃` 그대로 · 768 · 641 꺾임 없음 · 1100(설치 + 8자 이름 + 42)은 1099px 판 범위 밖이라 꺾였음(→ 1199px 보강 이유).
- **1199px 보강 뒤** `ui-i-header-1150-head-vs-1199.png` · `ui-i-header-1200-head-vs-1199.png`(설치 + 8자 이름 + 42, 데스크톱 꼴): 1150 = 설치 아이콘 · 종 · 북마크 아이콘 + 42 · 이름 전체 · 꺾임 0, 1200 = `앱 설치` · 종 · `북마크 42` · 이름 전체 · 꺾임 0(HEAD와 같은 꼴에서 종만 더해짐).
- `badge-preview.png`(2절).

## 8. 꼭 필요했던 내부 세부 · 계획과 다른 점(보고)

1. 로컬 서버는 계획 방식(next start 3001 + 3000 세는 프록시)을 씀 — 요청 상한을 세고, ④ v1 판 · D20 HEAD 비교용 표지(`sw-v1` · `index-head`)를 주려고(코디네이터 문구 "next start -p 3000"과 다름).
2. 15 ①의 "임시 계정 로그인" 상태는 페이지 상태(`state.loggedIn` · `renderUser()`)로 만듦 — 계획 한도 "임시 계정 0 · 다른 9표 쓰기 0"을 지키려고. 이름 `홍길동` · 8자 이름 두 가지.
3. `sw.js` push: `getNotifications` 단계가 실패해도 `showNotification`은 꼭 부르게 `.catch(() => {})` 1개(계획 "모든 push에 알림 1개"). 클릭 때 `data.url`도 같은 검사(틀리면 `/p`).
4. endpoint 검사 세부: 역슬래시 · 빈 사용자 정보(`@`) · `:443`(포트 표기) · IPv6 거절, 키 base64url 끝 `=`는 허용(길이로 검사). 보내기 직전 재검사는 키까지 포함(못 지나면 삭제 → 요약 끝에 `검사 실패 삭제 n`, 0이면 안 보임). 겹친 실행이 먼저 표시한 기기는 건너뜀(`다른 실행이 먼저 표시 n`, 0이면 안 보임).
5. 명령 문구(계획에 없던 것): `--test <기기 코드>만 받습니다` · `보내지 않음: 시험 코드는 8자 16진수입니다` · `…코드에 맞는 기기가 둘 이상입니다` · `시험 알림 · 구독 n · 코드에 맞는 기기 n` · `DATABASE_URL이 없습니다`. 시험 코드는 앞뒤 공백을 빼고 소문자로 봄. 로컬 모드는 06:30 대기 줄만 찍고 기다리지 않음, 건너뛰는 결정이어도 마지막 줄 `로컬 실행: 보내지 않음`.
6. 구독 API DB 오류 = 500 `Internal Server Error`(기존 onError와 같은 꼴) + `PUSH_SAVE_FAILED`. DELETE도 endpoint 검사(받지 않는 호스트 400).
7. 실제 payload 92B(본문 195B, 시험 225B) — 계획 "약 120B"보다 작음.
8. `/privacy` 시행일 · 변경 줄 = **2026년 10월 20일**(오늘 배포 예정 기준). 다른 날 배포면 세 곳(머리 시행일 · 12항 적용일 · 변경 줄) + `test/push-ui.test.ts`의 같은 기대 문자열만 고칠 것.
9. `push.test.ts`는 `vi.mock`으로 `VAPID_PUBLIC_KEY`만 일회용 키로 바꿈 — 제품 코드에 키 주입 · 허용 호스트 끄기 경로 없음(발송 시험은 주입 fetch → 가짜 서비스).
10. 9절 ① C2는 **코디네이터 판단(2026-10-20 12:45 KST, 1099px)**으로 넣고, 이어서 **코디네이터 보강(10-20, 1199px)**으로 기준을 넓힘(둘 다 사용자 승인 아님 — 사용자에게는 결과를 알리고 12절 확인 목록에 넣음). 계획 파일 `## 화면 변경`에 한 줄 덧붙임(641~1199px).

## 9. 발견 · 처리

① **641~1024px 헤더 글자 줄바꿈(눌림) → C2 적용(코디네이터 판단 10-20) → 1199px로 보강(코디네이터 판단 10-20)** — 계획 전제("641~767px 설치 보임 눌림은 HEAD보다 작음")와 일부 다름. 종(34px + 사이 6~8px)이 더해져, 오른쪽 칸이 모자랄 때 알약 버튼의 `북마크` · `앱 설치` 글자가 두 줄로 꺾여 32px 버튼 밖으로 나옴(가로 넘침은 0 — 계획의 641px 이상 기준은 충족). file://로 HEAD와 같은 상태를 잼(176경우, 요청 0, `ui-c-result.json`): HEAD보다 나아짐 34경우(641~899px 설치 보임 — 계획 규칙 효과) · **나빠짐 30경우**: 641~660px 비로그인(종 + 북마크 숫자, 최대 16.8px 모자람) · 700~768px 로그인(최대 11.1px) · **900~1024px 설치 가능 + 로그인 · 숫자(최대 30.7px, 900~960px)**. 1100px 이상은 0. 그림 `ui-d-header-960-head-vs-now.png`. 고침 후보를 런타임 주입으로만 잼(제품 파일 변경 0, `ui-e-result.json`, 336경우): 지금 160경우 · 최대 30.7px → **C2 `@media (max-width: 1099px) { #installBtn .lbl, #bmNav .lbl { display: none; } }`** 4경우(1100px · 설치 + 8자 이름만) · C1(설치 <1100 · 북마크 <800) 48경우. 처음엔 계획 "④ 북마크 · 641px 이상 … 그대로"와 달라져 넣지 않고 보고함 → **코디네이터가 C2로 결정(10-20 12:45 KST)** → `index.html` 반응형 절에 주석 1 + 규칙 1줄(처음 `@media (max-width: 1099px) { … }` → **코디네이터 보강으로 `@media (max-width: 1199px) { #installBtn .lbl, #bmNav .lbl { display: none; } }`**), `push-ui.test.ts` 기대값 1줄. 아이콘만 남는 두 버튼의 이름: `aria-label`이 그대로 있음(`이슈랭크 앱 설치` · `북마크`), `title`은 없어 마우스 툴팁은 없음(보고만 — 더하지 않음). 실제 Pretendard 글꼴에서는 폭이 다를 수 있음(측정은 대체 글꼴 — 계획 단계와 같음).
   **C2(처음 1099px 판) 뒤 다시 잼(전부 file:// — 서버 요청 0, `ui-g-result.json` · `ui-h-result.json`)**: (a) 줄 15 표 340경우(17폭 × 5상태 × 밝게/어둡게 × 모바일/데스크톱 — 데스크톱은 스크롤바 15px 강제): 모바일 문제 0 · 641~1280px 꺾임 · 넘침 · 겹침 0 · 설치 40px · 북마크는 641~1099px 아이콘 + 숫자 · 1100px 이상 글자 · 남은 문제 = 데스크톱 320px + 북마크 3자리 6경우(②, C2와 무관). (b) 641~1280px 15폭 × 24상태 × 2꼴(720경우씩) 눌림: HEAD 259 · C2 전 339 → **C2 16**, 1099px 이하 0. 남은 16(폭을 20px 간격으로 더 잘게 보면 29/240)은 **모두 1100~1180px · 설치 보임 · 8자 이름(로그인)**이고 HEAD는 그중 1경우만 1.9px → **회귀(종 때문, 아주 좁은 경우)**. 3자 이름 · 비로그인은 1100px 이상 0. 참고로 1199px 판을 런타임으로 넣으면 0(파일 변경 안 함 — 원하면 결정). (c) 640px 이하 · 1100px 이상 배치(브랜드 · 전환 · 오른쪽 버튼 사각형) = C2 전과 130경우 모두 같음. (d) 그림 960 · 768 · 641 · 1100 직접 봄(7절).
   **1199px 보강 뒤 다시 잼(file:// — 서버 요청 0, `ui-i-result.json`)**: (a) 340경우: 모바일 문제 0 · 641~1280px 꺾임 · 넘침 · 겹침 0 · 남은 것 = 데스크톱 320px 6경우(②) (b) 641~1280px 21폭 × 24상태 × 2꼴(1,008경우씩) 눌림: HEAD 259 · C2 전 352 → **지금 0**(1099px 판에서 남던 1100~1180px · 설치 · 8자 이름 29경우도 0) (c) 640px 이하 · 1200px 이상 배치 = C2 전과 130경우 모두 같음 (d) 그림 1150 · 1200 직접 봄(7절). **참고(판정 제외 · 고치지 않음) 12자 이름**: 영문 12자(`ABCDEFGHIJKL`) = HEAD · 지금 모두 문제 0. 한글 6 + 영문 6(`가나다라마바ABCDEF`) = **기존**(HEAD도 1100~1140px · 설치 + 북마크 숫자에서 글자 꺾임 10경우, 최대 11.5px) → 지금은 1100~1199px 0이고 글자가 다시 보이는 1200~1220px · 설치 + 42/123에서 7경우(최대 7.9px, 그 폭에서 HEAD는 0) — 같은 기존 문제가 종 폭만큼 위 폭으로 옮겨 간 것. 넘침 · 겹침은 HEAD · 지금 모두 0, 이름 말줄임은 지금 5 · HEAD 9경우.
② 데스크톱 꼴(스크롤바 15px) 320px + 북마크 3자리에서 전환 버튼 두 줄(약 2.4px 모자람) — 계획 단계의 "스크롤바 0/15px 전환 한 줄"과 다름. 휴대폰은 덮개 스크롤바(0px)라 해당 없음, 데스크톱 브라우저 창은 보통 이 폭까지 줄지 않음 → 기록만.

## 10. 요청 · 비용 · 프로세스

- 로컬 3000: **83/100**(api.mjs 15 · CDP A 43 · B 16 · F 9, C · D · E · G · H · I는 file:// 0 — C2 · 1199px 재검증 요청 0) + 처음 켤 때 3001 직접 확인 1(프록시 밖). 막힘 0. CDP 9번(포트 9365~9373), 매번 새 프로필 → 삭제 확인.
- 바깥: newsdata · OpenAI · Google · Kakao · 배포 주소 · GitHub API 0 · **실제 푸시 서비스 0 · 진짜 subscribe 0**. Chromium = `MAP * ~NOTFOUND`(바깥 = jsdelivr 글꼴 CSS 시도뿐, 이름 풀이에서 실패) · next 서버 바깥 연결 = DB 소켓 3뿐(`outbound.jsonl`) · Node 검증은 주입 fetch → 127.0.0.1 가짜 서비스만(전역 fetch 막음, 시도 0) · `npm run push`(진짜 .env) fetch 0 · DB 소켓 1 · redocly는 `npx --offline`.
- DB 쓰기: 0007 적용 1번 + `push_subscriptions` 시험 행(`/o5-build-`) 7개(api 1 · 종 1 · 발송 3 · 로컬 명령 2) — 모두 지움(제품 410 처리 1 · finally/cleanup 6), 최종 0행. 다른 9표 쓰기 0(전후 해시 같음) · 임시 계정 0.
- 비밀값: 출력 · 결과 파일 · 서버 로그 모두 `.env` 값 11개(VAPID 개인 키 · 그 base64 · hex 꼴 포함) 대조 누출 false 뒤에만 남김. JWT · Authorization 값은 파일에 남기지 않음(검사 결과 true/false만).
- 프로세스: 내가 띄운 next 서버 3번 · 프록시 · proot 래퍼 · Chromium 6번 모두 종료(`mineAfter: []`), 끝 확인 curl 3000 · 3001 · 9365~9370 = 000, ps에 next-server · next start · server.mjs · 내 chromium 0(작업 전 ps와 비교, Claude Code 자신만 제외), **code-oss 30854 살아 있음**. scratchpad의 `o5-feasibility` · `o5-header`는 계획 단계 것이라 그대로 둠(내 임시 파일은 지움).
- 서버 로그 안전 코드: `AUTH_INTERNAL_ERROR` 1(로컬 Google 버튼 = GOOGLE_* 없음 → Better Auth 기존 로그, O2 동작).

## 11. 코디네이터 할 일(값은 채팅 · 문서 · 캡처에 남기지 않음)

1. **Secret 등록(커밋 · 푸시 전)** — 저장소 루트에서:
   `grep -c '^VAPID_PRIVATE_KEY=' ex12/.env`(= `1`) → `grep '^VAPID_PRIVATE_KEY=' ex12/.env | cut -d= -f2- | tr -d '\r\n' | gh secret set VAPID_PRIVATE_KEY --repo Jwavascript/issuerank` → `gh secret list --repo Jwavascript/issuerank`(이름 · 날짜만). 새 Secret은 이것 하나 · `DATABASE_URL` 그대로 · Vercel env 추가 0.
2. 독립 심사(새 세션) 결과 사용자 줄(21 · 22 · 24) · iPhone(23)만 남으면 커밋 · 푸시(대상: 1절 파일 + `plan/PLAN_ops_O5_morning_push.md` · 이 보고 · 심사 보고 · HANDOFF · PLAN.md · SPEC.md, 작업 전부터 있던 O4 심사 보고 변경은 코디네이터 판단). C2(9절 ①)는 이미 들어가 있음 — 심사는 C2 포함 판으로.
3. **배포 줄 18**(06:30~10:00 KST 피함, Node ≤ 20요청): 배포 반영 확인 뒤 `api.mjs`를 배포용으로(주소 `https://issuerank-kappa.vercel.app` · Origin = 배포 주소 · 시험 endpoint `https://fcm.googleapis.com/fcm/send/o5-deploy-{uuid}` · 스크립트 키) — `/sw.js`(헤더 3 · 본문 v2) · `/p` · `/icons/badge-96.png` · `/widget` · `/privacy` = 저장소 파일 → POST 204 → DB 읽기(행 1) → DELETE 204 → 행 0 → Origin 없음 403 · 다른 호스트 400(약 10요청), finally에서 `/o5-deploy-` 행 삭제 · 남은 0.
4. **배포 줄 19**(CDP ≤ 25, NetLog `HTTP_TRANSACTION_SEND_REQUEST` BEGIN만 셈): O4 `deploy15.mjs` 방식 새 프로필 360 · 320, `--host-resolver-rules`로 배포 호스트 밖 NOTFOUND: `/` → SW v2 activated · 설치 오류 `[]` · 헤더 넘침 0 · `deliverPushMessage` → `getNotifications()` 1 · 가짜 pushManager로 종 상태(`/api/push/` 요청은 CDP Fetch로 막음) · 배포 호스트 밖 요청 0 · 프로필 삭제.
5. **줄 20**(푸시 뒤 **10:00~24:00 KST**에만): `gh workflow run push.yml --repo Jwavascript/issuerank`(코드 비움) 1번 → 그 실행 요약 `키 확인 OK · 구독 n · … · 보내지 않음: 10:00 지남` · 초록 · 요청 0 · daily 그대로. 06:30~10:00에는 실행하지 않음.

## 12. 사용자 할 일(마지막에 한 번에 — 휴대폰 · GitHub 화면만, 계획 순서 그대로)

미리(코디네이터): Secret 등록 · 커밋 · 푸시 · 배포 줄 18~20.
1. Android Chrome(또는 설치한 이슈랭크 앱)으로 이슈랭크 → 오른쪽 위 **종 모양 버튼** → 허용 → 종이 칠해지고 `아침 알림을 켰습니다` 토스트(캡처).
2. `/widget#push` → `이 기기: 아침 알림 켜짐 · 기기 코드 xxxxxxxx` 확인(코드는 비밀 아님).
3. 시험 알림: GitHub › Actions › **push** › Run workflow › `test_code`에 그 코드 → Run(또는 코드를 코디네이터에게 알려 주면 `gh workflow run push.yml -f test_code=…`로 대신) → 1~2분 안에 `[시험] 오늘의 뉴스가 도착했어요`(아이콘 · 상태 표시줄 배지 · 알림 머리 이름 캡처). 한 번 더 실행하면 앞 알림이 사라지고 새 알림이 소리와 함께 오는지.
4. 알림 누르기 → 이슈랭크 첫 화면(Chrome 탭인지 앱 창인지 기록).
5. (선택) 카카오톡 나와의 채팅에 이슈랭크 주소 → 앱 안 브라우저에서 종 → `/widget#push` 안내 · 메뉴 이름 기록.
6. 다음 날 아침: 알림이 온 시각 · 모양 + Actions › push 그날 실행 요약 캡처(22번).
7. 그 뒤 종을 한 번 더 눌러 끄기 → `아침 알림을 껐습니다` → (선택) 3번 다시 → 요약 `코드에 맞는 기기가 없습니다`(빨간 실행이 정상).
8. `/privacy` 새 문장 확인(O2 21번과 같이) → "확인" 또는 고칠 점(24번). iPhone이 생기면 23번 · 며칠 뒤 (선택) Vercel › Analytics › Pages에서 `/p`.
+ (C2 확인 — 코디네이터 결정 10-20) 태블릿 · 좁은 PC 창(641~1199px)에서 헤더 설치 · 북마크가 **아이콘(+숫자)만** 보이는지(글자 꺾임 · 이름 잘림 없음). 1200px 이상은 글자 그대로.

## 13. 필요했지만 없던 키 · 계정

- 없음(VAPID 키는 이번에 만듦). GitHub Secret `VAPID_PRIVATE_KEY`는 아직 없음 → 코디네이터 11절 1. Firebase · Apple 개발자 계정 · Vercel env 추가 불필요. 로컬 `ex12/.env`에 GOOGLE_* 없음(O2 로컬 확인과 같음).

## 14. 미해결 · 추가로 보인 점

1. 9절 ①(C2 1199px 적용 — 641~1280px 꺾임 0 · 12자 섞인 이름 1200~1220px 7경우는 참고 기록) · ②(기록).
2. headless Chromium 155에서 `deliverPushMessage` → `getNotifications()`가 답함(계획 "확인 필요" 해소) · Chrome이 알림마다 `badge-96.png`를 받아 감(로컬 요청 3).
3. 확인 필요로 남음(계획 그대로): 알림 머리 이름 · 같은 tag 대체 때 소리 · 앱 창으로 열리는지 · Samsung Internet 호스트 · Edge(Windows) 주소 꼴 · Android 13 앱 알림 권한 · iOS 동작 · `workflow_run`이 예약 실행 끝에 바로 오는지 · Pretendard 실제 폭 · 법률 문구.
4. `/privacy` 첫 문단("회원 가입 · 로그인과 계정별 북마크에 필요한…")은 초안 범위 밖이라 그대로 — 알림도 넣을지는 24번 확인 때 같이 보면 좋음.
5. `index.html` C2 주석 줄의 숫자: 1199px 보강 때 지시대로 두었는데(그때 "1099px 이하는…"), 그 뒤 작업 트리에서 "1199px 이하는…"으로 맞춰져 있음을 확인(제작 역할이 고친 것 아님 — 규칙과 숫자 같음). → **코디네이터가 고침(10-20): 주석도 "1199px 이하는…"**(규칙 · 테스트 변화 없음, 다시 심사에서 확인).

## 15. 심사 FAIL 수정(10-20)

근거: 독립 심사 `reports/ops_O5_morning_push_judge.md` 줄 8 FAIL(7절 1, 필수) · 7절 2(낮음 · 선택) — 코디네이터 수정 요청(10-20). 두 가지만 고침, 커밋 · 푸시 0.

- **원인(줄 8 · SSRF)**: `pushService()`의 정확 일치 검사 `if (EXACT[host]) return EXACT[host];` — 일반 객체 리터럴 조회는 `Object.prototype`에서 상속한 속성도 찾음. URL이 호스트를 소문자로 바꾼 뒤에도 그대로인 원형 속성 이름 `constructor` · `__proto__`가 참 값(`Object` 함수 · `Object.prototype`)으로 통과 → 구독 API 204 · 저장 → 발송 직전 재검사도 통과 → Actions가 `https://constructor/…`로 VAPID JWT · 암호화 본문을 담아 POST할 수 있었음(실패하면 되돌림이라 행이 남아 매일 빨강). 대문자가 섞인 다른 상속 이름(`toString` 등)은 소문자 호스트와 안 맞아 원래도 null. 제작 검증(줄 8 · 16)은 이 입력을 넣지 않아 놓침.
- **바꾼 줄**
  1. `ex12/server/push-config.ts` 39줄: `if (EXACT[host]) return EXACT[host];` → `if (Object.hasOwn(EXACT, host)) return EXACT[host];`(이 줄만).
  2. `.github/workflows/push.yml` run 줄: `npm run push -- "${args[@]}"` → `npm run --silent push -- "${args[@]}"`(npm 머리 줄 `> tsx … server/push.ts --test <코드>`가 실행 요약에 들어가던 것 — 계획 "요약에 기기 코드 0").
  3. 테스트: `test/push-api.test.ts` 400 묶음 +2줄(`https://constructor/x` · `https://__proto__/x`) · `test/push.test.ts` 발송 직전 재검사 +1줄(두 행을 저장소에 바로 넣음) + 기대 1줄(`검사 실패 삭제 2` → `4`, 요청은 `…/ok` 하나뿐 · 두 행 삭제).
- **같은 꼴(일반 객체를 계산한 키로 찾는 곳) 검색** — O5 새 · 바뀐 코드(server 6파일 · routes/push · store · sample · app · sw.js · index.html Push 구역 · widget.html 상태 스크립트 · pwa-images · push.yml): 3곳.
  - ① `push-config.ts` 39줄 `EXACT[host]`: 키 = 바깥 입력(endpoint 호스트) → **고침**(위 1).
  - ② `public/sw.js` 33줄 `typeof p[k] === 'string'`: k = 고정 4개(`title` · `body` · `tag` · `url`), `Object.prototype`에 그 이름의 문자열 속성이 없어 상속 값이 통과할 길 없음 → 그대로.
  - ③ `public/index.html` Push 구역 `setPush()`의 `PUSH_LABEL[value]` 2줄: value는 코드 안 고정값뿐(`setPush` 호출 6곳 = `'on'` · `'off'` · `'blocked'`, 첫 HTML 값 `'unsupported'`) — 바깥 입력 0 → 그대로(고치려면 같은 `Object.hasOwn` 2줄, 동작 변화 없음).
  - 그 밖: `routes/push.ts`는 본문의 고정 이름(`endpoint` · `keys` · `p256dh` · `auth`)만 읽음(상속 속성에 그 이름 없음) · `push-run.ts` env는 고정 이름 2개 · 나머지 조회표는 `Map`(JWT · 집계) · 배열(SUFFIX · SERVICES). 고친 줄 = 위 1 하나뿐.
- **테스트**: 고치기 전 새 기대 2개가 실패하는 것 확인(push-api: `원형 속성 constructor` → 204, push: 요청 3곳 = ok · constructor · __proto__) → 고친 뒤 통과. 전체 `npm test` **26파일 268/268**(경우는 기존 테스트 안에 더해 개수 같음) · vitest 추적 파일 변경 0 · `npm run build` 성공(누출 false) · 번들 발송 표지 0(검사 함수는 `/api/[...path]` 청크에 `hasOwn`으로).
- **심사 다시 보기(심사 보고 9절 ①)**: 심사 스크립트 `j-ssrf.mts` · `jlib.mjs` · `jguard.mjs`를 내 폴더(`ex12/.data/o5-build-20261020/jr-ssrf.mts` 등)로 복사해 같은 명령으로 실행(심사 증거 파일 덮어쓰기 0): 원형 이름 6개(constructor · __proto__ · tostring · hasownproperty · valueof · __definegetter__) 모두 null · API POST/DELETE 400/400 · `apiVsServiceMismatch` [] · 보낸 곳 = `…/ok-judge` 하나 · 요약 `검사 실패 삭제 6` · 종료 0 · 가드 기록 = tsx 로컬 파이프뿐 → 심사 기대값과 같음.
- **실행 요약 흉내(Actions 실행 0)**: push.yml run 블록을 그대로 뽑아(옛 줄 판 · 새 줄 판) 실제 npm 11.16 + 실제 `push` 스크립트로 `GITHUB_ACTIONS=true` · `TEST_CODE=xyz`(형식 틀림 → DB · 네트워크 전에 끝남) 실행(`ex12/.data/o5-build-20261020/wf-sim/`): 옛 줄 = 요약에 `> tsx --env-file-if-exists=.env server/push.ts --test xyz`(코드 있음) / **새 줄 = 요약이 제품 출력 3줄뿐**(`아침 알림 … KST` · `키 확인 OK` · `보내지 않음: 시험 코드는 8자 16진수입니다`) — 코드 · `https://` · 키 · npm 오류 줄 0 · 종료 코드 2 그대로 · 연결 = tsx 로컬 IPC뿐(DB · 바깥 0) · 누출 false. 맞는 기기가 있을 때의 제품 출력에 코드 · endpoint · 키가 없음은 `push.test.ts`(시험 알림 줄 · 결과 줄)가 확인.
- **요청 · 쓰기 · 정리**: 로컬 3000 이번 0(누계 83/100) · DB 쓰기 0(시험 행 0) · 바깥 0 · 새 패키지 0 · 보호 58파일 해시 같음 · 누출 검사 false · 소유 프로세스 0. 심사 7절 3 · 5 · 6 · 7 · 8(낮음 · 참고)은 손대지 않음.
