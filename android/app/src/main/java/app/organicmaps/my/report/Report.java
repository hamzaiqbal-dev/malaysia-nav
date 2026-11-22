package app.organicmaps.my.report;

import androidx.annotation.NonNull;

public record Report(long timeMillis, @NonNull ReportType type, double lat, double lon)
{
  // The last fix is kept after GPS is lost (e.g. in a tunnel), so an older one is not where the driver is now.
  public static final long MAX_FIX_AGE_MS = 30_000;

  public static boolean isFixFresh(long fixAgeMillis)
  {
    return fixAgeMillis >= 0 && fixAgeMillis <= MAX_FIX_AGE_MS;
  }
}
