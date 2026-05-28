package com.expenso.app.core.ui.components

import java.text.NumberFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

private val INR: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}

fun formatInr(amountMinor: Long): String {
    val rupees = amountMinor / 100.0
    return INR.format(rupees)
}

fun formatInrPlain(amountMinor: Long): String {
    val rupees = amountMinor / 100.0
    return "₹${"%,.2f".format(rupees).trimEnd('0').trimEnd('.')}"
}

/**
 * Compact Indian-grouping currency used on crowded Insights tiles/labels.
 *
 * Thresholds (on the rupee value, sign-preserving):
 *  - < ₹1,00,000 (one lakh)  → full formatted value, e.g. ₹99,999 / ₹8,450
 *  - < ₹1,00,00,000 (crore)  → ₹X.XXL (lakhs), trailing zeros dropped
 *  - ≥ ₹1,00,00,000          → ₹X.XXCr (crores), trailing zeros dropped
 *
 * We truncate (not round) so values like 1.089L render as 1.08L.
 */
fun formatInrCompact(amountMinor: Long): String {
    val rupees = amountMinor / 100.0
    val sign = if (rupees < 0) "-" else ""
    val v = abs(rupees)
    return when {
        v < 100_000.0 -> INR.format(rupees)
        v < 1_00_00_000.0 -> "${sign}₹${trimDec(v / 1_00_000.0)}L"
        else -> "${sign}₹${trimDec(v / 1_00_00_000.0)}Cr"
    }
}

private fun trimDec(value: Double): String {
    return BigDecimal.valueOf(value)
        .setScale(2, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
}
