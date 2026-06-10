package edu.cqu.drs.data;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Always-green analytics tests over the in-memory store: the aggregate logic of
 * feature f2 pinned with zero database, using the same {@link AnalyticsFixture}
 * (and therefore the same expected values) the MySQL-backed spec uses - the
 * cross-backend parity seam. Also pins the regression the data tier depends on:
 * a stored copy preserves {@code resolvedAt}.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Analytics (f2) - aggregate logic over the in-memory store")
class AnalyticsInMemorySpec {

    private InMemoryDataStore store;
    private AnalyticsDao analytics;

    @BeforeEach
    void setUp() {
        this.store = new InMemoryDataStore();
        this.analytics = this.store.analyticsDao();
    }

    @Test
    @DisplayName("an empty store yields empty maps, zero victims and no response times")
    void shouldHandleEmptyStore() {
        assertTrue(this.analytics.countByHazard().isEmpty());
        assertTrue(this.analytics.countBySeverity().isEmpty());
        assertTrue(this.analytics.countByStatus().isEmpty());
        assertEquals(0, this.analytics.totalVictims());
        assertTrue(this.analytics.responseMinutes().isEmpty());
    }

    @Test
    @DisplayName("the fixture's hazard, severity and status counts match the hand-derived expecteds")
    void shouldCountFixture() {
        AnalyticsFixture.seed(this.store.incidentDao());

        // Full map equality: every count right AND hazards with no incidents
        // absent (EARTHQUAKE is not a key), never zero.
        assertEquals(AnalyticsFixture.EXPECTED_BY_HAZARD, this.analytics.countByHazard());
        assertNull(this.analytics.countByHazard().get(HazardType.EARTHQUAKE),
                "a hazard with no incidents must be absent, not zero");
        assertEquals(AnalyticsFixture.EXPECTED_BY_SEVERITY, this.analytics.countBySeverity());
        assertEquals(AnalyticsFixture.EXPECTED_BY_STATUS, this.analytics.countByStatus());
    }

    @Test
    @DisplayName("the victim total is summed from the stored incidents, never hardcoded")
    void shouldSumVictims() {
        AnalyticsFixture.seed(this.store.incidentDao());
        assertEquals(AnalyticsFixture.EXPECTED_TOTAL_VICTIMS, this.analytics.totalVictims());
    }

    @Test
    @DisplayName("response minutes are derived in Java from the timestamp pairs of resolved incidents")
    void shouldDeriveResponseMinutes() {
        AnalyticsFixture.seed(this.store.incidentDao());
        List<Long> minutes = new java.util.ArrayList<>(this.analytics.responseMinutes());
        java.util.Collections.sort(minutes);
        assertEquals(AnalyticsFixture.EXPECTED_RESPONSE_MINUTES, minutes);
    }

    @Test
    @DisplayName("a stored copy preserves resolvedAt (the copyOf regression the analytics depend on)")
    void shouldPreserveResolvedAtInCopies() {
        LocalDateTime reported = LocalDateTime.of(2026, 6, 9, 9, 0, 0);
        LocalDateTime resolved = reported.plusMinutes(20);
        Incident incident = new Incident(UUID.randomUUID(), HazardType.FIRE, Severity.HIGH,
                new GpsCoordinate(-23.0, 150.0), "copy check", 1,
                reported, IncidentStatus.RESOLVED, null, null);
        incident.setResolvedAt(resolved);

        this.store.incidentDao().insert(incident);
        Incident loaded = this.store.incidentDao().findByUuid(incident.getId()).orElseThrow();
        assertEquals(resolved, loaded.getResolvedAt());

        // An open incident's copy must keep resolvedAt null.
        Incident open = new Incident(UUID.randomUUID(), HazardType.STORM, Severity.LOW,
                new GpsCoordinate(-23.0, 150.0), "open", 0,
                reported, IncidentStatus.REPORTED, null, null);
        this.store.incidentDao().insert(open);
        assertNull(this.store.incidentDao().findByUuid(open.getId()).orElseThrow().getResolvedAt());
    }

    @Test
    @DisplayName("resolve() stamps resolvedAt on the live path too")
    void shouldStampOnLiveResolve() {
        Incident incident = new Incident(HazardType.FLOOD,
                new GpsCoordinate(-23.0, 150.0), "live", 0);
        assertNull(incident.getResolvedAt());
        incident.resolve();
        assertNotNull(incident.getResolvedAt());
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }
}
