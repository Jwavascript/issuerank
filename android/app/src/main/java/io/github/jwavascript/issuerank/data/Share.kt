package io.github.jwavascript.issuerank.data

/**
 * 공유(웹 issueShare 1286~1289 · SITE_SHARE 1284): 글 = "[섹션] 제목"(요약 · 숫자 · 원문 주소 없음), 주소 앞은 늘 배포 주소
 * (debug 서버 주소와 무관 — COMMON 5). ACTION_SEND의 EXTRA_TEXT = sendText(웹 링크 복사 1300 `${text}\n${url}`와 같은 꼴), 제목 = title.
 */
object Share {
    const val BASE = "https://issuerank-kappa.vercel.app"

    data class Data(val title: String, val text: String, val url: String) {
        val sendText: String get() = "$text\n$url"
    }

    fun issueUrl(id: String): String = "$BASE/s?issue=${Web.encodeURIComponent(id)}"

    /** 랭킹 카드 · 북마크 카드(bookmark.issue) 모두 */
    fun issue(issue: Issue, s: Strings): Data =
        Data(s[Str.BRAND], s[Str.SHARE_ISSUE_TEXT, s[issue.section.label], issue.title], issueUrl(issue.id))

    /** 맨 아래 "이슈랭크 공유" */
    fun site(s: Strings): Data = Data(s[Str.BRAND], s[Str.SHARE_SITE_TEXT], "$BASE/s")
}

/**
 * 정책 · 문의 링크(맨 아래 538 · 로그인 창 557) — Custom Tab으로 여는 웹 페이지. 공유 주소처럼 늘 배포 주소
 * (가짜 서버에는 화면 페이지가 없음 — fake-server README 7절)
 */
object Links {
    const val PRIVACY = "${Share.BASE}/privacy"
    const val TERMS = "${Share.BASE}/terms"
    const val CONTACT = "${Share.BASE}/privacy#contact"
}
