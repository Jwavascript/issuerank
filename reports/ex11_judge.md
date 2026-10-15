# ex11 심사 보고 — 계정별 북마크·폴더

2026-10-15. 심사 역할로 `agent_prompts/3_judge_agent.md`에 따라 진행했다. 제품 코드는 수정하지 않았다. 기준은 `plan/PLAN_backend_ex11.md`, 제작 보고는 `reports/ex11_build.md`다.

진행 기록: 실제 API·DB·브라우저 검증은 이전 Codex 심사 세션이 `ex11/.data/verify-ownership-judge.ts`로 2026-10-15 07:41~07:44에 실행했고(결과 `ex11/.data/judge-ownership-verdict.json`), 보고서 작성 전에 세션이 끊겼다. 이어받은 Claude Code 세션에서 같은 심사의 나머지(코드·계약 대조, 테스트·빌드 재실행, 남은 임시 파일 정리, 보고)를 마쳤다. 제작 검증 결과(`build-*`)를 심사 결과로 대신 쓰지 않았다.

**판정: PASS — ex11 현행 검증표 11줄 중 OK 11 / FAIL 0 / SKIP 0.** ex08 AI 품질 FAIL과 ex09 자동화 보류는 그대로이며 이 판정과 별개다.

## 1. 계획 구현

| 계획 항목 | 판정 | 확인 |
|---|---|---|
| 인증 경계 | done | `server/app.ts`의 `/api/*` 미들웨어가 bookmarks·folders 경로에서 라우트(JSON 검사·Store)보다 먼저 `getSession` 확인, 실패 401 `로그인이 필요합니다`. userId는 세션에서만 `c.set`, 본문·쿼리 값은 쓰지 않음. 랭킹·인증 경로는 미들웨어 앞/밖 |
| 소유권 DB | done | `0006_ownership.sql`: 두 테이블 ACCESS EXCLUSIVE 잠금 → 빈 행 guard(P0001) → user_id NOT NULL·user FK(NO ACTION), `issue_id` 단독 unique를 `(user_id,issue_id)`로 교체, folders `(user_id,id)` unique를 복합FK보다 먼저 생성, bookmarks `(user_id,folder_id)` 복합FK. issues FK·기존 folder FK·RLS 유지. 0000~0005 파일 변경 없음 |
| 조회 | done | `server/db/store.ts`·`server/sample.ts` 개인 5함수 필수 userId. 북마크 `created_at↓, id↓`, 폴더 `created_at↑, id↑`, count JOIN에 소유자 조건. 응답에 userId 없음. Hono 개인 경로·Next BFF 개인 경로(502 포함) no-store |
| 북마크 저장·해제 | done | 본인 폴더만 201, 타인/없는 폴더 404, 같은 계정 중복 409(이동 아님), 다른 계정 같은 이슈 각각 저장, 삭제는 본인 행만 |
| 폴더 | done | trim 후 1~16자·같은 이름 허용·`f<randomUUID>`·count 0 시작. 기존 id 보존 |
| 무소유 기존행 | done | 잠금 안 빈 행 guard. 적용 직전 확인·이력 hash 대조는 제작 기록과 심사 실행 모두 통과 |
| 화면 세션 | done | `public/index.html`: 개인 API 401이면 `applyUser(null)`로 상태·목록·필터·선택창(closePop·closeDrawer) 초기화 후 로그인 안내. authVersion으로 늦은 조회·401·저장·해제·폴더 생성·되돌리기 차단. 개인 데이터 localStorage 사용 없음 |

## 2. SPEC 근거 AC

| AC | 판정 |
|---|---|
| AC-07-1 | 충족. 실제 화면에서 폴더 고르기·새 폴더 저장 후 버튼 활성, DB 저장 확인 |
| AC-07-2 | 충족. 해제 204·서버 삭제, 다른 계정 행 보존, 되돌리기 |
| AC-07-3 | 충족. 비로그인 안내만·저장 0, 서버 개인 5경로 401·Store 미호출 |
| AC-08-1 | 충족. 본인 북마크만 최근 저장 순, 카드 칸(요약·연관기사 수·링크) 유지 |
| AC-08-2 | 충족. 새 계정 빈 배열과 "북마크한 기사가 없습니다" 안내 |
| AC-08-3 | 충족(D10). 격리된 Chromium 컨텍스트 2개에서 같은 A 계정 목록 동일, 한쪽 B 전환 시 A 데이터 0·다른 쪽 A 유지 |
| AC-09-5 | 충족. 실제 DB 과거 수집일 2026-10-14 Top5 카드 저장 201, 재로그인 후 유지(API·UI 모두) |
| AC-10-1 | 충족. 선택창 폴더 2개의 개수가 본인 count와 일치, 기존 폴더 저장 |
| AC-10-2 | 충족. 생성 즉시 저장, 17자 400, 공백 trim·같은 이름 201 |
| AC-10-3 | 충족. 폴더 칩·전체 칩 렌더 카드 확인 |
| AC-10-4 | 충족. 사용자당 이슈 하나 unique, 다른 본인 폴더 재저장 409 |

## 3. 범위·보존

- 새 패키지 0: ex10과 ex11 lockfile 패키지 목록 동일, package.json은 이름만 다름.
- 출발점 ex10: `ex11/.data/ex10-baseline.json`의 67파일 SHA-256 재계산 변경 0.
- ex10 대비 변경 파일은 PLAN 범위 안(app·routes·store·schema·sample·BFF route·index.html·openapi·0006·테스트 2개 추가)이며 삭제 파일 없음.
- 참고: 북마크 화면의 날짜 헤더가 "날짜별 한 묶음"에서 "연속된 같은 날짜만 한 묶음"으로 바뀌었다. 날짜가 다른 북마크가 섞여 저장되면 같은 날짜 헤더가 두 번 나올 수 있으나, AC-08-1(최근 저장 순)을 지키기 위한 변경이며 제작 보고와 PLAN.md 10절에 기록돼 있다.
- 참고: `sample.ts`의 `latestDate` 줄 주석이 하나 지워졌다(동작 변화 없음).
- PLAN.md 6절 기존 화면 동작(이슈맵·카드·날짜 이동·로그인 안내·문구)은 테스트와 브라우저 확인에서 깨지지 않았다.

## 4. 테스트·빌드 (이번 세션 재실행)

- `bash scripts/ex11-ubuntu.sh node .data/safe-run.mjs npm test` → leak:false, **13파일 96/96 PASS**.
- `bash scripts/ex11-ubuntu.sh node .data/safe-run.mjs npm run build` → leak:false, **Next 16.4 Turbopack 컴파일·TypeScript 검사·정적 페이지 생성 성공**.

## 5. 현행 검증표 결과

실행: `bash scripts/ex11-ubuntu.sh node .data/safe-run.mjs npx tsx .data/verify-ownership-judge.ts` (심사용 임시 A/B 2계정, 요청 **80/250**, 뉴스 0·OpenAI 0, leak:false). 68개 세부 검사 모두 true.

| 확인 | 결과 | 증거(verdict 키) |
|---|---|---|
| 로컬 회귀·계약 | OK | 위 test/build, ex10 해시 불변, 0006·OpenAPI 개인 5경로 쿠키/401/no-store 검토, 신규 패키지 0 |
| 세션 경계(AC-07-3) | OK | `guest-*` 5경로 401, `publicRanking`; 변조·만료 쿠키·잘못된 본문보다 인증 우선·위조 userId·Store 미호출은 `ownership.test.ts` 실제 Better Auth 테스트 |
| DB 제약·적용 | OK | `migrationCount/HistoryHashes/Applied/RepeatUnchanged/PreservesRows`, `ownerNotNull`, `privateRls`, `crossOwnerFk23503`, `duplicateUnique23505`, `ownerRequired23502`(롤백), `migrationRepeatWithOwnedRows` |
| A/B 분리 | OK | `aSaves`(A 쿠키 + 본문에 B userId 위조 → A 소유로 저장), `bSameIssueSaves`, `aSecondSaves`, `sameUserDuplicate409`, `otherFolder404`(A가 B 폴더로 저장), `aListAndOrder`, `bListIsolation`, `aCounts`, `empty-*`, `cardFieldsPreserved` |
| 삭제·경합 | OK | `aDelete204`, `bSurvivesADelete`, `aRepeatDelete404`(B만 가진 이슈), `sameUserRace` [201,409], `folderUuidRace`(같은 이름 둘 201·다른 id), `folderLength400` |
| 순서·계정응답 회귀 | OK | `dbTieOrder`, `account-ui.test.ts` 6개(늦은 목록/401/저장/해제/폴더 생성/되돌리기, 최근 순) |
| 실제 BFF·재시작 | OK | `restartPreservesBookmarks`, `sameAccountOtherSession`, `privateNoStore` |
| UI 폴더·북마크 | OK | `guestUiPrompt`, `newAccountUiEmpty`, `emptyUi`, `uiCreateAndSave`, `uiRecentFirst`, `uiFolderFilter`, `uiAllFilter`, `uiUndo`, `uiPickerCounts`, `uiExistingFolderSave`, `uiReload` |
| UI 두 세션 | OK | `uiSameAccountSecondBrowser`, `uiBHasNoAData`, `uiAContextUnaffected`, `uiExpiredSessionClears` |
| 지난날 저장 | OK | `existingPastDate`(2026-10-14), `pastRanking200`, `pastSave`, `pastDateAfterRelogin`, `uiPastDateSaved` |
| 정리·보존 | OK | `temporaryUsersRemoved`, `originalRowsPreserved`(기존 테이블 행 수·내용 hash 동일), `ownedServersStopped`, `requestBudget`. 남아 있던 심사용 Chromium 프로필(`$PREFIX/tmp/ex11-judge-chromium-*`)은 이번 세션에서 사용 프로세스 없음 확인 후 삭제, 3000·8787 포트 비어 있음 |

합계: **OK 11 / FAIL 0 / SKIP 0**.

## 6. 지켜야 할 것

- 기사 본문 저장·표시 없음(카드 칸 변경 없음).
- 코드에 키·접속 정보 없음. `ex10/.env`·`ex11/.env`는 `.gitignore` 4행으로 제외. 모든 실행은 safe-run 누출 검사 leak:false.
- PLAN 밖 패키지·D0 밖 출처·뉴스/OpenAI 호출 없음. 샘플을 실제 DB에 넣지 않음(fixture 사용자는 메모리 저장소만).
- AGENTS.md 위반 없음: 제품 코드 미수정, web/ 미접근, 사용자 서버 미종료.

## 참고(판정 외)

- 심사 스크립트는 제작 스크립트(`verify-ownership.ts`)를 바탕으로 만들고 선택창 개수·기존 폴더 저장·소유 행이 있을 때 재마이그레이션 검사를 더했다. 독립성을 더 높이려면 다음 단계에서는 심사 스크립트를 제작 스크립트와 따로 작성하는 것이 좋다.
- OpenAPI는 YAML 파서 없이 정적 검토만 했다(ex01 외 단계는 심사 기준상 필수 아님).
