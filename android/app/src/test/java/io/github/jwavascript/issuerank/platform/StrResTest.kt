package io.github.jwavascript.issuerank.platform

import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Test

/** Str → R.string 표(StrRes.kt)가 같은 이름의 리소스를 가리킴(R 필드 이름으로 대조 — 기기 없이) */
class StrResTest {
    @Test
    fun everyKeyPointsAtTheSameNamedResource() {
        val wrong = Str.entries.filter { str ->
            val field = runCatching { R.string::class.java.getField(str.resName).getInt(null) }.getOrNull()
            field != str.res
        }
        assertEquals(emptyList<Str>(), wrong)
        assertEquals(Str.entries.size, Str.entries.map { it.res }.toSet().size)
    }
}
