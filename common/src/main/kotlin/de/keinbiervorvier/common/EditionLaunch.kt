package de.keinbiervorvier.common

import android.content.Intent
import de.keinbiervorvier.core.Edition
import java.util.Locale

/** Intent extra naming the [Edition] the app was opened for. */
const val EXTRA_EDITION = "de.keinbiervorvier.EDITION"

/**
 * Which edition to show: whatever the caller (a watch face, a complication,
 * a widget) asked for, otherwise the one matching the device language.
 * Watch faces can't add extras, so they launch an activity alias whose
 * name ends in the edition instead.
 */
fun Intent?.requestedEdition(locale: Locale): Edition {
    this?.getStringExtra(EXTRA_EDITION)?.let { name -> Edition.entries.firstOrNull { it.name == name }?.let { return it } }
    when (this?.component?.className?.substringAfterLast('.')) {
        "VierActivity" -> return Edition.KEIN_BIER_VOR_VIER
        "FiveActivity" -> return Edition.FIVE_O_CLOCK_SOMEWHERE
    }
    return Edition.forLocale(locale)
}

/** The "It's 4/5 p.m. in" heading for [edition]. */
val Edition.nowInRes: Int
    get() = when (this) {
        Edition.KEIN_BIER_VOR_VIER -> R.string.now_in_vier
        Edition.FIVE_O_CLOCK_SOMEWHERE -> R.string.now_in_five
    }
