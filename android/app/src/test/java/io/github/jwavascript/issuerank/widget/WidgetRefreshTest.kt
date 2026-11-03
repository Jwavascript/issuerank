package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.FakeTransport
import io.github.jwavascript.issuerank.data.FakeTransport.Companion.json
import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.HttpResponse
import io.github.jwavascript.issuerank.data.IssueRankApi
import io.github.jwavascript.issuerank.data.MemorySessionStorage
import io.github.jwavascript.issuerank.data.SessionCookies
import io.github.jwavascript.issuerank.data.TestFiles
import io.github.jwavascript.issuerank.data.WidgetPayload
import io.github.jwavascript.issuerank.data.WidgetRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * 위젯 데이터(계획 4절 · 03-data 예시 91~93): 받을 때만 받음(dueForRefresh) · 결과 = resolve(받은 것, 캐시) · 캐시는 성공한 원문만(그대로) ·
 * 다음 시각 보관(refreshAfter — 오늘 데이터면 3시간, 아니면 KST 04:30 · 30분 뒤 중 늦은 때) · 9초 넘으면 끊고 캐시. 실제 IssueRankApi + 가짜 전송(네트워크 0).
 */
class WidgetRefreshTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val today = TestFiles.fixture("widget-d0.json") // date 2026-11-03(가짜 서버 씨앗 그대로)
    private val yesterday = TestFiles.fixture("widget-d1.json") // date 2026-11-02
    private val hour = 3_600_000L

    /** 다음 시각을 메모리에(Android = SharedPreferences — 계측 시험) */
    private class MemorySchedule : RefreshSchedule {
        var at: Long? = null
        override fun nextAt(): Long? = at
        override fun setNextAt(atMs: Long) {
            at = atMs
        }
    }

    private var now = TestFiles.millis("2026-11-03T00:00:00Z") // KST 11-03 09:00
    private val schedule = MemorySchedule()
    private val file: File by lazy { File(tmp.root, WidgetStore.CACHE_FILE) }
    private val cache: FileWidgetCache by lazy { FileWidgetCache(file) }
    private val requests = mutableListOf<HttpRequest>()

    private fun refresh(respond: (HttpRequest) -> HttpResponse): WidgetRefresh {
        val transport = FakeTransport { requests += it; respond(it) }
        val api = IssueRankApi({ "http://127.0.0.1:3100" }, "1.0.0-test", transport, SessionCookies(MemorySessionStorage()), Dispatchers.Unconfined)
        return WidgetRefresh(api::widget, cache, schedule) { now }
    }

    private val ok: (HttpRequest) -> HttpResponse = { json(200, today) }
    private val down: (HttpRequest) -> HttpResponse = { throw IOException("test-only connection reset") }
    private val unexpected: (HttpRequest) -> HttpResponse = { throw AssertionError("받을 때가 아닌데 요청함: ${it.url}") }

    @Test
    fun firstTime_fetchesOnce_cachesTheBodyAsIs_andTodaysDataWaitsThreeHours() = runTest {
        val view = refresh(ok).update()
        assertFalse(view.fromCache)
        assertEquals(30, view.widget!!.items.size)
        assertEquals(1, requests.size)
        assertEquals("http://127.0.0.1:3100/api/widget", requests.single().url)
        assertEquals(today, file.readText(Charsets.UTF_8)) // 받은 글자 그대로(writeCache)
        assertEquals(now + 3 * hour, schedule.at) // date = KST 오늘 → 3시간 뒤
    }

    @Test
    fun beforeTheNextTime_noRequest_drawsTheCache() = runTest {
        refresh(ok).update()
        val next = schedule.at!!
        now = next - 1
        val view = refresh(unexpected).update()
        assertTrue(view.fromCache)
        assertEquals(ApiJson.widget(today), view.widget)
        assertEquals(next, schedule.at) // 받지 않았으니 그대로
        assertEquals(1, requests.size)
    }

    @Test
    fun dueButServerDown_drawsTheCache_cacheUntouched_nextTimeFromTheCachedDate() = runTest {
        refresh(ok).update()
        now = schedule.at!! // 정확히 그때 = 받을 때
        val view = refresh(down).update()
        assertTrue(view.fromCache)
        assertEquals(30, view.widget!!.items.size)
        assertEquals(today, file.readText(Charsets.UTF_8))
        assertEquals(WidgetRules.refreshAfter("2026-11-03", now), schedule.at)
        assertEquals(now + 3 * hour, schedule.at) // 캐시 date = 오늘 → 3시간
        assertFalse(File(tmp.root, WidgetStore.CACHE_FILE + ".tmp").exists())
    }

    @Test
    fun downWithoutCache_isTheFailView_writesNothing_andWaitsUntil0430Kst() = runTest {
        now = TestFiles.millis("2026-11-02T18:00:00Z") // KST 11-03 03:00
        val view = refresh(down).update()
        assertTrue(view.failed)
        assertNull(view.widget)
        assertFalse(file.exists())
        assertEquals(TestFiles.millis("2026-11-02T19:30:00Z"), schedule.at) // KST 04:30(30분 뒤보다 늦음)
    }

    @Test
    fun onlySuccessIsCached_errorsAndBadBodiesNeverOverwrite() = runTest {
        refresh(ok).update()
        val padded = today.trimEnd().removeSuffix("}") + ",\"pad\":\"" + "x".repeat(70_000) + "\"}"
        val bad = listOf<(HttpRequest) -> HttpResponse>(
            { json(500, """{"message":"API 서버에 연결할 수 없습니다"}""") },
            { json(404, """{"message":"해당 날짜의 랭킹 데이터가 없습니다"}""") },
            { json(203, today) },
            { json(200, JSONObject(today).put("items", listOf<Any>()).toString()) },
            { json(200, "<html>") },
            { json(200, padded) }, // 64KB 넘음
            down,
        )
        for (respond in bad) {
            now = schedule.at!!
            val view = refresh(respond).update()
            assertTrue(view.fromCache)
            assertEquals(today, file.readText(Charsets.UTF_8))
        }
        assertEquals(1 + bad.size, requests.size)
        // 다음 성공은 덮어씀(어제 → 오늘이 아니라 받은 그대로)
        now = schedule.at!!
        val fresh = refresh { json(200, yesterday) }.update()
        assertFalse(fresh.fromCache)
        assertEquals(yesterday, file.readText(Charsets.UTF_8))
    }

    @Test
    fun yesterdaysDataBeforeMorning_waitsUntil0430_thenEvery30Minutes() = runTest {
        now = TestFiles.millis("2026-11-02T17:00:00Z") // KST 11-03 02:00 — 아직 어제 데이터
        refresh { json(200, yesterday) }.update()
        assertEquals(TestFiles.millis("2026-11-02T19:30:00Z"), schedule.at) // KST 04:30
        now = TestFiles.millis("2026-11-02T19:30:00Z")
        refresh { json(200, yesterday) }.update() // 04:30에도 아직 어제 데이터
        assertEquals(now + hour / 2, schedule.at) // 30분 뒤
        now = schedule.at!!
        refresh(ok).update() // 오늘 데이터가 나옴
        assertEquals(now + 3 * hour, schedule.at)
        assertEquals(3, requests.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // testScheduler.currentTime(가상 시계)
    @Test
    fun slowServer_isCutAtConnectPlusRead_9s_thenTheCache() = runTest {
        refresh(ok).update()
        now = schedule.at!!
        val slow = WidgetRefresh({ delay(60_000); ApiResult.Ok(WidgetPayload(ApiJson.widget(yesterday), yesterday)) }, cache, schedule) { now }
        val started = testScheduler.currentTime
        val view = slow.update()
        assertEquals(9_000L, WidgetRefresh.FETCH_BUDGET_MS)
        assertEquals(WidgetRefresh.FETCH_BUDGET_MS, testScheduler.currentTime - started) // 9초에서 끊음(10초 방송 한도 안)
        assertTrue(view.fromCache)
        assertEquals(today, file.readText(Charsets.UTF_8))
        assertEquals(now + 3 * hour, schedule.at)
    }

    @Test
    fun cached_neverFetches() = runTest {
        assertTrue(refresh(unexpected).cached().failed) // 캐시 없음
        refresh(ok).update()
        val view = refresh(unexpected).cached()
        assertTrue(view.fromCache)
        assertEquals(1, requests.size)
    }

    @Test
    fun fileCache_replacesWholeFile_noTempLeft_clearRemoves() {
        assertNull(cache.read())
        cache.write(today)
        cache.write(yesterday)
        assertEquals(yesterday, cache.read())
        assertEquals(listOf(WidgetStore.CACHE_FILE), tmp.root.list()!!.toList())
        cache.clear()
        assertNull(cache.read())
        // 쓸 수 없는 곳(폴더가 없음)이면 조용히 버림(widget.js 40) — 위젯은 다음 성공 때 다시 씀
        FileWidgetCache(File(tmp.root, "missing/widget-cache.json")).write(today)
        assertFalse(File(tmp.root, "missing").exists())
    }
}
