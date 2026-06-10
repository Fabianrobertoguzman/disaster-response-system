package edu.cqu.drs.data;

import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of {@link IncidentDao}.
 *
 * <p>Thread-safety: holds only the immutable {@link Database} factory and opens a
 * fresh {@link Connection} per request. Operations that touch more than one table
 * ({@link #assignResponder(UUID, UUID)}) run inside a single transaction so the
 * junction row and the responder's tasking update either both succeed or both
 * roll back.</p>
 *
 * <p>Enumerations are stored by {@link Enum#name()} and recovered with the
 * matching {@code valueOf}; the domain {@link UUID} maps to the {@code uuid}
 * column while the database generates the surrogate primary key
 * (Assessment&nbsp;3 §2.4).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class IncidentDaoImpl implements IncidentDao {

    private static final String INSERT_SQL =
            "INSERT INTO incidents (uuid, hazard_type, severity, status, latitude, longitude, "
            + "description, victim_count, reported_at, resolved_at, recommended_template) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE incidents SET hazard_type = ?, severity = ?, status = ?, latitude = ?, "
            + "longitude = ?, description = ?, victim_count = ?, resolved_at = ?, "
            + "recommended_template = ? WHERE uuid = ?";

    private static final String SELECT_COLUMNS =
            "SELECT id, uuid, hazard_type, severity, status, latitude, longitude, description, "
            + "victim_count, reported_at, resolved_at, recommended_template FROM incidents";

    private static final String SELECT_BY_UUID_SQL = SELECT_COLUMNS + " WHERE uuid = ?";

    private static final String SELECT_ALL_SQL = SELECT_COLUMNS + " ORDER BY id";

    private static final String SELECT_INCIDENT_KEY_SQL =
            "SELECT id FROM incidents WHERE uuid = ?";

    private static final String SELECT_RESPONDER_KEY_SQL =
            "SELECT id FROM responders WHERE uuid = ?";

    /** Loads the responders allocated to one incident (by the incident's surrogate key). */
    private static final String SELECT_ASSIGNED_SQL =
            "SELECT r.uuid AS responder_uuid, r.name AS name "
            + "FROM responders r JOIN incident_responders ir ON r.id = ir.responder_id "
            + "WHERE ir.incident_id = ? ORDER BY ir.assigned_at, r.id";

    private static final String INSERT_JUNCTION_SQL =
            "INSERT INTO incident_responders (incident_id, responder_id) VALUES (?, ?)";

    private static final String UPDATE_TASKING_SQL =
            "UPDATE responders SET current_tasking_id = ? WHERE id = ?";

    /** Immutable connection factory shared across threads. */
    private final Database database;

    /**
     * Creates an incident DAO over the given connection factory.
     *
     * @param database the connection factory (must not be null).
     */
    public IncidentDaoImpl(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        this.database = database;
    }

    @Override
    public void insert(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            GpsCoordinate location = incident.getGpsLocation();
            statement.setString(1, incident.getId().toString());
            statement.setString(2, incident.getHazardType().name());
            statement.setString(3, incident.getSeverity().name());
            statement.setString(4, incident.getStatus().name());
            statement.setDouble(5, location.getLatitude());
            statement.setDouble(6, location.getLongitude());
            statement.setString(7, incident.getDescription());
            statement.setInt(8, incident.getVictimCount());
            statement.setTimestamp(9, Timestamp.valueOf(incident.getReportedAt()));
            setNullableTimestamp(statement, 10, incident.getResolvedAt());
            setTemplate(statement, 11, incident.getRecommendedTemplate());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not insert incident " + incident.getId(), ex);
        }
    }

    @Override
    public void update(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            GpsCoordinate location = incident.getGpsLocation();
            statement.setString(1, incident.getHazardType().name());
            statement.setString(2, incident.getSeverity().name());
            statement.setString(3, incident.getStatus().name());
            statement.setDouble(4, location.getLatitude());
            statement.setDouble(5, location.getLongitude());
            statement.setString(6, incident.getDescription());
            statement.setInt(7, incident.getVictimCount());
            setNullableTimestamp(statement, 8, incident.getResolvedAt());
            setTemplate(statement, 9, incident.getRecommendedTemplate());
            statement.setString(10, incident.getId().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not update incident " + incident.getId(), ex);
        }
    }

    @Override
    public Optional<Incident> findByUuid(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_UUID_SQL)) {
            statement.setString(1, id.toString());
            IncidentRow row = null;
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    row = readRow(rows);
                }
            }
            // The incident result set is closed before the responder load runs, so
            // only one statement is ever active on the connection at a time.
            return row == null ? Optional.empty() : Optional.of(toIncident(connection, row));
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load incident " + id, ex);
        }
    }

    @Override
    public List<Incident> findAll() {
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL)) {
            List<IncidentRow> rawRows = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    rawRows.add(readRow(rows));
                }
            }
            // The incident result set is now closed, so the per-incident responder
            // load below never runs a second query while it is still open. The
            // dataset for this prototype is small, so this simple per-incident load
            // is preferred over a single join with in-memory grouping; it can be
            // batched if the data ever grows.
            List<Incident> incidents = new ArrayList<>();
            for (IncidentRow row : rawRows) {
                incidents.add(toIncident(connection, row));
            }
            return incidents;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load incidents", ex);
        }
    }

    @Override
    public void assignResponder(UUID incidentId, UUID responderId) {
        if (incidentId == null || responderId == null) {
            throw new IllegalArgumentException("incidentId and responderId must not be null");
        }
        try (Connection connection = this.database.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Long incidentKey = key(connection, SELECT_INCIDENT_KEY_SQL, incidentId);
                Long responderKey = key(connection, SELECT_RESPONDER_KEY_SQL, responderId);
                if (incidentKey == null) {
                    throw new SQLException("No incident with id " + incidentId);
                }
                if (responderKey == null) {
                    throw new SQLException("No responder with id " + responderId);
                }
                insertJunction(connection, incidentKey, responderKey);
                updateTasking(connection, incidentKey, responderKey);
                connection.commit();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException restoreEx) {
                    // Best-effort restore of the auto-commit flag; the connection is
                    // closed immediately afterwards by the surrounding try-with-resources.
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not allocate responder " + responderId
                    + " to incident " + incidentId, ex);
        }
    }

    @Override
    public List<Responder> findAssignedResponders(UUID incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("incidentId must not be null");
        }
        try (Connection connection = this.database.getConnection()) {
            Long incidentKey = key(connection, SELECT_INCIDENT_KEY_SQL, incidentId);
            if (incidentKey == null) {
                return new ArrayList<>();
            }
            return loadResponders(connection, incidentKey, incidentId);
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not load responders for incident " + incidentId, ex);
        }
    }

    /**
     * Reads the incident columns of the current row into a lightweight holder
     * (no database access), so the incident result set can be closed before any
     * responder query runs.
     *
     * @param rows a result set positioned on an incident row.
     * @return the row's incident fields, including the surrogate key.
     * @throws SQLException if a column cannot be read.
     */
    private static IncidentRow readRow(ResultSet rows) throws SQLException {
        String template = rows.getString("recommended_template");
        Timestamp resolvedAt = rows.getTimestamp("resolved_at");
        IncidentRow row = new IncidentRow(
                rows.getLong("id"),
                UUID.fromString(rows.getString("uuid")),
                HazardType.valueOf(rows.getString("hazard_type")),
                Severity.valueOf(rows.getString("severity")),
                IncidentStatus.valueOf(rows.getString("status")),
                rows.getDouble("latitude"),
                rows.getDouble("longitude"),
                rows.getString("description"),
                rows.getInt("victim_count"),
                rows.getTimestamp("reported_at").toLocalDateTime(),
                template == null ? null : AlertTemplate.valueOf(template));
        row.resolvedAt = (resolvedAt == null) ? null : resolvedAt.toLocalDateTime();
        return row;
    }

    /**
     * Builds a complete {@link Incident} aggregate from a previously-read row,
     * loading its assigned responders.
     *
     * @param connection the active connection (used to load responders).
     * @param row        the incident fields read from the result set.
     * @return the mapped incident with its responders attached.
     * @throws SQLException if the responder load fails.
     */
    private static Incident toIncident(Connection connection, IncidentRow row) throws SQLException {
        List<Responder> responders = loadResponders(connection, row.key, row.uuid);
        Incident incident = new Incident(row.uuid, row.hazardType, row.severity,
                new GpsCoordinate(row.latitude, row.longitude), row.description,
                row.victimCount, row.reportedAt, row.status, row.recommendedTemplate, responders);
        incident.setResolvedAt(row.resolvedAt);
        return incident;
    }

    /**
     * Immutable holder for the columns of one {@code incidents} row. It decouples
     * reading the incident result set from the follow-up responder load, so the
     * two never run concurrently on the same connection.
     */
    private static final class IncidentRow {
        private final long key;
        private final UUID uuid;
        private final HazardType hazardType;
        private final Severity severity;
        private final IncidentStatus status;
        private final double latitude;
        private final double longitude;
        private final String description;
        private final int victimCount;
        private final LocalDateTime reportedAt;
        private final AlertTemplate recommendedTemplate;

        /** When the incident was resolved, or null while open (set after construction). */
        private LocalDateTime resolvedAt;

        private IncidentRow(long key, UUID uuid, HazardType hazardType, Severity severity,
                IncidentStatus status, double latitude, double longitude, String description,
                int victimCount, LocalDateTime reportedAt, AlertTemplate recommendedTemplate) {
            this.key = key;
            this.uuid = uuid;
            this.hazardType = hazardType;
            this.severity = severity;
            this.status = status;
            this.latitude = latitude;
            this.longitude = longitude;
            this.description = description;
            this.victimCount = victimCount;
            this.reportedAt = reportedAt;
            this.recommendedTemplate = recommendedTemplate;
        }
    }

    /**
     * Loads the responders allocated to an incident, each carrying that incident's
     * id as its current tasking.
     *
     * @param connection   the active connection.
     * @param incidentKey  the incident's surrogate key.
     * @param incidentUuid the incident's domain id (set as each responder's tasking).
     * @return the assigned responders (never null; possibly empty).
     * @throws SQLException if the query fails.
     */
    private static List<Responder> loadResponders(Connection connection, long incidentKey,
            UUID incidentUuid) throws SQLException {
        List<Responder> responders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ASSIGNED_SQL)) {
            statement.setLong(1, incidentKey);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    responders.add(new Responder(
                            UUID.fromString(rows.getString("responder_uuid")),
                            rows.getString("name"),
                            incidentUuid));
                }
            }
        }
        return responders;
    }

    /**
     * Inserts a junction row recording a responder allocation.
     *
     * @param connection   the active (transactional) connection.
     * @param incidentKey  the incident's surrogate key.
     * @param responderKey the responder's surrogate key.
     * @throws SQLException if the insert fails.
     */
    private static void insertJunction(Connection connection, long incidentKey,
            long responderKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_JUNCTION_SQL)) {
            statement.setLong(1, incidentKey);
            statement.setLong(2, responderKey);
            statement.executeUpdate();
        }
    }

    /**
     * Sets a responder's current tasking to an incident.
     *
     * @param connection   the active (transactional) connection.
     * @param incidentKey  the incident's surrogate key.
     * @param responderKey the responder's surrogate key.
     * @throws SQLException if the update fails.
     */
    private static void updateTasking(Connection connection, long incidentKey,
            long responderKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_TASKING_SQL)) {
            statement.setLong(1, incidentKey);
            statement.setLong(2, responderKey);
            statement.executeUpdate();
        }
    }

    /**
     * Binds a nullable timestamp parameter, or SQL NULL when the value is null.
     *
     * @param statement the statement being populated.
     * @param index     the parameter index.
     * @param value     the timestamp, or null.
     * @throws SQLException if the bind fails.
     */
    private static void setNullableTimestamp(PreparedStatement statement, int index,
            LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    /**
     * Binds the recommended-template parameter, or SQL NULL when none is set.
     *
     * @param statement the statement being populated.
     * @param index     the parameter index.
     * @param template  the template, or null.
     * @throws SQLException if the bind fails.
     */
    private static void setTemplate(PreparedStatement statement, int index,
            AlertTemplate template) throws SQLException {
        if (template == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, template.name());
        }
    }

    /**
     * Runs a single-column key lookup, returning the surrogate key or null.
     *
     * @param connection the active connection.
     * @param sql        a query selecting {@code id} for a given {@code uuid}.
     * @param uuid       the domain id to resolve.
     * @return the surrogate key, or null if no row matches.
     * @throws SQLException if the query fails.
     */
    private static Long key(Connection connection, String sql, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong("id") : null;
            }
        }
    }
}
