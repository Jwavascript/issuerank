package io.github.jwavascript.issuerank.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.MainActivity
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.BookmarkView
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.View
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(북마크 · 폴더, AC-07 · 08 · 10 · O6): 폴더 고르기(폴더 0 = 새 폴더만 · 여럿 = 서버 순서 · 개수) · 새 폴더(16자 · 17번째 글자 거절) ·
 * 저장 토스트 + "북마크 보기" · 해제 + "되돌리기"(같은 폴더로) · 북마크 화면 칩(개수) · 빈 문구 · 폴더 삭제(확인 창 개수 · 취소 = 요청 0).
 * 가짜 서버(README) · 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class BookmarkFlowTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val s get() = server.strings
    private val vm get() = compose.activity.viewModel
    private val graph get() = AppGraph.get(server.context)

    private fun issue(id: String): Issue = vm.state.value.ranking!!.issueById(id)!!

    /** 목록 맨 위 정치 카드의 북마크 단추(머리 밑에 붙는 섹션 탭이 가리지 않게 맨 위로) */
    private fun tapListBookmark(issue: Issue) {
        compose.onNodeWithTag(TestTags.TAB_LIST).performClick()
        compose.waitForTag(TestTags.VIEW_LIST)
        compose.onNodeWithTag(TestTags.RANK_TITLE).performScrollTo()
        compose.onNodeWithTag(TestTags.cardBookmark(issue.id)).performClick()
    }

    private fun appFolders() = (runBlocking { graph.api.folders() } as ApiResult.Ok).value

    private fun appBookmarks() = (runBlocking { graph.api.bookmarks() } as ApiResult.Ok).value

    @Test
    fun newFolder_17thCharacterRejected_saveViewUnsaveUndo_keepsTheFolder() {
        compose.waitForTiles(30)
        compose.signUpViaDialog(s, "북마크 시험", "bookmark.flow@example.invalid")
        val pol = issue(vm.state.value.ranking!!.issuesOf(SectionKey.POL).first().id)
        tapListBookmark(pol)
        compose.waitForTag(TestTags.FOLDER_PICKER)
        compose.waitForText(s[Str.FOLDER_SAVE_TITLE])
        compose.onAllNodes(hasTestTagPrefix(TestTags.FOLDER_OPTION_PREFIX)).assertCountEquals(0) // 새 계정 = 폴더 0
        val sixteen = "가나다라마바사아자차카타파하가나"
        compose.onNodeWithTag(TestTags.FOLDER_NEW_NAME).performTextInput(sixteen + "다") // 17번째 글자는 안 들어감(웹 maxlength)
        assertEquals(sixteen, vm.state.value.picker!!.name)
        compose.onNodeWithTag(TestTags.FOLDER_CREATE).performClick()
        compose.waitForText(s[Str.TOAST_SAVED, sixteen])
        compose.waitForTagGone(TestTags.FOLDER_PICKER)
        val folder = appFolders().single()
        assertEquals(sixteen, folder.name)
        assertEquals(listOf(pol.id to folder.id), appBookmarks().map { it.id to it.folderId })
        // "북마크 보기" = 그 폴더 칩으로 북마크 화면
        compose.onNodeWithTag(TestTags.TOAST_ACTION, useUnmergedTree = true).assertTextEquals(s[Str.TOAST_VIEW_BOOKMARKS]).performClick()
        compose.waitForTag(TestTags.VIEW_BOOKMARKS)
        assertEquals(View.BOOKMARKS, vm.state.value.view)
        compose.onNodeWithTag(TestTags.folderChip(folder.id)).assertIsSelected()
        compose.onNodeWithTag(TestTags.FOLDER_DELETE).assertExists()
        compose.onNodeWithTag(TestTags.BM_SUB).assertTextEquals(s[Str.BM_SUB, 1])
        val meta = hasAnyAncestor(hasTestTag(TestTags.bookmarkMeta(pol.id)))
        compose.onNode(hasText(s[Str.SECTION_RANK, s[SectionKey.POL.label], pol.rank]) and meta, useUnmergedTree = true).assertExists()
        compose.onNode(hasText(sixteen) and meta, useUnmergedTree = true).assertExists()
        // 북마크 화면 카드에서 해제 → "북마크를 해제했습니다" + "되돌리기" → 같은 폴더로
        compose.onNodeWithTag(TestTags.cardBookmark(pol.id)).assertContentDescriptionEquals(s[Str.CD_UNBOOKMARK]).performClick()
        compose.waitForText(s[Str.TOAST_UNSAVED])
        compose.waitForTag(TestTags.BM_EMPTY)
        assertTrue(appBookmarks().isEmpty())
        compose.onNodeWithTag(TestTags.TOAST_ACTION, useUnmergedTree = true).assertTextEquals(s[Str.TOAST_UNDO]).performClick()
        compose.waitForTag(TestTags.card(pol.id))
        assertEquals(listOf(pol.id to folder.id), appBookmarks().map { it.id to it.folderId })
    }

    @Test
    fun picker_existingFolders_inServerOrderWithCounts_savesIntoTheChosenOne() {
        val email = "picker.order@example.invalid"
        val cookie = FakeAccounts.signUp("고르기 시험", email)
        val f1 = FakeAccounts.createFolder(cookie, "반도체·AI")
        val f2 = FakeAccounts.createFolder(cookie, "부동산·금리")
        FakeAccounts.saveBookmark(cookie, "${server.today}-eco-1", f2)
        compose.waitForTiles(30)
        compose.signInViaDialog(s, email)
        val pol = issue("${server.today}-pol-21")
        tapListBookmark(pol)
        compose.waitForTag(TestTags.FOLDER_PICKER)
        val tags = compose.onAllNodes(hasTestTagPrefix(TestTags.FOLDER_OPTION_PREFIX)).fetchSemanticsNodes().map { it.tag() }
        assertEquals(listOf(TestTags.folderOption(f1), TestTags.folderOption(f2)), tags)
        compose.onNodeWithTag(TestTags.folderOption(f1)).assertTextEquals("반도체·AI", "0")
        compose.onNodeWithTag(TestTags.folderOption(f2)).assertTextEquals("부동산·금리", "1")
        compose.onNodeWithTag(TestTags.folderOption(f2)).performClick()
        compose.waitForText(s[Str.TOAST_SAVED, "부동산·금리"])
        assertEquals(f2, appBookmarks().first { it.id == pol.id }.folderId)
        compose.onNodeWithTag(TestTags.cardBookmark(pol.id)).assertContentDescriptionEquals(s[Str.CD_UNBOOKMARK])
    }

    @Test
    fun bookmarksScreen_chipsWithCounts_emptyText_folderDeleteConfirmsTheCount() {
        val email = "chips.delete@example.invalid"
        val cookie = FakeAccounts.signUp("칩 시험", email)
        val fa = FakeAccounts.createFolder(cookie, "반도체·AI")
        val fb = FakeAccounts.createFolder(cookie, "나중에 읽기")
        FakeAccounts.saveBookmark(cookie, "${server.today}-pol-21", fa)
        FakeAccounts.saveBookmark(cookie, "${server.today}-eco-1", fa)
        compose.waitForTiles(30)
        compose.signInViaDialog(s, email)
        compose.onNodeWithTag(TestTags.BM_SUB).assertTextEquals(s[Str.BM_SUB, 2])
        compose.onNodeWithTag(TestTags.folderChip(BookmarkView.ALL)).assertTextEquals(s[Str.ALL], "2").assertIsSelected()
        compose.onNodeWithTag(TestTags.folderChip(fa)).assertTextEquals("반도체·AI", "2")
        compose.onNodeWithTag(TestTags.folderChip(fb)).assertTextEquals("나중에 읽기", "0")
        compose.onAllNodesWithTag(TestTags.FOLDER_DELETE).assertCountEquals(0) // 전체 칩 = 폴더 삭제 없음
        val group = hasAnyAncestor(hasTestTag(TestTags.bookmarkGroup(0)))
        compose.onNode(hasText(s[Str.RANKING_OF_DATE, KstDate.fmtDate(server.today, s)]) and group, useUnmergedTree = true).assertExists()
        compose.onNode(hasText("2") and group, useUnmergedTree = true).assertExists()
        // 빈 폴더 칩 → 빈 문구 + 폴더 삭제 → 확인 창(빈 폴더 안내) → 취소 = 요청 0
        compose.onNodeWithTag(TestTags.folderChip(fb)).performClick()
        compose.waitForTag(TestTags.BM_EMPTY)
        compose.waitForText(s[Str.BM_EMPTY_TITLE])
        compose.onNodeWithTag(TestTags.FOLDER_DELETE).assertContentDescriptionEquals(s[Str.CD_FOLDER_DELETE, "나중에 읽기"]).performClick()
        compose.waitForTag(TestTags.FOLDER_DELETE_DIALOG)
        compose.onNodeWithTag(TestTags.FOLDER_DELETE_TITLE).assertTextEquals(s[Str.FOLDER_DELETE_TITLE, "나중에 읽기"])
        compose.onNodeWithTag(TestTags.FOLDER_DELETE_NOTE).assertTextEquals(s[Str.FOLDER_DELETE_NOTE_EMPTY])
        compose.onNodeWithTag(TestTags.FOLDER_DELETE_CANCEL).assertTextEquals(s[Str.CANCEL]).performClick()
        compose.waitForTagGone(TestTags.FOLDER_DELETE_DIALOG)
        assertTrue(FakeServer.requests().none { it.getString("method") == "DELETE" })
        // 북마크 2개 폴더 → 확인 창에 개수 → 삭제 → 토스트 · 칩 사라짐 · 전체
        compose.onNodeWithTag(TestTags.folderChip(fa)).performClick()
        compose.onNodeWithTag(TestTags.FOLDER_DELETE).performClick()
        compose.waitForTag(TestTags.FOLDER_DELETE_DIALOG)
        compose.onNodeWithTag(TestTags.FOLDER_DELETE_NOTE).assertTextEquals(s[Str.FOLDER_DELETE_NOTE, 2])
        compose.onNodeWithTag(TestTags.FOLDER_DELETE_CONFIRM).assertTextEquals(s[Str.DELETE]).performClick()
        compose.waitForText(s[Str.TOAST_FOLDER_DELETED, "반도체·AI"])
        compose.waitForTagGone(TestTags.FOLDER_DELETE_DIALOG)
        compose.waitForTagGone(TestTags.folderChip(fa))
        compose.onNodeWithTag(TestTags.folderChip(BookmarkView.ALL)).assertTextEquals(s[Str.ALL], "0").assertIsSelected()
        compose.onNodeWithTag(TestTags.BM_SUB).assertTextEquals(s[Str.BM_SUB, 0])
        assertEquals(204, FakeServer.requests().single { it.getString("method") == "DELETE" }.getInt("status"))
        assertEquals(listOf(fb), appFolders().map { it.id })
        // 빈 화면 "이슈맵 보러 가기"
        compose.onNodeWithTag(TestTags.BM_EMPTY_ACTION).assertTextEquals(s[Str.BM_EMPTY_ACTION]).performClick()
        compose.waitForTag(TestTags.VIEW_MAP)
    }
}
