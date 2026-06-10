package edu.cqu.drs.server.service;

import edu.cqu.drs.data.AnalyticsFixture;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.protocol.AnalyticsReport;
import edu.cqu.drs.protocol.ResponseTimeMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Always-green unit tests for {@link AnalyticsService} over the in-memory
 * store: the report assembly of feature f2 with zero database, driven by the
 * same shared fixture (and the same expected constants) the data-tier specs
 * use. The statistics derivation (min/avg/max in Java) is pinned here.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("AnalyticsService (f2) - report assembly over the in-memory store")
class AnalyticsServiceSpec {

    private InMemoryDataStore store;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        this.store = new InMemoryDataStore();
        this.service = new AnalyticsService(this.store.analyticsDao());
    }

    @Test
    @DisplayName("an empty store yields an empty report with the zero response metric")
    void shouldBuildEmptyReport() {
        AnalyticsReport report = this.service.buildReport();
        assertTrue(report.getHazardCounts().isEmpty());
        assertTrue(report.getSeverityCounts().isEmpty());
        assertTrue(report.getStatusCounts().isEmpty());
        assertEquals(0, report.getTotalIncidents());
        assertEquals(0, report.getTotalVictims());
        assertEquals(0, report.getResponseTimes().getResolvedCount());
        assertEquals(0, report.getResponseTimes().getMinMinutes());
        assertEquals(0.0, report.getResponseTimes().getAverageMinutes());
        assertEquals(0, report.getResponseTimes().getMaxMinutes());
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    @DisplayName("the fixture's report matches every shared expected constant")
    void shouldBuildFixtureReport() {
        AnalyticsFixture.seed(this.store.incidentDao());
        AnalyticsReport report = this.service.buildReport();

        assertEquals(AnalyticsFixture.EXPECTED_BY_HAZARD, report.getHazardCounts());
        assertEquals(AnalyticsFixture.EXPECTED_BY_SEVERITY, report.getSeverityCounts());
        assertEquals(AnalyticsFixture.EXPECTED_BY_STATUS, report.getStatusCounts());
        assertEquals(AnalyticsFixture.TOTAL_INCIDENTS, report.getTotalIncidents());
        assertEquals(AnalyticsFixture.EXPECTED_TOTAL_VICTIMS, report.getTotalVictims());
    }

    @Test
    @DisplayName("the response statistics derive min 30 / average 45 / max 60 from the fixture")
    void shouldDeriveStatistics() {
        AnalyticsFixture.seed(this.store.incidentDao());
        ResponseTimeMetric times = this.service.buildReport().getResponseTimes();

        assertEquals(3, times.getResolvedCount());
        assertEquals(30, times.getMinMinutes());
        assertEquals(45.0, times.getAverageMinutes());
        assertEquals(60, times.getMaxMinutes());
    }

    @Test
    @DisplayName("the report carries a hand-written toString and a generation time")
    void shouldDescribeItself() {
        AnalyticsFixture.seed(this.store.incidentDao());
        AnalyticsReport report = this.service.buildReport();
        assertTrue(report.toString().startsWith("AnalyticsReport{"));
        assertTrue(report.toString().contains("victims=24"));
        assertTrue(report.getResponseTimes().toString().contains("avg=45.0m"));
    }

    @Test
    @DisplayName("a null dependency or null metric input is rejected")
    void shouldRejectNulls() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsService(null));
        assertThrows(IllegalArgumentException.class, () -> new ResponseTimeMetric(null));
    }
}
