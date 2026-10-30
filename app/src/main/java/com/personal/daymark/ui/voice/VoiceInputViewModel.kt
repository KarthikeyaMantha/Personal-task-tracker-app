package com.personal.daymark.ui.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.notifications.ReminderScheduler
import com.personal.daymark.core.voice.VoiceAction
import com.personal.daymark.core.voice.VoiceActionExecutor
import com.personal.daymark.core.voice.VoiceActionParser
import com.personal.daymark.core.voice.VoiceExecutionResult
import com.personal.daymark.core.voice.VoiceSpeechManager
import com.personal.daymark.core.voice.VoiceSpeechState
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.data.repository.HabitRepository
import com.personal.daymark.data.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class VoiceInputUiEvent {
    data class ExecutionComplete(val message: String) : VoiceInputUiEvent()
    data class ShowError(val message: String) : VoiceInputUiEvent()
    object Dismiss : VoiceInputUiEvent()
}

class VoiceInputViewModel(
    context: Context,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    reminderScheduler: ReminderScheduler? = null
) : ViewModel() {

    private val speechManager = VoiceSpeechManager(context)
    val speechState: StateFlow<VoiceSpeechState> = speechManager.state

    private val executor = VoiceActionExecutor(taskRepository, habitRepository, reminderScheduler)

    private val _parsedAction = MutableStateFlow<VoiceAction?>(null)
    val parsedAction: StateFlow<VoiceAction?> = _parsedAction.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(3)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    private val _events = MutableSharedFlow<VoiceInputUiEvent>()
    val events: SharedFlow<VoiceInputUiEvent> = _events.asSharedFlow()

    private var countdownJob: Job? = null
    private var categories: List<CategoryEntity> = emptyList()

    init {
        viewModelScope.launch {
            categories = try {
                categoryRepository.getAllCategories().first()
            } catch (e: Exception) {
                emptyList()
            }
        }

        viewModelScope.launch {
            speechManager.state.collect { state ->
                when (state) {
                    is VoiceSpeechState.Success -> {
                        onTranscriptionSuccess(state.rawText)
                    }
                    else -> {}
                }
            }
        }
    }

    fun startListening() {
        cancelCountdown()
        _parsedAction.value = null
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun retry() {
        cancelCountdown()
        _parsedAction.value = null
        speechManager.reset()
        speechManager.startListening()
    }

    fun cancel() {
        cancelCountdown()
        speechManager.cancelListening()
        viewModelScope.launch {
            _events.emit(VoiceInputUiEvent.Dismiss)
        }
    }

    private fun onTranscriptionSuccess(transcript: String) {
        val action = VoiceActionParser.parse(transcript, categories)
        _parsedAction.value = action

        if (action !is VoiceAction.Unknown) {
            startAutoSaveCountdown()
        }
    }

    private fun startAutoSaveCountdown() {
        cancelCountdown()
        _countdownSeconds.value = 3
        _isCountdownActive.value = true

        countdownJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _countdownSeconds.value = sec
                delay(1000L)
            }
            _countdownSeconds.value = 0
            _isCountdownActive.value = false
            confirmAction()
        }
    }

    fun pauseCountdown() {
        cancelCountdown()
    }

    fun confirmAction(customTitle: String? = null) {
        cancelCountdown()
        val baseAction = _parsedAction.value ?: return
        val finalAction = if (!customTitle.isNullOrBlank() && baseAction is VoiceAction.CreateTask) {
            baseAction.copy(title = customTitle.trim())
        } else {
            baseAction
        }

        viewModelScope.launch {
            when (val result = executor.execute(finalAction)) {
                is VoiceExecutionResult.Success -> {
                    _events.emit(VoiceInputUiEvent.ExecutionComplete(result.message))
                }
                is VoiceExecutionResult.NotFound -> {
                    _events.emit(VoiceInputUiEvent.ShowError(result.message))
                }
                is VoiceExecutionResult.Error -> {
                    _events.emit(VoiceInputUiEvent.ShowError(result.message))
                }
            }
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _isCountdownActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        cancelCountdown()
        speechManager.destroy()
    }

    companion object {
        fun provideFactory(
            context: Context,
            taskRepository: TaskRepository,
            habitRepository: HabitRepository,
            categoryRepository: CategoryRepository,
            reminderScheduler: ReminderScheduler? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VoiceInputViewModel(
                    context = context,
                    taskRepository = taskRepository,
                    habitRepository = habitRepository,
                    categoryRepository = categoryRepository,
                    reminderScheduler = reminderScheduler
                ) as T
            }
        }
    }
}

