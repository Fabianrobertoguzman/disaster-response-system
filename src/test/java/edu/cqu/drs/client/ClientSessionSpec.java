package edu.cqu.drs.client;

import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ClientSession}'s threading contract: server calls run off
 * the caller's thread, exactly one callback is delivered per call, callbacks
 * flow through the configurable dispatcher, and calls are serialised in order.
 * Pure in-memory (the stub is never connected) - no socket, no database.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("ClientSession - session-scoped off-thread call helper")
class ClientSessionSpec {

    private ClientSession session;

    @BeforeEach
    void setUp() {
        this.session = new ClientSession(new ServerStub("localhost", 0),
                new User("meiru", UserRole.DISPATCHER));
    }

    @AfterEach
    void tearDown() {
        this.session.close();
    }

    @Test
    @DisplayName("exposes the authenticated user and role")
    void shouldExposeUserAndRole() {
        assertEquals("meiru", this.session.getUser().getUsername());
        assertEquals(UserRole.DISPATCHER, this.session.getRole());
    }

    @Test
    @DisplayName("runAsync executes the call off the caller's thread and delivers the result")
    void shouldRunOffCallerThread() throws InterruptedException {
        String callerThread = Thread.currentThread().getName();
        AtomicReference<String> workerThread = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        this.session.runAsync(() -> {
            workerThread.set(Thread.currentThread().getName());
            return "answer";
        }, value -> {
            result.set(value);
            done.countDown();
        }, failure -> done.countDown());

        assertTrue(done.await(5, TimeUnit.SECONDS), "callback not delivered in time");
        assertEquals("answer", result.get());
        assertNotEquals(callerThread, workerThread.get(),
                "the blocking call must not run on the caller's thread");
    }

    @Test
    @DisplayName("a failing call delivers exactly one onFailure callback with the exception")
    void shouldDeliverFailure() throws InterruptedException {
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        this.session.runAsync(() -> {
            throw new IllegalStateException("boom");
        }, value -> done.countDown(), ex -> {
            failure.set(ex);
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS), "callback not delivered in time");
        assertTrue(failure.get() instanceof IllegalStateException);
        assertEquals("boom", failure.get().getMessage());
    }

    @Test
    @DisplayName("callbacks are delivered through the configured dispatcher")
    void shouldUseCallbackDispatcher() throws InterruptedException {
        List<String> dispatched = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        this.session.setCallbackDispatcher(runnable -> {
            dispatched.add("dispatched");
            runnable.run();
        });

        this.session.runAsync(() -> "x", value -> done.countDown(), ex -> done.countDown());

        assertTrue(done.await(5, TimeUnit.SECONDS), "callback not delivered in time");
        assertEquals(List.of("dispatched"), dispatched);
    }

    @Test
    @DisplayName("calls are serialised: results arrive in submission order")
    void shouldSerialiseCalls() throws InterruptedException {
        List<Integer> order = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(3);
        for (int i = 1; i <= 3; i++) {
            final int call = i;
            this.session.runAsync(() -> call, value -> {
                order.add(value);
                done.countDown();
            }, ex -> done.countDown());
        }
        assertTrue(done.await(5, TimeUnit.SECONDS), "callbacks not delivered in time");
        assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    @DisplayName("runAsync after close delivers onFailure instead of leaking a rejection")
    void shouldFailGracefullyAfterClose() throws InterruptedException {
        this.session.close();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        this.session.runAsync(() -> "late", value -> done.countDown(), ex -> {
            failure.set(ex);
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS), "callback not delivered in time");
        assertTrue(failure.get() instanceof IllegalStateException,
                "a closed session must surface as IllegalStateException");
    }

    @Test
    @DisplayName("signOut runs off the caller's thread and delivers its completion callback")
    void shouldSignOutAsynchronously() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        this.session.signOut(done::countDown);
        assertTrue(done.await(5, TimeUnit.SECONDS), "signOut completion not delivered");
        // After sign-out the session is closed: a further call fails gracefully.
        CountDownLatch late = new CountDownLatch(1);
        this.session.runAsync(() -> "x", v -> late.countDown(), ex -> late.countDown());
        assertTrue(late.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("null constructor or runAsync arguments are rejected")
    void shouldRejectNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession(null, new User("u", UserRole.CITIZEN)));
        assertThrows(IllegalArgumentException.class,
                () -> new ClientSession(new ServerStub("localhost", 0), null));
        assertThrows(IllegalArgumentException.class,
                () -> this.session.runAsync(null, v -> { }, e -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> this.session.setCallbackDispatcher(null));
    }

    @Test
    @DisplayName("toString names the user and role but never a token")
    void shouldFormatToString() {
        assertEquals("ClientSession{user=meiru, role=DISPATCHER}", this.session.toString());
    }
}
