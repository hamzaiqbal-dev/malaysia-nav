package app.organicmaps.sdk.my;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Malaysia Nav tolls: toll booths of the built route, taken from the map data.
public final class MyTolls
{
  private MyTolls() {}

  // Route geometry as [lat0, lon0, lat1, lon1, ...], or null without a valid route. Main thread only.
  @Nullable
  public static native double[] nativeGetRoutePolyline();

  // Named toll booths within maxDistanceM of the polyline, ordered from the route start. Any thread.
  @NonNull
  public static native TollBooth[] nativeFindTollBooths(@NonNull double[] latLons, double maxDistanceM);
}
