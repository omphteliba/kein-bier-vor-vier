package de.keinbiervorvier.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.keinbiervorvier.core.BeerSpot
import java.util.Locale

/**
 * Shows [spot] in Google Maps – the app if installed, the website otherwise.
 * Searches for the place by its English name and country, which lands on the
 * city itself (with photos and info) rather than a bare pin on some street.
 */
fun Context.openInMaps(spot: BeerSpot) {
    val query = "${spot.city(Locale.ENGLISH)}, ${spot.country(Locale.ENGLISH)}"
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No browser or maps app at all; nothing sensible to do.
    }
}
