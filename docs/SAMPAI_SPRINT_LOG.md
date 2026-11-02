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
