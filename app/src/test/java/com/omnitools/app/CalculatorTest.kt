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
}
