package com.omnitools.app.tools.calc

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Date arithmetic with java.time (available from Android 8 / API 26, which is this app's minimum). */
object DateCalc {

    /** Parses "yyyy-MM-dd". Throws [IllegalArgumentException] with a readable message. */
    fun parse(text: String): LocalDate {
        val t = text.trim()
        require(t.isNotEmpty()) { "Enter a date as yyyy-MM-dd, for example 2026-11-02." }
        return try {
            LocalDate.parse(t)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("\"$t\" is not a valid date. Use yyyy-MM-dd, for example 2026-11-02.")
        }
    }

    data class Difference(val years: Int, val months: Int, val days: Int, val totalDays: Long, val weeks: Long, val remainderDays: Long)

    /** Difference between two dates. The order does not matter; the result is always positive. */
    fun difference(a: LocalDate, b: LocalDate): Difference {
        val from = if (a.isAfter(b)) b else a
        val to = if (a.isAfter(b)) a else b
        val p = Period.between(from, to)
        val total = ChronoUnit.DAYS.between(from, to)
        return Difference(p.years, p.months, p.days, total, total / 7, total % 7)
    }

    data class Age(val years: Int, val months: Int, val days: Int, val totalDays: Long, val daysToNextBirthday: Long)

    /** Age on [today] for someone born on [birth]. Feb 29 birthdays fall on Feb 28 / Mar 1 per java.time rules. */
    fun age(birth: LocalDate, today: LocalDate): Age {
        require(!birth.isAfter(today)) { "The birth date is in the future." }
        val p = Period.between(birth, today)
        var next = birth.withYear(today.year)
        if (!next.isAfter(today)) next = birth.withYear(today.year + 1)
        return Age(p.years, p.months, p.days, ChronoUnit.DAYS.between(birth, today), ChronoUnit.DAYS.between(today, next))
    }

    /** Adds (or subtracts, with negative numbers) years, months and days. */
    fun add(date: LocalDate, years: Long, months: Long, days: Long): LocalDate =
        date.plusYears(years).plusMonths(months).plusDays(days)
}
