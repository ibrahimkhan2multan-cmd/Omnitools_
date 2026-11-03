package com.omnitools.app

import com.omnitools.app.tools.calc.BmiCalc
import com.omnitools.app.tools.calc.LoanCalc
import com.omnitools.app.tools.calc.NumberWords
import com.omnitools.app.tools.calc.RomanNumerals
import com.omnitools.app.tools.calc.TipCalc
import com.omnitools.app.tools.dev.TimestampTool
import com.omnitools.app.tools.qr.BarcodeEncoder
import com.omnitools.app.tools.text.Lorem
import com.omnitools.app.tools.text.Morse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Random

class MoreToolsTest {

    // ---- loan ----
    @Test fun loanKnownPayment() {
        // 100,000 at 6% for 30 years: the standard textbook payment is 599.55
        val r = LoanCalc.emi(100_000.0, 6.0, 360)
        assertEquals(599.55, r.monthlyPayment, 0.01)
        assertEquals(r.totalPayment - 100_000.0, r.totalInterest, 1e-6)
    }

    @Test fun loanZeroRateAndSchedule() {
        assertEquals(100.0, LoanCalc.emi(1200.0, 0.0, 12).monthlyPayment, 1e-9)
        val rows = LoanCalc.schedule(10_000.0, 8.0, 24)
        assertEquals(24, rows.size)
        assertEquals(0.0, rows.last().balance, 1e-6)
        assertEquals(10_000.0, rows.sumOf { it.principalPaid }, 1e-6)
    }

    @Test fun loanRejectsBadInput() {
        for (bad in listOf({ LoanCalc.emi(0.0, 5.0, 12) }, { LoanCalc.emi(1000.0, -1.0, 12) }, { LoanCalc.emi(1000.0, 5.0, 0) })) {
            try { bad(); fail("should throw") } catch (e: IllegalArgumentException) { assertNotNull(e.message) }
        }
    }

    // ---- bmi / tip ----
    @Test fun bmiValuesAndCategories() {
        assertEquals(22.86, BmiCalc.bmi(70.0, 1.75), 0.01)
        assertEquals("Normal weight", BmiCalc.category(22.86))
        assertEquals("Underweight", BmiCalc.category(18.4))
        assertEquals("Overweight", BmiCalc.category(25.0))
        assertEquals("Obesity (class 1)", BmiCalc.category(30.0))
        assertEquals(1.778, BmiCalc.feetInchesToM(5.0, 10.0), 0.001)
        assertEquals(45.359, BmiCalc.poundsToKg(100.0), 0.001)
    }

    @Test fun tipSplit() {
        val r = TipCalc.split(100.0, 15.0, 4, false)
        assertEquals(15.0, r.tip, 1e-9)
        assertEquals(115.0, r.total, 1e-9)
        assertEquals(28.75, r.perPerson, 1e-9)
        val rounded = TipCalc.split(100.0, 15.0, 4, true)
        assertEquals(29.0, rounded.perPerson, 1e-9)
        assertEquals(116.0, rounded.total, 1e-9)
        assertEquals(16.0, rounded.tip, 1e-9)
    }

    // ---- roman / words ----
    @Test fun romanRoundTrip() {
        assertEquals("MCMXCIV", RomanNumerals.toRoman(1994))
        assertEquals("MMMCMXCIX", RomanNumerals.toRoman(3999))
        assertEquals(1994, RomanNumerals.fromRoman("mcmxciv"))
        for (n in 1..3999) assertEquals(n, RomanNumerals.fromRoman(RomanNumerals.toRoman(n)))
        for (bad in listOf("IIII", "VX", "IC", "ABC", "")) {
            try { RomanNumerals.fromRoman(bad); fail("accepted $bad") } catch (e: IllegalArgumentException) { }
        }
    }

    @Test fun numberWords() {
        assertEquals("zero", NumberWords.fromString("0"))
        assertEquals("one hundred twenty-three", NumberWords.fromString("123"))
        assertEquals("one thousand two hundred thirty-four", NumberWords.fromString("1,234"))
        assertEquals("one million", NumberWords.fromString("1000000"))
        assertEquals("minus twelve point five", NumberWords.fromString("-12.5"))
        assertEquals("one million one", NumberWords.fromString("1000001"))
        assertEquals("nine hundred ninety-nine trillion nine hundred ninety-nine billion nine hundred ninety-nine million nine hundred ninety-nine thousand nine hundred ninety-nine",
            NumberWords.fromString("999999999999999"))
        assertEquals("zero point zero five", NumberWords.fromString("0.05"))
        for (bad in listOf("", "abc", "1.2.3", "1234567890123456")) {
            try { NumberWords.fromString(bad); fail("accepted $bad") } catch (e: IllegalArgumentException) { }
        }
    }

    // ---- timestamp ----
    @Test fun timestampConversion() {
        val utc = ZoneOffset.UTC
        assertEquals("1970-01-01 00:00:00", TimestampTool.format(TimestampTool.toInstant(0), utc))
        assertEquals("2023-11-14 22:13:20", TimestampTool.format(TimestampTool.toInstant(1_700_000_000L), utc))
        assertEquals(TimestampTool.Unit.MILLIS, TimestampTool.detectUnit(1_700_000_000_000L))
        assertEquals("2023-11-14 22:13:20", TimestampTool.format(TimestampTool.toInstant(1_700_000_000_000L), utc))
        assertEquals(1_700_000_000L, TimestampTool.parse("2023-11-14 22:13:20", utc).epochSecond)
        assertEquals(1_699_920_000L, TimestampTool.parse("2023-11-14", utc).epochSecond)
        assertEquals(1_700_000_000L, TimestampTool.parse("2023-11-14T22:13:20", utc).epochSecond)
        // The same wall-clock time in Karachi (UTC+5) is five hours earlier in absolute time.
        assertEquals(1_700_000_000L - 5 * 3600, TimestampTool.parse("2023-11-14 22:13:20", ZoneId.of("Asia/Karachi")).epochSecond)
        try { TimestampTool.parse("tomorrow", utc); fail() } catch (e: IllegalArgumentException) { }
    }

    // ---- morse / lorem ----
    @Test fun morseRoundTrip() {
        assertEquals("... --- ...", Morse.encode("sos").morse)
        assertEquals(".... .. / - .... . .-. .", Morse.encode("Hi there").morse)
        assertEquals("SOS", Morse.decode("... --- ..."))
        assertEquals("HI THERE", Morse.decode(".... .. / - .... . .-. ."))
        assertEquals("HI THERE", Morse.decode("•••• ••   − •••• • •−• •"))
        assertEquals("A?", Morse.decode(".- ......."))
        val e = Morse.encode("a~b")
        assertEquals(".- -...", e.morse)
        assertEquals(setOf('~'), e.skipped)
        val text = "HELLO, WORLD 2024?"
        assertEquals(text, Morse.decode(Morse.encode(text).morse))
    }

    @Test fun loremRespectsCountsAndOpening() {
        val p = Lorem.paragraphs(3, Random(1))
        assertEquals(3, p.split("\n\n").size)
        assertTrue(p.startsWith("Lorem ipsum dolor sit amet, consectetur adipiscing elit."))
        assertEquals(10, Lorem.words(10, Random(2)).split(" ").size)
        assertEquals(4, Lorem.sentences(4, Random(3)).count { it == '.' })
        assertEquals(Lorem.paragraphs(2, Random(7)), Lorem.paragraphs(2, Random(7)))
        try { Lorem.paragraphs(0); fail() } catch (e: IllegalArgumentException) { }
    }

    // ---- barcodes ----
    @Test fun barcodeValidation() {
        assertNull(BarcodeEncoder.validate(BarcodeEncoder.Type.EAN_13, "5901234123457"))
        assertNull(BarcodeEncoder.validate(BarcodeEncoder.Type.EAN_13, "590123412345"))
        assertNotNull(BarcodeEncoder.validate(BarcodeEncoder.Type.EAN_13, "59012341"))
        assertNotNull(BarcodeEncoder.validate(BarcodeEncoder.Type.ITF, "123"))
        assertNotNull(BarcodeEncoder.validate(BarcodeEncoder.Type.CODE_128, "é"))
        assertNull(BarcodeEncoder.validate(BarcodeEncoder.Type.CODE_39, "hello-123"))
    }

    @Test fun barcodesEncode() {
        val cases = listOf(
            BarcodeEncoder.Type.CODE_128 to "Hello 123", BarcodeEncoder.Type.CODE_39 to "ABC-123",
            BarcodeEncoder.Type.EAN_13 to "5901234123457", BarcodeEncoder.Type.EAN_13 to "590123412345",
            BarcodeEncoder.Type.EAN_8 to "96385074", BarcodeEncoder.Type.UPC_A to "036000291452",
            BarcodeEncoder.Type.ITF to "123456",
        )
        for ((type, text) in cases) {
            val m = BarcodeEncoder.matrix(type, text)
            assertTrue("$type $text", m.width >= 100 && m.height > 0)
            var dark = 0
            for (x in 0 until m.width) if (m.get(x, m.height / 2)) dark++
            assertTrue("$type has bars", dark > 10)
        }
    }

    @Test fun barcodeWrongCheckDigitIsRejected() {
        try { BarcodeEncoder.matrix(BarcodeEncoder.Type.EAN_13, "5901234123458"); fail("bad check digit accepted") }
        catch (e: BarcodeEncoder.BarcodeException) { assertEquals("The check digit is wrong.", e.message) }
    }
}
