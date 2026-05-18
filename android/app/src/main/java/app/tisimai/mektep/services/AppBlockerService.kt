package app.tisimai.mektep.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import app.tisimai.mektep.BlockingOverlayActivity

/**
 * Accessibility service that detects foreground app changes and enforces screen time.
 *
 * Communicates with [ScreenTimeService] through [ScreenTimePrefs]:
 * - Writes [ScreenTimePrefs.currentForegroundPackage] and [ScreenTimePrefs.isForegroundCounted]
 *   so the tick loop knows whether to decrement the balance.
 * - Reads [ScreenTimePrefs.balanceSeconds] / [ScreenTimePrefs.isChildModeActive] to decide
 *   whether to launch the blocking overlay immediately.
 *
 * Does NOT use Hilt -- accessibility services are instantiated by the system, outside the
 * Hilt component graph.  All shared state goes through [ScreenTimePrefs].
 */
class AppBlockerService : AccessibilityService() {

    private lateinit var prefs: ScreenTimePrefs

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = ScreenTimePrefs(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // System-essential apps are never counted and never blocked.
        if (SystemEssentials.isSystemEssential(packageName)) {
            prefs.isForegroundCounted = false
            return
        }

        prefs.currentForegroundPackage = packageName

        val needsEarnedTime = prefs.needsEarnedTimePackages

        if (packageName in needsEarnedTime) {
            prefs.isForegroundCounted = true

            if (prefs.balanceSeconds <= 0 && !prefs.isOverlayShowing && prefs.isChildModeActive) {
                val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                prefs.isOverlayShowing = true
            }
        } else {
            prefs.isForegroundCounted = false
        }
    }

    override fun onInterrupt() {
        // Required override -- nothing to clean up.
    }
}
