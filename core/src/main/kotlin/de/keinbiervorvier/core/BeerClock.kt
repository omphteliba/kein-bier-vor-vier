package de.keinbiervorvier.core

import java.time.Duration
import java.time.Instant
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

/** A spot where the beer hour is currently running. */
data class OpenSpot(val spot: BeerSpot, val localTime: LocalTime, val edition: Edition) {
    /** Minutes since the beer hour started locally. */
    val minutesIn: Int get() = localTime.hour * 60 + localTime.minute - edition.hour * 60
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
            .map { OpenSpot(it, now.atZone(it.zone).toLocalTime(), edition) }
            .filter { it.localTime.hour == edition.hour }
            .sortedBy { it.localTime }

        val byNextStart = spots.groupBy { nextBeerHourStart(now, it, edition) }
        val nextAt = byNextStart.keys.min()
        val next = NextRound(nextAt, Duration.between(now, nextAt), byNextStart.getValue(nextAt))

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

    /** The first start of the beer hour in [spot]'s zone strictly after [now]. */
    fun nextBeerHourStart(now: Instant, spot: BeerSpot, edition: Edition = Edition.KEIN_BIER_VOR_VIER): Instant {
        val local = now.atZone(spot.zone)
        var candidate = ZonedDateTime.of(local.toLocalDate(), LocalTime.of(edition.hour, 0), spot.zone)
        if (!candidate.isAfter(local)) candidate = candidate.plusDays(1)
        return candidate.toInstant()
    }
}
