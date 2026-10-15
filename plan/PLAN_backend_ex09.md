# PLAN_backend_ex09 — 하루 1회 자동 실행

> 이유: 하루 실행(collect → group → summarize)은 지금 사람이 손으로 세 명령을 돌려야 하고, 빠진 날은 나중에 채울 수 없다(R5). 그래서 `npm run daily` 한 명령으로 묶고, GitHub Actions 예약 실행이 매일 06:17 KST(GitHub 정각 부하 회피)에 돌린다(D5 · D3 확정). 06:17 수집분은 newsdata 약 12시간 지연 · 섹션당 약 20건이라 대략 전날 10~18시 낮 기사다.
> 코드 폴더: `ex09/` (`ex11/` 복사 — D17로 "ex(XX-1)" 규칙의 예외. 로그인·계정별 저장 포함). ex11·ex08은 보존.
> SPEC 근거: AC-01-1(US-01). AC-01-2는 회귀로 확인. D3 · D5 · D12 · D13 · D17 · D18, R6 · R7 적용.

## 이번 범위
- 새 명령 `npm run daily`(세 단계를 순서대로, 멈춤 규칙 · 종료 코드) + 저장소 루트 `.github/workflows/daily.yml`(예약 · 수동 실행). 수집 · 묶기 · 요약 · AI(`server/ai.ts`, gpt-6-luna, 배정 effort medium · 메타/요약 low) · API · 계약 · 화면 · 인증 · 소유권은 ex11 그대로(한 글자도 안 바꿈).
- 새 패키지: 없음(Node 내장 `child_process`). 워크플로는 GitHub 공식 액션 `actions/checkout` · `actions/setup-node`만(npm 패키지 아님, 제작 때 공식 문서의 현재 주 버전 태그 사용).
- 하지 않음: 실패 자동 재시도 · 알림 메일/메신저 직접 구현 · 지난 날짜 채우기(backfill) · 같은 날 이미 성공했는지 보고 건너뛰기 · AI 설정 · 모델 · 지시문 변경 · 품질 판정 자동화 · 배포(ex12) · Google OAuth · `web/`.

## 하루 실행 규칙
| 항목 | 규칙 |
|---|---|
| 명령 | `npm run daily [-- --pages N] [--search N] [--max N]` = `tsx --env-file-if-exists=.env server/daily.ts`. 옵션은 그 단계에만 그대로 넘김(`--pages`→collect · `--search`→group · `--max`→summarize, 값 검사는 각 명령의 기존 문구). 이 셋 말고 다른 인수가 있으면 단계 실행 없이 `--pages · --search · --max만 받습니다` · 종료 2. 옵션이 없으면 각 명령의 기본값(collect top1+전체2페이지 · group 검색 6 · summarize 30). `--ai`는 넘기지 않음(늘 AI, 실패하면 기존 규칙 대체) |
| 실행 방식 | 각 단계를 자식 프로세스 `tsx server/{collect\|group\|summarize}.ts`로 돌리고 출력은 그대로 이어 받음(기존 명령 · 문구 · 종료 코드 불변). 단계 앞에 `== {단계} ==` 한 줄. 순서 · 멈춤 · 종료 코드 결정은 순수 함수 `server/steps.ts`(단계 실행 함수를 받음, `process.env` · DB · 네트워크 안 읽음), `server/daily.ts`는 명령만 |
| 멈춤 | collect 종료 0 · 1(일부 섹션 실패, AC-01-2) → group 진행 / collect 2(모두 실패 · 키 없음 · DB 오류) → group · summarize 건너뜀. group 0 · 1(검색 실패 · AI 정리 실패 후 규칙 대체) → summarize 진행 / group 2 → summarize 건너뜀. 자식 프로세스를 못 띄우면 그 단계 2 · `실행 오류: {단계}`(오류 객체 · 환경 변수 출력 금지, `hide` 적용) |
| 끝 줄 · 종료 코드 | `하루 실행 끝 · collect {c} · group {g} · summarize {s}`(건너뛴 단계는 `건너뜀`). 종료 코드 = 실행한 단계 코드의 최댓값(0 모두 정상 · 1 일부 실패 · 2 멈춤). 0이 아니면 Actions 실행이 실패(빨강)로 표시됨 |
| 날짜 · 재실행 | 날짜는 각 명령의 KST 오늘(기존). 같은 날 다시 돌리면 기존 규칙대로 덮어씀: 성공 섹션 기사만 교체 · 이슈 id는 대표 기사 링크로 유지 · 요약은 null인 Top 5만 · 북마크 있는 섹션은 묶기 건너뜀. 실행 1번 = newsdata 최대 54크레딧(수집 18 + 검색 36, R6) · OpenAI 최대 32회(정리 2 + 요약 30). 운영 규칙(D5): 자동 재시도 없음, 같은 날 수동 재실행(`Run workflow` · Re-run)은 최대 2회 → 자동 1 + 수동 2 = newsdata 162크레딧 이하. 코드로 막지 않음("이미 성공하면 건너뛰기" 없음) |
| 워크플로 | `.github/workflows/daily.yml`: `on.schedule` cron `17 21 * * *`(UTC = 06:17 KST, D3) + `workflow_dispatch`(수동 재실행, 입력 없음). `concurrency: { group: daily, cancel-in-progress: false }`(겹쳐 돌지 않음). `permissions: contents: read`. `runs-on: ubuntu-latest` · `timeout-minutes: 30`. 단계: checkout → setup-node(Node 24, npm 캐시 `ex09/package-lock.json`) → `ex09`에서 `npm ci --no-audit --no-fund` → `npm run daily`. `pull_request` · `push` 트리거 없음, `continue-on-error` 없음 |
| 비밀값 | GitHub Secrets 3개 `DATABASE_URL`(Supabase 풀러 주소, IPv4 — R7) · `NEWSDATA_API_KEY` · `OPENAI_API_KEY`를 `npm run daily` 단계의 `env`에만. `BETTER_AUTH_*`는 하루 실행에 쓰지 않아 등록 안 함. `.env` 파일을 만들지 않고 비밀값을 `echo`하지 않음. 로그는 GitHub 마스킹 + 기존 `hide` |
| 실행 기록 | ① Actions 실행 기록(시작 시각 · 결과 · 로그) ② `npm run daily` 출력 전체를 같은 단계에서 `$GITHUB_STEP_SUMMARY`(실행 요약 화면)에 코드 블록으로 붙임 — `shell: bash`의 pipefail로 `tee` 뒤에도 daily 종료 코드 유지, 실패해도 붙임 ③ DB `collect_runs`(기존). 품질 확인에 쓰는 줄 = group 둘째 줄(`AI 정리 · 묶음 · 제외 · 빠진 기사 · 고침` 또는 `AI 정리 실패 …`) · 섹션별 후보 줄(AI 검색어 · 건수) · summarize 줄. 기사 제목 · 본문 · 비밀값은 출력에 새로 넣지 않음 |

- GitHub 쪽 사실(제작 때 공식 문서로 다시 확인): 예약 실행은 기본 브랜치(main)의 워크플로만 돌고, 부하가 크면 늦어지거나 빠질 수 있음(06:17보다 늦게 시작해도 KST 같은 날이라 날짜는 같음). 공개 저장소는 60일 동안 활동이 없으면 예약 실행이 꺼짐. 예약 실행 실패 알림은 cron을 마지막으로 바꾼 사용자에게 감.
- 품질 확인: 같은 116기사 표본으로 고른 설정(PLAN.md 10절 ex08 메모)이라, 실행 요약에 더해 **처음 7일은 `reports/ex09_daily.md`에 날짜별로 수동 기록**(D5). 표 머리 `| 날짜(KST) | 실행(로컬/수동/예약 · run id) | 결과 끝 줄 | AI 정리(묶음 · 제외 · 빠진 기사 · 고침 / 실패 문구) | 섹션별 첫 후보 한 사건?(pol~it, 아니면 이유) | 메모 |`. 쓰는 사람: 제작 = `실제 하루(로컬)` 1회분(파일을 만듦) · 심사 = 수동 · 예약 실행분 · 그 뒤 7일째까지 = 사용자 또는 다음 세션. 이상(전부 단독 · 대량 제외 · `AI 정리 실패` 연속 · 오병합)은 PLAN.md 10절에도 한 줄. 자동 판정은 하지 않음.
- 저장소 공개 여부 · 발표일은 미정(이번 PLAN에서 정하지 않음).

## 화면 변경
- 없음(`public/index.html` · BFF · API 그대로. 매일 새 날짜가 쌓여 날짜 이동 · 갱신 시각이 실제 실행 시각을 보임).

## 구현 순서
1. `ex11/` → `ex09/` 복사(node_modules · .next · dist · .data · .vitest · tsconfig.tsbuildinfo · next-env.d.ts 제외), `ex09/.env`는 ex11의 값을 비공개 복사(출력 금지). `scripts/ex09-ubuntu.sh` 준비(ex11 래퍼에서 작업 폴더만 `ex09`). `bash scripts/ex09-ubuntu.sh npm ci --no-audit --no-fund`.
2. `server/steps.ts` · `server/daily.ts` · `package.json`의 `daily` 스크립트 · `test/daily.test.ts`(가짜 단계 실행 함수, 네트워크 · DB 없음).
3. 저장소 루트 `.github/workflows/daily.yml`. 로컬 검증 → 아래 실제 줄(제작 한도 안, 사용자 허용) → `reports/ex09_daily.md` 첫 줄 → 보고에 사용자가 할 일(푸시 · Secrets 등록 · 수동 실행 1회)을 적음. 푸시 · Secrets 등록은 에이전트가 하지 않음.
마지막. `bash scripts/ex09-ubuntu.sh npm test` · `bash scripts/ex09-ubuntu.sh npm run build`.

## 검증 방법
- 실행 위치: 현재 Termux는 저장소 루트에서 `bash scripts/ex09-ubuntu.sh …`(작업 폴더 ex09). 다른 PC는 `ex09/`에서 npm 명령. 서버(8787 · 3000)는 이번 검증에 필요 없음 — 띄웠다면 소유 서버만 종료.
- 안전 실행 = ex08 · ex11 방식 그대로: `ex09/`에서 `node --env-file=.env -e`로 `child_process.spawnSync(명령, { shell: true, encoding: 'utf8', env })`(env는 그 줄에 적은 값만 바꿈)를 돌려 종료 코드와 "stdout + stderr에 `.env`의 `DATABASE_URL`(전체 · 비밀번호 · 디코딩한 비밀번호) · `NEWSDATA_API_KEY` · `OPENAI_API_KEY` · `BETTER_AUTH_SECRET` 값이나 그 줄의 가짜 값이 있는가"(누출 true/false)를 먼저 찍고, 모두 false일 때만 출력을 찍음. SQL은 같은 방식으로 `postgres`(`prepare: false`), 클라이언트 생성도 try 안 · 안전한 오류 코드만. 크레딧 확인 = ex08 방식(politics top 1요청, 상태 코드와 `x-api-limit-remaining`만).
- 호출 한도: **제작** newsdata 최대 20크레딧(크레딧 확인 2 + `실제 하루(로컬)` 수집 12 + 검색 최대 6) · OpenAI 최대 5회(정리 2 + 요약 3). **사용자 수동 실행**(Actions `Run workflow` 1회, 운영 실행) newsdata 최대 54 · OpenAI 최대 32. **심사** newsdata 최대 1(크레딧 확인) · OpenAI 0 — 실제 실행은 Actions 기록과 DB 읽기로만 봄. 잘못된 키(401)는 크레딧 · 과금 없음(횟수는 적음). 실패해도 다시 돌리지 않고 원인을 보고. 시작 전 잔여 크레딧을 새로 확인하고 과거 값(181)을 현재 잔액으로 보지 않음.
- DB 확인 행: 이번에 새로 생기는 확인용 행은 `잘못된 키(로컬)`의 `collect_runs` `id > M` failed 행뿐 — 그 줄 끝에서 지움. `실제 하루(로컬)` · 수동 · 예약 실행이 만든 기사 · 이슈 · 요약 · ok 수집 기록은 실제 데이터라 남김. 샘플 행 삽입 금지(D14). 사용자 · 세션 · 북마크 · 폴더는 읽기만(내용 해시 전후 비교).

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 출발점 · 변경 범위 | `git status --short ex11/ ex08/` · `git diff --no-index --stat ex11 ex09`(산출물 · .env 제외) · `sha256sum ex08/server/ai.ts ex11/server/ai.ts ex09/server/ai.ts` | ex11 · ex08 변경 없음. 차이는 `package.json`(daily 한 줄) · `server/daily.ts` · `server/steps.ts` · `test/daily.test.ts`뿐. ai.ts 셋 다 `07e10b1e…` |
| 로컬 회귀 · 빌드 | `bash scripts/ex09-ubuntu.sh npm test` · `npm run build` | ex11 기존 99개 + 새 daily 테스트 모두 통과 · 빌드/타입 성공 · 새 패키지 0(lockfile 차이 없음) |
| 순서 · 멈춤(테스트) | `test/daily.test.ts`, 가짜 단계 코드 조합: (0,0,0) · (1,0,0) · (2,–,–) · (0,2,–) · (0,1,1) · 단계 실행 함수가 던짐 | 차례로 3단계·종료0 / 3단계·1(AC-01-2: 일부 실패여도 묶기 · 요약 진행) / collect만·2·나머지 `건너뜀` / summarize 건너뜀·2 / 3단계·1 / `실행 오류: {단계}`·2·가짜 비밀값 출력 0. 끝 줄 형식이 위 표와 같음 |
| 옵션 넘김(테스트) | `--pages 1 --search 1 --max 3` · 옵션 없음 · `--ai 0` · `--x 1` | 각 단계가 자기 옵션만 받음 · 옵션 없이 기본값 · 마지막 둘은 단계 실행 0번 · `--pages · --search · --max만 받습니다` · 2 |
| 키 없음(실제 명령) | `.env`를 안 읽게 키 변수를 지운 env로 `npx tsx server/daily.ts` · `… --pages x` | 둘 다 종료 2 · `NEWSDATA_API_KEY가 없습니다` / `--pages는 0~2입니다` · group · summarize `건너뜀` · 외부 요청 0 |
| DB 오류 · 누출 | 안전 실행, 자식 env `NEWSDATA_API_KEY=wrong-key-0000` · `OPENAI_API_KEY=wrong-ant-0000` · `DATABASE_URL=postgres://u:SECRETPW@127.0.0.1:9/x`로 `npm run daily` | 누출 false(`SECRETPW` 포함) · 종료 2 · `DB 오류: ` 줄 · 섹션 요청 줄 없음 · group · summarize `건너뜀` |
| 잘못된 키(로컬, 실제 DB) | SQL로 `collect_runs` 최대 id M · 오늘 섹션별 기사 수 · 오늘 이슈 (id, url, article_count, summary) 목록 해시 → 안전 실행(자식 env 두 키 `wrong-…`) `npm run daily -- --pages 1` → SQL → `id > M` 행 삭제 | 누출 false · 종료 2 · 6섹션 `failed HTTP 401` · `요청 6번` · group · summarize `건너뜀`(오늘 AI 묶음 보존) → `id > M` failed 6행 · 기사 수 · 이슈 해시 불변(AC-01-2) → 지운 뒤 0행 |
| 실제 하루(로컬, 제작만) | 크레딧 확인(R0) → 안전 실행 `npm run daily -- --pages 1 --search 1 --max 3` → SQL: 오늘 마지막 `collect_runs` 6행 · 섹션별 기사 수 · 이슈 수 · 요약 있는 이슈 수 → 크레딧 확인 | 누출 false · 종료 0(1이면 실패 줄 원인 보고) · `== collect ==` 6섹션 ok · `== group ==` 둘째 줄 `AI 정리 · …`(실패면 그 문구 기록) · `== summarize ==` 요청 ≤ 3 · 끝 줄 `collect 0 · group 0 · summarize 0` → 6행 같은 `finished_at`, 이슈 수 = group `이슈 {n}`, 요약 수 증가 ≤ 3 → 잔여 = R0 − (12 + 검색 수) − 1(다르면 차이를 숫자로 적음) |
| 워크플로 파일 | `daily.yml` 읽고 표의 항목 대조 · `grep -n` cron · `workflow_dispatch` · `concurrency` · `permissions` · `working-directory` · `secrets.` | cron `17 21 * * *` · 수동 실행 · 겹침 방지 · contents read · timeout 30 · 작업 폴더 ex09 · Node 24 · `npm ci` · secrets 3개 이름만 daily 단계 env에 · `echo`/`.env` 생성 · push/PR 트리거 · `continue-on-error` 0건 · 실행 요약에 출력 붙이는 단계 있음 |
| 등록 · 수동 실행(사용자 → 확인) | 사용자: main 푸시 → Settings › Secrets and variables › Actions에 3개 등록 → Actions › daily › Run workflow 1회. 확인: `gh run list --workflow daily.yml` · `gh run view {id} --log`(인증 없으면 사용자가 결과 화면 전달) · `gh secret list`(이름만) | 워크플로 인식 · 이벤트 `workflow_dispatch` · 결과 success · 로그에 비밀값 0(`***`만) · 실행 요약에 daily 출력과 끝 줄 · Secrets 이름 3개(값은 보지 않음). DB(읽기): 그 실행 시각의 ok `collect_runs` 6행 |
| 예약 실행(AC-01-1, 심사) | 다음 날 06:17 KST 이후 `gh run list --workflow daily.yml --event schedule` · `gh run view` → SQL(읽기): 그 날짜 `collect_runs` · 섹션별 기사 수 · 이슈 수 · 요약 수 · `GET /api/rankings` 기본 응답(API를 띄웠다면) | 이벤트 `schedule` · 시작 21:17 UTC 이후(지연 분 수를 적음) · success · 그 날짜 6섹션 ok 행의 `finished_at`이 실행 시간 안 · 섹션마다 기사 > 0 · 이슈 > 0 · 요약 ≥ 1 · 기본 랭킹 `date` = 그 날짜. 아직 예약 실행이 없으면 SKIP(PASS 아님) |
| 품질 기록(사람) | 각 실행(제작: 로컬 1회 · 심사: 수동 · 예약)의 출력/실행 요약에서 group 둘째 줄 · 섹션별 첫 후보, SQL(읽기)로 그 대표 제목 · 묶인 제목 확인 → `reports/ex09_daily.md`에 한 줄 | 파일이 위 표 머리 그대로 · 그 실행 날짜 줄이 있고 칸이 모두 채워짐(묶음 · 제외 · 빠진 기사 · 고침, AI 정리 실패 여부, 6섹션 첫 후보 판정). 품질 자체는 PASS 조건 아님, 이상은 PLAN.md 10절로 |
| 정리 · 보존 | 이번 확인 행 · 사용자/북마크/폴더 내용 해시 · 지난 날짜 기사/이슈 수 · ex11 소스 · 소유 프로세스 · 호출 카운터 | 확인 행 0 · 해시 불변 · 지난 날짜 불변 · 실제 실행 데이터 남음 · 한도 안 · 남은 프로세스 0 |

### 앞 단계 검증표 보존
- ex11 · ex10 · ex08 검증표는 회귀 기준이다. 이번에 API · 화면 · 인증 · 묶기 · 요약 코드는 바뀌지 않으므로(`출발점 · 변경 범위` 줄) 그 표의 브라우저 · BFF · 실제 API 줄은 다시 실행하지 않고 `로컬 회귀 · 빌드`의 기존 테스트로 대신한다. 소스가 바뀌었으면 해당 표의 줄을 ex09에서 다시 실행한다.
