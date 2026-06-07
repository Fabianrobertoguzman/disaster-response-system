package edu.cqu.drs.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioural specification for {@link AlertTemplateRecommender} - the rule that
 * picks a CAP-1.2 alert template for an incident (creative feature FR-CR-01).
 */
class AlertTemplateRecommenderSpec {

    private AlertTemplateRecommender recommender;

    @BeforeEach
    void setUp() {
        this.recommender = new AlertTemplateRecommender();
    }

    private Incident incident(HazardType hazard) {
        return new Incident(hazard, GpsCoordinate.captureCurrentLocation(),
                "rule-test incident", 1);
    }

    @Test
    @DisplayName("a resolved incident is recommended ALL_CLEAR")
    void shouldRecommendAllClearForResolvedIncident() {
        Incident incident = incident(HazardType.FIRE);
        incident.resolve();
        assertEquals(AlertTemplate.ALL_CLEAR,
                this.recommender.recommend(incident));
    }

    @Test
    @DisplayName("a critical fire/flood/hurricane is recommended EVAC_NOTICE")
    void shouldRecommendEvacuationForCriticalEvacuationHazard() {
        Incident fire = incident(HazardType.FIRE);
        fire.triage(Severity.CRITICAL);
        assertEquals(AlertTemplate.EVAC_NOTICE,
                this.recommender.recommend(fire));
        Incident flood = incident(HazardType.FLOOD);
        flood.triage(Severity.CRITICAL);
        assertEquals(AlertTemplate.EVAC_NOTICE,
                this.recommender.recommend(flood));
    }

    @Test
    @DisplayName("a high/critical hazmat or storm gets SHELTER_IN_PLACE")
    void shouldRecommendShelterForHighShelterHazard() {
        Incident hazmat = incident(HazardType.HAZMAT);
        hazmat.triage(Severity.HIGH);
        assertEquals(AlertTemplate.SHELTER_IN_PLACE,
                this.recommender.recommend(hazmat));
        Incident storm = incident(HazardType.STORM);
        storm.triage(Severity.CRITICAL);
        assertEquals(AlertTemplate.SHELTER_IN_PLACE,
                this.recommender.recommend(storm));
    }

    @Test
    @DisplayName("a medium incident with no special rule gets GENERAL_ADVISORY")
    void shouldRecommendGeneralAdvisoryByDefault() {
        // A freshly reported incident (MEDIUM, REPORTED) - no special rule.
        assertEquals(AlertTemplate.GENERAL_ADVISORY,
                this.recommender.recommend(incident(HazardType.FIRE)));
        // A critical earthquake - not in either special hazard set.
        Incident quake = incident(HazardType.EARTHQUAKE);
        quake.triage(Severity.CRITICAL);
        assertEquals(AlertTemplate.GENERAL_ADVISORY,
                this.recommender.recommend(quake));
    }

    @Test
    @DisplayName("recommend rejects a null incident")
    void shouldRejectNullIncident() {
        assertThrows(IllegalArgumentException.class,
                () -> this.recommender.recommend(null));
    }
}
