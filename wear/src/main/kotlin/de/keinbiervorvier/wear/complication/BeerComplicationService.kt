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
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import de.keinbiervorvier.core.BeerClock
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
        build(type, city = "Kathmandu", nextCity = "Mumbai", nextAt = Instant.now().plus(Duration.ofMinutes(15)))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val type = request.complicationType
        val now = Instant.now()
        val status = BeerClock.status(now)
        val default = build(
            type,
            city = status.openNow.first().spot.city,
            nextCity = status.next.spots.first().city,
            nextAt = status.next.at,
        ) ?: return null

        val rounds = BeerClock.rounds(now, now.plus(TIMELINE_LENGTH))
        val entries = rounds.zipWithNext { round, following ->
            TimelineEntry(
                TimeInterval(round.at, following.at),
                build(type, round.spots.first().city, following.spots.first().city, following.at)!!,
            )
        }
        return ComplicationDataTimeline(default, entries)
    }

    private fun build(type: ComplicationType, city: String, nextCity: String, nextAt: Instant): ComplicationData? {
        val description = PlainComplicationText.Builder(getString(R.string.complication_description, city)).build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(plain(city), description)
                .setMonochromaticImage(icon())
                .setTapAction(openApp())
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(plain("🍺 $city"), description)
                .setTitle(countdown(nextCity, nextAt))
                .setMonochromaticImage(icon())
                .setTapAction(openApp())
                .build()

            else -> null
        }
    }

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
