package io.github.jwavascript.issuerank

import androidx.lifecycle.SavedStateHandle
import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.AuthUser
import io.github.jwavascript.issuerank.data.Bookmark
import io.github.jwavascript.issuerank.data.BookmarkView
import io.github.jwavascript.issuerank.data.FakeTransport
import io.github.jwavascript.issuerank.data.GoogleCredential
import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.HttpResponse
import io.github.jwavascript.issuerank.data.HttpTransport
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.IssueRankApi
import io.github.jwavascript.issuerank.data.MemorySessionStorage
import io.github.jwavascript.issuerank.data.Messages
import io.github.jwavascript.issuerank.data.Msg
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.SessionCookies
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.TestFiles
import io.github.jwavascript.issuerank.data.Toast
import io.github.jwavascript.issuerank.data.View
import io.github.jwavascript.issuerank.data.XmlStrings
import io.github.jwavascript.issuerank.data.bodyJson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

/**
 * 계정 · 북마크 · 폴더(A4) = 웹 로그인 창 · applyUser · signOut · googleReturn · api() 개인 경로 · save · unsave · createFolderAndSave · deleteFolder
 * (index.html 586~657 · 950~1105 · 1394~1620). 가짜 서버 = 메모리(ex12 · Better Auth 1.7.7 응답 꼴 — 네트워크 0), 시계 = 2026-11-03 12:00 KST.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelAccountTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestFiles.clock("2026-11-03T03:00:00Z")
    private val d0 = TestFiles.fixture("rankings-d0.json")
    private val ranking: Ranking = ApiJson.ranking(d0)
    private val cookies = SessionCookies(MemorySessionStorage())

    private val email = "app.user@example.invalid"
    private val password = "test-only-password"
    private val pol21 get() = ranking.issueById("2026-11-03-pol-21")!!
    private val eco1 get() = ranking.issueById("2026-11-03-eco-1")!!

    @Before
    fun setMain() = Dispatchers.setMain(dispatcher)

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun vm(backend: HttpTransport): MainViewModel =
        MainViewModel(SavedStateHandle(), IssueRankApi({ BASE }, "1.0.0-test", backend, cookies, io = dispatcher), clock)

    private fun TestScope.notices(vm: MainViewModel): MutableList<Notice> =
        mutableListOf<Notice>().also { list -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notices.toList(list) } }

    private fun TestScope.signIn(vm: MainViewModel, mail: String = email, pw: String = password) {
        vm.requestLogin(null)
        vm.setAuthEmail(mail)
        vm.setAuthPassword(pw)
        vm.submitAuth()
        advanceUntilIdle()
    }

    private val auth get() = vmUnderTest.state.value.auth
    private lateinit var vmUnderTest: MainViewModel

    private fun saved(folder: String) = Toast(Msg.Res(Str.TOAST_SAVED, listOf(folder)), Str.TOAST_VIEW_BOOKMARKS)

    @Test
    fun emailSignUp_closesTheDialog_loadsBookmarksAndFolders_thenToasts() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0)
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        vm.requestLogin(null)
        assertTrue(auth.open)
        assertFalse(auth.signUp)
        vm.switchAuthMode()
        assertTrue(auth.signUp)
        vm.setAuthName("  앱 사용자 ")
        vm.setAuthEmail(" $email\n")
        vm.setAuthPassword(password)
        vm.submitAuth()
        assertTrue(auth.busy)
        vm.submitAuth() // 보내는 동안 두 번째 제출 = 무시(웹 authBusy)
        advanceUntilIdle()
        assertEquals(1, b.paths().count { it == "POST /api/auth/sign-up/email" })
        val body = b.requests.first { it.url.endsWith("sign-up/email") }.bodyJson()
        assertEquals("앱 사용자", body.getString("name")) // 웹 trim
        assertEquals(email, body.getString("email"))
        assertEquals(password, body.getString("password"))
        val s = vm.state.value
        assertEquals(AuthUser("u1", "앱 사용자", email), s.user)
        assertFalse(s.auth.open)
        assertFalse(s.auth.busy)
        assertEquals("", s.auth.password)
        assertEquals(email.trim(), s.auth.email.trim()) // 이메일은 폼에 남음(웹)
        assertTrue(cookies.hasSession(BASE))
        // 웹: 창 닫기 → await applyUser(북마크 · 폴더) → "로그인했습니다"
        val after = b.paths().dropWhile { it != "POST /api/auth/sign-up/email" }.drop(1)
        assertEquals(setOf("GET /api/bookmarks", "GET /api/folders"), after.toSet())
        assertEquals(listOf(Messages.signedIn), notices.map { it.toast })
        assertFalse(auth.toString().contains(password)) // 비밀번호는 글로 남기지 않음
    }

    @Test
    fun emailErrors_areWebMessagesInTheDialog_passwordIsCleared_noToasts() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm, pw = "test-only-wrong")
        assertEquals(Str.AUTH_ERR_CREDENTIALS, auth.error)
        assertEquals("", auth.password)
        assertTrue(auth.open)
        assertFalse(auth.busy)
        // 회원가입 · 같은 이메일 = "이미 가입된 이메일입니다"
        val r0 = auth.revision
        vm.switchAuthMode()
        assertNull(auth.error)
        assertTrue(auth.revision > r0) // 입력 칸 다시 읽기(비밀번호 비움)
        vm.setAuthName("앱 사용자")
        vm.setAuthPassword(password)
        vm.submitAuth(); advanceUntilIdle()
        assertEquals(Str.AUTH_ERR_EXISTS, auth.error)
        // 짧은 비밀번호(서버 PASSWORD_TOO_SHORT)
        vm.setAuthEmail("new.user@example.invalid")
        vm.setAuthPassword("short")
        vm.submitAuth(); advanceUntilIdle()
        assertEquals(Str.AUTH_ERR_PASSWORD_LENGTH, auth.error)
        // 이름 공백 = "이름을 입력해 주세요", 요청 0 · 바쁨 0
        val sent = b.requests.size
        vm.setAuthName(" \t")
        vm.setAuthPassword(password)
        vm.submitAuth()
        assertEquals(Str.AUTH_ERR_NAME, auth.error)
        assertFalse(auth.busy)
        assertEquals(sent, b.requests.size)
        // 네트워크 = "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요"
        b.down = true
        vm.setAuthName("새 사용자")
        vm.setAuthPassword(password)
        vm.submitAuth(); advanceUntilIdle()
        assertEquals(Str.AUTH_ERR_GENERIC, auth.error)
        b.down = false
        // 닫기 → 비밀번호 비움 · 이메일 남음 → 다시 열면 로그인 모드 · 오류 없음
        vm.closeAuth()
        assertFalse(auth.open)
        vm.requestLogin(null)
        assertFalse(auth.signUp)
        assertEquals("new.user@example.invalid", auth.email)
        assertEquals("", auth.password)
        assertNull(auth.error)
        // 웹 maxlength: 이름 100 · 비밀번호 128
        vm.setAuthPassword("p".repeat(200))
        assertEquals(128, auth.password.length)
        vm.switchAuthMode()
        vm.setAuthName("n".repeat(101))
        assertEquals(100, auth.name.length)
        assertNull(vm.state.value.user)
        assertEquals(emptyList<Notice>(), notices) // 창 안 오류 줄만(토스트 0)
        assertEquals("이메일 또는 비밀번호를 확인해 주세요", XmlStrings[Str.AUTH_ERR_CREDENTIALS])
    }

    @Test
    fun whileSending_resubmitCloseSwitchAndReopenAreIgnored_thenTheLoginApplies() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val gate = CompletableDeferred<Unit>().also { b.gates["POST /api/auth/sign-in/email"] = it }
        val vm = vm(b).also { vmUnderTest = it }
        vm.start(null); advanceUntilIdle()
        vm.requestLogin("2026-11-03-pol-21")
        vm.setAuthEmail(email)
        vm.setAuthPassword(password)
        vm.submitAuth(); runCurrent()
        assertTrue(auth.busy)
        vm.submitAuth()
        vm.closeAuth() // 웹: 보내는 동안 닫기 · Esc 막힘
        vm.switchAuthMode()
        vm.requestLogin(null)
        vm.onAccount() // 비로그인 = 로그인 창 열기 → 바쁨이라 무시
        runCurrent()
        assertTrue(auth.open)
        assertFalse(auth.signUp)
        assertEquals("2026-11-03-pol-21", auth.issueId)
        assertEquals(1, b.paths().count { it == "POST /api/auth/sign-in/email" })
        gate.complete(Unit); advanceUntilIdle()
        assertNotNull(vm.state.value.user)
        assertFalse(auth.open)
        // 이메일 로그인 뒤에는 북마크하려던 이슈 상세를 열지 않음(웹 — 그 일은 Google 돌아옴만)
        assertNull(vm.state.value.selectedId)
    }

    @Test
    fun signOut_clearsTheDeviceSession_evenWhenTheServerFails() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        assertTrue(cookies.hasSession(BASE))
        b.signOutStatus = 500
        vm.onAccount() // 머리 계정 단추(로그인 상태) = 로그아웃
        vm.onAccount() // 보내는 동안 다시 = 무시
        advanceUntilIdle()
        assertEquals(1, b.paths().count { it == "POST /api/auth/sign-out" })
        assertEquals(BASE, b.requests.last { it.url.endsWith("sign-out") }.header("Origin"))
        assertNull(vm.state.value.user)
        assertFalse(cookies.hasSession(BASE)) // 서버가 실패해도 기기 세션은 지움(계획 3절)
        assertEquals(Messages.signedOut, notices.last().toast)
        // 다시 로그인 → 북마크 화면 640 이하 "{이름} · 로그아웃"(signOut) → 서버 성공(그 세션도 지움 — 앞의 실패한 세션 하나만 남음)
        b.signOutStatus = 200
        assertEquals(1, b.sessionCount(email))
        signIn(vm)
        assertEquals(2, b.sessionCount(email))
        vm.signOut(); advanceUntilIdle()
        assertNull(vm.state.value.user)
        assertFalse(cookies.hasSession(BASE))
        assertEquals(1, b.sessionCount(email))
        assertEquals(listOf(Messages.signedIn, Messages.signedOut, Messages.signedIn, Messages.signedOut), notices.map { it.toast })
    }

    @Test
    fun google_resultMessages_placeholderIsUnavailable_andSuccessOpensTheBookmarkedIssue() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        // 비로그인 북마크 → "북마크하려면 로그인이 필요합니다" + "로그인" → 그 이슈로 로그인 창(웹 openAuth(id))
        vm.onBookmark(pol21)
        assertEquals(Messages.bookmarkNeedsLogin, notices.single().toast)
        notices.single().onAction!!.invoke()
        assertTrue(auth.open)
        assertEquals(pol21.id, auth.issueId)
        val cases = listOf(
            GoogleCredential.Cancelled to Str.GOOGLE_CANCELLED,
            GoogleCredential.Failed to Str.GOOGLE_FAILED,
            GoogleCredential.Unavailable to Str.GOOGLE_UNAVAILABLE, // 자리표시자 ID(C3) — 실제 구현이 돌려주는 값
        )
        for ((credential, message) in cases) {
            vm.googleSignIn { credential }
            assertTrue(auth.busy)
            advanceUntilIdle()
            assertEquals(message, auth.error)
            assertFalse(auth.busy)
            assertTrue(auth.open)
        }
        assertEquals(0, b.paths().count { it == "POST /api/auth/sign-in/social" }) // 계정 창 단계 실패 = 요청 0
        // 같은 이메일의 이메일 계정 = 잇지 않음(D26)
        vm.googleSignIn { nonce -> GoogleCredential.Token(b.googleToken(email, nonce)) }
        advanceUntilIdle()
        assertEquals(Str.GOOGLE_NOT_LINKED, auth.error)
        assertNull(vm.state.value.user)
        assertFalse(cookies.hasSession(BASE))
        // 성공: 창 닫기 → 북마크 · 폴더 → 북마크하려던 이슈 상세(웹 Google 돌아옴 issue) → "로그인했습니다"
        vm.googleSignIn { nonce -> GoogleCredential.Token(b.googleToken("google.user@example.invalid", nonce)) }
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals("google.user@example.invalid", s.user!!.email)
        assertFalse(s.auth.open)
        assertNull(s.auth.error)
        assertEquals(pol21.id, s.selectedId)
        assertEquals(Messages.signedIn, notices.last().toast)
        val social = b.requests.last { it.url.endsWith("sign-in/social") }.bodyJson()
        assertEquals("google", social.getString("provider"))
        assertEquals(43, social.getJSONObject("idToken").getString("nonce").length) // 앱이 만든 nonce를 그대로
    }

    @Test
    fun bookmark_pickerSaveToFolder_viewBookmarks_unsave_undoKeepsTheFolder() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val fa = b.folder(email, "반도체·AI")
        val fb = b.folder(email, "부동산·금리")
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        assertEquals(listOf("반도체·AI", "부동산·금리"), vm.state.value.folders.map { it.name }) // 서버 순서
        vm.onBookmark(pol21)
        assertEquals(pol21.id, vm.state.value.picker!!.issueId)
        vm.onBookmark(pol21) // 같은 단추 다시 = 닫기
        assertNull(vm.state.value.picker)
        vm.onBookmark(pol21)
        vm.saveToFolder(fb)
        assertNull(vm.state.value.picker) // 웹 save: 먼저 closePop
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(listOf(pol21.id to fb), s.bookmarks.map { it.id to it.folderId })
        assertEquals(listOf(0, 1), s.folders.map { it.count })
        assertTrue(s.isBookmarked(pol21.id))
        assertEquals(1, s.bookmarkBadge)
        val toast = notices.last()
        assertEquals(saved("부동산·금리"), toast.toast)
        toast.onAction!!.invoke() // "북마크 보기" = 그 폴더 칩으로 북마크 화면
        assertEquals(View.BOOKMARKS, vm.state.value.view)
        assertEquals(fb, vm.state.value.folderFilter)
        // 해제 → "북마크를 해제했습니다" + "되돌리기" → 지운 북마크의 폴더(fb)로 다시
        vm.onBookmark(pol21)
        assertNull(vm.state.value.picker)
        advanceUntilIdle()
        assertEquals(emptyList<Bookmark>(), vm.state.value.bookmarks)
        assertEquals("DELETE /api/bookmarks/${pol21.id}", b.paths().last { it.startsWith("DELETE") })
        val undo = notices.last()
        assertEquals(Toast(Msg.Res(Str.TOAST_UNSAVED), Str.TOAST_UNDO), undo.toast)
        undo.onAction!!.invoke(); advanceUntilIdle()
        assertEquals(listOf(pol21.id to fb), vm.state.value.bookmarks.map { it.id to it.folderId })
        assertEquals(fb, b.requests.last { it.method == "POST" && it.url.endsWith("/api/bookmarks") }.bodyJson().getString("folderId"))
        assertEquals(Toast(Msg.Res(Str.TOAST_UNSAVED), Str.TOAST_UNDO), notices.last().toast) // 되돌리기 뒤 토스트 없음(웹)
        assertEquals(fa, vm.state.value.folders.first().id)
    }

    @Test
    fun newFolder_trimmed1to16_17thCharacterRejected_thenSavedIntoIt() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        assertEquals(emptyList<Any>(), vm.state.value.folders) // 새 계정 = 폴더 0(고르기 = 새 폴더만)
        vm.onBookmark(pol21)
        vm.setFolderName("  \t")
        vm.createFolder(); advanceUntilIdle() // 앞뒤 공백뿐 = 아무것도 안 함
        assertEquals(0, b.paths().count { it == "POST /api/folders" })
        assertNotNull(vm.state.value.picker)
        val sixteen = "가".repeat(16)
        vm.setFolderName(sixteen + "나") // 17번째 글자는 안 들어감(웹 maxlength 16)
        assertEquals(sixteen, vm.state.value.picker!!.name)
        vm.createFolder(); advanceUntilIdle()
        val created = b.requests.single { it.method == "POST" && it.url.endsWith("/api/folders") }
        assertEquals(sixteen, created.bodyJson().getString("name"))
        val folder = vm.state.value.folders.single()
        assertEquals(sixteen, folder.name)
        assertEquals(listOf(pol21.id to folder.id), vm.state.value.bookmarks.map { it.id to it.folderId })
        assertNull(vm.state.value.picker)
        assertEquals(saved(sixteen), notices.last().toast)
        // 한 글자(앞뒤 공백 뺌)
        vm.onBookmark(eco1)
        vm.setFolderName(" 가 ")
        vm.createFolder(); advanceUntilIdle()
        assertEquals("가", b.requests.last { it.method == "POST" && it.url.endsWith("/api/folders") }.bodyJson().getString("name"))
        assertEquals(listOf(sixteen, "가"), vm.state.value.folders.map { it.name })
        // 서버 400(웹 글 그대로 토스트 · 고르기 창 그대로) — 서버가 이름을 거절하면
        b.rejectFolderNames = true
        vm.onBookmark(ranking.issueById("2026-11-03-soc-2")!!)
        vm.setFolderName("나중에")
        vm.createFolder(); advanceUntilIdle()
        assertEquals(Toast(Msg.Server("폴더 이름은 1~16자로 입력해 주세요")), notices.last().toast)
        assertNotNull(vm.state.value.picker)
    }

    @Test
    fun personal401_logsOut_andToastsLoginRequired_whoseButtonOpensTheDialog() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val fa = b.folder(email, "반도체·AI")
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        b.expireSessions() // 가짜 서버 /__test/expire-sessions와 같음
        vm.onBookmark(pol21)
        vm.saveToFolder(fa); advanceUntilIdle()
        val s = vm.state.value
        assertNull(s.user)
        assertEquals(emptyList<Bookmark>(), s.bookmarks)
        assertEquals(emptyList<Any>(), s.folders)
        assertEquals(Messages.loginRequired, notices.last().toast)
        assertEquals(Str.LOGIN, notices.last().toast.action)
        notices.last().onAction!!.invoke()
        assertTrue(vm.state.value.auth.open)
        assertNull(vm.state.value.auth.issueId)
        assertEquals(1, notices.count { it.toast == Messages.loginRequired })
    }

    @Test
    fun lateResponsesAfterTheAccountChanged_areDropped() = runTest(dispatcher) {
        val other = "other.user@example.invalid"
        val b = FakeBackend(ranking, d0).apply {
            account(email, password, "앱 사용자")
            account(other, password, "다른 사용자")
        }
        val fa = b.folder(email, "반도체·AI")
        val fo = b.folder(other, "다른 폴더")
        b.bookmark(other, eco1, fo)
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        // A의 저장 요청이 늦게 옴 → 그사이 로그아웃 · B 로그인 → 늦은 응답(401이어도)은 버림(웹 stale)
        val gate = CompletableDeferred<Unit>().also { b.gates["POST /api/bookmarks"] = it }
        vm.onBookmark(pol21)
        vm.saveToFolder(fa); runCurrent()
        vm.signOut(); advanceUntilIdle()
        signIn(vm, mail = other)
        assertEquals(listOf(eco1.id), vm.state.value.bookmarks.map { it.id })
        gate.complete(Unit); advanceUntilIdle()
        val s = vm.state.value
        assertEquals("다른 사용자", s.user!!.name)
        assertEquals(listOf(eco1.id), s.bookmarks.map { it.id })
        assertEquals(listOf("다른 폴더"), s.folders.map { it.name })
        assertEquals(listOf(Messages.signedIn, Messages.signedOut, Messages.signedIn), notices.map { it.toast })
    }

    @Test
    fun folderDelete_noteHasTheCount_cancelSendsNothing_successToasts_404Reloads() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val fa = b.folder(email, "반도체·AI")
        val fb = b.folder(email, "나중에 읽기")
        b.bookmark(email, pol21, fa)
        b.bookmark(email, eco1, fa)
        val vm = vm(b).also { vmUnderTest = it }
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        vm.selectFolder(fb)
        assertEquals(fb, vm.state.value.effectiveFilter)
        vm.requestFolderDelete(fb)
        assertEquals("이 폴더는 비어 있습니다. 삭제하면 되돌릴 수 없습니다.", BookmarkView.deleteNote(vm.state.value.folderDelete!!.folder, XmlStrings))
        vm.cancelFolderDelete()
        assertNull(vm.state.value.folderDelete)
        assertEquals(0, b.paths().count { it.startsWith("DELETE /api/folders") }) // 취소 = 요청 0
        vm.selectFolder(fa)
        vm.requestFolderDelete(fa)
        assertEquals("폴더 안의 북마크 2개도 함께 삭제됩니다. 삭제하면 되돌릴 수 없습니다.", BookmarkView.deleteNote(vm.state.value.folderDelete!!.folder, XmlStrings))
        vm.confirmFolderDelete()
        assertTrue(vm.state.value.folderDelete!!.busy)
        vm.cancelFolderDelete() // 보내는 중 = 무시
        vm.confirmFolderDelete() // 두 번째 = 무시
        assertNotNull(vm.state.value.folderDelete)
        advanceUntilIdle()
        assertEquals(1, b.paths().count { it == "DELETE /api/folders/$fa" })
        val s = vm.state.value
        assertNull(s.folderDelete)
        assertEquals(listOf(fb), s.folders.map { it.id })
        assertEquals(emptyList<Bookmark>(), s.bookmarks)
        assertEquals(BookmarkView.ALL, s.folderFilter)
        assertEquals(Toast(Msg.Res(Str.TOAST_FOLDER_DELETED, listOf("반도체·AI"))), notices.last().toast)
        // 404(다른 기기에서 이미 지움): 서버 글 토스트 + 다시 받기 → 그 칩이 사라짐
        b.deleteFolderBehindTheApp(email, fb)
        vm.requestFolderDelete(fb)
        vm.confirmFolderDelete(); advanceUntilIdle()
        assertEquals(Toast(Msg.Server("폴더를 찾을 수 없습니다")), notices.last().toast)
        assertEquals(emptyList<Any>(), vm.state.value.folders)
        assertNull(vm.state.value.folderDelete)
    }

    @Test
    fun accountChange_resetsTheFolderChip_andClosesPickerAndFolderDialog() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val fa = b.folder(email, "반도체·AI")
        val vm = vm(b).also { vmUnderTest = it }
        vm.start(null); advanceUntilIdle()
        signIn(vm)
        vm.selectFolder(fa)
        vm.onBookmark(pol21)
        vm.requestFolderDelete(fa)
        vm.signOut(); advanceUntilIdle()
        val s = vm.state.value
        assertEquals(BookmarkView.ALL, s.folderFilter)
        assertNull(s.picker)
        assertNull(s.folderDelete)
        assertNull(s.user)
    }

    @Test
    fun restoredSession_firstRenderWaitsForBookmarks_andClosesALoginDialogLeftOpen() = runTest(dispatcher) {
        val b = FakeBackend(ranking, d0).apply { account(email, password, "앱 사용자") }
        val fa = b.folder(email, "반도체·AI")
        b.bookmark(email, pol21, fa)
        b.login(email, cookies) // 기기에 세션이 남아 있는 채로 시작
        val gate = CompletableDeferred<Unit>().also { b.gates["GET /api/bookmarks"] = it }
        val vm = vm(b).also { vmUnderTest = it }
        vm.start(null)
        vm.requestLogin(null) // 확인 전에 연 로그인 창
        advanceUntilIdle()
        assertEquals(Content.LOADING, vm.state.value.content) // 웹 Promise.all(… restoreSession → await applyUser → loadBookmarks)
        gate.complete(Unit); advanceUntilIdle()
        val s = vm.state.value
        assertEquals(Content.READY, s.content)
        assertEquals(listOf(pol21.id), s.bookmarks.map { it.id })
        assertTrue(s.isBookmarked(pol21.id)) // 첫 그림부터 타일 · 카드 표시
        assertFalse(s.auth.open) // 로그인이 확인되면 로그인 창은 닫힘
    }

    /** 04 9절 3 다시 봄: get-session이 네트워크로 실패해도 랭킹을 그렸으면 웹처럼 토스트(오프라인 안내일 때만 생략 — MainViewModelTest) */
    @Test
    fun sessionNetworkFailure_toastsWhenTheRankingWasDrawn() = runTest(dispatcher) {
        val vm = vm(FakeTransport { r ->
            if (r.url.endsWith("get-session")) throw IOException("test-only offline") else FakeTransport.json(200, d0)
        })
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        assertEquals(Content.READY, vm.state.value.content)
        assertEquals(listOf(Messages.sessionFailed), notices.map { it.toast })
    }

    companion object {
        const val BASE = "http://127.0.0.1:3100"
    }
}

/**
 * 메모리 가짜 서버(시험 전용 — 네트워크 0): ex12 라우트 · Better Auth 1.7.7 응답 꼴(가짜 서버 README 4절)만큼.
 * 세션 = Set-Cookie better-auth.session_token(값 test-only-…), 개인 API는 쿠키만 봄(없음 · 만료 = 401 {"message":"로그인이 필요합니다"}).
 * Google ID 토큰 = "test-only-google|이메일|nonce"(가짜 서버 /__test/google-id-token 자리 — 서명 검사 대신 nonce · 같은 이메일 규칙만).
 */
class FakeBackend(private val ranking: Ranking, private val rankingJson: String) : HttpTransport {
    val requests = mutableListOf<HttpRequest>()

    /** "메서드 경로" → 이 문이 열릴 때까지 응답을 미룸 */
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
    var down = false
    var signOutStatus = 200
    var rejectFolderNames = false

    private class Account(val user: AuthUser, val password: String?)

    private val accounts = mutableMapOf<String, Account>()
    private val sessions = mutableMapOf<String, String>()
    private val folders = mutableMapOf<String, MutableList<Pair<String, String>>>()
    private val bookmarks = mutableMapOf<String, MutableList<Bookmark>>()
    private var seq = 0
    private var savedAt = Instant.parse("2026-11-03T02:00:00Z")

    fun paths(): List<String> = requests.map { "${it.method} ${it.url.removePrefix(MainViewModelAccountTest.BASE)}" }

    fun account(email: String, password: String?, name: String) {
        accounts[email] = Account(AuthUser("u${accounts.size + 1}", name, email), password)
    }

    fun folder(email: String, name: String): String = "f${++seq}".also { folders.getOrPut(email) { mutableListOf() } += it to name }

    fun bookmark(email: String, issue: Issue, folderId: String) {
        bookmarks.getOrPut(email) { mutableListOf() } += Bookmark(issue, folderId, nextSavedAt())
    }

    fun googleToken(email: String, nonce: String) = "test-only-google|$email|$nonce"

    fun expireSessions() = sessions.clear()

    fun sessionCount(email: String) = sessions.values.count { it == email }

    fun deleteFolderBehindTheApp(email: String, folderId: String) {
        folders[email]?.removeAll { it.first == folderId }
    }

    /** 앱 밖에서 이미 로그인한 세션을 기기 쿠키에 넣음(다시 연 앱) */
    fun login(email: String, cookies: SessionCookies) {
        cookies.onResponse(MainViewModelAccountTest.BASE, listOf(newSession(email)))
    }

    private fun nextSavedAt(): String = savedAt.also { savedAt = savedAt.plusSeconds(60) }.toString()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        requests += request
        val path = request.url.removePrefix(MainViewModelAccountTest.BASE)
        gates["${request.method} $path"]?.await()
        if (down) throw IOException("test-only offline")
        val email = request.header("Cookie")?.substringAfter("better-auth.session_token=", "")?.let(sessions::get)
        return when {
            path == "/api/rankings" || path == "/api/rankings?date=2026-11-03" -> FakeTransport.json(200, rankingJson)
            path.startsWith("/api/rankings") -> FakeTransport.json(404, """{"message":"해당 날짜의 랭킹 데이터가 없습니다"}""")
            path == "/api/auth/get-session" -> FakeTransport.json(200, email?.let { session(it) } ?: "null")
            path == "/api/auth/sign-up/email" -> signUp(request.bodyJson())
            path == "/api/auth/sign-in/email" -> signIn(request.bodyJson())
            path == "/api/auth/sign-in/social" -> social(request.bodyJson())
            path == "/api/auth/sign-out" -> signOut(request)
            path.startsWith("/api/bookmarks") || path.startsWith("/api/folders") ->
                if (email == null) FakeTransport.json(401, """{"message":"로그인이 필요합니다"}""") else personal(request, path, email)
            else -> FakeTransport.json(404, "{}")
        }
    }

    private fun newSession(email: String): String {
        val token = "test-only-session-${++seq}"
        sessions[token] = email
        return "better-auth.session_token=$token; Max-Age=604800; Path=/; HttpOnly; SameSite=Lax"
    }

    private fun userJson(u: AuthUser) = JSONObject().put("id", u.id).put("name", u.name).put("email", u.email)
        .put("emailVerified", false).put("image", JSONObject.NULL)

    private fun session(email: String) = JSONObject().put("session", JSONObject().put("userId", accounts.getValue(email).user.id))
        .put("user", userJson(accounts.getValue(email).user)).toString()

    private fun signedIn(email: String) = FakeTransport.json(
        200, JSONObject().put("token", "test-only").put("user", userJson(accounts.getValue(email).user)).toString(),
        "Set-Cookie" to newSession(email),
    )

    private fun signUp(body: JSONObject): HttpResponse {
        val email = body.getString("email")
        val password = body.getString("password")
        return when {
            password.length < 8 -> FakeTransport.json(400, """{"code":"PASSWORD_TOO_SHORT","message":"Password too short"}""")
            email in accounts -> FakeTransport.json(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL","message":"User already exists"}""")
            else -> {
                account(email, password, body.getString("name"))
                signedIn(email)
            }
        }
    }

    private fun signIn(body: JSONObject): HttpResponse {
        val email = body.getString("email")
        val ok = accounts[email]?.password?.let { it == body.getString("password") } == true
        return if (ok) signedIn(email) else FakeTransport.json(401, """{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}""")
    }

    private fun social(body: JSONObject): HttpResponse {
        val idToken = body.getJSONObject("idToken")
        val parts = idToken.getString("token").split('|')
        if (body.getString("provider") != "google" || parts.size != 3 || parts[2] != idToken.getString("nonce")) {
            return FakeTransport.json(401, """{"code":"INVALID_TOKEN","message":"Invalid token"}""")
        }
        val email = parts[1]
        val existing = accounts[email]
        if (existing != null && existing.password != null) {
            return FakeTransport.json(401, """{"code":"OAUTH_LINK_ERROR","message":"account not linked"}""")
        }
        if (existing == null) account(email, null, "구글 시험 사용자")
        return signedIn(email)
    }

    private fun signOut(request: HttpRequest): HttpResponse {
        if (signOutStatus != 200) return FakeTransport.json(signOutStatus, "{}")
        request.header("Cookie")?.substringAfter("better-auth.session_token=", "")?.let(sessions::remove)
        return FakeTransport.json(200, """{"success":true}""", "Set-Cookie" to "better-auth.session_token=; Max-Age=0; Path=/")
    }

    private fun issueJson(i: Issue) = JSONObject().put("id", i.id).put("date", i.date).put("section", i.section.key).put("rank", i.rank)
        .put("title", i.title).put("summary", i.summary?.let { JSONArray(it) } ?: JSONObject.NULL).put("articleCount", i.articleCount).put("url", i.url)

    private fun personal(request: HttpRequest, path: String, email: String): HttpResponse {
        val mine = bookmarks.getOrPut(email) { mutableListOf() }
        val myFolders = folders.getOrPut(email) { mutableListOf() }
        return when {
            request.method == "GET" && path == "/api/bookmarks" -> FakeTransport.json(
                200, JSONArray(mine.sortedByDescending { it.savedAt }.map { issueJson(it.issue).put("folderId", it.folderId).put("savedAt", it.savedAt) }).toString(),
            )
            request.method == "POST" && path == "/api/bookmarks" -> {
                val body = request.bodyJson()
                val issue = ranking.issueById(body.getString("issueId"))
                val folderId = body.getString("folderId")
                when {
                    issue == null -> FakeTransport.json(404, """{"message":"이슈를 찾을 수 없습니다"}""")
                    myFolders.none { it.first == folderId } -> FakeTransport.json(404, """{"message":"폴더를 찾을 수 없습니다"}""")
                    mine.any { it.id == issue.id } -> FakeTransport.json(409, """{"message":"이미 북마크한 이슈입니다"}""")
                    else -> {
                        val bookmark = Bookmark(issue, folderId, nextSavedAt())
                        mine += bookmark
                        FakeTransport.json(201, issueJson(issue).put("folderId", folderId).put("savedAt", bookmark.savedAt).toString())
                    }
                }
            }
            request.method == "DELETE" && path.startsWith("/api/bookmarks/") ->
                if (mine.removeAll { it.id == path.removePrefix("/api/bookmarks/") }) FakeTransport.empty(204)
                else FakeTransport.json(404, """{"message":"북마크를 찾을 수 없습니다"}""")
            request.method == "GET" && path == "/api/folders" -> FakeTransport.json(
                200, JSONArray(myFolders.map { (id, name) -> JSONObject().put("id", id).put("name", name).put("count", mine.count { it.folderId == id }) }).toString(),
            )
            request.method == "POST" && path == "/api/folders" -> {
                val name = request.bodyJson().getString("name").trim()
                if (rejectFolderNames || name.isEmpty() || name.length > 16) {
                    FakeTransport.json(400, """{"message":"폴더 이름은 1~16자로 입력해 주세요"}""")
                } else {
                    val id = folder(email, name)
                    FakeTransport.json(201, JSONObject().put("id", id).put("name", name).put("count", 0).toString())
                }
            }
            request.method == "DELETE" && path.startsWith("/api/folders/") -> {
                val id = path.removePrefix("/api/folders/")
                if (myFolders.removeAll { it.first == id }) {
                    mine.removeAll { it.folderId == id }
                    FakeTransport.empty(204)
                } else {
                    FakeTransport.json(404, """{"message":"폴더를 찾을 수 없습니다"}""")
                }
            }
            else -> FakeTransport.json(404, "{}")
        }
    }
}
