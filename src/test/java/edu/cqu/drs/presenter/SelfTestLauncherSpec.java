package edu.cqu.drs.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cqu.drs.model.TestRunReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioural specification for {@link SelfTestLauncher} - the in-GUI self-test
 * suite (creative feature FR-CR-02).
 */
class SelfTestLauncherSpec {

    @Test
    @DisplayName("runAllTests reports every self-check as passing")
    void shouldRunAllSelfChecksGreen() {
        TestRunReport report = new SelfTestLauncher().runAllTests();
        assertNotNull(report);
        assertTrue(report.getTestsRun() > 0);
        assertEquals(report.getTestsRun(), report.getPassed());
        assertEquals(0L, report.getFailed());
        assertTrue(report.isAllGreen());
        assertTrue(report.getFailureSummaries().isEmpty());
    }
}
