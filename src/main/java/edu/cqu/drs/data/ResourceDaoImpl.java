package edu.cqu.drs.data;

import edu.cqu.drs.model.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of {@link ResourceDao}.
 *
 * <p>Thread-safety: this object holds only the immutable {@link Database}
 * connection factory and is therefore safe to share across server worker
 * threads. Every method opens its own {@link Connection} (connection-per-request)
 * with try-with-resources and never caches or shares one, because a JDBC
 * connection is not safe for concurrent use.</p>
 *
 * <p>All SQL is parameterised with {@link PreparedStatement}, which both
 * prevents SQL injection and lets the driver reuse statement plans.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ResourceDaoImpl implements ResourceDao {

    private static final String INSERT_SQL =
            "INSERT INTO resources (uuid, resource_type, available) VALUES (?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE resources SET resource_type = ?, available = ? WHERE uuid = ?";

    private static final String SELECT_BY_UUID_SQL =
            "SELECT uuid, resource_type, available FROM resources WHERE uuid = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT uuid, resource_type, available FROM resources ORDER BY id";

    /** Immutable connection factory shared across threads. */
    private final Database database;

    /**
     * Creates a resource DAO over the given connection factory.
     *
     * @param database the connection factory (must not be null).
     */
    public ResourceDaoImpl(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        this.database = database;
    }

    @Override
    public void insert(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, resource.getId().toString());
            statement.setString(2, resource.getResourceType());
            statement.setBoolean(3, resource.isAvailable());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not insert resource " + resource.getId(), ex);
        }
    }

    @Override
    public void update(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, resource.getResourceType());
            statement.setBoolean(2, resource.isAvailable());
            statement.setString(3, resource.getId().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not update resource " + resource.getId(), ex);
        }
    }

    @Override
    public Optional<Resource> findByUuid(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_UUID_SQL)) {
            statement.setString(1, id.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return Optional.of(map(rows));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load resource " + id, ex);
        }
    }

    @Override
    public List<Resource> findAll() {
        List<Resource> resources = new ArrayList<>();
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                resources.add(map(rows));
            }
            return resources;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load resources", ex);
        }
    }

    /**
     * Maps the current row of a result set to a {@link Resource} using the
     * reconstruction constructor (preserving the stored id and availability).
     *
     * @param rows a result set positioned on a resource row.
     * @return the mapped resource.
     * @throws SQLException if a column cannot be read.
     */
    private static Resource map(ResultSet rows) throws SQLException {
        return new Resource(
                UUID.fromString(rows.getString("uuid")),
                rows.getString("resource_type"),
                rows.getBoolean("available"));
    }
}
