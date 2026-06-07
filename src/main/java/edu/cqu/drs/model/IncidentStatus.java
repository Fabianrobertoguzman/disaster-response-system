package edu.cqu.drs.model;

/**
 * The lifecycle state of an {@link Incident} within the DRS-Initial prototype.
 *
 * <p>Assessment&nbsp;One's full incident lifecycle has more states
 * ({@code REPORTED -> TRIAGED -> ALLOCATED -> EN_ROUTE -> ON_SCENE -> CONTAINED
 * -> CLEARED -> RESOLVED}, plus {@code ESCALATED} and {@code NEEDS_SUPPORT}); the Stage-2
 * prototype models the three states it actually exercises  -  report, triage, resolve  - 
 * with the intermediate operational states deferred to Stage&nbsp;3. The simplification is a
 * documented scope decision (see the report's reflection section).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum IncidentStatus {

    /** Just submitted by a Citizen; awaiting Dispatcher triage. */
    REPORTED,

    /** A Dispatcher has assigned a severity classification (FR-04). */
    TRIAGED,

    /** The incident has been closed out. */
    RESOLVED
}
