# Sampai launch checklist

Updated by the nightly run. Last update: Sun 22 Nov 2026.
Goal: a Google Play closed test, then the public launch planned in `docs/LAUNCH_PLAN.md` (PR #25) for Tue 19 Jan 2027.
Background for each item: `docs/SAMPAI_FRIDAY_REVIEW.md` on branch `sprint/sampai-nov`.

Legend: [x] done, [ ] left to do, **(Hamza)** only Hamza can do it.

## Where things stand

- 63 PRs are open, nothing is merged into `master` yet.
- **Two stacks now overlap.** The old stack (#1–#53, `applicationId` `my.sampai.app`) was test-merged and is green on
  rollup #42 (14154a84). A second stack opened on 21 Nov (#54–#63, `com.sampai.maps`) redoes rebrand (#55 vs #3/#26),
  theme (#57 vs #1/#31), HUD (#58 vs #1), tolls (#59 vs #2/#43) and adds BM-first (#56), reports stub (#60) and a
  legal pack (#63). They will conflict; merge one stack first, then the other gets rebased onto it.
  **(Hamza)** pick which stack goes first and which app id to keep.
- New stack CI: all green as of 22 Nov (#57's lint warnings fixed tonight).
- Schedule: Phase 0 (phone test, 2–8 Nov) and Phase 1 (friends beta, 9–22 Nov) haven't started. The 19 Jan date still
  holds only if the Play closed test starts by about Mon 7 Dec and open testing is cut from four weeks to three.

## Install it on your phone

- [x] CI builds a debug APK on every PR and on `master` (Android Check, `android-WebDebug` arm64 and
  `android-FdroidDebug` arm32 artifacts).
- [x] Latest full build, every PR combined: rollup #42, run
  [42114121588](https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/42114121588), artifact
  `android-WebDebug`. **Expires 5 Dec 2026.** Download while signed in to GitHub, unzip, install the `.apk`.
- [x] Newest builds (expire 21 Dec 2026): new-stack rebrand + legal, run
  [44924461671](https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/44924461671), artifact
  `sampai-google-debug-arm64`; new-stack HUD + reports, run
  [44915287884](https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/44915287884), artifact `android-WebDebug`.
- [ ] After #23 is merged, every push to `master` makes a fresh APK. Until then, re-run Android Check on
  `ci/sprint-rollup` before 5 Dec so a download stays available.
- Debug builds are signed with the repo's public debug key: fine for you and friends, never for a store.

## 1. Branding

- [x] App name, launcher icon, splash, About, FAQ, menus, share texts, rate link: Sampai
  (#3, #19, #26, #28, #33, #37, #38, #40).
- [x] Donate, crowdfunding and Organic Maps community links removed (#26).
- [x] `applicationId` `my.sampai.app` (#3). The new stack uses `com.sampai.maps` (#55). **(Hamza)** keep one; it
  can't change after the first Play upload.
- [x] Store listings: only Sampai text left, en-US and ms-MY rewritten (#29).
- [x] Open-source licences screen credits Organic Maps and third-party libraries; OpenStreetMap credit on the map
  (#28).
- [ ] **(Hamza)** Merge the stack in the order in the Friday review, section 3.
- [ ] **(Hamza)** Decide the Organic Maps data licence question (Friday review, section 1) before any build reaches
  users. #30 (on-map credit wording) waits on it.
- [ ] **(Hamza)** Confirm the logo (`design/brand/sampai-icon.svg`) or send final art.
- [ ] Left in code on purpose, not on screen: `app.organicmaps` package names, the `omaps.app` link host.

## 2. Look and feel

- [x] Sampai theme and palette (#31), map colours day and night (#41), Plus Jakarta Sans (#34), new HUD (#1),
  UI polish (#24, #47, #49), search bar (#52), opens on Malaysia (#51), Malaysian region names (#48).
- [ ] Map colours checked on a real Malaysian city, not only the test map.
- [ ] **(Hamza)** Bahasa Melayu as the default language: #56 switches English phones in Malaysia to Malay unless the
  user picked a language. Merging it settles the open decision in `docs/MALAYSIA.md`.

## 3. Malaysian features

- [x] Toll totals in RM in the preview and the HUD, 52 open-system plazas (#2, #27, #32, #35, #36, #43).
- [ ] Closed-system fares (PLUS, LPT) and Sungai Besi: not priced, routes show "RM x+".
- [ ] Speed cameras: not started.
- [ ] Toll plaza data file, 75 PLUS plazas from OSM, all rates TBC (#59).
- [ ] User reports (police, accident, flood): designed in `docs/REPORTS.md`; a local-only "Lapor" button that saves
  to the phone is in #60, nothing is sent. **(Hamza)** needs hosting.
- [ ] Live traffic: needs a data source and a back-end decision. **(Hamza)**. Nothing built or signed up for.

## 4. Before a public beta

- [ ] **(Hamza)** Phone test: a real drive with tolls and voice, a real map download (Friday review, section 5.2).
- [ ] **(Hamza)** Map download server: the app still downloads maps from Organic Maps' servers (`private.h`).
  Self-hosting means generating maps with `tools/python/maps_generator` and a server budget.
- [ ] **(Hamza)** Own OpenStreetMap OAuth app: login still uses Organic Maps' client id (`private.h`, security item
  S-1). Register it at openstreetmap.org, then the id goes into `private.h`.
- [ ] **(Hamza)** Domain and contacts: `sampai.my` and `support@sampai.my` are placeholders.
- [ ] Privacy policy and terms pages: drafted offline in EN and BM in #63 (BM needs a native check). PDPA position
  (security item S-15) and a public URL for the Play listing still needed.
- [ ] Play Data safety form answers (drafted from the privacy policy).
- [ ] Store screenshots: none yet. Can be taken on the emulator from a real map once one is available.
- [ ] Crash reporting: none in debug builds. Firebase Crashlytics needs a `google-services.json` from Hamza's own
  Firebase project **(Hamza)**; it is a build-time option, off when the file is missing.
- [ ] Native reads: Tamil UI (#46) and the Malay FAQ.

## 5. Play Store (all Hamza)

- [ ] **(Hamza)** Play Console personal account and identity check (US$25).
- [ ] **(Hamza)** Upload key and Play App Signing. Keys never go into this repo.
- [ ] **(Hamza)** Release build of the `google` flavour, signed with the upload key.
- [ ] **(Hamza)** Closed test: at least 12 testers opted in for 14 days in a row (plan: recruit 30).
- [ ] **(Hamza)** Legal review of `DATA_LICENSE.txt` and the README's request for forks.
