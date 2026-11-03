package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.jwavascript.issuerank.AuthForm
import io.github.jwavascript.issuerank.data.Inputs
import io.github.jwavascript.issuerank.data.Links
import io.github.jwavascript.issuerank.data.Str

/**
 * 로그인 창 · 폴더 삭제 창의 고정 밝은 색(웹 .auth-dialog 406~431 — 다크에서도 늘 밝음, A1 02-skeleton 9절). 8비트 ARGB.
 * 대비(C11 ⑥): 글자 · 상태 그림은 웹 값 그대로 기준(4.5 · 3)을 넘음. 바꾼 칠은 입력 칸 초점 없는 테두리 하나(C12 ⑥ — [UNFOCUSED_INPUT_BORDER]).
 * ThemeContrastTest가 웹 CSS 글자와 대조하고 잼.
 */
internal object LightDialog {
    /** background #ffffff */
    const val SURFACE = 0xFFFFFFFF.toInt()

    /** color #222 — 제목 · 이름 칸 · 입력 글 · 단추 글 · 폴더 삭제 안내 */
    const val TEXT = 0xFF222222.toInt()

    /** 창 테두리 #ddd · "또는 이메일로" 양옆 선 #ddd */
    const val BORDER = 0xFFDDDDDD.toInt()

    /** 창 안 --line #e7e4df(.chip-btn 테두리) */
    const val LINE = 0xFFE7E4DF.toInt()

    /** input 테두리 #aaa(웹 값 — 흰 바탕에서 2.32) */
    const val INPUT_BORDER = 0xFFAAAAAA.toInt()

    /** 그리는 초점 없는 입력 칸 테두리(C12 ⑥): #aaa를 칸 바탕 · 창 바탕(둘 다 흰색)과 3:1이 될 만큼만 #222 쪽으로 */
    val UNFOCUSED_INPUT_BORDER: Int = Contrast.inputBorder(INPUT_BORDER, TEXT, SURFACE)

    /** .auth-or · .auth-legal #6f6b65 */
    const val MUTED = 0xFF6F6B65.toInt()

    /** .auth-error #b42318 · 폴더 삭제 단추 바탕(흰 글자) */
    const val ERROR = 0xFFB42318.toInt()
    const val ON_ERROR = 0xFFFFFFFF.toInt()

    /** .google-btn(Google 버튼 지침 밝은 꼴): 테두리 #747775 · 글자 #1F1F1F · 바탕 #FFFFFF */
    const val GOOGLE_BORDER = 0xFF747775.toInt()
    const val GOOGLE_TEXT = 0xFF1F1F1F.toInt()

    /** ::backdrop rgb(0 0 0 / 35%) — 창 dim 값(미리보기는 같은 색 ARGB) */
    const val BACKDROP = 0.35f
    const val BACKDROP_ARGB = 0x59000000

    /** 바쁜 동안 흐림(막힌 단추 — 대비 기준 밖): Google 단추 .38(.google-btn:disabled) · 폴더 삭제 창 두 단추 .5(#folderDialog button:disabled) — 흰 창 위에 미리 섞은 불투명 색 */
    const val DISABLED_GOOGLE = 0.38
    const val DISABLED_FOLDER = 0.5
}

/** 밝은 창 글자(로그인 창 · 폴더 삭제 창 공용) */
internal object AuthType {
    /** h2(기본 1.5em = 22.5px · 굵게) */
    val title = TextStyle(fontSize = 22.5.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Bold)
    val label = TextStyle(fontSize = 14.sp, lineHeight = 1.55.em)
    val input = TextStyle(fontSize = 15.sp, lineHeight = 1.55.em)
    val google = TextStyle(fontSize = 14.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Medium)
}

/** 창 안 글 고르기 색(밝은 창 — 다크 테마 색 묶음의 밝은 손잡이가 흰 바탕에 묻히지 않게): 손잡이 #222 · 칠 = #222를 .4로 흰 바탕에 섞은 불투명 색 */
private val LightSelection = TextSelectionColors(
    handleColor = Color(LightDialog.TEXT),
    backgroundColor = Color(Contrast.over(LightDialog.TEXT, 0.4, LightDialog.SURFACE)),
)

/** 창 뒤 어둡게(웹 ::backdrop) — Compose Dialog 창의 dim 값을 웹 값으로 */
@Composable
internal fun DialogDim(amount: Float) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(amount) }
}

/**
 * 밝은 창 틀(웹 .auth-dialog): 폭 min(400, 화면 − 32) · 테두리 #ddd · 모서리 16 · 여백 24 · 글 #222 · 바탕 흰색, 넘치면 창 안에서 스크롤(브라우저 dialog 기본).
 * 물결(누름) · 글 고르기 색도 밝은 색으로.
 */
@Composable
internal fun LightDialogFrame(paneTitle: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    CompositionLocalProvider(LocalContentColor provides Color(LightDialog.TEXT), LocalTextSelectionColors provides LightSelection) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier.widthIn(max = 400.dp).fillMaxWidth()
                    .clip(shape).background(Color(LightDialog.SURFACE)).border(1.dp, Color(LightDialog.BORDER), shape)
                    .verticalScroll(rememberScrollState()).padding(24.dp)
                    .semantics { this.paneTitle = paneTitle },
                content = content,
            )
        }
    }
}

/** 밝은 창의 .chip-btn(높이 32 · 모서리 8 · 테두리 --line · 13px 600 · #222), danger = 폴더 삭제 단추(바탕 #b42318 · 흰 글자) */
@Composable
internal fun LightChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    fadeWhenDisabled: Boolean = false,
) {
    val faded = !enabled && fadeWhenDisabled
    fun tone(argb: Int) = Color(if (faded) Contrast.over(argb, LightDialog.DISABLED_FOLDER, LightDialog.SURFACE) else argb)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.press(onClick, enabled = enabled).height(32.dp)
            .then(if (danger) Modifier.background(tone(LightDialog.ERROR), shape) else Modifier)
            .border(1.dp, tone(if (danger) LightDialog.ERROR else LightDialog.LINE), shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = WebType.pill, color = tone(if (danger) LightDialog.ON_ERROR else LightDialog.TEXT), maxLines = 1) }
}

/** 로그인 창(웹 #authDialog — 모달): 보내는 중이면 뒤로(Esc)도 막힘, 바깥을 눌러도 닫히지 않음(웹 dialog 기본) */
@Composable
fun AuthDialog(auth: AuthForm, actions: ScreenActions) {
    if (!auth.open) return
    Dialog(
        onDismissRequest = actions::closeAuth,
        properties = DialogProperties(dismissOnBackPress = !auth.busy, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        DialogDim(LightDialog.BACKDROP)
        AuthDialogContent(auth, actions)
    }
}

/**
 * 로그인 창 내용(웹 546~558 순서 그대로): 제목(로그인 · 회원가입) · "Google로 계속하기" · "또는 이메일로" · (회원가입) 이름 · 이메일 · 비밀번호 ·
 * 오류 줄 · 로그인/회원가입 · 회원가입/로그인으로 전환 · 닫기 · 이용약관 · 개인정보처리방침. 열면 이메일 칸에 초점(웹 email.focus()).
 * 입력 칸은 화면이 바로 그리고 ViewModel에 넘김(AuthForm.revision이 바뀔 때만 다시 읽음 — 비밀번호 비우기 · 전환).
 */
@Composable
fun AuthDialogContent(auth: AuthForm, actions: ScreenActions, modifier: Modifier = Modifier, autoFocus: Boolean = true) {
    val title = str(if (auth.signUp) Str.SIGNUP else Str.LOGIN)
    val emailFocus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { emailFocus.requestFocus() }
    LightDialogFrame(title, modifier.testTag(TestTags.AUTH_DIALOG)) {
        Text(title, style = AuthType.title, color = Color(LightDialog.TEXT), modifier = Modifier.semantics { heading() }.testTag(TestTags.AUTH_TITLE))
        Spacer(Modifier.height(20.dp))
        GoogleButton(enabled = !auth.busy, onClick = actions::googleSignIn)
        AuthOr()
        // 키보드: 이름 = 글 · 다음, 이메일 = 이메일 자판 · 다음, 비밀번호 = 비밀번호 자판(가림) · 완료 = 제출(웹 폼 Enter)
        // 자동 완성(웹 autocomplete 551~553 · 1407 → contentType, C12 ⑦): name · email · current-password(로그인) / new-password(회원가입)
        if (auth.signUp) {
            AuthField(
                str(Str.AUTH_NAME), auth.name, auth.revision, actions::setAuthName, Inputs.NAME_MAX,
                KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next), KeyboardActions.Default, TestTags.AUTH_NAME,
                autofill = ContentType.PersonFullName,
            )
        }
        AuthField(
            str(Str.AUTH_EMAIL), auth.email, auth.revision, actions::setAuthEmail, null,
            KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            KeyboardActions.Default, TestTags.AUTH_EMAIL, autofill = ContentType.EmailAddress, focusRequester = emailFocus,
        )
        AuthField(
            str(Str.AUTH_PASSWORD), auth.password, auth.revision, actions::setAuthPassword, Inputs.PASSWORD_MAX,
            KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            KeyboardActions(onDone = { actions.submitAuth() }), TestTags.AUTH_PASSWORD,
            autofill = if (auth.signUp) ContentType.NewPassword else ContentType.Password, password = true,
        )
        // .auth-error(role=alert · aria-live=polite): 빈 줄도 20 높이, 위아래 14(p 기본 여백 — 아래는 단추 줄 20과 겹침)
        Text(
            auth.error?.let { str(it) } ?: "", style = AuthType.label, color = Color(LightDialog.ERROR),
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth().heightIn(min = 20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }.testTag(TestTags.AUTH_ERROR),
        )
        // .auth-actions(gap 12 · wrap): 보내는 동안 세 단추 모두 막힘(웹 — 모양은 그대로)
        FlowRow(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LightChip(str(if (auth.signUp) Str.SIGNUP else Str.LOGIN), !auth.busy, actions::submitAuth, Modifier.testTag(TestTags.AUTH_SUBMIT))
            LightChip(str(if (auth.signUp) Str.AUTH_SWITCH_TO_LOGIN else Str.SIGNUP), !auth.busy, actions::switchAuthMode, Modifier.testTag(TestTags.AUTH_SWITCH))
            LightChip(str(Str.CLOSE), !auth.busy, actions::closeAuth, Modifier.testTag(TestTags.AUTH_CLOSE))
        }
        // .auth-legal: 이용약관 · 개인정보처리방침(Custom Tab — 늘 배포 주소)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LegalLink(str(Str.TERMS), { actions.openUrl(Links.TERMS) }, TestTags.AUTH_TERMS)
            Text(" · ", style = WebType.small, color = Color(LightDialog.MUTED))
            LegalLink(str(Str.PRIVACY), { actions.openUrl(Links.PRIVACY) }, TestTags.AUTH_PRIVACY)
        }
    }
}

/** .google-btn: 폭 전체 · 높이 40 · 모서리 20 · 4색 G 20px + "Google로 계속하기"(14px 500). 바쁜 동안 흐림(.38) */
@Composable
private fun GoogleButton(enabled: Boolean, onClick: () -> Unit) {
    fun tone(argb: Int) = Color(if (enabled) argb else Contrast.over(argb, LightDialog.DISABLED_GOOGLE, LightDialog.SURFACE))
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().press(onClick, enabled = enabled, shape = shape).testTag(TestTags.AUTH_GOOGLE)
            .heightIn(min = 40.dp).background(Color(LightDialog.SURFACE), shape).border(1.dp, tone(LightDialog.GOOGLE_BORDER), shape)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(if (enabled) WebIcons.google else WebIcons.googleFaded, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(str(Str.GOOGLE_CONTINUE), style = AuthType.google, color = tone(LightDialog.GOOGLE_TEXT))
    }
}

/** .auth-or: 양옆 1px #ddd 선 + "또는 이메일로"(13px #6f6b65) */
@Composable
private fun AuthOr() {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(Color(LightDialog.BORDER)))
        Text(str(Str.AUTH_OR_EMAIL), style = WebType.label13, color = Color(LightDialog.MUTED))
        Box(Modifier.weight(1f).height(1.dp).background(Color(LightDialog.BORDER)))
    }
}

/**
 * label(14px · 위 14) + input(위 6 · 여백 10 · 모서리 8 · 15px). 이름표가 입력 칸 안(누르면 초점 — 웹 label 감싸기).
 * 테두리: 초점 없음 = 웹 #aaa를 3:1이 될 만큼만 진하게(C12 ⑥), 초점 = 2dp 글자색(웹 = 브라우저 초점 테두리). max = 웹 maxlength(넘는 글자는 안 들어감).
 * autofill = 웹 autocomplete와 같은 뜻(contentType — 기기 자동 완성 서비스가 읽음).
 */
@Composable
private fun AuthField(
    label: String,
    value: String,
    revision: Int,
    onChange: (String) -> Unit,
    max: Int?,
    keyboard: KeyboardOptions,
    keyboardActions: KeyboardActions,
    tag: String,
    autofill: ContentType,
    password: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var text by remember(revision) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = text,
        onValueChange = { v ->
            val next = if (max != null) Inputs.limit(v, max) else v
            text = next
            onChange(next)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentType = autofill }
            .testTag(tag),
        textStyle = AuthType.input.copy(color = Color(LightDialog.TEXT)),
        keyboardOptions = keyboard,
        keyboardActions = keyboardActions,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        cursorBrush = SolidColor(Color(LightDialog.TEXT)),
        decorationBox = { inner ->
            Column {
                Text(label, style = AuthType.label, color = Color(LightDialog.TEXT))
                Box(
                    Modifier.padding(top = 6.dp).fillMaxWidth()
                        .border(if (focused) 2.dp else 1.dp, Color(if (focused) LightDialog.TEXT else LightDialog.UNFOCUSED_INPUT_BORDER), RoundedCornerShape(8.dp))
                        .padding(if (focused) 9.dp else 10.dp),
                ) { inner() }
            }
        },
    )
}

/** .auth-legal 링크(12px · #6f6b65 · 밑줄) */
@Composable
private fun LegalLink(text: String, onClick: () -> Unit, tag: String) {
    Box(Modifier.press(onClick, shape = RoundedCornerShape(4.dp)).testTag(tag), contentAlignment = Alignment.Center) {
        Text(text, style = WebType.small.copy(textDecoration = TextDecoration.Underline), color = Color(LightDialog.MUTED))
    }
}
