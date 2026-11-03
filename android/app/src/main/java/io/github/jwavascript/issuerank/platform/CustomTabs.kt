package io.github.jwavascript.issuerank.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import io.github.jwavascript.issuerank.R

/**
 * 원문 · 정책 링크 = Custom Tab(앱 안 브라우저 탭 — 계획 2절 · R12 "Custom Tab = Chrome", 웹 target=_blank 새 탭).
 * Chrome을 먼저(패키지 지정), 없으면 기본 브라우저. 탭 막대 색 = 웹 bg 토큰(지금 밝게/어둡게 — 기기 다크를 따름).
 * 패키지 조회(queries)는 하지 않음: startActivity는 패키지 공개 범위와 무관 → 매니페스트 변경 0.
 */
object CustomTabs {
    const val CHROME = "com.android.chrome"

    fun open(context: Context, url: String) {
        val activity = context.findActivity()
        val host = activity ?: context
        val tab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(context.getColor(R.color.bg)).build())
            .build()
        if (activity == null) tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.parse(url)
        tab.intent.setPackage(CHROME)
        try {
            tab.launchUrl(host, uri)
        } catch (e: ActivityNotFoundException) {
            tab.intent.setPackage(null)
            try { tab.launchUrl(host, uri) } catch (e: ActivityNotFoundException) { /* 브라우저 없음 — 아무것도 안 함 */ }
        }
    }

    /** Compose LocalContext(설정 덮어쓰기 감싸개 포함)에서 Activity 찾기 — 없으면 NEW_TASK로 엶 */
    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
