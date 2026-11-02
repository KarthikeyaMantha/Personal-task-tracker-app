package com.personal.daymark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.daymark.data.local.entity.FinanceAccountEntity
import com.personal.daymark.data.local.entity.FinanceBudgetEntity
import com.personal.daymark.data.local.entity.FinanceSavingsGoalEntity
import com.personal.daymark.data.local.entity.FinanceTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // Transactions
    @Query("SELECT * FROM finance_transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC, id DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): FinanceTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: FinanceTransactionEntity): Long

    @Update
    suspend fun updateTransaction(tx: FinanceTransactionEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    // Accounts
    @Query("SELECT * FROM finance_accounts")
    fun getAllAccounts(): Flow<List<FinanceAccountEntity>>

    @Query("SELECT * FROM finance_accounts WHERE id = :id")
    suspend fun getAccountById(id: String): FinanceAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: FinanceAccountEntity)

    // Budgets
    @Query("SELECT * FROM finance_budgets WHERE id = 1")
    fun getBudget(): Flow<FinanceBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: FinanceBudgetEntity)

    // Savings Goals
    @Query("SELECT * FROM finance_savings_goals WHERE id = 1")
    fun getSavingsGoal(): Flow<FinanceSavingsGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavingsGoal(goal: FinanceSavingsGoalEntity)

    @Query("DELETE FROM finance_savings_goals WHERE id = 1")
    suspend fun deleteSavingsGoal()
}

