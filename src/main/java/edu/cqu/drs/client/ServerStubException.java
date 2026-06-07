package edu.cqu.drs.client;

import edu.cqu.drs.protocol.Status;

/**
 * Thrown by {@link ServerStub} when a request fails - either the transport broke,
 * or the server returned a non-OK {@link Status}. It carries the status so the
 * client UI can react (for example, send the user back to the login screen on
 * {@link Status#UNAUTHORIZED}).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ServerStubException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The status that caused the failure. */
    private final Status status;

    /**
     * Creates a stub exception.
     *
     * @param status  the failure status (must not be null).
     * @param message the failure message.
     */
    public ServerStubException(Status status, String message) {
        super(message);
        this.status = status;
    }

    /** @return the status that caused the failure. */
    public Status getStatus() {
        return this.status;
    }
}
