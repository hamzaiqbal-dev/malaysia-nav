package app.organicmaps.my

import app.organicmaps.my.NavHudFormat.SpeedState
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NavHudFormatTest {
    @Test
    fun etaRoundsUpToWholeMinutes() {
        assertEquals(NavHudFormat.Eta(0, 14), NavHudFormat.eta(14 * 60))
        assertEquals(NavHudFormat.Eta(0, 14), NavHudFormat.eta(13 * 60 + 1))
        assertEquals(NavHudFormat.Eta(0, 2), NavHudFormat.eta(90))
    }

    @Test
    fun etaNeverShowsLessThanOneMinute() {
        assertEquals(NavHudFormat.Eta(0, 1), NavHudFormat.eta(0))
        assertEquals(NavHudFormat.Eta(0, 1), NavHudFormat.eta(30))
        assertEquals(NavHudFormat.Eta(0, 1), NavHudFormat.eta(-5))
    }

    @Test
    fun etaSplitsHours() {
        assertEquals(NavHudFormat.Eta(1, 0), NavHudFormat.eta(3600))
        assertEquals(NavHudFormat.Eta(1, 5), NavHudFormat.eta(3600 + 5 * 60))
        assertEquals(NavHudFormat.Eta(2, 0), NavHudFormat.eta(2 * 3600 - 30))
    }

    @Test
    fun arrivalTimeIn24HourClock() {
        assertEquals("15:02", NavHudFormat.arrivalTime(LocalTime.of(14, 48), 14 * 60, true, Locale.ENGLISH))
        assertEquals("00:10", NavHudFormat.arrivalTime(LocalTime.of(23, 50), 20 * 60, true, Locale.ENGLISH))
    }

    @Test
    fun arrivalTimeIn12HourClock() {
        assertEquals("3:02 PM", NavHudFormat.arrivalTime(LocalTime.of(14, 48), 14 * 60, false, Locale.ENGLISH))
        assertEquals("12:05 AM", NavHudFormat.arrivalTime(LocalTime.of(23, 55), 10 * 60, false, Locale.ENGLISH))
    }

    @Test
    fun speedStateWithoutLimit() {
        assertEquals(SpeedState.NO_LIMIT, NavHudFormat.speedState(30.0, -1.0, false))
        assertEquals(SpeedState.NO_LIMIT, NavHudFormat.speedState(30.0, 0.0, false))
        assertEquals(SpeedState.NO_LIMIT, NavHudFormat.speedState(null, 0.0, false))
    }

    @Test
    fun speedStateAgainstLimit() {
        val limit = 80 / 3.6
        assertEquals(SpeedState.WITHIN_LIMIT, NavHudFormat.speedState(60 / 3.6, limit, false))
        assertEquals(SpeedState.WITHIN_LIMIT, NavHudFormat.speedState(limit, limit, false))
        assertEquals(SpeedState.OVER_LIMIT, NavHudFormat.speedState(90 / 3.6, limit, false))
    }

    @Test
    fun speedStateWithoutLocationIsWithinLimit() {
        assertEquals(SpeedState.WITHIN_LIMIT, NavHudFormat.speedState(null, 80 / 3.6, false))
    }

    @Test
    fun speedCameraWinsOverEverything() {
        assertEquals(SpeedState.CAMERA_AHEAD, NavHudFormat.speedState(90 / 3.6, 80 / 3.6, true))
        assertEquals(SpeedState.CAMERA_AHEAD, NavHudFormat.speedState(null, -1.0, true))
    }
}
