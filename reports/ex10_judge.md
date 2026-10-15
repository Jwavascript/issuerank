# ex10 심사 보고 — 이메일·비밀번호 로그인

2026-10-14. 별도 심사 에이전트가 `agent_prompts/3_judge_agent.md`에 따라 코드·계약을 검토하고 현재 Termux 환경에서 재실행했다. 제품 코드는 수정하지 않았다. 기준은 `plan/PLAN_backend_ex10.md`, 제작 보고는 `reports/ex10_build.md`다.

**판정: PASS — ex10 현행 검증표 11줄 중 OK 11 / FAIL 0 / SKIP 0.** ex08 AI 품질 FAIL과 ex09 자동화 보류는 그대로다. 북마크·폴더 서버의 계정별 분리/401은 ex11 범위이므로 이 판정에 포함하지 않는다.

## 계획 구현 및 AC

| 계획 항목 | 판정 | 확인 |
|---|---|---|
| Better Auth 가입·로그인·세션·로그아웃 | done | 실제 라이브러리 메모리 테스트 및 실제 DB/BFF에서 성공·실패·폐기·만료·재시작 확인 |
| 설정·비밀번호·Origin·안전 로그 | done | 8~128자·공백 이름·이메일 검사, 라이브러리 해시, 제한된 trustedOrigins, CSRF/Origin 활성화, 예외는 안전 코드만 |
| 인증 스키마·마이그레이션 | done | auth4테이블·FK/고유값/인덱스·RLS, 0000~0005 파일 hash/적용시각 일치, 반복 실행 변경 없음 |
| Next BFF | done | Cookie/Origin/본문 전달, 상태·본문·Expires 쉼표 포함 복수 Set-Cookie 보존, no-store·안전한502 |
| 실제 화면 연결 | done | 브라우저 가입·새로고침·로그아웃·오류·정상 로그인, 360px 폼 검사 및 이미지 확인 |
| 계약·패키지·환경·범위 | done | OpenAPI 인증4경로, 기존 경로 계약 불변, 승인한3패키지1.7.7, 기존 lockfile 패키지 버전 변경0, .env/.data Git 제외 |
| 기존 코드·데이터 보존 | done | ex08 사전61파일 SHA-256 변경0, ex10 삭제 파일0, 뉴스/AI/순위 로직·기존76테스트 유지, DB 업무/기존인증 내용 해시 동일 |

| SPEC 근거 | 판정 |
|---|---|
| AC-06-1 | 충족. 실제 브라우저 가입 직후 이름·로그아웃 표시, DB 계정 생성과 세션, 새로고침 복원 확인 |
| AC-06-2 | 충족. 잘못된 로그인401·새 세션0, 화면 한국어 오류와 비로그인 상태 유지 확인 |
| AC-07-3 | 이번 단계의 화면 범위 충족. 비회원 북마크 안내·로그인 폼·저장요청0 확인. 서버 접근 차단은 ex11로 유보 |
| 과거 단계 AC | 기존76개 로컬 회귀 및 변경 비교로 확인. 이전 부록의 뉴스/AI 실서비스·샘플 DB mutation은 현행 ex10 실행 범위 밖이며 실행하지 않음 |

## 현행 검증표 결과

| 확인 | 결과 | 독립 심사 증거 |
|---|---|---|
| 로컬 회귀·빌드 | OK | 11파일85/85, Next16.4 Turbopack와 TypeScript 검사 성공, ex08 변경0 |
| auth 단위 통합 | OK | 실제 Better Auth + 메모리 어댑터7테스트: 가입·해시·쿠키·로그아웃·오류·중복·형식·길이·변조·만료·악성 Origin |
| BFF 쿠키 회귀 | OK | 2테스트: 복수 쿠키/Expires·전달 본문/Origin·오류상태·502/no-store |
| 설정·로그 | OK | 필수 설정/잘못된 URL·DB/auth 예외 테스트. 원본 비밀값/스택 대신 API_START_FAILED/API_INTERNAL_ERROR/AUTH_INTERNAL_ERROR |
| DB 마이그레이션·보존 | OK | 이미 적용된0005와 이전 이력 대조, 재실행 변경0, auth4 RLS 활성화, 업무데이터 내용 해시 동일 |
| 가입/API(AC-06-1) | OK | BFF 가입200·해시·HttpOnly/SameSite·세션 조회, Hono 재시작 후 같은 사용자 유지 |
| 잘못된 로그인/API(AC-06-2) | OK | 잘못된 비번/없는 이메일401·세션null, 중복422·짧은비번400·추가 세션0 |
| 정상 로그인·폐기·출처 | OK | 정상200·본인정보, 로그아웃 쿠키 만료·이전 쿠키null, 변조/만료null, 악성Origin403·세션0 |
| UI 가입/오류 | OK | 실제 Chromium144/CDP로 폼 제출·가입 자동로그인·새로고침·로그아웃·틀린 로그인 오류·정상 로그인. 360×800 모바일 폼 좌우 경계/scrollWidth 검사와 캡처 이미지 확인 |
| 기존 화면 안내/회귀 | OK | 비회원 북마크 버튼 안내·북마크 탭 로그인 폼·저장요청0, 목록의 원문 새 탭 링크/요약 DOM, 날짜 이전/오늘, 이슈맵·6섹션 확인. 기존 카드·문구·링크 생성 코드는 소스 비교로 보존 확인 |
| 정리 | OK | API/UI 임시계정 각각1개 총2개 모두 제거, 기존 업무·인증 해시 동일, 소유 Hono/Next/Chromium 종료 및 임시 브라우저 프로필 정리 |

합계 **OK 11 / FAIL 0 / SKIP 0**. 원문 언론사 페이지 자체와 외부 사이트 내용은 열지 않았으며, 이번 화면 회귀는 앱의 링크/target 속성과 기존 생성 코드 보존을 검증했다.

## 재실행 명령과 산출물

저장소 루트에서 실행:

```bash
bash scripts/ex10-ubuntu.sh npm test
bash scripts/ex10-ubuntu.sh node .data/safe-run.mjs npm run build
bash scripts/ex10-ubuntu.sh node .data/safe-run.mjs npx tsx .data/verify-auth.ts judge
bash scripts/ex10-ubuntu.sh node .data/safe-run.mjs npx tsx .data/judge-ui.ts
```

- 테스트85/85·빌드 성공은 심사 직접 재실행 결과다. 로컬 테스트의 악성Origin 거절에서 AUTH_INTERNAL_ERROR 한 줄은 의도된 안전 로그다.
- `ex10/.data/judge-auth-verdict.json`: 실제 API18요청 PASS, leak:false, 임시계정 제거·서버 종료·기존데이터 보존.
- `ex10/.data/judge-ui-verdict.json`: 실제 Chromium UI 인증7요청 PASS, leak:false, 임시계정 제거·기존데이터 보존.
- `ex10/.data/judge-request-count.json`: **심사 누계25/30 인증요청**, 임시계정 총2/2. 뉴스0·OpenAI0. 랭킹/폴더/북마크 읽기와 로컬 CDP 통신은 인증요청 수와 별도이며 북마크/폴더 쓰기0.
- `ex10/.data/judge-auth-mobile.png`: 실제360×800 로그인 폼. API를 브라우저 검증으로 대체하지 않았다.
- 업무 데이터 전후: 기사285·이슈102·북마크0·폴더0·수집기록54, 건수뿐 아니라 내용 해시 동일. 이번 계정만 finally에서 정리했다.
- Python 기존 YAML 파서로 중복키·내부참조 및 이전 API 경로 내용 불변 확인. 추가 도구 패키지 설치 없음.
- Termux 호스트 Node24.18.0/npm11.20.0·Linux, 패키지 실행은 기존 Ubuntu 래퍼 사용. Next는 `--hostname 127.0.0.1`, 브라우저 Origin은 `http://localhost:3000`.

## 환경 차이·범위 위반 검토

제작 당시 브라우저 미사용으로 UI가 SKIP였지만 이번 재개 환경에는 Chromium144 실행기가 있었다. 초기 dump-dom은 종료되지 않아 심사 소유 프로세스만 종료했다. CDP 프로세스의 파이프 수명 문제를 분리한 뒤 격리 프로필로 실제 화면 조작1회를 완료했다. 권한 완화 옵션이나 새 패키지는 추가하지 않았다. 이미지 뷰어의 bwrap 실패는 읽기 전용 이미지 로드로 확인했으며 제품 오류가 아니다.

이전 심사 턴의 실행 환경 조회는 자동 승인 검토의 사용량 제한으로 중단됐고, 그때 실제 인증요청은0회였다. 사용자 재개 후 위 API/UI 검증을 수행했다. 당시 완료된 로컬 테스트와 이번 완료된 빌드를 구분해 기록했다.

PLAN 밖 기능·불필요한 기존 파일 삭제·기존 패키지 버전 변경·기사 본문 저장·추가 뉴스 출처·비밀값 하드코딩/출력은 발견하지 않았다. 인증 스키마의 OAuth용 필드는 공식 라이브러리 기본 스키마이며 소셜로그인 활성화를 뜻하지 않는다. 소셜OAuth·이메일 발송·비밀번호 재설정·계정별 북마크는 구현하지 않았다. 제품 코드 변경 권고/심사 FAIL 항목 없음.
