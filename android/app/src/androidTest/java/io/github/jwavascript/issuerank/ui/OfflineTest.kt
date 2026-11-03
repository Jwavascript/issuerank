package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(오프라인, AC-13-3 · AC-15-5): 서버 끔(mode down = 연결 끊음) → offline.html 문구("인터넷에 연결되어 있지 않습니다" ·
 * "연결되면 다시 시도해 주세요. 랭킹은 매일 새벽 갱신됩니다." · "다시 시도") → 켜고 다시 시도 = 같은 요청. 토스트 0. 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class OfflineTest {
    @get:Rule(order = 0)
    val server = FakeServerRule(startDown = true)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val s get() = server.strings

    private fun assertOfflineNotice() {
        compose.waitForTag(TestTags.OFFLINE)
        val inNotice = hasAnyAncestor(hasTestTag(TestTags.OFFLINE))
        compose.onNode(hasText(s[Str.OFFLINE_TITLE]) and inNotice, useUnmergedTree = true).assertExists()
        compose.onNode(hasText(s[Str.OFFLINE_NOTE]) and inNotice, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(TestTags.OFFLINE_RETRY).assertTextEquals(s[Str.RETRY])
        compose.onAllNodes(hasTestTagPrefix(TestTags.TILE_PREFIX)).assertCountEquals(0)
        compose.onAllNodesWithTag(TestTags.TOAST).assertCountEquals(0)
    }

    @Test
    fun serverDownAtStart_showsTheOfflineNotice_retryLoads() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assertOfflineNotice()
            // 목록 탭도 같은 안내
            compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
            assertOfflineNotice()
            compose.onNodeWithTag(TestTags.TAB_MAP).performClick()
            FakeServer.normal()
            compose.onNodeWithTag(TestTags.OFFLINE_RETRY).performClick()
            compose.waitForTiles(30)
            compose.onAllNodesWithTag(TestTags.OFFLINE).assertCountEquals(0)
        }
    }
}

/** 보던 중 끊김: 이전 날짜 → 안내 → 켜고 다시 시도 = 그 날짜 */
@RunWith(AndroidJUnit4::class)
class OfflineMidSessionTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun dropDuringDateChange_retryRepeatsThatDate() {
        val s = server.strings
        compose.waitForTiles(30)
        FakeServer.down()
        compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
        compose.waitForTag(TestTags.OFFLINE)
        compose.onNode(hasText(s[Str.OFFLINE_TITLE])).assertExists()
        FakeServer.normal()
        compose.onNodeWithTag(TestTags.OFFLINE_RETRY).performClick()
        compose.waitForTiles(30)
        val d1 = server.day(-1)
        compose.onNodeWithTag(TestTags.RANK_TITLE).assertTextEquals(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(d1, s)])
        assertEquals("/api/rankings?date=$d1", FakeServer.rankingPaths().last())
    }
}
