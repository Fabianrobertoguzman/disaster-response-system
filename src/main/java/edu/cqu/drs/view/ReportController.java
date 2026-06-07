package edu.cqu.drs.view;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentQueue;
import edu.cqu.drs.presenter.AppContext;
import edu.cqu.drs.presenter.ReportPresenter;
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
 * optional estimate of people affected, and on submit the system files an
 * {@link Incident} and shows an acknowledgement carrying the incident reference
 * (FR-03). All coordination is delegated to {@link ReportPresenter}; this class
 * only reads and writes JavaFX controls.</p>
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
     * Shows the GPS-fix status. In DRS-Initial the captured location is a
     * simulated sample coordinate, not the device's real GPS (NFR-P06).
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
     * report fields; in DRS-Initial it is deferred to Assessment 3, so clicking
     * this button explains the deferral rather than attaching a file.
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

    /** Coordinates the report against the shared incident queue. */
    private final ReportPresenter presenter;

    /** The location captured for the report currently being entered. */
    private GpsCoordinate capturedLocation;

    /**
     * Creates the controller, wiring it to a {@link ReportPresenter} over the
     * application-wide incident queue. The {@link javafx.fxml.FXMLLoader}
     * instantiates this class via this no-argument constructor, named by the
     * {@code fx:controller} attribute in {@code report.fxml}.
     */
    public ReportController() {
        IncidentQueue queue = AppContext.getInstance().getIncidentQueue();
        this.presenter = new ReportPresenter(queue);
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
                "DRS-Initial uses a fixed sample coordinate (near CQU's "
                + "Rockhampton campus). Capturing the device's real GPS "
                + "satisfies FR-02 / NFR-P06 but is deferred to Assessment 3."));
        captureLocation();
        this.statusLabel.setText("");
    }

    /** Re-captures the device location. Handler for the "Refresh" button. */
    @FXML
    private void onRefreshLocation() {
        captureLocation();
    }

    /**
     * Explains that photo attachment is deferred to Assessment 3. Handler for
     * the "Attach Photo..." button.
     */
    @FXML
    private void onAttachPhoto() {
        Alert notice = new Alert(Alert.AlertType.INFORMATION,
                "Photo attachment is one of Assessment One's FR-01 report "
                + "fields. It needs durable storage, so it is deferred to "
                + "Assessment 3; in this prototype the report is filed "
                + "without a photo.");
        notice.setHeaderText("Photo attachment - planned for Assessment 3");
        notice.showAndWait();
    }

    /**
     * Validates the form, files an {@link Incident} via the presenter, and shows
     * an acknowledgement carrying the incident reference and the time taken
     * (FR-03). Handler for the "Submit Report" button.
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
        try {
            long startNanos = System.nanoTime();
            Incident reported = this.presenter.submitIncident(
                    hazardType, this.capturedLocation, description, victimCount);
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            showOk("Report filed. Reference " + reported.getId()
                    + " - acknowledged in " + elapsedMs + " ms. "
                    + this.presenter.queueSize()
                    + " incident(s) awaiting triage.");
            resetForm();
        } catch (IllegalArgumentException ex) {
            showError("Could not file the report: " + ex.getMessage());
        }
    }

    /** Clears all fields and the status line. Handler for the "Clear" button. */
    @FXML
    private void onClear() {
        resetForm();
        this.statusLabel.setText("");
    }

    /**
     * Captures the current device location (a fixed Rockhampton stub in
     * DRS-Initial) and updates the location and GPS-status labels.
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
