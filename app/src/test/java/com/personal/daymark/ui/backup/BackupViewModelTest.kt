package com.personal.daymark.ui.backup

import com.personal.daymark.core.backup.BackupManager
import com.personal.daymark.core.backup.RestoreResult
import com.personal.daymark.ui.screens.backup.BackupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeBackupManager : BackupManager {
        var restoreResultToReturn: RestoreResult = RestoreResult.Success(1, 1, 1, 1, 1, 1)

        override suspend fun createBackupJson(): String = "{\"schemaVersion\": 1, \"tasks\": []}"
        override suspend fun restoreBackupFromJson(jsonString: String): RestoreResult = restoreResultToReturn
        override suspend fun exportTasksCsv(): String = "ID,Title\n1,Test"
        override suspend fun exportHabitsCsv(): String = "ID,Name\n1,Habit"
    }

    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var viewModel: BackupViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBackupManager = FakeBackupManager()
        viewModel = BackupViewModel(fakeBackupManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun confirmRestore_onSuccess_updatesStateAndEmitsSnackbar() = runTest {
        // Prepare pending restore json
        val validJson = "{\"schemaVersion\": 1, \"tasks\": [{\"id\": 1, \"title\": \"Task 1\", \"createdAt\": 0, \"updatedAt\": 0}]}"
        val field = BackupViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.personal.daymark.ui.screens.backup.BackupUiState>
        stateFlow.value = stateFlow.value.copy(
            showRestoreConfirmation = true,
            pendingRestoreJson = validJson
        )

        viewModel.confirmRestore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertFalse(state.showRestoreConfirmation)
        assertTrue(state.snackbarMessage?.contains("Restored:") == true)
    }

    @Test
    fun dismissRestoreConfirmation_clearsState() = runTest {
        val field = BackupViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.personal.daymark.ui.screens.backup.BackupUiState>
        stateFlow.value = stateFlow.value.copy(
            showRestoreConfirmation = true,
            pendingRestoreJson = "{}",
            pendingRestoreSummary = "1 task"
        )

        viewModel.dismissRestoreConfirmation()

        val state = viewModel.uiState.value
        assertFalse(state.showRestoreConfirmation)
        assertEquals(null, state.pendingRestoreJson)
        assertEquals(null, state.pendingRestoreSummary)
    }
}
