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

@Entity(tableName = "lesson_progress", primaryKeys = ["childId", "lessonId"])
data class LessonProgress(
    val childId: String = "",
    val lessonId: String,
    val subjectId: String,
    val bestStars: Int = 0,
    val bestAccuracy: Double = 0.0,
    val timesCompleted: Int = 0,
    val lastCompletedAt: Long? = null,
    val nextReviewDate: String? = null // ISO date for spaced repetition
)

// ── Adaptive learning entities ──

@Entity(
    tableName = "question_attempt",
    indices = [androidx.room.Index(value = ["childId", "lessonId"])]
)
data class QuestionAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val lessonId: String,
    val questionIndex: Int,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    @androidx.room.ColumnInfo(defaultValue = "0") val attemptTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "topic_mastery", primaryKeys = ["childId", "topicId"])
data class TopicMastery(
    val childId: String,
    val topicId: String, // same as lessonId
    val masteryScore: Double = 50.0, // 0-100, starts neutral
    val totalAttempts: Int = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

// ── Adaptive learning helper types ──

data class AttemptResult(
    val questionIndex: Int,
    val isCorrect: Boolean,
    val responseTimeMs: Long
)

data class LessonStatus(
    val isUnlocked: Boolean,
    val isDueForReview: Boolean
)

data class RecommendedLesson(
    val lessonId: String,
    val reason: String // "review", "next", "practice"
)

@Entity(tableName = "screen_time_log")
data class ScreenTimeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String = "",
    val type: String, // EARNED, SPENT, BONUS
    val amountSeconds: Int,
    val source: String, // lesson ID or app name
    val timestamp: Long = System.currentTimeMillis()
)

// ── Daily quests ──

@Entity(tableName = "daily_quest", primaryKeys = ["childId", "id"])
data class DailyQuest(
    val childId: String = "",
    val id: String, // e.g. "complete_1_lesson", "earn_50_xp"
    val type: String, // LESSON_COUNT, XP_AMOUNT, QUICK_GAME, STREAK
    val targetValue: Int, // target to reach
    val currentValue: Int = 0,
    val xpReward: Int = 10,
    val completed: Boolean = false,
    val date: String // "2026-05-15" — quests reset daily
)

// ── Child profiles (multi-child support) ──

@Entity(tableName = "child_profile")
data class ChildProfile(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val parentUserId: String,
    val name: String,
    val birthDate: String = "", // ISO "2018-03-15"
    val avatarEmoji: String = "\uD83E\uDDD2", // 🧒
    val language: String = "kk", // child's preferred language
    val gradeLevel: Int = 1,
    val xpTotal: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String? = null,
    val screenTimeBalanceSecs: Int = 0,
    val dailyLimitMinutes: Int = 60,
    val createdAt: Long = System.currentTimeMillis()
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
    val childId: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val initialBalanceSecs: Int = 0,
    val consumedSecs: Int = 0,
    val endReason: String? = null // TIME_UP, PARENT_EXIT, FORCED_CLOSE
)
