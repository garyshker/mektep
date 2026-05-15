package com.mektep.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.mektep.app.ui.theme.*
import com.mektep.app.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSubjectClick: (String) -> Unit,
    onScreenTimeClick: () -> Unit,
    onQuickGame: () -> Unit = {},
    onLogout: () -> Unit,
    onParentSettings: () -> Unit = {},
    onStartChildMode: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val language by viewModel.language.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mektep", fontWeight = FontWeight.Bold)
                        state.profile?.let {
                            Text(it.displayName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            // Stats
            state.profile?.let { profile ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(Icons.Default.LocalFireDepartment, "${profile.currentStreak}", tr("streak", language), MektepOrange)
                    StatChip(Icons.Default.Star, "${profile.xpTotal}", "XP", MektepGreen)
                    StatChip(Icons.Default.EmojiEvents, "Lv ${profile.xpTotal / 100 + 1}", tr("level", language), MektepBlue)
                }
            }

            // Screen Time Card
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

            // Quick Game card
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

            // Daily Quests
            if (state.quests.isNotEmpty()) {
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
                state.quests.forEachIndexed { idx, quest ->
                    var questVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(idx * 80L)
                        questVisible = true
                    }
                    AnimatedVisibility(
                        visible = questVisible,
                        enter = slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()
                    ) {
                        QuestRow(quest, language, onClaim = { viewModel.claimQuestReward(quest) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Parent controls (only show if mode is SAME_DEVICE or REMOTE_PARENT)
            val deviceMode by viewModel.deviceMode.collectAsState()
            if (deviceMode == "SAME_DEVICE") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartChildMode,
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

            Text(tr("subjects", language), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(state.subjects) { index, item ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 100L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.6f))
                    ) {
                        SubjectCard(item, language) { onSubjectClick(item.subject.id) }
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
private fun QuestRow(quest: com.mektep.app.data.models.DailyQuest, language: String, onClaim: () -> Unit) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
