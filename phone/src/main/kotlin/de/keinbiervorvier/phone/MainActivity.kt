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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.keinbiervorvier.common.Aleo
import de.keinbiervorvier.common.Amber
import de.keinbiervorvier.common.Cream
import de.keinbiervorvier.common.DarkAmber
import de.keinbiervorvier.common.FlagCoaster
import de.keinbiervorvier.common.Foam
import de.keinbiervorvier.common.Ink
import de.keinbiervorvier.common.InkDeep
import de.keinbiervorvier.common.InkSoft
import de.keinbiervorvier.common.beerGlass
import de.keinbiervorvier.common.formatCountdown
import de.keinbiervorvier.common.nowInRes
import de.keinbiervorvier.common.rememberUserLocation
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerSpot
import de.keinbiervorvier.core.Brewery
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

private val BeerColors = lightColorScheme(
    primary = DarkAmber,
    onPrimary = Color.White,
    secondaryContainer = Amber,
    onSecondaryContainer = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Foam,
    onSurfaceVariant = InkSoft,
)

private val CardShape = RoundedCornerShape(28.dp)

/** Aleo for everything big; body text and small labels stay in the system font. */
private val BeerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Aleo),
        displayMedium = displayMedium.copy(fontFamily = Aleo),
        displaySmall = displaySmall.copy(fontFamily = Aleo),
        headlineLarge = headlineLarge.copy(fontFamily = Aleo),
        headlineMedium = headlineMedium.copy(fontFamily = Aleo),
        headlineSmall = headlineSmall.copy(fontFamily = Aleo),
        titleLarge = titleLarge.copy(fontFamily = Aleo),
        titleMedium = titleMedium.copy(fontFamily = Aleo),
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark status bar icons on the cream background.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Cream.toArgb(), Ink.toArgb()))
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = BeerColors, typography = BeerTypography) {
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
            item {
                Column {
                    SectionTitle(stringResource(R.string.breweries_here))
                    Text(stringResource(R.string.selection_note), style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
            item { BreweryCarousel(status, locale) }
            item { SectionTitle(stringResource(R.string.coming_up)) }
            // The very next round already has its own card.
            items(upcoming.drop(1), key = { it.at }) { round -> UpcomingRow(round, locale) }
            item { AddWidgetButton() }
            item {
                Text(
                    stringResource(R.string.drink_responsibly),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
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
            modifier = Modifier.size(52.dp).clearAndSetSemantics {}, // decoration
        )
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(if (edition == Edition.KEIN_BIER_VOR_VIER) R.string.tagline_vier else R.string.tagline_five),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings), tint = InkSoft)
        }
    }
}

/** The hero: a glass of beer with the flag above the foam and the place in the beer. */
@OptIn(ExperimentalLayoutApi::class) // FlowRow
@Composable
private fun GlassCard(status: BeerStatus, edition: Edition, userLocation: GeoPoint?, locale: Locale) {
    val context = LocalContext.current
    val current = status.openNow.first()
    val distance = userLocation?.distanceKmTo(current.spot.locationOn(current.date))
        ?.let { formatDistance(it, locale) ?: stringResource(CommonR.string.you_are_there) }
    // The foam sits wherever the top part ends, so it moves down with large font sizes.
    var foamAt by remember { mutableIntStateOf(0) }
    val saying = stringResource(edition.nowInRes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, CardShape, ambientColor = DarkAmber, spotColor = DarkAmber)
            .clip(CardShape)
            .beerGlass(foamAtPx = foamAt.toFloat())
            // One unit for TalkBack: "It's 4 p.m. in, flag: Iran, Teheran, Iran, ..."
            .clickable(onClickLabel = stringResource(R.string.show_on_map)) { context.openInMaps(current.spot, current.date) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GLASS_FOAM_AT)
                .onSizeChanged { foamAt = it.height }
                .padding(top = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                saying.uppercase(locale),
                modifier = Modifier.semantics { contentDescription = saying },
                textAlign = TextAlign.Center,
                color = Ink,
                fontFamily = Aleo,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(12.dp))
            FlagCoaster(current.spot.flag)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                current.place(locale),
                fontFamily = Aleo,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            val country = current.spot.country(locale)
            Text(
                country.uppercase(locale),
                modifier = Modifier.semantics { contentDescription = country },
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp,
                color = InkDeep, // small text on the beer needs the darker grey
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Pill("⏱", stringResource(CommonR.string.since_minutes, current.minutesIn))
                Pill("📍", distance ?: stringResource(R.string.show_on_map))
            }
            val alsoNow = status.openNow.drop(1).filter { it.localTime == current.localTime }
            if (alsoNow.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.also_now), style = MaterialTheme.typography.labelMedium, color = InkDeep)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    alsoNow.forEach { other ->
                        Surface(
                            onClick = { context.openInMaps(other.spot, other.date) },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.4f),
                            contentColor = Ink,
                        ) {
                            Text(
                                "${other.spot.flag} ${other.place(locale)}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A small label on the beer; the [icon] is decoration and not read out. */
@Composable
private fun Pill(icon: String, text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.4f), contentColor = Ink) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("$icon ", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics {})
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Where it's next, how long until then, and how much of this round is gone already. */
@Composable
private fun NextRoundCard(status: BeerStatus, locale: Locale) {
    val next = status.next
    val sinceStart = Duration.ofMinutes(status.openNow.first().minutesIn.toLong())
    val progress = sinceStart.toSeconds().toFloat() / (sinceStart + next.countdown).toSeconds().coerceAtLeast(1)
    val context = LocalContext.current
    Surface(
        onClick = { next.spots.first().let { context.openInMaps(it, it.dateAt(next.at)) } },
        shape = CardShape,
        color = Foam,
        contentColor = Ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    val label = stringResource(CommonR.string.next_round)
                    Text(
                        label.uppercase(locale),
                        modifier = Modifier.semantics { contentDescription = label },
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp,
                        color = InkSoft,
                    )
                    Text(
                        next.spots.joinToString(", ") { "${it.flag} ${it.place(locale, it.dateAt(next.at))}" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatCountdown(next.countdown),
                    fontFamily = Aleo,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink,
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

/** One page of the carousel: a brewery, or the way to Google Maps for more. */
private sealed interface CarouselPage {
    data class Brewer(val spot: BeerSpot, val brewery: Brewery) : CarouselPage
    data class Nearby(val spot: BeerSpot, val place: String) : CarouselPage
}

/**
 * The breweries of the places where it is beer o'clock right now: the brewery
 * of the day first, the others from there, those of places on the same clock,
 * and at the end Google Maps for everything we don't list.
 */
private fun carouselPages(status: BeerStatus, locale: Locale): List<CarouselPage> {
    val first = status.openNow.first()
    val breweries = status.openNow.filter { it.localTime == first.localTime }
        .flatMap { open -> open.spot.breweriesFrom(open.date).map { CarouselPage.Brewer(open.spot, it) } }
    if (breweries.isEmpty()) return emptyList()
    return breweries + CarouselPage.Nearby(first.spot, first.place(locale))
}

/** Swipe through the breweries; each links to Untappd, its website and the map. */
@Composable
private fun BreweryCarousel(status: BeerStatus, locale: Locale) {
    val pages = carouselPages(status, locale)
    if (pages.isEmpty()) {
        NoBeerCard()
        return
    }
    // A new round brings new breweries: start again at the first.
    val pagerState = key(pages) { rememberPagerState(pageCount = { pages.size }) }
    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            when (val page = pages[index]) {
                is CarouselPage.Brewer -> BreweryCard(page, locale)
                is CarouselPage.Nearby -> NearbyCard(page)
            }
        }
        Text(
            stringResource(R.string.brewery_count, pagerState.currentPage + 1, pages.size),
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@Composable
private fun BreweryCard(page: CarouselPage.Brewer, locale: Locale) {
    val context = LocalContext.current
    val brewery = page.brewery
    Surface(shape = CardShape, color = Foam, contentColor = Ink, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BeerTile("🍺")
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    val town = page.spot.placeOf(brewery, locale)
                    Text(
                        "${page.spot.flag} ${town.uppercase(locale)}",
                        modifier = Modifier.semantics { contentDescription = town },
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp,
                        color = InkSoft,
                    )
                    Text(brewery.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LinkButton(stringResource(R.string.on_untappd)) { context.openOnUntappd(brewery) }
                brewery.website?.let { url -> LinkButton(stringResource(R.string.brewery_website)) { context.openWebsite(url) } }
                LinkButton(stringResource(R.string.on_map)) { context.openInMaps(page.spot, brewery) }
            }
        }
    }
}

/** The last page: we only list a few, Google Maps knows them all. */
@Composable
private fun NearbyCard(page: CarouselPage.Nearby) {
    val context = LocalContext.current
    Surface(
        onClick = { context.openBreweriesNearby(page.spot, page.place) },
        shape = CardShape,
        color = Foam,
        contentColor = Ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BeerTile("🗺")
            Column(Modifier.padding(start = 16.dp)) {
                Text(stringResource(R.string.more_nearby), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.more_nearby_text, page.place), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BeerTile(emoji: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clearAndSetSemantics {} // decoration
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Amber, DarkAmber))),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 28.sp)
    }
}

@Composable
private fun LinkButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text, fontWeight = FontWeight.Bold) }
}

/** For places without a local beer. */
@Composable
private fun NoBeerCard() {
    Surface(shape = CardShape, color = Foam, contentColor = Ink, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎒", fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics {})
            Column(Modifier.padding(start = 16.dp)) {
                Text(stringResource(CommonR.string.no_local_beer), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(CommonR.string.bring_your_own), style = MaterialTheme.typography.bodyMedium, color = InkSoft)
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
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

/** One upcoming round: your local start time in a pill, then where. */
@Composable
private fun UpcomingRow(round: NextRound, locale: Locale) {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(round.at.atZone(ZoneId.systemDefault()))
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp) // touch target
            .clip(CircleShape)
            .clickable(onClickLabel = stringResource(R.string.show_on_map)) { round.spots.first().let { context.openInMaps(it, it.dateAt(round.at)) } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = Foam, modifier = Modifier.widthIn(min = 72.dp)) {
            Text(
                time,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Text(
            round.spots.joinToString(", ") { "${it.flag} ${it.place(locale, it.dateAt(round.at))}" },
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
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Amber, contentColor = Ink),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp),
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

/** Minimum height of the cream part; it grows with large font sizes. */
private val GLASS_FOAM_AT = 170.dp
private val UPCOMING_WINDOW: Duration = Duration.ofHours(12)
