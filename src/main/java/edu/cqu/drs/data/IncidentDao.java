package edu.cqu.drs.data;

import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for {@link Incident} persistence, including the
 * many-to-many allocation of responders to incidents.
 *
 * <p>Each loaded incident is a complete aggregate: {@link #findByUuid(UUID)} and
 * {@link #findAll()} return incidents with their assigned responders attached.
 * Responder allocations are recorded through {@link #assignResponder(UUID, UUID)}
 * and can be read back with {@link #findAssignedResponders(UUID)}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface IncidentDao {

    /**
     * Inserts a new incident (without its responder allocations, which are added
     * separately via {@link #assignResponder(UUID, UUID)}).
     *
     * @param incident the incident to persist (must not be null).
     * @throws DataAccessException if the insert fails.
     */
    void insert(Incident incident);

    /**
     * Updates the mutable fields of an existing incident (hazard, severity,
     * status, location, description, victim count and recommended template),
     * matched by its id. The id and creation timestamp are immutable and are not
     * changed.
     *
     * @param incident the incident carrying the new values (must not be null).
     * @throws DataAccessException if the update fails.
     */
    void update(Incident incident);

    /**
     * Finds an incident by its domain id, with its assigned responders attached.
     *
     * @param id the incident id (must not be null).
     * @return the incident if present, otherwise {@link Optional#empty()}.
     * @throws DataAccessException if the query fails.
     */
    Optional<Incident> findByUuid(UUID id);

    /**
     * Returns every stored incident, each with its assigned responders attached,
     * ordered by insertion.
     *
     * @return the list of incidents (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<Incident> findAll();

    /**
     * Records that a responder is allocated to an incident: it adds the junction
     * row and sets the responder's current tasking to that incident, atomically.
     *
     * @param incidentId  the incident's id (must not be null and must exist).
     * @param responderId the responder's id (must not be null and must exist).
     * @throws DataAccessException if either entity is unknown or the write fails.
     */
    void assignResponder(UUID incidentId, UUID responderId);

    /**
     * Returns the responders currently allocated to an incident.
     *
     * @param incidentId the incident's id (must not be null).
     * @return the assigned responders (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<Responder> findAssignedResponders(UUID incidentId);
}
