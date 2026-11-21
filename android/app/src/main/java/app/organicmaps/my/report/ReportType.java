package app.organicmaps.my.report;

import androidx.annotation.NonNull;

/**
 * Kinds of road reports. The key is what gets stored on disk, so never rename it.
 */
public enum ReportType
{
  JAM("jam"),
  ACCIDENT("accident"),
  POLICE("police"),
  AES_CAMERA("aes_camera"),
  POTHOLE("pothole"),
  FLOOD("flood"),
  ROAD_CLOSED("road_closed");

  private final String mKey;

  ReportType(@NonNull String key)
  {
    mKey = key;
  }

  @NonNull
  public String key()
  {
    return mKey;
  }

  @NonNull
  public static ReportType fromKey(@NonNull String key)
  {
    for (final ReportType t : values())
      if (t.mKey.equals(key))
        return t;
    throw new IllegalArgumentException("Unknown report type: " + key);
  }
}
