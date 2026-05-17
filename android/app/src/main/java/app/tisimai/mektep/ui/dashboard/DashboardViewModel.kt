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
    val childProfile: ChildProfile? = null,
    val subjects: List<SubjectWithProgress> = emptyList(),
    val screenTimeMinutes: Int = 0,
    val quests: List<DailyQuest> = emptyList(),
    val questsCompletedCount: Int = 0,
    val ageBand: AgeBand? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userDao: UserDao,
    private val childProfileDao: ChildProfileDao,
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

    private val activeChildId: StateFlow<String?> = parentalPrefsStore.activeChildId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        ensureQuestsExist()
    }

    // Observe profile + progress + quests reactively, switching on activeChildId
    val uiState: StateFlow<DashboardUiState> = activeChildId.flatMapLatest { childId ->
        if (childId != null) {
            combine(
                childProfileDao.observeChild(childId),
                progressDao.getAllForChild(childId),
                questDao.getQuestsForChild(childId, today)
            ) { child, allProgress, quests ->
                buildUiState(
                    profile = null,
                    childProfile = child,
                    allProgress = allProgress,
                    quests = quests,
                    screenTimeSecs = child?.screenTimeBalanceSecs ?: 0
                )
            }
        } else {
            combine(
                userDao.getProfile(),
                progressDao.getAll(),
                questDao.getQuestsForDate(today)
            ) { profile, allProgress, quests ->
                buildUiState(
                    profile = profile,
                    childProfile = null,
                    allProgress = allProgress,
                    quests = quests,
                    screenTimeSecs = profile?.screenTimeBalanceSecs ?: 0
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun buildUiState(
        profile: UserProfile?,
        childProfile: ChildProfile?,
        allProgress: List<LessonProgress>,
        quests: List<DailyQuest>,
        screenTimeSecs: Int
    ): DashboardUiState {
        val gradeLevel = childProfile?.gradeLevel ?: profile?.gradeLevel ?: 6
        val ageBand = AgeBand.fromGradeLevel(gradeLevel)
        val subjects = lessonLoader.subjectsWithLessonsForGrade(gradeLevel)
        val subjectsWithProgress = subjects.map { subject ->
            val subjectLessons = lessonLoader.lessonsForSubject(subject.id, gradeLevel)
            val subjectProgress = allProgress.filter { it.subjectId == subject.id }
            SubjectWithProgress(
                subject = subject,
                completedLessons = subjectProgress.count { it.timesCompleted > 0 },
                totalLessons = subjectLessons.size,
                bestStars = subjectProgress.maxOfOrNull { it.bestStars } ?: 0
            )
        }
        return DashboardUiState(
            isLoading = false,
            profile = profile,
            childProfile = childProfile,
            subjects = subjectsWithProgress,
            screenTimeMinutes = screenTimeSecs / 60,
            quests = quests,
            questsCompletedCount = quests.count { it.completed },
            ageBand = ageBand
        )
    }

    fun claimQuestReward(quest: DailyQuest) {
        if (!quest.completed) return
        viewModelScope.launch {
            val childId = activeChildId.value
            if (childId != null) {
                childProfileDao.addXp(childId, quest.xpReward)
            } else {
                val profile = userDao.getProfileOnce() ?: return@launch
                userDao.addXp(profile.id, quest.xpReward)
            }
            // Mark as "claimed" by setting xpReward to 0 so we know it's been collected
            questDao.upsertQuest(quest.copy(xpReward = 0))
        }
    }

    private fun ensureQuestsExist() {
        viewModelScope.launch {
            // Clean old quests
            questDao.clearOldQuests(today)

            val childId = activeChildId.value ?: ""
            val existing = questDao.getQuestsForChildOnce(childId, today)
            if (existing.isNotEmpty()) {
                // Update quest progress based on current state
                updateQuestProgress(existing)
                return@launch
            }

            // Generate 4 new daily quests
            val quests = listOf(
                DailyQuest(
                    childId = childId,
                    id = "lesson_1_$today", type = "LESSON_COUNT", targetValue = 1,
                    xpReward = 10, date = today
                ),
                DailyQuest(
                    childId = childId,
                    id = "xp_50_$today", type = "XP_AMOUNT", targetValue = 50,
                    xpReward = 15, date = today
                ),
                DailyQuest(
                    childId = childId,
                    id = "quick_game_$today", type = "QUICK_GAME", targetValue = 1,
                    xpReward = 15, date = today
                ),
                DailyQuest(
                    childId = childId,
                    id = "streak_$today", type = "STREAK", targetValue = 1,
                    xpReward = 25, date = today
                )
            )
            questDao.insertAll(quests)
        }
    }

    private suspend fun updateQuestProgress(quests: List<DailyQuest>) {
        val childId = activeChildId.value
        val xpTotal: Int
        val currentStreak: Int

        if (childId != null) {
            val child = childProfileDao.getChild(childId) ?: return
            xpTotal = child.xpTotal
            currentStreak = child.currentStreak
        } else {
            val profile = userDao.getProfileOnce() ?: return
            xpTotal = profile.xpTotal
            currentStreak = profile.currentStreak
        }

        val progressChildId = childId ?: ""
        val todayProgress = progressDao.getAllForChild(progressChildId).first()
        val todayCompletions = todayProgress.count {
            it.lastCompletedAt != null && it.lastCompletedAt >= todayStartMillis()
        }

        for (quest in quests) {
            if (quest.xpReward == 0) continue // already claimed
            val (newValue, done) = when (quest.type) {
                "LESSON_COUNT" -> todayCompletions to (todayCompletions >= quest.targetValue)
                "XP_AMOUNT" -> xpTotal to (xpTotal >= quest.targetValue)
                "QUICK_GAME" -> {
                    // Check if quick game was played today (tracked via screen_time_log)
                    val played = if (quest.completed) 1 else 0
                    played to quest.completed
                }
                "STREAK" -> currentStreak to (currentStreak >= quest.targetValue)
                else -> quest.currentValue to quest.completed
            }
            if (newValue != quest.currentValue || done != quest.completed) {
                questDao.updateProgress(progressChildId, quest.id, newValue, done)
            }
        }
    }

    private fun todayStartMillis(): Long {
        return LocalDate.now().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
    }
}
