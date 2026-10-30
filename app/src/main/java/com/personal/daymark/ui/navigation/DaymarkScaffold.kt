package com.personal.daymark.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.daymark.ui.theme.DaymarkTheme
import kotlinx.coroutines.launch

/**
 * Root Daymark Scaffold with Bottom Navigation Bar, Sidebar Drawer, and state-preserved tab navigation.
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isFocusMode,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Brand Header with Sunset Coral Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daymark",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mindful momentum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Sidebar Navigation Links
                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    selected = currentRoute == DaymarkDestination.Settings.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(DaymarkDestination.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Insights & Analytics", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null) },
                    selected = currentRoute == DaymarkDestination.Insights.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(DaymarkDestination.Insights.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Weekly Review", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                    selected = currentRoute == DaymarkDestination.WeeklyReview.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(DaymarkDestination.WeeklyReview.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Day Templates", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                    selected = currentRoute == DaymarkDestination.ManageTemplates.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(DaymarkDestination.ManageTemplates.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Sidebar Footer: Version and Privacy Guarantee
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Daymark v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "100% Offline • Zero Tracking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                if (!isFocusMode) {
                    DaymarkBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigateToDestination = { destination ->
                            if (destination == DaymarkDestination.Today) {
                                navController.navigate(DaymarkDestination.Today.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            } else {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                targetTaskId = targetTaskId,
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
            )
        }
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
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
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
