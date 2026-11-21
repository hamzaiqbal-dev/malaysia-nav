package app.organicmaps.my;

import androidx.annotation.NonNull;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Text formatting for {@link NavHudView}, kept free of Android classes so it can be unit tested.
 */
public final class NavHudFormat
{
  private NavHudFormat() {}

  /**
   * Remaining trip time as "1 h 5 min" or "12 min". Anything under a minute is shown as "1 min".
   */
  @NonNull
  public static String timeLeft(int seconds, @NonNull String hourUnit, @NonNull String minuteUnit)
  {
    final long total = Math.max(1, TimeUnit.SECONDS.toMinutes(Math.max(0, seconds)));
    final long hours = total / 60;
    final long minutes = total % 60;
    if (hours == 0)
      return minutes + " " + minuteUnit;
    if (minutes == 0)
      return hours + " " + hourUnit;
    return hours + " " + hourUnit + " " + minutes + " " + minuteUnit;
  }

  /**
   * Local clock time of arrival, e.g. "14:35" or "2:35 PM".
   */
  @NonNull
  public static String arrival(@NonNull LocalTime now, int seconds, boolean is24Hour)
  {
    final String pattern = is24Hour ? "HH:mm" : "h:mm a";
    return now.plusSeconds(Math.max(0, seconds)).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()));
  }
}
