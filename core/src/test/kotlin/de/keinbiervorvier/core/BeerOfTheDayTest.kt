package de.keinbiervorvier.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class BeerOfTheDayTest {

    private val germany = BeerSpots.ALL.single { it.countryCode == "DE" }
    private val day = LocalDate.of(2026, 11, 2)

    @Test
    fun `beers take turns day by day and come round again`() {
        val beers = (0L until germany.beers.size).map { germany.beerOn(day.plusDays(it)) }
        assertEquals(germany.beers.toSet(), beers.toSet())
        assertEquals(germany.beerOn(day), germany.beerOn(day.plusDays(germany.beers.size.toLong())))
        assertNotEquals(germany.beerOn(day), germany.alsoOn(day))
    }

    @Test
    fun `the place is the town of the day's beer`() {
        val kirn = germany.beers.indexOfFirst { it.brewery == "Kirner Privatbrauerei" }
        val kirnDay = (0L until germany.beers.size).map { day.plusDays(it) }
            .first { germany.beerOn(it) == germany.beers[kirn] }
        assertEquals("Kirn", germany.place(Locale.GERMANY, kirnDay))
        assertEquals(GeoPoint(49.79, 7.46), germany.locationOn(kirnDay))

        val munichDay = (0L until germany.beers.size).map { day.plusDays(it) }
            .first { germany.beerOn(it)?.town == "München" }
        assertEquals("Munich", germany.place(Locale.US, munichDay))
    }

    @Test
    fun `spots without beer have none and keep their city`() {
        val tehran = BeerSpots.ALL.single { it.zone.id == "Asia/Tehran" }
        assertNull(tehran.beerOn(day))
        assertEquals("Teheran", tehran.place(Locale.GERMANY, day))
    }

    @Test
    fun `every beer names its brewery, and a town comes with a location`() {
        BeerSpots.ALL.flatMap { it.beers }.forEach {
            assertTrue(it.brewery.isNotBlank(), it.name)
            assertEquals(it.town == null, it.location == null, "${it.name}: town and location go together")
        }
    }

    @Test
    fun `germany france and belgium take turns being first`() {
        // 15:30 UTC in winter = 16:30 in Berlin, Paris, Brussels - and Lagos, which joins in.
        val sameTime = BeerClock.status(Instant.parse("2026-12-01T15:30:00Z")).openNow.map { it.spot.countryCode }
        assertEquals(setOf("DE", "FR", "BE", "NG"), sameTime.toSet())
        val firsts = (0L until sameTime.size.toLong()).map { d ->
            val now = Instant.parse("2026-12-01T15:30:00Z").plusSeconds(d * 86_400)
            BeerClock.status(now).openNow.first().spot.countryCode
        }
        assertEquals(sameTime.toSet(), firsts.toSet())
    }
}
