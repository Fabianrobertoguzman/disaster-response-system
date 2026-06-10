# Skipped vs Passed — test-result policy

COIT20258 Assessment 3 · DRS-Enhanced · Fabian Roberto Guzman (12287570)

The test plan (D6) and every reported run use a **three-valued** result column:

| Result | Meaning | How it happens |
|--------|---------|----------------|
| **PASS** (green) | The test executed and every assertion held. | Normal run. |
| **FAIL** (red) | The test executed and an assertion or the code under test failed. | A genuine defect — never shipped. |
| **SKIPPED** (yellow) | The test did **not** execute because a precondition of its environment was absent. | `assumeTrue(...)` aborted the test container. |

## Rules

1. **A skipped test is never reported as a pass.** The database-backed
   integration tests (`AuditDaoSpec`, `IncidentDaoSpec`, `ResourceDaoSpec`,
   `ResponderDaoSpec`, and later DB-gated specs) call
   `assumeTrue(DatabaseTestSupport.available())`: when no MySQL server is
   reachable they are *aborted*, which Surefire counts under `Skipped:`. Any
   green bar must be read **together with the Skipped count**; the D6 table
   records such rows as `SKIPPED`, with the environment that *did* execute them
   referenced from [CAPTURE_ENVIRONMENT.md](CAPTURE_ENVIRONMENT.md).
2. **`assumeTrue` is a graceful-degradation guard only.** It exists so
   `mvn test` stays green on a database-free machine (compile-and-run safety),
   not as a way to claim coverage. The availability probe is bounded
   (3-second login/connect timeout) so it skips fast instead of hanging.
3. **The H2 path turns yellow into green where it is honest to do so.**
   `mvn test -Ptest-h2` points the database-backed tests at in-memory H2 in
   MySQL mode (see [H2_NOT_A_DROPIN.md](H2_NOT_A_DROPIN.md)) - the production
   DAO SQL running on the H2 backend, so the whole suite is green with no MySQL
   server. A row proven on H2 is reported as a PASS *on H2*; a row whose SQL
   ever diverges between dialects would be tagged MySQL-only and remain SKIPPED
   off-MySQL rather than silently red (none diverge today - durations are
   computed in Java precisely to keep it that way).
4. **ACTUAL values in D6 are transcribed only from a real executed run** on the
   environment stated in CAPTURE_ENVIRONMENT.md — never inferred from the
   expected column.
