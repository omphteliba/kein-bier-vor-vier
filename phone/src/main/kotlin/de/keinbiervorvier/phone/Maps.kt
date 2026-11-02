package de.keinbiervorvier.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.keinbiervorvier.core.Beer
import de.keinbiervorvier.core.BeerSpot
import de.keinbiervorvier.core.BeerSpots
import java.time.LocalDate
import java.util.Locale

/** Shows the place of [spot] on [date] in Google Maps, see the other overload. */
fun Context.openInMaps(spot: BeerSpot, date: LocalDate) = openInMaps(spot, spot.beerOn(date))

/**
 * Shows where [beer] is brewed in Google Maps - the app if installed, the
 * website otherwise. It searches for the brewery (handy for a visit), or for
 * the city when there is no beer. English names find the most.
 */
fun Context.openInMaps(spot: BeerSpot, beer: Beer?) {
    val town = beer?.town?.let { BeerSpots.ENGLISH_TOWNS[it] ?: it } ?: spot.city(Locale.ENGLISH)
    val query = listOfNotNull(beer?.brewery, town, spot.country(Locale.ENGLISH)).joinToString(", ")
    open(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)))
}

/** Opens [url] (a brewery's website) in the browser. */
fun Context.openWebsite(url: String) = open(Uri.parse(url))

private fun Context.open(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No browser or maps app at all; nothing sensible to do.
    }
}
