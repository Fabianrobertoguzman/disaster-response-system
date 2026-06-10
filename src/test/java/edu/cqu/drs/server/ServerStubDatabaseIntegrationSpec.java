package edu.cqu.drs.server;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.data.AnalyticsDaoImpl;
import edu.cqu.drs.data.AuditDaoImpl;
import edu.cqu.drs.data.Database;
import edu.cqu.drs.data.DatabaseTestSupport;
import edu.cqu.drs.data.IncidentDaoImpl;
import edu.cqu.drs.data.ResponderDaoImpl;
import edu.cqu.drs.data.UserDaoImpl;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.AnalyticsReport;
import edu.cqu.drs.security.AuthService;
import edu.cqu.drs.security.FieldCipher;
import edu.cqu.drs.security.PasswordHasher;
import edu.cqu.drs.server.service.AnalyticsService;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The suite's first true client-server-database path: a fully wired, secured
 * {@link DrsServer} - JDBC DAOs over the selected backend (MySQL, or in-memory
 * H2 under {@code -Ptest-h2}), authentication, field encryption, analytics -
 * exercised end to end through a real {@link ServerStub} socket: log in, file
 * a report, triage, allocate a responder from the seeded roster, resolve, and
 * read the analytics back, all landing in the database. Skipped (not failed)
 * when no database is reachable.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Client-server-database - full secured cycle over JDBC (MySQL/H2)")
class ServerStubDatabaseIntegrationSpec {

    private Database database;
    private DrsServer server;

    /** The field cipher the server encrypts with - retained so the tests can
     *  prove the stored value is genuine, recoverable AES-GCM ciphertext. */
    private FieldCipher cipher;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "Database not reachable - client-server-database tests skipped");
    }

    @BeforeEach
    void startFullyWiredServer() throws Exception {
        this.database = DatabaseTestSupport.freshDatabase();
        AuthService authService = new AuthService(new UserDaoImpl(this.database),
                new PasswordHasher(), new AuditDaoImpl(this.database));
        authService.register("dispatch1", "pw-dispatch", UserRole.DISPATCHER);
        this.cipher = FieldCipher.withGeneratedKey();
        IncidentService incidentService = new IncidentService(
                new IncidentDaoImpl(this.database), new ResponderDaoImpl(this.database),
                new AuditDaoImpl(this.database), new AlertTemplateRecommender(),
                this.cipher);
        AnalyticsService analyticsService =
                new AnalyticsService(new AnalyticsDaoImpl(this.database));
        this.server = new DrsServer(0, new DrsRequestDispatcher(
                incidentService, authService, analyticsService));
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        if (this.server != null) {
            this.server.stop();
        }
    }

    @Test
    @DisplayName("the full secured dispatch cycle lands in the database and reads back")
    void shouldRunFullCycleAgainstDatabase() throws Exception {
        try (ServerStub stub = new ServerStub("localhost", this.server.getPort())) {
            stub.connect();
            stub.login("dispatch1", "pw-dispatch");

            Incident submitted = stub.submitIncident(HazardType.FIRE,
                    -23.3781, 150.5136, "Warehouse blaze", 3);
            Incident triaged = stub.triage(submitted.getId(), Severity.CRITICAL);
            assertEquals(IncidentStatus.TRIAGED, triaged.getStatus());

            // Pick the responder from the server's seeded roster (6 rows), never
            // from a hardcoded fixture id.
            List<Responder> roster = stub.listResponders();
            assertEquals(6, roster.size(), "the reference seed provides six responders");
            Incident staffed = stub.assignResponder(
                    submitted.getId(), roster.get(0).getId());
            assertEquals(1, staffed.getResponders().size());

            Incident resolved = stub.resolve(submitted.getId());
            assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());

            // The analytics aggregate over the same database confirms the write.
            AnalyticsReport report = stub.getAnalytics();
            assertEquals(1, report.getTotalIncidents());
            assertEquals(3, report.getTotalVictims());
            assertEquals(1, report.getResponseTimes().getResolvedCount());
        }

        // Authoritative check straight at the database, bypassing the server.
        Incident persisted = new IncidentDaoImpl(this.database)
                .findAll().get(0);
        assertEquals(IncidentStatus.RESOLVED, persisted.getStatus());
        assertTrue(persisted.getResolvedAt() != null,
                "the resolution timestamp must be persisted");
        assertEquals(1, persisted.getResponders().size());
    }

    @Test
    @DisplayName("the description is stored encrypted in the database and served decrypted")
    void shouldEncryptAtRestInTheDatabase() throws Exception {
        try (ServerStub stub = new ServerStub("localhost", this.server.getPort())) {
            stub.connect();
            stub.login("dispatch1", "pw-dispatch");
            Incident submitted = stub.submitIncident(HazardType.FLOOD,
                    -23.0, 150.0, "secret-description", 0);

            // Served decrypted to the authorised session, both on the submit echo
            // and on a genuine read-back through the server's DAO-read path.
            assertEquals("secret-description", submitted.getDescription());
            assertEquals("secret-description",
                    stub.listIncidents().get(0).getDescription());

            // The actual database row holds genuine, RECOVERABLE AES-GCM
            // ciphertext: decrypting it with the server's key yields the
            // plaintext (which simultaneously proves it is neither null, nor
            // empty, nor the plaintext itself, nor corrupted).
            String stored = new IncidentDaoImpl(this.database)
                    .findByUuid(submitted.getId()).orElseThrow().getDescription();
            assertTrue(!"secret-description".equals(stored),
                    "the database row must hold ciphertext, not the plaintext");
            assertEquals("secret-description", this.cipher.decrypt(stored),
                    "the stored value must decrypt back to the original plaintext");
        }
    }
}
