package com.coit20258.drs.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import com.coit20258.drs.dao.DisasterReportDao;
import com.coit20258.drs.dao.DisasterReportDaoImpl;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.User;
import com.coit20258.drs.util.SceneManager;
import com.coit20258.drs.util.SessionContext;

public class DisasterReportFormController implements Initializable {

    private static final Logger LOGGER
            = Logger.getLogger(DisasterReportFormController.class.getName());

    // ── FXML — reporter card (read-only display) ───────────────────────────
    @FXML
    private Label reporterNameLabel;
    @FXML
    private Label reporterRoleLabel;
    @FXML
    private Label reporterEmailLabel;

    // ── FXML — form inputs ─────────────────────────────────────────────────
    @FXML
    private ComboBox<String> disasterTypeCombo;
    @FXML
    private ComboBox<String> severityCombo;
    @FXML
    private TextField locationField;
    @FXML
    private TextArea descriptionArea;

    // ── FXML — error labels ────────────────────────────────────────────────
    @FXML
    private Label locationError;
    @FXML
    private Label descriptionError;
    @FXML
    private Label validationLabel;

    @FXML
    private Button submitButton;

    private final DisasterReportDao reportDao = new DisasterReportDaoImpl();

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        populateReporterCard();
        populateDropdowns();
        clearErrors();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleSubmit() {
        clearErrors();

        String error = validateForm();
        if (error != null) {
            if (!error.isEmpty()) {
                showValidationError(error);
            }
            return;
        }

        submitButton.setDisable(true);
        submitButton.setText("Submitting…");

        new Thread(() -> {
            try {
                User currentUser = SessionContext.getCurrentUser();

                DisasterReport report = new DisasterReport(
                        disasterTypeCombo.getValue(),
                        locationField.getText().trim(),
                        severityCombo.getValue(),
                        descriptionArea.getText().trim(),
                        currentUser
                );

                reportDao.create(report);
                LOGGER.info("Report saved: id=" + report.getId());

                // Navigate back — list reloads fresh data from DB
                Platform.runLater(()
                        -> SceneManager.switchContent("DisasterReportListView"));

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to save report", ex);
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    submitButton.setText("Submit Report");
                    showValidationError("Submission failed: " + ex.getMessage());
                });
            }
        }, "submit-report-thread").start();
    }

    @FXML
    private void handleClear() {
        disasterTypeCombo.getSelectionModel().clearSelection();
        severityCombo.getSelectionModel().clearSelection();
        locationField.clear();
        descriptionArea.clear();
        clearErrors();
    }

    @FXML
    private void handleCancel() {
        SceneManager.switchContent("DisasterReportListView");
    }

    // ── Private helpers ────────────────────────────────────────────────────
    /**
     * Fills the "Reporting As" card with the logged-in user's details. This is
     * purely informational — it is NOT editable.
     */
    private void populateReporterCard() {
        if (!SessionContext.isLoggedIn()) {
            return;
        }
        User u = SessionContext.getCurrentUser();
        reporterNameLabel.setText(u.getFullName());
        reporterRoleLabel.setText(u.getRole());
        reporterEmailLabel.setText(u.getEmail());
    }

    private void populateDropdowns() {
        disasterTypeCombo.setItems(FXCollections.observableArrayList(
                "Hurricane", "Fire", "Earthquake", "Flood",
                "Tornado", "Tsunami", "Landslide", "Other"
        ));
        severityCombo.setItems(FXCollections.observableArrayList(
                DisasterReport.SEVERITY_CRITICAL,
                DisasterReport.SEVERITY_HIGH,
                DisasterReport.SEVERITY_LOW,
                DisasterReport.SEVERITY_MEDIUM
        ));
    }

    /**
     * @return error message string, empty string if field-level only, null if
     * valid
     */
    private String validateForm() {
        if (disasterTypeCombo.getValue() == null) {
            return "Please select a disaster type.";
        }
        if (severityCombo.getValue() == null) {
            return "Please select a severity level.";
        }
        if (locationField.getText().trim().isEmpty()) {
            showFieldError(locationError, "Affected location is required.");
            return "";
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            showFieldError(descriptionError, "A description is required.");
            return "";
        }
        return null;
    }

    private void showValidationError(String msg) {
        validationLabel.setText(msg);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void showFieldError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setManaged(true);
        lbl.setVisible(true);
    }

    private void clearErrors() {
        for (Label lbl : new Label[]{
            validationLabel, locationError, descriptionError}) {
            lbl.setManaged(false);
            lbl.setVisible(false);
        }
    }
}
