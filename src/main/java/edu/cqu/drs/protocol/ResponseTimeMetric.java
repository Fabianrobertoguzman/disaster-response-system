package edu.cqu.drs.protocol;

import java.io.Serializable;
import java.util.List;

/**
 * The response-time statistics of the resolved incidents (new feature f2):
 * how many incidents have been resolved, and the minimum, average and maximum
 * minutes from report to resolution.
 *
 * <p>The statistics are derived <em>in Java</em> from the per-incident minutes
 * the data tier supplies (dialect-independent by design - see the analytics
 * DAO); an empty input yields the explicit zero metric rather than dividing by
 * zero. Immutable and serialisable for the wire.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class ResponseTimeMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How many incidents the statistics cover (the resolved ones). */
    private final int resolvedCount;

    /** Fastest resolution, in minutes (0 when nothing is resolved). */
    private final long minMinutes;

    /** Mean resolution time, in minutes (0 when nothing is resolved). */
    private final double averageMinutes;

    /** Slowest resolution, in minutes (0 when nothing is resolved). */
    private final long maxMinutes;

    /**
     * Computes the metric from the per-incident response minutes.
     *
     * @param responseMinutes one entry per resolved incident (must not be null;
     *                        empty yields the zero metric).
     * @throws IllegalArgumentException if {@code responseMinutes} is null.
     */
    public ResponseTimeMetric(List<Long> responseMinutes) {
        if (responseMinutes == null) {
            throw new IllegalArgumentException("responseMinutes must not be null");
        }
        this.resolvedCount = responseMinutes.size();
        long min = 0;
        long max = 0;
        double average = 0;
        if (!responseMinutes.isEmpty()) {
            long total = 0;
            min = Long.MAX_VALUE;
            for (long minutes : responseMinutes) {
                total += minutes;
                min = Math.min(min, minutes);
                max = Math.max(max, minutes);
            }
            average = (double) total / responseMinutes.size();
        }
        this.minMinutes = min;
        this.averageMinutes = average;
        this.maxMinutes = max;
    }

    /** @return how many resolved incidents the statistics cover. */
    public int getResolvedCount() {
        return this.resolvedCount;
    }

    /** @return the fastest resolution in minutes (0 when nothing is resolved). */
    public long getMinMinutes() {
        return this.minMinutes;
    }

    /** @return the mean resolution time in minutes (0 when nothing is resolved). */
    public double getAverageMinutes() {
        return this.averageMinutes;
    }

    /** @return the slowest resolution in minutes (0 when nothing is resolved). */
    public long getMaxMinutes() {
        return this.maxMinutes;
    }

    /**
     * Hand-written display string (a default {@code Object.toString()} would
     * print only a Class@hash representation).
     *
     * @return e.g. {@code ResponseTimeMetric{resolved=3, min=30m, avg=45.0m, max=60m}}.
     */
    @Override
    public String toString() {
        return "ResponseTimeMetric{resolved=" + this.resolvedCount
                + ", min=" + this.minMinutes + "m"
                + ", avg=" + this.averageMinutes + "m"
                + ", max=" + this.maxMinutes + "m}";
    }
}
