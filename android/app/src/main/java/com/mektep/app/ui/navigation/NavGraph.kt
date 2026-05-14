package com.mektep.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mektep.app.ui.auth.AuthViewModel
import com.mektep.app.ui.auth.LoginScreen
import com.mektep.app.ui.auth.RegisterScreen
import com.mektep.app.ui.dashboard.DashboardScreen
import com.mektep.app.ui.lesson.LessonListScreen
import com.mektep.app.ui.lesson.LessonRunnerScreen
import com.mektep.app.ui.parent.ParentDashboardScreen
import com.mektep.app.ui.screentime.ScreenTimeScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val LESSON_LIST = "lesson_list/{subjectId}"
    const val LESSON_RUNNER = "lesson_runner/{lessonId}"
    const val SCREEN_TIME = "screen_time"
    const val PARENT_DASHBOARD = "parent_dashboard"

    fun lessonList(subjectId: String) = "lesson_list/$subjectId"
    fun lessonRunner(lessonId: String) = "lesson_runner/$lessonId"
}

@Composable
fun MektepNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)
    val userRole by authViewModel.userRole.collectAsState(initial = null)

    val startDest = when {
        isLoggedIn == null -> Routes.LOGIN // still loading
        isLoggedIn == true && userRole == "PARENT" -> Routes.PARENT_DASHBOARD
        isLoggedIn == true -> Routes.DASHBOARD
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "PARENT") navController.navigate(Routes.PARENT_DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    else navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    if (role == "PARENT") navController.navigate(Routes.PARENT_DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    else navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onSubjectClick = { subjectId -> navController.navigate(Routes.lessonList(subjectId)) },
                onScreenTimeClick = { navController.navigate(Routes.SCREEN_TIME) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
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

        composable(Routes.SCREEN_TIME) {
            ScreenTimeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PARENT_DASHBOARD) {
            ParentDashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
