package com.expenso.app.feature.onboarding

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.ui.components.LottieLoop
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(state.page) {
        if (state.page == 2) vm.refreshInstalledApps()
    }

    when (state.page) {
        0, 1, 2 -> ValuePropPage(
            page = state.page,
            onNext = { vm.goToPage(state.page + 1) },
        )
        3 -> CameraPermissionPage(
            granted = cameraPermission.status.isGranted,
            onRequest = { cameraPermission.launchPermissionRequest() },
            onSkip = { vm.goToPage(4) },
            onNext = { vm.goToPage(4) },
        )
        4 -> UpiAppPickerPage(
            apps = state.installedUpiApps,
            selected = state.selectedUpiPackage,
            onSelect = vm::selectUpiApp,
            onDone = {
                vm.saveDefaultUpiApp()
                onDone()
            },
        )
    }
}

@Composable
private fun ValuePropPage(
    page: Int,
    onNext: () -> Unit,
) {
    val (icon, title, body) = when (page) {
        0 -> Triple(
            Icons.Rounded.Bolt,
            stringResource(R.string.onboarding_1_title),
            stringResource(R.string.onboarding_1_body),
        )
        1 -> Triple(
            Icons.Rounded.Lock,
            stringResource(R.string.onboarding_2_title),
            stringResource(R.string.onboarding_2_body),
        )
        else -> Triple(
            Icons.Rounded.CheckCircle,
            stringResource(R.string.onboarding_3_title),
            stringResource(R.string.onboarding_3_body),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val lottieRes = when (page) {
                0 -> R.raw.scan_pulse
                1 -> R.raw.camera_permission
                else -> R.raw.onboarding_hero
            }
            LottieLoop(res = lottieRes, modifier = Modifier.size(200.dp))
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0f),
                modifier = Modifier.size(1.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (page < 2) R.string.onboarding_cta_next
                    else R.string.onboarding_cta_next,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CameraPermissionPage(
    granted: Boolean,
    onRequest: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    LaunchedEffect(granted) {
        if (granted) onNext()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.camera_permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.camera_permission_grant))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun UpiAppPickerPage(
    apps: List<com.expenso.app.core.domain.model.InstalledUpiApp>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            "Pick your default UPI app",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We'll open this app when you pay. You can change it any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        if (apps.isEmpty()) {
            Text(
                "No UPI apps found on this device. Install Google Pay, PhonePe, Paytm, or BHIM first.",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(apps) { app ->
                    UpiAppRow(
                        app = app,
                        selected = app.packageName == selected,
                        onClick = { onSelect(app.packageName) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDone,
            enabled = selected != null || apps.isEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(stringResource(R.string.onboarding_cta_done))
        }
    }
}

@Composable
private fun UpiAppRow(
    app: com.expenso.app.core.domain.model.InstalledUpiApp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
