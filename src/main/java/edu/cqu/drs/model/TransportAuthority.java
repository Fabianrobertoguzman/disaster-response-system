package edu.cqu.drs.model;

/**
 * Stub for the Transport Authority partner agency (one of the eight agencies
 * named in Assessment One's NFR-O04).
 *
 * <p>One of eight differentiated {@link IPartnerAgency} stubs in DRS-Initial.
 * In a real deployment this class would wrap the authority's road-closure and
 * traffic-management APIs; here {@link #notify(Incident)} is a no-op (the
 * notification is recorded by {@code PartnerNotifier}) and
 * {@link #acknowledge(Incident)} always succeeds.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class TransportAuthority implements IPartnerAgency {

    /** Human-readable agency name shown on the Dispatcher console. */
    private static final String NAME = "Transport Authority";

    /** Hardcoded count of available traffic-management units (prototype). */
    private static final int AVAILABLE_UNITS = 8;

    /** Creates the stub agency (it has no mutable state). */
    public TransportAuthority() {
        // No state to initialise.
    }

    /**
     * {@inheritDoc} For this stub the notification is a no-op; the real dispatch
     * push is an Assessment-3 integration.
     */
    @Override
    public void notify(Incident incident) {
        // A real implementation would push incident details to the authority's
        // traffic-management channel here.
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
     * Compact diagnostic format with the agency name and unit count.
     *
     * @return e.g. {@code "Transport Authority (8 units)"}.
     */
    @Override
    public String toString() {
        return NAME + " (" + AVAILABLE_UNITS + " units)";
    }
}
