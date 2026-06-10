package edu.cqu.drs.server.service;

import edu.cqu.drs.data.AnalyticsDao;
import edu.cqu.drs.protocol.AnalyticsReport;
import edu.cqu.drs.protocol.ResponseTimeMetric;

import java.time.LocalDateTime;

/**
 * Server-side business service for the analytics feature (f2): assembles one
 * consistent {@link AnalyticsReport} from the {@link AnalyticsDao} aggregates -
 * the counts the database groups, the victim total it sums, and the
 * response-time statistics derived in Java from the per-incident minutes -
 * stamped with the server clock.
 *
 * <p>This is a <em>Service/Application-layer</em> class on the server (the
 * business tier of the three-tier architecture), not a presenter: presentation
 * lives in the JavaFX client, which receives the finished report over the
 * wire. Like the other services it depends only on the DAO interface, so it
 * runs unchanged over MySQL or the in-memory store.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AnalyticsService {

    private final AnalyticsDao analyticsDao;

    /**
     * Creates the service over its data-access dependency.
     *
     * @param analyticsDao the analytics aggregates store (must not be null).
     * @throws IllegalArgumentException if {@code analyticsDao} is null.
     */
    public AnalyticsService(AnalyticsDao analyticsDao) {
        if (analyticsDao == null) {
            throw new IllegalArgumentException("analyticsDao must not be null");
        }
        this.analyticsDao = analyticsDao;
    }

    /**
     * Assembles the current analytics report.
     *
     * @return a server-stamped report of the stored incidents (never null; an
     *         empty store yields empty count maps and the zero metric).
     */
    public AnalyticsReport buildReport() {
        return new AnalyticsReport(
                this.analyticsDao.countByHazard(),
                this.analyticsDao.countBySeverity(),
                this.analyticsDao.countByStatus(),
                this.analyticsDao.totalVictims(),
                new ResponseTimeMetric(this.analyticsDao.responseMinutes()),
                LocalDateTime.now());
    }
}
