package com.personal.daymark.ui.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.data.local.entity.DayTemplateEntity
import com.personal.daymark.data.local.entity.TemplateItemEntity
import com.personal.daymark.data.local.entity.TemplateWithItems
import com.personal.daymark.data.repository.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageTemplatesUiState(
    val templates: List<TemplateWithItems> = emptyList(),
    val isLoading: Boolean = false
)

class ManageTemplatesViewModel(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    val uiState: StateFlow<ManageTemplatesUiState> = templateRepository.getAllTemplatesWithItems()
        .map { templates ->
            ManageTemplatesUiState(templates = templates, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ManageTemplatesUiState(isLoading = true)
        )

    fun createTemplate(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            templateRepository.createTemplate(name.trim(), emptyList())
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(id)
        }
    }

    fun addItemToTemplate(
        templateId: Long,
        title: String,
        startTime: String,
        durationMinutes: Int,
        isTask: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val item = TemplateItemEntity(
                templateId = templateId,
                title = title.trim(),
                startTime = startTime,
                durationMinutes = durationMinutes,
                isTask = isTask
            )
            templateRepository.addTemplateItem(item)
        }
    }

    fun deleteTemplateItem(item: TemplateItemEntity) {
        viewModelScope.launch {
            templateRepository.deleteTemplateItem(item)
        }
    }

    fun seedStarterTemplates() {
        viewModelScope.launch {
            val morningItems = listOf(
                TemplateItemEntity(templateId = 0, title = "Morning Exercise", startTime = "07:00", durationMinutes = 30, isTask = true),
                TemplateItemEntity(templateId = 0, title = "Healthy Breakfast", startTime = "07:30", durationMinutes = 30, isTask = false),
                TemplateItemEntity(templateId = 0, title = "Daily Planning", startTime = "08:00", durationMinutes = 15, isTask = true)
            )
            templateRepository.createTemplate("Morning Routine", morningItems)

            val workdayItems = listOf(
                TemplateItemEntity(templateId = 0, title = "Deep Work Block", startTime = "09:00", durationMinutes = 90, isTask = true),
                TemplateItemEntity(templateId = 0, title = "Team Sync / Meeting", startTime = "11:00", durationMinutes = 45, isTask = false),
                TemplateItemEntity(templateId = 0, title = "Lunch Break", startTime = "12:00", durationMinutes = 60, isTask = false),
                TemplateItemEntity(templateId = 0, title = "Afternoon Focus Session", startTime = "14:00", durationMinutes = 60, isTask = true)
            )
            templateRepository.createTemplate("Standard Workday", workdayItems)
        }
    }

    companion object {
        fun provideFactory(templateRepository: TemplateRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ManageTemplatesViewModel(templateRepository) as T
                }
            }
    }
}
