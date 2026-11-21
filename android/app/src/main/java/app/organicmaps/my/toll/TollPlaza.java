package app.organicmaps.my.toll;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

/**
 * One toll plaza from data/my/tolls.csv. A rate that is still "TBC" in the file is null here.
 */
public record TollPlaza(@NonNull String name, @Nullable String highway, double lat, double lon,
                        @NonNull List<BigDecimal> ratesRm, @NonNull String sourceUrl, @NonNull String asOfDate)
{
  public static final int CLASS_COUNT = 5;

  /** Rate in RM for vehicle class 1..5, or null if not known yet. */
  @Nullable
  public BigDecimal rate(int vehicleClass)
  {
    if (vehicleClass < 1 || vehicleClass > CLASS_COUNT)
      throw new IllegalArgumentException("Vehicle class must be 1.." + CLASS_COUNT + ": " + vehicleClass);
    return ratesRm.get(vehicleClass - 1);
  }
}
