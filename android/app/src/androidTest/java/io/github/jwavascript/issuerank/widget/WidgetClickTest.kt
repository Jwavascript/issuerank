package io.github.jwavascript.issuerank.widget

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jwavascript.issuerank.DebugWidgetStates
import io.github.jwavascript.issuerank.DebugWidgetStates.State
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.ui.FakeServerRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 11 누르기(AC-15-4): 위젯 RemoteViews의 줄 · 줄 밖 · 하단 · 실패 안내를 실제로 누르면(RemoteViews가 단 PendingIntent를 보냄)
 * MainActivity가 그 앱 경로로 열림 — 줄 = `/?issue=<그 줄 id>` · 줄 밖 · 하단 = `/#list` · 실패 = `/`. 경로 → 상세 화면은 A3 AppRouteTest.
 * MainActivity가 랭킹을 받으므로 가짜 서버 필요(FakeServerRule). 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class WidgetClickTest {
    @get:Rule
    val server = FakeServerRule()

    private fun press(v: View, path: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { v.performClick() }
        WidgetRender.waitForMainPath(path)
    }

    @Test
    fun eachRowOpensItsIssue_outsideAndFootOpenTheList_failOpensHome() {
        val view = DebugWidgetStates.view(State.OK30, server.today)!!
        val items = view.widget!!.items
        val r = WidgetRender.render(view, 300) // 10줄
        assertEquals(10, r.rows.size)
        try {
            // 이웃한 두 번이 같은 경로가 되지 않게 섞음(줄 1, 줄 밖, 줄 2, 하단, 줄 3 … 줄 10) — 떠 있으면 onNewIntent로 경로가 바뀜
            r.rows.forEachIndexed { i, tv ->
                press(tv, items[i].path)
                if (i == 0) press(r.root, "/#list")
                if (i == 1) press(r.foot, "/#list")
            }
            press(WidgetRender.render(DebugWidgetStates.view(State.FAIL, server.today)!!, 117).root, WidgetRules.FAIL_PATH)
        } finally {
            WidgetRender.finishMain()
        }
    }

    @Test
    fun cacheRowsOpenYesterdaysIssues() {
        val view = DebugWidgetStates.view(State.CACHE, server.today)!!
        val second = view.widget!!.items[1]
        val r = WidgetRender.render(view, 117)
        try {
            press(r.rows[1], second.path)
            assertEquals("/?issue=${server.day(-1)}-soc-3", second.path)
        } finally {
            WidgetRender.finishMain()
        }
    }
}
