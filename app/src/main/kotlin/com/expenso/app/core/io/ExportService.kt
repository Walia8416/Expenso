package com.expenso.app.core.io

import android.content.Context
import android.net.Uri
import com.expenso.app.core.data.repository.ExpenseRepository
import com.expenso.app.core.data.repository.IncomeRepository
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.Income
import com.expenso.app.core.domain.model.PaymentStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * CSV exporters for expenses and income. The app writes the exact same
 * column headers that [ImportService] expects so round-tripping works.
 */
@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val tsFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun exportExpenses(uri: Uri): Int = withContext(Dispatchers.IO) {
        val expenses = expenseRepository.observeAll().first()
            .filter { it.status != PaymentStatus.CANCELLED && it.status != PaymentStatus.FAILED }
            .sortedByDescending { it.createdAt }

        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
            OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                writer.appendLine("Timestamp,Purchase Date,Item,Amount,Category,Payment Method,Note")
                expenses.forEach { e -> writer.appendLine(expenseRow(e)) }
            }
        } ?: error("Unable to open destination")

        expenses.size
    }

    suspend fun exportIncome(uri: Uri): Int = withContext(Dispatchers.IO) {
        val incomes = incomeRepository.observeAll().first()
            .sortedByDescending { it.createdAt }

        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
            OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                writer.appendLine("Timestamp,Date,Income Source,Description/Invoice No.,Income Amount")
                incomes.forEach { i -> writer.appendLine(incomeRow(i)) }
            }
        } ?: error("Unable to open destination")

        incomes.size
    }

    private fun expenseRow(e: Expense): String {
        val ts = formatTs(e.completedAt ?: e.createdAt)
        val date = formatDate(e.createdAt)
        val item = e.merchantName
            ?: e.payee?.displayName
            ?: e.note
            ?: e.category.name
        return listOf(
            ts,
            date,
            item,
            "%.2f".format(e.amountMinor / 100.0),
            e.category.name,
            e.paymentMethod.displayName,
            e.note.orEmpty(),
        ).joinToString(",") { escape(it) }
    }

    private fun incomeRow(i: Income): String {
        val ts = formatTs(i.recordedAt)
        val date = formatDate(i.createdAt)
        return listOf(
            ts,
            date,
            i.source,
            i.description.orEmpty(),
            "%.2f".format(i.amountMinor / 100.0),
        ).joinToString(",") { escape(it) }
    }

    private fun formatTs(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDateTime().format(tsFmt)

    private fun formatDate(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().format(dateFmt)

    private fun escape(raw: String): String {
        val needsQuote = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        val body = raw.replace("\"", "\"\"")
        return if (needsQuote) "\"$body\"" else body
    }
}
