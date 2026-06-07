package edu.cqu.drs.data;

import edu.cqu.drs.model.Responder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of {@link ResponderDao}.
 *
 * <p>Thread-safety: holds only the immutable {@link Database} factory and opens a
 * fresh {@link Connection} per request (see {@link ResourceDaoImpl} for the
 * rationale). All statements are parameterised.</p>
 *
 * <p>The {@code current_tasking_id} column is the incident's surrogate key, so
 * writes resolve the incident's domain {@link UUID} to that key with
 * {@link #incidentKey(Connection, UUID)} and reads recover the incident's
 * {@link UUID} with a {@code LEFT JOIN}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ResponderDaoImpl implements ResponderDao {

    private static final String INSERT_SQL =
            "INSERT INTO responders (uuid, name, current_tasking_id) VALUES (?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE responders SET name = ?, current_tasking_id = ? WHERE uuid = ?";

    /** Selects responders, recovering the tasking incident's UUID via a left join. */
    private static final String SELECT_BASE_SQL =
            "SELECT r.uuid AS responder_uuid, r.name AS name, i.uuid AS tasking_uuid "
            + "FROM responders r LEFT JOIN incidents i ON r.current_tasking_id = i.id";

    private static final String SELECT_BY_UUID_SQL =
            SELECT_BASE_SQL + " WHERE r.uuid = ?";

    private static final String SELECT_ALL_SQL =
            SELECT_BASE_SQL + " ORDER BY r.id";

    private static final String SELECT_INCIDENT_KEY_SQL =
            "SELECT id FROM incidents WHERE uuid = ?";

    /** Immutable connection factory shared across threads. */
    private final Database database;

    /**
     * Creates a responder DAO over the given connection factory.
     *
     * @param database the connection factory (must not be null).
     */
    public ResponderDaoImpl(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        this.database = database;
    }

    @Override
    public void insert(Responder responder) {
        if (responder == null) {
            throw new IllegalArgumentException("responder must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, responder.getId().toString());
            statement.setString(2, responder.getName());
            setTasking(connection, statement, 3, responder.getCurrentTaskingId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not insert responder " + responder.getId(), ex);
        }
    }

    @Override
    public void update(Responder responder) {
        if (responder == null) {
            throw new IllegalArgumentException("responder must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, responder.getName());
            setTasking(connection, statement, 2, responder.getCurrentTaskingId());
            statement.setString(3, responder.getId().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not update responder " + responder.getId(), ex);
        }
    }

    @Override
    public Optional<Responder> findByUuid(UUID id) {
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
            throw new DataAccessException("Could not load responder " + id, ex);
        }
    }

    @Override
    public List<Responder> findAll() {
        List<Responder> responders = new ArrayList<>();
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                responders.add(map(rows));
            }
            return responders;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load responders", ex);
        }
    }

    /**
     * Binds the current-tasking parameter, resolving the incident's domain id to
     * its surrogate key, or binding SQL NULL when the responder is available.
     *
     * @param connection the active connection (used to resolve the incident key).
     * @param statement  the statement being populated.
     * @param index      the parameter index to set.
     * @param taskingId  the incident's domain id, or null if available.
     * @throws SQLException if the lookup or bind fails.
     */
    private void setTasking(Connection connection, PreparedStatement statement,
            int index, UUID taskingId) throws SQLException {
        if (taskingId == null) {
            statement.setNull(index, Types.BIGINT);
            return;
        }
        Long key = incidentKey(connection, taskingId);
        if (key == null) {
            throw new SQLException(
                    "Cannot set current tasking: no incident with id " + taskingId);
        }
        statement.setLong(index, key);
    }

    /**
     * Resolves an incident's domain id to its surrogate primary key.
     *
     * @param connection the active connection.
     * @param incidentId the incident's domain id.
     * @return the surrogate key, or null if no such incident exists.
     * @throws SQLException if the query fails.
     */
    private static Long incidentKey(Connection connection, UUID incidentId) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(SELECT_INCIDENT_KEY_SQL)) {
            statement.setString(1, incidentId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong("id") : null;
            }
        }
    }

    /**
     * Maps the current row to a {@link Responder}, recovering the tasking
     * incident's UUID (null when the responder is available).
     *
     * @param rows a result set positioned on a responder row.
     * @return the mapped responder.
     * @throws SQLException if a column cannot be read.
     */
    private static Responder map(ResultSet rows) throws SQLException {
        String taskingUuid = rows.getString("tasking_uuid");
        return new Responder(
                UUID.fromString(rows.getString("responder_uuid")),
                rows.getString("name"),
                taskingUuid == null ? null : UUID.fromString(taskingUuid));
    }
}
