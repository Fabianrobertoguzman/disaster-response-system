package com.coit20258.drs.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import com.coit20258.drs.util.SceneManager;

public class AppShellController implements Initializable {

    private static final Logger LOGGER
            = Logger.getLogger(AppShellController.class.getName());

    private static final String VIEW_PATH = "/com/coit20258/drs/";

    // ── FXML injected ───────────────────────────────────────────────────────
    @FXML
    private StackPane contentArea;

    @FXML
    private SidebarController sidebarController;

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Register this controller with SceneManager so switchContent()
        // can delegate to loadContent() on the FX thread.
        SceneManager.setShellController(this);
    }

    // ── Public API — called by SceneManager ────────────────────────────────
    public void loadContent(String viewName) {
        URL fxmlUrl = getClass().getResource(VIEW_PATH + viewName + ".fxml");
        if (fxmlUrl == null) {
            LOGGER.severe("Content FXML not found: " + VIEW_PATH + viewName + ".fxml");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent content = loader.load();

            // Swap the content area
            contentArea.getChildren().setAll(content);

            // Sync the sidebar highlight
            sidebarController.setActiveItem(viewName);

            LOGGER.info("Content loaded: " + viewName);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load content: " + viewName, e);
            throw new RuntimeException(
                    "Cannot load view '" + viewName + "': " + e.getMessage(), e);
        }
    }

    public SidebarController getSidebarController() {
        return sidebarController;
    }

    public void loadContentNode(javafx.scene.Parent content, String viewName) {
        contentArea.getChildren().setAll(content);
        sidebarController.setActiveItem(viewName);
        LOGGER.info("Content node placed: " + viewName);
    }
}
