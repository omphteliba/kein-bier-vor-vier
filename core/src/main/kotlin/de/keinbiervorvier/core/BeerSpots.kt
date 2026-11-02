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

/**
 * One well-known city per distinct UTC offset (including the odd half- and
 * quarter-hour ones), ordered from east to west. Central Europe has three
 * spots (Germany, France, Belgium) that take turns, like their breweries.
 */
object BeerSpots {
    val ALL: List<BeerSpot> = listOf(
        BeerSpot("Kiritimati", "Pacific/Kiritimati", "KI", GeoPoint(1.87, -157.43), emptyList(), shortName = "Kiriti"),
        BeerSpot("Chatham-Inseln", "Pacific/Chatham", "NZ", GeoPoint(-43.95, -176.56), emptyList(), shortName = "Chatham"),
        BeerSpot(
            "Nukuʻalofa", "Pacific/Tongatapu", "TO", GeoPoint(-21.14, -175.2),
            listOf(Brewery("Tonga Breweries")),
            shortName = "Tonga",
        ),
        BeerSpot(
            "Auckland", "Pacific/Auckland", "NZ", GeoPoint(-36.85, 174.76),
            listOf(
                Brewery("Garage Project", "Wellington", GeoPoint(-41.29, 174.77), website = "https://www.garageproject.co.nz"),
                Brewery("Lion"),
            ),
            shortName = "Auckld",
        ),
        BeerSpot("Suva", "Pacific/Fiji", "FJ", GeoPoint(-18.14, 178.44), listOf(Brewery("Carlton Brewery Fiji"))),
        BeerSpot("Nouméa", "Pacific/Noumea", "NC", GeoPoint(-22.27, 166.46), listOf(Brewery("Grande Brasserie de Nouvelle-Calédonie"))),
        BeerSpot("Lord Howe", "Australia/Lord_Howe", "AU", GeoPoint(-31.55, 159.08), emptyList(), shortName = "L. Howe"),
        BeerSpot(
            "Sydney", "Australia/Sydney", "AU", GeoPoint(-33.87, 151.21),
            listOf(
                Brewery("Young Henrys", "Newtown", GeoPoint(-33.90, 151.18), website = "https://www.younghenrys.com"),
                Brewery("Tooheys"),
            ),
        ),
        BeerSpot("Brisbane", "Australia/Brisbane", "AU", GeoPoint(-27.47, 153.03), listOf(Brewery("Castlemaine Perkins")), shortName = "Brisb."),
        BeerSpot("Adelaide", "Australia/Adelaide", "AU", GeoPoint(-34.93, 138.6), listOf(Brewery("Coopers")), shortName = "Adel."),
        BeerSpot("Darwin", "Australia/Darwin", "AU", GeoPoint(-12.46, 130.84), listOf(Brewery("One Mile Brewery", "Winnellie", GeoPoint(-12.43, 130.88), website = "https://www.onemilebrewery.com.au"))),
        BeerSpot(
            "Tokio", "Asia/Tokyo", "JP", GeoPoint(35.68, 139.69),
            listOf(
                Brewery("Kiuchi Brewery", "Naka", GeoPoint(36.46, 140.49)),
                Brewery("Asahi"),
            ),
        ),
        BeerSpot("Eucla", "Australia/Eucla", "AU", GeoPoint(-31.68, 128.88), emptyList()),
        BeerSpot("Shanghai", "Asia/Shanghai", "CN", GeoPoint(31.23, 121.47), listOf(Brewery("Tsingtao Brewery")), shortName = "Peking"),
        BeerSpot(
            "Bangkok", "Asia/Bangkok", "TH", GeoPoint(13.76, 100.5),
            listOf(
                Brewery("Full Moon Brewworks", "Phuket", GeoPoint(7.88, 98.39)),
                Brewery("Boon Rawd"),
            ),
        ),
        BeerSpot("Yangon", "Asia/Yangon", "MM", GeoPoint(16.87, 96.2), listOf(Brewery("Myanmar Brewery"))),
        BeerSpot("Dhaka", "Asia/Dhaka", "BD", GeoPoint(23.81, 90.41), emptyList()),
        BeerSpot(
            "Kathmandu", "Asia/Kathmandu", "NP", GeoPoint(27.72, 85.32),
            listOf(
                Brewery("Sherpa Brewery", "Chitwan", GeoPoint(27.68, 84.43), website = "https://sherpabrewery.com.np"),
                Brewery("Mount Everest Brewery"),
            ),
            shortName = "Nepal",
        ),
        BeerSpot(
            "Mumbai", "Asia/Kolkata", "IN", GeoPoint(19.08, 72.88),
            listOf(
                Brewery("Gateway Brewing Co."),
                Brewery("United Breweries"),
            ),
        ),
        BeerSpot("Karatschi", "Asia/Karachi", "PK", GeoPoint(24.86, 67.01), listOf(Brewery("Murree Brewery", "Rawalpindi", GeoPoint(33.60, 73.05))), shortName = "Lahore"),
        BeerSpot("Kabul", "Asia/Kabul", "AF", GeoPoint(34.56, 69.21), emptyList()),
        BeerSpot("Dubai", "Asia/Dubai", "AE", GeoPoint(25.2, 55.27), emptyList()),
        BeerSpot("Teheran", "Asia/Tehran", "IR", GeoPoint(35.69, 51.39), emptyList()),
        BeerSpot("Moskau", "Europe/Moscow", "RU", GeoPoint(55.76, 37.62), listOf(Brewery("Baltika"))),
        BeerSpot("Nairobi", "Africa/Nairobi", "KE", GeoPoint(-1.29, 36.82), listOf(Brewery("East African Breweries"))),
        BeerSpot(
            "Athen", "Europe/Athens", "GR", GeoPoint(37.98, 23.73),
            listOf(
                Brewery("Noctua"),
                Brewery("Mythos Brewery"),
            ),
        ),
        BeerSpot("Kairo", "Africa/Cairo", "EG", GeoPoint(30.04, 31.24), listOf(Brewery("Al Ahram Beverages"))),
        BeerSpot(
            "München", "Europe/Berlin", "DE", GeoPoint(48.14, 11.58),
            listOf(
                Brewery("Kirner Privatbrauerei", "Kirn", GeoPoint(49.79, 7.46), website = "https://www.kirner-bier.de"),
                Brewery("Kuehn Kunz Rosen", "Mainz", GeoPoint(49.99, 8.27), website = "https://kuehnkunzrosen.de"),
                Brewery("Sudden Death Brewing", "Lübeck", GeoPoint(53.87, 10.69), website = "https://suddendeathbrewing.de"),
                Brewery("Orca Brau", "Nürnberg", GeoPoint(49.45, 11.04)),
                Brewery("Privatbrauerei Sander", "Worms", GeoPoint(49.63, 8.36), website = "https://brauerei-sander.de"),
                Brewery("Wormser Hagenbräu", "Worms", GeoPoint(49.63, 8.37), website = "https://www.hagenbraeu.de"),
                Brewery("Schwarze Rose", "Mainz", GeoPoint(50.00, 8.26), website = "https://sr-cb.de"),
                Brewery("Fuerst Wiacek", "Berlin", GeoPoint(52.54, 13.27), website = "https://fuerstwiacek.com"),
                Brewery("Yankee & Kraut", "Ingolstadt", GeoPoint(48.77, 11.43), website = "https://www.yankeeundkraut.de"),
                Brewery("Brauerei Lemke", "Berlin", GeoPoint(52.52, 13.40), website = "https://www.lemke.berlin"),
                Brewery("Still Brewing Society", "Mannheim", GeoPoint(49.49, 8.47)),
                Brewery("Giesinger Bräu", "München", GeoPoint(48.11, 11.58)),
                Brewery("Augustiner-Bräu", "München", GeoPoint(48.14, 11.55), website = "https://www.augustiner-braeu.de"),
            ),
        ),
        BeerSpot(
            "Paris", "Europe/Paris", "FR", GeoPoint(48.86, 2.35),
            listOf(
                Brewery("Paname Brewing Company", "Paris", GeoPoint(48.89, 2.38), website = "https://www.panamebrewingcompany.com"),
                Brewery("Brasserie Sainte Cru", "Colmar", GeoPoint(48.10, 7.36)),
                Brewery("Les Intenables", "Rosheim", GeoPoint(48.50, 7.47)),
                Brewery("Brasserie Galibot", "Forbach", GeoPoint(49.19, 6.90), website = "https://brasserie-galibot.com"),
                Brewery("Mogwaï Beer Company", "Mundolsheim", GeoPoint(48.64, 7.71), website = "https://www.mogwaibeercompany.fr"),
            ),
        ),
        BeerSpot(
            "Brüssel", "Europe/Brussels", "BE", GeoPoint(50.85, 4.35),
            listOf(Brewery("Brasserie Cantillon", website = "https://www.cantillon.be")),
        ),
        BeerSpot("Lagos", "Africa/Lagos", "NG", GeoPoint(6.52, 3.38), listOf(Brewery("Nigerian Breweries"))),
        BeerSpot(
            "Dublin", "Europe/Dublin", "IE", GeoPoint(53.35, -6.26),
            listOf(
                Brewery("Whiplash", "Ballyfermot", GeoPoint(53.34, -6.36)),
                Brewery("Guinness"),
            ),
        ),
        BeerSpot(
            "Lissabon", "Europe/Lisbon", "PT", GeoPoint(38.72, -9.14),
            listOf(
                Brewery("Dois Corvos", website = "https://www.doiscorvos.pt"),
                Brewery("Super Bock Group", "Leça do Balio", GeoPoint(41.21, -8.63)),
            ),
            shortName = "Lisboa",
        ),
        BeerSpot("Reykjavík", "Atlantic/Reykjavik", "IS", GeoPoint(64.15, -21.94), listOf(Brewery("Einstök Ölgerð", "Akureyri", GeoPoint(65.68, -18.09), website = "https://einstokbeer.com")), shortName = "Island"),
        BeerSpot("Azoren", "Atlantic/Azores", "PT", GeoPoint(37.74, -25.67), listOf(Brewery("Melo Abreu", "Ponta Delgada", GeoPoint(37.74, -25.67), website = "https://www.meloabreu.pt"))),
        BeerSpot("Praia", "Atlantic/Cape_Verde", "CV", GeoPoint(14.93, -23.51), listOf(Brewery("Sociedade Cabo-verdiana de Cerveja"))),
        BeerSpot("Südgeorgien", "Atlantic/South_Georgia", "GS", GeoPoint(-54.28, -36.51), emptyList(), shortName = "Südgeo."),
        BeerSpot("Fernando de Noronha", "America/Noronha", "BR", GeoPoint(-3.85, -32.42), listOf(Brewery("Ambev")), shortName = "Noronha"),
        BeerSpot(
            "São Paulo", "America/Sao_Paulo", "BR", GeoPoint(-23.55, -46.63),
            listOf(
                Brewery("Cervejaria Dogma", website = "https://cervejariadogma.com.br"),
                Brewery("Ambev"),
            ),
            shortName = "Rio",
        ),
        BeerSpot("Buenos Aires", "America/Argentina/Buenos_Aires", "AR", GeoPoint(-34.6, -58.38), listOf(Brewery("Cervecería Quilmes")), shortName = "B.Aires"),
        BeerSpot("St. John's", "America/St_Johns", "CA", GeoPoint(47.56, -52.71), listOf(Brewery("Quidi Vidi Brewing")), shortName = "St.John"),
        BeerSpot(
            "Halifax", "America/Halifax", "CA", GeoPoint(44.65, -63.57),
            listOf(
                Brewery("Garrison Brewing", website = "https://www.garrisonbrewing.com"),
                Brewery("Labatt"),
            ),
        ),
        BeerSpot("Caracas", "America/Caracas", "VE", GeoPoint(10.48, -66.9), listOf(Brewery("Cervecería Polar"))),
        BeerSpot(
            "New York", "America/New_York", "US", GeoPoint(40.71, -74.01),
            listOf(
                Brewery("Other Half Brewing", "Brooklyn", GeoPoint(40.68, -74.00), website = "https://otherhalfbrewing.com"),
                Brewery("Brooklyn Brewery", "Brooklyn", GeoPoint(40.72, -73.96)),
            ),
            shortName = "NYC",
        ),
        BeerSpot("Chicago", "America/Chicago", "US", GeoPoint(41.88, -87.63), listOf(Brewery("Half Acre", website = "https://halfacrebeer.com"))),
        BeerSpot("Mexiko-Stadt", "America/Mexico_City", "MX", GeoPoint(19.43, -99.13), listOf(Brewery("Grupo Modelo")), shortName = "Mexiko"),
        BeerSpot(
            "Denver", "America/Denver", "US", GeoPoint(39.74, -104.99),
            listOf(
                Brewery("Great Divide", website = "https://greatdivide.com"),
                Brewery("Coors", "Golden", GeoPoint(39.76, -105.22)),
            ),
        ),
        BeerSpot("Phoenix", "America/Phoenix", "US", GeoPoint(33.45, -112.07), listOf(Brewery("Arizona Wilderness"))),
        BeerSpot("Los Angeles", "America/Los_Angeles", "US", GeoPoint(34.05, -118.24), listOf(Brewery("Highland Park Brewery", website = "https://hpb.la")), shortName = "L.A."),
        BeerSpot("Anchorage", "America/Anchorage", "US", GeoPoint(61.22, -149.9), listOf(Brewery("Alaskan Brewing", "Juneau", GeoPoint(58.36, -134.58), website = "https://www.alaskanbeer.com")), shortName = "Alaska"),
        BeerSpot("Gambierinseln", "Pacific/Gambier", "PF", GeoPoint(-23.12, -134.97), listOf(Brewery("Brasserie de Tahiti", "Papeete", GeoPoint(-17.54, -149.57))), shortName = "Gambier"),
        BeerSpot("Marquesas", "Pacific/Marquesas", "PF", GeoPoint(-8.91, -140.1), listOf(Brewery("Brasserie de Tahiti", "Papeete", GeoPoint(-17.54, -149.57))), shortName = "Marq."),
        BeerSpot("Honolulu", "Pacific/Honolulu", "US", GeoPoint(21.31, -157.86), listOf(Brewery("Honolulu Beerworks", website = "https://www.honolulubeerworks.com")), shortName = "Hawaii"),
        BeerSpot("Pago Pago", "Pacific/Pago_Pago", "AS", GeoPoint(-14.28, -170.7), emptyList(), shortName = "Samoa"),
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
        "Europe/Brussels" to EnglishName("Brussels", "BXL"),
        "Europe/Lisbon" to EnglishName("Lisbon"),
        "Atlantic/Reykjavik" to EnglishName("Reykjavík", "Iceland"),
        "Atlantic/Azores" to EnglishName("Azores"),
        "Atlantic/South_Georgia" to EnglishName("South Georgia", "S.Georg"),
        "America/Mexico_City" to EnglishName("Mexico City", "Mexico"),
        "Pacific/Gambier" to EnglishName("Gambier Islands", "Gambier"),
    )

    /** English names of brewery towns where they differ from the German ones. */
    val ENGLISH_TOWNS: Map<String, String> = mapOf(
        "München" to "Munich",
        "Nürnberg" to "Nuremberg",
    )
}
