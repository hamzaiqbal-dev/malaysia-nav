#include "ge0/geo_url_parser.hpp"
#include "ge0/parser.hpp"

#include "base/logging.hpp"

#include <cstddef>
#include <cstdint>
#include <string>

// Fuzzes the map-link URL parsers that run on attacker-controlled input: the ge0/om/omaps.app
// short links (ge0::Ge0Parser) and the geo:/OSM/2GIS links (geo::UnifiedParser). Both are reached
// from map/mwm_url.cpp when another app hands Sampai a URL via an intent or a tapped link, so they
// must never crash, read out of bounds, or trigger undefined behaviour on any byte string.
//
// Build and run (see tools/fuzz/README.md):
//   enable fuzzing in the CMake configure, then
//   cmake --build build-<name> --target ge0_fuzz_tests
//   ./ge0_fuzz_tests -max_total_time=600 tools/fuzz/corpus/ge0
extern "C" int LLVMFuzzerTestOneInput(uint8_t const * data, size_t size)
{
  base::ScopedLogLevelChanger const quiet(base::LCRITICAL);

  std::string const url(reinterpret_cast<char const *>(data), size);

  ge0::Ge0Parser ge0Parser;
  ge0::Ge0Parser::Result ge0Result;
  ge0Parser.Parse(url, ge0Result);

  ge0::Ge0Parser::Result clearResult;
  ge0::Ge0Parser::ParseClearCoordinates(url, clearResult);

  geo::UnifiedParser geoParser;
  geo::GeoURLInfo info;
  geoParser.Parse(url, info);

  return 0;
}
