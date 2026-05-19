package app.tisimai.mektep.ui.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.tisimai.mektep.data.models.AgeBand
import app.tisimai.mektep.data.models.QuestionData
import app.tisimai.mektep.ui.components.ConfettiEffect
import app.tisimai.mektep.ui.components.SoundPlayer
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepRed

@Composable
fun LessonRunnerScreen(
    lessonId: String,
    onFinish: () -> Unit,
    viewModel: LessonRunnerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val language by viewModel.language.collectAsState()
    val band by viewModel.bandInfo.collectAsState()

    LaunchedEffect(lessonId) { viewModel.loadLesson(lessonId) }

    // Play sounds on feedback
    LaunchedEffect(state.feedbackShown, state.lastAnswerCorrect) {
        if (state.feedbackShown) {
            if (state.lastAnswerCorrect) SoundPlayer.playCorrect() else SoundPlayer.playWrong()
        }
    }

    // Play complete sound
    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted && state.starsEarned > 0) SoundPlayer.playComplete()
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.isCompleted -> {
                CompletionScreen(state, onFinish, band)
                ConfettiEffect(isActive = state.starsEarned > 0)
            }
            state.currentQuestion != null -> QuestionScreen(state, language, viewModel, band)
        }
    }
}

@Composable
private fun QuestionScreen(state: LessonRunnerState, language: String, viewModel: LessonRunnerViewModel, band: AgeBand) {
    val question = state.currentQuestion ?: return
    val prompt = question.prompt[language] ?: question.prompt["en"] ?: ""

    // Animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = (state.questionIndex + 1).toFloat() / state.totalQuestions,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "progress"
    )

    // Heart shake when lost
    val heartShake = remember { Animatable(0f) }
    LaunchedEffect(state.hearts) {
        if (state.feedbackShown && !state.lastAnswerCorrect) {
            heartShake.animateTo(10f, tween(50))
            heartShake.animateTo(-10f, tween(50))
            heartShake.animateTo(6f, tween(50))
            heartShake.animateTo(-6f, tween(50))
            heartShake.animateTo(0f, tween(50))
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { viewModel.quit() }) { Icon(Icons.Default.Close, "Quit") }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MektepGreen,
            )
            Spacer(Modifier.width(8.dp))
            if (band.heartsEnabled) {
                Row(Modifier.graphicsLayer { translationX = heartShake.value }) {
                    repeat(band.heartsCount) { i ->
                        val isAlive = i < state.hearts
                        val scale by animateFloatAsState(
                            targetValue = if (isAlive) 1f else 0.6f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "heartScale$i"
                        )
                        Icon(
                            if (isAlive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            tint = if (isAlive) MektepRed else MektepRed.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp).scale(scale)
                        )
                    }
                }
            }
        }

        Text(
            "Question ${state.questionIndex + 1} of ${state.totalQuestions}",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Animated prompt entrance
        AnimatedContent(
            targetState = state.questionIndex,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            label = "questionTransition"
        ) { _ ->
            Column {
                Text(prompt, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(Modifier.height(32.dp))

                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (question.type) {
                        "mc", "word" -> McQuestion(state, language, viewModel)
                        "type" -> TypeQuestion(state, viewModel)
                        "tap" -> TapQuestion(state, language, viewModel)
                        "match" -> MatchQuestion(state, viewModel)
                        else -> McQuestion(state, language, viewModel)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Feedback card with slide-up animation
        AnimatedVisibility(
            visible = state.feedbackShown,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val bgColor = if (state.lastAnswerCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            val fgColor = if (state.lastAnswerCorrect) MektepGreen else MektepRed

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Animated checkmark / X
                    val iconScale by animateFloatAsState(
                        targetValue = if (state.feedbackShown) 1f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "feedbackIcon"
                    )
                    Icon(
                        if (state.lastAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null, tint = fgColor, modifier = Modifier.size(28.dp).scale(iconScale)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            when {
                                state.lastAnswerCorrect -> "Correct!"
                                !band.heartsEnabled -> "Try again! You're doing great!"
                                else -> "Incorrect"
                            },
                            fontWeight = FontWeight.Bold, color = fgColor, fontSize = 16.sp
                        )
                        if (state.lastAnswerCorrect) {
                            Text("+5 XP", fontSize = 14.sp, color = fgColor.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Animated button
        val buttonScale by animateFloatAsState(
            targetValue = if (state.selectedAnswer.isNotEmpty() || state.feedbackShown) 1f else 0.95f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "buttonScale"
        )
        Button(
            onClick = { if (state.feedbackShown) viewModel.nextQuestion() else viewModel.submitCurrentAnswer() },
            enabled = state.selectedAnswer.isNotEmpty() || state.feedbackShown,
            modifier = Modifier.fillMaxWidth().height(50.dp).scale(buttonScale),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (state.feedbackShown) "Continue" else "Check", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun McQuestion(state: LessonRunnerState, language: String, viewModel: LessonRunnerViewModel) {
    state.optionTexts.forEachIndexed { index, option ->
        val isSelected = state.selectedAnswer == index.toString()
        val optionText = when (option) {
            is Map<*, *> -> (option[language] ?: option["en"] ?: option.values.firstOrNull()) as? String ?: "Option ${index + 1}"
            else -> option.toString()
        }

        // Staggered entrance animation
        val animatedAlpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(300, delayMillis = index * 80),
            label = "optionAlpha$index"
        )
        val animatedOffset by animateDpAsState(
            targetValue = 0.dp,
            animationSpec = tween(300, delayMillis = index * 80, easing = EaseOutCubic),
            label = "optionOffset$index"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .graphicsLayer { alpha = animatedAlpha; translationY = animatedOffset.value }
                .clickable(enabled = !state.feedbackShown) { viewModel.selectAnswer(index.toString()) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(optionText, Modifier.fillMaxWidth().padding(16.dp), fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TypeQuestion(state: LessonRunnerState, viewModel: LessonRunnerViewModel) {
    OutlinedTextField(
        value = state.selectedAnswer,
        onValueChange = { viewModel.selectAnswer(it) },
        label = { Text("Your answer") },
        singleLine = true,
        enabled = !state.feedbackShown,
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TapQuestion(state: LessonRunnerState, language: String, viewModel: LessonRunnerViewModel) {
    val selected = state.selectedAnswer.split(",").filter { it.isNotEmpty() }.toSet()
    Text("Tap all correct answers:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.optionTexts.forEachIndexed { index, option ->
            val isSelected = index.toString() in selected
            val text = when (option) {
                is Map<*, *> -> (option[language] ?: option["en"] ?: option.values.firstOrNull()) as? String ?: ""
                else -> option.toString()
            }
            val chipScale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "chipScale$index"
            )
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (!state.feedbackShown) {
                        val newSet = selected.toMutableSet()
                        if (isSelected) newSet.remove(index.toString()) else newSet.add(index.toString())
                        viewModel.selectAnswer(newSet.joinToString(","))
                    }
                },
                label = { Text(text, fontSize = 16.sp) },
                modifier = Modifier.scale(chipScale)
            )
        }
    }
}

@Composable
private fun MatchQuestion(state: LessonRunnerState, viewModel: LessonRunnerViewModel) {
    // Interactive tap-to-match: tap a left item, then tap its right match
    var selectedLeft by remember { mutableIntStateOf(-1) }
    var matchedPairs by remember { mutableStateOf(setOf<Int>()) }
    val shuffledRight = remember(state.matchPairs) { state.matchPairs.indices.toList().shuffled() }

    // Auto-complete when all matched
    LaunchedEffect(matchedPairs) {
        if (matchedPairs.size == state.matchPairs.size && state.matchPairs.isNotEmpty()) {
            kotlinx.coroutines.delay(500)
            viewModel.selectAnswer("matched")
        }
    }

    // Left column + Right column side by side
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Left items
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.matchPairs.forEachIndexed { index, (left, _) ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 120L); visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300))
                ) {
                    val isMatched = index in matchedPairs
                    val isSelected = selectedLeft == index && !isMatched
                    val bgColor by animateColorAsState(
                        when {
                            isMatched -> MektepGreen.copy(alpha = 0.3f)
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }, label = "leftBg$index"
                    )
                    val textColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isMatched) {
                            selectedLeft = if (selectedLeft == index) -1 else index
                        },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            left, Modifier.padding(14.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = textColor
                        )
                    }
                }
            }
        }

        // Right items (shuffled)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shuffledRight.forEachIndexed { displayIndex, originalIndex ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(displayIndex * 120L + 60L); visible = true }

                val (_, right) = state.matchPairs[originalIndex]
                val isMatched = originalIndex in matchedPairs

                AnimatedVisibility(
                    visible = visible,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300))
                ) {
                    val bgColor by animateColorAsState(
                        if (isMatched) MektepGreen.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.secondaryContainer,
                        label = "rightBg$originalIndex"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isMatched && selectedLeft >= 0) {
                            if (selectedLeft >= 0 && originalIndex == selectedLeft) {
                                // Correct match!
                                matchedPairs = matchedPairs + originalIndex
                                selectedLeft = -1
                            } else {
                                // Wrong match — shake and deselect
                                selectedLeft = -1
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            right, Modifier.padding(14.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center, fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionScreen(state: LessonRunnerState, onFinish: () -> Unit, band: AgeBand) {
    // Animated entrance
    val titleScale by animateFloatAsState(
        targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "titleScale"
    )

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 64.sp, modifier = Modifier.scale(titleScale))
        Spacer(Modifier.height(16.dp))
        Text("Lesson Complete!", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.scale(titleScale))
        Spacer(Modifier.height(24.dp))

        // Animated stars
        Row {
            repeat(3) { i ->
                val starScale by animateFloatAsState(
                    targetValue = if (i < state.starsEarned) 1.2f else 0.8f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "star$i"
                )
                val delay = i * 200
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(delay.toLong())
                    visible = true
                }
                AnimatedVisibility(visible = visible, enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn()) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = if (i < state.starsEarned) Color(0xFFFFD700) else Color(0xFFE0E0E0),
                        modifier = Modifier.size(52.dp).scale(starScale)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Screen time earned — big highlight
        var timeVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(500); timeVisible = true }
        AnimatedVisibility(
            visible = timeVisible,
            enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MektepGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱️", fontSize = 32.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "+${state.earnedScreenTimeMinutes} min",
                        fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MektepGreen
                    )
                    Text("screen time earned", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.timeSpentMinutes} min learning × ${band.screenTimeRatio}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Stats card with slide-up
        var statsVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(700); statsVisible = true }
        AnimatedVisibility(
            visible = statsVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(400))
        ) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("${state.xpEarned}", "XP", MektepGreen)
                        StatItem("${state.accuracyPct.toInt()}%", "Accuracy", MaterialTheme.colorScheme.onSurface)
                        StatItem("${state.score}/${state.totalQuestions}", "Correct", MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        var btnVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(1000); btnVisible = true }
        AnimatedVisibility(visible = btnVisible, enter = fadeIn(tween(300)) + scaleIn(spring(dampingRatio = 0.6f))) {
            Button(onClick = onFinish, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
