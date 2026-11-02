package de.keinbiervorvier.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.keinbiervorvier.core.BeerSpot
import java.time.LocalDate
import java.util.Locale

/**
 * Shows the place of [spot] on [date] in Google Maps - the app if installed,
 * the website otherwise. With a beer of the day it searches for its brewery
 * (handy for a visit), else for the city. English names find the most.
 */
fun Context.openInMaps(spot: BeerSpot, date: LocalDate) {
    val beer = spot.beerOn(date)
    val query = listOfNotNull(beer?.brewery, spot.place(Locale.ENGLISH, date), spot.country(Locale.ENGLISH))
        .joinToString(", ")
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No browser or maps app at all; nothing sensible to do.
    }
}
