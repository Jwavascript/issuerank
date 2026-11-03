package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.TestFiles
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.XmlStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 위젯 리소스 · 매니페스트(계획 4절 · 검증 6 앞부분 — APK는 aapt2로 다시 봄): widget_info 값 · receiver(exported false · brand · 방송 · meta-data) ·
 * debug 도구는 debug 매니페스트에만(exported true · 필터 0) · 레이아웃 칸 = 줄 수 식의 숫자 · RemoteViews가 받는 뷰만.
 */
class WidgetResourcesTest {
    private val res = "android/app/src/main/res"

    private fun xml(path: String): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(TestFiles.repoFile(path))

    private fun Document.elements(tag: String): List<Element> = getElementsByTagName(tag).let { l -> List(l.length) { l.item(it) as Element } }

    private fun Element.children(tag: String): List<Element> =
        List(childNodes.length) { childNodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }

    private fun styleItems(name: String): Map<String, String> =
        xml("$res/values/widget.xml").elements("style").single { it.getAttribute("name") == name }.children("item")
            .associate { it.getAttribute("name") to it.textContent.trim() }

    @Test
    fun widgetInfo_isThePlanValues() {
        val info = xml("$res/xml/widget_info.xml").documentElement
        assertEquals("appwidget-provider", info.tagName)
        val want = mapOf(
            "android:minWidth" to "276dp", "android:minHeight" to "117dp",
            "android:minResizeWidth" to "130dp", "android:minResizeHeight" to "66dp",
            "android:updatePeriodMillis" to WidgetRules.UPDATE_PERIOD_MS.toString(),
            "android:resizeMode" to "horizontal|vertical", "android:widgetCategory" to "home_screen",
            "android:targetCellWidth" to "4", "android:targetCellHeight" to "2",
            "android:description" to "@string/widget_description",
            "android:previewLayout" to "@layout/widget_preview", "android:initialLayout" to "@layout/widget_preview",
        )
        for ((k, v) in want) assertEquals(k, v, info.getAttribute(k))
        assertEquals("1800000", info.getAttribute("android:updatePeriodMillis"))
        assertEquals(117, WidgetSizes.DEFAULT_HEIGHT_DP)
        assertEquals(3, WidgetRules.rows(WidgetSizes.DEFAULT_HEIGHT_DP.toDouble(), 1.0)) // 기본 4×2 = 3줄
    }

    @Test
    fun description_isAnAppStringNotInTheWebFiles() {
        val app = XmlStrings.read(TestFiles.repoFile("$res/values/strings_app.xml"))
        assertEquals("어제 가장 많이 보도된 이슈 헤드라인", app["widget_description"])
        // C9: 웹 파일에 없는 글 → strings_app(보고 표). 실패 안내는 웹 글(widget.js) → strings_web
        for (f in listOf("index.html", "offline.html", "widget.js", "widget.html")) {
            assertFalse(f, "어제 가장 많이 보도된 이슈 헤드라인" in TestFiles.repoFile("ex12/public/$f").readText(Charsets.UTF_8))
        }
        assertTrue("widget_description" !in XmlStrings.values)
        assertEquals("불러오지 못했습니다", XmlStrings.values["widget_fail"])
    }

    @Test
    fun manifest_receiverIsNotExported_namedBrand_listensToWidgetUpdatesOnly() {
        val app = xml("android/app/src/main/AndroidManifest.xml").elements("application").single()
        val receiver = app.children("receiver").single()
        assertEquals(".widget.IssueRankWidget", receiver.getAttribute("android:name"))
        assertEquals("false", receiver.getAttribute("android:exported"))
        assertEquals("@string/brand", receiver.getAttribute("android:label"))
        val actions = receiver.children("intent-filter").flatMap { it.children("action") }.map { it.getAttribute("android:name") }
        assertEquals(listOf("android.appwidget.action.APPWIDGET_UPDATE"), actions)
        val meta = receiver.children("meta-data").single()
        assertEquals("android.appwidget.provider", meta.getAttribute("android:name"))
        assertEquals("@xml/widget_info", meta.getAttribute("android:resource"))
        // 이름 = brand "이슈랭크"(debug 앱 이름 "이슈랭크 시험"과 따로)
        assertEquals("이슈랭크", XmlStrings.values["brand"])
        // 앱 아이콘(MAIN/LAUNCHER)은 MainActivity 하나 · main에 debug 도구 없음
        val activities = app.children("activity")
        assertEquals(listOf(".MainActivity"), activities.map { it.getAttribute("android:name") })
        assertEquals(1, activities.flatMap { it.children("intent-filter") }.flatMap { it.children("category") }.count { it.getAttribute("android:name") == "android.intent.category.LAUNCHER" })
    }

    @Test
    fun debugManifest_toolIsExportedForAdbOnly_noIntentFilter_noLauncher() {
        val app = xml("android/app/src/debug/AndroidManifest.xml").elements("application").single()
        val tool = app.children("activity").single()
        assertEquals(".DebugToolsActivity", tool.getAttribute("android:name"))
        assertEquals("true", tool.getAttribute("android:exported"))
        assertEquals(0, tool.children("intent-filter").size)
        assertEquals("@xml/network_security_config", app.getAttribute("android:networkSecurityConfig"))
        assertFalse(TestFiles.repoFile("android/app/src/main/java/io/github/jwavascript/issuerank/DebugToolsActivity.kt").exists())
    }

    @Test
    fun layout_slotsAreTheFormulaNumbers_rowsHiddenUntilDrawn_onlyRemoteViewsViews() {
        val root = xml("$res/layout/widget.xml").documentElement
        assertEquals("@android:id/background", root.getAttribute("android:id"))
        val texts = root.children("TextView")
        assertEquals((1..10).map { "@+id/widget_row_$it" } + "@+id/widget_fail" + "@+id/widget_foot", texts.map { it.getAttribute("android:id") })
        for (t in texts.dropLast(1)) {
            assertEquals("@style/IssueRankWidgetRow", t.getAttribute("style"))
            assertEquals("처음엔 숨김 ${t.getAttribute("android:id")}", "gone", t.getAttribute("android:visibility"))
        }
        assertEquals("@string/widget_fail", texts[10].getAttribute("android:text"))
        assertEquals("@style/IssueRankWidgetFoot", texts[11].getAttribute("style"))
        // 칸: 위아래 안쪽 12dp × 2 = 24 · 줄 22sp · 하단 20sp = 식의 숫자(WidgetSizes)
        val dimens = xml("$res/values/widget.xml").elements("dimen").associate { it.getAttribute("name") to it.textContent.trim() }
        assertEquals("@dimen/widget_padding_vertical", root.getAttribute("android:paddingTop"))
        assertEquals("@dimen/widget_padding_vertical", root.getAttribute("android:paddingBottom"))
        assertEquals("${WidgetSizes.PADDING_DP / 2}dp", dimens["widget_padding_vertical"])
        val row = styleItems("IssueRankWidgetRow")
        val foot = styleItems("IssueRankWidgetFoot")
        assertEquals("${WidgetSizes.ROW_SP}sp", row["android:layout_height"])
        assertEquals("${WidgetSizes.FOOT_SP}sp", foot["android:layout_height"])
        // 글자: 제목 13sp 한 줄 끝 "…"(widget.js medium) · 하단 10sp
        assertEquals(listOf("13sp", "1", "end"), listOf(row["android:textSize"], row["android:maxLines"], row["android:ellipsize"]))
        assertEquals(listOf("10sp", "1", "end"), listOf(foot["android:textSize"], foot["android:maxLines"], foot["android:ellipsize"]))
        // RemoteViews가 부풀리는 뷰만(View 자체 · 사용자 뷰 0)
        for (name in listOf("widget.xml", "widget_preview.xml")) {
            val tags = xml("$res/layout/$name").getElementsByTagName("*").let { l -> List(l.length) { (l.item(it) as Element).tagName } }.toSet()
            assertTrue("$name $tags", tags.all { it in setOf("LinearLayout", "FrameLayout", "TextView") })
        }
    }

    @Test
    fun rowIdsInCode_areTheTenRowsInOrder() {
        val names = R.id::class.java.fields.associate { it.getInt(null) to it.name }
        assertEquals((1..10).map { "widget_row_$it" }, WidgetViews.ROW_IDS.map { names[it] })
        assertEquals(WidgetRules.MAX_ROWS, WidgetViews.ROW_IDS.size)
    }
}
