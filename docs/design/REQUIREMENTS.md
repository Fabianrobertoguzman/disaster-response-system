# DRS-Enhanced — Requirements Specification (R1–R5)

COIT20258 Assessment 3 · Fabian Roberto Guzman (12287570)

This document specifies the requirements of the enhanced, distributed three-tier
Disaster Response System. Items marked **(carried)** are inherited from the A1/A2
requirements catalogue; **(new)** items arise from the move to a client/server/
MySQL architecture and the two new features. Status reflects the current build:
**Implemented**, or **Planned** (scheduled for the feature increment).

---

## R1 — Functional requirements

| ID | Requirement | Origin | Status |
|----|-------------|--------|--------|
| FR-01 | A citizen can submit an incident report (hazard type, GPS location, description, estimated victims). | carried | Implemented |
| FR-02 | The system records each report's location and creation time automatically. | carried | Implemented |
| FR-03 | Each incident is assigned a unique identifier acting as an acknowledgement. | carried | Implemented |
| FR-04 | A dispatcher can triage an incident, assigning a severity; the queue is ordered most-urgent-first. | carried | Implemented |
| FR-05 | A dispatcher can allocate one or more responders (max 8) to an incident. | carried | Implemented |
| FR-06 | A dispatcher can mark an incident resolved. | carried | Implemented |
| FR-07 | The system recommends a CAP-style public-alert template for an incident (rule-driven). | carried (cf1) | Implemented |
| FR-08 | The system maintains an append-only audit log of significant actions. | carried (FR-14) | Implemented |
| FR-16 | A client connects to the server over TCP and exchanges serialised request/response messages. | new | Implemented |
| FR-17 | A user logs in with a username and password before performing protected actions. | new | Implemented |
| FR-18 | The server authorises each protected action against the user's role. | new | Implemented |
| FR-19 | Incidents, responders, users and audit entries are persisted in MySQL with database-generated IDs. | new | Implemented |
| FR-20 | A dispatcher can list current incidents and the responder roster from the server. | new | Implemented |
| FR-21 | A user can log out, ending their session. | new | Implemented |
| FR-LB-01 | A live multi-dispatcher board shows the current incident queue, refreshed periodically. | new (f1) | Planned |
| FR-AN-01 | An analytics dashboard reports incident counts, severity distribution and response metrics. | new (f2) | Planned |

> **Security is not a feature.** The §2.5 security measures (R2) satisfy a
> mandatory requirement; they are not counted among the two new features
> (FR-LB / FR-AN), which are genuinely new domain capabilities.

## R2 — Non-functional requirements

| ID | Requirement | Measure | Status |
|----|-------------|---------|--------|
| NFR-P01 | A triage/dispatch action completes well within a 2-second budget. | `DispatchPresenter.EXPECTED_LATENCY_MS` | Implemented |
| NFR-CONC-01 | The server serves multiple clients concurrently without lost updates or corruption. | multi-threaded server + thread-safe data tier; `ConcurrencySpec` (@RepeatedTest) | Implemented |
| NFR-PORT-01 | The system targets Java 17, JavaFX 17.0.6 and MySQL 8. | `pom.xml` pinned versions | Implemented |
| **NFR-SEC-ACCESS** | **Access rights**: passwords stored as salted one-way hashes; every protected action gated by role. | `PasswordHasher` (PBKDF2-HMAC-SHA-256, 210k iters, 16-byte salt); `AuthService.requireRole` | Implemented |
| **NFR-SEC-ENCRYPTION** | **Encryption/decryption**: a sensitive field is protected with genuine *reversible* encryption (AES-256-GCM). Password hashing is one-way and does **not** satisfy this measure. | `FieldCipher` (AES/GCM/NoPadding, per-record IV, auth tag); applied to the incident description at rest | Implemented |
| **NFR-SEC-TIMESTAMP** | **Time stamping**: every audit entry is stamped by the server, not the client. | DB `CURRENT_TIMESTAMP` default; server-side stamp in the in-memory double | Implemented |
| **NFR-SEC-NONREP** | **Non-repudiation**: an append-only audit trail records the actor, action and time of each significant event. | `audit_log` table; `AuditDao`; audit-on-write in the services and `AuthService` | Implemented |

> **Stated quality conflict.** Strengthening the PBKDF2 work factor (security)
> increases login latency (performance). The chosen 210,000 iterations balances
> resistance to offline attack against an interactive login still completing in
> well under a second on the target hardware.

## R3 — System (platform) requirements

| Item | Requirement |
|------|-------------|
| Java | JDK 17 (the build is compiled and tested on JDK 17). |
| Build | Apache Maven (the NetBeans-bundled Maven is sufficient). |
| UI | JavaFX 17.0.6 (`javafx-controls`, `javafx-fxml`); launch via `mvn javafx:run`. |
| Database | MySQL 8.x reachable on `localhost:3306` (configurable). The connecting user must hold the `CREATE` privilege, because `createDatabaseIfNotExist=true` lets the server create the `drs` database on first connection. |
| JDBC | `mysql-connector-j` 8.3.0 (on the Maven classpath). |
| Config | `db.properties` (editable) or `DB_URL`/`DB_USER`/`DB_PASSWORD` env vars; `DRS_FIELD_KEY` for the encryption key; `DRS_ADMIN_PASSWORD` to override the bootstrap admin. |
| Tests | `mvn test` runs without a database — the DAO integration tests skip (not fail) when MySQL is unreachable. |

## R4 — User requirements (by role)

| Role | Can | Implemented surface |
|------|-----|---------------------|
| CITIZEN | Log in; submit an incident report; log out. | login + submit over `ServerStub` |
| DISPATCHER | Log in; list incidents and responders; triage; allocate responders; resolve; recommend a template. | full dispatch action set, role-gated |
| ADMINISTRATOR | All dispatcher actions (account management UI is a future surface). | role-gated; bootstrap admin created on first run |
| FIELD_RESPONDER / PUBLIC_INFORMATION_OFFICER / AUDITOR | Authenticate (their action sets are defined but the prototype surfaces the citizen/dispatcher operations). | role catalogue present in `UserRole` |

## R5 — The two new features

| Feature | Requirement summary | Tier distinction | Status |
|---------|---------------------|------------------|--------|
| **f1 — Live Multi-Dispatcher Board** | Multiple dispatchers see a shared, near-real-time incident board. Baseline: client-side polling of `LIST_INCIDENTS` on an interval; server-push is optional gold-plating layered only once the polling baseline is solid. | Exercises the full client↔server↔DB path under concurrent dispatchers; depends on the multi-threaded server and thread-safe queue already built. | Planned |
| **f2 — Damage Assessment & Analytics Dashboard** | Aggregate views over incidents: counts by hazard, severity distribution, and response-time metrics, computed by SQL aggregates. | Adds an analytics DAO of parameterised aggregate queries over the MySQL incident store. | Planned |

Both features are **new domain capabilities** (not the inherited cf1/cf3, and not
the §2.5 security work). Each will be delivered end-to-end across all three tiers,
and each new model class will carry a hand-written `toString()`.
