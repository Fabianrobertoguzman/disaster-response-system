package edu.cqu.drs.server;

import edu.cqu.drs.protocol.Request;
import edu.cqu.drs.protocol.Response;

/**
 * Turns a decoded {@link Request} into a {@link Response}. One dispatcher
 * instance is shared by every {@link ClientHandler} (one per connected client),
 * so an implementation must be safe for concurrent use.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface RequestDispatcher {

    /**
     * Handles one request and returns the response to send back.
     *
     * @param request the decoded request (never null).
     * @return the response (never null).
     */
    Response handle(Request request);
}
