package app.tisimai.mektep.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedLanguage by remember { mutableStateOf("en") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.handleGoogleSignInResult(result) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    // Icon bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "iconBounce"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.School, contentDescription = null,
            modifier = Modifier.size(80.dp).scale(iconScale),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text("Mektep", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Text(
            when (selectedLanguage) {
                "kk" -> "Үйреніп, экран уақытын жина"
                "ru" -> "Учись и зарабатывай экранное время"
                else -> "Learn & Earn Screen Time"
            },
            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Language selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("kk" to "Қазақша", "ru" to "Русский", "en" to "English").forEach { (code, label) ->
                FilterChip(
                    selected = selectedLanguage == code,
                    onClick = { selectedLanguage = code },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Google Sign In
        Button(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(app.tisimai.mektep.R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context as Activity, gso)
                googleSignInLauncher.launch(client.signInIntent)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(
                    when (selectedLanguage) {
                        "kk" -> "Google арқылы кіру"
                        "ru" -> "Войти через Google"
                        else -> "Sign in with Google"
                    }, fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.skipSignIn(selectedLanguage) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(
                when (selectedLanguage) {
                    "kk" -> "Тіркеусіз жалғастыру"
                    "ru" -> "Продолжить без аккаунта"
                    else -> "Continue without account"
                }, fontSize = 16.sp
            )
        }

        if (uiState.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            when (selectedLanguage) {
                "kk" -> "Сабақтарды орындап, экран уақытын жина.\nКөбірек үйренсең, көбірек ойнайсың!"
                "ru" -> "Выполняй уроки и зарабатывай экранное время.\nБольше учишься — больше играешь!"
                else -> "Complete lessons to earn screen time.\nThe more you learn, the more you play!"
            },
            textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
