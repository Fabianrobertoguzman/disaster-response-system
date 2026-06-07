package edu.cqu.drs.protocol;

/**
 * Shared parameter-key constants for {@link Request} payloads, so the client and
 * the server never disagree on a key name (a single source of truth removes the
 * class of bugs where one tier writes {@code "incidentId"} and the other reads
 * {@code "incident_id"}).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class ProtocolKeys {

    /** Login username (String). */
    public static final String USERNAME = "username";

    /** Login password (String). */
    public static final String PASSWORD = "password";

    /** Hazard type of a submitted incident ({@link edu.cqu.drs.model.HazardType}). */
    public static final String HAZARD_TYPE = "hazardType";

    /** Latitude of a submitted incident (Double). */
    public static final String LATITUDE = "latitude";

    /** Longitude of a submitted incident (Double). */
    public static final String LONGITUDE = "longitude";

    /** Free-text description of a submitted incident (String). */
    public static final String DESCRIPTION = "description";

    /** Estimated victim count of a submitted incident (Integer). */
    public static final String VICTIM_COUNT = "victimCount";

    /** Domain id of an incident (UUID). */
    public static final String INCIDENT_ID = "incidentId";

    /** Domain id of a responder (UUID). */
    public static final String RESPONDER_ID = "responderId";

    /** Severity to apply when triaging ({@link edu.cqu.drs.model.Severity}). */
    public static final String SEVERITY = "severity";

    private ProtocolKeys() {
    }
}
