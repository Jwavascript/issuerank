package io.github.jwavascript.issuerank

import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.DebugWidgetStates.State
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.ui.FakeServer
import io.github.jwavascript.issuerank.ui.FakeServerRule
import io.github.jwavascript.issuerank.ui.LOAD_TIMEOUT_MS
import io.github.jwavascript.issuerank.ui.TestTags
import io.github.jwavascript.issuerank.ui.waitForTag
import io.github.jwavascript.issuerank.widget.WidgetRender
import io.github.jwavascript.issuerank.widget.WidgetStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * debug 도구(검증 11 · 12 — adb `am start` Extra와 같은 Intent): Extra → 미리보기(위젯과 같은 RemoteViews · 높이 · 다크 · 글자 배율) ·
 * 허용 목록 밖 = 무시하고 화면에 "무시함" · click 2 = 그 줄 PendingIntent를 실제로 보내 앱 상세 · server = fake · prod · default만 ·
 * live = 지금 서버에서 한 번(위젯 저장은 안 건드림) → down이면 실패 안내. 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class DebugToolsTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val context get() = server.context

    private fun intent(vararg extras: Pair<String, Any>): Intent = Intent(context, DebugToolsActivity::class.java).apply {
        for ((k, v) in extras) {
            when (v) {
                is String -> putExtra(k, v)
                is Int -> putExtra(k, v)
                is Boolean -> putExtra(k, v)
                is Float -> putExtra(k, v)
                else -> throw IllegalArgumentException("시험 Extra 형: $k")
            }
        }
    }

    private fun launch(vararg extras: Pair<String, Any>): ActivityScenario<DebugToolsActivity> = ActivityScenario.launch(intent(*extras))

    /** 미리보기 틀 안의 위젯(그려지고 [ready]가 맞을 때까지) */
    private fun preview(s: ActivityScenario<DebugToolsActivity>, ready: (WidgetRender.Rendered) -> Boolean = { true }): WidgetRender.Rendered {
        val until = SystemClock.uptimeMillis() + LOAD_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < until) {
            var out: WidgetRender.Rendered? = null
            s.onActivity { act ->
                val root = act.findViewById<FrameLayout>(R.id.debug_widget_frame)?.getChildAt(0)
                if (root != null && root.isLaidOut) WidgetRender.Rendered(root.context, root).takeIf(ready)?.let { out = it }
            }
            out?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError("미리보기가 그려지지 않음")
    }

    @Test
    fun extras_drawTheWidgetRemoteViews_atThatHeight_inDark_withThatFontScale() {
        launch("state" to "ok30", "height" to 150, "dark" to true, "scale" to 1.3f).use { s ->
            val r = preview(s)
            val items = DebugWidgetStates.view(State.OK30, server.today)!!.widget!!.items
            val n = WidgetRules.rows(150.0, 1.3f.toDouble())
            assertEquals(3, n)
            assertEquals(items.take(n).map { it.title }, r.rows.map { it.text.toString() })
            assertEquals(0xFFECEBE8.toInt(), r.rows[0].currentTextColor) // 어둡게 text 토큰
            assertEquals(0xFF9B978F.toInt(), r.foot.currentTextColor) // 어둡게 muted
            val density = context.resources.displayMetrics.density
            assertTrue("틀 높이 ${r.root.height}", abs(r.root.height - (150 * density).roundToInt()) <= 1)
            assertTrue("틀 너비 ${r.root.width}", abs(r.root.width - (WidgetRender.WIDTH_DP * density).roundToInt()) <= 1)
            compose.onNodeWithTag(DebugTags.ROWS).assertTextContains("줄 3개", substring = true)
            compose.onAllNodesWithTag(DebugTags.IGNORED).assertCountEquals(0)
        }
    }

    @Test
    fun extrasOutsideTheAllowList_areIgnoredAndShown_andChangeNothing() {
        val bad = arrayOf<Pair<String, Any>>(
            "height" to 700, "scale" to 3f, "click" to 11, "server" to "http://10.0.2.2:3100", "state" to "weird", "foo" to "bar", "dark" to "yes", "scale2" to 1,
        )
        launch(*bad).use { s ->
            val r = preview(s)
            // 기본값으로 그림(ok30 · 117 · 기기 글자 배율)
            val deviceScale = context.resources.configuration.fontScale.toDouble()
            assertEquals(WidgetRules.rows(117.0, deviceScale), r.rows.size)
            for (key in listOf("click", "dark", "foo", "height", "scale", "scale2", "server", "state")) {
                compose.onNodeWithTag(DebugTags.IGNORED).assertTextContains(key, substring = true)
            }
            assertNull(DebugServer.read(context)) // 자유 주소는 Extra로 안 받음
            assertEquals(emptyList<MainActivity>(), WidgetRender.resumedMain()) // click 무시 → 앱 안 열림
        }
    }

    @Test
    fun click2_sendsTheSecondRowsPendingIntent_andTheAppOpensThatDetail() {
        val items = DebugWidgetStates.view(State.OK30, server.today)!!.widget!!.items
        launch("state" to "ok30", "height" to 117, "click" to 2).use {
            try {
                WidgetRender.waitForMainPath(items[1].path)
                compose.waitForTag(TestTags.DETAIL)
                compose.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals(items[1].title)
            } finally {
                WidgetRender.finishMain()
            }
        }
    }

    @Test
    fun clickBeyondTheVisibleRows_isIgnored() {
        launch("state" to "ok30", "height" to 66, "scale" to 1f, "click" to 2).use { s ->
            assertEquals(1, preview(s).rows.size)
            compose.waitUntil(LOAD_TIMEOUT_MS) { compose.onAllNodesWithTag(DebugTags.IGNORED).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag(DebugTags.IGNORED).assertTextContains("click", substring = true)
            assertEquals(emptyList<MainActivity>(), WidgetRender.resumedMain())
        }
    }

    @Test
    fun serverExtra_onlyTheThreeChoices_writeTheDebugServer() {
        try {
            launch("server" to "prod", "state" to "ok3").use {
                assertEquals("https://issuerank-kappa.vercel.app", DebugServer.read(context))
                assertEquals("https://issuerank-kappa.vercel.app", AppGraph.get(context).serverUrl())
                compose.onNodeWithTag(DebugTags.SERVER_NOW).assertTextContains("https://issuerank-kappa.vercel.app", substring = true)
            }
            launch("server" to "default").use {
                assertNull(DebugServer.read(context))
                assertEquals(BuildConfig.SERVER_URL, AppGraph.get(context).serverUrl())
            }
            launch("server" to "fake").use { assertEquals("http://127.0.0.1:3100", DebugServer.read(context)) }
        } finally {
            DebugServers.save(context, null) // 운영 주소를 남기지 않음(다음 시험 · 앱이 운영에 닿지 않게)
        }
    }

    @Test
    fun live_fetchesOnceFromTheServer_widgetStoreUntouched_thenDownShowsTheFailNotice() {
        val store = WidgetStore(context)
        store.clear()
        launch("state" to "live", "height" to 300, "scale" to 1f).use { s ->
            assertEquals(10, preview(s) { it.rows.size == 10 }.rows.size)
            compose.onNodeWithTag(DebugTags.LIVE).assertTextContains("30", substring = true)
            assertEquals(1, FakeServer.requests().count { it.getString("path") == "/api/widget" })
            assertNull(store.cache.read()) // 미리보기는 위젯 캐시 · 다음 시각을 쓰지 않음
            assertNull(store.schedule.nextAt())
            FakeServer.down()
            compose.onNodeWithText(context.getString(R.string.debug_live_again)).performClick()
            val failed = preview(s) { it.fail.visibility == View.VISIBLE }
            assertEquals("불러오지 못했습니다", failed.fail.text.toString())
            compose.onNodeWithTag(DebugTags.LIVE).assertTextContains("NETWORK", substring = true)
        }
    }
}
