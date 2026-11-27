package app.organicmaps.my;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MalayDefaultLocaleTest
{
  @Test
  public void malayDevicesUseMalay()
  {
    assertTrue(MalayDefaultLocale.shouldUseMalay("ms", "MY"));
    assertTrue(MalayDefaultLocale.shouldUseMalay("ms", "BN"));
    assertTrue(MalayDefaultLocale.shouldUseMalay("ms", ""));
  }

  @Test
  public void englishInMalaysiaUsesMalay()
  {
    assertTrue(MalayDefaultLocale.shouldUseMalay("en", "MY"));
  }

  @Test
  public void otherLanguagesAreKept()
  {
    assertFalse(MalayDefaultLocale.shouldUseMalay("en", "US"));
    assertFalse(MalayDefaultLocale.shouldUseMalay("en", ""));
    assertFalse(MalayDefaultLocale.shouldUseMalay("zh", "MY"));
    assertFalse(MalayDefaultLocale.shouldUseMalay("ta", "MY"));
    assertFalse(MalayDefaultLocale.shouldUseMalay("id", "ID"));
    assertFalse(MalayDefaultLocale.shouldUseMalay(null, null));
  }

  @Test
  public void languageNamesAreInTheirOwnLanguage()
  {
    assertEquals("English", MalayDefaultLocale.getLanguageName("en"));
    assertEquals("Melayu", MalayDefaultLocale.getLanguageName("ms"));
    assertEquals("中文", MalayDefaultLocale.getLanguageName("zh"));
  }
}
