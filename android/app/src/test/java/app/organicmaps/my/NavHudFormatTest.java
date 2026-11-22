package app.organicmaps.my;

import static org.junit.Assert.assertEquals;

import java.time.LocalTime;
import org.junit.Test;

public class NavHudFormatTest
{
  @Test
  public void timeLeftShowsMinutesOnlyUnderAnHour()
  {
    assertEquals("12 min", NavHudFormat.timeLeft(12 * 60 + 30, "h", "min"));
    assertEquals("59 min", NavHudFormat.timeLeft(59 * 60 + 59, "h", "min"));
  }

  @Test
  public void timeLeftShowsHoursAndMinutes()
  {
    assertEquals("1 h 5 min", NavHudFormat.timeLeft(65 * 60, "h", "min"));
    assertEquals("2 h", NavHudFormat.timeLeft(2 * 3600, "h", "min"));
    assertEquals("1 j 1 min", NavHudFormat.timeLeft(3600 + 60, "j", "min"));
  }

  @Test
  public void timeLeftRoundsShortAndInvalidValuesUpToOneMinute()
  {
    assertEquals("1 min", NavHudFormat.timeLeft(0, "h", "min"));
    assertEquals("1 min", NavHudFormat.timeLeft(20, "h", "min"));
    assertEquals("1 min", NavHudFormat.timeLeft(-5, "h", "min"));
  }

  @Test
  public void arrivalUses24HourClock()
  {
    assertEquals("14:35", NavHudFormat.arrival(LocalTime.of(13, 5), 90 * 60, true));
    assertEquals("00:10", NavHudFormat.arrival(LocalTime.of(23, 40), 30 * 60, true));
  }

  @Test
  public void arrivalIgnoresNegativeDuration()
  {
    assertEquals("13:05", NavHudFormat.arrival(LocalTime.of(13, 5), -100, true));
  }
}
