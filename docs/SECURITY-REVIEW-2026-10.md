# Sampai security review — October 2026

Defensive review of the Sampai fork (`hamzaiqbal-dev/malaysia-nav`, forked from Organic Maps) covering committed
secrets and signing, Android app hardening, network and data handling, privacy, CI and supply chain, and the
design of the not-yet-built reports back-end. Reviewed at `feature/sampai-review-url`, the top of the 12-PR
stack, on 30 Oct 2026.

Scope note: the fork's own code -- `libs/my_tolls/`, `android/.../my/`, the Bahasa Melayu strings and the Sampai
rename -- was reviewed line by line. Upstream Organic Maps code was reviewed where the fork depends on it for a
security property, and findings in it are marked `upstream`. Upstream code was not churned for style.

## Severity summary

| ID | Severity | Finding | Status |
|---|---|---|---|
| S-1 | Medium | Sampai inherits Organic Maps' OpenStreetMap OAuth credentials via the tracked `private.h` | **Blocks release** |
| S-2 | High | Cleartext HTTP permitted for all domains, while map downloads have no integrity hash | Fixed in #14 |
| S-3 | Medium | 14 CI workflows declared no `permissions:`, falling back to the repository default | Fixed in #15 |
| S-4 | Medium | Nine third-party GitHub Actions pinned to mutable tags, not commit SHAs | Deferred -- see below |
| S-5 | Medium | `.gitignore` covered only upstream's exact credential filenames | Fixed in #13 |
| S-6 | Medium | Reports back-end: `X-Device-Key` is client-minted, so per-device rate limits are bypassable | Documented in #16 |
| S-7 | Low | `FileProvider` exposes the whole external storage root (`upstream`) | Deferred |
| S-8 | Low | `allowBackup="true"` sends bookmarks and settings to the user's cloud backup (`upstream`) | Your decision |
| S-9 | Low | WebView Safe Browsing disabled app-wide (`upstream`, deliberate) | Accepted |
| S-10 | Low | `nativeFindTollBooths` does not null-check its `jdoubleArray` argument | Deferred |
| S-11 | Low | `TollTable.parse` throws on a malformed bundled CSV | Deferred |
| S-12 | Info | Debug keystore and its password committed (`upstream`, standard practice) | Accepted |
| S-13 | Info | Crashlytics is off for release builds; `-Pfirebase=true` would turn it on | Accepted |
| S-14 | Info | `mpandroidchart` is an alpha of a community fork | Monitor |
| S-15 | **Decision** | Privacy notice and PDPA position required before public release | **Needs your decision** |

## S-1 (Medium) -- Sampai inherits Organic Maps' OpenStreetMap OAuth credentials

`private.h` at the repository root is tracked by git and three of its macros hold real-looking credential
values -- 24+ characters, token-shaped, not placeholder or filler text:

- `OSM_CONSUMER_KEY`
- `OSM_CONSUMER_SECRET`
- `OSM_OAUTH2_CLIENT_SECRET`

The other sensitive macros (`USER_BINDING_PKCS12`, `USER_BINDING_PKCS12_PASSWORD`) are empty. The remaining
non-empty macros are endpoint URLs, an OAuth2 client id, a redirect URI and scope, and a Kayak affiliate id.
`TRAFFIC_DATA_BASE_URL`, `DIFF_LIST_URL` and `MWM_GEOLOCATION_SERVER` are empty.

**No value is reproduced here, or anywhere in this review, or in any commit or pull request description.**

### These are upstream's credentials, not ours, and nothing of ours is exposed

This was initially assessed as a leak introduced by this fork. It is not. The evidence:

- The blob at the stack tip (`a4002ef75ff758d8081ba78c74377a981fedc193`) is byte-identical to the blob
  committed by upstream commit `08598e1417` ("Refactor product and meta configs", 13 Aug 2025), which is an
  ancestor of our `master`.
- All six commits that have ever touched `private.h`, from 2021 to 2025, are by Organic Maps maintainers
  (Alexander Borsuk, Roman Tsisyk, Viktor Govako). **This fork has never modified the file.**

So these values have been public in upstream's public repository by upstream's own hand, and they are published
there independently of anything we do. There is consequently:

- **nothing for you to rotate** -- the credentials are not yours, and you have no access to the OSM account
  that registered that application;
- **no point purging history** -- a rewrite of this fork would not unpublish values that remain in upstream's
  repository;
- **no secret of yours exposed** -- `secure.properties`, the release keystore and `google-services.json` are
  all correctly gitignored and absent, and the twelve-PR stack added no secrets at all.

This is a common and deliberate trade-off in open-source apps: a mobile app is a public OAuth client and cannot
truly hold a client secret, so the credentials ship in the open repository. Upstream separately moved its
genuinely private material to GitHub Secrets in Dec 2024 (`e00f33a076`), which is consistent with the tracked
`private.h` holding only the values they accept as public.

### Why it still blocks release

1. **Identity.** A released Sampai would authenticate to OpenStreetMap *as the Organic Maps application*. Edits
   made by Sampai users would be attributed to Organic Maps' OAuth client, which misattributes your users' work
   and amounts to presenting your app as theirs.
2. **Silent breakage.** Organic Maps can rotate those credentials whenever they like. If they do, the
   OpenStreetMap login and editing features in Sampai stop working with no warning and nothing you can do
   except ship an update.
3. **Terms.** Using another project's OAuth client credentials in a published application is not acceptable
   practice and is very likely outside both the OpenStreetMap API terms and Organic Maps' expectations.

### What to do before public release

1. **Register Sampai's own OAuth 2 application** at openstreetmap.org, under an account you control, with its
   own redirect URI. This is the whole fix.
2. **Supply your values through the `PRIVATE_H` CI secret**, as `docs/CREDENTIALS.md` already describes, and
   keep them out of commits locally with `git update-index --skip-worktree private.h`. Do **not** rely on
   `.gitignore`: `private.h` is a tracked file, so an ignore rule has no effect on it, which is why #13
   deliberately does not add one.
3. **Do not commit your own credentials** even though upstream commits theirs. Upstream's keys are already
   public and theirs to expose; yours would be a new disclosure, and unlike upstream you have no reason to make
   it.
4. Until you have your own application registered, keep OpenStreetMap editing out of any build you publish.

Optional courtesy, not a requirement: if you want certainty that upstream intends those values to be public,
ask the Organic Maps maintainers. **Upstream's intent is inferred here, not verified** -- the inference rests on
the file being committed and refreshed repeatedly by maintainers over four years, alongside a separate move of
private material into GitHub Secrets. There is no indication of an accident, but it is their call to confirm.

## S-2 (High) -- Cleartext HTTP was permitted everywhere. Fixed in #14

`network_security_config.xml` set `cleartextTrafficPermitted="true"` in the base config, covering every domain,
and then again in a `domain-config` for `organicmaps.app` and `omaps.app`.

This matters more than usual here. Downloaded map files carry no hash: the error-handling policy in `CLAUDE.md`
trusts them specifically because they come over TLS from known servers. There is no SHA or signature anywhere in
`libs/storage/` or `libs/platform/` to fall back on, so TLS is the *only* integrity control on map data. And the
download URLs are not hardcoded -- they are fetched from the metaserver at runtime, and `ParseMetaConfig`
accepts whatever server list it is given, including `http://` URLs (see `libs/platform/platform_tests/
meta_config_tests.cpp`). An attacker positioned on the network could therefore answer the metaserver request
with plain-HTTP mirrors and then serve altered map data to a driver following a route.

Nothing in the app needed cleartext: `private.h` defines only `https://` endpoints, and the only use of
`DEFAULT_CONNECTION_CHECK_IP` is a raw socket in `platform_linux.cpp`, not an HTTP request. #14 sets
`cleartextTrafficPermitted="false"` and drops the redundant `domain-config`.

Side benefit: Wikipedia place descriptions are rendered in a WebView from map data, and any `http://` image or
stylesheet embedded in that text would previously have been fetched, revealing the reader's IP address and which
place they were looking at. Those subresources are now blocked.

**Please verify on a device before merging**: download a map region and open a place description. If some mirror
turns out to be HTTP-only, add a `domain-config` for that one domain rather than reopening cleartext globally.

## S-3 (Medium) -- CI workflows relied on the repository default token. Fixed in #15

Fourteen of the 24 workflows declared no `permissions:` at all, so their `GITHUB_TOKEN` fell back to the
repository-wide default. Where that default is "read and write", a lint job triggered by a pull request runs
holding a token that can push to the repository -- which is the first thing a compromised third-party action
reaches for.

I could not read the repository's current default setting: this environment's proxy blocks the GitHub Actions
API (`/actions/permissions/workflow` returns 403), so **whether the default is currently read-only is
unverified**. Worth checking under Settings → Actions → General → Workflow permissions; either way, #15 makes
every workflow explicit so the default no longer decides.

Audited and clean: there is **no `pull_request_target` anywhere** in the repository, so the dangerous
"untrusted PR code with access to secrets" pattern does not occur. The three style workflows that the stack
changed to run on stacked PRs use plain `pull_request`, which is the safe trigger.

## S-4 (Medium) -- Third-party actions are pinned to mutable tags. Deferred

A tag can be moved to point at new code, so a tag-pinned third-party action is a standing supply-chain risk: the
author (or anyone who compromises their account) can change what runs in your CI retroactively. Only one action
in the repository is SHA-pinned.

Tag-pinned, in `.github/workflows/`:

| Action | Pin |
|---|---|
| `TheMrMilchmann/setup-msvc-dev` | `v4.1.0` |
| `google-github-actions/setup-gcloud` | `v3` |
| `hendrikmuhs/ccache-action` | `v1.2.24` |
| `ilyagulya/android-lint-action` | `v0.12.1` |
| `irgaly/xcode-cache` | `v1` |
| `jurplel/install-qt-action` | `v4` |
| `reactivecircus/android-emulator-runner` | `v2` |
| `reviewdog/action-setup` | `v1` |
| `softprops/action-gh-release` | `v2` |

Already correct: `reviewdog/action-suggester@aa38384ceb608d00f84b4690cacc83a5aba307ff`.

Reported rather than fixed: resolving each tag to its commit SHA requires API access to those nine repositories,
which this environment does not have, and I was asked not to touch other repositories. Pinning them by guess
would be worse than leaving them. The highest-value ones to pin first are the two that handle credentials or
write to the repository: `softprops/action-gh-release` (uses the release token) and
`ilyagulya/android-lint-action` (receives `GITHUB_TOKEN` and posts PR reviews). `actions/*` and `github/*`
actions are first-party and lower risk.

## S-5 (Medium) -- `.gitignore` only covered upstream's exact filenames. Fixed in #13

`android/app/.gitignore` lists `release.keystore`, `secure.properties`, `google-services.json` and the other
names upstream uses. A release keystore under any other name -- `sampai-release.jks`, which is exactly what you
are likely to create -- an `.env` file, or a PKCS#12 certificate was not covered, and a single `git add -A`
would have committed it. You will be creating exactly such a keystore, and S-1 means you will also be editing
`private.h` by hand, so this is a live failure mode rather than a hypothetical one.

#13 ignores the credential file *types* repo-wide while leaving `android/gradle.properties`,
`gradle-wrapper.properties` and the bundled CA roots tracked.

## S-6 (Medium) -- Reports back-end identity model. Documented in #16

The planned live road-reports service authenticates anonymous reports with `X-Device-Key`, a 128-bit value the
client generates itself. Every rate limit in the design is per device key, so all of them can be bypassed by
minting keys in a loop. Separately, the 30-day key rotation silently clears the 30-day shadow ban it is meant to
work alongside.

Full treatment, including report-id guessability, storage and retention separation, note-field handling and the
PDPA analysis, is in `docs/REPORTS-SECURITY.md` (#16). Five decisions are listed there; none of them can be
settled without you.

## Android hardening -- what was checked and found clean

Reviewed in `AndroidManifest.xml` and the Kotlin/Java, with no change required:

- **Exported components.** Four are exported and all four need to be: `SplashActivity` (deep links and file
  opening), the launcher `activity-alias`, the `ManageSpaceActivity` alias the system settings page invokes, and
  `AndroidAutoService`. Every service, receiver and provider that does not need exporting is explicitly
  `android:exported="false"`. Nothing is over-exposed.
- **`PendingIntent` mutability.** Every one of the ten construction sites sets `FLAG_IMMUTABLE` (with the
  correct pre-API-23 fallback). No implicit or mutable PendingIntent.
- **`android:debuggable`.** Not set anywhere; debuggability comes only from the `debug` build type.
- **WebViews.** Four exist. The two with JavaScript enabled (`WebContainerDelegate` for FAQ and copyright,
  `AdvancedTimetableFragment`) load only `file:///android_asset/` documents shipped in the APK. The two that
  render untrusted content -- `WikiArticleFragment` (place descriptions from map data) and `ExpandableNotesView`
  -- both have JavaScript disabled, and the latter uses a `null` base URL. No `addJavascriptInterface` anywhere,
  and no `setAllowFileAccessFromFileURLs` / `setAllowUniversalAccessFromFileURLs` loosening.
- **Permissions.** The requested set is proportionate for an offline navigation app: coarse and fine location,
  internet, network state, wake lock, the four foreground-service types it actually runs, notifications, and
  legacy external storage read capped at `maxSdkVersion="22"`. Nothing unused. `permission-checker.gradle`
  enforces the allowlist at build time, and the stack correctly updated it for the `my.sampai.app` rename.
- **Intent and deep-link handling.** The fork's own additions (`ExploreChips`, the HUD, `RouteTolls`) take no
  data from intents -- the chip queries are hardcoded constants. No new deep-link surface was added.
- **TLS.** No weakened certificate or hostname verification anywhere: no permissive `TrustManager`, no custom
  `HostnameVerifier`, no `setAllowsAnyHTTPSCertificate`. `Android7RootCertificateWorkaround` *narrows* trust
  rather than widening it -- on API ≤ 24 only, it builds a trust store from six bundled public CA roots and
  uses a standard `TrustManagerFactory`, so chain and hostname validation still apply.

### S-7 (Low) -- FileProvider path breadth (`upstream`)

`res/xml/file_paths.xml` declares `<external-path name="external" path="." />`, exposing the entire external
storage root through the provider, plus the app's own files, cache and external files directories. The provider
is `exported="false"` and relies on `grantUriPermissions`, so another app cannot read arbitrary files on its
own -- it would need Sampai to hand it a URI. Upstream needs this breadth because users export and import
KML/GPX from arbitrary locations. Narrowing it risks breaking bookmark export for real users, so I have not
touched it; worth revisiting if you ever add a code path that grants a URI built from a filename supplied by
another app.

### S-8 (Low) -- Cloud backup of bookmarks and settings (`upstream`, your decision)

`allowBackup="true"` with `backupInForeground="true"`, and `backup_content.xml` / `backup_content_v31.xml`
including `bookmarks/` and `settings.ini` for both cloud backup and device transfer. So a user's saved places --
home, work, the places they care about -- leave the device into their own Google account backup, encrypted in
transit and at rest by the platform but outside your control and outside Malaysia.

This is upstream's deliberate choice (bookmarks surviving a phone change is a real user benefit) and it is the
user's own backup, not a transfer to you. But it is a location-data export that your privacy notice must
mention, and it is the one place where Sampai data leaves the country by default. Options: leave as is and
disclose it; or exclude `bookmarks/` and accept that users lose saved places when they change phones. I would
leave it and disclose it.

### S-9 / S-10 (Low) -- Robustness in the fork's own toll code. Deferred

- `Java_app_organicmaps_sdk_my_MyTolls_nativeFindTollBooths` calls `GetArrayLength` on its `jdoubleArray`
  without a null check. Passing `null` from Java is undefined behaviour in JNI rather than a Java exception.
  Not reachable today -- `RouteTolls.compute` null-checks the polyline before the call and `MyTolls.java` marks
  the parameter `@NonNull` -- but `MyTolls` is a public SDK class, so a future caller could. A one-line `CHECK`
  would match the house style; I left it rather than spend a PR on an unreachable path.
- `TollTable.parseLine` uses `require(...)` and `toDouble()`, so a malformed `my_tolls.csv` throws on the first
  toll lookup of a route rather than degrading. The CSV is a bundled asset, so per `CLAUDE.md`'s error-handling
  policy that is acceptable (bundled data is trusted, crashing on corrupted data is fine). Flagging only because
  if toll prices ever become a downloaded file, this parser becomes a remote crash vector and must change.

### S-12 (Info) -- Committed debug keystore (`upstream`)

`android/app/debug.keystore` is committed and `application-signing.gradle` hardcodes its password. This is
standard Android practice -- debug keystores are shared, publicly known and only ever sign `.debug` builds. The
release config reads `app/secure.properties`, which is correctly gitignored and absent. No action, but: never
point the release signing config at the debug keystore, and keep your real keystore and its passwords off this
repository entirely. Losing it means you can never update Sampai on Play again, so back it up somewhere that is
not a git repository.

## Network and data

- **Endpoints actually contacted.** The metaserver (`METASERVER_URL`, HTTPS) for the download server list, and
  the map download mirrors it returns. That is all, for a normal session.
- **Features that are compiled in but disabled** because their `private.h` macros are empty: traffic data
  (`TRAFFIC_DATA_BASE_URL`, guarded by an explicit empty check in `traffic_info.cpp`), map diffs
  (`DIFF_LIST_URL`), and IP geolocation (`MWM_GEOLOCATION_SERVER`, which is empty *and* referenced nowhere in
  the codebase). None of these make requests as shipped.
- **Download integrity.** No hash, SHA or signature verification exists in the map download path, by upstream
  policy. TLS is the whole of it -- which is the reasoning behind S-2.
- **The fork added no network code.** The toll feature reads a bundled CSV asset (`android/app/src/main/assets/
  my_tolls.csv`) and the map's own toll-booth nodes via JNI. No endpoint, no key, no remote fetch. `my_config.xml`
  contains only booleans and dimensions.
- **Secrets in the 12-PR stack.** Scanned every added line across `master..feature/sampai-review-url` for
  key-, token- and private-key-shaped strings: zero hits. The stack is clean; S-1 is upstream's file, inherited
  and never modified here.

## Privacy -- what leaves the device

As the app currently stands, with Sampai's own additions included:

| Leaves the device | To where | When | Contains location? |
|---|---|---|---|
| Map download requests | Metaserver, then map mirrors (HTTPS) | When the user downloads or updates a region | Only which region was chosen |
| OpenStreetMap edits | openstreetmap.org, via OAuth under the user's own OSM account | Only when the user edits a place and uploads | Yes -- the edited place |
| Bookmarks and `settings.ini` | The user's own cloud backup (Google) | Automatic platform backup | Yes -- saved places (S-8) |
| Outbound link taps | Whichever site (OSM, Wikipedia, Kayak with an affiliate id, donation links) | Only on an explicit tap | Reveals the place being viewed |

Does **not** leave the device: the live GPS position, the route, search queries, recorded tracks, or any
analytics. Routing, search and rendering are entirely offline. There is no telemetry. The fork's toll
calculation is local.

**S-13 (Info) -- Crashlytics.** Firebase Crashlytics is a dependency but is wired behind a compile-time flag:
it is enabled only for `beta` non-`fdroid` builds *and* only if `android/app/google-services.json` exists (it is
gitignored and absent). So `assembleGoogleRelease` ships no Firebase and no telemetry. If you ever pass
`-Pfirebase=true` or add `google-services.json`, crash reports including device and OS details start flowing to
Google and that becomes a disclosure you must make. I added no telemetry.

### S-15 (Decision) -- PDPA 2010 (as amended 2024) before public release

You will own Sampai personally and are based in Malaysia, so this is yours. The app as it stands today is in a
strong position: no account, no telemetry, no location leaving the device. The exposure arrives with the reports
service, which uploads positions by design.

What the amended Act requires, with dates (Personal Data Protection (Amendment) Act 2024, assented 9 Oct 2024,
gazetted 24 Dec 2024, commenced in phases on 1 Jan, 1 Apr and 1 Jun 2025):

- **Breach notification**, in force 1 Jun 2025: notify the Commissioner "as soon as practicable and within 72
  hours" of a breach; notify affected individuals "without unnecessary delay and within seven days of notifying
  the Commissioner" where significant harm is likely.
- **DPO appointment**, in force 1 Jun 2025: mandatory where processing involves personal data of more than
  20,000 data subjects, sensitive personal data of more than 10,000, **or** "regular and systematic monitoring"
  of personal data. That third trigger has no volume threshold and may catch a continuously polling location
  service from day one.
- **Penalties**, from 1 Apr 2025: maximum fine for breaching the protection principles rose from RM300,000 to
  RM1,000,000; maximum imprisonment from 2 to 3 years. "Data user" is now "data controller".
- **Cross-border transfers**, from 1 Apr 2025: the whitelist was removed in favour of a
  "substantially similar protection" test.

Decisions needed before Sampai is public:

1. **Publish a privacy notice**, in Bahasa Melayu and English, inside the app and on the listing. Google Play
   requires a privacy policy URL and a Data Safety declaration for any app requesting location, independently of
   Malaysian law. This is required even for today's build, because of S-8 and the location permission.
2. **Decide whether the PDPA applies at all today.** The Act governs processing "in respect of commercial
   transactions". A free app with no monetisation may sit outside it; ads or a paid tier bring it in squarely. I
   have **not verified** how the Commissioner reads that phrase for a free consumer app -- get Malaysian advice
   rather than relying on either reading, and redo the analysis the day you monetise.
3. **Get advice on the "regular and systematic monitoring" DPO trigger** before the reports service launches.
   Appointing a DPO contact is close to free for a sole founder; guessing wrong is not.
4. **Settle data residency for the reports service**, including the CDN contradiction flagged in
   `docs/REPORTS-SECURITY.md`.
5. **Have a breach process on paper before launch.** 72 hours is not enough time to invent one.

Sources for the dated statements: DFDL, "Malaysia: Implementation of the Personal Data Protection (Amendment)
Act 2024"; DLA Piper Privacy Matters, "Malaysia: Guidelines issued on data breach notification and data
protection officer appointment", 4 Mar 2025. Both read on 30 Oct 2026. These are practitioner summaries, not the
gazetted text -- cite the Act itself in anything you publish.

## Supply chain

### S-14 (Info) -- Dependencies

`android/gradle/libs.versions.toml` is in good shape: AGP 9.4.1, OkHttp 5.5.1, Kotlin/AndroidX and Google Play
services all current, and the pinned-back versions carry comments explaining the `minSdk 21` constraint. Nothing
obviously unmaintained or known-vulnerable.

Two notes rather than findings:

- `devnullorthrow-mpandroidchart = "3.2.0-alpha"` is an alpha build of a community fork of MPAndroidChart (the
  original is unmaintained). That is a reasonable choice given the original's state, but it is a single-
  maintainer fork on an alpha version in your dependency graph -- worth watching.
- **No CVE scan was performed.** I compared versions against current releases and checked for abandoned
  packages, but did not run a vulnerability database against the full transitive graph. Enabling Dependabot or
  `gradle dependencyCheck` on this repository would cover that properly and continuously. Treat "nothing
  obviously vulnerable" as the limited claim it is.

## Explicitly unverified

Listed so none of it reads as cleared:

1. **Whether upstream intends the S-1 credentials to be public.** Established by git: the file is upstream's,
   byte-identical to their commit, never touched by this fork. Inferred, not confirmed: that they are content
   for those values to be public. Only the Organic Maps maintainers can confirm that, and it does not change
   what you need to do (register your own OAuth application).
2. **The repository's default workflow token permission** -- the Actions API is blocked by this environment's
   proxy. #15 removes the dependency on it either way.
3. **Whether every map mirror supports HTTPS** -- S-2's fix needs a real device test of a map download.
4. **PDPA scope for a free app** -- the "commercial transactions" question needs Malaysian counsel.
5. **Transitive dependency CVEs** -- no vulnerability database was consulted.
6. **CI results for this stack** -- the PRs were opened with checks queued; this environment's proxy blocks
   Actions log storage, so verification was done locally (YAML parsing, XML validity, `git check-ignore`
   behaviour) rather than by reading CI logs. #14 and #15 were confirmed green, including both Android debug
   builds, before the documentation PRs were opened.
7. **The content of upstream's 45,000-commit history.** The secret scan covered the current working tree in
   full, every filename ever added across all branches, and the complete added-line content of this fork's own
   work (`master..feature/sampai-review-url`). It did not scan the diff content of upstream's entire history,
   which is impractical and is Organic Maps' to audit rather than ours. If a secret of *theirs* is buried in an
   old upstream commit, this review would not have found it.

## The short version

The fork's own twelve PRs are clean: no secrets added, no new network surface, no unvalidated intent handling,
and the toll code is sound. Upstream Organic Maps is a careful codebase -- immutable PendingIntents, no
loosened TLS, no over-exported components, proportionate permissions, telemetry off by default.

No secret of yours is exposed anywhere in this repository, and there is nothing here you need to rotate. The
credential finding (S-1) turned out to be upstream's own published OAuth credentials, inherited untouched by
the fork.

Two things to carry forward. Before Sampai is public, **register its own OpenStreetMap OAuth application**
(S-1) and **publish a privacy notice, with a decided PDPA position** (S-15) -- Google Play requires the former
of any app requesting location, regardless of Malaysian law. Everything else on this list is hardening, and the
highest-value remaining item is pinning the two credential-handling GitHub Actions to commit SHAs (S-4).
