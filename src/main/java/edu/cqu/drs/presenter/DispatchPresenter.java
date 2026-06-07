package edu.cqu.drs.presenter;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentQueue;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import java.util.List;

/**
 * Coordinates the dispatcher's triage, responder allocation, alert-template
 * recommendation and resolution actions over the shared incident queue.
 *
 * <p>The presenter is the business-tier bridge between the dispatcher console
 * ({@link edu.cqu.drs.view.DispatchController}) and the domain model. Because a
 * {@link java.util.PriorityQueue} fixes an element's position at insertion
 * time, {@link #triage(Incident, Severity)} re-positions an incident by
 * removing it, changing its severity, and re-enqueuing it, so the queue stays
 * correctly ordered most-urgent-first after a severity change.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DispatchPresenter {

    /**
     * Indicative response-time budget for a triage action, in milliseconds
     * (Assessment One's NFR-P01: a prioritised response within 2 seconds). The
     * in-process triage here is microseconds; this constant lets the test plan
     * assert the budget is honoured. The networked half of the SLO is deferred
     * to Assessment 3.
     */
    public static final long EXPECTED_LATENCY_MS = 2000L;

    /** The shared, most-urgent-first incident queue. */
    private final IncidentQueue incidentQueue;

    /**
     * The rule-driven alert-template recommender (creative feature FR-CR-01).
     */
    private final AlertTemplateRecommender recommender;

    /**
     * Creates a presenter over the given incident queue.
     *
     * @param incidentQueue the shared incident queue (never null).
     * @throws IllegalArgumentException if {@code incidentQueue} is null.
     */
    public DispatchPresenter(IncidentQueue incidentQueue) {
        if (incidentQueue == null) {
            throw new IllegalArgumentException(
                    "incidentQueue must not be null");
        }
        this.incidentQueue = incidentQueue;
        this.recommender = new AlertTemplateRecommender();
    }

    /**
     * Returns the incidents currently in the queue, ordered most-urgent-first.
     * The returned list is a snapshot; mutating it does not affect the queue.
     *
     * @return the ordered snapshot of incidents in the queue.
     */
    public List<Incident> pendingIncidents() {
        return this.incidentQueue.asSortedList();
    }

    /**
     * Returns the number of incidents in the queue.
     *
     * @return the queue size.
     */
    public int pendingCount() {
        return this.incidentQueue.size();
    }

    /**
     * Sets an incident's severity and re-positions it in the queue. The
     * incident is removed, its severity changed (which also moves it to
     * {@code TRIAGED}), and it is re-enqueued so the ordering reflects the new
     * severity.
     *
     * @param incident the incident to triage (never null).
     * @param severity the severity to apply (never null).
     * @throws IllegalArgumentException if either argument is null.
     */
    public void triage(Incident incident, Severity severity) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        this.incidentQueue.remove(incident);
        incident.triage(severity);
        this.incidentQueue.enqueue(incident);
    }

    /**
     * Allocates a responder to an incident (FR-05). Delegates to
     * {@link Incident#assignResponder(Responder)}, which records the allocation
     * and marks the responder tasked.
     *
     * @param incident  the incident to staff (never null).
     * @param responder the responder to allocate (never null).
     * @throws IllegalArgumentException if either argument is null, or the
     *         responder is already allocated to this incident.
     * @throws IllegalStateException if the incident already has the maximum
     *         number of responders.
     */
    public void assignResponder(Incident incident, Responder responder) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        if (responder == null) {
            throw new IllegalArgumentException("responder must not be null");
        }
        incident.assignResponder(responder);
    }

    /**
     * Recommends a CAP-1.2 alert template for an incident (creative feature
     * FR-CR-01) and records it on the incident.
     *
     * @param incident the incident to recommend a template for (never null).
     * @return the recommended {@link AlertTemplate} (never null).
     * @throws IllegalArgumentException if {@code incident} is null.
     */
    public AlertTemplate recommendTemplate(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        AlertTemplate template = this.recommender.recommend(incident);
        incident.setRecommendedTemplate(template);
        return template;
    }

    /**
     * Marks an incident resolved. The incident stays in the queue (so it is
     * still visible to the dispatcher) with status {@code RESOLVED}.
     *
     * @param incident the incident to resolve (never null).
     * @throws IllegalArgumentException if {@code incident} is null.
     */
    public void resolve(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        incident.resolve();
    }

    /**
     * Summary line for debug output (queue size and NFR-P01 budget).
     *
     * @return e.g. {@code "DispatchPresenter{queueSize=3, sloMs=2000}"}.
     */
    @Override
    public String toString() {
        return "DispatchPresenter{queueSize=" + this.incidentQueue.size()
                + ", sloMs=" + EXPECTED_LATENCY_MS + "}";
    }
}
