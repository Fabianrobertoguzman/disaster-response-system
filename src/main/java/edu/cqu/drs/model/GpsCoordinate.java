package edu.cqu.drs.model;

import java.io.Serializable;

/**
 * An immutable WGS-84 geographic coordinate (latitude/longitude in decimal degrees).
 *
 * <p>Realises part of Assessment One's FR-02 ("record the GPS coordinates ... of every
 * submitted incident report"). The prototype's {@link #captureCurrentLocation()} returns a
 * fixed Rockhampton coordinate  -  a stub standing in for a real device-GPS read, which is a
 * Stage-3 integration item.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class GpsCoordinate implements Serializable {

    /** Serialisation version (coordinates travel inside incidents over the protocol). */
    private static final long serialVersionUID = 1L;

    /** Minimum valid latitude in decimal degrees. */
    public static final double MIN_LATITUDE = -90.0;

    /** Maximum valid latitude in decimal degrees. */
    public static final double MAX_LATITUDE = 90.0;

    /** Minimum valid longitude in decimal degrees. */
    public static final double MIN_LONGITUDE = -180.0;

    /** Maximum valid longitude in decimal degrees. */
    public static final double MAX_LONGITUDE = 180.0;

    /** Stub latitude returned by {@link #captureCurrentLocation()} (Rockhampton, Qld). */
    private static final double STUB_LATITUDE = -23.3781;

    /** Stub longitude returned by {@link #captureCurrentLocation()} (Rockhampton, Qld). */
    private static final double STUB_LONGITUDE = 150.5136;

    /** Latitude in decimal degrees, in [{@value #MIN_LATITUDE}, {@value #MAX_LATITUDE}]. */
    private final double latitude;

    /** Longitude in decimal degrees, in [{@value #MIN_LONGITUDE}, {@value #MAX_LONGITUDE}]. */
    private final double longitude;

    /**
     * Creates a coordinate, validating the ranges.
     *
     * @param latitude  decimal-degree latitude; must be in [{@value #MIN_LATITUDE}, {@value #MAX_LATITUDE}].
     * @param longitude decimal-degree longitude; must be in [{@value #MIN_LONGITUDE}, {@value #MAX_LONGITUDE}].
     * @throws IllegalArgumentException if either value is outside its valid range.
     */
    public GpsCoordinate(double latitude, double longitude) {
        validateRanges(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Validates that the supplied latitude/longitude lie within WGS-84 bounds.
     * (Extracted as a private helper so the constructor stays small; the same
     * Extract Method pattern is used by the other model classes.)
     *
     * @param latitude  the latitude to check.
     * @param longitude the longitude to check.
     * @throws IllegalArgumentException if either is out of range.
     */
    private static void validateRanges(double latitude, double longitude) {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException("latitude out of range: " + latitude);
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException("longitude out of range: " + longitude);
        }
    }

    /**
     * Returns a stand-in "current location" for the prototype (a fixed Rockhampton coordinate).
     *
     * @return a {@link GpsCoordinate} representing the device's location (stubbed).
     */
    public static GpsCoordinate captureCurrentLocation() {
        // TODO A3: replace with a real device-GPS read via the platform location service.
        return new GpsCoordinate(STUB_LATITUDE, STUB_LONGITUDE);
    }

    /**
     * @return the latitude in decimal degrees.
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * @return the longitude in decimal degrees.
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * Compact diagnostic format used in logs and UI dialogs.
     *
     * @return e.g. {@code "(-23.3781, 150.5136)"}.
     */
    @Override
    public String toString() {
        return "(" + this.latitude + ", " + this.longitude + ")";
    }

    /**
     * Value equality on latitude and longitude.
     *
     * @param other the object to compare with.
     * @return true if {@code other} is a {@code GpsCoordinate} with the same latitude and longitude.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GpsCoordinate)) {
            return false;
        }
        GpsCoordinate that = (GpsCoordinate) other;
        return Double.compare(this.latitude, that.latitude) == 0
                && Double.compare(this.longitude, that.longitude) == 0;
    }

    /**
     * @return a hash code consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.latitude, this.longitude);
    }
}
