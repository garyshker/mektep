package com.mektep.app.data.models

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
