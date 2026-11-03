package com.omnitools.app

import com.omnitools.app.tools.timer.TimerModel
import com.omnitools.app.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerPrecisionTest {

    @Test fun oneMillisecondIsAValidDuration() {
        assertEquals(1L, TimerModel.durationFrom(0, 0, 0, 1))
        assertEquals(0L, TimerModel.durationFrom(0, 0, 0, 0))
    }

    @Test fun fieldsAreAddedAndCarry() {
        assertEquals(3_723_004L, TimerModel.durationFrom(1, 2, 3, 4))
        // 90 seconds and 1500 ms are legal inputs; they just add up.
        assertEquals(91_500L, TimerModel.durationFrom(0, 0, 90, 1500))
    }

    @Test fun durationIsCappedAt99Hours() {
        assertEquals(TimerModel.MAX_MS, TimerModel.durationFrom(500, 0, 0, 0))
        assertEquals(99L * 3_600_000L, TimerModel.MAX_MS)
        // Absurdly large digits cannot overflow.
        assertEquals(TimerModel.MAX_MS, TimerModel.durationFrom(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
    }

    @Test fun splitIsInverseOfDuration() {
        val parts = TimerModel.split(3_723_004L)
        assertEquals(TimerModel.Parts(1, 2, 3, 4), parts)
        assertEquals(3_723_004L, TimerModel.durationFrom(parts.hours, parts.minutes, parts.seconds, parts.millis))
        assertEquals(TimerModel.Parts(0, 0, 0, 1), TimerModel.split(1))
    }

    @Test fun preciseCountdownNeverRounds() {
        assertEquals("00:00.001", TimeFormat.countdownPrecise(1))
        assertEquals("00:00.000", TimeFormat.countdownPrecise(0))
        assertEquals("00:00.000", TimeFormat.countdownPrecise(-5))
        assertEquals("01:01.005", TimeFormat.countdownPrecise(61_005))
        assertEquals("1:00:00.000", TimeFormat.countdownPrecise(3_600_000))
        assertEquals("99:00:00.000", TimeFormat.countdownPrecise(TimerModel.MAX_MS))
    }
}
