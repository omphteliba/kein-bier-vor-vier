package de.keinbiervorvier.phone

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.keinbiervorvier.common.formatCountdown
import de.keinbiervorvier.common.nowInRes
import de.keinbiervorvier.common.rememberUserLocation
import de.keinbiervorvier.common.requestedEdition
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerStatus
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.core.GeoPoint
import de.keinbiervorvier.core.NextRound
import de.keinbiervorvier.core.formatDistance
import de.keinbiervorvier.phone.widget.BeerWidgetReceiver
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import de.keinbiervorvier.common.R as CommonR

val Cream = Color(0xFFFDF6E3)
val Amber = Color(0xFFF6B400)
val DarkAmber = Color(0xFFD98300)
val Brown = Color(0xFF3B2A1A)
val BeerRed = Color(0xFFC62828)
val Foam = Color(0xFFF3E7C9)

private val BeerColors = lightColorScheme(
    primary = DarkAmber,
    onPrimary = Color.White,
    secondaryContainer = Amber,
    onSecondaryContainer = Brown,
    background = Cream,
    onBackground = Brown,
    surface = Cream,
    onSurface = Brown,
    surfaceVariant = Foam,
    onSurfaceVariant = Color(0xFF6B5A45),
)

class MainActivity : ComponentActivity() {
    private var edition by mutableStateOf(Edition.KEIN_BIER_VOR_VIER)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark status bar icons on the cream background.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Cream.toArgb(), Brown.toArgb()))
        super.onCreate(savedInstanceState)
        edition = intent.requestedEdition(resources.configuration.locales[0])
        setContent {
            MaterialTheme(colorScheme = BeerColors) {
                BeerApp(edition, onEditionChange = { edition = it })
            }
        }
    }

    // singleTask: a widget tap while the app is open lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        edition = intent.requestedEdition(resources.configuration.locales[0])
    }
}

@Composable
fun BeerApp(edition: Edition, onEditionChange: (Edition) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            // Tick at the start of each minute so the countdown stays exact.
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
            now = Instant.now()
        }
    }
    val status = BeerClock.status(now, edition)
    val upcoming = remember(now, edition) { BeerClock.rounds(now, now.plus(UPCOMING_WINDOW), edition) }
    val userLocation = rememberUserLocation()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { EditionPicker(edition, onEditionChange) }
            item { NowCard(status, edition, userLocation, locale) }
            item { BeerCard(status) }
            item {
                Text(
                    stringResource(R.string.coming_up),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(upcoming, key = { it.at }) { round -> UpcomingRow(round, isNext = round == upcoming.first(), locale) }
            item { AddWidgetButton() }
        }
    }
}

@Composable
private fun EditionPicker(edition: Edition, onEditionChange: (Edition) -> Unit) {
    val labels = listOf(R.string.edition_vier, R.string.edition_five)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Edition.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == edition,
                onClick = { onEditionChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, Edition.entries.size),
            ) {
                Text(stringResource(labels[index]), maxLines = 1)
            }
        }
    }
}

/** The big card: flag, city and country on the watch face's cream-and-beer background. */
@Composable
private fun NowCard(status: BeerStatus, edition: Edition, userLocation: GeoPoint?, locale: Locale) {
    val current = status.openNow.first()
    val distance = userLocation?.distanceKmTo(current.spot.location)
        ?.let { formatDistance(it, locale) ?: stringResource(CommonR.string.you_are_there) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Cream)) {
        // Top: cream glass with the saying and the flag.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(edition.nowInRes), color = BeerRed, fontWeight = FontWeight.Bold)
            Text(current.spot.flag, fontSize = 72.sp)
        }
        // Bottom: the beer, with the place in it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Amber, DarkAmber)))
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                current.spot.city(locale),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Brown,
                textAlign = TextAlign.Center,
            )
            Text(current.spot.country(locale), style = MaterialTheme.typography.titleMedium, color = Brown)
            Text(
                listOfNotNull(stringResource(CommonR.string.since_minutes, current.minutesIn), distance).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = Brown,
            )
        }
    }
}

@Composable
private fun BeerCard(status: BeerStatus) {
    val beer = status.openNow.first().spot.beer
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Foam, contentColor = Brown)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "🍺 " + (beer?.name ?: stringResource(CommonR.string.no_local_beer)),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                beer?.style ?: stringResource(CommonR.string.bring_your_own),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One upcoming round: your local time (or the countdown for the very next one) and where. */
@Composable
private fun UpcomingRow(round: NextRound, isNext: Boolean, locale: Locale) {
    val time = if (isNext) {
        formatCountdown(round.countdown)
    } else {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(round.at.atZone(ZoneId.systemDefault()))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            time,
            modifier = Modifier.width(88.dp),
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = if (isNext) DarkAmber else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            round.spots.joinToString(", ") { "${it.flag} ${it.city(locale)}" },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddWidgetButton() {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return
    OutlinedButton(
        onClick = { manager.requestPinAppWidget(ComponentName(context, BeerWidgetReceiver::class.java), null, null) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(stringResource(R.string.add_widget))
    }
}

private val UPCOMING_WINDOW: Duration = Duration.ofHours(12)
