package edu.cqu.drs;

import edu.cqu.drs.client.ClientSession;
import edu.cqu.drs.model.TestRunReport;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.presenter.SelfTestLauncher;
import edu.cqu.drs.view.AnalyticsDashboardController;
import edu.cqu.drs.view.DispatchController;
import edu.cqu.drs.view.LiveBoardController;
import edu.cqu.drs.view.LoginController;
import edu.cqu.drs.view.ReportController;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
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
 * JavaFX application entry point for the DRS-Enhanced <em>client</em>
 * (COIT20258 Assessment 3).
 *
 * <p>The client boots to the login view; every operation beyond that point runs
 * over one session-scoped server connection. After a successful login the shell
 * builds a <strong>role-adapted</strong> {@link MenuBar} - mirroring the
 * server's own authorisation gate, so the UI never offers an action the server
 * would deny:</p>
 * <ul>
 *   <li><b>File -&gt; Report a Disaster</b> - every role (the citizen
 *       surface); reports travel to the server.</li>
 *   <li><b>View -&gt; Dispatcher Console</b> - DISPATCHER / ADMINISTRATOR
 *       only: triage, allocation and resolution, all performed on the
 *       server.</li>
 *   <li><b>View -&gt; Live Board</b> - DISPATCHER / ADMINISTRATOR only:
 *       the polling Live Multi-Dispatcher Board (new feature f1).</li>
 *   <li><b>View -&gt; Analytics Dashboard</b> - DISPATCHER / ADMINISTRATOR
 *       only: the Damage Assessment &amp; Analytics Dashboard (new feature
 *       f2).</li>
 *   <li><b>Tools -&gt; Run Self-Tests</b> - the inherited in-GUI self-test
 *       suite (creative feature FR-CR-02).</li>
 *   <li><b>Session -&gt; Sign out</b> - ends the server session and returns
 *       to the login view.</li>
 * </ul>
 *
 * <p>Network calls never run on the FX Application Thread: the session's
 * callback dispatcher is pointed at {@link Platform#runLater(Runnable)} once,
 * here, so every view's server callback lands on the FX thread.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class App extends Application {

    /** Window title shown in the stage's title bar. */
    private static final String WINDOW_TITLE =
            "DRS-Enhanced - Disaster Response System";

    /** Default window width in pixels. */
    private static final double DEFAULT_WIDTH = 1000;

    /** Default window height in pixels. */
    private static final double DEFAULT_HEIGHT = 660;

    /** Classpath location of the login FXML. */
    private static final String LOGIN_FXML = "/edu/cqu/drs/view/login.fxml";

    /** Classpath location of the citizen-report FXML. */
    private static final String REPORT_FXML = "/edu/cqu/drs/view/report.fxml";

    /** Classpath location of the dispatcher-console FXML. */
    private static final String DISPATCH_FXML =
            "/edu/cqu/drs/view/dispatch.fxml";

    /** Classpath location of the live-board FXML (feature f1). */
    private static final String LIVE_BOARD_FXML =
            "/edu/cqu/drs/view/liveBoard.fxml";

    /** Classpath location of the analytics-dashboard FXML (feature f2). */
    private static final String ANALYTICS_FXML =
            "/edu/cqu/drs/view/analyticsDashboard.fxml";

    /** Classpath location of the shared stylesheet. */
    private static final String STYLESHEET = "/edu/cqu/drs/view/drs.css";

    /** The application root; its centre is swapped on each navigation. */
    private BorderPane root;

    /** The live server session, or null while the login view is showing. */
    private ClientSession session;

    /**
     * Builds and shows the primary stage, opening on the login view. The rest
     * of the UI is constructed only after a successful login.
     *
     * @param primaryStage the primary stage supplied by the JavaFX runtime
     *                     (never null).
     */
    @Override
    public void start(Stage primaryStage) {
        this.root = new BorderPane();
        showLogin();

        Scene scene = new Scene(this.root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        applyStylesheet(scene);
        primaryStage.setTitle(WINDOW_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> endSession(null));
        primaryStage.show();
    }

    /**
     * Shows the login view (clearing any menu bar) and registers the
     * logged-in callback that builds the operations UI.
     */
    private void showLogin() {
        this.root.setTop(null);
        this.root.setCenter(loadView(LOGIN_FXML,
                (LoginController controller) ->
                        controller.setOnLoginSuccess(this::onLoggedIn)));
    }

    /**
     * Takes over after a successful login: routes every session callback to the
     * FX thread, then builds the role-adapted menu and opens the report view.
     *
     * @param newSession the connected, authenticated session (never null).
     */
    private void onLoggedIn(ClientSession newSession) {
        this.session = newSession;
        this.session.setCallbackDispatcher(Platform::runLater);
        this.root.setTop(buildMenuBar());
        this.root.setCenter(loadView(REPORT_FXML,
                (ReportController controller) -> controller.init(this.session)));
    }

    /**
     * Constructs the role-adapted navigation menu bar. The dispatcher console
     * is offered only to the roles the server's authorisation gate admits to
     * the dispatch actions (DISPATCHER and ADMINISTRATOR).
     *
     * @return the populated {@link MenuBar}.
     */
    private MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem reportItem = new MenuItem("Report a Disaster");
        reportItem.setOnAction(e -> this.root.setCenter(loadView(REPORT_FXML,
                (ReportController controller) -> controller.init(this.session))));
        fileMenu.getItems().add(reportItem);

        MenuBar menuBar = new MenuBar(fileMenu);

        UserRole role = this.session.getRole();
        if (role == UserRole.DISPATCHER || role == UserRole.ADMINISTRATOR) {
            Menu viewMenu = new Menu("View");
            MenuItem dispatchItem = new MenuItem("Dispatcher Console");
            dispatchItem.setOnAction(e -> this.root.setCenter(loadView(DISPATCH_FXML,
                    (DispatchController controller) -> controller.init(this.session))));
            MenuItem liveBoardItem = new MenuItem("Live Board");
            liveBoardItem.setOnAction(e -> this.root.setCenter(loadView(LIVE_BOARD_FXML,
                    (LiveBoardController controller) -> controller.init(this.session))));
            MenuItem analyticsItem = new MenuItem("Analytics Dashboard");
            analyticsItem.setOnAction(e -> this.root.setCenter(loadView(ANALYTICS_FXML,
                    (AnalyticsDashboardController controller) ->
                            controller.init(this.session))));
            viewMenu.getItems().addAll(dispatchItem, liveBoardItem, analyticsItem);
            menuBar.getMenus().add(viewMenu);
        }

        Menu toolsMenu = new Menu("Tools");
        MenuItem selfTestItem = new MenuItem("Run Self-Tests");
        selfTestItem.setOnAction(e -> runSelfTests());
        toolsMenu.getItems().add(selfTestItem);
        menuBar.getMenus().add(toolsMenu);

        Menu sessionMenu = new Menu("Signed in as "
                + this.session.getUser().getUsername() + " (" + role + ")");
        MenuItem signOutItem = new MenuItem("Sign out");
        signOutItem.setOnAction(e -> endSession(this::showLogin));
        sessionMenu.getItems().add(signOutItem);
        menuBar.getMenus().add(sessionMenu);

        return menuBar;
    }

    /**
     * Ends the live session, if any, off the FX thread (best-effort logout,
     * then connection close), and optionally continues with a follow-up action.
     *
     * @param andThen what to do once the session is closed (run on the FX
     *                thread), or null for nothing (window close).
     */
    private void endSession(Runnable andThen) {
        ClientSession ending = this.session;
        this.session = null;
        // Tear the session UI down immediately so no menu item can fire against
        // a session that is signing out (the close itself completes off-thread).
        this.root.setTop(null);
        this.root.setCenter(placeholder("Signing out..."));
        if (ending == null) {
            if (andThen != null) {
                andThen.run();
            }
            return;
        }
        ending.signOut(andThen != null ? andThen : () -> { });
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
     * Loads an FXML view from the classpath and hands its controller to the
     * given initialiser - the injection seam that gives each view the shared
     * {@link ClientSession} while the FXML keeps its {@code fx:controller}
     * attribute.
     *
     * @param <C>          the controller type declared by the FXML.
     * @param resourcePath the classpath location of the FXML (never null).
     * @param initialiser  receives the loaded controller (never null).
     * @return the loaded view's root node, or a placeholder label if the
     *         resource cannot be found or loaded.
     */
    private <C> Parent loadView(String resourcePath, Consumer<C> initialiser) {
        URL location = getClass().getResource(resourcePath);
        if (location == null) {
            return placeholder("Could not find " + resourcePath
                    + " on the classpath.");
        }
        try {
            FXMLLoader loader = new FXMLLoader(location);
            Parent view = loader.load();
            C controller = loader.getController();
            if (controller == null) {
                return placeholder(resourcePath + " declares no fx:controller.");
            }
            initialiser.accept(controller);
            return view;
        } catch (IOException | ClassCastException ex) {
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
