package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str

/**
 * 섹션 탭(웹 #secTabs role=tablist "섹션" — 전체 + 6섹션 점 · 이름, 넘치면 가로 밀기). 목록 화면에서 머리 밑에 붙음(sticky — 화면이 offset으로).
 */
@Composable
fun SectionTabs(section: SectionKey?, actions: ScreenActions, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    val label = str(Str.CD_SECTIONS)
    Row(
        modifier.background(c.bg)
            .drawBehind { drawLine(c.line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = label }.selectableGroup().testTag(TestTags.SECTION_TABS),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionTab(null, str(Str.ALL), section, actions)
        for (key in SectionKey.entries) SectionTab(key, str(key.label), section, actions)
    }
}

@Composable
private fun SectionTab(key: SectionKey?, name: String, current: SectionKey?, actions: ScreenActions) {
    val c = WebTheme.colors
    val on = key == current
    Row(
        Modifier.press({ actions.selectSection(key) }, role = Role.Tab, shape = RectangleShape)
            .semantics { selected = on }.testTag(TestTags.sectionTab(key))
            .drawBehind { if (on) drawRect(c.text, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx())) }
            .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (key != null) Dot(c.section(key))
        Text(name, style = WebType.tab, color = if (on) c.text else c.muted, maxLines = 1)
    }
}

/**
 * 웹 .list-tools · .toggle(28×16 막대 + "요약 펼치기", 기본 켬) — 끄면 요약 · 원문 · 공유 숨김(.compact).
 * 끔 상태 막대 = 웹 line(바탕 대비 1.18 · 1.31)을 바탕 · 손잡이와 3:1이 될 만큼만 진하게(C11 ⑧ — Contrast.toggleTrackOff), 켬(ink) · 손잡이(surface)는 웹 그대로.
 */
@Composable
fun SummaryToggle(expanded: Boolean, actions: ScreenActions) {
    val c = WebTheme.colors
    val line = c.line.toArgb()
    val text = c.text.toArgb()
    val bg = c.bg.toArgb()
    val knob = c.surface.toArgb()
    val trackOff = remember(line, text, bg, knob) { Color(Contrast.toggleTrackOff(line, text, bg, knob)) }
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.CenterEnd) {
        Row(
            Modifier.press({ actions.setExpanded(!expanded) }, role = Role.Switch, shape = RoundedCornerShape(4.dp))
                .semantics { toggleableState = ToggleableState(expanded) }.testTag(TestTags.SUMMARY_TOGGLE),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(28.dp, 16.dp).clip(RoundedCornerShape(99.dp)).background(if (expanded) c.ink else trackOff)) {
                Box(Modifier.offset(x = if (expanded) 14.dp else 2.dp, y = 2.dp).size(12.dp).clip(RoundedCornerShape(50)).background(c.surface))
            }
            Text(str(Str.SUMMARY_TOGGLE), style = WebType.label13, color = c.muted)
        }
    }
}

/** 웹 renderList(837~847): 섹션마다 머리(점 · 이름 · "Top 5") + 카드 5개 */
@Composable
fun RankingGroups(ranking: Ranking, section: SectionKey?, expanded: Boolean, bp: Breakpoints, isBookmarked: (String) -> Boolean, actions: ScreenActions) {
    val c = WebTheme.colors
    val keys = if (section == null) SectionKey.entries else listOf(section)
    val max = ranking.maxArticleCount
    for (key in keys) {
        Column(Modifier.padding(top = 26.dp).testTag(TestTags.group(key))) {
            Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.alignByBaseline(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(c.section(key))
                    Text(str(key.label), style = WebType.groupHead, color = c.text)
                }
                Text(str(Str.TOP5), style = WebType.small, color = c.muted, modifier = Modifier.alignByBaseline())
            }
            ranking.issuesOf(key).forEachIndexed { i, issue ->
                IssueCard(issue, max, isBookmarked(issue.id), expanded, bp, first = i == 0, actions = actions)
            }
        }
    }
}

/**
 * 카드 한 장(웹 itemHTML 677~694 — 목록 · (A4) 북마크 화면 공용): 숫자 칸 | 순위(meta) · 제목(→ 원문) · 3줄 요약 · "기사 원문" · "공유" | 북마크.
 * 칸 52 · 1fr · 36(640 이하 44 · 1fr · 32), 요약 접으면 요약 · 원문 · 공유를 숨기고 줄임. meta = 북마크 화면은 "정치 1위 · 폴더 · n분 전 저장"(A4).
 */
@Composable
fun IssueCard(
    issue: Issue,
    maxCount: Int,
    bookmarked: Boolean,
    expanded: Boolean,
    bp: Breakpoints,
    first: Boolean,
    actions: ScreenActions,
    /** 카드가 놓인 실제 바탕(숫자 칸 대비를 이 위에서 잼 — 목록 · 북마크 화면 = bg) */
    scoreUnder: Color = WebTheme.colors.bg,
    meta: @Composable RowScope.() -> Unit = { RankMeta(issue) },
) {
    val c = WebTheme.colors
    val topLine = if (first) c.text else c.line
    Row(
        Modifier.fillMaxWidth()
            .drawBehind { drawLine(topLine, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx()) }
            .padding(vertical = if (expanded) 16.dp else 11.dp)
            .testTag(TestTags.card(issue.id)),
        horizontalArrangement = Arrangement.spacedBy(if (bp.narrow) 10.dp else 14.dp),
    ) {
        ScoreBox(issue.section, issue.articleCount, maxCount, if (bp.narrow) 44.dp else 52.dp, if (expanded) 40.dp else 32.dp, showLabel = expanded, under = scoreUnder)
        Column(Modifier.weight(1f)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically, content = { meta() })
            Text(
                issue.title, style = WebType.cardTitle, color = c.text,
                modifier = Modifier.padding(top = 2.dp).clickable { actions.openUrl(issue.url) }.testTag(TestTags.cardTitle(issue.id)),
            )
            if (expanded) {
                SummaryLines(issue.summary, WebType.summary, Modifier.padding(top = 8.dp).testTag(TestTags.cardSummary(issue.id)))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SourceLink(str(Str.ORIGINAL_ARTICLE), true, null, { actions.openUrl(issue.url) }, Modifier.testTag(TestTags.cardOriginal(issue.id)))
                    SourceLink(str(Str.SHARE), false, str(Str.CD_SHARE_ISSUE), { actions.share(issue) }, Modifier.testTag(TestTags.cardShare(issue.id)))
                }
            }
        }
        BookmarkButton(bookmarked, if (bp.narrow) 32.dp else 36.dp, { actions.bookmark(issue) }, Modifier.testTag(TestTags.cardBookmark(issue.id)))
    }
}

/** 목록 카드 meta: "1위"(.sec — muted 600) */
@Composable
fun RankMeta(issue: Issue) {
    Text(str(Str.RANK_N, issue.rank), style = WebType.small600, color = WebTheme.colors.muted)
}

/** 웹 .src: "기사 원문 ↗"(링크) · "공유"(단추 — 이름 "이 이슈 공유") */
@Composable
private fun SourceLink(text: String, external: Boolean, label: String?, onClick: () -> Unit, modifier: Modifier) {
    val c = WebTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(4.dp)).clickable(role = if (external) null else Role.Button, onClick = onClick)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, style = WebType.small600, color = c.muted)
        if (external) WebIcon(WebIcons.external, c.muted, Modifier.size(11.dp))
    }
}

/** 웹 .bm(32×32): 끔 = 선(muted), 켬 = 채움(text) · 이름 "북마크" / "북마크 해제"(aria-label) · 눌림 상태(aria-pressed) */
@Composable
fun BookmarkButton(on: Boolean, columnWidth: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    val label = str(if (on) Str.CD_UNBOOKMARK else Str.BOOKMARKS)
    Box(Modifier.width(columnWidth), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Checkbox, onClick = onClick)
                .semantics { contentDescription = label; toggleableState = ToggleableState(on) },
            contentAlignment = Alignment.Center,
        ) { WebIcon(if (on) WebIcons.bookmarkOn else WebIcons.bookmark, if (on) c.text else c.muted, Modifier.size(17.dp)) }
    }
}
