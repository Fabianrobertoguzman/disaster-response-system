package edu.cqu.drs.model;

/**
 * Stub for the Police Service (law enforcement) partner agency (one of the
 * eight agencies named in Assessment One's NFR-O04).
 *
 * <p>One of eight differentiated {@link IPartnerAgency} stubs in DRS-Initial.
 * In a real deployment this class would wrap the service's patrol-dispatch and
 * cordon-management APIs; here {@link #notify(Incident)} is a no-op (the
 * notification is recorded by {@code PartnerNotifier}) and
 * {@link #acknowledge(Incident)} always succeeds.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class PoliceService implements IPartnerAgency {

    /** Human-readable agency name shown on the Dispatcher console. */
    private static final String NAME = "Police Service";

    /** Hardcoded count of available patrols (a prototype constant). */
    private static final int AVAILABLE_UNITS = 15;

    /** Creates the stub agency (it has no mutable state). */
    public PoliceService() {
        // No state to initialise.
    }

    /**
     * {@inheritDoc} For this stub the notification is a no-op; the real dispatch
     * push is an Assessment-3 integration.
     */
    @Override
    public void notify(Incident incident) {
        // A real implementation would push incident details to the service's
        // patrol-dispatch channel here.
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
     * Format string required by Assessment 2's section-3 coding mandate.
     *
     * @return e.g. {@code "Police Service (15 units)"}.
     */
    @Override
    public String toString() {
        return NAME + " (" + AVAILABLE_UNITS + " units)";
    }
}
