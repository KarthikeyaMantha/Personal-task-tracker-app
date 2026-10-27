package com.personal.daymark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.screens.search.SearchScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.ui.components.DaymarkTextField
import com.personal.daymark.ui.components.DaymarkTopAppBar
import com.personal.daymark.ui.components.SwipeableTaskRow
import com.personal.daymark.ui.components.getPriorityColor
import com.personal.daymark.ui.screens.tasks.AddEditTaskBottomSheet
import com.personal.daymark.ui.screens.tasks.AddEditTaskViewModel
import com.personal.daymark.ui.screens.tasks.TaskListEvent
import com.personal.daymark.ui.screens.tasks.TaskListViewModel
import com.personal.daymark.ui.screens.tasks.TaskSortOption
import com.personal.daymark.ui.screens.tasks.TaskViewFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    targetTaskId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).container

    val viewModel: TaskListViewModel = viewModel(
        factory = TaskListViewModel.provideFactory(
            taskRepository = appContainer.taskRepository,
            categoryRepository = appContainer.categoryRepository,
            reminderScheduler = appContainer.reminderScheduler
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTaskToEdit by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var editSheetSessionKey by remember { mutableLongStateOf(0L) }
    var recurringTaskToDelete by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var taskToDefer by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var addSheetSessionKey by remember { mutableLongStateOf(0L) }
    var isManageCategoriesOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(targetTaskId, uiState.tasks) {
        if (targetTaskId != null && targetTaskId > 0) {
            val matchingTask = uiState.tasks.firstOrNull { it.task.id == targetTaskId }
            if (matchingTask != null) {
                selectedTaskToEdit = matchingTask
                editSheetSessionKey = System.currentTimeMillis()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskListEvent.ShowSnackbar -> {
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
        ) {
            DaymarkTopAppBar(
                title = "Tasks",
                actions = {
                    IconButton(onClick = { isSearchOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { isSortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            TaskSortOption.entries.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOption.label,
                                            fontWeight = if (uiState.selectedSortOption == sortOption) FontWeight.Bold
                                            else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.onSortOptionSelected(sortOption)
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // Search Bar (collapsible)
            if (isSearchExpanded) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DaymarkTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = "Search in ${uiState.selectedViewFilter.label}...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Primary View Filter Tabs (All, Today, Inbox, Upcoming, Overdue, Completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskViewFilter.entries.forEach { filter ->
                    val isSelected = uiState.selectedViewFilter == filter
                    val badgeCount = when (filter) {
                        TaskViewFilter.ALL -> null
                        TaskViewFilter.TODAY -> uiState.todayCount
                        TaskViewFilter.INBOX -> uiState.inboxCount
                        TaskViewFilter.UPCOMING -> uiState.upcomingCount
                        TaskViewFilter.OVERDUE -> uiState.overdueCount
                        TaskViewFilter.COMPLETED -> uiState.completedCount
                    }

                    val isAlertFilter = filter == TaskViewFilter.OVERDUE && (badgeCount ?: 0) > 0

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> if (isAlertFilter) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                    isAlertFilter -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainer
                                }
                            )
                            .clickable { viewModel.onViewFilterSelected(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = when {
                                    isSelected -> if (isAlertFilter) MaterialTheme.colorScheme.onError
                                    else MaterialTheme.colorScheme.onPrimary
                                    isAlertFilter -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )

                            if (badgeCount != null && badgeCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            if (uiState.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllCatSelected = uiState.selectedCategoryId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isAllCatSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable { viewModel.onCategorySelected(null) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "All Categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAllCatSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    uiState.categories.forEach { category ->
                        val isSelected = uiState.selectedCategoryId == category.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable { viewModel.onCategorySelected(category.id) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(category.parsedColor())
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Manage Categories Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { isManageCategoriesOpen = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Manage Categories",
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Task List or Empty State
            if (uiState.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val emptyTitle = when {
                            uiState.searchQuery.isNotBlank() -> "No matching tasks"
                            uiState.selectedViewFilter == TaskViewFilter.OVERDUE -> "No overdue tasks"
                            uiState.selectedViewFilter == TaskViewFilter.INBOX -> "Inbox is clear"
                            uiState.selectedViewFilter == TaskViewFilter.TODAY -> "Nothing due today"
                            uiState.selectedViewFilter == TaskViewFilter.UPCOMING -> "No upcoming scheduled tasks"
                            uiState.selectedViewFilter == TaskViewFilter.COMPLETED -> "No completed tasks yet"
                            else -> "No tasks yet"
                        }

                        val emptyMessage = when {
                            uiState.searchQuery.isNotBlank() -> "Try searching for a different keyword."
                            uiState.selectedViewFilter == TaskViewFilter.OVERDUE -> "You're completely on schedule."
                            uiState.selectedViewFilter == TaskViewFilter.INBOX -> "All tasks have been scheduled or completed."
                            uiState.selectedViewFilter == TaskViewFilter.TODAY -> "Take time to relax or add a new task."
                            uiState.selectedViewFilter == TaskViewFilter.UPCOMING -> "Tasks scheduled for future dates will appear here."
                            uiState.selectedViewFilter == TaskViewFilter.COMPLETED -> "Completed tasks will be recorded here for review."
                            else -> "Tap the + button to capture what's on your mind."
                        }

                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.tasks,
                        key = { it.task.id }
                    ) { taskItem ->
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
                    }
                }
            }
        }
    }

    // Manage Categories Sheet
    if (isManageCategoriesOpen) {
        val manageCategoriesViewModel: com.personal.daymark.ui.screens.categories.ManageCategoriesViewModel = viewModel(
            factory = com.personal.daymark.ui.screens.categories.ManageCategoriesViewModel.provideFactory(appContainer.categoryRepository)
        )
        com.personal.daymark.ui.screens.categories.ManageCategoriesSheet(
            viewModel = manageCategoriesViewModel,
            onDismissRequest = { isManageCategoriesOpen = false }
        )
    }

    // Add Task Sheet
    if (isAddSheetOpen) {
        val addViewModel: AddEditTaskViewModel = viewModel(
            key = "tasks_view_add_vm_$addSheetSessionKey",
            factory = AddEditTaskViewModel.provideFactory(
                taskId = null,
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

    // Edit Task Sheet
    selectedTaskToEdit?.let { taskToEdit ->
        val editViewModel: AddEditTaskViewModel = viewModel(
            key = "tasks_view_edit_vm_${taskToEdit.task.id}_$editSheetSessionKey",
            factory = AddEditTaskViewModel.provideFactory(
                taskId = taskToEdit.task.id,
                taskRepository = appContainer.taskRepository,
                categoryRepository = appContainer.categoryRepository,
                reminderScheduler = appContainer.reminderScheduler
            )
        )
        AddEditTaskBottomSheet(
            viewModel = editViewModel,
            onDismissRequest = { selectedTaskToEdit = null }
        )
    }

    // Defer / Snooze Task Dialog
    if (taskToDefer != null) {
        val taskItem = taskToDefer!!
        com.personal.daymark.ui.components.DeferTaskDialog(
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
}
