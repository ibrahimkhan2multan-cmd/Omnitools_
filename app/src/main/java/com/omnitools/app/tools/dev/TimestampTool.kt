package com.omnitools.app.tools.dev

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

/** Unix timestamp <-> date conversion. Pure java.time, unit tested. */
object TimestampTool {
    enum class Unit(val label: String) { SECONDS("seconds"), MILLIS("milliseconds") }

    private val display = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val patterns = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    )

    /** Values of 100 billion or more (year 5138 in seconds) are taken as milliseconds. */
    fun detectUnit(value: Long): Unit = if (abs(value) >= 100_000_000_000L) Unit.MILLIS else Unit.SECONDS

    fun toInstant(value: Long, unit: Unit = detectUnit(value)): Instant =
        if (unit == Unit.MILLIS) Instant.ofEpochMilli(value) else Instant.ofEpochSecond(value)

    fun format(instant: Instant, zone: ZoneId): String = display.format(instant.atZone(zone))

    /** Years 1 to 9999 only: beyond that the millisecond value no longer fits in a Long and conversions throw. */
    private fun checked(i: Instant): Instant {
        require(i.epochSecond in -62_135_596_800L..253_402_300_799L) { "Use a year from 0001 to 9999." }
        return i
    }

    fun parse(text: String, zone: ZoneId): Instant {
        val t = text.trim()
        require(t.isNotEmpty()) { "Enter a date like 2024-03-15 14:30:00." }
        for (p in patterns) {
            try {
                return checked(LocalDateTime.parse(t, p).atZone(zone).toInstant())
            } catch (e: DateTimeParseException) {
                // try the next format
            }
        }
        try {
            return checked(LocalDate.parse(t).atStartOfDay(zone).toInstant())
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Use the form 2024-03-15 14:30:00 (the time is optional).")
        }
    }
}
