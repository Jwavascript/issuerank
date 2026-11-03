package io.github.jwavascript.issuerank.data

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** 날짜 규칙(KST = UTC+9 고정, 웹 · 서버 kstToday와 같은 계산). 날짜 글은 strings_web.xml 꼴로 */
object KstDate {
    val OFFSET: ZoneOffset = ZoneOffset.ofHours(9)

    /** index.html 603 TODAY = new Date(Date.now() + 9h).toISOString().slice(0, 10) */
    fun today(clock: Clock): String = clock.instant().atOffset(OFFSET).toLocalDate().toString()

    /** 606 addDays(s, n) */
    fun addDays(date: String, days: Long): String = LocalDate.parse(date).plusDays(days).toString()

    /** 712~716 setDate: 오늘 이후는 오늘로(미래 막기) */
    fun clampToToday(date: String, today: String): String = if (date > today) today else date

    /** 608 fmtDate → "11월 3일 (화)" */
    fun fmtDate(date: String, s: Strings): String {
        val d = LocalDate.parse(date)
        return s[Str.DATE_FULL, d.monthValue, d.dayOfMonth, weekday(d, s)]
    }

    private fun weekday(d: LocalDate, s: Strings): String = s[
        when (d.dayOfWeek.value % 7) { 0 -> Str.WEEKDAY_0; 1 -> Str.WEEKDAY_1; 2 -> Str.WEEKDAY_2; 3 -> Str.WEEKDAY_3
            4 -> Str.WEEKDAY_4; 5 -> Str.WEEKDAY_5; else -> Str.WEEKDAY_6 },
    ]

    /** 609 fmtShort → "11.3" */
    fun fmtShort(date: String, s: Strings): String {
        val d = LocalDate.parse(date)
        return s[Str.DATE_SHORT, d.monthValue, d.dayOfMonth]
    }

    /** 611 fmtUpdated: updatedAt(+09:00)의 글자 그대로 → "11.3 오전 4:31"(0시 = 오전 12시 · 12시 = 오후 12시) */
    fun fmtUpdated(updatedAt: String, s: Strings): String {
        val (day, time) = updatedAt.split('T')
        val hm = time.split(':')
        val h = hm[0].toInt()
        val m = hm[1].toInt()
        return s[Str.TIME_UPDATED, fmtShort(day, s), s[if (h < 12) Str.AM else Str.PM], if (h % 12 == 0) 12 else h % 12, m]
    }

    /** 662 timeAgo(savedAt) → "방금" · "n분 전" · "n시간 전" · "n일 전"(Math.round = 반올림 위로) */
    fun timeAgo(savedAt: String, nowMs: Long, s: Strings): String {
        val then = ApiJson.savedAtMillis(savedAt) ?: return ""
        val m = Math.round((nowMs - then) / 60e3)
        if (m < 1) return s[Str.AGO_NOW]
        if (m < 60) return s[Str.AGO_MINUTES, m]
        val h = Math.round(m / 60.0)
        if (h < 24) return s[Str.AGO_HOURS, h]
        return s[Str.AGO_DAYS, Math.round(h / 24.0)]
    }
}

/**
 * 이슈맵 · 랭킹이 같이 쓰는 날짜 줄(renderContext 698~711). date = 화면 날짜(state.date), today = KST 오늘,
 * ranking = 받은 랭킹(없는 날 · 실패면 null), view = 지금 화면.
 */
class DateLine(val date: String, val today: String, val view: View, val ranking: Ranking?) {
    val isToday: Boolean get() = date == today

    /** 다음 날짜 단추: 오늘이면 막음 */
    val nextEnabled: Boolean get() = !isToday

    /** "오늘" 단추: 오늘이 아닐 때만 */
    val todayVisible: Boolean get() = !isToday

    /** 달력 최대 = 오늘 */
    val maxDate: String get() = today

    val prevDate: String get() = KstDate.addDays(date, -1)
    val nextDate: String get() = KstDate.addDays(date, 1)

    /** "11월 3일 (화) · 오늘" / "11월 2일 (월)" */
    fun dateText(s: Strings): String = KstDate.fmtDate(date, s).let { if (isToday) s[Str.DATE_TEXT_TODAY, it] else it }

    /** "오늘 가장 많이 보도된 이슈" / "11월 2일 (월)의 주요 이슈" */
    fun title(s: Strings): String = if (isToday) s[Str.RANK_TITLE_TODAY] else s[Str.RANK_TITLE_PAST, KstDate.fmtDate(date, s)]

    fun desc(s: Strings): String = s[if (view == View.MAP) Str.RANK_DESC_MAP else Str.RANK_DESC_LIST]

    /** "11.3 오전 4:31 수집 · 이슈 30개", 랭킹 없으면 빈 글 */
    fun updated(s: Strings): String =
        ranking?.let { s[Str.UPDATED_LINE, KstDate.fmtUpdated(it.updatedAt, s), it.issueCount] } ?: ""

    companion object {
        /** 상세 '수집'(932): 랭킹 날짜의 카드 = 수집 시각, 대체 섹션 카드 = 그 날짜만 */
        fun collected(issue: Issue, ranking: Ranking, s: Strings): String =
            if (issue.date == ranking.date) KstDate.fmtUpdated(ranking.updatedAt, s) else KstDate.fmtShort(issue.date, s)
    }
}
