package io.github.jwavascript.issuerank.ui

import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.Links
import io.github.jwavascript.issuerank.data.Share
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.platform.ShareSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(공유 · 링크, AC-13-2 · 계획 2절): 공유 글 = 웹 issueShare · SITE_SHARE(걸이 기록 — 북마크 카드 · 맨 아래 "이슈랭크 공유") ·
 * 기본 걸이 = ACTION_SEND 고르기 창(text/plain · EXTRA_TEXT "[섹션] 제목\n주소" · 제목 "이슈랭크") · 개인정보처리방침 · 이용약관 = Custom Tab(맨 아래 · 로그인 창).
 * 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class ShareAndLinksTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val s get() = server.strings
    private val vm get() = compose.activity.viewModel

    @Test
    fun shareTexts_areTheWebText_forTheSiteAndABookmarkCard() {
        val email = "share.test@example.invalid"
        val cookie = FakeAccounts.signUp("공유 시험", email)
        val folder = FakeAccounts.createFolder(cookie, "공유")
        FakeAccounts.saveBookmark(cookie, "${server.today}-wor-2", folder)
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.SHARE_SITE).performScrollTo().performClick()
        compose.waitUntil(LOAD_TIMEOUT_MS) { server.shared.isNotEmpty() }
        assertEquals(Share.site(s), server.shared.last())
        assertEquals("${Share.BASE}/s", server.shared.last().url)
        compose.signInViaDialog(s, email)
        val saved = vm.state.value.bookmarks.single()
        compose.onNodeWithTag(TestTags.cardShare(saved.id)).performScrollTo().performClick()
        compose.waitUntil(LOAD_TIMEOUT_MS) { server.shared.size == 2 }
        assertEquals(Share.issue(saved.issue, s), server.shared.last())
        assertEquals("[${s[saved.issue.section.label]}] ${saved.issue.title}\n${Share.issueUrl(saved.id)}", server.shared.last().sendText)
    }

    @Test
    fun defaultShare_isAnActionSendChooser_withTheWebText() {
        compose.waitForTiles(30)
        val issue = vm.state.value.ranking!!.issueById("${server.today}-pol-21")!!
        val data = Share.issue(issue, s)
        val chooser = ShareSheet.chooser(data)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(s[Str.BRAND], chooser.getCharSequenceExtra(Intent.EXTRA_TITLE).toString())
        val send = if (Build.VERSION.SDK_INT >= 33) {
            chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            chooser.getParcelableExtra(Intent.EXTRA_INTENT)
        }!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals(data.sendText, send.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(data.title, send.getStringExtra(Intent.EXTRA_TITLE))
    }

    @Test
    fun policyLinks_openInACustomTab_fromTheFooterAndTheLoginDialog() {
        compose.waitForTiles(30)
        compose.onNodeWithTag(TestTags.FOOTER_PRIVACY).performScrollTo().performClick()
        compose.onNodeWithTag(TestTags.FOOTER_TERMS).performScrollTo().performClick()
        compose.waitUntil(LOAD_TIMEOUT_MS) { server.opened.size == 2 }
        assertEquals(listOf(Links.PRIVACY, Links.TERMS), server.opened.toList())
        compose.openLoginFromBookmarks()
        compose.onNodeWithTag(TestTags.AUTH_TERMS).performScrollTo().performClick()
        compose.onNodeWithTag(TestTags.AUTH_PRIVACY).performScrollTo().performClick()
        compose.waitUntil(LOAD_TIMEOUT_MS) { server.opened.size == 4 }
        assertEquals(listOf(Links.PRIVACY, Links.TERMS, Links.TERMS, Links.PRIVACY), server.opened.toList())
    }
}
