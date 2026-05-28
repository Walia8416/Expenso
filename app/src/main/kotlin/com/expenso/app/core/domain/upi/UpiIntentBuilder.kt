package com.expenso.app.core.domain.upi

import android.content.Intent
import android.net.Uri
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/**
 * Builds a `upi://pay?...` URI and Android Intent per NPCI UPI Linking Specification.
 *
 * Important details that commonly cause "Payment failed after UPI PIN":
 * - `tr` (Transaction Reference) MUST be alphanumeric (A-Z, 0-9) — UUIDs with dashes
 *   are rejected by several banks (HDFC, Axis, SBI) at the PSP layer after auth.
 * - For merchant QRs with `sign` / `orgid`, every signed param (including `tr`, `pn`,
 *   `tn`, `mc`, `url`, `mode`) must be preserved exactly as signed or the signature
 *   verification at the PSP fails and the bank declines the transfer after PIN entry.
 */
object UpiIntentBuilder {

    /** Generates a bank-safe `tr` value: alphanumeric, <= 35 chars, unique. */
    fun generateTxnRef(): String {
        val ts = System.currentTimeMillis().toString()
        val chars = ('A'..'Z') + ('0'..'9')
        val rand = buildString { repeat(8) { append(chars[Random.nextInt(chars.size)]) } }
        return ("EXP$ts$rand").take(35)
    }

    fun buildUri(
        payeeVpa: String,
        payeeName: String?,
        amountRupees: BigDecimal?,
        note: String?,
        transactionRef: String,
        currency: String = "INR",
        extraParams: Map<String, String> = emptyMap(),
        preserveSignedPayload: Boolean = false,
    ): Uri {
        val b = Uri.Builder()
            .scheme("upi")
            .authority("pay")

        if (preserveSignedPayload) {
            // Merchant-signed QR: ORDER MATTERS — re-emit every original param in
            // the exact order/values the merchant signed, otherwise the PSP will
            // reject the payment after UPI PIN because the signature won't verify.
            for ((k, v) in extraParams) {
                if (k.isBlank()) continue
                b.appendQueryParameter(k, v)
            }
            // Only layer on our amount if caller explicitly set one that differs
            // (signed QRs already include `am` when fixed).
            if (amountRupees != null && !extraParams.containsKey("am")) {
                b.appendQueryParameter(
                    "am",
                    amountRupees.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                )
            }
            return b.build()
        }

        b.appendQueryParameter("pa", payeeVpa)
        if (!payeeName.isNullOrBlank()) {
            b.appendQueryParameter("pn", payeeName)
        }
        if (amountRupees != null) {
            b.appendQueryParameter("am", amountRupees.setScale(2, RoundingMode.HALF_UP).toPlainString())
        }
        b.appendQueryParameter("cu", currency)
        if (!note.isNullOrBlank()) {
            b.appendQueryParameter("tn", sanitizeNote(note))
        }
        b.appendQueryParameter("tr", sanitizeTxnRef(transactionRef))

        val reserved = setOf("pa", "pn", "am", "cu", "tn", "tr")
        for ((k, v) in extraParams) {
            if (k in reserved) continue
            if (k.isBlank() || v.isBlank()) continue
            b.appendQueryParameter(k, v)
        }

        return b.build()
    }

    fun buildIntent(
        uri: Uri,
        targetPackage: String? = null,
    ): Intent {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!targetPackage.isNullOrBlank()) {
            intent.setPackage(targetPackage)
        }
        return intent
    }

    /** Bank-safe transaction reference: keeps [A-Za-z0-9], caps at 35 chars. */
    private fun sanitizeTxnRef(raw: String): String {
        val cleaned = raw.filter { it.isLetterOrDigit() }
        return if (cleaned.length in 1..35) cleaned else cleaned.take(35).ifEmpty { generateTxnRef() }
    }

    /** `tn` is limited to printable ASCII and 50 chars by NPCI spec. */
    private fun sanitizeNote(raw: String): String =
        raw.filter { it.code in 32..126 }.take(50)
}
