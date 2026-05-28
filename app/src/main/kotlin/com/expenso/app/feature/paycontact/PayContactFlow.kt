package com.expenso.app.feature.paycontact

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.core.data.repository.DeviceContact
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.Payee
import com.expenso.app.core.domain.upi.UpiPaymentRequest
import com.expenso.app.feature.pay.LaunchPayload
import com.expenso.app.feature.pay.PayEvent
import com.expenso.app.feature.pay.PaySheetContent
import com.expenso.app.feature.pay.PayViewModel
import java.math.BigDecimal

@Composable
fun PayContactFlow(
    categories: List<Category>,
    lastUsedCategoryId: String?,
    onLaunchUpi: (LaunchPayload) -> Unit,
    onLaunched: (String) -> Unit,
    onError: (String) -> Unit,
    vm: PayContactViewModel = hiltViewModel(),
    payVm: PayViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val payState by payVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> vm.setPermissionGranted(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        vm.setPermissionGranted(granted)
    }

    LaunchedEffect(payVm) {
        payVm.events.collect { ev ->
            when (ev) {
                is PayEvent.LaunchUpi -> {
                    onLaunchUpi(
                        LaunchPayload(
                            uri = ev.intentUri,
                            targetPackage = ev.targetPackage,
                            expenseId = ev.expenseId,
                        ),
                    )
                    onLaunched(ev.expenseId)
                }
                is PayEvent.Error -> onError(ev.message)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        when (state.step) {
            PayContactStep.Picker -> {
                if (!state.permissionGranted) {
                    PermissionRequestBlock(onGrant = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    })
                } else {
                    ContactPickerBlock(
                        query = state.searchQuery,
                        onQueryChange = vm::setSearchQuery,
                        loading = state.loading,
                        contacts = state.contacts,
                        recents = state.recentPayees,
                        onPick = vm::pickContact,
                        onPickRecent = vm::pickRecentPayee,
                    )
                }
            }
            PayContactStep.Capture -> {
                val contact = state.selectedContact
                if (contact == null) {
                    vm.resetToPicker()
                } else {
                    CaptureVpaBlock(
                        contact = contact,
                        vpa = state.vpaInput,
                        knownVpas = state.knownVpas,
                        error = state.error,
                        onVpaChange = vm::setVpa,
                        onPickKnown = { payee -> vm.setVpa(payee.vpa) },
                        onBack = vm::resetToPicker,
                        onContinue = {
                            if (vm.confirmVpa()) {
                                val defaultCat = lastUsedCategoryId
                                    ?: categories.firstOrNull { it.id != "other" }?.id
                                    ?: categories.firstOrNull()?.id
                                payVm.bindRequest(
                                    UpiPaymentRequest(
                                        payeeVpa = state.vpaInput,
                                        payeeName = contact.displayName,
                                        amountRupees = null as BigDecimal?,
                                        currency = "INR",
                                        transactionNote = null,
                                        transactionRef = null,
                                        merchantCode = null,
                                        url = null,
                                        isSigned = false,
                                        rawParams = emptyMap(),
                                    ),
                                    defaultCat,
                                )
                            }
                        },
                    )
                }
            }
            PayContactStep.Ready -> {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = vm::resetToPicker) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            "Paying ${state.selectedContact?.displayName.orEmpty()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    PaySheetContent(
                        state = payState,
                        categories = categories,
                        onVpaChange = payVm::setVpa,
                        onAmountChange = payVm::setAmount,
                        onNoteChange = payVm::setNote,
                        onCategorySelect = payVm::selectCategory,
                        onChangeUpiApp = payVm::chooseUpiApp,
                        onPayClicked = payVm::onPayClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestBlock(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Contacts,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Pay people from your contacts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow Expenso to read contacts so you can pick a friend and send UPI money in one tap.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Allow contacts") }
    }
}

@Composable
private fun ContactPickerBlock(
    query: String,
    onQueryChange: (String) -> Unit,
    loading: Boolean,
    contacts: List<DeviceContact>,
    recents: List<Payee>,
    onPick: (DeviceContact) -> Unit,
    onPickRecent: (Payee) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search contacts") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        if (recents.isNotEmpty()) {
            Text(
                "Recent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recents, key = { it.id }) { p ->
                    RecentChip(payee = p, onClick = { onPickRecent(p) })
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Text(
            "Contacts",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().height(360.dp),
        ) {
            if (loading && contacts.isEmpty()) {
                item { Text("Loading contacts...", modifier = Modifier.padding(16.dp)) }
            }
            items(contacts, key = { it.lookupKey + it.phoneNumber }) { c ->
                ContactRow(contact = c, onClick = { onPick(c) })
            }
            if (!loading && contacts.isEmpty()) {
                item {
                    Text(
                        "No contacts found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentChip(payee: Payee, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                payee.displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            payee.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ContactRow(contact: DeviceContact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                contact.displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                contact.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CaptureVpaBlock(
    contact: DeviceContact,
    vpa: String,
    knownVpas: List<Payee>,
    error: String?,
    onVpaChange: (String) -> Unit,
    onPickKnown: (Payee) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    contact.displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Their UPI ID (VPA)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = vpa,
            onValueChange = onVpaChange,
            placeholder = { Text("e.g. 9876543210@upi") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        if (knownVpas.size > 1) {
            Spacer(Modifier.height(8.dp))
            Text("Previously used", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(knownVpas) { k ->
                    Box(
                        modifier = Modifier
                            .clickable { onPickKnown(k) }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp),
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) { Text(k.vpa, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            enabled = vpa.contains("@"),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text("Continue") }

        Spacer(Modifier.height(4.dp))

        Text(
            "VPA isn't available from the contact app; enter it once and we'll remember it for this person.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8A8A),
        )
    }
}
