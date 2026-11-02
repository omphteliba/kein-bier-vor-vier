package de.keinbiervorvier.core

import java.time.ZoneId

/**
 * A place where it can be 16:00, shown with a human-friendly name.
 * [shortName] fits small watch face slots (at most [SHORT_NAME_MAX] characters).
 */
data class BeerSpot(val city: String, val zone: ZoneId, val shortName: String = city) {
    constructor(city: String, zoneId: String, shortName: String = city) : this(city, ZoneId.of(zoneId), shortName)
}

/** Short text complications only have room for about seven characters. */
const val SHORT_NAME_MAX = 7

/**
 * One well-known city per distinct UTC offset (including the odd half- and
 * quarter-hour ones), ordered from east to west.
 */
object BeerSpots {
    val ALL: List<BeerSpot> = listOf(
        BeerSpot("Kiritimati", "Pacific/Kiritimati", "Kiriti"),
        BeerSpot("Chatham-Inseln", "Pacific/Chatham", "Chatham"),
        BeerSpot("Nukuʻalofa", "Pacific/Tongatapu", "Tonga"),
        BeerSpot("Auckland", "Pacific/Auckland", "Auckld"),
        BeerSpot("Suva", "Pacific/Fiji"),
        BeerSpot("Nouméa", "Pacific/Noumea"),
        BeerSpot("Lord Howe", "Australia/Lord_Howe", "L. Howe"),
        BeerSpot("Sydney", "Australia/Sydney"),
        BeerSpot("Brisbane", "Australia/Brisbane", "Brisb."),
        BeerSpot("Adelaide", "Australia/Adelaide", "Adel."),
        BeerSpot("Darwin", "Australia/Darwin"),
        BeerSpot("Tokio", "Asia/Tokyo"),
        BeerSpot("Eucla", "Australia/Eucla"),
        BeerSpot("Shanghai", "Asia/Shanghai", "Peking"),
        BeerSpot("Bangkok", "Asia/Bangkok"),
        BeerSpot("Yangon", "Asia/Yangon"),
        BeerSpot("Dhaka", "Asia/Dhaka"),
        BeerSpot("Kathmandu", "Asia/Kathmandu", "Nepal"),
        BeerSpot("Mumbai", "Asia/Kolkata"),
        BeerSpot("Karatschi", "Asia/Karachi", "Lahore"),
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
        BeerSpot("Lissabon", "Europe/Lisbon", "Lisboa"),
        BeerSpot("Reykjavík", "Atlantic/Reykjavik", "Island"),
        BeerSpot("Azoren", "Atlantic/Azores"),
        BeerSpot("Praia", "Atlantic/Cape_Verde"),
        BeerSpot("Südgeorgien", "Atlantic/South_Georgia", "Südgeo."),
        BeerSpot("Fernando de Noronha", "America/Noronha", "Noronha"),
        BeerSpot("São Paulo", "America/Sao_Paulo", "Rio"),
        BeerSpot("Buenos Aires", "America/Argentina/Buenos_Aires", "B.Aires"),
        BeerSpot("St. John's", "America/St_Johns", "St.John"),
        BeerSpot("Halifax", "America/Halifax"),
        BeerSpot("Caracas", "America/Caracas"),
        BeerSpot("New York", "America/New_York", "NYC"),
        BeerSpot("Chicago", "America/Chicago"),
        BeerSpot("Mexiko-Stadt", "America/Mexico_City", "Mexiko"),
        BeerSpot("Denver", "America/Denver"),
        BeerSpot("Phoenix", "America/Phoenix"),
        BeerSpot("Los Angeles", "America/Los_Angeles", "L.A."),
        BeerSpot("Anchorage", "America/Anchorage", "Alaska"),
        BeerSpot("Gambierinseln", "Pacific/Gambier", "Gambier"),
        BeerSpot("Marquesas", "Pacific/Marquesas", "Marq."),
        BeerSpot("Honolulu", "Pacific/Honolulu", "Hawaii"),
        BeerSpot("Pago Pago", "Pacific/Pago_Pago", "Samoa"),
    )
}
