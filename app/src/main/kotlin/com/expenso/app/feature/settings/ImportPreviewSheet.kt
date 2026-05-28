package com.expenso.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenso.app.core.io.ImportKind
import com.expenso.app.core.io.ImportPreview
import com.expenso.app.core.ui.components.formatInr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheet(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            val title = when (preview.kind) {
                ImportKind.EXPENSE -> "Expense import preview"
                ImportKind.INCOME -> "Income import preview"
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Review the data below before adding it to Expenso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            val rowsLabel = when (preview.kind) {
                ImportKind.EXPENSE -> "Expenses"
                ImportKind.INCOME -> "Income"
            }
            val rowsCount = when (preview.kind) {
                ImportKind.EXPENSE -> preview.expenses.size
                ImportKind.INCOME -> preview.income.size
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Summary(rowsLabel, rowsCount.toString(), Modifier.weight(1f))
                Summary(
                    "Skipped",
                    preview.skipped.size.toString(),
                    Modifier.weight(1f),
                    accent = if (preview.skipped.isEmpty()) Color.Unspecified
                        else MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("What's coming in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(preview.expenses.take(50)) { e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            e.item ?: e.categoryName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            formatInr(e.amountMinor),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                items(preview.income.take(50)) { inc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x2214B886), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            inc.source + (inc.description?.let { " · $it" } ?: ""),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "+${formatInr(inc.amountMinor)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF14B886),
                        )
                    }
                }
                if (preview.skipped.isNotEmpty()) {
                    items(preview.skipped.take(20)) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                        ) {
                            Text(
                                "${s.file} row ${s.rowNumber}: ${s.reason}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    val n = preview.expenses.size + preview.income.size
                    Text(if (n > 0) "Import $n" else "Import")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Summary(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Color.Unspecified,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (accent != Color.Unspecified) accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
