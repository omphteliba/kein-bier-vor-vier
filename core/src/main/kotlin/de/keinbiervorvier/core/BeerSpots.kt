package de.keinbiervorvier.core

import java.time.ZoneId

/** A typical local beer: brand name and style, no logos. */
data class Beer(val name: String, val style: String)

/**
 * A place where it can be 16:00, shown with a human-friendly name.
 * [countryCode] is ISO 3166-1 alpha-2 and drives the [flag]; [beer] is null
 * where there is no local beer to speak of.
 * [shortName] fits small watch face slots (at most [SHORT_NAME_MAX] characters).
 */
data class BeerSpot(
    val city: String,
    val zone: ZoneId,
    val countryCode: String,
    val beer: Beer?,
    val shortName: String = city,
) {
    constructor(city: String, zoneId: String, countryCode: String, beer: Beer?, shortName: String = city) :
        this(city, ZoneId.of(zoneId), countryCode, beer, shortName)

    /** The country's flag emoji, built from regional indicator symbols. */
    val flag: String
        get() = countryCode.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")
}

/** Short text complications only have room for about seven characters. */
const val SHORT_NAME_MAX = 7

/**
 * One well-known city per distinct UTC offset (including the odd half- and
 * quarter-hour ones), ordered from east to west.
 */
object BeerSpots {
    val ALL: List<BeerSpot> = listOf(
        BeerSpot("Kiritimati", "Pacific/Kiritimati", "KI", null, shortName = "Kiriti"),
        BeerSpot("Chatham-Inseln", "Pacific/Chatham", "NZ", null, shortName = "Chatham"),
        BeerSpot("Nukuʻalofa", "Pacific/Tongatapu", "TO", Beer("Ikale", "Lager"), shortName = "Tonga"),
        BeerSpot("Auckland", "Pacific/Auckland", "NZ", Beer("Steinlager Classic", "Lager"), shortName = "Auckld"),
        BeerSpot("Suva", "Pacific/Fiji", "FJ", Beer("Fiji Bitter", "Lager")),
        BeerSpot("Nouméa", "Pacific/Noumea", "NC", Beer("Number One", "Lager")),
        BeerSpot("Lord Howe", "Australia/Lord_Howe", "AU", null, shortName = "L. Howe"),
        BeerSpot("Sydney", "Australia/Sydney", "AU", Beer("Tooheys New", "Lager")),
        BeerSpot("Brisbane", "Australia/Brisbane", "AU", Beer("XXXX Gold", "Lager"), shortName = "Brisb."),
        BeerSpot("Adelaide", "Australia/Adelaide", "AU", Beer("Coopers Pale Ale", "Pale Ale"), shortName = "Adel."),
        BeerSpot("Darwin", "Australia/Darwin", "AU", Beer("NT Draught", "Lager")),
        BeerSpot("Tokio", "Asia/Tokyo", "JP", Beer("Asahi Super Dry", "Dry Lager")),
        BeerSpot("Eucla", "Australia/Eucla", "AU", null),
        BeerSpot("Shanghai", "Asia/Shanghai", "CN", Beer("Tsingtao", "Lager"), shortName = "Peking"),
        BeerSpot("Bangkok", "Asia/Bangkok", "TH", Beer("Singha", "Lager")),
        BeerSpot("Yangon", "Asia/Yangon", "MM", Beer("Myanmar Beer", "Lager")),
        BeerSpot("Dhaka", "Asia/Dhaka", "BD", null),
        BeerSpot("Kathmandu", "Asia/Kathmandu", "NP", Beer("Everest", "Lager"), shortName = "Nepal"),
        BeerSpot("Mumbai", "Asia/Kolkata", "IN", Beer("Kingfisher Premium", "Lager")),
        BeerSpot("Karatschi", "Asia/Karachi", "PK", Beer("Murree", "Lager"), shortName = "Lahore"),
        BeerSpot("Kabul", "Asia/Kabul", "AF", null),
        BeerSpot("Dubai", "Asia/Dubai", "AE", null),
        BeerSpot("Teheran", "Asia/Tehran", "IR", null),
        BeerSpot("Moskau", "Europe/Moscow", "RU", Beer("Baltika Nr. 7", "Lager")),
        BeerSpot("Nairobi", "Africa/Nairobi", "KE", Beer("Tusker", "Lager")),
        BeerSpot("Athen", "Europe/Athens", "GR", Beer("Mythos", "Lager")),
        BeerSpot("Kairo", "Africa/Cairo", "EG", Beer("Sakara Gold", "Lager")),
        BeerSpot("München", "Europe/Berlin", "DE", Beer("Augustiner Helles", "Helles")),
        BeerSpot("Lagos", "Africa/Lagos", "NG", Beer("Star Lager", "Lager")),
        BeerSpot("Dublin", "Europe/Dublin", "IE", Beer("Guinness Draught", "Irish Stout")),
        BeerSpot("Lissabon", "Europe/Lisbon", "PT", Beer("Super Bock", "Lager"), shortName = "Lisboa"),
        BeerSpot("Reykjavík", "Atlantic/Reykjavik", "IS", Beer("Einstök White Ale", "Witbier"), shortName = "Island"),
        BeerSpot("Azoren", "Atlantic/Azores", "PT", Beer("Especial", "Lager")),
        BeerSpot("Praia", "Atlantic/Cape_Verde", "CV", Beer("Strela", "Lager")),
        BeerSpot("Südgeorgien", "Atlantic/South_Georgia", "GS", null, shortName = "Südgeo."),
        BeerSpot("Fernando de Noronha", "America/Noronha", "BR", Beer("Brahma Chopp", "Lager"), shortName = "Noronha"),
        BeerSpot("São Paulo", "America/Sao_Paulo", "BR", Beer("Antarctica Original", "Pilsner"), shortName = "Rio"),
        BeerSpot("Buenos Aires", "America/Argentina/Buenos_Aires", "AR", Beer("Quilmes Clásica", "Lager"), shortName = "B.Aires"),
        BeerSpot("St. John's", "America/St_Johns", "CA", Beer("Quidi Vidi Iceberg", "Lager"), shortName = "St.John"),
        BeerSpot("Halifax", "America/Halifax", "CA", Beer("Alexander Keith's", "India Pale Ale")),
        BeerSpot("Caracas", "America/Caracas", "VE", Beer("Polar Pilsen", "Pilsner")),
        BeerSpot("New York", "America/New_York", "US", Beer("Brooklyn Lager", "Amber Lager"), shortName = "NYC"),
        BeerSpot("Chicago", "America/Chicago", "US", Beer("Goose Island IPA", "IPA")),
        BeerSpot("Mexiko-Stadt", "America/Mexico_City", "MX", Beer("Victoria", "Vienna Lager"), shortName = "Mexiko"),
        BeerSpot("Denver", "America/Denver", "US", Beer("Coors Banquet", "Lager")),
        BeerSpot("Phoenix", "America/Phoenix", "US", Beer("Kilt Lifter", "Scottish Ale")),
        BeerSpot("Los Angeles", "America/Los_Angeles", "US", Beer("Mango Cart", "Wheat Ale"), shortName = "L.A."),
        BeerSpot("Anchorage", "America/Anchorage", "US", Beer("Alaskan Amber", "Altbier"), shortName = "Alaska"),
        BeerSpot("Gambierinseln", "Pacific/Gambier", "PF", Beer("Hinano", "Lager"), shortName = "Gambier"),
        BeerSpot("Marquesas", "Pacific/Marquesas", "PF", Beer("Hinano", "Lager"), shortName = "Marq."),
        BeerSpot("Honolulu", "Pacific/Honolulu", "US", Beer("Kona Big Wave", "Golden Ale"), shortName = "Hawaii"),
        BeerSpot("Pago Pago", "Pacific/Pago_Pago", "AS", null, shortName = "Samoa"),
    )
}
