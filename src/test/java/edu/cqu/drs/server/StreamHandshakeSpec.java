package edu.cqu.drs.server;

import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.server.service.IncidentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins the object-stream handshake order that prevents the classic paired-
 * stream deadlock: each peer must construct and flush its
 * {@link java.io.ObjectOutputStream} <em>before</em> opening its
 * {@link java.io.ObjectInputStream}, because the input constructor blocks until
 * the other side's stream header arrives.
 *
 * <p>The proof is a raw socket that sends <strong>nothing</strong>: if the
 * server honours the output-first order, its serialization stream header
 * (magic {@code 0xACED}, version {@code 5}) arrives unprompted; a server that
 * opened its input first would sit waiting for our header and the read below
 * would time out. The client side of the same order is exercised by every
 * {@code ServerStub} spec in the suite.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Object-stream handshake - the server writes its header first")
class StreamHandshakeSpec {

    /** The java.io serialization stream magic number. */
    private static final int STREAM_MAGIC = 0xACED;

    /** The java.io serialization stream version. */
    private static final int STREAM_VERSION = 5;

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

    @Test
    @DisplayName("a silent client still receives the server's stream header (no deadlock possible)")
    void shouldReceiveServerHeaderUnprompted() throws IOException {
        try (Socket raw = new Socket("localhost", this.server.getPort())) {
            // If the server tried to read our header first, this read would hang;
            // the timeout turns that regression into a clear, named failure.
            raw.setSoTimeout(5_000);
            DataInputStream in = new DataInputStream(raw.getInputStream());
            try {
                assertEquals(STREAM_MAGIC, in.readUnsignedShort(),
                        "the server must send its ObjectOutputStream header unprompted");
                assertEquals(STREAM_VERSION, in.readUnsignedShort(),
                        "the header must carry the java.io serialization version");
            } catch (java.net.SocketTimeoutException deadlocked) {
                fail("no stream header arrived within 5s - the server may be "
                        + "opening its ObjectInputStream before constructing and "
                        + "flushing its ObjectOutputStream (the paired-stream "
                        + "handshake deadlock)");
            }
        }
    }
}
