package edu.cqu.drs.client;

import edu.cqu.drs.model.User;

/**
 * Client-side presenter for the login use case (MVP): it takes the credentials
 * captured by the login view and authenticates them through the
 * {@link ServerStub}, returning the authenticated {@link User} so the view can
 * route to a role-appropriate screen.
 *
 * <p>Keeping this off the FXML controller follows the same MVP split as the rest
 * of the client and makes the use case testable against a live server without a
 * running JavaFX toolkit.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class LoginPresenter {

    private final ServerStub serverStub;

    /**
     * Creates a login presenter over a (connected) server stub.
     *
     * @param serverStub the server gateway (must not be null).
     * @throws IllegalArgumentException if {@code serverStub} is null.
     */
    public LoginPresenter(ServerStub serverStub) {
        if (serverStub == null) {
            throw new IllegalArgumentException("serverStub must not be null");
        }
        this.serverStub = serverStub;
    }

    /**
     * Authenticates the user; the stub stores the session token on success.
     *
     * @param username the login name.
     * @param password the password.
     * @return the authenticated user.
     * @throws ServerStubException if the credentials are rejected or the server
     *         is unreachable.
     */
    public User authenticate(String username, String password) {
        return this.serverStub.login(username, password).getUser();
    }
}
