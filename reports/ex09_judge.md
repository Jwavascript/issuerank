# ex09 심사 보고 — 하루 1회 자동 실행

- 날짜: 2026-10-15 (KST, 13:55~14:05) · 역할: 심사(agent_prompts/3_judge_agent.md) · 실행: Claude Code 독립 심사 세션(제작 세션과 다름), Android/Termux + proot Ubuntu, `bash scripts/ex09-ubuntu.sh …`
- 대상: `ex09/`(출발점 ex11, D17) · 저장소 루트 `.github/workflows/daily.yml` · 기준: SPEC AC-01-1 · AC-01-2, PLAN.md 5절(D3 · D5 · D17 · D18) · 6절, `plan/PLAN_backend_ex09.md`, AGENTS.md
- 제작 보고 `reports/ex09_build.md`는 참고만 하고 모든 줄을 직접 다시 확인함(제작 전용 `실제 하루(로컬)` 줄 제외).

## 판정: **PASS** (2026-10-16 예약 실행 확인, 9절) — 아래 첫 판정(2026-10-15)은 기록으로 보존

- 2026-10-16: ⑪ 예약 실행 [OK] → 합계 [OK] 12 · [FAIL] 0 · [SKIP] 0 · 해당 없음 1. 실행 요약(Summary) 화면은 API · 비로그인으로 여전히 볼 수 없어 사용자 확인 항목(⑩ 하위, 참고)으로 남김(9절).

### 첫 판정(2026-10-15): PASS 보류 — 예약 실행 확인 대기

- 구현 항목 모두 done · 테스트 111/111 · 빌드 성공 · 위반 없음 · 수동 실행(workflow_dispatch) success.
- 남은 것: `예약 실행(AC-01-1)` 줄 [SKIP] — 첫 예약 실행은 2026-10-16 06:17 KST(2026-10-15 21:17 UTC) 이후라 아직 없음. 같은 확인 때 Actions 실행 요약 화면(아래 4절 ⑩의 하위 항목)도 함께 본다. 이 둘이 OK면 PASS.

## 1. PLAN 항목별

| PLAN 항목 | 판정 | 근거 |
|---|---|---|
| `npm run daily` 명령(옵션 3개만 · 다른 인수 2) | done | `ex09/package.json` `"daily": "tsx --env-file-if-exists=.env server/daily.ts"`. `splitArgs`가 `--pages/--search/--max`만 각 단계로, 그 밖은 `--pages · --search · --max만 받습니다` · 2(테스트 + 실제 `--pages x`) |
| 실행 방식(자식 프로세스 · `== 단계 ==` · 순수 함수 steps.ts) | done | `server/steps.ts`는 process.env · DB · 네트워크 없음. `server/daily.ts`는 `spawn(process.execPath, ['--import','tsx', server/{단계}.ts, …], stdio inherit)` — PLAN 문구 `tsx server/…`와 같은 동작(참고) |
| 멈춤 규칙 · 실행 오류 | done | 코드 2면 남은 단계 건너뜀, 던지면 `실행 오류: {단계}` · 2 · 오류 객체 미출력(테스트) |
| 끝 줄 · 종료 코드 | done | `하루 실행 끝 · collect c · group g · summarize s`/`건너뜀`, 최댓값. 실제 실행 4회에서 형식 일치 |
| 날짜 · 재실행 규칙(기존 그대로) | done | 수집 · 묶기 · 요약 코드 변경 0(diff). 수동 실행 로그 `이미 요약 2`로 기존 규칙 동작 확인 |
| 워크플로 `daily.yml` | done | 4절 ⑨ |
| 비밀값(Secrets 3개 · daily 단계 env만) | done | `gh secret list` 이름 3개(DATABASE_URL · NEWSDATA_API_KEY · OPENAI_API_KEY, BETTER_AUTH 없음). 로그 env 줄은 `***` |
| 실행 기록(Actions 기록 · 실행 요약 · collect_runs) | done(실행 요약 화면은 미확인) | Actions 기록 · DB collect_runs 확인. 요약 붙이는 명령은 로그에 있고 단계 success, 화면은 4절 ⑩ |
| 테스트 `test/daily.test.ts` | done | 12개, PLAN 조합 전부 + `--pages 1 --ai 1` |
| 품질 기록 `reports/ex09_daily.md` | done | 표 머리 PLAN 그대로, 로컬(제작) 줄 + 이번 심사 수동 실행 줄 추가 |
| 새 패키지 없음 · 워크플로는 공식 액션만 | done | lockfile `cmp` 동일, `actions/checkout@v7` · `actions/setup-node@v7`만 |

## 2. SPEC AC

| AC | 판정 | 근거 |
|---|---|---|
| AC-01-1(매일 지정 시각 자동 수집 · 저장) | **미확정(예약 실행 대기)** | cron `17 21 * * *` · main에 워크플로 있음(HEAD = origin/main = 269ec9c) · 수동 실행으로 6섹션 ok 수집 · DB 저장 확인(collect_runs 155~160 ok, 기사 158). "매일 지정 시각"은 내일 예약 실행으로만 확인 가능 |
| AC-01-2(일부 실패 → 나머지 진행, 직전 결과 유지) — 회귀 | 충족 | 테스트 (1,0,0)→3단계 · 1. 실제 DB 잘못된 키 줄: 6섹션 failed 기록 · 오늘 기사 수 · 이슈 해시 불변(직전 결과 유지) |

## 3. 범위 밖 변경 · 출발점 · 기존 동작

- ex11↔ex09 `diff -rq`(산출물 · .env 제외): `package.json`(daily 1줄 + 앞 줄 쉼표) · `server/daily.ts` · `server/steps.ts` · `test/daily.test.ts`뿐. `package-lock.json` 동일. ai.ts sha256 ex08 · ex11 · ex09 모두 `07e10b1e7746…3159134`.
- `git status --short ex11/ ex08/ ex10/` 0줄 — 출발점 폴더 변경 없음.
- PLAN에 없는 추가: 없음. 화면 · API · 인증 · 묶기 · 요약 소스 변경 0이라 PLAN.md 6절 기존 동작은 `로컬 회귀 · 빌드`로 대신(PLAN "앞 단계 검증표 보존" 규칙).
- 참고: `package.json` name이 `issuerank-ex11` 그대로라 Actions 로그 머리가 `> issuerank-ex11@0.1.0 daily`(제작 보고에 이미 기록, 동작 영향 없음).

## 4. 검증 방법 한 줄씩

안전 실행은 `ex09/.data/judge/`에 둔 심사용 도우미(제작 `safe-run.mjs`와 같은 방식, 줄 덮어쓰기가 .env보다 이김 · `.env` 비밀값 · 비밀번호 · 디코딩 비밀번호 · 가짜 값 누출 검사 후 출력)로 실행. SQL은 `postgres` `prepare:false`, 클라이언트 생성 try 안 · 오류 코드만 · 내용은 해시/건수만.

| # | 확인 | 결과 | 판정 |
|---|---|---|---|
| ① | 출발점 · 변경 범위 | 3절 그대로 | [OK] |
| ② | 로컬 회귀 · 빌드 | `npm test` **첫 실행에서 14파일 111/111 통과**(18.1초, auth 기동 테스트 8.2초 — 15초 제한 문제 재현 안 됨). vitest 전후 `git status` 동일(추적 파일 변경 0). `npm run build` 종료 0(Next 16.4.0 컴파일 · TypeScript · `tsc --noEmit`). 새 패키지 0 | [OK] |
| ③ | 순서 · 멈춤(테스트) | (0,0,0) · (1,0,0) · (2,–,–) · (0,2,–) · (0,1,1) · 던짐(가짜 비밀값 출력 0) 모두 통과, 끝 줄 형식 일치 | [OK] |
| ④ | 옵션 넘김(테스트) | 자기 옵션만 · 옵션 없음 · `--ai 0` · `--x 1` 통과(단계 실행 0 · 문구 · 2) | [OK] |
| ⑤ | 키 없음(실제 명령) | `env -u DATABASE_URL -u NEWSDATA_API_KEY -u OPENAI_API_KEY -u BETTER_AUTH_SECRET -u BETTER_AUTH_URL npx tsx server/daily.ts` → 누출 false · `NEWSDATA_API_KEY가 없습니다` · `collect 2 · group 건너뜀 · summarize 건너뜀` · 종료 2. `… --pages x` → `--pages는 0~2입니다` · 같은 끝 줄 · 2. 요청 줄 없음(외부 요청 0) | [OK] |
| ⑥ | DB 오류 · 누출 | 자식 env `NEWSDATA_API_KEY=wrong-key-0000 OPENAI_API_KEY=wrong-ant-0000 DATABASE_URL=postgres://u:SECRETPW@127.0.0.1:9/x` `npm run daily` → 누출 false(SECRETPW 포함) · 종료 2 · `DB 오류: connect ECONNREFUSED 127.0.0.1:9` · 섹션 요청 줄 없음 · 나머지 `건너뜀` | [OK] |
| ⑦ | 잘못된 키(로컬, 실제 DB) | 전: M=160, 오늘 기사 cul27 · eco24 · it27 · pol28 · soc30 · wor22, 이슈 133(요약 29) 해시 `30cec339…`. 두 키 `wrong-…`로 `npm run daily -- --pages 1` → 누출 false · 종료 2 · 6섹션 `failed 1페이지: HTTP 401 …` · `요청 6번` · group/summarize `건너뜀`. 후: `id > 160` failed 6행(161~166), maxRunId 외 스냅숏 전체 동일 → 6행 삭제 → `id > 160` 0행, 스냅숏이 실행 전과 완전히 같음 | [OK] |
| ⑧ | 실제 하루(로컬, 제작만) | 제작 전용 줄이고 심사 한도(OpenAI 0)라 다시 실행하지 않음. 제작 결과는 DB(수동 실행 전 값은 덮어써짐)와 `reports/ex09_daily.md` 첫 줄로만 참고 | 해당 없음 |
| ⑨ | 워크플로 파일 | cron `17 21 * * *`(8행) · `workflow_dispatch`(9) · `concurrency` group daily · cancel false(11~13) · `contents: read`(15~16) · `ubuntu-latest` · `timeout-minutes: 30` · `working-directory: ex09` · Node 24 · npm 캐시 `ex09/package-lock.json` · `npm ci --no-audit --no-fund` · `secrets.` 3개 daily 단계 env에만 · `$GITHUB_STEP_SUMMARY` 붙이는 줄 있음. `echo` · `.env` · push · pull_request · `continue-on-error` · BETTER_AUTH 0건. 셸은 로그상 `bash --noprofile --norc -e -o pipefail`라 `set +e` 뒤 `code=$?`가 daily 종료 코드. GitHub가 파일을 받아 실행했으므로 YAML 구문도 확인됨 | [OK] |
| ⑩ | 등록 · 수동 실행 | `gh run list --workflow daily.yml`: 1건, run 38683426817 · `workflow_dispatch` · completed/success · 04:48:47Z~04:52:59Z · headSha 269ec9c(= 로컬 HEAD = origin/main). 잡 단계 8개 모두 success. 로그(294줄)를 먼저 `.env` 값과 대조 → **누출 false**, env 줄 3개는 `***`. daily 출력: collect ok 6 · 기사 158 · 요청 18 · 남은 143 / group `AI 정리 · 묶음 133 · 제외 13 · 빠진 기사 0` · `이슈 133 · 검색 36번 · 실패 0 · 남은 크레딧 107` / summarize `대상 28 · 요청 27번 · 성공 27 · 실패 0 · 이미 요약 2 · 설명문 없음 1` / 끝 줄 `collect 0 · group 0 · summarize 0`. `gh secret list` 이름 3개. DB(읽기): 그 실행 시간 안 ok `collect_runs` 6행(155~160, `finished_at` 04:49:33.968Z, 섹션별 건수 = 로그), 오늘 이슈 133 = `이슈 133`, 요약 있는 이슈 29 = 27 + 이미 2. 하위 항목 "실행 요약 화면에 daily 출력과 끝 줄": API(check-run output 비어 있음) · 비로그인 요청(404)으로 볼 수 없어 **미확인** — 명령은 로그에 있으나 `set +e`라 붙이기 실패도 단계 success가 될 수 있어 간접 증거만 있음 | [OK](실행 요약 화면만 내일 확인) |
| ⑪ | 예약 실행(AC-01-1) | 지금은 `schedule` 실행 없음(첫 예정 2026-10-15 21:17 UTC 이후) | [SKIP] |
| ⑫ | 품질 기록(사람) | 수동 실행 줄을 표 머리대로 추가(칸 모두 채움, 6절). 대표 · 묶인 제목은 DB 읽기로 확인 | [OK] |
| ⑬ | 정리 · 보존 | 확인 행 0(161~166 삭제 후 0행). 최종 스냅숏 = 시작 스냅숏(사용자 · 세션 · 계정 · 북마크 · 폴더 해시 불변, 현재 모두 0행). 지난 날짜 기사 285 · 이슈 102 · 수집기록 54 · 지난 이슈 해시 `48968836…` 불변. 수동 실행이 만든 오늘 데이터(기사 158 · 이슈 133 · 요약 29 · ok 6행) 보존. 심사가 띄운 서버 없음. 실행 중인 ex08 `npm run api` · `next dev`(3000)는 사용자 서버라 그대로 둠. 심사용 임시 폴더 `ex09/.data/judge/` 삭제 | [OK] |

**합계: [OK] 11 · [FAIL] 0 · [SKIP] 1 · 해당 없음 1** (⑩의 실행 요약 화면 하위 확인 1건 남음)

## 5. 지켜야 할 것 위반

- 기사 본문 저장 · 표시: 없음(수집 · 묶기 · 요약 코드 변경 0, 출력에 제목 · 본문 새로 넣지 않음).
- 코드에 들어간 키 · 접속 정보: 추적 파일 78개(ex09 · .github · ex09 래퍼 · ex09 보고)를 `.env` 값과 대조 → 0건. 키 모양 문자열 3파일은 모두 테스트의 가짜 값(`FAKE-SECRET` 등).
- `.env` git 제외: `.gitignore:4:.env`로 `ex09/.env` 제외, `.data/`도 제외. `git ls-files ex09`에 `.env`(예시 파일 외) · `.data` 없음.
- PLAN에 없는 패키지: 없음(lockfile 동일).
- D0 밖의 출처: 없음(newsdata.io만, 수집 코드 불변).
- AGENTS.md: 출발점 보존 · 비밀값 미출력 · 안전 실행 · 확인 행 정리 · 실제 데이터 보존 지킴. 위반 없음.

## 6. 발견 · 참고 (심각도 순)

1. **(중) 실행 요약 화면 미확인**: `set +e` 때문에 `$GITHUB_STEP_SUMMARY` 붙이기가 실패해도 단계는 daily 코드로 끝나므로 success만으로는 붙었는지 알 수 없음. 내일 확인 때 Actions 실행 화면 Summary에서 코드 블록과 끝 줄을 직접 봐야 함.
2. **(참고) 같은 날 재실행 시 요약 소실 — 확인됨**: 로컬(제작) 실행의 요약 pol-22 · 23 · 24 중 대표 기사 링크가 그대로인 pol-23 · 24는 유지(`이미 요약 2`), 대표가 바뀐 pol-22는 이슈가 pol-38로 새로 생기며 사라짐(현재 pol-22 이슈 없음). 수동 재실행 비용: 이번 수동 실행에서 newsdata 54 · OpenAI 29회(정리 2 + 요약 27, 한도 32 안). 기존 규칙대로의 동작.
3. **(참고) 수동 실행이 오늘(10-15) 데이터를 덮어씀**: 제작 로컬 실행(기사 108 · 이슈 92 · 요약 3)이 수동 실행(기사 158 · 이슈 133 · 요약 29)으로 교체됨. 규칙대로이며 오늘 수동 실행 1회 사용(D5 같은 날 수동 최대 2회 중).
4. **(참고) 품질**: 섹션 첫 후보 6개 모두 한 사건. pol-34(김지용 결격 사유)가 pol-38 청문회 묶음과 따로 남음(미병합 1). eco 1위 eco-27은 설명문 없음으로 요약 없음. 큰 이상(전부 단독 · 대량 제외 · AI 정리 실패 · 오병합) 없음 → PLAN.md 10절 추가 불필요.
5. **(참고) 저장소 공개(PUBLIC)**: 60일 활동이 없으면 예약 실행이 꺼짐. 로그는 Secrets 마스킹되고 누출 없음을 확인.
6. **(참고) 경계 입력**: `--pages 1 --pages 2`처럼 같은 옵션을 두 번 주면 뒤 값만 넘어감, `--pages`만 주면 값 없이 collect에 넘어가 collect 문구로 거절. PLAN에 없는 경계라 판정에 넣지 않음. 로그 머리 `.env not found. Continuing without it.` 2줄은 Actions에 .env가 없어서 나는 정상 안내.

## 7. 호출 횟수

- newsdata 실제 크레딧: 크레딧 확인 **1회**(14:01 KST, `status 200 · x-api-limit-remaining 106` = 수동 실행 끝 107 − 1, 차이 0). 잘못된 키 401 요청 6번(크레딧 0).
- OpenAI: **0회**(잘못된 키 줄은 collect 2에서 멈춤).
- 남은 newsdata 크레딧: **106**(2026-10-15 14:01 KST). Actions 실행 0 · Secrets 변경 0 · 커밋/푸시 0.

## 8. 내일 예약 실행 확인 방법(2026-10-16 06:17 KST 이후, 이 줄만)

1. `gh run list --workflow daily.yml --event schedule --limit 3 --json databaseId,event,conclusion,createdAt,startedAt,updatedAt,headSha` → 이벤트 `schedule` · conclusion `success` · `startedAt`이 2026-10-15T21:17Z 이후(지연 분 = startedAt − 21:17Z를 적음).
2. `gh run view <id> --log > ex09/.data/judge/sched-log.txt` → 먼저 `ex09/.env` 값(DATABASE_URL 전체 · 비밀번호 · 디코딩 비밀번호 · NEWSDATA_API_KEY · OPENAI_API_KEY · BETTER_AUTH_SECRET)과 대조해 leak true/false만 출력 → false일 때만 `npm run daily` 단계의 collect · group 둘째 줄 · 섹션 첫 후보 · summarize · 끝 줄을 봄.
3. 브라우저(로그인)로 그 실행 페이지 Summary에 daily 출력 코드 블록과 끝 줄이 있는지 확인(오늘 수동 실행 38683426817의 Summary도 함께 보면 ⑩ 하위 항목 해결).
4. SQL(읽기, 안전 실행): 날짜 2026-10-16 `collect_runs` ok 6행의 `finished_at`이 실행 시간 안 · 섹션마다 기사 > 0 · 이슈 > 0 · 요약 ≥ 1. API를 띄웠다면 `GET /api/rankings` 기본 응답 `date` = 2026-10-16(node fetch).
5. `reports/ex09_daily.md`에 예약 실행 줄 추가(run id 포함). newsdata 호출은 크레딧 확인 최대 1, OpenAI 0. 모두 OK면 이 보고의 판정을 PASS로 갱신.

## 9. 예약 실행 확인(2026-10-16 09:50~10:00 KST, ⑪ 줄만)

- 실행: Claude Code 독립 심사 세션, Android/Termux + proot. 서버 띄우지 않음. 코드 수정 · 커밋 · 푸시 · Actions 실행 · Secrets/Vercel 설정 0. 로컬 main은 origin보다 1커밋(5156e89) 앞선 그대로 둠.

| 기대(PLAN 검증표 「예약 실행」) | 확인 | 판정 |
|---|---|---|
| 이벤트 `schedule` | `gh run list --workflow daily.yml`: run **38825041172** · event `schedule` · completed | OK |
| 시작 21:17 UTC 이후(지연 분 수를 적음) | startedAt `2026-10-15T23:41:04Z`(08:41 KST) → **지연 144분**(2시간 24분 4초). updatedAt 23:45:10Z(4분 6초). PLAN 문구는 하한(21:17 UTC 이후)과 지연 분 기록만 요구하고 상한이 없으며, 같은 PLAN의 GitHub 사실 줄에 "부하가 크면 늦어지거나 빠질 수 있음(06:17보다 늦게 시작해도 KST 같은 날이라 날짜는 같음)"이 있음 → 지연 자체는 FAIL 아님. 실제로 KST 날짜 2026-10-16 그대로 | OK |
| success | conclusion `success`, 잡 단계 8개 모두 success. 로그 끝 줄 `하루 실행 끝 · collect 0 · group 0 · summarize 0` | OK |
| 실행 코드 | headSha `292c47b`(ex12 배포 준비 커밋). `git diff --stat 269ec9c 292c47b -- ex09 .github` 출력 0줄 → ex09 · 워크플로가 심사한 269ec9c와 같음 | OK |
| 그 날짜 6섹션 ok 행의 `finished_at`이 실행 시간 안 | SQL(읽기 전용 트랜잭션, `transaction_read_only = on`): 2026-10-16 `collect_runs` 167~172 6행 모두 ok(pol 29 · eco 27 · soc 30 · cul 23 · wor 30 · it 28 = 로그), `finished_at` 모두 23:41:50.499Z ∈ [23:41:04Z, 23:45:10Z]. 그 날짜 다른 행 없음 | OK |
| 섹션마다 기사 > 0 | articles 2026-10-16: cul 23 · eco 27 · it 28 · pol 29 · soc 30 · wor 30(합 167 = 로그 `기사 167건`) | OK |
| 이슈 > 0 | issues 2026-10-16: 합 135(= 로그 `이슈 135`), 섹션 cul 26 · eco 23 · it 13 · pol 17 · soc 34 · wor 22 | OK |
| 요약 ≥ 1 | 요약 있는 이슈 29(= 로그 `성공 29` · 이미 요약 0), 섹션별 pol 4 · 나머지 5 | OK |
| 기본 랭킹 `date` = 그 날짜 | 로컬 API 대신 배포 주소(같은 DB) `GET https://issuerank-kappa.vercel.app/api/rankings` 1회(node fetch) → 200 · `date` **2026-10-16** · `updatedAt` 2026-10-16T08:41:50+09:00(= collect finished_at) | OK |

**⑪ 예약 실행(AC-01-1): [OK]** → AC-01-1 충족(매일 지정 시각 예약 실행으로 자동 수집 · 저장, 지연 144분은 PLAN이 허용한 GitHub 지연).

- 로그 누출: `gh run view 38825041172 --log`(301줄)를 scratchpad 임시 파일로 받아 `ex09/.env`의 DATABASE_URL 전체 · 비밀번호 · 디코딩 비밀번호 · NEWSDATA_API_KEY · OPENAI_API_KEY · BETTER_AUTH_SECRET(6값)과 대조 → **leak false**. 그 뒤 daily 줄만 봄: collect `ok 6 · failed 0 · 기사 167건 · 요청 18번 · 남은 크레딧 64` / group 둘째 줄 `AI 정리 · 묶음 135 · 제외 14 · 빠진 기사 0` / `이슈 135 · 검색 36번 · 실패 0 · 남은 크레딧 28` / summarize `대상 30 · 요청 29번 · 성공 29 · 실패 0 · 이미 요약 0 · 설명문 없음 1` / 끝 줄 위와 같음. 임시 로그 · DB 확인 스크립트 삭제.
- 실행 요약(Summary) 화면: check-run API `output.summary` 길이 0(주석 1개는 GitHub 공지 "ubuntu-latest가 2026-10-19부터 Ubuntu 26으로 이동"), 비로그인 실행 페이지에 daily 출력 없음 · `summary_raw` 404 → **볼 수 없음**. 붙이는 명령 줄은 로그에 있음. ⑩의 하위 항목이라 판정 줄이 아니며 사용자 확인 항목으로 남김(아래).
- 품질 기록: `reports/ex09_daily.md`에 예약 실행 줄 추가. 첫 후보 6개 모두 한 사건, `n건 (묶음 k)` n = k **6/6**(후보 줄 36개 전부 n = k, 최대 n 6) — 푸시된 옛 검색어 코드(D19 이전)라 예상대로. 참고: pol-1에 `[이시각헤드라인] 10월 15일 뉴스리뷰`(여러 소식 모음 기사) 1건이 섞임, cul-1 · cul-2 같은 사건 미병합 1. 전부 단독 · 대량 제외(14/167) · AI 정리 실패 · 명백한 다른 사건 오병합은 없음 → PLAN.md 10절 추가는 이번 허용 범위 밖이라 하지 않음(모음 기사를 오병합으로 볼지는 사용자 판단).
- 호출: newsdata 크레딧 확인 **1회**(09:54 KST, status 200 · `x-api-limit-remaining` **199** — 일일 한도가 초기화된 뒤라 실행 끝 28과 직접 비교 불가). OpenAI 0. 배포 주소 GET 1회. DB 쓰기 0(읽기 전용 트랜잭션).
- 사용자 확인: ① 로그인 브라우저로 run 38825041172(그리고 수동 실행 38683426817)의 Summary에 daily 출력 코드 블록 · 끝 줄이 있는지. ② 예약 지연(오늘 144분)이 계속 크면 06:17 수집분 시각이 밀림 — 기록만. ③ GitHub 공지: `ubuntu-latest`가 2026-10-19부터 Ubuntu 26으로 바뀜(그 뒤 첫 예약 실행 결과를 한 번 볼 것).
