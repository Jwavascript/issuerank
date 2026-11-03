package io.github.jwavascript.issuerank

import io.github.jwavascript.issuerank.DebugWidgetStates.State
import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.TestFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * debug 미리보기 고정 상태(검증 11): 줄 = 가짜 서버 씨앗 그대로(fixture widget-d0 · d1 — a5-fake-server-check가 실제 서버와 대조) ·
 * 날짜는 보는 날 기준(오늘 · 어제) · cache = resolve의 캐시 길 · fail = 받기 실패 + 캐시 없음 · live = 서버에서(null).
 */
class DebugWidgetStatesTest {
    private val d0 = "2026-11-03" // fixture 날짜

    @Test
    fun ok30_isTheFakeServerWidgetOfToday() {
        val v = DebugWidgetStates.view(State.OK30, d0)!!
        assertFalse(v.fromCache)
        assertEquals(ApiJson.widget(TestFiles.fixture("widget-d0.json")), v.widget)
    }

    @Test
    fun ok3_isTheFirstThreeLines() {
        val v = DebugWidgetStates.view(State.OK3, d0)!!
        val all = ApiJson.widget(TestFiles.fixture("widget-d0.json"))
        assertEquals(all.copy(items = all.items.take(3)), v.widget)
    }

    @Test
    fun cache_isYesterdaysWidgetThroughTheCachePath() {
        val v = DebugWidgetStates.view(State.CACHE, d0)!!
        assertTrue(v.fromCache)
        assertEquals(ApiJson.widget(TestFiles.fixture("widget-d1.json")), v.widget)
        assertEquals("11.2 오전 4:28 수집", v.widget!!.updatedLabel)
    }

    @Test
    fun fail_hasNoWidget_live_comesFromTheServer() {
        val fail = DebugWidgetStates.view(State.FAIL, d0)!!
        assertTrue(fail.failed)
        assertFalse(fail.fromCache)
        assertNull(DebugWidgetStates.view(State.LIVE, d0))
    }

    @Test
    fun datesFollowTheViewingDay() {
        val w = DebugWidgetStates.view(State.OK30, "2026-12-01")!!.widget!!
        assertEquals("2026-12-01", w.date)
        assertEquals("12.1 오전 4:31 수집", w.updatedLabel)
        assertEquals("2026-12-01-eco-20", w.items[0].id)
        assertEquals("/?issue=2026-12-01-eco-20", w.items[0].path)
        assertEquals("2026-11-30-cul-5", w.items[5].id) // 생활·문화 = 대체 섹션(어제 줄)
        assertEquals("/#list", w.morePath)
        val c = DebugWidgetStates.view(State.CACHE, "2026-12-01")!!.widget!!
        assertEquals("2026-11-30", c.date)
        assertTrue(c.items.all { it.id?.startsWith("2026-11-30-") == true })
    }
}
