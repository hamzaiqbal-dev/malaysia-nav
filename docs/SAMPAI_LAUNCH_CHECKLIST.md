# Sampai launch checklist

Updated by the nightly run. Last update: Sun 29 Nov 2026.
Goal: a Google Play closed test, then a public launch. Background: `docs/my/AUDIT-2026-11-22.md` on `master`.

Legend: [x] done, [ ] left to do, **(Hamza)** only Hamza can do it.

## Where things stand

- 23 Nov: the second stack is merged into `master` (#54–#66). That settles the app id (`com.sampai.maps`), Bahasa
  Melayu first, the theme tokens, the HUD, toll data v1, the Lapor stub, Sampai's own map files and the legal pack.
- 23 Nov: #67 (beta jobs skipped without secrets) and #68 (map palette, Malaysian region names, search categories
  and brands, README, last upstream links) merged. `master` CI is green; the weekly monkey test passed.
- 18 old-stack PRs are still open (#2–#50). Most overlap `master` now. **(Hamza)** close them, or name the ones you
  still want ported: the toll total (#2/#27/#32/#35/#36/#43), quick chips (#6), Tamil UI (#46).

## Install it on your phone

- [x] Workflow "Android Sampai APK" builds a debug APK from `master` (artifact `sampai-google-debug-arm64`). Latest
  from `master` at #68 (23 Nov, succeeded): [45057814862](https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/45057814862),
  expires 22 Dec 2026.
  Download while signed in to GitHub, unzip, install the `.apk`. Artifacts expire after 30 days, so re-run the
  workflow (Actions → Android Sampai APK → Run workflow) for a fresh one.
- Debug builds are signed with the repo's public debug key: fine for you and friends, never for a store.

## 1. Branding

- [x] App name, launcher, About, menus, share texts, Play store listings, watch app: Sampai (#55, #62, #66).
- [ ] In-app FAQ (`data/faq.html`, shown on iOS) was still the upstream page: Sampai FAQ in EN/BM/ZH in #71.
- [ ] iOS About: Organic Maps website links, socials, donation and `ios@organicmaps.app` removed in #70.
- [ ] iOS App Store texts were still Organic Maps' in ~50 languages, and the App Review contact was the upstream
  maintainer's: Sampai texts in English and Malay, review contact emptied, in #72.
- [ ] **(Hamza)** App Store Connect: review contact (name, email, phone), copyright holder (#72 says
  `2026 Hamza Iqbal`; change it if a company publishes), and keywords (kept out of git, as upstream does).
- [x] Donate, crowdfunding and Organic Maps community links removed (#62).
- [x] `applicationId` `com.sampai.maps` (#55). Can't change after the first Play upload.
- [x] Open-source licences screen credits Organic Maps and third-party libraries; "© OpenStreetMap contributors" on
  the map; `LICENSE`/`NOTICE` untouched (#63).
- [ ] **(Hamza)** Support email: `SUPPORT_MAIL` is empty in `android/app/build.gradle`, and
  `android/app/src/*/play/contact-email.txt` still say `googleplay@organicmaps.app` (audit finding 8).
- [ ] iOS About: fixed in #70 (bundled privacy/terms, no Organic Maps links, social, news, donation or
  `ios@organicmaps.app`). Merge #70. Then move `privacy.html`/`terms.html` to `data/` for both platforms.
  `translated_om_site_url` is still used by core `Framework::GetDonateUrl` (dormant: our server sends no DonateUrl).
- [ ] **(Hamza)** iOS support address: `AppConstants.supportEmails` is empty (bug reports open with no recipient),
  same as Android `SUPPORT_MAIL`.
- [ ] **(Hamza)** Confirm the logo or send final art.
- [ ] Left in code on purpose, not on screen: `app.organicmaps` package names.

## 2. Look and feel

- [x] Theme tokens and Plus Jakarta Sans (#57), navigation HUD (#58), Malay first (#56), opens on Malaysia.
- [x] Sampai map palette, day and night (#68).
- [x] 3D perspective and buildings on by default in navigation (design/README.md step 3): already the upstream
  default (`Framework::Load3dMode` falls back to on; tilt applies in navigation follow mode). Only power saving
  turns them off.
- [ ] Language choice in Settings: an English (Malaysia) phone is switched to Malay on every start (finding 9).
  Fixed in #73: Malay on the first start only, plus Settings > General > App language.
- [ ] Malay voice guidance: fixed in #74 (generated `ms.json`, Bahasa Melayu in the voice list), waiting for merge; supersedes #8 (finding 6).

## 3. Malaysian features

- [x] Toll plaza data file, PLUS plazas from OSM, all rates TBC (#59). Not used by the app yet.
- [x] Malaysian search categories and brands in Malay, English, Chinese and Tamil (#68).
- [ ] Toll total in RM on the route preview and HUD (was #2/#35/#43 in the old stack; needs porting on #59's data).
- [ ] Speed cameras: the upstream engine already warns from OpenStreetMap `highway=speed_camera` points; how many
  Malaysian AES/AWAS cameras OSM has is not checked yet.
- [x] Local-only Lapor button (#60): saves to the phone, nothing is sent.
- [ ] **(Hamza)** Report hosting: needed before reports can be shared between users.
- [ ] **(Hamza)** Live traffic: needs a data source and a back-end decision. Nothing built or signed up for.

## 4. Before a public beta

- [ ] **(Hamza)** Phone test from the `master` APK: install, download the Malaysia map, search, route, drive with the
  HUD on and off, file a Lapor report, open About and the legal pages, Malay and English, light and dark.
- [x] Map downloads come only from Sampai's GitHub releases (#64). No request goes to Organic Maps' servers.
- [ ] **(Hamza)** Map hosting for a public launch: GitHub Releases or an own server; a `maps/<date>` release must
  never be deleted (finding 7).
- [ ] **(Hamza)** Own OpenStreetMap OAuth app: `private.h` still has Organic Maps' client id and the `om://`
  callback (finding 3). Or hide OSM login and editing for launch.
- [x] HTTPS only (`cleartextTrafficPermitted="false"`, #66).
- [ ] Privacy policy text names GitHub as the map host and the transfer outside Malaysia (finding 5, this PR #69).
  Contact details still to be added **(Hamza)**.
- [ ] Privacy policy at a public URL (Play and the App Store require one) **(Hamza)**: where to host it. Until
  then the iOS `privacy_url.txt` (#72) points at `privacy.html` on GitHub, which shows the HTML source.
- [ ] Play Data safety form answers (drafted from the privacy policy).
- [ ] Store screenshots: none yet.
- [ ] Crash reporting: none. Firebase Crashlytics needs Hamza's own `google-services.json` **(Hamza)**.
- [ ] `SECURITY.md` and a dependency CVE list.
- [ ] Native-speaker pass over the Malay strings.

## 5. Play Store (all Hamza)

- [ ] **(Hamza)** Play Console account and identity check (US$25).
- [ ] **(Hamza)** Upload key and Play App Signing; keys only as GitHub Actions secrets, never in the repo.
- [ ] **(Hamza)** Release build of the `google` flavour, signed with the upload key.
- [ ] **(Hamza)** Closed test: at least 12 testers opted in for 14 days in a row.
- [ ] **(Hamza)** Lawyer review of `docs/my/LEGAL.md`, privacy policy and terms; trademark check for "Sampai".
