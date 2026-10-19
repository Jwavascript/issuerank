# PLAN_ops_O3_insta_card — 인스타 "오늘의 이슈" 카드 1단계

> 이유: 이슈랭크를 아는 사람이 아직 없어서 첫 사용자를 데려올 채널이 필요하다(PRODUCT_PLAN 5절, 첫 채널 = 인스타 카드). 그런데 운영자가 매일 아침 헤드라인을 손으로 옮겨 이미지를 만들기는 어렵다. 그래서 서버가 그날 랭킹으로 4:5 카드 PNG와 캡션을 자동으로 만들고(위젯과 같은 순서, D22), 운영자는 휴대폰으로 `/card` 페이지를 열어 이미지를 저장하고 캡션을 복사해 **직접** 올린다(PRODUCT_PLAN 4절 O3 "처음엔 수동 게시"). 자동 게시는 조사만 해 두고 사용자가 결정한다.
> 코드 폴더: `ex12/` (운영 폴더, 복사 없음 — PLAN.md D21). 보고: `reports/ops_O3_insta_card_build.md` · `reports/ops_O3_insta_card_judge.md`.
> SPEC 근거: **US-12 · AC-12-1~4**(운영 O3, PLAN.md D28). 회귀: AC-11-1(같은 순서 규칙) · AC-03-1 · AC-03-2 · AC-05-1 · AC-09-3 · 범위 밖 "기사 본문 전체 재게시". 적용 결정: D3(04:17 · 실제 06:26~06:34 완료) · D5 · D8 · D10 · D16 · D21 · D22 · D24 · D26(인앱 브라우저 안내는 문제 확인 시 O3). **D28**(인스타 카드 규칙, 확정 — 사용자 2026-10-19).

## 이번 범위
- 서버: 공개 `GET /api/card`(PNG) · `GET /api/card/caption`(JSON) — Next 라우트 2개가 같은 순수 함수(`server/card.ts`)를 씀. 화면: 운영자용 `public/card.html`(`/card`, 미리보기 · 이미지 저장 · 캡션 복사 · 한 줄 빼기). 글꼴: Noto Sans KR 2굵기 + 라이선스 원문을 `ex12/assets/fonts/`에 넣음.
- 새 패키지: **없음** — `next/og`의 `ImageResponse`가 Next 16.4에 들어 있음(`node_modules/next/dist/compiled/@vercel/og` 1.0.1, Satori + Resvg → PNG). 공식: "This isn't required for Next.js App Router projects, as the package is already included" https://vercel.com/docs/og-image-generation
- 확정(D28, 사용자 2026-10-19): ① 같은 사건 두 줄은 운영자가 `/card`에서 최대 3개 빼면 다음 순서로 채움(자동 판정 없음) ② 카드 6줄 · 연관기사 수 숫자 없음 ③ 출처는 하단 한 줄 ④ 글꼴 Noto Sans KR Bold · Regular ⑤ 밝은 카드 하나 ⑥ AI 요약 넣지 않음 ⑦ 해시태그 5개(캡션 꼴 그대로) ⑧ `/card`는 Web Analytics 제외 ⑨ 자동 게시는 1단계 2~4주 운영 뒤 결정(이번 범위 밖, 조사 표만) ⑩ 첫 게시 전 사용자 할 일 = newsdata 약관 캡처 · 제목 재게시 법률 검토 권장 ⑪ 인스타 인앱 브라우저의 Google 로그인은 검증 17에서 사용자 확인(로그인 코드 변경 없음).
- 하지 않음: 자동 게시(아래 2단계 조사 표, 사용자 결정 전) · JPEG 변환 · Instagram/Meta 계정 · 키 · 푸시 알림 · PWA(O4) · 이슈 공유 카드 버튼(O4 — 같은 `server/card.ts` 렌더러를 이슈 1개짜리로 재사용 가능, 메모만) · 메인 화면에서 `/card`로 가는 링크 · 위젯(`/api/widget` · `widget.js` · `widget.html`) · 로그인 · 정책 페이지 문안 · 하루 실행 · AI · 수집 · 묶기 · `.github/workflows/daily.yml`(Actions 단계 추가 안 함) · Hono 앱 · BFF · 기존 API 계약 · DB 스키마 · `web/`.

## 카드 규칙 (D28)
| 항목 | 규칙 |
|---|---|
| 크기 · 형식 | **1080×1350 PNG(4:5 세로)**. 근거: 인스타 피드는 폭 1080px 이상 · 비율 1.91:1~4:5면 원래 해상도 유지, 범위 밖은 잘림 https://help.instagram.com/1631821640426723 (같은 글의 옛 판에 상한 3:4 표기가 있어 **확인 필요** — 1080×1350은 두 표기 모두 범위 안). 그래프 API도 4:5~1.91:1 · 폭 320~1440 https://developers.facebook.com/docs/instagram-platform/instagram-graph-api/reference/ig-user/media |
| 내용 순서 | 그날 `buildWidget` 결과(D22 · AC-11-1: 섹션 1위들 → 나머지, 각각 D16)의 **앞에서 6개**(빼기한 id는 건너뛰고 다음 항목으로 채움). 항목이 6개보다 적으면 있는 만큼(최소 1). 위젯 · 사이트와 같은 순서라 따로 순서 규칙을 만들지 않음. 날짜 = 랭킹 date(수집한 날, D3), D8 대체 섹션 항목도 위젯처럼 포함 |
| 넣는 것(위→아래) | ① 왼쪽 위 사이트와 같은 브랜드 마크(3칸 사각형) + `이슈랭크`, 오른쪽 위 `{M}월 {D}일 {요일}`(예 `10월 19일 월요일`) ② 제목 `오늘의 이슈` ③ 부제 `어제 가장 많이 보도된 뉴스` ④ 6줄: 왼쪽 칸에 번호 `1`~`6`과 섹션 칩(`정치` · `경제` · `사회` · `생활·문화` · `세계` · `IT·과학`, 바탕 = 사이트 밝은 테마 `--s-*` 색, 글자 #ffffff), 오른쪽에 제목(최대 2줄) ⑤ `issuerank-kappa.vercel.app` + `섹션별 순위 · 3줄 요약 · 원문 링크` ⑥ 맨 아래 작은 글 `{updatedLabel} · 기사 제목 출처: 각 언론사(newsdata.io 수집) · 순위는 연관기사 수 기준 참고 지표`(updatedLabel = 위젯과 같은 값, 예 `10.19 오전 6:34 수집`) |
| 넣지 않는 것 | 기사 본문 · 설명문 · AI 3줄 요약 · 연관기사 수 숫자(D22와 같음 · 10-18 pol 검색어 OR 오용처럼 숫자가 부풀 수 있음) · 원문 이미지 · 언론사 로고 · 원문 URL · 그림 문자(이모지) |
| 제목 다듬기 | 앞뒤 공백 정리 · 연속 공백 하나로 · `\p{Extended_Pictographic}` 제거(@vercel/og가 그림 문자를 jsdelivr에서 받으러 나가므로 — `index.node.js`의 `loadDynamicAsset` · `twemoji` 주소 확인) · 그 뒤 **34자(코드 포인트) 넘으면 앞 33자 + `…`**. 제목 낱말은 고치지 않음(원문 그대로) |
| 모양 · 글자 | 바탕 #f7f7f5 · 글자 #161616 · 흐린 글자 #6f6b65(사이트 밝은 토큰). 바깥 여백 72px. 크기: 브랜드 34 · 날짜 30 · 제목 88 · 부제 34 · 번호 44 · 칩 24 · 헤드라인 40(줄 높이 1.3, 2줄 넘지 않게) · 주소 30 · 하단 22. 굵기: 브랜드 · 제목 · 번호 · 칩 · 헤드라인 · 주소 Bold, 나머지 Regular. 레이아웃은 flex만(Satori는 grid 미지원 https://nextjs.org/docs/app/api-reference/functions/image-response). 실제 제목으로 3줄 · 넘침이 보이면 크기만 고치고 보고에 이유 |
| 다크 | 밝은 카드 하나만. 이미지는 보는 사람 테마를 따르지 않음 |
| 글꼴 | **Noto Sans KR Bold · Regular OTF**(notofonts/noto-cjk `Sans/SubsetOTF/KR`, 4,816,044 B · 4,644,748 B ≈ 9.4MB) + `OFL.txt`. 라이선스 SIL OFL 1.1, Reserved Font Name 없음 https://github.com/notofonts/noto-cjk/blob/main/Sans/LICENSE . 고른 이유: 제목에 美 · 中 · 日(D19-2)이 자주 나오는데 **Pretendard 1.3.9 Bold(1.5MB, OFL 1.1 · RFN "Pretendard")에는 한자 글리프가 없음**(계획 단계에서 cmap 직접 확인: 한글 11,172자는 둘 다 있음, 美中日北韓與野尹李 = Pretendard 0/9 · Noto 9/9). 글리프가 없으면 @vercel/og가 Google Fonts에서 글꼴을 받으러 나감(`fonts.googleapis.com/css2` 코드 확인) → 바깥 요청 0을 지키려고 한자 있는 글꼴을 넣음. 형식은 ttf · otf · woff만(woff2 안 됨, 위 Next 문서). 읽기: Node 런타임에서 첫 카드 요청 때 `fs.readFile(join(process.cwd(), 'assets/fonts/…'))` 한 번, 모듈 범위에 보관 |
| 배포 포함 | `next.config.mjs`에 `outputFileTracingIncludes: { '/api/card': ['./assets/fonts/*'] }`(키 = 라우트 경로, 값 = 프로젝트 기준 glob https://nextjs.org/docs/app/api-reference/config/next-config-js/output). 캡션 라우트 · `/api/[...path]` 함수에는 글꼴이 들어가지 않음(다른 API 시작 시간 그대로) |
| 500KB 한도 | Next · Vercel 문서에 "Maximum bundle size of 500KB … includes … fonts"가 있으나 Node 런타임에도 걸리는지 문서에 없음 → **확인 필요**. Node 함수 크기 한도는 250MB https://vercel.com/docs/functions/limitations . 배포 줄 15에서 200이 안 나오면 멈추고 사용자에게: 글꼴 부분 집합 만들기(Noto는 RFN이 없어 OFL상 수정 배포 가능, 도구는 새 패키지라 승인 필요) |

## API · 페이지
| 주소 | 규칙 |
|---|---|
| `GET /api/card?date=YYYY-MM-DD&hide=id,id` | 새 `app/api/card/route.tsx`(runtime nodejs · `dynamic = 'force-dynamic'` — 빌드 때 DB를 읽지 않게). `date` 생략 = 최근 수집일(D3, 위젯과 같음). 200 = `Content-Type: image/png` · `Cache-Control: public, max-age=0, s-maxage=300`(O1과 같은 CDN 300초 — 같은 날 수동 재실행 D5도 5분 안에 반영, 급하면 Vercel CDN Purge https://vercel.com/docs/caching/cdn-cache/purge). **PNG를 끝까지 만든 뒤 응답**(Next `ImageResponse`는 스트림이라 렌더 실패가 200 뒤에 나므로 버퍼로 받아 확인) |
| 실패(두 주소 공통) | 날짜 형식 400 `{ "message": "날짜는 YYYY-MM-DD 형식으로 입력해 주세요" }` · 미래 400 `오늘 이후 날짜는 조회할 수 없습니다`(기존 `rankingsQuery` 그대로) · `hide`가 4개 이상 · 그날 위젯 목록에 없는 id · 중복 400 `요청 형식이 올바르지 않습니다` · 랭킹 없음 404 `해당 날짜의 랭킹 데이터가 없습니다` · DB 준비 실패 502 `API 서버에 연결할 수 없습니다`(로그 `CARD_START_FAILED`) · 렌더 실패 500 `{ "message": "카드를 만들 수 없습니다" }`(로그 `CARD_RENDER_FAILED`, 오류 객체 · 스택 출력 0). 실패에는 캐시 헤더 · Set-Cookie 없음. GET 말고는 405(Next 기본) |
| `GET /api/card/caption?date=&hide=` | 새 `app/api/card/caption/route.ts`. 200 `{ date, dateLabel, updatedLabel, items: [{ id, section, title }], hidden: [id], caption }`(items = 카드와 같은 6개 · 같은 순서 · 다듬기 전 원문 제목), 같은 캐시 헤더 |
| 캡션 문구 | 아래 꼴 그대로(줄바꿈 포함). 제목은 원문(그림 문자만 제거), 한 제목 100자 넘으면 99자 + `…`. 전체 ≤ 2,200자 · 해시태그 5개(그래프 API 한도 2,200자 · 30개 · @ 20개, 위 ig-user/media 문서). 캡션의 주소는 눌리지 않는 것으로 알려져 있어(**확인 필요**) 프로필 링크로 안내(프로필 웹사이트 칸 https://help.instagram.com/362497417173378) |
| 데이터 | DB 읽기만: 기존 `dbStore`의 `latestDate()` · `widget(date)`(Store · `server/app.ts` · `bff.ts` 그대로). 연결은 라우트 모듈에서 첫 요청 때 `connectDb({ max: 1 })` 한 번 |
| `/card` 페이지 | `public/card.html` + rewrite `/card`. 운영자용: `<meta name="robots" content="noindex">` · 메인 화면 링크 없음 · Web Analytics 스크립트 넣지 않음(아침 방문자 지표 D24를 운영자 방문이 흐리지 않게). 밝은/어두운 페이지 토큰은 `widget.html`과 같음(카드 이미지는 밝은 것 하나) |

캡션 꼴(10-19 예):
```
10월 19일 오늘의 이슈 — 어제 가장 많이 보도된 뉴스

1. [정치] {제목}
…
6. [{섹션}] {제목}

섹션별 순위 · 3줄 요약 · 원문 링크는 프로필 링크의 이슈랭크에서 볼 수 있어요.
기사 제목 출처: 각 언론사(newsdata.io 수집). 순위는 연관기사 수 기준 참고 지표입니다.

#오늘의이슈 #뉴스 #헤드라인 #시사 #이슈랭크
```

## 화면 변경
- 새 `/card`(360px 기준 세로 한 줄): `오늘의 이슈 카드` 제목 → 그날 날짜 · `{updatedLabel}` → 카드 미리보기(`<img>` 폭 100% · 최대 540px, alt `{M}월 {D}일 오늘의 이슈 카드`) → 버튼 `이미지 저장`(`download="issuerank-{date}.png"`, 안 되는 브라우저는 "이미지를 길게 눌러 저장해 주세요") · `캡션 복사`(성공 `복사했습니다` · 실패 `복사하지 못했습니다. 아래 글을 길게 눌러 복사해 주세요`) → 6줄 목록(각 줄 `빼기`, 3개 뺀 뒤에는 비활성) · `뺀 이슈 {n}개 · 되돌리기` → 캡션 글 상자(읽기 전용) → 올리는 순서 안내 1~3.
- 페이지는 먼저 `GET /api/card/caption`(빼기 있으면 `hide`)을 받고, 그 `date`로 `<img src="/api/card?date={date}&hide=…">`를 붙임(두 요청이 같은 날짜). date가 KST 오늘이 아니면 위에 `아직 오늘 카드가 없습니다. 마지막 카드: {M}월 {D}일`. 실패면 `카드를 불러오지 못했습니다` + `다시 시도`.
- 안내 1~3: ① `이미지 저장`(또는 길게 눌러 저장) ② `캡션 복사` ③ 인스타그램 › 새 게시물 › 저장한 이미지 › 비율이 4:5(세로)인지 확인 › 캡션 붙여넣기 › 공유. 기존 화면(`/` · `/widget` · 정책 페이지)은 그대로.

## 2단계 자동 게시 조사 (이번 범위 밖 — 1단계 2~4주 운영 뒤 사용자 결정, D28)
| 항목 | Meta 공식 문서 내용 |
|---|---|
| 계정 | Instagram **프로페셔널 계정**(비즈니스 또는 크리에이터) https://developers.facebook.com/docs/instagram-platform/overview |
| 로그인 · 페이지 | Instagram 로그인(Business Login for Instagram)이면 Facebook 페이지 불필요, Facebook 로그인이면 페이지 연결 + 관리 권한 필요(같은 문서) |
| 권한 | Instagram 로그인: `instagram_business_basic` · `instagram_business_content_publish` / Facebook 로그인: `instagram_basic` · `instagram_content_publish` · `pages_read_engagement` https://developers.facebook.com/docs/instagram-platform/content-publishing/ |
| 앱 검수 | 내 계정 · 내가 관리하는 계정만 쓰면 Standard Access(기본), 남의 계정까지면 Advanced Access = 앱 검수 + 비즈니스 인증(overview). 자기 계정 게시에 검수가 필요 없다는 명시 문장은 없음 → **확인 필요** |
| 토큰 | 단기 약 1시간 → 서버에서 장기 토큰(60일)으로 교환, 24시간 지난 유효 토큰은 `refresh_access_token`으로 60일 연장, 60일 안 갱신하면 만료 https://developers.facebook.com/docs/instagram-platform/reference/refresh_access_token/ → 갱신 예약 작업 · GitHub Secrets 필요 |
| 게시 흐름 · 이미지 | 컨테이너 만들기(`POST /<IG_ID>/media`, `image_url` · `caption`) → `POST /<IG_ID>/media_publish`. 이미지는 **JPEG만**(PNG 안 됨) · 최대 8MB · sRGB · 게시 시점에 공개 서버에 있어야 함(content-publishing · ig-user/media 문서) → 지금 PNG 카드를 그대로 못 씀, JPEG 변환 방법은 **확인 필요**(새 패키지 또는 Actions 도구, 결정 대상) |
| 한도 | 24시간 이동 창에 API 게시 100개(같은 문서 캐러셀 절은 50개로 적혀 충돌 → `GET /<IG_ID>/content_publishing_limit`로 확인). 하루 1개라 여유 |
| 준비물 | Meta 개발자 계정 · 앱 · 앱 secret · 장기 토큰 · IG 사용자 id. 실행 위치 후보 = 하루 실행 뒤 Actions 단계(daily.yml 변경 = 하루 실행 범위라 별도 승인) |

## 저작권 · 약관 · 운영
- 제목 재게시 위험: 카드는 언론사 기사 **제목**을 이미지로 SNS에 다시 싣는 것. 사이트(제목 · 링크)보다 노출이 넓음. 제목의 저작물성 · 시사보도 예외 적용은 **확인 필요(법률 판단 안 함, 게시 시작 전 법률 검토 권장)**. 보수적 기본안 = 제목 원문 그대로(편집 · 합성 안 함) · 본문 · 설명문 · 요약 · 사진 · 로고 0 · 하단에 "기사 제목 출처: 각 언론사(newsdata.io 수집)" · 원문은 사이트의 언론사 링크로(`terms.html` 3절과 같은 입장).
- newsdata: 홈페이지 FAQ는 제목 · 짧은 설명 · 언론사 이름 · 날짜는 게시 가능, 이미지 · 전체 본문은 안 됨, 원 언론사 표기 권장 https://newsdata.io/ — 그러나 같은 회사 블로그 비교표는 무료 플랜 상업 이용 "No" https://newsdata.io/blog/best-free-news-api/ , 약관 원문은 이 기기에서 읽지 못함(스크립트 렌더) → **확인 필요**. 첫 게시 전 사용자 할 일(D28): newsdata 약관(재배포 · 표기 · 무료 플랜) 화면 캡처(PRODUCT_PLAN 6절과 같은 할 일) · 제목 재게시 법률 검토 권장.
- 준비 시각: 예약 04:17 · 실제 시작 06:26~06:30 · 끝 06:30~06:34(`reports/ex09_daily.md`) + CDN 300초 → **보통 06:40쯤 새 카드**. 운영자는 07시 이후 올림. 하루 실행 실패면 `/card`가 전날 카드와 "아직 오늘 카드가 없습니다" 안내를 보임(그날은 올리지 않거나 D5 수동 재실행 뒤).
- 비용: Vercel Hobby 월 함수 호출 100만 · Active CPU 4시간 · 전송 100GB 무료 https://vercel.com/docs/limits/fair-use-guidelines . 카드 1장 렌더 = 함수 1회(CPU 실측은 검증 12), 운영자 하루 몇 장 + CDN 300초 캐시 → 한도의 아주 작은 부분. 함수 최대 300초 · 응답 4.5MB · 메모리 2GB(functions/limitations). Actions 시간 0(워크플로 변경 없음, 저장소 공개 = 표준 러너 무료 https://docs.github.com/en/billing/concepts/product-billing/github-actions). newsdata · OpenAI 추가 0. Hobby는 비상업 전용 — 광고 · 결제가 붙으면 Pro(같은 문서).
- 측정(D24 범위 안, 새 코드 없음): 인스타 유입은 Vercel Web Analytics의 참조처(instagram)에서 봄 — Hobby에서 참조처 · UTM 표시 범위는 **확인 필요**. 카드 생성 수는 Vercel 사용량의 함수 호출로 봄.
- 인앱 브라우저: 인스타 프로필 링크는 인스타 앱 안 브라우저로 열릴 수 있고, Google은 임베디드 웹뷰 OAuth를 막음(`403 disallowed_useragent`) https://developers.google.com/identity/protocols/oauth2/policies → 인스타 안에서 Google 로그인이 되는지는 **확인 필요**(검증 17 ③, 문제면 안내 문구는 별도 결정 — 이번엔 로그인 코드 변경 없음).

## SPEC (SPEC.md US-12 · AC-12-1~4와 같음)
- **US-12 (오늘의 이슈 카드, 운영 O3)** — 운영자로서, 매일 아침 그날 헤드라인 카드 이미지와 캡션을 받아 인스타그램에 올리고 싶다. 그래야 새 사용자를 이슈랭크로 데려올 수 있다.
- **AC-12-1** — Given 그날 랭킹이 있을 때, When 카드를 요청하면, Then 1080×1350 PNG에 날짜 · 위젯과 같은 순서(AC-11-1)의 앞 6개 제목과 섹션 · 사이트 주소 · 수집 시각 · 출처 문구가 보이고, 본문 · 요약 · 연관기사 수 숫자 · 원문 이미지 · 언론사 로고는 없다.
- **AC-12-2** — Given 카드 페이지를 열었을 때, When 이미지 저장 · 캡션 복사를 누르면, Then 같은 날짜 카드 PNG가 저장되고 같은 6개 제목 · 사이트 안내 · 해시태그로 된 2,200자 이하 캡션이 복사된다.
- **AC-12-3** — Given 그 날짜 랭킹이 없거나 날짜가 잘못됐을 때, Then 기존 문구의 404 · 400이 오고, 카드 페이지는 오늘 데이터가 아니면 마지막 카드 날짜와 안내를 보인다.
- **AC-12-4** — Given 같은 사건이 두 줄로 보일 때, When 운영자가 한 줄을 빼면, Then 그 줄은 빠지고 다음 순서 이슈가 채워지며 카드와 캡션이 같다.

## 구현 순서
1. 복사 없음(D21): `git status --short`로 사용자 변경 확인 · 보존, 작업 전 해시(검증 1) 기록. 글꼴 2개 · `OFL.txt`를 notofonts/noto-cjk 원본에서 받아 `ex12/assets/fonts/`에 두고 sha256 · 크기를 보고에 적음.
2. 순수 함수 `server/card.ts`: `buildCard(widget, hide)` → `{ date, dateLabel, updatedLabel, items(≤6), hidden }` · 제목 다듬기 · `cardCaption()` · 쿼리 검사(기존 `rankingsQuery` + hide). `server/card-image.tsx`: 카드 JSX + 글꼴 읽기 + `ImageResponse` → PNG 버퍼.
3. 라우트: `app/api/card/route.tsx` · `app/api/card/caption/route.ts`(둘 다 같은 저장소 준비 · 오류 처리) → `next.config.mjs`(rewrite `/card` 1줄 · `outputFileTracingIncludes`) → `openapi.yaml`(`/api/card` image/png 200 · 400 · 404, `/api/card/caption` `CardCaption` 스키마, info.version 0.4.0).
4. `public/card.html` → 테스트: 새 `test/card.test.ts`(순수 함수 · 캡션 · 라우트 처리기를 메모리 저장소로) · `test/card-image.test.ts`(실제 렌더) · `test/card-ui.test.ts`(card.html 함수를 VM에서).
5. 로컬 검증 1~14 → `reports/ops_O3_insta_card_build.md` → 사용자 할 일: ① 커밋 · 푸시 요청 → 배포 줄 15 ② 다음 날 아침 16 ③ 휴대폰 17 ④ 첫 게시 전 newsdata 약관 캡처 · 제목 재게시 법률 검토 권장(D28, 18번에 기록).
마지막. `bash scripts/ex12-ubuntu.sh npm test` · `bash scripts/ex12-ubuntu.sh npm run build`

## 검증 방법
- 실행: 래퍼 `bash scripts/ex12-ubuntu.sh …`. 로컬 서버는 `npm run build` 뒤 `npx next start -p 3000 --hostname 127.0.0.1`(이 기기 dev Turbopack은 파일 감시 한도로 실패, PLAN.md 10절). 안전 실행 = ex12 방식(`ex12/.env`만 · stdout + stderr 비밀값 true/false 검사 후 false일 때만 출력 · 응답 원문 대신 상태 · 크기 · bool). 산출물은 `ex12/.data/o3-build-<날짜>/`.
- 한도: newsdata · OpenAI 0 · Instagram/Meta 요청 0 · 렌더 중 바깥 fetch 0 · DB 쓰기 0(읽기는 랭킹 행 · collect_runs만) · 로컬 3000 요청 ≤ 60 · 배포 요청 역할마다 ≤ 30 · 임시 계정 0. 날짜는 "SQL 최근 수집일 · 직전 수집일" 또는 고정 날짜. 소유 서버 · Chromium PID만 기록해 종료(넓은 패턴 kill 금지 — 사용자 VS Code가 Electron), 끝나면 `curl` 000 · `ps`에 그 PID 0(proot 포트는 Termux `ss`에 안 보임).

| 확인 | 방법 | 기대 결과 |
|---|---|---|
| 1 변경 범위 · 불변 | `git status --short` · `git diff --stat` · 작업 전후 `sha256sum ex12/server/{app,auth,ai,bff,daily,steps,collect,group,grouping,newsdata,summary,summarize,ranking,schemas}.ts ex12/server/db/store.ts ex12/server/routes/*.ts "ex12/app/api/[...path]/route.ts" ex12/public/{index,widget,privacy,terms}.html ex12/public/widget.js ex12/package.json ex12/package-lock.json .github/workflows/daily.yml` | 바뀐 · 새 파일 = 구현 순서 1~4 목록뿐 · 해시 모두 같음(새 패키지 0) · ex01~ex11 · `web/` · SPEC · PLAN.md 변경 0 |
| 2 카드 데이터(단위, AC-12-1 · 12-4) | `test/card.test.ts`: 샘플 2026-10-07 위젯 ① 그대로 ② hide 1개 · 3개 ③ hide 4개 · 없는 id · 중복 ④ 항목 3개뿐 ⑤ 제목 33 · 34 · 35자 · 공백 섞임 · 이모지 · 美/中 ⑥ dateLabel 2026-10-07 · 2026-10-19 · 2026-01-01 | ① items = `buildWidget` items 앞 6개(id · 순서 같음) ② 뺀 id 없음 · 다음 항목으로 6개 · hidden = 보낸 순서 ③ 400 `요청 형식이 올바르지 않습니다` ④ 3개 ⑤ 33 · 34자 그대로 · 35자 → 33자 + `…` · 공백 하나로 · 이모지 0 · 한자 그대로 ⑥ `10월 7일 수요일` · `10월 19일 월요일` · `1월 1일 목요일` |
| 3 캡션(단위, AC-12-2) | 같은 테스트: 고정 입력의 캡션 전문 비교 · 100자 넘는 제목 6개 | 위 꼴과 한 글자도 다르지 않음 · 번호 1~6 = items 순서 · 섹션 이름 = SECTIONS · 긴 제목 99자 + `…` · 전체 ≤ 2,200자 · `#` 5개 · URL · 숫자(연관기사 수) 0 |
| 4 라우트(테스트, AC-12-3) | 처리기를 메모리 저장소로 직접 호출: `/api/card/caption` · `?date=2026-10-07` · `?date=2026-10-08`(내일) · `?date=2026-1-7` · `?date=2026-10-01`(데이터 없음) · 저장소 준비 실패 · `/api/card` 같은 경우들 | 200 칸 = 표 · `Cache-Control: public, max-age=0, s-maxage=300` · Set-Cookie 0 / 400 두 문구 · 404 문구 · 502 문구 · 실패에는 Cache-Control 없음 · 로그는 안전 코드만 |
| 5 PNG(실제 렌더 테스트, AC-12-1) | `test/card-image.test.ts`: 전역 `fetch`를 불리면 실패하는 가짜로 바꾸고 샘플 날짜 렌더 · 美中日 · 긴 제목 섞은 가짜 위젯 렌더 · 렌더 함수가 던지게 한 경우 | 200 · `image/png` · 앞 8바이트 PNG 서명 · IHDR 1080×1350 · 크기 ≤ 4.5MB(목표 ≤ 1.5MB, 실제 값 기록) · fetch 호출 0 / 던짐 → 500 `카드를 만들 수 없습니다`(200 뒤 끊김 아님) |
| 6 화면 함수(VM, AC-12-2 · 12-3 · 12-4) | `test/card-ui.test.ts`: card.html 함수를 잘라 가짜 `fetch` · `navigator.clipboard` · `Date`로 ① 정상 ② 빼기 1 → 3 → 되돌리기 ③ date ≠ 오늘 ④ caption 500 · 네트워크 실패 ⑤ clipboard 거절 | ① img src = `/api/card?date={caption.date}` · download 이름 `issuerank-{date}.png` ② 요청 hide = 뺀 id들 · 3개 뒤 버튼 비활성 · 되돌리기면 hide 없음 ③ `아직 오늘 카드가 없습니다. 마지막 카드: …` ④ `카드를 불러오지 못했습니다` · 다시 시도 ⑤ 실패 문구 |
| 7 OpenAPI | `npx @redocly/cli lint openapi.yaml` · 표와 대조 | 오류 0 · 두 경로 상태 · 칸 = 표 · 다른 경로 차이 0 |
| 8 빌드 산출물 | `npm run build` 출력 · `.next/server/app/api/card/route.js.nft.json` · `…/[...path]/route.js.nft.json` | `/api/card` · `/api/card/caption` = 동적(ƒ, 빌드 중 DB 접속 0) · card nft에 otf 2개 있음 · catch-all nft에 otf 0 |
| 9 로컬 회귀 · 빌드 | 래퍼 `npm test` · `npm run build` | 기존 158개 + 새 테스트 통과 · 빌드 · 타입 성공 · 새 패키지 0 |
| 10 로컬 서버 실제 데이터(AC-12-1 · 12-3) | 안전 실행 `next start` · Node fetch `/api/widget` · `/api/card/caption` · `/api/card` · `?date={직전 수집일}` · `?date={KST 내일}` · `?date=2026-01-01` · `/card` · `/api/rankings` | caption date = widget date = SQL 최근 수집일 · caption items id = widget items 앞 6개 · PNG 200 · 1080×1350 / 직전 수집일 200 / 400 · 404 문구 / `/card` = card.html 본문 · `/api/rankings` 그대로(catch-all 영향 0) |
| 11 실제 이미지 눈으로(AC-12-1) | 10번 PNG 2장(최근 · 직전 수집일)을 `.data/`에 저장해 이미지로 열어 봄 + 최근 7개 수집일 위젯 제목 전체(DB 읽기)의 글자를 글꼴 cmap과 대조 | 한글 · 한자 · 따옴표 · `…` 깨짐(□) 0 · 헤드라인 2줄 이하 · 겹침 · 잘림 0 · 표의 문구 · 색 · 하단 출처 보임 · 숫자 · 요약 · 로고 0 · cmap에 없는 글자 0(있으면 글자 목록 보고) |
| 12 렌더 시간 | 10번에서 캐시 없는 `/api/card` 3번 응답 시간 · 첫 요청(글꼴 읽기 포함) | 값 기록 · 각 < 10초(함수 한도 300초) |
| 13 로컬 화면(Chromium CDP 360×800 · 1280, AC-12-2 · 12-4) | `judge-lib.mjs` 방식으로 `/card`: 이미지 로드 · 이미지 저장 버튼 속성 · 빼기 1번 → 이미지 · 캡션 갱신 · 캡션 복사(권한 못 주면 SKIP 사유) · 다크 · 가로 스크롤 | 이미지 natural 1080×1350 · 빼기 뒤 caption items와 img src hide 일치 · 복사 내용 = 캡션 · 다크 토큰 · 가로 스크롤 0 · `/_vercel/insights` 요청 0 · JS 예외 0 |
| 14 안전 · 한도(로컬) | 요청 카운터 · 비밀값 검사 · DB 내용 해시 전후 · 바깥 요청 기록 · 프로세스 | 3000 요청 ≤ 60 · newsdata · OpenAI · Google Fonts · jsdelivr 0 · 누출 false · DB 해시 같음 · 소유 PID 0 |
| 15 배포(푸시 뒤) | 배포 주소 Node fetch `/api/card/caption` · `/api/card` 2번(10초 간격) · `/card` · `/api/widget` | 200 · PNG 1080×1350 · 둘째 `x-vercel-cache: HIT` · 클라이언트 cache-control에 s-maxage 없음 · caption items = widget 앞 6개. 200이 아니면 멈추고 원인(500KB · 글꼴 포함) 보고 |
| 16 아침 준비(푸시 다음 날) | 그날 하루 실행이 끝난 뒤 ~ 09:00 KST 배포 `/api/card/caption` 1번 · `/card` 1번 | date = 그날 · "아직 오늘 카드가 없습니다" 없음 · 실제 준비 시각 기록 |
| 17 휴대폰 · 인스타(사용자) | 사용자가 폰으로 `/card` → ① 이미지 저장 · 캡션 복사 ② 인스타 새 게시물에서 저장한 이미지가 4:5로 잘림 없이 미리보기 · 캡션 붙여넣기(올리기는 선택) ③ 인스타 프로필 링크(또는 DM 링크)로 사이트를 열어 이슈 상세 · Google 로그인 시도 | ① ② 결과 · 캡처 전달 전에는 [SKIP — 사용자 확인 대기](PASS 아님) ③ 사이트 · 상세 열림 · Google 로그인 결과 기록(`disallowed_useragent`면 별도 결정, 이번 FAIL 아님) |
| 18 저작권 기본안(AC-12-1) | 2 · 3 · 11번 결과와 card.html 원문 | 본문 · 설명문 · 요약 · 연관기사 수 · 원문 이미지 · 로고 · 원문 URL 0 · 출처 문구가 카드 · 캡션 둘 다 있음 · newsdata 약관 캡처 여부(사용자) 기록 |
