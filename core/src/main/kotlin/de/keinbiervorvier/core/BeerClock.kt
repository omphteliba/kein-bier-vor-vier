package de.keinbiervorvier.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

/** Which saying the app follows, and so which hour counts as beer o'clock. */
enum class Edition(val hour: Int) {
    /** German: "Kein Bier vor Vier" – no beer before 4 p.m. */
    KEIN_BIER_VOR_VIER(16),

    /** English: "It's five o'clock somewhere". */
    FIVE_O_CLOCK_SOMEWHERE(17);

    companion object {
        /** German speakers get their saying, everyone else the English one. */
        fun forLocale(locale: Locale): Edition =
            if (locale.language == "de") KEIN_BIER_VOR_VIER else FIVE_O_CLOCK_SOMEWHERE
    }
}

/** A spot where the beer hour is currently running, on its local [date]. */
data class OpenSpot(val spot: BeerSpot, val date: LocalDate, val localTime: LocalTime, val edition: Edition) {
    /** Minutes since the beer hour started locally. */
    val minutesIn: Int get() = localTime.hour * 60 + localTime.minute - edition.hour * 60

    /** Today's brewery here. */
    val brewery: Brewery? get() = spot.breweryOn(date)

    /** Today's place name, see [BeerSpot.place]. */
    fun place(locale: Locale): String = spot.place(locale, date)
}

/** The next moment some spots reach the beer hour, and which ones. */
data class NextRound(val at: Instant, val countdown: Duration, val spots: List<BeerSpot>)

data class BeerStatus(
    /** Spots inside the beer hour right now, most recently started first. */
    val openNow: List<OpenSpot>,
    val next: NextRound,
)

object BeerClock {

    fun status(
        now: Instant,
        edition: Edition = Edition.KEIN_BIER_VOR_VIER,
        spots: List<BeerSpot> = BeerSpots.ALL,
    ): BeerStatus {
        require(spots.isNotEmpty()) { "Need at least one spot" }

        val openNow = spots
            .map { now.atZone(it.zone).let { local -> OpenSpot(it, local.toLocalDate(), local.toLocalTime(), edition) } }
            .filter { it.localTime.hour == edition.hour }
            .groupBy { it.localTime }
            .toSortedMap()
            .values
            .flatMap { sameTime -> sameTime.takingTurns(sameTime.first().date) }

        val byNextStart = spots.groupBy { nextBeerHourStart(now, it, edition) }
        val nextAt = byNextStart.keys.min()
        val nextSpots = byNextStart.getValue(nextAt).let { sameTime ->
            sameTime.takingTurns(nextAt.atZone(sameTime.first().zone).toLocalDate())
        }
        val next = NextRound(nextAt, Duration.between(now, nextAt), nextSpots)

        return BeerStatus(openNow, next)
    }

    /** All rounds starting after [from] up to and including [until], in order. */
    fun rounds(
        from: Instant,
        until: Instant,
        edition: Edition = Edition.KEIN_BIER_VOR_VIER,
        spots: List<BeerSpot> = BeerSpots.ALL,
    ): List<NextRound> {
        val result = mutableListOf<NextRound>()
        var t = from
        while (true) {
            val next = status(t, edition, spots).next
            if (next.at > until) return result
            result += next.copy(countdown = Duration.between(from, next.at))
            t = next.at
        }
    }

    /**
     * Spots whose beer hour starts at the same moment (Germany, France and
     * Belgium, say) take turns being first, one day each.
     */
    private fun <T> List<T>.takingTurns(date: LocalDate): List<T> =
        indices.map { this[Math.floorMod(it + date.toEpochDay(), size.toLong()).toInt()] }

    /** The first start of the beer hour in [spot]'s zone strictly after [now]. */
    fun nextBeerHourStart(now: Instant, spot: BeerSpot, edition: Edition = Edition.KEIN_BIER_VOR_VIER): Instant {
        val local = now.atZone(spot.zone)
        var candidate = ZonedDateTime.of(local.toLocalDate(), LocalTime.of(edition.hour, 0), spot.zone)
        if (!candidate.isAfter(local)) candidate = candidate.plusDays(1)
        return candidate.toInstant()
    }
}
