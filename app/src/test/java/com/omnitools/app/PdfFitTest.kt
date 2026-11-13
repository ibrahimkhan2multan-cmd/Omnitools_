package com.omnitools.app

import com.omnitools.app.tools.pdf.PdfSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfFitTest {
    @Test fun keepsAspectRatio() {
        val (w, h) = PdfSession.fit(595, 842, 1000)
        assertEquals(1000, w)
        assertEquals(1414, h)
    }

    @Test fun capsHugeWidthsAndTallPages() {
        val (w, _) = PdfSession.fit(595, 842, 99999)
        assertEquals(true, w <= 2400)
        val (tw, th) = PdfSession.fit(100, 20000, 2400)
        assertTrue(tw.toLong() * th <= PdfSession.MAX_PIXELS + 2400)
        assertTrue(tw >= 1 && th >= 1)
    }

    @Test fun survivesBadPageSizes() {
        val (w, h) = PdfSession.fit(0, 0, 500)
        assertTrue(w >= 1 && h >= 1)
    }
}
