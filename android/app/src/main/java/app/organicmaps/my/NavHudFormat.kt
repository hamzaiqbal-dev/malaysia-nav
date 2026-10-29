package app.organicmaps.my

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatting rules of the navigation HUD, kept free of Android types so they are unit-testable.
 */
object NavHudFormat {
    /** Remaining travel time split for display. */
    data class Eta(val hours: Int, val minutes: Int)

    enum class SpeedState {
        /** No speed limit is known for the current road. */
        NO_LIMIT,
        WITHIN_LIMIT,
        OVER_LIMIT,

        /** Over the limit with a speed camera ahead: the most urgent state. */
        CAMERA_AHEAD,
    }

    /**
     * Rounds the remaining time up to whole minutes (never below one) so the HUD never promises
     * an arrival earlier than the route can deliver.
     */
    fun eta(totalSeconds: Int): Eta {
        val totalMinutes = maxOf(1, (totalSeconds.coerceAtLeast(0) + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE)
        return Eta(totalMinutes / MINUTES_PER_HOUR, totalMinutes % MINUTES_PER_HOUR)
    }

    /** Clock time of arrival, e.g. "15:02" or "3:02 PM" depending on [use24Hours]. */
    fun arrivalTime(now: LocalTime, remainingSeconds: Int, use24Hours: Boolean, locale: Locale): String {
        val pattern = if (use24Hours) "HH:mm" else "h:mm a"
        return now.plusSeconds(remainingSeconds.toLong()).format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /**
     * @param speedMps current speed, or null when there is no location fix yet.
     * @param limitMps speed limit of the current road; negative or zero when unknown.
     */
    fun speedState(speedMps: Double?, limitMps: Double, cameraLimitExceeded: Boolean): SpeedState = when {
        cameraLimitExceeded -> SpeedState.CAMERA_AHEAD
        limitMps <= 0.0 -> SpeedState.NO_LIMIT
        speedMps != null && speedMps > limitMps -> SpeedState.OVER_LIMIT
        else -> SpeedState.WITHIN_LIMIT
    }

    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
}
