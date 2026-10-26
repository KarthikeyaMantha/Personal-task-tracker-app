package com.personal.daymark.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations in the Daymark navigation hierarchy.
 */
sealed class DaymarkDestination(
    val route: String,
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Today : DaymarkDestination(
        route = "today",
        title = "Today",
        unselectedIcon = Icons.Outlined.Today,
        selectedIcon = Icons.Filled.Today
    )

    data object Tasks : DaymarkDestination(
        route = "tasks",
        title = "Tasks",
        unselectedIcon = Icons.Outlined.CheckCircle,
        selectedIcon = Icons.Filled.CheckCircle
    )

    data object Calendar : DaymarkDestination(
        route = "calendar",
        title = "Calendar",
        unselectedIcon = Icons.Outlined.DateRange,
        selectedIcon = Icons.Filled.DateRange
    )

    data object Habits : DaymarkDestination(
        route = "habits",
        title = "Habits",
        unselectedIcon = Icons.Outlined.Repeat,
        selectedIcon = Icons.Filled.Repeat
    )

    data object Focus : DaymarkDestination(
        route = "focus",
        title = "Focus",
        unselectedIcon = Icons.Outlined.Timer,
        selectedIcon = Icons.Filled.Timer
    )

    data object Insights : DaymarkDestination(
        route = "insights",
        title = "Insights",
        unselectedIcon = Icons.AutoMirrored.Outlined.TrendingUp,
        selectedIcon = Icons.AutoMirrored.Filled.TrendingUp
    )

    data object WeeklyReview : DaymarkDestination(
        route = "weekly_review",
        title = "Weekly Review",
        unselectedIcon = androidx.compose.material.icons.Icons.Outlined.DateRange,
        selectedIcon = androidx.compose.material.icons.Icons.Filled.DateRange
    )

    data object Settings : DaymarkDestination(
        route = "settings",
        title = "Settings",
        unselectedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )

    data object FocusMode : DaymarkDestination(
        route = "focus_mode",
        title = "Focus Mode",
        unselectedIcon = Icons.Outlined.Timer,
        selectedIcon = Icons.Filled.Timer
    )

    companion object {
        val topLevelDestinations: List<DaymarkDestination> = listOf(
            Today,
            Tasks,
            Calendar,
            Habits,
            Settings
        )
    }
}
