package edu.cqu.drs.data;

/**
 * Unchecked exception that wraps a low-level {@link java.sql.SQLException} (or
 * similar data-tier failure) so that the service and presenter tiers can depend
 * on the data-access objects without importing JDBC types or handling checked
 * SQL exceptions everywhere.
 *
 * <p>This is the standard Data Access Object (DAO) pattern boundary: the DAO
 * catches the technology-specific checked exception and rethrows a single,
 * meaningful, technology-neutral runtime exception that callers can choose to
 * handle (or let propagate) as a genuine fault.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a data-access exception with a human-readable message and the
     * underlying cause.
     *
     * @param message a description of the operation that failed.
     * @param cause   the underlying exception (typically a
     *                {@link java.sql.SQLException}).
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
