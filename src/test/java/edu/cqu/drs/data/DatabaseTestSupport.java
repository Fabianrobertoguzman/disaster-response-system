package edu.cqu.drs.data;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Shared support for the data-access integration tests, and the seam where the
 * {@code -Ptest-h2} profile takes effect.
 *
 * <p>Backend selection: when the {@code drs.test.db} system property is
 * {@code h2} (set by the {@code test-h2} Maven profile), {@link #available()}
 * is always true and {@link #freshDatabase()} returns a {@link Database}
 * pointing the <em>production JDBC DAOs</em> at a clean, seeded, in-memory H2
 * database in MySQL mode (schema {@code schema-h2.sql}, seed
 * {@code seed-h2.sql}) - so a marker with no MySQL server still gets a green
 * run of the data-tier rows, executing the very same DAO SQL. Otherwise the
 * default MySQL path applies: {@link #available()} probes once whether a
 * connection can be opened (bounded by a 3-second login timeout), and each
 * {@code *Spec} calls {@code assumeTrue(available())} so the integration tests
 * are <em>skipped</em> (not failed) when MySQL is unreachable.</p>
 *
 * <p>This class is intentionally <strong>not</strong> named {@code *Spec}, so the
 * Surefire {@code **&#47;*Spec.java} include pattern does not run it as a test.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class DatabaseTestSupport {

    /** System property selecting the test database backend ({@code h2} or unset). */
    private static final String BACKEND_PROPERTY = "drs.test.db";

    /** Shared in-memory H2 database name for the DAO specs (clean-slated per test). */
    private static final String H2_DATABASE_NAME = "dao_specs";

    /** Cached MySQL availability result (computed once on first use). */
    private static Boolean available;

    private DatabaseTestSupport() {
    }

    /**
     * @return true when the {@code -Ptest-h2} profile selected the H2 backend.
     */
    public static boolean h2Selected() {
        return "h2".equalsIgnoreCase(System.getProperty(BACKEND_PROPERTY));
    }

    /**
     * Reports whether a database is available for the DB-gated tests: always
     * true on the H2 backend (in-memory, no server), otherwise a one-time,
     * 3-second-bounded MySQL connection probe.
     *
     * @return true if the selected backend can be used.
     */
    public static synchronized boolean available() {
        if (h2Selected()) {
            return H2SchemaBootstrap.h2Available();
        }
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
     * (re)created, giving each test a clean, known starting state - MySQL via
     * the production scripts, or in-memory H2 via the mirrored H2 scripts when
     * the {@code -Ptest-h2} profile selected that backend.
     *
     * @return an initialised {@link Database}.
     * @throws Exception if the schema or seed scripts cannot be applied.
     */
    public static Database freshDatabase() throws Exception {
        if (h2Selected()) {
            // Clean-slate + seed the shared in-memory database, then hand the
            // production DAOs a Database pointing at it.
            H2SchemaBootstrap.freshSeededDatabase(H2_DATABASE_NAME).close();
            return new Database(H2SchemaBootstrap.urlFor(H2_DATABASE_NAME), "", "");
        }
        Database database = new Database();
        database.initialise();
        return database;
    }
}
