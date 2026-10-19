# ops O3 인스타 "오늘의 이슈" 카드 1단계 — 독립 심사 보고

- 날짜: 2026-10-19(KST) 13:10~13:50 · 역할: 심사(`agent_prompts/3_judge_agent.md`), 제작자와 다른 세션. 제품 코드 수정 0 · 측정용 임시 교체 0
- 대상: `ex12/`의 미커밋 변경(기준 HEAD `6e7e18d`). 기준: `plan/PLAN_ops_O3_insta_card.md`(확정 + 끝 메모 ① 하단 한 줄 ② 끝 공백 자르기), SPEC US-12 · AC-12-1~4 · AC-11-1, PLAN.md D3 · D10 · D16 · D21~D28. 제작 보고 `reports/ops_O3_insta_card_build.md`는 참고만 하고 모든 줄을 다시 실행함
- 심사 스크립트는 직접 새로 씀(제작 스크립트 `ex12/.data/o3-build-20261019/*`는 실행하지 않음). 스크립트 · 증거는 세션 scratchpad `judge-o3-20261019/`(저장소 밖)
- 커밋 · 푸시 · stash · Vercel/GitHub/Meta 설정 · Actions: 하지 않음. `web/`: 읽거나 고치지 않음

## 판정

**PASS 보류** — 로컬 줄 1~14 모두 OK, 위반 0. 남은 것 = 배포 15(푸시 후) · 아침 16(푸시 다음 날) · 휴대폰 17(사용자) · 18의 약관 캡처(사용자).
**합계: OK 14 · FAIL 0 · SKIP 4**(15 · 16 · 17 · 18 사용자 부분. 18 저장소 부분은 OK).

## 1. PLAN 항목별

| PLAN 항목 | 판정 | 직접 확인한 근거 |
|---|---|---|
| 1080×1350 PNG · 밝은 카드 하나 | done | 실제 서버 PNG IHDR 1080×1350 · 서명 OK(3장), 색 = `index.html` 밝은 토큰(#f7f7f5 · #161616 · #6f6b65 · `--s-*`)과 같음 |
| 내용 = 위젯 순서 앞 6개 | done | SQL만으로 다시 계산한 순서(아래 5절 10) = `/api/widget` 30개 = caption items 6개 = 카드 6줄(PNG를 눈으로 대조) |
| 넣는 것 ①~⑥ | done | 브랜드 마크 + 이슈랭크 · `10월 19일 월요일` · 오늘의 이슈 · 부제 · 번호 + 섹션 칩 + 제목 · 주소 + 안내 · 하단 `10.19 오전 6:30 수집 · 기사 제목 출처: 각 언론사(newsdata.io 수집) · 순위는 연관기사 수 기준`(끝 메모 ①, 한 줄) |
| 넣지 않는 것 | done | 카드 데이터 칸이 id · section · title뿐(`CardItem`), PNG에 숫자(연관기사 수) · 요약 · 본문 · 로고 · 원문 이미지 · 원문 URL 0, 그림 문자 0 |
| 제목 다듬기 | done | 독립 경계 검사: 33 · 34자 그대로 · 35자 → 33 + `…` · 33번째가 공백이면 지우고 `…`(끝 메모 ②) · 연속 공백/탭/줄바꿈 → 공백 하나 · 국기 · 피부색 · ZWJ 가족 · ❤️ · 키캡 제거(`1️⃣` → `1`) · 美中日北與野尹李檢前 그대로 · 서로게이트 한자도 코드 포인트로 셈 |
| hide | done | 최대 3 · 다음 순서로 채움 · hidden = 보낸 순서 · 4개 · 중복 · 목록에 없는 id · 다른 날짜 id → 400 `요청 형식이 올바르지 않습니다`(두 주소) · 21번째 항목 id는 허용(top6 그대로) |
| 캡션 | done | 내 코드로 따로 만든 기대 캡션과 두 날짜 · hide 2개 모두 한 글자도 다르지 않음, 448자 · `#` 5 · URL 0, 150자 제목 6개 → 줄당 99 + `…` · 전체 821자 ≤ 2,200 |
| 실패 상태 | done | 400 형식 · 미래 · 404(2026-01-01 · 2000-01-01) 실제 서버, 500 `카드를 만들 수 없습니다`(글꼴 없는 cwd로 실제 렌더 실패 → 다음 호출 다시 읽어 200) · 502 `API 서버에 연결할 수 없습니다`(저장소 준비 실패 → 다음 요청 재준비), 로그 = `CARD_RENDER_FAILED` · `CARD_START_FAILED` 한 줄씩뿐. 실패 응답 Cache-Control 없음 · Set-Cookie 없음, POST 405 |
| 캐시 헤더 | done | 200 두 주소 `public, max-age=0, s-maxage=300` |
| `/card` 페이지 | done | 본문 = `public/card.html` 바이트 같음, noindex, 스크립트 src 0 · `https?://` 0 · Analytics 0, 저장 · 복사 · 빼기 · 되돌리기 · 지난 날 안내 · 실패 + 다시 시도(5절 13) |
| 글꼴 포함(카드 함수에만) | done | 깨끗한 빌드 nft: `api/card` = otf 2 + OFL.txt, `api/card/caption` 0, `api/[...path]` 0 |
| 글꼴 출처 · 라이선스 | done | 3절 |
| openapi | done | 0.4.0 · `/api/card`(image/png 200 · 400 · 404 · 500 · 502) · `/api/card/caption`(`CardCaption`) · 지운 줄 = version · description 2줄, lint 오류 0 · 경고 20 = HEAD 20(새 경고 0, card 관련 경고 0) |

PLAN과 다른 점(제작 보고 6절, 판정에 영향 없음): include 키 `'/api/card/route'`(PLAN 표 `'/api/card'`) — nft 결과가 PLAN 요구("캡션 · catch-all 함수에 글꼴 0")를 충족, Vercel 실제 함수 확인은 줄 15. 그림 문자 범위를 국기 등으로 넓힘 · 캡션 제목 공백 정리 — 바깥 요청 0 목적에 맞음.

## 2. AC

| AC | 판정 | 근거 |
|---|---|---|
| AC-12-1 | 충족(로컬) | 1080×1350 PNG · 날짜 · 위젯 순서 앞 6 · 섹션 · 주소 · 수집 시각 · 출처, 본문 · 요약 · 숫자 · 이미지 · 로고 0(줄 5 · 10 · 11) |
| AC-12-2 | 충족(로컬 Chromium) | 실제 클릭 저장 → `issuerank-2026-10-19.png` = 같은 날 `/api/card` PNG 바이트 같음, 실제 클릭 복사 → 클립보드 = 캡션(6제목 · 안내 · 해시태그 5 · 448자). 휴대폰 · 인스타는 줄 17 |
| AC-12-3 | 충족 | 404 · 400 기존 문구, 시계를 하루 뒤로 한 페이지 → `아직 오늘 카드가 없습니다. 마지막 카드: 10월 19일` |
| AC-12-4 | 충족 | 빼기 1 → 2~7번 · 3개 → 버튼 모두 비활성 · 되돌리기 → 처음과 같음, 이미지 src hide = 캡션 hidden(API · 화면) |
| AC-11-1(회귀) | 충족 | `/api/widget` 30개 = SQL 독립 순서, 테스트 전체 통과 |
| AC-03-1 · 03-2 · 05-1 · 09-3(회귀) | 참고 · 충족 | `/api/rankings` 200 · date = SQL 최근 수집일 · 6섹션, 기존 테스트 158개 포함 195/195 |

## 3. 범위 · 보호 파일 · 글꼴

- `git status --short`: 바뀐 것 = `ex12/next.config.mjs` · `ex12/openapi.yaml` · 새 `ex12/{server/card.ts, server/card-image.tsx, app/api/card/route.tsx, app/api/card/caption/route.ts, public/card.html, assets/fonts/×3, test/card*.test.ts ×3}` + 문서(PLAN 끝 메모 · PLAN.md 10절 1줄 · HANDOFF · 제작 보고). 심사 전후 `git status` 해시 같음.
- 보호 32파일(`server/{app,auth,ai,bff,daily,steps,collect,group,grouping,newsdata,summary,summarize,ranking,schemas}.ts` · `server/db/*.ts` 4 · `server/routes/*.ts` · `app/api/[...path]/route.ts` · `public/{index,widget,privacy,terms}.html` · `widget.js` · `package.json` · `package-lock.json` · `.github/workflows/daily.yml`): 작업 트리 SHA-256 = `git show HEAD:` SHA-256, 차이 0. 새 패키지 0. ex01~ex11 · frontend · web · SPEC 변경 0.
- 글꼴 대조: 공식 릴리스 `https://github.com/notofonts/noto-cjk/releases/download/Sans2.004/17_NotoSansKR.zip`을 새 빈 폴더에 다시 받음(HTTP 200 · 26,036,461 B · sha256 `ac7eeb4e…f1747b612` = 제작 보고). 3개만 풀어 `cmp` → `NotoSansKR-Bold.otf`(4,816,044 B · `5a6ceb28…02d9bda`) · `NotoSansKR-Regular.otf`(4,644,748 B · `69975a0a…e0148d68`) · `LICENSE` = `OFL.txt`(`6a73f954…efe2bf2`) **모두 바이트 같음**. OFL 1.1 원문, Reserved Font Name 지정 없음(정의 조항만). 받은 폴더는 끝나고 삭제.

## 4. 테스트 · 빌드 · OpenAPI

- `bash scripts/ex12-ubuntu.sh npm test` → 20파일 **195/195 통과**(32초).
- `.next` 지우고 `bash scripts/ex12-ubuntu.sh npm run build` → **성공**(Turbopack · TypeScript · `tsc --noEmit`), `ƒ /api/card` · `ƒ /api/card/caption` · `ƒ /api/[...path]`.
- `REDOCLY_TELEMETRY=off npx --offline @redocly/cli@2.64.1 lint openapi.yaml` → **오류 0 · 경고 20**, HEAD 판도 경고 20(규칙 분포 같음, card 경로 경고 0).

## 5. 검증 18줄

로컬 서버: 깨끗한 빌드 뒤 `npx next start -p 3000 --hostname 127.0.0.1`(proot, `NEXT_TELEMETRY_DISABLED=1`, `NODE_OPTIONS=--import` 심사 기록기 — 들어온 요청 · DNS · 소켓 연결 · fetch 기록). 비밀값 검사 = `ex12/.env`의 비밀 4값(DATABASE_URL · NEWSDATA · OPENAI · BETTER_AUTH_SECRET) + DB 비밀번호 · 사용자명, stdout · stderr · 응답 · PNG · 내려받은 파일 · 결과 JSON 모두 false. **`ex12/.env`에는 GOOGLE_* 항목이 없음**(이름 5개뿐) → 검사 대상 0(7절 2).

| 줄 | 결과 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | [OK] | 3절. PLAN.md는 10절 메모 1줄(AGENTS.md "실제 단계 문제는 10절" 규칙, O1 · O2와 같은 방식 — 검증 1 "PLAN.md 변경 0"과 글자상 충돌, 참고) |
| 2 카드 데이터 | [OK] | 테스트 통과 + 독립 경계 검사(1절 제목 다듬기 · hide) · dateLabel `10월 7일 수요일` · `10월 19일 월요일` · `1월 1일 목요일` · 항목 3개 → 3줄 |
| 3 캡션 | [OK] | 1절 캡션 |
| 4 라우트 | [OK] | 실제 서버 400 · 404 · 405 + 실패 경로 직접 호출 500 · 502(1절 실패 상태) |
| 5 PNG 실제 렌더 | [OK] | 서버 밖 렌더(전역 fetch를 `data:`만 통과 · 나머지 차단, 소켓 연결 기록): 가짜 카드 3장(긴 제목 · 한자 · 그림 문자 · 앞뒤/연속 공백 · 따옴표, hide, 3줄) → 1080×1350, 바깥 fetch 0 · 소켓 0. 실제 크기 146,046 · 142,579 · 143,954 B(≤ 1.5MB) |
| 6 화면 함수(VM) | [OK] | `card-ui.test.ts` 8개 통과 + 실제 Chromium으로 같은 경우 재확인(줄 13) |
| 7 OpenAPI | [OK] | 4절, 표와 칸 · 상태 대조 |
| 8 빌드 산출물 | [OK] | 1절 글꼴 포함, 두 카드 라우트 동적 |
| 9 로컬 회귀 · 빌드 | [OK] | 4절 |
| 10 로컬 서버 실제 데이터 | [OK] 38/38 | 읽기 전용 SQL: 최근 수집일 2026-10-19 · 직전 2026-10-18. **SQL만으로 순서 재계산**(섹션별 D 이하 최근 날짜 행 → 연관기사 수 ↓ · importance ↓ · 최근 발행 ↓ · id 숫자 순 Top5 → 1위들 먼저 → 나머지, 동점 0): `/api/widget` 30 = SQL 30 · caption date = widget date = SQL · items(id · 섹션 · 원문 제목) = SQL 앞 6 · updatedLabel = SQL `collect_runs` 최신 ok `10.19 오전 6:30 수집` · 캡션 전문 = 독립 생성. 직전 수집일 caption · card 200(같은 검사). `/api/card` 날짜 생략 PNG = `?date=2026-10-19` PNG 바이트 같음. hide 2 · 3 · 21번째 · 400 5가지 · 내일(2026-10-20) 400 · 형식 400 · 2026-01-01/2000-01-01 404 · POST 405 · `/card` = card.html · `/api/rankings` 그대로 |
| 11 실제 이미지 눈으로 | [OK] | 직접 열어 봄: 10-19 · 10-18 실제 카드 + 가짜 카드(李 · 與野 · 美中 · 韓日 · 北 · 尹 · 前 · 檢, 그림 문자 · 끝 공백 · 연속 공백) — 깨짐(□) 0 · 헤드라인 2줄 이하 · 겹침 0 · 하단 한 줄 · 순서 = 캡션. **cmap 직접 파싱**(형식 4 · 12, Bold · Regular 각 23,174자): 최근 7 수집일(10-19 · 18 · 17 · 16 · 15 · 14 · 08) 제목 754개를 다듬은 글자 997종 → **없는 글자 0**(한자 34종 포함), 다듬기로 빠진 그림 문자 = U+2665(♥)뿐. 추가: 754제목 전부를 카드 127장으로 렌더해 PNG를 직접 풀어 줄마다 글자 줄 · 경계 침범 · 오른쪽 넘침 검사 → 3줄 0 · 침범 0 · 넘침 0(검사기가 1줄을 3으로 셌으나 눈으로 2줄 — `스’`만 있는 둘째 줄의 자모 틈) |
| 12 렌더 시간 | [OK] | 캐시 없는 `/api/card`: 첫 요청 **6,998 ms**(글꼴 · wasm · DB 첫 연결) → 601 · 508 · 433 · 437 ms. 모두 < 10초 |
| 13 로컬 화면(Chromium CDP) | [OK] 37/37 | Termux Chromium headless, 내 프로필: 360×800 모바일 — 이미지 natural 1080×1350 · 미리보기 4:5 · src `/api/card?date=2026-10-19` = 저장 링크 · download `issuerank-2026-10-19.png` · alt `10월 19일 오늘의 이슈 카드` · 안내 숨김 · 캡션 상자 = API 캡션 · 6줄 = SQL 앞 6 · 가로 넘침 0 · 밝은 토큰. **실제 마우스 클릭** 복사 → `복사했습니다` · 클립보드 = 캡션 / 저장 → 내려받은 파일 = Node로 받은 PNG 바이트 같음 / 빼기 1 → src · 저장 링크 hide = 1번 id · 줄 = 2~7번 · `뺀 이슈 1개 ·` / 빼기 3개 → 버튼 6개 모두 비활성 · `뺀 이슈 3개 ·` · 줄 = SQL에서 3개 뺀 앞 6 / 되돌리기 → 처음과 같음 / 다크(#121211 · #ecebe8, 넘침 0) / 1280 폭 ≤ 540 · 4:5 · 넘침 0 / 시계 +1일 → 지난 날 안내 / 캡션 500(CDP로 가로챔, 서버에 안 감) → `카드를 불러오지 못했습니다` + `다시 시도` → 누르면 정상. 바깥 요청 0 · `/_vercel/insights` 0 · JS 예외 0(콘솔 오류 = `favicon.ico` 404(기존과 같음) · 내가 만든 500 1줄) |
| 14 안전 · 한도(로컬) | [OK] | 로컬 3000 요청(서버 쪽 새 기록기) **48/100**(Node 26 · 준비 확인 curl 1 · 브라우저 21), PLAN 목표 60 안. next-server 바깥 연결 = DB(pooler) 3(카드 · 캡션 · catch-all 함수 각 1) + 내장 wasm `fetch data:` 1뿐 → newsdata · OpenAI · Meta · Google Fonts · jsdelivr 0. 누출 false. DB 9표 건수 · 내용 md5 전후 같음(articles 1105 · issues 754 · collect_runs 96 · bookmarks 2 · folders 2 · user 2 · session 1 · account 2 · verification 0), 쓰기 0 · 임시 계정 0 |
| 15 배포(푸시 뒤) | [SKIP — 푸시 후] | 8절 |
| 16 아침 준비 | [SKIP — 다음 날 아침] | 8절 |
| 17 휴대폰 · 인스타 | [SKIP — 사용자 휴대폰] | 8절 |
| 18 저작권 기본안 | 저장소 부분 [OK] · 약관 캡처 [SKIP — 사용자] | 카드 · 캡션 · card.html에 본문 · 설명 · 요약 · 연관기사 수 · 원문 이미지 · 로고 · 원문 URL 0, 출처 문구가 카드 하단 · 캡션 둘 다 있음 |

## 6. 위반

없음. 비밀값 · 키 코드 포함 0(카드 파일에 env 읽기는 `DATABASE_URL` 존재 확인뿐) · `.env` git 제외 · PLAN 밖 패키지 0 · PLAN 밖 기능 0 · 카드에 본문 · 요약 · 로고 · 원문 이미지 0 · D0 밖 수집 0 · AGENTS.md 규칙 위반 0.

## 7. 발견한 점(심각도 순)

1. **낮음 · 참고(PLAN 밖 경계 입력)**: 띄어쓰기 없는 넓은 라틴 문자열(`WWW…` 38자)이나 줄표 33개 같은 합성 제목은 줄을 못 나눠 오른쪽으로 카드 밖까지 넘침. 실제 754제목에서는 0. 생기면 그날 그 줄을 `빼기`로 대신할 수 있음. 고칠지는 사용자 결정(예: 제목 칸 `overflow: hidden` 또는 낱말 중간 줄바꿈).
2. **낮음 · 보고 문구**: 제작 보고 줄 14 "누출 검사(ex12/.env 8값, GOOGLE_* 포함)" — 실제 `ex12/.env`에는 GOOGLE_* 항목이 없음(이름 5개). 판정 영향 없음(누출 false), 로컬에서는 Google 로그인이 꺼진 상태라는 뜻.
3. 참고: 같은 `hide`를 여러 번 주면(`?hide=a&hide=b`) 첫 값만 읽음 · `hide=`(빈 값)는 빼기 없음. 화면은 쉼표 하나로만 보냄.
4. 참고: 한국어 글자 단위 줄바꿈으로 `검 / 토”` · `패 / 스’`처럼 둘째 줄에 한 글자만 남는 줄이 있음(사용자 결정으로 그대로 — 제작 보고 13절).
5. 참고(심사 절차): 세션 scratchpad가 제작 세션과 같아, 처음 받은 zip이 제작자의 `fontdl/17_NotoSansKR.zip`(같은 sha256)을 덮어씀 → 내 새 폴더로 옮겨 대조한 뒤 삭제. 제작자 `fontdl/`에는 `main/` · `x/`만 남음(저장소 밖, 영향 없음).

## 8. 푸시 후 확인 절차(다음 역할)

1. **줄 15(푸시 · Vercel 재배포 뒤, 배포 요청 ≤ 30)**: Node fetch `https://issuerank-kappa.vercel.app/api/card/caption` 1 · `/api/card` 2번(10초 간격) · `/card` 1 · `/api/widget` 1. 기대: 200 · PNG IHDR 1080×1350 · 둘째 `/api/card` `x-vercel-cache: HIT` · 클라이언트 cache-control에 s-maxage 없음 · caption items id = widget 앞 6 · 그날 SQL 순서와 같음. **200이 아니면 멈추고** Vercel 빌드 로그의 함수 크기 · 글꼴 포함(`/api/card` 함수에만 otf 2) · 500KB 한도 · 첫 요청 시간(로컬 7초)을 보고.
2. **줄 16(푸시 다음 날)**: 하루 실행이 끝난 뒤(보통 06:30~06:34 + CDN 300초) ~ 09:00 KST에 배포 `/api/card/caption` 1 · `/card` 1 → date = 그날 · 안내 문구 없음 · 실제 준비 시각 기록.
3. **줄 17(사용자)**: 폰으로 `/card` → 저장 · 복사 → 인스타 새 게시물 4:5 미리보기 · 캡션 붙여넣기 → 인스타 안 브라우저로 사이트 · 상세 · Google 로그인(`disallowed_useragent`면 별도 결정, FAIL 아님) 캡처 전달.
4. **줄 18(사용자)**: 첫 게시 전 newsdata 약관(재배포 · 표기 · 무료 플랜 상업 이용) 캡처 · 제목 재게시 법률 검토 권장.

## 9. 요청 · 비용 · 정리

- 로컬 3000: 48/100(서버 쪽 기록). 3000 밖 로컬 직접 요청 0. 외부: GitHub 글꼴 zip 1회(26MB, 대조용)만. newsdata 0 · OpenAI 0 · Meta 0 · Google 0. redocly는 `--offline`(받기 0).
- DB: 읽기만(심사 SQL은 `read only` 트랜잭션), 9표 해시 전후 같음.
- 프로세스: 띄우기 전 `ps -ef` 저장. 내가 띄운 것 = proot 서버 1회(setsid 그룹 17982 → npm 18003 → sh 18158 → next-server 18159) · Chromium 1회(내 프로필, 끝에 `Browser.close` + 그룹 TERM). 끝 확인: curl 3000 · 9333 = 000, `ps`에 next-server · next start · 내 chromium 0, 작업 전 목록에 없던 프로세스 = 확인 셸 · 원래 있던 루프의 `sleep 2`뿐, code-oss(7243 등 electron 16개) 살아 있음. Chromium 프로필 · 내려받기 폴더 · 글꼴 폴더 · 빈 cwd 폴더 삭제. `ex12/.next`는 심사의 깨끗한 빌드 산출물(git 제외).
