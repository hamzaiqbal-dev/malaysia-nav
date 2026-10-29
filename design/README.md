# Malaysia Nav — UI redesign

`concept-3d/index.html` is the interactive design concept approved as the target look (29 Oct 2026):
Waze/Google-Maps-class 3D navigation on top of the Organic Maps engine.

Open it in any browser. It shows three states: **Navigasi** (turn banner, lane strip, speed/limit,
ETA sheet with toll in RM, report FAB), **Cari** (floating search, Malaysian quick chips, place card),
and the **Lapor** report sheet (kesesakan, kemalangan, polis/sekatan, kamera AES, lubang, banjir,
jalan ditutup, harga minyak). BM first, EN toggle.

## Implementation plan (Android first)
1. Map style `data/styles/my/` — Malaysian palette (night + day): soft greys, blue water, strong road
   hierarchy, 3D building extrusions enabled by default, highway shields (E1, E2 …), toll plaza icons.
2. `android/app/src/main/java/app/organicmaps/my/` — new Kotlin UI on the existing MapFragment:
   - `NavHudView` (turn banner, lane strip, speed + limit, ETA sheet with toll)
   - `ExploreSheet` (search bar, chips, place card with Malaysian facts)
   - `ReportSheet` + `ReportMarkers` (client side first; backend later)
   - Theme tokens in `res/values/my_colors.xml`, Plus Jakarta Sans in `res/font/`
3. Default to 3D perspective + 3D buildings in navigation; follow-and-rotate camera with tilt.
4. Strings: `strings.xml` BM (`values-ms`) is the default; EN, ZH, TA follow.
5. Tolls dataset `data/my_tolls.csv` → route toll total in the ETA sheet.
Each step is its own PR; CI (android-check) must stay green.
