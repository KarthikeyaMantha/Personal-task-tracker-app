package com.personal.daymark.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val CALM_CATEGORY_COLORS = listOf(
    "#40916C", // Sage Green
    "#1B4332", // Forest Evergreen
    "#4A6FA5", // Serene Blue
    "#D97706", // Warm Amber
    "#8B5CF6", // Lavender Violet
    "#EC4899", // Soft Rose
    "#0D9488", // Deep Teal
    "#E11D48", // Crimson
    "#6B7280"  // Slate Grey
)

data class ManageCategoriesUiState(
    val editingCategory: CategoryEntity? = null,
    val isCreatingNew: Boolean = false,
    val nameInput: String = "",
    val selectedColorHex: String = CALM_CATEGORY_COLORS[0],
    val errorMessage: String? = null,
    val categoryPendingDelete: CategoryEntity? = null
)

class ManageCategoriesViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(ManageCategoriesUiState())
    val uiState: StateFlow<ManageCategoriesUiState> = _uiState.asStateFlow()

    fun startCreateCategory() {
        _uiState.update {
            it.copy(
                isCreatingNew = true,
                editingCategory = null,
                nameInput = "",
                selectedColorHex = CALM_CATEGORY_COLORS[0],
                errorMessage = null
            )
        }
    }

    fun startEditCategory(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                isCreatingNew = false,
                editingCategory = category,
                nameInput = category.name,
                selectedColorHex = category.colorHex,
                errorMessage = null
            )
        }
    }

    fun cancelEditOrCreate() {
        _uiState.update {
            it.copy(
                isCreatingNew = false,
                editingCategory = null,
                nameInput = "",
                errorMessage = null
            )
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(nameInput = newName, errorMessage = null) }
    }

    fun onColorSelected(hex: String) {
        _uiState.update { it.copy(selectedColorHex = hex) }
    }

    fun saveCategory() {
        val trimmed = _uiState.value.nameInput.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Category name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val editing = _uiState.value.editingCategory
            if (editing != null) {
                categoryRepository.updateCategory(
                    editing.copy(
                        name = trimmed,
                        colorHex = _uiState.value.selectedColorHex
                    )
                )
            } else {
                categoryRepository.createCategory(
                    name = trimmed,
                    colorHex = _uiState.value.selectedColorHex
                )
            }
            cancelEditOrCreate()
        }
    }

    fun promptDelete(category: CategoryEntity) {
        _uiState.update { it.copy(categoryPendingDelete = category) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(categoryPendingDelete = null) }
    }

    fun confirmDeleteCategory() {
        val target = _uiState.value.categoryPendingDelete ?: return
        viewModelScope.launch {
            // Safe deletion: tasks referencing this category are set to null via categoryRepository
            categoryRepository.deleteCategoryById(target.id)
            _uiState.update { it.copy(categoryPendingDelete = null) }
        }
    }

    companion object {
        fun provideFactory(categoryRepository: CategoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ManageCategoriesViewModel(categoryRepository) as T
                }
            }
    }
}
