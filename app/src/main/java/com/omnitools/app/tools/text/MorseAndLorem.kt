package com.omnitools.app.tools.text

import java.util.Random

object Morse {
    private val table: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....",
        'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.",
        'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....",
        '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--", '/' to "-..-.",
        '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.", '-' to "-....-", '_' to "..--.-", '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-.",
    )
    private val reverse = table.entries.associate { it.value to it.key }

    data class Encoded(val morse: String, val skipped: Set<Char>)

    /** Letters are separated by a space, words by " / ". Characters with no Morse code are skipped and reported. */
    fun encode(text: String): Encoded {
        val skipped = linkedSetOf<Char>()
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { word ->
            word.uppercase().mapNotNull { ch ->
                table[ch] ?: run { skipped += ch; null }
            }.joinToString(" ")
        }.filter { it.isNotEmpty() }
        return Encoded(words.joinToString(" / "), skipped)
    }

    /** Dots may be written . or • or ·, dashes - or − or –; letters split by spaces, words by / or a wide gap. */
    fun decode(morse: String): String {
        val cleaned = morse.trim()
            .replace('•', '.').replace('·', '.')
            .replace('−', '-').replace('–', '-').replace('—', '-')
            .replace('|', '/')
        return cleaned.split(Regex("\\s*/\\s*|\\s{3,}")).joinToString(" ") { word ->
            word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString("") { code ->
                (reverse[code] ?: '?').toString()
            }
        }.trim()
    }
}

object Lorem {
    private val words = (
        "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore " +
            "magna aliqua enim ad minim veniam quis nostrud exercitation ullamco laboris nisi aliquip ex ea commodo " +
            "consequat duis aute irure in reprehenderit voluptate velit esse cillum eu fugiat nulla pariatur excepteur " +
            "sint occaecat cupidatat non proident sunt culpa qui officia deserunt mollit anim id est laborum"
        ).split(" ")

    const val MAX_WORDS = 5000
    const val MAX_PARAGRAPHS = 100
    private const val OPENING = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

    private fun sentence(rnd: Random, first: Boolean): String {
        if (first) return "$OPENING."
        val n = 6 + rnd.nextInt(10)
        val w = List(n) { words[rnd.nextInt(words.size)] }.toMutableList()
        if (n > 9) w[3 + rnd.nextInt(3)] += ","
        return w.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
    }

    fun paragraphs(count: Int, rnd: Random = Random()): String {
        require(count in 1..MAX_PARAGRAPHS) { "Choose 1 to $MAX_PARAGRAPHS paragraphs." }
        return List(count) { p ->
            val sentences = 3 + rnd.nextInt(4)
            (0 until sentences).joinToString(" ") { s -> sentence(rnd, first = p == 0 && s == 0) }
        }.joinToString("\n\n")
    }

    fun sentences(count: Int, rnd: Random = Random()): String {
        require(count in 1..200) { "Choose 1 to 200 sentences." }
        return (0 until count).joinToString(" ") { sentence(rnd, first = it == 0) }
    }

    fun words(count: Int, rnd: Random = Random()): String {
        require(count in 1..MAX_WORDS) { "Choose 1 to $MAX_WORDS words." }
        return List(count) { i -> if (i < 5) words[i] else words[rnd.nextInt(words.size)] }.joinToString(" ")
    }
}
