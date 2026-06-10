package edu.cqu.drs.view;

import edu.cqu.drs.client.ClientSession;
import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.protocol.BoardSnapshot;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

/**
 * FXML controller for the Live Multi-Dispatcher Board ({@code liveBoard.fxml})
 * - new feature f1.
 *
 * <p>The committed baseline is client-side <em>polling</em>: a JavaFX
 * {@link Timeline} asks the server for a {@link BoardSnapshot} on a selectable
 * interval (default {@value #DEFAULT_INTERVAL_SECONDS}s), plus a manual
 * "Refresh Now". Each poll is one consistent snapshot - the rows, the open and
 * total counts, and the <em>server-stamped</em> "last updated" time all from the
 * same instant - so several dispatchers watching their own boards converge on
 * the shared server state within one interval of any change. No server-push is
 * involved.</p>
 *
 * <p>Threading: every poll runs off the FX Application Thread through
 * {@link ClientSession#runAsync}; a tick that fires while the previous poll is
 * still in flight is skipped (the in-flight guard), and the timeline stops
 * itself when the view leaves the scene, so a swapped-out board never keeps
 * polling invisibly. After a poll fails (for example the server went away) the
 * timeline pauses and the manual Refresh restarts it.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class LiveBoardController {

    /** Default polling interval, in seconds. */
    private static final int DEFAULT_INTERVAL_SECONDS = 3;

    /** The selectable polling intervals, in seconds. */
    private static final Integer[] INTERVAL_CHOICES = {2, 3, 5, 10};

    /** Time-of-day format for the report column and the last-updated label. */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Style class for an error message on the poll-status label. */
    private static final String STATUS_ERROR_CLASS = "status-error";

    /** Polling-interval selector (seconds). */
    @FXML
    private ComboBox<Integer> intervalCombo;

    /** Pulls a snapshot immediately and restarts a paused poll. */
    @FXML
    private Button refreshNowButton;

    /** Shows the snapshot's open/total counts. */
    @FXML
    private Label countsLabel;

    /** The board table - one row per incident, most urgent first. */
    @FXML
    private TableView<Incident> boardTable;

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

    /** "Responders" column - how many responders are allocated. */
    @FXML
    private TableColumn<Incident, String> respondersColumn;

    /** Shows the server-stamped time of the snapshot on display. */
    @FXML
    private Label lastUpdatedLabel;

    /** Shows the polling state, or the pause-and-retry message on failure. */
    @FXML
    private Label pollStatusLabel;

    /** The shared server session; injected by the shell after loading. */
    private ClientSession session;

    /** The session's server gateway (the board polls through it). */
    private ServerStub serverStub;

    /** The polling clock; rebuilt whenever the interval changes. */
    private Timeline pollTimeline;

    /** True while a poll is in flight - overlapping ticks are skipped. */
    private final AtomicBoolean pollInFlight = new AtomicBoolean(false);

    /**
     * Injects the live server session, starts the polling timeline, and pulls
     * the first snapshot immediately. Called by the application shell right
     * after the FXML is loaded.
     *
     * @param session the connected, authenticated session (must not be null).
     * @throws IllegalArgumentException if {@code session} is null.
     */
    public void init(ClientSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        this.session = session;
        this.serverStub = session.getServerStub();
        startPolling(this.intervalCombo.getValue());
        poll();
    }

    /**
     * Initialises the view after the FXML is loaded: configures the columns and
     * the interval selector, and stops the polling clock whenever the board
     * leaves the scene (view swapped out or window closed).
     */
    @FXML
    private void initialize() {
        configureColumns();
        this.intervalCombo.setItems(
                FXCollections.observableArrayList(INTERVAL_CHOICES));
        this.intervalCombo.setValue(DEFAULT_INTERVAL_SECONDS);
        this.intervalCombo.valueProperty().addListener(
                (obs, old, seconds) -> startPolling(seconds));
        this.boardTable.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                stopPolling();
            }
        });
    }

    /**
     * Pulls a snapshot immediately and (re)starts the polling clock - also the
     * recovery path after a failed poll paused it. Handler for "Refresh Now".
     */
    @FXML
    private void onRefreshNow() {
        startPolling(this.intervalCombo.getValue());
        poll();
    }

    /**
     * (Re)builds and starts the polling timeline at the given interval.
     *
     * @param seconds the polling interval in seconds (null falls back to the
     *                default).
     */
    private void startPolling(Integer seconds) {
        stopPolling();
        int interval = (seconds == null) ? DEFAULT_INTERVAL_SECONDS : seconds;
        this.pollTimeline = new Timeline(new KeyFrame(
                Duration.seconds(interval), event -> poll()));
        this.pollTimeline.setCycleCount(Timeline.INDEFINITE);
        this.pollTimeline.play();
    }

    /** Stops the polling timeline, if it is running. */
    private void stopPolling() {
        if (this.pollTimeline != null) {
            this.pollTimeline.stop();
        }
    }

    /**
     * Fetches one board snapshot off the FX thread and applies it. A tick that
     * arrives while the previous poll is still in flight is skipped, so a slow
     * server never piles up queued polls.
     */
    private void poll() {
        if (this.serverStub == null || !this.pollInFlight.compareAndSet(false, true)) {
            return;
        }
        this.session.runAsync(this.serverStub::getBoard,
                snapshot -> {
                    this.pollInFlight.set(false);
                    showSnapshot(snapshot);
                },
                failure -> {
                    this.pollInFlight.set(false);
                    stopPolling();
                    showPollError("Poll failed: " + failure.getMessage()
                            + " - polling paused; use Refresh Now to retry.");
                });
    }

    /**
     * Applies a snapshot to the table and labels (FX thread): rows, open/total
     * counts, and the server-stamped last-updated time.
     *
     * @param snapshot the snapshot to show.
     */
    private void showSnapshot(BoardSnapshot snapshot) {
        Incident keep = this.boardTable.getSelectionModel().getSelectedItem();
        this.boardTable.getItems().setAll(snapshot.getIncidents());
        if (keep != null && this.boardTable.getItems().contains(keep)) {
            this.boardTable.getSelectionModel().select(keep);
        }
        this.countsLabel.setText(snapshot.getOpenCount() + " open / "
                + snapshot.getTotalCount() + " total");
        this.lastUpdatedLabel.setText("Last updated "
                + snapshot.getSnapshotAt().format(TIME_FORMAT) + " (server time)");
        this.pollStatusLabel.getStyleClass().remove(STATUS_ERROR_CLASS);
        this.pollStatusLabel.setText("Polling every "
                + this.intervalCombo.getValue() + "s");
    }

    /**
     * Shows a poll failure on the status label (FX thread).
     *
     * @param message the message to display.
     */
    private void showPollError(String message) {
        if (!this.pollStatusLabel.getStyleClass().contains(STATUS_ERROR_CLASS)) {
            this.pollStatusLabel.getStyleClass().add(STATUS_ERROR_CLASS);
        }
        this.pollStatusLabel.setText(message);
    }

    /**
     * Configures each table column's cell-value factory. {@code Incident} is a
     * plain object (no JavaFX properties), so each cell value is computed and
     * wrapped in a {@link SimpleStringProperty}.
     */
    private void configureColumns() {
        this.refColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getId().toString().substring(0, 8)));
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
        this.respondersColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getResponders().size())));
    }
}
