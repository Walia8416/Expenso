package com.expenso.app.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.ui.components.CategoryChip
import com.expenso.app.core.ui.components.DatePillRow
import com.expenso.app.core.ui.components.LottieOneShot

/**
 * Compact single-screen expense logger. Unlike [AddExpenseSheet] / [LogExpenseSheet]
 * there are no tabs and no merchant field — just amount / method / category /
 * optional note / save. Designed to be opened from a secondary FAB on the
 * scanner screen so users can capture an out-of-band UPI / cash / card
 * payment in < 5 seconds.
 *
 * Reuses [LogExpenseViewModel] so saves land in exactly the same repository
 * path as the full flow (pending-status handling, import/export, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(
    categories: List<Category>,
    lastUsedCategoryId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    vm: LogExpenseViewModel = hiltViewModel(key = "quickLogVm"),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by vm.state.collectAsStateWithLifecycle()
    var showSuccess by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(categories, lastUsedCategoryId) {
        val defaultCat = lastUsedCategoryId
            ?: categories.firstOrNull { it.id != "other" }?.id
            ?: categories.firstOrNull()?.id
        vm.initializeDefault(defaultCat)
    }

    // Auto-focus the amount field on open so the keyboard pops — this is the
    // whole point of the "quick" flow.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                LogExpenseEvent.Saved -> showSuccess = true
                is LogExpenseEvent.Error -> Unit
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Text(
                    "Quick log",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Amount, method, category — done.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.amountRupeesInput,
                    onValueChange = vm::setAmount,
                    leadingIcon = {
                        Text("\u20B9", style = MaterialTheme.typography.headlineMedium)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                Spacer(Modifier.height(16.dp))
                QuickMethodPicker(method = state.method, onSelect = vm::setMethod)

                Spacer(Modifier.height(16.dp))
                DatePillRow(
                    epochMs = state.createdAt,
                    onDateChange = vm::setCreatedAt,
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(categories, key = { it.id }) { c ->
                        CategoryChip(
                            category = c,
                            selected = c.id == state.selectedCategoryId,
                            onClick = { vm.selectCategory(c.id) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.noteInput,
                    onValueChange = vm::setNote,
                    placeholder = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = vm::save,
                    enabled = state.amountRupeesInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Save", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(16.dp))
            }

            if (showSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LottieOneShot(
                        res = R.raw.success_check,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        onFinished = {
                            vm.reset()
                            showSuccess = false
                            onSaved()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickMethodPicker(method: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    // Quick-log only needs the common three. Users who want OTHER can still
    // reach it from the full AddExpenseSheet.
    val options = listOf(PaymentMethod.UPI, PaymentMethod.CASH, PaymentMethod.CARD)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = method == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(option.displayName) },
            )
        }
    }
}
