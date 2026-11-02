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
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerSpot
import de.keinbiervorvier.core.GeoPoint
import de.keinbiervorvier.wear.MainActivity
import de.keinbiervorvier.wear.R
import java.time.Duration
import java.time.Instant

/**
 * Shows the city where it most recently turned 16:00. The whole next day is
 * handed to the system as a timeline, so the complication switches cities on
 * time without waking this service; the countdown is rendered live by the
 * system as well.
 */
class BeerComplicationService : SuspendingTimelineComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(
            type,
            spot = BeerSpot("Kathmandu", "Asia/Kathmandu", "NP", GeoPoint(27.72, 85.32), null, shortName = "Nepal"),
            next = BeerSpot("Mumbai", "Asia/Kolkata", "IN", GeoPoint(19.08, 72.88), null),
            nextAt = Instant.now().plus(Duration.ofMinutes(15)),
        )

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val type = request.complicationType
        val now = Instant.now()
        val status = BeerClock.status(now)
        val default = build(
            type,
            spot = status.openNow.first().spot,
            next = status.next.spots.first(),
            nextAt = status.next.at,
        ) ?: return null

        val rounds = BeerClock.rounds(now, now.plus(TIMELINE_LENGTH))
        val entries = rounds.zipWithNext { round, following ->
            TimelineEntry(
                TimeInterval(round.at, following.at),
                build(type, round.spots.first(), following.spots.first(), following.at)!!,
            )
        }
        return ComplicationDataTimeline(default, entries)
    }

    private fun build(type: ComplicationType, spot: BeerSpot, next: BeerSpot, nextAt: Instant): ComplicationData? {
        val description = PlainComplicationText.Builder(getString(R.string.complication_description, spot.city)).build()
        return when (type) {
            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(flagImage(spot), description)
                .setTapAction(openApp())
                .build()

            // Watch faces tint complication text, which turns a flag emoji into a white
            // blob, so text slots get the short name and the flag only as a colour image.
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(plain(spot.shortName), description)
                .setMonochromaticImage(icon())
                .setSmallImage(flagImage(spot))
                .setTapAction(openApp())
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(plain(spot.city), description)
                .setTitle(countdown(next.city, nextAt))
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

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        val TIMELINE_LENGTH: Duration = Duration.ofHours(24)
    }
}
