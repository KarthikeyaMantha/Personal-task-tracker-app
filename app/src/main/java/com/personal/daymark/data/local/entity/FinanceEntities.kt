package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FinanceTransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class FinancePaymentMode {
    CASH,
    BANK_TRANSFER,
    UPI
}

@Entity(
    tableName = "finance_accounts",
    indices = [Index(value = ["id"], unique = true)]
)
data class FinanceAccountEntity(
    @PrimaryKey
    val id: String, // "cash", "bank"
    val name: String, // "Cash", "Bank Account"
    @ColumnInfo(name = "initial_balance_paise")
    val initialBalancePaise: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "finance_transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["account_id"]),
        Index(value = ["type"]),
        Index(value = ["category"])
    ]
)
data class FinanceTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // EXPENSE, INCOME, TRANSFER
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,
    val timestamp: Long,
    val category: String, // Food, Travel, Shopping, Bills, Personal, Other, Salary, Investment, etc.
    @ColumnInfo(name = "account_id")
    val accountId: String, // "cash", "bank"
    @ColumnInfo(name = "to_account_id")
    val toAccountId: String? = null, // for TRANSFER e.g., ATM withdrawal bank -> cash
    @ColumnInfo(name = "payment_mode")
    val paymentMode: String = "CASH", // CASH, BANK_TRANSFER, UPI (UPI debits from Bank)
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "finance_budgets"
)
data class FinanceBudgetEntity(
    @PrimaryKey
    val id: Long = 1L, // Single active monthly budget limit
    @ColumnInfo(name = "monthly_limit_paise")
    val monthlyLimitPaise: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "finance_savings_goals"
)
data class FinanceSavingsGoalEntity(
    @PrimaryKey
    val id: Long = 1L, // Single active savings goal
    val title: String,
    @ColumnInfo(name = "target_amount_paise")
    val targetAmountPaise: Long,
    @ColumnInfo(name = "current_amount_paise")
    val currentAmountPaise: Long = 0L,
    @ColumnInfo(name = "target_date")
    val targetDate: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

