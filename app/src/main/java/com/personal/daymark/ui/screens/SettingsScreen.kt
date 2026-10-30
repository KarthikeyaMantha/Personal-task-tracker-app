package com.personal.daymark.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.notifications.SystemReliabilityHelper
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.ui.components.DaymarkCard
import com.personal.daymark.ui.components.DaymarkTopAppBar
import com.personal.daymark.ui.components.getPriorityColor
import com.personal.daymark.ui.screens.backup.BackupViewModel
import com.personal.daymark.ui.screens.categories.ManageCategoriesSheet
import com.personal.daymark.ui.screens.categories.ManageCategoriesViewModel
import com.personal.daymark.ui.screens.settings.SettingsViewModel
import com.personal.daymark.ui.theme.DaymarkTheme
import java.time.LocalDate

@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToManageTemplates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).container

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(appContainer.settingsRepository)
    )
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

    val backupViewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.provideFactory(appContainer.backupManager)
    )
    val backupUiState by backupViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isManageCategoriesOpen by remember { mutableStateOf(false) }

    LaunchedEffect(backupUiState.snackbarMessage) {
        backupUiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            backupViewModel.clearSnackbar()
        }
    }

    // Storage Access Framework (SAF) Launchers
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.onBackupCreated(context, uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.onRestoreFileSelected(context, uri)
        }
    }

    val exportTasksCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.onExportTasksCsv(context, uri)
        }
    }

    val exportHabitsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.onExportHabitsCsv(context, uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            DaymarkTopAppBar(
                title = "Settings",
                navigationIcon = if (onNavigateBack != null) {
                    {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else null,
                centerTitle = false
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 1. Appearance & Senses
                SectionHeader(title = "Appearance & Senses")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Preference",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Changes take effect immediately across all screens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOptionItem(
                                title = "System",
                                icon = Icons.Outlined.SettingsBrightness,
                                isSelected = userPrefs.themePreference == UserPreferences.THEME_SYSTEM,
                                onClick = { settingsViewModel.setThemePreference(UserPreferences.THEME_SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionItem(
                                title = "Light",
                                icon = Icons.Outlined.LightMode,
                                isSelected = userPrefs.themePreference == UserPreferences.THEME_LIGHT,
                                onClick = { settingsViewModel.setThemePreference(UserPreferences.THEME_LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionItem(
                                title = "Dark",
                                icon = Icons.Outlined.DarkMode,
                                isSelected = userPrefs.themePreference == UserPreferences.THEME_DARK,
                                onClick = { settingsViewModel.setThemePreference(UserPreferences.THEME_DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(16.dp))

                        SettingToggleRow(
                            title = "Haptic Feedback",
                            description = "Subtle tactile click when completing tasks and timer transitions",
                            checked = userPrefs.hapticFeedbackEnabled,
                            onCheckedChange = { settingsViewModel.setHapticFeedbackEnabled(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Task & Calendar Defaults
                SectionHeader(title = "Task & Calendar Defaults")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Default Task Priority
                        Text(
                            text = "Default Task Priority",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Initial priority pre-selected when creating new tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "None",
                                1 to "Low",
                                2 to "Medium",
                                3 to "High",
                                4 to "Urgent"
                            ).forEach { (priorityVal, label) ->
                                val isSelected = userPrefs.defaultTaskPriority == priorityVal
                                val priorityColor = getPriorityColor(priorityVal)
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = 60.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(
                                            role = Role.RadioButton,
                                            onClick = { settingsViewModel.setDefaultTaskPriority(priorityVal) }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (priorityColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(priorityColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Week Start Day
                        Text(
                            text = "First Day of the Week",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Determines month grid columns and weekly timeline order",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DayChoiceChip(
                                label = "Monday",
                                isSelected = userPrefs.weekStartDay == UserPreferences.WEEK_START_MONDAY,
                                onClick = { settingsViewModel.setWeekStartDay(UserPreferences.WEEK_START_MONDAY) },
                                modifier = Modifier.weight(1f)
                            )
                            DayChoiceChip(
                                label = "Sunday",
                                isSelected = userPrefs.weekStartDay == UserPreferences.WEEK_START_SUNDAY,
                                onClick = { settingsViewModel.setWeekStartDay(UserPreferences.WEEK_START_SUNDAY) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // End-of-Day Review Time
                        DurationPickerRow(
                            title = "End-of-Day Review Time",
                            description = "Time to trigger daily shutdown ritual (default 5:00 PM)",
                            selectedValue = userPrefs.endOfDayHour,
                            options = listOf(16, 17, 18, 19, 20),
                            unit = "pm",
                            onSelect = { settingsViewModel.setEndOfDayTime(it, 0) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Focus & Timer Settings
                SectionHeader(title = "Focus & Timer Defaults")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DurationPickerRow(
                            title = "Focus Session Duration",
                            description = "Default deep work block",
                            selectedValue = userPrefs.defaultFocusDurationMinutes,
                            options = listOf(15, 20, 25, 30, 45, 60),
                            unit = "min",
                            onSelect = { settingsViewModel.setDefaultFocusDuration(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        DurationPickerRow(
                            title = "Short Break Duration",
                            description = "Rest between active focus blocks",
                            selectedValue = userPrefs.shortBreakDurationMinutes,
                            options = listOf(3, 5, 8, 10),
                            unit = "min",
                            onSelect = { settingsViewModel.setShortBreakDuration(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        DurationPickerRow(
                            title = "Long Break Duration",
                            description = "Extended recovery rest",
                            selectedValue = userPrefs.longBreakDurationMinutes,
                            options = listOf(10, 15, 20, 30),
                            unit = "min",
                            onSelect = { settingsViewModel.setLongBreakDuration(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        DurationPickerRow(
                            title = "Sessions Until Long Break",
                            description = "Number of focus rounds before extended break",
                            selectedValue = userPrefs.sessionsUntilLongBreak,
                            options = listOf(2, 3, 4, 5),
                            unit = "sessions",
                            onSelect = { settingsViewModel.setSessionsUntilLongBreak(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Organization & Categories Section
                SectionHeader(title = "Organization")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sizeIn(minHeight = 48.dp)
                                .clickable { isManageCategoriesOpen = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Manage Categories",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Add, edit, color-code, or delete task categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Configure",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (onNavigateToManageTemplates != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 48.dp)
                                    .clickable { onNavigateToManageTemplates() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Day Templates",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Configure recurring routines and time-block presets",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Manage",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Backup & Restore Section
                SectionHeader(title = "Backup & Restore")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingActionRow(
                            title = "Create JSON Backup",
                            description = "Export all tasks, categories, habits, and focus logs to a local file",
                            buttonText = "Backup",
                            onClick = {
                                createBackupLauncher.launch("daymark_backup_${LocalDate.now()}.json")
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Restore from Backup",
                            description = "Import and overwrite local data from a previously saved Daymark JSON file",
                            buttonText = "Restore",
                            buttonColor = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Data Export (CSV) Section
                SectionHeader(title = "Spreadsheet Export (CSV)")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingActionRow(
                            title = "Export Tasks to CSV",
                            description = "Spreadsheet of all tasks with due dates, priority, and category names",
                            buttonText = "Export",
                            onClick = {
                                exportTasksCsvLauncher.launch("daymark_tasks_${LocalDate.now()}.csv")
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        SettingActionRow(
                            title = "Export Habits to CSV",
                            description = "Spreadsheet of all habits and consistency configuration",
                            buttonText = "Export",
                            onClick = {
                                exportHabitsCsvLauncher.launch("daymark_habits_${LocalDate.now()}.csv")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 7. Reminders & Reliability Section
                var notificationsOk by remember { mutableStateOf(SystemReliabilityHelper.areNotificationsEnabled(context)) }
                var exactAlarmsOk by remember { mutableStateOf(SystemReliabilityHelper.canScheduleExactAlarms(context)) }
                var batteryOk by remember { mutableStateOf(SystemReliabilityHelper.isIgnoringBatteryOptimizations(context)) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            notificationsOk = SystemReliabilityHelper.areNotificationsEnabled(context)
                            exactAlarmsOk = SystemReliabilityHelper.canScheduleExactAlarms(context)
                            batteryOk = SystemReliabilityHelper.isIgnoringBatteryOptimizations(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                SectionHeader(title = "Reminders & Reliability")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            title = "Daily Summary Reminder",
                            description = "Local on-device notification at 8:00 PM highlighting pending tasks",
                            checked = userPrefs.dailySummaryEnabled,
                            onCheckedChange = { settingsViewModel.setDailySummaryEnabled(it) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        ReliabilityStatusRow(
                            title = "Notifications",
                            description = if (notificationsOk) "Allowed for alarms and task reminders" else "Disabled in system settings",
                            status = if (notificationsOk) "Allowed" else "Disabled",
                            isOk = notificationsOk,
                            actionLabel = if (!notificationsOk) "Enable" else null,
                            onAction = {
                                context.startActivity(SystemReliabilityHelper.createNotificationSettingsIntent(context))
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ReliabilityStatusRow(
                            title = "Exact Alarms",
                            description = if (exactAlarmsOk) "Exact schedule permitted" else "Alarms may be delayed without permission",
                            status = if (exactAlarmsOk) "Allowed" else "Restricted",
                            isOk = exactAlarmsOk,
                            actionLabel = if (!exactAlarmsOk) "Allow" else null,
                            onAction = {
                                context.startActivity(SystemReliabilityHelper.createExactAlarmSettingsIntent(context))
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ReliabilityStatusRow(
                            title = "Battery Optimization",
                            description = if (batteryOk) "Unrestricted background execution" else "May sleep app alarms on Samsung/Android devices",
                            status = if (batteryOk) "Unrestricted" else "Optimized",
                            isOk = batteryOk,
                            actionLabel = if (!batteryOk) "Disable Optimization" else null,
                            onAction = {
                                context.startActivity(SystemReliabilityHelper.createBatteryOptimizationIntent(context))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 8. Privacy & About Section
                SectionHeader(title = "About & Privacy")

                DaymarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = "Data Sovereignty",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "100% Offline & Local Sovereignty",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Zero Cloud Policy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Daymark does not request internet permissions in its Android Manifest. All tasks, categories, subtasks, habits, focus logs, and retrospectives are stored strictly inside your device's local Room database and DataStore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Application Version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v1.0.0 (Local Build)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Restore confirmation warning dialog
    if (backupUiState.showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { backupViewModel.dismissRestoreConfirmation() },
            title = {
                Text(
                    text = "Restore & Overwrite?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Restoring this backup will replace all current tasks, categories, habits, and focus sessions on this device with the backup contents (${backupUiState.pendingRestoreSummary ?: ""}).\n\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { backupViewModel.confirmRestore() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupViewModel.dismissRestoreConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Processing Dialog
    if (backupUiState.isProcessing) {
        Dialog(onDismissRequest = {}) {
            DaymarkCard(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = backupUiState.processingMessage ?: "Processing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (isManageCategoriesOpen) {
        val manageCategoriesViewModel: ManageCategoriesViewModel = viewModel(
            key = "settings_manage_categories",
            factory = ManageCategoriesViewModel.provideFactory(appContainer.categoryRepository)
        )
        ManageCategoriesSheet(
            viewModel = manageCategoriesViewModel,
            onDismissRequest = { isManageCategoriesOpen = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun ThemeOptionItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minHeight = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DayChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DurationPickerRow(
    title: String,
    description: String,
    selectedValue: Int,
    options: List<Int>,
    unit: String,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selectedValue == opt
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 54.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(role = Role.RadioButton, onClick = { onSelect(opt) })
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (unit) {
                            "min" -> "${opt}m"
                            "pm" -> "${if (opt > 12) opt - 12 else opt} PM"
                            else -> "$opt"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    buttonColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(buttonColor)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReliabilityStatusRow(
    title: String,
    description: String,
    status: String,
    isOk: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Preview(name = "Settings - Light Default", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    DaymarkTheme(darkTheme = false) {
        SettingsScreen()
    }
}

@Preview(name = "Settings - Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    DaymarkTheme(darkTheme = true) {
        SettingsScreen()
    }
}

@Preview(name = "Settings - Max Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun SettingsScreenMaxFontPreview() {
    DaymarkTheme {
        SettingsScreen()
    }
}
