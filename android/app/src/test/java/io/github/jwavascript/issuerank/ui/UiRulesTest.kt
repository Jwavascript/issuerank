package io.github.jwavascript.issuerank.ui

import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.IssueMap
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.TestFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 화면 쪽 순수 규칙: 달력 날짜(UTC 자정 밀리초) · 미래 막기 · 화면 구간 · 이슈맵 제목 줄 수(글자 배율) */
class UiRulesTest {
    @Test
    fun pickerDates_roundTripAndBlockTheFuture() {
        val today = "2026-11-03"
        for (d in listOf("2026-11-03", "2026-02-28", "2024-02-29", "2000-01-01", "1999-12-31")) {
            assertEquals(d, PickerDates.fromMillis(PickerDates.toMillis(d)))
            assertEquals(d, PickerDates.fromMillis(PickerDates.toMillis(d) + 23 * 3_600_000L)) // 그날 안 어느 시각이든 같은 날
        }
        assertEquals(1_793_664_000_000L, PickerDates.toMillis(today)) // 2026-11-03T00:00:00Z
        assertTrue(PickerDates.selectable(PickerDates.toMillis(today), today))
        assertTrue(PickerDates.selectable(PickerDates.toMillis("2000-01-01"), today)) // 앞쪽 한계 없음(없는 날 = 404 안내)
        assertFalse(PickerDates.selectable(PickerDates.toMillis(KstDate.addDays(today, 1)), today))
        assertFalse(PickerDates.selectable(PickerDates.toMillis("2027-01-01"), today))
        assertTrue(PickerDates.selectableYear(2026, today))
        assertFalse(PickerDates.selectableYear(2027, today))
    }

    @Test
    fun breakpoints_areTheWebMediaQueries() {
        assertTrue(Breakpoints(640).narrow)
        assertFalse(Breakpoints(641).narrow)
        assertTrue(Breakpoints(340).tiny)
        assertFalse(Breakpoints(341).tiny)
        assertTrue(Breakpoints(899).belowMedium)
        assertFalse(Breakpoints(900).belowMedium)
        assertTrue(Breakpoints(1199).belowWide)
        assertFalse(Breakpoints(1200).belowWide)
        // 이 태블릿(세로 753 · 가로 1205)은 넓은 배치, 폰 360은 좁은 배치(이슈맵 세로 쌓기 = IssueMap.isNarrow와 같은 경계)
        for (w in listOf(360, 640, 641, 753, 1205)) assertEquals("$w", IssueMap.isNarrow(w.toDouble()), Breakpoints(w).narrow)
    }

    @Test
    fun tileTitleLines_equalTheWebAtScale1_andFitAtLargerScales() {
        val ranking = ApiJson.ranking(TestFiles.fixture("rankings-d0.json"))
        for (vw in listOf(360, 753, 1205)) {
            for (zoom in listOf(null, ranking.sections[1].key)) {
                val layout = IssueMap.layout(ranking, zoom, IssueMap.isNarrow(vw.toDouble()), IssueMap.mapWidth(vw.toDouble()), 800.0)
                for (t in layout.boxes.filter { !it.hidden }.flatMap { it.tiles }.filter { !it.xs }) {
                    assertEquals(t.maxLines, titleLines(t, 1f))
                    for (scale in listOf(1.15f, 1.3f, 2f)) {
                        val lines = titleLines(t, scale)
                        assertTrue(lines in 1..t.maxLines)
                        // 두 줄 이상이면 제목 + 아래 줄 + 여백이 타일 높이 안
                        if (lines > 1) assertTrue("${t.issue.id} $scale", lines * t.fontSize * scale * 1.32 + 36 + 20 * (scale - 1) <= t.h)
                    }
                }
            }
        }
    }
}
