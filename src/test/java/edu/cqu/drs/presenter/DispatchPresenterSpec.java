package edu.cqu.drs.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentQueue;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioural specification for {@link DispatchPresenter} - the dispatcher's
 * triage, responder-allocation and resolution actions over the shared incident
 * queue.
 */
class DispatchPresenterSpec {

    private IncidentQueue queue;
    private DispatchPresenter presenter;
    private GpsCoordinate location;

    @BeforeEach
    void setUp() {
        this.queue = new IncidentQueue();
        this.presenter = new DispatchPresenter(this.queue);
        this.location = GpsCoordinate.captureCurrentLocation();
    }

    private Incident enqueue(HazardType hazard, String description,
            int victims) {
        Incident incident =
                new Incident(hazard, this.location, description, victims);
        this.queue.enqueue(incident);
        return incident;
    }

    @Test
    @DisplayName("pendingIncidents lists the queue most-urgent-first")
    void shouldListPendingIncidentsMostUrgentFirst() {
        // Arrange
        Incident low = enqueue(HazardType.FLOOD, "minor flooding", 0);
        Incident high = enqueue(HazardType.FIRE, "structure fire", 4);
        this.presenter.triage(low, Severity.LOW);
        this.presenter.triage(high, Severity.HIGH);
        // Act
        List<Incident> pending = this.presenter.pendingIncidents();
        // Assert
        assertEquals(2, pending.size());
        assertSame(high, pending.get(0));
        assertSame(low, pending.get(1));
    }

    @Test
    @DisplayName("triage re-positions an incident in the queue")
    void shouldRePositionIncidentAfterTriage() {
        // Arrange: A low, B high.
        Incident a = enqueue(HazardType.STORM, "wind damage", 1);
        Incident b = enqueue(HazardType.HAZMAT, "chemical spill", 2);
        this.presenter.triage(a, Severity.LOW);
        this.presenter.triage(b, Severity.HIGH);
        assertSame(b, this.presenter.pendingIncidents().get(0));
        // Act: lift A to CRITICAL.
        this.presenter.triage(a, Severity.CRITICAL);
        // Assert: A is now first; the queue size is unchanged.
        assertSame(a, this.presenter.pendingIncidents().get(0));
        assertEquals(2, this.presenter.pendingCount());
    }

    @Test
    @DisplayName("triage moves the incident to TRIAGED")
    void shouldMarkIncidentTriaged() {
        Incident incident = enqueue(HazardType.FIRE, "grass fire", 0);
        this.presenter.triage(incident, Severity.MEDIUM);
        assertEquals(IncidentStatus.TRIAGED, incident.getStatus());
        assertEquals(Severity.MEDIUM, incident.getSeverity());
    }

    @Test
    @DisplayName("assignResponder allocates a responder and marks it tasked")
    void shouldAssignResponderToIncident() {
        Incident incident = enqueue(HazardType.FIRE, "warehouse fire", 4);
        Responder responder = new Responder("Unit Test");
        this.presenter.assignResponder(incident, responder);
        assertEquals(1, incident.getResponders().size());
        assertFalse(responder.isAvailable());
    }

    @Test
    @DisplayName("assignResponder rejects null arguments")
    void shouldRejectNullInAssignResponder() {
        Incident incident = enqueue(HazardType.FIRE, "x", 0);
        Responder responder = new Responder("Unit X");
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.assignResponder(null, responder));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.assignResponder(incident, null));
    }

    @Test
    @DisplayName("resolve marks the incident RESOLVED and keeps it in queue")
    void shouldMarkIncidentResolved() {
        Incident incident = enqueue(HazardType.EARTHQUAKE, "aftershock", 0);
        this.presenter.resolve(incident);
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals(1, this.presenter.pendingCount());
    }

    @Test
    @DisplayName("triage completes well within the response-time SLO budget")
    void shouldTriageWithinSloBudget() {
        Incident incident = enqueue(HazardType.FIRE, "warehouse fire", 6);
        assertTimeout(
                Duration.ofMillis(DispatchPresenter.EXPECTED_LATENCY_MS),
                () -> this.presenter.triage(incident, Severity.CRITICAL));
    }

    @Test
    @DisplayName("pendingCount counts the incidents in the queue")
    void shouldCountPendingIncidents() {
        enqueue(HazardType.FIRE, "one", 0);
        enqueue(HazardType.FLOOD, "two", 0);
        enqueue(HazardType.STORM, "three", 0);
        assertEquals(3, this.presenter.pendingCount());
    }

    @Test
    @DisplayName("constructor rejects a null incident queue")
    void shouldRejectNullQueueInConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new DispatchPresenter(null));
    }

    @Test
    @DisplayName("triage and resolve reject null arguments")
    void shouldRejectNullArguments() {
        Incident incident = enqueue(HazardType.FIRE, "x", 0);
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.triage(null, Severity.HIGH));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.triage(incident, null));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.resolve(null));
    }
}
