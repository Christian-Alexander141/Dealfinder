package com.dealfinder.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Haversine-Distanz zwischen zwei Koordinaten (reine Logik, unit-testbar).
 * Ohne externe Geocoding-API bzw. API-Schlüssel nutzbar.
 */
object GeoDistance {

    private const val EARTH_RADIUS_KM = 6371.0

    /** Distanz in Kilometern zwischen zwei WGS84-Punkten. */
    fun betweenKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Deutsche Darstellung: unter 1 km in Metern, sonst mit Komma-Kilometern.
     * Beispiel: "850 m", "3,4 km"
     */
    fun format(km: Double): String {
        return if (km < 1.0) {
            "${(km * 1000).toInt()} m"
        } else {
            String.format(java.util.Locale.GERMAN, "%.1f km", km)
        }
    }
}
