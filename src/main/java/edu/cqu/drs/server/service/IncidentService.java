package edu.cqu.drs.server.service;

import edu.cqu.drs.data.AuditDao;
import edu.cqu.drs.data.AuditEntry;
import edu.cqu.drs.data.IncidentDao;
import edu.cqu.drs.data.ResponderDao;
import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.presenter.AlertTemplateRecommender;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Server-side business service for the incident lifecycle - the tier the A2
 * dispatcher/report presenters became once the system was split into client and
 * server. It coordinates the domain model and persists every change through the
 * {@link IncidentDao} / {@link ResponderDao} interfaces (a MySQL implementation
 * in production, an in-memory one under test), recording each mutating action in
 * the audit trail through {@link AuditDao} (non-repudiation, Assessment One
 * FR-14).
 *
 * <p>The service depends only on DAO <em>interfaces</em>, so it is unit-testable
 * without a database and runs unchanged against either backend.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class IncidentService {

    /** Most-urgent-first: severity priority descending, then victim count descending. */
    private static final Comparator<Incident> MOST_URGENT_FIRST =
            Comparator.comparingInt((Incident incident) -> incident.getSeverity().getPriority())
                    .thenComparingInt(Incident::getVictimCount)
                    .reversed();

    private final IncidentDao incidentDao;
    private final ResponderDao responderDao;
    private final AuditDao auditDao;
    private final AlertTemplateRecommender recommender;

    /**
     * Creates the service over its data-access dependencies.
     *
     * @param incidentDao  the incident store (must not be null).
     * @param responderDao the responder store (must not be null).
     * @param auditDao     the audit trail (must not be null).
     * @param recommender  the alert-template recommender (must not be null).
     * @throws IllegalArgumentException if any dependency is null.
     */
    public IncidentService(IncidentDao incidentDao, ResponderDao responderDao,
            AuditDao auditDao, AlertTemplateRecommender recommender) {
        if (incidentDao == null || responderDao == null || auditDao == null
                || recommender == null) {
            throw new IllegalArgumentException("all dependencies are required");
        }
        this.incidentDao = incidentDao;
        this.responderDao = responderDao;
        this.auditDao = auditDao;
        this.recommender = recommender;
    }

    /**
     * Files a new incident from a citizen report and persists it.
     *
     * @param hazardType  the reported hazard (must not be null).
     * @param location    the captured location (must not be null).
     * @param description the free-text description (null becomes "").
     * @param victimCount the estimated victim count (&gt;= 0).
     * @param actorId     the acting user's id, or null for an anonymous citizen.
     * @return the persisted incident.
     */
    public Incident submitIncident(HazardType hazardType, GpsCoordinate location,
            String description, int victimCount, UUID actorId) {
        Incident incident = new Incident(hazardType, location, description, victimCount);
        this.incidentDao.insert(incident);
        audit(actorId, incident.getId(), "SUBMIT_INCIDENT", incident.getId());
        return incident;
    }

    /**
     * Returns all incidents, most urgent first.
     *
     * @return the ordered incident list (never null).
     */
    public List<Incident> listIncidents() {
        List<Incident> incidents = this.incidentDao.findAll();
        incidents.sort(MOST_URGENT_FIRST);
        return incidents;
    }

    /**
     * Triages an incident: sets its severity and persists it.
     *
     * @param incidentId the incident id (must exist).
     * @param severity   the severity to apply (must not be null).
     * @param actorId    the acting user's id, or null.
     * @return the updated incident.
     * @throws NoSuchElementException if the incident does not exist.
     */
    public Incident triage(UUID incidentId, Severity severity, UUID actorId) {
        Incident incident = require(incidentId);
        incident.triage(severity);
        this.incidentDao.update(incident);
        audit(actorId, incidentId, "TRIAGE_INCIDENT severity=" + severity, incidentId);
        return incident;
    }

    /**
     * Allocates a responder to an incident.
     *
     * @param incidentId  the incident id (must exist).
     * @param responderId the responder id (must exist).
     * @param actorId     the acting user's id, or null.
     * @return the incident with its responders attached.
     * @throws NoSuchElementException if the incident does not exist afterwards.
     */
    public Incident assignResponder(UUID incidentId, UUID responderId, UUID actorId) {
        this.incidentDao.assignResponder(incidentId, responderId);
        audit(actorId, incidentId, "ASSIGN_RESPONDER responder=" + responderId, responderId);
        return require(incidentId);
    }

    /**
     * Marks an incident resolved.
     *
     * @param incidentId the incident id (must exist).
     * @param actorId    the acting user's id, or null.
     * @return the updated incident.
     * @throws NoSuchElementException if the incident does not exist.
     */
    public Incident resolve(UUID incidentId, UUID actorId) {
        Incident incident = require(incidentId);
        incident.resolve();
        this.incidentDao.update(incident);
        audit(actorId, incidentId, "RESOLVE_INCIDENT", incidentId);
        return incident;
    }

    /**
     * Recommends and records a public-alert template for an incident.
     *
     * @param incidentId the incident id (must exist).
     * @param actorId    the acting user's id, or null.
     * @return the recommended template.
     * @throws NoSuchElementException if the incident does not exist.
     */
    public AlertTemplate recommendTemplate(UUID incidentId, UUID actorId) {
        Incident incident = require(incidentId);
        AlertTemplate template = this.recommender.recommend(incident);
        incident.setRecommendedTemplate(template);
        this.incidentDao.update(incident);
        audit(actorId, incidentId, "RECOMMEND_TEMPLATE template=" + template, incidentId);
        return template;
    }

    /**
     * Returns the field responders.
     *
     * @return the responder list (never null).
     */
    public List<Responder> listResponders() {
        return this.responderDao.findAll();
    }

    /**
     * Returns the responders allocated to an incident.
     *
     * @param incidentId the incident id.
     * @return the assigned responders (never null).
     */
    public List<Responder> assignedResponders(UUID incidentId) {
        return this.incidentDao.findAssignedResponders(incidentId);
    }

    /**
     * Loads an incident or fails if it is unknown.
     *
     * @param incidentId the incident id.
     * @return the incident.
     * @throws NoSuchElementException if absent.
     */
    private Incident require(UUID incidentId) {
        return this.incidentDao.findByUuid(incidentId).orElseThrow(
                () -> new NoSuchElementException("No incident with id " + incidentId));
    }

    /**
     * Appends an incident-scoped audit entry.
     *
     * @param actorId    the acting user's id, or null.
     * @param incidentId the incident id.
     * @param action     the action description.
     * @param entityId   the affected entity's id.
     */
    private void audit(UUID actorId, UUID incidentId, String action, UUID entityId) {
        this.auditDao.record(new AuditEntry(actorId, incidentId, action, "Incident", entityId));
    }
}
