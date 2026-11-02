package de.keinbiervorvier.wear.complication

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

/**
 * Asks watch faces for fresh complication data. Needed after an app update:
 * Android cancels the old tap PendingIntents, so tapping the complication
 * would do nothing until the next scheduled update.
 */
fun Context.refreshComplications() {
    listOf(KeinBierVorVierComplicationService::class.java, FiveOClockComplicationService::class.java).forEach {
        ComplicationDataSourceUpdateRequester.create(this, ComponentName(this, it)).requestUpdateAll()
    }
}

/** Refreshes complications right after this app was updated. */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) context.refreshComplications()
    }
}
