package com.personal.daymark.ui.screens.tasks

import com.personal.daymark.core.recurrence.RecurrenceFrequency
import com.personal.daymark.core.recurrence.RecurrenceRule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import com.personal.daymark.data.local.entity.parsedColor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.ui.components.DaymarkButton
import com.personal.daymark.ui.components.DaymarkTextField
import com.personal.daymark.ui.components.getPriorityColor
import java.util.Calendar

import androidx.compose.material.icons.outlined.Timer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskBottomSheet(
    viewModel: AddEditTaskViewModel,
    onDismissRequest: () -> Unit,
    onStartFocus: ((Long, String) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var isManageCategoriesOpen by remember { mutableStateOf(false) }

    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val tomorrowMillis = remember { todayMillis + 86400000L }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditTaskEvent.SaveSuccess -> onDismissRequest()
                is AddEditTaskEvent.ShowError -> {}
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.taskId == null || uiState.taskId == 0L) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Title (Primary quick-entry field)
            DaymarkTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                placeholder = "What needs to be done?",
                isError = uiState.isTitleError,
                errorMessage = if (uiState.isTitleError) "Task title is required" else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.saveTask()
                })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Task Description / Notes (Optional)
            DaymarkTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                placeholder = "Notes or extra details (optional)",
                singleLine = false,
                maxLines = 3
            )

            // Subtasks / Checklist
            Spacer(modifier = Modifier.height(18.dp))
            val completedSubtasksCount = uiState.subtasks.count { it.isCompleted }
            val totalSubtasksCount = uiState.subtasks.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (totalSubtasksCount > 0) "Checklist ($completedSubtasksCount/$totalSubtasksCount)" else "Checklist",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtask items list
            if (uiState.subtasks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.subtasks.forEachIndexed { index, subtask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (subtask.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.5.dp, if (subtask.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { viewModel.toggleSubtask(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (subtask.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (subtask.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { viewModel.deleteSubtask(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove subtask",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick entry row to add subtask
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DaymarkTextField(
                        value = uiState.newSubtaskInput,
                        onValueChange = viewModel::onNewSubtaskInputChanged,
                        placeholder = "Add a checklist item...",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.addSubtask()
                        })
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::addSubtask,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtask",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority Selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                    val isSelected = uiState.priority == priorityVal
                    val priorityColor = getPriorityColor(priorityVal)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable { viewModel.onPrioritySelected(priorityVal) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Due Date Selector
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickDates: List<Pair<Long?, String>> = listOf(
                    null to "No Date",
                    todayMillis to "Today",
                    tomorrowMillis to "Tomorrow"
                )

                val isCustomDate = uiState.dueDate != null && uiState.dueDate != todayMillis && uiState.dueDate != tomorrowMillis
                val customDateLabel = remember(uiState.dueDate) {
                    if (isCustomDate && uiState.dueDate != null) {
                        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(uiState.dueDate!!))
                    } else "Pick Date..."
                }

                quickDates.forEach { (dateMillis, label) ->
                    val isSelected = uiState.dueDate == dateMillis
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable { viewModel.onDueDateSelected(dateMillis) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dateMillis != null) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Custom Date Picker Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCustomDate) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .clickable {
                            val calendar = Calendar.getInstance().apply {
                                uiState.dueDate?.let { timeInMillis = it }
                            }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.onDueDateSelected(selectedCal.timeInMillis)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isCustomDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = customDateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCustomDate) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reminder Time Selector
            Text(
                text = "Reminder Time",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isCustomTime = uiState.dueTimeMinutes != null &&
                        uiState.dueTimeMinutes != 540 &&
                        uiState.dueTimeMinutes != 780 &&
                        uiState.dueTimeMinutes != 1080

                val customTimeLabel = remember(uiState.dueTimeMinutes) {
                    if (uiState.dueTimeMinutes != null) {
                        val hours = uiState.dueTimeMinutes!! / 60
                        val minutes = uiState.dueTimeMinutes!! % 60
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hours)
                            set(Calendar.MINUTE, minutes)
                        }
                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
                    } else "Pick Time..."
                }

                val quickTimes = listOf(
                    null to "No Time",
                    540 to "Morning (9:00 AM)",
                    780 to "Afternoon (1:00 PM)",
                    1080 to "Evening (6:00 PM)"
                )

                quickTimes.forEach { (timeMins, label) ->
                    val isSelected = uiState.dueTimeMinutes == timeMins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable {
                                if (uiState.dueDate == null && timeMins != null) {
                                    // Default to Today if no date was chosen yet
                                    viewModel.onDueDateSelected(todayMillis)
                                }
                                viewModel.onDueTimeSelected(timeMins)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Custom Time Picker Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCustomTime) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .clickable {
                            val initialHours = (uiState.dueTimeMinutes ?: 540) / 60
                            val initialMinutes = (uiState.dueTimeMinutes ?: 540) % 60
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    if (uiState.dueDate == null) {
                                        viewModel.onDueDateSelected(todayMillis)
                                    }
                                    viewModel.onDueTimeSelected(hourOfDay * 60 + minute)
                                },
                                initialHours,
                                initialMinutes,
                                android.text.format.DateFormat.is24HourFormat(context)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCustomTime) customTimeLabel else "Pick Time...",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCustomTime) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Scheduled Reminder Confirmation Badge
            if (uiState.dueDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val formattedReminder = DateFormatter.formatDueDate(uiState.dueDate, uiState.dueTimeMinutes)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "⏰ Reminder scheduled for $formattedReminder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Repeat / Recurrence Selector
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val repeatOptions: List<Pair<RecurrenceFrequency?, String>> = listOf(
                    null to "Does not repeat",
                    RecurrenceFrequency.DAILY to "Daily",
                    RecurrenceFrequency.WEEKDAYS to "Weekdays",
                    RecurrenceFrequency.WEEKLY to "Weekly",
                    RecurrenceFrequency.MONTHLY to "Monthly"
                )

                repeatOptions.forEach { (freq, label) ->
                    val isSelected = (freq == null && uiState.recurrenceRule == null) ||
                            (freq != null && uiState.recurrenceRule?.frequency == freq)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable {
                                if (freq == null) {
                                    viewModel.onRecurrenceRuleSelected(null)
                                } else {
                                    if (uiState.dueDate == null) {
                                        viewModel.onDueDateSelected(todayMillis)
                                    }
                                    val newRule = when (freq) {
                                        RecurrenceFrequency.WEEKLY -> {
                                            val cal = Calendar.getInstance().apply {
                                                timeInMillis = uiState.dueDate ?: todayMillis
                                            }
                                            val dayOfWeekJava = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                                Calendar.MONDAY -> 1
                                                Calendar.TUESDAY -> 2
                                                Calendar.WEDNESDAY -> 3
                                                Calendar.THURSDAY -> 4
                                                Calendar.FRIDAY -> 5
                                                Calendar.SATURDAY -> 6
                                                Calendar.SUNDAY -> 7
                                                else -> 1
                                            }
                                            RecurrenceRule(
                                                frequency = RecurrenceFrequency.WEEKLY,
                                                daysOfWeek = listOf(dayOfWeekJava)
                                            )
                                        }
                                        else -> RecurrenceRule(frequency = freq)
                                    }
                                    viewModel.onRecurrenceRuleSelected(newRule)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Specific weekdays toggles if Weekly is selected
            if (uiState.recurrenceRule?.frequency == RecurrenceFrequency.WEEKLY) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val days = listOf(
                        1 to "M",
                        2 to "T",
                        3 to "W",
                        4 to "T",
                        5 to "F",
                        6 to "S",
                        7 to "S"
                    )
                    days.forEach { (dayVal, name) ->
                        val isDaySelected = uiState.recurrenceRule?.daysOfWeek?.contains(dayVal) == true
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isDaySelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable {
                                    val currentList = uiState.recurrenceRule?.daysOfWeek.orEmpty().toMutableList()
                                    if (isDaySelected) {
                                        if (currentList.size > 1) {
                                            currentList.remove(dayVal)
                                        }
                                    } else {
                                        currentList.add(dayVal)
                                    }
                                    viewModel.onRecurrenceRuleSelected(
                                        uiState.recurrenceRule?.copy(daysOfWeek = currentList.sorted())
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Recurrence Confirmation Badge
            if (uiState.recurrenceRule != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🔁 Repeats ${uiState.recurrenceRule!!.toDisplayString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Category Selector
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+ Manage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isManageCategoriesOpen = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isNoneSelected = uiState.categoryId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .clickable { viewModel.onCategorySelected(null) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isNoneSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                categories.forEach { cat ->
                    val isSelected = uiState.categoryId == cat.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable { viewModel.onCategorySelected(cat.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(cat.parsedColor())
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Action
            DaymarkButton(
                text = if (uiState.taskId == null || uiState.taskId == 0L) "Create Task" else "Save Changes",
                onClick = { viewModel.saveTask() },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.taskId != null && uiState.taskId != 0L && onStartFocus != null) {
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        onStartFocus(uiState.taskId!!, uiState.title)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Focus on this Task",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (uiState.showRecurringEditDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissRecurringEditDialog,
            title = {
                Text(text = "Edit Recurring Task")
            },
            text = {
                Text(text = "Do you want to apply your changes to this task only, or to all tasks in this recurring series?")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.saveTask(RecurringEditMode.ENTIRE_SERIES) }
                ) {
                    Text("Entire series")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.saveTask(RecurringEditMode.THIS_INSTANCE_ONLY) }
                ) {
                    Text("This task only")
                }
            }
        )
    }

    if (isManageCategoriesOpen) {
        val appContainer = (context.applicationContext as com.personal.daymark.DaymarkApplication).container
        val manageCategoriesViewModel: com.personal.daymark.ui.screens.categories.ManageCategoriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            key = "add_edit_manage_categories",
            factory = com.personal.daymark.ui.screens.categories.ManageCategoriesViewModel.provideFactory(appContainer.categoryRepository)
        )
        com.personal.daymark.ui.screens.categories.ManageCategoriesSheet(
            viewModel = manageCategoriesViewModel,
            onDismissRequest = { isManageCategoriesOpen = false }
        )
    }
}
