/**
 * Presentation tier - the JavaFX view layer: FXML-backed controllers.
 *
 * <p>Each controller ({@code ReportController}, {@code DispatchController}) is bound to an FXML
 * document authored in Scene Builder ({@code report.fxml}, {@code dispatch.fxml}, styled by
 * {@code drs.css}) and exposes {@code @FXML} event-handler methods that delegate to the presenters
 * in {@code edu.cqu.drs.presenter} (the View->Controller->Presenter->Model chain).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.view;
