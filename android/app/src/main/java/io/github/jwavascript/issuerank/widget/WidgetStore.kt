package io.github.jwavascript.issuerank.widget

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/** 다음에 받을 시각 = 앱 전용 SharedPreferences 한 칸(epoch ms) */
class PrefsRefreshSchedule(private val prefs: SharedPreferences) : RefreshSchedule {
    override fun nextAt(): Long? = if (prefs.contains(KEY_NEXT_AT)) prefs.getLong(KEY_NEXT_AT, 0) else null

    override fun setNextAt(atMs: Long) = edit { putLong(KEY_NEXT_AT, atMs) }

    fun clear() = edit { remove(KEY_NEXT_AT) }

    private fun edit(change: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(change).apply()
    }

    companion object {
        const val KEY_NEXT_AT = "next_refresh_at"
    }
}

/**
 * 위젯 상태(앱 전용 — 세션 쿠키와 따로, allowBackup false): 캐시 = files/widget-cache.json(성공한 응답 원문) · 다음 시각 = SharedPreferences "issuerank_widget".
 * 위젯이 여럿이어도 데이터가 같아 하나만 둠.
 */
class WidgetStore(context: Context) {
    private val app = context.applicationContext
    val cache = FileWidgetCache(File(app.filesDir, CACHE_FILE))
    val schedule = PrefsRefreshSchedule(app.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    /** 마지막 위젯을 치웠을 때(onDisabled) · 시험: 다음에 놓으면 바로 받게 */
    fun clear() {
        cache.clear()
        schedule.clear()
    }

    companion object {
        const val PREFS = "issuerank_widget"
        const val CACHE_FILE = "widget-cache.json"
    }
}
