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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _existingChild = MutableStateFlow<ChildProfile?>(null)
    val existingChild: StateFlow<ChildProfile?> = _existingChild.asStateFlow()

    fun loadChild(childId: String) {
        viewModelScope.launch {
            _existingChild.value = childProfileDao.getChild(childId)
        }
    }

    fun saveChild(
        childId: String?,
        name: String,
        birthDate: String,
        avatarEmoji: String,
        language: String,
        gradeLevel: Int,
        dailyLimitMinutes: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = if (childId != null) childProfileDao.getChild(childId) else null
            if (existing != null) {
                // Update existing child
                val updated = existing.copy(
                    name = name,
                    birthDate = birthDate,
                    avatarEmoji = avatarEmoji,
                    language = language,
                    gradeLevel = gradeLevel,
                    dailyLimitMinutes = dailyLimitMinutes
                )
                childProfileDao.update(updated)
                firebaseSync.pushChild(updated)
            } else {
                // Create new child
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
            }
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    childId: String? = null,
    lang: String = "en",
    viewModel: AddChildViewModel = hiltViewModel()
) {
    val existingChild by viewModel.existingChild.collectAsState()
    val isEditMode = childId != null

    LaunchedEffect(childId) {
        if (childId != null) viewModel.loadChild(childId)
    }

    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("\uD83E\uDDD2") } // 🧒
    var selectedLanguage by remember { mutableStateOf("kk") }
    var selectedGrade by remember { mutableIntStateOf(0) }
    var gradeManuallySet by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Auto-calculate grade from birth date (unless parent manually set it)
    fun calculateGradeFromAge(birthDateStr: String): Int {
        return try {
            val birth = java.time.LocalDate.parse(birthDateStr)
            val age = java.time.Period.between(birth, java.time.LocalDate.now()).years
            (age - 5).coerceIn(0, 3) // age 4-5→0(Pre-K), 6→1, 7→2, 8+→3
        } catch (_: Exception) { 0 }
    }

    // Pre-fill when editing
    LaunchedEffect(existingChild) {
        existingChild?.let { child ->
            if (!prefilled) {
                name = child.name
                birthDate = child.birthDate
                selectedAvatar = child.avatarEmoji
                selectedLanguage = child.language
                selectedGrade = child.gradeLevel
                gradeManuallySet = true
                prefilled = true
            }
        }
    }

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
                title = { Text(tr(if (isEditMode) "edit_child" else "add_child", lang), fontWeight = FontWeight.Bold) },
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

            // Birth date picker
            OutlinedTextField(
                value = if (birthDate.isNotEmpty()) {
                    try {
                        val d = java.time.LocalDate.parse(birthDate)
                        val age = java.time.Period.between(d, java.time.LocalDate.now()).years
                        "$birthDate ($age ${tr("age_years", lang)})"
                    } catch (_: Exception) { birthDate }
                } else "",
                onValueChange = {},
                label = { Text(tr("birth_date", lang)) },
                placeholder = { Text(tr("tap_to_select", lang)) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                singleLine = true,
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = try {
                        java.time.LocalDate.parse(birthDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        // Default: 5 years ago
                        java.time.LocalDate.now().minusYears(5).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                                birthDate = date.toString()
                                // Auto-calculate grade
                                if (!gradeManuallySet) {
                                    selectedGrade = calculateGradeFromAge(birthDate)
                                }
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(tr("cancel", lang)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

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
                (0..3).forEach { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade; gradeManuallySet = true },
                        label = { Text(if (grade == 0) "Pre-K" else "$grade") },
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
                        childId = childId,
                        name = name,
                        birthDate = birthDate,
                        avatarEmoji = selectedAvatar,
                        language = selectedLanguage,
                        gradeLevel = selectedGrade,
                        dailyLimitMinutes = existingChild?.dailyLimitMinutes ?: AgeBand.fromGradeLevel(selectedGrade).dailyLimitDefaultMinutes,
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
