# PLAN_ops_O9_daily_trigger_alert — 아침 갱신 정시 시작 · 운영 경보

> 이유: ① 04:17 예약(`daily.yml` schedule)이 실제로는 늦게 시작한다 — 10-18~11-14 28일 중앙값 약 3시간 30분 늦음, 07:00 전 완료 4일뿐, 11-03 · 11-10 · 11-12는 10:00 넘어 끝나 그날 아침 알림이 규칙(`push-run.ts` 10:00 지남)상 가지 않았다. GitHub 문서도 예약은 부하 때 늦거나 빠질 수 있다고 적는다. ② 실패가 조용히 지나간다 — 11-04 · 11-13 · 11-14 daily가 종료 코드 1로 끝났고, 11-13 · 11-14는 랭킹 30개 전부 요약 없음 · 자동 주가 기사가 상위에 올라옴(AI 정리 실패 → 규칙 묶기로 보임)인데 HANDOFF에도 기록이 없다. 그래서 ① Vercel Cron이 매일 04시대에 GitHub API로 daily를 바로 실행시키고(기존 예약은 안전망) ② 실행이 끝날 때와 08시대에 상태를 검사해 문제가 있으면 GitHub 이슈로 알린다.
> 코드 폴더: `ex12/`(운영 폴더, 복사 없음 — PLAN.md D21) + 루트 `.github/workflows/`. 보고: `reports/ops_O9_daily_trigger_alert_build.md` · `reports/ops_O9_daily_trigger_alert_judge.md`.
> SPEC 근거: **US-01 · AC-01-3 · AC-01-4**(새 — 2026-11-14 SPEC.md 반영). 회귀: AC-01-1 · 01-2 · AC-14-2(하루 1통 · 06:30~10:00) · AC-11-1 · AC-05-1. 적용 결정: D3 · D5(자동 재시도 없음 · 수동 재실행 최대 2회) · D10 · D21 · D30 · **D37**(확정 — 사용자 2026-11-14).
> 상태: **확정** — 다음 = 제작(`agent_prompts/2_build_agent.md`).

## 이번 범위
- 정시 시작: Vercel Cron `GET /api/cron/daily`(04:00~04:59 KST) → GitHub `workflow_dispatch`(daily.yml, `trigger=cron`). 기존 schedule은 05:17 KST 안전망으로, 오늘 랭킹이 이미 있으면 바로 끝남.
- 경보: 새 `npm run health` + 새 `.github/workflows/health.yml`(daily가 끝나면 + Vercel Cron `GET /api/cron/health` 08:00~08:59 KST). 문제가 있으면 그날 이슈 `운영 경보 YYYY-MM-DD`를 열거나 댓글.
- 새 패키지: 없음(GitHub 호출은 `fetch`, 이슈는 러너의 `gh`).
- 판 올림(코디네이터 결정 2026-11-14 제작 중 — D38 O13 수락 범위를 앞당김): `next` 16.4.0 → **16.4.1** · `zod` 4.6.5 → **4.6.6** · `hono` 4.13.13 → **4.13.16** · `@hono/node-server` 2.1.3 → **2.1.6**(모두 같은 줄 안 패치, 다른 잠금 항목 변화 = next의 `@next/*` 묶음뿐). 이유: Next 16.4.0은 보안 공지 때문에 `next build`가 번갈아 멈춰(`SecurityFatalError`) O9 배포 자체가 막힐 수 있음 · Hono 공지 GHSA-h7mg-cq63-vhpq(RegExpRouter가 와일드카드 미들웨어를 건너뜀)는 북마크 · 폴더 로그인 확인(`app.use('/api/*')`)에 걸릴 수 있음. 각 판은 npm 공지 목록에서 남은 공지 0을 확인한 가장 낮은 판. drizzle-kit 개발 도구 쪽 중간 4건은 큰 판 변경이라 O13으로 둠.
- 하지 않음: 11-13~ OpenAI 실패 원인 고치기(사용자가 실행 요약 확인 뒤 따로) · 자동 재실행(D5) · 호출 간격 · 남는 크레딧(O12) · AI 짧은 제목 · 화면 문구(O10) · 운영자 기기 푸시 경보(사용자 결정: 이슈만).

## 정시 시작 (Vercel Cron → GitHub)

| 항목 | 값 |
|---|---|
| `vercel.json` crons | `{"path":"/api/cron/daily","schedule":"0 19 * * *"}` · `{"path":"/api/cron/health","schedule":"0 23 * * *"}`(UTC — KST 04시대 · 08시대, Hobby는 그 시 안 아무 때나) · `regions` 그대로 |
| 경로 | Next `app/api/cron/[job]/route.ts`(GET만, `runtime nodejs` · `dynamic force-dynamic`), 판단은 `server/cron.ts`(테스트 대상). job = `daily` → `daily.yml`(inputs `{trigger:"cron"}`) · `health` → `health.yml`(inputs `{mode:"morning"}`), 그 밖 404 |
| 인증 | `CRON_SECRET` 없으면 503 `{"message":"CRON_NOT_CONFIGURED"}` · `Authorization`이 `Bearer {CRON_SECRET}`과 다르면(길이 같게 맞춘 상수 시간 비교) 401 `{"message":"UNAUTHORIZED"}` · 그다음 `GITHUB_DISPATCH_TOKEN` 없으면 503 같은 문구 |
| GitHub 호출 | `POST https://api.github.com/repos/Jwavascript/issuerank/actions/workflows/{파일}/dispatches` 본문 `{"ref":"main","inputs":…}` · 머리 `Authorization: Bearer {토큰}` · `Accept: application/vnd.github+json` · `X-GitHub-Api-Version: 2022-11-28` · `User-Agent: issuerank-cron` · 10초 제한 · 재시도 0. 200 · 204 → 202 `{"dispatched":"daily"}`, 그 밖 · 시간 초과 → 502 `{"message":"DISPATCH_FAILED","status":<GitHub 상태 또는 0>}` |
| 응답 · 로그 | 모두 `Cache-Control: no-store` · 쿠키 0. 로그는 `CRON_DISPATCH_FAILED {상태}` 한 줄뿐(토큰 · 비밀값 · 응답 본문 0) |
| 저장소 · 파일 이름 | 코드 상수(env로 받지 않음) |

## daily.yml 바뀌는 곳
- `on.schedule` cron `17 19 * * *` → `17 20 * * *`(05:17 KST 안전망). `workflow_dispatch.inputs.trigger`(string, 기본 `manual`) 추가.
- `npm ci` 다음, `npm run daily` 앞에 **오늘 확인** 단계: `event == schedule` 또는 `inputs.trigger == cron`이고 첫 시도(`run_attempt == 1`)일 때만(Re-run은 사람이 누른 것 — 심사 반영 2026-11-14) `npm run --silent today` → 종료 0 = 오늘(KST) 랭킹 있음 → 실행 요약에 `오늘 랭킹이 이미 있어 건너뜀 YYYY-MM-DD` 쓰고 daily 단계 건너뜀(성공) · 1 = 없음 → daily 실행 · 2 = 확인 오류 → daily 실행(안 돌리는 쪽이 더 나쁨). 사람이 누른 수동 실행(`manual`)은 확인 없이 지금처럼(D5 수동 재실행 최대 2회 그대로).
- `npm run today` = 새 `server/today.ts`: 그날 `issues` 행이 1개 이상이면 0, 0개면 1, DB 오류 2. 출력은 `오늘 랭킹 있음|없음 YYYY-MM-DD` 또는 `DB 오류: …`(hide 거침).
- 그 밖 단계 · `concurrency` · timeout · 비밀값 줄은 그대로. push.yml은 바꾸지 않음(daily가 일찍 끝나면 기존 "06:30까지 기다림"이 처음으로 쓰임).

## 경보 (health)

| 코드 | 언제 문제인가(KST 오늘 기준) |
|---|---|
| `NO_RANKING` | 그날 `issues` 0행 |
| `RUN_FAILED` | daily 결론이 success가 아님(`--run {결론}`으로 받을 때만) |
| `COLLECT_FAILED` | 그날 가장 늦은 수집 실행(같은 `finished_at`)에 `failed` 섹션이 있음 — 문구에 섹션 키 |
| `AI_FALLBACK` | 그날 이슈가 있고 `importance`가 전부 0(AI 정리 실패 → 규칙 묶기) |
| `SUMMARY_LOW` | 그날 섹션별 Top 5(D16, 최대 30) 중 요약 없음 8개 이상 — 문구에 `요약 없음 n/30` |
| `ALL_ONES` | 어느 섹션 Top 5(5개)가 모두 연관기사 1건 — 섹션 키 |
| `LATE` | 그날 ok 수집 완료 시각(가장 늦은 것)이 07:00 KST 이후 — 문구에 시각 |
| `CHECK_ERROR` | DB를 못 읽음 |

- `npm run health [-- --run <결론>] [--mode after|morning]`: 새 `server/health.ts`(명령) + `server/health-check.ts`(판단만, DB · env 없이 행을 받아 코드 목록을 돌려줌 — 테스트 대상). `morning`은 `NO_RANKING`만 본다(같은 날 두 번 같은 경보 방지). 출력 = 첫 줄 `상태 OK` 또는 `상태 문제 n` + 코드마다 한 줄. 종료 0 OK · 1 문제 · 2 `CHECK_ERROR`. 출력에 비밀값 · 기사 제목 · 링크 0(숫자 · 코드 · 섹션 키 · 시각뿐).
- `health.yml`: `on: workflow_run [daily] completed` + `workflow_dispatch(inputs.mode: after|morning, 기본 after; test: boolean)`. `permissions: contents read · issues write`. `concurrency: health`. timeout 10분. 단계 = checkout · setup-node · `npm ci` · `npm run --silent health -- --run "$CONCLUSION" --mode "$MODE"`(값은 env로) → 출력을 실행 요약에 붙임 → 종료 코드가 0이 아니면 `gh`로 제목 `운영 경보 {날짜}` 열린 이슈를 찾아 있으면 댓글, 없으면 새 이슈. 본문 = health 출력 + 실행 주소 + `@Jwavascript`(알림용). 이 워크플로 자체는 경보를 냈어도 성공으로 끝남(빨간 표시는 daily 몫).
- `test: true` 수동 실행 = DB를 보지 않고 제목 `운영 경보 시험 {날짜}` 이슈 하나(사용자 줄에서 휴대폰 알림 확인용).
- 이슈 닫기는 사용자가 한다(자동 닫기 없음). 저장소가 공개라 이슈도 공개 — 내용은 위 출력뿐.

## 사용자가 할 일 (제작 전 또는 제작 중, 값은 채팅 · 저장소에 두지 않음)
1. GitHub › Settings › Developer settings › Fine-grained token: 저장소 `Jwavascript/issuerank` 하나만 · 권한 **Actions: Read and write**(그 밖 없음) · 만료 1년(만료일을 달력에).
2. Vercel › issuerank › Settings › Environment Variables(Production): `GITHUB_DISPATCH_TOKEN` = 1의 토큰 · `CRON_SECRET` = 영문 · 숫자 32자 이상 무작위(둘 다 Sensitive). 푸시 뒤 배포가 이 값을 가져감(이미 배포됐으면 Redeploy).
3. GitHub Secrets 추가 없음(이슈는 워크플로 기본 토큰).

## SPEC · PLAN.md 반영 (2026-11-14 완료 — 계획 역할)
- SPEC.md US-01에 AC-01-3 · AC-01-4. PLAN.md 5절 D37(O9) · D38(O10~ 로드맵 · AI 짧은 제목 · 화면 문구 결정) · D3 · D5 보충 줄. `plan/PRODUCT_PLAN.md` 로드맵. HANDOFF 1줄.

## 화면 변경
- 없음(웹 · 앱 화면 0).

## 구현 순서
1. `server/cron.ts` + `app/api/cron/[job]/route.ts` + `vercel.json` crons.
2. `server/today.ts`(+ store 읽기 함수) · package.json `today` · `health` 스크립트.
3. `server/health-check.ts` · `server/health.ts`.
4. `.github/workflows/daily.yml` 바뀌는 곳 · 새 `health.yml`.
5. 테스트 `test/cron.test.ts` · `test/health.test.ts` · `test/workflows-o9.test.ts`(워크플로 글자 검사).
6. `openapi.yaml`에 `/api/cron/{job}`(내부용 표시) · 판 올림.
마지막. `npm test` · `npm run build`.

## 검증 방법
| # | 확인 | 방법 | 기대 결과 |
|---|---|---|---|
| 1 | 변경 범위 | `git status` · diff | 바뀐 것 = 위 파일 + 문서. web/ · android/ · ex01~11 0 |
| 2 | cron 인증 | 단위: 비밀값 없음 · 머리 없음 · 틀림 · 길이 다름 · 맞음 | 503 · 401 · 401 · 401 · (토큰 없음) 503 |
| 3 | cron 호출 | 단위: 가짜 fetch가 받은 주소 · 본문 · 머리 | daily → `daily.yml` `{"ref":"main","inputs":{"trigger":"cron"}}`, health → `health.yml` `{mode:"morning"}`, `job=x` 404 · fetch 0 |
| 4 | cron 실패 | 단위: GitHub 401 · 500 · 시간 초과 | 502 `DISPATCH_FAILED` 상태 401 · 500 · 0, 응답 · 로그에 토큰 글자 0 |
| 5 | today | 단위(메모리 행) | 오늘 행 있음 0 · 없음 1 · DB 오류 2 |
| 6 | health 판단 | 단위: 코드마다 걸리는 행 · 안 걸리는 행(경계: 요약 없음 7/8 · 07:00 정각/07:01 · importance 하나라도 1) | 기대 코드만 나옴 · morning은 NO_RANKING만 |
| 7 | 11-13 재현 | 단위: importance 전부 0 · 요약 30개 null | `AI_FALLBACK` · `SUMMARY_LOW 30/30` |
| 8 | 워크플로 글자 | 테스트로 YAML 읽기 | daily: schedule `17 20 * * *` · 확인 단계 조건(schedule · cron만) · 기존 단계 그대로 / health: 트리거 2 · 권한 2 · 입력은 env로만 |
| 9 | 테스트 · 빌드 | `npm test` · `npm run build` | 모두 통과(이 PC CRLF 3파일은 LF 사본으로 — 기존 기록과 구분) |
| 10 | 배포 cron 보호 | 푸시 · 배포 뒤 Node fetch `GET /api/cron/daily`(머리 없음) 1번 | 401 · daily 실행 생기지 않음 |
| 11 | 첫 아침 | 다음 날 공개 Actions API | daily `workflow_dispatch` 실행 04:00~05:10 KST 시작 · 05:17 안전망 실행은 `건너뜀`(있다면) · push가 06:30까지 기다린 뒤 보냄 |
| 12 | 경보 시험 | 사용자: Actions › health › Run workflow `test` 켬 1번 | 이슈 `운영 경보 시험` 생김 · 휴대폰(GitHub 앱 · 메일) 알림 도착 |
| 13 | 실제 경보 | 다음 문제 날 또는 11-13 같은 날 | 그날 이슈에 코드 줄 |

### 사용자 확인 (마지막에 한 번에)
- 줄 12(시험 이슈 알림이 휴대폰에 오는지) · 다음 날 아침 06:30~07:00 사이 아침 알림이 왔는지.
