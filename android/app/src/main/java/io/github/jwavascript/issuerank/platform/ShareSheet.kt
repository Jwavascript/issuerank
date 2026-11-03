package io.github.jwavascript.issuerank.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import io.github.jwavascript.issuerank.data.Share

/**
 * 공유 창(계획 2절 · AC-13-2): ACTION_SEND text/plain, EXTRA_TEXT = "[섹션] 제목\n주소"(Share.Data.sendText — A2 7절 8).
 * 제목 = 웹 navigator.share의 title("이슈랭크" — Share.Data.title): 고르기 창 제목 + 미리보기 제목(EXTRA_TITLE).
 */
object ShareSheet {
    fun chooser(data: Share.Data): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, data.sendText)
            .putExtra(Intent.EXTRA_TITLE, data.title)
        return Intent.createChooser(send, data.title)
    }

    fun open(context: Context, data: Share.Data) {
        val activity = context.findActivity()
        val intent = chooser(data)
        if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            (activity ?: context).startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 고르기 창이 없는 기기 — 아무것도 안 함
        }
    }
}

/** Compose LocalContext(설정 덮어쓰기 감싸개 포함)에서 Activity 찾기 */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
