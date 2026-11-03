package io.github.jwavascript.issuerank.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** 세션 쿠키(계획 3절 R4 · 검증 4): 두 이름 · 갱신 · 만료 · Max-Age=0 · 받은 출처에만 · 값은 글로 안 남김 */
class SessionCookiesTest {
    private val prod = "https://issuerank-kappa.vercel.app"
    private val local = "http://127.0.0.1:3100"
    private var now = Instant.parse("2026-11-03T00:00:00Z")
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
    }
    private val storage = MemorySessionStorage()
    private val jar = SessionCookies(storage, clock)

    // Better Auth(better-call _serialize) 꼴 — 값은 시험 전용
    private fun session(name: String, value: String, maxAge: Long = 604800, secure: Boolean = false) =
        "$name=$value; Max-Age=$maxAge; Path=/; HttpOnly${if (secure) "; Secure" else ""}; SameSite=Lax"

    @Test
    fun storesBothSessionCookieNamesAndSendsOnlyToThatOrigin() {
        jar.onResponse(prod, listOf(session("__Secure-better-auth.session_token", "test-only-prod.sig%3D", secure = true)))
        assertEquals("__Secure-better-auth.session_token=test-only-prod.sig%3D", jar.cookieHeader(prod))
        assertNull(jar.cookieHeader(local))
        assertNull(jar.cookieHeader("https://evil.example"))
        assertNull(jar.cookieHeader("http://issuerank-kappa.vercel.app")) // 같은 호스트라도 scheme가 다르면 다른 출처
        assertTrue(jar.hasSession(prod))

        jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-local")))
        assertEquals("better-auth.session_token=test-only-local", jar.cookieHeader(local))
        assertNull(jar.cookieHeader(prod)) // 세션은 하나(마지막으로 로그인한 서버)
    }

    @Test
    fun ignoresOtherCookies() {
        jar.onResponse(local, listOf(
            "better-auth.session_data=test-only-cache; Max-Age=300; Path=/; HttpOnly; SameSite=Lax",
            "better-auth.oauth_state=test-only-state; Max-Age=600; Path=/; HttpOnly; SameSite=Lax",
            "better-auth.dont_remember=true; Path=/",
            "garbage-without-equals",
            "=empty-name",
        ))
        assertNull(jar.cookieHeader(local))
        assertNull(storage.load())
    }

    @Test
    fun refreshReplacesValueAndExpiry() {
        jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-v1")))
        assertEquals(TestFiles.millis("2026-11-10T00:00:00Z"), storage.load()!!.expiresAtMs)
        now = Instant.parse("2026-11-05T00:00:00Z") // get-session이 하루 지나 다시 줌(7일 연장)
        jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-v2")))
        assertEquals("better-auth.session_token=test-only-v2", jar.cookieHeader(local))
        assertEquals(TestFiles.millis("2026-11-12T00:00:00Z"), storage.load()!!.expiresAtMs)
    }

    @Test
    fun expiredSessionIsNotSentAndIsWiped() {
        jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-v1", maxAge = 60)))
        now = now.plusSeconds(59)
        assertTrue(jar.cookieHeader(local) != null)
        now = now.plusSeconds(1)
        assertNull(jar.cookieHeader(local))
        assertNull(storage.load())
    }

    @Test
    fun maxAgeZeroEmptyValueOrPastExpiresDeletes() {
        val deletions = listOf(
            "better-auth.session_token=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax",
            "better-auth.session_token=test-only-x; Max-Age=0; Path=/",
            "better-auth.session_token=test-only-x; Max-Age=-1; Path=/",
            "better-auth.session_token=; Path=/",
            "better-auth.session_token=test-only-x; Expires=Mon, 02 Nov 2026 00:00:00 GMT; Path=/",
        )
        for (line in deletions) {
            jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-v1")))
            jar.onResponse(local, listOf(line))
            assertNull(line, jar.cookieHeader(local))
            assertNull(line, storage.load())
        }
    }

    @Test
    fun maxAgeWinsOverExpires() {
        jar.onResponse(local, listOf("better-auth.session_token=test-only-x; Expires=Mon, 02 Nov 2026 00:00:00 GMT; Max-Age=100; Path=/"))
        assertEquals("better-auth.session_token=test-only-x", jar.cookieHeader(local))
        jar.onResponse(local, listOf("better-auth.session_token=test-only-y; Max-Age=0; Expires=Wed, 03 Nov 2027 00:00:00 GMT"))
        assertNull(jar.cookieHeader(local))
        jar.onResponse(local, listOf("better-auth.session_token=test-only-z; Expires=Tue, 10 Nov 2026 00:00:00 GMT"))
        assertEquals(TestFiles.millis("2026-11-10T00:00:00Z"), storage.load()!!.expiresAtMs)
    }

    @Test
    fun deletionFromAnotherOriginOrNameDoesNotTouchSession() {
        jar.onResponse(prod, listOf(session("__Secure-better-auth.session_token", "test-only-prod", secure = true)))
        jar.onResponse(local, listOf("__Secure-better-auth.session_token=; Max-Age=0; Path=/"))
        jar.onResponse(prod, listOf("better-auth.session_token=; Max-Age=0; Path=/"))
        assertEquals("__Secure-better-auth.session_token=test-only-prod", jar.cookieHeader(prod))
    }

    /** sign-out 200의 실제 Set-Cookie 4줄(ex12 native-client.test ③) */
    @Test
    fun signOutResponseClearsSession() {
        jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-v1")))
        jar.onResponse(local, listOf("session_token", "session_data", "oauth_state", "dont_remember")
            .map { "better-auth.$it=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax" })
        assertNull(jar.cookieHeader(local))
        assertNull(storage.load())
    }

    @Test
    fun persistsThroughStorageAndClear() {
        jar.onResponse(local, listOf(session("better-auth.session_token", "test-only-v1")))
        val again = SessionCookies(storage, clock) // 앱 다시 시작
        assertEquals("better-auth.session_token=test-only-v1", again.cookieHeader(local))
        again.clear()
        assertNull(again.cookieHeader(local))
        assertNull(storage.load())
        assertFalse(again.hasSession(local))
    }

    @Test
    fun valuesNeverAppearInText() {
        val secret = "test-only-secret-value"
        val s = StoredSession(local, "better-auth.session_token", secret, 1L)
        val parsed = SetCookie.parse(session("better-auth.session_token", secret))!!
        val request = HttpRequest("GET", "$local/api/bookmarks", listOf("Cookie" to "better-auth.session_token=$secret"), "{\"password\":\"$secret\"}".toByteArray(), 5000, 10000)
        for (text in listOf(s.toString(), parsed.toString(), request.toString())) assertFalse(text, text.contains(secret))
        assertEquals(secret, parsed.value)
        assertEquals(604800L, parsed.maxAgeSec)
    }
}
