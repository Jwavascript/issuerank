package io.github.jwavascript.issuerank.data

import io.github.jwavascript.issuerank.data.TestFiles.webCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** squarify = 웹 함수(index.html 719~751)를 vm으로 돌린 사각형(web/squarify.json) — 순서 · 소수 셋째 자리까지 */
class SquarifyTest {
    private fun num(v: Any): Double = when (v) {
        is Number -> v.toDouble()
        else -> v.toString().toDouble() // "NaN" · "Infinity"
    }

    @Test
    fun equalsWebForEveryCase() {
        val cases = webCases("squarify.json")
        assertEquals(21, cases.size)
        for (c in cases) {
            val values = c.getJSONArray("values").let { a -> List(a.length()) { num(a.get(it)) } }
            val out = squarify(values.indices.toList(), { values[it] }, num(c.get("x")), num(c.get("y")), num(c.get("w")), num(c.get("h")))
            val expected = c.getJSONArray("out")
            val name = c.getString("name")
            assertEquals("$name 개수", expected.length(), out.size)
            for (i in 0 until expected.length()) {
                val e = expected.getJSONArray(i)
                val r = out[i]
                assertEquals("$name[$i] 항목(정렬 · 안정성)", e.getInt(0), r.item)
                assertWebNumber("$name[$i].x", e.get(1), r.x)
                assertWebNumber("$name[$i].y", e.get(2), r.y)
                assertWebNumber("$name[$i].w", e.get(3), r.w)
                assertWebNumber("$name[$i].h", e.get(4), r.h)
            }
        }
    }

    @Test
    fun areasAddUpAndTiesKeepInputOrder() {
        val out = squarify(listOf("a", "b", "c", "d"), { 1.0 }, 0.0, 0.0, 200.0, 100.0)
        assertEquals(listOf("a", "b", "c", "d"), out.map { it.item })
        assertEquals(20000.0, out.sumOf { it.w * it.h }, 1e-9)
        assertTrue(squarify(listOf(1.0), { it }, 0.0, 0.0, 0.0, 10.0).isEmpty())
        assertTrue(squarify(listOf(0.0, 0.0), { it }, 0.0, 0.0, 10.0, 10.0).isEmpty())
    }
}
