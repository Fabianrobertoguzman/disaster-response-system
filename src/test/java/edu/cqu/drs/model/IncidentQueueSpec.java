package edu.cqu.drs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link IncidentQueue} (the priority-ordered incident queue).
 *
 * <p>Verifies the prioritisation rule (severity priority first, victim count as tiebreaker), the
 * partition coverage of {@link Severity} via a parameterised test, and the boundary/abnormal cases
 * (null incident, empty queue, non-destructive snapshot).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("IncidentQueue - priority-ordered incident queue")
class IncidentQueueSpec {

    /** A fresh queue before each test. */
    private IncidentQueue queue;

    /** A valid GPS coordinate shared by the tests. */
    private GpsCoordinate location;

    /** Arrange: an empty queue and a valid location. */
    @BeforeEach
    void setUp() {
        this.queue = new IncidentQueue();
        this.location = new GpsCoordinate(-23.3781, 150.5136);
    }

    /**
     * Helper: builds a triaged incident with the given severity and victim count.
     *
     * @param severity the severity to triage to.
     * @param victims  the victim count.
     * @return a TRIAGED {@link Incident}.
     */
    private Incident triaged(Severity severity, int victims) {
        Incident incident = new Incident(HazardType.FIRE, this.location, "x", victims);
        incident.triage(severity);
        return incident;
    }

    @Test
    @DisplayName("dequeue returns the highest-severity incident first, then the next, then the lowest")
    void shouldReturnHighestSeverityFirst() {
        Incident low = triaged(Severity.LOW, 1);
        Incident critical = triaged(Severity.CRITICAL, 1);
        Incident high = triaged(Severity.HIGH, 1);
        this.queue.enqueue(low);
        this.queue.enqueue(critical);
        this.queue.enqueue(high);
        assertSame(critical, this.queue.dequeue());
        assertSame(high, this.queue.dequeue());
        assertSame(low, this.queue.dequeue());
    }

    @Test
    @DisplayName("ties on severity are broken by the higher victim count")
    void shouldBreakTiesByVictimCount() {
        Incident fewVictims = triaged(Severity.HIGH, 2);
        Incident manyVictims = triaged(Severity.HIGH, 9);
        this.queue.enqueue(fewVictims);
        this.queue.enqueue(manyVictims);
        assertSame(manyVictims, this.queue.dequeue());
        assertSame(fewVictims, this.queue.dequeue());
    }

    @ParameterizedTest
    @EnumSource(Severity.class)
    @DisplayName("every Severity value has a positive priority (equivalence-partition coverage)")
    void everySeverityHasPositivePriority(Severity severity) {
        assertTrue(severity.getPriority() > 0);
    }

    @Test
    @DisplayName("enqueue rejects a null incident")
    void shouldRejectNullIncident() {
        assertThrows(IllegalArgumentException.class, () -> this.queue.enqueue(null));
    }

    @Test
    @DisplayName("dequeue and peek return null on an empty queue; the queue reports empty / size 0")
    void shouldReturnNullWhenEmpty() {
        assertNull(this.queue.dequeue());
        assertNull(this.queue.peek());
        assertTrue(this.queue.isEmpty());
        assertEquals(0, this.queue.size());
    }

    @Test
    @DisplayName("asSortedList returns the incidents most-urgent-first without removing them")
    void asSortedListIsMostUrgentFirstAndNonDestructive() {
        Incident low = triaged(Severity.LOW, 1);
        Incident critical = triaged(Severity.CRITICAL, 1);
        this.queue.enqueue(low);
        this.queue.enqueue(critical);
        List<Incident> sorted = this.queue.asSortedList();
        assertEquals(2, sorted.size());
        assertSame(critical, sorted.get(0));
        assertSame(low, sorted.get(1));
        assertEquals(2, this.queue.size());
    }

    @Test
    @DisplayName("contains reports membership; remove takes an incident out of the queue")
    void containsAndRemove() {
        Incident incident = triaged(Severity.HIGH, 1);
        Incident other = triaged(Severity.LOW, 1);
        this.queue.enqueue(incident);
        assertTrue(this.queue.contains(incident));
        assertFalse(this.queue.contains(other));
        assertTrue(this.queue.remove(incident));
        assertFalse(this.queue.contains(incident));
        assertEquals(0, this.queue.size());
        assertFalse(this.queue.remove(incident), "removing an absent incident returns false");
    }
}
