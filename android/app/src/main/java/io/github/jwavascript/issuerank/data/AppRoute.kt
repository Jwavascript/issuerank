package io.github.jwavascript.issuerank.data

/** 위젯 줄의 이슈 바로가기: id + 그 id가 가리키는 날짜(형식이 틀리면 null → 최근 랭킹을 받고 "이슈를 찾을 수 없습니다") */
data class IssueLink(val id: String, val date: String?)

/** 앱 경로 → 첫 화면 + (있으면) 열 이슈 */
data class AppRoute(val view: View, val issue: IssueLink?)

sealed interface ShortcutOutcome {
    /** 그 날짜 랭킹에 있음 → 상세 열기 */
    data class Open(val issue: Issue) : ShortcutOutcome
    /** 서버가 답했는데(200 · 404) 없음 → 토스트 "이슈를 찾을 수 없습니다" */
    data object NotFound : ShortcutOutcome
    /** 5xx · 네트워크: 랭킹 오류 토스트만 그대로(D25) */
    data object None : ShortcutOutcome
}

/**
 * 앱 경로(위젯 → MainActivity Intent의 문자열 — COMMON 5): `/?issue=<id>` · `/#list` · `/`.
 * 경로 → 화면 = 웹 start()의 issueShortcut(1583~1594) + viewFromHash(1382) + 상세 열기(1614~1617).
 */
object AppRoutes {
    const val HOME = "/"
    const val LIST = "/#list"

    private val ISSUE_ID = Regex("(\\d{4}-\\d{2}-\\d{2})-([a-z]+)-\\d+")

    /** 위젯 줄 path와 같은 꼴(서버 buildWidget: /?issue={encodeURIComponent(id)}) */
    fun issuePath(id: String): String = "/?issue=" + Web.encodeURIComponent(id)

    /** 경로가 없거나 /로 시작하지 않으면 첫 화면(이슈맵). 해시 = 화면(map · list · bookmarks, 그 밖 = 이슈맵), 쿼리 issue = 바로가기 */
    fun parse(path: String?, today: String): AppRoute {
        if (path == null || !path.startsWith("/")) return AppRoute(View.MAP, null)
        val hashAt = path.indexOf('#')
        val beforeHash = if (hashAt < 0) path else path.substring(0, hashAt)
        val view = View.of(if (hashAt < 0) "" else path.substring(hashAt + 1)) ?: View.MAP
        val q = beforeHash.indexOf('?')
        val id = if (q < 0) null else Web.queryParams(beforeHash.substring(q + 1)).firstOrNull { it.first == "issue" }?.second
        return AppRoute(view, id?.let { IssueLink(it, issueDate(it, today)) })
    }

    /**
     * 1590~1591: {YYYY-MM-DD}-{소문자}-{숫자}이고, 날짜가 달력에 있고(웹 iso(parse(d)) === d — JS Date라 1000년 앞은 틀림),
     * KST 오늘 이하이고, 섹션 키를 알면 그 날짜. 아니면 null
     */
    fun issueDate(id: String, today: String): String? {
        val m = ISSUE_ID.matchEntire(id) ?: return null
        val date = m.groupValues[1]
        val ok = ApiJson.isIsoDate(date) && date[0] != '0' && date <= today && SectionKey.of(m.groupValues[2]) != null
        return if (ok) date else null
    }

    /** 랭킹(loadRanking(link.date)) 뒤 */
    fun outcome(link: IssueLink, load: RankingLoad): ShortcutOutcome {
        val found = if (link.date != null && load is RankingLoad.Loaded) load.ranking.issueById(link.id) else null
        return when {
            found != null -> ShortcutOutcome.Open(found)
            load.answered -> ShortcutOutcome.NotFound
            else -> ShortcutOutcome.None
        }
    }

    val notFoundToast: Toast = Toast(Msg.Res(Str.TOAST_ISSUE_NOT_FOUND))
}
