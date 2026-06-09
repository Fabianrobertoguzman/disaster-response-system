package edu.cqu.drs.view;

import edu.cqu.drs.client.ClientSession;
import edu.cqu.drs.client.ReportClientPresenter;
import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * FXML controller for the citizen-report view ({@code report.fxml}).
 *
 * <p>Realises Assessment One's {@code ReportView} and use case UC-01
 * ("Report Disaster"): a citizen picks a hazard type, the system captures the
 * location and time automatically (FR-02), the citizen adds a description and an
 * optional estimate of people affected, and on submit the report is filed on the
 * <em>server</em> through the session's {@link ReportClientPresenter}; the
 * acknowledgement carries the server-assigned incident reference (FR-03).</p>
 *
 * <p>The blocking server call runs off the FX Application Thread via
 * {@link ClientSession#runAsync}; the submit button is disabled while a call is
 * in flight, and the result lands back on the FX thread through the session's
 * callback dispatcher. The shell injects the session via {@link
 * #init(ClientSession)} right after the FXML is loaded.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class ReportController {

    /** Style class added to {@link #statusLabel} for a success message. */
    private static final String STATUS_OK_CLASS = "status-ok";

    /** Style class added to {@link #statusLabel} for an error message. */
    private static final String STATUS_ERROR_CLASS = "status-error";

    /** Hazard-type selector, populated from {@link HazardType#values()}. */
    @FXML
    private ComboBox<HazardType> hazardTypeCombo;

    /** Shows the auto-captured location as a coordinate pair. */
    @FXML
    private Label locationLabel;

    /**
     * Shows the GPS-fix status. The captured location is a simulated sample
     * coordinate, not the device's real GPS (NFR-P06).
     */
    @FXML
    private Label gpsStatusLabel;

    /** Free-text description of the situation. */
    @FXML
    private TextArea descriptionArea;

    /** Optional citizen estimate of the number of people affected. */
    @FXML
    private TextField victimCountField;

    /**
     * Photo-attachment button. Photo upload is one of Assessment One's FR-01
     * report fields; it remains deferred, so clicking this button explains the
     * deferral rather than attaching a file.
     */
    @FXML
    private Button attachPhotoButton;

    /** Files the report. */
    @FXML
    private Button submitButton;

    /** Resets the form. */
    @FXML
    private Button clearButton;

    /** Shows the post-submit acknowledgement or a validation error. */
    @FXML
    private Label statusLabel;

    /** The shared server session; injected by the shell after loading. */
    private ClientSession session;

    /** Files the report on the server; created when the session is injected. */
    private ReportClientPresenter presenter;

    /** The location captured for the report currently being entered. */
    private GpsCoordinate capturedLocation;

    /**
     * Injects the live server session. Called by the application shell right
     * after the FXML is loaded (the {@link javafx.fxml.FXMLLoader} itself uses
     * the no-argument constructor named by {@code fx:controller}).
     *
     * @param session the connected, authenticated session (must not be null).
     * @throws IllegalArgumentException if {@code session} is null.
     */
    public void init(ClientSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        this.session = session;
        this.presenter = new ReportClientPresenter(session.getServerStub());
    }

    /**
     * Initialises the view after the FXML is loaded: populates the hazard-type
     * selector, captures an initial location, and clears the status line.
     */
    @FXML
    private void initialize() {
        this.hazardTypeCombo.setItems(
                FXCollections.observableArrayList(HazardType.values()));
        this.locationLabel.setTooltip(new Tooltip(
                "The prototype uses a fixed sample coordinate (near CQU's "
                + "Rockhampton campus) standing in for a real device-GPS read "
                + "(FR-02 / NFR-P06)."));
        captureLocation();
        this.statusLabel.setText("");
    }

    /** Re-captures the device location. Handler for the "Refresh" button. */
    @FXML
    private void onRefreshLocation() {
        captureLocation();
    }

    /**
     * Explains that photo attachment is deferred. Handler for the
     * "Attach Photo..." button.
     */
    @FXML
    private void onAttachPhoto() {
        Alert notice = new Alert(Alert.AlertType.INFORMATION,
                "Photo attachment is one of Assessment One's FR-01 report "
                + "fields. It needs durable binary storage, so it remains "
                + "deferred; the report is filed without a photo.");
        notice.setHeaderText("Photo attachment - deferred");
        notice.showAndWait();
    }

    /**
     * Validates the form and files the report on the server (off the FX
     * thread), showing an acknowledgement with the server-assigned reference
     * and the round-trip time (FR-03). Handler for the "Submit Report" button.
     */
    @FXML
    private void onSubmit() {
        HazardType hazardType = this.hazardTypeCombo.getValue();
        if (hazardType == null) {
            showError("Please choose a hazard type before submitting.");
            return;
        }
        String description = this.descriptionArea.getText();
        if (description == null || description.trim().isEmpty()) {
            showError("Please describe what you see.");
            return;
        }
        int victimCount;
        try {
            victimCount = parseVictimCount(this.victimCountField.getText());
        } catch (NumberFormatException ex) {
            showError("People affected must be a whole number.");
            return;
        }
        if (victimCount < 0) {
            showError("People affected cannot be negative.");
            return;
        }
        if (this.presenter == null) {
            showError("Not connected to the server. Sign in first.");
            return;
        }

        GpsCoordinate location = this.capturedLocation;
        long startNanos = System.nanoTime();
        this.submitButton.setDisable(true);
        this.session.runAsync(
                () -> this.presenter.submitIncident(
                        hazardType, location, description, victimCount),
                reported -> onSubmitted(reported, startNanos),
                failure -> {
                    this.submitButton.setDisable(false);
                    showError("Could not file the report: " + failure.getMessage());
                });
    }

    /**
     * Completes a successful submission on the FX thread: shows the
     * acknowledgement and resets the form.
     *
     * @param reported   the incident as persisted by the server.
     * @param startNanos when the submission started, for the round-trip time.
     */
    private void onSubmitted(Incident reported, long startNanos) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        this.submitButton.setDisable(false);
        showOk("Report filed. Reference " + reported.getId()
                + " - acknowledged by the server in " + elapsedMs + " ms.");
        resetForm();
    }

    /** Clears all fields and the status line. Handler for the "Clear" button. */
    @FXML
    private void onClear() {
        resetForm();
        this.statusLabel.setText("");
    }

    /**
     * Captures the current device location (a fixed Rockhampton stub) and
     * updates the location and GPS-status labels.
     */
    private void captureLocation() {
        this.capturedLocation = GpsCoordinate.captureCurrentLocation();
        this.locationLabel.setText(this.capturedLocation.toString());
        this.gpsStatusLabel.setText("GPS fix acquired (simulated location).");
    }

    /**
     * Parses the optional "people affected" field. An empty field means zero.
     *
     * @param raw the raw text from {@link #victimCountField} (may be null/blank).
     * @return the parsed integer, or 0 if the text is null or blank.
     * @throws NumberFormatException if non-blank text is not a valid integer.
     */
    private int parseVictimCount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(raw.trim());
    }

    /**
     * Resets the input controls to their empty/initial state and re-captures the
     * location (a fresh report gets a fresh location fix).
     */
    private void resetForm() {
        this.hazardTypeCombo.getSelectionModel().clearSelection();
        this.descriptionArea.clear();
        this.victimCountField.clear();
        captureLocation();
    }

    /**
     * Shows a success message on the status line.
     *
     * @param message the text to show (never null).
     */
    private void showOk(String message) {
        this.statusLabel.getStyleClass()
                .removeAll(STATUS_OK_CLASS, STATUS_ERROR_CLASS);
        this.statusLabel.getStyleClass().add(STATUS_OK_CLASS);
        this.statusLabel.setText(message);
    }

    /**
     * Shows an error message on the status line.
     *
     * @param message the text to show (never null).
     */
    private void showError(String message) {
        this.statusLabel.getStyleClass()
                .removeAll(STATUS_OK_CLASS, STATUS_ERROR_CLASS);
        this.statusLabel.getStyleClass().add(STATUS_ERROR_CLASS);
        this.statusLabel.setText(message);
    }
}
