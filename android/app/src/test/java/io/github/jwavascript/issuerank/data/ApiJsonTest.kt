package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.TestFiles.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** 응답 해석: 실제 응답 꼴 fixture(openapi 예시 · 가짜 서버 씨앗 D0 꼴 · Better Auth 응답) */
class ApiJsonTest {
    @Test
    fun rankingOpenapiExample() {
        val r = ApiJson.ranking(fixture("rankings-openapi-example.json"))
        assertEquals("2026-10-07", r.date)
        assertEquals("2026-10-07T06:00:00+09:00", r.updatedAt)
        assertEquals(SectionKey.entries, r.sections.map { it.key })
        assertEquals(listOf("정치", "경제", "사회", "생활·문화", "세계", "IT·과학"), r.sections.map { it.name })
        val soc = r.issuesOf(SectionKey.SOC).single()
        assertEquals(Issue("2026-10-07-soc-6", "2026-10-07", SectionKey.SOC, 1, "저출생 대책, 육아휴직 급여 상한 인상", null, 97, "https://news.naver.com/"), soc)
        assertEquals(3, r.issuesOf(SectionKey.POL).single().summary!!.size)
        assertEquals(6, r.issueCount)
        assertEquals(184, r.maxArticleCount)
    }

    @Test
    fun rankingSeedD0_orderTiesNullSummaryLongTitleFallbackSection() {
        val r = ApiJson.ranking(fixture("rankings-d0.json"))
        assertEquals(30, r.issueCount)
        // D16 동점: 받은 순서 그대로(앱은 다시 정렬 0)
        assertEquals(listOf("pol-21", "pol-22", "pol-9", "pol-10", "pol-3"), r.issuesOf(SectionKey.POL).map { it.id.substring(11) })
        assertEquals(listOf(1, 2, 3, 4, 5), r.issuesOf(SectionKey.POL).map { it.rank })
        assertNull(r.issueById("2026-11-03-pol-22")!!.summary)
        assertNull(r.issueById("2026-11-03-soc-6")!!.summary)
        assertEquals(100, r.issueById("2026-11-03-eco-20")!!.title.length)
        // 대체 섹션(D8): 생활·문화 = 어제 카드
        val cul = r.issuesOf(SectionKey.CUL)
        assertTrue(cul.all { it.date == "2026-11-02" && r.isFallback(it) })
        assertFalse(r.isFallback(r.issuesOf(SectionKey.POL)[0]))
        assertEquals(199, r.maxArticleCount)
        assertEquals(544, r.sectionTotal(SectionKey.POL))
        assertNull(r.issueById("2026-11-03-none-1"))
    }

    @Test
    fun rankingSkipsUnknownSectionKeysLikeTheWeb() {
        val r = ApiJson.ranking("""{"date":"2026-11-03","updatedAt":"2026-11-03T04:31:27+09:00","sections":[{"key":"new","name":"새","issues":[]},{"key":"pol","name":"정치","issues":[]}]}""")
        assertEquals(listOf(SectionKey.POL), r.sections.map { it.key })
    }

    @Test
    fun rankingRejectsShapesOutsideTheContract() {
        val ok = """{"id":"2026-11-03-pol-1","date":"2026-11-03","section":"pol","rank":1,"title":"t","summary":null,"articleCount":3,"url":"u"}"""
        fun ranking(issue: String, date: String = "2026-11-03", updatedAt: String = "2026-11-03T04:31:27+09:00") =
            """{"date":"$date","updatedAt":"$updatedAt","sections":[{"key":"pol","name":"정치","issues":[$issue]}]}"""
        ApiJson.ranking(ranking(ok)) // 기준은 통과
        val bad = listOf(
            ranking(ok.replace("\"title\":\"t\"", "\"title\":null")),          // Android optString이면 "null" 글자 제목이 됐을 곳
            ranking(ok.replace("\"section\":\"pol\"", "\"section\":\"xx\"")),
            ranking(ok.replace("\"date\":\"2026-11-03\"", "\"date\":\"2026-02-30\"")),
            ranking(ok.replace("\"articleCount\":3", "\"articleCount\":-1")),
            ranking(ok.replace("\"articleCount\":3", "\"articleCount\":\"3\"")),
            ranking(ok.replace("\"rank\":1", "\"rank\":1.5")),
            ranking(ok.replace("\"summary\":null", "\"summary\":\"한 줄\"")),
            ranking(ok.replace("\"summary\":null", "\"summary\":[1,2,3]")),
            ranking(ok, date = "2026-1-3"),
            ranking(ok, updatedAt = "어제"),
            """{"date":"2026-11-03","updatedAt":"2026-11-03T04:31:27+09:00"}""",
            "[]", "null", "", "<html>",
        )
        for (text in bad) assertThrows(text, Exception::class.java) { ApiJson.ranking(text) }
    }

    @Test
    fun widgetSeedD0() {
        val w = ApiJson.widget(fixture("widget-d0.json"))
        assertEquals("2026-11-03", w.date)
        assertEquals("11.3 오전 4:31 수집", w.updatedLabel)
        assertEquals("/#list", w.morePath)
        assertEquals(30, w.items.size)
        assertEquals(
            listOf("2026-11-03-eco-20", "2026-11-03-soc-2", "2026-11-03-wor-2", "2026-11-03-it-3", "2026-11-03-pol-21", "2026-11-02-cul-5"),
            w.items.take(6).map { it.id },
        )
        assertEquals("/?issue=2026-11-03-eco-20", w.items[0].path)
        assertEquals(SectionKey.ECO, w.items[0].section)
    }

    @Test
    fun authResponsesGiveUserOnly() {
        val user = AuthUser("test-user-1", "앱 사용자", "app.user@example.invalid")
        assertEquals(user, ApiJson.authUser(fixture("auth-sign-up.json")))
        assertEquals(user, ApiJson.authUser(fixture("auth-sign-in.json")))
        assertEquals(AuthUser("test-user-2", "구글 앱 사용자", "o8.google@example.invalid"), ApiJson.authUser(fixture("auth-social.json")))
        assertEquals(user, ApiJson.session(fixture("session.json")))
        assertNull(ApiJson.session(fixture("session-null.json")))
        assertNull(ApiJson.session("{}"))
        assertNull(ApiJson.session("""{"session":null,"user":null}"""))
        // { user }가 없는 200(웹: !data?.user → 오류) · 이름 null(Android optString이면 "null · 로그아웃")
        assertThrows(BadResponseException::class.java) { ApiJson.authUser("""{"redirect":true,"url":"https://accounts.google.com/"}""") }
        assertThrows(BadResponseException::class.java) { ApiJson.session("""{"user":{"id":"u","name":null,"email":"e"}}""") }
        assertThrows(BadResponseException::class.java) { ApiJson.session("[]") }
    }

    @Test
    fun bookmarksAndFolders() {
        val b = ApiJson.bookmarks(fixture("bookmarks.json"))
        assertEquals(listOf("2026-11-03-it-1", "2026-11-02-cul-5", "2026-11-03-pol-22"), b.map { it.id })
        assertEquals("f52bfc5e7-6572-463d-8695-ad6b48737215", b[0].folderId)
        assertEquals("2026-11-02T21:40:00+09:00", b[1].savedAt)
        assertEquals(TestFiles.millis("2026-11-02T12:40:00Z"), ApiJson.savedAtMillis(b[1].savedAt))
        assertNull(b[2].issue.summary)
        assertEquals("2026-11-02", b[1].issue.date)
        assertThrows(BadResponseException::class.java) { ApiJson.bookmarks(fixture("bookmarks.json").replace("2026-11-03T00:12:33.120Z", "어제")) }

        assertEquals(
            listOf(Folder("f1", "반도체·AI", 4), Folder("f52bfc5e7-6572-463d-8695-ad6b48737215", "부동산·금리", 2), Folder("f3", "나중에 읽기", 0)),
            ApiJson.folders(fixture("folders.json")),
        )
        assertEquals(Folder("f52bfc5e7-6572-463d-8695-ad6b48737215", "반도체·AI", 0), ApiJson.folder(fixture("folder-created.json")))
        assertEquals(emptyList<Bookmark>(), ApiJson.bookmarks("[]"))
    }

    @Test
    fun errorBodies() {
        assertEquals(null to "로그인이 필요합니다", ApiJson.errorBody("""{"message":"로그인이 필요합니다"}"""))
        assertEquals("INVALID_EMAIL_OR_PASSWORD" to "Invalid email or password", ApiJson.errorBody("""{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}"""))
        assertEquals("MISSING_OR_NULL_ORIGIN" to null, ApiJson.errorBody("""{"code":"MISSING_OR_NULL_ORIGIN"}"""))
        assertEquals(null to null, ApiJson.errorBody("Internal Server Error"))
        assertEquals(null to null, ApiJson.errorBody(""))
        assertEquals(null to null, ApiJson.errorBody("""{"message":""}"""))
        assertEquals(null to null, ApiJson.errorBody("""{"message":null,"code":7}"""))
        assertEquals(null to null, ApiJson.errorBody("""["x"]"""))
    }

    @Test
    fun isoDateIsStrictCalendarDate() {
        assertTrue(ApiJson.isIsoDate("2028-02-29"))
        for (s in listOf("2026-02-29", "2026-13-01", "2026-1-03", "26-11-03", "2026-11-03T00:00", "")) assertFalse(s, ApiJson.isIsoDate(s))
    }
}
