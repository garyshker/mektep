package com.mektep.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// --- Auth ---

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String,
    @SerialName("language_preference") val languagePreference: String = "en",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("grade_level") val gradeLevel: Int? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: User
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("language_preference") val languagePreference: String = "en",
    val timezone: String = "UTC",
    @SerialName("created_at") val createdAt: String = ""
)

// --- Family ---

@Serializable
data class Family(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreateFamilyRequest(val name: String)

@Serializable
data class JoinFamilyRequest(val code: String)

@Serializable
data class InviteCode(
    val id: String,
    @SerialName("family_id") val familyId: String,
    val code: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class FamilyMember(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("user_id") val userId: String,
    val role: String
)

// --- Child Profile ---

@Serializable
data class ChildProfile(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val avatar: String = "",
    @SerialName("grade_level") val gradeLevel: Int = 1,
    @SerialName("xp_total") val xpTotal: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("screen_time_balance_seconds") val screenTimeBalanceSeconds: Int = 0
)

// --- Subjects & Lessons ---

typealias I18nText = Map<String, String>

@Serializable
data class Subject(
    val id: String,
    val name: I18nText,
    val icon: String = "",
    @SerialName("color_scheme") val colorScheme: String = "",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class Lesson(
    val id: String,
    @SerialName("subject_id") val subjectId: String,
    val title: I18nText,
    val description: I18nText = emptyMap(),
    @SerialName("grade_level") val gradeLevel: Int = 1,
    @SerialName("difficulty_tier") val difficultyTier: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val questions: List<Question> = emptyList()
)

@Serializable
data class Question(
    val id: String,
    @SerialName("lesson_id") val lessonId: String = "",
    val type: String,
    val prompt: I18nText,
    @SerialName("media_url") val mediaUrl: String? = null,
    val options: JsonElement? = null,
    @SerialName("correct_answer") val correctAnswer: JsonElement? = null,
    val pairs: JsonElement? = null,
    @SerialName("difficulty_score") val difficultyScore: Int = 1,
    @SerialName("time_limit_seconds") val timeLimitSeconds: Int? = null,
    @SerialName("sort_order") val sortOrder: Int = 0
)

// --- Lesson Attempts ---

@Serializable
data class LessonAttempt(
    val id: String,
    @SerialName("child_id") val childId: String,
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("started_at") val startedAt: String = "",
    val status: String = "IN_PROGRESS"
)

@Serializable
data class StartAttemptRequest(
    @SerialName("child_id") val childId: String
)

@Serializable
data class SubmitAnswerRequest(
    @SerialName("question_id") val questionId: String,
    @SerialName("given_answer") val givenAnswer: String,
    @SerialName("time_spent_ms") val timeSpentMs: Int = 0
)

@Serializable
data class SubmitAnswerResponse(
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("correct_answer") val correctAnswer: String? = null
)

@Serializable
data class CompleteResponse(
    val score: Int,
    @SerialName("accuracy_pct") val accuracyPct: Double,
    @SerialName("stars_earned") val starsEarned: Int,
    @SerialName("xp_earned") val xpEarned: Int
)

// --- Dashboard ---

@Serializable
data class DashboardResponse(
    val profile: ChildProfile,
    val progress: List<SubjectProgress>
)

@Serializable
data class SubjectProgress(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("completed_lessons") val completedLessons: Int = 0,
    @SerialName("total_lessons") val totalLessons: Int = 0,
    @SerialName("best_stars") val bestStars: Int = 0,
    @SerialName("total_xp") val totalXp: Int = 0
)

// --- Screen Time ---

@Serializable
data class ScreenTimeBalance(
    @SerialName("balance_seconds") val balanceSeconds: Int,
    @SerialName("balance_minutes") val balanceMinutes: Int
)

@Serializable
data class SpendRequest(
    val seconds: Int,
    @SerialName("app_name") val appName: String
)

@Serializable
data class BonusRequest(
    val minutes: Int,
    val reason: String
)

@Serializable
data class ScreenTimeConfig(
    val id: String = "",
    @SerialName("child_id") val childId: String = "",
    @SerialName("points_per_minute") val pointsPerMinute: Int = 10,
    @SerialName("daily_max_minutes") val dailyMaxMinutes: Int = 120,
    @SerialName("bedtime_start") val bedtimeStart: String? = null,
    @SerialName("bedtime_end") val bedtimeEnd: String? = null,
    @SerialName("blocked_apps") val blockedApps: JsonElement? = null,
    @SerialName("allowed_apps") val allowedApps: JsonElement? = null
)
