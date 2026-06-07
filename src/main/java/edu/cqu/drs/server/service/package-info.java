/**
 * Business (service) tier of DRS-Enhanced - the server-side application logic the
 * A2 presenters became once the system was split into client and server.
 *
 * <p>Each service ({@link edu.cqu.drs.server.service.IncidentService}) coordinates
 * the domain model and persists changes through the data-tier DAO
 * <em>interfaces</em>, recording mutating actions in the audit trail. Depending
 * only on interfaces keeps the services testable without a database and identical
 * over the JDBC or in-memory backends.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.server.service;
