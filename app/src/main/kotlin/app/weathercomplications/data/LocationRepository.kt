package app.weathercomplications.data

import app.weathercomplications.util.LOG_TAG
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import android.content.pm.ApplicationInfo
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(private val context: Context) {

    @Suppress("MissingPermission")
    suspend fun getLocation(): LatLon {
        Log.d(LOG_TAG, "getLocation: requesting lastLocation")
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        Log.d(LOG_TAG, "getLocation: lastLocation hit ${loc.latitude},${loc.longitude}")
                        cont.resume(LatLon(loc.latitude, loc.longitude))
                    } else {
                        Log.d(LOG_TAG, "getLocation: lastLocation null, requesting current")
                        val cts = CancellationTokenSource()
                        cont.invokeOnCancellation { cts.cancel() }
                        fusedClient.getCurrentLocation(
                            CurrentLocationRequest.Builder()
                                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                .build(),
                            cts.token
                        ).addOnSuccessListener { fresh: Location? ->
                            if (fresh != null) {
                                Log.d(LOG_TAG, "getLocation: currentLocation hit ${fresh.latitude},${fresh.longitude}")
                                cont.resume(LatLon(fresh.latitude, fresh.longitude))
                            } else {
                                Log.d(LOG_TAG, "getLocation: currentLocation null, trying LocationManager fallback")
                                val fallback = locationManagerFallback()
                                if (fallback != null) {
                                    Log.d(LOG_TAG, "getLocation: fallback hit ${fallback.latitude},${fallback.longitude}")
                                    cont.resume(fallback)
                                } else if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                                    Log.w(LOG_TAG, "getLocation: all sources exhausted, using debug hardcoded location")
                                    cont.resume(LatLon(41.8781, -87.6298))
                                } else {
                                    Log.e(LOG_TAG, "getLocation: all sources exhausted")
                                    cont.resumeWithException(Exception("Location unavailable"))
                                }
                            }
                        }.addOnFailureListener {
                            Log.e(LOG_TAG, "getLocation: currentLocation failed", it)
                            cont.resumeWithException(it)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(LOG_TAG, "getLocation: lastLocation failed", it)
                    cont.resumeWithException(it)
                }
        }
    }

    @Suppress("MissingPermission")
    suspend fun getLastKnownLocation(): LatLon? = suspendCancellableCoroutine { cont ->
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { loc ->
                cont.resume(
                    if (loc != null) LatLon(loc.latitude, loc.longitude)
                    else locationManagerFallback()
                )
            }
            .addOnFailureListener { cont.resume(locationManagerFallback()) }
    }

    @Suppress("MissingPermission")
    private fun locationManagerFallback(): LatLon? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { LatLon(it.latitude, it.longitude) }
    }
}

