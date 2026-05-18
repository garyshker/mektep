package app.tisimai.mektep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.tisimai.mektep.ui.childmode.ChildLauncherScreen
import app.tisimai.mektep.ui.theme.MektepTheme
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
                            // Launch MainActivity in child mode (no parent controls)
                            val intent = Intent(this@ChildLauncherActivity, MainActivity::class.java)
                            intent.putExtra("CHILD_MODE", true)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            android.util.Log.d("BilimALL", "Launching MainActivity in child mode")
                            startActivity(intent)
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
