package com.omnitools.app.tools.text

/**
 * "Bold", "italic" and other looks for plain text. Plain text cannot really be bold, so these swap English letters
 * and digits for look-alike Unicode characters (the "Mathematical Alphanumeric Symbols" block and friends). The result
 * can be pasted into chats and social posts. Other characters (accents, other alphabets, emoji) are left alone, and
 * some fonts or apps may show boxes for the special characters.
 */
object FancyText {

    enum class Style(val label: String) {
        BOLD("Bold"),
        ITALIC("Italic"),
        BOLD_ITALIC("Bold italic"),
        SCRIPT("Script"),
        FRAKTUR("Fraktur"),
        DOUBLE_STRUCK("Double-struck"),
        MONOSPACE("Monospace"),
        CIRCLED("Circled"),
        SMALL_CAPS("Small caps"),
        UNDERLINE("Underline"),
        STRIKETHROUGH("Strikethrough"),
        UPSIDE_DOWN("Upside down"),
    }

    private const val STRIKE = '̶'
    private const val UNDER = '̲'

    // Letters that have a separate code point in the double-struck block.
    private val doubleStruckHoles = mapOf(
        'C' to 0x2102, 'H' to 0x210D, 'N' to 0x2115, 'P' to 0x2119, 'Q' to 0x211A, 'R' to 0x211D, 'Z' to 0x2124,
    )

    private const val SMALL_CAPS_LOWER = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"

    private val upsideDown: Map<Char, Char> = buildMap {
        val from = "abcdefghijklmnopqrstuvwxyz0123456789.,'\"?!()[]{}<>&_;"
        val to = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz" + "0ƖᄅƐㄣϛ9ㄥ86" + "˙',„¿¡)(][}{><⅋‾؛"
        for (i in from.indices) put(from[i], to[i])
    }

    /** Applies [style] to [text]. Never throws; unknown characters pass through. */
    fun apply(style: Style, text: String): String = when (style) {
        Style.BOLD -> block(text, 0x1D5D4, 0x1D5EE, 0x1D7EC)
        Style.ITALIC -> block(text, 0x1D608, 0x1D622, null)
        Style.BOLD_ITALIC -> block(text, 0x1D63C, 0x1D656, null)
        Style.SCRIPT -> block(text, 0x1D4D0, 0x1D4EA, null)
        Style.FRAKTUR -> block(text, 0x1D56C, 0x1D586, null)
        Style.DOUBLE_STRUCK -> block(text, 0x1D538, 0x1D552, 0x1D7D8, doubleStruckHoles)
        Style.MONOSPACE -> block(text, 0x1D670, 0x1D68A, 0x1D7F6)
        Style.CIRCLED -> circled(text)
        Style.SMALL_CAPS -> smallCaps(text)
        Style.UNDERLINE -> combine(text, UNDER)
        Style.STRIKETHROUGH -> combine(text, STRIKE)
        Style.UPSIDE_DOWN -> text.lowercase().reversed().map { upsideDown[it] ?: it }.joinToString("")
    }

    /** Turns styled text made by [apply] back into ordinary letters (upside-down text is not reversed). */
    fun plain(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == STRIKE.code || cp == UNDER.code) continue
            val back = reverse[cp]
            if (back != null) sb.append(back) else sb.appendCodePoint(cp)
        }
        return sb.toString()
    }

    private fun block(
        text: String, upper: Int, lower: Int, digit: Int?, holes: Map<Char, Int> = emptyMap(),
    ): String {
        val sb = StringBuilder(text.length * 2)
        for (ch in text) {
            when {
                ch in 'A'..'Z' -> sb.appendCodePoint(holes[ch] ?: (upper + (ch - 'A')))
                ch in 'a'..'z' -> sb.appendCodePoint(holes[ch] ?: (lower + (ch - 'a')))
                ch in '0'..'9' && digit != null -> sb.appendCodePoint(digit + (ch - '0'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun circled(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when {
                ch in 'A'..'Z' -> sb.appendCodePoint(0x24B6 + (ch - 'A'))
                ch in 'a'..'z' -> sb.appendCodePoint(0x24D0 + (ch - 'a'))
                ch in '1'..'9' -> sb.appendCodePoint(0x2460 + (ch - '1'))
                ch == '0' -> sb.appendCodePoint(0x24EA)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun smallCaps(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val l = ch.lowercaseChar()
            if (l in 'a'..'z') sb.append(SMALL_CAPS_LOWER[l - 'a']) else sb.append(ch)
        }
        return sb.toString()
    }

    private fun combine(text: String, mark: Char): String {
        val sb = StringBuilder(text.length * 2)
        for (ch in text) {
            sb.append(ch)
            if (!ch.isWhitespace()) sb.append(mark)
        }
        return sb.toString()
    }

    /** Styled code point -> plain letter or digit. */
    private val reverse: Map<Int, Char> by lazy {
        val map = HashMap<Int, Char>()
        for (style in Style.values()) {
            if (style == Style.UNDERLINE || style == Style.STRIKETHROUGH || style == Style.UPSIDE_DOWN) continue
            for (c in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789") {
                val styled = apply(style, c.toString())
                if (styled != c.toString()) map[styled.codePointAt(0)] = c
            }
        }
        map
    }
}
