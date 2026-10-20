# ops O5 아침 알림(웹 푸시) — 독립 심사 보고

- 날짜: 2026-10-20(KST 13:04~13:58) · 역할: 독립 심사(`agent_prompts/3_judge_agent.md`, 운영 단계) · 코드 수정 0
- 기준: `plan/PLAN_ops_O5_morning_push.md`(확정) · SPEC US-14 · AC-14-1~4 · 회귀 AC-13-1~3 · 11-2 · 06-3 · 01-1 · PLAN.md D10 · D21~D30 · 6절 · AGENTS.md
- 대상: ex12 작업 트리 + 새 `.github/workflows/push.yml`(HEAD `8aaf6bd` 대비 미커밋). 제작 보고 `reports/ops_O5_morning_push_build.md`는 주장으로만 봄
- 판정 근거는 모두 심사가 새로 쓴 스크립트(`ex12/.data/judge-o5-20261020/`, git 제외): `jcrypto.mjs`(P-256 · ECDH · ECDSA를 BigInt로 직접 + noble 순수 JS HMAC/AES-GCM — 제품 코드 · node:crypto · OpenSSL 안 씀) · `jguard.mjs`(바깥 연결 · fetch 감시 · 차단) · `j-crypto.mts` · `j-ssrf.mts` · `j-api.mts` · `j-run.mts` · `j-sw.mjs` · `j-yaml.cjs` · `j-send.mts` · `j-local.mjs` · `j-server.mjs`(3000 세는 프록시 + next start 3001) · `jcdp.mjs` · `jfake.mjs` · `j-cdp-a/a2/b/c/d/e/f.mjs` · `jdb.mjs` · `j-png.mjs` · `j-leakscan.mjs`. 제작 스크립트(`.data/o5-build-20261020/`)는 Chromium 플래그 · 프록시 방식만 참고, 실행하지 않음

## 판정

**PASS 보류**(2026-10-20 14:13~14:24 KST 줄 8 재확인 뒤 — 11절). 처음 판정(13:58)은 FAIL(줄 8 — 받는 푸시 서비스 검사 우회)이었고, 제작이 고친 한 줄을 심사 절차로 다시 확인해 OK로 바꿈. 위반 0.
**합계: OK 20 · FAIL 0 · SKIP 4**(21 · 22 · 24 사용자, 23 iPhone) — SKIP이 남아 PASS 아님. 줄 20은 코디네이터가 14:34에 OK(13절). 배포 줄 18 · 19는 푸시 `249c5d7` · Vercel 배포 뒤 2026-10-20 14:29~14:31 KST에 OK(12절). (재확인 직후 14:24: OK 17 · FAIL 0 · SKIP 7.)
(처음 판정 13:58: FAIL — OK 16 · FAIL 1 · SKIP 7.)
- 커밋 전 고칠 것: **없음**(7절 1 필수 · 7절 2 선택 모두 고침 확인 — 11절). 남은 것은 낮음 · 참고(7절 3~8).
- 코디네이터 판단 C2: **적절**(641~1199px 글자 꺾임 해결, 640px 이하 · 1221px 이상 변화 0, 회귀는 아주 긴 이름의 1200~1220px뿐) — 8절.

## 1. PLAN 항목별

| PLAN 항목 | 판정 | 직접 확인한 근거 |
|---|---|---|
| 이번 범위(구독 → API → 표 → 발송 → 워크플로 → SW → 화면 · 안내 · 정책 · /p · 배지) | done | 3~5절 |
| 보내는 시각 · 방식(Q1 · Q3) | done | 5.3: 06:30~10:00 · 03:30~06:30 기다림(정확히 180분까지) · 10:00/03:30 전 건너뜀 · KST 오늘 랭킹 · 보내기 전 그날 표시 · 404/410 삭제 · 무응답 유지 · 그 밖 되돌림 · 로컬 모드 · 종료 코드 · 요약 숫자만. 처음 참고: 시험 실행 요약에 npm 머리 줄로 입력 코드가 찍힘(7절 2) → `npm run --silent push`로 고침, 요약 = 제품 출력뿐 확인(11절) |
| 발송 구현(Q2) · payload 유지 | done | 5.1: RFC 8291 5절 144B 바이트 같음 · 내 복호화로 풂 · 새 패키지 0 · 함수 번들에 발송 코드 0 |
| 키(VAPID) | done | 5.1: .env 개인 키 → 내 BigInt 공개 키 = 코드 상수 = index.html PUSH_KEY · 키 없음/다름 → 목록 · fetch 0 · 종료 2 · `push:keys` 임시 폴더 시험(공개 키만 출력 · 600 · 두 번째 종료 2) |
| 구독 저장 · API | done | 5.2 · 줄 8 · 14: Origin · 4KB · 400 묶음 · 5,000행 503 · 재구독 행 1 · DELETE · no-store · 쿠키 0 · BFF 같음 |
| 받는 푸시 서비스(SSRF — "이 목록 밖은 400", 정확히 같은 호스트) | done(10-20 재확인 — 처음 wrong(부분)) | 처음(5.2): 호스트 `constructor` · `__proto__`가 204로 저장되고 발송 직전 재검사도 지나 실제로 `https://constructor/…`로 POST(7절 1) → 고친 뒤(11절): 원형 이름 12개 × 그대로/소문자 44 endpoint 모두 null · API 400 · 발송 직전 삭제 · fetch 0 · 실제 BFF 400 |
| 알림 내용 · 누르면 | done | 5.1 · 5.5: 고정 JSON 92B · 뉴스 내용 0 · tag 대체 · `/p` |
| 서비스 워커 | done | 5.5: push(모든 push에 알림 1 · 틀리면 기본) · 같은 tag 닫고 띄움 · 클릭 → 같은 출처 창 focus+navigate / 실패 · 없음 → openWindow · v2 · v1 삭제 · O4 부분 글자 그대로 · 실제 Chromium deliverPushMessage |
| 화면 · UX(Q5) | done | 5.6: 종 4상태 · aria-label = title · 누를 때만 subscribe(같은 틱 · userActivation true) · 토스트 8개 중 실제 브라우저 7 + VM · 폭 표 넘침 0 |
| `/widget#push` | done | 5.6: 위치(앱 뒤 · iPhone 앞) · 문구 = 계획 · 켜짐 기기 코드 = SHA-256 앞 8자 · 꺼짐 · 지원 안 함 |
| 개인정보 `/privacy` | done(사용자 확인 대기) | 5.6: 초안 문장 9개가 각 절에 그대로 · 이메일 = 문의처 절 2곳뿐 · 7 · 11항 = HEAD · 시행일 10-20 |
| SPEC · PLAN.md 반영 | done | SPEC US-14 · AC-14-1~4 · 범위 밖 줄 · PLAN.md D30 · 10절 메모(C2 기록) |
| 구현 순서 · 테스트 | done | 새 테스트 4파일 + `pwa.test.ts` 두 가지만(v1 → v2 · widget 경계) |
| 코디네이터 할 일(Secret · 커밋 · 푸시 · 20) | — | 심사 범위 밖(9절 절차) |

## 2. AC

| AC | 판정 | 근거 |
|---|---|---|
| AC-14-1 종으로 구독 · 누를 때만 권한 · 지원 안 함 안내 | 충족(로컬) | 실제 Chromium + 가짜 pushManager: 처음 `off` · 누름 → subscribe 1(같은 click 틱, userActivation true, `{userVisibleOnly: true, key 65B = 코드 키}`) → POST(Origin 있음) → 실제 DB 행 1 → `on` · `아침 알림 끄기`. 페이지 로드 때 subscribe · requestPermission 0(코드에 requestPermission 없음). PushManager · Notification 없음 → `unsupported` → `/widget#push`. 실기기 21 · iPhone 23은 SKIP |
| AC-14-2 06:30~10:00 하루 1통 · 재실행 0 · 랭킹 없음/10:00 뒤 0 · 하루 실행 무관 | 충족(로컬) | 가짜 시계 표 · 겹친 실행 2개도 기기당 1 · 실제 DB 두 번째 실행 요청 0 · daily 쪽 파일이 push를 부르지 않음 · daily.yml = HEAD. 실제 다음 날 아침 22는 SKIP |
| AC-14-3 고정 문구만 · 누르면 첫 화면 | 충족 | 복호화한 본문 = `{"title":"오늘의 뉴스가 도착했어요","body":"","tag":"issuerank-daily","url":"/p"}` 정확히 · 실제 SW 알림 = 그 값 + icon · badge · lang ko · actions/image 0 · 클릭 → `/p`(VM) · `/p` = 첫 화면(상세창 0) |
| AC-14-4 끄면 서버 삭제 · 권한 끈 기기는 다음 발송 때 | 충족 | UI 끄기 → unsubscribe 1 → DELETE → 실제 DB 행 0 · 410 → 실제 DB 행 삭제(발송 경로) |
| 회귀 AC-13-1 · 13-2 · 13-3 | 충족 | 설치(가짜 이벤트 → 헤더 앱 설치 → prompt 1 · 숨김, 401px 이상) · 오프라인(SW v2 + 닫힌 포트 → `/` · `/p` · `/s?issue=` 안내) · 공유 복사(제목 · 섹션 · `/s?issue=` 주소, 요약 0) |
| 회귀 AC-11-2 | 충족 | `/?issue=` · `/s?issue=` → 상세창 · 쿼리 지움 |
| 회귀 AC-06-3 | 참고 · 로컬 경로 충족 | 1280 로그인 창 → `Google로 계속하기` → (로컬 GOOGLE_* 없음) `지금은 Google 로그인을 사용할 수 없습니다` · 주소 그대로. 실제 Google은 O2 사용자 몫 |
| 회귀 AC-01-1 · 범위 밖 "이메일 알림" · "본문 재게시" | 참고 · 충족 | daily.yml · 하루 실행 코드 = HEAD · 알림에 뉴스 내용 0 · 이메일 알림 0 |

## 3. 범위 · 불변 · PLAN 밖

- `git status`: ex12 바뀜 15(.env.example · _journal.json · next.config.mjs · openapi.yaml · package.json · index · privacy · sw · widget · app · schema · store · sample · pwa.test · pwa-images) + 새 13(push-config · push-keys · push-run · push · webpush · routes/push · 0007 sql · 0007 snapshot · badge-96.png · 테스트 4) + 새 `push.yml` = 계획 "변경 범위" 목록과 정확히 같음. O5 코드가 아닌 미커밋 6개(HANDOFF · PLAN.md · SPEC.md · O4 심사 보고 · 계획 · 제작 보고)는 따로 셈. ex01~ex11 · frontend · `web/` 변경 0(이름 목록만 git으로 확인, 내용은 읽지 않음).
- 보호 파일 60개(계획 검증 1 목록 + tsconfig · vitest 설정) = `git cat-file HEAD:` 해시와 같음: daily.yml · 하루 실행 코드 · auth · bff · 기존 라우트 · card · package-lock(새 패키지 0) · 0000~0006 SQL · 스냅숏 · 글꼴 · 아이콘 5 · og.png 등.
- `package.json`은 scripts `push` · `push:keys` 2줄 + `daily` 줄 끝 쉼표뿐. `pwa.test.ts`는 계획의 두 가지(캐시 v1 → v2 · widget 경계 `<h2 id="push">`)뿐.
- `index.html`: HEAD 1,349줄이 모두 순서대로 남음(지운 줄 0), 더해진 97줄 = CSS 7 · 반응형 6(C2 주석 · 규칙 포함) · 헤더 `#pushBtn` 3 · Push 구역 81. 기존 VM 테스트가 잘라 쓰는 10구역(`async function api(` ~ `/* Date` · Events · Share · install (O4) ~ Navigation · Navigation ~ `</script>` 등) 글자 그대로 · `\nstart();\nregisterServiceWorker();\n</script>` 1번 · `serviceWorker.register(` 수 = HEAD(1).
- PLAN 밖: C2(코디네이터 판단, 8절) · 제작 8절의 내부 세부(DELETE도 endpoint 검사 · `--test` 앞뒤 공백 · 대문자 허용 · 요약 `검사 실패 삭제 n` · `다른 실행이 먼저 표시 n` · SW `.catch` · 클릭 때 data.url 재검사) — 모두 계획 규칙 안의 작은 세부로 봄.
- 작업 트리 35파일 sha256 = 심사 시작 때(제품 · 테스트 · 문서 수정 0, vitest가 바꾼 파일 0).

## 4. 테스트 · 빌드 · 계약

- `bash scripts/ex12-ubuntu.sh npm test`(안전 실행): **26파일 268/268**, 누출 false, git status 전후 같음.
- 깨끗한 빌드: `ex12/.next` 삭제 → `npm run build`(next build + tsc --noEmit) **성공**, 누출 false. `.next/server`에서 `WebPush: info` · `Content-Encoding: aes128gcm` · `nonce` · `vapid t=` · `PUSH_PAYLOAD_TOO_LARGE` · `ieee-p1363` 0파일(발송 코드는 함수 번들 밖) · 구독 라우트 문구는 `/api/[...path]` 청크 · `VAPID_PRIVATE_KEY` 글자는 소스맵 주석 1곳(값 아님) · `.next` 204파일 누출 0.
- OpenAPI: `npx --offline @redocly/cli@2.64.1 lint` 지금 판 · HEAD 판 둘 다 **오류 0 · 경고 20**, 경고 규칙 · 위치 = HEAD(줄 번호만 밀림), 새 경로 경고 0 · `info.version 0.5.0`.
- 스키마: 임시 복사본(`dk/`, 끝에 삭제)에서 `drizzle-kit generate` → `No schema changes, nothing to migrate` · 0007 스냅숏에서 새 표를 빼면 0006과 같음 · prevId 이어짐.

## 5. 검증 24줄

| # | 결과 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | [OK] | 3절 |
| 2 암호화 = RFC | [OK] | 5.1 |
| 3 VAPID | [OK] | 5.1 |
| 4 요청 · 복호화 | [OK] | 5.1 · 5.3 |
| 5 결과 · 하루 1통 | [OK] | 5.3 |
| 6 날 · 시각 | [OK] | 5.3 |
| 7 시험 알림 | [OK] | 5.3 |
| 8 구독 API | [OK](10-20 재확인 — 처음 [FAIL]) | 계획 표의 입력 · 기대(400 묶음 · 403 · 413 · 503 · DELETE · GET 404 · BFF)는 모두 맞음. 처음 심사(13:58)에서는 계획 규칙 "이 목록 밖은 400 · `fcm.googleapis.com`(정확히)"이 호스트 `constructor` · `__proto__`에서 깨져(204 · 저장 · 발송) FAIL(계획에 없는 경계 입력이 아니라 계획 규칙 위반, 5.2 · 7절 1) → 제작 수정(`Object.hasOwn`) 뒤 원형 이름 전부 400 · 저장 0 · 발송 직전 삭제 · fetch 0 · 실제 next start + BFF 400 · 나머지 표 결과 처음과 같음(11절) |
| 9 스키마 · 마이그레이션 | [OK] | 4절 · 5.7 |
| 10 서비스 워커(VM) | [OK] | 5.5 |
| 11 화면 함수(VM) | [OK] | `push-ui.test.ts` 15개 통과 + 같은 흐름을 실제 Chromium으로 다시(5.6) |
| 12 안내 · 정책 · 그림 | [OK] | 5.6 · 그림 7개 두 번 같음 · 기존 6 = HEAD 바이트 · 배지 직접 풀기(투명 4,456 · 불투명 흰 4,091 · 반투명 흰 669 · 흰색 아닌 0 · 마크 12~83 · 모서리 투명) · fetch는 `data:` 2번뿐 |
| 13 OpenAPI · 회귀 · 빌드 · 번들 | [OK] | 4절 |
| 14 로컬 서버 · 실제 DB | [OK] | Node 18요청: sw.js 헤더 3 = O4 · 본문 = 파일 / `/` · `/p` = index.html / badge 200 image/png 바이트 같음 / widget · privacy = 파일 / POST 204 → 실제 행 1(키 같음 · last_sent_on null · id UUID) → 같은 endpoint 다른 키 204 → 행 1(키 바뀜 · id · created_at 그대로) → Origin 없음 · 다른 Origin 403 · `evilpush.apple.com` 400 · 4,097B 413(행 그대로) → DELETE 204 → 행 0 → 다시 DELETE 204 / 모두 no-store · Set-Cookie 0 / 회귀 rankings · widget · caption · get-session 200 |
| 15 로컬 브라우저(CDP) | [OK] | 5.6(폭 표 넘침 0 · 상태 · 다크 · v1 → v2 · deliverPushMessage · 회귀 · D20) — 참고 2건(7절 5 · 8절) |
| 16 로컬 발송 경로(실제 DB) | [OK] | 5.3 끝 |
| 17 안전 · 한도 | [OK] | 10절 |
| 18 배포 정적 · API | [OK](10-20 14:29, 12절) | 배포 요청 11/20: 정적 5 = 저장소 바이트 · sw.js 헤더 3 · POST 204 → 실제 DB 행 1 → DELETE 204 → 행 0 · 403 · 400 · 400(constructor) · 413 |
| 19 배포 브라우저 | [OK](10-20 14:31, 12절) | 배포 요청 10/25(NetLog): SW v2 activated · 캐시 v2 offline.html · 설치 오류 [] · 360 · 320 넘침 0 · deliverPushMessage → 알림 1 · 종 켜기/끄기(구독 API는 로컬 204) · 바깥 0 |
| 20 워크플로 연결 | [OK] 코디네이터 2026-10-20 14:34 KST | 13절(run 39375573798 · 키 확인 OK · 보내지 않음: 10:00 지남 · 요청 0) |
| 21 Android 실제 | [SKIP — 사용자 확인 대기] | 계획 순서 1~5 · 7 |
| 22 다음 날 아침 실제 발송 | [SKIP — 사용자 + 기록] | 계획 순서 6 |
| 23 iPhone | [SKIP — iPhone 기기 없음] | — |
| 24 정책 문안 | [SKIP — 사용자 문안 확인 대기] | 계획 순서 8 |

### 5.1 암호화 · VAPID · 키 (`j-crypto.mts`, 판정은 심사 도구만)

- 심사 도구 자체 확인(`j-selftest.mjs`): RFC 8291 5절 값(심사가 직접 옮김)으로 내 암호화 = RFC 본문 144B · 내 복호화 = `When I grow up, I want to be a watermelon`(rs 4096 · idlen 65 · 구분자 0x02) · 태그 1비트 바꾸면 거절 · 내 ES256 검증기가 node 서명 참 · 1바이트 바꾼 서명 · 다른 메시지 · 다른 키 거절.
- 제품 `encrypt`(RFC 입력 · salt · 서버 키 주입) = RFC 5절 본문과 **바이트 같음**(144B · 머리 salt 16 · rs 4096 · idlen 65 · 서버 공개 키). 테스트 파일의 RFC 값도 내가 옮긴 값과 같음.
- 무작위 두 번: 본문 · salt · 서버 키 모두 다름 · 둘 다 내 BigInt 키 쌍 + noble로 풀림 = 아침 JSON 정확히(평문 92B → 본문 195B) · 시험 JSON도 풀림 · 3,993B → 본문 4,096B 풀림 · 3,994B → `PUSH_PAYLOAD_TOO_LARGE`.
- 키 거절: 곡선 밖 y · 영점 · x = p · x + p(축약 안 된 좌표, 같은 점) · 압축 33B · 첫 바이트 0x03 · 64/66B · 표준 base64 · auth 15/17B · 빈 값 · 숫자 · null · 공백 → `validKeys` false(발송 직전 검사도 같은 함수). 곡선 밖 점은 `encrypt`도 ECDH에서 던짐.
- VAPID(실제 .env 키, 메모리만): 내 BigInt로 계산한 공개 키 = `VAPID_PUBLIC_KEY` = `index.html` `PUSH_KEY`(87자 · 곡선 위) · 제품 `vapidKey` 받아들임. 4종 origin(fcm · mozilla · `web.push.apple.com` · `wns2-par02p.notify.windows.com`)마다 `vapid t=<JWT>, k=<코드 키>` 꼴 · 머리 `{"typ":"JWT","alg":"ES256"}` · claim = aud · exp · sub 셋뿐 · aud = origin(끝 `/` 없음) · exp − now = 43,200(≤ 86,400) · sub = `https://issuerank-kappa.vercel.app`(이메일 · mailto 0) · 서명 64B를 **내 BigInt ECDSA로 검증 참** · 바꾼 메시지 거짓. JWT · Authorization 값은 저장 · 출력 0.
- 키 없음 · 빈 키 · 다른 키(시험 모드 포함, Actions 모드여도) → 종료 2 · `VAPID_PRIVATE_KEY가 없습니다` / `VAPID 키가 코드의 공개 키와 다릅니다` · store.list · latestDate · fetch · sleep 모두 0. `vapidKey`는 다른 키 · 빈 값 · 짧음 · 44자 · `=` 붙음 모두 null.
- 요청 머리(제품 `sendPush` + 가짜 fetch): POST · 머리 5개 정확히(TTL 21600 · Urgency normal · Content-Encoding aes128gcm · Content-Type application/octet-stream · Authorization vapid) · `redirect: 'error'` · AbortSignal · 본문 ≤ 4,096B. 상태: 2xx → ok · 404/410 → gone · 400/401/403/413/429/500/502/503 · 연결 실패 · redirect 오류 → failed · **진짜 10초 타이머로 응답 없음 → timeout(10,004ms)**.
- 저장소 · 증거 누출: git 추적 + 새 파일 646개 · 제작 · 심사 증거 폴더 · `.next` 모두 .env 비밀값(VAPID 개인 키의 base64url · base64 · hex 꼴 포함 10개) 대조 누출 0. `ex12/.env` = git 제외(`.gitignore:4`) · 추적 안 됨 · 권한 600 · `VAPID_PRIVATE_KEY=` 줄 1개.
- `push:keys`(임시 폴더, 일회용 키, 끝에 삭제): 기존 줄 유지 + 줄바꿈 넣고 한 줄 덧붙임 · 600 · 화면 = 공개 키 + 안내뿐(개인 키 0) · 출력한 공개 키 = 내 BigInt 계산 · 두 번째 실행 `이미 .env에 있습니다` 종료 2.

### 5.2 받는 푸시 서비스(SSRF) 표 (`j-ssrf.mts`)

- 받음: fcm · mozilla(정확히) · `web.push.apple.com` · `api.push.apple.com` · `*.notify.windows.com` · 대문자 호스트 · `HTTPS://` · 2,048자 · 쿼리만 · 경로 없음.
- 거절(400, POST · DELETE 같음): http · ftp · 스킴 없음 · `//` · `u@` · `u:p@` · 빈 `@` · `:443` · `:8443` · 빈 포트 · **끝 점**(`fcm.googleapis.com.` · `web.push.apple.com.`) · `fcm.googleapis.com.evil.example` · `push.apple.com.evil.com` · `evilpush.apple.com` · `push.apple.com` · `.push.apple.com` · `notify.windows.com` · `x.fcm.googleapis.com` · `fcm.googleapis.co` · 옛 GCM · Mozilla 스테이징/개발 · IPv4(점 · 정수 `2130706433` · 16진 `0x7f000001` — URL이 127.0.0.1로 바꾼 뒤 거절) · IPv6 · localhost · 169.254.169.254 · 역슬래시 · `#` · 공백 · 탭 · 줄바꿈 · NUL · DEL · `%2f` 섞인 호스트 · `%40` · IDN 동형 문자(`cοm`) · 2,049자 · 문자열 아닌 값.
- 참고(받지만 같은 호스트로 정규화): `%2e` · 전각 `ｆｃｍ` · 전각 마침표 → URL이 `fcm.googleapis.com`으로 바꿈(실제 FCM으로 감, SSRF 아님) · `통.push.apple.com`(punycode 하위 도메인) · `..push.apple.com`(빈 레이블, 이름 풀이 실패 → 7절 3).
- **틀림**: `https://constructor/x` · `https://__proto__/x` → `pushService()`가 `Object` 함수 · `Object.prototype`(참 값)을 돌려줌 → POST 204 · DELETE 204 · 저장. 발송(메모리 저장소 · 가짜 fetch · Actions 모드): `https://evilpush…` · 꼬리 호스트 · http · IP 행은 재검사로 삭제(`검사 실패 삭제 4`)되지만 **`https://constructor/x` · `https://__proto__/x`로 POST가 나감**(가짜 fetch 기록, `서비스:` 줄에는 안 셈). `tostring` 등 소문자가 아닌 원형 이름은 URL이 소문자로 바꿔 해당 없음.
- 제품 코드에 검사를 끄는 env · 시험용 우회 0: push 관련 7파일의 env 사용 = routes/push `BETTER_AUTH_URL`, push-run `VAPID_PRIVATE_KEY` · `GITHUB_ACTIONS`(로컬 모드 전환), push.ts `VAPID_PRIVATE_KEY` · `DATABASE_URL`뿐. 테스트의 `vi.mock`(공개 키 상수 교체)은 테스트 안에만.

### 5.3 하루 1통(가짜 시계 · 가짜 sleep · 기록하는 저장소, `j-run.mts`) · 실제 DB 경로(`j-send.mts`)

- 시각(그날 랭킹 있음): 06:30:00 · 07:00 · 09:59:59 보냄 / 10:00:00 · 10:00:01 · 23:59:59 `보내지 않음: 10:00 지남`(latestDate 0) / 00:00 · 00:10 · 03:29:59 `너무 이른 시각` / 03:30:00 → sleep 10,800,000ms(180분) 1번 → latestDate 다시 → 보냄 / 05:10 → 80분 / 06:29:59 → 1초(`약 1분`) / 기다린 뒤 랭킹 사라짐 → `새 데이터 없음` / 기다린 뒤 시계가 10:00 → `10:00 지남` / 06:30 전인데 랭킹 없음 → 기다리지 않고 건너뜀.
- 랭킹 없음: 어제 · 없음(null) · 미래 날짜 → `새 데이터 없음(최근 수집일 …)` · fetch · 표시 0.
- 12기기(201 · 404 · 410 · 400 · 401 · 403 · 413 · 429 · 500 · 503 · 연결 실패 · 응답 없음, 절반은 전 값 = 어제): **모든 기기에서 mark가 fetch보다 먼저** · 전부 내 복호화로 풀림 · 201 · 응답 없음 = 그날 표시 유지 · 404 · 410 = 행 삭제 · 나머지 8 = **전 값으로 되돌림(null 또는 어제)** · 종료 1 · 요약 `보냄 1 · 만료 삭제 2 · 실패 8 · 응답 없음 1` · 상태별 12 · `서비스: fcm 12` / 같은 날 두 번째 = 되돌린 8만 · 세 번째 = 0(`이미 보냄 10`) / 다음 날 = 10 모두.
- 겹친 실행 2개(같은 저장소, 25기기) → 기기마다 정확히 1. 동시 요청 최대 10 · JWT는 origin마다 1개.
- 요약 22줄의 낱말 = 고정 문구 · 숫자 · 날짜 · 시각 · `fcm`뿐(endpoint · 키 · id · 기기 코드 0).
- 시험 `--test`: 23:30 · 최근 수집일 10-01이어도 그 기기만 1 · 시험 JSON · TTL 21600 · mark/revert/remove 0 · latestDate · sleep 0 · 그날 표시 그대로 / 대문자 · 앞뒤 공백 → 됨 / 0개 · 2개(SHA-256 앞 8자 충돌 쌍을 찾아 씀) · `xyz` · 7자 · 9자 · 빈 값 → 종료 2 · 각 문구 · fetch 0 / `--test`만 · 인수 3개 · 다른 인수 → `--test <기기 코드>만 받습니다` 종료 2(키 확인 전).
- 로컬 모드: `GITHUB_ACTIONS` 없음 · `false` · `1` · `TRUE` × 07:00 · 05:10 · 11:00 → 결정 · 대상 수 · `로컬 실행: 보내지 않음` · 종료 0 · fetch · mark · revert · remove · sleep 0(정확히 `true`일 때만 보냄).
- 하루 실행과 분리: daily · steps · collect · group · summarize에 push 언급 0 · push.ts가 쓰는 dbStore 함수 = `latestDate` 하나 · DB 오류(닫힌 포트 DATABASE_URL) → `DB 오류: connect ECONNREFUSED 127.0.0.1:9` 첫 줄 · 종료 2 · 누출 false.
- 실제 DB(`judge-o5-send-` 3행 fcm · apple · mozilla, 실제 `pushStore` · `latestDate` = 2026-10-20, now = 그날 07:00, 가짜 서비스): 첫 실행 요청 3 · 셋 다 내 복호화 = 아침 JSON · JWT 내 검증 참 · aud = origin · sub · k = 코드 · 머리 = 표 · redirect error → mozilla 410 → **제품이 실제 행 삭제** · 나머지 2 `last_sent_on = 2026-10-20` / 둘째 요청 0 · `구독 2 · 이미 보냄 2 · 대상 0` / `--test {fcm 코드}` → fcm 1 · 시험 JSON · 표시 그대로 / 출력에 endpoint · 키 · 코드 0. 이어서 **진짜 `npm run push`(실제 .env · 로컬 모드 · 가드: DB만 허용 · fetch 차단)** → `키 확인 OK · 구독 2 · 이미 보냄 2 · 대상 0 · 보내지 않음: 10:00 지남 · 로컬 실행: 보내지 않음` · 종료 0 · 연결 = DB 1 · fetch 0 → `jdb.mjs cleanup`으로 judge-o5- 2행 삭제 · 남은 0.

### 5.4 push.yml

- 같은 npx 캐시에 이미 있던 `yaml` 1.10.3 · `js-yaml` 4.3.2(새 패키지 0)로 엄격히 읽음: 오류 · 경고 0 · 두 파서 결과 같음(daily.yml도).
- 트리거 `workflow_run`(workflows [daily] = daily.yml의 name · types [completed]) + `workflow_dispatch`(test_code string · 필수 아님 · 기본 빈 값). `concurrency: push`(cancel 안 함) · `permissions: contents: read`뿐 · `timeout-minutes: 200` · `working-directory: ex12` · `actions/checkout@v7` · `actions/setup-node@v7`(24 · npm 캐시 `ex12/package-lock.json`) = daily.yml과 같은 꼴 · Secret은 env 2개(`DATABASE_URL` · `VAPID_PRIVATE_KEY`)뿐 · run 줄에 `${{ }}` · echo · printenv · set -x 0.
- 실행 단계를 가짜 `npm`으로 GitHub 셸(`bash --noprofile --norc -eo pipefail`) 그대로 돌림: 빈 코드 → `npm run push --`(인수 0) · 코드 → `--test <코드>` · `"; printf INJ; $(…) \`…\`` 같은 값도 글자 그대로 인수 1개(실행 0) · 종료 코드 0/1/2가 tee 뒤에도 그대로 · 요약 = 코드 블록.
- 시험 실행 10:00~24:00 KST 규칙은 코드가 아니라 운영 규칙(계획대로) — 06:30~10:00 빈 코드 수동 실행은 그날 못 받은 기기에 실제로 나감(9절 ④).
- 참고: 진짜 npm은 머리 줄 `> tsx --env-file-if-exists=.env server/push.ts --test <코드>`를 찍음(심사 확인, DB 접속 전 종료하는 빈 DATABASE_URL로) → 시험 실행의 요약에 입력 코드가 남음(7절 2).

### 5.5 서비스 워커

- VM(심사가 새로 쓴 가짜 self, `j-sw.mjs`): push 21가지(아침 · 시험 · 데이터 없음 · JSON 아님 · null · 배열 · 칸 빠짐 · 숫자 body · tag `ISSUE!` · 65자 · 바깥 url · `//` · 상대 · 81자 제목 · 빈 제목 · 201자 body · 다른 칸 actions/image/silent/requireInteraction) → 모두 waitUntil 1 · showNotification 정확히 1 · 옵션 키 = body · icon · badge · tag · data · lang 정확히(80자 제목 · `/p?x=1`은 그 값, 나머지는 아침 값) / 같은 tag 2개 닫고 다른 tag 그대로 → show(renotify 없음) / getNotifications 실패여도 show 1 / 클릭: close 1 · matchAll(window · includeUncontrolled) · 같은 출처 창만 focus → navigate(`http://localhost:3000/p`) · navigate 실패 · focus 실패 · 창 없음 · 다른 출처 창만 · 잘못된 창 url → openWindow(`/p`) · data.url 틀림 · 없음 → `/p` · `/p?x=1`은 그대로 / install = v2에 offline.html · skipWaiting · activate = v0 · **v1** · other 삭제 · claim / fetch = 같은 출처 GET 탐색만(`/api/` · `/_vercel/` · POST · 탐색 아님 · 다른 출처 0) · 실패 → offline.html / HEAD의 install · activate · fetch 덩어리와 글자 같음(캐시 이름만 v2).
- 실제 Chromium 155(A): 360 첫 로드 → v2 activated · controller · 캐시 = `issuerank-v2: [/offline.html]` · `ServiceWorker.deliverPushMessage`(페이지 세션) → `getNotifications()` 아침 1 → 같은 것 다시 → 1(대체) → 시험 → 2 → `not json {` → 2(아침 값으로 대체) · 값 = 제목 · body '' · tag · data.url `/p` · icon-192 · badge-96 · lang ko · renotify/silent/requireInteraction false · actions 0 · image ''.
- v1 → v2(B, 새 프로필, 세는 프록시가 HEAD sw.js): v1 설치 · 캐시 v1만 · push → 알림 0(처리 없음) → 표지 끄고 `update()` → v2 activated · 캐시 v2만 · push → 알림 1.
- 클릭(notificationclick)은 CDP로 실제 알림을 누를 수 없어 VM으로만(계획 10번 방식).

### 5.6 화면 · 상태 · 회귀 (실제 Chromium 155 headless)

- 폭 표(D · E · F — 서버 요청 0: localhost를 닫힌 포트로, 페이지 요청은 CDP Fetch로 저장소 파일 + 실제 서버에서 한 번 받은 응답): 지금 판 · HEAD 판 × 상태 5(종만 · 설치 보임 · 로그인 홍길동 + 북마크 123 · 설치 + 8자 이름 + 123 · 설치 + 한글영문 12자 + 42) × 휴대폰 꼴(덮개 스크롤바)/데스크톱 꼴(스크롤바 15px) × 글꼴 2(대체 글꼴 — CDN 막힘 · **로컬 Pretendard Bold를 모든 굵기로**(실제 600보다 넓은 보수적 대용, F에서 `document.fonts.check` 참 확인)). 폭 = 320 · 340 · 360 · 400 · 401 · 640 · 641 · 768 · 899 · 900 · 960 · 1024 · 1100 · 1150 · 1199 · 1200 · 1280 + 경계 1px/5px 훑기.
  - 지금 판 전부: **가로 넘침 0 · 겹침 0 · 칸 밖 0 · 400px 이하 헤더 설치 0 · 종 34×32 · 설치 · 북마크 버튼 글자 눌림 0**, 이름 말줄임 = 대체 글꼴 2/170경우(HEAD 31) · Pretendard 0/300(HEAD 29).
  - 문제 있는 경우 수(꺾임 · 줄바꿈 포함): 대체 글꼴 지금 7/170 · HEAD 38/170, Pretendard 지금 9/300 · HEAD 51/300, 데스크톱 꼴 경계 훑기(E, 대체 글꼴 215경우) 지금 11 · HEAD 42.
  - 휴대폰 꼴 640px 이하 전환 한 줄(모든 상태 · 두 글꼴).
  - 지금 판에 남은 문제(괄호 = HEAD도 같은지): 641px 8자 이상 이름 전환 줄바꿈(HEAD 같음 — 계획이 허용한 기록) · **데스크톱 꼴 320~322px + 북마크 123 전환 두 줄(새로 생김, 휴대폰 해당 없음 — 7절 5)** · **1200~1220px(대체 글꼴) / 1200px(Pretendard) + 설치 + 12자 섞인 이름 → 설치 · 북마크 글자 꺾임(새로 생김, HEAD는 1100px대에서 같은 꺾임 — 8절)**.
  - HEAD보다 나아진 경우: 대체 글꼴 37(D, 170경우 중) · Pretendard 48(F, 300경우 중) — 641~1100px 설치 · 북마크 글자 꺾임 · 320~340px 전환 줄바꿈 등. (D · E의 "pretendard" 열은 글꼴이 안 불린 무효 판이라 버림 — 5.8)
  - 글자 규칙: 1199px 이하 `#installBtn .lbl` · `#bmNav .lbl` none, 1200px 이상 block, 640px 이하는 HEAD 규칙으로 원래 none.
- 색 · 다크(360 · 1280 × 밝게/어둡게 × 4상태 = 16): off = `--muted` · 채움 none / on = `--text` · `fill: currentColor` / blocked = `--muted` + 사선 inline / unsupported = 불투명도 0.55 — 16/16 토큰과 같음. 그림 `d-header-{360,1280}-{light,dark}-{off,on,blocked,unsupported}.png`.
- 종 동작(실제 서버 A · A2, 가짜 pushManager — 진짜 subscribe 0, 원래 함수는 부르지 않음): off → 누름 → on(`아침 알림을 켰습니다` · POST · 실제 DB 행 1) → `/widget#push` `이 기기: 아침 알림 켜짐 · 기기 코드 be5e168b(시험 알림용)` = Node에서 계산한 SHA-256 앞 8자 → 다시 누름 → unsubscribe 1 · DELETE · 실제 행 0 · `아침 알림을 껐습니다` + `다시 켜기` → 토스트 단추 → 다시 on → off / subscribe가 400ms 걸릴 때 빠르게 두 번 → subscribe 1 · POST 1 / 권한 거절 → `blocked` · 사선 · `알림이 허용되지 않았습니다` · POST 0 → 다시 누름 → `알림이 차단되어 있습니다` + `방법 보기`(subscribe 0) → `/widget#push` · `꺼짐` / 모든 POST · DELETE에 Origin `http://localhost:3000`. A2의 구독 요청은 CDP Fetch로 페이지에서 204를 돌려 서버 · DB 0.
- 상태(D): PushManager 없음 · Notification 없음 → `unsupported` · `아침 알림 — 켜는 방법 안내` · 0.55 · 누르면 링크 그대로 `/widget#push` → `이 브라우저에서는 아침 알림을 받을 수 없습니다` / 처음부터 denied → `blocked` / SW 등록 없음 → `잠시 후 다시 눌러 주세요` · subscribe 0 / `/widget` 등록 없음 → `이 기기: 아침 알림 꺼짐`. `/widget#push` 절 순서 = `#app` → `#push` → iPhone → Android · 문구 = 계획 · 360 넘침 0 · 다크 그림.
- 회귀(D, 서버 0): `/?issue=2026-10-20-pol-3` · `/s?issue=` → 상세창 · 제목 · 쿼리 지움(`/` · `/s` 남음) / `/p` → 이슈맵 · 상세창 0 · 주소 `/p` / 공유(1280 · 복사) → `링크를 복사했습니다` · 클립보드 = `[정치] {제목}\nhttps://issuerank-kappa.vercel.app/s?issue=…`(요약 0) / 설치(가짜 이벤트) 1280 · 412 → 헤더 앱 설치 → prompt 1 · 숨김 / 맨 아래 `앱 설치` 320 · 360 · 400 보임(헤더 설치는 none) / 로그인 창 · Google 버튼 · 이메일 칸 / 오프라인(C: A 프로필 + 닫힌 포트) `/` · `/p` · `/s?issue=` → `인터넷에 연결되어 있지 않습니다` + `다시 시도`, `/api/rankings` 탐색은 SW 밖(브라우저 오류 화면 — O4 규칙) / O2 실제 서버(A2) Google 버튼 → `지금은 Google 로그인을 사용할 수 없습니다`.
- D20: HEAD 판 · 지금 판(같은 저장 응답) 이슈맵 타일 30개 좌표 · 크기 · class · style이 360 · 768 · 1280에서 같음.
- JS 예외 0(모든 세션). 바깥 시도 = jsdelivr 글꼴 CSS(이름 풀이 막힘)뿐.

### 5.7 DB 0007(읽기 전용)

- SQL: `CREATE TABLE "push_subscriptions"`(id text PK · endpoint · p256dh · auth text NOT NULL · created_at timestamptz default now() NOT NULL · last_sent_on date) · `CONSTRAINT push_subscriptions_endpoint_unique UNIQUE(endpoint)` + `ENABLE ROW LEVEL SECURITY`뿐(DROP · DELETE · 다른 표 ALTER 0).
- 운영 DB(읽기 전용 트랜잭션): 열 6(위와 같음) · 제약 pkey + endpoint unique · 인덱스 2(고유) · RLS 켜짐 · force 아님 · 정책 0 · 행 0 · public 표 10개 모두 RLS 켜짐 · `drizzle.__drizzle_migrations` 8행 — **모든 행 hash = 0000~0007 파일 sha256 · created_at = journal when**.

### 5.8 내 검사가 처음에 틀렸던 것(제품 문제 아님)

| 원시 결과 | 실제 | 처리 |
|---|---|---|
| A 1회차 `deliverPushMessage wasn't found` | 브라우저 수준으로 보냄(이 명령은 페이지 세션) | 페이지 세션으로 고쳐 2회차 OK(1회차는 첫 로드 8요청만 씀) |
| A 2회차 `다시 켜기` 대기 시간 초과 | DB 확인(proot 기동 몇 초) 사이에 토스트(3.6초)가 사라짐 | A2에서 토스트 단추를 바로 누름 → OK |
| A2 1회차 빠른 두 번 누름 → POST 2 | 가짜 subscribe가 바로 끝나 두 번째 누름이 처리 뒤에 옴 + 내 대기 조건이 같은 상태를 바로 통과 | subscribe 400ms + 카운터 대기로 다시 → subscribe 1 · POST 1 |
| D · E의 Pretendard 결과 = 대체 글꼴과 같음 | 가로챈 글꼴 응답에 CORS 머리가 없어 글꼴이 안 불림 | 머리 더해 F에서 다시(불림 확인), D · E의 Pretendard 열은 버림 |
| j-crypto 1회차 "unsettled top-level await" | `AbortSignal.timeout` 타이머는 unref라 소켓 없는 가짜 fetch에선 Node가 먼저 끝남 | 시험 중 이벤트 루프를 붙잡아 10초 타이머 확인(제품은 실제 소켓이 붙잡음) |

## 6. 위반

없음. 기사 본문 저장 · 표시 0(알림 본문 = 고정 문구) · 코드에 키 · 접속 정보 0(공개 키는 비밀 아님, 누출 대조 0) · `.env` git 제외 · PLAN 밖 패키지 0(lockfile = HEAD) · D0 밖 수집 0 · 새 문장에 이메일 0(privacy 문의처 절에만) · `web/` · ex01~ex11 변경 0 · AGENTS.md 규칙 위반 0.

## 7. 발견한 점(심각도 순)

1. **[고침 확인 10-20 — 11절] 중간 · 커밋 전 필수 — 받는 푸시 서비스 검사 우회(SSRF 규칙 위반)**: `server/push-config.ts`
   ```ts
   const EXACT: Record<string, PushService> = { 'fcm.googleapis.com': 'fcm', 'updates.push.services.mozilla.com': 'mozilla' };
   ...
   if (EXACT[host]) return EXACT[host];
   ```
   객체 리터럴을 호스트로 바로 찾아 `constructor` · `__proto__`(URL이 소문자로 바꿔도 그대로인 원형 속성 이름)가 참 값으로 통과 → 구독 API 204 · 저장(curl로 Origin을 위조하면 누구나) → 발송 직전 재검사도 통과 → GitHub Actions가 매일 `https://constructor/<공격자가 고른 경로>`로 VAPID JWT · 암호화 본문을 담아 POST(이름 풀이는 러너의 검색 도메인에 맡겨짐). 실패 = `연결 실패` → 되돌림이라 행이 지워지지 않아 **매일 push 실행이 종료 1(빨강)**. 고칠 곳 1줄: `if (Object.hasOwn(EXACT, host)) return EXACT[host];`(또는 `Map`/`Object.create(null)`). 테스트 권장: `push-api.test.ts` 400 묶음에 `https://constructor/x` · `https://__proto__/x`, `push.test.ts` 발송 직전 재검사에 같은 행.
2. **[고침 확인 10-20 — 11절] 낮음 · 선택 — 시험 실행 요약에 기기 코드**: `npm run push -- --test <코드>`의 npm 머리 줄(`> tsx … server/push.ts --test <코드>`)이 tee로 실행 요약에 들어감(계획 "요약에 기기 코드 0"과 글자상 어긋남). 코드는 비밀이 아니고(/widget#push에 보임) Actions가 단계 env(`TEST_CODE`)를 로그에 이미 보여 주므로 영향은 작음. 고치려면 `push.yml`의 `npm run push --` → `npm run --silent push --`(요약은 제품 출력만), 아니면 계획 문구를 "제품 출력"으로 좁힘.
3. **낮음 · 설계(계획의 남용 대비 범위)**: 404/410이 아닌 오류(400 · 401 · 403 · 429 · 5xx · 연결 실패)를 계속 받는 행은 영영 지워지지 않음 — 받는 호스트 안의 엉터리 주소(예: `..push.apple.com` 빈 레이블 = 이름 풀이 실패, 서비스가 400을 주는 엉터리 토큰)도 같고, 그때마다 push 실행이 빨강. 계획이 IP 제한 없음 · 5,000행 · Vercel Firewall · 사용자 결정으로 대비를 정해 두었으므로 판정 제외 · 다음 후보: "N일 연속 실패면 삭제".
4. **낮음 · 참고(C2)**: 8절.
5. **참고**: 데스크톱 꼴(스크롤바 15px) 320~322px + 북마크 123에서 전환 버튼 두 줄(HEAD는 한 줄) — 휴대폰은 덮개 스크롤바라 해당 없고 데스크톱 창은 이 폭까지 줄지 않음(제작 9절 ②와 같음). 기록만.
6. **참고**: BFF가 앱을 못 띄울 때(502 `API 서버에 연결할 수 없습니다`) `/api/push/*` 응답에는 `Cache-Control: no-store`가 없음(인증 · 북마크 · 폴더만 붙임). POST · DELETE 응답은 기본으로 캐시되지 않아 영향 없음 — 다음에 BFF를 고칠 때 같이.
7. **참고**: `/privacy` 시행일 · 변경 줄 = 2026-10-20(오늘 배포 가정). 다른 날이면 세 곳 + `test/push-ui.test.ts` 기대 문자열(제작 보고와 같음).
8. **참고(문서)**: PLAN.md D30 글은 아직 "900px 미만은 설치 버튼 글자를 숨김"(C2는 10절 메모 · 계획 "화면 변경" 줄에만). 사용자가 C2를 확인하면 D30에 반영. → 10-20 코디네이터가 D30에 C2 한 줄을 덧붙임(문서만 — "1199px 이하로 넓히고 북마크 글자도 숨김 … 사용자 확인 목록에 포함", 11절에서 확인). HANDOFF 맨 위 요약 줄은 "다음 = 독립 심사" 그대로(이번 심사는 지시대로 O5 표 줄 · 다음 작업 O5 줄만 갱신).

## 8. 코디네이터 판단(C2) 평가

`@media (max-width: 1199px) { #installBtn .lbl, #bmNav .lbl { display: none; } }` + 주석(숫자 1199 — 규칙과 맞음).
- **문제를 바르게 풂**: 종이 들어간 뒤 HEAD보다 나빠지던 641~1199px의 `앱 설치` · `북마크` 글자 꺾임이 0(대체 글꼴 · Pretendard 둘 다). HEAD 자체에 있던 641~1100px 꺾임도 사라져 HEAD보다 나아진 경우 74 · 48.
- **범위 밖 영향 0**: 640px 이하는 HEAD가 이미 `.pill-btn .lbl`을 숨겨 변화 0, 1221px 이상 배치 = 종만 더해진 꼴. 접근성 이름은 `aria-label`(`이슈랭크 앱 설치` · `북마크`)이 그대로, 북마크 숫자는 보임. 899px 규칙의 설치 글자 숨김은 겹치지만 해 없음(간격 6px 규칙은 그대로 필요).
- **남는 것(낮음)**: ① 1200~1220px(대체 글꼴) · 1200px(Pretendard) + 설치 보임 + 한글영문 12자 이름에서 설치 · 북마크 글자 꺾임 — HEAD는 같은 이름에서 1100px대에 꺾였던 것이 위로 옮겨 온 것(넘침 · 겹침 0, 8자 이하 이름은 0) ② 641~1199px 아이콘만 남은 두 버튼에 `title`이 없어 PC 마우스 툴팁 없음(종은 있음) ③ 사용자 승인 아님 — 제작 보고 12절 끝 "C2 확인"을 사용자 확인 목록에 그대로 두고, 확인되면 PLAN.md D30 글을 맞출 것.
- 결론: **그대로 커밋해도 됨**(고칠 것 없음). 툴팁이 필요하면 다음 화면 작업에서 두 버튼에 `title` 1개씩.

## 9. 고친 뒤 다시 볼 것 · 푸시 후 절차(다음 역할)

① **(끝 — 10-20 14:13~14:24 재확인 OK, 11절) 커밋 전(제작)**: 7절 1을 고친 뒤 저장소 루트에서 —
`bash scripts/ex12-ubuntu.sh env NODE_OPTIONS=--import=./.data/judge-o5-20261020/jguard.mjs JUDGE_GUARD_FILE=/workspace/capstone/ex12/.data/judge-o5-20261020/guard-ssrf2.jsonl npx tsx .data/judge-o5-20261020/j-ssrf.mts`
기대: `proto constructor` · `proto __proto__` = null · `apiPostDelete` 400/400 · `apiVsServiceMismatch` [] · `sendCheck.sentTo` = `ok-judge` 하나 · `검사 실패 삭제 6`. 그다음 `npm test`(새 테스트 포함 통과) · `npm run build`. 이 세 가지가 맞으면 줄 8 OK → **PASS 보류**(로컬 서버 · 브라우저 · DB 다시 할 필요 없음 — 바뀌는 곳은 검사 함수 하나). 7절 2를 같이 고치면 `j-yaml.cjs`를 다시 돌려 run 줄이 `npm run --silent push -- "${args[@]}"`인지만.
② **Secret 등록(커밋 · 푸시 전, 값 출력 0)**: `grep -c '^VAPID_PRIVATE_KEY=' ex12/.env` = 1 → `grep '^VAPID_PRIVATE_KEY=' ex12/.env | cut -d= -f2- | tr -d '\r\n' | gh secret set VAPID_PRIVATE_KEY --repo Jwavascript/issuerank` → `gh secret list --repo Jwavascript/issuerank`에 `VAPID_PRIVATE_KEY`(오늘 날짜) · `DATABASE_URL`이 이름 · 날짜로만 보이면 등록 확인. 값이 코드 공개 키와 짝인지는 ⑤의 첫 실행 `키 확인 OK`로 확인(다르면 `VAPID 키가 코드의 공개 키와 다릅니다` · 종료 2 · 발송 0 → 다시 등록). Vercel env 추가 0.
③ **(끝 — 10-20 14:29 OK, 12절) 배포 줄 18**(배포 Ready 뒤 · 06:30~10:00 KST 피함 · 배포 Node ≤ 20요청): 정적 5(`/sw.js` 헤더 3 = O4 · 본문 = 저장소 v2 / `/p` = index.html / `/icons/badge-96.png` 200 image/png 바이트 같음 / `/widget` · `/privacy` = 파일) + API 9(Origin `https://issuerank-kappa.vercel.app` · endpoint `https://fcm.googleapis.com/fcm/send/o5-deploy-{uuid}` · 스크립트가 만든 키: POST 204 → 읽기 전용 SQL 행 1 · 키 같음 → 같은 endpoint 다른 키 POST 204 → 행 1 · 키 바뀜 → DELETE 204 → 행 0 → Origin 없음 403 → `evilpush.apple.com` 400 → `https://constructor/x` 400(①을 고친 판인지) → 4,097B 413) = 14요청. finally에서 `%o5-deploy-%` 행만 지우고 남은 0 · 9표 스냅숏 전후 비교(세션 수는 실제 사용자 때문에 바뀔 수 있음 — 그때는 해당 표만 기록).
④ **(끝 — 10-20 14:31 OK, 12절) 배포 줄 19**(CDP ≤ 25 — NetLog `HTTP_TRANSACTION_SEND_REQUEST` BEGIN만 셈): 새 프로필 · `--host-resolver-rules="MAP * ~NOTFOUND, EXCLUDE issuerank-kappa.vercel.app"` · 페이지에서 `/_vercel/insights/*` 막기 · 문서 전 가짜 pushManager(`jfake.mjs` 꼴, 진짜 subscribe 0) + 페이지 CDP Fetch로 `*/api/push/subscriptions*`는 로컬에서 204(배포 요청 · DB 0). 360 `/`(≈ 8: html · rankings · get-session · manifest · icon · sw.js · offline.html) → SW v2 activated · 캐시 `issuerank-v2: [/offline.html]` · `Page.getInstallabilityErrors` [] · 헤더 넘침 0 · 종 34×32 `off` → `Browser.grantPermissions(notifications)` → **페이지 세션에서** `ServiceWorker.enable` · `ServiceWorker.deliverPushMessage`(registrationId는 `workerRegistrationUpdated`에서) 아침 JSON 1번 → `getNotifications()` 1(제목 · tag · data.url `/p`, 아이콘 · 배지 ≈ 2요청) → 종 누름 → on → 바로 다시 누름 → off(토스트 단추는 3.6초 안에) → 320으로 바꿔 넘침 0(다시 읽지 않음) ≈ 12~16요청. 배포 호스트 밖 요청 0 · 프로필 삭제.
⑤ **줄 20(코디네이터, 푸시 뒤 10:00~24:00 KST에만)**: `gh workflow run push.yml --repo Jwavascript/issuerank`(코드 비움) 1번 → `gh run list --workflow push.yml --repo Jwavascript/issuerank -L 1` → 그 실행 요약: 초록 · `아침 알림 {날짜} · {HH:MM} KST` · `키 확인 OK` · `구독 n · 이미 보냄 n · 대상 n` · `보내지 않음: 10:00 지남`(Actions라 `로컬 실행` 줄 없음) · 요청 0 · daily 실행 · daily.yml 그대로. 00:00~10:00에는 빈 코드 수동 실행 금지(03:30~10:00이면 기다렸다가 실제로 보냄). `-f test_code=`는 사용자가 코드를 준 뒤에만(그 기기에 실제 알림).
⑥ 사용자 줄 21 · 22 · 24 · iPhone 23은 계획 "사용자 확인 순서" 그대로 + C2 확인(태블릿 · 좁은 PC 창 641~1199px 아이콘(+숫자)).

## 10. 요청 · 비용 · 정리

- 배포(12절, 새 별도 카운터): 줄 18 Node **11/20** · 줄 19 CDP **10/25**(NetLog) · 배포 호스트 밖 0 · 시험 행 `judge-o5-deploy-` 1(곧바로 API로 지움, 끝 0행).
- **로컬 3000: 86/100**(처음 심사 82 + 11절 재확인 4 · 심사 별도 카운터, 503 · 차단 0). 처음 심사 82의 나눔: Node 18(줄 14) · CDP A 1회차 8 · A 2회차 21(첫 로드 5 · push 전달 중 아이콘/배지 9 · POST 1 · `/widget` 3 · DELETE 1 · sw.js 확인 등 2) · A2 1회차 6 · 2회차 17(로드 5 · `/widget#push` 4 · 다시 로드 5 · Google `sign-in/social` 1(404 → 안내 문구) · 탐색 뒤 sw.js 확인 등 2) · B 12(v1 첫 로드 8 · update 2 · 배지 등 2) · C · D · E · F 0. 3001 직접 0(끝 확인 curl은 연결 거부).
- 바깥: newsdata · OpenAI · Google · Kakao · 배포 주소 · GitHub API 0 · **실제 푸시 서비스 0 · 진짜 subscribe 0**(가짜가 원형 함수를 바꾸고 원래 함수는 한 번도 부르지 않음). Chromium = `MAP * ~NOTFOUND`(바깥 시도 = jsdelivr 글꼴 CSS뿐) · next 서버 바깥 연결 = DB 2(가드 기록) · Node 심사 스크립트 = DB · 127.0.0.1 · tsx 파이프만, fetch 바깥 시도 0 · npm 내려받기 0(`npx --offline`, 이미 있는 캐시).
- DB: 시작 · 끝 스냅숏(읽기 전용) 9표 + push_subscriptions + 마이그레이션 기록 **건수 · 내용 md5 모두 같음**(articles 1279 · issues 897 · collect_runs 102 · bookmarks 2 · folders 2 · user 2 · session 2 · account 1 · verification 0 · push 0 · 기록 8). 시험 행 = `judge-o5-` 5개(API 1 · 종 1 · 발송 3) — API DELETE 1 · 종 끄기 DELETE 1 · 제품 410 처리 1 · `cleanup` 2로 모두 지움, 남은 0. 임시 계정 0.
- 비밀값: 모든 출력 · 결과 파일 · 서버 로그를 .env 비밀값 10개 대조 뒤에만 남김(누출 false) · 심사 증거 138파일 누출 0 · JWT · Authorization 값 저장 0. 서버 로그 안전 코드 = `AUTH_INTERNAL_ERROR` 1(로컬 Google 버튼, O2 동작).
- 프로세스: 시작 전 목록(`ps-before.txt`, code-oss 30854). 내가 띄운 것 = 서버(proot 16788 → j-server 16905 → next-server 16913, 멈춤 표지로 next 그룹 종료) · Chromium 9번(20958 · 21539 · 23696 · 24957 · 25982 · 26827 · 30726 · 3848 · 6560, 각각 `Browser.close` + 내 그룹만 종료, `mineAfter` 모두 []). 끝 확인: curl 3000 · 3001 · 9531~9537 = 000 · `ps`에 next-server · next start · j-server · 내 chromium · tsx · vitest 0 · 시작 뒤 새로 생긴 것 = 원래 있던 사용자 프로세스(xfce 패널 · `usb-bookmarks --watch`)의 자식과 확인 셸뿐 · **code-oss 30854 살아 있음** · 넓은 패턴 kill 0. 프로필 5개(prof-a = A · A2 · C 공용, prof-b · d · e · f) · 임시 폴더(`keys-tmp` · `dk` · 그림 `gen1/gen2` · scratchpad `judge-o5`) 삭제. `ex12/.next`는 심사의 깨끗한 빌드 산출물(git 제외).

## 11. 줄 8 재확인(10-20)

- 때 · 요청: 2026-10-20 14:13~14:24 KST, 코디네이터 요청(제작이 9절 ①대로 고침 — 제작 보고 15절 "심사 FAIL 수정(10-20)"). 제작 보고 · 제작의 `jr-ssrf.mts` · 요약 흉내는 주장으로만 보고, 아래는 모두 심사 스크립트로 다시 함. 코드 수정 0 · 커밋 · 푸시 0.
- **바뀐 것 = 말한 파일 · 줄뿐(바이트로 확인)**: 처음 심사 시작(13:04)과 끝(14:01)의 작업 트리 해시는 같았음 → 지금과 비교하면 바뀐 것 = `ex12/server/push-config.ts` · `.github/workflows/push.yml` · `ex12/test/push-api.test.ts` · `ex12/test/push.test.ts` + 문서(PLAN.md · HANDOFF.md · 제작 보고 · 이 보고)뿐. 지금 파일에서 말한 변경만 되돌리면 **심사 때 sha256과 정확히 같음**(= 그 밖 글자 변경 0):
  - `push-config.ts` 39줄 `if (Object.hasOwn(EXACT, host)) return EXACT[host];`(옛 `if (EXACT[host]) …`로 되돌리면 `3bf1d378…`)
  - `push.yml` 54줄 `npm run --silent push -- "${args[@]}" 2>&1 | tee …`(`--silent `만 빼면 `f49c95f7…`)
  - `push-api.test.ts` 80 · 81줄 400 묶음 2줄(`원형 속성 constructor` · `원형 속성 __proto__`)(빼면 `29865601…`)
  - `push.test.ts` 270줄(두 행을 저장소에 바로 넣음) + 275줄 기대 `검사 실패 삭제 2` → `4`(되돌리면 `8c188f47…`)
  - 그 밖 O5 파일 25개(테스트 포함 — index.html · sw.js · routes/push · push-run · webpush · store · sample · openapi · push-sw/push-ui/pwa 테스트 등) = 심사 때 해시 · SPEC.md · 계획 · O4 심사 보고도 그대로 · 보호 60파일 = HEAD · package.json(scripts 2줄) · package-lock 그대로(새 패키지 0) · ex01~ex11 · `web/` · frontend 0.
- **SSRF 다시(심사 스크립트 · 가드 — 바깥 시도 0)**:
  - `j-ssrf.mts`(처음 표 그대로): 받는 경우 = 처음과 같은 16가지(원형 이름 2개만 빠짐) · `proto constructor` · `__proto__` · `tostring` · `hasownproperty` · `valueof` · `__definegetter__` = null · POST/DELETE 400/400 · `apiVsServiceMismatch` [] · 발송 직전: 7행 중 6행 삭제(`검사 실패 삭제 6`) · 보낸 곳 = `…/ok-judge` 하나 · 종료 0.
  - 새 `j-ssrf-proto.mts`: `Object.prototype`의 상속 이름 12개 전부(constructor · `__proto__` · hasOwnProperty · isPrototypeOf · propertyIsEnumerable · toString · toLocaleString · valueOf · `__defineGetter__` · `__defineSetter__` · `__lookupGetter__` · `__lookupSetter__`) × 그대로/소문자 = 호스트 22 × 경로 2 = **44 endpoint → `pushService` 모두 null · 구독 API POST/DELETE 모두 400 · 저장 0** / 저장소에 바로 넣은 44행 + 정상 1 → Actions 모드 · 가짜 fetch → **요청 1(정상)뿐 · `검사 실패 삭제 44` · 원형 이름 행으로 fetch 0**.
  - **실제 next start + BFF**(심사 서버 다시 띄움, 로컬 4요청): POST · DELETE `https://constructor/judge-o5-proto` · `https://__proto__/judge-o5-proto` → 모두 400 `요청 형식이 올바르지 않습니다` · no-store · Set-Cookie 0 → 실제 DB `push_subscriptions` 0행(읽기 전용 확인, 400이라 저장 경로에 가지 않음 · 시험 행 0).
  - 깨끗한 빌드의 구독 라우트 청크에 `Object.hasOwn(el,t))return el[t]`(고친 검사가 Vercel 함수 번들에 들어감).
- **회귀 · 테스트 · 빌드**: `j-api.mts` · `j-run.mts` · `j-crypto.mts`를 다시 돌린 결과 JSON = 처음과 같음(시간 값만 다름) · `npm test` **26파일 268/268**(새 경우는 기존 테스트 안에 더해 개수 같음) · vitest 뒤 git status 같음 · `.next` 지우고 `npm run build` **성공**(next build + tsc) · 누출 false · `.next/server` 발송 표지(`WebPush: info` · aes128gcm · nonce · `vapid t=` · `PUSH_PAYLOAD_TOO_LARGE` · ieee-p1363) 0 · `.next` 204파일 누출 0. 처음 심사의 j-ssrf(고치기 전: 원형 이름 204 · `https://constructor/x`로 실제 POST)는 새 테스트 기대(400 · 요청 ok 하나)를 어기므로 새 테스트는 고치기 전 코드를 잡아냄(제품 파일을 잠깐 바꾸는 시험은 코드 수정 금지라 하지 않음).
- **push.yml 요약에 기기 코드 0(심사 방법, Actions 실행 0)**: 지금 push.yml의 run 블록(캐시된 yaml 파서로 뽑음)과 `--silent`만 뺀 옛 블록을 GitHub `shell: bash` 꼴(`bash --noprofile --norc -eo pipefail`)로, **실제 npm 11.16 · 실제 `push` 스크립트**로 돌림 — `GITHUB_ACTIONS=true` · `TEST_CODE=abcd1234`(형식 맞음 → 키 확인 · 형식 검사를 지나 저장소 읽기까지 감) / 빈 코드(아침 규칙), `DATABASE_URL` = 닫힌 로컬 포트(실제 DB 0) · 가드(fetch 차단 · 연결 127.0.0.1만), VAPID 키는 `--env-file-if-exists=.env`. 결과(`wfsim/summary-*.md`): 옛 줄 = 요약에 `> tsx --env-file-if-exists=.env server/push.ts --test abcd1234`(코드 1) / **새 줄 = 요약이 제품 출력 3줄뿐**(`아침 알림 2026-10-20 · 14:19 KST` · `키 확인 OK` · `DB 오류: connect ECONNREFUSED 127.0.0.1:9`) — 코드 · npm 머리 줄 · `npm error` · `https://` 0 · 4경우 모두 종료 2가 tee 뒤에도 그대로 · 누출 false · 연결 = 닫힌 127.0.0.1:9뿐. (GitHub이 단계 로그 머리에 env `TEST_CODE` 값을 보여 주는 것은 요약 밖 — 코드는 비밀 아님.)
- **같은 꼴(일반 객체를 계산한 키로 찾는 곳) 따로 훑기**: O5가 더한 줄(새 파일 전부 + 바뀐 파일의 + 줄)에서 `이름[계산한 키]`는 `push-config` `EXACT[host]`(고침 — `Object.hasOwn` 뒤에만 읽음) · `push-run` `targets[next++]`(배열 번호) · `sw.js` `p[k]`(k = 고정 4개, `Object.prototype`에 그 이름의 문자열 없음) · index.html `PUSH_LABEL[value]`(value = 코드의 고정값 4개 — `setPush` 호출)뿐. `in` 연산자는 `'serviceWorker' in navigator` 같은 기능 확인(글자 키)뿐 · `?.[` · `Reflect` 0 · 집계 · JWT · 메모리 저장소는 `Map`. → 제작의 "바깥 입력은 39줄뿐"과 같음.
- **문서**: PLAN.md D30에 C2 한 줄(코디네이터, 문서만) 확인 — 7절 8 해결. HANDOFF 맨 위 줄 · O5 표 줄 · 다음 작업 줄은 이 재확인 결과로 갱신.
- **요청 · 쓰기 · 정리**: 로컬 3000 이번 4(누계 **86/100**) · 바깥 0(newsdata · OpenAI · Google · Kakao · 배포 · GitHub API · 푸시 서비스 · 진짜 subscribe 0) · DB 쓰기 0(시험 행 0, 읽기 전용 `judge-rows` 1번 — push_subscriptions 0행) · 서버(proot 23749 → j-server 23771 → next-server 23812) 멈춤 표지로 종료 · curl 3000 · 3001 = 000 · next-server · j-server · 내 chromium 0 · code-oss 30854 살아 있음 · 임시 `wfsim/tmp-*` 지움(요약 · 단계 스크립트 · 가드 기록은 증거로 남김).
- **판정 바뀜**: 줄 8 [FAIL] → [OK] · 1절 SSRF wrong → done · 7절 1 · 2 고침 확인 → **PASS 보류(OK 17 · FAIL 0 · SKIP 7)**. 남은 발견 = 7절 3(낮음 · 설계: 404/410 아닌 오류가 계속되는 행은 안 지워짐) · 4(C2 — 낮음 · 참고) · 5 · 6 · 7(참고).

## 12. 배포 줄 18 · 19 (2026-10-20)

- 때 · 대상: 2026-10-20 14:28~14:32 KST(06:30~10:00 피함), `https://issuerank-kappa.vercel.app`(Production). 코디네이터: 커밋 `249c5d7`(O5 전체 — 11절에서 재확인한 작업 트리 그대로, 로컬 HEAD = `249c5d7` · 작업 트리 깨끗 확인) 푸시 · Vercel 배포 success 14:26 · GitHub Secret `VAPID_PRIVATE_KEY` 등록 14:25(이름 · 날짜만). 심사는 줄 18 · 19만(줄 20 push 수동 실행 0 · Actions 0 · GitHub API 0).
- DB(읽기 전용 스냅숏, 시작 14:28:37 · 끝 14:31:36): 9표 + push_subscriptions + 마이그레이션 기록 건수 · 내용 md5 **모두 같음**(push_subscriptions 0행 → 0행, 11절 뒤 심사 끝 스냅숏과도 같음).

### 줄 18 [OK] — Node fetch, 배포 요청 **11/20**(`j-deploy18.mjs`, 가드: 배포 호스트 · DB만 허용, 기대와 다르면 멈추는 꼴 — 멈춤 0)

| 요청 | 결과(기록한 머리 값) |
|---|---|
| `GET /sw.js` | 200 · `Content-Type: application/javascript; charset=utf-8` · `Cache-Control: no-cache, no-store, must-revalidate` · `Content-Security-Policy: default-src 'self'; script-src 'self'` · `Content-Encoding: br` · `x-vercel-cache: MISS` · `age: 0` · 본문 3,603B = 저장소 파일(`issuerank-v2` · push 처리 있음) |
| `GET /p` | 200 · `text/html; charset=utf-8` · `public, max-age=0, must-revalidate` · br · MISS · 85,686B = `index.html`(rewrite) |
| `GET /icons/badge-96.png` | 200 · `image/png` · `public, max-age=0, must-revalidate` · MISS · 611B = 저장소 |
| `GET /widget` · `GET /privacy` | 200 · `text/html; charset=utf-8` · `public, max-age=0, must-revalidate` · br · MISS · 9,509B · 8,417B = 저장소 |
| `POST /api/push/subscriptions`(Origin = 배포 주소, `…/judge-o5-deploy-{uuid}` · 스크립트 키) | 204 · 본문 0 · `no-store` · Set-Cookie 0 → **실제 DB 행 1**(키 같음 · last_sent_on null) |
| `DELETE`(같은 endpoint) | 204 · `no-store` → **실제 DB 행 0**(API 삭제 — DB 직접 삭제 대비 길은 쓰지 않음) |
| Origin 없음 POST | 403 · `{"message":"요청 형식이 올바르지 않습니다"}` · no-store |
| `https://evilpush.apple.com/…` POST | 400 · 같은 문구 · no-store |
| `https://constructor/…` POST | 400 · 같은 문구 · no-store(11절 수정이 배포됨) |
| 4,097B 본문 POST | 413 · 같은 문구 · no-store |

- 시험 행: `judge-o5-deploy-` **1행만**(약 1초 살아 있음 · 그 행으로 발송 0) → API DELETE로 지움 → 끝에 push_subscriptions 0행 · judge 0. 바깥: fetch · 연결 = 배포 호스트 11 · 6연결 + DB 1뿐(가드 기록, 차단 0).
- 참고: 로컬 next start는 `text/html; charset=UTF-8` · `public, max-age=0`, Vercel은 `text/html; charset=utf-8` · `public, max-age=0, must-revalidate` — 플랫폼 기본 차이(본문은 같음).

### 줄 19 [OK] — CDP, 배포 요청 **10/25**(`j-deploy19.mjs`, NetLog `HTTP_TRANSACTION_SEND_REQUEST` BEGIN만 셈)

- 설정: Termux Chromium 155 headless · **새 프로필**(끝에 삭제 확인) · `--host-resolver-rules="MAP * ~NOTFOUND, EXCLUDE issuerank-kappa.vercel.app"` · `--disable-quic` · 페이지에서 `/_vercel/*` 막음 · 문서 전 가짜 pushManager(진짜 subscribe 0) · 구독 API는 페이지 CDP Fetch로 로컬 204(배포 요청 · DB 행 0) · Google 버튼 · 로그인 누르지 않음.
- 360×800 `/`: SW scope `/` · `/sw.js` · **activated** · controller 있음 · 캐시 = `issuerank-v2: [/offline.html]` · `Page.getInstallabilityErrors` = [] · manifest 오류 0 · 타일 30 · 헤더 넘침 0 · 전환 버튼 34px(한 줄) · 헤더 설치 숨김 · 종 34×32 `off` · `아침 알림 받기`.
- `Browser.grantPermissions(notifications)` → **페이지 세션** `ServiceWorker.deliverPushMessage`(아침 JSON) → `getNotifications()` **1**: 제목 `오늘의 뉴스가 도착했어요` · body '' · tag `issuerank-daily` · data.url `/p` · icon `https://issuerank-kappa.vercel.app/icons/icon-192.png` · badge `…/icons/badge-96.png` · lang ko · actions 0.
- 종: 누름 → subscribe 1(`userVisibleOnly: true` · 키 65B = 코드 공개 키 · userActivation true · click 틱) → `on` · `아침 알림 끄기` · `아침 알림을 켰습니다` → 다시 누름 → unsubscribe 1 → `off` · `아침 알림을 껐습니다` + `다시 켜기`. 가로챈 요청 = POST(Origin = 배포 주소 · keys 있음) · DELETE 각 1 — 배포로 안 감.
- 320(다시 읽지 않음): 넘침 0 · 전환 34px · 종 34×32. 그림 `deploy19-360.png` · `deploy19-320.png`.
- 배포 요청 10 = `/` · `/api/rankings` · `/api/auth/get-session` · `/manifest.webmanifest` · `/icons/icon-192.png` ×3(파비콘 · manifest · 알림 아이콘) · `/sw.js` · `/offline.html` · `/icons/badge-96.png`. **배포 호스트 밖으로 보낸 요청 0**(jsdelivr 글꼴 CSS는 이름 풀이에서 막힘). JS 예외 0.

### 정리

- 프로세스: Chromium 10471 하나(`Browser.close` + 내 그룹만, `mineAfter` []) · CDP 9541 = 000 · 내 chromium · next 0 · 새로 남은 것 = 사용자 `usb-bookmarks` 자식 · 확인 셸뿐 · code-oss 30854 살아 있음. 프로필 · NetLog(scratchpad `judge-o5-deploy/`) 삭제. 결과 파일 누출 검사 false.
- 남은 줄: **20(코디네이터)** — 푸시 뒤 10:00~24:00 KST에 빈 코드로 1번(9절 ⑤, 기대 `키 확인 OK` · `보내지 않음: 10:00 지남` · 요청 0) · 21 · 22 · 24(사용자) · 23(iPhone).

## 13. 줄 20 (코디네이터, 2026-10-20)

- 14:33 KST(10:00~24:00 창 안) `gh workflow run push.yml --repo Jwavascript/issuerank`(test_code 비움) 1번 → run `39375573798`(workflow_dispatch · headSha `249c5d7`, 05:33:57Z 시작 · 05:34:22Z 끝) **success**, 단계 8개 모두 success(checkout · setup-node · `npm ci` · `npm run push`).
- `npm run push` 출력(= 실행 요약 본문): `아침 알림 2026-10-20 · 14:34 KST` · `키 확인 OK`(Secret `VAPID_PRIVATE_KEY`로 계산한 공개 키 = 코드 공개 키) · `구독 0 · 이미 보냄 0 · 대상 0` · `보내지 않음: 10:00 지남` → 발송 요청 0. 기기 코드 · endpoint · 키 0(로그도 같음).
- 참고(낮음, 고치지 않음): 출력 맨 앞에 `.env not found. Continuing without it.` 두 줄 — Actions에는 `.env`가 없어 생기는 안내(비밀 · 코드 없음)이고 요약에도 들어감.
- daily 실행 · daily.yml 변화 0(이번 실행은 push만). → **줄 20 OK. 합계 OK 20 · FAIL 0 · SKIP 4(21 · 22 · 24 사용자, 23 iPhone) → PASS 보류.**

## 사용자 할 일

1. (끝 — 10-20 제작 수정 · 심사 재확인 OK(11절) · 코디네이터 Secret 등록 14:25 · 커밋 `249c5d7` 푸시 · Vercel 배포 14:26 · 배포 줄 18 · 19 OK(12절)) → 남은 코디네이터 줄 20(push 수동 실행 1번, **10:00~24:00 KST에만**, 9절 ⑤).
2. 그 뒤 계획 "사용자 확인 순서" 1~8(종 → 기기 코드 → 시험 알림 → 누르기 → 다음 날 아침 → 끄기 → `/privacy` 문안) + C2 확인(641~1199px 아이콘). iPhone이 생기면 23.
