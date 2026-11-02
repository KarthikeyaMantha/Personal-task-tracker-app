package com.personal.daymark.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.daymark.core.finance.FinanceCurrencyHelper
import com.personal.daymark.data.local.entity.FinanceSavingsGoalEntity

@Composable
fun SetBudgetDialog(
    currentLimitPaise: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amountText by remember {
        mutableStateOf(currentLimitPaise?.let { FinanceCurrencyHelper.paiseToRupeeString(it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Monthly Spending Budget",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Set a monthly expense limit to track remaining balance and progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Budget Limit (₹)") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val paise = FinanceCurrencyHelper.parseRupeesToPaise(amountText)
                    if (paise != null && paise > 0) {
                        onSave(paise)
                    }
                }
            ) {
                Text("Save Limit", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SetSavingsGoalDialog(
    existingGoal: FinanceSavingsGoalEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, targetPaise: Long, currentPaise: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    var targetText by remember {
        mutableStateOf(existingGoal?.targetAmountPaise?.let { FinanceCurrencyHelper.paiseToRupeeString(it) } ?: "")
    }
    var currentText by remember {
        mutableStateOf(existingGoal?.currentAmountPaise?.let { FinanceCurrencyHelper.paiseToRupeeString(it) } ?: "0")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingGoal != null) "Edit Savings Goal" else "New Savings Goal",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Name (e.g. Emergency Fund, Trip)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount (₹)") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Currently Saved (₹)") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetPaise = FinanceCurrencyHelper.parseRupeesToPaise(targetText)
                    val currentPaise = FinanceCurrencyHelper.parseRupeesToPaise(currentText) ?: 0L
                    if (title.isNotBlank() && targetPaise != null && targetPaise > 0) {
                        onSave(title.trim(), targetPaise, currentPaise)
                    }
                }
            ) {
                Text("Save Goal", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                if (existingGoal != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun SetAccountBalancesDialog(
    cashPaise: Long,
    bankPaise: Long,
    onDismiss: () -> Unit,
    onSave: (cashPaise: Long, bankPaise: Long) -> Unit
) {
    var cashText by remember { mutableStateOf(FinanceCurrencyHelper.paiseToRupeeString(cashPaise)) }
    var bankText by remember { mutableStateOf(FinanceCurrencyHelper.paiseToRupeeString(bankPaise)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Starting Balances",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Set your initial funds. Note: UPI is linked to your Bank account, so UPI payments automatically deduct from Bank without double-counting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = bankText,
                    onValueChange = { bankText = it },
                    label = { Text("Bank Account Balance (₹)") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text("Cash in Hand (₹)") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cash = FinanceCurrencyHelper.parseRupeesToPaise(cashText) ?: 0L
                    val bank = FinanceCurrencyHelper.parseRupeesToPaise(bankText) ?: 0L
                    onSave(cash, bank)
                }
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FinanceSecurityDialog(
    isPinSet: Boolean,
    isBiometricEnabled: Boolean,
    canUseBiometrics: Boolean,
    onDismiss: () -> Unit,
    onSetPin: (String) -> Boolean,
    onRemovePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var biometricChecked by remember { mutableStateOf(isBiometricEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Finance Security & Lock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isPinSet) "Update your 4–6 digit security PIN or biometric settings."
                    else "Set up a 4–6 digit PIN to protect your personal financial records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pinText = it
                            error = null
                        }
                    },
                    label = { Text(if (isPinSet) "New 4–6 Digit PIN" else "4–6 Digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = confirmPinText,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPinText = it
                            error = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (canUseBiometrics) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Unlock",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "Unlock with fingerprint",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = biometricChecked,
                            onCheckedChange = {
                                biometricChecked = it
                                onToggleBiometric(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pinText.length !in 4..6) {
                        error = "PIN must be 4 to 6 digits."
                        return@TextButton
                    }
                    if (pinText != confirmPinText) {
                        error = "PINs do not match."
                        return@TextButton
                    }
                    val success = onSetPin(pinText)
                    if (!success) {
                        error = "Failed to set PIN."
                    }
                }
            ) {
                Text(if (isPinSet) "Update PIN" else "Set PIN", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                if (isPinSet) {
                    TextButton(onClick = onRemovePin) {
                        Text("Disable Lock", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

