package io.github.jwavascript.issuerank.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 문구 대조(계획 5절 · 검증 4): strings_web.xml 값마다 자리 표시(%1$s · %1$d · %4$02d)를 뺀 글자 조각이
 * ex12/public/index.html · offline.html · widget.js · widget.html(C9 — 위젯 문구) 중 한 곳에 그대로 있음 —
 * XML 이스케이프는 XML 해석기 · aapt2 규칙으로, 웹 HTML 엔티티는 풀어서. 차이 0.
 */
class WebStringsTest {
    private val placeholder = Regex("""%(\d+)\$[-#+ 0,(]*\d*(?:\.\d+)?[sdf]""")

    private fun read(paths: List<String>): String =
        paths.joinToString("\n") { TestFiles.repoFile(it).readText(Charsets.UTF_8).replace("\r\n", "\n") }.let(::decodeHtmlEntities)

    private val web: String by lazy { read(SOURCES) }

    companion object {
        /** 대조 원본(C9: 위젯 실패 안내 "불러오지 못했습니다"는 widget.js에만 있어 widget.js · widget.html을 더함) */
        val SOURCES = listOf("ex12/public/index.html", "ex12/public/offline.html", "ex12/public/widget.js", "ex12/public/widget.html")

        /** 위젯 문구(widget.js) — 그 밖 문구는 처음 원본(index · offline)에만 대조해도 그대로 통과해야 함 */
        val WIDGET_KEYS = setOf(Str.WIDGET_FAIL)
    }

    private fun missingFragments(values: Map<String, String>, text: String): List<String> = values.flatMap { (name, value) ->
        value.replace("%%", "\u0000").split(placeholder).map { it.replace("\u0000", "%") }.filter { it.isNotEmpty() && it !in text }
            .map { "$name: \"$it\"" }
    }

    private fun decodeHtmlEntities(s: String): String = Regex("&(#x[0-9a-fA-F]+|#\\d+|amp|lt|gt|quot|apos|nbsp);").replace(s) { m ->
        when (val e = m.groupValues[1]) {
            "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> Char(0x00A0).toString()
            else -> String(Character.toChars(if (e.startsWith("#x")) e.substring(2).toInt(16) else e.substring(1).toInt()))
        }
    }

    @Test
    fun everyFragmentIsInTheWebPages() {
        val values = XmlStrings.values
        assertTrue(values.size >= 100)
        assertEquals(emptyList<String>(), missingFragments(values, web))
    }

    @Test
    fun nonWidgetStrings_stillMatchTheFirstTwoPagesAlone() {
        // 원본을 넓힌 것(C9)이 기존 133개 대조를 느슨하게 만들지 않았는지: 위젯 문구 말고는 index · offline만으로도 차이 0
        val widgetNames = WIDGET_KEYS.map { it.resName }.toSet()
        val values = XmlStrings.values.filterKeys { it !in widgetNames }
        assertEquals(133, values.size)
        assertEquals(emptyList<String>(), missingFragments(values, read(SOURCES.take(2))))
        // 위젯 문구는 처음 원본에 없음(그래서 widget.js를 더함)
        assertEquals(WIDGET_KEYS.size, missingFragments(XmlStrings.values.filterKeys { it in widgetNames }, read(SOURCES.take(2))).size)
    }

    @Test
    fun wholeStaticStringsAreWebTextVerbatim() {
        // 자리 표시 없는 문구 몇 개는 통째로(조각 나눔이 너무 너그럽지 않은지)
        for (key in listOf(Str.SUMMARY_NONE, Str.RANK_DESC_MAP, Str.MAP_FOOTER, Str.GOOGLE_NOT_LINKED, Str.OFFLINE_NOTE, Str.SHARE_SITE_TEXT, Str.AUTH_ERR_PASSWORD_LENGTH)) {
            assertTrue(key.name, XmlStrings[key] in web)
        }
    }

    @Test
    fun widgetFailIsTheWidgetJsTextVerbatim() {
        // widget.js 97: const fail = widget.addText('불러오지 못했습니다') — 위젯이 실패 안내로 그리는 글 그대로(조각 대조보다 좁게)
        val js = TestFiles.repoFile("ex12/public/widget.js").readText(Charsets.UTF_8)
        assertTrue("widget.addText('${XmlStrings[Str.WIDGET_FAIL]}')" in js)
        assertEquals("불러오지 못했습니다", XmlStrings[Str.WIDGET_FAIL])
    }

    @Test
    fun strEnumAndXmlHaveTheSameNames() {
        assertEquals(XmlStrings.values.keys.sorted(), Str.entries.map { it.resName }.sorted())
    }

    @Test
    fun placeholdersArePositionalAndContiguous() {
        for ((name, value) in XmlStrings.values) {
            val indexes = placeholder.findAll(value).map { it.groupValues[1].toInt() }.toSortedSet()
            assertEquals(name, (1..indexes.size).toSet(), indexes)
            assertTrue(name, !Regex("%[sd]").containsMatchIn(value)) // 위치 없는 %s · %d 금지(aapt2)
        }
    }

    @Test
    fun excludedTopicsAreNotHere() {
        // 알림 · 설치 · /card · /widget 안내 · Analytics 문구는 앱 v1에 없음
        for (word in listOf("알림", "앱 설치", "카드를", "위젯", "Analytics", "링크를 복사")) {
            assertTrue(word, XmlStrings.values.values.none { word in it })
        }
    }

    @Test
    fun androidResourceTextRules() {
        assertEquals("a b", XmlStrings.androidResourceText("  a \n  b  "))
        assertEquals("  a  ", XmlStrings.androidResourceText("\"  a  \""))
        assertEquals("it's \"x\"\n", XmlStrings.androidResourceText("it\\'s \\\"x\\\"\\n"))
    }
}
