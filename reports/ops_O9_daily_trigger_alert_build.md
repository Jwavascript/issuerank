# ops O9 제작 보고 — 아침 갱신 정시 시작 · 운영 경보

- 날짜: 2026-11-14 · PC Windows(Node 24.22.2) · 계획 `plan/PLAN_ops_O9_daily_trigger_alert.md`(확정 · D37 · D38 · SPEC AC-01-3 · 01-4)
- 상태: **제작 로컬 완료 · 작업 트리에만**(커밋 · 푸시 0). 다음 = 독립 심사(새 세션, `agent_prompts/3_judge_agent.md`) → 사용자 토큰 · Vercel env → 커밋 · 푸시 → 배포 줄 10 · 다음 날 줄 11 → 사용자 줄 12.

## 1. 계기 (11-14 다시 본 운영 기록, 공개 Actions API · 공개 GET만)
- daily 예약 시작(KST): 11-06 09:11 · 07 08:43 · 08 07:42 · 09 07:51 · 10 **11:03** · 11 09:01 · 12 **10:30** · 13 09:18 · 14 09:19 → 10:00 뒤 끝난 날(그날 아침 알림 없음) 11-03 · 10 · 12.
- 실패(종료 1): 11-04 · **11-13 · 11-14**(각 약 2분 — 평소 4분). 11-13 · 14 랭킹은 30개 **전부 요약 없음**, 상위에 자동 주가 기사(`Rosenblatt, 수요 호조에…`, `실적 발표: Altius Tele…`) → AI 정리 실패로 규칙 묶기 + 요약 전부 실패로 보임 = **OpenAI 호출이 11-13부터 실패**. 원인 문구는 로그인해야 보이는 실행 요약(`AI 정리 실패: …` 줄)에만 있음 → 사용자 확인 필요(O9 범위 밖).

## 2. 계획 항목별 반영 위치
| 계획 | 파일 |
|---|---|
| Vercel Cron 2개 | `ex12/vercel.json`(`0 19 * * *` daily · `0 23 * * *` health, regions 그대로) |
| cron 경로 · 판단 | `ex12/app/api/cron/[job]/route.ts`(GET · nodejs · force-dynamic) · `ex12/server/cron.ts`(404 → 503 → 401(sha256 + timingSafeEqual) → 503 → GitHub POST, 200 · 204 = 202, 그 밖 · 던짐 = 502 + 상태, 로그 `CRON_DISPATCH_FAILED {상태}`만, 모든 응답 no-store) |
| 오늘 확인 | `ex12/server/today.ts` · `health-check.ts` `todayResult` · `db/store.ts` `healthStore.issueCount` · package.json `today` |
| 경보 판단 | `ex12/server/health-check.ts`(`checkHealth` · `formatHealth` · `parseHealthArgs`) · `ex12/server/health.ts` · `healthStore.day` · package.json `health` |
| daily.yml | schedule `17 20 * * *`(05:17 안전망) · `workflow_dispatch.inputs.trigger`(기본 manual) · `npm ci` 뒤 "오늘 확인" 단계(schedule · cron만, 종료 0이면 skip + 실행 요약 한 줄) · daily 단계 `if: steps.guard.outputs.skip != 'true'`. 그 밖 그대로 |
| health.yml(새) | workflow_run [daily] + 수동(mode after · morning, test). 권한 contents read · issues write. 문제면 `운영 경보 {KST 날짜}` 열린 이슈에 댓글(같은 출력이 이미 있으면 생략) 또는 새 이슈, 본문 = health 출력 + 실행 주소 + `@Jwavascript`. test = DB 없이 `운영 경보 시험 {날짜}` |
| openapi | `/api/cron/{job}` 내부용 · 0.8.0 → 0.9.0 |
| 판 올림(코디네이터 결정) | next 16.4.1 · zod 4.6.6 · hono 4.13.16 · @hono/node-server 2.1.6 — 계획 "이번 범위" 둘째 줄에 이유 |

지운 것: 없음. 새 패키지: 없음.

## 3. 검증 (계획 검증 표 번호)
| # | 결과 | 근거 |
|---|---|---|
| 1 | OK | `git status`: O9 파일 + 문서(SPEC · PLAN · PRODUCT_PLAN · HANDOFF · 이 보고)만. `android/` 8개 · `reports/ops_O8_android_app_build.md`는 작업 전부터 있던 미커밋(O8) — 손대지 않음. web/ · ex01~11 0 |
| 2~4 | OK | `test/cron.test.ts` 8개: 없는 job 4가지(`constructor` · `__proto__` 포함) 404 · 비밀값 없음 503 · 401 5가지 · 토큰 없음 503(모두 fetch 0) · 주소 · 본문 · 머리 4개 · 타임아웃 신호 · health 입력 · GitHub 401/404/500/던짐 → 502 + 상태, 응답 · 로그에 토큰 · 비밀값 글자 0 · no-store · 쿠키 0 |
| 5 · 6 · 7 | OK | `test/health.test.ts` 13개: 코드마다 걸림/안 걸림 경계(요약 없음 7/8 · 07:00/07:01 · importance 하나만 1 · 앞 수집 실행의 실패 안 셈 · 5개 미만 섹션 안 셈) · morning = NO_RANKING만 · 11-13 재현 출력 5줄 정확히 · 인수 · today 0/1 |
| 8 | OK | `test/workflows-o9.test.ts` 9개(daily 예약 1개 · 확인 단계 위치 · 조건 · skip · DATABASE_URL만 / health 트리거 · 권한 · run 줄에 `${{` 0 / push.yml 이어짐 그대로 / vercel.json 전체) |
| 9 | OK | **LF 사본** `npm test` 35파일 **356/356**(새 30). 이 PC 작업 사본 그대로는 32/35 — 실패 3파일(`pwa` · `summary-text` · `push-ui`)은 기대 문자열 `\n` vs CRLF로 HANDOFF 기록과 같은 환경 원인. `npm run build` 2번 연속 exit 0(Next.js 16.4.1, 보안 멈춤 0, 경로 표에 `/api/cron/[job]`) |
| 추가 | OK | 로컬 `next start`(127.0.0.1:3101, DB · GitHub 토큰 없음, CRON_SECRET 임시값): `/api/cron/daily` 머리 없음 401 · 틀림 401 · 맞음 503(토큰 없음) · health 503 · nope 404, 모두 no-store → catch-all BFF가 아니라 새 경로가 받음. GitHub 호출 0. 띄운 프로세스 종료 확인 |
| 10~13 | SKIP | 푸시 · 배포 뒤(10 · 11) · 사용자(12) · 다음 문제 날(13) |

합계 OK 9/FAIL 0/SKIP 4(10~13).

### 3.1 심사 반영(2026-11-14, `reports/ops_O9_daily_trigger_alert_judge.md` FAIL — 문서 1건)
- [필수] PLAN.md D3 보충 · D5 보충 줄 추가(04시대 cron 시작 · 05:17 안전망 · 첫 시도만 오늘 확인 · Re-run은 그대로 · 크레딧 216 주의 → 그런 날 수동 재실행 1번까지).
- [권장] `daily.yml` 오늘 확인 조건에 `&& github.run_attempt == 1`(Re-run이 처음 실행의 schedule · cron 값을 물려받아 건너뛰던 경우). 계획 문구도 같이.
- [참고 반영] 실행 요약 · 공개 이슈에 stdout만(`today` · `health`의 stderr = Node `.env not found` 안내 · 스택은 작업 로그로) · 이슈 찾기 `--search`(색인 지연) 대신 열린 이슈 200개에서 제목 정확 비교 · `today.ts` · `health.ts` `client.end()` 실패 무시.
- 그대로 둔 참고: concurrency 3겹 cancelled · 랭킹 없는 날 morning/after 출력 차이로 댓글 1개 더 · PRODUCT_PLAN O13 줄의 Next · zod 문구(O13 계획 때 정리).
- 다시 확인: LF 사본 35파일 356/356(테스트 수는 그대로 — 기존 시험에 단정 추가) · 빌드 exit 0 · 보안 멈춤 0. 새 시험 수는 처음 보고의 "30"이 아니라 **29**(심사 지적, 전체 356은 맞음).

### 3.2 커밋 · 푸시 · 배포 줄 10 (2026-11-14 23:0x KST, 사용자 "이어서 진행" — 토큰 · Vercel env 2개 넣었다고 함)
- 커밋 `c78b02b`(O9 22파일, HANDOFF는 O9 줄만 — O8 미커밋 `android/` 8 · O8 보고 · HANDOFF O8 기록은 작업 트리에 그대로) · main 푸시.
- 줄 10 **OK**: 배포 반영 23:05:54 KST(`/api/cron/nope` → 새 404 JSON). `/api/cron/daily` · `/api/cron/health` 머리 없음 401 · 틀린 값 401(503 아님 = 배포에 `CRON_SECRET` 있음) · no-store · 쿠키 0. 그 뒤 Actions 실행 목록에 새 daily · health 실행 0(잘못 시작 안 됨) · 워크플로 `health` active. 운영 요청 7(GET만).
- `GITHUB_DISPATCH_TOKEN`이 맞는지는 맞는 비밀값 호출 = Vercel cron만 할 수 있어 줄 11(다음 날 04시대)에서 드러남.

## 4. 실행 환경 메모 (다음 세션용)
- **vitest는 드라이브 문자가 대문자인 경로에서 돌려야 함**: VS Code 터미널의 `c:\Users\...`(소문자)에서 `npm test`를 돌리면 모든 파일이 `Vitest failed to find the runner`로 실패(모듈이 두 번 잡힘). PowerShell `Set-Location 'C:\Users\phoria\issuerank\ex12'` 뒤 실행하면 정상.
- npm 11이 esbuild postinstall을 승인 대기로 둠(`npm warn install-scripts`) — 이번 빌드 · 테스트에는 영향 없음, 잠금 파일 esbuild 판 변화 0.
- `npm install`이 package.json `engines` 줄 모양을 바꿈 → 두 번 되돌림(글자 diff = 스크립트 2줄 + 판 4줄뿐).

## 5. 필요했지만 없던 키 · 계정 (사용자)
1. GitHub fine-grained 토큰: 저장소 `Jwavascript/issuerank`만 · **Actions: Read and write** · 만료 1년.
2. Vercel Production env(둘 다 Sensitive): `GITHUB_DISPATCH_TOKEN` = 1 · `CRON_SECRET` = 영문 · 숫자 32자 이상. 푸시 전에 넣으면 그 배포가 가져감(뒤에 넣으면 Redeploy).
- 없으면: cron은 503으로 끝나고(아무 일도 안 함) 05:17 예약 안전망만 돎 = 지금보다 나빠지지 않음. health.yml은 토큰 없이 동작.

## 6. 추가로 보인 점
- **OpenAI 장애(1절)가 지금 가장 급함** — O9 경보가 있었다면 11-13 아침에 알았을 것. 실행 요약의 `AI 정리 실패:` 줄 · OpenAI Platform 사용량/결제 화면 확인 부탁.
- daily가 04시대에 끝나면 push.yml의 "06:30까지 기다림"이 운영에서 처음 쓰임(timeout 200분 안) → 줄 11에서 확인.
- 예약 안전망(05:17)이 정시 실행 뒤 돌면 daily는 건너뛰지만 push · health도 한 번 더 돎 — push는 기기별 그날 표시로 0통, health는 같은 출력이면 댓글 생략.
- Vercel 문서상 cron은 드물게 두 번 올 수 있음 → 두 번째 dispatch는 concurrency 대기 뒤 "오늘 확인"에서 건너뜀.
