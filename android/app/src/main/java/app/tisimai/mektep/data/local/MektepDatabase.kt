package app.tisimai.mektep.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tisimai.mektep.data.models.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        UserProfile::class,
        ChildProfile::class,
        LessonProgress::class,
        ScreenTimeLog::class,
        DailyQuest::class,
        ParentalConfig::class,
        AllowedApp::class,
        ChildSession::class,
        QuestionAttempt::class,
        TopicMastery::class
    ],
    version = 7,
    exportSchema = false
)
abstract class MektepDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun progressDao(): ProgressDao
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun parentalConfigDao(): ParentalConfigDao
    abstract fun allowedAppDao(): AllowedAppDao
    abstract fun childSessionDao(): ChildSessionDao
    abstract fun questDao(): QuestDao
    abstract fun questionAttemptDao(): QuestionAttemptDao
    abstract fun topicMasteryDao(): TopicMasteryDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create child_profile table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS child_profile (
                        id TEXT NOT NULL PRIMARY KEY,
                        parentUserId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        birthDate TEXT NOT NULL DEFAULT '',
                        avatarEmoji TEXT NOT NULL DEFAULT '🧒',
                        gradeLevel INTEGER NOT NULL DEFAULT 1,
                        xpTotal INTEGER NOT NULL DEFAULT 0,
                        currentStreak INTEGER NOT NULL DEFAULT 0,
                        longestStreak INTEGER NOT NULL DEFAULT 0,
                        lastActiveDate TEXT,
                        screenTimeBalanceSecs INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 2. Create default child from existing UserProfile
                db.execSQL("""
                    INSERT OR IGNORE INTO child_profile (id, parentUserId, name, gradeLevel, xpTotal, currentStreak, longestStreak, lastActiveDate, screenTimeBalanceSecs, createdAt)
                    SELECT 'default_child', id, displayName, gradeLevel, xpTotal, currentStreak, longestStreak, lastActiveDate, screenTimeBalanceSecs, ${System.currentTimeMillis()}
                    FROM user_profile LIMIT 1
                """.trimIndent())

                // 3. Recreate lesson_progress with composite PK (childId + lessonId)
                db.execSQL("""
                    CREATE TABLE lesson_progress_new (
                        childId TEXT NOT NULL DEFAULT 'default_child',
                        lessonId TEXT NOT NULL,
                        subjectId TEXT NOT NULL,
                        bestStars INTEGER NOT NULL DEFAULT 0,
                        bestAccuracy REAL NOT NULL DEFAULT 0.0,
                        timesCompleted INTEGER NOT NULL DEFAULT 0,
                        lastCompletedAt INTEGER,
                        PRIMARY KEY(childId, lessonId)
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO lesson_progress_new (childId, lessonId, subjectId, bestStars, bestAccuracy, timesCompleted, lastCompletedAt) SELECT 'default_child', lessonId, subjectId, bestStars, bestAccuracy, timesCompleted, lastCompletedAt FROM lesson_progress")
                db.execSQL("DROP TABLE lesson_progress")
                db.execSQL("ALTER TABLE lesson_progress_new RENAME TO lesson_progress")

                // 4. Add childId to screen_time_log
                db.execSQL("ALTER TABLE screen_time_log ADD COLUMN childId TEXT NOT NULL DEFAULT 'default_child'")

                // 5. Recreate daily_quest with composite PK (childId + id)
                db.execSQL("""
                    CREATE TABLE daily_quest_new (
                        childId TEXT NOT NULL DEFAULT 'default_child',
                        id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        targetValue INTEGER NOT NULL,
                        currentValue INTEGER NOT NULL DEFAULT 0,
                        xpReward INTEGER NOT NULL DEFAULT 10,
                        completed INTEGER NOT NULL DEFAULT 0,
                        date TEXT NOT NULL,
                        PRIMARY KEY(childId, id)
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO daily_quest_new (childId, id, type, targetValue, currentValue, xpReward, completed, date) SELECT 'default_child', id, type, targetValue, currentValue, xpReward, completed, date FROM daily_quest")
                db.execSQL("DROP TABLE daily_quest")
                db.execSQL("ALTER TABLE daily_quest_new RENAME TO daily_quest")

                // 6. Add childId to child_session
                db.execSQL("ALTER TABLE child_session ADD COLUMN childId TEXT NOT NULL DEFAULT 'default_child'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE child_profile ADD COLUMN dailyLimitMinutes INTEGER NOT NULL DEFAULT 60")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE child_profile ADD COLUMN language TEXT NOT NULL DEFAULT 'kk'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS question_attempt (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, childId TEXT NOT NULL, lessonId TEXT NOT NULL, questionIndex INTEGER NOT NULL, isCorrect INTEGER NOT NULL, responseTimeMs INTEGER NOT NULL, attemptTimestamp INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_attempt_child_lesson ON question_attempt(childId, lessonId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS topic_mastery (childId TEXT NOT NULL, topicId TEXT NOT NULL, masteryScore REAL NOT NULL DEFAULT 50.0, totalAttempts INTEGER NOT NULL DEFAULT 0, lastUpdatedAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(childId, topicId))")
                db.execSQL("ALTER TABLE lesson_progress ADD COLUMN nextReviewDate TEXT DEFAULT NULL")
            }
        }
    }
}

// ── Child Profile DAO ──

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profile WHERE parentUserId = :parentId ORDER BY createdAt")
    fun getChildrenForParent(parentId: String): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profile WHERE parentUserId = :parentId ORDER BY createdAt")
    suspend fun getChildrenForParentOnce(parentId: String): List<ChildProfile>

    @Query("SELECT * FROM child_profile WHERE id = :childId")
    suspend fun getChild(childId: String): ChildProfile?

    @Query("SELECT * FROM child_profile WHERE id = :childId")
    fun observeChild(childId: String): Flow<ChildProfile?>

    @Insert
    suspend fun insert(child: ChildProfile)

    @Update
    suspend fun update(child: ChildProfile)

    @Query("UPDATE child_profile SET xpTotal = xpTotal + :xp WHERE id = :childId")
    suspend fun addXp(childId: String, xp: Int)

    @Query("UPDATE child_profile SET currentStreak = :streak, longestStreak = MAX(longestStreak, :streak), lastActiveDate = :date WHERE id = :childId")
    suspend fun updateStreak(childId: String, streak: Int, date: String)

    @Query("UPDATE child_profile SET screenTimeBalanceSecs = screenTimeBalanceSecs + :deltaSecs WHERE id = :childId")
    suspend fun updateScreenTimeBalance(childId: String, deltaSecs: Int)

    @Query("UPDATE child_profile SET dailyLimitMinutes = :limit WHERE id = :childId")
    suspend fun updateDailyLimit(childId: String, limit: Int)

    @Delete
    suspend fun delete(child: ChildProfile)

    @Query("SELECT COUNT(*) FROM child_profile WHERE parentUserId = :parentId")
    suspend fun getChildCount(parentId: String): Int
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
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId")
    fun getAllForChild(childId: String): Flow<List<LessonProgress>>

    // Backward compat for solo mode
    @Query("SELECT * FROM lesson_progress WHERE childId = ''")
    fun getAll(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND lessonId = :lessonId")
    suspend fun getForLesson(childId: String, lessonId: String): LessonProgress?

    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND subjectId = :subjectId")
    suspend fun getForSubject(childId: String, subjectId: String): List<LessonProgress>

    @Upsert
    suspend fun upsert(progress: LessonProgress)

    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND nextReviewDate IS NOT NULL AND nextReviewDate <= :today ORDER BY nextReviewDate ASC")
    suspend fun getLessonsDueForReview(childId: String, today: String): List<LessonProgress>
}

@Dao
interface QuestionAttemptDao {
    @Insert suspend fun insert(attempt: QuestionAttempt)
    @Insert suspend fun insertAll(attempts: List<QuestionAttempt>)

    @Query("SELECT * FROM question_attempt WHERE childId = :childId AND lessonId = :lessonId ORDER BY attemptTimestamp DESC")
    suspend fun getForLesson(childId: String, lessonId: String): List<QuestionAttempt>

    @Query("SELECT DISTINCT questionIndex FROM question_attempt WHERE childId = :childId AND lessonId = :lessonId AND isCorrect = 0 AND attemptTimestamp = (SELECT MAX(attemptTimestamp) FROM question_attempt a2 WHERE a2.childId = question_attempt.childId AND a2.lessonId = question_attempt.lessonId AND a2.questionIndex = question_attempt.questionIndex)")
    suspend fun getRecentlyWrongIndices(childId: String, lessonId: String): List<Int>
}

@Dao
interface TopicMasteryDao {
    @Query("SELECT * FROM topic_mastery WHERE childId = :childId AND topicId = :topicId")
    suspend fun get(childId: String, topicId: String): TopicMastery?

    @Query("SELECT * FROM topic_mastery WHERE childId = :childId ORDER BY masteryScore ASC")
    suspend fun getAllForChild(childId: String): List<TopicMastery>

    @Query("SELECT * FROM topic_mastery WHERE childId = :childId ORDER BY masteryScore ASC LIMIT 1")
    suspend fun getWeakest(childId: String): TopicMastery?

    @Upsert suspend fun upsert(mastery: TopicMastery)
}

@Dao
interface ScreenTimeDao {
    @Insert
    suspend fun addLog(log: ScreenTimeLog)

    @Query("SELECT * FROM screen_time_log WHERE childId = :childId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogsForChild(childId: String, limit: Int = 50): List<ScreenTimeLog>

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

    @Query("SELECT * FROM child_session WHERE childId = :childId ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastSessionForChild(childId: String): ChildSession?

    @Query("SELECT * FROM child_session ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastSession(): ChildSession?

    @Query("SELECT * FROM child_session WHERE childId = :childId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentSessionsForChild(childId: String, limit: Int = 20): List<ChildSession>

    @Query("SELECT * FROM child_session ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 20): List<ChildSession>
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM daily_quest WHERE childId = :childId AND date = :date ORDER BY id")
    fun getQuestsForChild(childId: String, date: String): Flow<List<DailyQuest>>

    // Backward compat for solo mode
    @Query("SELECT * FROM daily_quest WHERE childId = '' AND date = :date ORDER BY id")
    fun getQuestsForDate(date: String): Flow<List<DailyQuest>>

    @Query("SELECT * FROM daily_quest WHERE childId = :childId AND date = :date ORDER BY id")
    suspend fun getQuestsForChildOnce(childId: String, date: String): List<DailyQuest>

    @Upsert
    suspend fun upsertQuest(quest: DailyQuest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quests: List<DailyQuest>)

    @Query("UPDATE daily_quest SET currentValue = :value, completed = :completed WHERE childId = :childId AND id = :id")
    suspend fun updateProgress(childId: String, id: String, value: Int, completed: Boolean)

    @Query("SELECT COUNT(*) FROM daily_quest WHERE childId = :childId AND date = :date AND completed = 1")
    suspend fun getCompletedCount(childId: String, date: String): Int

    @Query("DELETE FROM daily_quest WHERE date < :date")
    suspend fun clearOldQuests(date: String)
}
