package app.tisimai.mektep.ui.pairing

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.remote.FirebaseFamilyRepository
import app.tisimai.mektep.ui.theme.MektepBlue
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──

data class PairingState(
    val isLoading: Boolean = false,
    val inviteCode: String? = null,
    val error: String? = null,
    val paired: Boolean = false,
    val mode: String = "" // "PARENT" or "CHILD"
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: FirebaseFamilyRepository,
    private val userDao: UserDao,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun createFamily() {
        viewModelScope.launch {
            _state.value = PairingState(isLoading = true, mode = "PARENT")
            try {
                val profile = userDao.getProfileOnce()
                val userId = profile?.id ?: "unknown"
                val name = profile?.displayName ?: "Parent"
                val code = repository.createFamily(name, userId)
                _state.value = PairingState(inviteCode = code, mode = "PARENT")
            } catch (e: Exception) {
                _state.value = PairingState(error = e.message ?: "Failed to create family", mode = "PARENT")
            }
        }
    }

    fun joinFamily(code: String) {
        viewModelScope.launch {
            _state.value = PairingState(isLoading = true, mode = "CHILD")
            try {
                val profile = userDao.getProfileOnce()
                val userId = profile?.id ?: "unknown"
                val name = profile?.displayName ?: "Student"
                val familyId = repository.joinFamily(code.uppercase(), userId, name)
                if (familyId != null) {
                    _state.value = PairingState(paired = true, mode = "CHILD")
                } else {
                    _state.value = PairingState(error = "Invalid or expired code", mode = "CHILD")
                }
            } catch (e: Exception) {
                _state.value = PairingState(error = e.message ?: "Failed to join", mode = "CHILD")
            }
        }
    }
}

// ── Parent Pairing Screen (shows invite code) ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPairingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang by viewModel.language.collectAsState()

    LaunchedEffect(Unit) { viewModel.createFamily() }
    LaunchedEffect(state.paired) { if (state.paired) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("pairing_parent_title", lang)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = MektepGreen)
                Spacer(Modifier.height(16.dp))
                Text(tr("generating_code", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (state.inviteCode != null) {
                Icon(Icons.Default.QrCode2, null, Modifier.size(64.dp), tint = MektepBlue)
                Spacer(Modifier.height(16.dp))
                Text(tr("share_code", lang), fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                // Big invite code
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MektepGreen.copy(alpha = 0.1f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.inviteCode!!,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MektepGreen,
                        letterSpacing = 6.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(tr("code_expires", lang), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(32.dp))

                // Waiting indicator
                val dots by rememberInfiniteTransition(label = "dots").animateFloat(
                    initialValue = 0f, targetValue = 3f,
                    animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
                    label = "dotsAnim"
                )
                Text(
                    tr("waiting_child", lang) + ".".repeat(dots.toInt() + 1),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))
                Button(onClick = onDone, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(tr("continue_btn", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else if (state.error != null) {
                Text("❌", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Child Pairing Screen (enter invite code) ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildPairingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang by viewModel.language.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state.paired) { if (state.paired) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("pairing_child_title", lang)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Devices, null, Modifier.size(64.dp), tint = MektepBlue)
            Spacer(Modifier.height(16.dp))
            Text(tr("enter_invite_code", lang), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(tr("ask_parent_code", lang), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it.uppercase() },
                label = { Text(tr("invite_code", lang)) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.joinFamily(code) },
                enabled = code.length == 6 && !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(tr("join_family", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
