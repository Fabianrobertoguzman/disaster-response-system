package edu.cqu.drs.view;

import edu.cqu.drs.client.ClientSession;
import edu.cqu.drs.client.LoginPresenter;
import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.client.ServerStubException;
import edu.cqu.drs.model.User;
import edu.cqu.drs.server.DrsServer;

import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the login view ({@code login.fxml}) - the client's
 * entry surface for the §2.5 access-rights measure, and the place the
 * application's single server connection is born.
 *
 * <p>On Sign In it connects a {@link ServerStub} and authenticates through a
 * {@link LoginPresenter} <em>off the FX Application Thread</em> (a dead server
 * must not freeze the button for the socket timeout). On success the stub is
 * kept open, wrapped in a {@link ClientSession}, and handed to the success
 * callback the application shell registered - the session then backs every
 * view until sign-out. On failure the stub is closed and a non-sensitive
 * message is shown.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label statusLabel;

    /** Server host the client connects to. */
    private String host = "localhost";

    /** Server port the client connects to. */
    private int port = DrsServer.DEFAULT_PORT;

    /** Invoked on the FX thread with the established session after a login. */
    private Consumer<ClientSession> onLoginSuccess;

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
     * Registers the callback the application shell uses to take over after a
     * successful login. It is invoked on the FX Application Thread with a
     * connected, authenticated {@link ClientSession}.
     *
     * @param onLoginSuccess the success callback (may be null, in which case the
     *                       view only reports the outcome).
     */
    public void setOnLoginSuccess(Consumer<ClientSession> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    /**
     * Handles the Sign In button: connects and authenticates on a background
     * thread, then either hands the open session to the shell or shows an error.
     */
    @FXML
    private void onLogin() {
        String username = this.usernameField.getText();
        String password = this.passwordField.getText();
        if (username == null || username.isBlank()) {
            showError("Enter a username.");
            return;
        }
        this.loginButton.setDisable(true);
        this.statusLabel.getStyleClass().remove("status-error");
        this.statusLabel.setText("Connecting to " + this.host + ":" + this.port + "...");

        Thread loginThread = new Thread(
                () -> attemptLogin(username, password), "drs-login");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    /**
     * Connects and authenticates (runs on the background login thread); reports
     * the outcome back on the FX thread.
     *
     * @param username the entered login name.
     * @param password the entered password.
     */
    private void attemptLogin(String username, String password) {
        ServerStub stub = new ServerStub(this.host, this.port);
        try {
            stub.connect();
            User user = new LoginPresenter(stub).authenticate(username, password);
            ClientSession session = new ClientSession(stub, user);
            Platform.runLater(() -> onAuthenticated(session));
        } catch (IOException ex) {
            stub.close();
            Platform.runLater(() -> showErrorAndReenable("Cannot reach the server on "
                    + this.host + ":" + this.port + ". Is it running?"));
        } catch (ServerStubException ex) {
            stub.close();
            Platform.runLater(() -> showErrorAndReenable("Login failed: " + ex.getMessage()));
        } catch (RuntimeException ex) {
            // Any unexpected fault must still close the connection and revive
            // the Sign In button - never a leaked stub or a dead login screen.
            stub.close();
            Platform.runLater(() -> showErrorAndReenable(
                    "Unexpected error during login: " + ex.getMessage()));
        }
    }

    /**
     * Completes a successful login on the FX thread: clears the password and
     * hands the session to the shell (or just reports the role when no shell
     * callback is registered).
     *
     * @param session the connected, authenticated session.
     */
    private void onAuthenticated(ClientSession session) {
        this.passwordField.clear();
        this.loginButton.setDisable(false);
        if (this.onLoginSuccess != null) {
            try {
                this.onLoginSuccess.accept(session);
            } catch (RuntimeException ex) {
                // A failed takeover must not leak the open connection.
                session.close();
                showError("Could not open the application: " + ex.getMessage());
            }
            return;
        }
        this.statusLabel.setText("Signed in as " + session.getUser().getUsername()
                + " (" + session.getRole() + ").");
        session.close();
    }

    /**
     * Shows an error and re-enables the Sign In button (FX thread).
     *
     * @param message the message to display.
     */
    private void showErrorAndReenable(String message) {
        this.loginButton.setDisable(false);
        showError(message);
    }

    /**
     * Shows an error message in the status label.
     *
     * @param message the message to display.
     */
    private void showError(String message) {
        if (!this.statusLabel.getStyleClass().contains("status-error")) {
            this.statusLabel.getStyleClass().add("status-error");
        }
        this.statusLabel.setText(message);
    }
}
