package app.tisimai.mektep.ui.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import app.tisimai.mektep.data.local.AllowedAppDao
import app.tisimai.mektep.data.local.ParentalConfigDao
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.models.ParentalConfig
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepOrange
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    private val configDao: ParentalConfigDao,
    private val allowedAppDao: AllowedAppDao,
    private val prefsStore: ParentalPrefsStore
) : ViewModel() {

    val config: StateFlow<ParentalConfig?> = configDao.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _allowedAppCount = MutableStateFlow(0)
    val allowedAppCount: StateFlow<Int> = _allowedAppCount.asStateFlow()

    init {
        viewModelScope.launch {
            _allowedAppCount.value = allowedAppDao.getAppsForConfigOnce("local").size
        }
    }

    fun updateDailyLimit(minutes: Int) {
        viewModelScope.launch {
            val c = configDao.getConfigOnce() ?: return@launch
            configDao.upsertConfig(c.copy(dailyLimitMinutes = minutes))
        }
    }

    fun updateBedtime(start: String?, end: String?) {
        viewModelScope.launch {
            val c = configDao.getConfigOnce() ?: return@launch
            configDao.upsertConfig(c.copy(bedtimeStart = start, bedtimeEnd = end))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    onBack: () -> Unit,
    onSelectApps: () -> Unit,
    lang: String = "en",
    viewModel: ParentSettingsViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val allowedAppCount by viewModel.allowedAppCount.collectAsState()
    var dailyLimit by remember(config) { mutableFloatStateOf((config?.dailyLimitMinutes ?: 60).toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("parent_settings", lang), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            // App selector
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectApps() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apps, null, tint = MektepGreen, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("allowed_apps", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            tr("apps_selected", lang, allowedAppCount),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Daily limit slider
            Text(tr("daily_limit", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${dailyLimit.toInt()} ${tr("minutes", lang)}",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MektepGreen
            )
            Slider(
                value = dailyLimit,
                onValueChange = { dailyLimit = it },
                onValueChangeFinished = { viewModel.updateDailyLimit(dailyLimit.toInt()) },
                valueRange = 15f..240f,
                steps = 14, // 15-min increments
                colors = SliderDefaults.colors(thumbColor = MektepGreen, activeTrackColor = MektepGreen)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("15m", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("4h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))

            // Bedtime
            Text(tr("bedtime", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            config?.bedtimeStart?.let { "${config?.bedtimeStart} — ${config?.bedtimeEnd}" } ?: tr("not_set", lang),
                            fontSize = 16.sp
                        )
                        Text(tr("bedtime_desc", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Screen time ratio
            Text(tr("screen_time_ratio", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = MektepOrange)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("1 min → 1.5 min", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(tr("screen_time_ratio_desc", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
