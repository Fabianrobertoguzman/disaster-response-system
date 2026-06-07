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

## What carries over from Assessments 1 and 2

Domain model (Incident, IncidentQueue, Responder, Resource, User/UserRole, AuditLog, GpsCoordinate, AlertTemplate, the enums, IPartnerAgency + 8 stubs), the MVP structure, the 95-test JUnit suite, the A1 use-case/sequence/class diagrams, and the A1 requirements register.
