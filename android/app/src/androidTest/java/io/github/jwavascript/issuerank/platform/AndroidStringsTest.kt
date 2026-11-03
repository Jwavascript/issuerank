package io.github.jwavascript.issuerank.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Share
import io.github.jwavascript.issuerank.data.Str
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** 기기 리소스로 Str → 글(getString(id, args)): JVM 시험(XmlStrings)과 같은 글. 지금은 컴파일만(C2) */
@RunWith(AndroidJUnit4::class)
class AndroidStringsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val s = AndroidStrings(context)

    @Test
    fun everyKeyResolvesToItsOwnResource() {
        for (str in Str.entries) assertEquals(str.resName, context.resources.getResourceEntryName(str.res))
    }

    @Test
    fun formatsLikeTheWeb() {
        assertEquals("11월 3일 (화)", KstDate.fmtDate("2026-11-03", s))
        assertEquals("11.3 오전 4:31", KstDate.fmtUpdated("2026-11-03T04:31:27+09:00", s))
        assertEquals("오늘 가장 많이 보도된 이슈", s[Str.RANK_TITLE_TODAY])
        // debug 앱 이름은 "이슈랭크 시험"(src/debug) — 공유 제목은 brand "이슈랭크"
        assertEquals("이슈랭크 시험", s[Str.APP_NAME])
        assertEquals("이슈랭크", Share.site(s).title)
    }
}
