package de.keinbiervorvier.common

import androidx.compose.ui.graphics.Color

// The beer glass colours, shared by phone and watch.
val Cream = Color(0xFFFDF6E3)
val Foam = Color(0xFFF3E7C9)

/** The foam itself: a touch whiter than [Cream]. */
val FoamWhite = Color(0xFFFFFCF2)
val Amber = Color(0xFFF6B400)
val DarkAmber = Color(0xFFD98300)
val BeerRed = Color(0xFFC62828)

/** Text: warm grey, 90 % neutral grey with 10 % golden yellow (#FFD700) mixed in. */
val Ink = Color(0xFF595540)

/** Secondary text, same mix on a lighter grey: 5.3:1 on cream, 4.6:1 on [Foam] (WCAG AA). */
val InkSoft = Color(0xFF6B6756)

/** Small text on the beer: the mix on #303030, 5.6:1 on [Amber] (WCAG AA). */
val InkDeep = Color(0xFF45412B)
