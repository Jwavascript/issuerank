package io.github.jwavascript.issuerank.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.jwavascript.issuerank.data.TestFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 대비(COMMON C11 · 계획 5절): 화면이 실제로 그리는 8비트 색으로 글자 4.5 · 뜻을 전하는 그림 3 이상.
 * 색 = colors.xml(웹 hex, 밝게 · 어둡게). 웹이 토큰을 섞는 칠(타일 · 숫자 칸 · 반투명 글자)은 웹 칸 전부(섹션 6 × 밝게/어둡게 × 연관기사 ratio 0~1 —
 * 덧칠 l = 0.000~0.240 1000분의 1 전부 · w = 10~36 정수 전부)를 재고, 웹이 이미 4.5 이상인 칸은 웹 합성색 그대로인지 봄.
 * 이 시험은 대비 · 섞기 · 최소 옮기기를 따로 한 벌 계산해 제품 함수(Contrast)와 대조함(제품 함수로 제품 함수를 시험하지 않음).
 */
class ThemeContrastTest {
    // ── 시험 쪽 계산(제품과 따로) ──
    private fun lin(v: Int): Double { val s = v / 255.0; return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4) }
    private fun lum(c: Int): Double = 0.2126 * lin(c ushr 16 and 255) + 0.7152 * lin(c ushr 8 and 255) + 0.0722 * lin(c and 255)
    private fun cr(a: Int, b: Int): Double { val (x, y) = lum(a) to lum(b); return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05) }
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xff shl 24) or (r shl 16) or (g shl 8) or b

    /** a × p + b × (1 − p), 채널마다 반올림 */
    private fun mix8(a: Int, b: Int, p: Double): Int {
        val ch = IntArray(3) { i -> val sh = 16 - 8 * i; ((a ushr sh and 255) * p + (b ushr sh and 255) * (1 - p)).roundToInt() }
        return rgb(ch[0], ch[1], ch[2])
    }

    /** 처음 색이 모자라면 to 쪽으로 1000분의 1씩, 8비트로 내린 색이 against와 min을 넘는 첫 색 */
    private fun nearest(from: Int, to: Int, against: Int, min: Double = 4.5): Int {
        if (cr(from, against) >= min) return from
        var k = 1
        while (k <= 1000) {
            val c = mix8(to, from, k / 1000.0)
            if (cr(c, against) >= min) return c
            k++
        }
        return to
    }

    private fun hex(c: Int) = String.format(Locale.ROOT, "#%06x", c and 0xffffff)

    private fun colors(dir: String): Map<String, Int> {
        val file = TestFiles.repoFile("android/app/src/main/res/$dir/colors.xml")
        val list = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("color")
        return (0 until list.length).associate { i ->
            val e = list.item(i) as Element
            val h = e.textContent.trim().removePrefix("#")
            e.getAttribute("name") to (if (h.length == 6) "ff$h" else h).toLong(16).toInt()
        }
    }

    private val light = colors("values")
    private val dark = light + colors("values-night")
    private val modes = listOf("밝게" to light, "어둡게" to dark)
    private val sections = listOf("s_pol", "s_eco", "s_soc", "s_cul", "s_wor", "s_it")
    private val white = rgb(255, 255, 255)
    private val black = rgb(0, 0, 0)

    private fun fmt(v: Double) = String.format(Locale.ROOT, "%.2f", v)

    @Test
    fun tiles_everySectionAndRatio_whiteTextIsAtLeast45_webColorWherePasses() {
        var webTitle = 99.0
        var webCount = 99.0
        var webRank = 99.0
        var after = 99.0
        var markMin = 99.0
        var ringMin = 99.0
        var kept = 0
        var moved = 0
        val examples = mutableListOf<String>()
        for ((mode, c) in modes) for (s in sections) for (i in 0..240) {
            val l = i / 1000.0 // 웹 --l = toFixed(3)(0.24 × (1 − ratio))
            val text = c.getValue("tile_text")
            val web = mix8(white, c.getValue(s), l)
            val want = nearest(web, black, text)
            val got = Contrast.tileFill(c.getValue(s), l, text)
            assertEquals("$mode $s l=$l", hex(want), hex(got))
            // 제목 · 순위 · 건수 = 같은 흰 글자(투명도 1) — 그린 8비트 색으로 4.5 이상
            assertTrue("$mode $s l=$l ${fmt(cr(text, got))}", cr(text, got) >= 4.5)
            // 웹이 이미 4.5 이상이면 웹 합성색 그대로
            if (cr(text, web) >= 4.5) { assertEquals("$mode $s l=$l", hex(web), hex(got)); kept++ } else moved++
            // 북마크 표시(그림 · 흰색 .95) · 고른 타일 테두리(그림 · text 색) 3:1 이상
            markMin = minOf(markMin, cr(mix8(text, got, 0.95), got))
            ringMin = minOf(ringMin, cr(c.getValue("text"), got))
            webTitle = minOf(webTitle, cr(text, web))
            webCount = minOf(webCount, cr(mix8(text, web, 0.9), web))
            webRank = minOf(webRank, cr(mix8(text, web, 0.7), web))
            after = minOf(after, cr(text, got))
            if (i == 0 || i == 240) examples += "$mode $s l=$l 웹 ${hex(web)}(${fmt(cr(text, web))}) → ${hex(got)}(${fmt(cr(text, got))})"
        }
        assertTrue(markMin >= 3.0)
        assertTrue(ringMin >= 3.0)
        // 보기(코디네이터 C11 예): 어둡게 s_it ratio 1(l=0) = 웹 #6f56a8 그대로 · 밝게 s_soc ratio 1 = 더 어둡게
        assertEquals("#6f56a8", hex(Contrast.tileFill(dark.getValue("s_it"), 0.0, dark.getValue("tile_text"))))
        assertTrue(Contrast.tileFill(light.getValue("s_soc"), 0.0, light.getValue("tile_text")) != light.getValue("s_soc"))
        println("타일 최소 대비 전: 제목 ${fmt(webTitle)} · 건수(.9) ${fmt(webCount)} · 순위(.7) ${fmt(webRank)} → 후(투명도 1): ${fmt(after)} · 북마크 표시 ${fmt(markMin)} · 고른 테두리 ${fmt(ringMin)}")
        println("타일 칸 ${kept + moved}개 중 웹 그대로 $kept · 옮김 $moved")
        println(examples.joinToString("\n"))
    }

    @Test
    fun scoreBoxes_everyWeight_onBgAndSurface_textIsAtLeast45_backgroundIsWeb() {
        var webB = 99.0
        var webSmall = 99.0
        var after = 99.0
        var kept = 0
        var moved = 0
        val examples = mutableListOf<String>()
        for ((mode, c) in modes) for (s in sections) for (under in listOf("bg", "surface")) for (w in 10..36) {
            val u = c.getValue(under)
            val back = mix8(c.getValue(s), u, w / 100.0) // color-mix(in srgb, var(--c) w%, transparent) 위 아래 바탕
            assertEquals("$mode $s $under w$w 바탕", hex(back), hex(Contrast.scoreBackground(c.getValue(s), w, u)))
            val web = mix8(c.getValue(s), c.getValue("text"), 0.8)
            val want = nearest(web, c.getValue("text"), back)
            val got = Contrast.scoreText(c.getValue(s), c.getValue("text"), w, u)
            assertEquals("$mode $s $under w$w", hex(want), hex(got))
            // 숫자(b) · "연관기사"(small) = 같은 색 · 투명도 1
            assertTrue("$mode $s $under w$w ${fmt(cr(got, back))}", cr(got, back) >= 4.5)
            if (cr(web, back) >= 4.5) { assertEquals(hex(web), hex(got)); kept++ } else moved++
            webB = minOf(webB, cr(web, back))
            webSmall = minOf(webSmall, cr(mix8(web, back, 0.8), back))
            after = minOf(after, cr(got, back))
            if (under == "bg" && (w == 10 || w == 36)) examples += "$mode $s w$w 웹 글자 ${hex(web)}(${fmt(cr(web, back))}) → ${hex(got)}(${fmt(cr(got, back))}) · 바탕 ${hex(back)}"
        }
        println("숫자 칸 최소 대비 전: 숫자 ${fmt(webB)} · small(.8) ${fmt(webSmall)} → 후(투명도 1): ${fmt(after)}")
        println("숫자 칸 ${kept + moved}개 중 웹 그대로 $kept · 옮김 $moved")
        println(examples.joinToString("\n"))
    }

    @Test
    fun otherTextAndIconPairs_andMaterialParts_meetTheBar() {
        val failing = mutableListOf<String>()
        for ((mode, c) in modes) {
            fun need(name: String, fg: Int, bg: Int, min: Double = 4.5) {
                val r = cr(fg, bg)
                println("$mode $name ${fmt(r)}")
                if (r < min) failing += "$mode $name ${fmt(r)} < $min"
            }
            val (bg, surface, text, muted) = listOf(c.getValue("bg"), c.getValue("surface"), c.getValue("text"), c.getValue("muted"))
            val (hover, ink, inkText) = listOf(c.getValue("hover"), c.getValue("ink"), c.getValue("ink_text"))
            // 화면 글자
            need("text/bg", text, bg)
            need("muted/bg(설명 · 갱신 시각 · meta · 요약 · 안내 · 탭 · 맨 아래)", muted, bg)
            need("text/surface(상세)", text, surface)
            need("muted/surface(상세 facts · meta · 단추)", muted, surface)
            need("muted/hover(보기 전환 안 고른 탭)", muted, hover)
            need("text/surface(고른 탭)", text, surface)
            need("inkText/ink(진한 단추 · 토스트 · 북마크 탭)", inkText, ink)
            // 머리 북마크 개수(웹 .cnt opacity .7) — 제품 = Contrast.fadedText, 시험 = 따로 계산
            for ((name, fg, under) in listOf(Triple("muted .7/bg", muted, bg), Triple("inkText .7/ink", inkText, ink))) {
                val got = Contrast.fadedText(fg, 0.7, under)
                assertEquals("$mode $name", hex(nearest(mix8(fg, under, 0.7), fg, under)), hex(got))
                need("머리 개수 $name(웹 ${fmt(cr(mix8(fg, under, 0.7), under))})", got, under)
            }
            // 뜻을 전하는 그림 3:1(이름 글자가 옆에 있는 보기 전환 아이콘 .8도 3 이상)
            need("아이콘 muted/bg(이전 · 다음 · 북마크 끔)", muted, bg, 3.0)
            need("아이콘 muted/surface(닫기)", muted, surface, 3.0)
            need("보기 전환 아이콘 muted .8/hover", mix8(muted, hover, 0.8), hover, 3.0)
            need("보기 전환 아이콘 text .8/surface", mix8(text, surface, 0.8), surface, 3.0)
            // Material 부품(Theme.kt 색 묶음): DatePicker 창 = surface 위 onSurface · onSurfaceVariant(제목 · 요일 · 월) · primary(오늘 · 확인 · 취소) ·
            // 고른 날 onPrimary/primary · 오류 글(입력 모드 — Material 기본 error), 아래 창 = surface 위 text. 막힌 날(.38)은 쓸 수 없는 칸이라 기준 밖
            need("M3 onSurface/surface", text, surface)
            need("M3 onSurfaceVariant/surface", muted, surface)
            need("M3 primary/surface", ink, surface)
            need("M3 onPrimary/primary", inkText, ink)
            need("M3 onPrimaryContainer · onSecondaryContainer/hover", text, hover)
            val error = (if (c === dark) darkColorScheme() else lightColorScheme()).error.toArgb()
            need("M3 error/surface", error, surface)
        }
        assertEquals(emptyList<String>(), failing)
    }

    /**
     * 요약 펼치기 끈(C11 ⑧ — 상태 그림 3:1): 끔 막대(웹 line — 바탕 대비 1.18 · 1.31)를 바탕 · 손잡이(surface) 둘 다와 3:1이 될 때까지만 text 쪽으로.
     * 제품 Contrast.toggleTrackOff = 시험 쪽 따로 계산. 켬(ink 막대 · surface 손잡이)은 웹 그대로 3 이상.
     */
    @Test
    fun summaryToggle_offTrackIs3ToBackgroundAndKnob_onStateIsTheWeb() {
        for ((mode, c) in modes) {
            val (line, text, bg) = listOf(c.getValue("line"), c.getValue("text"), c.getValue("bg"))
            val (surface, ink) = listOf(c.getValue("surface"), c.getValue("ink"))
            val want = nearest(nearest(line, text, bg, 3.0), text, surface, 3.0)
            val got = Contrast.toggleTrackOff(line, text, bg, surface)
            assertEquals(mode, hex(want), hex(got))
            assertTrue("$mode 웹 끔 막대 ${fmt(cr(line, bg))}", cr(line, bg) < 3.0) // 웹 그대로면 미달 — 그래서 고침
            assertTrue("$mode 끔 막대/바탕 ${fmt(cr(got, bg))}", cr(got, bg) >= 3.0)
            assertTrue("$mode 손잡이/끔 막대 ${fmt(cr(surface, got))}", cr(surface, got) >= 3.0)
            assertTrue("$mode 켬 막대/바탕", cr(ink, bg) >= 3.0)
            assertTrue("$mode 손잡이/켬 막대", cr(surface, ink) >= 3.0)
            println("$mode 끔 막대 ${hex(line)}(바탕 ${fmt(cr(line, bg))} · 손잡이 ${fmt(cr(surface, line))}) → ${hex(got)}(${fmt(cr(got, bg))} · ${fmt(cr(surface, got))}) · 켬 ${fmt(cr(ink, bg))} · ${fmt(cr(surface, ink))}")
        }
    }

    /**
     * 로그인 창 · 폴더 삭제 창(늘 밝은 고정 색 — C11 ⑥): 제품 상수 = 웹 CSS 글자(index.html 406~431)와 같은 값, 글자 4.5 · 상태 그림 3 이상.
     * 입력 칸 테두리는 웹 상수(#aaa · 2.32)를 그대로 두고, 그리는 초점 없는 테두리만 C12 ⑥으로 3:1(아래 inputBorders 시험) · 초점 = 2dp 글자색.
     */
    @Test
    fun lightDialogs_fixedColorsAreTheWebCss_andMeetTheBar() {
        val css = TestFiles.repoFile("ex12/public/index.html").readText(Charsets.UTF_8)
        for (snippet in listOf(
            "border: 1px solid #ddd; border-radius: 16px; padding: 24px; color: #222; background: #ffffff; --hover: #efede9; --line: #e7e4df;",
            ".auth-dialog::backdrop { background: rgb(0 0 0 / 35%); }",
            ".auth-dialog input { width: 100%; box-sizing: border-box; padding: 10px; margin-top: 6px; border: 1px solid #aaa;",
            ".auth-error { min-height: 20px; color: #b42318; font-size: 14px; }",
            "border: 1px solid #747775; border-radius: 20px; background: #FFFFFF; color: #1F1F1F;",
            ".google-btn:disabled { opacity: .38;",
            ".auth-or { display: flex; align-items: center; gap: 10px; margin: 16px 0 0; color: #6f6b65; font-size: 13px; }",
            ".auth-or::before, .auth-or::after { content: \"\"; flex: 1; height: 1px; background: #ddd; }",
            ".auth-legal { margin: 16px 0 0; text-align: center; font-size: 12px; color: #6f6b65; }",
            "#folderDialog .danger { background: #b42318; border-color: #b42318; color: #ffffff; }",
            "#folderDialog button:disabled { opacity: .5;",
        )) assertTrue(snippet, snippet in css)
        assertEquals(
            listOf("#ffffff", "#222222", "#dddddd", "#e7e4df", "#aaaaaa", "#6f6b65", "#b42318", "#ffffff", "#747775", "#1f1f1f"),
            listOf(
                LightDialog.SURFACE, LightDialog.TEXT, LightDialog.BORDER, LightDialog.LINE, LightDialog.INPUT_BORDER, LightDialog.MUTED,
                LightDialog.ERROR, LightDialog.ON_ERROR, LightDialog.GOOGLE_BORDER, LightDialog.GOOGLE_TEXT,
            ).map(::hex),
        )
        assertEquals(0x59000000, LightDialog.BACKDROP_ARGB) // 35% 검정(미리보기 칠) = dim 0.35
        assertEquals(0.35f, LightDialog.BACKDROP)
        val white = LightDialog.SURFACE
        val failing = mutableListOf<String>()
        fun need(name: String, fg: Int, bg: Int, min: Double) {
            val r = cr(fg, bg)
            println("밝은 창 $name ${fmt(r)}")
            if (r < min) failing += "$name ${fmt(r)} < $min"
        }
        need("제목 · 이름표 · 입력 글 · 단추 #222/흰", LightDialog.TEXT, white, 4.5)
        need("또는 이메일로 · 이용약관 #6f6b65/흰", LightDialog.MUTED, white, 4.5)
        need("오류 줄 #b42318/흰", LightDialog.ERROR, white, 4.5)
        need("Google 글자 #1F1F1F/흰", LightDialog.GOOGLE_TEXT, white, 4.5)
        need("삭제 단추 흰/#b42318", LightDialog.ON_ERROR, LightDialog.ERROR, 4.5)
        need("고른 글 #222/고르기 칠", LightDialog.TEXT, mix8(LightDialog.TEXT, white, 0.4), 4.5)
        need("입력 칸 초점 테두리 #222/흰(상태)", LightDialog.TEXT, white, 3.0)
        need("Google 테두리 #747775/흰", LightDialog.GOOGLE_BORDER, white, 3.0)
        assertEquals(emptyList<String>(), failing)
        println("밝은 창 입력 칸 테두리 웹 값 #aaa/흰 ${fmt(cr(LightDialog.INPUT_BORDER, white))} → 그리는 초점 없는 테두리 ${hex(LightDialog.UNFOCUSED_INPUT_BORDER)} ${fmt(cr(LightDialog.UNFOCUSED_INPUT_BORDER, white))}(C12 ⑥)")
        // 바쁜 동안 흐림 = 흰 창 위 웹 opacity 그대로(막힌 단추 — 대비 기준 밖)
        assertEquals(hex(mix8(LightDialog.GOOGLE_TEXT, white, 0.38)), hex(Contrast.over(LightDialog.GOOGLE_TEXT, LightDialog.DISABLED_GOOGLE, white)))
        assertEquals(hex(mix8(LightDialog.ERROR, white, 0.5)), hex(Contrast.over(LightDialog.ERROR, LightDialog.DISABLED_FOLDER, white)))
    }

    /** A4 화면(테마 색): 북마크 화면 · 폴더 고르기 글자 4.5 · 상태 그림 3 — 칩 개수(.7)는 머리 개수와 같은 Contrast.fadedText(위 시험) */
    @Test
    fun accountScreens_textAndStatePairs_meetTheBar() {
        val failing = mutableListOf<String>()
        for ((mode, c) in modes) {
            fun need(name: String, fg: Int, bg: Int, min: Double = 4.5) {
                val r = cr(fg, bg)
                println("$mode $name ${fmt(r)}")
                if (r < min) failing += "$mode $name ${fmt(r)} < $min"
            }
            val (bg, surface, text, muted) = listOf(c.getValue("bg"), c.getValue("surface"), c.getValue("text"), c.getValue("muted"))
            val (ink, inkText) = listOf(c.getValue("ink"), c.getValue("ink_text"))
            need("칩 · 폴더 삭제 · bm-user · 개수 줄 · 카드 meta muted/bg", muted, bg)
            need("고른 칩 inkText/ink", inkText, ink)
            need("칩 개수 muted .7/bg", Contrast.fadedText(muted, 0.7, bg), bg)
            need("고른 칩 개수 inkText .7/ink", Contrast.fadedText(inkText, 0.7, ink), ink)
            need("고르기 제목 · 폴더 개수 muted/surface", muted, surface)
            need("고르기 폴더 이름 text/surface", text, surface)
            need("새 폴더 입력 글 text/bg", text, bg)
            need("새 폴더 이름(자리 글) muted/bg", muted, bg)
            need("만들기 inkText/ink", inkText, ink)
            need("새 폴더 초점 테두리 text/bg(상태)", text, bg, 3.0)
            need("고른 칩 바탕 ink/bg(상태)", ink, bg, 3.0)
        }
        assertEquals(emptyList<String>(), failing)
    }

    /** 처음 from에서 to 쪽으로 1000분의 1씩, 8비트로 내린 색이 against 모두와 min 이상이 되는 걸음 수(0 = 이미 넘음 — 웹 색 그대로) */
    private fun firstStep(from: Int, to: Int, against: List<Int>, min: Double): Int {
        if (against.all { cr(from, it) >= min }) return 0
        for (k in 1..1000) if (against.all { cr(mix8(to, from, k / 1000.0), it) >= min }) return k
        return 1000
    }

    private fun webColors(c: Map<String, Int>) = WebColors(
        bg = Color(c.getValue("bg")), surface = Color(c.getValue("surface")), text = Color(c.getValue("text")),
        muted = Color(c.getValue("muted")), faint = Color(c.getValue("faint")), line = Color(c.getValue("line")),
        hover = Color(c.getValue("hover")), ink = Color(c.getValue("ink")), inkText = Color(c.getValue("ink_text")),
        tileText = Color(c.getValue("tile_text")), shadow = Color(c.getValue("shadow")), sections = sections.map { Color(c.getValue(it)) },
    )

    /**
     * 글 입력 칸 경계(C12 ⑥ — WCAG 1.4.11, C11 ⑧을 넓힘): 초점 없는 테두리 = 웹 테두리 색에서 그 화면 글자색 쪽으로, 바로 닿는 색(칸 바탕 · 칸을 둘러싼 창 바탕)
     * 모두와 8비트 실제 색으로 3:1이 될 때까지만. 제품 값 = 시험 쪽 따로 계산 · 3.0 이상 · 한 걸음 덜 옮긴 색은 3.0 미만(최소).
     * 칸 3가지: 로그인 창(웹 #aaa · 칸 · 창 = 흰색) · 새 폴더(웹 line · 칸 bg · 고르기 창 surface) · 달력 입력 모드(colorScheme.outline = 웹 line · 달력 창 surface — 칸 속 투명).
     * 대상 밖(웹 그대로): 웹 상수 · 토큰(#aaa · line) · 초점 테두리(글자색) · 단추 · 칩 테두리(LightDialog.LINE · line) · 창 테두리(#ddd) · 구분선(outlineVariant = line).
     */
    @Test
    fun inputBorders_unfocusedAre3ToEveryAdjacentColor_minimally_restStaysWeb() {
        class Case(val name: String, val web: Int, val text: Int, val adjacent: List<Int>, val got: Int)
        val cases = mutableListOf(
            Case("로그인 창 3칸(#aaa · 칸 · 창 흰색)", LightDialog.INPUT_BORDER, LightDialog.TEXT, listOf(LightDialog.SURFACE), LightDialog.UNFOCUSED_INPUT_BORDER),
        )
        for ((mode, c) in modes) {
            val (line, text, bg, surface) = listOf(c.getValue("line"), c.getValue("text"), c.getValue("bg"), c.getValue("surface"))
            cases += Case("$mode 새 폴더 칸(line · 칸 bg · 창 surface)", line, text, listOf(bg, surface), Contrast.inputBorder(line, text, bg, surface))
            cases += Case("$mode 달력 입력 모드 칸(outline = line · 창 surface)", line, text, listOf(surface), inputOutline(webColors(c)).toArgb())
        }
        for (k in cases) {
            val step = firstStep(k.web, k.text, k.adjacent, 3.0)
            assertTrue("${k.name}: 웹 색이 이미 3:1 — 고칠 칸이 아님", step > 0)
            val want = mix8(k.text, k.web, step / 1000.0)
            assertEquals(k.name, hex(want), hex(k.got))
            for (a in k.adjacent) assertTrue("${k.name} ${hex(k.got)}/${hex(a)} ${fmt(cr(k.got, a))}", cr(k.got, a) >= 3.0)
            val less = mix8(k.text, k.web, (step - 1) / 1000.0)
            assertTrue("${k.name}: 한 걸음 덜(${hex(less)})도 3:1 — 최소가 아님", k.adjacent.any { cr(less, it) < 3.0 })
            println(
                "${k.name}: 웹 ${hex(k.web)}(${k.adjacent.joinToString { fmt(cr(k.web, it)) }}) → ${hex(k.got)}(${k.adjacent.joinToString { fmt(cr(k.got, it)) }})" +
                    " · ${step}걸음 · 한 걸음 덜 ${hex(less)}(${k.adjacent.joinToString { fmt(cr(less, it)) }})",
            )
        }
        // 대상 밖 = 웹 그대로(상수 · 토큰은 바꾸지 않고 그리는 칠만 따로)
        assertEquals("#aaaaaa", hex(LightDialog.INPUT_BORDER))
        assertEquals(listOf("#e7e4df", "#dddddd", "#222222"), listOf(LightDialog.LINE, LightDialog.BORDER, LightDialog.TEXT).map(::hex))
        assertEquals(listOf("#e7e4df", "#2b2a27"), modes.map { (_, c) -> hex(c.getValue("line")) })
        // 제품 화면이 이 칠을 씀 · 초점 테두리 · 구분선은 그대로(소스)
        val ui = TestFiles.repoFile("android/app/src/main/java/io/github/jwavascript/issuerank/ui")
        val auth = File(ui, "AuthDialog.kt").readText()
        val folder = File(ui, "FolderDialogs.kt").readText()
        val theme = File(ui, "Theme.kt").readText()
        assertTrue("Color(if (focused) LightDialog.TEXT else LightDialog.UNFOCUSED_INPUT_BORDER)" in auth)
        assertTrue("Contrast.inputBorder(lineArgb, textArgb, fillArgb, aroundArgb)" in folder)
        assertTrue("if (focused) c.text else border" in folder)
        assertTrue("outline = outline, outlineVariant = c.line" in theme)
    }

    @Test
    fun faintIsOnlyDecoration_inUiSources() {
        val uses = uiSources().flatMap { f -> f.readLines().filter { Regex("""\.faint\b""").containsMatchIn(it) }.map { "${f.name}: ${it.trim()}" } }
        assertTrue("faint 쓰임 없음", uses.isNotEmpty())
        for (u in uses) assertTrue(u, ".background(c.faint)" in u)
    }

    /** ui/ 글자 · 아이콘의 투명도는 시험이 잰 것만(새로 넣으면 여기에 이유와 함께) */
    @Test
    fun translucentTextOrIcons_inUiSources_areOnlyTheMeasuredOnes() {
        val allowed = listOf(
            "Modifier.size(13.dp).alpha(0.8f)", // 보기 전환 아이콘(그림 · 3:1 — 위 시험)
            ".alpha(if (enabled) 1f else 0.35f)", // 막힌 단추(쓸 수 없는 단추 — 대비 기준 밖)
            "c.tileText.copy(alpha = 0.95f)", // 타일 북마크 표시(그림 · 3:1 — 위 시험)
        )
        val uses = uiSources().flatMap { f ->
            f.readLines().filter { "copy(alpha" in it || ".alpha(" in it }.filter { !it.trim().startsWith("//") && !it.trim().startsWith("*") }
                .map { "${f.name}: ${it.trim()}" }
        }
        for (u in uses) assertTrue(u, allowed.any { it in u })
        assertEquals(allowed.size, uses.size)
    }

    private fun uiSources(): List<File> =
        TestFiles.repoFile("android/app/src/main/java/io/github/jwavascript/issuerank/ui")
            .listFiles { f: File -> f.extension == "kt" && f.name != "Theme.kt" && f.name != "Contrast.kt" }!!.toList()
}
