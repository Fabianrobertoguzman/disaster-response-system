package edu.cqu.drs.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A deployable response resource (e.g. an ambulance, fire appliance, tow truck or pump).
 *
 * <p>Referenced by Assessment&nbsp;One's UC-03 ("Allocate Resources"): when a Dispatcher commits an
 * allocation, the chosen resources are marked allocated ({@code Resource.markAllocated()}). A resource
 * starts available; allocating it makes it unavailable until it is released.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class Resource implements Serializable {

    /** Serialisation version (resources travel over the client/server protocol). */
    private static final long serialVersionUID = 1L;

    /** Immutable unique identifier. */
    private final UUID id;

    /** Human-readable resource type (never null or blank), e.g. {@code "Ambulance"}. */
    private String resourceType;

    /** Whether this resource is currently available for allocation. */
    private boolean available;

    /**
     * Creates an available resource of the given type.
     *
     * @param resourceType the resource type (must not be null or blank).
     * @throws IllegalArgumentException if {@code resourceType} is null or blank.
     */
    public Resource(String resourceType) {
        requireNonBlankType(resourceType);
        this.id = UUID.randomUUID();
        this.resourceType = resourceType;
        this.available = true;
    }

    /**
     * Reconstruction constructor used only by the persistence tier to rebuild a
     * resource from a stored database row, preserving the persisted identity and
     * availability flag rather than generating a new id and defaulting to
     * available.
     *
     * @param id           the persisted identifier (must not be null).
     * @param resourceType the resource type (must not be null or blank).
     * @param available    whether the stored resource is available.
     * @throws IllegalArgumentException if {@code id} is null or
     *         {@code resourceType} is null or blank.
     */
    public Resource(UUID id, String resourceType, boolean available) {
        requireNonBlankType(resourceType);
        if (id == null) {
            throw new IllegalArgumentException("id is required to reconstruct a resource");
        }
        this.id = id;
        this.resourceType = resourceType;
        this.available = available;
    }

    /** @throws IllegalArgumentException if {@code resourceType} is null or blank. */
    private static void requireNonBlankType(String resourceType) {
        if (resourceType == null || resourceType.trim().isEmpty()) {
            throw new IllegalArgumentException("resourceType must not be null or blank");
        }
    }

    /** @return the immutable resource identifier. */
    public UUID getId() {
        return this.id;
    }

    /** @return the resource type. */
    public String getResourceType() {
        return this.resourceType;
    }

    /** @param resourceType the new resource type (must not be null or blank). @throws IllegalArgumentException if invalid. */
    public void setResourceType(String resourceType) {
        requireNonBlankType(resourceType);
        this.resourceType = resourceType;
    }

    /** @return true if this resource is currently available for allocation. */
    public boolean isAvailable() {
        return this.available;
    }

    /** Marks this resource as allocated (no longer available). (Assessment&nbsp;One UC-03 {@code Resource.markAllocated()}.) */
    public void markAllocated() {
        this.available = false;
    }

    /** Marks this resource as available again (e.g. after the incident is resolved). */
    public void markAvailable() {
        this.available = true;
    }

    /**
     * Hand-written display string used by the dispatcher console (the default Object.toString would only print Class@hash).
     *
     * @return e.g. {@code "Resource{id=..., type=Ambulance, available=true}"}.
     */
    @Override
    public String toString() {
        return "Resource{id=" + this.id + ", type=" + this.resourceType + ", available=" + this.available + "}";
    }

    /**
     * Identity equality on the immutable {@link #id}.
     *
     * @param other the object to compare with.
     * @return true if {@code other} is a {@code Resource} with the same id.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource)) {
            return false;
        }
        return this.id.equals(((Resource) other).id);
    }

    /** @return a hash code consistent with {@link #equals(Object)} (based on {@link #id}). */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
