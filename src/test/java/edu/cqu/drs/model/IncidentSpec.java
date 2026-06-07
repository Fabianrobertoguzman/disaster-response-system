package edu.cqu.drs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Incident} (the central domain entity).
 *
 * <p>BDD-style {@code *Spec} naming; every test is structured Arrange-Act-Assert with the
 * shared arrangement in {@link #setUp()}; coverage includes at least one normal and one abnormal case
 * for each behaviour (creation, validation, triage, responder allocation, resolution, custom toString,
 * audit logging, identity equality).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Incident - central domain entity")
class IncidentSpec {

    /** A valid GPS coordinate shared by the tests. */
    private GpsCoordinate location;

    /** A valid hazard type shared by the tests. */
    private HazardType hazard;

    /** Arrange: a valid location and hazard before each test. */
    @BeforeEach
    void setUp() {
        this.location = new GpsCoordinate(-23.3781, 150.5136);
        this.hazard = HazardType.FIRE;
    }

    @Test
    @DisplayName("a valid report creates a REPORTED incident with default MEDIUM severity, a non-null id, and no responders")
    void shouldCreateIncidentWithValidInputs() {
        Incident incident = new Incident(this.hazard, this.location, "Warehouse blaze", 3);
        assertAll(
                () -> assertNotNull(incident.getId()),
                () -> assertEquals(IncidentStatus.REPORTED, incident.getStatus()),
                () -> assertEquals(Severity.MEDIUM, incident.getSeverity()),
                () -> assertEquals(HazardType.FIRE, incident.getHazardType()),
                () -> assertEquals(3, incident.getVictimCount()),
                () -> assertEquals("Warehouse blaze", incident.getDescription()),
                () -> assertSame(this.location, incident.getGpsLocation()),
                () -> assertNotNull(incident.getReportedAt()),
                () -> assertTrue(incident.getResponders().isEmpty()),
                () -> assertNull(incident.getRecommendedTemplate())
        );
    }

    @Test
    @DisplayName("a null hazard type is rejected")
    void shouldRejectNullHazardType() {
        assertThrows(IllegalArgumentException.class, () -> new Incident(null, this.location, "x", 0));
    }

    @Test
    @DisplayName("a null GPS location is rejected")
    void shouldRejectNullGpsLocation() {
        assertThrows(IllegalArgumentException.class, () -> new Incident(this.hazard, null, "x", 0));
    }

    @Test
    @DisplayName("a negative victim count is rejected")
    void shouldRejectNegativeVictimCount() {
        assertThrows(IllegalArgumentException.class, () -> new Incident(this.hazard, this.location, "x", -1));
    }

    @Test
    @DisplayName("a null description is coerced to the empty string")
    void shouldCoerceNullDescriptionToEmpty() {
        assertEquals("", new Incident(this.hazard, this.location, null, 0).getDescription());
    }

    @Test
    @DisplayName("triage assigns the severity and moves the incident to TRIAGED")
    void shouldTriageToTriagedStatus() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        incident.triage(Severity.CRITICAL);
        assertEquals(Severity.CRITICAL, incident.getSeverity());
        assertEquals(IncidentStatus.TRIAGED, incident.getStatus());
    }

    @Test
    @DisplayName("triage with a null severity is rejected")
    void shouldRejectNullSeverityOnTriage() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        assertThrows(IllegalArgumentException.class, () -> incident.triage(null));
    }

    @Test
    @DisplayName("assigning a responder records it and sets that responder's current tasking")
    void shouldAssignResponderAndSetTasking() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        Responder responder = new Responder("Alex Tan");
        incident.assignResponder(responder);
        assertAll(
                () -> assertEquals(1, incident.getResponders().size()),
                () -> assertTrue(incident.getResponders().contains(responder)),
                () -> assertEquals(incident.getId(), responder.getCurrentTaskingId()),
                () -> assertFalse(responder.isAvailable())
        );
    }

    @Test
    @DisplayName("assigning the same responder twice is rejected")
    void shouldRejectDuplicateResponder() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        Responder responder = new Responder("Alex Tan");
        incident.assignResponder(responder);
        assertThrows(IllegalArgumentException.class, () -> incident.assignResponder(responder));
    }

    @Test
    @DisplayName("assigning more than MAX_RESPONDERS responders is rejected")
    void shouldRejectMoreThanMaxResponders() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        for (int i = 0; i < Incident.MAX_RESPONDERS; i++) {
            incident.assignResponder(new Responder("Responder-" + i));
        }
        assertThrows(IllegalStateException.class, () -> incident.assignResponder(new Responder("Overflow")));
    }

    @Test
    @DisplayName("a null responder is rejected on assignment")
    void shouldRejectNullResponder() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        assertThrows(IllegalArgumentException.class, () -> incident.assignResponder(null));
    }

    @Test
    @DisplayName("resolve moves the incident to RESOLVED")
    void shouldResolveToResolvedStatus() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0);
        incident.resolve();
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    @DisplayName("toString uses the custom 'Incident{...}' format, not the IDE-generated Class@hash form")
    void shouldFormatCustomToString() {
        Incident incident = new Incident(HazardType.FIRE, this.location, "x", 2);
        incident.triage(Severity.HIGH);
        String text = incident.toString();
        assertAll(
                () -> assertTrue(text.startsWith("Incident{"), "should start with 'Incident{'"),
                () -> assertTrue(text.contains("hazard=FIRE")),
                () -> assertTrue(text.contains("severity=HIGH")),
                () -> assertTrue(text.contains("status=TRIAGED")),
                () -> assertTrue(text.contains("victims=2")),
                () -> assertFalse(text.contains("@"), "should not look like the IDE-generated Class@hash form")
        );
    }

    @Test
    @DisplayName("significant actions (reported, triaged, responder allocated, resolved) are written to the audit log")
    void shouldLogActionsToAuditLog() {
        Incident incident = new Incident(this.hazard, this.location, "x", 0); // 1: reported
        incident.triage(Severity.HIGH);                                       // 2: triaged
        incident.assignResponder(new Responder("Alex Tan"));                  // 3: responder allocated
        incident.resolve();                                                   // 4: resolved
        assertEquals(4, incident.getAuditLog().size());
    }

    @Test
    @DisplayName("two incidents with different ids are not equal; an incident equals itself")
    void identityEquality() {
        Incident a = new Incident(this.hazard, this.location, "x", 0);
        Incident b = new Incident(this.hazard, this.location, "x", 0);
        assertNotEquals(a, b);
        assertEquals(a, a);
    }
}
