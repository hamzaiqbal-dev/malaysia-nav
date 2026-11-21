package app.organicmaps.my;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;

/**
 * The "Sampai navigation HUD" switch in Settings. On by default.
 */
public final class NavHudSettings
{
  public static final boolean DEFAULT = true;

  private NavHudSettings() {}

  public static boolean isEnabled(@NonNull Context context)
  {
    final SharedPreferences prefs = MwmApplication.prefs(context);
    return prefs.getBoolean(context.getString(R.string.pref_nav_hud), DEFAULT);
  }
}
