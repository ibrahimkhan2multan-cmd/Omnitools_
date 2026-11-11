package com.omnitools.app

import com.omnitools.app.ui.theme.BackgroundTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundThemeTest {
    @Test fun lookupFallsBackToPlain() {
        assertEquals(BackgroundTheme.GALAXY, BackgroundTheme.from("GALAXY"))
        assertEquals(BackgroundTheme.DEFAULT, BackgroundTheme.from("nope"))
        assertEquals(BackgroundTheme.DEFAULT, BackgroundTheme.from(null))
    }

    @Test fun everyThemeIsDescribed() {
        val labels = BackgroundTheme.values().map { it.label }
        assertEquals(labels.size, labels.toSet().size)
        for (b in BackgroundTheme.values()) {
            assertTrue(b.label.isNotBlank() && b.description.isNotBlank() && b.swatch.isNotEmpty())
        }
    }
}
