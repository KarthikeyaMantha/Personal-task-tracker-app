package com.personal.daymark.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.DaymarkTextField
import com.personal.daymark.ui.components.SwipeableTaskRow
import com.personal.daymark.ui.components.getPriorityColor
import com.personal.daymark.ui.screens.tasks.AddEditTaskBottomSheet
import com.personal.daymark.ui.screens.tasks.AddEditTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onDismiss: () -> Unit,
    onTaskClick: (TaskWithSubtasksAndCategory) -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).container

    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(
            taskRepository = appContainer.taskRepository,
            categoryRepository = appContainer.categoryRepository,
            reminderScheduler = appContainer.reminderScheduler
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    var selectedTaskToEdit by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 8.dp)
                    ) {
                        // Search Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                DaymarkTextField(
                                    value = uiState.filterState.query,
                                    onValueChange = viewModel::onQueryChanged,
                                    placeholder = "Search tasks, notes, or subtasks...",
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        if (uiState.filterState.query.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear search query",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true
                                )
                            }

                            // Filter Button with Badge
                            IconButton(onClick = { viewModel.openFilterSheet() }) {
                                val activeCount = uiState.filterState.activeFilterCount
                                BadgedBox(
                                    badge = {
                                        if (activeCount > 0) {
                                            Badge {
                                                Text(activeCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filters",
                                        tint = if (activeCount > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Filter Chips Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (uiState.filterState.completionFilter != TaskCompletionFilter.ALL)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .clickable { viewModel.openFilterSheet() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = uiState.filterState.completionFilter.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.filterState.completionFilter != TaskCompletionFilter.ALL)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Category Chip
                            val selectedCategory = uiState.categories.find { it.id == uiState.filterState.selectedCategoryId }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedCategory != null) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .clickable { viewModel.openFilterSheet() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedCategory != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(selectedCategory.parsedColor())
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = selectedCategory?.name ?: "All Categories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedCategory != null) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Priority Chip
                            val priorityLabel = when (uiState.filterState.selectedPriority) {
                                4 -> "Priority: Urgent"
                                3 -> "Priority: High"
                                2 -> "Priority: Medium"
                                1 -> "Priority: Low"
                                0 -> "Priority: None"
                                else -> "All Priorities"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (uiState.filterState.selectedPriority != null) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .clickable { viewModel.openFilterSheet() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = priorityLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.filterState.selectedPriority != null) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Date Range Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (uiState.filterState.dateRangeFilter != TaskDateRangeFilter.ALL)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .clickable { viewModel.openFilterSheet() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = uiState.filterState.dateRangeFilter.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.filterState.dateRangeFilter != TaskDateRangeFilter.ALL)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Reset Filters Chip
                            if (uiState.filterState.activeFilterCount > 0 || uiState.filterState.query.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                        .clickable { viewModel.clearAll() }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Reset All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Result Summary Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.totalCount} task(s) found",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sorted by ${uiState.filterState.sortBy.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Search Results List or Empty State
                    if (uiState.searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.filterState.query.isNotBlank()) "No matching tasks found"
                                    else "No tasks match your filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try adjusting your search keywords, category, priority, or date filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (uiState.filterState.activeFilterCount > 0 || uiState.filterState.query.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { viewModel.clearAll() }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Clear All Filters",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.searchResults,
                                key = { it.task.id }
                            ) { taskItem ->
                                val recurrenceRule = remember(taskItem.task.recurrenceRule) {
                                    com.personal.daymark.core.recurrence.RecurrenceRule.fromJson(taskItem.task.recurrenceRule)
                                }
                                Column {
                                    SwipeableTaskRow(
                                        title = taskItem.task.title,
                                        isCompleted = taskItem.task.isCompleted,
                                        onToggleComplete = { viewModel.toggleTaskCompletion(taskItem) },
                                        onDelete = { viewModel.deleteTask(taskItem) },
                                        categoryName = taskItem.category?.name,
                                        categoryColor = taskItem.category?.parsedColor(),
                                        dueDateText = DateFormatter.formatDueDate(taskItem.task.dueDate, taskItem.task.dueTime),
                                        recurrenceText = recurrenceRule?.toDisplayString(),
                                        subtasksCompleted = taskItem.subtasks.count { it.isCompleted },
                                        subtasksTotal = taskItem.subtasks.size,
                                        priorityColor = getPriorityColor(taskItem.task.priority),
                                        onClick = { selectedTaskToEdit = taskItem }
                                    )

                                    // If query matched in description notes, show matching snippet
                                    val query = uiState.filterState.query.trim().lowercase()
                                    val desc = taskItem.task.description
                                    if (query.isNotEmpty() && desc != null && desc.lowercase().contains(query) && !taskItem.task.title.lowercase().contains(query)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 44.dp, top = 2.dp, end = 12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Matches note: \"$desc\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filter Bottom Sheet
            if (uiState.filterState.isFilterSheetOpen) {
                SearchFilterBottomSheet(
                    filterState = uiState.filterState,
                    categories = uiState.categories,
                    onCompletionSelected = viewModel::onCompletionFilterSelected,
                    onCategorySelected = viewModel::onCategorySelected,
                    onPrioritySelected = viewModel::onPrioritySelected,
                    onDateRangeSelected = viewModel::onDateRangeSelected,
                    onSortSelected = viewModel::onSortBySelected,
                    onResetFilters = viewModel::resetFilters,
                    onDismissRequest = { viewModel.closeFilterSheet() }
                )
            }

            // Edit Task Sheet
            selectedTaskToEdit?.let { taskToEdit ->
                val editViewModel: AddEditTaskViewModel = viewModel(
                    key = "search_edit_task_${taskToEdit.task.id}",
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
        }
    }
}
