# ops O7 아침 알림 자동 다시 연결 · 요약 없음 문구 — 제작 보고

- 날짜: 2026-10-30(KST 10:30~11:45) · 역할: 제작(`agent_prompts/2_build_agent.md`) · 기준: `plan/PLAN_ops_O7_push_heal_summary_text.md`(확정, 122줄), SPEC **AC-14-5** · **AC-04-3**(고친 문장) + 계획의 회귀 AC(AC-14-1~4 · 13-1 · 13-2 · 04-1 · 04-2 · 08-1 · 05-2 · 11-2 · 06-3 · 10-5), PLAN.md 5절 D21 · D29 · D30 · D31 · **D32**
- 사용자 결정(2026-10-30, D32): 아침 알림은 앱을 열 때 자동 다시 연결 · 요약 없는 카드는 문구를 사실대로 · 심사 뒤 푸시까지(코디네이터)
- 작업 폴더: `ex12/`(D21, 복사 없음). 시작 HEAD `1b71cf7`(origin과 같음). 시작 때 미커밋 9개(`AGENTS.md` · `HANDOFF.md` · `PLAN.md` · `SPEC.md` · `plan/PLAN_ops_O7_push_heal_summary_text.md` · `reports/ops_O2~O5_*_judge.md` 4개 — 사용자 확인 기록)는 O7 코드가 아님 — 되돌리지 않았고 변경 범위 셈에서 뺌(작업 내내 sha256 그대로 확인, 끝에 `HANDOFF.md` O7 줄 · `PLAN.md` 10절 메모 1줄만 고침). 계획 "확인 필요 6"은 코디네이터가 처리(AGENTS.md 37줄 · PLAN.md 시연 점검표, 문서만)
- 하지 않음: git commit · push · Vercel/GitHub 설정 · Actions 실행(push 수동 실행 포함) · `web/` · `plan/PLAN_web.md` · 계획 밖 기능. 새 npm 패키지 0 · 마이그레이션 0
- 증거 · 검증 스크립트: `ex12/.data/o7-build-20261030/`(git 제외 폴더)

## 1. 바뀐 · 새 파일 (= 계획 "변경 범위" 목록과 정확히 같음)

| 파일 | 구분 | 내용 |
|---|---|---|
| `ex12/server/app.ts` | 고침 +1 −1 | Store `savePushSubscription(sub, allowNew): Promise<'created' \| 'updated' \| null>` + 주석 |
| `ex12/server/sample.ts` | 고침 +4 −3 | 메모리: 있던 endpoint = 키 갱신 `'updated'` · 새 endpoint = allowNew일 때만 `'created'` · 상한 = `null` |
| `ex12/server/db/store.ts` | 고침 +12 −8 | `UPDATE … WHERE endpoint RETURNING id` 1행 = `'updated'` → 0행 · allowNew = `INSERT … ON CONFLICT (endpoint) DO NOTHING RETURNING id` 1행 = `'created'` · 0행(그사이 다른 요청이 넣음) = UPDATE 한 번 더 → `'updated'` / allowNew 거짓 = `null`. id · created_at · last_sent_on 규칙 그대로(키만 갱신) |
| `ex12/server/routes/push.ts` | 고침 +5 −2 | `'created'` = **201** `{ "created": true }` · `'updated'` = **204**(본문 없음) · `null` = 503(그대로). 검사 순서 · 문구 · no-store · Set-Cookie 0 · 로그 그대로 |
| `ex12/openapi.yaml` | 고침 +22 −3 | post `'201'`(no-store · 새 스키마 `PushSubscriptionCreated` = `created: { type: boolean, const: true }` 필수 · `additionalProperties: false`) · `'204'` 설명 "이미 있던 기기(키 갱신, 본문 없음)" · post 설명 1문장 · Issue `summary` 설명의 화면 문구 · `info.version` 0.6.0 → **0.7.0**. 지운 줄 = `version` · 204 설명 · summary 설명 3줄뿐 |
| `ex12/public/sw.js` | 고침 +25 −3 | 캐시 **`issuerank-v3`** · 맨 위 주석(v3 · "fetch 처리 없는 판에도 push · notificationclick · pushsubscriptionchange는 남김") · `PUSH_KEY` 87자 1번(주석 "server/push-config.ts VAPID_PUBLIC_KEY와 같은 값(테스트가 비교)") · `pushsubscriptionchange` 처리기(2절 표) |
| `ex12/public/index.html` | 고침 +85 −3 | `summaryHTML` 한 줄(새 문구) · Push (O5) 구역(켜기 성공 때 표시 · 끄기 시작에 지움 · 켜기 주석 1줄 · 새 블록 = 상수 · 표시 읽기/쓰기 · 토스트 한도 · 확인 · 바꾸기 · 맨 아래 준비 뒤 분기 + `visibilitychange`) |
| `ex12/test/push-api.test.ts` | 기대값 +9 −9 | 새 endpoint POST 8곳 `204 · 본문 null` → `201 · { created: true }`(4종 저장 · 같은 endpoint 첫 번째 · 2,048자 경계 · BETTER_AUTH_URL 배포 · 4,096B · DELETE 묶음 앞 POST 2 · BFF POST) + 그 테스트 제목 1. 같은 endpoint 다시 · 5,000행 기존 갱신 = 204 그대로 |
| `ex12/test/pwa.test.ts` | 기대값 +8 −8 | 캐시 v2 → v3(install · activate 기록 · 남은 캐시 · 제목) + 옛 캐시 목록에 `issuerank-v2`(지워지는지) |
| `ex12/test/push-sw.test.ts` | 기대값 +3 −2 | 마지막 테스트 `const CACHE = 'issuerank-v3';` · `'issuerank-v2'` 없음 · `'issuerank-v1'` 없음 · 제목 |
| `ex12/test/push-heal.test.ts` | 새 235줄 | 구독 API · 저장소 7 + 서비스 워커 VM 7(4절) |
| `ex12/test/push-heal-ui.test.ts` | 새 435줄 | 화면 자동 다시 연결 VM 13(4절) |
| `ex12/test/summary-text.test.ts` | 새 47줄 | 요약 문구 4(4절) |

- 그대로(검증 1): 계획 목록 78 + 기존 테스트 25(바꾸는 3개 뺀 작업 전 목록) = **103개 sha256 작업 전후 같음**(`hash-before.txt` → `sha256sum -c` 모두 OK). `package-lock.json` `0b248041…` 같음. 추적 687파일 blob 전후 비교(11:30, 문서 갱신 전): 바뀐 것 = 위 고침 10개뿐(+ 새 테스트 3개). 그 뒤 고친 문서 = `HANDOFF.md` 97줄(O7 줄) · `PLAN.md` 267줄(10절 메모 1줄 더함)뿐 — 그 줄만 되돌리면 시작 sha256과 같음(확인함). ex01~ex11 · `frontend/` · `web/` · `plan/PLAN_web.md` 변경 0. 시작 때 미커밋 문서 9개 중 7개(`AGENTS.md` · `SPEC.md` · O7 계획 · 심사 보고 4개)와 `CLAUDE.md`는 끝까지 sha256 같음.
- index.html: 기존 VM 테스트가 잘라 쓰는 구역 31곳(api · loadBookmarks · allBookmarks · postBookmark~syncBookmarkUI · applyUser · 인증 · Events · DATA · 날짜 · ranking · issueById · loadRanking · ORDER · viewFromHash · issueShortcut~`start();` · openDrawer · findBookmark · Share · Header CSS · Responsive · header-right 마크업 · Init · Toast · syncBookmarkUI 본문 · renderBookmarks · Folder delete (O6) · esc · currentAccount · head · style · start 뒤) = HEAD 글자 같음, **`summaryHTML` 구역과 Push (O5) 구역을 뺀 나머지 = HEAD 바이트 같음**(`index-regions.txt`). `\nstart();\nregisterServiceWorker();\n</script>` 그대로 · `serviceWorker.register(` 1 · 87자 키 1번 · Events · Init · `start()` · 헤더 마크업 · CSS 변경 0.
- `test/push-ui.test.ts`(O5 화면)는 **수정 0으로 통과**(그 문맥엔 `localStorage` · `document.addEventListener`가 없어 저장소 막힘 길 = O5 그대로).

## 2. PLAN 항목별 반영 위치

| 계획 | 반영 |
|---|---|
| 켜 둔 표시 `issuerank-push` = `on` | `PUSH_MARK` · `pushMark`(읽기 시험까지 try — 없음 · 접근 예외 · getItem 예외 = `null` = 저장소 막힘) · `pushMarked()` · `setPushMark()`(쓰기 실패 무시). 켜기 = `pushOn` POST 성공 뒤 · 끄기 = `pushOff` 맨 앞(unsubscribe 전) · 권한 꺼짐 안내 때 지움(같은 틱, 토스트 바로 앞 — 계획 "지우고 토스트") |
| 저장소 막힘 = O5 그대로 | 맨 아래 `if (pushMark) { … } else navigator.serviceWorker.ready …`(else = O5 줄 그대로) — 막힘이면 새 코드가 `document.addEventListener` · 표시 쓰기 · 새 요청을 부르지 않음 |
| 옛 구독 기기(O5 때 켬) | `pushCheck` "있음" 길 맨 앞 `setPushMark(true)` |
| 확인 시점 · 상한 | 열 때 = `serviceWorker.ready` 뒤 `pushCheck()` 1번 / 다시 보일 때 = `visibilitychange`에서 `visible` · `Date.now() - pushChecked >= 60분`일 때만. `pushCheck`는 `pushBusy`(확인 중 · 종 처리 중)면 건너뜀, 확인 동안 `pushBusy` = 참(종 누름 무시), 시작 시각을 `pushChecked`에 |
| 순서(granted) | `getSubscription` → 있음: 종 on · `pushSub` · POST → 204 끝 / `{created: true}` = 바꾸기(이 페이지에서 했으면 끝) / 실패 = 조용히 끝 · 없음 + 표시 = 바꾸기 ③④ · 없음 + 표시 없음 = 종 off |
| 바꾸기(한 페이지 1번) | `pushReplace(old)`: ① `old.unsubscribe()` ② DELETE `{ endpoint: old }`(둘 다 실패 무시) ③ `subscribe({ userVisibleOnly: true, applicationServerKey: pushKey() })` ④ 새 구독 POST(201 · 204 성공) → on · 토스트 0 / ③ 거절 · ④ 실패 → 새 구독 unsubscribe · `pushSub` null · off · 토스트 `아침 알림 연결이 끊겼습니다` + `다시 켜기`(→ `pushOn`) · 표시 그대로 |
| 권한 꺼짐 | denied = blocked + (표시 있으면) 지움 + `알림이 차단되어 있습니다` + `방법 보기`(→ `/widget#push`) / default = off + 지움 + `아침 알림이 꺼져 있습니다` + `다시 켜기`(→ `pushOn`). subscribe · 서버 요청 0 |
| 토스트 한 페이지 1개 | `pushNotice()`(확인이 띄우는 3문구만, 기존 `toast(msg, action, fn)` 꼴 · 새 CSS 0) |
| 코드 위치 | Push (O5) 구역 안(Toast 뒤 · Share 앞 그대로). 공개 키 = `PUSH_KEY` · `pushKey()` 다시 씀 |
| POST 201 · 204 · 저장소 | 1절 `routes/push.ts` · `db/store.ts` · `sample.ts` · `app.ts` |
| 계약 | 1절 `openapi.yaml` |
| pushsubscriptionchange | `event.waitUntil((async () => { … })().catch(() => {}))`: 새 구독 = `event.newSubscription`, 없으면 `self.registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: 공개 키 바이트 })`(실패 = 없음) → 있으면 `fetch('/api/push/subscriptions', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(새.toJSON()) })` · `oldSubscription`이 있고 새 구독이 없거나 endpoint가 다르면 DELETE `{ endpoint: 옛 }`. fetch 실패는 `.catch`로 삼킴 · 알림 · 캐시 · 화면 0. 바이트 변환(`atob`)은 처리기 안에서만(맨 위 실행 코드 0) |
| 캐시 v3 | `const CACHE = 'issuerank-v3';` — install · activate · fetch · push · notificationclick 글자 그대로 |
| 요약 없음 문구 | `summaryHTML` 한 줄 → 카드(목록 · 북마크) · 이슈맵 툴팁 · 상세창이 함께 바뀜. 마크업 · 기울임 · 색 그대로 |

## 3. 서비스 워커 · 화면 코드에서 꼭 필요했던 내부 세부(계획과 다른 점 0, 꼴만)

1. `pushReplace` ③ 바로 앞 `if (Notification.permission !== 'granted') throw` — 계획 "granted일 때만 부르므로 권한 창 없음"을 그 순간에도 지키려고(확인 시작 뒤 권한이 바뀐 드문 경우 = ③ 실패 길과 같음).
2. 권한 꺼짐 길도 O5처럼 `getSubscription()` 1번 · `pushSub = sub`(O5 열 때 처리와 같은 값 — 종 누름 동작 그대로).
3. 다시 보임 처리기는 `visible` · 60분을 먼저 보고 `pushCheck()`를 부름 — `pushCheck`가 `pushBusy` · 등록 준비 전(`pushReg` null)을 건너뜀.
4. DB 저장소의 두 번째 UPDATE는 계획대로 결과와 상관없이 `'updated'`(INSERT 충돌 뒤 그 행이 곧바로 지워지는 3중 경합이면 204 — 다음 열기 POST가 201이라 바꾸기로 이어짐).
5. 그 밖 글: Push 구역 켜기 주석 "서버 저장(204)" → "(201 · 204) · 켜 둔 표시(O7)", 끄기 줄 주석 1개.

## 4. 테스트 · 빌드 · 계약

- `bash scripts/ex12-ubuntu.sh npm test`(안전 실행, 누출 false, `NPM_CONFIG_UPDATE_NOTIFIER=false`): **31파일 319개 통과**(기존 28파일 288 — 조정 3개 포함, 그 밖 기대값 · 파일 수정 0 + 새 3파일 31). vitest가 바꾼 추적 파일 0. 마지막 확인 11:29.
  - `push-heal.test.ts` 14: ① 4종 새 endpoint → 201 · 본문 `{"created":true}` 정확히 · `application/json` · no-store · Set-Cookie 0 · 행 4 ② 같은 endpoint(같은 키 · 다른 키) 204 · 본문 0 · 행 1 · id 그대로 + 같은 새 endpoint 동시 2개 = 201 하나 + 204 하나 · 행 1 ③ DELETE 뒤 같은 endpoint 201 · 새 id ④ 5,000행: 기존 204 · 새 503 `지금은 알림을 켤 수 없습니다` ⑤ 400 · 403 · 413(O5 문구 · 저장 0) ⑥ createBff 201 · 204 = 앱과 상태 · 본문 · 머리 같음 ⑦ `savePushSubscription` 직접 `'created'` · `'updated'` · `null`(쓰기 0) / SW: ①② old · new 둘 다 null · 두 칸 없음 → subscribe 1(`userVisibleOnly` · 키 65B = 코드 키) → POST 1(본문 = toJSON · JSON 머리) · DELETE 0 ③ old + 다른 new → subscribe 0 · POST new · DELETE old ④ old + new 없음(null · 칸 없음) → subscribe 1 · POST · DELETE old ⑤ old = new → POST 1 · DELETE 0 ⑥ subscribe 거절 + old → POST 0 · DELETE old 1(old 없으면 요청 0) ⑦ POST 500 · POST 네트워크 · 둘 다 네트워크 → 거절 없이 끝 · DELETE old 1 ⑧ 상수 = `VAPID_PUBLIC_KEY` · 87자 1번 · 맨 위 실행에서 `registration` · `atob` 접근 0(getter로 셈) · 처리기 6종 · showNotification · getNotifications · caches 호출 0. 모든 이벤트 waitUntil 1 · 거절 없이 끝.
  - `push-heal-ui.test.ts` 13(한 기기 = 권한 · 기기 구독 · 서버 행 · localStorage를 여러 열기가 이어 씀): ① 저장소 막힘 3모드(없음 · getItem 예외 · 접근 예외) × 5상태 = O5(열 때 getSubscription 1 · 요청 0 · addEventListener 0 · 토스트 0 · 표시 쓰기 0 · 켜기도 O5) ② 켜기 성공 = 표시 on · 503 실패 = 표시 그대로 · 끄기 = unsubscribe 전에 지움(DELETE 204 · 500 · 네트워크) → 다시 열기 off ③ 구독 + 서버 행 = POST 1 → 204만 ④ 표시 없음(O5 기기) = 표시 씀 + ③ · 쓰기 예외에도 확인 그대로 ⑤ 410 꼴(표시 있음 · 없음 · unsubscribe 실패) = `POST 옛 201 → DELETE 옛 → POST 새 201` 정확히 · subscribe 1(키 65B · granted · 누름 없이) · requestPermission 0 · 끄면 새 endpoint DELETE ⑥ subscribe 거절 · 새 POST 400 · 503 · 500 · 네트워크 = 새 구독 unsubscribe · off · `아침 알림 연결이 끊겼습니다` + 다시 켜기(같은 틱 subscribe) · 표시 그대로 + 다시 켜기 성공/실패 ⑦ 구독 없음 + 표시 = subscribe → POST · 표시 없음 = 0 ⑧ denied(+ 기기 구독 변형) = blocked · 차단 토스트 · `/widget#push` · 표시 지움 → 다시 열기 0 ⑨ default = off · 꺼짐 토스트 → 단추 = subscribe(누름) ⑩ 다시 등록 실패 4가지 = 토스트 0 · on · 60분 뒤 다시 ⑪ 바로 · 59분 · 60분 hidden = 0 · 60분 visible = 1 · 확인 중 · 종 처리 중 건너뜀 · 두 번째 201 바꾸기 0 · 바꾸기 실패 토스트 뒤 권한 꺼짐 = 두 번째 토스트 0 ⑫ 준비 전 확인 0 · 확인 중 누름 무시 → 끝난 뒤 O5 · 코드 위치 · 문구 3 · `requestPermission` · `setTimeout` · 새 register 0.
  - `summary-text.test.ts` 4: null → `<ul class="summary pending"><li>요약 없음 · 원문을 확인해 주세요</li></ul>` 정확히 · 3줄(`<b>&"'`) 이스케이프 결과 = HEAD 함수와 같음(HEAD 사본으로 따로 대조 — `summary-head-compare.txt`) · index.html 옛 문구 0 · 새 문구 1 · `summaryHTML(` 3곳 = itemHTML · 툴팁 · openDrawer · openapi 설명 = 새 문구.
- 새 테스트가 실수를 잡는지(제작 자체 확인, `mutate.mjs`): 일부러 틀린 **15가지 모두 테스트가 실패**(POST 새 행 204 · 메모리 updated · 한 페이지 1번 뺌 · 저장소 막힘 판정 뺌 · 59분 · 끄기 표시 안 지움 · 옛 DELETE 뺌 · 토스트 한도 뺌 · 권한 꺼짐 표시 안 지움 · 없음 + 표시 뺌 · 실패 때 새 구독 unsubscribe 뺌 · SW 다른 endpoint DELETE 뺌 · SW subscribe 뺌 · 캐시 v2 · 옛 문구) → 파일 되돌림 · sha256 같음.
- `bash scripts/ex12-ubuntu.sh npm run build`(= `next build && tsc --noEmit`, 바깥 연결 차단기 아래): **성공**(Compiled · TypeScript · 라우트 3개 그대로, 11:29 — 모든 파일 수정보다 뒤). 첫 빌드는 내 새 테스트의 타입 오류(가짜 응답 함수 꼴)로 실패 → 테스트 파일 타입만 고쳐 성공. 빌드의 바깥 연결 시도 0.
- `npx --offline @redocly/cli@2.64.1 lint`(지금 판 · HEAD 사본, 차단기 + `REDOCLY_TELEMETRY=off`): **오류 0 · 경고 20 = HEAD 20**(줄 번호만 다르고 위치 · 규칙 목록 같음) · bundle JSON: `0.7.0` · post 응답 = 201 · 204 · 400 · 403 · 413 · 503 · delete 그대로 · 201 = no-store + `PushSubscriptionCreated` · Issue.summary 설명 = 새 문구. redocly 바깥 연결 시도 0.

## 5. 검증 줄(PLAN 1~14)

| 줄 | 판정 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | **OK** | 1절 |
| 2 구독 API · 저장소(단위) | **OK** | 4절 `push-heal.test.ts` ①~⑦ |
| 3 서비스 워커(VM) | **OK** | 4절 SW ①~⑧ |
| 4 화면 자동 다시 연결(VM) | **OK** | 4절 `push-heal-ui.test.ts` ①~⑫ |
| 5 요약 문구(VM · 정적) | **OK** | 4절 `summary-text.test.ts` |
| 6 회귀 · 빌드 · 계약 | **OK** | 4절(319 · 빌드 · tsc · OpenAPI) |
| 7 로컬 서버 · 실제 DB | **OK** | 6절 ① `api-result.json` 12/12 |
| 8 로컬 브라우저 · 실제 서버 | **OK** | 6절 ② `ui-real-result.json` 13/13 + 토스트 보강 `ui-real2-result.json` 6/6 |
| 9 로컬 브라우저 · 가짜 서버 3100 | **OK** | 6절 ③ `ui-mock-result.json` 24/24 + O5 회귀 보강 `ui-mock-extra-result.json` 4/4 |
| 10 요약 문구 화면 | **OK** | 6절 ④ (a) 4/4 · (b) 3/3 |
| 11 안전 · 한도(로컬) | **OK** | 7 · 8절 |
| 12 배포 정적 · API | **[SKIP — 푸시 후 · 코디네이터]** | 절차 9절 |
| 13 배포 브라우저 | **[SKIP — 푸시 후 · 코디네이터]** | 절차 9절 |
| 14 휴대폰 실제 | **[SKIP — 사용자 확인 대기]** | 문장 10절 |

합계 **OK11 / FAIL0 / SKIP3**(12 · 13 푸시 후, 14 사용자 — 판정은 독립 심사).

## 6. 화면 · 서버에서 본 결과

브라우저 = Termux Chromium 155 headless + CDP, 측정마다 새 프로필(끝나면 삭제) · NetLog · `--host-resolver-rules="MAP localhost 127.0.0.1, MAP * ~NOTFOUND, EXCLUDE 127.0.0.1"` · 문서 전 가짜 pushManager(`fakepush.mjs` — `PushManager.prototype` getSubscription · subscribe · 구독 unsubscribe를 바꿈, 원래 함수는 부르지 않음 = **진짜 subscribe 0**, subscribe마다 `userVisibleOnly` · 키 · 그때 `Notification.permission` · 사용자 활성화 기록, `requestPermission` 셈) · 권한 = `Browser.grantPermissions` · `Browser.setPermission` · 관찰용 `Runtime.evaluate`는 사용자 활성화 없이(누름은 실제 CDP 입력 이벤트만).

### ① 줄 7 — 로컬 서버(next start 3001 + 3000 세는 프록시) · 실제 DB (11:01:23~11:01:30 KST, 12/12)
- `/sw.js` 200 · 머리 3개 = O5 기록(`application/javascript; charset=utf-8` · `no-cache, no-store, must-revalidate` · `default-src 'self'; script-src 'self'`) · 본문 = 파일(v3 · pushsubscriptionchange) / `/` = index.html 바이트 · 머리 = O5 html.
- E1(`…/o7-build-{uuid}-1`): POST **201 `{"created":true}`** · `application/json` · no-store · Set-Cookie 0 · DB 행 1 → 같은 키 **204** · 본문 0 → 다른 키 **204** · 행 1 · 키 바뀜 · id · created_at 그대로 → DELETE 204 · 행 0 → POST **201** · 새 id → DELETE 204 · 행 0.
- E2(`-2`): POST 2개 동시(`Promise.all`) → 상태 **{204, 201}** · DB 행 1(줄 8에서 씀).
- Origin 없음 **403** · `https://evil.example` **403** · 4,097B **413** · 형식 밖 endpoint **400**(모두 같은 문구 · no-store) → E3 행 0 · push 전체 = 시작 + E2 1 · 시험 행 밖 스냅숏 = 시작.

### ② 줄 8 — 로컬 브라우저 · 실제 서버(360×800, 알림 허용, 13/13, 11:08:56~11:09:22)
- ① 첫 방문: 설치 오류 `[]` · SW activated · controller · 캐시 `issuerank-v3: [/offline.html]` · 종 off · 구독 API 0.
- ② 가짜 구독 = E2 · 표시 `on`으로 다시 열기: 요청 = **POST E2 → 204** 하나 · 종 on · subscribe · unsubscribe 0.
- ③ Node DELETE E2(= 410 뒤 서버가 지운 꼴) → 새로 고침: 요청 = **POST E2 201 → DELETE E2 204 → POST E3 201**(정확히, Origin 3개 모두 `http://localhost:3000`) · 가짜 unsubscribe E2 1 · subscribe 1(`userVisibleOnly` · 키 65B = 코드 키 · 그때 `granted` · 사용자 활성화 없음) · `requestPermission` 0 · 종 on · DB = E3 1 · E2 0 · E1 0.
- ④ **SW 문맥(v3)에서 처리기와 같은 꼴 fetch: POST E3 → 204 · DELETE E2 → 204(403 아님)** · 프록시가 받은 머리 `Origin: http://localhost:3000` · `Sec-Fetch-Site: same-origin` · `Sec-Fetch-Mode: cors` · `Content-Type: application/json`(2개) → 서버 Origin 검사 그대로 통과(멈출 일 없음).
- ⑤ v2 → v3: 프록시 표지(sw-v2)로 `/sw.js`에 HEAD 판(v2) → update → v2 activated · 캐시 `issuerank-v2: [/offline.html]` → 표지 끄고 update → v3 activated · 캐시 키 = `issuerank-v3`만(v2 지움) · registrationId · scope 그대로(실제 구독은 등록에 붙어 남음) · 가짜 구독 그대로. 요청 = `GET /sw.js (v2)` · `GET /offline.html 304` · `GET /sw.js` · `GET /offline.html 304`.
- ⑥ `ServiceWorker.deliverPushMessage`(아침 JSON) → `getNotifications()` = 1(`오늘의 뉴스가 도착했어요` · tag `issuerank-daily` · `/p` · 아이콘 · 배지 · ko).
- 토스트 확인 보강(`ui-real2.mjs`, 11:28, 6/6): 첫 실행의 토스트 기록기는 DOMContentLoaded에 붙어 그 전에 뜬 토스트를 놓칠 수 있음을 뒤(줄 9)에서 알게 됨 → 기록기를 `#toast` 파싱 즉시 붙이고 + 경합 없는 확인(새 페이지에서 `toast()`가 한 번도 안 불리면 `#toast`가 빈 채)으로 **같은 ①②③을 실제 서버에서 다시**: 첫 방문 · ② POST E2 204 · ③ `POST E2 201 → DELETE E2 204 → POST E3 201` 모두 그대로 + **토스트 0(기록 0 · `#toast` 빈 채)** · DB E3 1 → 정리 0. (새 endpoint 0 — 같은 E2 · E3)
- JS 예외 0 · console 오류 = jsdelivr 글꼴 CSS 이름 풀이 실패뿐.

### ③ 줄 9 — 가짜 서버 127.0.0.1:3100(실제 `createApp(memoryStore)` + public 파일 · `BETTER_AUTH_URL=http://localhost:3100`, DB · 3000 · 바깥 0) — 24/24 + 보강 4/4
- 시작: 360 · 허용 · 구독 없음 · 표시 없음 → 종 off · 요청 0 · 토스트 0.
- ① **죽은 구독(기기 구독 A · 서버에 A 없음 — 10-21 꼴)**: 표시 있음 = `POST A 201 → DELETE A 204 → POST B 201` · unsubscribe A 1 · subscribe 1(키 65B · granted · 활성화 없음) · requestPermission 0 · 종 on · 토스트 0 · 표시 on / **표시 없음(O5 때 켠 기기 = 사용자의 실제 꼴)** = 같은 순서(C → D) · 표시를 새로 씀.
- ② 기기 구독 없음 + 표시 → subscribe 1 → `POST F 201` → on · 토스트 0.
- ③ **denied** + 표시 → blocked · 토스트 `알림이 차단되어 있습니다` + `방법 보기` 1 · 표시 지움 → 다시 열기 토스트 0 · 서버 요청 · subscribe 0 / `방법 보기` 누름 → `/widget#push` · **default** + 표시 → off · `아침 알림이 꺼져 있습니다` + `다시 켜기` 1 · 표시 지움 → 다시 열기 0.
- ④ **저장소 막힘**(문서 전 `localStorage` 접근 = `SecurityError`) × granted + 구독 → O5 그대로: 열 때 getSubscription 1 · 서버 요청 0 · 종 on · 토스트 0 · 61분 뒤로 돌린 `visibilitychange` 3번에도 요청 0(등록 0).
- ⑤ **여러 번 열기**(서버 행 H 있음): 새로 고침 3번 = 열기마다 `POST H 204` 1번 · 곧바로 `visibilitychange` 3번 = 0 · **59분 = 0 · 61분 hidden = 0 · 61분 visible = `POST H 204` 1 · 그 직후 다시 = 0**(60분 상한) · 바꾸기 0 · 새로 고침마다 `#toast` 빈 채. (`Date.now`를 페이지에서 앞당김 · `visibilityState`는 페이지에서 hidden으로 바꿔 사건을 보냄)
- ⑥ **Safari 꼴 거절**(가짜 subscribe = 사용자 활성화 없으면 NotAllowedError): `POST J 201 → DELETE J 204` → subscribe 거절 · 새 POST 0 · 종 off · `아침 알림 연결이 끊겼습니다` + `다시 켜기` · 표시 on 그대로 → 단추 실제 누름 → subscribe(활성화 있음) → `POST K 201` → on · `아침 알림을 켰습니다`.
- ⑦ 토스트 3개 × 320 · 360 × 밝게 · 어둡게: 글 · 단추 **한 줄** · 화면 안(320: 왼쪽 26.1 ~ 오른쪽 293.9, 폭 251~268px — 계획 추산 270~290보다 조금 좁음, 이 기기 대체 글꼴) · 문서 가로 넘침 0 · 종 **34×32** · 대비 밝게 **18.1** · 어둡게 **15.72**(≥ 4.5).
- ⑧ 회귀: 종 켜기(누름) `POST M 201` · `아침 알림을 켰습니다` · 표시 on → 끄기 unsubscribe M · `DELETE M 204` · `아침 알림을 껐습니다` + 다시 켜기 · 표시 지움 → 다시 켜기 `POST N 201` · 서버만 끊음 → 오프라인 안내 → 다시 평소 화면(O4) · 맨 아래 `앱 설치`(/widget#app) · `/?issue={오늘 soc-6}` 상세창 · 쿼리 지움(O1) · 목록 카드 `공유`(1280) = `링크를 복사했습니다` · 로그인 창 · `Google로 계속하기`(O2) · 폴더 삭제 창 열기 · 문구 · 취소(O6, 가짜 세션) · **이슈맵 타일 좌표 1280 · 360(30개) = HEAD index.html과 같음** · JS 예외 0. 보강(`ui-mock-extra.mjs`): **O5 시험 알림 표시**(아침 + `[시험]` deliverPushMessage → 알림 2 · 같은 아침 다시 = 그대로 2) · **C2**(1100 · 1199 = 설치 · 북마크 글자 숨김 / 1200 · 1280 = 보임 · 종 34×32 · 넘침 0) · **종 unsupported**(PushManager 없음 → `unsupported` · 라벨 `아침 알림 — 켜는 방법 안내` · 흐림 0.55 · 누르면 `/widget#push`). 종 4상태(off · on · blocked · unsupported) 모두 봄.

### ④ 줄 10 — 요약 문구 화면
- (a) 가짜 서버(오늘 soc-6 = 샘플 요약 null): 목록 펼침 = 요약 없음 카드 1(soc-6) · 새 문구 · 기울임 · 요약 있는 카드 29 = 3줄 · 옛 문구 0 · 넘침 0 / 이슈맵 툴팁(1280 마우스) 12.5px · 상세창 15px · 북마크 카드(가짜 세션으로 저장 201) — 320 · 360 · 1280 × 밝게 · 어둡게.
- (b) 실제 로컬 서버(읽기만): 어제(10-29) 랭킹에 summary null **2/30** → `/?issue=2026-10-29-pol-5#list` → 상세창 열림 · 요약 = 새 문구 1 · 목록 펼침 요약 없음 카드 수 = API null 수(2) · 요약 있는 카드 28 = 3줄 · 옛 문구 0 · 넘침 0(6가지). 색 밝게 `rgb(111, 107, 101)` · 어둡게 `rgb(155, 151, 143)` = `--muted` 토큰 · 기울임(CSS = HEAD).
- **줄 수**(이 기기는 Pretendard가 막혀 대체 글꼴): 상세창 320 · 360 · 1280 = **1줄** · 툴팁 1280 = **1줄** · 목록 · 북마크 카드 360 · 1280 = **1줄**, 320 = **2줄**(계획 기대 "320 카드 ≤ 2줄"과 같음). 그림: `ui-real-10b-list-360-light.png` · `ui-real-10b-drawer-360-dark.png` · `ui-mock-10a-tip-1280-dark.png` 등 18장.

## 7. 실제 DB

- 시작 스냅숏 10:59:11(읽기 전용): 9표 + push_subscriptions(**0행**) + 마이그레이션 기록 8의 건수 · 내용 md5(`db-snapshot-start.json`).
- 쓰기 = `push_subscriptions` 시험 행만, endpoint `https://fcm.googleapis.com/fcm/send/o7-build-{uuid}-{1|2|3}`(스크립트가 만든 키) **3개 endpoint**, 동시에 남은 것 최대 1행. 쓰기 시각 11:01~11:28 KST(04:00~10:00 밖 — 스크립트가 창 안이면 멈춤). 실제 사용자 데이터 쓰기 0 · `o7-` 꼴이 아닌 push 행 0(시작부터 없음).
- 정리: 11:09(줄 8 뒤) · 11:28(보강 뒤) 각각 그 endpoint만 지움(지운 행 1 · 1) → **남은 `o7-build-` 0**(E1 · E2 · E3 행 0).
- 끝 스냅숏 11:09:48 · 11:28:41 = 시작: 9표 · push_subscriptions · 마이그레이션 **11항목 모두 같음**.
- DB 클라이언트 생성은 try 안 · 오류는 안전 코드만 · 값 · endpoint · 키 출력 0(건수 · md5 앞 10자 · bool만).

## 8. 요청 · 바깥 · 비밀값 · 프로세스

- 로컬 3000(세는 프록시, 시작 0): **85/100**(Node 18 · 그 밖 67 = 브라우저 66 + 서버 확인 curl 1 · 막힘 0) = 서버 확인 curl 1 · 줄 7 14 · 줄 8 + 10 (b) 42 · 줄 8 토스트 보강 28. 가짜 서버 3100은 따로: 시험 운전(같은 스크립트를 실제 실행 전에 3100으로, 첫 서버 604요청 — 시험 운전 4번 · 디버그 3번 포함) · 마지막 줄 9 실행 184 · 보강 23.
- 바깥 **0**: next 서버(차단기 `monitor.mjs`, DB만 허용) = DB 소켓 · DB 이름 풀이 · local뿐, 막힌 시도 0. 브라우저 NetLog = TCP 연결 127.0.0.1(3000 · 3100)뿐 · 이름 = `~notfound`(localhost만 127.0.0.1) · UDP connect(IPv6/IPv4 도달 확인, 보낸 바이트 0) · jsdelivr 글꼴 CSS = 이름 풀이 실패(`ERR_NAME_NOT_RESOLVED`). **진짜 subscribe · 실제 푸시 서비스 · newsdata · OpenAI · Google · Kakao · 배포 주소 · GitHub API 0**. 테스트 첫 실행에서 `npx`가 npm 레지스트리 업데이트 확인을 2번 시도 → 차단기가 막음(연결 0, 그 뒤 `npm test` + 업데이트 알림 끔). 빌드 · redocly 바깥 시도 0.
- 비밀값: 모든 명령 출력 = 안전 실행(`ex12/.env` 이름에 URL · KEY · SECRET · TOKEN · PASSWORD · GOOGLE_ · VAPID가 든 값 전체 + DB 비밀번호 · 사용자 · 호스트 + VAPID 개인 키 다른 꼴 11개를 대조, 누출 false일 때만 출력) · 증거 파일 105개(PNG 18장 말고) + 이 보고 · `HANDOFF.md` · `PLAN.md` 누출 false.
- 프로세스: 띄우기 전 PID 목록 저장(`ps-before-*.txt`) · 내 PID만 종료(표지 파일 `stop-server` · `stop-mock`으로 내 서버가 스스로 끝냄 — proot · server.mjs 9270 · 13430 → next 9278 · 13438, 가짜 서버 17191 · 10413 · 20084, 내 Chromium 묶음은 `Browser.close` 뒤 내 자손만). 넓은 패턴 kill 0. 끝 확인(11:32): curl 3000 · 3001 · 3100 · CDP 9371~9378 = **000** · ps에 `/usr/lib/chromium/` 0 · next-server · next start 0 · 내 스크립트 0 · **code-oss 24593 살아 있음** · 프로필 · NetLog · 임시 파일 삭제(scratchpad에 `o7-` 0).

## 9. 코디네이터 할 일 — 배포 줄 12 · 13(푸시 뒤)

독립 심사 결과 사용자 줄만 남으면(D32 Q3): 커밋 · 푸시 → Vercel 배포 완료 확인 → **04:00~10:00 KST 밖**(시험 행을 만드는 줄):
- **줄 12(Node ≤ 20, Origin = `https://issuerank-kappa.vercel.app`, 안전 실행 · 바깥 = 배포 · DB뿐)**
  1. 시작 스냅숏(9표 + push + 마이그레이션) → `GET /sw.js` = 200 · 머리 3개(위 6절 ①) · 본문 = 저장소 `ex12/public/sw.js` 바이트(v3) → `GET /` = 저장소 `ex12/public/index.html` 바이트
  2. `o7-deploy-{uuid}` endpoint(스크립트가 만든 키) POST → **201 `{"created":true}`** · no-store → 같은 것 다시 **204** → DELETE **204** → Origin 없이 POST **403**
  3. `/api/rankings?date=` 어제부터 거슬러 summary null이 있는 날(≤ 5요청) → 그 id 기록(줄 13에 씀)
  4. `finally`: 그 endpoint만 지움(남은 0) · DB 행 수 0 · 끝 스냅숏 = 시작. 배포 요청 약 9~13/20.
  - 재사용: `ex12/.data/o7-build-20261030/api.mjs`(주소 · Origin · STOP_AT 20 · endpoint 접두 `o7-deploy-`) + `dbcore.mjs`(`TEST_PREFIX` · `TEST_EP`를 `o7-deploy-`로) + `db.mjs snapshot`.
- **줄 13(CDP ≤ 25, NetLog, O5 줄 19 방식 새 프로필 360)**: `/` → SW v3 activated · `issuerank-v3: [/offline.html]` · 설치 오류 `[]` / 가짜 pushManager(`fakepush.mjs`, 기록기 보강판) + **CDP Fetch가 `/api/push/`를 잡아 첫 POST 201 `{"created":true}` · 그 뒤 204로 답함**(서버 · DB에 안 감) · 가짜 구독 + 표시 `on` → 새로 고침 → 요청 순서 = POST → DELETE → POST · 종 on · 토스트 0(`#toast` 빈 채) / `/?issue={줄 12의 id}` → 상세창 `요약 없음 · 원문을 확인해 주세요` · 옛 문구 0 · 배포 호스트 밖 0 · 진짜 subscribe 0. 재사용: `ui-real2.mjs` · `ui-real.mjs`(ORIGIN · Fetch 가로채기만 바꿈).

## 10. 사용자 할 일 — 줄 14(마지막 확인 목록에 넣을 문장, 계획 그대로)

- 휴대폰(설치한 이슈랭크 앱 또는 Chrome)에서 이슈랭크를 연다 → 열자마자 오른쪽 위 종 모양을 본다(칠한 종 = 켜짐 · 빈 종 = 꺼짐 · 사선 = 차단 — 처음 본 모양과 뜬 토스트를 알려 주기) → 꺼짐이면 종을 한 번 눌러 켬(허용) · 차단이면 `방법 보기`대로 허용한 뒤 다시 누름 → 칠한 종 확인 → 다음 날 아침 6:30~10:00에 `오늘의 뉴스가 도착했어요` 알림이 오는지(온 시각). 코디네이터가 그날 push 실행 요약 `구독 ≥ 1 · 보냄 ≥ 1`을 확인. 자동으로 다시 이어지면 `/widget#push`의 기기 코드가 바뀜(시험 알림은 새 코드로). (선택) 요약 없는 카드가 보이면 `요약 없음 · 원문을 확인해 주세요`로 보이는지(캡처).

## 11. 필요했지만 없던 키 · 계정

- 없음(실제 FCM 구독은 규칙상 만들지 않음 — 사용자 줄 14).

## 12. 지운 것

- 기존 코드 삭제 0. 바뀐 기존 줄 = `app.ts` Store 한 줄 · `sample.ts` 반환 3줄 · `store.ts` 저장 함수 본문 · `routes/push.ts` 저장 · 응답 줄 · `openapi.yaml` 3줄(version · 204 설명 · summary 설명) · `sw.js` 맨 위 주석 2줄 + CACHE 줄 · index.html `summaryHTML` 1줄 · 켜기 주석 1줄 · 맨 아래 `navigator.serviceWorker.ready` 줄 앞에 `} else ` · 테스트 기대값 · 제목.

## 13. 추가로 보인 점 · 미해결

1. (낮음 · 기록) **종으로 끌 때 unsubscribe가 실패한 기기**: `pushOff`는 표시를 먼저 지우지만 기기 구독이 남으면, 다음 열기에서 "옛 구독 기기"(권한 허용 + 기기 구독 → 켜 둔 것) 규칙으로 표시를 다시 쓰고 POST 201 → 바꾸기 → 다시 켜짐(사용자가 끈 것이 되살아남). O5에서도 같은 실패면 종이 켜짐으로 보였던 드문 경우 — 계획 규칙대로 두었고 고치지 않음.
2. (기록) 계획 확인 필요 1(누름 없는 실제 subscribe) · 2(10-21 원인) → 사용자 줄 14 · 3(열 때 토스트가 로그인 돌아옴 · `?issue=` 토스트를 덮을 수 있음) · 4(두 탭 동시 바꾸기) · 5(group → summarize 사이 몇 분) · 7(`/privacy` 7항 기기 저장 한 줄 후보) · 8(문구 대안) — 계획대로 기록만, 이번에 재현하지 않음.
3. (검증 도구 · 기록) ① 토스트 기록기를 DOMContentLoaded에 붙이면 SW가 이미 활성인 새로 고침에서 확인이 그 전에 끝나 토스트를 놓침(제품 문제 아님 — 페이지는 표시를 지웠음) → `#toast` 파싱 즉시 붙임 + 경합 없는 확인으로 바꾸고 줄 8 ①②③을 다시 봄(6절 ②) ② `ex12/.data`에 `*.test.ts` 이름으로 둔 HEAD 사본을 vitest가 같이 돌림(include `**/*.test.ts`) → 첫 테스트 실행 무효, 사본은 `.head.txt`로(심사도 주의) ③ CDP `Page.navigate`로 해시만 바꾸면 보기가 바뀌지 않음 → `location.hash` ④ 차단기를 넣은 첫 테스트 실행에서 auth 기동 테스트가 15초 제한 초과 1번(알려진 환경 요인 + 차단기 부담, 그 뒤 일반 실행 통과).
4. (참고) 토스트 실측 폭 251~268px(계획 추산 270~290px) — 320px 화면 안.
