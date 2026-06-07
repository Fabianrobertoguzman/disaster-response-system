/**
 * Client tier of DRS-Enhanced: the {@link edu.cqu.drs.client.ServerStub} that the
 * JavaFX presenters use to reach the server. The stub owns the socket connection
 * and the {@link edu.cqu.drs.protocol} message exchange, exposing typed
 * operations so the presentation tier never sees the wire format.
 *
 * <p>This is what makes the client a true MVP client: View (FXML) and Presenter
 * run on the client and obtain all data through the stub, never touching the
 * database directly.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.client;
