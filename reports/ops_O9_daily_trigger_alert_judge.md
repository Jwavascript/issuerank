# ops O9 독립 심사 — 아침 갱신 정시 시작 · 운영 경보

- 날짜: 2026-11-14 22:30~23:00 KST · PC Windows 11 · Node 24.22.2(`issuerank-tools`) · 심사 = 새 세션(제품 코드 · 워크플로 · 시험 파일 수정 0, 이 보고만 씀)
- 기준: `plan/PLAN_ops_O9_daily_trigger_alert.md` · SPEC AC-01-1~01-4 · PLAN.md D3 · D5 · D37 · D38 · AGENTS.md · 제작 보고 `reports/ops_O9_daily_trigger_alert_build.md`
- 대상: 작업 트리(미커밋) — `git diff` HEAD 비교. `android/` 8개 수정 + 새 `EllipsisDeclarationsTest.kt`(파일 시각 11-05, O8 보고 447줄에 기록)와 `reports/ops_O8_android_app_build.md`는 O8 미커밋이라 범위 밖.

## 판정

> **재확인 뒤 최종 판정 = PASS 보류(SKIP 남음), OK 9 / FAIL 0 / SKIP 4 — 8절.** 아래는 첫 심사 기록(그대로 둠).


**FAIL (문서 1건 — 코드 · 워크플로 · 시험 · 빌드는 문제 없음)**

- 합계 **OK 9 / FAIL 0 / SKIP 4**(10~13 = 배포 · 다음 날 · 사용자 · 다음 문제 날 — 정상 SKIP).
- FAIL 이유: 계획 "SPEC · PLAN.md 반영"이 약속한 **PLAN.md D3 · D5 보충 줄이 없다**(PLAN.md diff = D37 · D38 두 줄뿐). D3 칸은 지금도 "수집은 매일 04:17(cron `17 19 * * *` UTC)"라서 바뀐 `daily.yml`(`17 20 * * *` = 05:17 안전망 · 주 경로 Vercel Cron 04시대)과 어긋난다. 계획 항목 partial → 심사 기준(항목 모두 done)상 PASS가 아님. 고치면(문서 2줄) 다시 보는 것은 문서 확인만으로 충분.

## 확정 위반

- 없음(기사 본문 · 키 · 접속 정보 코드 포함 0, `.env` 이 PC에 없음 · git 제외 그대로, 새 패키지 0, D0 밖 출처 0, web/ · ex01~11 · frontend 변경 0).
- 문서 불일치 1건(위 FAIL 이유)은 위반이 아니라 계획 항목 partial로 셈.

## 커밋 전 고칠 것

1. **[필수 · 문서] `PLAN.md` 139줄(D3) · 141줄(D5)** — 계획이 약속한 보충 줄 추가. D3: 주 경로 = Vercel Cron 04시대(`0 19 * * *` UTC) → workflow_dispatch, 예약 `17 20 * * *` = 05:17 안전망(D37). D5: 예약 · 정시 실행은 오늘 랭킹이 있으면 건너뜀, 사람이 누른 실행은 그대로 + 아래 2번의 Re-run 처리 + 크레딧 계산(정시 실행이 수집만 하고 이슈 0행으로 실패하면 05:17 안전망이 다시 수집 → 자동 2 + 수동 2 = 하루 200 크레딧을 넘을 수 있음, 아래 참고 2).
2. **[권장 · 계획 빈틈] `.github/workflows/daily.yml` 44줄(`오늘 확인` 단계 `if:`)** — GitHub의 **Re-run은 처음 실행의 이벤트 · 입력을 그대로 씀**(`github.event_name == 'schedule'` · `inputs.trigger == 'cron'` 유지, `github.run_attempt`만 늘어남). 그래서 11-13 · 11-14처럼 "AI 실패로 규칙 묶기 행은 들어갔는데 종료 1"인 예약 · 정시 실행을 사람이 **Re-run**하면 오늘 확인이 "랭킹 있음"으로 보고 daily를 **건너뛴다**. daily.yml 2줄 주석 · D5는 수동 재실행을 "Run workflow · Re-run"으로 적고, AC-01-3은 "사람이 누른 수동 실행은 그대로"라 어긋난다. 계획 글자(`trigger=manual`만 확인 없음)는 지켰으므로 판정 근거로는 넣지 않았지만, 다음 실패 날 바로 걸릴 가능성이 높다. 고치는 법(한 줄): `if: (github.event_name == 'schedule' || inputs.trigger == 'cron') && github.run_attempt == 1` + `test/workflows-o9.test.ts` 21줄 기대 글자 같이 바꿈. 또는 고치지 않고 D5 보충 줄에 "다시 돌릴 때는 Re-run이 아니라 Run workflow"를 적기.
3. **[낮음 · 선택] `daily.yml` 55줄 실행 요약 문구** — 계획은 `오늘 랭킹이 이미 있어 건너뜀 YYYY-MM-DD`인데 실제는 `건너뜀 · {today 출력 전체}`이고, Actions에는 `.env`가 없어 Node가 `.env not found. Continuing without it.` 두 줄을 stderr로 내므로(이 PC에서 재현 — 기존 daily · push 요약에도 있던 안내, ex09 · O5 심사 참고 기록) 요약이 `건너뜀 · .env not found…` 여러 줄이 된다. 같은 두 줄이 `health.yml`의 `2>&1` 때문에 **공개 이슈 본문 맨 앞**에도 들어간다(비밀값 아님 · 같은 출력 생략은 그대로 동작). 원하면 마지막 줄만 쓰기(`tail -n 1`) 또는 `.env not found` 줄 거르기.

## 1. 계획 항목별

| 계획 항목 | 결과 | 근거 |
|---|---|---|
| `vercel.json` crons 2개 · regions 그대로 | done | `0 19 * * *` `/api/cron/daily` · `0 23 * * *` `/api/cron/health`, `regions: ["icn1"]` |
| 경로 `app/api/cron/[job]/route.ts`(GET · nodejs · force-dynamic) | done | GET만(POST → 405 실측), 판단은 `server/cron.ts` |
| job → 워크플로 · 입력, 그 밖 404 | done | `daily.yml {trigger:cron}` · `health.yml {mode:morning}`, `Object.hasOwn`(`constructor` · `__proto__` · `toString` 404) |
| 인증 503 · 401 · 503 | done | CRON_SECRET 없음 503 → `Bearer {secret}`와 sha256 + `timingSafeEqual`(길이 달라도 같은 시간) 401 → 토큰 없음 503. 404가 인증보다 먼저(이름 daily · health만 드러남 — 문제 없음) |
| GitHub 호출 | done | `POST https://api.github.com/repos/Jwavascript/issuerank/actions/workflows/{파일}/dispatches`, 본문 `{"ref":"main","inputs":…}`, 머리 4개 + Content-Type, `AbortSignal.timeout(10s)` · 재시도 0, 200 · 204 → 202(GitHub 문서: 성공 204, `return_run_details`면 200) · 그 밖 · 던짐 → 502 + 상태/0 |
| 응답 · 로그 | done | 모든 응답 `no-store` · 쿠키 0, 로그 `CRON_DISPATCH_FAILED {상태}`뿐, 응답 본문은 읽지 않고 cancel |
| 저장소 · 파일 이름 = 코드 상수 | done | `REPO` · `JOBS` |
| daily.yml schedule · 입력 | done | `17 20 * * *` 하나 · `inputs.trigger`(string, 기본 manual) |
| daily.yml 오늘 확인 단계 | done(문구만 partial — 고칠 것 3) | npm ci 뒤 · daily 앞, `if: github.event_name == 'schedule' \|\| inputs.trigger == 'cron'`, 종료 0이면 `skip=true` · 요약 한 줄, 1 · 2면 그대로 실행, `exit 0`. schedule 이벤트에서 `inputs.trigger`는 null → 조건 false지만 앞쪽이 true라 문제 없음. 확인 단계가 건너뛰어지면 `steps.guard.outputs.skip` = '' → daily 실행(수동 실행 그대로). DATABASE_URL만 받음 |
| daily.yml 그 밖 그대로 | done | concurrency · timeout 30 · permissions · daily 단계 비밀값 3줄 · tee · 요약 그대로(diff로 확인) |
| `server/today.ts` · `npm run today` | done | 0 있음 · 1 없음 · 2 DB 오류(hide 거침) |
| 경보 코드 8개 판단(`health-check.ts`) | done | 아래 4절 |
| `npm run health` 출력 · 종료 코드 | done | 첫 줄 `상태 OK {날짜}` / `상태 문제 n · {날짜}` + 코드 줄, 0 · 1 · 2. 인수 오류도 2(계획에 없는 경계 — 참고) |
| `health.yml` | done | 아래 3절 |
| `test: true` 시험 이슈 | done | DB · npm ci 없이 `운영 경보 시험 {날짜}` 1개 |
| 새 패키지 0 | done | dependencies 이름 변화 0 |
| 판 올림 4개 | done | 아래 5절 |
| openapi `/api/cron/{job}` 내부용 · 0.9.0 | done | |
| 시험 3파일 | done | cron 8 · health 13 · workflows-o9 8 = **29개**(제작 보고 "새 30"은 1개 많게 적음 — 전체 356은 맞음) |
| SPEC AC-01-3 · 01-4 · D37 · D38 · PRODUCT_PLAN · HANDOFF | done | |
| **PLAN.md D3 · D5 보충 줄** | **missing** | 고칠 것 1 |

### AC 충족(계획 "SPEC 근거")
- AC-01-3: 코드 · 설정 기준 충족(정시 dispatch · 05:17 안전망 · 오늘 랭킹 있으면 건너뜀 · 수동 실행 그대로). 단 Re-run은 예외(고칠 것 2). 실제 시각 확인은 줄 11(SKIP).
- AC-01-4: 판단 7가지 + 이슈 열기/댓글 · 숫자 · 상태만 — 시뮬레이션까지 충족. 실제 알림은 줄 12 · 13(SKIP).
- 회귀 AC-01-1 · 01-2(수집 코드 무변경) · AC-14-2(push.yml 무변경) · AC-11-1 · AC-05-1: 관련 코드 diff 0, 전체 시험 통과 — 참고.

## 2. 검증 방법 표

| # | 결과 | 실제로 한 것 |
|---|---|---|
| 1 | **OK** | `git status --short`(31줄, 시작과 끝 같음) · `git diff --stat`: O9 파일 + 문서. web/ · ex01~11 · frontend 0. android는 O8 미커밋(범위 밖) |
| 2 | **OK** | `test/cron.test.ts` 인증 4개 통과 + 로컬 `next start`(127.0.0.1:3117, `CRON_SECRET` 임시값 · `GITHUB_DISPATCH_TOKEN` · `DATABASE_URL` 없음, Node fetch): 머리 없음 401 · 틀림 401 · 비밀값+1글자 401 · 맞음 503 `CRON_NOT_CONFIGURED` · health 503 · `nope` 404 · `constructor` 404 · POST 405, 전부 `no-store` · 쿠키 0 · 비밀값 글자 0 → catch-all `[...path]`이 아니라 새 경로가 받음. GitHub 호출 0. 띄운 프로세스만 종료(포트 3117 Listen 0) |
| 3 | **OK** | 시험: 주소 · 본문 · 머리 · signal, health 입력, 없는 job fetch 0 |
| 4 | **OK** | 시험: 401 · 404 · 500 · 던짐 → 502 + 상태, 응답 · 로그에 토큰 · 비밀값 0. 추가로 **실제 10초 시간 초과**(signal에만 반응하는 가짜 fetch, tsx): `502 {"message":"DISPATCH_FAILED","status":0}` · 로그 `CRON_DISPATCH_FAILED 0` · 10초 |
| 5 | **OK** | 시험 `todayResult` 0/1 + 직접 실행(실제 ex12, `.env` 없음): `DATABASE_URL` 없음 → `DB 오류: DATABASE_URL이 없습니다` 종료 2 · 닫힌 로컬 포트(`postgres://judge:<가짜>@127.0.0.1:1/x`) → `DB 오류: connect ECONNREFUSED 127.0.0.1:1` 종료 2(가짜 비밀번호 글자 0). DB 오류 2는 시험이 아니라 실행으로 확인 |
| 6 | **OK** | `test/health.test.ts`: 코드마다 걸림/안 걸림, 경계 요약 없음 7/8 · 07:00/07:01 · importance 하나만 1, morning = NO_RANKING만. 직접 실행: health `DATABASE_URL` 없음 · 닫힌 포트 → `상태 문제 1` + `- CHECK_ERROR DB를 읽지 못함` 종료 2 · `--run 'a;b'` 종료 2 |
| 7 | **OK** | 11-13 재현 시험: `RUN_FAILED` · `AI_FALLBACK` · `SUMMARY_LOW 요약 없음 30/30` · `LATE 09:19` 5줄 정확히 |
| 8 | **OK** | `test/workflows-o9.test.ts` 8개 + 아래 3절 bash 흉내 |
| 9 | **OK** | 실제 ex12(`C:\Users\…` 대문자 경로): `npm.cmd test` 32/35 파일 · 339/341 — 실패 3파일 `pwa` · `summary-text` · `push-ui` = CRLF 작업 사본 환경 원인(기록과 같음). **LF 사본**(scratchpad `judge-o9\ex12` + `.github/workflows`, 텍스트 94파일 LF, node_modules junction): **35/35 파일 · 356/356**. `npm.cmd run build` 실제 ex12 **2번 연속 exit 0**(Next.js 16.4.1 Turbopack, 보안 멈춤 0, 경로 표 `ƒ /api/cron/[job]`). junction은 `cmd /c rmdir`로만 지움 · 실제 node_modules 그대로 |
| 10 | SKIP | 푸시 · 배포 뒤 |
| 11 | SKIP | 다음 날 아침 |
| 12 | SKIP | 사용자(휴대폰 알림) |
| 13 | SKIP | 다음 문제 날 |

합계 **OK 9 / FAIL 0 / SKIP 4**.

## 3. 워크플로 한 줄씩

### daily.yml
- 예약 · cron만 확인, 사람 `manual`은 확인 없이 실행: 맞음. `skip` 조건: 확인 단계가 안 돌거나 종료 1 · 2면 출력이 없어 `!= 'true'` → daily 실행. 기존 단계 · 비밀값 줄 · concurrency · timeout 그대로.
- bash 흉내(scratchpad `judge-o9\sim`, 가짜 `npm`): 종료 0 → `skip=true` + 요약 1줄, 1 · 2 → 출력 0 · 요약 0, 단계는 늘 종료 0.
- Re-run 문제 → 고칠 것 2.

### health.yml (가짜 `gh` · `npm` · jq 식 2개 흉내로 실제 run 블록을 그대로 돌림)
| 경우 | 결과 |
|---|---|
| workflow_run · failure · 열린 이슈 없음(제목이 비슷한 `운영 경보 시험 …`만 있음) | 정확 일치 선택으로 시험 이슈를 고르지 않음 → `gh issue create --title "운영 경보 {날짜}"`, 본문 = 코드 블록 출력 + 검사 실행 · 하루 실행 주소 + `@Jwavascript`, 실행 요약에 같은 출력, 종료 0 |
| 같은 출력이 이미 이슈 본문에 있음(LF · CRLF 둘 다) | `같은 경보가 이미 있어 댓글 생략` · 종료 0 |
| 다른 출력(morning NO_RANKING) | `gh issue comment` · 종료 0 |
| 상태 OK | gh 호출 0 · 종료 0 |
| `TEST=true` | npm 호출 0 · `운영 경보 시험 {날짜}` 1개 |
| 이슈 만들기 실패(gh 종료 1) | 단계 종료 1(실패가 빨갛게 보임 — 괜찮음) |
| 결론 빈 값 | `--run ""` → RUN_FAILED 안 냄(계획 밖 경계 — 참고) |

- `set +e` 아래 `code=$?`는 npm 바로 뒤라 맞음. 이벤트 · 입력 값은 모두 env, run 줄에 `${{ }}` 0. `inputs.test != true`는 workflow_run(입력 null)에서 true라 npm ci가 돎. 권한 contents read · issues write뿐. `GH_TOKEN`은 env만.
- 공개 출력: health 출력은 숫자 · 코드 · 섹션 키 · 시각 · 날짜뿐(오류 객체 · 문구는 찍지 않음, 기사 제목 · 링크 0). 다만 `.env not found…` 두 줄이 이슈 맨 앞에 붙음(고칠 것 3).

## 4. health-check 판단 대 계획 표

| 코드 | 계획 | 코드 | 결과 |
|---|---|---|---|
| NO_RANKING | issues 0행 | `issues.length === 0` | 같음 |
| RUN_FAILED | `--run`이 success 아님 | `run !== undefined && run !== 'success'` | 같음 |
| COLLECT_FAILED | 가장 늦은 `finished_at` 실행의 failed 섹션 | 최대 finishedAt과 같은 행 중 `status !== 'ok'`(스키마 `'ok' \| 'failed'`) | 같음 |
| AI_FALLBACK | 이슈 있고 importance 전부 0 | `every(importance === 0)` | 같음 |
| SUMMARY_LOW | Top 5(D16, 최대 30) 중 요약 없음 ≥ 8 | 섹션마다 `rankIssues`(summary.ts와 같은 함수 · 같은 섹션별 호출, D16 정렬 + `slice(0,5)`), null 또는 빈 배열 셈 | 같음 |
| ALL_ONES | 섹션 Top 5(5개) 전부 1건 | `top.length === 5 && every(articleCount === 1)` | 같음 |
| LATE | ok 수집 가장 늦은 완료 > 07:00 | KST "HH:MM" 분 단위 > 420 → 07:00:59까지는 안 늦음, 07:01부터 | 같음(초 버림 — 경계 참고) |
| CHECK_ERROR | DB 못 읽음 | catch 전체 | 같음 |
| morning | NO_RANKING만 | 첫 줄 뒤 return | 같음 |

## 5. 판 올림 · audit
- 잠금 파일 바뀐 패키지 판: `next` 16.4.0→16.4.1 · `@next/env` + `@next/swc-*` 8개 16.4.1 · `hono` 4.13.13→4.13.16 · `@hono/node-server` 2.1.3→2.1.6 · `zod` 4.6.5→4.6.6 — **그 밖 판 변화 0**.
- 판이 아닌 줄 3곳(문제 없음): 루트 항목에 `engines: {node: 24.x}`(package.json에 이미 있던 값이 잠금 파일에 기록됨) · `@hono/node-server` 2.1.6의 peer `hono` 범위 `^4 || ^5.0.0-0` · `next` 16.4.1의 optional `sharp` 범위 `^0.35.5`(설치된 sharp는 HEAD부터 0.35.5 — 판 변화 0).
- `npm audit`: **moderate 4**(esbuild ← @esbuild-kit ← drizzle-kit 개발 도구 사슬). next · hono · zod · @hono/node-server 공지 0. 계획대로 O13 몫.

## 6. 참고(판정에 넣지 않음)
1. **concurrency 대기 취소**: GitHub concurrency는 대기 실행을 하나만 두고 새 것이 오면 앞 대기를 cancelled로 만든다(`cancel-in-progress: false`여도). daily가 3개 겹치면(cron 두 번 + 예약) 하나가 cancelled → health가 `RUN_FAILED cancelled` 경보. 드묾. health 그룹도 같음.
2. **크레딧**: 정시 실행이 수집(크레딧 사용) 뒤 이슈 0행으로 실패하면 05:17 안전망이 다시 수집 → D5의 "자동1 + 수동2 = 162 크레딧 이하" 계산이 깨질 수 있음(자동2 + 수동2 ≈ 216 > 200). 고칠 것 1의 D5 보충 줄에 적을 것.
3. **이슈 찾기**: `gh issue list --search`는 검색 색인을 써서 방금 만든 이슈가 몇 초~1분 안 보일 수 있고 한글 토큰 처리에 기대게 됨 → 못 찾으면 댓글 대신 같은 제목 이슈가 하나 더 생김(경보는 감). `--search` 없이 `--state open --limit 100 --json number,title`로 정확 일치를 고르면 색인과 무관. 낮음.
4. morning 출력(`상태 문제 1 · NO_RANKING`)은 after 출력과 글자가 달라 랭킹 없는 날은 08시대에 댓글이 하나 더 달림 — 계획 의도("같은 경보 두 번 방지")와 약간 다르나 알림이 늘어날 뿐.
5. `health.ts` · `today.ts`의 `finally { await conn?.client.end() }`가 드물게 던지면 처리 안 된 거부가 되어 Node가 스택을 stderr로 냄 → health는 `2>&1`로 공개 이슈에 들어감(접속 문자열은 보통 안 들어가지만 호스트가 보일 수 있음). today는 종료 1이 되어 daily가 한 번 더 돎. 가능성 낮음.
6. `plan/PRODUCT_PLAN.md` O13 줄에 아직 "Next 16.4.1 · zod 4.6.6" — O9에서 앞당겨 했으니 O13 계획 때 지울 것.
7. 이 PC 첫 `next build`에 Next.js 익명 통계 안내가 다시 찍힘(환경 — 제품 무관). `next start` 확인 때는 `NEXT_TELEMETRY_DISABLED=1`로 띄움.
8. 제작 보고의 새 시험 수 "30"은 실제 29(cron 8 · health 13 · workflows-o9 8). 전체 356은 맞음.

## 7. 정리한 것
- 띄운 `next start`(PID 기록 → 그 프로세스와 자식만 종료, 포트 3117 Listen 0) · 임시 로그 2개 삭제.
- scratchpad LF 사본의 node_modules junction은 `cmd /c rmdir`로만 삭제(실제 `ex12/node_modules/next/package.json` 있음 확인). LF 사본 · 시뮬레이션 파일은 scratchpad에만 남음.
- 작업 트리: 이 보고 파일 하나만 새로 생김. 운영 DB · 운영 사이트 · GitHub API 쓰기 · newsdata · OpenAI 호출 0.

## 8. 재확인 (2026-11-14 23시대 KST — 제작 보고 3.1절 반영분만)

**판정: PASS 보류(SKIP 남음)** — 합계 **OK 9 / FAIL 0 / SKIP 4**(10~13). 확정 위반 0. 커밋 전 고칠 것 없음.

| 앞 지적 | 반영 | 확인 |
|---|---|---|
| 1 PLAN.md D3 · D5 보충(필수) | done | 186줄 D3 보충(정시 = Vercel Cron `0 19 * * *` → workflow_dispatch, 예약 `17 20` = 05:17 안전망, 04:17 안 씀) · 188줄 D5 보충(첫 시도만 오늘 확인 · Run workflow와 Re-run은 그대로 · 216크레딧 주의 → 그런 날 수동 재실행 1번까지 · 자동 재시도 없음). 계획 28줄도 같은 조건으로 고침 |
| 2 Re-run 건너뜀(권장) | done | daily.yml 44줄 `if: (github.event_name == 'schedule' \|\| inputs.trigger == 'cron') && github.run_attempt == 1`. `github.run_attempt`는 문자열이지만 식의 `==`가 숫자로 바꿔 비교하므로 첫 시도에서 참 · Re-run(2부터)에서 거짓 → daily 그대로 실행. 수동(manual) · 확인 단계가 안 돈 경우 `skip`이 비어 daily 실행은 그대로 |
| 3 stderr가 요약 · 공개 이슈에 들어감(낮음) | done | daily guard는 `2>"$RUNNER_TEMP/today.err"` 뒤 작업 로그(stderr)로만, health는 `2> health.err` 뒤 작업 로그로만 · 이슈 본문과 실행 요약은 stdout만 |
| 참고 3 `--search` 색인 | done | `gh issue list --state open --limit 200 --json number,title` + 제목 정확 비교 |
| 참고 5 `client.end()` 거부 | done | today.ts 23줄 · health.ts 24줄 `.catch(() => {})` |
| 시험 기대값 | done | workflows-o9.test.ts 22 · 24 · 60줄 |

다시 돌린 것:
- **bash 흉내**(scratchpad `judge-o9\sim`, 지금 워크플로에서 run 블록을 다시 뽑음, 가짜 npm이 stderr에 `.env not found…`와 가짜 `STACK_SECRET_LINE`을 냄): health 새 이슈(목록에 `운영 경보 시험 …`만 있으면 새로 만듦 · `--search` 없음 · `--limit 200`) · 같은 출력 이미 있음(CRLF 본문) → 댓글 생략 · 다른 출력 → 댓글 · test → 시험 이슈만. **이슈 본문 4개 · 실행 요약 4개에 stderr 글자 0**, stderr는 단계 stderr(작업 로그)로만 나감. daily guard 종료 0 → `skip=true` + 요약 `오늘 랭킹이 이미 있어 건너뜀 · 오늘 랭킹 있음 YYYY-MM-DD` 한 줄, 1 · 2 → 출력 · 요약 0, 셋 다 단계 종료 0 · stderr는 로그로만.
- **시험**: LF 사본을 지금 파일로 다시 만들어(95파일 LF, node_modules junction → `cmd /c rmdir`로만 삭제 · 실제 node_modules 있음 확인) `npm.cmd test` **35/35 파일 · 356/356**.
- **빌드**: 실제 ex12 `npm.cmd run build` exit 0(Next.js 16.4.1, `ƒ /api/cron/[job]`). 앞서 2번 연속 OK에 더해 3번째.
- 변경 범위: 새로 바뀐 파일은 위 표의 곳 + 제작 보고 · HANDOFF뿐, web/ · ex01~11 0.

남은 참고(고칠 것 아님): 앞 6절 1(concurrency 대기 취소 → `RUN_FAILED cancelled`) · 4(랭킹 없는 날 morning 댓글 하나 더) · 6(PRODUCT_PLAN O13 줄의 Next · zod 판). 실행 요약 문구는 계획 글자(`건너뜀 YYYY-MM-DD`)와 달리 `건너뜀 · 오늘 랭킹 있음 YYYY-MM-DD`이지만 날짜 한 줄이라 그대로 둬도 됨.
다음: 사용자 토큰 · Vercel env → 커밋 · 푸시 → 줄 10(배포 뒤 머리 없음 401) · 11(다음 날 아침) · 12(사용자 시험 이슈 알림) · 13.
