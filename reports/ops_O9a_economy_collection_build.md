# O9a 경제 수집 표본 보완 — 제작 보고

2026-11-15, Windows PowerShell · Node26.13.0 · npm11.20.0. 기준 `plan/PLAN_ops_O9a_economy_collection.md`, SPEC AC-01-1·01-2·01-5, PLAN D39. 사용자 요청 범위에서 계획→제작을 같은 Codex 세션에서 순차 수행했다. 기존 Android·보고서 변경을 보존했다.

## 결과

- `ex12/server/newsdata.ts`: 경제 top 요청에만 `excludedomainurl=kr.investing.com`. 전체 출처 최대2페이지는 그대로라 Investing 일반 경제·실적 기사도 후보가 될 수 있다. 요청·페이지 한도는 증가하지 않는다.
- source_id=investing_kr, 임원 직함, 주식/보통주/자사주, 수량 또는 달러금액, 매매동작으로 끝나는 제목 조합만 저장 전에 제외한다. 제공한 임원 매매 제목8건 모두 해당. 직함 위치 검사로 ‘이사회’를 개인 직함으로 오인하지 않는다. 타출처·자사주 정책·실적·시장 전반 매도세 보도는 유지한다.
- `filtered`는 섹션별 제외 **응답** 횟수다. 같은 알림이 여러 응답에 있으면 각각 세고, DB에는 새 필드를 저장하지 않는다. `collect.ts`가 0이 아닌 섹션 로그에만 `매매 알림 제외 응답 N건`을 붙인다.
- 필터 후 유효 기사가0이면 failed로 이전 기사를 보존한다. 빈 top 뒤 전체 출처가 정상이라면 계속 수집한다. 전체 페이지 중간 실패도 기존과 같이 그 섹션의 부분 결과를 저장하지 않는다.
- 새 패키지·환경변수·DB 마이그레이션 없음. 기존 코드·기능 삭제 없음. AI 모델·지시문·분류·연관기사 수 계산·순위·화면·Android 제품코드·워크플로 변경 없음. 기존 수집 기사·이슈·요약·북마크를 삭제하거나 daily를 재실행하지 않았다.

## 검증 명령과 이번 결과

| 실행 | 결과 |
|---|---|
| 변경 전 `ex12/`에서 `npm.cmd test` | 35파일 중4파일 실패. 실행된341개 중338통과·3실패, push-ui는 로딩 실패. auth는 제한 실행환경 자식 프로세스 문제, 나머지는 기존 CRLF 검사 문제 |
| `npx.cmd vitest run test/auth.test.ts --reporter=dot`(제한 밖) | 13/13 통과. 환경 실패임을 분리 |
| `npx.cmd vitest run test/collect.test.ts --reporter=dot` | **13/13 통과**, 실제 제목8개·유지반례·요청18회·페이지·실패보존 포함 |
| 변경 후 `npm.cmd test -- --reporter=json --outputFile=.data/o9a-economy/tests.json`(제한 밖) | 실행된346개 중344통과·2실패 + push-ui 로딩 실패. auth 통과. 실패는 변경 전과 같은 pwa·summary-text·push-ui 줄바꿈 검사이며 새 수집 실패 없음 |
| `npm.cmd run build` | **성공**, Next 프로덕션 빌드·Next 타입검사·tsc --noEmit 완료 |
| 별도 LF 사본에서 `npm.cmd test -- --reporter=json --outputFile=../lf-tests.json`(제한 밖) | **361/361 통과**, 로딩 실패0. 원본 워킹 트리 결과와 구분해야 함 |
| `git diff --check` | 성공(기존 LF→CRLF 안내 경고만) |
| 실제 API 확인(계획 줄6) | **SKIP**: ex12/.env 없음. 공식 문서 파라미터와 fake 요청만 확인. API가 실제로 개선된 표본을 주는지는 미확인 |
| 배포 후 다음 하루 실행(줄7) | **SKIP**: 커밋·푸시·배포 요청 없음. 운영은 현재 기존 규칙 |

LF 사본 경로 `ex12/.data/o9a-economy/lf-check/`. ex12 소스·fixture·도구·설정을 복사하고 `node_modules`는 기존 설치 경로에 junction 연결. `.env`·기존 `.data`·`.next`는 복사하지 않았다. public/index.html·widget.html·offline.html과 openapi.yaml **사본만** LF로 변환, 워크플로 검사에 필요한 `.github/workflows/{daily,health,push}.yml`도 사본의 부모에 복사했다. 준비 중 누락 워크플로·혼합 줄바꿈으로 실패한 중간 실행은 통과로 세지 않았고 마지막 결과가 361/361이다. 원본 줄바꿈·의존성·테스트 단정은 바꾸지 않았다.

실제 newsdata·OpenAI 요청0, DB 조회·쓰기0. 운영 계정 로그인0. 시험용 서버 기동0. 정리할 DB행·서버 프로세스 없음. 증거 JSON은 `.data/o9a-economy/{tests,lf-tests}.json`에 있으며 git 제외 대상이다.

## 남은 확인

실제 API 호환·경제 표본 개선·ALL_ONES 해소는 아직 입증되지 않았다. top 출처 혼합은 유효 경제 표본을 더 확보하려는 조치이며 어떤 사건이 몇 건 검색되는지 보장하지 않는다. 기사별 재분류·검색 응답 원 건수 및 비율 진단, 제목·요약 불일치 정황은 기존 조사와 O11에서 따로 다룬다. 독립 심사는 새 세션에서 수행한다.
