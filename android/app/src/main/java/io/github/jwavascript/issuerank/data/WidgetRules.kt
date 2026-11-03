package io.github.jwavascript.issuerank.data

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 위젯이 그릴 것: 받은(또는 캐시) 목록, 없으면 실패 안내 "불러오지 못했습니다" */
data class WidgetView(val widget: Widget?, val fromCache: Boolean) {
    val failed: Boolean get() = widget == null
}

/** 위젯 규칙의 순수 부분(계획 4절 · widget.js). 그리기 · 깨우기 · 캐시 파일은 위젯 구현(widget/)이 함 */
object WidgetRules {
    const val CONNECT_TIMEOUT_MS = IssueRankApi.CONNECT_TIMEOUT_MS
    const val READ_TIMEOUT_MS = 4_000
    /** 본문 64KB */
    const val MAX_BODY_BYTES = 64 * 1024
    /** 응답 items 중 앞 30개만(openapi maxItems 30) */
    const val MAX_ITEMS = 30
    const val MAX_ROWS = 10
    /** AppWidgetProviderInfo updatePeriodMillis(공식 하한 30분) */
    const val UPDATE_PERIOD_MS = 1_800_000L

    /** 줄 수 = min(10, max(1, ⌊(h − 24 − 20f) ÷ 22f⌋)), h = 위젯 높이 dp, f = 글자 배율. f=1: 117→3 · 150→4 · 260→9 · 290→10 */
    fun rows(heightDp: Double, fontScale: Double): Int =
        min(MAX_ROWS.toDouble(), max(1.0, floor((heightDp - 24 - 20 * fontScale) / (22 * fontScale)))).toInt()

    /** widget.js 95: 줄 밖 · 하단 누르기 = morePath(문자열 · /로 시작)일 때만, 아니면 / */
    fun morePath(widget: Widget): String = widget.morePath?.takeIf { it.startsWith("/") } ?: AppRoutes.HOME

    /** widget.js 100: 실패 안내를 누르면 첫 화면 */
    const val FAIL_PATH = AppRoutes.HOME

    /**
     * widget.js refreshAfter(58~65): 보여 주는 date가 KST 오늘이면 3시간 뒤, 아니면(실패 안내 포함) KST 오늘 04:30과 30분 뒤 중 늦은 때.
     * 30분마다 깨면 이 시각 전에는 요청하지 않음(dueForRefresh).
     */
    fun refreshAfter(shownDate: String?, nowMs: Long): Long {
        val today = KstDate.today(java.time.Clock.fixed(java.time.Instant.ofEpochMilli(nowMs), KstDate.OFFSET))
        if (shownDate == today) return nowMs + 3 * 3_600_000L
        val at0430 = LocalDate.parse(today).atTime(LocalTime.of(4, 30)).toInstant(KstDate.OFFSET).toEpochMilli()
        return max(at0430, nowMs + 30 * 60_000L)
    }

    fun dueForRefresh(nowMs: Long, nextRefreshAtMs: Long?): Boolean = nextRefreshAtMs == null || nowMs >= nextRefreshAtMs

    /**
     * widget.js 67~74 `load() || cached()`: 이번에 받았으면(200 · 형식 맞음) 그것 + 캐시에 그 원문을 씀(writeCache),
     * 못 받았으면 캐시 원문을 다시 검사해 맞을 때만, 없거나 깨졌으면 실패 안내. 실패 응답은 캐시에 쓰지 않음.
     */
    fun resolve(fetched: ApiResult<WidgetPayload>?, cachedJson: String?): Resolution {
        if (fetched is ApiResult.Ok) return Resolution(WidgetView(fetched.value.widget, fromCache = false), writeCache = fetched.value.json)
        val cached = cachedJson?.let { json -> try { ApiJson.widget(json) } catch (e: Exception) { null } }
        return Resolution(WidgetView(cached, fromCache = cached != null), writeCache = null)
    }

    /** writeCache = 캐시 파일에 덮어쓸 원문(성공일 때만) */
    data class Resolution(val view: WidgetView, val writeCache: String?) {
        override fun toString(): String = "Resolution(view=$view, writeCache=${writeCache?.length})"
    }
}
