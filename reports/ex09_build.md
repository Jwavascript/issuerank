# ex09 제작 보고 — 하루 1회 자동 실행

- 날짜: 2026-10-15 (KST) · 역할: 제작(agent_prompts/2_build_agent.md) · 실행: Claude Code 세션(Android/Termux + proot Ubuntu, Node 24.18.0)
- 기준: `plan/PLAN_backend_ex09.md`, PLAN.md 5절 D3 · D5 · D17 · D18 (사용자 확정 2026-10-15)
- 결론: 로컬 테스트 111/111 · 빌드 성공 · 실제 하루(로컬) 1회 종료 0. GitHub 등록 · 수동 실행 · 예약 실행은 사용자 단계라 아직 없음(아래 6절).

## 1. 작업 범위

- 출발점 `ex11/`(D17 예외) → `ex09/` 복사. 제외: node_modules · .next · dist · .data · .vitest · tsconfig.tsbuildinfo · next-env.d.ts · .env. `ex09/.env`는 `cp ex11/.env ex09/.env`로 비공개 복사(내용 출력 · 읽기 없음, `cmp`로 같음만 확인).
- `scripts/ex09-ubuntu.sh`: ex11 래퍼에서 설명 2줄과 작업 폴더만 `ex09`로 바꿈.
- `bash scripts/ex09-ubuntu.sh npm ci --no-audit --no-fund` → 199 패키지 설치. npm 11 `allow-scripts` 경고(esbuild postinstall 3개)는 나왔지만 `node -e "require('esbuild').transform('let a: number = 1',{loader:'ts'})"` → `"let a = 1;\n"`로 실제 변환 확인.
- 수집 · 묶기 · 요약 · AI · API · 계약 · 화면 · 인증 · 소유권 코드는 ex11 그대로(한 글자도 안 바꿈).

## 2. PLAN 항목별 반영 위치

| PLAN 항목 | 반영 위치 |
|---|---|
| `npm run daily` 명령 | `ex09/package.json` `"daily": "tsx --env-file-if-exists=.env server/daily.ts"` (그 한 줄 + 앞 줄 쉼표) |
| 순서 · 멈춤 · 종료 코드(순수 함수) | `ex09/server/steps.ts` — `splitArgs`(`--pages`→collect · `--search`→group · `--max`→summarize, 다른 인수면 null), `runDaily(args, run, log)`(단계 앞 `== {단계} ==`, 단계 코드 2면 남은 단계 건너뜀, 던지면 `실행 오류: {단계}` · 2 · 오류 객체 안 찍음, 끝 줄 `하루 실행 끝 · collect {c} · group {g} · summarize {s}`/`건너뜀`, 종료 코드 = 실행한 단계 코드 최댓값). `process.env` · DB · 네트워크를 읽지 않음 |
| 자식 프로세스 실행 | `ex09/server/daily.ts` — `spawn(process.execPath, ['--import','tsx','server/{단계}.ts', ...인수], { stdio: 'inherit' })`(ex11 `test/auth.test.ts`와 같은 tsx 실행 방식, `tsx server/…`와 같은 동작이며 Windows에서도 PATH · .cmd 문제 없음). 자식은 부모 env(.env 값 포함)를 그대로 받고 출력 · 종료 코드는 기존 명령 그대로. `error` 이벤트면 던져서 `실행 오류`. 부모 출력 줄은 `hide` 적용. `--ai`는 넘기지 않음(받으면 거절) |
| 테스트 | `ex09/test/daily.test.ts` 12개 — (0,0,0) · (1,0,0) · (2,–,–) · (0,2,–) · (0,1,1) · 던짐(가짜 비밀값 출력 0) · 옵션 넘김 · 옵션 없음 · `--pages x` 그대로 넘김 · `--ai 0` · `--x 1` · `--pages 1 --ai 1` |
| 워크플로 | 저장소 루트 `.github/workflows/daily.yml` — cron `17 21 * * *` + `workflow_dispatch`(입력 없음) · `concurrency { group: daily, cancel-in-progress: false }` · `permissions: contents: read` · `ubuntu-latest` · `timeout-minutes: 30` · `defaults.run.working-directory: ex09` · `actions/checkout@v7` → `actions/setup-node@v7`(node 24, `cache: npm`, `cache-dependency-path: ex09/package-lock.json`) → `npm ci --no-audit --no-fund` → `npm run daily`(secrets 3개는 이 단계 `env`에만). 같은 단계에서 `set +e` → `npm run daily 2>&1 \| tee "$RUNNER_TEMP/daily.log"` → `code=$?`(pipefail로 daily 코드) → 출력을 코드 블록으로 `$GITHUB_STEP_SUMMARY`에 붙임 → `exit "$code"`. push · pull_request 트리거 · `continue-on-error` · `echo` · `.env` 생성 없음 |
| 품질 기록 첫 줄 | `reports/ex09_daily.md`(표 머리 PLAN 그대로, 2026-10-15 로컬 1줄) |

### 액션 주 버전 확인 근거(2026-10-15 조회)
- https://github.com/actions/checkout — README Usage `actions/checkout@v7`, https://github.com/actions/checkout/releases/latest = v7.0.1
- https://github.com/actions/setup-node — README Usage `actions/setup-node@v7`("What's new in V7"), https://github.com/actions/setup-node/releases/latest = v7.1.0. `cache-dependency-path`는 하위 폴더 lockfile 지정용(같은 README)
- GitHub 쪽 사실: https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#schedule — "Scheduled workflows run on the latest commit on the default branch", 부하가 크면 지연 · 일부 drop 가능, 공개 저장소는 60일 활동 없으면 예약 실행 자동 비활성, 알림은 cron을 마지막으로 바꾼 사용자에게. PLAN의 설명과 같음.

## 3. 지운 것

- 제품 코드 · 파일 삭제 없음.
- DB: `잘못된 키(로컬)` 줄이 만든 `collect_runs` failed 6행(id 143~148, `id > 136`, 오늘 날짜 · failed 조건)만 그 줄 끝에서 삭제 → `id > 136` 0행, 스냅숏이 실행 전과 완전히 같음(`diff` 0).

## 4. 필요했지만 없던 키/계정

- 로컬 실행에 필요한 키(`DATABASE_URL` · `NEWSDATA_API_KEY` · `OPENAI_API_KEY`)는 ex11/.env 복사본에 있었음(값 미확인 · 미출력).
- 없음(이번 범위에서 막힌 것): GitHub 저장소 Secrets 등록 · Actions 실행 권한 · `gh` 인증은 사용자 단계라 시도하지 않음 → 수동 실행 · 예약 실행 줄은 **SKIP(사용자 단계 대기)**.

## 5. 테스트 · 검증 결과

모든 명령은 저장소 루트에서 `bash scripts/ex09-ubuntu.sh …`. 실제 서비스 줄은 `ex09/.data/safe-run.mjs`(ex11 방식 + 줄별 덮어쓰기가 .env보다 이김, 출력에 .env 비밀값 · 비밀번호(디코딩 포함) · 덮어쓴 가짜 값이 있으면 막음)로 실행. SQL은 `ex09/.data/db-check.mjs`(`postgres` `prepare:false`, 클라이언트 생성 try 안, 오류는 코드만, 내용은 해시 · 건수만).

| 확인 | 결과 | 판정 |
|---|---|---|
| 출발점 · 변경 범위 | `git status --short ex11/ ex08/` 0줄. `diff -rq`(산출물 · .env 제외) ex11↔ex09 차이 = `package.json`(+daily 1줄, `git diff --no-index --stat` 2+/1−) · `server/daily.ts` · `server/steps.ts` · `test/daily.test.ts`뿐. `package-lock.json` 동일(`cmp`). ai.ts sha256 ex08 · ex11 · ex09 모두 `07e10b1e7746732594bf904c6b17f74084f526d9486e9334336d3bca3a159134` | OK |
| 로컬 회귀 · 빌드 | `npm test` **14파일 111/111 통과**(ex11 99 + daily 12). 첫 실행에서는 기존 `auth.test.ts` "진입점의 누락 설정…" 1개가 `STARTUP_TEST_TIMEOUT`(15초) — 새로 설치한 ex09의 tsx 첫 변환이 느려 `server/index.ts` 기동이 16.4초 걸림(재실행 8.1초, ex11은 4.2초). 코드 차이 없음, 캐시가 생긴 뒤 전체 재실행 111/111. `npm run build` 종료 0(Next 16.4.0 Turbopack 컴파일 · TypeScript · `tsc --noEmit`). 새 패키지 0 | OK(첫 실행 시간 초과는 환경 요인으로 기록) |
| 순서 · 멈춤 · 옵션(테스트) | 위 12개 모두 통과 | OK |
| 키 없음(실제 명령) | `env -u DATABASE_URL -u NEWSDATA_API_KEY -u OPENAI_API_KEY -u BETTER_AUTH_SECRET npx tsx server/daily.ts` → `== collect ==` · `NEWSDATA_API_KEY가 없습니다` · 끝 줄 `collect 2 · group 건너뜀 · summarize 건너뜀` · 종료 2. `… --pages x` → `--pages는 0~2입니다` · 같은 끝 줄 · 2. (추가) `--ai 0` → `--pages · --search · --max만 받습니다` · 2. 외부 요청 0 | OK |
| DB 오류 · 누출 | 안전 실행 `NEWSDATA_API_KEY=wrong-key-0000 OPENAI_API_KEY=wrong-ant-0000 DATABASE_URL=postgres://u:SECRETPW@127.0.0.1:9/x -- npm run daily` → `leak:false` · 종료 2 · `DB 오류: connect ECONNREFUSED 127.0.0.1:9` · 섹션 요청 줄 없음 · group/summarize `건너뜀` | OK |
| 잘못된 키(로컬, 실제 DB) | 전: M=136, 오늘 기사 cul19 · eco17 · it19 · pol19 · soc20 · wor19, 이슈 88(요약 3) 해시 `d23f200e…`. 안전 실행(두 키 `wrong-…`) `npm run daily -- --pages 1` → `leak:false` · 종료 2 · 6섹션 `failed 1페이지: HTTP 401 …` · `요청 6번` · group/summarize `건너뜀`. 후: `id > 136` failed 6행(143~148), 기사 수 · 이슈 해시 불변 → 삭제 6행 → 0행, 스냅숏 전체가 실행 전과 동일 | OK |
| 실제 하루(로컬, 제작) | 크레딧 R0 = **180**(200 응답). 안전 실행 `npm run daily -- --pages 1 --search 1 --max 3`(13:18:31~13:20:53 KST) → `leak:false` · 종료 0 · collect 6섹션 ok(pol20 · eco19 · soc20 · cul19 · wor13 · it17 = 108건, 요청 12, 남은 168) · group 둘째 줄 `AI 정리 · 묶음 92 · 제외 8 · 빠진 기사 0` · `이슈 92 · 검색 6번 · 실패 0 · 남은 크레딧 162` · summarize `요청 3번 · 성공 3` · 끝 줄 `하루 실행 끝 · collect 0 · group 0 · summarize 0`. SQL: 마지막 collect_runs 6행 모두 ok · 같은 `finished_at`(04:19:01.271Z), 섹션 기사 수 = collect 줄, 오늘 이슈 92 = group `이슈 92`, 요약 있는 이슈 3(pol-22 · 23 · 24, 각 3줄) → 크레딧 확인 **161** = 180 − (12 + 6) − 1(차이 0) | OK |
| 워크플로 파일 | `grep -n` 결과: cron `17 21 * * *`(8행) · `workflow_dispatch`(9) · `concurrency`(11) · `permissions`(15) · `timeout-minutes: 30`(21) · `working-directory: ex09`(24) · `node-version: 24`(29) · `npm ci`(32) · `secrets.` 3개(36~38, daily 단계 env만) · `GITHUB_STEP_SUMMARY`(44). `echo` · `.env` · push · pull_request · `continue-on-error` 0건. 요약 단계는 `bash --noprofile --norc -eo pipefail`(Actions의 bash 기본)로 가짜 실패 명령을 넣어 모의 실행 → 종료 1 유지 · 요약 파일에 코드 블록으로 붙음 확인. YAML 파서는 이 기기에 없어(python yaml · node yaml 없음, 새 패키지 금지) 구문 검사는 눈 검토만 | OK(구문 자동 검사는 SKIP) |
| 등록 · 수동 실행 | 사용자 단계(푸시 · Secrets · Run workflow) 전 | SKIP(사용자 대기) |
| 예약 실행(AC-01-1) | 아직 저장소에 워크플로가 없음 | SKIP(심사 · 다음 날) |
| 품질 기록 | `reports/ex09_daily.md` 생성, 표 머리 PLAN 그대로, 2026-10-15 로컬 줄 칸 모두 채움 | OK |
| 정리 · 보존 | 확인 행 0. 사용자 · 세션 · 북마크 · 폴더 내용 해시 전후 동일(현재 모두 0행). 지난 날짜(기사 285 · 이슈 102 · 수집기록 54)와 지난 이슈 해시 `48968836…` 불변. 실제 실행 데이터(오늘 기사 108 · 이슈 92 · 요약 3 · ok 6행) 남김. 띄운 서버 · 남은 프로세스 없음 | OK |

### 호출 횟수(제작 한도 newsdata 20 · OpenAI 5)
- newsdata 실제 크레딧: 크레딧 확인 2 + 수집 12 + 검색 6 = **18 / 20**. 잘못된 키 401 요청 6번(크레딧 0, R0 확인 전 실행).
- OpenAI: 정리 2(배정 · 메타데이터) + 요약 3 = **5 / 5**. 잘못된 키 OpenAI 호출 0(collect 2에서 멈춤).
- **남은 newsdata 크레딧: 161**(2026-10-15 13:21 KST 확인).

## 6. 사용자가 할 일(에이전트는 하지 않음)

1. 지금 변경을 커밋하고 GitHub 저장소 기본 브랜치 `main`에 푸시(`.github/workflows/daily.yml`이 main에 있어야 예약 실행이 돎). 커밋 대상: `ex09/`(.env · .data는 .gitignore로 빠짐), `.github/workflows/daily.yml`, `scripts/ex09-ubuntu.sh`, `reports/ex09_build.md` · `reports/ex09_daily.md`, `plan/PLAN_backend_ex09.md`, `PLAN.md`, `HANDOFF.md`.
2. GitHub 저장소 › Settings › Secrets and variables › Actions › New repository secret으로 3개 등록: `DATABASE_URL`(Supabase **풀러** 주소, IPv4 — 지금 .env와 같은 값) · `NEWSDATA_API_KEY` · `OPENAI_API_KEY`. `BETTER_AUTH_*`는 등록하지 않음.
3. Actions 탭 › `daily` › Run workflow(브랜치 main) **1회**. 운영 실행이라 newsdata 최대 54 · OpenAI 최대 32. 오늘(10-15)은 이미 로컬로 1회 돌았고 남은 크레딧이 161이라 충분. 같은 날 수동 재실행은 최대 2회(D5).
4. 끝나면 결과 화면(성공/실패, 실행 요약의 daily 출력)을 심사 세션에 전달하거나 `gh` 인증 후 심사가 `gh run list --workflow daily.yml`로 확인.
- 저장소를 공개로 두면 60일 동안 활동이 없을 때 예약 실행이 꺼짐. 예약 실행 실패 알림은 cron을 마지막으로 바꾼(=이 파일을 푸시한) 사용자에게 감.

## 7. 추가로 보인 점

- **같은 날 다시 돌리면 요약이 사라질 수 있음**: 오전(10:59 KST) 수집분의 오늘 요약 3건이, 이번 재수집 · 재묶기에서 대표 기사 링크가 바뀌어 이슈가 새로 만들어지면서 없어지고 새 Top 3 요약 3건만 남음(요약 수 3 → 3, 증가 0). 기존 규칙대로의 동작이지만, 수동 재실행마다 요약 비용(최대 30회)이 다시 들 수 있음.
- **품질 메모(자동 판정 아님)**: cul 첫 후보가 `[사진]` 기사 2건 묶음(제외되지 않음), soc 인천 흉기 난동이 2이슈로 갈라짐(1건 미병합). 큰 이상(전부 단독 · 대량 제외 · AI 정리 실패 · 오병합)은 없어 PLAN.md 10절에는 짧게 한 줄만 남김. 대부분 단독(108기사 → 92이슈)은 섹션당 약 20건이라는 수집 구조 때문.
- 첫 `npm test`에서 기존 `auth.test.ts`의 기동 테스트가 15초 제한에 걸림(느린 기기 · 첫 tsx 변환). Actions(ubuntu-latest)에서는 테스트를 돌리지 않으므로 하루 실행에는 영향 없음. 이 기기에서 다시 보이면 캐시가 데워진 뒤 재실행.
- `package.json`의 `name`은 `issuerank-ex11` 그대로(PLAN이 package.json 차이를 daily 한 줄로 정했고 lockfile 차이 0을 지키기 위해). Actions 로그 머리에 `> issuerank-ex11@0.1.0 daily`로 보임 — 바꾸려면 lockfile 이름도 함께 바뀌므로 사용자 확인 필요.
- PLAN.md 10절 ex06 메모의 "seed · api 서버(`index.ts` · `seed.ts`) 오류 처리 → ex09(GitHub Actions 로그) 전에 필요"는 하루 실행이 collect · group · summarize만 돌려 해당 없음(그 세 명령은 이미 `hide` 적용). 이번 범위 밖이라 손대지 않음.
- 안전 실행 · SQL 도우미(`ex09/.data/safe-run.mjs` · `db-check.mjs` · `credit.mjs` · `sum-ids.mjs`)와 실행 출력(`ex09/.data/*.txt`)은 .gitignore 대상인 `ex09/.data/`에 둠.
