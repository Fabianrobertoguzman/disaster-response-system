package com.coit20258.drs.model;

import java.time.LocalDateTime;

public class User {

    public static final String ROLE_REPORTER = "REPORTER";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_ADMIN = "ADMIN";

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private String role;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public User() {
    }

    public User(
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            String role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(
            int id,
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            String role,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime lastLoginAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    public int getId() {
        return this.id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public String getRole() {
        return this.role;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return this.lastLoginAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public String toString() {
        return String.format(
                "[%d] %s | %s | %s | active=%b",
                this.id,
                this.getFullName(),
                this.email,
                this.role,
                this.isActive);
    }
}
