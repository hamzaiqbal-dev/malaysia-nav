package app.organicmaps.help;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * The legal pages are bundled assets; this keeps the notices the licences require from being dropped by accident.
 */
public class LegalAssetsTest
{
  private static final Path ASSETS = Paths.get("src", "main", "assets");

  private static String read(String name) throws IOException
  {
    return new String(Files.readAllBytes(ASSETS.resolve(name)), StandardCharsets.UTF_8);
  }

  @Test
  public void licencesPageKeepsRequiredNotices() throws IOException
  {
    final String html = read("copyright.html");
    assertTrue(html.contains("OpenStreetMap"));
    assertTrue(html.contains("ODbL"));
    assertTrue(html.contains("Apache License, Version 2.0"));
    assertTrue(html.contains("Organic Maps"));
    assertTrue(html.contains("Hamza Iqbal"));
  }

  @Test
  public void termsCoverDrivingSafetyInBothLanguages() throws IOException
  {
    final String html = read("terms.html");
    assertTrue(html.contains("traffic laws"));
    assertTrue(html.contains("while driving"));
    assertTrue(html.contains("may be wrong"));
    assertTrue(html.contains("undang-undang jalan raya"));
    assertTrue(html.contains("semasa memandu"));
  }

  @Test
  public void privacyPolicyIsBilingualAndNamesPdpa() throws IOException
  {
    final String html = read("privacy.html");
    assertTrue(html.contains("Personal Data Protection Act 2010"));
    assertTrue(html.contains("Akta Perlindungan Data Peribadi 2010"));
    assertTrue(html.contains("lang=\"ms\""));
  }
}
