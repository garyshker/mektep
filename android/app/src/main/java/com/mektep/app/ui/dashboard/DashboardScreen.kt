package com.mektep.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mektep.app.data.models.Subject
import com.mektep.app.data.models.SubjectProgress
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
                            Text(
                                it.displayName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Row
            state.profile?.let { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(icon = Icons.Default.LocalFireDepartment, label = "${profile.currentStreak}", sublabel = "Streak", color = MektepOrange)
                    StatChip(icon = Icons.Default.Star, label = "${profile.xpTotal}", sublabel = "XP", color = MektepGreen)
                    StatChip(icon = Icons.Default.EmojiEvents, label = "Lv ${profile.xpTotal / 100 + 1}", sublabel = "Level", color = MektepBlue)
                }
            }

            // Screen Time Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onScreenTimeClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Screen Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "${state.screenTimeBalance?.balanceMinutes ?: 0} minutes available",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // Subjects Header
            Text(
                "Subjects",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Subject Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.subjects) { subject ->
                    val progress = state.progress.find { it.subjectId == subject.id }
                    SubjectCard(
                        subject = subject,
                        progress = progress,
                        language = language,
                        onClick = { onSubjectClick(subject.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(sublabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    progress: SubjectProgress?,
    language: String,
    onClick: () -> Unit
) {
    val subjectColor = when {
        subject.colorScheme.contains("math", true) -> MathColor
        subject.colorScheme.contains("kaz", true) -> KazakhColor
        subject.colorScheme.contains("eng", true) -> EnglishColor
        subject.colorScheme.contains("world", true) -> WorldColor
        else -> MektepGreen
    }

    val subjectEmoji = when {
        subject.name["en"]?.contains("Math", true) == true -> "📐"
        subject.name["en"]?.contains("Kazakh", true) == true -> "🇰🇿"
        subject.name["en"]?.contains("English", true) == true -> "🇬🇧"
        subject.name["en"]?.contains("World", true) == true -> "🌍"
        else -> "📚"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = subjectColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(subjectEmoji, fontSize = 28.sp)

            Column {
                Text(
                    subject.name[language] ?: subject.name["en"] ?: "Subject",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (progress != null && progress.totalLessons > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.completedLessons.toFloat() / progress.totalLessons },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = subjectColor,
                    )
                    Text(
                        "${progress.completedLessons}/${progress.totalLessons} lessons",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
