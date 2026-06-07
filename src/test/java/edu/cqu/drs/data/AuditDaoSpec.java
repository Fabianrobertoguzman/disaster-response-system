package edu.cqu.drs.data;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link AuditDaoImpl}, the persistent append-only audit
 * trail.
 *
 * <p>Skipped (not failed) when no database is reachable - see
 * {@link DatabaseTestSupport}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("AuditDao - persistent audit trail (MySQL)")
class AuditDaoSpec {

    private Database database;
    private AuditDao dao;
    private IncidentDao incidentDao;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "MySQL not reachable - AuditDao integration tests skipped");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.database = DatabaseTestSupport.freshDatabase();
        this.dao = new AuditDaoImpl(this.database);
        this.incidentDao = new IncidentDaoImpl(this.database);
    }

    /**
     * Inserts and returns a persisted incident the audit entries can reference.
     *
     * @return the stored incident.
     */
    private Incident persistedIncident() {
        Incident incident = new Incident(UUID.randomUUID(), HazardType.FLOOD, Severity.MEDIUM,
                new GpsCoordinate(-23.3781, 150.5136), "Riverbank overflow", 0,
                LocalDateTime.of(2026, 6, 8, 10, 0, 0), IncidentStatus.REPORTED, null, null);
        this.incidentDao.insert(incident);
        return incident;
    }

    @Test
    @DisplayName("an incident-scoped entry round-trips its action, entity and timestamp")
    void shouldRecordIncidentScopedEntry() {
        Incident incident = persistedIncident();
        UUID entityUuid = incident.getId();
        this.dao.record(new AuditEntry(null, incident.getId(),
                "Triaged: severity=MEDIUM", "Incident", entityUuid));

        List<AuditEntry> entries = this.dao.findByIncident(incident.getId());
        assertEquals(1, entries.size());
        AuditEntry entry = entries.get(0);
        assertEquals("Triaged: severity=MEDIUM", entry.getAction());
        assertEquals("Incident", entry.getEntity());
        assertEquals(entityUuid, entry.getEntityUuid());
        assertEquals(incident.getId(), entry.getIncidentUuid());
        assertNull(entry.getActorUuid());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    @DisplayName("a system event with no actor and no incident is appended and listed")
    void shouldRecordSystemEvent() {
        this.dao.record(new AuditEntry(null, null, "System started", null, null));

        List<AuditEntry> all = this.dao.findAll();
        assertEquals(1, all.size());
        assertEquals("System started", all.get(0).getAction());
        assertNull(all.get(0).getIncidentUuid());
    }

    @Test
    @DisplayName("findByIncident returns only the entries for the given incident")
    void shouldFilterByIncident() {
        Incident first = persistedIncident();
        Incident second = persistedIncident();
        this.dao.record(new AuditEntry(null, first.getId(), "Reported", "Incident", first.getId()));
        this.dao.record(new AuditEntry(null, second.getId(), "Reported", "Incident", second.getId()));
        this.dao.record(new AuditEntry(null, first.getId(), "Resolved", "Incident", first.getId()));

        assertEquals(2, this.dao.findByIncident(first.getId()).size());
        assertEquals(1, this.dao.findByIncident(second.getId()).size());
    }

    @Test
    @DisplayName("recording a null entry is rejected")
    void shouldRejectNullEntry() {
        assertThrows(IllegalArgumentException.class, () -> this.dao.record(null));
    }

    @Test
    @DisplayName("referencing an unknown incident is rejected as a data-integrity error")
    void shouldRejectUnknownIncident() {
        assertThrows(DataAccessException.class, () -> this.dao.record(
                new AuditEntry(null, UUID.randomUUID(), "Orphan", "Incident", UUID.randomUUID())));
    }
}
