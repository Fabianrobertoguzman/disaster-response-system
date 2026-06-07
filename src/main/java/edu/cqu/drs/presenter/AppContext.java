package edu.cqu.drs.presenter;

import edu.cqu.drs.model.EducationDepartment;
import edu.cqu.drs.model.ElectricityUtility;
import edu.cqu.drs.model.FireEmergencyService;
import edu.cqu.drs.model.HospitalSystem;
import edu.cqu.drs.model.IPartnerAgency;
import edu.cqu.drs.model.IncidentQueue;
import edu.cqu.drs.model.PoliceService;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.TransportAuthority;
import edu.cqu.drs.model.WasteManagementService;
import edu.cqu.drs.model.WaterUtility;
import java.util.List;

/**
 * Process-wide application context: the in-memory state shared across the
 * prototype's views - the {@link IncidentQueue} (the citizen-report view
 * enqueues; the dispatcher console triages and dispatches), the fixed
 * {@link Responder} roster, and the {@link PartnerNotifier} over the eight
 * stubbed partner agencies (all of Assessment One's NFR-O04 named partners).
 *
 * <p>DRS-Initial has no database, so this singleton holds the application's
 * only shared state. It is a lazily-created, thread-safe singleton; a larger
 * system would inject the state through a DI container, but for a two-view
 * prototype a single holder keeps the wiring shorter.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class AppContext {

    /** The lazily-created sole instance. */
    private static AppContext instance;

    /** The shared incident queue, ordered most-urgent-first. */
    private final IncidentQueue incidentQueue;

    /**
     * The fixed roster of field responders available to the dispatcher (FR-05).
     */
    private final List<Responder> responderRoster;

    /** The partner-agency notifier over the eight stubbed agencies (NFR-O04). */
    private final PartnerNotifier partnerNotifier;

    /**
     * Creates the context: an empty incident queue, a fixed six-strong
     * responder roster, and the partner notifier over the eight stub agencies.
     */
    private AppContext() {
        this.incidentQueue = new IncidentQueue();
        this.responderRoster = List.of(
                new Responder("Unit Alpha"),
                new Responder("Unit Bravo"),
                new Responder("Unit Charlie"),
                new Responder("Unit Delta"),
                new Responder("Unit Echo"),
                new Responder("Unit Foxtrot"));
        List<IPartnerAgency> stubbedAgencies = List.of(
                new FireEmergencyService(),
                new HospitalSystem(),
                new ElectricityUtility(),
                new TransportAuthority(),
                new PoliceService(),
                new WasteManagementService(),
                new WaterUtility(),
                new EducationDepartment());
        this.partnerNotifier = new PartnerNotifier(stubbedAgencies);
    }

    /**
     * Returns the sole {@code AppContext} instance, creating it on first call.
     *
     * @return the shared application context (never null).
     */
    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    /**
     * Returns the shared incident queue.
     *
     * @return the in-memory, most-urgent-first incident queue.
     */
    public IncidentQueue getIncidentQueue() {
        return this.incidentQueue;
    }

    /**
     * Returns the fixed roster of field responders.
     *
     * @return an unmodifiable list of the field responders.
     */
    public List<Responder> getResponderRoster() {
        return this.responderRoster;
    }

    /**
     * Returns the partner-agency notifier.
     *
     * @return the notifier over the eight stubbed partner agencies.
     */
    public PartnerNotifier getPartnerNotifier() {
        return this.partnerNotifier;
    }

    /**
     * Custom display form for log output and debugging.
     *
     * @return e.g. {@code "AppContext{queueSize=3, responders=6, agencies=8}"}.
     */
    @Override
    public String toString() {
        return "AppContext{queueSize=" + this.incidentQueue.size()
                + ", responders=" + this.responderRoster.size()
                + ", agencies=" + this.partnerNotifier.getAgencies().size()
                + "}";
    }
}
