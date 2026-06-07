package edu.cqu.drs.data;

import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for the persistent, append-only audit trail
 * (Assessment&nbsp;One FR-14).
 *
 * <p>The trail is append-only by design: there are no update or delete
 * operations. Each {@link AuditEntry} references its actor and incident by their
 * domain {@link UUID}s, which the implementation translates to and from the
 * database surrogate keys.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface AuditDao {

    /**
     * Appends an audit entry. The database stamps the timestamp on insert.
     *
     * @param entry the entry to append (must not be null).
     * @throws DataAccessException if a referenced actor or incident is unknown,
     *         or the insert fails.
     */
    void record(AuditEntry entry);

    /**
     * Returns every audit entry in chronological order.
     *
     * @return the audit entries (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<AuditEntry> findAll();

    /**
     * Returns the audit entries associated with one incident, in chronological
     * order.
     *
     * @param incidentId the incident's id (must not be null).
     * @return the matching entries (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<AuditEntry> findByIncident(UUID incidentId);
}
