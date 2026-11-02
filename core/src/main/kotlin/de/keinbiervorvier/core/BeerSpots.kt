package de.keinbiervorvier.core

import java.time.ZoneId

/** A place where it can be 16:00, shown with a human-friendly name. */
data class BeerSpot(val city: String, val zone: ZoneId) {
    constructor(city: String, zoneId: String) : this(city, ZoneId.of(zoneId))
}

/**
 * One well-known city per distinct UTC offset (including the odd half- and
 * quarter-hour ones), ordered from east to west.
 */
object BeerSpots {
    val ALL: List<BeerSpot> = listOf(
        BeerSpot("Kiritimati", "Pacific/Kiritimati"),
        BeerSpot("Chatham-Inseln", "Pacific/Chatham"),
        BeerSpot("Nukuʻalofa", "Pacific/Tongatapu"),
        BeerSpot("Auckland", "Pacific/Auckland"),
        BeerSpot("Suva", "Pacific/Fiji"),
        BeerSpot("Nouméa", "Pacific/Noumea"),
        BeerSpot("Lord Howe", "Australia/Lord_Howe"),
        BeerSpot("Sydney", "Australia/Sydney"),
        BeerSpot("Brisbane", "Australia/Brisbane"),
        BeerSpot("Adelaide", "Australia/Adelaide"),
        BeerSpot("Darwin", "Australia/Darwin"),
        BeerSpot("Tokio", "Asia/Tokyo"),
        BeerSpot("Eucla", "Australia/Eucla"),
        BeerSpot("Shanghai", "Asia/Shanghai"),
        BeerSpot("Bangkok", "Asia/Bangkok"),
        BeerSpot("Yangon", "Asia/Yangon"),
        BeerSpot("Dhaka", "Asia/Dhaka"),
        BeerSpot("Kathmandu", "Asia/Kathmandu"),
        BeerSpot("Mumbai", "Asia/Kolkata"),
        BeerSpot("Karatschi", "Asia/Karachi"),
        BeerSpot("Kabul", "Asia/Kabul"),
        BeerSpot("Dubai", "Asia/Dubai"),
        BeerSpot("Teheran", "Asia/Tehran"),
        BeerSpot("Moskau", "Europe/Moscow"),
        BeerSpot("Nairobi", "Africa/Nairobi"),
        BeerSpot("Athen", "Europe/Athens"),
        BeerSpot("Kairo", "Africa/Cairo"),
        BeerSpot("München", "Europe/Berlin"),
        BeerSpot("Lagos", "Africa/Lagos"),
        BeerSpot("Dublin", "Europe/Dublin"),
        BeerSpot("Lissabon", "Europe/Lisbon"),
        BeerSpot("Reykjavík", "Atlantic/Reykjavik"),
        BeerSpot("Azoren", "Atlantic/Azores"),
        BeerSpot("Praia", "Atlantic/Cape_Verde"),
        BeerSpot("Südgeorgien", "Atlantic/South_Georgia"),
        BeerSpot("Fernando de Noronha", "America/Noronha"),
        BeerSpot("São Paulo", "America/Sao_Paulo"),
        BeerSpot("Buenos Aires", "America/Argentina/Buenos_Aires"),
        BeerSpot("St. John's", "America/St_Johns"),
        BeerSpot("Halifax", "America/Halifax"),
        BeerSpot("Caracas", "America/Caracas"),
        BeerSpot("New York", "America/New_York"),
        BeerSpot("Chicago", "America/Chicago"),
        BeerSpot("Mexiko-Stadt", "America/Mexico_City"),
        BeerSpot("Denver", "America/Denver"),
        BeerSpot("Phoenix", "America/Phoenix"),
        BeerSpot("Los Angeles", "America/Los_Angeles"),
        BeerSpot("Anchorage", "America/Anchorage"),
        BeerSpot("Gambierinseln", "Pacific/Gambier"),
        BeerSpot("Marquesas", "Pacific/Marquesas"),
        BeerSpot("Honolulu", "Pacific/Honolulu"),
        BeerSpot("Pago Pago", "Pacific/Pago_Pago"),
    )
}
