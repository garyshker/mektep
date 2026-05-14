package com.mektep.app.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.mektep.app.data.api.MektepApi
import com.mektep.app.data.local.TokenStore
import com.mektep.app.data.models.*
import com.mektep.app.ui.theme.MektepGreen
import com.mektep.app.ui.theme.MektepOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentUiState(
    val isLoading: Boolean = true,
    val family: Family? = null,
    val members: List<FamilyMember> = emptyList(),
    val inviteCode: String? = null,
    val error: String? = null
)

@HiltViewModel
class ParentViewModel @Inject constructor(
    private val api: MektepApi,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentUiState())
    val uiState: StateFlow<ParentUiState> = _uiState.asStateFlow()

    init {
        loadFamily()
    }

    private fun loadFamily() {
        viewModelScope.launch {
            _uiState.value = ParentUiState(isLoading = true)
            // Parent dashboard will show family members and controls
            _uiState.value = ParentUiState(isLoading = false)
        }
    }

    fun createFamily(name: String) {
        viewModelScope.launch {
            try {
                val family = api.createFamily(CreateFamilyRequest(name))
                _uiState.value = _uiState.value.copy(family = family)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun generateInvite() {
        viewModelScope.launch {
            val familyId = _uiState.value.family?.id ?: return@launch
            try {
                val invite = api.generateInvite(familyId)
                _uiState.value = _uiState.value.copy(inviteCode = invite.code)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun grantBonus(childId: String, minutes: Int) {
        viewModelScope.launch {
            try {
                api.grantBonus(childId, BonusRequest(minutes, "Parent bonus"))
            } catch (_: Exception) { }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ParentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var familyName by remember { mutableStateOf("") }
    var showCreateFamily by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Family Management
            Text("Family", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))

            if (state.family == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Create a family to link with your children", fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            label = { Text("Family Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.createFamily(familyName) },
                            enabled = familyName.isNotBlank()
                        ) {
                            Text("Create Family")
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.family!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))

                        if (state.inviteCode != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Invite Code:", fontSize = 12.sp)
                                    Text(state.inviteCode!!, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MektepGreen)
                                    Text("Share this code with your child", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Button(onClick = { viewModel.generateInvite() }) {
                                Icon(Icons.Default.PersonAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Invite Code")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Screen Time Controls
            Text("Screen Time Controls", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = MektepOrange)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Points per Minute", fontWeight = FontWeight.Medium)
                            Text("How many XP points equal 1 minute of screen time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Default: 10 XP = 1 minute", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Bedtime Mode", fontWeight = FontWeight.Medium)
                            Text("Block all apps during bedtime hours", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Not configured yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("App Blocking", fontWeight = FontWeight.Medium)
                            Text("Manage which apps require earned screen time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Configure in device settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick Actions
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { /* Grant 15 min bonus */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+15 min")
                }
                OutlinedButton(
                    onClick = { /* Grant 30 min bonus */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+30 min")
                }
                OutlinedButton(
                    onClick = { /* Emergency unlock */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Unlock")
                }
            }
        }
    }
}
