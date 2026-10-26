package com.personal.daymark.ui.screens.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.backup.BackupManager
import com.personal.daymark.core.backup.CURRENT_BACKUP_SCHEMA_VERSION
import com.personal.daymark.core.backup.DaymarkBackupData
import com.personal.daymark.core.backup.RestoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class BackupUiState(
    val isProcessing: Boolean = false,
    val processingMessage: String? = null,
    val snackbarMessage: String? = null,
    val showRestoreConfirmation: Boolean = false,
    val pendingRestoreJson: String? = null,
    val pendingRestoreSummary: String? = null
)

class BackupViewModel(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun onBackupCreated(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Creating backup...") }
            try {
                val jsonString = backupManager.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = "Backup successfully created and saved."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = "Backup failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun onRestoreFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Reading backup file...") }
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw IllegalArgumentException("Could not read from selected file.")

                // Pre-validate JSON format and schema
                val backupData: DaymarkBackupData = try {
                    json.decodeFromString(DaymarkBackupData.serializer(), content)
                } catch (e: SerializationException) {
                    throw IllegalArgumentException("The selected file is not a valid Daymark JSON backup.")
                }

                if (backupData.schemaVersion > CURRENT_BACKUP_SCHEMA_VERSION) {
                    throw IllegalArgumentException("This backup was created with a newer app version (Schema v${backupData.schemaVersion}).")
                }

                if (backupData.schemaVersion <= 0) {
                    throw IllegalArgumentException("Invalid backup file format.")
                }

                val summary = "${backupData.tasks.size} tasks, ${backupData.habits.size} habits, ${backupData.focusSessions.size} focus sessions"

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        showRestoreConfirmation = true,
                        pendingRestoreJson = content,
                        pendingRestoreSummary = summary
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = e.message ?: "Failed to read backup file."
                    )
                }
            }
        }
    }

    fun confirmRestore() {
        val pendingJson = _uiState.value.pendingRestoreJson ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showRestoreConfirmation = false,
                    isProcessing = true,
                    processingMessage = "Restoring data..."
                )
            }

            when (val result = backupManager.restoreBackupFromJson(pendingJson)) {
                is RestoreResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingMessage = null,
                            pendingRestoreJson = null,
                            pendingRestoreSummary = null,
                            snackbarMessage = "Restored: ${result.tasksCount} tasks, ${result.habitsCount} habits, ${result.focusSessionsCount} focus sessions."
                        )
                    }
                }
                is RestoreResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            processingMessage = null,
                            pendingRestoreJson = null,
                            pendingRestoreSummary = null,
                            snackbarMessage = "Restore failed: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun dismissRestoreConfirmation() {
        _uiState.update {
            it.copy(
                showRestoreConfirmation = false,
                pendingRestoreJson = null,
                pendingRestoreSummary = null
            )
        }
    }

    fun onExportTasksCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Exporting tasks...") }
            try {
                val csvContent = backupManager.exportTasksCsv()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = "Tasks successfully exported to CSV."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = "Tasks export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun onExportHabitsCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Exporting habits...") }
            try {
                val csvContent = backupManager.exportHabitsCsv()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = "Habits successfully exported to CSV."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = null,
                        snackbarMessage = "Habits export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        fun provideFactory(backupManager: BackupManager): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BackupViewModel(backupManager) as T
            }
        }
    }
}
