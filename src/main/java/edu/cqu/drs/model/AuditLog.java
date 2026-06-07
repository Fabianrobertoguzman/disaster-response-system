package edu.cqu.drs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An append-only log of significant actions taken on an {@link Incident}
 * (triage, responder allocation, partner-agency notification, resolution).
 *
 * <p>Realises the data side of Assessment One's FR-14 ("an append-only audit
 * log of triage, allocation, broadcast and closure actions")  -  the
 * prototype holds the log in memory; the CSV/JSON/PDF <em>export</em> of FR-14
 * is an Assessment 3 item. Entries can only be added, never modified or removed
 * (the list returned by {@link #getEntries()} is unmodifiable).</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AuditLog {

    /** Chronological list of timestamped entry strings. */
    private final List<String> entries;

    /**
     * Creates an empty audit log.
     */
    public AuditLog() {
        this.entries = new ArrayList<>();
    }

    /**
     * @return the number of entries currently in the log.
     */
    public int size() {
        return this.entries.size();
    }

    /**
     * @return an unmodifiable, chronologically-ordered view of the log entries.
     */
    public List<String> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    /**
     * Appends a timestamped entry. Entries can never be edited or removed afterwards.
     *
     * @param message the action description to log (must not be null or blank).
     * @throws IllegalArgumentException if {@code message} is null or blank.
     */
    public void record(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("audit message must not be null or blank");
        }
        this.entries.add(LocalDateTime.now() + " | " + message);
    }

    /**
     * Hand-written display string with singular/plural handling (Object's default toString returns only Class@hash).
     *
     * @return e.g. {@code "AuditLog{2 entries: ...; ...}"} (or {@code "AuditLog{0 entries}"}).
     */
    @Override
    public String toString() {
        int count = this.entries.size();
        String noun = (count == 1) ? " entry" : " entries";
        if (count == 0) {
            return "AuditLog{0 entries}";
        }
        return "AuditLog{" + count + noun + ": " + String.join("; ", this.entries) + "}";
    }
}
