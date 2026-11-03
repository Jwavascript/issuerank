package io.github.jwavascript.issuerank.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * 응답 본문 → 모델. 계약(openapi.yaml 0.8.0 · server/routes · ranking.ts · Better Auth)과 다르면 BadResponseException.
 * 계약에 없는 칸은 무시(사용자 image · session 등). 위젯만 widget.js valid 규칙(더 느슨함).
 */
object ApiJson {
    private val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val DATE_TIME = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*")

    /** YYYY-MM-DD + 달력에 있는 날짜(서버 z.iso.date()와 같음) */
    fun isIsoDate(s: String): Boolean =
        DATE.matches(s) && try { LocalDate.parse(s); true } catch (e: DateTimeParseException) { false }

    /** GET /api/rankings 200 */
    fun ranking(text: String): Ranking {
        val o = jsonObject(text)
        val date = o.str("date").also { if (!isIsoDate(it)) bad("date") }
        val updatedAt = o.str("updatedAt").also { if (!DATE_TIME.matches(it)) bad("updatedAt") }
        val sections = o.arr("sections")
        // 웹은 SECTIONS로 찾아 그리므로 모르는 섹션 키는 무시(index.html 583)
        val list = (0 until sections.length()).mapNotNull { i ->
            val s = sections.objAt(i)
            val key = SectionKey.of(s.str("key")) ?: return@mapNotNull null
            RankingSection(key, s.str("name"), s.arr("issues").mapObjects(::issue))
        }
        return Ranking(date, updatedAt, list)
    }

    /** openapi Issue(8칸) */
    fun issue(o: JSONObject): Issue {
        val summary = o.arrOrNull("summary")?.let { a -> List(a.length()) { a.strAt(it) } }
        if (summary == null && !o.isNull("summary")) bad("summary")
        return Issue(
            id = o.str("id"),
            date = o.str("date").also { if (!isIsoDate(it)) bad("date") },
            section = SectionKey.of(o.str("section")) ?: bad("section"),
            rank = o.int("rank"),
            title = o.str("title"),
            summary = summary,
            articleCount = o.int("articleCount").also { if (it < 0) bad("articleCount") },
            url = o.str("url"),
        )
    }

    /**
     * GET /api/widget 200 — widget.js valid(24~29줄): date · updatedLabel 문자열, items 1개 이상,
     * 항목마다 title 문자열 · path는 /로 시작하는 문자열. 계획 4절 "30개까지" = 앞 30개만 씀(31개 이상도 거절하지 않음 — widget.js와 같음)
     */
    fun widget(text: String): Widget {
        val o = jsonObject(text)
        val items = o.arr("items")
        if (items.length() == 0) bad("items")
        val list = List(items.length()) { i ->
            val it = items.objAt(i)
            val path = it.str("path")
            if (!path.startsWith("/")) bad("path")
            WidgetItem(it.str("title"), path, it.strOrNull("id"), SectionKey.of(it.strOrNull("section")))
        }
        return Widget(
            date = o.str("date"),
            updatedAt = o.strOrNull("updatedAt"),
            updatedLabel = o.str("updatedLabel"),
            morePath = o.strOrNull("morePath"),
            items = list.take(WidgetRules.MAX_ITEMS),
        )
    }

    /** sign-up/email · sign-in/email · sign-in/social 200: { user, token, … } — token은 읽지 않음 */
    fun authUser(text: String): AuthUser = user(jsonObject(text).obj("user"))

    /** get-session 200: null 또는 { session, user }. user가 없으면 로그아웃 상태(웹 data?.user || null) */
    fun session(text: String): AuthUser? = when (val v = jsonValue(text)) {
        JSONObject.NULL -> null
        is JSONObject -> v.objOrNull("user")?.let(::user)
        else -> bad("(root)")
    }

    private fun user(o: JSONObject) = AuthUser(o.str("id"), o.str("name"), o.str("email"))

    /** GET /api/bookmarks 200 — 카드 8칸 + folderId · savedAt */
    fun bookmarks(text: String): List<Bookmark> = jsonArray(text).mapObjects { o ->
        val savedAt = o.str("savedAt")
        if (savedAtMillis(savedAt) == null) bad("savedAt")
        Bookmark(issue(o), o.str("folderId"), savedAt)
    }

    /** GET /api/folders 200 */
    fun folders(text: String): List<Folder> = jsonArray(text).mapObjects(::folder)

    /** POST /api/folders 201 */
    fun folder(text: String): Folder = folder(jsonObject(text))

    private fun folder(o: JSONObject) = Folder(o.str("id"), o.str("name"), o.int("count"))

    /** 오류 본문: Hono { message } · Better Auth { code, message }. 빈 message는 없음으로(웹 data?.message || …) */
    fun errorBody(text: String): Pair<String?, String?> {
        val o = try { jsonValue(text) as? JSONObject } catch (e: RuntimeException) { null } ?: return null to null
        return o.strOrNull("code") to o.strOrNull("message")?.takeIf { it.isNotEmpty() }
    }

    /** 저장 시각(ISO 8601, Z 또는 +09:00) → epoch ms. 웹 Date.parse */
    fun savedAtMillis(savedAt: String): Long? =
        try { OffsetDateTime.parse(savedAt).toInstant().toEpochMilli() } catch (e: DateTimeParseException) { null }
}
