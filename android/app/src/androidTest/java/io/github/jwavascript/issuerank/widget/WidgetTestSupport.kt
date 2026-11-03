package io.github.jwavascript.issuerank.widget

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView
import org.junit.Assert.assertTrue
import kotlin.math.roundToInt

/**
 * 위젯 계측 시험 도우미(창 없이): 위젯과 같은 WidgetViews 함수로 만든 RemoteViews를 시험 문맥(밝게/어둡게 · 글자 배율 덮기)에서 apply →
 * 정한 크기(dp)로 재고 놓음. 글자 상자 = TextView 안 글이 실제로 차지하는 세로(가운데 맞춤 칸 안).
 */
object WidgetRender {
    const val WIDTH_DP = 276
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val target: Context get() = instrumentation.targetContext

    /** 기기 설정 위에 다크 · 글자 배율만 덮은 문맥(debug 도구 미리보기와 같은 방법) */
    fun context(dark: Boolean, fontScale: Float): Context {
        val config = Configuration(target.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        config.fontScale = fontScale
        return target.createConfigurationContext(config)
    }

    class Rendered(val context: Context, val root: View) {
        val rows: List<TextView> get() = WidgetViews.ROW_IDS.map { root.findViewById<TextView>(it) }.filter { it.visibility == View.VISIBLE }
        val fail: TextView get() = root.findViewById(R.id.widget_fail)
        val foot: TextView get() = root.findViewById(R.id.widget_foot)
    }

    /** 줄 수 = 식(rows(h, f)) — 위젯 한 벌(forRows)을 그 높이 틀에 */
    fun render(view: WidgetView, heightDp: Int, dark: Boolean = false, fontScale: Float = 1f): Rendered {
        val ctx = context(dark, fontScale)
        val rows = WidgetRules.rows(heightDp.toDouble(), fontScale.toDouble())
        return place(ctx, WidgetViews.forRows(ctx, 0, view, rows, fontScale.toDouble()), heightDp)
    }

    fun place(ctx: Context, rv: RemoteViews, heightDp: Int, widthDp: Int = WIDTH_DP): Rendered {
        lateinit var out: Rendered
        instrumentation.runOnMainSync {
            val parent = FrameLayout(ctx)
            val root = rv.apply(ctx, parent)
            parent.addView(root)
            layout(parent, ctx, widthDp, heightDp)
            out = Rendered(ctx, root)
        }
        return out
    }

    /** dp 크기로 재고 놓음(메인 스레드에서) */
    fun layout(view: View, ctx: Context, widthDp: Int, heightDp: Int) {
        val d = ctx.resources.displayMetrics.density
        val w = (widthDp * d).roundToInt()
        val h = (heightDp * d).roundToInt()
        view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
    }

    /** sp → px(그 문맥의 글자 배율 — Android 14+ 비선형 확대 포함) */
    fun sp(ctx: Context, value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, ctx.resources.displayMetrics)

    fun glyphTop(tv: TextView): Int = tv.top + tv.totalPaddingTop
    fun glyphBottom(tv: TextView): Int = glyphTop(tv) + tv.layout.height

    /** 넘침 0: 글이 한 줄 · 칸 안(잘리지 않음) · 위젯 안쪽 여백 안(위아래 · 오른쪽) */
    fun assertInside(at: String, r: Rendered, tv: TextView) {
        val root = r.root
        assertTrue("$at 한 줄", tv.layout.lineCount == 1)
        assertTrue("$at 칸 안 위", tv.totalPaddingTop >= 0)
        assertTrue("$at 칸 안 아래 ${tv.totalPaddingTop + tv.layout.height} > ${tv.height}", tv.totalPaddingTop + tv.layout.height <= tv.height)
        assertTrue("$at 위 여백 ${glyphTop(tv)} < ${root.paddingTop}", glyphTop(tv) >= root.paddingTop)
        assertTrue("$at 아래 넘침 ${glyphBottom(tv)} > ${root.height - root.paddingBottom}", glyphBottom(tv) <= root.height - root.paddingBottom)
        assertTrue("$at 오른쪽 넘침", tv.right <= root.width - root.paddingRight)
    }

    /** 앞에 있는 MainActivity들(메인 스레드에서 읽음) */
    fun resumedMain(): List<MainActivity> {
        var out = emptyList<MainActivity>()
        instrumentation.runOnMainSync {
            out = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>()
        }
        return out
    }

    /** 앞에 있는 MainActivity의 앱 경로(EXTRA_PATH — 새로 열렸거나 onNewIntent로 받은 것)가 path가 될 때까지 */
    fun waitForMainPath(path: String, timeoutMs: Long = 15_000) {
        val until = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < until) {
            if (resumedMain().any { it.intent?.getStringExtra(MainActivity.EXTRA_PATH) == path }) return
            SystemClock.sleep(100)
        }
        throw AssertionError("MainActivity가 경로 $path 로 열리지 않음(지금 ${resumedMain().map { it.intent?.getStringExtra(MainActivity.EXTRA_PATH) }})")
    }

    fun finishMain() {
        val open = resumedMain() // runOnMainSync 안에서 다시 부르면 안 됨(메인 스레드에서 막힘)
        instrumentation.runOnMainSync { open.forEach { it.finish() } }
    }
}
