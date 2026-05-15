package com.mektep.app.ui.setup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mektep.app.ui.theme.MektepBlue
import com.mektep.app.ui.theme.MektepGreen
import com.mektep.app.ui.theme.MektepOrange

@Composable
fun SetupScreen(
    onSkip: () -> Unit,            // "Learning on my own" → go to dashboard
    onSameDevice: () -> Unit,      // → PIN setup → parent settings
    onRemoteParent: () -> Unit,    // → create family + pairing
    onRemoteChild: () -> Unit,     // → enter invite code
    viewModel: SetupViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text("How will you use Mektep?", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("You can change this later in settings", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(32.dp))

        // Option 1: Learning on my own
        SetupOptionCard(
            icon = Icons.Default.School,
            title = "I'm learning on my own",
            description = "Just lessons and earning screen time. No parental controls.",
            color = MektepGreen,
            onClick = {
                viewModel.setMode("NONE")
                onSkip()
            },
            delay = 0
        )

        Spacer(Modifier.height(16.dp))

        // Option 2: Same device
        SetupOptionCard(
            icon = Icons.Default.PhoneAndroid,
            title = "Child Mode on this phone",
            description = "I'm a parent. My child uses this phone. Lock it down when they play.",
            color = MektepOrange,
            onClick = {
                viewModel.setMode("SAME_DEVICE")
                onSameDevice()
            },
            delay = 100
        )

        Spacer(Modifier.height(16.dp))

        // Option 3: Remote - parent
        SetupOptionCard(
            icon = Icons.Default.Devices,
            title = "I'm a parent (remote control)",
            description = "My child has their own phone. I want to control their screen time from here.",
            color = MektepBlue,
            onClick = {
                viewModel.setMode("REMOTE_PARENT")
                onRemoteParent()
            },
            delay = 200
        )

        Spacer(Modifier.height(16.dp))

        // Option 4: Remote - child
        SetupOptionCard(
            icon = Icons.Default.ChildCare,
            title = "I'm a child (connect to parent)",
            description = "My parent has the Mektep app. I want to enter their invite code.",
            color = MektepGreen,
            onClick = {
                viewModel.setMode("REMOTE_CHILD")
                onRemoteChild()
            },
            delay = 300
        )
    }
}

@Composable
private fun SetupOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    delay: Int
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
