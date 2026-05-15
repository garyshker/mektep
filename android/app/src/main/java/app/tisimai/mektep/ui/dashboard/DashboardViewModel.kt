package app.tisimai.mektep.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.*
import app.tisimai.mektep.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubjectWithProgress(
    val subject: Subject,
    val completedLessons: Int,
    val totalLessons: Int,
    val bestStars: Int
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val subjects: List<SubjectWithProgress> = emptyList(),
    val screenTimeMinutes: Int = 0,
    val quests: List<DailyQuest> = emptyList(),
    val questsCompletedCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userDao: UserDao,
    private val progressDao: ProgressDao,
    private val questDao: QuestDao,
    private val lessonLoader: LessonLoader,
    private val tokenStore: TokenStore,
    private val parentalPrefsStore: ParentalPrefsStore
) : ViewModel() {

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    val deviceMode: StateFlow<String> = parentalPrefsStore.deviceMode.stateIn(viewModelScope, SharingStarted.Eagerly, "NONE")
    val hasPinConfigured: StateFlow<Boolean> = parentalPrefsStore.pinHash.map { !it.isNullOrEmpty() }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val today = LocalDate.now().toString()

    init {
        ensureQuestsExist()
    }

    // Observe profile + progress + quests reactively
    val uiState: StateFlow<DashboardUiState> = combine(
        userDao.getProfile(),
        progressDao.getAll(),
        questDao.getQuestsForDate(today)
    ) { profile, allProgress, quests ->
        val subjects = lessonLoader.subjects
        val subjectsWithProgress = subjects.map { subject ->
            val subjectLessons = lessonLoader.lessonsForSubject(subject.id)
            val subjectProgress = allProgress.filter { it.subjectId == subject.id }
            SubjectWithProgress(
                subject = subject,
                completedLessons = subjectProgress.count { it.timesCompleted > 0 },
                totalLessons = subjectLessons.size,
                bestStars = subjectProgress.maxOfOrNull { it.bestStars } ?: 0
            )
        }

        DashboardUiState(
            isLoading = false,
            profile = profile,
            subjects = subjectsWithProgress,
            screenTimeMinutes = (profile?.screenTimeBalanceSecs ?: 0) / 60,
            quests = quests,
            questsCompletedCount = quests.count { it.completed }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun claimQuestReward(quest: DailyQuest) {
        if (!quest.completed) return
        viewModelScope.launch {
            val profile = userDao.getProfileOnce() ?: return@launch
            userDao.addXp(profile.id, quest.xpReward)
            // Mark as "claimed" by setting xpReward to 0 so we know it's been collected
            questDao.upsertQuest(quest.copy(xpReward = 0))
        }
    }

    private fun ensureQuestsExist() {
        viewModelScope.launch {
            // Clean old quests
            questDao.clearOldQuests(today)

            val existing = questDao.getQuestsForDateOnce(today)
            if (existing.isNotEmpty()) {
                // Update quest progress based on current state
                updateQuestProgress(existing)
                return@launch
            }

            // Generate 4 new daily quests
            val quests = listOf(
                DailyQuest(
                    id = "lesson_1_$today", type = "LESSON_COUNT", targetValue = 1,
                    xpReward = 10, date = today
                ),
                DailyQuest(
                    id = "xp_50_$today", type = "XP_AMOUNT", targetValue = 50,
                    xpReward = 15, date = today
                ),
                DailyQuest(
                    id = "quick_game_$today", type = "QUICK_GAME", targetValue = 1,
                    xpReward = 15, date = today
                ),
                DailyQuest(
                    id = "streak_$today", type = "STREAK", targetValue = 1,
                    xpReward = 25, date = today
                )
            )
            questDao.insertAll(quests)
        }
    }

    private suspend fun updateQuestProgress(quests: List<DailyQuest>) {
        val profile = userDao.getProfileOnce() ?: return
        val todayProgress = progressDao.getAll().firstOrNull() ?: emptyList()
        val todayCompletions = todayProgress.count {
            it.lastCompletedAt != null && it.lastCompletedAt >= todayStartMillis()
        }

        for (quest in quests) {
            if (quest.xpReward == 0) continue // already claimed
            val (newValue, done) = when (quest.type) {
                "LESSON_COUNT" -> todayCompletions to (todayCompletions >= quest.targetValue)
                "XP_AMOUNT" -> profile.xpTotal to (profile.xpTotal >= quest.targetValue)
                "QUICK_GAME" -> {
                    // Check if quick game was played today (tracked via screen_time_log)
                    val played = if (quest.completed) 1 else 0
                    played to quest.completed
                }
                "STREAK" -> profile.currentStreak to (profile.currentStreak >= quest.targetValue)
                else -> quest.currentValue to quest.completed
            }
            if (newValue != quest.currentValue || done != quest.completed) {
                questDao.updateProgress(quest.id, newValue, done)
            }
        }
    }

    private fun todayStartMillis(): Long {
        return LocalDate.now().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
    }
}
