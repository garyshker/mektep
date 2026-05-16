package app.tisimai.mektep.ui.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.ChildProfile
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.util.tr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

@HiltViewModel
class ChildPickerViewModel @Inject constructor(
    private val childProfileDao: ChildProfileDao,
    private val userDao: UserDao,
    private val parentalPrefsStore: ParentalPrefsStore
) : ViewModel() {

    private val _children = MutableStateFlow<List<ChildProfile>>(emptyList())
    val children: StateFlow<List<ChildProfile>> = _children.asStateFlow()

    private val _autoSelected = MutableStateFlow(false)
    val autoSelected: StateFlow<Boolean> = _autoSelected.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userDao.getProfileOnce() ?: return@launch
            childProfileDao.getChildrenForParent(user.id).collect { list ->
                _children.value = list
                if (list.size == 1 && !_autoSelected.value) {
                    _autoSelected.value = true
                    parentalPrefsStore.setActiveChildId(list[0].id)
                }
            }
        }
    }

    fun selectChild(child: ChildProfile, onSelected: () -> Unit) {
        viewModelScope.launch {
            parentalPrefsStore.setActiveChildId(child.id)
            onSelected()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildPickerScreen(
    onChildSelected: () -> Unit,
    onAddChild: () -> Unit,
    lang: String = "en",
    viewModel: ChildPickerViewModel = hiltViewModel()
) {
    val children by viewModel.children.collectAsState()
    val autoSelected by viewModel.autoSelected.collectAsState()

    // Auto-select if only one child
    LaunchedEffect(autoSelected, children) {
        if (autoSelected && children.size == 1) {
            onChildSelected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("select_child", lang), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(children) { child ->
                    ChildCard(
                        child = child,
                        lang = lang,
                        onClick = { viewModel.selectChild(child) { onChildSelected() } }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Add child button
            OutlinedButton(
                onClick = onAddChild,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MektepGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("add_new_child", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChildCard(
    child: ChildProfile,
    lang: String,
    onClick: () -> Unit
) {
    val age = calculateAge(child.birthDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(child.avatarEmoji, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                child.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            if (age != null) {
                Text(
                    "$age ${tr("age_years", lang)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${child.xpTotal} XP",
                fontSize = 13.sp,
                color = MektepGreen,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${child.screenTimeBalanceSecs / 60} min",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateAge(birthDate: String): Int? {
    return try {
        val birth = LocalDate.parse(birthDate)
        Period.between(birth, LocalDate.now()).years
    } catch (_: Exception) {
        null
    }
}
