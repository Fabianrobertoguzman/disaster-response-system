package edu.cqu.drs.protocol;

import java.io.Serializable;

/**
 * A serialisable server-to-client response: a {@link Status}, a human-readable
 * message, and an optional payload (for example an incident, or a list of
 * incidents) that must itself be {@link Serializable}.
 *
 * <p>Static factory methods give each outcome a readable call-site
 * ({@code Response.ok(list)}, {@code Response.unauthorized("...")}).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The outcome status (never null). */
    private final Status status;

    /** A human-readable message describing the outcome (never null). */
    private final String message;

    /** The result payload, or null when there is none. */
    private final Serializable payload;

    /**
     * Creates a response.
     *
     * @param status  the outcome status (must not be null).
     * @param message a human-readable message (null is coerced to "").
     * @param payload the result payload, or null.
     */
    private Response(Status status, String message, Serializable payload) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        this.message = (message == null) ? "" : message;
        this.payload = payload;
    }

    /**
     * @param payload the result payload (may be null).
     * @return a success response carrying the payload.
     */
    public static Response ok(Serializable payload) {
        return new Response(Status.OK, "OK", payload);
    }

    /**
     * @return a success response with no payload.
     */
    public static Response ok() {
        return new Response(Status.OK, "OK", null);
    }

    /**
     * @param message the failure description.
     * @return an error response.
     */
    public static Response error(String message) {
        return new Response(Status.ERROR, message, null);
    }

    /**
     * @param message the reason the caller was rejected.
     * @return an unauthorized response.
     */
    public static Response unauthorized(String message) {
        return new Response(Status.UNAUTHORIZED, message, null);
    }

    /**
     * @param message the reason the request was malformed.
     * @return a bad-request response.
     */
    public static Response badRequest(String message) {
        return new Response(Status.BAD_REQUEST, message, null);
    }

    /**
     * @param message the reason the entity was not found.
     * @return a not-found response.
     */
    public static Response notFound(String message) {
        return new Response(Status.NOT_FOUND, message, null);
    }

    /** @return the outcome status. */
    public Status getStatus() {
        return this.status;
    }

    /** @return the human-readable message. */
    public String getMessage() {
        return this.message;
    }

    /** @return the result payload, or null. */
    public Serializable getPayload() {
        return this.payload;
    }

    /** @return true if the status is {@link Status#OK}. */
    public boolean isOk() {
        return this.status == Status.OK;
    }

    /**
     * Hand-written display string.
     *
     * @return e.g. {@code Response{status=OK, message=OK, payload=Incident{...}}}.
     */
    @Override
    public String toString() {
        return "Response{status=" + this.status
                + ", message=" + this.message
                + ", payload=" + this.payload + "}";
    }
}
