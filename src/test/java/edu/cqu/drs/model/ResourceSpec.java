package edu.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Resource} (a deployable response resource - Assessment One UC-03).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Resource - deployable response resource")
class ResourceSpec {

    @Test
    @DisplayName("a new resource is available, has the given type, and has a non-null id")
    void shouldStartAvailable() {
        Resource resource = new Resource("Ambulance");
        assertNotNull(resource.getId());
        assertEquals("Ambulance", resource.getResourceType());
        assertTrue(resource.isAvailable());
    }

    @Test
    @DisplayName("a null or blank resource type is rejected")
    void shouldRejectBlankType() {
        assertThrows(IllegalArgumentException.class, () -> new Resource(null));
        assertThrows(IllegalArgumentException.class, () -> new Resource("   "));
    }

    @Test
    @DisplayName("markAllocated makes the resource unavailable; markAvailable makes it available again")
    void allocationTogglesAvailability() {
        Resource resource = new Resource("Fire Appliance");
        resource.markAllocated();
        assertFalse(resource.isAvailable());
        resource.markAvailable();
        assertTrue(resource.isAvailable());
    }

    @Test
    @DisplayName("renaming to a blank type is rejected")
    void shouldRejectBlankRename() {
        Resource resource = new Resource("Tow Truck");
        assertThrows(IllegalArgumentException.class, () -> resource.setResourceType(""));
    }

    @Test
    @DisplayName("toString uses the custom 'Resource{...}' format")
    void shouldFormatCustomToString() {
        Resource resource = new Resource("Ambulance");
        assertTrue(resource.toString().startsWith("Resource{"));
        assertTrue(resource.toString().contains("type=Ambulance"));
        assertTrue(resource.toString().contains("available=true"));
        assertFalse(resource.toString().contains("@"));
    }

    @Test
    @DisplayName("two resources with different ids are not equal")
    void shouldNotBeEqualAcrossIds() {
        assertNotEquals(new Resource("Ambulance"), new Resource("Ambulance"));
    }
}
