package edu.cqu.drs.model;

/**
 * The role catalogue a {@link User} account may hold.
 *
 * <p>Taken from Assessment&nbsp;One's use case UC-09 ("Manage Users &amp; Permissions") and FR-12:
 * the Administrator assigns role-based permissions drawn from the Citizen, Dispatcher, Field Responder,
 * Public Information Officer, Administrator and Auditor catalogue.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum UserRole {

    /** A member of the public who can submit incident reports (UC-01). */
    CITIZEN,

    /** An operator who triages incidents and allocates resources/responders (UC-02, UC-03, UC-04). */
    DISPATCHER,

    /** A responder who acknowledges dispatches and posts status updates (UC-05, UC-07). */
    FIELD_RESPONDER,

    /** An officer who composes and approves public alerts (UC-06). */
    PUBLIC_INFORMATION_OFFICER,

    /** A user who manages accounts and runs disaster drills (UC-09, UC-12). */
    ADMINISTRATOR,

    /** A user who reviews and exports the audit log for compliance (UC-11). */
    AUDITOR
}
