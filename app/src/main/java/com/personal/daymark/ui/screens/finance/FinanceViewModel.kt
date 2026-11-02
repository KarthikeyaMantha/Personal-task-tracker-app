package com.personal.daymark.ui.screens.finance

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.daymark.core.security.FinanceSecurityManager
import com.personal.daymark.data.local.entity.FinanceAccountEntity
import com.personal.daymark.data.local.entity.FinancePaymentMode
import com.personal.daymark.data.local.entity.FinanceSavingsGoalEntity
import com.personal.daymark.data.local.entity.FinanceTransactionEntity
import com.personal.daymark.data.local.entity.FinanceTransactionType
import com.personal.daymark.data.repository.FinanceDashboardSummary
import com.personal.daymark.data.repository.FinanceRepository
import com.personal.daymark.data.repository.MonthlyFinanceHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FinanceSubTab {
    DASHBOARD,
    HISTORY
}

data class FinanceUiState(
    val isLocked: Boolean = false,
    val isPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val canUseBiometrics: Boolean = false,
    val dashboardSummary: FinanceDashboardSummary = FinanceDashboardSummary(
        totalBalancePaise = 0L,
        bankBalancePaise = 0L,
        cashBalancePaise = 0L,
        totalUpiSpentMonthPaise = 0L,
        monthlyIncomePaise = 0L,
        monthlyExpensePaise = 0L,
        monthlySavingsPaise = 0L,
        monthlyBudgetLimitPaise = null,
        monthlyBudgetRemainingPaise = null,
        savingsGoal = null
    ),
    val monthlyHistory: List<MonthlyFinanceHistory> = emptyList(),
    val accounts: List<FinanceAccountEntity> = emptyList(),
    val recentTransactions: List<FinanceTransactionEntity> = emptyList(),
    val selectedSubTab: FinanceSubTab = FinanceSubTab.DASHBOARD,
    val isAddEditSheetOpen: Boolean = false,
    val editingTransaction: FinanceTransactionEntity? = null,
    val isSetBudgetDialogOpen: Boolean = false,
    val isSavingsGoalDialogOpen: Boolean = false,
    val isAccountsDialogOpen: Boolean = false,
    val isSecurityDialogOpen: Boolean = false,
    val pinError: String? = null
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val securityManager: FinanceSecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FinanceUiState(
            isLocked = securityManager.isLocked.value,
            isPinSet = securityManager.isPinSet(),
            isBiometricEnabled = securityManager.isBiometricEnabled(),
            canUseBiometrics = securityManager.canAuthenticateBiometrics()
        )
    )
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        // Collect security lock state
        viewModelScope.launch {
            securityManager.isLocked.collect { locked ->
                _uiState.update {
                    it.copy(
                        isLocked = locked,
                        isPinSet = securityManager.isPinSet(),
                        isBiometricEnabled = securityManager.isBiometricEnabled(),
                        canUseBiometrics = securityManager.canAuthenticateBiometrics(),
                        pinError = null
                    )
                }
            }
        }

        // Collect dashboard summary
        viewModelScope.launch {
            repository.getDashboardSummary().collect { summary ->
                _uiState.update { it.copy(dashboardSummary = summary) }
            }
        }

        // Collect all transactions for recent list
        viewModelScope.launch {
            repository.getAllTransactions().collect { allTx ->
                _uiState.update { it.copy(recentTransactions = allTx.take(15)) }
            }
        }

        // Collect monthly history
        viewModelScope.launch {
            repository.getMonthlyHistory().collect { history ->
                _uiState.update { it.copy(monthlyHistory = history) }
            }
        }

        // Collect accounts
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val success = securityManager.verifyAndUnlock(enteredPin)
        if (!success) {
            _uiState.update { it.copy(pinError = "Incorrect PIN. Please try again.") }
        } else {
            _uiState.update { it.copy(pinError = null) }
        }
        return success
    }

    fun unlockWithBiometrics(activity: FragmentActivity) {
        if (!securityManager.isBiometricEnabled()) return
        securityManager.authenticateWithBiometrics(
            activity = activity,
            onSuccess = {
                _uiState.update { it.copy(isLocked = false, pinError = null) }
            },
            onError = { err ->
                _uiState.update { it.copy(pinError = err) }
            }
        )
    }

    fun setPin(pin: String): Boolean {
        val success = securityManager.setPin(pin)
        if (success) {
            _uiState.update {
                it.copy(
                    isPinSet = true,
                    isLocked = false,
                    isSecurityDialogOpen = false,
                    pinError = null
                )
            }
        }
        return success
    }

    fun removePin() {
        securityManager.removePin()
        _uiState.update {
            it.copy(
                isPinSet = false,
                isBiometricEnabled = false,
                isLocked = false,
                isSecurityDialogOpen = false
            )
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _uiState.update {
            it.copy(isBiometricEnabled = securityManager.isBiometricEnabled())
        }
    }

    fun lock() {
        securityManager.lock()
    }

    fun selectSubTab(tab: FinanceSubTab) {
        _uiState.update { it.copy(selectedSubTab = tab) }
    }

    fun openAddTransaction() {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = true,
                editingTransaction = null
            )
        }
    }

    fun openEditTransaction(tx: FinanceTransactionEntity) {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = true,
                editingTransaction = tx
            )
        }
    }

    fun closeAddEditSheet() {
        _uiState.update {
            it.copy(
                isAddEditSheetOpen = false,
                editingTransaction = null
            )
        }
    }

    fun saveTransaction(
        id: Long = 0L,
        type: FinanceTransactionType,
        amountPaise: Long,
        timestamp: Long,
        category: String,
        accountId: String,
        toAccountId: String?,
        paymentMode: FinancePaymentMode,
        notes: String?
    ) {
        viewModelScope.launch {
            val entity = FinanceTransactionEntity(
                id = id,
                type = type.name,
                amountPaise = amountPaise,
                timestamp = timestamp,
                category = category,
                accountId = accountId,
                toAccountId = toAccountId,
                paymentMode = paymentMode.name,
                notes = notes?.takeIf { it.isNotBlank() }
            )

            if (id == 0L) {
                repository.addTransaction(entity)
            } else {
                repository.updateTransaction(entity)
            }
            closeAddEditSheet()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            closeAddEditSheet()
        }
    }

    fun openSetBudgetDialog() {
        _uiState.update { it.copy(isSetBudgetDialogOpen = true) }
    }

    fun closeSetBudgetDialog() {
        _uiState.update { it.copy(isSetBudgetDialogOpen = false) }
    }

    fun saveBudget(monthlyLimitPaise: Long) {
        viewModelScope.launch {
            repository.setMonthlyBudget(monthlyLimitPaise)
            closeSetBudgetDialog()
        }
    }

    fun openSavingsGoalDialog() {
        _uiState.update { it.copy(isSavingsGoalDialogOpen = true) }
    }

    fun closeSavingsGoalDialog() {
        _uiState.update { it.copy(isSavingsGoalDialogOpen = false) }
    }

    fun saveSavingsGoal(
        title: String,
        targetAmountPaise: Long,
        currentAmountPaise: Long,
        targetDate: Long?
    ) {
        viewModelScope.launch {
            repository.setSavingsGoal(
                title = title,
                targetAmountPaise = targetAmountPaise,
                currentAmountPaise = currentAmountPaise,
                targetDate = targetDate
            )
            closeSavingsGoalDialog()
        }
    }

    fun deleteSavingsGoal() {
        viewModelScope.launch {
            repository.deleteSavingsGoal()
            closeSavingsGoalDialog()
        }
    }

    fun openAccountsDialog() {
        _uiState.update { it.copy(isAccountsDialogOpen = true) }
    }

    fun closeAccountsDialog() {
        _uiState.update { it.copy(isAccountsDialogOpen = false) }
    }

    fun updateAccountBalance(accountId: String, initialBalancePaise: Long) {
        viewModelScope.launch {
            repository.updateAccountInitialBalance(accountId, initialBalancePaise)
        }
    }

    fun openSecurityDialog() {
        _uiState.update { it.copy(isSecurityDialogOpen = true) }
    }

    fun closeSecurityDialog() {
        _uiState.update { it.copy(isSecurityDialogOpen = false) }
    }

    companion object {
        fun factory(
            repository: FinanceRepository,
            securityManager: FinanceSecurityManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FinanceViewModel(repository, securityManager) as T
            }
        }
    }
}

