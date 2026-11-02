package de.keinbiervorvier.common

import java.time.Duration

/** "12 min" or "1 h 5 min", rounded up so it never says 0 before the round starts. */
fun formatCountdown(d: Duration): String {
    val minutes = (d.toSeconds() + 59) / 60
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}
