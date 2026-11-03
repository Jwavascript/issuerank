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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jwavascript.issuerank.FolderDelete
import io.github.jwavascript.issuerank.FolderPicker
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.BookmarkView
import io.github.jwavascript.issuerank.data.Folder
import io.github.jwavascript.issuerank.data.Inputs
import io.github.jwavascript.issuerank.data.Str

private object PickerType {
    /** .pop-title(11px 700 · 자간 .04em) */
    val title = TextStyle(fontSize = 11.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em)
    val count = TextStyle(fontSize = 12.sp, lineHeight = 1.55.em)
}

/**
 * 폴더 고르기(웹 #pop · openPop 953~963): 북마크 단추 옆 팝오버 → 앱은 가운데 창(입력 칸 · 키보드 · 상세 창 위에서도 같은 자리, 웹 팝오버처럼 뒤를 어둡게 하지 않음).
 * 바깥 · 뒤로 = 닫기(웹 바깥 누름 · Esc).
 */
@Composable
fun FolderPickerDialog(state: UiState, actions: ScreenActions) {
    val picker = state.picker ?: return
    Dialog(onDismissRequest = actions::closePicker, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogDim(0f)
        FolderPickerContent(picker, state.folders, actions)
    }
}

/**
 * 폴더 고르기 내용(.pop: 232 · 여백 6 · 모서리 12 · surface · 테두리 line · 그림자): "폴더에 저장" · 폴더(서버 순서 · 이름 · 개수) · 새 폴더 이름 + "만들기".
 * 글자색 = muted(웹 faint 글자 — C11 ①).
 */
@Composable
fun FolderPickerContent(picker: FolderPicker, folders: List<Folder>, actions: ScreenActions, modifier: Modifier = Modifier) {
    val c = WebTheme.colors
    val title = str(Str.FOLDER_SAVE_TITLE)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.width(232.dp).shadow(12.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape).background(c.surface).border(1.dp, c.line, shape)
            .verticalScroll(rememberScrollState()).padding(6.dp)
            .semantics { paneTitle = title }.testTag(TestTags.FOLDER_PICKER),
    ) {
        Text(title, style = PickerType.title, color = c.muted, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp))
        for (folder in folders) FolderOption(folder, actions)
        NewFolderForm(picker, actions)
    }
}

/** .pop button.opt: 이름(14px) + 개수(12px · 오른쪽) — 누르면 그 폴더에 저장 */
@Composable
private fun FolderOption(folder: Folder, actions: ScreenActions) {
    val c = WebTheme.colors
    Row(
        Modifier.fillMaxWidth().press({ actions.saveToFolder(folder.id) }, shape = RoundedCornerShape(7.dp)).testTag(TestTags.folderOption(folder.id))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(folder.name, style = WebType.contextDesc, color = c.text, modifier = Modifier.weight(1f))
        Text("${folder.count}", style = PickerType.count, color = c.muted)
    }
}

/**
 * .pop form(위 선 · 여백 6 2 2): "새 폴더 이름"(maxlength 16 · 바탕 bg · 13px) + "만들기"(진한 단추).
 * 테두리: 초점 없음 = 웹 line을 칸 바탕(bg) · 고르기 창 바탕(surface) 둘 다와 3:1이 될 만큼만 text 쪽으로(C12 ⑥), 초점 = 2dp 글자색.
 * 자동 완성 없음(웹 autocomplete="off" 957 — contentType을 붙이지 않음).
 * 만들기 · 키보드 완료 = 키보드를 내리고 만들기(실패 토스트가 키보드에 가리지 않게).
 */
@Composable
private fun NewFolderForm(picker: FolderPicker, actions: ScreenActions) {
    val c = WebTheme.colors
    val lineArgb = c.line.toArgb()
    val textArgb = c.text.toArgb()
    val fillArgb = c.bg.toArgb()
    val aroundArgb = c.surface.toArgb()
    val border = remember(lineArgb, textArgb, fillArgb, aroundArgb) { Color(Contrast.inputBorder(lineArgb, textArgb, fillArgb, aroundArgb)) }
    val keyboard = LocalSoftwareKeyboardController.current
    val create = {
        keyboard?.hide()
        actions.createFolder()
    }
    var name by remember(picker.issueId) { mutableStateOf(picker.name) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp)
            .drawBehind { drawLine(c.line, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx()) }
            .padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = name,
            onValueChange = { v ->
                name = Inputs.limit(v, Inputs.FOLDER_NAME_MAX)
                actions.setFolderName(name)
            },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().onFocusChanged { focused = it.isFocused }.testTag(TestTags.FOLDER_NEW_NAME),
            textStyle = WebType.label13.copy(color = c.text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { create() }),
            singleLine = true,
            cursorBrush = SolidColor(c.text),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 30.dp).clip(shape).background(c.bg)
                        .border(if (focused) 2.dp else 1.dp, if (focused) c.text else border, shape)
                        .padding(horizontal = if (focused) 7.dp else 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (name.isEmpty()) Text(str(Str.FOLDER_NEW_HINT), style = WebType.label13, color = c.muted, maxLines = 1)
                    inner()
                }
            },
        )
        Box(
            Modifier.press(create, shape = shape).testTag(TestTags.FOLDER_CREATE)
                .height(30.dp).background(c.ink, shape).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(str(Str.FOLDER_CREATE), style = WebType.pill, color = c.inkText, maxLines = 1) }
    }
}

/** 폴더 삭제 확인 창(웹 #folderDialog — 로그인 창 꼴 · 늘 밝은 색): 뒤로 · 바깥 = 취소(보내는 중이면 무시) */
@Composable
fun FolderDeleteDialog(state: UiState, actions: ScreenActions) {
    val target = state.folderDelete ?: return
    Dialog(onDismissRequest = actions::cancelFolderDelete, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogDim(LightDialog.BACKDROP)
        FolderDeleteContent(target, actions)
    }
}

/**
 * 폴더 삭제 확인 내용(565~569 · 1039~1042): "‘이름’ 폴더를 삭제할까요?" · 함께 지워질 개수(= 칩 숫자) 또는 빈 폴더 안내 · 취소 · 삭제(#b42318 · 흰 글자).
 * 보내는 동안 두 단추 막음 + 흐림(.5).
 */
@Composable
fun FolderDeleteContent(target: FolderDelete, actions: ScreenActions, modifier: Modifier = Modifier) {
    val s = rememberStrings()
    val title = s[Str.FOLDER_DELETE_TITLE, target.folder.name]
    LightDialogFrame(title, modifier.testTag(TestTags.FOLDER_DELETE_DIALOG)) {
        Text(
            title, style = AuthType.title, color = Color(LightDialog.TEXT),
            modifier = Modifier.padding(bottom = 20.dp).semantics { heading() }.testTag(TestTags.FOLDER_DELETE_TITLE),
        )
        Text(
            BookmarkView.deleteNote(target.folder, s), style = WebType.contextDesc, color = Color(LightDialog.TEXT),
            modifier = Modifier.testTag(TestTags.FOLDER_DELETE_NOTE),
        )
        FlowRow(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LightChip(
                s[Str.CANCEL], !target.busy, actions::cancelFolderDelete, Modifier.testTag(TestTags.FOLDER_DELETE_CANCEL), fadeWhenDisabled = true,
            )
            LightChip(
                s[Str.DELETE], !target.busy, actions::confirmFolderDelete, Modifier.testTag(TestTags.FOLDER_DELETE_CONFIRM),
                danger = true, fadeWhenDisabled = true,
            )
        }
    }
}
