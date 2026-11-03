package io.github.jwavascript.issuerank.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 달력(Material3 DatePicker는 날짜를 UTC 자정 밀리초로 다룸) ↔ "YYYY-MM-DD"(화면 날짜). 순수 함수 — JVM 시험.
 * 고를 수 있음 = KST 오늘 이하(웹 dateInput.max = TODAY — AC-09-4). 앞쪽 한계는 웹처럼 없음(없는 날 = 404 안내).
 */
object PickerDates {
    fun toMillis(date: String): Long = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromMillis(utcMillis: Long): String = Instant.ofEpochMilli(utcMillis).atOffset(ZoneOffset.UTC).toLocalDate().toString()

    fun selectable(utcMillis: Long, today: String): Boolean = fromMillis(utcMillis) <= today

    fun selectableYear(year: Int, today: String): Boolean = year <= LocalDate.parse(today).year
}
