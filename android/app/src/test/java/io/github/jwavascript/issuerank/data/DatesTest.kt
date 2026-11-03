package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.TestFiles.webCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 날짜(KST): 웹 TODAY · fmtDate · fmtShort · fmtUpdated · addDays · renderContext · timeAgo를 vm으로 돌린 값(web/dates.json)과 같음 */
class DatesTest {
    private val s = XmlStrings

    @Test
    fun todayIsKstAndFlipsAtKstMidnight() {
        val cases = webCases("dates.json", "today")
        assertEquals(7, cases.size)
        for (c in cases) assertEquals(c.toString(), c.getString("today"), KstDate.today(TestFiles.clock(c.getString("now"))))
        // 경계: 11-02 23:59:59.999 KST = 11-02, 11-03 00:00 KST = 11-03
        assertEquals("2026-11-02", KstDate.today(TestFiles.clock("2026-11-02T14:59:59.999Z")))
        assertEquals("2026-11-03", KstDate.today(TestFiles.clock("2026-11-02T15:00:00Z")))
    }

    @Test
    fun formatsEqualWeb() {
        for (c in webCases("dates.json", "fmt")) {
            assertEquals(c.toString(), c.getString("fmtDate"), KstDate.fmtDate(c.getString("date"), s))
            assertEquals(c.toString(), c.getString("fmtShort"), KstDate.fmtShort(c.getString("date"), s))
        }
        for (c in webCases("dates.json", "updated")) assertEquals(c.toString(), c.getString("text"), KstDate.fmtUpdated(c.getString("at"), s))
        for (c in webCases("dates.json", "addDays")) {
            assertEquals(c.toString(), c.getString("out"), KstDate.addDays(c.getString("date"), c.getLong("n")))
        }
        assertEquals("11.3 오전 12:00", KstDate.fmtUpdated("2026-11-03T00:00:00+09:00", s))
        assertEquals("11.3 오후 12:30", KstDate.fmtUpdated("2026-11-03T12:30:00+09:00", s))
    }

    @Test
    fun dateLineEqualsRenderContext() {
        val cases = webCases("dates.json", "context")
        assertEquals(8, cases.size)
        for (c in cases) {
            val rk = c.optJSONObject("ranking")
            val ranking = rk?.let { o ->
                Ranking(o.getString("date"), o.getString("updatedAt"), listOf(RankingSection(SectionKey.POL, "정치",
                    List(o.getInt("count")) { Issue("id$it", o.getString("date"), SectionKey.POL, 1, "t", null, 1, "u") })))
            }
            val today = KstDate.today(TestFiles.clock(c.getString("now")))
            assertEquals(c.getString("today"), today)
            val line = DateLine(c.getString("date"), today, View.of(c.getString("view"))!!, ranking)
            val msg = c.toString()
            assertEquals(msg, c.getString("dateText"), line.dateText(s))
            assertEquals(msg, c.getString("title"), line.title(s))
            assertEquals(msg, c.getString("desc"), line.desc(s))
            assertEquals(msg, c.getString("updated"), line.updated(s))
            assertEquals(msg, c.getString("max"), line.maxDate)
            assertEquals(msg, c.getBoolean("nextDisabled"), !line.nextEnabled)
            assertEquals(msg, c.getBoolean("todayHidden"), !line.todayVisible)
        }
    }

    @Test
    fun pastDayTitleAndNavigation() {
        val line = DateLine("2026-11-02", "2026-11-03", View.MAP, null)
        assertEquals("11월 2일 (월)의 주요 이슈", line.title(s))
        assertEquals("2026-11-01", line.prevDate)
        assertEquals("2026-11-03", line.nextDate)
        assertTrue(line.nextEnabled)
        assertFalse(DateLine("2026-11-03", "2026-11-03", View.LIST, null).nextEnabled)
        // setDate: 미래는 오늘로
        assertEquals("2026-11-03", KstDate.clampToToday("2026-11-04", "2026-11-03"))
        assertEquals("2026-11-02", KstDate.clampToToday("2026-11-02", "2026-11-03"))
    }

    @Test
    fun collectedTextUsesDateOnlyForFallbackSection() {
        val r = ApiJson.ranking(TestFiles.fixture("rankings-d0.json"))
        assertEquals("11.3 오전 4:31", DateLine.collected(r.issueById("2026-11-03-pol-21")!!, r, s))
        assertEquals("11.2", DateLine.collected(r.issueById("2026-11-02-cul-5")!!, r, s))
    }

    @Test
    fun timeAgoEqualsWeb() {
        val cases = webCases("dates.json", "timeAgo")
        assertEquals(12, cases.size)
        for (c in cases) assertEquals(c.toString(), c.getString("text"), KstDate.timeAgo(c.getString("savedAt"), TestFiles.millis(c.getString("now")), s))
        assertEquals("1분 전 저장", s[Str.SAVED_AGO, KstDate.timeAgo("2026-11-03T02:59:30.000Z", TestFiles.millis("2026-11-03T03:00:00Z"), s)])
    }
}
