package com.coit20258.drs.util;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.coit20258.drs.controller.AppShellController;

public final class SceneManager {

    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());

    private static final String VIEW_PATH = "/com/coit20258/drs/";

    private static final String[] CSS_PATHS = {
        "/com/coit20258/drs/styles/drs-styles.css",
        "/com/coit20258/drs/styles/nav-styles.css"
    };

    /**
     * Views that replace the full scene (no sidebar shell). Every other view
     * name is routed through the shell.
     */
    private static final Set<String> FULL_SCENE_VIEWS = Set.of(
            "LoginView", "RegisterView"
    );

    private static Stage primaryStage;
    private static double stageWidth = 1100;
    private static double stageHeight = 720;
    private static AppShellController shellController;

    private SceneManager() {
    }

    // =========================================================================
    // Initialisation
    // =========================================================================
    /**
     * Binds the primary stage. Call once from {@code Application.start()}.
     *
     * @param stage the JavaFX primary stage
     * @param width initial window width
     * @param height initial window height
     */
    public static void init(Stage stage, double width, double height) {
        primaryStage = stage;
        stageWidth = width;
        stageHeight = height;
        primaryStage.setTitle("Disaster Response System");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
    }

    /**
     * Called by {@link AppShellController#initialize} to register itself. This
     * happens synchronously during {@code FXMLLoader.load()}, so it is always
     * set before any subsequent {@link #switchContent} call.
     */
    public static void setShellController(AppShellController controller) {
        shellController = controller;
        LOGGER.info("AppShellController registered with SceneManager.");
    }

    // =========================================================================
    // Navigation — full scene (Login / Register)
    // =========================================================================
    /**
     * Replaces the entire scene on the primary stage with the named view. Use
     * this only for {@code LoginView} and {@code RegisterView}.
     *
     * <p>
     * Also clears the shell reference so the next {@link #switchContent} call
     * will reload the shell from scratch.
     *
     * @param viewName FXML base name (e.g. {@code "LoginView"})
     * @return 
     */
    public static Object switchTo(String viewName) {
        requireInit();

        // Navigating to an auth view — discard the shell
        if (FULL_SCENE_VIEWS.contains(viewName)) {
            shellController = null;
        }

        URL fxmlUrl = resolveView(viewName);

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = buildScene(root);

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            LOGGER.info("Full scene switched to: " + viewName);
            return loader.getController();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view: " + viewName, e);
            throw new RuntimeException(
                    "Cannot load view '" + viewName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Variant that returns the loaded controller — used when the caller needs
     * to inject data before the view is shown (e.g. passing a success message
     * from RegisterController back to LoginController).
     *
     * @param <T> controller type
     * @param viewName FXML base name
     * @return the controller created by the FXMLLoader
     */
    public static <T> T switchToWithController(String viewName) {
        requireInit();
        if (FULL_SCENE_VIEWS.contains(viewName)) {
            shellController = null;
        }

        URL fxmlUrl = resolveView(viewName);
        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = buildScene(root);

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            LOGGER.info("Full scene switched to: " + viewName);
            return loader.getController();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view: " + viewName, e);
            throw new RuntimeException(
                    "Cannot load view '" + viewName + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Navigation — content swap inside the shell (authenticated views)
    // =========================================================================
    /**
     * Loads the named view into the shell's content area.
     *
     * <p>
     * If the shell is not yet on stage (first call after login), it is loaded
     * automatically before the content is injected.
     *
     * @param viewName FXML base name (e.g. {@code "DisasterReportListView"})
     */
    public static void switchContent(String viewName) {
        requireInit();

        if (shellController == null) {
            loadShell();          // loads AppShellView, which registers the controller
        }

        shellController.loadContent(viewName);
        LOGGER.info("Content switched to: " + viewName);
    }

    /**
     * Same as {@link #switchContent} but returns the loaded content controller
     * so the caller can inject data into it.
     *
     * <p>
     * Because the content is loaded inside
     * {@link AppShellController#loadContent}, use this overload only when you
     * need to pass data to the content controller before it renders. The shell
     * and sidebar are not affected.
     *
     * @param <T> controller type
     * @param viewName FXML base name
     * @return the content controller
     */
    public static <T> T switchContentWithController(String viewName) {
        requireInit();
        if (shellController == null) {
            loadShell();
        }

        URL fxmlUrl = resolveView(viewName);
        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent content = loader.load();

            shellController.loadContentNode(content, viewName);

            return loader.getController();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load content: " + viewName, e);
            throw new RuntimeException(
                    "Cannot load content '" + viewName + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Accessors
    // =========================================================================
    public static Stage getPrimaryStage() {
        requireInit();
        return primaryStage;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================
    /**
     * Loads {@code AppShellView.fxml} as the primary scene.
     * {@code AppShellController.initialize()} registers itself via
     * {@link #setShellController} during {@code loader.load()}, so
     * {@code shellController} is guaranteed non-null after this returns.
     */
    private static void loadShell() {
        URL shellUrl = resolveView("AppShellView");
        try {
            FXMLLoader loader = new FXMLLoader(shellUrl);
            Parent root = loader.load();          // triggers AppShellController.initialize()
            Scene scene = buildScene(root);

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            LOGGER.info("AppShellView loaded onto stage.");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load AppShellView", e);
            throw new RuntimeException("Cannot load application shell: " + e.getMessage(), e);
        }
    }

    private static URL resolveView(String viewName) {
        String path = VIEW_PATH + viewName + ".fxml";
        URL url = SceneManager.class.getResource(path);
        if (url == null) {
            throw new RuntimeException(
                    "FXML not found: " + path
                    + " — check that the file exists under src/main/resources.");
        }
        return url;
    }

    private static Scene buildScene(Parent root) {
        Scene scene = new Scene(root, stageWidth, stageHeight);
        for (String cssPath : CSS_PATHS) {
            URL cssUrl = SceneManager.class.getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                LOGGER.warning("Stylesheet not found: " + cssPath);
            }
        }
        return scene;
    }

    private static void requireInit() {
        if (primaryStage == null) {
            throw new IllegalStateException(
                    "SceneManager not initialised. Call SceneManager.init() first.");
        }
    }
}
