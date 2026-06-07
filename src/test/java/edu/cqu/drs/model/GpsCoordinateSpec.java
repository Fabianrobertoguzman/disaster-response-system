package edu.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link GpsCoordinate} (the immutable WGS-84 coordinate value object).
 *
 * <p>Covers the normal path (a valid coordinate, the {@code captureCurrentLocation} stub) and the
 * abnormal/boundary cases (latitude and longitude outside their valid ranges), plus value equality
 * and the custom {@code toString} format.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("GpsCoordinate - immutable WGS-84 coordinate")
class GpsCoordinateSpec {

    @Test
    @DisplayName("a valid coordinate exposes its latitude and longitude")
    void shouldExposeLatitudeAndLongitude() {
        GpsCoordinate coordinate = new GpsCoordinate(-23.3781, 150.5136);
        assertEquals(-23.3781, coordinate.getLatitude());
        assertEquals(150.5136, coordinate.getLongitude());
    }

    @Test
    @DisplayName("captureCurrentLocation returns a non-null coordinate within the valid ranges")
    void captureCurrentLocationIsValid() {
        GpsCoordinate coordinate = GpsCoordinate.captureCurrentLocation();
        assertNotNull(coordinate);
        assertTrue(coordinate.getLatitude() >= GpsCoordinate.MIN_LATITUDE
                && coordinate.getLatitude() <= GpsCoordinate.MAX_LATITUDE);
        assertTrue(coordinate.getLongitude() >= GpsCoordinate.MIN_LONGITUDE
                && coordinate.getLongitude() <= GpsCoordinate.MAX_LONGITUDE);
    }

    @Test
    @DisplayName("a latitude outside [-90, 90] is rejected")
    void shouldRejectLatitudeOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new GpsCoordinate(91.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new GpsCoordinate(-91.0, 0.0));
    }

    @Test
    @DisplayName("a longitude outside [-180, 180] is rejected")
    void shouldRejectLongitudeOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new GpsCoordinate(0.0, 181.0));
        assertThrows(IllegalArgumentException.class, () -> new GpsCoordinate(0.0, -181.0));
    }

    @Test
    @DisplayName("coordinates with the same latitude/longitude are equal; toString uses the '(lat, long)' format")
    void equalityAndCustomToString() {
        GpsCoordinate a = new GpsCoordinate(-23.0, 150.0);
        GpsCoordinate b = new GpsCoordinate(-23.0, 150.0);
        GpsCoordinate other = new GpsCoordinate(0.0, 0.0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(!a.equals(other));
        assertEquals("(-23.0, 150.0)", a.toString());
    }
}
