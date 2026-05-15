package app.tisimai.mektep.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Admin receiver — prevents the app from being uninstalled
 * while child mode is active.
 *
 * The parent enables Device Admin during setup.
 * If someone tries to disable it, they see a warning.
 */
class MektepDeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Device admin enabled
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling will remove Mektep's parental controls. Your child will be able to uninstall the app."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin disabled
    }
}
