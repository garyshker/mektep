package com.mektep.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mektep.app.data.local.TokenStore
import com.mektep.app.data.models.*
import com.mektep.app.data.repository.LessonRepository
import com.mektep.app.data.repository.ScreenTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: ChildProfile? = null,
    val subjects: List<Subject> = emptyList(),
    val progress: List<SubjectProgress> = emptyList(),
    val screenTimeBalance: ScreenTimeBalance? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val screenTimeRepository: ScreenTimeRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val childId = tokenStore.childId.firstOrNull()
                val subjects = lessonRepository.getSubjects()
                _uiState.value = _uiState.value.copy(subjects = subjects)

                if (childId != null) {
                    val dashboard = lessonRepository.getDashboard(childId)
                    val balance = screenTimeRepository.getBalance(childId)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = dashboard.profile,
                        progress = dashboard.progress,
                        screenTimeBalance = balance
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
