package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.FakeTransport.Companion.empty
import io.github.jwavascript.issuerank.data.FakeTransport.Companion.json
import io.github.jwavascript.issuerank.data.TestFiles.fixture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/** 요청 규칙(계획 3절 그 밖 머리 · 세션) + 결과 종류(가짜 전송 — 네트워크 0) */
class IssueRankApiTest {
    private var base = "http://127.0.0.1:3100"
    private val storage = MemorySessionStorage()
    private val cookies = SessionCookies(storage, TestFiles.clock("2026-11-03T00:00:00Z"))
    private fun api(transport: HttpTransport) = IssueRankApi({ base }, "1.0.0-test", transport, cookies, Dispatchers.Unconfined)
    private val signedIn = "better-auth.session_token=test-only-session.sig; Max-Age=604800; Path=/; HttpOnly; SameSite=Lax"

    private fun ok(body: String = "{}", status: Int = 200) = FakeTransport { json(status, body) }

    /** 응답을 기다리며 멈춰 있는 전송(취소 시험) */
    private fun waiting(gate: CompletableDeferred<HttpResponse>) = object : HttpTransport {
        override suspend fun execute(request: HttpRequest): HttpResponse = gate.await()
    }

    @Test
    fun getRequestHeaders_noOriginNoContentType() = runTest {
        val t = ok(fixture("rankings-d0.json"))
        val r = api(t).rankings()
        assertTrue(r is ApiResult.Ok)
        val req = t.last
        assertEquals("GET", req.method)
        assertEquals("http://127.0.0.1:3100/api/rankings", req.url)
        assertEquals(listOf("User-Agent" to "issuerank-android/1.0.0-test", "Accept" to "application/json"), req.headers)
        assertNull(req.body)
        assertEquals(5_000, req.connectTimeoutMs)
        assertEquals(10_000, req.readTimeoutMs)
        assertNull(req.maxResponseBytes)
        api(t).rankings("2026-11-02")
        assertEquals("http://127.0.0.1:3100/api/rankings?date=2026-11-02", t.last.url)
    }

    @Test
    fun postAndDeleteCarryOrigin_getsDoNot() = runTest {
        val t = FakeTransport { r ->
            when {
                r.url.endsWith("/api/folders") && r.method == "POST" -> json(201, fixture("folder-created.json"))
                r.url.endsWith("/api/bookmarks") && r.method == "POST" -> json(201, "{}")
                r.method == "DELETE" -> empty(204)
                r.url.contains("/api/auth/sign-") -> json(200, fixture("auth-sign-in.json"), "Set-Cookie" to signedIn)
                r.url.endsWith("get-session") -> json(200, fixture("session.json"))
                r.url.endsWith("/api/bookmarks") -> json(200, fixture("bookmarks.json"))
                r.url.endsWith("/api/folders") -> json(200, fixture("folders.json"))
                else -> json(200, fixture("rankings-d0.json"))
            }
        }
        val a = api(t)
        a.signUpEmail("앱 사용자", "app.user@example.invalid", "test-only-password")
        a.signInEmail("app.user@example.invalid", "test-only-password")
        a.signInGoogle("test-only-id-token", "test-only-nonce")
        a.session()
        a.bookmarks()
        a.saveBookmark("2026-11-03-pol-21", "f1")
        a.deleteBookmark("2026-11-03-pol-21")
        a.folders()
        a.createFolder("반도체·AI")
        a.deleteFolder("f52bfc5e7-6572-463d-8695-ad6b48737215")
        a.rankings()
        a.signOut()
        for (r in t.requests) {
            val writes = r.method == "POST" || r.method == "DELETE"
            assertEquals("$r Origin", if (writes) "http://127.0.0.1:3100" else null, r.header("Origin"))
            assertEquals("$r Content-Type", if (r.body != null) "application/json" else null, r.header("Content-Type"))
            assertEquals("issuerank-android/1.0.0-test", r.header("User-Agent"))
            assertEquals("application/json", r.header("Accept"))
        }
        assertEquals(
            listOf(
                "POST /api/auth/sign-up/email", "POST /api/auth/sign-in/email", "POST /api/auth/sign-in/social", "GET /api/auth/get-session",
                "GET /api/bookmarks", "POST /api/bookmarks", "DELETE /api/bookmarks/2026-11-03-pol-21", "GET /api/folders", "POST /api/folders",
                "DELETE /api/folders/f52bfc5e7-6572-463d-8695-ad6b48737215", "GET /api/rankings", "POST /api/auth/sign-out",
            ),
            t.requests.map { "${it.method} ${it.url.removePrefix(base)}" },
        )
        // 본문(웹과 같은 칸)
        val up = t.requests[0].bodyJson()
        assertEquals(listOf("app.user@example.invalid", "test-only-password", "앱 사용자"), listOf(up.getString("email"), up.getString("password"), up.getString("name")))
        val google = t.requests[2].bodyJson()
        assertEquals("google", google.getString("provider"))
        assertEquals("test-only-id-token", google.getJSONObject("idToken").getString("token"))
        assertEquals("test-only-nonce", google.getJSONObject("idToken").getString("nonce"))
        assertEquals(setOf("issueId", "folderId"), t.requests[5].bodyJson().keyNames())
        assertEquals("반도체·AI", t.requests[8].bodyJson().getString("name"))
        assertEquals(0, t.requests.last().bodyJson().length()) // sign-out 본문 {}
        assertNull(t.requests[6].body)
        // 첫 가입 전에는 쿠키 0, 받은 뒤 모든 요청에 같은 출처 쿠키
        assertNull(t.requests[0].header("Cookie"))
        assertTrue(t.requests.drop(1).all { it.header("Cookie") == "better-auth.session_token=test-only-session.sig" })
        assertNull(cookies.cookieHeader(base)) // 로그아웃 = 로컬 삭제
    }

    @Test
    fun cookieGoesOnlyToTheOriginThatSetIt() = runTest {
        val t = FakeTransport { r -> if (r.url.endsWith("sign-in/email")) json(200, fixture("auth-sign-in.json"), "Set-Cookie" to signedIn) else json(200, fixture("session.json")) }
        val a = api(t)
        a.signInEmail("e", "p")
        base = "https://issuerank-kappa.vercel.app" // debug 도구에서 서버를 바꿈
        a.session()
        assertNull(t.last.header("Cookie"))
        assertEquals("https://issuerank-kappa.vercel.app", t.last.url.removeSuffix("/api/auth/get-session"))
        base = "http://127.0.0.1:3100/"
        a.session()
        assertEquals("better-auth.session_token=test-only-session.sig", t.last.header("Cookie"))
        assertEquals("http://127.0.0.1:3100/api/auth/get-session", t.last.url) // 끝 / 정리
    }

    @Test
    fun originIsSchemeHostPort() {
        assertEquals("https://issuerank-kappa.vercel.app", Web.originOf("https://issuerank-kappa.vercel.app"))
        assertEquals("https://issuerank-kappa.vercel.app", Web.originOf("HTTPS://Issuerank-Kappa.Vercel.App:443/x?y#z"))
        assertEquals("http://127.0.0.1:3100", Web.originOf("http://127.0.0.1:3100/"))
        assertEquals("http://localhost", Web.originOf("http://localhost:80"))
        assertNull(Web.originOf("ftp://example.invalid"))
        assertNull(Web.originOf("not a url"))
        assertNull(Web.originOf("/api"))
    }

    @Test
    fun invalidServerAddressFailsWithoutRequest() = runTest {
        base = "127.0.0.1:3100"
        val t = ok()
        assertEquals(ApiResult.Fail(ApiError(ErrorKind.NETWORK)), api(t).rankings())
        assertTrue(t.requests.isEmpty())
    }

    @Test
    fun statusKindsAndServerMessages() = runTest {
        val cases = listOf(
            Triple(400, """{"message":"오늘 이후 날짜는 조회할 수 없습니다"}""", ApiError(ErrorKind.BAD_REQUEST, 400, null, "오늘 이후 날짜는 조회할 수 없습니다")),
            Triple(401, """{"message":"로그인이 필요합니다"}""", ApiError(ErrorKind.UNAUTHORIZED, 401, null, "로그인이 필요합니다")),
            Triple(401, """{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}""", ApiError(ErrorKind.UNAUTHORIZED, 401, "INVALID_EMAIL_OR_PASSWORD", "Invalid email or password")),
            Triple(403, """{"code":"MISSING_OR_NULL_ORIGIN"}""", ApiError(ErrorKind.FORBIDDEN, 403, "MISSING_OR_NULL_ORIGIN", null)),
            Triple(404, """{"message":"해당 날짜의 랭킹 데이터가 없습니다"}""", ApiError(ErrorKind.NOT_FOUND, 404, null, "해당 날짜의 랭킹 데이터가 없습니다")),
            Triple(409, """{"message":"이미 북마크한 이슈입니다"}""", ApiError(ErrorKind.CONFLICT, 409, null, "이미 북마크한 이슈입니다")),
            Triple(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL","message":"User already exists. Use another email."}""", ApiError(ErrorKind.UNPROCESSABLE, 422, "USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL", "User already exists. Use another email.")),
            Triple(429, """{"message":"Too many requests. Please try again later."}""", ApiError(ErrorKind.TOO_MANY_REQUESTS, 429, null, "Too many requests. Please try again later.")),
            Triple(500, "Internal Server Error", ApiError(ErrorKind.SERVER, 500)),
            Triple(502, """{"message":"API 서버에 연결할 수 없습니다"}""", ApiError(ErrorKind.SERVER, 502, null, "API 서버에 연결할 수 없습니다")),
            Triple(302, "", ApiError(ErrorKind.OTHER, 302)),
        )
        for ((status, body, expected) in cases) {
            val t = FakeTransport { json(status, body) }
            assertEquals("$status $body", ApiResult.Fail(expected), api(t).bookmarks())
            assertEquals(1, t.requests.size) // 리디렉션 · 다시 시도 0
        }
    }

    @Test
    fun networkFailuresAndBadBodies() = runTest {
        for (e in listOf(IOException("test-only refused"), SocketTimeoutException("test-only timeout"))) {
            assertEquals(ApiResult.Fail(ApiError(ErrorKind.NETWORK)), api(FakeTransport.failing(e)).rankings())
        }
        assertEquals(ApiResult.Fail(ApiError(ErrorKind.NETWORK)), api(FakeTransport { throw IllegalStateException("test-only") }).folders())
        assertEquals(ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE)), api(FakeTransport { throw ResponseTooLargeException(10) }).rankings())
        for (body in listOf("", "<html>", "{}", """{"date":"2026-11-03"}""", "[]")) {
            assertEquals(body, ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE, 200)), api(ok(body)).rankings())
        }
        // 200인데 user 없음(웹 !data?.user) → BAD_RESPONSE
        assertEquals(ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE, 200)), api(ok("""{"redirect":true}""")).signInEmail("e", "p"))
        // 204 · 201 본문 없음/무시
        assertEquals(ApiResult.Ok(Unit), api(FakeTransport { empty(204) }).deleteFolder("f1"))
        assertEquals(ApiResult.Ok(Unit), api(ok("not json", 201)).saveBookmark("2026-11-03-pol-21", "f1"))
        assertEquals(ApiResult.Ok(null), api(ok("null")).session())
    }

    @Test
    fun pathSegmentsAreEncodedLikeEncodeURIComponent() = runTest {
        val t = FakeTransport { empty(204) }
        api(t).deleteFolder("f/../x y")
        assertEquals("$base/api/folders/f%2F..%2Fx%20y", t.last.url)
        assertEquals("a%20b%2F%E3%84%B1~*'()!-_.", Web.encodeURIComponent("a b/ㄱ~*'()!-_."))
        assertEquals("%F0%9F%98%80%26%3D%3F%23%2B", Web.encodeURIComponent("😀&=?#+"))
    }

    @Test
    fun signOutClearsLocalSessionEvenWhenServerFails() = runTest {
        for (fail in listOf<HttpTransport>(
            FakeTransport { json(403, """{"code":"MISSING_OR_NULL_ORIGIN"}""") },
            FakeTransport.failing(),
            FakeTransport { json(500, "Internal Server Error") },
        )) {
            cookies.onResponse(base, listOf(signedIn))
            assertTrue(cookies.hasSession(base))
            val r = api(fail).signOut()
            assertTrue(r is ApiResult.Fail)
            assertNull(cookies.cookieHeader(base))
            assertNull(storage.load())
        }
    }

    @Test
    fun signOutClearsLocalSessionWhenCancelled() = runTest {
        cookies.onResponse(base, listOf(signedIn))
        val gate = CompletableDeferred<HttpResponse>()
        val a = IssueRankApi({ base }, "1.0.0-test", waiting(gate), cookies, StandardTestDispatcher(testScheduler))
        val job = async { a.signOut() }
        testScheduler.runCurrent()
        job.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(job.isCancelled)
        assertNull(cookies.cookieHeader(base))
    }

    @Test
    fun cancellationIsNotReportedAsNetworkFailure() = runTest {
        val gate = CompletableDeferred<HttpResponse>()
        val a = IssueRankApi({ base }, "1.0.0-test", waiting(gate), cookies, StandardTestDispatcher(testScheduler))
        val job = async { a.rankings() }
        testScheduler.runCurrent()
        job.cancel()
        testScheduler.advanceUntilIdle()
        var thrown: Throwable? = null
        try { job.await() } catch (e: CancellationException) { thrown = e }
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun loadRankingAnsweredRules() = runTest {
        val loaded = api(ok(fixture("rankings-d0.json"))).loadRanking(null)
        assertTrue(loaded is RankingLoad.Loaded && loaded.answered)
        assertEquals("2026-11-03", loaded.dateAfter(null, "2026-11-04"))
        val none = api(FakeTransport { json(404, """{"message":"해당 날짜의 랭킹 데이터가 없습니다"}""") }).loadRanking("2026-10-31")
        assertTrue(none is RankingLoad.NoData && none.answered)
        assertEquals("2026-10-31", none.dateAfter("2026-10-31", "2026-11-03"))
        assertEquals("2026-11-03", none.dateAfter(null, "2026-11-03"))
        val failed = api(FakeTransport { json(500, "Internal Server Error") }).loadRanking("2026-11-02")
        assertTrue(failed is RankingLoad.Failed)
        assertFalse(failed.answered)
        assertFalse(api(FakeTransport.failing()).loadRanking(null).answered)
    }
}
