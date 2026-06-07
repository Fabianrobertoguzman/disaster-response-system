package edu.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link User} (a DRS user account - FR-12 data side).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("User - DRS user account")
class UserSpec {

    @Test
    @DisplayName("a valid user has the given username and role and a non-null id; a non-admin reports isAdministrator() == false")
    void shouldCreateUserWithRole() {
        User user = new User("meiru", UserRole.DISPATCHER);
        assertNotNull(user.getId());
        assertEquals("meiru", user.getUsername());
        assertEquals(UserRole.DISPATCHER, user.getRole());
        assertFalse(user.isAdministrator());
    }

    @Test
    @DisplayName("an administrator user reports isAdministrator() == true")
    void administratorFlag() {
        assertTrue(new User("admin", UserRole.ADMINISTRATOR).isAdministrator());
    }

    @Test
    @DisplayName("a null or blank username is rejected")
    void shouldRejectBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> new User(null, UserRole.CITIZEN));
        assertThrows(IllegalArgumentException.class, () -> new User("   ", UserRole.CITIZEN));
    }

    @Test
    @DisplayName("a null role is rejected")
    void shouldRejectNullRole() {
        assertThrows(IllegalArgumentException.class, () -> new User("meiru", null));
    }

    @Test
    @DisplayName("reassigning to a different role updates the role")
    void shouldAllowRoleReassignment() {
        User user = new User("meiru", UserRole.CITIZEN);
        user.setRole(UserRole.DISPATCHER);
        assertEquals(UserRole.DISPATCHER, user.getRole());
    }

    @Test
    @DisplayName("toString uses the custom 'User{...}' format")
    void shouldFormatCustomToString() {
        User user = new User("meiru", UserRole.DISPATCHER);
        assertTrue(user.toString().startsWith("User{"));
        assertTrue(user.toString().contains("username=meiru"));
        assertTrue(user.toString().contains("role=DISPATCHER"));
        assertFalse(user.toString().contains("@"));
    }
}
