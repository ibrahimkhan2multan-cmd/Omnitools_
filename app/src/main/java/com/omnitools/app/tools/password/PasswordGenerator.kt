package com.omnitools.app.tools.password

import java.security.SecureRandom

/** Generates passwords from a cryptographically secure random source. Output is never stored or logged. */
object PasswordGenerator {

    const val MIN_LENGTH = 6
    const val MAX_LENGTH = 64

    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"   // no 'l'
    private const val LOWER_AMBIGUOUS = "l"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"     // no 'I' or 'O'
    private const val UPPER_AMBIGUOUS = "IO"
    private const val DIGITS = "23456789"                    // no '0' or '1'
    private const val DIGITS_AMBIGUOUS = "01"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?"

    data class Options(
        val length: Int = 16,
        val lower: Boolean = true,
        val upper: Boolean = true,
        val digits: Boolean = true,
        val symbols: Boolean = true,
        val avoidAmbiguous: Boolean = true,
    )

    fun generate(options: Options, random: SecureRandom = SecureRandom()): String {
        require(options.length in MIN_LENGTH..MAX_LENGTH) { "Length must be between $MIN_LENGTH and $MAX_LENGTH." }
        val pools = buildList {
            if (options.lower) add(if (options.avoidAmbiguous) LOWER else LOWER + LOWER_AMBIGUOUS)
            if (options.upper) add(if (options.avoidAmbiguous) UPPER else UPPER + UPPER_AMBIGUOUS)
            if (options.digits) add(if (options.avoidAmbiguous) DIGITS else DIGITS + DIGITS_AMBIGUOUS)
            if (options.symbols) add(SYMBOLS)
        }
        require(pools.isNotEmpty()) { "Select at least one character type." }

        val all = pools.joinToString("")
        val chars = ArrayList<Char>(options.length)
        // Guarantee at least one character from every selected pool.
        pools.forEach { chars.add(it[random.nextInt(it.length)]) }
        while (chars.size < options.length) chars.add(all[random.nextInt(all.length)])
        // Fisher-Yates shuffle so the guaranteed characters are not always at the start.
        for (i in chars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }
        return chars.joinToString("")
    }

    /** Rough strength estimate in bits of entropy for the chosen options (assumes uniform random choice). */
    fun entropyBits(options: Options): Double {
        var size = 0
        if (options.lower) size += if (options.avoidAmbiguous) LOWER.length else LOWER.length + LOWER_AMBIGUOUS.length
        if (options.upper) size += if (options.avoidAmbiguous) UPPER.length else UPPER.length + UPPER_AMBIGUOUS.length
        if (options.digits) size += if (options.avoidAmbiguous) DIGITS.length else DIGITS.length + DIGITS_AMBIGUOUS.length
        if (options.symbols) size += SYMBOLS.length
        if (size == 0) return 0.0
        return options.length * (Math.log(size.toDouble()) / Math.log(2.0))
    }

    fun strengthLabel(bits: Double): String = when {
        bits < 40 -> "Weak"
        bits < 60 -> "Fair"
        bits < 80 -> "Strong"
        else -> "Very strong"
    }
}
