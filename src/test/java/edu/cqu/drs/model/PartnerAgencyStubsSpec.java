package edu.cqu.drs.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract specification for the eight concrete {@link IPartnerAgency} stubs:
 * each must report a non-blank name, a non-negative unit count, accept a
 * notification without throwing, and acknowledge it.
 */
class PartnerAgencyStubsSpec {

    private static Stream<IPartnerAgency> agencies() {
        return Stream.of(
                new FireEmergencyService(),
                new HospitalSystem(),
                new ElectricityUtility(),
                new TransportAuthority(),
                new PoliceService(),
                new WasteManagementService(),
                new WaterUtility(),
                new EducationDepartment());
    }

    @ParameterizedTest
    @MethodSource("agencies")
    void agencyHonoursTheContract(IPartnerAgency agency) {
        Incident incident = new Incident(HazardType.FLOOD,
                GpsCoordinate.captureCurrentLocation(), "rising water", 2);
        assertNotNull(agency.getAgencyName());
        assertFalse(agency.getAgencyName().trim().isEmpty());
        assertTrue(agency.availableUnits() >= 0);
        assertDoesNotThrow(() -> agency.notify(incident));
        assertTrue(agency.acknowledge(incident));
    }
}
