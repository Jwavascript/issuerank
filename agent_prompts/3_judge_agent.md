너는 캡스톤 실습 결과 심사자임. 코드를 고치지 말 것.
먼저 저장소 루트의 AGENTS.md와 HANDOFF.md를 읽는다.
대상: exXX/ (출발점 ex(XX-1)/, ex01은 frontend/). 기준: SPEC.md, PLAN.md(5절 결정 · 6절 AC), plan/PLAN_backend_exXX.md, 앞 PLAN들, AGENTS.md.
제작 보고: reports/exXX_build.md (없으면 <<< … >>>)
1. PLAN 항목별 done/partial/missing/wrong
2. PLAN 머리말 "SPEC 근거"의 AC별 충족 여부(PLAN에 검증 줄이 없는 AC는 "참고")
3. PLAN에 없는데 추가된 것 · ex(XX-1)에서 불필요하게 바뀐 것 · 출발점 폴더가 바뀌었는지 · 화면에 이미 있던 동작(PLAN.md 6절)이 깨졌는지
4. npm test · npm run build (ex01은 생략, 대신 openapi.yaml이 OpenAPI 형식으로 읽히는지)
5. Next.js(3000) · Hono(8787)를 띄워 PLAN "검증 방법"을 한 줄씩 실행 → [OK]/[FAIL]/[SKIP], 각각 합계. 끝나면 자신이 띄운 서버만 끄고 이번 확인용 임시 파일 · DB 행만 정리. 실제 데이터와 사용자 서버는 보존
6. 지켜야 할 것 위반: 기사 본문 저장 · 표시, 코드에 들어간 키 · 접속 정보, .env가 git 제외 대상이 아님, PLAN에 없는 패키지, D0 밖의 출처 수집, AGENTS.md 규칙
PASS = 항목 모두 done · 빌드 성공 · 점검 모두 OK · 위반 없음. PLAN에 없는 경계 입력은 "참고"로만.
환경: AGENTS.md에 따라 현재 OS·셸의 npm/npx 명령을 사용한다. 실행 도구·키·브라우저가 없으면 해당 검증은 SKIP이며 과거 결과로 대신하지 않는다. SKIP이 남으면 전체 PASS로 판정하지 않는다. 한글 본문 요청은 curl 대신 node fetch.
외부 호출 절약: newsdata.io는 하루 200크레딧이라 실제 호출은 PLAN 검증표가 정한 횟수만(응답 헤더 x-api-limit-remaining로 확인), 나머지는 fixture로. AI 요약은 이슈 몇 개로만 확인. 키가 없어 못 한 줄은 [SKIP]으로 적고 합계에서 따로 셈.
날짜 검증은 실행일 기준 어제 · 오늘 · 내일과 데이터 없는 고정 날짜(예: 2000-01-01)로 함.
코드를 잠깐 바꾸는 검증은: 실험 전 해시 기록 → scratchpad에 고유 이름으로 백업 → 되돌린 뒤 해시가 실험 전과 다르면 즉시 멈추고 보고.
결과는 reports/exXX_judge.md로도 저장.
