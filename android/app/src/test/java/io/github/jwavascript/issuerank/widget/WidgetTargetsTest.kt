package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.AppRoutes
import io.github.jwavascript.issuerank.data.TestFiles
import io.github.jwavascript.issuerank.data.View
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 위젯 누르기 대상(계획 4절 · AC-15-4): 줄 → 그 줄 path(`/?issue=<id>`) · 줄 밖 · 하단 → morePath(`/#list`, 틀리면 `/`) · 실패 안내 → `/`,
 * requestCode = 위젯 id × 12 + 칸(위젯 · 줄마다 다름), 경로는 앱 경로 해석(A3 AppRoutes.parse)이 그대로 읽는 꼴. WidgetViews.forRows가 이것대로 그림.
 */
class WidgetTargetsTest {
    private val today = ApiJson.widget(TestFiles.fixture("widget-d0.json")) // 30줄 · morePath /#list

    @Test
    fun eachVisibleRowGetsItsOwnTitleAndPath_outsideAndFootGetMorePath() {
        val targets = WidgetTargets.of(WidgetView(today, fromCache = false), 3)
        assertEquals(listOf(0, 1, 2, WidgetTargets.MORE), targets.map { it.slot })
        for (i in 0..2) {
            assertEquals(today.items[i].path, targets[i].path)
            assertEquals(today.items[i].title, targets[i].title)
        }
        assertEquals("/?issue=2026-11-03-eco-20", targets[0].path)
        assertEquals(WidgetTarget(WidgetTargets.MORE, "/#list"), targets.last())
        // 캐시로 그린 위젯도 같음(widget.js — 캐시 표시가 따로 없음)
        assertEquals(targets, WidgetTargets.of(WidgetView(today, fromCache = true), 3))
    }

    @Test
    fun rowsStopAtTheFormulaTheItemsAndTen() {
        assertEquals(10, WidgetTargets.of(WidgetView(today, false), 12).count { it.slot < WidgetTargets.MORE })
        assertEquals(1, WidgetTargets.of(WidgetView(today, false), 1).count { it.slot < WidgetTargets.MORE })
        val three = today.copy(items = today.items.take(3))
        assertEquals(listOf(0, 1, 2, WidgetTargets.MORE), WidgetTargets.of(WidgetView(three, false), 10).map { it.slot })
    }

    @Test
    fun badOrMissingMorePath_isHome_failNoticeIsHomeOnly() {
        assertEquals("/", WidgetTargets.of(WidgetView(today.copy(morePath = "list"), false), 3).last().path)
        assertEquals("/", WidgetTargets.of(WidgetView(today.copy(morePath = null), false), 3).last().path)
        assertEquals(listOf(WidgetTarget(WidgetTargets.FAIL, WidgetRules.FAIL_PATH)), WidgetTargets.of(WidgetView(null, false), 10))
        assertEquals("/", WidgetRules.FAIL_PATH)
    }

    @Test
    fun requestCodes_differPerWidgetAndSlot_previewIdZeroStaysBelowRealWidgets() {
        val codes = (0..500).flatMap { id -> (0 until WidgetTargets.SLOTS).map { WidgetTargets.requestCode(id, it) } }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue((0 until WidgetTargets.SLOTS).all { WidgetTargets.requestCode(0, it) < WidgetTargets.requestCode(1, 0) })
        assertTrue(WidgetTargets.MORE >= WidgetRules.MAX_ROWS && WidgetTargets.FAIL < WidgetTargets.SLOTS && WidgetTargets.MORE != WidgetTargets.FAIL)
    }

    @Test
    fun everyPathIsAnAppRouteTheAppReads() {
        for (t in WidgetTargets.of(WidgetView(today, false), 10)) {
            val route = AppRoutes.parse(t.path, "2026-11-03")
            if (t.slot == WidgetTargets.MORE) {
                assertEquals(View.LIST, route.view)
                assertNull(route.issue)
            } else {
                assertEquals(today.items[t.slot].id, route.issue?.id) // 줄 → 그 이슈 상세(A3)
                assertEquals(today.items[t.slot].id?.take(10), route.issue?.date)
            }
        }
        val fail = AppRoutes.parse(WidgetRules.FAIL_PATH, "2026-11-03")
        assertEquals(View.MAP, fail.view)
        assertNull(fail.issue)
    }
}
