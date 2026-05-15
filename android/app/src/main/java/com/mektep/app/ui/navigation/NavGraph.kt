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
import com.mektep.app.ui.dashboard.DashboardScreen
import com.mektep.app.ui.lesson.LessonListScreen
import com.mektep.app.ui.lesson.LessonRunnerScreen
import com.mektep.app.ui.screentime.ScreenTimeScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val LESSON_LIST = "lesson_list/{subjectId}"
    const val LESSON_RUNNER = "lesson_runner/{lessonId}"
    const val SCREEN_TIME = "screen_time"

    fun lessonList(subjectId: String) = "lesson_list/$subjectId"
    fun lessonRunner(lessonId: String) = "lesson_runner/$lessonId"
}

@Composable
fun MektepNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)

    val startDest = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
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
            ScreenTimeScreen(onBack = { navController.popBackStack() })
        }
    }
}
