package com.coit20258.drs.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import com.coit20258.drs.dao.UserDao;
import com.coit20258.drs.dao.UserDaoImpl;
import com.coit20258.drs.model.User;
import com.coit20258.drs.util.SceneManager;

public class LoginController implements Initializable {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Label feedbackLabel;

    private final UserDao userDao = new UserDaoImpl();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.hideFeedback();

        // Move focus to password when Enter is pressed in the username field
        // emailField.setOnAction(e -> passwordField.requestFocus());
    }

    /**
     * Triggered by the Sign In button or pressing Enter in either field.
     * Validates input, authenticates via the DAO, then navigates on success.
     */
    @FXML
    private void handleLogin() {
        hideFeedback();

        String email = emailField.getText().trim();
        String password = passwordField.getText();       // do NOT trim passwords

        // ── Client-side validation ────────────────────────────────────────────
        if (email.isEmpty()) {
            showError("Email is required.");
            emailField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required.");
            passwordField.requestFocus();
            return;
        }

        // ── Disable button to prevent double-clicks ───────────────────────────
        loginButton.setDisable(true);
        loginButton.setText("Signing in…");

        // ── Run DB call off the FX thread ─────────────────────────────────────
        new Thread(() -> {
            try {
                Optional<User> result = userDao.login(email, password);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");

                    if (result.isPresent()) {
//                        onLoginSuccess(result.get());
                    } else {
                        onLoginFailure();
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                    showError("A system error occurred. Please try again.");
                });
            }
        }, "login-thread").start();
    }

    /**
     * Navigates to the registration screen.
     */
   @FXML
   private void handleGoToRegister() {
       SceneManager.switchTo("RegisterView");
   }

    /**
     * Stores the user in the session and routes to the appropriate dashboard
     * based on their role.
     */
//    private void onLoginSuccess(User user) {
//        SessionContext.setCurrentUser(user);
//
//        switch (user.getRole()) {
//            case ADMIN:
//            case OPERATOR:
//                SceneManager.switchTo("DashboardView");
//                break;
//            case RESPONDER:
//                SceneManager.switchTo("DashboardView");
//                break;
//            case PUBLIC:
//            default:
//                SceneManager.switchTo("DashboardView");
//                break;
//        }
//    }

    /**
     * Shows a generic "invalid credentials" message — no specifics to prevent
     * enumeration.
     */
    private void onLoginFailure() {
        passwordField.clear();
        passwordField.requestFocus();
        showError("Invalid username or password. Please try again.");
    }

    /**
     * Shows an error banner and makes it take up layout space.
     */
    private void showError(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("success-label");
        if (!feedbackLabel.getStyleClass().contains("validation-label")) {
            feedbackLabel.getStyleClass().add("validation-label");
        }
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    /**
     * Shows a success banner (used after registration redirect).
     */
    public void showSuccess(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("validation-label");
        if (!feedbackLabel.getStyleClass().contains("success-label")) {
            feedbackLabel.getStyleClass().add("success-label");
        }
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    private void hideFeedback() {
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        feedbackLabel.setText("");
    }
}
