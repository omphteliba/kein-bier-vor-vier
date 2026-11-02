package de.keinbiervorvier.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class BreweryOfTheDayTest {

    private val germany = BeerSpots.ALL.single { it.countryCode == "DE" }
    private val day = LocalDate.of(2026, 11, 2)

    @Test
    fun `breweries take turns day by day and come round again`() {
        val breweries = (0L until germany.breweries.size).map { germany.breweryOn(day.plusDays(it)) }
        assertEquals(germany.breweries.toSet(), breweries.toSet())
        assertEquals(germany.breweryOn(day), germany.breweryOn(day.plusDays(germany.breweries.size.toLong())))
    }

    @Test
    fun `the carousel starts with the brewery of the day and has them all`() {
        val all = germany.breweriesFrom(day)
        assertEquals(germany.breweryOn(day), all.first())
        assertEquals(germany.breweries.toSet(), all.toSet())
        assertNotEquals(germany.breweriesFrom(day), germany.breweriesFrom(day.plusDays(1)))
    }

    @Test
    fun `the place is the town of the day's brewery`() {
        val kirn = germany.breweries.single { it.name == "Kirner Privatbrauerei" }
        val kirnDay = (0L until germany.breweries.size).map { day.plusDays(it) }
            .first { germany.breweryOn(it) == kirn }
        assertEquals("Kirn", germany.place(Locale.GERMANY, kirnDay))
        assertEquals(GeoPoint(49.79, 7.46), germany.locationOn(kirnDay))

        val munichDay = (0L until germany.breweries.size).map { day.plusDays(it) }
            .first { germany.breweryOn(it)?.town == "München" }
        assertEquals("Munich", germany.place(Locale.US, munichDay))
    }

    @Test
    fun `spots without beer have no brewery and keep their city`() {
        val tehran = BeerSpots.ALL.single { it.zone.id == "Asia/Tehran" }
        assertNull(tehran.breweryOn(day))
        assertTrue(tehran.breweriesFrom(day).isEmpty())
        assertEquals("Teheran", tehran.place(Locale.GERMANY, day))
    }

    @Test
    fun `every brewery has a name, a town comes with a location, websites are https`() {
        BeerSpots.ALL.flatMap { it.breweries }.forEach {
            assertTrue(it.name.isNotBlank())
            assertEquals(it.town == null, it.location == null, "${it.name}: town and location go together")
            it.website?.let { url -> assertTrue(url.startsWith("https://"), url) }
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
