package app.organicmaps.my;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
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
    // The Settings screen saves its switches in the default preferences file, not in MwmApplication.prefs().
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getBoolean(context.getString(R.string.pref_nav_hud), DEFAULT);
  }
}
