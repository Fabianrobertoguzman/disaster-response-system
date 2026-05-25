package com.coit20258.drs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.User;
import com.coit20258.drs.util.Database;

public class DisasterReportDaoImpl implements DisasterReportDao {

    private static final String SELECT_WITH_USER
            = "SELECT dr.id, dr.disasterType, dr.location, dr.severityLevel, "
            + "dr.description, dr.status, dr.reportedAt, "
            + "u.id AS userId, u.firstName, u.lastName, u.email, u.role "
            + "FROM disaster_reports dr "
            + "JOIN users u ON dr.reportedById = u.id ";

    @Override
    public DisasterReport create(DisasterReport report) {
        final String SQL
                = "INSERT INTO disaster_reports "
                + "(disasterType, location, severityLevel, description, "
                + "status, reportedById, reportedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, report.getDisasterType());
            ps.setString(2, report.getLocation());
            ps.setString(3, report.getSeverityLevel());
            ps.setString(4, report.getDescription());
            ps.setString(5, report.getStatus());
            ps.setInt(6, report.getReportedBy().getId());
            ps.setTimestamp(7, Timestamp.valueOf(report.getReportedAt()));

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException(
                        "Create failed: no rows inserted.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    report.setId(keys.getInt(1));
                }
            }

            return report;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database error during create: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DisasterReport> findAll() {
        final String SQL = SELECT_WITH_USER + "ORDER BY dr.reportedAt DESC";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL); ResultSet rs = ps.executeQuery()) {

            List<DisasterReport> reports = new ArrayList<>();
            while (rs.next()) {
                reports.add(mapRow(rs));
            }
            return reports;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database error during findAll: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DisasterReport> findById(int id) {
        final String SQL = SELECT_WITH_USER + "WHERE dr.id = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database error during findById: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DisasterReport> findByReporterId(int reporterId) {
        final String SQL = SELECT_WITH_USER
                + "WHERE dr.reportedById = ? "
                + "ORDER BY dr.reportedAt DESC";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, reporterId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DisasterReport> reports = new ArrayList<>();
                while (rs.next()) {
                    reports.add(mapRow(rs));
                }
                return reports;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database error during findByReporterId: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(int id, String status) {
        final String SQL
                = "UPDATE disaster_reports SET status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database error during updateStatus: "
                    + e.getMessage(), e);
        }
    }

    private DisasterReport mapRow(ResultSet rs) throws SQLException {
        Timestamp reportedAt = rs.getTimestamp("reportedAt");

        User reporter = new User(
                rs.getInt("userId"),
                rs.getString("firstName"),
                rs.getString("lastName"),
                rs.getString("email"),
                null,
                rs.getString("role"),
                true,
                null,
                null);

        return new DisasterReport(
                rs.getInt("id"),
                rs.getString("disasterType"),
                rs.getString("location"),
                rs.getString("severityLevel"),
                rs.getString("description"),
                rs.getString("status"),
                reporter,
                reportedAt != null
                        ? reportedAt.toLocalDateTime() : null);
    }
}
