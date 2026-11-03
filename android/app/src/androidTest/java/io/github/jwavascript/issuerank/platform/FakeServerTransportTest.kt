package io.github.jwavascript.issuerank.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jwavascript.issuerank.BuildConfig
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.ErrorKind
import io.github.jwavascript.issuerank.data.GoogleCredential
import io.github.jwavascript.issuerank.data.GoogleSignInResult
import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.HttpResponse
import io.github.jwavascript.issuerank.data.IssueRankApi
import io.github.jwavascript.issuerank.data.ResponseTooLargeException
import io.github.jwavascript.issuerank.data.SessionCookies
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.signInWithGoogle
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * 실제 전송(HttpURLConnection) + 세션(Keystore) + API를 가짜 서버(android/.data/fake-server, PC 127.0.0.1:3100 ← adb reverse tcp:3100 tcp:3100)로.
 * 서버 요청 기록(GET /__test/requests)으로 Origin(POST · DELETE만) · Cookie(우리 출처 · 로그인 뒤만) · User-Agent를 확인한다.
 * 데스크톱 JDK의 HttpURLConnection은 Origin 머리를 조용히 버려서 이 확인은 기기에서만. 지금은 컴파일만(C2 — 기기 시험은 나중).
 */
@RunWith(AndroidJUnit4::class)
class FakeServerTransportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val base = BuildConfig.SERVER_URL
    private val transport = UrlConnectionTransport()
    private val storage = KeystoreSessionStorage(context)
    private val cookies = SessionCookies(storage)
    private val api = IssueRankApi({ base }, BuildConfig.VERSION_NAME, transport, cookies)
    private val password = "Device-test-only-pass-5518"

    private fun control(method: String, path: String, body: String? = null): JSONObject = runBlocking {
        val r: HttpResponse = try {
            transport.execute(HttpRequest(method, base + path, listOf("Content-Type" to "application/json"), body?.toByteArray(), 5_000, 10_000))
        } catch (e: IOException) {
            throw AssertionError(
                "가짜 서버($base)에 닿지 않음 — PC에서 android/.data/fake-server/README.md대로 띄우고 `adb reverse tcp:3100 tcp:3100` (${e.javaClass.simpleName})",
            )
        }
        assertEquals("$method $path", 200, r.status)
        JSONObject(r.text)
    }

    private fun requests(): List<JSONObject> = control("GET", "/__test/requests").getJSONArray("requests").let { a -> List(a.length()) { a.getJSONObject(it) } }

    @Before
    fun reset() {
        assertEquals("http://127.0.0.1:3100", base)
        storage.clear()
        control("POST", "/__test/reset")
    }

    @After
    fun cleanUp() {
        storage.clear()
        control("POST", "/__test/mode", """{"mode":"normal"}""")
    }

    @Test
    fun originOnlyOnWrites_cookieOnlyAfterLogin_userAgent() = runBlocking {
        val ranking = (api.rankings() as ApiResult.Ok).value
        assertTrue(api.signUpEmail("기기 시험", "device.test@example.invalid", password) is ApiResult.Ok)
        assertTrue(cookies.hasSession(base))
        val folder = (api.createFolder("기기 폴더") as ApiResult.Ok).value
        val issueId = ranking.sections[0].issues[0].id
        assertEquals(ApiResult.Ok(Unit), api.saveBookmark(issueId, folder.id))
        assertEquals(listOf(issueId), (api.bookmarks() as ApiResult.Ok).value.map { it.id })
        assertEquals(ApiResult.Ok(Unit), api.deleteBookmark(issueId))
        assertEquals(ApiResult.Ok(Unit), api.deleteFolder(folder.id))
        assertTrue(api.signOut() is ApiResult.Ok)
        assertFalse(cookies.hasSession(base))
        val after = api.bookmarks() as ApiResult.Fail
        assertEquals(ErrorKind.UNAUTHORIZED, after.error.kind)
        assertEquals("로그인이 필요합니다", after.error.message)

        val log = requests()
        assertEquals(
            listOf("GET /api/rankings", "POST /api/auth/sign-up/email", "POST /api/folders", "POST /api/bookmarks", "GET /api/bookmarks",
                "DELETE /api/bookmarks/$issueId", "DELETE /api/folders/${folder.id}", "POST /api/auth/sign-out", "GET /api/bookmarks"),
            log.map { "${it.getString("method")} ${it.getString("path")}" },
        )
        for (e in log) {
            val writes = e.getString("method") == "POST" || e.getString("method") == "DELETE"
            assertEquals("$e", if (writes) base else null, if (e.isNull("origin")) null else e.getString("origin"))
            assertEquals("$e", "issuerank-android/${BuildConfig.VERSION_NAME}", e.getString("userAgent"))
        }
        // 쿠키: 가입 전 · 로그아웃 뒤 0, 그 사이 = 세션 토큰 하나
        assertEquals(listOf(false, false, true, true, true, true, true, true, false), log.map { it.getBoolean("cookie") })
        for (e in log.filter { it.getBoolean("cookie") }) assertEquals("[\"better-auth.session_token\"]", e.getJSONArray("cookieNames").toString())
        assertEquals(listOf(200, 200, 201, 201, 200, 204, 204, 200, 401), log.map { it.getInt("status") })
    }

    @Test
    fun errorBodiesComeFromErrorStream_limitAndWidget() = runBlocking {
        val none = api.rankings("2000-01-01") as ApiResult.Fail
        assertEquals(ErrorKind.NOT_FOUND, none.error.kind)
        assertEquals("해당 날짜의 랭킹 데이터가 없습니다", none.error.message)
        var tooLarge = false
        try {
            transport.execute(HttpRequest("GET", "$base/api/rankings", emptyList(), null, 5_000, 4_000, maxResponseBytes = 100))
        } catch (e: ResponseTooLargeException) { tooLarge = true }
        assertTrue(tooLarge)
        val widget = (api.widget() as ApiResult.Ok).value.widget
        assertEquals(30, widget.items.size)
        assertEquals("/#list", widget.morePath)
        val log = requests()
        assertEquals(listOf(false, false, false), log.map { it.getBoolean("cookie") })
        assertNull(if (log.last().isNull("origin")) null else log.last().getString("origin"))
    }

    @Test
    fun serverDownIsNetworkFailure() = runBlocking {
        control("POST", "/__test/mode", """{"mode":"down"}""")
        assertEquals(ErrorKind.NETWORK, (api.rankings() as ApiResult.Fail).error.kind)
        control("POST", "/__test/mode", """{"mode":"normal"}""")
        assertTrue(api.rankings() is ApiResult.Ok)
    }

    @Test
    fun googleIdTokenWithFakeCredential() = runBlocking {
        val nonce = "device-test-only-nonce"
        val token = control("POST", "/__test/google-id-token", """{"email":"device.google@example.invalid","nonce":"$nonce"}""").getString("idToken")
        val ok = signInWithGoogle({ GoogleCredential.Token(token) }, api) { nonce }
        assertTrue("$ok", ok is GoogleSignInResult.SignedIn)
        assertTrue(cookies.hasSession(base))
        assertEquals("device.google@example.invalid", ((api.session() as ApiResult.Ok).value)!!.email)
        api.signOut()
        // 같은 이메일의 이메일 계정 → 잇지 않음
        assertTrue(api.signUpEmail("이메일 가입", "shared.device@example.invalid", password) is ApiResult.Ok)
        api.signOut()
        val shared = control("POST", "/__test/google-id-token", """{"email":"shared.device@example.invalid","nonce":"$nonce"}""").getString("idToken")
        assertEquals(GoogleSignInResult.Error(Str.GOOGLE_NOT_LINKED), signInWithGoogle({ GoogleCredential.Token(shared) }, api) { nonce })
        assertEquals(GoogleSignInResult.Error(Str.GOOGLE_FAILED), signInWithGoogle({ GoogleCredential.Token(token) }, api) { "other-nonce" })
        assertFalse(cookies.hasSession(base))
    }

    @Test
    fun sessionSurvivesRestartThroughKeystore() = runBlocking {
        assertTrue(api.signUpEmail("다시 시작", "restart.device@example.invalid", password) is ApiResult.Ok)
        val again = IssueRankApi({ base }, BuildConfig.VERSION_NAME, transport, SessionCookies(KeystoreSessionStorage(context)))
        assertEquals("restart.device@example.invalid", ((again.session() as ApiResult.Ok).value)!!.email)
        control("POST", "/__test/expire-sessions")
        assertEquals(ApiResult.Ok(null), again.session())
    }
}
