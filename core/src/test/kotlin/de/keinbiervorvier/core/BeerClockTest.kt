package de.keinbiervorvier.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class BeerClockTest {

    private val berlin = BeerSpot("München", "Europe/Berlin", "DE", null)
    private val kathmandu = BeerSpot("Kathmandu", "Asia/Kathmandu", "NP", null)
    private val mumbai = BeerSpot("Mumbai", "Asia/Kolkata", "IN", null)

    @Test
    fun `every zone id in the list exists`() {
        BeerSpots.ALL.forEach { ZoneId.of(it.zone.id) }
    }

    @Test
    fun `flags are built from the country code`() {
        assertEquals("🇩🇪", BeerSpot("München", "Europe/Berlin", "DE", null).flag)
        BeerSpots.ALL.forEach { assertTrue(it.countryCode.matches(Regex("[A-Z]{2}")), it.city) }
    }

    @Test
    fun `every short name fits a small complication`() {
        BeerSpots.ALL.forEach {
            val length = it.shortName.codePointCount(0, it.shortName.length)
            assertTrue(length <= SHORT_NAME_MAX, "${it.city}: '${it.shortName}' is too long")
        }
    }

    @Test
    fun `somewhere it is always beer o'clock`() {
        var t = Instant.parse("2026-01-01T00:00:00Z")
        val end = t.plus(Duration.ofDays(366))
        while (t < end) {
            assertTrue(BeerClock.status(t).openNow.isNotEmpty(), "Nobody drinking at $t")
            t = t.plus(Duration.ofMinutes(7))
        }
    }

    @Test
    fun `open spots are those in the 16 o'clock hour, newest first`() {
        // 10:20 UTC = 16:05 Kathmandu (+5:45), 15:50 Mumbai (+5:30)
        val status = BeerClock.status(Instant.parse("2026-11-02T10:20:00Z"), listOf(berlin, kathmandu, mumbai))
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
        val lagos = BeerSpot("Lagos", "Africa/Lagos", "NG", null) // UTC+1, like Berlin in winter
        val status = BeerClock.status(Instant.parse("2026-12-01T12:00:00Z"), listOf(berlin, lagos, kathmandu))
        assertEquals(setOf(berlin, lagos), status.next.spots.toSet())
    }
}
