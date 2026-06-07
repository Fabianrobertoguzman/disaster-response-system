package edu.cqu.drs.model;

/**
 * Stub for the Hospital System partner agency (one of the eight agencies named
 * in Assessment One's NFR-O04).
 *
 * <p>One of eight differentiated {@link IPartnerAgency} stubs in DRS-Initial.
 * In a real deployment this class would wrap the hospital network's
 * bed-management and ambulance-dispatch APIs; here {@link #notify(Incident)}
 * is a no-op (the notification is recorded by {@code PartnerNotifier}) and
 * {@link #acknowledge(Incident)} always succeeds.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class HospitalSystem implements IPartnerAgency {

    /** Human-readable agency name shown on the Dispatcher console. */
    private static final String NAME = "Hospital System";

    /** Hardcoded count of available beds (a prototype constant). */
    private static final int AVAILABLE_UNITS = 40;

    /** Creates the stub agency (it has no mutable state). */
    public HospitalSystem() {
        // No state to initialise.
    }

    /**
     * {@inheritDoc} For this stub the notification is a no-op; the real dispatch
     * push is an Assessment-3 integration.
     */
    @Override
    public void notify(Incident incident) {
        // A real implementation would push incident details to the hospital
        // network's notification channel here.
    }

    /**
     * {@inheritDoc} This stub always acknowledges.
     */
    @Override
    public boolean acknowledge(Incident incident) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getAgencyName() {
        return NAME;
    }

    /** {@inheritDoc} */
    @Override
    public int availableUnits() {
        return AVAILABLE_UNITS;
    }

    /**
     * Compact diagnostic line used in the partner-agency log.
     *
     * @return e.g. {@code "Hospital System (40 units)"}.
     */
    @Override
    public String toString() {
        return NAME + " (" + AVAILABLE_UNITS + " units)";
    }
}
