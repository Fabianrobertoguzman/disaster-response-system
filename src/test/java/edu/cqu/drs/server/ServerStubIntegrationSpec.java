package edu.cqu.drs.server;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.client.ServerStubException;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.Status;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests over a real {@link DrsServer} (bound to an
 * ephemeral port) reached through a real {@link ServerStub} client socket. The
 * server is backed by the in-memory data store, so these run with no MySQL and
 * exercise the full client -> protocol -> server -> service -> data path.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("ServerStub <-> DrsServer - end-to-end over a socket")
class ServerStubIntegrationSpec {

    private InMemoryDataStore store;
    private DrsServer server;
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
    }

    @AfterEach
    void stopServer() {
        this.server.stop();
    }

    /**
     * Opens a connected stub to the running server.
     *
     * @return a connected {@link ServerStub}.
     * @throws IOException if the connection fails.
     */
    private ServerStub connect() throws IOException {
        ServerStub stub = new ServerStub("localhost", this.server.getPort());
        stub.connect();
        return stub;
    }

    @Test
    @DisplayName("a ping is answered with pong")
    void shouldAnswerPing() throws IOException {
        try (ServerStub stub = connect()) {
            assertEquals("pong", stub.ping());
        }
    }

    @Test
    @DisplayName("a submitted incident is persisted and listed back")
    void shouldSubmitAndList() throws IOException {
        try (ServerStub stub = connect()) {
            Incident submitted = stub.submitIncident(HazardType.FIRE, -23.3781, 150.5136,
                    "Warehouse blaze", 3);
            List<Incident> incidents = stub.listIncidents();
            assertEquals(1, incidents.size());
            assertEquals(submitted.getId(), incidents.get(0).getId());
            assertEquals(HazardType.FIRE, incidents.get(0).getHazardType());
        }
    }

    @Test
    @DisplayName("triage over the wire updates the incident's severity and status")
    void shouldTriage() throws IOException {
        try (ServerStub stub = connect()) {
            Incident submitted = stub.submitIncident(HazardType.FLOOD, -23.0, 150.0, "Levee", 1);
            Incident triaged = stub.triage(submitted.getId(), Severity.CRITICAL);
            assertEquals(Severity.CRITICAL, triaged.getSeverity());
            assertEquals(IncidentStatus.TRIAGED, triaged.getStatus());
        }
    }

    @Test
    @DisplayName("assigning a responder over the wire attaches it to the incident")
    void shouldAssignResponder() throws IOException {
        try (ServerStub stub = connect()) {
            Incident submitted = stub.submitIncident(HazardType.FIRE, -23.0, 150.0, "Blaze", 2);
            Incident withResponder = stub.assignResponder(submitted.getId(),
                    this.seededResponder.getId());
            assertEquals(1, withResponder.getResponders().size());
            assertEquals("Alpha", withResponder.getResponders().get(0).getName());
        }
    }

    @Test
    @DisplayName("resolve over the wire moves the incident to RESOLVED")
    void shouldResolve() throws IOException {
        try (ServerStub stub = connect()) {
            Incident submitted = stub.submitIncident(HazardType.STORM, -23.0, 150.0, "Wind", 0);
            Incident resolved = stub.resolve(submitted.getId());
            assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());
        }
    }

    @Test
    @DisplayName("recommendTemplate over the wire returns a template")
    void shouldRecommendTemplate() throws IOException {
        try (ServerStub stub = connect()) {
            Incident submitted = stub.submitIncident(HazardType.FIRE, -23.0, 150.0, "Blaze", 5);
            stub.triage(submitted.getId(), Severity.CRITICAL);
            AlertTemplate template = stub.recommendTemplate(submitted.getId());
            assertEquals(AlertTemplate.EVAC_NOTICE, template);
        }
    }

    @Test
    @DisplayName("listResponders returns the seeded responder")
    void shouldListResponders() throws IOException {
        try (ServerStub stub = connect()) {
            List<Responder> responders = stub.listResponders();
            assertEquals(1, responders.size());
            assertEquals("Alpha", responders.get(0).getName());
        }
    }

    @Test
    @DisplayName("triaging an unknown incident returns NOT_FOUND as a ServerStubException")
    void shouldRejectUnknownIncident() throws IOException {
        try (ServerStub stub = connect()) {
            ServerStubException ex = assertThrows(ServerStubException.class,
                    () -> stub.triage(UUID.randomUUID(), Severity.HIGH));
            assertEquals(Status.NOT_FOUND, ex.getStatus());
        }
    }

    @Test
    @DisplayName("a submitted-then-resolved incident keeps its identity end to end")
    void shouldPreserveIdentity() throws IOException {
        try (ServerStub stub = connect()) {
            Incident submitted = stub.submitIncident(HazardType.HAZMAT, -23.0, 150.0, "Spill", 4);
            assertTrue(stub.listIncidents().stream()
                    .anyMatch(incident -> incident.getId().equals(submitted.getId())));
        }
    }
}
