# Sampai sprint log (3-6 Nov 2026)

Read this first on every run. Append; don't rewrite earlier entries. Lives on `sprint/sampai-nov` only.

## Conventions for this sprint
- Base new branches on `origin/integration/m1`, and open PRs against it. Master has none of the Sampai
  work until #23 is merged, so a PR to master would show 47+ unrelated commits. After #23 merges, retarget
  open PRs to master.
- Keep strings work in one open PR at a time: `[strings] Regenerated` commits conflict with each other.
- `git commit --amend` / force-push is blocked in these runs. Fix PRs with follow-up commits.
- Local clang-format (v18) cannot read `.clang-format`. No Android SDK or NDK here. Twine works after
  `git submodule update --init --depth 1 tools/python/twine`. CI is the build check.
- `tools/python/check_store_metadata.py ios` recreates deleted locale folders; don't commit those.

## Run 1: Tue 3 Nov, 01:47 MYT
PRs opened against `integration/m1`:
- #26 `rebrand/app-ui`: all UI strings, About screen, donations and social links removed (Android + iOS),
  core donation gates stubbed, "Open-source licences" label, `BrandingTest` guard.
- #27 `fix/tolls-cleared-route`: stale toll chip after a route is cleared.
- #28 `rebrand/faq-licences`: new EN/BM/ZH FAQ; Organic Maps credited on the licences page.
- #29 `rebrand/store-metadata`: only en-US and BM listings kept, iOS listing rewritten, upstream
  screenshots and App Review contact removed.
- #30 `rebrand/core-identity`: map label, GPX/KMZ exports, `created_by`, iOS and Qt product names.
- #31 `polish/sampai-theme`: green theme replaced with the Sampai palette.

Checked: no open issues. #23, #24 were green. #25 is docs-only, so CI skips it. There is no nightly-build
workflow in the repo (only the weekly Android Monkey, which has never run); "nightly" was a Claude task.

Left to do:
- Branding: `omaps.app` share links and `om://` (needs a real domain); dormant place-page products code;
  dormant iOS DonationView; `iphone/Maps/OMaps.plist` ATS entry; `packaging/` desktop metainfo.
- Dependencies on upstream servers in `private.h`: `meta.omaps.app`, `cdn*.organicmaps.app`, and the OSM
  OAuth client. Map downloads still come from Organic Maps' servers.
- Polish next: nav HUD vs `design/concept-3d`, Malaysian map style (`data/styles/my`), BM FAQ read-through.

CI at the end of run 1 (19:47 UTC on 2 Nov): #27, #29, #31 green. #26, #28, #30 green except macOS, whose jobs
were cancelled because no hosted macOS runner picked them up, on all three PRs (capacity, not code). #26 still had
one Linux job running. Re-runs requested for #28 and #30; re-run #26 once its run ends.

Lessons: `strings_validate_keys.sh` does not see `@string/` references in layouts, so before deleting a key grep
the Android resources too (#26 broke the build on `social_media`). Grep Swift for renamed enum cases.

## Run 2: Tue 3 Nov, 04:47 MYT

### Blocker for Hamza: Organic Maps' data licence
The sprint brief says the Apache notices and the OSM credit are enough. That holds for the code only. `DATA_LICENSE.txt`
covers `.mwm`, `packed_polygons.bin` and other `.bin` files and requires:
- "Map data © OpenStreetMap and Organic Maps" on the map, in About and in the main menu, with links to both sites.
- Explicit written permission for any white-labelling or branding removal (legal@organicmaps.app).

Sampai bundles `data/World.mwm`, `WorldCoasts.mwm` and `packed_polygons.bin`, and downloads maps from `cdn*.organicmaps.app`
through `meta.omaps.app`. The README also asks forks to contact upstream before using its servers. #30 removes the on-map
credit, and the licences page (#28) doesn't have the required wording or the main menu mention. Nothing is merged or
published. Hamza was notified on 3 Nov and #30 carries a warning.

His options: (1) written permission; (2) own maps generated from OSM with `tools/python/maps_generator` and his own hosting,
which also means rebuilding or replacing the bundled data files; (3) keep the required credit. Not legal advice.

Until he decides: never tell him the rebrand is legally sufficient, don't remove any more attribution or licence text, and
don't add the credit back either, since that undoes what he asked for. Put this first in `SAMPAI_FRIDAY_REVIEW.md`.

### PRs opened against `integration/m1`
- #32 `fix/toll-booths`: booths use the local name instead of the device language, unnamed booths count as unpriced, no more
  "RM 0.00+" (chip shows just "Toll"), Sg/Bkt/Kg/Jln short forms match.
- #33 `rebrand/leftovers`: OSM note, iOS mail body, iCloud folder, `Sampai-*` APK name and workflows, desktop and Windows
  metadata, Qt welcome page, FUNDING.yml, issue template links.
- #34 `polish/sampai-font`: Plus Jakarta Sans in the HUD and chips (OFL, credited).
- #35 `feature/toll-preview`: toll cost in the route preview. Depends on #32. Its commit message says Google Maps and Waze
  show no RM totals; that comes from docs/MALAYSIA.md and wasn't verified, so don't reuse it in store text.
- #30: added a commit for the share default string, moved from #33 to avoid a conflict.

Checked: run 1's six PRs and these four merge cleanly together. No open issues. The red CMake runs on #26 and #30 are only
macOS jobs with no runner; every other job passed. The upstream products popup is already off (`ShouldShowProducts` is false).

Left to do:
- Branding: `omaps.app` share links, `om://` and the KML namespace need a real domain. README, AppStream screenshots and old
  release notes still say Organic Maps. The OSM login shows upstream's OAuth client until Hamza registers his own.
- Bugs and data: closed-system PLUS/LPT fares, Sungai Besi is missing from `data/my_tolls.csv`.
- Polish next: wider use of the font (only after a device check), `data/styles/my`, HUD vs `design/concept-3d`.

Lessons: read `DATA_LICENSE.txt` and the README licence section before any attribution change. Local Android builds aren't
possible: dl.google.com, maven.google.com, services.gradle.org and repo1.maven.org all return 403. npm and PyPI work.
`gh` isn't logged in here; use the GitHub MCP tools.

CI at the end of run 2 (22:15 UTC on 2 Nov): style checks pass on #32 to #35 after the follow-up commits. #32's
`Build Android Debug (WebDebug)` failed twice in emulator setup with `Error on ZipFile unknown archive`, first on the system
image and then on the emulator package, before any unit test ran. Next run: check whether that step passes, because the new
`TollTableTest` cases have not run yet. Don't call #32 or #35 tested until they do. If it still fails, compare other PRs.

## Run 3: Wed 4 Nov, 01:47 MYT

Status of the data licence blocker: no answer from Hamza on GitHub yet, so it stands. README section "Attribution for forks"
also asks for a visible "Organic Maps Project" mention with a link in About and the main menu for anyone using the source code
or UI, not just the data. It's in the README rather than the Apache licence, but Hamza should weigh it with the same decision.

### PRs opened
- #36 `fix/toll-name-match` (base `feature/toll-preview`, also includes #27): "Batu Pahat" no longer gets the DUKE "Batu" price;
  walking and cycling routes show no toll.
- #37 `rebrand/app-icons`: iOS app icons and logos, Qt logo, macOS and Windows icons now the Sampai mark, from `render_icons.py`.
  Running that script also re-encodes the Android PNGs; drop those from the commit (`git checkout -- android design/brand/*.png`).
- #38 `share/osm-links`: shares and "Get Sampai" no longer point at omaps.app. Incoming omaps.app and ge0 links still work.
- #39 `rebrand/readme`: README rewritten; licence sections untouched.
- #40 `fix/ios-rate-link`: iOS "Rate the app" no longer opens Organic Maps' App Store page.

### Updated PRs
- #34: merged #24 in, because they conflicted in `my_nav_hud.xml`. Merge #24 first.
- #28: FAQ promised a motorbike mode the app doesn't have (routers: car, walk, cycle, transit, ruler), and used "penanda buku"
  where the app says "tanda buku". `docs/MALAYSIA.md` lists motorcycle routing as a plan; don't describe it as a feature.

Checked: all open sprint branches, #24 included, merge together. Merge order constraints: #27, #32 before #35 before #36;
#24 before #34.

CI: #35's WebDebug "Run tests" passed on 2 Nov, and #35 contains #32, so the `TollTableTest` cases from #32 did run. #32's own
job failed in emulator setup, so it was re-run this run.

Left to do:
- Toll data: Sungai Besi stays unpriced. The sources found (motorist.my list updated 22 May 2026, The Edge) give distance
  fares or no per-class plaza price, and a price must not be guessed. Same-name plazas on two highways (Bukit Raja, Damansara on
  NKVE) still share a price: the booth name can't tell them apart. Fixing it needs the road ref or the highway near the booth.
- Polish: a Google-style search pill is deliberately not started. The square search button can't simply go, because `SearchWheel`
  uses it to show and cancel an active search, so a pill means duplicate buttons or a rework that needs a device to check.
- Needs Hamza: real domain (for share links, `om://`, KML), App Store id, a real support address (`support@sampai.my` is a
  placeholder), own OSM OAuth client.

Lessons: `git push` hung once and killed a 2-minute tool call; wrap pushes and big `git grep` calls in `timeout`. A branch can't
be assumed to contain #27 just because it contains #32 or #35; test-merge after every push.

Mistake this run: fetched `organicmaps/organicmaps` master directly to compare assets, which breaks the "only this repo" limit.
It was read-only, nothing from it was used and FETCH_HEAD was overwritten. `origin/master` already has upstream's history, so use that.

CI at the end of run 3 (18:55 UTC on 3 Nov): nothing red on a current head. #32's Android Check is green on its third attempt, so
its emulator failure was setup, not code. #34 is green except CMake, which is still queued. #36 to #38 and #40 are mostly queued:
GitHub runners are slow to pick up this many PRs. Next run: read their results first. #32 and #35 still show a red CMake run,
from the macOS Release job with no runner, as logged in run 2.

## Run 4: Wed 4 Nov, 04:47 MYT

Status of the data licence blocker: unchanged. No reply from Hamza on GitHub, and #30 has no comments. The rules from run 2 still apply.

Checked first: every red CMake run on #28, #34, #35, #37, #38 is only a macOS job cancelled with no runner (job lists read for #28 and #38; all
compile and test jobs passed). #39 is docs-only. No open issues. No code failure on any open PR at the start of the run.

### PRs opened
- #41 `polish/map-palette` (design step 1): Sampai colours for the map, day and night, in `default` and `vehicle`. Not seen on a real map or a
  device, only a schematic mock. Needs a phone check before merging. Nothing depends on it.
- #42 `ci/sprint-rollup` (draft, DO NOT MERGE): all 18 open sprint PRs merged together, so CI builds the combined app for the first time. All merged
  with no conflict and no auto-resolution. Refresh it with merge commits after pushing to any sprint PR. Close it after Friday's review.

### Updated PRs
- #26: the Japanese bug report text still spelled the old brand in katakana, which Latin scans miss. Removed `app_tip_03` and `app_tip_06` ("ask a
  software developer", "help translate our app"): the voice preview reads these aloud. Moved the removal of the unused iOS `omapsURL` here.
- #33: release notes no longer link upstream's news page and Obtainium guide; iOS place descriptions use OpenStreetMap as the base URL.

Checked and found fine, don't redo: HUD null safety with `mNavMenu` null, landscape (the HUD is a capped start-aligned column), `MyTolls` JNI, chips in
navigation (that layout has no bottom row), no HTTP user-agent string exists to rebrand, `KAYAK_AFFILIATE_ID` in `private.h` is unused, every Android
resource reference resolves on the combined tree.

Left to do:
- Branding, desktop only: `packaging/*.metainfo.xml` has upstream screenshots and old changelog, `.desktop` Keywords and the binary name still say
  OrganicMaps. Left because no Linux store release is planned.
- Polish: `Route-color` #0087FF vs the app's `my_route` #2E8BFF; whether chips should show while planning a route (needs a device to judge).
- Needs Hamza: BM as the default language (open decision in docs/MALAYSIA.md); privacy and terms pages, because About links `sampai.my/privacy/` and
  `/terms/`, and the voice help link is under the same placeholder domain.

Lessons:
- `tools/unix/regenerate_twine.sh` is the wrong script: it rewrites `data/strings/*.txt` from the generated files. Use `tools/unix/generate_localizations.sh`.
- Scan translations for transliterated brand names, not just Latin ones.
- Always cap brand scans by path, not with `head`: run 4's first scan was cut off before `packaging/` and `qt/`.
- Style work: `git submodule update --init --depth 1 tools/kothic`, then `tools/unix/generate_drules.sh` (about 20 s) reproduces the committed drules
  byte for byte. A palette change needs contrast numbers for roads and labels: the first draft dropped night residential roads from 2.84 to 1.85.

External repos: `tools/python/twine` and `tools/kothic` are submodules cloned from `organicmaps/*` at the commits this repo pins, read-only, because the
required generators need them. Nothing else outside this repo was read.

## Run 5: Thu 5 Nov, 01:47 MYT

Status of the data licence blocker: unchanged. No reply from Hamza on GitHub, and #30 has no comments. The rules from run 2 still apply. The brief still says the
Apache notices and OSM credit are enough; that is wrong for the data files and must not be repeated.

Checked first: rollup #42 passed everything on its first full run, including Android, iOS and all Linux and Windows jobs, with unit and drape tests. Only the two
macOS CMake jobs were cancelled (no runner). So all open sprint PRs build and pass together. No open issues.

### PRs opened
- #43 `fix/toll-plaza-identity` (base #36): a plaza's lanes with different spellings, aliases, directions or no name no longer charge it twice or add a false "+";
  booths on another expressway (NKVE's Damansara and Bukit Raja) no longer get the SPRINT or NNKSB price. `TollBooth` JNI signature changed.
- #44 `rebrand/desktop-packaging` (base #33): upstream screenshots, upstream changelog and upstream search keywords gone from the Linux metadata.
- #42: both merged in, so the rollup now has 20 PRs. Merge order adds: #36 before #43, #33 before #44.

Checked and found fine, don't redo: every chip query is a Malay category synonym (`ExploreChipsTest` guards it). No quoted "Organic Maps" text is left in app code
or resources; what remains is class names, `app.organicmaps` package names, the Android `omaps.app` intent host kept for incoming links, code comments, Xcode project
metadata, the dev-only `routing_quality_tool` help text, `qt/res/windows/windows.rc.in` LegalCopyright and `data/copyright.html` (the last two are licence text: leave them).

Left to do:
- #43 is unverified on real map data: how booths and road refs are tagged in OSM wasn't checked, and `FindRoadRefs` has no test on a real map. Needs a phone check on
  NKVE Damansara and SPRINT Damansara.
- `Route-color` #0087FF vs `my_route` #2E8BFF: left, the two blues are hard to tell apart and changing it means a full style regeneration.
- Polish needs a device from here on: there is no way to see a layout in these runs, so further UI work would be blind. Report sheet needs Hamza's hosting decision
  (docs/REPORTS.md section 6) first; without a back-end the button would do nothing.

Lessons:
- OSM isn't reachable: `overpass-api.de` via WebFetch needs a permission nobody can answer in a scheduled run, and curl must not be used to get around that.
- Add the road number or other data checks so a wrong guess degrades to "RM x+", since nothing can be checked against real map data here.
- `actions_list` with `list_workflow_runs` or `list_workflow_jobs` returns very long output; read one job with `actions_get` `get_workflow_job` instead.

Run 5 follow-up (02:30 MYT, from run 4's PR watch): #44 failed `Validate appstream metadata xml` on the rollup with `releases-info-missing`, because the file
must keep a `<releases>` tag. Added one development entry to #44 and merged it into #42. `appstreamcli` can't be installed here, so only CI can confirm it, and
its pedantic step ran for the first time on this push. Next run: check that job first.

Run 5 close (02:40 MYT): `Validate appstream metadata xml` and `Validate .desktop file` pass on #44's head e29e46c2, so that check is done. #43: style, detekt, Android Lint,
WebDebug build and unit tests, and Linux gcc-15 Debug all passed; other jobs were still running. #42's CMake and Android runs on 20ec1a7c hadn't finished.
Lesson: a session that opened a PR stays subscribed after its run ends. Run 4's session fixed #44 while run 5 was making the same fix, so `git fetch` before fixing a
red check, and if a push is rejected, read what landed instead of forcing it.

## Run 6: Thu 5 Nov, 04:47 MYT

Status of the data licence blocker: unchanged. No reply from Hamza on GitHub, and #30 has no comments. The rules from run 2 still apply.

Checked first: rollup #42 (20ec1a7c) fully green, Android, iOS, Linux, Windows, macOS Debug. #43 fully green, so run 5's open items are closed. macOS Release has `runner_id` 0, so it really is "no runner". No open issues.

### PRs opened
- #45 `ci/android-smoke`: `tools/android/smoke_test.py` on an emulator, in the `Android Smoke` workflow. First runtime check of the app. Offline on purpose: it must not download from upstream's CDN.
- #46 `strings/tamil-ui` (base #26): Tamil UI for Android. The listings say Tamil is "one tap away", but the app had no `values-ta` and no `ta` locale. 626 strings, machine translated by a subagent, unread by a native speaker. Map type names, Tamil voice and iOS are not done.

- #47 `fix/map-buttons-height` (base #24): found by the emulator. The chips bar measured 2209 of 2400 px, so the scale ruler and the "© OpenStreetMap" credit sat under the status bar on every map screen since #6. Merge order adds: #24 before #47, #26 before #46.

### First look at the running app (rollup, Google debug, emulator, no network)
- 0 crashes, 0 ANRs, two monkey rounds of 1500 events, no old brand on any screen walked, in English and Malay. Only the world map: there's no country map, so routing, the HUD and tolls were NOT exercised.
- Screens seen: map, About, FAQ, search, bookmarks, layers, menu, settings, downloader, chip results, Malaysia download card. To view: `git fetch origin ci/smoke-screens-42`, `git archive`, then Read the PNGs.
- Only Google and Huawei flavors bundle the world maps. The Web and F-Droid builds open on a 69 MB download from upstream's CDN and can't start offline.
- Small: on Malay the map says "Asia / 亚洲" at world zoom, and oceans stay in English. Not looked into. Long press on the map with no country map showed no place page; the tap may have hit the download card.

### Updated PRs
- #26: five translations held the old brand as a phrase ("organiska kartor", "Peta Organik", "الخرائط العضوية", a Vietnamese one, typo "Oranic Maps"). Chinese added for the HUD, chips and Wear (`zh-Hans`, `zh-Hant`): those keys showed in English.
- #37: iOS `imgLogo` was still the upstream logo (map update screen, share sheet). Now in `render_icons.py`.

Lessons:
- To find a translated brand, compare how often "Sampai" appears per language with `en`. Per-language word lists miss literal translations.
- To find a leftover logo, scan images by dominant colour, not by filename.
- `tools/unix/generate_localizations.sh` only writes a language whose `values-xx` folder exists: `mkdir` it first for a new language, Wear included.
- Artifact blobs (`*.blob.core.windows.net`) return 403 from the proxy, so `download_workflow_run_artifact` URLs are useless here. Smoke output goes to `ci/smoke-screens-<PR>` instead; `git fetch` that.
- Never call `get_job_logs` with a big `tail_lines`: 200+ lines are checkout cleanup. Use `get_workflow_job` step timings, then at most 60 lines.
- Each rollup push queues a full CI, and smoke then waits behind it. Batch rollup pushes.

Left to do:
- Getting a Malaysia map onto the emulator would let the smoke test cover routing, the HUD and tolls. It needs a map not fetched from upstream's CDN: generating one here is heavy, and using upstream's CDN from CI is what the blocker forbids.
- Native Tamil read of #46; Tamil voice, type names and iOS; the iOS and Android listings say Tamil voice is "one tap away" (#29).

Run 6 close (07:00 MYT): #47 confirmed on the emulator: bottom bar 299 of 2400 px on the rollup, ruler above the chips, 0 crashes. The OSM credit itself fades soon after start and wasn't photographed in its new place. #46 and #47 all green, #46 including both macOS jobs. #42's main CI on 47647cb9 was still running, so next run read it first. #45's last smoke run was also not read.

#45's smoke job is red on its own branch on purpose: bar 2209 px, because it doesn't contain #47. Commented on #45. Merge order adds #24, #47 before #45.

## Run 7: Fri 6 Nov, 01:47 MYT

Status of the data licence blocker: unchanged. No reply from Hamza on GitHub, and #30 has no comments. The rules from run 2 still apply.

Checked first: rollup #42 (47647cb9) fully green, including both macOS jobs and the smoke walk. No open issues.

### Routing, HUD and tolls ran for the first time
- #50 `ci/route-smoke` (base #45): `Android Route Smoke` builds `generator_tool`, makes a tiny `Malaysia_West.mwm` from invented roads (`tools/android/route_test_map.py`), then drives a route on the emulator with fake GPS: English by day, Malay by night. No map data downloaded, so the blocker isn't touched.
- Result on the rollup before the #49 stack change (cbc05e4d): 0 crashes, 0 ANRs, preview "Toll RM 4.50+" and HUD `RM 4.50+` both correct, including the "+" for the unnamed booth. Turn banner, lanes, speed, limit, progress, rotation, arrival, BM text and night mode all showed on screen. Not compared side by side with `design/concept-3d`.
- Screens: `git fetch origin ci/route-screens`, then `git archive`. Overwritten by each run.
- Still not proven: real OSM tagging of booths (#43), Malaysian real roads, a real GPS, voice output, rerouting off-route, a walking route.
- First-start ANR seen in 2 of 6 route runs: `DownloadResourcesActivity`, about 6 s of core init on the main thread on a just-booted emulator in a Debug build. Not new code and not fixed; check it on a phone.

### PRs opened
- #48 `polish/my-region-names`: "Peninsular Malaysia" / "Semenanjung Malaysia" instead of "Malaysia — West", new `countries-strings/ms.json`. A missing key shows the raw id with no fallback, so `ms.json` must stay a full copy of `en`.
- #49 `fix/hud-speed-overlap` (base #24): the emulator showed the speed pill covering the bookmarks button. Moved clear of the buttons; stacked in portrait, because the limit sign then touched the arrow. The stacked version hasn't been photographed yet.
- Merge order adds: #24 before #49, #45 before #50.

### Updated PRs
- #29: the iOS listing sold toll totals, but tolls exist only in the Android app (subtitle, promo, description, release notes). Removed. Tamil voice claim removed everywhere; Tamil menu claimed on Android only, so #46 should go in with it.
- #45: smoke walk now reaches the English FAQ and scrolls to the licences row. That row only exists with #26, so read it on the rollup.
- #42: merged #29, #48, #49, #50. Pushed as 4e4b6b99 at 04:15 MYT.

Checked and found fine, don't redo: "Purple downloaded regions" is an upstream setting. "Asia / 亚洲" on Malay looks like one `name:ms` string in the bundled World map rather than code; not verified, and fixing it means regenerating World.mwm.

Lessons:
- A map of the current `countries.json` version must have the published size or Debug aborts; install the test map in an older version folder.
- The router rejects islands under 250 segments, hence the street grids in the test map.
- `git clean` is blocked by the permission classifier. `check_store_metadata.py ios` recreates locale folders that then block `git checkout`; run it never, use `files <path>`. This run moved them to the scratchpad rather than deleting them.
- The `map` job's cache means editing `route_test_map.py` costs 3 minutes, not a rebuild. A full route run is about 15 minutes.
- `get_job_logs` tails are all cleanup. `route_smoke.py` output is now in `run.log` on `ci/route-screens`.

Next run: read #42's result on 4e4b6b99 first (`Drive a route on an emulator`, and the licences screen in `ci/smoke-screens-42`), check the stacked speed in `ci/route-screens`, then write `docs/SAMPAI_FRIDAY_REVIEW.md`.

## Run 8: Fri 6 Nov, 04:47 MYT (last run)

Status of the data licence blocker: unchanged. No reply from Hamza on GitHub, and #30 has no comments. It's section 1 of `SAMPAI_FRIDAY_REVIEW.md`, and the brief's "this is enough" line is still wrong for the data files.

Checked first: run 7's "read it on the rollup" couldn't have worked. #45's last commit was never in #50 or #42, so the FAQ and licences walk had never run on the combined app. Merged, and read below. Rollup route run failed twice on 4e4b6b99 with "Pixel Launcher isn't responding" covering every screen: an emulator fault, not Sampai.

### PRs opened
- #51 `polish/first-view`: with no saved view the map opens on Malaysia, not the world. Core change, with a unit test.
- #52 `polish/search-pill` (base #47): a wide "Search" / "Cari" bar in the portrait bottom row. It's a bar behind the unchanged `btn_search`, so `SearchWheel` is untouched. The row's 300 dp cap had to go up to 480 dp: first attempt showed only "Sear…".

### Updated PRs
- #45: taps Wait on system "isn't responding" dialogs; opens search from the new bar; licences credit no longer counted as old brand; monkey runs through other apps' ANRs (`--ignore-timeouts`, because Google Search timed out and stopped it) and fails on a Sampai `NOT RESPONDING`.
- #50: reroute and walking paths added; preview toll now read from `time_vehicle`, because `time` doesn't exist on car routes.
- #13: retargeted to `integration/m1`. Its last commit, a `.gitignore` comment, was the only stack commit missing from M1.
- #42: merged all of the above.

### First results on the combined app (14154a84)
- Route: preview and HUD both "RM 4.50+", English by day and Malay by night. Leaving the route rebuilds it and removes the toll chip. Walking route has the HUD and no toll. 0 crashes, 0 ANRs.
- Screens seen and fine: licences page with Organic Maps and OpenStreetMap credits, FAQ, search bar in both languages, stacked speed pill (clear of the buttons, day, night, landscape), Malaysia first view.

Not fixed: monkey on #45's own branch aborted on the debug-only `ASSERT(mark, ...)` at `framework.cpp:2481`, upstream code, once in about 8 rounds. Release falls through to `FindUserMarkInTapPosition`. CLAUDE.md says no defensive code for it.

Checked and found fine, don't redo: `RouteTolls.requestId` shared by the preview and the HUD can't drop a result, because the preview only computes while planning.

Lessons:
- After a push, check `git merge-base --is-ancestor` for every PR head against the rollup, instead of trusting the log.
- A smoke walk that prints "not found" passes silently. Read the step list, not just the exit code.
- `ci/route-screens` is shared by #42 and #50, whichever finished last. Check the SHA in its commit message; #50's run fails on its own because it has no #32 or #49.
- Pushing to the rollup cancels every running job on it. Hold small test changes until the running jobs finish.
- A session that opened a PR stays subscribed: an earlier run's session pushed the same reroute fix seconds before this run tried to. Read what landed, keep theirs, add only the difference.
- Check that a commit message's claim is true in the code: "our ANRs still fail the run" wasn't, until the follow-up commit.
Note from run 6's PR watch (Fri 06:26 MYT): #45's smoke run on bbf8dca7 had a real native crash, the first seen, in monkey at event 1055: Debug `ASSERT(mark, ...)` at `libs/map/framework.cpp:2481`, in `BuildPlacePageInfo`. It looks like a tap on a user mark that was already gone (read from the stack, not reproduced). Release compiles the assert out and falls back to `FindUserMarkInTapPosition`. Timing-dependent, so the smoke job can go red at random on any branch. If it repeats on the rollup, find which marks are removed between tap and task. Don't delete the assert to get green. Details in the #45 comment.

Run 8 close: #52's WebDebug failed in 21 s in "Run tests" and passed on re-run, so it was setup. Smoke on 14154a84 went red once (Google Search ANR stopped monkey), then green on a re-run by another session: 0 crashes, 0 ANRs, no old brand. #42 stays at 14154a84 on purpose: a push would cancel its last Linux jobs and a new run wouldn't finish inside this run. It lacks only #45's two monkey commits, b7e7b0e0 and bbf8dca7. Close #42 after the review; no sprint run follows this one.

At 07:05 MYT two #42 Linux jobs were unfinished: gcc-15 Debug compiling after 70 minutes in apt, arm clang-22 Release still in apt. Not read to the end. #51 19 of 19 and #52 5 of 5 green. #45 and #50 are red on their own branches, as expected: read them on #42.

Check-in (07:55 MYT): both slow #42 jobs ended green on 14154a84, tests and drape tests included, so the rollup has no failed job. #51 and #52 unchanged and mergeable.

## Nightly run 1: Sat 21 Nov, 01:47 MYT

First run of the nightly routine (set up 20 Nov). Since 6 Nov: no merges, no comments, no new issues; all branches unchanged. 52 PRs open. Rollup #42 at 14154a84 still the latest full build: all green except two macOS jobs cancelled (no runner).

APK: rollup run 42114121588, artifact `android-WebDebug` (arm64), https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/42114121588 . Expires 5 Dec. The run-41220342185 APK in the launch plan expires 30 Nov. Android Check already uploads APKs on PRs and master pushes, so no new workflow was needed.

### PR opened
- #53 `docs/launch-checklist` -> master: `docs/SAMPAI_LAUNCH_CHECKLIST.md`. Update it on that branch each night while open. Docs only, so no CI checks run.

Launch plan schedule has slipped: Phase 0 and 1 not started; 19 Jan holds only if the Play closed test starts by about 7 Dec.

Next run: if still nothing merged, no new code PRs. Before 5 Dec re-run Android Check on `ci/sprint-rollup` so an APK stays downloadable. Small useful items that don't need Hamza: privacy policy draft (placeholders for domain), Play Data safety answers draft.

## Nightly run 2: Sun 22 Nov, 01:47 MYT

Found 10 PRs opened on 21 Nov after run 1 by another session, not logged here: a second stack based on `master`,
#54 (APK workflow) <- #55 (rebrand, `com.sampai.maps`) <- #56 (BM first) <- #57 (theme tokens) <- #58 (nav HUD)
<- #59 (toll data) <- #60 (Lapor stub) <- #61 (weekly monkey); #62 (brand sweep) <- #63 (legal pack) on #55.
It overlaps the old stack (#1-#53, `my.sampai.app`) almost everywhere; they will conflict. 63 PRs open, 0 merged.
Needs Hamza: which stack first, which app id.

### Updated PRs
- #57: Android Lint red on reviewdog warnings (font family had only API 26 attrs with minSdk 21; tokens unused in
  this PR but used by #58). Added `app:` font attrs, `tools:ignore="UnusedResources"` on token files. b254c0c2:
  Lint green. WebDebug failed on an emulator system-image download error (infra), re-run started.
  #58-#61 don't contain the fix; fine, they get it once #57 merges and they are retargeted.
- #53: checklist updated (two stacks, new APKs, app id choice, #56/#59/#60/#63 items).

APK (newest, expires 21 Dec): rebrand + legal https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/44924461671
artifact `sampai-google-debug-arm64`.

Lessons: reviewdog in Android Lint fails on warnings, but only for files in the PR's own diff; stacked PRs can be
green while their base is red. Force-push to amend is blocked by the classifier: set the author before the first push.

Next run: confirm #57 WebDebug re-run. No new code PRs until Hamza picks a stack. Then: privacy policy hosting text,
Play Data safety draft (check #63 first so it isn't duplicated).

## Nightly run 3: Mon 23 Nov, 01:47 MYT

Hamza merged the second stack into `master` at 00:49–01:45 MYT: #55, #62, #63, #64, #56–#61, #65 (audit), #66
(final audit fixes). App id is now `com.sampai.maps`. The old stack (#1–#53) is superseded; 42 of those PRs are
still open and will conflict. Don't rebase them; port single pieces only if Hamza asks.

`master` CI on d8dcdb74: strings check green; Android Beta and iOS Beta red on every push (upstream Firebase /
TestFlight jobs without secrets); Android Check, CMake, iOS Check and Android Sampai APK (run 45051266794,
dispatched by Hamza) still running during the run.

### PRs
- #67 `ci/skip-beta-without-secrets` -> master: gate both beta jobs on repo variable `SAMPAI_BETA_ENABLED`.
  Only `.github/**` changed, so no checks run on the PR itself.
- #53 checklist rewritten for the merged state (open audit findings 3, 5–10, ports needed from the old stack).
  Merges cleanly onto `master`.

APK from master: run 45051266794 succeeded, artifact `sampai-google-debug-arm64` (44 MB), expires 22 Dec.
Android Check and CMake on master were still running at the end of the run.

Next run: confirm master Android Check and CMake. Then small code items that
don't need Hamza: privacy text for GitHub map hosting (finding 5, `assets/privacy.html` + `docs/my/LEGAL.md`),
drop unused `translated_om_site_url`, then port the toll total onto #59's `TollTable` (HUD + preview).

## Nightly run 4: Tue 24 Nov, 01:47 MYT

Hamza merged #67 (beta jobs skipped) and #68 (map palette, region names, search categories and brands, README, last
upstream links) on 23 Nov. #53 closed (checklist is on `master` via #68). 18 old-stack PRs still open (#2–#50);
not touched, they overlap `master`.

`master` CI at 22e36f3c all green: Android Check, CMake, iOS Check, strings; betas skipped; weekly Android Sampai
Monkey (first scheduled run, 45222202072) passed.

APK: Android Sampai APK run 45057814862 on master at #68, artifact `sampai-google-debug-arm64`, expires 22 Dec:
https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/45057814862

### PR opened
- #69 `docs/privacy-github-maps` -> master: privacy policy (EN + BM) and `docs/my/LEGAL.md` name GitHub as the map
  host and the transfer outside Malaysia (audit finding 5); launch checklist updated for 23 Nov.

Found: iOS About still opens organicmaps.app for privacy, terms, news, support-us (`translated_om_site_url`),
`Common.contactEmail` is `ios@organicmaps.app`, and SocialMedia.swift has Organic Maps' Threads. Android is clean.

Next run: iOS About leftovers (point privacy/terms at the bundled pages or drop the rows; remove news/support-us and
the Threads link; then drop `translated_om_site_url` via Twine in a separate `[strings] Regenerated` commit). Then
port the toll total onto #59's `TollTable` if Hamza hasn't said which old PRs to keep.

## Nightly run 5: Wed 25 Nov, 01:47 MYT

No merges or comments since run 4. `master` (22e36f3c) still all green. #69 (privacy text) green, waiting for Hamza.

### PRs
- #70 `fix/ios-about-leftovers` -> master: iOS About opens the bundled privacy/terms pages (referenced from the
  Android assets so #69 doesn't conflict), drops News, Volunteer, the Organic Maps social links, donation button
  and `ios@organicmaps.app` (bug reports with logs went there). Unused strings removed, separate
  `[strings] Regenerated` commit.
- #69: launch checklist updated for 25 Nov.

CI on #70 all green (strings, Swift style, iOS Check Debug+Release, Android Sampai APK run 45453362463).
APK: master run 45057814862 (expires 22 Dec); #70 APK: https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/45453362463

Left on purpose: `translated_om_site_url` (core `GetDonateUrl`, dormant), `app.organicmaps.3daction.*` quick-action
ids (not visible), tab-bar crowdfunding icon (only shows with a server DonateUrl).

Next run: after #69 and #70 merge, move `privacy.html`/`terms.html` to `data/` with Android symlinks. Then port the
toll total onto `TollTable` (HUD + route preview) if Hamza hasn't said which old toll PRs to keep.

## Nightly run 6: Thu 26 Nov, 01:47 MYT

No merges or comments since run 5. `master` (22e36f3c) unchanged and green; #69 and #70 green, waiting for Hamza.
Toll total port skipped again: every rate in `data/my/tolls.csv` is still TBC, so it would show nothing.

Found: `data/faq.html` (iOS About → FAQ) was still the upstream Organic Maps FAQ; the checklist wrongly had it done.
Android's `FaqFragment` is unreachable. `iphone/metadata/*/description.txt` (App Store texts) also still Organic Maps.

### PRs
- #71 `docs/sampai-faq` -> master: Sampai FAQ (EN/BM/ZH) ported from closed #28, without the toll question and the
  non-existent support@sampai.my; drops `tools/unix/refresh-embedded-faq.sh`. Includes an add+revert of a
  checklist edit (force-push blocked); squash is fine.
- #69: launch checklist updated for 26 Nov (FAQ, iOS About, iOS store texts).

APK: master run 45057814862 (expires 22 Dec).

Next run: iOS App Store texts (`iphone/metadata/en-US`, `ms-MY` first; drop or regenerate the rest), then
`privacy.html`/`terms.html` to `data/` once #69/#70 merge.
CI on #71 at the end of the run: Android Lint, Fdroid/Web/Google APK builds, iOS Debug and most CMake jobs green;
iOS Release, macOS and a few CMake jobs still running. #71 APK: https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/45669267174
#69 all green after the checklist update.
Later on 26 Nov: #71 all green except the two macOS CMake jobs, cancelled twice by GitHub ("job was not acquired
by Runner", macOS arm64 capacity). Not caused by the PR (HTML + one script removed; macOS green on master). Not
re-run again; Hamza can re-run them or merge without them.

## Nightly run 7: Fri 27 Nov, 01:47 MYT

No merges or comments since run 6. `master` (22e36f3c) unchanged and green. #69 and #70 green; #71 green except
the two macOS CMake jobs GitHub cancelled (runner capacity). All waiting for Hamza.

### PRs
- #72 `rebrand/ios-store-texts` -> master: iOS App Store texts (`iphone/metadata`) were still Organic Maps' in ~50
  languages and the App Review contact was the upstream maintainer's name, email and phone. Now English + Malay only
  (same texts as the Play listing; the metadata check fills other locales from English), repo URLs, copyright
  `2026 Hamza Iqbal`, review contact empty, review notes without the upstream demo video.
- #69: launch checklist updated for 27 Nov (App Store items, privacy URL).

Still open for Hamza: support email (`contact-email.txt` still `googleplay@organicmaps.app`), App Store review
contact and keywords, a public privacy-policy URL.

APK: master run 45057814862 (expires 22 Dec); #71 APK run 45669304222 (expires 25 Dec).

Next run: `privacy.html`/`terms.html` to `data/` once #69/#70 merge; otherwise the Android `contact-email.txt`
can't be fixed without Hamza's address, so look at 3D perspective on by default in navigation (design step 3).
