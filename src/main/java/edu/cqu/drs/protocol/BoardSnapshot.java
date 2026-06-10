package edu.cqu.drs.protocol;

import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.IncidentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One server-authoritative snapshot of the live incident board (new feature f1,
 * the Live Multi-Dispatcher Board): the priority-ordered incidents plus the
 * server-clock time the snapshot was taken and the open/total counts.
 *
 * <p>This is a deliberate, distinct wire artefact rather than a reuse of the
 * bare incident list: a polling board needs a <em>server-stamped</em>
 * "last updated" time (a client clock can drift and would misstate when the
 * data was true), and the open/total counts belong to the same instant as the
 * rows - computing them client-side from a list that arrives later would let
 * the header and the rows disagree. One {@code GET_BOARD} round-trip carries
 * all three consistently.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class BoardSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The incidents at the snapshot instant, most urgent first. */
    private final List<Incident> incidents;

    /** The server-clock time the snapshot was taken. */
    private final LocalDateTime snapshotAt;

    /** How many of the incidents are not yet resolved. */
    private final int openCount;

    /**
     * Creates a snapshot, computing the open count from the rows.
     *
     * @param incidents  the incidents, most urgent first (must not be null; the
     *                   list is defensively copied).
     * @param snapshotAt the server-clock snapshot time (must not be null).
     * @throws IllegalArgumentException if either argument is null.
     */
    public BoardSnapshot(List<Incident> incidents, LocalDateTime snapshotAt) {
        if (incidents == null || snapshotAt == null) {
            throw new IllegalArgumentException("incidents and snapshotAt are required");
        }
        this.incidents = new ArrayList<>(incidents);
        this.snapshotAt = snapshotAt;
        int open = 0;
        for (Incident incident : this.incidents) {
            if (incident.getStatus() != IncidentStatus.RESOLVED) {
                open++;
            }
        }
        this.openCount = open;
    }

    /** @return an unmodifiable view of the incidents, most urgent first. */
    public List<Incident> getIncidents() {
        return Collections.unmodifiableList(this.incidents);
    }

    /** @return the server-clock time the snapshot was taken. */
    public LocalDateTime getSnapshotAt() {
        return this.snapshotAt;
    }

    /** @return how many incidents are not yet resolved. */
    public int getOpenCount() {
        return this.openCount;
    }

    /** @return the total number of incidents on the board. */
    public int getTotalCount() {
        return this.incidents.size();
    }

    /**
     * Hand-written display string (a default {@code Object.toString()} would
     * print only a Class@hash representation).
     *
     * @return e.g. {@code BoardSnapshot{open=2, total=3, at=2026-06-10T10:04:09}}.
     */
    @Override
    public String toString() {
        return "BoardSnapshot{open=" + this.openCount
                + ", total=" + getTotalCount()
                + ", at=" + this.snapshotAt + "}";
    }
}
