package edu.cqu.drs.view;

import edu.cqu.drs.client.ClientSession;
import edu.cqu.drs.client.ServerStub;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Severity;
import edu.cqu.drs.protocol.AnalyticsReport;
import edu.cqu.drs.protocol.ResponseTimeMetric;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

/**
 * FXML controller for the Damage Assessment &amp; Analytics Dashboard
 * ({@code analyticsDashboard.fxml}) - new feature f2.
 *
 * <p>One Refresh pulls one server-assembled {@link AnalyticsReport} and renders
 * it: incidents-by-hazard as a bar chart, the severity distribution as a pie
 * chart, the victim total and incident count in the summary line, and the
 * response-time statistics of the resolved incidents - every figure computed by
 * the server over the persisted data at the same server-stamped instant, never
 * by the client.</p>
 *
 * <p>The blocking call runs off the FX Application Thread through
 * {@link ClientSession#runAsync} with the Refresh button disabled in flight;
 * the shell injects the session via {@link #init(ClientSession)} right after
 * the FXML is loaded and the first report loads immediately.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AnalyticsDashboardController {

    /** Time-of-day format for the server-stamped generation time. */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Style class for an error message on the status label. */
    private static final String STATUS_ERROR_CLASS = "status-error";

    /** Pulls a fresh report from the server. */
    @FXML
    private Button refreshButton;

    /** Shows the server-stamped time the displayed report was generated. */
    @FXML
    private Label generatedAtLabel;

    /** Shows the incident and victim totals of the displayed report. */
    @FXML
    private Label summaryLabel;

    /** Incidents-by-hazard bar chart. */
    @FXML
    private BarChart<String, Number> hazardChart;

    /** Severity-distribution pie chart. */
    @FXML
    private PieChart severityChart;

    /** Shows the response-time statistics of the resolved incidents. */
    @FXML
    private Label responseTimesLabel;

    /** Shows a load failure, or stays empty. */
    @FXML
    private Label statusLabel;

    /** The shared server session; injected by the shell after loading. */
    private ClientSession session;

    /** The session's server gateway (the dashboard fetches through it). */
    private ServerStub serverStub;

    /**
     * Initialises the view after the FXML is loaded: incident counts are whole
     * numbers, so the bar chart's auto-ranged count axis hides the fractional
     * tick labels it would otherwise show for small data sets.
     */
    @FXML
    private void initialize() {
        NumberAxis countAxis = (NumberAxis) this.hazardChart.getYAxis();
        countAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number value) {
                double ticks = value.doubleValue();
                return (ticks == Math.rint(ticks)) ? String.valueOf((long) ticks) : "";
            }

            @Override
            public Number fromString(String text) {
                return null;
            }
        });
    }

    /**
     * Injects the live server session and loads the first report. Called by the
     * application shell right after the FXML is loaded.
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
        onRefresh();
    }

    /**
     * Fetches a fresh report off the FX thread and renders it. Handler for the
     * "Refresh" button; also the initial load.
     */
    @FXML
    private void onRefresh() {
        if (this.serverStub == null) {
            return;
        }
        this.refreshButton.setDisable(true);
        this.session.runAsync(this.serverStub::getAnalytics,
                report -> {
                    this.refreshButton.setDisable(false);
                    showReport(report);
                },
                failure -> {
                    this.refreshButton.setDisable(false);
                    showError("Could not load the analytics report: "
                            + failure.getMessage());
                });
    }

    /**
     * Renders a report (FX thread): charts, summary line, response times and
     * the server-stamped generation time.
     *
     * @param report the report to show.
     */
    private void showReport(AnalyticsReport report) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<HazardType, Long> entry : report.getHazardCounts().entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().name(), entry.getValue()));
        }
        this.hazardChart.getData().setAll(List.of(series));

        this.severityChart.getData().setAll(toPieData(report.getSeverityCounts()));

        this.summaryLabel.setText(report.getTotalIncidents() + " incident(s), "
                + report.getTotalVictims() + " victim(s) estimated");
        this.generatedAtLabel.setText("Report generated "
                + report.getGeneratedAt().format(TIME_FORMAT) + " (server time)");

        ResponseTimeMetric times = report.getResponseTimes();
        if (times.getResolvedCount() == 0) {
            this.responseTimesLabel.setText("no incidents resolved yet");
        } else {
            this.responseTimesLabel.setText(times.getResolvedCount()
                    + " resolved - fastest " + times.getMinMinutes()
                    + " min, average " + String.format("%.1f", times.getAverageMinutes())
                    + " min, slowest " + times.getMaxMinutes() + " min");
        }
        this.statusLabel.getStyleClass().remove(STATUS_ERROR_CLASS);
        this.statusLabel.setText("");
    }

    /**
     * Converts the severity counts to pie-chart slices.
     *
     * @param severityCounts the per-severity counts.
     * @return one labelled slice per severity present.
     */
    private static List<PieChart.Data> toPieData(Map<Severity, Long> severityCounts) {
        List<PieChart.Data> slices = new ArrayList<>();
        for (Map.Entry<Severity, Long> entry : severityCounts.entrySet()) {
            slices.add(new PieChart.Data(
                    entry.getKey().name() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        return slices;
    }

    /**
     * Shows an error message on the status label.
     *
     * @param message the message to display.
     */
    private void showError(String message) {
        if (!this.statusLabel.getStyleClass().contains(STATUS_ERROR_CLASS)) {
            this.statusLabel.getStyleClass().add(STATUS_ERROR_CLASS);
        }
        this.statusLabel.setText(message);
    }
}
