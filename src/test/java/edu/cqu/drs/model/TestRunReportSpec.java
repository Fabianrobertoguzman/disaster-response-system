package edu.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TestRunReport} (the immutable self-test result value object - FR-CR-02).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("TestRunReport - self-test result value object")
class TestRunReportSpec {

    @Test
    @DisplayName("an all-green report exposes its counts and reports isAllGreen() == true")
    void allGreenReport() {
        TestRunReport report = new TestRunReport(27, 27, 0, 412, null);
        assertEquals(27, report.getTestsRun());
        assertEquals(27, report.getPassed());
        assertEquals(0, report.getFailed());
        assertEquals(412, report.getDurationMs());
        assertTrue(report.getFailureSummaries().isEmpty());
        assertTrue(report.isAllGreen());
    }

    @Test
    @DisplayName("a report with failures reports isAllGreen() == false and lists the failures")
    void reportWithFailures() {
        List<String> failures = Arrays.asList("IncidentSpec.shouldTriage: expected TRIAGED");
        TestRunReport report = new TestRunReport(27, 26, 1, 500, failures);
        assertEquals(1, report.getFailed());
        assertFalse(report.isAllGreen());
        assertEquals(1, report.getFailureSummaries().size());
        assertEquals("IncidentSpec.shouldTriage: expected TRIAGED", report.getFailureSummaries().get(0));
    }

    @Test
    @DisplayName("a negative count or duration is rejected")
    void shouldRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new TestRunReport(-1, 0, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new TestRunReport(0, 0, -1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new TestRunReport(0, 0, 0, -1, null));
    }

    @Test
    @DisplayName("the failure-summary list is unmodifiable and defensively copied")
    void failureSummariesAreUnmodifiable() {
        TestRunReport report = new TestRunReport(1, 0, 1, 10, Arrays.asList("a failure"));
        assertThrows(UnsupportedOperationException.class, () -> report.getFailureSummaries().add("tampered"));
    }

    @Test
    @DisplayName("toString uses the custom 'TestRunReport{...}' format")
    void shouldFormatCustomToString() {
        TestRunReport report = new TestRunReport(27, 27, 0, 412, null);
        assertTrue(report.toString().startsWith("TestRunReport{"));
        assertTrue(report.toString().contains("run=27"));
        assertTrue(report.toString().contains("allGreen=true"));
        assertFalse(report.toString().contains("@"));
    }

    @Test
    @DisplayName("two reports with the same field values are equal")
    void valueEquality() {
        TestRunReport a = new TestRunReport(5, 5, 0, 100, null);
        TestRunReport b = new TestRunReport(5, 5, 0, 100, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(new TestRunReport(5, 4, 1, 100, null)));
    }
}
