/**
 * Server tier of DRS-Enhanced: the multi-threaded {@link
 * edu.cqu.drs.server.DrsServer} (a {@link java.net.ServerSocket} accept-loop with
 * one pooled {@link edu.cqu.drs.server.ClientHandler} per connected client) and
 * the {@link edu.cqu.drs.server.RequestDispatcher} that routes each protocol
 * {@link edu.cqu.drs.protocol.Request} to the business {@link
 * edu.cqu.drs.server.service service} layer.
 *
 * <p>The server is the system's serialisation point for concurrent access: worker
 * threads share one stateless dispatcher and reach shared state only through the
 * thread-safe data tier (a fresh JDBC connection per request, or the lock-guarded
 * in-memory store under test).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.server;
