package com.coit20258.drs.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;

    private Database() {
    }

    public static void boot() {
        resolveConfig();

        try (Connection conn = getConnection()) {
            createTables(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DatabaseUtil.boot() failed: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (jdbcUrl == null) {
            throw new IllegalStateException(
                    "DatabaseUtil has not been initialised. Call DatabaseUtil.boot() first.");
        }
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    private static void resolveConfig() {
        String host = getEnvOrDefault("DRS_DB_HOST", "localhost");
        String port = getEnvOrDefault("DRS_DB_PORT", "3306");
        String name = getEnvOrDefault("DRS_DB_NAME", "drs_db");
        dbUser = getEnvOrDefault("DRS_DB_USER", "root");
        dbPassword = getEnvOrDefault("DRS_DB_PASS", "pass");

        // Extra JDBC options improve reliability:
        //   useSSL=false            — disable SSL for local dev (set to true in prod)
        //   allowPublicKeyRetrieval — needed for MySQL 8 caching_sha2_password
        //   serverTimezone          — avoids timezone negotiation warnings
        jdbcUrl = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC&characterEncoding=UTF-8",
                host, port, name);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // -----------------------------------------------------------------
            // 1. users
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                    + "  id            INT          NOT NULL AUTO_INCREMENT, "
                    + "  firstName     VARCHAR(50), "
                    + "  lastName      VARCHAR(50), "
                    + "  email         VARCHAR(150) NOT NULL UNIQUE, "
                    + "  passwordHash  VARCHAR(255) NOT NULL, " // plain SHA-256 hex per spec
                    + "  role          VARCHAR(255), "
                    + "  isActive      TINYINT(1)   NOT NULL DEFAULT 1, "
                    + "  createdAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  lastLoginAt   TIMESTAMP, "
                    + "  PRIMARY KEY (id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // -----------------------------------------------------------------
            // 2. disaster_reports
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS disaster_reports ("
                    + "  id             INT          NOT NULL AUTO_INCREMENT, "
                    + "  disasterType   VARCHAR(100) NOT NULL, "
                    + "  location       VARCHAR(255) NOT NULL, "
                    + "  severityLevel  VARCHAR(20)  NOT NULL, "
                    + "  description    TEXT, "
                    + "  status         VARCHAR(50)  NOT NULL DEFAULT 'REPORTED', "
                    + "  reportedById   INT          NOT NULL, "
                    + "  reportedAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  PRIMARY KEY (id), "
                    + "  CONSTRAINT fk_report_user FOREIGN KEY (reportedById) "
                    + "    REFERENCES users(id) ON DELETE RESTRICT"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }
}
