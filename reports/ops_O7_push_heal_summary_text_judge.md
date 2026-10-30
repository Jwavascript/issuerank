# ops O7 아침 알림 자동 다시 연결 · 요약 없음 문구 — 독립 심사

- 날짜: 2026-10-30(KST 11:41~12:40) · 역할: 독립 심사(`agent_prompts/3_judge_agent.md`, 코드 수정 0) · 대상: `ex12/` 작업 트리(HEAD `1b71cf7` 대비 미커밋 변경)
- 기준: `plan/PLAN_ops_O7_push_heal_summary_text.md`(확정, 122줄) · SPEC **AC-14-5** · **AC-04-3** + 계획의 회귀 AC · PLAN.md 5절 D14 · D21 · D29~D32 · 6절 · AGENTS.md. 사용자 결정(10-30, D32): 열 때 자동 다시 연결 · 요약 없는 카드 문구를 사실대로(화면만) · 심사 뒤 푸시까지
- 제작 보고(`reports/ops_O7_push_heal_summary_text_build.md`)는 주장으로만 봄. 판정 근거 = 심사가 새로 만든 검사(`ex12/.data/judge-o7-20261030/`, git 제외). 제작 스크립트(`o7-build-20261030/`)는 열어 보지도 돌리지도 않음(폴더 이름만 봄)

## 1. 판정

**PASS 보류 — OK 11 · FAIL 0 · SKIP 3**(12 · 13 = 푸시 뒤 코디네이터, 14 = 사용자). 위반 0. **커밋 전에 꼭 고칠 것 없음.**
- 발견 1건(낮음): 종으로 끌 때 `unsubscribe()`가 실패한 기기는 다음 열기에 다시 켜짐(제작 보고 13절 1 — 실제 브라우저에서 재현). 드물고 복구 가능 → 다음 계획 후보(8절 1).
- 그 밖은 참고(경합 1 · 불필요한 바꾸기 · 존재 확인 · 심사 도구 문제).

## 2. PLAN 항목별

| 계획 항목 | 판정 | 근거(심사 검사) |
|---|---|---|
| 켜 둔 표시 `issuerank-push` = `on`(켜기 POST 2xx 뒤 씀 · 끄기 unsubscribe 전 지움 · 권한 꺼짐 안내 때 지움 · 쓰기 실패 무시) | done | VM ② · ④(쓰기 예외) · ⑧ · ⑨, 브라우저 o5 · perm |
| 저장소 막힘 = O5 그대로(getSubscription 1 · 요청 0 · visibilitychange 등록 0 · 토스트 0) | done | VM ① 3모드 × 5상태 + 막힘에서 종 끄기 · 켜기 = O5, 브라우저 blocked 3모드(61분 보임 3번에도 0) |
| 옛 구독 기기(granted + 기기 구독 → 표시 씀) | done | VM ④, 브라우저 dead(표시 없음 = O5 기기) |
| 확인 시점(열 때 1 · 다시 보일 때 60분) · 확인 중 · 종 처리 중 건너뜀 · 종 누름 무시 | done | VM ⑪ · ⑫ · 준비 전, 브라우저 multi · busy |
| 순서(있음 204 끝 / 201 바꾸기 / 실패 조용히 · 없음+표시 = 새 구독 · 없음 = off) | done | VM ③ · ⑤ · ⑦ · ⑩, 실제 서버 ② · ③ |
| 바꾸기(옛 unsubscribe → DELETE 옛 → subscribe(granted만) → POST 새) · 실패 토스트 · 한 페이지 1번 | done | VM ⑤ · ⑥ · ⑪, 브라우저 dead · safari · second, 실제 서버 ③ |
| 권한 꺼짐(denied 차단 + 방법 보기 · default 꺼짐 + 다시 켜기, 요청 · subscribe 0) | done | VM ⑧ · ⑨, 브라우저 perm(+ `/widget#push` · 다시 켜기 실제 누름) |
| 토스트(기존 꼴 · 새 CSS 0 · 확인 토스트 한 페이지 1개) | done | 브라우저 toast 12가지 · safari(같은 페이지 두 번째 0) |
| 코드 위치(Push (O5) 구역 안 · 키 재사용 · Events · Init · `start()` · 헤더 · CSS 0) | done | 범위 검사(3절) |
| POST 201 `{ "created": true }` · 기존 204 · 5,000행 503 · 그 밖 O5 그대로 | done | API 20/20(HEAD 판과 차등 45요청), 실제 DB 15/15, 로컬 서버 10/10 |
| 저장소 `'created' \| 'updated' \| null`(UPDATE → INSERT ON CONFLICT DO NOTHING → UPDATE) | done | 메모리 · 실제 DB(경합 강제 순서 포함 — 8절 2) |
| 계약 `openapi.yaml`(201 · 스키마 · 204 설명 · post 설명 · summary 설명 · 0.7.0) | done | redocly 오류 0 · 경고 20 = HEAD 목록 · 번들 비교(그 밖 같음) |
| `pushsubscriptionchange`(① ~ ⑧) · 공개 키 · 맨 위 실행 코드 0 · 캐시 v3 | done | SW VM 18/18, 실제 Chromium에서 sw.js 실제 처리기 호출(Origin) · v2 → v3 |
| 요약 없음 문구(`summaryHTML` 한 줄 → 카드 · 툴팁 · 상세창) | done | VM(HEAD와 같은 결과 · null 정확한 마크업), 브라우저(가짜 · 실제 데이터) |
| 기존 동작 보존(테스트 3개 기대값 · 제목만 · `push-ui.test.ts` 수정 0 통과) | done | 범위 검사 · npm test |
| `/privacy` 그대로 | done(판단 동의) | 6절 |
| 새 패키지 0 · 마이그레이션 0 · 발송 · 워크플로 그대로 | done | 범위 검사 |
| "하지 않음" 목록 | 지킴 | 범위 밖 파일 변경 0 |

missing · wrong 0. 계획에 없던 내부 세부(제작 보고 3절 1 `pushReplace` 앞 granted 재확인)는 계획 규칙 "granted일 때만"을 확인 도중 권한이 바뀐 경우까지 지키는 것 — 심사 VM에 그 경우를 넣어 확인(subscribe 0 · 토스트 · off).

## 3. 범위 · 불변(검증 1 — `jscope.mjs`, 시작 11:50 · 끝 12:38 같음)

- 추적 687파일 작업 트리 blob vs HEAD blob: 바뀐 것 = 계획 목록 10개(`ex12/{openapi.yaml,public/index.html,public/sw.js,server/app.ts,server/db/store.ts,server/routes/push.ts,server/sample.ts,test/push-api.test.ts,test/push-sw.test.ts,test/pwa.test.ts}`) + 새 3개(`test/push-heal.test.ts` · `test/push-heal-ui.test.ts` · `test/summary-text.test.ts`) + 문서(계획 역할 3 · 코디네이터 5 · 제작 2 — 9절에서 따로 셈)뿐. 지운 추적 파일 0 · 목록 밖 0 · ex01~ex11 · `frontend/` · `web/` · `plan/PLAN_web.md` 0.
- 보호 목록(계획 검증 1, 글롭 펼침 78) + 기존 테스트 25개 = **103개 모두 HEAD blob과 같음**. 발송 `server/{push,push-run,webpush,push-config}.ts` · `.github/workflows/{daily,push}.yml` = HEAD. `package.json` · `package-lock.json` = HEAD(새 패키지 0) · `drizzle/` 새 · 바뀐 파일 0(마이그레이션 0).
- index.html: diff 덩어리 7개가 모두 `summaryHTML` 구역 · Push (O5) 구역 안(HEAD · 지금 줄 번호 둘 다) · 두 구역을 뺀 나머지 = HEAD 바이트 · 요약 구역은 없음 문구 1줄만 다름. 기존 VM 테스트(account · google · widget · share · pwa · folder-delete · push-ui)가 자르는 구역 27개 중 25개 글자 같음, 다른 2개 = `push-ui`의 Push 구역 · `widget-ui`의 summaryHTML 구역(둘 다 계획이 예상). Events · Init · `start();\nregisterServiceWorker();` · `<head>` · CSS · body 마크업 = HEAD · `serviceWorker.register(` 1 · 87자 키 1번.
- 기존 테스트 3개 diff 분류: push-api 9줄 = 기대값 8(`204 · null` → `201 · { created: true }`) + 제목 1 / pwa 8줄 = v2 → v3 5 + 제목 1 + 옛 캐시 목록 · 지움 기록에 `issuerank-v2` 2 / push-sw 3줄 = CACHE v3 · 제목 + `not.toContain("'issuerank-v2'")` 1 — 계획 "보존"과 같음. `push-ui.test.ts` = HEAD(수정 0).
- 심사 중 문서 · 코드 sha256 변화 0, HEAD `1b71cf7` 그대로, vitest가 바꾼 추적 파일 0.

## 4. 테스트 · 빌드 · 계약(검증 6)

- `bash scripts/ex12-ubuntu.sh … npm test`(안전 실행 누출 false · 바깥 차단기): **31파일 319개 통과**(새 3파일 31개 — skip · only 0). 바깥 연결 0(유닉스 소켓만).
- 깨끗한 빌드(`.next` · `tsconfig.tsbuildinfo` 지운 뒤 `npm run build` = `next build && tsc --noEmit`): **성공**(라우트 3개 그대로) · 바깥 연결 0.
- `npx --offline @redocly/cli@2.64.1 lint`(지금 판 · `git show HEAD` 판): **오류 0 · 경고 20 = HEAD 20(규칙 · 위치 목록 같음)**. 번들: `0.7.0` · post 응답 201 · 204 · 400 · 403 · 413 · 503 · 201 = no-store + `PushSubscriptionCreated`(`created: const true` 필수 · 다른 칸 금지) · delete 그대로 · post 설명 · version · 201 · 204 설명 · 새 스키마 · summary 설명을 뺀 나머지 = HEAD. (redocly CLI 자체가 registry.npmjs.org로 업데이트 확인 4번 시도 → 차단기가 막음, 연결 0.)

## 5. 검증 줄(PLAN 1~14)

| 줄 | 판정 | 근거(파일) |
|---|---|---|
| 1 범위 · 불변 | **[OK]** | 3절(`scope-start.json` · `scope-end.json`) |
| 2 구독 API · 저장소(단위) | **[OK]** | `japi.mts` 20/20: HEAD 판 앱과 같은 순서 45요청 차등 — 새 endpoint POST 8가지만 HEAD 204 → 지금 201 `{"created":true}`(application/json · no-store · Set-Cookie 0), 나머지 37(Origin 없음 · evil · `null` · 4,097B · 깨진 JSON · 배열 · 형식 밖 endpoint 15 · 키 · DELETE · GET · PUT) 상태 · 본문 · 머리 이름 = HEAD · 끝 행 = HEAD / 같은 endpoint 204 · 본문 0 · id 그대로 · 키 갱신 · DELETE 뒤 201 새 id · 5,000행 기존 204 · 새 503 · 4,999행 201 · 저장 실패 500 = HEAD(로그 `PUSH_SAVE_FAILED`만) · 동시 2개 · 5개 = 201 하나 · 행 1 · 저장소 직접 `'created'` · `'updated'` · `null` · BFF = 앱 |
| 3 서비스 워커(VM) | **[OK]** | `jvm-sw.mjs` 18/18: ① ② subscribe 1(userVisibleOnly · 65B = `VAPID_PUBLIC_KEY`) → POST(본문 = toJSON · JSON 머리 · 상대 주소) · DELETE 0 ③ subscribe 0 · POST new → DELETE `{endpoint: old}`(순서) ④ subscribe → POST → DELETE old ⑤ POST만 ⑥ 거절 + old = DELETE만 · old 없음 = 0 ⑦ 500 · 네트워크 · toJSON 예외 = 거절 없이 끝 · DELETE 그대로 ⑧ 키 1번 = push-config = index.html · 맨 위 실행에서 registration · atob · fetch · caches 접근 0(프록시로 셈 — 넣으면 잡히는 것 확인) · 처리기 6종 · HEAD 1~58줄 = 지금(3~5줄 주석 · CACHE만) |
| 4 화면 자동 다시 연결(VM) | **[OK]** | `jvm-ui.mjs` 61/61(index.html API + Push 구역, 한 기기 여러 열기): 표 ①~⑫ 전부 + 410 꼴 unsubscribe 실패 변형(같은 구독이 돌아와 `POST 옛 → DELETE 옛 → POST 옛`) + 확인 도중 권한 바뀜 + 요청 ≤ 3 · subscribe ≤ 1 · unsubscribe ≤ 2. 심사 VM이 틀린 판을 잡는지 사본(심사 폴더)으로 10가지 바꿔 돌림 → **10/10 잡음**(SW VM 7/7) · 제품 파일 해시 그대로 |
| 5 요약 문구(VM · 정적) | **[OK]** | `jvm-summary.mjs`: null · undefined → `<ul class="summary pending"><li>요약 없음 · 원문을 확인해 주세요</li></ul>` 정확히 · 배열 6가지(`<b>&"'`, `<script>`, 빈 배열 등) = HEAD 함수 결과 · 옛 문구 0 · 새 문구 1 · `summaryHTML(` 3곳(HEAD도 3) · openapi summary 설명 = 새 문구 |
| 6 회귀 · 빌드 · 계약 | **[OK]** | 4절 |
| 7 로컬 서버 · 실제 DB | **[OK]** | ① 앱 + 실제 DB(`jreal.mts` 15/15, 11:58:06~11:58:15): J1 201 → 같은 키 204(id · created_at 그대로) → 다른 키 204(키만 바뀜) → DELETE → 201 새 id · allowNew 거짓 기존 `'updated'` · 새 `null`(쓰기 0) · 5,000행 꼴 새 503 · 기존 204 · J2 동시 2 · 5(연결 5개) → 201 하나 · 행 1 · 강제 순서 경합 · 3중 경합(8절 2) ② next start + 세는 프록시(`jlocal.mjs` 10/10, 12:27:12~12:27:16, 14요청): `/sw.js` 200 · 머리 3개 = O5 · **Referrer-Policy 없음** · 본문 = 파일(v3) · `/` = index.html 바이트 · J1 순서 = 위와 같음(BFF 201 머리 = content-type `application/json` · no-store · Set-Cookie 0) · J2 동시 2 = {201, 204} · 행 1 · Origin 없음 403 · evil 403 · 4,097B 413 · 형식 밖 400(같은 문구 · no-store · J3 쓰기 0) |
| 8 로컬 브라우저 · 실제 서버 | **[OK]** | `jui-real.mjs real`(12:27:25, ①~④ 6/6) + `real b`(12:29:02, ⑤ ⑥ 7/7) — 360×800 · 알림 허용 · 문서 전 가짜 pushManager: ① 설치 오류 [] · activated · 제어 · `issuerank-v3: [/offline.html]` · 종 off · 구독 API 0 ② J2 + 표시 → `POST J2 204` 하나 · on · 토스트 0 ③ DB에서 J2 지움 → 새로 고침 → `POST J2 201 → DELETE J2 204 → POST J3 201` · unsubscribe J2 1 · subscribe 1(granted · 활성화 없음 · 65B = VAPID) · requestPermission 0 · on · 토스트 0 · DB J3 1 · J2 0 ④ **SW 대상에 붙어 `getEventListeners(self)`로 sw.js의 실제 `pushsubscriptionchange` 처리기를 부름**(SW 쪽 `PushManager.prototype.subscribe`는 가짜 — 진짜 0): old J3 · new 없음 → `POST J2 201 · DELETE J3 204`, old J2 · new J3 → `POST J3 201 · DELETE J2 204` — 프록시가 받은 머리 `Origin: http://localhost:3000` · `Sec-Fetch-Site: same-origin` · 403 0 ⑤ 프록시 표지로 `/sw.js` = HEAD v2 → v2 활성 `[issuerank-v2]` → 표지 끔 → v3 활성 `[issuerank-v3: /offline.html]` · scope · registrationId 그대로 ⑥ `deliverPushMessage` → 알림 1(`오늘의 뉴스가 도착했어요` · `issuerank-daily` · `/p`). JS 예외 0. (첫 실행 ⑤는 심사 프록시 결함 — 8절 5 — 고친 뒤 다시) |
| 9 로컬 브라우저 · 가짜 서버 3100 | **[OK]** | `jui-mock.mjs` 72/72(12:20~12:23, 한 브라우저 이어서): 첫 방문 · 죽은 구독(표시 있음 · 없음) · 구독 없음 ± 표시 · denied · default(± 다시 열기 · `방법 보기` → `/widget#push` · `다시 켜기` 실제 누름) · 저장소 막힘 3모드 · 여러 번 열기 · 59 · 61분 · hidden · Safari 꼴 거절 → 다시 켜기 · 같은 페이지 두 번째 201 · 한 페이지 토스트 1개 · 확인 중 누름 무시 · 13절 1 재현(+ HEAD 대조) · 토스트 3 × 320 · 360 × 밝게 · 어둡게 · 회귀 · 배포 직후 O5 화면 · 사생활 |
| 10 요약 문구 화면 | **[OK]** | (a) 가짜 서버(샘플 `soc-6`): 목록 · 상세창 · 툴팁(1280 마우스) · 북마크 카드 × 320 · 360 · 1280 × 밝게 · 어둡게 — 새 문구 수 = null 수 · 옛 0 · 요약 있는 카드 29 = 3줄 · 기울임 · 색 = HEAD(`rgb(111,107,101)` · 어둡게 `rgb(155,151,143)`) · 넘침 0 (b) 실제 로컬 서버(읽기만): 어제 10-29 summary null **2/30**(`pol-5` · `eco-3`) → `/?issue=2026-10-29-pol-5#list` → 상세창 새 문구 · 목록 요약 없음 카드 2 = API null 2 · 요약 있는 카드 28 = 3줄 · 옛 0 · 넘침 0. 줄 수(이 기기 대체 글꼴): 카드 · 북마크 360 · 1280 = 1줄 · 320 = 2줄 · 상세창 · 툴팁 = 1줄 |
| 11 안전 · 한도(로컬) | **[OK]** | 3000 **61/100**(Node 15 · 브라우저 46) · 3100 따로 1,676(시험 운전 포함) · 바깥 0(next = DB 소켓만 · 브라우저 NetLog TCP = 127.0.0.1뿐 · 이름 = `~notfound`) · 진짜 subscribe · 푸시 서비스 · newsdata · OpenAI · Google · Kakao · 배포 주소 · GitHub API 0 · 누출 false · 끝 스냅숏 = 시작 · `o7-judge-` 0 · 소유 PID 0 · code-oss 24593 살아 있음 |
| 12 배포 정적 · API | **[SKIP — 푸시 후 · 코디네이터]** | 9절 절차(스크립트 준비 · 가짜 서버로 시험 운전 8/8 · 7요청) |
| 13 배포 브라우저 | **[SKIP — 푸시 후 · 코디네이터]** | 9절 절차(시험 운전 5/5 · 18요청) |
| 14 휴대폰 실제 | **[SKIP — 사용자 확인]** | 계획 "사용자 확인" 문장 그대로(제작 보고 10절) |

**합계 OK 11 · FAIL 0 · SKIP 3 → PASS 보류**(PASS 아님).

## 6. AC

- **AC-14-5**: 충족(로컬). 켠 표시 · 옛 기기 · 서버 행 없음(410 꼴) · 기기 구독 없음 → 열 때 · 다시 볼 때 권한 창 없이 다시 등록 · on / 못 하면 `다시 켜기` / denied · default → 차단 · 꺼짐 + 한 번 알림 / 저장소 막힘 = 그대로 / 종으로 끈 기기 = 그대로(정상 unsubscribe). **예외 1(낮음)**: 끌 때 unsubscribe가 실패한 기기는 다음 열기에 다시 켜짐(8절 1). 기기 실제 동작은 14.
- **AC-04-3**: 충족(로컬 — 카드 · 상세창 · 툴팁, 실제 데이터 포함). 배포는 13.
- 회귀: AC-14-1 · 14-4(종 켜기 · 끄기 · 다시 켜기 · 차단 누름 · 지원 안 함 · 끄면 새 endpoint DELETE) · 14-2 · 14-3(발송 코드 = HEAD · 알림 표시 · 같은 tag 대체 · 시험 알림) · 13-1(맨 아래 앱 설치) · 13-2(오프라인 안내 → 평소) · 13-3(공유 = 링크 복사) · 04-1 · 08-1 · 05-2(요약 있는 카드 3줄 · 북마크 카드 · 360/320 넘침 0) · 11-2(`?issue=` 상세창 · 쿼리 지움) · 10-5(폴더 삭제 창 열기 · 문구 · 취소) · D20 이슈맵 타일 좌표 1280 · 360(30개) = HEAD index.html · C2(1100 · 1199 글자 숨김 / 1200 · 1280 보임) — 모두 그대로. AC-04-2 · 06-3은 참고(구역 글자 = HEAD · Google 누르지 않음).
- 범위 밖("알림/구독 중 이메일 · 키워드 · 하루 2통 이상"): 발송 · 워크플로 = HEAD.
- 사생활: 화면 · SW가 보낸 push 요청 본문 = `endpoint,expirationTime,keys` 또는 `endpoint`뿐 · 표시 글자(`issuerank-push`) · 쿠키 0(프록시 · 가짜 서버 기록). `/privacy` "그대로" 판단에 동의 — 서버가 받는 값 = 1항 그대로, 표시는 기기 안 `on` 한 칸(서버 전송 0 · 쿠키 아님 → 7항 "쿠키는 …만" 그대로 사실), 3 · 6항(끄면 바로 · 권한 · 데이터 지우면 다음 발송) 그대로 맞음. 7항 기기 저장 한 줄은 계획 확인 필요 7대로 다음 문안 후보.

## 7. 위반

없음 — 기사 본문 저장 · 표시 0 · 코드의 키 · 접속 정보 0(VAPID 공개 키뿐 — 비밀 아님, 바뀐 · 새 파일 23개 누출 검사 false) · `.env` git 제외 · 계획 밖 패키지 0 · D0 밖 수집 0 · 커밋 · 푸시 · stash · reset 0 · `web/` 0.

## 8. 발견한 점(심각도 순)

1. **(낮음) 종으로 끌 때 `unsubscribe()`가 실패한 기기는 다음 열기에 다시 켜짐**(제작 보고 13절 1). `pushOff`는 표시를 먼저 지우지만 기기 구독이 남으면, 다음 열기에서 "옛 구독 기기"(granted + 기기 구독 → 켜 둔 것) 규칙이 표시를 다시 쓰고 `POST 201 → 바꾸기 → 새 구독 201`로 켬. 재현: VM + 실제 Chromium(가짜 unsubscribe 한 번 거절): 끔(`POST r 204 · DELETE r 204` · off · 표시 지움) → 다음 열기 `POST r 201 → DELETE r 204 → POST s 201` · on · 표시 on. HEAD(O5) 화면은 같은 실패 뒤 요청 0 · 종 on(서버 행 없음 → 알림 0)이었음.
   - AC-14-5 "종으로 끈 기기 … 지금처럼"과 이 실패 경우에만 어긋남(정상 unsubscribe면 그대로 — 확인함).
   - 일어날 가능성: unsubscribe 약속이 거절될 때만 — 드묾. Chromium은 네트워크 오류도 '보류 성공'으로 돌려주고 서비스 워커 없음 · 저장소 오류 등에서만 거절하는 것으로 앎(이번에 기기 · 소스로 확인 안 함 — 바깥 요청 0). 영향: 끈 사람에게 하루 1통이 다시 옴 · 종이 켜짐으로 보여 다시 끌 수 있음(복구 가능) · 데이터 · 보안 영향 0.
   - 커밋 전 필수 아님. 고친다면(계획 역할 · 사용자 결정, 다음 계획 후보): 끌 때 표시를 지우는 대신 `off`로 남기고 "옛 구독 기기" 규칙은 표시가 아예 없을 때만 적용(1~2줄).
2. **(참고) DB 저장의 3중 경합 → 204인데 행 0**(제작 보고 3절 4). 드리즐 쿼리를 멈추는 감싸개로 순서를 강제해 실제 DB에서 재현: A UPDATE 0행 → B `'created'` → A INSERT 충돌(0행) → C DELETE → A 두 번째 UPDATE 0행 → `'updated'`(204) · 행 0. 같은 endpoint DELETE가 두 쿼리 사이 몇 ms 안에 와야 함(같은 기기 두 탭이 동시에 열리며 한쪽이 바꾸기 중일 때 정도) — 그때도 바꾸는 탭이 새 endpoint를 등록하므로 최종 상태는 맞음, 아니면 다음 열기 POST 201로 스스로 고쳐짐. 계획이 정한 동작 그대로 · 고칠 것 없음. 두 요청 경합(가장 흔한 꼴)은 메모리 · 실제 DB(연결 5개, 2 · 5개 동시) · BFF(연결 1개, 2개 동시) 모두 201 하나 · 행 1, 강제 순서(A가 INSERT 앞에서 멈춘 사이 B 생성)도 A `'updated'` · 행 1.
3. **(참고) 201 = "서버에 그 행이 없음"이라 살아 있는 구독도 바꾸는 경우가 있음**: SW 처리기의 POST는 실패하고 옛 DELETE만 된 경우 · 다른 탭이 그 endpoint를 지운 경우 등 → 다음 열기에 멀쩡한 구독을 한 번 바꿈(unsubscribe + subscribe 1번, `/widget#push` 기기 코드가 바뀜). 해 없음 · 기록만.
4. **(참고) 201/204가 endpoint 존재 여부를 알려 줌**: endpoint 주소를 이미 가진 사람만 쓸 수 있고(추측 불가 주소), O5에서도 그 주소로 키 덮어쓰기 · DELETE가 됐으므로 새 위험 아님.
5. **(참고 · 심사 도구, 제품 문제 아님)** ① 실제 서버 첫 실행의 ⑤(v2 바꿔 주기)가 실패 — 심사 프록시가 next의 `content-encoding: gzip` 머리를 둔 채 본문만 v2로 바꿈 → 압축 머리 빼고 다시 실행해 OK(요청 41 → 61에 포함). ② `Browser.grantPermissions`는 목록 밖 권한을 거절으로 바꿈(알림까지 꺼짐) → 알림을 같이 넣음. ③ redocly CLI가 업데이트 확인을 시도(차단됨). ④ `tsconfig.json` include `**/*.ts`라 `ex12/.data` 안 `.ts` 파일도 tsc 대상 — 심사의 HEAD 서버 사본(`.ts`)은 차등 검사 뒤 지움(지금 `ex12/.data`에 남은 옛 단계 `.ts` 9개도 tsc 대상 — 빌드는 통과 중이지만 다음 단계에서 깨질 수 있음, 기록).
6. **(참고)** 계획 "확인 필요" 1(누름 없는 실제 subscribe — 사용자 줄 14) · 2(10-21 원인) · 3(열 때 토스트 겹침) · 4(두 탭) · 5(group → summarize 사이) · 7(`/privacy` 7항 후보) · 8(문구 대안)은 계획대로 기록만.

## 9. 문서 평가

- **코디네이터 문서(사용자 승인 아님, 10-30)**
  - `AGENTS.md` 37줄: D32와 코드(`요약 없음 · 원문을 확인해 주세요` · 그 전 `요약을 준비 중입니다`)와 맞음 · 최소 수정 · 계획 확인 필요 6을 푼 것 — 적절. 다만 공통 규칙 파일이므로 커밋 메시지 · HANDOFF에 바뀐 줄을 적어 사용자가 보게 하면 좋음.
  - `PLAN.md` 11절 시연 점검표 1줄: D32 새 문구 + 옛 문구 · 날짜 — 정확.
  - `reports/ops_O2~O5_*_judge.md` "사용자 확인 (2026-10-30)" 절: 끝에 덧붙임(앞 기록 소급 수정 0). O5 합계 `OK 22 · FAIL 1(22) · SKIP 1(23)` = 24줄(10-20 커밋 `f13f749`의 줄 21 OK 뒤 24 OK · 22 FAIL)로 맞음 · 22 FAIL 진단(10-21 410 → 행 삭제 → 10-22~30 구독 0)이 이 계획의 이유와 같음. O4 10-20 "테스트 했어(문제 보고 없음) → OK(사용자)"는 '통과'라는 말 없이 추론 — "사용자 시험 · 문제 보고 없음"으로 적거나 확인하면 더 정확. O2의 Google 게시 상태 참고 문장은 바깥 문서 근거라 이번에 확인 안 함(바깥 요청 0). O3 · O4(인앱 브라우저 쿠키 따로) 기록은 타당. O7 판정에 영향 0.
  - HANDOFF의 O2~O5 줄 · 머리("최종 갱신 2026-10-20")는 10-30 기록(O5 22 FAIL · 24 OK 등)과 10-20 줄 21 OK를 아직 반영하지 않음 — 커밋 때 코디네이터가 맞추면 좋음(문서, 낮음).
- **계획 역할 문서**: SPEC AC-14-5 · AC-04-3(카드 · 상세창 · 툴팁 · 옛 문구 날짜) · PLAN.md D32 · 계획 파일이 코드와 맞음.
- **제작 문서**: 제작 보고의 주장(바뀐 줄 수 · 103 보호 · 319 테스트 · OpenAPI 20 = HEAD · 410 꼴 순서 · SW Origin · v2 → v3 · 토스트 폭 251~268px · 대비 18.1 / 15.72 · 10-29 null 2/30)은 심사 검사로 모두 다시 나옴(SW Origin은 심사가 실제 처리기로 더 강하게 확인). HANDOFF O7 줄 · PLAN.md 10절 메모는 사실과 맞음(아래 HANDOFF O7 줄은 심사 결과로 바꿈).

## 10. 푸시 후 절차 — 배포 줄 12 · 13(코디네이터)

조건: 커밋 · 푸시 → Vercel 배포 Ready → **04:00~10:00 KST 밖**(스크립트가 창 안이면 멈춤) → 작업 트리 = 푸시한 커밋. 스크립트는 `ex12/.data/judge-o7-20261030/`(git 제외)에 있고 가짜 서버로 시험 운전함.

1. **줄 12(Node ≤ 20 — 스크립트가 세어 20에서 멈춤, 시험 행 `o7-deploy-{uuid}-1` 하나 · 최대 3)** — Termux, 그 폴더에서:
   ```bash
   cd ex12/.data/judge-o7-20261030
   JUDGE_TEST_PREFIX=o7-deploy- NODE_OPTIONS=--import=./jguard.mjs JUDGE_GUARD_FILE=$PWD/guard-deploy12.jsonl \
     JUDGE_ALLOW=issuerank-kappa.vercel.app JUDGE_ALLOW_DB=1 node jsafe.mjs deploy12 -- node jdeploy12.mjs
   ```
   기대 8/8: 시작 시험 행 0 → `/sw.js` 200 · 머리 3개 = O5 · Referrer-Policy가 `no-referrer` 아님(SW fetch Origin 유지) · 본문 = 저장소 `sw.js`(v3) → `/` = 저장소 index.html 바이트 → POST 201 `{"created":true}` · no-store · 행 1 → 같은 것 204 → DELETE 204 · 행 0 → Origin 없이 403 → 어제부터 summary null 날 찾기(≤ 5, `deploy12-found.json`) → finally 그 endpoint만 지움 · 남은 `o7-deploy-` 0 · 9표 + push + 마이그레이션 스냅숏 같음(`snapshotSame: true`). 배포 요청 약 7~11.
2. **줄 13(CDP ≤ 25 — NetLog `HTTP_TRANSACTION_SEND_REQUEST`로 셈, DB 쓰기 0)**: `node jdeploy13.mjs`(id는 줄 12 결과) — 새 프로필 360 · 배포 호스트 밖 이름 풀이 막음 · QUIC 끔 · `/_vercel/*` 막음 · 가짜 pushManager(진짜 subscribe 0) · `/api/push/`는 페이지 CDP Fetch가 받아 첫 POST 201 · 그 뒤 204(서버 · DB에 안 감). 기대 5/5: SW v3 activated · `issuerank-v3: [/offline.html]` · 설치 오류 [] → 가짜 구독 a + 표시 on 새로 고침 = `POST a 201 → DELETE a 204 → POST b 204` · Origin = 배포 주소 · subscribe 1(granted · 활성화 없음) · on · 토스트 0 → `/?issue={id}` 상세창 `요약 없음 · 원문을 확인해 주세요` · 옛 문구 0 → 응답 받은 요청 호스트 = 배포 호스트 · NetLog 이름 = 배포 호스트 · `~notfound`뿐 · 배포 요청 ≤ 25(시험 운전 18).
3. 끝 확인: CDP 9491 · `curl` 000 · ps에 내 chromium 0 · 프로필 · NetLog 삭제(스크립트가 함) · `JUDGE_TEST_PREFIX=o7-deploy- node jdb.mjs test` → `deploy 0`. 두 줄 OK면 합계 OK 13 · SKIP 1(14) → 사용자 줄 14.

## 11. 요청 · 정리

- 요청: 로컬 3000 **61/100**(심사 카운터 — Node 15 · 브라우저 46, 막힘 0) · 가짜 서버 3100 1,676(따로, 시험 운전 · 디버그 포함) · 배포 0 · 바깥 0(newsdata · OpenAI · Google · Kakao · 실제 푸시 서비스 · GitHub API · jsdelivr 실제 연결 0).
- DB(실제, 안전 실행): 시작 스냅숏 11:56:47 → 쓰기 11:58:06~11:58:15 · 12:27:12~12:28 KST(04:00~10:00 밖) → 끝 스냅숏 12:29:39 = 시작(9표 + push_subscriptions + 마이그레이션 11항목 건수 · 내용 해시 같음). 쓴 곳 = `push_subscriptions`의 `https://fcm.googleapis.com/fcm/send/o7-judge-{uuid}-{1|2|3}` **3개 endpoint**, 동시에 남은 것 최대 2(④ 순간) → finally에서 그 endpoint만 지움 · **남은 `o7-judge-` 0**(`o7-build-` · `o7-deploy-` 0, push 전체 0). 실제 사용자 데이터 쓰기 0. 출력 = 건수 · 해시 앞부분 · bool만, 누출 false.
- 프로세스: 띄우기 전 PID 목록 저장(`ps-before-*.txt`) · 내 것만 종료(가짜 서버 · next 서버 2번은 표지 파일로 스스로 끝냄, Chromium은 `Browser.close` 뒤 내 자손만 — 매번 남은 0) · 넓은 패턴 kill 0. 끝 확인 12:38: `curl` 3000 · 3001 · 3100 · CDP 9471 · 9472 · 9481 · 9491 = **000** · ps에 next-server · next start · 내 chromium · proot 0 · **code-oss 24593 살아 있음** · 프로필 · NetLog 삭제 · HEAD 사본(`head/`) · 바꾼 사본(`mut/`) · 시험 운전 `endpoints-deploy.json` 삭제 · scratchpad 내 임시 파일 0.
- 심사 스크립트 · 증거: `ex12/.data/judge-o7-20261030/`(`jscope.mjs` · `japi.mts` · `jreal.mts` · `jlocal.mjs` · `jvm-ui.mjs` · `jvm-sw.mjs` · `jvm-summary.mjs` · `jmut*.mjs` · `jserver.mjs` · `jmock.mts` · `jcdp.mjs` · `jfake.mjs` · `juilib.mjs` · `jui-mock.mjs` · `jui-real.mjs` · `jdeploy12.mjs` · `jdeploy13.mjs` · 결과 `*-result*.json` · 그림 9장).
