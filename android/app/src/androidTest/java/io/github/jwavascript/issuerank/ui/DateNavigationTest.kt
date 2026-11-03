package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(날짜, AC-09 · AC-15-1): 지난 날 "n월 n일의 주요 이슈" · 갱신 시각 · "오늘" 단추 · 없는 날 "해당 날짜의 랭킹 데이터가 없습니다"(토스트 0) ·
 * "오늘 랭킹 보기" · 오늘이면 다음 막힘 · 미래(달력이 막고 앱도 오늘로) · 달력 창 · 지난 날 카드의 북마크 단추. 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class DateNavigationTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val s get() = server.strings

    private fun waitTitle(text: String) = compose.waitUntil(LOAD_TIMEOUT_MS) {
        compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun pastDays_missingDay_backToToday() {
        val today = server.today
        compose.waitForTiles(30)
        // D-1: 제목 · 날짜 · 갱신 04:28 · "오늘" 보임 · 다음 열림
        val d1 = server.day(-1)
        compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
        waitTitle(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(d1, s)])
        compose.onNodeWithTag(TestTags.DATE_TEXT, useUnmergedTree = true).assertTextEquals(KstDate.fmtDate(d1, s))
        compose.onNodeWithTag(TestTags.UPDATED).assertTextEquals(s[Str.UPDATED_LINE, KstDate.fmtUpdated("${d1}T04:28:05+09:00", s), 30])
        compose.onNodeWithTag(TestTags.DATE_TODAY).assertExists()
        compose.onNodeWithTag(TestTags.DATE_NEXT).assertIsEnabled()
        compose.waitForTiles(30)
        // D-2: 수집 기록 없음 → 그날 06:00(AC-05-1)
        val d2 = server.day(-2)
        compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
        waitTitle(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(d2, s)])
        compose.onNodeWithTag(TestTags.UPDATED).assertTextEquals(s[Str.UPDATED_LINE, KstDate.fmtUpdated("${d2}T06:00:00+09:00", s), 30])
        // D-3: 없는 날 = 빈 화면 · 갱신 줄 없음 · 토스트 없음
        val d3 = server.day(-3)
        compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
        compose.waitForTag(TestTags.EMPTY_NO_DATA)
        compose.onNodeWithTag(TestTags.RANK_TITLE).assertTextEquals(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(d3, s)])
        compose.onNodeWithText(s[Str.EMPTY_NO_DATA_TITLE]).assertExists()
        compose.onNodeWithText(s[Str.EMPTY_NO_DATA_BODY]).assertExists()
        compose.onAllNodesWithTag(TestTags.UPDATED).assertCountEquals(0)
        compose.onAllNodesWithTag(TestTags.TOAST).assertCountEquals(0)
        compose.onAllNodes(hasTestTagPrefix(TestTags.TILE_PREFIX)).assertCountEquals(0)
        // 목록 화면에서도 같은 빈 화면
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.EMPTY_NO_DATA)
        // "오늘 랭킹 보기" → 오늘(?date=오늘)
        compose.onNodeWithTag(TestTags.EMPTY_TODAY).performClick()
        waitTitle(s[Str.RANK_TITLE_TODAY])
        compose.onNodeWithTag(TestTags.DATE_NEXT).assertIsNotEnabled()
        compose.onAllNodesWithTag(TestTags.DATE_TODAY).assertCountEquals(0)
        assertEquals(
            listOf("/api/rankings", "/api/rankings?date=$d1", "/api/rankings?date=$d2", "/api/rankings?date=$d3", "/api/rankings?date=$today"),
            FakeServer.rankingPaths(),
        )
    }

    @Test
    fun future_isBlocked_andTheTodayChipReturns() {
        val today = server.today
        compose.waitForTiles(30)
        // 오늘: 다음 막힘
        compose.onNodeWithTag(TestTags.DATE_NEXT).assertIsNotEnabled()
        // 달력에서 내일을 골라도(달력이 막음 — UiRulesTest) 앱은 오늘로: 내일 요청 0
        compose.runOnUiThread { compose.activity.viewModel.pickDate(KstDate.addDays(today, 1)) }
        compose.waitForIdle()
        compose.waitUntil(LOAD_TIMEOUT_MS) { FakeServer.rankingPaths().size == 2 }
        assertEquals("/api/rankings?date=$today", FakeServer.rankingPaths().last())
        assertFalse(FakeServer.rankingPaths().any { it.endsWith(KstDate.addDays(today, 1)) })
        compose.onNodeWithTag(TestTags.RANK_TITLE).assertTextEquals(s[Str.RANK_TITLE_TODAY])
        // 지난 날 → "오늘" 단추
        compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
        compose.waitForTag(TestTags.DATE_TODAY)
        compose.onNodeWithTag(TestTags.DATE_TODAY).performClick()
        waitTitle(s[Str.RANK_TITLE_TODAY])
    }

    @Test
    fun datePicker_opensAndCancels() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.DATE_PICK).performClick()
        compose.waitForTag(TestTags.DATE_DIALOG)
        compose.onNodeWithTag(TestTags.DATE_DIALOG_OK).assertExists()
        compose.onNode(hasText(s[Str.CANCEL])).performClick()
        compose.waitForTagGone(TestTags.DATE_DIALOG)
        compose.onAllNodesWithText(s[Str.EMPTY_NO_DATA_TITLE]).assertCountEquals(0)
    }

    @Test
    fun pastDayCards_keepTheBookmarkButton() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
        waitTitle(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(server.day(-1), s)])
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.VIEW_LIST)
        val first = compose.activity.viewModel.state.value.ranking!!.issuesOf(SectionKey.POL).first()
        assertEquals(server.day(-1), first.date)
        compose.onNodeWithTag(TestTags.cardBookmark(first.id)).performClick()
        compose.waitForText(s[Str.TOAST_BM_LOGIN])
    }
}
