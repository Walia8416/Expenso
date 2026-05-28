package com.expenso.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFormatSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "CSV import format",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Expenso imports two separate CSV files — one for expenses and one for income. " +
                    "The first row must be the header row with the exact column names below (order doesn't matter).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Expenses CSV")
            HeaderChips(
                listOf(
                    "Timestamp", "Purchase Date", "Item", "Amount",
                    "Category", "Payment Method", "Note",
                ),
            )
            Spacer(Modifier.height(10.dp))
            CodeBlock(
                """
                Timestamp,Purchase Date,Item,Amount,Category,Payment Method,Note
                2026-01-23 01:04:00,2026-01-05,Flat Rent,15500,Bills,UPI,
                2026-02-03 23:14:52,2026-02-03,Coat Pant Shirts,6245,Shopping,UPI,Winter sale
                """.trimIndent(),
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Income CSV")
            HeaderChips(
                listOf(
                    "Timestamp", "Date", "Income Source", "Description/Invoice No.", "Income Amount",
                ),
            )
            Spacer(Modifier.height(10.dp))
            CodeBlock(
                """
                Timestamp,Date,Income Source,Description/Invoice No.,Income Amount
                2026-01-23 00:56:47,2026-01-01,Hyperverge,Salary,81150
                2026-04-01 19:47:13,2026-04-01,Hyperverge,Health incentive,26670
                """.trimIndent(),
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader("Column rules")
            Bullet("Timestamp  — when you logged the entry. Format: yyyy-MM-dd HH:mm:ss (M/d/yyyy H:mm:ss also works).")
            Bullet("Purchase Date / Date — when the expense or income actually happened. Format: yyyy-MM-dd (M/d/yyyy also works).")
            Bullet("Amount / Income Amount — in rupees, as a number. Commas and currency symbols are not required.")
            Bullet("Category — must match a category name in the app (Groceries, Bills, Rent, Transport, Health, Food, Shopping, Fun, Travel, Fitness, Education, Subscriptions, Transfer, Other). Unknown names fall back to Other.")
            Bullet("Payment Method — one of UPI, Cash, Card, Other. Blank is treated as UPI.")
            Bullet("Note — optional free text. Wrap it in double-quotes if it contains commas.")
            Bullet("Item / Description — optional free text; wrap in double-quotes if it contains commas.")

            Spacer(Modifier.height(20.dp))
            SectionHeader("Tips")
            Bullet("Tip: click \"Export expenses (CSV)\" or \"Export income (CSV)\" once, open the file, and use it as your template.")
            Bullet("Tip: empty or invalid rows are shown in the preview as \"skipped\" — nothing is added until you tap Import.")

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun HeaderChips(columns: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        columns.forEach { col ->
            Text(
                col,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun CodeBlock(content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.width(0.dp))
}
