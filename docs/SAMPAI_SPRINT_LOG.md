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
