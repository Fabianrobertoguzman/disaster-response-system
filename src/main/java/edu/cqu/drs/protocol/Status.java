package edu.cqu.drs.protocol;

/**
 * The outcome status carried by a {@link Response}.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum Status {

    /** The request succeeded; any result is in the response payload. */
    OK,

    /** The request was understood but failed during processing. */
    ERROR,

    /** The request was rejected because the caller is not authenticated or lacks the required role. */
    UNAUTHORIZED,

    /** The request was malformed or missing required parameters. */
    BAD_REQUEST,

    /** The referenced entity does not exist. */
    NOT_FOUND
}
