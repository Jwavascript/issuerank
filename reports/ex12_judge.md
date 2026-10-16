# ex12 심사 보고 — 배포(Vercel)

2026-10-15 15:10~15:50 KST. 심사 역할(`agent_prompts/3_judge_agent.md`), Claude Code, Android/Termux + proot Ubuntu(`bash scripts/ex12-ubuntu.sh …`). 제품 코드는 고치지 않았다. 기준: `plan/PLAN_backend_ex12.md`, PLAN.md 5절 D10, SPEC, AGENTS.md. 제작 보고 `reports/ex12_build.md`는 참고만 했고, 모든 줄을 심사 스크립트(`ex12/.data/judge-*.mjs`)로 다시 실행했다. 제작 결과(`build-*`)를 심사 결과로 대신 쓰지 않았다.

- 배포 주소: `https://issuerank-kappa.vercel.app` (사용자가 배포, Root `ex12`, GitHub 연동, 292c47b 이후).
- 외부 호출: newsdata 0 · OpenAI 0. 배포 주소 요청 **66/150**(Node fetch 26 + 브라우저 자동 요청 40), 로컬 요청 **22/100**(3000 18 + 8787 4). Vercel·GitHub 설정 변경 · 재배포 · 커밋 · 푸시 안 함.

**판정: PASS 보류 — 검증 17줄 중 OK 13 / FAIL 0 / SKIP 4(모두 사용자 전달 대기).** 코드 · 테스트 · 빌드 · 에이전트가 직접 볼 수 있는 배포 동작은 모두 통과. 남은 SKIP은 Vercel 대시보드 설정 화면 · 배포 고유 주소 · 사용자 PC/폰 결과 · Vercel Logs이며, 이 넷이 오면 ex12 PASS 여부를 판정한다.

## 1. PLAN 항목별

| 항목 | 판정 | 확인 |
|---|---|---|
| 요청 경로 | done | `app/api/[...path]/route.ts`: `runtime='nodejs'` · `dynamic='force-dynamic'`, `createBff(loadApp)` 처리 함수를 GET·POST·DELETE로. `API_URL` 읽기 없음. 배포 `/api/rankings`의 `x-vercel-id` = `icn1::icn1::…`(함수 실행 서울) |
| 앱 준비 | done | `server/bff.ts` `loadApp`: DATABASE_URL 확인 → `authOptions(process.env)` → `connectDb({max:1})` → `createApp(dbStore, createAuth)`. Promise를 모듈 범위에 저장, 실패 시 지움(테스트 ③④, 로컬 실패 실행에서 요청마다 `API_START_FAILED` 2줄 = 재시도). 빌드 로그 DB 접속 0 |
| 계약 유지 | done | 응답 헤더 복사 · `getSetCookie()`로 하나씩 다시 붙임 · 개인 경로 no-store(성공·실패·502). 502 문구 그대로, 로그 `API_START_FAILED`만. 배포 개인 응답 19개 모두 no-store · `x-vercel-cache` HIT 0 |
| DB(서버리스) | done | `connectDb(options={})`, `max` 있을 때만 전달 → 기존 호출 불변 |
| 로컬 개발 | done | `npm run api`(server/index.ts) 그대로 남음(8787 비교에 사용) |
| Better Auth | done | `server/auth.ts` 해시 ex09와 같음. 배포에서 정상 Origin 200 · 악성 Origin 403 → 배포 `BETTER_AUTH_URL`이 프로덕션 origin과 맞음 |
| Vercel 설정 | done | `vercel.json` `{ "regions": ["icn1"] }`, package.json `engines.node 24.x` 한 줄, lockfile ex09와 `cmp` 동일 |
| 환경 변수(Vercel) | partial(사용자 확인 대기) | 배포 동작으로 DB · 인증 값이 들어간 것은 확인. "Production만 · Secret/Config 종류 · 키 4개 미등록"은 대시보드 화면이 필요 → SKIP 줄 |
| 하루 실행 | done | `git diff --stat .github/` 빈 출력, daily.ts · steps.ts 해시 ex09와 같음 |
| 화면 변경 없음 | done | `public/index.html` ex09와 동일(diff 없음) |
| 구현 순서 0~4 | done | 0 선행 조건(ex09 예약 실행 1줄 보류) 확인, 1~3 완료, 4 배포 줄은 이번 심사가 실행 |

## 2. SPEC 근거 AC

| AC | 판정 |
|---|---|
| AC-08-3 | 부분 충족. ① 격리 Chromium 컨텍스트 2개에서 같은 A 계정: 시작 · 저장 후 · 해제 후 목록 id · 폴더(이름·개수) · 폴더 칩 · 배지 · 카드가 같음, B로 바꾼 컨텍스트에 A 카드 0, 다른 컨텍스트는 A 그대로. ② 사용자 PC · 폰 결과 미전달 → SKIP |
| AC-03-1 · AC-05-1 | 충족. 배포 랭킹 200 · 섹션 6개(pol·eco·soc·cul·wor·it) · `updatedAt` · 화면 갱신 시각 표시 · 화면의 `ranking` 객체가 API 응답과 문자 단위로 같음 · 목록 카드 30 = API 30 |
| AC-05-2 | 충족. 360×800 모바일에서 map · list · bookmarks 가로 스크롤 0 |
| AC-06-1 · AC-06-2 | 충족. 가입 200 · 세션 본인 · 로그아웃 뒤 null · 틀린 비밀번호 401 · 정상 로그인 200 |
| AC-07-1 · 07-2 · 10-1 · 10-2 | 충족. 폴더 201 · 저장 201×2 · 폴더 count · 해제 204, 화면 폴더 선택창으로 저장 · 해제 |
| AC-07-3 · AC-04-2 | 충족. 비로그인 클릭 → 로그인 안내 · 저장 요청 0. 원문 링크 `target=_blank` · `noopener` · API url과 같음 · 실제 새 탭이 언론사 주소(`www.dailian.co.kr`)로 열리고 원래 탭 유지 |
| AC-08-1 · 08-2 | 충족. 최근 저장 순, B 빈 목록 `[]` |
| AC-09-1~3 | 충족. 직전 수집일(2026-10-14) 200 · 2000-01-02 / 2000-01-01 404 "해당 날짜의 랭킹 데이터가 없습니다" · 내일(10-16) 400 · 화면 이전 날 → 날짜 표시 바뀜 → "오늘" 복귀 |
| AC-10-3 | 참고(PLAN에 배포 검증 줄 없음). 화면 코드 ex09와 동일, 테스트 통과 |

## 3. 범위 밖 변경 · 회귀

- ex09와의 차이(산출물 · .env 제외 `diff -rq`): `.env.example` · `app/api/[...path]/route.ts` · `package.json` · `server/bff.ts`(새) · `server/db/client.ts` · `test/auth-proxy.test.ts` · `vercel.json`(새)뿐 = PLAN 목록과 같음.
- auth · app · ai · daily · steps 해시 쌍 모두 같음. `git status --short ex09/ ex11/ .github/` 빈 출력(출발점 폴더 불변). 새 패키지 0.
- PLAN에 없는 추가: 테스트 1개(공개 경로 502에 no-store 없음) — PLAN 규칙을 확인하는 테스트라 문제 없음.
- 화면 기존 동작(PLAN.md 6절): `public/index.html` 불변, 배포 화면에서 비로그인 안내 · 모바일 · 날짜 · 링크 · 폴더 선택창 · 배지 동작 확인.

## 4. 테스트 · 빌드

- `bash scripts/ex12-ubuntu.sh npm test` → **14파일 115/115 통과**(첫 실행, 24.9초). git 추적 파일 변경 0.
- `bash scripts/ex12-ubuntu.sh npm run build` → 종료 0. Next.js 16.4.0 Turbopack · TypeScript · `tsc --noEmit`, `ƒ /api/[...path]`. 로그에 `API_START_FAILED` · postgres · 연결 오류 0.

## 5. 검증 방법 줄별

| # | 줄 | 판정 | 결과 |
|---|---|---|---|
| 1 | 출발점 · 변경 범위 | OK | 3절과 같음 |
| 2 | 로컬 회귀 · 빌드 | OK | 4절. lockfile 동일 |
| 3 | BFF 같은 프로세스(테스트) | OK | `test/auth-proxy.test.ts` 6개가 ①~⑥을 그대로 검사(쿠키 2개 Expires 포함 · Cookie/Origin/본문 · 로더 1번 · 가짜 비밀값 502 뒤 재시도 200 · 앱 throw 502 · rankings no-store 없음 · fetch 0) 하고 통과 |
| 4 | 로컬 한 서버 | OK(대체 실행) | `npm run api` 없이 3000 하나. `next start`와 `next dev --webpack`(+`WATCHPACK_POLLING=true`) 두 모드 모두: `/` 200 HTML · `/api/rankings` 200 · `date`=SQL 최근 수집일 2026-10-15 · 섹션 6 · updatedAt · no-store 없음 · 2000-01-02/2000-01-01 404 문구 · 어제 200 · 내일 400 · `/api/bookmarks` 401 "로그인이 필요합니다" no-store · 앱 오류 로그 0. 이 기기의 Turbopack `npm run dev`는 파일 감시 한도 환경 문제(PLAN.md 10절)라 대체 |
| 5 | 앱 준비 실패(로컬) | OK | `next start`, 자식 env `DATABASE_URL=` / 따로 `BETTER_AUTH_SECRET=short`: 둘 다 rankings · get-session 502 "API 서버에 연결할 수 없습니다", auth no-store(rankings는 없음). 서버 로그: Next 시작 문구 5줄 외에는 `API_START_FAILED` 2줄뿐, 인증 성공 0, 누출 false |
| 6 | 비밀값 누출(빌드 · 저장소) | OK | `.next/static`(9) · `.next/server`(113) · `public/` · `vercel.json` 모두 leak=false. `git check-ignore` → `.gitignore:4:.env` 무시, 추적 `.env` 0건. 코드의 `postgres://` 4건은 테스트용 가짜 값(실제 값 포함 false) |
| 7a | 배포 설정 — 직접 관찰 | OK | `GET /` 200 · `x-vercel-id` `icn1::…`, `/api/rankings` `icn1::icn1::…` |
| 7b | 배포 설정 — 대시보드 화면 | SKIP — 사용자 전달 대기 | Root `ex12` · Next.js · Node 24.x · 환경 변수 3개 Production만 · Standard Protection |
| 8 | 배포 랭킹 · 날짜 | OK | 배포 기본 200(date 2026-10-15) · `?date=2026-10-14` 200 · 2000-01-02 / 2000-01-01 404 같은 문구 · 2026-10-16 400("오늘 이후 날짜는 조회할 수 없습니다"). 로컬 8787 같은 요청과 **앞 둘 본문 문자 단위 동일**, 오류 상태도 같음. 섹션 6 · updatedAt. rankings `x-vercel-cache` MISS |
| 9a | 배포 가입 · 쿠키 | OK | A 가입 200 · Set-Cookie 1개(세션 토큰) `Secure` · `HttpOnly` · `SameSite=Lax` · `__Secure-` 접두 모두 true · get-session 본인 · 로그아웃 200 → null · 틀린 비밀번호 401 · 정상 로그인 200 · `Origin: https://evil.example` 403(쿠키 0) · B 가입 200 |
| 9b | 배포 고유 주소 보호 | SKIP — 사용자 전달 대기 | `…-{해시}.vercel.app` 주소는 Deployments 화면에서만 알 수 있음(응답 헤더에 없음) |
| 10 | 배포 계정별 북마크 | OK | A 폴더 201 → Top 5 이슈 2개 201·201 → 목록 최근 저장 순 · count 2 → 해제 204 · count 1 / B 목록 `[]` · 폴더 `[]` · A 폴더로 POST 404 "폴더를 찾을 수 없습니다". 비로그인 401. 개인 응답 19개 모두 `Cache-Control: no-store` · `x-vercel-cache` HIT 0 |
| 11a | 두 기기 ①(격리 Chromium 2개) | OK | Termux Chromium 155 headless + CDP, 격리 컨텍스트 2개. AC-08-3 행 참고 |
| 11b | 두 기기 ②(사용자 PC · 폰) | SKIP — 사용자 전달 대기 | 실행 시점 DB 회원 0 → 사용자 계정 아직 없음 |
| 12 | 배포 화면 회귀 | OK | 비로그인 북마크 클릭 → "북마크하려면 로그인이 필요합니다" · 쓰기 요청 0 · 360px 가로 스크롤 0(3화면) · 이전 날 → 날짜 표시 바뀜("오늘" 없어짐) → "오늘" 버튼 → 당일 복귀 · 원문 링크 새 탭 언론사 주소 |
| 13 | 배포 로그 | SKIP — 사용자 전달 대기 | Vercel › Logs(이번 검증 시간대 15:20~15:45 KST) 필요. 예상되는 앱 줄: 악성 Origin 1회의 `AUTH_INTERNAL_ERROR` |
| 14 | 정리 · 보존 | OK | 임시 계정 2개(`ex12-<uuid>@example.invalid`) finally에서 bookmarks → folders → session → account → user(+verification) 삭제, 남은 행 0. 9개 테이블(articles 443 · issues 235 · collect_runs 72 · bookmarks 0 · folders 0 · user 0 · session 0 · account 0 · verification 0) 건수 · md5 전후 동일. 소유 서버(3000 · 8787) · Chromium 종료: `curl` 3000/8787/9339 = 000, `ps` 대상 0줄, 브라우저 프로필 삭제. 뉴스/OpenAI 0 · 한도 안 |

합계: **OK 13 / FAIL 0 / SKIP 4**.

## 6. 지켜야 할 것

- 기사 본문 저장 · 표시: 없음(화면 · API 코드 ex09와 같음).
- 코드의 키 · 접속 정보: 없음(빌드 산출물 · 공개 파일 leak=false, 테스트 값은 가짜).
- `.env` git 제외: 됨. PLAN에 없는 패키지: 없음. D0 밖 출처 수집: 없음(수집 코드 불변, 이번 심사 newsdata 0).
- AGENTS.md: 비밀값 출력 0(모든 출력은 누출 검사 false 뒤), 소유 서버만 종료, 실제 데이터 보존. 위반 없음.

## 7. 발견한 점(심각도 순)

1. (낮음 · 기록) 제작의 `ex12/.data/build-local-verdict-{start,dev-webpack}.json` status가 FAIL로 남아 있다. 원인은 제작 스크립트의 로그 검사가 의도된 `AUTH_INTERNAL_ERROR`(악성 Origin)와 터미널 제어 문자 `ESC[?25h`, 실패 실행의 Next 시작 문구까지 막은 것. 심사가 독립 실행한 로컬 · 실패 줄은 모두 OK이고 로그에 다른 앱 줄이 없음을 직접 봤으므로 제작 보고의 설명은 맞다. 다만 증거 파일과 보고가 어긋나니 다음 제작 때 검사 기준을 고치면 좋다(제품 영향 없음).
2. (낮음) 이 기기의 Turbopack `npm run dev`는 API를 컴파일하지 못한다(환경 요인, PLAN.md 10절 기록). PLAN의 `npm run dev` 그대로는 실행 못 했고 `next start` · `next dev --webpack`으로 대신했다.
3. (참고) `package.json` name이 `issuerank-ex11` 그대로(ex09부터). 동작 영향 없음.
4. (참고) 공개 랭킹 응답은 Vercel 기본 `Cache-Control: public, max-age=0, must-revalidate` · `x-vercel-cache: MISS` — 캐시로 날짜가 굳는 문제 없음.
5. (참고, 판정 밖) 사용자 질문 "섹션 1위가 기사 3~4건처럼 적다"는 하루 실행 · 묶기(ex08 계열) 품질 문제이며 ex12 판정에 넣지 않았다. 배포 화면 값은 API · 로컬 8787(같은 DB)과 문자 단위로 같았다.

## 8. 사용자에게 받을 것(남은 SKIP 4)

1. Vercel 설정 화면(캡처나 글): Settings › Build and Deployment(Root Directory `ex12` · Framework Next.js · Node.js 24.x) · Functions(Region icn1) · Environment Variables(이름 3개 `DATABASE_URL` · `BETTER_AUTH_SECRET` · `BETTER_AUTH_URL`, Production만, 값은 보내지 않음, 다른 키 미등록) · Deployment Protection(Standard Protection).
2. 배포 고유 주소 하나(Deployments › 최신 배포의 `issuerank-kappa-…-….vercel.app`) — 심사가 `GET /` 1회로 보호 화면(401 또는 로그인 이동)인지 확인. 또는 사용자가 비로그인 창으로 열어 결과 전달.
3. PC · 폰 결과(AC-08-3 ②): 본인 계정으로 PC에서 가입 · 북마크 1개 저장 → 폰에서 같은 계정 로그인 → 북마크 화면의 목록 · 폴더 칩 · 개수가 같은지.
4. Vercel › Logs: 2026-10-15 15:20~15:45 KST 로그(비밀값 · 쿠키 · SQL 인수 없음, 오류는 안전 코드만인지).

넷 모두 문제없으면 SKIP 줄만 다시 판정해 PASS로 바꾼다(추가 배포 요청은 고유 주소 1회 정도, 남은 한도 84).

## 9. 증거 파일(git 무시)

- `ex12/.data/judge-local.mjs` · `judge-local-verdict.json`: 로컬 줄 4 · 5.
- `ex12/.data/judge-deploy.mjs` · `judge-deploy-verdict.json`: 배포 줄 7a · 8 · 9a · 10 · 11a · 12 · 14. 이 실행의 `linkOpenedNewTab` false는 스크립트가 사용자 제스처 없이 `click()`해 Chromium 팝업 차단에 걸린 것 → `judge-link.mjs` · `judge-link-verdict.json`에서 `userGesture`로 다시 실행해 새 탭 · 언론사 주소 확인(제품 문제 아님).
- `ex12/.data/judge-lib.mjs`(공용) · 요청 카운터 `judge-deploy-request-count.json`(66) · `judge-local-request-count.json`(22).

## 10. 2026-10-15 KST — D19 · D19-2 검색어 보완 2차 · 사진 접두 제외 독립 심사

대상: 미커밋 작업 트리의 ex12 · ex09 `server/ai.ts` · `test/ai.test.ts`. 기준: `plan/PLAN_backend_ex12.md` 마지막 두 절, PLAN.md D13 · D18 · D19 · D19-2, AGENTS.md. 제작 보고 10 · 11절은 참고만 하고 아래는 모두 심사가 다시 실행한 결과다. 코드 수정 · 커밋 · 푸시 없음.

**판정: 이번 변경 PASS 보류** — 실행한 14줄 모두 OK, FAIL 0. 남은 SKIP 1은 PLAN상 푸시 뒤 첫 예약 실행(10-17) 분포 줄이라 지금 실행할 수 없음. 실제 검증 확정 기준(D19 · D19-2)은 이번 실행으로 **통과**.

### 10.1 계획 항목
| 항목 | 판정 | 근거 |
|---|---|---|
| terms 지시: 띄어쓰기 없는 한 낱말 · 그룹1 고유 명사 · 그룹2 핵심 행동/대상 한 낱말 · 표기 변형 동의어 1~3 · 美·中·日 약칭 허용(옛 "한자 약칭 대신 한글" 삭제) · 흔한 말만 금지 · 수치/지명 상세/가운뎃점 금지 · 원인/결과 동의어 금지 유지 | done | diff는 METADATA_SYSTEM 4절 검색어 문단만 바뀜. 1차 "1~2낱말" · "가장 구체적인 표현 하나" 문장 없음 |
| 사진 접두(`[사진]` · `[포토]` · `[화보]`) 코드 제외 | done | `PHOTO_PREFIX = /^\S+ \[[a-z]+\] \[(사진\|포토\|화보)\]/`(제목 시작만, 중간 `[사진]` 제외 안 함). 배정 입력 · 배정 스키마 · 후보 힌트(`assignInput(rest)` → `candidateGroups(rest)`) · 메타 입력(`byNumber`가 rest 기준)에서 빠지고 excluded 앞에 들어감. 중복 · 형식 번호 검사는 전체 줄, 대표 검사는 남은 번호 기준 → 묶음+제외 = 전체 번호. 전부 사진이면 `{issues: [], excluded}`로 호출 0 |
| 테스트 | done | 지시문 문구 1개 수정 + 사진 접두 제외(입력 · 스키마 · 메타 입력 · excluded · 번호 완전성) 1개 + 전부 사진이면 fetch 0회 1개 |
| 유지: 스키마 2×(1~3) · 모델 gpt-6-luna · effort(배정 medium · 메타 low) · ASSIGN_SYSTEM · 섹션/importance 지시 · buildQuery · grouping.ts · D13 · 저장 · 순위 | done | 해당 줄 diff 없음. ex09 · ex12 `grouping.ts` 둘 다 `f085810c…` |
| ex09 해시 일치 복사 | done | ai.ts 둘 다 `8c41162a740254dd…`, ai.test.ts 둘 다 `8b939bbf944512d1…` |
| ex08 · ex11 보존 | done | ai.ts 둘 다 `07e10b1e7746…`, ai.test.ts 둘 다 `95e7b3d1…` |

### 10.2 범위 밖 변경
- `git diff --stat -- ex08 ex09 ex11 ex12` = ex09 · ex12의 `server/ai.ts` · `test/ai.test.ts` 4파일만. ex09 · ex12 아래 추적 안 된 새 파일 0(`.data`는 git 무시). package.json · lockfile 변경 0 → 새 패키지 0.
- 문서 변경(별도): HANDOFF.md · PLAN.md · `plan/PLAN_backend_ex12.md` · `reports/ex12_build.md` · `reports/ex12_judge.md`.

### 10.3 테스트 · 빌드(심사 재실행)
| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 **118/118** |
| `bash scripts/ex12-ubuntu.sh npm run build` | 성공(`/_not-found` · `/api/[...path]`) |
| `bash scripts/ex09-ubuntu.sh npm test` | 14파일 **114/114**(첫 실행 출력 끝부분에 합계가 잘려 한 번 더 실행해 확인, 둘 다 실패 0) |
| `bash scripts/ex09-ubuntu.sh npm run build` | 성공 |

vitest · 빌드 전후 `git status --short` 같음(되돌린 파일 없음).

### 10.4 독립 실제 검증(로컬, 2026-10-15 16:27:11 KST 시작 · group 16:27:18~16:28:42)
- 스크립트: 제작 `ex12/.data/verify-terms2.ts`를 읽고 검토(OpenAI 외 주소 차단 · 횟수 상한 · 읽기 전용 트랜잭션 load · save 기록만 · `wx` 저장 · 누출 검사 · 직전 크레딧 70 미만 중단)한 뒤 `ex12/.data/judge-terms.ts`로 복사. 바꾼 것: 출력 폴더 `judge-terms-20261015`, 독립 사진 판정(제품 정규식이 아니라 DB 원래 제목에서 앞 공백 · 전각 괄호까지 허용해 판단) · 제목 어디든 사진 낱말 목록 · 다중 묶음/제외 전체 목록(`review.json`). 가짜 응답 모드(외부 호출 0, DB 읽기만)로 먼저 돌려 후처리를 확인하고 그 폴더를 지운 뒤 실제 1회. `safe-run` 출력 `leak:false`, 재시도 0, 서버 띄우지 않음.
- 입력: 158줄, sha256 `7526497bea54c3989a8030f03c1407ab0a06ac23e41c998f512a9f3a49547ed5` = 기대값.
- AI 정리: ok · 묶음 132 · 제외 15(사진 접두 5 + AI 10) · 빠진 0 · 고침 모두 0. 배정 입력 153줄.
- 사진 접두: 제품 판정 5건(a098 · a099 · a109 · a110 · a144) = 심사 독립 판정 5건, **전부 excluded**, 배정 · 메타 요청 input에 번호 없음. 제목 중간에 사진 낱말이 있는 2건(a027 `관광사진 공모전` · a084 `청사진`)은 제외 안 됨(맞음).

| 섹션 | 대표 제목 | 검색어 | n | 묶음 | totalResults | 1쪽 | p | 제작 n | 판단 |
|---|---|---|---|---|---|---|---|---|---|
| pol | 김지용 청문회서 부각된 여권 분열… | `김지용 AND 청문회` | **34** | 3 | 43 | 10 | 0.8 | 35 | 1쪽 10건 모두 김지용 중수청장 후보자 청문회 → OK · n>묶음 |
| eco | 트럼프 "韓의 알래스카 500억불 투자, 나와의 무역·투자 합의" | `트럼프 AND 알래스카` | **45** | 2 | 50 | 10 | 0.9 | (제작은 CPI 23 — 첫 후보 다름) | 1쪽 10건 모두 알래스카 LNG 투자 주장(1건은 대만 구매분, 같은 발언) → OK · n>묶음 |
| soc | 흉기 난동범 "병원 무좀 치료 불만…" | `인천 AND 흉기난동` | **58** | 3 | 115 | 10 | 0.5 | 58 | 1쪽 10건 모두 인천 구월동 흉기난동(영장 · 진술 · 신상공개 + 후속 반응) → OK · n>묶음 |
| cul | '흥이·나유'와 '꿈돌이' 만났다…협업 굿즈 첫선 | `꿈돌이 AND 굿즈` | 1 | 1 | 1 | 1 | 1 | 1 | 1쪽 = 같은 기사 → OK · n=묶음 |
| wor | 트럼프 "그린란드서 대규모 석유 탐사 예정"…그린란드 반박 | `그린란드 AND 탐사` | **6** | 4 | 6 | 6 | 1 | 6 | 1쪽 6건 모두 같은 사건(`석유·가스 탐사` 표기도 잡힘) → OK · n>묶음 |
| it | AI해킹, 금융사 16곳서 개인정보 10만건 유출 | `금융사 AND 해킹` | **20** | 2 | 20 | 10 | 1 | 20 | 1쪽 10건 모두 금융사 16곳 해킹 · 10만건 유출 → OK · n>묶음 |

- 확정 기준: n>묶음 **5/6**(기준 4 이상) · 모든 n<1,000(최대 58) · p 6개 기록(스크립트 재계산이 6개 모두 n과 일치, buildQuery 재현 6개 일치) · 사람 검토 OK · 접두 사진 전부 excluded → **통과**.
- 기록만: 132이슈 264말 중 공백 포함 **0** · 3낱말 이상 0 · 숫자 포함 1(`오우라링5`) · 가운뎃점 0 · **동의어 사용 0**(제작 9).

### 10.5 추가 사람 검토(이번 실행 전체)
- 다중 묶음 7개 전부: pol 김지용 청문회 3 · eco 트럼프 알래스카 2 · eco 메타 주가 1000달러 2(같은 월가 목표가 보도, 1건은 wor 태그) · wor 그린란드 탐사 4 · wor 레오14세 폐 수술 회복 2 · soc 인천 흉기난동 3 · it 금융사 해킹 2(a136 `"금융망 취약점, AI는 1시간 만에 찾아 대처"` — 설명문이 같은 AI 연쇄 해킹 사태 속 우리은행 방어 → 같은 사건의 후속으로 봄). **오병합 0.**
- 제외 15개 전부: 사진 접두 5(a144 `[사진] 버넘 英 총리의 약속…`은 발언 내용이 있는 기사 → **규칙대로**), AI 제외 10 = 자동 주식 · 실적 기사 6(칼투라 임원 매도 · Beyond Air 자사주 · Havas · State Street · JD.com 목표가 · GreenMobility — D15 "자동 주가 기사"), 레서판다 장면 사진 · 빌리 츠키 숏폼 · 변우석 장면(사진성), 부음 1. **잘못된 제외 0.**
- 참고(판정 밖): 병합 누락 3곳 — 美 9월 CPI 3건(a080 · a149 · a153) 단독, 충남학 강좌 2건(a019 · a028) 단독, 김지용 `결격 사유 없어`(a087)는 청문회 묶음과 별도. 어학 칼럼 `[신문으로 배우는 실용한자]`(a129)가 제외되지 않고 soc 하위 이슈(1건)로 남음.

### 10.6 제작 결과와의 차이(같은 입력, AI 결과가 달라짐)
| 항목 | 제작(11절) | 심사 |
|---|---|---|
| 묶음 · 제외 | 134 · 10(AI 제외 5: 실용한자 포함) | 132 · 15(AI 제외 10: 자동 주식 기사 6 포함, 실용한자 미제외) |
| eco 첫 후보 | 美 CPI 3건 → 23 | CPI 3건이 단독으로 흩어짐, 첫 후보 트럼프 알래스카(제작에선 wor 묶음) → 45 |
| pol | `김지용 AND 청문회` 35(tr 44) | 같은 검색어 34(tr 43) — 4시간 사이 1건 차이 |
| cul 검색어 | `(흥이 OR 나유 OR 꿈돌이) AND 굿즈` | `꿈돌이 AND 굿즈`(n 둘 다 1) |
| it 묶음 | 1 | 2(a136 합류), n 20 같음 |
| 공백 포함 말 · 동의어 | 5 · 9 | 0 · 0 |
| 판정 | PASS(5/6) | 통과(5/6) |

동의어 지시(표기 변형 OR)가 이번 실행에서는 한 번도 쓰이지 않았다. 판정 기준 밖이지만 D19-2의 핵심 변경 중 하나라 매일 결과로 계속 볼 항목.

### 10.7 점검 줄 합계
| # | 줄 | 결과 |
|---|---|---|
| 1 | 변경 범위 · 해시(ex09 · ex12 4파일, ai 해시 일치, ex08 · ex11 07e10b1e…, grouping f085810c…) | [OK] |
| 2 | 지시문 문구 테스트 | [OK] |
| 3 | 사진 접두 테스트(제외 · 번호 완전성 · 호출 0) | [OK] |
| 4 | ex12 `npm test` 118/118 | [OK] |
| 5 | ex12 `npm run build` | [OK] |
| 6 | ex09 `npm test` 114/114 | [OK] |
| 7 | ex09 `npm run build` | [OK] |
| 8 | vitest가 추적 파일 바꿈 없음 | [OK] |
| 9 | 실제 정리(입력 해시 일치 · 형식 통과 · 빠진 0 · OpenAI 2) | [OK] |
| 10 | 실제 검색 기준(5/6 · 최대 58 · 사람 검토 · 오병합 0) | [OK] |
| 11 | 접두 사진 5건 전부 excluded(제품 = 심사 독립 판정, 배정 · 메타 입력에 없음) | [OK] |
| 12 | DB 쓰기 0 · 9테이블 건수 · md5 전후 같음(articles 443 · issues 235 · collect_runs 72 · 나머지 0, 제작 실행 직후 값과도 같음) | [OK] |
| 13 | 호출 한도 · 크레딧(newsdata 8/8 · OpenAI 2/2, 직전 89 ≥ 70) | [OK] |
| 14 | 위반 없음(누출 false · 코드 속 키 없음(테스트 더미 키만) · 새 패키지 0 · DB 쓰기 0 · 실제 데이터 변경 0) | [OK] |
| 15 | 다음 날 분포(푸시 뒤 첫 예약 실행) | [SKIP — PLAN상 10-16 확인 · 푸시 뒤] |

**합계: OK 14 · FAIL 0 · SKIP 1.**

### 10.8 발견한 점(심각도 순)
1. (참고 · 중) 같은 입력에서 AI 묶음이 실행마다 달라짐: 이번엔 CPI 3건 · 충남학 2건 병합 누락, 동의어 0. 첫 후보 기준은 통과했지만 동의어 효과는 재현되지 않았다 → 10-17 이후 매일 분포로 확인.
2. (참고 · 하) 어학 칼럼(실용한자) 미제외 1건(하위 순위, 첫 후보 아님).
3. (참고 · 하) `PHOTO_PREFIX`는 `[섹션] ` 바로 뒤 반각 `[사진]`만 잡는다(앞 공백 · 전각 괄호 · `[ 사진 ]`은 안 잡음). 이번 데이터엔 해당 없음(독립 판정과 일치). PLAN에 없는 경계라 참고만.
4. (기록) a144처럼 접두 사진 + 발언 기사도 제외 — 승인 규칙대로.

### 10.9 호출 · 크레딧 · 증거
- newsdata **8/8**: 직전 확인 1(200 · 남은 **89** = 제작 직후 90 − 이 확인 1) + 검색 6(group 마지막 83) + 직후 확인 1(200 · 남은 **82**). 내일 예약 실행 몫(최대 54) 남음.
- OpenAI **2/2**: 배정 입력 16,478(캐시 16,475) · 출력 3,209(추론 2,276) / 메타 입력 22,951 · 출력 3,434(추론 50) → 약 0.007달러(D18 단가 기준).
- 증거(`ex12/.data/judge-terms-20261015/`, git 무시, `check-files` leak=false): `input-lines.txt` `7526497b…` · `openai-responses.json` `6b77ce47…` · `group-result.json` `38cf16f2…` · `newsdata-page1.json` `278d8f48…` · `result.json` `63f81ced…` · `review.json` `0fc3c5e0…`. 스크립트 `ex12/.data/judge-terms.ts` `0ceb3b1a…`(leak=false).

## 11. 2026-10-16 KST — D19-3 뉴스 모음 기사 머리표 제외 독립 심사

기준: `plan/PLAN_backend_ex12.md` 「뉴스 모음 기사 머리표 제외 (D19-3)」 · PLAN.md 5절 D19-2 · D19-3. 제작 보고 `reports/ex12_build.md` 12절은 참고만 하고 코드 · 테스트 · DB를 직접 확인했다. 코드 수정 · 커밋 · 외부 호출 없음.

**판정: PASS**(OK 13 · FAIL 0 · SKIP 0).

### 11.1 PLAN 항목별
| 항목 | 결과 | 근거 |
|---|---|---|
| 제외 규칙(제목 `[섹션] ` 뒤 맨 앞 대괄호 머리표 안, 공백 제거 후 헤드라인 · 뉴스리뷰 · 주요뉴스 · 뉴스브리핑) | done | `excludedBeforeAi`: `TITLE_TAG = /^\S+ \[[a-z]+\] \[([^\]]*)\]/`의 머리표 글자에서 `\s` 제거 후 `DIGEST_WORD` 검사. 첫 머리표만 봄 |
| 사진 접두와 같은 위치 · 방식, AI 배정 입력 · 스키마 · 후보 힌트 · 메타 입력에서 빠지고 excluded | done | organize의 두 줄 `PHOTO_PREFIX.test` → `excludedBeforeAi`. `assignInput(rest)`(후보 힌트 `candidateGroups`도 rest만) · 배정 스키마 `restNumbers` · 메타 입력 `byNumber`(rest만) · 전부 해당 시 AI 호출 0 그대로 |
| 본문 속 낱말 유지 | done | 머리표가 아닌 제목 · 설명문, 둘째 머리표(`[단독] 헤드라인…`)는 판정 안 함(테스트 · 경계 확인) |
| 사진 접두 규칙 불변 | done | `PHOTO_PREFIX` 줄 diff 없음 |
| 지시문 · 스키마 · 모델 · effort · grouping.ts 불변 | done | diff에 SYSTEM · Schema · MODEL 줄 변경 0, `grouping.ts` 둘 다 `f085810c…` |
| ex09 해시 일치 복사 · ex08 · ex11 보존 | done | 아래 11.2 |
| 단위 테스트(머리표 4종 · 띄어쓰기 변형 제외, 본문 속 유지, 번호 완전성) | done | 새 1개: `[이시각헤드라인]` · `[주요 뉴스]` · `[뉴스 브리핑]` · `[뉴스리뷰]` · `[헤드라인]` 제외, `헤드라인 뉴스 개편 발표 \| 헤드라인` · `[단독] 헤드라인 조작 의혹` 유지, `[포토]` 함께 제외, 배정 input · 스키마 · 메타 input에 제외 번호 없음, 묶음+제외 = 전체 |
| 실데이터 오탐 검사(일치 1건뿐) | done | 11.4 |

### 11.2 변경 범위 · 해시(직접 확인)
- `git status --short`: 코드는 ex09 · ex12의 `server/ai.ts` · `test/ai.test.ts` 4개뿐(나머지는 HANDOFF.md · PLAN.md · plan/PLAN_backend_ex12.md · reports/ex12_build.md 문서). `git diff --stat ex08 ex11` 빈 출력. 두 폴더 diff는 경로만 빼면 같음.
- SHA-256: ex09 · ex12 `server/ai.ts` `dff44e9f…`(같음) · `test/ai.test.ts` `c096e870…`(같음) · ex08 · ex11 `server/ai.ts` `07e10b1e…`(불변) · ex09 · ex12 `grouping.ts` `f085810c…`(불변). 새 패키지 0.

### 11.3 테스트 · 빌드(이번 심사에서 다시 실행)
| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 119/119 |
| `bash scripts/ex12-ubuntu.sh npm run build` | 성공(종료 0, `/_not-found` · `/api/[...path]`) |
| `bash scripts/ex09-ubuntu.sh npm test` | 14파일 115/115 |
| `bash scripts/ex09-ubuntu.sh npm run build` | 성공(종료 0) |

전후 `git status --short` 같음(vitest가 추적 파일을 바꾼 것 없음, checkout 불필요).

### 11.4 독립 실데이터 검사(읽기 전용, 제작 스크립트 미사용)
방법: 심사자가 새로 쓴 임시 스크립트 `ex12/.data/judge-digest-1016.ts`(sha256 `14363418…`) + 자체 안전 실행 `ex12/.data/judge-safe-1016.mjs`(`6234d76f…`; `.env` 값 7개와 DATABASE_URL 비밀번호 · 사용자 · 호스트를 출력에서 찾아 없을 때만 출력). 결과 3회 모두 `leak:false` · 종료 0. fetch 차단(호출 0), DB 클라이언트 생성 try 안 · 오류는 코드만. `sql.begin('read only')` 안에서 `transaction_read_only = on` 확인 후 articles 전체를 읽고, 날짜별로 제품 `inputLines`로 입력 줄을 만들어 제품 `excludedBeforeAi`를 적용. 동시에 PLAN 문장대로 쓴 심사자 독립 판정(제목 맨 앞 `[…]` 공백 제거 후 4낱말 · `[사진|포토|화보]` 접두)과 비교. 스크립트 2개는 끝나고 삭제. 결과 `ex12/.data/judge-digest-20261016/result3.json`(`17a0066d…`, git 무시).

| 확인 | 결과 |
|---|---|
| 대상 | 기사 610건(10-07 60 · 10-08 109 · 10-14 116 · 10-15 158 · 10-16 167), 입력 줄 610 |
| 모음 머리표 일치 | **1건**: id 1315 2026-10-16 pol `[이시각헤드라인] 10월 15일 뉴스리뷰` → 기대와 같음, 오탐 0 |
| 사진 접두 일치 | **7건**: 10-15 `[사진]` 5(트리플에스 김채연 2 · 트와이스 미나 2 · 버넘 英 총리) · 10-16 `[포토]` 2(담양터미널 · 구탕) — 모두 제품 제외 |
| 제품 판정 = 독립 판정 | 불일치 0/610 |
| 앞 공백 · 전각/다른 괄호로 시작하는 제목 | 0건 |
| DB 쓰기 0 | articles 610 · md5 `c54d0a0f…` · issues 370, 읽기 전후 같음(3회 모두) |
| 외부 호출 | newsdata 0 · OpenAI 0 · fetch 0 |

누락 검토(사람): ① 넓은 낱말(헤드라인 · 뉴스리뷰 · 주요뉴스 · 브리핑 · 뉴스모음 · 모음 · 이시각 · 간추린 · 한눈에 · 종합뉴스 · 아침/저녁뉴스 · 오늘의뉴스 · 리뷰 · 요약 · 브리프 · 조간 · 석간 · 영문 headline/briefing/digest 등, 제목 공백 제거)이 들어갔지만 제외되지 않은 제목 3건 — `광양 앞바다가 한눈에…`(관광 시설) · `내년 주총부터 안건 한눈에 본다…`(제도 개편)는 단일 기사, **`[간추린 단신] LG에너지솔루션, 문화체험 후원금 기탁 외`**(10-16 it, id 1449)는 여러 단신을 묶은 모음 성격. ② 맨 앞 머리표 57종 전체 목록 · 머리표 달린 제목 전부를 사람이 봄: 칼럼(`[노트북]` · `[춘추칼럼]` · `[참성단]` · `[Dr.이은봉의 의학 연구 다이제스트]` 등) · 방송 단일 꼭지(`[사건why]` · `[이슈빡]`) · 시황(`[올댓차이나]` · `[코인 시황]` · `[베트남 증시]`) · 운세 · 만평 · 인사 · 부음이며, 하루 뉴스 모음은 위 1315와 `[간추린 단신]` 1449뿐. ③ 설명문에 `■▲◆●▶`가 2개 이상인 기사 5건도 봄: 1315 외에는 방송 단일 꼭지 3 · 인사 1로 모음 아님.
→ PLAN의 4낱말 기준으로는 누락 0. `[간추린 단신]`은 PLAN 범위 밖의 같은 성격 기사(11.6-1).

### 11.5 경계 입력(참고, PLAN 밖)
제품 함수에 직접 넣어 봄(DB · 외부 없음, 임시 파일 삭제): 제외 = `[헤드라인 단독]` · `[ 주요 뉴스 ]` · `[뉴스 리뷰]` · `[이시각 헤드라인]` · `[KBS 뉴스브리핑]` · `[오늘의 주요뉴스]` · `[헤드 라인]`. 유지 = `［포토］` · `［헤드라인］` · `【헤드라인】` · 앞 공백 `[포토]` · `[이시각헤드라인]` · 둘째 머리표 `[단독][헤드라인]` · `[단독] [포토]` · `[브리핑]` · `[뉴스 모음]` · `[간추린 뉴스]` · `[HEADLINE]` · `[포토 ]` · `[ 사진]`. 저장 610건에는 앞 공백 · 전각 괄호 제목이 없어 실제 영향 0.

### 11.6 발견한 점(심각도 순)
1. (참고 · 중) `[간추린 단신] … 외`(id 1449, 10-16 it) — 단신 모음 기사인데 4낱말 밖이라 AI 배정에 들어감. PLAN 기준상 누락은 아니며, `간추린`/`단신`을 넣을지는 사용자 결정 사항(넓히면 오탐 재검사 필요).
2. (참고 · 하) 판정이 머리표 안 어디든 낱말이 있으면 제외(`[헤드라인 단독]`도 제외) — 제작 보고 12.4-1과 같음. 반대로 `[뉴스 모음]` · `[브리핑]` · `[간추린 뉴스]` · 전각 괄호 · 앞 공백은 안 잡음. 현재 데이터 영향 0.
3. (기록) 제작 보고 12.3의 "모음 낱말이 제목 어디든 있으나 제외되지 않은 제목 0건"은 4낱말 기준으로 맞음. 이번 넓은 낱말 검사에서는 3건(무관 2 · 간추린 단신 1).
4. (기록) 다음 예약 실행에 반영하려면 커밋 · 푸시 필요(사용자 확인). 지시문 불변이라 OpenAI 재검증 불필요 — PLAN 검증표와 같음.

### 11.7 점검 줄 합계
| # | 줄 | 결과 |
|---|---|---|
| 1 | 변경 범위(코드 4파일) · 해시(ai · 테스트 일치, ex08 · ex11 07e10b1e…, grouping f085810c…) | [OK] |
| 2 | 제외 규칙 구현(머리표 · 공백 무관 · 첫 머리표만 · 같은 위치) | [OK] |
| 3 | 배정 입력 · 스키마 · 후보 힌트 · 메타 입력에서 빠짐 · excluded · 번호 완전성 · 지시문/스키마/모델 불변 | [OK] |
| 4 | 단위 테스트 내용(4종 · 띄어쓰기 변형 · 본문 속 유지 · 사진 함께) | [OK] |
| 5 | ex12 `npm test` 119/119 | [OK] |
| 6 | ex12 `npm run build` | [OK] |
| 7 | ex09 `npm test` 115/115 | [OK] |
| 8 | ex09 `npm run build` | [OK] |
| 9 | vitest가 추적 파일 바꿈 없음 | [OK] |
| 10 | 실데이터 모음 일치 1건뿐(1315) · 오탐 0 · 제품 = 독립 판정 | [OK] |
| 11 | 사진 접두 7건 · 규칙 불변 | [OK] |
| 12 | 누락 검토(PLAN 4낱말 기준 누락 0, 범위 밖 1건 참고) | [OK] |
| 13 | 외부 호출 0 · DB 쓰기 0(read only on, 전후 같음) · 누출 false · 새 패키지 0 · 임시 스크립트 삭제 | [OK] |

**합계: OK 13 · FAIL 0 · SKIP 0 → PASS.**

## 12. 2026-10-16 KST — 남은 SKIP 4줄 재판정(7b · 9b · 11b · 13)

심사 역할(Claude Code), 제품 코드 · Vercel/GitHub 설정 · 재배포 · 커밋 · 푸시 없음, 서버 안 띄움. newsdata 0 · OpenAI 0.

### 12.1 로그 시간대용 확인 요청(이번 실행)
스크립트 `ex12/.data/judge-log.mjs`(sha256 `a6d4b6df…`), 결과 `ex12/.data/judge-log-verdict.json`(`0779bf12…`, git 무시). `bash scripts/ex12-ubuntu.sh node .data/judge-log.mjs` 종료 0, 출력은 `.env` 값 · DB 비밀번호 · 사용자 · 호스트 누출 검사 `leak:false` 뒤에만 봄. 쿠키 · 토큰 · 비밀번호 출력 0.

- **시간대: 2026-10-16 01:49:56Z ~ 01:50:05Z (10:49:56 ~ 10:50:05 KST)** — 첫 요청 직전 ~ 악성 Origin 요청 직후. 9b 확인 2회는 그 직후(다른 호스트, 01:50:05Z~01:50:06Z 무렵).
- `https://issuerank-kappa.vercel.app` 요청 10회(누계 76/150) + 9b 2회.

| 요청 | 상태 |
|---|---|
| GET `/api/rankings` | 200(date 2026-10-16) |
| GET `/api/rankings?date=2000-01-01` | 404 |
| POST 가입(`ex12-log-<uuid>@example.invalid`) | 200 |
| GET get-session | 200(본인 id 일치) |
| POST `/api/folders` | 201 |
| POST `/api/bookmarks`(그날 Top 5 첫 카드) | 201 |
| GET `/api/bookmarks` | 200(1개, 같은 id) |
| DELETE `/api/bookmarks/:id` | 204 |
| POST sign-out | 200 |
| POST 로그인 `Origin: https://evil.example` | **403**(Set-Cookie 0) |

정리(finally): 이번 userId의 bookmarks → folders → session → account → user 삭제, 남은 행 b0 · f0 · s0 · a0 · user0. 9테이블 건수 · md5 전후 **같음**(시작 시 articles 610 · issues 370 · collect_runs 78 · bookmarks 1 · folders 1 · user 1 · session 1 · account 1 · verification 0 — 사용자 본인 계정 행 보존).

### 12.2 SKIP 줄 재판정
| # | 줄 | 판정 | 근거 |
|---|---|---|---|
| 7b | 배포 설정 — 대시보드 화면 | **OK(사용자 확인)** | 사용자가 Root `ex12` · Next.js · Node 24.x · icn1 · 환경 변수 3개(Production) · Standard Protection 확인 |
| 9b | 배포 고유 주소 보호 | **OK** | 코디네이터 확인(로그인 없이 GET → 302 `vercel.com/sso-api`)에 더해 심사가 각 1회 다시 확인: `https://issuerank-c6f5r36em-jwavascripts-projects.vercel.app/api/rankings` **302 → `https://vercel.com/sso-api`**, `https://issuerank-git-main-jwavascripts-projects.vercel.app/api/rankings` **302 → `https://vercel.com/sso-api`**(쿼리 생략). 공개는 Production 주소뿐 |
| 11b | 두 기기 ②(사용자 PC · 폰) | **OK(사용자 확인)** | 사용자가 PC · 폰에서 같은 계정 북마크 연동 확인(DB에도 사용자 행 user 1 · bookmarks 1 · folders 1 존재) |
| 13 | 배포 로그 | **SKIP — 대기** | 아래 시간대 Vercel › Logs를 사용자가 줄 때 판정 |

참고(판정 근거 아님): 사용자가 준 2026-10-16 01:12~01:42Z 로그 12줄은 모두 level info · 200 · 오류 0. 고유 주소에서 200인 줄은 로그인한 브라우저(Vercel SSO 통과) 요청으로 보임 — 비로그인 302와 모순 없음.

**13 기대 결과**: 시간대 **2026-10-16 01:49:50Z ~ 01:50:10Z(10:49:50 ~ 10:50:10 KST)**의 `issuerank-kappa` 요청 10줄(위 표와 같은 경로 · 상태) — 5xx 0, 비밀값 · 쿠키 · 토큰 · 비밀번호 · SQL 인수 원문 0, 악성 Origin 로그인은 403. error 레벨 0을 기대하되, 2026-10-15 로컬 심사에서 악성 Origin 1회에 better-auth가 `AUTH_INTERNAL_ERROR` 안전 코드 한 줄을 남긴 적이 있으므로(5절 13줄) **그 403 요청에 붙은 이 코드 한 줄만은 예상된 줄**로 보고 FAIL로 치지 않는다(다른 error · 스택 · 원문 값이 있으면 FAIL). 9b 302 두 건은 SSO 단계라 앱 로그에 없을 수 있다.

**합계(5절 17줄): OK 16 / FAIL 0 / SKIP 1(13 배포 로그 대기) → PASS 보류.** 13이 기대대로면 ex12 PASS.

## 13. 배포 로그 줄 판정 · 최종 (2026-10-16)

사용자가 Vercel Logs(01:26~01:56Z, 22줄)를 전달. 12절 확인 요청 시간대 01:49:56~01:50:05Z에 해당하는 줄은 10줄로 요청 10회와 경로·상태가 모두 일치(rankings 200 · 404, sign-up 200, get-session 200, folders 201, bookmarks POST 201 · GET 200 · DELETE 204, sign-out 200, 악성 Origin sign-in 403). 5xx 0. level error는 403 요청의 `AUTH_INTERNAL_ERROR` 안전 코드 1줄뿐(12절에 예상된 줄로 명시, 스택·원문 없음). 22줄 전체에 비밀값·쿠키·토큰 원문 0. 배포 id는 e7dcd99 푸시 후 자동 재배포분(dpl_Btcr…)이며 ex12 코드 동작은 동일. 나머지 12줄은 사용자 브라우저 요청(모두 200 info).

**13 배포 로그 [OK].** ex12 최종 합계 OK 17 / FAIL 0 / SKIP 0 → **PASS**(D19·D19-2·D19-3 보완 심사 포함). 기록: 코디네이터(사용자 전달 로그 대조).

## 14. 2026-10-16 KST — D20 모바일 이슈맵 가독성 독립 심사

심사 역할(`agent_prompts/3_judge_agent.md`), Claude Code, Termux Chromium 155 headless + CDP, proot Ubuntu `next start -p 3000 --hostname 127.0.0.1`. 기준: `plan/PLAN_backend_ex12.md` 끝 절(D20), PLAN.md 5절 D20 · 6절. 제작 보고 14절은 참고만 했고 제작 스크립트(`verify-mapmobile.mjs`)는 쓰지 않았다. 측정 스크립트는 스크래치 폴더에 새로 작성 후 삭제. 제품 코드 수정 · 커밋 · 푸시 · stash 없음.

**판정: PASS 보류 — OK 18 / FAIL 0 / SKIP 1(배포 360px, 푸시 후).** 로컬에서 확인할 수 있는 줄은 모두 통과.

### 14.1 PLAN 항목별
| 항목 | 판정 | 근거 |
|---|---|---|
| 모바일 배치(640px 이하 세로 쌓기, 가로 스크롤 0, 확대 · 범례 · 이동 그대로) | done | `drawMap()` `stack = NARROW.matches && !state.zoom`, 블록 높이 260~320. 360 · 375 · 390 · 640에서 stacked, 641에서 기존 squarify |
| 360px 각 섹션 1위 제목 2줄 이상 | done | 360 1위 제목 줄 수 2·3·2·3·3·3 |
| 작은 타일 `n위 · m건`, xxs 0, 누르면 기존 상세창 | done | CSS `.tile.xs .rk` 숨김 해제 + `::after " ·"`. 360 xs 2개 `4위 · 1건`·`5위 · 1건`, xxs 0 |
| 유지(641px 이상 배치 · 산식 · 색 · 문구 · aria-label · API, 새 패키지 0, ex09 미복사) | done | diff 상 squarify · L() · 색 · aria-label 줄 변경 없음, 641 · 768 · 1280 좌표 동일 |

### 14.2 범위 · 기존 코드 경로
- `git diff --stat`: 제품 변경은 `ex12/public/index.html` 하나(+11 −1, sha256 `348787b4…` = 제작 보고). 나머지는 문서(HANDOFF · PLAN · PLAN_backend_ex12 · ex12_build). ex09 · 다른 단계 · package 파일 변경 0.
- 641px 이상 · 확대 상태: `stack=false` → `map.style.height=''`(변경 전엔 인라인 높이 없음 = 같은 CSS 높이), gRects는 기존 zoom 식 · squarify 식 그대로. 새로 계산되는 `shown`·`boxH`는 쓰이지 않음. 640 이하 확대도 CSS 높이 760으로 기존과 같음.

### 14.3 테스트 · 빌드
| 명령 | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | 14파일 119/119, 앞뒤 `git status --short` 같음 |
| `bash scripts/ex12-ubuntu.sh npm run build` | 변경 전(HEAD 임시 교체) · 변경 후 모두 종료 0, 출력 비밀값 검사 leak:false |

변경 전 기준: `git show HEAD:ex12/public/index.html`을 스크래치에 두고(HEAD `01cbdb25…`, 작업본 `348787b4…`) index.html만 교체 → 빌드 · 측정 → 작업본 복원, 복원 후 해시 `348787b4…` 일치.

### 14.4 뷰포트별 측정(10-16 랭킹 30타일, 직접 작성한 DOM 측정)
| 뷰포트 | 배치 | 지도 높이 | xs(전→후) | xxs | 순위 보임(전→후) | 1위 제목 · 줄 수 | 가로 스크롤 | 넘침 |
|---|---|---|---|---|---|---|---|---|
| 360×800 | 세로 쌓기 | 760→1572 | 12→2 | 0 | 18→30/30 | 6/6 · 2,3,2,3,3,3 | 0 (360=360) | 0 |
| 375×667 | 세로 쌓기 | 760→1644 | 9→2 | 0 | 21→30/30 | 6/6 · 2,3,2,3,3,3 | 0 | 0 |
| 390×844 | 세로 쌓기 | 760→1716 | 9→0 | 0 | 21→30/30 | 6/6 | 0 | 0 |
| 640×900 | 세로 쌓기 | 760→1920 | 2→0 | 0 | 28→30/30 | 6/6 | 0 | 0 |
| 641×900 | 기존 트리맵 | 640=640 | 4=4 | 0 | 26→30/30 | 6/6 | 0 | 0 |
| 768×1024 | 기존 트리맵 | 764=764 | 0 | 0 | 30/30 | 6/6 | 0 | 0 |
| 1280×800 | 기존 트리맵 | 540=540 | 0 | 0 | 30/30 | 6/6 | 0 (1265≤1280) | 0 |

- 순위 보임 = `.rk`가 표시되고 타일 사각형 안에 있으며 `n위`로 시작. 넘침 = 보이는 자식(.t · .foot · .rk · .n)이 타일 밖이거나 타일 scrollWidth/Height > client.
- 360 · 390에서 타일 30개를 하나씩 화면 가운데로 스크롤해 `elementFromPoint` = 그 타일 30/30(덮임 없음).
- 넓은 화면 좌표: 641 · 768 · 1280에서 섹션 블록 6개 · 타일 30개의 id별 x · y · w · h · xs · xxs가 변경 전과 **모두 같음**, 768 · 1280 캡처는 PNG 해시까지 같음.
- 640/641 경계: 640 세로 쌓기(블록 320 × 6), 641 기존 2열 트리맵 — `@media (max-width: 640px)`와 일치.

동작(360, 변경 후): 가장 작은 정치 4위(67.6×130.8) 클릭 → 상세창 h2 = 이슈 제목 OK · 정치 확대 → 정치 블록만 · 높이 760 · 가로 스크롤 0, 전체 복귀 → 좌표 처음과 같음 OK · 이전 날짜(10-15) → 세로 쌓기 · 30타일 순위 30/30 · xxs 0, 오늘 복귀 → 좌표 처음과 같음 OK · 창 360→1280→360 → 1280은 새로 연 1280과, 360은 새로 연 360과 좌표 같음 OK · 다크(prefers-color-scheme) → 배경 rgb(18,18,17), 세로 쌓기 · xxs 0 · 가로 스크롤 0 OK.

캡처(`ex12/.data/judge-mapmobile-20261016/`, git 무시): `before-*` · `after-*` 7뷰포트, `after-360-{drawer,zoom-pol,prevday,dark,view-soc,view-cul}.png`, 측정값 `before.json` · `after.json`. 눈으로 확인: after-360(타일마다 순위 · 1위 제목), before-360(xs `n건`만), after-641(기존 2열, xs `4위 · 1건`), after-360-drawer, after-360-dark, after-360-view-soc. 지도 전체 길이 캡처는 중간이 비어 보이는데 닫힌 바텀시트(82vh)가 늘린 캡처 영역을 덮는 캡처 방식 문제이며(변경 전 캡처도 같음), 스크롤 뷰포트 캡처와 elementFromPoint 30/30으로 화면 문제 아님을 확인.

### 14.5 외부 호출 · DB
- 브라우저 요청(localhost): `/`, `/api/rankings`(날짜 이동 포함), `/api/auth/get-session`만. newsdata · OpenAI 0(랭킹 라우트는 store만 import, 서버 로그 Error · newsdata · openai 0줄). 글꼴 CDN(jsdelivr Pretendard)은 기존 페이지 동작.
- DB: 심사 전후 9개 표(articles 610 · issues 370 · bookmarks · folders · collect_runs · user · account · session · verification) 행 수 · md5 같음 → 쓰기 0. 서버 · 빌드 · 스냅숏 출력 비밀값 검사 모두 leak:false.

### 14.6 점검 줄 합계
| # | 줄 | 판정 |
|---|---|---|
| 1 | npm test 119/119 | OK |
| 2 | npm run build(전 · 후) | OK |
| 3 | diff 범위(제품은 index.html뿐) | OK |
| 4 | 641 이상 · 확대 기존 코드 경로 | OK |
| 5 | 360 · 375 · 390 · 640 순위 30/30 | OK |
| 6 | 섹션 1위 제목(360 2줄 이상) | OK |
| 7 | xxs 0 | OK |
| 8 | 가로 스크롤 0(7뷰포트) | OK |
| 9 | 텍스트 넘침 0(7뷰포트) | OK |
| 10 | 640/641 경계 전환 | OK |
| 11 | 641 · 768 · 1280 좌표 변경 전과 동일 | OK |
| 12 | 타일 클릭 → 상세창 제목 | OK |
| 13 | 섹션 확대 · 복귀 | OK |
| 14 | 날짜 이전 · 오늘 재배치 | OK |
| 15 | 창 360→1280→360 재배치 | OK |
| 16 | 다크 모드 | OK |
| 17 | 외부 호출 0 · DB 쓰기 0 · 비밀값 누출 0 | OK |
| 18 | 임시 교체 원복 해시 · 프로세스 정리 | OK |
| 19 | 배포 주소 360px 캡처 | SKIP — 푸시 후 |

**합계 OK 18 / FAIL 0 / SKIP 1.**

### 14.7 발견한 점(심각도 순, 모두 참고)
1. 641~767px에서도 xs 타일이 이제 `n위 · m건`을 보인다(오늘 641에서 4개, 전엔 `1건`만). 배치 · 좌표는 같고 PLAN 「작은 타일」 줄(폭 조건 없음)과 맞음. 제작 보고는 768 · 1280의 xs 0만 언급.
2. 360 · 375에서 정치 4 · 5위는 여전히 xs(폭 67.6/70.8 < 72)라 제목 없이 `n위 · m건` — PLAN이 허용한 형태, 누르면 상세창 제목.
3. 페이지 전체 길이 캡처는 닫힌 바텀시트가 가린다(캡처 방식 문제, 제작 보고에도 언급). 배포 360 캡처도 뷰포트 캡처를 권장.

### 14.8 정리
next-server · npm exec · proot 트리와 Chromium(본체 · zygote) 종료 → curl 3000 = 000 · CDP 9447 = 000, `ps -ef | grep -E "next-server|next start|chromium"` 0줄. (첫 확인에서 `grep -v claude`가 프로필 경로의 `claude-…` 때문에 Chromium 줄까지 걸러 9447이 200으로 남은 것을 발견해 다시 종료함.) 브라우저 프로필 · 임시 스크립트(`ex12/.data/judge-d20-snap.mjs` 포함) · 백업 사본 삭제, `git status --short`는 심사 전과 같음(본 보고 · HANDOFF 제외).

### 14절 보충: 배포 확인 (2026-10-16, 코디네이터)

커밋 62c3d3d 푸시 후 Vercel 자동 재배포. 약 20초 뒤 `https://issuerank-kappa.vercel.app/` 응답 본문 SHA-256이 로컬 심사 파일 `ex12/public/index.html`(348787b4…)과 일치 — 배포된 화면 코드가 로컬에서 7뷰포트 측정한 코드와 동일하고, 배치는 이 파일과 같은 API 데이터만으로 결정된다. 배포 주소 360px 줄 **[OK](배포 파일 해시 일치)**. 실제 기기 화면은 사용자 휴대폰 확인을 권장(판정 외). D20 최종 OK 19 / FAIL 0 / SKIP 0 → **PASS**.
