package com.expenso.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.domain.model.PaymentStatus
import com.expenso.app.core.ui.components.CategoryChip
import com.expenso.app.core.ui.components.formatInr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    onBack: () -> Unit,
    vm: ExpenseDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val expense = state.expense

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.editMode && expense != null) {
                        IconButton(onClick = { vm.setEditMode(true) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { vm.delete() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        }
    ) { padding ->
        if (expense == null) {
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = expense.merchantName
                    ?: expense.payee?.displayName
                    ?: expense.payee?.vpa
                    ?: expense.category.name,
                style = MaterialTheme.typography.headlineMedium,
            )
            if (expense.payee != null && expense.payee.displayName != expense.payee.vpa) {
                Text(
                    text = expense.payee.vpa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))

            if (state.editMode) {
                OutlinedTextField(
                    value = state.amountRupeesInput,
                    onValueChange = vm::setAmount,
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.merchantInput,
                    onValueChange = vm::setMerchant,
                    label = { Text("Merchant / place") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.noteInput,
                    onValueChange = vm::setNote,
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Paid with", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                val methodOptions = listOf(
                    PaymentMethod.UPI,
                    PaymentMethod.CASH,
                    PaymentMethod.CARD,
                    PaymentMethod.OTHER,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    methodOptions.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = state.selectedMethod == option,
                            onClick = { vm.setMethod(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, methodOptions.size),
                            label = { Text(option.displayName) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(state.categories, key = { it.id }) { c ->
                        CategoryChip(
                            category = c,
                            selected = c.id == state.selectedCategoryId,
                            onClick = { vm.setCategory(c.id) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Save")
                }
            } else {
                Text(
                    formatInr(expense.amountMinor),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                DetailRow("Category", "${expense.category.emoji} ${expense.category.name}")
                DetailRow("Paid with", expense.paymentMethod.displayName)
                DetailRow("Status", expense.status.name.lowercase().replaceFirstChar { it.uppercase() })
                if (!expense.merchantName.isNullOrBlank()) DetailRow("Merchant", expense.merchantName!!)
                if (!expense.note.isNullOrBlank()) DetailRow("Note", expense.note!!)

                if (expense.status == PaymentStatus.PENDING) {
                    Spacer(Modifier.height(24.dp))
                    Text("Was this paid?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.markStatus(PaymentStatus.COMPLETED) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Paid") }
                        OutlinedButton(
                            onClick = { vm.markStatus(PaymentStatus.FAILED) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Failed") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
