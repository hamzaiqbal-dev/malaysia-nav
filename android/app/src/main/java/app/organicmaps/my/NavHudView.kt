package app.organicmaps.my

import android.content.Context
import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import app.organicmaps.R
import app.organicmaps.my.NavHudFormat.SpeedState
import app.organicmaps.sdk.routing.RoutingInfo
import app.organicmaps.sdk.sound.TtsPlayer
import app.organicmaps.sdk.util.Distance
import app.organicmaps.sdk.util.StringUtils
import app.organicmaps.sdk.widget.roadshield.RoadShieldUtils
import app.organicmaps.sdk.widgets.lanes.LanesView
import app.organicmaps.sdk.widgets.speedlimit.SpeedLimitView
import app.organicmaps.util.WindowInsetUtils.BaselinePaddingInsetsListener
import app.organicmaps.widget.menu.NavMenu
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.LocalTime
import java.util.Locale

/**
 * Malaysia Nav navigation HUD: turn banner, lane strip, speed + limit and the ETA sheet.
 * Overlays the map during navigation in place of the stock top frame and bottom sheet
 * (see `R.bool.my_nav_hud_enabled`). Empty areas let touches through to the map.
 */
class NavHudView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val turnBanner: View
    private val turnIcon: ImageView
    private val turnDistance: TextView
    private val turnStreet: TextView
    private val thenFrame: View
    private val thenIcon: ImageView
    private val lanes: LanesView
    private val speedPill: View
    private val speedValue: TextView
    private val speedUnits: TextView
    private val speedLimit: SpeedLimitView
    private val etaSheet: View
    private val etaText: TextView
    private val etaSubtitle: TextView
    private val voiceButton: ImageView
    private val progress: LinearProgressIndicator

    private val ttsStateListener = Runnable { refreshVoiceState() }
    private var lastTopContentHeight = -1
    private var lastBottomSheetHeight = -1

    /** Receives the ETA sheet buttons: end navigation, settings and voice settings. */
    var listener: NavMenu.NavMenuListener? = null

    /** Called (after layout) whenever [topContentHeight] or [bottomSheetHeight] change. */
    var onContentHeightChanged: Runnable? = null

    /** Height of the turn banner plus lane strip that the search sheet must clear, status bar excluded. */
    val topContentHeight: Int
        get() = if (lanes.isVisible) lanes.bottom else turnBanner.bottom

    /** Height of the ETA sheet, navigation bar inset included, that map widgets must stay above. */
    val bottomSheetHeight: Int
        get() = etaSheet.height

    init {
        LayoutInflater.from(context).inflate(R.layout.my_nav_hud, this, true)
        turnBanner = findViewById(R.id.my_turn_banner)
        turnIcon = findViewById(R.id.my_turn_icon)
        turnDistance = findViewById(R.id.my_turn_distance)
        turnStreet = findViewById(R.id.my_turn_street)
        thenFrame = findViewById(R.id.my_then_frame)
        thenIcon = findViewById(R.id.my_then_icon)
        lanes = findViewById(R.id.my_lanes)
        speedPill = findViewById(R.id.my_speed_pill)
        speedValue = findViewById(R.id.my_speed_value)
        speedUnits = findViewById(R.id.my_speed_units)
        speedLimit = findViewById(R.id.my_speed_limit)
        etaSheet = findViewById(R.id.my_eta_sheet)
        etaText = findViewById(R.id.my_eta)
        etaSubtitle = findViewById(R.id.my_eta_subtitle)
        voiceButton = findViewById(R.id.my_voice)
        progress = findViewById(R.id.my_progress)

        // Status bar and display cutouts pad the whole HUD; the ETA sheet alone extends under the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(this, BaselinePaddingInsetsListener.excludeBottom())
        ViewCompat.setOnApplyWindowInsetsListener(etaSheet, BaselinePaddingInsetsListener.onlyBottom())

        voiceButton.setOnClickListener { onVoiceClicked() }
        findViewById<View>(R.id.my_settings).setOnClickListener { listener?.onSettingsClicked() }
        findViewById<View>(R.id.my_end).setOnClickListener { listener?.onStopClicked() }
    }

    /**
     * @param speedMps current speed, or null until the first location fix.
     */
    fun update(info: RoutingInfo, isPedestrian: Boolean, speedMps: Double?) {
        updateTurn(info, isPedestrian)
        updateSpeed(info, speedMps)
        updateEta(info)
    }

    fun refreshVoiceState() {
        val (iconRes, colorRes) = when (TtsPlayer.getState()) {
            TtsPlayer.State.READY_ON -> R.drawable.ic_voice_on to R.color.my_route
            TtsPlayer.State.READY_OFF -> R.drawable.ic_voice_off to R.color.my_ink
            else -> R.drawable.ic_voice_off to R.color.my_muted
        }
        voiceButton.setImageResource(iconRes)
        voiceButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        TtsPlayer.addStateChangedListener(ttsStateListener)
        refreshVoiceState()
    }

    override fun onDetachedFromWindow() {
        TtsPlayer.removeStateChangedListener(ttsStateListener)
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // Re-shown at the same size, the next layout pass must re-announce heights the activity reset meanwhile.
        if (visibility == View.VISIBLE) {
            lastTopContentHeight = -1
            lastBottomSheetHeight = -1
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val topHeight = topContentHeight
        val bottomHeight = bottomSheetHeight
        if (topHeight == lastTopContentHeight && bottomHeight == lastBottomSheetHeight) return
        lastTopContentHeight = topHeight
        lastBottomSheetHeight = bottomHeight
        // Listeners resize other views; posting keeps that out of this layout pass.
        onContentHeightChanged?.let { post(it) }
    }

    private fun updateTurn(info: RoutingInfo, isPedestrian: Boolean) {
        val turnRes = if (isPedestrian) {
            info.pedestrianDirection.turnRes
        } else {
            info.carDirection.getTurnRes(info.exitNum)
        }
        turnIcon.setImageResource(turnRes)
        turnDistance.text = distanceText(info.distToTurn)

        val street = info.nextStreet
        val hasStreet = !street.isNullOrEmpty()
        turnStreet.isVisible = hasStreet
        if (hasStreet) {
            turnStreet.text =
                RoadShieldUtils.createStreetTextWithShields(street, info.nextStreetRoadShields, turnStreet.textSize)
        }

        val showThen = !isPedestrian && info.hasNextNextTurn()
        thenFrame.isVisible = showThen
        if (showThen) thenIcon.setImageResource(info.nextCarDirection.turnRes)

        lanes.setLanes(if (isPedestrian) null else info.lanes)
    }

    private fun updateSpeed(info: RoutingInfo, speedMps: Double?) {
        val state = NavHudFormat.speedState(speedMps, info.speedLimitMps, info.isSpeedCamLimitExceeded)

        speedPill.isVisible = speedMps != null
        if (speedMps != null) {
            val speedAndUnits = StringUtils.nativeFormatSpeedAndUnits(speedMps)
            speedValue.text = speedAndUnits.first
            speedUnits.text = speedAndUnits.second
        }
        val cameraAhead = state == SpeedState.CAMERA_AHEAD
        speedPill.isActivated = cameraAhead
        val valueColor = when (state) {
            SpeedState.CAMERA_AHEAD -> R.color.my_on_banner
            SpeedState.OVER_LIMIT -> R.color.my_danger
            else -> R.color.my_ink
        }
        speedValue.setTextColor(ContextCompat.getColor(context, valueColor))
        val unitsColor = if (cameraAhead) R.color.my_on_banner else R.color.my_muted
        speedUnits.setTextColor(ContextCompat.getColor(context, unitsColor))

        val hasLimit = info.speedLimitMps > 0.0
        speedLimit.isVisible = hasLimit
        if (hasLimit) {
            val alert = state != SpeedState.WITHIN_LIMIT
            speedLimit.setSpeedLimit(StringUtils.nativeFormatSpeed(info.speedLimitMps), alert)
        }
    }

    private fun updateEta(info: RoutingInfo) {
        val eta = NavHudFormat.eta(info.totalTimeInSeconds)
        etaText.text = when {
            eta.hours == 0 -> resources.getString(R.string.my_nav_eta_minutes, eta.minutes)
            eta.minutes == 0 -> resources.getString(R.string.my_nav_eta_hours, eta.hours)
            else -> resources.getString(R.string.my_nav_eta_hours_minutes, eta.hours, eta.minutes)
        }
        val arrival = NavHudFormat.arrivalTime(
            LocalTime.now(),
            info.totalTimeInSeconds,
            DateFormat.is24HourFormat(context),
            Locale.getDefault(),
        )
        val distance = info.distToTarget.toString(context)
        etaSubtitle.text = resources.getString(R.string.my_nav_eta_subtitle, distance, arrival)
        progress.setProgressCompat(info.completionPercent.toInt(), true)
    }

    private fun distanceText(distance: Distance): CharSequence {
        if (!distance.isValid) return ""
        val text = SpannableStringBuilder(distance.mDistanceStr)
            .append(NON_BREAKING_SPACE)
            .append(distance.getUnitsStr(context))
        val unitsStart = distance.mDistanceStr.length
        text.setSpan(RelativeSizeSpan(UNITS_SCALE), unitsStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }

    private fun onVoiceClicked() {
        when (TtsPlayer.getState()) {
            TtsPlayer.State.READY_ON, TtsPlayer.State.READY_OFF -> TtsPlayer.setEnabled(!TtsPlayer.isEnabled())
            TtsPlayer.State.UNAVAILABLE, TtsPlayer.State.NEEDS_LANGUAGE -> listener?.onTtsVoiceSettingsClicked()
            TtsPlayer.State.INITIALIZING -> Unit
        }
    }

    private companion object {
        const val NON_BREAKING_SPACE = ' '
        const val UNITS_SCALE = 0.55f
    }
}
