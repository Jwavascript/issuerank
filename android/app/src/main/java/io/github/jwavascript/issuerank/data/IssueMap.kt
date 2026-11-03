package io.github.jwavascript.issuerank.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.floor
import kotlin.math.ln1p
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** 이슈맵 타일 하나(섹션 블록 안 좌표 — 웹 tile.style left · top · width · height) */
data class MapTile(
    val issue: Issue,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    /** 제목 글자 크기(px = dp) */
    val fontSize: Double,
    /** 제목 줄 수 한도(webkitLineClamp). 넓이 0 타일은 NaN(웹도 NaN) — 그릴 때는 maxLines */
    val lines: Double,
    /** 작은 타일: 제목 · 표시 숨김, 가운데 "n위 · m건" */
    val xs: Boolean,
    /** 아주 작은 타일: 글 없음 */
    val xxs: Boolean,
    /** 흰 덧칠 세기 rgba(255,255,255,l) — 웹 toFixed(3) 값 */
    val lightness: Double,
    /** 웹 --l 글자 그대로("0.135") */
    val lightnessText: String,
) {
    val maxLines: Int get() = if (lines.isNaN()) 1 else lines.toInt()
}

/** 섹션 블록 하나(지도 좌표 — 웹 group-box). hidden = 확대 · 쌓기에서 안 보이는 섹션(넓이 0 · 투명) */
data class MapBox(
    val section: SectionKey,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val hidden: Boolean,
    /** 라벨 숫자 = 그 섹션 연관기사 합계 */
    val total: Int,
    /** 라벨 누르면: 전체일 때 그 섹션으로 확대, 확대 중이면 null(= 전체로) */
    val zoomTarget: SectionKey?,
    /** 라벨 끝 글 "확대 →" / "← 전체" */
    val arrow: Str,
    val tiles: List<MapTile>,
)

data class MapLayout(val width: Double, val height: Double, val stacked: Boolean, val boxes: List<MapBox>, val totalArticles: Int)

/**
 * 이슈맵 계산(웹 drawMap 770~828 · renderMap 757~768의 DOM 말고 계산만). 크기 단위 px = dp.
 * 같은 입력(랭킹 · 확대 · 좁은 화면 · 지도 폭 · 화면 높이) → 웹과 같은 블록 · 타일 사각형 · 글자 단계.
 */
object IssueMap {
    const val GAP = 2.0     // 753
    const val LABEL_H = 22.0 // 753
    /** 640px 이하 = 좁은 화면(754 matchMedia('(max-width: 640px)')) */
    const val NARROW_MAX = 640.0

    fun isNarrow(viewportWidth: Double): Boolean = viewportWidth <= NARROW_MAX

    /** 지도 폭 = .wrap-wide 안쪽(CSS 138 max-width 1240 · 좌우 24, 640 이하 16 — 390 · box-sizing border-box 64) */
    fun mapWidth(viewportWidth: Double): Double =
        min(1240.0, viewportWidth) - 2 * (if (isNarrow(viewportWidth)) 16.0 else 24.0)

    /** 웹 L(n) = Math.log1p(n)(613) */
    fun weight(articleCount: Int): Double = ln1p(articleCount.toDouble())

    /** 614 ratio(n, max) = max > 0 ? min(1, ln(1+n) / ln(1+max)) : 0 */
    fun ratio(n: Int, max: Int): Double = if (max > 0) min(1.0, weight(n) / weight(max)) else 0.0

    /** 카드 숫자 칸 바탕 세기 %(678 · 919): round(10 + 26 × ratio) */
    fun scoreWeight(n: Int, max: Int): Int = Math.round(10 + 26 * ratio(n, max)).toInt()

    /**
     * @param width 지도 폭(웹 map.clientWidth — mapWidth(화면 폭))
     * @param viewportHeight 화면 높이(웹 100vh) — 넓은 화면 지도 높이 clamp(440, vh − 260, 820)(CSS 251)
     */
    fun layout(ranking: Ranking, zoom: SectionKey?, narrow: Boolean, width: Double, viewportHeight: Double): MapLayout {
        val stack = narrow && zoom == null // 774
        val shown = SectionKey.entries.filter { ranking.issuesOf(it).isNotEmpty() }
        val boxH = Math.round(min(320.0, max(260.0, width * 0.8))).toDouble() // 776
        val height = when { // 777 + CSS(쌓기 = 블록 수 × boxH · 640 이하 760(394) · 그 밖 clamp(251))
            stack -> shown.size * boxH
            narrow -> 760.0
            else -> max(440.0, min(viewportHeight - 260, 820.0))
        }
        val w = width
        val h = height
        val groups: List<Placed<SectionKey>> = when { // 779~783 (hidden = 넓이 0 블록)
            zoom != null -> SectionKey.entries.map { if (it == zoom) Placed(it, 0.0, 0.0, w, h) else Placed(it, w / 2, h / 2, 0.0, 0.0) }
            stack -> SectionKey.entries.map { s ->
                val i = shown.indexOf(s)
                if (i < 0) Placed(s, w / 2, 0.0, 0.0, 0.0) else Placed(s, 0.0, i * boxH, w, boxH)
            }
            else -> squarify(SectionKey.entries, { if (ranking.issuesOf(it).isNotEmpty()) 1.0 else 0.0 }, 0.0, 0.0, w, h)
        }
        val hiddenOf = { s: SectionKey -> zoom != null && s != zoom || zoom == null && stack && s !in shown }
        val top = ranking.maxArticleCount
        val boxes = groups.map { r ->
            val bw = max(0.0, r.w - GAP)
            val bh = max(0.0, r.h - GAP)
            val hidden = hiddenOf(r.item)
            val tiles = if (hidden) emptyList() else squarify(ranking.issuesOf(r.item), { weight(it.articleCount) }, 0.0, LABEL_H, bw, bh - LABEL_H)
                .map { t -> tile(t, zoom != null, top) }
            MapBox(
                section = r.item, x = r.x + GAP / 2, y = r.y + GAP / 2, w = bw, h = bh, hidden = hidden,
                total = ranking.sectionTotal(r.item),
                zoomTarget = if (zoom != null) null else r.item,
                arrow = if (zoom != null) Str.MAP_ZOOM_OUT else Str.MAP_ZOOM_IN,
                tiles = tiles,
            )
        }
        return MapLayout(w, h, stack, boxes, SectionKey.entries.sumOf { ranking.sectionTotal(it) })
    }

    private fun tile(t: Placed<Issue>, zoomed: Boolean, top: Int): MapTile { // 809~821
        val tw = max(0.0, t.w - GAP)
        val th = max(0.0, t.h - GAP)
        val fs = max(11.5, min(if (zoomed) 24.0 else 17.0, sqrt(tw * th) / 10.5))
        val lines = max(1.0, floor((th - 36) / (fs * 1.32)))
        val l = toFixed3(0.24 * (1 - ratio(t.item.articleCount, top)))
        return MapTile(
            issue = t.item, x = t.x + GAP / 2, y = t.y + GAP / 2, w = tw, h = th, fontSize = fs, lines = lines,
            xs = th < 48 || tw < 72, xxs = th < 20 || tw < 30, lightness = l.toDouble(), lightnessText = l.toPlainString(),
        )
    }

    /** JS Number.prototype.toFixed(3): 이진 값 그대로에서 반올림(같은 거리면 큰 쪽) */
    private fun toFixed3(v: Double): BigDecimal = BigDecimal(v).setScale(3, RoundingMode.HALF_UP)

    // ── 글(Strings): 라벨 · 경로 줄 · 타일 ──

    /** 경로 줄(762~764): 확대 = 섹션 합계 "연관기사 n건", 전체 = "연관기사 합계 1,234건"(toLocaleString) */
    fun crumbTotal(layout: MapLayout, zoom: SectionKey?, ranking: Ranking, s: Strings): String =
        if (zoom != null) s[Str.MAP_SECTION_TOTAL, ranking.sectionTotal(zoom)]
        else s[Str.MAP_TOTAL, String.format(Locale.KOREA, "%,d", layout.totalArticles)]

    /** 타일 아래 "n위" · "m건", 작은 타일은 한 줄 "n위 · m건" */
    fun tileRank(issue: Issue, s: Strings): String = s[Str.RANK_N, issue.rank]
    fun tileCount(issue: Issue, s: Strings): String = s[Str.COUNT_N, issue.articleCount]
    fun tileXs(issue: Issue, s: Strings): String = s[Str.TILE_XS, tileRank(issue, s), tileCount(issue, s)]

    /** 타일 aria-label(824) → contentDescription: "정치 1위 제목, 연관기사 120건" */
    fun tileDescription(issue: Issue, s: Strings): String =
        s[Str.CD_TILE, s[issue.section.label], issue.rank, issue.title, issue.articleCount]
}
