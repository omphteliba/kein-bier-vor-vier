package de.keinbiervorvier.phone.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.keinbiervorvier.common.EXTRA_EDITION
import de.keinbiervorvier.common.nowInRes
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerStatus
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.phone.MainActivity
import de.keinbiervorvier.phone.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val Brown = ColorProvider(Color(0xFF3B2A1A))
private val BeerRed = ColorProvider(Color(0xFFC62828))

/**
 * Home screen widget: where it is beer o'clock right now and where it's next.
 * It shows the next round as a clock time rather than a countdown, so it only
 * has to change when a round starts; [WidgetUpdates] wakes it exactly then.
 */
class BeerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val locale = context.resources.configuration.locales[0]
        val edition = Edition.forLocale(locale)
        val status = BeerClock.status(Instant.now(), edition)
        WidgetUpdates.scheduleAt(context, status.next.at)
        provideContent { WidgetContent(context, status, edition, locale) }
    }
}

@Composable
private fun WidgetContent(context: Context, status: BeerStatus, edition: Edition, locale: Locale) {
    val current = status.openNow.first().spot
    val next = status.next.spots.first()
    val nextTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(status.next.at.atZone(ZoneId.systemDefault()))
    val open = Intent(context, MainActivity::class.java).putExtra(EXTRA_EDITION, edition.name)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp)
            .clickable(actionStartActivity(open)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            context.getString(edition.nowInRes),
            style = TextStyle(color = BeerRed, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        )
        Text(current.flag, style = TextStyle(fontSize = 34.sp))
        Text(
            current.city(locale),
            style = TextStyle(color = Brown, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        Text(
            current.beer?.name ?: context.getString(de.keinbiervorvier.common.R.string.no_local_beer),
            style = TextStyle(color = Brown, fontSize = 13.sp),
            maxLines = 1,
        )
        Text(
            context.getString(R.string.widget_next, "${next.flag} ${next.city(locale)}", nextTime),
            style = TextStyle(color = Brown, fontSize = 12.sp),
            maxLines = 1,
        )
    }
}

class BeerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BeerWidget()
}

/** Fired by [WidgetUpdates] when the next round starts. */
class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                BeerWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}

object WidgetUpdates {
    /**
     * Wakes the widget when the next round starts. An inexact one-minute window
     * is plenty and needs no exact-alarm permission.
     */
    fun scheduleAt(context: Context, at: Instant) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetUpdateReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarms.setWindow(AlarmManager.RTC, at.toEpochMilli(), WINDOW_MS, intent)
    }

    private const val WINDOW_MS = 60_000L
}
