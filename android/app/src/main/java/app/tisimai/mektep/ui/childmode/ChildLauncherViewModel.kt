package app.tisimai.mektep.ui.childmode

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.*
import app.tisimai.mektep.data.models.ChildSession
import app.tisimai.mektep.data.models.ScreenTimeLog
import app.tisimai.mektep.services.ScreenTimePrefs
import app.tisimai.mektep.services.ScreenTimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ChildLauncherState(
    val isLoading: Boolean = true,
    val apps: List<LauncherApp> = emptyList(),
    val balanceSeconds: Int = 0,
    val sessionId: Long = 0,
    val showBreakReminder: Boolean = false
)

@HiltViewModel
class ChildLauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

            // Write initial state to ScreenTimePrefs for cross-service communication
            val screenTimePrefs = ScreenTimePrefs(context)
            screenTimePrefs.activeChildId = resolvedChildId
            screenTimePrefs.balanceSeconds = balance
            screenTimePrefs.isChildModeActive = true
            screenTimePrefs.isOverlayShowing = false

            // Start the ScreenTimeService (source of truth for countdown)
            ScreenTimeService.start(context, resolvedChildId ?: "")

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
            val stPrefs = ScreenTimePrefs(context)
            while (isActive) {
                delay(1000)
                // Read balance from ScreenTimePrefs — ScreenTimeService is the source of truth
                val balance = stPrefs.balanceSeconds
                val current = _state.value
                val elapsed = sessionStartBalance - balance

                if (elapsed >= 1500 && !current.showBreakReminder) {
                    _state.value = current.copy(balanceSeconds = balance, showBreakReminder = true)
                } else {
                    _state.value = current.copy(balanceSeconds = balance)
                }

                if (balance <= 0) break
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

    fun dismissBreakReminder() {
        _state.value = _state.value.copy(showBreakReminder = false)
    }

    fun deactivateChildMode() {
        countdownJob?.cancel()

        // Stop the ScreenTimeService and clear prefs
        ScreenTimeService.stop(context)
        val screenTimePrefs = ScreenTimePrefs(context)
        screenTimePrefs.isChildModeActive = false
        screenTimePrefs.isOverlayShowing = false

        viewModelScope.launch {
            parentalPrefsStore.setChildModeActive(false)
            parentalConfigDao.setChildModeActive("local", false)
            parentalPrefsStore.setActiveChildId(null)

            // Restore parent's language
            val parentLang = parentalPrefsStore.parentLanguage.first()
            if (parentLang != null) {
                tokenStore.saveLanguage(parentLang)
            }

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
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
