package com.personal.daymark.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.habits.HabitCalculationHelper
import com.personal.daymark.core.habits.HabitDayHistory
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitItemUiState(
    val habit: HabitEntity,
    val category: CategoryEntity?,
    val isCompletedToday: Boolean,
    val isDueToday: Boolean,
    val isFrozenToday: Boolean = false,
    val currentStreak: Int,
    val completionsLast7Days: Int,
    val completionsLast30Days: Int,
    val historyDays: List<HabitDayHistory>,
    val todayValue: Float? = null,
    val todayNote: String? = null
)

data class HabitsUiState(
    val habits: List<HabitItemUiState> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val isAddEditSheetOpen: Boolean = false,
    val habitToEdit: HabitEntity? = null,
    val habitToDelete: HabitEntity? = null,
    val selectedCategoryId: Long? = null,
    val selectedRoutineTimeOfDay: String? = null
)

class HabitsViewModel(
    private val habitRepository: HabitRepository,
    categoryRepository: CategoryRepository,
    private val reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler? = null
) : ViewModel() {

    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _habitToEdit = MutableStateFlow<HabitEntity?>(null)
    private val _habitToDelete = MutableStateFlow<HabitEntity?>(null)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedRoutineTimeOfDay = MutableStateFlow<String?>(null)

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<HabitsUiState> = combine(
        habitRepository.getActiveHabitsWithLogs(),
        combine(_isAddEditSheetOpen, _habitToEdit, _habitToDelete) { isAddEdit, toEdit, toDelete ->
            Triple(isAddEdit, toEdit, toDelete)
        },
        _selectedCategoryId,
        _selectedRoutineTimeOfDay,
        com.personal.daymark.core.common.DaymarkDayManager.dayEpoch
    ) { habitsWithLogs, (isAddEditOpen, habitToEdit, habitToDelete), selectedCatId, selectedRoutine, _ ->
        val today = LocalDate.now()

        val habitItems = habitsWithLogs
            .filter { selectedCatId == null || it.habit.categoryId == selectedCatId }
            .filter { selectedRoutine == null || it.habit.routineTimeOfDay.equals(selectedRoutine, ignoreCase = true) }
            .map { habitWithLogs ->
                val habit = habitWithLogs.habit
                val logs = habitWithLogs.logs
                val isCompletedToday = HabitCalculationHelper.isCompletedOnDate(logs, today)
                val isDueToday = HabitCalculationHelper.isScheduledOnDate(habit, today)
                val isFrozenToday = HabitCalculationHelper.isFrozenOnDate(logs, today) || habit.isFrozen
                val streak = HabitCalculationHelper.calculateCurrentStreak(habit, logs, today)
                val last7 = HabitCalculationHelper.calculateCompletionsInWindow(logs, today, 7)
                val last30 = HabitCalculationHelper.calculateCompletionsInWindow(logs, today, 30)
                val history = HabitCalculationHelper.buildHistoryDays(habit, logs, today, daysCount = 14)
                val todayLog = logs.find { it.dateString == today.toString() || it.date == today.toEpochDay() }

                HabitItemUiState(
                    habit = habit,
                    category = habitWithLogs.category,
                    isCompletedToday = isCompletedToday,
                    isDueToday = isDueToday,
                    isFrozenToday = isFrozenToday,
                    currentStreak = streak,
                    completionsLast7Days = last7,
                    completionsLast30Days = last30,
                    historyDays = history,
                    todayValue = todayLog?.value,
                    todayNote = todayLog?.note
                )
            }

        HabitsUiState(
            habits = habitItems,
            today = today,
            isAddEditSheetOpen = isAddEditOpen,
            habitToEdit = habitToEdit,
            habitToDelete = habitToDelete,
            selectedCategoryId = selectedCatId,
            selectedRoutineTimeOfDay = selectedRoutine
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState()
    )

    fun openAddHabit() {
        _habitToEdit.value = null
        _isAddEditSheetOpen.value = true
    }

    fun openEditHabit(habit: HabitEntity) {
        _habitToEdit.value = habit
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _habitToEdit.value = null
    }

    fun requestDeleteConfirmation(habit: HabitEntity) {
        _habitToDelete.value = habit
    }

    fun dismissDeleteConfirmation() {
        _habitToDelete.value = null
    }

    fun filterByCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun filterByRoutine(routineTimeOfDay: String?) {
        _selectedRoutineTimeOfDay.value = routineTimeOfDay
    }

    fun toggleCompletionToday(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId, LocalDate.now())
        }
    }

    fun toggleFreezeToday(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabitFreeze(habitId, LocalDate.now())
        }
    }

    fun toggleCompletionForDate(habitId: Long, date: LocalDate) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId, date)
        }
    }

    fun incrementWater(habitId: Long) {
        viewModelScope.launch {
            habitRepository.incrementWater(habitId, LocalDate.now())
        }
    }

    fun logHealthMetric(habitId: Long, value: Float, note: String? = null) {
        viewModelScope.launch {
            habitRepository.logHealthMetric(habitId, value, note, LocalDate.now())
        }
    }

    fun saveHabit(
        id: Long = 0L,
        name: String,
        note: String?,
        frequency: String,
        targetWeekdays: Int,
        colorHex: String,
        categoryId: Long?,
        healthMetricType: String? = null,
        targetValue: Int? = null,
        unit: String? = null,
        reminderTime: Int? = null,
        targetDurationMinutes: Int? = null,
        routineTimeOfDay: String = "ANYTIME",
        isFrozen: Boolean = false
    ) {
        viewModelScope.launch {
            if (id > 0L) {
                val existing = _habitToEdit.value
                val updated = existing?.copy(
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
                ) ?: HabitEntity(
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
                habitRepository.updateHabit(updated)
                if (updated.reminderTime != null) {
                    reminderScheduler?.scheduleHabitReminder(updated)
                } else {
                    reminderScheduler?.cancelHabitReminder(updated.id)
                }
            } else {
                val newId = habitRepository.createHabit(
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
                if (reminderTime != null) {
                    val created = HabitEntity(
                        id = newId,
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
                    reminderScheduler?.scheduleHabitReminder(created)
                }
            }
            closeAddEditSheet()
        }
    }

    fun confirmDeleteHabit() {
        val habit = _habitToDelete.value ?: return
        viewModelScope.launch {
            reminderScheduler?.cancelHabitReminder(habit.id)
            habitRepository.deleteHabitById(habit.id)
            _habitToDelete.value = null
        }
    }

    companion object {
        fun provideFactory(
            habitRepository: HabitRepository,
            categoryRepository: CategoryRepository,
            reminderScheduler: com.personal.daymark.core.notifications.ReminderScheduler? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HabitsViewModel(habitRepository, categoryRepository, reminderScheduler) as T
            }
        }
    }
}
