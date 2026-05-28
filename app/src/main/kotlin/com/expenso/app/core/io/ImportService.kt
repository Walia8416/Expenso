package com.expenso.app.core.io

import android.content.Context
import android.net.Uri
import com.expenso.app.core.data.repository.CategoryRepository
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.data.repository.IncomeRepository
import com.expenso.app.core.domain.model.PaymentMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class ImportKind { EXPENSE, INCOME }

data class ImportPreview(
    val kind: ImportKind,
    val expenses: List<ParsedExpense> = emptyList(),
    val income: List<ParsedIncome> = emptyList(),
    val skipped: List<SkippedRow> = emptyList(),
)

data class ParsedExpense(
    val purchaseDateMs: Long,
    val item: String?,
    val amountMinor: Long,
    val categoryName: String,
    val paymentMethod: PaymentMethod,
    val note: String?,
    val timestampMs: Long,
)

data class ParsedIncome(
    val dateMs: Long,
    val source: String,
    val description: String?,
    val amountMinor: Long,
    val timestampMs: Long,
)

data class SkippedRow(
    val file: String,
    val rowNumber: Int,
    val reason: String,
)

data class ImportResult(
    val expensesAdded: Int,
    val incomeAdded: Int,
    val skipped: Int,
)

@Singleton
class ImportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val tsFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
        DateTimeFormatter.ofPattern("M/d/yyyy H:mm"),
    )
    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
    )

    suspend fun previewExpenses(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val expenses = mutableListOf<ParsedExpense>()
        val skipped = mutableListOf<SkippedRow>()
        readCsv(uri) { header, row, rowNum ->
            val parsed = parseExpenseRow(header, row, rowNum)
            if (parsed != null) expenses.add(parsed) else skipped.add(lastSkip(rowNum, "expenses.csv"))
        }
        ImportPreview(ImportKind.EXPENSE, expenses = expenses, skipped = skipped)
    }

    suspend fun previewIncome(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val income = mutableListOf<ParsedIncome>()
        val skipped = mutableListOf<SkippedRow>()
        readCsv(uri) { header, row, rowNum ->
            val parsed = parseIncomeRow(header, row, rowNum)
            if (parsed != null) income.add(parsed) else skipped.add(lastSkip(rowNum, "income.csv"))
        }
        ImportPreview(ImportKind.INCOME, income = income, skipped = skipped)
    }

    suspend fun commit(preview: ImportPreview): ImportResult = withContext(Dispatchers.IO) {
        val categories = categoryRepository.observeAll().first()
        val catByName = categories.associateBy { it.name.lowercase() }
        var expCount = 0
        var incCount = 0
        var skippedExtra = 0

        preview.expenses.forEach { pe ->
            val cat = catByName[pe.categoryName.lowercase()]
                ?: catByName["other"]
                ?: categories.firstOrNull()
            if (cat == null) {
                skippedExtra++
                return@forEach
            }
            expenseRepository.insertImported(
                amountMinor = pe.amountMinor,
                categoryId = cat.id,
                merchantName = pe.item,
                note = pe.note,
                paymentMethod = pe.paymentMethod,
                createdAt = pe.purchaseDateMs,
                completedAt = pe.timestampMs,
            )
            expCount++
        }

        preview.income.forEach { pi ->
            incomeRepository.add(
                amountMinor = pi.amountMinor,
                source = pi.source,
                description = pi.description,
                note = null,
                createdAt = pi.dateMs,
            )
            incCount++
        }

        ImportResult(
            expensesAdded = expCount,
            incomeAdded = incCount,
            skipped = preview.skipped.size + skippedExtra,
        )
    }

    // --- CSV parsing helpers ---

    private var lastSkipReason: String = ""
    private fun lastSkip(rowNum: Int, file: String) = SkippedRow(file, rowNum, lastSkipReason)

    private inline fun readCsv(
        uri: Uri,
        onRow: (header: Map<String, Int>, row: List<String>, rowNumber: Int) -> Unit,
    ) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                val headerLine = reader.readLine() ?: return
                val headers = splitCsv(headerLine).mapIndexed { i, h -> h.trim() to i }.toMap()
                var rowNum = 1
                while (true) {
                    val line = reader.readLine() ?: break
                    rowNum++
                    if (line.isBlank()) continue
                    val cells = splitCsv(line)
                    onRow(headers, cells, rowNum)
                }
            }
        } ?: error("Unable to open file")
    }

    private fun parseExpenseRow(
        header: Map<String, Int>,
        row: List<String>,
        rowNum: Int,
    ): ParsedExpense? {
        val iTs = header["Timestamp"]
        val iPurchase = header["Purchase Date"] ?: header["Date"]
        val iItem = header["Item"]
        val iAmount = header["Amount"]
        val iCategory = header["Category"]
        val iMethod = header["Payment Method"]
        val iNote = header["Note"]

        val amount = iAmount?.let { numAt(row, it) }
        val category = iCategory?.let { strAt(row, it) }.orEmpty()
        val purchase = iPurchase?.let { dateAt(row, it) }

        if (amount == null || amount <= 0.0) {
            lastSkipReason = "Missing or invalid amount"
            return null
        }
        if (category.isBlank()) {
            lastSkipReason = "Missing category"
            return null
        }
        if (purchase == null) {
            lastSkipReason = "Missing purchase date"
            return null
        }
        val item = iItem?.let { strAt(row, it) }?.takeIf { it.isNotBlank() }
        val method = when (iMethod?.let { strAt(row, it) }?.trim()?.lowercase()) {
            "cash" -> PaymentMethod.CASH
            "card" -> PaymentMethod.CARD
            "other" -> PaymentMethod.OTHER
            "upi", null, "" -> PaymentMethod.UPI
            else -> PaymentMethod.UPI
        }
        val note = iNote?.let { strAt(row, it) }?.takeIf { it.isNotBlank() }
        val ts = iTs?.let { tsAt(row, it) } ?: purchase
        val minor = BigDecimal.valueOf(amount).multiply(BigDecimal(100)).toLong()
        return ParsedExpense(
            purchaseDateMs = purchase,
            item = item,
            amountMinor = minor,
            categoryName = category,
            paymentMethod = method,
            note = note,
            timestampMs = ts,
        )
    }

    private fun parseIncomeRow(
        header: Map<String, Int>,
        row: List<String>,
        rowNum: Int,
    ): ParsedIncome? {
        val iTs = header["Timestamp"]
        val iDate = header["Date"] ?: header["Purchase Date"]
        val iSource = header["Income Source"] ?: header["Source"]
        val iDescription = header["Description/Invoice No."] ?: header["Description"]
        val iAmount = header["Income Amount"] ?: header["Amount"]

        val amount = iAmount?.let { numAt(row, it) }
        val source = iSource?.let { strAt(row, it) }.orEmpty()
        val date = iDate?.let { dateAt(row, it) }

        if (amount == null || amount <= 0.0) {
            lastSkipReason = "Missing or invalid amount"
            return null
        }
        if (source.isBlank()) {
            lastSkipReason = "Missing income source"
            return null
        }
        if (date == null) {
            lastSkipReason = "Missing date"
            return null
        }
        val description = iDescription?.let { strAt(row, it) }?.takeIf { it.isNotBlank() }
        val ts = iTs?.let { tsAt(row, it) } ?: date
        val minor = BigDecimal.valueOf(amount).multiply(BigDecimal(100)).toLong()
        return ParsedIncome(
            dateMs = date,
            source = source,
            description = description,
            amountMinor = minor,
            timestampMs = ts,
        )
    }

    private fun strAt(row: List<String>, col: Int): String? =
        row.getOrNull(col)?.trim()

    private fun numAt(row: List<String>, col: Int): Double? =
        row.getOrNull(col)?.trim()?.replace(",", "")?.toDoubleOrNull()

    private fun dateAt(row: List<String>, col: Int): Long? {
        val raw = row.getOrNull(col)?.trim().orEmpty()
        if (raw.isBlank()) return null
        // Try date-only formats first, then fall back to timestamps.
        for (fmt in dateFormatters) {
            runCatching {
                return LocalDate.parse(raw, fmt).atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
        return tsAtValue(raw)
    }

    private fun tsAt(row: List<String>, col: Int): Long? {
        val raw = row.getOrNull(col)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return tsAtValue(raw)
    }

    private fun tsAtValue(raw: String): Long? {
        for (fmt in tsFormatters) {
            runCatching {
                return LocalDateTime.parse(raw, fmt).atZone(zone).toInstant().toEpochMilli()
            }
        }
        // Fall back: try dateFormatters then treat as start of day
        for (fmt in dateFormatters) {
            runCatching {
                return LocalDate.parse(raw, fmt).atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    /**
     * Minimal RFC-4180 CSV splitter: supports quoted fields and escaped
     * double quotes ("").
     */
    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            sb.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else sb.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    out += sb.toString()
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out
    }
}
