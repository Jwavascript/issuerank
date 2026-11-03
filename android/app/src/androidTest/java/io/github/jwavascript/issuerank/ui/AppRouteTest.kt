package io.github.jwavascript.issuerank.ui

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.AppRoutes
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(앱 경로, AC-15-4 앞부분 · O1): 위젯이 줄 Intent(MainActivity.EXTRA_PATH)로 열면 `/?issue=<id>` → 그 날짜 랭킹 → 상세,
 * 없는 id · 형식 틀림 · 없는 날 → "이슈를 찾을 수 없습니다", `/#list` → 랭킹 목록, `/` → 첫 화면, 떠 있을 때(onNewIntent · singleTop) 같은 처리,
 * 다시 만들기(돌리기)에도 날짜 · 탭 유지. 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class AppRouteTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val s get() = server.strings

    private fun launch(path: String): ActivityScenario<MainActivity> =
        ActivityScenario.launch(Intent(server.context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PATH, path))

    @Test
    fun issuePath_opensThatDetail() {
        launch(AppRoutes.issuePath("${server.today}-pol-21")).use {
            compose.waitForTag(TestTags.DETAIL)
            compose.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals("여야, 내년도 예산안 처리 시한 앞두고 막판 협상")
            compose.onNodeWithTag(TestTags.VIEW_MAP).assertExists()
            assertEquals(listOf("/api/rankings?date=${server.today}"), FakeServer.rankingPaths())
        }
    }

    @Test
    fun pastIssuePath_opensThePastRankingAndDetail() {
        val d1 = server.day(-1)
        val issue = FakeServer.ranking(d1).issuesOf(SectionKey.ECO).first()
        launch(AppRoutes.issuePath(issue.id)).use {
            compose.waitForTag(TestTags.DETAIL)
            compose.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals(issue.title)
            compose.onNodeWithTag(TestTags.RANK_TITLE).assertTextEquals(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(d1, s)])
        }
    }

    @Test
    fun unknownMalformedOrMissingDay_toastsNotFound() {
        val toast = AppRoutes.notFoundToast.message.text(s)
        for (path in listOf(AppRoutes.issuePath("${server.today}-pol-99"), "/?issue=abc", AppRoutes.issuePath("${server.day(-3)}-pol-1"))) {
            launch(path).use {
                compose.waitForText(toast)
                compose.onAllNodesWithTag(TestTags.DETAIL).assertCountEquals(0)
            }
        }
    }

    @Test
    fun listPath_opensTheList_homePath_opensTheMap() {
        launch(AppRoutes.LIST).use {
            compose.waitForTag(TestTags.card("${server.today}-pol-21"))
            compose.onNodeWithTag(TestTags.VIEW_LIST).assertExists()
        }
        launch(AppRoutes.HOME).use {
            compose.waitForTiles(30)
            assertEquals("/api/rankings", FakeServer.rankingPaths().last())
        }
    }

    /** 지금 앞에 있는 MainActivity들(메인 스레드에서 읽음) */
    private fun resumedMainActivities(): List<MainActivity> {
        var out = emptyList<MainActivity>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            out = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>()
        }
        return out
    }

    @Test
    fun newIntentWhileShowing_isHandledByTheSameScreen() {
        // 위젯과 같은 길: 둘 다 routeIntent(NEW_TASK · CLEAR_TOP) — 두 번째는 떠 있는 화면이 onNewIntent로(singleTop)
        val context = server.context
        context.startActivity(MainActivity.routeIntent(context, AppRoutes.HOME))
        compose.waitForTiles(30)
        val first = resumedMainActivities().single()
        try {
            context.startActivity(MainActivity.routeIntent(context, AppRoutes.issuePath("${server.today}-it-3")))
            compose.waitForTag(TestTags.DETAIL)
            compose.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals("누리호 5차 발사 준비 착수")
            context.startActivity(MainActivity.routeIntent(context, AppRoutes.LIST))
            compose.waitForTag(TestTags.VIEW_LIST)
            compose.waitForTagGone(TestTags.DETAIL)
            assertEquals(listOf(first), resumedMainActivities()) // 같은 화면(새로 만들지 않음)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { first.finish() }
        }
    }

    @Test
    fun recreate_keepsTheDateAndTab() {
        launch(AppRoutes.HOME).use { scenario ->
            compose.waitForTiles(30)
            compose.onNodeWithTag(TestTags.DATE_PREV).performClick()
            compose.waitForTag(TestTags.DATE_TODAY)
            compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
            compose.waitForTag(TestTags.VIEW_LIST)
            scenario.recreate()
            compose.waitForTag(TestTags.VIEW_LIST)
            compose.onNodeWithTag(TestTags.RANK_TITLE).assertTextEquals(s[Str.RANK_TITLE_PAST, KstDate.fmtDate(server.day(-1), s)])
        }
    }
}
