package com.personal.daymark.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.ui.components.DaymarkTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as DaymarkApplication).container
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.factory(
            repository = appContainer.financeRepository,
            securityManager = appContainer.financeSecurityManager
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // 1. If Locked, show FinanceLockScreen
    if (uiState.isLocked && uiState.isPinSet) {
        FinanceLockScreen(
            isBiometricEnabled = uiState.isBiometricEnabled,
            errorMessage = uiState.pinError,
            onPinEntered = { pin -> viewModel.verifyPin(pin) },
            onBiometricRequested = { activity -> viewModel.unlockWithBiometrics(activity) }
        )
        return
    }

    // 2. Unlocked Main Finance Screen
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DaymarkTopAppBar(
                title = "Finance",
                navigationIcon = if (onOpenDrawer != null) {
                    {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open navigation menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else null,
                actions = {
                    if (uiState.isPinSet) {
                        IconButton(
                            onClick = { viewModel.lock() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Lock Finance",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.openSecurityDialog() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = "Finance Security & PIN",
                            tint = if (uiState.isPinSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTransaction() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // SubTab Switcher: Dashboard vs History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    FinanceSubTab.DASHBOARD to "Dashboard",
                    FinanceSubTab.HISTORY to "History"
                ).forEach { (tab, label) ->
                    val isSelected = uiState.selectedSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { viewModel.selectSubTab(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.selectedSubTab) {
                FinanceSubTab.DASHBOARD -> {
                    FinanceDashboardView(
                        summary = uiState.dashboardSummary,
                        recentTransactions = uiState.recentTransactions,
                        onTransactionClick = { tx -> viewModel.openEditTransaction(tx) },
                        onAdjustAccountsClick = { viewModel.openAccountsDialog() },
                        onSetBudgetClick = { viewModel.openSetBudgetDialog() },
                        onSetSavingsGoalClick = { viewModel.openSavingsGoalDialog() },
                        onViewAllHistoryClick = { viewModel.selectSubTab(FinanceSubTab.HISTORY) }
                    )
                }
                FinanceSubTab.HISTORY -> {
                    FinanceHistoryView(
                        monthlyHistory = uiState.monthlyHistory,
                        onTransactionClick = { tx -> viewModel.openEditTransaction(tx) }
                    )
                }
            }
        }
    }

    // Modal Sheets and Dialogs
    if (uiState.isAddEditSheetOpen) {
        AddEditTransactionSheet(
            existingTransaction = uiState.editingTransaction,
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { id, type, amountPaise, timestamp, category, accountId, toAccountId, paymentMode, notes ->
                viewModel.saveTransaction(
                    id = id,
                    type = type,
                    amountPaise = amountPaise,
                    timestamp = timestamp,
                    category = category,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    paymentMode = paymentMode,
                    notes = notes
                )
            },
            onDelete = { id -> viewModel.deleteTransaction(id) }
        )
    }

    if (uiState.isSetBudgetDialogOpen) {
        SetBudgetDialog(
            currentLimitPaise = uiState.dashboardSummary.monthlyBudgetLimitPaise,
            onDismiss = { viewModel.closeSetBudgetDialog() },
            onSave = { limitPaise -> viewModel.saveBudget(limitPaise) }
        )
    }

    if (uiState.isSavingsGoalDialogOpen) {
        SetSavingsGoalDialog(
            existingGoal = uiState.dashboardSummary.savingsGoal,
            onDismiss = { viewModel.closeSavingsGoalDialog() },
            onSave = { title, targetPaise, currentPaise ->
                viewModel.saveSavingsGoal(title, targetPaise, currentPaise, null)
            },
            onDelete = { viewModel.deleteSavingsGoal() }
        )
    }

    if (uiState.isAccountsDialogOpen) {
        SetAccountBalancesDialog(
            cashPaise = uiState.dashboardSummary.cashBalancePaise,
            bankPaise = uiState.dashboardSummary.bankBalancePaise,
            onDismiss = { viewModel.closeAccountsDialog() },
            onSave = { cash, bank ->
                viewModel.updateAccountBalance("cash", cash)
                viewModel.updateAccountBalance("bank", bank)
                viewModel.closeAccountsDialog()
            }
        )
    }

    if (uiState.isSecurityDialogOpen) {
        FinanceSecurityDialog(
            isPinSet = uiState.isPinSet,
            isBiometricEnabled = uiState.isBiometricEnabled,
            canUseBiometrics = uiState.canUseBiometrics,
            onDismiss = { viewModel.closeSecurityDialog() },
            onSetPin = { pin -> viewModel.setPin(pin) },
            onRemovePin = { viewModel.removePin() },
            onToggleBiometric = { enabled -> viewModel.setBiometricEnabled(enabled) }
        )
    }
}

