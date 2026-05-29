package com.expenso.app.core.domain.upi

import java.math.BigDecimal
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Represents a parsed UPI QR / URI.
 *
 * UPI URIs look like:
 *   upi://pay?pa=merchant@bank&pn=Chai%20Tapri&am=40.00&cu=INR&tn=order123&tr=TXN0001
 *
 * Notes:
 * - `pa` (payee VPA) is the only truly required field for a payment.
 * - `am` is optional — static QRs typically omit it.
 * - `sign`, `orgid`, `mode` appear on signed merchant QRs; we preserve them as-is.
 */
data class UpiPaymentRequest(
    val payeeVpa: String,
    val payeeName: String?,
    val amountRupees: BigDecimal?,
    val currency: String,
    val transactionNote: String?,
    val transactionRef: String?,
    val merchantCode: String?,
    val url: String?,
    val isSigned: Boolean,
    val rawParams: Map<String, String>,
    val sourceRawUri: String? = null,
)

sealed class UpiParseResult {
    data class Success(val request: UpiPaymentRequest) : UpiParseResult()
    data class Failure(val reason: String) : UpiParseResult()
}

object UpiUriParser {

    private const val UPI_SCHEME = "upi"
    private const val UPI_HOST = "pay"

    private val VPA_REGEX = Regex("""^[a-zA-Z0-9._\-]{2,}@[a-zA-Z][a-zA-Z0-9]{1,64}$""")

    fun parse(raw: String?): UpiParseResult {
        if (raw.isNullOrBlank()) return UpiParseResult.Failure("Empty input")

        val trimmed = raw.trim()
        if (!trimmed.startsWith("$UPI_SCHEME://", ignoreCase = true)) {
            return UpiParseResult.Failure("Not a UPI URI")
        }

        val afterScheme = trimmed.removePrefix("$UPI_SCHEME://")
        val (host, query) = splitHostAndQuery(afterScheme)
        if (!host.equals(UPI_HOST, ignoreCase = true)) {
            return UpiParseResult.Failure("Unsupported UPI action: $host")
        }

        val params = parseQueryParams(query)

        val pa = params["pa"]?.trim()
        if (pa.isNullOrEmpty()) {
            return UpiParseResult.Failure("Missing payee VPA (pa)")
        }
        if (!VPA_REGEX.matches(pa)) {
            return UpiParseResult.Failure("Invalid VPA format: $pa")
        }
        // Bank handle (right of `@`) is case-insensitive per NPCI but several
        // PSPs (HDFC/ICICI strict gateways) reject mixed-case handles after
        // PIN entry. Normalize so the parsed VPA is always safe to send to a
        // UPI app. `rawParams` keeps the original-cased value so signed
        // merchant QRs still verify byte-for-byte.
        val normalizedVpa = normalizeBankHandle(pa)

        val amount = params["am"]?.let { runCatching { BigDecimal(it) }.getOrNull() }
        if (amount != null && amount.signum() < 0) {
            return UpiParseResult.Failure("Negative amount not allowed")
        }

        val currency = params["cu"]?.uppercase() ?: "INR"

        return UpiParseResult.Success(
            UpiPaymentRequest(
                payeeVpa = normalizedVpa,
                payeeName = params["pn"]?.sanitize(),
                amountRupees = amount,
                currency = currency,
                transactionNote = params["tn"]?.sanitize(),
                transactionRef = params["tr"],
                merchantCode = params["mc"],
                url = params["url"],
                isSigned = !params["sign"].isNullOrBlank() || !params["orgid"].isNullOrBlank(),
                rawParams = params,
                sourceRawUri = trimmed,
            )
        )
    }

    private fun splitHostAndQuery(input: String): Pair<String, String> {
        val q = input.indexOf('?')
        return if (q < 0) input to "" else input.substring(0, q) to input.substring(q + 1)
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in query.split('&')) {
            if (pair.isBlank()) continue
            val eq = pair.indexOf('=')
            val key = if (eq < 0) pair else pair.substring(0, eq)
            val value = if (eq < 0) "" else pair.substring(eq + 1)
            val decodedKey = key.decodeUrlSafe()
            val decodedValue = value.decodeUrlSafe()
            if (decodedKey.isNotEmpty()) out[decodedKey] = decodedValue
        }
        return out
    }

    private fun String.decodeUrlSafe(): String = try {
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
        this
    }

    private fun String.sanitize(): String {
        return this.replace(Regex("[\\p{Cntrl}]"), "").trim()
    }

    private fun normalizeBankHandle(vpa: String): String {
        // NPCI treats VPA as case-insensitive. Several PSPs (HDFC/ICICI strict
        // gateways) reject mixed-case VPAs after PIN entry, surfacing as
        // "Invalid UPI ID" in GPay. Lowercasing the full VPA is the safest
        // normalization for non-signed paths. Signed-merchant flows preserve
        // the original case via rawParams + verbatim sourceRawUri launch.
        return vpa.lowercase()
    }
}
