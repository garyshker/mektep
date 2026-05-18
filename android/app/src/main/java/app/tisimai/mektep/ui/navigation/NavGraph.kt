package app.tisimai.mektep.ui.navigation

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.tisimai.mektep.ui.auth.AuthViewModel
import app.tisimai.mektep.ui.auth.LoginScreen
import app.tisimai.mektep.ui.components.PinEntryScreen
import app.tisimai.mektep.ui.dashboard.DashboardScreen
import app.tisimai.mektep.ui.lesson.LessonListScreen
import app.tisimai.mektep.ui.quickgame.QuickGameScreen
import app.tisimai.mektep.ui.lesson.LessonRunnerScreen
import app.tisimai.mektep.ui.pairing.ChildPairingScreen
import app.tisimai.mektep.ui.pairing.ParentPairingScreen
import app.tisimai.mektep.ui.parent.AddChildScreen
import app.tisimai.mektep.ui.parent.AppSelectorScreen
import app.tisimai.mektep.ui.parent.ChildPickerScreen
import app.tisimai.mektep.ui.parent.ParentRemoteDashboardScreen
import app.tisimai.mektep.ui.parent.ParentSettingsScreen
import app.tisimai.mektep.ui.screentime.ScreenTimeScreen
import app.tisimai.mektep.ui.setup.SetupScreen
import app.tisimai.mektep.ui.setup.SetupViewModel
import app.tisimai.mektep.util.tr

object Routes {
    const val LOGIN = "login"
    const val SETUP = "setup"
    const val PIN_SETUP = "pin_setup"
    const val PIN_VERIFY = "pin_verify/{purpose}" // purpose: activate, deactivate, settings
    const val DASHBOARD = "dashboard"
    const val LESSON_LIST = "lesson_list/{subjectId}"
    const val LESSON_RUNNER = "lesson_runner/{lessonId}"
    const val QUICK_GAME = "quick_game"
    const val SCREEN_TIME = "screen_time"
    const val PARENT_SETTINGS = "parent_settings"
    const val APP_SELECTOR = "app_selector"
    const val PAIRING = "pairing"
    const val PARENT_DASHBOARD = "parent_dashboard"
    const val ADD_CHILD = "add_child"
    const val CHILD_PICKER = "child_picker"

    fun lessonList(subjectId: String) = "lesson_list/$subjectId"
    fun lessonRunner(lessonId: String) = "lesson_runner/$lessonId"
    fun pinVerify(purpose: String) = "pin_verify/$purpose"
}

@Composable
fun MektepNavHost(isChildMode: Boolean = false) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    val setupCompleted by authViewModel.setupCompleted.collectAsState(initial = true) // default true to avoid flash

    val startDest = when {
        !isLoggedIn -> Routes.LOGIN
        !setupCompleted -> Routes.SETUP
        else -> Routes.DASHBOARD
    }

    NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // After login, check if setup is needed
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETUP) {
            val setupViewModel: SetupViewModel = hiltViewModel()
            SetupScreen(
                onSkip = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onSameDevice = {
                    navController.navigate(Routes.PIN_SETUP)
                },
                onRemoteParent = {
                    navController.navigate(Routes.PAIRING + "/parent")
                },
                onRemoteChild = {
                    navController.navigate(Routes.PAIRING + "/child")
                },
                viewModel = setupViewModel
            )
        }

        composable(Routes.PIN_SETUP) {
            val setupViewModel: SetupViewModel = hiltViewModel()
            var step by remember { mutableIntStateOf(1) }
            var firstPin by remember { mutableStateOf("") }
            val pinError by setupViewModel.pinError.collectAsState()

            if (step == 1) {
                PinEntryScreen(
                    title = "Create a PIN",
                    subtitle = "This PIN locks Child Mode. Only you should know it.",
                    onPinComplete = { pin ->
                        firstPin = pin
                        step = 2
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                PinEntryScreen(
                    title = "Confirm your PIN",
                    subtitle = "Enter the same PIN again",
                    onPinComplete = { pin ->
                        if (pin == firstPin) {
                            setupViewModel.savePin(pin) {
                                setupViewModel.completeSetup()
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.SETUP) { inclusive = true }
                                }
                            }
                        } else {
                            step = 1
                            firstPin = ""
                        }
                    },
                    onBack = { step = 1; firstPin = "" },
                    error = if (step == 2 && pinError != null) "PINs don't match. Try again." else null
                )
            }
        }

        composable(Routes.DASHBOARD) {
            val context = LocalContext.current
            DashboardScreen(
                isChildMode = isChildMode,
                onSubjectClick = { subjectId -> navController.navigate(Routes.lessonList(subjectId)) },
                onScreenTimeClick = { navController.navigate(Routes.SCREEN_TIME) },
                onQuickGame = { navController.navigate(Routes.QUICK_GAME) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onParentSettings = { navController.navigate(Routes.PARENT_SETTINGS) },
                onStartChildMode = {
                    navController.navigate(Routes.pinVerify("activate"))
                },
                onSetupPin = {
                    navController.navigate(Routes.PIN_SETUP)
                },
                onBackToLauncher = {
                    // Return to ChildLauncherActivity
                    (context as? android.app.Activity)?.finish()
                }
            )
        }

        composable(
            Routes.PIN_VERIFY,
            arguments = listOf(navArgument("purpose") { type = NavType.StringType })
        ) { backStackEntry ->
            val purpose = backStackEntry.arguments?.getString("purpose") ?: "activate"
            val setupViewModel: SetupViewModel = hiltViewModel()
            val pinError by setupViewModel.pinError.collectAsState()

            PinEntryScreen(
                title = when (purpose) {
                    "activate" -> tr("enter_pin_activate", "en")
                    "deactivate" -> tr("enter_pin_deactivate", "en")
                    else -> "Enter your PIN"
                },
                onPinComplete = { pin ->
                    setupViewModel.verifyPin(pin) {
                        when (purpose) {
                            "activate" -> {
                                // Navigate to child picker instead of launching directly
                                navController.navigate(Routes.CHILD_PICKER) {
                                    popUpTo(Routes.DASHBOARD)
                                }
                            }
                            "deactivate" -> {
                                navController.popBackStack()
                            }
                            else -> navController.popBackStack()
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                error = pinError
            )
        }

        composable(
            Routes.LESSON_LIST,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
            LessonListScreen(
                subjectId = subjectId,
                onLessonClick = { lessonId -> navController.navigate(Routes.lessonRunner(lessonId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.LESSON_RUNNER,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: return@composable
            LessonRunnerScreen(
                lessonId = lessonId,
                onFinish = { navController.popBackStack() }
            )
        }

        composable(Routes.QUICK_GAME) {
            QuickGameScreen(onFinish = { navController.popBackStack() })
        }

        composable(Routes.SCREEN_TIME) {
            ScreenTimeScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PARENT_SETTINGS) {
            ParentSettingsScreen(
                onBack = { navController.popBackStack() },
                onSelectApps = { navController.navigate(Routes.APP_SELECTOR) },
                onAddChild = { navController.navigate(Routes.ADD_CHILD) }
            )
        }

        composable(Routes.APP_SELECTOR) {
            AppSelectorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_CHILD) {
            AddChildScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.CHILD_PICKER) {
            val context = LocalContext.current
            ChildPickerScreen(
                onChildSelected = {
                    // Launch the custom ChildLauncher as home screen
                    val intent = Intent(context, app.tisimai.mektep.ChildLauncherActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                },
                onAddChild = { navController.navigate(Routes.ADD_CHILD) }
            )
        }

        composable("pairing/parent") {
            ParentPairingScreen(
                onDone = {
                    navController.navigate(Routes.PARENT_DASHBOARD) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("pairing/child") {
            ChildPairingScreen(
                onDone = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PARENT_DASHBOARD) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ParentRemoteDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
