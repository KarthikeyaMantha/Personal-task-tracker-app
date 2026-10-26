package com.personal.daymark.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            settingsRepository.updateThemePreference(theme)
        }
    }

    fun setDefaultTaskPriority(priority: Int) {
        viewModelScope.launch {
            settingsRepository.updateDefaultTaskPriority(priority)
        }
    }

    fun setWeekStartDay(day: Int) {
        viewModelScope.launch {
            settingsRepository.updateWeekStartDay(day)
        }
    }

    fun setDefaultFocusDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDefaultFocusDuration(minutes)
        }
    }

    fun setShortBreakDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateShortBreakDuration(minutes)
        }
    }

    fun setLongBreakDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateLongBreakDuration(minutes)
        }
    }

    fun setSessionsUntilLongBreak(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateSessionsUntilLongBreak(count)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHapticFeedbackEnabled(enabled)
        }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDailySummaryEnabled(enabled)
        }
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository) as T
                }
            }
    }
}
