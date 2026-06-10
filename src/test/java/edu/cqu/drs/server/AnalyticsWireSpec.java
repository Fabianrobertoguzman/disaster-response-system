package edu.cqu.drs.server;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.client.ServerStubException;
import edu.cqu.drs.data.AnalyticsFixture;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.AnalyticsReport;
import edu.cqu.drs.protocol.Status;
import edu.cqu.drs.security.AuthService;
import edu.cqu.drs.security.PasswordHasher;
import edu.cqu.drs.server.service.AnalyticsService;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end tests for the f2 analytics wire path: a dispatcher pulls the
 * server-assembled {@link AnalyticsReport} over a real socket (the fixture's
 * expecteds intact after serialisation), and the action is dispatcher-gated.
 * In-memory backed, loopback only; runs unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Analytics (f2) - GET_ANALYTICS over the wire")
class AnalyticsWireSpec {

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
                this.store.responderDao(), this.store.auditDao(), new AlertTemplateRecommender());
        AnalyticsService analyticsService = new AnalyticsService(this.store.analyticsDao());
        this.server = new DrsServer(0, new DrsRequestDispatcher(
                incidentService, authService, analyticsService));
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
    @DisplayName("a dispatcher receives the fixture's report intact over the socket")
    void shouldServeReportOverWire() throws IOException {
        AnalyticsFixture.seed(this.store.incidentDao());
        try (ServerStub dispatcher = loginAs("dispatch1", "pw-dispatch")) {
            AnalyticsReport report = dispatcher.getAnalytics();
            assertEquals(AnalyticsFixture.EXPECTED_BY_HAZARD, report.getHazardCounts());
            assertEquals(AnalyticsFixture.EXPECTED_BY_SEVERITY, report.getSeverityCounts());
            assertEquals(AnalyticsFixture.EXPECTED_BY_STATUS, report.getStatusCounts());
            assertEquals(AnalyticsFixture.EXPECTED_TOTAL_VICTIMS, report.getTotalVictims());
            assertEquals(3, report.getResponseTimes().getResolvedCount());
            assertEquals(45.0, report.getResponseTimes().getAverageMinutes());
        }
    }

    @Test
    @DisplayName("the analytics report is dispatcher-gated: a CITIZEN gets UNAUTHORIZED")
    void shouldGateAnalyticsToDispatchers() throws IOException {
        try (ServerStub citizen = loginAs("cit1", "pw-citizen")) {
            ServerStubException ex = assertThrows(
                    ServerStubException.class, citizen::getAnalytics);
            assertEquals(Status.UNAUTHORIZED, ex.getStatus());
        }
    }

    @Test
    @DisplayName("a server without the analytics service wired answers with a clear error")
    void shouldReportDisabledAnalytics() throws IOException {
        try (DrsServer bare = new DrsServer(0, new DrsRequestDispatcher(
                new IncidentService(this.store.incidentDao(), this.store.responderDao(),
                        this.store.auditDao(), new AlertTemplateRecommender())))) {
            bare.start();
            ServerStub stub = new ServerStub("localhost", bare.getPort());
            stub.connect();
            try {
                ServerStubException ex = assertThrows(
                        ServerStubException.class, stub::getAnalytics);
                assertEquals(Status.ERROR, ex.getStatus());
            } finally {
                stub.close();
            }
        }
    }
}
