package edu.cqu.drs.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A field responder who can be allocated to an {@link Incident}.
 *
 * <p>Realises the "people" side of "coordinate different departments and people": Assessment&nbsp;One's
 * FR-05 lets a Dispatcher "select one or more available Responders for a triaged incident". A responder
 * with no current tasking is available; once {@link Incident#assignResponder(Responder)} allocates them,
 * their {@link #currentTaskingId} is set to that incident's id and they are no longer available.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class Responder {

    /** Immutable unique identifier. */
    private final UUID id;

    /** Responder's display name (never null or blank). */
    private String name;

    /** Id of the incident this responder is currently tasked to, or {@code null} if available. */
    private UUID currentTaskingId;

    /**
     * Creates an available responder.
     *
     * @param name the responder's display name (must not be null or blank).
     * @throws IllegalArgumentException if {@code name} is null or blank.
     */
    public Responder(String name) {
        requireNonBlankName(name);
        this.id = UUID.randomUUID();
        this.name = name;
        this.currentTaskingId = null;
    }

    /**
     * Validates that a responder name is non-null and non-blank.
     *
     * @param name the name to check.
     * @throws IllegalArgumentException if null or blank.
     */
    private static void requireNonBlankName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("responder name must not be null or blank");
        }
    }

    /** @return the immutable responder identifier. */
    public UUID getId() {
        return this.id;
    }

    /** @return the responder's display name. */
    public String getName() {
        return this.name;
    }

    /** @param name the new display name (must not be null or blank). @throws IllegalArgumentException if invalid. */
    public void setName(String name) {
        requireNonBlankName(name);
        this.name = name;
    }

    /** @return the id of the incident this responder is tasked to, or {@code null} if available. */
    public UUID getCurrentTaskingId() {
        return this.currentTaskingId;
    }

    /** @param currentTaskingId the incident id this responder is now tasked to, or {@code null} to free them. */
    public void setCurrentTaskingId(UUID currentTaskingId) {
        this.currentTaskingId = currentTaskingId;
    }

    /** @return true if this responder has no current tasking and can be allocated. */
    public boolean isAvailable() {
        return this.currentTaskingId == null;
    }

    /**
     * Hand-written display string for log/dialog output (a default Object.toString returns only Class@hash).
     *
     * @return e.g. {@code "Responder{id=..., name=Alex Tan, tasking=...}"}.
     */
    @Override
    public String toString() {
        return "Responder{id=" + this.id + ", name=" + this.name + ", tasking=" + this.currentTaskingId + "}";
    }

    /**
     * Identity equality on the immutable {@link #id}.
     *
     * @param other the object to compare with.
     * @return true if {@code other} is a {@code Responder} with the same id.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Responder)) {
            return false;
        }
        return this.id.equals(((Responder) other).id);
    }

    /** @return a hash code consistent with {@link #equals(Object)} (based on {@link #id}). */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
