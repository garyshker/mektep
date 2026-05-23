package app.tisimai.mektep.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.tisimai.mektep.ui.components.BrandMark
import app.tisimai.mektep.ui.components.MektepBackground
import app.tisimai.mektep.ui.components.MektepButton
import app.tisimai.mektep.ui.theme.Spacing
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedLanguage by remember { mutableStateOf("en") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result -> viewModel.handleGoogleSignInResult(result) }

    androidx.compose.runtime.LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    val bounce = rememberInfiniteTransition(label = "bounce")
    val markScale by bounce.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "markBounce",
    )

    MektepBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 460.dp)
                .padding(horizontal = Spacing.xxl)
                .padding(bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(
                icon = Icons.Default.School,
                size = 92.dp,
                modifier = Modifier.scale(markScale),
            )

            Spacer(Modifier.height(Spacing.xl))

            Text(
                "Mektep",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                when (selectedLanguage) {
                    "kk" -> "Үйреніп, экран уақытын жина"
                    "ru" -> "Учись и зарабатывай экранное время"
                    else -> "Learn & earn screen time"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xxl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                listOf(
                    Triple("kk", "🇰🇿", "Қазақша"),
                    Triple("ru", "🇷🇺", "Русский"),
                    Triple("en", "🇬🇧", "English"),
                ).forEach { (code, flag, label) ->
                    LanguagePill(
                        flag = flag,
                        label = label,
                        selected = selectedLanguage == code,
                        onClick = { selectedLanguage = code },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xxl))

            MektepButton(
                text = "",
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(app.tisimai.mektep.R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context as Activity, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(
                        when (selectedLanguage) {
                            "kk" -> "Google арқылы кіру"
                            "ru" -> "Войти через Google"
                            else -> "Sign in with Google"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            OutlinedButton(
                onClick = { viewModel.skipSignIn(selectedLanguage) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    when (selectedLanguage) {
                        "kk" -> "Тіркеусіз жалғастыру"
                        "ru" -> "Продолжить без аккаунта"
                        else -> "Continue without account"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            uiState.error?.let { err ->
                Spacer(Modifier.height(Spacing.lg))
                Text(err, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(Spacing.xxl))

            Text(
                when (selectedLanguage) {
                    "kk" -> "Сабақтарды орындап, экран уақытын жина.\nКөбірек үйренсең, көбірек ойнайсың!"
                    "ru" -> "Выполняй уроки и зарабатывай экранное время.\nБольше учишься — больше играешь!"
                    else -> "Complete lessons to earn screen time.\nThe more you learn, the more you play!"
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LanguagePill(
    flag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.selectable(selected = selected, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(flag, fontSize = 24.sp)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
