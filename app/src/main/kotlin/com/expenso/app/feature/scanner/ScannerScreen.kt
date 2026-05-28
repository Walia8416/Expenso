package com.expenso.app.feature.scanner

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.domain.upi.UpiIntentBuilder
import com.expenso.app.core.ui.components.LottieLoop
import com.expenso.app.feature.expense.AddExpenseSheet
import com.expenso.app.feature.pay.ConfirmPaymentSheet
import com.expenso.app.feature.pay.PaySheet
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onOpenSettings: () -> Unit,
    openAddSheetTab: Int? = null,
    onAddSheetTabConsumed: () -> Unit = {},
    vm: ScannerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.checkForPendingExpense()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(openAddSheetTab) {
        openAddSheetTab?.let {
            vm.openAddSheet(it)
            onAddSheetTabConsumed()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onQrDetected = vm::onQrDetected,
            )
            ScannerOverlay()
        } else {
            PermissionGate(onGrant = { cameraPermission.launchPermissionRequest() })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.scanner_hint),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(100))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallFloatingActionButton(
                    onClick = { vm.openQuickLog() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = "Quick log")
                }
                ExtendedFloatingActionButton(
                    onClick = { vm.openAddSheet(0) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add expense") },
                )
            }
        }
    }

    val request = state.pendingRequest
    if (request != null && !state.showAddSheet) {
        PaySheet(
            request = request,
            categories = state.categories,
            lastUsedCategoryId = state.lastUsedCategoryId,
            onDismiss = { vm.dismissPaySheet() },
            onLaunched = { expenseId -> vm.setPendingExpenseId(expenseId) },
            onError = { Timber.w("Pay error: $it") },
            launchUpi = { payload ->
                try {
                    val intent = UpiIntentBuilder.buildIntent(
                        uri = Uri.parse(payload.uri),
                        targetPackage = payload.targetPackage,
                    )
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Timber.w(e, "No UPI app resolved for intent")
                }
            },
        )
    }

    if (state.showAddSheet) {
        val manualRequest = remember {
            com.expenso.app.core.domain.upi.UpiPaymentRequest(
                payeeVpa = "",
                payeeName = null,
                amountRupees = null,
                currency = "INR",
                transactionNote = null,
                transactionRef = null,
                merchantCode = null,
                url = null,
                isSigned = false,
                rawParams = emptyMap(),
            )
        }
        AddExpenseSheet(
            manualRequest = manualRequest,
            categories = state.categories,
            lastUsedCategoryId = state.lastUsedCategoryId,
            selectedTabIndex = state.addSheetTabIndex,
            onSelectTab = vm::selectAddSheetTab,
            onDismiss = vm::dismissAddSheet,
            onLaunchUpi = { payload ->
                try {
                    val intent = UpiIntentBuilder.buildIntent(
                        uri = Uri.parse(payload.uri),
                        targetPackage = payload.targetPackage,
                    )
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Timber.w(e, "No UPI app resolved for intent")
                }
            },
            onLaunched = { expenseId ->
                vm.setPendingExpenseId(expenseId)
                vm.dismissAddSheet()
            },
            onCashSaved = { vm.dismissAddSheet() },
            onIncomeSaved = { vm.dismissAddSheet() },
            onError = { Timber.w("Add sheet error: $it") },
        )
    }

    if (state.showQuickLog) {
        com.expenso.app.feature.expense.QuickLogSheet(
            categories = state.categories,
            lastUsedCategoryId = state.lastUsedCategoryId,
            onDismiss = { vm.dismissQuickLog() },
            onSaved = { vm.dismissQuickLog() },
        )
    }

    val awaiting = state.awaitingConfirmFor
    if (awaiting != null) {
        ConfirmPaymentSheet(
            expense = awaiting,
            onMarkStatus = { vm.confirmStatus(it) },
            onLater = { vm.dismissConfirm() },
        )
    }

    if (state.showDefaultUpiPrompt) {
        com.expenso.app.feature.upi.UpiDefaultPromptSheet(
            onDismiss = { vm.dismissDefaultUpiPrompt() },
        )
    }
}

@Composable
private fun ScannerOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(24.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                LottieLoop(
                    res = R.raw.scan_pulse,
                    modifier = Modifier.size(220.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                LottieLoop(
                    res = R.raw.camera_permission,
                    modifier = Modifier.size(140.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.camera_permission_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.camera_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Button(
                onClick = onGrant,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.camera_permission_grant))
            }
        }
    }
}
