package app.tisimai.mektep.ui.lesson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.LessonLoader
import app.tisimai.mektep.data.local.ProgressDao
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.data.models.Lesson
import app.tisimai.mektep.data.models.LessonProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonListViewModel @Inject constructor(
    private val lessonLoader: LessonLoader,
    private val progressDao: ProgressDao,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _lessons = MutableStateFlow<List<Pair<Lesson, LessonProgress?>>>(emptyList())
    val lessons: StateFlow<List<Pair<Lesson, LessonProgress?>>> = _lessons.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun load(subjectId: String) {
        viewModelScope.launch {
            val subjectLessons = lessonLoader.lessonsForSubject(subjectId)
            val progress = progressDao.getForSubject(subjectId)
            _lessons.value = subjectLessons.map { lesson ->
                lesson to progress.find { it.lessonId == lesson.id }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    subjectId: String,
    onLessonClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LessonListViewModel = hiltViewModel()
) {
    val lessons by viewModel.lessons.collectAsState()
    val language by viewModel.language.collectAsState()

    LaunchedEffect(subjectId) { viewModel.load(subjectId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lessons") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(lessons) { (lesson, progress) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onLessonClick(lesson.id) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title[language] ?: lesson.title["en"] ?: "Lesson", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(lesson.description[language] ?: lesson.description["en"] ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (progress != null && progress.timesCompleted > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(progress.bestStars) {
                                        Text("⭐", fontSize = 12.sp)
                                    }
                                    Text(" completed", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Icon(
                            if (progress?.timesCompleted ?: 0 > 0) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
