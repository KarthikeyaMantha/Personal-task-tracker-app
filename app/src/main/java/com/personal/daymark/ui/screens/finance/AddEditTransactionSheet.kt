package com.personal.daymark.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.daymark.core.finance.FinanceCurrencyHelper
import com.personal.daymark.data.local.entity.FinancePaymentMode
import com.personal.daymark.data.local.entity.FinanceTransactionEntity
import com.personal.daymark.data.local.entity.FinanceTransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    existingTransaction: FinanceTransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        type: FinanceTransactionType,
        amountPaise: Long,
        timestamp: Long,
        category: String,
        accountId: String,
        toAccountId: String?,
        paymentMode: FinancePaymentMode,
        notes: String?
    ) -> Unit,
    onDelete: (Long) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedType by remember {
        mutableStateOf(
            existingTransaction?.let {
                try { FinanceTransactionType.valueOf(it.type) } catch (_: Exception) { FinanceTransactionType.EXPENSE }
            } ?: FinanceTransactionType.EXPENSE
        )
    }

    var amountText by remember {
        mutableStateOf(
            existingTransaction?.let { FinanceCurrencyHelper.paiseToRupeeString(it.amountPaise) } ?: ""
        )
    }

    val expenseCategories = listOf("Food", "Travel", "Shopping", "Bills", "Personal", "Other")
    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Personal", "Other")

    var selectedCategory by remember {
        mutableStateOf(
            existingTransaction?.category ?: (if (selectedType == FinanceTransactionType.INCOME) "Salary" else "Food")
        )
    }

    // Account & Payment mode selection
    // Mode: UPI (debits from Bank), CASH, BANK_TRANSFER
    var selectedPaymentMode by remember {
        mutableStateOf(
            existingTransaction?.let {
                try { FinancePaymentMode.valueOf(it.paymentMode) } catch (_: Exception) { FinancePaymentMode.UPI }
            } ?: FinancePaymentMode.UPI
        )
    }

    var transferDirection by remember {
        // "BANK_TO_CASH" (ATM withdrawal) or "CASH_TO_BANK"
        mutableStateOf(
            if (existingTransaction?.accountId == "cash" && existingTransaction.toAccountId == "bank") "CASH_TO_BANK"
            else "BANK_TO_CASH"
        )
    }

    var notesText by remember { mutableStateOf(existingTransaction?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Type Switcher and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTransaction != null) "Edit Transaction" else "Add Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (existingTransaction != null) {
                    IconButton(onClick = { onDelete(existingTransaction.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Transaction Type Selector: Expense / Income / Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    FinanceTransactionType.EXPENSE to "Expense",
                    FinanceTransactionType.INCOME to "Income",
                    FinanceTransactionType.TRANSFER to "Transfer"
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable {
                                selectedType = type
                                if (type == FinanceTransactionType.INCOME && selectedCategory in expenseCategories) {
                                    selectedCategory = "Salary"
                                } else if (type == FinanceTransactionType.EXPENSE && selectedCategory in incomeCategories) {
                                    selectedCategory = "Food"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Amount Field in INR (₹)
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorText = null
                },
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = "₹ ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Payment Mode / Account Selector
            if (selectedType == FinanceTransactionType.TRANSFER) {
                Text(
                    text = "Transfer Route",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = transferDirection == "BANK_TO_CASH",
                        onClick = { transferDirection = "BANK_TO_CASH" },
                        label = { Text("ATM Withdrawal (Bank → Cash)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = transferDirection == "CASH_TO_BANK",
                        onClick = { transferDirection = "CASH_TO_BANK" },
                        label = { Text("Deposit (Cash → Bank)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "Account / Payment Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPaymentMode == FinancePaymentMode.UPI,
                        onClick = { selectedPaymentMode = FinancePaymentMode.UPI },
                        label = { Text("UPI (Bank)") },
                        leadingIcon = {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = selectedPaymentMode == FinancePaymentMode.CASH,
                        onClick = { selectedPaymentMode = FinancePaymentMode.CASH },
                        label = { Text("Cash") },
                        leadingIcon = {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedPaymentMode == FinancePaymentMode.BANK_TRANSFER,
                        onClick = { selectedPaymentMode = FinancePaymentMode.BANK_TRANSFER },
                        label = { Text("Bank") },
                        leadingIcon = {
                            Icon(Icons.Outlined.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                if (selectedPaymentMode == FinancePaymentMode.UPI) {
                    Text(
                        text = "• UPI is debited directly from your Bank account (no double-counting).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            // Category Chips (for Expense & Income)
            if (selectedType != FinanceTransactionType.TRANSFER) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val categories = if (selectedType == FinanceTransactionType.EXPENSE) expenseCategories else incomeCategories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Optional Notes Field
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorText != null) {
                Text(
                    text = errorText ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save Button
            Button(
                onClick = {
                    val paise = FinanceCurrencyHelper.parseRupeesToPaise(amountText)
                    if (paise == null || paise <= 0) {
                        errorText = "Please enter a valid amount."
                        return@Button
                    }

                    val (accountId, toAccountId, paymentMode) = when (selectedType) {
                        FinanceTransactionType.TRANSFER -> {
                            if (transferDirection == "BANK_TO_CASH") {
                                Triple("bank", "cash", FinancePaymentMode.BANK_TRANSFER)
                            } else {
                                Triple("cash", "bank", FinancePaymentMode.CASH)
                            }
                        }
                        FinanceTransactionType.EXPENSE, FinanceTransactionType.INCOME -> {
                            val account = if (selectedPaymentMode == FinancePaymentMode.CASH) "cash" else "bank"
                            Triple(account, null, selectedPaymentMode)
                        }
                    }

                    onSave(
                        existingTransaction?.id ?: 0L,
                        selectedType,
                        paise,
                        existingTransaction?.timestamp ?: System.currentTimeMillis(),
                        if (selectedType == FinanceTransactionType.TRANSFER) "Transfer" else selectedCategory,
                        accountId,
                        toAccountId,
                        paymentMode,
                        notesText.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (existingTransaction != null) "Update Transaction" else "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

