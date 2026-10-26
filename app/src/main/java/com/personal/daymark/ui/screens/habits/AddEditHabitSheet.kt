package com.personal.daymark.ui.screens.habits

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.DaymarkTextField

private val HABIT_COLORS = listOf(
    "#1B4332", // Pine
    "#40916C", // Sage
    "#4A6FA5", // Ocean Blue
    "#D97706", // Amber
    "#C25953", // Calm Rose
    "#6B7280", // Slate Gray
    "#7C3AED", // Royal Violet
    "#059669"  // Emerald
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
        unit: String?
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var name by remember(habitToEdit) { mutableStateOf(habitToEdit?.name ?: "") }
    var note by remember(habitToEdit) { mutableStateOf(habitToEdit?.note ?: "") }
    var frequency by remember(habitToEdit) { mutableStateOf(habitToEdit?.frequency ?: "DAILY") }
    var targetWeekdays by remember(habitToEdit) { mutableIntStateOf(habitToEdit?.targetWeekdays ?: 127) }
    var selectedColorHex by remember(habitToEdit) { mutableStateOf(habitToEdit?.colorHex ?: HABIT_COLORS.first()) }
    var selectedCategoryId by remember(habitToEdit) { mutableStateOf(habitToEdit?.categoryId) }
    var healthMetricType by remember(habitToEdit) { mutableStateOf(habitToEdit?.healthMetricType) }
    var targetValue by remember(habitToEdit) { mutableIntStateOf(habitToEdit?.targetValue ?: 8) }
    var unit by remember(habitToEdit) { mutableStateOf(habitToEdit?.unit ?: "glasses") }
    var isNameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (habitToEdit == null) "New Habit" else "Edit Habit",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Habit Name Input
            DaymarkTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (isNameError && it.isNotBlank()) {
                        isNameError = false
                    }
                },
                label = "Habit Name",
                placeholder = "e.g., Morning Meditation",
                isError = isNameError,
                errorMessage = if (isNameError) "Habit name cannot be empty" else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Note Input
            DaymarkTextField(
                value = note,
                onValueChange = { note = it },
                label = "Notes (Optional)",
                placeholder = "e.g., 10 minutes breathing exercises",
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Health Habit Type
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Habit Type",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = healthMetricType == null,
                    onClick = { healthMetricType = null },
                    label = { Text("Standard") }
                )
                FilterChip(
                    selected = healthMetricType == "water",
                    onClick = {
                        healthMetricType = "water"
                        if (name.isBlank() || name == "Sleep Duration" || name == "Daily Mood Check-in") name = "Water Intake"
                        targetValue = 8
                        unit = "glasses"
                    },
                    label = { Text("Water 💧") }
                )
                FilterChip(
                    selected = healthMetricType == "sleep",
                    onClick = {
                        healthMetricType = "sleep"
                        if (name.isBlank() || name == "Water Intake" || name == "Daily Mood Check-in") name = "Sleep Duration"
                        targetValue = 8
                        unit = "hours"
                    },
                    label = { Text("Sleep 🌙") }
                )
                FilterChip(
                    selected = healthMetricType == "mood",
                    onClick = {
                        healthMetricType = "mood"
                        if (name.isBlank() || name == "Water Intake" || name == "Sleep Duration") name = "Daily Mood Check-in"
                        targetValue = 5
                        unit = "rating"
                    },
                    label = { Text("Mood 🙂") }
                )
            }

            if (healthMetricType != null && healthMetricType != "mood") {
                Spacer(modifier = Modifier.height(10.dp))
                Column {
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
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Frequency Selection: Daily vs Specific Days
            Text(
                text = "Frequency",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = frequency == "DAILY",
                    onClick = {
                        frequency = "DAILY"
                        targetWeekdays = 127 // all days
                    },
                    label = { Text("Every Day") }
                )
                FilterChip(
                    selected = frequency == "SPECIFIC_DAYS",
                    onClick = {
                        frequency = "SPECIFIC_DAYS"
                    },
                    label = { Text("Selected Days") }
                )
            }

            // Target Weekdays Selector (shown when SPECIFIC_DAYS is selected)
            if (frequency == "SPECIFIC_DAYS") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Target Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WEEKDAY_LABELS.forEach { (label, bit) ->
                        val isSelected = (targetWeekdays and bit) != 0
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                targetWeekdays = if (isSelected) {
                                    val remaining = targetWeekdays and bit.inv()
                                    if (remaining == 0) bit else remaining // Ensure at least 1 day selected
                                } else {
                                    targetWeekdays or bit
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selection
            if (categories.isNotEmpty()) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("None") }
                    )

                    categories.forEach { category ->
                        val catColor = category.parsedColor()
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Color Selection
            Text(
                text = "Habit Color",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
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
                                unit.takeIf { healthMetricType != null }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (habitToEdit == null) "Create Habit" else "Save Changes")
                }
            }
        }
    }
}
