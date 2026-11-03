package io.github.jwavascript.issuerank.platform

import android.content.Context
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.Strings

/** data 층 Strings = 앱 리소스(getString(id, args) — 자리 표시 채움) */
class AndroidStrings(private val context: Context) : Strings {
    override fun get(key: Str, vararg args: Any): String =
        if (args.isEmpty()) context.getString(key.res) else context.getString(key.res, *args)
}
