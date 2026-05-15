package com.mektep.app.ui.parent

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mektep.app.data.local.AllowedAppDao
import com.mektep.app.data.models.AllowedApp
import com.mektep.app.ui.theme.MektepGreen
import com.mektep.app.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSelected: Boolean,
    val needsEarnedTime: Boolean
)

@HiltViewModel
class AppSelectorViewModel @Inject constructor(
    private val allowedAppDao: AllowedAppDao
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val apps: StateFlow<List<InstalledAppInfo>> = _apps.asStateFlow()

    private val _savedCount = MutableStateFlow(0)
    val savedCount: StateFlow<Int> = _savedCount.asStateFlow()

    // Packages to exclude from the list
    private val excludedPackages = setOf(
        "com.mektep.app",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.android.settings",
        "com.android.providers.settings",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin"
    )

    fun loadApps(pm: PackageManager) {
        viewModelScope.launch {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || isUserFacing(it, pm) }
                .filter { it.packageName !in excludedPackages }
                .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

            val allowed = allowedAppDao.getAppsForConfigOnce("local").associateBy { it.packageName }

            _apps.value = installed.map { app ->
                val existing = allowed[app.packageName]
                InstalledAppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    isSelected = existing != null,
                    needsEarnedTime = existing?.needsEarnedTime ?: true
                )
            }
            _savedCount.value = allowed.size
        }
    }

    fun toggleApp(packageName: String) {
        _apps.value = _apps.value.map {
            if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun toggleEarnedTime(packageName: String) {
        _apps.value = _apps.value.map {
            if (it.packageName == packageName) it.copy(needsEarnedTime = !it.needsEarnedTime) else it
        }
    }

    fun save() {
        viewModelScope.launch {
            allowedAppDao.clearForConfig("local")
            val selected = _apps.value.filter { it.isSelected }
            val entities = selected.map {
                AllowedApp(
                    configId = "local",
                    packageName = it.packageName,
                    appLabel = it.label,
                    needsEarnedTime = it.needsEarnedTime
                )
            }
            allowedAppDao.insertAll(entities)
            _savedCount.value = entities.size
        }
    }

    private fun isUserFacing(app: ApplicationInfo, pm: PackageManager): Boolean {
        return pm.getLaunchIntentForPackage(app.packageName) != null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    onBack: () -> Unit,
    lang: String = "en",
    viewModel: AppSelectorViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val savedCount by viewModel.savedCount.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadApps(context.packageManager) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("app_selector_title", lang), fontWeight = FontWeight.Bold)
                        Text("${apps.count { it.isSelected }} selected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = {
                        viewModel.save()
                        onBack()
                    }) {
                        Text(tr("save", lang), fontWeight = FontWeight.Bold, color = MektepGreen)
                    }
                }
            )
        }
    ) { padding ->
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    tr("app_selector_hint", lang),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(apps, key = { it.packageName }) { app ->
                val bgColor by animateColorAsState(
                    targetValue = if (app.isSelected) MektepGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                    label = "appBg"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleApp(app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = app.isSelected,
                            onCheckedChange = { viewModel.toggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(checkedColor = MektepGreen)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (app.isSelected) {
                            // Toggle: needs earned time vs always available
                            FilterChip(
                                selected = app.needsEarnedTime,
                                onClick = { viewModel.toggleEarnedTime(app.packageName) },
                                label = {
                                    Text(
                                        if (app.needsEarnedTime) "⏱️" else "✅",
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
