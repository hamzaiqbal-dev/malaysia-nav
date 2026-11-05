# Sampai launch plan

Written Sun 1 Nov 2026 for Hamza Iqbal (owner, personally; not Kelker). Android first, iOS later.
Companion docs: `docs/MALAYSIA.md` (product plan), `docs/REPORTS.md` (live reports design),
`docs/SECURITY_PENTEST_2026-10-31.md` (release blockers S-1, S-15).

## In one paragraph

Recommended official public launch: **Tuesday 19 January 2027**, on Google Play production, after a
two-week friends beta (APK), Google's mandatory 14-day closed test, and four weeks of open testing. That
date is the earliest that leaves room for the Play production review and a week of 99.9% crash-free data,
and it lands just before three long weekends of heavy driving (Thaipusam 22–24 Jan, Federal Territory Day
30 Jan–1 Feb, Chinese New Year 6–8 Feb). The Waze-ban news is used from mid-December through open
testing, so the momentum is not wasted waiting. Goal: **5,000 installs by Sun 28 Feb 2027**, before the
Hari Raya Aidilfitri exodus (around 10 Mar 2027).

## Where things stand (1 Nov 2026)

| Item | State |
|---|---|
| PR #23 "M1: integrate all Sampai work" (head `f3e4e36`) | Bundles #1–#22; 25/25 CI green; open, **not merged** — Hamza merges after testing |
| PR #24 UI polish (contrast, 48dp targets, spacing) | Stacked on #23, open |
| Test APK | `android-FdroidDebug`, applicationId `my.sampai.app`, from [run 41220342185](https://github.com/hamzaiqbal-dev/malaysia-nav/actions/runs/41220342185); **artifact expires 30 Nov** |
| Done | BM UI (582 strings) and BM voice; RM toll totals in the HUD (52 open-system plazas); Malaysian POI categories with 中文/தமிழ் synonyms and 31 brands; new nav HUD; Sampai name and icon; security fixes and pen-test (1 medium, P-1, fixed) |
| Not built | Live road reports (designed in `docs/REPORTS.md`, needs domain + Malaysian hosting) |
| Release blockers | **S-1** inherited Organic Maps OSM OAuth credentials; **S-15** privacy notice and PDPA position |
| Placeholders | `support@sampai.my`, `https://sampai.my` — domain not registered |
| Target SDK | `propTargetSdkVersion=36` in `android/gradle.properties` — already meets Play's current rule |

Found while writing this plan (not in the earlier reviews):

- **Map downloads still come from Organic Maps' servers.** `private.h` points `METASERVER_URL` at
  `meta.omaps.app` and the download list at `cdn*.organicmaps.app` (Netherlands/UK). At 5,000 users that is
  Organic Maps paying for Sampai's bandwidth, and it contradicts any "data stays in Malaysia" line. Needs a
  decision (blocker B8).
- **Organic Maps text is still user-visible:** 15 strings in `android/app/src/main/res/values/strings.xml`
  (e.g. "About Organic Maps", the bookmark-sharing text linking to `omaps.app`), 15 in `values-ms`, 5 in the
  SDK module; `Utils.java` and `place_page_email.xml` reference `organicmaps.app`; only 2 of the 95 Play
  listing locales (en-US, ms-MY) have been rewritten (blocker B7).
- The Phase 1 APK is a **debug** build of the F-Droid flavour: signed with the repo's public
  `android/app/debug.keystore` (updates install over each other, but anyone can sign as it) and with no crash
  reporting. Fine for 20 friends; never for public distribution.

## Google Play rules, verified 1 Nov 2026

| Rule | What it means for Sampai | Source |
|---|---|---|
| Registration fee | US$25, one-time. Applicant must be 18+. | [Play Console Help 6112435](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en) |
| Personal account verification | Legal name and address (from the Google Payments profile) and a government ID; legal name, country and developer e-mail are shown on Play. Must verify access to a real Android device via the Play Console app before publishing. Apps cannot be submitted until verified. | [13628312](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en), [10841920](https://support.google.com/googleplay/android-developer/answer/10841920) |
| Closed-testing rule | Personal accounts created after 13 Nov 2023: a closed test with **at least 12 testers opted in for 14 consecutive days** before "Apply for production". Review usually ≤ 7 days; Google may ask to keep testing if engagement is low. | [14151465](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en-GB) |
| Target API level | Since 31 Aug 2026 new apps and updates must target **Android 16 (API 36)**; extension requests ran to 1 Nov 2026. Sampai targets 36. | [developer.android.com target-sdk](https://developer.android.com/google/play/requirements/target-sdk) |
| Data safety form | Required for every app on closed, open or production tracks (internal-only is exempt), **including apps that collect nothing**, and needs a privacy-policy link. "Collected" = sent off the device; on-device-only processing need not be declared. SDK data counts. | [10787469](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) |
| Organisation account / D-U-N-S | Organisation accounts need a D-U-N-S number (free from Dun & Bradstreet, up to 30 days), organisation documents and website verification; they are **not** subject to the 12-tester rule. Hamza owns Sampai personally, so: personal account now. | [13628312](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en), [10841920](https://support.google.com/googleplay/android-developer/answer/10841920) |
| Open-source forks and trademarks | No Play rule bans apps built from open source; the IP policy bans "improper or unauthorized use of an identical or similar trademark" likely to cause confusion, and says modified copyrighted content can still violate. The Impersonation policy bans names, icons and in-app elements that imply a link to another app or organisation. So: Apache-2.0 attribution text is fine; Organic Maps' name as a brand, its logo, or its listing copy is not. Also: no Jata Negara, no "official"/"kerajaan" wording. | [IP policy 9888072](https://support.google.com/googleplay/android-developer/answer/9888072?hl=en), [Impersonation 9888374](https://support.google.com/googleplay/android-developer/answer/9888374?hl=en-GB) |
| Sideloading (context) | Android developer verification for sideloaded apps started 30 Sep 2026 in Brazil, Indonesia, Singapore and Thailand; Malaysia is not in the first wave (global through 2027). Phase 1 APKs install normally in Malaysia today. | [ProductNation](https://productnation.co/my/31699/android-developer-verification-asean-malaysia-IjA0/) |

## Phases

Dates assume each blocker below is cleared by its deadline. Metric definitions are at the end of the section.

### Phase 0 — internal test on Hamza's phone: Mon 2 – Sun 8 Nov 2026

- Install the `android-FdroidDebug` APK (download before 30 Nov). Drive with it daily: one tolled highway
  trip (e.g. LDP or NKVE), one kampung/offline trip, BM voice on, night mode once.
- Review PR #23 and #24 on the phone; merge #23 (then rebase/merge #24) when satisfied.
- Log every problem as a GitHub issue labelled `phase0`.

Exit criteria (all):
- **Crash-free:** 0 crashes in ≥ 10 trips / ≥ 300 km.
- **Retention:** used on 7 of 7 days (D7 for a single user).
- **Reports:** ≥ 10 issues logged; toll total in the HUD matches the actual Touch 'n Go deduction on ≥ 3 routes.
- #23 merged; decision on default language (B6) taken.

### Phase 1 — friends beta, 20 users, APK: Mon 9 – Sun 22 Nov 2026

- 20 people from Hamza's own circle, chosen for mix: Klang Valley commuters, one outstation driver, one
  Sabah/Sarawak user, one Chinese- and one Tamil-speaking user, two e-hailing/delivery drivers, two civil
  servants. Install the latest CI debug APK (debug keystore is stable, so updates install over the top).
- One WhatsApp group "Sampai Penguji" for feedback; a pinned form (what broke, where, screenshot).
- Mid-point build on Mon 16 Nov with fixes. Re-run CI before 30 Nov so a fresh artifact exists.

Exit criteria:
- **Crash-free:** ≥ 18 of 20 testers (90%) report no crash over the two weeks, and 0 known, reproducible
  crash bugs left open. (The F-Droid debug build has no crash reporter, so this is self-reported plus
  `adb bugreport` from affected phones.)
- **D7 retention:** ≥ 50% of the first-week cohort (installed 9–15 Nov) still using it on day 7.
- **Reports:** ≥ 40 feedback reports (2 per tester); every "wrong route / wrong toll" report triaged.

### Phase 2 — Play Console closed testing: Mon 23 Nov – about Mon 14 Dec 2026

Prerequisites (all must be done by Sun 22 Nov): Play account verified (B1); own OSM OAuth app built in (B3,
S-1); privacy policy published and Data safety form filled (B4, S-15); Organic Maps assets cleaned (B7);
release AAB of the **google** flavour signed with Hamza's upload key, Play App Signing on.

- Mon 23 Nov: closed-test track live, testers added via a Google Group. Recruit **30**, not 12 — the rule
  counts testers opted in on every one of 14 consecutive days, and drop-outs reset nothing but can leave you
  short. Phase 1 friends move over to the Play build (uninstall the debug APK first: different signing key).
- Reports v1 back-end (B5) deployed on the chosen Malaysian host by Mon 30 Nov; client shipped to the closed
  track by Mon 7 Dec (behind `my_reports_enabled`, police type off per `docs/REPORTS.md` §5).
- **Mon 7 Dec: Apply for production** (14 full days: 23 Nov – 6 Dec). Keep the closed test running during
  review. Planned production access: about Mon 14 Dec (review "usually ≤ 7 days").
- Buffer: if fewer than 12 testers are opted in on any day, the clock restarts from the day there are 12
  again; the plan still has three weeks before launch.

Exit criteria:
- **Crash-free:** ≥ 99.5% crash-free sessions in Play Console Android vitals for the last 7 days.
- **D7 retention:** ≥ 35% (Play Console acquisition/retention report).
- **Reports:** ≥ 12 testers opted in 14 consecutive days (target 30); ≥ 50 feedback reports; once reports v1
  ships, ≥ 100 live road reports from testers with < 10% dismissed as false.
- Production access granted.

### Phase 3 — open testing, then production: Tue 15 Dec 2026 – launch Tue 19 Jan 2027

- **3a open testing, 15 Dec – 17 Jan.** Public "early access" listing on Play, link shareable. This is where
  the Waze-ban angle starts (see the 5,000-user plan). Weekly releases; staged rollout for each update.
- Load-test the reports back-end at 10× the expected peak before 4 Jan (TuJu's outage after 700,000 sign-ups
  is the lesson; a 330 req/s per 10 k active users model is in `docs/REPORTS.md` §6).
- **3b production, Mon 18 Jan** staged rollout 20% → **Tue 19 Jan 2027 official public launch** at 100%,
  press release and social push that morning.

Exit criteria for the launch gate (checked Fri 15 Jan):
- **Crash-free:** ≥ 99.9% crash-free sessions for 7 consecutive days (the `docs/MALAYSIA.md` target).
- **D7 retention:** ≥ 25% for the open-testing cohorts.
- **Reports:** ≥ 1 road report per 10 daily active users per day; median report confirmed or expired
  naturally (not dismissed); 0 back-end incidents in the last 7 days.
- Then, after launch, the 30-day decision from `docs/MALAYSIA.md`: 5,000 installs or strong D7 → fund it.

### Phase 4 — iOS: start March 2027, App Store target May 2027

- Starts after Hari Raya (around 10–14 Mar 2027) and only if Android passed its 30-day decision.
- Needs: Apple Developer Program enrolment in Hamza's name (annual fee — check at enrolment, not verified
  here), a Mac, the same OSM OAuth app, an iOS privacy-nutrition-label equivalent of the Data safety form.
- Port the Android Malaysian layers: BM strings already flow from `data/strings/`; HUD, tolls and reports
  need Swift UI work on the existing `iphone/` app.
- TestFlight with 50 testers in April; App Store submission in May.

Exit criteria: ≥ 99.9% crash-free sessions in TestFlight for 7 days; D7 ≥ 25%; report volume per active user
within 20% of Android.

### Metric definitions

- **Crash-free %**: crash-free sessions from Play Console Android vitals (Phase 2 on). Phases 0–1: self-reported.
- **D7 retention**: share of an install cohort that opens the app on day 7 (Play Console). Sampai has no
  analytics SDK and should not add one; use what Play provides.
- **Report count**: feedback reports in Phases 0–2; live road reports (server-side aggregate counts per day,
  `docs/REPORTS.md` §3.5) once reports v1 ships.

## Blockers only Hamza can clear

| # | Blocker | Deadline | Notes |
|---|---|---|---|
| B1 | **Play developer account in his own name** (personal) | Start **Wed 4 Nov**; verified by **Fri 13 Nov** | US$25 once; government ID; payments-profile name and address must match the ID; verify a real Android phone through the Play Console app. Use a personal Google account, not a Kelker one. His legal name will show on the Play listing. An organisation account (needs a company/SSM registration + D-U-N-S, up to 30 days) skips the 12-tester rule but would not be faster. |
| B2 | **Domain `sampai.my` + `support@sampai.my`** | Register by **Fri 6 Nov**; mail live by **Fri 13 Nov** | .my for an individual needs a MyKad; activation typically 1–3 working days. Check availability first; fallback `sampai.app`. Also take `api.sampai.my` for reports. Replace placeholders if the final address differs. |
| B3 | **His own OSM OAuth application** (S-1) | **Fri 13 Nov** | Register on openstreetmap.org under a Sampai OSM account; give the client id and redirect URI so they replace upstream's values in `private.h`. Without it Sampai logs in to OSM as Organic Maps. |
| B4 | **Privacy policy and PDPA position** (S-15) | Approve text **Fri 13 Nov**; published at `sampai.my` by **Wed 18 Nov**; Data safety filled **Fri 20 Nov** | BM + EN. Must state truthfully: no account, no analytics; location stays on the phone except a report position when the user reports; map files download from (today) Organic Maps' servers in Europe; OSM edits go to the OpenStreetMap Foundation; Android cloud backup includes bookmarks (S-8); reports data per `docs/REPORTS.md` §4; Hamza as contact/DPO for now. |
| B5 | **Malaysian hosting for reports** | Decide **Sun 15 Nov**; account open **Fri 20 Nov** | Recommendation from `docs/REPORTS.md` §6: one Exabytes NVMe VPS (about RM76/month) in his name; move to AWS Malaysia (ap-southeast-5) past ~50 k DAU. Also decide: police/roadblock reports off at launch (recommended) and fuel-price reports on/off. |
| B6 | **Default language BM: yes or no** | **Sun 8 Nov** (end of Phase 0) | Recommendation: **yes** for the Play default listing (ms-MY) and a first-run language choice pre-selected to BM with one tap to EN/中文/தமிழ்; keep English as the code fallback (`values/`) so untranslated strings never show as blanks. |
| B7 | **Organic Maps trademark-free assets check** | **Fri 20 Nov** (before any Play upload) | Sign off a list prepared by the agent: icon/splash/about logo (done in #19), the 15+15+5 "Organic Maps" UI strings, `omaps.app` share links, the 93 un-rewritten Play listing locales (delete, do not upload), screenshots. Keep the Apache-2.0 notice and "built on the open-source Organic Maps engine" attribution — that is required and allowed. |
| B8 | **Map download servers** (new) | Decide by **Mon 30 Nov**; in place before open testing **Tue 15 Dec** | Either ask Organic Maps for written permission to use their CDN at Sampai's scale, or mirror the Malaysia/Singapore/Brunei map files on his own server (same VPS/CDN as reports). Required before claiming "data stays in Malaysia". |
| B9 | **Upload key and CI signing secrets** | **Fri 20 Nov** | Create the upload keystore himself, store it offline, add it to GitHub Actions secrets; agent wires the release workflow. |

## Recommended public launch date: Tue 19 Jan 2027

Why this date:
- **Earliest safe date.** Closed test can start 23 Nov at the earliest (blockers B1–B4, B7, B9 first);
  14 days → apply 7 Dec → production access about 14 Dec; then four weeks of open testing to reach and hold
  99.9% crash-free, load-test reports, and fix what thousands of open testers find. Launching into the year-end
  school holidays and New Year (1–3 Jan) with a solo owner on support is avoidable.
- **Driving calendar.** A Tuesday launch gets weekday press coverage, then three long weekends of heavy
  travel within three weeks: Thaipusam (22–24 Jan), Federal Territory Day (30 Jan–1 Feb) and Chinese New Year
  (6–8 Feb) — the moments RM toll totals, offline maps and BM voice show best. Hari Raya Aidilfitri (around
  10 Mar) is the next, bigger push.
- **News timing.** The Waze story (civil-servant ban circular of 20 Oct, reported 21 Oct; the PM said on 23 Oct
  a total ban is possible if local alternatives are good enough; TuJu's navigation was switched off on 22 Oct
  after a surge) is used from 15 Dec through open testing, so Sampai is visible while the debate is live.

Risks and mitigations:
- **A total Waze ban is announced before January** → demand arrives early. Mitigation: open-testing link is
  live from 15 Dec; if a ban lands, push it immediately and pull the production date forward, but only once
  crash-free ≥ 99.5%.
- **Production access delayed or refused** (low tester engagement) → keep 30 active closed testers; the plan
  has a three-week buffer before 19 Jan; worst case launch slips to Tue 2 Feb (before CNY).
- **Back-end overload, TuJu-style** → load test at 10×, per-cell caching, reports are optional (navigation
  keeps working offline if the server is down).
- **Organic Maps CDN or trademark complaint** → B7 and B8 closed before open testing.
- **OSM data gaps in Malaysia** (missing roads, wrong turn restrictions) → in-app "fix the map" call to
  action, a weekly list of the worst-reported areas, and an honest store description.
- **Political exposure** → no claims of government approval, no Jata Negara, police reports off at launch,
  privacy notice explicit about which data leaves the phone. Do not describe Sampai as "approved for civil
  servants" unless NACSA says so in writing.
- **Owner bandwidth** → Hamza is one person with a day job; support is limited to the WhatsApp/e-mail
  channels, and weekly releases only.

## The 5,000-user plan

Target: **5,000 installs by Sun 28 Feb 2027**, with ≥ 1,500 weekly active users. No paid installs, no
incentivised reviews (both break Play policy), and only post in groups where Hamza or a friend is a member.

Channels and expected share:

| Channel | What | Share of 5,000 |
|---|---|---|
| His own network | Family, friends, school/university alumni, colleagues as individuals (Sampai is his, not Kelker's — no company channels). Personal message with the link and one ask: "use it for one trip and tell me what broke". | 300 |
| WhatsApp groups | Taman/residents' associations, surau and mosque committees, alumni, car and motorcycle clubs, e-hailing and delivery driver groups. A 30-second BM screen recording + Play link; each tester asked to forward to 2 groups. | 1,500 |
| TikTok / Facebook Malaysia | 2–3 short BM videos a week: "berapa RM tol balik kampung?", BM voice at an LDP exit, offline in Sabah/Sarawak, "tiada akaun, tiada iklan". Facebook driving/traffic groups and balik-kampung threads before each long weekend. | 1,500 |
| Waze-ban news angle | Press note to soyacincau, Lowyat.NET, The Star tech, Malay Mail, Bernama, NST, Says: "Made-in-Malaysia, open-source navigation app; no account; your trips stay on your phone; reports hosted in Malaysia". Offer the source code for any security review. Stick to facts, never attack TuJu. | 1,200 |
| Civil servants | Through personal contacts and their own WhatsApp groups (they are the users told to drop Waze); a one-page BM explainer of what data leaves the phone. No implied endorsement. | 500 |
| Play search and word of mouth | ms-MY listing with "navigasi", "tol", "peta luar talian" keywords; ask happy users for a review. | remainder |

Weekly cumulative install targets (Play Console + APK count; weeks end Sunday):

| Week ending | Phase | Target | Main push |
|---|---|---|---|
| 8 Nov | 0 | 1 | Hamza |
| 15 Nov | 1 | 10 | own network |
| 22 Nov | 1 | 20 | own network |
| 29 Nov | 2 closed | 40 | Google Group from friends' friends |
| 6 Dec | 2 closed | 80 | driver and civil-servant contacts |
| 13 Dec | 2 closed | 120 | WhatsApp groups (closed-test sign-up) |
| 20 Dec | 3a open | 300 | open-testing link; first TikTok/FB videos |
| 27 Dec | 3a open | 450 | year-end holiday driving videos |
| 3 Jan | 3a open | 650 | New Year traffic; WhatsApp forwards |
| 10 Jan | 3a open | 900 | press pre-briefs under embargo |
| 17 Jan | 3a open | 1,200 | teaser posts |
| 24 Jan | launch | 2,200 | launch Tue 19 Jan; press; Thaipusam weekend |
| 31 Jan | 3b | 3,000 | FT Day weekend; driver groups |
| 7 Feb | 3b | 4,000 | CNY balik kampung toll videos |
| 14 Feb | 3b | 4,500 | post-CNY reviews, user stories |
| 21 Feb | 3b | 5,000 | **target reached** |
| 28 Feb | 3b | 5,500 | buffer; Raya campaign prep (Raya ~10 Mar) |

If a week misses by more than 30%, the next week drops new pushes and fixes whatever reviews and the
WhatsApp group say is driving people away.

## Sources

Google Play and Android
- [Play Console Help: registration fee, personal account requirements (6112435)](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en)
- [Play Console Help: personal vs organisation accounts, D-U-N-S (13628312)](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en)
- [Play Console Help: verify your developer identity (10841920)](https://support.google.com/googleplay/android-developer/answer/10841920)
- [Play Console Help: testing requirements for new personal accounts (14151465)](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en-GB)
- [Android Developers: target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Play Console Help: Data safety section (10787469)](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Play policy: Intellectual Property (9888072)](https://support.google.com/googleplay/android-developer/answer/9888072?hl=en)
- [Play policy: Impersonation (9888374)](https://support.google.com/googleplay/android-developer/answer/9888374?hl=en-GB)
- [ProductNation: Android developer verification and Malaysia](https://productnation.co/my/31699/android-developer-verification-asean-malaysia-IjA0/)

Malaysia context
- [The Star, 21 Oct 2026: civil servants banned from using Waze](https://www.thestar.com.my/tech/tech-news/2026/10/21/malaysia-bans-civil-servants-from-using-waze-according-to-a-circular-from-the-prime-minister039s-department)
- [Soyacincau, 23 Oct 2026: PM says total Waze ban possible](https://soyacincau.com/2026/10/23/pm-anwar-malaysia-considering-total-ban-on-waze/amp/)
- [Soyacincau, 22 Oct 2026: TuJu disables navigation after criticism](https://soyacincau.com/2026/10/22/tuju-disables-navigation-feature-following-criticism/)
- [The Star, 23 Oct 2026: 700,000 Malaysians driven towards TuJu](https://www.thestar.com.my/news/nation/2026/10/23/700000-msians-driven-towards-tuju-app)
- [Soyacincau, 27 Oct 2026: LLM says TuJu was never meant to replace Waze](https://soyacincau.com/2026/10/27/llm-tuju-not-meant-to-replace-waze/amp/)
- [Says.com: Malaysian public holidays and long weekends 2027](https://says.com/my/lifestyle/here-are-all-the-public-holidays-and-long-weekends-in-2027)
- [Shinjiru: .my domain document requirements](https://www.shinjiru.com.my/blog/supporting-document-requirement-for-my-domain-registration-full-guide/)

In this repository: `docs/MALAYSIA.md`, `docs/REPORTS.md`, `docs/SECURITY_PENTEST_2026-10-31.md`, `private.h`,
`android/gradle.properties`.
