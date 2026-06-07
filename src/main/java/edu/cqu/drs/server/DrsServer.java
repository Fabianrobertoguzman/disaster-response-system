package edu.cqu.drs.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The multi-threaded DRS-Enhanced server: a {@link ServerSocket} accept-loop that
 * hands each accepted connection to a {@link ClientHandler} on a pooled worker
 * thread, so many clients are served concurrently. One shared
 * {@link RequestDispatcher} backs every handler.
 *
 * <p>The accept-loop runs on its own daemon thread; {@link #start()} binds the
 * socket synchronously so {@link #getPort()} is valid as soon as it returns
 * (binding to port 0 picks a free ephemeral port, which the integration and
 * concurrency tests rely on). {@link #stop()} closes the socket - unblocking the
 * accept-loop - and shuts the worker pool down.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DrsServer implements AutoCloseable {

    /** Default listening port when none is supplied. */
    public static final int DEFAULT_PORT = 5599;

    private final int requestedPort;
    private final RequestDispatcher dispatcher;

    private ServerSocket serverSocket;
    private ExecutorService workerPool;
    private Thread acceptThread;
    private volatile boolean running;

    /**
     * Creates a server bound (on {@link #start()}) to the given port.
     *
     * @param port       the port to listen on; 0 selects a free ephemeral port.
     * @param dispatcher the shared request dispatcher (must not be null).
     * @throws IllegalArgumentException if {@code dispatcher} is null.
     */
    public DrsServer(int port, RequestDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.requestedPort = port;
        this.dispatcher = dispatcher;
    }

    /**
     * Binds the listening socket and starts accepting clients.
     *
     * @throws IOException if the socket cannot be bound.
     */
    public synchronized void start() throws IOException {
        if (this.running) {
            return;
        }
        this.serverSocket = new ServerSocket(this.requestedPort);
        this.workerPool = Executors.newCachedThreadPool();
        this.running = true;
        this.acceptThread = new Thread(this::acceptLoop, "drs-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    /**
     * The accept-loop: accept a connection, hand it to a pooled worker, repeat
     * until the server is stopped (closing the socket makes {@code accept()}
     * throw, which ends the loop).
     */
    private void acceptLoop() {
        while (this.running) {
            try {
                Socket client = this.serverSocket.accept();
                this.workerPool.execute(new ClientHandler(client, this.dispatcher));
            } catch (IOException ex) {
                if (this.running) {
                    System.err.println("Accept failed: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * @return the actual bound port (valid after {@link #start()}); useful when
     *         port 0 was requested.
     */
    public int getPort() {
        return (this.serverSocket != null) ? this.serverSocket.getLocalPort() : this.requestedPort;
    }

    /** @return true if the server is currently accepting connections. */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Stops accepting clients, closes the listening socket and shuts the worker
     * pool down. Safe to call more than once.
     */
    public synchronized void stop() {
        this.running = false;
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException ex) {
            System.err.println("Error closing server socket: " + ex.getMessage());
        }
        if (this.workerPool != null) {
            this.workerPool.shutdownNow();
        }
    }

    /** Calls {@link #stop()} so the server can be used in a try-with-resources block. */
    @Override
    public void close() {
        stop();
    }
}
