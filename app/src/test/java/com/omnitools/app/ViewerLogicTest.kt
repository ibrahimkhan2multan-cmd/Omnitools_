package com.omnitools.app

import com.omnitools.app.tools.viewer.ViewerLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerLogicTest {
    @Test fun sampleSizeShrinksBigPictures() {
        assertEquals(1, ViewerLogic.sampleSize(1000, 800, 2400))
        assertEquals(1, ViewerLogic.sampleSize(2400, 100, 2400))
        assertEquals(2, ViewerLogic.sampleSize(4800, 3000, 2400))
        assertEquals(4, ViewerLogic.sampleSize(12000, 9000, 2400))
        assertEquals(1, ViewerLogic.sampleSize(0, 0, 2400))
    }

    @Test fun positionAndZoomHelpers() {
        assertEquals("1 / 3", ViewerLogic.position(0, 3))
        assertEquals("3 / 3", ViewerLogic.position(9, 3))
        assertEquals("", ViewerLogic.position(0, 0))
        assertEquals(2.5f, ViewerLogic.doubleTapScale(1f), 0f)
        assertEquals(1f, ViewerLogic.doubleTapScale(2.5f), 0f)
        assertEquals(0f, ViewerLogic.maxPan(1000f, 1f), 0f)
        assertEquals(500f, ViewerLogic.maxPan(1000f, 2f), 0f)
    }

    @Test fun speedLabels() {
        assertEquals("1x", ViewerLogic.speedLabel(1f))
        assertEquals("0.5x", ViewerLogic.speedLabel(0.5f))
        assertEquals("2x", ViewerLogic.speedLabel(2f))
    }
}
