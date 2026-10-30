package com.personal.daymark.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Top-level navigation route wrapper for Habits screen.
 * Delegates to [com.personal.daymark.ui.screens.habits.HabitsScreen].
 */
@Composable
fun HabitsScreen(
    modifier: Modifier = Modifier,
    onNavigateToFocus: (() -> Unit)? = null
) {
    com.personal.daymark.ui.screens.habits.HabitsScreen(
        modifier = modifier,
        onNavigateToFocus = onNavigateToFocus
    )
}
