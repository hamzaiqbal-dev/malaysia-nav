# Sampai legal pack (checklist for a lawyer, not legal advice)

Prepared 21 Nov 2026 from the repository contents. Everything here must be confirmed by a Malaysian lawyer before a public launch. Sources: `LICENSE`, `NOTICE`, `DATA_LICENSE.txt`, `data/copyright.html`, `android/gradle/libs.versions.toml`, `android/app/src/main/AndroidManifest.xml`, `private.h`.

## 1. Blocker: the map data licence forbids white-labelling

`DATA_LICENSE.txt` covers the compiled binary files (`.mwm` maps, `packed_polygons.bin`, other `.bin` files; this includes `World.mwm`, `WorldCoasts.mwm` and the `drules_*.bin` shipped in the APK, and every map the app downloads from the upstream servers). Its conditions:

1. Show "Map data © OpenStreetMap and Organic Maps" on the map, About screen and main menu, with links to organicmaps.app and openstreetmap.org/copyright.
2. No redistribution without that attribution.
3. "Any white-labeling or rebranding use requires explicit written permission" from the Organic Maps team (legal@organicmaps.app).

This conflicts with the Sampai brand rule (no "Organic Maps" visible). The brand-sweep PR changed the on-map credit to "© OpenStreetMap contributors". The licences page keeps the full upstream credit. **Do not publish publicly until one option is chosen:**

- A. Ask legal@organicmaps.app for written permission to white-label (keep their credit if they require it).
- B. Generate Sampai's own data files from OpenStreetMap with `generator_tool` (Malaysia extract) and host them; then `DATA_LICENSE.txt` no longer applies to those files, only ODbL does. The compiled `drules_*.bin`, `packed_polygons.bin` and `World*.mwm` bundled in the APK would need regenerating too. A lawyer should say whether the Apache-2.0 source covers the generated style files.
- C. Keep the upstream credit on the map and in About (compliant, but visible branding).

## 2. Licence audit

Repository code: Apache-2.0 (`LICENSE`), copyright My.com B.V. (MAPS.ME) and Organic Maps Contributors. Commercial use, modification and redistribution are allowed. Conditions: keep `LICENSE`/`NOTICE` and file headers, state changes, no use of upstream trademarks. The Apache licence text and notices are shown in-app (About, Open-source licences). `LICENSES/` holds the texts.

| Item | Licence | Needed | Risk for a commercial app |
|---|---|---|---|
| Organic Maps / MAPS.ME code | Apache-2.0 | Notice in app and repo | None |
| OpenStreetMap data | ODbL 1.0 | "© OpenStreetMap contributors" + link to openstreetmap.org/copyright | Share-alike applies to a derived database: keep Sampai's own data (tolls, reports) as separate datasets, never merged into the OSM-derived map files, unless you accept publishing them under ODbL |
| Compiled map/binary files | `DATA_LICENSE.txt` | See section 1 | **Blocker** |
| Wikipedia text inside maps | CC BY-SA 4.0 | Attribution | Share-alike applies to the text only; shown in licences page |
| Sonny LiDAR terrain (Europe) | CC BY 4.0 | Attribution | None; Europe only, irrelevant to Malaysia, could be dropped |
| SRTM/ASTER elevation | Public domain / NASA terms | Credit courtesy | None |
| UK Code-Point/FHRS postcodes | Open Government Licence v3 | Attribution | None; irrelevant to Malaysia |
| US Census TIGER | Public domain | None | None |
| Native libs: BLAKE3, Boost, bsdiff/Courgette, Expat, fast_float, fast_obj, glaze, GLM, pugixml, skarupke, utfcpp, Vulkan headers/wrapper, open-location-code, succinct, Dear ImGui | MIT / BSD / Boost / Apache / CC0 | Notices (in `copyright.html`) | None |
| FreeType | FreeType Project Licence (BSD-like) | Credit | None |
| ICU | ICU licence | Notice | None |
| zlib-ng, minizip-ng, GLFW | Zlib | None | None |
| libkdtree++ | Artistic License 2.0 | Notice | Low (header-only, unmodified) |
| Android libs: AndroidX, Material Components 1.13.0, WorkManager 2.10.5, OkHttp 5.5.1, Android car-app | Apache-2.0 | Notice | None |
| MPAndroidChart 3.2.0-alpha (JitPack fork) | Apache-2.0 | Notice | Low; third-party fork |
| Firebase Crashlytics 33.11.0 BOM | Google terms | n/a | Not built today (needs `google-services.json`); if enabled, Data safety and privacy policy must change |
| Fonts: Roboto, Droid Sans Fallback, Noto (Arabic, Bengali, Hebrew, Malayalam, Thai, Devanagari) | Apache-2.0 / OFL | Notice | None |
| DejaVu Sans | Bitstream Vera licence | Notice | None |
| Jomolhari, Padauk | SIL OFL | Notice | OFL forbids selling the font alone only |
| Khmer OS | LGPL | Notice | Low; font used as data, must stay replaceable |
| **Code2000 font** | "Shareware" per `copyright.html` | Unclear | **Medium: shareware terms may not allow commercial redistribution; check, replace or drop (`data/fonts/06_code2000.ttf`)** |
| `organic_maps_emoji.ttf` | Not stated in repo | Unknown | Check the origin (likely Noto Emoji, OFL/Apache) before launch |
| Map icons: Material Design Icons, Remixicon (Apache); Evericons, Maki (CC0); Font Awesome Free, Boxicons, OnlineWebFonts, Röntgen, old MAPS.ME icons (CC BY 4.0); Nova, Bytesize, Tabler (MIT) | as listed | CC BY needs attribution (listed in licences page) | Low |
| **Freepik icons (flaticon.com)** | Freepik Free Licence | Attribution; no resale as standalone | **Medium: licence terms for apps must be checked, consider replacing** |
| Launcher icon / logo | Placeholder pin mark made for Sampai | None | Replace with final brand art |
| Translations | Contributors under Apache-2.0 | None | None |
| OSM OAuth keys in `private.h` | Belong to the Organic Maps OSM app registration | n/a | Sampai must register its own OSM OAuth app; do not reuse |
| Kayak affiliate id in `private.h` | Upstream's affiliate id | n/a | Not used by the Android app code; remove before launch |

The upstream audit is the authority for items marked "per `copyright.html`"; the third-party directories under `3party/` were not checked out in this environment, so their licence files were not re-read.

## 3. In-app pieces (this PR)

- About: "Open-source licences" row (shows `copyright.html`, now starting with the Sampai notice, upstream credit and OSM credit), "© OpenStreetMap contributors" row linking to openstreetmap.org/copyright, "Privacy policy" and "Terms of use" rows.
- `privacy.html` and `terms.html` are bundled assets, English and Bahasa Melayu (BM text needs a native check). The page language follows the phone language.
- Not done: a first-launch acceptance screen for the terms. Recommended before Play release.

## 4. Privacy policy (PDPA 2010) checklist

The draft is `android/app/src/main/assets/privacy.html`. It describes what the app does today:

- On the phone only: location, searches, history, bookmarks, tracks, settings, saved reports.
- Leaves the phone: map download requests (IP address visible to the map server operator, currently upstream's servers), optional OpenStreetMap login/edits, links the user opens, logs only if the user emails them.
- No ads, analytics, crash reporting, accounts or tracking.

For the lawyer: PDPA notice must be in Malay and English (done in draft); a named contact or data user and a public URL for the policy are needed (Google Play requires a web URL; the in-app copy is not enough, publish the same text, for example on GitHub Pages); whether an IP address in server logs makes upstream's hosting a cross-border transfer; the 2024 PDPA amendments (breach notification, data-protection officer thresholds, data portability) and whether they apply to a one-person app; retention (none held by Sampai today). Re-do this when Lapor reports leave the phone or any backend, analytics or crash service is added.

## 5. Google Play Data safety answers (matching today's build)

| Question | Answer |
|---|---|
| Does the app collect or share user data? | Collect: No (nothing is sent to Sampai). Share: No. Location is processed on the device only, which Google treats as not collected. |
| Location (approximate/precise) | Not collected (on-device only). Declare the use in the permissions form: foreground location for navigation. |
| Personal info, financial, health, messages, photos, contacts, audio | Not collected |
| App activity, web history, diagnostics | Not collected (no Crashlytics in the build; logs stay on the device unless the user emails them) |
| Device or other IDs | Not collected |
| Data encrypted in transit? | Yes for downloads; see `docs/my/SECURITY.md` once the security PR lands, because cleartext is still allowed in the network config |
| Can users request data deletion? | Nothing to delete off-device; uninstalling deletes local data |
| OpenStreetMap edits | Optional user-generated content sent to OpenStreetMap; if reviewers ask, declare as "user content, shared, optional" |
| Ads | None |
| Foreground service type "location" | Navigation and track recording; needs the Play permissions declaration and a short video |
| Target audience | Not designed for children |
| Privacy policy URL | Needs a public URL (see section 4) |

## 6. Risks for Hamza to decide (one line each)

1. Map data licence (section 1): ask permission, build own data, or keep the credit?
2. Map-download and metadata servers still point at upstream (`cdn*.organicmaps.app`, `meta.omaps.app`): will Sampai host its own before launch?
3. Trademark: is "Sampai" free for software and navigation services at MyIPO (classes 9, 42, 39) and in Play Store/App Store names? Not searched.
4. AES camera and police reporting via Lapor: lawyer to confirm no offence in Malaysia for showing or reporting camera and enforcement locations; consider showing fixed AES only, or leave out police.
5. Toll prices must come from the concessionaires; disclaimer text says "indicative only".
6. Contact details: which name, email and address go in the privacy policy and Play listing?
7. Fonts and icons flagged in section 2 (Code2000, Freepik, emoji font): replace or clear?
8. OSM OAuth: register a Sampai OSM app, or remove editing?
9. Insurance and liability wording in the terms for a navigation app: lawyer to review the clauses.
