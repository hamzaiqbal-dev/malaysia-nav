package app.organicmaps.routing;

import static app.organicmaps.sdk.util.Utils.dimen;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.maplayer.MapButtonsViewModel;
import app.organicmaps.my.NavHudFormat;
import app.organicmaps.my.NavHudSettings;
import app.organicmaps.my.NavHudView;
import app.organicmaps.my.report.ReportSheet;
import app.organicmaps.sdk.Router;
import app.organicmaps.sdk.maplayer.traffic.TrafficManager;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.sdk.routing.RoutingInfo;
import app.organicmaps.sdk.util.StringUtils;
import app.organicmaps.sdk.widget.roadshield.RoadShieldUtils;
import app.organicmaps.sdk.widgets.lanes.LanesView;
import app.organicmaps.sdk.widgets.speedlimit.SpeedLimitView;
import app.organicmaps.util.UiUtils;
import app.organicmaps.util.Utils;
import app.organicmaps.util.WindowInsetUtils;
import app.organicmaps.util.WindowInsetUtils.BaselinePaddingInsetsListener;
import app.organicmaps.widget.menu.NavMenu;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import java.time.LocalTime;

public class NavigationController implements TrafficManager.TrafficCallback, NavMenu.NavMenuListener
{
  private final View mFrame;

  private final ImageView mNextTurnImage;
  private final TextView mNextTurnDistance;

  private final View mNextNextTurnFrame;
  private final ImageView mNextNextTurnImage;

  private final View mStreetFrame;
  private final TextView mNextStreet;

  @NonNull
  private final LanesView mLanesView;
  @NonNull
  private final SpeedLimitView mSpeedLimit;

  private final MapButtonsViewModel mMapButtonsViewModel;
  private final View mTopFrame;
  private final View mNextTurnContainer;

  private final NavMenu mNavMenu;
  private final NavHudView mHud;
  private final View mBottomSheetCoordinator;
  private boolean mHudEnabled;
  private int mHudEtaBarHeight;
  View.OnClickListener mOnSettingsClickListener;
  View.OnClickListener mOnVoiceSettingsClickListener;

  public NavigationController(AppCompatActivity activity, View.OnClickListener onSettingsClickListener,
                              View.OnClickListener onVoiceSettingsClickListener,
                              NavMenu.OnMenuSizeChangedListener onMenuSizeChangedListener)
  {
    mMapButtonsViewModel = new ViewModelProvider(activity).get(MapButtonsViewModel.class);

    mFrame = activity.findViewById(R.id.navigation_frame);
    mNavMenu = new NavMenu(activity, this, onMenuSizeChangedListener);
    mHud = mFrame.findViewById(R.id.nav_hud);
    mFrame.findViewById(R.id.nav_report).setOnClickListener(v -> ReportSheet.show(activity));
    mBottomSheetCoordinator = mFrame.findViewById(R.id.nav_bottom_sheet_coordinator);
    mHud.setOnEndClickListener(v -> onStopClicked());
    mHud.setOnSettingsClickListener(v -> onSettingsClicked());
    mOnSettingsClickListener = onSettingsClickListener;
    mOnVoiceSettingsClickListener = onVoiceSettingsClickListener;

    // Top frame
    mTopFrame = mFrame.findViewById(R.id.nav_top_frame);
    mTopFrame.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> mMapButtonsViewModel.setTopHeaderHeight(computeNavContentHeight()));
    View turnFrame = mTopFrame.findViewById(R.id.nav_next_turn_frame);
    mNextTurnImage = turnFrame.findViewById(R.id.turn);
    mNextTurnDistance = turnFrame.findViewById(R.id.distance);

    mNextNextTurnFrame = mTopFrame.findViewById(R.id.nav_next_next_turn_frame);
    mNextNextTurnImage = mNextNextTurnFrame.findViewById(R.id.turn);

    mStreetFrame = mTopFrame.findViewById(R.id.street_frame);
    mNextStreet = mStreetFrame.findViewById(R.id.street);

    mHud.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
      // Lanes and speed limit are laid out below the hidden street banner: stretch it to clear the HUD card.
      final int topCardBottom = mHud.getTopCardBottom();
      if (mStreetFrame.getMinimumHeight() != topCardBottom)
        mStreetFrame.setMinimumHeight(topCardBottom);
      // Keep the OSM credit, scale and routing position arrow above the ETA bar.
      final int etaBarHeight = mHud.getEtaBarHeight();
      if (mHudEtaBarHeight != etaBarHeight)
      {
        mHudEtaBarHeight = etaBarHeight;
        onMenuSizeChangedListener.OnMenuSizeChange();
      }
    });

    mLanesView = mTopFrame.findViewById(R.id.lanes);

    mSpeedLimit = mTopFrame.findViewById(R.id.nav_speed_limit);

    // Blank rectangle below the navbar that hides menu content behind it.
    final View navigationBarBackground = mFrame.findViewById(R.id.nav_bottom_sheet_nav_bar);
    final View navBottomSheet = mFrame.findViewById(R.id.nav_bottom_sheet);
    mNextTurnContainer = mFrame.findViewById(R.id.nav_next_turn_container);

    ViewCompat.setOnApplyWindowInsetsListener(mStreetFrame, BaselinePaddingInsetsListener.excludeBottom());

    ViewCompat.setOnApplyWindowInsetsListener(mTopFrame, (v, windowInsets) -> {
      final Insets safeDrawing = windowInsets.getInsets(WindowInsetUtils.TYPE_SAFE_DRAWING);
      // Pad the start edge (LTR: left, RTL: right) so the next-turn container clears side
      // cutouts and system bars regardless of layout direction.
      final boolean isRtl = v.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
      final int startInset = isRtl ? safeDrawing.right : safeDrawing.left;
      mNextTurnContainer.setPaddingRelative(startInset, mNextTurnContainer.getPaddingTop(),
                                            mNextTurnContainer.getPaddingEnd(), mNextTurnContainer.getPaddingBottom());
      return windowInsets;
    });

    ViewCompat.setOnApplyWindowInsetsListener(navigationBarBackground, (v, windowInsets) -> {
      final ViewGroup.LayoutParams lp = v.getLayoutParams();
      lp.height = windowInsets.getInsets(WindowInsetUtils.TYPE_SAFE_DRAWING).bottom;
      v.setLayoutParams(lp);
      return windowInsets;
    });

    // navBottomSheet.getWidth() is 0 on the first inset dispatch (layout hasn't run yet),
    // so mirror the width through a layout listener instead of reading it inline.
    navBottomSheet.addOnLayoutChangeListener((v, l, t, r, b, oL, oT, oR, oB) -> {
      final int width = r - l;
      final ViewGroup.LayoutParams lp = navigationBarBackground.getLayoutParams();
      if (lp.width != width)
      {
        lp.width = width;
        navigationBarBackground.setLayoutParams(lp);
      }
    });
  }

  // Height the search sheet must clear when expanded over the navigation top frame: the always
  // shown street-name frame plus the taller of the turn/speed column or the lanes strip (the two
  // overlap rather than stack, so take the max). The turn/speed column is only laid out below the
  // street frame in portrait.
  private int computeNavContentHeight()
  {
    int turnAndSpeedHeight = 0;
    if (mFrame.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
    {
      if (UiUtils.isVisible(mNextTurnContainer))
        turnAndSpeedHeight += mNextTurnContainer.getHeight();
      if (UiUtils.isVisible(mSpeedLimit))
        turnAndSpeedHeight += mSpeedLimit.getHeight();
    }
    final int lanesHeight = UiUtils.isVisible(mLanesView) ? mLanesView.getHeight() : 0;
    final int headerHeight = mHudEnabled ? mHud.getTopCardBottom() : mStreetFrame.getHeight();
    return headerHeight + Math.max(turnAndSpeedHeight, lanesHeight);
  }

  private void updateVehicle(@NonNull RoutingInfo info)
  {
    mNextTurnDistance.setText(Utils.formatDistance(mFrame.getContext(), info.distToTurn));
    mNextTurnImage.setImageResource(info.carDirection.getTurnRes(info.exitNum));

    final boolean showNextNextTurn = info.hasNextNextTurn();
    UiUtils.showIf(showNextNextTurn, mNextNextTurnFrame);
    if (showNextNextTurn)
      mNextNextTurnImage.setImageResource(info.nextCarDirection.getTurnRes());

    mLanesView.setLanes(info.lanes);

    updateSpeedLimit(info);
  }

  private void updatePedestrian(@NonNull RoutingInfo info)
  {
    mNextTurnDistance.setText(Utils.formatDistance(mFrame.getContext(), info.distToTurn));
    mNextTurnImage.setImageResource(info.pedestrianDirection.getTurnRes());
  }

  public void update(@Nullable RoutingInfo info)
  {
    if (info == null)
      return;

    if (Router.get() == Router.Pedestrian)
      updatePedestrian(info);
    else
      updateVehicle(info);

    updateStreetView(info);
    mNavMenu.update(info);
    if (mHudEnabled)
      updateHud(info);
  }

  // The Sampai HUD replaces the street banner, next-turn column and bottom menu while it is enabled.
  private void applyHudMode()
  {
    mHudEnabled = NavHudSettings.isEnabled(mFrame.getContext());
    UiUtils.showIf(mHudEnabled, mHud);
    UiUtils.showIf(!mHudEnabled, mBottomSheetCoordinator, mNextTurnContainer);
    if (mHudEnabled)
      UiUtils.invisible(mStreetFrame);
    else
      mStreetFrame.setMinimumHeight(0);
  }

  /** Height the HUD's ETA bar covers at the bottom of the map, 0 when the HUD is not shown. */
  public int getHudBottomHeight()
  {
    return mHudEnabled && UiUtils.isVisible(mFrame) ? mHudEtaBarHeight : 0;
  }

  private void updateHud(@NonNull RoutingInfo info)
  {
    final Context context = mFrame.getContext();
    final int turnRes = Router.get() == Router.Pedestrian ? info.pedestrianDirection.getTurnRes()
                                                          : info.carDirection.getTurnRes(info.exitNum);
    mHud.setTurn(turnRes, info.distToTurn.toString(context), info.nextStreet);

    final Resources res = context.getResources();
    final String arrival =
        NavHudFormat.arrival(LocalTime.now(), info.totalTimeInSeconds, DateFormat.is24HourFormat(context));
    mHud.setEta(
        NavHudFormat.timeLeft(info.totalTimeInSeconds, res.getString(R.string.hour), res.getString(R.string.minute)),
        info.distToTarget.toString(context) + " · " + arrival);
  }

  private void updateStreetView(@NonNull RoutingInfo info)
  {
    boolean hasStreet = !TextUtils.isEmpty(info.nextStreet);
    // Sic: don't use UiUtils.showIf() here because View.GONE breaks layout
    // https://github.com/organicmaps/organicmaps/issues/3732
    UiUtils.visibleIf(hasStreet && !mHudEnabled, mStreetFrame);
    if (!TextUtils.isEmpty(info.nextStreet))
      mNextStreet.setText(RoadShieldUtils.createStreetTextWithShields(info.nextStreet, info.nextStreetRoadShields,
                                                                      mNextStreet.getTextSize()));
    int margin = dimen(mFrame.getContext(), R.dimen.nav_frame_padding);
    if (mHudEnabled)
      margin += mHud.getTopCardBottom();
    else if (hasStreet)
      margin += mStreetFrame.getHeight();
    mMapButtonsViewModel.setTopButtonsMarginTop(margin);
  }

  public void show(boolean show)
  {
    if (show && !UiUtils.isVisible(mFrame))
    {
      applyHudMode();
      collapseNavMenu();
      // Seed the panel from the already-built route so it isn't empty until the first GPS fix arrives.
      update(RoutingController.get().getCachedRoutingInfo());
    }
    UiUtils.showIf(show, mFrame);
    if (!show)
      mMapButtonsViewModel.setTopHeaderHeight(0);
  }

  public boolean isNavMenuCollapsed()
  {
    return mNavMenu.getBottomSheetState() == BottomSheetBehavior.STATE_COLLAPSED;
  }

  public boolean isNavMenuHidden()
  {
    return mNavMenu.getBottomSheetState() == BottomSheetBehavior.STATE_HIDDEN;
  }

  public void collapseNavMenu()
  {
    mNavMenu.collapseNavBottomSheet();
  }

  public void refresh()
  {
    mNavMenu.refreshTts();
  }

  @Override
  public void onEnabled()
  {
    // mNavMenu.refreshTraffic();
  }

  @Override
  public void onDisabled()
  {
    // mNavMenu.refreshTraffic();
  }

  @Override
  public void onWaitingData()
  {
    // no op
  }

  @Override
  public void onOutdated()
  {
    // no op
  }

  @Override
  public void onNoData()
  {
    // no op
  }

  @Override
  public void onNetworkError()
  {
    // no op
  }

  @Override
  public void onExpiredData()
  {
    // no op
  }

  @Override
  public void onExpiredApp()
  {
    // no op
  }

  @Override
  public void onSettingsClicked()
  {
    mOnSettingsClickListener.onClick(null);
  }

  @Override
  public void onTtsVoiceSettingsClicked()
  {
    mOnVoiceSettingsClickListener.onClick(null);
  }

  @Override
  public void onStopClicked()
  {
    RoutingController.get().cancel();
  }

  private void updateSpeedLimit(@NonNull final RoutingInfo info)
  {
    final Location location = MwmApplication.from(mFrame.getContext()).getLocationHelper().getSavedLocation();
    final boolean speedLimitExceeded = location != null && info.speedLimitMps < location.getSpeed();
    mSpeedLimit.setSpeedLimit(StringUtils.nativeFormatSpeed(info.speedLimitMps), speedLimitExceeded);
  }
}
