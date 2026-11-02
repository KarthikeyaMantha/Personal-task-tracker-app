package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.FinanceDao
import com.personal.daymark.data.local.entity.FinanceAccountEntity
import com.personal.daymark.data.local.entity.FinanceBudgetEntity
import com.personal.daymark.data.local.entity.FinancePaymentMode
import com.personal.daymark.data.local.entity.FinanceSavingsGoalEntity
import com.personal.daymark.data.local.entity.FinanceTransactionEntity
import com.personal.daymark.data.local.entity.FinanceTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class FinanceDashboardSummary(
    val totalBalancePaise: Long,
    val bankBalancePaise: Long,
    val cashBalancePaise: Long,
    val totalUpiSpentMonthPaise: Long,
    val monthlyIncomePaise: Long,
    val monthlyExpensePaise: Long,
    val monthlySavingsPaise: Long,
    val monthlyBudgetLimitPaise: Long?,
    val monthlyBudgetRemainingPaise: Long?,
    val savingsGoal: FinanceSavingsGoalEntity?
)

data class MonthlyFinanceHistory(
    val yearMonth: String, // e.g. "November 2026"
    val yearMonthKey: String, // e.g. "2026-11"
    val totalIncomePaise: Long,
    val totalExpensePaise: Long,
    val netSavingsPaise: Long,
    val transactions: List<FinanceTransactionEntity>
)

interface FinanceRepository {
    fun getAllTransactions(): Flow<List<FinanceTransactionEntity>>
    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<FinanceTransactionEntity>>
    suspend fun getTransactionById(id: Long): FinanceTransactionEntity?
    suspend fun addTransaction(transaction: FinanceTransactionEntity): Long
    suspend fun updateTransaction(transaction: FinanceTransactionEntity)
    suspend fun deleteTransaction(id: Long)

    fun getAllAccounts(): Flow<List<FinanceAccountEntity>>
    suspend fun updateAccountInitialBalance(accountId: String, initialBalancePaise: Long)

    fun getMonthlyBudget(): Flow<FinanceBudgetEntity?>
    suspend fun setMonthlyBudget(monthlyLimitPaise: Long)

    fun getSavingsGoal(): Flow<FinanceSavingsGoalEntity?>
    suspend fun setSavingsGoal(title: String, targetAmountPaise: Long, currentAmountPaise: Long, targetDate: Long?)
    suspend fun updateSavingsGoalProgress(currentAmountPaise: Long)
    suspend fun deleteSavingsGoal()

    fun getDashboardSummary(): Flow<FinanceDashboardSummary>
    fun getMonthlyHistory(): Flow<List<MonthlyFinanceHistory>>
}

class FinanceRepositoryImpl(
    private val financeDao: FinanceDao
) : FinanceRepository {

    override fun getAllTransactions(): Flow<List<FinanceTransactionEntity>> =
        financeDao.getAllTransactions()

    override fun getTransactionsForMonth(year: Int, month: Int): Flow<List<FinanceTransactionEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        return financeDao.getTransactionsBetween(startTime, endTime)
    }

    override suspend fun getTransactionById(id: Long): FinanceTransactionEntity? =
        financeDao.getTransactionById(id)

    override suspend fun addTransaction(transaction: FinanceTransactionEntity): Long =
        financeDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: FinanceTransactionEntity) =
        financeDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(id: Long) =
        financeDao.deleteTransaction(id)

    override fun getAllAccounts(): Flow<List<FinanceAccountEntity>> =
        financeDao.getAllAccounts()

    override suspend fun updateAccountInitialBalance(accountId: String, initialBalancePaise: Long) {
        val existing = financeDao.getAccountById(accountId)
        val updated = if (existing != null) {
            existing.copy(initialBalancePaise = initialBalancePaise, updatedAt = System.currentTimeMillis())
        } else {
            FinanceAccountEntity(
                id = accountId,
                name = if (accountId == "cash") "Cash" else "Bank Account",
                initialBalancePaise = initialBalancePaise
            )
        }
        financeDao.upsertAccount(updated)
    }

    override fun getMonthlyBudget(): Flow<FinanceBudgetEntity?> =
        financeDao.getBudget()

    override suspend fun setMonthlyBudget(monthlyLimitPaise: Long) {
        financeDao.upsertBudget(FinanceBudgetEntity(id = 1L, monthlyLimitPaise = monthlyLimitPaise))
    }

    override fun getSavingsGoal(): Flow<FinanceSavingsGoalEntity?> =
        financeDao.getSavingsGoal()

    override suspend fun setSavingsGoal(
        title: String,
        targetAmountPaise: Long,
        currentAmountPaise: Long,
        targetDate: Long?
    ) {
        financeDao.upsertSavingsGoal(
            FinanceSavingsGoalEntity(
                id = 1L,
                title = title,
                targetAmountPaise = targetAmountPaise,
                currentAmountPaise = currentAmountPaise,
                targetDate = targetDate
            )
        )
    }

    override suspend fun updateSavingsGoalProgress(currentAmountPaise: Long) {
        val currentGoal = financeDao.getSavingsGoal()
        // Handled through upsert
    }

    override suspend fun deleteSavingsGoal() {
        financeDao.deleteSavingsGoal()
    }

    override fun getDashboardSummary(): Flow<FinanceDashboardSummary> {
        return combine(
            financeDao.getAllTransactions(),
            financeDao.getAllAccounts(),
            financeDao.getBudget(),
            financeDao.getSavingsGoal()
        ) { transactions, accounts, budget, goal ->
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH)

            cal.set(currentYear, currentMonth, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfMonth = cal.timeInMillis

            val cashAccount = accounts.find { it.id == "cash" }
            val bankAccount = accounts.find { it.id == "bank" }

            var cashPaise = cashAccount?.initialBalancePaise ?: 0L
            var bankPaise = bankAccount?.initialBalancePaise ?: 0L

            var monthlyIncomePaise = 0L
            var monthlyExpensePaise = 0L
            var monthlyUpiSpentPaise = 0L

            for (tx in transactions) {
                val isCurrentMonth = tx.timestamp in startOfMonth..endOfMonth

                when (tx.type) {
                    FinanceTransactionType.INCOME.name -> {
                        // Note: If account is bank or paymentMode is UPI/BANK_TRANSFER, it affects bank
                        if (tx.accountId == "cash") {
                            cashPaise += tx.amountPaise
                        } else {
                            bankPaise += tx.amountPaise
                        }
                        if (isCurrentMonth) {
                            monthlyIncomePaise += tx.amountPaise
                        }
                    }
                    FinanceTransactionType.EXPENSE.name -> {
                        // Note: If paymentMode is UPI, it debits from Bank account!
                        // This strictly prevents double-counting bank-linked UPI transactions.
                        if (tx.accountId == "cash") {
                            cashPaise -= tx.amountPaise
                        } else {
                            bankPaise -= tx.amountPaise
                        }
                        if (isCurrentMonth) {
                            monthlyExpensePaise += tx.amountPaise
                            if (tx.paymentMode == FinancePaymentMode.UPI.name) {
                                monthlyUpiSpentPaise += tx.amountPaise
                            }
                        }
                    }
                    FinanceTransactionType.TRANSFER.name -> {
                        // Internal transfer: e.g. ATM withdrawal Bank -> Cash
                        val fromAccount = tx.accountId
                        val toAccount = tx.toAccountId ?: if (fromAccount == "bank") "cash" else "bank"

                        if (fromAccount == "bank") {
                            bankPaise -= tx.amountPaise
                        } else {
                            cashPaise -= tx.amountPaise
                        }

                        if (toAccount == "cash") {
                            cashPaise += tx.amountPaise
                        } else {
                            bankPaise += tx.amountPaise
                        }
                    }
                }
            }

            val totalBalance = cashPaise + bankPaise
            val monthlySavings = monthlyIncomePaise - monthlyExpensePaise
            val budgetLimit = budget?.monthlyLimitPaise
            val budgetRemaining = budgetLimit?.let { it - monthlyExpensePaise }

            FinanceDashboardSummary(
                totalBalancePaise = totalBalance,
                bankBalancePaise = bankPaise,
                cashBalancePaise = cashPaise,
                totalUpiSpentMonthPaise = monthlyUpiSpentPaise,
                monthlyIncomePaise = monthlyIncomePaise,
                monthlyExpensePaise = monthlyExpensePaise,
                monthlySavingsPaise = monthlySavings,
                monthlyBudgetLimitPaise = budgetLimit,
                monthlyBudgetRemainingPaise = budgetRemaining,
                savingsGoal = goal
            )
        }
    }

    override fun getMonthlyHistory(): Flow<List<MonthlyFinanceHistory>> {
        return financeDao.getAllTransactions().combine(financeDao.getAllAccounts()) { transactions, _ ->
            val calendar = Calendar.getInstance()
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )

            // Group by year-month key
            val grouped = transactions.groupBy { tx ->
                calendar.timeInMillis = tx.timestamp
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val key = String.format("%04d-%02d", year, month + 1)
                val label = "${monthNames[month]} $year"
                key to label
            }

            grouped.entries.map { (keyPair, txList) ->
                val (yearMonthKey, yearMonthLabel) = keyPair
                var income = 0L
                var expense = 0L
                for (tx in txList) {
                    when (tx.type) {
                        FinanceTransactionType.INCOME.name -> income += tx.amountPaise
                        FinanceTransactionType.EXPENSE.name -> expense += tx.amountPaise
                    }
                }
                MonthlyFinanceHistory(
                    yearMonth = yearMonthLabel,
                    yearMonthKey = yearMonthKey,
                    totalIncomePaise = income,
                    totalExpensePaise = expense,
                    netSavingsPaise = income - expense,
                    transactions = txList.sortedByDescending { it.timestamp }
                )
            }.sortedByDescending { it.yearMonthKey }
        }
    }
}

