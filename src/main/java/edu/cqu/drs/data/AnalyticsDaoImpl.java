package edu.cqu.drs.data;

import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Severity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JDBC implementation of {@link AnalyticsDao}.
 *
 * <p>Thread-safety: holds only the immutable {@link Database} factory and opens
 * a fresh {@link Connection} per request (see {@link ResourceDaoImpl} for the
 * rationale). The count aggregates are computed by the database with
 * {@code GROUP BY}/{@code SUM} over parameter-free statements; response times
 * read the two timestamps and subtract in Java, keeping the SQL inside the
 * MySQL/H2 dialect intersection.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AnalyticsDaoImpl implements AnalyticsDao {

    private static final String COUNT_BY_HAZARD_SQL =
            "SELECT hazard_type AS k, COUNT(*) AS n FROM incidents GROUP BY hazard_type";

    private static final String COUNT_BY_SEVERITY_SQL =
            "SELECT severity AS k, COUNT(*) AS n FROM incidents GROUP BY severity";

    private static final String COUNT_BY_STATUS_SQL =
            "SELECT status AS k, COUNT(*) AS n FROM incidents GROUP BY status";

    private static final String TOTAL_VICTIMS_SQL =
            "SELECT COALESCE(SUM(victim_count), 0) AS total FROM incidents";

    private static final String RESPONSE_PAIRS_SQL =
            "SELECT reported_at, resolved_at FROM incidents WHERE resolved_at IS NOT NULL";

    /** Immutable connection factory shared across threads. */
    private final Database database;

    /**
     * Creates an analytics DAO over the given connection factory.
     *
     * @param database the connection factory (must not be null).
     */
    public AnalyticsDaoImpl(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        this.database = database;
    }

    @Override
    public Map<HazardType, Long> countByHazard() {
        return groupCount(COUNT_BY_HAZARD_SQL, HazardType.class, HazardType::valueOf);
    }

    @Override
    public Map<Severity, Long> countBySeverity() {
        return groupCount(COUNT_BY_SEVERITY_SQL, Severity.class, Severity::valueOf);
    }

    @Override
    public Map<IncidentStatus, Long> countByStatus() {
        return groupCount(COUNT_BY_STATUS_SQL, IncidentStatus.class, IncidentStatus::valueOf);
    }

    @Override
    public long totalVictims() {
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(TOTAL_VICTIMS_SQL);
                ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong("total");
        } catch (SQLException ex) {
            throw new DataAccessException("Could not sum victims", ex);
        }
    }

    @Override
    public List<Long> responseMinutes() {
        List<Long> minutes = new ArrayList<>();
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(RESPONSE_PAIRS_SQL);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Timestamp reported = rows.getTimestamp("reported_at");
                Timestamp resolved = rows.getTimestamp("resolved_at");
                minutes.add(Duration.between(
                        reported.toLocalDateTime(), resolved.toLocalDateTime()).toMinutes());
            }
            return minutes;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load response times", ex);
        }
    }

    /**
     * Runs a two-column key/count {@code GROUP BY} query into an {@link EnumMap}.
     *
     * @param <K>     the enum key type.
     * @param sql     the GROUP BY query (columns aliased {@code k} and {@code n}).
     * @param keyType the enum class.
     * @param parser  maps the stored name to the enum constant.
     * @return the populated count map (absent keys mean zero incidents).
     */
    private <K extends Enum<K>> Map<K, Long> groupCount(String sql, Class<K> keyType,
            Function<String, K> parser) {
        Map<K, Long> counts = new EnumMap<>(keyType);
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                counts.put(parser.apply(rows.getString("k")), rows.getLong("n"));
            }
            return counts;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not compute analytics counts", ex);
        }
    }
}
