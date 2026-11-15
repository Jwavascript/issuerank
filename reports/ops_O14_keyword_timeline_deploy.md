# O14 운영 적용 기록

2026-11-16 KST. 사용자 승인: 푸시·배포 후 다음 아침 자동 Actions에서 확인. 기존 운영 Secret을 사용하고 로컬에 비밀값을 복원하지 않는다.

## 준비
- 원격 main과 로컬 HEAD가 f7637d232f1205d6594fbd898d7561fede3bab8f로 일치함을 확인.
- Git Credential Manager의 기존 계정 인증으로 이 저장소 push 권한 확인. 토큰은 API 요청 메모리 안에서만 사용하고 출력/파일 저장하지 않음.
- 전용0008 실행기와 수동 workflow 준비. 기존 이력 불일치 거절·같은0008 재실행 무변경 가드3개 추가. 새 패키지 없음.
- 두 번으로 나눠 반영: ①마이그레이션 도구·SQL만 먼저 ②실제 DB 적용 뒤 O14 기능. 다른 Android/경제 수집 변경은 커밋 대상에서 제외한다.
- 적용/배포 상태와 실제 확인 결과는 실행 후 아래에 추가한다.

## DB 적용
- 도구만 커밋/푸시: `965b53e6b08038af4b1ff11a0684a3bd0e3e881e`.
- [사전 점검](https://github.com/Jwavascript/issuerank/actions/runs/43730854611) 성공: HISTORY_COUNT=8·READY=true·CHECK_OK.
- [0008 적용](https://github.com/Jwavascript/issuerank/actions/runs/43731042137) 성공: OLD_TABLES_PRESERVED=true·NEW_TABLES=2·APPLIED. 새 표 RLS/4개FK/칼럼·빈 상태를 검사했고 기존 public 행 내용 해시가 같았다. 이전 기사·이슈·요약·북마크·인증 자료를 수정하지 않았다.
- [같은 apply 재실행](https://github.com/Jwavascript/issuerank/actions/runs/43731193343) 성공: HISTORY_COUNT=9·READY=true·ALREADY_APPLIED. 이력·SQL 해시·RLS/FK가 일치하며 추가 변경 없음.
- 배포 전 현재 작업 트리 전체 테스트394/394·41파일 통과, 빌드 종료0. 실제 푸시할 인덱스 파일만 복사한 별도 검증도 수행한다.

## 배포 대상 검증
- 기존 Android와 경제 수집 코드/시험은 스테이징하지 않았다. 공유 HANDOFF/PLAN/SPEC/PRODUCT_PLAN에는 O14 문단만 스테이징하고 다른 작업은 작업 트리에 보존했다.
- 인덱스의 ex12/워크플로156개 파일을 별도 폴더로 복사, .env 미포함·node_modules만 기존 설치를 연결. 복사 전후 배포 대상 인덱스 일치 확인.
- **실제 배포 대상 시험389/389·41파일, 실패0·suite실패0·success=true**, 빌드 종료0. 작업 트리394개와 차이5개는 이번 커밋에서 제외한 경제 수집 작업의 시험이다.
- 첫 사본의 워크플로 상대 경로 누락은 검증 도구 문제로 구분하고 저장소와 같은 ex12/../.github 구조로 보완한 뒤 위 결과를 얻었다. 중첩 lockfile 경고만 있었으며 설정·패키지를 바꾸지 않았다.
- 기능 커밋 푸시와 Vercel 운영 배포를 이어 진행한다.
