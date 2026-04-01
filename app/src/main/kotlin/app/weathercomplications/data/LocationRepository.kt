package app.weathercomplications.data

import android.content.Context
import android.location.Location
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
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        cont.resume(LatLon(loc.latitude, loc.longitude))
                    } else {
                        val cts = CancellationTokenSource()
                        cont.invokeOnCancellation { cts.cancel() }
                        fusedClient.getCurrentLocation(
                            CurrentLocationRequest.Builder()
                                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                                .build(),
                            cts.token
                        ).addOnSuccessListener { fresh: Location? ->
                            if (fresh != null) cont.resume(LatLon(fresh.latitude, fresh.longitude))
                            else cont.resumeWithException(Exception("Location unavailable"))
                        }.addOnFailureListener { cont.resumeWithException(it) }
                    }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
