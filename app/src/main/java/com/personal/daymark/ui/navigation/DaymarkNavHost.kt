package com.personal.daymark.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.personal.daymark.ui.screens.CalendarScreen
import com.personal.daymark.ui.screens.HabitsScreen
import com.personal.daymark.ui.screens.SettingsScreen
import com.personal.daymark.ui.screens.TasksScreen
import com.personal.daymark.ui.screens.TodayScreen

/**
 * Top-level navigation host for Daymark.
 * Uses calm, subtle fade transitions adhering to the serene design language.
 */
@Composable
fun DaymarkNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = DaymarkDestination.Today.route,
    targetTaskId: Long? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(route = DaymarkDestination.Today.route) {
            TodayScreen(
                onNavigateToFocus = { navController.navigate(DaymarkDestination.Focus.route) },
                onNavigateToInsights = { navController.navigate(DaymarkDestination.Insights.route) }
            )
        }
        composable(route = DaymarkDestination.Tasks.route) {
            TasksScreen(targetTaskId = targetTaskId)
        }
        composable(route = DaymarkDestination.Calendar.route) {
            CalendarScreen()
        }
        composable(route = DaymarkDestination.Habits.route) {
            HabitsScreen()
        }
        composable(route = DaymarkDestination.Focus.route) {
            com.personal.daymark.ui.screens.focus.FocusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = DaymarkDestination.Insights.route) {
            com.personal.daymark.ui.screens.insights.InsightsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeeklyReview = { navController.navigate(DaymarkDestination.WeeklyReview.route) },
                onNavigateToFocus = { navController.navigate(DaymarkDestination.Focus.route) }
            )
        }
        composable(route = DaymarkDestination.WeeklyReview.route) {
            com.personal.daymark.ui.screens.insights.WeeklyReviewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = DaymarkDestination.Settings.route) {
            SettingsScreen()
        }
    }
}
