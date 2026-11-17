# O11·O12 랭킹 품질 개선 — 운영 반영

2026-11-18 KST. 사용자 요청: 커밋·푸시·배포. O11·O12 1차와 이에 필요한 기존 O9a 수집 보완을 함께 반영하며, 기존 Android 작업·그 보고·공유 인계문서의 Android 변경은 보존한다.

## 준비·커밋 대상 검증
- 로컬 HEAD와 원격 main이 `2f10c7b192e27ea6e09d65831101f7ce67898559`로 같았고 시작 시 인덱스는 비어 있었다.
- 24개 관련 파일만 스테이징. HANDOFF는 O11·O12 인계 문단만 인덱스에 반영하고 다른 작업 내용은 작업 트리에 유지했다. PLAN/SPEC/PRODUCT_PLAN 변경은 랭킹·경제 수집 관련분만이었다.
- 인덱스의 ex12·워크플로159파일을 `.env` 없는 별도 사본에 복사하고 기존 node_modules만 연결했다. 사본 생성 중 큰 글꼴 파일의 기본 버퍼 한도를 넘어 도구가 멈춘 문제는 버퍼 상향으로 해결했다. 검증 뒤 인덱스159파일 해시가 그대로임을 확인했다.
- `npm.cmd test -- --reporter=dot`: 첫 확인404통과/1실패. 기존 push-api 시험이 무작위 P-256 키를 표준base64로 바꿨을 때 가끔 +/가 없는 문자열도 생성하여 유효 base64url과 구별되지 않는 사례였다. 제품 검사와 기존 시험을 검토했으며 이번 범위에서 변경하지 않았다. 재실행은 **42파일405/405통과**(22.41초). 간헐 시험 문제는 후속 보완 대상이다.
- `npm.cmd run build`: 첫 실행은 기존 Next16.4.1 보안 취약점 경고로 종료1. 같은 명령 재실행은 경고를 유지하며 **종료0**, 빌드/TypeScript 통과. 중첩 lockfile 경고는 검증 사본 위치 때문이며 설정을 바꾸지 않았다. 기존 보안 업그레이드 권고는 미해결이다.
- `git diff --cached --check`: 종료0. 테스트·빌드는 Windows Node26.13.0/npm11.20.0의 실제 결과이며 운영 workflow는 기존 Node24를 유지한다.

## 커밋·푸시
- 기능 커밋 [`570ed58241e334a5e919489399dbf289a73f088e`](https://github.com/Jwavascript/issuerank/commit/570ed58241e334a5e919489399dbf289a73f088e), `Improve ranking coverage and AI relevance verification`.
- `git push origin main` 성공. 관련 수집·AI·근거파일·workflow 및 계획/보고만 포함. Android 변경은 제외했다.

## 배포·운영 확인
- 기능 푸시의 [Vercel 배포](https://vercel.com/jwavascripts-projects/issuerank/HzBE8d9gTGoefq8e1fRwtJUaxBBz) **success**, GitHub deployment7857470875 **Production success**. 배포 주소 https://issuerank-mg20f9umv-jwavascripts-projects.vercel.app, 운영 별칭 https://issuerank-kappa.vercel.app.
- 운영 GET5항목 통과: 홈·keywords.js·sw.js 각200 및 커밋의 바이트 해시 일치, 랭킹200/날짜11-17/6섹션30카드, 잘못된 날짜400. 첫 비교는 Windows 작업트리 CRLF와 배포 LF를 비교해 실패했으나 실제 인덱스 사본과 비교하여 일치를 확인했다(제품 장애 아님). 증거 `ex12/.data/o11-production-check.json`.
- GitHub API로 daily·push·health workflow active 확인. 새 daily 정의는 기능 커밋에 반영됐으며 실제 실행은 다음 정규시각을 기다린다.
- 이 결과를 반영한 문서 마무리 커밋은 제품 코드·workflow 변경 없이 추가 푸시한다.
- 배포 확인은 GitHub 상태·deployment 상태와 공개 사이트 GET으로 한다. 수동 daily·수집·AI·푸시·DB쓰기0, 비밀값 복원·출력0.

## 다음 자동 실행
- 다음 정규 daily에서 top1+전체최대4페이지(수집최대30요청)·검색최대36·추가AI최대36·뉴스요청31초간격·60분제한을 확인한다.
- Actions의 ranking-quality artifact(JSON, 14일보관)에서 실제 새 기사수·출처수·섹션후보·원 totalResults·낱말/AI판정·추정값·대체율과 Top5를 사람이 비교한다. 실효·오판·토큰비용은 아직 미검증이므로 기존 심사 줄7은 SKIP이다.
- 이미 저장된 11-17 자료는 재수집·덮어쓰지 않는다. 배포 완료와 랭킹 품질 실효 검증은 구분한다.
