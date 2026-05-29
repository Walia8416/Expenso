package com.expenso.app.feature.pay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.upi.UpiIntentBuilder
import com.expenso.app.core.domain.upi.UpiPaymentRequest
import com.expenso.app.core.ui.components.CategoryChip
import com.expenso.app.core.ui.components.GlassButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaySheet(
    request: UpiPaymentRequest,
    categories: List<Category>,
    lastUsedCategoryId: String?,
    onDismiss: () -> Unit,
    onLaunched: (String) -> Unit,
    onError: (String) -> Unit,
    vm: PayViewModel = hiltViewModel(),
    launchUpi: (LaunchPayload) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(request) {
        val defaultCat = lastUsedCategoryId
            ?: categories.firstOrNull { it.id != "other" }?.id
            ?: categories.firstOrNull()?.id
        vm.bindRequest(request, defaultCat)
    }

    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is PayEvent.LaunchUpi -> {
                    launchUpi(
                        LaunchPayload(
                            uri = ev.intentUri,
                            targetPackage = ev.targetPackage,
                            expenseId = ev.expenseId,
                            qrSource = ev.qrSource,
                        )
                    )
                    onLaunched(ev.expenseId)
                }
                is PayEvent.Error -> onError(ev.message)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        PaySheetContent(
            state = state,
            categories = categories,
            onVpaChange = vm::setVpa,
            onAmountChange = vm::setAmount,
            onNoteChange = vm::setNote,
            onCategorySelect = vm::selectCategory,
            onChangeUpiApp = vm::chooseUpiApp,
            onPayClicked = vm::onPayClicked,
        )
    }
}

data class LaunchPayload(
    val uri: String,
    val targetPackage: String?,
    val expenseId: String,
    val qrSource: UpiIntentBuilder.QrSource? = null,
)

@Composable
internal fun PaySheetContent(
    state: PayUiState,
    categories: List<Category>,
    onVpaChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onChangeUpiApp: (String) -> Unit,
    onPayClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // VPA header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Paying to",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                if (state.isVpaEditable) {
                    OutlinedTextField(
                        value = state.vpaInput,
                        onValueChange = onVpaChange,
                        placeholder = { Text("VPA or phone@upi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                } else {
                    Text(
                        text = state.payeeNameInput.ifBlank { state.vpaInput },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.payeeNameInput.isNotBlank()) {
                        Text(
                            text = state.vpaInput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Amount
        Text(
            "Amount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.amountRupeesInput,
            onValueChange = onAmountChange,
            leadingIcon = { Text("₹", style = MaterialTheme.typography.headlineMedium) },
            singleLine = true,
            textStyle = MaterialTheme.typography.displayMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        if (state.isSignedQr && state.amountWasPrefilled) {
            Text(
                text = "Merchants may reject a changed amount on signed QRs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (state.duplicateWarning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(0.dp))
                Text(
                    "  You paid the same amount to this VPA recently.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

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
                    onClick = { onCategorySelect(c.id) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = state.noteInput,
            onValueChange = onNoteChange,
            placeholder = { Text("Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        UpiAppSelector(
            apps = state.installedUpiApps,
            chosen = state.chosenUpiPackage,
            onChoose = onChangeUpiApp,
        )

        Spacer(Modifier.height(20.dp))

        val chosenApp = state.installedUpiApps.firstOrNull { it.packageName == state.chosenUpiPackage }
        GlassButton(
            onClick = onPayClicked,
            enabled = state.amountRupeesInput.isNotBlank() && state.vpaInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (chosenApp != null) "Pay with ${chosenApp.displayName}" else "Pay",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun UpiAppSelector(
    apps: List<com.expenso.app.core.domain.model.InstalledUpiApp>,
    chosen: String?,
    onChoose: (String) -> Unit,
) {
    if (apps.isEmpty()) {
        Text(
            "No UPI apps installed — can't launch payment.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Column {
        Text(
            "Pay using",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps, key = { it.packageName }) { app ->
                val selected = app.packageName == chosen
                Box(
                    modifier = Modifier
                        .clickable { onChoose(app.packageName) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp),
                        )
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
