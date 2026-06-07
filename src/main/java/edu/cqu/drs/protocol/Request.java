package edu.cqu.drs.protocol;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A serialisable client-to-server request: an {@link Action}, an optional session
 * token (set once the caller has logged in), and a small parameter map.
 *
 * <p>Every value placed in the parameter map must itself be {@link Serializable}
 * (String, boxed primitives, {@link UUID}, the domain enums, and the domain model
 * classes all are), because the whole request is written across an
 * {@link java.io.ObjectOutputStream}. The builder-style {@link #with(String,
 * Serializable)} and {@link #withToken(String)} methods allow a request to be
 * assembled in one expression.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The requested operation (never null). */
    private final Action action;

    /** The caller's session token, or null before login. */
    private String token;

    /** Operation parameters keyed by {@link ProtocolKeys} (never null). */
    private final Map<String, Serializable> params = new LinkedHashMap<>();

    /**
     * Creates a request for an action with no token.
     *
     * @param action the operation requested (must not be null).
     * @throws IllegalArgumentException if {@code action} is null.
     */
    public Request(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        this.action = action;
    }

    /** @return the requested operation. */
    public Action getAction() {
        return this.action;
    }

    /** @return the caller's session token, or null if not authenticated. */
    public String getToken() {
        return this.token;
    }

    /**
     * Sets the session token and returns this request (builder style).
     *
     * @param token the session token, or null to clear it.
     * @return this request.
     */
    public Request withToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * Adds a parameter and returns this request (builder style).
     *
     * @param key   the parameter key (use a {@link ProtocolKeys} constant).
     * @param value the parameter value (must be serialisable; may be null).
     * @return this request.
     */
    public Request with(String key, Serializable value) {
        this.params.put(key, value);
        return this;
    }

    /** @return an unmodifiable view of the parameter map. */
    public Map<String, Serializable> getParams() {
        return Collections.unmodifiableMap(this.params);
    }

    /**
     * @param key the parameter key.
     * @return the raw parameter value, or null if absent.
     */
    public Serializable get(String key) {
        return this.params.get(key);
    }

    /**
     * @param key the parameter key.
     * @return the parameter as a String, or null if absent.
     */
    public String getString(String key) {
        Serializable value = this.params.get(key);
        return (value == null) ? null : value.toString();
    }

    /**
     * @param key the parameter key.
     * @return the parameter as a {@link UUID}, or null if absent.
     * @throws IllegalArgumentException if the value is not a valid UUID.
     */
    public UUID getUuid(String key) {
        Serializable value = this.params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        return UUID.fromString(value.toString());
    }

    /**
     * @param key the parameter key.
     * @return the parameter as an int.
     * @throws IllegalArgumentException if the value is absent or not numeric.
     */
    public int getInt(String key) {
        Serializable value = this.params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            throw new IllegalArgumentException("missing integer parameter: " + key);
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * @param key the parameter key.
     * @return the parameter as a double.
     * @throws IllegalArgumentException if the value is absent or not numeric.
     */
    public double getDouble(String key) {
        Serializable value = this.params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            throw new IllegalArgumentException("missing double parameter: " + key);
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * Hand-written display string (the token is redacted so it never lands in a log).
     *
     * @return e.g. {@code Request{action=TRIAGE_INCIDENT, token=present, params=[incidentId, severity]}}.
     */
    @Override
    public String toString() {
        return "Request{action=" + this.action
                + ", token=" + (this.token == null ? "none" : "present")
                + ", params=" + this.params.keySet() + "}";
    }
}
