package app.tisimai.mektep.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts services after device reboot if child mode was active.
 *
 * Reads state from [ScreenTimePrefs] (synchronous SharedPreferences) rather than
 * DataStore, which avoids coroutine overhead in a BroadcastReceiver.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = ScreenTimePrefs(context)
        val wasChildModeActive = prefs.isChildModeActive
        val childId = prefs.activeChildId

        if (wasChildModeActive && !childId.isNullOrEmpty()) {
            // Restart screen time monitoring service with the active child
            ScreenTimeService.start(context, childId)

            // Launch the child launcher as home
            val launcherIntent = Intent(context, app.tisimai.mektep.ChildLauncherActivity::class.java)
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launcherIntent)
        }
    }
}
