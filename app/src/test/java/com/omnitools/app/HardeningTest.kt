package com.omnitools.app

import com.omnitools.app.data.Prefs
import com.omnitools.app.data.ScanEntry
import com.omnitools.app.tools.calc.BmiCalc
import com.omnitools.app.tools.calc.ExpressionEvaluator
import com.omnitools.app.tools.calc.LoanCalc
import com.omnitools.app.tools.calc.NumberWords
import com.omnitools.app.tools.calc.RomanNumerals
import com.omnitools.app.tools.calc.TipCalc
import com.omnitools.app.tools.calc.UnitConverter
import com.omnitools.app.tools.files.Duplicates
import com.omnitools.app.tools.files.FileEntry
import com.omnitools.app.tools.random.RandomTools
import com.omnitools.app.tools.regex.RegexTester
import com.omnitools.app.tools.zip.ZipSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Edge cases and abuse inputs found during the reliability audit. */
class HardeningTest {
    private fun eval(s: String, deg: Boolean = true) = ExpressionEvaluator.evaluate(s, deg)
    private fun expectEvalError(s: String) {
        try { eval(s); fail("expected an error for: ${s.take(40)}") } catch (e: ExpressionEvaluator.EvalException) { /* expected */ }
    }

    // ---- calculator
    @Test fun deepNestingIsAnErrorNotACrash() {
        expectEvalError("(".repeat(500) + "1" + ")".repeat(500))
        expectEvalError("-".repeat(900) + "1")
        expectEvalError("sin(".repeat(300) + "1" + ")".repeat(300))
    }

    @Test fun veryLongInputIsRejected() = expectEvalError("1+".repeat(600) + "1")

    @Test fun reasonableNestingStillWorks() {
        assertEquals(1.0, eval("(".repeat(50) + "1" + ")".repeat(50)), 0.0)
        assertEquals(-1.0, eval("-".repeat(41) + "1"), 0.0)
    }

    @Test fun trigResultsSnapToZero() {
        assertEquals("0", ExpressionEvaluator.format(eval("sin(180)")))
        assertEquals("0", ExpressionEvaluator.format(eval("cos(90)")))
        assertEquals("0", ExpressionEvaluator.format(eval("tan(180)")))
        assertEquals(1.0, eval("tan(45)"), 1e-12)
        assertEquals(0.5, eval("sin(30)"), 1e-12)
    }

    @Test fun formatNeverThrowsOnSpecialValues() {
        assertEquals("Too large", ExpressionEvaluator.format(Double.POSITIVE_INFINITY))
        assertEquals("Too small", ExpressionEvaluator.format(Double.NEGATIVE_INFINITY))
        assertEquals("Not a number", ExpressionEvaluator.format(Double.NaN))
        assertEquals("0.1", ExpressionEvaluator.format(0.1 + 0.2 - 0.2))
    }

    @Test fun invalidExpressionsGiveErrors() {
        for (s in listOf("1/0", "0/0", "2+", "*3", "(1", "1)", "1..2", "sqrt(-1)", "ln(0)", "asin(2)", "5!!!!!", "(-1)!", "1.5!", "abc", "2^^3", "")) {
            try { eval(s); if (s.isNotEmpty()) fail("expected error: $s") } catch (e: ExpressionEvaluator.EvalException) { /* expected */ }
        }
        expectEvalError("10^400")
        expectEvalError("171!")
    }

    // ---- converters and finance
    @Test fun knotIsExact() {
        val kn = UnitConverter.unitsFor(UnitConverter.Kind.SPEED).first { it.symbol == "kn" }
        val kmh = UnitConverter.unitsFor(UnitConverter.Kind.SPEED).first { it.symbol == "km/h" }
        assertEquals(1.852, UnitConverter.convert(UnitConverter.Kind.SPEED, 1.0, kn, kmh), 1e-12)
    }

    @Test fun temperatureRoundTripAndLimits() {
        assertEquals(100.0, UnitConverter.convertTemperature(212.0, "°F", "°C"), 1e-9)
        assertEquals(-40.0, UnitConverter.convertTemperature(-40.0, "°C", "°F"), 1e-9)
        assertEquals(0.0, UnitConverter.convertTemperature(0.0, "K", "K") , 0.0)
        try { UnitConverter.convertTemperature(-300.0, "°C", "K"); fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun loanHandlesTinyRatesAndLimits() {
        val tiny = LoanCalc.emi(1200.0, 1e-12, 12)
        assertTrue(tiny.monthlyPayment.isFinite()); assertEquals(100.0, tiny.monthlyPayment, 1e-6)
        assertEquals(0.0, LoanCalc.emi(1200.0, 0.0, 12).totalInterest, 1e-9)
        val normal = LoanCalc.emi(100_000.0, 6.0, 360)
        assertEquals(599.55, normal.monthlyPayment, 0.01) // well-known 30-year, 6% figure
        for (bad in listOf({ LoanCalc.emi(0.0, 5.0, 12) }, { LoanCalc.emi(1e15, 5.0, 12) }, { LoanCalc.emi(10.0, -1.0, 12) }, { LoanCalc.emi(10.0, 5.0, 0) }, { LoanCalc.emi(10.0, 5.0, 1201) })) {
            try { bad(); fail() } catch (e: IllegalArgumentException) { }
        }
        val rows = LoanCalc.schedule(10_000.0, 5.0, 24)
        assertEquals(0.0, rows.last().balance, 1e-6)
    }

    @Test fun smallCalculatorsRejectBadInput() {
        try { BmiCalc.bmi(0.0, 1.7); fail() } catch (e: IllegalArgumentException) { }
        try { TipCalc.split(100.0, 10.0, 0, false); fail() } catch (e: IllegalArgumentException) { }
        assertEquals("MMMCMXCIX", RomanNumerals.toRoman(3999))
        for (s in listOf("IIII", "VX", "IC", "", "ABC")) try { RomanNumerals.fromRoman(s); fail(s) } catch (e: IllegalArgumentException) { }
        assertEquals("minus zero point five", NumberWords.fromString("-0.5").let { if (it.startsWith("minus")) it else it })
        try { NumberWords.fromString("1".repeat(16)); fail() } catch (e: IllegalArgumentException) { }
        try { NumberWords.fromString("1.2.3"); fail() } catch (e: IllegalArgumentException) { }
    }

    // ---- random and regex
    @Test fun randomLimits() {
        try { RandomTools.numbers(0, 10, 101); fail() } catch (e: IllegalArgumentException) { }
        try { RandomTools.numbers(Long.MIN_VALUE, Long.MAX_VALUE, 1); fail() } catch (e: IllegalArgumentException) { }
        val r = RandomTools.numbers(-5, 5, 100)
        assertTrue(r.all { it in -5..5 })
        assertEquals(7L, RandomTools.numbers(7, 7, 1)[0])
    }

    @Test fun regexTimeoutStopsMatching() {
        // Newer JVMs cache away most exponential patterns, so test the deadline itself: an expired deadline must stop at once.
        val r = RegexTester.run("a+b", "a".repeat(5000), timeoutMs = 0)
        assertTrue(r is RegexTester.Result.Error)
        assertTrue(RegexTester.run("a".repeat(501), "a") is RegexTester.Result.Error)
        assertTrue(RegexTester.run("a", "a".repeat(RegexTester.MAX_INPUT + 1)) is RegexTester.Result.Error)
        assertTrue(RegexTester.run("(", "a") is RegexTester.Result.Error)
    }

    @Test fun regexEmptyMatchesTerminate() {
        val r = RegexTester.run("a*", "baaac") as RegexTester.Result.Ok
        assertTrue(r.matches.size in 3..6)
    }

    // ---- files and settings
    @Test fun zipPathSafety() {
        for (bad in listOf("../x", "a/../../x", "/etc/passwd", "C:\\x", "a\\..\\..\\x", "", "a\u0000b", "..", "a/" + "b".repeat(300))) {
            assertEquals(bad, null, ZipSafety.safePath(bad))
        }
        assertEquals(listOf("a", "b.txt"), ZipSafety.safePath("a//./b.txt"))
        assertEquals(listOf("dir", "f"), ZipSafety.safePath("dir\\f"))
    }

    @Test fun smallFilesAreComparedInFullEvenIfSizeIsWrong() {
        // Two "10-byte" files whose real contents differ after the first bytes: hashing must use the whole content.
        val a = FileEntry("a", "a", 10, "a"); val b = FileEntry("b", "b", 10, "b")
        val content = mapOf("a" to "0123456789-A", "b" to "0123456789-B")
        val seenLimits = HashSet<Long?>()
        val groups = Duplicates.find(listOf(a, b), hash = { e, limit -> seenLimits.add(limit); content.getValue(e.id).let { c -> if (limit == null) c else c.take(limit.toInt()) } })
        assertTrue(groups.isEmpty())
        assertEquals(setOf<Long?>(null), seenLimits)
    }

    @Test fun badScanEntryDoesNotWipeHistory() {
        val raw = """[{"t":1,"f":"QR","x":"a"},{"t":"bad"},{"f":"QR"},{"t":2,"f":"EAN","x":"b"}]"""
        val list = Prefs.decodeScans(raw)
        assertEquals(listOf(ScanEntry(1, "QR", "a"), ScanEntry(2, "EAN", "b")), list)
        assertEquals(emptyList<ScanEntry>(), Prefs.decodeScans("not json"))
        assertEquals(emptyList<ScanEntry>(), Prefs.decodeScans(null))
        assertEquals(list, Prefs.decodeScans(Prefs.encodeScans(list)))
    }

    @Test fun recentsAreCappedAndDeduplicated() {
        var list = emptyList<String>()
        for (i in 1..20) list = Prefs.addRecent(list, "t${i % 10}")
        assertEquals(Prefs.MAX_RECENTS, list.size)
        assertEquals(list.size, list.toSet().size)
        assertEquals(listOf("x"), Prefs.decodeRecents(Prefs.encodeRecents(listOf("x"))))
    }
}
