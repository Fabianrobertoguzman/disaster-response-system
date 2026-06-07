package edu.cqu.drs.model;

/**
 * Stub for the Fire and Emergency Service partner agency (one of the eight
 * agencies named in Assessment One's NFR-O04).
 *
 * <p>One of eight differentiated {@link IPartnerAgency} stubs in DRS-Initial.
 * In a real deployment this class would wrap the agency's dispatch API; here
 * {@link #notify(Incident)} is a no-op (the notification is recorded by
 * {@code PartnerNotifier}) and {@link #acknowledge(Incident)} always succeeds.
 * Adding a ninth agency is just one more class implementing
 * {@link IPartnerAgency}, with no change to the notifier.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class FireEmergencyService implements IPartnerAgency {

    /** Human-readable agency name shown on the Dispatcher console. */
    private static final String NAME = "Fire and Emergency Service";

    /** Hardcoded count of available appliances (a prototype constant). */
    private static final int AVAILABLE_UNITS = 12;

    /** Creates the stub agency (it has no mutable state). */
    public FireEmergencyService() {
        // No state to initialise.
    }

    /**
     * {@inheritDoc} For this stub the notification is a no-op; the real dispatch
     * push is an Assessment-3 integration.
     */
    @Override
    public void notify(Incident incident) {
        // A real implementation would push incident details to the agency's
        // notification channel here.
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
     * Compact diagnostic format used by the dispatcher console.
     *
     * @return e.g. {@code "Fire and Emergency Service (12 units)"}.
     */
    @Override
    public String toString() {
        return NAME + " (" + AVAILABLE_UNITS + " units)";
    }
}
