package app.organicmaps.my;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks the Sampai theme tokens in res/values/my_colors.xml: each one is a valid colour, and the dark set
 * overrides only tokens that exist in the light set.
 */
public class SampaiTokensTest
{
  private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{8}");
  private static final String RES = "src/main/res/";

  private static Map<String, String> read(String file) throws Exception
  {
    final Element root =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(RES + file)).getDocumentElement();
    final NodeList nodes = root.getElementsByTagName("color");
    final Map<String, String> result = new TreeMap<>();
    for (int i = 0; i < nodes.getLength(); i++)
    {
      final Element e = (Element) nodes.item(i);
      result.put(e.getAttribute("name"), e.getTextContent().trim());
    }
    return result;
  }

  @Test
  public void colorsAreValidAndNamedSampai() throws Exception
  {
    for (final String file : new String[] {"values/my_colors.xml", "values-night/my_colors.xml"})
    {
      final Map<String, String> colors = read(file);
      assertFalse(file, colors.isEmpty());
      for (final Map.Entry<String, String> e : colors.entrySet())
      {
        assertTrue(e.getKey(), e.getKey().startsWith("sampai_"));
        assertTrue(e.getKey() + "=" + e.getValue(), COLOR.matcher(e.getValue()).matches());
      }
    }
  }

  @Test
  public void nightOverridesExistingTokens() throws Exception
  {
    final Map<String, String> day = read("values/my_colors.xml");
    final Map<String, String> night = read("values-night/my_colors.xml");
    assertTrue(day.keySet().containsAll(night.keySet()));
  }

  @Test
  public void conceptDarkPalette() throws Exception
  {
    // Values from design/concept-3d/index.html.
    final Map<String, String> night = read("values-night/my_colors.xml");
    assertEquals("#FF0B1220", night.get("sampai_bg"));
    assertEquals("#FF121C2E", night.get("sampai_panel"));
    assertEquals("#FFF3F6FB", night.get("sampai_ink"));
    assertEquals("#FF2E8BFF", read("values/my_colors.xml").get("sampai_route"));
    assertEquals("#FFFFC43D", read("values/my_colors.xml").get("sampai_accent"));
  }
}
