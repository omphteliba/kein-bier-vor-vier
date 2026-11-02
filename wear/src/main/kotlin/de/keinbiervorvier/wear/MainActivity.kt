package de.keinbiervorvier.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerStatus
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.core.GeoPoint
import de.keinbiervorvier.core.formatDistance
import de.keinbiervorvier.common.Aleo
import de.keinbiervorvier.common.FlagCoaster
import de.keinbiervorvier.common.Ink
import de.keinbiervorvier.common.FoamWhite
import de.keinbiervorvier.common.InkDeep
import de.keinbiervorvier.common.R as CommonR
import de.keinbiervorvier.common.beerGlass
import de.keinbiervorvier.common.formatCountdown
import de.keinbiervorvier.common.nowInRes
import de.keinbiervorvier.common.requestedEdition
import de.keinbiervorvier.common.rememberUserLocation
import de.keinbiervorvier.wear.complication.refreshComplications
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var edition by mutableStateOf(Edition.KEIN_BIER_VOR_VIER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshComplications()
        edition = intent.requestedEdition(resources.configuration.locales[0])
        setContent {
            MaterialTheme {
                AppScaffold {
                    ScreenScaffold {
                        BeerScreen(edition)
                    }
                }
            }
        }
    }

    // singleTask: a tap on a watch face while the app is open lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        edition = intent.requestedEdition(resources.configuration.locales[0])
    }
}

@Composable
fun BeerScreen(edition: Edition) {
    val locale = LocalConfiguration.current.locales[0]
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            // Tick at the start of each minute so the countdown stays exact.
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
            now = Instant.now()
        }
    }
    BeerStatusView(BeerClock.status(now, edition), edition, rememberUserLocation(), locale)
}

/**
 * The glass, watch edition: looking into a full glass. Foam across the top
 * (where the time sits) with the saying, then beer with the flag on its mat,
 * the place, the local beer and the next round.
 */
@Composable
fun BeerStatusView(status: BeerStatus, edition: Edition, userLocation: GeoPoint?, locale: Locale) {
    val context = LocalContext.current
    val current = status.openNow.first()
    val distance = userLocation?.distanceKmTo(current.spot.location)
        ?.let { formatDistance(it, locale) ?: stringResource(CommonR.string.you_are_there) }
    val next = status.next.spots.first()
    val beer = current.spot.beer
    var foamAt by remember { mutableIntStateOf(0) }
    // Small round watches (192 dp) get a smaller flag and type so nothing is cut off.
    val compact = LocalConfiguration.current.screenHeightDp < 210

    Column(
        modifier = Modifier.fillMaxSize().beerGlass(foamAtPx = foamAt.toFloat(), top = FoamWhite, foamScale = 1.3f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The foam: room for the system clock, then the saying.
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.27f).onSizeChanged { foamAt = it.height },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val saying = stringResource(edition.nowInRes)
            Text(
                saying.uppercase(locale),
                modifier = Modifier.padding(bottom = 6.dp).semantics { contentDescription = saying },
                fontFamily = Aleo,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 1.sp,
                color = Ink,
                maxLines = 1,
            )
        }
        // The beer.
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 28.dp, end = 28.dp, top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (compact) {
                FlagCoaster(current.spot.flag, size = 30.dp, flagSize = 16.sp)
            } else {
                FlagCoaster(current.spot.flag, size = 38.dp, flagSize = 21.sp)
            }
            Text(
                current.spot.city(locale),
                fontFamily = Aleo,
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 19.sp else 22.sp,
                color = InkDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    current.spot.country(locale),
                    stringResource(CommonR.string.since_minutes, current.minutesIn),
                    distance,
                ).joinToString(" · "),
                fontSize = 10.sp,
                color = InkDeep,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            Text(
                listOfNotNull(beer?.name ?: stringResource(CommonR.string.no_local_beer), beer?.style).joinToString(" · "),
                fontFamily = Aleo,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 12.sp else 13.sp,
                color = InkDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val nextLine = stringResource(
                CommonR.string.next_in,
                "${next.flag} ${next.city(locale)}",
                formatCountdown(status.next.countdown),
            )
            Text(
                "→ $nextLine",
                // The arrow stands in for "next round", which TalkBack still says.
                modifier = Modifier.padding(horizontal = 8.dp).semantics {
                    contentDescription = "${context.getString(CommonR.string.next_round)}: $nextLine"
                },
                fontSize = 10.sp,
                color = InkDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
