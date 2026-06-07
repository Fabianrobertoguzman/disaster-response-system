package edu.cqu.drs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A reported disaster incident  -  the central domain entity of DRS-Initial.
 *
 * <p>Created when a Citizen submits a report (FR-01/FR-02/FR-03); triaged with a {@link Severity}
 * by a Dispatcher (FR-04); assigned one or more {@link Responder}s (FR-05); optionally given a
 * recommended {@link AlertTemplate} by the rule-driven recommender (creative feature FR-CR-01);
 * and finally resolved. Every significant action is written to the incident's {@link AuditLog}
 * (FR-14 data side).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class Incident {

    /** Maximum number of {@link Responder}s that may be assigned to a single incident. */
    public static final int MAX_RESPONDERS = 8;

    /** Severity assigned to a freshly-reported incident, before a Dispatcher triages it. */
    public static final Severity DEFAULT_SEVERITY = Severity.MEDIUM;

    /** Immutable unique identifier (the acknowledgement receipt of FR-03). */
    private final UUID id;

    /** The kind of disaster reported (never null). */
    private HazardType hazardType;

    /** Current triage severity; {@link #DEFAULT_SEVERITY} until {@link #triage(Severity)} is called. */
    private Severity severity;

    /** Where the incident is (the captured GPS coordinate; never null). */
    private GpsCoordinate gpsLocation;

    /** Free-text description from the reporter (never null; may be empty). */
    private String description;

    /** Estimated number of people affected; used as the secondary key when ordering the dispatch queue. */
    private int victimCount;

    /** Immutable creation timestamp (the client-device timestamp of FR-02, simplified to server time here). */
    private final LocalDateTime reportedAt;

    /** Current lifecycle state ({@code REPORTED} -> {@code TRIAGED} -> {@code RESOLVED}). */
    private IncidentStatus status;

    /** Responders currently allocated to this incident (0..{@link #MAX_RESPONDERS}). */
    private final List<Responder> responders;

    /** The alert template recommended for this incident, if any (set by creative feature FR-CR-01). */
    private AlertTemplate recommendedTemplate;

    /** Append-only log of actions taken on this incident (FR-14 data side). */
    private final AuditLog auditLog;

    /**
     * Creates a new incident in {@link IncidentStatus#REPORTED} status with the default severity.
     *
     * @param hazardType  the kind of disaster (must not be null).
     * @param gpsLocation the captured location (must not be null).
     * @param description the reporter's free-text description (null is coerced to "").
     * @param victimCount the estimated number of people affected (must not be negative).
     * @throws IllegalArgumentException if {@code hazardType} or {@code gpsLocation} is null,
     *         or if {@code victimCount} is negative.
     */
    public Incident(HazardType hazardType, GpsCoordinate gpsLocation, String description, int victimCount) {
        validateInputs(hazardType, gpsLocation, victimCount);
        this.id = UUID.randomUUID();
        this.hazardType = hazardType;
        this.gpsLocation = gpsLocation;
        this.description = (description == null) ? "" : description;
        this.victimCount = victimCount;
        this.reportedAt = LocalDateTime.now();
        this.status = IncidentStatus.REPORTED;
        this.severity = DEFAULT_SEVERITY;
        this.responders = new ArrayList<>();
        this.recommendedTemplate = null;
        this.auditLog = new AuditLog();
        this.auditLog.record("Incident reported: hazard=" + hazardType + ", victims=" + victimCount);
    }

    /**
     * Reconstruction constructor used <strong>only by the persistence tier</strong>
     * (the data-access objects) to rebuild an incident from a stored database row.
     *
     * <p>Unlike {@link #Incident(HazardType, GpsCoordinate, String, int)}, this
     * constructor accepts the already-assigned identity, timestamp, severity,
     * status, recommended template and responder list, and it deliberately does
     * <strong>not</strong> write a "reported" entry to the audit log: the
     * persisted audit trail lives in its own table, so loading an incident must
     * not fabricate a fresh event. It exists so that the surrogate-key / UUID
     * round-trip performed by the data tier yields an object equal to the one
     * that was originally saved.</p>
     *
     * @param id                  the persisted identifier (must not be null).
     * @param hazardType          the kind of disaster (must not be null).
     * @param severity            the stored triage severity (must not be null).
     * @param gpsLocation         the stored location (must not be null).
     * @param description         the stored description (null is coerced to "").
     * @param victimCount         the stored victim estimate (must not be negative).
     * @param reportedAt          the stored creation timestamp (must not be null).
     * @param status              the stored lifecycle state (must not be null).
     * @param recommendedTemplate the stored recommended template, or null if none.
     * @param responders          the responders allocated to this incident, or null
     *                            for none (at most {@link #MAX_RESPONDERS}).
     * @throws IllegalArgumentException if a required argument is null, if
     *         {@code victimCount} is negative, or if more than
     *         {@link #MAX_RESPONDERS} responders are supplied.
     */
    public Incident(UUID id, HazardType hazardType, Severity severity, GpsCoordinate gpsLocation,
            String description, int victimCount, LocalDateTime reportedAt, IncidentStatus status,
            AlertTemplate recommendedTemplate, List<Responder> responders) {
        validateInputs(hazardType, gpsLocation, victimCount);
        if (id == null || severity == null || reportedAt == null || status == null) {
            throw new IllegalArgumentException(
                    "id, severity, reportedAt and status are required to reconstruct an incident");
        }
        List<Responder> loaded = (responders == null)
                ? new ArrayList<>() : new ArrayList<>(responders);
        if (loaded.size() > MAX_RESPONDERS) {
            throw new IllegalArgumentException(
                    "an incident cannot have more than " + MAX_RESPONDERS + " responders");
        }
        this.id = id;
        this.hazardType = hazardType;
        this.severity = severity;
        this.gpsLocation = gpsLocation;
        this.description = (description == null) ? "" : description;
        this.victimCount = victimCount;
        this.reportedAt = reportedAt;
        this.status = status;
        this.recommendedTemplate = recommendedTemplate;
        this.responders = loaded;
        this.auditLog = new AuditLog();
    }

    /**
     * Validates the constructor inputs. Extracted from the inline guard via an
     * Extract Method refactoring so the constructor stays small and the validation
     * rule lives in one named place. Matches the pattern used by the other model
     * classes (GpsCoordinate.validateRanges, Responder.requireNonBlankName,
     * Resource.requireNonBlankType, User.requireNonBlankUsername /
     * requireNonNullRole).
     *
     * @param hazardType  the kind of disaster (must not be null).
     * @param gpsLocation the captured location (must not be null).
     * @param victimCount the estimated number of people affected (must not be negative).
     * @throws IllegalArgumentException if {@code hazardType} or {@code gpsLocation} is null,
     *         or if {@code victimCount} is negative.
     */
    private static void validateInputs(HazardType hazardType,
            GpsCoordinate gpsLocation, int victimCount) {
        if (hazardType == null || gpsLocation == null) {
            throw new IllegalArgumentException(
                    "hazardType and gpsLocation are required");
        }
        if (victimCount < 0) {
            throw new IllegalArgumentException(
                    "victimCount must not be negative: " + victimCount);
        }
    }

    // --- accessors / mutators ---

    /** @return the immutable incident identifier. */
    public UUID getId() {
        return this.id;
    }

    /** @return the kind of disaster reported. */
    public HazardType getHazardType() {
        return this.hazardType;
    }

    /** @param hazardType the new hazard type (must not be null). @throws IllegalArgumentException if null. */
    public void setHazardType(HazardType hazardType) {
        if (hazardType == null) {
            throw new IllegalArgumentException("hazardType must not be null");
        }
        this.hazardType = hazardType;
    }

    /** @return the current triage severity. */
    public Severity getSeverity() {
        return this.severity;
    }

    /** @param severity the new severity (must not be null). @throws IllegalArgumentException if null. */
    public void setSeverity(Severity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        this.severity = severity;
    }

    /** @return the captured location. */
    public GpsCoordinate getGpsLocation() {
        return this.gpsLocation;
    }

    /** @param gpsLocation the new location (must not be null). @throws IllegalArgumentException if null. */
    public void setGpsLocation(GpsCoordinate gpsLocation) {
        if (gpsLocation == null) {
            throw new IllegalArgumentException("gpsLocation must not be null");
        }
        this.gpsLocation = gpsLocation;
    }

    /** @return the reporter's free-text description (never null; may be empty). */
    public String getDescription() {
        return this.description;
    }

    /** @param description the new description (null is coerced to ""). */
    public void setDescription(String description) {
        this.description = (description == null) ? "" : description;
    }

    /** @return the estimated number of people affected. */
    public int getVictimCount() {
        return this.victimCount;
    }

    /** @param victimCount the new estimate (must not be negative). @throws IllegalArgumentException if negative. */
    public void setVictimCount(int victimCount) {
        if (victimCount < 0) {
            throw new IllegalArgumentException("victimCount must not be negative: " + victimCount);
        }
        this.victimCount = victimCount;
    }

    /** @return the creation timestamp. */
    public LocalDateTime getReportedAt() {
        return this.reportedAt;
    }

    /** @return the current lifecycle state. */
    public IncidentStatus getStatus() {
        return this.status;
    }

    /** @param status the new lifecycle state (must not be null). @throws IllegalArgumentException if null. */
    public void setStatus(IncidentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    /** @return the recommended alert template, or null if none has been set. */
    public AlertTemplate getRecommendedTemplate() {
        return this.recommendedTemplate;
    }

    /** @param recommendedTemplate the template recommended by the creative-feature recommender (may be null). */
    public void setRecommendedTemplate(AlertTemplate recommendedTemplate) {
        this.recommendedTemplate = recommendedTemplate;
    }

    /** @return an unmodifiable view of the responders currently allocated to this incident. */
    public List<Responder> getResponders() {
        return Collections.unmodifiableList(this.responders);
    }

    /** @return this incident's append-only audit log. */
    public AuditLog getAuditLog() {
        return this.auditLog;
    }

    // --- domain operations ---

    /**
     * Triages this incident: records the assigned severity and moves it to {@code TRIAGED} status (FR-04).
     *
     * @param severity the severity classification to assign (must not be null).
     * @throws IllegalArgumentException if {@code severity} is null.
     */
    public void triage(Severity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        this.severity = severity;
        this.status = IncidentStatus.TRIAGED;
        this.auditLog.record("Triaged: severity=" + severity);
    }

    /**
     * Allocates a responder to this incident and sets that responder's current tasking (FR-05  - 
     * the "and people" half of "coordinate different departments and people").
     *
     * @param responder the responder to allocate (must not be null and not already allocated here).
     * @throws IllegalArgumentException if {@code responder} is null or already allocated to this incident.
     * @throws IllegalStateException if this incident already has {@link #MAX_RESPONDERS} responders.
     */
    public void assignResponder(Responder responder) {
        if (responder == null) {
            throw new IllegalArgumentException("responder must not be null");
        }
        if (this.responders.contains(responder)) {
            throw new IllegalArgumentException("responder already allocated to this incident: " + responder.getName());
        }
        if (this.responders.size() >= MAX_RESPONDERS) {
            throw new IllegalStateException("incident already has the maximum of " + MAX_RESPONDERS + " responders");
        }
        this.responders.add(responder);
        responder.setCurrentTaskingId(this.id);
        this.auditLog.record("Responder allocated: " + responder.getName());
    }

    /**
     * Closes this incident: moves it to {@code RESOLVED} status.
     */
    public void resolve() {
        this.status = IncidentStatus.RESOLVED;
        this.auditLog.record("Resolved");
    }

    // --- toString / equals / hashCode ---

    /**
     * Hand-written display string required by the Assessment 2 specification's
     * section-3 coding mandate (a default {@code Object.toString()} would
     * return only a Class@hash representation).
     *
     * @return e.g. {@code Incident{id=3f2..., hazard=FIRE, severity=HIGH,
     *         status=TRIAGED, victims=3, at=2026-05-13T09:15, responders=2}}.
     */
    @Override
    public String toString() {
        return "Incident{id=" + this.id
                + ", hazard=" + this.hazardType
                + ", severity=" + this.severity
                + ", status=" + this.status
                + ", victims=" + this.victimCount
                + ", at=" + this.reportedAt
                + ", responders=" + this.responders.size() + "}";
    }

    /**
     * Identity equality on the immutable {@link #id}.
     *
     * @param other the object to compare with.
     * @return true if {@code other} is an {@code Incident} with the same id.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Incident)) {
            return false;
        }
        return this.id.equals(((Incident) other).id);
    }

    /**
     * @return a hash code consistent with {@link #equals(Object)} (based on {@link #id}).
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
