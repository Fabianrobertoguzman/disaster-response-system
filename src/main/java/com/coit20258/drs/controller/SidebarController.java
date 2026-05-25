package com.coit20258.drs.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import com.coit20258.drs.model.User;
import com.coit20258.drs.util.SceneManager;
import com.coit20258.drs.util.SessionContext;

public class SidebarController implements Initializable {

    // ── FXML — user labels ──────────────────────────────────────────────────
    @FXML
    private Label navUserName;
    @FXML
    private Label navUserRole;
    @FXML
    private Label navUserEmail;

    // ── FXML — nav buttons (one per view) ──────────────────────────────────
    @FXML
    private Button navDashboard;
    @FXML
    private Button navReports;
    @FXML
    private Button navAssessments;
    @FXML
    private Button navDepartments;
    @FXML
    private Button navEvacuation;
    @FXML
    private Button navResources;

    /**
     * All nav buttons in display order — used for bulk style reset.
     */
    private List<Button> allNavButtons;

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Populated after FXML injection completes
        allNavButtons = List.of(
                navDashboard, navReports, navAssessments,
                navDepartments, navEvacuation, navResources
        );
        populateUserInfo();
    }

    // ── Public API — called by AppShellController ───────────────────────────
    /**
     * Highlights the nav button that corresponds to the given view name. All
     * other buttons revert to the default {@code nav-item} style.
     *
     * @param viewName the FXML base name currently loaded in the content area
     * (e.g. {@code "DisasterReportListView"})
     */
    public void setActiveItem(String viewName) {
        Button active = resolveButton(viewName);
        for (Button btn : allNavButtons) {
            btn.getStyleClass().removeAll("nav-item-active");
            if (!btn.getStyleClass().contains("nav-item")) {
                btn.getStyleClass().add("nav-item");
            }
        }
        if (active != null) {
            active.getStyleClass().remove("nav-item");
            if (!active.getStyleClass().contains("nav-item-active")) {
                active.getStyleClass().add("nav-item-active");
            }
        }
    }

    /**
     * Refreshes the user info labels — call after a session change if needed.
     */
    public void refreshUserInfo() {
        populateUserInfo();
    }

    // ── Nav FXML handlers ───────────────────────────────────────────────────
    @FXML
    private void handleNavDashboard() {
        SceneManager.switchContent("PriorityDashboardView");
    }

    @FXML
    private void handleNavReports() {
        SceneManager.switchContent("DisasterReportListView");
    }

    @FXML
    private void handleNavAssessments() {
        SceneManager.switchContent("DisasterAssessmentView");
    }

    @FXML
    private void handleNavDepartments() {
        SceneManager.switchContent("DepartmentView");
    }

    @FXML
    private void handleNavEvacuation() {
        SceneManager.switchContent("EvacuationZoneView");
    }

    @FXML
    private void handleNavResources() {
        SceneManager.switchContent("ResourceView");
    }

    @FXML
    private void handleLogout() {
        SessionContext.clearSession();
        SceneManager.switchTo("LoginView");
    }

    // ── Private helpers ─────────────────────────────────────────────────────
    private void populateUserInfo() {
        if (!SessionContext.isLoggedIn()) {
            return;
        }
        User u = SessionContext.getCurrentUser();
        navUserName.setText(u.getFullName());
        navUserRole.setText(u.getRole());
        navUserEmail.setText(u.getEmail());
    }

    /**
     * Maps a view name to its corresponding nav button. Returns {@code null}
     * for views that have no dedicated nav entry (e.g. the form view — its
     * parent "Reports" stays active).
     */
    private Button resolveButton(String viewName) {
        if (viewName == null) {
            return null;
        }
        return switch (viewName) {
            case "PriorityDashboardView" ->
                navDashboard;
            // Both list and form keep "Reports" highlighted
            case "DisasterReportListView", "DisasterReportFormView" ->
                navReports;
            case "DisasterAssessmentView" ->
                navAssessments;
            case "DepartmentView" ->
                navDepartments;
            case "EvacuationZoneView" ->
                navEvacuation;
            case "ResourceView" ->
                navResources;
            default ->
                null;
        };
    }
}
