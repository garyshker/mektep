package app.tisimai.mektep.ui.quickgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.local.ScreenTimeDao
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.AgeBand
import app.tisimai.mektep.data.models.ScreenTimeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class QuickGameQuestion(
    val prompt: String,
    val correctAnswer: Int,
    val options: List<Int>
)

data class QuickGameState(
    val isPlaying: Boolean = false,
    val isDone: Boolean = false,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 20,
    val score: Int = 0,
    val timeLeft: Int = 5, // seconds
    val currentQuestion: QuickGameQuestion? = null,
    val chosenAnswer: Int? = null, // which option index was tapped
    val isCorrect: Boolean? = null,
    val showingFeedback: Boolean = false
)

@HiltViewModel
class QuickGameViewModel @Inject constructor(
    private val userDao: UserDao,
    private val childProfileDao: ChildProfileDao,
    private val screenTimeDao: ScreenTimeDao,
    private val parentalPrefsStore: ParentalPrefsStore
) : ViewModel() {

    companion object {
        const val TOTAL_QUESTIONS = 20
        const val TIME_PER_QUESTION = 5
        const val FEEDBACK_DELAY_MS = 600L
    }

    private val _state = MutableStateFlow(QuickGameState())
    val state: StateFlow<QuickGameState> = _state.asStateFlow()

    private var currentBand: AgeBand = AgeBand.OQYSHY
    private var timerJob: Job? = null
    private var questions: List<QuickGameQuestion> = emptyList()

    fun startGame() {
        viewModelScope.launch {
            val childId = parentalPrefsStore.activeChildId.first()
            val gradeLevel = if (childId != null) {
                childProfileDao.getChild(childId)?.gradeLevel ?: 2
            } else {
                userDao.getProfileOnce()?.gradeLevel ?: 2
            }
            currentBand = AgeBand.fromGradeLevel(gradeLevel)

            questions = generateQuestions()
            _state.value = QuickGameState(
                isPlaying = true,
                totalQuestions = currentBand.quickGameQuestions,
                currentQuestion = questions.first(),
                timeLeft = currentBand.quickGameTimerSec
            )
            startTimer()
        }
    }

    fun selectAnswer(optionIndex: Int) {
        val s = _state.value
        if (s.showingFeedback || s.isDone || s.chosenAnswer != null) return

        timerJob?.cancel()
        val question = s.currentQuestion ?: return
        val isCorrect = question.options[optionIndex] == question.correctAnswer
        val newScore = if (isCorrect) s.score + 1 else s.score

        _state.value = s.copy(
            chosenAnswer = optionIndex,
            isCorrect = isCorrect,
            score = newScore,
            showingFeedback = true
        )

        // Advance after delay
        viewModelScope.launch {
            delay(FEEDBACK_DELAY_MS)
            advanceQuestion()
        }
    }

    private fun onTimeout() {
        val s = _state.value
        if (s.showingFeedback || s.isDone) return

        // Timeout counts as wrong — reveal correct answer
        _state.value = s.copy(
            chosenAnswer = -1, // -1 = timeout, no selection
            isCorrect = false,
            showingFeedback = true
        )

        viewModelScope.launch {
            delay(FEEDBACK_DELAY_MS)
            advanceQuestion()
        }
    }

    private fun advanceQuestion() {
        val s = _state.value
        val nextIdx = s.questionIndex + 1

        if (nextIdx >= currentBand.quickGameQuestions) {
            finishGame()
            return
        }

        _state.value = s.copy(
            questionIndex = nextIdx,
            currentQuestion = questions[nextIdx],
            chosenAnswer = null,
            isCorrect = null,
            showingFeedback = false,
            timeLeft = currentBand.quickGameTimerSec
        )
        startTimer()
    }

    private fun finishGame() {
        timerJob?.cancel()
        val s = _state.value
        _state.value = s.copy(isDone = true, isPlaying = false, showingFeedback = false)

        // Award XP and screen time
        viewModelScope.launch {
            val xp = s.score * 3 // 3 XP per correct quick game answer
            val earnedSeconds = (xp.toDouble() / 10 * 60 * currentBand.screenTimeRatio / 1.5).toInt().coerceAtLeast(60)

            val childId = parentalPrefsStore.activeChildId.first()

            if (childId != null) {
                // Multi-child mode
                childProfileDao.addXp(childId, xp)
                childProfileDao.updateScreenTimeBalance(childId, earnedSeconds)
            } else {
                // Solo mode
                val profile = userDao.getProfileOnce()
                if (profile != null) {
                    userDao.addXp(profile.id, xp)
                    userDao.updateScreenTimeBalance(profile.id, earnedSeconds)
                }
            }

            screenTimeDao.addLog(
                ScreenTimeLog(childId = childId ?: "", type = "EARNED", amountSeconds = earnedSeconds, source = "quick_game")
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (t in (currentBand.quickGameTimerSec - 1) downTo 0) {
                delay(1000)
                val current = _state.value
                if (current.showingFeedback || current.isDone) return@launch
                _state.value = current.copy(timeLeft = t)
                if (t == 0) {
                    onTimeout()
                    return@launch
                }
            }
        }
    }

    private fun generateQuestions(): List<QuickGameQuestion> {
        return (0 until currentBand.quickGameQuestions).map { generateOneQuestion() }
    }

    private fun generateOneQuestion(): QuickGameQuestion {
        return when (currentBand) {
            AgeBand.BALA -> {
                // Addition/subtraction only, single digit
                val isAddition = Random.nextBoolean()
                val a = Random.nextInt(1, 10)
                val b = Random.nextInt(1, 10)
                if (isAddition) {
                    QuickGameQuestion("$a + $b", a + b, generateWrongOptions(a + b))
                } else {
                    val big = maxOf(a, b)
                    val small = minOf(a, b)
                    QuickGameQuestion("$big - $small", big - small, generateWrongOptions(big - small))
                }
            }
            AgeBand.OQYSHY -> {
                // Multiply up to 5x9, divide by small numbers
                val isMultiplication = Random.nextFloat() > 0.4f
                val a = Random.nextInt(2, 6)  // 2-5
                val b = Random.nextInt(2, 10) // 2-9
                if (isMultiplication) {
                    QuickGameQuestion("$a \u00D7 $b", a * b, generateWrongOptions(a * b))
                } else {
                    QuickGameQuestion("${a * b} \u00F7 $b", a, generateWrongOptions(a))
                }
            }
            AgeBand.ZERDE -> {
                // Full multiplication/division
                val isMultiplication = Random.nextFloat() > 0.4f
                val a = Random.nextInt(2, 10)
                val b = Random.nextInt(2, 10)
                if (isMultiplication) {
                    QuickGameQuestion("$a \u00D7 $b", a * b, generateWrongOptions(a * b))
                } else {
                    QuickGameQuestion("${a * b} \u00F7 $b", a, generateWrongOptions(a))
                }
            }
        }
    }

    private fun generateWrongOptions(correct: Int): List<Int> {
        val wrongs = mutableSetOf<Int>()
        while (wrongs.size < 3) {
            val delta = Random.nextInt(1, 6) * if (Random.nextBoolean()) 1 else -1
            val wrong = correct + delta
            if (wrong > 0 && wrong != correct && wrong !in wrongs) wrongs.add(wrong)
        }
        return (wrongs + correct).toList().shuffled()
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
