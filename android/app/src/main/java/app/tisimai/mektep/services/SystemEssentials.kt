package app.tisimai.mektep.services

/**
 * Defines apps that must NEVER be blocked or counted toward screen time.
 * Phone calls, messaging, camera, system UI — always accessible regardless of settings.
 */
object SystemEssentials {

    private val exactPackages = setOf(
        // Our app
        "app.tisimai.mektep",

        // Phone / Dialer
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",

        // Contacts
        "com.android.contacts",
        "com.samsung.android.contacts",
        "com.google.android.contacts",

        // Messaging / SMS
        "com.android.messaging",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",

        // Camera
        "com.android.camera",
        "com.android.camera2",
        "com.samsung.android.camera",
        "com.sec.android.app.camera",
        "com.google.android.GoogleCamera",

        // System core
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.android.settings",
        "com.android.emergency",

        // Play Services / Store
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",

        // Utilities
        "com.android.calculator2",
        "com.google.android.calculator",
        "com.android.deskclock",
        "com.google.android.deskclock",

        // Xiaomi / MIUI specific
        "com.miui.home",
        "com.xiaomi.discover",
        "com.miui.securitycenter",
        "com.android.thememanager"
    )

    private val prefixes = setOf(
        "com.android.providers.",
        "com.android.inputmethod.",
        "com.google.android.inputmethod.",
        "com.samsung.android.inputmethod.",
        "com.android.server.",
        "com.android.internal."
    )

    /** Returns true if this package should NEVER be blocked or counted */
    fun isSystemEssential(packageName: String): Boolean {
        if (packageName in exactPackages) return true
        return prefixes.any { packageName.startsWith(it) }
    }

    /** Returns true if this app should count down the screen time balance */
    fun isCountedApp(packageName: String, needsEarnedTimePackages: Set<String>): Boolean {
        if (isSystemEssential(packageName)) return false
        return packageName in needsEarnedTimePackages
    }
}
