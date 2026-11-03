package io.github.jwavascript.issuerank.data

import kotlin.math.max
import kotlin.math.min

/** squarify 결과 한 칸(넣은 항목 + 사각형) */
data class Placed<T>(val item: T, val x: Double, val y: Double, val w: Double, val h: Double)

/**
 * 웹 squarify(index.html 719~751)를 줄 단위로 옮김 — 같은 입력이면 같은 사각형(IEEE double 연산 순서까지 같음).
 * 정렬 = 넓이 내림차순 안정 정렬(JS sort 비교 b.a − a.a: 같으면 넣은 순서, ±0 같음). value가 NaN이면 결과는 정하지 않음(웹도 같음).
 */
fun <T> squarify(items: List<T>, value: (T) -> Double, x0: Double, y0: Double, w0: Double, h0: Double): List<Placed<T>> {
    var total = 0.0
    for (it in items) total += value(it)
    // JS `!total`: 0 · -0 · NaN
    if (total == 0.0 || total.isNaN() || w0 <= 0 || h0 <= 0) return emptyList()
    val scale = (w0 * h0) / total

    class Area(val item: T, val a: Double)

    val rest = items.map { Area(it, value(it) * scale) }.sortedWith { p, q ->
        val d = q.a - p.a
        if (d > 0) 1 else if (d < 0) -1 else 0
    }
    val out = ArrayList<Placed<T>>(items.size)
    var x = x0
    var y = y0
    var w = w0
    var h = h0

    fun sum(row: List<Area>): Double {
        var s = 0.0
        for (r in row) s += r.a
        return s
    }

    fun worst(row: List<Area>, side: Double): Double {
        val s = sum(row)
        var mx = 0.0
        var mn = Double.POSITIVE_INFINITY
        for (r in row) { mx = max(mx, r.a); mn = min(mn, r.a) }
        return max((side * side * mx) / (s * s), (s * s) / (side * side * mn))
    }

    fun layout(row: List<Area>) {
        val s = sum(row)
        if (w >= h) { // 왼쪽에 세로 열
            val cw = s / h
            var cy = y
            for (r in row) { val rh = r.a / cw; out.add(Placed(r.item, x, cy, cw, rh)); cy += rh }
            x += cw; w -= cw
        } else { // 위쪽에 가로 행
            val rh = s / w
            var cx = x
            for (r in row) { val rw = r.a / rh; out.add(Placed(r.item, cx, y, rw, rh)); cx += rw }
            y += rh; h -= rh
        }
    }

    var row = ArrayList<Area>()
    var next = 0 // rest.shift() 대신 앞에서부터 읽음
    while (next < rest.size) {
        val side = min(w, h)
        if (row.isEmpty() || worst(row + rest[next], side) <= worst(row, side)) row.add(rest[next++])
        else { layout(row); row = ArrayList() }
    }
    if (row.isNotEmpty()) layout(row)
    return out
}
