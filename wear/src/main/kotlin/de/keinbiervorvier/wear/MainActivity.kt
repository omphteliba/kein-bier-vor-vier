package de.keinbiervorvier.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import de.keinbiervorvier.common.InkLight
import de.keinbiervorvier.common.InkLightSoft
import de.keinbiervorvier.common.Night
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
 * The glass, watch edition: a dark top (kind to OLED) with the saying, the flag
 * on its beer mat and the place; foam; then the local beer, in the beer.
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
        modifier = Modifier.fillMaxSize().beerGlass(foamAtPx = foamAt.toFloat(), top = Night, bubbleScale = 0.55f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.64f)
                .onSizeChanged { foamAt = it.height }
                .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            val saying = stringResource(edition.nowInRes)
            Text(
                saying.uppercase(locale),
                modifier = Modifier.semantics { contentDescription = saying },
                fontFamily = Aleo,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 1.sp,
                color = InkLightSoft,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            if (compact) {
                FlagCoaster(current.spot.flag, size = 30.dp, flagSize = 16.sp)
            } else {
                FlagCoaster(current.spot.flag, size = 36.dp, flagSize = 20.sp)
            }
            Text(
                current.spot.city(locale),
                fontFamily = Aleo,
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 18.sp else 21.sp,
                color = InkLight,
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
                color = InkLightSoft,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(0.36f).padding(start = 36.dp, end = 36.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                beer?.name ?: stringResource(CommonR.string.no_local_beer),
                fontFamily = Aleo,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 13.sp else 15.sp,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                beer?.style ?: stringResource(CommonR.string.bring_your_own),
                fontSize = 10.sp,
                color = Ink,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            val nextLine = stringResource(
                CommonR.string.next_in,
                "${next.flag} ${next.city(locale)}",
                formatCountdown(status.next.countdown),
            )
            Text(
                "→ $nextLine",
                // The arrow stands in for "next round", which TalkBack still says.
                modifier = Modifier.semantics {
                    contentDescription = "${context.getString(CommonR.string.next_round)}: $nextLine"
                },
                fontSize = 10.sp,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
