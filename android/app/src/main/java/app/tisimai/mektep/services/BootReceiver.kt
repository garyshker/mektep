package app.tisimai.mektep.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.parentalDataStore by preferencesDataStore(name = "mektep_parental")

/**
 * Restarts services after device reboot if child mode was active.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val wasChildModeActive = runBlocking {
            context.parentalDataStore.data.map {
                it[booleanPreferencesKey("child_mode_active")] ?: false
            }.firstOrNull() ?: false
        }

        if (wasChildModeActive) {
            // Restart screen time monitoring service
            ScreenTimeService.start(context)

            // Launch the child launcher as home
            val launcherIntent = Intent(context, app.tisimai.mektep.ChildLauncherActivity::class.java)
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launcherIntent)
        }
    }
}
