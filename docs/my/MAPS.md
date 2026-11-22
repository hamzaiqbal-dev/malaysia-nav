# Sampai map files

Organic Maps' `DATA_LICENSE.txt` does not let us rebrand their generated `.mwm` files, so Sampai builds
its own from OpenStreetMap (ODbL). Map data © OpenStreetMap contributors.

## How they are built
Workflow: `.github/workflows/sampai-maps.yaml` (Actions tab → "Sampai map files" → Run workflow; also runs
monthly on the 3rd). It:
1. builds `generator_tool`, `world_roads_builder_tool`, `mwm_diff_tool` on Ubuntu;
2. downloads the newest dated Geofabrik extract `asia/malaysia-singapore-brunei` and checks its `.md5`; Geofabrik
   often refuses GitHub runners, so it falls back to the openstreetmap.fr extracts of Malaysia, Singapore
   and Brunei (each checked against its `.md5`) merged with `osmium apply-changes`;
3. closes the coastline (`tools/my/maps/coastline_fix.py`, see below);
4. runs `tools/python/maps_generator` with the repo's own `data/` (borders, classificator, styles) for
   `World, WorldCoasts, Malaysia_West, Malaysia_East, Singapore, Brunei`;
5. publishes a GitHub Release tagged `maps/<YYMMDD>` with the `.mwm` files, `countries.json`,
   `packed_polygons.bin` and `SHA256SUMS`. Pull requests that touch the workflow only build an artifact.

Skipped generator stages (need data Sampai does not have yet): elevation (`Srtm`, `IsolinesInfo`), public
transport (`RoutingTransit`), `Popularity`, map diffs and statistics.

### Coastline
A regional extract cuts the coastline of Borneo and the Peninsula at its border; the generator needs closed
coastline rings. The script removes the `natural=coastline` tag from the cut ways (other tags such as island
names stay) and adds closed rings from the OSM land polygons
(`osmdata.openstreetmap.de/download/land-polygons-complete-4326.zip`, ODbL) clipped to the box around the
four border files plus 0.5°. Unit tests: `cd tools/my/maps && python -m unittest coastline_fix_test`.

## How the app finds them
One place: `private.h`.
- `METASERVER_URL` is empty, so the app uses `DEFAULT_URLS_JSON` directly.
- `DEFAULT_URLS_JSON` is `https://github.com/hamzaiqbal-dev/malaysia-nav/releases/download/`. The app asks
  for `<server>maps/<version>/<file>.mwm`, which is the release tag `maps/<version>`.
- `<version>` comes from the `countries.json` bundled in the app. The Android app bundles
  `data/my/maps/` (`countries.json`, `World.mwm`, `WorldCoasts.mwm`, `packed_polygons.bin`; the asset
  symlinks in `android/sdk/` point there), so a new map release reaches users only when those four files
  are replaced with the release's copies and a new app is shipped. `data/` keeps the upstream world files
  for the engine tests and the desktop app.
- `packed_polygons.bin` must list exactly the countries in `countries.json` (otherwise the app hits a
  `CHECK`); `CountryInfoGetter_Sampai_Countries_And_Polygons` in `storage_tests` checks this.

To move to another host later: upload the same files under `<host>/maps/<version>/` and change
`DEFAULT_URLS_JSON`.

## Limits
- World and WorldCoasts cover only this region: outside it the overview map is empty sea. A full-world
  overview needs the whole planet (about 80 GB download, a large server, 1 TB disk), which does not fit
  GitHub Actions (14 GB disk, 6 hours, 16 GB RAM).
- GitHub Releases are free for a public repo, but have no Malaysian edge servers and no download
  statistics; files are limited to 2 GB each.
