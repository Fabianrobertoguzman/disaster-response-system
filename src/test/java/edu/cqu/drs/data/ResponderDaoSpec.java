package edu.cqu.drs.data;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link ResponderDaoImpl}, including the current-tasking
 * foreign key to an incident.
 *
 * <p>Skipped (not failed) when no database is reachable - see
 * {@link DatabaseTestSupport}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("ResponderDao - responder persistence (MySQL)")
class ResponderDaoSpec {

    /** Number of responders created by the reference seed. */
    private static final int SEEDED_RESPONDERS = 6;

    private Database database;
    private ResponderDao dao;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "MySQL not reachable - ResponderDao integration tests skipped");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.database = DatabaseTestSupport.freshDatabase();
        this.dao = new ResponderDaoImpl(this.database);
    }

    @Test
    @DisplayName("a newly inserted responder is available and round-trips its name")
    void shouldRoundTripAvailableResponder() {
        Responder responder = new Responder("Golf");
        this.dao.insert(responder);

        Responder loaded = this.dao.findByUuid(responder.getId()).orElseThrow();
        assertEquals("Golf", loaded.getName());
        assertTrue(loaded.isAvailable());
        assertNull(loaded.getCurrentTaskingId());
    }

    @Test
    @DisplayName("findAll returns the seeded responders plus any newly inserted one")
    void shouldListSeededAndInserted() {
        assertEquals(SEEDED_RESPONDERS, this.dao.findAll().size());

        this.dao.insert(new Responder("Hotel"));
        assertEquals(SEEDED_RESPONDERS + 1, this.dao.findAll().size());
    }

    @Test
    @DisplayName("update persists a renamed responder")
    void shouldPersistRename() {
        Responder responder = new Responder("India");
        this.dao.insert(responder);

        responder.setName("India-Renamed");
        this.dao.update(responder);

        assertEquals("India-Renamed",
                this.dao.findByUuid(responder.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("a tasked responder round-trips the tasking incident id and is unavailable")
    void shouldRoundTripTasking() {
        Incident incident = new Incident(UUID.randomUUID(), HazardType.FIRE, Severity.HIGH,
                new GpsCoordinate(-23.3781, 150.5136), "Warehouse blaze", 3,
                LocalDateTime.of(2026, 6, 8, 9, 15, 0), IncidentStatus.TRIAGED, null, null);
        new IncidentDaoImpl(this.database).insert(incident);

        Responder responder = new Responder("Juliet");
        this.dao.insert(responder);
        responder.setCurrentTaskingId(incident.getId());
        this.dao.update(responder);

        Responder loaded = this.dao.findByUuid(responder.getId()).orElseThrow();
        assertEquals(incident.getId(), loaded.getCurrentTaskingId());
        assertFalse(loaded.isAvailable());
    }

    @Test
    @DisplayName("setting a current tasking to an unknown incident is rejected")
    void shouldRejectUnknownTasking() {
        Responder responder = new Responder("Mike");
        this.dao.insert(responder);

        responder.setCurrentTaskingId(UUID.randomUUID());
        assertThrows(DataAccessException.class, () -> this.dao.update(responder));
    }

    @Test
    @DisplayName("findByUuid returns empty for an unknown id")
    void shouldReturnEmptyForUnknownId() {
        assertTrue(this.dao.findByUuid(UUID.randomUUID()).isEmpty());
    }
}
