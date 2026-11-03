package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.FakeTransport.Companion.json
import io.github.jwavascript.issuerank.data.TestFiles.webCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** 위젯 규칙(계획 4절 · AC-15-4 · AC-11-1~4): 줄 수 공식 · widget.js valid · refreshAfter(web/widget.json) · morePath · 성공만 캐시 · 요청 꼴 */
class WidgetRulesTest {
    private val base = "http://127.0.0.1:3100"
    private val cookies = SessionCookies(MemorySessionStorage())
    private fun api(t: HttpTransport) = IssueRankApi({ base }, "1.0.0-test", t, cookies, Dispatchers.Unconfined)

    @Test
    fun rowsFormula() {
        val f1 = mapOf(117 to 3, 150 to 4, 260 to 9, 290 to 10, 66 to 1, 40 to 1, 0 to 1, 88 to 2, 1000 to 10, 220 to 8, 300 to 10)
        for ((h, rows) in f1) assertEquals("h=$h", rows, WidgetRules.rows(h.toDouble(), 1.0))
        // 글자 1.3배: ⌊(h − 24 − 26) ÷ 28.6⌋
        assertEquals(3, WidgetRules.rows(150.0, 1.3))
        assertEquals(8, WidgetRules.rows(290.0, 1.3))
        assertEquals(2, WidgetRules.rows(117.0, 1.3))
    }

    @Test
    fun validEqualsWidgetJs() {
        val cases = webCases("widget.json", "valid")
        assertEquals(26, cases.size)
        for (c in cases) {
            val text = when (val v = c.get("data")) { JSONObject.NULL -> "null"; is String -> JSONObject.quote(v); else -> v.toString() }
            val parsed = runCatching { ApiJson.widget(text) }
            assertEquals("${c.getString("name")}: $text", c.getBoolean("valid"), parsed.isSuccess)
        }
    }

    @Test
    fun morePathAndItemCap() {
        fun widget(name: String) = ApiJson.widget(webCases("widget.json", "valid").first { it.getString("name") == name }.get("data").toString())
        assertEquals("/#list", WidgetRules.morePath(widget("ok")))
        assertEquals("/", WidgetRules.morePath(widget("ok-bad-morePath")))
        assertEquals("/", WidgetRules.morePath(widget("ok-morePath-number")))
        assertEquals("/", WidgetRules.morePath(widget("ok-no-morePath")))
        assertEquals(30, widget("ok-31-items").items.size) // 계획 "30개까지" — 거절하지 않고 앞 30개
        assertEquals("/", WidgetRules.FAIL_PATH)
    }

    @Test
    fun refreshAfterEqualsWidgetJs() {
        val cases = webCases("widget.json", "refreshAfter")
        assertEquals(11, cases.size)
        for (c in cases) {
            val at = WidgetRules.refreshAfter(c.text("date"), TestFiles.millis(c.getString("now")))
            assertEquals(c.toString(), c.getString("at"), Instant.ofEpochMilli(at).toString().let { if (it.length == 20) it.replace("Z", ".000Z") else it })
        }
        val now = TestFiles.millis("2026-11-02T19:00:00Z")
        assertFalse(WidgetRules.dueForRefresh(now, WidgetRules.refreshAfter("2026-11-02", now)))
        assertTrue(WidgetRules.dueForRefresh(now + 30 * 60_000, WidgetRules.refreshAfter("2026-11-02", now)))
        assertTrue(WidgetRules.dueForRefresh(now, null))
    }

    @Test
    fun onlySuccessIsCached() {
        val json = TestFiles.fixture("widget-d0.json")
        val payload = WidgetPayload(ApiJson.widget(json), json)
        val fresh = WidgetRules.resolve(ApiResult.Ok(payload), cachedJson = "{}")
        assertEquals(json, fresh.writeCache)
        assertFalse(fresh.view.fromCache)
        assertEquals(30, fresh.view.widget!!.items.size)

        val fromCache = WidgetRules.resolve(ApiResult.Fail(ApiError(ErrorKind.NETWORK)), cachedJson = json)
        assertNull(fromCache.writeCache)
        assertTrue(fromCache.view.fromCache)
        assertEquals("11.3 오전 4:31 수집", fromCache.view.widget!!.updatedLabel)

        for (broken in listOf(null, "", "{}", """{"date":"x","updatedLabel":"y","items":[]}""", "<html>")) {
            val r = WidgetRules.resolve(ApiResult.Fail(ApiError(ErrorKind.SERVER, 500)), broken)
            assertTrue("$broken", r.view.failed)
            assertNull(r.writeCache)
        }
        assertTrue(WidgetRules.resolve(null, json).view.fromCache) // 아직 갱신할 때가 아님 → 캐시
    }

    @Test
    fun widgetRequestHasNoCookieShortReadTimeoutAnd64KbLimit() = runTest {
        cookies.onResponse(base, listOf("better-auth.session_token=test-only-session; Max-Age=604800; Path=/"))
        val json = TestFiles.fixture("widget-d0.json")
        val t = FakeTransport { json(200, json, "Set-Cookie" to "better-auth.session_token=; Max-Age=0; Path=/") }
        val r = api(t).widget()
        assertTrue(r is ApiResult.Ok && r.value.json == json)
        val req = t.last
        assertEquals("$base/api/widget", req.url)
        assertNull(req.header("Cookie"))
        assertNull(req.header("Origin"))
        assertEquals(5_000, req.connectTimeoutMs)
        assertEquals(4_000, req.readTimeoutMs)
        assertEquals(65_536, req.maxResponseBytes)
        assertTrue(cookies.hasSession(base)) // 위젯 응답의 Set-Cookie는 보지 않음

        // 64KB 경계: 65,536바이트는 받음, 65,537바이트는 실패(캐시로)
        fun padded(size: Int): String {
            val head = json.trimEnd().removeSuffix("}") + ",\"pad\":\""
            return head + "x".repeat(size - head.toByteArray().size - 2) + "\"}"
        }
        assertEquals(65_536, padded(65_536).toByteArray().size)
        assertTrue(api(FakeTransport { json(200, padded(65_536)) }).widget() is ApiResult.Ok)
        assertEquals(ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE, 200)), api(FakeTransport { json(200, padded(65_537)) }).widget())
        // 200만 성공(widget.js) · 404 · 형식 틀림
        assertEquals(ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE, 203)), api(FakeTransport { json(203, json) }).widget())
        assertEquals(ErrorKind.NOT_FOUND, (api(FakeTransport { json(404, """{"message":"해당 날짜의 랭킹 데이터가 없습니다"}""") }).widget() as ApiResult.Fail).error.kind)
        assertEquals(ErrorKind.BAD_RESPONSE, (api(FakeTransport { json(200, JSONObject(json).put("items", listOf<Any>()).toString()) }).widget() as ApiResult.Fail).error.kind)
    }
}
