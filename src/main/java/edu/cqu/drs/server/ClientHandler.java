package edu.cqu.drs.server;

import edu.cqu.drs.protocol.Request;
import edu.cqu.drs.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Handles one connected client on its own worker thread: a read-dispatch-write
 * loop that decodes {@link Request}s, asks the shared {@link RequestDispatcher}
 * for a {@link Response}, and writes it back, until the client disconnects.
 *
 * <p><strong>Handshake order (deadlock avoidance):</strong> the
 * {@link ObjectOutputStream} is created and {@link ObjectOutputStream#flush()
 * flushed} <em>before</em> the {@link ObjectInputStream} is opened. An
 * {@code ObjectInputStream} constructor blocks until it has read the peer's
 * stream header; if both ends opened their input stream first, neither would ever
 * send its header and both would hang. The client mirrors this exact order.</p>
 *
 * <p>{@link ObjectOutputStream#reset()} is called after each write so the stream
 * does not keep back-references to previously written objects (which would make a
 * later mutated-then-resent object serialise stale).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestDispatcher dispatcher;

    /**
     * Creates a handler for one client socket.
     *
     * @param socket     the accepted client socket (must not be null).
     * @param dispatcher the shared request dispatcher (must not be null).
     * @throws IllegalArgumentException if either argument is null.
     */
    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        if (socket == null || dispatcher == null) {
            throw new IllegalArgumentException("socket and dispatcher are required");
        }
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try (Socket client = this.socket) {
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            serve(in, out);
        } catch (IOException ex) {
            System.err.println("Client handler ended on I/O error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            // A malformed stream or unexpected runtime fault ends this one client's
            // session cleanly; it must never propagate and take the server down.
            System.err.println("Client handler ended on unexpected error: " + ex.getMessage());
        }
    }

    /**
     * Runs the read-dispatch-write loop until the client disconnects.
     *
     * @param in  the object input stream from the client.
     * @param out the object output stream to the client.
     * @throws IOException if a non-recoverable stream error occurs.
     */
    private void serve(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        while (true) {
            Object message;
            try {
                message = in.readObject();
            } catch (EOFException endOfStream) {
                return;
            } catch (ClassNotFoundException ex) {
                out.writeObject(Response.badRequest("unknown message type: " + ex.getMessage()));
                out.flush();
                out.reset();
                continue;
            }
            Response response = (message instanceof Request)
                    ? this.dispatcher.handle((Request) message)
                    : Response.badRequest("expected a Request, got "
                            + (message == null ? "null" : message.getClass().getName()));
            out.writeObject(response);
            out.flush();
            out.reset();
        }
    }
}
