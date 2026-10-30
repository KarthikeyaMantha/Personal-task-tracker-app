package com.personal.daymark.ui.screens.habits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import com.personal.daymark.ui.voice.VoiceInputBottomSheet
import com.personal.daymark.ui.voice.VoiceInputViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.data.local.entity.parsedColor
import com.personal.daymark.ui.components.DaymarkCard
import com.personal.daymark.ui.components.DaymarkTopAppBar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    modifier: Modifier = Modifier,
    onNavigateToFocus: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).appContainer

    val viewModel: HabitsViewModel = viewModel(
        factory = HabitsViewModel.provideFactory(
            habitRepository = appContainer.habitRepository,
            categoryRepository = appContainer.categoryRepository,
            reminderScheduler = appContainer.reminderScheduler
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var habitSheetSessionKey by remember { mutableLongStateOf(0L) }
    var isVoiceInputOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DaymarkTopAppBar(
                title = "Habits",
                actions = {
                    IconButton(onClick = { isVoiceInputOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Action",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        habitSheetSessionKey = System.currentTimeMillis()
                        viewModel.openAddHabit()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Habit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Bar (if categories exist)
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("All") }
                    )
                    categories.forEach { category ->
                        val catColor = category.parsedColor()
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = {
                                if (uiState.selectedCategoryId == category.id) {
                                    viewModel.filterByCategory(null)
                                } else {
                                    viewModel.filterByCategory(category.id)
                                }
                            },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }
            }

            // Routine / Time-of-Day Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(null, "All Routines", null),
                    Triple("MORNING", "Morning", Icons.Outlined.WbSunny),
                    Triple("AFTERNOON", "Afternoon", Icons.Outlined.LightMode),
                    Triple("EVENING", "Evening", Icons.Outlined.Bedtime),
                    Triple("ANYTIME", "Anytime", Icons.Outlined.Bolt)
                ).forEach { (routineKey, label, icon) ->
                    val isSelected = uiState.selectedRoutineTimeOfDay.equals(routineKey, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterByRoutine(if (isSelected && routineKey != null) null else routineKey) },
                        label = { Text(label) },
                        leadingIcon = icon?.let {
                            {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Habits List or Empty State
            if (uiState.habits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DaymarkCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Build Mindful Habits",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Track daily rituals, maintain honest streaks, and reflect on your consistency over time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.openAddHabit() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.padding(2.dp))
                                Text("Create First Habit")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.selectedRoutineTimeOfDay != null) {
                        items(
                            items = uiState.habits,
                            key = { it.habit.id }
                        ) { habitItem ->
                            HabitCard(
                                habitItem = habitItem,
                                onToggleToday = { viewModel.toggleCompletionToday(habitItem.habit.id) },
                                onToggleDate = { date -> viewModel.toggleCompletionForDate(habitItem.habit.id, date) },
                                onEditHabit = { viewModel.openEditHabit(habitItem.habit) },
                                onDeleteHabit = { viewModel.requestDeleteConfirmation(habitItem.habit) },
                                onIncrementWater = { viewModel.incrementWater(habitItem.habit.id) },
                                onLogHealthMetric = { value, note -> viewModel.logHealthMetric(habitItem.habit.id, value, note) },
                                onStartTimer = { habit ->
                                    appContainer.focusTimerManager.startTimer(
                                        durationMinutes = habit.targetDurationMinutes ?: 15,
                                        habitId = habit.id,
                                        taskTitle = "Habit: ${habit.name}"
                                    )
                                    onNavigateToFocus?.invoke()
                                },
                                onToggleFreezeToday = { viewModel.toggleFreezeToday(habitItem.habit.id) }
                            )
                        }
                    } else {
                        val routines = listOf(
                            Triple("MORNING", "Morning Routine", Icons.Outlined.WbSunny),
                            Triple("AFTERNOON", "Afternoon Routine", Icons.Outlined.LightMode),
                            Triple("EVENING", "Evening Wind-down", Icons.Outlined.Bedtime),
                            Triple("ANYTIME", "Anytime Habits", Icons.Outlined.Bolt)
                        )

                        routines.forEach { (routineKey, routineTitle, icon) ->
                            val habitsInRoutine = uiState.habits.filter {
                                if (routineKey == "ANYTIME") {
                                    it.habit.routineTimeOfDay.equals("ANYTIME", ignoreCase = true) ||
                                        (!it.habit.routineTimeOfDay.equals("MORNING", ignoreCase = true) &&
                                         !it.habit.routineTimeOfDay.equals("AFTERNOON", ignoreCase = true) &&
                                         !it.habit.routineTimeOfDay.equals("EVENING", ignoreCase = true))
                                } else {
                                    it.habit.routineTimeOfDay.equals(routineKey, ignoreCase = true)
                                }
                            }

                            if (habitsInRoutine.isNotEmpty()) {
                                item(key = "header_$routineKey") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = routineTitle,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                items(
                                    items = habitsInRoutine,
                                    key = { it.habit.id }
                                ) { habitItem ->
                                    HabitCard(
                                        habitItem = habitItem,
                                        onToggleToday = { viewModel.toggleCompletionToday(habitItem.habit.id) },
                                        onToggleDate = { date -> viewModel.toggleCompletionForDate(habitItem.habit.id, date) },
                                        onEditHabit = {
                                            habitSheetSessionKey = System.currentTimeMillis()
                                            viewModel.openEditHabit(habitItem.habit)
                                        },
                                        onDeleteHabit = { viewModel.requestDeleteConfirmation(habitItem.habit) },
                                        onIncrementWater = { viewModel.incrementWater(habitItem.habit.id) },
                                        onLogHealthMetric = { value, note -> viewModel.logHealthMetric(habitItem.habit.id, value, note) },
                                        onStartTimer = { habit ->
                                            appContainer.focusTimerManager.startTimer(
                                                durationMinutes = habit.targetDurationMinutes ?: 15,
                                                habitId = habit.id,
                                                taskTitle = "Habit: ${habit.name}"
                                            )
                                            onNavigateToFocus?.invoke()
                                        },
                                        onToggleFreezeToday = { viewModel.toggleFreezeToday(habitItem.habit.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Modal Sheet
    if (uiState.isAddEditSheetOpen) {
        androidx.compose.runtime.key(uiState.habitToEdit?.id, habitSheetSessionKey) {
            AddEditHabitSheet(
                habitToEdit = uiState.habitToEdit,
                categories = categories,
                onDismissRequest = { viewModel.closeAddEditSheet() },
                onSaveHabit = { id, name, note, frequency, targetWeekdays, colorHex, categoryId, healthMetricType, targetValue, unit, reminderTime, targetDurationMinutes, routineTimeOfDay, isFrozen ->
                    viewModel.saveHabit(
                        id = id,
                        name = name,
                        note = note,
                        frequency = frequency,
                        targetWeekdays = targetWeekdays,
                        colorHex = colorHex,
                        categoryId = categoryId,
                        healthMetricType = healthMetricType,
                        targetValue = targetValue,
                        unit = unit,
                        reminderTime = reminderTime,
                        targetDurationMinutes = targetDurationMinutes,
                        routineTimeOfDay = routineTimeOfDay,
                        isFrozen = isFrozen
                    )
                }
            )
        }
    }

    // Explicit Delete Confirmation Dialog with Cascade Warning
    uiState.habitToDelete?.let { habitToDelete ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text("Delete Habit?") },
            text = {
                Text(
                    "Deleting \"${habitToDelete.name}\" will permanently remove all associated completion logs and history. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteHabit() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Voice Input Action Modal Sheet
    if (isVoiceInputOpen) {
        val voiceViewModel: VoiceInputViewModel = viewModel(
            key = "habits_voice_input_vm",
            factory = VoiceInputViewModel.provideFactory(
                context = context,
                taskRepository = appContainer.taskRepository,
                habitRepository = appContainer.habitRepository,
                categoryRepository = appContainer.categoryRepository,
                reminderScheduler = appContainer.reminderScheduler
            )
        )
        VoiceInputBottomSheet(
            viewModel = voiceViewModel,
            onDismissRequest = { isVoiceInputOpen = false },
            onActionCompleted = { }
        )
    }
}
