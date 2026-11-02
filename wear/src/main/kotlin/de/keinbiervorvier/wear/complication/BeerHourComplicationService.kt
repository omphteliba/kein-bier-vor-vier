package de.keinbiervorvier.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import de.keinbiervorvier.common.EXTRA_EDITION
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerSpot
import de.keinbiervorvier.core.BeerSpots
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.wear.MainActivity
import de.keinbiervorvier.wear.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/** "Kein Bier vor Vier": where it is 16:00. */
class KeinBierVorVierComplicationService : BeerHourComplicationService(Edition.KEIN_BIER_VOR_VIER)

/** "It's five o'clock somewhere": where it is 17:00. */
class FiveOClockComplicationService : BeerHourComplicationService(Edition.FIVE_O_CLOCK_SOMEWHERE)

/**
 * Shows the city where the beer hour most recently started. The whole next day
 * is handed to the system as a timeline, so the complication switches cities on
 * time without waking this service; the countdown is rendered live by the
 * system as well. Texts follow the watch's language, the hour follows [edition].
 */
abstract class BeerHourComplicationService(private val edition: Edition) :
    SuspendingTimelineComplicationDataSourceService() {

    private val locale: Locale get() = resources.configuration.locales[0]

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(
            type,
            spot = BeerSpots.ALL.first { it.zone.id == "Asia/Kathmandu" },
            date = LocalDate.now(),
            next = BeerSpots.ALL.first { it.zone.id == "Asia/Kolkata" },
            nextAt = Instant.now().plus(Duration.ofMinutes(15)),
        )

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val type = request.complicationType
        val now = Instant.now()
        val status = BeerClock.status(now, edition)
        val default = build(
            type,
            spot = status.openNow.first().spot,
            date = status.openNow.first().date,
            next = status.next.spots.first(),
            nextAt = status.next.at,
        ) ?: return null

        val rounds = BeerClock.rounds(now, now.plus(TIMELINE_LENGTH), edition)
        val entries = rounds.zipWithNext { round, following ->
            TimelineEntry(
                TimeInterval(round.at, following.at),
                build(type, round.spots.first(), round.spots.first().dateAt(round.at), following.spots.first(), following.at)!!,
            )
        }
        return ComplicationDataTimeline(default, entries)
    }

    /** [date] is the local date of [spot]'s round, which picks its beer and place of the day. */
    private fun build(type: ComplicationType, spot: BeerSpot, date: LocalDate, next: BeerSpot, nextAt: Instant): ComplicationData? {
        val place = spot.place(locale, date)
        val description = PlainComplicationText.Builder(getString(R.string.complication_description, place)).build()
        return when (type) {
            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(flagImage(spot), description)
                .setTapAction(openApp())
                .build()

            // Watch faces tint complication text, which turns a flag emoji into a white
            // blob, so text slots get the short name and the flag only as a colour image.
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(plain(spot.shortName(locale)), description)
                .setMonochromaticImage(icon())
                .setSmallImage(flagImage(spot))
                .setTapAction(openApp())
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(plain(place), description)
                .setTitle(countdown(next.place(locale, next.dateAt(nextAt)), nextAt))
                .setMonochromaticImage(icon())
                .setSmallImage(flagImage(spot))
                .setTapAction(openApp())
                .build()

            else -> null
        }
    }

    private fun flagImage(spot: BeerSpot): SmallImage =
        SmallImage.Builder(Icon.createWithBitmap(renderFlag(spot.flag)), SmallImageType.PHOTO).build()

    private fun plain(text: String): ComplicationText = PlainComplicationText.Builder(text).build()

    private fun countdown(nextCity: String, nextAt: Instant): ComplicationText =
        TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.SHORT_DUAL_UNIT, CountDownTimeReference(nextAt))
            .setText(getString(R.string.complication_next, nextCity))
            .build()

    private fun icon() = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_beer)).build()

    /** Opens the app on this complication's edition, whatever the watch's language. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        edition.ordinal,
        Intent(this, MainActivity::class.java).putExtra(EXTRA_EDITION, edition.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        val TIMELINE_LENGTH: Duration = Duration.ofHours(24)
    }
}
