package edu.cqu.drs.server;

import edu.cqu.drs.data.DataAccessException;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.protocol.Action;
import edu.cqu.drs.protocol.BoardSnapshot;
import edu.cqu.drs.protocol.ProtocolKeys;
import edu.cqu.drs.protocol.Request;
import edu.cqu.drs.protocol.Response;
import edu.cqu.drs.security.AuthException;
import edu.cqu.drs.security.AuthService;
import edu.cqu.drs.security.Session;
import edu.cqu.drs.server.service.IncidentService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Routes each {@link Request} to the matching {@link IncidentService} operation,
 * enforces authentication/authorisation when an {@link AuthService} is present,
 * and maps the result - or any failure - onto a {@link Response}.
 *
 * <p>Two modes: with an {@link AuthService} (the production server) every action
 * except {@code PING}/{@code LOGIN} requires a valid session token of a permitted
 * role, and the acting user is recorded against each audited action; without one
 * (used by the lower-level integration tests) the actions are open and the actor
 * is null.</p>
 *
 * <p>Thread-safe: stateless apart from the injected services (themselves safe for
 * concurrent use), so a single instance is shared by every {@link ClientHandler}.
 * Exceptions are translated to status codes rather than propagated, so a bad
 * request never tears down a worker thread.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DrsRequestDispatcher implements RequestDispatcher {

    private final IncidentService incidentService;

    /** Authentication authority; null disables auth (open mode for low-level tests). */
    private final AuthService authService;

    /**
     * Creates an open-mode dispatcher (no authentication).
     *
     * @param incidentService the incident service (must not be null).
     */
    public DrsRequestDispatcher(IncidentService incidentService) {
        this(incidentService, null);
    }

    /**
     * Creates a dispatcher, optionally secured by an authentication service.
     *
     * @param incidentService the incident service (must not be null).
     * @param authService     the authentication service, or null for open mode.
     * @throws IllegalArgumentException if {@code incidentService} is null.
     */
    public DrsRequestDispatcher(IncidentService incidentService, AuthService authService) {
        if (incidentService == null) {
            throw new IllegalArgumentException("incidentService must not be null");
        }
        this.incidentService = incidentService;
        this.authService = authService;
    }

    @Override
    public Response handle(Request request) {
        if (request == null) {
            return Response.badRequest("null request");
        }
        try {
            return route(request);
        } catch (AuthException ex) {
            return Response.unauthorized(ex.getMessage());
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
     * Authenticates/authorises (when enabled) and dispatches the request.
     *
     * @param request the request to route.
     * @return the response.
     */
    private Response route(Request request) {
        Action action = request.getAction();
        if (action == Action.PING) {
            return Response.ok("pong");
        }
        if (action == Action.LOGIN) {
            return login(request);
        }

        UUID actorId = null;
        if (this.authService != null) {
            actorId = authorize(action, request.getToken()).getId();
        }
        if (action == Action.LOGOUT) {
            if (this.authService != null) {
                this.authService.logout(request.getToken());
            }
            return Response.ok();
        }

        switch (action) {
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
            case GET_BOARD:
                // The snapshot time is the SERVER clock: a polling board's
                // "last updated" must state when the data was true on the
                // authority, not when a drifting client clock received it.
                return Response.ok(new BoardSnapshot(
                        this.incidentService.listIncidents(), LocalDateTime.now()));
            default:
                return Response.error("Unsupported action: " + action);
        }
    }

    /**
     * Handles a login request, returning the new {@link Session} on success.
     *
     * @param request the login request.
     * @return an OK response carrying the session, or an error if auth is disabled.
     * @throws AuthException if the credentials are invalid.
     */
    private Response login(Request request) {
        if (this.authService == null) {
            return Response.error("authentication is not enabled on this server");
        }
        String token = this.authService.login(
                request.getString(ProtocolKeys.USERNAME),
                request.getString(ProtocolKeys.PASSWORD));
        User user = this.authService.resolve(token)
                .orElseThrow(() -> new AuthException("session lost immediately after login"));
        return Response.ok(new Session(token, user));
    }

    /**
     * Authorises a request: the token must resolve to a user with a role
     * permitted for the action. Reporting (SUBMIT) and LOGOUT need only a valid
     * session; the dispatch operations require DISPATCHER or ADMINISTRATOR.
     *
     * @param action the requested action.
     * @param token  the session token.
     * @return the authorised user.
     * @throws AuthException if not authenticated or not permitted.
     */
    private User authorize(Action action, String token) {
        if (action == Action.LOGOUT || action == Action.SUBMIT_INCIDENT) {
            return this.authService.requireRole(token, UserRole.values());
        }
        return this.authService.requireRole(
                token, UserRole.DISPATCHER, UserRole.ADMINISTRATOR);
    }
}
