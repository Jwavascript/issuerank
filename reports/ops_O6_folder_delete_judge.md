# ops O6 북마크 폴더 삭제 — 독립 심사 보고

- 날짜: 2026-10-20(KST 17:17~18:05) · 역할: 독립 심사(`agent_prompts/3_judge_agent.md`, 운영 단계) · 코드 수정 0
- 대상: `ex12/` 작업 트리 — HEAD `f13f749` 대비 미커밋 변경. 제작이 말한 범위 = 제작 보고 1절(바뀐 7 + 새 테스트 2)
- 기준: `plan/PLAN_ops_O6_folder_delete.md`(확정) · SPEC AC-10-5 · US-10(AC-10-1~4 회귀) · 계획의 회귀 AC · PLAN.md 5절 D21 · D29~D31 · 6절 · AGENTS.md
- 사용자 결정(10-20): 폴더와 안의 북마크를 함께 삭제(확인 창에 개수) · 폴더 칩을 고르면 `폴더 삭제` · 심사 뒤 푸시까지
- 코디네이터 판단(사용자 승인 아님, 10-20): 제작 보고 15절 "보완"(성공 뒤 칩 줄 `scrollLeft = 0` · 보내는 동안 창 `tabindex="-1"`에 초점) — 계획 밖 변경으로 8절에서 따로 판정
- 제작 보고는 주장으로만 봄. 판정 근거는 모두 심사가 새로 쓴 스크립트(`ex12/.data/judge-o6-20261020/`, git 제외)로 만듦. 제작 스크립트(`o6-build-20261020/`)는 돌리지 않음

## 0. 판정

**PASS 보류 — OK 9 / FAIL 0 / SKIP 1**(10 사용자 확인만 남음 — 2026-10-20 18:12 배포 줄 9 OK, 12절). 처음 심사 때는 OK 8 / FAIL 0 / SKIP 2(9 배포 · 10 사용자). 위반 0. **커밋 전에 고칠 것 없음**(그 뒤 커밋 `ae4b31b` 푸시 · 배포 반영). 발견한 점은 모두 낮음 · 참고(7절). 보완(코디네이터 판단) = 적절, 부작용은 낮은 참고 2개(8절).

## 1. PLAN 항목별

| 계획 항목 | 판정 | 근거(심사 확인) |
|---|---|---|
| API 순서: 세션 → id 형식 → 저장소 | done | 쿠키 없음 · 변조 · 빈 값 × id 7가지(형식 틀림 포함) = 401 · 저장소 호출 0(j-api A), 실제 서버도 `bad` 쿠키 없이 401(j-real 1) |
| id 형식(계획 식 그대로, 틀리면 400 · 저장소 0) | done | 형식 밖 33가지 400 · 같은 문구 · no-store · 저장소 호출 0 / 형식 맞음 10가지 = 저장소까지(j-api B). 서버 식 = 계획 식 = openapi pattern 글자 같음 |
| 없음 · 남의 것 같은 404 | done | 메모리 두 사용자: 남의 f1 = f999 = 새 UUID → 상태 · 본문 · 머리 전부 같음(앱 · BFF 둘 다, j-api C). 실제 서버: {A} 다시 · 새 UUID 404 본문 · 머리(date 뺌) 같음(j-real 8) |
| 성공 204(본문 없음) | done | 메모리 · BFF · 실제 서버 모두 204 · 본문 0 · `cache-control: no-store`(content-type 없음) |
| 지우기(DB) 한 트랜잭션 · FOR UPDATE → 북마크 → 폴더 | done | 실제 DB(임시 계정): 중간 실패 3가지(둘째 delete 전 · 둘째 delete 실행 뒤 · 첫째 delete 전) 모두 되돌림(j-real 6). 동시: 지우기가 잠근 사이 저장 = 기다렸다 FK 23503, 저장 트랜잭션이 먼저 = 지우기가 기다렸다 그 북마크까지 지우고 성공(j-real 10 — FOR UPDATE가 실제로 걸림). 빌드 청크에 `.for("update")` |
| 지우기(메모리) = DB와 같은 결과 | done | 같은 순서(남의 · 자기 · 다시 · 빈 · 없는 UUID) → 반환값 `[false,true,false,true,false]` · 폴더 · 북마크 순서 · 끝 상태 메모리 = DB(j-real 9) |
| 응답 · 보호(no-store · Origin 검사 안 더함 · 그 밖 Hono 404) | done | 401 · 400 · 404 · 204 · 500 모두 no-store. 500(저장소 오류) = `Internal Server Error` · 로그 `API_INTERNAL_ERROR`뿐(j-api500). GET/PUT/PATCH/POST `/api/folders/f1` · `DELETE /api/folders(/)` · `/f1/x` · `/f1/` = 404(세션 있을 때), 지운 것 0(j-api E). Origin 코드 0(diff) |
| 계약 openapi(0.6.0 · 새 경로만) | done | redocly bundle로 읽음: `info`에서 version만 다름 · 새 경로 1 · 그 밖 경로 · components 글자 같음 · delete = `deleteFolder` · sessionCookie · pattern · 204/400/401/404 |
| 화면 버튼(폴더 고를 때만 · 마지막 자식 · esc) | done | 실제 서버 360: 전체 0 · B = 마지막 자식 1(type=button · class · aria-label · 글자 · data) · 다시 전체 0. VM: 비로그인 · 없는 폴더(→ 전체) = 0, 이름 `<b>&"x` 이스케이프(브라우저에서도 칩 · aria-label · 창 제목 글자 그대로) |
| 꼴 · 자리(sticky · 30px · 8px · 6px 띠 · 다크) | done | 높이 30 · 칩과 세로 가운데 같음 · 모서리 8px · sticky · 바탕/띠 = 페이지 바탕색, 대비 밝게 4.94 · 어둡게 6.44, 1280 칩 4개 = 사이 6px |
| 고른 칩 보이기(가로만) | done | 끝까지 민 채 H = H 오른쪽 ≤ 버튼 왼쪽 − 6 · 겹침 0 · 버튼 오른쪽 끝(밝게 · 어둡게), 문서 가로 넘침 0 · 세로 이동 0 |
| 확인 창(로그인 창 꼴 · 문구 n ≥ 1 / n = 0 · danger 색) | done | 제목 · 본문 글자 그대로 · role alertdialog · 360 너비 324 · 1280 400 · 어둡게도 밝은 꼴, 대비: 제목 · 본문 15.91 · 취소 15.91(hover 13.61) · 삭제 6.57(hover 8.66) |
| 열기 · 닫기 · 초점(Esc · 바깥 · 여백 · 단축키) | done | 초점 = 취소 / Esc · 바깥 = 닫힘 · 초점 = 폴더 삭제 / 여백 = 열린 채 / `1` `2` `3` `←` = 보기 그대로 / 여닫는 동안 서버 요청 0 |
| 삭제 중(잠금 · 흐림 · Esc · 바깥 · 다시 누름 무시 · 응답 오면 먼저 닫기) | done | CDP로 DELETE를 잡아 둔 동안 두 버튼 disabled · opacity 0.5 · 다시 누름 · Esc 1번 · 바깥 · `1` `2` 무시 · DELETE 1, 빨리 두 번 눌러도 DELETE 1. (Esc를 연달아 두 번이면 브라우저 규칙으로 닫힘 — 7절 1, 계획 검증 7①은 Esc 1번) |
| 성공 204 처리 | done | 창 닫힘 → 토스트 `‘심사D 둘’ 폴더를 삭제했습니다`(버튼 0) · 전체 눌림 · 초점 = 전체 · D 칩 · 카드 0 · 헤더 3 → 1 · 요청 = GET bookmarks · folders · 이슈맵 타일 북마크 표시 · 상세창 버튼 · 팝오버 목록 맞음 |
| 실패 처리(404 · 401 · 500/502/네트워크 · 계정 바뀜) | done | 404 = 토스트 + 다시 받아 칩 사라짐 · 전체 / 500 · 502 · 네트워크 = 각 문구 · 상태 · 고른 칩 그대로 · 초점 = 폴더 삭제 · 다시 열림 / 401 = 로그아웃 상태 · 토스트 하나 `로그인이 필요합니다 [로그인]` · 서버 폴더 그대로 / 계정 바뀜(204 · 404 · 500) = 창만 닫힘(VM) |
| 코드 위치(새 구역 · CSS 2곳) | done | 새 구역 = `syncBookmarkUI` 끝 ~ Toast 앞(글자 위치 확인) · CSS = `.chip .cnt` 뒤 · `.share-text` 뒤 |
| 기존 동작 보존(기존 테스트 0 수정 · VM 구역 · Events · Init · start()) | done | 3절 |
| SPEC · PLAN.md 반영(계획 역할 문서) | done | SPEC AC-10-5 · 범위 밖 줄 · PLAN.md D31(diff 확인), PLAN.md 10절 메모 1줄(제작) |

## 2. AC

| AC | 판정 | 근거 |
|---|---|---|
| **AC-10-5** | 충족(사용자 줄 10 남음) | 폴더 칩 → `폴더 삭제` → 창의 개수 = 칩 숫자(2 · 0) → 취소 = 변화 0 → 확인 = 서버에서 폴더 · 안의 북마크 삭제(실제 DB 행으로 확인) + 전체 보기, 남의 폴더 = 404 · 변화 0 |
| AC-10-1 · 10-2 · 10-3 · 10-4 | 충족(회귀) | 가짜 서버 브라우저(j-ui-ac10): 팝오버 = 기존 폴더 + 개수 → 저장 · 새 폴더(앞뒤 공백 뺌) 만들고 바로 저장 · 칩 거르기 · 개수 · 같은 이슈 다른 폴더 409. API는 HEAD 판 앱과 29요청 결과 같음 |
| AC-07-1 · 07-2 | 충족(회귀) | 저장 → 버튼 켜짐, 해제 → 서버 삭제 · 토스트 → 되돌리기 = 같은 폴더로 |
| AC-07-3 | 충족(기존 테스트) | 문서 클릭 분기(Events) = HEAD 글자 같음 · google-ui/account-ui 테스트 통과. 브라우저로 따로 누르지 않음(참고) |
| AC-08-1 · 08-2 | 충족(회귀) | 북마크 카드 · 최근 저장 순 · 빈 폴더 고르면 `북마크한 기사가 없습니다`(스크린샷) |
| AC-08-3 | 사용자 줄 10 | 다른 기기 흉내(Node가 먼저 F 삭제 → 화면 404 → 다시 받아 사라짐)는 OK |
| AC-09-5 | 충족(회귀) | 어제(10-19) 이슈 북마크 저장 201 · 어제 목록 표시 맞음 |
| AC-06-3 · AC-11-2 · AC-13-3 · AC-14-1 | 충족(회귀) | Google 버튼 = 로컬 시작 요청 → 안내 문구(.env에 Google 값 없음) · `/?issue=` 상세창 · 쿼리 지움 · 목록 카드 공유 = 링크 복사(`/s?issue=<id>`) · 종 34×32 |
| 범위 밖(이름 변경 · 이동 · 공유) | 충족 | 더한 줄에 해당 코드 · 경로 0 |

## 3. 범위 · 불변(검증 1)

- 바뀐 추적 파일(작업 트리 blob = HEAD와 다른 것, 682개 전부 비교) = 코드 7(`openapi.yaml` · `public/index.html` · `server/{app,schemas,sample}.ts` · `server/routes/folders.ts` · `server/db/store.ts`) + 문서 3(HANDOFF · PLAN · SPEC). 새 파일 = 테스트 2 + 문서 2(계획 · 제작 보고). 지운 파일 · 이름 바꿈 · staged 0. → 제작 보고 1절과 같음. 문서 5개는 따로 셈(계획 역할 + 제작 메모).
- numstat: index.html +101 −1 · openapi +39 −1 · folders.ts +9 −1 · store.ts +12 · sample.ts +8 · schemas.ts +4 · app.ts +1 / 새 테스트 177 · 343줄.
- 보호 102개(계획 검증 1 목록 76 + 기존 테스트 26) = HEAD blob 같음 · `package.json` · `package-lock.json` · `drizzle/` · `db/schema.ts` · BFF(`server/bff.ts` · `app/api/[...path]/route.ts`) · 다른 public HTML · `sw.js` · `next.config.mjs` · 워크플로 2개 = HEAD. ex01~ex11 · `web/` · `frontend/` 변경 0. 새 import = 기존 의존성뿐(새 패키지 0) · 마이그레이션 0.
- index.html: `git diff -U0` 덩어리 6개(= 내용 5곳: CSS 2 · 마크업 1 · renderBookmarks 1(덩어리 2) · 새 구역 1). 기존 테스트 26개가 `between()`으로 자르는 구역 24개를 HEAD 판 · 지금 판에서 직접 잘라 비교 → 23개 글자 같음, 1개(`function renderBookmarks()`~Tooltip, account-ui)만 다름 = 계획이 바꾸라고 한 곳. 그 구역은 VM으로 HEAD와 비교: '전체' 길 4가지 상태에서 DOM 접근 순서 · 결과 글자 · folderFilter 모두 HEAD와 같음, 고른 길 = HEAD 칩 글자 + 버튼 글자 하나 · 더한 접근 = 고른 칩 · 마지막 자식 · scrollLeft뿐(j-vm ①②).
- Events · Init~`start();` · 끝 줄(`\nstart();\nregisterServiceWorker();\n</script>`) · head · Toast~끝 · Data~View 3 · header 마크업 · 칩 줄~shareDialog = HEAD 글자 같음. `registerServiceWorker()` 2 · `serviceWorker.register(` 1 · `tabindex="-1"` 1(새 창만).
- 심사 시작 · 끝에 바뀐 · 새 파일 sha256 지문이 같음(심사 중 작업 트리 변경 0, `npm test` · build도 추적 파일 변경 0).

## 4. 테스트 · 빌드 · 계약(검증 4)

- `bash scripts/ex12-ubuntu.sh … npm test`(안전 실행 · 바깥 연결 감시기 `--import`): **28파일 288개 통과**(기존 26파일 268 + 새 2파일 20), 감시기 32프로세스 · 유닉스 소켓뿐(네트워크 0) · 누출 false · 추적 파일 변경 0.
- 깨끗한 빌드: `.next` · `tsconfig.tsbuildinfo` 지운 뒤 `npm run build`(= `next build && tsc --noEmit`) **성공**(라우트 3개 그대로, 감시기 11프로세스 · 연결 0, `NEXT_TELEMETRY_DISABLED=1`).
- `npx --offline @redocly/cli@2.64.1 lint`(HEAD 판 · 지금 판, 감시기 · `REDOCLY_TELEMETRY=off`): **오류 0 · 경고 20 = HEAD 20**, 경고 목록(규칙 · 심각도 · 위치) 완전히 같음 · 새 경로 경고 0 · 바깥 연결 0. bundle(JSON)로 읽어 구조 비교(1절 계약 줄).

## 5. 검증 줄(PLAN 1~10)

| 줄 | 판정 | 근거(심사 실행) |
|---|---|---|
| 1 변경 범위 · 불변 | **[OK]** | 3절 |
| 2 저장소 · API 단위 | **[OK]** | 새 테스트 9/9 + 심사 독립 `j-api.mts` 32/32(세션 먼저 · 형식 33가지 · 404 머리까지 같음 · 204 · 다른 사용자 깊은 비교 · BFF · 그 밖 경로 · 메모리 직접 · 형식 식 = openapi(말뭉치 12,043개 판정 같음 · `f${randomUUID()}` 2만 개 · 메모리 createFolder 300개 맞음 · 100만 자 < 200ms) · **HEAD 판 앱과 같은 요청 29개 = 상태 · 머리 · 본문 같음**) + 500 경로 |
| 3 화면 함수 VM | **[OK]** | 새 테스트 11/11 + 심사 `j-vm.mjs` 16/16(HEAD 비교 · 204/404/500/401/계정 바뀜 3가지 호출 순서 · 다시 받기 실패해도 상태 맞음) |
| 4 회귀 · 빌드 · 계약 | **[OK]** | 4절 |
| 5 로컬 서버 · 실제 DB | **[OK]** | `j-real.mts` 39/39(next start 3001 + 세는 프록시 3000): `/` = 저장소 바이트 · 401 → 가입(임시 계정 1) → 10-19 랭킹 → A · B · C + 북마크 → 남의 userId 직접 false → 중간 실패 3가지 되돌림 → DELETE {A} 204 → DB [B 1, C 0] → 목록 → 404 · 404(같은 본문 · 머리) · {C} 204 · bad 400 → 메모리 = DB → 동시 2가지 → 브라우저 준비. 각 DELETE 뒤 임시 계정 밖 해시 = 시작. 실제 DB 형식 밖 폴더 id 0(2행, SQL · JS 둘 다) |
| 6 브라우저 버튼 · 창 | **[OK]** | 실제 서버 Chromium 155(새 프로필): 360 밝게 · 어둡게 · 1280 · 창 · Esc · 바깥 · 여백 · 단축키 · 취소 모두 OK(1절 화면 줄들) |
| 7 브라우저 삭제 흐름 · 회귀 | **[OK]** | 실제 서버: D(잡아 둠) · F(Node 먼저 → 404) · G(500 · 502 · 네트워크, 서버 안 감) · 회귀(1280 6px · 헤더 360/641/1100/1199/1280 · 어제 목록 · 팝오버 · 공유 · `/?issue=` · SW · 오프라인 안내 · 다시 연결 · 쿠키 없는 문맥 로그인 창 · Google 버튼) OK. **빈 E는 첫 실행 FAIL → 심사 스크립트 오류**(상태가 바뀌자마자 = 다시 받기 전에 읽음, 실제 서버 GET이 가짜 서버보다 느림) → 기다림을 토스트 문구로 고쳐 빈 폴더 E2로 실제 서버 다시 **6/6 OK**(토스트 · 전체 · 초점 · 칩 줄 맨 앞 · 요청 · DB). 가짜 서버로 같은 흐름 63/63 · AC-10 회귀 11/11 · 이슈맵/상세창 4/4 · 어둡게 창 2/2 |
| 8 안전 · 한도(로컬) | **[OK]** | 10절: 3000 = 75/100 · 바깥 0 · 누출 false · 끝 스냅숏 = 시작 · 임시 계정 행 0 · 소유 프로세스 0 · code-oss 30854 살아 있음 |
| 9 배포 | **[OK]**(2026-10-20 18:11:50~18:12:00, 12절) | 처음 심사 땐 [SKIP — 푸시 후]. 커밋 `ae4b31b` 배포에서 `j-deploy9.mjs` 20/20 · 요청 14/20 · 임시 계정 `o6-deploy-` 1개 정리 남은 0 · 스냅숏 같음 · 바깥 = 배포 · DB뿐 |
| 10 휴대폰 실제 | **[SKIP — 사용자 확인 대기]** | 제작 보고 11절 문장 그대로 |

**합계 OK 9 / FAIL 0 / SKIP 1 → PASS 보류**(10 사용자 줄이 남아 PASS 아님. 처음 심사 합계 = OK 8 / FAIL 0 / SKIP 2).

## 6. 지켜야 할 것(위반 점검)

- 기사 본문 저장 · 표시 0(이번 변경은 폴더 · 북마크만) · 코드 · 테스트 · 문서에 키 · 접속 정보 0(diff · 새 파일 누출 검사 false) · `ex12/.env` git 제외(.gitignore:4) · 새 패키지 0 · D0 밖 출처 0 · `web/` · `plan/PLAN_web.md` 읽기/수정 0 · 커밋 · 푸시 0 · 실제 사용자 데이터 쓰기 0 → **위반 0**.

## 7. 발견한 점(심각도 순 — 모두 낮음 · 참고, 커밋을 막지 않음)

1. (낮음) **보내는 중 Esc를 연달아 누르면 두 번째에 창이 닫힘**: Chrome의 닫기 규칙(CloseWatcher — 사용자 활성화 없이 이어진 cancel은 `preventDefault`로 막을 수 없음). 가짜 서버 측정 `[true, false, false]`, **기존 로그인 창도 같은 결과**(보내는 중 Esc 3번 `[true, false, false]`). 닫힌 뒤에도 DELETE 1번 · `폴더 삭제` 다시 누름 무시 · 응답이 오면 토스트 · 전체 · 칩 정리 그대로라 데이터 · 화면 상태 영향 0. 계획 검증 7①(Esc 1번)은 OK. 바꾸려면 계획 결정이 필요해 기록만.
2. (낮음) **404(다른 기기에서 이미 지움) 뒤 초점이 body로 빠지고 칩 줄 가로 위치를 되돌리지 않음**: 실제 서버 · 가짜 서버 모두 초점 `BODY` · scrollLeft 28 → 밀려 있던 칩 줄이면 눌린 `전체` 칩이 왼쪽에 걸침(목록 · 칩 · 토스트는 맞음). 계획은 404 초점을 정하지 않음 → FAIL 아님. 보완과 같은 두 줄(`scrollLeft = 0` · 전체 칩 초점)을 404 다시 받기 뒤에도 쓰면 맞춰짐 — 다음 화면 작업 후보.
3. (낮음 · 보완 부작용) 8절 ①②.
4. (참고) 확인 필요 3(같은 계정 두 기기)을 실제 DB에서 재현: 지우기가 먼저 잠그면 그 사이 다른 기기 저장은 기다렸다 FK `23503`(→ 그 기기 500 토스트), 저장이 먼저면 지우기가 기다렸다 그 북마크까지 지우고 성공 — 계획 설명과 같음.
5. (참고) 1280에서도 칩 7개는 이 기기 대체 글꼴로 넘침(754 > 712, 제작과 같음) — 칩 4개에서 사이 6px 확인. 360 창 너비 324(브라우저 기본 dialog max-width, 로그인 창과 같음).
6. (참고) 경로 칸은 Hono가 퍼센트 인코딩을 풀어 읽음(`/api/folders/%66%31` = f1, 같은 자원) — 영향 0.
7. (참고) 서버 로그 `AUTH_INTERNAL_ERROR` 1번 = Google 버튼 시험(.env에 Google 값 없음 → Better Auth 오류 기록, O2부터 같음). DELETE 경로 새 로그 0.
8. (참고) 계획은 심사 임시 계정 표지를 `o6-judge-`로 적었고 코디네이터 지시는 `judge-o6-` — 지시대로 씀. 정리는 브라우저 단계 뒤 별도 명령(`jdb.mjs cleanup`, 계정 이메일 표지 확인 뒤 그 user_id 행만)으로 했음(같은 프로세스 finally가 아님 — 결과 남은 0).
9. (참고) 심사 스크립트 자체 오류(제품 문제 아님, 고친 뒤 다시 실행): j-api C 호출 수 셈(4 → 5) · 실제 7② 기다림 조건 · 가짜 서버 오프라인 표지(keep-alive 연결은 안 끊김 → 요청 단위로 끊게 고침).

## 8. 코디네이터 판단(보완) 평가

- 범위: 바뀐 곳 = 새 마크업 `tabindex="-1"` 1곳 + 새 구역(`deleteFolder`) 줄 3개. 기존 VM 구역 · Events · Init · `start()` 글자 그대로(3절). 계획 규칙(성공 초점 = 전체, 500/502/네트워크 초점 = 폴더 삭제)은 그대로 지킴.
- 효과(측정): ⓐ 칩 줄을 민 채 지워도 성공 뒤 scrollLeft 0 · `전체` 칩 칩 줄 안에 온전히(실제 서버 D · E2) ⓑ 잡아 둔 동안 `document.activeElement` = 창 자체 → `1` `2` 키 변화 0(실제 서버) ⓒ 덤: 창 안 여백을 누르면 초점이 창에 머물러 그 뒤 `1` 키도 창 뒤 보기를 바꾸지 않음.
- 부작용 점검:
  - 초점: 처음 초점은 그대로 `취소`(showModal 뒤 명시 focus) · 탭 순서에 창이 안 들어감(tabindex −1) · 끝난 뒤 초점은 계획대로.
  - ① (낮음) 키보드로 `삭제`를 누르면 보내는 동안 창 테두리에 브라우저 기본 초점 고리(`:focus-visible` · outline auto, `ui-mock-k-busy.png`). 마우스로 누르면 없음(측정 false). 짧고 초점 위치를 정확히 보여 줌.
  - ② (낮음) 스크린 리더: 보내는 동안 초점 노드 = alertdialog, 이름 = 제목 · 설명 = 본문 → 창을 한 번 더 읽음(틀린 정보 0). 성공 뒤 초점 노드 = 버튼 `전체 N` · pressed true, 토스트는 role=status.
  - 스크롤: `scrollLeft = 0`은 성공 길에서만 · 칩 줄만(페이지 세로 이동 0) · 1280처럼 안 넘치면 변화 0. 실패 길은 칩 줄 위치 그대로(테스트 기대와 같음).
  - 다른 창과 충돌: 문서 안 `tabindex="-1"`은 이 창 하나 · 로그인 · 링크 복사 창 마크업 그대로 · 401 뒤 토스트 `로그인` = 로그인 창만 열림(폴더 창 닫힘, 초점 이메일).
- 판단: **적절(받아들일 만함)**. 계획 밖이지만 새 구역 안에서 계획의 의도(창 뒤 단축키 막기 · 전체 칩 보이기)를 지키는 최소 변경이고 회귀 0. 7절 2(404 길)는 같은 원칙을 404에도 넓힐지 다음에 정할 후보.

## 9. 푸시 후 절차 — 배포 줄 9(코디네이터, 그대로 실행)

> 2026-10-20 18:11~18:12 KST 코디네이터 요청으로 심사가 이 절차를 다듬은 스크립트로 실행 → 줄 9 [OK](12절).

전제: 사용자 줄만 남았으므로(D31 Q3) 코디네이터 커밋 · 푸시 → Vercel 배포 완료 확인(대시보드 Ready — 기다리는 동안 배포 요청을 쓰지 않음) → **04:00~10:00 KST 밖**(스크립트도 막음).

```bash
# 저장소 루트(Termux node). 바깥 = 배포 주소 · DB만 허용(감시기 기록 guard-deploy9.jsonl)
JUDGE_TEMP_PREFIX=o6-deploy- JUDGE_TEMP_FILE=temp-deploy-account.json \
JUDGE_ALLOW=issuerank-kappa.vercel.app JUDGE_ALLOW_DB=1 \
JUDGE_GUARD_FILE=ex12/.data/judge-o6-20261020/guard-deploy9.jsonl \
node --import ./ex12/.data/judge-o6-20261020/jguard.mjs ex12/.data/judge-o6-20261020/j-deploy9.mjs
```

- 요청 14개(한도 20 — 넘기 전에 멈춤, Origin = `https://issuerank-kappa.vercel.app`): ① `GET /` = 200 · 저장소 `ex12/public/index.html` 바이트 ② 쿠키 없이 `DELETE /api/folders/f1` = 401 · no-store · `로그인이 필요합니다` ③ 가입 `o6-deploy-<uuid>@example.invalid`(쿠키 `__Secure-better-auth.session_token`, user id를 바로 `temp-deploy-account.json`에) ④ `GET /api/rankings?date=<어제 KST>`(없으면 하루씩, 최대 3) ⑤ `POST /api/folders` 배포A · 배포B ⑥ `POST /api/bookmarks` 2개(A) ⑦ `DELETE {A}` = 204 · no-store · 본문 0 ⑧ `GET /api/folders` = `[배포B 0]` · `GET /api/bookmarks` = `[]` ⑨ `DELETE {A}` 다시 = 404 `폴더를 찾을 수 없습니다` · `DELETE /api/folders/bad` = 400 · `DELETE {B}` = 204.
- DB(같은 운영 DB): 시작 스냅숏(9표 + push_subscriptions + 마이그레이션 기록 건수 · 내용 md5) → DELETE마다 "임시 계정 밖 folders · bookmarks" 해시 = 시작 → **finally**: 이메일 표지 `o6-deploy-` 확인 뒤 그 user_id 행만 bookmarks → folders → session → account → user(한 트랜잭션) → 남은 0 → 끝 스냅숏 = 시작(다르면 표 이름만 나옴 — 실제 사용자의 세션 갱신 등이면 우리 쓰기와 구분해 적기). 정리가 끝났을 때만 임시 파일을 지움(남으면 `JUDGE_TEMP_PREFIX=o6-deploy- JUDGE_TEMP_FILE=temp-deploy-account.json node ex12/.data/judge-o6-20261020/jdb.mjs cleanup`).
- 출력: 누출 검사(false) 뒤 요약만 · 결과 `ex12/.data/judge-o6-20261020/j-deploy9-result.json`. 판정: 모든 줄 OK · requests ≤ 20 · 정리 남은 0 · 스냅숏 같음 · guard 기록에 배포 · DB 말고 0 → 줄 9 [OK].
- 그다음 사용자 줄 10: 제작 보고 11절 문장(계획 "사용자 확인")을 마지막 확인 목록에. (선택) 실제 글꼴(Pretendard) 휴대폰 캡처로 칩 줄 끝 버튼 폭 확인(계획 확인 필요 4) · iPhone은 다음 iPhone 확인 때.

## 10. 요청 · 바깥 · 비밀값 · DB · 프로세스 정리

- 로컬 3000(심사 별도 카운터, 시작 0): **75/100**(Node 19 · 브라우저 56 · 막힘 0) + 오프라인 시험으로 서버에 안 간 끊긴 연결 5. 가짜 서버(3200, 실제 Hono 앱 + 메모리 저장소)는 3000 · DB 0.
- 바깥 **0**: next 서버 감시기 = DB 소켓 1뿐 · 심사 스크립트 = DB · 127.0.0.1뿐 · 테스트 · 빌드 · redocly · 가짜 서버 = 0. 브라우저: 모든 이름 풀이 `~NOTFOUND`(localhost만 127.0.0.1) — jsdelivr 글꼴 CSS · 크롬 자체 배경 요청(google · gvt1 호스트)은 이름 풀이에서 끝. NetLog 사건 종류별: TCP 연결 = 127.0.0.1뿐, UDP connect 2개(`[2001:4860:4860::8888]:443` · `127.0.0.1:443`, 크롬 주소 확인용 · 보낸 바이트 0), 그 밖 주소 = 이 기기 자기 IPv6(로컬 주소). newsdata · OpenAI · Google · Kakao · 푸시 서비스 · 배포 주소 · GitHub API 0.
- 비밀값: 모든 명령 출력 = 안전 실행(누출 true/false 뒤) · 증거 파일 83개(PNG 말고) 누출 false · 세션 쿠키 파일(`temp-account.json`)은 정리 뒤 지움.
- DB: 쓰기 17:48~17:52 KST(04:00~10:00 밖) · 임시 계정 **1개**(`judge-o6-<uuid>@example.invalid`)와 그 폴더 · 북마크 · 세션 · account만. 정리(17:52:10): 지움 bookmarks 1 · folders 3 · session 1 · account 1 · user 1 → **남은 0**(5표 + verification + `judge-o6-` 이메일 0). 시작(17:30) · 끝(17:52) 스냅숏 **같음**(임시 계정 제외 · 제외 없이 둘 다): issues 897 · articles 1,279 · collect_runs 102 · folders 2 · bookmarks 2 · user 2 · session 3 · account 1 · verification 0 · push_subscriptions 1 · 마이그레이션 8. 실제 사용자 폴더 id로 보낸 요청 0.
- 프로세스: 띄우기 전 PID 목록 저장(`ps-before-*.txt`) · 내 것만 종료(next = 내 프록시가 띄운 프로세스 그룹, Chromium = 내 프로필 · 자손, 가짜 서버 = stop 파일). 끝 확인: curl 3000 · 3001 · 3200 · CDP 9421~9427 = 000 · ps에 next-server · next start · 내 chromium · tsx · proot 0 · **code-oss 30854 살아 있음** · 프로필 · NetLog 지움(scratchpad에 o6 항목 0) · 임시 HEAD 추출본 지움.

## 11. 심사 스크립트 · 증거(`ex12/.data/judge-o6-20261020/`)

`jlib.mjs`(비밀값 · 안전 출력) · `jsafe.mjs`(안전 실행) · `jguard.mjs`(바깥 연결 감시 · 차단) · `jscope.mjs`(범위 · 불변 → `scope-start.json` · `scope-end.json`) · `j-api.mts`(API 독립, HEAD 판 비교는 `git archive HEAD ex12/server | tar -x -C head-src` 뒤 실행) · `j-api500.mts` · `j-vm.mjs` · `jserver.mjs`(next start 3001 + 세는 프록시 3000) · `jdb.mjs`(스냅숏 · 정리, `JUDGE_TEMP_PREFIX`) · `jdb-insert.mjs` · `j-real.mts` · `jcdp.mjs` · `j-ui.mjs mock|real` · `j-ui-e.mjs` · `j-ui-ac10.mjs` · `j-ui-map.mjs` · `j-ui-dark.mjs` · `j-netlog.mjs` · `jmock.mts`(가짜 서버 3200) · `j-deploy9.mjs`(줄 9) / 결과 `*-result.json` · `npm-test.out` · `npm-build.out` · `redocly-*.out` · `openapi-{head,work}.{yaml,json}` · `db-snap-*.json` · `db-cleanup.json` · `request-log.json` · `server-runs.json` · `guard-*.jsonl` · 스크린샷 `ui-real-*.png` · `ui-mock-*.png`.

## 12. 배포 줄 9 (2026-10-20)

- 요청: 코디네이터(18:08 KST 이후) — 커밋 `ae4b31b`(심사한 작업 트리 그대로 + 문서) 푸시 · Vercel 배포 완료(코디네이터 확인: GitHub 커밋 상태 success, 18:08 KST). 같은 푸시에 O5 기록 커밋 `8a457de` · `f13f749`(문서만). 심사는 **배포 줄 9만** 실행(GitHub API 호출 0 — 배포 상태는 코디네이터 확인을 따름).
- 실행 전 확인(18:10): HEAD `ae4b31b` · 작업 트리 깨끗함(`git status` 0줄) · 코드 7 + 테스트 2 파일 sha256 = 심사 때 지문(`scope-start.json`)과 모두 같음 → 배포된 커밋 = 심사한 코드.
- 스크립트: 9절 `j-deploy9.mjs`를 다듬음(기대와 다르면 그 줄에서 멈추고 finally 정리 · 가입 응답 200이면 쿠키 확인 전에 user id를 정리 파일에 · id를 못 받으면 이메일로 찾아 정리 · 헤더 요약은 쿠키 이름 · 속성만 · 누출 검사 뒤 파일 삭제) → 가짜 서버로 다시 시험 운전(14요청 10/10, DB · 배포 0) → 실제 1번.
- 실행: 18:11:50~18:12:00 KST(04:00~10:00 밖), Termux node + 바깥 연결 감시기(허용 = `issuerank-kappa.vercel.app` · DB). Origin = 배포 주소. **결과 20/20 OK · 멈춤 없음 · 요청 14/20**(새 별도 카운터).

| # | 요청 | 상태 · 머리 요약 | 판정 |
|---|---|---|---|
| ① | `GET /` | 200 · `text/html; charset=utf-8` · `public, max-age=0, must-revalidate` · 92,942B = 저장소 `ex12/public/index.html` 바이트 | OK |
| ② | 쿠키 없이 `DELETE /api/folders/f1` | 401 · `no-store` · `{"message":"로그인이 필요합니다"}` | OK |
| ③ | `POST /api/auth/sign-up/email`(`o6-deploy-<uuid>@example.invalid`) | 200 · `no-store` · Set-Cookie `__Secure-better-auth.session_token; Max-Age=604800; Path=/; HttpOnly; Secure; SameSite=Lax`(값 기록 0) | OK |
| ④ | `GET /api/rankings?date=2026-10-19` | 200 · `public, max-age=0, must-revalidate`(오늘 제외 · 1번에 찾음) | OK |
| ⑤⑥ | `POST /api/folders` 배포A · 배포B, `POST /api/bookmarks` 2개(A) | 201 × 4 · `no-store` · id = `f` + 소문자 UUID / DB 임시 행 `[배포A 2, 배포B 0]` | OK |
| ⑦ | `DELETE /api/folders/{A}` | **204 · `no-store` · 본문 0 · content-type 없음** / DB 임시 행 `[배포B 0]` · 북마크 0(A의 2개 함께 지워짐) | OK |
| ⑧ | `GET /api/folders` · `GET /api/bookmarks` | 200 · `no-store` · `[배포B 0]` · `[]` | OK |
| ⑨ | `DELETE {A}` 다시 · `DELETE /api/folders/bad` · `DELETE {B}` | 404 `폴더를 찾을 수 없습니다` · 400 `요청 형식이 올바르지 않습니다` · 204 — 모두 `no-store` / DB 임시 폴더 0 | OK |

- 모든 응답 `x-vercel-cache: MISS` · 쿠키는 ③에서만. 머리 이름 = `cache-control` · `content-type`(204 없음) · `date` · `server` · `strict-transport-security` · `x-matched-path` · `x-vercel-cache` · `x-vercel-id`(+ 본문 있는 응답의 `transfer-encoding` · 일부 `content-encoding` · GET의 `age`). 결과 `ex12/.data/judge-o6-20261020/j-deploy9-result.json`.
- DB(운영 DB = 로컬과 같음): 준비 뒤 · DELETE 4번(⑦ · ⑨ 3개) 뒤마다 **임시 계정 밖 folders · bookmarks 해시 = 시작**(5/5). 쓰기 = 18:11:53~18:11:58, 그 임시 계정 행뿐.
- 정리(finally, 18:11:58): 이메일 표지 `o6-deploy-` 확인 뒤 그 user_id 행만 한 트랜잭션 — 지움 bookmarks 0 · folders 0(이미 API로 지움) · session 1 · account 1 · user 1 → **남은 0**(user · session · account · folders · bookmarks · verification · `o6-deploy-` 이메일). 정리 파일 `temp-deploy-account.json` 삭제(누출 검사 뒤).
- 스냅숏: 시작(18:11:52) = 끝(18:12:00) — 9표 + push_subscriptions + 마이그레이션 건수 · 내용 md5 **같음**(임시 계정 행 제외): issues 897 · articles 1,279 · collect_runs 102 · folders 2 · bookmarks 2 · user 2 · session 3 · account 1 · verification 0 · push_subscriptions 1 · 마이그레이션 8. 실제 사용자 폴더 id로 보낸 요청 0.
- 바깥: 감시기 기록 = `fetch issuerank-kappa.vercel.app` 14 · TCP 연결 배포 주소 2(재사용) · DB 1뿐. newsdata · OpenAI · Google · Kakao · 푸시 서비스 · GitHub API 0. 누출 검사: 결과 · 감시기 기록 · ps 파일 false.
- 프로세스: 띄운 것 = node 스크립트 하나(끝나며 종료) · 서버 · 브라우저 0. 끝 확인: ps에 j-deploy9 · next · 내 chromium · tsx · proot 0 · code-oss 30854 살아 있음.
- **줄 9 [OK] → 합계 OK 9 / FAIL 0 / SKIP 1 → PASS 보류**(남은 것 = 10 사용자 휴대폰 확인, 제작 보고 11절 문장). 커밋 · 푸시 0(이 절과 HANDOFF O6 줄은 작업 트리에만).
