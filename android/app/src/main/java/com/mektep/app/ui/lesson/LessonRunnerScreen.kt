package com.mektep.app.ui.lesson

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mektep.app.ui.theme.MektepGreen
import com.mektep.app.ui.theme.MektepRed

@Composable
fun LessonRunnerScreen(
    lessonId: String,
    onFinish: () -> Unit,
    viewModel: LessonRunnerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val language by viewModel.language.collectAsState()

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.isCompleted -> CompletionScreen(state, onFinish)
        state.currentQuestion != null -> QuestionScreen(state, language, viewModel)
    }
}

@Composable
private fun QuestionScreen(
    state: LessonRunnerState,
    language: String,
    viewModel: LessonRunnerViewModel
) {
    val question = state.currentQuestion ?: return
    val prompt = question.prompt[language] ?: question.prompt["en"] ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.quit() }) {
                Icon(Icons.Default.Close, "Quit")
            }
            LinearProgressIndicator(
                progress = { (state.questionIndex + 1).toFloat() / state.totalQuestions },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MektepGreen,
            )
            Spacer(Modifier.width(8.dp))
            // Hearts
            Row {
                repeat(state.hearts) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MektepRed, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Question ${state.questionIndex + 1} of ${state.totalQuestions}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Prompt
        Text(
            text = prompt,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Question type renderer
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (question.type) {
                "mc", "word" -> MultipleChoiceQuestion(state, language, viewModel)
                "type" -> TypeAnswerQuestion(state, viewModel)
                "tap" -> TapSelectQuestion(state, language, viewModel)
                "match" -> MatchQuestion(state, language, viewModel)
                else -> MultipleChoiceQuestion(state, language, viewModel)
            }
        }

        // Feedback bar
        AnimatedVisibility(visible = state.feedbackShown) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.lastAnswerCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.lastAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (state.lastAnswerCorrect) MektepGreen else MektepRed
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (state.lastAnswerCorrect) "Correct!" else "Incorrect",
                        fontWeight = FontWeight.Bold,
                        color = if (state.lastAnswerCorrect) MektepGreen else MektepRed
                    )
                }
            }
        }

        // Submit / Next button
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (state.feedbackShown) viewModel.nextQuestion()
                else viewModel.submitCurrentAnswer()
            },
            enabled = state.selectedAnswer.isNotEmpty() || state.feedbackShown,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                if (state.feedbackShown) "Continue" else "Check",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun MultipleChoiceQuestion(
    state: LessonRunnerState,
    language: String,
    viewModel: LessonRunnerViewModel
) {
    val options = state.optionTexts

    options.forEachIndexed { index, option ->
        val isSelected = state.selectedAnswer == index.toString()
        val optionText = if (option is Map<*, *>) {
            (option[language] ?: option["en"] ?: option.values.firstOrNull()) as? String ?: "Option ${index + 1}"
        } else option.toString()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(enabled = !state.feedbackShown) { viewModel.selectAnswer(index.toString()) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }
        ) {
            Text(
                text = optionText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TypeAnswerQuestion(
    state: LessonRunnerState,
    viewModel: LessonRunnerViewModel
) {
    OutlinedTextField(
        value = state.selectedAnswer,
        onValueChange = { viewModel.selectAnswer(it) },
        label = { Text("Your answer") },
        singleLine = true,
        enabled = !state.feedbackShown,
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, textAlign = TextAlign.Center)
    )
}

@Composable
private fun TapSelectQuestion(
    state: LessonRunnerState,
    language: String,
    viewModel: LessonRunnerViewModel
) {
    val options = state.optionTexts
    val selected = state.selectedAnswer.split(",").filter { it.isNotEmpty() }.toSet()

    Text("Tap all correct answers:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index.toString() in selected
            val optionText = if (option is Map<*, *>) {
                (option[language] ?: option["en"] ?: option.values.firstOrNull()) as? String ?: ""
            } else option.toString()

            FilterChip(
                selected = isSelected,
                onClick = {
                    if (!state.feedbackShown) {
                        val newSet = selected.toMutableSet()
                        if (isSelected) newSet.remove(index.toString()) else newSet.add(index.toString())
                        viewModel.selectAnswer(newSet.joinToString(","))
                    }
                },
                label = { Text(optionText, fontSize = 16.sp) }
            )
        }
    }
}

@Composable
private fun MatchQuestion(
    state: LessonRunnerState,
    language: String,
    viewModel: LessonRunnerViewModel
) {
    Text("Match the pairs by tapping left then right:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))

    // Simplified match UI - show pairs as a list the user can review
    state.matchPairs.forEachIndexed { index, pair ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(pair.first, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
            }
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically))
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(pair.second, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
            }
        }
    }

    // Auto-mark as answered for match questions
    LaunchedEffect(Unit) {
        viewModel.selectAnswer("matched")
    }
}

@Composable
private fun CompletionScreen(
    state: LessonRunnerState,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Lesson Complete!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // Stars
        Row {
            repeat(3) { i ->
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (i < state.starsEarned) Color(0xFFFFD700) else Color(0xFFE0E0E0),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.xpEarned}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MektepGreen)
                        Text("XP Earned", fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.accuracyPct.toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Accuracy", fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.score}/${state.totalQuestions}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Correct", fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Continue", fontSize = 16.sp)
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Use built-in FlowRow from Compose Foundation
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
