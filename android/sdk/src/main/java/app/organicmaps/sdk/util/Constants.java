package app.organicmaps.sdk.util;

public final class Constants
{
  public static final int KB = 1024;
  public static final int MB = 1024 * 1024;
  public static final int GB = 1024 * 1024 * 1024;

  static final int READ_TIMEOUT_MS = 10000;

  public static class Url
  {
    public static final String MAILTO_SCHEME = "mailto:";
    public static final String MAIL_SUBJECT = "?subject=";
    public static final String MAIL_BODY = "&body=";

    // TODO(FOUNDER TO CONFIRM): placeholder website until the Sampai domain is registered.
    public static final String WEBSITE = "https://sampai.my/";
    public static final String GITHUB = "https://github.com/hamzaiqbal-dev/malaysia-nav";

    public static final String COPYRIGHT = "file:///android_asset/copyright.html";
    public static final String FAQ = "file:///android_asset/faq.html";
    public static final String OPENING_HOURS_MANUAL = "file:///android_asset/opening_hours_how_to_edit.html";

    public static final String OSM_REGISTER = "https://www.openstreetmap.org/user/new";
    public static final String OSM_RECOVER_PASSWORD = "https://www.openstreetmap.org/user/forgot-password";

    private Url() {}
  }

  public static class Vendor
  {
    public static final String HUAWEI = "HUAWEI";
    public static final String XIAOMI = "XIAOMI";
  }

  /// @note! String values should be equal with map/framework.cpp constants kProductsPopupCloseReasonXXX.
  public static class ProductsPopupCloseReason
  {
    public static final String CLOSE = "close";
    public static final String REMIND_LATER = "remind_later";
    public static final String ALREADY_DONATED = "already_donated";
    public static final String SELECT_PRODUCT = "select_product";
  }

  private Constants() {}
}
