package com.expenso.app.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.core.io.ImportKind
import com.expenso.app.feature.data.DataEvent
import com.expenso.app.feature.data.DataViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCategoryManager: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
    dataVm: DataViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val dataState by dataVm.state.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    var showFormatHelp by remember { mutableStateOf(false) }

    val csvMime = "text/csv"
    val exportExpensesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(csvMime),
    ) { uri -> if (uri != null) dataVm.exportExpenses(uri) }
    val exportIncomeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(csvMime),
    ) { uri -> if (uri != null) dataVm.exportIncome(uri) }
    val importExpensesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) dataVm.previewExpenses(uri) }
    val importIncomeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) dataVm.previewIncome(uri) }

    LaunchedEffect(dataVm) {
        dataVm.events.collect { ev ->
            message = when (ev) {
                is DataEvent.Exported -> {
                    val label = if (ev.kind == ImportKind.EXPENSE) "expenses" else "income"
                    "Exported ${ev.rows} $label rows"
                }
                is DataEvent.ImportDone -> {
                    val added = ev.result.expensesAdded + ev.result.incomeAdded
                    "Imported $added rows. Skipped ${ev.result.skipped}"
                }
                is DataEvent.Error -> "\u26A0 ${ev.message}"
                is DataEvent.PreviewReady -> null
            }
        }
    }

    if (showFormatHelp) {
        ImportFormatSheet(onDismiss = { showFormatHelp = false })
    }

    val preview = dataState.preview
    if (preview != null) {
        ImportPreviewSheet(
            preview = preview,
            onConfirm = { dataVm.confirmImport() },
            onDismiss = { dataVm.dismissPreview() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SectionTitle("Default UPI app") }
            items(state.installedUpiApps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setDefaultUpi(app.packageName) }
                        .background(
                            if (app.packageName == state.defaultUpiPackage) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.displayName, modifier = Modifier.weight(1f))
                    if (app.packageName == state.defaultUpiPackage) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (state.installedUpiApps.isEmpty()) {
                item {
                    Text(
                        "No UPI apps detected on this device.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Text("Why Swiggy/Zepto may not show Expenso", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Some checkout SDKs open a fixed list of bank UPI apps using package whitelists, so Android cannot show every app that handles upi:// links.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Expenso still appears in the standard Android UPI chooser when apps use normal implicit UPI intents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Categories")
            }
            item {
                SimpleRow(title = "Manage categories", onClick = onOpenCategoryManager)
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Security")
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Require fingerprint / face to open Expenso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.biometricEnabled,
                        onCheckedChange = vm::setBiometric,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UPI Assist (non-root)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Detects merchant to UPI app hand-off and shows a quick Expenso log prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.upiAssistEnabled,
                        onCheckedChange = vm::setUpiAssist,
                    )
                }
            }
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val enabled = remember { isUpiAssistServiceEnabled(context) }
                DataRow(
                    title = "Enable Accessibility for UPI Assist",
                    subtitle = if (enabled) "Accessibility service enabled" else "Required for non-root merchant hand-off detection",
                    icon = Icons.Rounded.Info,
                    enabled = true,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Data  ·  CSV")
            }
            item {
                val stamp = remember { SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) }
                DataRow(
                    title = "Export expenses (CSV)",
                    subtitle = "Saves expenses.csv — openable in Excel, Sheets, Numbers",
                    icon = Icons.Rounded.FileDownload,
                    enabled = !dataState.busy,
                    onClick = { exportExpensesLauncher.launch("expenso-expenses-$stamp.csv") },
                )
            }
            item {
                val stamp = remember { SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) }
                DataRow(
                    title = "Export income (CSV)",
                    subtitle = "Saves income.csv with your recorded income rows",
                    icon = Icons.Rounded.FileDownload,
                    enabled = !dataState.busy,
                    onClick = { exportIncomeLauncher.launch("expenso-income-$stamp.csv") },
                )
            }
            item {
                DataRow(
                    title = "Import expenses (CSV)",
                    subtitle = "Pick a file matching the expense format",
                    icon = Icons.Rounded.FileUpload,
                    enabled = !dataState.busy,
                    onClick = {
                        importExpensesLauncher.launch(arrayOf(
                            "text/csv", "text/comma-separated-values", "application/csv",
                            "text/plain", "application/octet-stream", "*/*",
                        ))
                    },
                )
            }
            item {
                DataRow(
                    title = "Import income (CSV)",
                    subtitle = "Pick a file matching the income format",
                    icon = Icons.Rounded.FileUpload,
                    enabled = !dataState.busy,
                    onClick = {
                        importIncomeLauncher.launch(arrayOf(
                            "text/csv", "text/comma-separated-values", "application/csv",
                            "text/plain", "application/octet-stream", "*/*",
                        ))
                    },
                )
            }
            item {
                DataRow(
                    title = "How to format CSV files",
                    subtitle = "Expected columns, date formats, payment methods",
                    icon = Icons.Rounded.Info,
                    enabled = true,
                    onClick = { showFormatHelp = true },
                )
            }
            val msg = message
            if (msg != null) {
                item {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("About")
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Text("Expenso", style = MaterialTheme.typography.titleMedium)
                    Text("v0.1.0", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your data stays on this device. No SMS parsing, no cloud account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun isUpiAssistServiceEnabled(context: android.content.Context): Boolean {
    val cn = ComponentName(context, com.expenso.app.feature.upiassist.UpiAssistAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabled?.contains(cn.flattenToString(), ignoreCase = true) == true
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun DataRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SimpleRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    }
}
