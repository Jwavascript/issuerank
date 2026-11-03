package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.TestFiles.webCases
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 이슈맵 계산 = 웹 renderMap + drawMap을 가짜 DOM으로 그대로 돌린 값(web/issuemap.json, 38사례: 랭킹 5 × 폭 360 · 640 · 641 · 753 · 1205 · 1400 × 확대 없음/경제).
 * 블록 · 타일 사각형 · 순서 · 숨김 · 라벨 숫자 · 확대 대상 · 밝기(--l) · 글자 크기 · 줄 수 · xs/xxs · 타일 글 · 경로 줄 글.
 */
class IssueMapTest {
    private val s = XmlStrings
    private val seed = ApiJson.ranking(TestFiles.fixture("rankings-d0.json"))

    /** gen-web-expected.mjs와 같은 랭킹 변형 */
    private fun ranking(name: String): Ranking {
        fun counts(r: Ranking, section: Int, values: List<Int>) = r.copy(sections = r.sections.mapIndexed { i, sec ->
            if (i != section) sec else sec.copy(issues = sec.issues.mapIndexed { j, it -> if (j < values.size) it.copy(articleCount = values[j]) else it })
        })
        return when (name) {
            "seed-d0" -> seed
            "ties" -> seed.copy(sections = seed.sections.map { sec -> sec.copy(issues = sec.issues.map { it.copy(articleCount = 50) }) })
            "zero" -> counts(seed, 1, listOf(5, 0, 0, 3, 1)).let { r -> r.copy(sections = r.sections.mapIndexed { i, sec -> if (i == 0) sec.copy(issues = emptyList()) else sec }) }
            "huge" -> counts(seed, 0, listOf(1000000000, 999999999, 5, 2, 1))
            "partial" -> seed.copy(sections = seed.sections.mapIndexed { i, sec -> sec.copy(issues = sec.issues.take(listOf(1, 2, 3, 1, 5, 4)[i])) })
            else -> error(name)
        }
    }

    @Test
    fun layoutEqualsWeb() {
        val cases = webCases("issuemap.json")
        assertEquals(38, cases.size)
        for (c in cases) check(c)
    }

    private fun check(c: JSONObject) {
        val vw = c.getDouble("vw")
        val zoom = SectionKey.of(c.text("zoom"))
        val r = ranking(c.getString("ranking"))
        val id = "${c.getString("ranking")} ${vw.toInt()}x${c.getInt("vh")} zoom=$zoom"
        assertEquals("$id narrow", c.getBoolean("narrow"), IssueMap.isNarrow(vw))
        assertEquals("$id width", c.getDouble("width"), IssueMap.mapWidth(vw), 0.0)
        val m = IssueMap.layout(r, zoom, IssueMap.isNarrow(vw), IssueMap.mapWidth(vw), c.getDouble("vh"))
        assertEquals("$id height", c.getDouble("height"), m.height, 0.0005)
        assertEquals("$id crumb", c.getString("crumb"), IssueMap.crumbTotal(m, zoom, r, s))
        val boxes = c.getJSONArray("boxes").objects()
        assertEquals("$id boxes", boxes.map { it.getString("s") }, m.boxes.map { it.section.key })
        for ((e, b) in boxes.zip(m.boxes)) {
            val bid = "$id ${b.section.key}"
            assertWebNumber("$bid x", e.get("x"), b.x)
            assertWebNumber("$bid y", e.get("y"), b.y)
            assertWebNumber("$bid w", e.get("w"), b.w)
            assertWebNumber("$bid h", e.get("h"), b.h)
            assertEquals("$bid hidden", e.getBoolean("hidden"), b.hidden)
            assertEquals("$bid zoom", e.getString("zoom"), b.zoomTarget?.key ?: "")
            assertEquals("$bid total", e.getInt("total"), b.total)
            assertEquals("$bid arrow", e.getString("arrow"), s[b.arrow])
            val tiles = e.getJSONArray("tiles").objects()
            assertEquals("$bid tiles", tiles.map { it.getString("id") }, b.tiles.map { it.issue.id })
            for ((te, t) in tiles.zip(b.tiles)) {
                val tid = "$bid ${t.issue.id}"
                assertWebNumber("$tid x", te.get("x"), t.x)
                assertWebNumber("$tid y", te.get("y"), t.y)
                assertWebNumber("$tid w", te.get("w"), t.w)
                assertWebNumber("$tid h", te.get("h"), t.h)
                assertEquals("$tid l", te.getString("l"), t.lightnessText)
                assertWebNumber("$tid fs", te.get("fs"), t.fontSize)
                assertWebNumber("$tid lines", te.get("lines"), t.lines)
                assertEquals("$tid xs", te.getBoolean("xs"), t.xs)
                assertEquals("$tid xxs", te.getBoolean("xxs"), t.xxs)
                assertEquals("$tid rk", te.getString("rk"), IssueMap.tileRank(t.issue, s))
                assertEquals("$tid n", te.getString("n"), IssueMap.tileCount(t.issue, s))
                assertEquals("$tid aria", te.getString("aria"), IssueMap.tileDescription(t.issue, s))
            }
        }
    }

    @Test
    fun narrowStacksSectionBlocks_wideUsesClampedHeight() {
        val phone = IssueMap.layout(seed, null, true, IssueMap.mapWidth(360.0), 800.0)
        assertEquals(true, phone.stacked)
        assertEquals(6 * 262.0, phone.height, 0.0) // boxH = round(min(320, max(260, 328 × 0.8)))
        val tablet = IssueMap.layout(seed, null, false, IssueMap.mapWidth(753.0), 1205.0)
        assertEquals(false, tablet.stacked)
        assertEquals(820.0, tablet.height, 0.0)
        assertEquals(440.0, IssueMap.layout(seed, null, false, 1157.0, 600.0).height, 0.0)
        assertEquals(30, tablet.boxes.sumOf { it.tiles.size })
        assertEquals("연관기사 합계 2,446건", IssueMap.crumbTotal(tablet, null, seed, s))
        assertEquals("연관기사 630건", IssueMap.crumbTotal(tablet, SectionKey.ECO, seed, s))
        assertEquals("1위 · 199건", IssueMap.tileXs(seed.issueById("2026-11-03-eco-20")!!, s))
    }

    @Test
    fun scoreWeightAndRatio() {
        assertEquals(36, IssueMap.scoreWeight(199, 199))
        assertEquals(10, IssueMap.scoreWeight(0, 199))
        assertEquals(0.0, IssueMap.ratio(5, 0), 0.0)
        assertEquals(1.0, IssueMap.ratio(300, 199), 0.0)
        assertEquals(Math.round(10 + 26 * Math.log1p(14.0) / Math.log1p(199.0)).toInt(), IssueMap.scoreWeight(14, 199))
    }
}
