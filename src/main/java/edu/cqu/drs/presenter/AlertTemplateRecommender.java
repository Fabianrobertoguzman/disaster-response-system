package edu.cqu.drs.presenter;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;
import java.util.EnumSet;
import java.util.Set;

/**
 * Rule-driven recommender for a CAP-1.2 public-alert template (creative feature
 * FR-CR-01).
 *
 * <p>Given an {@link Incident}, {@link #recommend(Incident)} picks one of the
 * four {@link AlertTemplate}s from a small, transparent rule keyed on the
 * incident's status, severity and hazard type:</p>
 * <ul>
 *   <li>a resolved incident -> {@code ALL_CLEAR};</li>
 *   <li>a CRITICAL hurricane, flood or fire -> {@code EVAC_NOTICE};</li>
 *   <li>a HIGH or CRITICAL hazmat or severe-storm event ->
 *       {@code SHELTER_IN_PLACE};</li>
 *   <li>anything else -> {@code GENERAL_ADVISORY}.</li>
 * </ul>
 *
 * <p>The recommendation is advisory - the Dispatcher console records it on the
 * incident and displays it; the actual CAP broadcast (Assessment One's FR-09)
 * is out of scope for the prototype. Earthquakes have no vetted template in
 * this rule set, so they fall through to {@code GENERAL_ADVISORY}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AlertTemplateRecommender {

    /** Hazard types whose CRITICAL incidents warrant immediate evacuation. */
    private static final Set<HazardType> EVACUATION_HAZARDS =
            EnumSet.of(HazardType.HURRICANE, HazardType.FLOOD, HazardType.FIRE);

    /** Hazard types whose HIGH+ incidents warrant sheltering in place. */
    private static final Set<HazardType> SHELTER_HAZARDS =
            EnumSet.of(HazardType.HAZMAT, HazardType.STORM);

    /** Creates a recommender (it has no mutable state; the rule is fixed). */
    public AlertTemplateRecommender() {
        // No state to initialise.
    }

    /**
     * Recommends the most appropriate alert template for an incident.
     *
     * @param incident the incident to recommend a template for (never null).
     * @return the recommended {@link AlertTemplate} (never null).
     * @throws IllegalArgumentException if {@code incident} is null.
     */
    public AlertTemplate recommend(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            return AlertTemplate.ALL_CLEAR;
        }
        HazardType hazard = incident.getHazardType();
        Severity severity = incident.getSeverity();
        boolean critical = severity == Severity.CRITICAL;
        if (critical && EVACUATION_HAZARDS.contains(hazard)) {
            return AlertTemplate.EVAC_NOTICE;
        }
        boolean highOrCritical = critical || severity == Severity.HIGH;
        if (highOrCritical && SHELTER_HAZARDS.contains(hazard)) {
            return AlertTemplate.SHELTER_IN_PLACE;
        }
        return AlertTemplate.GENERAL_ADVISORY;
    }

    /**
     * Debug-friendly summary of the rule set's hazard partitions.
     *
     * @return e.g. {@code "AlertTemplateRecommender{evacuationHazards=3, shelterHazards=2}"}.
     */
    @Override
    public String toString() {
        return "AlertTemplateRecommender{evacuationHazards="
                + EVACUATION_HAZARDS.size()
                + ", shelterHazards=" + SHELTER_HAZARDS.size() + "}";
    }
}
