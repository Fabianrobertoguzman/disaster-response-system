package edu.cqu.drs.server.service;

import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link IncidentService} over the in-memory data store - the
 * business tier in isolation, with no socket and no database. Pure and
 * deterministic, so they run unconditionally and pin the service's behaviour
 * (including the abnormal not-found path the dispatcher maps to NOT_FOUND).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("IncidentService - business tier over DAO interfaces")
class IncidentServiceSpec {

    private InMemoryDataStore store;
    private IncidentService service;
    private Responder responder;

    @BeforeEach
    void setUp() {
        this.store = new InMemoryDataStore();
        this.responder = new Responder("Alpha");
        this.store.responderDao().insert(this.responder);
        this.service = new IncidentService(this.store.incidentDao(),
                this.store.responderDao(), this.store.auditDao(), new AlertTemplateRecommender());
    }

    @Test
    @DisplayName("submitting an incident persists it and writes an audit entry")
    void shouldSubmitAndAudit() {
        Incident incident = this.service.submitIncident(HazardType.FIRE,
                new GpsCoordinate(-23.3781, 150.5136), "Blaze", 3, null);
        assertEquals(1, this.service.listIncidents().size());
        assertEquals(incident.getId(), this.service.listIncidents().get(0).getId());
        assertFalse(this.store.auditDao().findByIncident(incident.getId()).isEmpty());
    }

    @Test
    @DisplayName("listIncidents returns incidents most urgent first")
    void shouldOrderByUrgency() {
        Incident low = this.service.submitIncident(HazardType.STORM,
                new GpsCoordinate(-23.0, 150.0), "Minor", 0, null);
        Incident high = this.service.submitIncident(HazardType.FIRE,
                new GpsCoordinate(-23.0, 150.0), "Major", 10, null);
        this.service.triage(low.getId(), Severity.LOW, null);
        this.service.triage(high.getId(), Severity.CRITICAL, null);

        List<Incident> ordered = this.service.listIncidents();
        assertEquals(high.getId(), ordered.get(0).getId());
        assertEquals(low.getId(), ordered.get(1).getId());
    }

    @Test
    @DisplayName("triage sets severity and status")
    void shouldTriage() {
        Incident incident = this.service.submitIncident(HazardType.FLOOD,
                new GpsCoordinate(-23.0, 150.0), "Levee", 1, null);
        Incident triaged = this.service.triage(incident.getId(), Severity.HIGH, null);
        assertEquals(Severity.HIGH, triaged.getSeverity());
        assertEquals(IncidentStatus.TRIAGED, triaged.getStatus());
    }

    @Test
    @DisplayName("assigning a responder attaches it to the incident")
    void shouldAssignResponder() {
        Incident incident = this.service.submitIncident(HazardType.FIRE,
                new GpsCoordinate(-23.0, 150.0), "Blaze", 2, null);
        Incident withResponder = this.service.assignResponder(
                incident.getId(), this.responder.getId(), null);
        assertEquals(1, withResponder.getResponders().size());
        assertEquals(1, this.service.assignedResponders(incident.getId()).size());
    }

    @Test
    @DisplayName("resolve moves the incident to RESOLVED")
    void shouldResolve() {
        Incident incident = this.service.submitIncident(HazardType.HAZMAT,
                new GpsCoordinate(-23.0, 150.0), "Spill", 0, null);
        assertEquals(IncidentStatus.RESOLVED,
                this.service.resolve(incident.getId(), null).getStatus());
    }

    @Test
    @DisplayName("recommendTemplate returns and records a template")
    void shouldRecommendTemplate() {
        Incident incident = this.service.submitIncident(HazardType.FIRE,
                new GpsCoordinate(-23.0, 150.0), "Blaze", 5, null);
        this.service.triage(incident.getId(), Severity.CRITICAL, null);
        AlertTemplate template = this.service.recommendTemplate(incident.getId(), null);
        assertEquals(AlertTemplate.EVAC_NOTICE, template);
        assertEquals(AlertTemplate.EVAC_NOTICE,
                this.service.listIncidents().get(0).getRecommendedTemplate());
    }

    @Test
    @DisplayName("listResponders returns the seeded responder")
    void shouldListResponders() {
        assertEquals(1, this.service.listResponders().size());
        assertEquals("Alpha", this.service.listResponders().get(0).getName());
    }

    @Test
    @DisplayName("triaging an unknown incident throws NoSuchElementException")
    void shouldRejectUnknownIncident() {
        assertThrows(NoSuchElementException.class,
                () -> this.service.triage(UUID.randomUUID(), Severity.HIGH, null));
    }

    @Test
    @DisplayName("a submitted incident starts REPORTED with default severity")
    void shouldStartReported() {
        Incident incident = this.service.submitIncident(HazardType.EARTHQUAKE,
                new GpsCoordinate(-23.0, 150.0), "Tremor", 0, null);
        assertEquals(IncidentStatus.REPORTED, incident.getStatus());
        assertTrue(this.service.listIncidents().stream()
                .anyMatch(i -> i.getId().equals(incident.getId())));
    }
}
