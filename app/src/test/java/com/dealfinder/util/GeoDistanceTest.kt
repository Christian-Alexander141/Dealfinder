package com.dealfinder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests für die Haversine-Distanzberechnung und deutsche Formatierung.
 */
class GeoDistanceTest {

    @Test
    fun `identische Punkte haben null Distanz`() {
        assertEquals(0.0, GeoDistance.betweenKm(52.52, 13.405, 52.52, 13.405), 0.0001)
    }

    @Test
    fun `Distanz zwischen Berlin Mitte und Berlin Charlottenburg ist plausibel`() {
        // ca. 6–7 km
        val km = GeoDistance.betweenKm(52.5200, 13.4050, 52.5167, 13.3000)
        assertTrue("Distanz war $km km", km in 5.0..9.0)
    }

    @Test
    fun `Distanz ist symmetrisch`() {
        val a = GeoDistance.betweenKm(52.52, 13.405, 48.1372, 11.5756)
        val b = GeoDistance.betweenKm(48.1372, 11.5756, 52.52, 13.405)
        assertEquals(a, b, 0.0001)
    }

    @Test
    fun `Formatierung unter einem Kilometer in Metern`() {
        assertEquals("850 m", GeoDistance.format(0.85))
        assertEquals("0 m", GeoDistance.format(0.0))
    }

    @Test
    fun `Formatierung ab einem Kilometer mit Komma`() {
        assertEquals("3,4 km", GeoDistance.format(3.44))
        assertEquals("1,0 km", GeoDistance.format(1.0))
    }
}
