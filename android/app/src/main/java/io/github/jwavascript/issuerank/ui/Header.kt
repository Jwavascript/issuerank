package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.View
import kotlin.math.max
import kotlin.math.min

private val PILL = RoundedCornerShape(percent = 50)

/**
 * 머리(index.html 436~468 · CSS 76~126 · 반응 383~402): 로고(→ 이슈맵) · 보기 전환(이슈맵 · 랭킹) · 북마크(세 번째 탭) · 계정.
 * 웹 grid 1fr auto 1fr(가운데 = 화면 가운데), 640 이하 auto 1fr auto(가운데 = 남은 칸 가운데). 알림 종 · 앱 설치는 앱에 없음(v1).
 */
@Composable
fun Header(state: UiState, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    Box(
        Modifier.fillMaxWidth().background(c.bg)
            .drawBehind { drawLine(c.line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.statusBars).height(56.dp).testTag(TestTags.HEADER),
        contentAlignment = Alignment.TopCenter,
    ) {
        val gap = when { bp.tiny -> 8.dp; bp.narrow -> 10.dp; else -> 16.dp }
        HeaderLayout(
            narrow = bp.narrow, gap = gap,
            modifier = Modifier.widthIn(max = WRAP_WIDE.dp).fillMaxWidth().fillMaxHeight().padding(horizontal = bp.gutter),
            left = { Brand(bp, actions) },
            center = { ViewSwitch(state.view, bp, actions) },
            right = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (bp.belowMedium) 6.dp else 8.dp)) {
                    BookmarkNav(state, bp, actions)
                    if (!bp.narrow) AccountButton(state, bp, actions)
                }
            },
        )
    }
}

/** 세 칸: 넓으면 가운데 칸을 화면 가운데에(양옆과 겹치면 사이로), 좁으면 양옆 사이 가운데에 */
@Composable
private fun HeaderLayout(
    narrow: Boolean,
    gap: Dp,
    modifier: Modifier,
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Layout(contents = listOf(left, center, right), modifier = modifier) { (l, m, r), constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val g = gap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val lp = l.first().measure(loose)
        val rp = r.first().measure(loose)
        val room = max(0, w - lp.width - rp.width - 2 * g)
        val mp = m.first().measure(loose.copy(maxWidth = room))
        val minX = lp.width + g
        val maxX = w - rp.width - g - mp.width
        val mx = if (narrow) minX + (room - mp.width) / 2 else min(max((w - mp.width) / 2, minX), max(minX, maxX))
        layout(w, h) {
            lp.place(0, (h - lp.height) / 2)
            mp.place(mx, (h - mp.height) / 2)
            rp.place(w - rp.width, (h - rp.height) / 2)
        }
    }
}

/** 로고 + "이슈랭크"(640 이하 글자 숨김) — 누르면 이슈맵(웹 href #map), 이름 = "이슈랭크 홈"(aria-label) */
@Composable
private fun Brand(bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val label = str(Str.CD_HOME)
    Row(
        Modifier.press({ actions.selectView(View.MAP) }, role = null, shape = RoundedCornerShape(4.dp))
            .semantics { contentDescription = label }.testTag(TestTags.BRAND),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        WebIcon(WebIcons.brand, c.ink, Modifier.size(18.dp))
        if (!bp.narrow) Text(str(Str.BRAND), style = WebType.brand, color = c.text, maxLines = 1)
    }
}

/**
 * 보기 전환(웹 .switch role=tablist "보기 전환"): 이슈맵 · 랭킹, 고른 탭 = 바탕 알약 + 글자 진하게. 북마크 화면이면 알약 없음(.switch.off).
 * 누름 칸: 알약(약 34dp)이 머리 높이 56 안에서 Compose 기본 누름 넓힘으로 48dp(겉 칸을 키우면 웹 틀이 커져서 — 노트 04).
 */
@Composable
private fun ViewSwitch(view: View, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val label = str(Str.CD_VIEW_SWITCH)
    Row(
        Modifier.semantics { contentDescription = label }.selectableGroup()
            .clip(PILL).background(c.hover).border(1.dp, c.line, PILL).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwitchTab(View.MAP, WebIcons.map, str(Str.TAB_MAP), view, bp, actions, TestTags.TAB_MAP)
        SwitchTab(View.LIST, WebIcons.list, str(Str.TAB_LIST), view, bp, actions, TestTags.TAB_LIST)
    }
}

@Composable
private fun SwitchTab(target: View, icon: ImageVector, text: String, view: View, bp: Breakpoints, actions: ScreenActions, tag: String) {
    val c = WebTheme.colors
    val on = view == target
    val padX = when { bp.tiny -> 10.dp; bp.narrow -> 12.dp; else -> 16.dp }
    Row(
        Modifier.clip(PILL).then(if (on) Modifier.background(c.surface) else Modifier)
            .clickable(role = Role.Tab) { actions.selectView(target) }
            .semantics { selected = on }.testTag(tag)
            .padding(horizontal = padX, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val color = if (on) c.text else c.muted
        // 아이콘 .8(웹 .switch svg) = 이름 글자 옆 그림 — 3:1 이상(ThemeContrastTest)
        WebIcon(icon, color, Modifier.size(13.dp).alpha(0.8f))
        Text(text, style = WebType.tab, color = color, maxLines = 1)
    }
}

/** 북마크(웹 #bmNav — 세 번째 탭): 아이콘 + "북마크"(1199 이하 숨김) + 개수(로그인 · 1개 이상). 북마크 화면이면 진한 알약(aria-current) */
@Composable
private fun BookmarkNav(state: UiState, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val current = state.view == View.BOOKMARKS
    val label = str(Str.BOOKMARKS)
    val fg = if (current) c.inkText else c.muted
    Row(
        Modifier.press({ actions.selectView(View.BOOKMARKS) }, shape = PILL)
            .semantics { contentDescription = label; selected = current }.testTag(TestTags.TAB_BOOKMARKS)
            .height(32.dp)
            .then(if (current) Modifier.background(c.ink) else Modifier)
            .border(1.dp, if (current) c.ink else c.line, PILL)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WebIcon(WebIcons.bookmarkNav, fg, Modifier.size(14.dp))
        if (!bp.belowWide) Text(label, style = WebType.pill, color = fg, maxLines = 1)
        state.bookmarkBadge?.let { Text("$it", style = WebType.small, color = countColor(fg, if (current) c.ink else c.bg), maxLines = 1) }
    }
}

/** 개수(웹 .pill-btn .cnt · .chip .cnt opacity .7): 아래 바탕(진한 알약 · bg) 위 합성색이 4.5 아래면 그만큼만 진하게(C11 — 알파를 올리는 것과 같음) */
@Composable
internal fun countColor(fg: Color, under: Color): Color {
    val f = fg.toArgb()
    val u = under.toArgb()
    return remember(f, u) { Color(Contrast.fadedText(f, 0.7, u)) }
}

/** 계정(웹 #userBtn — 640 이하 숨김): "로그인" / "{이름} · 로그아웃"(화면 폭 48% 넘으면 …). 누름 = 로그인 창 · 로그아웃 */
@Composable
private fun AccountButton(state: UiState, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val text = state.user?.let { str(Str.USER_LOGOUT, it.name) } ?: str(Str.LOGIN)
    val maxWidth = (bp.width * 0.48f).dp
    Box(
        Modifier.press(actions::account, shape = PILL).testTag(TestTags.ACCOUNT)
            .height(32.dp).widthIn(max = maxWidth).border(1.dp, c.line, PILL).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = WebType.pill, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}
