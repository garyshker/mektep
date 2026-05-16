package app.tisimai.mektep.ui.childmode

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.*
import app.tisimai.mektep.data.models.ChildSession
import app.tisimai.mektep.data.models.ScreenTimeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ChildLauncherState(
    val isLoading: Boolean = true,
    val apps: List<LauncherApp> = emptyList(),
    val balanceSeconds: Int = 0,
    val sessionId: Long = 0
)

@HiltViewModel
class ChildLauncherViewModel @Inject constructor(
    private val userDao: UserDao,
    private val childProfileDao: ChildProfileDao,
    private val allowedAppDao: AllowedAppDao,
    private val childSessionDao: ChildSessionDao,
    private val screenTimeDao: ScreenTimeDao,
    private val parentalConfigDao: ParentalConfigDao,
    private val parentalPrefsStore: ParentalPrefsStore,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow(ChildLauncherState())
    val state: StateFlow<ChildLauncherState> = _state.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    private var countdownJob: Job? = null
    private var sessionStartBalance = 0
    private var resolvedChildId: String? = null

    fun loadApps(pm: PackageManager) {
        viewModelScope.launch {
            val allowed = allowedAppDao.getAppsForConfigOnce("local")
            val apps = allowed.map { LauncherApp(it.packageName, it.appLabel, it.needsEarnedTime) }

            val childId = parentalPrefsStore.activeChildId.first()
            resolvedChildId = childId

            val balance: Int
            if (childId != null) {
                val child = childProfileDao.getChild(childId)
                balance = child?.screenTimeBalanceSecs ?: 0
            } else {
                val profile = userDao.getProfileOnce()
                balance = profile?.screenTimeBalanceSecs ?: 0
            }
            sessionStartBalance = balance

            // Start a child session
            val sessionId = childSessionDao.startSession(
                ChildSession(childId = childId ?: "", initialBalanceSecs = balance)
            )

            // Mark child mode active
            parentalPrefsStore.setChildModeActive(true)
            parentalConfigDao.setChildModeActive("local", true)

            _state.value = ChildLauncherState(
                isLoading = false,
                apps = apps,
                balanceSeconds = balance,
                sessionId = sessionId
            )

            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _state.value
                if (current.balanceSeconds <= 0) break

                val newBalance = current.balanceSeconds - 1
                _state.value = current.copy(balanceSeconds = newBalance)

                // Persist to DB every 10 seconds
                if (newBalance % 10 == 0) {
                    val childId = resolvedChildId
                    if (childId != null) {
                        childProfileDao.updateScreenTimeBalance(childId, -10)
                    } else {
                        val profile = userDao.getProfileOnce()
                        if (profile != null) {
                            userDao.updateScreenTimeBalance(profile.id, -10)
                        }
                    }
                }
            }

            // Time's up — log the session
            val consumed = sessionStartBalance - _state.value.balanceSeconds
            if (_state.value.sessionId > 0) {
                childSessionDao.endSession(
                    _state.value.sessionId,
                    System.currentTimeMillis(),
                    consumed,
                    "TIME_UP"
                )
            }

            // Log screen time spent
            screenTimeDao.addLog(
                ScreenTimeLog(childId = resolvedChildId ?: "", type = "SPENT", amountSeconds = consumed, source = "child_mode")
            )
        }
    }

    fun deactivateChildMode() {
        countdownJob?.cancel()
        viewModelScope.launch {
            parentalPrefsStore.setChildModeActive(false)
            parentalConfigDao.setChildModeActive("local", false)

            // End session
            val consumed = sessionStartBalance - _state.value.balanceSeconds
            if (_state.value.sessionId > 0) {
                childSessionDao.endSession(
                    _state.value.sessionId,
                    System.currentTimeMillis(),
                    consumed,
                    "PARENT_EXIT"
                )
            }

            // Sync final balance to DB
            val totalSpent = sessionStartBalance - _state.value.balanceSeconds
            val alreadyDeducted = (totalSpent / 10) * 10
            val remainder = totalSpent - alreadyDeducted
            if (remainder > 0) {
                val childId = resolvedChildId
                if (childId != null) {
                    childProfileDao.updateScreenTimeBalance(childId, -remainder)
                } else {
                    val profile = userDao.getProfileOnce()
                    if (profile != null) {
                        userDao.updateScreenTimeBalance(profile.id, -remainder)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
