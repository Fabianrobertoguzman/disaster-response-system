package edu.cqu.drs.model;

/**
 * Contract for an external partner agency the DRS coordinates with when responding to an incident.
 *
 * <p>Assessment One's NFR-O04 names eight such agencies: Fire and Emergency Service,
 * Hospital System, Electricity Utility, Transport Authority, Waste Management Service, Water Utility,
 * Education Department (Schools) and Police Service (Law Enforcement). The prototype implements all
 * eight of them as differentiated stubs, fully realising NFR-O04. Adding a ninth agency is a single
 * new class implementing this interface plus one line registering it with {@code PartnerNotifier};
 * the notifier itself does not change.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface IPartnerAgency {

    /**
     * Pushes the incident's details to this agency over the agency's notification channel.
     *
     * @param incident the incident being coordinated (never null).
     */
    void notify(Incident incident);

    /**
     * Records the agency's acknowledgement of a prior {@link #notify(Incident)}.
     *
     * @param incident the incident the acknowledgement relates to (never null).
     * @return true if the agency acknowledged within its service-level objective; false if it did not
     *         (which the caller treats as a "retry needed" condition  -  the NFR-P03/NFR-P04
     *         graceful-degradation path).
     */
    boolean acknowledge(Incident incident);

    /**
     * @return a human-readable agency name for display on the Dispatcher console.
     */
    String getAgencyName();

    /**
     * @return a count of this agency's currently-available response units (beds, appliances,
     *         patrols, crews, etc. depending on the agency). Used by {@code PartnerNotifier} to
     *         fan notifications out highest-availability-first. (Hardcoded per stub in the prototype.)
     */
    int availableUnits();
}
