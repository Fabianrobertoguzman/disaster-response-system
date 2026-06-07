package edu.cqu.drs.presenter;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentQueue;

/**
 * Coordinates filing a citizen disaster report.
 *
 * <p>The presenter sits between the citizen-report view
 * ({@link edu.cqu.drs.view.ReportController}) and the domain model: it builds an
 * {@link Incident} from the captured inputs and enqueues it on the shared
 * {@link IncidentQueue} so the dispatcher console can triage it. Keeping this
 * step out of the FXML controller follows the MVP split (controller handles
 * JavaFX, presenter handles the use case) and makes the use case unit-testable
 * without a running JavaFX toolkit.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ReportPresenter {

    /** The shared, most-urgent-first incident queue that filed reports join. */
    private final IncidentQueue incidentQueue;

    /**
     * Creates a presenter over the given incident queue.
     *
     * @param incidentQueue the shared incident queue (never null).
     * @throws IllegalArgumentException if {@code incidentQueue} is null.
     */
    public ReportPresenter(IncidentQueue incidentQueue) {
        if (incidentQueue == null) {
            throw new IllegalArgumentException("incidentQueue must not be null");
        }
        this.incidentQueue = incidentQueue;
    }

    /**
     * Files a new incident from a citizen report and enqueues it for triage.
     *
     * @param hazardType  the reported hazard type (never null).
     * @param location    the captured report location (never null).
     * @param description the citizen's description (null is treated as empty by
     *                    the model).
     * @param victimCount the estimated number of people affected (>= 0).
     * @return the newly created and enqueued {@link Incident}.
     * @throws IllegalArgumentException if the model rejects the inputs.
     */
    public Incident submitIncident(HazardType hazardType, GpsCoordinate location,
            String description, int victimCount) {
        Incident incident =
                new Incident(hazardType, location, description, victimCount);
        this.incidentQueue.enqueue(incident);
        return incident;
    }

    /**
     * Returns the number of incidents currently awaiting triage.
     *
     * @return the size of the shared incident queue.
     */
    public int queueSize() {
        return this.incidentQueue.size();
    }

    /**
     * Compact summary used in debug logs.
     *
     * @return e.g. {@code "ReportPresenter{queueSize=3}"}.
     */
    @Override
    public String toString() {
        return "ReportPresenter{queueSize=" + this.incidentQueue.size() + "}";
    }
}
