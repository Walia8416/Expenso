package com.expenso.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.R
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.ui.components.CategoryChip
import com.expenso.app.core.ui.components.DatePillRow
import com.expenso.app.core.ui.components.GlassButton
import com.expenso.app.core.ui.components.LottieOneShot
import com.expenso.app.feature.expense.LogExpenseEvent
import com.expenso.app.feature.expense.LogExpenseViewModel
import com.expenso.app.feature.income.AddIncomeSheet

/**
 * Minimal logging-first home. Empty state shows only the amount field and two
 * small icon buttons (Scan QR, Add Income). Settings sits as a tiny corner
 * gear. Once the user starts typing, category / date / method / note / Save
 * stagger in with fade + slide animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenScanner: () -> Unit,
    onOpenSettings: () -> Unit,
    homeBootstrap: HomeBootstrap? = null,
    onBootstrapConsumed: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
    formVm: LogExpenseViewModel = hiltViewModel(key = "homeFormVm"),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val formState by formVm.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var showSuccess by remember { mutableStateOf(false) }
    var showIncomeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.categories, state.lastUsedCategoryId) {
        val defaultCat = state.lastUsedCategoryId
            ?: state.categories.firstOrNull { it.id != "other" }?.id
            ?: state.categories.firstOrNull()?.id
        formVm.initializeDefault(defaultCat)
    }

    LaunchedEffect(homeBootstrap) {
        val b = homeBootstrap ?: return@LaunchedEffect
        b.presetMethod?.let { formVm.setMethod(it) }
        if (b.focusAmount) {
            kotlinx.coroutines.delay(180)
            runCatching { focusRequester.requestFocus() }
        }
        onBootstrapConsumed()
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(180)
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(formVm) {
        formVm.events.collect { event ->
            when (event) {
                LogExpenseEvent.Saved -> showSuccess = true
                is LogExpenseEvent.Error -> Unit
            }
        }
    }

    val amountFilled = formState.amountRupeesInput.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Push the amount field toward the vertical center when the form
            // is empty; collapse the gap once the user starts typing so the
            // rest of the form has room to flow in.
            val topSpace by animateDpAsState(
                targetValue = if (amountFilled) 40.dp else 180.dp,
                animationSpec = tween(durationMillis = 320),
                label = "topSpace",
            )
            Spacer(Modifier.height(topSpace))

            AmountField(
                value = formState.amountRupeesInput,
                onChange = formVm::setAmount,
                focusRequester = focusRequester,
            )

            Spacer(Modifier.height(14.dp))
            // Two tiny pill buttons under the amount field. Always present so
            // Scan & Income are one tap away.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                TinyPillButton(
                    icon = Icons.Rounded.QrCodeScanner,
                    label = "Scan",
                    onClick = onOpenScanner,
                )
                TinyPillButton(
                    icon = Icons.Rounded.Add,
                    label = "Income",
                    onClick = { showIncomeSheet = true },
                )
            }

            // Each progressive section gets its own AnimatedVisibility with a
            // staggered delay so they cascade in instead of popping together.
            StaggeredReveal(visible = amountFilled, delayMs = 0) {
                Spacer(Modifier.height(22.dp))
                CategoryRow(
                    categories = state.categories,
                    selectedId = formState.selectedCategoryId,
                    onSelect = formVm::selectCategory,
                )
            }

            StaggeredReveal(visible = amountFilled, delayMs = 90) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DatePillRow(
                        epochMs = formState.createdAt,
                        onDateChange = formVm::setCreatedAt,
                    )
                    QuickMethodPicker(
                        method = formState.method,
                        onSelect = formVm::setMethod,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            StaggeredReveal(visible = amountFilled, delayMs = 160) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = formState.noteInput,
                    onValueChange = formVm::setNote,
                    placeholder = {
                        Text("Note (optional)", style = MaterialTheme.typography.bodySmall)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
            }

            StaggeredReveal(visible = amountFilled, delayMs = 230) {
                Spacer(Modifier.height(14.dp))
                GlassButton(
                    onClick = formVm::save,
                    enabled = amountFilled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        // Settings cog is drawn AFTER the Column so it sits on top of any
        // scroll content and receives taps. Sized small / tinted muted so it
        // stays visually demoted.
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    CircleShape,
                ),
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
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
                    modifier = Modifier.fillMaxWidth(0.55f),
                    onFinished = {
                        formVm.reset()
                        showSuccess = false
                    },
                )
            }
        }
    }

    if (showIncomeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showIncomeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AddIncomeSheet(onSaved = { showIncomeSheet = false })
        }
    }
}

@Composable
private fun StaggeredReveal(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 280, delayMillis = delayMs)) +
            slideInVertically(
                tween(durationMillis = 320, delayMillis = delayMs),
                initialOffsetY = { it / 4 },
            ) +
            expandVertically(tween(durationMillis = 280, delayMillis = delayMs)),
        exit = fadeOut(tween(160)) +
            slideOutVertically(tween(160), targetOffsetY = { it / 4 }) +
            shrinkVertically(tween(160)),
    ) {
        Column { content() }
    }
}

@Composable
private fun TinyPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(100),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "How much?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            leadingIcon = {
                Text(
                    "₹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            placeholder = {
                Text(
                    "0",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(18.dp),
        )
    }
}

@Composable
private fun CategoryRow(
    categories: List<com.expenso.app.core.domain.model.Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(categories, key = { it.id }) { c ->
            CategoryChip(
                category = c,
                selected = c.id == selectedId,
                onClick = { onSelect(c.id) },
            )
        }
    }
}

@Composable
private fun QuickMethodPicker(
    method: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(PaymentMethod.UPI, PaymentMethod.CASH, PaymentMethod.CARD)
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = method == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(option.displayName, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

/** Bootstrap from a widget / notification deep link. */
data class HomeBootstrap(
    val presetMethod: PaymentMethod? = null,
    val focusAmount: Boolean = false,
)
