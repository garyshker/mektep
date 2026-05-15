package com.mektep.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mektep.app.data.local.LessonLoader
import com.mektep.app.data.local.ProgressDao
import com.mektep.app.data.local.TokenStore
import com.mektep.app.data.local.UserDao
import com.mektep.app.data.models.LessonProgress
import com.mektep.app.data.models.Subject
import com.mektep.app.data.models.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val screenTimeMinutes: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userDao: UserDao,
    private val progressDao: ProgressDao,
    private val lessonLoader: LessonLoader,
    private val tokenStore: TokenStore
) : ViewModel() {

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    // Observe profile reactively — auto-updates when XP/screen time changes
    val uiState: StateFlow<DashboardUiState> = combine(
        userDao.getProfile(),
        progressDao.getAll()
    ) { profile, allProgress ->
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
            screenTimeMinutes = (profile?.screenTimeBalanceSecs ?: 0) / 60
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
