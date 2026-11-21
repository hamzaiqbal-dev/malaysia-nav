package app.organicmaps.my.report;

import androidx.annotation.NonNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Device-only storage of reports, one line per report: timeMillis,type,lat,lon. Nothing leaves the phone.
 */
public final class ReportStore
{
  private final File mFile;

  public ReportStore(@NonNull File file)
  {
    mFile = file;
  }

  public synchronized void add(@NonNull Report report) throws IOException
  {
    final String line = String.format(Locale.ROOT, "%d,%s,%.6f,%.6f%n", report.timeMillis(), report.type().key(),
                                      report.lat(), report.lon());
    try (FileWriter w = new FileWriter(mFile, StandardCharsets.UTF_8, true))
    {
      w.write(line);
    }
  }

  @NonNull
  public synchronized List<Report> readAll() throws IOException
  {
    final List<Report> result = new ArrayList<>();
    if (!mFile.exists())
      return result;
    try (BufferedReader in = new BufferedReader(new FileReader(mFile, StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = in.readLine()) != null)
      {
        if (line.isBlank())
          continue;
        final String[] f = line.split(",");
        result.add(new Report(Long.parseLong(f[0]), ReportType.fromKey(f[1]), Double.parseDouble(f[2]),
                              Double.parseDouble(f[3])));
      }
    }
    return result;
  }
}
