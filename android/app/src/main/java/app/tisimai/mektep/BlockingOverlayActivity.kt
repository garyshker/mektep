package app.tisimai.mektep

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.local.ParentalConfigDao
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.services.ScreenTimePrefs
import app.tisimai.mektep.services.SystemEssentials
import app.tisimai.mektep.ui.theme.MektepGreen
import app.tisimai.mektep.ui.theme.MektepTheme
import app.tisimai.mektep.util.PinHasher
import app.tisimai.mektep.util.tr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Full-screen blocking overlay shown when screen time runs out.
 *
 * Uses [FragmentActivity] (required by [BiometricPrompt]) and Hilt injection for
 * Room DAOs. Anti-escape measures prevent the child from dismissing the activity.
 */
@AndroidEntryPoint
class BlockingOverlayActivity : FragmentActivity() {

    @Inject lateinit var parentalConfigDao: ParentalConfigDao
    @Inject lateinit var childProfileDao: ChildProfileDao

    private lateinit var prefs: ScreenTimePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = ScreenTimePrefs(this)

        // Show over lock screen, keep screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val lang = TokenStore.lastLanguage

        // Load child avatar from DB (synchronously, fast local query)
        val childEmoji = prefs.activeChildId?.let { childId ->
            runBlocking { childProfileDao.getChild(childId)?.avatarEmoji }
        } ?: "\u23F0" // ⏰ fallback

        // Check biometric availability
        val biometricAvailable = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

        setContent {
            MektepTheme(darkTheme = true) {
                BlockingOverlayScreen(
                    lang = lang,
                    childEmoji = childEmoji,
                    biometricAvailable = biometricAvailable,
                    onLearnMore = { launchLearnMore() },
                    onPinSubmit = { pin -> verifyPin(pin) },
                    onBiometric = { onSuccess -> launchBiometric(onSuccess) },
                    onBonusSelected = { bonusSeconds -> grantBonusAndDismiss(bonusSeconds) }
                )
            }
        }
    }

    // ── PIN verification ──

    private fun verifyPin(pin: String): Boolean {
        val config = runBlocking { parentalConfigDao.getConfigOnce() } ?: return false
        if (config.pinHash.isEmpty()) return false
        return PinHasher.verify(pin, config.pinSalt, config.pinHash)
    }

    // ── Biometric prompt ──

    private fun launchBiometric(onSuccess: () -> Unit) {
        val lang = TokenStore.lastLanguage
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            // onAuthenticationError / onAuthenticationFailed — do nothing, user can retry
        }
        val prompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(tr("biometric_title", lang))
            .setSubtitle(tr("biometric_subtitle", lang))
            .setNegativeButtonText(tr("use_pin", lang))
            .build()
        prompt.authenticate(promptInfo)
    }

    // ── Learn More — launch MainActivity in child mode ──

    private fun launchLearnMore() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("CHILD_MODE", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    // ── Bonus time and dismiss ──

    private fun grantBonusAndDismiss(bonusSeconds: Int) {
        if (bonusSeconds > 0) {
            prefs.balanceSeconds = prefs.balanceSeconds + bonusSeconds
            // Persist to Room
            val childId = prefs.activeChildId
            if (childId != null) {
                runBlocking { childProfileDao.updateScreenTimeBalance(childId, bonusSeconds) }
            }
        }
        prefs.isOverlayShowing = false
        finish()
    }

    // ── Anti-escape ──

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — child cannot dismiss overlay via back button
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (prefs.isOverlayShowing) {
            val foreground = prefs.currentForegroundPackage
            if (!SystemEssentials.isSystemEssential(foreground)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (prefs.isOverlayShowing) {
                        val relaunch = Intent(this, BlockingOverlayActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(relaunch)
                    }
                }, 200)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Compose UI
// ══════════════════════════════════════════════════════════════

@Composable
private fun BlockingOverlayScreen(
    lang: String,
    childEmoji: String,
    biometricAvailable: Boolean,
    onLearnMore: () -> Unit,
    onPinSubmit: (String) -> Boolean,
    onBiometric: (onSuccess: () -> Unit) -> Unit,
    onBonusSelected: (Int) -> Unit
) {
    var showPinEntry by remember { mutableStateOf(false) }
    var showBonusDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Child avatar or clock emoji
            Text(
                text = childEmoji,
                fontSize = 72.sp
            )

            Spacer(Modifier.height(24.dp))

            // "Time's Up!" title
            Text(
                text = tr("times_up", lang),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle
            Text(
                text = tr("times_up_desc", lang),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // "Learn More" button
            Button(
                onClick = onLearnMore,
                colors = ButtonDefaults.buttonColors(containerColor = MektepGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = tr("overlay_learn_more", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(48.dp))

            // PIN entry section
            if (!showPinEntry) {
                TextButton(onClick = { showPinEntry = true }) {
                    Text(
                        text = tr("parent_takeover", lang),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            } else {
                // Inline PIN entry
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                ) {
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { newValue ->
                            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                pinValue = newValue
                                pinError = false
                            }
                        },
                        label = { Text("PIN") },
                        isError = pinError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MektepGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = MektepGreen,
                            focusedLabelColor = MektepGreen,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            errorBorderColor = Color.Red
                        ),
                        modifier = Modifier.width(200.dp)
                    )

                    if (pinError) {
                        Text(
                            text = tr("wrong_pin", lang),
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (onPinSubmit(pinValue)) {
                                showBonusDialog = true
                            } else {
                                pinError = true
                                scope.launch {
                                    // Shake animation
                                    shakeOffset.animateTo(20f, spring(dampingRatio = 0.3f, stiffness = 800f))
                                    shakeOffset.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MektepGreen),
                        shape = RoundedCornerShape(8.dp),
                        enabled = pinValue.length >= 4
                    ) {
                        Text(tr("check", lang))
                    }
                }
            }

            // Biometric button
            if (biometricAvailable) {
                Spacer(Modifier.height(16.dp))
                IconButton(
                    onClick = { onBiometric { showBonusDialog = true } },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = tr("biometric_title", lang),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // Bonus time dialog
    if (showBonusDialog) {
        BonusTimeDialog(
            lang = lang,
            onSelect = { bonusSeconds ->
                showBonusDialog = false
                onBonusSelected(bonusSeconds)
            }
        )
    }
}

@Composable
private fun BonusTimeDialog(
    lang: String,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onSelect(0) },
        title = {
            Text(
                text = tr("bonus_title", lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                BonusOption(label = tr("bonus_15", lang)) { onSelect(15 * 60) }
                BonusOption(label = tr("bonus_30", lang)) { onSelect(30 * 60) }
                BonusOption(label = tr("bonus_60", lang)) { onSelect(60 * 60) }
                Spacer(Modifier.height(8.dp))
                BonusOption(label = tr("bonus_none", lang)) { onSelect(0) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onSelect(0) }) {
                Text(tr("cancel", lang))
            }
        }
    )
}

@Composable
private fun BonusOption(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
    }
}
