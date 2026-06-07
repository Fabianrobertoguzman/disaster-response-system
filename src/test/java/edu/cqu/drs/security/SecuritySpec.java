package edu.cqu.drs.security;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.client.ServerStubException;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.model.UserRole;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end security tests over a secured {@link DrsServer} reached through a
 * real {@link ServerStub}: authentication is required, roles are enforced, bad
 * credentials are rejected, and the incident description is encrypted at rest.
 * In-memory backed, so no MySQL is needed.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Security - authenticated, role-gated, encrypted server")
class SecuritySpec {

    private InMemoryDataStore store;
    private DrsServer server;

    @BeforeEach
    void startSecuredServer() throws IOException {
        this.store = new InMemoryDataStore();
        AuthService authService = new AuthService(
                this.store.userDao(), new PasswordHasher(), this.store.auditDao());
        authService.register("dispatch1", "pw-dispatch", UserRole.DISPATCHER);
        authService.register("cit1", "pw-citizen", UserRole.CITIZEN);
        IncidentService incidentService = new IncidentService(this.store.incidentDao(),
                this.store.responderDao(), this.store.auditDao(),
                new AlertTemplateRecommender(), FieldCipher.withGeneratedKey());
        this.server = new DrsServer(0, new DrsRequestDispatcher(incidentService, authService));
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.server.stop();
    }

    /**
     * @return a connected (but not yet logged-in) stub.
     * @throws IOException if the connection fails.
     */
    private ServerStub connect() throws IOException {
        ServerStub stub = new ServerStub("localhost", this.server.getPort());
        stub.connect();
        return stub;
    }

    @Test
    @DisplayName("a protected action without a session is rejected as UNAUTHORIZED")
    void shouldRejectUnauthenticated() throws IOException {
        try (ServerStub stub = connect()) {
            ServerStubException ex = assertThrows(ServerStubException.class, stub::listIncidents);
            assertEquals(Status.UNAUTHORIZED, ex.getStatus());
        }
    }

    @Test
    @DisplayName("a wrong password is rejected as UNAUTHORIZED")
    void shouldRejectWrongPassword() throws IOException {
        try (ServerStub stub = connect()) {
            ServerStubException ex = assertThrows(ServerStubException.class,
                    () -> stub.login("dispatch1", "wrong"));
            assertEquals(Status.UNAUTHORIZED, ex.getStatus());
        }
    }

    @Test
    @DisplayName("a dispatcher can log in and list incidents")
    void shouldAllowDispatcher() throws IOException {
        try (ServerStub stub = connect()) {
            stub.login("dispatch1", "pw-dispatch");
            assertTrue(stub.listIncidents().isEmpty());
        }
    }

    @Test
    @DisplayName("a citizen may submit but may not triage (role enforcement)")
    void shouldEnforceCitizenRole() throws IOException {
        try (ServerStub stub = connect()) {
            stub.login("cit1", "pw-citizen");
            Incident incident = stub.submitIncident(HazardType.FIRE, -23.0, 150.0, "Blaze", 1);
            ServerStubException ex = assertThrows(ServerStubException.class,
                    () -> stub.triage(incident.getId(), Severity.HIGH));
            assertEquals(Status.UNAUTHORIZED, ex.getStatus());
        }
    }

    @Test
    @DisplayName("the incident description is encrypted at rest but served in clear text")
    void shouldEncryptDescriptionAtRest() throws IOException {
        Incident submitted;
        try (ServerStub citizen = connect()) {
            citizen.login("cit1", "pw-citizen");
            submitted = citizen.submitIncident(HazardType.FLOOD, -23.0, 150.0, "secret-desc", 0);
            assertEquals("secret-desc", submitted.getDescription());
        }
        // Stored form is ciphertext, not the plaintext the client sent.
        String stored = this.store.incidentDao().findByUuid(submitted.getId())
                .orElseThrow().getDescription();
        assertNotEquals("secret-desc", stored);
        // Served form (to an authorised dispatcher) is the decrypted plaintext.
        try (ServerStub dispatcher = connect()) {
            dispatcher.login("dispatch1", "pw-dispatch");
            String served = dispatcher.listIncidents().stream()
                    .filter(incident -> incident.getId().equals(submitted.getId()))
                    .findFirst().orElseThrow().getDescription();
            assertEquals("secret-desc", served);
        }
    }

    @Test
    @DisplayName("after logout, protected actions are rejected again")
    void shouldRejectAfterLogout() throws IOException {
        try (ServerStub stub = connect()) {
            stub.login("dispatch1", "pw-dispatch");
            assertTrue(stub.listIncidents().isEmpty());
            stub.logout();
            ServerStubException ex = assertThrows(ServerStubException.class, stub::listIncidents);
            assertEquals(Status.UNAUTHORIZED, ex.getStatus());
        }
    }
}
