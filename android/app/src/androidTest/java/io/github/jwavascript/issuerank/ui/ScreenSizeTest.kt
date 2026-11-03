package io.github.jwavascript.issuerank.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.MainViewModel
import io.github.jwavascript.issuerank.data.AppRoutes
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(화면 크기 · 접근성, 계획 5절): 폰 360dp(ForcedSize) · 어둡게 · 글자 1.3배 → 가로 넘침 0(화면 노드가 모두 화면 폭 안 · 글 가로 넘침 0) ·
 * 이슈맵 세로 쌓기 · 목록 한 열 · 누를 곳 48dp 이상 + 서로 안 겹침(타일 · 블록 라벨은 데이터 크기라 뺌 — 노트 04) · 태블릿 753 · 1205 = 넓은 배치.
 * 앱 뿌리(IssueRankApp)를 시험 Activity에 덮어쓰기와 함께 그림(시험마다 setContent 한 번). 상세 창은 자기 창(기기 폭)에 그려져 그 창 폭 기준.
 * 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class ScreenSizeTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(width: Int, height: Int, dark: Boolean, fontScale: Float, path: String = AppRoutes.HOME): MainViewModel {
        val graph = AppGraph.get(compose.activity)
        val vm = MainViewModel(SavedStateHandle(), graph.api, graph.clock)
        compose.runOnUiThread { vm.start(path) }
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp)) then
                    DeviceConfigurationOverride.DarkMode(dark) then
                    DeviceConfigurationOverride.FontScale(fontScale),
            ) { IssueRankApp(vm) }
        }
        return vm
    }

    private fun node(tag: String): SemanticsNode = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()

    /** container 아래 모든 노드(펼친 나무)가 container 폭 안 + 글 노드 가로 넘침 0 */
    private fun assertNoHorizontalOverflow(where: String, container: String = TestTags.APP) {
        val box = node(container).boundsInRoot
        val px = 0.5f
        val bad = mutableListOf<String>()
        for (n in node(container).descendantsAndSelf()) {
            val b = n.boundsInRoot
            if (b.width <= 0f) continue
            if (b.left < box.left - px || b.right > box.right + px) bad += "${n.tag() ?: n.id} $b"
            if (n.textLayout()?.didOverflowWidth == true) bad += "글 넘침 ${n.tag() ?: n.id}"
        }
        assertEquals(where, emptyList<String>(), bad)
    }

    /**
     * 화면(APP) 안 누를 곳(OnClick): 누름 칸(touchBounds — 작으면 Compose가 48dp로 넓힘) 48dp 이상, 그리고 넓힌 칸이 이웃 누를 곳의
     * 보이는 칸을 덮지 않음(= 48dp가 실제로 그 단추 몫). 화면 끝에 잘린 것 · 머리 밑에 붙은 섹션 탭 아래 가린 것 · 타일 · 블록 라벨(데이터 크기)은 뺌.
     */
    private fun assertTouchTargets(where: String) {
        val min = with(compose.density) { 48.dp.toPx() } - 1f
        val tabs = compose.onAllNodesWithTag(TestTags.SECTION_TABS, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        val nodes = node(TestTags.APP).descendantsAndSelf()
            .filter { SemanticsActions.OnClick in it.config }
            .filter { n -> n.tag()?.let { it.startsWith(TestTags.TILE_PREFIX) || it.startsWith("map-label:") } != true }
            .filter { n -> n.boundsInRoot.let { it.width >= n.size.width - 1f && it.height >= n.size.height - 1f && it.width > 0f } } // 잘리지 않음
            .filter { n -> tabs == null || n.tag()?.startsWith("section-tab:") == true || !n.boundsInRoot.overlaps(tabs) }
        val small = nodes.filter { it.touchBoundsInRoot.width < min || it.touchBoundsInRoot.height < min }
            .map { "${it.tag() ?: it.id} ${it.touchBoundsInRoot}" }
        assertEquals("$where 48dp 미만", emptyList<String>(), small)
        val crowded = mutableListOf<String>()
        for (n in nodes.filter { it.boundsInRoot.width < min || it.boundsInRoot.height < min }) {
            for (m in nodes) {
                if (m === n) continue
                val o = n.touchBoundsInRoot.intersect(m.boundsInRoot)
                if (o.width > 1f && o.height > 1f) crowded += "${n.tag() ?: n.id} → ${m.tag() ?: m.id}"
            }
        }
        assertEquals("$where 넓힌 누름 칸이 이웃을 덮음", emptyList<String>(), crowded)
    }

    @Test
    fun phone360_dark_font13_map_isStackedWithoutOverflow() {
        show(360, 800, dark = true, fontScale = 1.3f)
        compose.waitForTiles(30)
        // 640 이하 = 섹션 블록 세로 쌓기(폭 전체 · 위에서 아래로)
        val boxes = SectionKey.entries.map { node(TestTags.mapBox(it)).boundsInRoot }
        assertTrue(boxes.zipWithNext().all { (a, b) -> b.top > a.top && b.left == a.left && b.width == a.width })
        assertNoHorizontalOverflow("이슈맵")
        assertTouchTargets("이슈맵")
        // 어둡게 = 웹 dark bg 토큰(#121211)
        val pixel = compose.onNodeWithTag(TestTags.HEADER).captureToImage().asAndroidBitmap().getPixel(2, 2)
        assertEquals(0xFF121211.toInt(), pixel)
    }

    @Test
    fun phone360_dark_font13_list_isOneColumnWithoutOverflow() {
        show(360, 800, dark = true, fontScale = 1.3f, path = AppRoutes.LIST)
        val ranking = FakeServer.ranking()
        compose.waitForTag(TestTags.card(ranking.issuesOf(SectionKey.POL).first().id))
        val cards = ranking.issuesOf(SectionKey.POL).map { compose.onNodeWithTag(TestTags.card(it.id)).performScrollTo().fetchSemanticsNode().boundsInRoot }
        assertTrue(cards.all { it.left == cards[0].left && it.right == cards[0].right }) // 한 열
        assertNoHorizontalOverflow("목록 위")
        assertTouchTargets("목록 위")
        compose.onNodeWithTag(TestTags.card(ranking.issuesOf(SectionKey.IT).last().id)).performScrollTo()
        assertNoHorizontalOverflow("목록 끝")
        assertTouchTargets("목록 끝")
    }

    @Test
    fun phone360_dark_font13_detail_hasNoOverflow() {
        show(360, 800, dark = true, fontScale = 1.3f)
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.tile("${server.today}-eco-20")).performScrollTo().performClick() // 아주 긴 제목 · 긴 요약
        compose.waitForTag(TestTags.DETAIL)
        assertNoHorizontalOverflow("상세", container = TestTags.DETAIL)
    }

    @Test
    fun phone360_light_bookmarksAndOffline_haveNoOverflow() {
        val vm = show(360, 800, dark = false, fontScale = 1f)
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.TAB_BOOKMARKS).performClick()
        compose.waitForTag(TestTags.BOOKMARKS_LOGIN)
        assertNoHorizontalOverflow("북마크")
        assertTouchTargets("북마크")
        FakeServer.down()
        compose.runOnUiThread { vm.selectView(View.MAP); vm.prevDay() }
        compose.waitForTag(TestTags.OFFLINE)
        assertNoHorizontalOverflow("오프라인")
        assertTouchTargets("오프라인")
    }

    @Test
    fun tabletPortrait753_isTheWideLayout() = assertWide(753, 1205)

    @Test
    fun tabletLandscape1205_isTheWideLayout() = assertWide(1205, 753)

    private fun assertWide(width: Int, height: Int) {
        show(width, height, dark = false, fontScale = 1f)
        compose.waitForTiles(30)
        val boxes = SectionKey.entries.map { node(TestTags.mapBox(it)).boundsInRoot }
        assertTrue("$width: 블록이 나란히(쌓지 않음)", boxes.map { it.left }.distinct().size > 1)
        assertNoHorizontalOverflow("태블릿 $width")
        assertTouchTargets("태블릿 $width")
    }
}
