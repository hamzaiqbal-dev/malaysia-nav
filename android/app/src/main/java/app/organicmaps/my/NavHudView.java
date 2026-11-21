package app.organicmaps.my;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import app.organicmaps.R;
import app.organicmaps.util.WindowInsetUtils;

/**
 * Waze-style navigation HUD: a turn instruction card at the top and an ETA bar at the bottom.
 */
public class NavHudView extends FrameLayout
{
  private final View mTopCard;
  private final ImageView mTurn;
  private final TextView mDistance;
  private final TextView mStreet;
  private final View mEtaBar;
  private final TextView mTimeLeft;
  private final TextView mSummary;
  private final View mSettings;
  private final View mEnd;

  public NavHudView(@NonNull Context context)
  {
    this(context, null);
  }

  public NavHudView(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.view_nav_hud, this);
    mTopCard = findViewById(R.id.hud_top_card);
    mTurn = findViewById(R.id.hud_turn);
    mDistance = findViewById(R.id.hud_distance);
    mStreet = findViewById(R.id.hud_street);
    mEtaBar = findViewById(R.id.hud_eta_bar);
    mTimeLeft = findViewById(R.id.hud_time_left);
    mSummary = findViewById(R.id.hud_summary);
    mSettings = findViewById(R.id.hud_settings);
    mEnd = findViewById(R.id.hud_end);

    // Keep the cards clear of the status bar, cutouts and the navigation bar.
    ViewCompat.setOnApplyWindowInsetsListener(this, (v, windowInsets) -> {
      final Insets insets = windowInsets.getInsets(WindowInsetUtils.TYPE_SAFE_DRAWING);
      setPadding(insets.left, insets.top, insets.right, 0);
      mEtaBar.setPadding(mEtaBar.getPaddingLeft(), mEtaBar.getPaddingTop(), mEtaBar.getPaddingRight(),
                         insets.bottom + getResources().getDimensionPixelSize(R.dimen.margin_base));
      return windowInsets;
    });
  }

  public void setTurn(@DrawableRes int turnRes, @NonNull CharSequence distance, @Nullable CharSequence street)
  {
    mTurn.setImageResource(turnRes);
    mDistance.setText(distance);
    mStreet.setText(street);
    mStreet.setVisibility(street == null || street.length() == 0 ? View.GONE : View.VISIBLE);
  }

  public void setEta(@NonNull CharSequence timeLeft, @NonNull CharSequence summary)
  {
    mTimeLeft.setText(timeLeft);
    mSummary.setText(summary);
  }

  public void setOnEndClickListener(@Nullable OnClickListener listener)
  {
    mEnd.setOnClickListener(listener);
  }

  public void setOnSettingsClickListener(@Nullable OnClickListener listener)
  {
    mSettings.setOnClickListener(listener);
  }

  /** Height of the top card including its margin, used to place the map buttons below it. */
  public int getTopCardBottom()
  {
    return mTopCard.getBottom();
  }
}
