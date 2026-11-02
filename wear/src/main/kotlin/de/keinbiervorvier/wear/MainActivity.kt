package de.keinbiervorvier.wear

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
import de.keinbiervorvier.core.GeoPoint
import de.keinbiervorvier.core.formatDistance
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScaffold {
                    ScreenScaffold {
                        BeerScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun BeerScreen() {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            // Tick at the start of each minute so the countdown stays exact.
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
            now = Instant.now()
        }
    }
    BeerStatusView(BeerClock.status(now), rememberUserLocation())
}

@Composable
fun BeerStatusView(status: BeerStatus, userLocation: GeoPoint?) {
    val current = status.openNow.first()
    val distanceKm = userLocation?.distanceKmTo(current.spot.location)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🍺 Gerade 16 Uhr in", style = MaterialTheme.typography.labelMedium)
        Text(
            buildAnnotatedString {
                append("${current.spot.flag} ${current.spot.city}")
                withStyle(
                    MaterialTheme.typography.bodyMedium.toSpanStyle()
                        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    append(", ${current.spot.country}")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            listOfNotNull(
                "seit ${current.minutesIn} min",
                distanceKm?.let { formatDistance(it) ?: "Du bist da! 🍻" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        val beer = current.spot.beer
        Text(
            beer?.name ?: "Kein lokales Bier",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            beer?.style ?: "Selbst mitbringen 🎒",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text("Nächste Runde", style = MaterialTheme.typography.labelSmall)
        val next = status.next.spots.first()
        Text(
            "${next.flag} ${next.city} in ${formatCountdown(status.next.countdown)}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatCountdown(d: Duration): String {
    val minutes = (d.toSeconds() + 59) / 60
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}
