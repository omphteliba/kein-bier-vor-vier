package de.keinbiervorvier.core

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A position on Earth in decimal degrees. */
data class GeoPoint(val lat: Double, val lon: Double) {

    /** Great-circle distance (haversine), good to well under 1 % for our purposes. */
    fun distanceKmTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0088
    }
}

/** Below this distance you are basically there. */
const val THERE_KM = 50

/**
 * "7.600 km" (or "7,600 km" in English) style text: rounded to 100 km above
 * 1000 km and to 10 km below, since city coordinates are only approximate
 * anyway. Null when you're already there.
 */
fun formatDistance(km: Double, locale: Locale = Locale.GERMANY): String? {
    val rounded = when {
        km < THERE_KM -> return null
        km < 1000 -> (km / 10).roundToInt() * 10
        else -> (km / 100).roundToInt() * 100
    }
    return String.format(locale, "%,d km", rounded)
}
