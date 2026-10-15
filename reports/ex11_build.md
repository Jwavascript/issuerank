# ex11 제작 보고 — 계정별 북마크·폴더

2026-10-14 시작. ex10 PASS 복사본에서 `plan/PLAN_backend_ex11.md`를 구현했다. ex08 AI 품질 FAIL·ex09 자동화는 보류한다. ex10과 사용자의 web/는 수정하지 않았다.

## 반영 위치

| 계획 | 구현 |
|---|---|
| 개인 API의 서버 세션 경계 | `server/app.ts`, `server/routes/bookmarks.ts`, `server/routes/folders.ts`: 본문 검사·Store 접근 전에 Better Auth getSession, 쿠키 없음/변조/만료401, 세션 userId만 전달. 공개 랭킹·인증 경로 유지 |
| 필수 소유자 저장소 | Store의 개인5함수에 필수 userId. `server/db/store.ts`의 조회·삭제·폴더 count JOIN을 소유자 조건으로 제한. 북마크는 created_at↓·id↓, 폴더는 created_at↑·id↑. 신규폴더 UUID |
| DB 소유권 제약 | `server/db/schema.ts`, `drizzle/0006_ownership.sql`·snapshot·journal: user_id NOT NULL/FK, 사용자+이슈 unique, 폴더 사용자+id unique, 북마크 사용자+폴더 복합FK. 기존 단일폴더/이슈 FK·RLS 유지 |
| 안전한 0006 적용 | 두 테이블 ACCESS EXCLUSIVE 잠금→빈행 확인 DO guard→DDL을 Drizzle 트랜잭션에서 적용. 무소유 기존행이면 P0001로 중단. 참조 대상 폴더 unique를 복합FK보다 먼저 생성 |
| 개인 응답 캐시 | Hono 개인 성공·실패 no-store, Next BFF 개인 경로/연결실패502도 no-store. 기존 쿠키 중계 보존 |
| 화면 | `public/index.html`: 현재 개인API401만 상태·목록·폴더필터·선택창 초기화. authVersion으로 늦은 목록/401/저장/해제/폴더생성 응답과 후속 쓰기/되돌리기 차단. 날짜별 헤더는 연속된 날짜 구간만 묶어 전역 최근저장순 보존 |
| fixture·계약 | `server/sample.ts`에 명시적 fixture 소유자, 신규계정은 빈 배열. `openapi.yaml` 개인5경로 쿠키/401/UUID/소유권·캐시 계약 |
| 테스트 | 기존85개 유지·기대값 사용자별/UUID 계약 조정, `ownership.test.ts` 실제 Better Auth5개, `account-ui.test.ts` 실제 HTML 함수 VM 경합/최근순6개 |

새 패키지·새 외부 계정/키 없음. 삭제한 파일·기존 데이터 없음. 계획에 따라 기존 전역 issue_id 단독 unique만 사용자+issue unique로 교체했다. `.env`는 로컬 파일이며 값/접속 문자열/쿠키는 출력하지 않았다. 실제 기사·이슈·요약·수집기록 생성 없음.

## 로컬 검증

모든 패키지 명령은 저장소 루트에서 `bash scripts/ex11-ubuntu.sh ...`로 실행했다.

- `npm test`: **13파일96/96 PASS**. 쿠키 없음/변조/만료 개인5경로401·잘못된 JSON보다 인증 우선·Store 미호출, A/B 동일이슈 독립저장/타인폴더404/중복409/삭제분리/동시UUID/동률정렬/과거일 저장을 확인했다. VM에서는 실제 화면 함수를 실행해 이전계정의 늦은 목록/401/저장/해제/폴더생성/되돌리기와 서로 다른날짜 최근저장순을 검증했다.
- `node .data/safe-run.mjs npm run build`: **Next16.4 Turbopack·tsc PASS**, leak:false.
- `node .data/safe-run.mjs npm run db:generate -- --name ownership`: **0006 생성**, 이후 잠금·빈행 guard 및 FK 순서 보완. 이전0000~0005 그대로.
- ex10 시작 baseline SHA-256 비교: 변경0. `git diff --check`: 오류0.

환경: 첫 실행 때 proot 배포판 자동 이동으로 기존 esbuild 절대 심볼릭 링크가 깨져 같은 linux-arm64 패키지를 못 찾았다. 부모가 ex11에서 lockfile대로 npm ci를 재실행해 네이티브 의존성을 복원했고 이후 테스트·빌드가 통과했다. 제품 코드/패키지 버전 변경으로 우회하지 않았다. 초기 usage-limit 자동승인 거절 명령은 실행되지 않았고 사용자 재개 요청 후 계속했다.

## 실제 검증

실행: `bash scripts/ex11-ubuntu.sh node .data/safe-run.mjs npx tsx .data/verify-ownership.ts build` — **PASS**, 종료코드0. 안전 결과는 `ex11/.data/build-ownership-verdict.json`에 보관했다.

- **실제 요청74/250회**, 임시 A/B2계정을 API와 UI에서 재사용했다. 인증·랭킹·개인 API와 브라우저 자동 요청을 포함한다. 뉴스·OpenAI 요청0회.
- 기존0000~0005 마이그레이션 hash·시각과 folders/bookmarks 빈행을 확인한 뒤 **0006 적용 완료**. 잠금·빈행 guard·NOT NULL·RLS를 확인했고 재실행으로 추가 변경이 없었다.
- 실제 PostgreSQL에서 타인 폴더 복합FK23503·같은 사용자 중복23505·소유자 누락23502를 각각 롤백 트랜잭션으로 확인했다. A/B 동일이슈 독립저장, 타인폴더404, 중복409, 사용자 위조 무시, 계정별 목록·개수·삭제, 동시 저장201/409·폴더 UUID, 동률정렬, 서버 재시작 및 같은 계정의 추가 세션에서 유지 모두 통과했다.
- 기존 과거일 `2026-10-14` 랭킹을 조회·저장하고 다시 로그인한 세션에서 확인했다. 실제 기사·이슈·요약은 추가하지 않았다.
- **실제 Chromium의 격리된 두 컨텍스트**에서 비로그인 안내, 신규계정 빈 목록, 폴더 생성·과거일 카드 저장, 최근저장순, 폴더/전체 필터의 렌더 카드, 해제·되돌리기, 새로고침, 다른 브라우저의 같은 계정 데이터, B 전환 시 빈 목록과 A 유지, A 세션 만료 시 화면 초기화를 검증했다. 지연 응답과 후속 쓰기 경합은 실제 HTML 함수를 실행하는 VM 테스트로 별도 검증했다.
- 이번 userId의 북마크→폴더→session/account/user를 정리하고 임시계정2개 제거를 확인했다. 검증 전후 기존9개 테이블의 행 수·내용 hash가 같았다. 자신이 시작한 Hono·Next 서버와 브라우저를 종료했고 부모가 브라우저 프로필을 정리했다.
- 서버·브라우저 로그를 환경 비밀값과 생성 비밀번호/쿠키/토큰으로 검사했다. **leak:false**이며 원문 비밀값을 출력하지 않았다.

OpenAPI는 개인5경로의 쿠키 인증·401 참조·소유권·no-store 계약을 정적으로 검토했다. 현재 환경에 YAML 파서가 없어 파서 검증은 SKIP이며 새 패키지는 설치하지 않았다. 제작 검증은 완료했으며 독립 심사는 별도 보고에서 판정한다.

다른 단계의 AI 품질 문제는 이번 계정별 저장 판정과 분리한다. 배포·OAuth·폴더 공유/이동/삭제·계정 삭제·자동화는 추가하지 않았다.
