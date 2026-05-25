package com.coit20258.drs.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import com.coit20258.drs.dao.UserDao;
import com.coit20258.drs.dao.UserDaoImpl;
import com.coit20258.drs.model.User;
import com.coit20258.drs.model.User.Role;
import com.coit20258.drs.util.PasswordUtil;
import com.coit20258.drs.util.SceneManager;

public class RegisterController implements Initializable {

    private static final Logger LOGGER
            = Logger.getLogger(RegisterController.class.getName());

    private static final Pattern EMAIL_PATTERN
            = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ComboBox<Role> roleCombo;

    // Per-field inline error labels
    @FXML
    private Label fullNameError;
    @FXML
    private Label usernameError;
    @FXML
    private Label emailError;
    @FXML
    private Label passwordError;
    @FXML
    private Label confirmPasswordError;

    // Global feedback banner (success / top-level error)
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button registerButton;

    private final UserDao userDao = new UserDaoImpl();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateRoleCombo();
        clearAllErrors();

        // Real-time uniqueness hints — fire on focus-lost to avoid spamming the DB
        usernameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                checkUsernameAvailability();
            }
        });
        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                checkEmailAvailability();
            }
        });

        // Live password-match hint
        confirmPasswordField.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty()) {
                validatePasswordMatch(false);
            }
        });
    }

    @FXML
    private void handleRegister() {
        clearAllErrors();
        hideFeedback();

        if (!validateAllFields()) {
            return;       // errors already displayed inline
        }

        registerButton.setDisable(true);
        registerButton.setText("Creating account…");

        new Thread(() -> {
            try {
                // ── Final uniqueness check on the DB thread ───────────────────
                boolean usernameTaken = userDao.usernameExists(usernameField.getText().trim());
                boolean emailTaken = userDao.emailExists(emailField.getText().trim());

                if (usernameTaken || emailTaken) {
                    Platform.runLater(() -> {
                        registerButton.setDisable(false);
                        registerButton.setText("Create Account");
                        if (usernameTaken) {
                            showFieldError(usernameError,
                                    "This username is already taken.");
                        }
                        if (emailTaken) {
                            showFieldError(emailError,
                                    "This email is already registered.");
                        }
                    });
                    return;
                }

                // ── Build and persist the user ────────────────────────────────
                String hashedPassword = PasswordUtil.hash(passwordField.getText());

                User newUser = new User(
                        usernameField.getText().trim(),
                        hashedPassword,
                        fullNameField.getText().trim(),
                        emailField.getText().trim().toLowerCase(),
                        roleCombo.getValue()
                );

                userDao.register(newUser);

                LOGGER.info("New user registered: " + newUser.getUsername()
                        + " (id=" + newUser.getUserId() + ")");

                // ── Navigate back to login with a success message ─────────────
                Platform.runLater(() -> {
                    LoginController loginCtrl
                            = SceneManager.switchToWithController("LoginView");
                    loginCtrl.showSuccess(
                            "✔  Account created! You can now sign in, "
                            + newUser.getUsername() + ".");
                });

            } catch (Exception ex) {
                LOGGER.severe("Registration error: " + ex.getMessage());
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Create Account");
                    showGlobalError("Registration failed: " + ex.getMessage());
                });
            }
        }, "register-thread").start();
    }

    /**
     * Resets all form fields and error labels.
     */
    @FXML
    private void handleClear() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleCombo.getSelectionModel().clearSelection();
        clearAllErrors();
        hideFeedback();
    }

    /**
     * Navigates back to the login screen without saving anything.
     */
    @FXML
    private void handleBackToLogin() {
        SceneManager.switchTo("LoginView");
    }

    /**
     * Runs all field validators in order.
     */
    private boolean validateAllFields() {
        boolean ok = true;

        // Full name
        if (fullNameField.getText().trim().isEmpty()) {
            showFieldError(fullNameError, "Full name is required.");
            ok = false;
        } else if (fullNameField.getText().trim().length() < 2) {
            showFieldError(fullNameError, "Full name must be at least 2 characters.");
            ok = false;
        }

        // Username
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showFieldError(usernameError, "Username is required.");
            ok = false;
        } else if (username.length() < 3) {
            showFieldError(usernameError, "Username must be at least 3 characters.");
            ok = false;
        } else if (!username.matches("[A-Za-z0-9_]+")) {
            showFieldError(usernameError,
                    "Only letters, numbers, and underscores allowed.");
            ok = false;
        }

        // Email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showFieldError(emailError, "Email address is required.");
            ok = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            showFieldError(emailError, "Enter a valid email address.");
            ok = false;
        }

        // Password
        String password = passwordField.getText();
        if (password.isEmpty()) {
            showFieldError(passwordError, "Password is required.");
            ok = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            showFieldError(passwordError,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            ok = false;
        }

        // Confirm password
        if (!validatePasswordMatch(true)) {
            ok = false;
        }

        // Role
        if (roleCombo.getValue() == null) {
            showGlobalError("Please select an account role.");
            ok = false;
        }

        return ok;
    }

    /**
     * Checks that the confirm-password field matches the password field.
     */
    private boolean validatePasswordMatch(boolean showIfBlank) {
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (confirm.isEmpty()) {
            if (showIfBlank) {
                showFieldError(confirmPasswordError, "Please confirm your password.");
                return false;
            }
            return true;    // not yet typed — don't flag yet
        }

        if (!pass.equals(confirm)) {
            showFieldError(confirmPasswordError, "Passwords do not match.");
            return false;
        }

        hideFieldError(confirmPasswordError);
        return true;
    }

    /**
     * Checks email availability on focus-lost.
     */
    private void checkEmailAvailability() {
        String email = emailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return;
        }

        new Thread(() -> {
            boolean taken = userDao.emailExists(email);
            Platform.runLater(() -> {
                if (taken) {
                    showFieldError(emailError, "Email already registered.");
                } else {
                    hideFieldError(emailError);
                }
            });
        }, "email-check-thread").start();
    }

    private void populateRoleCombo() {
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));

        roleCombo.setConverter(new StringConverter<Role>() {
            @Override
            public String toString(Role role) {
                if (role == null) {
                    return "";
                }
                return switch (role) {
                    case ADMIN ->
                        "Administrator";
                    case OPERATOR ->
                        "Operator";
                    case RESPONDER ->
                        "Field Responder";
                    case PUBLIC ->
                        "Public / Civilian";
                };
            }

            @Override
            public Role fromString(String s) {
                return null;
            }  // not needed for ComboBox
        });

        // Default selection — most registrations will be PUBLIC
        roleCombo.getSelectionModel().select(Role.PUBLIC);
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideFieldError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    private void clearAllErrors() {
        hideFieldError(fullNameError);
        hideFieldError(usernameError);
        hideFieldError(emailError);
        hideFieldError(passwordError);
        hideFieldError(confirmPasswordError);
    }

    private void showGlobalError(String message) {
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
