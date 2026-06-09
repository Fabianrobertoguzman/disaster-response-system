package edu.cqu.drs.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Proves the H2 (MySQL-mode) test substrate works before any data-tier test
 * depends on it: the dedicated {@code schema-h2.sql} loads cleanly, all eight
 * tables exist, the database generates surrogate keys, and re-initialising
 * yields a clean slate. Pure in-memory - no MySQL server, no network - so this
 * runs unconditionally green.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("H2 bootstrap - MySQL-mode test database substrate")
class H2BootstrapSpec {

    /** The eight tables the production schema defines. */
    private static final String[] TABLES = {
        "users", "incidents", "responders", "resources",
        "partner_agencies", "incident_responders", "notifications", "audit_log",
    };

    @BeforeAll
    static void requireH2Driver() {
        assumeTrue(H2SchemaBootstrap.h2Available(),
                "H2 driver not on the test classpath - H2 substrate tests skipped");
    }

    @Test
    @DisplayName("schema-h2.sql loads and creates all eight tables")
    void shouldCreateAllTables() throws Exception {
        try (Connection connection = H2SchemaBootstrap.freshDatabase("bootstrap_tables");
                Statement statement = connection.createStatement()) {
            for (String table : TABLES) {
                try (ResultSet rows = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + table)) {
                    assertTrue(rows.next(), table + " should be queryable");
                    assertEquals(0, rows.getInt(1), table + " should start empty");
                }
            }
        }
    }

    @Test
    @DisplayName("the database generates the surrogate key; the uuid column carries the domain id")
    void shouldGenerateSurrogateKeys() throws Exception {
        String uuid = UUID.randomUUID().toString();
        try (Connection connection = H2SchemaBootstrap.freshDatabase("bootstrap_keys")) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO resources (uuid, resource_type) VALUES (?, ?)")) {
                insert.setString(1, uuid);
                insert.setString(2, "Ambulance");
                insert.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id, uuid, available FROM resources WHERE uuid = ?")) {
                select.setString(1, uuid);
                try (ResultSet rows = select.executeQuery()) {
                    assertTrue(rows.next());
                    assertTrue(rows.getLong("id") > 0, "id should be database-generated");
                    assertEquals(uuid, rows.getString("uuid"));
                    assertTrue(rows.getBoolean("available"), "available should default TRUE");
                }
            }
        }
    }

    @Test
    @DisplayName("re-initialising the same database drops and recreates the tables (clean slate)")
    void shouldResetOnReinitialise() throws Exception {
        try (Connection connection = H2SchemaBootstrap.freshDatabase("bootstrap_reset");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO partner_agencies (name, available_units) "
                    + "VALUES ('Test Agency', 1)");
        }
        try (Connection connection = H2SchemaBootstrap.freshDatabase("bootstrap_reset");
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT COUNT(*) FROM partner_agencies")) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1), "re-initialisation should clean-slate the data");
        }
    }
}
