package com.omnitools.app.tools.everyday

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Whole-number conversion between binary, octal, decimal and hexadecimal, with no size limit (BigInteger). */
object NumberBase {
    const val MAX_DIGITS = 200

    /** Parses [text] in [base] (2, 8, 10 or 16). Accepts spaces, underscores, a leading minus and the 0b/0o/0x prefix. Null if invalid. */
    fun parse(text: String, base: Int): BigInteger? {
        var s = text.trim().replace(" ", "").replace("_", "")
        var negative = false
        if (s.startsWith("-")) { negative = true; s = s.substring(1) }
        val prefix = when (base) { 2 -> "0b"; 8 -> "0o"; 16 -> "0x"; else -> "" }
        if (prefix.isNotEmpty() && s.lowercase().startsWith(prefix)) s = s.substring(2)
        // A second sign ("--5", "+5") is not a number; BigInteger would accept it and flip the meaning.
        if (s.isEmpty() || s.length > MAX_DIGITS || s[0] == '+' || s[0] == '-') return null
        return try {
            val v = BigInteger(s, base)
            if (negative) v.negate() else v
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun format(value: BigInteger, base: Int): String = value.toString(base).uppercase()

    /** Groups digits for reading: binary and hex in fours, decimal in thousands. Sign is kept. */
    fun grouped(text: String, base: Int): String {
        val neg = text.startsWith("-")
        val digits = text.removePrefix("-")
        val size = if (base == 10) 3 else 4
        val out = digits.reversed().chunked(size).joinToString(" ").reversed()
        return if (neg) "-$out" else out
    }
}

/** Discount, sale-price and sales-tax arithmetic. */
object Discount {
    data class Result(val savings: Double, val afterDiscount: Double, val tax: Double, val total: Double)

    fun apply(price: Double, percentOff: Double, taxPercent: Double = 0.0): Result {
        require(price >= 0.0) { "Price can't be negative." }
        require(percentOff in 0.0..100.0) { "Discount must be between 0% and 100%." }
        require(taxPercent >= 0.0) { "Tax can't be negative." }
        val savings = price * percentOff / 100.0
        val after = price - savings
        val tax = after * taxPercent / 100.0
        return Result(savings, after, tax, after + tax)
    }

    /** What percent off [original] the [sale] price is (negative if the sale price is higher). */
    fun percentOff(original: Double, sale: Double): Double {
        require(original > 0.0) { "The original price must be more than 0." }
        require(sale >= 0.0) { "The sale price can't be negative." }
        return (original - sale) / original * 100.0
    }
}

data class CountdownItem(val id: String, val name: String, val epochDay: Long)

object Countdown {
    fun daysUntil(today: LocalDate, target: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    fun describe(days: Long): String = when {
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days == -1L -> "Yesterday"
        days > 1 -> "In $days days"
        else -> "${-days} days ago"
    }

    /** "2 months, 3 days" style distance between two dates, in calendar units. */
    fun breakdown(today: LocalDate, target: LocalDate): String {
        val (a, b) = if (target >= today) today to target else target to today
        val p = Period.between(a, b)
        val parts = buildList {
            if (p.years > 0) add("${p.years} year${if (p.years == 1) "" else "s"}")
            if (p.months > 0) add("${p.months} month${if (p.months == 1) "" else "s"}")
            if (p.days > 0) add("${p.days} day${if (p.days == 1) "" else "s"}")
        }
        return if (parts.isEmpty()) "0 days" else parts.joinToString(", ")
    }

    fun encode(items: List<CountdownItem>): String {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("n", it.name).put("d", it.epochDay)) }
        return arr.toString()
    }

    fun decode(raw: String?): List<CountdownItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                CountdownItem(o.getString("id"), o.getString("n"), o.getLong("d"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class Habit(val id: String, val name: String, val days: Set<Long>)

object Habits {
    /** Consecutive days ending today; if today isn't ticked yet, the streak ending yesterday still counts. */
    fun currentStreak(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun bestStreak(days: Set<Long>): Int {
        var best = 0
        var run = 0
        var prev: Long? = null
        for (d in days.sorted()) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    fun toggle(days: Set<Long>, day: Long): Set<Long> = if (day in days) days - day else days + day

    fun encode(habits: List<Habit>): String {
        val arr = JSONArray()
        habits.forEach { h ->
            arr.put(JSONObject().put("id", h.id).put("n", h.name).put("d", JSONArray(h.days.sorted())))
        }
        return arr.toString()
    }

    fun decode(raw: String?): List<Habit> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                val da = o.optJSONArray("d") ?: JSONArray()
                Habit(o.getString("id"), o.getString("n"), (0 until da.length()).map { i -> da.getLong(i) }.toSet())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
