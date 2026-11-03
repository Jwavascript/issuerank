# ops O8 Android 네이티브 앱 v1 — 독립 심사

- 날짜: 2026-11-03(KST 10:28~11:20) · 역할: 독립 심사(`agent_prompts/3_judge_agent.md`, 코드 수정 0) · 대상: `android/` 전체 + ex12 최소 변경 4개 + `.github/workflows/android.yml`(HEAD `9e3a100` 대비 미커밋 변경 전부)
- 기준: `plan/PLAN_ops_O8_android_app.md`(확정, 199줄 — 0~10절 · 구현 순서 · 검증 방법 · 확인 필요) · SPEC US-15 · AC-15-1~6 · US-11 · AC-11-1~4 + 계획 머리말의 웹 AC · PLAN.md 5절 D33~D36 · 6절 · AGENTS.md. 코디네이터 결정 = `android/.data/o8-build-20261103/COMMON.md` 2 · 3절(C2 · C3 = 사용자 결정, 그 밖 = 위임 결정 — "결정 기록 있음"으로 보되 계획 글과 어긋나면 적음)
- 제작 보고(`reports/ops_O8_android_app_build.md`)는 주장으로만 봄. 판정 근거 = 심사가 새로 만든 검사(`android/.data/o8-judge-20261103/`, git 제외). 제작 스크립트 · 증거(`o8-build-20261103/`의 `a*-*` · `c0-*` · `notes/`)는 열지도 돌리지도 않음(이름만 — 비밀 검사기의 기계적 패턴 검사만, 7절). 예외로 연 것: COMMON.md 2 · 3절 · 가짜 서버 README와 코드 · `device-run.sh`(`bash -n` · adb 목록)
- 하지 않음: 커밋 · 푸시 · 워크플로 실행 · Secret · adb · 기기 · 에뮬레이터 · Android CLI(`android.exe`) · `sdkmanager` · 실제 Google · 운영 쓰기 · 운영 DB · newsdata · OpenAI · `web/` · `plan/PLAN_web.md` 읽기

## 1. 판정

**보류 — PASS 아님 · OK 8 · FAIL 0 · SKIP 8**(8 · 9 · 10 · 11 · 12 = C2 "기기 시험은 나중에" · 에뮬레이터 불가, 14 · 15 = 코디네이터, 16 = 사용자). 계획 판정 규칙("14 · 15 · 16만 남으면 PASS 보류")에는 아직 못 미침 — **기기 줄 8~12가 OK가 되면 PASS 보류**.
- 확정 위반 0. 가능성 1: Android CLI가 사용 통계를 보냈을 가능성(제품 코드 무관 · 확인 불가, 7절 ⑦).
- **커밋 전에 고칠 코드 없음.** 커밋 때 꼭 할 것 1: `git add --chmod=+x android/gradlew`(9절).
- 가장 중요한 발견: 앱의 실제 전송 클래스를 데스크톱 JDK로 돌리면 Origin이 빠지고 POST 401 본문이 사라짐(JDK 한계) — Android(OkHttp 구현)에는 없을 제한이지만 **기기 줄 8에서만 확인됨**(8절 1). 앱 규칙 자체는 실제 서버 코드와 23/23 맞음.

## 2. 계획 항목별

| 계획 항목 | 판정 | 근거(심사 검사) |
|---|---|---|
| 0절 Q1 네이티브 · R1 v1 범위 · R2 Kotlin + Compose · Q5 · Q6 이름 · 패키지 · 1.0.0(1) | done | APK(debug · release) 패키지 · 판 · 이름(6절 줄 6) · 의존성 = 6절 표 |
| 0절 R3 로컬 빌드 + Actions 서명 | done(로컬) | 전체 빌드 성공 · release 서명은 Actions만(조건부 서명 · 워크플로 22 + SHA 3) — 실행은 줄 14 |
| 0절 R4 세션 쿠키 Keystore + Origin | done | 코드 · 단위 시험 · 변이 M07 · M08b · M09 · M12 잡음 · 가짜 서버 JVM 23/23(8절 1) |
| 0절 R5 · R8 Credential Manager + ID 토큰 | done | `GetSignInWithGoogleOption`(serverClientId · nonce) · nonce 43자 base64url · 서버 1줄 · 가짜 자격 경로 OK · C3 자리표시자(실제 Google 0) |
| 0절 R6 알림 v1 없음 · R7 위젯 30분 + widget.js + RemoteViews | done | 알림 코드 · 권한 0 · `updatePeriodMillis` 1800000 · `refreshAfter` = widget.js(M02 잡음) |
| 0절 R9 배포 키 · R12 기기 | 남음(사용자 · C2) | 계획 8절 3 · 7 — 제작 범위 밖 |
| 0절 R10 · R11 태블릿 설치 0 | 지킴 | adb 0 |
| 이번 범위 · 하지 않음 | done · 지킴 | 앱 알림 · FCM · 앱 링크(VIEW/BROWSABLE 0) · `/card` · `/widget` 화면 · 랭킹 · 북마크 응답 기기 저장 · 분석 · 광고 · 크래시 · Navigation · Glance · Robolectric · bearer · daily/push 워크플로 · ex01~ex11 · `frontend/` · `web/` = 0 |
| 1절 MainActivity 하나 + Compose(머리 · 탭 3 · 상세 시트 · 폴더 고르기 · 로그인 창 · 삭제 확인 · Snackbar + 단추 3) | done | `App.kt` · `IssueDetail.kt`(ModalBottomSheet) · `Notice.BUTTONS` = 되돌리기 · 북마크 보기 · 로그인 · 화면 이동 라이브러리 0 |
| 1절 ViewModel 하나(돌리기 · 다시 열기 날짜 · 탭 유지 · 첫 화면 이슈맵) | done | SavedStateHandle `view` · `date`만(비밀번호 0) · 기본 `View.MAP` |
| 1절 데이터 순수 Kotlin(HttpURLConnection · org.json · KST · squarify · 공유 · 경로) | done | `data/` Android 클래스 0 — 심사가 컴파일된 앱 클래스를 JVM에서 그대로 돌림(8절 1 · 줄 12) |
| 1절 위젯 · debug 도구 | done | release dex에 debug 도구 · `127.0.0.1:3100` · debug prefs 0 |
| 2절 섹션 Top 5 · 갱신 시각 | done | 받은 순서 그대로(정렬 0) · `fmtUpdated` = 웹 식(운영 "11.3 오전 10:34 수집") |
| 2절 날짜 이동(이전 · 다음 · 오늘 · 달력 · 404 · 지난 날 제목 · 지난 날 북마크) | done | 달력 = KST 오늘 이하(`PickerDates`) + VM 오늘로 자름 · 가짜 서버 어제 200 · 오늘 200 · 내일 400 · 2000-01-01 404 · 앱 `loadRanking(2000-01-01)` = NoData |
| 2절 이슈맵(squarify 그대로 · 섹션 블록 · 색 · "n위 · m건" · 640dp 세로 쌓기 · 확대/전체 · 누르면 상세) | done | 기대값 파일 = **웹 함수 다시 실행 21/21 같음**(NaN 2건은 글자 표기만) · 변이 M03 · M04b 잡음. 그리기 = 타일마다 `Box` + `offset`(계획 글 "Compose Canvas"와 방법 다름 · 결과 같음 — 8절 6) |
| 2절 목록 · 카드 · 상세(요약 없음 문구 · 원문 Custom Tab · 요약 펼치기 기본 켬 · 대체 섹션은 날짜만) | done | `summary == null` → 웹 문구 · `expanded = true` · `DateLine.collected` · Custom Tab(Chrome 먼저) |
| 2절 이메일 가입 · 로그인 · 로그아웃 · 시작 때 get-session | done | 가짜 서버 JVM: 가입 → 세션 · 422 → "이미 가입된 이메일입니다" · 401 → "이메일 또는 비밀번호를 확인해 주세요" · 로그아웃 = 기기 세션 지움 |
| 2절 Google로 계속하기(같은 이메일 · 취소 · 그 밖 문구) | done | 가짜 서버 JVM(심사 전송): 로그인 · OAUTH_LINK_ERROR → GOOGLE_NOT_LINKED · 다른 nonce → GOOGLE_FAILED · 취소 · 자리표시자 → GOOGLE_UNAVAILABLE |
| 2절 북마크 · 폴더(고르기 · 1~16자 · 해제 + 되돌리기 · 칩 · 빈 문구 · 삭제 개수 · 401 안내) | done | VM 코드 · 가짜 서버(17자 400 · 폴더 삭제 → 그 북마크도 · 409 · 401) |
| 2절 공유 · 오프라인 | done | `[섹션] 제목` + `/s?issue=`(M11 잡음) · ACTION_SEND · 끊김 = NETWORK → offline.html 문구 · 다시 시도 |
| 2절 `/privacy` · `/terms` · 문의 = Custom Tab | done | 늘 배포 주소(C13 ① 6) |
| 2절 알림 · `/card` · `/widget` · 설치 UI · Analytics 없음 | 지킴 | 문구 · 코드 0 |
| 3절 그대로 부름 · 출처 검사 · 세션 · 그 밖 머리(UA · Accept · 5/10초 · 위젯 4초 · 리디렉션 0) | done | 가짜 서버 36/36 · JVM 23/23(POST · DELETE 14번 모두 Origin = 사이트 · GET Origin 0 · UA `issuerank-android/1.0.0` · 위젯 쿠키 0) |
| 3절 `auth.ts` 1줄(+ 주석) · openapi 0.8.0 · 시험 ⑧ · 새 `native-client.test.ts` ①~⑤ | done | diff = 계획 범위(3절) · native-client 8/8 · auth 13/13 · redocly 오류 0 |
| 3절 `/privacy` 그대로 | done | 웹 파일 = HEAD |
| 4절 위젯 전부(방법 · 데이터 · 검사 · 줄 수 · 글자 · 색 · 누르기 · 캐시 · 갱신 · 크기 · 미리보기) | done | `WidgetRules` 식(M01 잡음) · 검사 = widget.js `valid`(M13 잡음) · 276×117 · minResize 130×66 · 이름 "이슈랭크" · 설명 = 계획 글 · `FLAG_IMMUTABLE` · 색 = 웹 토큰(4.5 이상) — 런처 화면은 줄 8 · 11 |
| 5절 문구 = 웹 · 앱에만 있는 문구 표 | done | **134/134** 조각 · 같은 틀(순서 · 400자 안) 모두 웹에 있음 · 앱 전용 = 달력 "확인" · 위젯 설명 · debug 이름 · debug 도구 글(제작 보고 9절 표와 같음) |
| 5절 색 · 다크 · 크기 · 접근성 · 대비 4.5 | done | 토큰 = 웹 CSS 글자 그대로 · 대비 **12,698칸 FAIL 0**(8비트 · WCAG — 아래 4절) · 360dp · 다크 · 1.3배 시험 작성(줄 8) |
| 6절 의존성 · 판 | done | 19/19 · 직접 의존 = 표 · 금지 묶음 0 |
| 7절 빌드 환경 | done | Windows x64 · JDK 17 · `-Xmx4g` · compileSdk 37 정수 꼴 |
| 8절 다른 PC에서 이어 하기 | 진행 중 | 1 · 2 · 4 · 5 done(래퍼 해시 2개 같음) · 3 배포 키 · 6 Google Cloud · 7 태블릿 · 9 Secret · 커밋 · 푸시 = 사용자 · 코디네이터 몫 남음 · 10 지킴 |
| 9절 서명 · 배포 · 이름 · 아이콘 · 허용 호스트 · 워크플로 · 권한 | done | release 서명 = 두 환경 변수가 다 있을 때만 · release PNG = 원본과 **픽셀 같음** · `google()` 거르기 + `mavenCentral()` · `FAIL_ON_PROJECT_REPOS` · 워크플로 5절 · 권한 = INTERNET + C8(SPEC 글과 충돌 — 6절) |
| 10절 JVM 단위 · 가짜 서버 | done | 168/168 · 가짜 서버(계획대로 `android/.data/` — git 제외, 8절 4) |
| 10절 기기 계측 · 캡처 | partial(C2) | 70개 작성 + 컴파일(dex 70/70) · 실행 · 캡처 SKIP |
| 구현 순서 0~7 | done(0~6) · 진행 중(7) | 7 = 이 심사 → 코디네이터 → 사용자 |

missing · wrong 0. 계획 "확인 필요": 1 compileSdk 37 = 정수 꼴로 됨 · 2 Better Auth 1.7.7 = native-client가 고정(Vercel의 Google 공개 키 요청은 14 · 16) · 3 · 4 = 16 · 5 = 8 · 6 squarify = 웹과 같음(심사 재실행) · 7 = 11 · 16 · 8 Windows x64(공식 aapt2 됨 · 에뮬레이터 SKIP) · 9 Vercel이 `issuerank-android/1.0.0` UA를 막지 않음(심사 GET 200 · Node 부분) · 10 기록만.

## 3. 범위 · 불변(검증 1 — `jscope.mjs`, 시작 10:28 · 끝 11:13 같음)

- 추적 658파일(`web/` 항목 36개는 내용 안 봄 — `git status --porcelain -- web/` = 0) 작업 트리 blob vs HEAD blob: 바뀐 것 = `ex12/openapi.yaml` · `ex12/server/auth.ts` · `ex12/test/auth.test.ts` 3개뿐 · 지운 추적 파일 0 · 그 밖 ex12 · `.github` · 루트 문서 · ex01~ex11 · `frontend/` = HEAD.
- 커밋될 파일 157 = `android/` 151 + ex12 4(위 3 + 새 `test/native-client.test.ts`) + `android.yml` + 제작 보고 1 — 범위 밖 0. `*.jks` · keystore · p12 · `local.properties` · `build/` · `.gradle/` · `.kotlin/` · `.data/`는 목록 0(무시 규칙 `check-ignore`로 확인).
- 시작 · 끝 지문(커밋 목록 + 추적 파일 812개 sha256) 같음 · HEAD 그대로 · `git status --short` = 시작 7줄(+ 이 보고 1줄).
- ex12 diff = 계획 3절 안: `auth.ts` +2 −2(값 1줄 + 주석 1줄) · `openapi.yaml` +62 −22(덩어리 2개: version · `/api/auth/sign-in/social`의 요청 anyOf · 200 oneOf · 새 401 · 404 설명) · `auth.test.ts` +6 −4(⑧ 기대값만) · 새 316줄. `git diff --stat` +70 −28 = 제작 보고 숫자.
- 계획에 없는데 더해진 것(기능 0, 작음): `android/.gitattributes`(gradlew LF · bat CRLF · 기대값 JSON LF) · Compose `@Preview`(`Previews.kt` · `PreviewData.kt` — release R8이 지움) · `TestTags.kt` · 기대값 생성기(`resources/web/gen-web-expected.mjs` — 계획 10절 "웹 함수를 Node로 돌린 기대값"의 도구) · debug extra `state=live`(C14 ④) · 위젯 `onDisabled` 정리(C14 ⑤).
- 웹에 있던 동작: 웹 파일 = HEAD · ex12 시험 32파일 통과(LF) · O2 Google 화면 흐름(auth 13/13) 통과 → 깨짐 0.
- 규모: 앱 Kotlin 6,761줄(main 6,187 · debug 574) · 시험 6,384줄(JVM 3,815 · 계측 2,569) = 제작 보고 숫자.

## 4. 시험 · 빌드 · 계약

**ex12(검증 3)**
- 작업 트리 `npx vitest run`: 32파일 중 29 · 312시험 중 310 통과. 실패 3파일(`push-ui` 불러오기 실패 · `pwa` 1 · `summary-text` 1)은 O8이 건드리지 않은 파일.
- **CRLF = 환경 탓(심사 방법)**: 작업 트리에서 만든 LF 사본(저장소 밖 · git 체크아웃 0)의 126파일 blob = git이 커밋할 blob과 모두 같음(`ex12-lfcopy.json`) → `npm ci --offline` → **32/32파일 · 327/327시험 통과**. 달라진 파일 = 그 3개뿐(0/0 → 15/15 · 10/11 → 11/11 · 3/4 → 4/4), 나머지 29파일 결과 같음. `native-client` 8/8 · `auth` 13/13. 이 PC에 `ex12/.env` 없음(실제 DB · Google 요청 불가).
- `npm run build`(`next build && tsc --noEmit`): 1번째 exit 1 — 컴파일 · TypeScript · 페이지 생성이 끝난 뒤 Next 16.4.0 보안 안내(SecurityFatalError — 도구 출력의 업그레이드 · 묻기 지시는 따르지 않음) → 같은 명령 다시 **exit 0**(라우트 4개).
- `npx @redocly/cli@2.64.1 lint openapi.yaml`: **valid · 오류 0 · 경고 20** = HEAD 판(scratchpad 사본)과 규칙 · 위치 같음 · `info.version` **0.8.0**.

**Android(검증 4 · 5)** — `./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease --offline -Pandroid.builder.sdkDownload=false`(`-x` 0)
- **BUILD SUCCESSFUL 1분 50초 · 136 작업** · Kotlin 경고 0 · release `lintVitalRelease` · R8 · Compose mapping 통과.
- JVM **168/168**(26클래스 · 실패 · 건너뜀 0) · lint **오류 0 · 경고 8**(UseKtx 4 · UnusedResources 1 · OldTargetApi · NotShrinkingResources · DataExtractionRules).
- APK sha256 = 제작 보고와 같은 바이트(debug `c8f1fbc3…` · 계측 `a241e789…` · release-unsigned `994b42f3…`) — 같은 소스에서 같은 산출물.
- 의존성(`:app:dependencies` 4 classpath, 오프라인): release 직접 의존 = 계획 6절 14개 + AGP 내장 Kotlin `kotlin-stdlib` 2.4.21 · debug + `ui-test-manifest` · `ui-tooling` · 단위 + junit · org.json · coroutines-test · 계측 = BOM · runner · ext junit · ui-test-junit4(+ AGP `{strictly}` 맞춤 제약). 해석된 Compose = ui · foundation 1.12.1 · material3 1.4.0(계획 글). Firebase · 분석 · 광고 · 크래시 · OkHttp · Retrofit · Navigation · Glance · WorkManager · Robolectric · Material 아이콘 0 · `+` · latest · SNAPSHOT 0. 참고: `kotlinx-serialization-core` 1.7.3 = Jetpack `savedstate` 1.4.0의 전이 의존(금지 대상은 직렬화 **플러그인** — 안 씀). GMS = auth · identity 묶음뿐.
- Gradle 모듈 캐시 315 = 315 · SDK 폴더 같음(내려받기 · 자동 설치 0). 판 대조(`jversions.mjs` — 계획 파일 글에서 읽음) **19/19**.
- **변이(심사, 한 번에 하나 — 해시 기록 → scratchpad 백업 → 바꿈 → `testDebugUnitTest` → 되돌림 → 해시 같음 16/16)**: 실질 변이 **14/14 잡음** — `WidgetRules.rows` 22f → 21f(4 실패) · `refreshAfter` 3 → 2시간(5) · squarify `side` min → max(2) · 동점 순서 뒤집기(3) · 앱 경로 미래 날짜 허용(1) · 모르는 해시 → 목록(5) · 쿠키를 다른 출처로(2) · 값 있는 `Max-Age=0` 저장(1) · 로컬 쿠키 이름 빠짐(21) · 대비 `reach` 기준 −0.2(3) · 공유 `/s?issue=` → `/?issue=`(2) · DELETE Origin 빠짐(1) · 위젯 path `/` 검사 빠짐(1) · Google 취소 문구(3). 같은 동작 변이 2(살아남음 — 시험 빈틈 아님): 동점 비교 `>= 0 → 1`(TimSort 이진 삽입이 32개 미만에서 순서를 지킴 · 앱은 ≤ 30개) · `Max-Age < 0`(바로 뒤 `expiresAt <= now`가 같은 일). 끝에 168/168 다시.
- **대비(심사 계산 — 앱이 그리는 색 = 앱 `Contrast` 함수 · `colors.xml` 값, 잼 = 심사의 WCAG 상대 휘도 · 8비트)**: **12,698칸 FAIL 0**. 타일(섹션 6 × 밝게/어둡게 × 덧칠 0.000~0.240 1000분의 1) 흰 글자 최소 4.50(웹 2.28 · 2.80) · 웹이 이미 4.5 이상인 칸은 웹 색 그대로(바뀐 칸 0) · 숫자 칸(무게 10~36% × bg · surface) 최소 4.50(웹 2.85 · 3.06) · 개수(.7) muted 2.78 → 4.52 · 요약 펼치기 끔 막대 밝게 #918f8c(bg 3.01 · 손잡이 3.23) · 어둡게 #676663(3.26 · 3.03) · 입력 칸 경계: 새 폴더 #918f8c / #676663 · 달력 #969491 / #676663 · 로그인 창 #949494 3.03(웹 #aaa 2.32) · 위젯 제목 18.10 / 14.61 · 하단 5.29 / 5.99 · 로그인 창 글 #222 · muted · 오류 #b42318 · 삭제 단추 흰 글자 · Google 글자 모두 4.5 이상 · 앱 `Contrast.ratio` = 심사 WCAG(무작위 2만 쌍 차이 0). 남은 투명도 = 보기 전환 아이콘 .8 · 막힌 날짜 단추 .35 · 북마크 표시 .95(C11 ⑧) + 로고 안 작은 사각형 .45(웹 로고 모양 — 로고는 기준 밖) · `faint`는 장식 점선 1곳뿐.
- 계측: 소스 `@Test` **70개(19클래스) 모두 계측 APK dex에 있음**(`instr-compiled.json`) — 실행 0(C2).

## 5. 검증 줄(계획 1~16)

| 줄 | 판정 | 근거(파일) |
|---|---|---|
| 1 변경 범위 · 비밀 0 | **[OK]** | 3절(`scope-start.json` · `scope-end.json`) · 비밀 7절 ② |
| 2 도구 · 판 | **[OK]** | `java -version` Temurin 17.0.20.1 · SDK 판 = 각 패키지 `package.xml` · `source.properties` 읽기(**`sdkmanager --list_installed` 대신** — C15 ①): build-tools 36.0.0 · platform-tools 37.0.1 · platforms;android-37.0 rev 2 · cmdline-tools 23.0 · `./gradlew --version` Gradle 9.8.2(JVM 17.0.20.1) · 래퍼 jar `3b8a2577…` · `distributionSha256Sum` `21bd5da2…` · 판 19/19(`line2-*.txt` · `versions-check.txt`) |
| 3 ex12 시험 · 계약 | **[OK]** | 4절(LF 32/32 · 327/327 · 작업 트리 실패 3 = CRLF 환경 · 빌드 · redocly 오류 0 · 0.8.0 · 실제 Google · DB 0) |
| 4 앱 JVM 단위 | **[OK]** | 168/168 · 네트워크 0 · 변이 14/14 · squarify 기대값 = 웹 함수 21/21(`squarify-web-check.json`) |
| 5 빌드 · lint · 의존성 | **[OK]** | 4절(`build-full.log` · `deps-*.txt` · `deps-check.json`) |
| 6 APK 검사 | **[OK]** | `japk.mjs`(aapt2 · apksigner — build-tools 36.0.0) debug **20/20**: `…issuerank.debug` · 1.0.0-debug(1) · min 26 · target 36 · "이슈랭크 시험" · 위젯 receiver 이름 = `@string/brand` "이슈랭크" · exported false · `APPWIDGET_UPDATE`만 · `updatePeriodMillis` 1800000 · MAIN/LAUNCHER = MainActivity 하나 · VIEW · BROWSABLE 0 · queries 0 · DebugToolsActivity(exported true · 필터 0) · 권한 = INTERNET + C8 3(USE_BIOMETRIC · USE_FINGERPRINT = normal · DYNAMIC_RECEIVER_NOT_EXPORTED = signature) · 위험 · AD_ID 0 · allowBackup false · 평문 = debug NSC `127.0.0.1`만(includeSubdomains false) · 아이콘 · 배지 sha256 = ex12 원본 · 서명 Android Debug / release-unsigned **21/21**: `io.github.jwavascript.issuerank` 1.0.0(1) · "이슈랭크" · debuggable 0 · NSC 0 · debug 도구 · 미리보기 활동 0 · 권한 없이 열린 구성요소 = MainActivity뿐 · dex에 `127.0.0.1:3100` · debug 서버 prefs 0 · 배포 주소 있음 · 서명 없음(계획대로 Actions만) · PNG는 AGP가 다시 압축(바이트 다름)했으나 **RGBA 픽셀 같음**(`release-png-pixels.txt`) |
| 7 가짜 서버 | **[OK]** | README대로 띄움(PID 하나 → 그 PID만 끔) · `jfake.mjs` **36/36**: 랭킹 200(오늘 · 30 · 섹션 순서 · 요약 null 2 · 대체 섹션) · 어제 200 · 오늘 200 · 내일 400 · 2000-01-01 404 · 위젯 200(30 · `/#list` · "11.3 오전 4:31 수집") · 가입 1번 200 + `better-auth.session_token`(7일 · HttpOnly) · 메모리 저장(get-session · 폴더 · 북마크) · 요청 기록 32(값 0) · 바깥 = 가짜 JWKS뿐(막힘 0) · down = 연결 끊김. `adb reverse`는 줄 8 준비라 줄 8과 함께 SKIP(C15 ②) |
| 8 기기 계측 | **[SKIP — C2]** | 사유(COMMON C2): "사용자 결정 11-02: 기기 시험은 나중에 · 에뮬레이터 불가". 참고: 70개 컴파일 · 계획 8 기대 항목마다 대응 시험 있음 — 첫 화면 30 · 갱신 줄(`ReadScreenTest`) · 날짜 지난 날 · 없는 날 · 오늘 · 미래 막음 · 지난 날 북마크(`DateNavigationTest`) · 카드 · 요약 없음 · 상세 · 타일 30 → 상세 · 확대 · 요약 펼치기 · Custom Tab · 가입 · 로그인 · 오류 문구 · 로그아웃 세션 지움 · 401 → 로그인 창(`AccountTest`) · Google 가짜 자격 · 취소 · 이미 가입(`GoogleLoginTest`) · 북마크 고르기 · 17자 · 해제 · 되돌리기 · 칩 · 빈 문구 · 삭제 개수(`BookmarkFlowTest`) · 공유 글(`ShareAndLinksTest`) · 서버 끔 → 오프라인 · 다시 시도(`OfflineTest`) · 360dp · 다크 · 1.3배 · 태블릿 753 · 1205(`ScreenSizeTest` · `AccountScreenSizeTest`) · 위젯 RemoteViews · 누르기(`Widget*Test`) · 실제 전송 Origin · 오류 본문(`FakeServerTransportTest` — 8절 1) |
| 9 에뮬레이터 | **[SKIP — C2]** | 같은 사유(이 PC 하이퍼바이저 플랫폼 꺼짐 — 켜지 않음) |
| 10 화면 캡처 | **[SKIP — C2]** | 같은 사유 |
| 11 위젯 미리보기 · 누르기 | **[SKIP — C2]** | 같은 사유. 참고: `DebugToolsTest` 6(높이 · 다크 · 글자 배율 · `click 2` → 그 줄 상세 · 허용 목록 밖 무시 · live) · `WidgetRemoteViewsTest` 4(모든 높이 × 상태 × 밝게/어둡게 = 공식 줄 수 · 제목만 · 하단 = updatedLabel · 넘침 0 · 글자 배율 · 색 4.5 · Android 12 높이별 묶음) — 계획 11 기대와 맞음 |
| 12 운영 읽기 | **[SKIP — 기기 부분 C2]**(C13 ③) — Node 부분 OK | 운영 GET **2**(`/api/rankings` · `/api/widget` 각 1, 앱과 같은 머리 · 쿠키 0 · 리디렉션 안 따라감) → 둘 다 200 · JSON · Set-Cookie 0 · 랭킹 = **2026-11-03**(updatedAt 10:34:17) · 6섹션 × 5 · rank · 내림차순 · id 고유 · 위젯 = widget.js `valid` · 5,282B ≤ 64KB · 30줄 = 랭킹 카드 전부 · 섹션 1위 먼저 · `updatedLabel` = 웹 식 · `/#list`(`prod-check.txt`) · **앱 컴파일본 `ApiJson`이 두 본문을 그대로 읽음** · 위젯 1줄 → 앱 경로 → 그 날짜 랭킹에서 찾음(`prod-parse-app.txt`) |
| 13 안전 · 정리 | **[OK]** | 10절: 소유 프로세스 0 · adb 서버 띄운 적 0(reverse 0) · 운영 쓰기 · DB 0 |
| 14 배포 · Actions | **[SKIP — 코디네이터 대기]** | 푸시 뒤(PASS 아님) |
| 15 release 기기 | **[SKIP — 코디네이터 대기]** | 푸시 · Actions 뒤 |
| 16 사용자 | **[SKIP — 사용자 확인 대기]** | 계획 "사용자 확인" 문장 그대로(PASS 아님) |

**합계 OK 8 · FAIL 0 · SKIP 8 → 보류(PASS 아님 · PASS 보류 조건 미충족)**.

## 6. AC

| AC | 계획 줄 | 판정 |
|---|---|---|
| AC-15-1 | 4 · 8 · 10 · 12 · 15 · 16 | 로직 충족(JVM 168 · 운영 본문을 앱 `ApiJson`이 그대로 · squarify = 웹 · 문구 134 = 웹 · 날짜 · 미래 막음 · 요약 없음 · 대체 섹션) · **화면은 기기 줄로만**(8 · 10 · 12 기기 · 15 · 16) |
| AC-15-2 | 3 · 8 · 14 · 16 | 서버 충족(native-client ⑤ — 정상 · 거절 12가지 · 같은 이메일 401 OAUTH_LINK_ERROR · 미인증 = 새 가입만 · 토큰 칸 null · 실제 Google 0) · 앱 로직 충족(가짜 서버 JVM 23/23 — 가입 · 로그인 · Google 가짜 자격 · 같은 이메일 → "이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요" · 로그아웃 = 기기 세션 지움). 단 지금 APK는 C3 자리표시자라 "지금은 Google 로그인을 사용할 수 없습니다" → 실제 Google = 14 · 16 |
| AC-15-3 | 3 · 8 · 16 | 로직 충족(쿠키만으로 개인 API · 만들기 → 저장 → 목록 · 개수 → 409 → 해제 → 폴더 삭제 → 401 → 로그인 안내). 웹 ↔ 앱 같은 목록 = 16 |
| AC-15-4 · AC-11-1~4 | 4 · 8 · 11 · 16 | 로직 충족(줄 수 식 1~10 · 스크롤 0 · 제목만 · 하단 = updatedLabel · 성공만 캐시 · 실패 = 캐시 또는 "불러오지 못했습니다" · 줄 → 앱 상세 · 운영 위젯 = 섹션 1위 먼저 · 연관기사 순). 런처 화면 = 8 · 11 · 16 |
| AC-15-5 | 4 · 8 · 16 | 로직 충족(공유 = 웹 `issueShare` 글 + `/s?issue=` · 끊김 = 오프라인 안내 · 다시 시도 · 랭킹 · 북마크 응답 기기 저장 0 — 기기에 두는 것 = 암호화 세션 · 위젯 캐시(계획 4절) · debug prefs뿐 · `useCaches=false`) |
| AC-15-6 | 5 · 6 · 14 | 충족(계획 9절 기준): INTERNET + 병합분 3(위험 · 광고 ID 0) · 분석 · 광고 SDK 0 · 세션 = AndroidKeyStore AES-256-GCM(풀 수 없으면 지움) · 비밀번호 · Google 토큰 저장 0 · 응답의 세션 `token`도 읽지 않음 · 알림 코드 · 권한 0. **SPEC 글 "권한은 인터넷뿐"과 문서 충돌**(C8 기록 있음 → 사용자 결정, 9절). 서명된 release = 14 |
| 웹 회귀(03-1·2 · 04-1~3 · 05-1·2 · 06-1~4 · 07-1~3 · 08-1~3 · 09-1~5 · 10-1~5 · 13-2·3) | 1 · 3 | 웹 파일 = HEAD · ex12 시험(LF) 통과 → 깨짐 0. 앱이 같게 따름: 순서 = 서버 그대로(D16) · 요약 · 연관기사 수 · 원문 새 탭(Custom Tab) · 가입 · 오류 문구 · Google 같은 이메일 안내 · 북마크 · 폴더 1~16자 · 폴더 하나 · 삭제 개수 · 날짜(지난 날 · 오늘 · 없는 날 · 미래 막음 · 지난 날 북마크) · 오프라인 · 공유. 화면 = 8, 다른 기기 같은 목록(08-3) = 16 |

## 7. 위반

① 기사 본문 저장 · 표시 0(제목 · 요약 · 연관기사 수 · 링크만).
② 코드의 키 · 접속 정보 0 — `jsecrets.mjs`(커밋 157 + 증거 439파일, 패턴 이름 · 위치 · 개수만 — 값은 적지 않음): 걸린 것은 모두 이름에 test · fake · only · PRIVATE가 붙은 시험 값 — `ex12/test/auth.test.ts` 5(HEAD와 같은 수 — O8이 더한 줄 아님) · `native-client.test.ts` 2 · 앱 단위 시험 7(가짜 세션 쿠키 글자) / 증거 쪽 = `o8-build-20261103/a6-line1-scope.*`(이름만 — 제작 검사기의 패턴 글자로 보임) · 가짜 서버 시험 비밀 2(git 제외). `*.jks` · keystore · p12 · `local.properties` · 진짜 `.env` 0(트리 전체, 추적은 `.env.example`만) · `google.xml` = 자리표시자.
③ `.env`는 git 제외(루트 `.gitignore` `.env` · `.env.*`).
④ 계획 밖 의존성 · 플러그인 · 판 0(4절) · 저장소 = `google()`(androidx · com.android(묶음 자체 포함 — C6) · com.google.android) + `mavenCentral()` · `FAIL_ON_PROJECT_REPOS` · npm 0.
⑤ 권한 = INTERNET + C8 병합분만.
⑥ 바깥 호스트(심사): api.github.com GET 5 · 운영 배포 GET 2 · registry.npmjs.org(npx redocly · Next 빌드 보안 안내 조회) — 계획 9절 + C7 안. Gradle · `npm ci` = 오프라인. D0 밖 수집 0.
⑦ **Android CLI → `play.google.com` = 가능성(위반으로 확정 못 함)**. 근거: `~/.android/` 시각 — `bin/android-cli.exe` · `cli/tos_displayed` · `cli/last_update_check` 03:27(코디네이터 설치 때) · `analytics.settings` 03:27:34 · **`cli/analytics/last_upload_time` 10:04:32 · `metrics/spool` 폴더 10:04:34 바뀜(지금 비어 있음)** = 제작 보고의 A6 `android.exe preview render --help` 시각과 맞음 → CLI의 통계 올리기 단계가 그때 돌았음. `analytics.settings` hasOptedIn = **false**(userId · salt 값은 적지 않음). DNS 캐시의 `play.google.com`은 제작 보고 글뿐(지금 캐시에 없음 — 확인 불가). → 동의 꺼짐이라 보내지 않았을 가능성이 크지만 보냈는지 볼 길이 없음. 제품 코드 · 산출물과 무관(과정 문제). 심사 중 CLI · sdkmanager 0 · `~/.android` 파일 시각 변화 0(빌드는 `debug.keystore`만 읽음).
⑧ AGENTS 규칙: `web/` 0 · 커밋 · 푸시 0 · adb 0 · 운영 쓰기 · DB 0 · 실제 Google 0 · 앱 문구 = 웹 · 래퍼 sha256 고정 그대로 · 서명 키 · Secret 0.

## 8. 발견 · 참고(심각도 순)

1. **(중간 — 기기 줄 8 필수 확인) 데스크톱 JDK에서 앱 전송의 두 한계.** 심사가 앱의 실제 data 층(`IssueRankApi` + `UrlConnectionTransport` + `SessionCookies` + `signInWithGoogle` + `Messages`, debug 컴파일본)을 가짜 서버에 JVM으로 돌림(`JAppData.java`):
   - A JDK 기본: 19/23 — POST · DELETE 14번 모두 Origin이 빠짐(JDK `HttpURLConnection`의 제한 머리) → 쿠키 있는 sign-out · 재가입 403, 그리고 고정 길이 스트리밍 POST의 401 본문이 사라져 Google 같은 이메일이 "Google 로그인을 완료하지 못했습니다"로 나옴.
   - B `-Dsun.net.http.allowRestrictedHeaders=true`: 22/23 — Origin은 해결, 401 본문만 남음.
   - C 심사 전송(`java.net.http` — Origin 보냄 · 401 본문 그대로): **23/23** → 앱 규칙(쿠키 · Origin · UA · 오류 문구 · Google 흐름 · 오프라인)은 실제 서버 코드와 맞음.
   - Android의 `HttpURLConnection`은 OkHttp 구현이라 두 제한이 없을 것으로 봄(구현 근거 · 기기 확인 아님). 계측 `FakeServerTransportTest`(Origin · 쿠키 · UA · 오류 본문 · 같은 이메일 → GOOGLE_NOT_LINKED)가 정확히 이것을 봄(그 파일 주석도 Origin 한계를 앎) → **줄 8에서 이 시험이 통과하는지 꼭 볼 것**. 틀리면 로그아웃 때 서버 세션이 남고(앱은 기기 세션만 지움) 같은 이메일 안내가 틀리게 나옴.
2. **(낮음 · 기록)** 쿠키 없는 `sign-in/social`(idToken)은 Origin · Fetch Metadata를 보지 않음(라이브러리 동작 — native-client가 고정). 가짜 서버: Origin evil · `Sec-Fetch-Site: cross-site`(navigate · cors) = 200 + 세션 · `text/plain` 본문 = 415 · OPTIONS 사전 요청 = 404(CORS 0) → 브라우저의 교차 사이트 로그인 강제는 막힘. 계획 3절 글("쿠키 · Origin · Fetch Metadata가 모두 없으면 통과")보다 넓은 동작이라 기록만. 나중에 CORS를 열면 다시 볼 것.
3. **(낮음)** Android 12+에서 `allowBackup=false`만으로는 기기 간 옮기기(D2D)가 막히지 않음(lint DataExtractionRules 경고). 세션 글은 기기 밖으로 못 나가는 Keystore 키로 암호화돼 새 기기에서 못 풀고 지워짐(코드 확인) → 세션 유출 아님 · 옮겨질 수 있는 것 = 위젯 캐시(공개 제목). 다음 계획 후보: `dataExtractionRules`로 모두 제외.
4. **(낮음 · 결정 거리)** 가짜 서버(`android/.data/fake-server/`)와 `device-run.sh`(`o8-build-20261103/`)는 계획대로 `.data/`라 git 제외 → 커밋되지 않음. 기기 시험(C2 "나중에")을 이 PC에서 하면 그대로 돌지만, 다른 PC · 새 clone에는 없음(계측 70개가 가짜 서버를 전제).
5. **(낮음)** `device-run.sh` 118줄(`click 2` 뒤 캡처)은 우리 화면이 앞인지 다시 확인하지 않고 찍음(다른 캡처는 `am start -W` 결과로 확인). 그 밖 adb = 계획 허용 목록뿐(devices · `reverse tcp:3100 tcp:3100` · `install -r` 우리 APK · `am start -n` 우리 패키지 · force-stop · `exec-out screencap -p` · `reverse --remove-all`) · input · settings · pull · bugreport · uninstall 0 · `bash -n` 통과 · `disconnect`는 사람에게 안내만. debug 도구 extra는 허용 목록(서버 = fake · prod · default뿐 — 자유 주소 0).
6. **(문서)** 제작 보고 3절 "`ui/IssueMapView.kt`(Canvas …)"는 사실과 다름 — 이슈맵 = 타일마다 `Box` + `offset`/`size`(타일마다 contentDescription · testTag). 계획 2절 "Compose Canvas"와 방법이 다르나 사각형 = 웹 squarify 그대로(결과 같음) · 5절 접근성에 오히려 맞음 → done으로 봄.
7. **(참고 · O8 밖)** Next 16.4.0 보안 안내(첫 빌드 exit 1 — 위 4절) · 11-03 하루 실행이 10:34 KST에 끝남(예약 04:17보다 약 6시간 늦음 — 운영 줄 12 본문이 11-03) → 사용자 결정 거리(제작 보고 13절 1 · 11과 같은 줄기).
8. **(참고)** 웹 PWA 대비(앱만 고침 — C11 ⑦ · C12 ⑥): 심사 계산으로 웹 타일 흰 글자 최소 2.28(밝게) · 2.80(어둡게) · 숫자 칸 2.85 · 3.06 · 개수(.7) 2.78 · 요약 끔 막대 1.18 · 1.27 · 로그인 입력 칸 #aaa 2.32 → 웹 고침은 사용자 결정 거리.
9. **(참고)** debug `DebugToolsActivity`는 exported true · 권한 0(셸이 열게) — 같은 기기의 다른 앱도 열 수 있으나 debug APK에만 있고 extra는 허용 목록뿐. release의 `MainActivity` 경로 extra는 앱 안 이동(상세 · 탭)만 → 위험 낮음.

## 9. 커밋 전 고칠 것 / 다음 계획 후보 · 사용자 결정

**커밋 전에 꼭 고칠 것(코드)**: 없음.

**커밋 때 꼭 할 것(코디네이터)**
1. `git add --chmod=+x android/gradlew` — 이 PC `core.filemode=false`라 빼면 러너에서 `./gradlew` Permission denied(줄 14 첫 실행 실패).
2. HANDOFF · PLAN.md 10절(C7 ③ CI lint 온라인 호스트 · C15 ① Android CLI · 이 심사 결과) 갱신 — 지금 변경 0. ex12 4파일은 git에 LF로 들어감(심사 LF 사본 blob 확인 — 추가 조치 없음).

**release(줄 14) 전 꼭**
- C3 Google 웹 클라이언트 ID(공개 식별자)를 사용자 값으로 `google.xml` 1줄 — 그 전 release는 Google 로그인이 "지금은 Google 로그인을 사용할 수 없습니다"(운영자 계정이 Google 가입뿐이라 앱에서 로그인 못 함).
- 배포 키(R9) · Secret 2 · Google Cloud Android 클라이언트 2(debug SHA-1 = 제작 보고의 공개 값과 같음 — 심사 apksigner 확인).
- 기기 줄 8~12(이 PC에서 `device-run.sh`) → 8절 1 확인 → OK면 PASS 보류.

**사용자 결정 거리**
1. SPEC AC-15-6 "권한은 인터넷뿐" ↔ 계획 9절 병합분(USE_BIOMETRIC · USE_FINGERPRINT · DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION): SPEC 글을 계획에 맞출지 · `tools:node="remove"`로 뺄지(Credential Manager 동작 위험 — 기기 확인 필요).
2. Android CLI 통계 가능성(7절 ⑦): CLI 지우기 · 앞으로 쓰지 않기.
3. 가짜 서버 · 기기 실행 스크립트를 커밋할 곳(8절 4).
4. 웹 PWA 대비 고치기(8절 8) · Next 보안 안내 · 하루 실행 지연(8절 7 — O8 밖).

**다음 계획 후보**: `dataExtractionRules`(8절 3) · `device-run.sh` 캡처 확인 줄(8절 5) · CORS를 열 계획이 생기면 idToken 경로 출처 검사(8절 2).

## 10. 요청 · 정리

- 요청: 운영(Vercel) **GET 2/20**(`prod-requests.log`) · 인증 · 쓰기 · DB 0 · api.github.com GET 5(토큰 0 · `github-requests.log` — 액션 3개 태그 → 커밋: checkout v7.0.1 · setup-gradle v6.4.1(주석 태그 풀기) · upload-artifact v7.0.2 = 고정 SHA와 같음 · 각 주판의 가장 높은 판) · registry.npmjs.org(npx · Next 안내) · 가짜 서버 127.0.0.1:3100(로컬) · 실제 Google · newsdata · OpenAI · adb 0.
- 워크플로(`jworkflow.mjs`, 구문 = ex12 `confbox` parseYAML · daily · push.yml도 읽힘): 22/22 — 트리거 push(`android/**` · 이 파일) + 수동(입력 `release` boolean) · `pull_request` · schedule 0 · `contents: read`(맨 위만) · 브랜치별 차례 · checkout `persist-credentials: false` · setup-gradle `basic` · 요약 끔 · JDK 17 · SDK 37.0 확인 · 빌드 명령 = 계획 · release = `workflow_dispatch` + 입력 + main일 때만 · Secret은 env로만(run 안 `${{ }}` 0) · `$RUNNER_TEMP` · `umask 077` · 풀어 쓴 뒤 `unset` · `if: always()` 키 지움(release 단계 뒤) · 요약 = APK sha256 · 인증서 SHA-256만 · 산출물 30일 · 없으면 실패 · 값 출력 0(첫 실행에서 주석 글자 "set -x 0"에 걸린 심사 정규식은 주석을 빼고 다시 — 0). 실행 · 푸시 · Secret 0.
- 띄운 것 · 끈 것: Gradle 데몬 1 + Kotlin 데몬 → `./gradlew --stop`(남은 java 0) · 가짜 서버 1번(PID 하나 — 그 PID만 `taskkill`, `server.pid`는 원래 바이트로 되돌림) · node · java 단일 실행(스스로 끝남) · aapt2 · apksigner. 끝 확인(11:13): java · adb · 내 node 0 · 3100 닫힘 · 사용자 프로그램(Stream Dock node20 3개 · PID 13796 · Code.exe · claude · firefox · steam) 손대지 않음.
- 쓴 곳: `android/.data/o8-judge-20261103/`(git 제외) · 이 보고 · scratchpad 임시(LF 사본 · 변이 백업 · 컴파일 클래스 · HEAD openapi 사본 — 끝에 지움). 저장소 그 밖 변경 0(3절 지문).
- 심사 스크립트 · 증거: `jscope.mjs` · `jsdk.mjs` · `jtestresults.mjs` · `jdeps.mjs` · `japk.mjs` · `jpng.mjs` · `jlfcopy.mjs` · `jfake.mjs` · `JAppData.java` · `jprod.mjs` · `jprodcheck.mjs` · `JParse.java` · `jstrings.mjs` · `jliterals.mjs` · `JContrast.java` · `jsquarify.mjs` · `jmut.mjs` · `jmut-run.sh` · `jworkflow.mjs` · `jversions.mjs` · `jinstr.mjs` · `jsecrets.mjs` · `jsecrets-ctx.mjs` · 결과 `scope-*.json` · `build-full.log` · `tests-*.json` · `deps-*` · `apk/` · `apk-check.json` · `ex12-*` · `redocly-*` · `fake-check.json` · `app-data-vs-fake-*.txt` · `prod-*` · `strings-check.json` · `contrast-check.txt` · `mut-*` · `workflow-check*.txt` · `instr-compiled.json` · `secrets-*`.
