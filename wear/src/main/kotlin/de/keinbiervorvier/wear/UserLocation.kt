package de.keinbiervorvier.wear

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import de.keinbiervorvier.core.GeoPoint
import kotlinx.coroutines.delay

private const val PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
private const val REFRESH_MS = 15 * 60_000L

/**
 * The watch's approximate position, or null while unknown or if the user
 * declined. Asks for the permission once on first use.
 */
@Composable
fun rememberUserLocation(): GeoPoint? {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(PERMISSION)
    }
    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        while (true) {
            context.fetchLocation { location = it }
            delay(REFRESH_MS)
        }
    }
    return location
}

private fun Context.hasLocationPermission() =
    ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED

/**
 * Asks the platform for a fresh fix, falling back to the newest last-known one.
 * Uses LocationManager rather than Play Services: on a watch without a paired
 * phone the latter may never turn on GPS. Coarse permission still works with
 * every provider (Android 12+), the result is just blurred to city level.
 *
 * The cheap fused provider goes first. With coarse permission it runs in low
 * power mode and never starts GPS, so a standalone watch gets nothing from it;
 * GPS is the fallback then.
 */
@SuppressLint("MissingPermission") // only called once the permission is granted
private fun Context.fetchLocation(onResult: (GeoPoint) -> Unit) {
    val manager = getSystemService(LocationManager::class.java) ?: return
    val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { it in manager.allProviders && manager.isProviderEnabled(it) }

    fun tryProvider(index: Int) {
        if (index == providers.size) {
            manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
                ?.let { onResult(GeoPoint(it.latitude, it.longitude)) }
            return
        }
        manager.getCurrentLocation(providers[index], null, mainExecutor) { fix ->
            if (fix != null) onResult(GeoPoint(fix.latitude, fix.longitude)) else tryProvider(index + 1)
        }
    }
    tryProvider(0)
}
