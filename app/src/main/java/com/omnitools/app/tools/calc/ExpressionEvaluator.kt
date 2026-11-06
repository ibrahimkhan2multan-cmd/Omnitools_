package com.omnitools.app.tools.calc

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Recursive-descent evaluator for the calculator.
 *
 * Grammar (highest precedence last):
 *   expr    = term (('+' | '-') term)*
 *   term    = unary (('*' | '/' | '×' | '÷') unary)*
 *   unary   = ('-' | '+') unary | power
 *   power   = postfix ('^' unary)?          (right-associative)
 *   postfix = primary ('%' | '!')*
 *   primary = number | constant | function '(' expr ')' | '(' expr ')'
 *
 * "%" divides by 100, so 50% = 0.5 and 200*10% = 20.
 */
object ExpressionEvaluator {

    class EvalException(message: String) : Exception(message)

    const val MAX_LENGTH = 1000
    private const val MAX_DEPTH = 150

    fun evaluate(input: String, degrees: Boolean = true): Double {
        if (input.length > MAX_LENGTH) throw EvalException("Expression is too long")
        val parser = Parser(input.replace(" ", ""), degrees)
        val result = parser.parseExpression()
        if (!parser.atEnd()) throw EvalException("Unexpected \"${parser.peek()}\"")
        if (result.isNaN()) throw EvalException("Result is not a number")
        if (result.isInfinite()) throw EvalException("Result is too large")
        return result
    }

    /** Formats like a calculator: no trailing ".0", up to 12 significant digits, scientific notation when extreme. */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        if (value.isNaN()) return "Not a number"
        if (value.isInfinite()) return if (value > 0) "Too large" else "Too small"
        val abs = abs(value)
        if (abs >= 1e15 || abs < 1e-9) return String.format(java.util.Locale.ROOT, "%.9E", value).replace(Regex("\\.?0+E"), "E")
        val bd = java.math.BigDecimal(value).round(java.math.MathContext(12))
        return bd.stripTrailingZeros().toPlainString()
    }

    private class Parser(val s: String, val degrees: Boolean) {
        var i = 0
        private var depth = 0

        /** Guards the recursion so a very deeply nested input becomes an error, not a crash. */
        private inline fun <T> nested(block: () -> T): T {
            if (++depth > MAX_DEPTH) throw EvalException("Expression is too deeply nested")
            try { return block() } finally { depth-- }
        }

        fun atEnd() = i >= s.length
        fun peek(): Char = if (atEnd()) '\u0000' else s[i]

        private fun consume(c: Char): Boolean {
            if (peek() == c) {
                i++
                return true
            }
            return false
        }

        fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                v = when {
                    consume('+') -> v + parseTerm()
                    consume('-') -> v - parseTerm()
                    else -> return v
                }
            }
        }

        private fun parseTerm(): Double {
            var v = parseUnary()
            while (true) {
                when {
                    consume('*') || consume('×') -> v *= parseUnary()
                    consume('/') || consume('÷') -> {
                        val d = parseUnary()
                        if (d == 0.0) throw EvalException("Cannot divide by zero")
                        v /= d
                    }
                    else -> return v
                }
            }
        }

        private fun parseUnary(): Double = nested {
            when {
                consume('-') -> -parseUnary()
                consume('+') -> parseUnary()
                else -> parsePower()
            }
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            return if (consume('^')) base.pow(parseUnary()) else base
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (true) {
                v = when {
                    consume('%') -> v / 100.0
                    consume('!') -> factorial(v)
                    else -> return v
                }
            }
        }

        private fun factorial(v: Double): Double {
            if (v < 0 || v != Math.floor(v)) throw EvalException("Factorial needs a whole number")
            if (v > 170) throw EvalException("Result is too large")
            var r = 1.0
            for (k in 2..v.toInt()) r *= k
            return r
        }

        private fun parsePrimary(): Double {
            if (consume('(')) {
                val v = nested { parseExpression() }
                if (!consume(')')) throw EvalException("Missing closing bracket")
                return v
            }
            if (peek().isDigit() || peek() == '.') return parseNumber()
            if (peek().isLetter() || peek() == 'π' || peek() == '√') return parseNameOrFunction()
            if (atEnd()) throw EvalException("Incomplete expression")
            throw EvalException("Unexpected \"${peek()}\"")
        }

        private fun parseNumber(): Double {
            val start = i
            while (peek().isDigit() || peek() == '.') i++
            // Optional exponent like 1e5 or 2E-3 (only if digits follow).
            if ((peek() == 'e' || peek() == 'E') && i + 1 < s.length) {
                var j = i + 1
                if (s[j] == '+' || s[j] == '-') j++
                if (j < s.length && s[j].isDigit()) {
                    i = j
                    while (peek().isDigit()) i++
                }
            }
            return s.substring(start, i).toDoubleOrNull() ?: throw EvalException("Invalid number")
        }

        private fun parseNameOrFunction(): Double {
            if (consume('π')) return Math.PI
            if (consume('√')) {
                val arg = if (consume('(')) {
                    val v = parseExpression()
                    if (!consume(')')) throw EvalException("Missing closing bracket")
                    v
                } else parsePostfix()
                if (arg < 0) throw EvalException("Square root of a negative number")
                return sqrt(arg)
            }
            val start = i
            while (peek().isLetter()) i++
            val name = s.substring(start, i).lowercase()
            when (name) {
                "pi" -> return Math.PI
                "e" -> return Math.E
            }
            if (!consume('(')) throw EvalException("Unknown name \"$name\"")
            val arg = nested { parseExpression() }
            if (!consume(')')) throw EvalException("Missing closing bracket")
            val rad = if (degrees) Math.toRadians(arg) else arg
            return when (name) {
                "sqrt" -> if (arg < 0) throw EvalException("Square root of a negative number") else sqrt(arg)
                "cbrt" -> cbrt(arg)
                "abs" -> abs(arg)
                "sin" -> snap(sin(rad))
                "cos" -> snap(cos(rad))
                "tan" -> snap(tan(rad))
                "asin" -> if (arg !in -1.0..1.0) throw EvalException("asin needs a value from -1 to 1") else fromRad(asin(arg))
                "acos" -> if (arg !in -1.0..1.0) throw EvalException("acos needs a value from -1 to 1") else fromRad(acos(arg))
                "atan" -> fromRad(atan(arg))
                "ln" -> if (arg <= 0) throw EvalException("ln needs a positive number") else ln(arg)
                "log" -> if (arg <= 0) throw EvalException("log needs a positive number") else log10(arg)
                "exp" -> exp(arg)
                else -> throw EvalException("Unknown function \"$name\"")
            }
        }

        /** sin(180°) is 1.2e-16 in floating point; show the 0 a person expects. */
        private fun snap(v: Double) = if (abs(v) < 1e-14) 0.0 else v

        private fun fromRad(v: Double) = if (degrees) Math.toDegrees(v) else v
    }
}
