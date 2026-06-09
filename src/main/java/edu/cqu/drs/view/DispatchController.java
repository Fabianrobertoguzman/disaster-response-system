package edu.cqu.drs.view;

import edu.cqu.drs.client.ClientSession;
import edu.cqu.drs.client.DispatchClientPresenter;
import edu.cqu.drs.model.AlertTemplate;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.presenter.AppContext;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/**
 * FXML controller for the dispatcher console ({@code dispatch.fxml}).
 *
 * <p>Realises Assessment One's {@code DispatchView} and use cases UC-02
 * (Verify and Triage) and UC-03 (Allocate Resources) over the
 * <em>client/server</em> path: the queue is pulled from the server, and triage,
 * allocation and resolution are performed on the server through the session's
 * {@link DispatchClientPresenter}. Every server call runs off the FX
 * Application Thread via {@link ClientSession#runAsync}, with the triggering
 * button disabled while a call is in flight. The "Refresh" button re-pulls the
 * queue, picking up incidents filed by <em>other clients</em> - the
 * multi-dispatcher view of shared server state.</p>
 *
 * <p>Partner-agency notification (NFR-O04) is the one deliberately
 * <em>local</em> action: there is no partner wire action in this build, so the
 * button is labelled a local stub and drives the in-process
 * {@link edu.cqu.drs.presenter.PartnerNotifier} only.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DispatchController {

    /** Placeholder shown in the detail labels when nothing is selected. */
    private static final String NO_VALUE = "-";

    /** Time-of-day format for the "Reported" column and the detail panel. */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Style class added to a status label for a success message. */
    private static final String STATUS_OK_CLASS = "status-ok";

    /** Style class added to a status label for an error or hint message. */
    private static final String STATUS_ERROR_CLASS = "status-error";

    /** The incident-queue table. */
    @FXML
    private TableView<Incident> incidentTable;

    /** "Ref" column - the first eight characters of the incident id. */
    @FXML
    private TableColumn<Incident, String> refColumn;

    /** "Hazard" column. */
    @FXML
    private TableColumn<Incident, String> hazardColumn;

    /** "Severity" column. */
    @FXML
    private TableColumn<Incident, String> severityColumn;

    /** "Victims" column - the estimated number of people affected. */
    @FXML
    private TableColumn<Incident, String> victimsColumn;

    /** "Status" column - the incident lifecycle state. */
    @FXML
    private TableColumn<Incident, String> statusColumn;

    /** "Reported" column - the report time of day. */
    @FXML
    private TableColumn<Incident, String> reportedColumn;

    /** Re-pulls the queue from the server into the table. */
    @FXML
    private Button refreshButton;

    /** Shows the number of incidents in the queue. */
    @FXML
    private Label queueSummaryLabel;

    /** Detail: full incident reference. */
    @FXML
    private Label detailReferenceLabel;

    /** Detail: hazard type. */
    @FXML
    private Label detailHazardLabel;

    /** Detail: current severity. */
    @FXML
    private Label detailSeverityLabel;

    /** Detail: estimated people affected. */
    @FXML
    private Label detailVictimsLabel;

    /** Detail: lifecycle status. */
    @FXML
    private Label detailStatusLabel;

    /** Detail: report time. */
    @FXML
    private Label detailReportedLabel;

    /** Detail: captured location. */
    @FXML
    private Label detailLocationLabel;

    /** Detail: number of responders allocated. */
    @FXML
    private Label detailRespondersLabel;

    /** Detail: recommended CAP alert template (FR-CR-01), or "-" if none. */
    @FXML
    private Label detailRecommendedTemplateLabel;

    /** Detail: incident description (read-only). */
    @FXML
    private TextArea detailDescriptionArea;

    /** Severity selector for triage. */
    @FXML
    private ComboBox<Severity> severityCombo;

    /** Applies the selected severity to the selected incident. */
    @FXML
    private Button triageButton;

    /** Marks the selected incident resolved. */
    @FXML
    private Button resolveButton;

    /** Shows the result of a triage/resolve action, or a hint. */
    @FXML
    private Label actionStatusLabel;

    /** Allocates the next available responder to the selected incident. */
    @FXML
    private Button assignResponderButton;

    /** Notifies the partner agencies of the selected incident (local stub). */
    @FXML
    private Button notifyPartnersButton;

    /** Shows the result of an allocation or partner-notification action. */
    @FXML
    private Label coordinationStatusLabel;

    /**
     * Read-only display of the partner agencies and the last notification
     * result.
     */
    @FXML
    private TextArea partnerLogArea;

    /**
     * The in-process application context, retained ONLY for the local
     * partner-notifier stub (the incident queue and responder roster it also
     * holds are no longer wired into this client/server build).
     */
    private final AppContext appContext;

    /** The shared server session; injected by the shell after loading. */
    private ClientSession session;

    /** Performs the dispatch actions on the server; created on injection. */
    private DispatchClientPresenter presenter;

    /** The incident currently selected in the table, or null. */
    private Incident selected;

    /**
     * Creates the controller. Instantiated by the {@link javafx.fxml.FXMLLoader}
     * via this no-argument constructor named by the {@code fx:controller}
     * attribute in {@code dispatch.fxml}; the live server session is injected
     * afterwards through {@link #init(ClientSession)}.
     */
    public DispatchController() {
        this.appContext = AppContext.getInstance();
    }

    /**
     * Injects the live server session and pulls the initial queue. Called by
     * the application shell right after the FXML is loaded.
     *
     * @param session the connected, authenticated session (must not be null).
     * @throws IllegalArgumentException if {@code session} is null.
     */
    public void init(ClientSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        this.session = session;
        this.presenter = new DispatchClientPresenter(session.getServerStub());
        onRefresh();
    }

    /**
     * Initialises the view after the FXML is loaded: configures the table
     * columns, populates the severity selector, lists the partner agencies,
     * and wires the selection listener. The first queue pull happens when the
     * session is injected ({@link #init(ClientSession)}).
     */
    @FXML
    private void initialize() {
        configureColumns();
        this.severityCombo.setItems(
                FXCollections.observableArrayList(Severity.values()));
        this.partnerLogArea.setText("Local stub - no server round-trip.\n"
                + String.join("\n",
                        this.appContext.getPartnerNotifier().describeAgencies()));
        this.incidentTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> onSelectionChanged(sel));
        this.actionStatusLabel.setText("");
        this.coordinationStatusLabel.setText("");
    }

    /**
     * Re-pulls the incident queue from the server (most-urgent-first), keeping
     * the current selection if that incident is still present. Handler for the
     * "Refresh" button; also called after each mutating action.
     */
    @FXML
    private void onRefresh() {
        if (this.presenter == null) {
            return;
        }
        this.refreshButton.setDisable(true);
        this.session.runAsync(this.presenter::pendingIncidents,
                this::showQueue,
                failure -> {
                    this.refreshButton.setDisable(false);
                    showAction("Could not refresh the queue: "
                            + failure.getMessage(), false);
                });
    }

    /**
     * Replaces the table contents with a fresh server snapshot (FX thread),
     * restoring the selection when the same incident is still present.
     *
     * @param incidents the server's incidents, most urgent first.
     */
    private void showQueue(List<Incident> incidents) {
        Incident keep = this.selected;
        this.incidentTable.getItems().setAll(incidents);
        this.queueSummaryLabel.setText(incidents.size() + " incident(s)");
        if (keep != null && this.incidentTable.getItems().contains(keep)) {
            this.incidentTable.getSelectionModel().select(keep);
        } else {
            this.incidentTable.getSelectionModel().clearSelection();
        }
        this.refreshButton.setDisable(false);
    }

    /**
     * Applies the severity chosen in the combo to the selected incident on the
     * server, and asks it for the recommended alert template (FR-CR-01).
     * Handler for the "Triage" button.
     */
    @FXML
    private void onTriage() {
        if (this.selected == null) {
            showAction("Select an incident in the queue first.", false);
            return;
        }
        Severity severity = this.severityCombo.getValue();
        if (severity == null) {
            showAction("Choose a severity to apply.", false);
            return;
        }
        Incident incident = this.selected;
        this.triageButton.setDisable(true);
        this.session.runAsync(() -> {
            this.presenter.triage(incident.getId(), severity);
            return this.presenter.recommendTemplate(incident.getId());
        }, template -> {
            this.triageButton.setDisable(false);
            onRefresh();
            showAction("Severity set to " + severity + "; recommended alert: "
                    + template + ".", true);
        }, failure -> {
            this.triageButton.setDisable(false);
            // The severity may already have been applied when the follow-up
            // recommendation failed, so re-pull the authoritative state.
            onRefresh();
            showAction("Could not complete the triage: "
                    + failure.getMessage(), false);
        });
    }

    /**
     * Marks the selected incident resolved on the server. Handler for the
     * "Resolve" button.
     */
    @FXML
    private void onResolve() {
        if (this.selected == null) {
            showAction("Select an incident in the queue first.", false);
            return;
        }
        Incident incident = this.selected;
        this.resolveButton.setDisable(true);
        this.session.runAsync(() -> this.presenter.resolve(incident.getId()),
                resolved -> {
                    this.resolveButton.setDisable(false);
                    onRefresh();
                    showAction("Incident " + shortRef(resolved)
                            + " marked resolved.", true);
                }, failure -> {
                    this.resolveButton.setDisable(false);
                    showAction("Could not resolve: " + failure.getMessage(), false);
                });
    }

    /**
     * Allocates the next available field responder (from the server's roster)
     * to the selected incident (FR-05). Handler for the "Assign Next Available
     * Responder" button.
     */
    @FXML
    private void onAssignResponder() {
        if (this.selected == null) {
            showCoordination("Select an incident in the queue first.", false);
            return;
        }
        Incident incident = this.selected;
        java.util.concurrent.atomic.AtomicReference<String> assignedName =
                new java.util.concurrent.atomic.AtomicReference<>("responder");
        this.assignResponderButton.setDisable(true);
        this.session.runAsync(() -> {
            Responder responder = nextAvailable(this.presenter.listResponders());
            if (responder == null) {
                throw new IllegalStateException(
                        "No responders are currently available.");
            }
            assignedName.set(responder.getName());
            return this.presenter.assignResponder(
                    incident.getId(), responder.getId());
        }, updated -> {
            this.assignResponderButton.setDisable(false);
            this.selected = updated;
            showDetail(updated);
            showCoordination("Assigned " + assignedName.get()
                    + "; incident now has " + updated.getResponders().size()
                    + " responder(s).", true);
        }, failure -> {
            this.assignResponderButton.setDisable(false);
            showCoordination("Could not assign responder: "
                    + failure.getMessage(), false);
        });
    }

    /**
     * Notifies every partner agency of the selected incident and shows the
     * per-agency acknowledgement result (NFR-O04). This is a deliberate LOCAL
     * stub - no partner wire action exists in this build, so the in-process
     * notifier is used and the view says so. Handler for the "Notify Partner
     * Agencies (local stub)" button.
     */
    @FXML
    private void onNotifyPartners() {
        if (this.selected == null) {
            showCoordination("Select an incident in the queue first.", false);
            return;
        }
        Map<String, Boolean> result =
                this.appContext.getPartnerNotifier().notifyAll(this.selected);
        long acknowledged = result.values().stream().filter(b -> b).count();
        StringBuilder text = new StringBuilder(
                "Local stub - no server round-trip.\nPartner agencies:\n");
        for (String line : this.appContext.getPartnerNotifier()
                .describeAgencies()) {
            text.append("  ").append(line).append("\n");
        }
        text.append("\nLast notification (incident ")
                .append(shortRef(this.selected)).append("):\n");
        result.forEach((name, ack) -> text.append("  ").append(name)
                .append(": ").append(ack ? "ACK" : "NO ACK").append("\n"));
        this.partnerLogArea.setText(text.toString());
        showCoordination("Notified " + result.size() + " partner agencies "
                + "(local stub); " + acknowledged + " acknowledged.", true);
    }

    /**
     * Configures each table column's cell-value factory. {@code Incident} is a
     * plain object (no JavaFX properties), so each cell value is computed and
     * wrapped in a {@link SimpleStringProperty}.
     */
    private void configureColumns() {
        this.refColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(shortRef(cd.getValue())));
        this.hazardColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getHazardType().name()));
        this.severityColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getSeverity().name()));
        this.victimsColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getVictimCount())));
        this.statusColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatus().name()));
        this.reportedColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getReportedAt().format(TIME_FORMAT)));
    }

    /**
     * Reacts to a table-selection change: remembers the selected incident,
     * shows its detail, and enables or disables the action controls.
     *
     * @param incident the newly selected incident, or null if cleared.
     */
    private void onSelectionChanged(Incident incident) {
        this.selected = incident;
        showDetail(incident);
        boolean hasSelection = incident != null;
        this.severityCombo.setDisable(!hasSelection);
        this.triageButton.setDisable(!hasSelection);
        this.resolveButton.setDisable(!hasSelection);
        this.assignResponderButton.setDisable(!hasSelection);
        this.notifyPartnersButton.setDisable(!hasSelection);
    }

    /**
     * Shows an incident's fields in the detail panel, or clears the panel.
     *
     * @param incident the incident to show, or null to clear the panel.
     */
    private void showDetail(Incident incident) {
        if (incident == null) {
            this.detailReferenceLabel.setText(NO_VALUE);
            this.detailHazardLabel.setText(NO_VALUE);
            this.detailSeverityLabel.setText(NO_VALUE);
            this.detailVictimsLabel.setText(NO_VALUE);
            this.detailStatusLabel.setText(NO_VALUE);
            this.detailReportedLabel.setText(NO_VALUE);
            this.detailLocationLabel.setText(NO_VALUE);
            this.detailRespondersLabel.setText(NO_VALUE);
            this.detailRecommendedTemplateLabel.setText(NO_VALUE);
            this.detailDescriptionArea.clear();
            this.severityCombo.getSelectionModel().clearSelection();
            return;
        }
        this.detailReferenceLabel.setText(incident.getId().toString());
        this.detailHazardLabel.setText(incident.getHazardType().name());
        this.detailSeverityLabel.setText(incident.getSeverity().name());
        this.detailVictimsLabel.setText(
                String.valueOf(incident.getVictimCount()));
        this.detailStatusLabel.setText(incident.getStatus().name());
        this.detailReportedLabel.setText(
                incident.getReportedAt().format(TIME_FORMAT));
        this.detailLocationLabel.setText(incident.getGpsLocation().toString());
        this.detailRespondersLabel.setText(
                String.valueOf(incident.getResponders().size()));
        AlertTemplate template = incident.getRecommendedTemplate();
        this.detailRecommendedTemplateLabel.setText(
                template == null ? NO_VALUE : template.name());
        this.detailDescriptionArea.setText(incident.getDescription());
        this.severityCombo.getSelectionModel().select(incident.getSeverity());
    }

    /**
     * Returns the first available responder in the server's roster, or null if
     * every responder is currently tasked.
     *
     * @param roster the server's responder roster.
     * @return an available {@link Responder}, or null.
     */
    private static Responder nextAvailable(List<Responder> roster) {
        for (Responder responder : roster) {
            if (responder.isAvailable()) {
                return responder;
            }
        }
        return null;
    }

    /**
     * Returns the short reference for an incident (the first eight characters
     * of its id).
     *
     * @param incident the incident (never null).
     * @return the short reference string.
     */
    private String shortRef(Incident incident) {
        return incident.getId().toString().substring(0, 8);
    }

    /**
     * Shows a message on the triage action-status line.
     *
     * @param message the text to show (never null).
     * @param success true for the success style, false for the error style.
     */
    private void showAction(String message, boolean success) {
        applyStatus(this.actionStatusLabel, message, success);
    }

    /**
     * Shows a message on the coordination action-status line.
     *
     * @param message the text to show (never null).
     * @param success true for the success style, false for the error style.
     */
    private void showCoordination(String message, boolean success) {
        applyStatus(this.coordinationStatusLabel, message, success);
    }

    /**
     * Sets a status label's text and success/error style class.
     *
     * @param label   the label to update.
     * @param message the text to show.
     * @param success true for the success style, false for the error style.
     */
    private void applyStatus(Label label, String message, boolean success) {
        String style = success ? STATUS_OK_CLASS : STATUS_ERROR_CLASS;
        label.getStyleClass().removeAll(STATUS_OK_CLASS, STATUS_ERROR_CLASS);
        label.getStyleClass().add(style);
        label.setText(message);
    }
}
