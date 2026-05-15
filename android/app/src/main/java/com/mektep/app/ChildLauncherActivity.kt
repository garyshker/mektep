package com.mektep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mektep.app.ui.childmode.ChildLauncherScreen
import com.mektep.app.ui.theme.MektepTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Custom Launcher Activity — becomes the device home screen during child mode.
 *
 * Declared with CATEGORY_HOME in AndroidManifest.xml so the system offers it
 * as a launcher choice. When child mode is active, the parent selects this as
 * the default home app, locking the child into the Mektep environment.
 */
@AndroidEntryPoint
class ChildLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MektepTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ChildLauncherScreen(
                        onOpenMektep = {
                            // Launch the main Mektep activity for learning
                            val intent = packageManager.getLaunchIntentForPackage("com.mektep.app")
                            if (intent != null) startActivity(intent)
                        },
                        onExitChildMode = {
                            // Close this launcher — system reverts to previous default
                            finishAffinity()
                        }
                    )
                }
            }
        }
    }

    // Prevent back button from exiting the launcher
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — child cannot back out of the launcher
    }
}
