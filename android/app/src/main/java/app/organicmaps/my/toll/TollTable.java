package app.organicmaps.my.toll;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Loader for data/my/tolls.csv: a header row, comment lines starting with a hash, no quoted fields.
 * Columns: plaza, highway, lat, lon, class1_rm..class5_rm, source_url, as_of_date.
 */
public final class TollTable
{
  private static final String TBC = "TBC";
  private static final int COLUMNS = 4 + TollPlaza.CLASS_COUNT + 2;

  private final List<TollPlaza> mPlazas;

  private TollTable(@NonNull List<TollPlaza> plazas)
  {
    mPlazas = Collections.unmodifiableList(plazas);
  }

  @NonNull
  public List<TollPlaza> plazas()
  {
    return mPlazas;
  }

  /** Number of plazas whose class-1 rate is known. */
  public long pricedCount()
  {
    return mPlazas.stream().filter(p -> p.rate(1) != null).count();
  }

  @NonNull
  public static TollTable parse(@NonNull Reader reader) throws IOException
  {
    final BufferedReader in = new BufferedReader(reader);
    final List<TollPlaza> plazas = new ArrayList<>();
    boolean header = true;
    String line;
    int lineNo = 0;
    while ((line = in.readLine()) != null)
    {
      lineNo++;
      if (line.isBlank() || line.startsWith("#"))
        continue;
      if (header)
      {
        header = false;
        continue;
      }
      final String[] f = line.split(",", -1);
      if (f.length != COLUMNS)
        throw new IOException("tolls.csv line " + lineNo + ": expected " + COLUMNS + " columns, got " + f.length);
      final BigDecimal[] rates = new BigDecimal[TollPlaza.CLASS_COUNT];
      for (int i = 0; i < rates.length; i++)
        rates[i] = parseRate(f[4 + i], lineNo);
      final String highway = TBC.equals(f[1]) ? null : f[1];
      try
      {
        plazas.add(new TollPlaza(f[0], highway, Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                                 Collections.unmodifiableList(Arrays.asList(rates)), f[4 + rates.length],
                                 f[5 + rates.length]));
      }
      catch (NumberFormatException e)
      {
        throw new IOException("tolls.csv line " + lineNo + ": bad coordinate", e);
      }
    }
    return new TollTable(plazas);
  }

  @Nullable
  private static BigDecimal parseRate(@NonNull String s, int lineNo) throws IOException
  {
    if (TBC.equals(s))
      return null;
    final BigDecimal v;
    try
    {
      v = new BigDecimal(s);
    }
    catch (NumberFormatException e)
    {
      throw new IOException("tolls.csv line " + lineNo + ": bad rate '" + s + "'", e);
    }
    if (v.signum() < 0)
      throw new IOException("tolls.csv line " + lineNo + ": negative rate " + s);
    return v;
  }
}
