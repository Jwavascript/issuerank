package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.WidgetRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 위젯 높이 → 줄 수(계획 4절 식 min(10, max(1, ⌊(h − 24 − 20f) ÷ 22f⌋)))와 Android 12+ 높이별 묶음(≤ 16):
 * 묶음 경계 = 식의 경계(f = 0.85 · 1 · 1.15 · 1.3 · 2) · 런처가 고르는 묶음의 줄 수 = 식(40~600dp를 0.01씩 전부).
 */
class WidgetSizesTest {
    private val scales = listOf(0.85, 1.0, 1.15, 1.3, 2.0)

    @Test
    fun scale1_bucketsAre66To264Every22_tenOfThem() {
        val buckets = WidgetSizes.buckets(1.0, 30)
        assertEquals(listOf(66.0, 88.0, 110.0, 132.0, 154.0, 176.0, 198.0, 220.0, 242.0, 264.0), buckets.map { it.minHeightDp })
        assertEquals((1..10).toList(), buckets.map { it.rows })
        // 계획 4절 예: 117→3 · 150→4 · 260→9 · 290→10, 최소 크기 66 → 1
        assertEquals(listOf(3, 4, 9, 10, 1), listOf(117.0, 150.0, 260.0, 290.0, 66.0).map { WidgetSizes.visibleRows(it, 1.0, 30) })
    }

    @Test
    fun everyScale_eachBucketStartsExactlyWhereItsRowCountStarts_andThereAreAtMost16() {
        for (f in scales) {
            val buckets = WidgetSizes.buckets(f, 30)
            assertEquals("f=$f", 10, buckets.size)
            assertTrue("f=$f", buckets.size <= WidgetSizes.MAX_SIZED)
            assertEquals("f=$f 높이 순", buckets.sortedBy { it.minHeightDp }, buckets)
            for ((h, n) in buckets) {
                assertEquals("f=$f n=$n h=$h", n, WidgetRules.rows(h, f))
                // 한 칸(0.01dp) 아래 = 한 줄 적음. 칸은 센트로 셈(h − 0.01을 부동소수로 빼면 칸 위가 됨 — 221.6 − 0.01 = 221.60000000000002)
                val below = (Math.round(h * 100) - 1) / 100.0
                if (n > 1) assertEquals("f=$f n=$n 한 칸 아래 $below", n - 1, WidgetRules.rows(below, f))
                assertEquals("f=$f n=$n 식의 경계 24 + 20f + 22f·n", 24 + 20 * f + 22 * f * n, h, 0.0100001)
            }
            println("f=$f 묶음 ${buckets.map { "${it.rows}줄≥${it.minHeightDp}" }}")
        }
        // 항목이 적으면 그 수까지(그 위 높이는 같은 그림이라 묶지 않음)
        assertEquals(listOf(1, 2, 3), WidgetSizes.buckets(1.3, 3).map { it.rows })
        assertEquals(listOf(1), WidgetSizes.buckets(2.0, 1).map { it.rows })
    }

    /**
     * 런처 규칙(RemoteViews(Map) 공식 설명): 지금 크기에 들어가는 것 중 넓이가 가장 큰 것, 들어가는 것이 없으면 넓이가 가장 작은 것.
     * 묶음 가로는 모두 BUCKET_WIDTH_DP(어떤 폭에도 들어감)라 넓이 = 가로 × 높이.
     */
    private fun launcherPick(buckets: List<WidgetBucket>, heightDp: Double): WidgetBucket {
        val area = { b: WidgetBucket -> WidgetSizes.BUCKET_WIDTH_DP * b.minHeightDp }
        return buckets.filter { it.minHeightDp <= heightDp }.maxByOrNull(area) ?: buckets.minBy(area)
    }

    @Test
    fun launcherPick_rowsEqualTheFormula_forEveryHeight40To600() {
        assertTrue(WidgetSizes.BUCKET_WIDTH_DP > 0f) // 0이면 넓이가 모두 0 — 고를 칸이 정해지지 않음
        for (f in scales) for (items in listOf(30, 10, 3, 1)) {
            val buckets = WidgetSizes.buckets(f, items)
            for (cents in 4_000..60_000) {
                val h = cents / 100.0
                assertEquals("f=$f items=$items h=$h", WidgetSizes.visibleRows(h, f, items), launcherPick(buckets, h).rows)
            }
        }
    }

    @Test
    fun belowTheFirstBucket_theSmallestBucketIsOneRow() {
        // 글자가 크고 위젯이 아주 낮으면(h < 24 + 42f) 들어가는 칸이 없음 → 가장 작은 칸 = 1줄(식의 max(1, …)과 같음)
        for (f in scales) {
            val buckets = WidgetSizes.buckets(f, 30)
            assertEquals(1, launcherPick(buckets, buckets.first().minHeightDp - 1).rows)
            assertEquals(1, WidgetRules.rows(buckets.first().minHeightDp - 1, f))
        }
    }
}
