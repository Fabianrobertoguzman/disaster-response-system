package edu.cqu.drs;

import edu.cqu.drs.model.TestRunReport;
import edu.cqu.drs.presenter.SelfTestLauncher;
import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for DRS-Initial (COIT20258 Assessment 2).
 *
 * <p>Hosts the top-level {@link MenuBar} that navigates between the prototype's
 * views, swapping the {@link BorderPane} centre, so that every implemented
 * capability is reachable in the running application:</p>
 * <ul>
 *   <li><b>File -> Report a Disaster</b>  -  the citizen-report view,
 *       loaded from {@code report.fxml} (criterion 1); the application opens
 *       on this view.</li>
 *   <li><b>View -> Dispatcher Console</b>  -  the triage/dispatch view,
 *       loaded from {@code dispatch.fxml}: the incident queue, triage and
 *       resolution (criterion 2), field-responder allocation (FR-05), and
 *       partner-agency notification (NFR-O04).</li>
 *   <li><b>Tools -> Run Self-Tests</b>  -  runs the in-GUI self-test
 *       suite ({@code SelfTestLauncher}) and shows the result in a dialog
 *       (creative feature FR-CR-02).</li>
 * </ul>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class App extends Application {

    /** Window title shown in the stage's title bar. */
    private static final String WINDOW_TITLE =
            "DRS-Initial - Disaster Response System";

    /** Default window width in pixels. */
    private static final double DEFAULT_WIDTH = 1000;

    /** Default window height in pixels. */
    private static final double DEFAULT_HEIGHT = 660;

    /** Classpath location of the citizen-report FXML. */
    private static final String REPORT_FXML = "/edu/cqu/drs/view/report.fxml";

    /** Classpath location of the dispatcher-console FXML. */
    private static final String DISPATCH_FXML =
            "/edu/cqu/drs/view/dispatch.fxml";

    /** Classpath location of the shared stylesheet. */
    private static final String STYLESHEET = "/edu/cqu/drs/view/drs.css";

    /** The application root; its centre is swapped on each navigation. */
    private BorderPane root;

    /**
     * Builds and shows the primary stage: a {@link BorderPane} with the
     * navigation {@link MenuBar} at the top and the citizen-report view in the
     * centre.
     *
     * @param primaryStage the primary stage supplied by the JavaFX runtime
     *                     (never null).
     */
    @Override
    public void start(Stage primaryStage) {
        this.root = new BorderPane();
        this.root.setTop(buildMenuBar());
        this.root.setCenter(loadView(REPORT_FXML));

        Scene scene = new Scene(this.root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        applyStylesheet(scene);
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Constructs the navigation menu bar (File / View / Tools). "Report a
     * Disaster" loads {@code report.fxml}; "Dispatcher Console" loads
     * {@code dispatch.fxml}; "Run Self-Tests" runs the in-GUI self-test
     * suite (see {@link #runSelfTests()}).
     *
     * @return the populated {@link MenuBar}.
     */
    private MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem reportItem = new MenuItem("Report a Disaster");
        reportItem.setOnAction(e -> this.root.setCenter(loadView(REPORT_FXML)));
        fileMenu.getItems().add(reportItem);

        Menu viewMenu = new Menu("View");
        MenuItem dispatchItem = new MenuItem("Dispatcher Console");
        dispatchItem.setOnAction(
                e -> this.root.setCenter(loadView(DISPATCH_FXML)));
        viewMenu.getItems().add(dispatchItem);

        Menu toolsMenu = new Menu("Tools");
        MenuItem selfTestItem = new MenuItem("Run Self-Tests");
        selfTestItem.setOnAction(e -> runSelfTests());
        toolsMenu.getItems().add(selfTestItem);

        // All eight partner agencies named in Assessment One section 2.1 /
        // NFR-O04 are implemented via the IPartnerAgency interface. Adding a
        // ninth agency is a new class implementing IPartnerAgency plus a single
        // line in AppContext; the notifier itself does not change.

        return new MenuBar(fileMenu, viewMenu, toolsMenu);
    }

    /**
     * Runs the in-GUI self-test suite (creative feature FR-CR-02) and shows
     * the result in a dialog (information if all checks pass, warning if any
     * fail).
     */
    private void runSelfTests() {
        TestRunReport report = new SelfTestLauncher().runAllTests();
        StringBuilder body = new StringBuilder();
        body.append(report.getPassed()).append(" of ")
                .append(report.getTestsRun())
                .append(" self-checks passed in ")
                .append(report.getDurationMs()).append(" ms.");
        for (String failure : report.getFailureSummaries()) {
            body.append("\n  FAILED: ").append(failure);
        }
        Alert.AlertType type = report.isAllGreen()
                ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(type, body.toString());
        alert.setHeaderText(report.isAllGreen()
                ? "Self-tests: all green"
                : "Self-tests: " + report.getFailed() + " failure(s)");
        alert.showAndWait();
    }

    /**
     * Loads an FXML view from the classpath.
     *
     * @param resourcePath the classpath location of the FXML (never null).
     * @return the loaded view's root node, or a placeholder label if the
     *         resource cannot be found or loaded.
     */
    private Parent loadView(String resourcePath) {
        URL location = getClass().getResource(resourcePath);
        if (location == null) {
            return placeholder("Could not find " + resourcePath
                    + " on the classpath.");
        }
        try {
            return FXMLLoader.load(location);
        } catch (IOException ex) {
            return placeholder("Could not load " + resourcePath + ": "
                    + ex.getMessage());
        }
    }

    /**
     * Builds a simple centred message node for views not yet wired.
     *
     * @param message the text to display (never null).
     * @return a styled {@link Label} carrying the message.
     */
    private Label placeholder(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("placeholder");
        return label;
    }

    /**
     * Attaches the shared stylesheet to the scene, if it is on the classpath.
     *
     * @param scene the scene to style (never null).
     */
    private void applyStylesheet(Scene scene) {
        URL stylesheet = getClass().getResource(STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    /**
     * Standard Java entry point - launches the JavaFX runtime.
     *
     * @param args command-line arguments (unused).
     */
    public static void main(String[] args) {
        launch(args);
    }
}
