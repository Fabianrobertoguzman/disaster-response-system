package edu.cqu.drs.client;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;

/**
 * Client-side presenter for the citizen-report use case (MVP): it takes the
 * inputs captured by the report view and files the incident on the
 * <em>server</em> through the {@link ServerStub}, replacing the single-process
 * {@code presenter.ReportPresenter} that enqueued into the in-memory queue.
 *
 * <p>It keeps the same use-case surface the report view already calls
 * (hazard type, captured {@link GpsCoordinate}, description, victim count) and
 * translates the coordinate to the wire's latitude/longitude pair, so the
 * controller swap from in-process to client/server is mechanical. As with
 * {@link LoginPresenter}, keeping this off the FXML controller makes the use
 * case testable against a live server without a running JavaFX toolkit.</p>
 *
 * <p>Unlike the in-process predecessor, this presenter deliberately exposes
 * <em>no</em> queue-size query: listing incidents is a dispatcher-gated server
 * action a CITIZEN session is not permitted to call, so the citizen
 * acknowledgement carries the incident reference instead of a queue count.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ReportClientPresenter {

    private final ServerStub serverStub;

    /**
     * Creates a report presenter over a connected server stub.
     *
     * @param serverStub the server gateway (must not be null).
     * @throws IllegalArgumentException if {@code serverStub} is null.
     */
    public ReportClientPresenter(ServerStub serverStub) {
        if (serverStub == null) {
            throw new IllegalArgumentException("serverStub must not be null");
        }
        this.serverStub = serverStub;
    }

    /**
     * Files a new incident on the server.
     *
     * @param hazardType  the reported hazard type (must not be null).
     * @param location    the captured report location (must not be null).
     * @param description the citizen's description (null is treated as empty by
     *                    the server's model).
     * @param victimCount the estimated number of people affected (&gt;= 0).
     * @return the incident as persisted by the server (server-assigned id).
     * @throws IllegalArgumentException if {@code hazardType} or {@code location}
     *         is null.
     * @throws ServerStubException if the server rejects the report or the
     *         transport fails.
     */
    public Incident submitIncident(HazardType hazardType, GpsCoordinate location,
            String description, int victimCount) {
        if (hazardType == null || location == null) {
            throw new IllegalArgumentException("hazardType and location are required");
        }
        return this.serverStub.submitIncident(hazardType,
                location.getLatitude(), location.getLongitude(), description, victimCount);
    }

    /**
     * Compact summary used in debug logs (no live server call).
     *
     * @return e.g. {@code "ReportClientPresenter{transport=ServerStub}"}.
     */
    @Override
    public String toString() {
        return "ReportClientPresenter{transport=ServerStub}";
    }
}
