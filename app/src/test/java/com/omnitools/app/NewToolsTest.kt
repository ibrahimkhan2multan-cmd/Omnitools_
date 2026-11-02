package com.omnitools.app

import com.omnitools.app.tools.calc.DateCalc
import com.omnitools.app.tools.calc.PercentCalc
import com.omnitools.app.tools.color.ColorTools
import com.omnitools.app.tools.random.RandomTools
import com.omnitools.app.tools.regex.RegexTester
import com.omnitools.app.tools.tasks.Task
import com.omnitools.app.tools.tasks.TasksRepository
import com.omnitools.app.util.BitmapLoader
import com.omnitools.app.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Random

class NewToolsTest {

    // ---- Percentages ----
    @Test fun percentages() {
        assertEquals(30.0, PercentCalc.percentOf(20.0, 150.0), 1e-9)
        assertEquals(12.5, PercentCalc.whatPercent(25.0, 200.0), 1e-9)
        assertEquals(50.0, PercentCalc.change(50.0, 75.0), 1e-9)
        assertEquals(-25.0, PercentCalc.change(80.0, 60.0), 1e-9)
        assertThrows(IllegalArgumentException::class.java) { PercentCalc.change(0.0, 5.0) }
        assertThrows(IllegalArgumentException::class.java) { PercentCalc.whatPercent(1.0, 0.0) }
    }

    @Test fun discountThenTax() {
        val d = PercentCalc.discount(200.0, 25.0, 10.0)
        assertEquals(50.0, d.saved, 1e-9)
        assertEquals(150.0, d.afterDiscount, 1e-9)
        assertEquals(15.0, d.tax, 1e-9)
        assertEquals(165.0, d.total, 1e-9)
        assertThrows(IllegalArgumentException::class.java) { PercentCalc.discount(100.0, 101.0) }
        assertThrows(IllegalArgumentException::class.java) { PercentCalc.discount(-1.0, 10.0) }
    }

    // ---- Dates ----
    @Test fun dateDifference() {
        val a = LocalDate.of(2024, 1, 1)
        val b = LocalDate.of(2024, 3, 1)
        val d = DateCalc.difference(a, b)
        assertEquals(60L, d.totalDays) // 2024 is a leap year: 31 + 29
        assertEquals(2, d.months)
        assertEquals(8L, d.weeks)
        assertEquals(4L, d.remainderDays)
        assertEquals(d, DateCalc.difference(b, a)) // order does not matter
    }

    @Test fun ageAndBirthday() {
        val age = DateCalc.age(LocalDate.of(1990, 5, 15), LocalDate.of(2026, 11, 2))
        assertEquals(36, age.years)
        assertEquals(5, age.months)
        assertEquals(18, age.days)
        assertEquals(194L, age.daysToNextBirthday)
        assertThrows(IllegalArgumentException::class.java) { DateCalc.age(LocalDate.of(2030, 1, 1), LocalDate.of(2026, 11, 2)) }
    }

    @Test fun addToDateAndParse() {
        assertEquals(LocalDate.of(2024, 2, 29), DateCalc.add(LocalDate.of(2024, 1, 31), 0, 1, 0))
        assertEquals(LocalDate.of(2027, 10, 31), DateCalc.add(LocalDate.of(2026, 11, 2), 1, 0, -2))
        assertEquals(LocalDate.of(2026, 11, 2), DateCalc.parse(" 2026-11-02 "))
        assertThrows(IllegalArgumentException::class.java) { DateCalc.parse("2026-02-30") }
        assertThrows(IllegalArgumentException::class.java) { DateCalc.parse("hello") }
        assertThrows(IllegalArgumentException::class.java) { DateCalc.parse("") }
    }

    // ---- Colors ----
    @Test fun hexParsingAndFormatting() {
        assertEquals(ColorTools.Rgb(79, 140, 255), ColorTools.parseHex("#4F8CFF"))
        assertEquals(ColorTools.Rgb(170, 187, 204), ColorTools.parseHex("abc"))
        assertNull(ColorTools.parseHex("zzz"))
        assertNull(ColorTools.parseHex("12345"))
        assertEquals("#000000", ColorTools.toHex(ColorTools.Rgb(0, 0, 0)))
        assertEquals("#4F8CFF", ColorTools.toHex(ColorTools.Rgb(79, 140, 255)))
    }

    @Test fun hslConversion() {
        val red = ColorTools.toHsl(ColorTools.Rgb(255, 0, 0))
        assertEquals(0.0, red.h, 1e-9); assertEquals(100.0, red.s, 1e-9); assertEquals(50.0, red.l, 1e-9)
        assertEquals(120.0, ColorTools.toHsl(ColorTools.Rgb(0, 255, 0)).h, 1e-9)
        assertEquals(240.0, ColorTools.toHsl(ColorTools.Rgb(0, 0, 255)).h, 1e-9)
        assertEquals(0.0, ColorTools.toHsl(ColorTools.Rgb(128, 128, 128)).s, 1e-9)
        assertEquals(ColorTools.Rgb(0, 255, 0), ColorTools.fromHsl(120.0, 100.0, 50.0))
        val c = ColorTools.Rgb(79, 140, 255)
        val h = ColorTools.toHsl(c)
        assertEquals(c, ColorTools.fromHsl(h.h, h.s, h.l))
    }

    @Test fun contrastRatio() {
        assertEquals(21.0, ColorTools.contrast(ColorTools.Rgb(0, 0, 0), ColorTools.Rgb(255, 255, 255)), 0.01)
        assertEquals(1.0, ColorTools.contrast(ColorTools.Rgb(9, 9, 9), ColorTools.Rgb(9, 9, 9)), 1e-9)
    }

    // ---- Regex ----
    @Test fun regexMatchesAndGroups() {
        val r = RegexTester.run("a+", "caaat aa") as RegexTester.Result.Ok
        assertEquals(2, r.matches.size)
        assertEquals(1, r.matches[0].start); assertEquals(4, r.matches[0].end)
        assertEquals(6, r.matches[1].start)
        val g = RegexTester.run("(\\d+)-(\\d+)", "10-20") as RegexTester.Result.Ok
        assertEquals(listOf<String?>("10", "20"), g.matches.single().groups)
    }

    @Test fun regexFlagsAndEmptyMatches() {
        assertEquals(1, (RegexTester.run("HELLO", "say hello", ignoreCase = true) as RegexTester.Result.Ok).matches.size)
        assertEquals(0, (RegexTester.run("HELLO", "say hello") as RegexTester.Result.Ok).matches.size)
        // Empty matches must terminate: "a*" on "baa" matches "", "aa", "".
        assertEquals(3, (RegexTester.run("a*", "baa") as RegexTester.Result.Ok).matches.size)
    }

    @Test fun regexErrorsAndLimits() {
        assertTrue(RegexTester.run("(", "x") is RegexTester.Result.Error)
        assertTrue(RegexTester.run("", "x") is RegexTester.Result.Error)
        assertTrue(RegexTester.run("a", "x".repeat(RegexTester.MAX_INPUT + 1)) is RegexTester.Result.Error)
        // Newer JDKs optimise the classic "(a+)+$" pattern, so a real slow pattern is not reliable in a unit test.
        // Instead use a deadline that has already passed: the very first character read must abort the match.
        val stopped = RegexTester.run("a+b", "a".repeat(5000), timeoutMs = -1)
        assertTrue("time limit must stop the match", stopped is RegexTester.Result.Error && stopped.message.startsWith("Stopped"))
    }

    // ---- Random ----
    @Test fun diceAndNumbers() {
        val roll = RandomTools.rollDice(5, 6, Random(42))
        assertEquals(5, roll.rolls.size)
        assertTrue(roll.rolls.all { it in 1..6 })
        assertEquals(roll.rolls.sum(), roll.total)
        assertEquals(listOf(5L, 5L, 5L), RandomTools.numbers(5, 5, 3, Random(1)))
        val nums = RandomTools.numbers(-10, 10, 100, Random(7))
        assertTrue(nums.all { it in -10..10 })
        assertThrows(IllegalArgumentException::class.java) { RandomTools.rollDice(0, 6) }
        assertThrows(IllegalArgumentException::class.java) { RandomTools.rollDice(1, 1) }
        assertThrows(IllegalArgumentException::class.java) { RandomTools.numbers(10, 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { RandomTools.pick(emptyList()) }
        assertEquals("a", RandomTools.pick(listOf("a")))
    }

    // ---- Time formatting ----
    @Test fun timeFormatting() {
        assertEquals("01:23.45", TimeFormat.stopwatch(83_450))
        assertEquals("1:02:03.45", TimeFormat.stopwatch(3_723_450))
        assertEquals("00:00.00", TimeFormat.stopwatch(-5))
        assertEquals("00:01", TimeFormat.countdown(1))
        assertEquals("00:00", TimeFormat.countdown(0))
        assertEquals("01:01", TimeFormat.countdown(61_000))
        assertEquals("1:00:00", TimeFormat.countdown(3_600_000))
    }

    // ---- Tasks storage format ----
    @Test fun tasksRoundTrip() {
        val tasks = listOf(Task("1", "Buy milk", false, 100L), Task("2", "Call \"Sam\" ✓", true, 200L))
        assertEquals(tasks, TasksRepository.decode(TasksRepository.encode(tasks)))
        assertTrue(TasksRepository.decode("not json").isEmpty())
        assertTrue(TasksRepository.decode(null).isEmpty())
    }

    // ---- Image decoding maths ----
    @Test fun bitmapSampleSize() {
        assertEquals(2, BitmapLoader.sampleSize(4000, 3000, 2400))
        assertEquals(1, BitmapLoader.sampleSize(1000, 500, 2400))
        assertEquals(8, BitmapLoader.sampleSize(10000, 10, 2400))
    }
}
