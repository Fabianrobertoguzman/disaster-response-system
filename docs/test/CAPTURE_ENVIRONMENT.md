# Capture environment — where the D6 ACTUALs come from

COIT20258 Assessment 3 · DRS-Enhanced · Fabian Roberto Guzman (12287570)

Every **ACTUAL** value in the D6 test plan, and every D7 screenshot, is
transcribed from a real executed run on a stated environment — never inferred.
This file is that statement. (Per
[SKIPPED_VS_PASSED_POLICY.md](SKIPPED_VS_PASSED_POLICY.md), rows whose
environment precondition was absent are recorded as SKIPPED, not PASS.)

## Reference environment (developer machine)

| Item | Value |
|------|-------|
| OS | Windows 11 Home |
| JDK | 17 (project compiled and tested with `maven.compiler.source/target=17`) |
| Maven | Apache NetBeans-bundled Maven (`mvn` equivalent) |
| JavaFX | 17.0.6 (Maven dependency) |
| MySQL | MySQL 8.x on `localhost:3306`, user with `CREATE` privilege — **required only for the DB-gated rows**; when absent those rows SKIP |
| H2 (alternative) | in-memory, MySQL mode, via `mvn test -Ptest-h2` |

## The three run configurations

| Run | Command | What executes |
|-----|---------|---------------|
| Default (no DB) | `mvn test` | All unit/in-memory/socket tests PASS; MySQL-gated specs SKIP (yellow). |
| MySQL available | `mvn test` with MySQL up | Everything above **plus** the MySQL integration specs execute for real — this is the run D6 ACTUALs for DB rows are transcribed from. |
| H2 profile | `mvn test -Ptest-h2` | The H2-adopting database-backed tests run against in-memory H2 (MySQL mode) — the marker's database-free green path. Tests that have not yet adopted the substrate keep their MySQL probe-and-SKIP behaviour. |

## Capture procedure (performed at code freeze)

1. Freeze the code (all increments committed; this is the artefact being submitted).
2. Run `mvn clean test` on the reference environment **with MySQL running**;
   archive the Surefire output (`target/surefire-reports`) and transcribe each
   row's ACTUAL into the D6 table, marking PASS/FAIL/SKIPPED truthfully.
3. Run `mvn clean test -Ptest-h2`; capture the green bar for the H2 evidence shot.
4. Take the D7 screenshots (test tree, green bar **with the Skipped count
   visible**, red-then-green pair, running features) on the same environment,
   and caption each with this file's environment row.

The exact MySQL server version and the run date are filled in on the captured
artefacts at capture time, so they always describe the run that actually produced
the evidence.
