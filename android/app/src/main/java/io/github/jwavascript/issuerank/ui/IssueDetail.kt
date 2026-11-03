package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.DateLine
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.Str
import kotlinx.coroutines.launch

/**
 * 이슈 상세(웹 openDrawer 915~942 — 640 이하 아래 창, 넓으면 오른쪽 서랍 → 앱은 늘 ModalBottomSheet, 계획 1절).
 * 열림 = UiState.selectedId(지금 랭킹에 있을 때). 닫기 = X · 바깥 · 뒤로 · 아래로 끌기.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailSheet(state: UiState, actions: ScreenActions, snackbar: SnackbarHostState) {
    val issue = state.selected ?: return
    val ranking = state.ranking ?: return
    val c = WebTheme.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = actions::closeIssue,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = c.surface,
        contentColor = c.text,
        scrimColor = ScrimColor,
        dragHandle = null,
    ) {
        Box {
            IssueDetailContent(issue, ranking, state.isBookmarked(issue.id), actions) {
                scope.launch { sheet.hide() }.invokeOnCompletion { actions.closeIssue() }
            }
            // 창이 열려 있는 동안 토스트는 창 위에(창 밖 Snackbar는 창 아래 층이라 가려짐)
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) { ToastPill(it) }
        }
    }
}

/** 상세 내용(미리보기 · 시험이 따로 그릴 수 있게 창과 나눔) */
@Composable
fun IssueDetailContent(issue: Issue, ranking: Ranking, bookmarked: Boolean, actions: ScreenActions, onClose: () -> Unit) {
    val c = WebTheme.colors
    val s = rememberStrings()
    val title = str(Str.CD_ISSUE_DETAIL)
    Column(Modifier.fillMaxWidth().semantics { paneTitle = title }.testTag(TestTags.DETAIL)) {
        // drawer-top: 점 · 섹션 · "11월 3일 (화) 랭킹" | 닫기
        Row(
            Modifier.fillMaxWidth().drawBehind { drawLine(c.line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
                .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                Modifier.weight(1f).testTag(TestTags.DETAIL_META),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Dot(c.section(issue.section))
                Text(s[issue.section.label], style = WebType.small600, color = c.muted)
                Text(s[Str.META_SEPARATOR], style = WebType.small, color = c.muted)
                Text(s[Str.RANKING_OF_DATE, KstDate.fmtDate(issue.date, s)], style = WebType.small, color = c.muted)
            }
            IconBox(WebIcons.close, s[Str.CLOSE], true, onClose, Modifier.testTag(TestTags.DETAIL_CLOSE))
        }
        // drawer-body
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreBox(issue.section, issue.articleCount, ranking.maxArticleCount, 56.dp, 44.dp, showLabel = true, under = c.surface, modifier = Modifier.alignByBaseline())
                Text(s[Str.SECTION_RANK, s[issue.section.label], issue.rank], style = WebType.small600, color = c.muted, modifier = Modifier.alignByBaseline())
            }
            Text(issue.title, style = WebType.detailTitle, color = c.text, modifier = Modifier.padding(top = 14.dp).testTag(TestTags.DETAIL_TITLE))
            SummaryLines(issue.summary, WebType.detailSummary, Modifier.padding(top = 16.dp).testTag(TestTags.DETAIL_SUMMARY))
            // .facts(grid auto 1fr · 간격 8 16): 연관기사 · 섹션 순위 · 수집(대체 섹션은 날짜만)
            Row(
                Modifier.padding(top = 22.dp).fillMaxWidth()
                    .drawBehind { drawLine(c.line, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx()) }
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (label in listOf(Str.RELATED_ARTICLES, Str.DETAIL_RANK_LABEL, Str.DETAIL_COLLECTED)) {
                        Text(s[label], style = WebType.label13, color = c.muted, maxLines = 1)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s[Str.COUNT_N, issue.articleCount], style = WebType.label13, color = c.text, maxLines = 1, modifier = Modifier.testTag(TestTags.DETAIL_COUNT))
                    Text(s[Str.DETAIL_RANK_VALUE, s[issue.section.label], issue.rank], style = WebType.label13, color = c.text, maxLines = 1, modifier = Modifier.testTag(TestTags.DETAIL_RANK))
                    Text(DateLine.collected(issue, ranking, s), style = WebType.label13, color = c.text, maxLines = 1, modifier = Modifier.testTag(TestTags.DETAIL_COLLECTED))
                }
            }
        }
        // drawer-foot: 기사 원문(진한 단추) · 북마크 · 공유 — 글자 배율이 크면 다음 줄로(넘침 0)
        FlowRow(
            Modifier.fillMaxWidth().drawBehind { drawLine(c.line, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx()) }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailButton(s[Str.ORIGINAL_ARTICLE], WebIcons.external, primary = true, on = false, role = null, onClick = { actions.openUrl(issue.url) }, tag = TestTags.DETAIL_ORIGINAL)
            DetailButton(
                s[if (bookmarked) Str.BOOKMARKED else Str.BOOKMARKS], if (bookmarked) WebIcons.bookmarkOn else WebIcons.bookmark,
                primary = false, on = bookmarked, role = Role.Checkbox, onClick = { actions.bookmark(issue) }, tag = TestTags.DETAIL_BOOKMARK,
            )
            DetailButton(s[Str.SHARE], WebIcons.share, primary = false, on = false, role = Role.Button, onClick = { actions.share(issue) }, tag = TestTags.DETAIL_SHARE)
        }
    }
}

/** 웹 .btn-primary(진한 바탕) · .btn-ghost(테두리 · 켜면 글자 진하게 + 채운 아이콘), 높이 38 · 모서리 8 */
@Composable
private fun DetailButton(text: String, icon: ImageVector, primary: Boolean, on: Boolean, role: Role?, onClick: () -> Unit, tag: String) {
    val c = WebTheme.colors
    val fg = when { primary -> c.inkText; on -> c.text; else -> c.muted }
    Row(
        Modifier.press(onClick, role = role).testTag(tag)
            .then(if (role == Role.Checkbox) Modifier.semantics { toggleableState = ToggleableState(on) } else Modifier)
            .height(38.dp)
            .then(if (primary) Modifier.background(c.ink) else Modifier.border(1.dp, c.line, RoundedCornerShape(8.dp)))
            .padding(horizontal = if (primary) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (primary) {
            Text(text, style = WebType.button, color = fg, maxLines = 1)
            WebIcon(icon, fg, Modifier.size(11.dp))
        } else {
            WebIcon(icon, fg, Modifier.size(17.dp))
            Text(text, style = WebType.button.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
        }
    }
}
