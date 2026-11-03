package io.github.jwavascript.issuerank.widget

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView

/**
 * 위젯 그림(RemoteViews, layout/widget.xml) — 위젯(IssueRankWidget) · debug 미리보기 · 계측 시험이 이 함수를 그대로 씀.
 * 받은 목록: 제목 줄(위에서부터 보일 줄만, 글 = title) · 하단 = updatedLabel · 줄 누르기 = 그 줄 path(`/?issue=<id>`) · 줄 밖 · 하단 = morePath(`/#list`).
 * 실패(받지 못했고 캐시도 없음): "불러오지 못했습니다"(레이아웃 글 — strings_web) · 누르기 = 첫 화면(FAIL_PATH). 경로 → 화면은 MainActivity(A3).
 * 색은 레이아웃의 @color(웹 토큰)뿐 — 코드는 색을 정하지 않음(런처가 기기 다크를 따라 values-night을 풂).
 */
object WidgetViews {
    /** 제목 줄 id(위에서부터 10개 = WidgetRules.MAX_ROWS) */
    val ROW_IDS = intArrayOf(
        R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3, R.id.widget_row_4, R.id.widget_row_5,
        R.id.widget_row_6, R.id.widget_row_7, R.id.widget_row_8, R.id.widget_row_9, R.id.widget_row_10,
    )

    /**
     * 위젯에 보낼 것. Android 12+ = 줄 수가 바뀌는 높이마다 한 벌(WidgetSizes.buckets — 런처가 지금 높이로 고름),
     * 그 아래 판 = 높이 옵션 [minHeightDp]의 한 벌(작은 쪽 — 가로 · 세로 어느 쪽에서도 넘치지 않게). 실패 안내는 크기와 무관한 한 벌.
     */
    fun forWidget(context: Context, appWidgetId: Int, view: WidgetView, fontScale: Double, minHeightDp: Int): RemoteViews {
        val widget = view.widget ?: return forRows(context, appWidgetId, view, 1, fontScale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RemoteViews(
                WidgetSizes.buckets(fontScale, widget.items.size).associate {
                    SizeF(WidgetSizes.BUCKET_WIDTH_DP, it.minHeightDp.toFloat()) to forRows(context, appWidgetId, view, it.rows, fontScale)
                },
            )
        }
        return forRows(context, appWidgetId, view, WidgetRules.rows(minHeightDp.toDouble(), fontScale), fontScale)
    }

    /**
     * 줄 수 [rows](식 — 항목이 적으면 그 수까지)인 한 벌. 줄은 레이아웃에서 모두 숨김 → 보일 줄만 글 · 누르기 · 보임(WidgetTargets대로).
     * 줄 밖(뿌리) · 하단 = morePath, 실패면 뿌리 = 첫 화면.
     */
    fun forRows(context: Context, appWidgetId: Int, view: WidgetView, rows: Int, fontScale: Double): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) fixSlots(rv, fontScale)
        val widget = view.widget
        if (widget == null) {
            rv.setViewVisibility(R.id.widget_fail, View.VISIBLE)
            rv.setViewVisibility(R.id.widget_foot, View.GONE)
        } else {
            rv.setTextViewText(R.id.widget_foot, widget.updatedLabel)
        }
        for (target in WidgetTargets.of(view, rows)) {
            val open = open(context, appWidgetId, target)
            when (target.slot) {
                WidgetTargets.MORE -> {
                    rv.setOnClickPendingIntent(android.R.id.background, open)
                    rv.setOnClickPendingIntent(R.id.widget_foot, open)
                }
                WidgetTargets.FAIL -> rv.setOnClickPendingIntent(android.R.id.background, open)
                else -> {
                    val id = ROW_IDS[target.slot]
                    rv.setTextViewText(id, target.title)
                    rv.setViewVisibility(id, View.VISIBLE)
                    rv.setOnClickPendingIntent(id, open)
                }
            }
        }
        return rv
    }

    /** 앱 경로로 MainActivity(A3 routeIntent — 떠 있으면 onNewIntent) · requestCode = 위젯 id × 12 + 칸 */
    fun open(context: Context, appWidgetId: Int, target: WidgetTarget): PendingIntent = PendingIntent.getActivity(
        context, WidgetTargets.requestCode(appWidgetId, target.slot), MainActivity.routeIntent(context, target.path),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Android 12+: 칸 = 식 그대로(줄 22f · 하단 20f dp). 레이아웃의 sp 칸은 Android 14+ 글자 비선형 확대에서 식보다 작아짐(글자는 덜 작아짐) */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun fixSlots(rv: RemoteViews, fontScale: Double) {
        val row = (WidgetSizes.ROW_SP * fontScale).toFloat()
        for (id in ROW_IDS + R.id.widget_fail) rv.setViewLayoutHeight(id, row, TypedValue.COMPLEX_UNIT_DIP)
        rv.setViewLayoutHeight(R.id.widget_foot, (WidgetSizes.FOOT_SP * fontScale).toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }
}
