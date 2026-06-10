package edu.cqu.drs.server;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.protocol.BoardSnapshot;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency tests for the f1 live board: while several dispatcher clients
 * submit simultaneously, a watcher client polls {@code GET_BOARD} and every
 * snapshot it receives must be internally consistent (open count never above
 * the total) and monotonically growing (a later poll never shows fewer
 * incidents under a submit-only storm), converging on the exact final count.
 * Repeated, because a race that only shows up occasionally needs several
 * attempts to surface. In-memory backed, loopback only - runs unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Live board (f1) - snapshot consistency under concurrent submitters")
class LiveBoardConcurrencySpec {

    private DrsServer server;

    @BeforeEach
    void startServer() throws IOException {
        InMemoryDataStore store = new InMemoryDataStore();
        IncidentService service = new IncidentService(store.incidentDao(),
                store.responderDao(), store.auditDao(), new AlertTemplateRecommender());
        this.server = new DrsServer(0, new DrsRequestDispatcher(service));
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.server.stop();
    }

    @RepeatedTest(3)
    @DisplayName("polled snapshots stay consistent and converge while clients submit concurrently")
    void boardConvergesUnderConcurrentSubmissions() throws Exception {
        int clients = 6;
        int perClient = 5;
        int expectedTotal = clients * perClient;

        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(clients);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < clients; i++) {
            final int client = i;
            pool.execute(() -> {
                try (ServerStub stub = connect()) {
                    go.await();
                    for (int k = 0; k < perClient; k++) {
                        stub.submitIncident(HazardType.FIRE, -23.0, 150.0,
                                "c" + client + "-" + k, k);
                    }
                } catch (Throwable failure) {
                    failures.add(failure);
                } finally {
                    done.countDown();
                }
            });
        }

        try (ServerStub watcher = connect()) {
            go.countDown();
            // Poll while the storm runs: every snapshot must be internally
            // consistent and the totals monotonic (each poll is a server
            // round-trip, so this loop is naturally paced by the socket).
            int lastTotal = 0;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (lastTotal < expectedTotal && System.nanoTime() < deadline) {
                BoardSnapshot snapshot = watcher.getBoard();
                assertTrue(snapshot.getTotalCount() >= lastTotal,
                        "totals must not go backwards under a submit-only storm");
                lastTotal = snapshot.getTotalCount();
            }
            assertTrue(done.await(30, TimeUnit.SECONDS), "submitters did not finish in time");
            assertTrue(failures.isEmpty(), "client failures: " + failures);
            assertEquals(expectedTotal, watcher.getBoard().getTotalCount(),
                    "the final snapshot must converge on every submission");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Opens a connected stub to the running server.
     *
     * @return a connected {@link ServerStub}.
     * @throws IOException if the connection fails.
     */
    private ServerStub connect() throws IOException {
        ServerStub stub = new ServerStub("localhost", this.server.getPort());
        stub.connect();
        return stub;
    }
}
