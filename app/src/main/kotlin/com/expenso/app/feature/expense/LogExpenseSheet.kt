package com.expenso.app.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.ui.components.CategoryChip
import com.expenso.app.core.ui.components.LottieOneShot

@Composable
fun LogExpenseSheet(
    categories: List<Category>,
    lastUsedCategoryId: String?,
    onSaved: () -> Unit,
    vm: LogExpenseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(categories, lastUsedCategoryId) {
        val defaultCat = lastUsedCategoryId
            ?: categories.firstOrNull { it.id != "other" }?.id
            ?: categories.firstOrNull()?.id
        vm.initializeDefault(defaultCat)
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                LogExpenseEvent.Saved -> showSuccess = true
                is LogExpenseEvent.Error -> Unit
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Text(
                "Log an expense",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Already paid — UPI, cash, or card? Just log it here, no launch needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.amountRupeesInput,
                onValueChange = vm::setAmount,
                leadingIcon = { Text("\u20B9", style = MaterialTheme.typography.headlineMedium) },
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Paid with")
            Spacer(Modifier.height(8.dp))
            MethodPicker(
                method = state.method,
                onSelect = vm::setMethod,
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Category")
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

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.merchantNameInput,
                onValueChange = vm::setMerchant,
                placeholder = { Text("Merchant or place (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.noteInput,
                onValueChange = vm::setNote,
                placeholder = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = vm::save,
                enabled = state.amountRupeesInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    "Save expense",
                    style = MaterialTheme.typography.titleMedium,
                )
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

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MethodPicker(method: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    // UPI first: most users manually log a UPI payment that bypassed the
    // mediator (merchant flows that `setPackage(...)` a specific PSP), so
    // preselect it.
    val options = listOf(
        PaymentMethod.UPI,
        PaymentMethod.CASH,
        PaymentMethod.CARD,
        PaymentMethod.OTHER,
    )
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
