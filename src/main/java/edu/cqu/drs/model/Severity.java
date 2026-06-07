package edu.cqu.drs.model;

/**
 * Incident triage severity, with an ordinal priority used to order the {@link IncidentQueue}.
 *
 * <p>The four classifications are taken verbatim from Assessment&nbsp;One's functional requirement
 * FR-04 ("a severity classification of Critical, High, Medium or Low"). Each constant carries an
 * integer {@code priority} (higher&nbsp;=&nbsp;more urgent) so that prioritised dispatching is a
 * data lookup  -  {@code incident.getSeverity().getPriority()}  -  rather than a
 * {@code switch} on a discriminator, which keeps the dispatch logic free of the
 * "switch on type code" code smell.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum Severity {

    /** Lowest urgency. */
    LOW(1),

    /** Moderate urgency. */
    MEDIUM(2),

    /** High urgency. */
    HIGH(3),

    /** Highest urgency  -  life-critical, dispatched first. */
    CRITICAL(4);

    /** Ordinal priority: higher means more urgent; used by {@link IncidentQueue}'s comparator. */
    private final int priority;

    /**
     * Binds the ordinal priority to the constant.
     *
     * @param priority the urgency rank (1 = LOW ... 4 = CRITICAL).
     */
    Severity(int priority) {
        this.priority = priority;
    }

    /**
     * @return the ordinal priority of this severity (higher&nbsp;=&nbsp;more urgent).
     */
    public int getPriority() {
        return this.priority;
    }
}
