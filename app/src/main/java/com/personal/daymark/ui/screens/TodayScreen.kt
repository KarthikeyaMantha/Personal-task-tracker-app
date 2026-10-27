package com.personal.daymark.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import com.personal.daymark.data.local.entity.parsedColor
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.personal.daymark.ui.screens.search.SearchScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.ui.components.DaymarkCard
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import com.personal.daymark.ui.components.DaymarkProgressCard
import com.personal.daymark.ui.components.DeferTaskDialog
import com.personal.daymark.ui.components.DaymarkTopAppBar
import com.personal.daymark.ui.components.SwipeableTaskRow
import com.personal.daymark.ui.components.getPriorityColor
import com.personal.daymark.ui.screens.tasks.AddEditTaskBottomSheet
import com.personal.daymark.ui.screens.tasks.AddEditTaskViewModel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.runtime.rememberCoroutineScope
import com.personal.daymark.core.focus.FocusSessionType
import com.personal.daymark.core.focus.FocusTimerState
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.ui.screens.today.EndOfDayReviewSheet
import com.personal.daymark.ui.screens.today.EndOfDayViewModel
import com.personal.daymark.ui.screens.today.TodayEvent
import com.personal.daymark.ui.screens.today.TodayViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onNavigateToFocus: (() -> Unit)? = null,
    onNavigateToFocusMode: (() -> Unit)? = null,
    onNavigateToInsights: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).container

    val viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.provideFactory(
            taskRepository = appContainer.taskRepository,
            reminderScheduler = appContainer.reminderScheduler
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val userPrefs by appContainer.settingsRepository.userPreferencesFlow
        .collectAsState(initial = UserPreferences())
    val timerData by appContainer.focusTimerManager.timerState.collectAsState()

    var selectedTaskToEdit by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var editSheetSessionKey by remember { mutableLongStateOf(0L) }
    var recurringTaskToDelete by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var taskToDefer by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var addSheetSessionKey by remember { mutableLongStateOf(0L) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isFocusOpen by remember { mutableStateOf(false) }
    var isEndOfDayOpen by remember { mutableStateOf(false) }
    var isEndOfDayPromptDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addSheetSessionKey = System.currentTimeMillis()
                    isAddSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Task"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            DaymarkTopAppBar(
                title = "Daymark",
                actions = {
                    IconButton(onClick = { isEndOfDayOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = "End Day Review",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val isLinkedRunning = timerData.state != FocusTimerState.IDLE && timerData.linkedTaskId != null
                        if (isLinkedRunning && onNavigateToFocusMode != null) {
                            onNavigateToFocusMode()
                        } else if (onNavigateToFocus != null) {
                            onNavigateToFocus()
                        } else {
                            isFocusOpen = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Focus Timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onNavigateToInsights?.invoke() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = "Insights",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { isSearchOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Greeting and Date Header
                Text(
                    text = uiState.greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = uiState.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // End-of-Day Automatic Shutdown Prompt Banner
                val currentCal = remember { Calendar.getInstance() }
                val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
                val currentMinute = currentCal.get(Calendar.MINUTE)
                val isPastShutdownTime = (currentHour > userPrefs.endOfDayHour) ||
                        (currentHour == userPrefs.endOfDayHour && currentMinute >= userPrefs.endOfDayMinute)
                val hasIncompleteItems = uiState.todayTasks.any { !it.task.isCompleted } || uiState.overdueTasks.isNotEmpty()
                val showShutdownBanner = isPastShutdownTime && hasIncompleteItems && !isEndOfDayPromptDismissed

                if (showShutdownBanner) {
                    DaymarkCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End of Day Review",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ready to wrap up? Review remaining tasks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { isEndOfDayOpen = true }) {
                                Text("Review", fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { isEndOfDayPromptDismissed = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // System Reliability & Permissions Banner (Alerts if exact alarms or battery optimization restrict the app)
                com.personal.daymark.ui.components.SystemReliabilityBanner()

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Progress Card
                DaymarkProgressCard(
                    completedCount = uiState.completedTodayCount,
                    totalCount = uiState.totalTodayAndOverdueCount,
                    progressRatio = uiState.progressRatio
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Focus Deep Work Card
                val isTimerActive = timerData.state != FocusTimerState.IDLE
                val isLinkedTimer = isTimerActive && timerData.linkedTaskId != null

                DaymarkCard(
                    containerColor = if (isLinkedTimer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isLinkedTimer) {
                                if (onNavigateToFocusMode != null) onNavigateToFocusMode()
                                else if (onNavigateToFocus != null) onNavigateToFocus()
                                else isFocusOpen = true
                            } else {
                                if (onNavigateToFocus != null) onNavigateToFocus() else isFocusOpen = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLinkedTimer) "Focus Active: ${timerData.linkedTaskTitle ?: "Deep Work"}" else "Deep Work Focus",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isLinkedTimer) "${timerData.formattedRemainingTime} remaining • Tap to enter Focus Mode"
                                else "Start a 25-minute Pomodoro session",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Focus",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }


                // Overdue Tasks Section (prominent alert banner)
                if (uiState.overdueTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Overdue",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Overdue Tasks (${uiState.overdueTasks.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    uiState.overdueTasks.forEach { taskItem ->
                        val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                            com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                        }
                        SwipeableTaskRow(
                            title = taskItem.task.title,
                            isCompleted = taskItem.task.isCompleted,
                            onToggleComplete = { viewModel.toggleTaskCompletion(taskItem) },
                            onDelete = {
                                if (taskItem.task.recurrenceRule != null) {
                                    recurringTaskToDelete = taskItem
                                } else {
                                    viewModel.deleteTask(taskItem)
                                }
                            },
                            categoryName = taskItem.category?.name,
                            categoryColor = taskItem.category?.parsedColor(),
                            dueDateText = DateFormatter.formatDueDate(
                                taskItem.task.dueDate,
                                taskItem.task.dueTime
                            ),
                            recurrenceText = recurrenceRule?.toDisplayString(),
                            subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                            subtasksTotal = taskItem.subtasks.size,
                            priorityColor = getPriorityColor(taskItem.task.priority),
                            isMedicine = taskItem.task.isMedicine,
                            dosage = taskItem.task.dosage,
                            stockCount = taskItem.task.stockCount,
                            onSnooze = { taskToDefer = taskItem },
                            onClick = {
                                selectedTaskToEdit = taskItem
                                editSheetSessionKey = System.currentTimeMillis()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Today's Scheduled Tasks
                Text(
                    text = "Today's Focus",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.todayTasks.isEmpty()) {
                    DaymarkCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No tasks scheduled for today",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enjoy your free time, or tap + below to add a new task.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    uiState.todayTasks.forEach { taskItem ->
                        val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                            com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                        }
                        SwipeableTaskRow(
                            title = taskItem.task.title,
                            isCompleted = taskItem.task.isCompleted,
                            onToggleComplete = { viewModel.toggleTaskCompletion(taskItem) },
                            onDelete = {
                                if (taskItem.task.recurrenceRule != null) {
                                    recurringTaskToDelete = taskItem
                                } else {
                                    viewModel.deleteTask(taskItem)
                                }
                            },
                            categoryName = taskItem.category?.name,
                            categoryColor = taskItem.category?.parsedColor(),
                            dueDateText = DateFormatter.formatDueDate(
                                taskItem.task.dueDate,
                                taskItem.task.dueTime
                            ),
                            recurrenceText = recurrenceRule?.toDisplayString(),
                            subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                            subtasksTotal = taskItem.subtasks.size,
                            priorityColor = getPriorityColor(taskItem.task.priority),
                            isMedicine = taskItem.task.isMedicine,
                            dosage = taskItem.task.dosage,
                            stockCount = taskItem.task.stockCount,
                            onSnooze = { taskToDefer = taskItem },
                            onClick = {
                                selectedTaskToEdit = taskItem
                                editSheetSessionKey = System.currentTimeMillis()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Upcoming Reminders Active Card
                DaymarkCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upcoming Reminders",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.upcomingRemindersCount > 0)
                                    "${uiState.upcomingRemindersCount} alarm reminder${if (uiState.upcomingRemindersCount == 1) "" else "s"} scheduled for today"
                                else
                                    "No further alarms scheduled for today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Task Modal Sheet
    if (isAddSheetOpen) {
        val addViewModel: AddEditTaskViewModel = viewModel(
            key = "today_add_task_vm_$addSheetSessionKey",
            factory = AddEditTaskViewModel.provideFactory(
                taskId = null,
                initialDueDate = DateFormatter.getStartOfTodayMillis(),
                taskRepository = appContainer.taskRepository,
                categoryRepository = appContainer.categoryRepository,
                reminderScheduler = appContainer.reminderScheduler,
                settingsRepository = appContainer.settingsRepository
            )
        )
        AddEditTaskBottomSheet(
            viewModel = addViewModel,
            onDismissRequest = { isAddSheetOpen = false }
        )
    }

    // Edit Task Modal Sheet
    selectedTaskToEdit?.let { taskToEdit ->
        val editViewModel: AddEditTaskViewModel = viewModel(
            key = "today_edit_task_vm_${taskToEdit.task.id}_$editSheetSessionKey",
            factory = AddEditTaskViewModel.provideFactory(
                taskId = taskToEdit.task.id,
                taskRepository = appContainer.taskRepository,
                categoryRepository = appContainer.categoryRepository,
                reminderScheduler = appContainer.reminderScheduler
            )
        )
        AddEditTaskBottomSheet(
            viewModel = editViewModel,
            onDismissRequest = { selectedTaskToEdit = null },
            onStartFocus = { taskId, taskTitle ->
                appContainer.focusTimerManager.startTimer(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    sessionType = FocusSessionType.FOCUS
                )
                selectedTaskToEdit = null
                if (onNavigateToFocusMode != null) onNavigateToFocusMode()
                else if (onNavigateToFocus != null) onNavigateToFocus()
                else isFocusOpen = true
            }
        )
    }

    // End-of-Day Review Modal Sheet
    if (isEndOfDayOpen) {
        val endOfDayViewModel: EndOfDayViewModel = viewModel(
            key = "today_end_of_day_vm",
            factory = EndOfDayViewModel.provideFactory(
                taskRepository = appContainer.taskRepository,
                reminderScheduler = appContainer.reminderScheduler
            )
        )
        EndOfDayReviewSheet(
            viewModel = endOfDayViewModel,
            onDismiss = { isEndOfDayOpen = false },
            onShutdownCompleted = { message ->
                isEndOfDayOpen = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    // Defer / Snooze Task Dialog
    if (taskToDefer != null) {
        val taskItem = taskToDefer!!
        DeferTaskDialog(
            taskTitle = taskItem.task.title,
            onDismissRequest = { taskToDefer = null },
            onDeferToTomorrow = {
                val tomorrowMillis = DateFormatter.getStartOfTodayMillis() + 86400000L
                viewModel.deferTask(taskItem, tomorrowMillis, "Deferred to Tomorrow")
                taskToDefer = null
            },
            onDeferToNextWeek = {
                val nextWeekMillis = DateFormatter.getStartOfTodayMillis() + 7 * 86400000L
                viewModel.deferTask(taskItem, nextWeekMillis, "Deferred to Next Week")
                taskToDefer = null
            },
            onPickCustomDate = { selectedMillis ->
                val dateLabel = DateFormatter.formatRelativeDate(selectedMillis)
                viewModel.deferTask(taskItem, selectedMillis, "Deferred to $dateLabel")
                taskToDefer = null
            }
        )
    }

    // Delete Recurring Task Confirmation Dialog
    recurringTaskToDelete?.let { taskItem ->
        AlertDialog(
            onDismissRequest = { recurringTaskToDelete = null },
            title = { Text("Delete Recurring Task") },
            text = { Text("Do you want to delete only this occurrence, or delete all future occurrences in this recurring series?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTaskSeries(taskItem)
                        recurringTaskToDelete = null
                    }
                ) {
                    Text("Entire series")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTaskOccurrence(taskItem)
                        recurringTaskToDelete = null
                    }
                ) {
                    Text("This task only")
                }
            }
        )
    }

    // Global Search Screen
    if (isSearchOpen) {
        SearchScreen(
            onDismiss = { isSearchOpen = false },
            onTaskClick = { task ->
                isSearchOpen = false
                selectedTaskToEdit = task
                editSheetSessionKey = System.currentTimeMillis()
            }
        )
    }

    // Fallback Focus Screen Modal
    if (isFocusOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFocusOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.personal.daymark.ui.screens.focus.FocusScreen(
                onNavigateBack = { isFocusOpen = false }
            )
        }
    }
}
