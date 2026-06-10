package edu.cqu.drs.server;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.client.ServerStubException;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.BoardSnapshot;
import edu.cqu.drs.protocol.Status;
import edu.cqu.drs.security.AuthService;
import edu.cqu.drs.security.PasswordHasher;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Server-side tests for the f1 live-board polling baseline: each
 * {@code GET_BOARD} poll returns a consistent, priority-ordered,
 * server-timestamped snapshot, another client's change is visible in the next
 * poll, and the board is dispatcher-gated. Real sockets on an ephemeral port,
 * in-memory store - runs unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Live board (f1) - GET_BOARD polling baseline")
class LiveBoardPollingSpec {

    private DrsServer server;

    @BeforeEach
    void startSecuredServer() throws IOException {
        InMemoryDataStore store = new InMemoryDataStore();
        AuthService authService = new AuthService(
                store.userDao(), new PasswordHasher(), store.auditDao());
        authService.register("dispatch1", "pw-dispatch", UserRole.DISPATCHER);
        authService.register("dispatch2", "pw-dispatch", UserRole.DISPATCHER);
        authService.register("cit1", "pw-citizen", UserRole.CITIZEN);
        IncidentService service = new IncidentService(store.incidentDao(),
                store.responderDao(), store.auditDao(), new AlertTemplateRecommender());
        this.server = new DrsServer(0, new DrsRequestDispatcher(service, authService));
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.server.stop();
    }

    /**
     * Opens a connected, logged-in stub.
     *
     * @param username the account.
     * @param password its password.
     * @return the logged-in stub.
     * @throws IOException if the connection fails.
     */
    private ServerStub loginAs(String username, String password) throws IOException {
        ServerStub stub = new ServerStub("localhost", this.server.getPort());
        stub.connect();
        stub.login(username, password);
        return stub;
    }

    @Test
    @DisplayName("an empty queue yields a valid snapshot with zero counts and a server timestamp")
    void shouldSnapshotEmptyBoard() throws IOException {
        try (ServerStub dispatcher = loginAs("dispatch1", "pw-dispatch")) {
            BoardSnapshot snapshot = dispatcher.getBoard();
            assertEquals(0, snapshot.getTotalCount());
            assertEquals(0, snapshot.getOpenCount());
            assertTrue(snapshot.getIncidents().isEmpty());
            assertNotNull(snapshot.getSnapshotAt());
        }
    }

    @Test
    @DisplayName("the snapshot rows are ordered most urgent first with consistent counts")
    void shouldOrderAndCount() throws IOException {
        try (ServerStub dispatcher = loginAs("dispatch1", "pw-dispatch")) {
            Incident minor = dispatcher.submitIncident(
                    HazardType.STORM, -23.0, 150.0, "Minor", 0);
            Incident major = dispatcher.submitIncident(
                    HazardType.FIRE, -23.0, 150.0, "Major", 9);
            dispatcher.triage(minor.getId(), Severity.LOW);
            dispatcher.triage(major.getId(), Severity.CRITICAL);
            dispatcher.resolve(minor.getId());

            BoardSnapshot snapshot = dispatcher.getBoard();
            assertEquals(2, snapshot.getTotalCount());
            assertEquals(1, snapshot.getOpenCount());
            assertEquals(major.getId(), snapshot.getIncidents().get(0).getId());
        }
    }

    @Test
    @DisplayName("another client's submission is visible in the next poll (multi-dispatcher view)")
    void shouldSeeOtherClientsChangeOnNextPoll() throws IOException {
        try (ServerStub watcher = loginAs("dispatch1", "pw-dispatch");
                ServerStub reporter = loginAs("dispatch2", "pw-dispatch")) {
            assertEquals(0, watcher.getBoard().getTotalCount());

            Incident filed = reporter.submitIncident(
                    HazardType.FLOOD, -23.0, 150.0, "From the other client", 2);

            BoardSnapshot next = watcher.getBoard();
            assertEquals(1, next.getTotalCount());
            assertEquals(filed.getId(), next.getIncidents().get(0).getId());
        }
    }

    @Test
    @DisplayName("an all-resolved board keeps total and open counts distinct from an empty board")
    void shouldCountAllResolvedBoard() throws IOException {
        try (ServerStub dispatcher = loginAs("dispatch1", "pw-dispatch")) {
            Incident first = dispatcher.submitIncident(
                    HazardType.FIRE, -23.0, 150.0, "First", 1);
            Incident second = dispatcher.submitIncident(
                    HazardType.STORM, -23.0, 150.0, "Second", 0);
            dispatcher.resolve(first.getId());
            dispatcher.resolve(second.getId());

            BoardSnapshot snapshot = dispatcher.getBoard();
            assertEquals(2, snapshot.getTotalCount());
            assertEquals(0, snapshot.getOpenCount());
        }
    }

    @Test
    @DisplayName("repeated polls carry non-decreasing server timestamps")
    void shouldStampWithServerClock() throws IOException {
        try (ServerStub dispatcher = loginAs("dispatch1", "pw-dispatch")) {
            BoardSnapshot first = dispatcher.getBoard();
            BoardSnapshot second = dispatcher.getBoard();
            assertTrue(!second.getSnapshotAt().isBefore(first.getSnapshotAt()),
                    "snapshot times must not go backwards");
        }
    }

    @Test
    @DisplayName("the board is dispatcher-gated: a CITIZEN gets UNAUTHORIZED")
    void shouldGateBoardToDispatchers() throws IOException {
        try (ServerStub citizen = loginAs("cit1", "pw-citizen")) {
            ServerStubException ex = assertThrows(
                    ServerStubException.class, citizen::getBoard);
            assertEquals(Status.UNAUTHORIZED, ex.getStatus());
        }
    }
}
