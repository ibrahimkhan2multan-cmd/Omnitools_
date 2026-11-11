package com.omnitools.app

import com.omnitools.app.tools.calc.ExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorTest {
    private fun eval(s: String, deg: Boolean = true) = ExpressionEvaluator.evaluate(s, deg)

    @Test fun precedence() {
        assertEquals(14.0, eval("2+3*4"), 1e-9)
        assertEquals(20.0, eval("(2+3)*4"), 1e-9)
        assertEquals(2.5, eval("10/4"), 1e-9)
    }

    @Test fun unaryAndPower() {
        assertEquals(-3.0, eval("-3"), 1e-9)
        assertEquals(512.0, eval("2^3^2"), 1e-9) // right associative: 2^(3^2)
        assertEquals(-4.0, eval("-2^2"), 1e-9) // -(2^2)
        assertEquals(5.0, eval("2--3"), 1e-9)
    }

    @Test fun percentAndFactorial() {
        assertEquals(0.5, eval("50%"), 1e-9)
        assertEquals(20.0, eval("200*10%"), 1e-9)
        assertEquals(120.0, eval("5!"), 1e-9)
        assertEquals(1.0, eval("0!"), 1e-9)
    }

    @Test fun functionsAndConstants() {
        assertEquals(1.0, eval("sin(90)"), 1e-9)
        assertEquals(1.0, eval("sin(π/2)", deg = false), 1e-9)
        assertEquals(4.0, eval("√(16)"), 1e-9)
        assertEquals(2.0, eval("log(100)"), 1e-9)
        assertEquals(1.0, eval("ln(e)"), 1e-9)
        assertEquals(45.0, eval("atan(1)"), 1e-9)
    }

    @Test fun scientificNotationNumbers() {
        assertEquals(1500.0, eval("1.5e3"), 1e-9)
        assertEquals(0.002, eval("2E-3"), 1e-12)
    }

    @Test fun errorsAreReported() {
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("1/0") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("(1+2") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("2+") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("√(-4)") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("ln(0)") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("2 3 +") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("foo(2)") }
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("2.5!") }
    }

    @Test fun formatting() {
        assertEquals("14", ExpressionEvaluator.format(14.0))
        assertEquals("0.3", ExpressionEvaluator.format(0.1 + 0.2))
        assertEquals("0", ExpressionEvaluator.format(0.0))
        assertEquals("-2.5", ExpressionEvaluator.format(-2.5))
    }

    @Test fun implicitMultiplication() {
        assertEquals(6.0, eval("2(3)"), 1e-9)
        assertEquals(9.0, eval("(1+2)(3)"), 1e-9)
        assertEquals(2 * Math.PI, eval("2π"), 1e-9)
        assertEquals(1.5, eval("3sin(30)"), 1e-9)
        assertEquals(6.0, eval("2√9"), 1e-9)
        assertEquals(2 * Math.E, eval("2e"), 1e-9)
        assertEquals(200.0, eval("2e2"), 1e-9)          // still scientific notation
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("2foo") }
    }

    @Test fun tanOfRightAngleIsAnError() {
        assertThrows(ExpressionEvaluator.EvalException::class.java) { eval("tan(90)") }
        assertEquals(1.0, eval("tan(45)"), 1e-9)
    }

    @Test fun bigWholeNumbersKeepEveryDigit() {
        assertEquals("562949953421312", ExpressionEvaluator.format(Math.pow(2.0, 49.0)))
        assertEquals("123456789012345", ExpressionEvaluator.format(123456789012345.0))
        assertEquals("-9007199254740", ExpressionEvaluator.format(-9007199254740.0))
        assertEquals("1E+15", ExpressionEvaluator.format(1e15))
        assertEquals("0.3", ExpressionEvaluator.format(0.1 + 0.2))
    }
}

class UrduKeyboardDigitsTest {
    @Test fun numberWordsRejectsNonAsciiDigitsInsteadOfCrashing() {
        // Arabic-Indic and Extended Arabic-Indic digits come from Urdu / Arabic keyboards.
        assertThrows(IllegalArgumentException::class.java) { com.omnitools.app.tools.calc.NumberWords.fromString("1.٣") }
        assertThrows(IllegalArgumentException::class.java) { com.omnitools.app.tools.calc.NumberWords.fromString("۱۲۳") }
        assertEquals("one hundred twenty-three point four five", com.omnitools.app.tools.calc.NumberWords.fromString("123.45"))
    }
}

class AuditEdgeCaseTest {
    @Test fun timestampRejectsYearsThatBreakMillisecondMaths() {
        val utc = java.time.ZoneOffset.UTC
        assertThrows(IllegalArgumentException::class.java) { com.omnitools.app.tools.dev.TimestampTool.parse("+999999999-12-31T00:00:00", utc) }
        assertEquals(253402300799L, com.omnitools.app.tools.dev.TimestampTool.parse("9999-12-31 23:59:59", utc).epochSecond)
        assertEquals(1710513000L, com.omnitools.app.tools.dev.TimestampTool.parse("2024-03-15 14:30:00", utc).epochSecond)
    }

    @Test fun numberBaseRejectsDoubleSigns() {
        val n = com.omnitools.app.tools.everyday.NumberBase
        assertEquals(null, n.parse("--5", 10))
        assertEquals(null, n.parse("+5", 10))
        assertEquals(null, n.parse("-+5", 10))
        assertEquals(java.math.BigInteger.valueOf(-255), n.parse("-0xFF", 16))
        assertEquals(java.math.BigInteger.valueOf(10), n.parse("1010", 2))
    }

    @Test fun itfBarcodeRejectsNonAsciiDigitsCleanly() {
        val t = com.omnitools.app.tools.qr.BarcodeEncoder.Type.ITF
        assertEquals("Digits only.", com.omnitools.app.tools.qr.BarcodeEncoder.validate(t, "١٢٣٤"))
        assertEquals(null, com.omnitools.app.tools.qr.BarcodeEncoder.validate(t, "1234"))
    }
}
