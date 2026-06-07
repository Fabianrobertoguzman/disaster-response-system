package edu.cqu.drs.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable persistence-tier record of a single auditable action, mirroring one
 * row of the {@code audit_log} table.
 *
 * <p>Assessment&nbsp;One's FR-14 requires an append-only audit trail. In the
 * enhanced system that trail is persisted in the database rather than held only
 * in memory; this value object is the data-tier representation that
 * {@link AuditDao} writes and reads. The acting user and the related incident
 * are referenced by their domain {@link UUID}s (not the database surrogate keys),
 * and both are optional because some events have no actor (e.g. a system event)
 * or no associated incident.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class AuditEntry implements Serializable {

    /** Serialisation version (audit entries travel over the client/server protocol). */
    private static final long serialVersionUID = 1L;

    /** The acting user's domain id, or null if the action had no specific actor. */
    private final UUID actorUuid;

    /** The related incident's domain id, or null if the action is not incident-scoped. */
    private final UUID incidentUuid;

    /** A short description of the action (never null or blank). */
    private final String action;

    /** The kind of entity affected (e.g. {@code "Incident"}), or null. */
    private final String entity;

    /** The affected entity's domain id, or null. */
    private final UUID entityUuid;

    /** When the action occurred; null on a not-yet-persisted entry (the database stamps it). */
    private final LocalDateTime timestamp;

    /**
     * Creates an audit entry to be recorded. The timestamp is left null so the
     * database default ({@code CURRENT_TIMESTAMP}) supplies it on insert.
     *
     * @param actorUuid    the acting user's id, or null.
     * @param incidentUuid the related incident's id, or null.
     * @param action       a short description of the action (must not be null or blank).
     * @param entity       the kind of entity affected, or null.
     * @param entityUuid   the affected entity's id, or null.
     * @throws IllegalArgumentException if {@code action} is null or blank.
     */
    public AuditEntry(UUID actorUuid, UUID incidentUuid, String action,
            String entity, UUID entityUuid) {
        this(actorUuid, incidentUuid, action, entity, entityUuid, null);
    }

    /**
     * Full constructor used when reconstructing a stored entry (including the
     * database-assigned timestamp).
     *
     * @param actorUuid    the acting user's id, or null.
     * @param incidentUuid the related incident's id, or null.
     * @param action       a short description of the action (must not be null or blank).
     * @param entity       the kind of entity affected, or null.
     * @param entityUuid   the affected entity's id, or null.
     * @param timestamp    when the action occurred, or null if not yet persisted.
     * @throws IllegalArgumentException if {@code action} is null or blank.
     */
    public AuditEntry(UUID actorUuid, UUID incidentUuid, String action,
            String entity, UUID entityUuid, LocalDateTime timestamp) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("audit action must not be null or blank");
        }
        this.actorUuid = actorUuid;
        this.incidentUuid = incidentUuid;
        this.action = action;
        this.entity = entity;
        this.entityUuid = entityUuid;
        this.timestamp = timestamp;
    }

    /** @return the acting user's id, or null. */
    public UUID getActorUuid() {
        return this.actorUuid;
    }

    /** @return the related incident's id, or null. */
    public UUID getIncidentUuid() {
        return this.incidentUuid;
    }

    /** @return the short description of the action. */
    public String getAction() {
        return this.action;
    }

    /** @return the kind of entity affected, or null. */
    public String getEntity() {
        return this.entity;
    }

    /** @return the affected entity's id, or null. */
    public UUID getEntityUuid() {
        return this.entityUuid;
    }

    /** @return when the action occurred, or null if this entry has not been persisted. */
    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    /**
     * Hand-written display string (a default {@code Object.toString()} would
     * print only a Class@hash representation).
     *
     * @return e.g. {@code AuditEntry{action=Triaged, entity=Incident, actor=null,
     *         incident=3f2..., at=2026-06-08T09:15}}.
     */
    @Override
    public String toString() {
        return "AuditEntry{action=" + this.action
                + ", entity=" + this.entity
                + ", actor=" + this.actorUuid
                + ", incident=" + this.incidentUuid
                + ", at=" + this.timestamp + "}";
    }
}
