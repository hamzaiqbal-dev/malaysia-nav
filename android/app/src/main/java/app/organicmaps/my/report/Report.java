package app.organicmaps.my.report;

import androidx.annotation.NonNull;

public record Report(long timeMillis, @NonNull ReportType type, double lat, double lon)
{
}
