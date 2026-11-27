package app.organicmaps.my;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;
import java.util.Locale;

/**
 * Makes Bahasa Melayu the default UI language on devices set to Malaysia, and backs the app language setting.
 */
public final class MalayDefaultLocale
{
  private static final String MALAY = "ms";
  private static final String MALAYSIA = "MY";
  private static final String KEY_APPLIED = "MalayDefaultLocaleApplied";

  /** Languages offered in Settings; "" follows the system. */
  private static final String[] LANGUAGE_TAGS = {"", MALAY, "en", "zh"};

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
   * Call once from Application.onCreate(). Switches to Malay on the first start only, so a language picked
   * later in Settings or in the system per-app language settings is never overridden.
   */
  public static void apply(@NonNull Context context)
  {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (prefs.getBoolean(KEY_APPLIED, false))
      return;
    prefs.edit().putBoolean(KEY_APPLIED, true).apply();

    if (!AppCompatDelegate.getApplicationLocales().isEmpty())
      return;

    @Nullable
    final Locale device = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0);
    if (device != null && shouldUseMalay(device.getLanguage(), device.getCountry()))
      AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(MALAY));
  }

  /** Language tags for the Settings list: the offered ones, plus the current one if it was set elsewhere. */
  @NonNull
  public static String[] getLanguageTags()
  {
    final String current = getAppLanguageTag();
    for (String tag : LANGUAGE_TAGS)
    {
      if (tag.equals(current))
        return LANGUAGE_TAGS.clone();
    }
    final String[] tags = new String[LANGUAGE_TAGS.length + 1];
    System.arraycopy(LANGUAGE_TAGS, 0, tags, 0, LANGUAGE_TAGS.length);
    tags[LANGUAGE_TAGS.length] = current;
    return tags;
  }

  /** The language's name in that language, e.g. "Melayu", "English", "中文". */
  @NonNull
  public static String getLanguageName(@NonNull String tag)
  {
    final Locale locale = Locale.forLanguageTag(tag);
    final String name = locale.getDisplayLanguage(locale);
    if (name.isEmpty())
      return tag;
    return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
  }

  /** The app's own UI language, or "" when it follows the system. */
  @NonNull
  public static String getAppLanguageTag()
  {
    final LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
    @Nullable
    final Locale locale = locales.isEmpty() ? null : locales.get(0);
    return locale == null ? "" : locale.getLanguage();
  }

  /** Sets the app's UI language; "" follows the system. Recreates the open activities. */
  public static void setAppLanguage(@NonNull String tag)
  {
    AppCompatDelegate.setApplicationLocales(tag.isEmpty() ? LocaleListCompat.getEmptyLocaleList()
                                                          : LocaleListCompat.forLanguageTags(tag));
  }
}
