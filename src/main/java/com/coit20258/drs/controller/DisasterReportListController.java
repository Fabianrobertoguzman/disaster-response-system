package com.coit20258.drs.controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import com.coit20258.drs.dao.DisasterReportDao;
import com.coit20258.drs.dao.DisasterReportDaoImpl;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.User;
import com.coit20258.drs.util.SceneManager;

public class DisasterReportListController implements Initializable {

    private static final Logger LOGGER
            = Logger.getLogger(DisasterReportListController.class.getName());
    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML ───────────────────────────────────────────────────────────────
    @FXML
    private TableView<DisasterReport> reportsTable;
    @FXML
    private TableColumn<DisasterReport, String> colType;
    @FXML
    private TableColumn<DisasterReport, String> colLocation;
    @FXML
    private TableColumn<DisasterReport, String> colSeverity;
    @FXML
    private TableColumn<DisasterReport, String> colStatus;
    @FXML
    private TableColumn<DisasterReport, User> colReportedBy;
    @FXML
    private TableColumn<DisasterReport, LocalDateTime> colReportedAt;
    @FXML
    private TableColumn<DisasterReport, Void> colAction;
    @FXML
    private Label bannerLabel;

    private final DisasterReportDao reportDao = new DisasterReportDaoImpl();
    private final ObservableList<DisasterReport> reportData
            = FXCollections.observableArrayList();

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        reportsTable.setItems(reportData);
        loadReportsAsync();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleNewReport() {
        SceneManager.switchContent("DisasterReportFormView");
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private void setupTableColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("disasterType"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReportedBy.setCellValueFactory(new PropertyValueFactory<>("reportedBy"));
        colReportedAt.setCellValueFactory(new PropertyValueFactory<>("reportedAt"));

        colSeverity.setCellFactory(col -> buildSeverityCell());
        colStatus.setCellFactory(col -> buildStatusComboCell());
        colReportedBy.setCellFactory(col -> buildReportedByCell());
        colReportedAt.setCellFactory(col -> buildDateCell());
//        colAction.setCellFactory(col -> buildActionCell());
    }

    private void loadReportsAsync() {
        new Thread(() -> {
            try {
                List<DisasterReport> reports = reportDao.findAll();
                Platform.runLater(() -> reportData.setAll(reports));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load reports", ex);
                Platform.runLater(() -> showBanner(
                        "Could not load reports: " + ex.getMessage(), false));
            }
        }, "load-reports-thread").start();
    }

    private void showBanner(String msg, boolean success) {
        bannerLabel.setText(msg);
        bannerLabel.getStyleClass().removeAll("success-label", "validation-label");
        bannerLabel.getStyleClass().add(success ? "success-label" : "validation-label");
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
    }

    // ── Cell factory builders ──────────────────────────────────────────────
    private TableCell<DisasterReport, String> buildSeverityCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String sev, boolean empty) {
                super.updateItem(sev, empty);
                if (empty || sev == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(sev);
            }
        };
    }

    private TableCell<DisasterReport, String> buildStatusComboCell() {
        return new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>();

            {
                combo.getItems().addAll(
                        DisasterReport.STATUS_REPORTED,
                        DisasterReport.STATUS_RESOLVED,
                        DisasterReport.STATUS_RESPONDING,
                        DisasterReport.STATUS_UNDER_ASSESSMENT
                );
                combo.getStyleClass().add("status-combo");
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                combo.setOnAction(null);
                combo.setValue(status);
                combo.setOnAction(e -> {
                    DisasterReport r = getTableView().getItems().get(getIndex());
                    String selected = combo.getValue();
                    if (selected != null && selected != r.getStatus()) {
                        new Thread(() -> {
                            try {
                                reportDao.updateStatus(r.getId(), selected);
                                r.setStatus(selected);
                                Platform.runLater(() -> getTableView().refresh());
                            } catch (Exception ex) {
                                LOGGER.warning("Status update failed: " + ex.getMessage());
                                Platform.runLater(() -> combo.setValue(r.getStatus()));
                            }
                        }, "status-update-thread").start();
                    }
                });
                setGraphic(combo);
            }
        };
    }

    /**
     * Shows the reporter's full name (hydrated via JOIN in the DAO).
     */
    private TableCell<DisasterReport, User> buildReportedByCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                DisasterReport r = getTableView().getItems().get(getIndex());
                User u = r.getReportedBy();
                setText(u != null ? u.getFullName() : "—");
            }
        };
    }

    private TableCell<DisasterReport, LocalDateTime> buildDateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText((empty || dt == null) ? null : dt.format(DATE_FMT));
            }
        };
    }

//    private TableCell<DisasterReport, Void> buildActionCell() {
//        return new TableCell<>() {
//            private final Button btn = new Button("View Assessment");
//
//            {
//                btn.getStyleClass().add("btn-action");
//                btn.setOnAction(e -> {
//                    DisasterReport r = getTableView().getItems().get(getIndex());
//                    DisasterAssessmentController ctrl
//                            = SceneManager.switchContentWithController("DisasterAssessmentView");
//                    ctrl.loadContext(r);
//                });
//            }
//
//            @Override
//            protected void updateItem(Void item, boolean empty) {
//                super.updateItem(item, empty);
//                setGraphic(empty ? null : btn);
//            }
//        };
//    }
}
