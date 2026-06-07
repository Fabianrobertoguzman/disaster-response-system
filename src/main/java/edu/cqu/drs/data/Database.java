package edu.cqu.drs.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Properties;

/**
 * JDBC connection factory and schema initialiser for the MySQL data tier.
 *
 * <p>Connection settings are resolved in this order of precedence: environment
 * variables ({@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}); then an
 * editable {@code db.properties} file in the working directory; then a bundled
 * {@code /db.properties} on the classpath as a last-resort default. This lets a
 * marker change credentials without recompiling.</p>
 *
 * <p>{@link #getConnection()} returns a fresh {@link Connection} on every call;
 * the caller is responsible for closing it (use try-with-resources). A single
 * connection is deliberately never cached and shared, because a JDBC connection
 * is not safe to use from more than one server worker thread at a time.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class Database {

    /** Working-directory configuration file a marker can edit. */
    private static final String CONFIG_FILE = "db.properties";

    /** Classpath fallback configuration if no working-directory file exists. */
    private static final String CONFIG_RESOURCE = "/db.properties";

    /** Classpath location of the DDL script. */
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    /** Classpath location of the reference-data script. */
    private static final String SEED_RESOURCE = "/db/seed.sql";

    /** Resolved JDBC URL. */
    private final String url;

    /** Resolved database user. */
    private final String user;

    /** Resolved database password. */
    private final String password;

    /**
     * Builds a database helper, resolving the connection settings once.
     */
    public Database() {
        Properties config = loadConfig();
        this.url = resolve("DB_URL", config, "db.url");
        this.user = resolve("DB_USER", config, "db.user");
        this.password = resolve("DB_PASSWORD", config, "db.password");
    }

    /**
     * Loads {@code db.properties} from the working directory if present,
     * otherwise from the classpath; returns empty properties if neither exists
     * (environment variables may still supply every value).
     *
     * @return the loaded configuration (possibly empty, never null).
     */
    private static Properties loadConfig() {
        Properties config = new Properties();
        File workingCopy = new File(CONFIG_FILE);
        if (workingCopy.isFile()) {
            try (InputStream in = new FileInputStream(workingCopy)) {
                config.load(in);
                return config;
            } catch (IOException ex) {
                System.err.println("Could not read " + CONFIG_FILE
                        + "; falling back to classpath/env: " + ex.getMessage());
            }
        }
        try (InputStream in = Database.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (in != null) {
                config.load(in);
            }
        } catch (IOException ex) {
            System.err.println("Could not read bundled " + CONFIG_RESOURCE
                    + ": " + ex.getMessage());
        }
        return config;
    }

    /**
     * Resolves one setting from the environment first, then the file.
     *
     * @param envVar   the environment variable to check first.
     * @param config   the loaded file configuration.
     * @param fileKey  the key to read from the file if the env var is unset.
     * @return the resolved value, or an empty string if neither source has it.
     */
    private static String resolve(String envVar, Properties config, String fileKey) {
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return config.getProperty(fileKey, "");
    }

    /**
     * Opens a new connection to the database.
     *
     * @return a fresh {@link Connection}; the caller must close it.
     * @throws SQLException if the connection cannot be established; the message
     *         includes a remediation hint and the (password-free) JDBC URL.
     */
    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(this.url, this.user, this.password);
        } catch (SQLException ex) {
            throw new SQLException("Could not connect to MySQL at " + this.url
                    + " as user '" + this.user + "'. Check that the MySQL server "
                    + "is running and that the credentials in " + CONFIG_FILE
                    + " (or the DB_URL/DB_USER/DB_PASSWORD environment variables) "
                    + "are correct. Underlying cause: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates the schema and loads the reference data by executing the bundled
     * SQL scripts. Safe to call on every start-up: the schema script drops and
     * recreates the tables, so a stale database cannot retain old definitions.
     *
     * @throws SQLException if a statement fails.
     * @throws IOException  if a script cannot be read from the classpath.
     */
    public void initialise() throws SQLException, IOException {
        runScript(SCHEMA_RESOURCE);
        runScript(SEED_RESOURCE);
    }

    /**
     * Executes every {@code ;}-terminated statement in a classpath SQL script.
     *
     * @param resource the classpath location of the script.
     * @throws SQLException if a statement fails.
     * @throws IOException  if the script cannot be read.
     */
    private void runScript(String resource) throws SQLException, IOException {
        List<String> statements = readStatements(resource);
        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    /**
     * Reads a SQL script from the classpath and splits it into individual
     * statements, ignoring blank lines and {@code --} line comments.
     *
     * <p>The split is a simple one on {@code ;}; it assumes the bundled scripts
     * contain no semicolons inside string literals and no {@code DELIMITER}-style
     * stored-routine blocks, which holds for {@code schema.sql} and
     * {@code seed.sql}. A fuller SQL parser would be needed if that changes.</p>
     *
     * @param resource the classpath location of the script.
     * @return the list of non-empty SQL statements, in order.
     * @throws IOException if the script is missing or unreadable.
     */
    private static List<String> readStatements(String resource) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("SQL script not found on classpath: " + resource);
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
