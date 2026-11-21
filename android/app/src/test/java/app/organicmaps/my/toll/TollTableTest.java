package app.organicmaps.my.toll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class TollTableTest
{
  private static final String HEADER =
      "plaza,highway,lat,lon,class1_rm,class2_rm,class3_rm,class4_rm,class5_rm,source_url,as_of_date\n";

  private static TollTable parse(String body) throws IOException
  {
    return TollTable.parse(new StringReader("# comment\n" + HEADER + body));
  }

  @Test
  public void parsesKnownAndTbcRates() throws IOException
  {
    final TollTable t = parse("A,E1,3.1,101.6,1.50,TBC,3.00,0.80,TBC,http://x,2026-01-01\n\n# skip\n"
                              + "B,TBC,2.5,102.0,TBC,TBC,TBC,TBC,TBC,http://y,2026-01-02\n");
    assertEquals(2, t.plazas().size());
    final TollPlaza a = t.plazas().get(0);
    assertEquals("A", a.name());
    assertEquals("E1", a.highway());
    assertEquals(3.1, a.lat(), 1e-9);
    assertEquals(new BigDecimal("1.50"), a.rate(1));
    assertNull(a.rate(2));
    assertEquals(new BigDecimal("0.80"), a.rate(4));
    assertEquals("http://x", a.sourceUrl());
    assertEquals("2026-01-01", a.asOfDate());
    assertNull(t.plazas().get(1).highway());
    assertEquals(1, t.pricedCount());
  }

  @Test
  public void rejectsBadRows()
  {
    assertThrows(IOException.class, () -> parse("A,E1,3.1,101.6,1.50\n"));
    assertThrows(IOException.class, () -> parse("A,E1,3.1,101.6,x,1,1,1,1,u,d\n"));
    assertThrows(IOException.class, () -> parse("A,E1,3.1,101.6,-1,1,1,1,1,u,d\n"));
    assertThrows(IOException.class, () -> parse("A,E1,north,101.6,1,1,1,1,1,u,d\n"));
  }

  @Test
  public void rateRejectsUnknownClass() throws IOException
  {
    final TollPlaza p = parse("A,E1,3.1,101.6,1,1,1,1,1,u,d\n").plazas().get(0);
    assertThrows(IllegalArgumentException.class, () -> p.rate(0));
    assertThrows(IllegalArgumentException.class, () -> p.rate(6));
  }

  @Test
  public void shippedDataFileIsValid() throws IOException
  {
    // Unit tests run in android/app.
    try (FileReader r = new FileReader("../../data/my/tolls.csv", StandardCharsets.UTF_8))
    {
      final TollTable t = TollTable.parse(r);
      assertTrue(t.plazas().size() > 50);
      for (final TollPlaza p : t.plazas())
      {
        // Malaysia bounding box (peninsula and Borneo).
        assertTrue(p.name(), p.lat() > 0.8 && p.lat() < 7.5);
        assertTrue(p.name(), p.lon() > 99.5 && p.lon() < 119.5);
        assertTrue(p.name(), p.sourceUrl().startsWith("https://"));
      }
    }
  }
}
