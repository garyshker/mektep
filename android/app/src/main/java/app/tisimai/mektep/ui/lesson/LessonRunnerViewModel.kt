package app.tisimai.mektep.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.*
import app.tisimai.mektep.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import javax.inject.Inject

data class LessonRunnerState(
    val isLoading: Boolean = true,
    val lessonTitle: String = "",
    val questions: List<QuestionData> = emptyList(),
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val hearts: Int = 3,
    val selectedAnswer: String = "",
    val feedbackShown: Boolean = false,
    val lastAnswerCorrect: Boolean = false,
    val isCompleted: Boolean = false,
    val score: Int = 0,
    val xpEarned: Int = 0,
    val starsEarned: Int = 0,
    val accuracyPct: Double = 0.0,
    val optionTexts: List<Any> = emptyList(),
    val matchPairs: List<Pair<String, String>> = emptyList(),
    val currentQuestion: QuestionData? = null,
    val startTimeMs: Long = 0L,          // when current question started
    val lessonStartTimeMs: Long = 0L,    // when entire lesson started
    val timeSpentMinutes: Int = 0,       // total learning time
    val earnedScreenTimeMinutes: Int = 0 // screen time earned
)

@HiltViewModel
class LessonRunnerViewModel @Inject constructor(
    private val lessonLoader: LessonLoader,
    private val userDao: UserDao,
    private val progressDao: ProgressDao,
    private val screenTimeDao: ScreenTimeDao,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonRunnerState())
    val uiState: StateFlow<LessonRunnerState> = _uiState.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    private var currentLessonId: String = ""
    private var currentSubjectId: String = ""

    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = LessonRunnerState(isLoading = true)
            val lesson = lessonLoader.getLesson(lessonId)
            if (lesson == null) {
                _uiState.value = LessonRunnerState(isLoading = false)
                return@launch
            }

            currentLessonId = lessonId
            currentSubjectId = lesson.subjectId
            val lang = language.value
            val title = lesson.title[lang] ?: lesson.title["en"] ?: "Lesson"

            val now = System.currentTimeMillis()
            _uiState.value = LessonRunnerState(
                isLoading = false,
                lessonTitle = title,
                questions = lesson.questions,
                totalQuestions = lesson.questions.size,
                currentQuestion = lesson.questions.firstOrNull(),
                optionTexts = parseOptions(lesson.questions.firstOrNull()),
                matchPairs = parsePairs(lesson.questions.firstOrNull()),
                startTimeMs = now,
                lessonStartTimeMs = now
            )
        }
    }

    fun selectAnswer(answer: String) {
        _uiState.value = _uiState.value.copy(selectedAnswer = answer)
    }

    fun submitCurrentAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return

        val isCorrect = checkAnswer(question, state.selectedAnswer)
        val newHearts = if (isCorrect) state.hearts else (state.hearts - 1).coerceAtLeast(0)
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            feedbackShown = true,
            lastAnswerCorrect = isCorrect,
            hearts = newHearts,
            score = newScore
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.questionIndex + 1

        if (nextIndex >= state.totalQuestions || state.hearts <= 0) {
            completeLesson()
            return
        }

        val nextQ = state.questions[nextIndex]
        _uiState.value = state.copy(
            questionIndex = nextIndex,
            currentQuestion = nextQ,
            selectedAnswer = "",
            feedbackShown = false,
            lastAnswerCorrect = false,
            optionTexts = parseOptions(nextQ),
            matchPairs = parsePairs(nextQ),
            startTimeMs = System.currentTimeMillis()
        )
    }

    fun quit() {
        _uiState.value = _uiState.value.copy(isCompleted = true, starsEarned = 0, xpEarned = 0)
    }

    companion object {
        // Time-based screen time model:
        // Every 1 minute of learning earns 1.5 minutes of screen time
        // Minimum 1 minute earned per lesson (even if lesson takes < 40 seconds)
        const val SCREEN_TIME_RATIO = 1.5
        const val MIN_EARNED_MINUTES = 1
    }

    private fun completeLesson() {
        val state = _uiState.value
        val accuracy = if (state.totalQuestions > 0) state.score.toDouble() / state.totalQuestions * 100 else 0.0
        val stars = when {
            accuracy >= 95 -> 3
            accuracy >= 80 -> 2
            else -> 1
        }
        val xp = state.score * 5 + when (stars) { 3 -> 20; 2 -> 10; else -> 0 }

        // Time-based screen time:
        // Calculate how long the student actually spent learning
        val elapsedMs = System.currentTimeMillis() - state.lessonStartTimeMs
        val learningMinutes = (elapsedMs / 60_000.0).coerceAtLeast(0.0)
        val earnedMinutes = maxOf(MIN_EARNED_MINUTES, (learningMinutes * SCREEN_TIME_RATIO).toInt())
        val earnedSeconds = earnedMinutes * 60

        viewModelScope.launch {
            val profile = userDao.getProfileOnce()
            if (profile != null) {
                // Update XP (gamification — separate from screen time)
                userDao.addXp(profile.id, xp)

                // Update screen time balance (time-based)
                userDao.updateScreenTimeBalance(profile.id, earnedSeconds)

                // Log screen time earned
                screenTimeDao.addLog(
                    ScreenTimeLog(type = "EARNED", amountSeconds = earnedSeconds, source = currentLessonId)
                )

                // Update streak
                val today = java.time.LocalDate.now().toString()
                val newStreak = if (profile.lastActiveDate == today) {
                    profile.currentStreak
                } else if (profile.lastActiveDate == java.time.LocalDate.now().minusDays(1).toString()) {
                    profile.currentStreak + 1
                } else 1
                userDao.updateStreak(profile.id, newStreak, today)
            }

            // Save lesson progress
            val existing = progressDao.getForLesson(currentLessonId)
            progressDao.upsert(
                LessonProgress(
                    lessonId = currentLessonId,
                    subjectId = currentSubjectId,
                    bestStars = maxOf(stars, existing?.bestStars ?: 0),
                    bestAccuracy = maxOf(accuracy, existing?.bestAccuracy ?: 0.0),
                    timesCompleted = (existing?.timesCompleted ?: 0) + 1,
                    lastCompletedAt = System.currentTimeMillis()
                )
            )
        }

        val timeSpentMin = (elapsedMs / 60_000.0).toInt()

        _uiState.value = state.copy(
            isCompleted = true,
            xpEarned = xp,
            starsEarned = stars,
            accuracyPct = accuracy,
            timeSpentMinutes = maxOf(1, timeSpentMin),
            earnedScreenTimeMinutes = earnedMinutes
        )
    }

    private fun checkAnswer(q: QuestionData, answer: String): Boolean {
        val correct = q.correctAnswer ?: return answer == "matched"
        return try {
            when (correct) {
                is JsonPrimitive -> {
                    if (correct.isString) {
                        correct.content.equals(answer.trim(), ignoreCase = true)
                    } else {
                        correct.content == answer.trim()
                    }
                }
                else -> answer == "matched" // match questions auto-pass
            }
        } catch (_: Exception) { false }
    }

    private fun parseOptions(question: QuestionData?): List<Any> {
        question ?: return emptyList()
        val options = question.options ?: return emptyList()
        return try {
            when (options) {
                is JsonArray -> options.map { element ->
                    when (element) {
                        is JsonArray -> {
                            if (element.size == 1 && element[0] is JsonObject) {
                                val obj = element[0] as JsonObject
                                obj.mapValues { it.value.jsonPrimitive.content }
                            } else element.toString()
                        }
                        is JsonObject -> element.mapValues { it.value.jsonPrimitive.content }
                        is JsonPrimitive -> element.content
                        else -> element.toString()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parsePairs(question: QuestionData?): List<Pair<String, String>> {
        question ?: return emptyList()
        val pairs = question.pairs ?: return emptyList()
        return try {
            when (pairs) {
                is JsonArray -> pairs.map { element ->
                    val obj = element.jsonObject
                    (obj["left"]?.jsonPrimitive?.content ?: "") to (obj["right"]?.jsonPrimitive?.content ?: "")
                }
                else -> emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }
}
