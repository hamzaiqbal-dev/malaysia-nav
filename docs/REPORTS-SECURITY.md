# Sampai live road reports — security design note (30 Oct 2026)

Companion to [REPORTS.md](REPORTS.md), which fixes the data model, API and hosting for the live road reports
service. That document already sets out the privacy intent (no account, no personal data by design, data in
Malaysia) and a first cut at rate limits. This note covers what is left: how an anonymous service resists abuse
when its only identifier is self-asserted, what must and must not be stored, and where PDPA actually bites.

Nothing here is built yet. Read it before the first line of back-end code.

## 1. The central problem: `X-Device-Key` is not a credential

REPORTS.md section 4 makes the device key "a random 128-bit value generated on first use" and section 7 sends it
as `X-Device-Key`. Every limit in section 5 is per device key. But the client mints that value itself, so an
attacker mints ten thousand of them in a shell loop and every per-device limit evaporates. The rate limits as
designed stop an annoyed driver, not someone who wants to fill the Federal Highway with fake accidents.

Worse, the two mechanisms contradict each other: the key is "rotated every 30 days" (section 4) while a bad
reporter gets "a shadow weight of 0 ... for 30 days" (section 5). A rotation clears the penalty, and a malicious
client rotates on demand.

Fix the identity layer before anything else:

1. **Server-issued enrolment.** `POST /enrol` returns an opaque token: a server-generated key id plus an HMAC
   over it under a server secret (or an Ed25519 signature). The client stores and sends the token; the server
   recomputes the MAC and rejects anything it did not issue. This alone does not stop mass enrolment, but it
   means the server decides what a valid identity is and can revoke one.
2. **Make enrolment cost something.** Require a proof of work on `POST /enrol` (a hashcash-style puzzle, tuned
   to roughly a second on a phone). One install never notices; ten thousand identities become real compute.
   Keep the difficulty a server-side config value so it can be raised during an attack.
3. **Tiered trust, not a gate.** On the `google` flavour, verify a Play Integrity token at enrolment and mark
   the identity attested. F-Droid and web builds cannot attest, and Sampai must not punish them -- give
   unattested identities lower trust weight and stricter limits (say a third of the quota, and their reports
   need two confirmations before display) rather than refusing them. Never make attestation a hard requirement:
   it would tie an open-source app to Google Play and exclude exactly the privacy-minded users it is for.
4. **Separate rotation from reputation.** Rotating an identity must not reset a penalty, or the penalty is
   decorative. Either drop the 30-day rotation (the token is pseudonymous and never leaves the device, so
   rotation buys little) or carry a server-side reputation across rotation by issuing the new token in exchange
   for the old one. Prefer the first: it is simpler and honest. If rotation stays, say in the privacy notice
   that it does not clear a block.

## 2. Report ids must be unguessable

REPORTS.md section 2 makes `id` a UUIDv7, which is time-ordered by design. With `POST /reports/{id}/confirm`,
`/dismiss` and `DELETE /reports/{id}`, guessable ids let an attacker walk the id space and mass-dismiss reports
they never saw, anywhere in the country, without ever fetching a feed. Time-ordered ids also leak creation
ordering and rough timing to anyone holding two of them.

Use a random 128-bit id (UUIDv4 or 16 random bytes, base64url) for anything the API exposes. Keep UUIDv7
internally as a database sort key if the time ordering is useful for sharding. Additionally, accept a
confirm/dismiss only for a report the caller could plausibly have seen -- one returned in a feed response to
that identity within the last few minutes -- tracked server-side as a short-lived set per identity.

For `DELETE`, compare the caller's identity against the report's stored identity hash in constant time, and
return `404` rather than `403` for a report that exists but is not theirs, so the endpoint is not an oracle for
which ids exist.

## 3. What to store, and what never to store

Store:

- The report: id, type, position, heading, note, timestamps, counts. This is the product.
- A salted hash of the reporting identity, **with a per-report random salt**, for the abuse window only.
- A coarse cell id for sharding and per-cell limits.

Never store:

- The raw identity token (store a hash), a device id, advertising id, IMEI, phone number, e-mail or name.
- A trip, a track, or any position the driver did not deliberately submit. The feed request carries a cell list
  or a bbox snapped to the grid, and that request must not be written to any durable log alongside an identity.
- An exact timestamp joined to an exact position for longer than needed. A report is a precise statement that a
  specific person was at a specific place at a specific minute, and the first report a new install makes is
  disproportionately often outside their home.

The important design move is to **decouple abuse data from report data on a timer**. Keep the identity hash
attached to a report only while it could matter for abuse handling -- the report's TTL plus a short window, so
hours, not forever. After that, drop the hash and keep the report. Reputation lives in a separate table keyed by
identity with counters and no positions. Then a dump of the reports table is not a movement history, and a dump
of the reputation table is not a location log. Nothing in REPORTS.md currently forces that separation, and the
nightly database dump in section 6 would otherwise carry both together.

Round stored coordinates to the precision the feature needs. 5 decimals is roughly a metre; no report type
needs that. Snap to road geometry on the client (already the design) and store 4 decimals (~11 m) for
stationary types like `pothole` and `camera`, which are the ones that persist for days.

## 4. Treat the note field as hostile

The 80-character note is the only free text in the system, so it is the only stored-XSS and injection vector.
REPORTS.md filters it for phone numbers and URLs, which is a spam control, not a security control.

- Validate on the server, never trust the client filter: length in characters (not bytes), a Unicode allowlist
  covering BM, English, digits and basic punctuation, and rejection of control characters and bidi overrides.
- Escape on output in every consumer, including any future admin page or dashboard. Assume a note will one day
  be rendered in a WebView; the app's existing article WebView runs with JavaScript disabled, and anything new
  that displays notes must do the same.
- Normalise Unicode (NFC) before storing and before filtering, so a filter is not bypassed by decomposed or
  homoglyph forms.
- Never interpolate a note into SQL, a log line, or a shell command. Parameterised queries only; structured
  logging only.

## 5. Rate limiting that works

Per-identity limits from REPORTS.md section 5 stay, but they can only be the inner layer. Add:

- **Per-IP and per-prefix limits** at the reverse proxy (IPv4 /24, IPv6 /56) for `POST /enrol`, `POST /reports`
  and the confirm/dismiss endpoints. This is the layer that actually costs a Sybil attacker something.
- **Global circuit breakers**: a cap on new reports per minute nationally and per cell, above which new reports
  are accepted but not displayed pending confirmation. A genuine mass event (a flood, a major crash) looks like
  an attack; failing closed on display rather than on acceptance keeps the data and protects the map.
- **Cost asymmetry on reads.** `GET /reports` is the hot path and is cacheable for 10 s per cell at the proxy
  (already the design) -- keep it cheap and cached, and cap bbox area and cell count server-side, not just in
  the documented contract.
- `429` with `Retry-After`, and no detail in the error body about which limit was hit.

## 6. Operational security

Mostly as REPORTS.md section 6, with additions:

- The nightly dump is the highest-value target in the system: it holds positions and identity hashes. Encrypt it
  with a key that is not on the database host, and restore-test it. Keep 7 days as designed.
- Separate the proxy access logs (which hold IP addresses) from the application database, on different retention
  and with no join key. 7 days for abuse handling is defensible; make sure nothing copies them elsewhere.
- Rotate the HMAC/signing secret for enrolment tokens on a schedule, supporting two valid keys during overlap so
  rotation does not log everyone out.
- No admin UI on the public host (already stated). If one is ever added, it needs real authentication and must
  not be reachable from the internet.
- Pin dependencies and keep the service small. A ~1 500-line Go or Rust service with Postgres is a realistic
  attack surface for one person to own; anything larger is not.

## 7. PDPA -- where it actually bites

REPORTS.md section 4 says that "because the service is designed to hold no personal data, most obligations do
not attach". That is too confident, and it is the one legal claim in the design I would not rely on.

The service processes **precise location, timestamped, tied to a persistent pseudonymous identifier**. That
combination is capable of identifying a person indirectly, which is the test that matters. Design the service as
though the PDPA applies, because the cost of doing so is small and the alternative is a bet.

Dated specifics, from the Personal Data Protection (Amendment) Act 2024 (assented 9 Oct 2024, gazetted
24 Dec 2024), which commenced in three phases -- 1 Jan 2025, 1 Apr 2025 and 1 Jun 2025:

- **Breach notification (in force 1 Jun 2025).** Notify the Commissioner "as soon as practicable and within 72
  hours" of the breach, and affected data subjects "without unnecessary delay and within seven days of
  notifying the Commissioner" where significant harm is likely. REPORTS.md already states these; what is
  missing is a rehearsed path -- a breach register, a named decision-maker, and a drafted notice -- because 72
  hours is not enough time to invent a process.
- **Data Protection Officer (in force 1 Jun 2025).** The guidelines trigger a mandatory DPO at personal data of
  more than 20,000 data subjects, sensitive personal data of more than 10,000, **or** processing involving
  "regular and systematic monitoring" of personal data. REPORTS.md defers a DPO until "user volume makes it
  mandatory", which reads only the volume triggers. A continuously polling location service may well meet the
  monitoring trigger at any volume. Get a Malaysian practitioner's view on that specific question before
  launch; appointing a DPO contact is cheap, and for a sole founder it is a formality.
- **Terminology and penalties (1 Apr 2025).** "Data user" became "data controller"; the maximum fine for
  breaching the protection principles rose from RM300,000 to RM1,000,000 and maximum imprisonment from 2 to 3
  years. Use "data controller" in the privacy notice.
- **Cross-border transfer (1 Apr 2025).** The whitelist regime was removed; transfers are allowed to places with
  substantially similar protection or equivalent safeguards. Hosting in Malaysia avoids the question -- but
  REPORTS.md section 6 also proposes CloudFront in front of the service, and a CDN terminates requests abroad
  and processes IP addresses there. Either keep the CDN inside Malaysia, restrict it to in-country edge
  locations, or document the transfer basis. This is a contradiction inside the existing design, not a new
  constraint.
- **Scope.** The PDPA applies to processing of personal data "in respect of commercial transactions". A free app
  with no monetisation arguably sits outside it; ads, a paid tier or a data deal bring it in squarely, and the
  analysis should be redone the day monetisation is added rather than assumed to carry over. How the
  Commissioner reads "commercial transaction" for a free consumer app is not verified here -- that is a
  question for counsel, not for this document.

Regardless of scope, publish the privacy notice in BM and EN inside the app before the first public build, state
plainly what leaves the device (report position when the driver reports; a viewport or cell list while browsing)
and what does not (no trip, no account, no telemetry), and get consent for the report submission itself rather
than burying it in an acceptance of terms.

Sources for the dated statements above: DFDL, "Malaysia: Implementation of the Personal Data Protection
(Amendment) Act 2024"; DLA Piper Privacy Matters, "Malaysia: Guidelines issued on data breach notification and
data protection officer appointment", 4 Mar 2025. Both read on 30 Oct 2026.

## 8. Decisions needed before the back-end is written

1. Identity model: server-issued token with proof of work (recommended), or keep client-minted keys and accept
   that rate limits are advisory.
2. Whether to use Play Integrity for tiered trust on the `google` flavour, accepting that it splits behaviour
   between store and F-Droid builds.
3. Whether to drop the 30-day device key rotation, or carry reputation across it.
4. Whether a DPO must be appointed on the "regular and systematic monitoring" trigger -- needs Malaysian legal
   advice, not an engineering answer.
5. CDN placement, given the cross-border point in section 7.
