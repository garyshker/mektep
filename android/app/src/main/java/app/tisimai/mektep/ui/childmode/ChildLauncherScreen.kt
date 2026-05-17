package app.tisimai.mektep.ui.childmode

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.tisimai.mektep.ui.components.PinEntryScreen
import app.tisimai.mektep.ui.setup.SetupViewModel
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepOrange
import app.tisimai.mektep.ui.theme.MektepRed
import app.tisimai.mektep.util.tr

data class LauncherApp(
    val packageName: String,
    val label: String,
    val needsEarnedTime: Boolean
)

@Composable
fun ChildLauncherScreen(
    onOpenMektep: () -> Unit,
    onExitChildMode: () -> Unit,
    viewModel: ChildLauncherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang by viewModel.language.collectAsState()
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadApps(context.packageManager) }

    if (showPinDialog) {
        val setupVm: SetupViewModel = hiltViewModel()
        val pinError by setupVm.pinError.collectAsState()
        PinEntryScreen(
            title = tr("enter_pin_deactivate", lang),
            onPinComplete = { pin ->
                setupVm.verifyPin(pin) {
                    viewModel.deactivateChildMode()
                    onExitChildMode()
                }
            },
            onBack = { showPinDialog = false },
            error = pinError
        )
        return
    }

    // Time's up overlay
    if (state.balanceSeconds <= 0 && !state.isLoading) {
        TimesUpScreen(lang = lang, onLearnMore = onOpenMektep)
        return
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top bar: timer + lock button
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer
            val hours = state.balanceSeconds / 3600
            val mins = (state.balanceSeconds % 3600) / 60
            val secs = state.balanceSeconds % 60

            val timerColor = when {
                state.balanceSeconds <= 60 -> MektepRed
                state.balanceSeconds <= 300 -> MektepOrange
                else -> MektepGreen
            }

            // Pulsing animation when low
            val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f,
                targetValue = if (state.balanceSeconds <= 60) 1.1f else 1f,
                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                label = "timerPulse"
            )

            Icon(Icons.Default.Timer, null, tint = timerColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hours > 0) "%d:%02d:%02d".format(hours, mins, secs) else "%d:%02d".format(mins, secs),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = timerColor,
                modifier = Modifier.weight(1f)
            )

            // Lock icon to exit child mode
            IconButton(onClick = { showPinDialog = true }) {
                Icon(Icons.Default.Lock, tr("enter_pin_deactivate", lang), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))

        // "Learn More" card
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenMektep() },
            colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = MektepGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("earn_more_time", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(tr("earn_more_desc", lang), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MektepGreen)
            }
        }

        Spacer(Modifier.height(16.dp))

        // App grid
        Text(tr("your_apps", lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.apps) { app ->
                val canOpen = !app.needsEarnedTime || state.balanceSeconds > 0

                Column(
                    modifier = Modifier
                        .clickable(enabled = canOpen) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App icon placeholder (circle with first letter)
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (canOpen) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            app.label.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canOpen) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        if (!canOpen) {
                            Icon(
                                Icons.Default.Lock, null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp).align(Alignment.BottomEnd)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        app.label,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (canOpen) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    // Break reminder overlay (non-blocking)
    if (state.showBreakReminder) {
        BreakReminderOverlay(lang = lang, onDismiss = { viewModel.dismissBreakReminder() })
    }
    } // end Box
}

@Composable
private fun BreakReminderOverlay(lang: String, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\uD83C\uDF3F", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(tr("break_title", lang), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(tr("break_desc", lang), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(tr("break_dismiss", lang), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun TimesUpScreen(lang: String, onLearnMore: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏰", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(tr("times_up", lang), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            tr("times_up_desc", lang),
            fontSize = 16.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onLearnMore,
            Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.School, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("earn_more_time", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
