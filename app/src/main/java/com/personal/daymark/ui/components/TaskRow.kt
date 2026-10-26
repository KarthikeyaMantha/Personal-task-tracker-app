package com.personal.daymark.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.personal.daymark.ui.theme.DaymarkTheme
import com.personal.daymark.ui.theme.PriorityHigh
import com.personal.daymark.ui.theme.PriorityLow
import com.personal.daymark.ui.theme.PriorityMedium
import com.personal.daymark.ui.theme.PriorityUrgent

fun getPriorityColor(priority: Int): Color? {
    return when (priority) {
        1 -> PriorityLow
        2 -> PriorityMedium
        3 -> PriorityHigh
        4 -> PriorityUrgent
        else -> null
    }
}

/**
 * Reusable TaskRow component with completion toggle, title, due date, category, and priority indicator.
 */
@Composable
fun TaskRow(
    title: String,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    categoryColor: Color? = null,
    dueDateText: String? = null,
    recurrenceText: String? = null,
    subtasksCompleted: Int = 0,
    subtasksTotal: Int = 0,
    priorityColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val checkBackground by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "checkBackground"
    )

    val checkBorderColor by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "checkBorder"
    )

    DaymarkCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile circular completion toggle with WCAG 48x48dp minimum touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        onClick = onToggleComplete,
                        role = androidx.compose.ui.semantics.Role.Checkbox
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(checkBackground)
                        .border(1.5.dp, checkBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark task incomplete",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (categoryName != null || dueDateText != null || recurrenceText != null || subtasksTotal > 0) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (categoryName != null) {
                            val badgeColor = categoryColor ?: MaterialTheme.colorScheme.secondary
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                        }

                        if (dueDateText != null) {
                            Text(
                                text = dueDateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (recurrenceText != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Recurring",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = recurrenceText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (subtasksTotal > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "$subtasksCompleted/$subtasksTotal",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (subtasksCompleted == subtasksTotal) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { if (subtasksTotal > 0) subtasksCompleted.toFloat() / subtasksTotal else 0f },
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (subtasksCompleted == subtasksTotal) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                }
            }

            // Priority Indicator Dot
            if (priorityColor != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
            }
        }
    }
}

/**
 * Swipeable wrapper for TaskRow supporting swipe-to-complete (left-to-right)
 * and swipe-to-delete (right-to-left).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskRow(
    title: String,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    categoryColor: Color? = null,
    dueDateText: String? = null,
    recurrenceText: String? = null,
    subtasksCompleted: Int = 0,
    subtasksTotal: Int = 0,
    priorityColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleComplete()
                    false // Snap back after triggering toggle
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true // Dismiss and trigger delete with undo
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Toggle Complete",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isCompleted) "Reopen" else "Complete",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    SwipeToDismissBoxValue.Settled -> {}
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        TaskRow(
            title = title,
            isCompleted = isCompleted,
            onToggleComplete = onToggleComplete,
            categoryName = categoryName,
            categoryColor = categoryColor,
            dueDateText = dueDateText,
            recurrenceText = recurrenceText,
            subtasksCompleted = subtasksCompleted,
            subtasksTotal = subtasksTotal,
            priorityColor = priorityColor,
            onClick = onClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskRowPreview() {
    DaymarkTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TaskRow(
                title = "Review Q3 product architecture and blueprint",
                isCompleted = false,
                onToggleComplete = {},
                categoryName = "Work",
                dueDateText = "Today, 4:00 PM",
                priorityColor = PriorityHigh
            )
            Spacer(modifier = Modifier.size(10.dp))
            TaskRow(
                title = "Daily 20-minute meditation session",
                isCompleted = true,
                onToggleComplete = {},
                categoryName = "Health",
                dueDateText = "Completed"
            )
        }
    }
}
