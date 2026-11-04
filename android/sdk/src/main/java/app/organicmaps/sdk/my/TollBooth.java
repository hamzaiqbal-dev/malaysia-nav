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
  // OpenStreetMap numbers of the roads at the booth, ';'-separated; empty when they have none.
  @NonNull
  public final String mRoadRefs;
  public final double mLat;
  public final double mLon;
  // Distance from the route start to the booth, in meters.
  public final double mDistanceAlongRouteM;

  private TollBooth(@NonNull String name, @NonNull String roadRefs, double lat, double lon, double distanceAlongRouteM)
  {
    mName = name;
    mRoadRefs = roadRefs;
    mLat = lat;
    mLon = lon;
    mDistanceAlongRouteM = distanceAlongRouteM;
  }
}
