package de.keinbiervorvier.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class BeerClockTest {

    private val berlin = BeerSpot("München", "Europe/Berlin", "DE", GeoPoint(48.14, 11.58), null)
    private val kathmandu = BeerSpot("Kathmandu", "Asia/Kathmandu", "NP", GeoPoint(27.72, 85.32), null)
    private val mumbai = BeerSpot("Mumbai", "Asia/Kolkata", "IN", GeoPoint(19.08, 72.88), null)

    @Test
    fun `every zone id in the list exists`() {
        BeerSpots.ALL.forEach { ZoneId.of(it.zone.id) }
    }

    @Test
    fun `flags are built from the country code`() {
        assertEquals("Deutschland", berlin.country(Locale.GERMANY))
        assertEquals("Germany", berlin.country(Locale.US))
        assertEquals("🇩🇪", BeerSpot("München", "Europe/Berlin", "DE", GeoPoint(48.14, 11.58), null).flag)
        BeerSpots.ALL.forEach { assertTrue(it.countryCode.matches(Regex("[A-Z]{2}")), it.city) }
    }

    @Test
    fun `every short name fits a small complication`() {
        for (locale in listOf(Locale.GERMAN, Locale.ENGLISH)) {
            BeerSpots.ALL.forEach {
                val name = it.shortName(locale)
                assertTrue(name.codePointCount(0, name.length) <= SHORT_NAME_MAX, "${it.city}: '$name' is too long")
            }
        }
    }

    @Test
    fun `names follow the language`() {
        assertEquals("München", berlin.city(Locale.GERMANY))
        assertEquals("Munich", berlin.city(Locale.UK))
        assertEquals("Kathmandu", kathmandu.city(Locale.FRENCH))
        assertTrue(BeerSpots.ENGLISH_NAMES.keys.all { id -> BeerSpots.ALL.any { it.zone.id == id } })
    }

    @Test
    fun `somewhere it is always beer o'clock, in both editions`() {
        for (edition in Edition.entries) {
            var t = Instant.parse("2026-01-01T00:00:00Z")
            val end = t.plus(Duration.ofDays(366))
            while (t < end) {
                assertTrue(BeerClock.status(t, edition).openNow.isNotEmpty(), "Nobody drinking at $t ($edition)")
                t = t.plus(Duration.ofMinutes(7))
            }
        }
    }

    @Test
    fun `five o'clock edition starts an hour later`() {
        // 11:20 UTC = 17:05 Kathmandu
        val status = BeerClock.status(
            Instant.parse("2026-11-02T11:20:00Z"),
            Edition.FIVE_O_CLOCK_SOMEWHERE,
            listOf(berlin, kathmandu, mumbai),
        )
        assertEquals(listOf(kathmandu), status.openNow.map { it.spot })
        assertEquals(5, status.openNow.single().minutesIn)
    }

    @Test
    fun `edition follows the language`() {
        assertEquals(Edition.KEIN_BIER_VOR_VIER, Edition.forLocale(Locale.GERMANY))
        assertEquals(Edition.KEIN_BIER_VOR_VIER, Edition.forLocale(Locale.forLanguageTag("de-AT")))
        assertEquals(Edition.FIVE_O_CLOCK_SOMEWHERE, Edition.forLocale(Locale.US))
    }

    @Test
    fun `open spots are those in the 16 o'clock hour, newest first`() {
        // 10:20 UTC = 16:05 Kathmandu (+5:45), 15:50 Mumbai (+5:30)
        val status = BeerClock.status(Instant.parse("2026-11-02T10:20:00Z"), spots = listOf(berlin, kathmandu, mumbai))
        assertEquals(listOf(kathmandu), status.openNow.map { it.spot })
        assertEquals(5, status.openNow.single().minutesIn)
        assertEquals(listOf(mumbai), status.next.spots)
        assertEquals(Duration.ofMinutes(10), status.next.countdown)
    }

    @Test
    fun `next round rolls over to tomorrow once 16 o'clock has passed`() {
        val now = Instant.parse("2026-11-02T15:00:00Z") // 16:00 Berlin (CET) exactly
        assertEquals(Instant.parse("2026-11-03T15:00:00Z"), BeerClock.nextBeerHourStart(now, berlin))
    }

    @Test
    fun `daylight saving time is respected`() {
        val summer = Instant.parse("2026-07-01T00:00:00Z")
        assertEquals(Instant.parse("2026-07-01T14:00:00Z"), BeerClock.nextBeerHourStart(summer, berlin))
        val winter = Instant.parse("2026-12-01T00:00:00Z")
        assertEquals(Instant.parse("2026-12-01T15:00:00Z"), BeerClock.nextBeerHourStart(winter, berlin))
    }

    @Test
    fun `rounds lists every start in order and matches the open spot`() {
        val from = Instant.parse("2026-11-02T00:00:00Z")
        val rounds = BeerClock.rounds(from, from.plus(Duration.ofDays(1)))
        assertTrue(rounds.size > 24)
        assertEquals(rounds.sortedBy { it.at }, rounds)
        rounds.forEach { round ->
            val open = BeerClock.status(round.at).openNow.first()
            assertEquals(0, open.minutesIn)
            assertTrue(open.spot in round.spots)
        }
    }

    @Test
    fun `spots reaching 16 o'clock at the same moment are grouped`() {
        val lagos = BeerSpot("Lagos", "Africa/Lagos", "NG", GeoPoint(6.52, 3.38), null) // UTC+1, like Berlin in winter
        val status = BeerClock.status(Instant.parse("2026-12-01T12:00:00Z"), spots = listOf(berlin, lagos, kathmandu))
        assertEquals(setOf(berlin, lagos), status.next.spots.toSet())
    }
}
