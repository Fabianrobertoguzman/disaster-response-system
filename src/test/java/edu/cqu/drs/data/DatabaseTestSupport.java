package edu.cqu.drs.data;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Shared support for the data-access integration tests.
 *
 * <p>The DAO tests exercise real JDBC against a MySQL database. That database is
 * not present on every machine (for example a marker checking the source without
 * a server running), so {@link #available()} probes once whether a connection
 * can be opened. Each {@code *Spec} calls {@code assumeTrue(available())} so the
 * integration tests are <em>skipped</em> (not failed) when MySQL is unreachable,
 * leaving the pure unit tests to run unconditionally.</p>
 *
 * <p>This class is intentionally <strong>not</strong> named {@code *Spec}, so the
 * Surefire {@code **&#47;*Spec.java} include pattern does not run it as a test.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
final class DatabaseTestSupport {

    /** Cached availability result (computed once on first use). */
    private static Boolean available;

    private DatabaseTestSupport() {
    }

    /**
     * Reports whether a database connection can be opened, caching the result so
     * the probe runs only once per test JVM.
     *
     * @return true if a connection was opened successfully, false otherwise.
     */
    static synchronized boolean available() {
        if (available == null) {
            // Bound the probe so a machine with a different/firewalled MySQL on
            // 3306 makes the DAO specs skip quickly instead of hanging the build.
            DriverManager.setLoginTimeout(3);
            try (Connection connection = new Database().getConnection()) {
                available = connection != null;
            } catch (Exception ex) {
                available = false;
            }
        }
        return available;
    }

    /**
     * Returns a database whose schema and reference data have been freshly
     * (re)created, giving each test a clean, known starting state.
     *
     * @return an initialised {@link Database}.
     * @throws Exception if the schema or seed scripts cannot be applied.
     */
    static Database freshDatabase() throws Exception {
        Database database = new Database();
        database.initialise();
        return database;
    }
}
