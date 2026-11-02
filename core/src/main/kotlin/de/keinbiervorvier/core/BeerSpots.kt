package de.keinbiervorvier.core

import java.time.ZoneId
import java.util.Locale

/** A typical local beer: brand name and style, no logos. */
data class Beer(val name: String, val style: String)

/**
 * A place where the beer hour can start, shown with a human-friendly name.
 * [city] and [shortName] are German; English ones come from [BeerSpots.ENGLISH_NAMES].
 * [countryCode] is ISO 3166-1 alpha-2 and drives the [flag]; [location] is the
 * city centre, roughly. [beer] is null where there is no local beer to speak of.
 * [shortName] fits small watch face slots (at most [SHORT_NAME_MAX] characters).
 */
data class BeerSpot(
    val city: String,
    val zone: ZoneId,
    val countryCode: String,
    val location: GeoPoint,
    val beer: Beer?,
    val shortName: String = city,
) {
    constructor(
        city: String,
        zoneId: String,
        countryCode: String,
        location: GeoPoint,
        beer: Beer?,
        shortName: String = city,
    ) : this(city, ZoneId.of(zoneId), countryCode, location, beer, shortName)

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

/**
 * One well-known city per distinct UTC offset (including the odd half- and
 * quarter-hour ones), ordered from east to west.
 */
object BeerSpots {
    val ALL: List<BeerSpot> = listOf(
        BeerSpot("Kiritimati", "Pacific/Kiritimati", "KI", GeoPoint(1.87, -157.43), null, shortName = "Kiriti"),
        BeerSpot("Chatham-Inseln", "Pacific/Chatham", "NZ", GeoPoint(-43.95, -176.56), null, shortName = "Chatham"),
        BeerSpot("Nukuʻalofa", "Pacific/Tongatapu", "TO", GeoPoint(-21.14, -175.2), Beer("Ikale", "Lager"), shortName = "Tonga"),
        BeerSpot("Auckland", "Pacific/Auckland", "NZ", GeoPoint(-36.85, 174.76), Beer("Steinlager Classic", "Lager"), shortName = "Auckld"),
        BeerSpot("Suva", "Pacific/Fiji", "FJ", GeoPoint(-18.14, 178.44), Beer("Fiji Bitter", "Lager")),
        BeerSpot("Nouméa", "Pacific/Noumea", "NC", GeoPoint(-22.27, 166.46), Beer("Number One", "Lager")),
        BeerSpot("Lord Howe", "Australia/Lord_Howe", "AU", GeoPoint(-31.55, 159.08), null, shortName = "L. Howe"),
        BeerSpot("Sydney", "Australia/Sydney", "AU", GeoPoint(-33.87, 151.21), Beer("Tooheys New", "Lager")),
        BeerSpot("Brisbane", "Australia/Brisbane", "AU", GeoPoint(-27.47, 153.03), Beer("XXXX Gold", "Lager"), shortName = "Brisb."),
        BeerSpot("Adelaide", "Australia/Adelaide", "AU", GeoPoint(-34.93, 138.6), Beer("Coopers Pale Ale", "Pale Ale"), shortName = "Adel."),
        BeerSpot("Darwin", "Australia/Darwin", "AU", GeoPoint(-12.46, 130.84), Beer("NT Draught", "Lager")),
        BeerSpot("Tokio", "Asia/Tokyo", "JP", GeoPoint(35.68, 139.69), Beer("Asahi Super Dry", "Dry Lager")),
        BeerSpot("Eucla", "Australia/Eucla", "AU", GeoPoint(-31.68, 128.88), null),
        BeerSpot("Shanghai", "Asia/Shanghai", "CN", GeoPoint(31.23, 121.47), Beer("Tsingtao", "Lager"), shortName = "Peking"),
        BeerSpot("Bangkok", "Asia/Bangkok", "TH", GeoPoint(13.76, 100.5), Beer("Singha", "Lager")),
        BeerSpot("Yangon", "Asia/Yangon", "MM", GeoPoint(16.87, 96.2), Beer("Myanmar Beer", "Lager")),
        BeerSpot("Dhaka", "Asia/Dhaka", "BD", GeoPoint(23.81, 90.41), null),
        BeerSpot("Kathmandu", "Asia/Kathmandu", "NP", GeoPoint(27.72, 85.32), Beer("Everest", "Lager"), shortName = "Nepal"),
        BeerSpot("Mumbai", "Asia/Kolkata", "IN", GeoPoint(19.08, 72.88), Beer("Kingfisher Premium", "Lager")),
        BeerSpot("Karatschi", "Asia/Karachi", "PK", GeoPoint(24.86, 67.01), Beer("Murree", "Lager"), shortName = "Lahore"),
        BeerSpot("Kabul", "Asia/Kabul", "AF", GeoPoint(34.56, 69.21), null),
        BeerSpot("Dubai", "Asia/Dubai", "AE", GeoPoint(25.2, 55.27), null),
        BeerSpot("Teheran", "Asia/Tehran", "IR", GeoPoint(35.69, 51.39), null),
        BeerSpot("Moskau", "Europe/Moscow", "RU", GeoPoint(55.76, 37.62), Beer("Baltika Nr. 7", "Lager")),
        BeerSpot("Nairobi", "Africa/Nairobi", "KE", GeoPoint(-1.29, 36.82), Beer("Tusker", "Lager")),
        BeerSpot("Athen", "Europe/Athens", "GR", GeoPoint(37.98, 23.73), Beer("Mythos", "Lager")),
        BeerSpot("Kairo", "Africa/Cairo", "EG", GeoPoint(30.04, 31.24), Beer("Sakara Gold", "Lager")),
        BeerSpot("München", "Europe/Berlin", "DE", GeoPoint(48.14, 11.58), Beer("Augustiner Helles", "Helles")),
        BeerSpot("Lagos", "Africa/Lagos", "NG", GeoPoint(6.52, 3.38), Beer("Star Lager", "Lager")),
        BeerSpot("Dublin", "Europe/Dublin", "IE", GeoPoint(53.35, -6.26), Beer("Guinness Draught", "Irish Stout")),
        BeerSpot("Lissabon", "Europe/Lisbon", "PT", GeoPoint(38.72, -9.14), Beer("Super Bock", "Lager"), shortName = "Lisboa"),
        BeerSpot("Reykjavík", "Atlantic/Reykjavik", "IS", GeoPoint(64.15, -21.94), Beer("Einstök White Ale", "Witbier"), shortName = "Island"),
        BeerSpot("Azoren", "Atlantic/Azores", "PT", GeoPoint(37.74, -25.67), Beer("Especial", "Lager")),
        BeerSpot("Praia", "Atlantic/Cape_Verde", "CV", GeoPoint(14.93, -23.51), Beer("Strela", "Lager")),
        BeerSpot("Südgeorgien", "Atlantic/South_Georgia", "GS", GeoPoint(-54.28, -36.51), null, shortName = "Südgeo."),
        BeerSpot("Fernando de Noronha", "America/Noronha", "BR", GeoPoint(-3.85, -32.42), Beer("Brahma Chopp", "Lager"), shortName = "Noronha"),
        BeerSpot("São Paulo", "America/Sao_Paulo", "BR", GeoPoint(-23.55, -46.63), Beer("Antarctica Original", "Pilsner"), shortName = "Rio"),
        BeerSpot("Buenos Aires", "America/Argentina/Buenos_Aires", "AR", GeoPoint(-34.6, -58.38), Beer("Quilmes Clásica", "Lager"), shortName = "B.Aires"),
        BeerSpot("St. John's", "America/St_Johns", "CA", GeoPoint(47.56, -52.71), Beer("Quidi Vidi Iceberg", "Lager"), shortName = "St.John"),
        BeerSpot("Halifax", "America/Halifax", "CA", GeoPoint(44.65, -63.57), Beer("Alexander Keith's", "India Pale Ale")),
        BeerSpot("Caracas", "America/Caracas", "VE", GeoPoint(10.48, -66.9), Beer("Polar Pilsen", "Pilsner")),
        BeerSpot("New York", "America/New_York", "US", GeoPoint(40.71, -74.01), Beer("Brooklyn Lager", "Amber Lager"), shortName = "NYC"),
        BeerSpot("Chicago", "America/Chicago", "US", GeoPoint(41.88, -87.63), Beer("Goose Island IPA", "IPA")),
        BeerSpot("Mexiko-Stadt", "America/Mexico_City", "MX", GeoPoint(19.43, -99.13), Beer("Victoria", "Vienna Lager"), shortName = "Mexiko"),
        BeerSpot("Denver", "America/Denver", "US", GeoPoint(39.74, -104.99), Beer("Coors Banquet", "Lager")),
        BeerSpot("Phoenix", "America/Phoenix", "US", GeoPoint(33.45, -112.07), Beer("Kilt Lifter", "Scottish Ale")),
        BeerSpot("Los Angeles", "America/Los_Angeles", "US", GeoPoint(34.05, -118.24), Beer("Mango Cart", "Wheat Ale"), shortName = "L.A."),
        BeerSpot("Anchorage", "America/Anchorage", "US", GeoPoint(61.22, -149.9), Beer("Alaskan Amber", "Altbier"), shortName = "Alaska"),
        BeerSpot("Gambierinseln", "Pacific/Gambier", "PF", GeoPoint(-23.12, -134.97), Beer("Hinano", "Lager"), shortName = "Gambier"),
        BeerSpot("Marquesas", "Pacific/Marquesas", "PF", GeoPoint(-8.91, -140.1), Beer("Hinano", "Lager"), shortName = "Marq."),
        BeerSpot("Honolulu", "Pacific/Honolulu", "US", GeoPoint(21.31, -157.86), Beer("Kona Big Wave", "Golden Ale"), shortName = "Hawaii"),
        BeerSpot("Pago Pago", "Pacific/Pago_Pago", "AS", GeoPoint(-14.28, -170.7), null, shortName = "Samoa"),
    )

    /** Keyed by zone id. */
    val ENGLISH_NAMES: Map<String, EnglishName> = mapOf(
        "Pacific/Chatham" to EnglishName("Chatham Islands", "Chatham"),
        "Asia/Tokyo" to EnglishName("Tokyo"),
        "Asia/Shanghai" to EnglishName("Shanghai", "Beijing"),
        "Asia/Karachi" to EnglishName("Karachi", "Lahore"),
        "Europe/Moscow" to EnglishName("Moscow"),
        "Europe/Athens" to EnglishName("Athens"),
        "Africa/Cairo" to EnglishName("Cairo"),
        "Europe/Berlin" to EnglishName("Munich"),
        "Europe/Lisbon" to EnglishName("Lisbon"),
        "Atlantic/Reykjavik" to EnglishName("Reykjavík", "Iceland"),
        "Atlantic/Azores" to EnglishName("Azores"),
        "Atlantic/South_Georgia" to EnglishName("South Georgia", "S.Georg"),
        "America/Mexico_City" to EnglishName("Mexico City", "Mexico"),
        "Pacific/Gambier" to EnglishName("Gambier Islands", "Gambier"),
    )
}
