package edu.cqu.drs.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates and initialises an in-memory H2 database in MySQL compatibility mode
 * for the database-backed tests, so they can run green on a machine with no
 * MySQL server (the {@code -Ptest-h2} profile - the marker's database-free re-run path).
 *
 * <p>The schema applied is the dedicated {@code /db/schema-h2.sql} on the test
 * classpath - a hand-maintained copy of the production DDL written to the
 * MySQL-8 / H2 intersection - because the production {@code schema.sql} carries
 * MySQL-only {@code ENGINE/CHARSET} clauses that H2 cannot parse. H2 is
 * <em>not</em> a drop-in for MySQL; the known divergences are documented in
 * {@code docs/test/H2_NOT_A_DROPIN.md}.</p>

 * <p>This class deliberately touches the H2 driver only by name (via
 * {@link DriverManager} URLs and {@link Class#forName(String)}), never by
 * compile-time import, so the test sources compile even if the H2 jar were
 * absent. It is intentionally <strong>not</strong> named {@code *Spec}, so the
 * Surefire include pattern does not run it as a test.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class H2SchemaBootstrap {

    /** Classpath location of the H2-dialect DDL script (test resources). */
    private static final String SCHEMA_RESOURCE = "/db/schema-h2.sql";

    /** Classpath location of the H2-dialect reference-data script (fixed uuids). */
    private static final String SEED_RESOURCE = "/db/seed-h2.sql";

    /** Fully-qualified name of the H2 JDBC driver, looked up reflectively. */
    private static final String H2_DRIVER = "org.h2.Driver";

    /**
     * In-memory, MySQL-mode H2 URL. {@code DB_CLOSE_DELAY=-1} keeps the database
     * alive for the whole JVM (not just the first connection), so a spec can open
     * several connections against the same data.
     */
    private static final String URL_TEMPLATE =
            "jdbc:h2:mem:%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private H2SchemaBootstrap() {
    }

    /**
     * Reports whether the H2 driver is on the classpath.
     *
     * @return true if H2 can be used in this JVM.
     */
    public static boolean h2Available() {
        try {
            Class.forName(H2_DRIVER);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Opens a connection to a named in-memory H2 database (creating it on first
     * use) without applying any schema.
     *
     * @param databaseName the in-memory database name (must not be null/blank).
     * @return an open connection; the caller closes it.
     * @throws SQLException if the connection cannot be opened.
     */
    public static Connection openConnection(String databaseName) throws SQLException {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("databaseName must not be null or blank");
        }
        return DriverManager.getConnection(String.format(URL_TEMPLATE, databaseName));
    }

    /**
     * Opens (or reuses) the named in-memory database and applies the H2 schema,
     * dropping and recreating every table - each call yields a clean, known
     * starting state, mirroring the production clean-slate reset.
     *
     * @param databaseName the in-memory database name.
     * @return an open connection to the freshly initialised database.
     * @throws SQLException if a DDL statement fails.
     * @throws IOException  if the schema script cannot be read.
     */
    public static Connection freshDatabase(String databaseName) throws SQLException, IOException {
        Connection connection = openConnection(databaseName);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String sql : readStatements(SCHEMA_RESOURCE)) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException | IOException | RuntimeException ex) {
            // The caller never receives the connection on a failed initialisation,
            // so close it here rather than leaking it.
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                ex.addSuppressed(closeFailure);
            }
            throw ex;
        }
        return connection;
    }

    /**
     * Opens (or reuses) the named in-memory database, applies the H2 schema
     * (clean slate) and the H2 reference-data seed - the full equivalent of the
     * production {@code Database.initialise()}, in the H2 dialect.
     *
     * @param databaseName the in-memory database name.
     * @return an open connection to the freshly initialised, seeded database.
     * @throws SQLException if a statement fails.
     * @throws IOException  if a script cannot be read.
     */
    public static Connection freshSeededDatabase(String databaseName)
            throws SQLException, IOException {
        Connection connection = freshDatabase(databaseName);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String sql : readStatements(SEED_RESOURCE)) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException | IOException | RuntimeException ex) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                ex.addSuppressed(closeFailure);
            }
            throw ex;
        }
        return connection;
    }

    /**
     * Builds the JDBC URL for a named in-memory H2 database in MySQL mode, so a
     * {@code Database} handle can point the production DAOs at it.
     *
     * @param databaseName the in-memory database name.
     * @return the H2 JDBC URL.
     */
    public static String urlFor(String databaseName) {
        return String.format(URL_TEMPLATE, databaseName);
    }

    /**
     * Reads an SQL script from the test classpath and splits it into
     * {@code ;}-terminated statements, skipping blank lines and {@code --}
     * comments (same simple-split constraint as the production loader: no
     * semicolons inside string literals).
     *
     * @param resource the classpath location of the script.
     * @return the statements in order.
     * @throws IOException if the script is missing or unreadable.
     */
    private static List<String> readStatements(String resource) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (InputStream in = H2SchemaBootstrap.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("SQL script not found on test classpath: " + resource);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                        continue;
                    }
                    buffer.append(line).append('\n');
                }
            }
        }
        List<String> statements = new ArrayList<>();
        for (String part : buffer.toString().split(";")) {
            String sql = part.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }
}
