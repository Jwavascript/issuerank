package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.WidgetRules
import kotlin.math.ceil

/** Android 12+ 높이별 묶음 한 칸: 위젯 높이가 [minHeightDp] 이상이면 [rows]줄 그림 */
data class WidgetBucket(val minHeightDp: Double, val rows: Int)

/**
 * 위젯 칸 숫자 = 줄 수 식 min(10, max(1, ⌊(h − 24 − 20f) ÷ 22f⌋))의 숫자 = res/values/widget.xml(f = 글자 배율, 단위 dp):
 * 위아래 안쪽 12dp씩(24) · 하단 칸 20f · 줄 칸 22f. 순수 계산(Android 0 — JVM 시험).
 *
 * 높이별 묶음(Android 12+ `RemoteViews(Map<SizeF, RemoteViews>)`): 줄 수가 n이 되는 가장 낮은 높이마다 한 벌(n = 1…min(10, 항목 수)).
 * 런처 규칙(RemoteViews 공식 설명): 지금 크기에 들어가는 것 중 넓이가 가장 큰 것, 들어가는 것이 없으면 넓이가 가장 작은 것.
 * 칸의 가로는 모두 [BUCKET_WIDTH_DP](어떤 폭에도 들어감) → 넓이 순서 = 높이 순서 → 고른 칸 = 지금 높이를 넘지 않는 가장 높은 칸 → 줄 수 = 식.
 * 묶음 수 ≤ 10(한도 16). 그 아래 판(Android 8~11)은 높이 옵션 하나로 줄 수를 정함(IssueRankWidget.draw).
 */
object WidgetSizes {
    const val PADDING_DP = 24
    const val FOOT_SP = 20
    const val ROW_SP = 22

    /** 높이별 묶음 칸의 가로(dp): 모두 같은 작은 양수 — 0이면 넓이가 모두 0이라 런처가 고를 칸이 정해지지 않음 */
    const val BUCKET_WIDTH_DP = 1f

    /** widget_info minHeight(4×2) — 높이 옵션이 아직 없을 때 */
    const val DEFAULT_HEIGHT_DP = 117

    /** RemoteViews(Map) 한 번에 담을 수 있는 수(AOSP) */
    const val MAX_SIZED = 16

    /** n줄이 보이는 가장 낮은 높이(dp, 0.01 칸): 식의 경계를 올림한 뒤 rows(h, f) ≥ n이 될 때까지 0.01씩 */
    fun minHeightFor(rows: Int, fontScale: Double): Double {
        var cents = ceil((PADDING_DP + (FOOT_SP + ROW_SP * rows) * fontScale) * 100 - 1e-6).toLong()
        while (WidgetRules.rows(cents / 100.0, fontScale) < rows) cents++
        return cents / 100.0
    }

    /** 줄 수가 바뀌는 높이마다 한 벌. 항목이 10개보다 적으면 그 수까지(그 위 높이는 같은 그림) */
    fun buckets(fontScale: Double, items: Int): List<WidgetBucket> =
        (1..minOf(WidgetRules.MAX_ROWS, items)).map { WidgetBucket(minHeightFor(it, fontScale), it) }

    /** 그릴 줄 수 = 식과 항목 수 중 작은 쪽(widget.js items.slice(0, rows)) */
    fun visibleRows(heightDp: Double, fontScale: Double, items: Int): Int = minOf(WidgetRules.rows(heightDp, fontScale), items)
}
