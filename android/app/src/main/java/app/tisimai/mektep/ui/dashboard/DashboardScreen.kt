package app.tisimai.mektep.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.tisimai.mektep.data.models.AgeBand
import app.tisimai.mektep.ui.theme.*
import app.tisimai.mektep.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isChildMode: Boolean = false,
    onSubjectClick: (String) -> Unit,
    onScreenTimeClick: () -> Unit,
    onQuickGame: () -> Unit = {},
    onLogout: () -> Unit,
    onParentSettings: () -> Unit = {},
    onStartChildMode: () -> Unit = {},
    onSetupPin: () -> Unit = {},
    onBackToLauncher: () -> Unit = {},
    onAddChild: () -> Unit = {},
    onEditChild: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val language by viewModel.language.collectAsState()
    // Child mode is active when: Intent flag is set OR a child profile is active
    val effectiveChildMode = isChildMode || state.childProfile != null
    var childToDelete by remember { mutableStateOf<app.tisimai.mektep.data.models.ChildProfile?>(null) }
    var showParentPinDialog by remember { mutableStateOf(false) }

    // PIN dialog for parent takeover from child mode
    if (showParentPinDialog) {
        val setupVm: app.tisimai.mektep.ui.setup.SetupViewModel = hiltViewModel()
        val pinError by setupVm.pinError.collectAsState()
        app.tisimai.mektep.ui.components.PinEntryScreen(
            title = tr("parent_takeover", language),
            onPinComplete = { pin ->
                setupVm.verifyPin(pin) {
                    viewModel.exitChildMode()
                    showParentPinDialog = false
                }
            },
            onBack = { showParentPinDialog = false },
            error = pinError
        )
        return
    }

    // Delete confirmation dialog
    childToDelete?.let { child ->
        AlertDialog(
            onDismissRequest = { childToDelete = null },
            title = { Text(tr("delete_child_title", language)) },
            text = { Text(tr("delete_child_confirm", language, child.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChild(child)
                    childToDelete = null
                }) {
                    Text(tr("delete", language), color = MektepRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { childToDelete = null }) {
                    Text(tr("cancel", language))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val cp = state.childProfile
                    if (cp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cp.avatarEmoji, fontSize = 24.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(cp.name, fontWeight = FontWeight.Bold)
                                Text(
                                    tr(state.ageBand?.greetingKey ?: "", language),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column {
                            Text("BilimALL", fontWeight = FontWeight.Bold)
                            state.profile?.let {
                                Text(it.displayName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                actions = {
                    if (effectiveChildMode) {
                        // Child mode: lock icon → PIN required to switch to parent
                        IconButton(onClick = { showParentPinDialog = true }) {
                            Icon(Icons.Default.Lock, tr("parent_takeover", language), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    } else {
                        IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val deviceMode by viewModel.deviceMode.collectAsState()

        // Parent view: if children exist, this is a parent — regardless of deviceMode setup
        val isParentView = !effectiveChildMode && state.children.isNotEmpty()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
        ) {
            if (isParentView) {
                // ── Parent View: show children's progress ──
                item {
                    Text(tr("children", language), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(state.children.size) { index ->
                    val child = state.children[index]
                    val band = AgeBand.fromGradeLevel(child.gradeLevel)
                    val bandColor = when (band) {
                        AgeBand.BOLASHAK -> MektepOrange
                        AgeBand.BALA -> MektepGreen
                        AgeBand.OQYSHY -> MektepBlue
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bandColor.copy(alpha = 0.08f))
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(child.avatarEmoji, fontSize = 36.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(child.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tr(band.labelKey, language), fontSize = 11.sp, color = bandColor, fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("\uD83D\uDD25 ${child.currentStreak}", fontSize = 13.sp)
                                    Text("⭐ ${child.xpTotal} XP", fontSize = 13.sp, color = MektepGreen)
                                    Text("⏱ ${child.screenTimeBalanceSecs / 60} ${tr("min", language)}", fontSize = 13.sp)
                                }
                            }
                            Column {
                                IconButton(onClick = { onEditChild(child.id) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { childToDelete = child }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MektepRed.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
                // Add child button
                item {
                    OutlinedButton(
                        onClick = onAddChild,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MektepGreen)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("add_child", language), fontSize = 14.sp)
                    }
                }
            } else {
                // ── Learner View: show own stats ──
                val displayProfile = state.childProfile
                val xp = displayProfile?.xpTotal ?: state.profile?.xpTotal ?: 0
                val streak = displayProfile?.currentStreak ?: state.profile?.currentStreak ?: 0

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip(Icons.Default.LocalFireDepartment, "$streak", tr("streak", language), MektepOrange)
                        StatChip(Icons.Default.Star, "$xp", "XP", MektepGreen)
                        StatChip(Icons.Default.EmojiEvents, "Lv ${xp / 100 + 1}", tr("level", language), MektepBlue)
                    }
                }

                // Screen Time Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onScreenTimeClick() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tr("screen_time", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${state.screenTimeMinutes} ${tr("minutes_available", language)}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }

            // ── Child/Learner-only content (hidden for parent view) ──
            if (!isParentView) {

            // 1. Subjects FIRST — child came to learn
            item {
                Text(tr("subjects", language), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))
            }

            val subjects = state.subjects
            val rows = subjects.chunked(2)
            itemsIndexed(rows) { rowIndex, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEachIndexed { colIndex, item ->
                        val index = rowIndex * 2 + colIndex
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 100L)
                            visible = true
                        }
                        Box(Modifier.weight(1f)) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.6f))
                            ) {
                                SubjectCard(item, language) { onSubjectClick(item.subject.id) }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 2. Quick Game
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onQuickGame() },
                    colors = CardDefaults.cardColors(containerColor = MektepOrange.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("quick_game", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(tr("quick_game_desc", language), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MektepOrange)
                    }
                }
            }

            // 3. Daily Quests
            if (state.quests.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("daily_quests", language), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${state.questsCompletedCount}/${state.quests.size}",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                itemsIndexed(state.quests) { idx, quest ->
                    var questVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(idx * 80L)
                        questVisible = true
                    }
                    AnimatedVisibility(
                        visible = questVisible,
                        enter = slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()
                    ) {
                        QuestRow(
                            quest = quest,
                            language = language,
                            onClaim = { viewModel.claimQuestReward(quest) },
                            onTap = {
                                when (quest.type) {
                                    "LESSON_COUNT" -> state.subjects.firstOrNull()?.let { onSubjectClick(it.subject.id) }
                                    "QUICK_GAME" -> onQuickGame()
                                    "XP_AMOUNT" -> state.subjects.firstOrNull()?.let { onSubjectClick(it.subject.id) }
                                    "STREAK" -> state.subjects.firstOrNull()?.let { onSubjectClick(it.subject.id) }
                                }
                            }
                        )
                    }
                }
            }

            } // end if (!isParentView)

            // Parent controls (hidden in child mode)
            if (isParentView) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hasPinConfigured by viewModel.hasPinConfigured.collectAsState()
                        Button(
                            onClick = { if (hasPinConfigured) onStartChildMode() else onSetupPin() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MektepOrange)
                        ) {
                            Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("start_child_mode", language), fontSize = 14.sp)
                        }
                        OutlinedButton(
                            onClick = onParentSettings,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Settings, null, Modifier.size(18.dp))
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, sublabel: String, color: Color) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "statScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(sublabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectCard(item: SubjectWithProgress, language: String, onClick: () -> Unit) {
    val subjectColor = when (item.subject.colorKey) {
        "math" -> MathColor; "kazakh" -> KazakhColor; "english" -> EnglishColor; "world" -> WorldColor
        else -> MektepGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = subjectColor.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.subject.emoji, fontSize = 28.sp)
                // Star rating
                if (item.bestStars > 0) {
                    Row {
                        repeat(3) { i ->
                            Text(
                                if (i < item.bestStars) "⭐" else "☆",
                                fontSize = if (i < item.bestStars) 14.sp else 12.sp
                            )
                        }
                    }
                }
            }
            Column {
                Text(item.subject.name[language] ?: item.subject.name["en"] ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (item.totalLessons > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.completedLessons.toFloat() / item.totalLessons },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = subjectColor,
                    )
                    Text("${item.completedLessons}/${item.totalLessons} ${tr("lessons", language)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QuestRow(quest: app.tisimai.mektep.data.models.DailyQuest, language: String, onClaim: () -> Unit, onTap: () -> Unit = {}) {
    val questEmoji = when (quest.type) {
        "LESSON_COUNT" -> "📚"
        "XP_AMOUNT" -> "⭐"
        "QUICK_GAME" -> "⚡"
        "STREAK" -> "🔥"
        else -> "🎯"
    }
    val questText = when (quest.type) {
        "LESSON_COUNT" -> tr("quest_complete_lessons", language, quest.targetValue)
        "XP_AMOUNT" -> tr("quest_earn_xp", language, quest.targetValue)
        "QUICK_GAME" -> tr("quest_play_quick_game", language)
        "STREAK" -> tr("quest_streak", language)
        else -> quest.type
    }

    val isClaimed = quest.xpReward == 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
            if (quest.completed && !isClaimed) onClaim()
            else if (!quest.completed) onTap()
        },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isClaimed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                quest.completed -> MektepGreen.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(questEmoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(questText, fontSize = 14.sp, fontWeight = if (quest.completed) FontWeight.Bold else FontWeight.Normal)
                if (!quest.completed && quest.targetValue > 1) {
                    LinearProgressIndicator(
                        progress = { (quest.currentValue.toFloat() / quest.targetValue).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = MektepGreen,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (isClaimed) {
                Text("✅", fontSize = 18.sp)
            } else if (quest.completed) {
                TextButton(onClick = onClaim) {
                    Text("+${quest.xpReward} XP", color = MektepGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Text("+${quest.xpReward} XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
