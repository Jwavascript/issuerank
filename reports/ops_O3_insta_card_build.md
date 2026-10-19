# ops O3 인스타 "오늘의 이슈" 카드 1단계 — 제작 보고

- 날짜: 2026-10-19(KST) · 역할: 제작(`agent_prompts/2_build_agent.md`) · 기준: `plan/PLAN_ops_O3_insta_card.md`(확정), SPEC US-12 · AC-12-1~4 · AC-11-1, PLAN.md D3 · D5 · D10 · D16 · D21~D28
- 작업 폴더: `ex12/`만(D21, 복사 없음). 시작 HEAD `6e7e18d`, 시작 때 `git status --short` 비어 있음(사용자 변경 없음)
- 커밋 · 푸시 · Vercel/GitHub/Meta 설정 · Actions 실행: 하지 않음. `web/` · `plan/PLAN_web.md`: 읽거나 고치지 않음
- 증거 · 검증 스크립트: `ex12/.data/o3-build-20261019/`(git 제외)

## 1. 작업 범위 · 바뀐 파일

| 파일 | 구분 | 내용 |
|---|---|---|
| `ex12/server/card.ts` | 새 | 순수 함수: `parseCardQuery`(기존 `rankingsQuery` + hide 최대 3 · 중복 없음) · `buildCard(widget, hide)`(위젯 순서 앞 6개, 뺀 id 건너뜀) · `cardTitle`(그림 문자 제거 · 공백 정리 · 34자 넘으면 33자 + …) · `dateLabel` · `cardCaption`(D28 꼴) · `captionResponse` · 두 라우트 공통 `createCardHandler`(쿼리 검사 → 저장소 준비 → 날짜 → 위젯 → 카드). DB · 글꼴을 읽지 않음 |
| `ex12/server/card-image.tsx` | 새 | 카드 JSX(flex만) · 글꼴 읽기(첫 요청 때 `fs.readFile(join(process.cwd(),'assets/fonts/…'))` 한 번, 모듈 범위, 실패하면 다음 요청이 다시) · `next/og` `ImageResponse` → **버퍼로 끝까지 받은 뒤** 응답(`cardPngResponse`, 실패 500) |
| `ex12/app/api/card/route.tsx` | 새 | `GET /api/card` · runtime nodejs · `force-dynamic` · 첫 요청 때 `connectDb({ max: 1 })` |
| `ex12/app/api/card/caption/route.ts` | 새 | `GET /api/card/caption` · 같은 처리기 · 글꼴/그림 코드 import 없음 |
| `ex12/public/card.html` | 새 | `/card` 운영자 페이지(noindex · Analytics 없음 · 바깥 스크립트/글꼴 0 · 밝은/어두운 토큰 = widget.html) |
| `ex12/next.config.mjs` | 고침 | rewrite `/card` 1줄 + `outputFileTracingIncludes: { '/api/card/route': ['./assets/fonts/*'] }`(키 이유 6절 ①) |
| `ex12/openapi.yaml` | 고침 | `/api/card`(image/png 200 · 400 · 404 · 500 · 502) · `/api/card/caption`(`CardCaption`) · 공통 응답 3 · 파라미터 2 · info.version 0.4.0. 지운 줄은 version · description 2줄뿐 |
| `ex12/test/card.test.ts` | 새 | 23개: 데이터 · 캡션 · 처리기(메모리 저장소) |
| `ex12/test/card-image.test.ts` | 새 | 5개: 실제 렌더 · 바깥 fetch 0 · 렌더 실패 500(실제 next/og 스트림 실패 포함) |
| `ex12/test/card-ui.test.ts` | 새 | 8개: card.html 스크립트를 VM에서 |
| `ex12/assets/fonts/NotoSansKR-Bold.otf` · `NotoSansKR-Regular.otf` · `OFL.txt` | 새 | 아래 2절 |

지운 것: 없음. 새 npm 패키지: 0(package.json · lock 해시 같음).

## 2. 글꼴 출처 · 해시 · 라이선스

- 받은 곳(공식 GitHub 릴리스): `https://github.com/notofonts/noto-cjk/releases/download/Sans2.004/17_NotoSansKR.zip`(릴리스 Sans2.004, 2022-01-27 게시, 26,036,461 B, sha256 `ac7eeb4e2b0d41de8ff31b2d6e1e2a41caf253fd5cefb380bfa1f40f1747b612`). 세션 scratchpad의 새 빈 폴더에 받아 `unzip -l`로 목록 확인 → 3개만 풀어 확인 → `ex12/assets/fonts/`로 복사(`LICENSE` → `OFL.txt`).
- 대조: 같은 저장소 main의 `Sans/SubsetOTF/KR/NotoSansKR-{Bold,Regular}.otf`(`https://github.com/notofonts/noto-cjk/raw/main/…`) · `Sans/LICENSE`(raw.githubusercontent.com)를 따로 받아 sha256 비교 → 세 파일 모두 같음.

| 파일 | 크기(B) | sha256 |
|---|---|---|
| NotoSansKR-Bold.otf | 4,816,044 | `5a6ceb287ed2fc6cfc6213144ebea68cbd94b20fc9eb873d8486493bf02d9bda` |
| NotoSansKR-Regular.otf | 4,644,748 | `69975a0ac8472717870aefeab0a4d52739308d90856b9955313b2ad5e0148d68` |
| OFL.txt | 4,301 | `6a73f9541c2de74158c0e7cf6b0a58ef774f5a780bf191f2d7ec9cc53efe2bf2` |

- 크기는 PLAN 표(4,816,044 · 4,644,748)와 같음. 라이선스 SIL Open Font License 1.1, 파일에 Reserved Font Name 지정 없음(OFL 본문의 RFN 정의 조항만 있음). `file`: OpenType font data. cmap 글자 수 Bold · Regular 각 23,174.

## 3. PLAN 항목별 반영 위치

| PLAN 항목 | 반영 |
|---|---|
| 1080×1350 PNG · 밝은 카드 하나 | `card-image.tsx` `CARD_WIDTH/HEIGHT` · 색 상수(사이트 밝은 토큰) |
| 내용 = 위젯 순서 앞 6개 · 빼기 건너뜀 · 적으면 있는 만큼 | `card.ts` `buildCard` (입력은 기존 `store.widget(date)` 그대로) |
| 넣는 것 ①~⑥ · 크기 · 굵기 | `CardView`: 브랜드 마크(18px 마크를 40px로, 아래 칸 opacity .45) + 이슈랭크 34 Bold · 날짜 30 · 제목 88 Bold · 부제 34 · 번호 44 Bold · 칩 24 Bold(바탕 `--s-*` 밝은 값, 글자 #ffffff) · 헤드라인 40 Bold 줄 높이 1.3 · 주소 30 Bold + 안내 · 하단 22. 바깥 여백 72, flex만 |
| 넣지 않는 것 | 본문 · 설명 · 요약 · 연관기사 수 · 이미지 · 로고 · URL 칸 자체가 카드 데이터에 없음(`CardItem` = id · section · title) |
| 제목 다듬기 | `cardTitle`(그림 문자 → 공백 정리 → 코드 포인트 34 초과면 33 + …). 그림 문자 범위는 6절 ② |
| 글꼴 · 읽기 | `loadFonts`(Node 런타임, 모듈 범위) |
| 배포 포함 | `next.config.mjs` include(키 6절 ①). 빌드 nft: card 함수 otf 2 · 캡션 0 · catch-all 0 |
| `/api/card` · 캐시 · 실패 문구 · 로그 | `createCardHandler` + `cardPngResponse`. 200 `image/png` · `public, max-age=0, s-maxage=300`, 실패 = 캐시 헤더 · Set-Cookie 없음, 로그 `CARD_START_FAILED` · `CARD_RENDER_FAILED`만 |
| `/api/card/caption` 칸 · 캡션 꼴 | `captionBody` = `{ date, dateLabel, updatedLabel, items, hidden, caption }`, `cardCaption` |
| DB 읽기만 · max 1 | 두 라우트 `loadStore`(기존 `dbStore`의 `latestDate` · `widget`만 부름) |
| `/card` 화면 | `card.html`: 제목 → 날짜 · updatedLabel → (지난 날이면) `아직 오늘 카드가 없습니다. 마지막 카드: {M}월 {D}일` → 미리보기(`<img>` 100% · 최대 540px, alt) → `이미지 저장`(download, 모르는 브라우저는 안내) · `캡션 복사`(성공 · 실패 문구) → 6줄 + `빼기`(3개 뒤 비활성) → `뺀 이슈 n개 · 되돌리기` → 캡션 상자(읽기 전용) → 안내 1~3. 실패 `카드를 불러오지 못했습니다` + `다시 시도`. 빼기 요청은 처음 받은 date를 붙여 같은 날짜 유지 |
| OpenAPI | 1절 |
| 기존 화면 · API | 그대로(해시 4절) |

## 4. 테스트 · 빌드

- `bash scripts/ex12-ubuntu.sh npm test` → **20파일 194/194 통과**(기존 158 + 새 36: card 23 · card-image 5 · card-ui 8). vitest가 git 추적 파일을 바꾸지 않음(`git status`에 이번 파일만).
- `bash scripts/ex12-ubuntu.sh npm run build` → **성공**(Turbopack 컴파일 · TypeScript · `tsc --noEmit`). 라우트: `ƒ /api/[...path]` · `ƒ /api/card` · `ƒ /api/card/caption`(빌드 중 DB 접속 0). 첫 빌드 1회는 테스트 파일 타입 오류(`vi.fn` 타입)로 실패 → 테스트 파일만 고쳐 통과.
- OpenAPI: `REDOCLY_TELEMETRY=off npx --offline @redocly/cli@2.64.1 lint openapi.yaml` → **오류 0 · 경고 20**. 같은 도구로 HEAD 판(`git show HEAD:ex12/openapi.yaml`)도 경고 20 → 새 경로가 더한 경고 0. (처음 한 번은 npx 캐시에 없어 `npx -y`로 받았고 그 1회는 텔레메트리 끄기 없이 실행됨 — 프로젝트 의존성 변경 0.)
- 해시(검증 1): 작업 전 기록한 28개(`server/{app,auth,ai,bff,daily,steps,collect,group,grouping,newsdata,summary,summarize,ranking,schemas}.ts` · `db/store.ts` · `routes/*.ts` 4 · `app/api/[...path]/route.ts` · `public/{index,widget,privacy,terms}.html` · `widget.js` · `package.json` · `package-lock.json` · `.github/workflows/daily.yml`) **모두 같음**(`sha256sum -c`, 기록 `hash-before.txt`).

## 5. 검증 줄(PLAN 1~18)

| 줄 | 결과 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | OK | `git status` = 1절 목록뿐, 해시 28개 같음, ex01~ex11 · `web/` · SPEC 변경 0. PLAN.md는 10절 메모 1줄만(사용자 지시 문서 갱신) |
| 2 카드 데이터 | OK | `card.test.ts` ①~⑥: 앞 6개 = `buildWidget` 앞 6개 · hide 1/3 → 다음 항목으로 채움 · hidden 순서 · 4개/중복/없는 id 400 · 3개뿐 → 3 · 33/34자 그대로 · 35자 → 33 + … · 공백 · 그림 문자(국기 · 피부색 · ZWJ 가족 · ❤️ 포함) 0 · 美中日北與野尹李 그대로 · 서로게이트 한자도 코드 포인트로 셈 · `10월 7일 수요일` · `10월 19일 월요일` · `1월 1일 목요일` |
| 3 캡션 | OK | 고정 입력 전문 일치 · 번호 · 섹션 이름 · 150자 제목 → 99 + … · ≤ 2,200 · `#` 5 · URL 0 · `n건` 0 |
| 4 라우트 | OK | 처리기 직접 호출: 200 칸 · 캐시 헤더 · Set-Cookie 0 / 미래 · 형식 · 빈 date 400 / 2026-10-01(데이터 없게 한 저장소) 404 / hide 4 · 중복 · 없는 id 400 / 최근 수집일 없음 404 / 저장소 준비 실패 502 · 다음 요청 재준비 / 조회 실패 502 / 렌더 실패 500 · 실패에는 캐시 헤더 없음 · 로그 = 안전 코드 1줄만 · 형식 400은 저장소를 열지 않음 |
| 5 PNG 실제 렌더 | OK | `card-image.test.ts`: 전역 fetch를 `data:` 말고는 실패하는 가짜로 바꿈(6절 ③) → 샘플 날짜 · 美中日/따옴표/긴 제목/그림 문자 카드 · 3줄 카드 모두 PNG 서명 · IHDR 1080×1350 · 바깥 fetch 0. 크기 111,821 B · 136,799 B(목표 ≤ 1.5MB). 렌더 함수 던짐 → 500, 실제 `ImageResponse` 스트림 안 Satori 실패도 500(200 뒤 끊김 아님) |
| 6 화면 함수(VM) | OK | `card-ui.test.ts` ①~⑦(정상 · 빼기 1→3→4번째 무시→되돌리기 · 지난 날 안내 · 500/네트워크 실패 → 다시 시도 · 복사 성공/거절 · download 미지원 안내 · noindex/Analytics/바깥 주소 0) |
| 7 OpenAPI | OK | 4절 |
| 8 빌드 산출물 | OK | `.next/server/app/api/card/route.js.nft.json` otf 2 + OFL · `card/caption/…nft.json` otf 0 · `[...path]/…nft.json` otf 0 |
| 9 로컬 회귀 · 빌드 | OK | 194/194 · 빌드 · 새 패키지 0 |
| 10 로컬 서버 실제 데이터 | OK 21/21 | `next start`(3001) + 세는 프록시(3000), Node fetch(`api.mjs`): SQL 최근 수집일 2026-10-19 · 직전 2026-10-18(읽기 전용 SQL). widget date = caption date = SQL · caption items id/섹션/원문 제목 = widget 앞 6 · updatedLabel `10.19 오전 6:30 수집` 같음 · 캡션 448자 · `#` 5 / `/api/card` 200 image/png · 1080×1350 · 캐시 헤더 · 쿠키 0 / 직전 수집일 caption · card 200 / `?date=2026-10-20`(KST 내일) 400 미래 · `2026-01-01` 404 · `2026-1-7` 400 형식(두 주소, 실패 응답 cache-control 없음 — 실제 헤더 목록 확인) / hide 2개 → 다음 순서 6 · hidden / `/card` = card.html 바이트 같음 / `/api/rankings` 200 · date = SQL · 섹션 6(catch-all 영향 0) / `POST /api/card` 405 |
| 11 실제 이미지 눈으로 | OK | 7절 + cmap 대조: 최근 7개 수집일(10-19 · 18 · 17 · 16 · 15 · 14 · 08) 이슈 제목 754개(위젯 목록의 상위 집합, D8 대체 행 제외)를 다듬은 뒤 글자 997종 → Bold · Regular cmap에 **없는 글자 0**. 한자 32종(亞銀證株韓車來中美曺不軍男李與日乙丑秋尹露北反弗檢女伊英靑孝故獨佛南) · 기호(‘’“” · … ‥ ⋯ ′ Ⅳ ↑→↓ ㎜ ㎡ 등) 모두 있음. 다듬기로 빠진 그림 문자 = ♥(U+2665) 12제목 |
| 12 렌더 시간 | OK | 캐시 없는 `/api/card`: 첫 요청 **7,150 ms**(서버 첫 실행, 모듈 · wasm · 글꼴 · DB 첫 연결 포함) → 603 · 593 · 502 ms. 서버를 다시 띄운 첫 요청 7,894 ms. 모두 < 10초 |
| 13 로컬 화면(Chromium CDP) | OK 22/22 | Termux Chromium 155 headless: 360×800(모바일) 이미지 natural 1080×1350 · 미리보기 비율 4:5 · src = `/api/card?date=2026-10-19` · 저장 링크 download `issuerank-2026-10-19.png` · alt · 오늘이라 안내 숨김 · 캡션 상자 = 10번 캡션 · 6줄 · 가로 스크롤 0 · noindex · 밝은 토큰 / **캡션 복사**(권한 부여) → `복사했습니다` · 클립보드 = 캡션 / **이미지 저장** 클릭 → 내려받은 `issuerank-2026-10-19.png` = 같은 날짜 `/api/card` PNG와 바이트 같음 / 빼기 1번 → img src · 저장 링크 hide = 뺀 id · 줄 = 처음 2~7번 · `뺀 이슈 1개 ·` · 캡션 번호 줄 = 화면 줄 / 다크 토큰(#121211 · #ecebe8) / 되돌리기 → 처음과 같음 / 1280 가로 스크롤 0 · 폭 ≤ 540 · 4:5 / 바깥 요청 0 · `/_vercel/insights` 0 · JS 예외 0(콘솔은 `favicon.ico` 404 한 줄 — 기존 페이지와 같음, 이번 범위 밖). 첫 실행에서 미리보기 눌림 발견 → 고친 뒤 다시 실행(6절 ④) |
| 14 안전 · 한도(로컬) | OK | 3000 요청 **36/100**(PLAN 목표 60 안, 차단 0) + 3001 직접 curl 1회(`/card`, 서버 준비 확인 때 실수로 — 세지 않은 로컬 요청으로 기록) · newsdata 0 · OpenAI 0 · Instagram/Meta 0 · Google Fonts · jsdelivr 0. 바깥 연결 기록기(`monitor.mjs`를 `--import`로 next-server에 넣음): 둘째 서버 실행 기록 = `loaded` 1 · `fetch data:` 1(내장 wasm)뿐, 첫 실행도 기록 0. 누출 검사(ex12/.env 8값, GOOGLE_* 포함) 서버 로그 2개 · 결과 JSON · 캡션 · PNG 모두 false. 서버 로그 안전 코드 0건(실패 없음). DB 9표 건수 · 내용 md5 전후 같음(articles 1105 · issues 754 · collect_runs 96 · bookmarks 2 · folders 2 · user 2 · session 1 · account 2 · verification 0). 임시 계정 0 |
| 15 배포(푸시 뒤) | SKIP — 푸시 후 | 8절 1 |
| 16 아침 준비(푸시 다음 날) | SKIP — 푸시 후 | 8절 2 |
| 17 휴대폰 · 인스타(사용자) | SKIP — 사용자 확인 | 8절 3 |
| 18 저작권 기본안 | 저장소 부분 OK · 약관 캡처 SKIP(사용자) | 카드 · 캡션 · card.html에 본문 · 설명 · 요약 · 연관기사 수 · 원문 이미지 · 로고 · 원문 URL 0, 출처 문구 카드 · 캡션 둘 다 있음(2 · 3 · 11번). newsdata 약관 캡처 · 법률 검토는 사용자 할 일 |

**합계: OK 14 · FAIL 0 · SKIP 4(15 · 16 · 17 · 18의 사용자 부분)**. 배포 · 사용자 줄 전이라 PASS 아님.

## 6. 꼭 필요했던 내부 세부 · PLAN과 다른 점(보고)

1. **include 키 `'/api/card/route'`**(PLAN 표는 `'/api/card'`). 이 Next 16.4 Turbopack 빌드는 키를 진입점 이름(`app/api/card/route`)에 "포함" 일치로 맞춤. 실험(표식 파일로 4가지 키): `'/api/card'` → 캡션 함수에도 글꼴 9.4MB가 들어감, `outputFileTracingExcludes`로도 빠지지 않음, `'/api/card/route'` → 카드 함수만. PLAN의 "캡션 · catch-all 함수에는 글꼴 0" 요구를 지키려고 바꿈. (include가 없어도 Turbopack이 `join(process.cwd(),'assets/fonts',…)`를 보고 카드 함수에 글꼴을 넣는 것도 확인 — include는 보험.) Vercel에서도 같은 nft를 쓰는지는 배포 줄 15에서 확인.
2. **그림 문자 범위**: `\p{Extended_Pictographic}`에 더해 국기(지역 표시 문자) · 피부색 · 변형 선택자 FE0F · ZWJ · 키캡 20E3도 뺌. 국기 · 남은 조각도 Satori가 그림 문자로 보고 jsdelivr로 나갈 수 있어서(바깥 요청 0 목표). 캡션 제목에도 같은 함수 + 공백 정리(줄바꿈이 섞이면 번호 줄 꼴이 깨지므로) — PLAN "그림 문자만 제거"보다 공백 정리 하나 더.
3. **바깥 fetch 검사에서 `data:` 제외**: Satori가 내장 yoga.wasm을 `fetch('data:…')`로 읽음(네트워크 아님). 처음 테스트가 이것 때문에 실패 → `data:`만 진짜 fetch로 넘기고 나머지는 기록 · 실패.
4. **미리보기 눌림 수정**: `<img width="1080" height="1350">` 속성이 CSS 높이 1350px이 되어 360 화면에서 폭 328 · 높이 1350으로 눌림(첫 CDP 캡처로 발견) → `.preview { height: auto }`. 다시 측정 4:5.
5. 처리기 순서: 쿼리 형식 400은 DB를 열기 전에. 저장소 준비 실패뿐 아니라 첫 조회 실패도 502(postgres는 첫 조회 때 접속하므로 "DB 준비 실패"가 조회에서 남). hide로 줄이 0개가 되면(항목 ≤ 3개인 날) 400 `요청 형식이 올바르지 않습니다`.
6. 두 라우트의 DB 연결 함수(4줄)는 "연결은 라우트 모듈에서" 대로 각 라우트 파일에 둠(`card.ts`는 DB를 모름). 캡션 200 응답 함수 `captionResponse`는 테스트가 실제 라우트와 같은 것을 쓰도록 `card.ts`에 둠.
7. `/card` 미리보기 이미지가 실패(`error` 이벤트)해도 `카드를 불러오지 못했습니다` + `다시 시도`를 보임.

## 7. 카드 PNG — 눈으로 본 결과

파일: `ex12/.data/o3-build-20261019/card-2026-10-19.png`(146,410 B) · `card-2026-10-18.png` · `card-2026-10-19-hide2.png` · `fake.png`(美中日 · 긴 제목 · 그림 문자) · `sample.png`(메모리 샘플). 화면 캡처 `ui-360-first.png` · `ui-360-hide1.png` · `ui-360-dark.png` · `ui-1280.png`.

- 한글 · 한자(中 · 韓 · 曺 · 與野 · 尹 · 李 · 美 · 日 · 北) · 따옴표 ‘’“” · `"` · 쉼표 · 숫자 · 영문 모두 깨짐(□) 0.
- 헤드라인 6줄 모두 2줄 이하, 겹침 · 잘림 0. 34자 넘는 제목은 2줄째 끝이 `…`로 끝남. 줄 높이는 6줄이 남는 높이를 나눠 가져 1줄 제목도 같은 칸.
- 위 → 아래: 브랜드 마크 + 이슈랭크 · `10월 19일 월요일` / `오늘의 이슈` / `어제 가장 많이 보도된 뉴스` / 번호 1~6 + 섹션 칩(색 = 사이트 밝은 섹션 색) + 제목 / `issuerank-kappa.vercel.app 섹션별 순위 · 3줄 요약 · 원문 링크` / `10.19 오전 6:30 수집 · 기사 제목 출처: 각 언론사(newsdata.io 수집) · 순위는 연관기사 수 기준 참고 지표`. 숫자(연관기사 수) · 요약 · 로고 · 원문 이미지 0.
- hide2 카드: 1위(정치) · 3위(IT·과학)가 빠지고 다음 순서(정치 2개 등)가 채워짐 = 캡션과 같은 순서.
- 눈에 띈 점(고치지 않음, 결정 후보): ① 하단 22px 줄이 2줄로 넘어가 끝 낱말 `지표`만 둘째 줄에 남음(넘침 아님) ② Noto Sans KR의 `…`(U+2026)는 가운데 높이 점 3개 모양 ③ 한국어 줄바꿈이 글자 단위(사이트와 같은 기본값, 예 `북 미 / 사일`) ④ 33번째 글자가 공백이면 `입장 …`처럼 공백 뒤 `…`(PLAN "앞 33자 + …" 그대로) ⑤ 제목 원문의 `[사설]` 같은 머리말도 그대로 나옴(10-19 정치 1위).

## 8. SKIP 줄 절차(사용자 · 다음 역할)

1. **배포 줄 15(푸시 뒤)**: 사용자가 커밋 · 푸시 승인 → Vercel 재배포 끝난 뒤 배포 요청 ≤ 30: Node fetch `https://issuerank-kappa.vercel.app/api/card/caption` 1 · `/api/card` 2번(10초 간격) · `/card` 1 · `/api/widget` 1. 기대: 200 · PNG IHDR 1080×1350 · 둘째 `/api/card`에 `x-vercel-cache: HIT` · 클라이언트 cache-control에 s-maxage 없음 · caption items id = widget 앞 6개. **200이 아니면 멈추고 원인 보고** — 특히 Vercel 함수 크기 · 글꼴 포함(nft에 otf 2개가 실제 함수에 들어갔는지, Vercel 빌드 로그 함수 크기, "500KB 번들 한도"가 Node 런타임에도 걸리는지 = 확인 필요) · 첫 요청 시간(로컬 첫 요청 7~8초). 막히면 사용자 결정: 글꼴 부분 집합(도구는 새 패키지 → 승인 필요).
2. **아침 줄 16(푸시 다음 날)**: 그날 하루 실행이 끝난 뒤(보통 06:30~06:34 + CDN 300초 → 06:40쯤) ~ 09:00 KST에 배포 `/api/card/caption` 1 · `/card` 1. 기대: date = 그날 · 안내 문구 없음 · 실제 준비 시각 기록.
3. **휴대폰 줄 17(사용자)**: 폰 브라우저로 `https://issuerank-kappa.vercel.app/card` → ① `이미지 저장`(안 되면 길게 눌러 저장) · `캡션 복사` ② 인스타그램 › 새 게시물 › 저장한 이미지 → 4:5 세로로 잘림 없이 보이는지 · 캡션 붙여넣기(올리기는 선택) — 화면 캡처 전달 ③ 인스타 프로필 웹사이트 칸(또는 DM)에 사이트 주소를 넣고 인스타 안에서 열어 이슈 상세 · Google 로그인 시도 → 결과(`403 disallowed_useragent` 여부) 전달. `disallowed_useragent`면 안내 문구는 별도 결정(이번 FAIL 아님).
4. **줄 18 사용자 부분**: 첫 게시 전 newsdata 약관(재배포 · 표기 · 무료 플랜 상업 이용) 화면 캡처 · 제목 재게시 법률 검토 권장(D28).

## 9. 요청 · 비용 · 프로세스

- 로컬 3000: 36/100(Node 21 + 1 확인 + 브라우저 2회 실행 14). 3001 직접 1회. 바깥: newsdata 0 · OpenAI 0 · Instagram/Meta 0 · Google 0 · 렌더 중 바깥 fetch 0. 그 밖의 바깥 요청(제품 아님): GitHub(글꼴 zip 1 · 대조 파일 3 · 릴리스 API 3), npm 레지스트리(redocly 1회 받기 + 그 실행의 텔레메트리 가능성).
- DB: 읽기만(검증 SQL은 읽기 전용 트랜잭션), 9표 해시 전후 같음.
- 프로세스: 띄우기 전 PID 목록 저장(`ps-before.txt`, 사용자 code-oss = 7243 등 electron). 내가 띄운 것 = proot 서버 2회(server.mjs 25554/29227 → next-server 25563/29244) · Chromium 2회(1943 · 4689 트리 각 12개). 모두 정지 파일 · 프로세스 그룹 신호로 종료. 끝 확인: curl 3000 · 3001 · 9363 = 000, `ps`에 next-server · next start · server.mjs · 내 chromium 0, 작업 전 목록에 없던 프로세스 = 이 확인 셸뿐(Claude Code 자신 `.local/share/claude/versions` 제외), code-oss 7243 살아 있음. Chromium 프로필 · 내려받기 폴더 삭제(빈 폴더 확인).

## 10. 필요했지만 없던 키 · 계정

없음(카드는 공개, 임시 계정 0). Instagram/Meta 계정 · 키는 이번 범위 밖(2단계).

## 11. 사용자 할 일

1. 이 제작 확인 → 독립 심사(새 세션, `agent_prompts/3_judge_agent.md`) 또는 바로 커밋 · 푸시 승인(커밋 대상 = 1절 ex12 파일 + `plan/PLAN_ops_O3_insta_card.md` 그대로 + 이 보고 · HANDOFF · PLAN.md 10절 메모).
2. 푸시 뒤 배포 줄 15 → 다음 날 아침 16 → 휴대폰 17(8절).
3. 첫 게시 전 newsdata 약관 캡처 · 제목 재게시 법률 검토 권장.
4. 결정 후보(선택): 7절 ①~⑤(하단 줄 배치 · 줄바꿈 방식 등). 자동 게시(2단계)는 1단계 2~4주 운영 뒤.

## 12. 미해결

- 배포 함수 크기 · 글꼴 포함 · 500KB 한도 적용 여부 · 배포 첫 요청 시간(줄 15에서).
- 인스타 피드 4:5 미리보기 · 인앱 브라우저 Google 로그인(줄 17).
- newsdata 약관 · 제목 재게시 법률 판단(사용자).

## 13. 사용자 결정 반영(2026-10-19, 심사 전 수정)

결정: 7절 ① 하단 줄 넘침 · ④ 자른 끝 공백만 고침. ② 줄임표 글리프 · ③ 글자 단위 줄바꿈 · ⑤ 머리말은 그대로.

| 항목 | 바뀐 곳 | 내용 |
|---|---|---|
| ① 하단 작은 줄 한 줄 | `server/card-image.tsx` `cardFooter` | `{updatedLabel} · 기사 제목 출처: 각 언론사(newsdata.io 수집) · 순위는 연관기사 수 기준` — 끝 "참고 지표"만 뺌. 수집 시각 · 출처 문구는 그대로, 캡션 문구는 바꾸지 않음 |
| ② 자른 끝 공백 | `server/card.ts` `clip`(카드 · 캡션 공통) | 34자(캡션 100자) 넘을 때 앞 33자(99자)에서 `trimEnd()` 뒤 `…`. 그 이하 길이는 그대로 |
| 문서 | `openapi.yaml` 설명 2곳(“자른 끝 공백은 지움”) · `plan/PLAN_ops_O3_insta_card.md` 끝 메모 1줄 | |

- 테스트: `card.test.ts` ⑤에 끝 공백 자르기 5개 · 캡션 99번째 공백 · 정확히 100자 그대로 추가. `card-image.test.ts`에 새 1개 — 하단 문구 문자열 + PNG를 직접 풀어(zlib · 행 필터 복원) 맨 아래 구간(y 1120~1350)의 글자 줄 덩어리 = 2(주소 줄 + 하단 줄)인지, 수집 시각 `10.19 오전 6:34 수집`과 가장 긴 `12.31 오후 12:59 수집` 둘 다. 음성 대조(임시 파일, 실행 뒤 지움): 하단이 두 줄이 되게 한 라벨 → 3이 나와 검사가 넘침을 잡음 확인.
- `npm test` **20파일 195/195**(+1 테스트, 기존 테스트 안에 확인 추가) · `npm run build` 성공(nft: 카드 otf 2 · 캡션 0 · catch-all 0) · OpenAPI 오류 0 · 경고 20 · 보호 28파일 해시 같음 · 새 패키지 0.
- 다시 만든 PNG(직접 열어 봄): `ex12/.data/o3-build-20261019/card-2026-10-19-v2.png`(146,046 B, 로컬 서버 실제 데이터) · `card-2026-10-18-v2.png` · `fake.png`(메모리 render 스크립트). 하단 줄 한 줄로 들어감(세 장 모두), 10-18 1위 `…추미애 사과에 야당 …` → `야당…`, fake 1위 `李 대표 입장 …` → `입장…`. 한글 · 한자 · 따옴표 깨짐 0, 헤드라인 2줄 이하. 10-19 캡션은 처음과 글자 하나 다르지 않음(100자 넘는 제목 없음).
- 요청: 로컬 3000 이번 분 **4**(누계 40/100), 3001 직접 0. 렌더 중 바깥 fetch 0(기록 = `loaded` · `fetch data:`뿐) · newsdata/OpenAI/Meta 0. 누출 검사 false. DB 9표 해시 작업 처음과 같음.
- 정리: 서버 1회(server.mjs 5282 → next-server 5301) 종료, 3000 · 3001 · 9363 = 000, 작업 전 목록에 없던 프로세스는 확인 셸뿐(`sleep 2`는 부모 13653 — 작업 전부터 있던 다른 루프), code-oss 7243 살아 있음. 브라우저는 이번에 띄우지 않음(화면 변경 없음).
- 검증 합계는 그대로 OK 14 · FAIL 0 · SKIP 4.

## 14. 로그인 창 버튼 대비 수정(사용자 신고 · 결정 2026-10-19, O3와 같은 배포)

- 신고: 로그인 창의 로그인 · 회원가입 · 닫기를 누르면 버튼이 검게 바뀌어 글자가 안 보임(휴대폰 다크 모드, 탭 뒤 남는 :hover).
- 원인: `.auth-dialog`는 글자 #222로 늘 밝은 창인데, 창 안 `.chip-btn:hover { background: var(--hover) }`가 다크 토큰 `--hover #222220`을 그대로 씀.
- 바뀐 줄(`ex12/public/index.html` 1규칙 + 주석 1줄): `.auth-dialog { …; color: #222; background: #ffffff; --hover: #efede9; --line: #e7e4df; }`(값 = :root 밝은 토큰). 창 밖 화면 · 다른 버튼 · Google 버튼 · 다크 일반 화면은 그대로(사용자 지정 속성이라 창 안에만 적용).
- 보호 파일: `public/index.html`은 이번 사용자 결정으로 바뀜. 나머지 27개 해시는 같음. 새 패키지 0.
- `npm test` 195/195 · `npm run build` 성공.
- 측정(`ui-auth.mjs`, 실제 Chromium 155 · CDP · 360×800): 변경 전 = HEAD `index.html`을 CDP Fetch로 `/` 문서 응답에 넣음(서버 요청 아님), 변경 후 = 작업 트리 파일. 다크 · 밝게 × 세 버튼에 `CSS.forcePseudoState`로 기본 · :hover · :active · :focus-visible을 주고, 계산된 글자색과 배경색(투명이면 조상 배경)의 대비를 잼. 세 버튼 값이 모두 같음:

| | 기본 | :hover | :active | :focus-visible |
|---|---|---|---|---|
| 변경 전 다크 | 15.91 | **1.00**(#222 위 #222220) | 15.91 | 15.91 |
| 변경 전 밝게 | 15.91 | 13.61 | 15.91 | 15.91 |
| 변경 후 다크 | 15.91 | 13.61(#efede9) | 15.91 | 15.91 |
| 변경 후 밝게 | 15.91 | 13.61 | 15.91 | 15.91 |

- 다크 hover가 실제 문제였음을 재현함(1.00:1). 변경 후 24칸 모두 ≥ 4.5. :active · :focus-visible은 배경을 바꾸는 규칙이 없어 원래 문제 없음.
- 캡처(세 버튼 hover, 직접 열어 봄): `ex12/.data/o3-build-20261019/auth-before-dark-hover.png`(버튼 셋이 검은 덩어리, 글자 안 보임) · `auth-after-dark-hover.png` · `auth-after-light-hover.png`(밝은 회색 버튼 · 글자 보임 · 테두리 밝은 선) · 참고 `auth-before-light-hover.png`.
- 요청: 로컬 3000 이번 분 **8**(누계 48/100, 변경 전 `/`는 CDP가 넣어 서버로 안 감). 바깥: 기존 `index.html`의 Pretendard CSS(jsdelivr) 요청을 2번 시도했으나, `--host-resolver-rules`로 이름 풀이를 막아 나간 연결은 0. newsdata/OpenAI/Google/Meta 0. `get-session`은 세션 없음(읽기만). DB 9표 해시 처음과 같음. 누출 false.
- 정리: 서버(26241 → next-server 26251) · Chromium(26347 트리 12개) 종료. 3000 · 3001 · 9363 = 000, 작업 전 목록에 없던 프로세스는 확인 셸뿐, code-oss 7243 살아 있음, 프로필 폴더 삭제(빈 폴더).
- 참고: 작업 중에 `reports/ops_O3_insta_card_judge.md`가 새로 생김(다른 세션 — 건드리지 않음).
