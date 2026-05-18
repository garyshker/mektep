package app.tisimai.mektep.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.tisimai.mektep.BlockingOverlayActivity
import app.tisimai.mektep.MainActivity
import app.tisimai.mektep.R
import app.tisimai.mektep.data.local.AllowedAppDao
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.ChildSessionDao
import app.tisimai.mektep.data.local.ScreenTimeDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that ticks the screen-time balance once per second.
 *
 * Relies on [AppBlockerService] writing [ScreenTimePrefs.isForegroundCounted] so
 * this service knows whether the child is actively using a counted app.
 *
 * Room DAOs are obtained via a Hilt [EntryPoint] because plain [Service] classes
 * are not part of the Hilt component graph.
 */
class ScreenTimeService : Service() {

    // ── Hilt EntryPoint for Room DAOs ──

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun childProfileDao(): ChildProfileDao
        fun allowedAppDao(): AllowedAppDao
        fun screenTimeDao(): ScreenTimeDao
        fun childSessionDao(): ChildSessionDao
    }

    companion object {
        private const val CHANNEL_ID = "screen_time_channel"
        private const val NOTIFICATION_ID = 2001
        private const val EXTRA_CHILD_ID = "child_id"

        fun start(context: Context, childId: String) {
            val intent = Intent(context, ScreenTimeService::class.java).apply {
                putExtra(EXTRA_CHILD_ID, childId)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenTimeService::class.java))
        }
    }

    private lateinit var prefs: ScreenTimePrefs
    private lateinit var childProfileDao: ChildProfileDao
    private lateinit var allowedAppDao: AllowedAppDao
    private lateinit var screenTimeDao: ScreenTimeDao
    private lateinit var childSessionDao: ChildSessionDao

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var childId: String = ""

    // ── Lifecycle ──

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        childId = intent?.getStringExtra(EXTRA_CHILD_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        prefs = ScreenTimePrefs(this)

        // Obtain DAOs via Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ServiceEntryPoint::class.java
        )
        childProfileDao = entryPoint.childProfileDao()
        allowedAppDao = entryPoint.allowedAppDao()
        screenTimeDao = entryPoint.screenTimeDao()
        childSessionDao = entryPoint.childSessionDao()

        // Blocking initialisation -- runs once, quickly
        runBlocking {
            // Load the child's current balance into prefs
            val child = childProfileDao.getChild(childId)
            prefs.balanceSeconds = child?.screenTimeBalanceSecs ?: 0

            // Partition allowed apps into "needs earned time" vs "always allowed"
            val apps = allowedAppDao.getAppsForConfigOnce(childId)
            val needsEarned = mutableSetOf<String>()
            val alwaysAllowed = mutableSetOf<String>()
            for (app in apps) {
                if (app.needsEarnedTime) {
                    needsEarned.add(app.packageName)
                } else {
                    alwaysAllowed.add(app.packageName)
                }
            }
            prefs.needsEarnedTimePackages = needsEarned
            prefs.alwaysAllowedPackages = alwaysAllowed
        }

        prefs.activeChildId = childId
        prefs.isChildModeActive = true

        createNotificationChannel()
        val notification = buildNotification(prefs.balanceSeconds)
        startForeground(NOTIFICATION_ID, notification)

        startTickLoop()

        return START_STICKY
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    // ── Tick loop ──

    private fun startTickLoop() {
        serviceScope.launch {
            while (isActive) {
                delay(1000)

                if (!prefs.isForegroundCounted) continue // not a counted app -- skip

                val newBalance = prefs.balanceSeconds - 1
                prefs.balanceSeconds = newBalance
                updateNotification(newBalance)

                // Persist to DB every 10 seconds to reduce disk I/O
                if (newBalance % 10 == 0) {
                    childProfileDao.updateScreenTimeBalance(childId, -10)
                }

                // Time's up -- show blocking overlay if not already showing
                if (newBalance <= 0 && !prefs.isOverlayShowing) {
                    launchOverlay()
                    prefs.isOverlayShowing = true
                }
            }
        }
    }

    // ── Stop / cleanup ──

    private fun stop() {
        serviceScope.cancel()

        // Persist whatever balance remains
        val finalBalance = prefs.balanceSeconds
        runBlocking {
            val child = childProfileDao.getChild(childId)
            if (child != null) {
                val delta = finalBalance - child.screenTimeBalanceSecs
                if (delta != 0) {
                    childProfileDao.updateScreenTimeBalance(childId, delta)
                }
            }
        }

        prefs.isChildModeActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Overlay ──

    private fun launchOverlay() {
        val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    // ── Notification ──

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows remaining screen time"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(balanceSeconds: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (balanceSeconds > 0) {
            val minutes = balanceSeconds / 60
            "$minutes min remaining"
        } else {
            "Screen time used up"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BilimALL")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(balanceSeconds: Int) {
        val notification = buildNotification(balanceSeconds)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
