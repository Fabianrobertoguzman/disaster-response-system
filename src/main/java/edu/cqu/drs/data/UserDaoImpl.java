package edu.cqu.drs.data;

import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of {@link UserDao}.
 *
 * <p>Thread-safety: holds only the immutable {@link Database} factory and opens a
 * fresh {@link Connection} per request (see {@link ResourceDaoImpl} for the
 * rationale). All statements are parameterised. The domain {@link UUID} maps to
 * the {@code uuid} column while the database generates the surrogate key; the
 * {@code password_hash} and {@code salt} columns hold the credential material.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class UserDaoImpl implements UserDao {

    private static final String INSERT_SQL =
            "INSERT INTO users (uuid, username, role, password_hash, salt) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_USERNAME_SQL =
            "SELECT uuid, username, role, password_hash, salt FROM users WHERE username = ?";

    private static final String SELECT_BY_UUID_SQL =
            "SELECT uuid, username, role FROM users WHERE uuid = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT uuid, username, role FROM users ORDER BY id";

    /** Immutable connection factory shared across threads. */
    private final Database database;

    /**
     * Creates a user DAO over the given connection factory.
     *
     * @param database the connection factory (must not be null).
     */
    public UserDaoImpl(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null");
        }
        this.database = database;
    }

    @Override
    public void insert(User user, String passwordHash, String salt) {
        if (user == null || passwordHash == null || salt == null) {
            throw new IllegalArgumentException("user, passwordHash and salt are required");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, user.getId().toString());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getRole().name());
            statement.setString(4, passwordHash);
            statement.setString(5, salt);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not insert user " + user.getUsername(), ex);
        }
    }

    @Override
    public Optional<StoredUser> findByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    User user = new User(
                            UUID.fromString(rows.getString("uuid")),
                            rows.getString("username"),
                            UserRole.valueOf(rows.getString("role")));
                    return Optional.of(new StoredUser(user,
                            rows.getString("password_hash"), rows.getString("salt")));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load user " + username, ex);
        }
    }

    @Override
    public Optional<User> findByUuid(UUID id) {
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
            throw new DataAccessException("Could not load user " + id, ex);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                users.add(map(rows));
            }
            return users;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load users", ex);
        }
    }

    /**
     * Maps the current row to a {@link User} (without credential material).
     *
     * @param rows a result set positioned on a user row.
     * @return the mapped user.
     * @throws SQLException if a column cannot be read.
     */
    private static User map(ResultSet rows) throws SQLException {
        return new User(
                UUID.fromString(rows.getString("uuid")),
                rows.getString("username"),
                UserRole.valueOf(rows.getString("role")));
    }
}
