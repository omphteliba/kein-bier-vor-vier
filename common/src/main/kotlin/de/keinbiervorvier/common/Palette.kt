package de.keinbiervorvier.common

import androidx.compose.ui.graphics.Color

// The beer glass colours, shared by phone and watch.
val Cream = Color(0xFFFDF6E3)
val Foam = Color(0xFFF3E7C9)
val Amber = Color(0xFFF6B400)
val DarkAmber = Color(0xFFD98300)
val BeerRed = Color(0xFFC62828)

/** Text: warm grey, 90 % neutral grey with 10 % golden yellow (#FFD700) mixed in. */
val Ink = Color(0xFF595540)

/** Secondary text, same mix on a lighter grey: 5.3:1 on cream, 4.6:1 on [Foam] (WCAG AA). */
val InkSoft = Color(0xFF6B6756)

/** Dark glass for the watch: OLED-friendly, a hint of brown. */
val Night = Color(0xFF15120C)

/** Text on [Night]: the same 10 % gold mix on a light grey (#E0E0E0), 14:1. */
val InkLight = Color(0xFFE3DFCA)

/** Secondary text on [Night]: the mix on #A8A8A8, 8:1. */
val InkLightSoft = Color(0xFFB1AD97)
