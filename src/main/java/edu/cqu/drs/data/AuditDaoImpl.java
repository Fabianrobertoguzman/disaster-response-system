package edu.cqu.drs.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of {@link AuditDao}.
 *
 * <p>Thread-safety: holds only the immutable {@link Database} factory and opens a
 * fresh {@link Connection} per request. The actor and incident foreign keys are
 * the respective surrogate keys, so writes resolve the domain {@link UUID}s to
 * those keys and reads recover them with {@code LEFT JOIN}s (either side may be
 * null).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AuditDaoImpl implements AuditDao {

    private static final String INSERT_SQL =
            "INSERT INTO audit_log (actor_id, incident_id, action, entity, entity_uuid) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BASE_SQL =
            "SELECT a.action AS action, a.entity AS entity, a.entity_uuid AS entity_uuid, "
            + "a.ts AS ts, u.uuid AS actor_uuid, i.uuid AS incident_uuid "
            + "FROM audit_log a "
            + "LEFT JOIN users u ON a.actor_id = u.id "
            + "LEFT JOIN incidents i ON a.incident_id = i.id";

    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL + " ORDER BY a.ts, a.id";

    private static final String SELECT_BY_INCIDENT_SQL =
            SELECT_BASE_SQL + " WHERE i.uuid = ? ORDER BY a.ts, a.id";

    private static final String SELECT_USER_KEY_SQL =
            "SELECT id FROM users WHERE uuid = ?";

    private static final String SELECT_INCIDENT_KEY_SQL =
            "SELECT id FROM incidents WHERE uuid = ?";

    /** Immutable connection factory shared across threads. */
    private final Database database;

    /**
     * Creates an audit DAO over the given connection factory.
     *
     * @param database the connection factory (must not be null).
     */
    public AuditDaoImpl(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        this.database = database;
    }

    @Override
    public void record(AuditEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            setForeignKey(connection, statement, 1,
                    SELECT_USER_KEY_SQL, entry.getActorUuid(), "actor");
            setForeignKey(connection, statement, 2,
                    SELECT_INCIDENT_KEY_SQL, entry.getIncidentUuid(), "incident");
            statement.setString(3, entry.getAction());
            statement.setString(4, entry.getEntity());
            statement.setString(5,
                    entry.getEntityUuid() == null ? null : entry.getEntityUuid().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not append audit entry", ex);
        }
    }

    @Override
    public List<AuditEntry> findAll() {
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
                ResultSet rows = statement.executeQuery()) {
            return mapAll(rows);
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load audit entries", ex);
        }
    }

    @Override
    public List<AuditEntry> findByIncident(UUID incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("incidentId must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT_BY_INCIDENT_SQL)) {
            statement.setString(1, incidentId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return mapAll(rows);
            }
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not load audit entries for incident " + incidentId, ex);
        }
    }

    /**
     * Binds a nullable foreign-key parameter by resolving a domain id to its
     * surrogate key. A null id binds SQL NULL; a non-null id that resolves to no
     * row is a data-integrity error.
     *
     * @param connection the active connection.
     * @param statement  the statement being populated.
     * @param index      the parameter index.
     * @param keySql     the query resolving {@code id} for a given {@code uuid}.
     * @param uuid       the domain id, or null.
     * @param label      a label used in the error message.
     * @throws SQLException if the lookup fails or a non-null id is unknown.
     */
    private static void setForeignKey(Connection connection, PreparedStatement statement,
            int index, String keySql, UUID uuid, String label) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, Types.BIGINT);
            return;
        }
        Long key = resolveKey(connection, keySql, uuid);
        if (key == null) {
            throw new SQLException("Unknown " + label + " for audit entry: " + uuid);
        }
        statement.setLong(index, key);
    }

    /**
     * Resolves a domain id to its surrogate key.
     *
     * @param connection the active connection.
     * @param keySql     the query selecting {@code id} for a given {@code uuid}.
     * @param uuid       the domain id to resolve.
     * @return the surrogate key, or null if no row matches.
     * @throws SQLException if the query fails.
     */
    private static Long resolveKey(Connection connection, String keySql, UUID uuid)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(keySql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong("id") : null;
            }
        }
    }

    /**
     * Maps every remaining row of a result set to an {@link AuditEntry}.
     *
     * @param rows the result set to drain.
     * @return the mapped entries in order.
     * @throws SQLException if a column cannot be read.
     */
    private static List<AuditEntry> mapAll(ResultSet rows) throws SQLException {
        List<AuditEntry> entries = new ArrayList<>();
        while (rows.next()) {
            String actor = rows.getString("actor_uuid");
            String incident = rows.getString("incident_uuid");
            String entityUuid = rows.getString("entity_uuid");
            Timestamp ts = rows.getTimestamp("ts");
            entries.add(new AuditEntry(
                    actor == null ? null : UUID.fromString(actor),
                    incident == null ? null : UUID.fromString(incident),
                    rows.getString("action"),
                    rows.getString("entity"),
                    entityUuid == null ? null : UUID.fromString(entityUuid),
                    ts == null ? null : ts.toLocalDateTime()));
        }
        return entries;
    }
}
