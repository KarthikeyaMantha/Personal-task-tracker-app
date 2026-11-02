package com.personal.daymark.core.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceCurrencyHelperTest {

    @Test
    fun parseRupeesToPaise_handlesVariousFormats() {
        assertEquals(50000L, FinanceCurrencyHelper.parseRupeesToPaise("500"))
        assertEquals(50050L, FinanceCurrencyHelper.parseRupeesToPaise("500.50"))
        assertEquals(50005L, FinanceCurrencyHelper.parseRupeesToPaise("500.05"))
        assertEquals(50050L, FinanceCurrencyHelper.parseRupeesToPaise("500.5"))
        assertEquals(120075L, FinanceCurrencyHelper.parseRupeesToPaise("1,200.75"))
        assertEquals(120075L, FinanceCurrencyHelper.parseRupeesToPaise("₹ 1,200.75"))
        assertEquals(10000000L, FinanceCurrencyHelper.parseRupeesToPaise("1,00,000"))
        assertEquals(0L, FinanceCurrencyHelper.parseRupeesToPaise("0"))
        assertEquals(75L, FinanceCurrencyHelper.parseRupeesToPaise("0.75"))
        assertNull(FinanceCurrencyHelper.parseRupeesToPaise(""))
        assertNull(FinanceCurrencyHelper.parseRupeesToPaise("abc"))
    }

    @Test
    fun paiseToRupeeString_convertsCorrectly() {
        assertEquals("500", FinanceCurrencyHelper.paiseToRupeeString(50000L))
        assertEquals("500.50", FinanceCurrencyHelper.paiseToRupeeString(50050L))
        assertEquals("500.05", FinanceCurrencyHelper.paiseToRupeeString(50005L))
        assertEquals("0", FinanceCurrencyHelper.paiseToRupeeString(0L))
        assertEquals("0.75", FinanceCurrencyHelper.paiseToRupeeString(75L))
    }

    @Test
    fun formatInr_formatsWithIndianGrouping() {
        assertEquals("₹500", FinanceCurrencyHelper.formatInr(50000L))
        assertEquals("₹500.50", FinanceCurrencyHelper.formatInr(50050L))
        assertEquals("₹1,50,000", FinanceCurrencyHelper.formatInr(15000000L))
        assertEquals("₹12,34,567.50", FinanceCurrencyHelper.formatInr(123456750L))
        assertEquals("-₹500", FinanceCurrencyHelper.formatInr(-50000L))
        assertEquals("-₹1,250.25", FinanceCurrencyHelper.formatInr(-125025L))
        assertEquals("₹0", FinanceCurrencyHelper.formatInr(0L))
    }
}

