package io.github.jwavascript.issuerank.ui

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 대비 규칙(COMMON C11 — 계획 5절 "4.5:1 이상"이 앞섬): colors.xml(웹 hex)은 그대로, 웹이 토큰을 섞어 만드는 칠만
 * 4.5 아래인 칸에서 4.5가 될 만큼 최소로 옮김(웹이 이미 4.5 이상이면 웹과 같은 색). 근거 = c0-contrast-options "Option A".
 * 색 = 8비트 ARGB Int(Android 색 그대로 — 위젯 RemoteViews도 씀). 섞기 = sRGB 값 그대로(CSS color-mix in srgb · 반투명 덧칠과 같음),
 * 대비 = WCAG 2 상대 휘도. 결과는 늘 불투명 8비트이고, 8비트로 내린 색으로 다시 재서 기준을 넘는지 확인함. 순수 함수(Android · Compose 0).
 */
object Contrast {
    const val TEXT_MIN = 4.5
    const val GRAPHIC_MIN = 3.0
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    /** 옮기는 칸: 0.001씩(최대 1000번) — 매번 8비트로 내려서 잼 */
    private const val STEPS = 1000

    private fun channel(v: Int): Double = (v / 255.0).let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }

    fun luminance(argb: Int): Double =
        0.2126 * channel(argb shr 16 and 0xff) + 0.7152 * channel(argb shr 8 and 0xff) + 0.0722 * channel(argb and 0xff)

    /** 두 불투명 색의 대비(1~21) */
    fun ratio(a: Int, b: Int): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    /** CSS color-mix(in srgb, a p, b) — 채널마다 a × p + b × (1 − p), 반올림해 불투명 8비트 */
    fun mix(a: Int, b: Int, p: Double): Int {
        fun ch(shift: Int) = ((a shr shift and 0xff) * p + (b shr shift and 0xff) * (1 - p)).roundToInt().coerceIn(0, 255)
        return (0xff shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** top 색(RGB)을 알파 alpha로 불투명 under 위에 얹은 색(웹 rgba 덧칠 · opacity · color-mix … transparent) */
    fun over(top: Int, alpha: Double, under: Int): Int = mix(top, under, alpha)

    /**
     * moving을 toward 쪽으로(sRGB 직선) 옮겨 fixed와의 대비가 min 이상이 되는 가장 가까운 색. 이미 넘으면 moving 그대로(웹 색).
     * 0.001씩 옮기며 8비트로 내린 색을 다시 잼(반올림 때문에 4.49가 나오면 다음 칸). 끝까지 가도 모자라면 toward.
     */
    fun reach(moving: Int, toward: Int, fixed: Int, min: Double = TEXT_MIN): Int {
        val start = moving or (0xff shl 24)
        if (ratio(start, fixed) >= min) return start
        for (i in 1..STEPS) {
            val c = mix(toward, start, i.toDouble() / STEPS)
            if (ratio(c, fixed) >= min) return c
        }
        return toward or (0xff shl 24)
    }

    // ── 화면 칠(웹 꼴 → 최소 보정) ──

    /** 이슈맵 타일 바탕: 웹 = 섹션 색 + 흰 덧칠 lightness(MapTile.lightness, 0~0.24) → 흰 글자(tile_text) 4.5 아래면 검정 쪽으로 */
    fun tileFill(section: Int, lightness: Double, tileText: Int): Int = reach(over(WHITE, lightness, section), BLACK, tileText)

    /** 숫자 칸 바탕(웹 color-mix(in srgb, var(--c) w%, transparent))을 실제 아래 바탕(under — 목록 bg · 상세 surface)에 얹은 색 — 바뀌지 않음 */
    fun scoreBackground(section: Int, weightPercent: Int, under: Int): Int = over(section, weightPercent / 100.0, under)

    /** 숫자 칸 글자(숫자 · "연관기사"): 웹 color-mix(in srgb, var(--c) 80%, var(--text)) → 실제 바탕 위 4.5 아래면 text 쪽으로 */
    fun scoreText(section: Int, text: Int, weightPercent: Int, under: Int): Int =
        reach(mix(section, text, 0.8), text, scoreBackground(section, weightPercent, under))

    /** 반투명 글자(웹 opacity · 알파): 웹 = fg를 alpha로 under 위에 → 4.5 아래면 fg 쪽으로(= 알파를 올림) */
    fun fadedText(fg: Int, alpha: Double, under: Int): Int = reach(over(fg, alpha, under), fg, under)

    /**
     * 요약 펼치기 끈의 끔 상태 막대(웹 .track = line — 상태 그림, C11 ⑧): 바탕(bg) · 손잡이(knob = surface) 둘 다와 3:1이 될 때까지만 text 쪽으로.
     * text 쪽으로 옮기면 두 대비가 함께 커져서(막대가 바탕 · 손잡이보다 text에 가까움) 차례로 맞춰도 앞 조건이 깨지지 않음. 켬 상태(ink)는 웹 그대로.
     */
    fun toggleTrackOff(line: Int, text: Int, bg: Int, knob: Int): Int =
        reach(reach(line, text, bg, GRAPHIC_MIN), text, knob, GRAPHIC_MIN)

    /**
     * 글 입력 칸의 초점 없는 테두리(C12 ⑥ — WCAG 1.4.11 입력 칸 경계 3:1): 웹 테두리 색을 글자색 쪽으로, 바로 닿는 색(칸 바탕 · 칸을 둘러싼 창 바탕) **모두**와
     * 3:1이 될 때까지만(0.001씩 · 8비트로 내린 색으로 잼 — 한 걸음 덜 옮기면 3 미만). 이미 넘으면 웹 색 그대로.
     * 초점 테두리 · 글자가 있는 단추와 칩 · 카드와 창 테두리는 대상 밖(C12).
     */
    fun inputBorder(border: Int, text: Int, vararg adjacent: Int): Int {
        val start = border or (0xff shl 24)
        fun fits(c: Int) = adjacent.all { ratio(c, it) >= GRAPHIC_MIN }
        if (fits(start)) return start
        for (i in 1..STEPS) {
            val c = mix(text, start, i.toDouble() / STEPS)
            if (fits(c)) return c
        }
        return text or (0xff shl 24)
    }
}
