package com.mektep.app.data.local

import androidx.room.*
import com.mektep.app.data.models.LessonProgress
import com.mektep.app.data.models.ScreenTimeLog
import com.mektep.app.data.models.UserProfile
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [UserProfile::class, LessonProgress::class, ScreenTimeLog::class],
    version = 1,
    exportSchema = false
)
abstract class MektepDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun progressDao(): ProgressDao
    abstract fun screenTimeDao(): ScreenTimeDao
}

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
