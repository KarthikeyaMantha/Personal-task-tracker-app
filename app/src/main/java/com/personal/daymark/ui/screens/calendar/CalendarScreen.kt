package com.personal.daymark.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.DaymarkCard
import com.personal.daymark.ui.components.DaymarkTopAppBar
import com.personal.daymark.ui.components.SwipeableTaskRow
import com.personal.daymark.ui.components.getPriorityColor
import com.personal.daymark.ui.screens.search.SearchScreen
import com.personal.daymark.ui.screens.tasks.AddEditTaskBottomSheet
import com.personal.daymark.ui.screens.tasks.AddEditTaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToManageTemplates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).container

    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(
            taskRepository = appContainer.taskRepository,
            timeBlockRepository = appContainer.timeBlockRepository,
            templateRepository = appContainer.templateRepository,
            reminderScheduler = appContainer.reminderScheduler,
            settingsRepository = appContainer.settingsRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    var selectedTaskToEdit by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var isSearchOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openQuickAdd() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Task for ${uiState.selectedDate}"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            DaymarkTopAppBar(
                title = "Calendar",
                actions = {
                    IconButton(onClick = { isSearchOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.jumpToToday() }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Jump to Today",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // View Mode Selector Tabs (Daily, Weekly, Monthly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalendarViewMode.entries.forEach { mode ->
                    val isSelected = uiState.viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .clickable { viewModel.selectViewMode(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Navigation Strip with Arrows & Current Date/Month Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigatePrevious() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                val headerTitle = when (uiState.viewMode) {
                    CalendarViewMode.MONTHLY -> {
                        uiState.displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    }
                    CalendarViewMode.WEEKLY -> {
                        val firstDay = uiState.weekDays.firstOrNull()?.date ?: uiState.selectedDate
                        val lastDay = uiState.weekDays.lastOrNull()?.date ?: uiState.selectedDate
                        val monthFmt = DateTimeFormatter.ofPattern("MMM d")
                        "${firstDay.format(monthFmt)} – ${lastDay.format(monthFmt)}, ${lastDay.year}"
                    }
                    CalendarViewMode.DAILY -> {
                        uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    }
                }

                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.navigateNext() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Main Content Area based on View Mode
            when (uiState.viewMode) {
                CalendarViewMode.MONTHLY -> {
                    MonthlyCalendarView(
                        uiState = uiState,
                        onDateSelected = viewModel::selectDate,
                        onToggleTask = viewModel::toggleTaskCompletion,
                        onDeleteTask = viewModel::deleteTask,
                        onTaskClick = { selectedTaskToEdit = it }
                    )
                }
                CalendarViewMode.WEEKLY -> {
                    WeeklyCalendarView(
                        uiState = uiState,
                        onDateSelected = viewModel::selectDate,
                        onToggleTask = viewModel::toggleTaskCompletion,
                        onDeleteTask = viewModel::deleteTask,
                        onTaskClick = { selectedTaskToEdit = it }
                    )
                }
                CalendarViewMode.DAILY -> {
                    com.personal.daymark.ui.screens.timeline.DailyTimelineScreen(
                        selectedDate = uiState.selectedDate,
                        tasks = uiState.tasksForSelectedDate,
                        busyBlocks = uiState.busyBlocksForSelectedDate,
                        templates = uiState.templates,
                        onUpdateTaskTimeline = viewModel::updateTaskTimeline,
                        onToggleTaskComplete = viewModel::toggleTaskCompletion,
                        onTaskClick = { selectedTaskToEdit = it },
                        onAddBusyBlock = { title, start, dur -> viewModel.addBusyBlock(title, start, dur, uiState.selectedDate) },
                        onDeleteBusyBlock = viewModel::deleteBusyBlock,
                        onApplyTemplate = { templateId -> viewModel.applyTemplate(templateId, uiState.selectedDate) },
                        onManageTemplates = { onNavigateToManageTemplates?.invoke() },
                        onQuickAdd = { viewModel.openQuickAdd() }
                    )
                }
            }
        }
    }

    // Quick Add Task Sheet pre-filled with selected calendar date
    if (uiState.isQuickAddOpen) {
        val addViewModel: AddEditTaskViewModel = viewModel(
            key = "calendar_quick_add_${uiState.quickAddDateMillis}",
            factory = AddEditTaskViewModel.provideFactory(
                taskId = null,
                initialDueDate = uiState.quickAddDateMillis,
                taskRepository = appContainer.taskRepository,
                categoryRepository = appContainer.categoryRepository,
                reminderScheduler = appContainer.reminderScheduler,
                settingsRepository = appContainer.settingsRepository
            )
        )
        AddEditTaskBottomSheet(
            viewModel = addViewModel,
            onDismissRequest = { viewModel.closeQuickAdd() }
        )
    }

    // Edit Task Sheet
    selectedTaskToEdit?.let { taskToEdit ->
        val editViewModel: AddEditTaskViewModel = viewModel(
            key = "calendar_edit_task_${taskToEdit.task.id}",
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

    // Global Search Modal / Screen
    if (isSearchOpen) {
        SearchScreen(
            onDismiss = { isSearchOpen = false },
            onTaskClick = { task ->
                isSearchOpen = false
                selectedTaskToEdit = task
            }
        )
    }
}

/**
 * Monthly Calendar View: 7-column calendar grid with indicator dots and selected-day agenda.
 */
@Composable
private fun MonthlyCalendarView(
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onToggleTask: (TaskWithSubtasksAndCategory) -> Unit,
    onDeleteTask: (TaskWithSubtasksAndCategory) -> Unit,
    onTaskClick: (TaskWithSubtasksAndCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Weekday column headers (Mon - Sun or Sun - Sat based on preference)
        item {
            val dayLabels = if (uiState.weekStartDay == com.personal.daymark.data.datastore.UserPreferences.WEEK_START_SUNDAY) {
                listOf("S", "M", "T", "W", "T", "F", "S")
            } else {
                listOf("M", "T", "W", "T", "F", "S", "S")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayLabels.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Calendar Grid Days
        item {
            val rows = uiState.monthGridDays.chunked(7)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { day ->
                            CalendarDayCell(
                                day = day,
                                onClick = { onDateSelected(day.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Minimalist Completion Heatmap Legend
        item {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDark) Color(0xFF1B4332).copy(alpha = 0.50f) else Color(0xFFD8EADF).copy(alpha = 0.85f))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "1–3",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDark) Color(0xFF2D6A4F) else Color(0xFF1B4332))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "4+",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Overdue Alert Section (visible on past dates or today)
        if (uiState.overdueTasks.isNotEmpty() && !uiState.selectedDate.isAfter(LocalDate.now())) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Overdue",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.overdueTasks.size} overdue task(s) require attention",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Selected Day Header
        item {
            val dateLabel = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.tasksForSelectedDate.size} task(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Task Items for selected date
        if (uiState.tasksForSelectedDate.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks scheduled for this day.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = uiState.tasksForSelectedDate,
                key = { it.task.id }
            ) { taskItem ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                        com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                    }
                    SwipeableTaskRow(
                        title = taskItem.task.title,
                        isCompleted = taskItem.task.isCompleted,
                        onToggleComplete = { onToggleTask(taskItem) },
                        onDelete = { onDeleteTask(taskItem) },
                        categoryName = taskItem.category?.name,
                        categoryColor = taskItem.category?.parsedColor(),
                        dueDateText = DateFormatter.formatDueDate(taskItem.task.dueDate, taskItem.task.dueTime),
                        recurrenceText = recurrenceRule?.toDisplayString(),
                        subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                        subtasksTotal = taskItem.subtasks.size,
                        priorityColor = getPriorityColor(taskItem.task.priority),
                        onClick = { onTaskClick(taskItem) }
                    )
                }
            }
        }
    }
}

/**
 * Weekly Calendar View: 7-day strip with day timeline.
 */
@Composable
private fun WeeklyCalendarView(
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onToggleTask: (TaskWithSubtasksAndCategory) -> Unit,
    onDeleteTask: (TaskWithSubtasksAndCategory) -> Unit,
    onTaskClick: (TaskWithSubtasksAndCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Week Strip Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                uiState.weekDays.forEach { day ->
                    val isSelected = day.date == uiState.selectedDate
                    val isToday = day.isToday
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            )
                            .clickable { onDateSelected(day.date) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.date.format(DateTimeFormatter.ofPattern("EEE")),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(4.dp)
                            ) {
                                day.taskDots.forEach { dotColor ->
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.tasksForSelectedDate.size} task(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (uiState.tasksForSelectedDate.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks scheduled for this day.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = uiState.tasksForSelectedDate,
                key = { it.task.id }
            ) { taskItem ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                        com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                    }
                    SwipeableTaskRow(
                        title = taskItem.task.title,
                        isCompleted = taskItem.task.isCompleted,
                        onToggleComplete = { onToggleTask(taskItem) },
                        onDelete = { onDeleteTask(taskItem) },
                        categoryName = taskItem.category?.name,
                        categoryColor = taskItem.category?.parsedColor(),
                        dueDateText = DateFormatter.formatDueDate(taskItem.task.dueDate, taskItem.task.dueTime),
                        recurrenceText = recurrenceRule?.toDisplayString(),
                        subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                        subtasksTotal = taskItem.subtasks.size,
                        priorityColor = getPriorityColor(taskItem.task.priority),
                        onClick = { onTaskClick(taskItem) }
                    )
                }
            }
        }
    }
}

/**
 * Daily Calendar View: Focused agenda view with time-ordered tasks and overdue section.
 */
@Composable
private fun DailyCalendarView(
    uiState: CalendarUiState,
    onToggleTask: (TaskWithSubtasksAndCategory) -> Unit,
    onDeleteTask: (TaskWithSubtasksAndCategory) -> Unit,
    onTaskClick: (TaskWithSubtasksAndCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Daily Focus Header Card
        item {
            DaymarkCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.selectedDate == LocalDate.now()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Overdue Alert Section
        if (uiState.overdueTasks.isNotEmpty() && !uiState.selectedDate.isAfter(LocalDate.now())) {
            item {
                Text(
                    text = "Overdue Attention (${uiState.overdueTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(
                items = uiState.overdueTasks,
                key = { "overdue_${it.task.id}" }
            ) { taskItem ->
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                        com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                    }
                    SwipeableTaskRow(
                        title = taskItem.task.title,
                        isCompleted = taskItem.task.isCompleted,
                        onToggleComplete = { onToggleTask(taskItem) },
                        onDelete = { onDeleteTask(taskItem) },
                        categoryName = taskItem.category?.name,
                        categoryColor = taskItem.category?.parsedColor(),
                        dueDateText = DateFormatter.formatDueDate(taskItem.task.dueDate, taskItem.task.dueTime),
                        recurrenceText = recurrenceRule?.toDisplayString(),
                        subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                        subtasksTotal = taskItem.subtasks.size,
                        priorityColor = getPriorityColor(taskItem.task.priority),
                        onClick = { onTaskClick(taskItem) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Scheduled Tasks Section
        item {
            Text(
                text = "Scheduled Tasks (${uiState.tasksForSelectedDate.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.tasksForSelectedDate.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks scheduled for today.\nTap + below to plan ahead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = uiState.tasksForSelectedDate,
                key = { it.task.id }
            ) { taskItem ->
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                        com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                    }
                    SwipeableTaskRow(
                        title = taskItem.task.title,
                        isCompleted = taskItem.task.isCompleted,
                        onToggleComplete = { onToggleTask(taskItem) },
                        onDelete = { onDeleteTask(taskItem) },
                        categoryName = taskItem.category?.name,
                        categoryColor = taskItem.category?.parsedColor(),
                        dueDateText = DateFormatter.formatDueDate(taskItem.task.dueDate, taskItem.task.dueTime),
                        recurrenceText = recurrenceRule?.toDisplayString(),
                        subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                        subtasksTotal = taskItem.subtasks.size,
                        priorityColor = getPriorityColor(taskItem.task.priority),
                        onClick = { onTaskClick(taskItem) }
                    )
                }
            }
        }
    }
}

/**
 * Calendar Grid Day Cell: Displays day number and task indicator dots.
 */
@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val completedCount = day.completedCount

    // Completion Heatmap color rules:
    // 0 completed = transparent / default background
    // 1-3 completed = very light forest-green/teal tint
    // 4+ completed = deep, rich forest-green/teal
    val heatmapBgColor: Color = when {
        completedCount == 0 -> Color.Transparent
        completedCount in 1..3 -> {
            if (isDark) Color(0xFF1B4332).copy(alpha = 0.50f)
            else Color(0xFFD8EADF).copy(alpha = 0.85f)
        }
        else -> { // 4+ completed
            if (isDark) Color(0xFF2D6A4F)
            else Color(0xFF1B4332)
        }
    }

    val backgroundColor by animateColorAsState(
        when {
            day.isSelected -> MaterialTheme.colorScheme.primary
            heatmapBgColor != Color.Transparent -> heatmapBgColor
            day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        label = "dayCellBg"
    )

    val textColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary
        completedCount >= 4 -> Color.White // Deep forest-green requires high-contrast white text
        completedCount in 1..3 -> {
            if (isDark) Color(0xFFB7E4C7) else Color(0xFF0E281D)
        }
        day.isToday -> MaterialTheme.colorScheme.primary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val isHeatmapActive = completedCount > 0 && !day.isSelected

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(
                if (day.isSelected && completedCount > 0) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(10.dp))
                } else if (day.isToday && !day.isSelected && completedCount == 0) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isSelected || day.isToday || completedCount >= 4) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            // Category indicator dots (or completion indicator)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .height(4.dp)
                    .padding(top = 2.dp)
            ) {
                if (day.isSelected) {
                    repeat(day.taskDots.size.coerceAtMost(3)) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        )
                    }
                } else if (completedCount >= 4) {
                    // Subtle white dots for high-completion days
                    repeat(day.taskDots.size.coerceAtMost(3).coerceAtLeast(1)) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }
                } else {
                    day.taskDots.forEach { dotColor ->
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}
