package edu.cqu.drs.server;

import edu.cqu.drs.data.DataAccessException;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.protocol.ProtocolKeys;
import edu.cqu.drs.protocol.Request;
import edu.cqu.drs.protocol.Response;
import edu.cqu.drs.server.service.IncidentService;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Routes each {@link Request} to the matching {@link IncidentService} operation
 * and maps the result - or any failure - onto a {@link Response}.
 *
 * <p>Thread-safe: it is stateless apart from the injected service (which is itself
 * safe for concurrent use), so a single instance is shared by every
 * {@link ClientHandler}. Exceptions are translated to status codes rather than
 * propagated, so a bad request never tears down a worker thread.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DrsRequestDispatcher implements RequestDispatcher {

    private final IncidentService incidentService;

    /**
     * Creates the dispatcher over the incident service.
     *
     * @param incidentService the incident service (must not be null).
     * @throws IllegalArgumentException if {@code incidentService} is null.
     */
    public DrsRequestDispatcher(IncidentService incidentService) {
        if (incidentService == null) {
            throw new IllegalArgumentException("incidentService must not be null");
        }
        this.incidentService = incidentService;
    }

    @Override
    public Response handle(Request request) {
        if (request == null) {
            return Response.badRequest("null request");
        }
        try {
            return route(request);
        } catch (NoSuchElementException ex) {
            return Response.notFound(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return Response.badRequest(ex.getMessage());
        } catch (DataAccessException ex) {
            return Response.error("data error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return Response.error("server error: " + ex.getMessage());
        }
    }

    /**
     * Dispatches a request to the matching operation. The acting user is null in
     * this increment; the security increment resolves it from the session token.
     *
     * @param request the request to route.
     * @return the response.
     */
    private Response route(Request request) {
        UUID actorId = null;
        switch (request.getAction()) {
            case PING:
                return Response.ok("pong");
            case SUBMIT_INCIDENT:
                return Response.ok(this.incidentService.submitIncident(
                        (HazardType) request.get(ProtocolKeys.HAZARD_TYPE),
                        new GpsCoordinate(request.getDouble(ProtocolKeys.LATITUDE),
                                request.getDouble(ProtocolKeys.LONGITUDE)),
                        request.getString(ProtocolKeys.DESCRIPTION),
                        request.getInt(ProtocolKeys.VICTIM_COUNT),
                        actorId));
            case LIST_INCIDENTS:
                return Response.ok(new ArrayList<>(this.incidentService.listIncidents()));
            case TRIAGE_INCIDENT:
                return Response.ok(this.incidentService.triage(
                        request.getUuid(ProtocolKeys.INCIDENT_ID),
                        (Severity) request.get(ProtocolKeys.SEVERITY),
                        actorId));
            case ASSIGN_RESPONDER:
                return Response.ok(this.incidentService.assignResponder(
                        request.getUuid(ProtocolKeys.INCIDENT_ID),
                        request.getUuid(ProtocolKeys.RESPONDER_ID),
                        actorId));
            case RESOLVE_INCIDENT:
                return Response.ok(this.incidentService.resolve(
                        request.getUuid(ProtocolKeys.INCIDENT_ID), actorId));
            case RECOMMEND_TEMPLATE:
                return Response.ok(this.incidentService.recommendTemplate(
                        request.getUuid(ProtocolKeys.INCIDENT_ID), actorId));
            case LIST_RESPONDERS:
                return Response.ok(new ArrayList<>(this.incidentService.listResponders()));
            default:
                return Response.error("Unsupported action: " + request.getAction());
        }
    }
}
