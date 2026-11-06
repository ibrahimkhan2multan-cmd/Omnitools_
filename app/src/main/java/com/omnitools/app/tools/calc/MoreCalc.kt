package com.omnitools.app.tools.calc

import kotlin.math.ceil

/** Loan / EMI maths. Fixed-rate loan repaid in equal monthly payments. */
object LoanCalc {
    data class Result(val monthlyPayment: Double, val totalPayment: Double, val totalInterest: Double)
    data class Row(val month: Int, val interest: Double, val principalPaid: Double, val balance: Double)

    fun emi(principal: Double, annualRatePercent: Double, months: Int): Result {
        require(principal > 0.0) { "Loan amount must be more than 0." }
        require(principal <= 1e12) { "Loan amount is too large." }
        require(months in 1..1200) { "The term must be between 1 month and 100 years." }
        require(annualRatePercent in 0.0..200.0) { "The interest rate must be between 0% and 200%." }
        val r = annualRatePercent / 1200.0
        val payment = if (r == 0.0) {
            principal / months
        } else {
            // expm1/ln1p stay accurate for tiny rates, where (1 + r)^n - 1 would round to zero.
            val growthMinusOne = Math.expm1(months * Math.log1p(r))
            principal * r * (growthMinusOne + 1) / growthMinusOne
        }
        val total = payment * months
        return Result(payment, total, total - principal)
    }

    /** Month-by-month split of each payment into interest and principal. */
    fun schedule(principal: Double, annualRatePercent: Double, months: Int): List<Row> {
        val pay = emi(principal, annualRatePercent, months).monthlyPayment
        val r = annualRatePercent / 1200.0
        var balance = principal
        val rows = ArrayList<Row>(months)
        for (m in 1..months) {
            val interest = balance * r
            val paid = if (m == months) balance else pay - interest
            balance = (balance - paid).coerceAtLeast(0.0)
            rows += Row(m, interest, paid, balance)
        }
        return rows
    }
}

/** Body mass index with the standard WHO adult categories. A screening number, not a diagnosis. */
object BmiCalc {
    fun bmi(weightKg: Double, heightM: Double): Double {
        require(weightKg in 2.0..500.0) { "Enter a weight between 2 and 500 kg." }
        require(heightM in 0.5..2.8) { "Enter a height between 0.5 and 2.8 m." }
        return weightKg / (heightM * heightM)
    }

    fun category(bmi: Double): String = when {
        bmi < 16.0 -> "Severely underweight"
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal weight"
        bmi < 30.0 -> "Overweight"
        bmi < 35.0 -> "Obesity (class 1)"
        bmi < 40.0 -> "Obesity (class 2)"
        else -> "Obesity (class 3)"
    }

    /** Weights (kg) that give a BMI of 18.5 and 24.9 at this height. */
    fun healthyRange(heightM: Double): Pair<Double, Double> = 18.5 * heightM * heightM to 24.9 * heightM * heightM

    fun poundsToKg(lb: Double) = lb * 0.45359237
    fun feetInchesToM(feet: Double, inches: Double) = (feet * 12.0 + inches) * 0.0254
}

object TipCalc {
    data class Result(val tip: Double, val total: Double, val perPerson: Double)

    /** [roundUpPerPerson] rounds each person's share up to a whole unit; the tip grows to match. */
    fun split(bill: Double, tipPercent: Double, people: Int, roundUpPerPerson: Boolean): Result {
        require(bill > 0.0) { "Enter the bill amount." }
        require(tipPercent in 0.0..100.0) { "Tip must be between 0% and 100%." }
        require(people in 1..100) { "People must be between 1 and 100." }
        var total = bill * (1 + tipPercent / 100.0)
        var each = total / people
        if (roundUpPerPerson) {
            each = ceil(each - 1e-9)
            total = each * people
        }
        return Result(total - bill, total, each)
    }
}

object RomanNumerals {
    private val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    private val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    private val valid = Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")

    fun toRoman(n: Int): String {
        require(n in 1..3999) { "Roman numerals cover 1 to 3999." }
        var rest = n
        val sb = StringBuilder()
        for (i in values.indices) {
            while (rest >= values[i]) {
                sb.append(symbols[i])
                rest -= values[i]
            }
        }
        return sb.toString()
    }

    fun fromRoman(text: String): Int {
        val s = text.trim().uppercase()
        require(s.isNotEmpty() && valid.matches(s)) { "Not a valid Roman numeral." }
        var i = 0
        var total = 0
        for (k in values.indices) {
            while (s.startsWith(symbols[k], i)) {
                total += values[k]
                i += symbols[k].length
            }
        }
        return total
    }
}

/** English number names, American style (no "and"): 1,234 -> "one thousand two hundred thirty-four". */
object NumberWords {
    private val ones = arrayOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
        "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )
    private val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    private val scales = arrayOf("", "thousand", "million", "billion", "trillion")

    const val MAX_INTEGER_DIGITS = 15

    private fun below1000(n: Int): String {
        val parts = ArrayList<String>()
        var rest = n
        if (rest >= 100) {
            parts += ones[rest / 100] + " hundred"
            rest %= 100
        }
        if (rest >= 20) {
            parts += tens[rest / 10] + if (rest % 10 > 0) "-" + ones[rest % 10] else ""
        } else if (rest > 0) {
            parts += ones[rest]
        }
        return parts.joinToString(" ")
    }

    fun words(n: Long): String {
        if (n == 0L) return "zero"
        if (n < 0) return "minus " + words(-n)
        val groups = ArrayList<String>()
        var rest = n
        var scale = 0
        while (rest > 0) {
            val g = (rest % 1000).toInt()
            if (g > 0) groups += below1000(g) + if (scales[scale].isNotEmpty()) " " + scales[scale] else ""
            rest /= 1000
            scale++
        }
        return groups.reversed().joinToString(" ")
    }

    /** Accepts "-12.5", "1,234" (commas ignored). Decimal digits are read out one by one. */
    fun fromString(input: String): String {
        val s = input.trim().replace(",", "")
        require(s.isNotEmpty()) { "Enter a number." }
        val negative = s.startsWith("-")
        val body = s.removePrefix("-")
        val parts = body.split(".")
        require(parts.size <= 2 && parts.all { p -> p.all { it.isDigit() } } && parts[0].isNotEmpty()) { "Enter a valid number." }
        require(parts[0].length <= MAX_INTEGER_DIGITS) { "Up to $MAX_INTEGER_DIGITS digits before the decimal point." }
        val fraction = parts.getOrNull(1).orEmpty()
        require(fraction.length <= 30) { "Up to 30 digits after the decimal point." }
        val integerWords = words(parts[0].toLong())
        val sb = StringBuilder()
        if (negative && (parts[0].toLong() != 0L || fraction.any { it != '0' })) sb.append("minus ")
        sb.append(integerWords)
        if (fraction.isNotEmpty()) sb.append(" point ").append(fraction.map { ones[it - '0'] }.joinToString(" "))
        return sb.toString()
    }
}
