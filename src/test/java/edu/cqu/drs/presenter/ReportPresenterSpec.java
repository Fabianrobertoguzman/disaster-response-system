package edu.cqu.drs.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioural specification for {@link ReportPresenter} - the business-tier
 * step that files a citizen report onto the shared incident queue.
 */
class ReportPresenterSpec {

    private IncidentQueue queue;
    private ReportPresenter presenter;
    private GpsCoordinate location;

    @BeforeEach
    void setUp() {
        this.queue = new IncidentQueue();
        this.presenter = new ReportPresenter(this.queue);
        this.location = GpsCoordinate.captureCurrentLocation();
    }

    @Test
    @DisplayName("submitIncident files an incident and enqueues it for triage")
    void shouldFileAndEnqueueIncident() {
        // Arrange: an empty queue (from setUp).
        // Act
        Incident filed = this.presenter.submitIncident(
                HazardType.FIRE, this.location, "Grass fire near the highway", 3);
        // Assert
        assertNotNull(filed);
        assertEquals(HazardType.FIRE, filed.getHazardType());
        assertEquals(1, this.presenter.queueSize());
        assertEquals(1, this.queue.size());
    }

    @Test
    @DisplayName("queueSize reflects every filed report; IDs are distinct")
    void shouldReportQueueSizeAfterMultipleSubmits() {
        // Arrange / Act
        Incident first = this.presenter.submitIncident(
                HazardType.FLOOD, this.location, "Rising water on Main St", 0);
        Incident second = this.presenter.submitIncident(
                HazardType.STORM, this.location, "Roof torn off", 2);
        Incident third = this.presenter.submitIncident(
                HazardType.HAZMAT, this.location, "Tanker leak", 5);
        // Assert
        assertEquals(3, this.presenter.queueSize());
        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(second.getId(), third.getId());
        assertNotEquals(first.getId(), third.getId());
    }

    @Test
    @DisplayName("constructor rejects a null incident queue")
    void shouldRejectNullQueueInConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReportPresenter(null));
    }

    @Test
    @DisplayName("a model rejection propagates and leaves the queue untouched")
    void shouldPropagateModelRejectionAndLeaveQueueUntouched() {
        // Act + Assert: the model rejects a null hazard type before any enqueue.
        assertThrows(IllegalArgumentException.class, () -> this.presenter
                .submitIncident(null, this.location, "no type given", 0));
        // Assert: nothing was enqueued.
        assertTrue(this.queue.isEmpty());
        assertEquals(0, this.presenter.queueSize());
    }
}
