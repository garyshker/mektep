package com.mektep.app.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mektep.app.data.local.TokenStore
import com.mektep.app.data.models.Question
import com.mektep.app.data.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import javax.inject.Inject

data class LessonRunnerState(
    val isLoading: Boolean = true,
    val lessonTitle: String = "",
    val questions: List<Question> = emptyList(),
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
    val attemptId: String = "",
    val optionTexts: List<Any> = emptyList(),
    val matchPairs: List<Pair<String, String>> = emptyList(),
    val currentQuestion: Question? = null,
    val startTimeMs: Long = 0L
)

@HiltViewModel
class LessonRunnerViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonRunnerState())
    val uiState: StateFlow<LessonRunnerState> = _uiState.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = LessonRunnerState(isLoading = true)
            try {
                val lesson = lessonRepository.getLesson(lessonId)
                val childId = tokenStore.childId.firstOrNull()

                var attemptId = ""
                if (childId != null) {
                    try {
                        val attempt = lessonRepository.startAttempt(lessonId, childId)
                        attemptId = attempt.id
                    } catch (_: Exception) { }
                }

                val lang = language.value
                val title = lesson.title[lang] ?: lesson.title["en"] ?: "Lesson"

                _uiState.value = LessonRunnerState(
                    isLoading = false,
                    lessonTitle = title,
                    questions = lesson.questions,
                    totalQuestions = lesson.questions.size,
                    attemptId = attemptId,
                    currentQuestion = lesson.questions.firstOrNull(),
                    optionTexts = parseOptions(lesson.questions.firstOrNull()),
                    matchPairs = parsePairs(lesson.questions.firstOrNull()),
                    startTimeMs = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = LessonRunnerState(isLoading = false)
            }
        }
    }

    fun selectAnswer(answer: String) {
        _uiState.value = _uiState.value.copy(selectedAnswer = answer)
    }

    fun submitCurrentAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val timeMs = (System.currentTimeMillis() - state.startTimeMs).toInt()

        viewModelScope.launch {
            val isCorrect = try {
                if (state.attemptId.isNotEmpty()) {
                    val response = lessonRepository.submitAnswer(
                        state.attemptId, question.id, state.selectedAnswer, timeMs
                    )
                    response.isCorrect
                } else {
                    checkAnswerLocally(question, state.selectedAnswer)
                }
            } catch (_: Exception) {
                checkAnswerLocally(question, state.selectedAnswer)
            }

            val newHearts = if (isCorrect) state.hearts else (state.hearts - 1).coerceAtLeast(0)
            val newScore = if (isCorrect) state.score + 1 else state.score

            _uiState.value = state.copy(
                feedbackShown = true,
                lastAnswerCorrect = isCorrect,
                hearts = newHearts,
                score = newScore
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.questionIndex + 1

        if (nextIndex >= state.totalQuestions || state.hearts <= 0) {
            completeLesson()
            return
        }

        val nextQuestion = state.questions[nextIndex]
        _uiState.value = state.copy(
            questionIndex = nextIndex,
            currentQuestion = nextQuestion,
            selectedAnswer = "",
            feedbackShown = false,
            lastAnswerCorrect = false,
            optionTexts = parseOptions(nextQuestion),
            matchPairs = parsePairs(nextQuestion),
            startTimeMs = System.currentTimeMillis()
        )
    }

    fun quit() {
        _uiState.value = _uiState.value.copy(isCompleted = true, starsEarned = 0, xpEarned = 0)
    }

    private fun completeLesson() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                if (state.attemptId.isNotEmpty()) {
                    val result = lessonRepository.completeAttempt(state.attemptId)
                    _uiState.value = state.copy(
                        isCompleted = true,
                        score = result.score,
                        xpEarned = result.xpEarned,
                        starsEarned = result.starsEarned,
                        accuracyPct = result.accuracyPct
                    )
                    return@launch
                }
            } catch (_: Exception) { }

            // Local fallback scoring
            val accuracy = if (state.totalQuestions > 0) state.score.toDouble() / state.totalQuestions * 100 else 0.0
            val stars = when {
                accuracy >= 95 -> 3
                accuracy >= 80 -> 2
                else -> 1
            }
            val xp = state.score * 5 + when(stars) { 3 -> 20; 2 -> 10; else -> 0 }

            _uiState.value = state.copy(
                isCompleted = true,
                xpEarned = xp,
                starsEarned = stars,
                accuracyPct = accuracy
            )
        }
    }

    private fun checkAnswerLocally(question: Question, answer: String): Boolean {
        val correct = question.correctAnswer ?: return false
        return try {
            when {
                correct is JsonPrimitive && correct.isString ->
                    correct.content.equals(answer.trim(), ignoreCase = true)
                correct is JsonPrimitive ->
                    correct.content == answer.trim()
                else -> false
            }
        } catch (_: Exception) { false }
    }

    private fun parseOptions(question: Question?): List<Any> {
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

    private fun parsePairs(question: Question?): List<Pair<String, String>> {
        question ?: return emptyList()
        val pairs = question.pairs ?: return emptyList()
        return try {
            when (pairs) {
                is JsonArray -> pairs.map { element ->
                    val obj = element.jsonObject
                    val left = obj["left"]?.jsonPrimitive?.content ?: ""
                    val right = obj["right"]?.jsonPrimitive?.content ?: ""
                    left to right
                }
                else -> emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }
}
