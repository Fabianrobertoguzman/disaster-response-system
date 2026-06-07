/**
 * Business tier - coordination logic mediating between the JavaFX controllers and the domain model.
 *
 * <p>The presenters ({@code ReportPresenter}, {@code DispatchPresenter}) and the coordination helpers
 * ({@code AppContext}, {@code PartnerNotifier}, {@code AlertTemplateRecommender},
 * {@code SelfTestLauncher}) live here. Controllers in {@code edu.cqu.drs.view} delegate to these
 * classes; these classes operate on {@code edu.cqu.drs.model} entities and never touch JavaFX
 * directly. This follows the MVP variant of MVC that JavaFX with FXML naturally encourages.</p>
 *
 * <p>{@code AppContext} holds the prototype's shared in-memory state (the incident queue, the
 * responder roster, the partner notifier). {@code PartnerNotifier} performs priority-ordered fan-out
 * to the registered {@code IPartnerAgency} implementations, collects their acknowledgements (one
 * retry on a non-ack), and records each notification in both the incident's {@code AuditLog} and its
 * own log. {@code AlertTemplateRecommender} is creative feature FR-CR-01 (the rule-driven CAP-alert
 * recommender). {@code SelfTestLauncher} is creative feature FR-CR-02 (it runs an in-process suite
 * of smoke checks against the domain model and returns a {@code TestRunReport}).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.presenter;
