/**
 * Data tier of DRS-Enhanced: the JDBC connection factory and (in later
 * increments) the Data Access Objects that persist the domain model to MySQL.
 *
 * <p>Connections are handed out one per request and closed by the caller; a
 * {@link java.sql.Connection} is never shared across server worker threads,
 * because a JDBC connection is not thread-safe.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.data;
