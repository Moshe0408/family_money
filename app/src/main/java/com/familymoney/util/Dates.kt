package com.familymoney.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Dates {
    val zone: ZoneId = ZoneId.systemDefault()

    private val dayFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM")

    private val hebrewMonths = arrayOf(
        "ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
        "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"
    )

    fun today(): LocalDate = LocalDate.now(zone)

    fun nowMillis(): Long = System.currentTimeMillis()

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun monthStart(ym: YearMonth = YearMonth.now(zone)): Long = toMillis(ym.atDay(1))

    fun monthEnd(ym: YearMonth = YearMonth.now(zone)): Long =
        ym.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    fun yearMonthKey(millis: Long): String {
        val d = toLocalDate(millis)
        return "%04d-%02d".format(d.year, d.monthValue)
    }

    fun yearMonthKey(ym: YearMonth): String = "%04d-%02d".format(ym.year, ym.monthValue)

    fun hebrewMonth(ym: YearMonth): String = hebrewMonths[ym.monthValue - 1]

    fun hebrewMonthYear(ym: YearMonth): String = "${hebrewMonths[ym.monthValue - 1]} ${ym.year}"

    fun formatDay(millis: Long): String = toLocalDate(millis).format(dayFmt)

    fun formatShort(millis: Long): String = toLocalDate(millis).format(shortFmt)

    fun daysLeftInMonth(from: LocalDate = today()): Int =
        (from.lengthOfMonth() - from.dayOfMonth).coerceAtLeast(0)

    fun monthsBetween(fromMillis: Long, toMillis: Long): Int {
        val a = YearMonth.from(toLocalDate(fromMillis))
        val b = YearMonth.from(toLocalDate(toMillis))
        return ((b.year - a.year) * 12 + (b.monthValue - a.monthValue)).coerceAtLeast(0)
    }

    /** Next occurrence of [dayOfMonth] that has not passed yet, in millis. */
    fun nextOccurrence(dayOfMonth: Int, from: LocalDate = today()): Long {
        val safeDay = dayOfMonth.coerceIn(1, 28)
        val candidate = from.withDayOfMonth(minOf(safeDay, from.lengthOfMonth()))
        val target = if (candidate.isBefore(from)) {
            val next = from.plusMonths(1)
            next.withDayOfMonth(minOf(safeDay, next.lengthOfMonth()))
        } else candidate
        return toMillis(target)
    }

    fun isInCurrentMonth(millis: Long): Boolean {
        val d = toLocalDate(millis)
        val now = today()
        return d.year == now.year && d.monthValue == now.monthValue
    }
}
