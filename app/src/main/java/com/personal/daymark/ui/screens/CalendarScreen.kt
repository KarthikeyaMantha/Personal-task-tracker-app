package com.personal.daymark.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Calendar screen entry point delegating to full calendar implementation in calendar package.
 */
@Composable
fun CalendarScreen(
    onNavigateToManageTemplates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    com.personal.daymark.ui.screens.calendar.CalendarScreen(
        onNavigateToManageTemplates = onNavigateToManageTemplates,
        modifier = modifier
    )
}
