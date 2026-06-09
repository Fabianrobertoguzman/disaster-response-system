package edu.cqu.drs.client;

import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.Status;
import edu.cqu.drs.security.AuthService;
import edu.cqu.drs.security.PasswordHasher;
import edu.cqu.drs.server.DrsRequestDispatcher;
import edu.cqu.drs.server.DrsServer;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the role-authorisation contract the client UI is built against: which
 * operations each role may perform through the client presenters on a
 * <em>secured</em> server. The role-adaptive menu shows the dispatcher console
 * only to DISPATCHER/ADMINISTRATOR because these are exactly the roles the
 * server admits to those actions - this spec keeps the two in lock-step.
 * In-memory backed, loopback only; runs unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Client role authorisation - the contract behind the role-adaptive UI")
class ClientRoleAuthorizationSpec {

    private DrsServer server;
    private Responder seededResponder;

    @BeforeEach
    void startSecuredServer() throws IOException {
        InMemoryDataStore store = new InMemoryDataStore();
        this.seededResponder = new Responder("Alpha");
        store.responderDao().insert(this.seededResponder);
        AuthService authService = new AuthService(
                store.userDao(), new PasswordHasher(), store.auditDao());
        authService.register("cit1", "pw-citizen", UserRole.CITIZEN);
        authService.register("dispatch1", "pw-dispatch", UserRole.DISPATCHER);
        authService.register("admin1", "pw-admin", UserRole.ADMINISTRATOR);
        IncidentService incidentService = new IncidentService(store.incidentDao(),
                store.responderDao(), store.auditDao(), new AlertTemplateRecommender());
        this.server = new DrsServer(0,
                new DrsRequestDispatcher(incidentService, authService));
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.server.stop();
    }

    /**
     * Opens a connected stub and logs in.
     *
     * @param username the account to authenticate.
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
    @DisplayName("a CITIZEN can submit a report but cannot reach any dispatch operation")
    void citizenSubmitsButCannotDispatch() throws IOException {
        try (ServerStub stub = loginAs("cit1", "pw-citizen")) {
            ReportClientPresenter report = new ReportClientPresenter(stub);
            Incident incident = report.submitIncident(HazardType.FIRE,
                    new GpsCoordinate(-23.0, 150.0), "Blaze", 1);

            DispatchClientPresenter dispatch = new DispatchClientPresenter(stub);
            assertUnauthorized(() -> dispatch.pendingIncidents());
            assertUnauthorized(() -> dispatch.triage(incident.getId(), Severity.HIGH));
            assertUnauthorized(() -> dispatch.assignResponder(
                    incident.getId(), incident.getId()));
            assertUnauthorized(() -> dispatch.resolve(incident.getId()));
            assertUnauthorized(() -> dispatch.recommendTemplate(incident.getId()));
            assertUnauthorized(() -> dispatch.listResponders());
        }
    }

    @Test
    @DisplayName("a DISPATCHER can run the full dispatch action set")
    void dispatcherRunsDispatchActions() throws IOException {
        Incident incident;
        try (ServerStub citizen = loginAs("cit1", "pw-citizen")) {
            incident = new ReportClientPresenter(citizen).submitIncident(
                    HazardType.FLOOD, new GpsCoordinate(-23.0, 150.0), "Levee", 2);
        }
        try (ServerStub stub = loginAs("dispatch1", "pw-dispatch")) {
            DispatchClientPresenter dispatch = new DispatchClientPresenter(stub);
            assertEquals(1, dispatch.pendingCount());
            assertEquals(Severity.CRITICAL,
                    dispatch.triage(incident.getId(), Severity.CRITICAL).getSeverity());
            assertEquals(1, dispatch.listResponders().size());
            assertEquals(1, dispatch.assignResponder(incident.getId(),
                    this.seededResponder.getId()).getResponders().size());
            assertEquals(edu.cqu.drs.model.AlertTemplate.EVAC_NOTICE,
                    dispatch.recommendTemplate(incident.getId()));
            assertEquals(IncidentStatus.RESOLVED,
                    dispatch.resolve(incident.getId()).getStatus());
        }
    }

    @Test
    @DisplayName("PING is open: it answers before any login on the secured server")
    void pingIsOpen() throws IOException {
        ServerStub stub = new ServerStub("localhost", this.server.getPort());
        stub.connect();
        try {
            assertEquals("pong", stub.ping());
        } finally {
            stub.close();
        }
    }

    @Test
    @DisplayName("LOGOUT works for any authenticated session and ends it")
    void logoutWorksForAnySession() throws IOException {
        try (ServerStub stub = loginAs("cit1", "pw-citizen")) {
            ReportClientPresenter report = new ReportClientPresenter(stub);
            report.submitIncident(HazardType.FIRE,
                    new GpsCoordinate(-23.0, 150.0), "Blaze", 0);
            stub.logout();
            assertUnauthorized(() -> report.submitIncident(HazardType.FIRE,
                    new GpsCoordinate(-23.0, 150.0), "After logout", 0));
        }
    }

    @Test
    @DisplayName("an ADMINISTRATOR holds the dispatcher action set too")
    void administratorRunsDispatchActions() throws IOException {
        try (ServerStub stub = loginAs("admin1", "pw-admin")) {
            DispatchClientPresenter dispatch = new DispatchClientPresenter(stub);
            assertTrue(dispatch.pendingIncidents().isEmpty());
            assertEquals(1, dispatch.listResponders().size());
        }
    }

    @Test
    @DisplayName("without a login, even submitting is rejected on the secured server")
    void anonymousIsRejected() throws IOException {
        ServerStub stub = new ServerStub("localhost", this.server.getPort());
        stub.connect();
        try {
            ReportClientPresenter report = new ReportClientPresenter(stub);
            assertUnauthorized(() -> report.submitIncident(HazardType.FIRE,
                    new GpsCoordinate(-23.0, 150.0), "Blaze", 1));
        } finally {
            stub.close();
        }
    }

    /**
     * Asserts an action is rejected with the UNAUTHORIZED status.
     *
     * @param action the client action to attempt.
     */
    private static void assertUnauthorized(Runnable action) {
        ServerStubException ex = assertThrows(ServerStubException.class, action::run);
        assertEquals(Status.UNAUTHORIZED, ex.getStatus());
    }
}
