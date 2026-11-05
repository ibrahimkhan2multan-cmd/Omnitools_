package com.omnitools.app

import com.omnitools.app.tools.everyday.Countdown
import com.omnitools.app.tools.everyday.CountdownItem
import com.omnitools.app.tools.everyday.Discount
import com.omnitools.app.tools.everyday.Habit
import com.omnitools.app.tools.everyday.Habits
import com.omnitools.app.tools.everyday.NumberBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

class EverydayTest {
    @Test fun baseParseAndFormat() {
        assertEquals(BigInteger.valueOf(255), NumberBase.parse("ff", 16))
        assertEquals(BigInteger.valueOf(255), NumberBase.parse("0xFF", 16))
        assertEquals(BigInteger.valueOf(-5), NumberBase.parse("-101", 2))
        assertEquals(BigInteger.valueOf(10), NumberBase.parse("0b1010", 2))
        assertNull(NumberBase.parse("12", 2))
        assertNull(NumberBase.parse("", 10))
        assertNull(NumberBase.parse("abc", 10))
        assertEquals("11111111", NumberBase.format(BigInteger.valueOf(255), 2))
        assertEquals("FF", NumberBase.format(BigInteger.valueOf(255), 16))
        assertEquals("1111 1111", NumberBase.grouped("11111111", 2))
        assertEquals("1 234 567", NumberBase.grouped("1234567", 10))
        assertEquals("-FF", NumberBase.grouped("-FF", 16))
    }

    @Test fun bigNumbersWork() {
        val big = "1" + "0".repeat(60)
        assertEquals(big, NumberBase.format(NumberBase.parse(big, 10)!!, 10))
        assertNull(NumberBase.parse("1".repeat(NumberBase.MAX_DIGITS + 1), 10))
    }

    @Test fun discountMath() {
        val r = Discount.apply(200.0, 25.0, 10.0)
        assertEquals(50.0, r.savings, 1e-9)
        assertEquals(150.0, r.afterDiscount, 1e-9)
        assertEquals(15.0, r.tax, 1e-9)
        assertEquals(165.0, r.total, 1e-9)
        assertEquals(25.0, Discount.percentOff(200.0, 150.0), 1e-9)
        assertEquals(-50.0, Discount.percentOff(100.0, 150.0), 1e-9)
        assertTrue(runCatching { Discount.apply(10.0, 101.0) }.isFailure)
        assertTrue(runCatching { Discount.percentOff(0.0, 5.0) }.isFailure)
    }

    @Test fun countdownText() {
        val today = LocalDate.of(2026, 11, 4)
        assertEquals(0L, Countdown.daysUntil(today, today))
        assertEquals(30L, Countdown.daysUntil(today, LocalDate.of(2026, 12, 4)))
        assertEquals("Today", Countdown.describe(0))
        assertEquals("Tomorrow", Countdown.describe(1))
        assertEquals("In 5 days", Countdown.describe(5))
        assertEquals("Yesterday", Countdown.describe(-1))
        assertEquals("3 days ago", Countdown.describe(-3))
        assertEquals("1 month, 3 days", Countdown.breakdown(today, LocalDate.of(2026, 12, 7)))
        assertEquals("1 year", Countdown.breakdown(today, LocalDate.of(2027, 11, 4)))
        assertEquals("0 days", Countdown.breakdown(today, today))
    }

    @Test fun countdownJsonRoundTrip() {
        val items = listOf(CountdownItem("a", "Trip \"x\"", 20000), CountdownItem("b", "Ünï", 20100))
        assertEquals(items, Countdown.decode(Countdown.encode(items)))
        assertTrue(Countdown.decode("not json").isEmpty())
        assertTrue(Countdown.decode(null).isEmpty())
    }

    @Test fun habitStreaks() {
        val days = setOf(10L, 11, 12, 20, 21)
        assertEquals(2, Habits.currentStreak(days, 21))
        assertEquals(2, Habits.currentStreak(days, 22)) // today not ticked yet, yesterday's streak still counts
        assertEquals(0, Habits.currentStreak(days, 24))
        assertEquals(3, Habits.bestStreak(days))
        assertEquals(0, Habits.bestStreak(emptySet()))
        assertEquals(setOf(1L, 2L), Habits.toggle(setOf(1L), 2))
        assertEquals(emptySet<Long>(), Habits.toggle(setOf(1L), 1))
    }

    @Test fun habitJsonRoundTrip() {
        val habits = listOf(Habit("h", "Read", setOf(1L, 5L, 6L)), Habit("g", "Walk", emptySet()))
        assertEquals(habits, Habits.decode(Habits.encode(habits)))
        assertTrue(Habits.decode("{").isEmpty())
    }
}
