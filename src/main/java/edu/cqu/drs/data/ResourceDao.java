package edu.cqu.drs.data;

import edu.cqu.drs.model.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for {@link Resource} persistence.
 *
 * <p>The interface is technology-neutral: callers see only the domain type and a
 * {@link DataAccessException} on failure, never JDBC types. The implementation
 * ({@link ResourceDaoImpl}) maps the domain {@link UUID} identity onto the
 * {@code resources.uuid} column while the database generates the surrogate
 * primary key.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface ResourceDao {

    /**
     * Inserts a new resource.
     *
     * @param resource the resource to persist (must not be null).
     * @throws DataAccessException if the insert fails.
     */
    void insert(Resource resource);

    /**
     * Updates the type and availability of an existing resource, matched by its id.
     *
     * @param resource the resource carrying the new values (must not be null).
     * @throws DataAccessException if the update fails.
     */
    void update(Resource resource);

    /**
     * Finds a resource by its domain id.
     *
     * @param id the resource id (must not be null).
     * @return the resource if present, otherwise {@link Optional#empty()}.
     * @throws DataAccessException if the query fails.
     */
    Optional<Resource> findByUuid(UUID id);

    /**
     * Returns every stored resource, ordered by insertion.
     *
     * @return the list of resources (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<Resource> findAll();
}
