package edu.cqu.drs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AuditLog} (the append-only action log - FR-14 data side).
 *
 * <p>Covers the normal path (record, accumulate, read), the abnormal case (null/blank message),
 * the append-only invariant (the entries list is unmodifiable), and the custom {@code toString}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("AuditLog - append-only action log")
class AuditLogSpec {

    /** A fresh, empty log before each test. */
    private AuditLog log;

    /** Arrange: an empty audit log. */
    @BeforeEach
    void setUp() {
        this.log = new AuditLog();
    }

    @Test
    @DisplayName("a new log is empty")
    void shouldStartEmpty() {
        assertEquals(0, this.log.size());
        assertTrue(this.log.getEntries().isEmpty());
    }

    @Test
    @DisplayName("recording an entry appends it, prefixed by a timestamp")
    void shouldAppendTimestampedEntry() {
        this.log.record("Triaged: severity=HIGH");
        assertEquals(1, this.log.size());
        String entry = this.log.getEntries().get(0);
        assertTrue(entry.contains("Triaged: severity=HIGH"));
        assertTrue(entry.length() > "Triaged: severity=HIGH".length(), "a timestamp should have been prepended");
    }

    @Test
    @DisplayName("entries accumulate in chronological order")
    void shouldAccumulateInOrder() {
        this.log.record("first action");
        this.log.record("second action");
        assertEquals(2, this.log.size());
        assertTrue(this.log.getEntries().get(0).contains("first action"));
        assertTrue(this.log.getEntries().get(1).contains("second action"));
    }

    @Test
    @DisplayName("a null or blank message is rejected")
    void shouldRejectBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> this.log.record(null));
        assertThrows(IllegalArgumentException.class, () -> this.log.record("   "));
    }

    @Test
    @DisplayName("the entries list is unmodifiable (the append-only invariant cannot be bypassed)")
    void entriesAreUnmodifiable() {
        this.log.record("an action");
        assertThrows(UnsupportedOperationException.class, () -> this.log.getEntries().add("tampered entry"));
    }

    @Test
    @DisplayName("toString uses the custom 'AuditLog{...}' format, with singular/plural agreement")
    void shouldFormatCustomToString() {
        assertEquals("AuditLog{0 entries}", this.log.toString());
        this.log.record("an action");
        assertTrue(this.log.toString().startsWith("AuditLog{"));
        assertTrue(this.log.toString().contains("1 entry"));
        this.log.record("a second action");
        assertTrue(this.log.toString().contains("2 entries"));
    }
}
