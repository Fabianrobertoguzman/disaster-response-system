package edu.cqu.drs.client;

import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.Status;
import edu.cqu.drs.server.DrsRequestDispatcher;
import edu.cqu.drs.server.DrsServer;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for {@link DispatchClientPresenter} over a real socket: the
 * full dispatcher action set (list, triage, allocate, resolve, recommend)
 * performed from the client tier against a {@link DrsServer} backed by the
 * in-memory store. No MySQL, loopback only, so they run unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("DispatchClientPresenter - dispatcher console over the wire")
class DispatchClientPresenterSpec {

    private InMemoryDataStore store;
    private DrsServer server;
    private ServerStub stub;
    private DispatchClientPresenter presenter;
    private Responder seededResponder;

    @BeforeEach
    void startServer() throws IOException {
        this.store = new InMemoryDataStore();
        this.seededResponder = new Responder("Alpha");
        this.store.responderDao().insert(this.seededResponder);
        IncidentService service = new IncidentService(this.store.incidentDao(),
                this.store.responderDao(), this.store.auditDao(), new AlertTemplateRecommender());
        this.server = new DrsServer(0, new DrsRequestDispatcher(service));
        this.server.start();
        this.stub = new ServerStub("localhost", this.server.getPort());
        this.stub.connect();
        this.presenter = new DispatchClientPresenter(this.stub);
    }

    @AfterEach
    void stopServer() {
        this.stub.close();
        this.server.stop();
    }

    /**
     * Files an incident directly through the stub (the citizen path) so the
     * dispatcher tests have something to act on.
     *
     * @param hazard  the hazard type.
     * @param victims the victim count.
     * @return the persisted incident.
     */
    private Incident submit(HazardType hazard, int victims) {
        return this.stub.submitIncident(hazard, -23.3781, 150.5136, "test", victims);
    }

    @Test
    @DisplayName("pendingIncidents returns the server's queue most urgent first")
    void shouldListMostUrgentFirst() {
        Incident minor = submit(HazardType.STORM, 0);
        Incident major = submit(HazardType.FIRE, 10);
        this.presenter.triage(minor.getId(), Severity.LOW);
        this.presenter.triage(major.getId(), Severity.CRITICAL);

        List<Incident> ordered = this.presenter.pendingIncidents();
        assertEquals(2, ordered.size());
        assertEquals(major.getId(), ordered.get(0).getId());
        assertEquals(minor.getId(), ordered.get(1).getId());
        assertEquals(2, this.presenter.pendingCount());
    }

    @Test
    @DisplayName("triage updates severity and status on the server")
    void shouldTriage() {
        Incident incident = submit(HazardType.FLOOD, 1);
        Incident triaged = this.presenter.triage(incident.getId(), Severity.HIGH);
        assertEquals(Severity.HIGH, triaged.getSeverity());
        assertEquals(IncidentStatus.TRIAGED, triaged.getStatus());
    }

    @Test
    @DisplayName("assignResponder attaches the responder on the server")
    void shouldAssignResponder() {
        Incident incident = submit(HazardType.FIRE, 2);
        Incident updated = this.presenter.assignResponder(
                incident.getId(), this.seededResponder.getId());
        assertEquals(1, updated.getResponders().size());
        assertEquals("Alpha", updated.getResponders().get(0).getName());
    }

    @Test
    @DisplayName("resolve moves the incident to RESOLVED on the server")
    void shouldResolve() {
        Incident incident = submit(HazardType.HAZMAT, 0);
        assertEquals(IncidentStatus.RESOLVED,
                this.presenter.resolve(incident.getId()).getStatus());
    }

    @Test
    @DisplayName("recommendTemplate returns the rule-driven template")
    void shouldRecommendTemplate() {
        Incident incident = submit(HazardType.FIRE, 5);
        this.presenter.triage(incident.getId(), Severity.CRITICAL);
        assertEquals(AlertTemplate.EVAC_NOTICE,
                this.presenter.recommendTemplate(incident.getId()));
    }

    @Test
    @DisplayName("listResponders returns the server roster")
    void shouldListResponders() {
        List<Responder> responders = this.presenter.listResponders();
        assertEquals(1, responders.size());
        assertEquals("Alpha", responders.get(0).getName());
    }

    @Test
    @DisplayName("acting on an unknown incident surfaces NOT_FOUND")
    void shouldSurfaceNotFound() {
        ServerStubException ex = assertThrows(ServerStubException.class,
                () -> this.presenter.triage(UUID.randomUUID(), Severity.HIGH));
        assertEquals(Status.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("null arguments are rejected before reaching the wire")
    void shouldRejectNullArguments() {
        Incident incident = submit(HazardType.FIRE, 1);
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.triage(null, Severity.HIGH));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.triage(incident.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.assignResponder(null, this.seededResponder.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.resolve(null));
        assertThrows(IllegalArgumentException.class,
                () -> this.presenter.recommendTemplate(null));
    }
}
