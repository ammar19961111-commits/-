package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed interface LocationFetchResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val locationLabel: String
    ) : LocationFetchResult

    data object PermissionMissing : LocationFetchResult
    data object Unavailable : LocationFetchResult
}

interface DeviceLocationProvider {
    fun hasLocationPermission(): Boolean
    suspend fun fetchCurrentLocation(): LocationFetchResult
}

object NoOpDeviceLocationProvider : DeviceLocationProvider {
    override fun hasLocationPermission(): Boolean = false
    override suspend fun fetchCurrentLocation(): LocationFetchResult = LocationFetchResult.PermissionMissing
}

class AndroidDeviceLocationProvider(
    context: Context
) : DeviceLocationProvider {

    private val appContext = context.applicationContext

    override fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    override suspend fun fetchCurrentLocation(): LocationFetchResult {
        if (!hasLocationPermission()) {
            return LocationFetchResult.PermissionMissing
        }
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationFetchResult.Unavailable

        val bestLastKnown = pickBestLastKnownLocation(locationManager)
        val location = bestLastKnown ?: requestFreshLocation(locationManager)
            ?: return LocationFetchResult.Unavailable

        val label = resolveCityLabel(location.latitude, location.longitude)
        return LocationFetchResult.Success(
            latitude = location.latitude,
            longitude = location.longitude,
            locationLabel = label
        )
    }

    @SuppressLint("MissingPermission")
    private fun pickBestLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return providers.mapNotNull { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider) || provider == LocationManager.PASSIVE_PROVIDER) {
                    locationManager.getLastKnownLocation(provider)
                } else null
            }.getOrNull()
        }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(locationManager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { cont ->
                val cancellationSignal = CancellationSignal()
                cont.invokeOnCancellation { cancellationSignal.cancel() }
                runCatching {
                    locationManager.getCurrentLocation(
                        provider,
                        cancellationSignal,
                        appContext.mainExecutor
                    ) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private suspend fun resolveCityLabel(latitude: Double, longitude: Double): String =
        withContext(Dispatchers.IO) {
            val fallback = appContext.getString(R.string.prayer_current_location_fallback)
            if (!Geocoder.isPresent()) return@withContext fallback
            runCatching {
                val geocoder = Geocoder(appContext, Locale.forLanguageTag("ar"))
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    withTimeoutOrNull(3000L) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(latitude, longitude, 1) { list ->
                                if (cont.isActive) cont.resume(list)
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }
                val first = addresses?.firstOrNull()
                val city = first?.locality ?: first?.subAdminArea ?: first?.adminArea
                val country = first?.countryName
                when {
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city، $country"
                    !city.isNullOrBlank() -> city
                    !country.isNullOrBlank() -> country
                    else -> fallback
                }
            }.getOrDefault(fallback)
        }
}
