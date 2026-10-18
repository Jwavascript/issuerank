# ops O1 홈 화면 위젯 1단계 — 독립 심사 (2026-10-17)

> 역할: 심사(`agent_prompts/3_judge_agent.md`), Claude Code 세션, 제작자와 다른 독립 심사. 대상: `ex12/`의 미커밋 변경(작업 트리). 기준: `plan/PLAN_ops_O1_widget.md`(검증 21줄 + 끝 「범위 추가 (D25)」) · SPEC US-11 · AC-11-1~4 · 회귀 AC · PLAN.md 5절 D3 · D8 · D10 · D16 · D20~D25 · AGENTS.md.
> 제작 보고 `reports/ops_O1_widget_build.md`는 참고만 했고 제작 스크립트(`verify-o1-widget.mjs` · `verify-o1-d25.mjs`)는 실행하지 않았다. 측정 스크립트는 모두 새로 작성(`ex12/.data/judge-o1-20261017/`).
> 제품 코드 수정 0(측정용 임시 교체도 0 — 변경 전 판 비교는 브라우저 안에서 문서만 바꿔 줌) · 커밋 · 푸시 · stash 0 · Vercel/GitHub 설정 · Actions 0 · `web/` 0. 심사 전후 대상 15파일 sha256 같음, `git status`는 이 보고(새 파일) · HANDOFF 상태 줄 말고 같음.

**판정: PASS 보류 — 푸시 후 배포 줄(15 · 16 · 17 · 21②③) 확인 + iPhone(18) 대기.**
검증 21줄 **OK 16 · FAIL 0 · SKIP 5**(15 · 16 · 17 · 21 = 푸시 후, 18 = 사용자 iPhone 대기). D25 추가 검증 2줄 **OK 2**. 로컬에서 확인할 수 있는 줄은 모두 통과했고 위반 없음.

## 1. PLAN 항목별
| 항목 | 판정 | 근거(심사가 직접 확인) |
|---|---|---|
| 공개 `GET /api/widget`(쿼리 무시, `routes/widget.ts`를 `/api/rankings`처럼 연결, BFF · route.ts 그대로) | done | `app.ts`에서 세션 미들웨어보다 앞에 `app.route('/api/widget', …)`. 로컬 서버: 쿠키 없이 200, `?date=2000-01-01&x=1`도 같은 본문 · 같은 헤더. bff.ts · route.ts 해시 = HEAD |
| 200 본문 5칸 · 항목 4칸 · `updatedLabel` · `morePath` | done | 칸 `date · items · morePath · updatedAt · updatedLabel`, 항목 `id · path · section · title`뿐. `updatedLabel` `10.17 오전 8:34 수집` = 내 계산 = SQL 그날 ok 실행 최대 finished_at `08:34:27` |
| 연관기사 수 · 요약 · url · rank 넣지 않음 | done | 응답 본문에 `articleCount · summary · url · rank · importance · latestPublishedAt` 키 0 |
| 순서(1위들 → 나머지, 각각 D16, `compareIssues` 공유, `rankIssues` 불변) | done | 서버 코드 없이 SQL 행만으로 계산한 순서와 30개 완전 일치(5.2). HEAD `ranking.ts`와 무작위 2000묶음 `rankIssues` · `buildRanking` 출력 차이 0 |
| 항목 수 전부(≤30) · 크기 인자 없음 | done | 30개, 쿼리 무시 |
| 실패(404 문구 · 500 onError · 502 BFF · GET 말고 404) | done | 404 · 캐시 헤더 없음은 단위 테스트, 로컬 `POST /api/widget` → 404. 500 · 502는 기존 경로 그대로(라우트에 별도 처리 없음) |
| 캐시(200만 `public, max-age=0, s-maxage=300`, Set-Cookie 0) | done(로컬) | 로컬 200 헤더 그대로(Next가 바꾸지 않음), Set-Cookie 0. Vercel 쪽 동작은 15번(푸시 후) |
| 계약 · `openapi.yaml`(0.2.0, `/api/widget` 200 · 404, `Widget` · `WidgetItem`) | done | YAML 구조 비교: 다른 경로 · 스키마 차이 0, 실제 응답이 `Widget` 스키마에 맞음(5번) |
| `/?issue=` 바로가기(즉시 replaceState · 형식 · `?date=` 1번 · 그리기 뒤 상세창 · 지난 날짜 화면) | done | 브라우저 ①②⑤, VM 경계 14경우(5.4 · 5.5) |
| 바로가기 실패(형식 · 미래 무시, 404 빈 화면, id 없음 → 토스트) + D25(5xx · 네트워크는 기존 토스트) | done | 브라우저: 없는 번호 · `abc` · 내일 id · `2000-01-01` · 500 · 502 · 네트워크 실패(5.4) |
| 상세창 "수집" = 실제 수집 완료 시각(D8 카드는 날짜만, "오전 6:00" 삭제) | done | 브라우저 `10.17 오전 8:34` · 지난 날 `10.16 오전 8:41`, VM 5시각 + D8 카드 `10.8` |
| Web Analytics 2줄(index · widget, `</body>` 앞, `window.va` 호출 0) | done | 두 파일 모두 PLAN 문구 그대로 1번씩, 앱 코드의 `va(` 호출 0 |
| Scriptable `widget.js`(버전 주석 · BASE · 모듈 0 · 최상위 await · 문서 API만 · 요청 1개 10초) | done | 가짜 전역 하네스(문서 이름표 밖 접근은 위반으로 던짐) 위반 0, 정적 검사 전역 7개뿐 |
| 크기별 줄 수 · 글꼴 · 줄 제한 · 간격 / 누르기 / 하단 · 색 / 갱신 힌트 / 오프라인 | done | 하네스 826검사 × 5시간대 통과(5.3) |
| 설치 안내 `widget.html` + rewrite `/widget` | done | 안내문 1~7 PLAN과 글자까지 같음, 읽기 전용 칸 = `/widget.js`, 실제 클릭 복사 · 대체 경로 · 읽기 실패 문구 · 다크 · 360px · 외부 리소스 0 |
| Android 안내 칸만(코드 0) | done | 7번 문구 그대로 · 360px 화면 안, Android 파일 0 |
| 측정(새 DB 테이블 · 개인정보 · 사용자 지정 이벤트 0) | done | 스키마 · 마이그레이션 변경 0, 쿠키 0 |
| D25 CSS(`main { overflow-x: clip }`) · 실패 문구 | done | 5폭 × 애니메이션 켬/끔 × 6전환 넘침 0, 좌표 · 목록 배치 · sticky = 변경 전(5.4) |
| 테스트 추가(ranking · api · auth-proxy · widget-ui · widget-script) | done | 16파일 142/142 |

partial · missing · wrong: 없음.

## 2. SPEC AC
| AC | 판정 | 근거 |
|---|---|---|
| AC-11-1 섹션 1위들(건수 ↓) → 나머지(건수 ↓, 동점 D16), 크기만큼 제목만, 숫자 없음 | 충족(로컬 · 가짜 전역) · 실기기 보류(18) | 서버 순서 = SQL 독립 계산(동점 14쌍: 중요도 5 · 발행 시각 9로 갈림), 하네스: 줄 수 = min(n, 3 · 5 · 10 · 10 · 3 · 3 · 3 · 5), 글은 제목 + 하단뿐 |
| AC-11-2 줄 누르면 그 이슈 상세(요약 · 연관기사 · 원문) | 충족(로컬) · 실기기 보류 | 줄 url = BASE + `/?issue={id}`(medium · large · 앱), 사이트에서 그 주소 → 상세창 제목 · 3줄 요약 · `106건` · 원문 새 탭 |
| AC-11-3 갱신 시각 표시 | 충족(로컬) · 실기기 보류 | 하단 = `updatedLabel`(regular 10), 상세창 · 날짜 줄과 같은 시각 |
| AC-11-4 받지 못하면 마지막 내용 또는 "불러오지 못했습니다" | 충족(가짜 전역) · 실기기 보류 | 실패 19종 × 4크기 → 캐시 그대로 · 캐시 안 덮음, 캐시 없음/깨짐 → 한 줄 안내 · 예외 0 |
| 회귀 AC-03-1 · 03-2 · 04-1~3 · 05-1 · 05-2 · 07-3 · 09-1~3 | 충족 | 목록 6섹션 카드(건수 ↓) · 요약(없는 2개는 "요약을 준비 중입니다") · 원문 새 탭 · 날짜 줄 `… 수집 · 이슈 30개` · 360~1280 가로 넘침 0 · 비로그인 북마크 로그인 안내(저장 요청 0) · 지난 날짜 랭킹 · "오늘" 버튼 · `2000-01-01` 빈 화면 |

## 3. 범위 · 보호 파일 · 새 패키지
- 바뀐 파일(ex12): 수정 10(`next.config.mjs · openapi.yaml · public/index.html · server/app.ts · server/db/store.ts · server/ranking.ts · server/sample.ts · test/api.test.ts · test/auth-proxy.test.ts · test/ranking.test.ts`) + 새 5(`public/widget.js · public/widget.html · server/routes/widget.ts · test/widget-ui.test.ts · test/widget-script.test.ts`) = PLAN 구현 순서 2~4 목록뿐. 기존 테스트는 import 1줄 말고 추가만.
- 보호 15파일 sha256 = `git show HEAD:` 파일: `server/{bff,auth,ai,daily,steps,collect,group,grouping,newsdata,summary,summarize}.ts` · `app/api/[...path]/route.ts` · `package.json` · `package-lock.json` · `.github/workflows/daily.yml` 모두 SAME(심사 시작 · 끝 두 번). 새 패키지 0.
- ex01~ex11 · ex09 · frontend · web · SPEC.md · `.github` · `plan/PLAN_web.md` · `agent_prompts` · `scripts` 변경 0.
- 문서: PLAN.md +4줄(D23 · D24 사용자 행(제작 전) · D25 사용자 승인 행 · 10절 O1 메모 1줄), HANDOFF 상태 줄, 새 `plan/PLAN_ops_O1_widget.md` · `reports/ops_O1_widget_build.md`. 검증 1번의 "PLAN.md 변경 0"과 글자로는 다르지만 제품 결정 · 단계 메모 기록(AGENTS 규칙)이고 범위 밖 변경은 아님(참고).
- `summary.ts`(요약 대상)는 `rankIssues`를 그대로 import — 비교식 공유 뒤 결과 불변(HEAD와 2000묶음 비교 차이 0).

## 4. 테스트 · 빌드 · OpenAPI
| 명령(저장소 루트) | 결과 |
|---|---|
| `bash scripts/ex12-ubuntu.sh npm test` | **16파일 142/142** 통과(widget-script 5 · widget-ui 8 · ranking 14 · api 16 · auth-proxy 7 …). 앞뒤 `git status` 같음(vitest가 추적 파일 안 바꿈 → checkout 불필요) |
| `… env TZ=UTC / Asia/Seoul / America/Los_Angeles npx vitest run test/widget-script.test.ts test/widget-ui.test.ts` | 세 시간대 모두 13/13(테스트 이름에 TZ 찍힘) |
| `bash scripts/ex12-ubuntu.sh npm run build` | 종료 0: Next 16.4.0 "Compiled successfully" · `tsc --noEmit` 통과, `.next/routes-manifest.json`에 `/widget → /widget.html` rewrite |
| `node --check public/widget.js`(Termux · Ubuntu Node 24.18.0) | 둘 다 종료 0, 끝에 문법 오류를 붙인 사본은 종료 1(전체를 읽음), import · export · require 0, async 함수로 감싼 컴파일도 OK |
| `npx --yes @redocly/cli@2.64.1 lint openapi.yaml`(지금 · HEAD 판) | 둘 다 "valid" · **오류 0 · 경고 18**, 경고 규칙 구성 같음(info-license 1 · no-server-example.com 1 · get-session operation-4xx-response 1 · Section oneOf 15), 위젯 관련 경고 0 |
| YAML 구조 비교(`judge-openapi.cjs`, js-yaml) | 10/10: 3.1.0 · 0.2.0, `/api/widget`·`Widget`·`WidgetItem` 말고 차이 0, `/api/rankings` 바로 다음, get만 · 200 · 404 · 보안 없음, Cache-Control const, 404 = Error, required · items 1~30 · IssueId · SectionKey · path 패턴, **실제 로컬 응답이 `Widget` 스키마에 맞음** |

## 5. 검증 21줄 + D25 (심사가 직접 다시 실행)
환경: Termux + proot Ubuntu(Node 24.18.0), `npm run build` 뒤 `next start -p 3001 --hostname 127.0.0.1` + **심사가 쓴 요청 세기 프록시 `127.0.0.1:3000` → 3001**(상태 · 헤더 · 본문 그대로, 누계 100이면 전달 안 함). 지시의 `next start -p 3000` 대신 앞에 프록시를 둔 이유: 브라우저가 스스로 내는 요청(favicon 등)까지 3000 요청을 빠짐없이 세려고. 브라우저 접속 주소는 그대로 `http://localhost:3000`. 브라우저: Termux Chromium 155.0.8059.39 headless + CDP, 측정 탭은 about:blank에서 준비 뒤 이동, 기록 수만 주소로 바로 연 탭. 데이터: SQL 최근 수집일 **2026-10-17** · 직전 **2026-10-16**. 안전 실행: `ex12/.env`만 읽고 모든 출력 · 결과 파일을 비밀값 검사(leak:false) 뒤에만 냄.

| # | 판정 | 심사 결과 |
|---|---|---|
| 1 변경 범위 · 하루 실행 불변 | OK | 3절. 보호 15파일 = HEAD, 다른 폴더 0, 새 패키지 0 |
| 2 순서 규칙(단위) | OK | `test/ranking.test.ts` 14/14(새 7: 6×5 · 동점 · pol2/eco5 · 행 없음 · D8 · 입력 뒤집기 · 라벨 4시각). + HEAD와 2000묶음 비교 차이 0 |
| 3 위젯 API 계약(테스트) | OK | `test/api.test.ts` 16/16(새 2: 200 칸 · 30개 · rank 1 앞 · path · `10.7 오전 6:00 수집` · 캐시 헤더 · Set-Cookie 없음 · 쿼리 무시 / 404 같은 문구 · 캐시 헤더 없음 / POST 404) |
| 4 BFF 공개 경로 | OK | `test/auth-proxy.test.ts` 7/7(새 1: Cache-Control 그대로 · no-store 0 · 외부 fetch 0) + 로컬 서버에서도 헤더 그대로 |
| 5 OpenAPI | OK | 4절 |
| 6 바로가기(VM) | OK | `test/widget-ui.test.ts` 8/8 + 심사 VM `judge-drawer-vm.mjs`: 오늘 · 지난 날 · 윤년 2024-02-29 OK / 2026-02-29 · 13월 · 내일 · 없는 섹션 · 빈 번호 · 대문자 · HTML · 빈 값 무시 / 다른 쿼리 섞여도 · 쿼리 없음 → 쿼리 지움 0 · 해시 `#list` 유지, KST · 뉴욕 시간대 20/20 |
| 7 Scriptable 문법 | OK | 4절 |
| 8 크기 · 줄 수 · 링크(가짜 전역) | OK | 제작 테스트 5/5 + **심사 하네스**(5.3) 크기 8 × 항목 30 · 12 · 7 · 4 · 2 · 1 |
| 9 글자 · 색 · 하단 · 갱신 힌트 | OK | 제작 테스트 TZ 3개 통과 + 심사 하네스 12경우(KST 기대값) × 시간대 5개 |
| 10 실패 · 오프라인 캐시 | OK | 제작 테스트 + 심사 하네스 실패 19종 × 4크기 · 캐시 상태 4 × 실패 4 × 3크기 · 덮어쓰기 · 쓰기 실패 |
| 11 로컬 회귀 · 빌드 | OK | 142/142 · 빌드 · 타입 성공 · 새 패키지 0 |
| 12 로컬 서버 API · 정적 | OK | 26/26(5.2) |
| 13 로컬 화면(CDP) | OK | 57검사 전부 통과(5.4) — ①~⑧ 모두, 모든 화면 가로 넘침 0 |
| 14 안전 · 한도(로컬) | OK | **심사 3000 요청 70/100**(Node 9 + 브라우저 61, 막힘 0) · newsdata 0 · OpenAI 0 · DB 9테이블 건수 · md5 전후 같음(읽기 전용 트랜잭션) · 누출 false(파일 125개 검사) · 남은 프로세스 0(8절). 제작 FAIL 14(요청 누계 초과)는 제작 기록이며 이 줄은 심사 요청 수로 판정 |
| 15 배포 API · 캐시 | SKIP — 푸시 후 | 커밋 · 푸시 전(9절 절차) |
| 16 배포 바로가기(CDP) | SKIP — 푸시 후 | 9절 |
| 17 04:17 반영 | SKIP — 푸시 다음 날 05~09시 | 9절 |
| 18 iPhone 실제 | SKIP — iPhone 실기기 당분간 불가(D24, PASS 아님 · 10-17 표기 변경, 12절) | 기기 없음. 볼 것은 8~10번 하네스로 고정 |
| 19 Android 안내 칸 | OK | 7번 문구 PLAN과 같음(`더보기(⋮) › 설치 및 바로가기 만들기 › 바로가기 만들기` · "홈 화면 위젯은 준비 중입니다"), 360px에서 화면 안(left ≥ 0 · right ≤ 360), Android 코드 · 빌드 파일 0 |
| 20 상세창 수집 시각 | OK | ① VM: `04:31:12 → 10.17 오전 4:31` · `13:05 → 오후 1:05` · `00:07 → 오전 12:07` · `12:00:59 → 오후 12:00` · `10-09 23:59 → 10.9 오후 11:59`, D8 카드 `10.8`, 템플릿에 "오전 6:00" 없음 ② 브라우저: 상세창 "수집" `10.17 오전 8:34` = 날짜 줄 `10.17 오전 8:34 수집 · 이슈 30개`의 앞 = `/api/widget` updatedLabel − " 수집"(지난 날 `10.16 오전 8:41` = SQL 그날 실행) |
| 21 Web Analytics | SKIP — ①만 OK, ②③ 푸시 · Enable 뒤 | ① 로컬(Enable 없음): `/` · `/?issue=` 10탭 · `/widget` 모두 script.js 404(불러오기 실패만 기록) · JS 예외 0(페이지 시작 전 넣은 오류 수집기 12탭 전부, CDP 예외 이벤트는 `/` · `/widget` · 지난 날 · 5xx 3탭에서도 0) · `typeof window.va` = function · `document.cookie` 빈 값 · 브라우저 쿠키 0 · 13①⑦ · 19 그대로 |
| D25 ① 가로 넘침 · 좌표 | OK | 5.4 |
| D25 ② 실패 문구 | OK | 제작 VM + 심사 브라우저(5.4) |

### 5.1 합계
- 21줄: **OK 16(1~14 · 19 · 20) · FAIL 0 · SKIP 5(15 · 16 · 17 · 21 푸시 후, 18 iPhone)**. D25 추가: OK 2.
- PLAN 판정 규칙: 18번 하나만 남으면 "PASS 보류". 지금은 푸시 전이라 배포 줄도 SKIP → **"PASS 보류(푸시 후 배포 줄 확인 + iPhone 대기)"**. 배포 줄이 모두 OK가 되면 18번 하나만 남는 PASS 보류가 됨.

### 5.2 12번 — 서버 코드 없이 따로 계산한 순서와 비교(`judge-db.mjs order` → `judge-api.mjs`)
- SQL(읽기 전용): 최근 수집일 = `max(date) where date ≤ KST 오늘` → 2026-10-17, 섹션마다 그날 이하 가장 늦은 날짜 행(D8 — 오늘은 6섹션 모두 10-17), 각 섹션 D16(건수 ↓ → importance ↓ → latest_published_at ↓ → 내 자연 순서 비교식)으로 Top 5, 그 카드로 ① rank 1들 → ② 나머지 각각 D16.
- 로컬 `/api/rankings` 섹션별 카드 = SQL Top 5(순서까지), `/api/widget` 30개 id 순서 = SQL 순서 **완전 일치**, 앞 6개 = rank 1(`soc-3 · pol-1 · it-1 · wor-1 · eco-2 · cul-1`), rank 줄 `111111232423234452553453452345`(제작 보고와 같음). 인접 동점: 건수 같음 14쌍 → 중요도로 5쌍 · 발행 시각으로 9쌍이 갈림, 완전 동점(id로 갈림) 0쌍 — id 자연 순서는 실제 데이터로는 안 쓰여 단위 테스트(②)로만 확인됨.
- 헤더: widget 200 `public, max-age=0, s-maxage=300` · Set-Cookie 0 · JSON. 참고 값: 로컬 `/api/rankings` Cache-Control 없음, `/widget.js` · `/widget.html` · `/` `public, max-age=0`(배포 값은 15번).
- 정적: `/widget.js` 200 · 본문 바이트 = 파일 · javascript, `/widget.html` = 파일, `/widget` = widget.html, `/?issue={첫 id}` = `/` = index.html, Set-Cookie 0.

### 5.3 Scriptable — 심사 하네스(`judge-widget-vm.mjs`, 제작 테스트와 다른 경우 · 다른 구현)
- 가짜 `ListWidget · WidgetText · WidgetSpacer · WidgetStack · Color · Font · Request · FileManager · config · Script`를 docs.scriptable.app 이름표의 Proxy로 만들고, 그 밖의 이름을 읽거나 쓰면 위반으로 기록 + 던짐(`Device`도 쓰면 위반). 파일을 새 VM에서 async 함수 본문으로, 고정 시각 Date로 실행.
- **826검사 × 시간대 5개(UTC · Asia/Seoul · America/New_York · Pacific/Kiritimati(+14) · Ubuntu America/Los_Angeles) 모두 826/826.**
  - 크기 8(small · medium · large · extraLarge · accessory 3종 · 앱 null) × 항목 30 · 12 · 7 · 4 · 2 · 1: 제목 줄 = min(n, 3 · 5 · 10 · 10 · 3 · 3 · 3 · 5), 순서 · 글자 그대로, 텍스트 = 제목 + 하단뿐, 제목들 → 가변 spacer(null) → 하단, semibold 13 · 13 · 14 · lineLimit 2 · 1 · 1 · 간격 4 · 3 · 6, 하단 regular 10, 색 `#ffffff/#1a1a19` · `#161616/#ecebe8` · `#6f6b65/#9b978f`, 줄 url은 medium · large · extraLarge · 앱만 BASE + path(small · accessory 0), 위젯 url BASE `/#list`, 요청 1개(`BASE/api/widget` · GET · 머리글 0 · **loadString 부를 때 timeoutInterval 10**), 위젯 setWidget 1(같은 위젯) · 앱 presentMedium 1, complete 1번 · 맨 끝, 캐시는 library 폴더만 · iCloud 0 · 패딩 호출 0.
  - morePath `list` · 없음 · 숫자 · `https://…` → BASE `/`, `/#list` · `/?from=widget` → 그대로.
  - 갱신 힌트(KST): 08:00 오늘 → 11:00 · 21:59 오늘 → 다음 날 00:59 · 00:00 어제 → 04:30 · 04:05 어제 → 04:35 · 04:00 어제 → 04:30 · 16:45 어제 → 17:15 · 23:59 오늘 → 02:59 · 23:59 어제 → 00:29 · 10:00 이틀 전 → 10:30 · 06:20 실패(캐시 없음) → 06:50 · 06:20 실패(오늘 캐시) → 09:20 · 02:00 실패(어제 캐시) → 04:30.
  - 오프라인: 성공 → `libraryDirectory()/issuerank-widget.json` = 받은 본문 그대로. 이어서 실패 19종(던짐 · 시간 초과 · 500 · 502 · **503인데 모양 맞는 새 본문** · 404 · 304 · 200 HTML · 빈 본문 · null · items 빈/없음 · title 숫자 · path `https` · path 없음 · 항목 null · date 숫자 · updatedLabel 없음 · 둘째 항목만 틀림) × 4크기 → 캐시 제목 · 순서 · 링크 · 하단 그대로, 캐시 안 바뀜, 예외 0. 캐시 없음 · JSON 아님 · 모양 틀림 · 빈 문자열 × 실패 4 × 3크기 → "불러오지 못했습니다" 한 줄 · url BASE `/` · 줄 url 0 · 캐시 안 씀. 새 성공은 덮어씀, 캐시 쓰기 실패해도 그림.
- **하네스가 실제로 잡는지(대조군)**: 사본 11개에 일부러 실수 → 모두 잡힘: small 줄 url(43건) · timeout 줄 삭제(48) · cacheDirectory(51) · 모양 검사 전에 캐시 씀(49) · 갱신 2시간(4) · `Device.isUsingDarkAppearance`(631) · medium 4줄(63) · 머리말 추가(402) · 기기 시간대 기준 04:30(KST에선 0, 뉴욕에선 3 — 시간대 바꿔 돌려야 잡힘) · 상태 코드 검사 삭제(60) · complete 삭제(172). 원본 `widget.js` 해시 불변.

### 5.4 13번 · D25 — 브라우저(`judge-ui.mjs`, 57검사 57 OK)
- ① 주소로 바로 연 탭 `/?issue=2026-10-17-soc-3`(위젯 첫 항목): 상세창 제목 = API 제목, 3줄 요약 = API, `106건`, 원문 href = API url · `_blank` · `noopener`, `location.search` 빈 값 · 해시 없음 · **history.length 1**, 토스트 0. 새로고침 → 상세창 · 토스트 0 · 이슈맵 · rankings 요청에 date 없음 · 기록 1(쿼리가 지워져 평소 화면).
- ② `/?issue=2026-10-16-pol-2`(직전일 Top 5): 날짜 10-16 · "10월 16일 (금)의 주요 이슈" · "오늘" 버튼 보임 · 상세창 · 수집 `10.16 오전 8:41` · rankings 요청 `?date=2026-10-16` 1번. "오늘" 누르면 당일 · "오늘 가장 많이 보도된 이슈" · 버튼 숨김(AC-09-2).
- ③ `…-pol-99999` → `?date=2026-10-17` · 상세창 0 · 토스트 "이슈를 찾을 수 없습니다" 하나. ④ `abc` → date 없이 1번 · 같은 토스트 하나. 내일 id → 무시(date 없이) · 같은 토스트. `2000-01-01-pol-1` → 그 날짜 · 빈 화면 "해당 날짜의 랭킹 데이터가 없습니다" · 같은 토스트 하나(AC-09-3).
- ⑤ `/?issue={id}#list` → 목록(카드 30) + 상세창 · 해시 `#list` 그대로. ⑥ 상세창 북마크 실제 클릭(비로그인) → "북마크하려면 로그인이 필요합니다" + 로그인 버튼, `/api/bookmarks` · `/api/folders` 요청 0(프록시 기록 전체에서도 0). ⑦ `/` → 랭킹 탭(실제 클릭, `#list` · 기록 +1) → 뒤로(이슈맵) → 앞으로(목록), 상세창 계속 닫힘 · `/#bookmarks`(같은 문서 · 첫 로드 둘 다) → "로그인이 필요합니다".
- ⑧ `/widget`: 칸 = `widget.js` 3821자 그대로 · 실제 클릭 → "복사했습니다" · 클립보드 = 파일 · 쓰기 권한 거절 → 칸 전체 선택(0~3821, 포커스) + "길게 눌러 복사해 주세요" · `/widget.js` 404(브라우저 안에서 만듦) → "스크립트를 불러오지 못했습니다" · 버튼 꺼짐 · 밝게 `rgb(247,247,245)/rgb(22,22,22)` · 다크 배경 `#121211` · 글자 `#ecebe8` · 칸 `#1a1a19` · 버튼 `#ecebe8` · 360px 넘침 0 · 외부 리소스 0.
- **D25 ② 5xx · 네트워크(서버 요청 없이 CDP Fetch로 응답을 만듦)**: 바로가기 날짜 랭킹 500 → 토스트 "Internal Server Error" 하나 · 502 → "API 서버에 연결할 수 없습니다" 하나 · 네트워크 실패 → "Failed to fetch" 하나, 셋 다 "이슈를 찾을 수 없습니다" 0 · 상세창 0 · 그 날짜 · 쿼리 지움 · JS 예외 0. (참고, PLAN 밖 경계) 형식 틀림 + 최근 날짜 502 → 서버 오류 토스트만.
- **D25 ① 가로 넘침**: 360 · 375 · 390(모바일 에뮬레이션) · 768 · 1280(데스크톱, 세로 스크롤바 있음) × 애니메이션 켬/끔 × 6전환(map→list · list→bookmarks · bookmarks→map · map→bookmarks · bookmarks→list · list→map). 전환마다 클릭 직후(같은 작업) + rAF · 15ms 타이머 표본 + 정지 뒤 = 47~72표본(애니메이션 켬이면 0~380ms 사이 39~50개). 판정 `scrollWidth ≤ min(clientWidth, 기기 폭)` · `scrollWidth ≤ innerWidth` · 모바일 `innerWidth = 기기 폭` → **60전환 · 표본 약 4,000개 넘침 0**(최대 scrollWidth: 360 · 375 · 390 = 기기 폭, 768 = 753/768, 1280 = 1265/1280).
- **대조군**: 같은 표본기로 변경 전(HEAD) 판을 재면 360 map→list · list→bookmarks 61/61 · 70/70 표본 넘침(최대 378), 768 map→list 64/64(771) · list→bookmarks 34/70(786) — 표본기가 실제로 잡음. 변경 전 판은 서버 파일을 바꾸지 않고 CDP Fetch로 문서만 `git show HEAD:ex12/public/index.html`로 바꿔 줌(API는 같은 서버 · 같은 데이터).
- **좌표 · 배치 불변**: 768 → 1280 → 390 → 360 순서로 새 판과 HEAD 판을 같은 절차로 잼 — 섹션 블록 6 · 타일 30의 인라인 left · top · width · height · opacity · 클래스(xs/xxs) · 글자 크기 · 줄 수와 지도 높이(768 `764` · 1280 `640` · 390 `1716px` · 360 `1572px`)가 **네 폭 모두 HEAD와 같음**(xs 360 · 390 각 3, xxs 0). 768 · 1280 목록(섹션 탭 · 머리 6 · 카드 30 문서 좌표, 글꼴 로드 뒤) = HEAD. 900px 내린 뒤 섹션 탭 top 56 = 머리글 높이(sticky 그대로, 360 · 768 · 1280, HEAD와 같음).

### 5.5 마련한 대조 · 드라이런
- 실제 서버에 쓰기 전 같은 브라우저 스크립트를 가짜 서버(127.0.0.1:3999, 3000 요청 0)로 돌려 도구를 확인했고, 그 가짜 서버가 **D25 전 동작(`else toast`)으로 바꾼 index.html**을 내주면 D25② 4검사가 정확히 실패(토스트 `[오류, "이슈를 찾을 수 없습니다"]` 두 개를 잡음)함을 확인. 드라이런 결과 · 캡처는 증거로 남기지 않고 지움(가짜 데이터).

## 6. 지켜야 할 것 위반
- 기사 본문 저장 · 표시: 없음(위젯은 제목만, API에 요약 · url조차 없음, DB 스키마 변경 0).
- 코드 속 키 · 접속 정보: 없음(바뀐 파일 · 증거 파일 125개에서 `.env` 비밀값 8개 일치 0, 패턴 일치 1건은 기존 테스트의 가짜 문자열 `FAKE_SECRET_123@db.example.invalid` — 이번 diff 밖).
- `.env` git 제외: `git check-ignore` → `.gitignore:4:.env`.
- PLAN 밖 패키지 · 기능: 없음(package 파일 해시 불변). 작은 세부: `widget.html` 머리 한 줄 "휴대폰 버전에 따라 메뉴 이름이 조금 다를 수 있습니다."(PLAN 안내문 제목의 단서를 옮김) · 스크립트를 받기 전 복사 버튼 꺼둠 — 참고.
- 개인정보 수집: 없음(쿠키 0, 새 테이블 0, `window.va` 호출 0).
- AGENTS 규칙: 운영 폴더 ex12에서만 고침, 사용자 변경 보존, 커밋 · 푸시 없음 — 위반 없음.

## 7. 발견한 점(심각도 순, 모두 낮음 · 참고)
1. (낮음 · 문서) PLAN.md 10절 O1 메모가 D25 전 문장 그대로("테스트141" · "→ 수정 여부 사용자 결정"). 지금은 142 · D25로 결정됨 — 커밋 때 한 줄 고치면 좋음. 제작 보고 2절의 `index.html` 줄 번호(1108 · 827 · 1132)도 D25 뒤 5~6줄 밀림(13절은 맞음).
2. (참고 · 기존 동작) 바로가기 날짜 랭킹이 5xx면 토스트는 D25대로 서버 오류 문구지만, 화면은 기존 빈 화면 "해당 날짜의 랭킹 데이터가 없습니다"를 함께 보임(`loadRanking` 실패 공통 — 날짜 바꾸기에서도 같음, `o1j-360-shortcut-502.png`). PLAN 범위 밖, 필요하면 다음 단계에서 오류 전용 안내.
3. (참고) `overflow: clip`은 iOS 16 · Chrome 90 이상. iOS 15.x Safari는 이 줄을 무시해 화면 전환 중에만 예전처럼 잠깐 넘칠 수 있음(제작 보고와 같은 판단, 깨짐 없음).
4. (참고) 실제 데이터에는 완전 동점이 없어 id 자연 순서 규칙은 단위 테스트로만 확인됨.
5. (참고 · 기존) 메인 화면은 Pretendard 글꼴을 cdn.jsdelivr.net에서 받음(이번 측정의 유일한 외부 호스트, newsdata · OpenAI 아님). 휴대폰에서 토스트가 바텀시트 아래 버튼과 겹치는 것도 기존 동작.
6. (참고 · 측정 방식) 지시의 `next start -p 3000` 대신 `-p 3001` + 심사 프록시 3000으로 셈(5절 머리). 브라우저가 보는 주소 · 동작은 같음.

## 8. 요청 수 · 외부 · DB · 임시 계정 · 프로세스
- 로컬 3000(프록시가 셈, **시작 전 카운터 0**): **70/100** — Node 9(`/api/widget` · `/api/rankings` · 쿼리 붙인 widget · POST widget · `/widget.js` · `/widget.html` · `/widget` · `/?issue=` · `/`) + 브라우저 61(문서 16 · rankings 12 · get-session 15 · script.js 404 16 · `/widget.js` 1 · favicon 1). 막힘(503) 0 · 서버 5xx 0(5xx는 브라우저 안에서만 만듦) · 상태 200 43 · 304 8 · 404 19. 서버 로그 오류 0줄.
- newsdata 0 · OpenAI 0(심사 스크립트에 그 주소 없음, 로컬 서버 경로도 부르지 않음). 그 밖의 네트워크: 페이지 글꼴 jsdelivr, `npx @redocly/cli@2.64.1`(캐시에 있던 판).
- DB: 읽기 전용 트랜잭션(`BEGIN READ ONLY`)으로만, 9테이블 건수 · 내용 md5 전후 **같음**(articles 767 · issues 492 · collect_runs 84 · bookmarks 1 · folders 1 · user 1 · session 1 · account 1 · verification 0). **임시 계정 0개**(로그인이 필요한 줄 없음) → 정리할 행 0.
- 프로세스: Chromium은 띄우기 전 PID 목록과 비교해 새로 생긴 chromium PID(+ 내 프로필 경로를 가진 것)만 기록 — 본체 1838 · crashpad 2 · zygote/renderer/utility 9 = 12개, `Browser.close` 한 번에 모두 종료(강제 종료 0, 넓은 패턴 kill 0), 드라이런 2회도 같은 방식으로 남은 것 0. next-server(1471) · 프록시(1455) · proot(1408)는 정지 파일로 스스로 종료. 확인: `curl` 3000 · 3001 · 9350 · 3999 = 000, `ps`에서 next-server · next start · judge-server · lib/chromium · proot 0줄(Claude Code `.local/share/claude/versions`만 제외), **사용자 code-oss PID 17320 실행 중**. 브라우저 프로필 삭제 확인(`judge-o1-chrome-profile-*` 0), 임시 파일(스크래치의 사본 · 드라이런 · 로그) 삭제. 참고: code-oss의 crashpad 처리기는 제작 때 사고(제작 보고 10절) 뒤로 없음 — 심사는 손대지 않음.

## 9. 푸시 후 확인할 줄(심사 · 사용자)
배포 주소 `https://issuerank-kappa.vercel.app`, 배포 요청 역할마다 ≤ 40(브라우저 자동 요청 포함), newsdata · OpenAI 0, 임시 계정 0. 사용자가 커밋 · 푸시를 승인하고 Vercel 자동 재배포가 끝난 뒤:
1. **15**(약 7요청): Node fetch `/api/widget` 2번(10초 간격) → 둘 다 200, 둘째 `x-vercel-cache: HIT`(아니면 30초 뒤 1번 더, 그래도 아니면 FAIL — Next/Vercel이 라우트 Cache-Control을 바꾸는지 조사), 클라이언트 `cache-control`에 s-maxage 없음 · `max-age=0` 포함, Set-Cookie 0. `/api/rankings` 1번 → 같은 date · `public, max-age=0, must-revalidate`. `/widget.js`(본문 sha256 = 저장소 파일) · `/widget` · `/?issue={위젯 첫 id}` 각 200.
2. **16**(탭마다 약 4요청: 문서 · rankings · get-session · script.js): Chromium 360×800으로 배포 주소의 13① `/?issue={첫 id}`(주소로 바로 연 탭: 상세창 · 쿼리 지움 · 기록 1) · ④ `/?issue=abc`(토스트) · ⑦ `/` → 랭킹 → 뒤로 → 앞으로 · `/#bookmarks` · 20②(수집 = 날짜 줄 = updatedLabel − " 수집") → 로컬과 같음. `judge-ui.mjs`는 `JUDGE_ORIGIN`으로 주소를 바꿀 수 있으나 3000 프록시 카운터가 없으므로 배포용 요청 수는 CDP 네트워크 기록으로 셈.
3. **21②**(Analytics Enable 전에 푸시했다면, 16의 탭으로 함께): 배포 `/` · `/?issue={id}` · `/widget` → script.js 404여도 JS 예외 0 · `typeof window.va` = function · 쿠키 0.
4. **21③**(사용자: Vercel › 프로젝트 › Analytics › Enable → 최신 배포 Redeploy): 배포 `/` 1번 → `/_vercel/insights/script.js` 200 · `…/view` 요청 1회 · `document.cookie` 빈 값 · Set-Cookie 0, 사용자가 대시보드 방문 1 이상 전달. 대시보드가 다른 스크립트 경로를 안내하면 두 HTML의 `src`만 바꾸는 작은 수정이 필요(제품 변경 → 제작 역할).
5. **17**(푸시 다음 날 05:00~09:00 KST, 2요청): 배포 `/api/widget` · `/api/rankings` 1번씩 → 두 date = 그날, 순서 = 그날 rankings 기준 12번 계산법(`bash scripts/ex12-ubuntu.sh node .data/judge-o1-20261017/judge-db.mjs order`로 SQL 순서를 다시 만들어 비교, 읽기 전용).
6. **18**(iPhone이 생기면 사용자): 제작 보고 9절 4의 ①~⑧(줄 수 3 · 5 · 10 · 제목만 · 긴 제목 … · 하단 시각 · 줄 → Safari 그 이슈 상세 · 작은 위젯 → 목록 · 비행기 모드에서 마지막 내용 · 다크 · 다음 날 아침 하단 · iOS/Scriptable 버전) 결과를 받으면 18번만 다시 판정.

## 10. 증거 파일(`ex12/.data/judge-o1-20261017/`, git 무시 폴더)
- 스크립트(심사가 새로 작성): `jlib.mjs`(비밀값 검사 · D16 · 라벨) · `judge-db.mjs`(SQL 순서 · DB 스냅샷) · `judge-server.mjs`(next start 3001 + 세기 프록시 3000) · `judge-api.mjs`(12번) · `judge-ui.mjs`(13 · 19 · 20 · 21① · D25) · `judge-widget-vm.mjs`(Scriptable 하네스) · `judge-drawer-vm.mjs`(20① · 바로가기 형식) · `judge-openapi.cjs`(5번 구조) · `judge-rank-equiv.ts`(HEAD 비교). 비교 기준 사본: `index-head.html` · `head-ranking.ts`(`git show HEAD:`).
- 결과: `sql-order.json` · `api-result.json` · `ui-result.json` · `widget-vm-*.json`(5시간대) · `db-snapshot-before.json` · `db-snapshot-after.json` · `request-count.json`(70) · `request-log.json` · `server-state.json` · `server-log.txt`(leak:false).
- 캡처(360×800, 직접 열어 확인한 것 ✓): `o1j-360-issue-first.png` ✓(상세창 · 수집 `10.17 오전 8:34`) · `o1j-360-issue-prev.png` ✓(10월 16일 · 수집 `10.16 오전 8:41`) · `o1j-360-issue-missing.png` ✓(토스트) · `o1j-360-shortcut-502.png` ✓(서버 오류 토스트) · `o1j-360-list-after.png` ✓(전환 뒤 목록, 넘침 없음) · `o1j-360-widget-light.png` ✓ · `o1j-360-widget-dark.png` ✓(전체 선택 대체 경로) · `o1j-360-issue-first-bookmark.png` · `o1j-360-issue-bad.png` · `o1j-360-issue-list.png` · `o1j-360-bookmarks.png` · `o1j-360-bookmarks-after.png` · `o1j-360-widget-copy-fallback.png`.

## 11. 2026-10-17 16:35 KST — 배포 줄 판정(푸시 d249399 뒤)
근거: 코디네이터 전달(사용자 승인으로 `d249399` 푸시 · Vercel 재배포 끝). 심사 확인: 로컬 HEAD = `d249399`, `main...origin/main` 앞뒤 차이 없음, 커밋된 O1 파일 sha256 = 이 심사가 본 파일(1~10절)과 같음. 지금 할 수 있는 줄(15 · 16 · 21②)만 실행. 스크립트 `ex12/.data/judge-o1-20261017/judge-deploy.mjs`(새로 작성, 배포 요청을 직접 셈 · 한도 40 · 넘을 것 같으면 탭 전에 멈춤). 실행 전 같은 스크립트를 가짜 서버(127.0.0.1:3999, 배포 요청 0)로 드라이런해 도구를 확인(23/23, 결과는 지움). DB는 순서 비교용 SQL만 다시 읽음(`judge-db.mjs order`, 읽기 전용 — 1~10절 값과 같음). newsdata · OpenAI 0 · 임시 계정 0 · 제품 코드 · 커밋 · 푸시 0.

| # | 판정 | 결과(배포 `https://issuerank-kappa.vercel.app`) |
|---|---|---|
| 15 배포 API · 캐시 | **OK** | `/api/widget` ①(07:35:15Z) 200 `x-vercel-cache: HIT` `age: 296` → ②(10초 뒤) 200 `MISS` `age: 0`(300초 보관이 그 사이 끝남) → PLAN대로 30초 뒤 ③ 200 **`HIT` `age: 30`** — `s-maxage=300` CDN 보관이 설계대로 동작. 세 번 모두 클라이언트 `cache-control: public, max-age=0`(s-maxage 없음 · max-age=0) · Set-Cookie 0 · 같은 본문. 본문 5칸 · 항목 4칸, date `2026-10-17` = `/api/rankings` date = SQL 최근 수집일, **30개 순서 = SQL 독립 계산(12번 방법)과 일치**, updatedLabel `10.17 오전 8:34 수집`. `/api/rankings` 200 `public, max-age=0, must-revalidate` · Set-Cookie 0. `/widget.js` 200(본문 sha256 = 저장소 파일) · `/widget` 200(= widget.html) · `/?issue=2026-10-17-soc-3` 200(= D25 판 index.html) |
| 16 배포 바로가기(CDP 360×800) | **OK** | 13① 주소로 바로 연 탭: 상세창 제목 `부천 도장공장 화재로 8명 부상…` · 3줄 요약 · `106건` · 원문 새 탭 · noopener = 배포 rankings 카드, 쿼리 지움 · 해시 없음 · **history.length 1** · 토스트 0 · 넘침 0(측정 탭에서도 상세창 · 토스트 0). 13④ `/?issue=abc` → date 없이 1번 · 상세창 0 · 토스트 "이슈를 찾을 수 없습니다" 하나. 13⑦ `/` → 랭킹 탭 실제 클릭(`#list`, 기록 2→3) → 뒤로(이슈맵) → 앞으로(목록) · 상세창 계속 닫힘 · `/#bookmarks` → "로그인이 필요합니다" · 넘침 0. 20② 상세창 "수집" `10.17 오전 8:34` = 날짜 줄 `10.17 오전 8:34 수집 · 이슈 30개` 앞 = 배포 updatedLabel − " 수집". 19 그대로: `/widget` 칸 = 저장소 widget.js(3821자) · Android 7번 문구 · 360px 안 |
| 21 ② Enable 전 | **OK**(줄 21은 ③ 남아 SKIP 유지) | `/?issue={id}` · `/` · `/widget` 세 측정 탭 모두 `/_vercel/insights/script.js` **404**(아직 Enable 전), JS 예외 0(페이지 시작 전 수집기 + CDP 예외 이벤트), `typeof window.va` = function, `document.cookie` 빈 값, 브라우저 쿠키(배포 도메인) 0, view 요청 0 |
| 17 · 18 · 21③ | SKIP | 17 = 내일(10-18) 05~09시 KST, 21③ = 사용자 Analytics Enable · Redeploy 뒤, 18 = 사용자 iPhone |

- 배포 요청(심사 카운터, 코디네이터의 3요청은 밖): **32/40**(위로 어림한 값) — Node 7(widget 3 · rankings · widget.js · widget · /?issue=) + 브라우저 25(탭 5개마다 `max(CDP 기록, Resource Timing)` + favicon 1 어림 = 6 · 5 · 5 · 5 · 4, 바로 연 탭은 Resource Timing으로 셈). favicon 어림을 빼고 확인된 값은 Node 7 + 브라우저 20 = 27. 외부 호스트는 글꼴 jsdelivr뿐.
- 참고 ①: PLAN 위젯 규칙 표는 Vercel이 클라이언트에 `public, max-age=0, must-revalidate`를 준다고 적었지만 실제는 **`public, max-age=0`**(must-revalidate 없음). 15번 기대("s-maxage 없음 · max-age=0")는 충족, 동작 차이 없음(문서 표현만). ②: 문서 `/?issue=…`는 `MISS`(정적 `/widget.js` · `/widget`은 `HIT` `age 338`) — 쿼리가 붙은 rewrite 문서가 따로 보관되는지 한 번으로는 판단 못 함, 본문은 같아 기능 영향 없음.
- 정리: Chromium 새 PID 17개(시작 12 + 탭 렌더러 5)만 기록 → `Browser.close` 한 번에 모두 종료, 남은 PID 0, 프로필 삭제. 가짜 서버(드라이런)는 그 PID만 종료. `curl` 9351 · 3000 · 3001 · 3999 = 000, `ps` chromium 0, code-oss 17320 실행 중. 임시 파일 삭제, 결과 · 캡처 누출 검사 false(57파일).
- 증거: `deploy-result.json` · `deploy-request-count.json`(32) · 캡처 `deploy-360-issue-first.png` ✓(직접 열어 확인) · `deploy-360-issue-bad.png` ✓ · `deploy-360-bookmarks.png` · `deploy-360-widget.png`.

### 11.1 갱신된 합계 · 판정
- 21줄: **OK 18(1~16 · 19 · 20) · FAIL 0 · SKIP 3(17 내일 · 21 ③ Enable 뒤(①② OK) · 18 iPhone)**. D25 추가 OK 2.
- 판정: **PASS 보류 — 17(10-18 05~09시 KST) · 21③(사용자 Enable 뒤) 확인 + 18 iPhone 대기.** 17 · 21③이 OK가 되면 18번 하나만 남는 PASS 보류(D24).
- 남은 절차: 9절 4(21③) · 5(17) · 6(18) 그대로.

## 12. 2026-10-17 16:58 KST — 21③ 판정(Analytics Enable · Redeploy 뒤)
근거: 코디네이터 전달(사용자가 Vercel › Analytics › Enable과 Redeploy 실행). 이번 확인은 21③만. 스크립트 `ex12/.data/judge-o1-20261017/judge-analytics.mjs`(새로 작성, 가짜 서버 드라이런으로 도구 확인 뒤 1회 실행). 배포 `/`를 360×800 Chromium 측정 탭(about:blank에서 준비 뒤 이동)으로 **1번**만 엶 — `/widget`은 필요 없어 열지 않음. 실제 사용자 브라우저처럼 모바일 Chrome UA · `navigator.webdriver` false(`--disable-blink-features=AutomationControlled`)로 엶(헤드리스 표시로 분석 스크립트 동작이 달라지는 것을 피함). newsdata · OpenAI 0 · DB 0 · 임시 계정 0 · 제품 코드 · 커밋 · 푸시 0.

| 확인 | 결과 |
|---|---|
| script.js | 페이지가 부르는 `/_vercel/insights/script.js` **200**(Enable 전 404 → 이제 200, 경로는 지금 코드 그대로) |
| view 이벤트 | `POST /_vercel/insights/view` **1회 · 200**, 그 밖의 insights 요청 0 |
| 쿠키 | 모든 응답 Set-Cookie 0 · `document.cookie` 빈 값 · 브라우저 쿠키(배포 도메인) 0 |
| JS 예외 | 0(페이지 시작 전 수집기 + CDP 예외 이벤트) |
| 화면 | `window.va` 함수 · 이슈맵 · 날짜 줄 `10.17 오전 8:34 수집 · 이슈 30개` · 360px 넘침 0(`analytics-360-home.png` 직접 열어 확인) |
| 대시보드 방문 수 | **사용자 대시보드 확인 대기**(심사는 볼 수 없음). 이 확인 방문은 2026-10-17 16:57 KST, 모바일 Chrome UA 1페이지뷰 — 대시보드에서 구분 가능 |

- 판정(관찰한 것으로): **21 OK**(① 로컬 · ② Enable 전 배포 · ③ Enable 뒤 배포 모두 OK, 대시보드 방문 수는 사용자 확인 대기로 따로 둠).
- 배포 요청(이번 확인분 별도 카운터, 앞의 32와 따로): **7/10**(CDP로 본 6 — 문서 · script.js · rankings · get-session · view · favicon 404 — + favicon 1 위로 어림, favicon이 CDP에 보였으므로 실제는 6). `analytics-request-count.json`.
- 정리: Chromium 새 PID 14개만 기록 → `Browser.close` 한 번에 모두 종료, 남은 0, 프로필 삭제. 드라이런 가짜 서버는 그 PID만 종료. `curl` 9352 · 3999 · 3000 = 000, `ps` chromium 0, code-oss 17320 실행 중. 임시 파일 삭제, 증거 58파일 누출 검사 false.
- 증거: `analytics-result.json` · `analytics-request-count.json` · `analytics-360-home.png`.

### 12.1 18번 표기 · 갱신된 합계 · 판정
- 18번: 사용자 알림(iPhone 실기기 테스트 당분간 불가)에 따라 **[SKIP — iPhone 실기기 당분간 불가]**로 표기 변경(5절 표도 같게 고침). 확인할 내용은 8~10번 가짜 전역 하네스로 고정돼 있음.
- 21줄: **OK 19(1~16 · 19 · 20 · 21) · FAIL 0 · SKIP 2(17 · 18)**. D25 추가 OK 2.
- 판정(PLAN 규칙): 17번(10-18 05~09시 KST) 결과 전까지 **"PASS 보류"**. 17번이 OK면 **"18번만 남은 PASS 보류(운영 반영 완료)"**로 갱신. 17번 절차는 9절 5.

## 13. 2026-10-18 13:19 KST — 17번 판정(04:17 하루 실행 다음 날 반영)
근거: 코디네이터 전달 + 심사 확인. 지금은 PLAN 17번 창(푸시 다음 날 05:00~09:00 KST) **밖**(13시대)이라 창 밖에서 본 사실을 그대로 적고, 아침 상태는 기록으로 판단함. 배포 요청 2/6(이번 확인분), DB 읽기 전용 2회, newsdata · OpenAI 0, 서버 · 브라우저 띄우지 않음, ex12 소스 · O2 관련 줄 손대지 않음. 배포 코드 = origin/main `d249399`(그 뒤 로컬 커밋 `b0181a2`는 문서뿐 · 미푸시, ex12 변경 0).

**기록(읽기만)**
- GitHub(`gh run view` · `gh run list`, 읽기만): 10-18 예약 실행 `39070939260` schedule · success · headSha `d249399`, 시작 21:26:20Z(**06:26:20 KST**, 예정 04:17보다 129분 늦음), `npm run daily` 06:26:43~**06:30:22 KST**, 끝 06:30:26. 10-18의 실행은 이것 하나(수동 재실행 없음).
- DB: 10-18 `collect_runs` ok 6행 · 완료 시각 하나 **06:27:07 KST**(기사 170), failed 0. 10-18 이슈 · 요약은 그 뒤 group · summarize가 써서 늦어도 06:30:22에 끝남(issues에는 시각 칸이 없어 이 범위로만 앎). 그 뒤 10-18 데이터를 바꾼 실행 없음.

**창 밖 확인(13:19 KST, `judge-day17-deploy.mjs`)** — 6/6 OK
- 배포 `/api/widget` 200 · `/api/rankings` 200, **두 date = 2026-10-18**(= KST 오늘 = SQL 최근 수집일).
- `/api/widget` 30개 순서 = **그날 SQL 재계산(`judge-db.mjs order`, 12번 계산법) 완전 일치**(앞 6 = `pol-1(98) · soc-1(88) · wor-5(40) · it-2(10) · cul-1(3) · eco-1(2)`, 건수 동점 12쌍 → 중요도 6 · 발행 시각 6으로 갈림), rankings 섹션별 카드 = SQL Top 5.
- `updatedAt` = rankings = 그날 ok 수집 완료 `06:27:07+09:00`, `updatedLabel` **`10.18 오전 6:27 수집`**. widget 클라이언트 `public, max-age=0`(MISS · age 0) · Set-Cookie 0. 결과 `day17-result.json`(배포 비교), 수집 기록 수치는 `judge-day17.mjs` 출력(위 "기록"), 그날 SQL 순서는 `sql-order.json`(10-17 판은 `sql-order-1017-1631.json`으로 보존).

**아침 사용자가 그날 데이터를 받은 시각(기록 + CDN 상한으로 판단)**
- 10-18 랭킹이 생긴 때 = group이 10-18 이슈를 쓴 때(06:27:07 수집 완료 뒤 ~ 06:30:22 사이). 그 전에는 최근 수집일이 10-17이라 위젯 · 사이트 모두 **10-17 데이터**(하단 `10.17 오전 8:34 수집` — 전날임이 보임, AC-11-3).
- CDN 보관 상한 300초(15번에서 실제로 `age 296` → 만료 뒤 `MISS` → `HIT age 30` 확인) → **늦어도 06:35:22 KST부터 모든 사용자에게 10-18 데이터**. 출근길 7시 전 목표(D3)는 이날 충족.
- 따라서 창 05:00~09:00 안이라도 **05:00~약 06:30에 확인했다면 date = 10-17이라 기대 결과("두 date = 그날")와 다름**, 06:35 뒤 ~ 09:00에 확인했다면 OK였을 상태. 차이를 만든 것은 GitHub 예약 지연(129분, D3 · D5 운영 문제)이고 O1 코드가 아님 — O1 코드(위젯 API가 최근 수집일 · 그날 순서 · 그날 수집 시각을 내보냄)는 13:19 관찰로 확인됨.

**판단: 17 = [SKIP — PLAN 창(10-18 05~09시) 밖 확인]** (OK 아님 · FAIL 아님)
- PLAN 17번은 방법에 시간 창을 정했고 그 창에서의 한 번 확인이 기대 결과를 내야 함. 창 밖(13:19) 관찰은 기대 결과를 충족하지만 방법 문구를 충족하지 못함. 기록으로 본 창 안 상태도 확인 시각에 따라 결과가 갈리므로(05:00~06:30 = 기대와 다름) 창 밖 관찰을 OK로 바꾸는 것은 사후 기준 완화가 됨. 창 안에서 FAIL을 실제로 관찰한 것도 아니라 FAIL도 아님.
- "푸시 다음 날"(10-18) 창은 지나서 PLAN 문구 그대로는 더 할 수 없음. 남은 길은 사용자 결정: ① 다음 아침(10-19) 그날 실행이 끝난 뒤(`gh run list`로 확인) ~ 09:00 사이에 배포 `/api/widget` · `/api/rankings` 1번씩(배포 2요청, 이 절과 같은 비교)을 17번 대체 확인으로 인정 — 심사 권장(직접 관찰) ② 이 절의 기록 근거(06:35:22부터 그날 데이터)를 17번으로 인정. 어느 쪽이든 실행이 09:00 넘어 늦게 끝나면 창 안 확인은 기대와 달라짐(운영 문제로 따로 보고).

### 13.1 갱신된 합계 · O1 판정
- 21줄: **OK 19 · FAIL 0 · SKIP 2** — 17 [SKIP — PLAN 창 밖 확인(기대 결과는 10-18 13:19에 충족)] · 18 [SKIP — iPhone 실기기 당분간 불가]. D25 추가 OK 2.
- **O1 판정: PASS 보류**(운영 반영은 13:19 관찰로 확인). 17번이 위 ① 또는 ②로 OK가 되면 "18번만 남은 PASS 보류(운영 반영 완료)".
