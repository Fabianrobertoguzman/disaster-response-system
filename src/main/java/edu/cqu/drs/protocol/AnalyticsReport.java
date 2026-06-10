package edu.cqu.drs.protocol;

import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * One server-assembled analytics report (new feature f2, the Damage Assessment
 * &amp; Analytics Dashboard): the incident counts by hazard, severity and
 * status, the victim total, the {@link ResponseTimeMetric response-time
 * statistics}, and the server-clock time the report was generated.
 *
 * <p>The count groups travel as enum-keyed maps rather than one wrapper class
 * per group: the maps are exactly the shape the data tier computes, they are
 * serialisable as-is, and the figures are assembled in one server pass and
 * delivered as one server-stamped artefact per round-trip - following the
 * {@link BoardSnapshot} precedent. (The underlying aggregates are separate
 * queries, so a write landing between them can skew one figure against
 * another by a single incident; the report does not claim point-in-time
 * snapshot isolation.) Hazards, severities or statuses with no incidents are
 * absent from their map (never a zero entry).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class AnalyticsReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Incident count per hazard type (absent key = no incidents). */
    private final EnumMap<HazardType, Long> hazardCounts;

    /** Incident count per severity (absent key = no incidents). */
    private final EnumMap<Severity, Long> severityCounts;

    /** Incident count per lifecycle status (absent key = no incidents). */
    private final EnumMap<IncidentStatus, Long> statusCounts;

    /** Estimated victims summed across every incident. */
    private final long totalVictims;

    /** Response-time statistics over the resolved incidents. */
    private final ResponseTimeMetric responseTimes;

    /** The server-clock time the report was generated. */
    private final LocalDateTime generatedAt;

    /**
     * Creates a report (maps are defensively copied).
     *
     * @param hazardCounts   incident count per hazard (must not be null).
     * @param severityCounts incident count per severity (must not be null).
     * @param statusCounts   incident count per status (must not be null).
     * @param totalVictims   the victim total.
     * @param responseTimes  the response-time statistics (must not be null).
     * @param generatedAt    the server-clock generation time (must not be null).
     * @throws IllegalArgumentException if any required argument is null.
     */
    public AnalyticsReport(Map<HazardType, Long> hazardCounts,
            Map<Severity, Long> severityCounts, Map<IncidentStatus, Long> statusCounts,
            long totalVictims, ResponseTimeMetric responseTimes, LocalDateTime generatedAt) {
        if (hazardCounts == null || severityCounts == null || statusCounts == null
                || responseTimes == null || generatedAt == null) {
            throw new IllegalArgumentException("all report components are required");
        }
        this.hazardCounts = copy(hazardCounts, HazardType.class);
        this.severityCounts = copy(severityCounts, Severity.class);
        this.statusCounts = copy(statusCounts, IncidentStatus.class);
        this.totalVictims = totalVictims;
        this.responseTimes = responseTimes;
        this.generatedAt = generatedAt;
    }

    /** @return an unmodifiable view of the per-hazard counts. */
    public Map<HazardType, Long> getHazardCounts() {
        return Collections.unmodifiableMap(this.hazardCounts);
    }

    /** @return an unmodifiable view of the per-severity counts. */
    public Map<Severity, Long> getSeverityCounts() {
        return Collections.unmodifiableMap(this.severityCounts);
    }

    /** @return an unmodifiable view of the per-status counts. */
    public Map<IncidentStatus, Long> getStatusCounts() {
        return Collections.unmodifiableMap(this.statusCounts);
    }

    /** @return the victim total across every incident. */
    public long getTotalVictims() {
        return this.totalVictims;
    }

    /** @return the response-time statistics over the resolved incidents. */
    public ResponseTimeMetric getResponseTimes() {
        return this.responseTimes;
    }

    /** @return the server-clock time the report was generated. */
    public LocalDateTime getGeneratedAt() {
        return this.generatedAt;
    }

    /** @return the total number of incidents (the status counts summed). */
    public long getTotalIncidents() {
        long total = 0;
        for (long count : this.statusCounts.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Copies an enum-keyed map into an {@link EnumMap} (empty maps allowed).
     *
     * @param <K>     the enum key type.
     * @param source  the map to copy.
     * @param keyType the enum class.
     * @return the copied map.
     */
    private static <K extends Enum<K>> EnumMap<K, Long> copy(Map<K, Long> source,
            Class<K> keyType) {
        EnumMap<K, Long> map = new EnumMap<>(keyType);
        map.putAll(source);
        return map;
    }

    /**
     * Hand-written display string (a default {@code Object.toString()} would
     * print only a Class@hash representation).
     *
     * @return e.g. {@code AnalyticsReport{incidents=6, victims=24,
     *         resolved=3, at=2026-06-10T11:00}}.
     */
    @Override
    public String toString() {
        return "AnalyticsReport{incidents=" + getTotalIncidents()
                + ", victims=" + this.totalVictims
                + ", resolved=" + this.responseTimes.getResolvedCount()
                + ", at=" + this.generatedAt + "}";
    }
}
