# O14 제작 보고 — 관심 키워드 타임라인

2026-11-15 · Windows/PowerShell · Node26.13.0/npm11.20.0. 사용자 요청의 계획→제작→검증 중 제작 기록. 같은 세션 역할 순차 수행, 서브에이전트 없음.

## 1. 범위와 구현 위치

기준 `plan/PLAN_ops_O14_keyword_timeline.md`·D40·SPEC US16. 웹 검색·비교 타임라인·관심 저장·아침 웹 푸시 연동을 ex12에서 구현했다. 운영에 배포하거나 DB를 변경하지 않았다.

| 항목 | 반영 위치 |
|---|---|
| 공개 검색·KST 날짜·구절 경계·중복·주요 선정·20개 커서 | ex12/server/timeline.ts |
| 관심/기기 API·세션·Origin·4KB·상한·소유권·no-store | server/routes/keywords.ts, app.ts, bff.ts, index.ts |
| DB 조회·사용자 행 잠금·기기 증명·세션 만료·발송 준비 | server/db/keywords.ts, server/db/store.ts |
| 새 표2개·FK·RLS·고정 색상 슬롯 | server/db/schema.ts, drizzle/0008_keyword_timeline.sql, meta/0008_snapshot.json, meta/_journal.json |
| 등록/알림 켜기 baseline·기기별 발송 이력·개인 payload | server/keyword-store.ts, push-run.ts |
| 키워드 진입·접히는 기간 설정·비교·출처·설정·오류·계정 전환 | public/index.html, keywords.js, keywords.css |
| 수집 범위·요약 없음·선택적 계정 연결 안내 | public/keywords.js, public/privacy.html |
| API 계약 | ex12/openapi.yaml |
| 기능/회귀/마이그레이션 검사 | test/timeline.test.ts, keywords-api.test.ts, keyword-notification.test.ts, keyword-migration.test.ts, helpers/keyword-fixture.ts, push.test.ts |

주요 기준은 출처2개 또는 제목의 명시적 발표·결정·시행 단서이며 키워드별 하루3개다. 기존 importance를 개인 중요도로 사용하지 않는다. 모든 관련 보기도 제공한다. 최근48시간 추정 기사 수의 누적 합산·증가량 알림은 추가하지 않았다.

기존 issue_id 묶음을 사용하고 수집일이 달라도 URL이 같거나 보도일·제목·비어 있지 않은 설명문이 동일할 때만 중복 처리한다. 의미가 비슷한 사건의 자동 연결과 새 사실의 AI 판정은 보류한다. 새 제목/정형 반복 제목을 함부로 합치지 않는 회귀 사례를 추가했다. 초안의 “이전 흐름 보기”를 의미 추정으로 만들어 내지 않았다.

알림은 기존06:30~10:00·하루1통·mark/revert·410 정리를 유지한다. 실제 구독 암호화값으로 기기 연결을 증명하고 활성 세션에 연결한다. 등록·알림 켜기 때 **주요 여부와 무관한 기존 관련 보도 전체**를 baseline으로 삼는다. 개인 후보는 최근3일 보도 중 활성화 이후 수집분이다. 성공/응답불명은 확인 이력을 저장하고 확정 실패는 재시도 가능하다. 본문에 검색어를 넣지 않고 알림 이동은 본인 저장 목록의 불투명ID·기간으로 복원한다. 3개를 넘는 알림 키워드는 저장 목록에서 선택을 바꿔 본다.

## 2. 보존·패키지·설정

- 삭제한 기존 기능·파일 없음. 기존 랭킹·북마크 API의 카드 칸과 섹션 키 유지. ex01~ex11·frontend·android·web 변경 없음.
- 시작 시 확인한 사용자 변경 중 경제 수집 관련 collect/newsdata/collect.test 및 Android 작업은 수정하지 않았다. 이미 수정 중인 문서에는 O14 부분만 추가했다. 커밋·푸시 없음.
- 새 패키지·외부 계정·뉴스/AI 호출·실제 푸시0. .env 값 읽기0. 이 PC에는 ex12/.env가 없다. 기존 DB·인증·VAPID 설정을 쓰므로 새 키 이름은 없다.
- 배포 전0008을 먼저 적용해야 한다. 키워드 기기 표를 읽는 기존 발송 명령도 함께 바뀌므로 코드만 먼저 운영에 반영하면 안 된다.
- Node engines는24.x이나 이번 PC는26.13.0이다. 판을 변경하지 않았고 현재 PC 결과로만 보고한다.

## 3. 실제 검증

| 명령/항목 | 결과 |
|---|---|
| ex12: npm.cmd test -- --reporter=json --outputFile=.data/o14-verified-tests.json | **391/391 통과, 40파일, 실패0** |
| ex12: npm.cmd run build | **종료0**, Next 최적화 빌드·TypeScript 모두 통과 |
| node --check public/keywords.js | 종료0 |
| 설치된 Prettier로 openapi.yaml을 parser:yaml로 파싱 | OPENAPI_YAML_SYNTAX_OK |
| Drizzle0007→0008/현 스키마 비교 | 기존 표 그대로·추가2표·생성 SQL 일치·미반영 스키마 변경0. **실제 DB 실행과는 다름** |
| 브라우저 | 최종 심사에서 Next3000+fixture Hono8787·별도 Chrome으로 재확인하고 심사 보고에 기록 |
| 실제 DB·운영 배포·실기기 개인 푸시 | **SKIP**, .env/로컬 PostgreSQL 없음·미배포. 실제 데이터에 fixture를 넣지 않음 |

시험은 모두 현재 PC에서 실행했다. 가짜 뉴스·구독·키는 테스트 메모리만 사용했다. 주요 신규 사례: 한글 조사/동명이름 접두어·영문단어 경계, 비연결/Top5밖, KST 날짜, 중복URL·다른 보도일 정형 제목, 공통 사건의 하루 상한, 양방향 커서, 계정 분리·baseline 비공개, Origin·4KB, idempotent 알림 켜기, 등록 전 보도 제외, 알림 대상 복원, 개인/일반 통합 payload,500/timeout/반복 발송.

## 4. 검증 중 실제 문제와 수정

1. 기본 Vitest가 .data/o9a-economy/lf-check의 과거 사본도 수집했다. vitest.config.ts에 test/**/*.test.ts를 명시했다.
2. 기존 pwa/push-ui/summary-text의 LF 고정 문자열 검사가 Windows CRLF에서 실패했다. 해당 텍스트 읽기에 CRLF→LF 정규화를 적용했다. 제품 동작 기대는 유지하되 개인정보 문장은 O14 실제 처리에 맞춰 검사한다.
3. 샌드박스의 tsx가 os.userInfo에서 uv_os_get_passwd ENOMEM으로 제품 진입 전에 실패했다. 같은 시험을 자동 승인된 샌드박스 밖 실행으로 검증했다. 권한 설정·SDK·Node 판은 바꾸지 않았다. 최종391개에는 실제 Better Auth 시작 실패 시험도 포함한다.
4. 모바일의 긴 설정이 뉴스 첫 카드를 아래로 밀어 기간·정렬·보기 설정을 접을 수 있게 했다.
5. 개인 알림 이동이 단순히 저장 순서 앞3개를 고르지 않도록, 실제 알림 대상 ID를 본인 저장 목록과 대조하도록 보완했다.
6. 첫 심사에서 임시 검색어를 빼고 넣다 색상을 재사용하는 경계를 발견했다. 제작 역할로 돌아가 임시 색상 번호를 재사용하지 않게 고치고 저장 색상 슬롯과 분리했다. 키워드 교체·재선택·저장 슬롯 충돌 방지 회귀1개(test/keyword-colors.test.ts)를 더해391개·빌드를 재검증했다. 그 뒤 최종 심사를 다시 시작하며 해시를 새로 기록했다.

## 5. 남은 확인과 실제 한계

- 운영 DB0008 적용 후 PostgreSQL 행 잠금·FK·RLS·동시 상한·실제 세션 삭제를 확인해야 한다. 메모리 API 시험이나 SQL 생성 확인으로 대체하지 않는다.
- 운영 저장 뉴스에서 관심 기업별 검색량·오탐/누락과 주요 선정 품질을 사람이 검토해야 한다. 현재 수집 표본 밖의 뉴스는 찾지 못한다.
- 실제 브라우저 구독 연결 및 다음 아침 개인 푸시·로그아웃 후 미발송은 배포 뒤 확인해야 한다. Android 앱 기능은 이번에 변경하지 않았다.
- 제목의 구절·명시적 단서 기반 첫 구현이다. 의미 기반 사건 연결/새 사실 판정과 별칭 처리는 후속 범위다.
- 계정 삭제 기능(O13)을 만들 때는 연결된 기기의 일반 아침 알림 설정과 연결 정리도 함께 다뤄야 한다.

다음 역할: 제품 코드를 고치지 않는 심사. 파일 해시를 .data/o14-review-hashes.json에 기록했다. SKIP이 남으므로 전체 PASS로 표현하지 않는다.
