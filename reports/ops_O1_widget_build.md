# ops O1 홈 화면 위젯 1단계 — 제작 보고 (2026-10-17)

> 역할: 제작(`agent_prompts/2_build_agent.md`), Claude Code 세션. 기준: `plan/PLAN_ops_O1_widget.md`(확정) · SPEC US-11 · AC-11-1~4 · PLAN.md D3 · D8 · D10 · D16 · D20 · D21 · D22 · D23 · D24 · `plan/PRODUCT_PLAN.md` 4절.
> 운영 폴더 `ex12/`에서만 고침(D21, 복사 없음). 커밋 · 푸시 · Vercel/GitHub 설정 변경 · Actions 실행 없음 → **운영(배포 · 04:17 하루 실행)에는 아직 반영되지 않음**.
> 결과(D25 반영 뒤 갱신, 13절): 검증 21줄 **OK 15 · FAIL 1 · SKIP 5** — 13번은 D25로 고쳐 OK, 14번은 처음 실행의 요청 누계 초과 기록을 그대로 둠(D25 재검증은 53/60 한도 안). 처음 결과: 검증 21줄 **OK 14 · FAIL 2 · SKIP 5**. FAIL 2줄은 13(목록 · 북마크 화면 360px 가로 넘침 — **O1 전부터 있던 동작**, 변경 전 코드에서도 같음)과 14(로컬 3000 요청 **누계 약 142/100 초과** — 검증 방식 보완으로 3회 실행). 그래서 "PASS 보류"가 아니라 이 두 줄에 대한 사용자 결정이 먼저다(10절).

## 1. 작업 범위
- 서버: 공개 `GET /api/widget`(순서 · 링크를 서버가 정함, 200만 `Cache-Control: public, max-age=0, s-maxage=300`) + `openapi.yaml`(0.2.0).
- 사이트: `/?issue=<id>` 바로가기(첫 로드 때 그 이슈 상세창) · 상세창 "수집" 칸 = 실제 수집 완료 시각 · Vercel Web Analytics 스크립트 2줄.
- iPhone: Scriptable 스크립트 `public/widget.js`(배포 `/widget.js`) + 설치 안내 `public/widget.html`(`/widget`, Android 바로가기 칸 포함).
- 새 패키지 0(package.json · lock 해시 불변). 하루 실행 코드 · 워크플로 변경 0(해시 불변). DB 쓰기 0 · newsdata 0 · OpenAI 0 · 임시 계정 0.

## 2. PLAN 항목별 반영 위치
| PLAN 항목 | 반영 위치 |
|---|---|
| D16 비교식 공유(`rankIssues` · 요약 대상 결과 불변) | `ex12/server/ranking.ts:24` `compareIssues`(기존 정렬식을 그대로 옮김), `:32` `rankIssues`가 `sort(compareIssues)`. `summary.ts`는 그대로(해시 불변)라 요약 대상도 같은 함수 |
| 위젯 목록 순수 함수 · `updatedLabel` | `ranking.ts:60-61` `WidgetItem` · `Widget` 타입, `:64` `updatedLabel`(사이트 `fmtUpdated` + " 수집"과 같은 꼴), `:73` `buildWidget`(= `buildRanking` 카드 전부 → ① rank 1들 ② 나머지, 각각 `compareIssues`, 항목 칸 4개 · `path=/?issue={id}` · `morePath=/#list`) |
| Store에 위젯 읽기 하나 + 라우트 연결 | `ex12/server/app.ts:17` `widget(date)`, `:42` `app.route('/api/widget', widgetRoute(store))`(`/api/rankings`처럼, 세션 미들웨어보다 앞) |
| 새 라우트 | `ex12/server/routes/widget.ts`(새, 16줄): 날짜 = `latestDate()`(쿼리 무시), 없으면 404 `{ message: '해당 날짜의 랭킹 데이터가 없습니다' }`, 200에만 캐시 헤더(`c.json(..., 200, headers)` — 404 · 500(onError) · 502(BFF)에는 안 붙음) |
| 메모리 저장소 | `ex12/server/sample.ts:150` `widget: date => buildWidget(date, rowsOn(date))` |
| DB 저장소(행 읽기 공유, `ranking` 결과 불변) | `ex12/server/db/store.ts:26` `rankingInput`(그날 행 · D8 대체 행 · 그날 ok 실행 — 기존 `ranking` 본문을 그대로 옮김), `ranking`은 `buildRanking(…input)`, `:51` `widget`은 `buildWidget(…input)` |
| BFF · `app/api/[...path]/route.ts` 그대로 | 변경 0(해시 불변). 공개 경로라 no-store 안 붙음(테스트 4) |
| `openapi.yaml` | `:4` version 0.2.0(설명에 위젯 추가), `:246` `/api/widget`(200 · 404, 200 `Cache-Control` 헤더 문서화), `:635` `Widget`(필수 5칸, items 1~30), `:662` `WidgetItem`(id = IssueId, section = SectionKey, path 패턴 `^/\?issue=`). 다른 경로 변경 0 |
| 바로가기 `/?issue=<id>` | `ex12/public/index.html:1108` `issueShortcut()`(값을 읽은 즉시 `history.replaceState(null, '', pathname + hash)` → 형식 `{YYYY-MM-DD}-{섹션 키}-{숫자}` · 달력 날짜 · KST 오늘 이하 검사), `:1117` `start()`(기존 익명 초기화 함수를 이름 있는 함수로: 형식이 맞으면 `loadRanking(그 날짜)`, 로그인 확인 · `go` 뒤 그 랭킹에 id가 있으면 `openDrawer`, 아니면 토스트 "이슈를 찾을 수 없습니다"), `:1130` `start();` |
| 상세창 "수집" 칸 | `index.html:827` 카드 date = 보고 있는 랭킹 date면 `fmtUpdated(ranking.updatedAt)`, 아니면(D8 대체 카드) `fmtShort(it.date)`. 고정 "오전 6:00" 삭제 |
| Web Analytics 2줄 | `index.html:1132-1133`, `widget.html` 끝 2줄(`</body>` 바로 앞, 공식 HTML 방식). 앱 코드는 `window.va`를 부르지 않음 |
| Scriptable 스크립트 | `ex12/public/widget.js`(새, 106줄): 맨 위 버전 주석(v1) · `const BASE = 'https://issuerank-kappa.vercel.app'` · 모듈 문법 0 · 최상위 await · 문서 API만. 요청 1개(`timeoutInterval` 10). 크기별 small 3줄/13pt/2줄 · medium 5/13/1 · large · extraLarge 10/14/1(제목 semibold), accessory* = small 규칙, 앱(`null`) = medium 미리보기. 줄 간격 `widget.spacing` 4 · 3 · 6, 패딩 기본값. 줄 url은 medium · large만, 위젯 url = BASE + morePath(`/`로 시작 안 하면 BASE + `/`). 하단 = `updatedLabel`(regular 10, 가변 spacer 뒤). 색 `Color.dynamic`(표의 hex). 갱신 힌트(KST = UTC+9 계산). 성공 때만 `libraryDirectory()/issuerank-widget.json`에 받은 JSON 저장, 실패면 그 캐시, 없거나 깨지면 "불러오지 못했습니다" 한 줄 + url BASE + `/`. 위젯이면 `Script.setWidget` 1번 · 앱이면 `presentMedium` · 끝에 `Script.complete()` |
| 설치 안내 | `ex12/public/widget.html`(새, 99줄): 안내문 iPhone 1~6 · Android 7(PLAN 문구 그대로, 굵게 표시만) + 머리 한 줄 "휴대폰 버전에 따라 메뉴 이름이 조금 다를 수 있습니다."(PLAN 안내문 제목의 단서) + `/widget.js`를 읽어 보여 주는 읽기 전용 칸(실패면 "스크립트를 불러오지 못했습니다", 그때 버튼은 비활성) + "스크립트 복사"(누른 순간 `navigator.clipboard.writeText`, 되면 "복사했습니다", 안 되면 칸 전체 선택 + "길게 눌러 복사해 주세요"). 외부 파일 0(시스템 글꼴) · 다크 토큰 · 360px 가로 넘침 0 |
| rewrite | `ex12/next.config.mjs:4` `{ source: '/widget', destination: '/widget.html' }` 1개 추가 |
| 테스트 | `test/ranking.test.ts`(+7) · `test/api.test.ts`(+2) · `test/auth-proxy.test.ts`(+1) · 새 `test/widget-ui.test.ts`(7) · 새 `test/widget-script.test.ts`(5) — 5절 |
| 검증 스크립트(git 무시 폴더) | `ex12/.data/verify-o1-widget.mjs`(로컬 서버 · Chromium 검증), 결과 `ex12/.data/o1-widget-20261017/` |

## 3. 지운 것
- `index.html` 상세창의 고정 문구 `${fmtShort(it.date)} 오전 6:00`(PLAN이 지우라고 한 것). 그 밖에 지운 코드 없음(익명 초기화 함수는 같은 내용으로 이름 있는 `start()`가 됨, `dbStore.ranking` 본문은 `rankingInput`으로 옮김).

## 4. 필요했지만 없던 키 · 계정
- 로컬 작업에 없던 키는 없음(`ex12/.env`의 DB 연결은 읽기만, 값 출력 0).
- 아직 할 수 없는 것: 배포 주소 확인(푸시 전), Vercel Web Analytics Enable(사용자 Vercel 계정), iPhone 실기기(없음, D24).

## 5. 테스트
명령(저장소 루트): `bash scripts/ex12-ubuntu.sh npm test` → **16파일 141/141 통과**(D20 기준 119 + 새 22). vitest 뒤 추적 파일 변경 없음.

| 파일 | 새로 더한 것 |
|---|---|
| `test/ranking.test.ts` (+7, 계 14) | 비교식 공유 뒤 `rankIssues` 불변(바꾸기 전 정렬식을 그대로 옮긴 기준과 동점 많은 무작위 300묶음 비교) · 위젯 ① 6섹션×5(30개 · 앞 6 = 섹션 1위 건수↓ · id 집합 = 랭킹 카드 · 칸 4개 · path · morePath) ② 동점(건수 같으면 importance → 시각(UTC · +09:00 섞음) → id `-9` < `-10`, 1위끼리 · 나머지끼리) ③ pol 2 · eco 5 → 1위 2개 → 나머지 5개 ④ 그날 행 없음 null ⑤ D8(1위 묶음에 전날 id 5개 · path 날짜도 전날) ⑥ 입력 순서 뒤집기(①②⑤에서) ⑦ updatedLabel 04:31 · 00:07 · 12:00 · 13:05 + runs로 계산한 updatedAt |
| `test/api.test.ts` (+2, 계 16) | 세션 없이 200 · 칸 5 · 항목 칸 4 · 30개 = 랭킹 카드 · 앞 6 = rank 1 · 묶음 안 건수↓ · path · morePath · `10.7 오전 6:00 수집` · `Cache-Control` · Set-Cookie 없음 · 쿼리 무시 / 최근 수집일 없음 · 데이터 없는 날짜 → 404 같은 문구 · 캐시 헤더 없음 / POST 404 |
| `test/auth-proxy.test.ts` (+1, 계 7) | BFF가 `/api/widget`의 `Cache-Control`을 그대로 둠(no-store 안 붙음) · Set-Cookie 0 · 외부 fetch 0 |
| `test/widget-ui.test.ts` (새 7) | `index.html`의 실제 함수를 잘라 VM(가짜 fetch · history · location, 고정 시각 Date)에서 `start()` 실행: ① 그날 카드 → 호출 순서 `replaceState /` → `fetch ?date=그날` → 로그인 확인 → `go` → `openDrawer(id)` · 토스트 0 ② 지난 날 + `#list` → 해시 그대로 ③ `abc` · `2026-02-30` · 내일 · 없는 섹션 · `constructor` · 빈 값 · HTML 값 → 날짜 없는 랭킹 1회 · 상세창 0 · 토스트 하나 ④ 404 ⑤ 랭킹에 없는 id ⑥ 쿼리 없음 · `#list` → 지금과 같음 / 상세창 "수집" 칸 `10.17 오전 4:31` · `오후 1:05` · `오전 12:07` · 대체 카드 `10.16` · = 위젯 `updatedLabel` − " 수집" · 템플릿에 "오전 6:00" 없음 |
| `test/widget-script.test.ts` (새 5) | `widget.js`를 새 VM 컨텍스트(Node 전역 없음)에서 async 함수 본문으로 실행. 가짜 Scriptable 10종은 docs.scriptable.app(2026-10-17 확인)의 속성 · 메서드만 가짐 — 문서에 없는 이름 읽기 · 쓰기, 가짜에 없는 메서드 호출은 위반으로 기록(스크립트의 try가 삼켜도 실패). 8크기 × 항목 30·10·5·3·1(줄 수 · 순서 · 글자 · 링크 · 요청 1개 · setWidget/미리보기/complete) · morePath 이상 · 글꼴 · 줄 제한 · 줄 간격 · 색 · 하단 · 갱신 힌트 8경우 · 실패 ①~⑧(캐시 저장 위치 · 덮지 않음 · 안내 한 줄) |

추가 확인:
- 테스트가 실제로 잡는지: `widget.js`에 일부러 실수 7가지(문서에 없는 `fontSize` · 캐시 폴더 · `Device` · small 줄 url · 시간 제한 60 · 갱신 2시간 · try 안의 없는 메서드) → 모두 실패로 잡힘. `index.html` 실수 5가지(해시까지 지움 · 미래 허용 · 그리기 전에 엶 · "오전 6:00" · 날짜 생략) → 모두 잡힘. 원본은 해시로 복원 확인.
- 9번 두 시간대: `bash scripts/ex12-ubuntu.sh env TZ=UTC npx vitest run test/widget-script.test.ts` · `TZ=Asia/Seoul` → 둘 다 5/5(테스트 이름에 TZ가 찍혀 실제로 바뀐 것 확인).

## 6. build 결과
`bash scripts/ex12-ubuntu.sh npm run build` → **성공(종료 0)**: Next.js 16.4.0 Turbopack "Compiled successfully" · `tsc --noEmit` 통과(새 테스트 파일 포함). 라우트 `ƒ /api/[...path]` 그대로. 마지막 빌드는 최종 소스 기준(아래 10절의 이름 바꾸기 되돌림 뒤 다시 실행).

## 7. 검증 21줄
환경: Termux + proot Ubuntu(Node 24.18.0), 로컬 서버 `next start -p 3001 --hostname 127.0.0.1` + 요청을 세는 얇은 프록시 `127.0.0.1:3000`(→ 3001, 헤더 · 본문 그대로), 실제 Chromium 155.0.8059.39 headless + CDP. 데이터 날짜: SQL 최근 수집일 **2026-10-17** · 직전 **2026-10-16**. 안전 실행: `ex12/.env`만, 결과는 비밀값 검사(`leak:false`) 뒤에만 출력 · 저장.

| # | 판정 | 근거 |
|---|---|---|
| 1 변경 범위 · 하루 실행 불변 | OK | 바뀐 파일 = `ex12/{next.config.mjs, openapi.yaml, public/index.html, server/app.ts, server/db/store.ts, server/ranking.ts, server/sample.ts, test/api.test.ts, test/auth-proxy.test.ts, test/ranking.test.ts}`, 새 파일 = `ex12/{public/widget.js, public/widget.html, server/routes/widget.ts, test/widget-ui.test.ts, test/widget-script.test.ts}`(구현 순서 2~4 목록뿐, `git diff --stat -- ex12` 10 files +343 −29). 작업 전후 `sha256sum` 15개(auth · ai · bff · daily · steps · collect · group · grouping · newsdata · summary · summarize · route.ts · package.json · lock · daily.yml) **모두 같음**. ex01~ex11 · frontend · web · SPEC · `.github` · PLAN_web 변경 0. PLAN.md는 사용자 기존 변경(D23 · D24 행) + 지시받은 10절 메모 1줄만 |
| 2 순서 규칙(단위) | OK | `test/ranking.test.ts` 새 7개 통과(5절), 기존 rankIssues · buildRanking · findCard 7개 그대로 통과 |
| 3 위젯 API 계약 | OK | `test/api.test.ts` 새 2개 통과(5절), 기존 rankings 테스트 그대로 |
| 4 BFF 공개 경로 | OK | `test/auth-proxy.test.ts` 새 1개: 헤더 그대로 · no-store 0 · 외부 fetch 0 |
| 5 OpenAPI | OK | `npx --yes @redocly/cli@2.64.1 lint openapi.yaml`: **오류 0 · 경고 18**(변경 전 같은 버전으로 잰 기준도 경고 18 — 같은 규칙 18개, 새 경로가 더한 경고 0). diff = info 2줄 + `/api/widget` · `Widget` · `WidgetItem` 추가뿐, 다른 경로 차이 0 |
| 6 바로가기(VM) | OK | `test/widget-ui.test.ts` ①~⑥ 통과(5절) + 실수 넣기 5가지 모두 잡힘 |
| 7 Scriptable 문법 | OK | `node --check public/widget.js` Termux Node 24.18.0 · Ubuntu Node 24.18.0 둘 다 종료 0, import/export/require 0, 최상위 await 2곳. 일부러 틀린 최상위 await 파일은 오류로 잡힘(검사가 실제로 ESM으로 읽음) |
| 8 크기 · 줄 수 · 링크 | OK | `test/widget-script.test.ts`: 제목 줄 = min(n, 3·5·10·10·3·3·3·5), 순서 · 글자 그대로, 텍스트 = 제목 + 하단뿐, medium · large · extraLarge · null 줄 url = BASE + path(정확히), small · accessory* 줄 url 0 · 스택 0, 위젯 url = BASE + `/#list`, morePath 이상 → BASE + `/`, Request 1개(`…/api/widget` · 쿼리 0 · 10초), 위젯 setWidget 1 · 앱 presentMedium 1 · complete 1(마지막) |
| 9 글자 · 색 · 하단 · 갱신 힌트 | OK | semibold 13·13·14 · lineLimit 2·1·1 · spacing 4·3·6 · 하단 regular 10 = updatedLabel · 배경 #ffffff/#1a1a19 · 제목 #161616/#ecebe8 · 하단 #6f6b65/#9b978f. refreshAfterDate 8경우 = 15:00 · 다음 날 02:00 · 12:30 · 04:30 · 04:30 · 04:50 · 05:40 · 08:10, **TZ=UTC · Asia/Seoul 둘 다 5/5** |
| 10 실패 · 오프라인 캐시 | OK | ① 성공 → `libraryDirectory()/issuerank-widget.json` = 받은 JSON, cache · documents 폴더 안 씀 ②~⑥(던짐 · 시간 초과 · 500 · 404 · 200 형식 틀림 4가지) 캐시의 제목 · 순서 · 링크 · 하단 그대로 · 캐시 파일 안 바뀜 ⑦ ⑧(캐시 없음 · JSON 아님 · 모양 틀림) "불러오지 못했습니다" 한 줄 · url BASE + `/` · 4크기 · 예외 0 · setWidget/미리보기 1 · complete 1 |
| 11 로컬 회귀 · 빌드 | OK | 141/141 · 빌드 · 타입 성공 · 새 패키지 0(5 · 6절) |
| 12 로컬 서버 API · 정적 | OK | `/api/widget` 200 · `Cache-Control: public, max-age=0, s-maxage=300` · Set-Cookie 없음 · date 2026-10-17 = rankings date = SQL 최근 수집일 · updatedAt `2026-10-17T08:34:27+09:00` = rankings = SQL 그날 ok `collect_runs` 최대 finished_at(KST) · `10.17 오전 8:34 수집`(따로 계산과 같음) · 칸 5/4 · 30개 · id 집합 · 제목 · 섹션 = rankings · 앞 6개 = rank 1(순서 rank `111111232423234452553453452345`) · **순서 = rankings articleCount + SQL importance · latest_published_at으로 서버 코드 없이 따로 계산한 D16과 같음**(나머지 묶음 건수 동점 14개 포함) · 쿼리 붙여도 같은 본문. `/widget.js` 본문 = 파일(`application/javascript`) · `/widget.html` = 파일 · `/widget` = widget.html 본문 · `/?issue={id}` 본문 = `/` 본문 = index.html. 참고: 로컬 `/api/rankings`는 Cache-Control 없음(배포의 `must-revalidate`는 Vercel 기본값 → 15번) |
| 13 로컬 화면(CDP) | **FAIL**(하위 1개, O1 전부터 있던 동작) → **D25로 고친 뒤 OK**(13절) | 360×800 새 탭마다. **OK인 것**: ① `/?issue=2026-10-17-soc-3`(위젯 첫 항목) → 상세창 제목 = API 제목 · 3줄 요약 = API · 연관기사 `106건` · 원문 `target=_blank` `rel=noopener` · `location.search` 빈 값 · 주소로 바로 연 새 탭 `history.length` 1(about:blank에서 이동한 측정 탭은 2 = 같은 방식의 `/`와 같음) ② `/?issue=2026-10-16-pol-2` → 날짜 10-16 · "10월 16일 (금)의 주요 이슈" · "오늘" 버튼 · 상세창 ③ `…-pol-99999` → 그날 랭킹 · 상세창 없음 · 토스트 "이슈를 찾을 수 없습니다" ④ `abc` → 최근 날짜 · 같은 토스트 ⑤ `/?issue={id}#list` → 랭킹 목록(30) + 상세창 · 해시 `#list`(바로 연 탭 기록 1) ⑥ 비로그인 북마크 → "북마크하려면 로그인이 필요합니다" · `/api/bookmarks` · `/api/folders` 요청 0(프록시 전체 기록) ⑦ `/` → 랭킹 탭(`#list`, 기록 +1) → 뒤로(이슈맵) → 앞으로(목록), 상세창 계속 닫힘 · `/#bookmarks` → "로그인이 필요합니다" ⑧ `/widget` 칸 = `/widget.js`(3821자 같음) · 실제 마우스 클릭 복사 → "복사했습니다" · 클립보드 = 파일(CDP로 권한 · 포커스 부여) · 쓰기 거절 대체 경로 → 칸 전체 선택(0~3821) · "길게 눌러 복사해 주세요" · 다크 배경 rgb(18,18,17) · 글자 rgb(236,235,232) · 외부 리소스 0. **FAIL인 것**: "모든 화면 가로 스크롤 0" — 이슈맵 · 위젯 안내 화면은 레이아웃 폭 360이지만 **목록(⑤ · ⑦) · 북마크(⑦) 화면은 363~378px**(보이는 폭 360, 18px 넘침). 측정 스크립트의 판정식(`scrollWidth ≤ innerWidth`)은 모바일 에뮬레이션에서 innerWidth가 함께 늘어 이를 못 잡았고, 결과 값을 다시 보고 찾음. 원인 확인(로컬 서버 요청 0, file://): 같은 화면을 애니메이션 켬 → 378, `prefers-reduced-motion` → 360. **git HEAD(O1 전) index.html로도 값이 모두 같음** → 기존 화면 전환 애니메이션 관련 동작, O1 변경 탓 아님(`ex12/.data/o1-widget-20261017/overflow-360.json`). 범위 밖이라 고치지 않음(11절 제안) |
| 14 안전 · 한도(로컬) | **FAIL**(요청 한도, 기록 유지) — D25 재검증은 53/60 한도 안(13.3) | 마지막 실행: 3000 요청 **55**(Node 9 + 브라우저 46, 프록시가 문서 · 자동 요청까지 셈) · newsdata 0 · OpenAI 0 · DB 9테이블 건수 · md5 전후 **같음**(articles 767 · issues 492 · collect_runs 84 · bookmarks 1 · folders 1 · user 1 · session 1 · account 1 · verification 0, 쓰기 0) · 누출 false(결과 · 캡처 · 새 파일 13곳 검사) · 소유 서버 · 프록시 · Chromium 종료(`curl` 3000 · 3001 · 9339 · 9340 = 000, `ps` 대상 0). **그러나 로컬 실행을 3번 해서 누계 약 142/100(초과)**: 1회차(주소로 바로 연 탭 → CDP가 일부 요청을 못 봄, 정확한 수 없음 — 같은 흐름 2회차 43 + favicon 1로 추정 44) · 2회차 43 · 3회차 55. 모두 읽기 전용 · 외부 유료 호출 0(10절) |
| 15 배포 API · 캐시 | SKIP — 푸시 후 | 아직 커밋 · 푸시 전(8절 절차) |
| 16 배포 바로가기(CDP) | SKIP — 푸시 후 | 8절 절차 |
| 17 04:17 반영 | SKIP — 푸시 다음 날 | 8절 절차 |
| 18 iPhone 실제 | SKIP — 사용자 iPhone 확인 대기(D24, PASS 아님) | 기기 없음. 볼 것은 8~10번 가짜 전역 테스트로 미리 고정. 9절 확인 방법 |
| 19 Android 안내 칸 | OK | `widget.html` 7번 문구가 PLAN과 글자까지 같음(더보기(⋮) › 설치 및 바로가기 만들기 › 바로가기 만들기 · "홈 화면 위젯은 준비 중입니다"), 360px에서 칸 안에 보임(`o1-360-widget-light.png`), Android 코드 · 빌드 파일 0 |
| 20 상세창 수집 시각 | OK | ① 단위 테스트(5절) ② 13번 ① 상세창 "수집" = `10.17 오전 8:34` = 날짜 줄 `10.17 오전 8:34 수집 · 이슈 30개`의 앞 시각 = `/api/widget` updatedLabel − " 수집". ② 지난 날 상세창은 `10.16 오전 8:41` |
| 21 Web Analytics | SKIP — ①만 OK, ②③ 푸시 · Enable 뒤 | ① 로컬(Enable 없음): 측정 탭 9개 모두 `/_vercel/insights/script.js` 404 한 번 · 페이지 JS 예외 0(CDP 이벤트 0 + 페이지 시작 전 넣은 오류 수집기 0) · `typeof window.va` = function · `document.cookie` 빈 값 · 쿠키 0 · 13번 ①⑦ · 19번 결과 그대로. ②③ 8절 |

## 8. 배포 · iPhone 줄 실행 절차(심사 · 사용자용)
배포 주소 `https://issuerank-kappa.vercel.app`, 요청 한도 역할마다 ≤ 40(브라우저 자동 요청 포함), newsdata · OpenAI 0, 임시 계정 0.
- **15**(푸시 · 자동 재배포 끝난 뒤): Node fetch `/api/widget` 2번(10초 간격) → 둘 다 200, 둘째 `x-vercel-cache: HIT`(아니면 30초 뒤 1번 더, 그래도 아니면 FAIL로 원인 조사 — 예: Next가 라우트 Cache-Control을 바꾸는지), 클라이언트 `cache-control`에 s-maxage 없음 · `max-age=0` 포함. `/api/rankings` 1번 → date 같음 · `cache-control: public, max-age=0, must-revalidate`(지금처럼). `/widget.js` · `/widget` · `/?issue={위젯 첫 id}` 각 200. 예상 6~7요청.
- **16**: Chromium 360×800으로 배포 주소의 13번 ①(`/?issue={첫 id}` → 상세창 · 쿼리 지움) · ④(`/?issue=abc` → 토스트) · ⑦(`/` → 랭킹 탭 → 뒤로 → 앞으로 · `/#bookmarks`) · 20번 ②(수집 칸 = 날짜 줄 = updatedLabel) → 로컬과 같음. 탭마다 약 4~5요청(문서 · rankings · get-session · script.js).
- **17**(푸시 다음 날 05:00~09:00 KST): `/api/widget` · `/api/rankings` 1번씩 → 두 date = 그날, 순서가 그날 rankings 기준(12번 계산법: rank 1들 → 나머지, 각각 건수 ↓ → importance ↓ → 최신 발행 ↓ → id 자연 순서, importance · 발행 시각은 DB 읽기).
- **21 ②**(Enable 전에 푸시했다면): 배포 `/` · `/?issue={id}` · `/widget` → script.js 404여도 JS 예외 0 · `typeof window.va` = function · 쿠키 0. **21 ③**(사용자 Enable · 재배포 뒤): 배포 `/` 1번 → `/_vercel/insights/script.js` 200 · `…/view` 요청 1회(경로는 스크립트가 정함) · `document.cookie` 빈 값 · Set-Cookie 0 · (사용자 전달) 대시보드 방문 1 이상. 대시보드가 다른 고유 경로(`/<unique-path>/script.js`)를 안내하면 그 경로로 두 파일의 `src`만 바꿈.
- **18**(iPhone이 생기면, 사용자): 9절 ③.

## 9. 사용자 할 일
1. (결정됨 2026-10-17 → D25로 고침, 13절) **먼저 결정**: 13번 FAIL(목록 · 북마크 화면 360px에서 18px 가로 넘침, O1 전부터 있음)을 ㉮ O1과 별개 기존 문제로 기록만 하고 진행할지, ㉯ 작은 별도 수정(11절 제안, 새 결정 번호)으로 고칠지. 14번 FAIL(제작 검증의 로컬 요청 누계 초과)은 제품 문제가 아니라 검증 절차 문제 — 심사는 자기 한도 100 안에서 다시 잼.
2. **커밋 · 푸시 승인**: 승인하면 커밋 · `main` 푸시 → Vercel 자동 재배포. 다음 04:17 KST 하루 실행도 같은 ex12 코드로 돎(하루 실행 파일 해시 불변. `ranking.ts`는 비교식을 함수로 뺐을 뿐 `rankIssues` · 요약 대상 결과 불변 — 무작위 300묶음 테스트로 고정). 커밋 대상 = 1절 · 7절 1번의 15개 파일 + `plan/PLAN_ops_O1_widget.md` · `reports/ops_O1_widget_build.md` · HANDOFF · PLAN(사용자 D23 · D24 행 포함) — `ex12/.data/`는 git 무시.
3. **Vercel Web Analytics 켜기**: Vercel 대시보드 › issuerank 프로젝트 › **Analytics** › **Enable** → **Deployments**에서 최신 배포 **Redeploy**(Enable 전에 배포된 코드는 script.js 404). 켠 뒤 배포 주소 `/` 한 번 열고 대시보드에 방문이 잡히는지 알려 주기(21 ③). 대시보드가 다른 스크립트 경로를 안내하면 그 경로를 알려 주기. 지표(아침 05~10시 KST 일별 방문자)는 운영 중 대시보드에서 읽음(O1 판정과 무관).
4. **iPhone 확인(기기가 생기면, 18번)**: iPhone Safari로 `https://issuerank-kappa.vercel.app/widget` → 안내 1~6대로 설치 → 작게 · 중간 · 크게 위젯 1개씩 추가 → 확인해서 알려 줄 것: ① 줄 수 3 · 5 · 10, 제목만(숫자 · 섹션 이름 없음), 긴 제목 끝 … ② 하단 시각(`10.17 오전 4:31 수집` 꼴) ③ 중간 · 큰 위젯 줄 누르기 → Safari에서 그 이슈 상세(Scriptable이 잠깐 보이는지) ④ 작은 위젯 누르기 → 랭킹 목록 ⑤ 비행기 모드에서 Scriptable 앱의 ▶ 또는 위젯 새로 고침 → 마지막 내용 그대로(처음 설치 직후 오프라인이면 "불러오지 못했습니다") ⑥ 다크 모드 색 ⑦ 다음 날 아침 하단 시각이 그날로 바뀌는지 ⑧ iOS · Scriptable 버전. 결과를 받으면 18번만 다시 판정.

## 10. 사고 · 한도 초과(정직하게 남김)
- **로컬 3000 요청 누계 초과(약 142/100)**: 1회차는 "주소로 바로 연 탭"에서 CDP가 첫 이동 중 요청 · 오류 이벤트를 일부 못 받아(문서 요청 · script.js 상태 누락) 판정이 불완전했고, 2회차에 요청을 정확히 세는 프록시를 넣었으나 페이지 시작 전 오류 수집기가 같은 이유로 안 붙어, 3회차에 "about:blank에서 준비 뒤 이동(측정) + 주소로 바로 연 탭(기록 수 확인)"으로 바꿈. 모두 읽기 전용(DB 쓰기 0 · 해시 불변) · 외부 유료 호출 0. 그 뒤 원인 확인은 file://로만 함(3000 요청 0).
- **사용자 프로세스 1개를 잘못 종료**: 첫 Chromium을 끈 뒤 남은 것을 `/usr/lib/chromium/|chrome_crashpad` 패턴으로 강제 종료했는데, 그중 PID 17342는 우리 Chromium 트리(11xxx · 14xxx)가 아니라 **사용자 code-oss(Electron 42, PID 17320) 바로 다음 번호의 크래시 처리기(`chrome_crashpad_handler`)로 보임**(code-oss는 `~/.config/Code - OSS/Crashpad` 사용). code-oss 본체와 하위 프로세스 · 이 Claude Code 세션은 계속 정상. 영향은 code-oss를 다시 켤 때까지 크래시 보고가 꺼진 것뿐(code-oss를 재시작하면 복구 — 이 세션이 그 안에서 돌고 있어 제가 재시작하지 않음). 두 번째 Chromium은 `/usr/lib/chromium/` 경로 · 우리 프로필 PID만 정확히 골라 끔.
- 검증 뒤 `updatedLabel` 변수 이름만 바꿨다가(동작 같음) 증거와 소스를 같게 하려고 되돌림 → 최종 소스 = 브라우저 검증 3회차 소스. 테스트 · 빌드는 되돌린 뒤 다시 실행(141/141 · 성공).

## 11. 추가로 보인 점
1. (D25로 ㉠ 방식 반영, 13절) **13번 기존 가로 넘침 제안(사용자 결정 필요)**: 360px에서 목록 · 북마크로 바꿀 때 레이아웃 폭이 378px(`prefers-reduced-motion`이면 360). 휴대폰에서 18px 좌우로 밀릴 수 있음(AC-05-2 관련). 후보: ㉠ `main { overflow-x: clip; }` 1줄 ㉡ 좁은 화면에서 `.view.active[data-from]`의 `--dx`를 0으로. 어느 쪽이든 D20처럼 360 · 390 · 768 · 1280 측정이 필요해 별도 작은 계획으로 하는 것이 맞음.
2. 휴대폰에서 토스트(화면 아래 28px, 기존 위치)가 상세창(바텀시트) 아래쪽 버튼 위에 겹쳐 보임(`o1-360-issue-first-bookmark.png`, 토스트가 사라지는 중에 찍힘). 기존 동작, 변경 안 함.
3. (D25로 반영 — 이제 기존 오류 토스트를 덮지 않음, 13절) 바로가기로 연 날의 `/api/rankings`가 404가 아닌 오류(500 · 502)면 기존 오류 토스트를 "이슈를 찾을 수 없습니다"가 덮음(PLAN에 정의 없음, 빈 화면 안내는 그대로). 필요하면 다음 단계에서 정함.
4. `/?issue=` 쿼리는 첫 스크립트 실행 중 바로 지워지고 Analytics 스크립트는 `defer`라 그 뒤에 돌므로, 방문은 `/`(또는 `/#list`)로 집계됨 — PLAN의 "쿼리 없이 집계"와 같은 결과.
5. 오늘(10-17) 데이터의 updatedAt은 08:34(06:17 예약이 늦게 돈 날). 04:17(D3)은 10-18부터라 17번은 그 뒤 날짜로 확인.
6. 위젯 첫 항목은 사회 1위 `부천 도장공장 화재…`(106건). 1위 묶음 6개는 건수 동점 없음, 나머지 24개 중 건수 동점 14개(D16 importance · 시각 · id로 정해짐 — 따로 계산과 같음).
7. 검증 방법 메모(심사용): 이 기기의 Chromium 155에서 `Target.createTarget`에 주소를 바로 주면 첫 이동 중 CDP 이벤트 · `addScriptToEvaluateOnNewDocument`가 빠질 수 있음 → 측정은 about:blank에서 준비 뒤 `Page.navigate`, 기록 수만 주소로 바로 연 탭으로. 요청 수는 3000 앞 프록시로 세면 정확함. 모바일 에뮬레이션의 가로 넘침은 `scrollWidth ≤ 360`(기기 폭)으로 봐야 함.
8. 안내 페이지는 시스템 글꼴만 써 외부 요청 0. 메인 화면은 기존처럼 Pretendard CSS(jsdelivr)를 받음(이번 측정 중 142건, newsdata · OpenAI 아님).
9. Scriptable 최상위 await · 줄 누른 뒤 Safari로 여는지 · 줄임표 모양 · 최신 iOS 동작은 문서에 없어 18번(실기기)에서만 확인 가능.

## 12. 증거 파일
- 검증 스크립트: `ex12/.data/verify-o1-widget.mjs`(실행: 저장소 루트에서 Chromium을 `--remote-debugging-port=<포트>`로 띄운 뒤 `bash scripts/ex12-ubuntu.sh node .data/verify-o1-widget.mjs <포트>` — 실행마다 로컬 요청 약 55)
- 결과: `ex12/.data/o1-widget-20261017/result.json`(줄별 판정 · 요청 · 상태 · DB 해시), `overflow-360.json`(13번 원인 확인)
- 캡처(360×800): `ex12/.data/o1-widget-20261017/o1-360-issue-first.png`(①) · `o1-360-issue-first-bookmark.png`(⑥) · `o1-360-issue-previous.png`(②) · `o1-360-issue-missing.png`(③) · `o1-360-issue-bad.png`(④) · `o1-360-issue-list.png`(⑤) · `o1-360-home.png` · `o1-360-bookmarks.png`(⑦) · `o1-360-widget-light.png` · `o1-360-widget-dark.png` · `o1-360-widget-copy-fallback.png`(⑧ · 19). 직접 열어 확인: ① · ② · ③ · ⑤ · ⑥ · 안내 페이지 밝게 · 어둡게.
- 최종 파일 sha256(앞 12자리): `widget.js` d44777c18108 · `widget.html` 94cc3beef52c · `index.html` 7f9ce86a3359 · `ranking.ts` 7facaafc9c2d · `routes/widget.ts` dc8b30002418 · `openapi.yaml` 1138eb92ea19 · `next.config.mjs` 82c97d084632 · `app.ts` 8c6c09575de6 · `db/store.ts` ff7fd0c50767 · `sample.ts` 2be237807d02 · `widget-script.test.ts` 8912dbd6715b · `widget-ui.test.ts` 2a954685f997.

## 13. 범위 추가 D25 — 제작 · 재검증 (2026-10-17)
근거: 사용자 결정(2026-10-17) · PLAN.md D25 · `plan/PLAN_ops_O1_widget.md` 끝 「범위 추가 (D25)」. 고친 곳은 `ex12/public/index.html`과 그 VM 테스트뿐(다른 규칙 · 금지는 처음 지시 그대로, 커밋 · 푸시 없음).

### 13.1 바뀐 줄
| 위치 | 변경 |
|---|---|
| `index.html:111-112`(CSS, 화면 전환 블록 바로 아래) | `main { overflow-x: clip; }` 1줄 + 주석. 화면 전환 `translateX(±18px)`가 `main` 밖으로 레이아웃 폭을 늘리지 못하게 자름. `clip`은 스크롤 컨테이너가 아니라 목록 섹션 탭(`position: sticky`)은 그대로 붙음(측정 13.2). 배치 계산에는 영향 없음(좌표 동일, 13.2). 애니메이션 모양 · 시간 그대로 |
| `index.html:545-556` `loadRanking` | 반환값만 더함: 서버가 답했으면 `true`(200 · 404), 그 밖의 오류 · 네트워크 실패면 `false`. 오류 토스트를 띄우는 기존 동작은 그대로(다른 호출부 `setDate`는 반환값을 안 씀) |
| `index.html:1125-1132` `start()` | `const [answered] = await Promise.all(...)`, 상세창을 못 열 때 `answered`일 때만 "이슈를 찾을 수 없습니다". 그래서 5xx · 네트워크 실패면 `loadRanking`의 기존 오류 토스트("API 서버에 연결할 수 없습니다" · "Internal Server Error" 등)가 그대로 남음. 200(id 없음) · 404 · 형식 틀림(최근 날짜 200)은 전처럼 "이슈를 찾을 수 없습니다". 형식 틀림인데 최근 날짜 요청까지 5xx인 경우도 같은 규칙으로 서버 오류 토스트만(덮지 않음) |
| `test/widget-ui.test.ts` (+1, 계 8) | D25: 바로가기 날짜 응답 500 · 502 · 네트워크 실패 → 토스트 = 기존 오류 문구 하나 · 상세창 0 · 그 날짜 · 쿼리 지움 / 형식 틀림 + 최근 날짜 502 → 서버 오류 토스트만 / 200(id 없음) · 404 → "이슈를 찾을 수 없습니다" 하나. 옛 동작(`else toast`)으로 되돌리면 이 테스트가 실패함을 확인(원본 해시 복원) |
- 최종 sha256(앞 12자리): `index.html` 5012e4c992c1 · `widget-ui.test.ts` b1077186568d. 그 밖의 O1 파일은 12절 해시 그대로. 보호 파일 15개 해시 불변(재확인).
- 참고: `overflow: clip`은 iOS 16 · Chrome 90 이상. iOS 15 Safari는 이 줄을 무시해 예전처럼 전환 중에만 잠깐 넘칠 수 있음(화면 깨짐은 없음).

### 13.2 재검증
- `bash scripts/ex12-ubuntu.sh npm test` → **16파일 142/142**(141 + D25 1). `npm run build` → 성공(타입 검사 포함).
- 원인 · 수정 효과 미리 확인(file://, 로컬 3000 요청 0): 360px에서 수정 후 목록 · 북마크 전환의 모든 샘플 ≤ 360(넘침 0/26), 변경 전 HEAD는 378(26/26).
- 실제 서버 재검증 1회: `ex12/.data/verify-o1-d25.mjs`(`next start` 3001 + 요청을 세는 프록시 3000, 측정 탭 = about:blank에서 준비 뒤 이동, Chromium 155 headless). 변경 전(git HEAD) index.html은 프록시가 `/__head/`로만 내주고 API는 같은 서버 · 같은 데이터(검증용, 제품 코드 아님). 데이터 날짜 2026-10-17 · 직전 2026-10-16. 결과 `ex12/.data/o1-widget-20261017/d25/result.json`, **판정 실패 0**.

**① 가로 넘침(D25 검증 ①)** — 화면 전환마다 전환 직후 1 + 전환 중 25ms × 24 + 정지 후 1 = 26샘플, 판정 `scrollWidth ≤ clientWidth` 이고 `innerWidth` = 기기 폭(모바일에서 레이아웃 폭이 늘지 않음 — 처음 판정식 `scrollWidth ≤ innerWidth`는 모바일 에뮬레이션에서 innerWidth가 함께 늘어 못 잡았으므로 더 엄격하게). 360 · 375 · 390은 모바일 에뮬레이션, 768 · 1280은 데스크톱(세로 스크롤바 15px 때문에 clientWidth 753 · 1265).

| 폭 | 애니메이션 | 이슈맵(정지) | → 목록 | → 북마크 | → 이슈맵 |
|---|---|---|---|---|---|
| 360 | 켬 / 끔(reduced) | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 |
| 375 | 켬 / 끔 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 |
| 390 | 켬 / 끔 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 |
| 768 | 켬 / 끔 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 |
| 1280 | 켬 / 끔 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 | 0/26 · 0/26 |
| (참고) 768 변경 전 HEAD | 켬 / 끔 | 0/26 · 0/26 | **26/26**(최대 771 > 753) · 0/26 | **10/26**(최대 786) · 0/26 | 0/26 · 0/26 |
| (참고) 360 변경 전 HEAD(file://) | 켬 / 끔 | 0 · 0 | **26/26**(378) · 0 | **26/26**(378) · 0 | 0 · 0 |
(숫자 = 넘친 샘플 수/전체. 최대 scrollWidth: 새 판은 모든 칸에서 기기 폭 이하.) 변경 전 코드는 데스크톱에서도 전환 중 잠깐 가로로 넘쳤음.

**좌표 · 배치 그대로** — 같은 페이지에서 폭을 768 → 1280 → 390 → 360으로 바꿔 가며 새 판과 HEAD 판을 같은 절차로 잼: 이슈맵 섹션 블록 6개 · 타일 30개의 인라인 left/top/width/height · 클래스(xs/xxs 포함) 문자열과 지도 높이(768 · 1280 = 540, 390 = 1716, 360 = 1572)가 **네 폭 모두 HEAD와 같음**(D20 좌표 포함). 641px 이상 목록 배치(768 · 1280: 카드 30개 · 섹션 머리 6개 · 섹션 탭 줄의 문서 좌표, 글꼴 로드 뒤)도 **HEAD와 같음**. 목록에서 900px 내려도 섹션 탭 top = 56 = 머리글 높이, 머리글 top 0(sticky 그대로). 화면 캡처는 `ex12/.data/o1-widget-20261017/d25/d25-360-list.png` 등 5장(직접 열어 확인: 360 목록 정상).

**② 실패 문구(D25 검증 ②)** — VM 테스트(13.1) 통과.

**영향받는 기존 줄 다시 확인(360×800, 새 판)**
- 13: ① 위젯 첫 항목(`2026-10-17-soc-3`) → 상세창 제목 · 요약 · `106건` · 원문 새 탭 · 쿼리 지움 · 기록 수 = 같은 방식의 `/`와 같음 · 토스트 0 ② 직전일 Top 5 → 10-16 · "오늘" 버튼 · 상세창 ③ 없는 번호 · ④ `abc` → 상세창 없이 "이슈를 찾을 수 없습니다" ⑤ `#list` + 상세창 ⑥ 비로그인 북마크 → 로그인 안내 · 북마크/폴더 요청 0 ⑦ 랭킹 탭 → 뒤로 → 앞으로 · `/#bookmarks` ⑧ `/widget` 칸 = `widget.js` · 실제 클릭 복사 "복사했습니다" · 클립보드 = 파일 · 다크 토큰 · 외부 리소스 0. **모든 화면 `scrollWidth ≤ clientWidth` · innerWidth 360**(처음 FAIL이던 ⑤ · ⑦ 목록 · 북마크 포함).
- 19: Android 7번 문구 그대로 · 360px에서 보임. 20: 상세창 "수집" `10.17 오전 8:34` = 날짜 줄 = `/api/widget` updatedLabel − " 수집". 21 ①: 새 판 페이지 12개 모두 script.js 404 한 번 · JS 예외 0(CDP + 페이지 시작 전 수집기) · `window.va` 함수 · 쿠키 0(HEAD 판은 스크립트 없음 · 예외 0).
- 그 밖: 지도 xs 타일은 오늘 데이터로 360 · 390 각 3개(HEAD와 같음 — 데이터에 따른 값, 10-16 D20 기록의 2 · 0과 날짜가 다름).

### 13.3 요청 수 · 외부 · DB
- 로컬 3000(프록시가 셈, 시작 전 카운터 **0**): **53/60** — Node 2(`/api/widget` · 직전일 `/api/rankings`) + 브라우저 51(문서 11 · rankings 12 · get-session 12 · script.js 404 12 · `/widget` 1 · `/widget.js` 1 · `/__head/` 1 · favicon 404 1). 한도 안 · 503 0. 원인 · 효과 확인 file:// 실행은 3000 요청 0.
- newsdata 0 · OpenAI 0(외부는 메인 화면 글꼴 jsdelivr뿐). DB 9테이블 건수 · md5 전후 같음(읽기만), 서버 로그 오류 없음, 누출 검사 false(결과 폴더 · 스크립트 · index.html · 테스트).

### 13.4 프로세스 정리
- Chromium은 띄우기 전 PID 목록을 저장하고, 띄운 뒤 새로 생긴 PID 중 실행 파일이 `/usr/lib/chromium/`인 것만 내 것으로 기록: 본체 15681(+ 자식 zygote · utility · renderer, 실행 뒤 생긴 크래시 처리기 15690 · 15695). 본체에 TERM 한 번 → 기록한 11개 모두 종료(강제 종료 0). 넓은 패턴 kill 없음.
- `next start`(3001)는 스크립트가 띄운 프로세스 그룹만 종료, 프록시(3000)는 스크립트 안에서 닫음.
- 확인: `curl` 3000 · 3001 · 9341 = 000, 실행 파일이 chromium인 프로세스 0, `ps -ef | grep -E 'next-server|next start|chromium|proot'`(Claude Code 자신 `.local/share/claude/versions`만 제외) 0줄. 사용자 code-oss(PID 17320)는 그대로 실행 중. 브라우저 프로필 · 임시 스크립트 삭제(변경 전 HTML 사본은 비교 증거로 git 무시 폴더 `d25/index-head.html`에 둠).

### 13.5 21줄 합계(갱신)
- **OK 15 · FAIL 1 · SKIP 5.** 13번 FAIL → **OK**(D25로 고치고 재검증). 14번은 **FAIL 그대로**(처음 제작 검증의 로컬 요청 누계 약 142/100 초과 기록을 지우지 않음) — **이번 D25 재검증은 53/60으로 한도 안**(별도 표기). SKIP 15 · 16 · 17 · 21(②③, ①은 OK) · 18 그대로.
- 남은 것: O1 독립 심사(새 세션) → 사용자 커밋 · 푸시 승인 → 배포 줄(8절) → Analytics Enable(9절 3) → iPhone(9절 4).
