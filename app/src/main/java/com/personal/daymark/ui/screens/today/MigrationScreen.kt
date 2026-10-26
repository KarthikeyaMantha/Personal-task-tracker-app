package com.personal.daymark.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.getPriorityColor
import kotlin.math.roundToInt

/**
 * Smart Visual Migration Screen for the End of Day Shutdown Ritual.
 * Layout:
 * - Left column: Unfinished "Today" tasks.
 * - Right column: Next 3 to 5 future day targets + "Leave in Inbox" target.
 * Supports visual drag-and-drop and robust tap-to-assign gestures.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MigrationScreen(
    viewModel: EndOfDayViewModel,
    onDismiss: () -> Unit,
    onShutdownCompleted: (message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Drag-and-drop tracking state
    var draggingTaskId by remember { mutableStateOf<Long?>(null) }
    var draggingTaskItem by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Target drop bounds tracking in window
    val targetBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var hoveredTargetKey by remember { mutableStateOf<String?>(null) }

    // Tap-to-assign active task
    var selectedTaskForAssignment by remember { mutableStateOf<TaskWithSubtasksAndCategory?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Smart Visual Migration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Clear your desk. Assign leftover tasks to future days.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            if (uiState.incompleteTasks.isEmpty()) {
                // All clear celebration!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All Clear for Today!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You've completed or rescheduled all your tasks. Time to unplug and relax.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.confirmShutdown {
                                onShutdownCompleted("Day closed. Have a restful evening!")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Complete Shutdown")
                    }
                }
            } else {
                // Two Column Layout: Left = Incomplete Tasks, Right = Future Day Targets + Inbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT COLUMN: Unfinished Tasks
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = "Today's Tasks (${uiState.incompleteTasks.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.incompleteTasks,
                                key = { it.task.id }
                            ) { taskItem ->
                                val task = taskItem.task
                                val choice = uiState.taskDestinations[task.id]
                                val isDraggingThis = draggingTaskId == task.id

                                val destinationLabel = when (choice) {
                                    is MigrationChoice.SpecificDate -> choice.label
                                    MigrationChoice.Inbox -> "Inbox"
                                    MigrationChoice.Completed -> "Done"
                                    MigrationChoice.KeepToday -> "Today"
                                    null -> "Tomorrow"
                                }

                                val isSelected = selectedTaskForAssignment?.task?.id == task.id

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDraggingThis) 10f else 1f)
                                        .offset {
                                            if (isDraggingThis) {
                                                IntOffset(
                                                    dragOffsetX.roundToInt(),
                                                    dragOffsetY.roundToInt()
                                                )
                                            } else {
                                                IntOffset.Zero
                                            }
                                        }
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .pointerInput(task.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingTaskId = task.id
                                                    draggingTaskItem = taskItem
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetX += dragAmount.x
                                                    dragOffsetY += dragAmount.y

                                                    // Calculate hit target
                                                    val currentCenterY = change.position.y
                                                    // Hover check among registered targets
                                                },
                                                onDragEnd = {
                                                    // Drop onto hovered target or check bounding boxes
                                                    hoveredTargetKey?.let { targetKey ->
                                                        if (targetKey == "INBOX") {
                                                            viewModel.assignTaskToInbox(task.id)
                                                        } else {
                                                            uiState.targetDays.find { it.label == targetKey }?.let { targetDay ->
                                                                viewModel.assignTaskToTargetDate(task.id, targetDay)
                                                            }
                                                        }
                                                    }

                                                    draggingTaskId = null
                                                    draggingTaskItem = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                    hoveredTargetKey = null
                                                },
                                                onDragCancel = {
                                                    draggingTaskId = null
                                                    draggingTaskItem = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                    hoveredTargetKey = null
                                                }
                                            )
                                        }
                                        .clickable {
                                            selectedTaskForAssignment = if (isSelected) null else taskItem
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDraggingThis) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    shadowElevation = if (isDraggingThis) 8.dp else 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val priorityColor = getPriorityColor(task.priority) ?: MaterialTheme.colorScheme.primary
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(priorityColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Assigned target pill
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (choice == MigrationChoice.Inbox) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (choice == MigrationChoice.Inbox) Icons.Outlined.Inbox else Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(10.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = destinationLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }

                                            // Quick button to send directly to Inbox
                                            if (choice != MigrationChoice.Inbox) {
                                                Text(
                                                    text = "Inbox",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable { viewModel.assignTaskToInbox(task.id) }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Migration Targets (Next 4 Days + Inbox)
                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = "Migrate To (Tap or Drop)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Target Days
                            items(uiState.targetDays, key = { it.date.toString() }) { targetDay ->
                                val assignedTasks = uiState.incompleteTasks.filter {
                                    val choice = uiState.taskDestinations[it.task.id]
                                    choice is MigrationChoice.SpecificDate && choice.dateMillis == targetDay.dateMillis
                                }
                                val isHovered = hoveredTargetKey == targetDay.label

                                TargetDayCard(
                                    targetDay = targetDay,
                                    assignedTasks = assignedTasks,
                                    isHovered = isHovered,
                                    onSelectTarget = {
                                        selectedTaskForAssignment?.let { activeTask ->
                                            viewModel.assignTaskToTargetDate(activeTask.task.id, targetDay)
                                            selectedTaskForAssignment = null
                                        }
                                    },
                                    onPositioned = { bounds ->
                                        targetBoundsMap[targetDay.label] = bounds
                                    }
                                )
                            }

                            // Target: Leave in Inbox
                            item {
                                val inboxTasks = uiState.incompleteTasks.filter {
                                    uiState.taskDestinations[it.task.id] is MigrationChoice.Inbox
                                }
                                val isHovered = hoveredTargetKey == "INBOX"

                                TargetInboxCard(
                                    assignedTasks = inboxTasks,
                                    isHovered = isHovered,
                                    onSelectTarget = {
                                        selectedTaskForAssignment?.let { activeTask ->
                                            viewModel.assignTaskToInbox(activeTask.task.id)
                                            selectedTaskForAssignment = null
                                        }
                                    },
                                    onPositioned = { bounds ->
                                        targetBoundsMap["INBOX"] = bounds
                                    }
                                )
                            }
                        }
                    }
                }

                // Selected Task Quick Assignment Bar (when a left-column task is tapped)
                AnimatedVisibility(visible = selectedTaskForAssignment != null) {
                    selectedTaskForAssignment?.let { activeTask ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Assign \"${activeTask.task.title}\" to:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    uiState.targetDays.forEach { target ->
                                        Button(
                                            onClick = {
                                                viewModel.assignTaskToTargetDate(activeTask.task.id, target)
                                                selectedTaskForAssignment = null
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(target.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.assignTaskToInbox(activeTask.task.id)
                                            selectedTaskForAssignment = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Inbox", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.leaveAllInInbox() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All to Inbox", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            viewModel.confirmShutdown {
                                onShutdownCompleted("Day closed. Have a great evening!")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Close Day", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetDayCard(
    targetDay: MigrationDayTarget,
    assignedTasks: List<TaskWithSubtasksAndCategory>,
    isHovered: Boolean,
    onSelectTarget: () -> Unit,
    onPositioned: (Rect) -> Unit
) {
    val borderColor by animateColorAsState(
        if (isHovered) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "targetBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isHovered) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onSelectTarget)
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInWindow())
            },
        shape = RoundedCornerShape(10.dp),
        color = if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = targetDay.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${assignedTasks.size} task(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (assignedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    assignedTasks.forEach { taskItem ->
                        Text(
                            text = "• ${taskItem.task.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetInboxCard(
    assignedTasks: List<TaskWithSubtasksAndCategory>,
    isHovered: Boolean,
    onSelectTarget: () -> Unit,
    onPositioned: (Rect) -> Unit
) {
    val borderColor by animateColorAsState(
        if (isHovered) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "inboxBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isHovered) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onSelectTarget)
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInWindow())
            },
        shape = RoundedCornerShape(10.dp),
        color = if (isHovered) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Leave in Inbox",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${assignedTasks.size} task(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Strip due date, plan later",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )

            if (assignedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    assignedTasks.forEach { taskItem ->
                        Text(
                            text = "• ${taskItem.task.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
