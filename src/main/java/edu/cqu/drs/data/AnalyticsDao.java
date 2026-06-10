package edu.cqu.drs.data;

import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;

import java.util.List;
import java.util.Map;

/**
 * Data Access Object for the analytics aggregates behind the Damage Assessment
 * &amp; Analytics Dashboard (new feature f2).
 *
 * <p>Every aggregate is computed from the stored incidents - never hardcoded:
 * the count maps come from SQL {@code GROUP BY} queries (or the equivalent walk
 * of the in-memory store under test) and the victim total from SQL {@code SUM}.
 * Response times are deliberately returned as the raw per-incident minutes and
 * <em>derived in Java</em> from the {@code reported_at}/{@code resolved_at}
 * timestamp pair, not by SQL date arithmetic - date-difference functions are a
 * known MySQL/H2 dialect divergence (see {@code docs/test/H2_NOT_A_DROPIN.md}),
 * while plain timestamp reads and a Java subtraction behave identically on
 * both. The statistics (average/maximum) are computed by the service tier from
 * this list.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface AnalyticsDao {

    /**
     * Counts the stored incidents by hazard type.
     *
     * @return a map of hazard type to incident count; hazards with no incidents
     *         are absent (never null).
     * @throws DataAccessException if the query fails.
     */
    Map<HazardType, Long> countByHazard();

    /**
     * Counts the stored incidents by current severity.
     *
     * @return a map of severity to incident count; severities with no incidents
     *         are absent (never null).
     * @throws DataAccessException if the query fails.
     */
    Map<Severity, Long> countBySeverity();

    /**
     * Counts the stored incidents by lifecycle status.
     *
     * @return a map of status to incident count; statuses with no incidents are
     *         absent (never null).
     * @throws DataAccessException if the query fails.
     */
    Map<IncidentStatus, Long> countByStatus();

    /**
     * Sums the estimated victims across every stored incident.
     *
     * @return the victim total (0 for an empty store).
     * @throws DataAccessException if the query fails.
     */
    long totalVictims();

    /**
     * Returns the response time, in whole minutes, of every <em>resolved</em>
     * incident (resolution time minus report time, computed in Java).
     *
     * @return one entry per resolved incident, unordered (empty if none are
     *         resolved; never null).
     * @throws DataAccessException if the query fails.
     */
    List<Long> responseMinutes();
}
