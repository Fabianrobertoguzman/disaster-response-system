package edu.cqu.drs.data;

import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link AnalyticsDaoImpl} against a real MySQL database
 * - the SQL aggregates of feature f2 over the shared {@link AnalyticsFixture},
 * asserting the very same expected values the in-memory spec asserts (the
 * cross-backend parity contract). Skipped (not failed) when no database is
 * reachable - see {@link DatabaseTestSupport}.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("AnalyticsDao - f2 SQL aggregates (MySQL)")
class AnalyticsDaoSpec {

    private Database database;
    private AnalyticsDao analytics;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "MySQL not reachable - AnalyticsDao integration tests skipped");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.database = DatabaseTestSupport.freshDatabase();
        this.analytics = new AnalyticsDaoImpl(this.database);
    }

    @Test
    @DisplayName("an empty incident table yields empty maps, zero victims and no response times")
    void shouldHandleEmptyTable() {
        assertTrue(this.analytics.countByHazard().isEmpty());
        assertTrue(this.analytics.countBySeverity().isEmpty());
        assertTrue(this.analytics.countByStatus().isEmpty());
        assertEquals(0, this.analytics.totalVictims());
        assertTrue(this.analytics.responseMinutes().isEmpty());
    }

    @Test
    @DisplayName("the GROUP BY counts match the fixture's hand-derived expecteds")
    void shouldCountFixture() {
        AnalyticsFixture.seed(new IncidentDaoImpl(this.database));

        // Full map equality against the SAME constants the in-memory spec uses -
        // the cross-backend parity contract in one assertion per aggregate.
        assertEquals(AnalyticsFixture.EXPECTED_BY_HAZARD, this.analytics.countByHazard());
        assertNull(this.analytics.countByHazard().get(HazardType.EARTHQUAKE),
                "a hazard with no incidents must be absent, not zero");
        assertEquals(AnalyticsFixture.EXPECTED_BY_SEVERITY, this.analytics.countBySeverity());
        assertEquals(AnalyticsFixture.EXPECTED_BY_STATUS, this.analytics.countByStatus());
    }

    @Test
    @DisplayName("SUM(victim_count) matches the fixture total")
    void shouldSumVictims() {
        AnalyticsFixture.seed(new IncidentDaoImpl(this.database));
        assertEquals(AnalyticsFixture.EXPECTED_TOTAL_VICTIMS, this.analytics.totalVictims());
    }

    @Test
    @DisplayName("response minutes derive in Java from the persisted timestamp pairs")
    void shouldDeriveResponseMinutes() {
        AnalyticsFixture.seed(new IncidentDaoImpl(this.database));
        List<Long> minutes = new java.util.ArrayList<>(this.analytics.responseMinutes());
        java.util.Collections.sort(minutes);
        assertEquals(AnalyticsFixture.EXPECTED_RESPONSE_MINUTES, minutes);
    }
}
