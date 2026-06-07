package edu.cqu.drs.protocol;

/**
 * The set of operations a client may request of the server over the DRS-Enhanced
 * application protocol. The action travels inside a {@link Request}; the server's
 * dispatcher routes on it to the matching service method.
 *
 * <p>Enumerations are {@link java.io.Serializable} by default, so this type is
 * safe to send across the {@link java.io.ObjectOutputStream} wire.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum Action {

    /** Liveness check; the server replies OK. */
    PING,

    /** Authenticate a user and open a session (returns a session token). */
    LOGIN,

    /** Invalidate the caller's session token. */
    LOGOUT,

    /** File a new incident from a citizen report. */
    SUBMIT_INCIDENT,

    /** List the current incidents, most urgent first. */
    LIST_INCIDENTS,

    /** Triage an incident: set its severity. */
    TRIAGE_INCIDENT,

    /** Allocate a responder to an incident. */
    ASSIGN_RESPONDER,

    /** Mark an incident resolved. */
    RESOLVE_INCIDENT,

    /** Recommend (and record) a public-alert template for an incident. */
    RECOMMEND_TEMPLATE,

    /** List the field responders. */
    LIST_RESPONDERS
}
