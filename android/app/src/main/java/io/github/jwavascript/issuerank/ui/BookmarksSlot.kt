package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.Bookmark
import io.github.jwavascript.issuerank.data.BookmarkView
import io.github.jwavascript.issuerank.data.Folder
import io.github.jwavascript.issuerank.data.FolderChip
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.Strings
import io.github.jwavascript.issuerank.data.View
import java.time.Clock
import kotlin.math.roundToInt

/** 북마크 카드 "n분 전 저장"의 지금 시각(웹 timeAgo — 그릴 때 Date.now()). 미리보기 = 고정 시계 */
internal val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemUTC() }

/**
 * 북마크 화면 내용(웹 view-bookmarks 524~536 · renderBookmarks 850~892): 머리 "북마크" + (640 이하 · 로그인) "{이름} · 로그아웃" + "저장한 기사 n개 · 최근 저장 순" ·
 * 폴더 칩(전체 + 폴더 · 개수, 폴더를 고르면 줄 끝 "폴더 삭제") · 이어진 같은 날짜 묶음("11월 3일 (화) 랭킹" n) 카드(meta = "정치 1위 · 폴더 · n분 전 저장") ·
 * 빈 화면("북마크한 기사가 없습니다" + "이슈맵 보러 가기") · 비로그인 안내 · 안내 글. 카드 = 목록 카드(IssueCard — 제목 · 기사 원문 = 원문, 공유, 북마크 = 해제).
 */
@Composable
fun BookmarksContent(state: UiState, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val user = state.user
    Column(Modifier.fillMaxWidth().padding(top = if (bp.narrow) 24.dp else 32.dp, bottom = 14.dp)) {
        Text(str(Str.BOOKMARKS), style = if (bp.narrow) WebType.h1Narrow else WebType.h1, color = c.text, modifier = Modifier.semantics { heading() })
        // 640 이하는 머리 계정 단추가 숨어 여기에 같은 글 · 동작(웹 bm-user — O2 ⑨)
        if (user != null && bp.narrow) BookmarksUser(user.name, actions)
        if (user != null) {
            Text(
                str(Str.BM_SUB, state.bookmarks.size), style = WebType.contextDesc, color = c.muted,
                modifier = Modifier.padding(top = 4.dp).testTag(TestTags.BM_SUB),
            )
        }
    }
    if (user == null) {
        EmptyBox(str(Str.LOGIN_REQUIRED), str(Str.BM_LOGIN_BODY)) {
            ChipButton(str(Str.LOGIN), actions::login, Modifier.testTag(TestTags.BOOKMARKS_LOGIN))
        }
    } else {
        FolderChips(state, actions)
        val items = BookmarkView.items(state.bookmarks, state.effectiveFilter)
        if (items.isEmpty()) {
            EmptyBox(str(Str.BM_EMPTY_TITLE), str(Str.BM_EMPTY_BODY), Modifier.testTag(TestTags.BM_EMPTY)) {
                ChipButton(str(Str.BM_EMPTY_ACTION), { actions.selectView(View.MAP) }, Modifier.testTag(TestTags.BM_EMPTY_ACTION))
            }
        } else {
            BookmarkGroups(state, items, bp, actions)
        }
    }
    Footer(str(Str.BM_FOOTER))
}

/** .bm-user(높이 30 · 알약 · 테두리 · 13px 600 muted · 넘치면 …): 누르면 로그아웃 */
@Composable
private fun BookmarksUser(name: String, actions: ScreenActions) {
    val c = WebTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    Box(
        Modifier.padding(top = 8.dp).press(actions::signOut, shape = shape).testTag(TestTags.BM_USER)
            .height(30.dp).border(1.dp, c.line, shape).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(str(Str.USER_LOGOUT, name), style = WebType.pill, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/**
 * .chips(가로 밀기 · gap 6 · 위 4 아래 2) — role=group "폴더". 폴더 칩을 고르면 줄 끝 "폴더 삭제"(웹 sticky right: 칩이 다 들어가면 마지막 칩 바로 뒤,
 * 넘치면 오른쪽 끝에 붙고 칩 줄이 그 앞에서 밀림). 고른 칩이 "폴더 삭제"에 가리면 칩 줄만 밀어 다 보이게(웹 869~873 — 페이지 세로 이동 0),
 * 폴더가 지워지면 칩 줄을 맨 앞으로(웹 1086).
 */
@Composable
private fun FolderChips(state: UiState, actions: ScreenActions) {
    val filter = state.effectiveFilter
    val chips = BookmarkView.chips(state.bookmarks, state.folders)
    val picked = BookmarkView.picked(filter, state.folders)
    val scroll = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    val rights = remember { mutableStateMapOf<String, Int>() }
    val folderIds = state.folders.map { it.id }
    LaunchedEffect(folderIds) { scroll.scrollTo(0) }
    LaunchedEffect(picked?.id, viewport) {
        val right = picked?.let { rights[it.id] } ?: return@LaunchedEffect
        val over = right - (scroll.value + viewport)
        if (over > 0) scroll.scrollTo(scroll.value + over)
    }
    val label = str(Str.CD_FOLDERS)
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp).semantics { contentDescription = label }.testTag(TestTags.FOLDER_CHIPS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f, fill = false).onSizeChanged { viewport = it.width }.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (chip in chips) {
                FolderChipButton(
                    chip, chip.id == filter, actions,
                    Modifier.onPlaced { rights[chip.id] = (it.positionInParent().x + it.size.width).roundToInt() },
                )
            }
        }
        if (picked != null) {
            Spacer(Modifier.width(6.dp))
            FolderDeleteButton(picked, actions)
        }
    }
}

/** .chip(높이 30 · 알약 · 13px 600 muted, 고름 = 진한 바탕 · ink-text): 이름 + 개수(.cnt 500 · .7 → 4.5가 될 만큼만 진하게 — C11, 머리 개수와 같음) */
@Composable
private fun FolderChipButton(chip: FolderChip, on: Boolean, actions: ScreenActions, modifier: Modifier) {
    val c = WebTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    val fg = if (on) c.inkText else c.muted
    Row(
        modifier.press({ actions.selectFolder(chip.id) }, shape = shape).semantics { selected = on }.testTag(TestTags.folderChip(chip.id))
            .height(30.dp).then(if (on) Modifier.background(c.ink) else Modifier).border(1.dp, if (on) c.ink else c.line, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(chip.name ?: str(Str.ALL), style = WebType.pill, color = fg, maxLines = 1)
        Text("${chip.count}", style = WebType.pill.copy(fontWeight = FontWeight.Medium), color = countColor(fg, if (on) c.ink else c.bg), maxLines = 1)
    }
}

/** .chip-del(높이 30 · 모서리 8 = 고르는 칩이 아니라 동작 · 바탕 bg · 13px 600 muted) — 이름 "‘폴더’ 폴더 삭제"(aria-label) */
@Composable
private fun FolderDeleteButton(folder: Folder, actions: ScreenActions) {
    val c = WebTheme.colors
    val label = str(Str.CD_FOLDER_DELETE, folder.name)
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.press({ actions.requestFolderDelete(folder.id) }, shape = shape).semantics { contentDescription = label }.testTag(TestTags.FOLDER_DELETE)
            .height(30.dp).background(c.bg, shape).border(1.dp, c.line, shape).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(str(Str.FOLDER_DELETE), style = WebType.pill, color = c.muted, maxLines = 1) }
}

/** 날짜 묶음(웹 881~891): 최근 저장 순을 지키며 이어진 같은 카드 날짜만 한 머리("11월 3일 (화) 랭킹" + 개수) · 카드 숫자 칸 세기 = 받은 북마크 최대(bmMax) */
@Composable
private fun BookmarkGroups(state: UiState, items: List<Bookmark>, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val s = rememberStrings()
    val now = LocalClock.current.millis()
    val max = BookmarkView.maxArticleCount(state.bookmarks)
    BookmarkView.groups(items).forEachIndexed { index, group ->
        Column(Modifier.padding(top = 26.dp).testTag(TestTags.bookmarkGroup(index))) {
            Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s[Str.RANKING_OF_DATE, KstDate.fmtDate(group.date, s)], style = WebType.groupHead, color = c.text, modifier = Modifier.alignByBaseline())
                Text("${group.items.size}", style = WebType.small, color = c.muted, modifier = Modifier.alignByBaseline())
            }
            group.items.forEachIndexed { i, b ->
                IssueCard(
                    b.issue, max, state.isBookmarked(b.id), expanded = true, bp, first = i == 0, actions = actions,
                    meta = { BookmarkMeta(b, state.folders, now, s) },
                )
            }
        }
    }
}

/** 북마크 카드 meta(웹 itemHTML saved): "정치 1위"(600) · "·" · 폴더 이름 · 오른쪽 끝 "n분 전 저장"(.saved-at margin-left auto) — 글자색 muted(C11 ①) */
@Composable
private fun BookmarkMeta(b: Bookmark, folders: List<Folder>, nowMs: Long, s: Strings) {
    val c = WebTheme.colors
    Row(Modifier.fillMaxWidth().testTag(TestTags.bookmarkMeta(b.id)), verticalAlignment = Alignment.CenterVertically) {
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(s[Str.SECTION_RANK, s[b.issue.section.label], b.issue.rank], style = WebType.small600, color = c.muted)
            Text(s[Str.META_SEPARATOR], style = WebType.small, color = c.muted)
            Text(BookmarkView.folderName(folders, b.folderId), style = WebType.small, color = c.muted)
        }
        Text(s[Str.SAVED_AGO, KstDate.timeAgo(b.savedAt, nowMs, s)], style = WebType.small, color = c.muted, modifier = Modifier.padding(start = 6.dp))
    }
}
