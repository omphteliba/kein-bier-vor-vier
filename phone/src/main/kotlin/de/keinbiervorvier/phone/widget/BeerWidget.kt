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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.Image
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.keinbiervorvier.common.nowInRes
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerStatus
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.phone.EditionSetting
import de.keinbiervorvier.common.Ink
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

/** Same warm grey as in the app. */
private val InkColor = ColorProvider(Ink)

/**
 * Home screen widget: where it is beer o'clock right now and where it's next.
 * It shows the next round as a clock time rather than a countdown, so it only
 * has to change when a round starts; [WidgetUpdates] wakes it around then.
 *
 * Everything is computed inside the composition and depends on the [TICK]
 * state, because an update while a Glance session is still alive only
 * recomposes - it does not run [provideGlance] again.
 */
class BeerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            currentState<Preferences>()[TICK] // recompose whenever refreshAll() bumps it
            val locale = context.resources.configuration.locales[0]
            val edition = EditionSetting.effective(context, locale)
            val status = BeerClock.status(Instant.now(), edition)
            WidgetUpdates.scheduleAt(context, status.next.at)
            WidgetContent(context, status, edition, locale)
        }
    }

    companion object {
        private val TICK = longPreferencesKey("tick")

        /** Recomputes every placed widget, whether or not its session is still alive. */
        suspend fun refreshAll(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(BeerWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { it[TICK] = System.currentTimeMillis() }
                BeerWidget().update(context, id)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, status: BeerStatus, edition: Edition, locale: Locale) {
    val current = status.openNow.first().spot
    val next = status.next.spots.first()
    val nextTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(status.next.at.atZone(ZoneId.systemDefault()))
    val open = Intent(context, MainActivity::class.java)

    val nextLine = context.getString(R.string.widget_next, "${next.flag} ${next.city(locale)}", nextTime)
    // Read as one sentence by TalkBack instead of piece by piece.
    val description = listOfNotNull(
        context.getString(edition.nowInRes),
        "${current.city(locale)}, ${current.country(locale)}",
        current.beer?.name,
        nextLine,
    ).joinToString(". ")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(open))
            .semantics { contentDescription = description },
    ) {
        // Top: cream glass with the saying and the flag.
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                .background(ImageProvider(R.drawable.widget_top)).padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                context.getString(edition.nowInRes).uppercase(locale),
                style = TextStyle(color = InkColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Box(
                modifier = GlanceModifier.size(58.dp).background(ImageProvider(R.drawable.widget_coaster)),
                contentAlignment = Alignment.Center,
            ) {
                Text(current.flag, style = TextStyle(fontSize = 30.sp))
            }
        }
        Image(
            provider = ImageProvider(R.drawable.widget_foam),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxWidth().height(22.dp).background(ImageProvider(R.drawable.widget_top_flat)),
        )
        // Bottom: the beer, with the place in it.
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                .background(ImageProvider(R.drawable.widget_beer)).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current.city(locale),
                style = TextStyle(color = InkColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
            )
            Text(
                current.beer?.name ?: context.getString(de.keinbiervorvier.common.R.string.no_local_beer),
                style = TextStyle(color = InkColor, fontSize = 13.sp),
                maxLines = 1,
            )
            Text(
                nextLine,
                style = TextStyle(color = InkColor, fontSize = 12.sp),
                maxLines = 1,
            )
        }
    }
}

class BeerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BeerWidget()
}

/** Fired by [WidgetUpdates] when the next round starts, and after app updates. */
class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                BeerWidget.refreshAll(context)
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
