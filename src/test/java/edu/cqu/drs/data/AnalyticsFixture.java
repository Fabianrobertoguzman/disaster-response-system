package edu.cqu.drs.data;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The deterministic incident dataset behind every analytics test: six incidents
 * spread across hazards, severities and statuses, three of them resolved with
 * hand-checkable response times. Seeding goes through the {@link IncidentDao}
 * <em>interface</em>, so the very same fixture - and therefore the very same
 * expected values - drives the MySQL implementation, the in-memory view, and
 * (later) the H2 profile: one source of truth for the cross-backend parity
 * the test plan relies on.
 *
 * <p>Expected aggregates, derivable by hand from the table below:
 * hazards FIRE=2, FLOOD=2, STORM=1, HAZMAT=1; severities CRITICAL=2, HIGH=2,
 * MEDIUM=1, LOW=1; statuses TRIAGED=2, RESOLVED=3, REPORTED=1; victims total
 * 24; response minutes {30, 60, 45} (average 45, maximum 60).</p>
 *
 * <p>Intentionally <strong>not</strong> named {@code *Spec}, so Surefire does
 * not collect it as a test.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
final class AnalyticsFixture {

    /** Number of incidents the fixture seeds. */
    static final int TOTAL_INCIDENTS = 6;

    /** Expected victim total across the fixture. */
    static final long EXPECTED_TOTAL_VICTIMS = 24;

    /**
     * Expected per-incident response times in minutes, ASCENDING, so a spec can
     * sort the actual list and assert full multiset equality.
     */
    static final List<Long> EXPECTED_RESPONSE_MINUTES = List.of(30L, 45L, 60L);

    /** Expected incident count per hazard (hazards with no incidents are absent). */
    static final Map<HazardType, Long> EXPECTED_BY_HAZARD = Map.of(
            HazardType.FIRE, 2L, HazardType.FLOOD, 2L,
            HazardType.STORM, 1L, HazardType.HAZMAT, 1L);

    /** Expected incident count per severity. */
    static final Map<Severity, Long> EXPECTED_BY_SEVERITY = Map.of(
            Severity.CRITICAL, 2L, Severity.HIGH, 2L,
            Severity.MEDIUM, 1L, Severity.LOW, 1L);

    /** Expected incident count per lifecycle status. */
    static final Map<IncidentStatus, Long> EXPECTED_BY_STATUS = Map.of(
            IncidentStatus.TRIAGED, 2L, IncidentStatus.RESOLVED, 3L,
            IncidentStatus.REPORTED, 1L);

    /** Fixed reference day for the fixture's timestamps. */
    private static final LocalDateTime DAY = LocalDateTime.of(2026, 6, 9, 8, 0, 0);

    private AnalyticsFixture() {
    }

    /**
     * Seeds the deterministic dataset through the given DAO.
     *
     * @param dao the incident store to seed (must not be null).
     */
    static void seed(IncidentDao dao) {
        // Open: FIRE/CRITICAL/5 TRIAGED, STORM/LOW/0 REPORTED, FLOOD/CRITICAL/4 TRIAGED.
        dao.insert(incident(HazardType.FIRE, Severity.CRITICAL, IncidentStatus.TRIAGED,
                5, DAY.plusHours(2), null));
        dao.insert(incident(HazardType.STORM, Severity.LOW, IncidentStatus.REPORTED,
                0, DAY.plusHours(3), null));
        dao.insert(incident(HazardType.FLOOD, Severity.CRITICAL, IncidentStatus.TRIAGED,
                4, DAY.plusHours(4), null));
        // Resolved: FIRE/HIGH/3 in 30 min, FLOOD/MEDIUM/2 in 60 min, HAZMAT/HIGH/10 in 45 min.
        dao.insert(incident(HazardType.FIRE, Severity.HIGH, IncidentStatus.RESOLVED,
                3, DAY.plusHours(2), DAY.plusHours(2).plusMinutes(30)));
        dao.insert(incident(HazardType.FLOOD, Severity.MEDIUM, IncidentStatus.RESOLVED,
                2, DAY.plusHours(1), DAY.plusHours(2)));
        dao.insert(incident(HazardType.HAZMAT, Severity.HIGH, IncidentStatus.RESOLVED,
                10, DAY, DAY.plusMinutes(45)));
    }

    /**
     * Builds one fixture incident via the reconstruction constructor (fixed
     * timestamps, no audit side effects).
     *
     * @param hazard     the hazard type.
     * @param severity   the severity.
     * @param status     the lifecycle status.
     * @param victims    the victim count.
     * @param reportedAt the fixed report time.
     * @param resolvedAt the fixed resolution time, or null while open.
     * @return the incident, ready to insert.
     */
    private static Incident incident(HazardType hazard, Severity severity,
            IncidentStatus status, int victims, LocalDateTime reportedAt,
            LocalDateTime resolvedAt) {
        Incident incident = new Incident(UUID.randomUUID(), hazard, severity,
                new GpsCoordinate(-23.3781, 150.5136), "fixture", victims,
                reportedAt, status, null, null);
        incident.setResolvedAt(resolvedAt);
        return incident;
    }
}
