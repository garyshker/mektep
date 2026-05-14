package com.mektep.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mektep.app.MainActivity

/**
 * Accessibility Service that detects when a blocked app opens.
 *
 * This is more reliable than UsageStatsManager for real-time detection.
 * When a blocked app is detected and screen time balance is zero,
 * it immediately redirects to the Mektep app.
 *
 * User must enable this in:
 * Settings > Accessibility > Mektep
 *
 * Note: This approach is used by many parental control apps.
 * Google Play may review apps using AccessibilityService more carefully.
 */
class AppBlockerService : AccessibilityService() {

    companion object {
        var isRunning = false
            private set

        var screenTimeBalance: Int = 0
        var blockedPackages: MutableSet<String> = mutableSetOf()
        var allowedPackages: MutableSet<String> = mutableSetOf(
            "com.mektep.app",
            "com.android.dialer",
            "com.android.contacts",
            "com.android.messaging",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.settings"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d("AppBlocker", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip allowed apps
        if (packageName in allowedPackages) return

        // If no blocked list configured, block everything not in allowed
        val shouldBlock = if (blockedPackages.isNotEmpty()) {
            packageName in blockedPackages
        } else {
            true // block all non-allowed by default
        }

        if (shouldBlock && screenTimeBalance <= 0) {
            Log.d("AppBlocker", "Blocking $packageName - no screen time")
            redirectToMektep()
        }
    }

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    private fun redirectToMektep() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("blocked", true)
        }
        startActivity(intent)
    }
}
