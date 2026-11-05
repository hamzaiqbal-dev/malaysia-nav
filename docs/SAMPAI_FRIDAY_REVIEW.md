# Sampai Friday review (6 Nov 2026)

Everything from the sprint is in open PRs. Nothing is merged, published or sent to a store. Read section 1 first: it's the one decision only you can make.

## 1. Decide this first: Organic Maps' data licence

The sprint brief says the Apache notices and the OpenStreetMap credit are enough to look like your own product. **That's true for the code, not for the map data.**

`DATA_LICENSE.txt` covers `.mwm`, `packed_polygons.bin` and other `.bin` files. Sampai bundles `World.mwm`, `WorldCoasts.mwm` and `packed_polygons.bin`, and downloads all country maps from Organic Maps' servers. The licence requires:

- "Map data © OpenStreetMap and Organic Maps" on the map, in About and in the main menu, with links to both sites.
- Explicit written permission for any white-labelling or branding removal (legal@organicmaps.app).

The README also asks anyone using its source code or UI to name the "Organic Maps Project" with a link in About and the main menu, and to contact them before using their servers. That's a request in the README rather than a term of the Apache licence, but weigh it with the same decision.

The sprint's rebrand goes against the data licence as it stands: #30 changes the on-map credit to "Map data © OpenStreetMap contributors", #26 and #28 leave the required wording out of About and the menu. Merging into your own repo publishes nothing, so the risk starts when a build reaches users.

Your options:

1. **Written permission** from Organic Maps. Fastest if they agree. Keeps today's plan.
2. **Your own maps:** generate them from OpenStreetMap with `tools/python/maps_generator`, host them yourself, and replace the bundled data files. Their data licence should then no longer apply, only OpenStreetMap's ODbL credit. This needs a server budget, and it's the only option that could give you the "built from A to Z" look without their permission. The README's request about source code still stands, so raise that with your lawyer too.
3. **Keep the credit:** hold #30 and add the required line to About and the main menu.

Don't ship to any store until one of these is settled. This isn't legal advice; for a commercial launch, a lawyer should read `DATA_LICENSE.txt`.

Whatever you choose, keep `LICENSE`, `NOTICE`, the per-file headers, the Open-source licences screen and the OpenStreetMap credit.

## 2. How to merge

- Use **Create a merge commit** every time. Squash or rebase rewrites commits, and the stacked PRs then conflict.
- Merge in the order below, into `integration/m1`. **#23 goes last** and carries it all to `master`, so nothing needs retargeting.
- Click **Delete branch** after each merge: GitHub then retargets the PRs stacked on it. Don't delete `integration/m1` before #23.
- Check a PR's base before you press merge. Stacked ones show `integration/m1` only once their parent is merged and its branch deleted.

The order below was test-merged: the whole list merged with no conflict. The result differs from the tested rollup (#42 at 14154a84) only by #13's `.gitignore` comment and two lines in #45's `smoke_test.py`, which went in too late for one more full run. Those two commits ran only on #45's own branch.

## 3. Merge order

### Bugs: tolls

| # | What | Check before merging |
|---|---|---|
| 27 | Toll chip no longer sticks after a route is cleared | |
| 32 | Unnamed and translated booths count; no more "RM 0.00+" | |
| 35 | Toll total in the route preview | Its commit message says Google Maps and Waze show no RM totals. That wasn't verified, so don't reuse it in store text. |
| 36 | "Batu Pahat" no longer gets the DUKE "Batu" price; no toll on walking or cycling routes | |
| 43 | A plaza's lanes no longer charge twice; NKVE's Damansara and Bukit Raja no longer get the SPRINT or NNKSB price | Not tested on real map data. Check on a phone. |

### Layout

| # | What | Check before merging |
|---|---|---|
| 24 | Spacing, contrast, touch targets, Malay text fit | |
| 47 | Chips bar no longer fills the screen and pushes the scale and credit under the status bar | |
| 49 | Speed pill no longer covers the bookmarks button | Stacked speed pill seen on the emulator: clear of the buttons, day and night, portrait and landscape. |
| 52 | New search bar in the map's bottom row (portrait) | **New.** Its base is #47. Seen on the emulator in English and Malay. Worth a look on your phone, because it's the biggest visible change. To undo it, leave #52 out. |

### Rebrand

| # | What | Check before merging |
|---|---|---|
| 26 | All app screens and strings say Sampai; donations and community links gone | See section 1 |
| 46 | Tamil UI on Android | Machine translated, 626 strings. A Tamil speaker should read it. |
| 29 | Only the Sampai store listings remain | Merge with #46: the listing says the Tamil menu exists. |
| 28 | New FAQ; Organic Maps credited on the licences page | See section 1 |
| 30 | Sampai name in exports, on-map label and app metadata | **Hold until section 1 is decided.** Merges cleanly if left out: the rest of the list merged without it, but that combination hasn't been built. |
| 33 | Remaining product names in code and workflows | |
| 44 | Linux metadata: upstream screenshots and changelog gone | |
| 37 | iOS, macOS, Windows and Qt icons and logos | |
| 38 | Shares use openstreetmap.org links, not omaps.app | |
| 39 | README rewritten for Sampai; licence sections kept | |
| 40 | iOS "Rate the app" no longer opens Organic Maps' page | Real App Store id still needed. |
| 13 | One `.gitignore` comment that missed M1 | Retargeted to `integration/m1` on 6 Nov. |

### Look

| # | What | Check before merging |
|---|---|---|
| 31 | Sampai palette replaces upstream's green | |
| 34 | Plus Jakarta Sans in the HUD and chips | |
| 41 | Sampai map colours, day and night | Seen only on the emulator's small test map. Look at a real city first. Nothing depends on it. |
| 48 | "Peninsular Malaysia" / "Semenanjung Malaysia" names | |
| 51 | App opens on Malaysia instead of the whole world | **New.** |

### Tests and docs

| # | What | Check before merging |
|---|---|---|
| 45 | Emulator walk of the app | May be red on its own branch, which lacks #47. Read it on #42. |
| 50 | Drives a route on the emulator on a made-up map | Red on its own branch, which lacks #32 and #49. Read it on #42. |
| 25 | Launch plan | |

### Last

| # | What |
|---|---|
| 23 | M1: everything into `master`. |

### Then close, unmerged

- #42: rollup, for CI only.
- #2 to #12 and #14 to #22: all their commits are already in `integration/m1`, so they arrive with #23. #1 closes itself as merged.
- Delete `ci/route-screens` and `ci/smoke-screens-*`.

## 4. What was tested

All of this ran on rollup #42 at 14154a84, which combines every PR above except the two small items in section 2:

- **CI:** Android, iOS, Windows, style, lint, translations and metadata jobs pass, and 6 of 8 Linux jobs. When this was written, `ubuntu-26.04 gcc-15 Debug` and `ubuntu-26.04-arm clang-22 Release` hadn't finished: both spent over an hour in the runner's package install, before any of our code. Check they ended green on #42. The two macOS jobs were cancelled with no runner; macOS passed on #51 alone and on the earlier rollup 47647cb9.
- **PR by PR:** no failed check on any PR's latest commit. The only unfinished jobs are macOS ones cancelled for lack of a runner. #13, #25 and #39 are docs-only and have no checks.
- **App walk on an emulator, English and Malay:** map, About, FAQ, Open-source licences, search, bookmarks, layers, menu, settings, downloader. 0 Sampai crashes or ANRs. No old brand except the licence credit, which must stay. Monkey ran 3,000 random taps across two rounds with no Sampai crash.
- **A full drive on a made-up road, English by day and Malay by night:** route preview "Toll RM 4.50+", HUD total, turn banner, lanes, speed, rotation, arrival.
- **First runs on 6 Nov:** leaving the route rebuilds it and drops the toll chip. A walking route shows the HUD with no toll.

Monkey on #45's own branch hit a debug-only `ASSERT` in `BuildPlacePageInfo` ("There is no user mark with id"), once, in upstream code this sprint didn't change. Release builds skip it and carry on, and it hasn't happened on the rollup. It's most likely a tap on a mark that had just gone; that cause isn't proven.

A first-start ANR in `DownloadResourcesActivity` showed in 2 of 6 route runs on 5 Nov: about 6 s of start-up work on the main thread, on a just-booted emulator in a Debug build. It isn't new code and isn't fixed. Check how the first start feels on a phone.

**Not tested at all:** real Malaysian roads and OSM toll tagging, a real phone, a real GPS, voice output, downloading a real map, iOS at runtime. Please try the app on a phone before anything else.

## 5. What needs you

In order of what blocks most:

1. **Section 1:** the data licence decision.
2. **A phone test** of the rollup or `master`: a real drive with tolls, voice, a real map download. Check NKVE Damansara and SPRINT Damansara for #43.
3. **Map hosting:** `private.h` still uses Organic Maps' servers (`meta.omaps.app`, `cdn*.organicmaps.app`). Web and F-Droid builds can't start without a 69 MB download from them. The README asks forks to contact them before using their servers.
4. **Real domain and contacts:** `sampai.my` and `support@sampai.my` are placeholders behind Website, Email, Report a bug, privacy, terms and the voice help link. Privacy and terms pages need writing. A real domain also unlocks Sampai share links, `om://` and the KML namespace.
5. **Accounts:** Play Store and App Store owner, App Store id for iOS "Rate the app", your own OpenStreetMap OAuth client (the login still shows Organic Maps' client).
6. **Logo:** `docs/MALAYSIA.md` lists brand and logo as open, so confirm the current mark (`design/brand/sampai-icon.svg`) or send final art.
7. **Reports back-end hosting** (`docs/REPORTS.md` section 6): the report button can't work without it.
8. **Bahasa Melayu as default language:** still open in `docs/MALAYSIA.md`.
9. **Native read:** Tamil UI (#46) and the Malay FAQ.
10. **Toll data:** PLUS and LPT closed-system fares and Sungai Besi aren't priced, so those routes show "RM x+" or no total.

Branding left in the code, on purpose: `app.organicmaps` package and class names, the Android `omaps.app` intent host for incoming links, code comments, the licence text. None of it shows on a screen.
