package io.github.jwavascript.issuerank

import io.github.jwavascript.issuerank.IgnoredExtra.Reason.NOT_ALLOWED
import io.github.jwavascript.issuerank.IgnoredExtra.Reason.OUT_OF_RANGE
import io.github.jwavascript.issuerank.IgnoredExtra.Reason.UNKNOWN_NAME
import io.github.jwavascript.issuerank.IgnoredExtra.Reason.WRONG_TYPE
import io.github.jwavascript.issuerank.data.Web

/** debug 서버 주소 고르기(adb Extra server · 화면 단추): fake = 가짜 서버 · prod = 운영 · default = 덮어쓰기 지움(debug 기본 = 가짜 서버) */
enum class DebugServerChoice(val key: String, val url: String?) {
    FAKE("fake", "http://127.0.0.1:3100"),
    PROD("prod", "https://issuerank-kappa.vercel.app"),
    DEFAULT("default", null);

    companion object {
        fun of(key: String): DebugServerChoice? = entries.firstOrNull { it.key == key }
    }
}

/** 쓰지 않은 Extra 하나(화면 "무시함") — 이름과 까닭만(값은 적지 않음) */
data class IgnoredExtra(val key: String, val reason: Reason) {
    enum class Reason { UNKNOWN_NAME, WRONG_TYPE, NOT_ALLOWED, OUT_OF_RANGE }
}

/** 걸러 낸 Extra(없는 칸 = 화면 기본값) */
data class DebugRequest(
    val state: DebugWidgetStates.State? = null,
    val heightDp: Int? = null,
    val dark: Boolean? = null,
    val scale: Float? = null,
    val click: Int? = null,
    val server: DebugServerChoice? = null,
    val ignored: List<IgnoredExtra> = emptyList(),
)

/**
 * debug 도구 adb Extra(검증 11 · 12 — adb input 없이 `am start`만으로 돎). 허용 목록만 받음:
 * state(--es ok30 · ok3 · cache · fail · live) · height(--ei 40~600) · dark(--ez) · scale(--ef 0.85~2) ·
 * click(--ei 1~10 — 미리보기에서 그 줄의 PendingIntent를 실제로 보냄 = 앱 상세) · server(--es fake · prod · default — 자유 주소는 화면 입력으로만).
 * 이름 · 형 · 값 · 범위가 틀리면 그 Extra는 쓰지 않고 [DebugRequest.ignored]에 적음. 순수(Android 0 — JVM 시험).
 */
object DebugExtras {
    const val STATE = "state"
    const val HEIGHT = "height"
    const val DARK = "dark"
    const val SCALE = "scale"
    const val CLICK = "click"
    const val SERVER = "server"

    val NAMES = setOf(STATE, HEIGHT, DARK, SCALE, CLICK, SERVER)
    val HEIGHTS = 40..600
    val SCALES = 0.85f..2f
    val CLICKS = 1..10

    /** [extras] = 이름 → 그 이름의 형으로 꺼낸 값(state · server = String, height · click = Int, dark = Boolean, scale = Float). 형이 다르면 null */
    fun parse(extras: Map<String, Any?>): DebugRequest {
        val ignored = mutableListOf<IgnoredExtra>()
        var r = DebugRequest()
        for ((key, value) in extras.toSortedMap()) {
            val reason: IgnoredExtra.Reason? = when (key) {
                STATE -> if (value !is String) WRONG_TYPE else {
                    val state = DebugWidgetStates.State.of(value)
                    if (state == null) NOT_ALLOWED else { r = r.copy(state = state); null }
                }
                SERVER -> if (value !is String) WRONG_TYPE else {
                    val server = DebugServerChoice.of(value)
                    if (server == null) NOT_ALLOWED else { r = r.copy(server = server); null }
                }
                HEIGHT -> if (value !is Int) WRONG_TYPE else if (value !in HEIGHTS) OUT_OF_RANGE else { r = r.copy(heightDp = value); null }
                CLICK -> if (value !is Int) WRONG_TYPE else if (value !in CLICKS) OUT_OF_RANGE else { r = r.copy(click = value); null }
                SCALE -> if (value !is Float) WRONG_TYPE else if (value !in SCALES) OUT_OF_RANGE else { r = r.copy(scale = value); null }
                DARK -> if (value !is Boolean) WRONG_TYPE else { r = r.copy(dark = value); null }
                else -> UNKNOWN_NAME
            }
            if (reason != null) ignored += IgnoredExtra(key, reason)
        }
        return r.copy(ignored = ignored)
    }

    /** 화면 직접 입력(자유 주소는 여기로만): 앞뒤 공백을 뺀 http · https 주소(출처를 셀 수 있을 때), 아니면 null */
    fun customServer(input: String): String? {
        val url = input.trim()
        val ok = (url.startsWith("http://") || url.startsWith("https://")) && Web.originOf(url) != null
        return if (ok) url else null
    }
}
