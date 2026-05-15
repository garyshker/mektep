package app.tisimai.mektep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinEntryScreen(
    title: String,
    subtitle: String = "",
    onPinComplete: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    error: String? = null
) {
    var pin by remember { mutableStateOf("") }
    var shakeError by remember { mutableStateOf(false) }

    // Shake animation on error
    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeError) 1f else 0f,
        animationSpec = if (shakeError) {
            keyframes {
                durationMillis = 400
                0f at 0
                -15f at 50
                15f at 100
                -10f at 150
                10f at 200
                -5f at 250
                5f at 300
                0f at 400
            }
        } else tween(0),
        label = "shake",
        finishedListener = { shakeError = false }
    )

    LaunchedEffect(error) {
        if (error != null) {
            shakeError = true
            pin = ""
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(32.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset(x = shakeOffset.dp)
            ) {
                repeat(4) { i ->
                    val filled = i < pin.length
                    val dotScale by animateFloatAsState(
                        targetValue = if (filled) 1f else 0.8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(
                                if (filled) MektepGreen
                                else Color.Transparent
                            )
                            .border(2.dp, if (error != null) MektepRed else MektepGreen, CircleShape)
                    )
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MektepRed, fontSize = 14.sp)
            }

            Spacer(Modifier.weight(1f))

            // Numeric keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (key == "⌫") {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        } else if (pin.length < 4) {
                                            pin += key
                                            if (pin.length == 4) {
                                                onPinComplete(pin)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "⌫") {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, "Delete", modifier = Modifier.size(24.dp))
                                } else {
                                    Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
