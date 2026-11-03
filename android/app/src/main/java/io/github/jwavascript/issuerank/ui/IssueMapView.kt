package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.IssueMap
import io.github.jwavascript.issuerank.data.MapBox
import io.github.jwavascript.issuerank.data.MapLayout
import io.github.jwavascript.issuerank.data.MapTile
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.Strings
import kotlin.math.floor
import kotlin.math.max

/**
 * 이슈맵(웹 view-map 500~510 · renderMap 757~768 · drawMap 770~828 · CSS 240~285). 사각형 = A2 IssueMap.layout(웹과 같은 입력 → 같은 사각형):
 * 지도 폭 = mapWidth(화면 폭 내림), 높이 = 화면 높이(웹 100vh). 타일 = 의미 노드 하나(이름 = 웹 aria-label "정치 1위 제목, 연관기사 n건" · tag "tile:<id>").
 */
@Composable
fun IssueMapBody(state: UiState, ranking: Ranking, bp: Breakpoints, viewportHeight: Float, actions: ScreenActions) {
    val s = rememberStrings()
    val layout = remember(ranking, state.zoom, bp.narrow, bp.width, viewportHeight) {
        IssueMap.layout(ranking, state.zoom, bp.narrow, IssueMap.mapWidth(bp.width.toDouble()), viewportHeight.toDouble())
    }
    SpreadRow(
        gap = 12.dp, alignBottom = false,
        modifier = Modifier.heightIn(min = 30.dp).padding(bottom = 10.dp),
        left = { Crumbs(layout, state.zoom, ranking, s, actions) },
        right = { Legend(actions) },
    )
    Box(Modifier.size(layout.width.dp, layout.height.dp).clip(RoundedCornerShape(10.dp)).testTag(TestTags.MAP)) {
        for (box in layout.boxes) {
            if (!box.hidden && box.w > 0 && box.h > 0) SectionBlock(box, state, s, actions)
        }
    }
}

/** 경로 줄(762~764): 확대면 "전체 섹션 / 경제 연관기사 n건", 아니면 "연관기사 합계 1,234건" */
@Composable
private fun Crumbs(layout: MapLayout, zoom: SectionKey?, ranking: Ranking, s: Strings, actions: ScreenActions) {
    val c = WebTheme.colors
    Row(Modifier.testTag(TestTags.CRUMBS), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (zoom != null) {
            Box(Modifier.press({ actions.zoom(null) }, shape = RoundedCornerShape(4.dp)).testTag(TestTags.CRUMB_ALL), contentAlignment = Alignment.Center) {
                Text(s[Str.MAP_ALL_SECTIONS], style = WebType.label13.copy(fontWeight = FontWeight.SemiBold), color = c.muted)
            }
            Text(s[Str.CRUMB_SEPARATOR], style = WebType.label13, color = c.muted)
            Text(s[zoom.label], style = WebType.label13.copy(fontWeight = FontWeight.Bold), color = c.text)
        }
        Text(IssueMap.crumbTotal(layout, zoom, ranking, s), style = WebType.label13, color = c.muted)
    }
}

/** 범례(765): 섹션 점 + 이름 — 누르면 그 섹션 확대 */
@Composable
private fun Legend(actions: ScreenActions) {
    val c = WebTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (key in SectionKey.entries) {
            Row(
                Modifier.press({ actions.zoom(key) }, shape = RoundedCornerShape(4.dp)).testTag(TestTags.legend(key)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Dot(c.section(key), shape = RoundedCornerShape(2.dp))
                Text(str(key.label), style = WebType.small, color = c.muted, maxLines = 1)
            }
        }
    }
}

/** 섹션 블록(group-box): 위 22dp 라벨(점 · 이름 · 합계 · "확대 →"/"← 전체") + 타일 */
@Composable
private fun SectionBlock(box: MapBox, state: UiState, s: Strings, actions: ScreenActions) {
    val c = WebTheme.colors
    Box(Modifier.offset(box.x.toFloat().dp, box.y.toFloat().dp).size(box.w.toFloat().dp, box.h.toFloat().dp).testTag(TestTags.mapBox(box.section))) {
        Row(
            Modifier.fillMaxWidth().height(IssueMap.LABEL_H.toFloat().dp)
                .clickable(role = Role.Button) { actions.zoom(box.zoomTarget) }
                .testTag(TestTags.mapLabel(box.section)).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Dot(c.section(box.section))
            Text(s[box.section.label], style = WebType.mapLabel, color = c.text, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            Text("${box.total}", style = WebType.mapLabel.copy(fontWeight = FontWeight.Medium), color = c.muted, maxLines = 1, softWrap = false)
            Spacer(Modifier.weight(1f))
            // 웹은 마우스를 올렸을 때만 보임(opacity 0 → 1) — 앱은 올림이 없어 늘 보임
            Text(s[box.arrow], style = WebType.mapLabel.copy(fontWeight = FontWeight.Medium), color = c.muted, maxLines = 1, softWrap = false)
        }
        for (t in box.tiles) {
            if (t.w > 0 && t.h > 0) Tile(t, state.selectedId == t.issue.id, state.isBookmarked(t.issue.id), s, actions)
        }
    }
}

/**
 * 타일(.tile): 바탕 = 섹션 색 + 흰 덧칠(연관기사가 적을수록 밝게)의 웹 합성색을 불투명 하나로(흰 글자 4.5 아래면 그만큼만 어둡게 — C11) ·
 * 제목(글자 크기 · 줄 수 = 웹 계산) · 아래 "n위 m건"(글자 투명도 1) · 북마크 표시.
 * 작은 타일(xs) = 가운데 "n위 · m건", 아주 작은 타일(xxs) = 글 없음. 고른 타일 = 안쪽 테두리(.sel). 기기 글자 배율이면 줄 수를 다시 셈(넘침 0).
 */
@Composable
private fun Tile(t: MapTile, selected: Boolean, saved: Boolean, s: Strings, actions: ScreenActions) {
    val c = WebTheme.colors
    val issue = t.issue
    val description = IssueMap.tileDescription(issue, s)
    val fontScale = LocalDensity.current.fontScale
    val open = { actions.openIssue(issue.id) }
    // 바탕 = 웹 칠(섹션 색 + 흰 덧칠 l) 하나를 불투명으로 — 흰 글자 4.5 아래면 그만큼만 어둡게(C11, Contrast.tileFill)
    val sectionArgb = c.section(issue.section).toArgb()
    val textArgb = c.tileText.toArgb()
    val fill = remember(sectionArgb, t.lightness, textArgb) { Color(Contrast.tileFill(sectionArgb, t.lightness, textArgb)) }
    Box(
        Modifier.offset(t.x.toFloat().dp, t.y.toFloat().dp).size(t.w.toFloat().dp, t.h.toFloat().dp)
            .clip(RoundedCornerShape(3.dp))
            .background(fill)
            .drawWithContent {
                drawContent()
                if (selected) {
                    // box-shadow: inset 0 0 0 2px var(--text), inset 0 0 0 3px var(--bg)
                    val w2 = 2.dp.toPx()
                    val w1 = 1.dp.toPx()
                    val r = CornerRadius(3.dp.toPx())
                    drawRoundRect(c.text, Offset(w2 / 2, w2 / 2), Size(size.width - w2, size.height - w2), r, Stroke(w2))
                    drawRoundRect(c.bg, Offset(w2 + w1 / 2, w2 + w1 / 2), Size(size.width - 2 * w2 - w1, size.height - 2 * w2 - w1), r, Stroke(w1))
                }
            }
            .clickable(onClick = open)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
                testTag = TestTags.tile(issue.id)
                onClick { open(); true }
            },
    ) {
        when {
            t.xxs -> Unit
            // 타일 글자는 모두 투명도 1(웹 .rk .7 · .n .9는 흐린 흰 글자라 4.5 불가 — C11)
            t.xs -> Text(
                IssueMap.tileXs(issue, s), style = WebType.tileFoot, color = c.tileText, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 5.dp, vertical = 4.dp),
            )
            else -> Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    issue.title, style = WebType.tileTitle.copy(fontSize = t.fontSize.toFloat().sp), color = c.tileText,
                    maxLines = titleLines(t, fontScale), overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(IssueMap.tileRank(issue, s), style = WebType.tileFoot, color = c.tileText, maxLines = 1)
                    Text(IssueMap.tileCount(issue, s), style = WebType.tileFoot, color = c.tileText, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    // 북마크 표시 = 그림(3:1 기준) — 웹 .95 그대로
                    if (saved) WebIcon(WebIcons.mark, c.tileText.copy(alpha = 0.95f), Modifier.size(10.dp, 12.dp))
                }
            }
        }
    }
}

/** 제목 줄 수: 글자 배율 1 = 웹 값(MapTile.lines), 그 밖 = 같은 식을 배율로(위아래 여백 16 + 아래 줄 · 간격 20 → 배율만큼) */
internal fun titleLines(t: MapTile, fontScale: Float): Int {
    if (fontScale == 1f) return t.maxLines
    val reserved = 36 + 20 * (fontScale - 1)
    return max(1.0, floor((t.h - reserved) / (t.fontSize * fontScale * 1.32))).toInt()
}
