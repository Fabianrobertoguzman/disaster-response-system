package edu.cqu.drs.data;

import edu.cqu.drs.model.Responder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for {@link Responder} persistence.
 *
 * <p>A responder's current tasking is a foreign key to an incident. The
 * implementation translates between the incident's domain {@link UUID} (held by
 * the {@link Responder}) and the incident's surrogate primary key (stored in the
 * {@code responders.current_tasking_id} column).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface ResponderDao {

    /**
     * Inserts a new responder. If the responder has a current tasking, that
     * incident must already exist.
     *
     * @param responder the responder to persist (must not be null).
     * @throws DataAccessException if the insert fails (including an unknown tasking).
     */
    void insert(Responder responder);

    /**
     * Updates a responder's name and current tasking, matched by its id.
     *
     * @param responder the responder carrying the new values (must not be null).
     * @throws DataAccessException if the update fails.
     */
    void update(Responder responder);

    /**
     * Finds a responder by its domain id, resolving its current tasking back to
     * the incident's id.
     *
     * @param id the responder id (must not be null).
     * @return the responder if present, otherwise {@link Optional#empty()}.
     * @throws DataAccessException if the query fails.
     */
    Optional<Responder> findByUuid(UUID id);

    /**
     * Returns every stored responder, ordered by insertion.
     *
     * @return the list of responders (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<Responder> findAll();
}
