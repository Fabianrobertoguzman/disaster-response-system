# H2 is not a MySQL drop-in — dialect caveats for the test profile

COIT20258 Assessment 3 · DRS-Enhanced · Fabian Roberto Guzman (12287570)

The `-Ptest-h2` Maven profile runs the database-backed tests against **in-memory
H2 in MySQL compatibility mode** (`MODE=MySQL`), so a marker without a MySQL
server still gets a green data-tier run. H2's MySQL mode is a close but
imperfect emulation; this file records the constraints we build to and the
known divergences, so nobody mistakes an H2 pass for a full MySQL guarantee.

## How the substrate works

- **Dedicated DDL**: H2 cannot parse MySQL's `ENGINE=InnoDB DEFAULT
  CHARSET=utf8mb4` clauses, so the tests load
  `src/test/resources/db/schema-h2.sql` — a hand-maintained copy of
  `src/main/resources/db/schema.sql` written to the MySQL-8 ∩ H2 intersection
  (same tables, columns, types, keys and FK actions, no engine clauses).
  **Any production schema change must be mirrored there.**
- **Bootstrap**: `H2SchemaBootstrap` (test sources) opens
  `jdbc:h2:mem:<name>;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1` and
  applies the H2 DDL; each initialisation drops and recreates the tables,
  mirroring the production clean-slate reset.
- **Selection**: the profile passes `-Ddrs.test.db=h2`. The database-backed
  specs adopt that property in a later increment (when the analytics rows land);
  **until then they continue to probe for MySQL and SKIP when it is absent**, so
  `-Ptest-h2` currently behaves like the default run plus the H2 substrate
  self-test.
- `H2BootstrapSpec` proves the substrate itself (schema loads, eight tables,
  database-generated keys, clean-slate re-init) and runs unconditionally.

## Constraints we write SQL to (the safe intersection)

- `BIGINT AUTO_INCREMENT` primary keys, `CHAR/VARCHAR/TEXT`, `DOUBLE`, `INT`,
  `BOOLEAN`, `DATETIME DEFAULT CURRENT_TIMESTAMP`, composite primary keys, and
  `FOREIGN KEY ... ON DELETE CASCADE/SET NULL` behave equivalently.
- Plain `INSERT/UPDATE/SELECT ... WHERE`, `ORDER BY`, `GROUP BY` with
  `COUNT/SUM/AVG/MAX/MIN` are portable.
- The schema scripts keep to one-statement-per-`;` with no semicolons inside
  string literals (both loaders split naively on `;`).

## Known divergences (do NOT rely on these matching MySQL)

| Area | Risk | Our rule |
|------|------|----------|
| Date/time arithmetic (`TIMESTAMPDIFF`, `DATEDIFF`, …) | Semantics and rounding differ between H2 and MySQL 8. | Compute durations (e.g. response time) **in Java** from the two timestamps; keep SQL to plain reads/`GROUP BY` counts. |
| Error codes / `SQLState` on constraint violations | Both throw `SQLIntegrityConstraintViolationException`, but vendor codes differ. | Assert on the exception type, never the vendor code. |
| `utf8mb4` collation behaviour | H2 ignores MySQL collation clauses. | No test asserts collation-sensitive ordering of non-ASCII text. |
| MySQL-specific functions / `ON DUPLICATE KEY` etc. | Unsupported or different in H2. | Not used anywhere in the production DAOs. |
| Concurrency model (InnoDB row locks vs H2) | Lock behaviour under contention differs. | Concurrency tests run against the in-memory data store or a real MySQL, never H2. |

**Policy**: an aggregate or query is reported as *proven on H2* only when the
same test, with the same fixture and expected values, passes on both backends;
anything dialect-divergent is tagged MySQL-only and its D6 row is captured on
the stated MySQL environment (see
[CAPTURE_ENVIRONMENT.md](CAPTURE_ENVIRONMENT.md)).
