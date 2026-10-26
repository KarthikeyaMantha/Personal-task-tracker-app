package com.personal.daymark.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.daymark.ui.theme.DaymarkTheme

/**
 * Root Daymark Scaffold with Bottom Navigation Bar and state-preserved tab navigation.
 */
@Composable
fun DaymarkScaffold(
    navController: NavHostController = rememberNavController(),
    targetTaskId: Long? = null,
    targetRoute: String? = null,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    androidx.compose.runtime.LaunchedEffect(targetTaskId) {
        if (targetTaskId != null && targetTaskId > 0) {
            navController.navigate(DaymarkDestination.Tasks.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(targetRoute) {
        if (!targetRoute.isNullOrBlank()) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val isFocusMode = currentRoute == DaymarkDestination.FocusMode.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isFocusMode) {
                DaymarkBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        DaymarkNavHost(
            navController = navController,
            modifier = Modifier
                .padding(if (isFocusMode) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                .consumeWindowInsets(innerPadding),
            targetTaskId = targetTaskId
        )
    }
}

@Composable
fun DaymarkBottomNavigation(
    currentRoute: String?,
    onNavigateToDestination: (DaymarkDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        DaymarkDestination.topLevelDestinations.forEach { destination ->
            val isSelected = currentRoute == destination.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DaymarkScaffoldPreview() {
    DaymarkTheme {
        DaymarkScaffold()
    }
}
