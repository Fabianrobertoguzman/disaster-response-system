package com.coit20258.drs.util;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SceneManager {

    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());

    private static final String VIEW_PATH = "/com/coit20258/drs/";

    private static final String CSS_PATH = "/com/coit20258/drs/styles/drs-styles.css";

    private static Stage primaryStage;
    private static double stageWidth = 1024;
    private static double stageHeight = 768;

    private SceneManager() {
    }

    // =========================================================================
    // Initialisation
    // =========================================================================
    /**
     * Binds the primary stage and sets the default window dimensions. Call once
     * from {@code Application.start()}.
     *
     * @param stage the application's primary stage
     * @param width initial window width (px)
     * @param height initial window height (px)
     */
    public static void init(Stage stage, double width, double height) {
        primaryStage = stage;
        stageWidth = width;
        stageHeight = height;
        primaryStage.setTitle("Disaster Response System");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
    }

    // =========================================================================
    // Navigation
    // =========================================================================
    /**
     * Loads the named FXML view and replaces the current scene on the primary
     * stage.
     *
     * @param viewName the FXML filename without extension (e.g.
     * {@code "LoginView"})
     * @throws RuntimeException if the FXML cannot be located or loaded
     */
    public static void switchTo(String viewName) {
        requireInit();

        String resourcePath = VIEW_PATH + viewName + ".fxml";
        URL fxmlUrl = SceneManager.class.getResource(resourcePath);

        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "FXML resource not found: " + resourcePath
                    + "  — check that the file exists under src/main/resources.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, stageWidth, stageHeight);

            // Attach the shared stylesheet (gracefully skip if missing)
            URL cssUrl = SceneManager.class.getResource(CSS_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                LOGGER.warning("Stylesheet not found at: " + CSS_PATH);
            }

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view: " + viewName, e);
            throw new RuntimeException("Cannot load view '" + viewName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Loads a view and returns its controller, allowing the caller to inject
     * data before the view is displayed.
     *
     * <pre>
     *   SomeController ctrl = SceneManager.switchToWithController("SomeView");
     *   ctrl.initData(myModel);
     * </pre>
     *
     * @param <T> the controller type
     * @param viewName the FXML filename without extension
     * @return the controller instance created by the FXMLLoader
     */
    public static <T> T switchToWithController(String viewName) {
        requireInit();

        String resourcePath = VIEW_PATH + viewName + ".fxml";
        URL fxmlUrl = SceneManager.class.getResource(resourcePath);

        if (fxmlUrl == null) {
            throw new RuntimeException("FXML resource not found: " + resourcePath);
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, stageWidth, stageHeight);
            URL cssUrl = SceneManager.class.getResource(CSS_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            return loader.getController();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view: " + viewName, e);
            throw new RuntimeException("Cannot load view '" + viewName + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Accessors
    // =========================================================================
    /**
     * @return the primary stage (never null after {@link #init})
     */
    public static Stage getPrimaryStage() {
        requireInit();
        return primaryStage;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================
    private static void requireInit() {
        if (primaryStage == null) {
            throw new IllegalStateException(
                    "SceneManager has not been initialised. Call SceneManager.init() first.");
        }
    }
}
