# ops O6 북마크 폴더 삭제 — 제작 보고

- 날짜: 2026-10-20(KST 16:03~16:55) · 역할: 제작(`agent_prompts/2_build_agent.md`) · 기준: `plan/PLAN_ops_O6_folder_delete.md`(확정, 102줄), SPEC AC-10-5(회귀 AC-10-1~4 · 07-1~3 · 08-1~3 · 09-5 · 06-3 · 11-2 · 13-3 · 14-1), PLAN.md D21 · D29 · D30 · D31
- 사용자 요청(2026-10-20): "기존의 북마크 폴더를 삭제하는 기능이 없어. 이 부분을 추가해줘." · 결정(D31): 폴더와 안의 북마크를 함께 삭제(확인 창에 개수) · 폴더 칩을 고르면 `폴더 삭제` · 심사 뒤 푸시까지(코디네이터)
- 작업 폴더: `ex12/`(D21, 복사 없음). 시작 HEAD `f13f749`(origin보다 2커밋 앞 — 그대로, 푸시 안 함). 시작 때 미커밋 4개(`HANDOFF.md` · `PLAN.md` · `SPEC.md` · `plan/PLAN_ops_O6_folder_delete.md`)는 O6 코드가 아님 — 되돌리지 않았고 변경 범위 셈에서 뺌(이 보고 전까지 sha256 그대로 확인, 끝에 HANDOFF O6 줄 · PLAN.md 10절 메모 1줄만 고침)
- 하지 않음: git commit · push · Vercel/GitHub 설정 · Actions 실행 · `web/` · `plan/PLAN_web.md` · 계획 밖 기능(이름 변경 · 이동 · 공유 · 되돌리기 등 0). 새 npm 패키지 0 · 마이그레이션 0
- 증거 · 검증 스크립트: `ex12/.data/o6-build-20261020/`(git 제외 폴더)

## 1. 바뀐 · 새 파일 (= 계획 "변경 범위" 목록과 정확히 같음)

| 파일 | 구분 | 내용 |
|---|---|---|
| `ex12/server/schemas.ts` | 고침 +4 | `FOLDER_ID` = `^f(?:[0-9]{1,9}\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$`(계획 식 · openapi pattern과 글자 같음 — 확인함) |
| `ex12/server/app.ts` | 고침 +1 | Store `deleteFolder(userId, folderId): Promise<boolean>` + 설명 주석 |
| `ex12/server/sample.ts` | 고침 +8 | memoryStore.deleteFolder: 그 사용자 · 그 id 폴더 없으면 false, 있으면 그 사용자 · 그 폴더 북마크 → 폴더 삭제(순서 그대로) |
| `ex12/server/db/store.ts` | 고침 +12 | dbStore.deleteFolder: `db.transaction` 하나 — `folders` 행 `FOR UPDATE`(id + user_id) → 없으면 false(쓰기 0) → `bookmarks`(user_id + folder_id) 삭제 → 그 폴더 삭제 → true |
| `ex12/server/routes/folders.ts` | 고침 +9 −1 | `route.delete('/:folderId')`: 형식 틀림 400 `요청 형식이 올바르지 않습니다`(저장소 호출 0) → false면 404 `폴더를 찾을 수 없습니다` → 204 본문 없음. 세션 401 · no-store는 기존 `app.ts` 미들웨어 그대로 |
| `ex12/openapi.yaml` | 고침 +39 −1 | 새 경로 `/api/folders/{folderId}` delete(`operationId: deleteFolder` · sessionCookie · pattern · 204 · 400 · 401(LoginRequired) · 404) · `info.version` 0.5.0 → 0.6.0. 지운 줄 = `version: 0.5.0` 하나뿐 |
| `ex12/public/index.html` | 고침 +101 −1(보완 포함, 15절) | 덩어리 5개 = CSS 2곳(`.chip .cnt` 뒤 `.chip-del` · `:hover` / `.share-text` 뒤 `#folderDialog` danger · hover · disabled) · `folderDialog` 마크업(`shareDialog` 뒤) · renderBookmarks 칩 문자열 + 고른 칩 가로 맞춤 · 새 구역 `/* ─── Folder delete (O6)`(`syncBookmarkUI` 뒤 · Toast 앞) |
| `ex12/test/folder-delete.test.ts` | 새 177줄 | API · 저장소 9개(4절) |
| `ex12/test/folder-delete-ui.test.ts` | 새 343줄(보완 포함) | 화면 함수 VM 11개(4절 · 15절) |

- 그대로(검증 1): 계획 목록 76개 + 기존 테스트 26개 = **102개 sha256 작업 전후 같음**(`hash-before.txt` → `sha256sum -c` 전부 OK). `package-lock.json` `0b248041…` 같음. ex01~ex11 · `web/` · `frontend/` 변경 0. 이름 변경 · 이동 · 공유 코드 0.
- index.html: 기존 VM 테스트가 잘라 쓰는 구역 24곳(api · loadBookmarks · postBookmark~syncBookmarkUI · applyUser · 인증 · Events · DATA · 날짜 · ranking · issueById · loadRanking · ORDER · viewFromHash · issueShortcut~`start();` · openDrawer · findBookmark · Share · Push · Header CSS · Responsive · header-right 마크업 · Init · Toast · syncBookmarkUI 본문)이 HEAD와 글자 같음. 바뀐 구역은 renderBookmarks뿐(계획대로). head의 meta · link · title 20개 같음 · style은 옛 줄 그대로 + 8줄. `\nstart();\nregisterServiceWorker();\n</script>` 그대로 · `registerServiceWorker()` 2 · `serviceWorker.register(` 1.

## 2. id 형식 확인(확인 필요 1) · 실제 DB

- **형식 밖 폴더 id = 0**(읽기 전용, 16:12 KST): folders 2행 — JS 식 0 · SQL 식 0 · 형식 밖 `bookmarks.folder_id` 0(id 값 출력 0, `db-idcheck.json`). → 멈추지 않고 진행.
- 시작 스냅숏(16:13): 9표 + push_subscriptions(1행) + 마이그레이션 기록(8) 건수 · 내용 md5(`db-snapshot-start.json`).
- 쓰기 = 임시 계정 **1개**(`o6-build-<uuid>@example.invalid`, 16:31 가입)와 그 계정의 폴더 · 북마크 · 세션 · account뿐. DB 쓰기 시각 16:31~16:47(04:00~10:00 밖, 스크립트가 창 안이면 멈춤).
- 남의 폴더 확인 = 가짜 userId(`o6-other-<uuid>`)로 실제 `dbStore(db).deleteFolder` 직접 → false · 임시 행 그대로(실제 사용자 폴더 id로 보낸 요청 0).
- DELETE마다 바로 뒤 "임시 계정 밖 bookmarks · folders" 해시 = 시작: Node 5번(A · A 다시 · 새 UUID · C · bad) + 직접 호출 1 + 브라우저 준비 · D · E · F(Node) 뒤 각각 + 화면 F(404) · G(500, 서버 안 감) 뒤 한 번 = 모두 같음.
- 정리(16:47, 그 user_id 행만, 한 트랜잭션 bookmarks → folders → session → account → user): 지움 1 · 3 · 1 · 1 · 1 → **남은 행 0**(5표 모두) · `o6-build-` 계정 0.
- 끝 스냅숏 = 시작: 9표 + push_subscriptions + 마이그레이션 **모두 같음**(임시 계정 제외 비교 · 제외 없이 다시 찍은 것 둘 다).

## 3. PLAN 항목별 반영 위치

| 계획 | 반영 |
|---|---|
| API 순서(세션 → 형식 → 저장소) | `app.ts` 기존 미들웨어(`/api/(bookmarks\|folders)(/\|$)` → 401 · no-store) → `routes/folders.ts` 형식 400 → `store.deleteFolder` |
| 없음 · 남의 것 같은 404 | 저장소가 (id, user_id)로만 찾음 → 둘 다 false → 같은 404(상태 · 본문 · 머리 이름 같음 — 단위 ⑤ · 실제 서버 5) |
| 성공 204 · no-store | `c.body(null, 204)` + 미들웨어 머리(단위 ① · ⑧ BFF · 실제 서버) |
| DB 한 트랜잭션 · FOR UPDATE | `db/store.ts` `.for('update')` → bookmarks → folders. FK 두 개(no action) 때문에 북마크 먼저 |
| 메모리 같은 결과 | `sample.ts`(단위 ① ⑨) |
| Origin 검사 더하지 않음 · BFF 그대로 | 코드 0(BFF DELETE 내보내기 · 개인 경로 판정 기존 그대로 — 단위 ⑧) |
| 계약 | `openapi.yaml`(4절) |
| 버튼(전체 · 비로그인 · 폴더 0 = 없음, esc) | renderBookmarks `picked`(전체 길에는 새 DOM 조회 · 메서드 0 — 기존 account-ui 테스트 그대로 통과 + 새 VM ①) |
| 꼴 · 자리(sticky · 30px · 8px · 6px 띠 · 다크 토큰) | CSS 1곳 — 화면 6 ①②(대비 밝게 4.94 · 어둡게 6.44) |
| 고른 칩 보이기(가로만, scrollIntoView 0) | renderBookmarks: 고른 칩 오른쪽 − (버튼 왼쪽 − 6) > 0이면 `scrollLeft += ceil` |
| 확인 창 마크업 · 문구(n ≥ 1 · n = 0) · textContent | `folderDialog`(로그인 창 꼴 · role alertdialog) + `openFolderDelete` |
| 열기 · 닫기 · 초점 · 바깥 누름(사각형 밖만) · keydown stopPropagation | 새 구역 addEventListener(칩 줄 위임 · 취소 · 삭제 · cancel · click · keydown) |
| 삭제 중(disabled · opacity .5 · Esc · 바깥 · 다시 누름 무시) · 응답 오면 먼저 닫기 | `deleteFolder` `folderBusy` + CSS `#folderDialog button:disabled` |
| 성공(상태에서 바로 빼기 → 전체 → 다시 받기 → sync → 토스트 · 초점 전체) | `deleteFolder` 성공 길(VM ④ 순서 고정) |
| 실패(404 다시 받기 · 401 기존 · 그 밖 e.message · 계정 바뀜 = 창만 닫기) | `deleteFolder` 실패 길(VM ⑤~⑧) |

## 4. 테스트 · 빌드 · 계약

- `bash scripts/ex12-ubuntu.sh npm test`(안전 실행, 누출 false): **28파일 288개 통과**(기존 26파일 268 + 새 2파일 20). 기존 테스트 파일 수정 0. vitest가 바꾼 추적 파일 0. 마지막 확인 16:52 같은 결과.
  - `folder-delete.test.ts` 9: ① f1 삭제 204 · 본문 0 · no-store → 폴더 `f2 3 · f3 3` · 북마크 6(f1의 4개만 빠지고 순서 그대로) → 다시 404 → f1로 저장 404 ② 새 빈 폴더 204 · 북마크 10 그대로 ③ `abc` · `F1` · `f` · `f1x` · `f%20` · `f1%2Fx` · 대문자 UUID · UUID+1자 · 65자 = 400 · deleteFolder 호출 0 ④ `f999` · `f{새 UUID}` 404 ⑤ Better Auth 메모리 A · B: B→A 폴더 404(없는 폴더와 상태 · 본문 · 머리 같음) · A 그대로 / A 자기 폴더 204 · B 깊은 비교 같음 ⑥ 쿠키 없음 · 변조 · 만료 × id 4가지(형식 틀림 포함) = 401 · no-store · 저장소 호출 0 ⑦ GET `/api/folders/f1` · DELETE `/api/folders` · DELETE `/api/folders/f1/x` = Hono 기본 404 · 지운 것 0 ⑧ createBff 204 · 404 · 400 ⑨ memoryStore 직접(남의 userId) false · 변화 0
  - `folder-delete-ui.test.ts` 11: ① 칩(전체 · 고름 · 없는 폴더 · 비로그인 · 이름 `<b>&"` 이스케이프 · 전체 길 조회 = 기존 4개뿐) + 가로 맞춤 3경우 ② 열기 n = 3 · 0 ③ 취소 · Esc · 바깥(2) · 창 안 여백 · 창 안 글 ④ 204(잡아 둔 동안 다시 누름 · Esc · 바깥 · 취소 무시, 순서 `DELETE → close → GET bookmarks · folders → sync → toast`) + 다시 받기 실패해도 상태 맞음 ⑤ 404 ⑥ 500 · 502 · 네트워크 ⑦ 401 ⑧ 계정 바뀜(204 · 404 · 500) ⑨ keydown 7키
  - 새 테스트가 실수를 잡는지(제작 자체 확인, `mutate.sh`): 일부러 틀린 9가지(형식 식 넓힘 · 소유 검사 뺌 · 북마크 안 지움 · stopPropagation 뺌 · 가로 맞춤 뺌 · 닫기 전 토스트 · 보내는 중 취소 허용 · 404 다시 받기 뺌 · 전체 길에 DOM 조회) **9/9 모두 테스트가 실패** → 파일 되돌림 · sha256 같음.
- `bash scripts/ex12-ubuntu.sh npm run build`(= `next build && tsc --noEmit`): **성공**(Compiled · TypeScript · 라우트 3개 그대로, 16:25 — 모든 제품 파일 수정보다 뒤).
- `npx --offline @redocly/cli@2.64.1 lint openapi.yaml`(지금 판 · HEAD 판, 바깥 연결 차단기 + `REDOCLY_TELEMETRY=off`): **오류 0 · 경고 20 = HEAD 20**(경고 위치 · 규칙 목록 diff 0, 새 경로 경고 0) · `0.6.0` · 새 경로 delete 응답 = 204 · 400 · 401 · 404. redocly 바깥 연결 시도 0.

## 5. 검증 줄(PLAN 1~10)

| 줄 | 판정 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | **OK** | 1절. 바뀐 7 + 새 2 = 목록뿐 · 102 해시 같음 · index.html 덩어리 5 · ex01~ex11 · web 0 |
| 2 저장소 · API 단위 | **OK** | 4절 9개 |
| 3 화면 함수 VM | **OK** | 4절 11개 |
| 4 회귀 · 빌드 · 계약 | **OK** | 268 + 20 · 빌드 · tsc · OpenAPI(4절) |
| 5 로컬 서버 · 실제 DB | **OK** | `api-result.json` 24/24: `/` = index.html 바이트 · 쿠키 없이 401 · no-store → 가입 → 어제(10-19) 랭킹 → A · B · C + 북마크 A 2 · B 1 → DB 임시 행 3 · 3 → 직접 호출(가짜 userId) false · 그대로 → `DELETE {A}` 204 · no-store · 본문 0 → 임시 행 2 · 1(B) · 임시 계정 밖 해시 = 시작 → folders `[B 1, C 0]` · bookmarks B의 1 → `{A}` 다시 404 · 새 UUID 404(같은 본문 · 머리 이름) · `{C}` 204 · `bad` 400(각 뒤 해시 = 시작) · B 남김 |
| 6 브라우저 버튼 · 창 | **OK** | 7절 · `ui-real-result.json`(6 · 7 합계 82/82) |
| 7 브라우저 삭제 흐름 · 회귀 | **OK** | 7절 |
| 8 안전 · 한도 | **OK** | 9절: 3000 = 65/100 · 바깥 0 · 누출 false · 끝 스냅숏 = 시작 · 임시 행 0 · 소유 PID 0 · code-oss 30854 살아 있음 |
| 9 배포 | **[SKIP — 푸시 후 · 코디네이터]** | 절차 10절 |
| 10 휴대폰 실제 | **[SKIP — 사용자 확인 대기]** | 문장 11절 |

합계 **OK8 / FAIL0 / SKIP2**(9 · 10만 남음 — 판정은 독립 심사). 보완(15절) 뒤 3 · 4 · 6 · 7 다시 확인 = 그대로 OK.

## 6. 화면 — 브라우저 확인 방법

Termux Chromium 155 headless + CDP, 새 프로필(끝나면 삭제), `--host-resolver-rules="MAP localhost 127.0.0.1, MAP * ~NOTFOUND, EXCLUDE 127.0.0.1"`, 문서 전 임시 계정 쿠키 주입(`Network.setCookie`), 실제 입력 이벤트(`Input.dispatchMouseEvent` · `dispatchKeyEvent`), 포커스 에뮬레이션. 데이터: Node로 빈 폴더 D~H(이름 5~7자) + D에 어제 이슈 북마크 2(B 포함 칩 7개 — 360px 넘침).
- 실제 실행 전 같은 스크립트를 **가짜 서버(127.0.0.1:3100 — 실제 Hono 앱 + 메모리 저장소 + 가짜 세션, 실제 DB · 3000 · 바깥 0)로 3번 시험 운전**(77/77) — 임시 계정 1개 · 3000 한도 100 안에서 실제 실행을 한 번에 끝내려고. 가짜 서버는 `stop-mock` 파일로 끝냄(내 PID만).

## 7. 화면에서 본 결과(`ex12/.data/o6-build-20261020/ui-real-*.png`)

- 6 ① 360×800 밝게: 전체 = 버튼 0 · B = 칩 줄 마지막 `폴더 삭제` 1(type=button · aria-label `‘시험B 유지’ 폴더 삭제` · 높이 30 · 칩과 세로 가운데 같음 · 모서리 8px · sticky) · B(처음) = 버튼이 칩 줄 오른쪽 끝(row 16~344 · 버튼 266.5~344) · 다시 전체 = 0 · **끝까지 민 채 H = H 148~260.1 · 버튼 266.1~343.6(H 오른쪽 = 버튼 왼쪽 − 6, 띠까지 다 보임 · 겹침 0)** · 처음으로 민 채 B = 버튼 오른쪽 끝 · 문서 가로 넘침 0 · 페이지 세로 이동 0 · 서버 요청 0.
- 6 ② 어둡게(바탕 #121211) 같은 순서 같음 · 글자 대비 밝게 **4.94** · 어둡게 **6.44**(≥ 4.5) · 1280×800(새로 읽지 않음): 칩 7개는 이 기기 대체 글꼴로 1280에서도 넘침(766 > 712) → 끝까지 민 위치에서 버튼 = 마지막 칩 바로 뒤 **6px** / 칩 4개(7 ④ 뒤)는 넘침 0 · 간격 **6px**.
- 6 ③ D → `폴더 삭제` → 창 `‘시험D 둘’ 폴더를 삭제할까요?` · `폴더 안의 북마크 2개도 함께 삭제됩니다. 삭제하면 되돌릴 수 없습니다.` · role alertdialog · 초점 = 취소 · 360에서 넘침 0(324px, 8절 ②) · `삭제` 대비 **6.57**(#b42318) / Esc = 닫힘 · 초점 = 폴더 삭제 / 바깥 누름 = 닫힘 · 초점 = 폴더 삭제 / 창 안 여백 = 열린 채 / `1` 키 = 보기 그대로(#bookmarks) / 취소 = 닫힘 · 칩 · 개수 · 목록 그대로 · 서버 요청 0.
- 7 ① D 삭제: CDP Fetch로 DELETE를 잡은 동안 두 버튼 disabled · 흐림 .5 · 창 그대로 → 다시 누름 · Esc · 바깥 누름 = 무시(열린 채 · DELETE 1) → 놓음 → 204 → 창 닫힘 · 토스트 `‘시험D 둘’ 폴더를 삭제했습니다`(버튼 0) · 전체 눌림 · 초점 = 전체 · D 칩 0 · D 북마크 카드 0 · 헤더 숫자 3 → 1 · 요청 = DELETE 1 → GET bookmarks · folders. DB 임시 폴더 = 화면 칩.
- 7 ② 빈 E: 본문 `이 폴더는 비어 있습니다. 삭제하면 되돌릴 수 없습니다.` · 204 · 토스트 · 전체.
- 7 ③ F를 Node로 먼저 지움(204) → 화면에서 F 삭제 → 404 · 토스트 `폴더를 찾을 수 없습니다` · 다시 받아 F 칩 사라짐 · 전체.
- 7 ④ G의 DELETE에 CDP가 500(`Internal Server Error`, 서버 요청 0) → 토스트 `Internal Server Error` · G 칩 · 개수 · 고른 칩 그대로 · 초점 = 폴더 삭제 · 다시 누르면 창 열림.
- 7 ⑤ 회귀: 목록 어제(10-19) = D였던 이슈 2개 북마크 버튼 aria-pressed false · B 이슈 true · 북마크 안 된 카드 버튼 → 팝오버 폴더 = B · G · H(개수 = 칩, 열고 Esc로 닫기만) · 목록 카드 `공유` = `링크를 복사했습니다`(복사 글 = `[섹션] 제목` + `https://issuerank-kappa.vercel.app/s?issue=…`) · 헤더 360 = 종 34×32 · 설치 아이콘 0 · 북마크 글자 0 / 1100 · 1199 = 북마크 글자 0 / 1280 = 종 34×32 · 북마크 글자 보임 · 넘침 0 · 맨 아래 `앱 설치`(/widget#app) · `/?issue=2026-10-19-eco-2` = 그 이슈 상세창 · 쿼리 지움 · SW 활성 · 캐시 `issuerank-v2 [/offline.html]` · 내 서버만 연결을 끊고 새로 고침 = 오프라인 안내 / 쿠키 없는 새 문맥(쿠키 0) = 로그인 창 · `Google로 계속하기` · 초점 이메일 · 로그인 창 너비 1280 = 400 · 360 = 폴더 삭제 창과 같음 · Google 버튼 = 로컬 시작 요청 1 → `지금은 Google 로그인을 사용할 수 없습니다`(ex12/.env에 Google 값 없음, 바깥 0).
- JS 예외 0 · console error 0.

## 8. 꼭 필요했던 내부 세부 · 계획과 다른 점

1. 창 버튼 id `folderCancel` · `folderConfirm`(계획은 클래스만 정함, 처리 연결용). 그 밖 마크업 · 문구는 계획 글자 그대로.
2. 360px 창 너비 = **324px**: `.auth-dialog` `min(400px, 화면 − 32px)` = 328이지만 브라우저 기본 `dialog:modal { max-width: calc(100% − 6px − 2em) }`(= 360 − 36)가 더 작음. 같은 클래스인 로그인 창도 360에서 같은 너비(측정 — 링크 복사 창은 같은 클래스, 측정 안 함) — 코드 변경 0, 넘침 0.
3. Esc(cancel)는 늘 `preventDefault` 뒤 직접 닫음(보내는 중이면 닫지 않음) — 초점을 `폴더 삭제`로 정확히 돌리려고(브라우저 기본 닫기도 결과는 같음).

## 9. 요청 · 바깥 · 비밀값 · 프로세스

- 로컬 3000(세는 프록시, 시작 0): **65/100**(Node 25 · 브라우저 40 · 막힘 0). 오프라인 확인(새로 고침 1번)에서 끊긴 `GET /` 4는 크롬이 같은 탐색을 다시 시도한 것으로 보임(셈에 넣음). 가짜 서버(3100) 시험 운전은 3000 · DB 0.
- 바깥 **0**: next 서버(차단기 `monitor.mjs`, DB만 허용) = DB 소켓 1 · DB 이름 풀이 1뿐, 막힌 시도 0. 브라우저 = jsdelivr 글꼴 CSS 3번이 이름 풀이에서 실패(`ERR_NAME_NOT_RESOLVED`). NetLog: 모든 이름 = `~notfound`(localhost만 127.0.0.1), 크롬 자체 배경 요청(google · gvt1 호스트)도 이름 풀이에서 끝. 바깥 주소 소켓 = IPv6 도달 확인용 UDP connect(`[2001:4860:4860::8888]:443`)뿐이고 보낸 이벤트 0(UDP connect는 패킷 없음). newsdata · OpenAI · Google · Kakao · 푸시 서비스 · 배포 주소 · GitHub API 0.
- 비밀값: 모든 명령 출력 = 안전 실행(누출 true/false 검사 뒤만), 증거 파일 62개(PNG 말고) 누출 false. 세션 쿠키 파일은 정리 뒤 지움.
- 프로세스: 띄우기 전 PID 목록 저장 · 내 PID만 종료(proot 8229 → server.mjs 8306 → next-server 8322, 가짜 서버 20012 · 24284 · 내 Chromium 묶음). 끝 확인: curl 3000 · 3001 · 3100 · CDP 9366 · 9367 = 000 · ps에 next-server · next start · 내 chromium · 내 스크립트 0(Claude Code 자신 경로만 제외) · **code-oss 30854 살아 있음** · 프로필 · NetLog · 임시 파일 삭제.

## 10. 코디네이터 할 일 — 배포 줄 9(푸시 뒤, Node ≤ 20)

독립 심사 결과 사용자 줄만 남으면(D31 Q3): 커밋 · 푸시 → Vercel 배포 완료 확인 → **04:00~10:00 KST 밖**에서 Node로(Origin = `https://issuerank-kappa.vercel.app`, 안전 실행 · 바깥 = 배포 · DB뿐):
1. 시작 스냅숏(9표 + push_subscriptions + 마이그레이션) → `GET /` 본문 = 저장소 `ex12/public/index.html` 바이트 → 쿠키 없이 `DELETE /api/folders/f1` = 401 · no-store
2. 임시 계정 `o6-deploy-{uuid}@example.invalid` 가입(쿠키 `__Secure-better-auth.session_token`) → 어제 랭킹(어제부터 하루씩, 처음 200) 이슈 2개 → 폴더 A(북마크 2) · B(빈)
3. `DELETE {A}` = 204 · no-store · 본문 0 → `GET /api/folders` = `[B 0]` · `GET /api/bookmarks` = `[]` → `DELETE {A}` 다시 404 · `DELETE /api/folders/bad` 400 · `DELETE {B}` 204
4. `finally`: DB에서 그 user_id 행만 bookmarks → folders → session → account → user 삭제(남은 0) · 끝 스냅숏 = 시작. 배포 요청 약 14/20.
- 재사용: `ex12/.data/o6-build-20261020/api.mts`(주소 · Origin · 쿠키 이름 · 계정 접두 `o6-deploy-` · 한도 20으로) · `dbcore.mjs`(임시 이메일 식이 `o6-build-` — `o6-deploy-`로 바꿔 쓸 것) · `db.mjs snapshot`.

## 11. 사용자 할 일 — 줄 10(마지막 확인 목록에 넣을 문장, 계획 그대로)

- 휴대폰(Chrome 또는 설치한 이슈랭크 앱)에서 로그인 → 북마크 → 지워도 되는 폴더 칩을 누르면 칩 줄 오른쪽 끝에 `폴더 삭제`가 보이고 `전체`를 누르면 사라지는지 → 다시 그 폴더 → `폴더 삭제` → 창의 "북마크 n개도 함께 삭제됩니다"의 n이 칩 숫자와 같은지 → `취소`(바뀐 것 없음) → 다시 `폴더 삭제` → `삭제` → `‘이름’ 폴더를 삭제했습니다` 토스트 · `전체` 보기 · 그 폴더 칩과 안의 북마크가 사라짐(캡처). (선택) PC에서 새로 고쳐도 같은지. 지울 폴더가 없으면 새 폴더를 만들어 북마크 1개를 넣은 뒤 지워 보기.

## 12. 필요했지만 없던 키 · 계정

- 없음. (`ex12/.env`에 GOOGLE_* 없음 → 로컬 Google 버튼은 안내 문구 — 기대대로, 이번 범위 아님.)

## 13. 지운 것

- 기존 코드 삭제 0. 바뀐 기존 줄 = `routes/folders.ts` import 1줄(이름 더함) · renderBookmarks 칩 문자열 줄 끝(`+ 버튼`) · `openapi.yaml` `version` 1줄.

## 14. 추가로 보인 점 · 미해결

1. **→ 해결(15절, 보완 10-20)** (낮음 · 제안이었음) **삭제 뒤 `전체` 칩이 칩 줄 왼쪽 밖에 걸칠 수 있음**: 칩 줄을 오른쪽으로 민 채 폴더를 지우면 칩 줄 가로 위치가 그대로라, 눌린 `전체` 칩이 왼쪽 끝에 조금만 보임(`ui-real-7-D-deleted.png`). 초점은 `전체`에 가지만 이 Chromium의 `focus()`는 일부라도 보이는 요소는 밀지 않음(빈 문서 실험: 다 가려짐 → 0으로 밀림 / 8px · 30px 보임 → 그대로). 목록 · 토스트는 맞음. 계획에 없는 동작이라 넣지 않음 — 원하면 새 구역 성공 길에서 초점 전에 `document.getElementById('folderChips').scrollLeft = 0` 한 줄(renderBookmarks 전체 길은 그대로).
2. **→ 해결(15절, 보완 10-20)** (낮음 · 기록이었음) 보내는 동안 두 버튼이 disabled가 되면 크롬이 초점을 `body`로 옮김(측정) → 그 짧은 사이 누른 `1` · `2`는 창의 keydown을 거치지 않고 문서 단축키로 가 창 뒤 보기를 바꿀 수 있음(추정, 키 입력은 확인 안 함 · 창 안 초점일 때의 stopPropagation은 측정대로 작동 · 로그인 창 보내는 중과 같은 꼴). Esc는 측정한 순서(삭제 누름 뒤 Esc 1번)에서 막힘. 사용자 활성화 없이 Esc를 연달아 누를 때 브라우저 규칙(CloseWatcher)으로 창이 먼저 닫힐 수 있는지는 확인 안 함(추정) — 그래도 응답 처리(닫기 · 토스트 · 다시 받기)는 같은 길.
3. (기록) 크롬 Network 기록은 본문을 읽지 않은 204 응답(D · E)을 `ERR_ABORTED`로 적음 — 응답 상태 204는 받음(측정 표기일 뿐).
4. (기록) 확인 필요 3(같은 계정 두 기기: 창이 열린 사이 다른 기기에서 넣은 북마크도 함께 지워짐 · 지우는 순간 그 저장은 FK 오류 500) — 계획대로 기록만, 이번에 재현하지 않음. 확인 필요 2(`/privacy` 3항) 그대로. 확인 필요 4(실제 글꼴 · iPhone) → 사용자 줄 10 · 다음 iPhone 확인.

## 15. 보완(10-20, 코디네이터 판단) — 17:00~17:10 KST

- 요청: 14절 1 · 2를 **새 구역(syncBookmarkUI 뒤 · Toast 앞) 안에서만** 고침(`tabindex="-1"`은 새 마크업에만). 기존 VM 테스트 구역 · Events · Init · `start()` 표지 그대로(24곳 = HEAD 다시 확인).
- 바꾼 줄(`ex12/public/index.html`, 보완 전 대비 바뀐 줄 1 · 더한 줄 4):
  1. 새 마크업 `<dialog id="folderDialog" … tabindex="-1">` — 창 자체가 초점을 받을 수 있게(탭 순서에는 안 들어감, 문서 안 `tabindex="-1"`은 이것 하나)
  2. `deleteFolder`: 두 버튼을 잠그기 **전에** `folderDialog.focus({ preventScroll: true })`(+ 주석) — 보내는 동안 초점이 창 안에 머물러 창 keydown 막기(stopPropagation)가 계속 작동
  3. 성공 길: 토스트 뒤 · `전체` 칩 초점 **전에** `document.getElementById('folderChips').scrollLeft = 0`(+ 주석)
  - 끝난 뒤 초점은 계획 규칙 그대로: 성공 = `전체` 칩 · 500/502/네트워크 = `폴더 삭제`(칩 줄 위치 그대로) · 404 · 401 · 계정 바뀜 = 이전과 같음.
- 테스트(`folder-delete-ui.test.ts` 기대값 추가, 테스트 수 그대로 11): ② 새 마크업 `tabindex="-1"` 1개 / ④ 칩 줄을 민 채(120) 삭제 → 잡힌 동안 초점 = 창 자체(preventScroll) · 잠금보다 먼저 → 성공 뒤 마지막 두 동작 = `scrollLeft 0` → `전체` 초점 / ⑥ 500 · 502 · 네트워크 = `창 초점 → 잠금 → 풀기 → 폴더 삭제 초점` 순서 · 칩 줄 위치 그대로. `npm test` **28파일 288개 통과**(17:03) · 보완 2개를 하나씩 빼면 테스트 실패 **2/2**(되돌림 sha256 같음) · `npm run build` **성공**(17:05, tsc 포함).
- CDP(가짜 서버 127.0.0.1:3100 = 실제 Hono 앱 + 메모리 저장소 + 가짜 세션 — 3000 · 실제 DB · 임시 계정 0, 새 프로필 · 끝나면 삭제): **83/83**(앞 77 + 새 6), JS 예외 0.
  - 360px: D를 고른 채 칩 줄을 끝까지 밀고(scrollLeft > 0) `폴더 삭제` → `삭제` → DELETE를 잡은 동안 `document.activeElement` = 창 자체(DIALOG, body 아님) → `1` · `2` 키 = 보기 `bookmarks` · 주소 `#bookmarks` · history 길이 2 그대로 · 창 열린 채 → 놓음 → 204 → 칩 줄 scrollLeft 0 · `전체` 칩 칩 줄 안에 온전히 보임 · 초점 = `전체` 칩(`ui-mock-7-D-deleted.png`).
  - G의 DELETE를 잡은 동안 초점 = 창 · `1` · `2` 키 변화 0 → CDP가 500 → 토스트 `Internal Server Error` · 초점 = `폴더 삭제` · 고른 칩 그대로.
  - 회귀: 6 ③(Esc · 바깥 · 창 안 여백 · `1` 키 · 취소 · 초점) · 7 ②③ · 7 ⑤(목록 · 팝오버 · 공유 · 헤더 · 바로가기 · 오프라인 · 로그인 창) 그대로 OK.
- 안전: 보호 102파일 해시 같음 · 새 패키지 0 · 로컬 3000 누계 **65/100**(보완 0) · 실제 DB 접속 · 쓰기 0 · 임시 계정 0 · 바깥 0(jsdelivr 이름 풀이 실패 3뿐, NetLog 바깥 소켓 = IPv6 확인 UDP connect, 보낸 이벤트 0) · 누출 false · 내 PID(가짜 서버 · Chromium)만 종료 → 3000 · 3001 · 3100 · CDP 포트 000 · 내 프로세스 0 · code-oss 30854 살아 있음.
- 검증 줄 합계 그대로 **OK8 / FAIL0 / SKIP2**. 보완 두 동작의 브라우저 근거는 가짜 서버(위), 실제 서버 화면 결과는 7절 그대로.
