package com.personal.daymark.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = viewModel(
        factory = HabitsViewModel.provideFactory(
            habitRepository = (LocalContext.current.applicationContext as DaymarkApplication).appContainer.habitRepository,
            categoryRepository = (LocalContext.current.applicationContext as DaymarkApplication).appContainer.categoryRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DaymarkTopAppBar(
                title = "Habits",
                actions = {
                    IconButton(onClick = { viewModel.openAddHabit() }) {
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
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            onLogHealthMetric = { value, note -> viewModel.logHealthMetric(habitItem.habit.id, value, note) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Modal Sheet
    if (uiState.isAddEditSheetOpen) {
        AddEditHabitSheet(
            habitToEdit = uiState.habitToEdit,
            categories = categories,
            onDismissRequest = { viewModel.closeAddEditSheet() },
            onSaveHabit = { id, name, note, frequency, targetWeekdays, colorHex, categoryId, healthMetricType, targetValue, unit ->
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
                    unit = unit
                )
            }
        )
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
}
