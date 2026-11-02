package de.keinbiervorvier.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * A brewery, big or small, by name only (no logos). [town] and [location] say
 * where it is when that isn't the spot's own city; [website] where known.
 * No beers or styles: those change too often to keep up with. Sources for
 * every entry: docs/beer-sources.md.
 */
data class Brewery(
    val name: String,
    val town: String? = null,
    val location: GeoPoint? = null,
    val website: String? = null,
)

/**
 * A place where the beer hour can start, shown with a human-friendly name.
 * [city] and [shortName] are German; English ones come from [BeerSpots.ENGLISH_NAMES].
 * [countryCode] is ISO 3166-1 alpha-2 and drives the [flag]; [location] is the
 * city centre, roughly. [breweries] are a small selection, no claim to
 * completeness; they take turns day by day, empty where there's no local beer. [shortName] fits small watch face slots (at most
 * [SHORT_NAME_MAX] characters).
 */
data class BeerSpot(
    val city: String,
    val zone: ZoneId,
    val countryCode: String,
    val location: GeoPoint,
    val breweries: List<Brewery>,
    val shortName: String = city,
) {
    constructor(
        city: String,
        zoneId: String,
        countryCode: String,
        location: GeoPoint,
        breweries: List<Brewery>,
        shortName: String = city,
    ) : this(city, ZoneId.of(zoneId), countryCode, location, breweries, shortName)

    /** The brewery of the day: the list takes turns, one per local day. */
    fun breweryOn(date: LocalDate): Brewery? =
        if (breweries.isEmpty()) null else breweries[Math.floorMod(date.toEpochDay(), breweries.size.toLong()).toInt()]

    /** All breweries here, starting with the one of the day. */
    fun breweriesFrom(date: LocalDate): List<Brewery> {
        val first = breweries.indexOf(breweryOn(date) ?: return emptyList())
        return breweries.indices.map { breweries[(first + it) % breweries.size] }
    }

    /**
     * Where to say it's beer o'clock on [date]: the town of the day's brewery if
     * it has one (same time zone, so just as true), otherwise the city.
     */
    fun place(locale: Locale, date: LocalDate): String = placeOf(breweryOn(date), locale)

    /** The town of [brewery] in [locale], or this spot's city. */
    fun placeOf(brewery: Brewery?, locale: Locale): String =
        brewery?.town?.let { if (locale.isGerman()) it else BeerSpots.ENGLISH_TOWNS[it] ?: it } ?: city(locale)

    /** Where [place] is, for distances. */
    fun locationOn(date: LocalDate): GeoPoint = breweryOn(date)?.location ?: location

    /** The local date here at [instant], which picks the beer of the day. */
    fun dateAt(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    /** The city's name in German, or in English for any other language. */
    fun city(locale: Locale): String =
        if (locale.isGerman()) city else BeerSpots.ENGLISH_NAMES[zone.id]?.city ?: city

    /** Like [city], but short enough for a small complication. */
    fun shortName(locale: Locale): String =
        if (locale.isGerman()) shortName else BeerSpots.ENGLISH_NAMES[zone.id]?.shortName ?: shortName

    /** The country's name in German, or in English for any other language. */
    fun country(locale: Locale): String =
        Locale.Builder().setRegion(countryCode).build()
            .getDisplayCountry(if (locale.isGerman()) Locale.GERMAN else Locale.ENGLISH)

    /** The country's flag emoji, built from regional indicator symbols. */
    val flag: String
        get() = countryCode.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")
}

private fun Locale.isGerman() = language == "de"

/** English names where they differ from the German ones. */
data class EnglishName(val city: String, val shortName: String = city)

/** Short text complications only have room for about seven characters. */
const val SHORT_NAME_MAX = 7
