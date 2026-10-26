package com.personal.daymark.ui.screens.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.daymark.core.habits.HabitDayHistory
import com.personal.daymark.core.habits.HabitDayStatus
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.DaymarkCard
import com.personal.daymark.ui.components.DaymarkTextField
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitCard(
    habitItem: HabitItemUiState,
    onToggleToday: () -> Unit,
    onToggleDate: (LocalDate) -> Unit,
    onEditHabit: () -> Unit,
    onDeleteHabit: () -> Unit,
    modifier: Modifier = Modifier,
    onIncrementWater: (() -> Unit)? = null,
    onLogHealthMetric: ((Float, String?) -> Unit)? = null
) {
    val habit = habitItem.habit
    val habitColor = habit.parsedColor()
    var isMenuExpanded by remember { mutableStateOf(false) }

    DaymarkCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Habit Name, Category, Streak, Menu, and Today's Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large One-Tap Checkbox for Today
                val checkboxBgColor by animateColorAsState(
                    targetValue = if (habitItem.isCompletedToday) habitColor else Color.Transparent,
                    label = "checkboxBg"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(checkboxBgColor)
                        .border(
                            width = 2.dp,
                            color = if (habitItem.isCompletedToday) habitColor else habitColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable(
                            onClick = onToggleToday,
                            role = androidx.compose.ui.semantics.Role.Checkbox
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (habitItem.isCompletedToday) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark ${habit.name} incomplete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Note
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (habit.healthMetricType != null) {
                            val healthIcon = when (habit.healthMetricType) {
                                "water" -> Icons.Default.WaterDrop
                                "sleep" -> Icons.Default.Bedtime
                                "mood" -> Icons.Default.Favorite
                                else -> Icons.Default.Spa
                            }
                            Icon(
                                imageVector = healthIcon,
                                contentDescription = "Health Habit",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!habit.note.isNullOrBlank()) {
                        Text(
                            text = habit.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Metadata Badges (Category & Streak)
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (habitItem.category != null) {
                            val catColor = habitItem.category.parsedColor()
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = catColor.copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = habitItem.category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Streak Badge
                        Text(
                            text = "🔥 ${habitItem.currentStreak} streak",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (habitItem.currentStreak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 7d consistency
                        Text(
                            text = "7d: ${habitItem.completionsLast7Days}/7",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Options Menu
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Habit Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                isMenuExpanded = false
                                onEditHabit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Habit", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteHabit()
                            }
                        )
                    }
                }
            }

            // Health Metric Check-In UI
            if (habit.healthMetricType != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HealthMetricControls(
                    habitItem = habitItem,
                    onIncrementWater = onIncrementWater,
                    onLogHealthMetric = onLogHealthMetric
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 14-Day Calendar History View (Completed, Missed, Not Due)
            Text(
                text = "Recent 14-Day History",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                habitItem.historyDays.forEach { dayHistory ->
                    HabitDayCell(
                        dayHistory = dayHistory,
                        habitColor = habitColor,
                        onToggle = { onToggleDate(dayHistory.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitDayCell(
    dayHistory: HabitDayHistory,
    habitColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayInitial = dayHistory.date.dayOfWeek.name.take(1)
    val dayOfMonth = dayHistory.date.dayOfMonth.toString()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Text(
            text = dayInitial,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (dayHistory.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (dayHistory.isToday) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Visual Indicator representing COMPLETED, MISSED, NOT_SCHEDULED, or NOT_YET_DUE
        when (dayHistory.status) {
            HabitDayStatus.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(habitColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            HabitDayStatus.MISSED -> {
                // Missed day: visibly marked with an outline and subtle indicator (never erased!)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Missed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            HabitDayStatus.NOT_SCHEDULED -> {
                // Not scheduled: faint subtle dot
                Box(
                    modifier = Modifier
                        .size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    )
                }
            }
            HabitDayStatus.NOT_YET_DUE -> {
                // Not yet due (today before completion): subtle dashed/unfilled circle
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(
                            width = 1.dp,
                            color = if (dayHistory.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = dayOfMonth,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (dayHistory.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = if (dayHistory.isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun HealthMetricControls(
    habitItem: HabitItemUiState,
    onIncrementWater: (() -> Unit)?,
    onLogHealthMetric: ((Float, String?) -> Unit)?
) {
    val habit = habitItem.habit
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (habit.healthMetricType?.lowercase()) {
                "water" -> {
                    val currentGlasses = (habitItem.todayValue ?: 0f).toInt()
                    val targetGlasses = habit.targetValue ?: 8
                    val progress = if (targetGlasses > 0) (currentGlasses.toFloat() / targetGlasses).coerceIn(0f, 1f) else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Water Intake Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currentGlasses / $targetGlasses ${habit.unit ?: "glasses"}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { onIncrementWater?.invoke() },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val unitSingular = habit.unit?.removeSuffix("es")?.removeSuffix("s") ?: "glass"
                            Text("+1 $unitSingular", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                "sleep" -> {
                    var sleepValue by remember(habitItem.todayValue) {
                        mutableFloatStateOf(habitItem.todayValue ?: 7.5f)
                    }
                    val targetHours = habit.targetValue ?: 8

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sleep Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", sleepValue)} / $targetHours ${habit.unit ?: "hours"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = sleepValue,
                        onValueChange = { sleepValue = it },
                        onValueChangeFinished = {
                            onLogHealthMetric?.invoke(sleepValue, null)
                        },
                        valueRange = 0f..14f,
                        steps = 27
                    )
                }

                "mood" -> {
                    val currentMood = habitItem.todayValue?.toInt()
                    val emojis = listOf(
                        1 to "😞",
                        2 to "😐",
                        3 to "🙂",
                        4 to "😊",
                        5 to "😄"
                    )
                    var noteText by remember(habitItem.todayNote) {
                        mutableStateOf(habitItem.todayNote ?: "")
                    }
                    var showNoteField by remember { mutableStateOf(!habitItem.todayNote.isNullOrBlank()) }

                    Text(
                        text = "Daily Mood Check-in",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEach { (rating, emoji) ->
                            val isSelected = currentMood == rating
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onLogHealthMetric?.invoke(rating.toFloat(), noteText.ifBlank { null })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (!showNoteField) {
                        Text(
                            text = if (habitItem.todayNote.isNullOrBlank()) "+ Add note" else habitItem.todayNote ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showNoteField = true }
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        DaymarkTextField(
                            value = noteText,
                            onValueChange = {
                                noteText = it
                                if (currentMood != null) {
                                    onLogHealthMetric?.invoke(currentMood.toFloat(), it.ifBlank { null })
                                }
                            },
                            placeholder = "Optional note (e.g. feeling calm)...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

