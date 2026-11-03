package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView

/** 누르기 대상 하나: 칸(줄 0~9 · [WidgetTargets.MORE] · [WidgetTargets.FAIL]) → 앱 경로(줄이면 그 제목도) */
data class WidgetTarget(val slot: Int, val path: String, val title: String? = null)

/**
 * 위젯 누르기 대상(순수 — JVM 시험, 그리기는 WidgetViews.forRows가 이것대로): 보일 줄(식 · 항목 수 안)마다 그 줄 제목 · path(`/?issue=<id>`) ·
 * 줄 밖 · 하단 = morePath(`/#list`, 틀리면 `/`) · 실패(목록 없음) = 실패 안내 하나 → FAIL_PATH(`/`). 경로 → 화면은 MainActivity(A3).
 */
object WidgetTargets {
    /** 칸 수(PendingIntent requestCode = 위젯 id × 12 + 칸) */
    const val SLOTS = 12
    const val MORE = 10
    const val FAIL = 11

    fun of(view: WidgetView, rows: Int): List<WidgetTarget> {
        val widget = view.widget ?: return listOf(WidgetTarget(FAIL, WidgetRules.FAIL_PATH))
        return widget.items.take(minOf(rows, WidgetRules.MAX_ROWS)).mapIndexed { i, item -> WidgetTarget(i, item.path, item.title) } +
            WidgetTarget(MORE, WidgetRules.morePath(widget))
    }

    /** 위젯 · 칸마다 다른 requestCode(미리보기 위젯 id 0 = 0~11, 실제 위젯 id는 1부터) */
    fun requestCode(appWidgetId: Int, slot: Int): Int = appWidgetId * SLOTS + slot
}
