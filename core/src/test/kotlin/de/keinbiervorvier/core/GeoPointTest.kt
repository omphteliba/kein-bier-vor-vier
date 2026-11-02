package de.keinbiervorvier.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeoPointTest {

    @Test
    fun `quarter of the equator`() {
        assertEquals(10_007.5, GeoPoint(0.0, 0.0).distanceKmTo(GeoPoint(0.0, 90.0)), 1.0)
    }

    @Test
    fun `munich to new york`() {
        val munich = GeoPoint(48.14, 11.58)
        val newYork = GeoPoint(40.71, -74.01)
        assertEquals(6_490.0, munich.distanceKmTo(newYork), 30.0)
        assertEquals(munich.distanceKmTo(newYork), newYork.distanceKmTo(munich), 1e-9)
    }

    @Test
    fun `distance across the date line takes the short way`() {
        assertTrue(GeoPoint(0.0, 179.0).distanceKmTo(GeoPoint(0.0, -179.0)) < 300)
    }

    @Test
    fun `every spot has plausible coordinates`() {
        BeerSpots.ALL.forEach {
            assertTrue(it.location.lat in -90.0..90.0 && it.location.lon in -180.0..180.0, it.city)
        }
    }

    @Test
    fun `distance formatting`() {
        assertNull(formatDistance(12.0))
        assertEquals("480 km", formatDistance(483.0))
        assertEquals("7.600 km", formatDistance(7_563.0))
    }
}
