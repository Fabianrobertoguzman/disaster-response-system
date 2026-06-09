package edu.cqu.drs.client;

import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.server.DrsRequestDispatcher;
import edu.cqu.drs.server.DrsServer;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link ReportClientPresenter} over a real socket: a
 * {@link DrsServer} on an ephemeral port backed by the in-memory store. They
 * prove a citizen report filed through the client presenter lands on the
 * <em>server</em> (not in any in-process queue) - the client tier of the
 * distributed submit path. No MySQL, loopback only, so they run unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("ReportClientPresenter - citizen report over the wire")
class ReportClientPresenterSpec {

    private InMemoryDataStore store;
    private DrsServer server;
    private ServerStub stub;
    private ReportClientPresenter presenter;

    @BeforeEach
    void startServer() throws IOException {
        this.store = new InMemoryDataStore();
        IncidentService service = new IncidentService(this.store.incidentDao(),
                this.store.responderDao(), this.store.auditDao(), new AlertTemplateRecommender());
        this.server = new DrsServer(0, new DrsRequestDispatcher(service));
        this.server.start();
        this.stub = new ServerStub("localhost", this.server.getPort());
        this.stub.connect();
        this.presenter = new ReportClientPresenter(this.stub);
    }

    @AfterEach
    void stopServer() {
        this.stub.close();
        this.server.stop();
    }

    @Test
    @DisplayName("a submitted report is persisted on the server and listed back")
    void shouldSubmitToServer() {
        Incident submitted = this.presenter.submitIncident(HazardType.FIRE,
                new GpsCoordinate(-23.3781, 150.5136), "Warehouse blaze", 3);

        assertEquals(IncidentStatus.REPORTED, submitted.getStatus());
        List<Incident> onServer = this.store.incidentDao().findAll();
        assertEquals(1, onServer.size());
        assertEquals(submitted.getId(), onServer.get(0).getId());
    }

    @Test
    @DisplayName("the captured GpsCoordinate is translated to the wire and back intact")
    void shouldTranslateCoordinate() {
        Incident submitted = this.presenter.submitIncident(HazardType.FLOOD,
                new GpsCoordinate(-23.3781, 150.5136), "Levee", 0);
        assertEquals(-23.3781, submitted.getGpsLocation().getLatitude());
        assertEquals(150.5136, submitted.getGpsLocation().getLongitude());
    }

    @Test
    @DisplayName("every submission lands on the server (no in-process queue involved)")
    void shouldAccumulateOnServer() {
        this.presenter.submitIncident(HazardType.STORM,
                new GpsCoordinate(-23.0, 150.0), "Wind", 1);
        this.presenter.submitIncident(HazardType.HAZMAT,
                new GpsCoordinate(-23.0, 150.0), "Spill", 2);
        assertEquals(2, this.store.incidentDao().findAll().size());
    }

    @Test
    @DisplayName("a null hazard type or location is rejected before reaching the wire")
    void shouldRejectNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> this.presenter.submitIncident(
                null, new GpsCoordinate(-23.0, 150.0), "x", 0));
        assertThrows(IllegalArgumentException.class, () -> this.presenter.submitIncident(
                HazardType.FIRE, null, "x", 0));
    }

    @Test
    @DisplayName("a negative victim count is rejected by the server as a bad request")
    void shouldRejectNegativeVictims() {
        ServerStubException ex = assertThrows(ServerStubException.class,
                () -> this.presenter.submitIncident(HazardType.FIRE,
                        new GpsCoordinate(-23.0, 150.0), "x", -1));
        assertEquals(edu.cqu.drs.protocol.Status.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("connecting to a server that is not running fails with IOException")
    void shouldFailWhenServerDown() throws IOException {
        // Bind an ephemeral port and release it: nothing listens there afterwards,
        // so the connection is refused deterministically on every platform.
        int freePort;
        try (java.net.ServerSocket probe = new java.net.ServerSocket(0)) {
            freePort = probe.getLocalPort();
        }
        ServerStub unreachable = new ServerStub("localhost", freePort);
        assertThrows(IOException.class, unreachable::connect);
        unreachable.close();
    }

    @Test
    @DisplayName("toString never performs a live server call")
    void shouldFormatToString() {
        assertTrue(this.presenter.toString().startsWith("ReportClientPresenter{"));
    }
}
