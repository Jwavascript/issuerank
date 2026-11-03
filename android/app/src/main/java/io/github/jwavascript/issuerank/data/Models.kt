package io.github.jwavascript.issuerank.data

/** 섹션 키 · 순서 = 웹 SECTIONS(index.html 572~579) · 서버 ranking.ts SECTIONS */
enum class SectionKey(val key: String) {
    POL("pol"), ECO("eco"), SOC("soc"), CUL("cul"), WOR("wor"), IT("it");

    /** 섹션 이름 문구 키(정치 · 경제 · 사회 · 생활·문화 · 세계 · IT·과학) */
    val label: Str
        get() = when (this) {
            POL -> Str.SECTION_POL
            ECO -> Str.SECTION_ECO
            SOC -> Str.SECTION_SOC
            CUL -> Str.SECTION_CUL
            WOR -> Str.SECTION_WOR
            IT -> Str.SECTION_IT
        }

    companion object {
        fun of(key: String?): SectionKey? = entries.firstOrNull { it.key == key }
    }
}

/** 웹 해시 화면 3개(index.html 1352 ORDER) */
enum class View(val hash: String) {
    MAP("map"), LIST("list"), BOOKMARKS("bookmarks");

    companion object {
        fun of(hash: String?): View? = entries.firstOrNull { it.hash == hash }
    }
}

/** 이슈 카드 8칸(openapi Issue). summary null = "요약 없음 · 원문을 확인해 주세요" */
data class Issue(
    val id: String,
    val date: String,
    val section: SectionKey,
    val rank: Int,
    val title: String,
    val summary: List<String>?,
    val articleCount: Int,
    val url: String,
)

data class RankingSection(val key: SectionKey, val name: String, val issues: List<Issue>)

/** GET /api/rankings 200. 섹션 · 이슈 순서는 서버 그대로(D16 — 앱은 다시 정렬하지 않음) */
data class Ranking(val date: String, val updatedAt: String, val sections: List<RankingSection>) {
    /** 웹 secIssues(k) */
    fun issuesOf(key: SectionKey): List<Issue> = sections.firstOrNull { it.key == key }?.issues ?: emptyList()

    /** 웹 issueById(id) */
    fun issueById(id: String): Issue? = sections.firstNotNullOfOrNull { s -> s.issues.firstOrNull { it.id == id } }

    /** 날짜 줄 "이슈 n개" */
    val issueCount: Int get() = sections.sumOf { it.issues.size }

    /** 웹 rankMax(): 받은 랭킹의 최대 연관기사 수(없으면 0) */
    val maxArticleCount: Int get() = maxOf(0, sections.flatMap { it.issues }.maxOfOrNull { it.articleCount } ?: 0)

    /** 웹 secTotal(k) */
    fun sectionTotal(key: SectionKey): Int = issuesOf(key).sumOf { it.articleCount }

    /** 대체 섹션(D8): 그날 행이 없어 직전 날짜 Top 5를 받은 카드 — 상세 '수집'은 날짜만 */
    fun isFallback(issue: Issue): Boolean = issue.date != date
}

/** GET /api/bookmarks 한 줄 = 이슈 카드 + 폴더 + 저장 시각(ISO 8601) */
data class Bookmark(val issue: Issue, val folderId: String, val savedAt: String) {
    val id: String get() = issue.id
}

data class Folder(val id: String, val name: String, val count: Int)

/** Better Auth 사용자(앱이 쓰는 칸만). 토큰 · 세션 칸은 읽지 않음 */
data class AuthUser(val id: String, val name: String, val email: String)

/** GET /api/widget 200 — 형식 검사는 widget.js valid 그대로(ApiJson.widget) */
data class Widget(
    val date: String,
    val updatedAt: String?,
    val updatedLabel: String,
    /** 문자열이 아니면 null(쓸 때는 WidgetRules.morePath) */
    val morePath: String?,
    val items: List<WidgetItem>,
)

/** 위젯 한 줄. 화면에는 title만, 누르면 path(앱 경로) */
data class WidgetItem(val title: String, val path: String, val id: String?, val section: SectionKey?)

/** 위젯 응답 + 그 원문(성공만 캐시 — 캐시에는 원문을 쓰고 읽을 때 다시 검사, widget.js load · cached) */
class WidgetPayload(val widget: Widget, val json: String) {
    override fun toString(): String = "WidgetPayload(date=${widget.date}, items=${widget.items.size})"
}
