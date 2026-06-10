# DRS-Enhanced — COIT20258 Assessment 3

**Disaster Response System, enhanced (Stage 3).** A distributed three-tier Java application: a JavaFX/Scene Builder client talking to a multi-threaded server backed by a MySQL database. Built by enhancing the DRS-Initial prototype from Assessment 2.

Central Queensland University · COIT20258 Software Engineering · Term 1 2026.

## Status

- **Baseline imported**: this repository is seeded from the A2 DRS-Initial (the selected base), tagged `a2-baseline`. The A2 code is the single-process JavaFX prototype; A3 enhances it into the distributed client/server/MySQL system.

## Target architecture

```
JavaFX client (Scene Builder)  <-- sockets/DTOs -->  multi-threaded server  <-- JDBC/DAO -->  MySQL
   Presentation tier                                   Business tier                          Data tier
```

- **Toolchain**: Java 17, JavaFX 17.0.6, Maven, JUnit 5, mysql-connector-j.
- **Pattern**: MVP on the client (View + Presenter via a server stub) + a Service/Application layer on the server.
- **Security (spec §2.5)**: role-based access (login), server-side timestamping, genuine reversible encryption/decryption (AES-GCM field or TLS — password hashing is one-way and covers access-rights, not the §2.5 encryption measure), append-only audit log for non-repudiation.

## Build & run

```
mvn clean compile        # compile
mvn test                 # JUnit suite (DAO integration tests skip cleanly if MySQL is absent)
mvn javafx:run           # launch the client (use the javafx-maven-plugin goal, not a plain Run)
```

### Database setup

- **Prerequisite**: a running MySQL 8 server. The data tier connects with the settings in `db.properties` (repo root); edit `db.user`/`db.password`/`db.url` to match your install, or override with the `DB_USER` / `DB_PASSWORD` / `DB_URL` environment variables (no recompilation needed). The default user must hold the `CREATE` privilege, because `createDatabaseIfNotExist=true` lets the application create the `drs` database on first connection.
- **Schema and reference data** ship as SQL scripts at `src/main/resources/db/schema.sql` and `src/main/resources/db/seed.sql`. They are applied programmatically at start-up (the schema is dropped and recreated so a stale database cannot keep old definitions), and can also be run by hand to inspect or set up the database. A bundled copy of `db.properties` is also packaged on the classpath as a last-resort default, so the project compiles and runs from a clean extract on a fresh machine.
- The JUnit suite contains DAO integration tests that exercise real JDBC; when no MySQL server is reachable they are **skipped** (not failed), so `mvn test` still produces a green build on a database-free machine.

### Testing

```
mvn test                 # full suite; MySQL-gated specs SKIP (yellow) when no database is reachable
mvn test -Ptest-h2       # the database-backed tests run against in-memory H2 in MySQL mode instead
                         # (no MySQL needed - the full suite is green; see docs/test/H2_NOT_A_DROPIN.md)
```

- A **skipped** test is never reported as a pass — see `docs/test/SKIPPED_VS_PASSED_POLICY.md`.
- The H2 path uses a dedicated DDL (`src/test/resources/db/schema-h2.sql`) because H2 is **not** a MySQL drop-in; the dialect caveats are in `docs/test/H2_NOT_A_DROPIN.md`.
- Test-plan ACTUALs and evidence screenshots are captured per `docs/test/CAPTURE_ENVIRONMENT.md`.

### Running the distributed system (server + client)

The enhanced system is two processes. Start the server first, then one or more clients:

1. **Start MySQL** (see *Database setup* above).
2. **Start the server** — it applies the schema/seed and listens on a TCP port (default `5599`):
   - Double-click `run-server.bat` (Windows) or run `./run-server.sh` (macOS/Linux); or
   - In NetBeans: right-click `DrsServerLauncher.java` → **Run File**; or
   - On the command line: `mvn exec:java -Dexec.mainClass=edu.cqu.drs.server.DrsServerLauncher` (a port may be passed as the first argument).
3. **Start a client**: double-click `run-app.bat` (or `./run-app.sh`, or `mvn javafx:run`, or run `App` from NetBeans). The client reaches the server through `edu.cqu.drs.client.ServerStub`; launch it more than once to see concurrent multi-client dispatch.
4. **First login**: on a fresh database the server creates a default administrator — username **`admin`**, password **`admin12345`** (override with the `DRS_ADMIN_PASSWORD` environment variable; change it after first login) — plus demo accounts covering each demonstrable role, so every role can be exercised without writing SQL. Dispatch operations require the `DISPATCHER` or `ADMINISTRATOR` role; citizens may submit reports.

   | Username | Password | Role |
   |----------|----------|------|
   | `admin` | `admin12345` | ADMINISTRATOR |
   | `dispatch1` / `dispatch2` | `dispatch12345` | DISPATCHER (two accounts, so two dispatchers can share the live board) |
   | `citizen1` | `citizen12345` | CITIZEN |

**Security configuration (§2.5).** Passwords are stored as salted PBKDF2 hashes (one-way). Sensitive incident descriptions are encrypted at rest with AES-256-GCM (genuine, reversible — distinct from hashing). The encryption key is read from the `DRS_FIELD_KEY` environment variable; if it is unset the server generates an ephemeral key and prints it at start-up — set `DRS_FIELD_KEY` to that value to keep encrypted data readable across restarts. Every login/logout and mutating action is written, server-timestamped, to the append-only audit trail (non-repudiation).

The client/server protocol lives in the shared `edu.cqu.drs.protocol` package; the multi-threaded server (`edu.cqu.drs.server.DrsServer`) serves one pooled `ClientHandler` per connection.

## What carries over from Assessments 1 and 2

Domain model (Incident, IncidentQueue, Responder, Resource, User/UserRole, AuditLog, GpsCoordinate, AlertTemplate, the enums, IPartnerAgency + 8 stubs), the MVP structure, the 95-test JUnit suite, the A1 use-case/sequence/class diagrams, and the A1 requirements register.
