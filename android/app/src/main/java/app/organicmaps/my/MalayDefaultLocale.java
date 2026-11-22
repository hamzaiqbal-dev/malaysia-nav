package app.organicmaps.my;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

/**
 * Makes Bahasa Melayu the default UI language on devices set to Malaysia.
 */
public final class MalayDefaultLocale
{
  private static final String MALAY = "ms";
  private static final String MALAYSIA = "MY";

  private MalayDefaultLocale() {}

  /**
   * Malay devices get the "ms" resources from the system anyway; English devices in Malaysia are switched to
   * Malay. Other languages (Chinese, Tamil, ...) are left alone.
   */
  public static boolean shouldUseMalay(@Nullable String language, @Nullable String country)
  {
    if (MALAY.equals(language))
      return true;
    return "en".equals(language) && MALAYSIA.equals(country);
  }

  /**
   * Call once from Application.onCreate(). A language picked by the user in the system per-app language
   * settings is never overridden.
   */
  public static void apply()
  {
    if (!AppCompatDelegate.getApplicationLocales().isEmpty())
      return;

    @Nullable
    final Locale device = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
    if (device != null && shouldUseMalay(device.getLanguage(), device.getCountry()))
      AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(MALAY));
  }
}
