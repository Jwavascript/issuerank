package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.FakeTransport.Companion.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Google로 계속하기": 가짜 Credential(시험 토큰) → sign-in/social 본문 · 같은 nonce · 결과 문구 */
class GoogleSignInTest {
    private val base = "http://127.0.0.1:3100"
    private val cookies = SessionCookies(MemorySessionStorage())
    private val token = "test-only-header.test-only-payload.test-only-signature"

    private fun api(t: HttpTransport) = IssueRankApi({ base }, "1.0.0-test", t, cookies, Dispatchers.Unconfined)

    @Test
    fun tokenGoesToServerWithTheSameNonce_andSessionIsStored() = runTest {
        val asked = mutableListOf<String>()
        val t = FakeTransport {
            json(200, TestFiles.fixture("auth-social.json"), "Set-Cookie" to "better-auth.session_token=test-only-google.sig; Max-Age=604800; Path=/; HttpOnly; SameSite=Lax")
        }
        val r = signInWithGoogle({ n -> asked += n; GoogleCredential.Token(token) }, api(t)) { "test-only-nonce-1" }
        assertEquals(GoogleSignInResult.SignedIn(AuthUser("test-user-2", "구글 앱 사용자", "o8.google@example.invalid")), r)
        assertEquals(listOf("test-only-nonce-1"), asked)
        val req = t.last
        assertEquals("$base/api/auth/sign-in/social", req.url)
        assertEquals(base, req.header("Origin"))
        val body = req.bodyJson()
        assertEquals(setOf("provider", "idToken"), body.keyNames())
        assertEquals("google", body.getString("provider"))
        assertEquals(token, body.getJSONObject("idToken").getString("token"))
        assertEquals("test-only-nonce-1", body.getJSONObject("idToken").getString("nonce"))
        assertEquals("better-auth.session_token=test-only-google.sig", cookies.cookieHeader(base))
    }

    @Test
    fun cancelOrCredentialFailureSendsNothing() = runTest {
        val t = FakeTransport { error("요청이 가면 안 됨") }
        assertEquals(GoogleSignInResult.Error(Str.GOOGLE_CANCELLED), signInWithGoogle({ GoogleCredential.Cancelled }, api(t)))
        assertEquals(GoogleSignInResult.Error(Str.GOOGLE_FAILED), signInWithGoogle({ GoogleCredential.Failed }, api(t)))
        assertTrue(t.requests.isEmpty())
        assertEquals("Google 로그인을 취소했습니다", XmlStrings[Str.GOOGLE_CANCELLED])
    }

    @Test
    fun serverRejectionsMapToWebMessages() = runTest {
        val cases = listOf(
            json(401, """{"code":"OAUTH_LINK_ERROR","message":"account not linked"}""") to Str.GOOGLE_NOT_LINKED,
            json(401, """{"code":"INVALID_TOKEN","message":"Invalid token"}""") to Str.GOOGLE_FAILED,
            json(401, """{"code":"USER_EMAIL_NOT_FOUND","message":"User email not found"}""") to Str.GOOGLE_FAILED,
            json(404, """{"code":"PROVIDER_NOT_FOUND","message":"Provider not found"}""") to Str.GOOGLE_UNAVAILABLE,
            json(429, """{"message":"Too many requests. Please try again later."}""") to Str.TRY_LATER,
            json(403, """{"code":"INVALID_ORIGIN","message":"Invalid origin"}""") to Str.AUTH_ERR_GENERIC,
        )
        for ((response, expected) in cases) {
            val r = signInWithGoogle({ GoogleCredential.Token(token) }, api(FakeTransport { response }))
            assertEquals(GoogleSignInResult.Error(expected), r)
            assertFalse(cookies.hasSession(base))
        }
        assertEquals(GoogleSignInResult.Error(Str.AUTH_ERR_GENERIC), signInWithGoogle({ GoogleCredential.Token(token) }, api(FakeTransport.failing())))
    }

    /** C3: 웹 클라이언트 ID가 자리표시자면 실제 구현(GoogleCredentialManager)이 Unavailable → "지금은 Google 로그인을 사용할 수 없습니다"(웹 PROVIDER_NOT_FOUND), 요청 0 */
    @Test
    fun placeholderClientIdIsUnavailable_withoutRequests() = runTest {
        val t = FakeTransport { error("요청이 가면 안 됨") }
        assertEquals(GoogleSignInResult.Error(Str.GOOGLE_UNAVAILABLE), signInWithGoogle({ GoogleCredential.Unavailable }, api(t)))
        assertTrue(t.requests.isEmpty())
        assertEquals(Messages.googleStartMessage(404, "PROVIDER_NOT_FOUND"), Str.GOOGLE_UNAVAILABLE)
        assertEquals("지금은 Google 로그인을 사용할 수 없습니다", XmlStrings[Str.GOOGLE_UNAVAILABLE])
        // 앱 google.xml 값(C3 끝, 2026-11-03) = 실제 웹 클라이언트 ID 꼴 · 자리표시자 아님 → 기본 걸이가 Credential Manager를 부름
        val xml = XmlStrings.read(TestFiles.repoFile("android/app/src/main/res/values/google.xml"))
        val id = xml.getValue("google_web_client_id")
        assertFalse(GoogleClientId.isPlaceholder(id))
        assertTrue(id.matches(Regex("""[0-9]{6,}-[a-z0-9]{10,}\.apps\.googleusercontent\.com""")))
        assertTrue(GoogleClientId.isPlaceholder("REPLACE_WITH_WEB_CLIENT_ID.apps.googleusercontent.com"))
        assertFalse(GoogleClientId.isPlaceholder("123456789012-abcdefghijklmnop.apps.googleusercontent.com"))
    }

    @Test
    fun nonceAndTokenText() {
        val a = Nonce.create()
        val b = Nonce.create()
        assertEquals(43, a.length)
        assertTrue(a.matches(Regex("[A-Za-z0-9_-]+")))
        assertNotEquals(a, b)
        assertFalse(GoogleCredential.Token(token).toString().contains("test-only"))
    }
}
