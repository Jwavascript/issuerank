package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.TestFiles.webCases
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 공유 글 = 웹 issueShare · SITE_SHARE / 앱 경로 = 웹 issueShortcut · viewFromHash(web/text.json) + 상세 열기 규칙 */
class ShareAndRouteTest {
    private val s = XmlStrings
    private val seed = ApiJson.ranking(TestFiles.fixture("rankings-d0.json"))
    private val bookmarkOnly = Issue("2026-10-30-wor-3", seed.issuesOf(SectionKey.CUL)[0].date, SectionKey.WOR, 5, "랭킹에 없는 북마크 카드 [괄호] & 기호", null, 112, "https://news.naver.com/")

    @Test
    fun issueShareEqualsWeb() {
        val cases = webCases("text.json", "issueShare")
        assertEquals(6, cases.size)
        for (c in cases) {
            val id = c.getString("id")
            val issue = seed.issueById(id) ?: bookmarkOnly.takeIf { it.id == id }
            if (c.isNull("data")) { assertNull(id, issue); continue }
            val e = c.getJSONObject("data")
            val got = Share.issue(issue!!, s)
            assertEquals(id, Share.Data(e.getString("title"), e.getString("text"), e.getString("url")), got)
            assertEquals("${e.getString("text")}\n${e.getString("url")}", got.sendText)
        }
        val site = webCases("text.json", "siteShare").single().getJSONObject("data")
        assertEquals(Share.Data(site.getString("title"), site.getString("text"), site.getString("url")), Share.site(s))
    }

    @Test
    fun shareUrlAlwaysDeployedAddressAndEncoded() {
        assertEquals("https://issuerank-kappa.vercel.app/s?issue=2026-11-03-pol-21", Share.issueUrl("2026-11-03-pol-21"))
        assertEquals("https://issuerank-kappa.vercel.app/s?issue=a%26b%3Dc%20d", Share.issueUrl("a&b=c d"))
        assertEquals("https://issuerank-kappa.vercel.app/privacy", Links.PRIVACY)
        assertEquals("https://issuerank-kappa.vercel.app/terms", Links.TERMS)
        assertEquals("https://issuerank-kappa.vercel.app/privacy#contact", Links.CONTACT)
    }

    @Test
    fun appPathEqualsIssueShortcutAndViewFromHash() {
        val cases = webCases("text.json", "shortcut")
        assertEquals(26, cases.size)
        for (c in cases) {
            val path = c.getString("path")
            val route = AppRoutes.parse(path, c.getString("today"))
            assertEquals(path, c.getString("view"), route.view.hash)
            val back = c.opt("back")
            val issue = (back as? JSONObject)?.opt("issue") as? JSONObject
            if (issue == null) assertNull(path, route.issue)
            else assertEquals(path, IssueLink(issue.getString("id"), issue.text("date")), route.issue)
        }
    }

    @Test
    fun appPathEdgeCases() {
        assertEquals(AppRoute(View.MAP, null), AppRoutes.parse(null, "2026-11-03"))
        assertEquals(AppRoute(View.MAP, null), AppRoutes.parse("", "2026-11-03"))
        assertEquals(AppRoute(View.MAP, null), AppRoutes.parse("https://issuerank-kappa.vercel.app/?issue=2026-11-03-pol-21", "2026-11-03"))
        assertEquals(AppRoute(View.LIST, null), AppRoutes.parse(AppRoutes.LIST, "2026-11-03"))
        assertEquals(AppRoute(View.MAP, null), AppRoutes.parse(AppRoutes.HOME, "2026-11-03"))
        // 위젯 줄 path(서버 buildWidget)와 같은 꼴
        val widget = ApiJson.widget(TestFiles.fixture("widget-d0.json"))
        for (item in widget.items) {
            val id = requireNotNull(item.id)
            assertEquals(item.path, AppRoutes.issuePath(id))
            assertEquals(IssueLink(id, id.substring(0, 10)), AppRoutes.parse(item.path, "2026-11-03").issue)
        }
    }

    @Test
    fun shortcutOutcome() {
        val link = IssueLink("2026-11-03-pol-22", "2026-11-03")
        val loaded = RankingLoad.Loaded(seed)
        assertEquals(ShortcutOutcome.Open(seed.issueById("2026-11-03-pol-22")!!), AppRoutes.outcome(link, loaded))
        // 랭킹에 없는 id · 형식 틀림(날짜 null → 최근 랭킹) · 그 날짜 404 → 토스트, 5xx · 네트워크 → 토스트 없음(오류 토스트만, D25)
        assertEquals(ShortcutOutcome.NotFound, AppRoutes.outcome(IssueLink("2026-11-03-pol-99", "2026-11-03"), loaded))
        assertEquals(ShortcutOutcome.NotFound, AppRoutes.outcome(IssueLink("2026-11-03-pol-22", null), loaded))
        assertEquals(ShortcutOutcome.NotFound, AppRoutes.outcome(link, RankingLoad.NoData(ApiError(ErrorKind.NOT_FOUND, 404))))
        assertEquals(ShortcutOutcome.None, AppRoutes.outcome(link, RankingLoad.Failed(ApiError(ErrorKind.SERVER, 502))))
        assertEquals(ShortcutOutcome.None, AppRoutes.outcome(link, RankingLoad.Failed(ApiError(ErrorKind.NETWORK))))
        assertEquals("이슈를 찾을 수 없습니다", AppRoutes.notFoundToast.message.text(s))
        // 대체 섹션 카드도 그 날짜(D-1)의 id로 열림
        assertTrue(AppRoutes.outcome(IssueLink("2026-11-02-cul-5", "2026-11-02"), RankingLoad.Loaded(seed)) is ShortcutOutcome.Open)
    }

    @Test
    fun queryDecodingLikeUrlSearchParams() {
        assertEquals(listOf("issue" to "a b", "x" to ""), Web.queryParams("issue=a+b&x"))
        assertEquals("2026-11-03-pol-21", Web.decodeQueryComponent("2026-11-03%2Dpol%2D21"))
        assertEquals("100%", Web.decodeQueryComponent("100%"))
        assertEquals("%zz", Web.decodeQueryComponent("%zz"))
        assertEquals("ㄱ😀", Web.decodeQueryComponent("%E3%84%B1%F0%9F%98%80"))
        assertEquals("😀+", Web.decodeQueryComponent("😀%2B"))
    }
}
