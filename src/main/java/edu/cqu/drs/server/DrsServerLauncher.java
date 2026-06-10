package edu.cqu.drs.server;

import edu.cqu.drs.data.AnalyticsDaoImpl;
import edu.cqu.drs.data.AuditDao;
import edu.cqu.drs.data.AuditDaoImpl;
import edu.cqu.drs.data.DataAccessException;
import edu.cqu.drs.data.Database;
import edu.cqu.drs.data.IncidentDao;
import edu.cqu.drs.data.IncidentDaoImpl;
import edu.cqu.drs.data.ResponderDao;
import edu.cqu.drs.data.ResponderDaoImpl;
import edu.cqu.drs.data.UserDao;
import edu.cqu.drs.data.UserDaoImpl;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.security.AuthService;
import edu.cqu.drs.security.FieldCipher;
import edu.cqu.drs.security.PasswordHasher;
import edu.cqu.drs.server.service.AnalyticsService;
import edu.cqu.drs.server.service.IncidentService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Entry point that starts the secured DRS-Enhanced server against the MySQL data
 * tier.
 *
 * <p>Start sequence: ensure MySQL is running, then run this class (optionally with
 * a port as the first argument; the default is {@link DrsServer#DEFAULT_PORT}).
 * It applies the schema and seed, wires the JDBC DAOs into the service and
 * authentication layers, enables AES-GCM field encryption (key from the
 * {@code DRS_FIELD_KEY} environment variable, or a freshly generated one printed
 * at start-up), bootstraps a default administrator, and starts accepting clients.
 * Clients connect with an {@link edu.cqu.drs.client.ServerStub} and must log in
 * before issuing protected actions.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class DrsServerLauncher {

    /** Default administrator username created on a fresh database. */
    private static final String DEFAULT_ADMIN_USER = "admin";

    /** Default administrator password (a marker should change it). */
    private static final String DEFAULT_ADMIN_PASSWORD = "admin12345";

    private DrsServerLauncher() {
    }

    /**
     * Boots the secured server and keeps it running until the process is
     * terminated (Ctrl+C / IDE stop), at which point the shutdown hook closes
     * the listening socket and the worker pool.
     *
     * @param args optional single argument: the listening port.
     * @throws IOException  if the database scripts cannot be read or the socket cannot bind.
     * @throws SQLException if the schema/seed cannot be applied.
     * @throws InterruptedException if the launcher thread is interrupted.
     */
    public static void main(String[] args)
            throws IOException, SQLException, InterruptedException {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DrsServer.DEFAULT_PORT;

        Database database = new Database();
        database.initialise();

        IncidentDao incidentDao = new IncidentDaoImpl(database);
        ResponderDao responderDao = new ResponderDaoImpl(database);
        AuditDao auditDao = new AuditDaoImpl(database);
        UserDao userDao = new UserDaoImpl(database);

        PasswordHasher hasher = new PasswordHasher();
        AuthService authService = new AuthService(userDao, hasher, auditDao);
        FieldCipher cipher = resolveCipher();
        IncidentService incidentService = new IncidentService(
                incidentDao, responderDao, auditDao, new AlertTemplateRecommender(), cipher);
        AnalyticsService analyticsService =
                new AnalyticsService(new AnalyticsDaoImpl(database));
        RequestDispatcher dispatcher =
                new DrsRequestDispatcher(incidentService, authService, analyticsService);

        bootstrapDefaultAdmin(authService);

        DrsServer server = new DrsServer(port, dispatcher);
        server.start();
        System.out.println("DRS-Enhanced server listening on port " + server.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "drs-shutdown"));

        // The accept-loop runs on a daemon thread, so the launcher must hold the
        // JVM open itself: park the main thread until the process is terminated.
        Thread.currentThread().join();
    }

    /**
     * Resolves the field-encryption key from the environment, or generates an
     * ephemeral one and prints it so it can be persisted.
     *
     * @return a configured {@link FieldCipher}.
     */
    private static FieldCipher resolveCipher() {
        String key = System.getenv("DRS_FIELD_KEY");
        if (key != null && !key.isBlank()) {
            return FieldCipher.fromBase64Key(key);
        }
        FieldCipher cipher = FieldCipher.withGeneratedKey();
        System.out.println("WARNING: no DRS_FIELD_KEY set; generated an ephemeral "
                + "field-encryption key (encrypted data will not be readable after a restart).");
        System.out.println("         To persist it, set DRS_FIELD_KEY=" + cipher.exportKeyBase64());
        return cipher;
    }

    /**
     * Creates the default administrator on a fresh database (the schema is reset
     * on each start, so this normally succeeds; an existing account is left as-is).
     *
     * @param authService the authentication service.
     */
    private static void bootstrapDefaultAdmin(AuthService authService) {
        String password = System.getenv("DRS_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            password = DEFAULT_ADMIN_PASSWORD;
        }
        try {
            authService.register(DEFAULT_ADMIN_USER, password, UserRole.ADMINISTRATOR);
            System.out.println("Created default administrator '" + DEFAULT_ADMIN_USER
                    + "' (set DRS_ADMIN_PASSWORD to override; please change it after first login).");
        } catch (DataAccessException alreadyExists) {
            System.out.println("Default administrator already present.");
        }
    }
}
