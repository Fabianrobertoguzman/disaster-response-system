package edu.cqu.drs.presenter;

import edu.cqu.drs.model.AuditLog;
import edu.cqu.drs.model.IPartnerAgency;
import edu.cqu.drs.model.Incident;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fans an incident notification out to every registered partner agency and
 * collects their acknowledgements (Assessment One's NFR-O04: coordinate with
 * up to eight named partner agencies).
 *
 * <p>Agencies are ordered highest-available-units first (ties broken by name),
 * so the agency with the most capacity is contacted first. For each agency the
 * notifier calls {@link IPartnerAgency#notify(Incident)} then
 * {@link IPartnerAgency#acknowledge(Incident)}; if the agency does not
 * acknowledge, it is retried once (the NFR-P03/NFR-P04 graceful-degradation
 * path). Every notification is recorded both in the incident's own
 * {@link AuditLog} (the broadcast side of FR-14) and in this notifier's
 * cross-incident log; the result is an acknowledgement map (agency name to
 * acknowledged?). New agencies are added by registering another
 * {@link IPartnerAgency}; the notifier itself does not change.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class PartnerNotifier {

    /**
     * Registered agencies, ordered highest-available-units first, then by name.
     */
    private final List<IPartnerAgency> agencies;

    /** Cross-incident append-only log of notification and retry events. */
    private final AuditLog notificationLog;

    /**
     * Creates a notifier over the given partner agencies.
     *
     * @param agencies the partner agencies to register (never null; a copy is
     *                 taken and sorted; an empty list is permitted).
     * @throws IllegalArgumentException if {@code agencies} is null.
     */
    public PartnerNotifier(List<IPartnerAgency> agencies) {
        if (agencies == null) {
            throw new IllegalArgumentException("agencies must not be null");
        }
        List<IPartnerAgency> ordered = new ArrayList<>(agencies);
        ordered.sort(Comparator.comparingInt(IPartnerAgency::availableUnits)
                .reversed()
                .thenComparing(IPartnerAgency::getAgencyName));
        this.agencies = Collections.unmodifiableList(ordered);
        this.notificationLog = new AuditLog();
    }

    /**
     * Returns the registered agencies, in notification order (highest available
     * units first, then by name).
     *
     * @return an unmodifiable, ordered view of the registered agencies.
     */
    public List<IPartnerAgency> getAgencies() {
        return this.agencies;
    }

    /**
     * Returns the cross-incident append-only log of notification and retry
     * events.
     *
     * @return the notification audit log.
     */
    public AuditLog getNotificationLog() {
        return this.notificationLog;
    }

    /**
     * Notifies every registered agency of the incident, retrying once any
     * agency that does not acknowledge, and records each notification in both
     * the incident's audit log and this notifier's log.
     *
     * @param incident the incident to broadcast (never null).
     * @return a map from agency name to whether that agency acknowledged
     *         (insertion order is the notification order).
     * @throws IllegalArgumentException if {@code incident} is null.
     */
    public Map<String, Boolean> notifyAll(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        Map<String, Boolean> acknowledgements = new LinkedHashMap<>();
        for (IPartnerAgency agency : this.agencies) {
            agency.notify(incident);
            boolean acknowledged = agency.acknowledge(incident);
            if (!acknowledged) {
                this.notificationLog.record("Retry: " + agency.getAgencyName()
                        + " did not acknowledge incident " + incident.getId());
                agency.notify(incident);
                acknowledged = agency.acknowledge(incident);
            }
            this.notificationLog.record("Notified " + agency.getAgencyName()
                    + " of incident " + incident.getId()
                    + "; acknowledged=" + acknowledged);
            incident.getAuditLog().record("Partner notified: "
                    + agency.getAgencyName()
                    + " (acknowledged=" + acknowledged + ")");
            acknowledgements.put(agency.getAgencyName(), acknowledged);
        }
        return acknowledgements;
    }

    /**
     * Returns one human-readable line per registered agency (name and available
     * units), in notification order - for display on the Dispatcher console.
     *
     * @return a list of agency description lines.
     */
    public List<String> describeAgencies() {
        List<String> lines = new ArrayList<>();
        for (IPartnerAgency agency : this.agencies) {
            lines.add(agency.getAgencyName() + " - " + agency.availableUnits()
                    + " units available");
        }
        return lines;
    }

    /**
     * Diagnostic summary of agency count and notification-log size.
     *
     * @return e.g. {@code "PartnerNotifier{agencies=8, log=17 entries}"}.
     */
    @Override
    public String toString() {
        return "PartnerNotifier{agencies=" + this.agencies.size()
                + ", log=" + this.notificationLog.size() + " entries}";
    }
}
