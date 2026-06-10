# D6 — Test Plan (input · expected · ACTUAL)

COIT20258 Assessment 3 · DRS-Enhanced · Fabian Roberto Guzman (12287570)

This is the master test plan. The mark-earning rows are the **net-new
client-server-database integration tests** written for DRS-Enhanced; the 95
unit tests inherited from DRS-Initial are listed as unit-tier context only
(§ Unit tier). Every ACTUAL below is transcribed from real executed runs on the
environment stated in [CAPTURE_ENVIRONMENT.md](CAPTURE_ENVIRONMENT.md); results
use the three-valued PASS / FAIL / SKIPPED convention of
[SKIPPED_VS_PASSED_POLICY.md](SKIPPED_VS_PASSED_POLICY.md).

## Verified runs the ACTUALs come from (2026-06-10)

| Run | Command | Result |
|-----|---------|--------|
| R1 — no database | `mvn test` | **219 run, 0 failures, 0 errors, 7 skipped** (the 7 DB-gated spec classes skip, never fail) — BUILD SUCCESS |
| R2 — H2 profile | `mvn test -Ptest-h2` | **244 run, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS |
| R3 — real MySQL 8.4.5 | `mvn test` with `DB_USER`/`DB_PASSWORD` set | **244 run, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS |

R2 and R3 execute the identical suite on both backends — the cross-backend
parity contract in practice.

## Net-new integration rows (the D6 mark-earning set)

Layer key: **DB** = JDBC DAO ↔ database · **SRV** = socket server/services ·
**CS** = client→server over a real socket · **CSD** = client→server→database.

| ID | Layer | Test (class · behaviour) | Precondition | Input | Expected | ACTUAL (R3 / R2) | Result |
|----|-------|--------------------------|--------------|-------|----------|------------------|--------|
| T-DB-01 | DB | `ResourceDaoSpec` · insert/find/update/list round-trips (4 tests) | clean seeded schema | a `Resource("Ambulance")` etc. | stored uuid/type/availability round-trip; seed count 3 | as expected on MySQL and H2 | PASS |
| T-DB-02 | DB | `ResponderDaoSpec` · persistence + tasking FK (6 tests) | clean seeded schema | responders, one tasked to a stored incident | tasking uuid round-trips; unknown tasking rejected (`DataAccessException`) | as expected on both backends | PASS |
| T-DB-03 | DB | `IncidentDaoSpec` · full-field round-trip, junction, `resolved_at` (7 tests) | clean seeded schema | reconstructed incidents incl. fixed timestamps | every column round-trips; duplicate allocation rejected by composite PK leaving one row; `resolved_at` null→stamped | as expected on both backends | PASS |
| T-DB-04 | DB | `AuditDaoSpec` · append-only trail (5 tests) | clean seeded schema | entries with/without actor/incident | server-stamped `ts`; incident filter; unknown refs rejected | as expected on both backends | PASS |
| T-DB-05 | DB | `AnalyticsDaoSpec` · f2 SQL aggregates (4 tests) | `AnalyticsFixture` (6 incidents) | GROUP BY/SUM queries + response pairs | counts FIRE2/FLOOD2/STORM1/HAZMAT1, severities 2/2/1/1, statuses 2/3/1, victims 24, minutes {30,45,60}; empty table → empty/zero | exact match on both backends | PASS |
| T-DB-06 | DB | `UserDaoSpec` · credential persistence (4 tests) | clean schema | user + PBKDF2 hash/salt | hash and salt round-trip AND verify against the original password; plain reads credential-free; duplicate username rejected, original row intact | as expected on both backends | PASS |
| T-SRV-01 | SRV | `ProtocolSerializationSpec` · wire round-trips (5 tests) | none (in-memory streams) | Request/Response/Incident graph/BoardSnapshot/AnalyticsReport | byte-exact object round-trip incl. enum maps and timestamps | as expected (every run) | PASS |
| T-SRV-02 | SRV | `IncidentServiceSpec` · business tier (9 tests) | in-memory store | submit/triage/assign/resolve/recommend | state transitions + audit-on-write + most-urgent-first | as expected (every run) | PASS |
| T-SRV-03 | SRV | `AnalyticsServiceSpec` · report assembly (5 tests) | shared fixture | `buildReport()` | fixture constants; min 30 / avg 45.0 / max 60; zero metric on empty | as expected (every run) | PASS |
| T-SRV-04 | SRV | `StreamHandshakeSpec` · V-M2 deadlock pin (1 test) | running server, silent raw socket | nothing (that is the test) | the server's `0xACED/5` stream header arrives unprompted within 5 s | header received | PASS |
| T-CS-01 | CS | `ServerStubIntegrationSpec` · open-mode e2e (9 tests) | in-memory server, ephemeral port | the full dispatch action set over the socket | each action lands server-side and returns the updated state | as expected (every run) | PASS |
| T-CS-02 | CS | `ClientSessionSpec` · threading contract (9 tests) | none | runAsync/signOut calls | off-caller-thread execution, exactly-one callback, ordering, graceful closed-session failure | as expected (every run) | PASS |
| T-CS-03 | CS | `ReportClientPresenterSpec` + `DispatchClientPresenterSpec` (15 tests) | in-memory server | the client presenters' surfaces | submissions/triage/assign/resolve land on the SERVER; BAD_REQUEST/NOT_FOUND surfaced; deterministic server-down refusal | as expected (every run) | PASS |
| T-CS-04 | CS | `ClientRoleAuthorizationSpec` · the role contract (6 tests) | secured in-memory server | citizen/dispatcher/admin sessions | PING open; LOGIN/LOGOUT any session; submit any role; dispatch set DISPATCHER/ADMIN only; UNAUTHORIZED otherwise | as expected (every run) | PASS |
| T-CS-05 | CS | `SecuritySpec` · §2.5 end-to-end (6 tests) | secured in-memory server | bad creds, forbidden roles, encrypted submit | UNAUTHORIZED paths; description ciphertext at rest, clear when served | as expected (every run) | PASS |
| T-CS-06 | CS | `LiveBoardPollingSpec` · f1 polling baseline (6 tests) | secured in-memory server | repeated GET_BOARD polls, cross-client submit | ordered snapshots, consistent counts, non-decreasing server timestamps, citizen gated | as expected (every run) | PASS |
| T-CS-07 | CS | `LiveBoardConcurrencySpec` · f1 under contention (×3 reps) | in-memory server | 6 clients submit 30 incidents while a watcher polls | totals never go backwards; final snapshot converges on exactly 30 | converged in all repetitions | PASS |
| T-CS-08 | CS | `ConcurrencySpec` · multi-client safety (×5 reps, 2 scenarios) | in-memory server | concurrent submits / concurrent allocations | no lost updates; cap-8 allocations all applied under the lock | as expected (every run) | PASS |
| T-CS-09 | CS | `AnalyticsWireSpec` · f2 over the wire (3 tests) | secured in-memory server | GET_ANALYTICS as dispatcher/citizen; analytics-less server | fixture report intact over the socket; citizen UNAUTHORIZED; clear disabled-analytics error | as expected (every run) | PASS |
| T-CSD-01 | CSD | `ServerStubDatabaseIntegrationSpec` · the full secured cycle (2 tests) | fully-wired server over the real database | login → submit → triage → assign (from the 6-row seed) → resolve → analytics | every step lands in the DATABASE: row RESOLVED, `resolved_at` persisted, description stored as **recoverable AES-GCM ciphertext** (decrypts back to the plaintext), analytics aggregates match | as expected on MySQL and H2 | PASS |
| T-H2-01 | DB | `H2BootstrapSpec` · the H2 substrate itself (3 tests) | none (in-memory H2) | schema-h2.sql load, insert, re-init | 8 tables, DB-generated keys, clean-slate reset | as expected (every run) | PASS |

**Result on R1 (no database):** rows T-DB-01..06 and T-CSD-01 are **SKIPPED**
(assume-gated, never failed); every other row passes. That skip is honest
yellow, not green — see the policy doc.

## Unit tier (context, not the mark-earning set)

The 95 inherited DRS-Initial unit tests (model + presenter packages: 14 spec
classes — 9 model + 5 presenter — from `AuditLogSpec` to `SelfTestLauncherSpec`)
continue to pass unchanged in every run. They validate the single-process domain
logic the enhanced system builds on and are counted here as regression context
only. A further 27 net-new unit tests also run in every tier: `AuthServiceSpec`
(10), `FieldCipherSpec` (6) and `PasswordHasherSpec` (5) pin the §2.5 security
primitives, and `AnalyticsInMemorySpec` (6) is the in-memory side of the f2
cross-backend parity contract cited by `AnalyticsDaoSpec`. The arithmetic thus
reconciles: 122 integration executions (the rows above) + 95 inherited +
27 net-new unit = **244**, the R2/R3 total.

## Evidence cross-reference (D7)

| Evidence | What it shows |
|----------|----------------|
| Screenshot: dispatcher console acceptance (2026-06-10 10:05) | login as admin; report→triage→assign→resolve cycle against MySQL |
| Screenshot: two-client live board (2026-06-10 10:50) | client A's triaged incident visible on client B's polling board, "Last updated … (server time)" |
| Screenshot: analytics dashboard (2026-06-10 11:48–11:53) | bar/pie charts + response times over live MySQL data |
| Surefire reports `target/surefire-reports` | the R1/R2/R3 runs transcribed above |

Final D7 captures are re-taken at code freeze on the stated environment, per
[CAPTURE_ENVIRONMENT.md](CAPTURE_ENVIRONMENT.md).
