package app.organicmaps.sdk.my;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

// Called from JNI.
@Keep
@SuppressWarnings("unused")
public final class TollBooth
{
  @NonNull
  public final String mName;
  public final double mLat;
  public final double mLon;
  // Distance from the route start to the booth, in meters.
  public final double mDistanceAlongRouteM;

  private TollBooth(@NonNull String name, double lat, double lon, double distanceAlongRouteM)
  {
    mName = name;
    mLat = lat;
    mLon = lon;
    mDistanceAlongRouteM = distanceAlongRouteM;
  }
}
