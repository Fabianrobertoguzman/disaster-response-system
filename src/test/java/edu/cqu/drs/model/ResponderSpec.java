package edu.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Responder} (a field responder - the "people" half of the coordination capability).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Responder - field responder")
class ResponderSpec {

    @Test
    @DisplayName("a new responder is available, has the given name, and has a non-null id")
    void shouldStartAvailable() {
        Responder responder = new Responder("Alex Tan");
        assertNotNull(responder.getId());
        assertEquals("Alex Tan", responder.getName());
        assertNull(responder.getCurrentTaskingId());
        assertTrue(responder.isAvailable());
    }

    @Test
    @DisplayName("a null or blank name is rejected")
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Responder(null));
        assertThrows(IllegalArgumentException.class, () -> new Responder("   "));
    }

    @Test
    @DisplayName("setting a current tasking marks the responder unavailable; clearing it makes them available again")
    void taskingTogglesAvailability() {
        Responder responder = new Responder("Alex Tan");
        UUID tasking = UUID.randomUUID();
        responder.setCurrentTaskingId(tasking);
        assertEquals(tasking, responder.getCurrentTaskingId());
        assertFalse(responder.isAvailable());
        responder.setCurrentTaskingId(null);
        assertTrue(responder.isAvailable());
    }

    @Test
    @DisplayName("renaming to a blank name is rejected")
    void shouldRejectBlankRename() {
        Responder responder = new Responder("Alex Tan");
        assertThrows(IllegalArgumentException.class, () -> responder.setName(" "));
    }

    @Test
    @DisplayName("toString uses the custom 'Responder{...}' format")
    void shouldFormatCustomToString() {
        Responder responder = new Responder("Alex Tan");
        assertTrue(responder.toString().startsWith("Responder{"));
        assertTrue(responder.toString().contains("name=Alex Tan"));
        assertFalse(responder.toString().contains("@"));
    }

    @Test
    @DisplayName("two responders with different ids are not equal")
    void shouldNotBeEqualAcrossIds() {
        assertNotEquals(new Responder("Alex Tan"), new Responder("Alex Tan"));
    }
}
