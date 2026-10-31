# Fuzz harnesses

libFuzzer harnesses for the parsers in Sampai that run on attacker-controlled input (map links a
user taps or another app sends, and KML/GPX files a user imports). Added during the October 2026
offensive security test (`docs/SECURITY_PENTEST_2026-10-31.md`).

The harnesses live next to the code they test, following the repo's existing convention
(`libs/tracking/tracking_fuzz_tests`):

| Harness | Target | What it exercises |
|---|---|---|
| `libs/ge0/ge0_fuzz_tests` | `ge0::Ge0Parser`, `geo::UnifiedParser` | `om://` / `ge0://` / `omaps.app` short links and `geo:` / OSM / 2GIS links |
| `libs/kml/kml_fuzz_tests` | `kml::DeserializerKml`, `kml::DeserializerGpx` | KML and GPX XML import (incl. XXE / billion-laughs shaped input) |

## Build and run

Fuzzers are gated behind the `USE_LIBFUZZER` CMake option and build with Clang.

```bash
cmake --preset debug -B build-fuzz -DUSE_LIBFUZZER=ON
cmake --build build-fuzz --target ge0_fuzz_tests kml_fuzz_tests

# ~10 minutes each; the seed corpus lives under tools/fuzz/corpus/.
./build-fuzz/libs/ge0/ge0_fuzz_tests/ge0_fuzz_tests   -max_total_time=600 tools/fuzz/corpus/ge0
./build-fuzz/libs/kml/kml_fuzz_tests/kml_fuzz_tests   -max_total_time=600 tools/fuzz/corpus/kml
```

`omim_add_executable` adds `-fsanitize=fuzzer` automatically under `USE_LIBFUZZER`; add
`-fsanitize=address,undefined` in your configure flags for ASan/UBSan coverage (recommended).

## Corpus

`corpus/ge0/` and `corpus/kml/` hold seed inputs plus regression inputs named `regress_*`. The
`regress_*` files are the exact inputs that crashed a parser during the pentest and must stay
non-crashing:

- `corpus/ge0/regress_leading_plus` (`geo:0,+0`) and `regress_embedded_nul` — the coordinate
  regexes accept text `strings::to_double` rejects; the result was only `VERIFY`-checked, so these
  aborted the app (asserts-on builds) / read an unparsed value (release). Fixed in
  `libs/ge0/geo_url_parser.cpp`; also covered by `GeoURL_MalformedCoordinatesDoNotCrash` in
  `libs/ge0/ge0_tests/geo_url_tests.cpp`.
- `corpus/kml/regress_far_future_time.gpx` — a far-future `<time>` triggers a signed-integer
  overflow (UBSan) in upstream `base::TimeGM`; see the pentest doc (finding, not yet fixed upstream).
