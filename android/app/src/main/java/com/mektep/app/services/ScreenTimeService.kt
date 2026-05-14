package com.mektep.app.services

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mektep.app.MainActivity
import com.mektep.app.R
import java.util.Timer
import java.util.TimerTask

/**
 * Foreground service that monitors screen time usage.
 *
 * When the child opens a blocked app and has no screen time balance,
 * it launches an overlay activity prompting them to earn more time.
 *
 * Requires:
 * - PACKAGE_USAGE_STATS permission (granted via Settings > Usage Access)
 * - FOREGROUND_SERVICE permission
 * - SYSTEM_ALERT_WINDOW permission for overlay
 */
class ScreenTimeService : Service() {

    companion object {
        const val CHANNEL_ID = "mektep_screen_time"
        const val NOTIFICATION_ID = 1001
        const val CHECK_INTERVAL_MS = 5000L

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenTimeService::class.java))
        }
    }

    private var timer: Timer? = null
    private var balanceSeconds: Int = 0
    private var isTracking: Boolean = false
    private var blockedPackages: Set<String> = emptySet()
    private var allowedPackages: Set<String> = setOf(
        "com.mektep.app",
        "com.android.dialer",
        "com.android.contacts",
        "com.android.messaging",
        "com.google.android.dialer",
        "com.samsung.android.dialer"
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Monitoring screen time")
        startForeground(NOTIFICATION_ID, notification)
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkForegroundApp()
            }
        }, 0, CHECK_INTERVAL_MS)
    }

    private fun checkForegroundApp() {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10000, now)

            if (stats.isNullOrEmpty()) return

            val currentApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return

            // Skip if it's our app or an allowed app
            if (currentApp in allowedPackages) {
                if (isTracking) {
                    isTracking = false
                    updateNotification("Monitoring screen time")
                }
                return
            }

            // Check if app is blocked (or all non-allowed apps are blocked by default)
            if (blockedPackages.isNotEmpty() && currentApp !in blockedPackages) {
                return
            }

            if (balanceSeconds <= 0) {
                // No balance - show block overlay
                showBlockOverlay()
                return
            }

            // Deduct time
            if (!isTracking) {
                isTracking = true
            }

            balanceSeconds -= (CHECK_INTERVAL_MS / 1000).toInt()
            val minutes = balanceSeconds / 60
            updateNotification("Screen time: ${minutes}m remaining")

            if (balanceSeconds <= 0) {
                showBlockOverlay()
            }
        } catch (e: Exception) {
            Log.e("ScreenTimeService", "Error checking foreground app", e)
        }
    }

    private fun showBlockOverlay() {
        // Launch Mektep app with a "time's up" message
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("times_up", true)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors screen time usage"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mektep")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
