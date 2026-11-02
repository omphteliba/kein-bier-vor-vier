package de.keinbiervorvier.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import de.keinbiervorvier.core.BeerClock
import de.keinbiervorvier.core.BeerStatus
import de.keinbiervorvier.core.Edition
import de.keinbiervorvier.core.GeoPoint
import de.keinbiervorvier.core.formatDistance
import de.keinbiervorvier.common.R as CommonR
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

@Composable
fun BeerStatusView(status: BeerStatus, edition: Edition, userLocation: GeoPoint?, locale: Locale) {
    val current = status.openNow.first()
    val distanceKm = userLocation?.distanceKmTo(current.spot.location)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(edition.nowInRes), style = MaterialTheme.typography.labelMedium)
        Text(
            buildAnnotatedString {
                append("${current.spot.flag} ${current.spot.city(locale)}")
                withStyle(
                    MaterialTheme.typography.bodyMedium.toSpanStyle()
                        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    append(", ${current.spot.country(locale)}")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            listOfNotNull(
                stringResource(CommonR.string.since_minutes, current.minutesIn),
                distanceKm?.let { formatDistance(it, locale) ?: stringResource(CommonR.string.you_are_there) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        val beer = current.spot.beer
        Text(
            beer?.name ?: stringResource(CommonR.string.no_local_beer),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            beer?.style ?: stringResource(CommonR.string.bring_your_own),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(CommonR.string.next_round), style = MaterialTheme.typography.labelSmall)
        val next = status.next.spots.first()
        Text(
            stringResource(CommonR.string.next_in, "${next.flag} ${next.city(locale)}", formatCountdown(status.next.countdown)),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
