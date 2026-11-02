package de.keinbiervorvier.phone

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.keinbiervorvier.common.formatCountdown
import de.keinbiervorvier.common.nowInRes
import de.keinbiervorvier.common.rememberUserLocation
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerStatus
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.core.GeoPoint
import de.keinbiervorvier.core.NextRound
import de.keinbiervorvier.core.formatDistance
import de.keinbiervorvier.phone.widget.BeerWidget
import de.keinbiervorvier.phone.widget.BeerWidgetReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
val SoftBrown = Color(0xFF7A6650)
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
    onSurfaceVariant = SoftBrown,
)

private val CardShape = RoundedCornerShape(28.dp)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark status bar icons on the cream background.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Cream.toArgb(), Brown.toArgb()))
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = BeerColors) {
                BeerApp()
            }
        }
    }
}

@Composable
fun BeerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    var choice by remember { mutableStateOf(EditionSetting.get(context)) }
    val edition = choice ?: Edition.forLocale(locale)
    var showSettings by remember { mutableStateOf(false) }
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Header(edition, now, onSettings = { showSettings = true }) }
            item { GlassCard(status, edition, userLocation, locale) }
            item { NextRoundCard(status, locale) }
            item { BeerCard(status) }
            item { SectionTitle(stringResource(R.string.coming_up)) }
            // The very next round already has its own card.
            items(upcoming.drop(1), key = { it.at }) { round -> UpcomingRow(round, locale) }
            item { AddWidgetButton() }
            item {
                Text(
                    stringResource(R.string.drink_responsibly),
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftBrown,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            choice = choice,
            onChoice = {
                choice = it
                EditionSetting.set(context, it)
                scope.launch { BeerWidget.refreshAll(context) }
            },
            onDismiss = { showSettings = false },
        )
    }
}

/** Brand mark, title and tagline; the gear tucks the settings away. */
@Composable
private fun Header(edition: Edition, now: Instant, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MiniDial(
            digit = edition.hour - 12,
            time = now.atZone(ZoneId.systemDefault()).toLocalTime(),
            modifier = Modifier.size(52.dp),
        )
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                stringResource(if (edition == Edition.KEIN_BIER_VOR_VIER) R.string.tagline_vier else R.string.tagline_five),
                style = MaterialTheme.typography.bodyMedium,
                color = SoftBrown,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings), tint = SoftBrown)
        }
    }
}

/** The hero: a glass of beer with the flag above the foam and the place in the beer. */
@Composable
private fun GlassCard(status: BeerStatus, edition: Edition, userLocation: GeoPoint?, locale: Locale) {
    val current = status.openNow.first()
    val distance = userLocation?.distanceKmTo(current.spot.location)
        ?.let { formatDistance(it, locale) ?: stringResource(CommonR.string.you_are_there) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, CardShape, ambientColor = DarkAmber, spotColor = DarkAmber)
            .clip(CardShape)
            .beerGlass(foamAt = GLASS_FOAM_AT),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.height(GLASS_FOAM_AT).padding(top = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(edition.nowInRes).uppercase(locale),
                color = BeerRed,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(12.dp))
            FlagCoaster(current.spot.flag)
        }
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                current.spot.city(locale),
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                color = Brown,
                textAlign = TextAlign.Center,
            )
            Text(
                current.spot.country(locale).uppercase(locale),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp,
                color = Brown.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("⏱ " + stringResource(CommonR.string.since_minutes, current.minutesIn))
                distance?.let { Pill("📍 $it") }
            }
        }
    }
}

/** The flag on a round white beer mat. */
@Composable
private fun FlagCoaster(flag: String) {
    Box(
        modifier = Modifier
            .size(92.dp)
            .shadow(6.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(3.dp, Foam, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(flag, fontSize = 50.sp)
    }
}

@Composable
private fun Pill(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.4f), contentColor = Brown) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Where it's next, how long until then, and how much of this round is gone already. */
@Composable
private fun NextRoundCard(status: BeerStatus, locale: Locale) {
    val next = status.next
    val sinceStart = Duration.ofMinutes(status.openNow.first().minutesIn.toLong())
    val progress = sinceStart.toSeconds().toFloat() / (sinceStart + next.countdown).toSeconds().coerceAtLeast(1)
    Surface(shape = CardShape, color = Foam, contentColor = Brown, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(CommonR.string.next_round).uppercase(locale),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp,
                        color = SoftBrown,
                    )
                    Text(
                        next.spots.joinToString(", ") { "${it.flag} ${it.city(locale)}" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatCountdown(next.countdown),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = DarkAmber,
                )
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Amber,
                trackColor = Color.White,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
    }
}

@Composable
private fun BeerCard(status: BeerStatus) {
    val beer = status.openNow.first().spot.beer
    Surface(shape = CardShape, color = Foam, contentColor = Brown, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Amber, DarkAmber))),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (beer != null) "🍺" else "🎒", fontSize = 28.sp)
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(R.string.local_beer).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.5.sp,
                    color = SoftBrown,
                )
                Text(
                    beer?.name ?: stringResource(CommonR.string.no_local_beer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Surface(shape = CircleShape, color = Color.White) {
                    Text(
                        beer?.style ?: stringResource(CommonR.string.bring_your_own),
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftBrown,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** One upcoming round: your local start time in a pill, then where. */
@Composable
private fun UpcomingRow(round: NextRound, locale: Locale) {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(round.at.atZone(ZoneId.systemDefault()))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Foam, modifier = Modifier.width(72.dp)) {
            Text(
                time,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        Text(
            round.spots.joinToString(", ") { "${it.flag} ${it.city(locale)}" },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

@Composable
private fun AddWidgetButton() {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return
    FilledTonalButton(
        onClick = { manager.requestPinAppWidget(ComponentName(context, BeerWidgetReceiver::class.java), null, null) },
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Amber, contentColor = Brown),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
    ) {
        Text("＋  " + stringResource(R.string.add_widget), fontWeight = FontWeight.Bold)
    }
}

/** Which saying to follow; tucked away since the language default fits most people. */
@Composable
private fun SettingsDialog(choice: Edition?, onChoice: (Edition?) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        null to R.string.edition_auto,
        Edition.KEIN_BIER_VOR_VIER to R.string.edition_vier_option,
        Edition.FIVE_O_CLOCK_SOMEWHERE to R.string.edition_five_option,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.which_saying)) },
        text = {
            Column {
                options.forEach { (edition, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = choice == edition, onClick = { onChoice(edition) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == edition, onClick = null)
                        Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        containerColor = Cream,
    )
}

private val GLASS_FOAM_AT = 170.dp
private val UPCOMING_WINDOW: Duration = Duration.ofHours(12)
