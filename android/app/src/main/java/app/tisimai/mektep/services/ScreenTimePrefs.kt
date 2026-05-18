package app.tisimai.mektep.services

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences bridge for cross-service communication.
 * Both AppBlockerService and ScreenTimeService read/write here.
 * Uses plain SharedPreferences (not DataStore) for synchronous access in services.
 */
class ScreenTimePrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mektep_screen_time_service", Context.MODE_PRIVATE)

    var currentForegroundPackage: String
        get() = prefs.getString("foreground_package", "") ?: ""
        set(value) = prefs.edit().putString("foreground_package", value).apply()

    var isForegroundCounted: Boolean
        get() = prefs.getBoolean("foreground_counted", false)
        set(value) = prefs.edit().putBoolean("foreground_counted", value).apply()

    var balanceSeconds: Int
        get() = prefs.getInt("balance_seconds", 0)
        set(value) = prefs.edit().putInt("balance_seconds", value).apply()

    var activeChildId: String?
        get() = prefs.getString("active_child_id", null)
        set(value) = prefs.edit().putString("active_child_id", value).apply()

    var isChildModeActive: Boolean
        get() = prefs.getBoolean("child_mode_active", false)
        set(value) = prefs.edit().putBoolean("child_mode_active", value).apply()

    var isOverlayShowing: Boolean
        get() = prefs.getBoolean("overlay_showing", false)
        set(value) = prefs.edit().putBoolean("overlay_showing", value).apply()

    var needsEarnedTimePackages: Set<String>
        get() = prefs.getStringSet("needs_earned_time_packages", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("needs_earned_time_packages", value).apply()

    var alwaysAllowedPackages: Set<String>
        get() = prefs.getStringSet("always_allowed_packages", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("always_allowed_packages", value).apply()

    fun clear() = prefs.edit().clear().apply()
}
