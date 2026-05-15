package app.tisimai.mektep.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.ParentalConfigDao
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.remote.ChildStatus
import app.tisimai.mektep.data.remote.FirebaseFamilyRepository
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepOrange
import app.tisimai.mektep.ui.theme.MektepRed
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentDashState(
    val isLoading: Boolean = true,
    val familyId: String? = null,
    val inviteCode: String? = null,
    val childStatuses: Map<String, ChildStatus> = emptyMap(),
    val bonusGranted: Boolean = false
)

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val repository: FirebaseFamilyRepository,
    private val configDao: ParentalConfigDao,
    private val userDao: UserDao,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow(ParentDashState())
    val state: StateFlow<ParentDashState> = _state.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    init {
        loadFamily()
    }

    private fun loadFamily() {
        viewModelScope.launch {
            val config = configDao.getConfigOnce()
            val familyId = config?.familyId
            _state.value = ParentDashState(isLoading = false, familyId = familyId, inviteCode = config?.inviteCode)

            if (familyId != null) {
                repository.observeChildStatuses(familyId).collect { statuses ->
                    _state.value = _state.value.copy(childStatuses = statuses)
                }
            }
        }
    }

    fun grantBonus(childId: String, minutes: Int) {
        viewModelScope.launch {
            val familyId = _state.value.familyId ?: return@launch
            val profile = userDao.getProfileOnce()
            repository.grantBonusTime(familyId, childId, minutes * 60, profile?.id ?: "parent")
            _state.value = _state.value.copy(bonusGranted = true)
            kotlinx.coroutines.delay(2000)
            _state.value = _state.value.copy(bonusGranted = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentRemoteDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ParentDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang by viewModel.language.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("parent_dashboard", lang), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            // Invite code display
            if (state.inviteCode != null) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(tr("invite_code", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.inviteCode!!, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MektepGreen, letterSpacing = 4.sp)
                        Text(tr("share_code", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Connected children
            Text(tr("connected_children", lang), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))

            if (state.childStatuses.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👶", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(tr("no_children_yet", lang), fontWeight = FontWeight.Medium)
                        Text(tr("share_invite", lang), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            state.childStatuses.forEach { (childId, status) ->
                ChildStatusCard(childId, status, lang, onGrantBonus = { mins -> viewModel.grantBonus(childId, mins) })
                Spacer(Modifier.height(12.dp))
            }

            // Bonus granted snackbar
            if (state.bonusGranted) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(tr("bonus_granted", lang), fontWeight = FontWeight.Bold, color = MektepGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildStatusCard(
    childId: String,
    status: ChildStatus,
    lang: String,
    onGrantBonus: (Int) -> Unit
) {
    val balanceMin = status.balanceSecs / 60
    val statusColor = if (status.childModeActive) MektepOrange else MektepGreen

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status dot
                Icon(
                    if (status.childModeActive) Icons.Default.PhoneAndroid else Icons.Default.CheckCircle,
                    null, tint = statusColor, modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(childId.take(8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (status.childModeActive) tr("child_mode_active", lang) else tr("child_mode_inactive", lang),
                        fontSize = 13.sp, color = statusColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${balanceMin}m", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MektepGreen)
                    Text(tr("remaining", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (status.currentApp != null) {
                Spacer(Modifier.height(8.dp))
                Text("📱 ${status.currentApp}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))

            // Quick bonus buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 15, 30).forEach { mins ->
                    OutlinedButton(
                        onClick = { onGrantBonus(mins) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+${mins}m", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
