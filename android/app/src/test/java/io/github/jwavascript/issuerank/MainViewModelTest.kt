package io.github.jwavascript.issuerank

import androidx.lifecycle.SavedStateHandle
import io.github.jwavascript.issuerank.data.AppRoutes
import io.github.jwavascript.issuerank.data.FakeTransport
import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.HttpResponse
import io.github.jwavascript.issuerank.data.HttpTransport
import io.github.jwavascript.issuerank.data.IssueRankApi
import io.github.jwavascript.issuerank.data.MemorySessionStorage
import io.github.jwavascript.issuerank.data.Messages
import io.github.jwavascript.issuerank.data.Msg
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.SessionCookies
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.TestFiles
import io.github.jwavascript.issuerank.data.Toast
import io.github.jwavascript.issuerank.data.View
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * 화면 상태(MainViewModel) = 웹 start · loadRanking · setDate · go · openDrawer · issueShortcut 규칙(index.html 586~1620).
 * 가짜 전송(네트워크 0) + 씨앗 D0 fixture, 시계 = 2026-11-03 12:00 KST. 요청 순서 · 오래된 응답 버리기 · 저장된 날짜 · 탭 · 앱 경로까지.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestFiles.clock("2026-11-03T03:00:00Z")
    private val base = "http://127.0.0.1:3100"
    private val d0 = TestFiles.fixture("rankings-d0.json")
    private val d1 = d0.replace("2026-11-03", "2026-11-02")
    private val missing = """{"message":"해당 날짜의 랭킹 데이터가 없습니다"}"""

    @Before
    fun setMain() = Dispatchers.setMain(dispatcher)

    @After
    fun resetMain() = Dispatchers.resetMain()

    /** 가짜 서버 씨앗 꼴: 날짜 생략 · D0 = d0, D-1 = d1, 그 밖 404, get-session = 비로그인 */
    private fun seed(r: HttpRequest): HttpResponse = when (r.url.removePrefix(base)) {
        "/api/auth/get-session" -> FakeTransport.json(200, "null")
        "/api/rankings", "/api/rankings?date=2026-11-03" -> FakeTransport.json(200, d0)
        "/api/rankings?date=2026-11-02" -> FakeTransport.json(200, d1)
        else -> FakeTransport.json(404, missing)
    }

    private fun vm(transport: HttpTransport, saved: SavedStateHandle = SavedStateHandle()): MainViewModel =
        MainViewModel(saved, IssueRankApi({ base }, "1.0.0-test", transport, SessionCookies(MemorySessionStorage()), io = dispatcher), clock)

    private fun FakeTransport.paths(): List<String> = requests.map { it.url.removePrefix(base) }
    private fun FakeTransport.rankingPaths(): List<String> = paths().filter { it.startsWith("/api/rankings") }

    /** 토스트 모으기(뒤 작업은 advanceUntilIdle이 돌리지 않아 Unconfined — 보내는 즉시 받음) */
    private fun TestScope.notices(vm: MainViewModel): MutableList<Notice> =
        mutableListOf<Notice>().also { list -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notices.toList(list) } }

    @Test
    fun firstScreen_isTheMapOfTheLatestRanking() = runTest(dispatcher) {
        val t = FakeTransport(::seed)
        val vm = vm(t)
        assertEquals(Content.LOADING, vm.state.value.content)
        vm.start(null)
        vm.start(null) // 두 번째(돌리기)는 무시
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(View.MAP, s.view)
        assertEquals(Content.READY, s.content)
        assertEquals("2026-11-03", s.date)
        assertEquals(30, s.ranking!!.issueCount)
        assertNull(s.selectedId)
        assertNull(s.user)
        assertEquals(listOf("/api/rankings"), t.rankingPaths())
        assertEquals(setOf("/api/auth/get-session", "/api/rankings"), t.paths().toSet())
        assertTrue(s.dateLine.isToday)
        assertFalse(s.dateLine.nextEnabled)
    }

    @Test
    fun dates_prevNextTodayPick_andFutureIsClampedToToday() = runTest(dispatcher) {
        val t = FakeTransport(::seed)
        val vm = vm(t)
        vm.start(null); advanceUntilIdle()
        vm.prevDay(); advanceUntilIdle()
        assertEquals("2026-11-02", vm.state.value.date)
        assertEquals(Content.READY, vm.state.value.content)
        assertTrue(vm.state.value.dateLine.todayVisible)
        vm.nextDay(); advanceUntilIdle()
        assertEquals("2026-11-03", vm.state.value.date)
        vm.nextDay(); advanceUntilIdle() // 오늘이면 다음 날짜 막힘 — 요청 0
        vm.goToday(); advanceUntilIdle()
        vm.pickDate("2026-11-05"); advanceUntilIdle() // 달력이 막지만 여기서도 오늘로
        vm.pickDate("2026-11-03"); advanceUntilIdle() // 같은 날 = 요청 0
        assertEquals(
            listOf("/api/rankings", "/api/rankings?date=2026-11-02", "/api/rankings?date=2026-11-03", "/api/rankings?date=2026-11-03", "/api/rankings?date=2026-11-03"),
            t.rankingPaths(),
        )
        assertEquals("2026-11-03", vm.state.value.date)
    }

    @Test
    fun missingDay_isTheNoDataScreenWithoutToast() = runTest(dispatcher) {
        val vm = vm(FakeTransport(::seed))
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        vm.pickDate("2026-10-31"); advanceUntilIdle()
        val s = vm.state.value
        assertEquals(Content.NO_DATA, s.content)
        assertNull(s.ranking)
        assertEquals("2026-10-31", s.date)
        assertEquals("", s.dateLine.updated(io.github.jwavascript.issuerank.data.XmlStrings))
        assertEquals(emptyList<Notice>(), notices)
    }

    @Test
    fun serverError_isNoDataPlusTheServerMessage() = runTest(dispatcher) {
        val vm = vm(FakeTransport { r ->
            if (r.url.contains("rankings")) FakeTransport.json(502, """{"message":"API 서버에 연결할 수 없습니다"}""") else seed(r)
        })
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        assertEquals(Content.NO_DATA, vm.state.value.content)
        assertEquals(listOf(Toast(Msg.Server("API 서버에 연결할 수 없습니다"))), notices.map { it.toast })
    }

    @Test
    fun networkFailure_isOffline_andRetryRepeatsTheSameRequest() = runTest(dispatcher) {
        var down = true
        val t = FakeTransport { r -> if (down) throw IOException("test-only offline") else seed(r) }
        val vm = vm(t)
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        vm.prevDay(); advanceUntilIdle()
        assertEquals(Content.OFFLINE, vm.state.value.content)
        assertEquals("2026-11-02", vm.state.value.date)
        assertEquals(emptyList<Notice>(), notices) // 랭킹 · 로그인 확인 모두 네트워크 — 안내 화면만(토스트 0)
        down = false
        vm.retry(); advanceUntilIdle()
        assertEquals(Content.READY, vm.state.value.content)
        assertEquals("2026-11-02", vm.state.value.ranking!!.date)
        assertEquals("/api/rankings?date=2026-11-02", t.rankingPaths().last())
        assertEquals(2, t.paths().count { it == "/api/auth/get-session" }) // 처음 실패 → 다시 시도에서 한 번 더
    }

    @Test
    fun olderResponseIsDropped_onlyTheLastRequestIsShown() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val t = object : HttpTransport {
            override suspend fun execute(request: HttpRequest): HttpResponse {
                if (request.url.endsWith("date=2026-11-02")) gate.await()
                return seed(request)
            }
        }
        val vm = vm(t)
        vm.start(null); advanceUntilIdle()
        vm.prevDay() // 느린 요청(D-1)
        advanceUntilIdle()
        vm.goToday() // 다음 요청이 먼저 옴
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("2026-11-03", vm.state.value.date)
        assertEquals("2026-11-03", vm.state.value.ranking!!.date)
    }

    @Test
    fun firstRender_waitsForTheSessionCheck_likeWebPromiseAll() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val t = object : HttpTransport {
            override suspend fun execute(request: HttpRequest): HttpResponse {
                if (request.url.endsWith("get-session")) gate.await()
                return seed(request)
            }
        }
        val vm = vm(t)
        vm.start("/?issue=2026-11-03-pol-21"); advanceUntilIdle()
        assertEquals(Content.LOADING, vm.state.value.content) // 랭킹은 왔지만 로그인 확인 전에는 그리지 않음
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(Content.READY, vm.state.value.content)
        assertEquals("2026-11-03-pol-21", vm.state.value.selectedId)
        vm.openIssue("2026-11-03-eco-1")
        vm.prevDay(); advanceUntilIdle() // 그 뒤 요청은 기다리지 않음
        assertEquals("2026-11-02", vm.state.value.date)
    }

    @Test
    fun appRoute_issueOpensTheDetailOfThatDate() = runTest(dispatcher) {
        val t = FakeTransport(::seed)
        val vm = vm(t)
        vm.start("/?issue=2026-11-02-eco-1"); advanceUntilIdle()
        val s = vm.state.value
        assertEquals(View.MAP, s.view)
        assertEquals("2026-11-02", s.date)
        assertEquals("2026-11-02-eco-1", s.selectedId)
        assertEquals("반도체 수출 석 달 연속 증가세", s.selected!!.title)
        assertEquals(listOf("/api/rankings?date=2026-11-02"), t.rankingPaths())
    }

    @Test
    fun appRoute_unknownOrMalformedIssue_toastsNotFound() = runTest(dispatcher) {
        for (path in listOf("/?issue=2026-11-03-pol-99", "/?issue=abc", "/?issue=2026-10-31-pol-1")) {
            val t = FakeTransport(::seed)
            val vm = vm(t)
            val notices = notices(vm)
            vm.start(path); advanceUntilIdle()
            assertNull(path, vm.state.value.selectedId)
            assertEquals(path, listOf(AppRoutes.notFoundToast), notices.map { it.toast })
        }
    }

    @Test
    fun appRoute_networkFailureKeepsTheShortcutForRetry() = runTest(dispatcher) {
        var down = true
        val vm = vm(FakeTransport { r -> if (down) throw IOException("test-only offline") else seed(r) })
        val notices = notices(vm)
        vm.start("/?issue=2026-11-03-pol-21"); advanceUntilIdle()
        assertEquals(Content.OFFLINE, vm.state.value.content)
        assertNull(vm.state.value.selectedId)
        down = false
        vm.retry(); advanceUntilIdle()
        assertEquals("2026-11-03-pol-21", vm.state.value.selectedId)
        assertEquals(emptyList<Notice>(), notices)
    }

    @Test
    fun appRoute_listAndHome_andNewIntentWhileRunning() = runTest(dispatcher) {
        val t = FakeTransport(::seed)
        val vm = vm(t)
        vm.start("/#list"); advanceUntilIdle()
        assertEquals(View.LIST, vm.state.value.view)
        assertNull(vm.state.value.selectedId)
        vm.prevDay(); advanceUntilIdle()
        vm.openPath("/?issue=2026-11-03-it-3"); advanceUntilIdle() // 떠 있는 화면(onNewIntent)
        assertEquals(View.MAP, vm.state.value.view)
        assertEquals("2026-11-03", vm.state.value.date)
        assertEquals("2026-11-03-it-3", vm.state.value.selectedId)
        vm.openPath("/#list"); advanceUntilIdle()
        assertEquals(View.LIST, vm.state.value.view)
        assertNull(vm.state.value.selectedId)
        vm.openPath("/"); advanceUntilIdle()
        assertEquals(View.MAP, vm.state.value.view)
        assertEquals("/api/rankings", t.rankingPaths().last()) // 첫 화면 = 최근 수집일
    }

    @Test
    fun savedState_restoresViewAndDate_andIsWrittenBack() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf(MainViewModel.KEY_VIEW to "list", MainViewModel.KEY_DATE to "2026-11-02"))
        val t = FakeTransport(::seed)
        val vm = vm(t, saved)
        assertEquals(View.LIST, vm.state.value.view) // 첫 그림부터
        vm.start(null); advanceUntilIdle()
        assertEquals(listOf("/api/rankings?date=2026-11-02"), t.rankingPaths())
        assertEquals("2026-11-02", vm.state.value.date)
        vm.selectView(View.BOOKMARKS)
        vm.goToday(); advanceUntilIdle()
        assertEquals("bookmarks", saved.get<String>(MainViewModel.KEY_VIEW))
        assertEquals("2026-11-03", saved.get<String>(MainViewModel.KEY_DATE))
    }

    @Test
    fun detailZoomTabs_closeTheDetailLikeTheWeb() = runTest(dispatcher) {
        val vm = vm(FakeTransport(::seed))
        vm.start(null); advanceUntilIdle()
        vm.openIssue("2026-11-03-pol-21")
        assertEquals("2026-11-03-pol-21", vm.state.value.selectedId)
        vm.zoom(SectionKey.ECO)
        assertNull(vm.state.value.selectedId)
        assertEquals(SectionKey.ECO, vm.state.value.zoom)
        vm.openIssue("2026-11-03-nope-1") // 랭킹에 없으면 무시
        assertNull(vm.state.value.selectedId)
        vm.openIssue("2026-11-02-cul-5") // 대체 섹션 카드
        vm.selectView(View.LIST)
        assertNull(vm.state.value.selectedId)
        vm.openIssue("2026-11-03-pol-21")
        vm.prevDay(); advanceUntilIdle() // 날짜를 바꾸면 닫힘(웹 setDate → closeDrawer)
        assertNull(vm.state.value.selectedId)
        assertEquals(SectionKey.ECO, vm.state.value.zoom) // 확대 · 섹션 탭 · 요약 펼치기는 날짜를 넘어 그대로
        vm.selectSection(SectionKey.IT); vm.setExpanded(false)
        assertEquals(SectionKey.IT, vm.state.value.section)
        assertFalse(vm.state.value.expanded)
    }

    @Test
    fun bookmarkWhileLoggedOut_toastsWithLoginButton() = runTest(dispatcher) {
        val vm = vm(FakeTransport(::seed))
        val notices = notices(vm)
        vm.start(null); advanceUntilIdle()
        vm.onBookmark(vm.state.value.ranking!!.issueById("2026-11-03-pol-21")!!)
        advanceUntilIdle()
        assertEquals(listOf(Messages.bookmarkNeedsLogin), notices.map { it.toast })
        assertNotNull(notices.single().onAction)
        notices.single().onAction!!.invoke() // A4 자리(로그인 창) — 지금은 아무것도 안 함
    }

    @Test
    fun sessionCheck_failureToastsUnlessNetwork_andUserIsApplied() = runTest(dispatcher) {
        val failing = vm(FakeTransport { r -> if (r.url.endsWith("get-session")) FakeTransport.json(500, "{}") else seed(r) })
        val failed = notices(failing)
        failing.start(null); advanceUntilIdle()
        assertEquals(listOf(Messages.sessionFailed), failed.map { it.toast })

        val session = TestFiles.fixture("session.json")
        val signedIn = vm(FakeTransport { r -> if (r.url.endsWith("get-session")) FakeTransport.json(200, session) else seed(r) })
        signedIn.start("/?issue=2026-11-03-pol-21"); advanceUntilIdle()
        assertNotNull(signedIn.state.value.user)
        assertEquals("2026-11-03-pol-21", signedIn.state.value.selectedId) // 로그인 확인(상세 닫음) 뒤에 열림
    }

    @Test
    fun noticeButtons_areOnlyTheThreeKinds() {
        for (button in Notice.BUTTONS) Notice(Toast(Msg.Res(Str.TOAST_UNSAVED), button))
        assertEquals(setOf(Str.TOAST_UNDO, Str.TOAST_VIEW_BOOKMARKS, Str.LOGIN), Notice.BUTTONS)
        var rejected = false
        try { Notice(Toast(Msg.Res(Str.TOAST_UNSAVED), Str.CLOSE)) } catch (e: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }
}
