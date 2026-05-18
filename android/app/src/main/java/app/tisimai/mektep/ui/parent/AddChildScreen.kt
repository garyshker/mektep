package app.tisimai.mektep.ui.parent

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.AgeBand
import app.tisimai.mektep.data.models.ChildProfile
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddChildViewModel @Inject constructor(
    private val childProfileDao: ChildProfileDao,
    private val userDao: UserDao,
    private val tokenStore: TokenStore,
    private val firebaseSync: app.tisimai.mektep.data.remote.FirebaseProfileSync
) : ViewModel() {

    fun saveChild(
        name: String,
        birthDate: String,
        avatarEmoji: String,
        language: String,
        gradeLevel: Int,
        dailyLimitMinutes: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val userId = tokenStore.userId.first() ?: return@launch
            val child = ChildProfile(
                parentUserId = userId,
                name = name,
                birthDate = birthDate,
                avatarEmoji = avatarEmoji,
                language = language,
                gradeLevel = gradeLevel,
                dailyLimitMinutes = dailyLimitMinutes,
                createdAt = System.currentTimeMillis()
            )
            childProfileDao.insert(child)
            firebaseSync.pushChild(child)
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    lang: String = "en",
    viewModel: AddChildViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("\uD83E\uDDD2") } // 🧒
    var selectedLanguage by remember { mutableStateOf("kk") } // default Kazakh for children
    var selectedGrade by remember { mutableIntStateOf(1) }

    val avatarOptions = listOf(
        "\uD83E\uDDD2", // 🧒
        "\uD83D\uDC66", // 👦
        "\uD83D\uDC67", // 👧
        "\uD83D\uDC76", // 👶
        "\uD83E\uDDD2\uD83C\uDFFB", // 🧒🏻
        "\uD83D\uDC66\uD83C\uDFFB", // 👦🏻
        "\uD83D\uDC67\uD83C\uDFFB"  // 👧🏻
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("add_child", lang), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("child_name", lang)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Birth date field
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text(tr("birth_date", lang)) },
                placeholder = { Text("2018-03-15") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Avatar picker
            Text(tr("avatar", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                avatarOptions.forEach { emoji ->
                    val isSelected = emoji == selectedAvatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MektepGreen, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedAvatar = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
            }

            // Language picker
            Text(tr("child_language", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("kk" to "\uD83C\uDDF0\uD83C\uDDFF", "ru" to "\uD83C\uDDF7\uD83C\uDDFA", "en" to "\uD83C\uDDEC\uD83C\uDDE7").forEach { (code, flag) ->
                    FilterChip(
                        selected = selectedLanguage == code,
                        onClick = { selectedLanguage = code },
                        label = { Text(flag, fontSize = 20.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MektepGreen,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Grade level selector
            Text(tr("grade", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..6).forEach { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade },
                        label = { Text("$grade") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MektepGreen,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Band label preview
            val band = AgeBand.fromGradeLevel(selectedGrade)
            Text(
                "${tr(band.labelKey, lang)} \u2022 ${tr("ratio", lang)}: 1:${band.screenTimeRatio}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.saveChild(
                        name = name,
                        birthDate = birthDate,
                        avatarEmoji = selectedAvatar,
                        language = selectedLanguage,
                        gradeLevel = selectedGrade,
                        dailyLimitMinutes = AgeBand.fromGradeLevel(selectedGrade).dailyLimitDefaultMinutes,
                        onSaved = onSaved
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MektepGreen)
            ) {
                Text(tr("save", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
