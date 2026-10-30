package com.personal.daymark.ui.screens.habits

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.DaymarkButton
import com.personal.daymark.ui.components.DaymarkTextField

private val HABIT_COLORS = listOf(
    "#18181B", // Zinc / Charcoal
    "#1B4332", // Forest Green
    "#2563EB", // Royal Blue
    "#D97706", // Amber
    "#DC2626", // Crimson
    "#7C3AED", // Violet
    "#059669", // Emerald
    "#4B5563"  // Muted Gray
)

private val WEEKDAY_LABELS = listOf(
    "Mon" to 1,
    "Tue" to 2,
    "Wed" to 4,
    "Thu" to 8,
    "Fri" to 16,
    "Sat" to 32,
    "Sun" to 64
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitSheet(
    habitToEdit: HabitEntity?,
    categories: List<CategoryEntity>,
    onDismissRequest: () -> Unit,
    onSaveHabit: (
        id: Long,
        name: String,
        note: String?,
        frequency: String,
        targetWeekdays: Int,
        colorHex: String,
        categoryId: Long?,
        healthMetricType: String?,
        targetValue: Int?,
        unit: String?,
        reminderTime: Int?,
        targetDurationMinutes: Int?,
        routineTimeOfDay: String,
        isFrozen: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var name by remember(habitToEdit) { mutableStateOf(habitToEdit?.name ?: "") }
    var note by remember(habitToEdit) { mutableStateOf(habitToEdit?.note ?: "") }
    var frequency by remember(habitToEdit) { mutableStateOf(habitToEdit?.frequency ?: "DAILY") }
    var targetWeekdays by remember(habitToEdit) { mutableIntStateOf(habitToEdit?.targetWeekdays ?: 127) }
    var selectedColorHex by remember(habitToEdit) { mutableStateOf(habitToEdit?.colorHex ?: HABIT_COLORS.first()) }
    var selectedCategoryId by remember(habitToEdit) { mutableStateOf(habitToEdit?.categoryId) }
    var healthMetricType by remember(habitToEdit) { mutableStateOf(habitToEdit?.healthMetricType) }
    var targetValue by remember(habitToEdit) { mutableIntStateOf(habitToEdit?.targetValue ?: 8) }
    var unit by remember(habitToEdit) { mutableStateOf(habitToEdit?.unit ?: "glasses") }
    var reminderTime by remember(habitToEdit) { mutableStateOf(habitToEdit?.reminderTime) }
    var targetDurationMinutes by remember(habitToEdit) { mutableStateOf(habitToEdit?.targetDurationMinutes) }
    var routineTimeOfDay by remember(habitToEdit) { mutableStateOf(habitToEdit?.routineTimeOfDay ?: "ANYTIME") }
    var isFrozen by remember(habitToEdit) { mutableStateOf(habitToEdit?.isFrozen ?: false) }
    var isNameError by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    val handleSave = {
        if (name.isBlank()) {
            isNameError = true
        } else {
            onSaveHabit(
                habitToEdit?.id ?: 0L,
                name.trim(),
                note.trim().takeIf { it.isNotEmpty() },
                frequency,
                targetWeekdays,
                selectedColorHex,
                selectedCategoryId,
                healthMetricType,
                targetValue.takeIf { healthMetricType != null },
                unit.takeIf { healthMetricType != null },
                reminderTime,
                targetDurationMinutes,
                routineTimeOfDay,
                isFrozen
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (habitToEdit == null) "New Habit" else "Edit Habit",
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

            // Habit Name Input (Fast entry field)
            DaymarkTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (isNameError && it.isNotBlank()) {
                        isNameError = false
                    }
                },
                placeholder = "What habit do you want to build?",
                isError = isNameError,
                errorMessage = if (isNameError) "Habit name is required" else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { handleSave() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Repeat Selector
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
                val isEveryDay = frequency == "DAILY" && targetWeekdays == 127
                val isWeekdays = frequency == "SPECIFIC_DAYS" && targetWeekdays == 31
                val isCustomDays = frequency == "SPECIFIC_DAYS" && targetWeekdays != 31

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isEveryDay) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable {
                            frequency = "DAILY"
                            targetWeekdays = 127
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Every day",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isEveryDay) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isWeekdays) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable {
                            frequency = "SPECIFIC_DAYS"
                            targetWeekdays = 31 // Mon-Fri
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Weekdays (Mon–Fri)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isWeekdays) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCustomDays) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable {
                            frequency = "SPECIFIC_DAYS"
                            if (targetWeekdays == 127 || targetWeekdays == 31) {
                                targetWeekdays = 21 // Mon, Wed, Fri
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Selected days",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCustomDays) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Target Weekdays Selector (shown when specific days active)
            if (frequency == "SPECIFIC_DAYS") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WEEKDAY_LABELS.forEach { (label, bit) ->
                        val isSelected = (targetWeekdays and bit) != 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable {
                                    targetWeekdays = if (isSelected) {
                                        val remaining = targetWeekdays and bit.inv()
                                        if (remaining == 0) bit else remaining
                                    } else {
                                        targetWeekdays or bit
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.take(1),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Fast Save Action (accessible immediately without scrolling)
            DaymarkButton(
                text = "Save",
                onClick = handleSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // More Options Toggle (Never expanded automatically)
            TextButton(
                onClick = { showMoreOptions = !showMoreOptions },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showMoreOptions) "Fewer options" else "More options (remind me, routine, tracker, notes)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }

            if (showMoreOptions) {
                Spacer(modifier = Modifier.height(10.dp))

                // Notes (Optional)
                DaymarkTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Notes or motivation (optional)",
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Remind me / Reminder Alarm
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Remind me",
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
                    val quickAlarms = listOf(
                        null to "No reminder",
                        480 to "Morning (8:00 AM)",
                        780 to "Afternoon (1:00 PM)",
                        1200 to "Evening (8:00 PM)"
                    )
                    val isCustomTime = reminderTime != null && reminderTime != 480 && reminderTime != 780 && reminderTime != 1200

                    quickAlarms.forEach { (timeVal, label) ->
                        val isSelected = reminderTime == timeVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { reminderTime = timeVal }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCustomTime) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable {
                                val initHour = (reminderTime ?: 480) / 60
                                val initMin = (reminderTime ?: 480) % 60
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        reminderTime = hourOfDay * 60 + minute
                                    },
                                    initHour,
                                    initMin,
                                    android.text.format.DateFormat.is24HourFormat(context)
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCustomTime) DateFormatter.formatTime(reminderTime) else "Pick time...",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCustomTime) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Routine / Time of Day Grouping
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Routine",
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
                        Triple("ANYTIME", "Anytime", Icons.Outlined.Bolt),
                        Triple("MORNING", "Morning", Icons.Outlined.WbSunny),
                        Triple("AFTERNOON", "Afternoon", Icons.Outlined.LightMode),
                        Triple("EVENING", "Evening", Icons.Outlined.Bedtime)
                    ).forEach { (routineKey, routineLabel, icon) ->
                        val isSelected = routineTimeOfDay.equals(routineKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { routineTimeOfDay = routineKey }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = routineLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Health Tracker (Optional)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Health Tracker (Optional)",
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
                        Triple(null, "None", null),
                        Triple("water", "Water", Icons.Outlined.WaterDrop),
                        Triple("sleep", "Sleep", Icons.Outlined.Bedtime),
                        Triple("mood", "Mood", Icons.Outlined.Mood)
                    ).forEach { (metricKey, label, icon) ->
                        val isSelected = healthMetricType == metricKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable {
                                    healthMetricType = metricKey
                                    when (metricKey) {
                                        "water" -> {
                                            if (name.isBlank() || name == "Sleep Duration" || name == "Daily Mood Check-in") name = "Water Intake"
                                            targetValue = 8
                                            unit = "glasses"
                                        }
                                        "sleep" -> {
                                            if (name.isBlank() || name == "Water Intake" || name == "Daily Mood Check-in") name = "Sleep Duration"
                                            targetValue = 8
                                            unit = "hours"
                                        }
                                        "mood" -> {
                                            if (name.isBlank() || name == "Water Intake" || name == "Sleep Duration") name = "Daily Mood Check-in"
                                            targetValue = 5
                                            unit = "rating"
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (healthMetricType != null && healthMetricType != "mood") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Daily Target ($unit)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DaymarkTextField(
                        value = targetValue.toString(),
                        onValueChange = { str ->
                            targetValue = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                        },
                        singleLine = true
                    )
                }

                // Focus Timer Duration (Optional)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Focus Timer",
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
                    val timerOptions = listOf<Int?>(null, 5, 10, 15, 20, 25, 30, 45, 60)
                    timerOptions.forEach { mins ->
                        val isSelected = targetDurationMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { targetDurationMinutes = mins }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mins == null) "None" else "${mins}m",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Color Selection
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Habit Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HABIT_COLORS.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Habit Freeze / Rest Day
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Freeze Habit (Rest Day)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Protects streak without breaking it on intentional rest days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFrozen,
                        onCheckedChange = { isFrozen = it }
                    )
                }

                // Secondary Save Action at the bottom of expanded options
                Spacer(modifier = Modifier.height(20.dp))
                DaymarkButton(
                    text = "Save",
                    onClick = handleSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
