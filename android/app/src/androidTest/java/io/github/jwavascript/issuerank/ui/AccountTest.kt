package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(계정, AC-06-1·2 · AC-15-1): 로그인 창(웹 순서 · 문구) · 가입 · 로그인 · 오류 문구(서버 실제 응답 → 웹 authMessage) · 로그아웃(기기 세션 지움) ·
 * 세션 만료(/__test/expire-sessions) → 개인 API 401 → "로그인이 필요합니다" + "로그인" · 자동 완성 힌트(웹 autocomplete → contentType, C12 ⑦).
 * 가짜 서버(README) · 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class AccountTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val s get() = server.strings
    private val vm get() = compose.activity.viewModel
    private val graph get() = AppGraph.get(server.context)

    private fun requests(path: String) = FakeServer.requests().filter { it.getString("path") == path }

    @Test
    fun signUp_showsTheUser_andSignOutClearsTheDeviceSession() {
        val name = "앱 시험 사용자"
        compose.waitForTiles(30)
        compose.openLoginFromBookmarks()
        // 웹 순서 · 문구: 제목 "로그인" · Google · "또는 이메일로" · 이메일 · 비밀번호 · 로그인 · 회원가입 · 닫기 · 약관
        compose.onNodeWithTag(TestTags.AUTH_TITLE).assertTextEquals(s[Str.LOGIN])
        compose.onNodeWithTag(TestTags.AUTH_GOOGLE).assertTextEquals(s[Str.GOOGLE_CONTINUE])
        compose.waitForText(s[Str.AUTH_OR_EMAIL])
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).assertTextEquals(s[Str.LOGIN])
        compose.onNodeWithTag(TestTags.AUTH_SWITCH).assertTextEquals(s[Str.SIGNUP])
        compose.onNodeWithTag(TestTags.AUTH_CLOSE).assertTextEquals(s[Str.CLOSE])
        assertFalse(compose.exists(TestTags.AUTH_NAME))
        compose.onNodeWithTag(TestTags.AUTH_SWITCH).performClick()
        compose.waitForTag(TestTags.AUTH_NAME)
        compose.onNodeWithTag(TestTags.AUTH_TITLE).assertTextEquals(s[Str.SIGNUP])
        compose.onNodeWithTag(TestTags.AUTH_SWITCH).assertTextEquals(s[Str.AUTH_SWITCH_TO_LOGIN])
        compose.onNodeWithTag(TestTags.AUTH_NAME).performTextInput(name)
        compose.onNodeWithTag(TestTags.AUTH_EMAIL).performTextInput("signup.flow@example.invalid")
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(FakeAccounts.PASSWORD)
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
        compose.waitForText(s[Str.TOAST_SIGNED_IN])
        compose.waitForTagGone(TestTags.AUTH_DIALOG)
        assertTrue(graph.cookies.hasSession(FakeServer.base))
        compose.onNodeWithTag(TestTags.BM_SUB).assertTextEquals(s[Str.BM_SUB, 0])
        compose.waitForTag(TestTags.BM_EMPTY)
        // 로그아웃(640 이하 = 북마크 화면, 넓으면 머리) — 글 "{이름} · 로그아웃"
        val logout = compose.signOutTag()
        compose.onNodeWithTag(logout).assertTextEquals(s[Str.USER_LOGOUT, name])
        compose.onNodeWithTag(logout).performClick()
        compose.waitForText(s[Str.TOAST_SIGNED_OUT])
        compose.waitForTag(TestTags.BOOKMARKS_LOGIN)
        assertFalse(graph.cookies.hasSession(FakeServer.base))
        assertNull(vm.state.value.user)
        val out = requests("/api/auth/sign-out").single()
        assertEquals(FakeServer.base, out.getString("origin"))
        assertEquals(200, out.getInt("status"))
    }

    @Test
    fun signInErrors_areTheWebMessagesInTheDialog_andNameIsRequired() {
        val email = "account.errors@example.invalid"
        FakeAccounts.signUp("기존 사용자", email)
        compose.waitForTiles(30)
        compose.openLoginFromBookmarks()
        compose.onNodeWithTag(TestTags.AUTH_EMAIL).performTextInput(email)
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput("test-only-wrong-1")
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
        compose.waitForText(s[Str.AUTH_ERR_CREDENTIALS])
        compose.onNodeWithTag(TestTags.AUTH_ERROR).assertTextEquals(s[Str.AUTH_ERR_CREDENTIALS])
        assertEquals("", vm.state.value.auth.password) // 웹 finally: 비밀번호 비움
        // 회원가입 · 같은 이메일 = "이미 가입된 이메일입니다"
        compose.onNodeWithTag(TestTags.AUTH_SWITCH).performClick()
        compose.waitForTag(TestTags.AUTH_NAME)
        compose.onNodeWithTag(TestTags.AUTH_NAME).performTextInput("두 번째")
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(FakeAccounts.PASSWORD)
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
        compose.waitForText(s[Str.AUTH_ERR_EXISTS])
        // 이름 공백 = "이름을 입력해 주세요"(요청 0)
        val signUps = requests("/api/auth/sign-up/email").size
        compose.onNodeWithTag(TestTags.AUTH_NAME).performTextClearance()
        compose.onNodeWithTag(TestTags.AUTH_NAME).performTextInput("   ")
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(FakeAccounts.PASSWORD)
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
        compose.waitForText(s[Str.AUTH_ERR_NAME])
        assertEquals(signUps, requests("/api/auth/sign-up/email").size)
        // 로그인으로 전환 → 올바른 비밀번호 → "로그인했습니다"
        compose.onNodeWithTag(TestTags.AUTH_SWITCH).performClick()
        compose.waitForTagGone(TestTags.AUTH_NAME)
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(FakeAccounts.PASSWORD)
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
        compose.waitForText(s[Str.TOAST_SIGNED_IN])
        compose.waitForTagGone(TestTags.AUTH_DIALOG)
        assertEquals("기존 사용자", vm.state.value.user!!.name)
    }

    /** C12 ⑦: 자동 완성 = 웹 autocomplete(551~553 · 1407) 그대로 — name · email · current-password / new-password, 새 폴더 칸(957 off) = 없음 */
    @Test
    fun autofillHints_areTheWebAutocomplete_andTheNewFolderFieldHasNone() {
        fun hint(tag: String, type: ContentType) =
            compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, type))
        compose.waitForTiles(30)
        compose.openLoginFromBookmarks()
        hint(TestTags.AUTH_EMAIL, ContentType.EmailAddress) // autocomplete="email"
        hint(TestTags.AUTH_PASSWORD, ContentType.Password) // 로그인 = current-password
        compose.onNodeWithTag(TestTags.AUTH_SWITCH).performClick()
        compose.waitForTag(TestTags.AUTH_NAME)
        hint(TestTags.AUTH_NAME, ContentType.PersonFullName) // autocomplete="name"
        hint(TestTags.AUTH_EMAIL, ContentType.EmailAddress)
        hint(TestTags.AUTH_PASSWORD, ContentType.NewPassword) // 회원가입 = new-password(웹 setAuthMode)
        compose.onNodeWithTag(TestTags.AUTH_NAME).performTextInput("자동 완성 시험")
        compose.onNodeWithTag(TestTags.AUTH_EMAIL).performTextInput("autofill.hints@example.invalid")
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(FakeAccounts.PASSWORD)
        compose.onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
        compose.waitForText(s[Str.TOAST_SIGNED_IN])
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.VIEW_LIST)
        compose.onNodeWithTag(TestTags.RANK_TITLE).performScrollTo()
        compose.onNodeWithTag(TestTags.cardBookmark(vm.state.value.ranking!!.issuesOf(SectionKey.POL).first().id)).performClick()
        compose.waitForTag(TestTags.FOLDER_PICKER)
        compose.onNodeWithTag(TestTags.FOLDER_NEW_NAME).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentType))
    }

    @Test
    fun closeKeepsTheEmail_andClearsThePassword() {
        compose.waitForTiles(30)
        compose.openLoginFromBookmarks()
        compose.onNodeWithTag(TestTags.AUTH_EMAIL).performTextInput("keep.email@example.invalid")
        compose.onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(FakeAccounts.PASSWORD)
        compose.onNodeWithTag(TestTags.AUTH_CLOSE).performClick()
        compose.waitForTagGone(TestTags.AUTH_DIALOG)
        compose.onNodeWithTag(TestTags.BOOKMARKS_LOGIN).performClick()
        compose.waitForTag(TestTags.AUTH_DIALOG)
        assertEquals("keep.email@example.invalid", vm.state.value.auth.email)
        assertEquals("", vm.state.value.auth.password)
        assertTrue(FakeServer.requests().none { it.getString("path").startsWith("/api/auth/sign-") })
    }

    @Test
    fun expiredSession_personal401_logsOut_andTheToastOpensTheLoginDialog() {
        compose.waitForTiles(30)
        compose.signUpViaDialog(s, "만료 시험", "expired.session@example.invalid")
        FakeAccounts.expireSessions()
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.VIEW_LIST)
        val first = vm.state.value.ranking!!.issuesOf(SectionKey.POL).first()
        compose.onNodeWithTag(TestTags.RANK_TITLE).performScrollTo()
        compose.onNodeWithTag(TestTags.cardBookmark(first.id)).performClick()
        compose.waitForTag(TestTags.FOLDER_PICKER)
        compose.onNodeWithTag(TestTags.FOLDER_NEW_NAME).performTextInput("만료 폴더")
        compose.onNodeWithTag(TestTags.FOLDER_CREATE).performClick()
        compose.waitForText(s[Str.LOGIN_REQUIRED])
        compose.waitForTagGone(TestTags.FOLDER_PICKER) // 계정이 바뀌면 고르기 창도 닫힘
        assertNull(vm.state.value.user)
        assertEquals(401, requests("/api/folders").last().getInt("status"))
        compose.onNodeWithTag(TestTags.TOAST_ACTION, useUnmergedTree = true).performClick() // "로그인"
        compose.waitForTag(TestTags.AUTH_DIALOG)
        compose.onNodeWithTag(TestTags.AUTH_TITLE).assertTextEquals(s[Str.LOGIN])
    }
}
