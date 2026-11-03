package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.DateLine
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.IssueMap
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Messages
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Share
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(읽기 화면, AC-15-1 · AC-04 · AC-05): 첫 화면 30개 · 갱신 시각 · 이슈맵 타일 30 → 상세 · 대체 섹션 '수집' · 원문 = Custom Tab ·
 * 목록 카드(순위 · 제목 · 요약 · 요약 없음 · 연관기사 · 기사 원문 · 공유 · 북마크) · 요약 펼치기 · 섹션 탭 · 확대.
 * 가짜 서버 씨앗 D0(README 3절). 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class ReadScreenTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val s get() = server.strings
    private val today get() = server.today

    private fun ranking(): Ranking = compose.activity.viewModel.state.value.ranking!!
    private fun Ranking.all(): List<Issue> = SectionKey.entries.flatMap { issuesOf(it) }

    @Test
    fun firstScreen_isTheMapOf30_withUpdatedLine() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.VIEW_MAP).assertExists()
        compose.onNodeWithTag(TestTags.RANK_TITLE).assertTextEquals(s[Str.RANK_TITLE_TODAY])
        compose.onNodeWithTag(TestTags.RANK_DESC).assertTextEquals(s[Str.RANK_DESC_MAP])
        compose.onNodeWithTag(TestTags.UPDATED).assertTextEquals(s[Str.UPDATED_LINE, KstDate.fmtUpdated("${today}T04:31:27+09:00", s), 30])
        compose.onNodeWithTag(TestTags.DATE_TEXT, useUnmergedTree = true).assertTextEquals(s[Str.DATE_TEXT_TODAY, KstDate.fmtDate(today, s)])
        compose.onNodeWithTag(TestTags.DATE_NEXT).assertIsNotEnabled()
        compose.onAllNodesWithTag(TestTags.DATE_TODAY).assertCountEquals(0)
        // 이름(웹 aria-label)
        compose.onNodeWithTag(TestTags.BRAND).assertContentDescriptionEquals(s[Str.CD_HOME])
        compose.onNodeWithTag(TestTags.DATE_PREV).assertContentDescriptionEquals(s[Str.CD_PREV_DAY])
        compose.onNodeWithTag(TestTags.DATE_NEXT).assertContentDescriptionEquals(s[Str.CD_NEXT_DAY])
        compose.onNodeWithTag(TestTags.DATE_PICK).assertContentDescriptionEquals(s[Str.CD_PICK_DATE])
        compose.onNodeWithTag(TestTags.tile("$today-pol-21"))
            .assertContentDescriptionEquals(s[Str.CD_TILE, s[Str.SECTION_POL], 1, "여야, 내년도 예산안 처리 시한 앞두고 막판 협상", 120])
        // 첫 요청 = 날짜 없이(최근 수집일) 한 번
        assertEquals(listOf("/api/rankings"), FakeServer.rankingPaths())
        // 경로 줄 = 연관기사 합계
        val r = ranking()
        compose.onNode(hasText(IssueMap.crumbTotal(IssueMap.layout(r, null, false, 1.0, 1.0), null, r, s)) and hasAnyAncestor(hasTestTag(TestTags.CRUMBS)))
            .assertExists()
    }

    @Test
    fun everyTile_opensItsDetail() {
        compose.waitForTiles(30)
        val r = ranking()
        assertEquals(30, r.all().size)
        for (issue in r.all()) {
            compose.onNodeWithTag(TestTags.tile(issue.id)).performScrollTo().performClick()
            compose.waitForTag(TestTags.DETAIL)
            assertDetail(issue, r)
            compose.onNodeWithTag(TestTags.DETAIL_CLOSE).assertContentDescriptionEquals(s[Str.CLOSE]).performClick()
            compose.waitForTagGone(TestTags.DETAIL)
        }
    }

    private fun assertDetail(issue: Issue, r: Ranking) {
        compose.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals(issue.title)
        compose.onNodeWithTag(TestTags.DETAIL_COUNT).assertTextEquals(s[Str.COUNT_N, issue.articleCount])
        compose.onNodeWithTag(TestTags.DETAIL_RANK).assertTextEquals(s[Str.DETAIL_RANK_VALUE, s[issue.section.label], issue.rank])
        compose.onNodeWithTag(TestTags.DETAIL_COLLECTED).assertTextEquals(DateLine.collected(issue, r, s))
        for (line in issue.summary ?: listOf(s[Str.SUMMARY_NONE])) {
            compose.onNode(hasText(line) and hasAnyAncestor(hasTestTag(TestTags.DETAIL_SUMMARY)), useUnmergedTree = true).assertExists()
        }
        compose.onNode(hasText(s[Str.RANKING_OF_DATE, KstDate.fmtDate(issue.date, s)]) and hasAnyAncestor(hasTestTag(TestTags.DETAIL_META)), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun detail_fallbackSectionShowsDateOnly_summaryNone_andOriginalIsACustomTab() {
        compose.waitForTiles(30)
        val r = ranking()
        // 대체 섹션(생활·문화 = 어제 카드): '수집' = 그 날짜만
        val cul = r.issuesOf(SectionKey.CUL).first()
        assertEquals(server.day(-1), cul.date)
        compose.onNodeWithTag(TestTags.tile(cul.id)).performScrollTo().performClick()
        compose.waitForTag(TestTags.DETAIL)
        compose.onNodeWithTag(TestTags.DETAIL_COLLECTED).assertTextEquals(KstDate.fmtShort(cul.date, s))
        compose.onNodeWithTag(TestTags.DETAIL_ORIGINAL).performClick()
        compose.waitUntil(LOAD_TIMEOUT_MS) { server.opened.isNotEmpty() }
        assertEquals(listOf(cul.url), server.opened.toList())
        compose.onNodeWithTag(TestTags.DETAIL_CLOSE).performClick()
        compose.waitForTagGone(TestTags.DETAIL)
        // 요약 없음(정치 2위)
        compose.onNodeWithTag(TestTags.tile("$today-pol-22")).performScrollTo().performClick()
        compose.waitForTag(TestTags.DETAIL)
        compose.onNode(hasText(s[Str.SUMMARY_NONE]) and hasAnyAncestor(hasTestTag(TestTags.DETAIL_SUMMARY)), useUnmergedTree = true).assertExists()
        // 상세 북마크(비로그인) = 로그인 안내 토스트 + "로그인" 단추
        compose.onNodeWithTag(TestTags.DETAIL_BOOKMARK).performClick()
        compose.waitForText(Messages.bookmarkNeedsLogin.message.text(s))
        compose.onNodeWithTag(TestTags.TOAST_ACTION, useUnmergedTree = true).assertExists()
    }

    @Test
    fun list_cardsInServerOrder_withSummaryOrNone_countOriginalShareBookmark() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.VIEW_LIST)
        compose.onNodeWithTag(TestTags.RANK_DESC).assertTextEquals(s[Str.RANK_DESC_LIST])
        val r = ranking()
        for (issue in r.all()) {
            compose.onNodeWithTag(TestTags.card(issue.id)).performScrollTo().assertExists()
            compose.onNodeWithTag(TestTags.cardTitle(issue.id)).assertTextEquals(issue.title)
            val card = hasAnyAncestor(hasTestTag(TestTags.card(issue.id)))
            compose.onNode(hasText(s[Str.RANK_N, issue.rank]) and card, useUnmergedTree = true).assertExists()
            compose.onNode(hasText("${issue.articleCount}") and card, useUnmergedTree = true).assertExists()
            for (line in issue.summary ?: listOf(s[Str.SUMMARY_NONE])) compose.onNode(hasText(line) and card, useUnmergedTree = true).assertExists()
            compose.onNodeWithTag(TestTags.cardShare(issue.id)).assertContentDescriptionEquals(s[Str.CD_SHARE_ISSUE])
            compose.onNodeWithTag(TestTags.cardBookmark(issue.id)).assertContentDescriptionEquals(s[Str.BOOKMARKS])
        }
        // 동점 순서 그대로(정치 120건 넷 — README: pol-21 · pol-22 · pol-9 · pol-10 · pol-3)
        assertEquals(listOf("pol-21", "pol-22", "pol-9", "pol-10", "pol-3").map { "$today-$it" }, r.issuesOf(SectionKey.POL).map { it.id })
        // 기사 원문 = Custom Tab(걸이 기록) · 공유 = 웹 글. 맨 위로 되돌려 누름(머리 밑에 붙은 섹션 탭이 카드를 가리지 않게)
        val first = r.issuesOf(SectionKey.POL).first()
        compose.onNodeWithTag(TestTags.RANK_TITLE).performScrollTo()
        compose.onNodeWithTag(TestTags.cardOriginal(first.id)).performClick()
        compose.onNodeWithTag(TestTags.cardShare(first.id)).performClick()
        compose.waitUntil(LOAD_TIMEOUT_MS) { server.opened.isNotEmpty() && server.shared.isNotEmpty() }
        assertEquals(listOf(first.url), server.opened.toList())
        assertEquals(Share.issue(first, s), server.shared.single())
        // 비로그인 북마크 = 토스트
        compose.onNodeWithTag(TestTags.cardBookmark(first.id)).performClick()
        compose.waitForText(s[Str.TOAST_BM_LOGIN])
    }

    @Test
    fun list_summaryToggleAndSectionTabs() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.VIEW_LIST)
        val id = "$today-pol-21"
        compose.onNodeWithTag(TestTags.cardSummary(id), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(TestTags.SUMMARY_TOGGLE).performClick() // 끔 = 요약 · 원문 · 공유 숨김
        compose.waitForTagGone(TestTags.cardSummary(id))
        compose.onAllNodesWithTag(TestTags.cardOriginal(id)).assertCountEquals(0)
        compose.onAllNodesWithTag(TestTags.cardShare(id)).assertCountEquals(0)
        compose.onNodeWithTag(TestTags.SUMMARY_TOGGLE).performClick()
        compose.waitForTag(TestTags.cardSummary(id))
        // 섹션 탭: 경제만 → 전체
        compose.onNodeWithTag(TestTags.sectionTab(SectionKey.ECO)).performClick()
        compose.waitForTagGone(TestTags.group(SectionKey.POL))
        compose.onNodeWithTag(TestTags.group(SectionKey.ECO)).assertExists()
        compose.onNodeWithTag(TestTags.sectionTab(null)).performClick()
        for (key in SectionKey.entries) compose.waitForTag(TestTags.group(key))
    }

    @Test
    fun map_zoomByLegendAndLabel_andBack() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.legend(SectionKey.ECO)).performClick()
        compose.waitForTiles(5)
        val r = ranking()
        compose.onNode(hasText(s[Str.MAP_SECTION_TOTAL, r.sectionTotal(SectionKey.ECO)]) and hasAnyAncestor(hasTestTag(TestTags.CRUMBS)), useUnmergedTree = true).assertExists()
        assertTrue(compose.onAllNodes(hasTestTagPrefix(TestTags.TILE_PREFIX)).fetchSemanticsNodes().all { it.tag()!!.contains("-eco-") })
        compose.onNodeWithTag(TestTags.CRUMB_ALL).performClick()
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.mapLabel(SectionKey.SOC)).performScrollTo().performClick() // "확대 →"
        compose.waitForTiles(5)
        compose.onNodeWithTag(TestTags.mapLabel(SectionKey.SOC)).performClick() // "← 전체"
        compose.waitForTiles(30)
    }
}
