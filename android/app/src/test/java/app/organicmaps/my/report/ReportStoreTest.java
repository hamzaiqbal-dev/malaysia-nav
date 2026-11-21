package app.organicmaps.my.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReportStoreTest
{
  @Rule
  public TemporaryFolder mTmp = new TemporaryFolder();

  @Test
  public void emptyWhenNoFile() throws IOException
  {
    assertTrue(new ReportStore(new File(mTmp.getRoot(), "none.csv")).readAll().isEmpty());
  }

  @Test
  public void appendsAndReadsBackInOrder() throws IOException
  {
    final ReportStore s = new ReportStore(mTmp.newFile("r.csv"));
    s.add(new Report(1000L, ReportType.JAM, 3.139, 101.6869));
    s.add(new Report(2000L, ReportType.AES_CAMERA, -1.5, 110.25));
    final List<Report> all = s.readAll();
    assertEquals(2, all.size());
    assertEquals(ReportType.JAM, all.get(0).type());
    assertEquals(1000L, all.get(0).timeMillis());
    assertEquals(101.6869, all.get(0).lon(), 1e-6);
    assertEquals(ReportType.AES_CAMERA, all.get(1).type());
    assertEquals(-1.5, all.get(1).lat(), 1e-6);
  }

  @Test
  public void survivesNewStoreInstance() throws IOException
  {
    final File f = mTmp.newFile("r.csv");
    new ReportStore(f).add(new Report(5L, ReportType.FLOOD, 1, 2));
    assertEquals(1, new ReportStore(f).readAll().size());
  }

  @Test
  public void onlyRecentFixesAreFresh()
  {
    assertTrue(Report.isFixFresh(0));
    assertTrue(Report.isFixFresh(Report.MAX_FIX_AGE_MS));
    assertFalse(Report.isFixFresh(Report.MAX_FIX_AGE_MS + 1));
    assertFalse(Report.isFixFresh(10 * 60 * 1000));
    assertFalse(Report.isFixFresh(-1));
  }

  @Test
  public void everyTypeRoundTripsByKey()
  {
    assertEquals(7, ReportType.values().length);
    for (final ReportType t : ReportType.values())
      assertEquals(t, ReportType.fromKey(t.key()));
    assertThrows(IllegalArgumentException.class, () -> ReportType.fromKey("nope"));
  }
}
