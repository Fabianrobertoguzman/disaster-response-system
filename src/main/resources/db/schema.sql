-- DRS-Enhanced relational schema (COIT20258 Assessment 3).
-- Executed programmatically at server start-up and also shipped for manual setup.
-- The schema is dropped and recreated so a stale database left by a previous run
-- cannot keep old table definitions (e.g. missing password_hash/salt columns).

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS incident_responders;
DROP TABLE IF EXISTS responders;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS partner_agencies;
DROP TABLE IF EXISTS incidents;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  uuid          CHAR(36)     NOT NULL UNIQUE,
  username      VARCHAR(64)  NOT NULL UNIQUE,
  role          VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  salt          VARCHAR(64)  NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE incidents (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  uuid                 CHAR(36)    NOT NULL UNIQUE,
  hazard_type          VARCHAR(32) NOT NULL,
  severity             VARCHAR(16) NOT NULL,
  status               VARCHAR(16) NOT NULL,
  latitude             DOUBLE      NOT NULL,
  longitude            DOUBLE      NOT NULL,
  description          TEXT,
  victim_count         INT         NOT NULL DEFAULT 0,
  reported_at          DATETIME    NOT NULL,
  recommended_template VARCHAR(32) NULL,
  reported_by          BIGINT      NULL,
  CONSTRAINT fk_incident_reporter FOREIGN KEY (reported_by)
      REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE responders (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  uuid               CHAR(36)     NOT NULL UNIQUE,
  name               VARCHAR(128) NOT NULL,
  current_tasking_id BIGINT       NULL,
  CONSTRAINT fk_responder_tasking FOREIGN KEY (current_tasking_id)
      REFERENCES incidents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE resources (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  uuid          CHAR(36)    NOT NULL UNIQUE,
  resource_type VARCHAR(64) NOT NULL,
  available     BOOLEAN     NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE partner_agencies (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(128) NOT NULL UNIQUE,
  available_units INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE incident_responders (
  incident_id  BIGINT   NOT NULL,
  responder_id BIGINT   NOT NULL,
  assigned_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (incident_id, responder_id),
  CONSTRAINT fk_ir_incident  FOREIGN KEY (incident_id)
      REFERENCES incidents(id)  ON DELETE CASCADE,
  CONSTRAINT fk_ir_responder FOREIGN KEY (responder_id)
      REFERENCES responders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  incident_id BIGINT      NOT NULL,
  agency_id   BIGINT      NOT NULL,
  ack_status  VARCHAR(16) NOT NULL,
  retries     INT         NOT NULL DEFAULT 0,
  ts          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_incident FOREIGN KEY (incident_id)
      REFERENCES incidents(id)        ON DELETE CASCADE,
  CONSTRAINT fk_notif_agency   FOREIGN KEY (agency_id)
      REFERENCES partner_agencies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id    BIGINT       NULL,
  incident_id BIGINT       NULL,
  action      VARCHAR(255) NOT NULL,
  entity      VARCHAR(64),
  entity_uuid CHAR(36),
  ts          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_actor    FOREIGN KEY (actor_id)
      REFERENCES users(id)     ON DELETE SET NULL,
  CONSTRAINT fk_audit_incident FOREIGN KEY (incident_id)
      REFERENCES incidents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
