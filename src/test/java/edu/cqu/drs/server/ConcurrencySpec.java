package edu.cqu.drs.server;

import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency tests: many clients hit a real {@link DrsServer} at once and the
 * thread-safe data tier must lose no updates and never corrupt shared state.
 * Repeated (via {@link RepeatedTest}) because a race that only shows up
 * occasionally needs several attempts to surface.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("DrsServer - concurrent multi-client safety")
class ConcurrencySpec {

    private InMemoryDataStore store;
    private DrsServer server;

    @BeforeEach
    void startServer() throws IOException {
        this.store = new InMemoryDataStore();
        IncidentService service = new IncidentService(this.store.incidentDao(),
                this.store.responderDao(), this.store.auditDao(), new AlertTemplateRecommender());
        this.server = new DrsServer(0, new DrsRequestDispatcher(service));
        this.server.start();
    }

    @AfterEach
    void stopServer() {
        this.server.stop();
    }

    @RepeatedTest(5)
    @DisplayName("concurrent submissions from many clients lose no updates")
    void concurrentSubmissionsAreNotLost() throws InterruptedException, IOException {
        int clients = 8;
        int perClient = 12;
        runConcurrent(clients, (stub, index) -> {
            for (int k = 0; k < perClient; k++) {
                stub.submitIncident(HazardType.FIRE, -23.0, 150.0, "c" + index + "-" + k, k);
            }
        });
        try (ServerStub stub = connect()) {
            assertEquals(clients * perClient, stub.listIncidents().size());
        }
    }

    @RepeatedTest(5)
    @DisplayName("concurrent responder allocations to one incident are all applied under the lock")
    void concurrentAssignmentsAreConsistent() throws InterruptedException, IOException {
        UUID incidentId;
        try (ServerStub stub = connect()) {
            incidentId = stub.submitIncident(HazardType.FIRE, -23.0, 150.0, "Blaze", 1).getId();
        }
        int count = Incident.MAX_RESPONDERS;
        List<UUID> responderIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Responder responder = new Responder("Unit-" + i);
            this.store.responderDao().insert(responder);
            responderIds.add(responder.getId());
        }

        runConcurrent(count, (stub, index) ->
                stub.assignResponder(incidentId, responderIds.get(index)));

        assertEquals(count, this.store.incidentDao().findAssignedResponders(incidentId).size());
    }

    /**
     * A unit of work for one concurrent client over its own connected stub.
     */
    @FunctionalInterface
    private interface ClientTask {
        void run(ServerStub stub, int index) throws Exception;
    }

    /**
     * Runs {@code n} clients concurrently, each on its own connection, releasing
     * them simultaneously for maximum contention and failing if any throws.
     *
     * @param n    the number of concurrent clients.
     * @param task the work each client performs.
     * @throws InterruptedException if interrupted while waiting.
     */
    private void runConcurrent(int n, ClientTask task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < n; i++) {
            final int index = i;
            pool.execute(() -> {
                ServerStub stub = new ServerStub("localhost", this.server.getPort());
                try {
                    stub.connect();
                } catch (Throwable connectFailure) {
                    failures.add(connectFailure);
                    ready.countDown();
                    done.countDown();
                    return;
                }
                ready.countDown();
                try {
                    go.await();
                    task.run(stub, index);
                } catch (Throwable failure) {
                    failures.add(failure);
                } finally {
                    stub.close();
                    done.countDown();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "clients did not all connect in time");
        go.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "clients did not finish in time");
        pool.shutdownNow();
        assertTrue(failures.isEmpty(), "client failures: " + failures);
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
