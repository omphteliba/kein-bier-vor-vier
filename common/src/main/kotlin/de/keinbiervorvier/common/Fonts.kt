package de.keinbiervorvier.common

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/** Aleo, a soft slab serif (OFL), bundled as one variable font file. */
@OptIn(ExperimentalTextApi::class) // variable font axes
val Aleo: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black)
        .map { weight ->
            Font(R.font.aleo, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        },
)
