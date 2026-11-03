package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.GoogleClientId
import io.github.jwavascript.issuerank.data.GoogleCredential
import io.github.jwavascript.issuerank.data.GoogleCredentials
import io.github.jwavascript.issuerank.data.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 검증 8(Google, AC-06-3·4 → AC-15-2 · D35): "Google로 계속하기" → 가짜 자격(걸이 — 가짜 서버 /__test/google-id-token 토큰, 실제 Google · Credential Manager 0) →
 * sign-in/social → 로그인 + 북마크하려던 이슈 상세 · 취소 = "Google 로그인을 취소했습니다" · 같은 이메일의 이메일 계정 = "이미 가입된 이메일입니다. …"(잇지 않음) ·
 * 웹 클라이언트 ID 자리표시자(C3) = 기본 걸이가 Credential Manager를 부르지 않고 "지금은 Google 로그인을 사용할 수 없습니다". 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class GoogleLoginTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val s get() = server.strings
    private val vm get() = compose.activity.viewModel
    private val graph get() = AppGraph.get(server.context)

    /** 걸이: 앱이 만든 nonce로 가짜 서버 시험 토큰(누른 순간 읽음 — FakeServerRule이 끝에 기본으로) */
    private fun fakeGoogle(email: String, asked: MutableList<String> = CopyOnWriteArrayList()) {
        graph.googleCredentials = { _ ->
            GoogleCredentials { nonce ->
                asked += nonce
                GoogleCredential.Token(withContext(Dispatchers.IO) { FakeAccounts.googleToken(email, nonce) })
            }
        }
    }

    private fun socialRequests() = FakeServer.requests().filter { it.getString("path") == "/api/auth/sign-in/social" }

    @Test
    fun fakeCredential_signsIn_andOpensTheIssueThatAskedToLogIn() {
        val asked = CopyOnWriteArrayList<String>()
        fakeGoogle("google.flow@example.invalid", asked)
        compose.waitForTiles(30)
        val issue = vm.state.value.ranking!!.issueById("${server.today}-pol-21")!!
        // 상세 북마크(비로그인) → "북마크하려면 로그인이 필요합니다" + "로그인" → 그 이슈로 로그인 창(웹 openAuth(id))
        compose.onNodeWithTag(TestTags.tile(issue.id)).performScrollTo().performClick()
        compose.waitForTag(TestTags.DETAIL)
        compose.onNodeWithTag(TestTags.DETAIL_BOOKMARK).performClick()
        compose.waitForText(s[Str.TOAST_BM_LOGIN])
        compose.onNodeWithTag(TestTags.TOAST_ACTION, useUnmergedTree = true).performClick()
        compose.waitForTag(TestTags.AUTH_DIALOG)
        assertEquals(issue.id, vm.state.value.auth.issueId)
        compose.onNodeWithTag(TestTags.AUTH_GOOGLE).assertTextEquals(s[Str.GOOGLE_CONTINUE]).performClick()
        compose.waitForText(s[Str.TOAST_SIGNED_IN])
        compose.waitForTagGone(TestTags.AUTH_DIALOG)
        // 웹 Google 돌아옴: 북마크하려던 이슈 상세가 다시 열림
        compose.waitForTag(TestTags.DETAIL)
        compose.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextEquals(issue.title)
        assertEquals("google.flow@example.invalid", vm.state.value.user!!.email)
        assertTrue(graph.cookies.hasSession(FakeServer.base))
        assertEquals(1, asked.size)
        assertEquals(43, asked.single().length) // 앱 nonce(32바이트 base64url)
        assertEquals(200, socialRequests().single().getInt("status"))
        // 서버의 바깥 요청 = 가짜 공개 키뿐(실제 Google 0)
        val outbound = FakeServer.call("GET", "/__test/requests").getJSONArray("outbound")
        assertTrue((0 until outbound.length()).all { outbound.getJSONObject(it).getString("result") == "fake-jwks" })
    }

    @Test
    fun cancel_showsCancelled_andSendsNothing() {
        graph.googleCredentials = { _ -> GoogleCredentials { GoogleCredential.Cancelled } }
        compose.waitForTiles(30)
        compose.openLoginFromBookmarks()
        compose.onNodeWithTag(TestTags.AUTH_GOOGLE).performClick()
        compose.waitForText(s[Str.GOOGLE_CANCELLED])
        compose.onNodeWithTag(TestTags.AUTH_ERROR).assertTextEquals(s[Str.GOOGLE_CANCELLED])
        assertTrue(compose.exists(TestTags.AUTH_DIALOG))
        assertTrue(socialRequests().isEmpty())
        assertFalse(graph.cookies.hasSession(FakeServer.base))
    }

    @Test
    fun sameEmailAsAnEmailAccount_isNotLinked() {
        val email = "linked.check@example.invalid"
        FakeAccounts.signUp("이메일 사용자", email)
        fakeGoogle(email)
        compose.waitForTiles(30)
        compose.openLoginFromBookmarks()
        compose.onNodeWithTag(TestTags.AUTH_GOOGLE).performClick()
        compose.waitForText(s[Str.GOOGLE_NOT_LINKED])
        compose.onNodeWithTag(TestTags.AUTH_ERROR).assertTextEquals(s[Str.GOOGLE_NOT_LINKED])
        assertEquals(401, socialRequests().single().getInt("status"))
        assertFalse(graph.cookies.hasSession(FakeServer.base))
        assertTrue(vm.state.value.user == null)
    }

    /** C3: google.xml이 자리표시자인 동안만(실제 ID가 들어오면 이 시험은 건너뜀 — 실제 계정 창을 띄우지 않게) */
    @Test
    fun placeholderClientId_defaultHookIsUnavailable_withoutCredentialManager() {
        assumeTrue(GoogleClientId.isPlaceholder(server.context.getString(R.string.google_web_client_id)))
        compose.waitForTiles(30) // 걸이 = 기본(FakeServerRule.resetHooks — 실제 구현)
        compose.openLoginFromBookmarks()
        compose.onNodeWithTag(TestTags.AUTH_GOOGLE).performClick()
        compose.waitForText(s[Str.GOOGLE_UNAVAILABLE])
        compose.onNodeWithTag(TestTags.AUTH_ERROR).assertTextEquals(s[Str.GOOGLE_UNAVAILABLE])
        assertTrue(socialRequests().isEmpty())
    }
}
