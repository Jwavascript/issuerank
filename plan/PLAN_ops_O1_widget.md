# PLAN_ops_O1_widget — 홈 화면 위젯 1단계

> 이유: 출근길 사용자(PRODUCT_PLAN 2절 A)는 사이트를 열고 로그인하는 것도 귀찮아 아침 헤드라인을 놓친다(불편 ④, 여정 06:30~07:10). 그래서 헤드라인 순서와 링크를 서버의 공개 API 한 곳이 정해 내려주고(D22), 사이트에 이슈 바로가기를 더하고, iPhone 홈 화면에 Scriptable 위젯을 둔다. Android는 1단계에 홈 화면 바로가기 안내만 두고 위젯은 2단계 네이티브 앱으로 미루며(D23), 사용은 Vercel Web Analytics로 센다(D24).
> 코드 폴더: `ex12/` (운영 폴더, 복사 없음 — PLAN.md D21). 보고: `reports/ops_O1_widget_build.md` · `reports/ops_O1_widget_judge.md`.
> SPEC 근거: US-11 · AC-11-1~4. 회귀: AC-03-1 · AC-03-2 · AC-04-1~3 · AC-05-1 · AC-05-2 · AC-07-3 · AC-09-1~3. 적용 결정(모두 확정): D3(04:17) · D8 · D10 · D16 · D20 · D21 · D22 · D23 · D24.

## 이번 범위
- 서버: 공개 `GET /api/widget`(순서 · 링크를 서버가 정함) + `openapi.yaml`. 사이트: `/?issue=<id>` 바로가기(첫 로드 때 그 이슈 상세창) · 상세창 "수집" 칸을 실제 수집 완료 시각으로 · Vercel Web Analytics 스크립트 2줄. iPhone: Scriptable 스크립트 `public/widget.js` + 설치 안내 `public/widget.html`(`/widget`, Android 바로가기 칸 포함).
- 새 패키지: 없음(Scriptable은 사용자 iPhone의 무료 앱, Web Analytics는 패키지 없는 HTML 방식).
- 확정(사용자 2026-10-17): Android는 안내 칸만(D23) · 측정은 Web Analytics, 지표 = 아침 05~10시 KST 일별 방문자(D24) · 상세창 수집 시각 고침(D24) · iPhone 실기기 없음 → 18번 줄 하나만 SKIP이면 O1은 "PASS 보류"(D24).
- 하지 않음: Android 위젯(2단계 네이티브, D23) · 네이티브 iOS 앱 · 잠금 화면(accessory) 위젯 다듬기 · 위젯 설정(섹션 고르기 등) · 분석 사용자 지정 이벤트(Hobby 미지원) · 푸시 알림 · PWA manifest · 앱 아이콘(O4) · 로그인 변경(O2) · 인스타 카드(O3) · AI · 수집 · 묶기 · 요약 · 하루 실행 코드와 워크플로 · `/api/rankings` 캐시 · 기존 계약 변경 · 새 DB 테이블 · 개인정보 수집 · 메인 화면의 안내 링크 · `web/`.

## 위젯 규칙
| 항목 | 규칙 |
|---|---|
| 주소 | `GET /api/widget` — 공개(로그인 · 쿠키 없이), 쿼리 없음(붙어도 무시하지만 CDN 캐시 키가 갈리므로 위젯은 붙이지 않음). 새 `server/routes/widget.ts`를 `app.ts`에서 `/api/rankings`처럼 연결. BFF(`server/bff.ts`) · `app/api/[...path]/route.ts`는 그대로(공개 경로라 no-store를 붙이지 않음) |
| 200 본문 | 칸은 `date` · `updatedAt` · `updatedLabel` · `morePath` · `items`뿐. `date` · `updatedAt` = 날짜 생략 `/api/rankings`와 같은 값(D3 최근 수집일). `updatedLabel` = updatedAt을 사이트 날짜 줄(`fmtUpdated`)과 같게 `{월}.{일} {오전·오후} {시}:{분 두 자리} 수집`(예 `10.17 오전 4:31 수집`, 0시 → 오전 12시, 12시 → 오후 12시). `morePath` = `/#list`(작은 위젯 전체 · 줄 밖을 누를 때). `items[]` = `{ id, section, title, path }`(`path` = `/?issue={id}`), 표시 순서대로 |
| 연관기사 수 | 넣지 않음(요약 · 원문 url · rank도). 이유: 위젯은 숫자를 보이지 않고(AC-11-1) 순서도 서버가 정해(D22) 쓸 곳이 없음 · 받은 숫자를 위젯이 그리는 실수를 막음 · 순서 검증은 같은 날 `/api/rankings`의 articleCount와 DB 읽기로 함 |
| 순서 | 대상 = 같은 날 `/api/rankings` 카드 전부(섹션마다 Top 5, D8 대체 섹션 포함, 최대 30, 같은 id 집합). ① 섹션 1위(rank 1)들 → ② 나머지(rank 2~5) 전부, 각각 D16(연관기사 수 ↓ → importance ↓ → 최신 발행 시각 ↓ → id 자연 순서)으로 정렬해 ① 뒤에 ②. 이슈 없는 섹션은 빠짐. `ranking.ts`의 `rankIssues` 비교식을 함수 하나로 빼서 같이 씀(`rankIssues` · 요약 대상 선정 결과 불변) |
| 항목 수 | 전부(≤ 30) 내려줌, 위젯이 앞에서부터 크기만큼 자름. 크기 인자 없음(캐시 하나) |
| 실패 | 랭킹 없음 404 `{ "message": "해당 날짜의 랭킹 데이터가 없습니다" }` · DB 오류 500 `Internal Server Error`(기존 onError, 로그 `API_INTERNAL_ERROR`) · 앱 준비 실패 502 `{ "message": "API 서버에 연결할 수 없습니다" }`(기존 BFF). GET 말고는 기존처럼 404 |
| 캐시 | 200만 `Cache-Control: public, max-age=0, s-maxage=300`(Vercel 권장 꼴 `max-age=0, s-maxage=N`). Vercel CDN이 지역별로 300초 보관하고 클라이언트에는 s-maxage를 뺀 `public, max-age=0, must-revalidate`를 줌. stale-while-revalidate는 안 씀(만료 뒤 첫 요청이 옛 데이터를 받아 위젯 갱신 한 번을 버림). 404 · 500 · 502는 캐시 헤더 없음(Vercel 기본 = 캐시 안 함) · Set-Cookie 없음. 함수 호출 = 지역마다 5분에 약 1번 |
| 04:17 반영 | 하루 실행이 DB에 쓴 뒤 CDN은 늦어도 300초 안에 새 응답. 새 배포는 캐시 키가 달라 첫 요청부터 새로 읽음. 같은 날 수동 재실행(D5)을 바로 내보내려면 Vercel › CDN › Caches › Purge(모든 요금제). 위젯 화면은 iOS가 정하는 다음 갱신 때(아래 갱신 힌트, iOS 예산 하루 40~70회 · 15~60분) |
| 계약 | `/api/rankings` · `/api/auth/*` · `/api/bookmarks*` · `/api/folders*`의 상태 · 본문 · 헤더 그대로. `openapi.yaml`에 `/api/widget`(200 · 404)과 `Widget` · `WidgetItem`(id = IssueId, section = SectionKey, path 패턴 `^/\?issue=`, items 1~30) 추가, info.version 0.2.0 |
| 바로가기 주소 | `/?issue=<id>`로 정함(`#issue=` 아님). 이유: ① 해시는 이미 화면 몫(`#map` · `#list` · `#bookmarks`)이고 `viewFromHash` · `popstate`를 안 고침(`#issue=`면 지금 코드가 이슈맵으로 떨어뜨리고 뒤로가기 처리를 바꿔야 함) ② 위젯에서 열 때마다 새 문서 로드라 첫 로드 한 곳에서만 처리(같은 문서 해시 이동 처리 불필요) ③ 정적 `index.html`에 영향 없음(Vercel은 정적 파일 캐시 키에서 쿼리를 무시) |
| 바로가기 동작 | `index.html`의 이름 있는 함수(VM 테스트가 잘라 실행)가 첫 로드 때만: 값을 읽은 즉시 `history.replaceState`로 주소의 쿼리만 지움(해시 · 기록 수 그대로 → 새로고침 · 탭 복원은 평소 화면, 뒤로가기 그대로) → id가 형식(`{YYYY-MM-DD}-{섹션 키}-{숫자}`, 달력에 있는 날짜, KST 오늘 이하)에 맞으면 날짜 생략 대신 `GET /api/rankings?date={id 날짜}` 한 번 → 로그인 확인 · 화면 그리기(`go`)가 끝난 뒤(그 전에 열면 `applyUser` · `go`가 닫음) 그 랭킹에 id가 있으면 기존 상세창(`openDrawer`: 3줄 요약 · 연관기사 수 · 원문 링크 · 북마크). 지난 날짜면 화면도 그 날짜(기존 "n월 n일의 주요 이슈" · "오늘" 버튼). D8 대체 섹션 항목은 id 날짜(직전 성공일) 랭킹으로 열림 |
| 바로가기 실패 | 형식 틀림 · 미래 → 바로가기 무시, 평소처럼 최근 날짜. 날짜 랭킹 없음(404) → 그 날짜 + 기존 빈 화면("해당 날짜의 랭킹 데이터가 없습니다"). 그 랭킹에 id 없음(Top 5 밖 · 없는 번호 · 같은 날 재실행으로 바뀜) → 그 날짜 랭킹. 셋 다 상세창 없이 토스트 "이슈를 찾을 수 없습니다"(서버 404 문구와 같음). `issue` 값은 화면에 쓰지 않음(찾기에만) |
| 상세창 수집 시각 | `openDrawer`의 "수집" 칸: 그 카드 `date` = 보고 있는 랭킹 `date`면 `fmtUpdated(ranking.updatedAt)`(기존 함수 그대로, 예 `10.17 오전 4:31` — 날짜 줄 "… 수집" · 위젯 `updatedLabel`에서 " 수집"을 뺀 것과 같은 값), 다르면(D8 대체 섹션 카드) `fmtShort(it.date)`만(예 `10.16`, 그날 수집 시각은 응답에 없음). 고정 문구 "오전 6:00"은 지움. updatedAt = 그날 ok 수집 실행의 가장 늦은 완료 시각(실행 기록이 없으면 기존 계약대로 06:00) |
| Scriptable 파일 | `public/widget.js`(배포 `/widget.js`, 정적 · 함수 호출 0). 맨 위 버전 주석과 `const BASE = 'https://issuerank-kappa.vercel.app'`. 모듈 문법 없음, 최상위 await, Scriptable 공식 문서에 있는 API만 씀. 요청은 `BASE + '/api/widget'` 하나(`timeoutInterval` 10초, 기본 60초) |
| 크기별 | `config.widgetFamily`: small 3줄 · 13pt semibold · 제목당 2줄까지 / medium 5줄 · 13pt · 1줄 / large · extraLarge 10줄 · 14pt · 1줄 / 그 밖(accessory*) small 규칙(1단계 미지원) / 앱에서 실행(null) = medium 미리보기(`presentMedium`). 항목이 적으면 있는 만큼. 글은 제목과 하단뿐(머리말 · 숫자 · 섹션 이름 없음), 줄 제한을 넘는 제목은 iOS가 끝을 …로 줄임. 줄 간격 small 4 · medium 3 · large 6, 패딩 기본값 |
| 누르기 | medium · large: 줄마다 `WidgetText.url = BASE + path`, 위젯 `url = BASE + morePath`. small: 탭 대상이 위젯 하나뿐(Scriptable 문서) → 위젯 `url = BASE + morePath`(랭킹 목록, 요약 펼침), 줄 url 없음. `morePath`가 `/`로 시작하지 않으면 `BASE + '/'`. 누르면 iOS가 Scriptable을 먼저 열고 그 앱이 주소를 엶(Apple 문서) |
| 하단 · 색 | 하단 = `updatedLabel`(10pt regular, 빈 칸 뒤 맨 아래). 색은 사이트 토큰 `Color.dynamic(밝게, 어둡게)`: 배경 #ffffff/#1a1a19 · 제목 #161616/#ecebe8 · 하단 #6f6b65/#9b978f(`Device.isUsingDarkAppearance`는 위젯 미지원) |
| 갱신 힌트 | `refreshAfterDate`: 보여 주는 데이터의 date가 KST 오늘이면 3시간 뒤, 아니면 "KST 오늘 04:30"과 "30분 뒤" 중 늦은 때(실패 안내 화면도). KST는 기기 시간대와 무관하게 UTC + 9시간으로 셈. 보장 아님(iOS가 정함) |
| 오프라인 | 실패 = 예외 · 시간 초과 · 200 아님 · 형식 틀림(`items`가 비었거나, 어떤 항목의 `title`이 문자열이 아니거나 `path`가 `/`로 시작하는 문자열이 아님, `date` · `updatedLabel`이 문자열 아님). 성공 때만 받은 JSON을 `FileManager.local().libraryDirectory()`의 `issuerank-widget.json`에 덮어씀(캐시 폴더는 시스템이 지울 수 있음). 실패면 그 파일로 같은 모양(하단 = 그때 시각), 없거나 깨지면 "불러오지 못했습니다" 한 줄 + 위젯 url `BASE + '/'`. 어느 경우든 위젯이면 `Script.setWidget` 1번(앱이면 미리보기) · 끝에 `Script.complete()` |
| 설치 안내 | `public/widget.html` + `next.config.mjs` rewrite `/widget` → `/widget.html`: 아래 안내문(iPhone 1~6 · Android 7) + `/widget.js`를 읽어 보여 주는 읽기 전용 칸(읽기 실패면 "스크립트를 불러오지 못했습니다") + "스크립트 복사" 버튼(눌린 순간 클립보드에 씀, 되면 "복사했습니다", 안 되면 칸 전체 선택 + "길게 눌러 복사해 주세요") + Web Analytics 2줄. 그 밖의 외부 파일 없음 · 다크 대응 · 360px 가로 스크롤 0 |
| Android | 1단계는 `widget.html`의 Android 칸(안내문 7)만, Android 코드 0. 위젯은 2단계 네이티브 앱(GitHub Actions 빌드 검토)이 같은 `/api/widget` · `?issue=`를 씀(D23) |
| 측정 | Vercel Web Analytics HTML 방식 2줄을 `index.html` · `widget.html`에(아래 "측정"), 지표 = 아침 05~10시 KST 일별 방문자, 대시보드 Enable은 사용자(D24). 새 DB 테이블 · 개인정보 수집 · 사용자 지정 이벤트 없음 |

- 공식 근거(2026-10-17 확인): Vercel [Cache-Control](https://vercel.com/docs/caching/cache-control-headers) · [CDN 캐시 조건 · 지역별](https://vercel.com/docs/caching/cdn-cache) · [캐시 키 · Purge](https://vercel.com/docs/caching/cdn-cache/purge) · [응답 헤더 · x-vercel-cache](https://vercel.com/docs/headers/response-headers) / Scriptable [ListWidget(url · refreshAfterDate)](https://docs.scriptable.app/listwidget/) · [WidgetStack(small 탭 대상 하나)](https://docs.scriptable.app/widgetstack/) · [WidgetText(lineLimit · url은 medium/large)](https://docs.scriptable.app/widgettext/) · [Color.dynamic](https://docs.scriptable.app/color/) · [Request](https://docs.scriptable.app/request/) · [FileManager](https://docs.scriptable.app/filemanager/) · [config.widgetFamily](https://docs.scriptable.app/config/) · [Device](https://docs.scriptable.app/device/) · [URL scheme(내용 넣어 추가 불가 → 복사 · 붙여넣기)](https://docs.scriptable.app/urlscheme/) · [App Store(무료 · iOS 15.5+ · 1.7.19, 2024-09-30)](https://apps.apple.com/us/app/scriptable/id1405459188) / Apple [갱신 예산](https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date) · [누르면 앱이 열림](https://developer.apple.com/documentation/widgetkit/linking-to-specific-app-scenes-from-your-widget-or-live-activity) / [Next rewrites](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites).
- 확인 필요(공식 문서에 없음): Scriptable 최상위 await(관행) · 줄을 누른 뒤 Safari로 여는지 · 줄임표 모양 · 최신 iOS 동작(앱 마지막 업데이트 2024-09-30) → 18번 · Next가 라우트 응답의 Cache-Control을 그대로 두는지 → 12 · 15번 · Web Analytics 스크립트 고유 경로 · 대시보드에서 05~10시 기간 지정 → 21번 · 아래 "측정".

### Android 1단계 (D23 확정 — 조사 근거, 2단계 계획에 씀)
| 후보 | 위젯 · 누르기 | 비용 | 만들기(이 기기) | 배포 · 설치 | 유지 · 위험 |
|---|---|---|---|---|---|
| (a) KWGT 프리셋 | HTTP 텍스트 받기 · JSON 파싱([비교](https://docs.kustom.rocks/docs/more/compare_with_zooper/)), `wg(url, json, JSONPath)`([wg](https://docs.kustom.rocks/docs/reference/functions/wg/))로 `/api/widget` 읽기. 줄마다 주소 열기 · 웹 갱신 주기는 확인 필요 | 직접 만든 위젯은 무료판 제한 없음, **남이 만든 프리셋 가져오기는 Pro 키 필요**(1회 결제, [공식](https://docs.kustom.rocks/docs/general_information/cracked_free), 가격 확인 필요) → 받는 사용자마다 유료 | 운영자 Android에서 KWGT 편집기(빌드 없음) | `.kwgt` 파일 → Load Preset › Import([FAQ](https://docs.kustom.rocks/docs/faq/faq_kwgt/)) | 프리셋을 바꾸면 다시 가져오기 · 배터리 최적화가 갱신을 막을 수 있음([공식](https://docs.kustom.rocks/docs/common_issues/widget_not_udpating/)) |
| (b) 최소 네이티브 위젯(Jetpack Glance) | 진짜 위젯 · 줄마다 누르기 · 같은 API. 앱 매니페스트에 위젯 receiver 선언, 갱신은 1시간에 1번 이하 권장 · 네트워크는 WorkManager([Glance](https://developer.android.com/develop/ui/compose/glance/create-app-widget)) | APK 직접 설치 0원 / Play 등록비 25달러 1회([공식](https://support.google.com/googleplay/android-developer/answer/6112435)) + 2023-11-13 뒤 개인 계정은 12명 · 14일 비공개 테스트([공식](https://support.google.com/googleplay/android-developer/answer/14151465)) | Android Studio는 ARM Linux 미지원([공식](https://developer.android.com/studio/install)) → 이 기기 공식 빌드 경로 없음(명령줄 Gradle은 확인 필요). GitHub Actions(x86_64)는 공개 저장소 무료 · 비공개 월 2,000분([공식](https://docs.github.com/en/billing/concepts/product-billing/github-actions)) | APK 직접 설치. 개발자 인증: 2026-09-30 4개국, 2027년 전 세계 — 제한 배포는 신분증 · 등록비 없이 기기 20대, 미등록 앱은 사용자 "advanced flow"([공식](https://developer.android.com/developer-verification/guides)) | Kotlin · Gradle · 서명 키 · SDK 갱신을 1인이 관리 |
| (c) 바로가기 + 위젯 대기 | 위젯 없음. Chrome ⋮ › 설치 및 바로가기 만들기 › 바로가기 만들기([도움말](https://support.google.com/chrome/answer/15085120?hl=ko&co=GENIE.Platform%3DAndroid)). 웹 · PWA는 Android 홈 위젯 불가(PWA 위젯은 Windows 11 위젯 보드만, [공식](https://learn.microsoft.com/en-us/microsoft-edge/progressive-web-apps/how-to/widgets)) | 0원 | 코드 0(안내 문구만) | `widget.html` Android 칸 | 1단계 API · `?issue=`는 플랫폼 공통이라 2단계가 그대로 씀 |
- 결정(D23): (c). 1단계는 `widget.html` Android 칸(안내문 7)만이고 Android 코드 0. 위젯은 2단계 네이티브 앱((b) Jetpack Glance 후보, GitHub Actions 빌드 검토)이 같은 `/api/widget` · `?issue=`를 씀. 바로가기 아이콘은 Chrome 기본(앱 아이콘 · manifest는 O4).
- 운영자 본인 폰: KWGT 무료판은 처음부터 직접 만드는 위젯에 제한이 없어([공식](https://docs.kustom.rocks/docs/general_information/cracked_free)) `wg(주소, json, JSONPath)`로 `/api/widget`을 읽어 직접 만들어 쓸 수 있음(코드 0). 받는 사람마다 Pro가 필요하고 지원하지 않는 길이라 공개 안내 페이지에는 넣지 않음.

### 측정 (D24 확정 — Vercel Web Analytics)
- 넣는 것: `public/index.html` · `public/widget.html`의 `</body>` 바로 앞에 공식 HTML 방식(패키지 없음, [quickstart](https://vercel.com/docs/analytics/quickstart)) 2줄. 경로는 Enable 때 생기는 `/_vercel/insights/*`([문제 해결](https://vercel.com/docs/analytics/troubleshooting)) — 대시보드가 다른 고유 경로(`/<unique-path>/script.js`)를 안내하면 사용자가 알려 준 경로로 바꿈. 앱 코드는 `window.va`를 부르지 않음(사용자 지정 이벤트는 Hobby 미지원).
  ```html
  <script>window.va = window.va || function () { (window.vaq = window.vaq || []).push(arguments); };</script>
  <script defer src="/_vercel/insights/script.js"></script>
  ```
- 동작: 쿠키 · 개인 식별자 없음, 방문자는 요청 해시로 24시간만([공식](https://vercel.com/docs/analytics/privacy-policy)). Hobby 월 5만 이벤트(페이지뷰) 무료 · 넘으면 수집 멈춤 · 보고 기간 1개월([공식](https://vercel.com/docs/analytics/limits-and-pricing)). Pages 패널은 쿼리 없이 집계해 `?issue=` 방문은 따로 안 보임([공식](https://vercel.com/docs/analytics/using-web-analytics)). 스크립트를 못 불러와도(로컬 · Enable 전 404) 화면은 그대로여야 함(21번).
- 사용자 할 일: Vercel › 프로젝트 › Analytics › Enable → 재배포(Enable 전에 배포된 코드는 script.js 404, [문제 해결](https://vercel.com/docs/analytics/troubleshooting)).
- 지표: 아침 05~10시 KST 일별 방문자(Production). 사용자가 대시보드에서 읽음 — 사용자 지정 기간에 시각을 넣을 수 있는지는 확인 필요, 안 되면 하루 범위 시간별 그래프의 05~10시 방문자 합으로 대신하고 그렇게 적음. CLI `vercel metrics vercel.analytics.page_view.count --aggregation unique/visitorId --granularity 1h`는 Observability Plus 없이 됨([공식](https://vercel.com/docs/analytics/accessing-metrics-with-vercel-cli), 이 기기 CLI 동작은 미확인). 지표 기록은 운영 중 사용자 일이고 O1 판정에는 넣지 않음.
- 고르지 않은 것: 측정 안 함(수치 없음) · 위젯 API 요청 수(Hobby Observability 보존 12시간 · 경로별 분석은 유료, [제한](https://vercel.com/docs/observability/observability-plus)) · 자체 집계(새 DB 테이블). 개인정보처리방침의 분석 도구 문구는 PRODUCT_PLAN 6절 운영 기본에서.

### 설치 안내문 (`widget.html`에 그대로, 휴대폰 버전에 따라 메뉴 이름이 조금 다를 수 있음)
**iPhone**
1. App Store에서 **Scriptable**(무료, iOS 15.5 이상)을 설치합니다.
2. 이 페이지의 **스크립트 복사**를 누릅니다.
3. Scriptable을 열고 오른쪽 위 **+** → 빈 화면을 길게 눌러 **붙여넣기** → 위쪽 제목을 **이슈랭크**로 바꾸고 **완료**. ▶를 누르면 중간 크기 미리보기가 보입니다.
4. 홈 화면 빈 곳을 길게 눌러 **편집 › 위젯 추가** → **Scriptable** → 크기(작게 3줄 · 중간 5줄 · 크게 10줄) → **위젯 추가**.
5. 위젯을 길게 눌러 **위젯 편집** → **Script**에서 **이슈랭크**를 고릅니다.
6. 중간 · 큰 위젯은 줄을, 작은 위젯은 아무 데나 누르면 Safari에서 이슈랭크가 열립니다(Scriptable이 잠깐 보일 수 있음). 하단 시각이 오늘이 아니면 아직 갱신 전입니다(매일 새벽 갱신). 새 버전이 나오면 같은 방법으로 내용을 바꿔 붙여넣습니다.

**Android**
7. Chrome으로 이슈랭크(https://issuerank-kappa.vercel.app)를 열고 주소 표시줄 오른쪽 **더보기(⋮) › 설치 및 바로가기 만들기 › 바로가기 만들기** → **추가**를 누르면 홈 화면에 아이콘이 생깁니다. 홈 화면 위젯은 준비 중입니다.

## 화면 변경
- `public/index.html`: 첫 로드의 `?issue=` 처리 · 상세창 "수집" 칸(위 표) · Web Analytics 2줄(보이는 변화 없음)만. 그 밖의 문구 · 해시 화면 · 상세창 · D20 배치 그대로, 새 문구는 토스트 "이슈를 찾을 수 없습니다" 하나.
- 새 페이지 `/widget`(iPhone 설치 · Android 바로가기 안내, 메인에서 링크 안 함 — 운영자가 주소 공유). 위젯 화면 자체는 iPhone(Scriptable).

## 구현 순서
1. 복사 없음(D21): `git status --short`로 사용자 변경 확인 · 보존, 작업 전 해시(검증 1번) 기록.
2. 서버: `server/ranking.ts`(D16 비교 함수 공유 + `buildRanking`을 쓰는 위젯 목록 순수 함수 · `updatedLabel`) → `server/app.ts`(Store에 위젯 목록 읽기 하나 + 라우트 연결) · 새 `server/routes/widget.ts` → `server/sample.ts`(메모리) · `server/db/store.ts`(`ranking`의 행 읽기 — 그날 행 · D8 대체 행 · 그날 ok 실행 — 를 내부 함수로 빼 같이 씀, `ranking` 결과 불변) → `openapi.yaml`.
3. 테스트: `test/ranking.test.ts` · `test/api.test.ts` · `test/auth-proxy.test.ts`에 줄 추가, 새 `test/widget-ui.test.ts`(바로가기 · 상세창 수집 시각, account-ui처럼 실제 HTML 함수를 VM에서) · 새 `test/widget-script.test.ts`(가짜 Scriptable 전역 — 공식 문서에 있는 속성 · 메서드만).
4. 화면 · 정적: `public/index.html`(바로가기 · 상세창 수집 시각 · Web Analytics 2줄) → `public/widget.js` → `public/widget.html`(안내문 1~7 · Web Analytics 2줄) → `next.config.mjs` rewrite 1줄.
5. 로컬 검증(1~14 · 19 · 20 · 21의 로컬 부분) → `reports/ops_O1_widget_build.md` → 사용자 할 일: ① 커밋 · 푸시 요청(Vercel 자동 재배포, 다음 예약 실행도 같은 ex12 — 하루 실행 파일은 해시 불변) ② Vercel › Analytics › Enable 뒤 재배포 ③ iPhone이 생기면 18번 결과 전달 → 배포 줄(15~17 · 21의 배포 부분).
마지막. `bash scripts/ex12-ubuntu.sh npm test` · `bash scripts/ex12-ubuntu.sh npm run build`

## 검증 방법
- 실행: 래퍼 `bash scripts/ex12-ubuntu.sh …`. 로컬 서버는 `npm run build` 뒤 `npx next start -p 3000 --hostname 127.0.0.1`(이 기기 dev Turbopack은 파일 감시 한도로 API 실패, PLAN.md 10절), 접속 `http://localhost:3000`. 안전 실행 = ex12 방식(`ex12/.env`만, stdout + stderr 비밀값 true/false 검사 후 false일 때만 출력, 응답 원문 대신 상태 · 건수 · bool).
- 한도: newsdata · OpenAI 0 · DB 쓰기 0(읽기는 랭킹 · importance · latest_published_at만) · 로컬 3000 요청 ≤ 100 · 배포 요청 역할마다 ≤ 40(브라우저 자동 요청 포함) · 임시 계정 0. 실제 데이터 줄의 날짜는 "SQL 최근 수집일 · 직전 수집일". 소유 서버 · Chromium만 끄고 `curl` 000 · `ps` 0으로 확인(proot 포트는 Termux `ss`에 안 보임).
- 판정: iPhone 기기가 없어 18번은 [SKIP — 사용자 iPhone 확인 대기](D24). 다른 줄이 모두 OK이고 18번 하나만 남으면 O1은 "PASS 보류"로 기록(PASS 아님), 다른 FAIL · SKIP이 있으면 그 줄이 먼저. iPhone 결과를 받으면 18번만 다시 판정. 그래서 iPhone에서 볼 것(줄 수 · 링크 · 오프라인 · 실패 안내 · 갱신 힌트)은 8~10번 가짜 전역 테스트로 미리 고정한다.

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 1 변경 범위 · 하루 실행 불변 | `git status --short` · `git diff --stat` · 작업 전후 `sha256sum ex12/server/{auth,ai,bff,daily,steps,collect,group,grouping,newsdata,summary,summarize}.ts "ex12/app/api/[...path]/route.ts" ex12/package.json ex12/package-lock.json .github/workflows/daily.yml` | 바뀐 · 새 파일 = 구현 순서 2~4 목록뿐 · 해시 모두 같음(새 패키지 0) · ex01~ex11 · `web/` · SPEC · PLAN.md 변경 0 |
| 2 순서 규칙(단위, AC-11-1) | `test/ranking.test.ts`: ① 6섹션 × 5 ② 동점(1위끼리 · 나머지끼리 건수 같음 → importance → 시각, UTC · +09:00 표기 섞음 → id `-9` < `-10`) ③ pol 2개 · eco 5개 · 나머지 0 ④ 그날 행 없음 ⑤ D8(그날 pol만, 다른 섹션은 전날 행) ⑥ 입력 순서 뒤집기 ⑦ updatedLabel 04:31 · 00:07 · 12:00 · 13:05 | ① 30개 · 앞 6개 = 각 섹션 rank 1을 건수 ↓ · id 집합 = `buildRanking` 카드 ② 정한 순서 ③ 1위 2개 → 나머지 5개 ④ null ⑤ 1위 묶음에 전날 id 5개 · path 날짜도 전날 ⑥ 같은 결과 ⑦ `10.17 오전 4:31 수집` · `오전 12:07` · `오후 12:00` · `오후 1:05`. 기존 rankIssues · buildRanking · findCard 테스트 그대로 통과 |
| 3 위젯 API 계약(테스트, AC-11-1 · 11-3) | `test/api.test.ts`(메모리, 2026-10-07 12:00 KST): `GET /api/widget` · 같은 날 `GET /api/rankings` · 최근 수집일 없는 저장소 · `POST /api/widget` | 200 · 최상위 칸 5개 · 항목 칸 4개(articleCount · summary · url · rank 없음) · items 30 · id 집합 = rankings 카드 · 앞 6개 = rank 1 · path `/?issue={id}` · morePath `/#list` · updatedLabel `10.7 오전 6:00 수집` · `Cache-Control: public, max-age=0, s-maxage=300` · Set-Cookie 없음 / 404 같은 문구 · Cache-Control 없음 / POST 404 · 기존 rankings 테스트 그대로 |
| 4 BFF 공개 경로 | `test/auth-proxy.test.ts`: 가짜 앱이 `/api/widget`에 위 Cache-Control로 응답 | 헤더 그대로(no-store 안 붙음) · 외부 fetch 0 |
| 5 OpenAPI | `npx @redocly/cli lint openapi.yaml`(설치 없이 npx) · 표와 대조 | 오류 0(경고 참고) · `/api/widget` 200 · 404 · `Widget` · `WidgetItem` 칸 = 표 · 다른 경로 차이 0 |
| 6 바로가기(VM, AC-11-2) | 새 `test/widget-ui.test.ts`: 바로가기 함수를 잘라 가짜 `fetch` · `openDrawer` · `toast` · `history` · `location`으로 ① `?issue=2026-10-07-pol-{그날 카드 번호}` ② 지난 날 id ③ `abc` · `2026-02-30-pol-1` · 내일 id ④ 그날 404 ⑤ 그날 카드에 없는 id ⑥ 쿼리 없음 · `#list`만 | ① `?date=2026-10-07` 1회 → openDrawer(id) 1회 · 토스트 0 · replaceState 1회(쿼리만 빠지고 해시 그대로) ② 같은 흐름 · 날짜 = 지난 날 ③ 날짜 없는 랭킹 1회 · openDrawer 0 · 토스트 "이슈를 찾을 수 없습니다" ④ ⑤ openDrawer 0 · 같은 토스트 ⑥ 지금과 같음(replaceState 0 · 토스트 0) |
| 7 Scriptable 문법 | `node --check public/widget.js`(Node 24는 최상위 await을 모듈로 감지해 검사 — Termux Node 24.18.0에서 확인함) | 통과 · import/export 0 |
| 8 크기 · 줄 수 · 링크(가짜 전역, AC-11-1 · 11-2) | 새 `test/widget-script.test.ts`: 파일을 async 함수 본문으로 실행. 가짜 `ListWidget` · `WidgetText` · `WidgetStack` · `WidgetSpacer` · `Color` · `Font` · `Request` · `FileManager` · `config` · `Script`는 공식 문서에 있는 속성 · 메서드만 가짐(없는 API를 부르면 실패). widgetFamily small · medium · large · extraLarge · accessoryRectangular · accessoryInline · accessoryCircular · null × 항목 30 · 10 · 5 · 3 · 1개 | 제목 줄 = min(항목 수, 3 · 5 · 10 · 10 · 3 · 3 · 3 · 5), items 앞에서부터 같은 순서 · 같은 글자 · 텍스트는 제목 + 하단뿐(머리말 · 숫자 · 섹션 이름 0) · medium · large · null 줄 url = `https://issuerank-kappa.vercel.app` + 그 항목 path(정확히 일치), 위젯 url = BASE + morePath · small · accessory* 줄 · 스택 url 0, 위젯 url = BASE + `/#list` · morePath가 `/`로 시작하지 않으면 위젯 url = BASE + `/` · Request 1개(주소 `BASE/api/widget`, 쿼리 0, timeoutInterval 10) · 위젯이면 setWidget 1 · 앱이면 presentMedium 1 · 둘 다 complete 1 |
| 9 글자 · 색 · 하단 · 갱신 힌트(AC-11-3) | 같은 테스트: 크기별 글꼴 · 줄 제한 · 색 / 시각 고정(KST): 12:00 + date 오늘 · 23:00 + 오늘 · 12:00 + 어제 · 00:10 + 어제 · 03:00 + 어제 · 04:20 + 어제 · 05:10 + 실패(캐시 없음) · 05:10 + 실패(오늘 캐시). 같은 경우를 `bash scripts/ex12-ubuntu.sh env TZ=UTC npx vitest run test/widget-script.test.ts`와 `TZ=Asia/Seoul`로 두 번 | 제목 semibold 13 · 13 · 14, lineLimit 2 · 1 · 1, 하단 regular 10 = updatedLabel 그대로 · 배경 · 제목 · 하단 Color.dynamic hex = 표 · refreshAfterDate(KST) 15:00 · 다음 날 02:00 · 12:30 · 04:30 · 04:30 · 04:50 · 05:40 · 08:10 · 두 시간대 결과 같음 |
| 10 실패 · 오프라인 캐시(AC-11-4) | 같은 테스트, 이어서 실행: ① 성공 ② Request가 던짐 ③ 시간 초과(거절) ④ 500 ⑤ 404 ⑥ 200인데 형식 틀림(items 빈 배열 · title 숫자 · path가 `http`로 시작) ⑦ 캐시 없음 + 실패 ⑧ 캐시가 JSON 아님 · 형식 틀림 + 실패 | ① `libraryDirectory()`/`issuerank-widget.json` = 받은 JSON(파싱 같음), cacheDirectory · documentsDirectory 안 씀 ②~⑥ 캐시의 제목 · 순서 · 링크 · 하단 그대로 · 캐시 파일 안 바뀜(⑥이 캐시를 덮지 않음) ⑦ ⑧ "불러오지 못했습니다" 한 줄뿐 · 위젯 url = BASE + `/` · 모든 경우 예외가 밖으로 안 나감 · setWidget 1 · complete 1 |
| 11 로컬 회귀 · 빌드 | wrapper `npm test` · `npm run build` | 기존 테스트 모두(D20 기준 119개) + 새 테스트 통과 · 빌드 · 타입 성공 · 새 패키지 0 |
| 12 로컬 서버 API · 정적 | 안전 실행 `next start` · Node fetch `/api/widget` · `/api/rankings` · `/widget.js` · `/widget.html` · `/widget` · `/?issue={id}` · `/` | widget 200 · date = rankings date = SQL 최근 수집일 · id 집합 같음 · 앞 묶음 = 섹션 rank 1 · 순서 = rankings articleCount + SQL 읽기 importance · latest_published_at으로 따로 계산한 D16 순서와 같음 · 헤더 표대로 / widget.js 본문 = 파일 / `/widget` = widget.html 본문 / `/?issue=` 본문 = `/` 본문 |
| 13 로컬 화면(Chromium CDP, AC-11-2 · 04-1~3 · 05-2 · 07-3 · 09-2) | `ex12/.data/verify-mapmobile.mjs` · `judge-lib.mjs` 방식, 360×800 새 탭마다: ① `/?issue={widget items[0].id}` ② `/?issue={직전 수집일 Top 5 id}` ③ `/?issue={그날}-pol-99999` ④ `/?issue=abc` ⑤ `/?issue={id}#list` ⑥ ①의 상세창 북마크(비로그인) ⑦ `/` → 랭킹 탭 → 뒤로 → 앞으로 · `/#bookmarks` ⑧ `/widget` 복사 칸 · 버튼 · 다크 | ① 상세창 제목 = API 제목 · 요약(또는 "요약을 준비 중입니다") · 연관기사 · 원문 새 탭 · `location.search` 빈 값 · `history.length` 1 ② 날짜 = 직전 수집일 · "오늘" 버튼 · 상세창 ③ 그날 랭킹 · 상세창 없음 · 토스트 ④ 최근 날짜 · 토스트 ⑤ 랭킹 목록 + 상세창 ⑥ 로그인 안내 · 저장 요청 0 ⑦ 지금과 같음(#list ↔ 이슈맵, 상세창 닫힘) ⑧ 칸 = `/widget.js` · 복사 뒤 클립보드 같음(권한을 못 주면 SKIP 사유) · 다크 토큰 · 모든 화면 가로 스크롤 0 |
| 14 안전 · 한도(로컬) | 요청 카운터 · 비밀값 검사 · DB 내용 해시 전후 · 프로세스 | 3000 요청 ≤ 100 · newsdata · OpenAI 0 · DB 쓰기 0(기사 · 이슈 · 북마크 · 폴더 해시 같음) · 누출 false · 남은 프로세스 0 |
| 15 배포 API · 캐시(푸시 뒤) | 배포 주소 Node fetch `/api/widget` 2번(10초 간격) · `/api/rankings` · `/widget.js` · `/widget` · `/?issue={id}` | widget 200 · 둘째 `x-vercel-cache: HIT`(아니면 30초 뒤 1번 더, 그래도 아니면 FAIL로 원인 조사) · 클라이언트 cache-control에 s-maxage 없음 · max-age=0 · date = rankings date · rankings는 지금처럼 `public, max-age=0, must-revalidate` · 정적 3개 200 |
| 16 배포 바로가기(CDP) | 배포 주소 360px로 13번의 ① ④ ⑦ · 20번 ② | 로컬과 같음 |
| 17 04:17 반영 | 푸시 다음 날 05:00~09:00 KST 배포 `/api/widget` · `/api/rankings` 1번씩 | 두 date = 그날 · 순서가 그날 rankings 기준(12번 계산법) |
| 18 iPhone 실제(AC-11-1~4) | 기기가 생기면 안내문대로 설치 → small · medium · large 1개씩 · 줄 누르기 · 작은 위젯 누르기 · 비행기 모드에서 새로 고침 · 다크 모드 · 다음 날 아침 하단 · iOS 버전 | 지금은 [SKIP — 사용자 iPhone 확인 대기](D24, PASS 아님). 결과를 받으면: 줄 수 3 · 5 · 10 · 제목만 · 긴 제목 … · 하단 시각 · 줄 → Safari 그 이슈 상세(Scriptable이 잠깐 열리는지) · small → 랭킹 목록 · 오프라인에서 마지막 내용 · 다크 색 |
| 19 Android 안내 칸(D23) | `widget.html` 원문 · 13번 ⑧ 화면 · `git diff --stat` | 안내문 7 문구 그대로(더보기(⋮) › 설치 및 바로가기 만들기 › 바로가기 만들기 · "홈 화면 위젯은 준비 중입니다") · 360px에서 보임 · Android 코드 · 빌드 파일 0 |
| 20 상세창 수집 시각(D24) | ① `test/widget-ui.test.ts`: `openDrawer`를 잘라 가짜 랭킹(date 2026-10-17, updatedAt `2026-10-17T04:31:12+09:00` · `…T13:05:00+09:00` · `…T00:07:00+09:00`)과 같은 날 카드 · D8 대체 카드(date 2026-10-16)로 ② 13번 ①의 상세창 | ① "수집" 칸 `10.17 오전 4:31` · `10.17 오후 1:05` · `10.17 오전 12:07` · 대체 카드 `10.16` · 상세창 템플릿에 고정 "오전 6:00" 없음 ② 칸 = 날짜 줄(`#updated`)의 "… 수집" 앞 시각 = `/api/widget` updatedLabel에서 " 수집"을 뺀 값 |
| 21 Web Analytics(D24) | ① 로컬 `next start`(Enable 없음) CDP로 `/` · `/?issue={id}` · `/widget` ② (Enable 전에 푸시했다면) 배포 주소 같은 3곳 ③ 사용자 Enable · 재배포 뒤 배포 `/` 1번 | ① ② `/_vercel/insights/script.js` 404여도 페이지 JS 예외 0 · 13번 ① ⑦ · 19번 결과 그대로 · `typeof window.va` = function · 쿠키 0 ③ script.js 200 · `…/view` 요청 1회(경로는 스크립트가 정함) · `document.cookie` 빈 값 · Set-Cookie 0 · (사용자 전달) 대시보드 방문 1 이상 |

## 2026-10-17 KST — 범위 추가 (D25, 제작 검증 FAIL 13 후속)

| 변경 | 규칙 |
|---|---|
| 가로 넘침 | 360px에서 목록 · 북마크 화면 레이아웃 폭이 363~378px로 넘침(기존 HEAD 동일, 화면 전환 애니메이션, prefers-reduced-motion에서는 0). `public/index.html` CSS를 최소로 고쳐 화면 전환 중·후 모두 `scrollWidth <= innerWidth`. 전환 애니메이션 모양·641px 이상 배치·D20 이슈맵 좌표는 그대로 |
| 바로가기 실패 문구 | 바로가기 날짜 랭킹 요청이 5xx · 네트워크 실패면 기존 오류 토스트(예 "API 서버에 연결할 수 없습니다")를 그대로 보이고 "이슈를 찾을 수 없습니다"로 덮지 않음. "이슈를 찾을 수 없습니다"는 200인데 id 없음 · 404 · 형식 오류일 때만 |

검증 추가: ① 360·375·390·768·1280px에서 이슈맵 · 목록 · 북마크 세 화면 각각 전환 직후 · 전환 중(애니메이션 프레임 여러 번 샘플)·정지 후 `document.documentElement.scrollWidth <= innerWidth`, reduced-motion 켜고 끄고 둘 다. 768·1280 이슈맵 타일 좌표 · 641px 이상 목록 화면 배치 변경 전과 같음 ② VM 테스트: 바로가기 날짜 응답 500 · 502 · 네트워크 실패 → 기존 오류 토스트, 200(id 없음) · 404 → "이슈를 찾을 수 없습니다". 재검증 로컬 요청 한도 60(제작 재검증분).
