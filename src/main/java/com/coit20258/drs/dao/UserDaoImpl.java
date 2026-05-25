package com.coit20258.drs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

import com.coit20258.drs.model.User;
import com.coit20258.drs.util.Database;
import com.coit20258.drs.util.Security;

public class UserDaoImpl implements UserDao {

    @Override
    public User register(User user) {
        String SQL = "INSERT INTO users (email, passwordHash, firstName, lastName, role, isActive)"
                + "VALUES (?, ?, ?, ? , ? , ?)";

        try (
                Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.isActive());

            int result = ps.executeUpdate();
            if (result == 0) {
                throw new RuntimeException("Register failed: no rows inserted for user '"
                        + user.getEmail() + "'.");
            }

            return user;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                String msg = e.getMessage().contains("email")
                        ? "Email '" + user.getEmail() + "' is already registered."
                        : e.getMessage();
                throw new RuntimeException(msg, e);
            }
            throw new RuntimeException("Database error during registration: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean emailExists(String email) {
        final String SQL = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean setActiveStatus(int userId, boolean isActive) {
        final String SQL = "UPDATE users SET isActive = ? WHERE id = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setBoolean(1, isActive);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> login(String email, String rawPassword) {
        Optional<User> found = this.findByEmail(email);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        User user = found.get();

        // Reject inactive accounts
        if (!user.isActive()) {
            return Optional.empty();
        }

        // Verify password using constant-time comparison
        if (!Security.verifyPassword(rawPassword, user.getPasswordHash())) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        final String SQL = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("createdAt");

        return new User(
                rs.getInt("id"),
                rs.getString("firstName"),
                rs.getString("lastName"),
                rs.getString("email"),
                rs.getString("passwordHash"),
                rs.getString("role"),
                rs.getBoolean("isActive"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                null
        );
    }
}
