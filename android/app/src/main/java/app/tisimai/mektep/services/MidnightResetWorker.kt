package app.tisimai.mektep.services

import android.content.Context
import androidx.room.Room
import androidx.work.*
import app.tisimai.mektep.data.local.MektepDatabase
import app.tisimai.mektep.data.models.ScreenTimeLog
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that resets daily screen time at midnight.
 * Also cleans up old quest data.
 */
class MidnightResetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(applicationContext, MektepDatabase::class.java, "mektep.db")
            .addMigrations(MektepDatabase.MIGRATION_1_2, MektepDatabase.MIGRATION_2_3)
            .build()

        try {
            // Clean old quests (older than today)
            val today = LocalDate.now().toString()
            db.questDao().clearOldQuests(today)

            // Log the daily reset
            db.screenTimeDao().addLog(
                ScreenTimeLog(type = "DAILY_RESET", amountSeconds = 0, source = "midnight_worker")
            )
        } finally {
            db.close()
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "mektep_midnight_reset"

        fun schedule(context: Context) {
            // Calculate delay until next midnight
            val now = java.time.LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delayMinutes = java.time.Duration.between(now, nextMidnight).toMinutes()

            val request = PeriodicWorkRequestBuilder<MidnightResetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
