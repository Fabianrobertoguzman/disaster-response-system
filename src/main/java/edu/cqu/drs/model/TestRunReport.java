package edu.cqu.drs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An immutable value object summarising the result of a self-test run.
 *
 * <p>Produced by creative feature FR-CR-02 ({@code SelfTestLauncher}), which runs an in-process
 * suite of smoke checks against the domain model from inside the GUI and summarises the run into one
 * of these. The Tools menu then displays the report's fields. Because the report is immutable it has
 * accessors and a constructor but no mutators  -  the "mutators where necessary" of the
 * Assessment&nbsp;2 coding mandate; none are necessary for a value object.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class TestRunReport {

    /** Total number of tests that started. */
    private final long testsRun;

    /** Number of tests that passed. */
    private final long passed;

    /** Number of tests that failed (or errored). */
    private final long failed;

    /** Wall-clock duration of the run in milliseconds. */
    private final long durationMs;

    /** One short summary string per failure (empty if the run was all green). */
    private final List<String> failureSummaries;

    /**
     * Creates a test-run report.
     *
     * @param testsRun         total tests started (must not be negative).
     * @param passed           tests passed (must not be negative).
     * @param failed           tests failed/errored (must not be negative).
     * @param durationMs        wall-clock duration in ms (must not be negative).
     * @param failureSummaries one summary per failure; null is treated as an empty list (defensively copied).
     * @throws IllegalArgumentException if any count or the duration is negative.
     */
    public TestRunReport(long testsRun, long passed, long failed, long durationMs, List<String> failureSummaries) {
        if (testsRun < 0 || passed < 0 || failed < 0 || durationMs < 0) {
            throw new IllegalArgumentException("counts and duration must not be negative");
        }
        this.testsRun = testsRun;
        this.passed = passed;
        this.failed = failed;
        this.durationMs = durationMs;
        this.failureSummaries = (failureSummaries == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(failureSummaries));
    }

    /** @return the total number of tests that started. */
    public long getTestsRun() {
        return this.testsRun;
    }

    /** @return the number of tests that passed. */
    public long getPassed() {
        return this.passed;
    }

    /** @return the number of tests that failed or errored. */
    public long getFailed() {
        return this.failed;
    }

    /** @return the wall-clock duration of the run in milliseconds. */
    public long getDurationMs() {
        return this.durationMs;
    }

    /** @return an unmodifiable list of one summary string per failure (empty if the run was all green). */
    public List<String> getFailureSummaries() {
        return this.failureSummaries;
    }

    /** @return true if no tests failed. */
    public boolean isAllGreen() {
        return this.failed == 0;
    }

    /**
     * Compact diagnostic format used by the Tools menu dialog and logs.
     *
     * @return e.g. {@code "TestRunReport{run=27, passed=27, failed=0, durationMs=412, allGreen=true}"}.
     */
    @Override
    public String toString() {
        return "TestRunReport{run=" + this.testsRun
                + ", passed=" + this.passed
                + ", failed=" + this.failed
                + ", durationMs=" + this.durationMs
                + ", allGreen=" + isAllGreen() + "}";
    }

    /**
     * Value equality on all fields.
     *
     * @param other the object to compare with.
     * @return true if {@code other} is a {@code TestRunReport} with the same counts, duration and failures.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TestRunReport)) {
            return false;
        }
        TestRunReport that = (TestRunReport) other;
        return this.testsRun == that.testsRun
                && this.passed == that.passed
                && this.failed == that.failed
                && this.durationMs == that.durationMs
                && this.failureSummaries.equals(that.failureSummaries);
    }

    /** @return a hash code consistent with {@link #equals(Object)}. */
    @Override
    public int hashCode() {
        return Objects.hash(this.testsRun, this.passed, this.failed, this.durationMs, this.failureSummaries);
    }
}
