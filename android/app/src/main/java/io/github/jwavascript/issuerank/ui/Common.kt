package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.data.IssueMap
import io.github.jwavascript.issuerank.data.Links
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.Strings
import io.github.jwavascript.issuerank.platform.AndroidStrings
import io.github.jwavascript.issuerank.platform.res
import kotlin.math.max

/** data 층 글 함수(DateLine · IssueMap …)에 넘길 Strings = 지금 화면 리소스 */
@Composable
fun rememberStrings(): Strings {
    val context = LocalContext.current
    return remember(context) { AndroidStrings(context) }
}

/** strings_web.xml 문구(키) */
@Composable
fun str(key: Str, vararg args: Any): String = stringResource(key.res, *args)

/**
 * 누를 곳(계획 5절 48dp): 누름 · 의미 칸 = 겉(minimumInteractiveComponentSize — 웹 32px 단추도 48dp 이상), 물결 = 속(웹 모양 shape).
 * 모양 · 바탕 · 여백은 이 뒤에 이어 씀. Compose 권장(Modifier.Node) 대신 @Composable인 이유: 겉 · 속이 같은 누름 상태를 나눠야 함.
 */
@Suppress("ComposableModifierFactory")
@Composable
fun Modifier.press(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    shape: Shape = RoundedCornerShape(8.dp),
    onClickLabel: String? = null,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)
        .minimumInteractiveComponentSize()
        .clip(shape)
        .indication(source, ripple())
}

/** 웹 SVG 아이콘(색 = currentColor 자리) */
@Composable
fun WebIcon(icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Image(icon, contentDescription = null, modifier = modifier, colorFilter = ColorFilter.tint(color))
}

/** 웹 .dot(섹션 점 7px — 범례는 모서리 2px) */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, shape: Shape = CircleShape) {
    Box(modifier.size(7.dp).clip(shape).background(color))
}

/** 웹 .wrap(760) · .wrap-wide(1240): 가운데 · 좌우 여백(640 이하 16) */
@Composable
fun Wrap(maxWidth: Dp, bp: Breakpoints, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(modifier.widthIn(max = maxWidth).fillMaxWidth().padding(horizontal = bp.gutter), content = content)
    }
}

const val WRAP = 760
const val WRAP_WIDE = 1240

/**
 * 웹 flex · space-between · flex-wrap(.ctx-row · .map-tools): 한 줄에 들어가면 왼쪽 · 오른쪽 끝(세로 맞춤 = bottom/center),
 * 안 들어가면 오른쪽이 다음 줄(왼쪽 맞춤). gap = 웹 gap(가로 · 세로 같음).
 */
@Composable
fun SpreadRow(gap: Dp, alignBottom: Boolean, left: @Composable () -> Unit, right: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(left, right), modifier = modifier) { (l, r), constraints ->
        val w = constraints.maxWidth
        val g = gap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val rp = r.first().measure(loose)
        val oneLine = l.first().maxIntrinsicWidth(constraints.maxHeight) + g + rp.width <= w
        val lp = l.first().measure(loose.copy(maxWidth = if (oneLine) max(0, w - g - rp.width) else w))
        if (oneLine) {
            val h = max(lp.height, rp.height)
            layout(w, h) {
                lp.place(0, if (alignBottom) h - lp.height else (h - lp.height) / 2)
                rp.place(w - rp.width, if (alignBottom) h - rp.height else (h - rp.height) / 2)
            }
        } else {
            layout(w, lp.height + g + rp.height) {
                lp.place(0, 0)
                rp.place(0, lp.height + g)
            }
        }
    }
}

/** 웹 .chip-btn(높이 32 · 모서리 8 · 테두리 · 13px 600) */
@Composable
fun ChipButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    Box(
        modifier.press(onClick).height(32.dp).border(1.dp, c.line, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = WebType.pill, color = c.text, maxLines = 1) }
}

/**
 * 웹 .score: 연관기사 수 칸. 바탕 = 섹션 색 w%(scoreWeight 10~36)를 실제 아래 바탕(under — 목록 · 북마크 카드 = bg, 상세 창 = surface)에 얹은 색(웹과 같은 색 —
 * 불투명으로 칠해 잰 색 = 그린 색). 글자(숫자 · "연관기사", 투명도 1) = 웹 color-mix(섹션 80%, text)에서 4.5가 될 만큼만 text 쪽으로(C11).
 */
@Composable
fun ScoreBox(section: SectionKey, articleCount: Int, maxCount: Int, width: Dp, height: Dp, showLabel: Boolean, under: Color, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    val weight = IssueMap.scoreWeight(articleCount, maxCount)
    val sectionArgb = c.section(section).toArgb()
    val underArgb = under.toArgb()
    val textArgb = c.text.toArgb()
    val bg = remember(sectionArgb, weight, underArgb) { Color(Contrast.scoreBackground(sectionArgb, weight, underArgb)) }
    val fg = remember(sectionArgb, textArgb, weight, underArgb) { Color(Contrast.scoreText(sectionArgb, textArgb, weight, underArgb)) }
    Column(
        modifier.size(width, height).clip(RoundedCornerShape(8.dp)).background(bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$articleCount", style = WebType.scoreNumber, color = fg, maxLines = 1)
        if (showLabel) Text(str(Str.RELATED_ARTICLES), style = WebType.scoreLabel, color = fg, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
    }
}

/** 웹 summaryHTML(672~674): 줄마다 앞 짧은 선 + 글, 요약 없으면 한 줄 "요약 없음 · 원문을 확인해 주세요"(기울임 — .summary.pending) */
@Composable
fun SummaryLines(summary: List<String>?, style: TextStyle, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    val lines = summary ?: listOf(str(Str.SUMMARY_NONE))
    val lineStyle = if (summary == null) style.copy(fontStyle = FontStyle.Italic) else style
    val dashTop = with(LocalDensity.current) { (style.fontSize * 0.72f).toDp() }
    Column(modifier) {
        for (line in lines) {
            Box {
                // li::before: 4×1px 선(top .72em) — 글자가 아닌 장식이라 faint 그대로
                Box(Modifier.padding(top = dashTop).size(4.dp, 1.dp).background(c.faint))
                Text(line, style = lineStyle, color = c.muted, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

/** 웹 .empty "해당 날짜의 랭킹 데이터가 없습니다"(emptyNoData 695) */
@Composable
fun EmptyNoData(onToday: () -> Unit, modifier: Modifier = Modifier) {
    EmptyBox(str(Str.EMPTY_NO_DATA_TITLE), str(Str.EMPTY_NO_DATA_BODY), modifier.testTag(TestTags.EMPTY_NO_DATA)) {
        ChipButton(str(Str.EMPTY_NO_DATA_ACTION), onToday, Modifier.testTag(TestTags.EMPTY_TODAY))
    }
}

/** 웹 .empty(가운데 · 72px 위아래, 제목 16 굵게 + 글 + 단추) — A4 북마크 빈 화면도 이것 */
@Composable
fun EmptyBox(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
    val c = WebTheme.colors
    Column(modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = WebType.emptyTitle, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 6.dp))
        Text(body, style = WebType.body, color = c.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        action()
    }
}

/** offline.html 안내(오프라인 — 랭킹 요청이 네트워크로 실패): 로고 · 제목 · 글 · "다시 시도" */
@Composable
fun OfflineNotice(onRetry: () -> Unit, bp: Breakpoints, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    Box(modifier.fillMaxWidth().testTag(TestTags.OFFLINE), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = if (bp.narrow) 48.dp else 72.dp, bottom = 56.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                WebIcon(WebIcons.brand, c.ink, Modifier.size(18.dp))
                Text(str(Str.BRAND), style = WebType.offlineBrand, color = c.muted)
            }
            Text(str(Str.OFFLINE_TITLE), style = WebType.offlineTitle, color = c.text, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            Text(str(Str.OFFLINE_NOTE), style = WebType.contextDesc.copy(lineHeight = WebType.summary.lineHeight), color = c.muted)
            Box(
                Modifier.padding(top = 20.dp).testTag(TestTags.OFFLINE_RETRY).press(onRetry).height(40.dp).background(c.ink).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(str(Str.RETRY), style = WebType.button, color = c.inkText) }
        }
    }
}

/** 웹 .footer(화면 끝 안내 12px) */
@Composable
fun Footer(text: String) {
    Text(
        text, style = WebType.small, color = WebTheme.colors.muted, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
    )
}

/** 웹 .site-foot(538): "이슈랭크 공유" · 개인정보처리방침 · 이용약관 · 문의(Custom Tab — 늘 배포 주소). 앱 설치 링크는 앱에 없음 */
@Composable
fun SiteFooter(actions: ScreenActions, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp).testTag(TestTags.SITE_FOOTER), horizontalAlignment = Alignment.CenterHorizontally) {
        FootLink(str(Str.SHARE_SITE), actions::shareSite, Modifier.testTag(TestTags.SHARE_SITE))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FootLink(str(Str.PRIVACY), { actions.openUrl(Links.PRIVACY) }, Modifier.testTag(TestTags.FOOTER_PRIVACY))
            Text(" · ", style = WebType.small, color = c.muted)
            FootLink(str(Str.TERMS), { actions.openUrl(Links.TERMS) }, Modifier.testTag(TestTags.FOOTER_TERMS))
            Text(" · ", style = WebType.small, color = c.muted)
            FootLink(str(Str.CONTACT), { actions.openUrl(Links.CONTACT) })
        }
    }
}

@Composable
private fun FootLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.press(onClick, shape = RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
        Text(text, style = WebType.small, color = WebTheme.colors.muted)
    }
}
