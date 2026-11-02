package com.personal.daymark.core.finance

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * High-precision helper for INR (₹) monetary calculations.
 * All monetary amounts are held in integer paise (1 Rupee = 100 paise)
 * to prevent IEEE 754 floating point arithmetic issues.
 */
object FinanceCurrencyHelper {

    /**
     * Parse a user-entered rupee string (e.g. "500", "500.50", "1,200.75") to paise (Long).
     */
    fun parseRupeesToPaise(input: String): Long? {
        val sanitized = input.trim().replace(",", "").replace("₹", "").trim()
        if (sanitized.isEmpty()) return null
        return try {
            val parts = sanitized.split(".")
            when (parts.size) {
                1 -> {
                    val rupees = parts[0].toLongOrNull() ?: return null
                    rupees * 100L
                }
                2 -> {
                    val rupees = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
                    val paiseStr = parts[1].padEnd(2, '0').take(2)
                    val paise = paiseStr.toLongOrNull() ?: return null
                    (rupees * 100L) + paise
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Convert paise to a standard decimal string (e.g. 50050 -> "500.50").
     */
    fun paiseToRupeeString(paise: Long): String {
        val rupees = paise / 100L
        val remainingPaise = abs(paise % 100L)
        return if (remainingPaise == 0L) {
            "$rupees"
        } else {
            String.format(Locale.US, "%d.%02d", rupees, remainingPaise)
        }
    }

    /**
     * Format paise as Indian Rupee currency (e.g. 15000000 -> "₹1,50,000", 2550 -> "₹25.50").
     */
    fun formatInr(paise: Long, forceDecimals: Boolean = false): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100L
        val remPaise = absPaise % 100L

        val formattedRupees = formatIndianGrouping(rupees)
        val sign = if (isNegative) "-" else ""

        return if (remPaise != 0L || forceDecimals) {
            val decimalPart = String.format(Locale.US, "%02d", remPaise)
            "$sign₹$formattedRupees.$decimalPart"
        } else {
            "$sign₹$formattedRupees"
        }
    }

    /**
     * Applies Indian numerical grouping (e.g., 12,34,567).
     */
    private fun formatIndianGrouping(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val last3 = str.takeLast(3)
        var remaining = str.dropLast(3)
        val chunks = mutableListOf<String>()
        while (remaining.length > 2) {
            chunks.add(remaining.takeLast(2))
            remaining = remaining.dropLast(2)
        }
        if (remaining.isNotEmpty()) {
            chunks.add(remaining)
        }
        chunks.reverse()
        chunks.add(last3)
        return chunks.joinToString(",")
    }
}

