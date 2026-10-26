package com.personal.daymark.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.focus.FocusSessionType
import com.personal.daymark.core.focus.FocusTimerData
import com.personal.daymark.core.focus.FocusTimerManager
import com.personal.daymark.core.focus.FocusTimerState
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.local.entity.TaskWithSubtasksAndCategory
import com.personal.daymark.data.repository.SettingsRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusScreenUiState(
    val isTaskPickerOpen: Boolean = false,
    val showResetConfirmation: Boolean = false
)

class FocusViewModel(
    private val focusTimerManager: FocusTimerManager,
    taskRepository: TaskRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val timerData: StateFlow<FocusTimerData> = focusTimerManager.timerState

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    val activeTasks: StateFlow<List<TaskWithSubtasksAndCategory>> = taskRepository.getTasksByCompletion(isCompleted = false)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(FocusScreenUiState())
    val uiState: StateFlow<FocusScreenUiState> = _uiState.asStateFlow()

    fun startTimer() {
        focusTimerManager.startTimer()
    }

    fun pauseTimer() {
        focusTimerManager.pauseTimer()
    }

    fun resumeTimer() {
        focusTimerManager.resumeTimer()
    }

    fun requestResetConfirmation() {
        if (timerData.value.state != FocusTimerState.IDLE) {
            _uiState.value = _uiState.value.copy(showResetConfirmation = true)
        }
    }

    fun dismissResetConfirmation() {
        _uiState.value = _uiState.value.copy(showResetConfirmation = false)
    }

    fun confirmReset(recordPartial: Boolean) {
        dismissResetConfirmation()
        focusTimerManager.resetTimer(recordPartial = recordPartial)
    }

    fun skipPhase() {
        focusTimerManager.skipPhase()
    }

    fun selectPhase(type: FocusSessionType) {
        if (timerData.value.state == FocusTimerState.IDLE) {
            val prefs = userPreferences.value
            val durationMin = when (type) {
                FocusSessionType.FOCUS -> prefs.defaultFocusDurationMinutes
                FocusSessionType.SHORT_BREAK -> prefs.shortBreakDurationMinutes
                FocusSessionType.LONG_BREAK -> prefs.longBreakDurationMinutes
            }
            focusTimerManager.startTimer(
                durationMinutes = durationMin,
                sessionType = type
            )
        }
    }

    fun openTaskPicker() {
        _uiState.value = _uiState.value.copy(isTaskPickerOpen = true)
    }

    fun closeTaskPicker() {
        _uiState.value = _uiState.value.copy(isTaskPickerOpen = false)
    }

    fun selectTask(task: TaskWithSubtasksAndCategory?) {
        closeTaskPicker()
        focusTimerManager.linkTask(
            taskId = task?.task?.id,
            taskTitle = task?.task?.title
        )
    }

    companion object {
        fun provideFactory(
            focusTimerManager: FocusTimerManager,
            taskRepository: TaskRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FocusViewModel(focusTimerManager, taskRepository, settingsRepository) as T
            }
        }
    }
}
