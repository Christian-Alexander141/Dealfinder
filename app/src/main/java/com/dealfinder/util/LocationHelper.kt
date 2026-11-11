package com.dealfinder.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Ermittelt den aktuellen Gerätestandort (nur bei erteilter Berechtigung).
 *
 * - Keine Hintergrundortung, keine permanente Ortung.
 * - Ohne Berechtigung wird null zurückgegeben – die App bleibt voll nutzbar
 *   (manuelle Ortseingabe als Alternative).
 * - Kein API-Schlüssel erforderlich (Google Play Services Standort-API).
 */
object LocationHelper {

    /** Liefert den aktuellen Standort oder null, falls keiner verfügbar ist. */
    suspend fun getCurrentLocation(context: Context): Location? {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { continuation ->
            try {
                client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }.addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
