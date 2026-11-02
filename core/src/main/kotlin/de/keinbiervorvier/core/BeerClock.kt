package de.keinbiervorvier.core

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/** A spot where the beer hour is currently running. */
data class OpenSpot(val spot: BeerSpot, val localTime: LocalTime) {
    /** Minutes since 16:00 local time. */
    val minutesIn: Int get() = localTime.hour * 60 + localTime.minute - BEER_HOUR * 60
}

/** The next moment some spots reach 16:00, and which ones. */
data class NextRound(val at: Instant, val countdown: Duration, val spots: List<BeerSpot>)

data class BeerStatus(
    /** Spots between 16:00 and 16:59 right now, most recently started first. */
    val openNow: List<OpenSpot>,
    val next: NextRound,
)

const val BEER_HOUR = 16

object BeerClock {

    fun status(now: Instant, spots: List<BeerSpot> = BeerSpots.ALL): BeerStatus {
        require(spots.isNotEmpty()) { "Need at least one spot" }

        val openNow = spots
            .map { OpenSpot(it, now.atZone(it.zone).toLocalTime()) }
            .filter { it.localTime.hour == BEER_HOUR }
            .sortedBy { it.localTime }

        val byNextStart = spots.groupBy { nextBeerHourStart(now, it) }
        val nextAt = byNextStart.keys.min()
        val next = NextRound(nextAt, Duration.between(now, nextAt), byNextStart.getValue(nextAt))

        return BeerStatus(openNow, next)
    }

    /** The first 16:00 in [spot]'s zone strictly after [now]. */
    fun nextBeerHourStart(now: Instant, spot: BeerSpot): Instant {
        val local = now.atZone(spot.zone)
        var candidate = ZonedDateTime.of(local.toLocalDate(), LocalTime.of(BEER_HOUR, 0), spot.zone)
        if (!candidate.isAfter(local)) candidate = candidate.plusDays(1)
        return candidate.toInstant()
    }
}
