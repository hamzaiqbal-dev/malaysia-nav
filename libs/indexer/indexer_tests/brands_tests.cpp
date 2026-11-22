#include "testing/testing.hpp"

#include "indexer/brands_holder.hpp"

#include "coding/reader.hpp"
#include "coding/string_utf8_multilang.hpp"

#include <memory>
#include <set>
#include <string>

namespace brands_tests
{
using namespace std;
using namespace indexer;

char const g_testBrandsTxt[] =
    "brand.mcdonalds\n"
    "en:McDonald's|Mc Donalds\n"
    "ru:МакДональд'с|Мак Доналдс\n"
    "uk:Макдональдз\n"
    "\n"
    "brand.subway\n"
    "en:Subway\n"
    "ru:Сабвэй|Сабвей";

UNIT_TEST(LoadDefaultBrands)
{
  auto const & brands = GetDefaultBrands();

  TEST(!brands.GetKeys().empty(), ());
}

// Malaysia Nav: the Malaysian brands in the shipped categories_brands.txt, keyed the way
// generator/brands_loader.cpp derives keys from the English brand name.
UNIT_TEST(LoadDefaultBrands_Malaysia)
{
  auto const & brands = GetDefaultBrands();
  auto const & keys = brands.GetKeys();

  for (char const * key : {"petronas",
                           "petron",
                           "shell",
                           "caltex",
                           "bhpetrol",
                           "99_speedmart",
                           "kk_super_mart",
                           "mynews",
                           "7_eleven",
                           "familymart",
                           "mydin",
                           "lotus_s",
                           "aeon",
                           "giant_hypermarket",
                           "jaya_grocer",
                           "econsave",
                           "nsk_trade_city",
                           "mr_diy",
                           "watsons",
                           "guardian",
                           "tealive",
                           "zus_coffee",
                           "secret_recipe",
                           "marrybrown",
                           "maybank",
                           "cimb_bank",
                           "public_bank",
                           "rhb_bank",
                           "hong_leong_bank",
                           "bank_islam",
                           "bank_simpanan_nasional",
                           "ambank"})
    TEST(keys.count(key), (key));

  auto const names = [&brands](char const * key, char const * lang)
  {
    set<string> res;
    brands.ForEachNameByKeyAndLang(key, lang, [&res](string const & name) { res.insert(name); });
    return res;
  };

  // The first English name is the OSM brand=* value; the rest are what Malaysians type.
  TEST(names("petronas", "en").count("Petronas"), ());
  TEST(names("petronas", "zh").count("国油"), ());
  TEST(names("99_speedmart", "en").count("99 Speedmart"), ());
  TEST(names("kk_super_mart", "en").count("KK Mart"), ());
  TEST(names("lotus_s", "en").count("Tesco"), ());
  TEST(names("mr_diy", "en").count("MR.DIY"), ());
  TEST(names("bank_simpanan_nasional", "en").count("BSN"), ());
  TEST(names("public_bank", "zh").count("大众银行"), ());
  TEST(names("watsons", "zh").count("屈臣氏"), ());
}

UNIT_TEST(LoadBrands)
{
  BrandsHolder const holder(make_unique<MemReader>(g_testBrandsTxt, sizeof(g_testBrandsTxt) - 1));

  set<string> expectedKeys = {"mcdonalds", "subway"};
  auto const keys = holder.GetKeys();
  TEST_EQUAL(keys, expectedKeys, ());

  using Names = set<BrandsHolder::Brand::Name>;

  {
    Names expectedNames;
    expectedNames.emplace("McDonald's", StringUtf8Multilang::GetLangIndex("en"));
    expectedNames.emplace("Mc Donalds", StringUtf8Multilang::GetLangIndex("en"));
    expectedNames.emplace("МакДональд'с", StringUtf8Multilang::GetLangIndex("ru"));
    expectedNames.emplace("Мак Доналдс", StringUtf8Multilang::GetLangIndex("ru"));
    expectedNames.emplace("Макдональдз", StringUtf8Multilang::GetLangIndex("uk"));

    Names names;
    holder.ForEachNameByKey("mcdonalds", [&names](BrandsHolder::Brand::Name const & name) { names.insert(name); });
    CHECK_EQUAL(names, expectedNames, ());
  }

  {
    Names expectedNames;
    expectedNames.emplace("Subway", StringUtf8Multilang::GetLangIndex("en"));
    expectedNames.emplace("Сабвэй", StringUtf8Multilang::GetLangIndex("ru"));
    expectedNames.emplace("Сабвей", StringUtf8Multilang::GetLangIndex("ru"));

    Names names;
    holder.ForEachNameByKey("subway", [&names](BrandsHolder::Brand::Name const & name) { names.insert(name); });
    CHECK_EQUAL(names, expectedNames, ());
  }

  {
    set<string> expectedNames = {"McDonald's", "Mc Donalds"};

    set<string> names;
    holder.ForEachNameByKeyAndLang("mcdonalds", "en", [&names](string const & name) { names.insert(name); });
    CHECK_EQUAL(names, expectedNames, ());
  }

  {
    set<string> expectedNames = {"МакДональд'с", "Мак Доналдс"};

    set<string> names;
    holder.ForEachNameByKeyAndLang("mcdonalds", "ru", [&names](string const & name) { names.insert(name); });
    CHECK_EQUAL(names, expectedNames, ());
  }

  {
    set<string> expectedNames = {"Макдональдз"};

    set<string> names;
    holder.ForEachNameByKeyAndLang("mcdonalds", "uk", [&names](string const & name) { names.insert(name); });
    CHECK_EQUAL(names, expectedNames, ());
  }
}
}  // namespace brands_tests
