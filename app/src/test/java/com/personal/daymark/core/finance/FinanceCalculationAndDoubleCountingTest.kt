package com.personal.daymark.core.finance

import com.personal.daymark.data.local.dao.FinanceDao
import com.personal.daymark.data.local.entity.FinanceAccountEntity
import com.personal.daymark.data.local.entity.FinanceBudgetEntity
import com.personal.daymark.data.local.entity.FinancePaymentMode
import com.personal.daymark.data.local.entity.FinanceSavingsGoalEntity
import com.personal.daymark.data.local.entity.FinanceTransactionEntity
import com.personal.daymark.data.local.entity.FinanceTransactionType
import com.personal.daymark.data.repository.FinanceRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class FinanceCalculationAndDoubleCountingTest {

    private class FakeFinanceDao(
        private val accounts: MutableList<FinanceAccountEntity> = mutableListOf(),
        private val transactions: MutableList<FinanceTransactionEntity> = mutableListOf(),
        private var budget: FinanceBudgetEntity? = null,
        private var goal: FinanceSavingsGoalEntity? = null
    ) : FinanceDao {
        override fun getAllTransactions() = flowOf(transactions.toList())
        override fun getTransactionsBetween(startTime: Long, endTime: Long) =
            flowOf(transactions.filter { it.timestamp in startTime..endTime })
        override suspend fun getTransactionById(id: Long) = transactions.find { it.id == id }
        override suspend fun insertTransaction(tx: FinanceTransactionEntity): Long {
            val id = (transactions.maxOfOrNull { it.id } ?: 0L) + 1L
            transactions.add(tx.copy(id = id))
            return id
        }
        override suspend fun updateTransaction(tx: FinanceTransactionEntity) {
            val idx = transactions.indexOfFirst { it.id == tx.id }
            if (idx >= 0) transactions[idx] = tx
        }
        override suspend fun deleteTransaction(id: Long) {
            transactions.removeAll { it.id == id }
        }
        override fun getAllAccounts() = flowOf(accounts.toList())
        override suspend fun getAccountById(id: String) = accounts.find { it.id == id }
        override suspend fun upsertAccount(account: FinanceAccountEntity) {
            val idx = accounts.indexOfFirst { it.id == account.id }
            if (idx >= 0) accounts[idx] = account else accounts.add(account)
        }
        override fun getBudget() = flowOf(budget)
        override suspend fun upsertBudget(budget: FinanceBudgetEntity) {
            this.budget = budget
        }
        override fun getSavingsGoal() = flowOf(goal)
        override suspend fun upsertSavingsGoal(goal: FinanceSavingsGoalEntity) {
            this.goal = goal
        }
        override suspend fun deleteSavingsGoal() {
            this.goal = null
        }
    }

    @Test
    fun upiTransaction_debitsBankAndPreventsDoubleCounting() = runBlocking {
        // Initial setup: Bank has ₹10,000, Cash has ₹2,000. Total = ₹12,000
        val fakeDao = FakeFinanceDao(
            accounts = mutableListOf(
                FinanceAccountEntity(id = "bank", name = "Bank Account", initialBalancePaise = 1000000L),
                FinanceAccountEntity(id = "cash", name = "Cash", initialBalancePaise = 200000L)
            )
        )
        val repository = FinanceRepositoryImpl(fakeDao)

        // Add a ₹1,500 UPI expense (which debits from Bank)
        val now = System.currentTimeMillis()
        repository.addTransaction(
            FinanceTransactionEntity(
                type = FinanceTransactionType.EXPENSE.name,
                amountPaise = 150000L,
                timestamp = now,
                category = "Food",
                accountId = "bank",
                paymentMode = FinancePaymentMode.UPI.name,
                notes = "Swiggy dinner via UPI"
            )
        )

        val summary = repository.getDashboardSummary().first()

        // Bank should be ₹10,000 - ₹1,500 = ₹8,500
        assertEquals(850000L, summary.bankBalancePaise)
        // Cash remains ₹2,000
        assertEquals(200000L, summary.cashBalancePaise)
        // Total balance should be ₹10,500 (NOT double-deducted)
        assertEquals(1050000L, summary.totalBalancePaise)
        // UPI spent this month is tracked transparently
        assertEquals(150000L, summary.totalUpiSpentMonthPaise)
        // Monthly expense
        assertEquals(150000L, summary.monthlyExpensePaise)
    }

    @Test
    fun transfer_bankToCash_preservesTotalNetWorth() = runBlocking {
        // Bank: ₹50,000, Cash: ₹5,000. Total = ₹55,000
        val fakeDao = FakeFinanceDao(
            accounts = mutableListOf(
                FinanceAccountEntity(id = "bank", name = "Bank Account", initialBalancePaise = 5000000L),
                FinanceAccountEntity(id = "cash", name = "Cash", initialBalancePaise = 500000L)
            )
        )
        val repository = FinanceRepositoryImpl(fakeDao)

        // Withdraw ₹10,000 from ATM (Bank -> Cash)
        val now = System.currentTimeMillis()
        repository.addTransaction(
            FinanceTransactionEntity(
                type = FinanceTransactionType.TRANSFER.name,
                amountPaise = 1000000L,
                timestamp = now,
                category = "Transfer",
                accountId = "bank",
                toAccountId = "cash",
                paymentMode = FinancePaymentMode.BANK_TRANSFER.name,
                notes = "ATM withdrawal"
            )
        )

        val summary = repository.getDashboardSummary().first()

        // Bank decreased by ₹10,000 -> ₹40,000
        assertEquals(4000000L, summary.bankBalancePaise)
        // Cash increased by ₹10,000 -> ₹15,000
        assertEquals(1500000L, summary.cashBalancePaise)
        // Total balance remains unchanged at ₹55,000
        assertEquals(5500000L, summary.totalBalancePaise)
        // Transfer does not count as monthly expense or income
        assertEquals(0L, summary.monthlyExpensePaise)
        assertEquals(0L, summary.monthlyIncomePaise)
    }

    @Test
    fun monthlyBudgetAndSavingsGoal_calculateAccurately() = runBlocking {
        val fakeDao = FakeFinanceDao(
            accounts = mutableListOf(
                FinanceAccountEntity(id = "bank", name = "Bank Account", initialBalancePaise = 10000000L),
                FinanceAccountEntity(id = "cash", name = "Cash", initialBalancePaise = 500000L)
            )
        )
        val repository = FinanceRepositoryImpl(fakeDao)

        // Set monthly budget of ₹30,000
        repository.setMonthlyBudget(3000000L)

        // Set savings goal: "Emergency Fund", Target ₹1,00,000, currently saved ₹45,000
        repository.setSavingsGoal(
            title = "Emergency Fund",
            targetAmountPaise = 10000000L,
            currentAmountPaise = 4500000L,
            targetDate = null
        )

        // Add expense of ₹12,000
        val now = System.currentTimeMillis()
        repository.addTransaction(
            FinanceTransactionEntity(
                type = FinanceTransactionType.EXPENSE.name,
                amountPaise = 1200000L,
                timestamp = now,
                category = "Bills",
                accountId = "bank",
                paymentMode = FinancePaymentMode.UPI.name
            )
        )

        val summary = repository.getDashboardSummary().first()

        // Budget remaining = ₹30,000 - ₹12,000 = ₹18,000
        assertEquals(3000000L, summary.monthlyBudgetLimitPaise)
        assertEquals(1800000L, summary.monthlyBudgetRemainingPaise)

        // Goal verification
        assertNotNull(summary.savingsGoal)
        assertEquals("Emergency Fund", summary.savingsGoal?.title)
        assertEquals(10000000L, summary.savingsGoal?.targetAmountPaise)
        assertEquals(4500000L, summary.savingsGoal?.currentAmountPaise)
    }
}

