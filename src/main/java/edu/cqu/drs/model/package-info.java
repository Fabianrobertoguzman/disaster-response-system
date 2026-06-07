/**
 * Data tier - the DRS-Initial domain model.
 *
 * <p>Holds the domain classes the system reasons about: {@code Incident} and its value enums
 * ({@code HazardType}, {@code Severity}, {@code IncidentStatus}, {@code AlertTemplate}),
 * {@code GpsCoordinate}, the in-memory {@code IncidentQueue}, {@code Responder}, {@code Resource},
 * {@code User}, {@code AuditLog}, the {@code IPartnerAgency} interface and its concrete agency stubs,
 * and the {@code TestRunReport} value object for the self-test feature.</p>
 *
 * <p>Every class in this package exposes a constructor, accessor and mutator methods for its mutable
 * attributes (qualified with {@code this.} per the Unit Java Coding Standards Rule 40), and a
 * hand-written {@code toString()} method  -  the IDE-generated {@code toString()} does not produce
 * the required display format (Assessment 2 specification section 3).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.model;
