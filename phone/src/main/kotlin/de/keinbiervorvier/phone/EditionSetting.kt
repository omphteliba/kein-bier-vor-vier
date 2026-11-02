package de.keinbiervorvier.phone

import android.content.Context
import de.keinbiervorvier.core.Edition
import java.util.Locale

/** The user's choice of saying, shared by the app and the widget. Null means "follow the language". */
object EditionSetting {
    private const val PREFS = "settings"
    private const val KEY = "edition"

    fun get(context: Context): Edition? =
        prefs(context).getString(KEY, null)?.let { name -> Edition.entries.firstOrNull { it.name == name } }

    fun set(context: Context, edition: Edition?) {
        prefs(context).edit().apply { if (edition == null) remove(KEY) else putString(KEY, edition.name) }.apply()
    }

    /** The edition to show: the stored choice, or the one matching the device language. */
    fun effective(context: Context, locale: Locale = context.resources.configuration.locales[0]): Edition =
        get(context) ?: Edition.forLocale(locale)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
