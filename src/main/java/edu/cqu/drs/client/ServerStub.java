package edu.cqu.drs.client;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.protocol.Action;
import edu.cqu.drs.protocol.ProtocolKeys;
import edu.cqu.drs.protocol.Request;
import edu.cqu.drs.protocol.Response;
import edu.cqu.drs.protocol.Status;
import edu.cqu.drs.security.Session;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

/**
 * Client-side gateway to the server: the single object the client-side presenters
 * talk to, hiding the socket, the {@link ObjectOutputStream}/{@link
 * ObjectInputStream} handshake and the {@link Request}/{@link Response} protocol.
 *
 * <p>This is the seam that makes the client a genuine MVP client: the JavaFX
 * controllers/presenters call typed methods here ({@link #submitIncident}, {@link
 * #listIncidents}, ...) and never see the wire format. One stub holds one
 * persistent connection; {@link #send(Request)} is synchronised so the stub is
 * safe to share, and each separate client uses its own stub.</p>
 *
 * <p>The stream-construction order (output first, then flush, then input) mirrors
 * {@link edu.cqu.drs.server.ClientHandler} to avoid the object-stream handshake
 * deadlock.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ServerStub implements AutoCloseable {

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /** Session token, set by the security increment after a successful login. */
    private String token;

    /**
     * Creates a stub aimed at a server endpoint (no connection is opened yet).
     *
     * @param host the server host (must not be null).
     * @param port the server port.
     * @throws IllegalArgumentException if {@code host} is null.
     */
    public ServerStub(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("host must not be null");
        }
        this.host = host;
        this.port = port;
    }

    /**
     * Opens the connection and performs the object-stream handshake.
     *
     * @throws IOException if the connection cannot be established.
     */
    public synchronized void connect() throws IOException {
        this.socket = new Socket(this.host, this.port);
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(this.socket.getInputStream());
    }

    /**
     * Sends a request (stamped with the current session token) and returns the
     * response.
     *
     * @param request the request to send (must not be null).
     * @return the server's response.
     * @throws ServerStubException if the transport fails.
     */
    public synchronized Response send(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        try {
            request.withToken(this.token);
            this.out.writeObject(request);
            this.out.flush();
            this.out.reset();
            return (Response) this.in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new ServerStubException(Status.ERROR,
                    "communication failure: " + ex.getMessage());
        }
    }

    /** @return the server's reply to a liveness check (expected: "pong"). */
    public String ping() {
        return (String) requireOk(send(new Request(Action.PING)));
    }

    /**
     * Authenticates with the server and stores the session token for subsequent
     * requests.
     *
     * @param username the login name.
     * @param password the password.
     * @return the established {@link Session} (token + authenticated user).
     * @throws ServerStubException with {@link Status#UNAUTHORIZED} if the
     *         credentials are rejected.
     */
    public Session login(String username, String password) {
        Session session = (Session) requireOk(send(new Request(Action.LOGIN)
                .with(ProtocolKeys.USERNAME, username)
                .with(ProtocolKeys.PASSWORD, password)));
        setToken(session.getToken());
        return session;
    }

    /** Ends the current session and clears the stored token. */
    public void logout() {
        send(new Request(Action.LOGOUT));
        setToken(null);
    }

    /**
     * Files a new incident.
     *
     * @param hazardType  the hazard type.
     * @param latitude    the latitude.
     * @param longitude   the longitude.
     * @param description the description.
     * @param victimCount the victim count.
     * @return the created incident.
     */
    public Incident submitIncident(HazardType hazardType, double latitude, double longitude,
            String description, int victimCount) {
        return (Incident) requireOk(send(new Request(Action.SUBMIT_INCIDENT)
                .with(ProtocolKeys.HAZARD_TYPE, hazardType)
                .with(ProtocolKeys.LATITUDE, latitude)
                .with(ProtocolKeys.LONGITUDE, longitude)
                .with(ProtocolKeys.DESCRIPTION, description)
                .with(ProtocolKeys.VICTIM_COUNT, victimCount)));
    }

    /** @return all incidents, most urgent first. */
    @SuppressWarnings("unchecked")
    public List<Incident> listIncidents() {
        return (List<Incident>) requireOk(send(new Request(Action.LIST_INCIDENTS)));
    }

    /**
     * Triages an incident.
     *
     * @param incidentId the incident id.
     * @param severity   the severity to apply.
     * @return the updated incident.
     */
    public Incident triage(UUID incidentId, Severity severity) {
        return (Incident) requireOk(send(new Request(Action.TRIAGE_INCIDENT)
                .with(ProtocolKeys.INCIDENT_ID, incidentId)
                .with(ProtocolKeys.SEVERITY, severity)));
    }

    /**
     * Allocates a responder to an incident.
     *
     * @param incidentId  the incident id.
     * @param responderId the responder id.
     * @return the incident with its responders attached.
     */
    public Incident assignResponder(UUID incidentId, UUID responderId) {
        return (Incident) requireOk(send(new Request(Action.ASSIGN_RESPONDER)
                .with(ProtocolKeys.INCIDENT_ID, incidentId)
                .with(ProtocolKeys.RESPONDER_ID, responderId)));
    }

    /**
     * Marks an incident resolved.
     *
     * @param incidentId the incident id.
     * @return the updated incident.
     */
    public Incident resolve(UUID incidentId) {
        return (Incident) requireOk(send(new Request(Action.RESOLVE_INCIDENT)
                .with(ProtocolKeys.INCIDENT_ID, incidentId)));
    }

    /**
     * Recommends (and records) an alert template for an incident.
     *
     * @param incidentId the incident id.
     * @return the recommended template.
     */
    public AlertTemplate recommendTemplate(UUID incidentId) {
        return (AlertTemplate) requireOk(send(new Request(Action.RECOMMEND_TEMPLATE)
                .with(ProtocolKeys.INCIDENT_ID, incidentId)));
    }

    /** @return the field responders. */
    @SuppressWarnings("unchecked")
    public List<Responder> listResponders() {
        return (List<Responder>) requireOk(send(new Request(Action.LIST_RESPONDERS)));
    }

    /** @return the current session token, or null if not logged in. */
    public synchronized String getToken() {
        return this.token;
    }

    /**
     * Sets the session token (used by the security increment after login).
     *
     * @param token the session token, or null to clear it.
     */
    public synchronized void setToken(String token) {
        this.token = token;
    }

    /**
     * Returns the payload of a successful response, or throws on a non-OK status.
     *
     * @param response the response to unwrap.
     * @return the payload (may be null).
     * @throws ServerStubException if the status is not OK.
     */
    private static Serializable requireOk(Response response) {
        if (response == null) {
            throw new ServerStubException(Status.ERROR, "no response from server");
        }
        if (!response.isOk()) {
            throw new ServerStubException(response.getStatus(), response.getMessage());
        }
        return response.getPayload();
    }

    /** Closes the connection. Safe to call when never connected. */
    @Override
    public synchronized void close() {
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException ex) {
            System.err.println("Error closing client connection: " + ex.getMessage());
        }
    }
}
