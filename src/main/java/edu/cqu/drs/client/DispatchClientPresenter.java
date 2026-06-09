package edu.cqu.drs.client;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;

import java.util.List;
import java.util.UUID;

/**
 * Client-side presenter for the dispatcher console (MVP): every triage,
 * allocation, resolution and listing operation is performed on the
 * <em>server</em> through the {@link ServerStub}, replacing the single-process
 * {@code presenter.DispatchPresenter} that mutated the in-memory queue.
 *
 * <p>The view works with whole {@link Incident}/{@link Responder} objects (its
 * table selection); this presenter translates them to the domain {@link UUID}s
 * the wire protocol carries, and returns the server's updated snapshots so the
 * view can refresh from authoritative state. The server enforces the role gate;
 * a caller without the DISPATCHER/ADMINISTRATOR role receives a
 * {@link ServerStubException} with an UNAUTHORIZED status.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DispatchClientPresenter {

    private final ServerStub serverStub;

    /**
     * Creates a dispatch presenter over a connected server stub.
     *
     * @param serverStub the server gateway (must not be null).
     * @throws IllegalArgumentException if {@code serverStub} is null.
     */
    public DispatchClientPresenter(ServerStub serverStub) {
        if (serverStub == null) {
            throw new IllegalArgumentException("serverStub must not be null");
        }
        this.serverStub = serverStub;
    }

    /**
     * Returns the server's incidents, most urgent first.
     *
     * @return the ordered incident snapshot (never null).
     * @throws ServerStubException if the call fails.
     */
    public List<Incident> pendingIncidents() {
        return this.serverStub.listIncidents();
    }

    /**
     * Returns the number of incidents currently held by the server.
     *
     * @return the incident count.
     * @throws ServerStubException if the call fails.
     */
    public int pendingCount() {
        return pendingIncidents().size();
    }

    /**
     * Triages an incident on the server: sets its severity (and TRIAGED status).
     *
     * @param incidentId the incident's id (must not be null).
     * @param severity   the severity to apply (must not be null).
     * @return the updated incident as persisted by the server.
     * @throws IllegalArgumentException if either argument is null.
     * @throws ServerStubException if the server rejects the action.
     */
    public Incident triage(UUID incidentId, Severity severity) {
        if (incidentId == null || severity == null) {
            throw new IllegalArgumentException("incidentId and severity are required");
        }
        return this.serverStub.triage(incidentId, severity);
    }

    /**
     * Allocates a responder to an incident on the server.
     *
     * @param incidentId  the incident's id (must not be null).
     * @param responderId the responder's id (must not be null).
     * @return the incident with its responders attached.
     * @throws IllegalArgumentException if either argument is null.
     * @throws ServerStubException if the server rejects the allocation.
     */
    public Incident assignResponder(UUID incidentId, UUID responderId) {
        if (incidentId == null || responderId == null) {
            throw new IllegalArgumentException("incidentId and responderId are required");
        }
        return this.serverStub.assignResponder(incidentId, responderId);
    }

    /**
     * Marks an incident resolved on the server.
     *
     * @param incidentId the incident's id (must not be null).
     * @return the updated incident.
     * @throws IllegalArgumentException if {@code incidentId} is null.
     * @throws ServerStubException if the server rejects the action.
     */
    public Incident resolve(UUID incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("incidentId must not be null");
        }
        return this.serverStub.resolve(incidentId);
    }

    /**
     * Asks the server to recommend (and record) a public-alert template.
     *
     * @param incidentId the incident's id (must not be null).
     * @return the recommended template.
     * @throws IllegalArgumentException if {@code incidentId} is null.
     * @throws ServerStubException if the server rejects the action.
     */
    public AlertTemplate recommendTemplate(UUID incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("incidentId must not be null");
        }
        return this.serverStub.recommendTemplate(incidentId);
    }

    /**
     * Returns the server's field-responder roster.
     *
     * @return the responders (never null).
     * @throws ServerStubException if the call fails.
     */
    public List<Responder> listResponders() {
        return this.serverStub.listResponders();
    }

    /**
     * Compact summary used in debug logs (no live server call).
     *
     * @return e.g. {@code "DispatchClientPresenter{transport=ServerStub}"}.
     */
    @Override
    public String toString() {
        return "DispatchClientPresenter{transport=ServerStub}";
    }
}
