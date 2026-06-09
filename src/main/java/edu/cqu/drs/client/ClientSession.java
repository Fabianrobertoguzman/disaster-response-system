package edu.cqu.drs.client;

import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The client's session-scoped state after a successful login: the single
 * connected {@link ServerStub}, the authenticated {@link User}, and a one-thread
 * executor through which every server call is made off the caller's thread.
 *
 * <p>One {@code ClientSession} exists per running client, created by the login
 * flow and shared by all views, so the whole application talks to the server
 * over <em>one</em> connection with <em>one</em> session token. {@link
 * #runAsync(Supplier, Consumer, Consumer)} is the single off-thread helper the
 * controllers use: the blocking {@link ServerStub} call runs on the session's
 * background thread, and the success/failure callback is delivered through the
 * configurable {@link #setCallbackDispatcher(Consumer) callback dispatcher}.
 * The dispatcher defaults to running callbacks inline (right for tests); the
 * JavaFX shell sets it to {@code Platform::runLater} once, so every view's UI
 * updates land on the FX Application Thread without each controller having to
 * remember to hop threads.</p>
 *
 * <p>The single-thread executor also serialises the client's requests, which
 * matches the one-request-at-a-time nature of the underlying synchronised
 * stub - a later call simply queues instead of contending.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class ClientSession implements AutoCloseable {

    /** The single connected server gateway for this session. */
    private final ServerStub serverStub;

    /** The authenticated user (no credential material). */
    private final User user;

    /** Runs every server call off the caller's thread, one at a time. */
    private final ExecutorService executor;

    /** Delivers callbacks; the JavaFX shell points this at Platform::runLater. */
    private volatile Consumer<Runnable> callbackDispatcher = Runnable::run;

    /**
     * Creates a session over an already-connected, logged-in stub.
     *
     * @param serverStub the connected server gateway (must not be null).
     * @param user       the authenticated user (must not be null).
     * @throws IllegalArgumentException if either argument is null.
     */
    public ClientSession(ServerStub serverStub, User user) {
        if (serverStub == null || user == null) {
            throw new IllegalArgumentException("serverStub and user are required");
        }
        this.serverStub = serverStub;
        this.user = user;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "drs-client-io");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** @return the session's connected server gateway. */
    public ServerStub getServerStub() {
        return this.serverStub;
    }

    /** @return the authenticated user. */
    public User getUser() {
        return this.user;
    }

    /** @return the authenticated user's role. */
    public UserRole getRole() {
        return this.user.getRole();
    }

    /**
     * Replaces the callback dispatcher. The JavaFX shell calls this once with
     * {@code Platform::runLater} so every {@link #runAsync} callback runs on the
     * FX Application Thread; tests keep the inline default.
     *
     * @param callbackDispatcher the dispatcher (must not be null).
     * @throws IllegalArgumentException if {@code callbackDispatcher} is null.
     */
    public void setCallbackDispatcher(Consumer<Runnable> callbackDispatcher) {
        if (callbackDispatcher == null) {
            throw new IllegalArgumentException("callbackDispatcher must not be null");
        }
        this.callbackDispatcher = callbackDispatcher;
    }

    /**
     * Runs a blocking server call on the session's background thread and
     * delivers exactly one callback - {@code onSuccess} with the result, or
     * {@code onFailure} with the exception - through the callback dispatcher.
     *
     * @param <T>       the call's result type.
     * @param call      the blocking call (must not be null).
     * @param onSuccess invoked with the result on success (must not be null).
     * @param onFailure invoked with the failure on error (must not be null).
     * @throws IllegalArgumentException if any argument is null.
     */
    public <T> void runAsync(Supplier<T> call, Consumer<T> onSuccess,
            Consumer<RuntimeException> onFailure) {
        if (call == null || onSuccess == null || onFailure == null) {
            throw new IllegalArgumentException("call, onSuccess and onFailure are required");
        }
        try {
            this.executor.execute(() -> {
                T result;
                try {
                    result = call.get();
                } catch (RuntimeException ex) {
                    this.callbackDispatcher.accept(() -> onFailure.accept(ex));
                    return;
                }
                this.callbackDispatcher.accept(() -> onSuccess.accept(result));
            });
        } catch (RejectedExecutionException sessionClosed) {
            // The session was closed (sign-out / window close); preserve the
            // exactly-one-callback contract instead of leaking the executor's
            // rejection to the caller.
            this.callbackDispatcher.accept(() -> onFailure.accept(
                    new IllegalStateException("the session is closed", sessionClosed)));
        }
    }

    /**
     * Signs out without blocking the calling thread: the best-effort server
     * logout and the connection close run on the session's background thread,
     * and {@code onComplete} is then delivered through the callback dispatcher.
     * This is the path the UI's Sign out / window-close handlers use - a direct
     * {@link #logout()} + {@link #close()} would perform a network round-trip on
     * the caller's (FX) thread and could wait behind an in-flight call.
     *
     * @param onComplete invoked once the session is signed out and closed (must
     *                   not be null).
     * @throws IllegalArgumentException if {@code onComplete} is null.
     */
    public void signOut(Runnable onComplete) {
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete must not be null");
        }
        try {
            this.executor.execute(() -> {
                logout();
                this.serverStub.close();
                this.executor.shutdown();
                this.callbackDispatcher.accept(onComplete);
            });
        } catch (RejectedExecutionException alreadyClosed) {
            this.callbackDispatcher.accept(onComplete);
        }
    }

    /**
     * Ends the server session (best effort): tells the server to invalidate the
     * token and clears it locally. A transport failure here is swallowed - the
     * user is signing out, and the connection is about to be closed anyway.
     *
     * <p><strong>Blocking:</strong> this performs a network round-trip on the
     * calling thread and may wait behind an in-flight call. UI code uses
     * {@link #signOut(Runnable)} instead.</p>
     */
    public void logout() {
        try {
            this.serverStub.logout();
        } catch (RuntimeException ex) {
            this.serverStub.setToken(null);
        }
    }

    /**
     * Releases the session's resources: stops the background thread and closes
     * the connection. Safe to call more than once. Call {@link #logout()} first
     * when the sign-out should reach the server.
     *
     * <p><strong>Blocking:</strong> closing the stub contends on its monitor
     * with any in-flight call. UI code uses {@link #signOut(Runnable)}, which
     * performs the close on the session's background thread instead.</p>
     */
    @Override
    public void close() {
        this.executor.shutdown();
        this.serverStub.close();
    }

    /**
     * Hand-written display string (no token, no credential material).
     *
     * @return e.g. {@code ClientSession{user=meiru, role=DISPATCHER}}.
     */
    @Override
    public String toString() {
        return "ClientSession{user=" + this.user.getUsername()
                + ", role=" + this.user.getRole() + "}";
    }
}
