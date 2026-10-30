package com.personal.daymark.ui.screens.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.personal.daymark.core.timeline.TimeBlockConflictHelper
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.local.entity.TemplateWithItems
import com.personal.daymark.data.local.entity.TimeBlockEntity
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.AddBusyBlockDialog
import com.personal.daymark.ui.components.ApplyTemplateDialog
import com.personal.daymark.ui.components.BusyBlock
import com.personal.daymark.ui.components.DaymarkCard
import com.personal.daymark.ui.components.TimeBlockDialog
import com.personal.daymark.ui.components.TimelineBlock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyTimelineScreen(
    selectedDate: LocalDate,
    tasks: List<TaskWithSubtasksAndCategory>,
    busyBlocks: List<TimeBlockEntity> = emptyList(),
    templates: List<TemplateWithItems> = emptyList(),
    onUpdateTaskTimeline: (taskId: Long, startTime: String, durationMinutes: Int) -> Unit,
    onToggleTaskComplete: (TaskWithSubtasksAndCategory) -> Unit,
    onTaskClick: (TaskWithSubtasksAndCategory) -> Unit,
    onAddBusyBlock: ((title: String, startTime: String, durationMinutes: Int) -> Unit)? = null,
    onDeleteBusyBlock: ((id: Long) -> Unit)? = null,
    onApplyTemplate: ((templateId: Long) -> Unit)? = null,
    onManageTemplates: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onQuickAdd: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val startHour = 6 // 6:00 AM
    val endHour = 22 // 10:00 PM
    val totalHours = endHour - startHour
    val hourHeightDp = 64.dp
    val hourHeightPx = with(density) { hourHeightDp.toPx() }

    // Partition tasks into Scheduled (has start time & duration > 0) vs Unscheduled
    val scheduledTasks = remember(tasks) {
        tasks.filter { it.task.isTimelineScheduled }
            .sortedBy { it.task.getEffectiveStartMinutes() ?: Int.MAX_VALUE }
    }
    val unscheduledTasks = remember(tasks) {
        tasks.filter { !it.task.isTimelineScheduled }
    }

    var isUnscheduledExpanded by remember { mutableStateOf(true) }
    var taskToReschedule by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var showAddBusyBlockDialog by remember { mutableStateOf(false) }
    var showApplyTemplateDialog by remember { mutableStateOf(false) }

    // Drag-and-drop state
    var draggingTaskId by remember { mutableStateOf<Long?>(null) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggingTaskItem by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var timelineAreaTopInWindow by remember { mutableFloatStateOf(0f) }

    // Dialogs
    taskToReschedule?.let { taskItem ->
        TimeBlockDialog(
            taskTitle = taskItem.task.title,
            initialStartTime = taskItem.task.getEffectiveStartTime(),
            initialDuration = if (taskItem.task.durationMinutes > 0) taskItem.task.durationMinutes else 30,
            onDismissRequest = { taskToReschedule = null },
            onConfirmTimeBlock = { startTime, durationMinutes ->
                val startMinutes = TimeBlockConflictHelper.formatMinutesToTimeString(0)
                val parts = startTime.split(":")
                val startMins = if (parts.size >= 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else 0

                val conflict = TimeBlockConflictHelper.findConflictingBusyBlock(startMins, durationMinutes, busyBlocks)
                if (conflict != null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Warning: Overlaps with busy block \"${conflict.title}\"")
                    }
                }

                onUpdateTaskTimeline(taskItem.task.id, startTime, durationMinutes)
                taskToReschedule = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Scheduled at $startTime (${durationMinutes}m)")
                }
            }
        )
    }

    if (showAddBusyBlockDialog && onAddBusyBlock != null) {
        AddBusyBlockDialog(
            onDismissRequest = { showAddBusyBlockDialog = false },
            onConfirmBusyBlock = { title, startTime, duration ->
                onAddBusyBlock(title, startTime, duration)
                showAddBusyBlockDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Added Busy Block \"$title\" ($startTime, ${duration}m)")
                }
            }
        )
    }

    if (showApplyTemplateDialog && onApplyTemplate != null) {
        ApplyTemplateDialog(
            selectedDate = selectedDate,
            templates = templates,
            onDismissRequest = { showApplyTemplateDialog = false },
            onApplyTemplate = { templateId ->
                onApplyTemplate(templateId)
                showApplyTemplateDialog = false
            },
            onManageTemplates = {
                showApplyTemplateDialog = false
                onManageTemplates?.invoke()
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Day Summary & Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${scheduledTasks.size} tasks • ${busyBlocks.size} busy blocks • ${unscheduledTasks.size} unscheduled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onQuickAdd != null) {
                    IconButton(
                        onClick = onQuickAdd,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Quick Actions: "+ Busy Block" and "Templates"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onAddBusyBlock != null) {
                    AssistChip(
                        onClick = { showAddBusyBlockDialog = true },
                        label = { Text("Busy Block", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (onApplyTemplate != null) {
                    AssistChip(
                        onClick = { showApplyTemplateDialog = true },
                        label = { Text("Day Templates", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DashboardCustomize,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.primary,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Unscheduled Tasks Tray (Collapsible)
            if (unscheduledTasks.isNotEmpty()) {
                DaymarkCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isUnscheduledExpanded = !isUnscheduledExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unscheduled Tasks (${unscheduledTasks.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isUnscheduledExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isUnscheduledExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = isUnscheduledExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Tap Schedule or drag onto the timeline to time-block:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    unscheduledTasks.forEach { taskItem ->
                                        val catColor = taskItem.category?.parsedColor() ?: MaterialTheme.colorScheme.primary
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { taskToReschedule = taskItem }
                                                .pointerInput(taskItem.task.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            draggingTaskId = taskItem.task.id
                                                            draggingTaskItem = taskItem
                                                            dragStartY = offset.y
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                        },
                                                        onDragEnd = {
                                                            // Calculate target time block on drop
                                                            val dropPx = dragOffsetY.coerceAtLeast(0f)
                                                            val targetMinutesFromStart = ((dropPx / hourHeightPx) * 60).toInt()
                                                            val rawTotalMinutes = (startHour * 60) + targetMinutesFromStart
                                                            val snappedMinutes = ((rawTotalMinutes + 7) / 15 * 15).coerceIn(startHour * 60, (endHour - 1) * 60)

                                                            // Check for Busy Block conflict
                                                            val conflict = TimeBlockConflictHelper.findConflictingBusyBlock(
                                                                targetStartMinutes = snappedMinutes,
                                                                targetDurationMinutes = 30,
                                                                busyBlocks = busyBlocks
                                                            )

                                                            if (conflict != null) {
                                                                draggingTaskId = null
                                                                draggingTaskItem = null
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("Cannot schedule: overlaps with busy block \"${conflict.title}\" (${conflict.startTime})")
                                                                }
                                                            } else {
                                                                val h = snappedMinutes / 60
                                                                val m = snappedMinutes % 60
                                                                val newStartTime = String.format(Locale.US, "%02d:%02d", h, m)

                                                                onUpdateTaskTimeline(taskItem.task.id, newStartTime, 30)
                                                                draggingTaskId = null
                                                                draggingTaskItem = null
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar("Scheduled \"${taskItem.task.title}\" at $newStartTime (30m)")
                                                                }
                                                            }
                                                        },
                                                        onDragCancel = {
                                                            draggingTaskId = null
                                                            draggingTaskItem = null
                                                        }
                                                    )
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = taskItem.task.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Outlined.Schedule,
                                                    contentDescription = "Schedule",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Vertical Timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeightDp * totalHours)
                    .onGloballyPositioned { coordinates ->
                        timelineAreaTopInWindow = coordinates.parentLayoutCoordinates?.size?.height?.toFloat() ?: 0f
                    }
            ) {
                // Background Hour Grid Lines & Labels
                for (hour in startHour..endHour) {
                    val hourOffset: Dp = hourHeightDp * (hour - startHour)

                    // Hour label + guideline row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = hourOffset),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayHour = when {
                            hour == 0 || hour == 24 -> "12 AM"
                            hour < 12 -> "$hour AM"
                            hour == 12 -> "12 PM"
                            else -> "${hour - 12} PM"
                        }
                        Text(
                            text = displayHour,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .width(54.dp)
                                .padding(start = 12.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                    }

                    // Half-hour subtle dotted guide line
                    if (hour < endHour) {
                        val halfHourOffset: Dp = hourOffset + (hourHeightDp / 2)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = halfHourOffset),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(54.dp))
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                // Current Time Red Indicator (if selected date is today)
                if (selectedDate == LocalDate.now()) {
                    val now = LocalTime.now()
                    val currentMinutes = now.hour * 60 + now.minute
                    if (currentMinutes in (startHour * 60)..(endHour * 60)) {
                        val minutesFromStart = currentMinutes - (startHour * 60)
                        val indicatorOffset: Dp = hourHeightDp * (minutesFromStart.toFloat() / 60f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = indicatorOffset)
                                .zIndex(5f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 48.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.error,
                                thickness = 2.dp
                            )
                        }
                    }
                }

                // Render Busy Blocks (Protected Time in neutral/muted color)
                busyBlocks.forEach { block ->
                    val blockStartMinutes = block.getStartMinutes()
                    val minutesFromTimelineStart = (blockStartMinutes - (startHour * 60)).coerceAtLeast(0)
                    val baseTopDp: Dp = hourHeightDp * (minutesFromTimelineStart.toFloat() / 60f)
                    val rawBlockHeightDp: Dp = hourHeightDp * (block.durationMinutes.toFloat() / 60f)
                    val blockHeightDp: Dp = rawBlockHeightDp.coerceAtLeast(30.dp)

                    Box(
                        modifier = Modifier
                            .offset(y = baseTopDp)
                            .padding(start = 58.dp, end = 12.dp)
                            .fillMaxWidth()
                            .zIndex(2f)
                    ) {
                        BusyBlock(
                            busyBlock = block,
                            height = blockHeightDp,
                            onDeleteClick = if (onDeleteBusyBlock != null) {
                                { onDeleteBusyBlock(block.id) }
                            } else null
                        )
                    }
                }

                // Render Scheduled Task Blocks
                scheduledTasks.forEach { taskItem ->
                    val task = taskItem.task
                    val startMinutes = task.getEffectiveStartMinutes() ?: (startHour * 60)
                    val duration = if (task.durationMinutes > 0) task.durationMinutes else 30

                    val minutesFromTimelineStart = (startMinutes - (startHour * 60)).coerceAtLeast(0)
                    val baseTopDp: Dp = hourHeightDp * (minutesFromTimelineStart.toFloat() / 60f)
                    val rawBlockHeightDp: Dp = hourHeightDp * (duration.toFloat() / 60f)
                    val blockHeightDp: Dp = rawBlockHeightDp.coerceAtLeast(32.dp)

                    val isDraggingThis = draggingTaskId == task.id
                    val effectiveTopDp: Dp = if (isDraggingThis) {
                        baseTopDp + with(density) { dragOffsetY.toDp() }
                    } else {
                        baseTopDp
                    }

                    // Check live conflict if dragging
                    val deltaMinutes = ((dragOffsetY / hourHeightPx) * 60).toInt()
                    val currentSnappedMinutes = ((startMinutes + deltaMinutes + 7) / 15 * 15).coerceIn(startHour * 60, (endHour - 1) * 60)
                    val isConflictingWithBusy = isDraggingThis && TimeBlockConflictHelper.findConflictingBusyBlock(
                        targetStartMinutes = currentSnappedMinutes,
                        targetDurationMinutes = duration,
                        busyBlocks = busyBlocks
                    ) != null

                    Box(
                        modifier = Modifier
                            .offset(y = effectiveTopDp)
                            .padding(start = 58.dp, end = 12.dp)
                            .fillMaxWidth()
                            .zIndex(if (isDraggingThis) 10f else 3f)
                            .then(
                                if (isConflictingWithBusy) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                                } else Modifier
                            )
                            .pointerInput(task.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingTaskId = task.id
                                        draggingTaskItem = taskItem
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        // Calculate target start time from the drop offset
                                        val baseMinutes = startMinutes
                                        val deltaMins = ((dragOffsetY / hourHeightPx) * 60).toInt()
                                        val rawTargetMinutes = baseMinutes + deltaMins
                                        val snappedMinutes = ((rawTargetMinutes + 7) / 15 * 15).coerceIn(startHour * 60, (endHour - 1) * 60)

                                        // Conflict check: Busy Block protects time
                                        val conflict = TimeBlockConflictHelper.findConflictingBusyBlock(
                                            targetStartMinutes = snappedMinutes,
                                            targetDurationMinutes = duration,
                                            busyBlocks = busyBlocks
                                        )

                                        if (conflict != null) {
                                            // Prevent drop!
                                            draggingTaskId = null
                                            draggingTaskItem = null
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Cannot schedule: overlaps with busy block \"${conflict.title}\" (${conflict.startTime})")
                                            }
                                        } else {
                                            val h = snappedMinutes / 60
                                            val m = snappedMinutes % 60
                                            val newStartTime = String.format(Locale.US, "%02d:%02d", h, m)

                                            onUpdateTaskTimeline(task.id, newStartTime, duration)
                                            draggingTaskId = null
                                            draggingTaskItem = null
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Moved to $newStartTime (${duration}m)")
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        draggingTaskId = null
                                        draggingTaskItem = null
                                    }
                                )
                            }
                    ) {
                        TimelineBlock(
                            taskItem = taskItem,
                            height = blockHeightDp,
                            isDragging = isDraggingThis,
                            onToggleComplete = { onToggleTaskComplete(taskItem) },
                            onClick = { onTaskClick(taskItem) },
                            onRescheduleClick = { taskToReschedule = taskItem }
                        )
                    }
                }

                // Floating Drag Preview Pill showing snapped time & conflict warning during drag
                if (draggingTaskId != null && draggingTaskItem != null) {
                    val task = draggingTaskItem!!.task
                    val currentStartMinutes = task.getEffectiveStartMinutes() ?: (startHour * 60)
                    val taskDuration = if (task.durationMinutes > 0) task.durationMinutes else 30
                    val deltaMinutes = ((dragOffsetY / hourHeightPx) * 60).toInt()
                    val targetMinutes = ((currentStartMinutes + deltaMinutes + 7) / 15 * 15)
                        .coerceIn(startHour * 60, (endHour - 1) * 60)

                    val previewHour = targetMinutes / 60
                    val previewMinute = targetMinutes % 60
                    val previewTimeString = String.format(Locale.US, "%02d:%02d", previewHour, previewMinute)

                    val conflictingBlock = TimeBlockConflictHelper.findConflictingBusyBlock(
                        targetStartMinutes = targetMinutes,
                        targetDurationMinutes = taskDuration,
                        busyBlocks = busyBlocks
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .zIndex(20f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (conflictingBlock != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            if (conflictingBlock != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (conflictingBlock != null) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Conflicts with busy block: \"${conflictingBlock.title}\" ($previewTimeString)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                Text(
                                    text = "Release to schedule at $previewTimeString",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
