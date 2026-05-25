package com.coit20258.drs.util;

import com.coit20258.drs.model.User;

public final class SessionContext {

    private static User currentUser;

    private SessionContext() {
    }

    // =========================================================================
    // Mutators
    // =========================================================================
    /**
     * Stores the authenticated user for the duration of the session.
     *
     * @param user the logged-in user; must not be null
     * @throws IllegalArgumentException if {@code user} is null
     */
    public static void setCurrentUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot store a null user in the session.");
        }
        currentUser = user;
    }

    /**
     * Clears the session (logout). After this call {@link #isLoggedIn()}
     * returns {@code false}.
     */
    public static void clearSession() {
        currentUser = null;
    }

    // =========================================================================
    // Accessors
    // =========================================================================
    /**
     * Returns the currently authenticated user.
     *
     * @return the current {@link User}
     * @throws IllegalStateException if no user is logged in
     */
    public static User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException(
                    "No user is logged in. Check isLoggedIn() before calling getCurrentUser().");
        }
        return currentUser;
    }

    /**
     * @return {@code true} if a user is authenticated in this session
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Convenience role check used by controllers to gate access to views.
     *
     * @param role the required role
     * @return {@code true} if the current user holds that role
     */
    public static boolean currentUserHasRole(String role) {
        return isLoggedIn() && currentUser.getRole() == role;
    }
}
