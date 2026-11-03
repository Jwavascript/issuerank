package io.github.jwavascript.issuerank.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 북마크 화면 계산(웹 renderBookmarks) · 입력 규칙(trim · maxlength) */
class BookmarksTest {
    private val s = XmlStrings
    private val bookmarks = ApiJson.bookmarks(TestFiles.fixture("bookmarks.json"))
    private val folders = ApiJson.folders(TestFiles.fixture("folders.json"))
    private val uuid = "f52bfc5e7-6572-463d-8695-ad6b48737215"

    @Test
    fun recentFirstThenFolderFilterThenDateGroups() {
        // savedAt: 11-03 00:12Z · 11-02 21:40+09:00(= 12:40Z) · 11-02 03:05Z — Z와 +09:00을 같은 시각 축으로
        assertEquals(listOf("2026-11-03-it-1", "2026-11-02-cul-5", "2026-11-03-pol-22"), BookmarkView.items(bookmarks, BookmarkView.ALL).map { it.id })
        assertEquals(listOf("2026-11-03-it-1", "2026-11-02-cul-5"), BookmarkView.items(bookmarks, uuid).map { it.id })
        assertEquals(listOf("2026-11-03-pol-22"), BookmarkView.items(bookmarks, "f1").map { it.id })
        assertEquals(emptyList<Bookmark>(), BookmarkView.items(bookmarks, "f3"))
        // 이어진 같은 날짜(카드 date)만 묶음: 11-03 · 11-02 · 11-03(다시 따로)
        assertEquals(listOf("2026-11-03" to 1, "2026-11-02" to 1, "2026-11-03" to 1),
            BookmarkView.groups(BookmarkView.items(bookmarks, BookmarkView.ALL)).map { it.date to it.items.size })
        val reversed = bookmarks.reversed()
        assertEquals(BookmarkView.items(bookmarks, BookmarkView.ALL), BookmarkView.items(reversed, BookmarkView.ALL))
        assertEquals("11월 3일 (화) 랭킹", s[Str.RANKING_OF_DATE, KstDate.fmtDate("2026-11-03", s)])
    }

    @Test
    fun sameSavedAtKeepsServerOrder() {
        val a = bookmarks[0].copy(savedAt = "2026-11-03T00:00:00.000Z")
        val b = bookmarks[1].copy(savedAt = "2026-11-03T09:00:00+09:00")
        assertEquals(listOf(a.id, b.id), BookmarkView.items(listOf(a, b), BookmarkView.ALL).map { it.id })
        assertEquals(listOf(b.id, a.id), BookmarkView.items(listOf(b, a), BookmarkView.ALL).map { it.id })
    }

    @Test
    fun chipsPickedFilterAndBadge() {
        assertEquals(listOf(FolderChip("all", null, 3), FolderChip("f1", "반도체·AI", 4), FolderChip(uuid, "부동산·금리", 2), FolderChip("f3", "나중에 읽기", 0)),
            BookmarkView.chips(bookmarks, folders))
        assertNull(BookmarkView.picked(BookmarkView.ALL, folders))
        assertEquals("f3", BookmarkView.picked("f3", folders)!!.id)
        assertEquals(BookmarkView.ALL, BookmarkView.effectiveFilter("f-gone", folders))
        assertEquals("f3", BookmarkView.effectiveFilter("f3", folders))
        assertEquals(3, BookmarkView.badge(true, bookmarks))
        assertNull(BookmarkView.badge(false, bookmarks))
        assertNull(BookmarkView.badge(true, emptyList()))
        assertEquals("부동산·금리", BookmarkView.folderName(folders, uuid))
        assertEquals("", BookmarkView.folderName(folders, "f-gone"))
        assertEquals(bookmarks[2], BookmarkView.find(bookmarks, "2026-11-03-pol-22"))
        assertEquals(120, BookmarkView.maxArticleCount(bookmarks))
        assertEquals(0, BookmarkView.maxArticleCount(emptyList()))
    }

    @Test
    fun folderDeleteDialogText() {
        assertEquals("‘반도체·AI’ 폴더를 삭제할까요?", s[Str.FOLDER_DELETE_TITLE, folders[0].name])
        assertEquals("폴더 안의 북마크 4개도 함께 삭제됩니다. 삭제하면 되돌릴 수 없습니다.", BookmarkView.deleteNote(folders[0], s))
        assertEquals("이 폴더는 비어 있습니다. 삭제하면 되돌릴 수 없습니다.", BookmarkView.deleteNote(folders[2], s))
        assertEquals("‘부동산·금리’ 폴더 삭제", s[Str.CD_FOLDER_DELETE, folders[1].name])
    }

    @Test
    fun inputsTrimLikeJs() {
        assertEquals("새 폴더", Inputs.folderName("  새 폴더\t"))
        // 보이지 않는 글자는 코드 값으로 만듦(소스에 BOM 등을 두지 않음)
        val ideographic = Char(0x3000).toString()
        val bom = Char(0xFEFF).toString()
        val nbsp = Char(0x00A0).toString()
        val lineSep = Char(0x2028).toString()
        val zeroWidth = Char(0x200B).toString()
        assertEquals("가", Inputs.folderName("$ideographic${bom}가$nbsp$lineSep")) // JS trim은 전각 공백 · BOM · NBSP · 줄 구분도 뺌
        assertEquals("${zeroWidth}가", Inputs.folderName("${zeroWidth}가 "))      // 폭 없는 공백은 JS도 안 뺌
        assertNull(Inputs.folderName(" $ideographic "))
        assertEquals(Str.AUTH_ERR_NAME, Inputs.authError(signUp = true, name = " $ideographic"))
        assertNull(Inputs.authError(signUp = false, name = ""))
        assertNull(Inputs.authError(signUp = true, name = "앱"))
        assertEquals("app.user@example.invalid", Inputs.email(" app.user@example.invalid\n"))
        assertEquals(listOf(16, 100, 128), listOf(Inputs.FOLDER_NAME_MAX, Inputs.NAME_MAX, Inputs.PASSWORD_MAX))
        assertEquals("이름을 입력해 주세요", s[Str.AUTH_ERR_NAME])
    }

    @Test
    fun maxLengthCutsLikeHtml_withoutSplittingASurrogatePair() {
        val sixteen = "가나다라마바사아자차카타파하가나"
        assertEquals(16, sixteen.length)
        assertEquals(sixteen, Inputs.limit(sixteen, Inputs.FOLDER_NAME_MAX)) // 16자 = 그대로
        assertEquals(sixteen, Inputs.limit(sixteen + "다", Inputs.FOLDER_NAME_MAX)) // 17번째 글자는 안 들어감
        assertEquals("", Inputs.limit("", 16))
        val emoji = String(Character.toChars(0x1F600)) // UTF-16 2글자
        assertEquals("a".repeat(15), Inputs.limit("a".repeat(15) + emoji, 16)) // 쌍을 쪼개지 않음(15에서 멈춤)
        assertEquals("a".repeat(14) + emoji, Inputs.limit("a".repeat(14) + emoji + "b", 16))
    }
}
