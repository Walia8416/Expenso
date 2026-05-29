package com.expenso.app.feature.income

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.ui.components.DatePillRow
import com.expenso.app.core.ui.components.LottieOneShot

@Composable
fun AddIncomeSheet(
    onSaved: () -> Unit,
    vm: AddIncomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSuccess by remember { mutableStateOf(false) }
    val accent = Color(0xFF14B886)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                AddIncomeEvent.Saved -> showSuccess = true
                is AddIncomeEvent.Error -> Unit
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
                "Log income",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Salary, gifts, interest \u2014 anything that came in.",
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

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.sourceInput,
                onValueChange = vm::setSource,
                label = { Text("Source") },
                placeholder = { Text("Salary, Family, Freelance\u2026") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.recentSources.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(state.recentSources, key = { it }) { src ->
                        val selected = src.equals(state.sourceInput, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clickable { vm.pickSource(src) }
                                .background(
                                    if (selected) accent.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp),
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                src,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(8.dp))
            DatePillRow(
                epochMs = state.createdAt,
                onDateChange = vm::setCreatedAt,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.descriptionInput,
                onValueChange = vm::setDescription,
                label = { Text("Description / Invoice #") },
                placeholder = { Text("Optional") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.noteInput,
                onValueChange = vm::setNote,
                label = { Text("Note") },
                placeholder = { Text("Optional") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = vm::save,
                enabled = state.amountRupeesInput.isNotBlank() && state.sourceInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    "Save income",
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
