package app.tisimai.mektep.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── Bundled lesson JSON format ──

typealias I18nText = Map<String, String>

@Serializable
data class LessonFile(
    val subject: String,
    @SerialName("grade_level") val gradeLevel: Int,
    @SerialName("difficulty_tier") val difficultyTier: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val title: I18nText,
    val description: I18nText = emptyMap(),
    val questions: List<QuestionData> = emptyList()
)

@Serializable
data class QuestionData(
    val type: String,
    val prompt: I18nText,
    @SerialName("media_url") val mediaUrl: String? = null,
    val options: JsonElement? = null,
    @SerialName("correct_answer") val correctAnswer: JsonElement? = null,
    val pairs: JsonElement? = null,
    @SerialName("difficulty_score") val difficultyScore: Int = 1,
    @SerialName("time_limit_seconds") val timeLimitSeconds: Int? = null
)

@Serializable
data class MatchPair(
    val left: String,
    val right: String
)

// ── Domain models (in-memory) ──

data class Subject(
    val id: String,
    val name: I18nText,
    val emoji: String,
    val colorKey: String
)

data class Lesson(
    val id: String,
    val subjectId: String,
    val title: I18nText,
    val description: I18nText,
    val gradeLevel: Int,
    val sortOrder: Int,
    val questions: List<QuestionData>,
    val fileName: String
)

// ── Room entities (persisted) ──

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val role: String = "CHILD", // CHILD or PARENT
    val language: String = "en",
    val gradeLevel: Int = 1,
    val xpTotal: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String? = null,
    val screenTimeBalanceSecs: Int = 0
)

@Entity(tableName = "lesson_progress")
data class LessonProgress(
    @PrimaryKey val lessonId: String,
    val subjectId: String,
    val bestStars: Int = 0,
    val bestAccuracy: Double = 0.0,
    val timesCompleted: Int = 0,
    val lastCompletedAt: Long? = null
)

@Entity(tableName = "screen_time_log")
data class ScreenTimeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // EARNED, SPENT, BONUS
    val amountSeconds: Int,
    val source: String, // lesson ID or app name
    val timestamp: Long = System.currentTimeMillis()
)

// ── Daily quests ──

@Entity(tableName = "daily_quest")
data class DailyQuest(
    @PrimaryKey val id: String, // e.g. "complete_1_lesson", "earn_50_xp"
    val type: String, // LESSON_COUNT, XP_AMOUNT, QUICK_GAME, STREAK
    val targetValue: Int, // target to reach
    val currentValue: Int = 0,
    val xpReward: Int = 10,
    val completed: Boolean = false,
    val date: String // "2026-05-15" — quests reset daily
)

// ── Parental control entities ──

@Entity(tableName = "parental_config")
data class ParentalConfig(
    @PrimaryKey val id: String = "local", // "local" for same-device, familyId for remote
    val mode: String = "NONE", // NONE, SAME_DEVICE, REMOTE_PARENT, REMOTE_CHILD
    val pinHash: String = "",
    val pinSalt: String = "",
    val childModeActive: Boolean = false,
    val dailyLimitMinutes: Int = 60,
    val bedtimeStart: String? = null, // "21:00"
    val bedtimeEnd: String? = null, // "07:00"
    val familyId: String? = null,
    val inviteCode: String? = null,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "allowed_app")
data class AllowedApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: String = "local",
    val packageName: String,
    val appLabel: String,
    val needsEarnedTime: Boolean = true, // true = must earn screen time; false = always available
    val dailyLimitMinutes: Int? = null // per-app limit, null = use global
)

@Entity(tableName = "child_session")
data class ChildSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val initialBalanceSecs: Int = 0,
    val consumedSecs: Int = 0,
    val endReason: String? = null // TIME_UP, PARENT_EXIT, FORCED_CLOSE
)
