package edu.cqu.drs.model;

/**
 * Stub for the Education Department partner agency (one of the eight agencies
 * named in Assessment One's NFR-O04; schools are used as evacuation shelters).
 *
 * <p>One of eight differentiated {@link IPartnerAgency} stubs in DRS-Initial.
 * In a real deployment this class would wrap the department's shelter-roster
 * and student-evacuation APIs; here {@link #notify(Incident)} is a no-op (the
 * notification is recorded by {@code PartnerNotifier}) and
 * {@link #acknowledge(Incident)} always succeeds.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class EducationDepartment implements IPartnerAgency {

    /** Human-readable agency name shown on the Dispatcher console. */
    private static final String NAME = "Education Department";

    /** Hardcoded count of schools available as evacuation shelters. */
    private static final int AVAILABLE_UNITS = 25;

    /** Creates the stub agency (it has no mutable state). */
    public EducationDepartment() {
        // No state to initialise.
    }

    /**
     * {@inheritDoc} For this stub the notification is a no-op; the real dispatch
     * push is an Assessment-3 integration.
     */
    @Override
    public void notify(Incident incident) {
        // A real implementation would push incident details to the department's
        // shelter-roster channel here.
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
     * Hand-written format required by the Assessment 2 specification section 3.
     *
     * @return e.g. {@code "Education Department (25 units)"}.
     */
    @Override
    public String toString() {
        return NAME + " (" + AVAILABLE_UNITS + " units)";
    }
}
