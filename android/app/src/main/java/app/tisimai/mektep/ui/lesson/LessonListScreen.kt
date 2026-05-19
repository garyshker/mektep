package app.tisimai.mektep.ui.lesson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.LessonLoader
import app.tisimai.mektep.data.local.MasteryEngine
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.local.ProgressDao
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.Lesson
import app.tisimai.mektep.data.models.LessonProgress
import app.tisimai.mektep.data.models.LessonStatus
import app.tisimai.mektep.ui.theme.*
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonListViewModel @Inject constructor(
    private val lessonLoader: LessonLoader,
    private val progressDao: ProgressDao,
    private val tokenStore: TokenStore,
    private val parentalPrefsStore: ParentalPrefsStore,
    private val childProfileDao: ChildProfileDao,
    private val userDao: UserDao,
    private val masteryEngine: MasteryEngine
) : ViewModel() {

    private val _lessons = MutableStateFlow<List<Triple<Lesson, LessonProgress?, LessonStatus>>>(emptyList())
    val lessons: StateFlow<List<Triple<Lesson, LessonProgress?, LessonStatus>>> = _lessons.asStateFlow()

    val language: StateFlow<String> = tokenStore.language.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun load(subjectId: String) {
        viewModelScope.launch {
            val childId = parentalPrefsStore.activeChildId.first() ?: ""
            val gradeLevel = if (childId.isNotEmpty()) {
                childProfileDao.getChild(childId)?.gradeLevel ?: 6
            } else {
                userDao.getProfileOnce()?.gradeLevel ?: 6
            }
            val subjectLessons = lessonLoader.lessonsForSubject(subjectId, gradeLevel)
            val progress = progressDao.getForSubject(childId, subjectId)
            val today = java.time.LocalDate.now().toString()
            _lessons.value = subjectLessons.map { lesson ->
                val prog = progress.find { it.lessonId == lesson.id }
                val unlocked = masteryEngine.isLessonUnlocked(subjectLessons, lesson, progress)
                val dueReview = prog?.nextReviewDate != null && prog.nextReviewDate <= today
                Triple(lesson, prog, LessonStatus(unlocked, dueReview))
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
            items(lessons) { (lesson, progress, status) ->
                val isLocked = !status.isUnlocked
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isLocked) 0.5f else 1f)
                        .then(if (isLocked) Modifier else Modifier.clickable { onLessonClick(lesson.id) })
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                lesson.title[language] ?: lesson.title["en"] ?: "Lesson",
                                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                lesson.description[language] ?: lesson.description["en"] ?: "",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${lesson.questions.size} ${tr("questions", language)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (status.isDueForReview) {
                                    Text(
                                        "Review",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MektepOrange
                                    )
                                }
                                if (progress != null && progress.timesCompleted > 0) {
                                    repeat(progress.bestStars) {
                                        Text("⭐", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            when {
                                isLocked -> Icons.Default.Lock
                                (progress?.timesCompleted ?: 0) > 0 -> Icons.Default.CheckCircle
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = if (isLocked) "Locked" else "Start",
                            tint = when {
                                isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                (progress?.timesCompleted ?: 0) > 0 -> MektepGreen
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
