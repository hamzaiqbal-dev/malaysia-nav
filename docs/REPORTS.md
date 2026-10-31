# Sampai live road reports — design v1 (30 Oct 2026)

The Waze part of Sampai: drivers report what is on the road right now, other drivers see it on the map and hear it
while navigating. This document fixes the data model, privacy rules and API before any back-end code is written
(docs/MALAYSIA.md, 30-day plan week 3). Decision needed from the founder: hosting (section 6).

## 1. Scope

Report types (BM label / EN): kesesakan / jam, kemalangan / accident, polis / police or sekatan jalan / roadblock,
kamera AES-AWAS / speed camera, lubang / pothole, banjir / flood, jalan ditutup / road closed, harga minyak / fuel
price. A report is a pin on the map with a type, a position snapped to the road, an optional free-text note (80
characters), a creation time and an expiry time. Nothing else.

Not in v1: photos, chat, user profiles, points or ranks, routing around reports (traffic-aware routing needs a
different data pipeline), police-report debates (see section 5).

## 2. Data model

```
Report
  id            UUIDv7 (time-ordered)
  type          enum: jam | accident | police | camera | pothole | flood | closure | fuel_price
  lat, lon      WGS84, snapped to the nearest road on the client (6 decimals max, ~10 cm; stored at 5)
  heading       0-359 or null (direction of travel when reported; shown as "arah KL" style hints)
  note          0-80 chars, optional, filtered (no phone numbers, no URLs)
  fuel          only for fuel_price: {brand, ron95, ron97, diesel} in RM, optional fields
  created_at    server time
  expires_at    created_at + TTL(type), extended by confirmations (section 3)
  confirms      count of "masih ada" (still there)
  dismisses     count of "sudah tiada" (gone)
  device_key    opaque per-install key (section 4); never returned by the API
  region        cell id (H3 res 7 or geohash-6) for sharding and rate limits
```

TTL by type: jam 30 min, accident 60 min, police 45 min, camera 24 h, pothole 7 days, flood 2 h, closure 6 h,
fuel_price 24 h. A confirmation extends `expires_at` by half a TTL (capped at 2 × TTL from creation); three
dismisses with no confirmation in the last 10 minutes expire the report at once.

The client keeps reports in memory only, keyed by id; nothing is written to disk except the device key.

## 3. Lifecycle

1. Driver taps Lapor → type → the report is sent with the current snapped position and heading. The pin appears
   locally immediately (optimistic), with the server id replacing the temporary one on success.
2. Others within the viewport (or along the route) fetch reports every 30 s while navigating, every 2 min while
   browsing, never in the background. Only active, non-expired reports are returned.
3. Approaching a report along the route (within 500 m, same heading ± 60°): voice line in BM ("Kesesakan di
   hadapan", "Kamera AES di hadapan") and a chip on the HUD with "Masih ada? Ya / Tiada".
4. Confirmations and dismisses are counted once per device per report.
5. Expired reports are deleted from the live store within 24 h and aggregated only as counts per cell and day
   (no positions, no device keys) for the "how many reports did we get" metric.

## 4. Privacy and PDPA

Principles: no account, no personal data by design, data stays in Malaysia.

- Device key: a random 128-bit value generated on first use, stored on the device, rotated every 30 days. It is
  the only identifier; the server stores a salted hash of it on each report for rate limiting and abuse
  handling, never the key, never a device id, phone number, e-mail or name.
- Location: only the report position is sent, when the driver reports. No trip is uploaded. The reports feed
  requests send a viewport bounding box or a route cell list, rounded to the cell grid, not the exact position.
- IP addresses are not logged by the application; the reverse proxy keeps access logs for 7 days for abuse
  handling only.
- PDPA 2010 as amended in 2024 (in force in phases from 1 Jan and 1 Jun 2025): because the service is designed
  to hold no personal data, most obligations do not attach, but the project still: keeps a breach register,
  can notify the Commissioner within 72 hours and affected users within 7 days if device keys were ever to leak
  in a way that identifies someone, publishes a privacy notice in BM and EN inside the app, and appoints a DPO
  contact when user volume makes it mandatory (a Malaysian resident, BM and EN speaking; can be the founder
  until then). Hosting in Malaysia removes cross-border transfer questions.
- Retention: live reports as per TTL; aggregated counts indefinitely; proxy logs 7 days; abuse blocks 30 days.
- Users can delete a report they made (same device key) from the pin; no other user content exists to erase.

## 5. Abuse, rate limits and the police question

- Rate limits per device key: 1 report per 60 s, 10 per hour, 30 per day; 1 confirm/dismiss per report;
  per cell: 200 new reports per hour before the cell is throttled (a burst in one place is a jam, not spam).
- A device whose reports are dismissed far more than confirmed over 7 days gets a shadow weight of 0 (its
  reports are stored but not shown) for 30 days.
- Notes are filtered client- and server-side for phone numbers, URLs and a short BM/EN profanity list.
- Police / roadblock reports: Waze has shown these in Malaysia for years and no law forbids reporting a visible
  roadblock; the risk is reputational (JKSN decision of 20 Oct 2026). Sampai shows "sekatan jalan / polis"
  as a neutral traffic-slowdown marker with no description and the shortest TTL (45 min), and never shows
  speed-trap style text. Keep this type behind a server flag so it can be switched off per state or nationally
  without an app update.
- AES/AWAS camera positions are public (JPJ publishes the list); community reports only add mobile cameras.

## 6. Back-end and hosting

Architecture v1: one stateless HTTP service (Go or Rust, ~1 500 lines) + PostgreSQL with PostGIS (or SQLite
with R-tree for the private test) + a reverse proxy with TLS; a cron deletes expired rows. Reports are served from
an in-memory cell index refreshed every 10 s, so reads never hit the database. No queues, no Kubernetes.

Load model: a navigating user polls every 30 s → 10 k active users ≈ 330 req/s peak, 100 k ≈ 3 300 req/s,
1 M ≈ 33 000 req/s. Responses are small (≤ 20 KB) and cacheable per cell for 10 s at the proxy, so a single
8-vCPU box handles the 100 k tier; the 1 M tier needs 3-5 boxes behind a load balancer and a CDN in front.

| Option | Where | Monthly cost (RM) 10 k / 100 k / 1 M users | Notes |
|---|---|---|---|
| Exabytes NVMe VPS (self-managed) | Malaysia | C3 RM76 / C6 RM222 + 1 more / 4 × C8 + LB ≈ RM1 500 | Prices from exabytes.my NVMe VPS page (as low as, triennial); simplest, data stays in MY |
| AWS Asia Pacific (Malaysia), ap-southeast-5 | Malaysia (launched Aug 2024) | ≈ RM150 / RM600 / RM3 000-5 000 | t4g instances + RDS Postgres + ALB + CloudFront; elastic, more ops knowledge needed; check the AWS calculator before committing |
| AIMS / TM One colocation or cloud | Malaysia | from ≈ RM400 | Enterprise contracts; only if a client or regulator demands a local-carrier host |

Recommendation: start on one Exabytes NVMe VPS (C3, RM76/month) for the 20-user private test and the closed
beta; move to AWS Malaysia (or add VPS nodes) when daily active users pass ~50 k. Both keep data in Malaysia.

Secure by default: TLS only, HSTS, no admin UI on the public host, SSH by key, automatic security updates,
nightly encrypted database dump kept 7 days, secrets outside the repository, dependencies pinned.

## 7. API v1

Base URL `https://api.sampai.my/v1` (domain to be registered). JSON over HTTPS. Every request carries
`X-Device-Key` (section 4) and `X-App-Version`. Errors are `{ "error": "code", "message": "…" }`.

```
GET  /reports?bbox=minLon,minLat,maxLon,maxLat          viewport feed (bbox snapped to cells, max 50 × 50 km)
GET  /reports?cells=c1,c2,…                            route feed (max 200 cells)
     → { "reports": [Report…], "served_at": "…", "next_poll_s": 30 }
POST /reports            { type, lat, lon, heading?, note?, fuel? }   → 201 { "report": Report }
POST /reports/{id}/confirm                                          → 204
POST /reports/{id}/dismiss                                          → 204
DELETE /reports/{id}     (own reports only)                         → 204
GET  /config             → { "types_enabled": […], "ttl_s": {…}, "min_app_version": "…" }
```

Report as returned (no device key, no raw counts below 3 to avoid fingerprinting):
`{ id, type, lat, lon, heading, note, fuel, created_at, expires_at, confirms, dismisses }`.

Rate-limit responses: `429` with `Retry-After`. Reports older than `expires_at` are never returned. Clock skew is
handled server-side: `created_at` is always server time.

## 8. Client (Android, PR after this doc)

- `android/app/src/main/java/app/organicmaps/my/reports/`: `ReportSheet` (the 8-tile Lapor sheet from the
  concept), `ReportStore` (in-memory, expiry), `ReportMarkers` (pins through the existing user-mark layer),
  `ReportApi` (OkHttp, retries, offline queue of at most 20 unsent reports, sent when back online, dropped after
  2 hours), `ReportVoice` (TTS lines from `data/strings/sound.txt`).
- Everything behind `R.bool.my_reports_enabled` and a configurable base URL; a mock back-end in the app
  (`ReportApi.Fake`) for tests and the design review.
- Tests: TTL and confirm/dismiss rules, offline queue, note filter, cell computation, API serialisation.

## 9. Open questions for the founder

1. Hosting choice (section 6) and who holds the account.
2. Domain name (api.sampai.my needs a .my registrant; sampai.app is an alternative).
3. Whether to show police/roadblock reports at launch or keep the type disabled for the first public beta.
4. Fuel price reports: include at launch (useful, low risk) or defer.
