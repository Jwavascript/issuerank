# O11·O12 랭킹 품질 개선 1차 — 제작

2026-11-17 사용자 요청. 기준 `plan/PLAN_ops_O11_O12_ranking_quality.md`·PLAN D41·SPEC AC02-3. Windows PowerShell, Node26.13.0/npm11.20.0. 프로젝트 권장 Node24와 다르지만 이 PC에서 아래 명령을 실제 실행했다. 설치된 의존성을 사용했고 패키지·lockfile은 바꾸지 않았다.

## 반영
| 계획 | 위치·결과 |
|---|---|
| 후보 확대 | collect.ts 기본4·옵션0~4, newsdata.ts 페이지 설명. top1+전체4 최대30요청, 기존 nextPage·중복·실패보존·O9a 필터 유지 |
| 간격·예산 | news-rate.ts 요청 전31초. collect/group CLI 모두 적용, 규칙 함수의 waitMs는0. daily.yml timeout60분. 뉴스 전체 최대66회, 운영 하루총3회 상한(다른 점검 호출이 있으면 잔액 우선) |
| 같은 사건 검증 | ai.ts 기존 MODEL·Responses·store:false·maxRetries0 유지. 기사별boolean, 출력1500토큰. grouping.ts 길이/형식 확인·빈제목false·거절/오류시낱말대체/종료1. 빈 결과 AI0, ai0 규칙 유지 |
| 근거 보관 | search-evidence.ts 허용칸·설명300자·최대10기사. 기준묶음·크기·검색어·원전체건수·낱말/AI판정수·판정배열·방식·최종추정 저장. group.ts 숫자로그·쓰기실패 안전코드. daily artifact14일·명시JSON만 숨김경로 허용 |
| 계약 보존 | 공개 API/DB schema/대표선정/동점순위/북마크보호/요약문구 변경0. D13 산식 유지, AI가 성공하면 관련비율만 교체 |

삭제한 기능·파일 없음. 작업 시작부터 있던 Android·O9a·O14 문서/코드를 보존했고 필요한 collect/newsdata/test에는 추가 변경만 했다. `web/`·과거ex01~11·frontend 접근/수정0. .env 직접 열람·뉴스/OpenAI 실제 호출·DB쓰기·커밋·푸시·배포0. 빌드 도구는 기존 .env를 자동 읽으나 값 출력0. 이번 검증은 키를 요구하지 않는 fixture·SDK HTTP mock만 사용했다.

## 실제 명령·결과
- `npm.cmd test`: 첫 제작 확인 393통과/1실패. 시간제한을30→60분으로 바꿨으나 구테스트 기대가30분이어서 실패, 기대를 승인한 계획과 맞췄다.
- `npm.cmd test -- --reporter=dot`: 중간404/404, 최종 **42파일405/405** 통과(22.70초). 수집30요청·검색36/AI36상한·AI부정/실패/빈결과·본문제외/키가림·타이머 경계·기존 북마크/랭킹 회귀 포함.
- `npm.cmd run build`: Next 컴파일/정적 생성 후 보안 취약점 알림으로 종료1. 같은 명령 재실행은 보안 경고를 유지하며 **종료0**, Next 빌드 및 tsc 통과. 수정 후 최종 빌드도 같은 알림/재실행 성공을 확인했다. 기존 Next16.4.1 보안 업그레이드 권고는 남아 있으며 이번 범위에서 버전을 임의 변경하지 않았다.
- `npm.cmd exec tsc -- --noEmit`: 마지막 시험 추가 후 종료0.
- `git diff --check -- <이번 tracked 변경 경로>`: 종료0. CRLF 변환 안내만 있었음.
- `git check-ignore ex12/.env ex12/.data/ranking-quality/2026-11-17.json`: 모두 제외 확인.

## 한계·다음
- 실제 모델이 같은 사건을 얼마나 정확하게 판정하는지는 mock 시험으로 입증하지 않는다. 배포 후 첫 daily의 진단파일과 Top5를 사람이 확인해야 한다(PLAN7 SKIP).
- 연관기사 수는 첫쪽 표본 비율로 전체 검색건수를 추정하는 값이다. 개별 기사 목록 전체를 확보한 수가 아니다. AI 판정이 틀리면 과대/과소 추정할 수 있다.
- 후보 수집은 2→4페이지이며 실제 새 기사 수·출처 다양성·1건 카드 비율·AI 비용은 운영 후 측정한다. 같은 키의 다른 명령과 겹치면 간격 보장은 적용되지 않는다. 최대 뉴스 대기34.1분 외에 AI 시간이 더 든다. 60분은 작업 제한이며 성공 시간 보장은 아니다.
- 진단파일은 그날 성공한 group 결과로 덮어쓴다. Actions는 실행ID/시도별 artifact로 분리한다. DB 저장 단계에서 실패하면 파일까지 도달하지 않음. 파일쓰기실패는 안전코드2로 알리며 이미 성공한 DB 저장은 되돌리지 않는다.
- 새 패키지0, 새 workflow action은 공식 upload-artifact@v7. https://github.com/actions/upload-artifact (명시경로·숨김파일·retention 문서 확인).
- O11 후속 기사목록/요약입력 재사용과 O12 낮·저녁 추가실행은 미구현이며 로드맵에 남겼다.
