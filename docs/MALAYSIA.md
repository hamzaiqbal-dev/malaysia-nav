# Malaysia Nav — product plan (working name)

Goal set by the founder (24 Oct 2026): become Malaysia's number-one navigation app, made in Malaysia for
Malaysians, with the things Google Maps and Waze do not have. Bahasa Melayu first. Built to be bulletproof.

Base: this repository is a fork of Organic Maps (Apache-2.0, OpenStreetMap data). The engine (routing,
search, rendering, offline maps) is kept as-is and tracked upstream; everything Malaysian lives on top.
Attribution to "Organic Maps Project" stays in the About screen (licence requirement).

## Why now
- 20 Oct 2026: Malaysian government barred all civil servants from using Waze (JKSN decision).
- The recommended replacement (Tuju, LLM) is widely called hard to use.
- Google Maps remains, but has no live community reports, no RM tolls, no Malaysian-first UX.

## What makes it Malaysian (the features Google/Waze lack)
1. Language: Bahasa Melayu as the default UI and voice; English, 中文, தமிழ் as one-tap options.
   Malaysian voice guidance ("Belok kiri selepas 200 meter", "Susur keluar ke Tol Sungai Besi").
2. Tolls: every plaza with price in RM by vehicle class (PLUS, LDP, SPRINT, KESAS, DUKE, LPT, SMART...),
   total toll for a route, RFID / Touch 'n Go / SmartTAG lane hints, "avoid tolls" with RM saved shown.
3. Live community reports (the Waze part): jam, accident, police / roadblock (sekatan jalan), AES/AWAS
   camera, pothole, flood (banjir), road closure. Reports expire automatically; users confirm or dismiss.
4. Malaysian places out of the box: surau and masjid with prayer-time awareness, R&R (rest areas) with
   facilities, petrol stations with brand and RON95/RON97 price, mamak, kopitiam, pasar malam days,
   JPJ / PDRM / Puspakom, hospitals and klinik kesihatan, EV chargers.
5. Smart about Malaysian roads: U-turn-heavy routing hints, motorcycle lane routing (lorong motosikal),
   school zones, one-way kampung roads, Ops Tapis / balik kampung advisories.
6. Privacy and trust: no account needed, data stays in Malaysia, no tracking, no ads at launch.
7. Works offline everywhere: Sabah, Sarawak, kampung roads — the Organic Maps strength.

## Bulletproof rules (engineering)
- Never break upstream: Malaysian changes are additive and live in clearly named modules
  (`libs/my_*`, `android/.../my/`), so upstream merges stay easy.
- Every new feature ships with tests (unit tests in the same PR, per upstream policy).
- Crash-free target: 99.9% crash-free sessions before any public launch; monkey tests in CI.
- Offline first: every feature must degrade gracefully with no network.
- Secure by default: the reports back-end validates and rate-limits; no personal data stored; PDPA.
- Reproducible builds via GitHub Actions; signed releases only from CI.

## 30-day test plan
Week 1–2  Fork hardened: app id, name placeholder, BM default, Malaysian store metadata, CI building a
          debug APK on every push. Toll dataset v1 (CSV, sourced from LLM/concessionaire public rates).
Week 3    Reports v1: client button + minimal back-end (Malaysia-hosted), 20-user private test.
Week 4    Play Store internal/closed beta; measure retention (D1, D7) and report volume.
Decision  5,000 organic installs or strong D7 retention → fund and staff it; otherwise rethink.

## Decisions (founder)
- App name: **Sampai** (29 Oct 2026). Brand/logo still open.

## Open decisions (founder)
- Company/entity that will own the Play Store and App Store accounts.
- Back-end hosting for reports (Malaysian cloud: e.g. AIMS, Exabytes, or a MY region of a big cloud).

## UI direction (29 Oct 2026)
Founder reviewed the stock Organic Maps screens and wants the app recoded with a 3D, Google Maps / Waze-class
interface. Design concept lives in `design/concept-3d/index.html`; implementation steps in `design/README.md`.

## Work log
- 2026-10-24: Forked organicmaps/organicmaps → hamzaiqbal-dev/malaysia-nav. Plan written. Next: CI
  build of Android debug APK, app id and placeholder name, BM default language.
- 2026-10-29: 3D UI concept added (design/concept-3d). Android redesign plan written. Next: PR 1 = theme tokens + NavHudView.
- 2026-10-29 (night): PR #1 feature/nav-hud-redesign = theme tokens (my_colors.xml day/night) + NavHudView
  (turn banner, lane strip, speed + limit, ETA sheet) behind R.bool.my_nav_hud_enabled; BM + EN strings;
  unit tests for ETA/speed formatting. Blocker: GitHub Actions has never run on this fork (0 runs) — enable
  workflows in the repo's Actions tab, then CI validates the PR. Next: device check in day/night, portrait/
  landscape; Plus Jakarta Sans; tolls in the ETA sheet (step 5); ExploreSheet (step 2 cont.).
- 2026-10-30 (night): PR #2 tolls v1 — toll booths come from the map data (libs/my_tolls, JNI
  app.organicmaps.sdk.my.MyTolls), prices from data/my_tolls.csv (52 open-system plazas, sourced), total in RM in the
  HUD ETA sheet ("+" when a booth on the route has no price yet). PLUS/LPT closed-system fares and "RM saved when
  avoiding tolls" still to do. Name decided: Sampai.
- 2026-10-30: PR #3 identity — applicationId my.sampai.app, app name "Sampai" ("Sampai Beta"/"Sampai Debug"),
  Play listing text for en-US and ms-MY rewritten for Sampai (other locales still carry the Organic Maps text;
  rewrite before publishing). Still Organic Maps': launcher icon, splash logo, contact e-mail/website in
  android/app/src/{fdroid,google}/play — trademark: replace before any public build.
- 2026-10-30: PR #4 Bahasa Melayu search — "ms" added to the category locales (libs/indexer) and BM synonyms for 43
  categories/groups in data/categories.txt: surau/masjid, hentian rehat (RnR), stesen minyak, pengecas EV, klinik,
  balai polis, mamak, kopitiam, Puspakom, JPJ/pejabat kerajaan, pasar malam, parkir, tandas, stesen LRT/MRT/KTM…
  Next for search: 中文 and தமிழ் synonyms, Malaysian brands in categories_brands.txt (Petronas, Petron, Shell, 99
  Speedmart, Mydin, KK Mart…).
- 2026-10-30: docs/REPORTS.md — live reports v1 design (data model, TTLs, PDPA, rate limits, API, hosting options with
  RM costs). Needs the founder's hosting choice and domain before client/back-end code.
- 2026-10-30 (late): PR #7 Bahasa Melayu UI — 616 new ms definitions in data/strings/strings.txt, so the Android app,
  SDK, Android Auto, routing, downloader and Wear modules are fully BM (values-ms: 19 → 582 strings in the app module,
  plus new values-ms folders for the five other modules). PR #8 BM voice guidance — all 68 TTS phrases in sound.txt,
  data/sound-strings/ms.json generated, "Bahasa Melayu" added to the voice picker (libs/platform/languages.hpp).
  Open decision for the founder: whether values/ (the default locale, today English) should become BM, which is step 4
  of design/README.md. Until then BM is a selectable locale and English is the fallback for anything untranslated.
- 2026-10-30 (late): CI audit. The "Android Check" failure mailed for cfa6909 on feature/sampai-identity was the
  app-id rename: android/app/build.gradle's checkApkPermissions allow-list still matched
  app.organicmaps…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, so both debug builds failed on the renamed
  my.sampai.app permission. Already fixed in c086a9d (head of that branch), which builds green.
  Two gaps found and worked around: (a) the kotlin style, detekt and clang-format workflows only trigger on
  pull_request with branches: [master], so a stacked PR never runs them — ktlint 1.8.0, detekt 1.23.8 and
  clang-format 23 were run locally over the whole chain instead and are clean; (b) only Android Check had ever been
  dispatched, so the C++ of libs/my_tolls had never been compiled by CI — the CMake workflow was dispatched for the
  chain and passes. Worth making those three workflows run on every PR base, not just master.
- 2026-10-30 (night): PR #18 Malaysian POI search categories — 中文 and தமிழ் synonyms for the ten categories
  Malaysians search for (surau/masjid, R&R, mamak, kopitiam, JPJ, PDRM, Puspakom, klinik kesihatan, EV charging),
  "ta" registered as a category locale (libs/indexer, code 50), and the Malay/English lines widened with the
  colloquial spellings: musolla, kedai mamak, nasi kandar, kopi tiam, pondok polis, IPD, klinik kerajaan,
  hentian sebelah, R&R. Three concepts have no exact OSM tag and are mapped to the nearest real ones: R&R →
  highway=rest_area/services, mamak → amenity=restaurant + fast_food, kopitiam → amenity=cafe; JPJ has no
  government=transportation subtype in the classificator, so it sits on office=government, and klinik kesihatan
  on amenity=clinic. New test CategoriesHolder_MalaysianCategories checks all four languages against the shipped
  file. Next for search: Malaysian brands in categories_brands.txt (Petronas, Petron, Shell, 99 Speedmart, Mydin,
  KK Mart…), and ms/ta names in types_strings.txt so place pages are not English-only.
