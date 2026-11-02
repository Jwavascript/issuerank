# PLAN_ops_O8_android_app — Android 네이티브 앱 v1(웹 기능을 앱 화면으로 + Google 로그인 + 홈 화면 위젯)

> 이유: 사용자가 Android 앱을 웹을 여는 껍데기가 아니라 **네이티브 화면으로 새로** 만들기로 정했다(Q1=C, "두 벌 유지 · 범위가 큼"을 알고 선택). Android는 웹 · PWA로 홈 화면 위젯이 안 되므로(D23) 위젯도 이 앱에 넣고, 운영자 계정이 Google 가입뿐이라 Google 로그인도 v1에 넣는다. 서버(ex12)는 지금 계약을 그대로 쓰고 Google ID 토큰 로그인 한 줄만 연다.
> 코드 폴더: 새 최상위 `android/`(ex12 밖 — Vercel · 하루 실행과 무관) + ex12 최소(`server/auth.ts` 1줄 · `openapi.yaml` · 시험). 보고: `reports/ops_O8_android_app_build.md` · `reports/ops_O8_android_app_judge.md`.
> SPEC 근거: US-15 · AC-15-1~6 + US-11 · AC-11-1~4(Android 위젯) — 2026-11-02 SPEC.md 반영. 앱이 웹과 같게 따를 웹 AC: 03-1·2 · 04-1~3 · 05-1·2 · 06-1~4 · 07-1~3 · 08-1~3 · 09-1~5 · 10-1~5 · 13-2·3. 적용 결정: D16 · D20 · D22 · D23 · D26(D35가 일부 바꿈) · D29~D32 · **D33~D36**(확정 — PLAN.md 5절).
> 상태: **확정(사용자 2026-11-02)** — 이번 단계는 기획까지(R11, 문서만). 다음 = **제작은 다른 PC에서**(`agent_prompts/2_build_agent.md`, 아래 8절 "다른 PC에서 이어 하기"). 이 태블릿(Termux)에는 아무것도 설치 · 빌드하지 않는다(R10).

## 0. 사용자 결정 기록(확정 — 다시 묻지 않음)
| # | 질문 | 결정(2026-11-02) | 메모 |
|---|---|---|---|
| Q1 | 앱 구조 | C 네이티브로 새로 | TWA · WebView는 검토 후 제외(초안 기록 — git 이력) |
| R1 | 범위 · 단계 | B 단계: v1 = 읽기 + 이메일 · Google 로그인 + 북마크 · 폴더 + 위젯 | 앱 알림은 v1.2 |
| R2 | 화면 도구 | Kotlin + Jetpack Compose | PC 빌드라 메모리 문제 없음 |
| R3 | 빌드 · 검증 | 확인용 로컬 빌드(다른 PC) + Actions 서명 배포 | 7 · 9절 |
| R4 | 앱 세션 | 세션 쿠키를 Keystore로 암호화 · 요청에 Origin = 사이트 | 서버 변경은 Google 1줄 말고 0 |
| R5 · R8 | Google 로그인 | G1 Credential Manager + Google ID 토큰 → Better Auth, **v1** | 운영자 계정 = Google 가입뿐 |
| R6 | 아침 알림 | v1 = Chrome 웹 알림 그대로(P2) → v1.2 앱 로컬 알림(P3) | 서버 · 비밀값 0 |
| R7 | 위젯 갱신 | 30분 깨움 + widget.js 규칙 · RemoteViews(새 의존성 0) | 4절 |
| R9 | 배포 키 | 다른 PC의 keytool로 만듦(저장소 밖 · 백업 2곳 · Secret 2개) | "Termux에서" 답은 R10으로 바뀜 — 코디네이터가 알림 |
| R10 · R11 | 설치 · 진행 | 태블릿에 설치 0 · 이번은 기획까지(코디네이터가 문서만 커밋 · 푸시) | 코드 · 서버 변경 0 |
| R12 · 기기 | 브라우저 · 확인 기기 | Chrome · 이 태블릿 SM-T875N(Android 13 · 340dpi · 세로 753dp/가로 1205dp)을 PC adb 무선 디버깅(또는 USB)으로 | Custom Tab = Chrome |
| Q5 · Q6 | 배포 · 이름 | 운영자 기기만(Actions 수동 산출물, `/widget` 문구 그대로) · "이슈랭크" · `io.github.jwavascript.issuerank` · 1.0.0(1) | — |

## 이번 범위(v1)
- 앱: 첫 화면 이슈맵 · 랭킹 목록 · 날짜 이동 · 상세(원문 = Custom Tab) · 공유 · 오프라인 안내 · 이메일 가입/로그인/로그아웃 · **Google로 계속하기** · 북마크 · 폴더(고르기 · 만들기 · 칩 · 삭제) · 개인정보처리방침 · 이용약관 링크 + 홈 화면 위젯 + JVM 단위 · 기기 계측 시험 + debug 전용 도구(서버 주소 · 위젯 미리보기) / 새 `.github/workflows/android.yml`.
- ex12: `server/auth.ts` `disableIdTokenSignIn: true → false`(+ 주석) · `openapi.yaml`(sign-in/social의 idToken 본문 · 200 · 404 설명 · 0.7.0 → **0.8.0**) · `test/auth.test.ts` ⑧ 기대값 · 새 `test/native-client.test.ts`. 화면 · `/privacy` · `/widget` · DB · 워크플로 0.
- 새 의존성: 6절 표(글자 고정). npm 0.
- 다음 판(v1.2): 앱 로컬 알림(P3 — 종 → 알림 권한 → 주기 작업이 06:30~10:00 KST에 `/api/widget` 날짜가 오늘이면 하루 1통, 서버 0). 그 판 계획에서 자세히.
- 하지 않음: 앱 알림 · FCM · 앱 링크(공유 주소를 앱으로) · `/card` · `/widget`(웹 그대로) · 랭킹 · 북마크 응답의 기기 저장(AC-13-2와 같게) · 분석 · 광고 · 크래시 SDK · Navigation 라이브러리 · iOS · Glance · Robolectric · Play 스토어 · bearer 플러그인 · 하루 실행 · daily/push 워크플로 · ex01~ex11 · `frontend/` · `web/` · 이 태블릿에 설치.

## 1. 앱 구조(Kotlin + Compose)
| 층 | 내용 |
|---|---|
| 화면 | `MainActivity` 하나 + Compose: 머리(로고 · 날짜 줄 · 계정) · 탭 3개(이슈맵 · 랭킹 목록 · 북마크 — 웹 해시 화면과 같음) · 상세(ModalBottomSheet) · 폴더 고르기 · 로그인 창 · 폴더 삭제 확인 · 토스트(Snackbar + 단추 — "되돌리기" · "북마크 보기" · "로그인"). 화면 이동 라이브러리 없이 상태로 |
| 상태 | ViewModel(lifecycle 2.11.0) 하나: 랭킹 · 날짜 · 사용자 · 북마크 · 폴더 · 요약 펼치기. 돌리기 · 다시 열기 때 날짜 · 탭 유지, 첫 화면 = 이슈맵 |
| 데이터 | 순수 Kotlin(Android 클래스 없음 → JVM 시험): HTTP(`HttpURLConnection` — 새 라이브러리 0, 코루틴 IO) · 세션 쿠키 · Origin · JSON(`org.json` — 프레임워크) · 모델 · 날짜(KST) · squarify · 공유 글 · 앱 경로. Google = Credential Manager 호출을 감싼 인터페이스(시험에서 가짜로) |
| 위젯 · debug | `AppWidgetProvider` + RemoteViews(Kotlin) · debug 전용 `DebugToolsActivity`(서버 주소 · 위젯 미리보기, debug APK에만) |
| 규모(추산 ±30%) | 앱 약 4.5~5.5천 줄 + 시험 약 1.5천 줄 · ex12 약 300줄(대부분 시험) |

## 2. 기능 → 앱 화면(웹 US · AC 기준 — 문구 · 순서 · 데이터는 웹 그대로)
| 웹 기능(근거) | 앱 화면 · 규칙 |
|---|---|
| 섹션별 Top 5 · 갱신 시각(AC-03 · 05-1) | `GET /api/rankings`(날짜 생략 = 최근 수집일) · 순서 · 동점은 서버 그대로(D16) · "… 수집" = 웹 `fmtUpdated` 꼴 |
| 날짜 이동(AC-09-1~5) | 이전 · 다음 · "오늘" · 달력(KST 오늘 이후 막음) → `?date=` · 404 → "해당 날짜의 랭킹 데이터가 없습니다" · 지난 날 "n월 n일의 주요 이슈" · 지난 날 카드도 북마크 |
| 이슈맵(D20) | Compose Canvas: 웹 `squarify`를 그대로 옮김(같은 입력 → 같은 사각형) · 섹션 블록 · 색 · 작은 타일 "n위 · m건" · 640dp 이하 세로 쌓기 · "확대 →" / "← 전체" · 누르면 상세 |
| 목록 · 카드 · 상세(AC-04-1~3, O1 · D24) | 순위 · 제목 · 3줄 요약 또는 "요약 없음 · 원문을 확인해 주세요" · 연관기사 수 · "기사 원문" · 북마크 · 공유 · "요약 펼치기"(기본 켬) / 상세 = 요약 · 연관기사 수 · 수집 시각(대체 섹션은 날짜만) · 원문 = Custom Tab(Chrome) |
| 이메일 가입 · 로그인 · 로그아웃(AC-06-1·2) | 이름 · 이메일 · 비밀번호(8~128자) · 회원가입 ↔ 로그인 · 오류 문구 · 토스트 = 웹 · 앱 시작 때 `get-session` |
| Google 로그인(AC-06-3·4 → AC-15-2) | 로그인 창 "Google로 계속하기"(웹과 같은 버튼 · 문구) → Credential Manager `GetSignInWithGoogleOption`(serverClientId = 웹 클라이언트 ID · nonce) → `POST /api/auth/sign-in/social` `{provider:'google', idToken:{token, nonce}}` → 세션 쿠키. 같은 이메일의 이메일 계정 = "이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요"(잇지 않음, D26) · 취소 = "Google 로그인을 취소했습니다" · 그 밖 = 웹 문구 |
| 북마크 · 폴더(AC-07 · 08 · 10, O6) | 폴더 고르기(개수) · 새 폴더(1~16자) · 해제(+ "되돌리기") · 북마크 화면(최근 저장 순 · 칩 · 개수 · "북마크한 기사가 없습니다") · 폴더 삭제(확인 창에 개수) · 비로그인 · 401 → "북마크하려면 로그인이 필요합니다" / "로그인이 필요합니다" + 로그인 |
| 공유 · 오프라인(AC-13-2·3) | 공유 창(`ACTION_SEND`): `[섹션] 제목` + `https://issuerank-kappa.vercel.app/s?issue=<id>`(웹 `issueShare` 글 그대로) · "이슈랭크 공유" / 요청 실패 = offline.html 문구("인터넷에 연결되어 있지 않습니다" · "연결되면 다시 시도해 주세요. 랭킹은 매일 새벽 갱신됩니다." · "다시 시도") |
| `/privacy` · `/terms` · 문의 | Custom Tab으로 웹 페이지 |
| 아침 알림 · `/card` · `/widget` · 설치 UI · Analytics | 앱에 없음 — 알림은 Chrome 웹 알림 그대로(v1.2 P3) · 앱 측정 0(아침 방문자 지표는 웹만 셈) |

## 3. 서버 · 세션 · Google(ex12 변경)
| 항목 | 내용 |
|---|---|
| 그대로 부름 | 공개 `GET /api/rankings[?date=]` · `GET /api/widget` / 인증 `POST /api/auth/sign-up/email` · `sign-in/email` · `sign-in/social`(idToken) · `GET get-session` · `POST sign-out` · `/api/bookmarks`(GET · POST · DELETE `/{issueId}`) · `/api/folders`(GET · POST · DELETE `/{folderId}`) |
| 출처 검사(Better Auth 소스) | GET은 안 봄 · POST에 **Cookie가 있으면 Origin(없으면 Referer)이 trustedOrigins여야 하고 둘 다 없으면 403 `MISSING_OR_NULL_ORIGIN`** · 쿠키 · Origin · Fetch Metadata가 모두 없으면 통과. 개인 API(Hono)는 세션만 봄 |
| 세션(R4) | 응답 `Set-Cookie`의 `__Secure-better-auth.session_token`(로컬 http는 `better-auth.session_token`)만 저장 · 갱신(7일 · 하루마다 연장) · 만료 · `Max-Age=0`이면 지움 → 우리 출처 요청에만 `Cookie`. 모든 POST · DELETE에 `Origin: <사이트 출처>`(비브라우저라 브라우저 CSRF 방어와 무관 — curl과 같음). 보관 = SharedPreferences + AndroidKeyStore AES-GCM(security-crypto는 2025 폐기 — 공식 대안 = 플랫폼 API + Keystore) · `allowBackup=false` · 로그 · 화면 출력 0 · 비밀번호 · Google 토큰 저장 0 · 풀 수 없으면 로그아웃 상태 · 로그아웃 = `sign-out` + 로컬 삭제(서버 실패해도) |
| 그 밖 머리 | `User-Agent: issuerank-android/{versionName}`(기기 모델 안 보냄) · `Accept: application/json` · 연결 5초 · 읽기 10초(위젯 4초) · 리디렉션 안 따라감 |
| Google ID 토큰(D35) | `auth.ts` `disableIdTokenSignIn: false` 한 줄(D26 "본문 idToken 로그인 닫음"을 바꿈). Better Auth 검사(소스): Google 공개 키(`googleapis.com/oauth2/v3/certs`) · RS256 · 발급자 · aud = `clientId`(지금 웹 클라이언트 ID 그대로) · 1시간 · nonce(보내면). 잇기 금지 · 토큰 저장 0 · scope · 이름 · 사진 규칙 그대로(D26). 다른 앱은 우리 웹 클라이언트 ID로 토큰을 못 받음(같은 프로젝트에 등록한 패키지 + 서명만 — Android 클라이언트). 서버가 Google 공개 키를 받는 바깥 요청이 새로 생김 |
| openapi 0.8.0 | `/api/auth/sign-in/social` 요청에 `idToken { token, nonce? }` 꼴 · 200(세션 쿠키 · 돌아가기 없음) · 401(검증 실패) · 404 설명에서 "본문 idToken … 닫음" 지움 |
| 시험(ex12) | `auth.test.ts` ⑧ → 바뀐 기대값 · 새 `native-client.test.ts`(메모리 어댑터 · 실제 Better Auth 1.7.7 · 전역 fetch 가짜): ① 쿠키 · Origin 없는 가입 · 로그인 200 + Set-Cookie ② 쿠키만으로 get-session · 북마크 · 폴더 200 · 201 · 204 ③ 쿠키 + Origin 없는 sign-out 403 · Origin = 사이트 200 + 쿠키 만료 ④ 다른 Origin 403 ⑤ ID 토큰(시험 RSA 키 · 가짜 공개 키): 정상 → 200 + 세션 · 사용자 행(토큰 칸 null · image 빈 값) / aud · 발급자 다름 · 1시간 넘음 · nonce 틀림 · 서명 틀림 → 거절 · 행 0 / 같은 이메일의 이메일 계정 → account_not_linked · 잇지 않음 / 미인증 이메일 = 새 가입만(D26) / 실제 Google 요청 0 |
| /privacy | 그대로 — 새로 모으는 정보 0(Google 항목 · 세션 쿠키 · IP · user agent는 이미 적힘, 앱은 같은 범위 openid · email · profile) |

## 4. 위젯(RemoteViews — 앞 초안 규칙 그대로, 누르면 앱 안으로)
| 항목 | 규칙 |
|---|---|
| 방법 | `AppWidgetProvider` + `RemoteViews`(Kotlin, 새 의존성 0). Glance는 안 씀(R7 — Compose 앱이라도 의존성 · WorkManager가 늘어남) |
| 데이터 · 검사 | `GET /api/widget` 하나(쿼리 · 쿠키 0) · 연결 5초 + 읽기 4초(방송 10초 한도, `goAsync`) · 64KB · 형식 검사 = widget.js(items 1개 이상 · 30개까지 · title 문자열 · path `/` 시작 · date · updatedLabel 문자열, morePath 틀리면 `/`) |
| 줄 수 · 글자 · 색 | `min(10, max(1, ⌊(h − 24 − 20f) ÷ 22f⌋))`(h = 높이 dp, f = 글자 배율 — f=1: 117→3 · 150→4 · 260→9 · 290→10) · 제목 13sp 한 줄 "…" · 하단 10sp = `updatedLabel` · 숫자 · 섹션 이름 0 · 스크롤 없음(D22) · 사이트 토큰 밝게/어둡게 · Android 12+ 높이별 RemoteViews 묶음(≤ 16) |
| 누르기 | 줄 → `MainActivity`(PendingIntent `FLAG_IMMUTABLE`) + 앱 경로: `/?issue=<id>` → 그 날짜 랭킹 → 상세(없으면 "이슈를 찾을 수 없습니다" — O1과 같음) · 줄 밖 · 하단 `/#list` → 랭킹 목록 · 실패 안내 → 첫 화면. 경로 → 화면은 순수 함수(시험) |
| 실패 · 캐시 · 갱신 | 성공만 캐시(앱 전용 파일) · 실패면 캐시, 없으면 "불러오지 못했습니다" · `updatePeriodMillis` 1800000(공식 하한 30분) + widget.js 규칙(오늘 데이터면 3시간마다, 아니면 KST 04:30 뒤에만) |
| 크기 · 미리보기 | 4×2(`minWidth 276dp` · `minHeight 117dp`) · `minResize 130×66dp` · 이름 "이슈랭크" · 설명 "어제 가장 많이 보도된 이슈 헤드라인" / debug 미리보기(고정 JSON · 높이 · 상태 · 다크 · `click n`) |

## 5. 화면 공통
| 항목 | 규칙 |
|---|---|
| 문구 | 웹 글자 그대로(AGENTS) — `strings.xml`의 웹 문구는 `ex12/public/index.html` · `offline.html`에 같은 글자로 있어야 함(JVM 시험이 파일을 읽어 대조), 앱에만 새로 생기는 문구는 보고서에 표로 |
| 색 · 다크 | 웹 CSS 토큰 그대로(`--bg` #f7f7f5/#121211 · `--surface` · `--text` · `--muted` · 섹션 `--s-*` 밝게 · 어둡게) → Compose 색 표 + 위젯 `values-night`(기기 다크 따름) |
| 크기 · 접근성 | 웹 구간(340 · 400 · 640 · 900 · 1200px)을 dp로 · 640dp 이하 = 이슈맵 세로 쌓기 · 목록 한 열 · 이 태블릿은 넓은 배치 · 폰 폭(360dp)은 Compose 시험 `DeviceConfigurationOverride.ForcedSize`로 / 누를 곳 48dp 이상 · 글자 sp · 웹 aria-label → contentDescription · 대비 4.5:1 이상 |

## 6. 의존성 · 판(글자 고정 — `android/gradle/libs.versions.toml`)
| 묶음 | 판 |
|---|---|
| 빌드 | Gradle 래퍼 **9.8.2**(bin sha256 `21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607` · 래퍼 jar `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`) · AGP **9.5.0**(Gradle 9.6 이상 · JDK 17 · build-tools 36.0.0 · 최대 API 37) · Kotlin **2.4.21**(AGP 9 내장 Kotlin — `org.jetbrains.kotlin.android` 안 씀) · Compose 컴파일러 플러그인 `org.jetbrains.kotlin.plugin.compose` 2.4.21(Kotlin과 같은 판) |
| SDK | **compileSdk 37**(Compose 1.12가 compileSdk 37 · AGP 9.2+ 요구 — AGP 9.5는 37까지) · targetSdk **36**(Play 2026-08-31 요건 · Android 17 동작 변경은 받지 않음) · minSdk **26** |
| 화면 | Compose BOM **2026.09.00**(ui · foundation 1.12.1 · material3 1.4.0 — 확인한 최신 안정): `ui` · `ui-graphics` · `ui-tooling-preview` · `foundation` · `material3` / `activity-compose` 1.13.0 · `lifecycle-runtime-compose` · `lifecycle-viewmodel-compose` 2.11.0 · `kotlinx-coroutines-android` 1.11.0 · `androidx.browser:browser` 1.10.0 |
| Google | `androidx.credentials:credentials` 1.6.0 · `credentials-play-services-auth` 1.6.0 · `com.google.android.libraries.identity.googleid:googleid` 1.2.1(Play 서비스 필요 — 태블릿에 있음) |
| 시험 | 단위 `junit:junit:4.13.2` · `org.json:json:20260814` · `kotlinx-coroutines-test` 1.11.0 / 기기 `androidx.test:runner:1.7.0` · `androidx.test.ext:junit:1.3.0` · `androidx.compose.ui:ui-test-junit4`(BOM) / debug `ui-test-manifest` · `ui-tooling`(BOM) |
| 0 | 분석 · 광고 · 크래시 · Firebase · OkHttp · Retrofit · 직렬화 플러그인 · Navigation · Material 아이콘 묶음(아이콘은 웹 SVG를 ImageVector로) |

## 7. 빌드 환경(다른 PC — OS는 아직 모름, 공통 + 다른 곳만)
| 항목 | 공통 | OS별 |
|---|---|---|
| 도구 | git · gh · Node 24.x(ex12 `engines`) · JDK 17 이상(Gradle 9.8 실행 17~27 · AGP 17 — Android Studio 내장 JDK 가능) · Android SDK | Android Studio = Windows · macOS(Apple 칩 · Intel) · Linux x86_64. **Linux ARM은 Studio 미지원**이고 AGP의 공식 aapt2 jar가 windows · linux(x86_64) · osx뿐 → Linux arm64 PC는 쓰지 않음(쓰면 aapt2 우회 — 확인 필요) |
| SDK 패키지 | `platforms;android-37.0`(API 37 — 패키지 이름이 `37.0`) · `build-tools;36.0.0` · `platform-tools`(adb) · (선택) `emulator` + API 36 시스템 이미지. 라이선스는 사용자가 읽고 동의 | 경로 = `ANDROID_HOME` 또는 `android/local.properties` `sdk.dir`(무시 파일) |
| 명령 | `cd android && ./gradlew …` · `cd ex12 && npm …` | Windows = `gradlew.bat` · `npm.cmd` · `npx.cmd` |
| 해시 | 래퍼 zip · jar 대조 | Linux `sha256sum` · macOS `shasum -a 256` · Windows PowerShell `Get-FileHash -Algorithm SHA256` |
| 메모리 | `android/gradle.properties` `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`(Compose · Kotlin 컴파일 — CI 러너에도 맞음) | PC 메모리가 작으면 2g로 내리고 보고 |
| 기기 | 이 태블릿 = 무선 디버깅(Android 11+: 설정 › 개발자 옵션 › 무선 디버깅 › 페어링 코드 → `adb pair IP:포트` · `adb connect`, 같은 Wi-Fi) 또는 USB · 여러 기기면 `ANDROID_SERIAL` | — |
| 에뮬레이터(선택) | 추천: 계측 시험의 기준 = **이 태블릿(API 33 실기기)**, 에뮬레이터는 보조 1대(폰 크기 · API 36)로 같은 시험 한 번 — 하드웨어 가속이 될 때만, 안 되면 SKIP 사유 | 가속 = Windows WHPX(AEHD는 2026-12-31 종료) · macOS Hypervisor.Framework · Linux KVM · VM 안에서는 불가 |
| compileSdk 37 주의 | SDK가 `android-37.0`으로 깔려 AGP가 `android-37`을 못 찾으면 → `compileSdk { version = release(37) { minorApiLevel = 0 } }`(러너 이미지 이슈에서 확인된 꼴) 1줄. 그래도 막히면 멈추고 보고(대체 = compileSdk 36 + BOM 2026.06.01 — 코디네이터 결정) | — |

## 8. 다른 PC에서 이어 하기(제작이 바로 시작하도록)
1. **사용자 — PC 준비(처음 한 번)**: ① git · gh 설치 → `gh auth login`(본인 GitHub) ② Node 24.x ③ Android Studio(또는 JDK 17+ · cmdline-tools) → SDK Manager로 7절 SDK 패키지 · 라이선스 동의(본인) ④ `git clone https://github.com/Jwavascript/issuerank.git` → `git log -1`이 O8 기획 커밋인지.
2. **`ex12/.env`**: O8 제작 · 심사에는 **필요 없음**(시험 = 메모리 · 가짜, 운영 확인 = 배포 주소). 다른 단계의 실제 DB 줄 때문에 옮길 때만: 사용자가 비밀번호 관리자나 암호화한 저장 장치로 직접(채팅 · 메신저 · git · 평문 클라우드 금지) → PC `ex12/.env`(git 무시 · 본인만 읽기) · 값 출력 금지 · `.env.example`로 이름만 확인.
3. **사용자 — 배포 키(R9)**: PC에서 `keytool -genkeypair -v -storetype PKCS12 -keystore <저장소 밖 폴더>/issuerank-release.jks -alias issuerank -keyalg RSA -keysize 2048 -validity 10000` → 비밀번호는 본인만 → 백업 2곳(비밀번호 관리자 + 오프라인 · 암호화 사본) → `keytool -list -v -keystore … -alias issuerank`의 SHA-1 · SHA-256(공개 값)을 적어 둠. 잃으면 업데이트 불가.
4. **제작 — 새 세션**(Codex든 Claude Code든 — AGENTS.md는 도구를 가리지 않음, Claude Code는 CLAUDE.md → AGENTS.md): "AGENTS.md와 HANDOFF.md를 읽고 `agent_prompts/2_build_agent.md`를 O8(`plan/PLAN_ops_O8_android_app.md`, 보고 `reports/ops_O8_android_app_build.md`)로 수행". 첫 명령: `git status --short` · `node -v` · `java -version` · `adb version` → `cd ex12 && npm ci && npm test && npm run build`(기준선 기록) → 래퍼 만들기(5) → 구현 순서.
5. **래퍼 처음 만들기**(저장소에 `android/`가 아직 없음): `android/settings.gradle.kts`를 먼저 만들고 → Gradle 9.8.2 bin zip(`services.gradle.org`)을 받아 sha256 = `21bd5da2…` 대조 → 풀어서 `<풀린 곳>/bin/gradle wrapper --gradle-version 9.8.2 --distribution-type bin --gradle-distribution-sha256-sum 21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607`(`android/`에서) → `gradle/wrapper/gradle-wrapper.jar` sha256 = `3b8a2577…` 확인 → 래퍼 4파일(`gradlew` · `gradlew.bat` · jar · properties)을 저장소에 둠 → 그 뒤로는 `./gradlew`만.
6. **사용자 — debug SHA-1과 Google Cloud**: 첫 debug 빌드 뒤 `./gradlew signingReport`(또는 `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`)의 SHA-1 → O2와 같은 Google Cloud 프로젝트 › Google Auth Platform › 클라이언트 › Android 2개: ① `io.github.jwavascript.issuerank.debug` + 그 PC debug SHA-1 ② `io.github.jwavascript.issuerank` + release SHA-1. 웹 클라이언트 ID(지금 Vercel `GOOGLE_CLIENT_ID` — Google 로그인 주소에 보이는 공개 식별자)를 제작에 알려 줌 → `android/app/src/main/res/values/google.xml`. **보안 비밀번호는 알려 주지 않음**. PC가 바뀌면 debug SHA-1도 바뀜(그때 다시 등록).
7. **사용자 — 태블릿 연결**: 태블릿 개발자 옵션 › 무선 디버깅 켜기 › 페어링 코드 → PC `adb pair IP:포트` · `adb connect IP:포트` · `adb devices`(같은 집 Wi-Fi, 공용 Wi-Fi 금지 · USB도 됨) → 시험이 끝나면 끔.
8. **심사**: 새 세션 `agent_prompts/3_judge_agent.md`(O8, 보고 `reports/ops_O8_android_app_judge.md`) — 같은 PC 가능, 제작 보고는 주장으로만.
9. **코디네이터(사용자 승인 뒤)**: Secret 2개 `gh secret set ANDROID_KEYSTORE_B64`(jks base64) · `ANDROID_KEYSTORE_PASSWORD`(사용자 입력, 값 출력 · 임시 파일 0) → 커밋 · 푸시(Vercel이 auth.ts 반영) → 배포 줄 → `gh workflow run android.yml -f release=true` → `gh run download` → `adb install` → 사용자 줄.
10. **하지 말 것**: 키(`*.jks`) · 비밀번호 · `.env` · Google 보안 비밀번호 · 세션 값을 저장소 · 채팅 · 로그에 · `web/` · `plan/PLAN_web.md` · 승인 없는 커밋 · 푸시 · 이 태블릿에 설치 · 운영 DB 접속 · 에이전트의 실제 Google 로그인.

## 9. 서명 · 배포 · 공급망
| 항목 | 규칙 |
|---|---|
| 서명 | debug = AGP debug 키(설계상 안전하지 않아 배포 불가 — 공식) · `applicationIdSuffix ".debug"` · 이름 "이슈랭크 시험" / release = 8절 3 키 · Actions Secret으로만 서명(`$RUNNER_TEMP`에 풀고 `assembleRelease` 뒤 지움, 환경 변수 `ISSUERANK_KEYSTORE_FILE` · `ISSUERANK_KEYSTORE_PASSWORD`가 있을 때만 서명 설정) · 에이전트는 release 서명 안 함 |
| 배포(Q5) | `android.yml` 수동 실행(release, main) → 산출물 30일 → `gh run download` → PC adb로 태블릿에 설치. 한국 직접 설치에 개발자 인증 아직 미적용(2027 전 세계 · adb 예외) |
| 이름 · 아이콘 | "이슈랭크" · `io.github.jwavascript.issuerank` · 1.0.0(1, 배포마다 +1) · release R8 · 적응형 아이콘 = PWA `maskable-512.png` + 바탕 #161616 · 배지 = `badge-96.png`(복사, 해시 같음) |
| 허용 호스트 | 빌드 = `services.gradle.org` → `downloads.gradle.org` · `dl.google.com`(Google Maven · SDK) · `repo.maven.apache.org` · `registry.npmjs.org`(ex12 `npm ci`) · `api.github.com`(액션 SHA 읽기). `settings.gradle.kts` = `google()`(`androidx.*` · `com.android.*` · `com.google.android.*`만) + `mavenCentral()` · `FAIL_ON_PROJECT_REPOS` · 판 글자 고정(`+` 0). 앱 실행 중 = 배포 주소뿐(+ Credential Manager가 쓰는 Google 서비스) |
| 워크플로 | `android.yml`: 액션 전체 SHA 고정(checkout v7 · setup-gradle v6 `cache-provider: basic` · upload-artifact) · 러너 JDK 17(`JAVA_HOME_17_X64`) · 러너 SDK(android-37.0 있음) · `contents: read` · push(`android/**` · 이 파일) + 수동(release는 main만) · `pull_request` 0 · `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`(+ release · 요약 = sha256 · 인증서 지문만) · 머리 주석은 daily · push.yml 꼴 |
| 권한 | `INTERNET` + 라이브러리 병합분(이름 기록) · 위험 권한 · 광고 ID 권한 0 |

## 10. 시험 · 검증 환경(PC)
- JVM 단위(`src/test`, 네트워크 0): 모델 해석 · squarify(웹 함수를 Node로 돌린 기대값 파일과 소수 셋째 자리까지) · 위젯 규칙 · 세션 쿠키 · Origin · 날짜(KST) · 공유 글 · 앱 경로 · 문구 대조. 시험의 android.jar는 몸통이 비어 있어(공식) 로직은 순수 Kotlin, JSON은 시험에만 `org.json:json`.
- 기기 계측(`connectedDebugAndroidTest`, 태블릿): Compose UI 시험(`createAndroidComposeRule` · `ForcedSize` 360dp · 다크 · 글자 1.3배) + 위젯 RemoteViews 시험(시험 문맥에서 `apply` → 글자 · 줄 수 · 누르기 대상). 서버 = **가짜 서버**(PC `127.0.0.1:3100` — 실제 `createApp(memoryStore)` + Better Auth 메모리 어댑터 · 시험용 Google 검증, O6 · O7 `mock.mts` 꼴, `android/.data/`에) + `adb reverse tcp:3100 tcp:3100`(adb 도움말의 명령) · debug 앱 서버 주소 = `http://127.0.0.1:3100`(debug만 이 주소 평문 허용). Google 흐름은 Credential 호출 인터페이스를 가짜로 바꿔 시험(실제 Google 0).
- 캡처: `adb exec-out screencap -p`(우리 화면이 앞일 때만) · 태블릿 세로 · 가로 × 밝게 · 어둡게.

## 구현 순서(다른 PC)
0. 8절 1~3(사용자 준비 · 키) · 4(새 세션) · 기준선 기록(검증 1 · 2).
1. ex12: `auth.ts` 1줄 · `openapi.yaml` 0.8.0 · `auth.test.ts` ⑧ · 새 `native-client.test.ts` → `npm test` · `npm run build` · `npx @redocly/cli@2.64.1 lint openapi.yaml`.
2. `android/` 뼈대: `settings.gradle.kts` → 래퍼(8절 5) → `build.gradle.kts` · `gradle/libs.versions.toml` · `gradle.properties` · `.gitignore`(`.gradle/ build/ local.properties *.jks *.keystore *.p12 keystore.properties .kotlin/`) · `app/build.gradle.kts` · 매니페스트 · 리소스(문자열 · 색 · 아이콘 복사).
3. 데이터 층(순수 Kotlin) + JVM 시험 → 4. 화면(머리 · 탭 · 날짜 → 이슈맵 → 목록 · 카드 → 상세 → 로그인 창(이메일 · Google) → 북마크 · 폴더 → 공유 · 오프라인 · 링크) → 5. 위젯 · debug 도구 → 6. 가짜 서버 · 계측 시험 · 캡처 · `android.yml`.
7. 제작 보고 → 독립 심사 → 코디네이터(8절 9) → 사용자 줄.
마지막. `cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` · `cd ex12 && npm test && npm run build`

## 검증 방법
- 실행: 다른 PC. Android = `android/`에서 `./gradlew`(Windows `gradlew.bat`) · ex12 = `npm`(Windows `npm.cmd`, Ubuntu 래퍼는 이 태블릿 전용이라 PC에서는 안 씀) · 증거 `android/.data/o8-build-<날짜>/`(심사 `o8-judge-`, 루트 `.data/` 무시).
- adb 허용 = `pair` · `connect` · `devices` · `install -r <우리 APK>` · `connectedDebugAndroidTest` · `shell am start -n <우리 패키지>/…` · `shell am force-stop <우리 패키지>` · `reverse tcp:3100 tcp:3100` · `exec-out screencap -p`(우리 화면이 앞일 때만) · `logcat -d`(우리 태그 · pid) · `shell dumpsys package <우리 패키지>` · 끝에 debug 앱 `pm uninstall`(사용자 동의). 그 밖 앱 · settings · 권한 · input · pull · bugreport 금지, 끝나면 `reverse --remove-all` · `disconnect` · 사용자가 무선 디버깅 끔.
- 한도: 운영(Vercel) = 역할마다 읽기 GET ≤ 20 · 인증 · 쓰기 0(코디네이터 배포 줄의 가짜 ID 토큰 1번만) · **운영 DB 접속 0**(`.env` 불필요) · **실제 Google 로그인 = 사용자 줄만**(본인 계정) · newsdata · OpenAI · 푸시 서비스 0 · 바깥 = 9절 허용 호스트. 소유 프로세스만 종료(`./gradlew --stop` · 가짜 서버 PID), 남은 것 0 확인.
- 판정: 14 · 15 = 코디네이터(푸시 뒤), 16 = 사용자. 그 밖 줄이 모두 OK이고 이것만 남으면 "PASS 보류"(PASS 아님).

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 1 변경 범위 · 비밀 0 | `git status --short` · `git diff --stat` · 작업 전후 `git ls-files ex12 .github` 해시 · 추적 파일 비밀 검사 | 새 · 바뀐 파일 = `android/` · `android.yml` · ex12 4개(`auth.ts` · `openapi.yaml` · `auth.test.ts` · `native-client.test.ts`)뿐 · 그 밖 ex12 · 워크플로 해시 같음 · `*.jks` · `local.properties` · `build/` · `.gradle/` · `.data/` 추적 0 · 비밀번호 · 세션 값 · Google 보안 비밀번호 글자 0 |
| 2 도구 · 판 | `java -version` · `sdkmanager --list_installed` · `./gradlew --version` · 래퍼 jar 해시 · `libs.versions.toml` | JDK 17+ · Gradle 9.8.2 · jar `3b8a2577…` · `distributionSha256Sum` `21bd5da2…` · 판 = 6절 글자 그대로 |
| 3 ex12 시험 · 계약(AC-15-2 · 06-3·4) | `npm test` · `npm run build` · redocly lint | 기존 31파일 + 새 시험 통과(3절 ①~⑤ · ⑧ 새 기대값) · 실제 Google · DB 요청 0 · 빌드 성공 · OpenAPI 오류 0 · 0.8.0 |
| 4 앱 JVM 단위(AC-15-1·5 · 11-1~4) | `./gradlew testDebugUnitTest` | 해석 · squarify = 웹 기대값 · 위젯 규칙 · 세션(두 쿠키 이름 · 갱신 · `Max-Age=0` · 다른 호스트로 안 보냄 · POST · DELETE Origin) · 날짜 · 공유 글 = 웹 · 앱 경로 · 문구 대조 차이 0 — 전부 통과 · 네트워크 0 |
| 5 빌드 · lint · 의존성 | `assembleDebug assembleDebugAndroidTest lintDebug` · `:app:dependencies --configuration releaseRuntimeClasspath` | 성공 · lint 오류 0 · 직접 의존 = 6절 표뿐 · Firebase · 분석 · 광고 0 · `+` 0 |
| 6 APK 검사(debug, AC-15-6) | `aapt2 dump badging · permissions · xmltree --file AndroidManifest.xml`(SDK build-tools) · `apksigner verify --print-certs` | `…issuerank.debug` · 1.0.0-debug(1) · min 26 · target 36 · "이슈랭크 시험" · MAIN/LAUNCHER = MainActivity 하나 · VIEW · BROWSABLE 0 · 위젯 receiver(exported false · 1800000) · debug 도구 화면 · 권한 = INTERNET + 병합분(위험 · 광고 ID 0) · allowBackup false · 평문 = debug 127.0.0.1만 · 아이콘 해시 = ex12 원본 |
| 7 가짜 서버 | PC `127.0.0.1:3100` 기동 · `adb reverse` · Node로 랭킹 · 가입 1번 | 200 · 메모리 저장 · 요청 기록 · DB · 바깥 0 |
| 8 기기 계측(태블릿, AC-15-1~3·5) | `ANDROID_SERIAL=<태블릿> ./gradlew connectedDebugAndroidTest` | 전부 통과: 첫 화면 30개 · 갱신 시각 · 날짜(지난 날 · 없는 날 문구 · 오늘 · 미래 막음) · 카드 · 요약 없음 · 상세 · 이슈맵 타일 30 → 상세 · 가입 · 로그인 · 오류 문구 · 로그아웃(세션 지움) · Google 버튼 → 가짜 자격 → 로그인 · 취소 · 이미 가입 문구 · 북마크(폴더 고르기 · 새 폴더 0 · 17자 거절 · 해제 · 되돌리기) · 칩 · 빈 문구 · 폴더 삭제(개수) · 401 → 로그인 안내 · 공유 글 · 서버 끔 → 오프라인 문구 · 다시 시도 · 360dp · 다크 · 글자 1.3배 넘침 0 · 위젯 RemoteViews(줄 수 · 글자 · 누르기 대상) |
| 9 에뮬레이터(선택) | 폰 크기 API 36 이미지로 8 한 번 | 같은 결과(가속 없으면 SKIP 사유) |
| 10 화면 캡처(사람 판정) | 태블릿 세로 · 가로 × 밝게 · 어둡게 × 이슈맵 · 목록 · 상세 · 로그인 · 북마크(≤ 30장) | 같은 날 웹 화면과 나란히 — 문구 · 순서 · 색 · 넘침 차이 기록(심사 판정) |
| 11 위젯 미리보기 · 누르기(AC-15-4) | debug 도구: 높이 66 · 117 · 150 · 220 · 300 × ok30 · ok3 · cache · fail × 밝게 · 어둡게 · `click 2` | 줄 수 = 공식 · 제목만 · "…" · 하단 = updatedLabel · 넘침 0 · fail 문구 · 누르면 앱 상세(그 줄 id) |
| 12 운영 읽기(AC-15-1) | debug 앱 서버 주소 = 운영 · 첫 화면 · 위젯 실제 1번 + Node `/api/rankings` · `/api/widget` 1번씩 | 날짜 · 제목 · 순서 = API · 요청 ≤ 20 · 로그인 0 |
| 13 안전 · 정리 | 프로세스 · `adb reverse --list` · 연결 | 소유 프로세스 0 · reverse 0 · 운영 쓰기 · DB 0 |
| 14 배포 · Actions(코디네이터) | Secret 2 · 커밋 · 푸시 → Vercel 배포 → 운영 `POST /api/auth/sign-in/social`에 가짜 ID 토큰 1번 · `/api/rankings` 1번 → `gh workflow run android.yml -f release=true` · `gh run download` | 가짜 토큰 = 거절(401 — 404 아님) · Set-Cookie 0 / Actions 초록 · release `io.github.jwavascript.issuerank` · 1.0.0(1) · debug 도구 · 평문 0 · 서명 SHA-256 = 사용자 키 · 값 출력 0 |
| 15 release 기기(코디네이터) | PC adb로 태블릿에 설치 → 운영 첫 화면 · 위젯 | 12와 같음 · 로그인 0 |
| 16 사용자(AC-15-1~6) | 아래 "사용자 확인" | 결과 전 [SKIP — 사용자 확인 대기](PASS 아님) |

- AC → 줄: AC-15-1 → 4 · 8 · 10 · 12 · 15 · 16 / AC-15-2 → 3 · 8 · 14 · 16 / AC-15-3 → 3 · 8 · 16 / AC-15-4 · AC-11-1~4 → 4 · 8 · 11 · 16 / AC-15-5 → 4 · 8 · 16 / AC-15-6 → 5 · 6 · 14 / 웹 회귀 → 1 · 3.

### 사용자 확인(마지막에 한 번에)
- 태블릿의 "이슈랭크" 앱을 연다 → 이슈맵 · 목록 · 날짜 이동 · 상세 · 원문(앱 안 Chrome 탭)을 웹과 비교 → **Google로 계속하기** → 본인 Google 계정 → 웹에서 쓰던 계정으로 들어가 웹에서 저장한 북마크가 보이는지 → 앱에서 북마크 저장 · 해제 · 폴더 만들기 → 웹(Chrome)에서도 같은지 → 이슈 공유(공유 창 글) → 홈 화면 빈 곳 길게 › 위젯 › 이슈랭크 → 추가하고 크기를 바꿔 줄 수 → 줄 누르기(앱 상세) → 다크 모드 · 비행기 모드(위젯 그대로 · 앱 오프라인 안내) → 다음 날 아침 위젯 하단이 오늘 날짜인지(데이터 준비 뒤 약 30분 안) · Chrome 아침 알림은 그대로 오는지 → 로그아웃. 캡처: 화면별 · 위젯 크기별 · Google 계정 고르기 창.

## AGENTS 반영안(코디네이터가 읽고 AGENTS.md "실행 환경"에 넣을 문장)
- `android/`(O8)는 Android 네이티브 앱(Kotlin + Compose · 위젯)이다. Vercel · 하루 실행과 무관하다. 빌드 · 시험은 **PC**에서 한다(이 Termux 태블릿에는 설치 · 빌드하지 않는다). 명령은 `android/`에서 `./gradlew testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`(Windows `gradlew.bat`), 기기는 `ANDROID_SERIAL`로 정한다.
- 의존성 · 플러그인 · 판 · 저장소(`google()` · `mavenCentral()`)는 계획서와 `gradle/libs.versions.toml` 글자 그대로 쓰고, Gradle 래퍼는 sha256 고정 판을 바꾸지 않는다. 앱 문구는 웹(`ex12/public/index.html`) 글자 그대로다.
- 서명 키(`*.jks`) · 키 비밀번호 · 앱 세션 값 · Google 클라이언트 보안 비밀번호는 저장소 · 채팅 · 로그에 두지 않는다. 키는 저장소 밖 + 백업 2곳, Actions Secret은 `ANDROID_KEYSTORE_B64` · `ANDROID_KEYSTORE_PASSWORD`이며 release 서명은 Actions만 한다.
- adb는 우리 패키지만 다룬다(설치 · 실행 · 계측 시험 · `adb reverse`로 가짜 서버 · 우리 화면 캡처 · 우리 태그 logcat). 다른 앱 · 기기 설정 · 권한 · 입력은 금지하고, 끝나면 reverse를 지우고 연결을 끊는다.
- 실제 Google 로그인 · 운영 계정 · 운영 DB는 사용자 줄에서만 한다. O8 제작 · 심사에는 `ex12/.env`가 필요 없다(다른 단계에서 옮길 때는 사용자가 직접, 값 출력 · 커밋 금지).
- PC OS별 명령: npm/npx(Windows `npm.cmd` · `npx.cmd`) · `gradlew`/`gradlew.bat` · 해시 `sha256sum` / `shasum -a 256` / `Get-FileHash`. 이 태블릿의 Ubuntu 래퍼(`scripts/ex12-ubuntu.sh`)는 PC에서 쓰지 않는다.

## 비용
- 돈: 0원 — GitHub Actions(공개 저장소 표준 러너 무료, 1회 약 10~15분 · APK 산출물 30일) · Vercel(앱 요청 = 웹과 같은 API) · Google OAuth 클라이언트 0원 · Play 0(Q5). v1.2 P3도 0원.
- 사람 · 시간: 네이티브는 웹 변경마다 앱도 고쳐야 함(두 벌 — 사용자 선택). v1 제작 · 심사는 앞 운영 단계 여러 개 크기(앱 약 6~7천 줄 · 시험 포함). PC에 Android Studio · SDK · 캐시 수 GB.

## 확인 필요(PC 제작이 확인할 것 — 추천안으로 진행)
1. compileSdk 37: SDK 패키지 `android-37.0`을 AGP 9.5가 찾는지(못 찾으면 `release(37) { minorApiLevel = 0 }`) · Compose BOM 2026.09.00 · activity 1.13.0 · lifecycle 2.11.0이 37 · AGP 9.5 · Kotlin 2.4.21로 함께 빌드되는지.
2. Better Auth 1.7.7: 출처 검사 · idToken 경로(`idToken.nonce` 전달 · 1시간 · 잇기 금지 · 토큰 칸 null · 미인증 이메일)가 확인한 소스(main)와 같은지 → `native-client.test.ts`가 고정. 서버의 Google 공개 키 요청이 Vercel에서 되는지(배포 줄 14 · 사용자 줄).
3. Credential Manager: 태블릿(Android 13 · Play 서비스)에서 debug(`.debug` + PC debug SHA-1) · release 클라이언트로 계정 창이 뜨는지 · 브랜드 확인 상태(앱 이름 표시) — 사용자 줄.
4. 같은 이메일 규칙: 운영자 Google 계정(웹 OAuth로 만든 account 행)과 ID 토큰 로그인이 같은 사용자로 이어지는지(같은 sub) — 사용자 줄.
5. `adb reverse`로 태블릿 앱이 PC 가짜 서버(127.0.0.1:3100)에 닿는지 · 무선 디버깅 연결 안정성(끊기면 USB).
6. squarify를 Kotlin double로 옮겼을 때 JS와 소수 셋째 자리까지 같은지(정렬 안정성 포함).
7. 위젯 `values-night`이 런처에서 적용되는지 · 줄 높이 계수 22dp — 미리보기 · 사용자 줄.
8. PC OS: Linux arm64면 공식 aapt2가 없어 쓰지 않음 · Windows ARM의 Studio · 에뮬레이터 지원은 확인 필요 · 에뮬레이터 가속(없으면 9 SKIP).
9. Vercel이 비브라우저 앱 요청(User-Agent `issuerank-android/…`)을 막지 않는지 — 12 · 15.
10. 기록할 한계: D24 지표(아침 방문자)는 앱 사용을 못 셈 · 웹을 고칠 때 앱 대응 여부를 계획마다 적어야 함.

## 공식 근거(2026-11-02 확인)
- 서버 · 세션 · Google: Better Auth [보안(CSRF · Origin · 세션 7일 · 1일)](https://www.better-auth.com/docs/reference/security) · [Origin 검사 소스](https://github.com/better-auth/better-auth/blob/main/packages/better-auth/src/api/middlewares/origin-check.ts) · [bearer(경고)](https://www.better-auth.com/docs/plugins/bearer) · [Google ID 토큰 · 모바일](https://www.better-auth.com/docs/authentication/google) · [Google 공급자 소스(JWKS · RS256 · 1시간 · nonce)](https://github.com/better-auth/better-auth/blob/main/packages/core/src/social-providers/google.ts) · [Credential Manager로 Sign in with Google](https://developer.android.com/identity/sign-in/credential-manager-siwg) · [구현(nonce · 서버 검증)](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation) · [코드랩(Android 클라이언트 = 패키지 + SHA-1 · 웹 클라이언트 = 서버 ID)](https://codelabs.developers.google.com/sign-in-with-google-android) · [serverClientId](https://developers.google.com/identity/android-credential-manager/android/reference/kotlin/com/google/android/libraries/identity/googleid/GetSignInWithGoogleOption) · [Keystore](https://developer.android.com/privacy-and-security/keystore) · [security-crypto 폐기](https://developer.android.com/jetpack/androidx/releases/security).
- Compose · 판: [Compose 컴파일러(Kotlin 2.0+ · 1.12 = compileSdk 37 · AGP 9)](https://developer.android.com/develop/ui/compose/compiler) · [BOM](https://developer.android.com/develop/ui/compose/bom) · [BOM 대응표](https://developer.android.com/develop/ui/compose/bom/bom-mapping) · [Compose UI 출시 노트(1.12 · AGP 9.2)](https://developer.android.com/jetpack/androidx/releases/compose-ui) · [Compose 시험](https://developer.android.com/develop/ui/compose/testing) · [화면 크기 시험(ForcedSize)](https://developer.android.com/training/testing/different-screens/tools) · [내장 Kotlin으로 옮기기](https://developer.android.com/build/migrate-to-built-in-kotlin) · [AGP 9.5.0](https://developer.android.com/build/releases/agp-9-5-0-release-notes) · [AGP 9.0(내장 Kotlin)](https://developer.android.com/build/releases/agp-9-0-0-release-notes) · [Android 17 SDK 설정](https://developer.android.com/about/versions/17/setup-sdk) · [러너 이슈(android-37.0 이름)](https://github.com/actions/runner-images/issues/13859) · Maven 목록 [Kotlin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml) · [activity-compose](https://dl.google.com/android/maven2/androidx/activity/activity-compose/maven-metadata.xml) · [lifecycle-viewmodel-compose](https://dl.google.com/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) · [coroutines](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml) · [browser 1.10.0 POM](https://dl.google.com/android/maven2/androidx/browser/browser/1.10.0/browser-1.10.0.pom) · [credentials](https://dl.google.com/android/maven2/androidx/credentials/credentials/maven-metadata.xml) · [googleid](https://dl.google.com/android/maven2/com/google/android/libraries/identity/googleid/googleid/maven-metadata.xml) · [androidx.test runner](https://dl.google.com/android/maven2/androidx/test/runner/maven-metadata.xml) · [ext junit](https://dl.google.com/android/maven2/androidx/test/ext/junit/maven-metadata.xml) · [org.json](https://repo.maven.apache.org/maven2/org/json/json/maven-metadata.xml).
- PC 환경 · 시험: [Android Studio 설치 · 요건(Linux ARM 미지원)](https://developer.android.com/studio/install) · [aapt2(공식 jar = windows · linux · osx)](https://developer.android.com/tools/aapt2) · [Gradle 호환(JVM 17~27)](https://docs.gradle.org/current/userguide/compatibility.html) · [Gradle 릴리스](https://gradle.org/releases/) · [체크섬](https://gradle.org/release-checksums/) · [래퍼(만들기 · sha256)](https://docs.gradle.org/current/userguide/gradle_wrapper.html) · [에뮬레이터 가속](https://developer.android.com/studio/run/emulator-acceleration) · [adb(무선 디버깅 · ANDROID_SERIAL)](https://developer.android.com/tools/adb) · [명령줄 시험](https://developer.android.com/studio/test/command-line) · [로컬 시험(android.jar)](https://developer.android.com/training/testing/local-tests).
- 위젯: [고급 위젯(30분 · 10초 · goAsync)](https://developer.android.com/develop/ui/views/appwidgets/advanced) · [크기 · 반응형](https://developer.android.com/develop/ui/views/appwidgets/layouts) · [다크 테마](https://developer.android.com/develop/ui/views/appwidgets/enhance) · AOSP [AppWidgetProviderInfo(30분)](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/appwidget/AppWidgetProviderInfo.java) · [RemoteViews(16)](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/RemoteViews.java).
- 서명 · 배포 · 공급망: [앱 서명(키 분실 · 25년 · debug 키 · signingReport)](https://developer.android.com/studio/publish/app-signing) · [target API](https://developer.android.com/google/play/requirements/target-sdk) · [개발자 인증 FAQ(adb 예외 · 직접 설치 아직)](https://developer.android.com/developer-verification/guides/faq) · [Play Protect 경고](https://developers.google.com/android/play-protect/warning-dev-guidance) · [gradle/actions](https://github.com/gradle/actions) · [setup-gradle 캐시](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md) · [GitHub 보안(SHA 고정)](https://docs.github.com/en/actions/reference/security/secure-use) · [Actions 요금](https://docs.github.com/en/billing/concepts/product-billing/github-actions) · [러너 이미지(SDK · JDK 17)](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2604-Readme.md) · 알림 다음 판: [주기 작업 15분](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) · [알림 권한](https://developer.android.com/develop/ui/views/notifications/notification-permission).
