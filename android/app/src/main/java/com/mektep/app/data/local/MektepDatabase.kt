package com.mektep.app.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mektep.app.data.models.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        UserProfile::class,
        LessonProgress::class,
        ScreenTimeLog::class,
        DailyQuest::class,
        ParentalConfig::class,
        AllowedApp::class,
        ChildSession::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MektepDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun progressDao(): ProgressDao
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun parentalConfigDao(): ParentalConfigDao
    abstract fun allowedAppDao(): AllowedAppDao
    abstract fun childSessionDao(): ChildSessionDao
    abstract fun questDao(): QuestDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS parental_config (
                        id TEXT NOT NULL PRIMARY KEY,
                        mode TEXT NOT NULL DEFAULT 'NONE',
                        pinHash TEXT NOT NULL DEFAULT '',
                        pinSalt TEXT NOT NULL DEFAULT '',
                        childModeActive INTEGER NOT NULL DEFAULT 0,
                        dailyLimitMinutes INTEGER NOT NULL DEFAULT 60,
                        bedtimeStart TEXT,
                        bedtimeEnd TEXT,
                        familyId TEXT,
                        inviteCode TEXT,
                        lastSyncedAt INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS allowed_app (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        configId TEXT NOT NULL DEFAULT 'local',
                        packageName TEXT NOT NULL,
                        appLabel TEXT NOT NULL,
                        needsEarnedTime INTEGER NOT NULL DEFAULT 1,
                        dailyLimitMinutes INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS child_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startedAt INTEGER NOT NULL DEFAULT 0,
                        endedAt INTEGER,
                        initialBalanceSecs INTEGER NOT NULL DEFAULT 0,
                        consumedSecs INTEGER NOT NULL DEFAULT 0,
                        endReason TEXT
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_quest (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        targetValue INTEGER NOT NULL,
                        currentValue INTEGER NOT NULL DEFAULT 0,
                        xpReward INTEGER NOT NULL DEFAULT 10,
                        completed INTEGER NOT NULL DEFAULT 0,
                        date TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

// ── Existing DAOs ──

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfileOnce(): UserProfile?

    @Upsert
    suspend fun upsertProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET xpTotal = xpTotal + :xp WHERE id = :userId")
    suspend fun addXp(userId: String, xp: Int)

    @Query("UPDATE user_profile SET currentStreak = :streak, longestStreak = MAX(longestStreak, :streak), lastActiveDate = :date WHERE id = :userId")
    suspend fun updateStreak(userId: String, streak: Int, date: String)

    @Query("UPDATE user_profile SET screenTimeBalanceSecs = screenTimeBalanceSecs + :deltaSecs WHERE id = :userId")
    suspend fun updateScreenTimeBalance(userId: String, deltaSecs: Int)

    @Query("UPDATE user_profile SET language = :lang WHERE id = :userId")
    suspend fun updateLanguage(userId: String, lang: String)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress")
    fun getAll(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getForLesson(lessonId: String): LessonProgress?

    @Query("SELECT * FROM lesson_progress WHERE subjectId = :subjectId")
    suspend fun getForSubject(subjectId: String): List<LessonProgress>

    @Upsert
    suspend fun upsert(progress: LessonProgress)
}

@Dao
interface ScreenTimeDao {
    @Insert
    suspend fun addLog(log: ScreenTimeLog)

    @Query("SELECT * FROM screen_time_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<ScreenTimeLog>

    @Query("SELECT COALESCE(SUM(amountSeconds), 0) FROM screen_time_log WHERE type = 'EARNED'")
    suspend fun getTotalEarned(): Int

    @Query("SELECT COALESCE(SUM(amountSeconds), 0) FROM screen_time_log WHERE type = 'SPENT'")
    suspend fun getTotalSpent(): Int
}

// ── New Parental Control DAOs ──

@Dao
interface ParentalConfigDao {
    @Query("SELECT * FROM parental_config LIMIT 1")
    fun getConfig(): Flow<ParentalConfig?>

    @Query("SELECT * FROM parental_config LIMIT 1")
    suspend fun getConfigOnce(): ParentalConfig?

    @Upsert
    suspend fun upsertConfig(config: ParentalConfig)

    @Query("UPDATE parental_config SET childModeActive = :active WHERE id = :id")
    suspend fun setChildModeActive(id: String, active: Boolean)

    @Query("DELETE FROM parental_config")
    suspend fun clear()
}

@Dao
interface AllowedAppDao {
    @Query("SELECT * FROM allowed_app WHERE configId = :configId")
    fun getAppsForConfig(configId: String): Flow<List<AllowedApp>>

    @Query("SELECT * FROM allowed_app WHERE configId = :configId")
    suspend fun getAppsForConfigOnce(configId: String): List<AllowedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AllowedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AllowedApp>)

    @Delete
    suspend fun removeApp(app: AllowedApp)

    @Query("DELETE FROM allowed_app WHERE configId = :configId")
    suspend fun clearForConfig(configId: String)

    @Query("SELECT packageName FROM allowed_app WHERE configId = :configId AND needsEarnedTime = 0")
    suspend fun getAlwaysAllowedPackages(configId: String): List<String>

    @Query("SELECT packageName FROM allowed_app WHERE configId = :configId")
    suspend fun getAllAllowedPackages(configId: String): List<String>
}

@Dao
interface ChildSessionDao {
    @Insert
    suspend fun startSession(session: ChildSession): Long

    @Query("UPDATE child_session SET endedAt = :endedAt, consumedSecs = :consumed, endReason = :reason WHERE id = :id")
    suspend fun endSession(id: Long, endedAt: Long, consumed: Int, reason: String)

    @Query("SELECT * FROM child_session ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastSession(): ChildSession?

    @Query("SELECT * FROM child_session ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 20): List<ChildSession>
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM daily_quest WHERE date = :date ORDER BY id")
    fun getQuestsForDate(date: String): Flow<List<DailyQuest>>

    @Query("SELECT * FROM daily_quest WHERE date = :date ORDER BY id")
    suspend fun getQuestsForDateOnce(date: String): List<DailyQuest>

    @Upsert
    suspend fun upsertQuest(quest: DailyQuest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quests: List<DailyQuest>)

    @Query("UPDATE daily_quest SET currentValue = :value, completed = :completed WHERE id = :id")
    suspend fun updateProgress(id: String, value: Int, completed: Boolean)

    @Query("SELECT COUNT(*) FROM daily_quest WHERE date = :date AND completed = 1")
    suspend fun getCompletedCount(date: String): Int

    @Query("DELETE FROM daily_quest WHERE date < :date")
    suspend fun clearOldQuests(date: String)
}
