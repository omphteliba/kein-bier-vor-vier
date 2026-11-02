package de.keinbiervorvier.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.keinbiervorvier.core.BeerSpot
import de.keinbiervorvier.core.Brewery
import java.time.LocalDate
import java.util.Locale

/** Shows the place of [spot] on [date] in Google Maps, see the other overload. */
fun Context.openInMaps(spot: BeerSpot, date: LocalDate) = openInMaps(spot, spot.breweryOn(date))

/**
 * Shows [brewery] in Google Maps - the app if installed, the website otherwise
 * - or the city when there is no brewery. English names find the most.
 */
fun Context.openInMaps(spot: BeerSpot, brewery: Brewery?) =
    mapsSearch(listOfNotNull(brewery?.name, spot.placeOf(brewery, Locale.ENGLISH), spot.country(Locale.ENGLISH)))

/** Google Maps' own list of breweries around [place] - it knows far more than we do. */
fun Context.openBreweriesNearby(spot: BeerSpot, place: String) =
    mapsSearch(listOf("brewery", place, spot.country(Locale.ENGLISH)))

/** The brewery on Untappd, which keeps track of its current beers. Linking only, no API. */
fun Context.openOnUntappd(brewery: Brewery) =
    open(Uri.parse("https://untappd.com/search?type=brewery&q=" + Uri.encode(brewery.name)))

/** Opens [url] (a brewery's website) in the browser. */
fun Context.openWebsite(url: String) = open(Uri.parse(url))

private fun Context.mapsSearch(terms: List<String>) =
    open(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(terms.joinToString(", "))))

private fun Context.open(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No browser or maps app at all; nothing sensible to do.
    }
}
