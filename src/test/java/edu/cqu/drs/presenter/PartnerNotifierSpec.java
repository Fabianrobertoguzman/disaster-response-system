package edu.cqu.drs.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.cqu.drs.model.EducationDepartment;
import edu.cqu.drs.model.ElectricityUtility;
import edu.cqu.drs.model.FireEmergencyService;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.HospitalSystem;
import edu.cqu.drs.model.IPartnerAgency;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.PoliceService;
import edu.cqu.drs.model.TransportAuthority;
import edu.cqu.drs.model.WasteManagementService;
import edu.cqu.drs.model.WaterUtility;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioural specification for {@link PartnerNotifier} - the fan-out of an
 * incident notification to the registered partner agencies.
 */
class PartnerNotifierSpec {

    private PartnerNotifier notifier;
    private Incident incident;

    @BeforeEach
    void setUp() {
        this.notifier = new PartnerNotifier(List.of(
                new FireEmergencyService(),
                new HospitalSystem(),
                new ElectricityUtility(),
                new TransportAuthority(),
                new PoliceService(),
                new WasteManagementService(),
                new WaterUtility(),
                new EducationDepartment()));
        this.incident = new Incident(HazardType.FIRE,
                GpsCoordinate.captureCurrentLocation(), "structure fire", 5);
    }

    @Test
    @DisplayName("notifyAll notifies every agency and collects acknowledgements")
    void shouldNotifyAllAgenciesAndCollectAcknowledgements() {
        Map<String, Boolean> acks = this.notifier.notifyAll(this.incident);
        assertEquals(8, acks.size());
        assertTrue(acks.values().stream().allMatch(b -> b));
        assertTrue(this.notifier.getNotificationLog().size() >= 8);
    }

    @Test
    @DisplayName("agencies are ordered highest-available-units first")
    void shouldOrderAgenciesByAvailabilityDescending() {
        List<IPartnerAgency> ordered = this.notifier.getAgencies();
        assertEquals(8, ordered.size());
        assertEquals("Hospital System", ordered.get(0).getAgencyName());
        assertEquals("Electricity Utility", ordered.get(7).getAgencyName());
    }

    @Test
    @DisplayName("an agency that does not acknowledge first time is retried")
    void shouldRetryAgencyThatDoesNotAcknowledge() {
        FlakyAgency flaky = new FlakyAgency();
        PartnerNotifier withFlaky = new PartnerNotifier(List.of(flaky));
        Map<String, Boolean> acks = withFlaky.notifyAll(this.incident);
        assertTrue(acks.get("Flaky Agency"));
        assertEquals(2, flaky.notifyCalls);
        assertTrue(withFlaky.getNotificationLog().getEntries().stream()
                .anyMatch(entry -> entry.contains("Retry")));
    }

    @Test
    @DisplayName("describeAgencies returns one line per agency")
    void shouldDescribeAgencies() {
        List<String> lines = this.notifier.describeAgencies();
        assertEquals(8, lines.size());
        assertTrue(lines.get(0).contains("Hospital System"));
        assertTrue(lines.get(0).contains("units available"));
    }

    @Test
    @DisplayName("constructor rejects a null agency list")
    void shouldRejectNullAgenciesInConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new PartnerNotifier(null));
    }

    @Test
    @DisplayName("notifyAll rejects a null incident")
    void shouldRejectNullIncidentInNotifyAll() {
        assertThrows(IllegalArgumentException.class,
                () -> this.notifier.notifyAll(null));
    }

    @Test
    @DisplayName("getNotificationLog is never null")
    void shouldExposeNotificationLog() {
        assertNotNull(this.notifier.getNotificationLog());
    }

    /**
     * A test-only agency that fails its first acknowledgement and succeeds on
     * the second, exercising {@link PartnerNotifier}'s retry path.
     */
    private static final class FlakyAgency implements IPartnerAgency {

        private int notifyCalls;
        private int acknowledgeCalls;

        @Override
        public void notify(Incident incidentArg) {
            this.notifyCalls++;
        }

        @Override
        public boolean acknowledge(Incident incidentArg) {
            this.acknowledgeCalls++;
            return this.acknowledgeCalls >= 2;
        }

        @Override
        public String getAgencyName() {
            return "Flaky Agency";
        }

        @Override
        public int availableUnits() {
            return 1;
        }
    }
}
