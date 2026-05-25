package com.coit20258.drs.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

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
import com.coit20258.drs.util.SessionContext;

public class LoginController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    // ── FXML fields ────────────────────────────────────────────────────────
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button loginButton;

    private final UserDao userDao = new UserDaoImpl();

    // ── Initialisation ─────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideFeedback();
        emailField.setOnAction(e -> passwordField.requestFocus());
    }

    // ── FXML handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleLogin() {
        hideFeedback();

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Email address is required.");
            emailField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required.");
            passwordField.requestFocus();
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in…");

        new Thread(() -> {
            try {
                Optional<User> result = userDao.login(email, password);
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                    if (result.isPresent()) {
                        onLoginSuccess(result.get());
                    } else {
                        onLoginFailure();
                    }
                });
            } catch (Exception ex) {
                LOGGER.severe("Login error: " + ex.getMessage());
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                    showError("A system error occurred. Please try again.");
                });
            }
        }, "login-thread").start();
    }

    @FXML
    private void handleGoToRegister() {
        SceneManager.switchTo("RegisterView");
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private void onLoginSuccess(User user) {
        SessionContext.setCurrentUser(user);
        LOGGER.info("Login success — " + user.getEmail() + " role=" + user.getRole());
        SceneManager.switchContent("DisasterReportListView");
    }

    private void onLoginFailure() {
        passwordField.clear();
        passwordField.requestFocus();
        showError("Invalid email or password. Please try again.");
    }

    public void showSuccess(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("validation-label");
        if (!feedbackLabel.getStyleClass().contains("success-label")) {
            feedbackLabel.getStyleClass().add("success-label");
        }
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    private void showError(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("success-label");
        if (!feedbackLabel.getStyleClass().contains("validation-label")) {
            feedbackLabel.getStyleClass().add("validation-label");
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
