package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.TestFiles
import io.github.jwavascript.issuerank.ui.Contrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 위젯 색(C11 ⑥ — 위젯은 ui/ 밖이라 ThemeContrastTest의 투명도 검사가 닿지 않아 따로): 위젯이 그리는 색 = 웹 widget.js dynamicColor 3쌍 = 웹 토큰
 * (바탕 surface · 제목 · 실패 안내 text · 하단 muted, 밝게 · 어둡게). 글자 짝(밝게/어둡게 × 글자 종류 × 바탕) 모두 4.5 이상(ui/Contrast) →
 * 웹 값이 이미 4.5 이상이라 보정 0(Contrast.reach가 그대로 돌려줌). faint 글자 0 · 투명도 0 · 코드에서 색 지정 0.
 */
class WidgetColorsTest {
    private val res = "android/app/src/main/res"

    private fun colors(dir: String): Map<String, Int> {
        val list = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(TestFiles.repoFile("$res/$dir/colors.xml")).getElementsByTagName("color")
        return (0 until list.length).associate { i ->
            val e = list.item(i) as Element
            val h = e.textContent.trim().removePrefix("#")
            e.getAttribute("name") to (if (h.length == 6) "ff$h" else h).toLong(16).toInt()
        }
    }

    private val light = colors("values")
    private val dark = light + colors("values-night")
    private val modes = listOf("밝게" to light, "어둡게" to dark)

    private fun hex(c: Int) = String.format(Locale.ROOT, "#%06x", c and 0xffffff)
    private fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)

    @Test
    fun widgetColorsAreTheWidgetJsColors_whichAreTheWebTokens() {
        val js = TestFiles.repoFile("ex12/public/widget.js").readText(Charsets.UTF_8)
        // widget.js 77 · 80 · 94: 바탕 · 제목(실패 안내도) · 하단
        for ((call, token) in listOf(
            "widget.backgroundColor = dynamicColor('#ffffff', '#1a1a19')" to "surface",
            "const titleColor = dynamicColor('#161616', '#ecebe8')" to "text",
            "foot.textColor = dynamicColor('#6f6b65', '#9b978f')" to "muted",
        )) {
            assertTrue(call, call in js)
            val (l, d) = Regex("'(#[0-9a-f]{6})', '(#[0-9a-f]{6})'").find(call)!!.destructured
            assertEquals("$token 밝게", l, hex(light.getValue(token)))
            assertEquals("$token 어둡게", d, hex(dark.getValue(token)))
        }
        assertTrue("fail.textColor = titleColor;" in js)
        // 레이아웃이 그 토큰을 씀(@color — 런처가 values-night을 풂)
        val styles = TestFiles.repoFile("$res/values/widget.xml").readText(Charsets.UTF_8)
        assertTrue("<item name=\"android:textColor\">@color/text</item>" in styles)
        assertTrue("<item name=\"android:textColor\">@color/muted</item>" in styles)
        assertTrue("<solid android:color=\"@color/surface\" />" in TestFiles.repoFile("$res/drawable/widget_background.xml").readText(Charsets.UTF_8))
        assertTrue("android:background=\"@drawable/widget_background\"" in TestFiles.repoFile("$res/layout/widget.xml").readText(Charsets.UTF_8))
    }

    @Test
    fun everyTextPair_lightAndDark_isAtLeast45_andNeedsNoCorrection() {
        val failing = mutableListOf<String>()
        for ((mode, c) in modes) {
            val surface = c.getValue("surface")
            for ((name, fg) in listOf("제목 줄 text" to c.getValue("text"), "실패 안내 text" to c.getValue("text"), "하단 updatedLabel muted" to c.getValue("muted"))) {
                val r = Contrast.ratio(fg, surface)
                println("위젯 $mode $name ${hex(fg)}/surface ${hex(surface)} ${fmt(r)}")
                if (r < Contrast.TEXT_MIN) failing += "$mode $name ${fmt(r)}"
                // 웹 값이 이미 4.5 이상 → 최소 보정(Contrast.reach)이 웹 색 그대로를 돌려줌 = 웹과 같은 색
                assertEquals("$mode $name", hex(fg), hex(Contrast.reach(fg, c.getValue("text"), surface)))
            }
        }
        assertEquals(emptyList<String>(), failing)
        // 값(근거): 밝게 18.1 · 5.29 / 어둡게 14.6 · 5.99
        assertEquals(5.29, Contrast.ratio(light.getValue("muted"), light.getValue("surface")), 0.01)
        assertEquals(5.99, Contrast.ratio(dark.getValue("muted"), dark.getValue("surface")), 0.01)
    }

    @Test
    fun widgetSources_noFaintText_noAlpha_noColorsInCode() {
        val files = listOf(
            "$res/layout/widget.xml", "$res/layout/widget_preview.xml", "$res/values/widget.xml", "$res/values-v31/widget.xml",
            "$res/drawable/widget_background.xml", "$res/drawable/widget_preview_bar.xml", "$res/xml/widget_info.xml",
        ).map { TestFiles.repoFile(it) } +
            TestFiles.repoFile("android/app/src/main/java/io/github/jwavascript/issuerank/widget").listFiles { f: File -> f.extension == "kt" }!!
        assertEquals(13, files.size) // 리소스 7 + widget/*.kt 6
        for (f in files) {
            val text = f.readText(Charsets.UTF_8)
            val code = text.lines().filterNot { it.trim().startsWith("<!--") || it.trim().startsWith("*") || it.trim().startsWith("//") || it.trim().startsWith("/**") }
            for (line in code) {
                assertTrue("${f.name}: faint — $line", "faint" !in line)
                assertTrue("${f.name}: 투명도 — $line", Regex("""alpha|opacity|#[0-9a-fA-F]{8}\b""").find(line) == null)
                assertTrue("${f.name}: 색 직접 — $line", Regex("""#[0-9a-fA-F]{6}\b|setTextColor|setInt\(|Color\.""").find(line) == null)
            }
        }
        // 뼈대 막대(글자 아님 — 장식)만 line 토큰
        assertTrue("@color/line" in TestFiles.repoFile("$res/drawable/widget_preview_bar.xml").readText(Charsets.UTF_8))
    }

    @Test
    fun widgetHtmlPageTokens_areTheSameTokens_theWidgetHasNoFadedText() {
        // widget.html = 설치 안내 페이지 CSS(위젯 칸 아님): 같은 토큰 값. 그 페이지의 흐린 글(.code button:disabled .4)은 위젯에 없는 칸 → 보정 0
        val css = TestFiles.repoFile("ex12/public/widget.html").readText(Charsets.UTF_8)
        assertTrue("--bg: #f7f7f5; --surface: #ffffff; --text: #161616; --muted: #6f6b65; --faint: #a29e97;" in css)
        assertTrue("--bg: #121211; --surface: #1a1a19; --text: #ecebe8; --muted: #9b978f; --faint: #6a675f;" in css)
        assertEquals(listOf("#ffffff", "#161616", "#6f6b65"), listOf("surface", "text", "muted").map { hex(light.getValue(it)) })
        assertEquals(listOf("#1a1a19", "#ecebe8", "#9b978f"), listOf("surface", "text", "muted").map { hex(dark.getValue(it)) })
    }
}
