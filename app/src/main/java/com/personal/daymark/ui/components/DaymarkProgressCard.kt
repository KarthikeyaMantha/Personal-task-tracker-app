package com.personal.daymark.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.personal.daymark.ui.theme.DaymarkTheme

/**
 * Dual-Ring "Daily Rhythm" Progress Card for the Today dashboard.
 * Visualizes both Tasks (Outer Ring) and Habits (Inner Ring) with animated Canvas arcs
 * and calm Material 3 typography.
 */
@Composable
fun DaymarkProgressCard(
    completedTasksCount: Int,
    totalTasksCount: Int,
    taskProgressRatio: Float,
    completedHabitsCount: Int,
    totalHabitsCount: Int,
    habitProgressRatio: Float,
    modifier: Modifier = Modifier,
    onHabitsClick: (() -> Unit)? = null
) {
    val totalCommitments = totalTasksCount + totalHabitsCount
    val completedCommitments = completedTasksCount + completedHabitsCount

    val combinedRatio = if (totalCommitments > 0) {
        (completedCommitments.toFloat() / totalCommitments.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedTaskProgress by animateFloatAsState(
        targetValue = taskProgressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "taskProgressAnimation"
    )

    val animatedHabitProgress by animateFloatAsState(
        targetValue = habitProgressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "habitProgressAnimation"
    )

    val animatedCombinedProgress by animateFloatAsState(
        targetValue = combinedRatio,
        animationSpec = tween(durationMillis = 650),
        label = "combinedProgressAnimation"
    )

    val combinedPercentage = (animatedCombinedProgress * 100).toInt().coerceIn(0, 100)
    val isAllComplete = totalCommitments > 0 && completedCommitments >= totalCommitments

    val supportiveText = when {
        totalCommitments == 0 -> "All clear for today"
        isAllComplete -> "All tasks and habits finished"
        combinedPercentage >= 50 -> "Making steady progress"
        combinedPercentage > 0 -> "Making steady progress"
        else -> "Ready to begin today's focus"
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val primaryTrackColor = primaryColor.copy(alpha = 0.16f)
    val secondaryTrackColor = secondaryColor.copy(alpha = 0.16f)

    DaymarkCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dual Rings Canvas (90dp diameter)
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    val outerStrokeWidth = 6.5.dp.toPx()
                    val innerStrokeWidth = 5.dp.toPx()
                    val ringGap = 4.dp.toPx()

                    // Outer Ring (Tasks)
                    val outerInset = outerStrokeWidth / 2f
                    val outerDiameter = size.width - outerStrokeWidth
                    val outerTopLeft = Offset(outerInset, outerInset)
                    val outerArcSize = Size(outerDiameter, outerDiameter)

                    // Outer background track
                    drawArc(
                        color = primaryTrackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerArcSize,
                        style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
                    )

                    // Outer progress arc
                    if (animatedTaskProgress > 0.005f) {
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = (animatedTaskProgress * 360f).coerceIn(0f, 360f),
                            useCenter = false,
                            topLeft = outerTopLeft,
                            size = outerArcSize,
                            style = Stroke(width = outerStrokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Inner Ring (Habits)
                    val innerInset = outerStrokeWidth + ringGap + (innerStrokeWidth / 2f)
                    val innerDiameter = size.width - (innerInset * 2f)
                    val innerTopLeft = Offset(innerInset, innerInset)
                    val innerArcSize = Size(innerDiameter, innerDiameter)

                    // Inner background track
                    drawArc(
                        color = secondaryTrackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerArcSize,
                        style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
                    )

                    // Inner progress arc
                    if (animatedHabitProgress > 0.005f) {
                        drawArc(
                            color = secondaryColor,
                            startAngle = -90f,
                            sweepAngle = (animatedHabitProgress * 360f).coerceIn(0f, 360f),
                            useCenter = false,
                            topLeft = innerTopLeft,
                            size = innerArcSize,
                            style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center indicator: Checkmark when 100% complete, otherwise percentage
                if (isAllComplete) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "All commitments complete",
                        tint = primaryColor,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(
                        text = "$combinedPercentage%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Daily Rhythm",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = supportiveText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tasks breakdown row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val taskPercentage = if (totalTasksCount > 0) {
                        ((completedTasksCount.toFloat() / totalTasksCount.toFloat()) * 100).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    Text(
                        text = "$completedTasksCount / $totalTasksCount ($taskPercentage%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Habits breakdown row
                val habitRowModifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onHabitsClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(
                                    role = Role.Button,
                                    onClick = onHabitsClick
                                )
                                .padding(vertical = 2.dp)
                        } else {
                            Modifier.padding(vertical = 2.dp)
                        }
                    )

                Row(
                    modifier = habitRowModifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(secondaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val habitDetailText = if (totalHabitsCount == 0) {
                        "None scheduled"
                    } else {
                        val habitPercentage = ((completedHabitsCount.toFloat() / totalHabitsCount.toFloat()) * 100).toInt().coerceIn(0, 100)
                        "$completedHabitsCount / $totalHabitsCount ($habitPercentage%)"
                    }
                    Text(
                        text = habitDetailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onHabitsClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Habits",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backwards-compatible overload for existing callers of DaymarkProgressCard.
 */
@Composable
fun DaymarkProgressCard(
    completedCount: Int,
    totalCount: Int,
    progressRatio: Float,
    modifier: Modifier = Modifier,
    useCircular: Boolean = false
) {
    DaymarkProgressCard(
        completedTasksCount = completedCount,
        totalTasksCount = totalCount,
        taskProgressRatio = progressRatio,
        completedHabitsCount = 0,
        totalHabitsCount = 0,
        habitProgressRatio = 0f,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DaymarkProgressCardPreview() {
    DaymarkTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DaymarkProgressCard(
                completedTasksCount = 3,
                totalTasksCount = 4,
                taskProgressRatio = 0.75f,
                completedHabitsCount = 1,
                totalHabitsCount = 2,
                habitProgressRatio = 0.5f
            )
            Spacer(modifier = Modifier.height(16.dp))
            DaymarkProgressCard(
                completedTasksCount = 4,
                totalTasksCount = 4,
                taskProgressRatio = 1f,
                completedHabitsCount = 2,
                totalHabitsCount = 2,
                habitProgressRatio = 1f
            )
            Spacer(modifier = Modifier.height(16.dp))
            DaymarkProgressCard(
                completedTasksCount = 0,
                totalTasksCount = 0,
                taskProgressRatio = 0f,
                completedHabitsCount = 0,
                totalHabitsCount = 0,
                habitProgressRatio = 0f
            )
        }
    }
}
