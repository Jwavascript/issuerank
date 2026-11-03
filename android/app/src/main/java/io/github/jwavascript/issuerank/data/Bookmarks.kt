package io.github.jwavascript.issuerank.data

/** 북마크 화면 칩 하나: id = 폴더 id 또는 BookmarkView.ALL, name null = "전체" */
data class FolderChip(val id: String, val name: String?, val count: Int)

/** 북마크 화면 날짜 묶음 하나: "11월 2일 (월) 랭킹" + 개수 */
data class BookmarkGroup(val date: String, val items: List<Bookmark>)

/** 북마크 화면 계산(웹 renderBookmarks 850~892 · findBookmark · folderName 660~661) */
object BookmarkView {
    const val ALL = "all"

    fun find(bookmarks: List<Bookmark>, issueId: String): Bookmark? = bookmarks.firstOrNull { it.id == issueId }

    /** 661 folderName(id): 없으면 빈 글 */
    fun folderName(folders: List<Folder>, id: String): String = folders.firstOrNull { it.id == id }?.name ?: ""

    /** 862: 고른 폴더가 없어졌으면 전체로 */
    fun effectiveFilter(filter: String, folders: List<Folder>): String =
        if (filter != ALL && folders.none { it.id == filter }) ALL else filter

    /** 866: 전체(받은 북마크 수) + 폴더(만든 순 · 서버 count) */
    fun chips(bookmarks: List<Bookmark>, folders: List<Folder>): List<FolderChip> =
        listOf(FolderChip(ALL, null, bookmarks.size)) + folders.map { FolderChip(it.id, it.name, it.count) }

    /** 865: 폴더 칩(전체 말고)을 골랐으면 그 폴더 → 칩 줄 끝 "폴더 삭제" */
    fun picked(filter: String, folders: List<Folder>): Folder? = if (filter == ALL) null else folders.firstOrNull { it.id == filter }

    /** 875: 그 폴더(또는 전체)만, 최근 저장 순(같으면 받은 순서) */
    fun items(bookmarks: List<Bookmark>, filter: String): List<Bookmark> = bookmarks
        .filter { filter == ALL || it.folderId == filter }
        .sortedByDescending { ApiJson.savedAtMillis(it.savedAt) ?: Long.MIN_VALUE }

    /** 881~886: 순서를 지키며 이어진 같은 날짜(카드 date)만 한 묶음 */
    fun groups(items: List<Bookmark>): List<BookmarkGroup> {
        val out = ArrayList<BookmarkGroup>()
        for (b in items) {
            val last = out.lastOrNull()
            if (last != null && last.date == b.issue.date) out[out.size - 1] = last.copy(items = last.items + b)
            else out.add(BookmarkGroup(b.issue.date, listOf(b)))
        }
        return out
    }

    /** 852: 머리 북마크 숫자(로그인 + 1개 이상일 때만) */
    fun badge(loggedIn: Boolean, bookmarks: List<Bookmark>): Int? = if (loggedIn && bookmarks.isNotEmpty()) bookmarks.size else null

    /** 616 bmMax(): 북마크 카드 숫자 칸 세기의 기준 */
    fun maxArticleCount(bookmarks: List<Bookmark>): Int = maxOf(0, bookmarks.maxOfOrNull { it.issue.articleCount } ?: 0)

    /** 1040~1042 폴더 삭제 확인 창 글 */
    fun deleteNote(folder: Folder, s: Strings): String =
        if (folder.count > 0) s[Str.FOLDER_DELETE_NOTE, folder.count] else s[Str.FOLDER_DELETE_NOTE_EMPTY]
}

/** 입력 칸 규칙(웹 HTML maxlength · trim) */
object Inputs {
    /** 새 폴더 이름 maxlength(957) — HTML처럼 UTF-16 글자 수 */
    const val FOLDER_NAME_MAX = 16
    /** 이름 maxlength(551) */
    const val NAME_MAX = 100
    /** 비밀번호 maxlength(553) */
    const val PASSWORD_MAX = 128

    /** HTML maxlength: 넘는 입력은 max 글자(UTF-16)까지 — 붙여 넣기도 자름, 서로게이트 쌍은 쪼개지 않음 */
    fun limit(input: String, max: Int): String {
        if (input.length <= max) return input
        val end = if (Character.isHighSurrogate(input[max - 1])) max - 1 else max
        return input.substring(0, end)
    }

    /** 1560: 앞뒤 공백을 뺀 이름, 비면 null(요청 0) */
    fun folderName(input: String): String? = Web.jsTrim(input).ifEmpty { null }

    /** 1459~1463: 이메일은 앞뒤 공백 빼고 · 비밀번호는 그대로 · 회원가입인데 이름이 비면 "이름을 입력해 주세요"(요청 0) */
    fun authError(signUp: Boolean, name: String): Str? = if (signUp && Web.jsTrim(name).isEmpty()) Str.AUTH_ERR_NAME else null

    fun email(input: String): String = Web.jsTrim(input)

    fun name(input: String): String = Web.jsTrim(input)
}
