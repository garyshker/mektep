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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSubjectClick: (String) -> Unit,
    onScreenTimeClick: () -> Unit,
    onLogout: () -> Unit,
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
                    StatChip(Icons.Default.LocalFireDepartment, "${profile.currentStreak}", "Streak", MektepOrange)
                    StatChip(Icons.Default.Star, "${profile.xpTotal}", "XP", MektepGreen)
                    StatChip(Icons.Default.EmojiEvents, "Lv ${profile.xpTotal / 100 + 1}", "Level", MektepBlue)
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
                        Text("Screen Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${state.screenTimeMinutes} minutes available", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }

            Text("Subjects", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))

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
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = subjectColor.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(item.subject.emoji, fontSize = 28.sp)
            Column {
                Text(item.subject.name[language] ?: item.subject.name["en"] ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (item.totalLessons > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.completedLessons.toFloat() / item.totalLessons },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = subjectColor,
                    )
                    Text("${item.completedLessons}/${item.totalLessons} lessons", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
