package io.github.jwavascript.issuerank.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.DateLine
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.View

/**
 * 이슈맵 · 랭킹이 같이 쓰는 날짜 줄(index.html 472~497 · renderContext 698~711): 제목 · 설명 | 이전 · 날짜(달력) · 다음 · "오늘" + "… 수집 · 이슈 n개".
 * 목록 화면이면 폭 760(.context.narrow — 웹처럼 왼쪽 맞춤). 달력 = Material3 DatePicker(KST 오늘 이후 막음).
 */
@Composable
fun DateContext(state: UiState, bp: Breakpoints, actions: ScreenActions) {
    val c = WebTheme.colors
    val s = rememberStrings()
    val line = state.dateLine
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().then(if (state.view == View.LIST) Modifier.widthIn(max = WRAP.dp) else Modifier)
            .padding(top = if (bp.narrow) 24.dp else 32.dp, bottom = 16.dp),
    ) {
        SpreadRow(
            gap = 16.dp, alignBottom = true,
            left = {
                Column {
                    Text(
                        line.title(s), style = if (bp.narrow) WebType.h1Narrow else WebType.h1, color = c.text,
                        modifier = Modifier.padding(bottom = 6.dp).semantics { heading() }.testTag(TestTags.RANK_TITLE),
                    )
                    Text(line.desc(s), style = WebType.contextDesc, color = c.muted, modifier = Modifier.testTag(TestTags.RANK_DESC))
                }
            },
            right = {
                Column {
                    DateBar(line, line.dateText(s), actions) { picking = true }
                    val updated = line.updated(s)
                    if (updated.isNotEmpty()) {
                        Text(updated, style = WebType.small, color = c.muted, modifier = Modifier.padding(top = 8.dp).testTag(TestTags.UPDATED))
                    }
                }
            },
        )
    }
    if (picking) {
        DateDialog(
            date = state.date, today = state.today,
            onDismiss = { picking = false },
            onPick = { picking = false; actions.pickDate(it) },
        )
    }
}

/** 웹 .datebar(flex-wrap · gap 6): 이전 · 날짜 · 다음(오늘이면 막음) · "오늘"(오늘이 아닐 때) */
@Composable
private fun DateBar(line: DateLine, dateText: String, actions: ScreenActions, onPick: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(WebIcons.prev, str(Str.CD_PREV_DAY), true, actions::prevDay, Modifier.testTag(TestTags.DATE_PREV))
        DateLabel(dateText, onPick)
        IconBox(WebIcons.next, str(Str.CD_NEXT_DAY), line.nextEnabled, actions::nextDay, Modifier.testTag(TestTags.DATE_NEXT))
        if (line.todayVisible) ChipButton(str(Str.TODAY), actions::today, Modifier.testTag(TestTags.DATE_TODAY))
    }
}

/** 웹 .icon-btn(32×32 · 모서리 8 · 테두리 · 막히면 흐리게 .35 — 쓸 수 없는 단추라 대비 기준 밖, WCAG 1.4.3 · 1.4.11 예외) — 이름 = aria-label */
@Composable
fun IconBox(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    Box(
        modifier.press(onClick, enabled = enabled).semantics { contentDescription = label }
            .size(32.dp).border(1.dp, c.line, RoundedCornerShape(8.dp)).alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) { WebIcon(icon, c.muted, Modifier.size(14.dp)) }
}

/** 웹 .date-label: 달력 아이콘 + "11월 3일 (화) · 오늘" — 이름 = "날짜 선택"(input aria-label), 상태 = 날짜 글 */
@Composable
private fun DateLabel(text: String, onPick: () -> Unit) {
    val c = WebTheme.colors
    val label = str(Str.CD_PICK_DATE)
    Row(
        Modifier.press(onPick).semantics { contentDescription = label; stateDescription = text }.testTag(TestTags.DATE_PICK)
            .height(32.dp).border(1.dp, c.line, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WebIcon(WebIcons.calendar, c.text, Modifier.size(14.dp))
        Text(text, style = WebType.tab, color = c.text, maxLines = 1, modifier = Modifier.testTag(TestTags.DATE_TEXT))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(date: String, today: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val selectable = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = PickerDates.selectable(utcTimeMillis, today)
            override fun isSelectableYear(year: Int) = PickerDates.selectableYear(year, today)
        }
    }
    val picker = rememberDatePickerState(initialSelectedDateMillis = PickerDates.toMillis(date), selectableDates = selectable)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.DATE_DIALOG),
        confirmButton = {
            TextButton(
                onClick = { picker.selectedDateMillis?.let { onPick(PickerDates.fromMillis(it)) } ?: onDismiss() },
                modifier = Modifier.testTag(TestTags.DATE_DIALOG_OK),
            ) { Text(stringResource(R.string.date_pick_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(Str.CANCEL)) } },
    ) { DatePicker(picker) }
}
