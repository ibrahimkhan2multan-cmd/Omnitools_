package com.omnitools.app

import com.omnitools.app.tools.text.FancyText
import com.omnitools.app.tools.text.FancyText.Style
import com.omnitools.app.tools.text.TextTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TextStyleTest {

    private fun cps(vararg cp: Int) = cp.joinToString("") { String(Character.toChars(it)) }

    @Test fun boldUsesSansSerifBoldBlock() {
        // H = 0x1D5D4 + 7, i = 0x1D5EE + 8, digit 5 = 0x1D7EC + 5
        assertEquals(cps(0x1D5DB, 0x1D5F6, 0x1D7F1), FancyText.apply(Style.BOLD, "Hi5"))
        assertEquals("!? é", FancyText.apply(Style.BOLD, "!? é")) // everything else is untouched
    }

    @Test fun doubleStruckUsesTheSeparateHoleCodePoints() {
        assertEquals("ℂℍℕℙℚℝℤ", FancyText.apply(Style.DOUBLE_STRUCK, "CHNPQRZ"))
        assertEquals(cps(0x1D538, 0x1D539), FancyText.apply(Style.DOUBLE_STRUCK, "AB"))
    }

    @Test fun circledAndSmallCaps() {
        assertEquals("Ⓐⓐ①⓪", FancyText.apply(Style.CIRCLED, "Aa10"))
        assertEquals("ʜᴇʟʟᴏ ꜱᴏ", FancyText.apply(Style.SMALL_CAPS, "Hello So"))
    }

    @Test fun strikeAndUnderlineSkipSpaces() {
        assertEquals("a̶b̶ c̶", FancyText.apply(Style.STRIKETHROUGH, "ab c"))
        assertEquals("a̲ b̲", FancyText.apply(Style.UNDERLINE, "a b"))
    }

    @Test fun upsideDownReversesAndFlips() {
        assertEquals("ǝɥ", FancyText.apply(Style.UPSIDE_DOWN, "he"))
        assertEquals("¿ɐ", FancyText.apply(Style.UPSIDE_DOWN, "a?"))
    }

    @Test fun everyStyleRoundTripsBackToPlainText() {
        val original = "Hello World 123 CHNPQRZ"
        for (style in Style.values()) {
            if (style == Style.UPSIDE_DOWN || style == Style.SMALL_CAPS) continue // lossy: reversed / no capitals
            assertEquals("round trip of $style", original, FancyText.plain(FancyText.apply(style, original)))
        }
    }

    @Test fun stylingChangesLettersButNotWhenThereAreNone() {
        assertNotEquals("abc", FancyText.apply(Style.ITALIC, "abc"))
        assertEquals("123", FancyText.apply(Style.ITALIC, "123"))
        assertEquals("", FancyText.apply(Style.BOLD, ""))
    }

    @Test fun moreCases() {
        assertEquals("Hello. World is here\nNew line", TextTools.sentenceCase("hello. WORLD is here\nnew line"))
        assertEquals("AbC", TextTools.toggleCase("aBc"))
        assertEquals("hElLo wOrLd", TextTools.alternatingCase("hello world"))
        assertEquals("helloWorldFooBar", TextTools.camelCase("Hello World-fooBar"))
        assertEquals("hello_world_foo_bar", TextTools.snakeCase("Hello World-fooBar"))
        assertEquals("hello-world\nfoo-bar", TextTools.kebabCase("Hello World\nfoo bar"))
    }

    @Test fun moreLineTools() {
        assertEquals("a b c", TextTools.removeLineBreaks("a\nb\r\nc"))
        assertEquals("a\nb", TextTools.removeEmptyLines("a\n\n  \nb"))
        assertEquals("• x\n\n• y", TextTools.addBullets("x\n\ny"))
    }
}
