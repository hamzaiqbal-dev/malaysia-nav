#include "kml/serdes.hpp"
#include "kml/serdes_gpx.hpp"
#include "kml/types.hpp"

#include "coding/reader.hpp"

#include "base/logging.hpp"

#include <cstddef>
#include <cstdint>
#include <string>

// Fuzzes the KML and GPX XML importers, which run on attacker-controlled input: a user can open or
// share a .kml/.kmz/.gpx file from another app, and its bytes reach these parsers. A thrown
// DeserializeException is the parser correctly rejecting bad input, so it is swallowed here; what
// the fuzzer is looking for is a memory-safety bug or undefined behaviour (ASAN/UBSAN) on any byte
// string, including XXE and billion-laughs style XML.
//
// Build and run (see tools/fuzz/README.md):
//   cmake --build build-<name> --target kml_fuzz_tests
//   ./kml_fuzz_tests -max_total_time=600 tools/fuzz/corpus/kml
extern "C" int LLVMFuzzerTestOneInput(uint8_t const * data, size_t size)
{
  base::ScopedLogLevelChanger const quiet(base::LCRITICAL);

  MemReader const reader(data, size);

  try
  {
    kml::FileData kmlData;
    kml::DeserializerKml des(kmlData);
    des.Deserialize(reader);
  }
  catch (kml::DeserializerKml::DeserializeException const &)
  {}
  catch (Reader::Exception const &)
  {}

  try
  {
    kml::FileData gpxData;
    kml::DeserializerGpx des(gpxData);
    des.Deserialize(reader);
  }
  catch (kml::DeserializerGpx::DeserializeException const &)
  {}
  catch (Reader::Exception const &)
  {}

  return 0;
}
