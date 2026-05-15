package app.tisimai.mektep.ui.quickgame

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.tisimai.mektep.ui.components.ConfettiEffect
import app.tisimai.mektep.ui.components.SoundPlayer
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.ui.theme.*
import app.tisimai.mektep.util.tr

@Composable
fun QuickGameScreen(
    onFinish: () -> Unit,
    viewModel: QuickGameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Start game on first composition
    LaunchedEffect(Unit) { viewModel.startGame() }

    // Sound effects
    LaunchedEffect(state.showingFeedback, state.isCorrect) {
        if (state.showingFeedback) {
            if (state.isCorrect == true) SoundPlayer.playCorrect() else SoundPlayer.playWrong()
        }
    }
    LaunchedEffect(state.isDone) {
        if (state.isDone) SoundPlayer.playComplete()
    }

    // Tick sound at 3 seconds
    LaunchedEffect(state.timeLeft) {
        if (state.timeLeft in 1..3 && !state.showingFeedback) {
            // Light tick - we could add a tick sound but keeping it simple
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.isDone -> ResultsScreen(state, onRetry = { viewModel.startGame() }, onQuit = onFinish)
            state.currentQuestion != null -> GameScreen(state, viewModel, onQuit = onFinish)
        }

        if (state.isDone && state.score >= 10) {
            ConfettiEffect(isActive = true)
        }
    }
}

@Composable
private fun GameScreen(state: QuickGameState, viewModel: QuickGameViewModel, onQuit: () -> Unit) {
    val question = state.currentQuestion ?: return

    // Timer animation
    val timerProgress by animateFloatAsState(
        targetValue = state.timeLeft.toFloat() / QuickGameViewModel.TIME_PER_QUESTION,
        animationSpec = tween(900, easing = LinearEasing),
        label = "timer"
    )
    val timerColor = when {
        state.timeLeft <= 1 -> MektepRed
        state.timeLeft <= 3 -> MektepOrange
        else -> MektepGreen
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar: quit + progress + score
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onQuit) { Icon(Icons.Default.Close, "Quit") }
            Text(
                "${state.questionIndex + 1}/${state.totalQuestions}",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${tr("score", "en")}: ${state.score}",
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MektepGreen
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { (state.questionIndex + 1).toFloat() / state.totalQuestions },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = MektepGreen,
        )

        Spacer(Modifier.height(32.dp))

        // Timer ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                // Background ring
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Timer ring
                drawArc(
                    color = timerColor,
                    startAngle = -90f, sweepAngle = 360f * timerProgress, useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Timer text
            val timerScale by animateFloatAsState(
                targetValue = if (state.timeLeft <= 3 && !state.showingFeedback) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "timerScale"
            )
            Text(
                "${state.timeLeft}",
                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                color = timerColor,
                modifier = Modifier.scale(timerScale)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Question prompt
        AnimatedContent(
            targetState = state.questionIndex,
            transitionSpec = {
                (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 2 } + fadeOut())
            },
            label = "questionAnim"
        ) { _ ->
            Text(
                question.prompt,
                fontSize = 40.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(40.dp))

        // Answer options — 2x2 grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..1) {
                        val idx = row * 2 + col
                        if (idx < question.options.size) {
                            val option = question.options[idx]
                            val isChosen = state.chosenAnswer == idx
                            val isCorrectOption = option == question.correctAnswer
                            val isTimeout = state.chosenAnswer == -1

                            val bgColor = when {
                                !state.showingFeedback -> MaterialTheme.colorScheme.surface
                                isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                                isChosen -> Color(0xFFE53935).copy(alpha = 0.3f)
                                isTimeout && isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val borderColor = when {
                                !state.showingFeedback && isChosen -> MaterialTheme.colorScheme.primary
                                state.showingFeedback && isCorrectOption -> Color(0xFF4CAF50)
                                state.showingFeedback && isChosen -> Color(0xFFE53935)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }

                            val optionScale by animateFloatAsState(
                                targetValue = if (state.showingFeedback && isCorrectOption) 1.05f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "optScale$idx"
                            )

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .scale(optionScale)
                                    .clickable(enabled = !state.showingFeedback) {
                                        viewModel.selectAnswer(idx)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("$option", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsScreen(state: QuickGameState, onRetry: () -> Unit, onQuit: () -> Unit) {
    val lang = TokenStore.lastLanguage // static fallback

    val (emoji, messageKey) = when {
        state.score >= 18 -> "🏆" to "excellent"
        state.score >= 14 -> "🌟" to "great"
        state.score >= 10 -> "👍" to "good"
        else -> "💪" to "keep_practicing"
    }
    val message = tr(messageKey, lang)

    val earnedMinutes = (state.score * 3.0 / 10 * 60 / 60).toInt().coerceAtLeast(1)

    // Animated entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + scaleIn(spring(dampingRatio = 0.6f))
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(message, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(24.dp))

            // Score
            Text(
                "${state.score}/${state.totalQuestions}",
                fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MektepGreen
            )
            Text(tr("correct_answers", lang), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))

            // Earned rewards
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+${state.score * 3}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MektepGreen)
                        Text("XP", fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+${earnedMinutes}m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MektepGreen)
                        Text("Screen Time", fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("play_again", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(tr("back_to_dashboard", lang), fontSize = 16.sp)
            }
        }
    }
}
