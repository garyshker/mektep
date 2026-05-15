package app.tisimai.mektep.ui.screentime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.UserProfile
import app.tisimai.mektep.ui.theme.MektepGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = userDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeScreen(
    onBack: () -> Unit,
    viewModel: ScreenTimeViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Time") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Timer, null, Modifier.size(64.dp), tint = MektepGreen)
                    Spacer(Modifier.height(16.dp))

                    val totalSecs = profile?.screenTimeBalanceSecs ?: 0
                    val hours = totalSecs / 3600
                    val mins = (totalSecs % 3600) / 60

                    Text(
                        text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                        fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MektepGreen
                    )
                    Text("available screen time", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(32.dp))

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, null, tint = MektepGreen)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Earn More Time", fontWeight = FontWeight.Bold)
                        Text("Complete lessons to earn screen time!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("How It Works", fontWeight = FontWeight.Bold)
                        Text("Each correct answer earns 5 XP. 10 XP = 1 minute of screen time. Bonus XP for high accuracy!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
