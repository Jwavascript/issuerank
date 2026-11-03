package io.github.jwavascript.issuerank.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jwavascript.issuerank.Content
import io.github.jwavascript.issuerank.MainViewModel
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.View
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 앱 화면 뿌리(MainActivity · 계측 시험의 ComponentActivity가 씀): ViewModel 상태 → [IssueRankScreen], 토스트(한 번만) → Snackbar.
 * 뒤로: 이슈맵이 아니면 이슈맵으로(웹 뒤로가기 = 앞 해시 화면), 상세 창은 창이 먼저 닫음.
 */
@Composable
fun IssueRankApp(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val actions = remember(vm, context) { AppActions(vm, context) }
    val snackbar = remember { SnackbarHostState() }
    val strings = rememberStrings()
    LaunchedEffect(vm, strings) {
        vm.notices.collect { notice ->
            // 웹 toast(): 새 토스트가 앞 것을 바꿈(3.6초 → Snackbar Short 4초)
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val result = snackbar.showSnackbar(
                    message = notice.toast.message.text(strings),
                    actionLabel = notice.toast.action?.let { strings[it] },
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) notice.onAction?.invoke()
            }
        }
    }
    IssueRankTheme {
        BackHandler(enabled = state.view != View.MAP) { actions.selectView(View.MAP) }
        IssueRankScreen(state, actions, snackbar)
    }
}

/**
 * 화면 전체(상태만 받음 — 미리보기 · 시험 공용): 머리 + 탭 내용(이슈맵 · 목록 · 북마크) + 토스트 + 상세 창.
 * edge-to-edge: 머리 = 상태 표시줄 아래, 가로 = 안전 영역, 내용 끝 = 내비게이션 막대 위. 폭 구간 = 안전 영역 안 폭(dp 내림), 높이 = 창 − 시스템 막대(웹 100vh).
 */
@Composable
fun IssueRankScreen(state: UiState, actions: ScreenActions, snackbar: SnackbarHostState = remember { SnackbarHostState() }) {
    val c = WebTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg).testTag(TestTags.APP)) {
        val dir = LocalLayoutDirection.current
        val side = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
        val bars = WindowInsets.systemBars.asPaddingValues()
        val width = maxWidth - side.calculateLeftPadding(dir) - side.calculateRightPadding(dir)
        val bp = Breakpoints(floor(width.value).toInt())
        val viewportHeight = (maxHeight - bars.calculateTopPadding() - bars.calculateBottomPadding()).value
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            Header(state, bp, actions)
            val body = Modifier.weight(1f).fillMaxWidth()
            when (state.view) {
                View.MAP -> MapScreen(state, bp, viewportHeight, actions, body)
                View.LIST -> ListScreen(state, bp, actions, body)
                View.BOOKMARKS -> BookmarksScreen(state, bp, actions, body)
            }
        }
        // 토스트는 맨 위 층(웹 z-index 70 > 상세 50): 상세 창이 열려 있으면 창 안에서 보임(IssueDetailSheet)
        if (state.selected == null) {
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            ) { ToastPill(it) }
        }
        IssueDetailSheet(state, actions, snackbar)
        // 창(따로 창 — 연 순서대로 위): 폴더 고르기(상세 창 위에서도) · 폴더 삭제 확인 · 로그인
        FolderPickerDialog(state, actions)
        FolderDeleteDialog(state, actions)
        AuthDialog(state.auth, actions)
    }
}

/** 이슈맵 화면(웹 view-map): 날짜 줄 · 경로 줄 · 범례 · 지도(또는 없는 날 안내) · 안내 글 · 맨 아래 */
@Composable
private fun MapScreen(state: UiState, bp: Breakpoints, viewportHeight: Float, actions: ScreenActions, modifier: Modifier) {
    val pane = str(Str.TAB_MAP)
    Column(modifier.verticalScroll(rememberScrollState()).semantics { paneTitle = pane }.testTag(TestTags.VIEW_MAP)) {
        if (state.content == Content.OFFLINE) {
            OfflineNotice(actions::retry, bp)
        } else {
            Wrap(WRAP_WIDE.dp, bp) { DateContext(state, bp, actions) }
            Wrap(WRAP_WIDE.dp, bp) {
                val ranking = state.ranking
                when {
                    ranking != null -> IssueMapBody(state, ranking, bp, viewportHeight, actions)
                    state.content == Content.NO_DATA -> EmptyNoData(actions::today)
                }
                Footer(str(Str.MAP_FOOTER))
            }
            SiteFooter(actions)
        }
        BottomInset()
    }
}

/** 랭킹 목록 화면(웹 view-list): 날짜 줄 · 섹션 탭(머리 밑에 붙음) · 요약 펼치기 · 섹션별 카드(또는 없는 날 안내) · 안내 글 · 맨 아래 */
@Composable
private fun ListScreen(state: UiState, bp: Breakpoints, actions: ScreenActions, modifier: Modifier) {
    val pane = str(Str.CD_LIST)
    val scroll = rememberScrollState()
    var tabsTop by remember { mutableFloatStateOf(0f) }
    Column(modifier.verticalScroll(scroll).semantics { paneTitle = pane }.testTag(TestTags.VIEW_LIST)) {
        if (state.content == Content.OFFLINE) {
            OfflineNotice(actions::retry, bp)
        } else {
            Wrap(WRAP_WIDE.dp, bp) { DateContext(state, bp, actions) }
            // .tabs { position: sticky; top: header } — 스크롤이 탭 위치를 넘으면 그만큼 내려 그림
            SectionTabs(
                state.section, actions,
                Modifier.align(Alignment.CenterHorizontally).widthIn(max = WRAP.dp).fillMaxWidth().padding(horizontal = bp.gutter)
                    .onPlaced { tabsTop = it.positionInParent().y }
                    .offset { IntOffset(0, max(0f, scroll.value - tabsTop).roundToInt()) }
                    .zIndex(1f),
            )
            Wrap(WRAP.dp, bp) {
                SummaryToggle(state.expanded, actions)
                val ranking = state.ranking
                when {
                    ranking != null -> RankingGroups(ranking, state.section, state.expanded, bp, state::isBookmarked, actions)
                    state.content == Content.NO_DATA -> EmptyNoData(actions::today)
                }
                Footer(str(Str.LIST_FOOTER))
            }
            SiteFooter(actions)
        }
        BottomInset()
    }
}

/** 북마크 화면(웹 view-bookmarks): BookmarksContent + 맨 아래. 날짜 줄 없음(웹 contextWrap.hidden) */
@Composable
private fun BookmarksScreen(state: UiState, bp: Breakpoints, actions: ScreenActions, modifier: Modifier) {
    val pane = str(Str.BOOKMARKS)
    Column(modifier.verticalScroll(rememberScrollState()).semantics { paneTitle = pane }.testTag(TestTags.VIEW_BOOKMARKS)) {
        Wrap(WRAP.dp, bp) { BookmarksContent(state, bp, actions) }
        SiteFooter(actions)
        BottomInset()
    }
}

/** 내용 끝 = 내비게이션 막대 위(edge-to-edge) */
@Composable
private fun BottomInset() {
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

/** 토스트(웹 .toast): 진한 알약 · 글 + (있으면) 밑줄 단추 하나 */
@Composable
internal fun ToastPill(data: SnackbarData) {
    val c = WebTheme.colors
    val action = data.visuals.actionLabel
    Row(
        Modifier.testTag(TestTags.TOAST).clip(RoundedCornerShape(percent = 50)).background(c.ink)
            .padding(start = 16.dp, end = if (action != null) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            data.visuals.message, style = WebType.toast, color = c.inkText,
            modifier = Modifier.weight(1f, fill = false).padding(vertical = 10.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (action != null) {
            Box(Modifier.press({ data.performAction() }, shape = RoundedCornerShape(4.dp)).testTag(TestTags.TOAST_ACTION).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                Text(action, style = WebType.toast.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline), color = c.inkText)
            }
        }
    }
}
