package io.github.jwavascript.issuerank.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jwavascript.issuerank.DebugWidgetStates
import io.github.jwavascript.issuerank.DebugWidgetStates.State
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.ui.Contrast
import io.github.jwavascript.issuerank.widget.WidgetRender.assertInside
import io.github.jwavascript.issuerank.widget.WidgetRender.glyphBottom
import io.github.jwavascript.issuerank.widget.WidgetRender.glyphTop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

/**
 * 검증 11(위젯 미리보기 · 누르기, AC-15-4 · AC-11-1~4) · 검증 8 위젯 줄: 위젯 RemoteViews를 시험 문맥에서 apply.
 * 높이 66 · 117 · 150 · 220 · 300 × ok30 · ok3 · cache · fail × 밝게/어둡게: 보이는 줄 수 = 식 · 제목 글자만 · 한 줄 "…" · 하단 = updatedLabel ·
 * fail 문구 · 넘침 0 · 색 = 웹 토큰(4.5 이상), 글자 배율 0.85 · 1.15 · 1.3 · 2, Android 12+ 높이별 묶음을 위젯 틀(AppWidgetHostView)이 고른 줄 수.
 * 서버 0(누르기 · 데이터는 WidgetClickTest · WidgetDataTest — 가짜 서버). 지금은 컴파일만(C2 — "사용자 결정 11-02: 기기 시험은 나중에 · 에뮬레이터 불가").
 */
@RunWith(AndroidJUnit4::class)
class WidgetRemoteViewsTest {
    private val today = KstDate.today(Clock.systemUTC())
    private val heights = listOf(66, 117, 150, 220, 300)
    private val states = listOf(State.OK30, State.OK3, State.CACHE, State.FAIL)

    private fun checkDrawn(at: String, state: State, heightDp: Int, fontScale: Float, r: WidgetRender.Rendered) {
        val view = DebugWidgetStates.view(state, today)!!
        val widget = view.widget
        if (widget == null) {
            assertEquals(at, 0, r.rows.size)
            assertEquals(at, View.VISIBLE, r.fail.visibility)
            assertEquals(at, "불러오지 못했습니다", r.fail.text.toString())
            assertEquals(at, r.context.getString(R.string.widget_fail), r.fail.text.toString())
            assertEquals(at, View.GONE, r.foot.visibility)
            assertInside("$at 실패 안내", r, r.fail)
            return
        }
        val n = WidgetSizes.visibleRows(heightDp.toDouble(), fontScale.toDouble(), widget.items.size)
        assertEquals("$at 줄 수", n, r.rows.size)
        assertEquals("$at 줄 수 = 식(항목 수까지)", minOf(WidgetRules.rows(heightDp.toDouble(), fontScale.toDouble()), widget.items.size), n)
        r.rows.forEachIndexed { i, tv ->
            assertEquals("$at 줄 ${i + 1} = 제목만(숫자 · 섹션 이름 0)", widget.items[i].title, tv.text.toString())
            assertEquals("$at 줄 ${i + 1} 13sp", WidgetRender.sp(r.context, 13f), tv.textSize, 0.5f)
            assertInside("$at 줄 ${i + 1}", r, tv)
        }
        assertEquals("$at 하단 10sp", WidgetRender.sp(r.context, 10f), r.foot.textSize, 0.5f)
        // 아주 긴 제목(경제 1위 100자) = 한 줄 + 끝 "…"
        if (state != State.CACHE) assertTrue("$at 긴 제목 줄임", r.rows[0].layout.getEllipsisCount(0) > 0)
        assertEquals(at, View.GONE, r.fail.visibility)
        assertEquals(at, View.VISIBLE, r.foot.visibility)
        assertEquals("$at 하단", widget.updatedLabel, r.foot.text.toString())
        assertInside("$at 하단", r, r.foot)
        assertTrue("$at 줄 · 하단 겹침", glyphBottom(r.rows.last()) <= glyphTop(r.foot))
    }

    @Test
    fun everyHeightStateAndMode_rowsAreTheFormula_titlesOnly_footIsUpdatedLabel_noOverflow() {
        for (h in heights) for (state in states) for (dark in listOf(false, true)) {
            val r = WidgetRender.render(DebugWidgetStates.view(state, today)!!, h, dark)
            checkDrawn("h=$h $state ${if (dark) "어둡게" else "밝게"}", state, h, 1f, r)
        }
    }

    @Test
    fun fontScales_rowsFollowTheFormula_withoutOverflow() {
        // 식이 1줄을 억지로 넣는 높이(h < 24 + 42f)는 빼고(위젯이 그만큼 낮으면 하단이 잘림 — 식 그대로)
        for (f in listOf(0.85f, 1.15f, 1.3f, 2f)) for (h in heights) {
            if (h < WidgetSizes.minHeightFor(1, f.toDouble())) continue
            for (state in listOf(State.OK30, State.FAIL)) {
                val r = WidgetRender.render(DebugWidgetStates.view(state, today)!!, h, dark = false, fontScale = f)
                checkDrawn("f=$f h=$h $state", state, h, f, r)
            }
        }
    }

    @Test
    fun colorsAreTheWebTokens_inBothModes_andTextIsAtLeast45() {
        for (dark in listOf(false, true)) {
            val r = WidgetRender.render(DebugWidgetStates.view(State.OK30, today)!!, 150, dark)
            val surface = (r.root.background as GradientDrawable).color!!.defaultColor
            val text = r.rows[0].currentTextColor
            val muted = r.foot.currentTextColor
            val want = if (dark) listOf(0xFF1A1A19, 0xFFECEBE8, 0xFF9B978F) else listOf(0xFFFFFFFF, 0xFF161616, 0xFF6F6B65)
            assertEquals("밝게/어둡게=$dark 바탕 · 제목 · 하단", want.map { it.toInt() }, listOf(surface, text, muted))
            assertTrue(Contrast.ratio(text, surface) >= Contrast.TEXT_MIN)
            assertTrue(Contrast.ratio(muted, surface) >= Contrast.TEXT_MIN)
            val failText = WidgetRender.render(DebugWidgetStates.view(State.FAIL, today)!!, 150, dark).fail.currentTextColor
            assertEquals(text, failText)
        }
    }

    @Test
    fun android12_sizedViews_hostPicksTheFormulaRows_smallestIsOneRow() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val ctx = WidgetRender.context(dark = false, fontScale = 1f)
        val view = DebugWidgetStates.view(State.OK30, today)!!
        val info = AppWidgetManager.getInstance(ctx).getInstalledProvidersForPackage(ctx.packageName, null)
            .single { it.provider == ComponentName(ctx, IssueRankWidget::class.java) }
        // 크기를 모르는 곳(apply만) = 넓이가 가장 작은 칸 = 1줄
        val plain = WidgetRender.place(ctx, WidgetViews.forWidget(ctx, 0, view, 1.0, WidgetSizes.DEFAULT_HEIGHT_DP), 300)
        assertEquals(1, plain.rows.size)
        // 위젯 틀(런처가 쓰는 AppWidgetHostView): 놓인 높이로 고름 → 줄 수 = 식
        for (h in heights + listOf(87, 88, 263, 264)) {
            var rows = -1
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val host = AppWidgetHostView(ctx)
                host.setAppWidget(0, info)
                host.setPadding(0, 0, 0, 0)
                host.updateAppWidget(WidgetViews.forWidget(ctx, 0, view, 1.0, WidgetSizes.DEFAULT_HEIGHT_DP))
                WidgetRender.layout(host, ctx, WidgetRender.WIDTH_DP, h)
                rows = WidgetViews.ROW_IDS.count { host.findViewById<TextView>(it)?.visibility == View.VISIBLE }
            }
            assertEquals("위젯 틀 h=$h", WidgetSizes.visibleRows(h.toDouble(), 1.0, 30), rows)
        }
    }
}
