package com.personal.daymark.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.DaymarkDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration8To9Test {

    @Test
    fun migration8To9_createsFinanceTablesAndSeedsAccounts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_8_9_test.db"
        context.deleteDatabase(dbName)

        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Minimal version 8 schema
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Execute Migration 8 -> 9
        DaymarkDatabase.MIGRATION_8_9.migrate(db)

        // 1. Verify finance_accounts created and default accounts seeded
        val accountsCursor = db.query("SELECT id, name, initial_balance_paise FROM finance_accounts ORDER BY id ASC")
        assertTrue(accountsCursor.moveToNext())
        assertEquals("bank", accountsCursor.getString(0))
        assertEquals("Bank Account", accountsCursor.getString(1))
        assertEquals(0L, accountsCursor.getLong(2))

        assertTrue(accountsCursor.moveToNext())
        assertEquals("cash", accountsCursor.getString(0))
        assertEquals("Cash", accountsCursor.getString(1))
        assertEquals(0L, accountsCursor.getLong(2))
        accountsCursor.close()

        // 2. Verify finance_transactions table exists and accepts insertion
        val now = System.currentTimeMillis()
        db.execSQL("""
            INSERT INTO finance_transactions (type, amount_paise, timestamp, category, account_id, payment_mode, notes, created_at)
            VALUES ('EXPENSE', 25000, $now, 'Food', 'bank', 'UPI', 'Lunch', $now)
        """.trimIndent())

        val txCursor = db.query("SELECT category, amount_paise, payment_mode FROM finance_transactions")
        assertTrue(txCursor.moveToFirst())
        assertEquals("Food", txCursor.getString(0))
        assertEquals(25000L, txCursor.getLong(1))
        assertEquals("UPI", txCursor.getString(2))
        txCursor.close()

        // 3. Verify finance_budgets table exists
        db.execSQL("INSERT INTO finance_budgets (id, monthly_limit_paise, updated_at) VALUES (1, 5000000, $now)")
        val budgetCursor = db.query("SELECT monthly_limit_paise FROM finance_budgets WHERE id = 1")
        assertTrue(budgetCursor.moveToFirst())
        assertEquals(5000000L, budgetCursor.getLong(0))
        budgetCursor.close()

        // 4. Verify finance_savings_goals table exists
        db.execSQL("INSERT INTO finance_savings_goals (id, title, target_amount_paise, current_amount_paise, created_at) VALUES (1, 'Trip', 2000000, 500000, $now)")
        val goalCursor = db.query("SELECT title, target_amount_paise, current_amount_paise FROM finance_savings_goals WHERE id = 1")
        assertTrue(goalCursor.moveToFirst())
        assertEquals("Trip", goalCursor.getString(0))
        assertEquals(2000000L, goalCursor.getLong(1))
        assertEquals(500000L, goalCursor.getLong(2))
        goalCursor.close()

        db.close()
    }
}

