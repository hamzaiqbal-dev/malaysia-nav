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
