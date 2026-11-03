package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.Strings
import io.github.jwavascript.issuerank.platform.UrlConnectionTransport
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals

/**
 * 계정 · 북마크 시험 도우미(A4): 가짜 서버(README 4 · 5절)로 앱 세션과 따로 계정 · 폴더 · 북마크를 만들고, Google 시험 토큰 · 세션 만료를 부름.
 * 비밀번호 = 시험용 가짜 값(test-only). 실제 Google · 운영 0.
 */
object FakeAccounts {
    const val PASSWORD = "test-only-password-1"
    private val transport = UrlConnectionTransport()

    /** POST /__test/google-id-token: 그 이메일 · nonce로 가짜 서버 키가 서명한 시험 ID 토큰(앱 걸이가 Credential Manager 대신 돌려줌) */
    fun googleToken(email: String, nonce: String): String =
        FakeServer.call("POST", "/__test/google-id-token", JSONObject().put("email", email).put("nonce", nonce).toString()).getString("idToken")

    /** POST /__test/expire-sessions: 모든 세션 만료 → 개인 API 401 */
    fun expireSessions() {
        FakeServer.call("POST", "/__test/expire-sessions")
    }

    /** 이메일 계정 가입(앱 쿠키와 따로) → 그 계정 세션 쿠키("이름=값" — 아래 씨앗 만들기에만) */
    fun signUp(name: String, email: String, password: String = PASSWORD): String {
        val r = send("POST", "/api/auth/sign-up/email", JSONObject().put("name", name).put("email", email).put("password", password), null)
        assertEquals("sign-up $email", 200, r.first)
        return r.second.first { it.startsWith("better-auth.session_token=") }.substringBefore(';')
    }

    /** 그 계정에 폴더 → id */
    fun createFolder(cookie: String, name: String): String {
        val r = send("POST", "/api/folders", JSONObject().put("name", name), cookie)
        assertEquals("folder $name", 201, r.first)
        return JSONObject(r.third).getString("id")
    }

    fun saveBookmark(cookie: String, issueId: String, folderId: String) {
        val r = send("POST", "/api/bookmarks", JSONObject().put("issueId", issueId).put("folderId", folderId), cookie)
        assertEquals("bookmark $issueId", 201, r.first)
    }

    /** (상태, Set-Cookie 줄들, 본문) */
    private fun send(method: String, path: String, body: JSONObject, cookie: String?): Triple<Int, List<String>, String> = runBlocking {
        val headers = buildList {
            add("Content-Type" to "application/json")
            add("Origin" to FakeServer.base)
            if (cookie != null) add("Cookie" to cookie)
        }
        val r = transport.execute(HttpRequest(method, FakeServer.base + path, headers, body.toString().toByteArray(), 5_000, 10_000))
        Triple(r.status, r.headerValues("set-cookie"), r.text)
    }
}

// ── 화면 도우미 ──

/** 로그인 창 열기 — 어느 폭에서나 되는 길(북마크 탭 "로그인"; 머리 계정 단추는 641 이상에서만) */
fun ComposeTestRule.openLoginFromBookmarks() {
    onNodeWithTag(TestTags.TAB_BOOKMARKS).performClick()
    waitForTag(TestTags.BOOKMARKS_LOGIN)
    onNodeWithTag(TestTags.BOOKMARKS_LOGIN).performClick()
    waitForTag(TestTags.AUTH_DIALOG)
}

/** 로그인 창 이메일 로그인 → "로그인했습니다" · 창 닫힘 */
fun ComposeTestRule.signInViaDialog(s: Strings, email: String, password: String = FakeAccounts.PASSWORD) {
    openLoginFromBookmarks()
    onNodeWithTag(TestTags.AUTH_EMAIL).performTextInput(email)
    onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(password)
    onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
    waitForText(s[Str.TOAST_SIGNED_IN])
    waitForTagGone(TestTags.AUTH_DIALOG)
}

/** 로그인 창 회원가입(전환 → 이름 · 이메일 · 비밀번호) → "로그인했습니다" */
fun ComposeTestRule.signUpViaDialog(s: Strings, name: String, email: String, password: String = FakeAccounts.PASSWORD) {
    openLoginFromBookmarks()
    onNodeWithTag(TestTags.AUTH_SWITCH).performClick()
    waitForTag(TestTags.AUTH_NAME)
    onNodeWithTag(TestTags.AUTH_NAME).performTextInput(name)
    onNodeWithTag(TestTags.AUTH_EMAIL).performTextInput(email)
    onNodeWithTag(TestTags.AUTH_PASSWORD).performTextInput(password)
    onNodeWithTag(TestTags.AUTH_SUBMIT).performClick()
    waitForText(s[Str.TOAST_SIGNED_IN])
    waitForTagGone(TestTags.AUTH_DIALOG)
}

fun ComposeTestRule.exists(tag: String): Boolean = onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

/** 로그아웃 단추: 640 이하 = 북마크 화면 "{이름} · 로그아웃", 넓으면 머리 계정 단추(같은 글 · 동작) */
fun ComposeTestRule.signOutTag(): String = if (exists(TestTags.BM_USER)) TestTags.BM_USER else TestTags.ACCOUNT

/** container 아래 모든 노드가 그 폭 안 + 글 가로 넘침 0(ScreenSizeTest와 같은 규칙) */
fun ComposeTestRule.assertNoHorizontalOverflowIn(where: String, container: String) {
    val root = onNodeWithTag(container, useUnmergedTree = true).fetchSemanticsNode()
    val box = root.boundsInRoot
    val bad = mutableListOf<String>()
    for (n in root.descendantsAndSelf()) {
        val b = n.boundsInRoot
        if (b.width <= 0f) continue
        if (b.left < box.left - 0.5f || b.right > box.right + 0.5f) bad += "${n.tag() ?: n.id} $b"
        if (n.textLayout()?.didOverflowWidth == true) bad += "글 넘침 ${n.tag() ?: n.id}"
    }
    assertEquals(where, emptyList<String>(), bad)
}

/** container 아래 누를 곳(OnClick): 누름 칸 48dp 이상 · 넓힌 칸이 이웃 누를 곳을 덮지 않음(ScreenSizeTest와 같은 규칙, 잘린 것 뺌) */
fun ComposeTestRule.assertTouchTargetsIn(where: String, container: String) {
    val min = with(density) { 48.dp.toPx() } - 1f
    val nodes = onNodeWithTag(container, useUnmergedTree = true).fetchSemanticsNode().descendantsAndSelf()
        .filter { SemanticsActions.OnClick in it.config }
        .filter { n -> n.boundsInRoot.let { it.width >= n.size.width - 1f && it.height >= n.size.height - 1f && it.width > 0f } }
    val small = nodes.filter { it.touchBoundsInRoot.width < min || it.touchBoundsInRoot.height < min }.map { "${it.tag() ?: it.id} ${it.touchBoundsInRoot}" }
    assertEquals("$where 48dp 미만", emptyList<String>(), small)
    val crowded = mutableListOf<String>()
    for (n in nodes.filter { it.boundsInRoot.width < min || it.boundsInRoot.height < min }) {
        for (m in nodes) {
            if (m === n) continue
            val o = n.touchBoundsInRoot.intersect(m.boundsInRoot)
            if (o.width > 1f && o.height > 1f) crowded += "${n.tag() ?: n.id} → ${m.tag() ?: m.id}"
        }
    }
    assertEquals("$where 넓힌 누름 칸이 이웃을 덮음", emptyList<String>(), crowded)
}
