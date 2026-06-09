/**
 * The inherited single-process coordination tier of the DRS-Initial prototype,
 * retained in DRS-Enhanced for the components that remain deliberately local.
 *
 * <p>In the enhanced client/server build the live views no longer route through
 * the in-process presenters: the report and dispatch use cases travel to the
 * server via {@code edu.cqu.drs.client} (the session-scoped {@code ServerStub}
 * and its client-side presenters), and the business logic those presenters used
 * to host now lives in the server's {@code edu.cqu.drs.server.service} tier.</p>
 *
 * <p>What stays in active use here: {@code PartnerNotifier} (the NFR-O04
 * partner-agency fan-out, an honest <em>local stub</em> in this build - there is
 * no partner wire action), {@code AlertTemplateRecommender} (creative feature
 * FR-CR-01, now invoked by the server's {@code IncidentService}), and
 * {@code SelfTestLauncher} (creative feature FR-CR-02, the in-GUI smoke suite).
 * {@code ReportPresenter}, {@code DispatchPresenter} and {@code AppContext}'s
 * in-memory queue/roster are kept as the documented A2 baseline - exercised by
 * their unit tests and the self-test suite - but are no longer wired into the
 * running client.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.presenter;
