# ops O4 PWA(홈 화면 앱) + 공유 — 독립 심사 보고

- 날짜: 2026-10-19(KST) 16:38~17:25 · 역할: 심사(`agent_prompts/3_judge_agent.md`), 제작자와 다른 세션. 제품 코드 · 테스트 수정 0 · 제품 파일 임시 교체 0
- 대상: `ex12/`(운영 폴더, D21)의 미커밋 변경, 기준 HEAD `9a83c20`. 기준 문서: `plan/PLAN_ops_O4_pwa_share.md`(확정), SPEC US-13 · AC-13-1~4 · 머리말 회귀 AC(04-2 · 05-2 · 06-3 · 07-3 · 09-2 · 11-2 · 범위 밖 2), PLAN.md D10 · D21~D29(공유 주소 `/s?issue=<id>`) · 6절 · AGENTS.md
- 제작 보고 `reports/ops_O4_pwa_share_build.md`는 주장으로만 봄. 판정 근거는 모두 새로 쓴 심사 스크립트(`ex12/.data/judge-o4-20261019/`, git 제외 — `judge-server.mjs` 3000 세는 프록시 + next start 3001 · `monitor.mjs` 바깥 연결 기록 · `api.mjs` · `cdp.mjs` · `pwa-a.mjs` · `ui-b.mjs` · `diag-c.mjs` · `diag-d.mjs` · `static.mjs` · `vm-share.mjs` · `db.mjs` · `leak.mjs`). 제작 스크립트(`.data/o4-build-20261019/`)는 Chromium 실행 플래그만 참고, 실행하지 않음
- 커밋 · 푸시 · stash · reset · Vercel/GitHub 설정 · Actions: 하지 않음. `web/` · `plan/PLAN_web.md`: 읽거나 고치지 않음(`git status`만)

## 판정

**PASS 보류** — 로컬 줄 1~13 OK, 배포 줄 14 · 15 OK(푸시 `8aaf6bd`, 2026-10-19 18:01~18:05 KST — 아래 "배포 줄 14 · 15" 절), 위반 0. 남은 것 = 16 · 17(사용자 확인) · 18(iPhone 기기 없음) · 19(기록만).
**합계: OK 15 · FAIL 0 · SKIP 3 + 19 기록만**(로컬 심사 때 OK 13 · SKIP 5 → 배포 줄 2개 OK).
**커밋 전에 고쳐야 할 것: 없음**(7절은 모두 낮음 · 참고).

## 1. PLAN 항목별

| PLAN 항목 | 판정 | 직접 확인한 근거 |
|---|---|---|
| manifest(11칸) | done | 키 11개 · 값 = 표 글자 그대로(`id` `/` · `start_url` `/app` · `scope` `/` · standalone · 색 2 · icons 4), `prefer_related_applications` · shortcuts · share_target · screenshots 0. 서버 응답 = 파일 · `application/manifest+json`, CDP `Page.getAppManifest` 받은 본문 = 파일 · 오류 0 |
| 아이콘 5 | done | 내 PNG 해독기(서명 · IHDR · IDAT 풀기 · 필터 되돌리기): any 192/512 모서리 alpha 0 · 타일 #161616 · 흰 마크 / maskable 192/512 전부 불투명 · 중심 0.4W 밖 픽셀 = 모두 #161616 · 마크(검지 않은 픽셀) 최대 반지름 0.276W · 0.273W(안전 원 0.4W 안) / apple-touch 180×180 전부 불투명. 직접 열어 봄(둥근 검은 타일 + 흰 3칸 마크, 아래 칸 회색) |
| OG 그림 | done | 1200×630 · 전부 불투명 · 바탕 #f7f7f5 · 49,587 B(≤ 5MB). 직접 봄: 마크 + `이슈랭크` · `어제 가장 많이 보도된 뉴스` · `섹션별 순위 · 3줄 요약 · 원문 링크` · 칩 6(정치 · 경제 · 사회 · 생활·문화 · 세계 · IT·과학, 색 = index 밝은 `--s-*`) · `issuerank-kappa.vercel.app`, 날짜 · 기사 제목 · 숫자 0, 깨진 글자 0 |
| 그림 만들기 | done | `tools/pwa-images.tsx`를 래퍼 `npx --offline tsx`로 두 번(출력 `.data/judge-o4-20261019/gen1` · `gen2`, 끝나고 지움) → 6개 모두 커밋 파일과 `cmp` 같음. 내 기록기: `fetch data:` 1(내장 wasm) · tsx 자체 유닉스 파이프 1뿐, DNS · TCP 0. 글꼴 3파일 sha256 = O3 값. 런타임 라우트 0(빌드 라우트 = HEAD 3개) |
| head 태그 | done | HEAD 대비 index에 더한 태그 16개 = 표 순서 · 값 그대로(`<title>` 바로 아래, 글꼴 CSS 앞), widget 15개(자기 OG), 지운 태그 0, og:url · canonical · `apple-mobile-web-app-*` 0, og:image 절대 주소. privacy · terms · card = HEAD 바이트 같음 |
| 서비스 워커 | done | 내 VM 하네스 30경우 × 네트워크 성공/실패(아래 5절 3) + 실제 Chromium: 등록 scope `http://localhost:3000/` · activated · `updateViaCache` none, 등록은 `start();` 다음 줄 · load 뒤 |
| 기기에 저장 안 함 | done | Cache Storage = `[["issuerank-v1",["/offline.html"]]]`(여러 화면 · 로그인 뒤에도 같음), 페이지 `/api/*` 응답 fromServiceWorker 전부 false, sw.js에 `cache.put` 0 |
| 헤더(파일) | done(로컬) | 빌드 `routes-manifest.json` headers = 두 경로뿐 · 로컬 응답 3개 헤더 = 표. Vercel 적용은 줄 14 |
| 오프라인 화면 | done | 바깥 자원 · http(s) · Analytics 0 · noindex · 세 문구 · `다시 시도` = 새로 고침, 밝게/어둡게 실제 화면(5절 11) |
| 업데이트 | done | 내 프록시가 `/sw.js` 본문 한 곳만 v2로 → update · controllerchange · 캐시 = v2만(5절 11). 되돌림(v2 → v1)도 옛 캐시 삭제 확인 |
| 설치 버튼 | done | 진짜 이벤트를 capture에서 막으면 숨김 → 가짜 이벤트 → 보임 · prompt 1 · 숨김 · appinstalled 토스트(5절 12) + standalone 2종 VM |
| 맨 아래 줄 | done | `이슈랭크 공유 · 앱 설치` 줄이 기존 정책 줄 위, 이벤트 있음 = prompt(이동 0) · 없음 = `/widget#app` 링크 기본 동작 |
| 위젯 안내 | done | `<h2 id="app">` = 첫 h2 · iPhone 절 위, 문구 = 계획 글자 그대로(test + 원문 대조), Android 7번 새 문구, "바로가기 만들기" 0, 스크립트 칸 · iPhone 1~6 = HEAD |
| 측정 경로 | done(로컬) | rewrite `/app` · `/s` 본문 = index.html, `/s?issue=` 처리 뒤 경로 `/s` 남고 쿼리만 지움. Analytics 보기는 줄 19 |
| 공유 대상 · 버튼 | done | 상세창 `기사 원문 · 북마크 · 공유`(btn-ghost + SVG 직접), 카드 `기사 원문 ↗` 옆 `공유`(aria-label `이 이슈 공유`, 요약 접기에서 숨김), 맨 아래 `이슈랭크 공유`, 헤더 0 |
| 누르면 · 복사 실패 | done | Web Share(누른 같은 클릭 안 · 사용자 동작 유지) / AbortError 무시 / 그 밖 실패 · canShare 거짓 · 마우스 · share 없음 = 복사 / 복사 실패 · 클립보드 없음 = 복사 창(5절 12) |
| OG(미리보기) · 카카오 · 저작권 | done(로컬) | 공통 정적 OG · 보내는 값에 요약 · 원문 URL · 연관기사 수 · 추적 파라미터 0. 실제 카카오 미리보기는 줄 17 |
| 보내는 값 표 | done | 실제 브라우저에서 받은 값이 표와 JSON 같음: 이슈 `{ title: '이슈랭크', text: '[정치] {제목 원문}', url: 'https://issuerank-kappa.vercel.app/s?issue=2026-10-19-pol-1' }` · 사이트 `{ …, text: '어제 가장 많이 보도된 뉴스 — 섹션별 순위 · 3줄 요약', url: '…/s' }` · 복사 글 = text + `\n` + url |
| 하지 않음(범위 밖) | done | 푸시 · 알림 · Kakao SDK · `/s/<id>` · shortcuts/share_target · 위젯 · 로그인 · 카드 · 하루 실행 · DB · openapi · 정책 문안 · 새 패키지 변경 0(파일 해시 · grep) |

PLAN과 다른 점(판정 영향 없음): 줄 11의 v2 바꾸기를 CDP Fetch 대신 내 세는 프록시로 함 — CDP Fetch가 서비스 워커 스크립트 요청을 못 잡는 것을 5절 11에서 다시 확인.

## 2. AC

| AC | 판정 | 근거 |
|---|---|---|
| AC-13-1 설치 | 충족(로컬) · 실기기 줄 16 | 설치 오류 `[]` · manifest 오류 0 · 진짜 `beforeinstallprompt` 도착 · SW activated · start_url `/app`. 홈 화면 아이콘 · 주소창 없는 창은 Android 실기기(16) |
| AC-13-2 오프라인 | 충족(로컬) · 실기기 줄 16 | 내 서버만 끈 동안 `/privacy` · `/app` · `/s?issue=` · `/` 모두 안내 + `다시 시도` → 켜고 누르면 평소 화면, 랭킹 fetch 실패(캐시 0), 로그인 · 북마크 · 세션 응답 캐시 0 |
| AC-13-3 공유 | 충족(로컬) · 카카오톡 줄 17 | 휴대폰(coarse) = 공유 창 값 = 표, 그 밖 = 복사, `/s?issue=<id>` 열면 그 이슈 상세(제목 · 요약 · 연관기사 수 = API), 본문 · 요약 · 연관기사 수 전송 0 |
| AC-13-4 미리보기 | 충족(로컬 head · 그림) · 배포 14 · 카카오톡 17 | og:site_name · title · description · image(절대 주소, 1200×630). 실제 스크랩 미리보기는 사용자 줄 |
| AC-04-2 · 11-2(회귀) | 충족 | `/?issue=2026-10-19-pol-1` 새 탭 → 상세창 제목 · 요약 3줄 · 88건 = API, 원문 `target=_blank` `rel=noopener`, 쿼리 지움 |
| AC-05-2(회귀) | 충족 | 360 이슈맵 · 목록 · 북마크 · `/widget#app` · 오프라인 화면 가로 스크롤 0, 이슈맵 세로 쌓기 좌표 = HEAD |
| AC-06-3(회귀) | 참고 · 로컬 경로 충족 | 가짜 클라이언트로 시작 → Google 이동 시도(이름 풀이 막힘) · redirect_uri = 로컬 콜백 · 상태 쿠키 HttpOnly, 틀린 state 콜백 302(SW 밖) → 돌아온 `/` 문구. 실제 Google 승인은 사용자 몫(O2) |
| AC-07-3(회귀) | 충족 | 비로그인 북마크 → `북마크하려면 로그인이 필요합니다` + 로그인, POST 0 |
| AC-09-2(회귀, 6절 09-4 포함) | 충족(서버 요청 0 하네스) | 이전 날짜 → 날짜 표시 · 오늘 버튼 → 오늘, 최신 날짜에서 다음 날짜 막힘 · max = 오늘 · 미래 날짜 입력 → 오늘(5절 12 끝) |
| 범위 밖 "알림/구독" · "본문 재게시" | 충족 | Notification · PushManager · subscribe 0, 보내는 글 = 섹션 · 제목 · 주소뿐 |

## 3. 범위 · 불변 · PLAN 밖

- `git status --short --untracked-files=all`: 바뀐 것 = `ex12/next.config.mjs` · `ex12/public/index.html` · `ex12/public/widget.html` + 새 `ex12/tools/pwa-images.tsx` · `ex12/public/{manifest.webmanifest, sw.js, offline.html, og.png}` · `ex12/public/icons/*.png` 5 · `ex12/test/{pwa,share-ui}.test.ts` + 문서 `HANDOFF.md` · `PLAN.md`(10절 1줄) · `reports/ops_O4_pwa_share_build.md`. 심사 시작 · 끝 `git status` 해시 같음, 위 18파일 sha256 시작 = 끝(심사 중 제품 파일 변화 0).
- **HEAD의 추적 파일 615개(`web/` · `plan/PLAN_web.md` 제외) 전부를 `git hash-object`로 HEAD blob과 대조** → 다른 것 = 위 5개뿐. 그래서 보호 파일(`server/**` · `app/api/**` · privacy · terms · card.html · widget.js · openapi.yaml · package.json · package-lock.json · vercel.json · 글꼴 3 · `.github/workflows/daily.yml` · 기존 테스트 21파일) = HEAD, ex01~ex11 · frontend · SPEC 변경 0, 새 패키지 0. `web/` · `plan/PLAN_web.md`는 `git status` 변경 0.
- 줄 단위: index.html에서 HEAD 줄 중 사라진 것 = 맨 아래 footer 한 줄(같은 링크를 새 줄 아래로 옮김)뿐, widget.html = Android 7번 옛 문구 1줄뿐, next.config.mjs = `/card` 줄 끝 `];` → `},` 1줄뿐(include 그대로).
- 기존 VM 테스트가 자르는 index.html 구역 17개(`issueShortcut()`~`\nstart();` · API · loadRanking · 인증 블록 · renderBookmarks 등) = HEAD 글자 그대로, `start()` 안 = HEAD, 그 뒤 `registerServiceWorker();` 한 줄. 바뀐 구역은 계획대로 Events(+2줄 `[data-share]` · `[data-share-site]`, 지운 줄 0) · openDrawer(+1줄 공유 버튼, SVG 직접, 새 상수 0)뿐.
- PLAN 밖 추가: 없음(작은 CSS 규칙 `.src + .src` · `.site-foot button` · `.share-note` · `.share-text` · `.app-steps`, `e.prompt().catch` — 계획 화면을 그리는 데 필요한 내부 세부).
- 화면에 이미 있던 동작(PLAN.md 6절): 04-2 · 05-2 · 09-2 · 09-4 깨짐 0(2절), 10-3 폴더 칩은 이번 변경 구역 밖(renderBookmarks = HEAD).
- 제작 보고 12절 `.brand span`(640px 이하 마크까지 숨김): **HEAD에도 같은 기존 동작** — HEAD 원문 `@media (max-width: 640px) { … .brand span { display: none; } }` + `<span class="brand-mark">`, 실제 360에서 HEAD 판 · 지금 판 둘 다 `.brand-mark` display none. O4 회귀 아님.

## 4. 테스트 · 빌드

- `bash scripts/ex12-ubuntu.sh npm test` → **22파일 219/219 통과**(49초). vitest가 추적 파일을 바꾸지 않음(`git status` 해시 같음 — checkout 필요 0).
- `rm -rf ex12/.next` 뒤 `bash scripts/ex12-ubuntu.sh npm run build` → **성공**(Turbopack 14.5초 · TypeScript · `tsc --noEmit`), 라우트 = `ƒ /api/[...path]` · `ƒ /api/card` · `ƒ /api/card/caption`(HEAD와 같음). `tsc --listFilesOnly`에 `tools/pwa-images.tsx` · 새 테스트 2개 포함. rewrites 7(기존 5 + `/app` · `/s`) · headers 2.

## 5. 검증 19줄

로컬 서버: 깨끗한 빌드 뒤 proot `next start -p 3001 --hostname 127.0.0.1`(+ `NODE_OPTIONS=--import` 바깥 연결 기록기) + 3000 세는 프록시(심사 카운터 0에서 시작, 100번째 요청 뒤 포트를 닫아 넘을 수 없게). 로그인 경로 확인을 위해 서버 환경에만 가짜 `GOOGLE_CLIENT_ID/SECRET`(값 = `judge-o4-fake…`, 파일 변경 0). 브라우저 = Termux Chromium 155 headless + CDP, 측정마다 새 프로필(A · B · C · D, 끝나고 삭제), `--host-resolver-rules=MAP localhost 127.0.0.1, MAP * ~NOTFOUND, EXCLUDE 127.0.0.1`(바깥 이름 풀이 막음 · 기록), `/_vercel/*`는 페이지에서 막음.

| 줄 | 결과 | 근거 |
|---|---|---|
| 1 변경 범위 · 불변 | [OK] | 3절. PLAN.md는 10절 메모 1줄(AGENTS.md "실제 단계 문제는 10절" — 검증 1 "PLAN.md 변경 0"과 글자상 충돌, O1~O3와 같은 참고) |
| 2 manifest · 그림 | [OK] | 1절 manifest · 아이콘 · OG(내 해독기 + 직접 봄) + `pwa.test.ts` 통과 |
| 3 서비스 워커(VM) | [OK] | 내 하네스(`static.mjs`): install = open v1 → add `/offline.html` → skipWaiting / activate = `issuerank-v0` · 다른 이름 캐시 모두 삭제 → claim / fetch 30경우 × 네트워크 성공 · 실패: 같은 출처 GET 탐색 `/` · `/app` · `/s?issue=` · `/widget#app` · `/card` · `/privacy` · `/terms` · `/?login=google&error=…`만 respondWith(성공 = 네트워크 응답 그대로, 실패 = `caches.match('/offline.html')`) / `/api/auth/callback/google` · `/api/auth/sign-in/social` · `/api/card` · `/api/widget` 탐색 · `/_vercel/insights/*` · `accounts.google.com` · 닮은 다른 출처 · http 출처 · POST · HEAD · cors/same-origin/no-cors(`/api/*` · 아이콘 · manifest · sw.js 포함) = respondWith 0 · fetch · match 0 / 모든 경우 put 0 |
| 4 offline · head · OG | [OK] | 1절 head · 오프라인, `static.mjs` 규칙 문자열 |
| 5 공유(VM) | [OK] | `share-ui.test.ts` 13 통과 + 내 VM(`vm-share.mjs` 9/9: ⑦ 북마크에만 있는 이슈 = `[세계] 제목 원문` + `/s?issue=` · 요약 · 777 · 원문 URL 0 · 없는 id 0 · 클립보드 없음 → 창 · share 있어도 fine이면 복사) + 실제 브라우저 ①~⑥⑧⑨(줄 12) |
| 6 설치 · 등록(VM) | [OK] | 테스트 통과 + 내 VM(standalone display-mode · navigator.standalone → 맨 아래 숨김 · 헤더 그대로 숨김 · prompt 0 / 이벤트 두 번 → 마지막 것만 prompt 1 · rejection 조용히 / 이벤트 없음 → 링크 기본 동작) + 실제 등록 값 scope · updateViaCache none |
| 7 로컬 회귀 · 빌드 | [OK] | 4절(기존 테스트 파일 수정 0 = HEAD 해시) |
| 8 그림 다시 만들기 | [OK] | 1절 그림 만들기(두 번 = 커밋 파일 바이트 같음, 바깥 요청 0) |
| 9 로컬 서버 정적 · 헤더 | [OK] 19/19 | Node fetch 18요청(`api.mjs`): manifest 200 `application/manifest+json` · 본문 = 파일 / sw.js 200 `application/javascript; charset=utf-8` · `no-cache, no-store, must-revalidate` · CSP `default-src 'self'; script-src 'self'` · 본문 = 파일 / offline 200 text/html · CSP 없음 / 그림 6개 200 `image/png` · 바이트 = 파일 / `/` · `/app` · `/s` · `/s?issue=2026-10-19-pol-1` 본문 = index.html · 헤더 서로 같음(`public, max-age=0`) · CSP · Set-Cookie 0 / `/widget` = widget.html / 회귀 `/api/widget` · `/api/card/caption` `public, max-age=0, s-maxage=300` · `/api/rankings` Cache-Control 없음(로컬, O1 기록과 같음) · date 셋 다 2026-10-19 · `/privacy` = 파일. 다른 경로 헤더 변화 0(빌드 headers = 두 경로뿐) |
| 10 로컬 설치 가능 · 등록(CDP) | [OK] | 프로필 A 360×800: `Page.getAppManifest` url `/manifest.webmanifest` · errors `[]` · 받은 본문 = 파일(`parsed`는 이 판에서 scope만) · **`Page.getInstallabilityErrors` = `[]`** · scope `http://localhost:3000/` · activated · 새로 고침 뒤 controller `/sw.js` · 타일 30 |
| 11 로컬 캐시 · 오프라인 · 업데이트(CDP) | [OK] | A: 상세창 · `#list` · `#bookmarks`(비로그인 안내 · 페이지 fetch 401) · `/s?issue=2026-10-19-eco-2`(그 이슈 상세 · 경로 `/s` · 쿼리 지움) · `/widget#app` 뒤 캐시 = v1 offline.html 하나, `/api/*` fromServiceWorker 전부 false, SW가 보낸 요청 = 탐색 HTML · offline.html뿐 / **내 프록시만 닫음(연결 거부, 그동안 3000 도착 0)** → 네 곳 모두 안내 · 주소 그대로 · 가로 스크롤 0 · 페이지 `fetch('/api/rankings')` = TypeError / 다시 열고 `다시 시도` 실제 클릭 → 이슈맵 30 / **v2**(B): 표지 파일이 있을 때만 내 프록시가 `const CACHE = 'issuerank-v1';` 한 곳을 v2로(압축 없이 받아서) → `update()` = updated · controllerchange · 캐시 = `[["issuerank-v2",["/offline.html"]]]` · 꺼진 동안 새 탭 `/app` = v2 캐시의 안내. 아래 5.2 |
| 12 로컬 화면 · 공유 · 설치 UI(CDP) | [OK] | B 1280 · 360(아래 5.1 · 5.3) + D. 원시 행 중 검사 가정이 틀린 6행은 5.4에 근거와 함께 적음 |
| 13 안전 · 한도(로컬) | [OK] | 3000 누계 **100/100**(상한에서 포트 닫힘 — 넘지 않음, 9절) · newsdata · OpenAI · Kakao · Google 0(next-server 바깥 연결 = DB 3 · local뿐, 브라우저 바깥 = 이름 풀이 막힌 jsdelivr 글꼴 CSS · 막힌 Google 이동 시도뿐, 응답 0) · 누출 검사(`ex12/.env` 비밀 4값 + DB 비밀번호 · 사용자명 · 호스트 = 8개, `BETTER_AUTH_URL` 제외) 증거 88파일 · 테스트/빌드 로그 · 서버 로그 모두 false · DB 9표 건수 · 내용 해시 전후 같음(임시 계정 1 정리) · 소유 프로세스 0 · code-oss 30854 살아 있음 |
| 14 배포 정적 · 헤더 | [OK] 14/14(푸시 뒤) | 아래 "배포 줄 14 · 15" 절 — 배포 요청 14/15 |
| 15 배포 설치 가능 · 오프라인 | [OK](푸시 뒤) | 아래 "배포 줄 14 · 15" 절 — 배포 요청 21/25 |
| 16 Android 실제 | [SKIP — 사용자 확인] | 계획 표 그대로 |
| 17 카카오톡 공유 · 미리보기 | [SKIP — 사용자 확인] | 계획 표 그대로 |
| 18 iPhone | [SKIP — iPhone 기기 없음] | — |
| 19 Analytics 경로 | 기록만(판정 제외) | — |

### 5.1 설치 버튼 — 제작 6절 2 독립 확인

- A(막지 않음): 문서 전 capture 기록기에 **진짜 `beforeinstallprompt`(isTrusted true)가 첫 로드 388ms에 1번** 옴 → 우리 처리기가 헤더 아이콘을 보임(display flex · 40px). 제작 보고 "12번 첫 행 FAIL은 진짜 이벤트 때문" 주장과 같음.
- B(막음): 문서 전에 capture 단계 기록기가 진짜 이벤트를 세고 `stopImmediatePropagation` → 진짜 1번 막힘 · 1280 헤더 `앱 설치` **hidden · display none**(이벤트 전 숨김 = 제품 동작 맞음). 가짜 이벤트 → `preventDefault` · 헤더 오른쪽 맨 앞 `앱 설치`(글자 보임 · aria-label `이슈랭크 앱 설치`) → 실제 클릭 → prompt 1 · 숨김 → 다시 가짜 → 맨 아래 `앱 설치` 클릭 → prompt 1 더 · 주소 그대로 → 이벤트 없을 때 링크 기본 동작 막지 않음(`/widget#app`) → `appinstalled` → `앱을 설치했습니다` · 숨김. 360은 아이콘만(`.lbl` none) · 화면 안.

### 5.2 v2 업데이트 — 제작 6절 1 독립 확인

- CDP Fetch 시험(A): 브라우저 대상 · 페이지 · 서비스 워커 세션 2개 모두 `Fetch.enable(*sw.js*, Request 단계)` 뒤 `registration.update()` → **일시정지 0건**인데 서버에는 sw.js 3요청 도착 → 이 기기 Chromium 155에서 CDP Fetch로 SW 스크립트 요청을 못 잡는다는 주장 확인. 진단 C에서도 페이지 Fetch(`/*`)가 첫 등록의 sw.js · SW의 `cache.add('/offline.html')`는 못 잡음(서버 도착).
- 내 방법: 세는 프록시가 `sw-v2` 표지 파일이 있을 때만 `/sw.js` 본문 한 곳을 바꿈(제품 파일 그대로, `rewritten` 1). A의 첫 시도는 내 프록시가 gzip 본문을 그대로 고치려다 깨뜨려 `update()` TypeError — 프록시를 고친 뒤(그 요청만 압축 없이 받음) B에서 성공. 표지를 끈 뒤 브라우저의 탐색 뒤 업데이트 확인이 서버의 v1을 받아 v1을 다시 새 판으로 설치하고 v2 캐시를 지움(요청 78 · 79) → **반대 방향 옛 캐시 정리도 확인**(뒤 캐시 = v1 offline.html 하나).

### 5.3 B의 화면 · 공유 · 로그인 회귀(요지)

- 1280(share 없음 · pointer fine): 상세창 · 목록 카드(원문 링크 같은 줄 오른쪽) · 맨 아래 공유 → 클립보드 = 표의 복사 글 · 토스트 `링크를 복사했습니다` / writeText 거절 → `링크 복사` 창(auth-dialog) · 안내 문구 · 글 상자 = 복사 글 · readOnly · 전체 선택 · 초점 · 토스트 0 → Esc 닫힘(상세창 그대로) · `닫기` 닫힘 / 요약 접기 → 원문 · 공유 둘 다 숨김.
- 360(CDP 터치 에뮬레이션만으로 `(pointer: coarse)` true · 가짜 share 런타임 주입): 상세창 공유 = share 1 · **클릭 처리 안(window.event = click) · `userActivation.isActive` true** · 값 = 표 · 칸 3개 · 토스트 · 복사 · 창 0 / AbortError → 아무것도 안 함 / NotAllowedError → 복사 / canShare 거짓 → 복사 / 카드 · 맨 아래 = 표 값 / 보낸 값에 요약 · 원문 URL · `88건` · utm/ref 0 / 상세창 아래 버튼 3개 한 줄 · 창 안 / 세 화면 가로 스크롤 0 / 다크 #121211 · 맞는 theme-color #121211, 밝게 #f7f7f5.
- D20: 지금 판(실데이터)과 HEAD index.html(CDP Fetch로 넣고 API는 같은 저장 응답, 서버 요청 0) 이슈맵 768 · 1280(+360) 블록 · 타일 30개 style 좌표 같음.
- O1: `/?issue=2026-10-19-pol-1` → 상세창 · 88건 · 요약 = API · 원문 새 탭 · 쿼리 지움 · 비로그인 북마크 안내 · POST 0 · 랭킹 탭 → 뒤로(이슈맵) → 앞으로(랭킹).
- Google(가짜 클라이언트): `sign-in/social` 200 → `accounts.google.com` 이동 시도(이름 풀이 막힘, 응답 0) · redirect_uri `http://localhost:3000/api/auth/callback/google` · scope openid email profile · `better-auth.oauth_state` HttpOnly · Lax / 틀린 state 콜백 = **302 · fromServiceWorker false** → `/?login=google&error=state_mismatch` 문서 **fromServiceWorker true** → 쿼리 지움 · 로그인 창 `Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요`. 서버 로그 `AUTH_INTERNAL_ERROR` 1줄 = 이 일부러 틀린 state.
- 이메일(임시 계정 1): 가입 → `심사O4 · 로그아웃` · `로그인했습니다` · 세션 쿠키 · 캐시 = offline.html 하나 → 로그아웃 → 이메일 로그인 → 로그인 상태. 인증 · 북마크 · 폴더 응답 모두 fromServiceWorker false. 끝에 그 행만 삭제(user 1 · session 1 · account 1, 북마크 · 폴더 0).
- D(서버 요청 0 — 모든 응답 CDP 저장 파일): 09-2 · 09-4 = 이전 날짜(404) → `10월 18일 (일)` · 오늘 버튼 · 다음 날짜 됨 → 오늘 → 30타일 · 다음 날짜 막힘 · max 2026-10-19 · 미래 날짜 입력 → 2026-10-19.

### 5.4 심사 스크립트가 FAIL로 찍었지만 판정에서 바로잡은 행(내 검사 가정 오류)

| 원시 행 | 실제 | 근거 |
|---|---|---|
| O1 13① `history.length` 1 기대 → 2 | 내 탭을 about:blank로 열어 그 항목이 남음 | D: about:blank 탭에서 `/` = 2 · `/?issue=` = 2(쿼리 처리로 늘어난 항목 0), 13⑦ 탭 누름 뒤 3(+1 push) |
| 13⑦ 길이 2 기대 → 3 | 위와 같음 | 뒤로 = 이슈맵 · 앞으로 = 랭킹 `#list`, `#bookmarks` 안내는 A에서 확인 |
| v2 뒤 새 탭 캐시 = v2 기대 → v1 | 5.2 되돌림(표지를 끈 뒤 브라우저가 서버의 v1을 받아 설치) | 요청 기록 78 sw.js · 79 offline.html, 캐시 = v1 하나 · 탐색 fromServiceWorker true |
| Google 콜백 착지 0건 | 내 정규식이 `/?error=`만 찾음(실제 `/?login=google&error=state_mismatch`) | 네트워크 기록: 302 SW 밖 → 200 SW 경유, 화면 문구 OK |
| 가입 뒤 캐시 = v2 기대 → v1 | 위 되돌림 | 캐시 = offline.html 하나(개인 응답 0) |
| 바깥 accounts.google.com ≤ 1 기대 → 2 | 같은 이동이 이름 풀이 실패 1 + 취소 1 | 둘 다 응답 0, Google 실제 요청 0 |

그 밖 심사 절차 기록: B 첫 시도(요청 8)는 서버 재시작 직후 첫 API가 느려(DB 첫 연결 08:02:45) 내 고정 대기 안에 1280 첫 화면이 안 그려져 중단 — 진단 C(같은 설정 · 저장 응답 · 랭킹을 3초 늦추고 그 사이 SW claim)에서 3.2초에 정상으로 그려짐 → 제품 문제 아님. 100번째 요청(`/api/folders`)에서 내 상한 처리가 다른 열린 연결을 끊어 99번째 `/api/bookmarks` 응답이 브라우저에 못 감(서버는 200) — 두 번째 로그인 직후 목록 응답 하나만 영향, 제품 무관.

## 6. 위반

없음. 기사 본문 저장 · 표시 0 · 코드에 키 · 접속 정보 0(누출 검사 false) · `.env` git 제외(`.gitignore:4`) · PLAN 밖 패키지 0 · D0 밖 수집 0 · 새 · 바뀐 파일에 운영자 문의 이메일 0(privacy · terms 문의처 절에만, 두 파일 = HEAD) · offline.html 바깥 자원 · Analytics 0 · AGENTS.md 규칙 위반 0.

## 7. 발견한 점(심각도 순)

1. **낮음 · 참고(배포 줄 요청 계산)**: 서비스 워커가 있는 페이지는 탐색마다 몇 초 뒤 브라우저가 `/sw.js` 업데이트 확인을 보냄(no-store라 매번 서버 · CDN, 요청 78 · 85 · 96). 표준 동작이고 함수 호출은 아님(정적 파일). 배포 줄 · 앞으로의 측정에서 페이지당 `manifest` · 파비콘에 더해 sw.js 1을 셀 것(8절).
2. **낮음**: `beforeinstallprompt` 처리기는 본문 끝 인라인 스크립트에서 붙음. 이 기기 headless에서는 진짜 이벤트가 첫 로드 388ms에 와서 처리됐지만, 아주 느린 기기에서 스크립트 실행 전에 오면 그 방문엔 헤더 아이콘이 안 보일 수 있음(맨 아래 `앱 설치` → `/widget#app` 안내가 대신함). 고칠지는 16번 실기기 결과로 판단.
3. 참고: 서비스 워커 제외 조건은 `/api/`(끝 `/`)라 `/api` · `/apix` 탐색은 처리됨(네트워크 우선 → 실패 시 안내). 그런 화면 경로는 없음. 저장소가 지워져 offline.html이 없으면 실패 대체가 브라우저 기본 오류 화면이 됨(정상 저하).
4. 참고: 오프라인 화면에 아이콘 링크가 없어 꺼진 동안 `/favicon.ico` 요청이 콘솔 오류로 남음(제작 보고와 같음, 동작 영향 0).
5. 참고(문서): PLAN.md 10절 메모 1줄은 검증 1 "PLAN.md 변경 0"과 글자상 충돌(AGENTS.md 규칙에 따른 것, O1~O3와 같음). HANDOFF 맨 위 요약 줄은 아직 "다음 = O4 독립 심사"라 커밋 때 함께 고치면 좋음(이번 심사는 지시대로 O4 표 줄 · 다음 작업 O4 줄만 갱신).
6. 참고(심사 절차): 로컬 3000 한도 100을 다 씀 — 위 5.4 첫 시도 8 · 진단 4 · A의 v2 첫 시도 1이 원인. 다음 심사는 서버를 재시작했으면 첫 API를 한 번 데우거나 첫 화면이 그려질 때까지 기다리고, 서비스 워커 첫 로드 = 8 · 다음 탐색 = 5 + sw.js 1 · `/widget` = 4(widget.js)로 계산할 것.

## 8. 푸시 후 확인 절차(다음 역할)

1. **줄 14(푸시 · Vercel 재배포 뒤, 배포 요청 ≤ 15)** — Node fetch(브라우저 아님 → manifest · 파비콘 · sw.js 추가 요청 0). id는 DB 읽기 전용 SQL(최근 date의 pol 1위)로 정하면 14요청, `/api/widget` 첫 id를 쓰면 15요청. 순서: `/manifest.webmanifest` · `/sw.js` · `/offline.html` · `/icons/{icon-192,icon-512,maskable-192,maskable-512,apple-touch-icon}.png` · `/og.png` · `/` · `/app` · `/s` · `/s?issue={id}` · `/widget`. 기대: manifest 200 · Content-Type이 `application/manifest+json`으로 시작 · 본문 = 파일 / sw.js 200 · `application/javascript; charset=utf-8` · `no-cache, no-store, must-revalidate` · CSP `default-src 'self'; script-src 'self'` · 본문 = 파일 / 그림 200 `image/png` · 바이트 = 파일(og IHDR 1200×630) / 화면 4경로 본문 = index.html · head og:image = `https://issuerank-kappa.vercel.app/og.png` / `/widget` = widget.html. 각 응답의 cache-control · x-vercel-cache · age는 기록만. **manifest Content-Type이나 sw.js 헤더 3개 중 하나라도 없으면 멈추고 보고**.
2. **줄 15(CDP, 배포 요청 ≤ 25)** — 새 프로필 하나를 세 번 띄움(서비스 워커가 프로필에 남음, 끝나면 삭제). 공통 플래그 `--host-resolver-rules="MAP * ~NOTFOUND, EXCLUDE issuerank-kappa.vercel.app"` · 페이지에서 `/_vercel/insights/*` 막기(Analytics는 이 줄 대상 아님 — 페이지당 2요청 절약) · Google 버튼 · 로그인 누르지 않음. ① 360×800 `/`(≈ 8: 화면 · rankings · get-session · manifest · 아이콘 1~2 · sw.js · offline.html) → `Page.getAppManifest` 오류 0 · `Page.getInstallabilityErrors` `[]` · SW activated → 가짜 share(런타임 주입 + 터치 에뮬레이션)로 상세창 공유 1번 → url = `https://issuerank-kappa.vercel.app/s?issue=<id>` → 새로 고침(≈ 6, sw.js 확인 포함) → controller 있음 ② 같은 프로필을 `--host-resolver-rules="MAP issuerank-kappa.vercel.app 127.0.0.1:9, MAP * ~NOTFOUND"`로 다시 띄워 `/` · `/app` → 오프라인 안내(배포 요청 0) ③ ①의 규칙으로 다시 띄워 `/`(≈ 6) → 평소 화면. 합계 ≈ 20~22. v2 확인은 필요 없음(필요하면 CDP Fetch가 아니라 프록시가 필요 — 5.2).
3. 사용자 줄 16 · 17 · 18 · 19는 계획 표 그대로(사용자 할 일 아래).

## 9. 요청 · 비용 · 정리

- **로컬 3000: 100/100**(프록시 기록 n=1~100, 503 · 차단 0 · 업스트림 오류 0): Node 줄 9 = 18 · A = 37(첫 로드 8 · 새로 고침 5 · bookmarks 1 · `/s?issue=` 5 · `/widget` 4 · 다시 시도 5 · CDP Fetch 시험 sw.js 3 · v2 첫 시도 1 · 새로 고침 5) · B 첫 시도 8 · 진단 C 4(SW 스크립트 · offline.html — 페이지 Fetch가 못 잡음) · B 33. 100번째에서 포트를 닫아 넘지 않음, 그 뒤 D는 서버 요청 0. 3001 직접 0(준비는 로그로 확인). 정지 확인 curl 2(연결 거부 = 도착 0).
- 바깥: newsdata 0 · OpenAI 0 · Kakao 0 · Google 0(이동 시도 2 = 이름 풀이 막힘 · 취소, 응답 0) · 배포 주소 0 · npm 내려받기 0(`npx --offline`). 브라우저 바깥 시도 = 기존 jsdelivr Pretendard CSS(이름 풀이 막힘)뿐.
- DB: 읽기 전용 스냅숏 전후 9표 건수 · 내용 md5 같음(articles 1105 · issues 754 · collect_runs 96 · bookmarks 2 · folders 2 · user 2 · session 1 · account 2 · verification 0). 쓰기 = 임시 계정 1(가입 · 로그인 · 로그아웃) → finally에서 그 이메일 행만 삭제(user 1 · session 1 · account 1).
- 프로세스: 시작 전 목록 저장(`ps-before.txt`, code-oss 30854). 내가 띄운 것 = 서버 1(래퍼 17561 → proot 17562 → judge-server 17574 → next-server 17586) · 서버 2(26534 → 26536 → 26549 → 26560, 프록시 고친 뒤 카운트 55에서 이어감) · Chromium A 22996 · B 1943 · B 14169 · C 2회 · D(각각 `Browser.close` + 내 프로세스 그룹 TERM/KILL). 서버는 표지 파일로 next 그룹 종료. 끝 확인: curl 3000 · 3001 · 9481~9484 = 000, `ps`에 next-server · next start · judge-server · 내 chromium 0, 시작 뒤 새로 생긴 프로세스 = 확인 셸과 원래 있던 `usb-bookmarks --watch`(13653)의 `sleep 2`뿐, code-oss 30854 살아 있음. 넓은 패턴 kill 0. 프로필 4개 · 그림 재생성 폴더 2개 삭제. `ex12/.next`는 심사의 깨끗한 빌드 산출물(git 제외).

## 사용자 할 일

1. (완료 — 푸시 `8aaf6bd`) 커밋 · 푸시 승인(커밋 대상 = ex12 바뀐 3 + 새 13: `tools/pwa-images.tsx` · `public/manifest.webmanifest` · `public/sw.js` · `public/offline.html` · `public/og.png` · `public/icons/*.png` 5 · `test/pwa.test.ts` · `test/share-ui.test.ts` + 문서: 제작 보고 · 이 심사 보고 · HANDOFF · PLAN.md 10절 메모 1줄. 계획 문서는 이미 HEAD `9a83c20`에 있음).
2. (완료 — 2026-10-19 18:01~18:05 KST, 14 · 15 OK, 맨 아래 절) 푸시 뒤 배포 줄 14 · 15(8절, 역할이 실행).
3. Android 16(설치 · 아이콘 마스크 · 주소창 없는 창 · 상태 표시줄 색 · 비행기 모드 오프라인 · 앱 안 로그인 · 실제 메뉴 이름) · 카카오톡 17(나와의 채팅으로 이슈 · 사이트 공유 → 메시지 · 미리보기 · 링크 열기 · PC 붙여 넣기, 옛 미리보기면 카카오 공유 디버거) 결과 · 캡처 전달. iPhone이 생기면 18, (선택) 며칠 뒤 19.
4. 알아 둘 것: 배포 뒤 `sw.js` · `offline.html`을 바꾸는 배포는 `issuerank-v2`로 올림(옛 캐시 정리 동작은 이번에 두 방향 모두 확인). 문제가 생기면 같은 주소 `/sw.js`를 fetch 처리 없는 판으로 바꿔 배포(계획 "업데이트" 줄).

## 배포 줄 14 · 15 (2026-10-19)

- 대상: `https://issuerank-kappa.vercel.app`(Production, 푸시 `8aaf6bd` — 코디네이터 확인: GitHub 커밋 상태 Vercel success 17:58 KST). 로컬 HEAD = `8aaf6bd` · 작업 트리 깨끗, 제품 파일 sha256 = 심사한 작업 트리. 실행 18:01~18:05 KST. 줄마다 새 카운터. DB는 id 정할 때 읽기 1번(읽기 전용 트랜잭션: 최근 수집일 2026-10-19 · pol 1위 `2026-10-19-pol-1`)뿐
- 스크립트 `ex12/.data/judge-o4-20261019/deploy14.mjs` · `deploy15.mjs` · `netlog-count.mjs`, 결과 `deploy14-result.json` · `deploy15-result.json` · `deploy15-run3-result.json` · `deploy15-netlog-counts.txt`

### 줄 14 [OK] — Node fetch 14요청(≤ 15)

멈춤 조건(manifest Content-Type · sw.js 헤더 3개) 모두 통과 → 끝까지 실행. 모든 응답 200, 본문 · 바이트 = 저장소 파일, Set-Cookie 0.

| 경로 | Content-Type | Cache-Control | x-vercel-cache · age | 그 밖 |
|---|---|---|---|---|
| `/manifest.webmanifest` | `application/manifest+json; charset=utf-8`(Vercel이 charset을 붙임 — 기대 "application/manifest+json으로 시작" 충족) | `public, max-age=0, must-revalidate` | MISS · 0 | 본문 = 파일 |
| `/sw.js` | `application/javascript; charset=utf-8` | `no-cache, no-store, must-revalidate` | MISS · 0 | CSP `default-src 'self'; script-src 'self'` · br · 본문 = 파일 — **headers() 3개가 Vercel 정적 파일에도 붙음** |
| `/offline.html` | `text/html; charset=utf-8` | `public, max-age=0, must-revalidate` | MISS · 0 | 본문 = 파일 |
| `/icons/*.png` 5 · `/og.png` | `image/png` | `public, max-age=0, must-revalidate` | MISS · 0 | 바이트 · sha256 = 파일, IHDR 192 · 512 · 192 · 512 · 180 · og 1200×630 |
| `/` · `/app` | `text/html; charset=utf-8` | `public, max-age=0, must-revalidate` | MISS · 0 | 본문 = index.html · og:image = `https://issuerank-kappa.vercel.app/og.png` |
| `/s` · `/s?issue=2026-10-19-pol-1` | `text/html; charset=utf-8` | `public, max-age=0, must-revalidate` | HIT · 0 | 본문 = index.html · og:image 같음 |
| `/widget` | `text/html; charset=utf-8` | `public, max-age=0, must-revalidate` | MISS · 0 | 본문 = widget.html |

### 줄 15 [OK] — CDP, 배포 요청 21(≤ 25)

새 프로필 하나를 세 번 띄움(Termux Chromium 155 headless, `--disable-quic` · DoH 끔 · `/_vercel/*` 페이지에서 막음 · Google 버튼 · 로그인 누르지 않음). 배포 요청 수 = Chromium NetLog(Default — 쿠키 · 인증값 없음)에서 배포 호스트로 실제 보낸 요청(`HTTP_TRANSACTION_SEND_REQUEST` BEGIN) 수 — 서비스 워커 스크립트 · SW 안 fetch까지 셈.

| 단계 | 결과 | 배포 요청 |
|---|---|---|
| ① `MAP * ~NOTFOUND, EXCLUDE issuerank-kappa.vercel.app`, 360×800 `/` | 첫 화면 타일 30 · `isSecureContext` · `Page.getAppManifest` url = 배포 manifest · 오류 0 · 받은 본문 = 파일 · **HTTPS `Page.getInstallabilityErrors` = `[]`** · SW activated · scope `https://issuerank-kappa.vercel.app/` · script `/sw.js` · updateViaCache none · 진짜 `beforeinstallprompt` 655ms 도착 → 헤더 설치 아이콘 보임 / 터치 에뮬레이션(coarse) + 가짜 share로 상세창 공유 1번 → 클릭 안에서 1회 · 값 = `{ title: '이슈랭크', text: '[정치] {제목 원문}', url: 'https://issuerank-kappa.vercel.app/s?issue=2026-10-19-pol-1' }`(그 페이지 데이터로 만든 기대값과 같음) / 새로 고침 → controller = 배포 `/sw.js` · 문서 SW 경유 · 캐시 = `[["issuerank-v1",["https://issuerank-kappa.vercel.app/offline.html"]]]` · `/api/*` 4응답 fromServiceWorker false · JS 예외 0 | 14(`/` 2 · rankings 2 · get-session 2 · manifest 2 · icon-192 3 · sw.js 2 · offline.html 1) |
| ② 같은 프로필, `MAP issuerank-kappa.vercel.app 127.0.0.1:9, MAP * ~NOTFOUND` | `/` · `/app` 둘 다 오프라인 안내(`인터넷에 연결되어 있지 않습니다` · `다시 시도`) · 주소 그대로 · 가로 스크롤 0 · 캐시 그대로 · JS 예외 0 | 0 |
| ③ 같은 프로필, ①의 규칙 | `/` 평소 화면(타일 30 · 제목 `오늘 가장 많이 보도된 이슈`) · controller 있음 · 문서 SW 경유 · JS 예외 0 | 7(`/` · rankings · get-session · manifest · icon-192 2 · sw.js 1) |

- NetLog: 배포 호스트 밖으로 보낸 요청 0(세 번 모두). 이름 풀이 시도 = 배포 호스트(①③) · 규칙의 `~notfound`(jsdelivr 글꼴 CSS 등 막힘)뿐. newsdata · OpenAI · Kakao · Google 0(Google 버튼 · 로그인 안 누름, accounts.google · kakaocdn · googleapis 요청 0).
- 캡처(직접 봄): `deploy15-360-drawer.png`(배포 상세창 `기사 원문 · 북마크 · 공유` 한 줄) · `deploy15-360-offline.png`(배포 주소 막힘 → 오프라인 안내).
- 심사 절차 기록: 첫 실행의 NetLog 집계가 요청마다 이벤트 3개(BEGIN · END · HTTP/2 헤더)를 세어 ①을 42로 잘못 보고 ③을 한도 때문에 건너뜀 → 같은 NetLog를 BEGIN만 세도록 고쳐 다시 집계(① 14 · ② 0, `deploy15-netlog-counts.txt`), 같은 프로필로 ③만 다시 실행(7). 브라우저가 보낸 실제 요청 수는 이 고침과 무관(합계 21). `deploy15-result.json`의 ① 목록 · 합계 42는 이 잘못 센 값.
- 참고: 탐색 뒤 sw.js 업데이트 확인(①의 sw.js 2번째 · ③의 sw.js)과 페이지마다 manifest · 아이콘이 요청에 포함 — 7절 1과 같음. 배포 `/s` · `/s?issue=`는 CDN HIT, 나머지 정적 파일은 첫 요청 MISS.

### 배포 줄 요청 · 정리

- 배포 요청: 줄 14 = **14/15**(Node fetch, 리디렉트 0) · 줄 15 = **21/25**(NetLog). 로컬 3000 요청 0.
- 프로세스: Chromium ① 19754 · ② 20177 · ③ 21538(+ NetLog 플래그 시험 1회, data: 페이지만 · 네트워크 0) — 각각 `Browser.close` + 내 프로세스 그룹 TERM/KILL, 끝 확인 CDP 9491 · 9492 = 000, `ps`에 내 chromium · next 0, 새로 생긴 프로세스 = 확인 셸 · 원래 있던 `sleep 2`뿐, code-oss 30854 살아 있음. 넓은 패턴 kill 0.
- 프로필 `profile-deploy15` · NetLog 파일 3개(세션 scratchpad) 삭제. DB 쓰기 0 · 임시 계정 0.
- 남은 것: 사용자 16(Android 실기기) · 17(카카오톡) · 18(iPhone) · 19(기록만) → **PASS 보류 유지**.

## 사용자 확인 (2026-10-20 · 10-30)

- 10-20 사용자: "android pwa 앱 설치, 오프라인 안내, 카카오톡 공유 및 미리보기를 테스트 했어" → 16 설치 · 오프라인 안내, 17 카카오톡 공유 · 미리보기 = **사용자 시험 · 문제 보고 없음**('통과'라는 말은 없음 — 코디네이터는 OK로 봄).
- 10-30 사용자: "링크를 누르면 그 이슈 상세가 열려" → 17③ **OK**. "인 앱 브라우저 내에서는 로그인 상태가 이어지진 않아" — 카카오톡 앱 안 브라우저는 쿠키 저장소가 따로라 정상 동작으로 기록(FAIL 아님). 설치한 앱(WebAPK)의 로그인 유지 · 16 실제 메뉴 이름 · 17④ PC 붙여 넣기는 답 없음.
