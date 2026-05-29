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
 * - For merchant QRs with `sign` / `orgid`, every signed byte of the URI must be
 *   preserved verbatim or the signature verification at the PSP fails. Decoding
 *   the URI into a param map and rebuilding via [Uri.Builder] re-encodes
 *   characters (`+`→`%20`, hex casing, reserved-char policy) and breaks the
 *   signature. For signed QRs, always launch the raw source URI; if the user
 *   needs to add an amount, use [appendAmountToRawUri] which does raw string
 *   concat to keep every signed byte intact.
 */
object UpiIntentBuilder {

    /** Source of a scanned QR — passed as the Android 13 system QR-scanner extra. */
    enum class QrSource { LIVE_CAMERA, STATIC_IMAGE }

    /** Intent extra key emitted by the Android 13 system QR scanner. */
    const val EXTRA_UPI_QR_SOURCE = "com.google.android.gms.UPI_QR_SOURCE"

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
        // DEPRECATED: signed merchant QRs must launch their original
        // sourceRawUri verbatim. Use [appendAmountToRawUri] for
        // variable-amount signed QRs.
        preserveSignedPayload: Boolean = false,
    ): Uri {
        val b = Uri.Builder()
            .scheme("upi")
            .authority("pay")

        if (preserveSignedPayload) {
            // Legacy path. Decoding then re-emitting via Uri.Builder re-encodes
            // characters and breaks merchant signatures. Kept only for callers
            // not yet migrated; prefer raw-string concat via appendAmountToRawUri.
            for ((k, v) in extraParams) {
                if (k.isBlank()) continue
                b.appendQueryParameter(k, v)
            }
            if (amountRupees != null && !extraParams.containsKey("am")) {
                b.appendQueryParameter(
                    "am",
                    amountRupees.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                )
            }
            return b.build()
        }

        // Manual query-string concat (not Uri.Builder):
        //   - `pa` is NEVER URL-encoded. Some UPI apps (older PhonePe, BHIM
        //     forks) misparse `%40` as `@` after a stray space, mangling VPA.
        //   - Other fields use Android's [Uri.encode] (`%XX` form, not `+`).
        //   - `mode` is intentionally NOT emitted for P2P flows. Earlier
        //     versions appended `mode=00`, which is not a defined NPCI value;
        //     GPay (and a few strict PSP gateways) treat it as merchant-collect
        //     and apply much lower per-day cumulative caps — surfaces in GPay
        //     as "You've reached your bank limit" at small amounts. Modern
        //     UPI apps default to P2P when mode is absent. Only forward `mode`
        //     when the original QR / caller supplied one (handled below as a
        //     regular extra param).
        val parts = mutableListOf<String>()
        parts += "pa=${normalizeBankHandle(payeeVpa)}"
        if (!payeeName.isNullOrBlank()) {
            parts += "pn=${Uri.encode(payeeName)}"
        }
        if (amountRupees != null) {
            parts += "am=${amountRupees.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
        }
        parts += "cu=${Uri.encode(currency)}"
        if (!note.isNullOrBlank()) {
            parts += "tn=${Uri.encode(sanitizeNote(note))}"
        }
        val safeTr = sanitizeTxnRef(transactionRef)
        parts += "tr=${Uri.encode(safeTr)}"

        val reserved = setOf("pa", "pn", "am", "cu", "tn", "tr")
        for ((k, v) in extraParams) {
            if (k in reserved) continue
            if (k.isBlank() || v.isBlank()) continue
            parts += "${Uri.encode(k)}=${Uri.encode(v)}"
        }

        return Uri.parse("upi://pay?" + parts.joinToString("&"))
    }

    /**
     * Raw-string concat helper for variable-amount signed merchant QRs.
     *
     * Signed QRs include a `sign` parameter computed over the URI bytes. Any
     * decode→re-encode roundtrip via [Uri.Builder] mutates bytes (`+`→`%20`,
     * hex casing, reserved-char policy) and breaks signature verification at
     * the PSP — surfaces in GPay as "Invalid UPI ID" after PIN entry. This
     * helper appends `&am=NN.NN` (and `&cu=INR` if missing) directly onto the
     * source string so every signed byte stays intact.
     */
    fun appendAmountToRawUri(
        sourceRawUri: String,
        amountRupees: BigDecimal,
        currency: String = "INR",
    ): String {
        val sep = if (sourceRawUri.contains('?')) "&" else "?"
        val amt = amountRupees.setScale(2, RoundingMode.HALF_UP).toPlainString()
        val withAmount = "$sourceRawUri${sep}am=$amt"
        return if (sourceRawUri.contains("cu=")) withAmount else "$withAmount&cu=$currency"
    }

    fun buildIntent(
        uri: Uri,
        targetPackage: String? = null,
        qrSource: QrSource? = null,
    ): Intent {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!targetPackage.isNullOrBlank()) {
            intent.setPackage(targetPackage)
        }
        if (qrSource != null) {
            // Mirrors Android 13 system QR scanner. Caller package is still
            // com.expenso.app (not com.google.android.gms), so strict PSPs
            // may ignore this — but several UPI apps honor the extra in
            // isolation and switch initiation mode to QR.
            intent.putExtra(EXTRA_UPI_QR_SOURCE, qrSource.name)
        }
        return intent
    }

    /** VPA is case-insensitive per NPCI; lowercase it for non-signed flows. */
    internal fun normalizeBankHandle(vpa: String): String = vpa.lowercase()

    /** Bank-safe transaction reference: keeps [A-Za-z0-9], caps at 35 chars. */
    private fun sanitizeTxnRef(raw: String): String {
        val cleaned = raw.filter { it.isLetterOrDigit() }
        return if (cleaned.length in 1..35) cleaned else cleaned.take(35).ifEmpty { generateTxnRef() }
    }

    /** `tn` is limited to printable ASCII and 50 chars by NPCI spec. */
    private fun sanitizeNote(raw: String): String =
        raw.filter { it.code in 32..126 }.take(50)
}
