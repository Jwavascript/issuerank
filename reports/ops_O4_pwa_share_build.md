# ops O4 PWA(홈 화면 앱) + 공유 — 제작 보고

- 날짜: 2026-10-19(KST, 15:50~16:55) · 역할: 제작(`agent_prompts/2_build_agent.md`) · 기준: `plan/PLAN_ops_O4_pwa_share.md`(확정), SPEC US-13 · AC-13-1~4(회귀 AC-11-2 · 04-2 · 07-3 · 09-2 · 05-2 · 06-3), PLAN.md D10 · D21~D29(공유 주소 `/s?issue=<id>` — D29 최종 문구)
- 작업 폴더: `ex12/`만(D21, 복사 없음). 시작 HEAD `9a83c20`(origin보다 앞선 로컬 커밋 2개), 시작 때 `git status --short` 비어 있음(사용자 변경 없음)
- 커밋 · 푸시 · Vercel/GitHub 설정 · Actions 실행: 하지 않음. `web/` · `plan/PLAN_web.md`: 읽거나 고치지 않음. 새 npm 패키지 0
- 증거 · 검증 스크립트: `ex12/.data/o4-build-20261019/`(git 제외)

## 1. 작업 범위 · 바뀐 파일

| 파일 | 구분 | 내용 |
|---|---|---|
| `ex12/tools/pwa-images.tsx` | 새 | `next/og` ImageResponse로 PNG 6개를 그려 `public/`(또는 인수 폴더)에 씀. 글꼴 = `assets/fonts/NotoSansKR-{Regular,Bold}.otf`. 실행 중 `fetch`는 `data:`만 통과, 그 밖은 기록 · 던짐 → 하나라도 있으면 파일을 쓰지 않고 실패. 런타임 라우트 아님 |
| `ex12/public/icons/icon-192.png` · `icon-512.png` · `maskable-192.png` · `maskable-512.png` · `apple-touch-icon.png` · `ex12/public/og.png` | 새 | 위 도구로 한 번 생성(2절) |
| `ex12/public/manifest.webmanifest` | 새 | 칸 11개뿐(`id` `/` · name/short_name `이슈랭크` · description · `lang` ko · `start_url` `/app` · `scope` `/` · standalone · 색 2 · icons 4) |
| `ex12/public/sw.js` | 새 | 캐시 `issuerank-v1` 하나, install = `/offline.html`만 + skipWaiting, activate = 다른 캐시 전부 삭제 + claim, fetch = 같은 출처 · GET · `mode navigate` · `/api/` · `/_vercel/` 밖일 때만 `respondWith(네트워크 → 실패하면 /offline.html)`. `cache.put` 0 |
| `ex12/public/offline.html` | 새 | 바깥 파일 · http(s) 주소 0, noindex, Analytics 없음, 토큰 = widget.html, `이슈랭크` → `인터넷에 연결되어 있지 않습니다` → 안내 → `다시 시도`(새로 고침) |
| `ex12/next.config.mjs` | 고침 | rewrite `/app` · `/s` → `/index.html`, `headers()` 2(`/sw.js` 3개 · `/manifest.webmanifest` Content-Type). 기존 rewrite · include 그대로 |
| `ex12/public/index.html` | 고침 | head 태그(manifest · theme-color 2 · icon · apple-touch-icon · description · OG 10) · CSS 6줄 · 헤더 `앱 설치` 버튼 · 카드 `공유` 글 버튼 · 상세창 `공유`(아이콘 SVG는 템플릿 안에 직접) · 맨 아래 새 줄 · `shareDialog` · 새 구역 `Share · install (O4)`(Toast 뒤) · 클릭 분기 2줄 · `start();` 다음 줄 `registerServiceWorker();` |
| `ex12/public/widget.html` | 고침 | head(manifest · theme-color 2 · icon · apple-touch-icon · 자기 OG) · `<h2 id="app">앱으로 설치</h2>` 절(Android · iPhone) · Android 7번 새 문구(바로가기 만들기 문구 삭제) · `.app-steps` CSS 1줄 |
| `ex12/test/pwa.test.ts` | 새 | 11개: manifest · PNG 픽셀 · sw.js VM · offline/index/widget 원문 |
| `ex12/test/share-ui.test.ts` | 새 | 13개: 공유 ①~⑨ · 없는 id · 설치 3 · SW 등록 1 |

지운 것: 계획대로 바꾼 3줄뿐 — `widget.html` Android 7번 옛 문구(바로가기 만들기, Q7), `index.html` 맨 아래 한 줄은 같은 링크 그대로 새 줄 아래로(내용 변화 0), `next.config.mjs` `/card` 줄 끝 `];` → `},` 이어 쓰기. 기존 테스트 파일 수정 0.

## 2. 아이콘 · OG 그림(해시 · 크기)

`bash scripts/ex12-ubuntu.sh npx tsx tools/pwa-images.tsx`(→ `public/`) · 같은 명령에 `.data/o4-build-20261019/gen0` · `gen1` · `gen2` 세 번 더 → 네 번 모두 바이트 같음, 실행마다 `dataFetches 1`(내장 wasm) · `blocked 0`. 모두 PNG 8비트 RGBA(색 형식 6) · 비월 없음.

| 파일 | 크기(px) | 크기(B) | sha256 |
|---|---|---|---|
| `icons/icon-192.png` | 192×192 | 2,925 | `3a2a226d421fe82e3b987c28676024a31b3d03d68c09bd7007f03783ce8a4275` |
| `icons/icon-512.png` | 512×512 | 11,594 | `f8ca3c77df030be8d8a1b214407c8820294ee395036029fdd8bdb2a9c3502fb4` |
| `icons/maskable-192.png` | 192×192 | 1,670 | `8af397b371423d895649323cf8b8b311673a4929719e0c825d314020b8860778` |
| `icons/maskable-512.png` | 512×512 | 8,463 | `7e47b3de46387af65e52f041db133ff3b21c798ea770adecf8302b14e0661a6c` |
| `icons/apple-touch-icon.png` | 180×180 | 1,539 | `7f982c55f956a558f73d8fa34ea745b17dcd9271e8707e21f95069ff134c128b` |
| `og.png` | 1200×630 | 49,587 | `01920eefddc2977db003c0900b2d76216fa3cdd32f0dda98dd9be6bd74d61d60` |

- 마크 = O3 카드 40px 마크 비율(왼쪽 60% · 틈 5% · 오른쪽 35%, 오른쪽 위 60% · 아래 35%, 모서리 7.5%, 아래 칸 흰색 opacity .45)을 정수 픽셀로 반올림.
- 크기 선택(계획 밖 세부, 6절 9): any = 투명 바탕 + 온 면 #161616 타일(모서리 22%) + 흰 마크 한 변 50% / maskable = 온 면 #161616 + 마크 40%(꼭짓점이 중심에서 0.28W — 안전 원 0.4W 안) / apple-touch = 온 면 #161616 + 마크 50%.
- 눈으로 본 결과(직접 열어 봄): any = 둥근 검은 타일 가운데 흰 3칸 마크 · 오른쪽 아래 칸 회색, 모서리 투명 / maskable = 검은 정사각에 조금 작은 마크 · 가장자리 여백 넉넉 / apple = 검은 정사각에 마크 / og = 밝은 바탕 왼쪽 위 마크 + `이슈랭크`, 큰 글 `어제 가장 많이 보도된 뉴스`, 회색 `섹션별 순위 · 3줄 요약 · 원문 링크`, 섹션 색 칩 6개(`정치` `경제` `사회` `생활·문화` `세계` `IT·과학`, 흰 글자), 구분선 아래 `issuerank-kappa.vercel.app`. 깨진 글자(□) · 날짜 · 기사 제목 · 숫자 0. 칩 줄과 구분선 사이가 넓게 비어 있음(결정 후보 아님 — 카카오 잘림 모양은 17번에서 확인).

## 3. PLAN 항목별 반영 위치

| PLAN 항목 | 반영 |
|---|---|
| manifest 11칸 · 값 | `public/manifest.webmanifest`(키 순서 = 표). `prefer_related_applications` 없음 |
| 아이콘 · OG · 그림 만들기 | 2절 · `tools/pwa-images.tsx`(`IMAGES` 6개) |
| head 태그(index) | `<title>` 바로 아래 16줄. `og:url` · canonical · `apple-mobile-web-app-*` 없음. privacy · terms · card head 그대로(해시) |
| head 태그(widget) | manifest · theme-color 2 · icon · apple-touch-icon · OG 10(자기 title · description, alt = 자기 title) |
| 서비스 워커 · 기기에 저장 안 함 | `public/sw.js`. 등록은 index.html만: `registerServiceWorker()`(`'serviceWorker' in navigator`일 때 `load` 뒤 `register('/sw.js', { scope: '/', updateViaCache: 'none' })`, 실패는 `.catch(() => {})` — 문구 0) |
| 헤더(파일) | `next.config.mjs` `headers()` — 빌드 `routes-manifest.json`에 두 규칙 · 로컬 응답 9번 |
| 오프라인 화면 | `public/offline.html` |
| 업데이트 | `sw.js` 머리 주석(바뀌면 `issuerank-v2`로) · 11번에서 v2 정리 확인 |
| 설치 버튼 | 헤더 `#installBtn`(pill-btn · 아래 화살표 SVG · `.lbl` · aria-label `이슈랭크 앱 설치` · 처음 `hidden`), `beforeinstallprompt` → `preventDefault()` 후 보관(standalone이면 보관 안 함) → 누르면 `prompt()` 1번 · 숨김 · `appinstalled` → 숨김 + `앱을 설치했습니다` |
| 맨 아래 줄 | `<div class="foot-row"><button data-share-site>이슈랭크 공유</button><span id="installFoot"> · <a href="/widget#app" id="installLink">앱 설치</a></span></div>` + 기존 링크 그대로. 이벤트가 있으면 링크 이동을 막고 `prompt()`, 없으면 링크 그대로 `/widget#app`. standalone이면 `#installFoot` 숨김 |
| 위젯 안내 | `widget.html` 새 절(계획 문구 그대로, 메뉴 이름만 기존 단계처럼 굵게) · Android 7번 |
| 측정 경로 | rewrite `/app` · `/s`. `issueShortcut`은 그대로(경로 `/s`는 남고 쿼리만 지움 — 11번 `/s?issue=` 실측) |
| 공유 대상 · 버튼 | 상세창 `기사 원문 ↗` · `북마크` 뒤 `btn-ghost` `공유`(SVG 직접) · 카드 `기사 원문 ↗` 옆 `<button class="src" data-share aria-label="이 이슈 공유">공유</button>`(`.compact .src` 규칙으로 접기에서 숨김) · 맨 아래 `이슈랭크 공유`. 헤더 0 |
| 누르면 · 복사 실패 | `share()`: 클릭 처리기 안에서 바로(await 없음) `navigator.share`가 함수 · `canShare?.(값) !== false` · `matchMedia('(pointer: coarse)')`면 공유 창(AbortError 무시, 그 밖 실패 → 복사), 아니면 `copyShare()` → `clipboard.writeText(text + '\n' + url)` → 토스트 `링크를 복사했습니다`, 실패하면 `#shareDialog`(class `auth-dialog`, `링크 복사` · 안내 · 읽기 전용 글 상자 전체 선택 · `닫기` · Esc) |
| 보내는 값 | `SHARE_BASE` = `https://issuerank-kappa.vercel.app` 고정. 이슈 `{ title: '이슈랭크', text: '[섹션] 제목 원문', url: '…/s?issue={id}' }`(랭킹 → 없으면 북마크 카드), 사이트 `{ …, text: '어제 가장 많이 보도된 뉴스 — 섹션별 순위 · 3줄 요약', url: '…/s' }` |
| 테스트 | `test/pwa.test.ts` · `test/share-ui.test.ts` |

## 4. 테스트 · 빌드

- `bash scripts/ex12-ubuntu.sh npm test` → **22파일 219/219 통과**(O3 기준 195 + 새 24: pwa 11 · share-ui 13). 기존 테스트 21파일(setup 포함) 해시 같음 · vitest가 git 추적 파일을 바꾸지 않음. 마지막에 한 번 더 실행해도 219/219(`test-final.log`).
- `bash scripts/ex12-ubuntu.sh npm run build` → **성공**(Turbopack · TypeScript · `tsc --noEmit`, `tsc --listFilesOnly`에 `tools/pwa-images.tsx` · 새 테스트 2개 포함). 라우트 그대로(`ƒ /api/[...path]` · `/api/card` · `/api/card/caption`). 처음 빌드부터 통과, 마지막에 다시 실행해도 같음(`build1.log` · `build-final.log`).
- 해시(검증 1): 작업 전 기록 42개(`server/*.ts` 18 · `card-image.tsx` · `db/*.ts` 4 · `routes/*.ts` 4 · `app/api` 3 · privacy/terms/card.html · `widget.js` · `openapi.yaml` · package · lock · `vercel.json` · 글꼴 3 · `.github/workflows/daily.yml`) **모두 같음**(`sha256sum -c`, `hash-protected-before.txt`). `web/` 밖 추적 615파일 중 바뀐 것 = `next.config.mjs` · `index.html` · `widget.html` 3개뿐(`hash-all-tracked-before.txt`). index.html에서 기존 VM 테스트가 자르는 17구역 · `start()` 안 = HEAD와 글자 그대로 같음.

## 5. 검증 줄(PLAN 1~19)

| 줄 | 결과 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | OK | `git status` = 1절 목록뿐(+ 이 보고 · HANDOFF · PLAN.md 10절 메모 1줄), 해시 42개 같음, 새 패키지 · API · 스키마 0, ex01~ex11 · `web/` · SPEC 변경 0 |
| 2 manifest · 그림(단위) | OK | `pwa.test.ts`: 칸 11개 · 값 · prefer_related 0 · icons 4 파일 · IHDR = sizes · any 2 · maskable 2(합친 항목 0) · any 모서리 alpha 0 · maskable 0.4W 밖 전부 #161616 불투명(192 · 512) · apple 180×180 전부 불투명 · og 1200×630 · ≤ 5MB · 바탕 #f7f7f5 |
| 3 서비스 워커(VM) | OK | install = `open issuerank-v1` → `add /offline.html` → skipWaiting / activate = `issuerank-v0` · `other` 삭제 → claim · v1만 남음 / 탐색 `/` · `/app` · `/s?issue=x` · `/privacy`만 respondWith(성공 = 그 응답, 실패 = offline.html) / 콜백 · `/_vercel/` · accounts.google.com 탐색 · `/api/*` · jsdelivr · POST 2종 = respondWith 0, 그때 fetch · match · put 0 |
| 4 offline · head · OG(단위) | OK | offline: http(s) · 바깥 파일 · Analytics 0 · noindex · 본문 = 세 문구 + 버튼 · 스크립트 VM에서 `다시 시도` → `location.reload` 1 · 밝게/어둡게 토큰 두 덩이가 widget.html과 글자 그대로 같음 / index: 태그 · OG 10 = 표 · og:url · canonical · apple-mobile-web-app 0 · `\nstart();\nregisterServiceWorker();\n</script>` / widget: 자기 OG · `id="app"` 절 위치 · 문구 · Android 7번 · "바로가기 만들기" 0 |
| 5 공유(VM) | OK | `share-ui.test.ts` ①~⑨ + 없는 id(아무것도 안 함) · 클릭 분기 2줄 그대로 |
| 6 설치 · 등록(VM) | OK | 이벤트 → preventDefault 1 · 헤더 · 맨 아래 보임 → prompt 1 · 숨김(accepted · dismissed) · 다시 눌러도 0 / 맨 아래: 이벤트 있으면 이동 막고 prompt, 없으면 막지 않음(`/widget#app`) / appinstalled · standalone 2종 / register 1회 = 표 값(load 전 0) · API 없음 · 실패 → 예외 · 처리 안 된 rejection · 토스트 0 |
| 7 로컬 회귀 · 빌드 | OK | 4절 |
| 8 그림 다시 만들기 | OK | 2절(두 번 + 시험 1번 = 커밋 파일과 바이트 같음, `data:` 말고 fetch 0, 글꼴 3파일 해시 = 저장소) |
| 9 로컬 서버 정적 · 헤더 | OK 19/19 | Node fetch 18요청(`api.mjs`): manifest 200 `application/manifest+json` · 본문 = 파일 / sw.js 200 · `application/javascript; charset=utf-8` · `no-cache, no-store, must-revalidate` · CSP = 표 / offline 200 text/html · CSP 없음 / 그림 6개 200 `image/png` · 바이트 = 파일 / `/` · `/app` · `/s` · `/s?issue=2026-10-19-pol-1` 본문 = index.html · 헤더 서로 같음(`public, max-age=0`) · CSP 없음 / `/widget` = widget.html / 회귀: `/api/widget` · `/api/card/caption` `public, max-age=0, s-maxage=300` · `/api/rankings` Cache-Control 없음(로컬, O1 기록과 같음) · date 셋 다 2026-10-19 · `/privacy` = 파일 · 정적 html과 같은 헤더 · Set-Cookie 0 |
| 10 로컬 설치 가능 · 등록(CDP) | OK | 새 프로필 360×800 `/`: SW scope `http://localhost:3000/` · active `activated` · script `/sw.js` / `Page.getAppManifest` url = `/manifest.webmanifest` · errors 0 · 받은 본문 = 파일(`parsed`는 scope만 돌려줌 — 6절 5) / 새로 고침 뒤 controller = `/sw.js` / **`Page.getInstallabilityErrors` = `[]`**(이 기기 Chromium 155 headless가 답함) / 진짜 `beforeinstallprompt`도 옴(12번) / JS 예외 0 |
| 11 로컬 캐시 · 오프라인 · 업데이트(CDP) | OK | 같은 프로필: `#list` · 상세창 · `#bookmarks`(비로그인, 페이지 fetch `/api/bookmarks` 401) · `/widget` · `/s?issue=…`(그 이슈 상세 · 경로 `/s` · 쿼리 지움) 뒤 Cache Storage = `[['issuerank-v1', ['…/offline.html']]]` · `/api/*` 응답 fromServiceWorker 전부 false(탐색은 true) / **내 server.mjs만 정지 파일로 끔** → `/app` · `/s?issue=…` · `/privacy` · `/` 네 곳 모두 오프라인 안내(문구 · 버튼 · 주소 그대로 · 가로 스크롤 0) · 페이지 `fetch('/api/rankings')` = TypeError(캐시에서 안 나옴) · 다크 #121211 / 서버 다시 켜고 `다시 시도` → 이슈맵 / sw.js 본문 `issuerank-v1` → `v2` 업데이트 → v2 활성 · 캐시 키 `issuerank-v2`만 · 항목 offline.html · 새로 고침 정상. **바꾼 방법은 계획(CDP Fetch)과 다름 — 6절 1** |
| 12 로컬 화면 · 공유 · 설치 UI(CDP) | OK(33/34행, 1행은 검사 가정 오류 — 6절 2) | 새 프로필(`ui-cdp.mjs`): **1280 마우스 · share 없음(이 Chromium 기본: `share`/`canShare` 없음)** 상세창 · 목록 카드 · 맨 아래 `공유` → 클립보드 = 복사 글 그대로 · 토스트 `링크를 복사했습니다` / 카드 버튼 = 원문 링크 옆 · aria-label · 요약 접기에서 숨김 / 복사 거절 → `링크 복사` 창 · 글 상자 = 복사 글 · 전체 선택 · 초점 · 토스트 0 · Esc 닫힘 / 가짜 `beforeinstallprompt` → preventDefault · 1280 `앱 설치`(글자) 맨 앞 → 누름 prompt 1 · 숨김 → 다시 이벤트 → 맨 아래 `앱 설치` prompt · 이동 0 → `appinstalled` 토스트 `앱을 설치했습니다` / **360 가짜 share + coarse(문서 전 주입)** 상세창 · 카드 · 맨 아래 → 누른 같은 틱 share 1 · 값 = 표 · 토스트 · 클립보드 쓰기 0 / 360 상세창 버튼 3개 한 줄 · 창 안 / 360 설치 아이콘만(40px) / 세 화면 가로 스크롤 0(360 · 1280) / 다크 #121211 · theme-color 다크 줄 일치 / D20 768 · 1280 박스 · 타일 30개 style 좌표 = HEAD index.html(CDP Fetch로 넣음)과 같음 / 회귀(새 탭 360): O1 13① `/?issue=2026-10-19-pol-1` 상세창 제목 · 요약 · 88건 = API · 원문 `target=_blank` `rel=noopener` · 쿼리 지움 · `history.length` 1 · ⑥ 비로그인 북마크 → 로그인 안내 · POST 0 · ⑦ 랭킹 탭 → 뒤로 → 앞으로 · `#bookmarks` 안내 · O2 로그인 창(Google 버튼) 열림 · 닫힘 / 맨 아래 `앱 설치`(이벤트 없음) → `/widget#app` 절 위 · 문구 · 가로 스크롤 0 · 다크 / `/offline.html` 360 가로 스크롤 0 / JS 예외 0 · 바깥 = jsdelivr 글꼴 CSS 시도뿐(막힘) · kakaocdn · accounts.google.com 0 |
| 13 안전 · 한도(로컬) | OK | 3000 누계 **81/100**(차단 0) · newsdata · OpenAI · Kakao · Google 0 · 비밀값 검사(ex12/.env 8값) 서버 로그 2 · 결과 JSON · 빌드/테스트 로그 모두 false · DB 9표 건수 · 내용 md5 전후 같음(읽기 전용, 임시 계정 0) · 소유 PID 0 · code-oss 살아 있음(9절) |
| 14 배포 정적 · 헤더 | SKIP — 푸시 후 | 8절 1 |
| 15 배포 설치 가능 · 오프라인 | SKIP — 푸시 후 | 8절 2 |
| 16 Android 실제 | SKIP — 사용자 확인 | 8절 3 |
| 17 카카오톡 공유 · 미리보기 | SKIP — 사용자 확인 | 8절 4 |
| 18 iPhone | SKIP — iPhone 기기 없음 | 8절 5 |
| 19 Analytics 경로 | 기록만(판정 제외) | 8절 6 |

**합계: OK 13 · FAIL 0 · SKIP 5(14 · 15 푸시 후, 16 · 17 사용자, 18 iPhone) + 19 기록만.** 배포 · 사용자 줄 전이라 PASS 아님.

## 6. 꼭 필요했던 내부 세부 · PLAN과 다른 점(보고)

1. **v2 업데이트 바꾸기(11번) — CDP Fetch가 못 잡음**: 브라우저 대상(`Target.attachToBrowserTarget` → `Fetch.enable`)과 페이지 세션 둘 다 `*/sw.js*` Response 단계로 켰지만, `registration.update()`의 sw.js 요청은 이 기기 Chromium 155에서 **가로챔 0건**(서버는 그대로 v1 → 업데이트 없음). 미리 넣어 둔 예비대로 세는 프록시(`server.mjs`)가 `sw-v2` 표시 파일이 있을 때만 `/sw.js` 본문의 `const CACHE = 'issuerank-v1';` 한 곳을 `v2`로 바꿔 줌(요청 로그 `rewritten` 1건) → 다시 `update()` → v2 설치 · 활성 · 옛 캐시 삭제 확인 후 표시 파일 지움. 제품 파일은 바꾸지 않음. 심사도 같은 방법이 필요할 수 있음.
2. **headless가 진짜 `beforeinstallprompt`를 보냄**: 12번 첫 행 "1280 처음: 헤더 앱 설치 숨김(이벤트 전)"은 FAIL로 찍혔지만, 그때 헤더 버튼이 이미 보인 이유는 새 판 로드 뒤 Chromium이 보낸 진짜 이벤트 1번(문서 전 주입한 세기 = 4 = 진짜 1 + 내가 보낸 가짜 3, 가짜를 보내기 전 상태에서 버튼이 보임) → 우리 처리기가 계획대로 보관 · 표시한 것. 제품 결함이 아니라 검사 가정이 틀린 것이라 행은 FAIL로 남기고 줄은 OK로 판정. "이벤트 전 숨김"은 VM(6번)으로 확인. 진짜 이벤트가 왔다는 것 = fetch 처리가 있는 우리 sw로 이 Chromium이 설치 가능하다고 본 근거(계획 "확인 필요" 한 줄).
3. **`/_vercel/*`를 브라우저에서 막음**(`Network.setBlockedURLs`): 로컬은 404뿐이라 측정 요청 한도를 아끼려고. 콘솔 오류 줄만 남고 JS 예외 0. 새 탭 첫 로드에서 막기 전에 1번(404) 나감.
4. Chrome은 페이지를 열 때마다 `manifest.webmanifest`와 파비콘(`icons/icon-192.png`)을 받음(304 포함) → 페이지당 로컬 요청 2개 더. 오프라인 안내 화면에는 아이콘 링크가 없어 `/favicon.ico` 요청이 연결 거부로 콘솔에 남음(제품 동작 영향 0, 고치지 않음).
5. `Page.getAppManifest`의 `parsed`는 이 버전에서 `scope`만 돌려줌 → 받은 본문(`data`) = 파일 · 오류 0으로 대조.
6. 바깥 연결 기록기(O3 `monitor.mjs` 그대로): 두 서버 실행 모두 `loaded`만 기록(fetch · http(s) · net.connect · tls.connect 0). postgres.js는 `new net.Socket().connect()`를 써서 DB 연결이 이 기록기에 안 잡힘(O3와 같은 한계) — DB는 읽기 전용 스냅숏 해시로 쓰기 0 확인.
7. 오프라인 측정 = 내 서버만 끔: 정지 파일 → server.mjs가 자기 next 자식 프로세스 그룹만 끝냄 → 3000 · 3001 연결 거부. 다시 켤 때는 같은 래퍼를 새로 띄움(9절 PID).
8. 새 문구를 만들지 않으려고: 복사 창 글 상자는 `aria-labelledby` = 안내 문단, offline.html `<title>` = `이슈랭크`. widget 새 절의 메뉴 이름(설치 아이콘 · 앱 설치 · 설치 · 더보기(⋮) · 홈 화면에 추가 · 공유(□↑) › 홈 화면에 추가 › 추가)은 기존 단계 꼴대로 굵게, 문구는 계획 그대로.
9. 아이콘 마크 크기(any 50% · maskable 40% · apple 50%)는 계획에 숫자가 없어 정함 — maskable은 꼭짓점이 0.28W라 안전 원(0.4W) 안이고 런처가 더 깎아도(안드로이드 적응형 아이콘은 약 66%) 마크가 남음. 16번 실기기에서 크기 인상만 확인 부탁.
10. 그 밖: `e.prompt().catch(() => {})`(두 번 부르면 rejection) · 카드 `공유` 간격 `.src + .src { margin-left: 14px }` · 맨 아래 버튼 색 = 기존 링크(`--muted`) · 360 헤더에 설치 아이콘이 생기면 가운데 전환 버튼이 왼쪽으로 조금 옮겨감(가로 넘침 0, 캡처 `ui-360-install.png`).

## 7. 화면 — 눈으로 본 결과

캡처(`ex12/.data/o4-build-20261019/`): `pwa-360-first` · `pwa-360-offline` · `pwa-360-offline-dark` · `ui-1280-drawer` · `ui-1280-install` · `ui-1280-copy-dialog` · `ui-360-drawer` · `ui-360-drawer-dark` · `ui-360-list` · `ui-360-footer` · `ui-360-install` · `ui-360-dark` · `ui-360-login` · `ui-360-widget-app` · `ui-360-widget-dark` · `ui-360-offline`.

- 360 상세창 아래 줄 `기사 원문 ↗ · 북마크 · 공유`(상자 아이콘) 한 줄, 다크에서도 같음. 1280 상세창 오른쪽 아래 같은 세 버튼.
- 카드: `기사 원문 ↗   공유`(같은 작은 회색 글). 맨 아래: `이슈랭크 공유 · 앱 설치` / `개인정보처리방침 · 이용약관 · 문의`.
- 1280 헤더 오른쪽 `↓ 앱 설치` · `북마크` · `로그인`, 360은 `↓` 아이콘 · 북마크 아이콘.
- 복사 창: 밝은 로그인 창 꼴 `링크 복사` · 안내 · 파랗게 전체 선택된 두 줄 글(사이트 글이 길어 3줄로 감김) · `닫기`.
- 오프라인: 마크 + `이슈랭크`(회색) · 굵은 제목 · 회색 안내 · 검은 `다시 시도`(다크는 흰 버튼).
- `/widget#app`: 맨 위가 `앱으로 설치` 절(Android(Chrome) · iPhone(Safari) 두 점), 아래 기존 iPhone 위젯 1~6.

## 8. SKIP 줄 절차(사용자 · 다음 역할)

1. **14 배포 정적 · 헤더(푸시 · Vercel 재배포 뒤, 배포 요청 ≤ 15)**: Node fetch `https://issuerank-kappa.vercel.app` — `/manifest.webmanifest`(200 · `application/manifest+json` · 본문 = 파일) · `/sw.js`(200 · 헤더 3개 = 9번 표) · `/offline.html` · `/icons/*` 5 · `/og.png`(200 `image/png` · 1200×630 · 바이트 = 파일) · `/api/widget` 1(첫 id) · `/app` · `/s` · `/s?issue={id}` · `/`(본문 = index.html, head `og:image` = 절대 주소). 정적 파일 Cache-Control은 Vercel 기본값이 다를 수 있음 → 값 기록만. **manifest Content-Type이나 sw.js 헤더가 없으면 멈추고 보고**(Vercel이 `headers()`를 정적 파일에 붙이는지 = 확인 필요 줄).
2. **15 배포 설치 가능 · 오프라인(CDP, 배포 요청 ≤ 25)**: 새 프로필 360×800 배포 `/` → 10번(getAppManifest · getInstallabilityErrors `[]` · SW activated · 새로 고침 뒤 controller) + 상세창 공유 1번(가짜 share — url = 배포 주소) → 같은 프로필을 `--host-resolver-rules="MAP issuerank-kappa.vercel.app 127.0.0.1:9"`로 다시 띄워 `/` · `/app` → 오프라인 안내 → 규칙 없이 다시 → 평소 화면. 그 밖 이름 풀이는 막아 둘 것(jsdelivr 등). v2 검증이 다시 필요하면 6절 1처럼 CDP Fetch가 sw.js 업데이트 요청을 못 잡을 수 있음.
3. **16 Android 실제(사용자)**: Android Chrome으로 `https://issuerank-kappa.vercel.app` → ① 30초쯤 둘러본 뒤 헤더 설치 아이콘(또는 ⋮ 메뉴 — **실제 메뉴 이름 기록**) → 설치 ② 홈 화면 아이콘(마스크 잘림 없는지) · 이름 `이슈랭크` · 앱 목록 · 열면 주소창 없음 · 상태 표시줄 색(밝게 #f7f7f5 / 어둡게 #121211) ③ 앱 안 랭킹 · 상세창 · Chrome 로그인 상태가 이어지는지 · Google 로그인 1번 ④ 비행기 모드로 앱 열기 → 오프라인 안내 → 연결 → `다시 시도` ⑤ `/widget#app` 안내가 실제 메뉴와 맞는지. 결과 · 캡처 전달. 메뉴 이름이 다르면 안내 문구만 고침.
4. **17 카카오톡(사용자)**: ① Android 상세창 `공유` → 공유 창의 카카오톡 → 나와의 채팅 ② 맨 아래 `이슈랭크 공유` → 같은 곳 ③ 받은 링크 누르기(카카오톡 안 브라우저) → 그 이슈 상세 · 비로그인 북마크 안내 · (선택) Google 로그인 ④ PC Chrome에서 `공유` → `링크를 복사했습니다` → 카카오톡 PC에 붙여 넣기 ⑤ 옛 미리보기면 카카오 공유 디버거에서 그 주소 캐시 초기화 뒤 다시. 기대: 메시지 `[섹션] 제목` + `…/s?issue=…` · 미리보기 = 이슈랭크 · 제목 · 설명 · og.png(잘림 모양 기록) · 사이트 공유 = `/s` 첫 화면 · 앱 안 Google 로그인 결과 기록(`disallowed_useragent`면 별도 결정, FAIL 아님).
5. **18 iPhone**: 기기가 생기면 Safari 공유 › 홈 화면에 추가 · 아이콘 · standalone · 오프라인 · 공유 창의 카카오톡 · 로그인이 따로인지.
6. **19 Analytics(기록만)**: 설치 · 공유 며칠 뒤 Vercel › Analytics › Pages에서 `/app` · `/s`가 따로 보이는지.

## 9. 요청 · 비용 · 프로세스

- 로컬 3000: **81/100**(9번 Node 18 · 10–11번 브라우저 39(sw.js 4 = 등록 · 업데이트 확인 2 · 예비 바꿈 1 포함) · 12번 브라우저 23 · 서버 정지 확인 curl 1), 3001 직접 0, 차단 0. 바깥: newsdata 0 · OpenAI 0 · Kakao 0 · Google 0 · npm/GitHub 내려받기 0. 브라우저 바깥 요청은 기존 index.html의 jsdelivr Pretendard CSS 시도뿐(7회, `MAP * ~NOTFOUND`로 이름 풀이 막힘 → `ERR_NAME_NOT_RESOLVED`).
- DB: 읽기 전용 스냅숏 전후 9표 같음(articles 1105 · issues 754 · collect_runs 96 · bookmarks 2 · folders 2 · user 2 · session 1 · account 2 · verification 0). 임시 계정 0.
- 프로세스: 시작 전 목록 저장(`ps-before.txt`, 사용자 code-oss = 30854). 내가 띄운 것 = 서버 1(proot 27276 → server.mjs 27287 → next-server 27311) · 서버 2(래퍼 32611 → server.mjs 32617 → next-server 32626) · Chromium A(32262, 트리 12개) · Chromium B(8147, 트리 13개). 서버는 정지 파일로(두 번 모두 next 종료 확인), Chromium은 내 프로세스 그룹 SIGTERM 뒤 남은 0. 끝 확인: curl 3000 · 3001 · 9363 · 9364 · 9365 = 000, `ps`에 next-server · next start · server.mjs · 내 chromium 0, 시작 뒤 새로 생긴 프로세스 = 사용자 code-oss의 zygote 자식(12885 ← 30857 ← 30854, 건드리지 않음)과 확인 셸뿐, code-oss 30854 살아 있음. Chromium 프로필 2개 삭제 확인. 넓은 패턴 kill 0.

## 10. 필요했지만 없던 키 · 계정

없음(공유 · 설치는 계정 · 키 · 환경 변수 0, Kakao SDK 없음).

## 11. 사용자 할 일

1. 이 제작 확인 → 독립 심사(새 세션, `agent_prompts/3_judge_agent.md`, 로컬 3000 ≤ 100 · Kakao · Google 0) 또는 바로 커밋 · 푸시 승인. 커밋 대상 = 1절 ex12 파일(바뀐 3 + 새 13: `tools/pwa-images.tsx` · `public/manifest.webmanifest` · `public/sw.js` · `public/offline.html` · `public/og.png` · `public/icons/*.png` 5 · `test/pwa.test.ts` · `test/share-ui.test.ts`) + 이 보고 · HANDOFF · PLAN.md 10절 메모.
2. 푸시 뒤 배포 줄 14 · 15(8절 1 · 2, 역할이 실행).
3. Android 16 · 카카오톡 17 결과 · 캡처 전달(8절 3 · 4), (선택) 며칠 뒤 19.
4. 알아 둘 것: 배포되면 방문자 기기에 sw가 설치됨 → 앞으로 `sw.js` · `offline.html`을 바꾸는 배포는 `issuerank-v2`로 올려야 옛 캐시가 지워짐. 문제가 생기면 같은 주소 `/sw.js`를 fetch 처리 없는 판으로 바꿔 배포(계획 "업데이트" 줄).

## 12. 미해결 · 추가로 보인 점

- Vercel이 `.webmanifest` Content-Type · `headers()`를 정적 파일에 붙이는지(14), HTTPS 배포에서 설치 오류 `[]`(15), 실기기 설치 · 아이콘 마스크 · 오프라인 · 앱 안 로그인(16), 공유 창의 카카오톡 · PC 공유 · 미리보기 캐시 · 1200×630 잘림(17), iPhone(18).
- 이 기기 Chromium에서는 CDP Fetch로 SW 업데이트 스크립트 요청을 가로챌 수 없었음(6절 1) — 심사 · 배포 줄에서 v2 확인이 필요하면 다른 방법이 필요.
- (기존 동작, 고치지 않음) 640px 이하에서 `.brand span { display: none }`이 글자뿐 아니라 마크(`<span class="brand-mark">`)까지 숨겨 휴대폰 헤더에 브랜드가 안 보임. O4 범위 밖 — 바꿀지는 사용자 결정.
