package com.omnitools.app

import com.omnitools.app.ui.theme.Dur
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokensTest {
    @Test fun animationsStayShort() {
        listOf(Dur.PRESS, Dur.FAST, Dur.MEDIUM, Dur.SCREEN).forEach { assertTrue(it in 50..400) }
        assertTrue(Dur.PRESS < Dur.FAST && Dur.FAST < Dur.MEDIUM && Dur.MEDIUM <= Dur.SCREEN)
    }
}
