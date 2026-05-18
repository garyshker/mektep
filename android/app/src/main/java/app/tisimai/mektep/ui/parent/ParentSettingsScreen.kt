package app.tisimai.mektep.ui.parent

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.AllowedAppDao
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.ParentalConfigDao
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.AgeBand
import app.tisimai.mektep.data.models.ChildProfile
import app.tisimai.mektep.data.models.ParentalConfig
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepOrange
import app.tisimai.mektep.ui.theme.MektepRed
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configDao: ParentalConfigDao,
    private val allowedAppDao: AllowedAppDao,
    private val prefsStore: ParentalPrefsStore,
    private val childProfileDao: ChildProfileDao,
    private val userDao: UserDao
) : ViewModel() {

    val config: StateFlow<ParentalConfig?> = configDao.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _allowedAppCount = MutableStateFlow(0)
    val allowedAppCount: StateFlow<Int> = _allowedAppCount.asStateFlow()

    private val _children = MutableStateFlow<List<ChildProfile>>(emptyList())
    val children: StateFlow<List<ChildProfile>> = _children.asStateFlow()

    // ── Permission status flows ──

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _hasAccessibilityService = MutableStateFlow(false)
    val hasAccessibilityService: StateFlow<Boolean> = _hasAccessibilityService.asStateFlow()

    init {
        viewModelScope.launch {
            _allowedAppCount.value = allowedAppDao.getAppsForConfigOnce("local").size
        }
        viewModelScope.launch {
            val user = userDao.getProfileOnce() ?: return@launch
            childProfileDao.getChildrenForParent(user.id).collect { list ->
                _children.value = list
            }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        // Usage Access
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        _hasUsageAccess.value = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED

        // Display Over Apps
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)

        // Accessibility Service
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        _hasAccessibilityService.value = enabledServices.contains(context.packageName)
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

    fun updateChildDailyLimit(childId: String, minutes: Int) {
        viewModelScope.launch {
            childProfileDao.updateDailyLimit(childId, minutes)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    onBack: () -> Unit,
    onSelectApps: () -> Unit,
    onAddChild: () -> Unit = {},
    lang: String = "en",
    viewModel: ParentSettingsViewModel = hiltViewModel()
) {
    val allowedAppCount by viewModel.allowedAppCount.collectAsState()
    val children by viewModel.children.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    val hasAccessibilityService by viewModel.hasAccessibilityService.collectAsState()
    val context = LocalContext.current

    // Refresh permission status when screen resumes
    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    val anyPermissionMissing = !hasUsageAccess || !hasOverlayPermission || !hasAccessibilityService

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
            // ── Permissions section ──
            if (anyPermissionMissing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MektepRed.copy(alpha = 0.1f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MektepRed, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(tr("permissions_required", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(tr("permissions_desc", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(tr("permissions_required", lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))

            PermissionRow(
                icon = Icons.Default.QueryStats,
                label = tr("usage_access", lang),
                granted = hasUsageAccess,
                lang = lang,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )
            PermissionRow(
                icon = Icons.Default.Layers,
                label = tr("overlay_permission", lang),
                granted = hasOverlayPermission,
                lang = lang,
                onClick = {
                    context.startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                }
            )
            PermissionRow(
                icon = Icons.Default.Accessibility,
                label = tr("accessibility_service", lang),
                granted = hasAccessibilityService,
                lang = lang,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )

            Spacer(Modifier.height(20.dp))

            // Children section
            Text(tr("children", lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))

            if (children.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            tr("no_children_added", lang),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                children.forEach { child ->
                    var childDailyLimit by remember(child.id, child.dailyLimitMinutes) {
                        mutableFloatStateOf(child.dailyLimitMinutes.toFloat())
                    }
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(child.avatarEmoji, fontSize = 32.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(child.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    val age = try {
                                        Period.between(LocalDate.parse(child.birthDate), LocalDate.now()).years
                                    } catch (_: Exception) { null }
                                    Text(
                                        buildString {
                                            if (age != null) append("$age ${tr("age_years", lang)} | ")
                                            append("${tr("grade", lang)} ${child.gradeLevel}")
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val band = AgeBand.fromGradeLevel(child.gradeLevel)
                                    Text(
                                        tr(band.labelKey, lang),
                                        fontSize = 12.sp,
                                        color = MektepGreen
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${tr("daily_limit_child", lang)}: ${childDailyLimit.toInt()} ${tr("minutes", lang)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = childDailyLimit,
                                onValueChange = { childDailyLimit = it },
                                onValueChangeFinished = { viewModel.updateChildDailyLimit(child.id, childDailyLimit.toInt()) },
                                valueRange = 15f..240f,
                                steps = 14,
                                colors = SliderDefaults.colors(thumbColor = MektepGreen, activeTrackColor = MektepGreen)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAddChild) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(tr("add_child", lang))
            }

            Spacer(Modifier.height(20.dp))

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

        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    lang: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (granted) MektepGreen else MektepRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    if (granted) tr("permission_granted", lang) else tr("permission_not_granted", lang),
                    fontSize = 12.sp,
                    color = if (granted) MektepGreen else MektepRed
                )
            }
            Text(
                if (granted) "\u2713" else "\u2717",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (granted) MektepGreen else MektepRed
            )
        }
    }
}
