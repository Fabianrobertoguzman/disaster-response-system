package edu.cqu.drs.model;

/**
 * A vetted public-alert template whose message body is shaped to the Common Alerting Protocol
 * (CAP) v1.2 conventions used by Australian state emergency services.
 *
 * <p>Used by creative feature FR-CR-01 ({@code AlertTemplateRecommender}): when a Dispatcher
 * classifies an incident as CRITICAL, the recommender suggests one of these templates based on the
 * incident's hazard type, severity and status. (This enumeration provides the template <em>content</em>;
 * the actual broadcast through a CAP gateway is Assessment&nbsp;One's FR-09, which Assessment&nbsp;One
 * itself scopes to the external-interface gateway, not the desktop MVC application  -  so it is
 * out of scope for the prototype.)</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum AlertTemplate {

    /** Immediate evacuation notice  -  for critical hurricanes, floods and fires. */
    EVAC_NOTICE("EVACUATE NOW. Proceed immediately to the nearest designated assembly point. "
            + "Follow the directions of emergency services. Do not return until an ALL CLEAR is issued."),

    /** Shelter-in-place advisory  -  for high-severity hazardous-material and severe-storm events. */
    SHELTER_IN_PLACE("SHELTER IN PLACE. Close all windows and doors, turn off ventilation, stay indoors, "
            + "and monitor official channels. Do not leave until advised it is safe."),

    /** All-clear notice  -  issued once the incident is resolved. */
    ALL_CLEAR("ALL CLEAR. The hazard has been resolved. Normal activity may resume. "
            + "Report any further concerns to local authorities."),

    /** General advisory  -  the default when no more specific template applies. */
    GENERAL_ADVISORY("ADVISORY. An incident has been reported in your area. "
            + "Monitor official channels for updates and follow any instructions issued by emergency services.");

    /** The CAP-1.2-shaped message body for this template. */
    private final String capMessage;

    /**
     * Binds the message body to the template constant.
     *
     * @param capMessage the CAP-1.2-shaped public-alert text (never null).
     */
    AlertTemplate(String capMessage) {
        this.capMessage = capMessage;
    }

    /**
     * @return the CAP-1.2-shaped message body for broadcast via a public-alert channel.
     */
    public String getCapMessage() {
        return this.capMessage;
    }
}
