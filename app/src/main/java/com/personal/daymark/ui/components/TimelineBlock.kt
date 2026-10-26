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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.local.entity.parsedColor

@Composable
fun TimelineBlock(
    taskItem: TaskWithSubtasksAndCategory,
    height: Dp,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onToggleComplete: () -> Unit = {},
    onClick: () -> Unit = {},
    onRescheduleClick: (() -> Unit)? = null
) {
    val task = taskItem.task
    val category = taskItem.category
    val accentColor = category?.parsedColor() ?: MaterialTheme.colorScheme.primary

    val checkBackground by animateColorAsState(
        targetValue = if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "timelineCheckBg"
    )

    val startTime = task.getEffectiveStartTime() ?: "09:00"
    val duration = if (task.durationMinutes > 0) task.durationMinutes else 30

    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .shadow(if (isDragging) 8.dp else 0.dp, shape)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (isDragging) {
            accentColor.copy(alpha = 0.35f)
        } else {
            accentColor.copy(alpha = 0.15f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isDragging) 2.dp else 1.dp,
            color = if (isDragging) accentColor else accentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent color strip on the left edge
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (height > 36.dp) 24.dp else 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox for completion
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(checkBackground)
                    .border(1.5.dp, if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title & Time info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = if (height > 50.dp) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (height >= 40.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$startTime • ${duration}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        category?.name?.let { catName ->
                            Text(
                                text = "• $catName",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            // Drag handle or reschedule action
            if (onRescheduleClick != null) {
                IconButton(
                    onClick = onRescheduleClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Reschedule time block",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag handle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
