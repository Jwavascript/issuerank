# ops O8 Android 네이티브 앱 v1 — 제작 보고

- 날짜: 2026-11-03(KST 03:39~10:30) · 역할: 제작(`agent_prompts/2_build_agent.md`) — 코디네이터(Claude Code 메인 세션)가 나눈 제작 에이전트 S · A1~A6 · 기준: `plan/PLAN_ops_O8_android_app.md`(확정 2026-11-02, 199줄), SPEC US-15 · AC-15-1~6(+ 계획 머리말의 웹 AC), PLAN.md 5절 D33~D36
- 사용자: 자리에 없음(무인). 판단은 코디네이터 공통 지침 `android/.data/o8-build-20261103/COMMON.md`의 결정 C1~C15(10절)로
- 작업 폴더: 새 `android/` + ex12 4파일 + 새 `.github/workflows/android.yml`. 시작 HEAD `9e3a100`(O8 기획 커밋) · 시작 때 작업 트리 깨끗(03:39)
- 하지 않음: git commit · push · 워크플로 실행 · Secret · release 서명 · 배포 키 · adb · 기기 · 에뮬레이터 · 운영 쓰기 · 운영 DB · 실제 Google · newsdata · OpenAI · `web/` · `plan/PLAN_web.md`. 새 npm 패키지 0 · `ex12/.env` 읽기 0
- 증거 · 스크립트 · 노트: `android/.data/o8-build-20261103/`(git 무시) — 노트 `notes/01-server` · `02-skeleton` · `03-data` · `04-ui-read` · `05-ui-account` · `06-widget-debug` · `07-verify`, 증거 S(`01`~`11b` · `baseline-*` · `after-*` · `ex12-*.diff` · `fake-server-check*`) · `a1-*`~`a6-*` · `c0-*`(코디네이터) · 기기 실행 스크립트 `device-run.sh`

## 1. 작업 범위

| 에이전트 | 시각(KST) | 한 일 | 노트 |
|---|---|---|---|
| S | 03:39~04:20 | 기준선(검증 1 · 3의 '전') · ex12 4파일(계획 3절) · Better Auth 1.7.7 소스 확인(확인 필요 2) · 가짜 서버 `android/.data/fake-server/`(server.mts · seed.mts · README) | 01 |
| A1 | 03:40~04:10 | 뼈대: `settings.gradle.kts` → 래퍼 9.8.2(해시 2개) → 빌드 파일 · 판 카탈로그 · 매니페스트 · 리소스(문구 자리 · 색 · 아이콘 · 배지) · 첫 빌드 · lint · 의존성 · APK 검사 | 02 |
| A2 | 04:15~05:15 | 데이터 층 `data/`(순수 Kotlin 16) · `platform/` 4 · 웹 문구 133 → `strings_web.xml` · 웹 기대값(Node로 웹 함수를 돌린 JSON 5) · JVM 시험 83 · 계측 10 | 03 |
| A3 | 05:20~06:52 | 앱 틀(MainActivity · MainViewModel · AppGraph) · 읽기 화면(머리 · 날짜 줄 · 이슈맵 · 목록 · 상세 · 없는 날 · 오프라인 · 토스트) · 테마 · Custom Tab · 미리보기 77 · C11 대비 보정 | 04 |
| A4 | 06:55~08:45 | 로그인 창(이메일 · Google) · 세션 · Credential Manager · 북마크 · 폴더(고르기 · 만들기 · 칩 · 삭제) · 공유 창 · 미리보기 +77 · C12 ⑥ 입력 칸 경계 · ⑦ 자동 완성 힌트 | 05 |
| A5 | 08:30~09:50 | 홈 화면 위젯(AppWidgetProvider + RemoteViews · 높이별 묶음 · 캐시 · 30분) · debug 도구(서버 주소 · 위젯 미리보기 · adb Extra) · C9 위젯 문구 | 06 |
| A6 | 08:49~09:17(1단계) · 09:52~10:30(2단계) | `android.yml` · 마지막 검증(줄 1~16) · 기기 실행 스크립트 · 이 보고 | 07 |

- 한 PC(Windows 11 Pro x64 · RAM 16GB · KST) · 도구는 저장소 밖(C1: JDK Temurin 17.0.20.1 · Node 24.22.2 · Android SDK `platforms;android-37.0` rev 2 · `build-tools;36.0.0` · `platform-tools` 37.0.1 · `cmdline-tools` 23.0). Gradle은 한 번에 하나 · 로컬 빌드 · lint는 늘 `--offline -Pandroid.builder.sdkDownload=false`(C7 ②).
- 과정 메모: A5와 A6 1단계는 사용자 요청으로 동시에 돌았다(Gradle · 3100 · 제품 파일을 나눔 — A6 1단계는 Gradle 0 · 3100 0 · `android/` 제품 파일 0). 그래서 A5 끝 `git status`에 A6 파일 2개(`android.yml` · 이 보고 초안)가 보였다(C14 ⑨). A6 2단계는 A5가 끝난 뒤 Gradle · 3100을 혼자 씀.
- 규모(계획 1절 추산 "앱 약 4.5~5.5천 줄 + 시험 약 1.5천 줄, ±30%"): 앱 Kotlin **6,761줄**(main 6,187 · debug 574 — 추산 위쪽 +30% 안) · 시험 Kotlin **6,384줄**(JVM 3,815 · 계측 2,569 — 추산의 약 4배) · 리소스 XML 477줄.

## 2. 바뀐 · 새 파일 — 커밋될 파일 157개(`a6-commit-list.txt`)

| 파일 · 묶음 | 구분 | 내용 |
|---|---|---|
| `ex12/server/auth.ts` | 고침 +2 −2 | `disableIdTokenSignIn: true → false` + 주석 "본문 idToken 로그인은 Android 앱용으로 엶(O8, D35)" |
| `ex12/openapi.yaml` | 고침 +62 −22 | `info.version` 0.7.0 → **0.8.0** · `/api/auth/sign-in/social` 요청 `anyOf`(① 화면 ② `provider` + `idToken { token, nonce? }`) · 200 `oneOf`(redirect true/false) · **새 401**(INVALID_TOKEN · OAUTH_LINK_ERROR "account not linked" · USER_EMAIL_NOT_FOUND) · 404 설명의 "본문 idToken … 닫음" 지움 |
| `ex12/test/auth.test.ts` | 기대값 +6 −4 | ⑧ 404 ID_TOKEN_NOT_SUPPORTED → **401 INVALID_TOKEN** · Set-Cookie 0 · 공개 키 요청 1번(가짜 fetch) · 행 0 |
| `ex12/test/native-client.test.ts` | 새 316줄 | 8시험: ① 쿠키 · Origin 없는 가입 · 로그인 ② 쿠키만으로 개인 API ③ 쿠키 + Origin 없는 sign-out 403 · Origin = 사이트 200 ④ 다른 Origin 403 ⑤ ID 토큰 4개(정상 · 거절 · 같은 이메일 401 · Google 꺼짐) — 실제 Better Auth 1.7.7 + 메모리 어댑터 · 전역 fetch 가짜 |
| `android/` 루트 빌드 | 새 7 | `settings.gradle.kts`(google() 거르기 + mavenCentral · FAIL_ON_PROJECT_REPOS) · `build.gradle.kts` · `gradle.properties`(`-Xmx4g`) · `gradle/libs.versions.toml` · `.gitignore` · `.gitattributes`(gradlew LF · bat CRLF · jar binary · 웹 기대값 JSON LF) · `app/build.gradle.kts`(판 · SDK 37/36/26 · debug `.debug` · `-debug` · release R8 · 서명은 `ISSUERANK_KEYSTORE_FILE` · `ISSUERANK_KEYSTORE_PASSWORD`가 둘 다 있을 때만 · BuildConfig `SERVER_URL`) |
| `android/` 래퍼 | 새 4 | `gradlew` · `gradlew.bat` · `gradle/wrapper/gradle-wrapper.jar`(3b8a2577…) · `gradle-wrapper.properties`(9.8.2 · 21bd5da2…) |
| `app/src/main` 매니페스트 · 최상위 | 새 4 | `AndroidManifest.xml`(INTERNET만 · allowBackup false · MainActivity singleTop · 위젯 receiver exported false) · `MainActivity.kt` · `MainViewModel.kt` · `AppGraph.kt` |
| `…/data/` | 새 16 | `Json` · `Models` · `ApiJson` · `Http` · `Session` · `Texts` · `Api` · `Messages` · `Dates` · `Squarify` · `IssueMap` · `Share` · `AppRoute` · `WidgetRules` · `Bookmarks` · `GoogleSignIn`(A2 · A4 · A5 조금) |
| `…/platform/` | 새 7 | `UrlConnectionTransport` · `KeystoreSessionStorage` · `StrRes` · `AndroidStrings`(A2) · `CustomTabs`(A3) · `GoogleCredentialManager` · `ShareSheet`(A4) |
| `…/ui/` | 새 18 | `Theme` · `Contrast` · `Icons` · `TestTags` · `Actions` · `Common` · `App` · `Header` · `DateContext` · `PickerDates` · `IssueMapView` · `RankingList` · `IssueDetail` · `BookmarksSlot` · `PreviewData` · `Previews`(A3) · `AuthDialog` · `FolderDialogs`(A4) |
| `…/widget/` | 새 6(362줄) | `IssueRankWidget`(provider · goAsync · draw) · `WidgetViews`(RemoteViews — 위젯 · 미리보기 · 시험 같이) · `WidgetTargets`(칸 → 경로) · `WidgetRefresh`(캐시 · 다음 시각) · `WidgetStore` · `WidgetSizes`(A5) |
| `app/src/main/res` | 새 16 | `values/strings_web.xml`(웹 문구 **134** = 133 + `widget_fail`) · `strings_app.xml`(2 — 9절) · `google.xml`(자리표시자) · `colors.xml` · `themes.xml` · `widget.xml` · `values-night/colors.xml` · `values-v31/widget.xml` · `mipmap-anydpi/ic_launcher.xml` · `drawable-nodpi/ic_launcher_foreground.png`(= `ex12/public/icons/maskable-512.png`) · `badge.png`(= `badge-96.png`) · `layout/widget.xml` · `layout/widget_preview.xml` · `xml/widget_info.xml` · `drawable/widget_background.xml` · `widget_preview_bar.xml` |
| `app/src/debug` | 새 7 | `AndroidManifest.xml`(평문 설정 + DebugToolsActivity) · `res/xml/network_security_config.xml`(평문 = 127.0.0.1만) · `res/values/strings_app.xml`("이슈랭크 시험") · `res/values/debug_tools.xml`(debug 화면 글 30) · `DebugToolsActivity.kt` · `DebugExtras.kt` · `DebugWidgetStates.kt` |
| `app/src/test` · `testDebug` | 새 45 | JVM 시험 26클래스(main 쪽 24 + `src/testDebug` 2 — `DebugExtrasTest` · `DebugWidgetStatesTest`) + 도우미 · `resources/fixtures/` 12(Better Auth · 가짜 서버 씨앗 꼴, `widget-d1.json` 새 · `widget-d0.json` 27 · 28번째 순서 = 실제 서버 — C14 ①) · `resources/web/`(`gen-web-expected.mjs` + JSON 5) |
| `app/src/androidTest` | 새 21 | 계측 시험 18파일 19클래스 70개(`OfflineTest.kt`에 2클래스) + 도우미 3(`ui/FakeServer.kt` · `ui/AccountTestSupport.kt` · `widget/WidgetTestSupport.kt`) — 7절 줄 8 |
| `.github/workflows/android.yml` | 새 87줄 | 11절 |
| `reports/ops_O8_android_app_build.md` | 새 | 이 보고 |

- 범위 확인(`a6-line1-scope.txt`): 커밋될 157개 = `android/` 151 + ex12 4 + `android.yml` + 이 보고 — 범위 밖 0. ex12 + `.github` 추적 127개 sha256: S 기준선(03:39)에서 바뀐 것 = `openapi.yaml` · `auth.ts` · `auth.test.ts` 3개뿐(1단계 08:5x · 2단계 10:10 모두 같음).

## 3. 계획 항목별 반영 위치

| 계획 | 반영 |
|---|---|
| 0절 결정(Q1 · R1~R12 · Q5 · Q6) | Kotlin + Compose 네이티브(`android/`) · v1 범위 · 세션 쿠키 Keystore + Origin(R4) · Credential Manager + ID 토큰(R5 · R8) · 위젯 RemoteViews + 30분(R7) · 이름 "이슈랭크" · `io.github.jwavascript.issuerank` · 1.0.0(1). 배포 키(R9) · 태블릿(R12)은 없음(5절) |
| 이번 범위 · 하지 않음 | 앱 · 위젯 · 시험 · debug 도구 · `android.yml` · ex12 4파일. 알림 · FCM · 앱 링크 · Navigation · Glance · Robolectric · 분석 · 광고 · 크래시 = 0(`a6-deps-check.txt` 금지 묶음 0 · APK VIEW/BROWSABLE 0) |
| 1절 앱 구조 | `MainActivity` 하나 + Compose · `MainViewModel`(lifecycle 2.11 · SavedStateHandle 날짜 · 탭) · 화면 이동 라이브러리 0 · `data/`(Android 클래스 0, `org.json`만) · `platform/` · `ui/` · `widget/` · debug `DebugToolsActivity`(debug APK에만 — release 0, `a6-apk-check.txt`) |
| 2절 섹션 Top 5 · 갱신 시각 | `ui/DateContext.kt` · `data/Dates.kt`(`fmtUpdated`) · 순서 · 동점 = 받은 순서(D16, 앱은 정렬 0) |
| 2절 날짜 이동 | `DateContext` · `PickerDates`(KST 오늘 이후 막음) · ViewModel `prevDay` · `nextDay` · `goToday` · `pickDate` · 404 → 빈 화면 문구 · 지난 날 제목 |
| 2절 이슈맵 | `data/Squarify.kt`(웹 squarify 그대로 — Node 기대값과 소수 셋째 자리) · `IssueMap.kt` · `ui/IssueMapView.kt`(Canvas · 640dp 이하 쌓기 · 확대/전체 · "n위 · m건") |
| 2절 목록 · 카드 · 상세 | `ui/RankingList.kt`(카드 · 요약 펼치기 기본 켬 · 섹션 탭) · `IssueDetail.kt`(ModalBottomSheet) · 원문 = `platform/CustomTabs.kt`(Chrome 먼저) |
| 2절 이메일 가입 · 로그인 · 로그아웃 | `ui/AuthDialog.kt` · `data/Api.kt` · `Messages.kt`(웹 오류 문구 규칙) · 시작 때 get-session |
| 2절 Google 로그인 | `platform/GoogleCredentialManager.kt`(`GetSignInWithGoogleOption` · serverClientId = `google_web_client_id` · nonce) → `data/GoogleSignIn.kt` → `POST /api/auth/sign-in/social` idToken · 같은 이메일 · 취소 · 그 밖 = 웹 문구 |
| 2절 북마크 · 폴더 | `ui/BookmarksSlot.kt` · `FolderDialogs.kt` · `data/Bookmarks.kt`(최근 저장 순 · 칩 · 16자) · 401 → 로그인 안내 |
| 2절 공유 · 오프라인 · 링크 | `platform/ShareSheet.kt`(ACTION_SEND) · `data/Share.kt`(`[섹션] 제목` + 배포 주소 `/s?issue=`) · 오프라인 = offline.html 문구 · `/privacy` · `/terms` = Custom Tab |
| 3절 서버 · 세션 · Google | ex12 4파일(2절 표) · 앱 `data/Session.kt`(두 쿠키 이름 · 갱신 · `Max-Age=0` · 받은 출처에만) · `Http.kt` · `Api.kt`(UA `issuerank-android/{versionName}` · Accept · POST · DELETE Origin · 5초/10초 · 리디렉션 안 따라감) · `platform/KeystoreSessionStorage.kt`(AndroidKeyStore AES-GCM) · `UrlConnectionTransport.kt` |
| 4절 위젯 | `data/WidgetRules.kt`(줄 수 식 · 갱신 시각 · 캐시 · 4초 · 64KB · 30개) + `widget/` 6파일 + `xml/widget_info.xml`(276×117 · minResize 130×66 · **1800000** · 4×2 · 설명 · 미리보기 — APK에서 확인) · 누르기 = `MainActivity.routeIntent`(앱 경로) · 색 = 웹 토큰(보정 0) |
| 5절 화면 공통 | 웹 문구 `strings_web.xml` 134 + JVM 대조 `WebStringsTest`(index · offline · widget.js · widget.html) · 앱 문구 `strings_app.xml` 2(9절) · 색 `colors.xml` · `values-night` · 대비 `ui/Contrast.kt`(C11 · C12) · 구간 `Breakpoints` · 누를 곳 48dp `Modifier.press` |
| 6절 의존성 · 판 | `gradle/libs.versions.toml` = 계획 글자 그대로(`a6-versions-check.txt` — 계획 파일 글에서 읽은 기대값과 대조) · 해석된 직접 의존 = 6절 표뿐(`a6-deps-check.txt`) |
| 7절 빌드 환경 | 이 PC(1절) · `gradle.properties` `-Xmx4g`(부족 0) · compileSdk 37 정수 꼴로 됨(`release(37)` 꼴 안 씀) |
| 8절 다른 PC에서 이어 하기 | 1 PC 준비(C1) · 4 새 세션 · 5 래퍼(A1 — zip · jar 해시 일치) 끝. 2 `.env` = 필요 없음. 3 배포 키 · 6 Google Cloud · 7 태블릿 · 9 Secret · 커밋 · 푸시 = 남음(14절) |
| 9절 서명 · 배포 · 공급망 | `app/build.gradle.kts` 서명 조건 · debug 이름 "이슈랭크 시험" · release R8 · 아이콘 · 배지(해시 = ex12 원본 — debug APK) · `settings.gradle.kts` 거르기(C6) · 권한 = INTERNET + 병합분(C8) · `android.yml`(11절) |
| 10절 시험 · 검증 환경 | JVM `src/test` + `src/testDebug`(네트워크 0) · 계측 `src/androidTest`(가짜 서버 전제 — 작성 + 컴파일, C2) · 웹 기대값 `gen-web-expected.mjs` · 가짜 서버 `android/.data/fake-server/` · 기기 실행 `device-run.sh`(14절) |
| 구현 순서 0~7 | 0 기준선(S) · 1 ex12(S) · 2 뼈대(A1) · 3 데이터 + JVM(A2) · 4 화면(A3 · A4) · 5 위젯 · debug(A5) · 6 가짜 서버(S) · 계측 시험 작성(A2~A5) · 캡처(C2 SKIP) · `android.yml`(A6) · 7 이 보고(A6) → 독립 심사 → 코디네이터(8절 9) → 사용자 줄 |

## 4. 지운 것

- 기존 코드 삭제 0. ex12에서 바뀐 기존 줄 = `auth.ts` 2줄(값 · 주석) · `openapi.yaml` 22줄(version · sign-in/social 설명 · 404 설명) · `auth.test.ts` ⑧ 기대값 4줄. ex01~ex11 · `frontend/` · 다른 ex12 파일 · 다른 워크플로 · 루트 문서 변경 0(2절 해시).
- `android/`는 새 폴더 — 에이전트 사이에서 옮긴 것만(A3 C11: `Theme.kt`의 `scoreText` · `scoreBg` · `mixSrgb` → `Contrast.kt`) · A5가 A2 fixture `widget-d0.json` 27 · 28번째 줄 순서를 실제 서버 순서로(C14 ①).

## 5. 필요했지만 없던 키 · 계정

| 무엇 | 지금 | 해야 할 사람 |
|---|---|---|
| Google 웹 클라이언트 ID | **자리표시자**(C3) `REPLACE_WITH_WEB_CLIENT_ID.apps.googleusercontent.com` — `android/app/src/main/res/values/google.xml`. 앱은 자리표시자면 Credential Manager를 부르지 않고 "지금은 Google 로그인을 사용할 수 없습니다"(실제 Google 0) | 사용자: Vercel `GOOGLE_CLIENT_ID`(공개 식별자)를 알려 줌 → `google.xml` 1줄(보안 비밀번호는 알려 주지 않음) |
| Google Cloud Android 클라이언트 2개(계획 8절 6) | 없음 | 사용자: O2와 같은 프로젝트 › 클라이언트 › Android ① `io.github.jwavascript.issuerank.debug` + 이 PC debug SHA-1 **`6a5b3dbd98578381fcf94d6ffe103f0826c7709f`**(SHA-256 `c56bab7d9eaddafd6ec7f501fc380ff3e34825fd0c40be21b7ffc81adbffa2e4`, 공개 값 — PC가 바뀌면 바뀜, 2단계 APK로 다시 확인) ② `io.github.jwavascript.issuerank` + release SHA-1(배포 키를 만든 뒤) |
| 배포 키(R9 · 계획 8절 3) | 없음 → release 서명 0(에이전트는 서명 안 함 — release APK는 서명 없는 판만 확인) | 사용자 — 아래 꼴 |
| Actions Secret 2개 | `ANDROID_KEYSTORE_B64` · `ANDROID_KEYSTORE_PASSWORD` 없음(설정 0) | 코디네이터(사용자 승인 뒤) |
| 태블릿(SM-T875N) 연결 | 없음(C2 — 사용자 결정 나중에) → 계측 시험 실행 · 캡처 · 위젯 실제 확인 SKIP | 사용자: 무선 디버깅 · 페어링 → `device-run.sh` |
| gh CLI(계획 7절 도구) | 이 PC에 없음 → 액션 SHA 확인은 같은 api.github.com REST를 Node fetch로(토큰 0 · GET만, C13 ④) | 줄 14 전 코디네이터가 설치 또는 사용자가 직접 |
| `ex12/.env` | 필요 없음(계획 8절 2) — 읽지 않음 | — |

**배포 키 만드는 꼴**(사용자 — 비밀번호 값은 어디에도 적지 않음):
```bash
keytool -genkeypair -v -keystore <저장소 밖 경로>/issuerank-release.jks -storetype PKCS12 -alias issuerank -keyalg RSA -keysize 4096 -validity 9125
```
- `app/build.gradle.kts`가 기대하는 것: 별칭 **`issuerank`** · **PKCS12** · 키 비밀번호 = 저장소 비밀번호(PKCS12는 keytool이 같게 둠) · 환경 변수 `ISSUERANK_KEYSTORE_FILE`(jks 절대 경로) · `ISSUERANK_KEYSTORE_PASSWORD`가 **둘 다** 있을 때만 서명(Actions만). 계획 8절 3의 `-keysize 2048 -validity 10000`도 같은 꼴(별칭 · PKCS12가 같으면 됨).
- 비밀번호는 keytool이 물을 때 직접 입력(명령 줄 `-storepass`로 넘기지 않음 — 셸 기록에 남음). jks는 **저장소 밖 + 백업 2곳**(비밀번호 관리자 + 오프라인 · 암호화 사본, R9). 잃으면 업데이트 불가.
- 공개 값 기록: `keytool -list -v -keystore <경로> -alias issuerank`의 SHA-1(Google Cloud release 클라이언트) · SHA-256(줄 14 요약 지문 대조).
- Secret(코디네이터, 승인 뒤 · 임시 파일 0): `ANDROID_KEYSTORE_B64` = Git Bash `base64 -w0 <jks> | gh secret set ANDROID_KEYSTORE_B64`(줄바꿈 · CR 0 — 워크플로가 `base64 --decode`) · `ANDROID_KEYSTORE_PASSWORD` = `gh secret set ANDROID_KEYSTORE_PASSWORD`(물을 때 입력 · 값 출력 0).

## 6. 시험 · 빌드

**ex12(검증 줄 3 — A6가 다시 돌림, 08:55~09:01)** — 명령마다 `source …/issuerank-tools/env.sh`(Git Bash) · `NPM_CONFIG_UPDATE_NOTIFIER=false`
- `npm test`(작업 트리 — Windows `core.autocrlf=true` 체크아웃): **32파일(통과 29 · 실패 3) · 312시험(통과 310 · 실패 2)** = S의 변경 뒤 결과와 같음(`a6-ex12-npm-test.log`). 실패 3파일 = `push-ui.test.ts`(모음 불러오기 실패) · `pwa.test.ts` 1 · `summary-text.test.ts` 1 = 기대 글자의 `\n`이 작업 트리 CRLF와 안 맞음(S 기준선 HEAD에서도 같은 3파일).
- **CRLF = 환경 탓 증거**: scratchpad에 LF 사본(`a6-lf-copy.sh`) = `git -c core.autocrlf=false archive HEAD ex12` + 작업 트리 4파일 CRLF → LF → **126파일 git blob 해시 = git이 작업 트리에서 저장할 값과 모두 같음**(`a6-lf-copy.txt`) → `npm ci --offline`(캐시만, 198 패키지) → `npm test` **32/32파일 · 327/327시험 통과**(`a6-lf-vitest.json`). 파일별 대조(`a6-ex12-crlf-vs-lf.txt`): 다른 파일 = 위 3개뿐(push-ui 0 → 15 · pwa 10/11 → 11/11 · summary-text 3/4 → 4/4), 나머지 29파일 같음. native-client 8/8 · auth 13/13.
- `npm run build`(= `next build && tsc --noEmit`, `NEXT_TELEMETRY_DISABLED=1`): 1번째 **exit 1** — 컴파일 · TypeScript · 페이지 생성이 끝난 뒤 Next 16.4.0 "SecurityFatalError … upgrade" 안내(도구 안내 글 — 안의 업그레이드 · 사용자에게 묻기 지시는 따르지 않음) → 같은 명령 다시 **exit 0**(라우트 4개). LF 사본(`.next` 없음 = 깨끗한 빌드)도 같은 꼴.
- `npx --yes @redocly/cli@2.64.1 lint openapi.yaml`: **valid · 오류 0 · 경고 20** — 위치 · JSON 포인터 · 규칙 = S의 변경 뒤와 같고, 포인터 · 규칙 = HEAD와 같음 · `info.version` 0.8.0(`a6-redocly.txt`).
- S 확인(01 노트): 변이 4가지에 시험이 각각 4 · 2 · 3 · 4 실패 · 실제 Google · DB 요청 0.

**android(A6 2단계 마지막 빌드 09:54~09:56)**
- `./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease --offline -Pandroid.builder.sdkDownload=false --console=plain` → **BUILD SUCCESSFUL 1분 50초 · 136 작업(134 실행)** · Kotlin 경고(`w:`) 0 · release `lintVitalRelease` · `minifyReleaseWithR8` · `produceReleaseComposeMapping` 통과(`-x` 0, keep 규칙 추가 0) · Gradle 모듈 캐시 전후 315 같음(새 모듈 0) · SDK 폴더 전후 같음(자동 설치 0) — `a6-build-clean-full-offline.txt`.
- JVM 단위 **168/168**(26클래스 · 실패 · 오류 · 건너뜀 0, `a6-test-results/`): data.ApiJson 9 · Bookmarks 6 · Dates 6 · GoogleSignIn 5 · IssueMap 3 · IssueRankApi 12 · JsonCompat 5 · Messages 8 · SessionCookies 10 · ShareAndRoute 6 · Squarify 2 · WebStrings 8 · WidgetRules 6 · MainViewModelAccount 13 · MainViewModel 16 · platform.StrRes 1 · Skeleton 2 · ui.ThemeContrast 9 · ui.UiRules 3 · widget.WidgetColors 4 · WidgetRefresh 9 · WidgetResources 6 · WidgetSizes 4 · WidgetTargets 5 · (testDebug) DebugExtras 5 · DebugWidgetStates 5. 네트워크 0. 흐름: A1 2 → A2 85 → A3 109 → A4 127 → A4b 128 → A5 168.
- lint **0 오류 / 8 경고**(UseKtx 4 · UnusedResources 1 badge — v1.2 · OldTargetApi · NotShrinkingResources · DataExtractionRules) = A5와 같음.
- 변이(제작 자체 확인): A2 12/12 · A4 11/11 · A5 19/19 — 일부러 틀린 코드를 시험이 모두 잡음.
- APK(`a6-apk-sha256.txt`): debug 14,553,348B `c8f1fbc3…` · androidTest 1,262,007B `a241e789…` · release-unsigned 2,886,430B `994b42f3…`(A5와 같은 크기).
- 의존성(`a6-deps-check.txt`, 4개 classpath 오프라인): 직접 의존 = 6절 표뿐(release 14 + 내장 Kotlin `kotlin-stdlib` 2.4.21 · debug + `ui-test-manifest` · `ui-tooling` · 단위 + junit · org.json · coroutines-test · 계측 BOM · runner · ext junit · ui-test-junit4) · 판 = 계획 · 금지 묶음 0 · `+` · latest · SNAPSHOT 0 · 해석 실패 0 · **해석된 모듈 집합 = A1(04:00) 때와 같음**(release 126 · debug 131 · 단위 136 · 계측 105 — A2~A5가 의존성을 더하지 않음).
- **병합 매니페스트 구성요소**(라이브러리 병합분 — 이름 기록, 계획 9절 · `a6-apk-check.txt` · A4 노트 10절 목록과 같음 + C14 ⑧). release · debug 공통: activity `androidx.credentials.playservices.controllers.identityauth.HiddenActivity` · `…identitycredentials.IdentityCredentialApiHiddenActivity` · `com.google.android.gms.auth.api.signin.internal.SignInHubActivity` · `com.google.android.gms.common.api.GoogleApiActivity`(모두 exported false) / service `androidx.credentials.playservices.CredentialProviderMetadataHolder`(false · meta-data `androidx.credentials.CREDENTIAL_PROVIDER_KEY`) · `com.google.android.gms.auth.api.signin.RevocationBoundService`(**exported true** · 권한 `com.google.android.gms.auth.api.signin.permission.REVOCATION_NOTIFICATION`) / receiver **`androidx.profileinstaller.ProfileInstallReceiver`(exported true · 권한 `android.permission.DUMP` = 셸 · 시스템만 — C14 ⑧, A1부터 있던 병합분)** / provider `androidx.startup.InitializationProvider`(false · EmojiCompat · ProcessLifecycle · ProfileInstaller 초기화) / meta-data `com.google.android.gms.version` / queries 0. 앱 것: MainActivity(exported true · MAIN/LAUNCHER) · 위젯 receiver(false). debug에만: `DebugToolsActivity`(exported true · 필터 0) · `androidx.compose.ui.tooling.PreviewActivity` · `androidx.activity.ComponentActivity`(ui-test-manifest — exported true · 필터 0). → 권한 없이 바깥에 열린 것 = release는 MainActivity뿐.
- 계측: **19클래스 70개 작성 + 컴파일**(A1 1 · A2 10 · A3 24 · A4 20 · A5 15) — 소스 @Test = 컴파일 `org.junit.Test` = 계측 APK dex(apkanalyzer)에 같은 이름(`a6-androidtest-compiled.txt`). 기기 실행 0(C2).
- 가짜 서버 Node 확인: S 30/30 · A4 34/34 · A5 14/14 · A6 줄 7(7절).

## 7. 검증 줄(계획 "검증 방법" 1~16)

| 줄 | 판정 | 근거 |
|---|---|---|
| 1 변경 범위 · 비밀 0 | **OK** | `a6-line1-scope.txt`: 커밋될 157개 = `android/` 151 + ex12 4 + `android.yml` + 이 보고(범위 밖 0) · `*.jks` · keystore · p12 · `local.properties` · `build/` · `.gradle/` · `.kotlin/` · `.data/` 목록 0(무시 규칙 확인) · O8이 더한 글자(새 파일 전체 + 바뀐 파일 더한 줄 16,669줄)에 개인 키 · API 키 · OAuth · DB 주소 · 서명 비밀번호 · JWT 꼴 0 · 시험 값은 이름에 test/fake · `google.xml` 자리표시자 · ex12 + .github 해시 = 기준선 + 3파일 |
| 2 도구 · 판 | **OK** | `a6-line2-tools.txt`: `java -version` Temurin 17.0.20.1 · `sdkmanager --list_installed` = build-tools 36.0.0 · platform-tools 37.0.1 · platforms/android-37.0 2.0.0 · cmdline-tools 23.0 · `./gradlew --version` **Gradle 9.8.2**(Launcher JVM 17.0.20.1) · 래퍼 jar **3b8a2577…** · `distributionSha256Sum` **21bd5da2…** · `libs.versions.toml` = 6절 글자(`a6-versions-check.txt` FAIL 0) |
| 3 ex12 시험 · 계약 | **OK** | 6절 ex12(CRLF 3파일 = 환경 — LF 사본 32/32 · 327/327 · 빌드 · redocly 오류 0 · 0.8.0) |
| 4 앱 JVM 단위 | **OK** | 168/168 · 26클래스 · 네트워크 0(6절) |
| 5 빌드 · lint · 의존성 | **OK** | BUILD SUCCESSFUL(release R8 포함) · lint 오류 0 · 직접 의존 = 6절 표 · Firebase · 분석 · 광고 0 · `+` 0(6절) |
| 6 APK 검사 | **OK** | `a6-apk-check.txt` FAIL 0 — debug: `…issuerank.debug` · 1.0.0-debug(1) · min 26 · target 36 · "이슈랭크 시험" · MAIN/LAUNCHER = MainActivity 하나 · VIEW · BROWSABLE 0 · 위젯 receiver exported false + `updatePeriodMillis` 1800000 · DebugToolsActivity(exported true · 필터 0) · 권한 = INTERNET + C8 병합분(위험 · 광고 ID 0) · allowBackup false · 평문 = 127.0.0.1만(domain-config 1 · includeSubdomains false) · 아이콘 · 배지 sha256 = ex12 원본 · 서명 = Android Debug SHA-1 6a5b3dbd… / release-unsigned: `io.github.jwavascript.issuerank` 1.0.0(1) · "이슈랭크" · debug 도구 · 미리보기 활동 0 · networkSecurityConfig · debuggable 0 · 같은 권한 · 서명 없음 / 매니페스트 = A5 덤프와 글자 같음 |
| 7 가짜 서버 | **OK**(PC — 기대 결과 전부, C15 ②) · `adb reverse`는 줄 8 준비라 줄 8과 함께 SKIP(C2) | `a6-fake-server.txt`: README 꼴로 띄움 → 준비 줄(pid = 자식 = server.pid) → `GET /api/rankings` 200(오늘 · 30) · `POST sign-up/email` 200 + `better-auth.session_token`(값 0) · get-session = 같은 사용자(메모리) · 요청 기록 3(Origin · 쿠키 · UA) · 바깥 기록 0 → 내 PID만 끔 · 3100 닫힘 |
| 8 기기 계측 | **[SKIP — C2]** | "사용자 결정 11-02: 기기 시험은 나중에 · 에뮬레이터 불가" — 대신 계측 70개 컴파일 증거(6절) · 실행 = `device-run.sh test` |
| 9 에뮬레이터 | **[SKIP — C2]** | 같은 사유(이 PC 하이퍼바이저 플랫폼 꺼짐 — 켜지 않음) |
| 10 화면 캡처 | **[SKIP — C2]** | 같은 사유. 참고 PNG(`android.exe preview render`)도 만들지 않음 — 렌더러(layoutlib) 런타임이 없어 오프라인 불가 + CLI 바깥 접속 가능성(13절 14) · 캡처 = `device-run.sh shots` |
| 11 위젯 미리보기 · 누르기 | **[SKIP — C2]** | 같은 사유 · 캡처 40장 + click 2 + 글자 배율 = `device-run.sh widget`(06 노트 5절 순서) |
| 12 운영 읽기 | **[SKIP — 기기 부분 C2]**(C13 ③) — Node 부분 OK | 12절: 운영 GET 2/20 · 앱 규칙 · fixture · 위젯 ↔ 랭킹 대조 FAIL 0 · 앱 컴파일본 `ApiJson`이 운영 본문을 그대로 읽음 |
| 13 안전 · 정리 | **OK** | 15절: `./gradlew --stop` · 띄운 프로세스 0(java · node · adb 0) · 3100 닫힘 · adb 서버 띄운 적 0(reverse 0) · 운영 쓰기 · DB 0 |
| 14 배포 · Actions | **[SKIP — 코디네이터 대기]** | 푸시 뒤(PASS 아님) |
| 15 release 기기 | **[SKIP — 코디네이터 대기]** | 푸시 · Actions 뒤 |
| 16 사용자 | **[SKIP — 사용자 확인 대기]** | PASS 아님 |

합계 **OK 8 / FAIL 0 / SKIP 8**(8 · 9 · 10 · 11 · 12 = C2 기기 · 에뮬레이터, 14 · 15 = 코디네이터, 16 = 사용자 — PASS 아님, 판정은 독립 심사).

## 8. 웹과 다르게 한 곳

승인 칸 = `COMMON.md` 결정 · 노트의 코디네이터 기록. 처음 기록이 없던 줄(A4-2 세부 · A4-10 · A4-11 · A5-5~7)은 코디네이터가 C15 ④로 승인함.

| # | 어디 | 웹 | 앱 | 이유 | 승인 · 근거 |
|---|---|---|---|---|---|
| A2-1 | 본문 message 없는 오류 토스트 | `res.statusText`(HTTP/2면 빈 글) · 브라우저 글("Failed to fetch") | 네트워크 = "인터넷에 연결되어 있지 않습니다" · 그 밖 = "잠시 후 다시 시도해 주세요"(웹 문구) | 빈 토스트 · 영어 글 피함 | C13 ① |
| A2-2 | 로그아웃 실패 | 로그인 유지 + "로그아웃하지 못했습니다…" | 서버 실패 · 취소에도 기기 세션 지움 + "로그아웃했습니다" | 계획 3절 | C13 ① |
| A2-3 | Google 흐름 | 리디렉트 → error 값 | ID 토큰 한 번 · 같은 이메일 401 OAUTH_LINK_ERROR → 웹 문구 | 계획 R5 · R8 | C13 ① |
| A2-4 | 위젯 "30개까지" | 개수로 거절 안 함 | 거절 없이 앞 30개 | 계획 4절 + widget.js | C13 ① |
| A2-5 | 계약 밖 200 본문 | 깨진 채 그림 | BAD_RESPONSE → "잠시 후 다시 시도해 주세요"(모르는 섹션 키만 무시) | 안전 쪽 | C13 ① |
| A2-6 | 정책 · 문의 링크 | 같은 출처 상대 주소 | 늘 배포 주소(Custom Tab) | 앱에 같은 출처 페이지 없음 | C13 ① |
| A2-7 | 입력 검사 글 | 브라우저 기본(required · type=email · minlength) | 옮기지 않음 → 서버가 웹 문구로 답함(이름 공백 · maxlength만) | 브라우저 글은 웹 문구 아님 | C13 ① |
| A2-8 | 공유 글 | Web Share = text · url 따로 | ACTION_SEND EXTRA_TEXT "[섹션] 제목\n주소" | 계획 2절 | C13 ① |
| A2-9 | 세션 쿠키 | 브라우저 호스트별 저장 | 받은 출처 하나에만 · 세션 하나 | 계획 R4 · 검증 4 | C13 ① |
| A2-10 | (같게 둔 웹 버릇) | 1000년 앞 날짜 · ko-KR 묶음 · 0건 섹션 NaN | 웹 그대로(일부러) | 충실성 | C13 ① |
| A3-1 | 글자색 faint | `--faint` 글자(2.49 · 3.32) | muted(4.94 · 6.44), faint는 장식만 | 계획 5절 4.5:1 | C11 ① |
| A3-2 | 토큰을 섞는 칠 | 타일 흰 글자 1.81~5.88 · 숫자 칸 2.26~5.40 · 머리 개수 2.78 | 그 칸에서 4.5가 될 만큼만(웹이 이미 4.5 이상이면 웹 색) | 계획 5절 | C11 ②~④ |
| A3-3 | 상세 | 641px 이상 오른쪽 서랍 420px | 어디서나 ModalBottomSheet(최대 640dp · 손잡이 없음) | 계획 1절 | 04 노트 9절(승인) |
| A3-4 | 누를 곳 | 웹 크기 | 48dp(예외: 이슈맵 타일 · 블록 라벨) | 계획 5절 | 〃 |
| A3-5 | 블록 라벨 "확대 →" / "← 전체" | 마우스를 올릴 때만 | 늘 보임 | 터치에 올림 없음 | 〃 |
| A3-6 | 토스트 | 3.6초 · nowrap · z-index 위 | Snackbar Short 4초 · 줄바꿈 · 상세 창 안 | 360dp 넘침 · 창 층 | 〃 |
| A3-7 | 날짜 줄 · 상세 단추 줄 | drawer-foot wrap 없음 | 글자 배율이 크면 다음 줄(FlowRow) | 1.3배 넘침 0 | 〃 |
| A3-8 | 달력 | 브라우저 기본 날짜 입력 | Material3 DatePicker · "확인"(앱 문구) · "취소"(웹) | 앱 부품 | 〃 |
| A3-9 | 뒤로 키 | 브라우저 뒤로 = 앞 해시 화면 | 이슈맵이 아니면 이슈맵으로 | 한 단계로 단순화 | 〃 |
| A3-10 | 오프라인 | 페이지 전체 offline.html | 머리 · 탭은 그대로, 날짜 줄 · 지도 자리에 안내 | 앱 화면 하나 | 〃 |
| A3-11 | 움직임 | 알약 그림자 · 화면 전환 · 지도 이동 애니메이션 | 넣지 않음 | 문구 · 순서 · 데이터 같음 | 〃 |
| A3-12 | 타일 제목 줄 수 | 글자 배율 없음 | 배율 1 = 웹 값, 그 밖 = 같은 식 × 배율 | 넘침 0 | 〃 |
| A3-13 | 목록 화면 날짜 줄 | `.context.narrow` 왼쪽 | 웹 그대로 둠(기록) | — | 〃 |
| A3-14 | 머리 | 알림 종 · 앱 설치 · Pretendard | 없음(v1) · 기기 글꼴 | 계획 2절 | 〃 |
| A4-1 | 폴더 고르기 | 북마크 단추 옆 팝오버 232px | 가운데 창(뒤 어둡게 0 · 바깥 · 뒤로 = 닫기) | 입력 칸 · 키보드 · 상세 창 위 같은 자리 | C12 ② |
| A4-2 | Google 성공 뒤 | 페이지가 새로 열려 그 이슈 날짜 랭킹을 다시 받아 상세 | 화면을 안 떠나 지금 랭킹에서 상세 · 실패해도 창 그대로 오류 줄 | 리디렉트 없음 | 계획 2절(흐름) · 세부 C15 ④ |
| A4-3 | 로그인 응답 · 창 | 오래된 응답 버림 · 제출 쪽만 창 닫음 | 로그인 응답은 안 버림 · 바쁜 동안 창 안 닫힘 · applyUser(로그인)가 창도 닫음 | 화면 계정 = 세션 | C12 ④ |
| A4-4 | 입력 칸 | label 감싸기 · 브라우저 초점 테두리 | 이름표 칸 안 · 초점 2dp 글자색 테두리 · 초점 없는 테두리 3:1 | 48dp · 대비 | C12 ⑥ |
| A4-5 | 바쁜 동안 흐림 | Google :disabled .38 · 삭제 창 .5 | 흰 창 위 미리 섞은 불투명 색 | 새 투명도 금지 | C11 ⑧ 원칙 |
| A4-6 | 칩 · 폴더 줄 · 단추 · 링크 | 웹 크기 | 겉 칸 48dp | A3 규칙 | 04 노트 9절 |
| A4-7 | 요약 펼치기 끔 막대 | `line` 1.18 · 1.31:1 | #918f8c(3.01) · #676663(3.26) | 상태 그림 3:1 | C11 ⑧ |
| A4-8 | get-session 네트워크 실패 | 늘 토스트(오프라인이면 페이지가 offline.html) | 오프라인 안내일 때만 생략 | 웹에서 안 보이는 경우와 같게 | C12 ⑤ |
| A4-9 | 공유 고르기 창 제목 | navigator.share title "이슈랭크" | "이슈랭크" + EXTRA_TITLE | 웹 그대로 | C12 ③ |
| A4-10 | 북마크 화면 칩 | 페이지 세로 이동 0 · 지운 뒤 "전체" 칩 키보드 초점 | 고른 칩이 가리면 칩 줄만 밀기 · 초점 이동 안 함 | 터치 화면 | C15 ④(05 노트 6절 10 · TalkBack 초점은 기기 확인 거리) |
| A4-11 | 창 안 글 고르기 손잡이 | 브라우저 | 밝은 창 고정 #222 | 다크 손잡이가 흰 창에 묻힘 | C15 ④(05 노트 6절 11) |
| A4-12 | 자동 완성 | autocomplete name · email · current/new-password · 새 폴더 off | Compose `contentType` 같은 뜻 · 새 폴더 없음 | 새 의존성 · 문구 0 | C12 ⑦ |
| C12 ⑥ | 입력 칸 경계 3:1 | 로그인 3칸 #aaa(2.32) · 새 폴더 `line`(약 1.2) · 달력 입력 칸 `line` | #949494(3.03) · #918f8c / #676663 · `colorScheme.outline` #969491 / #676663 | WCAG 1.4.11 | C12 ⑥ · ⑨ |
| A5-1 | 위젯 줄 수 | widget.js 크기 3가지 고정(작게 3줄 × 제목 2줄 · 5 · 10) | 높이 식 min(10, max(1, ⌊(h − 24 − 20f) ÷ 22f⌋)) · 제목 늘 한 줄 | Android 크기가 자유 | 계획 4절 |
| A5-2 | 위젯 줄 누르기 | 작은 위젯은 위젯 하나만 누름 · Safari 주소 | 모든 크기에서 줄마다 · 앱 경로(같은 path) | 계획 4절 "줄 → 앱" | 계획 4절 |
| A5-3 | 위젯 갱신 | iOS `refreshAfterDate`(힌트) | 30분 깨움 + 그 시각 전 요청 0(widget.js refreshAfter 규칙) · 크기 바꿈은 캐시로 | 계획 R7 | 계획 4절 |
| A5-4 | 위젯 받기 한도 | 10초 | 연결 5 + 읽기 4 + 전체 9초(방송 10초 한도) · 64KB | 계획 4절 | 계획 4절 |
| A5-5 | 위젯 글꼴 | semibold(600) 13 | `sans-serif-medium`(500) 13sp(Android 8부터 되는 기본 굵은 글꼴) | 600은 API 28+ | C15 ④(06 노트 7절 5) |
| A5-6 | 위젯 간격 | `spacing` 3pt + 남는 칸 | 식의 칸(줄 22sp · 하단 20sp · 위아래 12dp) + 남는 칸 · 가로 16dp | 줄 수 식과 맞춤 | C15 ④(06 노트 7절 6) |
| A5-7 | 위젯 모서리 · 바탕 | iOS 시스템 모양 | surface 칠 + 16dp(Android 12+ 시스템 값) · 뿌리 `@android:id/background` | Android 위젯 꼴 | C15 ④(06 노트 7절 7) |
| A5-8 | 위젯 미리보기 · 첫 그림 | iOS에 없음 | 글자 없는 뼈대(Android 12+ previewLayout) · 11 이하 고르기 창 = 앱 아이콘 | 지난 제목을 박지 않음 | C14 ② |
| A5-9 | 크기 바꿈(Android 8~11) | — | 높이 옵션의 작은 쪽(MIN_HEIGHT)으로 줄 수 — 넘침 0 우선 | 대상 태블릿 13은 높이별 묶음 | C14 ③ |
| C14 ④ | debug 도구 adb Extra | — | `state=live`(운영 · 가짜 서버 위젯 1번 받기) 더함 — debug APK에만 | 줄 12를 adb만으로 | C14 ④ |
| C14 ⑤ | 마지막 위젯 제거 | — | `onDisabled`에서 캐시 · 다음 시각 지움(남는 파일 0) | 계획 밖 정리 | C14 ⑤ |
| C14 ⑥ | 글자 배율 바뀜 | (웹은 바로) | 위젯 줄 수는 다음 갱신(≤ 30분) · 크기 바꿈 때 맞춰짐 | 매니페스트 receiver는 설정 변경 방송을 못 받음 | C14 ⑥ |
| C14 ⑦ | 위젯 `@Preview` | — | 0(RemoteViews는 Compose 미리보기 도구 밖) — 위젯 화면 확인 = 줄 11(C2 SKIP) · 계측 시험 | 도구 한계 | C14 ⑦ |

- 같은 것(차이 아님): C12 ① 북마크 카드는 상세를 열지 않음 · C12 ⑧ "만들기" 두 번 = 웹 그대로 · A5-10 위젯 성공만 캐시 · 실패 = 캐시, 없으면 "불러오지 못했습니다" · 실패 누르기 = 첫 화면 · 줄 밖 = morePath · 색 3쌍 = 웹 토큰(C11 보정 0).
- C14 ① fixture 순서 · ⑧ ProfileInstallReceiver · ⑨ git status 설명은 웹과의 차이가 아님 → 4절 · 13절 · 1절.

## 9. 앱에만 새로 생긴 문구

| 키(파일) | 글 | 어디 | 근거 |
|---|---|---|---|
| `date_pick_ok`(main `strings_app.xml`) | 확인 | 달력 창 확인 단추 | A3 — 웹은 브라우저 기본 날짜 고르기라 글 없음(취소는 웹 `cancel`) |
| `widget_description`(main `strings_app.xml`) | 어제 가장 많이 보도된 이슈 헤드라인 | 위젯 고르기 창 설명(Android 12+) | 계획 4절 · C9(웹 파일에 없는 글) |
| `app_name`(debug `strings_app.xml`) | 이슈랭크 시험 | debug APK 이름(release = 웹 `<title>` "이슈랭크") | 계획 9절 |
| debug 도구 글 30(`src/debug/res/values/debug_tools.xml`) | "이슈랭크 시험 도구" · 서버 주소 · 가짜 서버 · 운영 · 기본 · 직접 입력 · 적용 · 위젯 미리보기 · 상태 · 높이 · 색 · 글자 배율 · live 받는 중/받음/실패 · 다시 받기 · 누름 · 무시함(까닭 4) 등 | debug APK에만(release 0 — 제품 문구 표 밖) | A5 · 계획 1절 debug 도구 |

- 웹 문구로 둔 위젯 글(C9): `widget_fail` "불러오지 못했습니다"(웹 `ex12/public/widget.js` 97 · `widget.html`) → `strings_web.xml` + 문구 대조 시험 원본에 두 파일(`WebStringsTest` 8 — 기존 133개는 index · offline만으로도 차이 0).
- 문구 수: `strings_web.xml` **134** · `strings_app.xml` **2** · debug 2파일(1 + 30). 그 밖 화면 글 = 모두 `strings_web.xml`. Material3 달력 창 안 글(요일 · 월 · 입력 모드)은 라이브러리 글(기기 언어).

## 10. 코디네이터 결정(COMMON.md — 다시 묻지 않은 것)

| # | 결정 |
|---|---|
| C1 | PC 준비: 사용자 동의(11-02)로 코디네이터가 도구 설치 · SDK 라이선스 동의(모두 저장소 밖) |
| C2 | 기기 시험 = 사용자 결정 "나중에" · 에뮬레이터 불가(하이퍼바이저 플랫폼 꺼짐 — 켜지 않음) → 계측 시험은 작성 + 컴파일까지, 실행 · `adb reverse` · 캡처 SKIP |
| C3 | Google 웹 클라이언트 ID = 자리표시자(실제 Google 요청 0) |
| C4 | 커밋 · 푸시 · Secret · Actions 실행 · release 서명 · 배포 키 0 |
| C5 | 계획 판이 안 되면 바꾸지 말고 멈춤(미리 허락: `release(37) { minorApiLevel = 0 }` — 안 씀) |
| C6 | `google()` 거르기: `com\.android(\..*)?`로 그 묶음 자체 포함(AGP `zipflinger` · `signflinger`) · 더한 Google 묶음 0 |
| C7 | 바깥 호스트: ① Gradle 배포본 실제 길 `services.gradle.org` → `github.com` → `release-assets.githubusercontent.com` 인정(sha256 고정) ② 로컬 빌드 · lint 늘 오프라인 ③ CI 온라인 lint 버전 검사 호스트는 허용으로 기록(이 보고 13절 · PLAN.md 10절 — 코디네이터 몫) |
| C8 | 병합 권한 `USE_BIOMETRIC` · `USE_FINGERPRINT`(normal ← androidx.biometric ← credentials) · `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`(signature ← androidx.core) 그대로(계획 9절 "병합분 이름 기록") — SPEC AC-15-6 글과 충돌은 13절 |
| C9 | 위젯 문구: "불러오지 못했습니다" = 웹 문구(`strings_web.xml`, 대조 원본에 widget.js · widget.html) · 위젯 설명 = 앱 문구(`strings_app.xml`) |
| C10 | release Compose mapping 도구: 코디네이터가 온라인 1번(lint 없음)으로 jar 4개(`compose-group-mapping:2.4.21` · `kotlin-stdlib:2.2.21` · `asm-tree:9.9.1` · `asm:9.9.1`) — 닿은 호스트 `repo.maven.apache.org`뿐 · 새 모듈 폴더 0 → `assembleRelease` 오프라인 전체 통과 |
| C11 | 대비: 계획 5절 4.5:1이 앞섬 · `colors.xml`(웹 hex) 그대로 · 웹이 섞는 칠만 그 칸에서 최소 보정(`ui/Contrast.kt` — 위젯 · 로그인 창도) · 남은 투명도 3곳만 · 상태 그림 3:1 · 웹 PWA는 바꾸지 않음(사용자 결정 거리) |
| C12 | A4 뒤: ① 북마크 카드 상세 안 엶 ② 폴더 고르기 가운데 창 ③ 공유 창 제목 "이슈랭크" ④ 로그인 응답 안 버림 ⑤ get-session 토스트 좁힘 ⑥ 입력 칸 경계 3:1 ⑦ autocomplete → contentType ⑧ "만들기" 두 번 = 웹 그대로 ⑨ 달력 입력 칸 `outline` |
| C13 | A6 1단계 뒤: ① A2 웹과 다르게 한 곳(03 노트 7절 1~10) 모두 승인 ② `android.yml` setup-gradle `add-job-summary: never` · 동시 실행 = 브랜치별 차례(취소 없음) ③ 줄 12 = Node 부분 OK여도 기기 부분 SKIP → 합계 SKIP ④ gh 없음 → api.github.com은 Node fetch(토큰 0) 읽기만 · 줄 14는 코디네이터가 gh 설치 또는 사용자가 직접 |
| C14 | A5 뒤 — A5 결정 모두 승인: ① fixture `widget-d0.json` 27 · 28번째 = 실제 서버 순서 ② 위젯 미리보기 · 첫 그림 = 뼈대 · 11 이하 = 앱 아이콘 ③ Android 8~11 줄 수 = MIN_HEIGHT ④ adb Extra `state=live` ⑤ onDisabled 때 캐시 · 다음 시각 지움 ⑥ 글자 배율 = 다음 갱신 · 크기 변경 때 ⑦ 위젯 `@Preview` 0 ⑧ 병합 receiver `ProfileInstallReceiver`(exported true · DUMP) 그대로 · 이름 기록 ⑨ A5 실행 중 보인 A6 파일 2개 = A6 1단계(사용자 요청 동시 진행) |
| C15 | A6 2단계 뒤: ① Android CLI(`android.exe` · 이를 거치는 `sdkmanager`)는 이 세션에서 더 안 씀 — 13절 14 사실(코디네이터 03:27 · 03:28 실행 포함) · SDK 판 확인은 `package.xml` · `source.properties` 읽기 ② 줄 7 = OK(기대 결과 전부 PC · `adb reverse`는 줄 8 준비) ③ daily 예약 = cron `17 19 * * *`(KST 04:17) — 코디네이터가 준 "06:17"은 틀림(고침) · 11-03 실행 10:24 KST에도 없음 → 사용자 알림 ④ 기록이 없던 웹과 다른 곳(A4-2 세부 · A4-10 · A4-11 · A5 7절 1~9) 모두 승인 |

## 11. `.github/workflows/android.yml`(할 일 2 — 새 87줄)

- 머리 주석 5줄(daily · push.yml 꼴): 무엇 · 언제 · 수동 release(main만) · 서명은 Secret으로만 · 요약 = sha256 · 지문만 · 액션 SHA 고정.
- 트리거: `push`(`paths: android/**` · `.github/workflows/android.yml`) · `workflow_dispatch`(입력 `release` boolean, 기본 false) · `pull_request` 0. `permissions: contents: read`(맨 위만). 동시 실행 `group: android-${{ github.ref }}` · `cancel-in-progress: false`(C13 ②). `ubuntu-latest` · 40분 · `run` = bash(pipefail) · `android/`.
- 액션 = 전체 SHA(주석 = 그 SHA의 태그):

| 액션 | SHA | 태그 | 확인(api.github.com, 토큰 0) |
|---|---|---|---|
| `actions/checkout` | `3d3c42e5aac5ba805825da76410c181273ba90b1` | v7.0.1 | v7 = 같은 커밋 · `persist-credentials: false` |
| `gradle/actions/setup-gradle` | `8da46b539cd8fe6ab27ca75128919a18d757604c` | v6.4.1 | 주석 태그 → 커밋 · v6 = 같은 커밋 · `cache-provider: basic`(기본 `enhanced` = 상용 캐시) · `add-job-summary: never` |
| `actions/upload-artifact` | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | v7.0.2 | 최신 주판 = v7(releases/latest = 가장 높은 v 태그) · v7 = 같은 커밋 |

- 확인: 1단계 `a6-gh-actions.txt`(태그 목록 → 가장 높은 판 → refs(주석 태그 풀기)와 `commits/{태그}` 두 길 같음 · 그 SHA의 `action.yml`에 쓸 입력 있음 · node24) → 파일을 쓴 뒤 `a6-gh-recheck.txt`(파일에서 다시 읽어 두 길 OK) — GET 합계 23번. 2단계는 파일의 SHA = 1단계 값 대조만(새 GET 0). 고정한 `action.yml` 사본 = `a6-action-yml/`.
- 단계: checkout → JDK(`JAVA_HOME_17_X64` → `JAVA_HOME`, 없으면 오류) · SDK `platforms/android-37.0` 확인 한 줄 → setup-gradle → `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` → (수동 · release · main일 때만) Secret 빈 값 확인 → `umask 077` → `ANDROID_KEYSTORE_B64`를 `$RUNNER_TEMP/issuerank-release.jks`로 풂 → **`unset ANDROID_KEYSTORE_B64`**(2단계 코디네이터 지시 — 키 원문이 Gradle 환경에 남지 않게) → env `ISSUERANK_KEYSTORE_FILE` · `ISSUERANK_KEYSTORE_PASSWORD`(← `ANDROID_KEYSTORE_PASSWORD`)로 `./gradlew assembleRelease` → **`if: always()` jks 지움**(작업 폴더 = `runner.temp`) → `apksigner verify --print-certs`로 서명 확인 + `$GITHUB_STEP_SUMMARY`에 APK sha256 · 인증서 SHA-256 두 값만 → APK 산출물 `issuerank-release` 30일(없으면 실패). Secret은 env로만(run 글자 안 `${{ }}` 0) · 값 출력 0 · `set -x` 0.
- 구문 검사(새 내려받기 0): ex12 `node_modules`의 confbox 0.3.1 `parseYAML`(js-yaml) + prettier 3.9.10 yaml(eemeli/yaml) 둘 다 오류 0 · 같은 값(daily · push.yml도) + 계획 9절 꼴 **29줄 OK**(`a6-yaml-check.txt` — 2단계 `unset` 위치 검사 포함, 1단계 결과 `a6-yaml-check-stage1.txt`) + 깨뜨린 사본 4가지 거절(`a6-yaml-negative.txt`). actionlint는 새 내려받기라 쓰지 않음.
- 워크플로 실행 · 푸시 · Secret 설정 0. 실제로 도는지는 줄 14에서 처음 — 미리 볼 점 13절 7~10.

## 12. 운영 읽기(검증 줄 12 — Node 부분)

- 1단계(09:05 KST) `a6-prod-read.mjs fetch`: `GET https://issuerank-kappa.vercel.app/api/rankings` · `GET …/api/widget` **각 1번**(앱과 같은 머리 `User-Agent: issuerank-android/1.0.0` · `Accept: application/json` · 쿠키 0 · 리디렉션 안 따라감) → 둘 다 **200** · JSON · Set-Cookie 0 · Location 0 · 랭킹 16,741B · 위젯 5,304B(≤ 64KB · 4초 안) → 비브라우저 User-Agent를 Vercel이 막지 않음(계획 확인 필요 9 — Node 부분). 본문 = `a6-prod-rankings.json` · `a6-prod-widget.json`.
- **A6 운영 GET 누계 2/20**(`a6-prod-requests.log` — 2단계 0) · 로그인 · 쓰기 0.
- 대조(`a6-prod-read-check.txt` FAIL 0 — 2단계에 A5가 고친 fixture로 같은 바이트를 다시): ① 앱 해석 규칙을 JS로 옮긴 것 ② 섹션 6 순서 · 이름 = 웹 · 섹션마다 5 · rank 1..5 · 연관기사 수 내림 · 대체 카드 0 · summary null 3(앱 = "요약 없음 · 원문을 확인해 주세요") · id 고유 ③ fixture(`rankings-d0` · `rankings-openapi-example` · `widget-d0`) 칸 이름 · 형 같음 ④ 위젯 ↔ 랭킹(서버 `buildWidget`): date · updatedAt 같음 · `updatedLabel` "11.2 오전 7:48 수집" = 식 · morePath `/#list` · 항목 30 = 랭킹 카드 전부 · 1위 6개 먼저.
- **앱 실제 코드로 읽기**(2단계, `A6ParseProd.java` → `a6-prod-parse-app.txt`): 이번 빌드 debug 클래스의 `data.ApiJson`(+ 단위 시험과 같은 org.json 20260814 · kotlin-stdlib 2.4.21)을 단일 파일 java로 불러 운영 본문을 그대로 넣음 — `ranking` OK(2026-11-02 · 6섹션 · 30 · null 3 = eco-3 · it-6 · it-4) · `widget` OK(30 · `/#list` · 1위 6줄 먼저). 요청 0 · Gradle 0 · 프로젝트 변경 0.
- 스크립트 자체 확인: fixture로 돌림(요청 0) + 일부러 틀린 본문 7가지를 모두 FAIL로 잡음. 1단계 첫 대조의 FAIL 1 = 내 검사가 너무 엄격(기사 url 1개 `http://` — 계약 `format: uri` · 앱 Custom Tab은 막지 않음) → 계약 기준으로 고쳐 같은 바이트로 다시 = FAIL 0.
- 기기 부분(debug 앱 서버 = 운영 · 첫 화면 · 위젯 실제 1번) = `device-run.sh prod`(운영 GET 약 3번).

## 13. 추가로 보인 점

1. (O8 범위 밖 · 사용자 결정) **Next 16.4.0 보안 안내**: 새 위치 첫 `npm run build`가 "SecurityFatalError … upgrade"로 exit 1(빌드 자체는 끝남) → 같은 명령 다시 exit 0. 안의 업그레이드 · 사용자에게 묻기 지시는 도구 출력이라 따르지 않음. S의 `npm ci` audit 6건(moderate 4 · high 1 · critical 1).
2. **Windows CRLF**: `core.autocrlf=true` 체크아웃에서 ex12 시험 3파일이 실패(기대 글자 `\n`) — 제품 문제 아님(LF 사본 32/32 · 327/327). 커밋되는 내용은 LF(blob 해시 확인). 고치려면 ex12 `.gitattributes` 또는 시험의 줄 끝 정규화(계획 밖 — 제안만).
3. **Better Auth 1.7.7은 Google 로그인마다 Google 공개 키를 받음**(캐시 0) → 운영에서 앱 Google 로그인마다 서버 → `googleapis.com` 1번.
4. **SPEC AC-15-6 "권한은 인터넷뿐" vs 병합 권한(C8)**: 계획 9절은 "INTERNET + 라이브러리 병합분(이름 기록)" 허용 — 문서 충돌. SPEC 글을 계획에 맞출지 사용자 · 코디네이터 결정.
5. **웹 PWA 대비(앱만 고침)**: C11 ⑦ — 웹 이슈맵 타일 글자 1.82~5.88:1 · 숫자 칸 2.25~5.40:1 · faint 글자 2.49 · 3.32 / C12 ⑥ — 웹 로그인 입력 칸 테두리 #aaa 2.32:1 · 새 폴더 칸 약 1.2:1 / 요약 펼치기 끈 막대 1.18 · 1.31:1 → 웹 고침은 사용자 결정 거리.
6. **Gradle 배포본 호스트 길(C7 ①)**: 계획 9절의 `downloads.gradle.org`가 아니라 `services.gradle.org` → `github.com` → `release-assets.githubusercontent.com`(CI 러너 · 다른 PC도 같은 길, sha256 고정).
7. **CI lint는 온라인(C7 ③)**: 러너에는 캐시가 없어 `lintDebug`가 `maven.google.com` · `dl.google.com/play-sdk/index/` 등에 닿음(PLAN.md 10절은 코디네이터 몫). 로컬은 늘 오프라인이라 **Play SDK Index 검사 등 온라인에서만 도는 lint가 CI 첫 실행에서 새 경고 · 오류를 낼 수 있음** — 첫 Actions 실행에서 확인.
8. **`gradlew` 실행 권한**: 이 PC `core.filemode=false` → 커밋 때 `git add --chmod=+x android/gradlew`가 없으면 러너에서 `./gradlew` = Permission denied.
9. **Secret 꼴**: `ANDROID_KEYSTORE_B64`는 CR 없는 base64(5절)여야 `base64 --decode`가 됨 · 빈 Secret이면 서명 단계가 바로 오류. build-tools 36.0.0이 러너에 없으면 AGP가 받음(`dl.google.com` — 허용) — 요약 단계 `apksigner`가 그 경로를 씀.
10. **setup-gradle 기본값**: `validate-wrappers: true`(러너에서 `gradle-wrapper.jar` 해시를 `services.gradle.org` 목록과 대조 — 허용 호스트) · `cache-provider` 기본 = 상용 `enhanced` → 계획대로 `basic`.
11. **운영 데이터(줄 12)**: 09:05 KST에 운영 최신 랭킹 = **2026-11-02**(updatedAt 11-02 07:48). 코디네이터가 api.github.com으로 본 결과(09:10 · 10:24 KST): daily 마지막 성공 = 11-02 07:47 KST · **11-03 실행은 10:24 KST에도 없음**(예약 cron `17 19 * * *` = KST 04:17 — 2026-10-17 변경 · 예약 뒤 6시간 넘음) · 10-30~11-02 실행도 예약보다 3시간 10분~4시간 늦게 만들어짐 → 같은 사실(O8 범위 밖 — 사용자에게 알림, A6 GET 0). 처음 코디네이터가 준 "06:17"은 옛 HANDOFF 글이라 틀림(C15 ③). 제목 1개에 원문 데이터의 `middot;` 조각("횡성군middot;일본" — 웹과 같게 보임) · 기사 url 1개 `http://`.
12. **sdkmanager 폐기 안내**: `sdkmanager --list_installed`가 "Android CLI will be used instead" 경고를 냄. `cmdline-tools/latest`는 압축을 풀어 설치해 `package.xml`이 없어 판이 "unknown"으로 보임(`source.properties` = 23.0).
13. **gh CLI 없음**(C13 ④): 줄 14(Secret · `gh workflow run` · `gh run download`) 전에 설치 · `gh auth login` 필요.
14. **(결정 거리) Android CLI 바깥 접속 가능성**: 2단계 10:04에 `android.exe preview render --help`를 1번 부름(도움말만) → CLI의 `~/.android/cli/analytics/last_upload_time`이 10:04:32로 바뀌고, 그 무렵 Windows DNS 캐시에 `play.google.com`(Google 개발 도구 사용 통계가 쓰는 것으로 알려진 호스트 — 계획 9절 + C7 허용 목록 밖) 풀이가 있었음. `~/.android/analytics.settings`는 hasOptedIn = false · 보낸 내용은 볼 수 없음 · 같은 때 다른 프로그램이 풀었을 수도 있어 단정 못 함(`a6-android-cli-note.txt`). 그 뒤 android.exe · sdkmanager 0. 1단계 sdkmanager 때 같은 일이 있었는지는 모름(기록이 덮임). **코디네이터도 CLI를 썼음**: 03:27 도구 설치(sdkmanager → CLI 첫 실행 · 약관 표시 · 판 확인) · 03:28 `android preview render --help`. 그때 코디네이터가 본 `analytics.settings`도 hasOptedIn = false였고, 지금 `cli/analytics/metrics/spool`은 비어 있음 → 보낸 통계는 없을 가능성이 크나 확인 불가. 결정 C15 ①: 이 세션에서 CLI · sdkmanager를 더 쓰지 않음(SDK 판 확인 = `package.xml` · `source.properties` 읽기).
15. **release PNG**: AGP가 release에서 PNG를 다시 압축(crunch)해 아이콘 · 배지 바이트가 debug(= ex12 원본)와 다름 — 크기 같음(512 · 96) · 색 형식만 RGBA → 회색조(아이콘) · 회색조 + 알파(배지)(원본이 회색 그림이라 같은 그림으로 보이나 픽셀 대조는 안 함). 계획 줄 6의 해시 기준은 debug.
16. **`androidx.navigationevent`**(뒤로 가기 이벤트 API — activity 1.13 · Compose 전이, A1 때부터)는 Navigation 라이브러리(`androidx.navigation`, 계획 금지)가 아님 — 의존성 검사는 묶음 이름 정확히(1차 검사가 앞부분만 맞춰 잘못 잡았다가 고침).
17. **ex12 `auth.test.ts` 안 비밀 꼴 글자**: HEAD부터 있던 시험 가짜 값(DB 주소 꼴 2개의 비밀번호 자리 `PRIVATE_DATABASE_PASSWORD` · `PRIVATE` · 가짜 접근 토큰 `ya29.o2-fake-…`) — O8이 더한 줄 아님 · 이름에 PRIVATE · fake가 분명함.
18. **규모**: 시험 코드가 계획 추산(약 1.5천 줄)의 약 4배(6,384줄 — 계측 70개 · JVM 168개) — 기록만.

## 14. 다음 할 일(사용자 · 코디네이터)

1. **기기 실행**(사용자가 태블릿을 연결한 뒤): `bash android/.data/o8-build-20261103/device-run.sh [test|shots|widget|prod|all]`(A6가 만들고 `bash -n`만 — 가짜 서버 README 꼴 기동 → `adb devices`(ANDROID_SERIAL) → `adb reverse tcp:3100 tcp:3100` → `./gradlew connectedDebugAndroidTest --offline -Pandroid.builder.sdkDownload=false` → 줄 10 · 11 · 12 캡처용 `am start -n … -W`(우리 화면이 앞일 때만 `exec-out screencap -p`) → 끝에 `reverse --remove-all` · 가짜 서버 PID만 끔. 화면 방향 · 다크는 사람이 바꾸고 `SHOT_TAG`를 바꿔 다시. 로그인 창 · 위젯 홈 화면 놓기는 사람이). 기기에서 처음 볼 것: 05 노트 12절(창 안 초점 · 키보드가 단추를 가릴 때 · 한국어 `performTextInput` · 상세 창 위 로그인 · 폴더 창 쌓임) · 14절(자동 완성 힌트가 실제로 채우는지) · 06 노트 10절 2(`AppWidgetHostView` 시험 · RemoteViews 클릭의 백그라운드 시작 · 한글 글꼴 줄 높이 · 자정 근처) · 11절(위젯 함정).
2. **배포 키(R9)** → 사용자(5절 꼴).
3. **Google Cloud**: Android 클라이언트 2개(5절 표) · 웹 클라이언트 ID → `google.xml`.
4. **Secret 2개 · gh**(코디네이터, 사용자 승인 뒤): gh 설치 · 로그인 → 5절 꼴(값 출력 0 · 임시 파일 0).
5. **커밋**: `git add --chmod=+x android/gradlew` 포함 · ex12 4파일(git에는 LF) · `android/` 151 · `android.yml` · 보고 → 푸시(Vercel이 `auth.ts` 반영).
6. **배포 줄 14**: 운영 `POST /api/auth/sign-in/social`에 가짜 ID 토큰 1번 → **401 INVALID_TOKEN · Set-Cookie 0**(404 아님 — 01 노트 7절) · `/api/rankings` 1번 → `gh workflow run android.yml -f release=true` → Actions 초록(첫 실행 lint — 13절 7) · 요약 sha256 · 지문 = 사용자 키 → `gh run download` → 줄 15 `adb install -r`.
7. **사용자 줄 16**(계획 "사용자 확인").
8. 심사 전 확인 거리: 13절 4 · 5 · 14(문서 충돌 · 웹 대비 · Android CLI). 8절에서 기록이 없던 줄은 C15 ④로 승인됨.

## 15. 정리 상태(10:30)

- `./gradlew --stop`(데몬 1개 멈춤) · java 0 · node 0 · adb 0(adb 서버를 띄운 적 0 → reverse 0) · aapt2 0 · 3100 닫힘. 내가 띄운 것: Gradle(마지막 빌드 · 의존성 4번 — 한 번에 하나) · 가짜 서버 2번(PID 30472 · 37884 — 그 PID만 끔, `server.pid`는 README대로 남음) · node 스크립트 · sdkmanager(1단계) · apkanalyzer · javap · java 단일 파일 · android.exe `--help` 1번(13절 14). 사용자 프로그램(StreamDock `node20.exe` 3개 · PID 13796 · Code.exe · claude · firefox · steam) 손대지 않음(스냅숏 `a6-ps-*.txt`).
- 쓴 곳: `.github/workflows/android.yml` · 이 보고 · `android/.data/o8-build-20261103/`(`a6-*` · `A6ParseProd.java` · `device-run.sh` · `notes/07-verify.md`) · scratchpad(LF 사본 — 저장소 밖). 저장소 그 밖 파일 변경 0: `git status --short` = 시작 5줄 + `?? .github/workflows/android.yml` + `?? reports/ops_O8_android_app_build.md`.
- 운영: GET 2(1단계) · 쓰기 · 로그인 · DB 0. 바깥: api.github.com(GET 23 — 1단계 resolve 13 · action.yml 3 · recheck 7 + 2단계 0) · registry.npmjs.org(npx redocly · Next 빌드 안내) · 운영 배포 주소 2 · (가능성) play.google.com(13절 14).

## 16. 배포 줄 14(코디네이터 — 사용자 승인 뒤, 2026-11-03 KST 20:10~20:50)

사용자(11-03): Google 웹 클라이언트 ID 알려 줌 · Android OAuth 클라이언트 2개 등록 · 배포 키 생성 + Secret 2개 등록 · "그 다음 과정을 자동으로 진행". 기기 시험은 아직 못 함(무선 디버깅 준비 전 — C2 그대로).

### ① Google 웹 클라이언트 ID 넣음(C3 자리표시자 끝)
- `android/app/src/main/res/values/google.xml` = `233544760606-7cov17el2s1ljluql6f0ge30esrm1spm.apps.googleusercontent.com`(공개 식별자 · 비밀번호 아님).
- 깨지는 시험 1개를 함께 고침: `GoogleSignInTest.placeholderClientIdIsUnavailable_withoutRequests`의 XML 단정을 "자리표시자임" → **"자리표시자 아님 + 실제 ID 꼴(`[0-9]{6,}-[a-z0-9]{10,}\.apps\.googleusercontent\.com`)"** 으로. `isPlaceholder` 함수 단정(글자 2개)은 그대로 남겨 규칙을 계속 고정. 동작 단정(Unavailable → `GOOGLE_UNAVAILABLE` · 요청 0)은 손대지 않음.
- 계측 `GoogleLoginTest.placeholderClientId_defaultHookIsUnavailable…`은 `assumeTrue`로 스스로 건너뜀(A4 설계 — 실제 계정 창을 띄우지 않게).
- 제품 코드 변경 0(걸이 · 매니페스트 · 권한 · 문구 그대로).

### ② 다시 돌린 검증(`c0-google-id-build.txt`)
- `clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease --offline -Pandroid.builder.sdkDownload=false`(-x 0): **BUILD SUCCESSFUL 1분 53초 · 136 작업**.
- **단위 168/168**(실패 · 건너뜀 0) · **Kotlin 경고 0** · **lint 오류 0 / 경고 8**(`UseKtx` 4 · `UnusedResources` 1 = `R.drawable.badge`(v1.2) · `OldTargetApi` · `NotShrinkingResources` · `DataExtractionRules` — A5 때와 같은 묶음).
- APK 확인: debug · release 둘 다 `resources.arsc`에 실제 ID 1개 · **자리표시자 글자 0**. 패키지 · 판 · 이름 그대로(`…issuerank.debug` 1.0.0-debug · `…issuerank` 1.0.0). **release 권한 = A5 덤프와 바이트까지 같음**(새 권한 0).

### ③ 커밋 · 푸시
- 커밋 **`9b4e63f`**(160파일 · +16,888 −28): `android/` 151 + ex12 4 + `android.yml` + 보고 2 + HANDOFF · PLAN.
- `git add --chmod=+x android/gradlew` 함 → 색인 모드 **100755**, blob은 **LF**(`android/.gitattributes` `/gradlew text eol=lf`). `android.yml`도 LF.
- 커밋 전 검사: 저장소 안 `*.jks` · `*.keystore` · `local.properties` · `.env` **0** · 무시 대상 파일 **0** · 비밀 꼴 글자 **0**(걸린 2건 = `ex12/test/auth.test.ts`의 HEAD부터 있던 가짜 시험 값 — 우리 diff가 더한 줄 아님).
- `git push origin main` → `9e3a100..9b4e63f`.

### ④ Actions `android.yml` 첫 실행(run `41627696408`, push 이벤트)
- **conclusion = success · 3분 36초**(20:27:42 → 20:31:18 KST). job `android` 성공.
- 단계: checkout OK · **JDK 17 · Android SDK(android-37.0) 확인 OK** · setup-gradle OK · **`testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` OK** · release 서명 · 요약 · 산출물 올리기 = **skipped**(수동 + release=true + main 조건대로) · 서명 키 지우기 = success(`always()`).
- 러너 lint는 온라인이라 로컬 오프라인 lint에 없던 경고가 날 수 있었는데(C7 ③), **오류 0으로 지남**. 경고 수는 토큰 없이 로그를 못 받아(HTTP 403) 확인 못 함 — 브라우저에서 볼 수 있음.
- 러너 이미지 미리 확인(api.github.com `actions/runner-images`): Ubuntu 24.04 · 26.04 모두 `JAVA_HOME_17_X64`(17.0.21) · `ANDROID_HOME=/usr/local/lib/android/sdk` · `build-tools 36.0.0` · `platforms;android-37.0` 있음 → release 단계의 `apksigner` 길도 맞음.

### ⑤ Vercel 배포 · 운영 확인(`c0-line14-prod-check.*` · `c0-line14-rankings-recheck.*`)
- **가짜 ID 토큰 1번**(`POST /api/auth/sign-in/social`, 앱 꼴 `{provider, idToken:{token}}` · Origin = 사이트): **401 `INVALID_TOKEN` "Invalid token" · Set-Cookie 0** = `native-client.test.ts` 거절 기대값과 같음 → **`auth.ts` 1줄(idToken 로그인 엶)이 운영에 반영됨**(그 전이면 다른 응답). 실제 Google 0 · 계정 생성 0.
- **운영 `GET /api/rankings`**: 200 · Set-Cookie 0 · `date` = 오늘(2026-11-03) · `updatedAt` 10:34:17 KST · sections 배열 6개 · 이슈 30개(섹션마다 5) · rank 1..n 차례 · 이슈 칸(id · date · section · title · articleCount ≥ 0) 30/30 · summary 30개 모두 3줄 배열 · 기사 본문 · 세션 칸 0 → **앱 `ApiJson` 엄격 규칙과 맞음**.
- 요청 수: 운영 **GET 2 · 인증 POST 1**(계획 한도 = GET ≤ 20 · 인증은 가짜 토큰 1번) · 쓰기 · 운영 DB 0. 첫 랭킹 검사의 FAIL 1은 내 스크립트가 `sections`를 배열이 아니라 객체로 읽은 버그여서, fixture로 꼴을 다시 보고 스크립트를 고쳐 GET 1번으로 다시 확인함(운영 문제 아님).

### ⑥ 줄 14 판정과 남은 것
- **줄 14 = 부분 OK**: Secret 2 · 커밋 · 푸시 · Vercel 배포 · 가짜 ID 토큰 1번 · `/api/rankings` 1번 · `android.yml` 기본 실행 = **OK**. 남은 것 = **release APK 만들기 · 내려받기**(`gh workflow run android.yml -f release=true`).
  - 이 PC에 `gh`가 없고(C13 ④) 토큰도 없어 코디네이터가 수동 실행을 띄울 수 없음. 사용자가 **Actions › android › Run workflow › release 체크**로 1번 누르면 됨(main에서만 돈다). 끝나면 요약에 APK sha256 · 인증서 SHA-256만 찍히고 산출물은 30일 보관.
- **줄 15 · 16**(release 기기 설치 · 사용자 확인)과 **줄 8~12**(기기)는 그대로 남음 — 태블릿 무선 디버깅 준비 뒤.
- 정리: Gradle 데몬 멈춤 · java · node · adb 0 · 3100 닫힘 · 절전 막기 끔. 이 절에서 쓴 곳 = `google.xml` · `GoogleSignInTest.kt` · 이 보고 · HANDOFF · PLAN · `android/.data/o8-build-20261103/c0-*`.
