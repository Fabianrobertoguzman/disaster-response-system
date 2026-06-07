package edu.cqu.drs.data;

import edu.cqu.drs.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for {@link User} persistence and credential lookup.
 *
 * <p>Credential material (hash + salt) is supplied to {@link #insert(User,
 * String, String)} and returned only by {@link #findByUsername(String)} as a
 * {@link StoredUser}; the plain {@link #findByUuid(UUID)} / {@link #findAll()}
 * reads never expose it.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public interface UserDao {

    /**
     * Inserts a new user with its credential material.
     *
     * @param user         the user account (must not be null).
     * @param passwordHash the password hash to store (must not be null).
     * @param salt         the salt to store (must not be null).
     * @throws DataAccessException if the insert fails (e.g. duplicate username).
     */
    void insert(User user, String passwordHash, String salt);

    /**
     * Finds a user and its stored credential by login name.
     *
     * @param username the login name (must not be null).
     * @return the stored user if present, otherwise {@link Optional#empty()}.
     * @throws DataAccessException if the query fails.
     */
    Optional<StoredUser> findByUsername(String username);

    /**
     * Finds a user account by its domain id (no credential material).
     *
     * @param id the user id (must not be null).
     * @return the user if present, otherwise {@link Optional#empty()}.
     * @throws DataAccessException if the query fails.
     */
    Optional<User> findByUuid(UUID id);

    /**
     * Returns every user account (no credential material).
     *
     * @return the list of users (never null; possibly empty).
     * @throws DataAccessException if the query fails.
     */
    List<User> findAll();
}
