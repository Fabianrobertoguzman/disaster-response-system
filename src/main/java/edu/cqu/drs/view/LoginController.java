package edu.cqu.drs.view;

import edu.cqu.drs.client.LoginPresenter;
import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.client.ServerStubException;
import edu.cqu.drs.model.User;
import edu.cqu.drs.server.DrsServer;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the login view ({@code login.fxml}) - the client-side
 * entry surface for the §2.5 access-rights measure.
 *
 * <p>It captures the username and password, opens a short-lived {@link ServerStub}
 * connection to the server, and delegates authentication to a
 * {@link LoginPresenter}. On success it reports the signed-in role; on failure it
 * shows a non-sensitive error. (Wiring a successful login through to the rest of
 * the client's screens is the remaining GUI-integration step.)</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    /** Server host the client connects to. */
    private String host = "localhost";

    /** Server port the client connects to. */
    private int port = DrsServer.DEFAULT_PORT;

    /**
     * Overrides the server endpoint (used when the server runs on a non-default
     * host/port).
     *
     * @param host the server host.
     * @param port the server port.
     */
    public void setServerEndpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Handles the Sign In button: authenticates and reports the outcome.
     */
    @FXML
    private void onLogin() {
        String username = this.usernameField.getText();
        String password = this.passwordField.getText();
        if (username == null || username.isBlank()) {
            showError("Enter a username.");
            return;
        }
        try (ServerStub stub = new ServerStub(this.host, this.port)) {
            stub.connect();
            User user = new LoginPresenter(stub).authenticate(username, password);
            this.statusLabel.getStyleClass().remove("error");
            this.statusLabel.setText(
                    "Signed in as " + user.getUsername() + " (" + user.getRole() + ").");
        } catch (IOException ex) {
            showError("Cannot reach the server on " + this.host + ":" + this.port
                    + ". Is it running?");
        } catch (ServerStubException ex) {
            showError("Login failed: " + ex.getMessage());
        }
    }

    /**
     * Shows an error message in the status label.
     *
     * @param message the message to display.
     */
    private void showError(String message) {
        if (!this.statusLabel.getStyleClass().contains("error")) {
            this.statusLabel.getStyleClass().add("error");
        }
        this.statusLabel.setText(message);
    }
}
