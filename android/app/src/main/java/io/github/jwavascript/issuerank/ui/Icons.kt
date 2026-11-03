package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 웹 SVG(index.html · offline.html) → ImageVector(Material 아이콘 묶음 0 — 계획 6절). 색은 그릴 때 tint(currentColor 자리).
 * 크기 = 웹 width/height(px = dp), 선 두께 · 끝 모양 = 웹 stroke-width · linecap · linejoin.
 */
object WebIcons {
    /** 로고(offline.html 39 — 머리 brand-mark와 같은 꼴) */
    val brand: ImageVector by lazy {
        icon("brand", 18.dp, 40f) {
            fill(rect(0f, 0f, 24f, 40f, 3f))
            fill(rect(26f, 0f, 14f, 24f, 3f))
            fill(rect(26f, 26f, 14f, 14f, 3f), alpha = 0.45f)
        }
    }

    /** 보기 전환 · 이슈맵(445) */
    val map: ImageVector by lazy {
        icon("map", 13.dp, 24f) {
            fill(rect(2f, 2f, 12f, 20f, 1.5f))
            fill(rect(16f, 2f, 6f, 11f, 1.5f))
            fill(rect(16f, 15f, 6f, 7f, 1.5f))
        }
    }

    /** 보기 전환 · 랭킹(449) */
    val list: ImageVector by lazy { icon("list", 13.dp, 24f) { stroke("M4 6h16M4 12h16M4 18h10", 2.6f) } }

    /** 북마크(머리 462 · 14px · 선 2) */
    val bookmarkNav: ImageVector by lazy { icon("bookmarkNav", 14.dp, 24f) { stroke(BOOKMARK, 2f, StrokeJoin.Round) } }

    /** 카드 · 상세 북마크(ICON_BM 667 · 17px · 선 1.9), 켜짐 = 채움(.bm.on svg { fill: currentColor }) */
    val bookmark: ImageVector by lazy { icon("bookmark", 17.dp, 24f) { stroke(BOOKMARK, 1.9f, StrokeJoin.Round) } }
    val bookmarkOn: ImageVector by lazy {
        icon("bookmarkOn", 17.dp, 24f) {
            fill(BOOKMARK)
            stroke(BOOKMARK, 1.9f, StrokeJoin.Round)
        }
    }

    /** 이전 · 다음 날짜(482 · 490) */
    val prev: ImageVector by lazy { icon("prev", 14.dp, 24f) { stroke("M15 18l-6-6 6-6", 2.2f) } }
    val next: ImageVector by lazy { icon("next", 14.dp, 24f) { stroke("M9 6l6 6-6 6", 2.2f) } }

    /** 날짜 선택(485) */
    val calendar: ImageVector by lazy {
        icon("calendar", 14.dp, 24f) {
            stroke(rect(3f, 5f, 18f, 16f, 2f), 2f)
            stroke("M3 10h18M8 3v4M16 3v4", 2f)
        }
    }

    /** 기사 원문 ↗(ICON_EXT 668 · 11px) */
    val external: ImageVector by lazy { icon("external", 11.dp, 24f) { stroke("M7 17L17 7M9 7h8v8", 2.4f) } }

    /** 닫기(ICON_X 669 · 16px) */
    val close: ImageVector by lazy { icon("close", 16.dp, 24f) { stroke("M6 6l12 12M18 6L6 18", 2f) } }

    /** 이슈맵 타일 북마크 표시(ICON_MARK 670 · 10×12) */
    val mark: ImageVector by lazy { icon("mark", 10.dp, 10f, 12f) { fill("M0 0h10v12L5 8.6 0 12z") } }

    /** 상세 공유(938 · 17px) */
    val share: ImageVector by lazy {
        icon("share", 17.dp, 24f) { stroke("M12 15V3M8 7l4-4 4 4M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7", 1.9f, StrokeJoin.Round) }
    }

    /**
     * "Google로 계속하기" 표준 4색 G(로그인 창 549 · 20px — 색이 정해진 그림이라 tint 없이 Image로). faded = 바쁜 동안 단추 흐림(웹 :disabled opacity .38)을
     * 흰 단추 위에 미리 섞은 불투명 색(막힌 단추 — 대비 기준 밖).
     */
    val google: ImageVector by lazy { googleLogo(faded = false) }
    val googleFaded: ImageVector by lazy { googleLogo(faded = true) }

    private const val BOOKMARK = "M6 3.8h12v17l-6-4.2-6 4.2z"

    private val GOOGLE_PATHS = listOf(
        0xFFEA4335.toInt() to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
        0xFF4285F4.toInt() to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z",
        0xFFFBBC05.toInt() to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
        0xFF34A853.toInt() to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z",
    )

    private fun googleLogo(faded: Boolean): ImageVector = ImageVector.Builder("google", 20.dp, 20.dp, 48f, 48f).apply {
        for ((argb, d) in GOOGLE_PATHS) {
            val color = if (faded) Contrast.over(argb, LightDialog.DISABLED_GOOGLE, LightDialog.SURFACE) else argb
            addPath(addPathNodes(d), fill = SolidColor(Color(color)))
        }
    }.build()

    private fun icon(name: String, width: Dp, viewportWidth: Float, viewportHeight: Float = viewportWidth, block: ImageVector.Builder.() -> Unit) =
        ImageVector.Builder(name, width, width * (viewportHeight / viewportWidth), viewportWidth, viewportHeight).apply(block).build()

    private fun ImageVector.Builder.fill(d: String, alpha: Float = 1f) {
        addPath(addPathNodes(d), fill = SolidColor(Color.Black), fillAlpha = alpha)
    }

    private fun ImageVector.Builder.stroke(d: String, width: Float, join: StrokeJoin = StrokeJoin.Miter) {
        addPath(addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = join)
    }

    /** SVG <rect x y width height rx> → 경로 */
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val iw = w - 2 * r
        val ih = h - 2 * r
        return "M${x + r} ${y}h${iw}a$r $r 0 0 1 $r ${r}v${ih}a$r $r 0 0 1 ${-r} ${r}h${-iw}a$r $r 0 0 1 ${-r} ${-r}v${-ih}a$r $r 0 0 1 $r ${-r}z"
    }
}
