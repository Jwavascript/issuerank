package io.github.jwavascript.issuerank.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.BuildConfig
import io.github.jwavascript.issuerank.DebugServer
import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.Share
import io.github.jwavascript.issuerank.platform.AndroidStrings
import io.github.jwavascript.issuerank.platform.UrlConnectionTransport
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.rules.ExternalResource
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 가짜 서버(android/.data/fake-server — PC 127.0.0.1:3100 ← `adb reverse tcp:3100 tcp:3100`) 제어: /__test/reset · mode · requests.
 * 서버가 안 닿으면 분명한 글로 실패. 지금은 컴파일만(C2 — 기기 시험은 나중).
 */
object FakeServer {
    val base: String = BuildConfig.SERVER_URL
    private val transport = UrlConnectionTransport()

    const val UNREACHABLE = "가짜 서버(127.0.0.1:3100)가 닿지 않음 — PC에서 서버를 띄우고(android/.data/fake-server/README.md) `adb reverse tcp:3100 tcp:3100`"

    fun call(method: String, path: String, body: String? = null): JSONObject = runBlocking {
        val r = try {
            transport.execute(HttpRequest(method, base + path, listOf("Content-Type" to "application/json"), body?.toByteArray(), 5_000, 10_000))
        } catch (e: IOException) {
            throw AssertionError("$UNREACHABLE (${e.javaClass.simpleName})")
        }
        assertEquals("$method $path", 200, r.status)
        JSONObject(r.text)
    }

    /** 씨앗 다시 · mode normal · 기록 비움 → 오늘(D0, 서버의 KST) */
    fun reset(): String = call("POST", "/__test/reset").getString("today")

    fun down() { call("POST", "/__test/mode", """{"mode":"down"}""") }

    fun normal() { call("POST", "/__test/mode", """{"mode":"normal"}""") }

    fun requests(): List<JSONObject> = call("GET", "/__test/requests").getJSONArray("requests").let { a -> List(a.length()) { a.getJSONObject(it) } }

    /** 앱이 부른 랭킹 경로(쿼리 포함) 순서대로 */
    fun rankingPaths(): List<String> = requests().map { it.getString("path") }.filter { it.startsWith("/api/rankings") }

    /** 화면 기대값을 만들 때 쓰는 랭킹(서버에서 직접 — 앱과 같은 해석) */
    fun ranking(date: String? = null): Ranking = runBlocking {
        val r = transport.execute(HttpRequest("GET", base + "/api/rankings" + (date?.let { "?date=$it" } ?: ""), emptyList(), null, 5_000, 10_000))
        assertEquals(200, r.status)
        ApiJson.ranking(r.text)
    }
}

/**
 * 화면 시험 앞(Activity를 띄우기 전 — @Rule(order = 0)): debug 서버 주소 덮어쓰기 지움(가짜 서버로) · 기기 세션 지움 · 걸이를 기록용으로 ·
 * 가짜 서버 reset(down = true면 그 뒤 mode down). 뒤: mode normal · 걸이 기본.
 */
class FakeServerRule(private val startDown: Boolean = false) : ExternalResource() {
    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    val strings = AndroidStrings(context)
    lateinit var today: String
        private set

    /** Custom Tab 대신 연 주소 · 공유 창 대신 보낸 글 */
    val opened = CopyOnWriteArrayList<String>()
    val shared = CopyOnWriteArrayList<Share.Data>()

    fun day(offset: Long): String = KstDate.addDays(today, offset)

    override fun before() {
        assertEquals("http://127.0.0.1:3100", FakeServer.base)
        context.getSharedPreferences(DebugServer.PREFS, Context.MODE_PRIVATE).edit().remove(DebugServer.KEY_SERVER_URL).commit()
        val graph = AppGraph.get(context)
        graph.cookies.clear()
        graph.resetHooks()
        graph.openUrl = { _, url -> opened += url }
        graph.share = { _, data -> shared += data }
        today = FakeServer.reset()
        if (startDown) FakeServer.down()
    }

    override fun after() {
        AppGraph.get(context).resetHooks()
        FakeServer.normal()
    }
}

// ── Compose 시험 도우미(A4 · A5도 씀) ──

const val LOAD_TIMEOUT_MS = 15_000L

fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("TestTag starts with \"$prefix\"") {
    it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}

fun ComposeTestRule.waitForTag(tag: String, timeoutMs: Long = LOAD_TIMEOUT_MS) =
    waitUntil(timeoutMs) { onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitForTagGone(tag: String, timeoutMs: Long = LOAD_TIMEOUT_MS) =
    waitUntil(timeoutMs) { onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = LOAD_TIMEOUT_MS) =
    waitUntil(timeoutMs) { onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

/** 이슈맵 타일 수가 n개가 될 때까지 */
fun ComposeTestRule.waitForTiles(count: Int, timeoutMs: Long = LOAD_TIMEOUT_MS) =
    waitUntil(timeoutMs) { onAllNodes(hasTestTagPrefix(TestTags.TILE_PREFIX)).fetchSemanticsNodes().size == count }

/** 노드와 그 아래 전부(펼친 나무) */
fun SemanticsNode.descendantsAndSelf(): List<SemanticsNode> = listOf(this) + children.flatMap { it.descendantsAndSelf() }

fun SemanticsNode.tag(): String? = config.getOrNull(SemanticsProperties.TestTag)

/** 글 노드의 배치 결과(가로 넘침 확인) */
fun SemanticsNode.textLayout(): TextLayoutResult? {
    val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
    val out = mutableListOf<TextLayoutResult>()
    return if (action(out)) out.firstOrNull() else null
}
