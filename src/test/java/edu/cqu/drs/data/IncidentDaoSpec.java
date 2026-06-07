package edu.cqu.drs.data;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link IncidentDaoImpl}, including the many-to-many
 * allocation of responders to incidents.
 *
 * <p>Skipped (not failed) when no database is reachable - see
 * {@link DatabaseTestSupport}. Timestamps use whole seconds so the round-trip is
 * exact regardless of the column's fractional-second precision.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("IncidentDao - incident persistence (MySQL)")
class IncidentDaoSpec {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 6, 8, 9, 15, 0);

    private Database database;
    private IncidentDao dao;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "MySQL not reachable - IncidentDao integration tests skipped");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.database = DatabaseTestSupport.freshDatabase();
        this.dao = new IncidentDaoImpl(this.database);
    }

    /**
     * Builds a triaged incident with a fixed identity and whole-second timestamp.
     *
     * @param template the recommended template, or null.
     * @return a reconstructed incident ready to insert.
     */
    private static Incident sampleIncident(AlertTemplate template) {
        return new Incident(UUID.randomUUID(), HazardType.FIRE, Severity.HIGH,
                new GpsCoordinate(-23.3781, 150.5136), "Warehouse blaze", 3,
                WHEN, IncidentStatus.TRIAGED, template, null);
    }

    @Test
    @DisplayName("insert then findByUuid round-trips every stored field")
    void shouldRoundTripIncident() {
        Incident incident = sampleIncident(null);
        this.dao.insert(incident);

        Incident loaded = this.dao.findByUuid(incident.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(incident.getId(), loaded.getId()),
                () -> assertEquals(HazardType.FIRE, loaded.getHazardType()),
                () -> assertEquals(Severity.HIGH, loaded.getSeverity()),
                () -> assertEquals(IncidentStatus.TRIAGED, loaded.getStatus()),
                () -> assertEquals(-23.3781, loaded.getGpsLocation().getLatitude()),
                () -> assertEquals(150.5136, loaded.getGpsLocation().getLongitude()),
                () -> assertEquals("Warehouse blaze", loaded.getDescription()),
                () -> assertEquals(3, loaded.getVictimCount()),
                () -> assertEquals(WHEN, loaded.getReportedAt()),
                () -> assertTrue(loaded.getResponders().isEmpty())
        );
    }

    @Test
    @DisplayName("a recommended template is persisted and recovered")
    void shouldRoundTripRecommendedTemplate() {
        Incident incident = sampleIncident(AlertTemplate.EVAC_NOTICE);
        this.dao.insert(incident);

        assertEquals(AlertTemplate.EVAC_NOTICE,
                this.dao.findByUuid(incident.getId()).orElseThrow().getRecommendedTemplate());
    }

    @Test
    @DisplayName("update persists a changed severity and status")
    void shouldPersistUpdate() {
        Incident incident = sampleIncident(null);
        this.dao.insert(incident);

        incident.setSeverity(Severity.CRITICAL);
        incident.setStatus(IncidentStatus.RESOLVED);
        this.dao.update(incident);

        Incident loaded = this.dao.findByUuid(incident.getId()).orElseThrow();
        assertEquals(Severity.CRITICAL, loaded.getSeverity());
        assertEquals(IncidentStatus.RESOLVED, loaded.getStatus());
    }

    @Test
    @DisplayName("assignResponder attaches the responder to the loaded incident and sets its tasking")
    void shouldAllocateResponder() {
        Incident incident = sampleIncident(null);
        this.dao.insert(incident);
        Responder responder = new Responder("Kilo");
        ResponderDao responderDao = new ResponderDaoImpl(this.database);
        responderDao.insert(responder);

        this.dao.assignResponder(incident.getId(), responder.getId());

        Incident loaded = this.dao.findByUuid(incident.getId()).orElseThrow();
        assertEquals(1, loaded.getResponders().size());
        assertEquals(responder.getId(), loaded.getResponders().get(0).getId());

        List<Responder> assigned = this.dao.findAssignedResponders(incident.getId());
        assertEquals(1, assigned.size());
        assertEquals(incident.getId(),
                responderDao.findByUuid(responder.getId()).orElseThrow().getCurrentTaskingId());
    }

    @Test
    @DisplayName("allocating the same responder twice is rejected and leaves a single allocation")
    void shouldRejectDuplicateAllocation() {
        Incident incident = sampleIncident(null);
        this.dao.insert(incident);
        Responder responder = new Responder("Lima");
        new ResponderDaoImpl(this.database).insert(responder);
        this.dao.assignResponder(incident.getId(), responder.getId());

        assertThrows(DataAccessException.class,
                () -> this.dao.assignResponder(incident.getId(), responder.getId()));
        assertEquals(1, this.dao.findAssignedResponders(incident.getId()).size());
    }

    @Test
    @DisplayName("findByUuid returns empty for an unknown id")
    void shouldReturnEmptyForUnknownId() {
        assertTrue(this.dao.findByUuid(UUID.randomUUID()).isEmpty());
    }
}
