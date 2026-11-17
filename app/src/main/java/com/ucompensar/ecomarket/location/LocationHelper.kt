package com.ucompensar.ecomarket.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Geolocalización con los servicios de Google Play:
 * - FusedLocationProviderClient obtiene la ubicación actual del dispositivo.
 * - Geocoder convierte latitud y longitud en una dirección legible (geocodificación inversa).
 */
class LocationHelper(private val context: Context) {

    /** Punto geográfico con la dirección resuelta (si el geocodificador la encontró). */
    data class Place(val latitude: Double, val longitude: Double, val address: String?)

    private val fused = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Pide una lectura de alta precisión; si no llega, usa la última ubicación conocida.
     * Requiere que la pantalla haya solicitado antes el permiso de ubicación.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentPlace(): Place? {
        val location = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            ?: fused.lastLocation.await()
            ?: return null
        return Place(location.latitude, location.longitude, reverseGeocode(location.latitude, location.longitude))
    }

    /** Dirección más cercana a unas coordenadas, en español de Colombia. */
    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, Locale("es", "CO")).getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
        }.getOrNull()
    }
}
