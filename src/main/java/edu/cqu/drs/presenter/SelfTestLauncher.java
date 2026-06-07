package edu.cqu.drs.presenter;

import edu.cqu.drs.model.AuditLog;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentQueue;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.model.TestRunReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs an in-process suite of smoke checks against the domain model from
 * inside the GUI and reports the result (creative feature FR-CR-02).
 *
 * <p>"Run Self-Tests" on the Tools menu calls {@link #runAllTests()}, which
 * exercises the model's key invariants - a fresh incident starts in
 * {@code REPORTED}; the dispatch queue surfaces the most urgent incident
 * first; triage moves an incident to {@code TRIAGED}; an allocated responder
 * is no longer available; the audit log is append-only - and returns a
 * {@link TestRunReport} the GUI displays. This makes the "tests as
 * system documentation" benefit of test-driven development concrete: the named checks
 * below are a runnable, GUI-visible specification of how the model behaves.
 * The full JUnit suite proper is run by {@code mvn test}; running that suite
 * from the running app would need the test classes on the app's runtime
 * classpath, which is out of scope for the prototype.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class SelfTestLauncher {

    /**
     * A single named self-check: an assertion to run that throws if the
     * invariant under test does not hold.
     */
    @FunctionalInterface
    private interface Check {

        /**
         * Runs the check.
         *
         * @throws AssertionError if the invariant does not hold.
         */
        void run();
    }

    /** Creates a self-test launcher (it has no mutable state). */
    public SelfTestLauncher() {
        // No state to initialise.
    }

    /**
     * Diagnostic summary of how many self-checks the launcher carries.
     *
     * @return e.g. {@code "SelfTestLauncher{checks=9}"}.
     */
    @Override
    public String toString() {
        return "SelfTestLauncher{checks=" + buildChecks().size() + "}";
    }

    /**
     * Runs every self-check and reports the outcome.
     *
     * @return a {@link TestRunReport} summarising the run (never null).
     */
    public TestRunReport runAllTests() {
        Map<String, Check> checks = buildChecks();
        long startedAt = System.currentTimeMillis();
        long passed = 0;
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, Check> entry : checks.entrySet()) {
            try {
                entry.getValue().run();
                passed++;
            } catch (RuntimeException | AssertionError ex) {
                failures.add(entry.getKey() + " - " + ex.getMessage());
            }
        }
        long durationMs = System.currentTimeMillis() - startedAt;
        return new TestRunReport(checks.size(), passed, failures.size(),
                durationMs, failures);
    }

    /**
     * Builds the ordered map of self-checks (description to assertion).
     *
     * @return the self-checks, in display order.
     */
    private static Map<String, Check> buildChecks() {
        Map<String, Check> checks = new LinkedHashMap<>();
        checks.put("Incident starts in REPORTED status", () -> {
            Incident incident = sampleIncident();
            require(incident.getStatus() == IncidentStatus.REPORTED,
                    "status was " + incident.getStatus());
        });
        checks.put("Incident defaults to MEDIUM severity", () ->
                require(sampleIncident().getSeverity() == Severity.MEDIUM,
                        "default severity was not MEDIUM"));
        checks.put("Triage sets the severity and TRIAGED status", () -> {
            Incident incident = sampleIncident();
            incident.triage(Severity.HIGH);
            require(incident.getStatus() == IncidentStatus.TRIAGED
                    && incident.getSeverity() == Severity.HIGH,
                    "post-triage state was wrong");
        });
        checks.put("Resolve sets RESOLVED status", () -> {
            Incident incident = sampleIncident();
            incident.resolve();
            require(incident.getStatus() == IncidentStatus.RESOLVED,
                    "status was " + incident.getStatus());
        });
        checks.put("Queue surfaces the most urgent incident first", () -> {
            IncidentQueue queue = new IncidentQueue();
            Incident routine = sampleIncident();
            Incident urgent = sampleIncident();
            urgent.triage(Severity.CRITICAL);
            queue.enqueue(routine);
            queue.enqueue(urgent);
            require(queue.peek() == urgent,
                    "peek did not return the CRITICAL incident");
        });
        checks.put("Allocated responder becomes unavailable", () -> {
            Incident incident = sampleIncident();
            Responder responder = new Responder("Self-Test Unit");
            incident.assignResponder(responder);
            require(!responder.isAvailable()
                    && incident.getResponders().size() == 1,
                    "responder/incident state wrong after allocation");
        });
        checks.put("Duplicate responder allocation is rejected", () -> {
            Incident incident = sampleIncident();
            Responder responder = new Responder("Self-Test Unit");
            incident.assignResponder(responder);
            requireRejected(() -> incident.assignResponder(responder),
                    "duplicate responder allocation");
        });
        checks.put("Audit log is append-only and rejects blank entries", () -> {
            AuditLog log = new AuditLog();
            log.record("self-test entry");
            require(log.size() == 1, "entry was not appended");
            requireRejected(() -> log.record("   "), "a blank audit entry");
        });
        checks.put("Negative victim count is rejected", () ->
                requireRejected(() -> new Incident(HazardType.FIRE,
                        GpsCoordinate.captureCurrentLocation(), "x", -1),
                        "a negative victim count"));
        return checks;
    }

    /**
     * Creates a fresh sample incident for a self-check.
     *
     * @return a new {@link Incident} (FIRE, default severity).
     */
    private static Incident sampleIncident() {
        return new Incident(HazardType.FIRE,
                GpsCoordinate.captureCurrentLocation(),
                "self-test incident", 1);
    }

    /**
     * Asserts a condition, throwing if it does not hold.
     *
     * @param condition the condition that must be true.
     * @param message   the failure message if it is not.
     * @throws AssertionError if {@code condition} is false.
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Asserts that running an action throws {@link IllegalArgumentException}.
     *
     * @param action      the action expected to be rejected.
     * @param description what the action represents (for the failure message).
     * @throws AssertionError if the action does not throw.
     */
    private static void requireRejected(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(description + " was not rejected");
    }
}
